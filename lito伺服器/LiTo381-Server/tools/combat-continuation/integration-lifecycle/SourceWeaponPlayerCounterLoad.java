import java.nio.file.*;
import java.net.*;
import java.util.*;
public final class SourceWeaponPlayerCounterLoad {
    public static void main(String[]args)throws Exception{
        SourceWeaponCounterMagicLoad.main(args);
        var jar=Path.of(args[0]).toRealPath();var urls=new ArrayList<URL>();urls.add(jar.toUri().toURL());
        try(var files=Files.list(Path.of(args[1]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
            for(var name:List.of("NativeSourceWeaponPlayerCounter","ClientSession","NativeLightAction","NativeWeaponBreakBattleCompanion")){
                var c=Class.forName("local.lito381.server."+name,false,loader);
                c.getDeclaredFields();c.getDeclaredMethods();c.getDeclaredConstructors();
                if(!Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("candidate weapon Counter Magic owner");
                if(name.equals("NativeSourceWeaponPlayerCounter")&&c.getDeclaredFields().length!=0)throw new AssertionError("fieldless bounded Counter Magic helper");
            }
        }
        System.out.println("PASS399 inherited runtime no-init coverage plus4 explicit weapon PC counter owners");
    }
}
