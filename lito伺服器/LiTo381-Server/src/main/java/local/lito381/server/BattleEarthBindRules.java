package local.lito381.server;

/**
 * Native 3.81 Earth Bind interaction rules recovered from the retained
 * {@code L1MagicPc} and {@code L1AttackList} paths.
 */
final class BattleEarthBindRules {
    static final int WEAPON_BREAK = 27;
    static final int CANCELLATION = 44;

    private BattleEarthBindRules() {}

    static boolean blocksProbabilityMagic(boolean earthBindActive, int skillId) {
        return earthBindActive && skillId != WEAPON_BREAK && skillId != CANCELLATION;
    }

    static int protectedCombatDamage(boolean earthBindActive, int damage) {
        return earthBindActive ? 0 : Math.max(0, damage);
    }
}
