import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaDoorsMethods {
    static final String P="local/lito381/server/", C=P+"NativeCloseoutNpcAreaTargets", H=P+"NativeCloseoutNpcAreaDoors";
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,C);int count=0;
            for(var m:c.methods)if(m.name.equals("eligible"))for(var n:m.instructions.toArray())
                if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleSpellCatalog")&&call.name.equals("hasNativeLineOfSight")) {
                    if(!call.desc.equals("(L"+P+"ArenaMapCatalog$Arena;IIII)Z"))throw new AssertionError("original LOS ABI");
                    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,0));call.owner=H;call.name="lineOfSight";
                    call.desc=call.desc.replace(")Z","L"+C+"$Selection;)Z");count++;
                }
            if(count!=1)throw new AssertionError("one shared NPC-area target guard");
            var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));
        }
    }
}
