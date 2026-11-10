package local.lito381.server;
import java.util.*;
/** Reuse first observer/life metadata for original accepted defense presentation only. */
final class NativeSourceDollDefenseAudience {
 static NativeSourceDollDefense.Receiver receiver(ClientSession pc,NativePlayerCommit.Binding original,NativeAreaMaterialAction.Context c){
  var list=NativeSourceDollAudience.copy(List.of(pc),c,c.pc);
  if(list instanceof NativeSourceDollAudience.Audience a){var first=a.receivers.get(0);return new NativeSourceDollDefense.Receiver(pc,first.binding(),first.nativeSourceDollAudienceProof);}
  return new NativeSourceDollDefense.Receiver(pc,original);
 }
 static boolean current(NativeSourceDollDefense.Output out){
  var receiver=out.receiver();Object value=receiver.nativeSourceDollDefenseAudienceProof;if(value==null)return true;
  if(!(value instanceof NativeSourceDollAudience.Life first)||first.life()==null||receiver.binding()==null)return false;
  var proof=out.proof();var pc=receiver.pc();Object expected=first.life();
  // Only this original defender's proof can substitute its original accepted-state death token.
  if(pc==proof.stamp.caster()&&Objects.equals(receiver.binding(),proof.binding)&&first.life()==proof.stamp.source().life())expected=proof.acceptedLife;
  return expected!=null&&first.hub()==proof.hub&&CombatAccess.field(pc,"hub")==first.hub()&&NativeSourceControlledDisplay.rawLife(pc)==expected&&(boolean)CombatAccess.field(pc,"observer")==first.observer();
 }
 static boolean allowFrame(ClientSession receiver,byte[] packet){
  var f=NativePlayerCommit.frame();if(f==null||!(f.nativeSourceDollDefenseOutput instanceof NativeSourceDollDefense.Output out)||out.receiver().nativeSourceDollDefenseAudienceProof==null)return true;
  return receiver==out.receiver().pc()&&packet!=null&&Arrays.equals(packet,out.packet())&&current(out);
 }
 private NativeSourceDollDefenseAudience(){}
}
