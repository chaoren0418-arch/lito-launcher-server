import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Independent inverse with receiver provenance; original admission/commit/packets remain exact. */
final class VerifyPlayerMagic {
 static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatPlayerMagic";
 static void require(boolean b,String s){if(!b)throw new AssertionError("PC magic scalar "+s);}
 static int role(SourceValue v){require(v.insns.size()==1&&v.insns.iterator().next() instanceof VarInsnNode load&&load.getOpcode()==ALOAD,"single PC receiver source");return ((VarInsnNode)v.insns.iterator().next()).var;}
 static MethodNode normalize(MethodNode original){
  var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);
  if(m.name.equals("sourceMagicDamageBeforeResistance"))return m;
  if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode c&&c.owner.equals(H)))return m;
  m.maxStack+=64;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(C,m);}catch(AnalyzerException e){throw new AssertionError(e);}var nodes=m.instructions.toArray();int pc=-1;
  for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode c){var f=frames[at];int candidate=-1;
   if(c.owner.equals(C)&&c.name.equals("counterMirrorDecision"))candidate=role(f.getStack(f.getStackSize()-5));
   if(c.owner.equals(P+"CombatMagicDamage")&&c.name.equals("direct"))candidate=role(f.getStack(f.getStackSize()-2));
   if(c.owner.equals(P+"CombatCounterDetection")&&c.name.equals("finish"))candidate=role(f.getStack(f.getStackSize()-2));
   if(candidate>=0){require(pc<0||pc==candidate,"same native PC reference");pc=candidate;}
  }
  require(pc>=0,"live mirror/direct/72 actor");int reductions=0,protect=0;
  for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(H)){
   require(call.getOpcode()==INVOKESTATIC&&call.desc.equals("(L"+C+";ILjava/lang/String;)I")&&Set.of("reductions","protect").contains(call.name),"exact scalar bridge identity");var f=frames[at];require(role(f.getStack(f.getStackSize()-3))==pc,"original target receiver, never NPC/self guessed by name");
   boolean cooking=call.name.equals("reductions");if(cooking)reductions++;else protect++;
   m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,C,cooking?"combatDamageAfterCooking":"earthBindProtectedCombatDamage","(ILjava/lang/String;)I",false));
  }
  Set<String> paths=Set.of("useLineAttackSpell","useAreaAttackSpell","useCenteredAreaAttackSpell","useCenteredFreezeAttackSpell","useFreezeAttackSpell","useClassicWorldFreezeAttackSpell","useAttackSpell","useClassicWorldAttackSpell","useClassicWorldLineAttackSpell","useClassicWorldLineFreezeAttackSpell","useClassicWorldCenteredFreezeAttackSpell","useClassicWorldCenteredAreaAttackSpell","useClassicWorldAreaAttackSpell","useShockStunSpell","useClassicWorldShockStunSpell","useCounterDetectionSpell","useClassicWorldCounterDetectionSpell");
  require(paths.contains(m.name)&&reductions==1&&protect==(Set.of("useAttackSpell","useClassicWorldAttackSpell").contains(m.name)?3:1),"exact native method site counts="+m.name);return m;
 }
}
