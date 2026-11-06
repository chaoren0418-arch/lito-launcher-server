// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-debuff-immunity/build-repro/inputs/original/NativeCancellationSpeedImmunity.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;

/** Only explicit skill-44 player call sites use this adapter; never death or owner cleanup. */
final class NativeCancellationSpeedImmunity {
 private static boolean protectedSkill(int skill){
  var controls=CombatDatabase.controlsOrNull();
  return skill>0&&controls!=null&&controls.cancellationImmune().contains(skill);
 }
 static BattleTimedStatus.MagicCancellation magic(BattleTimedStatus timed,ClientSession target,CombatPoisonLifecycle.Scope scope){
  synchronized(timed){
   int haste=(int)CombatAccess.field(timed,"hasteSkillId"),slow=(int)CombatAccess.field(timed,"slowSkillId");
   long hasteUntil=(long)CombatAccess.field(timed,"hasteExpiresAtNanos"),slowUntil=(long)CombatAccess.field(timed,"slowExpiresAtNanos");
   boolean keepHaste=hasteUntil!=0&&protectedSkill(haste),keepSlow=slowUntil!=0&&protectedSkill(slow);
   long weaknessUntil=(long)CombatAccess.field(timed,"weaknessExpiresAtNanos");
   boolean keepWeakness=weaknessUntil!=0&&protectedSkill(47);
   long diseaseUntil=(long)CombatAccess.field(timed,"diseaseExpiresAtNanos");
   boolean keepDisease=diseaseUntil!=0&&protectedSkill(56);
   long decayPotionUntil=(long)CombatAccess.field(timed,"decayPotionExpiresAtNanos");
   boolean keepDecayPotion=decayPotionUntil!=0&&protectedSkill(71);
   long polluteWaterUntil=(long)CombatAccess.field(timed,"polluteWaterExpiresAtNanos");
   boolean keepPolluteWater=polluteWaterUntil!=0&&protectedSkill(173);
   long strikerGaleUntil=(long)CombatAccess.field(timed,"strikerGaleExpiresAtNanos");
   boolean keepStrikerGale=strikerGaleUntil!=0&&protectedSkill(174);
   BattleTimedStatus.MagicCancellation result;
   try {result=CombatPoisonLifecycle.magic(timed,target,scope);}
   finally {
    if(keepHaste){CombatAccess.set(timed,"hasteExpiresAtNanos",hasteUntil);CombatAccess.set(timed,"hasteSkillId",haste);}
    if(keepSlow){CombatAccess.set(timed,"slowExpiresAtNanos",slowUntil);CombatAccess.set(timed,"slowSkillId",slow);}
    if(keepWeakness){CombatAccess.set(timed,"weaknessExpiresAtNanos",weaknessUntil);}
    if(keepDisease){CombatAccess.set(timed,"diseaseExpiresAtNanos",diseaseUntil);}
    if(keepDecayPotion){CombatAccess.set(timed,"decayPotionExpiresAtNanos",decayPotionUntil);}
    if(keepPolluteWater){CombatAccess.set(timed,"polluteWaterExpiresAtNanos",polluteWaterUntil);}
    if(keepStrikerGale){CombatAccess.set(timed,"strikerGaleExpiresAtNanos",strikerGaleUntil);}
   }
   if(!keepHaste&&!keepSlow&&!keepWeakness&&!keepDisease&&!keepDecayPotion&&!keepPolluteWater&&!keepStrikerGale)return result;
   var a=result;
   return new BattleTimedStatus.MagicCancellation(a.haste()&&!keepHaste,a.slow()&&!keepSlow,a.brave(),a.blind(),a.weakness()&&!keepWeakness,a.disease()&&!keepDisease,a.silence(),a.decayPotion()&&!keepDecayPotion,a.polluteWater()&&!keepPolluteWater,a.strikerGale()&&!keepStrikerGale,a.light(),a.bluePotion(),a.wisdomPotion(),a.meditation(),a.concentration(),a.counterMagic(),a.breath(),a.poison(),a.sleep(),a.freeze(),a.earthBind(),a.burningSpirit(),a.elementalFire(),a.soulOfFlame(),a.venomResist(),a.doubleBrake(),a.horrorOfDeath(),a.thunderGrab(),a.boneBreak(),a.paralysisCancellation());
  }
 }
}
