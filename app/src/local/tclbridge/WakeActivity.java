package local.tclbridge;
import android.app.Activity;
import android.os.*;
import android.view.WindowManager;
public class WakeActivity extends Activity {
  @Override public void onCreate(Bundle b){super.onCreate(b);setShowWhenLocked(true);setTurnScreenOn(true);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);new Handler().postDelayed(()->finish(),1200);
  }
}
