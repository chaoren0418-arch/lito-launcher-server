import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaSceneMethods {
    static final String P="local/lito381/server/", OLD=P+"NativeCloseoutNpcSpellAdmission", H=P+"NativeCloseoutNpcAreaScene";
    static final String D="(Ljava/lang/Object;JL"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;)Z";
    static void patch(ClassNode c) {
        int count=0;
        for(var m:c.methods)if(m.name.equals("aiAttackFor")&&m.desc.contains("[I")) {
            if((m.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("original encounter monitor required");
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(OLD)&&call.name.equals("attempt")) {
                if(!call.desc.equals(D)||call.getOpcode()!=INVOKESTATIC||!(call.getNext() instanceof JumpInsnNode j)||j.getOpcode()!=IFEQ)throw new AssertionError("selected attempt with original failure fallback");
                m.instructions.insertBefore(call,new VarInsnNode(ALOAD,0));call.owner=H;call.desc=D.replace(")", "Ljava/lang/Object;)");count++;
            }
        }
        if(count!=(c.name.contains("Quest")?1:3))throw new AssertionError("four original selected spell lanes");
    }
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of("BattleQuestEncounter","BattleTimeAttackEncounter")) {
                var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));
            }
        }
    }
}
