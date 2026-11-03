import java.nio.file.*;import java.net.*;import java.util.*;import java.lang.reflect.Modifier;
public final class SourceAreaPairFirstLoad {
 public static void main(String[]args)throws Exception{
  SourceNegativeFinalFrameLoad.main(args);var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());try(var files=Files.list(Path.of(args[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
   String p="local.lito381.server.";for(String name:List.of("NativeSourceAreaPairFirst","NativeSourceAreaPairFirst$First","NativeSourceAreaPairFirst$Cursor","NativeAreaMaterialAction$Context","NativeAreaMaterialAction","NativeSourceMassSlowTargets$Aim","NativeSourceWindTargets$Aim","NativeSourceAreaPlayerProof","ClientSession")){
    var type=Class.forName(p+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("same area-first candidate owner");
    if(name.equals("NativeSourceAreaPairFirst")&&type.getDeclaredFields().length!=0)throw new AssertionError("no new global roots");
    if(name.equals("NativeAreaMaterialAction$Context")){var f=type.getDeclaredField("nativeSourceAreaPairFirst");if(f.getType()!=Object.class||!Modifier.isTransient(f.getModifiers())||Modifier.isStatic(f.getModifiers()))throw new AssertionError("bounded transient Context field only");}
   }
  }System.out.println("PASS inherited negative-final no-init plus9 area primary/secondary owners; one transient Context slot and zero new global roots");
 }
}
