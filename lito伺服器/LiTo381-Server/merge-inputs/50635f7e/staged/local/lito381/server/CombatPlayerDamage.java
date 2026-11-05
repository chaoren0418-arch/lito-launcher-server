// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
package local.lito381.server;

/** Signed source magic commits at retained native PC boundaries; no owned actor state. */
final class CombatPlayerDamage{
 private static final String P="local/lito381/server/",C="L"+P+"ClientSession;";
 static void battle(ClientSession target,int damage,ClientSession caster){if(CombatRules.loaded()&&damage<0){CombatPlayerRecovery.battle(target,damage,caster,false);return;}NativeSourceDamageDisplay.invokeMagic(target,"takePlayerDamage","(I"+C+")V",new Object[]{damage,caster});}
 static void battleTerminal(ClientSession target,int damage,ClientSession caster,boolean earthBind,boolean suppress,long terminal){if(CombatRules.loaded()&&damage<0){CombatPlayerRecovery.battle(target,damage,caster,false);return;}CombatAccess.call(target,"takePlayerDamage","(I"+C+"ZZJ)V",damage,caster,earthBind,suppress,terminal);}
 static void world(ClientSession target,int damage,ClientSession caster,boolean earthBind){if(CombatRules.loaded()&&damage<0){CombatPlayerRecovery.world(target,damage,caster,false);return;}CombatAccess.call(target,"takeClassicWorldPlayerDamage","(I"+C+"Z)V",damage,caster,earthBind);}
 static int worldGate(int damage,ClientSession target,ClientSession caster){if(CombatRules.loaded()&&damage<0){CombatPlayerRecovery.world(target,damage,caster,false);return 0;}return damage;}
 static int dollGate(int damage,ClientSession target,ClientSession caster){if(CombatRules.loaded()&&damage<0&&target!=null){if(CombatWorldPhysical.world(caster))CombatPlayerRecovery.world(target,damage,caster,false);else CombatPlayerRecovery.battle(target,damage,caster,false);return 0;}return damage;}
 static byte[] shockWire(int action,long caster,long target,int damage,int heading){return Protocol.shockStunAttack(action,caster,target,CombatRules.loaded()?Math.max(0,damage):damage,heading);}
 static int mindMultiplier(BattleSpellCatalog.MindBreakSpell spell,int damage){return CombatRules.loaded()?damage*spell.damageMultiplier():spell.damageAfterMultiplier(damage);}
}
