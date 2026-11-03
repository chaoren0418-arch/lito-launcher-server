package local.lito381.server;

/** Compatibility facade for the selected-source Tournament NPC damage prefix. */
final class BattleTimeAttackPhysicalDamage {
    static final String SOURCE = BattleTournamentNpcPhysicalDamage.SOURCE
            + "+mobskill";

    record Outcome(
            int sourceDamage, int acMaximum, int acRoll,
            int acReduction, int afterAcDamage) {
        static Outcome from(BattleTournamentNpcPhysicalDamage.Outcome value) {
            return new Outcome(value.sourceDamage(), value.acMaximum(),
                    value.acRoll(), value.acReduction(), value.afterAcDamage());
        }
    }

    private BattleTimeAttackPhysicalDamage() {}

    static Outcome againstPlayer(
            int level, SourceNpcPhysicalStats attacker,
            SourcePhysicalGrowthStats growth, int leverage,
            int weaponBreakStacks,
            int characterType, int targetArmorClass,
            int levelRoll, int criticalRoll, int acRoll,
            long systemTimeMillis) {
        return againstPlayer(level, attacker, growth, leverage,
                weaponBreakStacks, characterType, targetArmorClass,
                levelRoll, criticalRoll, acRoll, systemTimeMillis, 0);
    }

    static Outcome againstPlayer(
            int level, SourceNpcPhysicalStats attacker,
            SourcePhysicalGrowthStats growth, int leverage,
            int weaponBreakStacks,
            int characterType, int targetArmorClass,
            int levelRoll, int criticalRoll, int acRoll,
            long systemTimeMillis, int abilityScoreModifier) {
        return Outcome.from(BattleTournamentNpcPhysicalDamage.againstPlayer(
                level, attacker, growth, leverage, weaponBreakStacks,
                characterType, targetArmorClass, levelRoll, criticalRoll,
                acRoll, systemTimeMillis, abilityScoreModifier));
    }
}
