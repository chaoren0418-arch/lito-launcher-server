import java.util.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Leave raw population APIs numeric; scoped WORLD clocks own managed damage callbacks. */
final class MonsterDamagePatch {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterCurseLifecycle";
 static void apply(ClassNode c,Map<String,Integer> hooks){var counts=new TreeMap<String,Integer>();
  for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals(T)){
   String name=null;if(x.name.equals("curePoison")){if(!m.name.equals("curePoison")||!x.desc.equals("()Z"))throw new IllegalStateException("damage cure shape");name="cureDamage";}
   else if(x.name.equals("pollPoison")){if(!m.name.equals("pollPoisons")||!x.desc.equals("(JI)L"+T+"$PoisonPoll;"))throw new IllegalStateException("damage poll shape");name="damagePoll";}
   else if(x.name.equals("poisonActive")){if(!Set.of("poisonPalette","poisonLeaseActive").contains(m.name)||!x.desc.equals("(J)Z"))throw new IllegalStateException("damage active shape "+m.name);name="damageActive";}
   else if(x.name.equals("earthBindActive")&&m.name.equals("damage")){if(!m.desc.equals("(IJL"+W+"$Contribution;I)L"+W+"$Hit;")||!x.desc.equals("(J)Z"))throw new IllegalStateException("NPC poison receive guard shape");name="damageEarthBound";}
   if(name!=null){m.instructions.set(x,new MethodInsnNode(INVOKESTATIC,H,name,"(L"+T+";"+x.desc.substring(1),false));counts.merge(name,1,Integer::sum);}
  }
  for(var m:c.methods)if(m.name.equals("damage")&&m.desc.equals("(IJL"+W+"$Contribution;I)L"+W+"$Hit;"))for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("hiddenStatus")&&f.desc.equals("I")){m.instructions.insertBefore(f,new InsnNode(DUP));m.instructions.insert(f,new MethodInsnNode(INVOKESTATIC,H,"damageHidden","(Ljava/lang/Object;I)I",false));counts.merge("damageHidden",1,Integer::sum);}
  if(!counts.equals(new TreeMap<>(Map.of("cureDamage",1,"damagePoll",1,"damageActive",2,"damageEarthBound",1,"damageHidden",2))))throw new IllegalStateException("WORLD damage state hooks "+counts);
  System.out.println("source-monster-damage-state="+counts);hooks.put("source-monster-damage-state",7);
 }
}
