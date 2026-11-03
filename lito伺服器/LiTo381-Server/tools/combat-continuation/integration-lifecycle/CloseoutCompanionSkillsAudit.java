import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionSkillsAudit {
    static final String P="local/lito381/server/",C=P+"BattleCompanion",H=P+"NativeCloseoutCompanionSkills",F="nativeCloseoutMobSkillState",D="(L"+C+";)V";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){
        boolean pet=c.name.equals(C);int installs=0,terminals=0,ticks=0;
        if(pet){var f=c.fields.stream().filter(x->x.name.equals(F)).findFirst().orElseThrow();need(f.access==ACC_VOLATILE&&f.desc.equals("Ljava/lang/Object;"),"published actor-owned state");c.fields.remove(f);}
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
            need(call.getOpcode()==INVOKESTATIC&&call.desc.equals(D),"helper ABI");
            var load=n.getPrevious();need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==(pet?0:1),"actual actor argument");
            if(call.name.equals("install")){
                need(pet&&(m.name.equals("<init>")||m.name.equals("evolve")),"factory construction/evolution only");
                need(load.getPrevious() instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)&&f.name.equals("nativeSourceNpcFactoryType"),"state immediately after accepted factory write");installs++;
            }else if(call.name.equals("terminal")){
                need(pet&&(m.access&ACC_SYNCHRONIZED)!=0,"terminal actor monitor");
                need(load.getPrevious() instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)
                    &&(m.name.equals("applyDamage")&&f.name.equals("hp")||f.name.equals("active")&&Set.of("deactivate","apply","applyDamage").contains(m.name)),"original HP/active write");terminals++;
            }else{
                need(call.name.equals("tick")&&!pet&&Set.of("serverCompanionTick","serverClassicWorldCompanionTick").contains(m.name)&&m.desc.startsWith("(L"+C+";"),"actual BATTLE/WORLD ticks");
                need(load==m.instructions.getFirst(),"before original ingress and target gates");ticks++;
            }
            m.instructions.remove(load);m.instructions.remove(n);
        }
        need(pet?installs==2&&terminals==4&&ticks==0:installs==0&&terminals==0&&ticks==2,"exact seam counts");
    }
    static void helper(JarFile jar)throws Exception{
        var c=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutCompanionSkills");
        var finish=c.methods.stream().filter(m->m.name.equals("finish")).findFirst().orElseThrow();int fills=0,epoch=0;
        for(var n:finish.instructions) {
            if(n instanceof MethodInsnNode call&&call.owner.equals("java/util/Arrays")&&call.name.equals("fill")){need(call.desc.equals("([II)V"),"only integer use-count array cleared");fills++;}
            if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD){need(f.owner.equals(H+"$State")&&f.name.equals("generation"),"finish only invalidates generation");epoch++;}
        }need(fills==1&&epoch==1,"count-only AI finish and lease invalidation");
        var lease=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutCompanionSkills$Lease");
        var delay=lease.methods.stream().filter(m->m.name.equals("startDelay")).findFirst().orElseThrow();int widen=0,mul=0;
        for(var n:delay.instructions){if(n.getOpcode()==I2L)widen++;if(n.getOpcode()==LMUL)mul++;}
        need(widen==1&&mul==1,"long nanosecond conversion");
        for(var name:List.of("NativeCloseoutCompanionSkills","NativeCloseoutCompanionSkills$State","NativeCloseoutCompanionSkills$Lease")){
            var x=CloseoutAreaOutputAudit.read(jar,name);
            for(var m:x.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call)
                need(!call.owner.startsWith("java/util/concurrent/")&&!call.owner.contains("Random")&&!call.name.startsWith("send")&&!call.owner.startsWith("java/sql/"),"no new scheduler/RNG/output/DB in state adapter");
        }
    }
    public static void main(String[]args)throws Exception{
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:List.of("BattleCompanion","ClientSession")){
                var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"original field retained");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(Set.of(C+".class",P+"ClientSession.class").contains(e.getName()))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            helper(candidate);
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained across BattleCompanion and ClientSession. Two factory-state assignments, four HP/active terminal seams and two existing companion ticks. AI finish clears only counts and lease generation, retaining cooldown deadlines. No new scheduler, RNG, packet or database calls. Effect adapters and remaining no-target finish cases are not qualified. No game execution or tests.\n");
    }
}
