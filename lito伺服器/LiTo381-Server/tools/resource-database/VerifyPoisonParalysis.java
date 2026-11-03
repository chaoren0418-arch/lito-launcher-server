import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;
/** Independent inverse proof of the field exception and original typed receivers. */
final class VerifyPoisonParalysis {
 static final String P="local/lito381/server/",T=P+"BattleTimedStatus",C=P+"ClientSession",H=P+"CombatPoisonParalysis";
 static void need(boolean b,String message){if(!b)throw new AssertionError("independent paralysis "+message);}
 static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static int local(AbstractInsnNode n,int op){need(n instanceof VarInsnNode v&&v.getOpcode()==op,"typed original operand");return ((VarInsnNode)n).var;}
 static ClassNode session(ClassNode original){var c=new ClassNode();original.accept(c);for(int i=0;i<c.methods.size();i++)c.methods.set(i,normalize(c.methods.get(i),C));return c;}
 static MethodNode normalize(MethodNode original,String owner){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);m.maxStack+=32;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(owner,m);}catch(Exception e){throw new AssertionError(e);}var nodes=m.instructions.toArray();
  for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode n&&n.owner.equals(H)){
   String nativeName=null,desc=null;
   switch(n.name){
    case "cancelNpc" -> {need(owner.equals(C)&&Set.of("useInventoryItemBeforeTravel","useClassicWorldCurseRemovalSpell").contains(m.name)&&n.desc.equals("(L"+T+";L"+C+";)L"+T+"$ParalysisCancellation;"),"only original poison-only antidote/skill9 cancellation "+owner+"/"+m.name+n.desc);m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,P+"CombatNpcPoison","cancelParalysis",n.desc,false));continue;}
    case "activate" -> {need(owner.equals(C)&&m.name.equals("sourceNpcPoisonState")&&n.desc.equals("(L"+T+";JII)Z"),"only NPC cloned activation");nativeName="activateParalysis";desc="(JII)Z";}
    case "poisonVisual","curseVisual" -> {need(owner.equals(C)&&n.desc.equals("(L"+T+";J)Z")&&(n.name.equals("poisonVisual")?m.name.equals("sourceNpcPoisonState"):Set.of("useParalysisSpell","useClassicWorldParalysisSpell").contains(m.name)),"independent gate callers");nativeName="paralysisVisualActive";desc="(J)Z";}
    case "note" -> {need(owner.equals(C)&&n.desc.equals("(L"+T+";J)Z"),"native marker signature");nativeName="noteParalysisLockSent";desc="(J)Z";}
    case "poll" -> {need(owner.equals(C)&&m.name.equals("serverExpireTimedStatuses")&&n.desc.equals("(L"+T+";JL"+C+";)L"+T+"$ParalysisPoll;")&&local(before(n),ALOAD)==0,"original clock PC");m.instructions.remove(before(n));nativeName="pollParalysis";desc="(J)L"+T+"$ParalysisPoll;";}
    case "otherActive","otherVisual" -> {need(owner.equals(T)&&m.name.equals(n.name.equals("otherActive")?"paralysisActive":"paralysisVisualActive")&&n.desc.equals("(ZL"+T+";J)Z")&&local(before(n),LLOAD)==1&&local(before(before(n)),ALOAD)==0&&after(n).getOpcode()==IRETURN,"status return wraps only original boolean");m.instructions.remove(before(n));m.instructions.remove(before(n));m.instructions.remove(n);continue;}
    case "discard" -> {need(owner.equals(T)&&m.name.equals("clear")&&n.desc.equals("(L"+T+";)V")&&local(before(n),ALOAD)==0&&before(before(n))==null,"clear only new independent slot at entry");m.instructions.remove(before(n));m.instructions.remove(n);continue;}
    case "paletteFirst" -> {need(owner.equals(C)&&m.name.equals("currentPoisonPalette")&&n.desc.equals("(ZL"+T+";J)Z")&&local(before(n),LLOAD)==1,"palette original time");var f=before(before(n));need(f instanceof FieldInsnNode field&&field.getOpcode()==GETFIELD&&field.owner.equals(C)&&field.name.equals("timedStatus")&&local(before(f),ALOAD)==0,"palette same PC status");var prior=before(before(f));need(prior instanceof MethodInsnNode call&&call.owner.equals(T)&&call.name.equals("freezeVisualLockActive"),"preserve original freeze predicate");for(int j=0;j<3;j++)m.instructions.remove(before(n));m.instructions.remove(n);continue;}
    case "release","releaseAt" -> {boolean clock=n.name.equals("releaseAt");need(owner.equals(C)&&n.desc.equals("(L"+C+";[BLjava/lang/String;"+(clock?"J":"")+")V"),"release preserves recipient, bytes and reason");var value=frames[i].getStack(frames[i].getStackSize()-(clock?3:2));if(clock){need(m.name.equals("serverExpireTimedStatuses")&&local(before(n),LLOAD)==1,"release uses the same native clock");m.instructions.remove(before(n));}need(value.insns.size()==1&&value.insns.iterator().next() instanceof MethodInsnNode factory&&factory.owner.equals(P+"Protocol")&&factory.name.equals("paralysis")&&before(factory) instanceof FieldInsnNode mode&&mode.owner.equals(P+"Protocol$ParalysisMode")&&mode.name.equals("PARALYSIS_RELEASE"),"only original paralysis release constructor");m.instructions.set(n,new MethodInsnNode(INVOKEVIRTUAL,C,"sendEncrypted","([BLjava/lang/String;)V",false));continue;}
    default -> throw new AssertionError("unknown independent paralysis bridge "+n.name);
   }
   need(n.getOpcode()==INVOKESTATIC,"typed static bridge");m.instructions.set(n,new MethodInsnNode(INVOKEVIRTUAL,T,nativeName,desc,false));
  }
  return m;
 }
}
