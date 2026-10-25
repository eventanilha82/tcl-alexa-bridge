package local.tclbridge;
import android.content.*;import android.os.PowerManager;import android.provider.Settings;

/** Global standby change is opt-in; preserve the user's original value. */
final class PowerCompatibility {
 static boolean enabled(Context c){return c.getSharedPreferences("bridge",0).getBoolean("standby_compatibility",false);}
 static void setEnabled(Context c,boolean enabled){
  SharedPreferences p=c.getSharedPreferences("bridge",0);PowerManager pm=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
  if(enabled){
   if(c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Permissão ADB de energia ainda não concedida");
   int original=p.contains("standby_original")?p.getInt("standby_original",1):(pm.isLowPowerStandbyEnabled()?1:0);
   if(!Settings.Global.putInt(c.getContentResolver(),"low_power_standby_enabled",0))throw new IllegalStateException("A TV não aceitou a configuração de energia");
   p.edit().putInt("standby_original",original).putBoolean("standby_compatibility",true).commit();
  }else{
   if(p.contains("standby_original")&&!Settings.Global.putInt(c.getContentResolver(),"low_power_standby_enabled",p.getInt("standby_original",1)))throw new IllegalStateException("Não foi possível restaurar a energia");
   p.edit().putBoolean("standby_compatibility",false).remove("standby_original").commit();
  }
 }
 static void apply(Context c){if(enabled(c)&&((PowerManager)c.getSystemService(Context.POWER_SERVICE)).isLowPowerStandbyEnabled()){
  if(!Settings.Global.putInt(c.getContentResolver(),"low_power_standby_enabled",0))throw new IllegalStateException("Configuração de energia recusada");
 }}
}
