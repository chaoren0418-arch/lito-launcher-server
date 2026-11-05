package local.lito381.server;
import java.util.*;
/** Existing accepted PC proc path for Source custom-duration scalar effects, without cast costs or new actor authority. */
final class NativeSourceProcScalarSkills {
 static boolean supports(int id){if(NativeSourceProcScalarExtras.supports(id))return true;return id==31||id==32||id==71||id==78;}
 static String expiry(int id){String extra=NativeSourceProcScalarExtras.expiry(id);if(extra!=null)return extra;return switch(id){case 31->"counterMagicExpiresAtNanos";case 32->"meditationExpiresAtNanos";case 71->"decayPotionExpiresAtNanos";case 78->"absoluteBarrierExpiresAtNanos";default->throw new IllegalArgumentException("unsupported Source scalar skill "+id);};}
 static NativeSourceEquipmentProcLife.Proof capture(ClientSession caster,ClientSession target){
  var f=NativePlayerCommit.frame();if(f==null||f.pc!=caster||!f.accepted||f.event.world()||f.queue==null||!(f.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof accepted))return null;
  var source=NativeSourceEquipmentProcLife.select(accepted,caster);var selected=NativeSourceEquipmentProcLife.select(accepted,target);if(source==null||selected==null||source.binding()==null||selected.binding()==null||!"BATTLE".equals(source.binding().phase())||!"BATTLE".equals(selected.binding().phase())||source.binding().room()!=selected.binding().room())return null;
  Object h=CombatAccess.field(caster,"hub");if(!(h instanceof GameHub hub))return null;Object value=CombatAccess.field(hub,"sourcePlayerCommitLifetime");if(!(value instanceof NativePlayerLifetime life)||CombatAccess.field(f.queue,"life")!=life)return null;
  return new NativeSourceEquipmentProcLife.Proof(source,selected,accepted,life,hub);
 }
 static boolean catalogCurrent(NativeProcSkillCatalog.Catalog catalog){try{return NativeProcSkillCatalog.catalog()==catalog;}catch(IllegalStateException missing){return false;}}
 static boolean current(NativeSourceEquipmentProcLife.Proof p,BattleTimedStatus timed,NativeProcSkillCatalog.Catalog catalog){
  var f=NativePlayerCommit.frame();return p!=null&&f!=null&&f.pc==p.caster().pc()&&f.accepted&&!f.event.world()&&f.nativeSourceAcceptedPlayers==p.accepted()&&catalogCurrent(catalog)&&CombatRules.loaded()&&CombatAccess.field(p.hub(),"sourcePlayerCommitLifetime")==p.lifetime()&&p.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(p.hub(),"nativeStaticMotionClosed")&&CombatAccess.field(p.target().pc(),"timedStatus")==timed&&CombatAccess.field(p.caster().pc(),"timedStatus")!=null&&!(boolean)CombatAccess.field(p.caster().pc(),"observer")&&!(boolean)CombatAccess.field(p.target().pc(),"observer")&&NativeSourceEquipmentProcLife.held(p)&&NativeSourceEquipmentProcLife.participant(p.caster(),p.hub())&&NativeSourceEquipmentProcLife.participant(p.target(),p.hub());
 }
 static Object state(BattleTimedStatus timed){return CombatAccess.field(timed,"sourcePoisonLifecycle");}
 static boolean queueCurrent(NativePlayerCommit.Binding binding,ClientSession pc,BattleTimedStatus timed,NativeSourceEquipmentProcLife.Proof proof,NativeProcSkillCatalog.Catalog catalog){return proof!=null&&pc==proof.target().pc()&&proof.target().binding().equals(binding)&&current(proof,timed,catalog);}
 static Object queueValue(Object queue,NativeSourceEquipmentProcLife.Proof proof){return queue!=null&&CombatAccess.field(queue,"life")==proof.lifetime()&&!Boolean.TRUE.equals(CombatAccess.field(queue,"closed"))&&CombatAccess.field(proof.hub(),"sourcePlayerCommitLifetime")==proof.lifetime()&&proof.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(proof.hub(),"nativeStaticMotionClosed")?queue:null;}
 static void activate(BattleTimedStatus timed,int id,long now,int seconds){if(NativeSourceProcScalarExtras.activate(timed,id,now,seconds))return;switch(id){case 31->timed.activateCounterMagic(now,seconds);case 32->timed.activateMeditation(now,seconds);case 71->timed.nativeSourceProcActivateDecayPotion(now,seconds);case 78->timed.activateAbsoluteBarrier(now,seconds);default->throw new IllegalArgumentException("unsupported Source scalar skill "+id);}}
 static boolean handle(ClientSession caster,ClientSession target,NativeArmorProc.Rule rule){
  if(caster==null||target==null||rule==null||!supports(rule.skill())||rule.seconds()<=0||!NativeSourceEquipmentProcLife.enabled(caster))return false;
  var catalog=NativeProcSkillCatalog.catalog();if(catalog.healing(rule.skill()))return false;
  var p=capture(caster,target);if(p==null||!NativeSourceEquipmentProcLife.held(p)||!p.current())return true;
  Object state=CombatAccess.field(target,"timedStatus");if(!(state instanceof BattleTimedStatus timed))return true;
  var mutation=NativeSourceEquipmentProcLife.guarded(p,()->{synchronized(timed){if(!current(p,timed,catalog))return false;int seconds=rule.seconds();long now=System.nanoTime(),previous=(long)CombatAccess.field(timed,expiry(rule.skill()));boolean active=previous>now;if(rule.selfSkill()&&active)return false;if(seconds>65535)throw new IllegalArgumentException("invalid native Source scalar duration");if(NativeSourceProcTimedRefresh.replace(active,NativeSourceIcons1Projection.seconds(previous,now),seconds*1000))activate(timed,rule.skill(),now,seconds);return true;}});
  if(!NativePlayerCommit.nativeSourceScalarMutateOwned(target,p.target().binding(),mutation,timed,p,catalog))return true;
  graphics(p,catalog,rule.skill());var output=NativeSourceProcScalarOutput.capture(p,catalog,rule.skill(),rule.seconds());if(output!=null)NativeSourceTargetIconBatch.deferTarget(()->NativeSourceProcScalarOutput.send(output),p.target());return true;
 }
 static void graphics(NativeSourceEquipmentProcLife.Proof p,NativeProcSkillCatalog.Catalog catalog,int skill){
  var source=catalog.find(skill);if(source==null||source.castGraphics()<=0||!p.caster().pc().nativeSourceShowProcSpr)return;
  var a=new NativeSourceProc68Graphics.Actors(p.caster(),p.target(),p.lifetime(),p.hub(),new java.lang.ref.WeakReference<>(CombatAccess.field(p.caster().pc(),"timedStatus")),new java.lang.ref.WeakReference<>(CombatAccess.field(p.target().pc(),"timedStatus")));
  var outputs=new ArrayList<NativeSourceProc68Graphics.Output>();synchronized(p.hub()){if(!NativeSourceProc68Graphics.actors(a))return;var members=new LinkedHashSet<ClientSession>();members.add(p.caster().pc());members.addAll(p.hub().roomMembers(p.caster().binding().room()));for(var pc:members){if(!NativeSourceProc68Graphics.visible(a,pc))continue;var first=NativeSourceProc68Graphics.first(pc,a);if(first==null)continue;var out=new NativeSourceProc68Graphics.Output(a,pc,first,source.castGraphics(),Protocol.effectObject(p.target().binding().object(),source.castGraphics()),"Source scalar proc graphics skill="+skill);if(NativeSourceProc68Graphics.current(out))outputs.add(out);}}
  for(var out:outputs)NativeLightAction.deferScoped(()->NativeSourceProc68Graphics.send(out));
 }
 private NativeSourceProcScalarSkills(){}
}
