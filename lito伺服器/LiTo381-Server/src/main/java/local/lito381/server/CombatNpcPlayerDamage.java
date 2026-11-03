package local.lito381.server;
import java.util.concurrent.ThreadLocalRandom;

/** Owned NPC->PC current source arithmetic with native rolls, admission and commits. */
final class CombatNpcPlayerDamage{
 record Attempt(int value,double raw,double multiplier,boolean source,boolean ranged,long npcObjectId,int npcId,int poisonAttack,int paralysisAttack){}
 record Commit(int value,boolean hit){}
 // The native action publishes the calcPcDamage value. Source commitPc applies
 // Striker only afterwards; recovery and positive-hit feedback belong there.
 static Commit commit(int damage,boolean hit,Attempt attempt,ClientSession target,ClientSession owner){
  if(attempt==null||!attempt.source()||!hit)return new Commit(damage,hit);
  int value=(int)(damage*striker(target,attempt,System.nanoTime()));
  if(value<0){if(CombatWorldPhysical.world(target))CombatPlayerRecovery.world(target,value,owner,false);else CombatPlayerRecovery.battle(target,value,owner,false);}
  return new Commit(value,value>0);
 }
 static Attempt prepare(int level,int strength,int type,int ac,int levelRoll,int criticalRoll,int defenseRoll,ClientSession owner,BattleCompanion controlled,ClientSession target,long now,boolean playerTarget,boolean ranged){
  var kind=controlled.snapshot().kind();boolean source=playerTarget&&target!=null&&CombatRules.loaded()&&(kind==BattleCompanion.Kind.PET||kind==BattleCompanion.Kind.SUMMON||kind==BattleCompanion.Kind.TAMED);
  if(!source){int value=CombatAccess.integerCall(owner,"companionPhysicalDamage","(IIIIIII)I",level,strength,type,ac,levelRoll,criticalRoll,defenseRoll);return new Attempt(value,value,1,false,false,0,0,0,0);}
  long millis=System.currentTimeMillis();int undead=controlled.undead();boolean night=(undead==1||undead==3||undead==4)&&CombatNpcPhysicalSource.night(millis);boolean boss=controlled.boss();int stacks=controlled.weaponBreakStacks();
  // Native LiTo has no pet-equipment receiver or NPC Dmgup producer here.
  // Keep their source positions explicit; do not invent equipment or buff state.
  double before=CombatNpcPhysicalSource.mode(CombatNpcPhysicalSource.base(level,strength,levelRoll,kind==BattleCompanion.Kind.PET,0),criticalRoll,0,night,boss,10,stacks);
  double raw=CombatNpcPhysicalSource.defense(before,type,ac,defenseRoll);
  System.out.println("[LiTo] source NPC PC base kind="+kind+" level="+level+" strength="+strength+" strength-damage="+CombatNpcPhysicalSource.strengthDamage(strength)+" pet-level="+(kind==BattleCompanion.Kind.PET?level/7:0)+" level-roll="+levelRoll+" critical-roll="+criticalRoll+" defense-type="+type+" ac="+ac+" defense-roll="+defenseRoll+" undead="+undead+" night="+night+" boss="+boss+" break-stacks="+stacks+" before-defense="+before+" raw="+raw+" millis="+millis+" ranged="+ranged);
  var actor=controlled.snapshot();return new Attempt(Math.max(0,(int)raw),raw,CombatRules.number("PET_TO_PC_DAMAGE_MULTIPLIER",.3),true,ranged,actor.objectId(),actor.npcId(),controlled.poisonAttack(),controlled.paralysisAttack());
 }
 static double striker(ClientSession target,Attempt attempt,long now){return attempt.ranged()&&((BattleTimedStatus)CombatAccess.field(target,"timedStatus")).strikerGaleActive(now)?CombatRules.number("STRIKER_DMG2",1.2):1;}
 static int finish(ClientSession target,int damage,String description,Attempt attempt,long now,ClientSession owner){
  if(attempt==null||!attempt.source())return CombatAccess.integerCall(target,"combatPhysicalDamageAfterArmorAndCooking","(ILjava/lang/String;)I",damage,description);
  if(CombatPlayerMagic.protectedZero(target,now))return 0;
  // Current ARM_BREAKER updates PC abilities only. NPC Striker Gale is applied
  // after calcPcDamage's final int in source commitPc, rather than as a delta.
  double raw=attempt.raw();int base=CombatAccess.integerCall(target,"effectiveArmorMagicReductionBase","()I");int offset=base<=10?0:ThreadLocalRandom.current().nextInt(base-10);int armor=BattlePhysicalDefense.armorReduction(base,offset);var ability=(BattleAbilityStatus)CombatAccess.field(target,"abilityStatus");int reduction=ability.reductionArmor(now).remainingSeconds()==0?0:(Math.max(target.battleLevel(),50)-50)/5+1;int cooking=((BattleCookingStatus)CombatAccess.field(target,"cookingStatus")).active(now).damageReduction();int dragon=((BattleDragonSkinStatus)CombatAccess.field(target,"dragonSkinStatus")).snapshot(now).active()?CombatSourceReductions.dragonSkin(10):0;int patience=CombatAccess.integerCall(target,"effectivePatiencePhysicalReduction","(J)I",now)==0?0:CombatSourceReductions.patience(2);boolean half=ability.immuneToHarm(now).divisor()!=0;
  double reduced=raw-armor-reduction-cooking-dragon-patience;if(half)reduced/=2;int scalar=Math.max(0,(int)(reduced*attempt.multiplier()));CombatNpcPoison.apply(target,attempt,now);double multiplier=striker(target,attempt,now);int result=scalar<=0?0:(int)(scalar*multiplier);System.out.println("[LiTo] source NPC player reduction before="+raw+" armor="+armor+" reduction-armor="+reduction+" cooking="+cooking+" dragon="+dragon+" patience="+patience+" half="+half+" multiplier="+attempt.multiplier()+" applied="+result+" source="+description+" pre-striker="+scalar+" striker-multiplier="+multiplier);
  return scalar;
 }
}
