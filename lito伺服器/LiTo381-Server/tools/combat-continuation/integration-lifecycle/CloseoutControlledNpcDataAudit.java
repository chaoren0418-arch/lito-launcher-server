import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutControlledNpcDataAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutControlledNpcData",B=H+"$Birth",S=P+"NativeSourceNpcFactoryType$Stamp",F="nativeCloseoutControlledNpcData";
    static final String OLD="(Ljava/lang/String;ILjava/lang/String;IILjava/lang/Object;)V",EXT=OLD.replace(")","Ljava/lang/Object;)");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){
        if(c.name.equals(S)){
            var field=c.fields.stream().filter(f->f.name.equals(F)).findFirst().orElseThrow();need(field.access==ACC_FINAL&&field.desc.equals("Ljava/lang/Object;"),"non-record immutable controlled data");c.fields.remove(field);
            var extended=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(EXT)).findFirst().orElseThrow();var old=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(OLD)).findFirst().orElseThrow();int count=0;
            for(var n:extended.instructions.toArray())if(n instanceof FieldInsnNode f&&f.owner.equals(S)&&f.name.equals(F)){
                var value=n.getPrevious();var self=value.getPrevious();need(n.getOpcode()==PUTFIELD&&value instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==7&&self instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&n.getNext().getOpcode()==RETURN,"frozen value after complete old constructor");extended.instructions.remove(self);extended.instructions.remove(value);extended.instructions.remove(n);count++;
            }need(count==1&&CloseoutAreaOutputAudit.text(old).equals(CloseoutAreaOutputAudit.text(extended)),"original constructor retained completely");c.methods.remove(extended);return;
        }
        int captures=0,constructors=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
            if(call.owner.equals(H)&&call.name.equals("capture")){
                need(m.name.equals("controlled")&&call.desc.equals("(I)L"+B+";"),"same controlled capture boundary");var dup=n.getNext();var recovery=dup.getNext();var swap=recovery.getNext();var skills=swap.getNext();
                need(dup.getOpcode()==DUP&&recovery instanceof MethodInsnNode r&&r.owner.equals(B)&&r.name.equals("recovery")&&r.desc.equals("()Ljava/lang/Object;")&&swap.getOpcode()==SWAP&&skills instanceof MethodInsnNode s&&s.owner.equals(B)&&s.name.equals("skills")&&s.desc.equals("()Ljava/lang/Object;"),"same birth snapshot unpacks both values");for(var x:List.of(dup,recovery,swap,skills))m.instructions.remove(x);call.owner=P+"NativeCloseoutCompanionRecoveryData";call.desc="(I)Ljava/lang/Object;";captures++;
            }
            if(call.owner.equals(S)&&call.name.equals("<init>")&&call.desc.equals(EXT)){need(m.name.equals("controlled"),"only controlled factory constructor");call.desc=OLD;constructors++;}
        }need(captures==2&&constructors==2,"exact pet and summon branches");
    }
    static List<String> calls(MethodNode m,String owner){var result=new ArrayList<String>();for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(owner))result.add(call.name);return result;}
    static void factsAndSelection(JarFile jar)throws Exception{
        var data=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutControlledNpcData");var capture=data.methods.stream().filter(m->m.name.equals("capture")).findFirst().orElseThrow();
        need(calls(capture,P+"NativeDeNpcTemplateResources").equals(List.of("snapshot")),"one readonly template snapshot per birth");
        var rowCalls=calls(capture,P+"NativeDeNpcMobSkill");
        for(var name:List.of("getType","getTriggerRandom","getTriggerHp","getTriggerCompanionHp","getTriggerRange","getTriggerCount","getChangeTarget","getRange","getAreaWidth","getAreaHeight","getLeverage","getSkillId","getGfxid","getActid","getSummon","getSummonMin","getSummonMax","getPolyId","getReuseDelay"))need(Collections.frequency(rowCalls,name)==1,"one scalar copy per Source row field "+name);
        var immutable=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutControlledNpcData$Data");need(immutable.methods.stream().anyMatch(m->m.name.equals("<init>")&&calls(m,"java/util/List").contains("copyOf")),"detached immutable row list");
        var selector=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutMobSkillSelection");String ports=P+"NativeCloseoutMobSkillSelection$Ports";
        var use=selector.methods.stream().filter(m->m.name.equals("use")).findFirst().orElseThrow();
        need(calls(use,ports).equals(List.of("physical","magic","summon","poly","shockStun","cancellation","weaponBreak","incrementUseCount","startDelay")),"seven explicit Source dispatch ports and count-before-delay completion");
        var usable=selector.methods.stream().filter(m->m.name.equals("usable")).findFirst().orElseThrow();var u=calls(usable,ports);
        need(u.get(0).equals("delayed")&&u.indexOf("nextInt")<u.indexOf("hp")&&u.indexOf("hp")<u.indexOf("distance")&&u.indexOf("distance")<u.indexOf("useCount"),"Source rejection and RNG ordering");
        need(Collections.frequency(u,"nextInt")==1,"one percentile RNG site");
        var select=selector.methods.stream().filter(m->m.name.equals("select")).findFirst().orElseThrow();need(Collections.frequency(calls(select,ports),"nextInt")==1,"one candidate choice RNG site");
        var change=selector.methods.stream().filter(m->m.name.equals("changeTarget")).findFirst().orElseThrow();need(Collections.frequency(calls(change,ports),"nextInt")==1,"one retarget RNG site");
        need(java.util.stream.StreamSupport.stream(Spliterators.spliteratorUnknownSize(change.instructions.iterator(),0),false).anyMatch(n->n.getOpcode()==IMUL)&&java.util.stream.StreamSupport.stream(Spliterators.spliteratorUnknownSize(change.instructions.iterator(),0),false).anyMatch(n->n.getOpcode()==IDIV),"Source retarget multiplied bound and divided index retained");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("NativeSourceNpcFactoryType$Stamp","NativeSourceNpcFactoryType");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts");
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"retained original field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
        }
        try(var candidate=new JarFile(args[1])){factsAndSelection(candidate);}
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original factory methods retained; old controlled constructor preserved in one extended sibling. Two factory branches capture recovery and complete mobskill scalar facts from one readonly snapshot. Existing evolution reaches the same factory. Record identity and unrelated entries retained. Static structure confirms one template snapshot, immutable complete scalar rows and Source selection call order/RNG sites; actual actor/effect adapters and tick binding remain pending; no game execution or tests.\n");
    }
}
