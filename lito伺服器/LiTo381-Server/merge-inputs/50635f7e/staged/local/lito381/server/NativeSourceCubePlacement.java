// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.util.*;
/** Actual material cube installation, using the original prewait Batch identity and cost body. */
final class NativeSourceCubePlacement {
 @AsRecord(components = {"engine", "epoch", "previousScene", "scene", "creator"})
 static final class Draft {
  private final Object engine,epoch,previousScene;private final NativeSourceCubeOrigin.Scene scene;private final NativeSourceCubeOrigin.Creator creator;final Object nativeSourceCubeTemplate;
  Draft(Object engine,Object epoch,Object previousScene,NativeSourceCubeOrigin.Scene scene,NativeSourceCubeOrigin.Creator creator){
   this.engine=engine;this.epoch=epoch;this.previousScene=previousScene;this.scene=scene;this.creator=creator;
   this.nativeSourceCubeTemplate=NativeSourceCubeTemplate.freeze(engine);
  }
  public Object engine(){return engine;}
  public Object epoch(){return epoch;}
  public Object previousScene(){return previousScene;}
  public NativeSourceCubeOrigin.Scene scene(){return scene;}
  public NativeSourceCubeOrigin.Creator creator(){return creator;}
 }
 static boolean same(Draft d){return d!=null&&d.engine()!=null&&d.epoch()!=null&&d.scene()!=null&&d.creator()!=null&&NativeSourceCubeOrigin.epoch(d.engine())==d.epoch()&&NativeSourceCubeOrigin.scene(d.engine())==d.previousScene()&&(d.previousScene()==null||d.previousScene()==d.scene());}
 static boolean scoped(GameHub hub){return CombatRules.loaded()&&Thread.holdsLock(hub)&&NativeLightAction.nativeSourceGroundPlacementBatch()!=null;}
 static Draft prepare(Object engine,long caster,ClientSession pc,GameHub hub,long roomId){
  var batch=NativeLightAction.nativeSourceGroundPlacementBatch();if(batch==null||CombatAccess.field(batch,"pc")!=pc)return NativeSourceCubeTemplate.ready(null);var ingress=CombatAccess.field(batch,"accepted");if(ingress==null||CombatAccess.field(ingress,"pc")!=pc||CombatAccess.field(ingress,"hub")!=hub)return NativeSourceCubeTemplate.ready(null);
  var binding=(NativePlayerCommit.Binding)CombatAccess.field(ingress,"binding");
  if(binding==null||binding.object()!=caster||!"BATTLE".equals(binding.phase())||binding.room()!=roomId||!NativeAcceptedFreezeMutation.roleFrame(binding,pc)||!pc.nativePlayerActionLock().isHeldByCurrentThread()||(boolean)CombatAccess.field(pc,"sourceItemDisconnected"))return NativeSourceCubeTemplate.ready(null);
  var room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(roomId);String field=engine instanceof BattleCubeIgnitionEffects?"cubeIgnitionEffects":engine instanceof BattleCubeHarmonyEffects?"cubeHarmonyEffects":null;
  if(field==null||room==null||CombatAccess.field(room,field)!=engine)return NativeSourceCubeTemplate.ready(null);var previous=NativeSourceCubeOrigin.scene(engine);NativeSourceCubeOrigin.Scene scene;
  if(previous==null)scene=new NativeSourceCubeOrigin.Scene(hub,room,roomId);else if(previous instanceof NativeSourceCubeOrigin.Scene s&&s.hub()==hub&&s.room()==room&&s.id()==roomId)scene=s;else return NativeSourceCubeTemplate.ready(null);
  return NativeSourceCubeTemplate.ready(new Draft(engine,NativeSourceCubeOrigin.epoch(engine),previous,scene,new NativeSourceCubeOrigin.Creator(pc,binding)));
 }
 static Object entity(Object cube,Object engine){
  if(!(NativeSourceCubeOrigin.installing(engine) instanceof Draft d))return NativeSourceCubeOrigin.nativeSourceCubePlacementOriginalEntity(cube,engine);
  if(d.engine()!=engine||!same(d))throw new IllegalStateException("cube lost original staged installation");
  if(d.creator().binding()!=null&&(long)CombatAccess.call(cube,"casterObjectId","()J")!=d.creator().binding().object())throw new IllegalStateException("cube changed original material creator");
  return new NativeSourceCubeOrigin.Entity(cube,engine,d.epoch(),d.scene(),d.creator(),engine instanceof BattleCubeIgnitionEffects?80149:80152,(long)CombatAccess.call(cube,"objectId","()J"));
 }
 static void publish(Draft d,Object cube){
  if(!same(d)||!(CombatAccess.field(cube,"nativeSourceCubeEntity") instanceof NativeSourceCubeOrigin.Entity e)||e.cube()!=cube||e.engine()!=d.engine()||e.epoch()!=d.epoch()||e.scene()!=d.scene()||e.creator()!=d.creator())throw new IllegalStateException("cube accepted without its original entity");
  // The original geometry/cost/constructor/list insertion has already succeeded.
  CombatAccess.set(d.engine(),"nativeSourceCubeScene",d.scene());
  NativeSourceCubeVisibility.seal(d,cube);
 }
 static BattleCubeIgnitionEffects.Placement place(BattleCubeIgnitionEffects engine,long caster,int x,int y,long now,ClientSession pc,GameHub hub,long room){
  if(!scoped(hub))return engine.nativeMaterialPlace(caster,x,y,now,pc);var d=prepare(engine,caster,pc,hub,room);if(d==null)return BattleCubeIgnitionEffects.Placement.rejected();var previous=NativeSourceCubeOrigin.installing(engine);CombatAccess.set(engine,"nativeSourceCubeInstalling",d);
  try{var result=engine.nativeMaterialPlace(caster,x,y,now,pc);if(result.accepted())publish(d,result.cube());return result;}finally{CombatAccess.set(engine,"nativeSourceCubeInstalling",previous);}
 }
 static BattleCubeHarmonyEffects.Placement place(BattleCubeHarmonyEffects engine,long caster,int x,int y,long now,ClientSession pc,GameHub hub,long room){
  if(!scoped(hub))return engine.nativeMaterialPlace(caster,x,y,now,pc);var d=prepare(engine,caster,pc,hub,room);if(d==null)return BattleCubeHarmonyEffects.Placement.rejected();var previous=NativeSourceCubeOrigin.installing(engine);CombatAccess.set(engine,"nativeSourceCubeInstalling",d);
  try{var result=engine.nativeMaterialPlace(caster,x,y,now,pc);if(result.accepted())publish(d,result.cube());return result;}finally{CombatAccess.set(engine,"nativeSourceCubeInstalling",previous);}
 }
 static long cadence(boolean source){return source?1000L:1000L;}
 /** Actual StartTimer_Skill bootstrap scans at1000ms; the unused leaf start500 is not the production path. Status900/1000 stays separate. */
 static long scanInterval(Object engine){return cadence(CombatRules.loaded()&&NativeSourceCubeOrigin.scene(engine) instanceof NativeSourceCubeOrigin.Scene);}
 private NativeSourceCubePlacement(){}
}
