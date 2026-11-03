import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;
final class VerifyIncomingSource {
 static final String P="local/lito381/server/",C=P+"ClientSession",A=P+"BattleAbilityStatus$IllusionAvatar",H=P+"CombatIncomingSource";
 static AbstractInsnNode prior(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode next(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static void need(boolean b,String why){if(!b)throw new AssertionError("incoming source "+why);}static int local(AbstractInsnNode n,int op){need(n instanceof VarInsnNode v&&v.getOpcode()==op,"native typed operand");return ((VarInsnNode)n).var;}
 static MethodNode normalize(MethodNode original){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode c&&c.owner.equals(H)))return m;
  boolean battle=m.name.equals("takePlayerDamage")&&m.desc.equals("(IL"+C+";ZZJ)V"),world=m.name.equals("takeClassicWorldDirectDamage")&&m.desc.equals("(DJ)L"+C+"$ClassicWorldFatalContext;");need(battle||world,"only original HP kernels");var nodes=m.instructions.toArray();Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(C,m);}catch(AnalyzerException e){throw new AssertionError(e);}int count=0,avatar=-1,prepared=-1;
  for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(H)){
   if(call.name.equals("battle")){need(battle&&call.desc.equals("(L"+A+";IL"+C+";)I")&&local(prior(call),ALOAD)==0,"same BATTLE HP target");m.instructions.remove(prior(call));m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,A,"applyIncomingDamage","(I)I",false));count++;}
   else if(call.name.equals("worldBefore")){need(world&&call.desc.equals("(DL"+A+";)I"),"WORLD before descriptor");var receiver=prior(call);need(local(prior(receiver),DLOAD)==1,"original double incoming parameter retained");avatar=local(receiver,ALOAD);prepared=local(next(call),ISTORE);boolean snapshot=false;for(var n=receiver.getPrevious();n!=null;n=n.getPrevious())if(n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==avatar){need(prior(n) instanceof MethodInsnNode c&&c.owner.equals(P+"BattleAbilityStatus")&&c.name.equals("illusionAvatar")&&c.desc.equals("(J)L"+A+";"),"original timed Avatar snapshot producer");snapshot=true;break;}need(snapshot,"native World Avatar local");m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,A,"incomingDamageRate","()D",false));var restore=new InsnList();restore.add(new InsnNode(DMUL));restore.add(new InsnNode(D2I));m.instructions.insert(receiver.getNext(),restore);count++;}
   else {need(world&&call.name.equals("world")&&call.desc.equals("(L"+C+";IZLjava/lang/String;DL"+A+";)I"),"WORLD final descriptor");var rate=prior(call);var raw=prior(rate);need(local(rate,ALOAD)==avatar&&local(raw,DLOAD)==1,"same original double and legacy Avatar fallback snapshot");var f=frames[at];need(f.getStack(f.getStackSize()-6).insns.size()==1&&local(f.getStack(f.getStackSize()-6).insns.iterator().next(),ALOAD)==0&&f.getStack(f.getStackSize()-5).insns.size()==1&&local(f.getStack(f.getStackSize()-5).insns.iterator().next(),ILOAD)==prepared,"original target and original active-reduction value local");m.instructions.remove(rate);m.instructions.remove(raw);m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,C,"nativeMagicDollDamageAfterActiveReduction","(IZLjava/lang/String;)I",false));count++;}
  }
  need(count==(battle?1:2),"one exact transform per native stage");return m;
 }
}
