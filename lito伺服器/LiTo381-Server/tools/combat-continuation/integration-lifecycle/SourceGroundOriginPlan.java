package local.lito381.server;
import java.lang.reflect.*;import java.util.*;
/** Real original lease/pulse/tick metadata and epoch contracts only; no PC/Hub/bound game fixture. */
public final class SourceGroundOriginPlan {
 static int cases;static void check(boolean v,String why){if(!v)throw new AssertionError(why);cases++;}
 static Object lease(BattleGroundEffects engine)throws Exception{var type=Class.forName("local.lito381.server.BattleGroundEffects$Lease",false,SourceGroundOriginPlan.class.getClassLoader());var c=type.getDeclaredConstructor(int.class,long.class,int.class,int.class,int.class,long.class,long.class,int.class,BattleGroundEffects.class);c.setAccessible(true);return c.newInstance(58,7L,1,1,1,10000000000L,1000000000L,1650,engine);}
 public static void main(String[]args)throws Exception{
  var engine=new BattleGroundEffects();var other=new BattleGroundEffects();check(engine.nativeSourceGroundEpoch!=null&&engine.nativeSourceGroundEpoch!=other.nativeSourceGroundEpoch,"per-engine epoch identity");
  var epoch=engine.nativeSourceGroundEpoch;engine.clear();check(epoch!=engine.nativeSourceGroundEpoch,"clear retires old scene epoch");
  var actual=lease(engine);check(CombatAccess.field(actual,"nativeSourceGroundEntity")==null,"unbound original factory has no Source authority");
  var scene=new NativeSourceGroundOrigin.Scene(null,new Object(),5);engine.nativeSourceGroundScene=scene;var e=new NativeSourceGroundOrigin.Entity(actual,engine,engine.nativeSourceGroundEpoch,scene,null);
  check(NativeSourceGroundOrigin.same(e,engine,actual),"exact original entity/lease/scene/epoch");check(!NativeSourceGroundOrigin.same(e,other,actual),"other engine cannot borrow lease");check(!NativeSourceGroundOrigin.same(e,engine,new Object()),"copied lease header cannot borrow entity");
  engine.nativeSourceGroundScene=new NativeSourceGroundOrigin.Scene(null,scene.room(),5);check(!NativeSourceGroundOrigin.same(e,engine,actual),"equal room header cannot replace scene object");engine.nativeSourceGroundScene=scene;
  engine.clear();check(!NativeSourceGroundOrigin.same(e,engine,actual),"clear retires same-id original lease");
  var pulse=new BattleGroundEffects.Pulse(58,7,8,false);check(pulse.nativeSourceGroundProof==null,"old pulse constructor is unbound");check(NativeSourceGroundOrigin.tick(pulse)==null,"copied pulse cannot invent source proof");
  check(BattleGroundEffects.Pulse.class.getRecordComponents().length==4,"original pulse record schema retained");
  var c=BattleGroundEffects.Pulse.class.getDeclaredConstructor(int.class,long.class,long.class,boolean.class,BattleGroundEffects.class,Object.class);c.setAccessible(true);var unbound=(BattleGroundEffects.Pulse)c.newInstance(58,7L,8L,false,engine,actual);check(unbound.nativeSourceGroundProof==null,"delegating pulse constructor rejects absent lease provenance");
  var tick=Class.forName("local.lito381.server.GameHub$GroundTick",false,SourceGroundOriginPlan.class.getClassLoader());var ctor=tick.getDeclaredConstructor(long.class,ClientSession.class,ClientSession.class,boolean.class,int.class,BattleGroundEffects.Pulse.class);ctor.setAccessible(true);var value=ctor.newInstance(5L,null,null,false,58,pulse);check(CombatAccess.field(value,"nativeSourceGroundProof")==null,"unbound original tick cannot fabricate pulse proof");
  check(!NativeSourceGroundOrigin.current(null),"missing proof rejects");check(!NativeSourceGroundOrigin.current(new NativeSourceGroundOrigin.PulseProof(e,null,null)),"unbound metadata does not authorize actors");
  var again=engine.nativeSourceGroundEpoch;engine.clear();engine.clear();check(again!=engine.nativeSourceGroundEpoch,"repeated clear cannot revive old epoch");
  if(cases!=16)throw new AssertionError(cases);System.out.println("PASS16 actual unbound ground engine/lease/pulse/tick epoch and schema contracts; no bound HP/role/pair/receiver/Main/game/MySQL qualification");
 }
}
