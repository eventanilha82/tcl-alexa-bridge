package local.tclbridge;
import android.app.Application;
import android.content.*;
import android.os.*;
import android.media.AudioManager;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;

/** Loads the installed TCL backend under the companion UID with private storage mapping. */
final class EmbeddedCore {
  static final ExecutorService resourceCleanup=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"TCLBridge-core-cleanup");thread.setDaemon(true);return thread;});
  static volatile Future<?> lastCleanup;
  static void awaitResourceCleanup()throws Exception{Future<?> pending=lastCleanup;if(pending!=null)pending.get(15,TimeUnit.SECONDS);}
  volatile Object wrappedInvoke,originalInvoke;final BridgeMetrics metrics;long lastExecutorRecovery;
  volatile boolean stopped,quiesced;WakeSupport wakeAdapter;
  final Map<ServiceConnection,ServiceConnection> bindings=Collections.synchronizedMap(new IdentityHashMap<>());
  final Set<Object> subscriptions=Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
  Class<?> subscriptionType;
  final AtomicBoolean heartbeatRunning=new AtomicBoolean();
  final ExecutorService heartbeatWorker=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"TCLBridge-native-reconnect");thread.setDaemon(true);return thread;});
  final Context host;final Consumer<String> report;Object manager;Class<?> managerClass;ClassLoader loader;
  BiConsumer<Integer,String> activationEvents;volatile long activationExpiresAt;
  final List<BroadcastReceiver> receivers=new ArrayList<>(),originalReceivers=new ArrayList<>();
  EmbeddedCore(Context c,Consumer<String> logger){host=c;report=logger;metrics=new BridgeMetrics(logger);wakeAdapter=new WakeSupport(c,logger);lastExecutorRecovery=SystemClock.elapsedRealtime();}
  void start(JSONObject credentials,JSONObject profile)throws Exception{
    initialize(credentials,profile,false,0);
  }
  void startActivation(JSONObject credentials,JSONObject profile,BiConsumer<Integer,String> events)throws Exception{
    startActivation(credentials,profile,events,0);
  }
  void startActivation(JSONObject credentials,JSONObject profile,BiConsumer<Integer,String> events,long resumeExpiresAt)throws Exception{
    activationEvents=events;initialize(credentials,profile,true,resumeExpiresAt);
  }
  void initialize(JSONObject credentials,JSONObject profile,boolean activation,long resumeExpiresAt)throws Exception{
    if(stopped||quiesced)throw new IllegalStateException("Core already stopped");
    Context packageContext=host.createPackageContext("com.tcl.ttvs",Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
    File runtime=CoreArchive.prepare(host,host.getPackageManager().getApplicationInfo("com.tcl.ttvs",0).sourceDir);
    tclx.os.SystemProperties.resetAlexa(!activation);
    loader=new IsolatedCoreLoader(runtime.getAbsolutePath(),packageContext.getClassLoader(),host.getClassLoader());
    Context wrapped=new ContextWrapper(host){
      @Override public Context getApplicationContext(){return this;}
      @Override public ClassLoader getClassLoader(){return loader;}
      @Override public boolean bindService(Intent intent,ServiceConnection connection,int flags){
        if(stopped||quiesced)return false;
        ServiceConnection relay=new ServiceConnection(){
          @Override public void onServiceConnected(ComponentName name,IBinder binder){
            if(stopped||quiesced){bindings.remove(connection);try{host.unbindService(this);}catch(IllegalArgumentException ignored){}return;}
            IBinder delivery=binder;try{
              // Return a local IAlexaInvoke facade so even the first queued command
              // traverses the observer before the native callback exposes the handler.
              Class<?> iface=loader.loadClass("com.tcl.ttvs.alexa.IAlexaInvoke");
              Object target=loader.loadClass("com.tcl.ttvs.alexa.IAlexaInvoke$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
              Object observed=wrapInvoke(target,iface);Binder facade=new Binder();
              facade.attachInterface((IInterface)observed,"com.tcl.ttvs.alexa.IAlexaInvoke");
              delivery=facade;
            }catch(Exception e){report.accept("Observador SmartAlexa indisponível: "+e.getClass().getSimpleName());}
            connection.onServiceConnected(name,delivery);
          }
          @Override public void onServiceDisconnected(ComponentName name){if(!stopped&&!quiesced)connection.onServiceDisconnected(name);}
          @Override public void onBindingDied(ComponentName name){if(!stopped&&!quiesced)connection.onBindingDied(name);}
          @Override public void onNullBinding(ComponentName name){if(!stopped&&!quiesced)connection.onNullBinding(name);}
        };
        bindings.put(connection,relay);
        try{boolean bound=host.bindService(intent,relay,flags);if(!bound)bindings.remove(connection);if(bound&&(stopped||quiesced)){bindings.remove(connection);try{host.unbindService(relay);}catch(IllegalArgumentException ignored){}return false;}return bound;}
        catch(RuntimeException e){bindings.remove(connection);throw e;}
      }
      @Override public void unbindService(ServiceConnection connection){unbindConnection(connection);}
      @Override public Intent registerReceiver(BroadcastReceiver receiver,IntentFilter filter){
        if(stopped||quiesced)return null;
        if(originalReceivers.contains(receiver))return null;originalReceivers.add(receiver);
        BroadcastReceiver relay=new BroadcastReceiver(){public void onReceive(Context context,Intent intent){if(stopped||quiesced)return;String action=intent.getAction();
          // The platform's private wake/standby APIs require TCL's system UID.
          // The companion uses its own wake adapter and leaves the native session alive.
          if(Intent.ACTION_SCREEN_ON.equals(action)||Intent.ACTION_SCREEN_OFF.equals(action)){
            try{managerClass.getMethod("setPowerStatus",String.class).invoke(manager,Intent.ACTION_SCREEN_ON.equals(action)?"PowerOn":"PowerOff");}catch(Exception ignored){}return;
          }
          if(Intent.ACTION_DREAMING_STARTED.equals(action)||Intent.ACTION_DREAMING_STOPPED.equals(action))return;
          try{receiver.onReceive(context,intent);}catch(LinkageError e){report.accept("Adaptador TROM: "+e.getClass().getSimpleName());}
        }};
        receivers.add(relay);return host.registerReceiver(relay,filter,Context.RECEIVER_NOT_EXPORTED);
      }
    };
    loader.loadClass("com.tcl.ff.component.utils.common.Utils").getField("sApp").set(null,new CoreApplication(host.getApplicationContext()));
    managerClass=loader.loadClass("com.tcl.ttvs.alexa.core.AlexaManager");
    manager=managerClass.getConstructor(Context.class).newInstance(wrapped);
    managerClass.getField("sInstance").set(null,manager);
    isolateBlockingHeartbeat();
    Class<?> deviceClass=loader.loadClass("com.tcl.ttvs.alexa.core.device.DeviceInfo");Object device=deviceClass.getConstructor().newInstance();
    for(Iterator<String> it=profile.keys();it.hasNext();){String key=it.next();try{Field f=deviceClass.getField(key);f.set(device,f.getType()==int.class?profile.optInt(key):profile.optString(key));}catch(NoSuchFieldException ignored){}}
    Class<?> fileClass=loader.loadClass("com.tcl.ttvs.alexa.core.device.FileInfo");Object file=fileClass.getConstructor().newInstance();
    set(file,"server_host","https://ai.tclking.com");set(file,"server_domain","https://ai.tclking.com");set(file,"software_version",profile.optString("software_version"));
    JSONObject initial=credentials==null?new JSONObject():credentials;
    set(file,"uuid",initial.optString("uuid"));set(file,"tcl_token",initial.optString("access_token"));set(file,"tcl_refreshtoken",initial.optString("refresh_token"));
    set(file,"tcl_token_expires_at",initial.optLong("expires_at"));set(file,"wwa_status","enable");set(file,"avgs_login",activation?"false":"true");set(file,"device",device);
    managerClass.getField("mFileInfo").set(manager,file);managerClass.getField("isDiscovered").setBoolean(manager,true);managerClass.getField("isNeedQuery").setBoolean(manager,activation);
    File storage=new File(host.getFilesDir(),"aa");if(!storage.exists()&&!storage.mkdirs())throw new java.io.IOException("Cannot create core state directory");new File(storage,".alexa.db").createNewFile();
    loader.loadClass("com.tcl.ttvs.alexa.core.utils.FileUtils").getMethod("saveConfig",fileClass).invoke(null,file);
    Class<?> handlerClass=loader.loadClass("com.tcl.ttvs.alexa.core.AlexaManager$MsgHandler");
    managerClass.getField("mHandler").set(manager,handlerClass.getConstructor(WeakReference.class).newInstance(new WeakReference<Object>(manager)));
    managerClass.getField("isDebug").setInt(manager,28);
    installSubscriptionGuard();
    Class<?> http=loader.loadClass("com.tcl.ff.component.core.http.core.HttpCore");
    http.getMethod("initialize",String.class).invoke(http.getMethod("getInstance").invoke(null),"https://ai.tclking.com");
    nativeBridge().getClass().getMethod("bindRemoteService",int.class,String.class).invoke(nativeBridge(),-1,"");
    if(activation){
      long remaining=resumeExpiresAt-System.currentTimeMillis()/1000;
      if(remaining>0&&remaining<=86400){
        activationExpiresAt=resumeExpiresAt;managerClass.getField("isGetActiveCode").setBoolean(manager,true);managerClass.getMethod("getAVGStateFromServer").invoke(manager);
        Handler h=(Handler)managerClass.getField("mHandler").get(manager);h.postDelayed(()->{if(stopped||quiesced||accountLinked())return;stopActivationTimer();h.removeMessages(4102);if(activationEvents!=null)activationEvents.accept(34,"");},remaining*1000);
      }else requestActivation();
    }else managerClass.getMethod("initSocketClient").invoke(manager);
    report.accept("TTVSCore carregado; persistência nativa redirecionada ao armazenamento privado do auxiliar");
  }
  void requestActivation()throws Exception{if(stopped||quiesced)return;activationExpiresAt=0;managerClass.getMethod("getActiveCodeImpl").invoke(manager);}
  boolean accountLinked(){if(stopped||quiesced||manager==null)return false;try{Object file=managerClass.getField("mFileInfo").get(manager);return "true".equalsIgnoreCase((String)file.getClass().getField("avgs_login").get(file));}catch(Exception e){return false;}}
  void isolateBlockingHeartbeat()throws Exception{
    // The original runnable only inspects the socket, connects it and posts native
    // handler messages; its blocking connect must not run on the activity looper.
    Field field=managerClass.getField("mHeartRunnable");Runnable nativeHeartbeat=(Runnable)field.get(manager);
    field.set(manager,(Runnable)()->{
      if(stopped||quiesced||!heartbeatRunning.compareAndSet(false,true))return;
      try{heartbeatWorker.execute(()->{try{if(!stopped&&!quiesced)nativeHeartbeat.run();}
        catch(RuntimeException|LinkageError e){if(!stopped&&!quiesced)report.accept("Reconexão nativa: "+e.getClass().getSimpleName());}
        finally{heartbeatRunning.set(false);}});
      }catch(RejectedExecutionException e){heartbeatRunning.set(false);}
    });
  }
  void unbindConnection(ServiceConnection connection){
    ServiceConnection relay=bindings.remove(connection);try{host.unbindService(relay==null?connection:relay);}catch(IllegalArgumentException ignored){}
  }
  void collectSubscriptionInterfaces(Class<?> type,Set<Class<?>> interfaces){
    if(type==null)return;for(Class<?> iface:type.getInterfaces()){
      if(Modifier.isPublic(iface.getModifiers())&&subscriptionType.isAssignableFrom(iface))interfaces.add(iface);
      collectSubscriptionInterfaces(iface,interfaces);
    }collectSubscriptionInterfaces(type.getSuperclass(),interfaces);
  }
  Object trackedSubscription(Object delegate){
    Set<Class<?>> protocols=new LinkedHashSet<>();protocols.add(subscriptionType);collectSubscriptionInterfaces(delegate.getClass(),protocols);
    // Preserve public Subscription subinterfaces such as QueueSubscription, including
    // requestFusion/poll, instead of silently disabling the native Rx fusion path.
    return java.lang.reflect.Proxy.newProxyInstance(loader,protocols.toArray(new Class<?>[0]),(proxy,method,args)->{
      try{Object result=method.invoke(delegate,args);if("cancel".equals(method.getName())&&(args==null||args.length==0))subscriptions.remove(delegate);return result;}
      catch(InvocationTargetException e){throw e.getCause();}
    });
  }
  void installSubscriptionGuard()throws Exception{
    Class<?> plugins=loader.loadClass("io.reactivex.plugins.RxJavaPlugins");
    Class<?> consumer=loader.loadClass("io.reactivex.functions.Consumer");
    Object errors=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{consumer},(proxy,method,args)->{
      if(!stopped&&"accept".equals(method.getName())&&args!=null&&args[0] instanceof Throwable){Throwable cause=(Throwable)args[0];for(int depth=0;depth<8&&cause.getCause()!=null;depth++)cause=cause.getCause();report.accept("Falha do núcleo TCL: "+cause.getClass().getSimpleName());if(!quiesced&&activationEvents!=null)activationEvents.accept(1000,"");}return null;
    });
    plugins.getMethod("setErrorHandler",consumer).invoke(null,errors);
    Class<?> function=loader.loadClass("io.reactivex.functions.BiFunction");
    Class<?> subscriber=loader.loadClass("io.reactivex.FlowableSubscriber");
    Method onSubscribe=null;for(Method method:subscriber.getMethods())if(method.getName().equals("onSubscribe")){onSubscribe=method;break;}
    if(onSubscribe==null)throw new NoSuchMethodException("FlowableSubscriber.onSubscribe");
    subscriptionType=onSubscribe.getParameterTypes()[0];
    Object previous=plugins.getMethod("getOnFlowableSubscribe").invoke(null);
    Object guard=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{function},(proxy,method,args)->{
      if(!"apply".equals(method.getName()))return method.getName().equals("toString")?"CoreSubscriptionGuard":null;
      Object target=previous==null?args[1]:function.getMethod("apply",Object.class,Object.class).invoke(previous,args);
      final Object[] subscription={null};
      return java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{subscriber},(relay,callback,values)->{
        String name=callback.getName();
        if(name.equals("onSubscribe")){Object value=values[0];synchronized(subscriptions){if(stopped){subscriptionType.getMethod("cancel").invoke(value);return null;}subscription[0]=value;subscriptions.add(value);}Object[] forwarded=values.clone();forwarded[0]=trackedSubscription(value);values=forwarded;}
        if(name.equals("onError")||name.equals("onComplete"))subscriptions.remove(subscription[0]);
        if(stopped)return null;
        if(name.equals("onNext")&&activationEvents!=null&&values!=null&&values.length>0&&values[0]!=null&&values[0].getClass().getName().equals("com.tcl.ttvs.alexa.core.bean.HttpRespInfoBean"))try{
          Object bean=values[0];Object code=bean.getClass().getMethod("getCode").invoke(bean);long seconds=((Number)bean.getClass().getMethod("getExpires_in").invoke(bean)).longValue();
          if(code!=null&&code.toString().matches("[0-9]{6}")&&seconds>0&&seconds<=86400)activationExpiresAt=System.currentTimeMillis()/1000+seconds;
        }catch(ReflectiveOperationException ignored){}
        try{return callback.invoke(target,values);}catch(InvocationTargetException e){throw e.getCause();}finally{if(quiesced&&name.equals("onNext"))stopActivationTimer();}
      });
    });
    plugins.getMethod("setOnFlowableSubscribe",function).invoke(null,guard);
  }
  static void set(Object object,String name,Object value)throws Exception{object.getClass().getField(name).set(object,value);}
  boolean connected(){if(stopped||quiesced)return false;try{Object socket=managerClass.getField("mWebSocketClient").get(manager);return socket!=null&&(Boolean)socket.getClass().getMethod("isOpen").invoke(socket);}catch(Exception e){return false;}}
  JSONObject credentials()throws Exception{
    Object file=managerClass.getField("mFileInfo").get(manager);Class<?> c=file.getClass();
    return new JSONObject().put("uuid",c.getField("uuid").get(file)).put("access_token",c.getField("tcl_token").get(file)).put("refresh_token",c.getField("tcl_refreshtoken").get(file)).put("expires_at",c.getField("tcl_token_expires_at").getLong(file));
  }
  boolean persisted(){try{
    File file=new File(host.getFilesDir(),"aa/.alexa.db");byte[] raw=java.nio.file.Files.readAllBytes(file.toPath());
    JSONObject stored=new JSONObject(new String(android.util.Base64.decode(raw,android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8));
    JSONObject active=credentials();return stored.optString("uuid").equals(active.getString("uuid"))&&stored.optString("tcl_token").equals(active.getString("access_token"))&&stored.getString("tcl_refreshtoken").equals(active.getString("refresh_token"))&&stored.getLong("tcl_token_expires_at")==active.getLong("expires_at");
  }catch(Exception e){return false;}}

  Object nativeBridge()throws Exception{Class<?> c=loader.loadClass("com.tcl.ttvs.alexa.core.WebSocketManager");return c.getMethod("getInstance",Context.class).invoke(null,host);}
  boolean executorConnected(){if(stopped||quiesced)return false;try{Object invoke=nativeBridge().getClass().getField("mInvokeHandler").get(nativeBridge());if(invoke==wrappedInvoke)invoke=originalInvoke;if(invoke==null)return false;IBinder binder=(IBinder)invoke.getClass().getMethod("asBinder").invoke(invoke);return binder!=null&&binder.isBinderAlive();}catch(Exception e){return false;}}
  void recoverExecutor()throws Exception{
    if(stopped||quiesced||executorConnected()||SystemClock.elapsedRealtime()-lastExecutorRecovery<5000)return;lastExecutorRecovery=SystemClock.elapsedRealtime();Object bridge=nativeBridge();Class<?> c=bridge.getClass();
    ServiceConnection old=(ServiceConnection)c.getField("mServiceConnection").get(bridge);if(old!=null)unbindConnection(old);
    c.getField("mInvokeHandler").set(bridge,null);c.getField("mServiceConnection").set(bridge,null);c.getField("mIsBind").setBoolean(bridge,false);
    Class<?> state=loader.loadClass("com.tcl.ttvs.alexa.core.WebSocketManager$BindAlexaServiceState");c.getField("mBindAlexaServiceState").set(bridge,state.getField("NOT_BIND").get(null));
    c.getMethod("bindRemoteService",int.class,String.class).invoke(bridge,-1,"");report.accept("Vínculo com executor SmartAlexa solicitado");
  }
  Object observeCallback(Object callback,Class<?> callbackType,BridgeMetrics.Ticket ticket)throws Exception{
    final IBinder delegate=(IBinder)callback.getClass().getMethod("asBinder").invoke(callback);final String descriptor="com.tcl.ttvs.alexa.IResultCallback";
    Binder forwarding=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
      if(code==IBinder.INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(descriptor);return true;}
      int position=data.dataPosition();if(code==1)try{data.enforceInterface(descriptor);data.readInt();data.readString();ticket.result(data.readInt());}catch(RuntimeException e){report.accept("Formato de retorno SmartAlexa não reconhecido");}finally{data.setDataPosition(position);}
      if(stopped||quiesced){if(reply!=null)reply.writeNoException();return true;}
      return delegate.transact(code,data,reply,flags);
    }};
    return java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{callbackType},(proxy,method,args)->{
      if("asBinder".equals(method.getName()))return forwarding;
      if("resultCall".equals(method.getName()))ticket.result((Integer)args[2]);
      if(stopped||quiesced)return null;
      try{return method.invoke(callback,args);}catch(InvocationTargetException e){throw e.getCause();}
    });
  }
  Object wrapInvoke(Object target,Class<?> iface){
    originalInvoke=target;
    wrappedInvoke=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{iface},(proxy,method,args)->{
      if((stopped||quiesced)&&"handleActionInvoke".equals(method.getName()))throw new RemoteException("Core stopped");
      if("handleActionInvoke".equals(method.getName())&&args!=null&&args.length==4&&activationEvents!=null){int event=(Integer)args[0];if(event==33||event==34||event==400||event==1000)activationEvents.accept(event,(String)args[1]);}
      if("handleActionInvoke".equals(method.getName())&&args!=null&&args.length==4&&((Integer)args[0])==0){
        JSONObject data=null;try{data=new JSONObject((String)args[1]);}catch(org.json.JSONException ignored){}
        if(data!=null){int type=data.optInt("msg_type",-1);BridgeMetrics.Ticket ticket=metrics.begin(type);Object[] observed=args.clone();
          try{observed[3]=observeCallback(args[3],method.getParameterTypes()[3],ticket);args=observed;}catch(Exception e){report.accept("Retorno sem observador: "+e.getClass().getSimpleName());}
          report.accept("Comando encaminhado: tipo="+type);
          if(type==6&&"on".equalsIgnoreCase(data.optString("power"))){wakeAdapter.wake();Object callback=args[3];new Handler(Looper.getMainLooper()).postDelayed(()->{if(stopped||quiesced)return;try{boolean on=((PowerManager)host.getSystemService(Context.POWER_SERVICE)).isInteractive();callback.getClass().getMethod("resultCall",int.class,String.class,int.class).invoke(callback,6,"Power on",on?0:1);}catch(Exception e){report.accept("Retorno de ligar indisponível");}},500);return null;}
        }
      }
      // Never filter or reconstruct native messages, callbacks or unknown types.
      try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}
    });return wrappedInvoke;
  }
  void installWakeAdapter(WakeSupport wake)throws Exception{
    if(stopped||quiesced)return;if(wake!=null)wakeAdapter=wake;
    Object bridge=nativeBridge();Field field=bridge.getClass().getField("mInvokeHandler");Object target=field.get(bridge);
    if(target==null||target==wrappedInvoke)return;field.set(bridge,wrapInvoke(target,field.getType()));
  }
  void disconnectExecutorForTest()throws Exception{Object bridge=nativeBridge();bridge.getClass().getMethod("unBindRemoteService").invoke(bridge);bridge.getClass().getField("mInvokeHandler").set(bridge,null);}
  void reconnect()throws Exception{if(stopped||quiesced)return;Handler handler=(Handler)managerClass.getField("mHandler").get(manager);handler.removeMessages(4131);managerClass.getMethod("initSocketClient").invoke(manager);}
  void disconnectForTest()throws Exception{Object socket=managerClass.getField("mWebSocketClient").get(manager);socket.getClass().getMethod("close").invoke(socket);}
  void refresh()throws Exception{if(stopped||quiesced)return;managerClass.getMethod("refreshTclTokenFromServer",int.class,String.class).invoke(manager,4103,"");}
  void command(JSONObject data)throws Exception{
    if(stopped||quiesced)throw new IllegalStateException("Core stopped");
    Class<?> bridge=loader.loadClass("com.tcl.ttvs.alexa.core.WebSocketManager");Object nativeBridge=bridge.getMethod("getInstance",Context.class).invoke(null,host);
    bridge.getMethod("onMessage",String.class).invoke(nativeBridge,data.toString());
  }
  void testVolume()throws Exception{
    int volume=((AudioManager)host.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
    JSONObject command=new JSONObject().put("msg_type",7).put("keyword","SetVolume").put("volume_number",String.valueOf(volume));
    Class<?> bridge=loader.loadClass("com.tcl.ttvs.alexa.core.WebSocketManager");Object nativeBridge=bridge.getMethod("getInstance",Context.class).invoke(null,host);
    bridge.getMethod("onMessage",String.class).invoke(nativeBridge,command.toString());
  }
  int pendingSubscriptions(){synchronized(subscriptions){return subscriptions.size();}}
  void stopActivationTimer(){if(manager==null)return;try{managerClass.getField("isNeedQuery").setBoolean(manager,false);Object timer=managerClass.getField("mDisposable").get(manager);if(timer!=null){loader.loadClass("io.reactivex.disposables.Disposable").getMethod("dispose").invoke(timer);managerClass.getField("mDisposable").set(manager,null);}}catch(Exception ignored){}}
  /** Stop control delivery while allowing an already started HTTP refresh to save tokens. */
  void quiesce(){
    if(stopped||quiesced)return;quiesced=true;metrics.stop();
    for(BroadcastReceiver r:receivers)try{host.unregisterReceiver(r);}catch(Exception ignored){}receivers.clear();
    synchronized(bindings){for(ServiceConnection relay:bindings.values())try{host.unbindService(relay);}catch(IllegalArgumentException ignored){}bindings.clear();}
    if(manager==null){heartbeatWorker.shutdownNow();return;}
    try{Object file=managerClass.getField("mFileInfo").get(manager);set(file,"avgs_login","false");managerClass.getField("isGetTokenPower").setBoolean(manager,false);}catch(Exception ignored){}
    stopActivationTimer();
    try{Object udp=managerClass.getField("udpClient").get(manager);if(udp!=null)udp.getClass().getMethod("releaseSource").invoke(udp);}catch(Exception ignored){}
    try{Object socket=managerClass.getField("mWebSocketClient").get(manager);if(socket!=null){Class<?> state=loader.loadClass("com.tcl.ttvs.alexa.core.CustomWebClient$OnSocketState");socket.getClass().getMethod("setOnSocketState",state).invoke(socket,new Object[]{null});socket.getClass().getMethod("close").invoke(socket);}}catch(Exception ignored){}
    heartbeatWorker.shutdownNow();
    // Do not leave a null-handler interval: an in-flight refresh may finish now.
    // MsgHandler constructed with a null manager ignores messages and cannot reopen a session.
    try{Object old=managerClass.getField("mHandler").get(manager);if(old instanceof Handler)((Handler)old).removeCallbacksAndMessages(null);Class<?> handler=loader.loadClass("com.tcl.ttvs.alexa.core.AlexaManager$MsgHandler");managerClass.getField("mHandler").set(manager,handler.getConstructor(WeakReference.class).newInstance(new WeakReference<Object>(null)));}catch(Exception ignored){}
    try{Object bridge=nativeBridge();bridge.getClass().getField("mInvokeHandler").set(bridge,null);bridge.getClass().getField("mIsBind").setBoolean(bridge,false);bridge.getClass().getField("mServiceConnection").set(bridge,null);}catch(Exception ignored){}
  }
  /** Final disposal: use after auth drain, or after a confirmed remote logout. */
  void stop(){
    if(stopped)return;quiesce();stopped=true;
    Object[] pending;synchronized(subscriptions){pending=subscriptions.toArray();subscriptions.clear();}
    for(Object item:pending)try{subscriptionType.getMethod("cancel").invoke(item);}catch(Exception ignored){}
    lastCleanup=resourceCleanup.submit(()->closePrivateResources());
  }
  void closePrivateResources(){
    if(loader==null)return;
    try{Class<?> http=loader.loadClass("com.tcl.ff.component.core.http.core.HttpCore");
      if(http.getClassLoader()==loader){Object instance=http.getMethod("getInstance").invoke(null);Object client=http.getMethod("getOkHttpClient").invoke(instance);
        if(client!=null&&client.getClass().getClassLoader()==loader){
          Object dispatcher=client.getClass().getMethod("dispatcher").invoke(client);
          if(dispatcher!=null&&dispatcher.getClass().getClassLoader()==loader){dispatcher.getClass().getMethod("cancelAll").invoke(dispatcher);Object executor=dispatcher.getClass().getMethod("executorService").invoke(dispatcher);if(executor instanceof ExecutorService)((ExecutorService)executor).shutdown();}
          Object pool=client.getClass().getMethod("connectionPool").invoke(client);if(pool!=null&&pool.getClass().getClassLoader()==loader)pool.getClass().getMethod("evictAll").invoke(pool);
          Object cache=client.getClass().getMethod("cache").invoke(client);if(cache!=null&&cache.getClass().getClassLoader()==loader)cache.getClass().getMethod("close").invoke(cache);
        }
      }
    }catch(Exception|LinkageError e){Throwable cause=BridgeService.root(e);report.accept("Limpeza do HTTP privado: "+cause.getClass().getSimpleName()+" em "+(cause.getStackTrace().length==0?"HTTP":cause.getStackTrace()[0].getClassName()+"."+cause.getStackTrace()[0].getMethodName()));}
    try{Class<?> schedulers=loader.loadClass("io.reactivex.schedulers.Schedulers"),pools=loader.loadClass("io.reactivex.internal.schedulers.SchedulerPoolFactory");if(schedulers.getClassLoader()==loader&&pools.getClassLoader()==loader)schedulers.getMethod("shutdown").invoke(null);}catch(Exception|LinkageError e){report.accept("Limpeza dos executores privados: "+e.getClass().getSimpleName());}
    // Keep the isolated error handler until canceled workers exit; a late error must not crash Android.
  }
}
