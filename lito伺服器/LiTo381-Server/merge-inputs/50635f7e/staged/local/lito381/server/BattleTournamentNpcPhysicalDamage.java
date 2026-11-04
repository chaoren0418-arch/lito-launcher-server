package local.lito381.server;


final class BattleTournamentNpcPhysicalDamage {
    static final String SOURCE = "selected-380_db-npc+retained-L1AttackNpc";

    private BattleTournamentNpcPhysicalDamage() {
    }

    static Outcome againstPlayer(int n, SourceNpcPhysicalStats sourceNpcPhysicalStats, SourcePhysicalGrowthStats sourcePhysicalGrowthStats, int n2, int n3, int n4, int n5, int n6, int n7, int n8, long l) {
        return BattleTournamentNpcPhysicalDamage.againstPlayer(n, sourceNpcPhysicalStats, sourcePhysicalGrowthStats, n2, n3, n4, n5, n6, n7, n8, l, 0);
    }

    static Outcome againstPlayer(int n, SourceNpcPhysicalStats sourceNpcPhysicalStats, SourcePhysicalGrowthStats sourcePhysicalGrowthStats, int n2, int n3, int n4, int n5, int n6, int n7, int n8, long l, int n9) {
        if (sourceNpcPhysicalStats == null || sourcePhysicalGrowthStats == null || n <= 0 || n6 < 0 || n6 >= n || n7 < 0 || n7 >= 100 || n2 <= 0 || n3 < 0 || n3 > 10 || n9 > 0 || n9 < -127) {
            throw new IllegalArgumentException("invalid Tournament NPC physical damage input");
        }
        int n10 = Math.max(1, Math.addExact(sourceNpcPhysicalStats.strength(), n9));
        boolean bl = switch (sourceNpcPhysicalStats.undead()) {
            case 1, 3, 4 -> ClassicWorldNpcDamage.retainedNight(l);
            default -> false;
        };
        double d = NativeNpcWeaponBreakDamage.sourceExact(n, n10, sourcePhysicalGrowthStats.damage(false, n10), n6, n7, bl, sourceNpcPhysicalStats.boss(), n2);
        int n11 = Math.max((int)d, 0);
        n11 = NativeNpcWeaponBreakDamage.afterStacks(d, n3);
        int n12 = BattlePhysicalDefense.playerAcMaximum(n4, n5);
        if (n12 == 0 && n8 != 0 || n12 > 0 && n8 >= n12) {
            throw new IllegalArgumentException("invalid Tournament NPC player AC roll");
        }
        int n13 = n12 == 0 ? 0 : n8 + Math.max(1, n12 >> 3);
        return new Outcome(n11, n12, n8, n13, Math.max(0, n11 - n13));
    }

    record Outcome(int sourceDamage, int acMaximum, int acRoll, int acReduction, int afterAcDamage) {
        Outcome {
            if (sourceDamage < 0 || acMaximum < 0 || acRoll < 0 || acReduction < 0 || afterAcDamage < 0 || afterAcDamage > sourceDamage) {
                throw new IllegalArgumentException("invalid Tournament NPC physical damage outcome");
            }
        }
    }
}
