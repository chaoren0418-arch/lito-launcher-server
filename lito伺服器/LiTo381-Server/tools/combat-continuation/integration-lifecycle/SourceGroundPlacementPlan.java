package local.lito381.server;
import java.lang.reflect.*;import java.util.*;
/** Original staged copy/install and lease identity contracts; no PC/Hub/material-cost/game/SQL fixture. */
public final class SourceGroundPlacementPlan {
 static int cases;static void check(boolean v,String why){if(!v)throw new AssertionError(why);cases++;}
 static List<Object> leases(BattleGroundEffects engine){return(List<Object>)CombatAccess.field(engine,"leases");}
 static Object lease(BattleGroundEffects engine,int skill)throws Exception{var type=Class.forName("local.lito381.server.BattleGroundEffects$Lease",false,SourceGroundPlacementPlan.class.getClassLoader());var c=type.getDeclaredConstructor(int.class,long.class,int.class,int.class,int.class,long.class,long.class,int.class,BattleGroundEffects.class);c.setAccessible(true);return c.newInstance(skill,7L,1,1,1,10000000000L,1000000000L,1650,engine);}
 static void rejected(Runnable task){try{task.run();throw new AssertionError("must reject retired draft");}catch(IllegalStateException expected){}}
 public static void main(String[]args)throws Exception{
  var live=new BattleGroundEffects();var old=lease(live,58);leases(live).add(old);var baseEpoch=live.nativeSourceGroundEpoch;
  var scene=new NativeSourceGroundOrigin.Scene(null,new Object(),5);var creator=new NativeSourceGroundOrigin.Creator(null,null);var draft=new NativeSourceGroundPlacement.Draft(live,baseEpoch,null,scene,creator);live.nativeSourceGroundInstalling=draft;
  check(NativeSourceGroundPlacement.same(draft),"original live metadata snapshot");
  var staged=NativeSourceGroundPlacement.copy(live);check(staged!=live&&leases(staged).size()==1&&leases(staged).get(0)==old,"original shallow-copy identity and retained old lease");
  check(staged.nativeSourceGroundEpoch==baseEpoch&&staged.nativeSourceGroundScene==scene&&staged.nativeSourceGroundInstalling==draft,"both staged rounds receive detached original owner metadata");
  check(live.nativeSourceGroundScene==null&&live.nativeSourceGroundEpoch==baseEpoch&&leases(live).size()==1,"preflight preparation does not publish live scene or leases");
  var actual=lease(staged,58);var entity=(NativeSourceGroundOrigin.Entity)CombatAccess.field(actual,"nativeSourceGroundEntity");check(entity!=null&&entity.engine()==live&&entity.actual()==actual,"new staged lease identifies live engine, never disposable copy");
  check(entity.creator()==creator&&entity.scene()==scene&&entity.epoch()==baseEpoch,"original creator and scene preserved without fresh capture");
  check(!NativeSourceGroundQueued.sameEntity(entity),"uninstalled scene proof is inactive");
  leases(staged).add(actual);check(leases(live).size()==1,"staged lease remains invisible before accepted install");
  var second=NativeSourceGroundPlacement.copy(live);check(second!=staged&&leases(second).size()==1&&leases(second).get(0)==old,"second original copy starts from live state, not preflight");
  check(second.nativeSourceGroundInstalling==draft,"second copy keeps same original detached draft");
  check(CombatAccess.field(lease(staged,63),"nativeSourceGroundEntity")==null,"non-firewall lease does not gain Source firewall authority");
  NativeSourceGroundPlacement.install(live,staged);check(live.nativeSourceGroundScene==scene&&live.nativeSourceGroundEpoch==baseEpoch&&leases(live).size()==2&&leases(live).get(1)==actual,"original install publishes same staged lease");
  check(NativeSourceGroundQueued.sameEntity(entity),"installed original pointer satisfies queued-scene metadata predicate");
  check(!NativeSourceGroundPlacement.same(draft),"initial unpublished draft cannot be reused after scene publication");
  live.nativeSourceGroundInstalling=null;var plain=NativeSourceGroundPlacement.copy(live);check(plain.nativeSourceGroundScene==null&&plain.nativeSourceGroundInstalling==null&&leases(plain).get(1)==actual,"unmarked copy retains original fallback");
  var next=new NativeSourceGroundPlacement.Draft(live,live.nativeSourceGroundEpoch,scene,scene,creator);live.nativeSourceGroundInstalling=next;live.clear();check(!NativeSourceGroundPlacement.same(next),"clear retires old source scene epoch");
  rejected(()->NativeSourceGroundPlacement.copy(live));check(leases(live).isEmpty(),"retired draft cannot publish effects");
  check(Arrays.stream(NativeSourceGroundPlacement.Draft.class.getDeclaredFields()).noneMatch(f->f.getType().getName().matches(".*(Batch|Ingress|Context|Action|Frame).*")),"detached draft retains no producing scope");
  if(cases!=18)throw new AssertionError(cases);System.out.println("PASS18 actual unbound material copy/install/live-lease provenance contracts; no bound caster/MP/reagent/rollback/HP/Main/game/MySQL qualification");
 }
}
