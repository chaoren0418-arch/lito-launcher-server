package local.lito381.server;
import java.util.*;
/** Real cube engines/leases and recorded event identity, no player/Hub/SQL/socket or BATTLE fixture. */
public final class SourceCubeOriginPlan {
 static int cases;static void check(boolean b,String name){if(!b)throw new AssertionError(name);cases++;}
 static NativeSourceCubeOrigin.Entity entity(Object cube){return(NativeSourceCubeOrigin.Entity)CombatAccess.field(cube,"nativeSourceCubeEntity");}
 static NativeSourceCubeOrigin.Event event(Object event){return(NativeSourceCubeOrigin.Event)CombatAccess.field(event,"nativeSourceCubeOrigin");}
 public static void main(String[]args){
  var ignition=new BattleCubeIgnitionEffects();var placement=ignition.place(10,100,100,0);var cube=placement.cube();var effect=entity(cube);
  check(effect.cube()==cube&&effect.engine()==ignition,"actual installed effect object, not caster identity");
  check(effect.npcId()==80149&&effect.objectId()==cube.objectId()&&effect.objectId()!=cube.casterObjectId(),"actual ignition NPC/object id");
  check(effect.epoch()==NativeSourceCubeOrigin.epoch(ignition),"original engine generation captured");
  var actors=List.of(new BattleCubeIgnitionEffects.Actor(20,100,100,true,BattleCubeIgnitionEffects.ActorKind.PLAYER));
  var applied=ignition.advance(0,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).get(0);var first=event(applied);
  check(first!=null&&first.lease().effect()==effect,"status keeps the actual original effect");
  check(first.lease().actual()!=null&&first.lease().expires()==8000000000L,"actual status lease and eight-second deadline");
  check(first.lease().target()==null,"unbound pure engine cannot manufacture a real player identity");
  var damage=ignition.advance(3900000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).findFirst().orElseThrow();
  check(event(damage).lease()==first.lease(),"periodic event carries exact original status, no fresh id lookup");
  var renewed=ignition.advance(8000000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY);var next=renewed.stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_APPLIED).findFirst().orElseThrow();
  check(event(next).lease().actual()!=first.lease().actual()&&event(next).lease().effect()==effect,"same cube/id status renewal has a different actual lease");
  var expired=renewed.stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_EXPIRED).findFirst().orElseThrow();
  check(event(expired).lease()==first.lease(),"old expiry event remains bound to old lease after renewal");
  ignition.clear();check(NativeSourceCubeOrigin.epoch(ignition)!=effect.epoch(),"clear retires original engine generation");
  var replacement=ignition.place(10,100,100,9000000000L).cube();check(entity(replacement).cube()!=cube&&entity(replacement).epoch()!=effect.epoch(),"new installation cannot borrow old effect generation");
  var lingering=new BattleCubeIgnitionEffects();var original=lingering.place(10,100,100,0).cube();lingering.advance(19000000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY);
  var afterDespawn=lingering.advance(23000000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).findFirst().orElseThrow();
  check(lingering.active(23000000000L).isEmpty()&&event(afterDespawn).lease().effect().cube()==original,"existing eight-second status retains source object after twenty-second cube despawn");
  var harmony=new BattleCubeHarmonyEffects();var balance=harmony.place(30,100,100,0).cube();check(entity(balance).npcId()==80152,"harmony actual effect NPC type");
  var people=List.of(new BattleCubeHarmonyEffects.Actor(40,100,100,true,BattleCubeHarmonyEffects.ActorKind.PLAYER));var installed=harmony.advance(0,people,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY).get(0);var lease=event(installed).lease();
  var mp=harmony.advance(3900000000L,people,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY).stream().filter(e->e.kind()==BattleCubeHarmonyEffects.EventKind.MP_RECOVERY).findFirst().orElseThrow();check(event(mp).lease()==lease,"harmony MP keeps original lease");
  var hp=harmony.advance(4900000000L,people,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY).stream().filter(e->e.kind()==BattleCubeHarmonyEffects.EventKind.HP_DAMAGE).findFirst().orElseThrow();check(event(hp).lease()==lease&&lease.effect().cube()==balance,"harmony HP keeps original effect/lease");
  if(cases!=15)throw new AssertionError(cases);System.out.println("PASS15 actual cube engine/status/event provenance cases, no player/Hub/SQL/socket/BATTLE fixture; HP source wiring remains open");
 }
}
