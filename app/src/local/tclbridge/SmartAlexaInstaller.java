package local.tclbridge;
import android.app.*;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.widget.*;
import java.io.*;import java.net.*;import java.security.*;import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

final class SmartAlexaInstaller implements Closeable {
  static final String SOURCE="https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/SmartAlexa-6.13.25.apk";
  static final String SHA="f1203599875f2e6d082b893eb3e64a44c071d230d55336d05cf74061531d4441",CERT="25606cd9fadf3da6a16e741db6d70c3c5c5ebf1ec58bfc327517dec430ceba98";
  final Activity activity;final Handler main=new Handler(Looper.getMainLooper());volatile boolean cancelled;volatile HttpsURLConnection connection;AlertDialog dialog;
  SmartAlexaInstaller(Activity a){activity=a;}
  void begin(){
    if(!activity.getPackageManager().canRequestPackageInstalls()){
      activity.getSharedPreferences("bridge",0).edit().putBoolean("pending_native_install",true).apply();
      new AlertDialog.Builder(activity).setTitle("Permitir a instalação do SmartAlexa").setMessage("Na próxima tela, permita que Alexa na TV instale aplicativos. Volte para continuar. O APK original da TCL será conferido antes de abrir o instalador do Android.").setNegativeButton("Agora não",(d,w)->activity.getSharedPreferences("bridge",0).edit().remove("pending_native_install").apply()).setPositiveButton("Abrir autorização",(d,w)->{try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}catch(Exception e){Toast.makeText(activity,"Abra Instalar apps desconhecidos nas configurações da TV",Toast.LENGTH_LONG).show();}}).show();return;
    }
    activity.getSharedPreferences("bridge",0).edit().remove("pending_native_install").apply();cancelled=false;
    TextView status=new TextView(activity);status.setText("Baixando o APK original da TCL…");status.setPadding(32,24,32,24);
    dialog=new AlertDialog.Builder(activity).setTitle("Instalar SmartAlexa").setView(status).setNegativeButton("Cancelar",(d,w)->close()).create();dialog.setCanceledOnTouchOutside(false);dialog.setOnCancelListener(d->close());dialog.show();
    new Thread(()->{File part=null;try{
      File dir=new File(activity.getCacheDir(),"install");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Não foi possível preparar o download");File apk=new File(dir,"SmartAlexa.apk");
      if(!apk.exists()||!SHA.equals(hash(apk))){
        part=File.createTempFile("SmartAlexa-",".part",dir);URL url=new URL(SOURCE);int redirects=0;
        for(;;){String host=url.getHost();if(!"https".equals(url.getProtocol())||!(host.equals("github.com")||host.endsWith(".githubusercontent.com")))throw new IOException("Origem de download não reconhecida");
          connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);int code=connection.getResponseCode();
          if(code>=300&&code<400&&redirects++<5){String location=connection.getHeaderField("Location");connection.disconnect();url=new URL(url,location);continue;}
          if(code!=200)throw new IOException("Download indisponível. Tente novamente mais tarde.");break;
        }
        long total=connection.getContentLengthLong(),bytes=0;int last=-1;
        try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(part)){
          byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1){if(cancelled)throw new IOException("Download cancelado");bytes+=n;if(bytes>30000000)throw new IOException("Tamanho do download inesperado");out.write(buffer,0,n);int percent=total>0?(int)(bytes*100/total):0;if(percent!=last){last=percent;final int shown=percent;main.post(()->{if(!cancelled&&!activity.isFinishing())status.setText("Baixando SmartAlexa… "+shown+"%");});}}out.getFD().sync();
        }
        if(cancelled)throw new IOException("Download cancelado");
        if(bytes!=24040141||!SHA.equals(hash(part)))throw new IOException("O arquivo recebido não passou na conferência de integridade");
        if(cancelled)throw new IOException("Download cancelado");
        if(!part.renameTo(apk))throw new IOException("Não foi possível salvar o instalador verificado");part=null;
      }
      PackageInfo info=activity.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
      if(info==null||!"com.tcl.smartalexa".equals(info.packageName)||info.getLongVersionCode()!=613250000L||info.signingInfo==null||info.signingInfo.getApkContentsSigners().length!=1||!CERT.equals(hex(MessageDigest.getInstance("SHA-256").digest(info.signingInfo.getApkContentsSigners()[0].toByteArray()))))throw new IOException("Assinatura do SmartAlexa não reconhecida");
      main.post(()->{if(dialog!=null)dialog.dismiss();if(cancelled||activity.isFinishing())return;try{activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://local.tclbridge.install/SmartAlexa.apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){Toast.makeText(activity,"Instalador indisponível nesta TV",Toast.LENGTH_LONG).show();}});
    }catch(Exception e){final String message=e instanceof IOException?e.getMessage():"Não foi possível verificar o SmartAlexa";main.post(()->{if(dialog!=null)dialog.dismiss();if(!cancelled&&!activity.isFinishing())new AlertDialog.Builder(activity).setTitle("Instalação não concluída").setMessage(message).setPositiveButton("Entendi",null).show();});}
    finally{if(connection!=null)connection.disconnect();if(part!=null)part.delete();}},"smartalexa-download").start();
  }
  static String hash(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}return hex(d.digest());}
  static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte value:bytes)b.append(String.format(Locale.ROOT,"%02x",value&255));return b.toString();}
  public void close(){cancelled=true;if(connection!=null)connection.disconnect();if(dialog!=null)dialog.dismiss();}
}
