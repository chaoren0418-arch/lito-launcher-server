import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Independent inverse of NPC-only scalar adapters and signed pre-MR clone. */
final class VerifyNpcMagic {
    static final String P="local/lito381/server/",H=P+"CombatNpcMagic",C=P+"ClientSession",A=P+"CombatMagicDamage$Attempt";
    static final Map<String,Integer> PATHS=Map.of("useLineAttackSpell",4,"useCenteredFreezeAttackSpell",4,"useClassicWorldLineAttackSpell",1,"useClassicWorldLineFreezeAttackSpell",1,"useClassicWorldCenteredFreezeAttackSpell",1,"useClassicWorldCenteredAreaAttackSpell",4,"useClassicWorldAreaAttackSpell",4,"useAreaAttackSpell",4,"useCenteredAreaAttackSpell",4);
    static void require(boolean good,String why){if(!good)throw new AssertionError("NPC magic "+why);}
    static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
    static MethodNode copy(MethodNode s){var m=new MethodNode(s.access,s.name,s.desc,s.signature,s.exceptions.toArray(String[]::new));s.accept(m);return m;}
    static MethodNode normalize(MethodNode source){
        var m=copy(source);if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode c&&c.owner.equals(H)))return m;
        require(PATHS.containsKey(m.name),"unexpected scalar owner="+m.name);m.maxStack+=64;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(C,m);}catch(AnalyzerException error){throw new AssertionError(error);}var nodes=m.instructions.toArray();int count=0;
        for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(H)){
            require(call.getOpcode()==INVOKESTATIC&&Set.of("ordinaryComplete","line","area").contains(call.name)&&call.desc.startsWith("(L"+C+";"),"scalar method identity");var frame=frames[at];var armor=frame.getStack(frame.getStackSize()-2);require(armor.insns.size()==1&&armor.insns.iterator().next().getOpcode()==ICONST_0,"actual native NPC constant armor producer");String old=call.name.equals("ordinaryComplete")?"ordinaryMagicDamage":call.name.equals("line")?"lineSpellDamage":"areaSpellDamage";String desc="("+call.desc.substring(("(L"+C+";").length());require(desc.equals(call.name.equals("ordinaryComplete")?"(IIIIIILjava/lang/String;)I":"(L"+P+"BattleSpellCatalog$"+(call.name.equals("line")?"DirectionalLineSpell":"AreaAttackSpell")+";IIIILjava/lang/String;)I"),"retained original NPC signature");m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,C,old,desc,false));count++;
        }
        require(count==PATHS.get(m.name),"exact scalar count "+m.name+"="+count);return m;
    }
    static void cloneOnly(MethodNode original,MethodNode source){
        require(source.name.equals("sourceNpcMagicBeforeResistance")&&source.desc.equals("(IIIIIILjava/lang/String;)L"+A+";"),"signed clone identity");var m=copy(source);m.name=original.name;m.desc=original.desc;int signed=0,mr=0,returns=0;
        for(var n:m.instructions.toArray()){
            if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("signedPreMr")){require(call.desc.equals("(II)I")&&before(n).getOpcode()==ICONST_0&&before(before(n)).getOpcode()==ISUB,"only original attacker-minus-armor clamp removed");m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,"java/lang/Math","max","(II)I",false));signed++;}
            if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatMagicDamage")&&call.name.equals("deferMr")){require(call.desc.equals("(III)I"),"deferred NPC MR signature");m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,P+"BattleMagicResistance","damageAfterResistance","(III)I",false));mr++;}
            if(n.getOpcode()==ARETURN){var factory=before(n);var hit=before(factory);var caster=before(hit);var resistance=before(caster);require(factory instanceof MethodInsnNode f&&f.owner.equals(H)&&f.name.equals("attempt")&&f.desc.equals("(III)L"+A+";")&&hit instanceof MethodInsnNode get&&get.owner.equals(C)&&get.name.equals("originalMagicHit")&&get.desc.equals("()I")&&VerifyLineSpells.local(caster,ALOAD).var==0&&VerifyLineSpells.local(resistance,ILOAD).var==5,"same scalar MR/hit returned as local attempt");VerifyLineSpells.erase(m,resistance,caster,hit,factory);m.instructions.set(n,new InsnNode(IRETURN));returns++;}
        }
        require(signed==1&&mr==1&&returns==1&&VerifyCombatPreservation.semantic(original).equals(VerifyCombatPreservation.semantic(m)),"signed clone changes only declared clamp/MR/return boundaries");
    }
}
