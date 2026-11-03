import java.nio.file.*;import java.net.*;import java.util.*;
public final class SourceMaterialMainLoad {
 public static void main(String[]a)throws Exception{
  SourceMaterialPairIngressLoad.main(a);var jar=Path.of(a[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(a[1]))){for(var path:files.filter(p->p.toString().endsWith(".jar")).toList())urls.add(path.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){for(var name:List.of("NativeSourceMaterialMain","NativeLightAction")){var type=Class.forName("local.lito381.server."+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same candidate material Main owner");if(name.equals("NativeSourceMaterialMain")&&type.getDeclaredFields().length!=0)throw new AssertionError("fieldless Main guard");}}
  System.out.println("PASS403 inherited runtime no-init coverage plus2 explicit material Main owners");
 }
}
