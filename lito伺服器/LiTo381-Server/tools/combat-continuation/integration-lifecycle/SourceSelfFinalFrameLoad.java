import java.nio.file.*;import java.net.*;import java.util.*;
public final class SourceSelfFinalFrameLoad {
 public static void main(String[]a)throws Exception{
  SourceMaterialMainLoad.main(a);var jar=Path.of(a[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(a[1]))){for(var path:files.filter(p->p.toString().endsWith(".jar")).toList())urls.add(path.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){for(var name:List.of("NativeSourceSelfFinalFrame","NativeSourceSelfFinalFrame$Participant","NativeSourceSelfFinalFrame$First","NativeSourceSelfFinalFrame$SelfTask","NativeSourceSelfFinalFrame$Scope","NativeSourceSelfFinalFrame$Output","NativeLightAction","NativeLightAction$Batch","NativeLightAction$Ingress","ClientSession","NativeSourceWeaponFinalFrame","NativeSourceBatchTerminal","NativeSourcePlayerActionLock")){var type=Class.forName("local.lito381.server."+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same candidate self final owner");if(name.equals("NativeSourceSelfFinalFrame")&&type.getDeclaredFields().length!=0)throw new AssertionError("fieldless self final guard");}}
  System.out.println("PASS404 inherited runtime no-init coverage plus13 explicit self ingress/final owners");
 }
}
