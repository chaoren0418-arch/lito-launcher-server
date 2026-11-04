// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/resurrection-material/NativeResurrectionMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve both existing roots and their target checks under ordered player locks. */
final class NativeResurrectionMaterialAction {
 private static final ThreadLocal<Target> current=new ThreadLocal<>(),ingress=new ThreadLocal<>();
 private record Target(ClientSession caster,ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  boolean valid(){
   if(pet==null)return true;
   if(!stamp.master.matches(owner)||(boolean)CombatAccess.field(owner,NativeDeItemApplication.DISCONNECTED))return false;
   if(CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",stamp.companion)!=pet)return false;
   return NativeCompanionCallbacks.capture(stamp.master,pet).life==stamp.life;
  }
 }
 private static Target target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  for(var owner:players)if(owner.playerObjectId()==request.targetObjectId())return new Target(pc,owner,null,null);
  if(world)for(var owner:NativeCompanionNegative.members(players,pc)){
   for(var snapshot:NativeCompanionResurrection.snapshots(owner,pc)){
    if(snapshot.objectId()!=request.targetObjectId())continue;
    var pet=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",request.targetObjectId());
    if(pet!=null)return new Target(pc,owner,pet,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));
   }
  }
  return new Target(pc,pc,null,null);
 }
 static void nativeSceneOriginalDispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  int skill=Protocol.spellSelector(packet).skillId();
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  if(skill!=61&&skill!=75||!phase.equals("WORLD")&&!phase.equals("BATTLE")||pc.selectedCharacterName().isBlank()){original.run();return;}
  var prior=ingress.get();ingress.set(target(pc,packet,phase.equals("WORLD")));
  try{original.run();}finally{if(prior==null)ingress.remove();else ingress.set(prior);}
 }
 private static void run(ClientSession pc,byte[] packet,boolean world,NativeSelfCastAction.Task body)throws IOException{
  var inherited=ingress.get();var target=inherited!=null&&inherited.caster==pc?inherited:target(pc,packet,world);ingress.remove();
  try{NativeLightAction.commitPair(pc,target.owner,()->{
   if(!target.valid())return;var prior=current.get();current.set(target);
   try{body.run();}finally{if(prior==null)current.remove();else current.set(prior);}
  });}finally{if(inherited==null)ingress.remove();else ingress.set(inherited);}
 }
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,packet,false,()->pc.nativeMaterialResurrectionBody(packet,spell));
 }
 static void nativeSceneOriginalWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,packet,true,()->pc.nativeMaterialWorldResurrectionBody(packet,spell));
 }
 static boolean nativeSceneOriginalAdmit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){
  if(skill!=61&&skill!=75)throw new IllegalStateException("unreviewed Resurrection material skill");
  var target=current.get();
  if(target==null||target.caster!=pc||!target.valid()||owner!=null&&target.owner!=owner)return false;
  return NativeLightAction.admitPair(pc,target.owner,skill,action,reuse);
 }
 static void dispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  NativeCompanionResurrection.dispatch(pc,packet,original);
 }
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  NativeCompanionResurrection.world(pc,packet,spell);
 }
 static boolean admit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){
  return NativeCompanionResurrection.admit(pc,owner,skill,action,reuse);
 }
 private NativeResurrectionMaterialAction(){}
}
