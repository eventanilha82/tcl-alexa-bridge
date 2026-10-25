package local.tclbridge;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import org.json.JSONObject;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/** C6K wake adapter: same UDP magic-packet format as TCL's UdpClient. */
final class WakeSupport {
  final Context context;final Consumer<String> log;final Handler main=new Handler(Looper.getMainLooper());
  volatile boolean running;DatagramSocket socket;
  WakeSupport(Context c,Consumer<String> logger){context=c;log=logger;}
  void wake(){PowerManager p=(PowerManager)context.getSystemService(Context.POWER_SERVICE);if(p.isInteractive())return;
    PowerManager.WakeLock screen=p.newWakeLock(PowerManager.FULL_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE,"TCLBridge:screen-on");screen.acquire(3000);
    if(!p.isInteractive()&&Settings.canDrawOverlays(context))context.startActivity(new Intent(context,WakeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));
  }
  void start(JSONObject profile){
    final List<byte[]> addresses=new ArrayList<>();Matcher m=Pattern.compile("(?i)[0-9a-f]{2}(?::[0-9a-f]{2}){5}").matcher(profile.optString("mac"));
    while(m.find()){String[] parts=m.group().split(":");byte[] mac=new byte[6];for(int i=0;i<6;i++)mac[i]=(byte)Integer.parseInt(parts[i],16);addresses.add(mac);}
    if(addresses.isEmpty()){log.accept("Despertar por rede: endereço não configurado");return;}
    running=true;Thread thread=new Thread(()->{
      long lastWake=0;boolean bindLogged=false;
      while(running){try{socket=new DatagramSocket(1025);bindLogged=false;log.accept("Despertar TCL por UDP disponível");
        while(running){byte[] bytes=new byte[1024];DatagramPacket packet=new DatagramPacket(bytes,bytes.length);socket.receive(packet);
          if(packet.getLength()<=12)continue;boolean prefix=true;for(int i=0;i<6;i++)prefix&=bytes[i]==(byte)255;if(!prefix)continue;
          byte[] target=Arrays.copyOfRange(bytes,6,12);boolean own=false;for(byte[] mac:addresses)own|=Arrays.equals(mac,target);
          long now=SystemClock.elapsedRealtime();if(own&&now-lastWake>1500){lastWake=now;log.accept("Pacote de despertar TCL recebido para esta TV");main.post(()->{if(running)try{wake();}catch(Exception e){log.accept("Despertar indisponível: "+e.getClass().getSimpleName());}});}
        }
      }catch(BindException e){if(!bindLogged){log.accept("Despertar TCL: aguardando liberação da porta UDP");bindLogged=true;}}
      catch(Exception e){if(running)log.accept("Despertar UDP: "+e.getClass().getSimpleName());}
      finally{DatagramSocket active=socket;if(active!=null)active.close();}
      if(running)try{Thread.sleep(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
      }
    },"TCL-wake-adapter");thread.setDaemon(true);thread.start();
  }
  void stop(){running=false;if(socket!=null)socket.close();main.removeCallbacksAndMessages(null);}
}
