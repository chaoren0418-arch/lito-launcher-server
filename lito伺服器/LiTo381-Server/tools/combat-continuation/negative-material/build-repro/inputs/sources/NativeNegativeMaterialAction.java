package local.lito381.server;
import java.io.IOException;
import java.util.List;

/** Ordered player/owner admission; original spell body remains the target and probability authority. */
final class NativeNegativeMaterialAction {
 private static ClientSession owner(ClientSession pc,long targetId){
  var hub=(GameHub)CombatAccess.field(pc,"hub");String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  List<ClientSession> players=phase.equals("WORLD")
   ?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId"))
   :phase.equals("BATTLE")?hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId")):List.of();
  for(var candidate:players)if(candidate!=pc&&candidate.playerObjectId()==targetId)return candidate;
  for(var candidate:players){
   var snapshots=phase.equals("WORLD")?candidate.classicWorldCompanionSnapshots():candidate.controlledCompanionSnapshots();
   for(var snapshot:snapshots)if(snapshot.objectId()==targetId
    &&CombatAccess.call(candidate,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",targetId)!=null)return candidate;
  }
  return pc;
 }
 static void entangle(ClientSession pc,byte[] packet,BattleSpellCatalog.EntangleSpell spell,int roll)throws IOException{
  var request=Protocol.attackSpellRequest(packet);
  NativeLightAction.commitPair(pc,owner(pc,request.targetObjectId()),()->pc.nativeMaterialEntangleBody(packet,spell,roll));
 }
 static void erase(ClientSession pc,byte[] packet,BattleSpellCatalog.EraseMagicSpell spell,int roll)throws IOException{
  var request=Protocol.attackSpellRequest(packet);
  NativeLightAction.commitPair(pc,owner(pc,request.targetObjectId()),()->pc.nativeMaterialEraseBody(packet,spell,roll));
 }
 static boolean admit(ClientSession pc,ClientSession player,ClientSession companionOwner,int skill,int action,int reuse){
  if(skill!=29&&skill!=152&&skill!=153)throw new IllegalStateException("unreviewed negative material skill");
  return NativeLightAction.admitPair(pc,player!=null?player:companionOwner!=null?companionOwner:pc,skill,action,reuse);
 }
 private NativeNegativeMaterialAction(){}
}
