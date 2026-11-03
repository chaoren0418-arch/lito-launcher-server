import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaOutputMethods {
    static final String P="local/lito381/server/", C=P+"ClientSession", E=P+"NativeSourceNpcMagicEffects", H=P+"NativeCloseoutNpcAreaOutput";
    static void patch(ClassNode c) {
        int count=0;
        for(var m:c.methods) {
            if(c.name.equals(C)&&Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name)) {
                for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);
                AbstractInsnNode start=null;LabelNode cleanup=null;
                for(var n:m.instructions) {
                    if(n instanceof FieldInsnNode f&&f.owner.equals(C)&&f.name.equals("ATTACK_SPELL_SEQUENCE"))start=n;
                    if(n.getOpcode()==RETURN&&n.getPrevious() instanceof MethodInsnNode call&&call.owner.equals(P+"NativeCombatAttacker")&&call.name.equals("leave")) {
                        var label=n.getPrevious().getPrevious().getPrevious();if(label instanceof LabelNode l)cleanup=l;
                    }
                }
                if(start==null||cleanup==null)throw new AssertionError("post-calculation and original cleanup boundaries");
                var x=new InsnList();x.add(new VarInsnNode(ALOAD,1));x.add(new VarInsnNode(ALOAD,2));x.add(new VarInsnNode(ALOAD,5));
                x.add(new VarInsnNode(ALOAD,m.name.contains("Quest")?12:11));
                x.add(new MethodInsnNode(INVOKESTATIC,H,"finish","(L"+P+"GameHub$MonsterAiTick;Ljava/lang/Object;Ljava/util/List;Ljava/util/List;)Z",false));
                x.add(new JumpInsnNode(IFNE,cleanup));m.instructions.insertBefore(start,x);count++;
            }
            if(c.name.equals(E)&&m.name.equals("allowFrame"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN) {
                var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,1));
                x.add(new MethodInsnNode(INVOKESTATIC,H,"allowFrame","(ZL"+C+";[B)Z",false));m.instructions.insertBefore(n,x);count++;
            }
        }
        if(count!=(c.name.equals(C)?2:1))throw new AssertionError("exact output hooks");
    }
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of(C,E)) {var c=FreezeAcceptedMethods.read(jar,name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
        }
    }
}
