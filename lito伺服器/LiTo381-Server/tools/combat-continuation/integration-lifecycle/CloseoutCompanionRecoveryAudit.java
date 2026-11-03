import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionRecoveryAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutCompanionRecovery";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){
        if(c.name.equals(P+"BattleCompanion")||c.name.equals(P+"NativeCompanionPlacement$State")){
            String field=c.name.endsWith("BattleCompanion")?"profile":"pending";
            var f=c.fields.stream().filter(x->x.name.equals(field)).findFirst().orElseThrow();need((f.access&ACC_VOLATILE)!=0,"final-frame field visibility");f.access&=~ACC_VOLATILE;return;
        }
        if(c.name.equals(P+"NativeSourceNpcMagicEffects")){
            int count=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
                var packet=n.getPrevious();var pc=packet.getPrevious();need(m.name.equals("allowFrame")&&call.name.equals("allowFrame")&&call.desc.equals("(ZL"+P+"ClientSession;[B)Z")&&call.getOpcode()==INVOKESTATIC&&pc instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&packet instanceof VarInsnNode b&&b.getOpcode()==ALOAD&&b.var==1&&n.getNext().getOpcode()==IRETURN,"postguard original result before return");m.instructions.remove(pc);m.instructions.remove(packet);m.instructions.remove(n);count++;
            }need(count==1,"exact original frame guard extension");return;
        }
        var field=c.fields.stream().filter(f->f.name.equals("nativeCloseoutCompanionRecoveryClock")).findFirst().orElseThrow();need(field.access==(ACC_PRIVATE|ACC_VOLATILE)&&field.desc.equals("Ljava/lang/Object;"),"one Hub-owned clock");c.fields.remove(field);int open=0,close=0;
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
            var self=n.getPrevious();need(self instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"same Hub receiver");
            if(call.name.equals("init")){need(m.name.equals("<init>")&&self.getPrevious() instanceof MethodInsnNode prior&&prior.owner.equals(P+"NativeCloseoutNpcRecovery")&&prior.name.equals("init"),"init after existing NPC recovery");open++;}
            else{need(call.name.equals("close")&&m.name.equals("close")&&self==m.instructions.getFirst(),"close before original shutdown");close++;}
            m.instructions.remove(self);m.instructions.remove(n);
        }need(open==1&&close==1,"one Hub open/close seam");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("GameHub","BattleCompanion","NativeCompanionPlacement$State","NativeSourceNpcMagicEffects");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts");
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"retained field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            var client=CloseoutAreaOutputAudit.read(candidate,"ClientSession");int frames=0;
            for(var m:client.methods){int guard=-1,encrypt=-1,i=0;for(var n:m.instructions){if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"NativeSourceNpcMagicEffects")&&call.name.equals("allowFrame"))guard=i;if(call.name.equals("encrypt"))encrypt=i;}i++;}if(guard>=0){need(encrypt>guard,"guard precedes original encryption in "+m.name);frames++;}}
            need(frames==8,"all eight original complete encryption routes");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained across four owners; Hub init/close, one postguard and two visibility flags only. Eight unchanged ClientSession encryption routes retain final guard before encryption. All other original archive entries byte-identical. No application execution or tests.\n");
    }
}
