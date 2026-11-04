// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/resurrection-lifetime/NativeResurrectionAnswerLife.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
/** One lease per pending question; no global actor map, timer or worker. */
final class NativeResurrectionAnswerLife {
 static final class Lease {
  final Object pending;final ClientSession caster;
  final NativePlayerCommit.Binding casterBinding,targetBinding;
  Lease(ClientSession target,Object pending){this.pending=pending;caster=(ClientSession)CombatAccess.field(pending,"caster");casterBinding=NativePlayerCommit.Binding.capture(caster);targetBinding=NativePlayerCommit.Binding.capture(target);}
  boolean valid(ClientSession target){return casterBinding.matches(caster)&&targetBinding.matches(target)&&!(boolean)CombatAccess.field(caster,NativeDeItemApplication.DISCONNECTED)&&!(boolean)CombatAccess.field(target,NativeDeItemApplication.DISCONNECTED);}
 }
 /** Executed immediately before every original volatile pendingResurrection store. */
 static void capture(ClientSession target,Object pending){target.nativeResurrectionAnswerLease=pending==null?null:new Lease(target,pending);}
 private static Object pending(ClientSession pc){return CombatAccess.field(pc,"pendingResurrection");}
 private static void clear(ClientSession pc,Object expected)throws IOException{
  NativeLightAction.commitPair(pc,pc,()->{if(pending(pc)==expected)pc.nativeRejectResurrectionQuestion();});
 }
 static void answer(ClientSession pc,Protocol.AskAnswer answer)throws IOException{
  Object expected=pending(pc);var lease=pc.nativeResurrectionAnswerLease;
  if(expected==null){NativeLightAction.commitPair(pc,pc,()->{if(pending(pc)==null)pc.nativeResurrectionAnswerBody(answer);});return;}
  if(lease==null||lease.pending!=expected||!lease.valid(pc)){clear(pc,expected);return;}
  boolean[] entered={false};
  NativeLightAction.commitPair(pc,lease.caster,()->{
   entered[0]=true;
   if(pending(pc)!=expected)return;
   if(!lease.valid(pc)){pc.nativeRejectResurrectionQuestion();return;}
   pc.nativeResurrectionAnswerBody(answer);
  });
  // A caster can retire/disconnect while the answer waits for its pair lock.
  // Clear only this question, under the receiver's current live receipt.
  if(!entered[0])clear(pc,expected);
 }
 private NativeResurrectionAnswerLife(){}
}
