import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
final class PlayerMagicPatch implements Opcodes {
 static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatPlayerMagic";
 static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException{
  var clone=CombatPatch.method(c,"sourceMagicDamageBeforeResistance",MagicDamagePatch.PRE_MR);int signed=0;
  for(var n:clone.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals("java/lang/Math")&&call.name.equals("max")&&call.desc.equals("(II)I")&&DragonPatch.prior(n).getOpcode()==ICONST_0&&DragonPatch.prior(DragonPatch.prior(n)).getOpcode()==ISUB){clone.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"signedPreMr","(II)I",false));signed++;}
  CombatPatch.require(signed,1,"PC signed armor subtraction clone");var counts=new TreeMap<String,Integer>();
  for(var m:c.methods){if(!MagicDamagePatch.MIRRORS.containsKey(m.name)&&!MagicDamagePatch.SINGLE.contains(m.name)&&!Set.of("useCounterDetectionSpell","useClassicWorldCounterDetectionSpell").contains(m.name))continue;
   m.maxStack+=64;var nodes=m.instructions.toArray();var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);int pc=-1;
   for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("counterMirrorDecision")){int receiver=MagicDamagePatch.load(frames[at].getStack(frames[at].getStackSize()-5),ALOAD).var;if(pc>=0&&pc!=receiver)throw new IllegalStateException("PC reduction actor drift");pc=receiver;}
   if(pc<0)for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("earthBindProtectedCombatDamage"))pc=MagicDamagePatch.load(frames[at].getStack(frames[at].getStackSize()-3),ALOAD).var;
   for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(C)&&call.desc.equals("(ILjava/lang/String;)I")&&Set.of("combatDamageAfterCooking","earthBindProtectedCombatDamage").contains(call.name)){
    int receiver=MagicDamagePatch.load(frames[at].getStack(frames[at].getStackSize()-3),ALOAD).var;if(receiver!=pc)throw new IllegalStateException("unqualified PC magic reduction actor="+m.name);
    String name=call.name.equals("combatDamageAfterCooking")?"reductions":"protect";m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,name,"(L"+C+";ILjava/lang/String;)I",false));counts.merge(m.name+":"+name,1,Integer::sum);
   }
  }
  var expected=new TreeMap<String,Integer>();for(String name:new HashSet<>(Arrays.asList("useAreaAttackSpell","useAttackSpell","useCenteredAreaAttackSpell","useCenteredFreezeAttackSpell","useClassicWorldAreaAttackSpell","useClassicWorldAttackSpell","useClassicWorldCenteredAreaAttackSpell","useClassicWorldCenteredFreezeAttackSpell","useClassicWorldCounterDetectionSpell","useClassicWorldFreezeAttackSpell","useClassicWorldLineAttackSpell","useClassicWorldLineFreezeAttackSpell","useClassicWorldShockStunSpell","useCounterDetectionSpell","useFreezeAttackSpell","useLineAttackSpell","useShockStunSpell"))){expected.put(name+":reductions",1);expected.put(name+":protect",name.equals("useAttackSpell")||name.equals("useClassicWorldAttackSpell")?3:1);}if(!counts.equals(expected))throw new IllegalStateException("source PC scalar sites drift="+counts);hooks.put("source-player-magic-signed-clone",signed);hooks.put("source-player-magic-reductions",counts.entrySet().stream().filter(e->e.getKey().endsWith(":reductions")).mapToInt(Map.Entry::getValue).sum());hooks.put("source-player-magic-protections",counts.entrySet().stream().filter(e->e.getKey().endsWith(":protect")).mapToInt(Map.Entry::getValue).sum());
 }
}
