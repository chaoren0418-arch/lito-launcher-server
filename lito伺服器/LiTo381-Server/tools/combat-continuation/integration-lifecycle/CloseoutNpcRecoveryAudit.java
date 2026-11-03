import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcRecoveryAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcRecovery",S=P+"NativeSourceNpcFactoryType$Stamp";
    static final String OLD="(Ljava/lang/String;ILjava/lang/String;IIZZZIILjava/lang/Object;)V",EXT=OLD.replace(")","Ljava/lang/Object;)");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){
        if(c.name.equals(S)){
            var field=c.fields.stream().filter(f->f.name.equals("nativeCloseoutNpcRecovery")).findFirst().orElseThrow();need(field.access==ACC_FINAL&&field.desc.equals("Ljava/lang/Object;"),"one frozen non-record component");c.fields.remove(field);
            var extended=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(EXT)).findFirst().orElseThrow();var old=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(OLD)).findFirst().orElseThrow();int count=0;
            for(var n:extended.instructions.toArray())if(n instanceof FieldInsnNode f&&f.owner.equals(S)&&f.name.equals("nativeCloseoutNpcRecovery")){
                var value=n.getPrevious();var self=value.getPrevious();need(n.getOpcode()==PUTFIELD&&value instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==12&&self instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&n.getNext().getOpcode()==RETURN,"extended immutable birth value");extended.instructions.remove(self);extended.instructions.remove(value);extended.instructions.remove(n);count++;
            }
            need(count==1&&CloseoutAreaOutputAudit.text(old).equals(CloseoutAreaOutputAudit.text(extended)),"complete original constructor body");c.methods.remove(extended);return;
        }
        if(c.name.equals(P+"NativeSourceNpcFactoryType")){
            int count=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(S)&&call.name.equals("<init>")&&call.desc.equals(EXT)){
                var capture=n.getPrevious();var row=capture.getPrevious();need(m.name.equals("row")&&capture instanceof MethodInsnNode h&&h.getOpcode()==INVOKESTATIC&&h.owner.equals(H)&&h.name.equals("capture")&&row instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==2,"exact original birth template capture");m.instructions.remove(row);m.instructions.remove(capture);call.desc=OLD;count++;
            }need(count==1,"one original factory row extension");return;
        }
        var field=c.fields.stream().filter(f->f.name.equals("nativeCloseoutNpcRecoveryClock")).findFirst().orElseThrow();need(field.access==(ACC_PRIVATE|ACC_VOLATILE)&&field.desc.equals("Ljava/lang/Object;"),"one Hub-owned clock");c.fields.remove(field);int open=0,close=0;
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
            var self=n.getPrevious();need(self instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"same Hub receiver");
            if(call.name.equals("init")){need(m.name.equals("<init>")&&self.getPrevious() instanceof MethodInsnNode prior&&prior.owner.equals(P+"NativePlayerLifetime")&&prior.name.equals("openHub"),"init after original lifetime registration");open++;}
            else{need(call.name.equals("close")&&m.name.equals("close")&&self==m.instructions.getFirst(),"close before original shutdown");close++;}
            m.instructions.remove(self);m.instructions.remove(n);
        }need(open==1&&close==1,"one Hub open/close seam");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("NativeSourceNpcFactoryType$Stamp","NativeSourceNpcFactoryType","GameHub");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts "+name);
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"retained original field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained across Hub and factory classes. One extended constructor preserves complete old body and record equality; one birth-data capture and one Hub init/close pair. Other original archive entries are byte-identical. No application execution or tests.\n");
    }
}
