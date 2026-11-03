import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Independently restores exact original status operands, source clears and boolean return. */
final class VerifyMonsterSilence {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterCurseLifecycle";
 static void need(boolean b,String why){if(!b)throw new AssertionError("NPC silence inverse "+why);}
 static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
 static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static void load(AbstractInsnNode n,int op,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot,"original actor/population local");}
 static void raw(ClassNode c){var counts=new TreeMap<String,Integer>();for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode x&&x.owner.equals(H)&&!VerifyMonsterDamage.NAMES.contains(x.name)&&!VerifyMonsterPoisonParalysis.NAMES.contains(x.name))counts.merge(x.name,1,Integer::sum);need(counts.equals(new TreeMap<>(Map.of("cureSilence",1,"damage",2,"clearSource",10,"poisonExisting",1))),"exact fourteen state hooks "+counts);}
 static MethodNode normalize(MethodNode original){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);m.maxStack+=16;
  Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(W,m);}catch(AnalyzerException e){throw new AssertionError(e);}var nodes=m.instructions.toArray();
  for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode x&&x.owner.equals(H)){
   if(x.name.equals("clearSource")){need((m.access&ACC_STATIC)==0&&x.desc.equals("(Ljava/lang/Object;L"+W+";)V"),"native static source clear signature");load(before(x),ALOAD,0);m.instructions.remove(before(x));m.instructions.set(x,new MethodInsnNode(INVOKESTATIC,W,"clearPoisonSource","(L"+W+"$Actor;)V",false));continue;}
   if(x.name.equals("poisonExisting")){need(m.name.equals("poisonLeaseActive")&&x.desc.equals("(ZL"+T+";)Z")&&after(x).getOpcode()==IRETURN,"only original poison boolean return");var field=before(x);need(field instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&f.desc.equals("L"+T+";"),"original actor status field");load(before(field),ALOAD,0);m.instructions.remove(before(field));m.instructions.remove(field);m.instructions.remove(x);continue;}
   String name,desc;int depth;
   if(x.name.equals("cureSilence")){need(m.name.equals("curePoison")&&x.desc.equals("(L"+T+";)Z"),"only native silence cure");name="curePoisonSilence";desc="()Z";depth=1;}
   else if(x.name.equals("damage")){need(Set.of("poison","npcPoison").contains(m.name)&&x.desc.equals("(L"+T+";JII)Z"),"only native damage poison admission");name="activatePoison";desc="(JII)Z";depth=4;}
   else throw new AssertionError("unknown NPC silence state bridge "+x.name);
   var frame=frames[i];var receiver=frame.getStack(frame.getStackSize()-depth);need(receiver.insns.size()==1,"unique native status operand");var node=receiver.insns.iterator().next();need(node instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&f.desc.equals("L"+T+";"),"same actual native actor status receiver");m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,T,name,desc,false));
  }return m;
 }
}
