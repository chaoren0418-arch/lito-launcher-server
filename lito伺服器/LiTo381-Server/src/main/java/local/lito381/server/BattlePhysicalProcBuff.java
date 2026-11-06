package local.lito381.server;

/** Retained player-attack multiplier procs used by Dark Elf self buffs. */
final class BattlePhysicalProcBuff {
    static final int BURNING_SPIRIT_CHANCE = 33;
    static final double BURNING_SPIRIT_MULTIPLIER = 1.5D;
    static final int ELEMENTAL_FIRE_CHANCE = 33;
    static final double ELEMENTAL_FIRE_MULTIPLIER = 1.5D;
    static final double SOUL_OF_FLAME_MULTIPLIER = 1.5D;
    static final int DOUBLE_BRAKE_CHANCE = 15;
    static final double DOUBLE_BRAKE_MULTIPLIER = 1.5D;

    private BattlePhysicalProcBuff() {}

    record Outcome(boolean active, boolean eligible, int chance, int roll,
                   boolean triggered, double multiplier) {
        static Outcome inactive(boolean active, boolean eligible, int chance) {
            return new Outcome(active, eligible, chance,
                    0, false, 1.0D);
        }
    }

    /**
     * The retained source excludes bows, gauntlets and Kiringku before it
     * consumes the shared 1..100 proc roll. A trigger contributes a 1.5
     * candidate to the attack's maximum-multiplier family.
     */
    static Outcome burningSpirit(boolean active, boolean eligible, int roll) {
        if (!active || !eligible) {
            if (roll != 0) {
                throw new IllegalArgumentException("inactive Burning Spirit has a roll");
            }
            return Outcome.inactive(active, eligible,
                    CombatRules.replaceInt(BURNING_SPIRIT_CHANCE, "BURNING_CHANCE"));
        }
        if (roll < 1 || roll > 100) {
            throw new IllegalArgumentException("invalid Burning Spirit roll");
        }
        boolean triggered = roll <= CombatRules.replaceInt(BURNING_SPIRIT_CHANCE, "BURNING_CHANCE");
        return new Outcome(true, true,
                CombatRules.replaceInt(BURNING_SPIRIT_CHANCE, "BURNING_CHANCE"), roll,
                triggered, triggered ? CombatRules.replace(BURNING_SPIRIT_MULTIPLIER, "BURNING_DMG") : 1.0D);
    }

    /**
     * Retained Elemental Fire shares Burning Spirit's one-based melee proc
     * roll, excludes bow, gauntlet and Kiringku attacks, and contributes the
     * same 1.5 candidate to the maximum-multiplier family.
     */
    static Outcome elementalFire(boolean active, boolean eligible, int roll) {
        if (!active || !eligible) {
            if (roll != 0) {
                throw new IllegalArgumentException("inactive Elemental Fire has a roll");
            }
            return Outcome.inactive(active, eligible,
                    CombatRules.replaceInt(ELEMENTAL_FIRE_CHANCE, "ELEMENTAL_FIRE_RND"));
        }
        if (roll < 1 || roll > 100) {
            throw new IllegalArgumentException("invalid Elemental Fire roll");
        }
        boolean triggered = roll <= CombatRules.replaceInt(ELEMENTAL_FIRE_CHANCE, "ELEMENTAL_FIRE_RND");
        return new Outcome(true, true,
                CombatRules.replaceInt(ELEMENTAL_FIRE_CHANCE, "ELEMENTAL_FIRE_RND"), roll,
                triggered, triggered ? CombatRules.replace(ELEMENTAL_FIRE_MULTIPLIER, "ELEMENTAL_FIRE") : 1.0D);
    }

    /** Soul of Flame is an always-on candidate in the retained max family. */
    static double soulOfFlameMultiplier(boolean active) {
        return active ? CombatRules.replace(SOUL_OF_FLAME_MULTIPLIER, "SOUL_OF_FLAME_DAMAGE") : 1.0D;
    }

    /**
     * The retained source admits only claw (11) and Edoryu (12) hits.  An
     * admitted hit rolls one-based inclusive 15 percent and contributes a
     * 1.5 candidate to the same maximum-multiplier family as weapon criticals.
     */
    static Outcome doubleBrake(boolean active, int weaponType, boolean physicalHit,
                               int roll) {
        boolean eligible = physicalHit && (weaponType == 11 || weaponType == 12);
        if (!active || !eligible) {
            if (roll != 0) {
                throw new IllegalArgumentException("inactive Double Brake has a roll");
            }
            return Outcome.inactive(active, eligible,
                    CombatRules.replaceInt(DOUBLE_BRAKE_CHANCE, "DOUBLE_BREAK_CHANCE"));
        }
        if (roll < 1 || roll > 100) {
            throw new IllegalArgumentException("invalid Double Brake roll");
        }
        boolean triggered = roll <= CombatRules.replaceInt(DOUBLE_BRAKE_CHANCE, "DOUBLE_BREAK_CHANCE");
        return new Outcome(true, true,
                CombatRules.replaceInt(DOUBLE_BRAKE_CHANCE, "DOUBLE_BREAK_CHANCE"), roll,
                triggered, triggered ? CombatRules.replace(DOUBLE_BRAKE_MULTIPLIER, "DOUBLE_BREAK_DMG") : 1.0D);
    }
}
