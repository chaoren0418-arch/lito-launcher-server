// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleCompanion;
import local.lito381.server.ClassicWorldMonsterCatalog;

record SourcePetTemplate(int npcId, String name, String nameId, int gfx, int sourceLevel, int baseMaxHp, int baseMaxMp, int armorClass, int strength, int dexterity, int moveIntervalMillis, int attackIntervalMillis, int collarCatalogId, int charismaCost, int tamingItemCatalogId, int tameRate, int hpUpMin, int hpUpMax, int mpUpMin, int mpUpMax, int minimumEvolutionLevel, int evolutionItemCatalogId, int evolutionNpcId, int constitution, int wisdom, int intelligence, int magicResistance, long baseExperience, int lawful, String size, int weakAttribute, int ranged, int bowActionId, int undead, int poisonAttack, int paralysisAttack, boolean aggressive, int damageReduction, boolean hard, boolean doppel, boolean boss, int lightSize, boolean changeHead) {
    SourcePetTemplate {
        if (npcId <= 0 || name == null || name.isBlank() || nameId == null || nameId.isBlank() || gfx <= 0 || gfx > 65535 || sourceLevel <= 0 || sourceLevel > 255 || baseMaxHp <= 0 || baseMaxMp < 0 || strength <= 0 || dexterity <= 0 || moveIntervalMillis <= 0 || attackIntervalMillis <= 0 || (long)collarCatalogId != 40314L && (long)collarCatalogId != 40316L || charismaCost <= 0 || tamingItemCatalogId < 0 || tameRate <= 0 || tameRate > 10000 || hpUpMin <= 0 || hpUpMax < hpUpMin || mpUpMin < 0 || mpUpMax < mpUpMin || minimumEvolutionLevel < 0 || minimumEvolutionLevel > 255 || minimumEvolutionLevel == 0 != (evolutionItemCatalogId == 0 && evolutionNpcId == 0) || minimumEvolutionLevel > 0 && (evolutionItemCatalogId <= 0 || evolutionNpcId <= 0) || constitution <= 0 || wisdom <= 0 || intelligence <= 0 || magicResistance < 0 || baseExperience < 0L || baseExperience > 0xFFFFFFFFL || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE || size == null || !size.equals("small") && !size.equals("large") || ranged < 0 || bowActionId < 0 || bowActionId > 65535 || undead < 0 || poisonAttack < 0 || paralysisAttack < 0 || damageReduction < 0 || lightSize < 0 || lightSize > 255) {
            throw new IllegalArgumentException("invalid selected-source pet template");
        }
    }

    boolean canEvolve() {
        return this.minimumEvolutionLevel > 0;
    }

    ClassicWorldMonsterCatalog.MonsterTemplate releasedMonsterTemplate(BattleCompanion.Snapshot pet) {
        if (pet == null || pet.npcId() != this.npcId || pet.level() <= 0 || pet.maxHp() <= 0 || pet.maxMp() < 0) {
            throw new IllegalArgumentException("released pet does not match source template");
        }
        return new ClassicWorldMonsterCatalog.MonsterTemplate(this.npcId, this.nameId, "L1Monster", this.gfx, pet.level(), pet.maxHp(), pet.maxMp(), this.armorClass, this.strength, this.constitution, this.dexterity, this.wisdom, this.intelligence, this.magicResistance, this.baseExperience, this.lawful, this.size, this.weakAttribute, this.ranged, this.moveIntervalMillis, this.attackIntervalMillis, this.bowActionId, this.undead, this.poisonAttack, this.paralysisAttack, this.aggressive, this.damageReduction, this.hard, this.doppel, this.boss, this.lightSize, this.changeHead);
    }

    static SourcePetTemplate memoryFixture(int npcId) {
        return switch (npcId) {
            case 45034 -> new SourcePetTemplate(45034, "\u7267\u7f8a\u72ac", "$907", 936, 5, 30, 5, 9, 15, 15, 560, 960, 40314, 6, 40057, 1500, 5, 8, 1, 2, 30, 40070, 45687, 12, 7, 7, 1, 26L, 0, "small", 0, 1, 0, 0, 0, 0, false, 0, false, false, false, 0, false);
            case 45687 -> new SourcePetTemplate(45687, "\u9ad8\u7b49\u7267\u7f8a\u72ac", "$2694", 3184, 5, 30, 5, 9, 15, 15, 560, 1000, 40316, 6, 0, 1500, 5, 8, 1, 2, 45, 41310, 46046, 12, 7, 7, 1, 26L, 0, "small", 0, 1, 0, 0, 0, 0, false, 0, false, false, false, 0, false);
            case 46046 -> new SourcePetTemplate(46046, "\u9ec3\u91d1\u9f8d", "$4076", 6396, 5, 60, 40, 30, 16, 16, 480, 800, 40316, 6, 0, 1500, 5, 10, 4, 8, 0, 0, 0, 16, 10, 15, 15, 26L, 0, "small", 0, 1, 0, 0, 0, 0, false, 0, false, false, false, 0, false);
            default -> throw new IllegalArgumentException("unknown memory pet NPC id: " + npcId);
        };
    }
}
