package local.lito381.server;
/** The original HEAL consumer admits battle and a currently registered WORLD scene. */
final class NativeWorldPotionPhase {
 static boolean allowed(ClientSession pc){
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));if(phase.equals("BATTLE"))return true;if(!phase.equals("WORLD"))return false;
  var hub=(GameHub)CombatAccess.field(pc,"hub");var location=hub.classicWorldLocation(pc);
  return location!=null&&location.mapId()==(int)CombatAccess.field(pc,"classicWorldMapId")&&location.instanceId()==(int)CombatAccess.field(pc,"classicWorldInstanceId");
 }
 private NativeWorldPotionPhase(){}
}
