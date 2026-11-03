import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaTargetsAudit {
    static final String P="local/lito381/server/", H=P+"NativeCloseoutNpcAreaTargets";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void load(AbstractInsnNode n,int op,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot,"typed area argument");}
    static void inverse(MethodNode m) {
        int select=0,allowed=0;
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode c&&c.owner.equals(H)) {
            if(c.name.equals("select")) {
                select++;need(c.desc.equals("(L"+P+"ClientSession;L"+P+"GameHub$MonsterAiTick;Ljava/lang/Object;)Ljava/util/List;"),"selection signature");
                var first=n.getPrevious().getPrevious().getPrevious();var a=first;for(int i=0;i<3;i++){load(a,ALOAD,i);a=a.getNext();}need(a==n,"three first ingress arguments");
                a=n.getNext();need(a instanceof VarInsnNode v&&v.getOpcode()==ASTORE,"private selection local");int slot=((VarInsnNode)a).var;
                a=a.getNext();load(a,ALOAD,slot);a=a.getNext();need(a instanceof JumpInsnNode j&&j.getOpcode()==IFNULL,"legacy only on null");var fallback=((JumpInsnNode)a).label;
                a=a.getNext();load(a,ALOAD,slot);a=a.getNext();load(a,ASTORE,5);a=a.getNext();need(a instanceof JumpInsnNode j&&j.getOpcode()==GOTO,"skip old radius selection");
                var merge=((JumpInsnNode)a).label;need(merge.getNext() instanceof MethodInsnNode clock&&clock.owner.equals("java/lang/System")&&clock.name.equals("nanoTime"),"original post-selection merge");
                need(a.getNext()==fallback,"bounded replacement");load(fallback.getNext(),ALOAD,4);
                need(fallback.getNext().getNext() instanceof MethodInsnNode t&&t.name.equals("targeting"),"before first original targeting");
                while(first!=fallback){var next=first.getNext();m.instructions.remove(first);first=next;}m.instructions.remove(fallback);
            } else if(c.name.equals("allowed")) {
                allowed++;need(c.desc.equals("(Ljava/util/List;L"+P+"ClientSession;)Z"),"guard signature");
                var first=n.getPrevious().getPrevious();load(first,ALOAD,5);var target=n.getPrevious();need(target instanceof VarInsnNode,"selected player local");
                load(first.getPrevious(),ASTORE,((VarInsnNode)target).var);need(first.getPrevious().getPrevious() instanceof TypeInsnNode cast&&cast.desc.equals(P+"ClientSession"),"guard immediately after original iterator target");
                var jump=n.getNext();need(jump instanceof JumpInsnNode j&&j.getOpcode()==IFEQ,"reject continues original iterator");
                var loop=((JumpInsnNode)jump).label;need(loop.getNext().getNext() instanceof MethodInsnNode it&&it.owner.equals("java/util/Iterator")&&it.name.equals("hasNext"),"continue hasNext");
                load(jump.getNext(),ALOAD,4);need(jump.getNext().getNext() instanceof MethodInsnNode cap&&cap.owner.equals(P+"NativeCloseoutNpcSkillValues")&&cap.name.equals("capture"),"before values and damage RNG");
                m.instructions.remove(first);m.instructions.remove(target);m.instructions.remove(n);m.instructions.remove(jump);
            } else throw new AssertionError("unexpected new helper call");
        }
        need(select==1&&allowed==1,"one selection and per-target guard");
    }
    public static void main(String[]args)throws Exception {
        int count=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])) {
            var a=CloseoutAreaOutputAudit.read(base,"ClientSession");var b=CloseoutAreaOutputAudit.read(candidate,"ClientSession");
            for(var m:b.methods)if(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name))inverse(m);
            need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+x.name);count++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(P+"ClientSession.class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcAreaTargets");int screen=0,los=0,same=0;
            for(var m:helper.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode c){
                need(!c.owner.contains("Random")&&!c.owner.startsWith("java/sql/")&&!c.owner.contains("Scheduled")&&!c.name.equals("set"),"no RNG/SQL/scheduler/actor writes");
                if(c.owner.equals(P+"BattleSpellCatalog")&&c.name.equals("isInNativeScreen"))screen++;
                if(c.owner.equals(P+"BattleSpellCatalog")&&c.name.equals("hasNativeLineOfSight"))los++;
                if(c.owner.equals(P+"NativeSourceNpcTickView")&&c.name.equals("same"))same++;
            }need(screen==1&&los==1&&same==1,"shared Source geometry and original NPC proof");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+count+" original ClientSession methods retained; two source-area selections before radius/directional branches and two current first-participant checks before scalar capture/RNG. Other archive entries byte-identical. No application execution, tests or database connections. TYPE_GMBUFF, success-only scheduling, dynamic doors and Source spatial iteration order remain open.\n");
    }
}
