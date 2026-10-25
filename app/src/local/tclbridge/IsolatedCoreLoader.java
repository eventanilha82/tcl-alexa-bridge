package local.tclbridge;
import dalvik.system.PathClassLoader;

/** Boot classes, private TCL runtime, then installed dependencies; one explicit metadata adapter. */
final class IsolatedCoreLoader extends PathClassLoader {
  private final ClassLoader app;
  IsolatedCoreLoader(String dex,ClassLoader nativeParent,ClassLoader appLoader){super(dex,null,nativeParent);app=appLoader;}
  @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    if("tclx.os.SystemProperties".equals(name))return app.loadClass(name);
    Class<?> result=findLoadedClass(name);
    if(result==null)try{result=Class.forName(name,false,null);}catch(ClassNotFoundException ignored){}
    if(result==null)try{result=findClass(name);}catch(ClassNotFoundException ignored){result=getParent().loadClass(name);}
    if(resolve)resolveClass(result);return result;
  }
}
