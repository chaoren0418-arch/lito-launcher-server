package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Retain original spell bodies; bind a queued companion cast to its real owner and actor. */
final class NativeResurrectionMaterialAction {
 private record Target(ClientSession caster,ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  boolean valid(){
   if(pet==null)return true;
   if(!stamp.master.matches(owner)||(boolean)CombatAccess.field(owner,NativeDeItemApplication.DISCONNECTED))return false;
   if(CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",stamp.companion)!=pet)return false;
   return NativeCompanionCallbacks.capture(stamp.master,pet).life==stamp.life;
  }
 }
 private static final ThreadLocal<Target> current=new ThreadLocal<>();
 private static Target target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  for(var player:players)if(player.playerObjectId()==request.targetObjectId())return new Target(pc,player,null,null);
  // The original WORLD root resolves companions; the BATTLE root does not.
  if(world)for(var owner:players)for(var snapshot:owner.classicWorldCompanionSnapshots())if(snapshot.objectId()==request.targetObjectId()){
   var pet=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",request.targetObjectId());
   if(pet!=null)return new Target(pc,owner,pet,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));
  }
  return new Target(pc,pc,null,null);
 }
 private static void run(ClientSession pc,Target target,NativeSelfCastAction.Task body)throws IOException{
  NativeLightAction.commitPair(pc,target.owner,()->{
   if(!target.valid())return;
   var previous=current.get();current.set(target);
   try{body.run();}finally{if(previous==null)current.remove();else current.set(previous);}
  });
 }
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,target(pc,packet,false),()->pc.nativeMaterialResurrectionBody(packet,spell));
 }
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,target(pc,packet,true),()->pc.nativeMaterialWorldResurrectionBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession player,int skill,int action,int reuse){
  if(skill!=61&&skill!=75)throw new IllegalStateException("unreviewed Resurrection material skill");
  var target=current.get();
  if(target==null||target.caster!=pc||!target.valid()||player!=null&&target.owner!=player)return false;
  return NativeLightAction.admitPair(pc,target.owner,skill,action,reuse);
 }
 private NativeResurrectionMaterialAction(){}
}
