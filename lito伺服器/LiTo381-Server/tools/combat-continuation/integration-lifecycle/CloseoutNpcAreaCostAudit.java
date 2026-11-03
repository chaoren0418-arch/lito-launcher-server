import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaCostAudit {
    static final String P="local/lito381/server/",C=P+"NativeCloseoutNpcSpellAdmission",H=P+"NativeCloseoutNpcAreaCost",ROW="L"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void var(AbstractInsnNode n,int op,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot,"same existing actor/row/time arguments");}
    static void inverse(ClassNode c){int count=0;for(var m:c.methods)if(Set.of("selectionSilence","selectionReady","attempt").contains(m.name)){
        boolean ready=m.name.equals("selectionReady");int row=ready?1:3,time=ready?2:1;var ns=m.instructions.toArray();var(ns[0],ALOAD,0);var(ns[1],ALOAD,row);
        need(ns[2] instanceof MethodInsnNode a&&a.getOpcode()==INVOKESTATIC&&a.owner.equals(H)&&a.name.equals("matches")&&a.desc.equals("(Ljava/lang/Object;"+ROW+")Z"),"frozen area-route match");need(ns[3] instanceof JumpInsnNode j&&j.getOpcode()==IFEQ,"normal path falls through");int i=4;
        if(m.name.equals("selectionSilence"))need(ns[i++].getOpcode()==ICONST_0,"area path bypasses ordinary NPC silence admission");
        else{var(ns[i++],ALOAD,0);if(ready){var(ns[i++],ALOAD,row);var(ns[i++],LLOAD,time);}else{var(ns[i++],LLOAD,time);var(ns[i++],ALOAD,row);}need(ns[i] instanceof MethodInsnNode a&&a.getOpcode()==INVOKESTATIC&&a.owner.equals(H)&&a.name.equals(ready?"ready":"attempt")&&a.desc.equals(m.desc),"original typed admission signature");i++;}
        need(ns[i++].getOpcode()==IRETURN&&ns[i]==((JumpInsnNode)ns[3]).label,"one area return before original body");for(int j=0;j<=i;j++)m.instructions.remove(ns[j]);count++;
    }need(count==3,"exact area branches");}
    static void helper(JarFile jar)throws Exception{
        var c=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutNpcAreaCost");int zeros=0,metadata=0,width=0,height=0;
        for(var m:c.methods)for(var n:m.instructions){if(n instanceof MethodInsnNode call){
            need(!call.name.equals("captureCost")&&!call.name.equals("consumeSilenceForSpell")&&!call.name.equals("payHp")&&!call.owner.contains("Random")&&!call.owner.startsWith("java/sql/"),"no NPC normal-cast cost, silence mutation, RNG or SQL");
            if(call.owner.equals(P+"NativeSkillTargetMetadata")&&call.name.equals("row"))metadata++;
            if(call.owner.equals(P+"NativeSourceNpcSpellTiming$Route")&&call.name.equals("width"))width++;
            if(call.owner.equals(P+"NativeSourceNpcSpellTiming$Route")&&call.name.equals("height"))height++;
            if(call.owner.equals(P+"NativeCloseoutNpcSkillValues$Values")&&call.name.equals("<init>")){
                var at=n.getPrevious();for(int i=0;i<5;i++){need(at.getOpcode()==ICONST_0,"zero HP/MP/damage placeholders used only by scheduling");at=at.getPrevious();}need(at.getOpcode()==ICONST_1,"sourced zero NPC cost");zeros++;
            }
        }}need(zeros==1&&metadata==1&&width==1&&height==1,"one selected metadata check and zero-cost snapshot with both frozen dimensions");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            var a=CloseoutAreaOutputAudit.read(base,"NativeCloseoutNpcSpellAdmission");var b=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcSpellAdmission");inverse(b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original normal/legacy admission "+x.name);methods++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(C+".class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}helper(candidate);
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original admission methods retained. Three area-route prefixes identify Source positive width/height from birth data; existing four typed call sites use zero NPC HP/MP and skip normal silence admission. Existing scheduling bodies, RNG, ordinary/legacy paths and all other entries retained. TYPE_GMBUFF target semantics, map93/visibility/LOS and success-only scheduling remain open. No tests or game execution.\n");
    }
}
