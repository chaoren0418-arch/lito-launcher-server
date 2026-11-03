import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Preserve native population admission and source clears behind exact typed adapters. */
final class MonsterSilencePatch {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterCurseLifecycle";
 static void apply(ClassNode c,Map<String,Integer> hooks){var counts=new TreeMap<String,Integer>();
  for(var m:c.methods){m.maxStack+=8;
   for(var n:m.instructions.toArray()){
    if(n instanceof MethodInsnNode x&&x.owner.equals(T)&&x.name.equals("curePoisonSilence")){if(!m.name.equals("curePoison")||!x.desc.equals("()Z"))throw new IllegalStateException("NPC silence cure shape");m.instructions.set(x,new MethodInsnNode(INVOKESTATIC,H,"cureSilence","(L"+T+";)Z",false));counts.merge("cureSilence",1,Integer::sum);}
    else if(n instanceof MethodInsnNode x&&x.owner.equals(T)&&x.name.equals("activatePoison")){if(!Set.of("poison","npcPoison").contains(m.name)||!x.desc.equals("(JII)Z"))throw new IllegalStateException("NPC damage admission shape");m.instructions.set(x,new MethodInsnNode(INVOKESTATIC,H,"damage","(L"+T+";JII)Z",false));counts.merge("damage",1,Integer::sum);}
    else if(n instanceof MethodInsnNode x&&x.owner.equals(W)&&x.name.equals("clearPoisonSource")){if((m.access&ACC_STATIC)!=0||!x.desc.equals("(L"+W+"$Actor;)V"))throw new IllegalStateException("NPC source clear owner");m.instructions.insertBefore(x,new VarInsnNode(ALOAD,0));m.instructions.set(x,new MethodInsnNode(INVOKESTATIC,H,"clearSource","(Ljava/lang/Object;L"+W+";)V",false));counts.merge("clearSource",1,Integer::sum);}
    else if(m.name.equals("poisonLeaseActive")&&n.getOpcode()==IRETURN){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new FieldInsnNode(GETFIELD,W+"$Actor","damagePoisonStatus","L"+T+";"));add.add(new MethodInsnNode(INVOKESTATIC,H,"poisonExisting","(ZL"+T+";)Z",false));m.instructions.insertBefore(n,add);counts.merge("poisonExisting",1,Integer::sum);}
   }
  }
  var expected=new TreeMap<>(Map.of("cureSilence",1,"damage",2,"clearSource",10,"poisonExisting",1));if(!counts.equals(expected))throw new IllegalStateException("NPC silence state hooks "+counts);
  System.out.println("source-monster-silence-state="+counts);hooks.put("source-monster-silence-state",counts.values().stream().mapToInt(Integer::intValue).sum());
 }
}
