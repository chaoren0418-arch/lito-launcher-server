package local.lito381.server;
import java.util.*;
/** Actual unbound actor status writers and detached acceptance/ABA contracts; no bound HP/Hub/PC/socket/SQL. */
public final class SourceTargetNpcPlan {
 static int cases;static void check(boolean b,String why){if(!b)throw new AssertionError(why);cases++;}
 public static void main(String[]a)throws Exception{
  var mutation=new NativeSourceTargetNpc.Mutation(null,3,null,false);check(!NativeSourceTargetNpc.claim(mutation,false)&&!mutation.claimed().get(),"rejected HP cannot claim fixed target mutation");check(NativeSourceTargetNpc.claim(mutation,true)&&!NativeSourceTargetNpc.claim(mutation,true),"one accepted completion only");check(!NativeSourceTargetNpc.claim(null,true),"missing plan cannot invent acceptance");
  var zero=new NativeSourceTargetNpc.Mutation(null,-2,null,false);check(zero.freezeSeconds()==0&&zero.shock()==null&&!zero.shockRolled(),"invalid duration and absent accepted shock cannot create status");
  check(!NativeSourceTargetNpc.freezeAllowed(true,true,100,1)&&!NativeSourceTargetNpc.freezeAllowed(false,false,100,1),"boss gate and non-freeze line retained");check(NativeSourceTargetNpc.freezeAllowed(true,false,50,50)&&!NativeSourceTargetNpc.freezeAllowed(true,false,49,50),"already rolled threshold retained without HP estimate");
  Object lease=new Object();check(!NativeSourceTargetNpc.leaseCurrent(null,null,10,10,1)&&!NativeSourceTargetNpc.leaseCurrent(lease,new Object(),10,10,1),"missing/replaced status grants no output");check(NativeSourceTargetNpc.leaseCurrent(lease,lease,10,10,9)&&!NativeSourceTargetNpc.leaseCurrent(lease,lease,10,11,9)&&!NativeSourceTargetNpc.leaseCurrent(lease,lease,10,10,10),"exact status expiry and boundary required");
  var access=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");access.setAccessible(true);var unsafe=(sun.misc.Unsafe)access.get(null);long now=1_000_000;
  for(String name:List.of("BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster")){
   // Only the actual actor's scalar status methods run; all owners, queues, timers and persistence remain unbound.
   Object actor=unsafe.allocateInstance(Class.forName("local.lito381.server."+name));CombatAccess.set(actor,"hp",1);
   boolean activated=(boolean)CombatAccess.call(actor,"activateFreeze","(JI)Z",now,2);Object first=CombatAccess.field(actor,"nativeSourceTargetFreezeLease");long expiry=(long)CombatAccess.field(actor,"freezeExpiresAtNanos");check(activated&&first!=null&&expiry==now+2_000_000_000L,"original freeze writer publishes its exact scalar expiry and lease");
   CombatAccess.call(actor,"cancelFreeze","()Z");CombatAccess.call(actor,"activateFreeze","(JI)Z",now,2);Object second=CombatAccess.field(actor,"nativeSourceTargetFreezeLease");check(second!=first&&(long)CombatAccess.field(actor,"freezeExpiresAtNanos")==expiry&&!NativeSourceTargetNpc.leaseCurrent(first,second,expiry,expiry,now),"cancel/recast at identical clock/expiry rejects old effect ABA");
   boolean expired=(boolean)CombatAccess.call(actor,"expireFreeze","(J)Z",expiry);Object cleared=CombatAccess.field(actor,"nativeSourceTargetFreezeLease");check(expired&&cleared!=second&&(long)CombatAccess.field(actor,"freezeExpiresAtNanos")==0,"original expiry retires queued status output");
   boolean stunned=(boolean)CombatAccess.call(actor,"activateShockStun","(JI)Z",now,2);Object shock=CombatAccess.field(actor,"nativeSourceTargetShockLease");check(stunned&&shock!=null&&(long)CombatAccess.field(actor,"shockStunExpiresAtNanos")==expiry,"original shock writer uses captured duration");
   CombatAccess.call(actor,"activateShockStun","(JI)Z",now,2);check(CombatAccess.field(actor,"nativeSourceTargetShockLease")!=shock&&CombatAccess.field(actor,"nativeSourceTargetFreezeLease")==cleared,"same-expiry shock refresh retires old output without modifying freeze lease");
  }
  if(cases!=18)throw new AssertionError(cases);System.out.println("PASS18 accepted fixed-input/threshold and actual unbound Quest-Time status writer cancellation-expiry-recast ABA contracts; no bound HP/Main/game/SQL qualification");
 }
}
