import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/** Static inverse audit only; no application classes loaded. */
public final class CloseoutNpcAreaAttemptAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcAreaAttempt";
    static final List<String> OWNERS=List.of("GameHub","GameHub$MonsterAiTick","NativeCloseoutNpcAreaScene","ClientSession","NativeCloseoutNpcAreaDoors");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void prefix(MethodNode m,String call,int opcode,int result) {
        MethodInsnNode found=null;JumpInsnNode branch=null;
        for(var n:m.instructions){if(n instanceof MethodInsnNode c&&c.owner.equals(H)&&c.name.equals(call))found=c;if(n instanceof JumpInsnNode j){branch=j;break;}}
        need(found!=null&&branch!=null&&branch.getOpcode()==opcode,"bounded prefix "+call);
        need(branch.label.getPrevious().getOpcode()==result,"prefix return "+call);
        var end=branch.label;while(m.instructions.getFirst()!=end)m.instructions.remove(m.instructions.getFirst());m.instructions.remove(end);
    }
    static void inverse(ClassNode c){
        if(c.name.equals(P+"GameHub$MonsterAiTick")){need(c.fields.removeIf(f->f.name.equals("nativeCloseoutNpcAreaReceipt")&&f.desc.equals("Ljava/lang/Object;")&&(f.access&ACC_TRANSIENT)!=0),"transient completed receipt");return;}
        if(c.name.equals(P+"GameHub")){
            var m=c.methods.stream().filter(x->x.name.equals("nativeSourceNpcPlayerFirstAppend")).findFirst().orElseThrow();
            var handler=m.tryCatchBlocks.remove(m.tryCatchBlocks.size()-1);need(handler.type==null,"finally catches all original exits");
            var entry=m.instructions.getFirst();int entered=0;
            while(entry!=handler.start){var next=entry.getNext();if(entry instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("enter"))entered++;m.instructions.remove(entry);entry=next;}
            m.instructions.remove(handler.start);need(entered==1,"one synchronous context");
            int exceptional=0;for(AbstractInsnNode n=handler.end;n!=null;){var next=n.getNext();if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("leave")){need(n.getPrevious().getOpcode()==ICONST_0,"failed exit does not attach");exceptional++;}n=next;}
            for(AbstractInsnNode n=handler.end;n!=null;){var next=n.getNext();m.instructions.remove(n);n=next;}
            need(exceptional==1,"one exceptional cleanup");int returns=0;
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("leave")){
                need(n.getNext().getOpcode()==RETURN&&n.getPrevious().getOpcode()==ICONST_1,"successful exit attaches before return");var flag=n.getPrevious();var ticks=flag.getPrevious();var token=ticks.getPrevious();need(ticks instanceof VarInsnNode v&&v.var==1&&v.getOpcode()==ALOAD&&token instanceof VarInsnNode t&&t.getOpcode()==ALOAD,"original list and context");
                for(var remove:List.of(token,ticks,flag,n))m.instructions.remove(remove);returns++;
            }need(returns==Arrays.stream(m.instructions.toArray()).filter(n->n.getOpcode()==RETURN).count(),"all original returns cleaned");return;
        }
        if(c.name.equals(P+"NativeCloseoutNpcAreaDoors")){
            var original=c.methods.stream().filter(m->m.name.equals("lineOfSight")).findFirst().orElseThrow();var copy=c.methods.stream().filter(m->m.name.equals("nativeCloseoutOriginLineOfSight")).findFirst().orElseThrow();
            int refs=0;for(var n:copy.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceNpcPlayer$Origin")&&Set.of("hub","room").contains(call.name)){
                need(n.getPrevious() instanceof VarInsnNode v&&v.var==5&&v.getOpcode()==ALOAD,"same original source argument");var nodes=new InsnList();nodes.add(new FieldInsnNode(GETFIELD,P+"NativeCloseoutNpcAreaTargets$Selection","scope","L"+P+"NativeSourceNpcPlayer$Scope;"));nodes.add(new MethodInsnNode(INVOKEVIRTUAL,P+"NativeSourceNpcPlayer$Scope","origin","()L"+P+"NativeSourceNpcPlayer$Origin;",false));copy.instructions.insertBefore(n,nodes);refs++;
            }need(refs==2&&CloseoutAreaOutputAudit.text(original).equals(CloseoutAreaOutputAudit.text(copy)),"complete original dynamic-door evaluator");c.methods.remove(copy);return;
        }
        String method=c.name.endsWith("AreaScene")?"attempt":"serverMonsterAiTick";var m=c.methods.stream().filter(x->x.name.equals(method)).findFirst().orElseThrow();
        if(method.equals("attempt"))prefix(m,"permit",IFNE,IRETURN);else prefix(m,"deliver",IFEQ,RETURN);
    }
    public static void main(String[]args)throws Exception{
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:OWNERS){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts "+name);
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"original body "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(OWNERS.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"unrelated entry "+e.getName());}
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcAreaAttempt");int permit=0,doors=0,claims=0;
            for(var m:helper.methods)for(var n:m.instructions){
                if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTSTATIC)need(f.owner.equals(H)&&f.name.equals("current"),"no global application state writes");
                if(n instanceof MethodInsnNode c){need(!c.owner.contains("Random")&&!c.owner.startsWith("java/sql/")&&!c.owner.contains("Scheduled")&&!c.name.startsWith("schedule")&&!c.owner.equals(P+"Protocol"),"no RNG/SQL/schedule/packets in synchronous cast decision");
                    if(c.owner.equals(P+"NativeCloseoutNpcAreaCast")&&c.name.equals("emptyTarget"))permit++;
                    if(c.owner.equals(P+"NativeCloseoutNpcAreaDoors")&&c.name.equals("nativeCloseoutOriginLineOfSight"))doors++;
                    if(c.owner.equals("java/util/concurrent/atomic/AtomicBoolean")&&c.name.equals("compareAndSet"))claims++;
                }
            }need(permit==1&&doors==1&&claims==1,"one exact no-effect classification, dynamic-door check and receipt claim");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained in five classes; original encounter selection, RNG, scheduling and physical fallback bodies byte-identical. Two bounded prefixes, synchronous Hub append context on every normal/exceptional exit, transient completed receipt, and exact dynamic-door sibling. No application execution, tests, SQL or deployment.\n");
    }
}
