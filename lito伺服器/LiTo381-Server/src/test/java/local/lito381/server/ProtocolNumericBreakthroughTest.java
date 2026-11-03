package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class ProtocolNumericBreakthroughTest {
    private static final String PROPERTY = "lito.numeric.breakthrough";

    @AfterEach
    void restoreDefault() {
        System.clearProperty(PROPERTY);
    }

    @Test
    void nativeVitalPacketsRemainWordsWhenCapabilityIsAbsent() {
        System.setProperty(PROPERTY, "0");
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_HIT_POINT, 0x34, 0x12, 0x67, 0x45
        }, new Protocol.Writer().c(Protocol.S_HIT_POINT)
                .h(0x1234).h(0x4567).bytes());
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MANA_POINT, 0x34, 0x12, 0x67, 0x45
        }, Protocol.manaPoint(0x1234, 0x4567));
    }

    @Test
    void negotiatedVitalPacketsUseLittleEndianDwords() {
        System.setProperty(PROPERTY, "1");
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_HIT_POINT,
                0x40, 0x0D, 0x03, 0x00,
                0x3F, 0x42, 0x0F, 0x00
        }, new Protocol.Writer().c(Protocol.S_HIT_POINT)
                .h(200_000).h(999_999).bytes());
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MANA_POINT,
                0x40, 0x0D, 0x03, 0x00,
                0x3F, 0x42, 0x0F, 0x00
        }, Protocol.manaPoint(200_000, 999_999));
    }

    @Test
    void negotiatedStatusUsesCurrentMaxOrderingAndDwordArmorClass() {
        System.setProperty(PROPERTY, "1");
        byte[] packet = Protocol.characterStatus(
                1, 80, 2, 18, 19, 20, 21, 22, 23,
                200_000, 150_000, 999_999, 888_888,
                -600, 40, 0, 0, 0, 0, 0, 0);
        Protocol.Reader reader = new Protocol.Reader(packet);
        assertEquals(Protocol.S_STATUS, reader.c());
        assertEquals(1, reader.d());
        assertEquals(80, reader.c());
        assertEquals(2, reader.d());
        for (int expected : new int[] {18, 22, 23, 20, 19, 21}) {
            assertEquals(expected, reader.c());
        }
        assertEquals(200_000, reader.d());
        assertEquals(999_999, reader.d());
        assertEquals(150_000, reader.d());
        assertEquals(888_888, reader.d());
        assertEquals(Integer.toUnsignedLong(-600), reader.d());
    }

    @Test
    void negotiatedArmorClassWidensOnlyTheBaseField() {
        System.setProperty(PROPERTY, "1");
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_AC, (byte) 0xA8, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF,
                1, 2, 3, 4
        }, Protocol.armorClass(-600, 1, 2, 3, 4));
    }

    @Test
    void schema2WidensSixAbilitiesAndPublishesFullLevelThroughExperience() {
        System.setProperty(PROPERTY, "2");
        assertEquals(2, NumericBreakthrough.schemaVersion());
        assertEquals(345, NativeCharacterStats.nativeLevel(345));
        assertEquals(456, NativeCharacterStats.runtimeAbility(456));
        assertEquals(999_999, NumericBreakthrough.clampAbility(1_200_000));
        assertEquals(345, NumericBreakthrough.clientStatusLevel(345, 99));
        assertEquals(80, NumericBreakthrough.clientStatusLevel(80, 80));

        Protocol.Reader status = new Protocol.Reader(Protocol.characterStatus(
                1, 345, 2, 301, 302, 303, 304, 305, 306,
                200_000, 150_000, 999_999, 888_888,
                -600, 40, 0, 0, 0, 0, 0, 0));
        assertEquals(Protocol.S_STATUS, status.c());
        assertEquals(1, status.d());
        assertEquals(0xFF, status.c(), "S_STATUS retains the native level shadow");
        assertEquals(2, status.d());
        for (int expected : new int[] {301, 305, 306, 303, 302, 304}) {
            assertEquals(expected, status.d());
        }
        assertEquals(200_000, status.d());
        assertEquals(999_999, status.d());
        assertEquals(150_000, status.d());
        assertEquals(888_888, status.d());
        assertEquals(Integer.toUnsignedLong(-600), status.d());

        Protocol.Reader abilities = new Protocol.Reader(Protocol.abilityScores(
                301, 302, 303, 304, 305, 306, 190));
        assertEquals(Protocol.S_ABILITY_SCORES, abilities.c());
        for (int expected : new int[] {301, 305, 306, 303, 302, 304}) {
            assertEquals(expected, abilities.d());
        }
        assertEquals(Protocol.nativeWeightGauge(190), abilities.c(),
                "final S_ABILITY_SCORES byte is the native 0..29 weight gauge");
        assertEquals(0, abilities.remaining());

        Protocol.Reader experience = new Protocol.Reader(Protocol.experience(345, 123_456));
        assertEquals(Protocol.S_EXP, experience.c());
        assertEquals(345, experience.d());
        assertEquals(123_456, experience.d());
        assertEquals(0, experience.remaining());

        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MAGE_STRENGTH, 10, 0,
                (byte) NativeCharacterStats.MAX_RUNTIME_ABILITY, 20, 5
        }, Protocol.mageStrength(10, 301, 20, 5));
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MAGE_DEXTERITY, 10, 0,
                (byte) NativeCharacterStats.MAX_RUNTIME_ABILITY, 5
        }, Protocol.mageDexterity(10, 303, 5));
    }

    @Test
    void schema3WidensSpellPowerAndMagicResistanceModifiers() {
        System.setProperty(PROPERTY, "3");
        assertEquals(3, NumericBreakthrough.schemaVersion());
        assertEquals(999_999, NumericBreakthrough.clampMagicModifier(1_200_000));
        assertEquals(-999_999, NumericBreakthrough.clampMagicModifier(-1_200_000));
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MAGIC_STATUS,
                0x2C, 0x01, 0x00, 0x00,
                (byte) 0xD4, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF
        }, Protocol.magicStatus(300, -300));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.magicStatus(1_000_000, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.magicStatus(0, -1_000_000));
    }

    @Test
    void schema2KeepsNativeMagicStatusSignedBytes() {
        System.setProperty(PROPERTY, "2");
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MAGIC_STATUS, (byte) -128, 127
        }, Protocol.magicStatus(-128, 127));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.magicStatus(128, 0));
    }

    @Test
    void schema4WidensElementalResistancesInBothStatusPackets() {
        System.setProperty(PROPERTY, "4");
        assertEquals(4, NumericBreakthrough.schemaVersion());
        assertEquals(999_999,
                NumericBreakthrough.clampElementalResistance(1_200_000));
        assertEquals(-999_999,
                NumericBreakthrough.clampElementalResistance(-1_200_000));

        Protocol.Reader status = new Protocol.Reader(Protocol.characterStatus(
                1, 345, 2, 301, 302, 303, 304, 305, 306,
                200_000, 150_000, 999_999, 888_888,
                -600, 40, 0, 0, 300, -301, 302, -303));
        assertEquals(Protocol.S_STATUS, status.c());
        assertEquals(1, status.d());
        assertEquals(0xFF, status.c());
        assertEquals(2, status.d());
        for (int expected : new int[] {301, 305, 306, 303, 302, 304}) {
            assertEquals(expected, status.d());
        }
        assertEquals(200_000, status.d());
        assertEquals(999_999, status.d());
        assertEquals(150_000, status.d());
        assertEquals(888_888, status.d());
        assertEquals(Integer.toUnsignedLong(-600), status.d());
        assertEquals(0, status.d());
        assertEquals(Protocol.nativeFoodGauge(40), status.c());
        assertEquals(0, status.c());
        assertEquals(0, status.h());
        assertEquals(300, status.d());
        assertEquals(Integer.toUnsignedLong(-301), status.d());
        assertEquals(302, status.d());
        assertEquals(Integer.toUnsignedLong(-303), status.d());
        assertEquals(0, status.remaining());

        assertArrayEquals(new byte[] {
                (byte) Protocol.S_AC,
                (byte) 0xA8, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF,
                0x2C, 0x01, 0x00, 0x00,
                (byte) 0xD3, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF,
                0x2E, 0x01, 0x00, 0x00,
                (byte) 0xD1, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF
        }, Protocol.armorClass(-600, 300, -301, 302, -303));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.armorClass(0, 1_000_000, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.characterStatus(
                        1, 1, 0, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, -1_000_000));
    }

    @Test
    void schema3KeepsElementalResistanceFieldsAsSignedBytes() {
        System.setProperty(PROPERTY, "3");
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_AC,
                (byte) 0xA8, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF,
                (byte) -128, 127, -1, 1
        }, Protocol.armorClass(-600, -128, 127, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.armorClass(0, 128, 0, 0, 0));
    }

    @Test
    void invalidSchemaNegotiationFailsClosed() {
        System.setProperty(PROPERTY, "5");
        assertEquals(0, NumericBreakthrough.schemaVersion());
        assertEquals(0xFF, NativeCharacterStats.nativeLevel(345));
        assertEquals(NativeCharacterStats.MAX_RUNTIME_ABILITY,
                NativeCharacterStats.runtimeAbility(456));
        assertEquals(NativeCharacterStats.MAX_RUNTIME_ABILITY,
                NumericBreakthrough.clampAbility(456));
        assertEquals(99, NumericBreakthrough.clientStatusLevel(345, 99));
    }
}
