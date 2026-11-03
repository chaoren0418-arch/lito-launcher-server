package local.lito381.server;
import java.util.concurrent.ThreadLocalRandom;

/** Source incoming equipment/Avatar tail, using native doll definitions and visuals. */
final class CombatIncomingSource {
 private static final String P="local/lito381/server/";
 static double weapon(ClientSession pc){var inventory=(RuntimeInventory)CombatAccess.field(pc,"inventory");return inventory.hasEquippedCatalog(145)||inventory.hasEquippedCatalog(149)?1.5:1;}
 static double rate(ClientSession pc){return ((BattleAbilityStatus)CombatAccess.field(pc,"abilityStatus")).illusionAvatar(System.nanoTime()).incomingDamageRate();}
 static int battle(BattleAbilityStatus.IllusionAvatar avatar,int damage,ClientSession pc){return CombatRules.loaded()?(int)(damage*weapon(pc)*rate(pc)):avatar.applyIncomingDamage(damage);}
 static int worldBefore(double raw,BattleAbilityStatus.IllusionAvatar avatar){return CombatRules.loaded()?(int)raw:(int)(raw*avatar.incomingDamageRate());}
 static int world(ClientSession pc,int legacy,boolean publish,String reason,double raw,BattleAbilityStatus.IllusionAvatar avatar){
  if(!CombatRules.loaded())return CombatAccess.integerCall(pc,"nativeMagicDollDamageAfterActiveReduction","(IZLjava/lang/String;)I",legacy,publish,reason);
  if(raw<=0)return 0;
  var powers=(NativeMagicDollPowers)CombatAccess.call(pc,"activeMagicDollPowers","()L"+P+"NativeMagicDollPowers;");
  var proc=NativeMagicDollEffects.defense(powers,()->ThreadLocalRandom.current().nextInt(1000));
  if(proc.damageReduction()>0&&publish)for(var trigger:proc.triggers())CombatAccess.call(pc,"publishNativeMagicDollProcVisual","(L"+P+"SourceMagicDollPower;JZLjava/lang/String;)V",trigger,pc.playerObjectId(),true,reason+"-defense");
  double reduced=Math.max(0,raw-proc.damageReduction());
  return (int)(reduced*weapon(pc)*rate(pc));
 }
}
