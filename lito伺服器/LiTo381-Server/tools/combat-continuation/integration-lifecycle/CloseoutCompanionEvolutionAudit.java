import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionEvolutionAudit {
    static final String P="local/lito381/server/",C=P+"BattleCompanion",H=P+"NativeCloseoutCompanionEvolution",F="nativeSourceNpcFactoryType";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){
        var field=c.fields.stream().filter(f->f.name.equals(F)).findFirst().orElseThrow();need(field.access==ACC_VOLATILE&&field.desc.equals("Ljava/lang/Object;"),"visible replacement proof");field.access=ACC_FINAL;
        int count=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
            need(m.name.equals("evolve")&&(m.access&ACC_SYNCHRONIZED)!=0&&call.name.equals("capture")&&call.desc.equals("(L"+C+";L"+C+"$Profile;)Ljava/lang/Object;"),"capture in original synchronized evolution only");
            var dup=n.getPrevious();var save=n.getNext();var profile=save.getNext();var self=profile.getNext();var load=self.getNext();var write=load.getNext();
            need(dup.getOpcode()==DUP2&&dup.getPrevious() instanceof MethodInsnNode ctor&&ctor.owner.equals(C+"$Profile")&&ctor.name.equals("<init>")&&save instanceof VarInsnNode sv&&sv.getOpcode()==ASTORE,"complete original replacement profile before capture");
            need(profile instanceof FieldInsnNode pf&&pf.getOpcode()==PUTFIELD&&pf.owner.equals(C)&&pf.name.equals("profile")&&self instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&load instanceof VarInsnNode b&&b.getOpcode()==ALOAD&&b.var==((VarInsnNode)save).var&&write instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)&&f.name.equals(F),"same accepted profile and proof updated under original lock");
            for(var x:List.of(dup,n,save,self,load,write))m.instructions.remove(x);count++;
        }need(count==1,"one evolution proof seam");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            var a=CloseoutAreaOutputAudit.read(base,"BattleCompanion");var b=CloseoutAreaOutputAudit.read(candidate,"BattleCompanion");inverse(b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts");
            for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"original fields after flag inversion");}
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+x.name);methods++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(C+".class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original BattleCompanion methods retained. One accepted synchronized evolution profile seam captures replacement Source proof before mutation and writes it adjacent to the original profile assignment; only the proof field changes from final to volatile. Constructor, validation, persistence and other archive entries retained. No game execution or tests.\n");
    }
}
