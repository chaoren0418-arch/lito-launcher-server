package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Skill67 preserves native control-ring/random-form branches inside ordered locks. */
final class NativePolymorphMaterialAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.PolymorphSpell spell)throws IOException {
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  boolean world=String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  var target=players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
  NativeLightAction.commitPair(pc,target,()->pc.nativeMaterialPolymorphBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=67)throw new IllegalStateException("unreviewed polymorph skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativePolymorphMaterialAction(){}
}
