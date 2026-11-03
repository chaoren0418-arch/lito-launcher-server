import java.nio.file.*;import java.net.*;import java.util.*;import java.lang.reflect.Modifier;
public final class SourceNegativeFinalFrameLoad {
 public static void main(String[]args)throws Exception{
  SourceNegativePairFirstLoad.main(args);var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(args[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
   String p="local.lito381.server.";for(String name:List.of("NativeSourceNegativeFinalFrame","NativeSourceNegativeFinalFrame$Scope","NativeSourceNegativeFinalFrame$Value","NativeSourceNegativeTargets$Aim","NativeSourceNegativeTargets","NativeSourceWeaponFinalFrame")){
    var type=Class.forName(p+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same negative final candidate owner");
    if(name.equals("NativeSourceNegativeFinalFrame")&&type.getDeclaredFields().length!=0)throw new AssertionError("no new global roots");
    if(name.equals("NativeSourceNegativeTargets$Aim")){var f=type.getDeclaredField("nativeSourceNegativeFinal");if(f.getType()!=Object.class||!Modifier.isTransient(f.getModifiers())||Modifier.isStatic(f.getModifiers()))throw new AssertionError("bounded transient producing field only");}
   }
   var banned=Set.of(p+"NativeAreaMaterialAction$Context",p+"NativePlayerCommit$Frame",p+"NativePlayerVitalCompletions$Receipt",p+"NativeSourceRawDamage$Member",p+"NativeCompanionTargets$Scope",p+"NativeSourceNegativeTargets$Aim",p+"NativeSourceNegativeFinalFrame$Scope",p+"NativeSelfCastAction$Task");
   for(String name:List.of("NativeSourceNegativeFinalFrame$Value","NativeSourceSelfFinalFrame$First","NativeSourceSelfFinalFrame$Participant","NativeSourceWeaponFinalFrame$Participant","NativeSourceWeaponFinalFrame$Npc")){
    var type=Class.forName(p+name,false,loader);if(!type.isRecord())throw new AssertionError("immutable queued value record required");for(var f:type.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&(!Modifier.isFinal(f.getModifiers())||banned.contains(f.getType().getName())||Collection.class.isAssignableFrom(f.getType())||Map.class.isAssignableFrom(f.getType())))throw new AssertionError("queued value retains producer or mutable collection: "+name+"."+f.getName());
   }
  }System.out.println("PASS inherited negative-first no-init plus6 negative final owners; immutable queued records contain no Aim/Context/Frame/receipt/Member/Scope/Task/collection and no new global roots");
 }
}
