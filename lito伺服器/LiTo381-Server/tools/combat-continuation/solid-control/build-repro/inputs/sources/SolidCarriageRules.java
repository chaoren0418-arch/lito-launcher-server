package local.lito381.server;
/** Source SOLID_CARRIAGE: type 7 always; type 13 only for mode 1. */
final class SolidCarriageRules {
 static boolean equipment(boolean shield,boolean guarder) {
  return shield || ((int)CombatRules.number("SOLID_CARRIAGE_MODE",1)==1 && guarder);
 }
 static boolean runtime(RuntimeInventory inventory) {
  for(var item:inventory.items()) {
   if(!item.equipped())continue;
   var armor=item.definition().sourceArmorStatus();
   if(armor!=null && equipment(armor.armorType()==7,armor.armorType()==13))return true;
   // Only old isolated fixtures may lack Source armor identity. In production,
   // UI useType 25 alone cannot distinguish a shield from a guarder.
   if(!CombatRules.loaded() && armor==null && item.definition().useType()==25)return true;
  }
  return false;
 }
}
