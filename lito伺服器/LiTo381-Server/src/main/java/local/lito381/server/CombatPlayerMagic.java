package local.lito381.server;

/** Source PC scalar reductions; no actor fields, caches, or persistence. */
final class CombatPlayerMagic {
 private static final String P="local/lito381/server/";
 static int signedPreMr(int value,int zero){return CombatRules.loaded()?value:Math.max(value,zero);}
 static boolean protectedZero(ClientSession target,long now){if(target==null)return false;var status=(BattleTimedStatus)CombatAccess.field(target,"timedStatus");return status.absoluteBarrierActive(now)||status.earthBindActive(now)||status.freezeActive(now);}
 static int protect(ClientSession target,int damage,String description){if(!CombatRules.loaded())return CombatAccess.integerCall(target,"earthBindProtectedCombatDamage","(ILjava/lang/String;)I",damage,description);return protectedZero(target,System.nanoTime())?0:damage;}
 static int reductions(ClientSession target,int damage,String description){
  if(!CombatRules.loaded())return CombatAccess.integerCall(target,"combatDamageAfterCooking","(ILjava/lang/String;)I",damage,description);
  long now=System.nanoTime();if(protectedZero(target,now))return 0;
  var cooking=((BattleCookingStatus)CombatAccess.field(target,"cookingStatus")).active(now);
  var ability=(BattleAbilityStatus)CombatAccess.field(target,"abilityStatus");
  int result=damage-cooking.damageReduction();
  if(ability.reductionArmor(now).remainingSeconds()!=0){int level=target.battleLevel();result-=(Math.max(level,50)-50)/5+1;}
  // Current source L1AttackList.SKD3 literals apply to PC magic as well as physical hits.
  if(((BattleDragonSkinStatus)CombatAccess.field(target,"dragonSkinStatus")).snapshot(now).active())result-=CombatSourceReductions.dragonSkin(10);
  if(CombatAccess.integerCall(target,"effectivePatiencePhysicalReduction","(J)I",now)!=0)result-=CombatSourceReductions.patience(2);
  if(ability.immuneToHarm(now).divisor()!=0)result>>=1;
  return result;
 }
}
