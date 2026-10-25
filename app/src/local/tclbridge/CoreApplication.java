package local.tclbridge;
import android.app.Application;
import android.content.Context;
import java.io.File;

/** Gives the isolated HTTP library its own cache without changing the host application's files. */
final class CoreApplication extends Application {
  CoreApplication(Context host){attachBaseContext(host);}
  @Override public Context getApplicationContext(){return this;}
  @Override public File getCacheDir(){File cache=new File(super.getCacheDir(),"tcl-core-http");if(!cache.isDirectory()&&!cache.mkdirs())throw new IllegalStateException("Cache TCL indisponível");return cache;}
}
