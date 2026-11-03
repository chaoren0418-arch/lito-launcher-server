package local.lito381.server;

/**
 * Lossless server state projected into the fixed-width fields understood by
 * the selected LiTo 3.81C client.  Database values remain authoritative; only
 * their native wire representation is saturated here.
 */
final class NativeCharacterStats {
    static final int MAX_DIRECTORY_VITAL = Short.MAX_VALUE;
    static final int MAX_DIRECTORY_LEVEL = Byte.MAX_VALUE;
    static final int MAX_DIRECTORY_ABILITY = Byte.MAX_VALUE;
    static final int MAX_RUNTIME_VITAL = Short.MAX_VALUE;
    static final int MAX_RUNTIME_ABILITY = 0xFE;

    private NativeCharacterStats() {}

    static DirectoryProjection directoryProjection(CharacterSummary character) {
        if (character == null) {
            throw new IllegalArgumentException("missing character status source");
        }
        return new DirectoryProjection(
                clamp(character.maxHp(), 1, MAX_DIRECTORY_VITAL),
                clamp(character.maxMp(), 1, MAX_DIRECTORY_VITAL),
                signedByte(character.armorClass()),
                clamp(character.level(), 1, MAX_DIRECTORY_LEVEL),
                clamp(character.strength(), 0, MAX_DIRECTORY_ABILITY),
                clamp(character.dexterity(), 0, MAX_DIRECTORY_ABILITY),
                clamp(character.constitution(), 0, MAX_DIRECTORY_ABILITY),
                clamp(character.wisdom(), 0, MAX_DIRECTORY_ABILITY),
                clamp(character.charisma(), 0, MAX_DIRECTORY_ABILITY),
                clamp(character.intelligence(), 0, MAX_DIRECTORY_ABILITY));
    }

    static int runtimeVital(long value) {
        return NumericBreakthrough.clampVital(value);
    }

    static int runtimeAbility(long value) {
        return NumericBreakthrough.clampAbility(value);
    }

    static int signedByte(long value) {
        return clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    static int nativeLevel(long value) {
        int maximum = NumericBreakthrough.levelAbilityEnabled()
                ? NumericBreakthrough.MAX_LEVEL : 0xFF;
        return clamp(value, 1, maximum);
    }

    static long nativeExperience(long value) {
        return Math.max(0L, Math.min(0xFFFF_FFFFL, value));
    }

    private static int clamp(long value, int minimum, int maximum) {
        return (int) Math.max((long) minimum, Math.min((long) maximum, value));
    }

    record DirectoryProjection(
            int maxHp, int maxMp, int armorClass, int level,
            int strength, int dexterity, int constitution, int wisdom,
            int charisma, int intelligence) {
        boolean adjusted(CharacterSummary source) {
            return maxHp != source.maxHp() || maxMp != source.maxMp()
                    || armorClass != source.armorClass() || level != source.level()
                    || strength != source.strength() || dexterity != source.dexterity()
                    || constitution != source.constitution() || wisdom != source.wisdom()
                    || charisma != source.charisma()
                    || intelligence != source.intelligence();
        }

        String auditSummary(CharacterSummary source) {
            return "hp=" + source.maxHp() + "->" + maxHp
                    + " mp=" + source.maxMp() + "->" + maxMp
                    + " ac=" + source.armorClass() + "->" + armorClass
                    + " level=" + source.level() + "->" + level
                    + " str=" + source.strength() + "->" + strength
                    + " dex=" + source.dexterity() + "->" + dexterity
                    + " con=" + source.constitution() + "->" + constitution
                    + " wis=" + source.wisdom() + "->" + wisdom
                    + " cha=" + source.charisma() + "->" + charisma
                    + " int=" + source.intelligence() + "->" + intelligence;
        }
    }
}
