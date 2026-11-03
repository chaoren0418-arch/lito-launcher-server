// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Locale;

record SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity, boolean eraseEligible, int ranged, int bowActionId, int poisonAttack, int paralysisAttack) {
    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity, boolean eraseEligible, int ranged, int bowActionId) {
        this(npcId, armorClass, large, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, eraseEligible, ranged, bowActionId, 0, 0);
    }

    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity, boolean eraseEligible) {
        this(npcId, armorClass, large, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, eraseEligible, 0, 0);
    }

    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity) {
        this(npcId, armorClass, large, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, false);
    }

    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard) {
        this(npcId, armorClass, large, damageReduction, hard, 0, 0, 0, false, 0, false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance) {
        this(npcId, armorClass, large, damageReduction, hard, undead, weakAttribute, magicResistance, false, 0, false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible) {
        this(npcId, armorClass, large, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, 0, false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats(int npcId, int armorClass, boolean large, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful) {
        this(npcId, armorClass, large, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, false, false, 0, 1, 1);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    SourceNpcPhysicalStats {
        if (npcId <= 0 || damageReduction < 0 || undead < 0 || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE || maxMp < 0 || strength < 0 || dexterity < 0 || ranged < 0 || bowActionId < 0 || bowActionId > 65535 || poisonAttack < 0 || paralysisAttack < 0) throw new IllegalArgumentException("invalid source NPC physical snapshot");
        switch (weakAttribute) {
            case -8: 
            case -4: 
            case -2: 
            case -1: 
            case 0: 
            case 1: 
            case 2: 
            case 4: 
            case 8: {
                break;
            }
            default: {
                throw new IllegalArgumentException("invalid source NPC physical snapshot");
            }
        }
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, 0, 0, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, undead, weakAttribute, magicResistance, false);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, false, false, 0, 1, 1);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, false);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity, boolean eraseEligible) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, eraseEligible, 0, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity, boolean eraseEligible, int ranged, int bowActionId) {
        return SourceNpcPhysicalStats.fromSourceRow(npcId, armorClass, size, damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, eraseEligible, ranged, bowActionId, 0, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(int npcId, int armorClass, String size, int damageReduction, boolean hard, int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible, int lawful, boolean tamable, boolean boss, int maxMp, int strength, int dexterity, boolean eraseEligible, int ranged, int bowActionId, int poisonAttack, int paralysisAttack) {
        String normalized;
        String string = normalized = size == null ? "" : size.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("small") && !normalized.equals("large")) {
            throw new IllegalArgumentException("unsupported source NPC size: " + size);
        }
        return new SourceNpcPhysicalStats(npcId, armorClass, normalized.equals("large"), damageReduction, hard, undead, weakAttribute, magicResistance, turnUndeadEligible, lawful, tamable, boss, maxMp, strength, dexterity, eraseEligible, ranged, bowActionId, poisonAttack, paralysisAttack);
    }

    static SourceNpcPhysicalStats memoryFixture(int npcId) {
        return switch (npcId) {
            case 60520 -> new SourceNpcPhysicalStats(npcId, -50, false, 0, false, 0, 0, 80, false, 0, false, false, 10, 60, 60, false, 10, 66);
            case 45001, 45002, 45003, 45004 -> new SourceNpcPhysicalStats(npcId, 0, false, 0, false, 0, 0, 0, false, 0, false, false, 0, 0, 0, false, 1, 0);
            case 20000, 20001 -> new SourceNpcPhysicalStats(npcId, -10, true, 0, false, 0, 0, 0, false, 0, false, false, 20, 0, 0, false, 0, 0);
            case 45262 -> new SourceNpcPhysicalStats(npcId, -4, true, 0, true, 0, 8, 0, false, -22, false, false, 10, 16, 15, true);
            case 45207 -> new SourceNpcPhysicalStats(npcId, -20, true, 0, false, 0, 8, 15, false, -25, true, false, 20, 17, 17, true);
            case 45157 -> new SourceNpcPhysicalStats(npcId, -4, false, 0, false, 1, 2, 13, true, -19, true, false, 30, 16, 16, true, 1, 0, 4, 0);
            case 45289 -> new SourceNpcPhysicalStats(npcId, -12, false, 0, false, 0, 2, 35, false, -40, true, false, 50, 17, 16, true);
            case 45259 -> new SourceNpcPhysicalStats(npcId, -15, true, 0, false, 0, 4, 5, false, -18, true, false, 30, 14, 12, true);
            case 45278 -> new SourceNpcPhysicalStats(npcId, -18, true, 0, false, 0, 0, 20, false, -34, true, false, 20, 20, 12, true);
            case 45322 -> new SourceNpcPhysicalStats(npcId, -25, false, 0, true, 2, 2, 40, false, -35, true, false, 50, 18, 12, true);
            case 45306 -> new SourceNpcPhysicalStats(npcId, -15, false, 0, false, 0, 0, 35, false, 10, false, false, 0, 16, 16, true, 1, 0);
            case 81215 -> new SourceNpcPhysicalStats(npcId, -3, false, 0, false, 0, 4, 5, false, 0, true, false, 70, 14, 14, true, 10, 66);
            case 81217 -> new SourceNpcPhysicalStats(npcId, 0, false, 0, false, 0, 0, 5, false, 0, true, false, 80, 16, 14, true, 1, 0, 1, 0);
            case 81227 -> new SourceNpcPhysicalStats(npcId, -5, false, 0, false, 0, 4, 20, false, 0, false, false, 110, 13, 16, true, 10, 66);
            case 81236 -> new SourceNpcPhysicalStats(npcId, -50, false, 0, false, 0, 0, 50, false, 0, false, false, 140, 15, 20, true, 3, 0, 2, 0);
            case 45390 -> new SourceNpcPhysicalStats(npcId, -20, false, 0, false, 0, 0, 0, false, -20, false, false, 20, 20, 10, true);
            case 45380 -> new SourceNpcPhysicalStats(npcId, -16, false, 0, false, 0, 0, 50, false, -39, false, false, 120, 17, 16, true);
            case 45601 -> new SourceNpcPhysicalStats(npcId, -75, false, 0, true, 3, 2, 150, false, -120, false, true, 250, 77, 17, false, 1, 0);
            case 45650 -> new SourceNpcPhysicalStats(npcId, -62, true, 0, true, 3, 0, 50, false, -180, false, true, 100, 67, 62, false, 2, 0);
            case 45161 -> new SourceNpcPhysicalStats(npcId, -3, false, 0, false, 1, 2, 18, true, -18, true, false, 30, 17, 16, true, 1, 0);
            case 45182 -> new SourceNpcPhysicalStats(npcId, -8, true, 0, true, 0, 8, 7, false, -14, true, false, 0, 46, 10, true);
            case 45812 -> new SourceNpcPhysicalStats(npcId, -23, false, 0, false, 0, 8, 35, false, -20, false, false, 500, 13, 13, true);
            case 45584 -> new SourceNpcPhysicalStats(npcId, -69, true, 0, true, 0, 2, 60, false, -110, false, true, 200, 72, 12, false, 1, 0);
            case 45610 -> new SourceNpcPhysicalStats(npcId, -67, true, 0, true, 0, 2, 70, false, -80, false, true, 350, 22, 18, false, 1, 0);
            case 45682 -> new SourceNpcPhysicalStats(npcId, -109, true, 0, true, 0, 2, 180, false, -500, false, true, 800, 85, 21, false, 2, 0);
            case 45683 -> new SourceNpcPhysicalStats(npcId, -109, true, 0, true, 0, 8, 180, false, -500, false, true, 5000, 83, 20, false, 2, 0);
            case 45684 -> new SourceNpcPhysicalStats(npcId, -109, true, 0, true, 0, 4, 180, false, -500, false, true, 4500, 83, 20, false, 2, 0);
            case 45065 -> new SourceNpcPhysicalStats(npcId, 8, false, 0, false, 1, 2, 5, true, -8, true, false, 12, 14, 6, true);
            case 81183, 81184, 81185, 81186, 81187, 81188 -> new SourceNpcPhysicalStats(npcId, 8, false, 0, false, 1, 2, 5, true, -8, true, false, 12, 16, 6, true);
            default -> new SourceNpcPhysicalStats(npcId, 10, false, 0, false);
        };
    }
}
