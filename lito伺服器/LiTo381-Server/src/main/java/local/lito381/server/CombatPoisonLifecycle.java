// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// 待驗證：nativeRawOriginalBattle 的外層例外保護區間仍 DIFF；不得視為已驗收，見 issue-68 報告。
package local.lito381.server;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/** Poison callbacks own exact leases. No timer, SQL or global actor registry. */
final class CombatPoisonLifecycle {
 private static final String P="local/lito381/server/",C="L"+P+"ClientSession;",S="L"+P+"CombatPoisonLifecycle$Scope;";
 private static final String FIELD="sourcePoisonLifecycle";
 private static final int CLOSING=1<<30,ACTIVE_MASK=CLOSING-1;
 private static final AtomicLong OWNER_TOKENS=new AtomicLong();
 private static final int PARALYSIS_INITIAL=1,PARALYSIS_WAITING=2,PARALYSIS_COLORING=3,PARALYSIS_APPLYING=4,PARALYSIS_ACTIVE=5;
 /** Values belong to a character, never to the next character using its socket. */
 private record Actor(long object,String source,String account,String selected){
  static Actor take(ClientSession pc){return pc==null?null:new Actor(pc.playerObjectId(),pc.sourceCharacterIdentity(),pc.sourceAccountIdentity(),pc.selectedCharacterName());}
  boolean same(ClientSession pc){return pc!=null&&object==pc.playerObjectId()&&source.equals(pc.sourceCharacterIdentity())&&account.equals(pc.sourceAccountIdentity())&&selected.equals(pc.selectedCharacterName());}
 }
 private record Death(Actor target,Actor caster,long room,long generation){}
 private static final class Damage {final Death identity;final long thread=Thread.currentThread().getId();Damage next;Damage(Death identity,Damage next){this.identity=identity;this.next=next;}}
 static final class State {
        private volatile Object sourceNpcOwner;
        private volatile Object sourcePlayerOwner;
        private volatile Object sourcePlayerQueue;
  private volatile Lease current;
  private State curse;
  private BattleTimedStatus monsterCurse;
  private State monsterCallbacks;
  private long monsterEpoch;
  private boolean sourceRawFreeze;
  private long sourceRawFreezeEpoch;
  private long ownerToken;
  private CombatNpcDamageQueue.Queue monsterDamage;
  Lease current(){return current;}
  private Death death;
  private Damage damage;
  private static final AtomicReferenceFieldUpdater<State,Lease> UPDATER=AtomicReferenceFieldUpdater.newUpdater(State.class,Lease.class,"current");
  boolean install(Lease value){return value!=null&&!(value instanceof Transition)&&UPDATER.compareAndSet(this,null,value);}
  boolean isCurrent(Lease value){return value!=null&&current==value;}
  Transition claim(Lease value){if(value==null)return null;var transition=new Transition(value);return UPDATER.compareAndSet(this,value,transition)?transition:null;}
  boolean complete(Transition value){return value!=null&&UPDATER.compareAndSet(this,value,null);}
 }
 static class Lease {
        private volatile Object sourceCombatOrigin;
  final Actor target,caster;
  BattleTimedStatus paralysis;long warningNanos,activeNanos,initialLogical,initialWall,activationLogical,activationWall;int paralysisPhase;
  Lease(){this(null,null);}
  Lease(Actor target,Actor caster){this.target=target;this.caster=caster;NativeCombatOrigin.leaseCreated(this);}
  final AtomicInteger callbacks=new AtomicInteger();
  private final AtomicReference<Plan> cleanup=new AtomicReference<>();
  private final AtomicBoolean cleanupStarted=new AtomicBoolean();
  volatile boolean started;
  boolean enter(State target){if(!target.isCurrent(this)||!enterCount())return false;if(target.isCurrent(this))return true;leave();return false;}
  private boolean enterCount(){while(true){int old=callbacks.get();if((old&CLOSING)!=0)return false;if((old&ACTIVE_MASK)==ACTIVE_MASK)throw new IllegalStateException("poison callback count overflow");if(callbacks.compareAndSet(old,old+1))return true;}}
  void leave(){int value=callbacks.decrementAndGet();if(value==CLOSING)finishCleanup();}
  Transition close(State target,Runnable action){return close(target,action,null);}
  Transition closeDeferred(State target,java.util.function.Consumer<Runnable> action){return close(target,null,action);}
  private Transition close(State target,Runnable action,java.util.function.Consumer<Runnable> deferred){var transition=target.claim(this);if(transition==null)return null;cleanup.set(new Plan(target,transition,action,deferred));while(true){int old=callbacks.get();if((old&CLOSING)!=0)return transition;int next=old|CLOSING;if(callbacks.compareAndSet(old,next)){if((next&ACTIVE_MASK)==0)finishCleanup();return transition;}}}
  private void finishCleanup(){if(!cleanupStarted.compareAndSet(false,true))return;var plan=cleanup.get();if(plan==null)throw new IllegalStateException("poison cleanup plan missing");Runnable complete=()->{if(!plan.target.complete(plan.transition))throw new IllegalStateException("poison cleanup lost transition ownership");};if(plan.deferred!=null){try{plan.deferred.accept(complete);}catch(Throwable failure){if(plan.target.current()==plan.transition)complete.run();throw failure;}}else try{plan.action.run();}finally{complete.run();}}
 }
 static final class Transition extends Lease {final Lease owner;Transition(Lease owner){this.owner=owner;}}
 private record Plan(State target,Transition transition,Runnable action,java.util.function.Consumer<Runnable> deferred){}
 private record ParalysisSnapshot(BattleTimedStatus slot,long starts,long ends,boolean locked){
  static ParalysisSnapshot take(BattleTimedStatus t){var child=(BattleTimedStatus)CombatAccess.field(t,"sourcePoisonParalysis");return child==null?null:new ParalysisSnapshot(child,number(child,"paralysisActivatesAtNanos"),number(child,"paralysisExpiresAtNanos"),(boolean)CombatAccess.field(child,"paralysisLockEmitted"));}
  void restore(){CombatAccess.set(slot,"paralysisActivatesAtNanos",starts);CombatAccess.set(slot,"paralysisExpiresAtNanos",ends);CombatAccess.set(slot,"paralysisLockEmitted",locked);}
 }
 private record Snapshot(long expires,long next,boolean silence,ClientSession caster,long npc,ClassicWorldMonsterPoisonAttack.Kind kind,ParalysisSnapshot paralysis){
  static Snapshot take(ClientSession pc,BattleTimedStatus t){return new Snapshot(number(t,"poisonExpiresAtNanos"),number(t,"poisonNextTickAtNanos"),(boolean)CombatAccess.field(t,"poisonSilence"),(ClientSession)CombatAccess.field(pc,"poisonAttacker"),number(pc,"classicWorldPoisonAttackerObjectId"),(ClassicWorldMonsterPoisonAttack.Kind)CombatAccess.field(pc,"classicWorldPoisonKind"),ParalysisSnapshot.take(t));}
  boolean present(){return expires!=0||silence||paralysis!=null&&paralysis.starts!=0;}
  void apply(ClientSession pc,BattleTimedStatus t){CombatAccess.set(t,"poisonExpiresAtNanos",expires);CombatAccess.set(t,"poisonNextTickAtNanos",next);CombatAccess.set(t,"poisonSilence",silence);CombatAccess.set(pc,"poisonAttacker",caster);CombatAccess.set(pc,"classicWorldPoisonAttackerObjectId",npc);CombatAccess.set(pc,"classicWorldPoisonKind",kind);if(paralysis!=null)paralysis.restore();CombatAccess.set(t,"sourcePoisonParalysis",paralysis==null?null:paralysis.slot);}
  void hide(ClientSession pc,BattleTimedStatus t){new Snapshot(0,0,false,null,0,ClassicWorldMonsterPoisonAttack.Kind.NONE,null).apply(pc,t);}
 }
 static final class Scope implements AutoCloseable {
  private ArrayList<Context> contexts;
  private boolean closed,failed;
  private CombatCurseLifecycle.Scope curses;
  private CombatMonsterCurseLifecycle.Scope monsters;
  CombatMonsterCurseLifecycle.Scope monsters(){if(monsters==null)monsters=new CombatMonsterCurseLifecycle.Scope();return monsters;}
  CombatCurseLifecycle.Scope curses(){if(curses==null)curses=new CombatCurseLifecycle.Scope();return curses;}
  Context context(ClientSession pc){if(contexts==null)contexts=new ArrayList<>();for(var c:contexts)if(c.pc==pc)return c;var c=new Context(this,pc);contexts.add(c);return c;}
  Context find(ClientSession pc){if(contexts!=null)for(var c:contexts)if(c.pc==pc)return c;return null;}
  public void close(){
   if(closed)return;Throwable failure=null;
   if(contexts!=null)for(var c:contexts)if((failed||!c.valid())&&(c.producerActive&&c.state.isCurrent(c.producer)||c.clockActive&&c.state.isCurrent(c.clock)))try{abort(c,this);if(c.valid())broadcast(c.pc,0,"source-poison-start-failed");}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
   closed=true;
   if(contexts!=null)for(var c:contexts)if(c.removalActive)try{c.removalActive=false;c.removal.lease.leave();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
   if(contexts!=null)for(var c:contexts)if(c.producerActive)try{c.producerActive=false;if(!failed&&c.valid()&&c.state.isCurrent(c.producer))c.producer.started=true;c.producer.leave();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
   if(contexts!=null)for(var c:contexts)if(c.clockActive)try{c.clockActive=false;c.clock.leave();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
   if(monsters!=null)try{monsters.close(failed);}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
   if(curses!=null)try{curses.close(failed);}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
   if(failure!=null)throw propagate(failure);
  }

 }
 private static final class Context {
  final Scope scope;final ClientSession pc;final BattleTimedStatus timed;final State state;
  final long objectId,room,map,instance;final Object phase;
  final Actor target;
  Lease producer,clock;boolean clockActive,producerActive,removalActive,paralysisActivatedPalette,paralysisTerminal;Snapshot frozen;long clockNow,generation;Removal removal;Palette initialSilence;
  Context(Scope scope,ClientSession pc){this.scope=scope;this.pc=pc;timed=CombatPoisonAtomic.status(pc);state=state(timed,true);target=Actor.take(pc);objectId=pc.playerObjectId();phase=CombatAccess.field(pc,"phase");room=number(pc,"currentRoomId");map=number(pc,"classicWorldMapId");instance=number(pc,"classicWorldInstanceId");}
  boolean valid(){return NativeCombatOrigin.contextValid(target.same(pc)&&pc.playerObjectId()==objectId&&CombatAccess.field(pc,"phase")==phase&&number(pc,"currentRoomId")==room&&number(pc,"classicWorldMapId")==map&&number(pc,"classicWorldInstanceId")==instance,this);}
 }
 private record Palette(int intended,String reason,ClientSession recipient,byte[] packet,long now,CombatCurseLifecycle.Release release){Palette(int intended,String reason,ClientSession recipient,byte[] packet,long now){this(intended,reason,recipient,packet,now,null);}}
 private static final class Removal implements Runnable {
  final Context context;final Lease lease;final Snapshot after;final long ownerThread;
  Transition transition;boolean applied,sealed,draining,palettePublished,noticePublished;final boolean wasSilence,paralysisReleaseNeeded,paralysisReleaseAfterActivation;Palette notice;final ArrayDeque<Palette> palettes=new ArrayDeque<>();
  Removal(Context c,Lease lease,Snapshot after,boolean applied,boolean wasSilence,boolean paralysisReleaseNeeded,boolean paralysisReleaseAfterActivation){context=c;this.lease=lease;this.after=after;this.applied=applied;this.wasSilence=wasSilence;this.paralysisReleaseNeeded=paralysisReleaseNeeded;this.paralysisReleaseAfterActivation=paralysisReleaseAfterActivation;ownerThread=Thread.currentThread().getId();}
  boolean enqueue(int type,String reason){return enqueue(type,reason,null,null,0);}
  boolean enqueue(int type,String reason,ClientSession recipient,byte[] packet,long now){return enqueue(new Palette(type,reason,recipient,packet,now));}
  boolean enqueue(Palette palette){synchronized(this){if(sealed)return false;palettes.add(palette);}if(ownerThread==Thread.currentThread().getId()&&!context.scope.closed&&(lease.callbacks.get()&ACTIVE_MASK)==1)drain(false);return true;}
  void notice(byte[] packet,String reason){synchronized(this){if(sealed||noticePublished||notice!=null||!wasSilence)return;notice=new Palette(0,reason,context.pc,packet,0);}if(ownerThread==Thread.currentThread().getId()&&!context.scope.closed&&(lease.callbacks.get()&ACTIVE_MASK)==1)drain(false);}
  public void run(){drain(true);}
  private void drain(boolean terminal){
   synchronized(this){if(draining){if(!terminal)return;throw new IllegalStateException("overlapping poison cleanup drain");}draining=true;}
   try{
    synchronized(context.timed){if(!applied){if(context.state.current!=transition)throw new IllegalStateException("poison cleanup does not own state");after.apply(context.pc,context.timed);applied=true;}}
    while(true){Palette palette;boolean message=false;synchronized(this){palette=palettes.poll();if(palette==null){if(notice!=null&&(terminal||palettePublished)){palette=notice;notice=null;noticePublished=true;message=true;}else{if(terminal)sealed=true;return;}}}if(context.valid()){if(message)CombatAccess.call(context.pc,"sendEncrypted","([BLjava/lang/String;)V",palette.packet,palette.reason);else{long now=palette.now!=0?palette.now:context.clockNow!=0?context.clockNow:System.nanoTime();if(palette.intended==-1){if(palette.release!=null){palette.release.send();continue;}if((!paralysisReleaseAfterActivation||lease.paralysisPhase>=PARALYSIS_APPLYING)&&!context.timed.paralysisActive(now))CombatAccess.call(context.pc,"sendEncrypted","([BLjava/lang/String;)V",palette.packet,palette.reason);continue;}int type=(int)CombatAccess.call(context.pc,"currentPoisonPalette","(J)I",now);if(palette.recipient==null)rawPalette(context.pc,type,palette.reason);else CombatAccess.call(palette.recipient,"sendEncrypted","([BLjava/lang/String;)V",Protocol.poison(context.pc.playerObjectId(),type),palette.reason.replace(" type="+palette.intended+" reason="," type="+type+" reason="));if(palette.recipient==context.pc)CombatCurseLifecycle.colorPublished(context.pc);synchronized(this){palettePublished=true;}}}}
   }finally{synchronized(this){draining=false;}}
  }
 }
 @SuppressWarnings("unchecked") private static <E extends Throwable> RuntimeException checked(Throwable e)throws E{throw (E)e;}
 static RuntimeException propagate(Throwable e){Throwable cause=e;while(cause instanceof IllegalStateException&&cause.getCause()!=null)cause=cause.getCause();if(cause instanceof java.io.IOException)return CombatPoisonLifecycle.<RuntimeException>checked(cause);if(e instanceof RuntimeException r)return r;if(e instanceof Error error)throw error;return new IllegalStateException("poison lifecycle callback failed",e);}
 private static long number(Object o,String field){return ((Number)CombatAccess.field(o,field)).longValue();}
 private static State state(BattleTimedStatus t,boolean create){synchronized(t){var value=(State)CombatAccess.field(t,FIELD);if(value==null&&create){value=new State();CombatAccess.set(t,FIELD,value);}return value;}}
 static long ownerToken(ClientSession pc,boolean create){var t=CombatPoisonAtomic.status(pc);synchronized(t){var value=state(t,create);if(value==null)return 0;if(create&&value.ownerToken==0){long token=OWNER_TOKENS.incrementAndGet();if(token<=0)throw new IllegalStateException("combat owner token exhausted");value.ownerToken=token;}return value.ownerToken;}}
 static State curseState(BattleTimedStatus t,boolean create){synchronized(t){var root=state(t,create);if(root==null)return null;if(root.curse==null&&create)root.curse=new State();return root.curse;}}
 static BattleTimedStatus monsterCurse(BattleTimedStatus t,boolean create){synchronized(t){var root=state(t,create);if(root==null)return null;if(root.monsterCurse==null&&create)root.monsterCurse=new BattleTimedStatus();return root.monsterCurse;}}
 static void discardMonsterCurse(BattleTimedStatus t){synchronized(t){var root=state(t,false);if(root!=null)root.monsterCurse=null;}}
 static State monsterCallbacks(BattleTimedStatus t,boolean create){synchronized(t){var root=state(t,create);if(root==null)return null;if(root.monsterCallbacks==null&&create)root.monsterCallbacks=new State();return root.monsterCallbacks;}}
 static State monsterPoisonCallbacks(BattleTimedStatus t,boolean create){return state(t,create);}
 static CombatNpcDamageQueue.Queue monsterDamage(BattleTimedStatus t,boolean create){synchronized(t){var root=state(t,create);if(root==null)return null;if(root.monsterDamage==null&&create)root.monsterDamage=new CombatNpcDamageQueue.Queue();return root.monsterDamage;}}
 static long monsterEpoch(BattleTimedStatus t,boolean advance){synchronized(t){var root=state(t,advance);if(root==null)return 0;if(advance)root.monsterEpoch++;return root.monsterEpoch;}}
 static boolean rawFreeze(BattleTimedStatus t){synchronized(t){var value=state(t,false);return value!=null&&value.sourceRawFreeze&&value.sourceRawFreezeEpoch==value.monsterEpoch;}}
 static void rawFreeze(BattleTimedStatus t,boolean active){synchronized(t){var value=state(t,true);value.sourceRawFreeze=active;value.sourceRawFreezeEpoch=value.monsterEpoch;}}
 static boolean reserved(BattleTimedStatus t){synchronized(t){var value=state(t,false);if(value!=null)retireIdle(t,value);return value!=null&&value.current!=null;}}
 static boolean existing(boolean legacy,ClientSession pc){return legacy||CombatRules.loaded()&&reserved(CombatPoisonAtomic.status(pc));}
 static boolean closing(ClientSession pc){if(!CombatRules.loaded())return false;var t=CombatPoisonAtomic.status(pc);synchronized(t){var value=state(t,false);return value!=null&&value.current instanceof Transition;}}
 static Scope open(){return CombatRules.loaded()?new Scope():null;}
 static void finish(Scope scope){if(scope!=null)scope.close();}
 static void failed(Scope scope,Throwable failure){if(scope!=null)scope.failed=true;try{finish(scope);}catch(Throwable e){if(e!=failure)failure.addSuppressed(e);}}
 static void installed(ClientSession pc,Scope scope,long now){var t=CombatPoisonAtomic.status(pc);var value=state(t,true);var lease=new Lease(Actor.take(pc),Actor.take((ClientSession)CombatAccess.field(pc,"poisonAttacker")));lease.started=scope==null;var child=(BattleTimedStatus)CombatAccess.field(t,"sourcePoisonParalysis");if(child!=null){lease.paralysis=child;lease.warningNanos=number(child,"paralysisActivatesAtNanos")-now;lease.activeNanos=number(child,"paralysisExpiresAtNanos")-number(child,"paralysisActivatesAtNanos");lease.initialWall=System.nanoTime();lease.initialLogical=Math.max(now,lease.initialWall);lease.paralysisPhase=scope==null?PARALYSIS_WAITING:PARALYSIS_INITIAL;CombatAccess.set(child,"paralysisExpiresAtNanos",Long.MAX_VALUE);if(scope!=null)CombatAccess.set(child,"paralysisActivatesAtNanos",Long.MAX_VALUE);}if(!value.install(lease))throw new IllegalStateException("poison lease installed over reservation");if(scope!=null){var c=scope.context(pc);c.producer=lease;if(!lease.enter(value))throw new IllegalStateException("new poison producer could not enter its lease");c.producerActive=true;}}
 private static Lease paralysisLease(BattleTimedStatus t,BattleTimedStatus child){var value=state(t,false);var lease=value==null?null:value.current;if(lease instanceof Transition transition)lease=transition.owner;return lease!=null&&lease.paralysis==child?lease:null;}
 static boolean paralysisVisual(BattleTimedStatus t,BattleTimedStatus child,long now){synchronized(t){var lease=paralysisLease(t,child);return lease==null?child.paralysisVisualActive(now):lease.paralysisPhase!=0;}}
 static boolean paralysisActive(BattleTimedStatus t,BattleTimedStatus child,long now){synchronized(t){var lease=paralysisLease(t,child);return lease==null?child.paralysisActive(now):(lease.paralysisPhase==PARALYSIS_APPLYING||lease.paralysisPhase==PARALYSIS_ACTIVE);}}
 static boolean paralysisGray(BattleTimedStatus t){synchronized(t){var child=(BattleTimedStatus)CombatAccess.field(t,"sourcePoisonParalysis");var lease=child==null?null:paralysisLease(t,child);return lease!=null&&lease.paralysisPhase>=PARALYSIS_COLORING;}}
 static BattleTimedStatus.ParalysisPoll paralysisStep(BattleTimedStatus t,BattleTimedStatus child,long now){
  synchronized(t){var lease=paralysisLease(t,child);if(lease==null)return child.pollParalysis(now);if(lease.paralysisPhase==PARALYSIS_WAITING&&now>=number(child,"paralysisActivatesAtNanos")){lease.paralysisPhase=PARALYSIS_COLORING;lease.activationWall=System.nanoTime();lease.activationLogical=Math.max(now,lease.activationWall);CombatAccess.set(child,"paralysisActivatesAtNanos",lease.activationLogical);return new BattleTimedStatus.ParalysisPoll(true,false,false);}if(lease.paralysisPhase==PARALYSIS_ACTIVE&&now>=number(child,"paralysisExpiresAtNanos")){var result=child.cancelParalysis();return new BattleTimedStatus.ParalysisPoll(false,true,result.unlockRequired());}return new BattleTimedStatus.ParalysisPoll(false,false,false);}
 }
 private static void grayParalysisPublished(Context c){synchronized(c.timed){var lease=c.clock;var owner=c.state.current;boolean current=owner==lease||owner instanceof Transition transition&&transition.owner==lease;if(c.valid()&&current&&lease.paralysis!=null&&lease.paralysisPhase==PARALYSIS_COLORING&&CombatAccess.field(c.timed,"sourcePoisonParalysis")==lease.paralysis){CombatAccess.set(lease.paralysis,"paralysisLockEmitted",true);lease.paralysisPhase=PARALYSIS_APPLYING;}}}
 private static void initialParalysisPublished(Context c){synchronized(c.timed){var lease=c.producer;if(c.valid()&&c.state.isCurrent(lease)&&lease.paralysis!=null&&lease.paralysisPhase==PARALYSIS_INITIAL&&CombatAccess.field(c.timed,"sourcePoisonParalysis")==lease.paralysis){long anchor=Math.addExact(lease.initialLogical,Math.max(0,System.nanoTime()-lease.initialWall));CombatAccess.set(lease.paralysis,"paralysisActivatesAtNanos",Math.addExact(anchor,lease.warningNanos));lease.paralysisPhase=PARALYSIS_WAITING;}}}
 private static void activeParalysisPublished(Context c){synchronized(c.timed){var lease=c.clock;if(c.valid()&&c.state.isCurrent(lease)&&lease.paralysis!=null&&lease.paralysisPhase==PARALYSIS_APPLYING&&CombatAccess.field(c.timed,"sourcePoisonParalysis")==lease.paralysis){long anchor=Math.addExact(lease.activationLogical,Math.max(0,System.nanoTime()-lease.activationWall));CombatAccess.set(lease.paralysis,"paralysisExpiresAtNanos",Math.addExact(anchor,lease.activeNanos));lease.paralysisPhase=PARALYSIS_ACTIVE;}}}
 private static void finishParalysis(Context c){Throwable failure=null;if(c.clockActive&&!c.valid()&&c.state.isCurrent(c.clock))try{abort(c,c.scope);}catch(Throwable e){failure=e;}if(c.removalActive&&c.removal.lease==c.clock)try{c.removalActive=false;c.removal.lease.leave();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}if(c.clockActive)try{c.clockActive=false;c.clock.leave();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}if(failure!=null)throw propagate(failure);}
 static void paralysisApplySend(ClientSession pc,byte[] packet,String reason,Scope scope){try{if(!CombatCurseLifecycle.allowApply(pc,scope==null?null:scope.curses))return;var c=scope==null?null:scope.find(pc);if(c!=null&&c.clockActive&&!c.valid()){finishParalysis(c);return;}CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,reason);CombatCurseLifecycle.applied(pc,scope==null?null:scope.curses);if(c!=null&&c.clockActive){if(c.paralysisActivatedPalette)activeParalysisPublished(c);finishParalysis(c);}}catch(Throwable failure){throw propagate(failure);}}
 static CombatPoisonAtomic.NpcState npc(ClientSession pc,long object,int npc,int poison,int paralysis,long now,String source,Scope scope){return CombatPoisonAtomic.npc(pc,object,npc,poison,paralysis,now,source,scope);}
 static boolean player(BattleTimedStatus t,long now,int seconds,int interval,ClientSession pc,ClientSession caster,Scope scope){return CombatPoisonAtomic.player(t,now,seconds,interval,pc,caster,scope);}
 private static boolean active(BattleTimedStatus t){var child=(BattleTimedStatus)CombatAccess.field(t,"sourcePoisonParalysis");return number(t,"poisonExpiresAtNanos")!=0||(boolean)CombatAccess.field(t,"poisonSilence")||child!=null&&number(child,"paralysisActivatesAtNanos")!=0;}
 private static void retireIdle(BattleTimedStatus t,State state){var old=state.current;if(old!=null&&!(old instanceof Transition)&&old.callbacks.get()==0&&!active(t))old.close(state,()->{});}
 private static Lease adopt(Context c){retireIdle(c.timed,c.state);var value=c.state.current;if(value==null&&active(c.timed)){var lease=new Lease(Actor.take(c.pc),Actor.take((ClientSession)CombatAccess.field(c.pc,"poisonAttacker")));lease.started=true;if(!c.state.install(lease))throw new IllegalStateException("poison adoption raced under status monitor");return lease;}return value;}
 private static <T> T mutate(BattleTimedStatus timed,ClientSession pc,Scope scope,Supplier<T> operation,boolean poll){
  if(!CombatRules.loaded())return operation.get();
  if(scope==null){var auto=new Scope();try{return mutate(timed,pc,auto,operation,poll);}finally{auto.close();}}
  synchronized(timed){var existing=state(timed,false);if((existing==null||existing.current==null)&&!active(timed))return operation.get();}
  var c=scope.context(pc);if(c.timed!=timed)throw new IllegalStateException("poison scope status receiver mismatch");
  synchronized(timed){var lease=adopt(c);var before=Snapshot.take(pc,timed);boolean capturedUnlock=before.paralysis==null||before.paralysis.locked||(boolean)CombatAccess.field(timed,"paralysisLockEmitted");boolean plannedUnlock=before.paralysis!=null&&lease!=null&&lease.paralysisPhase==PARALYSIS_COLORING;boolean unlockNeeded=capturedUnlock||plannedUnlock;
   if(lease instanceof Transition transition){boolean admitted=poll&&c.clockActive&&c.clock==transition.owner;if(!admitted){before.hide(pc,timed);try{return operation.get();}finally{before.apply(pc,timed);}}T result=operation.get();if(result instanceof BattleTimedStatus.PoisonPoll value&&value.expired()){before.apply(pc,timed);return (T)new BattleTimedStatus.PoisonPoll(0,false);}return result;}
   T result=operation.get();var after=Snapshot.take(pc,timed);
   if(lease!=null&&before.present()&&!after.present()){if(!lease.enter(c.state))throw new IllegalStateException("poison cure lost exact lease");boolean defer=(lease.callbacks.get()&ACTIVE_MASK)>1;var removal=new Removal(c,lease,after,!defer,before.silence,unlockNeeded,plannedUnlock&&!capturedUnlock);c.removal=removal;c.removalActive=true;var transition=lease.close(c.state,removal);if(transition==null){c.removalActive=false;lease.leave();throw new IllegalStateException("poison cure lost transition claim");}removal.transition=transition;if(defer)before.apply(pc,timed);}
   return result;
  }
 }
 private static <T> T mutateBoth(BattleTimedStatus t,ClientSession pc,Scope scope,Supplier<T> operation){if(!CombatRules.loaded())return operation.get();if(scope==null){try(var auto=new Scope()){return mutateBoth(t,pc,auto,operation);}}return mutate(t,pc,scope,()->CombatCurseLifecycle.mutate(t,pc,scope.curses(),operation),false);}
 static boolean cure(BattleTimedStatus t,ClientSession pc,Scope s){return mutate(t,pc,s,()->CombatNpcPoison.nativeCure(t,pc),false);}
 static boolean silence(BattleTimedStatus t,ClientSession pc,Scope s){return mutate(t,pc,s,()->CombatNpcPoison.nativeSilence(t,pc),false);}
 static BattleTimedStatus.ParalysisCancellation cancel(BattleTimedStatus t,ClientSession pc,Scope s){return mutateBoth(t,pc,s,()->CombatNpcPoison.nativeCancel(t,pc));}
 static void clear(BattleTimedStatus t,ClientSession pc,Scope s){mutateBoth(t,pc,s,()->{CombatNpcPoison.nativeClear(t,pc);return null;});}
 static BattleTimedStatus.CurseRemoval curse(BattleTimedStatus t,ClientSession pc,Scope s){return mutateBoth(t,pc,s,()->CombatNpcPoison.nativeCurse(t,pc));}
 static BattleTimedStatus.MagicCancellation magic(BattleTimedStatus t,ClientSession pc,Scope s){return mutateBoth(t,pc,s,()->CombatNpcPoison.nativeMagic(t,pc));}
 static boolean curseActivate(BattleTimedStatus t,long now,int warning,int duration,ClientSession pc,Scope s){if(!CombatRules.loaded())return t.activateParalysis(now,warning,duration);if(s==null){try(var auto=new Scope()){return curseActivate(t,now,warning,duration,pc,auto);}}return CombatCurseLifecycle.activate(t,now,warning,duration,pc,s.curses());}
 static BattleTimedStatus.ParalysisCancellation npcParalysis(ClientSession pc,Scope s){var t=CombatPoisonAtomic.status(pc);return mutate(t,pc,s,()->CombatNpcPoison.nativeNpcParalysis(pc),false);}
 static BattleTimedStatus.ParalysisCancellation npcCancel(BattleTimedStatus t,ClientSession pc,Scope s){return mutate(t,pc,s,()->CombatRules.loaded()?CombatNpcPoison.nativeNpcParalysis(pc):CombatNpcPoison.nativeCancel(t,pc),false);}
 static BattleTimedStatus.PoisonPoll poll(BattleTimedStatus t,long now,int interval,ClientSession pc,Scope s){return mutate(t,pc,s,()->CombatPoisonAtomic.nativePoll(t,now,interval,pc),true);}
 static BattleTimedStatus.ParalysisPoll paralysisPoll(BattleTimedStatus t,long now,ClientSession pc,Scope scope){
  if(!CombatRules.loaded())return CombatPoisonParalysis.nativePoll(t,now,pc,scope);
  if(scope==null){try(var auto=new Scope()){return paralysisPoll(t,now,pc,auto);}}
  synchronized(t){if(CombatAccess.field(t,"sourcePoisonParalysis")!=null){var c=scope.context(pc);var lease=adopt(c);if(lease!=null&&!(lease instanceof Transition)&&!lease.started){var pending=ParalysisSnapshot.take(t);CombatAccess.set(t,"sourcePoisonParalysis",null);try{return CombatPoisonParalysis.nativePoll(t,now,pc,scope);}finally{pending.restore();CombatAccess.set(t,"sourcePoisonParalysis",pending.slot);}}if(lease!=null&&!(lease instanceof Transition)&&!c.clockActive&&lease.enter(c.state)){c.clock=lease;c.clockActive=true;c.clockNow=now;c.frozen=Snapshot.take(pc,t);}}}
  var result=mutate(t,pc,scope,()->CombatPoisonParalysis.nativePoll(t,now,pc,scope),true);var c=scope.find(pc);
  if(c!=null&&c.clockActive&&c.frozen!=null&&c.frozen.paralysis!=null){var old=c.frozen.paralysis;c.paralysisTerminal=result.expired()&&(c.removalActive&&c.removal.lease==c.clock||CombatAccess.field(t,"sourcePoisonParalysis")==null);if(!old.locked&&now>=old.starts&&now<old.ends&&result.activated()&&c.valid()){rawPalette(pc,(int)CombatAccess.call(pc,"currentPoisonPalette","(J)I",now),"source-poison-paralysis-activated");grayParalysisPublished(c);c.paralysisActivatedPalette=true;if(number(pc,"playerHp")==0){activeParalysisPublished(c);finishParalysis(c);}}else if(!c.paralysisTerminal)finishParalysis(c);}
  if(number(pc,"playerHp")==0)CombatCurseLifecycle.applied(pc,scope.curses);
  return result;
 }
 static boolean paralysisNote(BattleTimedStatus t,long now,ClientSession pc,Scope scope){
  if(!CombatRules.loaded())return CombatPoisonParalysis.note(t,now,pc,scope);
  if(scope==null){try(var auto=new Scope()){return paralysisNote(t,now,pc,auto);}}
  synchronized(t){if(CombatAccess.field(t,"sourcePoisonParalysis")!=null){var c=scope.context(pc);var lease=adopt(c);if(lease!=null&&!(lease instanceof Transition)&&!lease.started){var pending=ParalysisSnapshot.take(t);CombatAccess.set(t,"sourcePoisonParalysis",null);try{return CombatPoisonParalysis.note(t,now,pc,scope);}finally{pending.restore();CombatAccess.set(t,"sourcePoisonParalysis",pending.slot);}}if(lease!=null&&!(lease instanceof Transition)&&!c.clockActive&&lease.enter(c.state)){c.clock=lease;c.clockActive=true;c.clockNow=now;c.frozen=Snapshot.take(pc,t);}}}
  return mutate(t,pc,scope,()->CombatPoisonParalysis.note(t,now,pc,scope),true);
 }
 static boolean releaseBlocked(ClientSession pc,long now){var t=CombatPoisonAtomic.status(pc);synchronized(t){var value=state(t,false);var lease=value==null?null:value.current;if(lease instanceof Transition tr)lease=tr.owner;boolean pending=lease!=null&&lease.paralysis!=null&&CombatAccess.field(t,"sourcePoisonParalysis")==lease.paralysis&&lease.paralysisPhase==PARALYSIS_COLORING;return t.paralysisActive(now)||pending||CombatCurseLifecycle.pending(t);}}
 static void paralysisRelease(ClientSession pc,byte[] packet,String reason,long now){
  if(!CombatRules.loaded()){CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,reason);return;}
  var token=new CombatCurseLifecycle.Release(pc,packet,reason,now);boolean queued=CombatCurseLifecycle.enqueueRelease(pc,token);Removal removal=null;var t=CombatPoisonAtomic.status(pc);synchronized(t){var value=state(t,false);if(value!=null&&value.current instanceof Transition transition){var plan=transition.owner.cleanup.get();if(plan!=null&&plan.action instanceof Removal r)removal=r;}}
  if(removal!=null&&removal.context.valid()){if(!removal.paralysisReleaseNeeded&&!queued)return;if(removal.enqueue(new Palette(-1,reason,pc,packet,now,token)))return;}if(!queued)token.send();
 }

