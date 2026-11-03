package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NativeCharacterStatsTest {
    @Test void saturatesDatabaseStatsToExactNativeDirectoryWidths() {
        CharacterSummary source = character(
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
                Integer.MAX_VALUE, -10, 400, 128);

        NativeCharacterStats.DirectoryProjection projected =
                NativeCharacterStats.directoryProjection(source);

        assertEquals(32767, projected.maxHp());
        assertEquals(32767, projected.maxMp());
        assertEquals(-128, projected.armorClass());
        assertEquals(127, projected.level());
        assertEquals(127, projected.strength());
        assertEquals(0, projected.dexterity());
        assertEquals(127, projected.constitution());
        assertEquals(0, projected.wisdom());
        assertEquals(127, projected.charisma());
        assertEquals(127, projected.intelligence());
        assertTrue(projected.adjusted(source));

        byte[] packet = Protocol.litoCharacterDirectoryRow(
                new Protocol.LitoCharacterDirectoryRow(
                        source.name(), source.clanName(), source.type(), source.sex(), 0,
                        projected.maxHp(), projected.maxMp(), projected.armorClass(),
                        projected.level(), projected.strength(), projected.dexterity(),
                        projected.constitution(), projected.wisdom(), projected.charisma(),
                        projected.intelligence(), 0));
        assertEquals(Protocol.S_LITO_CHARACTER_INFO, packet[0] & 0xFF);
    }

    @Test void saturatesRuntimeAdditionWithoutIntegerWraparound() {
        assertEquals(32767, NativeCharacterStats.runtimeVital(
                (long) Integer.MAX_VALUE + 500));
        assertEquals(1, NativeCharacterStats.runtimeVital(Integer.MIN_VALUE));
        assertEquals(254, NativeCharacterStats.runtimeAbility(
                (long) Integer.MAX_VALUE + 500));
        assertEquals(1, NativeCharacterStats.runtimeAbility(Integer.MIN_VALUE));
        assertEquals(127, NativeCharacterStats.signedByte(Integer.MAX_VALUE));
        assertEquals(-128, NativeCharacterStats.signedByte(Integer.MIN_VALUE));
        assertEquals(0xFFFF_FFFFL,
                NativeCharacterStats.nativeExperience(Long.MAX_VALUE));
        assertEquals(0L, NativeCharacterStats.nativeExperience(Long.MIN_VALUE));
    }

    @Test void statusWriterRejectsArmorClassWraparound() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.characterStatus(
                1, 99, 0, 127, 127, 127, 127, 127, 127,
                32767, 32767, 32767, 32767,
                128, 40, 0, 0, 0, 0, 0, 0));
    }

    private static CharacterSummary character(
            int maxHp, int maxMp, int armorClass, int level,
            int strength, int dexterity, int constitution, int wisdom,
            int charisma, int intelligence) {
        return new CharacterSummary(
                "Overflow", "", 1, 0, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                10, 10, 1, 0, 0,
                0, 0, 0, 0,
                0, 33000, 33000, 4,
                10, 40, 10, 10, 10, 0);
    }
}
