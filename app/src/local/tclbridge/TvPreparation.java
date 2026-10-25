package local.tclbridge;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;
import java.util.function.Consumer;

final class TvPreparation {
  static boolean hasProfile(Context c){try{RegistryClient.profile(c);return true;}catch(Exception e){return false;}}
  static boolean nativeReady(Context c){try{return NativeSigner.dryRun(c).optBoolean("verified")&&c.getPackageManager().getPackageInfo("com.tcl.smartalexa",0).getLongVersionCode()>=613250000L;}catch(Exception e){return false;}}
  static org.json.JSONObject components(Context c){org.json.JSONObject result=new org.json.JSONObject();try{
    try{if(!NativeSigner.dryRun(c).optBoolean("verified"))throw new Exception();}catch(Exception e){return result.put("ready",false).put("message","TTVSCore de sistema ausente ou incompatível. Este assistente requer uma TV TCL compatível.");}
    android.content.pm.PackageInfo smart;
    try{smart=c.getPackageManager().getPackageInfo("com.tcl.smartalexa",PackageManager.GET_SIGNING_CERTIFICATES);}catch(PackageManager.NameNotFoundException e){return result.put("ready",false).put("can_install_smartalexa",true).put("message","Falta instalar o SmartAlexa original da TCL.");}
    boolean certificate=smart.signingInfo!=null&&smart.signingInfo.getApkContentsSigners().length==1&&SmartAlexaInstaller.CERT.equals(SmartAlexaInstaller.hex(java.security.MessageDigest.getInstance("SHA-256").digest(smart.signingInfo.getApkContentsSigners()[0].toByteArray())));
    if(!certificate)return result.put("ready",false).put("message","A assinatura do SmartAlexa instalado não foi reconhecida. Confira a origem do aplicativo antes de continuar.");
    if(smart.getLongVersionCode()<613250000L)return result.put("ready",false).put("can_install_smartalexa",true).put("message","Atualize o SmartAlexa para a versão validada neste projeto.");
    return result.put("ready",true).put("message",smart.applicationInfo!=null&&!smart.applicationInfo.enabled?"SmartAlexa original detectado. Ele será ativado na preparação.":"TTVSCore e SmartAlexa originais disponíveis nesta TV.");
  }catch(Exception e){try{result.put("ready",false).put("message","Não foi possível verificar os componentes da TV. Tente novamente.");}catch(Exception ignored){}return result;}}
  static void run(Context c,LocalAdb adb,boolean standby,boolean saveProfile,Consumer<String> progress)throws Exception{
    if(!"local.tclbridge".equals(c.getPackageName()))throw new IllegalStateException("Pacote de instalação incompatível");
    adb.connect(progress);
    DeviceIdentity.Detected detected=null;
    if(!hasProfile(c)){progress.accept("Identificando esta TV…");detected=DeviceIdentity.collect(c,adb);}
    progress.accept("Preparando o início automático e os controles…");
    String[] commands={
      "pm enable com.tcl.smartalexa",
      "cmd appops set local.tclbridge AUTO_START allow","cmd appops set --uid local.tclbridge AUTO_START allow",
      "cmd appops set local.tclbridge ASSOC_START allow","cmd appops set --uid local.tclbridge ASSOC_START allow",
      "cmd appops set local.tclbridge RUN_ANY_IN_BACKGROUND allow","cmd appops set local.tclbridge SYSTEM_ALERT_WINDOW allow",
      "cmd appops set local.tclbridge TURN_SCREEN_ON allow","cmd appops set local.tclbridge WRITE_SETTINGS allow",
      "pm grant local.tclbridge android.permission.POST_NOTIFICATIONS","cmd deviceidle whitelist +local.tclbridge","cmd appops write-settings"};
    for(String command:commands)adb.shell(command);
    if(adb.isClosed())throw new java.io.IOException("Preparação cancelada");
    SharedPreferences settings=c.getSharedPreferences("bridge",0);
    if(standby){progress.accept("Preparando o despertar por Alexa…");adb.shell("pm grant local.tclbridge android.permission.WRITE_SECURE_SETTINGS");
      if(!settings.contains("network_standby_original_value")){String original=settings.contains("network_standby_original")?String.valueOf(settings.getInt("network_standby_original",0)):adb.shell("settings get system tcl_sys_network_standby_enabled");if(!original.matches("0|1|null"))throw new java.io.IOException("Estado de espera por rede não reconhecido");settings.edit().putString("network_standby_original_value",original).commit();}
      adb.shell("settings put system tcl_sys_network_standby_enabled 1");if(adb.isClosed())throw new java.io.IOException("Preparação cancelada");PowerCompatibility.setEnabled(c,true);
    }else{
      if(settings.contains("network_standby_original_value")){String original=settings.getString("network_standby_original_value","");if(!original.matches("0|1|null"))throw new java.io.IOException("Não foi possível recuperar o estado anterior da rede");adb.shell(original.equals("null")?"settings delete system tcl_sys_network_standby_enabled":"settings put system tcl_sys_network_standby_enabled "+original);}
      if(PowerCompatibility.enabled(c))PowerCompatibility.setEnabled(c,false);
      settings.edit().remove("network_standby_original_value").remove("network_standby_original").commit();
    }
    if(adb.isClosed())throw new java.io.IOException("Preparação cancelada");
    if(detected!=null&&saveProfile)RegistryClient.installDetected(c,detected);
    c.getSharedPreferences("bridge",0).edit().putBoolean("autostart",true).putInt("setup_permissions_version",1).commit();
    progress.accept("TV preparada. A autorização local foi encerrada.");
  }
}
