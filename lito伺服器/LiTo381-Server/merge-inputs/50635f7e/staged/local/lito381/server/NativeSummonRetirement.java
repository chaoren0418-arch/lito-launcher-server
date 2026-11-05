package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.*;
/** Detached Source deleteMe work owns the canonical bag, never a retired player. */
final class NativeSummonRetirement {
 static final String FALLBACK="nativeSummonGroundFallback",WORK="nativeSummonRetirements";
 @SuppressWarnings("unchecked") static Set<Work> works(NativePlayerLifetime life){return (Set<Work>)CombatAccess.field(life,WORK);}
 static long pending(NativePlayerLifetime life){synchronized(life.admission){return NativePlayerVitalCompletions.pending(life)+works(life).size();}}
 static NativeSummonGroundTransfer.Stage fallback(BattleCompanion pet){return (NativeSummonGroundTransfer.Stage)CombatAccess.field(pet,FALLBACK);}
 static void remember(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){if(fallback(pet)==null){var stage=NativeSummonGroundTransfer.captureFallback(owner,pet,stamp,false);if(stage!=null)CombatAccess.set(pet,FALLBACK,stage);}}
 static boolean ownerDeletion(ClientSession owner,BattleCompanion pet){
  Object target=NativeReturnNatureMaterialAction.ownerSlot().get();if(target==null||!(boolean)CombatAccess.field(target,"nativeSummonDeletionContext")||CombatAccess.field(target,"owner")!=owner||CombatAccess.field(target,"caster")!=owner||CombatAccess.field(target,"pet")!=pet||NativeReturnNatureMaterialAction.caster(owner,pet)!=owner)return false;
  var h=(GameHub)CombatAccess.field(owner,"hub");return pet.snapshot().kind()==BattleCompanion.Kind.SUMMON&&!pet.snapshot().active()&&NativeLightAction.retainedMaterial(owner)&&owner.nativePlayerActionLock().isHeldByCurrentThread()&&!(boolean)CombatAccess.field(h,"nativeStaticMotionClosed")&&NativeSummonTerminalLifecycle.registered(owner,h);
 }
 static void beforeDisconnect(GameHub h,ClientSession owner){
  if(owner==null)return;var binding=NativePlayerCommit.Binding.capture(owner);var handoffs=new ArrayList<Work>();boolean[] entered={false};
  try{NativeLightAction.commitPair(owner,owner,()->{entered[0]=true;prepare(h,owner,binding,true,handoffs);});}
  catch(IOException error){NativeReuseLog.getLogger(NativeSummonRetirement.class).error("Summon logout return failed; canonical bags retained for worker deletion",error);}
  finally{
   // A system disconnect remains responsible even when ordinary PC admission stopped.
   if(!entered[0])NativeSourcePlayerActionLock.run(owner,()->prepare(h,owner,binding,false,handoffs));
   for(var work:handoffs)work.request(!owner.nativePlayerActionLock().isHeldByCurrentThread()&&!Thread.holdsLock(h));
  }
 }
 static void prepare(GameHub h,ClientSession owner,NativePlayerCommit.Binding binding,boolean ownerReturn,List<Work> handoffs){
  if(!owner.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("retirement requires Source owner lock");
  synchronized(h){if(!binding.matches(owner)||(boolean)CombatAccess.field(h,"nativeStaticMotionClosed")||!NativeSummonTerminalLifecycle.registered(owner,h))return;
   var pets=new ArrayList<>(NativeBattleTamedLifecycle.pets(owner));var fixed=(BattleCompanion)CombatAccess.field(owner,"battleCompanion");if(fixed!=null&&!pets.contains(fixed))pets.add(fixed);
   for(var pet:pets)synchronized(pet){if(pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON)continue;
    var stage=fallback(pet);if(stage!=null&&!stage.actor()){CombatAccess.set(pet,FALLBACK,null);stage=null;}
    var original=NativeSummonTerminalLifecycle.pending(pet);if(pet.snapshot().active())pet.deactivate();
    var stamp=NativeCompanionCallbacks.capture(binding,pet);boolean returned=false;
    // An old death claim must never credit a different role/scene after retirement.
    if(ownerReturn&&NativeCompanionPlacement.ownerReturn(owner,pet)&&(original==null||original.master.matches(owner))){var slot=NativeReturnNatureMaterialAction.ownerSlot();Object prior=slot.get();slot.set(NativeReturnNatureMaterialAction.ownerDeletionContext(owner,pet,stamp));
     try{returned=NativeReturnNatureTransfer.release(owner,pet,pet.snapshot(),169);}catch(IOException error){NativeReuseLog.getLogger(NativeSummonRetirement.class).error("Summon logout inventory return failed; original bag retained",error);}finally{if(prior==null)slot.remove();else slot.set(prior);}
    }
    if(returned){CombatAccess.set(pet,FALLBACK,null);CombatAccess.set(pet,NativeSummonTerminalLifecycle.PENDING,null);continue;}
    if(stage==null&&(original==null||original.master.matches(owner)))stage=NativeSummonGroundTransfer.captureFallback(owner,pet,stamp,false);
    if(stage==null)continue;CombatAccess.set(pet,FALLBACK,stage);var work=register(NativePlayerLifetime.of(h),stage);handoffs.add(work);
    // The lifetime-owned Work takes responsibility before its master's container retires.
    var list=NativeBattleTamedLifecycle.pets(owner);synchronized(list){list.removeIf(p->p==pet);}
   }
  }
 }
 static Work register(NativePlayerLifetime life,NativeSummonGroundTransfer.Stage stage){synchronized(life.admission){for(var w:works(life))if(w.stage!=null&&w.stage.pet==stage.pet&&w.stage.stamp.life==stage.stamp.life)return w;var w=new Work(life,stage);works(life).add(w);life.pending.incrementAndGet();return w;}}
 static void tick(GameHub h){Object current=CombatAccess.field(h,"sourcePlayerCommitLifetime");if(current instanceof NativePlayerLifetime life)drain(life);}
 static void drain(NativePlayerLifetime life){List<Work> copy;synchronized(life.admission){copy=new ArrayList<>(works(life));}for(var w:copy)w.request(false);}
 static void closing(NativePlayerLifetime life){List<Work> copy;synchronized(life.admission){copy=new ArrayList<>(works(life));}for(var w:copy){var s=w.stage;if(s!=null&&!s.living())w.finish();else w.request(false);}}
 static final class Work implements Runnable {
  final NativePlayerLifetime life;volatile NativeSummonGroundTransfer.Stage stage;boolean queued,running,done;long next;ScheduledFuture<?> timer;
  Work(NativePlayerLifetime life,NativeSummonGroundTransfer.Stage stage){this.life=life;this.stage=stage;}
  void request(boolean inlineIfStopped){
   boolean inline=false;synchronized(life.admission){if(done||running||queued)return;if(System.nanoTime()<next){arm();return;}queued=true;
    try{life.workers.execute(this);return;}catch(RejectedExecutionException rejected){queued=false;life.retries.incrementAndGet();}
    if(inlineIfStopped&&life.workers.isShutdown()&&life.retry.isShutdown()){queued=true;inline=true;}else arm();
   }if(inline)run();
  }
  void arm(){if(done||timer!=null)return;long remaining=next-System.nanoTime(),delay=remaining>0?remaining:TimeUnit.SECONDS.toNanos(1);try{timer=life.retry.schedule(()->{synchronized(life.admission){timer=null;}request(false);},delay,TimeUnit.NANOSECONDS);}catch(RejectedExecutionException rejected){life.timerRejections.incrementAndGet();}}
  public void run(){synchronized(life.admission){queued=false;if(done||running)return;running=true;}try{life.executeCommitted(this::perform);}finally{synchronized(life.admission){running=false;if(!done){next=System.nanoTime()+TimeUnit.SECONDS.toNanos(1);arm();}}}}
  void perform(){var s=stage;if(s==null)return;if(!s.living()){finish();return;}if(s.transfer()){s.finishDeletion();finish();}}
  void finish(){NativeSummonGroundTransfer.Stage s;synchronized(life.admission){if(done)return;done=true;s=stage;stage=null;if(timer!=null){timer.cancel(false);timer=null;}works(life).remove(this);life.pending.decrementAndGet();}
   if(s!=null)synchronized(s.pet){if(fallback(s.pet)==s)CombatAccess.set(s.pet,FALLBACK,null);}
   NativeReuseShutdown.receiptReleased(life);
  }
 }
 private NativeSummonRetirement(){}
}
