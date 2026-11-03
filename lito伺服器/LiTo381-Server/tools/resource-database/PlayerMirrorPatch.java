import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class PlayerMirrorPatch implements Opcodes{
 static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatPlayerMirror",OLD=P+"BattleCounterMirrorStatus$Decision",D=H+"$Decision",A=P+"CombatMagicDamage$Attempt";
 static void apply(ClassNode c,Map<String,Integer> hooks){var paths=new TreeMap<String,Integer>();int publishers=0;
  for(var m:new ArrayList<>(c.methods)){if(!MagicDamagePatch.MIRRORS.containsKey(m.name))continue;
   for(var node:m.instructions.toArray())if(node instanceof MethodInsnNode call){
    if(call.owner.equals(C)&&call.name.equals("counterMirrorDecision")){var value=DragonPatch.next(call);var context=DragonPatch.next(value);if(!(value instanceof VarInsnNode v)||v.getOpcode()!=ILOAD||!(context instanceof VarInsnNode load)||load.getOpcode()!=ALOAD)throw new IllegalStateException("mirror has no per-cast source attempt");m.instructions.insertBefore(call,new VarInsnNode(ALOAD,load.var));m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"decide","(L"+C+";IZIJL"+A+";)L"+D+";",false));paths.merge(m.name,1,Integer::sum);}
    else if(call.owner.equals(OLD)){call.owner=D;}
    else if(call.owner.equals(C)&&call.name.equals("publishPlayerCounterMirrorTrigger")){call.name="publishSourcePlayerCounterMirrorTrigger";call.desc=call.desc.replace(OLD,D);publishers++;}
   }
   for(var node:m.instructions)if(node instanceof FrameNode f){replace(f.local);replace(f.stack);}if(m.localVariables!=null)for(var local:m.localVariables)if(local.desc.equals("L"+OLD+";"))local.desc="L"+D+";";
  }
  if(!paths.equals(new TreeMap<>(MagicDamagePatch.MIRRORS)))throw new IllegalStateException("source PC mirror paths drift="+paths);CombatPatch.require(publishers,17,"source PC mirror publishers");
  var old=CombatPatch.method(c,"publishPlayerCounterMirrorTrigger","(L"+C+";L"+OLD+";Ljava/lang/String;)V");var clone=new MethodNode(old.access,"publishSourcePlayerCounterMirrorTrigger",old.desc.replace(OLD,D),old.signature,old.exceptions.toArray(String[]::new));old.accept(clone);clone.name="publishSourcePlayerCounterMirrorTrigger";clone.desc=old.desc.replace(OLD,D);int world=0,battle=0;
  for(var node:clone.instructions.toArray()){if(node instanceof MethodInsnNode call){if(call.owner.equals(OLD))call.owner=D;else if(call.owner.equals(C)&&Set.of("takePlayerDamage","takeClassicWorldPlayerDamage").contains(call.name)){boolean w=call.name.equals("takeClassicWorldPlayerDamage");clone.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,w?"receiveWorld":"receiveBattle","(L"+C+";"+call.desc.substring(1),false));if(w)world++;else battle++;}}if(node instanceof FrameNode f){replace(f.local);replace(f.stack);}}
  if(clone.localVariables!=null)for(var local:clone.localVariables)if(local.desc.equals("L"+OLD+";"))local.desc="L"+D+";";CombatPatch.require(world,1,"source WORLD reflection HP adapter");CombatPatch.require(battle,1,"source BATTLE reflection HP adapter");c.methods.add(clone);hooks.put("source-player-mirror-decisions",paths.values().stream().mapToInt(Integer::intValue).sum());hooks.put("source-player-mirror-publishers",publishers);hooks.put("source-player-mirror-native-clone",world+battle);
 }
 static void avatar(ClassNode c,Map<String,Integer> hooks){int reads=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode field&&field.getOpcode()==GETFIELD&&field.owner.equals(P+"BattleAbilityStatus$IllusionAvatar")&&field.name.equals("incomingDamageRate")&&field.desc.equals("D")){if(!Set.of("incomingDamageRate","applyIncomingDamage").contains(m.name))throw new IllegalStateException("unexpected Avatar scalar reader");var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleAbilityStatus$IllusionAvatar","remainingSeconds","()I",false));i.add(new MethodInsnNode(INVOKESTATIC,P+"CombatRules","avatarIncomingRate","(DI)D",false));m.instructions.insert(n,i);reads++;}CombatPatch.require(reads,2,"source Avatar incoming scalar consumers");hooks.put("source-avatar-incoming-rate",reads);}
 static void replace(List<Object> values){if(values!=null)for(int i=0;i<values.size();i++)if(OLD.equals(values.get(i)))values.set(i,D);}
}
