package local.tclbridge;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Observes native callbacks without changing their payload, result code or delivery. */
final class BridgeMetrics {
 final Map<Integer,long[]> counts=new TreeMap<>();final Consumer<String> log;final Handler main=new Handler(Looper.getMainLooper());
 boolean stopped;long received,completed,failed,timeouts;
 BridgeMetrics(Consumer<String> logger){log=logger;}
 synchronized Ticket begin(int type){received++;counts.computeIfAbsent(type,k->new long[6])[0]++;Ticket t=new Ticket(type);main.postDelayed(t.timeout,30000);return t;}

 final class Ticket {
  final int type;final long start=SystemClock.elapsedRealtime();final AtomicBoolean done=new AtomicBoolean();
  final Runnable timeout;
  Ticket(int value){type=value;timeout=()->{synchronized(BridgeMetrics.this){if(stopped||done.get())return;timeouts++;counts.get(type)[3]++;}log.accept("SmartAlexa sem retorno em 30 s: tipo="+type+"; comando não reenviado");};}
  void result(int code){if(!done.compareAndSet(false,true))return;main.removeCallbacks(timeout);long elapsed=SystemClock.elapsedRealtime()-start;synchronized(BridgeMetrics.this){if(stopped)return;completed++;if(code!=0)failed++;long[] row=counts.get(type);row[1]++;if(code!=0)row[2]++;row[4]=elapsed;row[5]=Math.max(row[5],elapsed);}log.accept("Retorno SmartAlexa: tipo="+type+", código="+code+", tempo="+elapsed+" ms");}
 }
 synchronized JSONObject snapshot(){JSONObject out=new JSONObject(),types=new JSONObject();try{for(Map.Entry<Integer,long[]> item:counts.entrySet()){long[] n=item.getValue();types.put(String.valueOf(item.getKey()),new JSONObject().put("received",n[0]).put("completed",n[1]).put("failed",n[2]).put("timeout_observed",n[3]).put("last_ms",n[4]).put("max_ms",n[5]));}out.put("scope","executor_forwarded").put("forwarded",received).put("received",received).put("completed",completed).put("failed",failed).put("timeout_observed",timeouts).put("by_type",types);}catch(JSONException ignored){}return out;}
 synchronized void stop(){stopped=true;main.removeCallbacksAndMessages(null);}
}
