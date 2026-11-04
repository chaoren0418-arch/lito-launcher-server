package local.lito381.server;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Independent source curse channel in the existing transient status container. */
final class CombatCurseLifecycle {
 private static final int INITIAL=1,WAITING=2,APPLYING=3,ACTIVE=4;
 @AsRecord(components={"id","source","account","name","phase","room","map","instance"})
 private static final class Target {
  private final long id;private final String source;private final String account;private final String name;
  private final Object phase;private final long room;private final long map;private final long instance;
  private volatile Object nativeRoleOwner;
  private Target(long id,String source,String account,String name,Object phase,long room,long map,long instance){
   this.id=id;this.source=source;this.account=account;this.name=name;this.phase=phase;this.room=room;this.map=map;this.instance=instance;
  }
  public long id(){return id;}public String source(){return source;}public String account(){return account;}public String name(){return name;}
  public Object phase(){return phase;}public long room(){return room;}public long map(){return map;}public long instance(){return instance;}
  static Target take(ClientSession pc){
   Target t=new Target(pc.playerObjectId(),pc.sourceCharacterIdentity(),pc.sourceAccountIdentity(),pc.selectedCharacterName(),CombatAccess.field(pc,"phase"),number(pc,"currentRoomId"),number(pc,"classicWorldMapId"),number(pc,"classicWorldInstanceId"));
   t.nativeRoleOwner=NativePlayerCommit.owner(pc);
   return t;
  }
  boolean valid(ClientSession pc){
   if(nativeRoleOwner!=NativePlayerCommit.owner(pc))return false;
   return id==pc.playerObjectId()&&source.equals(pc.sourceCharacterIdentity())&&account.equals(pc.sourceAccountIdentity())&&name.equals(pc.selectedCharacterName())&&phase==CombatAccess.field(pc,"phase")&&room==number(pc,"currentRoomId")&&map==number(pc,"classicWorldMapId")&&instance==number(pc,"classicWorldInstanceId");
  }
 }
 private record Snapshot(long starts,long ends,boolean locked){
  static Snapshot take(BattleTimedStatus t){return new Snapshot(number(t,"paralysisActivatesAtNanos"),number(t,"paralysisExpiresAtNanos"),(boolean)CombatAccess.field(t,"paralysisLockEmitted"));}
  boolean present(){return starts!=0;}
  void apply(BattleTimedStatus t){CombatAccess.set(t,"paralysisActivatesAtNanos",starts);CombatAccess.set(t,"paralysisExpiresAtNanos",ends);CombatAccess.set(t,"paralysisLockEmitted",locked);}
 }
 private static final class Curse extends CombatPoisonLifecycle.Lease {
  final Target target;final boolean managed;
  int phase;long warning,active,initialWall,initialLogical,activationWall,activationLogical;
  Removal removal;
  Curse(ClientSession pc,boolean managed){this.target=Target.take(pc);this.managed=managed;}
 }
 static final class Scope {
  private final ArrayList<Context> contexts=new ArrayList<>();boolean closed;
  Context context(ClientSession pc){for(var c:contexts)if(c.pc==pc)return c;var c=new Context(this,pc);contexts.add(c);return c;}
  Context find(ClientSession pc){for(var c:contexts)if(c.pc==pc)return c;return null;}
  void close(boolean failed){if(closed)return;Throwable failure=null;for(var c:contexts)if((failed||!c.valid())&&(c.producerActive&&c.state.isCurrent(c.producer)||c.clockActive&&c.state.isCurrent(c.clock)))try{abort(c);}catch(Throwable e){failure=combine(failure,e);}closed=true;for(var c:contexts)try{finish(c,true);}catch(Throwable e){failure=combine(failure,e);}if(failure!=null)throw CombatPoisonLifecycle.propagate(failure);}
 }
 private static final class Context {
  final Scope scope;final ClientSession pc;final BattleTimedStatus timed;final CombatPoisonLifecycle.State state;final Target target;
  Curse producer,clock;boolean producerActive,clockActive,activation,terminal,removalActive;long now;Removal removal;
  Context(Scope scope,ClientSession pc){this.scope=scope;this.pc=pc;timed=CombatPoisonAtomic.status(pc);state=CombatPoisonLifecycle.curseState(timed,true);target=Target.take(pc);}
  boolean valid(){return target.valid(pc);}
 }
 /** One native release may belong to both channels; only the final unlocked state sends it. */
 static final class Release {
  final ClientSession pc;final Target target;final byte[] packet;final String reason;final long now;final AtomicBoolean sent=new AtomicBoolean(),colorRequired=new AtomicBoolean(),colorReady=new AtomicBoolean();
  Release(ClientSession pc,byte[] packet,String reason,long now){this.pc=pc;target=Target.take(pc);this.packet=packet;this.reason=reason;this.now=now;}
  void send(){if(colorRequired.get()&&!colorReady.get()||!target.valid(pc)||number(pc,"playerHp")==0||CombatPoisonLifecycle.releaseBlocked(pc,now)||!sent.compareAndSet(false,true))return;sendPacket(pc,packet,reason);}
 }
 private record Palette(int intended,String reason,ClientSession recipient,long now){}
 private static final class Removal implements Runnable {
  final Context context;final Curse lease;final Snapshot after;final long owner=Thread.currentThread().getId();
  CombatPoisonLifecycle.Transition transition;volatile boolean applied;boolean sealed,draining,colorPublished,releaseRegistered;
  final ArrayDeque<Palette> palettes=new ArrayDeque<>();final ArrayList<Release> releases=new ArrayList<>();
  Removal(Context c,Curse lease,Snapshot after,boolean applied){context=c;this.lease=lease;this.after=after;this.applied=applied;}
  boolean palette(Palette p){synchronized(this){if(sealed)return false;palettes.add(p);}fast();return true;}
  boolean release(Release r){synchronized(this){if(sealed)return false;releaseRegistered=true;r.colorRequired.set(true);if(applied&&colorPublished)r.colorReady.set(true);releases.add(r);}fast();return true;}
  void published(){synchronized(this){if(!applied)return;colorPublished=true;for(var r:releases)r.colorReady.set(true);}fast();}
  private void fast(){if(owner==Thread.currentThread().getId()&&!context.scope.closed&&(lease.callbacks.get()&((1<<30)-1))==1)drain(false);}
  public void run(){drain(true);}
  private void drain(boolean terminal){synchronized(this){if(draining){if(!terminal)return;throw new IllegalStateException("overlapping curse cleanup");}draining=true;}
   try{synchronized(context.timed){if(!applied){if(context.state.current()!=transition)throw new IllegalStateException("curse cleanup lost exact lease");after.apply(context.timed);applied=true;}}
    while(true){Palette p;synchronized(this){p=palettes.poll();}if(p==null)break;if(context.valid()&&lease.target.valid(context.pc)){int color=(int)CombatAccess.call(context.pc,"currentPoisonPalette","(J)I",p.now!=0?p.now:System.nanoTime());if(p.recipient==null)CombatPoisonLifecycle.rawPalette(context.pc,color,p.reason);else sendPacket(p.recipient,Protocol.poison(context.pc.playerObjectId(),color),p.reason.replace(" type="+p.intended+" reason="," type="+color+" reason="));colorPublished=true;}}
    if(terminal&&lease.managed&&context.valid()&&lease.target.valid(context.pc)){if(!colorPublished){CombatPoisonLifecycle.rawPalette(context.pc,(int)CombatAccess.call(context.pc,"currentPoisonPalette","(J)I",context.now!=0?context.now:System.nanoTime()),"source-curse-cleanup");colorPublished=true;}synchronized(this){if(!releaseRegistered&&number(context.pc,"playerHp")>0){var release=new Release(context.pc,Protocol.paralysis(Protocol.ParalysisMode.PARALYSIS_RELEASE),"S_PARALYSE mode=3 reason=source-curse-cleanup",context.now!=0?context.now:System.nanoTime());releaseRegistered=true;release.colorRequired.set(true);release.colorReady.set(true);releases.add(release);}}}
    List<Release> ready;synchronized(this){if(!(terminal||colorPublished))return;ready=List.copyOf(releases);for(var r:ready)r.colorReady.set(true);releases.clear();if(terminal)sealed=true;}for(var r:ready)if(context.valid()&&lease.target.valid(context.pc))r.send();
   }finally{synchronized(this){draining=false;}}
  }
 }
 private static long number(Object o,String f){return ((Number)CombatAccess.field(o,f)).longValue();}
 private static Throwable combine(Throwable first,Throwable next){if(first==null)return next;if(first!=next)first.addSuppressed(next);return first;}
 private static void sendPacket(ClientSession pc,byte[] packet,String reason){try{CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,reason);}catch(Throwable e){throw CombatPoisonLifecycle.propagate(e);}}
 private static CombatPoisonLifecycle.Lease current(BattleTimedStatus t){var s=CombatPoisonLifecycle.curseState(t,false);return s==null?null:s.current();}
 private static Curse owner(CombatPoisonLifecycle.Lease lease){if(lease instanceof CombatPoisonLifecycle.Transition tr)lease=tr.owner;return lease instanceof Curse c?c:null;}
 static boolean visual(BattleTimedStatus t,long now){synchronized(t){var snap=Snapshot.take(t);if(!snap.present())return false;var c=owner(current(t));return c!=null&&c.managed?c.phase!=0:now<snap.ends;}}
 static boolean active(BattleTimedStatus t,long now){synchronized(t){var snap=Snapshot.take(t);if(!snap.present())return false;var c=owner(current(t));return c!=null&&c.managed?c.phase==ACTIVE:now>=snap.starts&&now<snap.ends;}}
 static boolean pending(BattleTimedStatus t){synchronized(t){var c=owner(current(t));return Snapshot.take(t).present()&&c!=null&&c.managed&&c.phase==APPLYING;}}
 static boolean activate(BattleTimedStatus t,long now,int warning,int duration,ClientSession pc,Scope scope){
  if(!CombatRules.loaded())return t.activateParalysis(now,warning,duration);
  synchronized(t){var c=scope.context(pc);if(c.timed!=t||c.state.current()!=null)return false;if(!t.activateParalysis(now,warning,duration))return false;var before=Snapshot.take(t);var lease=new Curse(pc,true);lease.warning=before.starts-now;lease.active=before.ends-before.starts;lease.initialWall=System.nanoTime();lease.initialLogical=Math.max(now,lease.initialWall);lease.phase=INITIAL;new Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,false).apply(t);if(!c.state.install(lease))throw new IllegalStateException("curse installation lost slot");c.producer=lease;if(!lease.enter(c.state))throw new IllegalStateException("curse initial callback rejected");c.producerActive=true;return true;}
 }
 private static Curse adopt(Context c){var old=c.state.current();if(old==null&&Snapshot.take(c.timed).present()){var lease=new Curse(c.pc,false);lease.started=true;if(!c.state.install(lease))throw new IllegalStateException("curse adoption raced under status monitor");return lease;}return owner(old);}
 static <T> T mutate(BattleTimedStatus t,ClientSession pc,Scope scope,Supplier<T> operation){
  if(!CombatRules.loaded())return operation.get();var c=scope.context(pc);synchronized(t){var before=Snapshot.take(t);var lease=adopt(c);if(c.state.current() instanceof CombatPoisonLifecycle.Transition tr){boolean admitted=c.clockActive&&c.clock==tr.owner;if(!admitted){new Snapshot(0,0,false).apply(t);try{return operation.get();}finally{before.apply(t);}}return operation.get();}T result=operation.get();var after=Snapshot.take(t);if(lease!=null&&before.present()&&!after.present()){if(!lease.enter(c.state))throw new IllegalStateException("curse cure lost lease");boolean defer=(lease.callbacks.get()&((1<<30)-1))>1;var removal=new Removal(c,lease,after,!defer);lease.removal=removal;c.removal=removal;c.removalActive=true;var tr=lease.close(c.state,removal);if(tr==null){c.removalActive=false;lease.leave();throw new IllegalStateException("curse cure lost claim");}removal.transition=tr;if(defer)before.apply(t);}return result;}
 }
 static BattleTimedStatus.ParalysisPoll poll(BattleTimedStatus t,long now,ClientSession pc,Scope scope){
  if(!CombatRules.loaded())return t.pollParalysis(now);var c=scope.context(pc);c.now=now;
  synchronized(t){var lease=adopt(c);if(lease==null)return t.pollParalysis(now);if(c.state.current() instanceof CombatPoisonLifecycle.Transition)return new BattleTimedStatus.ParalysisPoll(false,false,false);if(!lease.managed)return mutate(t,pc,scope,()->t.pollParalysis(now));if(!lease.target.valid(pc)){mutate(t,pc,scope,()->t.cancelParalysis());return new BattleTimedStatus.ParalysisPoll(false,false,false);}if(lease.phase==INITIAL)return new BattleTimedStatus.ParalysisPoll(false,false,false);if(!lease.enter(c.state))return new BattleTimedStatus.ParalysisPoll(false,false,false);c.clock=lease;c.clockActive=true;}
  var result=mutate(t,pc,scope,()->{var lease=c.clock;var snap=Snapshot.take(t);if(lease.phase==WAITING&&now>=snap.starts){lease.activationWall=System.nanoTime();lease.activationLogical=Math.max(now,lease.activationWall);lease.phase=APPLYING;c.activation=true;return new BattleTimedStatus.ParalysisPoll(true,false,false);}if(lease.phase==ACTIVE&&now>=snap.ends){var cancelled=t.cancelParalysis();c.terminal=true;return new BattleTimedStatus.ParalysisPoll(false,true,cancelled.unlockRequired());}return new BattleTimedStatus.ParalysisPoll(false,false,false);});if(!c.activation&&!c.terminal)finish(c,false);return result;
 }
 static boolean note(BattleTimedStatus t,long now,ClientSession pc,Scope scope){if(!CombatRules.loaded())return t.noteParalysisLockSent(now);var c=scope.context(pc);synchronized(t){var lease=adopt(c);if(lease==null||!lease.managed)return active(t,now)&&t.noteParalysisLockSent(now);if(!lease.target.valid(pc)){mutate(t,pc,scope,()->t.cancelParalysis());return false;}if(!active(t,now)||c.state.current() instanceof CombatPoisonLifecycle.Transition||!lease.enter(c.state))return false;c.clock=lease;c.clockActive=true;CombatAccess.set(t,"paralysisLockEmitted",true);return true;}}
 static boolean initialPalette(ClientSession pc,int type,String reason,Scope scope){var c=scope==null?null:scope.find(pc);if(c==null||!c.producerActive)return false;if(c.valid())CombatPoisonLifecycle.rawPalette(pc,2,reason);synchronized(c.timed){if(c.valid()&&c.state.isCurrent(c.producer)){long anchor=c.producer.initialLogical+Math.max(0,System.nanoTime()-c.producer.initialWall);new Snapshot(Math.addExact(anchor,c.producer.warning),Long.MAX_VALUE,false).apply(c.timed);c.producer.phase=WAITING;c.producer.started=true;}}finish(c,false);return true;}
 static void applied(ClientSession pc,Scope scope){var c=scope==null?null:scope.find(pc);if(c==null||!c.clockActive)return;synchronized(c.timed){var lease=c.clock;var old=c.state.current();if(c.valid()&&c.activation&&(old==lease||old instanceof CombatPoisonLifecycle.Transition tr&&tr.owner==lease)){long end=c.state.isCurrent(lease)?Math.addExact(lease.activationLogical+Math.max(0,System.nanoTime()-lease.activationWall),lease.active):Long.MAX_VALUE;new Snapshot(lease.activationLogical,end,true).apply(c.timed);lease.phase=ACTIVE;}}finish(c,false);}
 static boolean allowApply(ClientSession pc,Scope scope){var c=scope==null?null:scope.find(pc);if(c!=null&&c.clockActive&&(!c.valid()||!c.clock.target.valid(pc))){finish(c,false);return false;}return true;}
 static void paletteFinished(ClientSession pc,String reason,Scope scope){var c=scope==null?null:scope.find(pc);if(c!=null&&c.terminal&&(reason.equals("curse-paralysis-expired")||reason.equals("classic-world-monster-paralysis-expired")))finish(c,false);}
 private static Removal removal(ClientSession pc){var t=CombatPoisonAtomic.status(pc);synchronized(t){var lease=current(t);return lease instanceof CombatPoisonLifecycle.Transition?owner(lease).removal:null;}}
 static boolean enqueuePalette(ClientSession pc,int type,String reason,ClientSession recipient,long now){var r=removal(pc);return r!=null&&r.context.valid()&&r.palette(new Palette(type,reason,recipient,now));}
 static boolean enqueueRelease(ClientSession pc,Release token){var r=removal(pc);return r!=null&&r.context.valid()&&r.release(token);}
 static void colorPublished(ClientSession pc){var r=removal(pc);if(r!=null&&r.context.valid())r.published();}
 static boolean skipActivationPalette(ClientSession pc,Scope scope){var c=scope==null?null:scope.find(pc);return c!=null&&c.activation;}
 private static void abort(Context c){mutate(c.timed,c.pc,c.scope,()->c.timed.cancelParalysis());if(c.removal!=null)c.removal.release(new Release(c.pc,Protocol.paralysis(Protocol.ParalysisMode.PARALYSIS_RELEASE),"S_PARALYSE mode=3 reason=source-curse-failed",c.now!=0?c.now:System.nanoTime()));}
 private static void finish(Context c,boolean all){Throwable failure=null;if((c.producerActive||c.clockActive)&&(!c.valid()||c.clockActive&&!c.clock.target.valid(c.pc)))try{abort(c);}catch(Throwable e){failure=e;}if(c.removalActive&&(all||c.removal.lease==c.clock))try{c.removalActive=false;c.removal.lease.leave();}catch(Throwable e){failure=combine(failure,e);}if(c.producerActive)try{c.producerActive=false;c.producer.leave();}catch(Throwable e){failure=combine(failure,e);}if(c.clockActive)try{c.clockActive=false;c.clock.leave();}catch(Throwable e){failure=combine(failure,e);}if(failure!=null)throw CombatPoisonLifecycle.propagate(failure);}
}
