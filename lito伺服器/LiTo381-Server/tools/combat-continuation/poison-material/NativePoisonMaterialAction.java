package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve existing Source scope and four bodies and their target checks under ordered player locks. */
final class NativePoisonMaterialAction {
 private static ClientSession target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void poisonBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->pc.nativeMaterialPoisonScopeBody(packet,spell,scope));
 }
 static void poisonWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->pc.nativeMaterialWorldPoisonScopeBody(packet,spell,scope));
 }
 static void paralysisBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.ParalysisSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->pc.nativeMaterialParalysisScopeBody(packet,spell,scope));
 }
 static void paralysisWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.ParalysisSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->pc.nativeMaterialWorldParalysisScopeBody(packet,spell,scope));
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=11&&skill!=33)throw new IllegalStateException("unreviewed poison/paralysis material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativePoisonMaterialAction(){}
}
