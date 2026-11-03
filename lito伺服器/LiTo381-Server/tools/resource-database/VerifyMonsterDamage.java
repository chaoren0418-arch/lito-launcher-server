import java.util.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Independent inverse checks the actual original actor status receiver at every hook. */
final class VerifyMonsterDamage {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterCurseLifecycle";
 static final Set<String> NAMES=Set.of("cureDamage","damagePoll","damageActive","damageEarthBound","damageHidden");
 static void need(boolean b,String why){if(!b)throw new AssertionError("WORLD damage state "+why);}
 static void raw(ClassNode c){var counts=new TreeMap<String,Integer>();for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode x&&x.owner.equals(H)&&NAMES.contains(x.name))counts.merge(x.name,1,Integer::sum);need(counts.equals(new TreeMap<>(Map.of("cureDamage",1,"damagePoll",1,"damageActive",2,"damageEarthBound",1,"damageHidden",2))),"exact seven hooks "+counts);}
 static MethodNode hubClose(MethodNode original){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();need(first instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"hub close original receiver");var call=VerifyCombatPreservation.following(first);need(call instanceof MethodInsnNode x&&x.getOpcode()==INVOKESTATIC&&x.owner.equals(H)&&x.name.equals("closeHub")&&x.desc.equals("(L"+P+"GameHub;)V"),"only owned queue shutdown prefix");m.instructions.remove(first);m.instructions.remove(call);return m;}
 static MethodNode normalize(MethodNode original){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);m.maxStack+=8;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(W,m);}catch(AnalyzerException e){throw new AssertionError(e);}var nodes=m.instructions.toArray();
  for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode x&&x.owner.equals(H)&&NAMES.contains(x.name)){
   if(x.name.equals("damageHidden")){need(m.name.equals("damage")&&m.desc.equals("(IJL"+W+"$Contribution;I)L"+W+"$Hit;")&&x.getOpcode()==INVOKESTATIC&&x.desc.equals("(Ljava/lang/Object;I)I"),"accepted input hidden guard shape");var field=VerifyCombatPreservation.prior(x);var dup=VerifyCombatPreservation.prior(field);need(field instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("hiddenStatus")&&f.desc.equals("I")&&dup.getOpcode()==DUP,"only original same actor hidden value is projected");m.instructions.remove(dup);m.instructions.remove(x);continue;}
   String name,desc;int depth;
   if(x.name.equals("cureDamage")){need(m.name.equals("curePoison"),"native cure owner");name="curePoison";desc="()Z";depth=1;}
   else if(x.name.equals("damagePoll")){need(m.name.equals("pollPoisons"),"native clock owner");name="pollPoison";desc="(JI)L"+T+"$PoisonPoll;";depth=3;}
   else if(x.name.equals("damageEarthBound")){need(m.name.equals("damage")&&m.desc.equals("(IJL"+W+"$Contribution;I)L"+W+"$Hit;"),"exact private HP kernel");name="earthBindActive";desc="(J)Z";depth=2;}
   else{need(Set.of("poisonPalette","poisonLeaseActive").contains(m.name),"native active/palette owners");name="poisonActive";desc="(J)Z";depth=2;}
   need(x.getOpcode()==INVOKESTATIC&&x.desc.equals("(L"+T+";"+desc.substring(1)),"same typed status/arguments/result");var value=frames[i].getStack(frames[i].getStackSize()-depth);need(value.insns.size()==1,"unique actual status receiver");var receiver=value.insns.iterator().next();need(receiver instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&f.desc.equals("L"+T+";"),"original native actor status field");m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,T,name,desc,false));
  }return m;
 }
}
