import java.nio.file.*;import java.net.*;import java.util.*;
public final class SourceMassTeleportExitLoad {
 public static void main(String[]args)throws Exception {
  SourceSelfFinalFrameLoad.main(args);var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());
  try(var files=Files.list(Path.of(args[1]))){for(var path:files.filter(p->p.toString().endsWith(".jar")).toList())urls.add(path.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())) {
   for(String name:List.of("NativeSourceMassTeleportExit","NativeSourceMassTeleportExit$Proof","NativeAreaMaterialAction","NativeLightAction")) {
    var type=Class.forName("local.lito381.server."+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();
    if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("mass exit owner must resolve from this exact candidate");
    if(name.equals("NativeSourceMassTeleportExit")&&type.getDeclaredFields().length!=0)throw new AssertionError("mass exit helper must have no static registry/TLS/worker");
   }
  }
  System.out.println("PASS inherited self no-init loader plus4 explicit mass-teleport exit owners from the same candidate");
 }
}
