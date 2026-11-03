package local.lito381.server;
import java.util.*;
/** Actual unbound cube factories/clock and detached tuple contracts only; no bound material/HP fixture. */
public final class SourceCubePlacementPlan {
 static int cases;static void check(boolean v,String why){if(!v)throw new AssertionError(why);cases++;}
 public static void main(String[]a){
  check(NativeSourceCubePlacement.cadence(true)==1000,"actual Source StartTimer_Skill bootstrap period (leaf start500 is not invoked)");check(NativeSourceCubePlacement.cadence(false)==1000,"original unloaded/WORLD scan cadence");
  var ignition=new BattleCubeIgnitionEffects();var harmony=new BattleCubeHarmonyEffects();check(NativeSourceCubePlacement.scanInterval(ignition)==1000,"unbound ignition does not gain Source scene");check(NativeSourceCubePlacement.scanInterval(harmony)==1000,"unbound harmony does not gain Source scene");
  var i=ignition.place(7,10,10,1);var h=harmony.place(7,10,10,1);check(i.accepted()&&h.accepted(),"actual unbound original cube factories");
  var ie=(NativeSourceCubeOrigin.Entity)i.cube().nativeSourceCubeEntity;var he=(NativeSourceCubeOrigin.Entity)h.cube().nativeSourceCubeEntity;
  check(ie.cube()==i.cube()&&ie.engine()==ignition&&ie.epoch()==NativeSourceCubeOrigin.epoch(ignition),"actual ignition object and engine epoch retained");check(he.cube()==h.cube()&&he.engine()==harmony&&he.epoch()==NativeSourceCubeOrigin.epoch(harmony),"actual harmony object and engine epoch retained");
  check(ie.scene()==null&&ie.creator()==null&&he.scene()==null&&he.creator()==null,"unscoped original constructors grant no player authority");check(ie.npcId()==80149&&he.npcId()==80152,"actual distinct effect NPC kinds");check(i.cube().nativeSourceCubeFaction==null&&h.cube().nativeSourceCubeFaction==null,"no invented original quest faction");
  check(!ignition.place(7,12,10,1).accepted()&&!harmony.place(7,12,10,1).accepted(),"original overlap rejection retained");
  var scene=new NativeSourceCubeOrigin.Scene(null,new Object(),7);var d=new NativeSourceCubePlacement.Draft(ignition,NativeSourceCubeOrigin.epoch(ignition),null,scene,new NativeSourceCubeOrigin.Creator(null,null));
  check(NativeSourceCubePlacement.same(d),"detached tuple structural equality only, no role authorization");
  CombatAccess.set(ignition,"nativeSourceCubeScene",new NativeSourceCubeOrigin.Scene(null,scene.room(),7));check(!NativeSourceCubePlacement.same(d),"same-id replacement scene retires draft");CombatAccess.set(ignition,"nativeSourceCubeScene",null);
  boolean rejected=false;try{NativeSourceCubePlacement.publish(d,i.cube());}catch(IllegalStateException expected){rejected=true;}check(rejected&&NativeSourceCubeOrigin.scene(ignition)==null,"unbound cube cannot publish staged Source scene");
  check(NativeSourceCubePlacement.prepare(ignition,7,null,null,7)==null,"absent original Batch rejects before role/cost lookup");
  var ia=new BattleCubeIgnitionEffects.Actor(8,10,10,true,BattleCubeIgnitionEffects.ActorKind.PLAYER);ignition.advance(1,List.of(ia),(source,target)->BattleCubeIgnitionEffects.Relation.ENEMY);check(ignition.advance(3_899_999_999L,List.of(),(source,target)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().noneMatch(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE),"unbound status clock does not fire before original fourth callback");
  check(ignition.advance(3_900_000_001L,List.of(),(source,target)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().anyMatch(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE),"original ignition900/1000ms clock retained");
  var ha=new BattleCubeHarmonyEffects.Actor(8,10,10,true,BattleCubeHarmonyEffects.ActorKind.PLAYER);harmony.advance(1,List.of(ha),(source,target)->BattleCubeHarmonyEffects.Relation.ENEMY);check(harmony.advance(3_900_000_001L,List.of(),(source,target)->BattleCubeHarmonyEffects.Relation.ENEMY).stream().anyMatch(e->e.kind()==BattleCubeHarmonyEffects.EventKind.MP_RECOVERY),"original harmony fourth MP callback retained");
  check(harmony.advance(4_900_000_001L,List.of(),(source,target)->BattleCubeHarmonyEffects.Relation.ENEMY).stream().anyMatch(e->e.kind()==BattleCubeHarmonyEffects.EventKind.HP_DAMAGE),"original harmony fifth HP callback retained");
  ignition.clear();check(!NativeSourceCubePlacement.same(d),"original clear retires old factory epoch");
  if(cases!=20)throw new AssertionError(cases);System.out.println("PASS20 actual unbound cube/epoch/overlap/status-clock and detached draft/cadence contracts; no bound MP/material/cost-rollback/HP/role/Main/game/MySQL qualification");
 }
}
