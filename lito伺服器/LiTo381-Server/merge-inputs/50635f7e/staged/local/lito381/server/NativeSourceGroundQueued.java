package local.lito381.server;
import java.util.*;
/** Detached ground proof on the original raw HP plan; never retains the producing Context/Action. */
final class NativeSourceGroundQueued {
 static NativeSourceDamageDisplay.Plan bind(NativeSourceDamageDisplay.Plan plan,ClientSession target,ClientSession caster){
  var c=NativeAreaMaterialAction.current.get();
  if(c!=null&&c.pc==target&&c.nativeSourceGroundProof instanceof NativeSourceGroundOrigin.PulseProof p&&p.target()!=null&&p.source()!=null&&p.target().pc()==target&&p.source().pc()==caster){
   var action=NativeSourceRawDamage.current();if(action!=null&&action.c==c)plan.nativeSourceGroundQueuedProof=p;
  }
  return plan;
 }
 static boolean sameEntity(NativeSourceGroundOrigin.Entity e){return e!=null&&e.actual()!=null&&e.engine()!=null&&e.actual().getClass().getName().equals("local.lito381.server.BattleGroundEffects$Lease")&&NativeSourceGroundOrigin.same(e,e.engine(),e.actual())&&CombatAccess.field(e.actual(),"nativeSourceGroundEntity")==e;}
 /** Tuple equality is not role authorization; role() separately verifies actual registered actors. */
 static boolean matches(NativeSourceGroundOrigin.Actor actor,ClientSession pc,NativePlayerCommit.Binding binding,Object life){return actor!=null&&life!=null&&actor.pc()==pc&&Objects.equals(actor.binding(),binding)&&actor.life()==life;}
 static boolean role(NativeSourceGroundOrigin.Actor actor,NativeSourceGroundOrigin.Scene scene,Object life){
  var pc=actor==null?null:actor.pc();return pc!=null&&life!=null&&actor.binding()!=null&&NativeAcceptedFreezeMutation.roleFrame(actor.binding(),pc)&&CombatAccess.field(pc,"hub")==scene.hub()&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&!(boolean)CombatAccess.field(pc,"observer")&&"BATTLE".equals(String.valueOf(CombatAccess.field(pc,"phase")))&&(long)CombatAccess.field(pc,"currentRoomId")==scene.id()&&NativeSourceControlledDisplay.rawLife(pc)==life;
 }
 static boolean scene(NativeSourceGroundOrigin.PulseProof p,NativeSourceDamageDisplay.Plan plan){
  if(p==null||p.entity()==null||p.source()==null||p.target()==null||p.entity().scene()==null||p.entity().creator()==null)return false;
  var e=p.entity();var s=e.scene();return s.hub()!=null&&plan.hub==s.hub()&&plan.owner.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(s.hub(),"nativeStaticMotionClosed")&&sameEntity(e)&&p.source().pc()==e.creator().pc()&&Objects.equals(p.source().binding(),e.creator().binding());
 }
 static NativeSourceGroundOrigin.PulseProof proof(NativeSourceDamageDisplay.Plan plan){return plan!=null&&plan.nativeSourceGroundQueuedProof instanceof NativeSourceGroundOrigin.PulseProof p?p:null;}
 static boolean namespace(ClientSession target,ClientSession caster,NativePlayerCommit.Event event,NativeSourceDamageDisplay.Plan plan,NativeSourceGroundOrigin.PulseProof p){
  var g=plan.nativeRawGuard;
  return p.source()!=null&&p.target()!=null&&target!=null&&caster!=null&&target!=caster&&p.target().pc()==target&&p.source().pc()==caster&&!event.world()&&!event.earth()&&event.suppress()&&event.damage()>0&&g!=null&&g.source()!=null&&matches(p.target(),g.target().pc,g.target().binding,g.target().life)&&matches(p.source(),g.source().pc,g.source().binding,g.source().life)&&Objects.equals(event.target(),p.target().binding())&&Objects.equals(event.caster(),p.source().binding());
 }
 static boolean ready(ClientSession pc,NativePlayerCommit.Event event,ClientSession resolvedCaster){
  var plan=event.sourceDisplay();var p=proof(plan);
  return p==null||namespace(pc,resolvedCaster,event,plan,p)&&scene(p,plan)&&role(p.source(),p.entity().scene(),p.source().life())&&role(p.target(),p.entity().scene(),p.target().life());
 }
 static boolean early(ClientSession pc,NativePlayerCommit.Event event,ClientSession resolvedCaster){return ready(pc,event,resolvedCaster);}
 static boolean beforeEvent(ClientSession pc,NativePlayerCommit.Event event,ClientSession resolvedCaster,boolean earlyAllowed){
  if(!earlyAllowed||!ready(pc,event,resolvedCaster))return false;
  return NativeSourceRawDamage.beforeEvent(pc,event);
 }
 static boolean validFrame(ClientSession pc){
  var frame=NativePlayerCommit.frame();if(frame==null)return true;var plan=frame.event.sourceDisplay();var p=proof(plan);if(p==null)return true;
  if(frame.pc!=pc||!namespace(pc,frame.casterSession,frame.event,plan,p)||!scene(p,plan))return false;
  // The original acceptedState updates this exact Member's acceptedLife while still under the HP lock.
  return role(p.source(),p.entity().scene(),p.source().life())&&role(p.target(),p.entity().scene(),plan.nativeRawGuard.target().acceptedLife);
 }
 private NativeSourceGroundQueued(){}
}
