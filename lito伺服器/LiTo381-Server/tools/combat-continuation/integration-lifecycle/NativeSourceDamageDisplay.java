package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Source visual-only plans travel with the existing HP event, without a new TLS or HP lane. */
final class NativeSourceDamageDisplay {
 static final String P="local/lito381/server/",PC="L"+P+"ClientSession;",PLAN="L"+P+"NativeSourceDamageDisplay$Plan;";
 record Actor(ClientSession pc,NativePlayerCommit.Binding binding,Object life,int x,int y){
  static Actor capture(ClientSession pc){return pc==null?null:new Actor(pc,NativePlayerCommit.Binding.capture(pc),NativeSourceDamageDisplay.life(pc),n(pc,"positionX"),n(pc,"positionY"));}
  boolean valid(Object token){var state=CombatAccess.field(CombatAccess.field(pc,"timedStatus"),"sourcePoisonLifecycle");return NativeAcceptedFreezeMutation.roleFrame(binding,pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&n(pc,"positionX")==x&&n(pc,"positionY")==y&&state!=null&&CombatAccess.field(state,"sourceNpcOwner")==token;}
 }
 static final class Plan {
  final Actor caster,target;final NativePlayerLifetime owner;final GameHub hub;final List<NativeSourceDisplayPlan.Effect> effects;final long deadline;final AtomicBoolean published=new AtomicBoolean();volatile Object targetLife;volatile NativeSourceNpcDamageDisplay.Proof npc;volatile NativeSourceRawDamage.Guard nativeRawGuard;
  Plan(Actor caster,Actor target,List<NativeSourceDisplayPlan.Effect> effects){this.caster=caster;this.target=target;this.effects=List.copyOf(effects);hub=(GameHub)CombatAccess.field(caster.pc,"hub");owner=NativePlayerLifetime.of(hub);deadline=NativePlayerLifetime.deadline();targetLife=target==null?null:target.life;}
  boolean valid(){return !owner.nativeSourceDisplayJobs.closed&&owner.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")&&caster.valid(caster.life)&&n(caster.pc,"playerHp")>0&&(target==null||target.valid(targetLife))&&(npc==null||npc.current());}
 }
 /** One bounded job set is owned by each existing application lifetime; close releases captured actors. */
 static final class Jobs {
  final NativePlayerLifetime owner;final Set<Job> pending=Collections.newSetFromMap(new IdentityHashMap<>());volatile boolean closed;long dropped,failed;
  Jobs(NativePlayerLifetime owner){this.owner=owner;}
  void submit(Plan plan){
   long remaining=plan.deadline==0?0:plan.deadline-System.nanoTime();if(remaining<=0){send(plan);return;}
   synchronized(owner.admission){if(closed||owner.mode!=NativePlayerLifetime.Mode.OPEN||pending.size()>=256){dropped++;return;}var job=new Job(this,plan);pending.add(job);try{job.timer=owner.retry.schedule(job::dispatch,remaining,TimeUnit.NANOSECONDS);}catch(RejectedExecutionException failure){dropped++;job.release();}}
  }
  void close(){synchronized(owner.admission){closed=true;for(var job:List.copyOf(pending))job.release();}}
 }
 static final class Job implements Runnable {
  final Jobs jobs;Plan plan;Future<?> timer;boolean released;
  Job(Jobs jobs,Plan plan){this.jobs=jobs;this.plan=plan;}
  void dispatch(){synchronized(jobs.owner.admission){if(released)return;if(jobs.closed||jobs.owner.mode!=NativePlayerLifetime.Mode.OPEN){jobs.dropped++;release();return;}try{jobs.owner.workers.execute(this);}catch(RejectedExecutionException failure){jobs.dropped++;release();}}}
  public void run(){Plan output;synchronized(jobs.owner.admission){if(released)return;output=plan;}try{send(output);}finally{release();}}
  void release(){synchronized(jobs.owner.admission){if(released)return;released=true;plan=null;if(timer!=null){timer.cancel(false);timer=null;}jobs.pending.remove(this);}}
 }
 static int n(ClientSession pc,String field){return(int)CombatAccess.field(pc,field);}
 static Object life(ClientSession pc){return NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pc,"timedStatus"),true);}
 static boolean shown(ClientSession caster){return caster!=null&&caster.nativeSourceShowDamage&&n(caster,"playerHp")>0;}
 static Plan capture(ClientSession caster,ClientSession target,int damage,boolean physical,boolean critical,boolean miss){
  if(!CombatRules.loaded()||caster==null)return null;var settings=NativeSourceOtherControls.settings();var effects=physical?NativeSourceDisplayPlan.physical(settings,caster.playerObjectId(),target==null?null:target.playerObjectId(),shown(caster),n(caster,"playerHp")<=0,damage,critical,miss):NativeSourceDisplayPlan.magic(settings,caster.playerObjectId(),target==null?null:target.playerObjectId(),shown(caster),n(caster,"playerHp")<=0,damage);
  return effects.isEmpty()?null:new Plan(Actor.capture(caster),Actor.capture(target),effects);
 }
 static Plan captureNpc(ClientSession caster,NativeSourceNpcDamageDisplay.Proof proof,long target,int damage,boolean physical,boolean critical){
  if(!CombatRules.loaded()||caster==null)return null;var settings=NativeSourceOtherControls.settings();var effects=physical?NativeSourceDisplayPlan.physical(settings,caster.playerObjectId(),target,shown(caster),n(caster,"playerHp")<=0,damage,critical,false):NativeSourceDisplayPlan.magic(settings,caster.playerObjectId(),target,shown(caster),n(caster,"playerHp")<=0,damage);if(effects.isEmpty())return null;var plan=new Plan(Actor.capture(caster),null,effects);plan.npc=proof;return plan;
 }
 static void damage(ClientSession target,int damage,ClientSession caster,boolean earth,boolean suppress,long terminal,Plan plan)throws IOException{
  if(plan==null){CombatAccess.call(target,"takePlayerDamage","(I"+PC+"ZZJ)V",damage,caster,earth,suppress,terminal);return;}
  CombatAccess.call(target,"nativeSourceDisplayDamage","(I"+PC+"ZZJ"+PLAN+")V",damage,caster,earth,suppress,terminal,plan);
 }
 static void physical(ClientSession target,int damage,ClientSession caster,NativeSourceKiringkuAdmission.Aim aim,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.RollOutcome maximum,BattleWeaponCritical.DamageOutcome multiplier)throws IOException{
  boolean critical=critical(weapon.weaponType(),maximum.effectEnabled(),multiplier.effectEnabled());var plan=capture(caster,target,damage,true,critical,false);NativeSourceKiringkuAdmission.owned(aim,caster,target,()->damage(target,damage,caster,true,false,0,plan));
 }
 static boolean critical(int weaponType,boolean maximum,boolean multiplier){return weaponType==4||weaponType==13||weaponType==10?maximum&&!multiplier:maximum||multiplier;}
 /** Called at the original accepted-physical-hit guard; misses never submit an HP event. */
 static void miss(ClientSession caster,ClientSession target,boolean hit){if(hit)return;publish(capture(caster,target,0,true,false,true));}
 static void magic(ClientSession target,int damage,ClientSession caster)throws IOException{if(CombatRules.loaded()&&damage<0){CombatPlayerRecovery.battle(target,damage,caster,false);return;}damage(target,damage,caster,true,false,0,capture(caster,target,damage,false,false,false));}
 static Object invokeMagic(Object target,String method,String descriptor,Object[] args){if(target instanceof ClientSession pc&&method.equals("takePlayerDamage")&&descriptor.equals("(I"+PC+")V")&&args.length==2){try{magic(pc,(int)args[0],(ClientSession)args[1]);return null;}catch(IOException error){throw new UncheckedIOException(error);}}return CombatAccess.call(target,method,descriptor,args);}
 /** Death token retirement belongs to the original accepted HP completion, never to a visual timer. */
 static void acceptedState(NativePlayerCommit.Frame frame){NativeSourceRawDamage.accepted(frame);var plan=frame.event.sourceDisplay();if(plan!=null&&frame.accepted&&frame.fatal&&plan.target!=null&&plan.target.pc==frame.pc)plan.targetLife=life(frame.pc);}
 static NativeAcceptedHpCompletion enqueue(NativePlayerCommit.Frame frame,NativePlayerVitalCompletions.Receipt receipt){acceptedState(frame);return NativeSourceLightningHp.enqueue(frame,receipt);}
 static void accepted(NativePlayerCommit.Frame frame){if(!frame.accepted)return;try{publish(frame.event.sourceDisplay());}catch(RuntimeException failure){var plan=frame.event.sourceDisplay();if(plan!=null)synchronized(plan.owner.admission){plan.owner.nativeSourceDisplayJobs.failed++;}}}
 static void publish(Plan plan){if(plan!=null&&!plan.effects.isEmpty()&&plan.published.compareAndSet(false,true))plan.owner.nativeSourceDisplayJobs.submit(plan);}
 /** One receiver monitor covers the ordered batch. Final validation reads existing volatile role/life/pose fields only. */
 static void send(Plan plan){if(plan==null)return;try{synchronized(plan.caster.pc){for(var effect:plan.effects){if(!plan.valid())return;plan.caster.pc.nativeSelfOriginalSend(Protocol.effectObject(effect.object(),effect.sprite()),"S_EFFECT Source damage display");}}}catch(IOException|RuntimeException failure){synchronized(plan.owner.admission){plan.owner.nativeSourceDisplayJobs.failed++;}}}
 static void closing(NativePlayerLifetime owner){owner.nativeSourceDisplayJobs.close();}
 static boolean toggle(ClientSession pc,String action)throws IOException{if(!CombatRules.loaded()||!action.equalsIgnoreCase("dmg_spr"))return false;NativeLightAction.commitSelf(pc,()->{pc.nativeSourceShowDamage=!pc.nativeSourceShowDamage;CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",Protocol.messageCode(166,"傷害顯示: "+(pc.nativeSourceShowDamage?"開啟":"關閉")),"S_MESSAGE_CODE Source dmg_spr");});return true;}
 private NativeSourceDamageDisplay(){}
}
