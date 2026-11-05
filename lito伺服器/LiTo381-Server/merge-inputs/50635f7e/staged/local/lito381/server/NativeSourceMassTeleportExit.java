package local.lito381.server;

import java.io.IOException;

/** Original mass-teleport exit publication, carrying the first role to the cipher boundary. */
final class NativeSourceMassTeleportExit {
 record Proof(NativePlayerCommit.Binding binding,NativeSourceSelfFinalFrame.First first) {}

 static Object ingress(ClientSession pc,Object value) {
  if(value==null)return null;
  var first=NativeSourceSelfFinalFrame.first(pc,value);
  return first==null?Boolean.FALSE:first;
 }

 static Proof capture(ClientSession pc,NativePlayerCommit.Binding binding) {
  if(!binding.phase().equals("BATTLE"))return new Proof(binding,null);
  Object inherited=NativeLightAction.nativeSourceMassTeleportIngress(pc);
  if(inherited==Boolean.FALSE)return new Proof(binding,null);
  if(inherited instanceof NativeSourceSelfFinalFrame.First first)
   return new Proof(binding,first.source().binding().equals(binding)?first:null);
  // Direct callers have no native input scope. Read the existing first role without creating one.
  var participant=NativeSourceSelfFinalFrame.receiver(pc);
  if(participant==null||!participant.binding().equals(binding))return new Proof(binding,null);
  var frame=NativePlayerCommit.frame();
  if(frame!=null&&frame.accepted&&!frame.event.world()) {
   if(!(frame.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof accepted))return new Proof(binding,null);
   var p=accepted.target()!=null&&accepted.target().pc()==pc?accepted.target():accepted.source()!=null&&accepted.source().pc()==pc?accepted.source():null;
   if(p==null||!p.binding().equals(binding)||p.life()!=participant.life())return new Proof(binding,null);
   participant=new NativeSourceSelfFinalFrame.Participant(pc,binding,participant.timed(),p.life(),participant.observer(),p.dead());
  } else if(NativeAreaMaterialAction.current.get()!=null) {
   var context=NativeAreaMaterialAction.current.get();
   if(!(context.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot))return new Proof(binding,null);
   var p=snapshot.members().get(pc);
   if(p==null||!p.binding.equals(binding)||p.acceptedLife!=participant.life())return new Proof(binding,null);
   participant=new NativeSourceSelfFinalFrame.Participant(pc,binding,participant.timed(),p.acceptedLife,participant.observer(),p.dead);
  }
  var hub=(GameHub)CombatAccess.field(pc,"hub");
  Object raw=CombatAccess.field(hub,"sourcePlayerCommitLifetime");
  if(!(raw instanceof NativePlayerLifetime life))return new Proof(binding,null);
  var first=new NativeSourceSelfFinalFrame.First(participant,life,hub,CombatAccess.field(hub,"sourceItemApplication"),life.application);
  return new Proof(binding,NativeSourceSelfFinalFrame.current(first)?first:null);
 }

 static boolean current(Proof proof,ClientSession pc) {
  if(proof==null)return false;
  if(!proof.binding().phase().equals("BATTLE"))return proof.binding().matches(pc);
  return proof.first()!=null&&proof.first().source().pc()==pc&&proof.binding().equals(proof.first().source().binding())&&NativeSourceSelfFinalFrame.current(proof.first());
 }

 static boolean matches(NativePlayerCommit.Binding binding,ClientSession pc,Proof proof) {
  return proof!=null&&binding.equals(proof.binding())&&current(proof,pc);
 }

 static Runnable completion(ClientSession pc,Runnable action,Proof proof) {
  if(!current(proof,pc))return ()->{};
  var result=new Runnable[1];
  Object parent=NativePlayerActionScope.nativeSourcePairScopeRoot();
  var lock=pc.nativePlayerActionLock();
  boolean preheld=lock.isHeldByCurrentThread();
  boolean borrowed=false;
  // A nested third actor must never wait on this source while retaining the parent lock.
  if(!lock.isHeldByCurrentThread()&&NativeSourcePairThird.other(parent,pc,pc)) {
   if(!lock.tryLock())return ()->{};
   borrowed=true;
  }
  try {
   NativeSourcePlayerActionLock.run(pc,()->{
    if(!current(proof,pc))return;
    var publication=NativeRolePublication.completion(pc,action);
    if(parent!=null||preheld) {
     // Use the existing Batch/root drain when an outer mutation still owns an actor.
     try{NativeLightAction.commitSelf(pc,()->NativeLightAction.deferScoped(publication::run));}
     catch(IOException failure){throw new java.io.UncheckedIOException(failure);}
    } else result[0]=publication;
   });
  } finally {
   if(borrowed) {
    lock.unlock();
    if(NativePlayerActionScope.nativeSourcePairScopeRoot()==parent&&NativeSourcePairThird.registered(parent,pc))NativePlayerActionScope.unlocked(pc);
   }
  }
  return result[0]==null?()->{}:result[0];
 }

 static void send(ClientSession pc,byte[] packet,String note,Proof proof)throws IOException {
  if(!current(proof,pc))return;
  if(!proof.binding().phase().equals("BATTLE")) {pc.nativeSelfOriginalSend(packet,note);return;}
  NativeSourceSelfFinalFrame.send(new NativeSourceSelfFinalFrame.Output(proof.first(),proof.first().source(),packet,note));
 }

 private NativeSourceMassTeleportExit() {}
}
