package local.lito381.server;
import java.io.IOException;import java.lang.ref.WeakReference;import java.util.*;
/** Dedicated Source ICON_I2H after original custom-duration status acceptance; not a full status sync. */
final class NativeSourceProc68Icon {
 record Proof(NativeSourceAcceptedPlayers.Participant target,NativePlayerLifetime lifetime,GameHub hub,WeakReference<Object> status,WeakReference<BattleAbilityStatus> ability,Object lease,long expiry,int seconds){}
 record Output(Proof proof,byte[] packet,String note){Output{packet=packet.clone();}}
 static byte[] packet(int seconds){return new Protocol.Writer().c(Protocol.S_PACKET_BOX).c(40).h(seconds).bytes();}
 static boolean current(Output out){if(out==null||out.proof()==null)return false;var p=out.proof();var pc=p.target().pc();var ability=p.ability().get();Object status=p.status().get();if(p.lease()==null||ability==null||status==null||p.seconds()<=0||p.seconds()>65535||CombatAccess.field(pc,"timedStatus")!=status||CombatAccess.field(pc,"abilityStatus")!=ability||CombatAccess.field(p.hub(),"sourcePlayerCommitLifetime")!=p.lifetime()||p.lifetime().mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(p.hub(),"nativeStaticMotionClosed")||(boolean)CombatAccess.field(pc,"observer")||!NativeSourceControlledRoleFrame.current(p.target().binding(),pc)||!NativeSourceEquipmentProcLife.participant(p.target(),p.hub()))return false;synchronized(ability){return ability.nativeSourceProc68IconLease==p.lease()&&(long)CombatAccess.field(ability,"immuneToHarmExpiresAtNanos")==p.expiry()&&p.expiry()>System.nanoTime()&&Arrays.equals(out.packet(),packet(p.seconds()));}}
 static void output(ClientSession originalReceiver,byte[] original,String note,ClientSession caster,ClientSession target,NativeArmorProc.Rule rule){
  NativeSourceProc68Graphics.graphics(originalReceiver,original,note,caster,target,rule);
  if(rule==null||rule.skill()!=68||rule.seconds()<=0||!NativeSourceEquipmentProcLife.enabled(caster))return;
  var accepted=NativeSourceEquipmentProcLife.proof(caster,target);if(accepted==null||!NativeSourceEquipmentProcLife.held(accepted)||!accepted.current())return;
  var ability=(BattleAbilityStatus)CombatAccess.field(target,"abilityStatus");Output out;
  synchronized(ability){var p=new Proof(accepted.target(),accepted.lifetime(),accepted.hub(),new WeakReference<>(CombatAccess.field(target,"timedStatus")),new WeakReference<>(ability),ability.nativeSourceProc68IconLease,(long)CombatAccess.field(ability,"immuneToHarmExpiresAtNanos"),rule.seconds());out=new Output(p,packet(rule.seconds()),"S_PACKETBOX ICON_I2H proc68 seconds="+rule.seconds());}
  if(current(out))NativeLightAction.deferScoped(()->send(out));
 }
 static void send(Output out){NativePlayerCommit.completionFrame(null,()->{var local=NativeAreaMaterialAction.current;var old=local.get();local.remove();try{out.proof().target().pc().nativeSourceProc68IconFinalFrame(out.packet().clone(),out.note(),out);}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(out.proof().lifetime());}finally{if(old==null)local.remove();else local.set(old);}});}
 static boolean allow(ClientSession pc,byte[] packet,Object value){return value instanceof Output out&&out.proof().target().pc()==pc&&packet!=null&&Arrays.equals(packet,out.packet())&&current(out);}
 private NativeSourceProc68Icon(){}
}
