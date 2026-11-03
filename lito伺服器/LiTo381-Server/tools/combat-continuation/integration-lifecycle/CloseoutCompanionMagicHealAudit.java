import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionMagicHealAudit {
    static final String P="local/lito381/server/",C=P+"NativeSourceNpcMagicEffects",H=P+"NativeCloseoutCompanionMagicHeal",S=C+"$Source",SCOPE=P+"NativeSourceNpcPlayer$Scope";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void inverse(ClassNode c){int prefixes=0,frames=0;
        for(var m:c.methods){
            if(Set.of("source","sourceCurrent","heal").contains(m.name)){
                var nodes=m.instructions.toArray();String arg=m.name.equals("source")?SCOPE:S;
                String pred=m.name.equals("source")?"controlled":"handles";
                String delegate=switch(m.name){case "source"->"capture";case "sourceCurrent"->"currentSource";default->"heal";};
                need(nodes[0] instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0,"same source or scope");
                need(nodes[1] instanceof MethodInsnNode a&&a.getOpcode()==INVOKESTATIC&&a.owner.equals(H)&&a.name.equals(pred)&&a.desc.equals("(L"+arg+";)Z"),"controlled-only prefix predicate");
                need(nodes[2] instanceof JumpInsnNode j&&j.getOpcode()==IFEQ,"natural source falls through original");
                need(nodes[3] instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0,"same delegated source");int at=4;
                if(m.name.equals("heal")){need(nodes[at] instanceof VarInsnNode a&&a.getOpcode()==ILOAD&&a.var==1,"same accepted heal amount");at++;}
                need(nodes[at] instanceof MethodInsnNode a&&a.getOpcode()==INVOKESTATIC&&a.owner.equals(H)&&a.name.equals(delegate)&&a.desc.equals(m.desc),"typed delegate ABI");at++;
                need(nodes[at++].getOpcode()==(m.name.equals("source")?ARETURN:m.name.equals("sourceCurrent")?IRETURN:RETURN),"controlled branch returns");
                need(nodes[at]==((JumpInsnNode)nodes[2]).label,"original body target");for(int i=0;i<=at;i++)m.instructions.remove(nodes[i]);prefixes++;
            }
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
                var packet=n.getPrevious();var pc=packet.getPrevious();need(m.name.equals("allowFrame")&&call.name.equals("allowFrame")&&call.desc.equals("(ZL"+P+"ClientSession;[B)Z")&&call.getOpcode()==INVOKESTATIC&&pc instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0&&packet instanceof VarInsnNode b&&b.getOpcode()==ALOAD&&b.var==1&&n.getNext().getOpcode()==IRETURN,"postguard original result before return");m.instructions.remove(pc);m.instructions.remove(packet);m.instructions.remove(n);frames++;
            }
        }need(prefixes==3&&frames==1,"exact source/heal/output seams");
    }
    static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static void helper(JarFile candidate)throws Exception{
        var c=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutCompanionMagicHeal");
        int pair=-1,send=-1,i=0;for(var n:method(c,"heal").instructions){if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"NativeSourcePlayerActionLock")&&call.name.equals("runPairOrdered"))pair=i;if(call.owner.equals(H)&&call.name.equals("send"))send=i;}i++;}need(pair>=0&&send>pair,"send after original owner action/mutation scope returns");
        var mutation=c.methods.stream().filter(m->m.name.startsWith("lambda$heal$")).findFirst().orElseThrow();int monitors=0,checks=0,writes=0;
        for(var n:mutation.instructions){if(n.getOpcode()==MONITORENTER)monitors++;if(n instanceof MethodInsnNode call){if(call.owner.equals(H)&&call.name.equals("currentSource"))checks++;if(call.owner.equals(P+"CombatAccess")&&call.name.equals("set")){need(checks==2,"recheck after all mutation locks");writes++;}need(!call.name.contains("Send")&&!call.name.equals("send")&&!call.owner.startsWith("java/sql/"),"no output/DB under mutation locks");}}
        need(monitors==5&&writes==1,"lifetime/Hub/bag/pet/status monitors and one bounded HP write");
        var proof=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutCompanionMagicHeal$Proof");
        need(proof.fields.stream().filter(f->f.name.equals("bag")).allMatch(f->f.desc.equals("Ljava/lang/ref/WeakReference;")),"roster identity is weak");
        for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call){
            need(!(call.owner.equals(P+"NativePlayerCommit$Binding")&&call.name.equals("capture"))&&!call.name.equals("ownedCompanion"),"no recaptured master or actor by id");
            need(!call.owner.contains("Random")&&!call.owner.startsWith("java/sql/")&&!call.name.equals("schedule"),"no RNG/DB/scheduler in effect source");
        }
        var client=CloseoutAreaOutputAudit.read(candidate,"ClientSession");int frames=0;
        for(var m:client.methods){int guard=-1,encrypt=-1,index=0;for(var n:m.instructions){if(n instanceof MethodInsnNode call){if(call.owner.equals(C)&&call.name.equals("allowFrame"))guard=index;if(call.name.equals("encrypt"))encrypt=index;}index++;}if(guard>=0){need(encrypt>guard,"guard before encryption "+m.name);frames++;}}need(frames==8,"eight complete cipher paths");
        var drain=method(CloseoutAreaOutputAudit.read(candidate,"NativePlayerVitalCompletions"),"drain");int unlock=-1,run=-1,index=0;boolean deferred=false,held=false;
        for(var n:drain.instructions){if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"NativePlayerActionScope")&&call.name.equals("deferDrain"))deferred=true;if(call.name.equals("isHeldByCurrentThread"))held=true;if(call.name.equals("unlock"))unlock=index;if(call.owner.equals("java/lang/Runnable")&&call.name.equals("run"))run=index;}index++;}
        need(deferred&&held&&unlock>=0&&run>unlock,"unchanged accepted completion defers active scopes and runs after action unlock");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            var a=CloseoutAreaOutputAudit.read(base,"NativeSourceNpcMagicEffects");var b=CloseoutAreaOutputAudit.read(candidate,"NativeSourceNpcMagicEffects");inverse(b);
            need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+x.name);methods++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(C+".class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            helper(candidate);
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original NPC magic effect methods retained; three controlled-source branches and one final guard. Existing accepted-only claim, natural actor effects, damage admission, complete cipher paths and completion scheduler are unchanged. Controlled heal uses five mutation monitors after owner action admission, sends after lock release, and rejects replaced first actor/master proofs. Eight cipher paths guard before encryption. No tests or game execution.\n");
    }
}
