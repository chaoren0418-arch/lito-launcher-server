// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** Actual controlled NPC target; original skill167 selection/cost/probability/output ports. */
final class NativeCompanionWindAction {
 static void run(ClientSession pc,int skill,NativeSelfCastAction.Task body)throws IOException{
  if(skill!=167){NativeCompanionForestArea.run(pc,skill,body);return;}var c=new NativeAreaMaterialAction.Context(pc);if(!c.phase.equals("WORLD")&&!c.phase.equals("BATTLE"))return;
  try(var scope=new NativeCompanionTargets.Scope(c)){if(!scope.valid)return;CombatAccess.set(c,"nativeCompanionTargetScope",scope);CombatAccess.set(c,NativeCompanionNegative.STAGE,1);var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);
   try{NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(pc,skill,()->{if(!c.scene()||NativeAreaMaterialAction.current.get()!=c)return;c.npcs.entrySet().removeIf(e->e.getValue().kind()==3&&scope.receipts.containsKey((ClientSession)e.getValue().owner())&&!scope.owns(c,(ClientSession)e.getValue().owner()));CombatAccess.set(c,NativeCompanionNegative.STAGE,0);try{body.run();}finally{CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}},c);}
   finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);CombatAccess.set(c,"nativeCompanionTargetScope",null);}
  }
 }
 static NativeAreaMaterialAction.Npc target(ClientSession pc,long id){var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!c.scene())return null;var n=c.npcs.get(id);if(n==null||n.kind()!=3||!c.npc(id))return null;var owner=(ClientSession)n.owner();var scope=NativeCompanionTargets.scope(c);return scope!=null&&(owner==pc||scope.owns(c,owner))?n:null;}
 static int[] select(ClientSession pc,long id,boolean present,int x,int y){if(present)return new int[]{1,x,y};var n=target(pc,id);if(n==null)return new int[]{0,x,y};var snap=((BattleCompanion)n.actor()).snapshot();int cx=NativePlayerCommit.integer(pc,"positionX"),cy=NativePlayerCommit.integer(pc,"positionY");if(NativeCompanionPlacement.world(pc)){var map=ClassicWorldMapCatalog.forMap(NativeCompanionPlacement.map(pc));if(map.isSafetyZone(cx,cy)||map.isSafetyZone(snap.x(),snap.y()))return new int[]{0,x,y};}return BattleSpellCatalog.isInNativeScreen(cx,cy,snap.x(),snap.y())?new int[]{1,snap.x(),snap.y()}:new int[]{0,x,y};}
 static boolean admit(ClientSession pc,int skill,int action,int reuse,long id){var c=NativeAreaMaterialAction.current.get();var n=c==null?null:c.npcs.get(id);if(n!=null&&n.kind()==3&&target(pc,id)==null)return false;return NativeAreaMaterialAction.admit(pc,skill,action,reuse);}
 static int[] apply(ClientSession pc,BattleSpellCatalog.WindShackleSpell spell,long id,int level,int intelligence,int magicHit,int fixture,long now){
  if(NativeLightAction.currentMaterialProducer()!=pc||!NativeLightAction.retainedMaterial(pc))throw new IllegalStateException("NPC wind requires its original admitted material context");var n=target(pc,id);if(n==null)return new int[]{0,0};var c=NativeAreaMaterialAction.current.get();var pet=(BattleCompanion)n.actor();synchronized(pet){var timed=(BattleTimedStatus)CombatAccess.field(pet,"timedStatus");synchronized(timed){var life=NativePlayerLifetime.of(c.hub);synchronized(life.admission){
   if(target(pc,id)!=n)return new int[]{0,0};var row=NativeSkillTargetMetadata.row(167);var snap=pet.snapshot();int x=NativePlayerCommit.integer(pc,"positionX"),y=NativePlayerCommit.integer(pc,"positionY");if((row.targetTo()&2)==0||row.area()!=0||row.ranged()>=0&&Math.max(Math.abs(x-snap.x()),Math.abs(y-snap.y()))>row.ranged())return new int[]{0,0};if(c.phase.equals("WORLD")){var map=ClassicWorldMapCatalog.forMap(c.map);if(map.isSafetyZone(x,y)||map.isSafetyZone(snap.x(),snap.y())||(!row.through()||(row.type()&64)!=0)&&!map.attackPosition(x,y,snap.x(),snap.y(),15))return new int[]{0,0};}
   if(BattleAbsoluteBarrierRules.blocksIncomingSpell(timed.absoluteBarrierActive(now),167))return new int[]{0,0};int chance=ClientSession.nativeCompanionWindChance(spell,level,snap.level(),intelligence,0,magicHit,pet.effectiveMagicResistance(pet.magicResistance(),now),CombatProbability.bossGate(pet.boss(),spell),pc,new Object[]{pet});int roll=fixture>0?fixture:ThreadLocalRandom.current().nextInt(1,101);boolean blocked=BattleEarthBindRules.blocksProbabilityMagic(pet.earthBindActive(now),167);pet.consumeEraseMagic(now);pet.cancelSleep();if(blocked||chance<roll)return new int[]{1,0};
   boolean bypass=spell.counterMagicBypass()||((Set<?>)CombatAccess.field(pc,"sourceCounterMagicBypassSkills")).contains(167);if(!bypass&&timed.consumeCounterMagic(now)){publish(pc,c,id,BattleSpellCatalog.counterMagicSpell(31).effectGfx());return new int[]{1,0};}
   if(!NativeCompanionWindState.activate(pet,now,spell.durationSeconds()))return new int[]{1,0};publish(pc,c,id,spell.effectGfx());return new int[]{1,1};
  }}}
 }
 static void publish(ClientSession pc,NativeAreaMaterialAction.Context c,long id,int gfx){
  CombatAccess.call(pc,"publishWindShackleEffect","(Ljava/util/List;JILlocal/lito381/server/ClientSession$Phase;II)V",c.players.stream().filter(p->NativeSourceIndirectPlayerProof.player(c,p)).toList(),id,gfx,CombatAccess.field(pc,"phase"),c.map,c.instance);
 }
 private NativeCompanionWindAction(){}
}
