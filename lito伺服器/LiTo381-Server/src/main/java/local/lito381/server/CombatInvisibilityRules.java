// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/invisibility-control/CombatInvisibilityRules.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
/** Source ConfigSkill flag; isolated legacy fixtures retain their old behavior. */
final class CombatInvisibilityRules {
 static boolean breakOnCast(){return CombatRules.number("INVIS_CAST_BREAK",1)!=0;}
 static boolean toggle(boolean sameSpell,ClientSession pc){
  if(!sameSpell||!breakOnCast())return false;
  var inv=pc.nativeDeInventory(0);
  return !inv.hasEquippedCatalog(20077)&&!inv.hasEquippedCatalog(120077)&&!inv.hasEquippedCatalog(20062);
 }
}
