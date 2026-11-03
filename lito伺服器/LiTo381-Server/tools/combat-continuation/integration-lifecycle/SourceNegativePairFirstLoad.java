import java.nio.file.*;import java.net.*;import java.util.*;import java.lang.reflect.Modifier;
public final class SourceNegativePairFirstLoad {
 public static void main(String[]args)throws Exception {
  SourceControlledNegativeFinalLoad.main(args);var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(args[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
   String p="local.lito381.server.";for(String name:List.of("NativeSourceNegativePairFirst","NativeSourceNegativePairFirst$First","NativeSourceNegativeTargets$Aim","NativeAreaMaterialAction")){
    var type=Class.forName(p+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same candidate owner");
    if(name.equals("NativeSourceNegativePairFirst")&&type.getDeclaredFields().length!=0)throw new AssertionError("no new global roots");
    if(name.equals("NativeSourceNegativeTargets$Aim")){var f=type.getDeclaredField("nativeSourceNegativePairFirst");if(f.getType()!=Object.class||!Modifier.isTransient(f.getModifiers())||Modifier.isStatic(f.getModifiers()))throw new AssertionError("transient bounded Aim field only");}
   }
  }System.out.println("PASS inherited controlled-negative no-init plus4 negative first-intent owners; one transient Aim slot and zero new global roots");
 }
}
