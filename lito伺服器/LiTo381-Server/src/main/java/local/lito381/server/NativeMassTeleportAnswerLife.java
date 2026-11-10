// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/mass-teleport-lifetime/NativeMassTeleportAnswerLife.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerActionScope;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSourceManualScope;
import local.lito381.server.Protocol;
/** Bounded per-question lease. No global player map, repeating timer or database write. */
final class NativeMassTeleportAnswerLife {
    static Object token(ClientSession clientSession) {
        Object object;
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.lock(reentrantLock, clientSession);
        try {
            if (clientSession.nativeMassTeleportRoleToken == null) {
                clientSession.nativeMassTeleportRoleToken = new Object();
            }
            object = clientSession.nativeMassTeleportRoleToken;
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.unlock(reentrantLock, clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.unlock(reentrantLock, clientSession, ticket, null);
        return object;
    }
 static void retireRole(ClientSession pc){pc.nativeMassTeleportRoleToken=new Object();pc.nativeRejectMassTeleportQuestion();}
 static final class Lease {
  final Object pending,casterRole,targetRole;final ClientSession caster;
  final NativePlayerCommit.Binding casterBinding,targetBinding;
  Lease(ClientSession target,Object pending){this.pending=pending;caster=(ClientSession)CombatAccess.field(pending,"caster");casterRole=token(caster);targetRole=token(target);casterBinding=NativePlayerCommit.Binding.capture(caster);targetBinding=NativePlayerCommit.Binding.capture(target);}
  boolean valid(ClientSession target){
   if(casterRole!=caster.nativeMassTeleportRoleToken||targetRole!=target.nativeMassTeleportRoleToken||!targetBinding.matches(target)||(boolean)CombatAccess.field(caster,NativeDeItemApplication.DISCONNECTED)||(boolean)CombatAccess.field(target,NativeDeItemApplication.DISCONNECTED))return false;
   if(!String.valueOf(CombatAccess.field(pending,"originPhase")).equals("WORLD"))return casterBinding.matches(caster);
   var now=NativePlayerCommit.Binding.capture(caster);var destination=(Protocol.PortalDestination)CombatAccess.field(pending,"destination");
   // A valid caster may already have completed this spell's cross-map portal.
   boolean identity=now.object()==casterBinding.object()&&now.source().equals(casterBinding.source())&&now.account().equals(casterBinding.account())&&now.selected().equals(casterBinding.selected())&&now.phase().equals("WORLD")&&now.room()==casterBinding.room();
   boolean origin=now.map()==casterBinding.map()&&now.instance()==casterBinding.instance();
   boolean arrived=destination!=null&&now.map()==destination.mapId()&&now.instance()==(destination.mapId()==casterBinding.map()?casterBinding.instance():-1);
   return identity&&(origin||arrived);
  }
 }
 static void capture(ClientSession target,Object pending){target.nativeMassTeleportAnswerLease=pending==null?null:new Lease(target,pending);}
 private static Object pending(ClientSession pc){return CombatAccess.field(pc,"pendingMassTeleport");}
    private static void clear(ClientSession clientSession, Object object) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.lock(reentrantLock, clientSession);
        try {
            if (NativeMassTeleportAnswerLife.pending(clientSession) == object) {
                clientSession.nativeRejectMassTeleportQuestion();
            }
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.unlock(reentrantLock, clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.unlock(reentrantLock, clientSession, ticket, null);
    }
 static void answer(ClientSession pc,Protocol.AskAnswer answer)throws IOException{
  Object expected=pending(pc);var lease=pc.nativeMassTeleportAnswerLease;
  if(expected==null){NativeLightAction.commitPair(pc,pc,()->{if(pending(pc)==null)pc.nativeMassTeleportAnswerBody(answer);});return;}
  if(lease==null||lease.pending!=expected||!lease.valid(pc)){clear(pc,expected);return;}
  boolean[] entered={false};
  NativeLightAction.commitPair(pc,lease.caster,()->{
   entered[0]=true;if(pending(pc)!=expected)return;
   if(!lease.valid(pc)){pc.nativeRejectMassTeleportQuestion();return;}
   pc.nativeMassTeleportAnswerBody(answer);
  });
  if(!entered[0])clear(pc,expected);
 }
 static void publish(ClientSession receiver,byte[] data,String note){
  var binding=NativePlayerCommit.Binding.capture(receiver);byte[] frozen=data.clone();
  NativeLightAction.finishScoped((accepted,failure)->{try{NativeLightAction.commitPair(receiver,receiver,()->{
   if(!binding.matches(receiver))return;
   var output=NativeRolePublication.completion(receiver,()->{if(binding.matches(receiver))try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException e){throw new UncheckedIOException(e);}});
   NativeLightAction.finishScoped((published,publicationFailure)->output.run());
  });}catch(IOException e){throw new UncheckedIOException(e);}});
 }
 private NativeMassTeleportAnswerLife(){}
}
