// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/target-negative-material/NativeTargetNegativeMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;import java.util.List;
/** The private native kind stays inside ClientSession's lambda lookup. */
final class NativeTargetNegativeMaterialAction {
 static void nativeControlledOriginalCast(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body)throws IOException{
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  boolean world=String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");
  List<ClientSession> peers=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  var target=peers.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
  NativeLightAction.commitPair(pc,target,body);
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=173&&skill!=174)throw new IllegalStateException("unreviewed target negative skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeTargetNegativeMaterialAction(){}
 static void cast(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body)throws IOException{
  NativeControlledNegativeMaterial.cast(pc,packet,body);
 }
}
