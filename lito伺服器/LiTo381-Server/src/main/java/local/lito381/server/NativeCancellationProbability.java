// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-probability/build-repro/inputs/NativeCancellationProbability.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.util.concurrent.ThreadLocalRandom;
import java.io.IOException;
import java.util.List;

/** Skill-44 PC probability bridge. Reuses the installed Source actor/control adapters. */
final class NativeCancellationProbability {
 private static final String P="local/lito381/server/";
 static int percentile(){return ThreadLocalRandom.current().nextInt(1,101);}
 static int guardianMr(long lawful){return lawful>=10000&&lawful<=19999?3:lawful>=20000&&lawful<=29999?6:lawful>=30000&&lawful<=39999?9:0;}
 static boolean evaded(int evasion,int roll){if(roll<1||roll>1000)throw new IllegalArgumentException("evasion roll");return evasion>=roll;}
 static boolean succeeds(int chance,int roll){if(chance<0||chance>100||roll<1||roll>100)throw new IllegalArgumentException("probability range");return roll<=chance;}
 static void prepare(ClientSession target)throws IOException{
  if(!CombatRules.loaded())return;
  try{NativeSourcePlayerActionLock.run(target,()->{try{NativeCastMaterialPolicy.nativeOriginalGm(target);}catch(IOException e){throw new java.io.UncheckedIOException(e);}});}
  catch(java.io.UncheckedIOException e){throw e.getCause();}
 }
 static boolean allied(ClientSession caster,ClientSession target){
  var hub=(GameHub)CombatAccess.field(caster,"hub");
  return caster==target||hub.sameClan(caster.sourceCharacterIdentity(),target.sourceCharacterIdentity())||hub.sameClassicParty(caster.sourceCharacterIdentity(),target.sourceCharacterIdentity());
 }
 static boolean safe(ClientSession pc){
  int x=(int)CombatAccess.field(pc,"positionX"),y=(int)CombatAccess.field(pc,"positionY");
  if(String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD"))return ClassicWorldMapCatalog.forMap((int)CombatAccess.field(pc,"classicWorldMapId")).isSafetyZone(x,y);
  // LiTo arena IDs reuse unrelated classic map IDs. Their admitted arena geometry is combat-only.
  var hub=(GameHub)CombatAccess.field(pc,"hub");ArenaMapCatalog.forMap(hub.battleMapId((long)CombatAccess.field(pc,"currentRoomId"),pc));return false;
 }
 static int targetMr(ClientSession target,long now){return CombatAccess.integerCall(target,"effectiveBaseMagicResistance","(J)I",now)+guardianMr(target.characterLawful());}
 static int legacyChance(int diceSum,int originalHit,int equipmentHit,int targetMr){
  int raw=diceSum+(originalHit<<1)+equipmentHit-targetMr;
  return Math.max(1,Math.min(90,raw));
 }
 private static boolean cachedSourceGm(ClientSession target){
  var actor=NativeReuseGate.actor(target);
  if(actor==null||actor.materialPolicy==null||actor.materialPolicy.owner()!=actor.owner||!actor.owner.matches(target))
   throw new IllegalStateException("Source character GM policy must be prepared before probability admission");
  return actor.materialPolicy.gm();
 }
 static int chance(ClientSession caster,ClientSession target,BattleSpellCatalog.CancellationSpell spell,long now){
  if(allied(caster,target))return 100;
  if(safe(caster)||safe(target))return 0;
  // L1MagicPc.calcProbability returns -1 for a GM target, followed by the outer 1% floor.
  if(cachedSourceGm(target))return 1;
  var controls=CombatDatabase.controlsOrNull();
  if(controls!=null&&controls.probabilities().containsKey(44))
   // CombatProbability.playerTarget adds the guardian MR itself; pass the base MR to avoid counting it twice.
   return CombatProbability.dispatch(spell,"unusedConfiguredCancellationFallback","(I)I","targetMr",new Object[]{CombatAccess.integerCall(target,"effectiveBaseMagicResistance","(J)I",now)},caster,new Object[]{target});
  var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");
  var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");
  int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","(J)I",now));
  int count=Math.max(CombatKiringku.magicBonus(intelligence)+CombatRules.magicLevel(summary.type(),summary.level())+(summary.type()==3?1:-1),1);
  int sides=CombatDatabase.value(3,44,"CancellationSpell","probabilityDice"),sum=0;
  if(sides>0)for(int i=0;i<count;i++)sum+=ThreadLocalRandom.current().nextInt(1,sides+1);
  var eye=CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
  return legacyChance(sum,CombatAccess.integerCall(caster,"originalMagicHit","()I"),equipment.totalMagicHit()+CombatAccess.number(eye,"magicHit"),
   targetMr(target,now));
 }
 static boolean allow(ClientSession caster,ClientSession target,BattleSpellCatalog.CancellationSpell spell,long now){
  if(spell.skillId()!=44||target==null)throw new IllegalArgumentException("explicit player cancellation only");
  if(!CombatRules.loaded())return true;
  int chance=chance(caster,target,spell,now),roll=chance==100||chance==0?0:percentile();
  boolean success=chance==100||(chance>0&&succeeds(chance,roll));
  ((BattleEraseMagicStatus)CombatAccess.field(target,"eraseMagicStatus")).consume(now);
  NativeCancellationSleep.removeFog(target);
  System.out.println("[LiTo] Source Cancellation probability target="+target.playerObjectId()+" chance="+chance+" roll="+roll+" success="+success);
  return success;
 }
 /** Called after atomic material and HP admission; the original body or failure tail commits bound MP. */
 static boolean gate(ClientSession caster,ClientSession target,BattleSpellCatalog.CancellationSpell spell,long now,int mpCost)throws IOException{
  if(allow(caster,target,spell,now))return true;
  CombatAccess.call(caster,"cancelMeditation","(Ljava/lang/String;)V","spell-44");
  CombatAccess.set(caster,"playerMp",NativeSkillConsumption.boundMp(caster,(int)CombatAccess.field(caster,"playerMp")-mpCost));
  CombatAccess.call(caster,"cancelAbsoluteBarrier","(Ljava/lang/String;)V","spell-44");
  CombatAccess.call(caster,"commitSpellReady","(IJ)V",44,now+spell.reuseDelayMillis()*1_000_000L);
  var hub=(GameHub)CombatAccess.field(caster,"hub");List<ClientSession> recipients;
  if(String.valueOf(CombatAccess.field(caster,"phase")).equals("WORLD")){
   recipients=((List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(caster,"classicWorldMapId"))).stream()
    .filter(p->(boolean)CombatAccess.call(caster,"sameClassicWorldMap","(L"+P+"ClientSession;)Z",p))
    .filter(p->BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(p,"positionX"),(int)CombatAccess.field(p,"positionY"),(int)CombatAccess.field(caster,"positionX"),(int)CombatAccess.field(caster,"positionY"))).toList();
  }else{long room=(long)CombatAccess.field(caster,"currentRoomId");recipients=hub.roomRules(room).mode()==BattleRules.Mode.QUEST_BATTLE?hub.battleRecipients(room,hub.forceOf(room,caster)):hub.roomMembers(room);}
  for(var p:recipients)if(!(boolean)CombatAccess.field(caster,"invisible")||p==caster)send(p,Protocol.objectAction(caster.playerObjectId(),spell.actionId()),"S_ACTION Cancellation probability-failed caster="+caster.playerObjectId());
  CombatAccess.call(caster,"sendInventoryDelta","(L"+P+"RuntimeInventory;L"+P+"RuntimeInventory;)V",NativeCastMaterial.previous(caster),NativeCastMaterial.next(caster));
  send(caster,Protocol.manaPoint((int)CombatAccess.field(caster,"playerMp"),(int)CombatAccess.field(caster,"playerMaxMp")),"S_MANA_POINT Cancellation probability-failed");
  send(caster,Protocol.messageCode(spell.failureMessage()),"S_MESSAGE_CODE Cancellation probability-failed");
  return false;
 }
 static void send(ClientSession pc,byte[] packet,String note)throws IOException{try{CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,note);}catch(java.io.UncheckedIOException e){throw e.getCause();}}
}
