import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaCastAudit {
    static final String P="local/lito381/server/", H=P+"NativeCloseoutNpcAreaCast", S="NativeCloseoutNpcAreaTargets$Selection";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void load(AbstractInsnNode n,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"original typed local");}
    public static void main(String[]args)throws Exception {
        int calls=0,methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])) {
            for(var name:List.of("ClientSession",S)) {
                var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);
                if(name.equals(S))need(b.fields.removeIf(f->f.name.equals("nativeCloseoutGmBuffResults")&&f.desc.equals("Ljava/lang/Object;")&&f.access==ACC_TRANSIENT),"invocation-local transient cast outcomes");
                else for(var m:b.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode c&&c.owner.equals(H)) {
                    need(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name)&&c.name.equals("calculate")&&c.desc.equals("(Ljava/util/List;L"+P+"ClientSession;L"+P+"BattleTimeAttackMagicSkillCatalog$Definition;)Z"),"bounded GMBUFF calculation branch");
                    var skill=n.getPrevious();load(skill,4);var pc=skill.getPrevious();need(pc instanceof VarInsnNode,"target local");var list=pc.getPrevious();load(list,5);
                    var old=list.getPrevious();var next=n.getNext();need(old instanceof JumpInsnNode o&&o.getOpcode()==IFEQ&&next instanceof JumpInsnNode q&&q.getOpcode()==IFEQ&&o.label==q.label,"same continue as original current-target guard");
                    need(old.getPrevious() instanceof MethodInsnNode guard&&guard.owner.equals(P+"NativeCloseoutNpcAreaTargets")&&guard.name.equals("allowed"),"after original first-participant guard");
                    load(old.getPrevious().getPrevious(),((VarInsnNode)pc).var);load(next.getNext(),4);
                    need(next.getNext().getNext() instanceof MethodInsnNode capture&&capture.owner.equals(P+"NativeCloseoutNpcSkillValues")&&capture.name.equals("capture"),"before scalar capture and RNG");
                    for(var remove:List.of(list,pc,skill,n,next))m.instructions.remove(remove);calls++;
                }
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original members");
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            need(calls==2,"two pre-calculation branches");
            for(var e:Collections.list(base.entries())){if(e.getName().equals(P+"ClientSession.class")||e.getName().equals(P+S+".class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcAreaCast");
            for(var m:helper.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode c)
                need(!c.owner.contains("Random")&&!c.owner.startsWith("java/sql/")&&!c.owner.contains("Scheduled")&&!c.owner.equals(P+"Protocol")&&!c.owner.equals(P+"NativePlayerCommit")&&!c.owner.equals(P+"NativeSourceDamageDisplay"),"empty self cast has no RNG, SQL, schedule, packet or damage submission");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original ClientSession/Selection methods retained; two branches after first-participant guards and before scalar capture/RNG. TYPE_GMBUFF empty self-target outcomes are invocation-local and distinct from HP acceptance. All other archive entries byte-identical. Nonempty GMBUFF execution and success-only NPC action/scheduling remain open. No tests, game execution or database connection.\n");
    }
}
