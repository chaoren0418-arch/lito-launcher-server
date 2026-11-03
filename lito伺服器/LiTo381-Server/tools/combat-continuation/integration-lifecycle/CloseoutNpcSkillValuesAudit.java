import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/** Independent inverse: retain all original conditions, RNG loops, writes and scheduling. */
public final class CloseoutNpcSkillValuesAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcSkillValues",V=H+"$Values",D=P+"BattleTimeAttackMagicSkillCatalog$Definition";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void remove(MethodNode m,AbstractInsnNode... nodes){for(var n:nodes)m.instructions.remove(n);}
    static void inverse(ClassNode c){
        if(c.name.equals(P+"CombatDatabase")){
            var m=c.methods.stream().filter(x->x.name.equals("nativeCloseoutSnapshot")).findFirst().orElseThrow();
            var ops=Arrays.stream(m.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
            need(m.access==ACC_STATIC&&m.desc.equals("()L"+P+"CombatDatabase$Snapshot;")&&ops.size()==2&&ops.get(0) instanceof FieldInsnNode f&&f.getOpcode()==GETSTATIC&&f.owner.equals(c.name)&&f.name.equals("current")&&ops.get(1).getOpcode()==ARETURN,"readonly snapshot accessor");c.methods.remove(m);return;
        }
        int getters=0,captures=0,guards=0,payments=0,readyCalls=0;
        for(var m:c.methods){
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(V)){
                need(Set.of("mp","baseDamage","diceCount","dieSides").contains(call.name)&&call.desc.equals("()I"),"numeric getter");
                if(call.name.equals("baseDamage")){
                    var store=n.getPrevious();var dup=store.getPrevious();var capture=dup.getPrevious();
                    need(store instanceof VarInsnNode s&&s.getOpcode()==ASTORE&&dup.getOpcode()==DUP&&capture instanceof MethodInsnNode q&&q.owner.equals(H)&&q.name.equals("capture"),"single first damage row");remove(m,store,dup,capture);
                }else{
                    var load=n.getPrevious();var pop=load.getPrevious();need(load instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&pop.getOpcode()==POP,"original definition operand replaced by captured values");remove(m,load,pop);
                }
                call.owner=D;if(call.name.equals("mp"))call.name="mpCost";getters++;
            }
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
                if(call.name.equals("captureCost")){
                    var load=n.getPrevious();var actor=load.getPrevious();var store=n.getNext();need(actor==m.instructions.getFirst()&&actor instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&load instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&store instanceof VarInsnNode s&&s.getOpcode()==ASTORE,"bounded cost capture prefix");remove(m,actor,load,n,store);captures++;
                }else{
                    need(Set.of("ready","payHp").contains(call.name),"explicit cost helper");var values=n.getPrevious();var actor=values.getPrevious();
                    need(values instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&actor instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0,"original actor and captured costs");
                    if(call.name.equals("ready")){need(n.getNext().getOpcode()==IRETURN,"original readiness result");guards++;}
                    else{need(actor.getPrevious() instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(c.name)&&f.name.equals("mp"),"HP payment adjacent to original MP write");payments++;}
                    remove(m,actor,values,n);
                }
            }
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("nativeCloseoutMagicReady")){
                var values=n.getPrevious();need(values instanceof VarInsnNode v&&v.getOpcode()==ALOAD,"captured readiness argument");remove(m,values);call.name="magicReady";call.desc="(L"+D+";J)Z";readyCalls++;
            }
        }
        if(c.name.equals(P+"ClientSession")){need(getters==6&&captures==0&&guards==0&&payments==0&&readyCalls==0,"two original three-value damage loops");return;}
        var original=c.methods.stream().filter(m->m.name.equals("magicReady")).findFirst().orElseThrow();
        var sibling=c.methods.stream().filter(m->m.name.equals("nativeCloseoutMagicReady")).findFirst().orElseThrow();
        need(sibling.desc.equals("(L"+D+";JL"+V+";)Z")&&CloseoutAreaOutputAudit.text(original).equals(CloseoutAreaOutputAudit.text(sibling)),"complete original captured readiness sibling");
        c.methods.remove(sibling);need(getters==3&&captures==2&&guards==2&&payments==1&&readyCalls==1,"exact NPC cost seams");
    }
    public static void main(String[]args)throws Exception{
        var owners=List.of("ClientSession","CombatDatabase","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster");int methods=0;
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts "+name);
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"unchanged field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: four original classes / "+methods+" methods retained; two complete captured readiness siblings; one readonly snapshot accessor; original RNG loops and mobskill reuse retained. No application execution or tests.\n");
    }
}
