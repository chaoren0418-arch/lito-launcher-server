package local.lito381.server;

/** Fixed current-source SKD3 reductions shared by physical and magic readers. */
final class CombatSourceReductions{
 /** 1-伺服器 L1AttackList.java:285 {@code SKD3.put(PATIENCE, -15)}. */
 static final int SOURCE_PATIENCE=15;
 /** 1-伺服器 L1AttackList.java:284 {@code SKD3.put(DRAGON_SKIN, -10)}. */
 static final int SOURCE_DRAGON_SKIN=10;
 static int patience(int legacy){return CombatRules.loaded()?SOURCE_PATIENCE:legacy;}
 static int dragonSkin(int legacy){return CombatRules.loaded()?SOURCE_DRAGON_SKIN:legacy;}
}
