// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import local.lito381.server.BattlePhysicalAttack;

record SourcePhysicalGrowthStats(List<Integer> strengthHit, List<Integer> strengthDamage, List<Integer> dexterityHit, List<Integer> dexterityDamage) {
    static final int MAX_STAT = 127;

    SourcePhysicalGrowthStats {
        strengthHit = SourcePhysicalGrowthStats.qualifiedTable(strengthHit, "STR hit");
        strengthDamage = SourcePhysicalGrowthStats.qualifiedTable(strengthDamage, "STR damage");
        dexterityHit = SourcePhysicalGrowthStats.qualifiedTable(dexterityHit, "DEX hit");
        dexterityDamage = SourcePhysicalGrowthStats.qualifiedTable(dexterityDamage, "DEX damage");
    }

    static SourcePhysicalGrowthStats classic381() {
        ArrayList<Integer> strHit = new ArrayList<Integer>(127);
        ArrayList<Integer> strDamage = new ArrayList<Integer>(127);
        ArrayList<Integer> dexHit = new ArrayList<Integer>(127);
        ArrayList<Integer> dexDamage = new ArrayList<Integer>(127);
        for (int stat = 1; stat <= 127; ++stat) {
            strHit.add(BattlePhysicalAttack.strengthHit(stat));
            strDamage.add(SourcePhysicalGrowthStats.classicStrengthDamage(stat));
            dexHit.add(BattlePhysicalAttack.dexterityHit(stat));
            dexDamage.add(SourcePhysicalGrowthStats.classicDexterityDamage(stat));
        }
        return new SourcePhysicalGrowthStats(strHit, strDamage, dexHit, dexDamage);
    }

    int hit(boolean ranged, int stat) {
        return (ranged ? this.dexterityHit : this.strengthHit).get(SourcePhysicalGrowthStats.index(stat));
    }

    int damage(boolean ranged, int stat) {
        return (ranged ? this.dexterityDamage : this.strengthDamage).get(SourcePhysicalGrowthStats.index(stat));
    }

    private static int classicStrengthDamage(int strength) {
        int value = Math.max(1, Math.min(127, strength));
        if (value <= 22) {
            return Math.floorDiv(value - 11, 2);
        }
        int damage = 5;
        for (int stat = 23; stat <= value; ++stat) {
            if (stat <= 28) {
                if (stat % 3 != 2) continue;
                ++damage;
                continue;
            }
            if (stat <= 32) {
                if (stat % 2 != 1) continue;
                ++damage;
                continue;
            }
            if (stat <= 34) {
                ++damage;
                continue;
            }
            if (stat % 4 != 1) continue;
            ++damage;
        }
        return damage;
    }

    private static int classicDexterityDamage(int dexterity) {
        int value = Math.max(1, Math.min(127, dexterity));
        if (value <= 14) {
            return 0;
        }
        if (value <= 18) {
            return value - 14;
        }
        if (value <= 20) {
            return 4;
        }
        if (value <= 23) {
            return 5;
        }
        int damage = 5;
        for (int stat = 24; stat <= value; ++stat) {
            if (stat <= 35 && stat % 3 == 1) {
                ++damage;
                continue;
            }
            if (stat < 36 || stat % 4 != 1) continue;
            ++damage;
        }
        return damage;
    }

    private static List<Integer> qualifiedTable(List<Integer> values, String name) {
        boolean containsNull = false;
        if (values != null) {
            for (Integer value : values) {
                if (value != null) continue;
                containsNull = true;
                break;
            }
        }
        if (values == null || values.size() != 127 || containsNull) {
            throw new IllegalArgumentException("source physical " + name + " table must contain ids 1..127");
        }
        return List.copyOf(values);
    }

    private static int index(int stat) {
        return Math.max(1, Math.min(127, stat)) - 1;
    }
}
