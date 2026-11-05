package local.lito381.server;
import java.util.*;
/** Actual material two-copy/commit/install path. Drafts never retain the original Batch or Ingress. */
final class NativeSourceGroundPlacement {
 record Draft(BattleGroundEffects live,Object epoch,Object previousScene,NativeSourceGroundOrigin.Scene scene,NativeSourceGroundOrigin.Creator creator){}
 static boolean same(Draft d){return d!=null&&d.live()!=null&&d.epoch()!=null&&d.scene()!=null&&d.creator()!=null&&d.live().nativeSourceGroundEpoch==d.epoch()&&d.live().nativeSourceGroundScene==d.previousScene()&&(d.previousScene()==null||d.previousScene()==d.scene());}
 static BattleGroundEffects.CastResult cast(BattleGroundEffects engine,BattleSpellCatalog.GroundSpell spell,long caster,int sx,int sy,int tx,int ty,ArenaMapCatalog.Arena arena,long now,GameHub hub,Runnable commit){
  if(!Thread.holdsLock(hub)||!CombatRules.loaded()||spell==null||spell.skillId()!=58)return NativeGroundMaterialAction.nativeSourceGroundPlacementOriginalBattle(engine,spell,caster,sx,sy,tx,ty,arena,now,hub,commit);
  var batch=NativeLightAction.nativeSourceGroundPlacementBatch();
  if(batch==null)return NativeGroundMaterialAction.nativeSourceGroundPlacementOriginalBattle(engine,spell,caster,sx,sy,tx,ty,arena,now,hub,commit);
  var pc=(ClientSession)CombatAccess.field(batch,"pc");var accepted=CombatAccess.field(batch,"accepted");
  if(pc==null||accepted==null)return BattleGroundEffects.CastResult.rejected();
  var binding=(NativePlayerCommit.Binding)CombatAccess.field(accepted,"binding");
  if(binding==null||!"BATTLE".equals(binding.phase()))return NativeGroundMaterialAction.nativeSourceGroundPlacementOriginalBattle(engine,spell,caster,sx,sy,tx,ty,arena,now,hub,commit);
  if(CombatAccess.field(accepted,"pc")!=pc||CombatAccess.field(accepted,"hub")!=hub||binding.object()!=caster||!NativeAcceptedFreezeMutation.roleFrame(binding,pc)||!pc.nativePlayerActionLock().isHeldByCurrentThread()||(boolean)CombatAccess.field(pc,"sourceItemDisconnected"))return BattleGroundEffects.CastResult.rejected();
  var room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(binding.room());if(room==null||CombatAccess.field(room,"groundEffects")!=engine)return BattleGroundEffects.CastResult.rejected();
  var old=engine.nativeSourceGroundScene;NativeSourceGroundOrigin.Scene scene;
  if(old==null)scene=new NativeSourceGroundOrigin.Scene(hub,room,binding.room());
  else if(old instanceof NativeSourceGroundOrigin.Scene s&&s.hub()==hub&&s.room()==room&&s.id()==binding.room())scene=s;
  else return BattleGroundEffects.CastResult.rejected();
  var draft=new Draft(engine,engine.nativeSourceGroundEpoch,old,scene,new NativeSourceGroundOrigin.Creator(pc,binding));var previous=engine.nativeSourceGroundInstalling;engine.nativeSourceGroundInstalling=draft;
  try{return NativeGroundMaterialAction.nativeSourceGroundPlacementOriginalBattle(engine,spell,caster,sx,sy,tx,ty,arena,now,hub,commit);}finally{engine.nativeSourceGroundInstalling=previous;}
 }
 static BattleGroundEffects copy(BattleGroundEffects live){
  var copy=NativeGroundMaterialAction.battleCopy(live);
  if(live.nativeSourceGroundInstalling instanceof Draft d){
   if(d.live()!=live||!same(d))throw new IllegalStateException("ground owner changed under staged placement");
   copy.nativeSourceGroundEpoch=d.epoch();copy.nativeSourceGroundScene=d.scene();copy.nativeSourceGroundInstalling=d;
  }
  return copy;
 }
 static Object entity(Object actual,BattleGroundEffects engine){
  if(!(engine.nativeSourceGroundInstalling instanceof Draft d))return NativeSourceGroundOrigin.nativeSourceGroundPlacementOriginalEntity(actual,engine);
  if(!same(d)||engine.nativeSourceGroundEpoch!=d.epoch()||engine.nativeSourceGroundScene!=d.scene())throw new IllegalStateException("ground draft provenance changed");
  if((int)CombatAccess.field(actual,"skillId")!=58)return null;
  if(d.creator().binding()!=null&&(long)CombatAccess.field(actual,"casterObjectId")!=d.creator().binding().object())throw new IllegalStateException("ground creator changed in staged lease");
  return new NativeSourceGroundOrigin.Entity(actual,d.live(),d.epoch(),d.scene(),d.creator());
 }
 static void install(Object original,Object staged){
  if(original instanceof BattleGroundEffects live&&staged instanceof BattleGroundEffects copy&&live.nativeSourceGroundInstalling instanceof Draft d){
   if(d.live()!=live||!same(d)||copy.nativeSourceGroundInstalling!=d||copy.nativeSourceGroundEpoch!=d.epoch()||copy.nativeSourceGroundScene!=d.scene())throw new IllegalStateException("ground commit lost its original draft");
   // Publish only at the original install boundary, after the original material callback accepted.
   live.nativeSourceGroundScene=d.scene();
  }
  NativeGroundMaterialAction.install(original,staged);
 }
 private NativeSourceGroundPlacement(){}
}
