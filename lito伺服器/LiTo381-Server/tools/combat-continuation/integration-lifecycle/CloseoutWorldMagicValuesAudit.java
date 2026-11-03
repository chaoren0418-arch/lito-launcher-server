import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutWorldMagicValuesAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutWorldMagicValues",V=P+"NativeCloseoutNpcSkillValues$Values",D=P+"ClassicWorldMonsterMagicSkillCatalog$Definition",A=P+"ClassicWorldMonsterPopulation$Actor",S=P+"ClassicWorldMonsterStatusSkillCatalog$Definition";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static boolean var(AbstractInsnNode n,int op,int slot){return n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot;}
    static void inverse(ClassNode c){
        int values=0,captures=0,pays=0;
        for(var m:c.methods){
            var definitions=new HashMap<Integer,String>();
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&Set.of("cost","statusCost").contains(call.name)){
                need(n.getNext() instanceof VarInsnNode v&&v.getOpcode()==ASTORE,"capture destination");definitions.put(((VarInsnNode)n.getNext()).var,call.name.equals("cost")?D:S);
            }
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(V)){
                need(call.desc.equals("()I")&&call.getOpcode()==INVOKEVIRTUAL,"integer value read");String originalOwner=call.name.equals("mp")?definitions.get(((VarInsnNode)n.getPrevious()).var):D;need(originalOwner!=null,"captured original definition type");
                if(call.name.equals("baseDamage")){
                    need(m.name.equals("rollSourceDamage"),"original roll boundary");var save=n.getPrevious();var dup=save.getPrevious();var capture=dup.getPrevious();need(save instanceof VarInsnNode sv&&sv.getOpcode()==ASTORE&&dup.getOpcode()==DUP&&capture instanceof MethodInsnNode h&&h.owner.equals(H)&&h.name.equals("capture")&&h.desc.equals("(L"+D+";)L"+V+";"),"one immutable dice snapshot");m.instructions.remove(save);m.instructions.remove(dup);m.instructions.remove(capture);captures++;
                }else{
                    need(Set.of("mp","diceCount","dieSides").contains(call.name),"expected value getter");var load=n.getPrevious();var pop=load.getPrevious();need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&pop.getOpcode()==POP,"replace only original definition receiver");m.instructions.remove(load);m.instructions.remove(pop);
                }
                if(call.name.equals("mp")){call.owner=originalOwner;call.name="mpCost";}else call.owner=D;values++;
            }
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&Set.of("cost","statusCost").contains(call.name)){
                need(Set.of("magicAttack","ckewSkillAttack").contains(m.name)&&(m.access&ACC_SYNCHRONIZED)!=0,"original population monitor");
                var definition=n.getPrevious();var first=definition.getPrevious();need(var(first,ALOAD,14)&&definition instanceof VarInsnNode dv&&dv.getOpcode()==ALOAD,"same resolved actor and chosen definition");
                var save=n.getNext();need(save instanceof VarInsnNode v&&v.getOpcode()==ASTORE,"cost snapshot local");int slot=((VarInsnNode)save).var;
                var actor=save.getNext();var load=actor.getNext();var ready=load.getNext();var jump=ready.getNext();
                need(var(actor,ALOAD,14)&&var(load,ALOAD,slot)&&ready instanceof MethodInsnNode h&&h.owner.equals(H)&&h.name.equals("ready")&&jump instanceof JumpInsnNode j&&j.getOpcode()==IFNE,"same snapshot HP admission");
                var end=((JumpInsnNode)jump).label;need(jump.getNext().getOpcode()==ACONST_NULL&&jump.getNext().getNext().getOpcode()==ARETURN&&jump.getNext().getNext().getNext()==end,"reject before original MP check");
                need(var(end.getNext(),ALOAD,14)&&end.getNext().getNext() instanceof FieldInsnNode f&&f.owner.equals(A)&&f.name.equals("currentMp"),"cost boundary location");
                var cur=first;while(cur!=end){var next=cur.getNext();m.instructions.remove(cur);cur=next;}m.instructions.remove(end);captures++;
            }
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("payHp")){
                var load=n.getPrevious();var actor=load.getPrevious();var previous=actor.getPrevious();need(var(actor,ALOAD,14)&&load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&previous instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(A)&&f.name.equals("currentMp"),"HP payment adjacent to original MP write");m.instructions.remove(actor);m.instructions.remove(load);m.instructions.remove(n);pays++;
            }
        }
        if(c.name.endsWith("MagicDamage"))need(values==4&&captures==1&&pays==0,"original four dice reads");else need(values==6&&captures==3&&pays==3,"three complete original cost routes");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("ClassicWorldMonsterPopulation","ClassicWorldMonsterMagicDamage");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);inverse(b);need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member counts");
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"retained original field");}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())){if(owners.stream().anyMatch(n->e.getName().equals(P+n+".class")))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original methods retained across WORLD population/damage owners. Three synchronized cost branches share each captured HP/MP value; original four dice reads share one snapshot. Original selection, RNG, gates, cooldown and output bodies retained. All unrelated archive entries byte-identical. No game execution or tests.\n");
    }
}
