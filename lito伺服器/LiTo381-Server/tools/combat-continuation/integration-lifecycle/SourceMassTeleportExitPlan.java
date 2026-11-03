package local.lito381.server;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;

/** First source lock and the complete original exit lambda/final cipher on strict memory roles. */
public final class SourceMassTeleportExitPlan {
 static int checks,sourceWaits,finalWaits,nested;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
 static NativeSourceMassTeleportExit.Proof proof(SourceWeaponFullPathPlan.Fixture f)throws Throwable {
  SourceSelfFinalFramePlan.first(f);var pc=f.base().first().base().base().caster();
  var proof=NativeSourceMassTeleportExit.capture(pc,NativePlayerCommit.Binding.capture(pc));
  check(NativeSourceMassTeleportExit.current(proof,pc),"readonly first mass-teleport exit proof from existing actor state");return proof;
 }
 static Runnable original(ClientSession pc,NativeSourceMassTeleportExit.Proof proof)throws Exception {
  var method=NativeAreaMaterialAction.class.getDeclaredMethod("nativeSourceMassTeleportExitOriginalUnlock",NativePlayerCommit.Binding.class,ClientSession.class,NativeSourceMassTeleportExit.Proof.class);method.setAccessible(true);
  return ()->{try{method.invoke(null,proof.binding(),pc,proof);}catch(java.lang.reflect.InvocationTargetException failure){var cause=failure.getCause();if(cause instanceof RuntimeException runtime)throw runtime;if(cause instanceof Error error)throw error;throw new AssertionError(cause);}catch(IllegalAccessException failure){throw new AssertionError(failure);}};
 }
 static void mutate(SourceWeaponFullPathPlan.Fixture f,String mode)throws Exception {
  var b=f.base().first().base().base();var pc=b.caster();Object timed=CombatAccess.field(pc,"timedStatus"),state=CombatAccess.field(timed,"sourcePoisonLifecycle");
  switch(mode) {
   case "normal"->{}case "DRAINING"->SourceWeaponFullPathPlan.put(b.life(),"mode",NativePlayerLifetime.Mode.DRAINING);
   case "Main"->SourceWeaponFullPathPlan.put(b.hub(),"nativeStaticMotionClosed",true);
   case "STOPPED"->SourceWeaponFullPathPlan.put(b.life(),"mode",NativePlayerLifetime.Mode.STOPPED);
   case "role"->SourceWeaponFullPathPlan.put(pc,"selectedCharacter","replacement-role");
   case "status"->SourceWeaponFullPathPlan.put(pc,"timedStatus",null);
   case "status-replaced"->{var replacement=new BattleTimedStatus();CombatAccess.set(replacement,"sourcePoisonLifecycle",state);SourceWeaponFullPathPlan.put(pc,"timedStatus",replacement);}
   case "life"->CombatAccess.set(state,"sourceNpcOwner",new Object());
   case "life-missing"->CombatAccess.set(state,"sourceNpcOwner",null);
   case "lifetime"->SourceWeaponFullPathPlan.put(b.hub(),"sourcePlayerCommitLifetime",null);
   case "observer"->SourceWeaponFullPathPlan.put(pc,"observer",true);
   case "application"->SourceWeaponFullPathPlan.put(b.hub(),"sourceItemApplication",SourceSignedRecoveryPlan.unsafe().allocateInstance(NativeDeItemApplication.class));
   case "server-application"->SourceWeaponFullPathPlan.put(b.life(),"application",SourceSignedRecoveryPlan.unsafe().allocateInstance(NativeServerLifetime.class));
   default->throw new AssertionError(mode);
  }
 }
 static void result(SourceWeaponFullPathPlan.Fixture f,String mode,long cipher,Object state) {
  var b=f.base().first().base().base();var pc=b.caster();boolean allowed=Set.of("normal","DRAINING").contains(mode);
  check(allowed?f.sockets().get(pc).bytes.size()>0:f.sockets().get(pc).bytes.size()==0,"original unlock frame obeys first source proof "+mode);
  check(allowed||((Protocol.CipherState)CombatAccess.field(pc,"outgoing")).value()==cipher,"rejection precedes cipher advance "+mode);
  if(mode.equals("status"))check(CombatAccess.field(pc,"timedStatus")==null,"missing source status never recreated");
  if(mode.equals("life-missing"))check(CombatAccess.field(state,"sourceNpcOwner")==null,"missing source actor token never recreated");
  if(mode.equals("lifetime"))check(CombatAccess.field(b.hub(),"sourcePlayerCommitLifetime")==null,"missing source lifetime never replaced");
 }
 static void waitAt(String mode,boolean cipherBoundary)throws Throwable {
  var f=SourceSelfFinalFramePlan.fixture();var b=f.base().first().base().base();var pc=b.caster();var proof=proof(f);var callback=original(pc,proof);var failure=new AtomicReference<Throwable>();
  Object state=CombatAccess.field(CombatAccess.field(pc,"timedStatus"),"sourcePoisonLifecycle");long cipher=((Protocol.CipherState)CombatAccess.field(pc,"outgoing")).value();
  var oldLock=pc.nativePlayerActionLock();java.lang.reflect.Field field=null;var gate=new SourceMaterialMainPlan.Gate();Thread worker=null;
  try {
   if(cipherBoundary) {
    var publication=NativeSourceMassTeleportExit.completion(pc,callback,proof);
    worker=new Thread(()->{try{publication.run();}catch(Throwable e){failure.set(e);}},"mass-exit-original-final-cipher");
    synchronized(pc){worker.start();SourceElementAudiencePlan.blocked(worker,"mass-teleport original final receiver monitor");check(!pc.nativePlayerActionLock().isLocked(),"mass-teleport final output occurs after source unlock");mutate(f,mode);}
   } else {
    field=Arrays.stream(ClientSession.class.getDeclaredFields()).filter(x->{try{x.setAccessible(true);return x.get(pc)==oldLock;}catch(Exception e){throw new AssertionError(e);}}).findFirst().orElseThrow();field.set(pc,gate);
    worker=new Thread(()->{try{NativeSourceMassTeleportExit.completion(pc,callback,proof).run();}catch(Throwable e){failure.set(e);}},"mass-exit-original-source-lock");
    gate.lock();try{worker.start();check(gate.tried.await(3,TimeUnit.SECONDS),"mass-teleport exit reaches original action mutex");mutate(f,mode);}finally{gate.unlock();}
   }
   worker.join(3000);if(failure.get()!=null)failure.get().printStackTrace();check(!worker.isAlive()&&failure.get()==null,"original mass-teleport exit wait terminates "+mode+" final="+cipherBoundary);result(f,mode,cipher,state);
   if(cipherBoundary)finalWaits++;else sourceWaits++;
  } finally {if(gate.isHeldByCurrentThread())gate.unlock();if(worker!=null){worker.join(3000);check(!worker.isAlive(),"exit worker terminal");}if(field!=null)field.set(pc,oldLock);SourceWeaponFullPathPlan.cleanup(f);}
 }
 static void nested(boolean busy,boolean preheld,boolean retire)throws Throwable {
  var f=SourceSelfFinalFramePlan.fixture();var b=f.base().first().base().base();var pc=b.caster();var third=b.target();var proof=proof(f);var callback=original(pc,proof);var failure=new AtomicReference<Throwable>();var done=new CountDownLatch(1);var root=new AtomicReference<Object>();
  var worker=new Thread(()->{try{NativeSourcePlayerActionLock.run(third,()->{root.set(NativePlayerActionScope.nativeSourcePairScopeRoot());if(preheld)pc.nativePlayerActionLock().lock();try{NativeSourceMassTeleportExit.completion(pc,callback,proof).run();check(f.sockets().get(pc).bytes.size()==0,"outer third actor retains original completion gate");if(retire)SourceWeaponFullPathPlan.put(b.hub(),"nativeStaticMotionClosed",true);}finally{if(preheld){pc.nativePlayerActionLock().unlock();NativePlayerActionScope.unlocked(pc);}}});}catch(Throwable e){failure.set(e);}finally{done.countDown();}},"mass-exit-nested-third-actor");
  try {
   if(busy)pc.nativePlayerActionLock().lock();try{worker.start();check(done.await(3,TimeUnit.SECONDS),"nested mass-teleport exit never waits while holding a third actor");}finally{if(busy)pc.nativePlayerActionLock().unlock();}
   worker.join(3000);if(failure.get()!=null)failure.get().printStackTrace();check(!worker.isAlive()&&failure.get()==null,"nested mass-teleport original transport terminates");
   check(busy||retire?f.sockets().get(pc).bytes.size()==0:f.sockets().get(pc).bytes.size()>0,"free nested output drains once after all actors unlock; stale/busy output rejects");
   check(root.get()!=null&&(boolean)CombatAccess.field(root.get(),"closed")&&((Map<?,?>)CombatAccess.field(root.get(),"members")).isEmpty(),"existing root releases all actor members");nested++;
  } finally {if(pc.nativePlayerActionLock().isHeldByCurrentThread())pc.nativePlayerActionLock().unlock();worker.join(3000);SourceWeaponFullPathPlan.cleanup(f);}
 }
 public static void main(String[]args)throws Throwable {
  SourceSelfFinalFramePlan.environment(()->{for(boolean boundary:List.of(false,true))for(String mode:List.of("normal","DRAINING","Main","STOPPED","role","status","status-replaced","life","life-missing","lifetime","observer","application","server-application"))waitAt(mode,boundary);nested(true,false,false);nested(false,false,false);nested(false,false,true);nested(false,true,false);nested(false,true,true);check(NativeSourceMassTeleportExit.class.getDeclaredFields().length==0,"no new actor registry/TLS/queue/worker");});
  System.out.println("PASS"+checks+" mass-teleport exit original-lock/original-unlock-lambda/final-cipher/third-actor contracts; source-waits="+sourceWaits+", final-monitor-waits="+finalWaits+", nested="+nested+"; full destination/invite/Source/Ctrl/backend/game remain open");
 }
}
