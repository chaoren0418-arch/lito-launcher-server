import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcSpellAdmissionMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcSpellAdmission",ROW="L"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;",VALUES="L"+P+"NativeCloseoutNpcSkillValues$Values;";
    static final Set<String> SCHEDULE=Set.of("scheduleMagic","scheduleVisual","scheduleGround"),READY=Set.of("magicReady","visualReady","groundReady");
    static void patch(ClassNode c){
        if(c.name.endsWith("$Monster")){
            int count=0;
            for(var old:List.copyOf(c.methods))if(SCHEDULE.contains(old.name)){
                var copy=new MethodNode(old.access,"nativeCloseoutAdmitted"+old.name.substring(8),old.desc.replace(")",VALUES+")"),old.signature,old.exceptions.toArray(String[]::new));old.accept(copy);copy.name="nativeCloseoutAdmitted"+old.name.substring(8);
                for(var n:copy.instructions.toArray()){if(n instanceof VarInsnNode v&&v.var>=4)v.var++;if(n instanceof IincInsnNode v&&v.var>=4)v.var++;}
                copy.maxLocals++;int captured=0;
                for(var n:copy.instructions.toArray())if(n instanceof MethodInsnNode call&&Set.of(P+"NativeCloseoutNpcSkillValues",P+"NativeCloseoutNpcAuxCost").contains(call.owner)&&call.name.equals("captureCost")){
                    var arg=n.getPrevious();var self=arg.getPrevious();if(!(self instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&arg instanceof VarInsnNode b&&b.getOpcode()==ALOAD&&b.var==3))throw new AssertionError("original one cost capture");
                    copy.instructions.remove(self);copy.instructions.remove(arg);copy.instructions.set(n,new VarInsnNode(ALOAD,4));captured++;
                }if(captured!=1)throw new AssertionError("one supplied immutable cost row");c.methods.add(copy);count++;
            }if(count!=(c.name.contains("Quest")?1:3))throw new AssertionError("original schedule siblings");return;
        }
        int silence=0,ready=0,attempts=0;
        for(var m:c.methods)if(m.name.equals("aiAttackFor")&&m.desc.contains("[I")){
            if((m.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("original encounter monitor");
            LabelNode fallback=null;int targets=0;
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals("java/util/List")&&call.name.equals("isEmpty")&&n.getNext() instanceof JumpInsnNode j&&j.getOpcode()==IFNE){fallback=j.label;targets++;}
            if(targets!=1)throw new AssertionError("one original empty-eligible physical fallback");
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(c.name+"$Monster")){
                if(call.name.equals("silenceActive")){
                    m.instructions.insertBefore(n,new VarInsnNode(ALOAD,15));call.setOpcode(INVOKESTATIC);call.owner=H;call.name="selectionSilence";call.desc="(Ljava/lang/Object;J"+ROW+")Z";silence++;
                }else if(READY.contains(call.name)){
                    call.setOpcode(INVOKESTATIC);call.owner=H;call.name="selectionReady";call.desc="(Ljava/lang/Object;"+ROW+"J)Z";ready++;
                }else if(SCHEDULE.contains(call.name)){
                    call.setOpcode(INVOKESTATIC);call.owner=H;call.name="attempt";call.desc="(Ljava/lang/Object;J"+ROW+")Z";m.instructions.insert(n,new JumpInsnNode(IFEQ,fallback));attempts++;
                }
            }
        }
        int expected=c.name.contains("Quest")?1:3;if(silence!=expected||ready!=expected||attempts!=expected)throw new AssertionError("selection/attempt seams "+silence+"/"+ready+"/"+attempts);
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("BattleQuestEncounter","BattleTimeAttackEncounter","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
