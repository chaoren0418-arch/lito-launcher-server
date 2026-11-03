package local.lito381.server;
import java.util.*;

/** Death TYPE_LOGIN cancellation: self only, no cast, cost, SQL, timer or actor cache. */
final class CombatDeathCancellation {
 private static final String P="local/lito381/server/";
 static void death(ClientSession pc){
  if(!CombatRules.loaded()||((Number)CombatAccess.field(pc,"playerHp")).intValue()!=0)return;
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));if(!phase.equals("BATTLE")&&!phase.equals("WORLD"))return;
  CombatAccess.call(pc,"sourceDeathCancellation","(L"+P+"BattleDeathmatchFatalityCatalog$Effect;)V",(Object)null);
 }
 static BattleTimedStatus.MagicCancellation timed(BattleTimedStatus.MagicCancellation result,ClientSession pc){
  // Native ordinary Cancellation leaves Absolute Barrier/Advance Spirit.
  // Death removes all skill/status leases; keep poison removal in its exact
  // closing transaction while an already admitted publisher/tick finishes.
  CombatAccess.call(pc,"cancelAbsoluteBarrier","(Ljava/lang/String;)V","source-death-cancellation");
  CombatPoisonLifecycle.clear(CombatPoisonAtomic.status(pc),pc,null);
  return result;
 }
 static BattleAbilityStatus.Cancellation ability(BattleAbilityStatus.Cancellation result,ClientSession pc){
  ((BattleAbilityStatus)CombatAccess.field(pc,"abilityStatus")).clear();return result;
 }
 static BattleWeaponBuffStatus.Cancellation weapon(BattleWeaponBuffStatus.Cancellation result,ClientSession pc){
  for(String name:List.of("weaponBuffStatus","itemWeaponEnchantStatus","itemArmorEnchantStatus"))CombatAccess.call(CombatAccess.field(pc,name),"clear","()V");return result;
 }
 static List<BattleCookingCatalog.Recipe> cooking(BattleCookingStatus status){
  if(!CombatRules.loaded())return status.clear();
  var removed=new ArrayList<BattleCookingCatalog.Recipe>();var immune=CombatDatabase.controls().cancellationImmune();
  synchronized(status){for(String name:List.of("mainCourse","dessert")){var lease=CombatAccess.field(status,name);var recipe=(BattleCookingCatalog.Recipe)CombatAccess.field(lease,"recipe");if(recipe!=null&&!immune.contains(recipe.statusId()))removed.add(status.remove(recipe.slot()));}}
  return List.copyOf(removed);
 }
}
