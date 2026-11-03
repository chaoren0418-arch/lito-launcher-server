package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class ModernCharacterDataTest {
    private static ModernCharacterData.PersistedValues values(long[] value) {
        return new ModernCharacterData.PersistedValues(value[0], value[1], value[2], value[3], value[4],
                value[5], value[6], value[7], value[8], value[9], value[10], value[11], value[12],
                value[13], value[14], value[15], value[16], value[17], value[18]);
    }
    private static long[] ordinaryValues() {
        return new long[] {100, 50, 101, 51, -200, 300, 301, 302, 303, 304, 305,
                4_000_000_000L, -32768, 100, 101, 102, 103, 104, 225};
    }
    private static ModernApplicationProtocol.CharacterRow character() {
        return new ModernApplicationProtocol.CharacterRow(3333, "synthetic-private-name", 1, 0, 4_000_000_000L);
    }
    private static ModernCharacterData.ItemTemplate template() {
        return new ModernCharacterData.ItemTemplate(100, ModernCharacterData.ItemKind.WEAPON,
                "$synthetic-private-token", 4_000_000_000L, Long.MAX_VALUE, Long.MAX_VALUE);
    }
    private static ModernCharacterData.InventoryItem item(long objectId) {
        return new ModernCharacterData.InventoryItem(objectId, template(), Long.MAX_VALUE,
                true, Long.MIN_VALUE, Long.MAX_VALUE, true, 255, false, Long.MAX_VALUE);
    }

    @Test void persistedValuesRetainI64SignedAcAndSavedVitalsAboveBaseMaxima() {
        long[] input = ordinaryValues();
        for (int index : new int[] {1,2,3,5,6,7,8,9,10,11,13,14,15,16,17}) input[index] = Long.MAX_VALUE;
        input[0] = 1; input[4] = Long.MIN_VALUE; input[12] = Long.MIN_VALUE;
        var wide = values(input);
        assertEquals(1, wide.maxHp()); assertEquals(Long.MAX_VALUE, wide.savedHp());
        assertTrue(wide.savedHp() > wide.maxHp());
        assertEquals(Long.MAX_VALUE, wide.maxMp()); assertEquals(Long.MAX_VALUE, wide.savedMp());
        assertEquals(Long.MIN_VALUE, wide.armorClass()); assertEquals(Long.MIN_VALUE, wide.lawful());
        assertEquals(Long.MAX_VALUE, wide.strength()); assertEquals(Long.MAX_VALUE, wide.dexterity());
        assertEquals(Long.MAX_VALUE, wide.constitution()); assertEquals(Long.MAX_VALUE, wide.intelligence());
        assertEquals(Long.MAX_VALUE, wide.wisdom()); assertEquals(Long.MAX_VALUE, wide.charisma());
        assertEquals(Long.MAX_VALUE, wide.experience()); assertEquals(Long.MAX_VALUE, wide.originalStrength());
        assertEquals(Long.MAX_VALUE, wide.originalDexterity()); assertEquals(Long.MAX_VALUE, wide.originalConstitution());
        assertEquals(Long.MAX_VALUE, wide.originalIntelligence()); assertEquals(Long.MAX_VALUE, wide.originalWisdom());
        input[1] = 0;
        assertTrue(values(input).savedMp() > values(input).maxMp(), "saved MP is not clamped to the base maximum");
        input[0] = Long.MAX_VALUE;
        assertEquals(Long.MAX_VALUE, values(input).maxHp());
    }

    @Test void foodUsesThePersisted225ScaleWithoutInterpretingItAsPercentOrByte255() {
        for (int[] pair : new int[][] {{0,0},{1,0},{112,49},{224,99},{225,100}}) {
            long[] input = ordinaryValues(); input[18] = pair[0];
            assertEquals(pair[0], values(input).food225()); assertEquals(pair[1], values(input).foodPercent());
        }
        for (long invalid : new long[] {-1,226,255,Long.MAX_VALUE}) {
            long[] input = ordinaryValues(); input[18] = invalid;
            assertThrows(IllegalArgumentException.class, () -> values(input));
        }
    }

    @Test void negativeUnsignedDomainFieldsAreRejectedRatherThanClamped() {
        for (int index : new int[] {0,1,2,3,5,6,7,8,9,10,11,13,14,15,16,17}) {
            long[] input = ordinaryValues(); input[index] = -1;
            assertThrows(IllegalArgumentException.class, () -> values(input));
        }
        long[] zeroMaximum = ordinaryValues(); zeroMaximum[0] = 0;
        assertThrows(IllegalArgumentException.class, () -> values(zeroMaximum));
        long[] positiveSigned = ordinaryValues(); positiveSigned[4] = Long.MAX_VALUE; positiveSigned[12] = Long.MAX_VALUE;
        assertEquals(Long.MAX_VALUE, values(positiveSigned).armorClass());
        assertEquals(Long.MAX_VALUE, values(positiveSigned).lawful());
    }

    @Test void itemInstancesPreserveWideCountsSignedEnchantmentAndSeparateFlags() {
        var item = item(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, item.objectId()); assertEquals(Long.MAX_VALUE, item.quantity());
        assertEquals(Long.MIN_VALUE, item.enchantment()); assertEquals(Long.MAX_VALUE, item.durability());
        assertEquals(255, item.blessing()); assertEquals(Long.MAX_VALUE, item.chargeCount());
        assertTrue(item.equipped()); assertTrue(item.identified()); assertFalse(item.sealed());
        assertEquals(4_000_000_000L, item.template().inventoryGraphic());
        assertEquals(Long.MAX_VALUE, item.template().groundGraphic());
        assertEquals(Long.MAX_VALUE, item.template().unitWeight());
        assertFalse(item.toString().contains("synthetic-private"));
        assertFalse(item.template().toString().contains("synthetic-private"));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.InventoryItem(0, template(), 1, false, 0, 0, false, 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.InventoryItem(1, template(), 0, false, 0, 0, false, 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.InventoryItem(1, template(), 1, false, 0, -1, false, 0, false, 0));
        for (long blessing : new long[] {-1,256,Long.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.InventoryItem(1, template(), 1, false, 0, 0, false, blessing, false, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.InventoryItem(1, template(), 1, false, 0, 0, false, 0, false, -1));
        assertThrows(NullPointerException.class, () -> new ModernCharacterData.InventoryItem(1, null, 1, false, 0, 0, false, 0, false, 0));
    }

    @Test void snapshotsAreImmutableSortedCompleteAndBoundedAt512Items() {
        var inventory = new ArrayList<>(LongStream.rangeClosed(1, 512).mapToObj(ModernCharacterDataTest::item).toList());
        var snapshot = new ModernCharacterData.Snapshot(7, character(), values(ordinaryValues()), inventory);
        inventory.clear();
        assertEquals(512, snapshot.inventory().size());
        assertEquals(512, snapshot.inventory().get(511).objectId());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.inventory().clear());
        assertFalse(snapshot.toString().contains("synthetic-private"));
        assertTrue(new ModernCharacterData.Snapshot(7, character(), values(ordinaryValues()), List.of()).inventory().isEmpty());
        for (var invalid : List.of(List.of(item(1), item(1)), List.of(item(2), item(1)),
                LongStream.rangeClosed(1, 513).mapToObj(ModernCharacterDataTest::item).toList())) {
            assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.Snapshot(7, character(), values(ordinaryValues()), invalid));
        }
    }

    @Test void characterIdentityAndSourceMustBeValidBeforePublishingASnapshot() {
        for (int source : new int[] {0,-1}) {
            assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.Snapshot(source, character(), values(ordinaryValues()), List.of()));
        }
        for (var character : List.of(new ModernApplicationProtocol.CharacterRow(0, "N", 1, 0, 1),
                new ModernApplicationProtocol.CharacterRow(-1, "N", 1, 0, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", -1, 0, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", 65536, 0, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", 1, 2, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", 1, 0, 0))) {
            assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.Snapshot(7, character, values(ordinaryValues()), List.of()));
        }
    }

    @Test void textRejectsMalformedUnicodeControlsAndUnicodeBlankNamesWithUtf8ByteBounds() {
        for (String invalid : new String[] {null,""," ","\t","\n","\0","x\u007fy","x\u0085y",
                "\ud800","\udc00","\u00a0","\u2007","\u202f","界".repeat(86),"x".repeat(257)}) {
            assertFalse(ModernCharacterData.validText(invalid), "invalid names must not reach a later wire encoder");
            assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.ItemTemplate(1,
                    ModernCharacterData.ItemKind.ETC, invalid, 0, 0, 0));
        }
        assertTrue(ModernCharacterData.validText("界".repeat(85)));
        assertTrue(ModernCharacterData.validText("\ud83d\ude00".repeat(64)));
        assertTrue(ModernCharacterData.validText("$123"));
    }

    @Test void templateIdentifiersAndMetadataNeverAcceptNegativeOrMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.ItemTemplate(0, ModernCharacterData.ItemKind.ETC, "N", 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.ItemTemplate(1, null, "N", 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.ItemTemplate(1, ModernCharacterData.ItemKind.ETC, "N", -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.ItemTemplate(1, ModernCharacterData.ItemKind.ETC, "N", 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterData.ItemTemplate(1, ModernCharacterData.ItemKind.ETC, "N", 0, 0, -1));
    }
}
