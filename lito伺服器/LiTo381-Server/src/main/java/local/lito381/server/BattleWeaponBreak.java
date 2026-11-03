package local.lito381.server;

/** Retained skill-27 weapon damage and NPC physical-output rules. */
final class BattleWeaponBreak {
    static final int SKILL_ID = 27;
    static final int MAX_STACKS = 10;
    static final int PERCENT_PER_STACK = 5;

    private BattleWeaponBreak() {}

    static int addStack(int current) {
        if (current < 0 || current > MAX_STACKS) {
            throw new IllegalArgumentException("invalid Weapon Break stack count");
        }
        return Math.min(MAX_STACKS, current + 1);
    }

    static int damageAfterStacks(int sourceDamage, int stacks) {
        if (sourceDamage < 0 || stacks < 0 || stacks > MAX_STACKS) {
            throw new IllegalArgumentException("invalid Weapon Break damage input");
        }
        if (sourceDamage == 0 || stacks == 0) return sourceDamage;
        int result = sourceDamage * (100 - stacks * PERCENT_PER_STACK) / 100;
        return Math.max(1, result);
    }

    static int playerWeaponDamage(int effectiveIntelligence, int oneBasedRoll) {
        int maximum = effectiveIntelligence / 3;
        if (maximum <= 0 || oneBasedRoll < 1 || oneBasedRoll > maximum) {
            throw new IllegalArgumentException("invalid Weapon Break durability roll");
        }
        return oneBasedRoll;
    }

    /**
     * Retained {@code L1Inventory.receiveDamage} caps positive weapon damage at
     * {@code enchantLevel + 5}.  An already-observed over-cap value is never
     * lowered by another hit; the retained mutation simply becomes a no-op.
     */
    static int durabilityAfterDamage(
            int currentDurability, int enchantLevel, int points) {
        if (currentDurability < 0 || currentDurability > 0x7F
                || enchantLevel < -128 || enchantLevel > 127
                || points <= 0 || points > 0x7F) {
            throw new IllegalArgumentException("invalid Weapon Break durability input");
        }
        int maximumDurability = enchantLevel + 5;
        if (currentDurability >= maximumDurability) return currentDurability;
        return Math.min(maximumDurability, currentDurability + points);
    }
}
