import java.net.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
public final class ArmorBoundaryProbe {
    static Object armor(Class<?> type,int ac,int material,int mask)throws Exception {
        var c=type.getDeclaredConstructors()[0];c.setAccessible(true);
        Object[] args=new Object[c.getParameterCount()];
        for(int i=0;i<args.length;i++)args[i]=c.getParameterTypes()[i]==boolean.class?false:0;
        args[0]=ac;args[1]=material;args[2]=1000;args[3]=mask;args[4]=-1;args[26]=9;
        return c.newInstance(args);
    }
    static byte[] detail(Class<?> type,Object armor)throws Exception {
        var m=type.getDeclaredMethod("detail",int.class,int.class,int.class,int.class);
        m.setAccessible(true);return (byte[])m.invoke(armor,9,1,0,0);
    }
    public static void main(String[] args)throws Exception {
        try(var before=new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()},ClassLoader.getPlatformClassLoader());
            var after=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
            String name="local.lito381.server.InventoryCatalog$NativeArmorStatus";
            var a=before.loadClass(name);var b=after.loadClass(name);
            for(int value=0;value<=255;value++)
                if(!Arrays.equals(detail(a,armor(a,value,3,127)),detail(b,armor(b,value,3,127))))
                    throw new AssertionError("legacy armor changed "+value);
            for(int value:new int[]{256,888,999999,Integer.MAX_VALUE}) {
                var item=armor(b,value,3,127);
                var getter=b.getDeclaredMethod("displayArmorClass");getter.setAccessible(true);
                if(!getter.invoke(item).equals(value))throw new AssertionError("raw AC truncated");
                if((detail(b,item)[1]&255)!=255)throw new AssertionError("wire AC wrapped");
            }
            for(int[] bad:new int[][]{{-1,3,127},{0,256,127},{0,3,128}}) {
                try {armor(b,bad[0],bad[1],bad[2]);throw new AssertionError("invalid metadata accepted");}
                catch(InvocationTargetException expected) {
                    if(!(expected.getCause() instanceof IllegalArgumentException))throw expected;
                }
            }
            System.out.println("PASS 256 legacy AC packets unchanged; 4 wide values retained/clamped only on wire; 3 invalid metadata cases rejected");
        }
    }
}
