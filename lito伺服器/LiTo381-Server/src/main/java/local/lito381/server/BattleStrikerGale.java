package local.lito381.server;

/** Exact retained skill174 physical-damage modifier predicates. */
final class BattleStrikerGale {
    static final double DAMAGE_MULTIPLIER = 1.2D;
    static final int BOW_WEAPON_TYPE = 20;
    static final int GAUNTLET_WEAPON_TYPE = 62;

    private BattleStrikerGale() {}

    static boolean playerWeaponEligible(int weaponType) {
        return weaponType == BOW_WEAPON_TYPE
                || weaponType == GAUNTLET_WEAPON_TYPE;
    }

    /** Retained L1AttackNpc: distance >1 and a non-zero NPC bow action. */
    static boolean npcAttackEligible(int chebyshevDistance, int bowActionId) {
        if (chebyshevDistance < 0 || bowActionId < 0) {
            throw new IllegalArgumentException("invalid NPC ranged predicate");
        }
        return chebyshevDistance > 1 && bowActionId > 0;
    }

    static int damageAfterStatus(
            int damage, boolean statusActive, boolean rangedEligible) {
        if (damage < 0) {
            throw new IllegalArgumentException("negative Striker Gale damage");
        }
        if (damage == 0 || !statusActive || !rangedEligible) return damage;
        return (int) (damage * DAMAGE_MULTIPLIER);
    }
}
