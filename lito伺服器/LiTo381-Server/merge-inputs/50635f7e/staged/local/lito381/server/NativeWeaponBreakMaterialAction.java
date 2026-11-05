// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/weapon-break-lifetime/build-repro/inputs/original/NativeWeaponBreakMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve both existing roots and their target checks under ordered player locks. */
final class NativeWeaponBreakMaterialAction {
 private static ClientSession target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void nativeTargetOriginalBattle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.WeaponBreakSpell weaponBreakSpell) throws IOException {
        if (String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD")) {
            NativeWeaponBreakMaterialAction.world(clientSession, byArray, weaponBreakSpell);
            return;
        }
        NativeLightAction.commitPair(clientSession, NativeWeaponBreakMaterialAction.target(clientSession, byArray, false), () -> clientSession.nativeMaterialWeaponBreakBody(byArray, weaponBreakSpell));
    }
 static void nativeTargetOriginalWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.WeaponBreakSpell spell)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->pc.nativeMaterialWorldWeaponBreakBody(packet,spell));
 }
 static boolean nativeTargetOriginalAdmit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=27)throw new IllegalStateException("unreviewed Weapon Break material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeWeaponBreakMaterialAction(){}

    static void battle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.WeaponBreakSpell weaponBreakSpell) throws IOException {
        NativeWeaponBreakTargetLifecycle.run(clientSession, byArray, weaponBreakSpell, false);
    }

    static void world(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.WeaponBreakSpell weaponBreakSpell) throws IOException {
        NativeWeaponBreakTargetLifecycle.run(clientSession, byArray, weaponBreakSpell, true);
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        return NativeWeaponBreakTargetLifecycle.admit(clientSession, clientSession2, n, n2, n3);
    }
}
