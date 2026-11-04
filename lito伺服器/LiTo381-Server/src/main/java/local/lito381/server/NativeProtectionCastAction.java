// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/protection-material/NativeProtectionCastAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve the original target rules; capture both role identities before the ordered action lock. */
final class NativeProtectionCastAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.DamageProtectionSpell spell)throws IOException {
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  List<ClientSession> players=phase.equals("WORLD")
   ?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId"))
   :phase.equals("BATTLE")?hub.roomCombatants(((Number)CombatAccess.field(pc,"currentRoomId")).longValue()):List.of();
  var target=players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(null);
  NativeLightAction.commitPair(pc,target==null?pc:target,()->pc.nativeMaterialProtectionBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession target,BattleSpellCatalog.DamageProtectionSpell spell){
  if(spell.skillId()!=68)throw new IllegalStateException("unreviewed target material skill");
  return NativeLightAction.admitPair(pc,target,spell.skillId(),spell.actionId(),spell.reuseDelayMillis());
 }
 private NativeProtectionCastAction(){}
}
