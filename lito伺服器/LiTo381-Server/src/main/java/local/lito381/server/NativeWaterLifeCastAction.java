// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/water-life-material/build-repro/inputs/sources/NativeWaterLifeCastAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve the original target rules; capture both role identities before the ordered action lock. */
final class NativeWaterLifeCastAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.WaterLifeSpell spell)throws IOException {
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  List<ClientSession> players=phase.equals("WORLD")
   ?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId"))
   :phase.equals("BATTLE")?hub.visibleBattleCombatants(((Number)CombatAccess.field(pc,"currentRoomId")).longValue(),pc):List.of();
  var target=players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(request.targetObjectId()==pc.playerObjectId()?pc:null);
  NativeLightAction.commitPair(pc,target==null?pc:target,()->pc.nativeMaterialWaterLifeBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession target,BattleSpellCatalog.WaterLifeSpell spell){
  if(spell.skillId()!=170)throw new IllegalStateException("unreviewed target material skill");
  return NativeLightAction.admitPair(pc,target,spell.skillId(),spell.actionId(),spell.reuseDelayMillis());
 }
 private NativeWaterLifeCastAction(){}
}
