package local.lito381.server;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SourceEquipmentMagicStatsTest {
    @Test void reproducesDirectPlusLateSetMagicHitWithoutUsingSpellPower() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(4, 7);

        assertEquals(11, stats.totalMagicHit());
        assertEquals(0, stats.itemSpellPower());
    }

    @Test void retainsHealingHelmIdentityWithoutConflatingItWithMagicHit() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(0, 0, true);

        assertEquals(true, stats.healingHelmEquipped());
        assertEquals(0, stats.totalMagicHit());
        assertEquals(false, stats.hasteCostHalvingHelmEquipped());
    }

    @Test void pinsOnlyTheTwoRetainedHasteCostHelmets() {
        assertEquals(true, LegacyEquipmentMagicLoader.halvesHasteCost(20008));
        assertEquals(true, LegacyEquipmentMagicLoader.halvesHasteCost(20013));
        assertEquals(false, LegacyEquipmentMagicLoader.halvesHasteCost(20014));
        assertEquals(false, LegacyEquipmentMagicLoader.halvesHasteCost(20015));
        assertEquals(false, LegacyEquipmentMagicLoader.halvesHasteCost(20023));
        assertEquals(true, LegacyEquipmentMagicLoader.halvesGreaterHasteCost(20023));
        assertEquals(false, LegacyEquipmentMagicLoader.halvesGreaterHasteCost(20008));
        assertEquals(false, LegacyEquipmentMagicLoader.halvesGreaterHasteCost(20013));
    }

    @Test void reproducesDirectPlusLateSetBlindResistanceAndNativeClamp() {
        SourceEquipmentMagicStats ordinary =
                new SourceEquipmentMagicStats(4, 7, false, 9, 11);
        assertEquals(11, ordinary.totalMagicHit());
        assertEquals(20, ordinary.totalBlindResistance());

        SourceEquipmentMagicStats upper =
                new SourceEquipmentMagicStats(0, 0, false, 120, 20);
        SourceEquipmentMagicStats lower =
                new SourceEquipmentMagicStats(0, 0, false, -120, -20);
        assertEquals(127, upper.totalBlindResistance());
        assertEquals(-128, lower.totalBlindResistance());
    }

    @Test void reproducesDirectPlusLateSetSleepResistanceAndNativeClamp() {
        SourceEquipmentMagicStats ordinary =
                new SourceEquipmentMagicStats(4, 7, false, 0, 0, 9, 11);
        assertEquals(20, ordinary.totalSleepResistance());
        SourceEquipmentMagicStats upper =
                new SourceEquipmentMagicStats(0, 0, false, 0, 0, 120, 20);
        SourceEquipmentMagicStats lower =
                new SourceEquipmentMagicStats(0, 0, false, 0, 0, -120, -20);
        assertEquals(127, upper.totalSleepResistance());
        assertEquals(-128, lower.totalSleepResistance());
    }

    @Test void reproducesFreezeAndStoneDirectPlusSetResistanceWithNativeClamp() {
        SourceEquipmentMagicStats ordinary = new SourceEquipmentMagicStats(
                4, 7, false, 0, 0, 0, 0, 9, 11, 13, 17);
        assertEquals(20, ordinary.totalFreezeResistance());
        assertEquals(30, ordinary.totalStoneResistance());
        SourceEquipmentMagicStats upper = new SourceEquipmentMagicStats(
                0, 0, false, 0, 0, 0, 0, 120, 20, 120, 20);
        SourceEquipmentMagicStats lower = new SourceEquipmentMagicStats(
                0, 0, false, 0, 0, 0, 0, -120, -20, -120, -20);
        assertEquals(127, upper.totalFreezeResistance());
        assertEquals(127, upper.totalStoneResistance());
        assertEquals(-128, lower.totalFreezeResistance());
        assertEquals(-128, lower.totalStoneResistance());
    }

    @Test void reproducesSustainDirectPlusSetResistanceWithNativeClamp() {
        SourceEquipmentMagicStats ordinary = new SourceEquipmentMagicStats(
                4, 7, false, 0, 0, 0, 0, 0, 0, 0, 0, 9, 11);
        SourceEquipmentMagicStats upper = new SourceEquipmentMagicStats(
                0, 0, false, 0, 0, 0, 0, 0, 0, 0, 0, 120, 20);
        SourceEquipmentMagicStats lower = new SourceEquipmentMagicStats(
                0, 0, false, 0, 0, 0, 0, 0, 0, 0, 0, -120, -20);
        assertEquals(20, ordinary.totalSustainResistance());
        assertEquals(127, upper.totalSustainResistance());
        assertEquals(-128, lower.totalSustainResistance());
    }

    @Test void reproducesShockStunResistanceClampAndUnclampedSuccessSum() {
        SourceEquipmentMagicStats ordinary =
                new SourceEquipmentMagicStats(4, 7, 9, 11, 13, 17);
        SourceEquipmentMagicStats upper =
                new SourceEquipmentMagicStats(0, 0, 120, 20, 120, 20);
        SourceEquipmentMagicStats lower =
                new SourceEquipmentMagicStats(0, 0, -120, -20, -120, -20);
        assertEquals(11, ordinary.totalMagicHit());
        assertEquals(20, ordinary.totalStunResistance());
        assertEquals(30, ordinary.totalStunSuccess());
        assertEquals(127, upper.totalStunResistance());
        assertEquals(140, upper.totalStunSuccess());
        assertEquals(-128, lower.totalStunResistance());
        assertEquals(-140, lower.totalStunSuccess());
    }

    @Test void separatesDirectItemAndLateSetLayersExactly() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(
                2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13,
                4, 3, 1, 2, 1, 1);

        assertEquals(31, stats.effectiveIntelligence(20));
        assertEquals(35, stats.magicIntelligenceTerm(20));
        assertEquals(15, stats.totalDirectMagicResistance());
        assertEquals(17, stats.totalMagicDamage());
        assertEquals(19, stats.totalFireResistance());
        assertEquals(21, stats.armorReductionBase());
    }

    @Test void clampsOnlyGetIntWhileLeavingItemSpAsAnUncappedTerm() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(
                300, 0, 25, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                1, 0, 1, 0, 0, 0);
        assertEquals(254, stats.effectiveIntelligence(10));
        assertEquals(279, stats.magicIntelligenceTerm(10));
    }

    @Test void carriesDirectAndLateSetHprThroughTheSameEquipmentReplay() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                7, 11,
                2, 1, 1, 0, 1, 0);
        assertEquals(18, stats.totalHpRegen());
    }

    @Test void carriesItemAndSetWisdomAndMprIntoNaturalMpRegen() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(
                0, 2, 0, 0, 0, 0, 0, 0,
                0, 3, 0, 0, 0, 0, 0,
                0, 0, 4, 5,
                2, 1, 0, 0, 1, 0,
                0, 0, 0, 0, 0);
        assertEquals(18, stats.effectiveMprWisdom(13));
        assertEquals(9, stats.totalMpRegen());
    }

    @Test void carriesDirectAndLateSetConIntoTheNaturalHprRollTable() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(
                0, 0, 4, 0, 0, 0, 0, 0,
                0, 3, 0, 0, 0, 0,
                0, 0,
                2, 1, 0, 0, 1, 0);
        assertEquals(25, stats.effectiveConstitution(18));
        assertEquals(6, BattleNaturalHpRegen.fallbackConHprMaximum(
                stats.effectiveConstitution(18)));
    }

    @Test void validatesSnapshotCounts() {
        assertThrows(IllegalArgumentException.class, () -> new SourceEquipmentMagicStats(
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                1, 2, 0, 0, 0, 0));
    }

    @Test void reproducesPerItemIntWisAndConByteCastAtEquipTime() {
        assertEquals(127, LegacyEquipmentMagicLoader.signedByte(127));
        assertEquals(-128, LegacyEquipmentMagicLoader.signedByte(128));
        assertEquals(-126, LegacyEquipmentMagicLoader.signedByte(130));
        assertEquals(127, LegacyEquipmentMagicLoader.signedByte(-129));
    }

    @Test void negotiatedAbilityLoaderPreservesWideDatabaseDeltasWithoutWrap() {
        String previous = System.getProperty("lito.numeric.breakthrough");
        try {
            System.clearProperty("lito.numeric.breakthrough");
            assertEquals(127, LegacyEquipmentMagicLoader.appliedAbilityModifier(300));
            assertEquals(-128, LegacyEquipmentMagicLoader.appliedAbilityModifier(-300));

            System.setProperty("lito.numeric.breakthrough", "4");
            assertEquals(300, LegacyEquipmentMagicLoader.appliedAbilityModifier(300));
            assertEquals(-300, LegacyEquipmentMagicLoader.appliedAbilityModifier(-300));
            assertEquals(300,
                    LegacyEquipmentMagicLoader.addAppliedAbilityModifier(200, 100));
            assertEquals(-300,
                    LegacyEquipmentMagicLoader.addAppliedAbilityModifier(-200, -100));
            assertEquals(999_999,
                    LegacyEquipmentMagicLoader.appliedAbilityModifier(1_500_000));
            assertEquals(-999_999,
                    LegacyEquipmentMagicLoader.appliedAbilityModifier(-1_500_000));
        } finally {
            if (previous == null) System.clearProperty("lito.numeric.breakthrough");
            else System.setProperty("lito.numeric.breakthrough", previous);
        }
    }

    @Test void importedExtendedStatsSaturateInsteadOfWrappingOrThrowing() {
        assertEquals(999_999,
                LegacyEquipmentMagicLoader.addExtendedValue(
                        Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(-999_999,
                LegacyEquipmentMagicLoader.addExtendedValue(
                        Integer.MIN_VALUE, Integer.MIN_VALUE));

        SourceEquipmentVitalStats vitals = new SourceEquipmentVitalStats(
                Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
        assertEquals(999_999, vitals.totalMaximumHp());
        assertEquals(-999_999, vitals.totalMaximumMp());

        String previous = System.getProperty("lito.numeric.breakthrough");
        try {
            System.setProperty("lito.numeric.breakthrough", "4");
            assertEquals(999_999, vitals.effectiveCharisma(Integer.MAX_VALUE));
            assertEquals(999_999,
                    new SourceEquipmentPhysicalStats(
                            SourceWeaponPhysicalStats.none(),
                            0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0,
                            Integer.MAX_VALUE, Integer.MIN_VALUE)
                            .armorClassModifier());
        } finally {
            if (previous == null) System.clearProperty("lito.numeric.breakthrough");
            else System.setProperty("lito.numeric.breakthrough", previous);
        }
    }

    @Test void nativeArmorTooltipSaturatesWideAbilityAndElementShadows() {
        InventoryCatalog.NativeArmorStatus status =
                new InventoryCatalog.NativeArmorStatus(
                        1, 0, 0, 0, -1,
                        300, -300, 0, 0, 0, 0,
                        0, 0, 0, 300, false,
                        300, -300, 0, 0,
                        0, 0, 0, 0, 0, 0, 2);

        byte[] detail = status.detail(2, 1, 0, 0);
        assertEquals(true, containsPair(detail, 0x08, 0x7F));
        assertEquals(true, containsPair(detail, 0x09, 0x80));
        assertEquals(true, containsPair(detail, 0x11, 0x7F));
        assertEquals(true, containsPair(detail, 0x1B, 0x7F));
        assertEquals(true, containsPair(detail, 0x1C, 0x80));
    }

    private static boolean containsPair(byte[] bytes, int first, int second) {
        for (int index = 0; index + 1 < bytes.length; index++) {
            if ((bytes[index] & 0xFF) == first
                    && (bytes[index + 1] & 0xFF) == second) return true;
        }
        return false;
    }

    @Test void carriesStrengthAndEveryEquipmentWeightReductionLayer() {
        SourceEquipmentMagicStats stats = new SourceEquipmentMagicStats(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                2, 1, 0, 0, 1, 0,
                3, 4, 5, 6, 7);
        assertEquals(22, stats.effectiveStrength(13));
        assertEquals(16, stats.totalWeightReduction());
    }

    @Test void layersEquippedRentalEffectsWithoutChangingSourceProvenance() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(
                115L, 1, 20019L, 1, 20107L, 1, 42092L, 1));
        for (RuntimeInventory.Item item : inventory.items()) {
            if (item.definition().weapon()) inventory.toggleWeapon(item.objectId());
            else assertEquals(1, inventory.toggleNativeArmor(item.objectId()).size());
        }
        SourceEquipmentMagicStats source = SourceEquipmentMagicStats.none();
        SourceEquipmentMagicStats effective = source.withRentalEquipment(
                inventory.equippedRentalBonuses());

        assertEquals(14, effective.effectiveStrength(13));
        assertEquals(14, effective.effectiveMprWisdom(13));
        assertEquals(25, effective.totalMpRegen());
        assertEquals(2, effective.armorReductionBase());
        assertEquals(0, effective.physical().itemDexterity());
        assertEquals(source.matchedEquipmentRows(), effective.matchedEquipmentRows());
        assertEquals(source.matchedArmorRows(), effective.matchedArmorRows());

        RuntimeInventory.Item staff = inventory.items().stream()
                .filter(item -> item.definition().catalogId() == 115L)
                .findFirst().orElseThrow();
        inventory.toggleWeapon(staff.objectId());
        assertEquals(15, source.withRentalEquipment(
                inventory.equippedRentalBonuses()).totalMpRegen());
    }

    @Test void resourceIdentifierWhitelistIsFailClosed() {
        assertEquals("380_db.`armor`",
                LegacyEquipmentMagicLoader.qualified("380_db", "armor"));
        assertEquals("380_db.`道具_自訂穿脫能力`",
                LegacyEquipmentMagicLoader.qualified(
                        "380_db", "道具_自訂穿脫能力"));
        assertEquals(true, LegacyEquipmentMagicLoader.requiredResourceTables()
                .contains("etcitem"));
        assertEquals(false, LegacyEquipmentMagicLoader.requiredResourceTables()
                .contains("道具_自訂穿脫能力"));
        assertThrows(IllegalArgumentException.class,
                () -> LegacyEquipmentMagicLoader.qualified("380_db", "characters"));
        assertThrows(IllegalArgumentException.class,
                () -> LegacyEquipmentMagicLoader.qualified("380_db;DROP", "armor"));
    }
}
