package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** 技能161補入真正受控NPC，沿用原材料交易、機率與輸出核心。 */
final class NativeCompanionForestArea {
 static void run(ClientSession pc,int skill,NativeSelfCastAction.Task body)throws IOException{
  if(skill!=161){NativeCompanionArea.run(pc,skill,body);return;}
  var c=new NativeAreaMaterialAction.Context(pc);if(!c.phase.equals("WORLD")&&!c.phase.equals("BATTLE"))return;
  try(var scope=new NativeCompanionTargets.Scope(c)){if(!scope.valid)return;CombatAccess.set(c,"nativeCompanionTargetScope",scope);CombatAccess.set(c,NativeCompanionNegative.STAGE,1);var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);
   try{NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(pc,skill,()->{if(!c.scene()||NativeAreaMaterialAction.current.get()!=c)return;c.npcs.entrySet().removeIf(e->e.getValue().kind()==3&&scope.receipts.containsKey((ClientSession)e.getValue().owner())&&!scope.owns(c,(ClientSession)e.getValue().owner()));CombatAccess.set(c,NativeCompanionNegative.STAGE,0);try{body.run();}finally{CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}},c);}
   finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);CombatAccess.set(c,"nativeCompanionTargetScope",null);}
  }
 }
 static boolean eligible(NativeAreaMaterialAction.Context c,NativeAreaMaterialAction.Npc n,BattleSpellCatalog.AreaSilenceSpell spell){
  if(n.kind()!=3||(spell.targetTo()&2)==0||!c.npc(n.id()))return false;var owner=(ClientSession)n.owner();var scope=NativeCompanionTargets.scope(c);if(scope==null||owner!=c.pc&&!scope.owns(c,owner))return false;var snap=((BattleCompanion)n.actor()).snapshot();int x=NativePlayerCommit.integer(c.pc,"positionX"),y=NativePlayerCommit.integer(c.pc,"positionY");if(!BattleSpellCatalog.isInNativeScreen(x,y,snap.x(),snap.y())||spell.area()>=0&&Math.max(Math.abs(x-snap.x()),Math.abs(y-snap.y()))>spell.area())return false;
  if(c.phase.equals("WORLD")){var map=ClassicWorldMapCatalog.forMap(c.map);return !map.isSafetyZone(x,y)&&!map.isSafetyZone(snap.x(),snap.y())&&(spell.through()!=0&&(spell.type()&64)==0||map.attackPosition(x,y,snap.x(),snap.y(),15));}
  return c.phase.equals("BATTLE")&&ForcedCombat.canAttackTarget(c.hub,c.room,c.pc,owner,n.id());
 }
 static boolean empty(List<?> original,ClientSession pc,BattleSpellCatalog.AreaSilenceSpell spell){if(!original.isEmpty())return false;var c=NativeAreaMaterialAction.current.get();return c==null||c.pc!=pc||c.npcs.values().stream().noneMatch(n->eligible(c,n,spell));}
 static void apply(ClientSession pc,BattleSpellCatalog.AreaSilenceSpell spell,int level,int intelligence,int magicHit,int fixture,long now){
  var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||spell.skillId()!=161||NativeLightAction.currentMaterialProducer()!=pc||!NativeLightAction.retainedMaterial(pc))throw new IllegalStateException("area companion effect requires its original committed context");
  for(var n:new ArrayList<>(c.npcs.values()))if(n.kind()==3){var pet=(BattleCompanion)n.actor();synchronized(pet){var timed=(BattleTimedStatus)CombatAccess.field(pet,"timedStatus");synchronized(timed){var life=NativePlayerLifetime.of(c.hub);synchronized(life.admission){
   if(!c.scene()||!eligible(c,n,spell))continue;var snap=pet.snapshot();int mr=pet.effectiveMagicResistance(pet.magicResistance(),now);int chance=ClientSession.nativeForestSilenceChance(spell,level,snap.level(),intelligence,magicHit,mr,CombatProbability.bossGate(pet.boss(),spell),pc,new Object[]{pet});int roll=fixture>0?fixture:ThreadLocalRandom.current().nextInt(1,101);pet.consumeEraseMagic(now);if(chance<roll)continue;timed.activateAreaSilence(now,spell.durationSeconds());
   var receivers=c.players.stream().filter(c::player).toList();CombatAccess.call(pc,"publishAreaSilenceEffect","(Ljava/util/List;JILlocal/lito381/server/ClientSession$Phase;II)V",receivers,n.id(),spell.effectGfx(),CombatAccess.field(pc,"phase"),c.map,c.instance);
  }}}}
 }
 private NativeCompanionForestArea(){}
}
