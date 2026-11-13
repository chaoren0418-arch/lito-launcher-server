// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.IOException;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** The missing BATTLE companion target uses existing material, probability and output authorities. */
final class NativeWeaponBreakBattleCompanion {
 static final String P="local/lito381/server/";
 static SourceNpcPhysicalStats nativeSourceWeaponFirstPrepare(ClientSession caster,BattleCompanion companion,NativeAreaMaterialAction.Context context){
  try{
   var accounts=(AccountRepository)CombatAccess.call(caster,"selectedSourceAccounts","()L"+P+"AccountRepository;");
   var snap=NativeSourceWeaponTargetLife.snapshot(companion,context);if(snap==null)return null;
   return accounts.npcPhysicalStats(snap.npcId());
  }
  catch(Exception error){NativeReuseLog.getLogger(NativeWeaponBreakBattleCompanion.class).error("Weapon Break companion Source lookup failed",error);return null;}
 }
 static SourceNpcPhysicalStats prepare(ClientSession caster,BattleCompanion companion){
  try{return ((AccountRepository)CombatAccess.call(caster,"selectedSourceAccounts","()L"+P+"AccountRepository;")).npcPhysicalStats(companion.snapshot().npcId());}
  catch(Exception error){NativeReuseLog.getLogger(NativeWeaponBreakBattleCompanion.class).error("Weapon Break companion Source lookup failed",error);return null;}
 }
 static void cast(ClientSession caster,ClientSession owner,BattleCompanion companion,BattleSpellCatalog.WeaponBreakSpell spell,SourceNpcPhysicalStats stats)throws IOException{
  if(stats==null||spell.skillId()!=27)return;var hub=(GameHub)CombatAccess.field(caster,"hub");long room=(long)CombatAccess.field(caster,"currentRoomId");var actor=companion.snapshot();int x=integer(caster,"positionX"),y=integer(caster,"positionY"),map=hub.battleMapId(room,caster);
  if(!String.valueOf(CombatAccess.field(caster,"phase")).equals("BATTLE")||!String.valueOf(CombatAccess.field(owner,"phase")).equals("BATTLE")
    ||(boolean)CombatAccess.field(caster,"observer")||(boolean)CombatAccess.field(owner,"observer")||integer(caster,"playerHp")<=0
    ||(long)CombatAccess.field(owner,"currentRoomId")!=room||hub.battleMapId(room,owner)!=map
    ||CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",actor.objectId())!=companion||!actor.active()||actor.hp()<=0
    ||owner!=caster&&!hub.canAttack(room,caster,owner)||Math.max(Math.abs(x-actor.x()),Math.abs(y-actor.y()))>spell.maximumRange()
    ||!BattleSpellCatalog.isInNativeScreen(x,y,actor.x(),actor.y())||!BattleSpellCatalog.hasNativeLineOfSight(ArenaMapCatalog.forMap(map),x,y,actor.x(),actor.y())
    ||!BattleAnimationCatalog.get().supportsTimedAction(caster.battleGfx(),spell.actionId()))return;
  var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");if(summary==null)return;
  var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","()I"));
  int reduction=BattleSpellCatalog.originalMagicConsumeReduction(summary.type(),Math.max(0,summary.originalIntelligence()));int cost=NativeSkillConsumptionBridges.WeaponBreakSpell_effectiveMpCost_III(spell,intelligence,reduction);
  long now=System.nanoTime();int mr=companion.effectiveMagicResistance(stats.magicResistance(),now);var eye=(BattleDragonEyeStatus.Snapshot)CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
  int probability=CombatProbability.dispatch(spell,"probabilityForTarget","(IIIIIIZ)I","casterLevel,targetLevel,effectiveIntelligence,magicLevel,totalMagicHit,targetMr,bossTarget",new Object[]{Math.max(1,summary.level()),actor.level(),intelligence,BattleTurnUndead.magicLevel(summary.type(),Math.max(1,summary.level())),Math.addExact(equipment.totalMagicHit(),eye.magicHit()),mr,CombatProbability.bossGate(stats.boss(),spell)},caster,new Object[]{companion,stats});
  var config=(ServerConfig)CombatAccess.field(caster,"config");int roll=config.weaponBreakFixtureD100()>0?config.weaponBreakFixtureD100():ThreadLocalRandom.current().nextInt(1,101);
  var ready=(Map<Integer,Long>)CombatAccess.field(caster,"spellReadyNanos");if(now<ready.getOrDefault(27,0L)||integer(caster,"playerMp")<cost||!NativeLightAction.admitPair(caster,owner,27,spell.actionId(),spell.reuseDelayMillis()))return;
  now=System.nanoTime();CombatAccess.call(caster,"cancelMeditation","(Ljava/lang/String;)V","spell-27");int mp=NativeSkillConsumption.boundMp(caster,integer(caster,"playerMp")-cost);CombatAccess.set(caster,"playerMp",mp);CombatAccess.call(caster,"commitSpellReady","(IJ)V",27,now+spell.reuseDelayMillis()*1_000_000L);
  CombatAccess.call(caster,"sendInventoryDelta","(L"+P+"RuntimeInventory;L"+P+"RuntimeInventory;)V",NativeCastMaterial.previous(caster),NativeCastMaterial.next(caster));
  boolean earth=NativeSourceWeaponPlayerCounter.earth(companion,now),success=!earth&&probability>=roll;companion.consumeEraseMagic(now);companion.cancelSleep();var recipients=hub.roomMembers(room);
  for(var member:recipients)if(!(boolean)CombatAccess.field(caster,"invisible")||member==caster)send(member,Protocol.objectAction(caster.playerObjectId(),spell.actionId()),"S_ACTION Weapon Break companion caster="+caster.playerObjectId());
  int stacks=success?NativeSourceWeaponCounterMagic.stack(companion,caster,recipients,actor.objectId()):-1;boolean applied=stacks>=0;
  if(applied)for(var member:recipients)send(member,Protocol.effectObject(actor.objectId(),spell.effectGfx()),"S_EFFECT Weapon Break target="+actor.objectId());
  caster.applySpellLawfulDelta(spell.lawfulDelta(),spell.key());
  send(caster,Protocol.manaPoint(mp,integer(caster,"playerMaxMp")),"S_MANA_POINT Weapon Break="+mp);if(!applied)send(caster,Protocol.messageCode(spell.failureMessage()),"S_MESSAGE_CODE Weapon-Break-failure="+spell.failureMessage());
  System.out.println("[LiTo] BATTLE companion Weapon Break target="+actor.objectId()+" probability="+probability+" roll="+roll+" stacks="+stacks+" earth="+earth+" mp="+mp);
 }
 static int integer(Object object,String field){return (int)CombatAccess.field(object,field);}
 static void send(ClientSession receiver,byte[] packet,String note)throws IOException{CombatCompanionSpells.send(receiver,packet,note);}
 private NativeWeaponBreakBattleCompanion(){}
}
