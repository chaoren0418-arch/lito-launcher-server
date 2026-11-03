package local.lito381.server;

/** Exact retained 3.81 player-source damage core for skill 108 Final Burn. */
final class BattleFinalBurn {
    private BattleFinalBurn() {}

    static Result resolve(int casterMpBeforeConsume,
                          int targetArmorReductionBase,
                          int targetArmorReductionOffset,
                          int targetMagicResistance,
                          int attackerOriginalMagicHit) {
        if (casterMpBeforeConsume < 0 || casterMpBeforeConsume > 0xFFFF
                || targetArmorReductionBase < 0
                || targetMagicResistance < 0
                || attackerOriginalMagicHit < 0) {
            throw new IllegalArgumentException("invalid Final Burn damage inputs");
        }
        int armorReduction = BattleMagicResistance.armorDamageReduction(
                targetArmorReductionBase, targetArmorReductionOffset);
        int preMrDamage = Math.max(casterMpBeforeConsume - armorReduction, 0);
        int damage = Math.max(BattleMagicResistance.damageAfterResistance(
                preMrDamage, targetMagicResistance, attackerOriginalMagicHit), 0);
        return new Result(casterMpBeforeConsume, armorReduction,
                preMrDamage, damage, 1, 0);
    }

    record Result(int sourceMp, int armorReduction, int preMrDamage,
                  int damage, int casterHpAfter, int casterMpAfter) {}
}
