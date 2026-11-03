import java.util.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;
/** Typed NPC poison-only cure and activation-color boundaries; native loops stay intact. */
final class MonsterPoisonParalysisPatch {
 static final String P="local/lito381/server/",T=P+"BattleTimedStatus",H=P+"CombatMonsterCurseLifecycle";
 static void apply(ClassNode c,Map<String,Integer> hooks){var counts=new TreeMap<String,Integer>();for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals(T)){
  String name=null;if(m.name.equals("curePoison")&&x.name.equals("cancelParalysis"))name="curePoisonParalysis";if(m.name.equals("poisonPalette")&&x.name.equals("paralysisActive"))name="poisonParalysisColor";
  if(name!=null){m.instructions.set(x,new MethodInsnNode(INVOKESTATIC,H,name,"(L"+T+";"+x.desc.substring(1),false));counts.merge(name,1,Integer::sum);}
 }if(!counts.equals(new TreeMap<>(Map.of("curePoisonParalysis",1,"poisonParalysisColor",1))))throw new IllegalStateException("NPC poison paralysis hooks "+counts);hooks.put("source-monster-poison-paralysis-state",2);System.out.println("source-monster-poison-paralysis-state="+counts);}
}
