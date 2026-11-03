import java.util.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;
/** Independent inverse proves each bridge consumes the actual original native actor status. */
final class VerifyMonsterPoisonParalysis {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterCurseLifecycle";
 static final Set<String> NAMES=Set.of("curePoisonParalysis","poisonParalysisColor");
 static void need(boolean b,String why){if(!b)throw new AssertionError("NPC poison paralysis inverse "+why);}
 static void raw(ClassNode c){var counts=new TreeMap<String,Integer>();for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode x&&x.owner.equals(H)&&NAMES.contains(x.name))counts.merge(x.name,1,Integer::sum);need(counts.equals(new TreeMap<>(Map.of("curePoisonParalysis",1,"poisonParalysisColor",1))),"exact two state adapters "+counts);}
 static MethodNode normalize(MethodNode original){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);m.maxStack+=8;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(W,m);}catch(AnalyzerException e){throw new AssertionError(e);}var nodes=m.instructions.toArray();for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode x&&x.owner.equals(H)&&NAMES.contains(x.name)){
  boolean cure=x.name.equals("curePoisonParalysis");String name=cure?"cancelParalysis":"paralysisActive",desc=cure?"()L"+T+"$ParalysisCancellation;":"(J)Z";need(m.name.equals(cure?"curePoison":"poisonPalette")&&x.getOpcode()==INVOKESTATIC&&x.desc.equals("(L"+T+";"+desc.substring(1)),"exact original method/operands/result");var value=frames[i].getStack(frames[i].getStackSize()-(cure?1:2));need(value.insns.size()==1,"unique native status receiver");var n=value.insns.iterator().next();need(n instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&f.desc.equals("L"+T+";"),"same actual original actor status");m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,T,name,desc,false));
 }return m;}
}
