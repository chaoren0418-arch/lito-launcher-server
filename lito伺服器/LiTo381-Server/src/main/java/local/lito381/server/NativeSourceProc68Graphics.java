package local.lito381.server;
import java.io.IOException;import java.lang.ref.WeakReference;import java.util.*;
/** Graphics-only output after the original accepted custom-duration 68 status mutation. */
final class NativeSourceProc68Graphics {
 record Actors(NativeSourceAcceptedPlayers.Participant caster,NativeSourceAcceptedPlayers.Participant target,NativePlayerLifetime lifetime,GameHub hub,WeakReference<Object> casterStatus,WeakReference<Object> targetStatus){}
 record Output(Actors actors,ClientSession receiver,NativeSourceElementAudience.Receiver first,int gfx,byte[] packet,String note){Output{packet=packet.clone();}}
 static boolean actors(Actors a){return a!=null&&a.lifetime()!=null&&a.hub()!=null&&CombatAccess.field(a.hub(),"sourcePlayerCommitLifetime")==a.lifetime()&&a.casterStatus().get()!=null&&a.casterStatus().get()==CombatAccess.field(a.caster().pc(),"timedStatus")&&a.targetStatus().get()!=null&&a.targetStatus().get()==CombatAccess.field(a.target().pc(),"timedStatus")&&a.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(a.hub(),"nativeStaticMotionClosed")&&participant(a.caster(),a.hub())&&participant(a.target(),a.hub())&&!(boolean)CombatAccess.field(a.caster().pc(),"observer")&&!(boolean)CombatAccess.field(a.target().pc(),"observer");}
 static boolean participant(NativeSourceAcceptedPlayers.Participant p,GameHub hub){return p!=null&&NativeSourceControlledRoleFrame.current(p.binding(),p.pc())&&NativeSourceEquipmentProcLife.participant(p,hub);}
 static boolean visible(Actors a,ClientSession pc){if(pc==a.caster().pc())return true;if((boolean)CombatAccess.field(a.caster().pc(),"invisible"))return false;return Math.max(Math.abs((long)NativeSourceRawDamage.number(pc,"positionX")-NativeSourceRawDamage.number(a.caster().pc(),"positionX")),Math.abs((long)NativeSourceRawDamage.number(pc,"positionY")-NativeSourceRawDamage.number(a.caster().pc(),"positionY")))<=10;}
 static boolean current(Output out){if(out==null||out.gfx()<=0||!actors(out.actors()))return false;var a=out.actors();var r=out.first();var pc=out.receiver();Object status=r==null?null:r.status().get();return pc!=null&&status!=null&&status==CombatAccess.field(pc,"timedStatus")&&NativeSourceControlledRoleFrame.current(r.binding(),pc)&&CombatAccess.field(pc,"hub")==a.hub()&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&(boolean)CombatAccess.field(pc,"observer")==r.observer()&&NativeSourceControlledDisplay.rawLife(pc)==r.life()&&(r.observer()||r.life()!=null&&NativeSourceRawDamage.number(pc,"playerHp")>0)&&a.caster().pc().nativeSourceShowProcSpr&&visible(a,pc)&&Arrays.equals(out.packet(),Protocol.effectObject(a.target().binding().object(),out.gfx()));}
 static NativeSourceElementAudience.Receiver first(ClientSession pc,Actors a){for(var p:List.of(a.caster(),a.target()))if(p.pc()==pc)return new NativeSourceElementAudience.Receiver(p.binding(),p.life(),false,new WeakReference<>(CombatAccess.field(pc,"timedStatus")));return NativeSourceElementAudience.capture(pc);}
 static void graphics(ClientSession originalReceiver,byte[] original,String note,ClientSession caster,ClientSession target,NativeArmorProc.Rule rule){
  if(rule==null||rule.skill()!=68||rule.seconds()<=0||!NativeSourceEquipmentProcLife.enabled(caster)){NativePlayerCommit.safeAuxSend(originalReceiver,original,note);return;}
  var proof=NativeSourceEquipmentProcLife.proof(caster,target);if(proof==null||!NativeSourceEquipmentProcLife.held(proof)||!proof.current())return;
  var skill=NativeProcSkillCatalog.catalog().find(68);if(skill==null||skill.castGraphics()<=0||!caster.nativeSourceShowProcSpr)return;
  var a=new Actors(proof.caster(),proof.target(),proof.lifetime(),proof.hub(),new WeakReference<>(CombatAccess.field(caster,"timedStatus")),new WeakReference<>(CombatAccess.field(target,"timedStatus")));var outputs=new ArrayList<Output>();
  synchronized(a.hub()){
   if(!actors(a))return;
   var members=new LinkedHashSet<ClientSession>();members.add(caster);members.addAll(a.hub().roomMembers(a.caster().binding().room()));
   for(var pc:members)if(visible(a,pc)){var out=new Output(a,pc,first(pc,a),skill.castGraphics(),Protocol.effectObject(a.target().binding().object(),skill.castGraphics()),note);if(current(out))outputs.add(out);}
  }
  for(var out:outputs)NativeLightAction.deferScoped(()->send(out));
 }
 static void send(Output out){NativePlayerCommit.completionFrame(null,()->{var local=NativeAreaMaterialAction.current;var old=local.get();local.remove();try{out.receiver().nativeSourceProc68FinalFrame(out.packet().clone(),out.note(),out);}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(out.actors().lifetime());}finally{if(old==null)local.remove();else local.set(old);}});}
 static boolean allow(ClientSession pc,byte[] packet,Object value){return value instanceof Output out&&out.receiver()==pc&&packet!=null&&Arrays.equals(packet,out.packet())&&current(out);}
 private NativeSourceProc68Graphics(){}
}
