package local.lito381.server;

/** Exact accepted-hit weapon-damage gate retained for skill 89. */
final class BattleBounceAttack {
    record Decision(boolean eligible, int chance, int roll, boolean damagesWeapon) {
        Decision {
            if (chance < 0 || chance > 100 || roll < 0 || roll > 100
                    || (!eligible && (chance != 0 || roll != 0 || damagesWeapon))
                    || (eligible && (chance == 0 || roll == 0
                            || damagesWeapon != (roll <= chance)))) {
                throw new IllegalArgumentException("invalid Bounce Attack decision");
            }
        }

        static Decision ineligible() {
            return new Decision(false, 0, 0, false);
        }
    }

    private BattleBounceAttack() {}

    static Decision decide(
            BattleAbilityStatus.BounceAttack status,
            SourceWeaponPhysicalStats weapon, int percentileRoll) {
        if (status == null || status.remainingSeconds() == 0
                || weapon == null || !weapon.present() || weapon.ranged()) {
            return Decision.ineligible();
        }
        if (percentileRoll < 1 || percentileRoll > 100) {
            throw new IllegalArgumentException("invalid Bounce Attack percentile roll");
        }
        return new Decision(true, status.weaponDamageChance(), percentileRoll,
                percentileRoll <= status.weaponDamageChance());
    }
}
