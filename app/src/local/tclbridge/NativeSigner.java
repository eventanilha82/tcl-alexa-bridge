package local.tclbridge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.JSONObject;

/** Uses the signer inside the device's system TTVSCore; never exports its key. */
final class NativeSigner {
  private static final String PACKAGE="com.tcl.ttvs";
  private static final String SIGNER="com.tcl.ttvs.alexa.core.utils.RSASignature";
  private static volatile Class<?> signerClass;

  private static Class<?> signer(Context context)throws IOException{
    Class<?> cached=signerClass;if(cached!=null)return cached;
    synchronized(NativeSigner.class){
      if(signerClass!=null)return signerClass;
      try{
        ApplicationInfo info=context.getPackageManager().getApplicationInfo(PACKAGE,0);
        if((info.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0)
          throw new IOException("O assinador requer o TTVSCore de sistema da TV TCL");
        Context nativeContext=context.createPackageContext(PACKAGE,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
        Class<?> type=nativeContext.getClassLoader().loadClass(SIGNER);
        Method sign=type.getMethod("sign2bytes",byte[].class);
        Method verify=type.getMethod("verify",byte[].class,byte[].class);
        if(sign.getReturnType()!=byte[].class||verify.getReturnType()!=boolean.class)
          throw new IOException("Versão do assinador TTVSCore incompatível");
        signerClass=type;return type;
      }catch(IOException e){throw e;}
      catch(Exception|LinkageError e){throw failure("Assinador do TTVSCore indisponível",e);}
    }
  }

  static byte[] sign(Context context,byte[] payload)throws IOException{
    if(payload==null||payload.length==0||payload.length>65536)
      throw new IOException("Dados de registro inválidos para assinatura");
    try{
      Class<?> type=signer(context);
      byte[] signature=(byte[])type.getMethod("sign2bytes",byte[].class).invoke(null,(Object)payload);
      if(signature==null||signature.length==0)throw new IOException("TTVSCore não produziu a assinatura de registro");
      boolean valid=(Boolean)type.getMethod("verify",byte[].class,byte[].class).invoke(null,payload,signature);
      if(!valid){Arrays.fill(signature,(byte)0);throw new IOException("Assinatura produzida pelo TTVSCore não foi validada");}
      return signature;
    }catch(IOException e){throw e;}
    catch(Exception|LinkageError e){throw failure("Assinatura nativa de registro falhou",e);}
  }

  /** Offline diagnostic: no API request, account change, profile or key output. */
  static JSONObject dryRun(Context context)throws IOException{
    byte[] payload="alexa-bridge-native-signer-diagnostic-v1".getBytes(StandardCharsets.UTF_8);
    byte[] signature=sign(context,payload);
    try{
      return new JSONObject().put("available",true).put("verified",true)
          .put("algorithm","SHA256withRSA").put("signature_bytes",signature.length)
          .put("provider_package",PACKAGE).put("method","RSASignature.sign2bytes");
    }catch(org.json.JSONException e){throw failure("Diagnóstico do assinador falhou",e);}
    finally{Arrays.fill(signature,(byte)0);}
  }

  private static IOException failure(String message,Throwable error){
    while(error instanceof InvocationTargetException&&error.getCause()!=null)error=error.getCause();
    // Do not expose native exception messages: they can contain request or key material.
    return new IOException(message+" ("+error.getClass().getSimpleName()+")");
  }
}
