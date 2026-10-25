package tclx.os;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

/** Only the isolated TCL runtime uses this adapter; the installed system library is untouched. */
public final class SystemProperties {
  private static final ConcurrentHashMap<String,String> alexa=new ConcurrentHashMap<>();
  private static boolean local(String key){return "sys.enablealexa".equals(key)||"sys.showalexa".equals(key);}
  public static void resetAlexa(boolean linked){alexa.put("sys.enablealexa",linked?"true":"false");alexa.put("sys.showalexa","true");}
  public static String get(String key){return get(key,"");}
  public static String get(String key,String fallback){
    if(local(key))return alexa.getOrDefault(key,fallback);
    try{return (String)Class.forName("android.os.SystemProperties").getMethod("get",String.class,String.class).invoke(null,key,fallback);}catch(Exception e){return fallback;}
  }
  public static int getInt(String key,int fallback){try{return Integer.parseInt(get(key,""));}catch(NumberFormatException e){return fallback;}}
  public static long getLong(String key,long fallback){try{return Long.parseLong(get(key,""));}catch(NumberFormatException e){return fallback;}}
  public static boolean getBoolean(String key,boolean fallback){String v=get(key,"");if(v.matches("(?i)1|y|yes|on|true"))return true;if(v.matches("(?i)0|n|no|off|false"))return false;return fallback;}
  public static void set(String key,String value){
    if(local(key)){alexa.put(key,value);return;}
    try{Class.forName("android.os.SystemProperties").getMethod("set",String.class,String.class).invoke(null,key,value);}
    catch(InvocationTargetException e){throw new IllegalStateException("Propriedade de sistema indisponível",e.getCause());}
    catch(Exception e){throw new IllegalStateException("Propriedade de sistema indisponível",e);}
  }
  private SystemProperties(){}
}
