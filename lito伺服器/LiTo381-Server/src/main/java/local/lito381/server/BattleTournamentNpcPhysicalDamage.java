package local.lito381.server;

/** Selected-source Tournament NPC-to-player physical damage prefix. */
final class BattleTournamentNpcPhysicalDamage {
    static final String SOURCE =
            "selected-380_db-npc+retained-L1AttackNpc";

    record Outcome(
            int sourceDamage, int acMaximum, int acRoll,
            int acReduction, int afterAcDamage) {
        Outcome {
            if (sourceDamage < 0 || acMaximum < 0 || acRoll < 0
                    || acReduction < 0 || afterAcDamage < 0
                    || afterAcDamage > sourceDamage) {
                throw new IllegalArgumentException(
                        "invalid Tournament NPC physical damage outcome");
            }
        }
    }

    private BattleTournamentNpcPhysicalDamage() {}

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
        if (attacker == null || growth == null || level <= 0
                || levelRoll < 0 || levelRoll >= level
                || criticalRoll < 0 || criticalRoll >= 100
                || leverage <= 0 || weaponBreakStacks < 0
                || weaponBreakStacks > BattleWeaponBreak.MAX_STACKS
                || abilityScoreModifier > 0 || abilityScoreModifier < -127) {
            throw new IllegalArgumentException(
                    "invalid Tournament NPC physical damage input");
        }

        int strength = Math.max(1,
                Math.addExact(attacker.strength(), abilityScoreModifier));
        boolean undeadNight = switch (attacker.undead()) {
            case 1, 3, 4 -> ClassicWorldNpcDamage.retainedNight(systemTimeMillis);
            default -> false;
        };
        int sourceDamage = ClassicWorldNpcDamage.sourceDamage(
                level, strength, growth.damage(false, strength),
                levelRoll, criticalRoll, undeadNight,
                attacker.boss(), leverage);

        sourceDamage = BattleWeaponBreak.damageAfterStacks(
                sourceDamage, weaponBreakStacks);
        int acMaximum = BattlePhysicalDefense.playerAcMaximum(
                characterType, targetArmorClass);
        if ((acMaximum == 0 && acRoll != 0)
                || (acMaximum > 0 && acRoll >= acMaximum)) {
            throw new IllegalArgumentException(
                    "invalid Tournament NPC player AC roll");
        }
        int acReduction = acMaximum == 0
                ? 0 : acRoll + Math.max(1, acMaximum >> 3);
        return new Outcome(sourceDamage, acMaximum, acRoll, acReduction,
                Math.max(0, sourceDamage - acReduction));
    }
}
