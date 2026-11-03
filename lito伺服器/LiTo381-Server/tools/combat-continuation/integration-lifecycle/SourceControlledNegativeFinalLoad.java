import java.nio.file.*;import java.net.*;import java.util.*;
public final class SourceControlledNegativeFinalLoad {
 public static void main(String[]args)throws Exception {
  SourceMassTeleportExitLoad.main(args);var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(args[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())) {
   String p="local.lito381.server.";
   for(String name:List.of("NativeSourceControlledNegativeFinal","NativeSourceControlledNegativeFinal$Scope","NativeSourceControlledNegativeFinal$Value","NativeAreaMaterialAction$Context","NativeSourceTargetNegativeLife","NativeSourceWeaponFinalFrame","NativeControlledNegativeMaterial")) {
    var type=Class.forName(p+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same candidate controlled-negative owner");
    if(name.equals("NativeSourceControlledNegativeFinal")&&type.getDeclaredFields().length!=0)throw new AssertionError("no new global roots");
   }
   var banned=new HashSet<String>(List.of(p+"NativeAreaMaterialAction$Context",p+"NativePlayerCommit$Frame",p+"NativePlayerVitalCompletions$Receipt",p+"NativeSourceRawDamage$Member",p+"NativeSourceControlledNegativeFinal$Scope",p+"NativeSelfCastAction$Task"));
   for(String name:List.of("NativeSourceControlledNegativeFinal$Value","NativeSourceWeaponFinalFrame$Output","NativeSourceWeaponFinalFrame$Proof","NativeSourceWeaponFinalFrame$Participant","NativeSourceWeaponFinalFrame$Npc","NativeSourceSelfFinalFrame$First","NativeSourceSelfFinalFrame$Participant"))for(var f:Class.forName(p+name,false,loader).getDeclaredFields())if(banned.contains(f.getType().getName())||Collection.class.isAssignableFrom(f.getType())||Map.class.isAssignableFrom(f.getType()))throw new AssertionError("queued immutable values retain producer state: "+name+"."+f.getName());
  }
  System.out.println("PASS inherited mass-exit no-init plus7 controlled-negative owners; immutable queued DTO graph has no Context/Frame/receipt/Member/Scope/Task/collection and no new global roots");
 }
}
