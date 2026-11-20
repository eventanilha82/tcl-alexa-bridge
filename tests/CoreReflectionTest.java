package local.tclbridge;

import java.lang.reflect.InvocationTargetException;

/** Regression cases for release/private, inherited and legacy/public TCL layouts. */
public final class CoreReflectionTest {
  static int checks;
  static void require(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
  private static class ReleaseState {
    private static ReleaseState instance;
    private long expiresAt;
    private boolean linked;
    private final String name;
    private ReleaseState(String name){this.name=name;}
  }
  private static final class ChildState extends ReleaseState {
    private ChildState(){super("child");}
  }
  public static final class LegacyState {
    public int country;
    public LegacyState(){}
  }
  private static final class BrokenState {
    private BrokenState(){throw new IllegalStateException("fixture");}
  }

  public static void main(String[] args)throws Exception {
    ReleaseState first=CoreReflection.constructor(ReleaseState.class,String.class).newInstance("first");
    ReleaseState second=CoreReflection.constructor(ReleaseState.class,String.class).newInstance("second");
    CoreReflection.field(ReleaseState.class,"instance").set(null,first);
    require(CoreReflection.field(ReleaseState.class,"instance").get(null)==first);
    CoreReflection.field(ReleaseState.class,"expiresAt").setLong(first,4294967296L);
    CoreReflection.field(ReleaseState.class,"linked").setBoolean(first,true);
    require(CoreReflection.field(ReleaseState.class,"expiresAt").getLong(first)==4294967296L);
    require(CoreReflection.field(ReleaseState.class,"expiresAt").getLong(second)==0L);
    require(CoreReflection.field(ReleaseState.class,"linked").getBoolean(first));
    ChildState child=CoreReflection.constructor(ChildState.class).newInstance();
    CoreReflection.field(ChildState.class,"expiresAt").setLong(child,42);
    require(CoreReflection.field(ReleaseState.class,"expiresAt").getLong(child)==42);
    require("child".equals(CoreReflection.field(ChildState.class,"name").get(child)));
    LegacyState legacy=CoreReflection.constructor(LegacyState.class).newInstance();
    CoreReflection.field(LegacyState.class,"country").setInt(legacy,52);
    require(legacy.country==52);
    try{CoreReflection.field(ReleaseState.class,"missing");throw new AssertionError("Missing state accepted");}
    catch(NoSuchFieldException expected){require(expected.getMessage().contains("ReleaseState.missing"));}
    try{CoreReflection.constructor(ReleaseState.class);throw new AssertionError("Wrong constructor accepted");}
    catch(NoSuchMethodException expected){require(true);}
    try{CoreReflection.constructor(BrokenState.class).newInstance();throw new AssertionError("Target failure hidden");}
    catch(InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException);}
    // Reflection access on one handle must not change the underlying class visibility.
    try{ReleaseState.class.getField("expiresAt");throw new AssertionError("Class layout mutated");}
    catch(NoSuchFieldException expected){require(true);}
    System.out.println("CoreReflection: "+checks+" checks passed (private/public/inherited/typed state, missing members, constructor failure).");
  }
}
