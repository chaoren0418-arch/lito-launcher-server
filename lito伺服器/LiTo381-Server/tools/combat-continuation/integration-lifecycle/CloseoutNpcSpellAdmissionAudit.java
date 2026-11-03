import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcSpellAdmissionAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcSpellAdmission",ROW="L"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;",VALUES="L"+P+"NativeCloseoutNpcSkillValues$Values;";
    static final Set<String> NAMES=Set.of("silenceActive","magicReady","visualReady","groundReady","scheduleMagic","scheduleVisual","scheduleGround");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode a,ClassNode b){
        if(b.name.endsWith("$Monster")){
            int count=0;
            for(var copy:List.copyOf(b.methods))if(copy.name.startsWith("nativeCloseoutAdmitted")){
                String name="schedule"+copy.name.substring("nativeCloseoutAdmitted".length());var old=a.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
                need(copy.desc.equals(old.desc.replace(")",VALUES+")"))&&copy.access==old.access,"one supplied values argument");
                MethodInsnNode originalCapture=null;for(var n:old.instructions)if(n instanceof MethodInsnNode call&&call.name.equals("captureCost"))originalCapture=call;
                need(originalCapture!=null,"original capture retained in original schedule");var first=copy.instructions.getFirst();
                need(first instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==4&&first.getNext() instanceof VarInsnNode save&&save.getOpcode()==ASTORE&&save.var==6,"frozen cost replaces one original capture");
                var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,3));x.add(new MethodInsnNode(originalCapture.getOpcode(),originalCapture.owner,originalCapture.name,originalCapture.desc,originalCapture.itf));copy.instructions.insertBefore(first,x);copy.instructions.remove(first);
                for(var n:copy.instructions.toArray()){if(n instanceof VarInsnNode v&&v.var>=5)v.var--;if(n instanceof IincInsnNode v&&v.var>=5)v.var--;}
                need(CloseoutAreaOutputAudit.text(old).equals(CloseoutAreaOutputAudit.text(copy)),"admitted schedule has complete original readiness/payment/timing body "+name);
                b.methods.remove(copy);count++;
            }need(count==(b.name.contains("Quest")?1:3),"four complete schedule siblings");return;
        }
        int count=0;
        for(var m:b.methods)if(m.name.equals("aiAttackFor")&&m.desc.contains("[I")){
            var old=a.methods.stream().filter(x->x.name.equals(m.name)&&x.desc.equals(m.desc)).findFirst().orElseThrow();var calls=new ArrayDeque<MethodInsnNode>();
            for(var n:old.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(a.name+"$Monster")&&NAMES.contains(call.name))calls.add(call);
            LabelNode fallback=null;for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals("java/util/List")&&call.name.equals("isEmpty")&&n.getNext() instanceof JumpInsnNode j&&j.getOpcode()==IFNE)fallback=j.label;
            need(fallback!=null&&(m.access&ACC_SYNCHRONIZED)!=0,"original fallback and encounter lock");
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
                var prior=calls.removeFirst();need(call.getOpcode()==INVOKESTATIC,"explicit static adapter");
                if(call.name.equals("selectionSilence")){
                    need(prior.name.equals("silenceActive")&&call.desc.equals("(Ljava/lang/Object;J"+ROW+")Z"),"original silence gate ABI");var row=n.getPrevious();need(row instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==15,"same enumerated row before eligibility");m.instructions.remove(row);
                }else if(call.name.equals("selectionReady"))need(prior.name.endsWith("Ready")&&call.desc.equals("(Ljava/lang/Object;"+ROW+"J)Z"),"original ready gate ABI");
                else{
                    need(call.name.equals("attempt")&&prior.name.startsWith("schedule")&&call.desc.equals("(Ljava/lang/Object;J"+ROW+")Z"),"selected row admission before original schedule");var branch=n.getNext();need(branch instanceof JumpInsnNode j&&j.getOpcode()==IFEQ&&j.label==fallback,"failure uses exact original empty-eligible physical fallback");m.instructions.remove(branch);
                }
                call.setOpcode(prior.getOpcode());call.owner=prior.owner;call.name=prior.name;call.desc=prior.desc;call.itf=prior.itf;count++;
            }need(calls.isEmpty(),"all corresponding original calls restored");
        }need(count==(b.name.contains("Quest")?3:9),"four triple-boundary admissions");
    }
    static void helper(JarFile jar)throws Exception{
        var c=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutNpcSpellAdmission");var attempt=c.methods.stream().filter(m->m.name.equals("attempt")).findFirst().orElseThrow();int silence=-1,captures=0,index=0;
        for(var n:attempt.instructions){if(n instanceof LdcInsnNode v&&Objects.equals(v.cst,"consumeSilenceForSpell"))silence=index;if(n instanceof MethodInsnNode call&&call.name.equals("captureCost")){need(silence>=0&&index>silence,"selected silence attempt precedes either scalar capture");captures++;}index++;}need(captures==2,"one mutually exclusive primary/aux cost capture per attempt");
        for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call)need(!call.owner.contains("Random")&&!call.owner.startsWith("java/sql/")&&!call.name.contains("Send"),"no new RNG/SQL/output");
        var select=c.methods.stream().filter(m->m.name.equals("selectionReady")).findFirst().orElseThrow();for(var n:select.instructions)if(n instanceof MethodInsnNode call)need(!call.name.equals("captureCost")&&!call.name.equals("payHp"),"eligibility does not capture/pay costs");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("BattleQuestEncounter","BattleTimeAttackEncounter","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(a,b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"retained original field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}helper(candidate);
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained across four owners. Four typed normal-spell lanes move ordinary silence/cost admission after row selection; failure reaches the original physical fallback before reuse bookkeeping or attack construction. Four added schedule siblings retain original bodies with a supplied immutable cost snapshot. Birth-frozen positive width/height area routes and unloaded legacy behavior keep their existing gates. No new RNG, SQL, packets, tests or game execution.\n");
    }
}
