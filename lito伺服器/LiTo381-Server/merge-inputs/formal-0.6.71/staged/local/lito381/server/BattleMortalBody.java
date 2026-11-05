package local.lito381.server;

/** Pure retained Mortal Body trigger and reflected-damage decision. */
final class BattleMortalBody {
    record Decision(boolean active, int probability, int roll,
                    boolean triggered, int baseDamage,
                    int totalReduction, int reflectedDamage) {
        Decision {
            if (probability < 0 || probability > 100
                    || roll < 0 || roll > 100
                    || baseDamage < 0 || totalReduction < 0
                    || reflectedDamage < 0
                    || (!active && (probability != 0 || roll != 0
                        || triggered || baseDamage != 0
                        || totalReduction != 0 || reflectedDamage != 0))
                    || (triggered && (!active || roll == 0
                        || baseDamage < 0 || reflectedDamage <= 0))
                    || (!triggered && (baseDamage != 0
                        || totalReduction != 0 || reflectedDamage != 0))) {
                throw new IllegalArgumentException(
                        "invalid Mortal Body decision");
            }
        }

        static Decision inactive() {
            return new Decision(false, 0, 0, false, 0, 0, 0);
        }
    }

    private BattleMortalBody() {}

    static Decision decide(
            BattleSpellCatalog.MortalBodySpell spell,
            boolean active, int holderLevel, int attackerLevel,
            boolean attackerBoss, int oneBasedRoll,
            boolean playerAttacker, int attackerWeaponEnchant,
            int attackerArmorReduction, int attackerPvpReduction,
            int attackerMeleeReduction) {
        if (spell == null) {
            throw new IllegalArgumentException("Mortal Body metadata is required");
        }
        if (!active || attackerBoss && spell.bossModifier() == 0) {
            return Decision.inactive();
        }
        if (holderLevel <= 0 || holderLevel > 0xFF
                || attackerLevel <= 0 || attackerLevel > 0xFF
                || oneBasedRoll < 1 || oneBasedRoll > 100
                || attackerWeaponEnchant < -128 || attackerWeaponEnchant > 127
                || attackerArmorReduction < 0
                || attackerPvpReduction < 0
                || attackerMeleeReduction < 0) {
            throw new IllegalArgumentException(
                    "invalid Mortal Body trigger input");
        }
        int probability = spell.triggerProbability(holderLevel, attackerLevel);
        boolean triggered = oneBasedRoll <= probability;
        if (!triggered) {
            return new Decision(true, probability, oneBasedRoll,
                    false, 0, 0, 0);
        }

        int baseDamage = Math.addExact(spell.baseReflectionDamage(),
                playerAttacker ? Math.max(0, attackerWeaponEnchant) : 0);
        int totalReduction = playerAttacker
                ? Math.addExact(attackerArmorReduction,
                    Math.addExact(attackerPvpReduction, attackerMeleeReduction))
                : 0;
        int reflectedDamage = Math.max(1, baseDamage - totalReduction);
        return new Decision(true, probability, oneBasedRoll,
                true, baseDamage, totalReduction, reflectedDamage);
    }
}