 static void rawPalette(ClientSession pc,int type,String reason){try{CombatAccess.call(pc,"sourceBroadcastPoisonNative","(ILjava/lang/String;)V",type,reason);CombatCurseLifecycle.colorPublished(pc);}catch(Throwable failure){throw propagate(failure);}}
 static void broadcast(ClientSession pc,int type,String reason){
  if(CombatRules.loaded()){Removal removal=null;var t=CombatPoisonAtomic.status(pc);synchronized(t){var value=state(t,false);if(value!=null&&value.current instanceof Transition transition){var plan=transition.owner.cleanup.get();if(plan!=null&&plan.action instanceof Removal r)removal=r;}}if(removal!=null&&removal.context.valid()&&removal.enqueue(type,reason))return;}
  if(!CombatCurseLifecycle.enqueuePalette(pc,type,reason,null,0))rawPalette(pc,type,reason);
 }
 static void cancellationSend(ClientSession recipient,byte[] packet,String reason,ClientSession target,int type,long now){
  if(CombatRules.loaded()){Removal removal=null;var t=CombatPoisonAtomic.status(target);synchronized(t){var value=state(t,false);if(value!=null&&value.current instanceof Transition transition){var plan=transition.owner.cleanup.get();if(plan!=null&&plan.action instanceof Removal r)removal=r;}}if(removal!=null&&removal.context.valid()&&removal.enqueue(type,reason,recipient,packet,now))return;}
  if(CombatCurseLifecycle.enqueuePalette(target,type,reason,recipient,now))return;
  try{CombatAccess.call(recipient,"sendEncrypted","([BLjava/lang/String;)V",packet,reason);}catch(Throwable failure){throw propagate(failure);}
 }
 static void palette(ClientSession pc,int type,String reason,Scope scope){
  var roots=scope==null?null:scope.curses;if(CombatCurseLifecycle.initialPalette(pc,type,reason,roots))return;var c=scope==null?null:scope.find(pc);if(reason.equals("classic-world-monster-paralysis-activated")&&(c!=null&&c.paralysisActivatedPalette||CombatCurseLifecycle.skipActivationPalette(pc,roots)))return;if(c!=null&&c.paralysisTerminal&&(reason.equals("classic-world-monster-paralysis-expired")||reason.equals("curse-paralysis-expired"))){broadcast(pc,type,reason);finishParalysis(c);CombatCurseLifecycle.paletteFinished(pc,reason,roots);return;}if(c==null||c.producer==null){broadcast(pc,type,reason);CombatCurseLifecycle.paletteFinished(pc,reason,roots);return;}var lease=c.producer;if(c.producerActive){if(c.valid()){rawPalette(pc,type,reason);initialParalysisPublished(c);initialNotice(c);}return;}if(!lease.enter(c.state))return;
  try{if(c.valid()){rawPalette(pc,type,reason);lease.started=true;}}
  catch(Throwable failure){try{abort(c,scope);broadcast(pc,0,"source-poison-start-failed");}catch(Throwable cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}throw propagate(failure);}
  finally{lease.leave();}
 }
 private static Context clock(ClientSession pc,long now,Scope scope){var t=CombatPoisonAtomic.status(pc);synchronized(t){if(number(t,"poisonExpiresAtNanos")==0)return null;var c=scope.context(pc);var lease=adopt(c);if(lease==null)return null;if(lease instanceof Transition||!lease.started||!lease.enter(c.state))return null;c.clock=lease;c.clockActive=true;c.clockNow=now;c.frozen=Snapshot.take(pc,c.timed);return c;}}
 static Scope beginClock(ClientSession pc,long now){if(!CombatRules.loaded())return null;var scope=new Scope();var c=clock(pc,now,scope);if(c==null){scope.close();return null;}if(c.clock.target!=null&&!c.clock.target.same(pc)){cure(c.timed,pc,scope);scope.close();return null;}c.generation=generation(pc);return scope;}
 static void drive(ClientSession pc,long now,boolean battle,boolean world,BattleSpellCatalog.PoisonSpell poison){
  if(!CombatRules.loaded()){CombatAccess.call(pc,"sourceDamagePoisonClock","(JZZL"+P+"BattleSpellCatalog$PoisonSpell;"+S+")V",now,battle,world,poison,null);return;}
  var scope=beginClock(pc,now);if(scope==null)return;var c=scope.find(pc);
  try{CombatAccess.call(pc,"sourceDamagePoisonClock","(JZZL"+P+"BattleSpellCatalog$PoisonSpell;"+S+")V",now,battle,world,poison,scope);}
  catch(Throwable failure){try{if(c.clockActive){cure(c.timed,pc,scope);broadcast(pc,0,"source-poison-callback-failed");}}catch(Throwable cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}throw propagate(failure);}
  finally{scope.close();}
 }
 private static Context frozen(ClientSession pc,Scope scope){var c=scope==null?null:scope.find(pc);return c!=null&&c.clockActive?c:null;}
 static ClassicWorldMonsterPoisonAttack.Kind kind(ClientSession pc,Scope scope){var c=frozen(pc,scope);return c==null?(ClassicWorldMonsterPoisonAttack.Kind)CombatAccess.field(pc,"classicWorldPoisonKind"):c.frozen.kind;}
 static ClientSession attacker(ClientSession pc,Scope scope){var c=frozen(pc,scope);return c==null?(ClientSession)CombatAccess.field(pc,"poisonAttacker"):c.frozen.caster;}
 static long object(ClientSession pc,Scope scope){var c=frozen(pc,scope);return c==null?number(pc,"classicWorldPoisonAttackerObjectId"):c.frozen.npc;}
 static boolean world(boolean legacy,ClientSession pc,Scope scope){return legacy||CombatRules.loaded()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&kind(pc,scope)!=ClassicWorldMonsterPoisonAttack.Kind.NONE;}
 private static boolean tickAllowed(ClientSession pc,Scope scope){var c=scope==null?null:scope.find(pc);return c==null||c.clock==null||c.clockActive&&c.valid();}
 private static ClientSession liveCaster(Context context) {
        return NativeCombatOrigin.caster(context.clock.caster != null && context.clock.caster.same(context.frozen.caster) && context.frozen.caster.transportOpen() ? context.frozen.caster : null, context);
    }
 private static long generation(ClientSession pc){var hub=(GameHub)CombatAccess.field(pc,"hub");synchronized(hub){var room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(number(pc,"currentRoomId"));return room==null?-1:number(room,"battleGeneration");}}
 private static Damage damage(Context c){synchronized(c.timed){var damage=new Damage(new Death(c.target,c.clock.caster,c.room,c.generation),c.state.damage);c.state.damage=damage;return damage;}}
 private static void finished(Context c,Damage damage){synchronized(c.timed){Damage prior=null;for(var next=c.state.damage;next!=null;next=next.next){if(next==damage){if(prior==null)c.state.damage=next.next;else prior.next=next.next;break;}prior=next;}if(c.state.death==damage.identity&&(number(c.pc,"playerHp")>0||!c.valid()))c.state.death=null;}}
 static void last(ClientSession pc,ClientSession value){var t=CombatPoisonAtomic.status(pc);synchronized(t){var state=state(t,false);if(state!=null)state.death=null;CombatAccess.set(pc,"lastAttacker",value);}}
 static void lastDamage(ClientSession pc,ClientSession value){var t=CombatPoisonAtomic.status(pc);synchronized(t){var state=state(t,false);if(state!=null){state.death=null;for(var damage=state.damage;damage!=null;damage=damage.next)if(damage.thread==Thread.currentThread().getId()){state.death=damage.identity;break;}}CombatAccess.set(pc,"lastAttacker",value);}}
 static BattleRules.Resolution defeated(GameHub hub,long room,ClientSession caster,ClientSession victim){
  if(!CombatRules.loaded())return hub.playerDefeated(room,caster,victim);
  synchronized(hub){var t=CombatPoisonAtomic.status(victim);Death death;synchronized(t){var state=state(t,false);death=state==null?null:state.death;if(state!=null)state.death=null;}
   if(death==null)return hub.playerDefeated(room,caster,victim);
   var raw=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room);
   if(!death.target.same(victim)||death.room!=room||number(victim,"playerHp")!=0||raw==null||number(raw,"battleGeneration")!=death.generation)return BattleRules.Resolution.continuing(false,"ignored-stale-poison-death");
   long killer=death.caster==null?0:death.caster.object;
   // Source character ids are scoped by source. A reused numeric id in the
   // native roster is not evidence that the newly selected character killed.
   if(death.caster!=null)for(var member:hub.battleResultCombatants(room))if(member.playerObjectId()==killer&&!death.caster.same(member)){killer=0;break;}
   return ((BattleRules.MatchState)CombatAccess.field(raw,"match")).playerDefeated(killer,victim.playerObjectId());
  }
 }
 private static void abort(Context c,Scope s){var result=mutate(c.timed,c.pc,s,()->{CombatNpcPoison.nativeCure(c.timed,c.pc);CombatNpcPoison.nativeSilence(c.timed,c.pc);return CombatNpcPoison.nativeNpcParalysis(c.pc);},false);if(result.unlockRequired())paralysisRelease(c.pc,Protocol.paralysis(Protocol.ParalysisMode.PARALYSIS_RELEASE),"S_PARALYSE mode=3 reason=source-poison-callback-failed",c.clockNow==0?System.nanoTime():c.clockNow);if(c.removal!=null&&c.removal.wasSilence)c.removal.notice(Protocol.messageCode(311),"S_MESSAGE_CODE source-npc-silence-poison-cured=311");}
 static final class SilenceNotice {final State state;final Lease lease;final Actor target;SilenceNotice(Context c,Lease lease){state=c.state;this.lease=lease;target=c.target;}}
 static SilenceNotice silenceNotice(ClientSession pc){if(!CombatRules.loaded())return null;var t=CombatPoisonAtomic.status(pc);synchronized(t){if(!t.poisonSilenceActive())return null;var scope=new Scope();var c=scope.context(pc);var lease=adopt(c);if(lease instanceof Transition transition)lease=transition.owner;return lease==null?null:new SilenceNotice(c,lease);}}
 private static void endSilence(ClientSession pc,SilenceNotice notice,byte[] packet,String reason){if(notice==null||!notice.target.same(pc))return;Removal removal=null;var t=CombatPoisonAtomic.status(pc);synchronized(t){if(state(t,false)==notice.state&&notice.state.current instanceof Transition transition&&transition.owner==notice.lease){var plan=notice.lease.cleanup.get();if(plan!=null&&plan.action instanceof Removal r)removal=r;}}if(removal!=null&&removal.context.valid())removal.notice(packet,reason);}
 static void poisonNotice(ClientSession pc,int palette,String reason,SilenceNotice notice){if(notice!=null&&!notice.target.same(pc))return;if(notice!=null){var t=CombatPoisonAtomic.status(pc);synchronized(t){var current=notice.state.current;if(current!=notice.lease&&!(current instanceof Transition transition&&transition.owner==notice.lease))palette=(int)CombatAccess.call(pc,"currentPoisonPalette","(J)I",System.nanoTime());}}broadcast(pc,palette,reason);endSilence(pc,notice,Protocol.messageCode(311),"S_MESSAGE_CODE source-npc-silence-poison-cured=311");}
 static void cancellationNotice(ClientSession caster,ClientSession target,Object cancellation,List<ClientSession> recipients,long now,SilenceNotice notice){CombatAccess.call(caster,"publishPlayerMagicCancellation","("+C+"L"+P+"ClientSession$PlayerMagicCancellation;Ljava/util/List;J)V",target,cancellation,recipients,now);endSilence(target,notice,Protocol.messageCode(311),"S_MESSAGE_CODE source-npc-silence-poison-cured=311");}
 static void silenceDeathSend(ClientSession pc,byte[] packet,String reason,Scope scope){var c=scope==null?null:scope.find(pc);if(c!=null&&c.removal!=null){if(c.valid())c.removal.notice(packet,reason);return;}CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,reason);}
 static void silenceStartSend(ClientSession pc,byte[] packet,String reason,Scope scope){var c=scope==null?null:scope.find(pc);if(c!=null&&c.producerActive){c.initialSilence=new Palette(0,reason,pc,packet,0);return;}CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,reason);}
 private static void initialNotice(Context c){var notice=c.initialSilence;c.initialSilence=null;if(notice!=null&&c.valid())CombatAccess.call(c.pc,"sendEncrypted","([BLjava/lang/String;)V",notice.packet,notice.reason);}
 static void nativeRawOriginalBattle(ClientSession clientSession, int n, ClientSession clientSession2, Scope scope) {
        NativeCombatOrigin.Clock clock = NativeCombatOrigin.enterClock(clientSession, scope);
        try {
            if (!CombatPoisonLifecycle.tickAllowed(clientSession, scope)) {
                NativeCombatOrigin.leaveClock(clock);
                return;
            }
            Context context = CombatPoisonLifecycle.frozen(clientSession, scope);
            if (context == null) {
                CombatNpcPoison.battleTick(clientSession, n, clientSession2);
                NativeCombatOrigin.leaveClock(clock);
                return;
            }
            boolean bl = context.frozen.kind == ClassicWorldMonsterPoisonAttack.Kind.DAMAGE;
            Damage damage = CombatPoisonLifecycle.damage(context);
            try {
                CombatAccess.call(clientSession, "takePlayerPoisonDamage", "(ILlocal/lito381/server/ClientSession;)V", bl ? 5 : n, bl ? null : CombatPoisonLifecycle.liveCaster(context));
            }
            finally {
                CombatPoisonLifecycle.finished(context, damage);
            }
            NativeCombatOrigin.leaveClock(clock);
            return;
        }
        catch (Throwable throwable) {
            NativeCombatOrigin.leaveClock(clock);
            throw throwable;
        }
    }
 static Object worldNpc(ClientSession clientSession, int n, long l, Scope scope) {
        NativeCombatOrigin.Clock clock = NativeCombatOrigin.enterClock(clientSession, scope);
        try {
            Object object = CombatPoisonLifecycle.tickAllowed(clientSession, scope) ? CombatAccess.call(clientSession, "takeClassicWorldMonsterDamage", "(IJ)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;", n, CombatPoisonLifecycle.frozen(clientSession, scope) == null ? l : CombatPoisonLifecycle.object(clientSession, scope)) : null;
            NativeCombatOrigin.leaveClock(clock);
            return object;
        }
        catch (Throwable throwable) {
            NativeCombatOrigin.leaveClock(clock);
            throw throwable;
        }
    }
 static void worldPlayer(ClientSession clientSession, int n, ClientSession clientSession2, Scope scope) {
        NativeCombatOrigin.Clock clock = NativeCombatOrigin.enterClock(clientSession, scope);
        try {
            if (!CombatPoisonLifecycle.tickAllowed(clientSession, scope)) {
                NativeCombatOrigin.leaveClock(clock);
                return;
            }
            Context context = CombatPoisonLifecycle.frozen(clientSession, scope);
            if (context == null) {
                CombatAccess.call(clientSession, "takeClassicWorldPlayerPoisonDamage", "(ILlocal/lito381/server/ClientSession;)V", n, clientSession2);
                NativeCombatOrigin.leaveClock(clock);
                return;
            }
            ClientSession clientSession3 = CombatPoisonLifecycle.liveCaster(context);
            if (clientSession3 != null) {
                CombatAccess.call(clientSession, "takeClassicWorldPlayerPoisonDamage", "(ILlocal/lito381/server/ClientSession;)V", n, clientSession3);
            } else if (context.clock.caster != null) {
                CombatPoisonLifecycle.last(clientSession, null);
                CombatAccess.call(clientSession, "takeClassicWorldMonsterDamage", "(IJ)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;", n, context.clock.caster.object);
            }
            NativeCombatOrigin.leaveClock(clock);
            return;
        }
        catch (Throwable throwable) {
            NativeCombatOrigin.leaveClock(clock);
            throw throwable;
        }
    }

    static void battle(ClientSession clientSession, int n, ClientSession clientSession2, Scope scope) {
        NativeSourceRawDamage.poison(clientSession, n, clientSession2, scope);
    }
}
