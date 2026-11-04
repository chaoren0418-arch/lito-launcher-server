// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/weakness-material/build-repro/inputs/sources/NativeWeaknessMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve both existing roots and their target checks under ordered player locks. */
final class NativeWeaknessMaterialAction {
 private static ClientSession target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.WeaknessSpell spell)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->pc.nativeMaterialWeaknessBody(packet,spell));
 }
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.WeaknessSpell spell)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->pc.nativeMaterialWorldWeaknessBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=47&&skill!=56)throw new IllegalStateException("unreviewed Weakness material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeWeaknessMaterialAction(){}
}
