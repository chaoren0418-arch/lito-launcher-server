// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/earth-bind-material/NativeEarthBindMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve both existing roots and their target checks under ordered player locks. */
final class NativeEarthBindMaterialAction {
 static ClientSession nativeSceneOriginalTarget(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.BindSpell spell)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->pc.nativeMaterialEarthBindBody(packet,spell));
 }
 static void nativeSceneOriginalWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.BindSpell spell)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->NativeCompanionEarth.body(pc,packet,spell));
 }
 static boolean nativeSceneOriginalAdmit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=157)throw new IllegalStateException("unreviewed Earth Bind material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeEarthBindMaterialAction(){}

    private static ClientSession target(ClientSession clientSession, byte[] byArray, boolean bl) {
        return NativeCompanionEarth.target(clientSession, byArray, bl);
    }

    static void world(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.BindSpell bindSpell) throws IOException {
        NativeCompanionEarth.world(clientSession, byArray, bindSpell);
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        return NativeCompanionEarth.admit(clientSession, clientSession2, n, n2, n3);
    }
}
