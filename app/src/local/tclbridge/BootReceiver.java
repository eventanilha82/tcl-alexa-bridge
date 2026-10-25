package local.tclbridge;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) {
    if("local.tclbridge.CONFIGURE_STANDBY".equals(i.getAction())){try{PowerCompatibility.setEnabled(c,i.getBooleanExtra("enabled",false));setResultData("standby_configuration=ok");}catch(RuntimeException e){setResultCode(1);setResultData("standby_configuration=failed; "+e.getClass().getSimpleName());}return;}
    if("local.tclbridge.DIAG".equals(i.getAction())){setResultData("enabled="+c.getSharedPreferences("bridge",0).getBoolean("enabled",false)+", autostart="+c.getSharedPreferences("bridge",0).getBoolean("autostart",true)+", profile_private="+new java.io.File(c.getFilesDir(),"device-profile.json").exists());return;}
    if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))return;
    if(c.getSharedPreferences("bridge",0).getBoolean("autostart",true)){
      RecoveryJob.schedule(c);RecoveryJob.start(c,i.getAction());
    }
  }
}
