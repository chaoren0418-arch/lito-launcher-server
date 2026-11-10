// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;


final class BattleTournamentNpcPhysicalDamage {
    static final String SOURCE = "selected-380_db-npc+retained-L1AttackNpc";

    private BattleTournamentNpcPhysicalDamage() {
    }

    static Outcome againstPlayer(int level, SourceNpcPhysicalStats attacker, SourcePhysicalGrowthStats growth, int leverage, int weaponBreakStacks, int characterType, int targetArmorClass, int levelRoll, int criticalRoll, int acRoll, long systemTimeMillis) {
        return BattleTournamentNpcPhysicalDamage.againstPlayer(level, attacker, growth, leverage, weaponBreakStacks, characterType, targetArmorClass, levelRoll, criticalRoll, acRoll, systemTimeMillis, 0);
    }

    static Outcome againstPlayer(int level, SourceNpcPhysicalStats attacker, SourcePhysicalGrowthStats growth, int leverage, int weaponBreakStacks, int characterType, int targetArmorClass, int levelRoll, int criticalRoll, int acRoll, long systemTimeMillis, int abilityScoreModifier) {
        if (attacker == null || growth == null || level <= 0 || levelRoll < 0 || levelRoll >= level || criticalRoll < 0 || criticalRoll >= 100 || leverage <= 0 || weaponBreakStacks < 0 || weaponBreakStacks > 10 || abilityScoreModifier > 0 || abilityScoreModifier < -127) {
            throw new IllegalArgumentException("invalid Tournament NPC physical damage input");
        }
        int n10 = Math.max(1, Math.addExact(attacker.strength(), abilityScoreModifier));
        boolean bl = switch (attacker.undead()) {
            case 1, 3, 4 -> ClassicWorldNpcDamage.retainedNight(systemTimeMillis);
            default -> false;
        };
        double d = NativeNpcWeaponBreakDamage.sourceExact(level, n10, growth.damage(false, n10), levelRoll, criticalRoll, bl, attacker.boss(), leverage);
        int n11 = Math.max((int)d, 0);
        n11 = NativeNpcWeaponBreakDamage.afterStacks(d, weaponBreakStacks);
        int n12 = BattlePhysicalDefense.playerAcMaximum(characterType, targetArmorClass);
        if (n12 == 0 && acRoll != 0 || n12 > 0 && acRoll >= n12) {
            throw new IllegalArgumentException("invalid Tournament NPC player AC roll");
        }
        int n13 = n12 == 0 ? 0 : acRoll + Math.max(1, n12 >> 3);
        return new Outcome(n11, n12, acRoll, n13, Math.max(0, n11 - n13));
    }

    record Outcome(int sourceDamage, int acMaximum, int acRoll, int acReduction, int afterAcDamage) {
        Outcome {
            if (sourceDamage < 0 || acMaximum < 0 || acRoll < 0 || acReduction < 0 || afterAcDamage < 0 || afterAcDamage > sourceDamage) {
                throw new IllegalArgumentException("invalid Tournament NPC physical damage outcome");
            }
        }
    }
}
