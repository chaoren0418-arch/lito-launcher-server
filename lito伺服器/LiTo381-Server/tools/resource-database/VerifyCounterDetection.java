import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Independently reverse the two native skill72 scalar stages and zero publisher clone. */
final class VerifyCounterDetection {
    static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatCounterDetection",A=P+"CombatMagicDamage$Attempt";
    static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
    static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
    static void require(boolean good,String why){if(!good)throw new AssertionError("Counter Detection "+why);}
    static VarInsnNode local(AbstractInsnNode n,int code){return VerifyLineSpells.local(n,code);}
    static MethodNode copy(MethodNode s){var m=new MethodNode(s.access,s.name,s.desc,s.signature,s.exceptions.toArray(String[]::new));s.accept(m);return m;}
    static void erase(MethodNode m,AbstractInsnNode...n){VerifyLineSpells.erase(m,n);}
    static int role(SourceValue value){require(value.insns.size()==1,"single PC source");return local(value.insns.iterator().next(),ALOAD).var;}
    static MethodNode normalize(MethodNode source){
        var m=copy(source);if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode c&&c.owner.equals(H)))return m;
        require(Set.of("useCounterDetectionSpell","useClassicWorldCounterDetectionSpell").contains(m.name)&&m.desc.equals("([BL"+P+"BattleSpellCatalog$CounterDetectionSpell;)V"),"native method identity");m.maxStack+=32;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(C,m);}catch(AnalyzerException error){throw new AssertionError(error);}var nodes=m.instructions.toArray();var indexes=new IdentityHashMap<AbstractInsnNode,Integer>();for(int i=0;i<nodes.length;i++)indexes.put(nodes[i],i);int slot=-1,pc=-1,prepared=0,finished=0;
        for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("prepare")){
            require(call.desc.equals("(L"+C+";IIIIIILjava/lang/String;L"+C+";J)L"+A+";"),"prepared scalar identity");var clock=before(call);var target=before(clock);pc=local(target,ALOAD).var;require(local(clock,LLOAD).var==VerifyLineSpells.slot(m,"castNow"),"same accepted clock at prepared protection check");var frame=frames[at];var armor=frame.getStack(frame.getStackSize()-4);require(armor.insns.size()==1&&armor.insns.iterator().next() instanceof MethodInsnNode getter&&getter.owner.equals(C)&&getter.name.equals("effectiveArmorMagicReductionBase")&&getter.desc.equals("()I"),"original player armor producer");var getter=armor.insns.iterator().next();var producer=frames[indexes.get(getter)];require(role(producer.getStack(producer.getStackSize()-1))==pc,"actual player actor provenance");var dup=after(call);var store=after(dup);var damage=after(store);require(dup.getOpcode()==DUP&&local(store,ASTORE).var>=0&&damage instanceof MethodInsnNode get&&get.owner.equals(A)&&get.name.equals("damage")&&get.desc.equals("()I"),"native per-target scalar captured once");slot=local(store,ASTORE).var;erase(m,target,clock,dup,store,damage);m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,C,"ordinaryMagicDamage","(IIIIIILjava/lang/String;)I",false));prepared++;
        }
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
            require(call.name.equals("finish")&&call.desc.equals("(IL"+A+";L"+C+";L"+C+";J)I"),"source finish identity");var time=before(call);var target=before(time);var caster=before(target);var context=before(caster);var original=before(context);var store=after(call);require(local(time,LLOAD).var==VerifyLineSpells.slot(m,"castNow")&&local(target,ALOAD).var==pc&&local(caster,ALOAD).var==0&&local(context,ALOAD).var==slot&&original instanceof MethodInsnNode reduce&&reduce.owner.equals(C)&&reduce.name.equals("earthBindProtectedCombatDamage")&&reduce.desc.equals("(ILjava/lang/String;)I")&&store instanceof VarInsnNode out&&out.getOpcode()==ISTORE,"source finish after original target reductions and before original result store");erase(m,context,caster,target,time,call);finished++;
        }
        require(prepared==1&&finished==1,"exact native stage counts");var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var init=after(first);require(first.getOpcode()==ACONST_NULL&&local(init,ASTORE).var==slot,"unique native method-local attempt initialization");erase(m,first,init);return m;
    }
}
