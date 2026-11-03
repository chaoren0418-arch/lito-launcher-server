package local.lito381.server;
/** Actual original erase status and detached identity contracts, without Hub/PC/socket/SQL initialization. */
public final class SourceMagicNpcPlan {
 static int cases;static void check(boolean b,String why){if(!b)throw new AssertionError(why);cases++;}
 public static void main(String[]a)throws Exception{
  var actor=new Object();var owner=new Object();var life=new Object();var generation=new Object();var n=new NativeAreaMaterialAction.Npc(1,41,owner,actor,life);var frozen=new NativeSourceMagicNpc.Npc(n,generation,10,20);
  check(NativeSourceMagicNpc.matches(n,frozen),"original detached NPC identity matches");check(!NativeSourceMagicNpc.matches(null,frozen)&&!NativeSourceMagicNpc.matches(n,null),"missing provenance rejects");
  check(!NativeSourceMagicNpc.matches(new NativeAreaMaterialAction.Npc(1,41,owner,new Object(),life),frozen),"same numerical id cannot select a replacement actor");check(!NativeSourceMagicNpc.matches(new NativeAreaMaterialAction.Npc(1,41,new Object(),actor,life),frozen),"another encounter owner cannot inherit");check(!NativeSourceMagicNpc.matches(new NativeAreaMaterialAction.Npc(1,41,owner,actor,new Object()),frozen),"new life cannot inherit");check(!NativeSourceMagicNpc.matches(new NativeAreaMaterialAction.Npc(2,41,owner,actor,life),frozen),"other natural kind cannot inherit");check(!NativeSourceMagicNpc.matches(new NativeAreaMaterialAction.Npc(1,42,owner,actor,life),frozen),"different target id cannot inherit");
  var status=new BattleEraseMagicStatus();long now=System.nanoTime();var lease=status.activate(now,100);check(lease!=null&&status.capture(now)==lease||lease!=null&&status.capture(now).equals(lease),"actual original erase activation/capture");
  check(status.consume(lease)&&!status.active(now),"accepted exact lease consumes original status");check(!status.consume(lease),"old accepted lease is once-only");
  var old=status.activate(now,100);var fresh=status.activate(now,100);check(!status.consume(old)&&status.active(now),"same-expiry recast rejects old queued accepted lease");check(status.consume(fresh)&&!status.active(now),"new exact lease remains consumable");
  var cancelled=status.activate(now,100);status.cancel();status.activate(now,100);check(!status.consume(cancelled)&&status.active(now),"cancel/reapply ABA rejects old queued lease");
  var cleared=status.capture(now);status.clear();status.activate(now,100);check(!status.consume(cleared)&&status.active(now),"clear/reapply preserves new status");
  var expired=status.capture(now);status.expire(now+200000000000L);check(!status.consume(expired),"expired old lease cannot consume an absent status");
  check(new NativeSourceMagicNpc.After(null).erase()==null,"absent erase metadata grants no accepted mutation");
  if(cases!=16)throw new AssertionError(cases);System.out.println("PASS16 detached original NPC identity and actual erase-status accepted-once/recast/cancel/clear/expiry contracts; no bound HP/Main/game/SQL qualification");
 }
}
