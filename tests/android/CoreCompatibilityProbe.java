package local.tclbridge;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import org.json.JSONArray;
import org.json.JSONObject;

/** Headless, separate-UID APK contract probe. No INTERNET permission or account access. */
public final class CoreCompatibilityProbe extends Instrumentation {
  int checks;String selectedCore;String contractStage;String failedAssertion;
  void require(boolean value,String step){checks++;if(!value){failedAssertion=step;throw new AssertionError(step);}}
  @Override public void onCreate(Bundle arguments){super.onCreate(arguments);selectedCore=arguments==null?null:arguments.getString("core");start();}
  @Override public void onStart(){
    Bundle result=new Bundle();
    try{
      Context host=getTargetContext();JSONArray cases=new JSONArray();boolean allPassed=true;
      for(String asset:host.getAssets().list("cores")){
        if(!asset.endsWith(".apk")||(selectedCore!=null&&!selectedCore.equals(asset)))continue;
        File apk=new File(host.getFilesDir(),asset);File runtime=null;String stage="copy";
        int initial=checks;contractStage="not_started";failedAssertion=null;
        try{
        try(InputStream in=host.getAssets().open("cores/"+asset);FileOutputStream out=new FileOutputStream(apk)){
          byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
        }
        stage="prepare";runtime=CoreArchive.prepare(host,apk.getAbsolutePath());
        stage="load";
        Context installed=host.createPackageContext("com.tcl.ttvs",Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
        ClassLoader loader=new IsolatedCoreLoader(runtime.getAbsolutePath(),installed.getClassLoader(),host.getClassLoader());
        final Throwable[] failure={null};final JSONObject[] summary={null};
        stage="contract";runOnMainSync(()->{try{summary[0]=checkCore(host,loader,asset);}catch(Throwable error){failure[0]=error;}});
        if(failure[0]!=null)throw new AssertionError(asset+": "+failure[0].getClass().getSimpleName(),failure[0]);
        cases.put(summary[0]);
        }catch(Throwable error){
          allPassed=false;Throwable root=error;
          for(int depth=0;depth<8&&root.getCause()!=null&&root.getCause()!=root;depth++)root=root.getCause();
          cases.put(new JSONObject().put("asset",asset).put("passed",false)
            .put("stage",stage).put("contract_stage",contractStage).put("checks",checks-initial)
            .put("error_class",error.getClass().getSimpleName()).put("root_error_class",root.getClass().getSimpleName()).put("failed_assertion",failedAssertion));
        }finally{
          // Only this separate probe UID's temporary copies are removed.
          apk.delete();if(runtime!=null)runtime.delete();
        }
      }
      require(cases.length()>0,"No vendor APKs supplied");
      result.putString("summary",new JSONObject().put("passed",allPassed).put("checks",checks).put("cases",cases).toString());
      finish(allPassed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }catch(Throwable error){
      // Fixed stage/class metadata only; never expose arbitrary vendor exception messages.
      result.putString("summary","failed: "+error.getClass().getSimpleName());
      finish(Activity.RESULT_CANCELED,result);
    }
  }
  Class<?> ownClass(ClassLoader loader,String name)throws Exception {
    Class<?> type=loader.loadClass(name);
    require(type.getClassLoader()==loader,"Selected class must come from supplied APK");
    return type;
  }
  JSONObject checkCore(Context host,ClassLoader loader,String label)throws Exception {
    int initial=checks;
    contractStage="utils";
    Class<?> utils=ownClass(loader,"com.tcl.ff.component.utils.common.Utils");
    require(utils.getClassLoader()==loader,"Runtime must use the supplied APK");
    boolean legacyPublic;
    try{utils.getField("sApp");legacyPublic=true;}catch(NoSuchFieldException expected){legacyPublic=false;}
    CoreApplication application=new CoreApplication(host.getApplicationContext());
    CoreReflection.field(utils,"sApp").set(null,application);
    require(utils.getMethod("getApp").invoke(null)==application,"Utils application context");
    contractStage="manager";
    Class<?> managerType=ownClass(loader,"com.tcl.ttvs.alexa.core.AlexaManager");
    Object manager=CoreReflection.constructor(managerType,Context.class).newInstance(host);
    CoreReflection.field(managerType,"sInstance").set(null,manager);
    require(CoreReflection.field(managerType,"sInstance").get(null)==manager,"Manager singleton");
    Object heart=CoreReflection.field(managerType,"mHeartRunnable").get(manager);
    require(heart instanceof Runnable,"Heartbeat field");
    contractStage="device";
    Class<?> deviceType=ownClass(loader,"com.tcl.ttvs.alexa.core.device.DeviceInfo");
    Object device=CoreReflection.constructor(deviceType).newInstance();
    for(String name:new String[]{"city","client_type","country","dev_desp","device_id","mac","netWork_standby","software_version","tv_name"}){
      Field field=CoreReflection.field(deviceType,name);
      if(field.getType()==int.class){field.setInt(device,52);require(field.getInt(device)==52,"Numeric profile field");}
      else{require(field.getType()==String.class,"Profile field type");field.set(device,"probe");require("probe".equals(field.get(device)),"String profile field");}
    }
    contractStage="file_info";
    Class<?> fileType=ownClass(loader,"com.tcl.ttvs.alexa.core.device.FileInfo");
    Object file=CoreReflection.constructor(fileType).newInstance();
    for(String name:new String[]{"server_host","server_domain","software_version","uuid","tcl_token","tcl_refreshtoken","wwa_status","avgs_login"}){
      Field field=CoreReflection.field(fileType,name);require(field.getType()==String.class,"String state type");field.set(file,"probe");require("probe".equals(field.get(file)),"State round trip");
    }
    CoreReflection.field(fileType,"tcl_token_expires_at").setLong(file,4294967296L);
    require(CoreReflection.field(fileType,"tcl_token_expires_at").getLong(file)==4294967296L,"64-bit expiry");
    CoreReflection.field(fileType,"device").set(file,device);
    CoreReflection.field(managerType,"mFileInfo").set(manager,file);
    require(CoreReflection.field(managerType,"mFileInfo").get(manager)==file,"Manager state");
    contractStage="handler";
    Class<?> handlerType=ownClass(loader,"com.tcl.ttvs.alexa.core.AlexaManager$MsgHandler");
    Handler handler=(Handler)CoreReflection.constructor(handlerType,WeakReference.class).newInstance(new WeakReference<Object>(manager));
    CoreReflection.field(managerType,"mHandler").set(manager,handler);
    CoreReflection.field(managerType,"isDebug").setInt(manager,28);
    for(String name:new String[]{"isDiscovered","isNeedQuery","isGetActiveCode","isGetTokenPower"}){
      Field field=CoreReflection.field(managerType,name);field.setBoolean(manager,true);require(field.getBoolean(manager),"Manager flag");
    }
    require(CoreReflection.field(managerType,"mDisposable").get(manager)==null,"Activation timer initially empty");
    require(CoreReflection.field(managerType,"mWebSocketClient").get(manager)==null,"No socket opened");
    require(CoreReflection.field(managerType,"udpClient").get(manager)==null,"No UDP service opened");
    contractStage="websocket_manager";
    Class<?> bridgeType=ownClass(loader,"com.tcl.ttvs.alexa.core.WebSocketManager");
    Object bridge=bridgeType.getMethod("getInstance",Context.class).invoke(null,host);
    require(CoreReflection.field(bridgeType,"mInvokeHandler").get(bridge)==null,"No SmartAlexa binding");
    require(CoreReflection.field(bridgeType,"mServiceConnection").get(bridge)==null,"No service connection");
    CoreReflection.field(bridgeType,"mIsBind").setBoolean(bridge,false);
    contractStage="executor_state";
    Class<?> state=ownClass(loader,"com.tcl.ttvs.alexa.core.WebSocketManager$BindAlexaServiceState");
    Object notBound=CoreReflection.field(state,"NOT_BIND").get(null);
    CoreReflection.field(bridgeType,"mBindAlexaServiceState").set(bridge,notBound);
    require(CoreReflection.field(bridgeType,"mBindAlexaServiceState").get(bridge)==notBound,"Executor recovery state");
    contractStage="binder_signatures";
    Class<?> invoke=ownClass(loader,"com.tcl.ttvs.alexa.IAlexaInvoke"),callback=ownClass(loader,"com.tcl.ttvs.alexa.IResultCallback");
    require(invoke.getMethod("handleActionInvoke",int.class,String.class,int.class,callback).getReturnType()==void.class,"Executor signature");
    require(callback.getMethod("resultCall",int.class,String.class,int.class).getReturnType()==void.class,"Callback signature");
    handler.removeCallbacksAndMessages(null);
    return new JSONObject().put("asset",label).put("legacy_public_fields",legacyPublic).put("selected_class_ownership_checked",true).put("checks",checks-initial).put("passed",true);
  }
}
