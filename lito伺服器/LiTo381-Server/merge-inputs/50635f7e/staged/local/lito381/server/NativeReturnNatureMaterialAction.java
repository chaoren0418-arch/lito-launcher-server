// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/return-nature-material/NativeReturnNatureMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Bind Return to Nature before the native dispatcher can wait on a player lock. */
final class NativeReturnNatureMaterialAction {
 private record Target(ClientSession caster,ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  BattleCompanion owned(){return pet==null?null:(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",stamp.companion);}
  boolean valid(){return pet==null||stamp.master.matches(owner)&&!(boolean)CombatAccess.field(owner,NativeDeItemApplication.DISCONNECTED)&&owned()==pet&&NativeCompanionCallbacks.capture(stamp.master,pet).life==stamp.life;}
  boolean canPublish(){if(pet==null)return true;var live=owned();return stamp.master.matches(owner)&&!(boolean)CombatAccess.field(owner,NativeDeItemApplication.DISCONNECTED)&&(live==null||live==pet);}
 }
 private static final ThreadLocal<Target> ingress=new ThreadLocal<>(),current=new ThreadLocal<>();
 private static Target target(ClientSession pc,byte[] packet){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");boolean world=String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  for(var owner:NativeCompanionNegative.members(players,pc)){
   List<BattleCompanion.Snapshot> snapshots=world?NativeCompanionTargets.snapshots(owner,pc):owner.controlledCompanionSnapshots();
   for(var snapshot:snapshots)if(snapshot.objectId()==request.targetObjectId()){
    var pet=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",request.targetObjectId());
    if(pet!=null)return new Target(pc,owner,pet,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));
   }
  }return new Target(pc,pc,null,null);
 }
 static void nativeSceneOriginalDispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  if(Protocol.spellSelector(packet).skillId()!=145||(!phase.equals("BATTLE")&&!phase.equals("WORLD"))||pc.selectedCharacterName().isBlank()){original.run();return;}
  var prior=ingress.get();ingress.set(target(pc,packet));try{original.run();}finally{if(prior==null)ingress.remove();else ingress.set(prior);}
 }
 static void nativeSceneOriginalCast(ClientSession pc,byte[] packet,BattleSpellCatalog.ReturnToNatureSpell spell,int roll)throws IOException{
  var inherited=ingress.get();var target=inherited!=null&&inherited.caster==pc?inherited:target(pc,packet);ingress.remove();
  try{NativeLightAction.commitPair(pc,target.owner,()->{
   if(!target.valid())return;var prior=current.get();current.set(target);
   try{pc.nativeMaterialReturnNatureBody(packet,spell,roll);}finally{if(prior==null)current.remove();else current.set(prior);}
  });}finally{if(inherited==null)ingress.remove();else ingress.set(inherited);}
 }
 static boolean nativeSceneOriginalAdmit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){
  if(skill!=145)throw new IllegalStateException("unreviewed Return to Nature skill");var target=current.get();
  if(target==null||target.caster!=pc||target.owner!=owner||!target.valid())return false;
  return NativeLightAction.admitPair(pc,owner,skill,action,reuse);
 }
 static boolean nativeSummonOriginalCaptureOutput(ClientSession receiver,byte[] plain,String note){
  var target=current.get();if(target==null){current.remove();return false;}
  if(!note.contains("Return-to-Nature")||!(note.startsWith("S_EFFECT ")||note.startsWith("S_REMOVE_OBJECT ")||note.startsWith("S_PUT_OBJECT ")))return false;
  byte[] snapshot=plain.clone();NativeLightAction.deferScoped(()->{if(target.canPublish())receiver.nativeSelfOriginalSend(snapshot,note);});return true;
 }
 static ClientSession caster(ClientSession owner,BattleCompanion pet){
  var live=current.get();
  if(live==null){current.remove();return null;}
  return live.owner==owner&&live.pet==pet&&live.valid()&&owner.nativePlayerActionLock().isHeldByCurrentThread()&&live.caster.nativePlayerActionLock().isHeldByCurrentThread()?live.caster:null;
 }
 static boolean captureOutput(ClientSession receiver,byte[] plain,String note){
  return NativeSummonOwnerLifecycle.captureOutput(receiver,plain,note);
 }
 static ThreadLocal<?> ownerSlot(){return current;}
 static Object ownerContext(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  return new Target(owner,owner,pet,stamp);
 }
 static boolean ownerCanPublish(Object object){return ((Target)object).canPublish();}
 static Object ownerContext(ClientSession caster,ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  return new Target(caster,owner,pet,stamp);
 }
 static Object ownerDeletionContext(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  return ownerContext(owner,pet,stamp);
 }
 static void dispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  NativeCompanionNature.dispatch(pc,packet,original);
 }
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.ReturnToNatureSpell spell,int roll)throws IOException{
  NativeCompanionNature.cast(pc,packet,spell,roll);
 }
 static boolean admit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){
  return NativeCompanionNature.admit(pc,owner,skill,action,reuse);
 }
 private NativeReturnNatureMaterialAction(){}
}
