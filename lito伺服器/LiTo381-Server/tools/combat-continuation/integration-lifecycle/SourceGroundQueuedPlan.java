package local.lito381.server;
import java.lang.reflect.*;import java.util.*;
/** Real lease/scene pointer and actor tuple contracts only; no PC/Hub/bound HP/game/SQL fixture. */
public final class SourceGroundQueuedPlan {
 static int cases;static void check(boolean v,String why){if(!v)throw new AssertionError(why);cases++;}
 static Object lease(BattleGroundEffects e)throws Exception{var type=Class.forName("local.lito381.server.BattleGroundEffects$Lease",false,SourceGroundQueuedPlan.class.getClassLoader());var c=type.getDeclaredConstructor(int.class,long.class,int.class,int.class,int.class,long.class,long.class,int.class,BattleGroundEffects.class);c.setAccessible(true);return c.newInstance(58,7L,1,1,1,10000000000L,1000000000L,1650,e);}
 static void attach(Object lease,Object entity)throws Exception{var f=lease.getClass().getDeclaredField("nativeSourceGroundEntity");f.setAccessible(true);f.set(lease,entity);}
 public static void main(String[]args)throws Exception{
  var engine=new BattleGroundEffects();var scene=new NativeSourceGroundOrigin.Scene(null,new Object(),5);engine.nativeSourceGroundScene=scene;var actual=lease(engine);var creator=new NativeSourceGroundOrigin.Creator(null,null);var entity=new NativeSourceGroundOrigin.Entity(actual,engine,engine.nativeSourceGroundEpoch,scene,creator);
  check(!NativeSourceGroundQueued.sameEntity(null),"missing lease proof rejects");check(!NativeSourceGroundQueued.sameEntity(entity),"unbound lease cannot gain authority from copied fields");attach(actual,entity);check(NativeSourceGroundQueued.sameEntity(entity),"actual lease stores this exact Entity");
  check(!NativeSourceGroundQueued.sameEntity(new NativeSourceGroundOrigin.Entity(actual,engine,entity.epoch(),scene,creator)),"copied Entity cannot borrow original lease");
  var other=new BattleGroundEffects();other.nativeSourceGroundScene=scene;check(!NativeSourceGroundQueued.sameEntity(new NativeSourceGroundOrigin.Entity(actual,other,other.nativeSourceGroundEpoch,scene,creator)),"different engine cannot borrow original field");
  engine.nativeSourceGroundScene=new NativeSourceGroundOrigin.Scene(null,scene.room(),scene.id());check(!NativeSourceGroundQueued.sameEntity(entity),"same-id scene replacement is not the old scene");engine.nativeSourceGroundScene=scene;check(NativeSourceGroundQueued.sameEntity(entity),"original scene still identifies original proof");
  var another=lease(engine);attach(another,entity);check(!NativeSourceGroundQueued.sameEntity(new NativeSourceGroundOrigin.Entity(another,engine,entity.epoch(),scene,creator)),"same header on another lease cannot authorize its Entity");
  var list=(List<Object>)CombatAccess.field(engine,"leases");list.add(actual);list.remove(actual);check(NativeSourceGroundQueued.sameEntity(entity),"individual lease expiry/removal does not withdraw an admitted HP proof");
  engine.clear();check(!NativeSourceGroundQueued.sameEntity(entity),"scene clear retires admitted old-scene proof");
  check(!NativeSourceGroundQueued.sameEntity(new NativeSourceGroundOrigin.Entity(new Object(),engine,engine.nativeSourceGroundEpoch,scene,creator)),"arbitrary actual object is not a native lease");
  var f=NativeSourceDamageDisplay.Plan.class.getDeclaredField("nativeSourceGroundQueuedProof");check(f.getType()==Object.class&&Modifier.isVolatile(f.getModifiers()),"original plan owns one visible detached metadata field");
  check(NativeSourceGroundQueued.class.getDeclaredFields().length==0,"helper does not retain producing scopes");
  var life=new Object();var actor=new NativeSourceGroundOrigin.Actor(null,null,life);check(NativeSourceGroundQueued.matches(actor,null,null,life),"same immutable actor tuple (not role authorization)");check(!NativeSourceGroundQueued.matches(actor,null,null,new Object()),"different life token cannot borrow original tuple");check(!NativeSourceGroundQueued.matches(null,null,null,life),"missing actor tuple rejects");check(!NativeSourceGroundQueued.matches(new NativeSourceGroundOrigin.Actor(null,null,null),null,null,null),"absent life is not an identity");check(NativeSourceGroundQueued.matches(new NativeSourceGroundOrigin.Actor(actor.pc(),actor.binding(),actor.life()),null,null,life),"tuple comparison preserves original life identity");
  if(cases!=18)throw new AssertionError(cases);System.out.println("PASS18 actual unbound lease identity/scene retirement/individual-expiry and actor tuple contracts; no bound HP/accepted fatal/role/pair/receiver/Main/game/MySQL qualification");
 }
}
