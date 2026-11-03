import java.nio.file.*;import java.util.*;import java.net.*;import java.lang.reflect.*;
public final class SourceDollDetachedOutputLoad {
 public static void main(String[]a)throws Exception{
  SourceManualActionScopeLoad.main(a);var jar=Path.of(a[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(a[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){var c=Class.forName("local.lito381.server.NativeSourceDollDetachedOutput",false,loader);if(!Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar)||c.getDeclaredFields().length!=0)throw new AssertionError("candidate fieldless helper");c.getDeclaredMethods();c.getDeclaredConstructors();var pc=Class.forName("local.lito381.server.ClientSession",false,loader);var m=pc.getDeclaredMethod("nativeSourceDollDetachedFinalFrame",byte[].class,String.class,Object.class);if(!Modifier.isSynchronized(m.getModifiers()))throw new AssertionError("original final receiver monitor");}
  System.out.println("PASS341 candidate no-init classes including fieldless detached doll output and original receiver monitor");
 }
}
