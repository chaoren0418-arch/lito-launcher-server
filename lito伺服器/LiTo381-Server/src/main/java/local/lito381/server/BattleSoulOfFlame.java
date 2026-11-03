package local.lito381.server;

/** Exact retained skill175 weapon-die and ranged-base rules. */
final class BattleSoulOfFlame {
    private BattleSoulOfFlame() {}

    /** Retained weaponDamage1 overrides an armed ordinary die with its maximum. */
    static int meleeWeaponRoll(boolean active, int ordinaryRoll, int maximum) {
        if (ordinaryRoll < 0 || maximum < 0 || ordinaryRoll > maximum) {
            throw new IllegalArgumentException("invalid Soul of Flame weapon die");
        }
        if (maximum == 0) return ordinaryRoll;
        return active ? maximum : ordinaryRoll;
    }

    /**
     * The retained ranged path normally has no weapon die, but Soul of Flame's
     * post-switch override injects the selected weapon's maximum before the
     * independently rolled arrow/sting damage.
     */
    static int rangedWeaponBase(
            boolean active, SourceWeaponPhysicalStats weapon,
            boolean targetLarge) {
        if (weapon == null) {
            throw new IllegalArgumentException("missing Soul of Flame weapon");
        }
        if (!active || !weapon.present() || !weapon.ranged()) return 0;
        return targetLarge ? weapon.damageLarge() : weapon.damageSmall();
    }

    static int damageAfterMultiplier(boolean active, int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("negative Soul of Flame damage");
        }
        if (!active) return damage;
        double result = damage * BattlePhysicalProcBuff.SOUL_OF_FLAME_MULTIPLIER;
        if (!Double.isFinite(result) || result > Integer.MAX_VALUE) {
            throw new ArithmeticException("Soul of Flame damage overflow");
        }
        return (int) result;
    }
}
