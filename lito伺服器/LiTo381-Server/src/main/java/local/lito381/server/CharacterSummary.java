// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleNaturalHpRegen;

record CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, long lastPkEpochSeconds, int pkCountForElf, long lastPkForElfEpochSeconds, long classId, long locationX, long locationY, long mapId, int originalConstitution, int food, int constitutionHprMaximum, int originalStrength, int originalDexterity, int elfAttribute) {
    CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, int pkCountForElf, long lastPkForElfEpochSeconds, long classId, long locationX, long locationY, long mapId, int originalConstitution, int food, int constitutionHprMaximum, int originalStrength, int originalDexterity, int elfAttribute) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass, strength, constitution, dexterity, charisma, intelligence, wisdom, originalIntelligence, originalWisdom, objectId, experience, lawful, pkCount, 0L, pkCountForElf, lastPkForElfEpochSeconds, classId, locationX, locationY, mapId, originalConstitution, food, constitutionHprMaximum, originalStrength, originalDexterity, elfAttribute);
    }

    CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, long classId, long locationX, long locationY, long mapId, int originalConstitution, int food, int constitutionHprMaximum, int originalStrength, int originalDexterity, int elfAttribute) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass, strength, constitution, dexterity, charisma, intelligence, wisdom, originalIntelligence, originalWisdom, objectId, experience, lawful, pkCount, 0L, 0, 0L, classId, locationX, locationY, mapId, originalConstitution, food, constitutionHprMaximum, originalStrength, originalDexterity, elfAttribute);
    }

    CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, long classId, long locationX, long locationY, long mapId, int originalConstitution, int food, int constitutionHprMaximum, int originalStrength, int originalDexterity) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass, strength, constitution, dexterity, charisma, intelligence, wisdom, originalIntelligence, originalWisdom, objectId, experience, lawful, pkCount, classId, locationX, locationY, mapId, originalConstitution, food, constitutionHprMaximum, originalStrength, originalDexterity, 0);
    }

    CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, long classId, long locationX, long locationY, long mapId, int originalConstitution, int food, int constitutionHprMaximum, int originalStrength) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass, strength, constitution, dexterity, charisma, intelligence, wisdom, originalIntelligence, originalWisdom, objectId, experience, lawful, pkCount, classId, locationX, locationY, mapId, originalConstitution, food, constitutionHprMaximum, originalStrength, dexterity);
    }

    CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, long classId, long locationX, long locationY, long mapId, int originalConstitution, int food, int constitutionHprMaximum) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass, strength, constitution, dexterity, charisma, intelligence, wisdom, originalIntelligence, originalWisdom, objectId, experience, lawful, pkCount, classId, locationX, locationY, mapId, originalConstitution, food, constitutionHprMaximum, strength, dexterity);
    }

    CharacterSummary(String name, String clanName, int type, int sex, int level, int maxHp, int maxMp, int armorClass, int strength, int constitution, int dexterity, int charisma, int intelligence, int wisdom, int originalIntelligence, int originalWisdom, long objectId, long experience, long lawful, int pkCount, long classId, long locationX, long locationY, long mapId) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass, strength, constitution, dexterity, charisma, intelligence, wisdom, originalIntelligence, originalWisdom, objectId, experience, lawful, pkCount, classId, locationX, locationY, mapId, constitution, 40, BattleNaturalHpRegen.fallbackConHprMaximum(constitution), strength, dexterity);
    }

    CharacterSummary {
        if (originalConstitution < 0 || originalStrength < 0 || originalDexterity < 0 || food < 0 || constitutionHprMaximum < 1 || pkCount < 0 || lastPkEpochSeconds < 0L || pkCountForElf < 0 || lastPkForElfEpochSeconds < 0L || elfAttribute < 0 || (elfAttribute & 0xFFFFFFF0) != 0) {
            throw new IllegalArgumentException("invalid natural-HP-regeneration character fields");
        }
    }
}
