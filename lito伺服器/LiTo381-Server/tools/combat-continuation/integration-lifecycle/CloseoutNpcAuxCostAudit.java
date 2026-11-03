import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAuxCostAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcAuxCost",C=P+"NativeCloseoutNpcSkillValues",V=C+"$Values";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void remove(MethodNode m,AbstractInsnNode... ns){for(var n:ns)m.instructions.remove(n);}
    static void inverse(ClassNode c){
        for(String kind:List.of("Visual","Ground")){
            String d=P+"BattleTimeAttackMagicSkillCatalog$"+kind+"Definition",ready=kind.toLowerCase()+"Ready",sibling="nativeCloseout"+kind+"Ready";
            int getters=0,captures=0,guards=0,payments=0,calls=0;
            for(var m:c.methods)if(Set.of(ready,sibling,"schedule"+kind).contains(m.name))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
                if(call.owner.equals(V)){
                    need(call.name.equals("mp")&&call.desc.equals("()I"),"captured MP getter");var load=n.getPrevious();var pop=load.getPrevious();need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&pop.getOpcode()==POP,"original definition operand replaced");remove(m,load,pop);call.owner=d;call.name="mpCost";getters++;
                }else if(call.owner.equals(H)){
                    var definition=n.getPrevious();var actor=definition.getPrevious();var store=n.getNext();need(call.name.equals("captureCost")&&actor==m.instructions.getFirst()&&actor instanceof VarInsnNode a&&a.var==0&&a.getOpcode()==ALOAD&&definition instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==(m.name.startsWith("schedule")?3:1)&&store instanceof VarInsnNode s&&s.getOpcode()==ASTORE,"exact existing actor and definition capture prefix");remove(m,actor,definition,n,store);captures++;
                }else if(call.owner.equals(C)){
                    var value=n.getPrevious();var actor=value.getPrevious();need(value instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&actor instanceof VarInsnNode a&&a.getOpcode()==ALOAD&&a.var==0,"original actor and captured values");
                    if(call.name.equals("ready")){need(n.getNext().getOpcode()==IRETURN,"original readiness result");guards++;}
                    else{need(call.name.equals("payHp")&&actor.getPrevious() instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(c.name)&&f.name.equals("mp"),"HP adjacent original MP payment");payments++;}
                    remove(m,actor,value,n);
                }else if(call.owner.equals(c.name)&&call.name.equals(sibling)){
                    var value=n.getPrevious();need(value instanceof VarInsnNode v&&v.getOpcode()==ALOAD,"captured readiness argument");remove(m,value);call.name=ready;call.desc="(L"+d+";J)Z";calls++;
                }
            }
            var original=c.methods.stream().filter(m->m.name.equals(ready)).findFirst().orElseThrow();var captured=c.methods.stream().filter(m->m.name.equals(sibling)).findFirst().orElseThrow();
            need(captured.desc.equals("(L"+d+";JL"+V+";)Z")&&CloseoutAreaOutputAudit.text(original).equals(CloseoutAreaOutputAudit.text(captured)),"complete original captured readiness sibling");c.methods.remove(captured);
            need(getters==3&&captures==2&&guards==2&&payments==1&&calls==1,"exact auxiliary cost seams "+kind);
        }
    }
    public static void main(String[]args)throws Exception{
        int methods=0;String owner=P+"BattleTimeAttackEncounter$Monster.class";
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            var a=CloseoutAreaOutputAudit.read(base,"BattleTimeAttackEncounter$Monster");var b=CloseoutAreaOutputAudit.read(candidate,"BattleTimeAttackEncounter$Monster");inverse(b);
            need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
            for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"original field");}
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained method "+x.name);methods++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(owner))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original Time Attack Monster methods retained; two complete captured readiness siblings; four cost captures, two HP payments and unchanged schedule bodies. All other original entries byte-identical. No application execution or tests.\n");
    }
}
