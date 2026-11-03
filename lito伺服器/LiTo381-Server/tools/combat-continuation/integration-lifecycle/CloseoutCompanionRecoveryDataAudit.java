import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionRecoveryDataAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutCompanionRecoveryData",S=P+"NativeSourceNpcFactoryType$Stamp";
    static final String OLD="(Ljava/lang/String;ILjava/lang/String;II)V",EXT=OLD.replace(")","Ljava/lang/Object;)");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){
        if(c.name.equals(S)){
            var extended=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(EXT)).findFirst().orElseThrow();var old=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(OLD)).findFirst().orElseThrow();int count=0;
            for(var n:extended.instructions.toArray())if(n instanceof FieldInsnNode f&&f.owner.equals(S)&&f.name.equals("nativeCloseoutNpcRecovery")){
                var value=n.getPrevious();var self=value.getPrevious();need(n.getOpcode()==PUTFIELD&&value instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==6&&self instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&n.getNext().getOpcode()==RETURN,"controlled frozen data assignment");extended.instructions.remove(self);extended.instructions.remove(value);extended.instructions.remove(n);count++;
            }need(count==1&&CloseoutAreaOutputAudit.text(old).equals(CloseoutAreaOutputAudit.text(extended)),"complete original constructor body");c.methods.remove(extended);return;
        }
        int count=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(S)&&call.name.equals("<init>")&&call.desc.equals(EXT)){
            var capture=n.getPrevious();var npc=capture.getPrevious();need(m.name.equals("controlled")&&capture instanceof MethodInsnNode h&&h.getOpcode()==INVOKESTATIC&&h.owner.equals(H)&&h.name.equals("capture")&&npc instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==0,"same original controlled npc id");m.instructions.remove(npc);m.instructions.remove(capture);call.desc=OLD;count++;
        }need(count==2,"exact pet and summon factory branches");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("NativeSourceNpcFactoryType$Stamp","NativeSourceNpcFactoryType");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts");
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"retained field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original factory methods retained; complete old constructor in new sibling and two controlled birth captures. Existing natural recovery, all old fields, record identity and unrelated archive entries retained. Companion timer/HP output still pending. No game execution or tests.\n");
    }
}
