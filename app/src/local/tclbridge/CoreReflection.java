package local.tclbridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/** Access only the selected TCL runtime's members, including release-build private members. */
final class CoreReflection {
  private CoreReflection() {}

  static Field field(Class<?> type,String name)throws NoSuchFieldException {
    for(Class<?> current=type;current!=null;current=current.getSuperclass()){
      try{
        Field field=current.getDeclaredField(name);
        field.setAccessible(true);
        return field;
      }catch(NoSuchFieldException ignored){}
    }
    throw new NoSuchFieldException(type.getName()+"."+name);
  }

  static <T> Constructor<T> constructor(Class<T> type,Class<?>... parameters)throws NoSuchMethodException {
    Constructor<T> constructor=type.getDeclaredConstructor(parameters);
    constructor.setAccessible(true);
    return constructor;
  }
}
