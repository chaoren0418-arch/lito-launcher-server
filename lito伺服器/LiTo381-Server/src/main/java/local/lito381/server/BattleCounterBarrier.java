package local.lito381.server;

/** Pure retained Counter Barrier trigger and reflected-damage decision. */
final class BattleCounterBarrier {
    record Decision(boolean active, int probability, int roll,
                    boolean triggered, int sourceDamage) {
        Decision {
            if (probability < 0 || probability > 100
                    || roll < 0 || roll > 100 || sourceDamage < 0
                    || (!active && (probability != 0 || roll != 0
                        || triggered || sourceDamage != 0))
                    || (triggered && (!active || roll == 0 || sourceDamage <= 0))
                    || (!triggered && sourceDamage != 0)) {
                throw new IllegalArgumentException(
                        "invalid Counter Barrier decision");
            }
        }

        static Decision inactive() {
            return new Decision(false, 0, 0, false, 0);
        }
    }

    private BattleCounterBarrier() {}

    static Decision decide(
            BattleSpellCatalog.CounterBarrierSpell spell,
            boolean active, SourceWeaponPhysicalStats holderWeapon,
            int holderLevel, int attackerLevel, int holderMagicHit,
            int attackerMagicResistance, boolean attackerBoss,
            int oneBasedRoll) {
        if (spell == null) {
            throw new IllegalArgumentException("Counter Barrier metadata is required");
        }
        if (!active || !spell.supportsWeapon(holderWeapon)) {
            return Decision.inactive();
        }
        if (attackerBoss && spell.bossModifier() == 0) {
            return Decision.inactive();
        }
        if (oneBasedRoll < 1 || oneBasedRoll > 100) {
            throw new IllegalArgumentException("Counter Barrier roll outside 1..100");
        }
        int probability = spell.triggerProbability(
                holderLevel, attackerLevel, holderMagicHit,
                attackerMagicResistance);
        boolean triggered = oneBasedRoll <= probability;
        int sourceDamage = triggered
                ? spell.reflectedSourceDamage(holderWeapon) : 0;
        return new Decision(true, probability, oneBasedRoll,
                triggered && sourceDamage > 0, sourceDamage);
    }
}
