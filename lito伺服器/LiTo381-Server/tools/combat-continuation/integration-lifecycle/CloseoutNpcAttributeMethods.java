import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAttributeMethods {
    static final String P="local/lito381/server/",D=P+"BattleTimeAttackMagicSkillCatalog$Definition",H=P+"NativeCloseoutNpcAttribute";
    static void patch(ClassNode c) {
        int count=0;
        for(var m:c.methods)if(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name)) {
            int sites=0;
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(D)&&call.name.equals("attribute")&&call.desc.equals("()I")) {
                if(!(call.getNext() instanceof MethodInsnNode next)||!next.owner.equals(c.name)||!next.name.equals("effectiveSpellElementalResistance")||!next.desc.equals("(I)I"))throw new AssertionError("original target resistance read");
                call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+D+";)I";sites++;
            }
            if(sites!=1)throw new AssertionError("one attribute read per producer");count++;
        }
        if(count!=2)throw new AssertionError("two producers");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){
            FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,P+"ClientSession");patch(c);var out=Path.of(args[1]).resolve(P+"ClientSession.class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));
        }
    }
}
