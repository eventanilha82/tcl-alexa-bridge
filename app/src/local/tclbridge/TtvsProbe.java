package local.tclbridge;
import android.content.*;
import android.os.*;
import java.util.function.Consumer;

/** Read-only probe of the service broker used by the official TCL client. */
final class TtvsProbe {
  static final String SERVICE="com.tcl.ttvs.alexa.core.AlexaService";
  static final String BROKER="com.tcl.ttvs.common.internal.ITTVServiceBroker";
  static final String CALLBACK="com.tcl.ttvs.common.internal.ITTVSCallbacks";
  static final String ALEXA="com.tcl.ttvs.alexa.IAlexaService";
  final Context context;final Consumer<String> report;final Handler handler=new Handler(Looper.getMainLooper());boolean bound;
  TtvsProbe(Context c,Consumer<String> r){context=c;report=r;}
  final ServiceConnection connection=new ServiceConnection(){
    public void onServiceConnected(ComponentName n,IBinder broker){new Thread(()->{
      try{
        report.accept("TTVSCore conectado: "+broker.getInterfaceDescriptor());
        if(ALEXA.equals(broker.getInterfaceDescriptor())){inspect(broker);return;}
        Parcel request=Parcel.obtain(),reply=Parcel.obtain();
        try{request.writeInterfaceToken(BROKER);request.writeStrongBinder(callback);
          request.writeInt(1);int object=begin(request,20293);string(request,2,SERVICE);request.writeInt((4<<16)|3);request.writeInt(19420000);string(request,4,context.getPackageName());end(request,object);
          if(!broker.transact(14,request,reply,0))throw new RemoteException("Broker sem operação getService");reply.readException();
        }finally{request.recycle();reply.recycle();}
      }catch(Exception e){report.accept("TTVSCore: "+e.getClass().getSimpleName());close();}
    },"TTVS-native-probe").start();}
    public void onServiceDisconnected(ComponentName n){report.accept("TTVSCore desconectado");}
  };
  final Binder callback=new Binder(){
    protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
      if(code==INTERFACE_TRANSACTION){reply.writeString(CALLBACK);return true;}
      if(code!=1)return super.onTransact(code,data,reply,flags);
      data.enforceInterface(CALLBACK);int status=data.readInt();IBinder service=data.readStrongBinder();if(data.readInt()!=0)Bundle.CREATOR.createFromParcel(data);reply.writeNoException();
      report.accept("TTVSCore getService: status="+status);
      if(status==0&&service!=null)new Thread(()->inspect(service),"TTVS-status").start();else close();return true;
    }
  };
  void start(){Intent i=new Intent("com.tcl.ttvs.alexa.services.START").setComponent(new ComponentName("com.tcl.ttvs",SERVICE));
    try{bound=context.bindService(i,connection,Context.BIND_AUTO_CREATE);if(!bound)report.accept("TTVSCore não vinculou o serviço");handler.postDelayed(()->close(),10000);}catch(Exception e){report.accept("TTVSCore bind: "+e.getClass().getSimpleName());}}
  void inspect(IBinder service){try{report.accept("TTVSCore nativo: WWA="+get(service,8)+", login="+get(service,9));}catch(Exception e){report.accept("TTVSCore status: "+e.getClass().getSimpleName());}finally{close();}}
  String get(IBinder service,int op)throws RemoteException{Parcel in=Parcel.obtain(),out=Parcel.obtain();try{in.writeInterfaceToken(ALEXA);in.writeInt(op);service.transact(4,in,out,0);out.readException();return out.readString();}finally{in.recycle();out.recycle();}}
  void close(){handler.post(()->{if(bound){try{context.unbindService(connection);}catch(Exception ignored){}bound=false;}});}
  static int begin(Parcel p,int id){p.writeInt(0xffff0000|id);p.writeInt(0);return p.dataPosition();}
  static void end(Parcel p,int start){int finish=p.dataPosition();p.setDataPosition(start-4);p.writeInt(finish-start);p.setDataPosition(finish);}
  static void string(Parcel p,int id,String value){int start=begin(p,id);p.writeString(value);end(p,start);}
}
