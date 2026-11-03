package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.*;
/** A detached proc snapshot rides the original physical HP event, never a new job or action scope. */
final class NativeSourceDollAccepted {
 static final String P="local/lito381/server/";
 static final class Pending {
  final ClientSession caster,target;final NativeMagicDollEffects.AttackProc proc;final NativeSourceRawDamage.Guard guard;final AtomicBoolean claimed=new AtomicBoolean();
  Pending(ClientSession caster,ClientSession target,NativeMagicDollEffects.AttackProc proc,NativeSourceRawDamage.Guard guard){this.caster=caster;this.target=target;this.proc=proc;this.guard=guard;}
  boolean current(){return guard.before()&&!(boolean)CombatAccess.field(CombatAccess.field(caster,"hub"),"nativeStaticMotionClosed")&&NativeSourceDamageDisplay.n(caster,"playerHp")>0;}
 }
 static void physical(ClientSession target,int damage,ClientSession caster,NativeSourceKiringkuAdmission.Aim aim,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.RollOutcome maximum,BattleWeaponCritical.DamageOutcome multiplier,NativeMagicDollEffects.AttackProc proc)throws IOException{
  if(!NativeSourceDollCommit.enabled(caster)){NativeSourceDamageDisplay.physical(target,damage,caster,aim,weapon,maximum,multiplier);return;}
  var plan=NativeSourceDamageDisplay.capture(caster,target,damage,true,NativeSourceDamageDisplay.critical(weapon.weaponType(),maximum.effectEnabled(),multiplier.effectEnabled()),false);
  if(plan==null)plan=new NativeSourceDamageDisplay.Plan(NativeSourceDamageDisplay.Actor.capture(caster),NativeSourceDamageDisplay.Actor.capture(target),List.of());
  var source=new NativeSourceRawDamage.Member(caster,plan.caster.binding(),plan.caster.life());var victim=new NativeSourceRawDamage.Member(target,plan.target.binding(),plan.target.life());
  var guard=new NativeSourceRawDamage.Guard(victim,source,true,plan.owner);plan.nativeRawGuard=guard;plan.nativeSourceDollPending=new Pending(caster,target,proc,guard);final var hp=plan;
  try{NativeSourcePlayerActionLock.runPairOrdered(caster,target,()->{synchronized(hp.owner.admission){if(hp.owner.mode!=NativePlayerLifetime.Mode.OPEN||!guard.before()||NativeSourceDamageDisplay.n(target,"playerHp")<=0||(boolean)CombatAccess.field(caster,"observer")||(boolean)CombatAccess.field(target,"observer")||(boolean)CombatAccess.field(hp.hub,"nativeStaticMotionClosed")||aim!=null&&!NativeSourceKiringkuAdmission.valid(aim,caster,target))return;try{NativeLightAction.commitPair(caster,target,()->{if(guard.before())NativeSourceDamageDisplay.damage(target,damage,caster,true,false,0,hp);});}catch(IOException error){throw new UncheckedIOException(error);}}});}catch(UncheckedIOException error){throw error.getCause();}
 }
 /** Old post-submission doll calls survive for every other target and unloaded/WORLD paths. */
 static void submitted(ClientSession caster,NativeMagicDollEffects.AttackProc proc,long id,ClientSession target,BattleQuestEncounter.Snapshot quest,BattleTimeAttackEncounter.Snapshot time,SourceNpcPhysicalStats stats,NativeSourceKiringkuAdmission.Aim aim)throws IOException{
  if(target!=null&&NativeSourceDollCommit.enabled(caster))return;
  NativeSourceKiringkuAdmission.doll(caster,proc,id,target,quest,time,stats,aim);
 }
 /** Called only by the existing accepted completion, before physical figures; no vital lock is acquired here. */
 static void completed(NativePlayerCommit.Frame frame){
  var plan=frame.event.sourceDisplay();if(!frame.accepted||frame.event.world()||plan==null||!(plan.nativeSourceDollPending instanceof Pending pending))return;
  plan.nativeSourceDollPending=null;if(frame.pc!=pending.target||frame.casterSession!=pending.caster||!pending.claimed.compareAndSet(false,true))return;
  try{NativeSourcePlayerActionLock.runPairOrdered(pending.caster,pending.target,()->{synchronized(pending.guard.lifetime().admission){if(!pending.current())return;try{NativeLightAction.commitPair(pending.caster,pending.target,()->{
   if(!pending.current())return;
   // Source executes proc GFX and bonus on a fatal hit, but executeSkillProc skips a dead target before magic RNG.
   var proc=NativeSourceDamageDisplay.n(pending.target,"playerHp")<=0?new NativeMagicDollEffects.AttackProc(pending.proc.additionalDamage(),pending.proc.damageTriggers(),List.of()):pending.proc;
   CombatAccess.call(pending.caster,"applyNativeMagicDollAttackProcs","(L"+P+"NativeMagicDollEffects$AttackProc;JL"+P+"ClientSession;L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;L"+P+"SourceNpcPhysicalStats;)V",proc,pending.target.playerObjectId(),pending.target,null,null,null);
  });}catch(IOException failure){throw new UncheckedIOException(failure);}}});}
  catch(RuntimeException failure){NativeSourceNpcDamageDisplay.failed(pending.caster);}
 }
 private NativeSourceDollAccepted(){}
}
