import java.nio.file.*;import java.net.*;import java.util.*;
public final class SourceWeaponFinalFrameLoad {
    public static void main(String[]args)throws Exception{
        SourceWeaponPlayerCounterLoad.main(args);var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());
        try(var files=Files.list(Path.of(args[1]))){for(var path:files.filter(p->p.toString().endsWith(".jar")).toList())urls.add(path.toUri().toURL());}
        try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
            for(var name:List.of("NativeSourceWeaponFinalFrame","NativeSourceWeaponFinalFrame$Participant","NativeSourceWeaponFinalFrame$Npc","NativeSourceWeaponFinalFrame$Proof","NativeSourceWeaponFinalFrame$Output","ClientSession","NativeStaticNpcMotion","NativeWeaponBreakTargetLifecycle")){
                var type=Class.forName("local.lito381.server."+name,false,loader);type.getDeclaredFields();type.getDeclaredMethods();type.getDeclaredConstructors();
                if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("candidate weapon final frame owner");
                if(name.equals("NativeSourceWeaponFinalFrame")&&type.getDeclaredFields().length!=0)throw new AssertionError("fieldless final guard");
            }
        }
        System.out.println("PASS400 inherited runtime no-init coverage plus8 explicit weapon detached-final owners");
    }
}
