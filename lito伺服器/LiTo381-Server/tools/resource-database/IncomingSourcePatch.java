import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class IncomingSourcePatch implements Opcodes {
 static final String P="local/lito381/server/",C=P+"ClientSession",A=P+"BattleAbilityStatus$IllusionAvatar",H=P+"CombatIncomingSource";
 static void apply(ClassNode c,Map<String,Integer> hooks){int battle=0,before=0,world=0;
  for(var m:c.methods){
   if(m.name.equals("takePlayerDamage")&&m.desc.equals("(IL"+C+";ZZJ)V"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(A)&&call.name.equals("applyIncomingDamage")&&call.desc.equals("(I)I")){m.instructions.insertBefore(call,new VarInsnNode(ALOAD,0));m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"battle","(L"+A+";IL"+C+";)I",false));battle++;}
   if(m.name.equals("takeClassicWorldDirectDamage")&&m.desc.equals("(DJ)L"+C+"$ClassicWorldFatalContext;")){
    int avatar=-1;for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(A)&&call.name.equals("incomingDamageRate")&&call.desc.equals("()D")){
     var receiver=DragonPatch.prior(call);if(!(receiver instanceof VarInsnNode v)||v.getOpcode()!=ALOAD)throw new IllegalStateException("native world Avatar snapshot absent");avatar=v.var;var mul=DragonPatch.next(call);var cast=DragonPatch.next(mul);if(mul.getOpcode()!=DMUL||cast.getOpcode()!=D2I)throw new IllegalStateException("native world incoming cast changed");m.instructions.remove(mul);m.instructions.remove(cast);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"worldBefore","(DL"+A+";)I",false));before++;
    }
    if(avatar<0)throw new IllegalStateException("world Avatar local missing");
    for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("nativeMagicDollDamageAfterActiveReduction")&&call.desc.equals("(IZLjava/lang/String;)I")){var args=new InsnList();args.add(new VarInsnNode(DLOAD,1));args.add(new VarInsnNode(ALOAD,avatar));m.instructions.insertBefore(call,args);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"world","(L"+C+";IZLjava/lang/String;DL"+A+";)I",false));world++;}
   }
  }
  CombatPatch.require(battle,1,"source BATTLE equipment/Avatar final cast");CombatPatch.require(before,1,"source WORLD defer incoming factors");CombatPatch.require(world,1,"source WORLD doll/equipment/Avatar double order");hooks.put("source-incoming-equipment-order",battle+before+world);
 }
}
