package local.lito381.server;
import java.util.*;
/** Actual native cube events and strict detached provenance gates; no player/Hub/SQL/BATTLE fixture. */
public final class SourceCubeNpcPlan {
 static int cases;static void check(boolean b,String why){if(!b)throw new AssertionError(why);cases++;}
 static NativeSourceCubeOrigin.Target target(long id,int kind){return new NativeSourceCubeOrigin.Target(id,kind,null,null,new Object(),new Object(),new Object(),new Object());}
 static void scan(Object engine,NativeSourceCubeOrigin.Target target){CombatAccess.set(engine,"nativeSourceCubeScan",new NativeSourceCubeOrigin.Scan(NativeSourceCubeOrigin.epoch(engine),Map.of(target.id(),target)));}
 static NativeSourceCubeOrigin.Lease origin(Object event){return((NativeSourceCubeOrigin.Event)CombatAccess.field(event,"nativeSourceCubeOrigin")).lease();}
 public static void main(String[]args){
  check(NativeSourceCubeNpc.rawDamage(true)==10&&NativeSourceCubeNpc.rawDamage(false)==25,"Source raw fixed damage, no ordinary spell formula");
  check(!NativeSourceCubeNpc.hp(null),"no event cannot become HP authority");
  check(!NativeSourceCubeNpc.accepted(null)&&!NativeSourceCubeNpc.accepted(new Object()),"missing/wrong commit outcome rejected");
  var engine=new BattleCubeIgnitionEffects();var cube=engine.place(10,100,100,0).cube();var t=target(20,1);scan(engine,t);var actors=List.of(new BattleCubeIgnitionEffects.Actor(20,100,100,true,BattleCubeIgnitionEffects.ActorKind.MONSTER));
  var applied=engine.advance(0,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).get(0);check(!NativeSourceCubeNpc.hp(applied),"APPLIED event is not an HP callback");
  var event=engine.advance(3900000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).findFirst().orElseThrow();var lease=origin(event);
  check(NativeSourceCubeNpc.hp(event),"actual original MONSTER periodic HP event");
  check(NativeSourceCubeNpc.matches(event,lease,20,true),"actual original effect/lease/target identity accepted as metadata");
  check(!NativeSourceCubeNpc.matches(event,lease,21,true),"different target id rejected");
  check(!NativeSourceCubeNpc.matches(event,lease,20,false),"Ignition cannot become Harmony source");
  check(!NativeSourceCubeNpc.matches(event,null,20,true),"missing exact original lease rejected");
  var rebound=new NativeSourceCubeOrigin.Lease(lease.effect(),lease.actual(),target(20,1),lease.expires());check(!NativeSourceCubeNpc.matches(event,rebound,20,true),"same-id replacement metadata cannot substitute original event lease");
  var copied=new BattleCubeIgnitionEffects.Event(BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE,cube,20,BattleCubeIgnitionEffects.ActorKind.MONSTER);check(!NativeSourceCubeNpc.matches(copied,lease,20,true),"header-only synthetic event has no original lease authority");
  check(!NativeSourceCubeNpc.hp(new BattleCubeIgnitionEffects.Event(BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE,cube,20,BattleCubeIgnitionEffects.ActorKind.PLAYER)),"PC callback remains parent typed PC path");
  check(!NativeSourceCubeNpc.hp(new BattleCubeIgnitionEffects.Event(BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE,cube,2,BattleCubeIgnitionEffects.ActorKind.BOT)),"BOT is not silently classified as Source Monster");
  var renewed=engine.advance(8000000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_APPLIED).findFirst().orElseThrow();check(!NativeSourceCubeNpc.matches(event,origin(renewed),20,true),"old periodic event cannot adopt renewed same-id status");
  check(origin(event)==lease&&lease.target()==t,"already emitted callback keeps actual original actor metadata");
  var harmony=new BattleCubeHarmonyEffects();harmony.place(10,100,100,0);var time=target(40,2);scan(harmony,time);var crowd=List.of(new BattleCubeHarmonyEffects.Actor(40,100,100,true,BattleCubeHarmonyEffects.ActorKind.MONSTER));harmony.advance(0,crowd,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY);
  var mp=harmony.advance(3900000000L,crowd,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY).stream().filter(e->e.kind()==BattleCubeHarmonyEffects.EventKind.MP_RECOVERY).findFirst().orElseThrow();check(!NativeSourceCubeNpc.hp(mp),"MP callback is a separate unmodified consumer");
  var hp=harmony.advance(4900000000L,crowd,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY).stream().filter(e->e.kind()==BattleCubeHarmonyEffects.EventKind.HP_DAMAGE).findFirst().orElseThrow();check(NativeSourceCubeNpc.hp(hp)&&NativeSourceCubeNpc.matches(hp,origin(hp),40,false),"Harmony actual original Time effect-NPC HP metadata");
  check(!NativeSourceCubeNpc.matches(hp,origin(hp),40,true),"Harmony cannot borrow Ignition guard/formula");
  check(!NativeSourceCubeNpc.dispatch(null,null),"unloaded actual periodic ingress keeps original fallback without Hub");
  check(!NativeSourceCubeNpc.legacy(null,0,-1,-1,event),"unloaded Ignition legacy body retained");
  check(!NativeSourceCubeNpc.legacy(null,0,-1,-1,hp),"unloaded Harmony legacy body retained");
  check(!NativeSourceCubeNpc.before(null,0,null,20,10),"no admitted typed commit cannot mutate HP");
  check(!NativeSourceCubeNpc.rawEffectTarget(new Object()),"no ambient effect reference cannot bypass ordinary earth guard");
  check(NativeCombatAttacker.nativeSourceCubeCurrent()==null,"new getter reads existing ingress without creating authority");
  check(NativeCombatAttacker.earth(true,new Object()),"actual ordinary earth gate is unchanged without a bound effect target");
  var effect=new NativeSourceCubeNpc.Effect(lease.effect(),null,t);var ref=new NativeCombatAttacker.Ref(5,lease.effect().objectId(),effect,0,null,7,-1,-1);var previous=NativeCombatAttacker.enterKnown(ref);try{check(!NativeSourceCubeNpc.rawEffectTarget(t.actor())&&NativeCombatAttacker.earth(true,t.actor()),"unbound effect metadata cannot bypass actual ordinary guard or manufacture BATTLE");}finally{NativeCombatAttacker.leave(previous);}
  check(NativeCombatAttacker.nativeSourceCubeCurrent()==null,"existing ingress restored in finally");
  var packet=new Protocol.Writer().c(Protocol.S_HIT_RATIO).d(0x89ABCDEFL).c(50).bytes();check(NativeSourceCubeNpc.object(packet)==0x89ABCDEFL&&NativeSourceCubeNpc.object(new byte[0])==-1,"actual packet identity decoded little endian and bounded");
  check(CombatAccess.field(cube,"nativeSourceCubeFaction")==null&&NativeSourceCubeNpc.captureFaction(lease.effect())==null,"unbound engine cannot manufacture original creator faction");
  check(NativeSourceCubeNpc.factionAllowed(1,0,0)&&NativeSourceCubeNpc.factionAllowed(1,1,1),"original same Quest lane preserved");
  check(!NativeSourceCubeNpc.factionAllowed(1,0,1)&&!NativeSourceCubeNpc.factionAllowed(1,1,0),"original opposite Quest lane rejected");
  check(!NativeSourceCubeNpc.factionAllowed(1,null,0),"unknown creator faction cannot borrow NPC target lane");
  check(!NativeSourceCubeNpc.factionAllowed(1,2,2),"invalid captured faction rejected");
  check(NativeSourceCubeNpc.factionAllowed(2,null,-1),"Time eligibility does not borrow a player's Quest force");
  check(!NativeSourceCubeNpc.factionAllowed(0,0,0)&&!NativeSourceCubeNpc.factionAllowed(3,0,0),"PC/BOT do not enter natural NPC faction path");
  check(NativeSourceCubeNpc.ratio(2000000000,2000000000)==100&&NativeSourceCubeNpc.ratio(1000000000,2000000000)==50,"large actual HPBAR arithmetic does not overflow int");
  check(NativeSourceCubeNpc.ratio(1,0)==100&&NativeSourceCubeNpc.ratio(-1,100)==0,"invalid maximum and nonpositive HP ratio remain bounded");
  if(cases!=37)throw new AssertionError(cases);System.out.println("PASS37 actual cube event/provenance/raw-gate/fallback/packet contracts; no player/Hub/SQL/socket/BATTLE fixture, not HP/Main gameplay qualification");
 }
}
