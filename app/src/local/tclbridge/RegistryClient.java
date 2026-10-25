package local.tclbridge;
import android.content.Context;
import android.provider.Settings;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

final class RegistryClient {
  private static final String PROFILE="device-profile.json";
  private static final String[] FIELDS={"city","client_type","country","dev_desp","device_id","mac","netWork_standby","software_version","tv_name"};
  private static final Pattern MAC=Pattern.compile("(?i)(?<![0-9a-f:])([0-9a-f]{2}(?::[0-9a-f]{2}){5})(?![0-9a-f:])");

  /** Prefer a private profile. Import only on first setup; never overwrite it silently. */
  static synchronized JSONObject profile(Context context)throws Exception{
    File privateFile=new File(context.getFilesDir(),PROFILE);JSONObject profile;
    if(privateFile.exists())profile=validateProfile(read(new FileInputStream(privateFile)));
    else{
      File external=context.getExternalFilesDir(null),inbox=external==null?null:new File(external,PROFILE);
      if(inbox!=null&&inbox.exists()){
        profile=validateProfile(read(new FileInputStream(inbox)));
        savePrivate(context,profile,privateFile);
        if(!inbox.delete())throw new IOException("Perfil salvo; remova o arquivo de importação do armazenamento externo");
      }else{
        try{profile=validateProfile(read(context.getAssets().open(PROFILE)));savePrivate(context,profile,privateFile);}
        catch(FileNotFoundException e){throw new IOException("Configure o perfil desta TV em device-profile.json antes de vincular a conta");}
      }
    }
    String name=Settings.Global.getString(context.getContentResolver(),"device_name");
    if(name!=null&&!name.trim().isEmpty())profile.put("tv_name",name.trim().substring(0,Math.min(name.trim().length(),128)));
    return profile;
  }

  private static void savePrivate(Context context,JSONObject profile,File target)throws IOException{
    File staged=new File(context.getFilesDir(),PROFILE+".tmp");
    try(FileOutputStream out=context.openFileOutput(staged.getName(),Context.MODE_PRIVATE)){
      out.write(profile.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();
    }
    if(!staged.renameTo(target))throw new IOException("Não foi possível salvar o perfil privado da TV");
  }

  static synchronized void installDetected(Context context,DeviceIdentity.Detected detected)throws Exception{
    File target=new File(context.getFilesDir(),PROFILE);
    if(target.exists()){validateProfile(read(new FileInputStream(target)));return;}
    savePrivate(context,validateProfile(detected.profile),target);
    context.getSharedPreferences("bridge",0).edit().putString("identity_source",detected.source).commit();
  }

  private static JSONObject read(InputStream source)throws Exception{
    try(InputStream in=source;ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] b=new byte[1024];int n;
      while((n=in.read(b))!=-1){if(out.size()+n>32768)throw new IOException("Perfil da TV excede o tamanho permitido");out.write(b,0,n);}
      JSONTokener input=new JSONTokener(out.toString("UTF-8"));Object parsed=input.nextValue();
      if(!(parsed instanceof JSONObject)||input.nextClean()!=0)throw new IOException("Perfil da TV deve ser um objeto JSON válido");
      return (JSONObject)parsed;
    }
  }

  static JSONObject validateProfile(JSONObject input)throws Exception{
    if(input==null)throw new IOException("Perfil da TV ausente");
    for(String forbidden:new String[]{"access_token","refresh_token","tcl_token","tcl_refreshtoken","sign_key","private_key"})
      if(input.has(forbidden))throw new IOException("O perfil da TV não deve conter credenciais");
    JSONObject profile=new JSONObject();
    for(String field:FIELDS){Object value=input.opt(field);if(value!=null&&value!=JSONObject.NULL&&!(value instanceof String)&&!(value instanceof Number))throw new IOException("Tipo inválido no campo "+field);profile.put(field,value==null||value==JSONObject.NULL?"":String.valueOf(value).trim());}
    String id=profile.optString("device_id"),client=profile.optString("client_type"),firmware=profile.optString("software_version");
    if(id.length()<8||id.length()>128||!id.matches("[A-Za-z0-9:_-]+")||placeholder(id)||id.matches("0+"))throw new IOException("device_id ausente, inválido ou de exemplo; use o identificador real desta TV");
    if(!client.startsWith("TCL-")||client.length()>128||placeholder(client))throw new IOException("client_type ausente, inválido ou de exemplo");
    if(firmware.isEmpty()||firmware.length()>128||placeholder(firmware))throw new IOException("software_version ausente, inválida ou de exemplo");
    String country=profile.optString("country");
    if(!country.matches("[0-9]{1,3}")||Integer.parseInt(country)==0)throw new IOException("country deve ser o código numérico TCL desta TV");
    String macs=profile.optString("mac");if(macs.length()>256||placeholder(macs))throw new IOException("mac ausente, inválido ou de exemplo");
    Matcher matcher=MAC.matcher(macs);boolean found=false;
    while(matcher.find()){
      String mac=matcher.group(1).toLowerCase(Locale.ROOT);
      if(mac.equals("02:00:00:00:00:00")||mac.equals("00:00:00:00:00:00")||mac.equals("ff:ff:ff:ff:ff:ff")||(Integer.parseInt(mac.substring(0,2),16)&1)!=0)
        throw new IOException("mac contém endereço padrão ou não utilizável; informe o MAC real da TV");
      found=true;
    }
    if(!found)throw new IOException("mac deve conter pelo menos um endereço MAC real da TV");
    for(String field:new String[]{"city","dev_desp","tv_name"})if(profile.optString(field).length()>256)throw new IOException("Campo de perfil muito longo: "+field);
    String standby=profile.optString("netWork_standby");
    if(standby.isEmpty())profile.put("netWork_standby","on");
    else if(!standby.equals("on")&&!standby.equals("off"))throw new IOException("netWork_standby deve ser on ou off");
    return profile;
  }

  private static boolean placeholder(String value){String s=value.toLowerCase(Locale.ROOT);return s.contains("example")||s.contains("exemplo")||s.contains("replace")||s.contains("changeme")||s.contains("your_")||s.contains("seu_")||s.contains("todo")||s.contains("<")||s.contains(">");}

  static JSONObject registration(Context context,String uuid)throws Exception{
    JSONObject info=profile(context);info.put("uuid",uuid==null?"":uuid);info.put("version_number",1);
    byte[] raw=info.toString().getBytes(StandardCharsets.UTF_8);byte[] signature=NativeSigner.sign(context,raw);
    try{return new JSONObject().put("tv_info",numbers(raw)).put("sign_key",numbers(signature));}
    finally{Arrays.fill(signature,(byte)0);}
  }
  static JSONArray numbers(byte[] bytes){JSONArray array=new JSONArray();for(byte value:bytes)array.put((int)value);return array;}
}
