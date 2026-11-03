package local.lito381.server;
/** Actual unbound original queue/drainer tests, without Hub/PC/life/socket/SQL initialization. */
public final class SourcePoisonNpcPlan {
 static int cases;static void check(boolean b,String why){if(!b)throw new AssertionError(why);cases++;}
 static CombatNpcDamageQueue.Event e(long id,CombatWorldCommit.Plan plan){return new CombatNpcDamageQueue.Event(0,0,null,id,0,10,0,"source-npc-poison","","",plan);}
 public static void main(String[]a)throws Exception{
  check(!NativeSourcePoisonNpc.ticket(null),"missing event is not poison");var marker=new NativeSourcePoisonNpc.Ticket(null,null);var poison=e(1,marker);var cube=e(2,new NativeSourceCubeNpcQueue.Ticket(null,null));var plain=e(3,null);
  check(NativeSourcePoisonNpc.ticket(poison)&&!NativeSourcePoisonNpc.ticket(cube)&&!NativeSourcePoisonNpc.ticket(plain),"poison/cube/old metadata are distinct typed plans");
  check(!NativeSourcePoisonNpc.envelope(poison,null),"header cannot invent installed provenance");check(!NativeSourcePoisonNpc.consume(null,null,cube)&&!NativeSourcePoisonNpc.consume(null,null,plain),"foreign plans retain their original route");check(NativeSourcePoisonNpc.consume(null,null,poison),"unbound poison safely drops");check(!NativeSourcePoisonNpc.before(null,0,null,0,0),"actual HP requires queued claimed original proof");
  long pending=CombatNpcDamageQueue.pending();var q=new CombatNpcDamageQueue.Queue();check(q.offer(poison)&&q.offer(cube)&&q.offer(plain),"mixed original FIFO admission");check(q.start()&&!q.start(),"same once-only drainer");check(q.claim()==poison,"poison retains actual FIFO identity");q.complete(poison);check(q.claim()==cube,"cube remains next original event");q.complete(cube);check(q.claim()==plain,"old event remains at original tail");q.complete(plain);check(q.claim()==null&&q.pending()==0,"actual original drain settles");
  var proc=SourceCubeNpcQueuePlan.originalProc();var mixed=new CombatNpcDamageQueue.Queue();mixed.offer(poison);mixed.offer(cube);mixed.offer(e(4,proc));mixed.start();NativeProcHealingEncounter.nativeSourceCubeDrain(null,mixed).run();check(mixed.pending()==0&&CombatNpcDamageQueue.pending()==pending,"actual owned drainer routes poison/cube/old proc and completes exactly once");
  var dropped=new CombatNpcDamageQueue.Queue();dropped.offer(poison);dropped.offer(cube);dropped.clear(true);check(dropped.closed()&&dropped.pending()==0&&!dropped.offer(poison)&&CombatNpcDamageQueue.pending()==pending,"original terminal clear rejects later poison and releases every slot");
  boolean monitor=false;try{NativeSourcePoisonNpc.clearing(new CombatNpcDamageQueue.Queue());}catch(IllegalStateException expected){monitor=true;}check(monitor,"drop observer requires original queue monitor");
  if(cases!=15)throw new AssertionError(cases);System.out.println("PASS15 actual unbound mixed poison/cube/old-proc FIFO/drainer/drop contracts; no HP/Main/game/SQL qualification");
 }
}
