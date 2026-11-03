import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcCountsAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcCounts";
    static final List<String> OWNERS=List.of("NativeSourceNpcSpellTiming","NativeSourceNpcSpellTiming$Data","NativeCloseoutNpcAreaAttempt$Receipt","NativeCloseoutNpcReuse","BattleQuestEncounter","BattleTimeAttackEncounter","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster","GameHub");
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void load(AbstractInsnNode n,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"original argument slot "+slot);}
    public static void main(String[]args)throws Exception{
        int methods=0,freeze=0,scheduled=0,gates=0,summon=0,finish=0;
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:OWNERS){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);
                if(name.endsWith("$Data")||name.endsWith("$Receipt"))need(b.fields.removeIf(f->f.name.equals(name.endsWith("$Data")?"nativeCloseoutNpcCountLimits":"nativeCloseoutNpcCounted")&&(f.access&ACC_TRANSIENT)!=0),"one transient fact/claim field");
                for(var m:b.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode c&&c.owner.equals(H)){
                    if(c.name.equals("summon")){
                        need(name.endsWith("$Monster")&&m.name.equals("summonReady"),"two exact summon predicates");
                        var first=m.instructions.getFirst();load(first,0);load(first.getNext(),1);need(first.getNext().getNext()==n,"bounded initial override");
                        JumpInsnNode branch=null;for(var p=n.getNext();p!=null;p=p.getNext())if(p instanceof JumpInsnNode j){branch=j;break;}
                        need(branch!=null&&branch.getOpcode()==IFNULL&&branch.label.getPrevious().getOpcode()==IRETURN,"unknown facts retain original readiness");
                        var end=branch.label;while(m.instructions.getFirst()!=end)m.instructions.remove(m.instructions.getFirst());m.instructions.remove(end);summon++;
                    }else if(c.name.equals("noTarget")){
                        need(name.equals("GameHub")&&m.name.equals("monsterAiTicks"),"original synchronized AI discovery");
                        var now=n.getPrevious();need(now instanceof VarInsnNode time&&time.getOpcode()==LLOAD&&time.var==1,"original AI tick clock");var snapshot=now.getPrevious();load(snapshot,8);var room=snapshot.getPrevious();load(room,5);var hub=room.getPrevious();load(hub,0);
                        need(hub.getPrevious() instanceof MethodInsnNode old&&old.owner.equals(P+"NativeSourceNpcPlayerView")&&old.name.equals("target"),"after original target extraction");
                        for(var p:List.of(hub,room,snapshot,now,n))m.instructions.remove(p);finish++;
                    }else{
                        var second=n.getPrevious();var first=second.getPrevious();
                        if(c.name.equals("freeze")){need(name.equals("NativeSourceNpcSpellTiming")&&m.name.equals("capture")&&n.getNext().getOpcode()==ARETURN,"same capture result");load(first,0);load(second,1);freeze++;}
                        else if(c.name.equals("scheduled")){need(name.equals("NativeCloseoutNpcReuse")&&m.name.equals("scheduled")&&first==m.instructions.getFirst(),"success receipt before reuse only");load(first,0);load(second,1);scheduled++;}
                        else{need(c.name.equals("allowed")&&m.name.equals("aiAttackFor")&&m.desc.contains("[I"),"two selected row gates");load(first,10);load(second,15);need(first.getPrevious() instanceof MethodInsnNode old&&old.name.equals("triggerRoll"),"count does not skip original chance draw/predicate");gates++;}
                        m.instructions.remove(first);m.instructions.remove(second);m.instructions.remove(n);
                    }
                }
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts "+name);
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"original body "+name+"."+x.name);methods++;}
            }
            need(freeze==1&&scheduled==1&&gates==2&&summon==2&&finish==2,"exact birth/count/finish seams");
            for(var e:Collections.list(base.entries())){if(OWNERS.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"unrelated entry "+e.getName());}
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcCounts");int merges=0,clears=0;
            for(var m:helper.methods)for(var n:m.instructions){
                if(n instanceof LdcInsnNode value)need(!Objects.equals(value.cst,"skillReadyNanos"),"count reset never clears reuse");
                if(n instanceof MethodInsnNode c){need(!c.owner.contains("Random")&&!c.owner.startsWith("java/sql/")&&!c.owner.contains("Scheduled")&&!c.owner.equals(P+"Protocol"),"no RNG/SQL/scheduler/output");
                    if(c.owner.equals("java/util/Map")&&c.name.equals("merge")){need(m.name.equals("scheduled"),"one proven success count");merges++;}
                    if(c.owner.equals("java/util/Map")&&c.name.equals("clear")){need(m.name.equals("noTarget"),"one no-visible-player reset");clears++;}
                }
            }need(merges==1&&clears==1,"bounded original actor map writes");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods in nine classes retained. Same birth snapshot supplies count limits; original chance draws and selected fallback remain. Two summon readiness overrides preserve unknown-fact fallback; original summon increment remains byte-identical. One completed-area receipt count and two no-target discovery seams; reset does not access cooldowns. All other archive entries byte-identical. No application execution, tests, SQL or deployment.\n");
    }
}
