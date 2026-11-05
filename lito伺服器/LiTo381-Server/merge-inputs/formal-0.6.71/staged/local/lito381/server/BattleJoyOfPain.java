package local.lito381.server;

/** Exact retained damage arithmetic and caps for Illusionist skill218. */
final class BattleJoyOfPain {
    static final int MISSING_HP_DIVISOR = 3;
    static final int MAXIMUM_FEEDBACK_DAMAGE = 1_500;

    private BattleJoyOfPain() {}

    static int feedbackDamage(
            int defenderMaximumHp, int defenderCurrentHp,
            int defenderEquipmentAddedHp) {
        if (defenderMaximumHp < 0 || defenderCurrentHp < 0
                || defenderCurrentHp > defenderMaximumHp
                || defenderEquipmentAddedHp < 0) {
            throw new IllegalArgumentException("invalid Joy of Pain HP state");
        }
        int retained = (defenderMaximumHp - defenderCurrentHp
                - defenderEquipmentAddedHp)
                / CombatRules.replaceInt(MISSING_HP_DIVISOR, "JOY_OF_PAIN_COUNTDMG");
        return Math.max(0, Math.min(retained,
                CombatRules.replaceInt(MAXIMUM_FEEDBACK_DAMAGE, "JOY_OF_PAIN_DMG")));
    }
}
