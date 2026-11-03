import java.nio.file.*;
import java.net.*;
import java.util.*;
public final class SourceWeaponCounterMagicLoad {
    public static void main(String[]args)throws Exception{
        SourceWeaponTargetLifeLoad.main(args);
        var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());
        try(var files=Files.list(Path.of(args[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
            for(var name:List.of("NativeSourceWeaponCounterMagic","NativeWeaponBreakBattleCompanion")){
                var c=Class.forName("local.lito381.server."+name,false,loader);
                c.getDeclaredFields();c.getDeclaredMethods();c.getDeclaredConstructors();
                if(!Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("candidate weapon Counter Magic owner");
                if(name.equals("NativeSourceWeaponCounterMagic")&&c.getDeclaredFields().length!=0)throw new AssertionError("fieldless bounded Counter Magic helper");
            }
        }
        System.out.println("PASS398 inherited runtime no-init coverage plus2 explicit weapon Counter Magic owners");
    }
}
