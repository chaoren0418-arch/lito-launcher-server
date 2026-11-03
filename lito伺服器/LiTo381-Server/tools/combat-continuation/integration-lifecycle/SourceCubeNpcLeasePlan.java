package local.lito381.server;
import java.util.*;
/** Actual cube engines/status maps plus detached identity metadata; no manufactured BATTLE/Hub/PC. */
public final class SourceCubeNpcLeasePlan {
 static int cases;static void check(boolean b,String why){if(!b)throw new AssertionError(why);cases++;}
 static NativeSourceCubeOrigin.Target target(long id,int kind,Object actor,Object owner,Object life,Object generation){return new NativeSourceCubeOrigin.Target(id,kind,null,null,actor,owner,life,generation);}
 static NativeSourceCubeOrigin.Scan scan(Object engine,NativeSourceCubeOrigin.Target target){return new NativeSourceCubeOrigin.Scan(NativeSourceCubeOrigin.epoch(engine),target==null?Map.of():Map.of(target.id(),target));}
 static Map<?,?> statuses(Object engine){return(Map<?,?>)CombatAccess.field(engine,"statuses");}
 static NativeSourceCubeOrigin.Lease lease(Object actual){return(NativeSourceCubeOrigin.Lease)CombatAccess.field(actual,"nativeSourceCubeOrigin");}
 static void retire(Object engine,NativeSourceCubeOrigin.Scan scan){synchronized(engine){NativeSourceCubeNpcLease.retireScanned(engine,scan);}}
 public static void main(String[]args){
  Object actor=new Object(),owner=new Object(),life=new Object(),generation=new Object();var t=target(20,1,actor,owner,life,generation);var engine=new BattleCubeIgnitionEffects();var cube=engine.place(10,100,100,0).cube();
  var originalScan=scan(engine,t);CombatAccess.set(engine,"nativeSourceCubeScan",originalScan);var actors=List.of(new BattleCubeIgnitionEffects.Actor(20,100,100,true,BattleCubeIgnitionEffects.ActorKind.MONSTER));
  var first=engine.advance(0,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).get(0);var bound=((NativeSourceCubeOrigin.Event)first.nativeSourceCubeOrigin).lease();
  check(bound.target()==t&&bound.actual()==statuses(engine).values().iterator().next(),"actual original status captured detached target once");
  check(NativeSourceCubeNpcLease.keep(bound,originalScan,engine,20),"original exact actor/owner/generation/life retained");
  check(!NativeSourceCubeNpcLease.keep(bound,scan(engine,target(20,1,new Object(),owner,life,generation)),engine,20),"same-id replacement actor rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,scan(engine,target(20,1,actor,owner,new Object(),generation)),engine,20),"same actor new life rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,scan(engine,target(20,1,actor,new Object(),life,generation)),engine,20),"same actor different owner rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,scan(engine,target(20,1,actor,owner,life,new Object())),engine,20),"new encounter generation rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,scan(engine,target(20,2,actor,owner,life,generation)),engine,20),"Quest cannot become Time actor");
  check(!NativeSourceCubeNpcLease.keep(bound,scan(engine,null),engine,20),"retired/missing original actor rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,null,engine,20),"no scan cannot grant identity");
  check(!NativeSourceCubeNpcLease.keep(bound,new NativeSourceCubeOrigin.Scan(new Object(),originalScan.targets()),engine,20),"old engine generation rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,originalScan,new Object(),20),"different engine rejected");
  check(!NativeSourceCubeNpcLease.keep(bound,originalScan,engine,21),"different target id rejected");
  check(!NativeSourceCubeNpcLease.natural(target(20,0,actor,owner,life,generation))&&!NativeSourceCubeNpcLease.natural(target(20,3,actor,owner,life,generation)),"PC/BOT not classified as natural Source monster");
  check(!NativeSourceCubeNpcLease.natural(target(20,1,actor,owner,null,generation))&&!NativeSourceCubeNpcLease.natural(target(20,1,actor,owner,life,null)),"incomplete life/generation cannot grant authority");
  retire(engine,scan(engine,target(20,1,actor,owner,life,generation)));check(statuses(engine).size()==1&&lease(statuses(engine).values().iterator().next())==bound,"unchanged scan preserves actual original lease");
  var moved=List.of(new BattleCubeIgnitionEffects.Actor(20,150,150,true,BattleCubeIgnitionEffects.ActorKind.MONSTER));engine.advance(1000000000L,moved,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY);retire(engine,originalScan);check(statuses(engine).size()==1,"movement outside cube does not end the target status");
  var replacement=target(20,1,new Object(),owner,new Object(),generation);var replacementScan=scan(engine,replacement);retire(engine,replacementScan);check(statuses(engine).isEmpty(),"same-id new actor retires old map lease");
  CombatAccess.set(engine,"nativeSourceCubeScan",replacementScan);var fresh=engine.advance(2000000000L,actors,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY).stream().filter(e->e.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_APPLIED).findFirst().orElseThrow();var second=((NativeSourceCubeOrigin.Event)fresh.nativeSourceCubeOrigin).lease();
  check(second!=bound&&second.actual()!=bound.actual()&&second.target()==replacement,"new actor receives its own fresh status rather than inheriting/blocking");
  check(((NativeSourceCubeOrigin.Event)first.nativeSourceCubeOrigin).lease()==bound&&bound.target()==t,"old already-created event keeps original target; retirement cannot retarget it");
  retire(engine,new NativeSourceCubeOrigin.Scan(new Object(),Map.of()));check(statuses(engine).size()==1,"stale scan cannot mutate the live status map");
  retire(engine,null);check(statuses(engine).size()==1,"unbound compatibility scan does not mutate map");
  retire(engine,scan(engine,null));check(statuses(engine).isEmpty(),"actual actor retirement clears its old status");
  var harmony=new BattleCubeHarmonyEffects();harmony.place(10,100,100,0);var time=target(40,2,new Object(),new Object(),new Object(),new Object());CombatAccess.set(harmony,"nativeSourceCubeScan",scan(harmony,time));var crowd=List.of(new BattleCubeHarmonyEffects.Actor(40,100,100,true,BattleCubeHarmonyEffects.ActorKind.MONSTER));harmony.advance(0,crowd,(a,b)->BattleCubeHarmonyEffects.Relation.ALLY);retire(harmony,scan(harmony,time));check(statuses(harmony).size()==1,"Time Harmony exact actor lease retained");
  retire(harmony,scan(harmony,target(40,2,time.actor(),time.owner(),new Object(),time.generation())));check(statuses(harmony).isEmpty(),"Harmony life replacement retires stale MP/HP status source");
  var mixed=new BattleCubeIgnitionEffects();mixed.place(10,100,100,0);var group=List.of(new BattleCubeIgnitionEffects.Actor(1,100,100,true,BattleCubeIgnitionEffects.ActorKind.PLAYER),new BattleCubeIgnitionEffects.Actor(2,100,100,true,BattleCubeIgnitionEffects.ActorKind.BOT),new BattleCubeIgnitionEffects.Actor(3,100,100,true,BattleCubeIgnitionEffects.ActorKind.MONSTER));mixed.advance(0,group,(a,b)->BattleCubeIgnitionEffects.Relation.ENEMY);retire(mixed,scan(mixed,null));check(statuses(mixed).size()==2&&statuses(mixed).values().stream().noneMatch(x->String.valueOf(CombatAccess.field(x,"targetKind")).equals("MONSTER")),"NPC retirement leaves PC/BOT original paths untouched");
  boolean refused=false;try{NativeSourceCubeNpcLease.retireScanned(mixed,scan(mixed,null));}catch(IllegalStateException expected){refused=true;}check(refused,"mutation requires original engine monitor");
  int before=statuses(mixed).size();NativeSourceCubeNpcLease.retire(mixed);check(statuses(mixed).size()==before,"actual ingress leaves unloaded/unbound compatibility engine unchanged");
  if(cases!=27)throw new AssertionError(cases);System.out.println("PASS27 actual cube maps and detached NPC lease identity scenarios; no player/Hub/SQL/socket/BATTLE fixture and no gameplay qualification");
 }
}
