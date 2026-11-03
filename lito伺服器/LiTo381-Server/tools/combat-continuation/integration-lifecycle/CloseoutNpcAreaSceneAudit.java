import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaSceneAudit {
    static final String P="local/lito381/server/", OLD=P+"NativeCloseoutNpcSpellAdmission", H=P+"NativeCloseoutNpcAreaScene";
    static final String D="(Ljava/lang/Object;JL"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;)Z";
    static final Set<String> OWNERS=Set.of("BattleQuestEncounter","BattleTimeAttackEncounter");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c) {
        int count=0;
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)) {
            need(m.name.equals("aiAttackFor")&&m.desc.contains("[I")&&(m.access&ACC_SYNCHRONIZED)!=0,"only original synchronized selected attempt");
            need(call.getOpcode()==INVOKESTATIC&&call.name.equals("attempt")&&call.desc.equals(D.replace(")","Ljava/lang/Object;)")),"one added encounter parameter");
            need(n.getPrevious() instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"actual encounter this");
            need(n.getNext() instanceof JumpInsnNode j&&j.getOpcode()==IFEQ,"same failure jump before schedule reuse and attack construction");
            m.instructions.remove(n.getPrevious());call.owner=OLD;call.desc=D;count++;
        }
        need(count==(c.name.contains("Quest")?1:3),"four original selected lane sites");
    }
    static void helper(JarFile jar)throws Exception {
        var c=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutNpcAreaScene");
        int map=0,blocked=0,match=0,delegate=0,held=0,arenaField=0,arenasField=0,force=0;
        for(var m:c.methods)for(var n:m.instructions) {
            need(n.getOpcode()!=PUTFIELD&&n.getOpcode()!=PUTSTATIC&&n.getOpcode()!=MONITORENTER&&n.getOpcode()!=MONITOREXIT,"no state writes or new lock order");
            if(n instanceof IntInsnNode v&&v.operand==93)blocked++;
            if(n instanceof LdcInsnNode v&&v.cst instanceof String value) {if(value.equals("arena"))arenaField++;if(value.equals("arenas"))arenasField++;if(value.equals("force"))force++;}
            if(n instanceof MethodInsnNode call) {
                need(!call.owner.contains("Random")&&!call.owner.startsWith("java/sql/")&&!call.name.startsWith("schedule")&&!call.name.equals("set"),"no RNG, SQL, scheduling or mutation");
                if(call.owner.equals(P+"ArenaMapCatalog$Arena")&&call.name.equals("mapId"))map++;
                if(call.owner.equals("java/lang/Thread")&&call.name.equals("holdsLock"))held++;
                if(call.owner.equals(P+"NativeCloseoutNpcAreaCost")&&call.name.equals("matches"))match++;
                if(call.owner.equals(OLD)&&call.name.equals("attempt"))delegate++;
            }
        }
        need(map==1&&blocked==1&&match==1&&delegate==1&&held==1&&arenaField==1&&arenasField==1&&force==1,"area-only map93 gate, original delegate and actual force/encounter map access");
        var m=c.methods.stream().filter(x->x.name.equals("attempt")).findFirst().orElseThrow();
        var calls=new ArrayList<String>();for(var n:m.instructions)if(n instanceof MethodInsnNode a)calls.add(a.owner+"."+a.name);
        need(calls.equals(List.of(P+"NativeCloseoutNpcAreaCost.matches",H+".arena",P+"ArenaMapCatalog$Arena.mapId",OLD+".attempt")),"spatial gate precedes all original admission/payment/scheduling");
    }
    public static void main(String[]args)throws Exception {
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])) {
            for(var name:OWNERS) {
                var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts retained");
                for(int i=0;i<a.methods.size();i++) {var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"original body retained "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())) {if(OWNERS.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            helper(candidate);
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original encounter methods retained. Four selected spell attempts carry their actual synchronized encounter to an area-only map93 check. Quest uses the original force-indexed arena; Time Attack uses its owning arena. Rejection reaches original physical fallback before NPC cost, silence mutation, spell scheduling/reuse or spell construction. No new locks, state, SQL, RNG or output. All other existing archive entries retained. TYPE_GMBUFF self-casts, visible-player LOS and success-only scheduling remain open. No tests or game execution.\n");
    }
}
