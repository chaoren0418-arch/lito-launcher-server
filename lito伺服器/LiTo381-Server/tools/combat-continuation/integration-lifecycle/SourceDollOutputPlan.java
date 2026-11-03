package local.lito381.server;
/** Actual detached timer-proof and wire-duration scenarios; no player, Hub, socket, SQL or worker fixture. */
public final class SourceDollOutputPlan {
 static final class Timers {volatile long hasteExpiresAtNanos,slowExpiresAtNanos;volatile Object nativeSourceDollSlowEpoch;}
 static int cases;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);cases++;}
 public static void main(String[]args){
  check(NativeSourceDollOutput.speedMode(0,0,100)==0,"normal state");
  check(NativeSourceDollOutput.speedMode(200,0,100)==1,"active haste");
  check(NativeSourceDollOutput.speedMode(0,200,100)==2,"active slow");
  check(NativeSourceDollOutput.speedMode(200,200,100)==2,"slow precedence matches existing timer state");
  check(NativeSourceDollOutput.speedMode(0,100,100)==0,"exact slow expiry is normal");
  check(NativeSourceDollOutput.remaining(100,100)==0,"expired self packet suppressed");
  check(NativeSourceDollOutput.remaining(101,100)==1,"positive subsecond lease rounds up");
  check(NativeSourceDollOutput.remaining(1000000100L,100)==1,"exact second");
  check(NativeSourceDollOutput.remaining(1000000101L,100)==2,"partial second rounds up");
  check(NativeSourceDollOutput.remaining(70000000000000L,0)==65535,"wire duration saturation");
  var state=new Timers();state.slowExpiresAtNanos=System.nanoTime()+10000000000L;var proof=new NativeSourceDollOutput.Speed(new Object(),state,0,state.slowExpiresAtNanos,2);
  check(proof.current(),"actual captured timer proof current");state.slowExpiresAtNanos+=1000000000L;
  check(!proof.current(),"replaced lease rejected even same state object");state.slowExpiresAtNanos=0;
  check(!proof.current(),"cleared lease cannot replay old slow");var normal=new NativeSourceDollOutput.Speed(new Object(),state,0,0,0);
  check(normal.current(),"accepted haste cancellation remains normal");state.hasteExpiresAtNanos=System.nanoTime()+10000000000L;
  check(!normal.current(),"new haste cannot be overwritten by old normal packet");
  if(cases!=15)throw new AssertionError(cases);System.out.println("PASS15 actual detached speed-proof/remaining-wire scenarios, no player/Hub/socket/SQL/worker fixture");
 }
}
