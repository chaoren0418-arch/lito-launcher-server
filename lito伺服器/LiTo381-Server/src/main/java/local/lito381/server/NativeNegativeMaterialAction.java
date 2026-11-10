// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/negative-material/build-repro/inputs/sources/NativeNegativeMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;

/** Ordered player/owner admission; original spell body remains the target and probability authority. */
final class NativeNegativeMaterialAction {
 static ClientSession nativeSceneOriginalOwner(ClientSession pc,long targetId){
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
 static void nativeSceneOriginalEntangle(ClientSession pc,byte[] packet,BattleSpellCatalog.EntangleSpell spell,int roll)throws IOException{
  var request=Protocol.attackSpellRequest(packet);
  NativeLightAction.commitPair(pc,owner(pc,request.targetObjectId()),()->NativeCompanionNegative.entangleBody(pc,packet,spell,roll));
 }
 static void nativeSceneOriginalErase(ClientSession pc,byte[] packet,BattleSpellCatalog.EraseMagicSpell spell,int roll)throws IOException{
  var request=Protocol.attackSpellRequest(packet);
  NativeLightAction.commitPair(pc,owner(pc,request.targetObjectId()),()->NativeCompanionNegative.eraseBody(pc,packet,spell,roll));
 }
 static boolean nativeSceneOriginalAdmit(ClientSession pc,ClientSession player,ClientSession companionOwner,int skill,int action,int reuse){
  if(skill!=29&&skill!=152&&skill!=153)throw new IllegalStateException("unreviewed negative material skill");
  return NativeLightAction.admitPair(pc,player!=null?player:companionOwner!=null?companionOwner:pc,skill,action,reuse);
 }
 private NativeNegativeMaterialAction(){}

    private static ClientSession owner(ClientSession clientSession, long l) {
        return NativeCompanionNegative.owner(clientSession, l);
    }

    static void entangle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.EntangleSpell entangleSpell, int n) throws IOException {
        NativeCompanionNegative.world(clientSession, byArray, () -> NativeNegativeMaterialAction.nativeSceneOriginalEntangle(clientSession, byArray, entangleSpell, n));
    }

    static void erase(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.EraseMagicSpell eraseMagicSpell, int n) throws IOException {
        NativeCompanionNegative.world(clientSession, byArray, () -> NativeNegativeMaterialAction.nativeSceneOriginalErase(clientSession, byArray, eraseMagicSpell, n));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, ClientSession clientSession3, int n, int n2, int n3) {
        return NativeCompanionNegative.admit(clientSession, clientSession2, clientSession3, n, n2, n3);
    }
}
