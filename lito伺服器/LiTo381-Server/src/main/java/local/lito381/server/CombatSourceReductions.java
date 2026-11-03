package local.lito381.server;

/** Fixed current-source SKD3 reductions shared by physical and magic readers. */
final class CombatSourceReductions{
 static int patience(int legacy){return CombatRules.loaded()?15:legacy;}
 static int dragonSkin(int legacy){return CombatRules.loaded()?10:legacy;}
}
