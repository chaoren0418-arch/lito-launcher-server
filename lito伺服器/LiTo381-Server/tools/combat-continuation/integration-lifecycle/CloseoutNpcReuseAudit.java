import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/** Original bodies are compared after removing bounded cooldown seams. No application loading. */
public final class CloseoutNpcReuseAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcReuse",T=P+"NativeSourceNpcSpellTiming";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void arguments(MethodNode m,AbstractInsnNode call,int... slots){
        var n=call.getPrevious();for(int i=slots.length-1;i>=0;i--){need(n instanceof VarInsnNode v&&v.var==slots[i],"exact existing local argument");var previous=n.getPrevious();m.instructions.remove(n);n=previous;}
    }
    static void inverse(ClassNode c){
        if(c.name.equals(T)){
            int calls=0;
            for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(T+"$Route")&&call.name.equals("<init>")&&call.desc.equals("(IIIII)V")){
                var max=n.getPrevious();var reuse=max.getPrevious();var index=reuse.getPrevious();var mob=index.getPrevious();var zero=mob.getPrevious();
                need(max instanceof MethodInsnNode x&&x.owner.equals("java/lang/Math")&&x.name.equals("max")&&reuse instanceof MethodInsnNode y&&y.owner.equals(P+"NativeDeNpcMobSkill")&&y.name.equals("getReuseDelay")&&zero.getOpcode()==ICONST_0,"birth reuse scalar");
                for(var item:List.of(max,reuse,index,mob,zero))m.instructions.remove(item);call.desc="(IIII)V";calls++;
            }need(calls==1,"one frozen birth route extension");return;
        }
        int distance=0,scheduled=0,roll=0,summon=0;
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
            if(call.name.equals("distance")){arguments(m,n,10,15,5);need(n.getPrevious() instanceof MethodInsnNode old&&old.name.equals("triggerDistance"),"original distance result");m.instructions.remove(n);distance++;}
            else if(call.name.equals("scheduled")){arguments(m,n,10,16,5);need(n.getPrevious() instanceof MethodInsnNode old&&old.owner.equals(c.name+"$Monster")&&old.name.startsWith("schedule"),"after original successful schedule");m.instructions.remove(n);scheduled++;}
            else if(call.name.equals("roll")){arguments(m,n,8,9,11,5);call.setOpcode(INVOKEVIRTUAL);call.owner="java/util/concurrent/ThreadLocalRandom";call.name="nextInt";call.desc="(II)I";roll++;}
            else if(call.name.equals("summonRoll")){arguments(m,n,8,11,5);need(n.getPrevious() instanceof MethodInsnNode old&&old.name.equals("summonRandomBound"),"original summon bound");call.setOpcode(INVOKEVIRTUAL);call.owner="java/util/concurrent/ThreadLocalRandom";call.name="nextInt";call.desc="(I)I";summon++;}
            else throw new AssertionError("unexpected helper call");
        }
        need(distance==1&&scheduled==(c.name.endsWith("BattleQuestEncounter")?3:5)&&roll==1&&summon==1,"bounded cooldown seam counts");
    }
    public static void main(String[]args)throws Exception{
        var owners=List.of("BattleQuestEncounter","BattleTimeAttackEncounter","NativeSourceNpcSpellTiming");int methods=0;
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member count "+name);
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"unchanged field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained method "+name+"."+x.name);methods++;}
            }
            var oldRoute=CloseoutAreaOutputAudit.read(base,"NativeSourceNpcSpellTiming$Route");var route=CloseoutAreaOutputAudit.read(candidate,"NativeSourceNpcSpellTiming$Route");
            need(route.fields.size()==oldRoute.fields.size()+1&&route.fields.stream().anyMatch(f->f.name.equals("reuse")&&f.desc.equals("I")&&(f.access&ACC_FINAL)!=0),"one immutable reuse component");
            need(route.methods.stream().anyMatch(m->m.name.equals("<init>")&&m.desc.equals("(IIII)V"))&&route.methods.stream().anyMatch(m->m.name.equals("<init>")&&m.desc.equals("(IIIII)V")),"old route constructor retained plus extended canonical constructor");
            for(var name:List.of("type","skill","width","height")){var a=oldRoute.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();var b=route.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();need(CloseoutAreaOutputAudit.text(a).equals(CloseoutAreaOutputAudit.text(b)),"original route accessor retained");}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(T+"$Route.class")||owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained across two encounter classes and timing helper; eight post-schedule seams, two readiness gates, four bounded RNG seams. Route adds one frozen reuse component and preserves its old constructor/accessors; record equality intentionally includes reuse. No application execution or tests.\n");
    }
}
