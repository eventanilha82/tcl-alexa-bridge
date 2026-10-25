package local.tclbridge;
import android.app.job.*;
import android.content.*;
import android.util.Log;

/** System-persisted recovery independent of the launcher activity. */
public final class RecoveryJob extends JobService {
  static void schedule(Context c){
    JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if(!c.getSharedPreferences("bridge",0).getBoolean("autostart",true)){scheduler.cancel(645031);return;}
    scheduler.schedule(new JobInfo.Builder(645031,new ComponentName(c,RecoveryJob.class)).setPeriodic(15*60*1000L).setPersisted(true).build());
  }
  static void start(Context c,String reason){
    if(!c.getSharedPreferences("bridge",0).getBoolean("enabled",false))return;
    try{c.startForegroundService(new Intent(c,BridgeService.class).setAction("RECOVER").putExtra("reason",reason));}
    catch(RuntimeException e){Log.w("TCLBridge","Recuperação adiada: "+e.getClass().getSimpleName());}
  }
  @Override public boolean onStartJob(JobParameters parameters){start(this,"verificação do sistema");jobFinished(parameters,false);return false;}
  @Override public boolean onStopJob(JobParameters parameters){return true;}
}
