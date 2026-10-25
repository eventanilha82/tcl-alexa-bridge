package local.tclbridge;

import android.content.Context;
import android.os.IBinder;
import android.provider.Settings;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.*;
import org.json.JSONObject;

/** Collects this TV only. Never copies another installation's identifiers or credentials. */
final class DeviceIdentity {
  static final class Detected {final JSONObject profile;final String source;Detected(JSONObject p,String s){profile=p;source=s;}}
  static Detected collect(Context context,LocalAdb adb)throws Exception{
    String country=Settings.System.getString(context.getContentResolver(),"tcl_sys_country_code");
    if(!"BR".equalsIgnoreCase(country))throw new IOException("Este assistente está validado para TVs TCL configuradas para Brasil. Não altere o país para continuar.");
    String client=adb.shell("getprop ro.product.clientType"),firmware=adb.shell("getprop ro.software.version_id");
    String wifi=adb.shell("cmd wifi status");Matcher match=Pattern.compile("(?i)\\bMAC:\\s*([0-9a-f]{2}(?::[0-9a-f]{2}){5})").matcher(wifi);
    if(!match.find())throw new IOException("Conecte esta TV ao Wi-Fi para identificar seu endereço de rede e tente novamente.");
    String mac=match.group(1).toLowerCase(Locale.ROOT),id="",source="tcl";
    try{
      Class<?> sm=Class.forName("android.os.ServiceManager");IBinder binder=(IBinder)sm.getMethod("getService",String.class).invoke(null,"deviceinfo");
      Class<?> stub=Class.forName("com.tcl.os.ITclDeviceInfoManager$Stub");Object service=stub.getMethod("asInterface",IBinder.class).invoke(null,binder);
      id=(String)Class.forName("com.tcl.os.ITclDeviceInfoManager").getMethod("getDeviceID").invoke(service);
    }catch(Exception|LinkageError unavailable){id="";}
    if(id==null||id.trim().isEmpty()){
      source="tcl_native_diagnostic";
      for(int attempt=0;attempt<10;attempt++){
        String nativeId=adb.shell("logcat --uid=0 -d -v raw -t 400 -s thal_mid_key:D '*:S' | grep -a -E 'thal_read_deviceId.*(dev_id=|p_buf=)' || true");
        Matcher values=Pattern.compile("(?:dev_id|p_buf)\\s*=\\s*([0-9a-fA-F]{40})(?![0-9a-fA-F])").matcher(nativeId);
        java.util.Set<String> unique=new java.util.HashSet<>();while(values.find())unique.add(values.group(1).toLowerCase(Locale.ROOT));
        if(unique.size()>1)throw new IOException("A identificação TCL retornou dados inconsistentes. Não foi criado um perfil.");
        if(unique.size()==1){id=unique.iterator().next();break;}
        Thread.sleep(1000);
      }
      if(id==null||id.isEmpty())throw new IOException("A TCL ainda não disponibilizou a identificação desta TV. Abra o SmartAlexa e tente novamente. Nenhum identificador foi inventado.");
    }
    JSONObject profile=new JSONObject().put("city","").put("client_type",client).put("country","52").put("dev_desp","").put("device_id",id).put("mac","wireless="+mac).put("netWork_standby","on").put("software_version",firmware).put("tv_name",Settings.Global.getString(context.getContentResolver(),"device_name"));
    return new Detected(RegistryClient.validateProfile(profile),source);
  }
}
