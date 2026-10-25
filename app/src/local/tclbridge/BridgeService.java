package local.tclbridge;
import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Hosts the installed TCL session engine; no media/control reimplementation. */
public class BridgeService extends Service {
  static final String TAG="TCLBridge",HOST="https://ai.tclking.com";
  final Handler main=new Handler(Looper.getMainLooper());
  final ScheduledExecutorService net=Executors.newSingleThreadScheduledExecutor();
  final LinkedList<String> events=new LinkedList<>();
  volatile EmbeddedCore core;volatile boolean alive=true,initializing=false,linked=false;
  volatile boolean draining,resumeRequested,drainFinalizing,drainUncertain,interruptedAuth,wakeStarting;
  volatile long drainStarted;Intent afterDrain;int latestStartId;String drainError="";
  volatile JSONObject credentials;WakeSupport wakeSupport;PowerManager.WakeLock wake;
  boolean waitingCode=false;long codeExpiresAt=0,lastLinkCheck=0,nextInit=0;
  String powerError="";volatile String configurationError="",setupError="";
  volatile long disconnectedSince=0,lastRecovery=0;long lastExecutorCheck;volatile long generation;ConnectivityManager.NetworkCallback networkCallback;
  String savedCredentials="";String lastEngineState="";

  void promote(){
    NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    nm.createNotificationChannel(new NotificationChannel("bridge","Controle da TV por Alexa",NotificationManager.IMPORTANCE_LOW));
    PendingIntent pi=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
    startForeground(1,new Notification.Builder(this,"bridge").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Alexa na TV").setContentText("Serviços TCL em execução").setContentIntent(pi).setOngoing(true).build());
  }
  @Override public void onCreate(){super.onCreate();promote();
    applyPowerCompatibility();
    wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TCLBridge:connection");wake.acquire();
    ensureWakeSupport();
    RecoveryJob.schedule(this);
    ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);networkCallback=new ConnectivityManager.NetworkCallback(){@Override public void onAvailable(Network network){disconnectedSince=0;main.postDelayed(()->{if(alive&&!draining&&core!=null&&!core.connected())recoverSocket();},1500);}};cm.registerDefaultNetworkCallback(networkCallback);
    event("Companion iniciado — modo TTVSCore");net.scheduleWithFixedDelay(()->tick(),0,1,TimeUnit.SECONDS);
  }
  @Override public int onStartCommand(Intent intent,int flags,int id){
    promote();latestStartId=id;
    String action=intent==null?"START":intent.getAction();
    if("PAUSE".equals(action)){requestPause();return START_NOT_STICKY;}
    if("RECOVER".equals(action)&&!getSharedPreferences("bridge",0).getBoolean("enabled",false)){if(!draining)stopSelf(id);return START_NOT_STICKY;}
    getSharedPreferences("bridge",0).edit().putBoolean("enabled",true).apply();
    if(draining){boolean already=resumeRequested;resumeRequested=true;if(afterDrain==null||!"START".equals(action)&&!"RECOVER".equals(action))afterDrain=intent==null?new Intent(this,BridgeService.class).setAction("START"):new Intent(intent);if(!already)event("Retomada aguardando a renovação em andamento");writeState();return START_STICKY;}
    ensureWakeSupport();dispatchAction(intent,action);return START_STICKY;
  }
  void dispatchAction(Intent intent,String action){
    if("RECOVER".equals(action))event("Inicialização automática: "+intent.getStringExtra("reason"));
    if("LINK".equals(action)||"RESUME_LINK".equals(action)){setupError="";writeState();net.execute(()->startLink("RESUME_LINK".equals(action)));}
    if("RECONNECT".equals(action))main.post(()->{try{if(core!=null&&!draining){core.refresh();event("Renovação solicitada ao TTVSCore");}else nextInit=0;}catch(Exception e){event("Reconexão TCL: "+root(e).getClass().getSimpleName());}});
    if("SELF_TEST".equals(action))testVolume();
    if("NATIVE_STATUS".equals(action))new TtvsProbe(this,text->event(text)).start();
    if("SCREEN_CYCLE_TEST".equals(action))screenCycleTest();
    if("LOGOUT".equals(action))net.execute(()->logout());
    if("BIND_GROUP".equals(action)&&intent.hasExtra("code")){String code=intent.getStringExtra("code");net.execute(()->bindGroup(code));}
    if("UPDATE_PROFILE".equals(action))net.execute(()->updateProfile());
  }
  void ensureWakeSupport(){
    if(wakeStarting||wakeSupport!=null&&wakeSupport.running||!getSharedPreferences("bridge",0).getBoolean("enabled",false))return;
    wakeStarting=true;WakeSupport candidate=new WakeSupport(this,text->event(text));wakeSupport=candidate;long expectedGeneration=generation;
    net.execute(()->{try{JSONObject profile=RegistryClient.profile(this);main.post(()->{try{if(alive&&!draining&&expectedGeneration==generation&&wakeSupport==candidate&&getSharedPreferences("bridge",0).getBoolean("enabled",false))candidate.start(profile);}finally{wakeStarting=false;}});}catch(Exception e){wakeStarting=false;configurationError=e.getMessage()==null?"Configure o perfil desta TV":e.getMessage();event("Perfil da TV pendente de configuração");}});
  }
  void requestPause(){
    getSharedPreferences("bridge",0).edit().putBoolean("enabled",false).apply();generation++;activationRequestPending=false;resumeRequested=false;afterDrain=null;
    if(!draining){draining=true;drainStarted=SystemClock.elapsedRealtime();drainUncertain=false;drainError="";event("Controle pausado; aguardando operações de autenticação em andamento");}
    EmbeddedCore active=core;if(active!=null)active.quiesce();if(wakeSupport!=null)wakeSupport.stop();writeState();
  }
  void drainTick(){
    EmbeddedCore retiring=core;int pending=retiring==null?0:retiring.pendingSubscriptions();
    if(pending>0){if(!drainUncertain&&SystemClock.elapsedRealtime()-drainStarted>=10000){drainUncertain=true;event("Renovação ainda em andamento; estado incerto, sem cancelar tokens");}writeState();return;}
    if(drainFinalizing){writeState();return;}
    try{
      // No auth callback is pending: persist the final snapshot, then reload the native file.
      if(retiring!=null){credentials=retiring.credentials();saveCredentials();}
      credentials=null;readCredentials();saveCredentials();drainFinalizing=true;
      main.post(()->finishDrain(retiring));
    }catch(Exception e){String error=root(e).getClass().getSimpleName();if(!error.equals(drainError)){drainError=error;event("Estado da conta ainda não salvo: "+error);}drainUncertain=true;writeState();}
  }
  void finishDrain(EmbeddedCore retiring){
    if(!alive||!draining||core!=retiring){drainFinalizing=false;return;}
    if(retiring!=null&&retiring.pendingSubscriptions()!=0){drainFinalizing=false;return;}
    if(retiring!=null)retiring.stop();core=null;activationRequestPending=false;initializing=false;drainFinalizing=false;draining=false;drainUncertain=false;drainStarted=0;
    boolean resume=resumeRequested&&getSharedPreferences("bridge",0).getBoolean("enabled",false);Intent next=afterDrain;resumeRequested=false;afterDrain=null;
    event("Estado da conta salvo após a pausa");writeState();
    if(resume){nextInit=0;lastLinkCheck=0;disconnectedSince=0;lastEngineState="";ensureWakeSupport();event("Retomando com o estado privado atualizado");dispatchAction(next,next==null?"START":next.getAction());}
    else stopSelf(latestStartId);
  }
  synchronized void event(String text){
    Log.i(TAG,text);events.addFirst(new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date())+"  "+text);while(events.size()>35)events.removeLast();
    try{StringBuilder s=new StringBuilder();for(String e:events)s.append(e).append('\n');Files.write(new File(getFilesDir(),"status.txt").toPath(),s.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
  }
  static Throwable root(Throwable error){while(error instanceof java.lang.reflect.InvocationTargetException&&error.getCause()!=null)error=error.getCause();return error;}
  void readCredentials()throws Exception{
    if(credentials!=null)return;
    File nativeFile=new File(getFilesDir(),"aa/.alexa.db");
    if(nativeFile.exists())try{JSONObject state=new JSONObject(new String(android.util.Base64.decode(Files.readAllBytes(nativeFile.toPath()),android.util.Base64.DEFAULT),StandardCharsets.UTF_8));
      if(!state.optString("tcl_refreshtoken").isEmpty())credentials=new JSONObject().put("uuid",state.getString("uuid")).put("access_token",state.getString("tcl_token")).put("refresh_token",state.getString("tcl_refreshtoken")).put("expires_at",state.getLong("tcl_token_expires_at"));
    }catch(Exception ignored){}
    File file=new File(getFilesDir(),"credentials.json");if(credentials==null&&file.exists())credentials=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
    File inbox=new File(getExternalFilesDir(null),"bootstrap.json");
    if(credentials==null&&inbox.exists()){
      credentials=new JSONObject(new String(Files.readAllBytes(inbox.toPath()),StandardCharsets.UTF_8));
      for(String field:new String[]{"uuid","access_token","refresh_token"})if(credentials.optString(field).isEmpty())throw new IOException("Incomplete configuration");
      saveCredentials();if(!inbox.delete())throw new IOException("Cannot delete bootstrap");event("Vínculo importado para armazenamento privado");
    }
  }
  void saveCredentials()throws Exception{
    if(credentials==null||credentials.optString("uuid").isEmpty()||credentials.optString("access_token").isEmpty()||credentials.optString("refresh_token").isEmpty())return;String data=credentials.toString();if(data.equals(savedCredentials))return;
    File temp=new File(getFilesDir(),"credentials.tmp"),file=new File(getFilesDir(),"credentials.json");
    try(FileOutputStream out=new FileOutputStream(temp)){out.write(data.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
    if(!temp.renameTo(file))throw new IOException("Cannot persist credentials");savedCredentials=data;
  }
  void applyPowerCompatibility(){try{PowerCompatibility.apply(this);}catch(RuntimeException e){String message=e.getMessage();if(!java.util.Objects.equals(powerError,message)){powerError=message;event("Permissão de energia pendente");}}}
  void tick(){if(!alive)return;if(draining){drainTick();return;}if(!getSharedPreferences("bridge",0).getBoolean("enabled",false)){writeState();return;}applyPowerCompatibility();
    try{
      EmbeddedCore active=core;
      if(active!=null){active.installWakeAdapter(wakeSupport);
        if(!active.executorConnected()&&SystemClock.elapsedRealtime()-lastExecutorCheck>5000){lastExecutorCheck=SystemClock.elapsedRealtime();main.post(()->{try{if(core==active)active.recoverExecutor();}catch(Exception e){event("Executor SmartAlexa: "+root(e).getClass().getSimpleName());}});}JSONObject snapshot=active.credentials();if(!snapshot.optString("uuid").isEmpty()&&!snapshot.optString("access_token").isEmpty()&&!snapshot.optString("refresh_token").isEmpty()){credentials=snapshot;saveCredentials();}linked=active.accountLinked();if(linked&&waitingCode){waitingCode=false;new File(getFilesDir(),"link-code.json").delete();}
        if(active.connected())disconnectedSince=0;else{if(disconnectedSince==0)disconnectedSince=SystemClock.elapsedRealtime();if(linked&&SystemClock.elapsedRealtime()-disconnectedSince>30000&&SystemClock.elapsedRealtime()-lastRecovery>30000)main.post(()->recoverSocket());}
        String status=active.connected()+":"+active.executorConnected()+":"+active.persisted();
        if(!status.equals(lastEngineState)){lastEngineState=status;event("TTVSCore: nuvem="+active.connected()+", executor TCL="+active.executorConnected()+", estado salvo="+active.persisted());}
        writeState();return;
      }
      readCredentials();if(credentials==null){writeState();return;}
      if(lastLinkCheck==0||waitingCode&&System.currentTimeMillis()-lastLinkCheck>5000)checkLink();
      if(linked&&!draining&&!initializing&&System.currentTimeMillis()>=nextInit){
        initializing=true;long expectedGeneration=generation;
        // Prepare the runtime copy off the UI thread.
        EmbeddedCore.awaitResourceCleanup();
      CoreArchive.prepare(this,getPackageManager().getApplicationInfo("com.tcl.ttvs",0).sourceDir);
        JSONObject tokenCopy=new JSONObject(credentials.toString()),profile=RegistryClient.profile(this);configurationError="";
        main.post(()->{
          if(!alive||draining||!getSharedPreferences("bridge",0).getBoolean("enabled",false)||expectedGeneration!=generation){initializing=false;return;}
          EmbeddedCore candidate=new EmbeddedCore(this,text->event(text));
          try{candidate.start(tokenCopy,profile);core=candidate;event("Motor original TCL ativo");}
          catch(Throwable e){candidate.stop();Throwable cause=root(e);Log.e(TAG,"TCL engine initialization",cause);event("Motor TCL indisponível: "+cause.getClass().getSimpleName());nextInit=System.currentTimeMillis()+60000;}
          finally{initializing=false;}
        });
      }
      writeState();
    }catch(Exception e){initializing=false;nextInit=System.currentTimeMillis()+30000;event("Inicialização TCL: "+root(e).getClass().getSimpleName());}
  }
  JSONObject request(String method,String path,JSONObject body)throws Exception{
    HttpsURLConnection c=(HttpsURLConnection)new URL(HOST+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
    try{try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
      if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
      try(InputStream in=c.getInputStream();ByteArrayOutputStream data=new ByteArrayOutputStream()){
        byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(data.size()+n>1048576)throw new IOException("Response too large");data.write(b,0,n);}return new JSONObject(data.toString("UTF-8"));}
    }finally{c.disconnect();}
  }
  JSONObject post(String path,JSONObject body)throws Exception{return request("POST",path,body);}
  void requireSuccess(JSONObject result)throws IOException{if(result.optInt("error_code",-1)!=0)throw new IOException("TCL "+result.optInt("error_code",-1));}
  void storeRegistration(JSONObject result)throws Exception{
    requireSuccess(result);configurationError="";credentials=new JSONObject().put("uuid",result.getString("uuid")).put("access_token",result.getString("access_token")).put("refresh_token",result.getString("refresh_token")).put("expires_at",System.currentTimeMillis()/1000+result.optLong("expires_in",3600));saveCredentials();
  }
  volatile boolean activationRequestPending;
  void startLink(boolean resumeOnly){
    if(activationRequestPending&&(core==null||core.pendingSubscriptions()>0))return;
    activationRequestPending=true;setupError="";writeState();
    try{
      EmbeddedCore current=core;
      if(current!=null){
        if(resumeOnly){activationRequestPending=false;return;}
        if(current.accountLinked()){activationRequestPending=false;linked=true;event("Conta Alexa já vinculada a esta TV");writeState();return;}
        main.post(()->{try{if(alive&&!draining&&core==current)current.requestActivation();}catch(Exception e){activationRequestPending=false;setupError="A TCL não conseguiu gerar o código. Tente novamente.";writeState();}});return;
      }
      if(initializing){activationRequestPending=false;return;}
      initializing=true;long expectedGeneration=generation;readCredentials();
      EmbeddedCore.awaitResourceCleanup();
      CoreArchive.prepare(this,getPackageManager().getApplicationInfo("com.tcl.ttvs",0).sourceDir);
      JSONObject profile=RegistryClient.profile(this),initial=credentials==null?null:new JSONObject(credentials.toString());configurationError="";
      long resumeExpiry=0;if(resumeOnly)try{JSONObject code=new JSONObject(new String(Files.readAllBytes(new File(getFilesDir(),"link-code.json").toPath()),StandardCharsets.UTF_8));resumeExpiry=code.getLong("expires_at");}catch(Exception ignored){}final long restoredExpiry=resumeExpiry;
      main.post(()->{
        if(!alive||draining||expectedGeneration!=generation){initializing=false;activationRequestPending=false;return;}
        EmbeddedCore candidate=new EmbeddedCore(this,text->event(text));
        try{candidate.startActivation(initial,profile,(type,value)->nativeActivation(candidate,type,value),restoredExpiry);core=candidate;activationRequestPending=!resumeOnly;event(resumeOnly?"Vínculo em andamento retomado pelo núcleo TCL":"Registro e código delegados ao núcleo TCL original");}
        catch(Exception|LinkageError e){candidate.stop();activationRequestPending=false;setupError="O núcleo TCL não conseguiu iniciar o vínculo nesta TV.";event(setupError);}
        finally{initializing=false;writeState();}
      });
    }catch(Exception e){initializing=false;activationRequestPending=false;setupError="Não foi possível preparar o vínculo. Revise a configuração da TV e tente novamente.";event(setupError);writeState();}
  }
  void nativeActivation(EmbeddedCore source,int type,String value){
    if(!alive||draining)return;
    try{net.execute(()->{if(!alive||draining||core!=source)return;
      try{
        if(type==33){activationRequestPending=false;
          if(value!=null&&value.matches("[0-9]{6}")){
            setupError="";codeExpiresAt=source.activationExpiresAt>System.currentTimeMillis()/1000?source.activationExpiresAt:System.currentTimeMillis()/1000+180;waitingCode=true;
            writeJson("link-code.json",new JSONObject().put("code",value).put("expires_at",codeExpiresAt));event("Código de ativação recebido do núcleo TCL");
          }else{setupError="A TCL não forneceu um código. Confira a rede e tente novamente.";event(setupError);}
        }
        if(type==34){waitingCode=false;new File(getFilesDir(),"link-code.json").delete();event("Código expirado. Gere outro para continuar.");}
        if(type==400){linked=true;waitingCode=false;new File(getFilesDir(),"link-code.json").delete();event("Vínculo confirmado pelo núcleo TCL");}
        if(type==1000){activationRequestPending=false;setupError="O registro TCL não foi concluído. Confira a rede e tente novamente.";event(setupError);}
        writeState();
      }catch(Exception e){activationRequestPending=false;setupError="Não foi possível salvar o estado do vínculo.";event(setupError);writeState();}
    });}catch(RejectedExecutionException ignored){}
  }
  void checkLink()throws Exception{
    JSONObject profile=RegistryClient.profile(this);configurationError="";JSONObject result=post("/api/v1/oauth/avgState",new JSONObject().put("deviceId",profile.getString("device_id")).put("mac",profile.getString("mac")));requireSuccess(result);
    boolean previous=linked;linked="enable".equalsIgnoreCase(result.optString("avg_status"));lastLinkCheck=System.currentTimeMillis();
    if(linked){waitingCode=false;new File(getFilesDir(),"link-code.json").delete();if(!previous)event("Vínculo Alexa confirmado pela TCL");}
    else if(waitingCode&&System.currentTimeMillis()/1000>=codeExpiresAt){waitingCode=false;new File(getFilesDir(),"link-code.json").delete();event("Código expirado. Gere outro em Vincular.");}
  }
  void bindGroup(String code){try{if(code==null||!code.matches("[0-9]{6}"))throw new IOException("Código deve conter seis dígitos");readCredentials();if(credentials==null)throw new IOException("Vincule esta TV primeiro");
    JSONObject result=post("/api/v1/group",new JSONObject().put("access_token",credentials.getString("access_token")).put("code",code).put("version_number",1));requireSuccess(result);event("Vínculo adicional aceito pela TCL");
  }catch(Exception e){event("Adicionar TV: "+e.getMessage());}}
  void updateProfile(){try{readCredentials();if(credentials==null)throw new IOException("TV sem vínculo");JSONObject profile=RegistryClient.profile(this);
    profile.put("version_number",1).put("access_token",credentials.getString("access_token")).put("sign_key","");requireSuccess(request("PUT","/api/v1/device",profile));event("Dados da TV atualizados na TCL");
  }catch(Exception e){event("Atualizar dados: "+e.getMessage());}}
  void logout(){try{readCredentials();if(credentials==null)return;
    JSONObject result=post("/api/v1/device/signoutWithRemove",new JSONObject().put("access_token",credentials.getString("access_token")).put("version_number",1));requireSuccess(result);
    generation++;draining=false;resumeRequested=false;afterDrain=null;EmbeddedCore old=core;core=null;if(old!=null){CountDownLatch stopped=new CountDownLatch(1);main.post(()->{try{old.stop();}finally{stopped.countDown();}});if(!stopped.await(5,TimeUnit.SECONDS))throw new IOException("Executor ainda encerrando");}credentials=null;savedCredentials="";linked=false;waitingCode=false;lastLinkCheck=0;
    getSharedPreferences("bridge",0).edit().remove("setup_complete").commit();
    new File(getFilesDir(),"credentials.json").delete();new File(getFilesDir(),"aa/.alexa.db").delete();new File(getFilesDir(),"link-code.json").delete();event("Conta desvinculada pela TCL");writeState();
  }catch(Exception e){event("Desvincular: "+e.getMessage());}}
  void testVolume(){try{if(core==null||draining){event("Aguarde o motor TCL conectar");return;}core.testVolume();event("Teste de volume enviado ao motor original TCL");}catch(Exception e){event("Teste TCL: "+root(e).getClass().getSimpleName());}}
  void screenCycleTest(){if(core==null||draining){event("Motor TCL indisponível para o teste");return;}
    try{core.command(new JSONObject().put("msg_type",6).put("keyword","PowerCtrl").put("power","off"));event("Teste: desligamento enviado ao executor TCL");
      main.postDelayed(()->{if(!alive||draining)return;event("Teste: tela desligada="+!((PowerManager)getSystemService(POWER_SERVICE)).isInteractive());try{wakeSupport.wake();}catch(Exception e){event("Teste de despertar: "+e.getClass().getSimpleName());}},2500);
      main.postDelayed(()->{if(alive&&!draining)event("Teste: tela ligada="+((PowerManager)getSystemService(POWER_SERVICE)).isInteractive());},5500);
    }catch(Exception e){event("Teste de energia TCL: "+e.getClass().getSimpleName());}
  }
  void recoverSocket(){if(draining||core==null||core.connected())return;lastRecovery=SystemClock.elapsedRealtime();try{core.reconnect();event("Recuperação automática da conexão TCL");}catch(Exception e){event("Recuperação TCL: "+root(e).getClass().getSimpleName());}}
  @Override public void onTaskRemoved(Intent intent){RecoveryJob.schedule(this);event("Tela fechada; controle permanece em segundo plano");}
  void writeJson(String name,JSONObject value)throws Exception{File temp=new File(getFilesDir(),name+".tmp"),target=new File(getFilesDir(),name);try(FileOutputStream out=new FileOutputStream(temp)){out.write(value.toString().getBytes(StandardCharsets.UTF_8));}if(!temp.renameTo(target))throw new IOException("State commit failed");}
  void writeState(){try{EmbeddedCore active=core;writeJson("state.json",new JSONObject().put("configured",credentials!=null).put("updated_at",System.currentTimeMillis()).put("service_running",alive).put("linked",linked).put("connected",active!=null&&active.connected()).put("executor",active!=null&&active.executorConnected()).put("persisted",active!=null&&active.persisted()).put("draining",draining).put("pending_subscriptions",active==null?0:active.pendingSubscriptions()).put("resume_requested",resumeRequested).put("drain_uncertain",drainUncertain).put("interrupted_auth",interruptedAuth).put("configuration_error",configurationError).put("setup_error",setupError).put("standby_compatibility",PowerCompatibility.enabled(this)).put("mode","ttvscore_smartalexa_bridge").put("metrics",active==null?new JSONObject():active.metrics.snapshot()).put("standby_exempt",Build.VERSION.SDK_INT<34||((PowerManager)getSystemService(POWER_SERVICE)).isExemptFromLowPowerStandby()).put("network_standby",Settings.System.getInt(getContentResolver(),"tcl_sys_network_standby_enabled",0)==1).put("enabled",getSharedPreferences("bridge",0).getBoolean("enabled",false)));}catch(Exception ignored){}}
  @Override public IBinder onBind(Intent i){return null;}
  @Override protected synchronized void dump(FileDescriptor fd,PrintWriter out,String[] args){
    if(args!=null&&args.length>0&&"test-pause".equals(args[0])){main.post(()->requestPause());out.println("Pausa assíncrona solicitada");return;}
    if(args!=null&&args.length>0&&"test-start".equals(args[0])){main.post(()->onStartCommand(new Intent(this,BridgeService.class).setAction("START"),0,latestStartId));out.println("Retomada solicitada");return;}
    if(args!=null&&args.length>0&&"test-refresh-pause".equals(args[0])){main.post(()->{try{if(core!=null&&!draining)core.refresh();requestPause();}catch(Exception e){event("Teste de pausa: "+root(e).getClass().getSimpleName());}});out.println("Renovação e pausa imediata solicitadas");return;}
    if(args!=null&&args.length>0&&"test-profile".equals(args[0])){try{RegistryClient.profile(this);out.println("profile_valid=true; private_profile="+new File(getFilesDir(),"device-profile.json").exists());}catch(Exception e){out.println("profile_valid=false; "+e.getMessage());}return;}
    if(args!=null&&args.length>0&&"test-native-signer".equals(args[0])){try{out.println(NativeSigner.dryRun(this));}catch(Exception e){out.println("native_signer_error="+e.getMessage());}return;}
    if(args!=null&&args.length>0&&"test-refresh".equals(args[0])){main.post(()->{try{if(core!=null&&!draining){core.refresh();event("Teste: renovação nativa solicitada");}}catch(Exception e){event("Renovação nativa: "+root(e).getClass().getSimpleName());}});out.println("Renovação nativa agendada");return;}
    if(args!=null&&args.length>=2&&"test-command".equals(args[0])){final String json=args[1];main.post(()->{try{if(core!=null&&!draining){JSONObject data=new JSONObject(json);core.command(data);event("Teste enviado: tipo="+data.optInt("msg_type")+", ação="+data.optString("keyword"));}}catch(Exception e){event("Teste: "+root(e).getClass().getSimpleName());}});out.println("Teste agendado no motor nativo");return;}
    if(args!=null&&args.length>0&&"test-disconnect".equals(args[0])){main.post(()->{try{core.disconnectForTest();event("Teste: socket fechado para verificar recuperação automática");}catch(Exception e){event("Teste de conexão falhou");}});out.println("Teste de recuperação agendado");return;}
    if(args!=null&&args.length>0&&"test-executor-disconnect".equals(args[0])){main.post(()->{try{core.disconnectExecutorForTest();event("Teste: executor desconectado");}catch(Exception e){event("Teste de executor indisponível");}});out.println("Teste de recuperação do executor agendado");return;}
    out.println("Alexa na TV 0.4.0 — TTVSCore + SmartAlexa oficiais");EmbeddedCore active=core;
    out.println("cloud_connected="+(active!=null&&active.connected()));out.println("tcl_executor_connected="+(active!=null&&active.executorConnected()));
    out.println("native_state_persisted="+(active!=null&&active.persisted()));out.println("account_linked="+linked);out.println("standby_exempt="+(Build.VERSION.SDK_INT<34||((PowerManager)getSystemService(POWER_SERVICE)).isExemptFromLowPowerStandby()));out.println("standby_compatibility="+getSharedPreferences("bridge",0).getBoolean("standby_compatibility",false));out.println("autostart="+getSharedPreferences("bridge",0).getBoolean("autostart",true));
    out.println("draining="+draining);out.println("pending_subscriptions="+(active==null?0:active.pendingSubscriptions()));out.println("resume_requested="+resumeRequested);out.println("drain_uncertain="+drainUncertain);
    out.println("forwarding=all_native_messages");out.println("metrics="+(active==null?"{}":active.metrics.snapshot()));
    for(String event:events)out.println(event);
  }
  @Override public void onDestroy(){interruptedAuth=core!=null&&core.pendingSubscriptions()>0;if(interruptedAuth)event("Android encerrou durante autenticação; resposta do servidor pode estar incerta");alive=false;generation++;if(networkCallback!=null)try{((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(networkCallback);}catch(Exception ignored){}main.removeCallbacksAndMessages(null);if(core!=null){core.stop();core=null;}if(wakeSupport!=null)wakeSupport.stop();net.shutdownNow();
    if(wake!=null&&wake.isHeld())wake.release();writeState();event("Companion encerrado");super.onDestroy();
  }
}
