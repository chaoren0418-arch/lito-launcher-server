import java.nio.file.*;import java.net.*;import java.util.*;
public final class SourceMaterialPairGuardLoad {
 public static void main(String[]a)throws Exception{
  SourceWeaponFinalFrameLoad.main(a);var jar=Path.of(a[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(a[1]))){for(var path:files.filter(p->p.toString().endsWith(".jar")).toList())urls.add(path.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
   for(var name:List.of("NativeSourceMaterialPairGuard","NativeSourceMaterialPairGuard$Guarded","NativeSourceLightPairLife","NativeLightAction","ClientSession")){
    var type=Class.forName("local.lito381.server."+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same candidate material pair owner");
    if(name.equals("NativeSourceMaterialPairGuard")&&type.getDeclaredFields().length!=0)throw new AssertionError("fieldless shared guard");
    if(name.endsWith("$Guarded"))for(var field:type.getDeclaredFields())if(Set.of("Context","Frame","Receipt","Member").contains(field.getType().getSimpleName())||Map.class.isAssignableFrom(field.getType())||ThreadLocal.class.isAssignableFrom(field.getType()))throw new AssertionError("bounded Task cannot retain mutable discovery/transport authority");
   }
  }System.out.println("PASS401 inherited runtime no-init coverage plus5 explicit shared material pair owners");
 }
}
