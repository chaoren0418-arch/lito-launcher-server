import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaOutputAudit {
    static final String P="local/lito381/server/", H=P+"NativeCloseoutNpcAreaOutput";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void load(AbstractInsnNode n,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"typed original output local");}
    public static void main(String[]args)throws Exception {
        int methods=0,finish=0,frames=0,cipher=0;
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])) {
            for(var name:List.of("ClientSession","NativeSourceNpcMagicEffects")) {
                var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);
                for(var m:b.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode c) {
                    if(name.equals("ClientSession")&&c.owner.equals(P+"NativeSourceNpcMagicEffects")&&c.name.equals("allowFrame"))cipher++;
                    if(!c.owner.equals(H))continue;
                    if(c.name.equals("finish")) {
                        need(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name)&&c.desc.equals("(L"+P+"GameHub$MonsterAiTick;Ljava/lang/Object;Ljava/util/List;Ljava/util/List;)Z"),"bounded post-calculation finish");
                        var hits=n.getPrevious();load(hits,m.name.contains("Quest")?12:11);var selected=hits.getPrevious();load(selected,5);var snapshot=selected.getPrevious();load(snapshot,2);var tick=snapshot.getPrevious();load(tick,1);
                        var branch=n.getNext();need(branch instanceof JumpInsnNode j&&j.getOpcode()==IFNE,"handled path skips legacy output");
                        var destination=((JumpInsnNode)branch).label.getNext();need(destination instanceof VarInsnNode&&destination.getNext() instanceof MethodInsnNode leave&&leave.owner.equals(P+"NativeCombatAttacker")&&leave.name.equals("leave")&&destination.getNext().getNext().getOpcode()==RETURN,"original leave/return preserved");
                        need(branch.getNext() instanceof FieldInsnNode f&&f.name.equals("ATTACK_SPELL_SEQUENCE"),"before legacy sequence and packets");
                        for(var remove:List.of(tick,snapshot,selected,hits,n,branch))m.instructions.remove(remove);finish++;
                    } else {
                        need(name.equals("NativeSourceNpcMagicEffects")&&m.name.equals("allowFrame")&&c.name.equals("allowFrame")&&c.desc.equals("(ZL"+P+"ClientSession;[B)Z")&&n.getNext().getOpcode()==IRETURN,"original result is additional final guard input");
                        var packet=n.getPrevious();load(packet,1);var pc=packet.getPrevious();load(pc,0);m.instructions.remove(packet);m.instructions.remove(pc);m.instructions.remove(n);frames++;
                    }
                }
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"original body "+name+"."+x.name);methods++;}
            }
            need(finish==2&&frames==1&&cipher==8,"two completions and eight existing final cipher routes");
            for(var e:Collections.list(base.entries())){if(e.getName().equals(P+"ClientSession.class")||e.getName().equals(P+"NativeSourceNpcMagicEffects.class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"unrelated archive entry "+e.getName());}
            var out=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcAreaOutput$Output");
            need(out.fields.stream().noneMatch(f->f.desc.contains("Selection")||f.desc.contains("Scope")||f.desc.contains("List")||f.desc.contains("Attempt")),"output retains no scope, roster or cast receipts");
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcAreaOutput");int packet=0;
            int fallback=0;for(var m:helper.methods)if(m.name.equals("finish"))for(var n:m.instructions)
                if(n instanceof IntInsnNode value&&value.operand==19)fallback++;
            need(fallback==1,"Source ACTION_SkillBuff 19 fallback");
            for(var m:helper.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode c){
                need(!c.owner.contains("Random")&&!c.owner.startsWith("java/sql/")&&!c.owner.contains("Scheduled")&&!c.name.startsWith("schedule"),"no RNG/SQL/scheduling");
                if(c.owner.equals(P+"Protocol")){need(c.name.equals("objectAction"),"only Source NPC action packet");packet++;}
            }need(packet==1,"one canonical action format");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original ClientSession/NPC-effect methods retained. Two empty-self-cast completions bypass legacy sequence/attack graphics; original leave/finally remain. Eight original cipher paths retain their complete bodies plus one conjunctive output guard. Other archive entries byte-identical; output holds no cast roster/scope/receipts. Success-only scheduling and nonempty GMBUFF execution remain open. No tests, game execution or database connections.\n");
    }
}
