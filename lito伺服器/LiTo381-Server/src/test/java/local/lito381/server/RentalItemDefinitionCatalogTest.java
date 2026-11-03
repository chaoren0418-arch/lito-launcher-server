package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class RentalItemDefinitionCatalogTest {
    @Test void loadsAllExactThirteenWeaponAndFortyFourArmorRows() {
        assertEquals(57, RentalItemDefinitionCatalog.all().size());
        Set<Integer> ids = RentalItemDefinitionCatalog.all().stream()
                .map(source -> Math.toIntExact(source.definition().catalogId()))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                2, 50, 51, 57, 66, 84, 104, 106, 115, 131, 164, 193, 219,
                20006, 20011, 20019, 20023, 20027, 20032, 20033, 20051,
                20055, 20056, 20063, 20070, 20084, 20085, 20092, 20093,
                20094, 20095, 20107, 20132, 20175, 20180, 20187, 20191,
                20194, 20195, 20225, 20229, 20230, 20236, 20256, 20263,
                20264, 20266, 20284, 20287, 20303, 20312, 20314, 20316,
                20318, 20319, 20320, 42092), ids);
        for (RentalItemDefinitionCatalog.SourceTemplate source
                : RentalItemDefinitionCatalog.all()) {
            InventoryCatalog.Definition definition = source.definition();
            assertNotNull(InventoryCatalog.find(definition.catalogId()));
            assertFalse(definition.identified());
            assertFalse(definition.stackable());
            assertTrue(definition.tradable());
            assertNotNull(definition.detail());
            assertEquals(source.integer("weight"), definition.unitWeight());
            assertEquals(source.kind().equals("weapon") ? 38 : 46,
                    source.fields().size());
        }
    }

    @Test void preservesSelectedWeaponWireStatusAndSpecialCombatFields() {
        RentalItemDefinitionCatalog.SourceTemplate dragonSlayer =
                RentalItemDefinitionCatalog.source(66);
        InventoryCatalog.Definition definition = dragonSlayer.definition();
        assertEquals("屠龍劍", dragonSlayer.selectedName());
        assertEquals("$1020", definition.name());
        assertEquals(438, definition.icon());
        assertEquals(3, definition.groundGfx());
        assertEquals(BattleActionCodes.TWO_HAND_SWORD_WALK,
                definition.weaponWalkAction());
        InventoryCatalog.NativeWeaponStatus status = definition.sourceWeaponStatus();
        assertNotNull(status);
        assertEquals(24, status.damageSmall());
        assertEquals(33, status.damageLarge());
        assertEquals(12, status.material());
        assertEquals(180_000, status.unitWeight());
        assertEquals(34, status.classMask());
        assertEquals(6, status.safeEnchant());

        RentalItemDefinitionCatalog.SourceTemplate edoryu =
                RentalItemDefinitionCatalog.source(84);
        assertEquals(4, edoryu.definition().sourceWeaponStatus().hitModifier());
        assertEquals(3, edoryu.definition().sourceWeaponStatus().damageModifier());
        assertEquals(10, edoryu.integer("dmg_cri_chance"));
        assertEquals(1.2D, edoryu.decimal("dmg_cri"));
        assertEquals(10, RentalItemDefinitionCatalog.source(115).integer("add_mpr"));
        assertEquals(33, RentalItemDefinitionCatalog.source(164)
                .integer("dmg_max_chance"));
    }

    @Test void preservesSelectedArmorWireStatusAndNonPaneEffects() {
        RentalItemDefinitionCatalog.SourceTemplate lich =
                RentalItemDefinitionCatalog.source(20107);
        InventoryCatalog.Definition definition = lich.definition();
        assertEquals("巫妖斗篷", lich.selectedName());
        assertEquals(2, definition.useType());
        assertEquals(978, definition.icon());
        assertEquals(2939, definition.groundGfx());
        InventoryCatalog.NativeArmorStatus status = definition.sourceArmorStatus();
        assertNotNull(status);
        assertEquals(3, status.displayArmorClass());
        assertEquals(6, status.material());
        assertEquals(72, status.classMask());
        assertEquals(4, status.safeEnchant());
        assertEquals(50, status.manaPoints());
        assertEquals(2, status.armorType());
        assertEquals(15, lich.integer("add_mpr"));

        RentalItemDefinitionCatalog.SourceTemplate manaCloak =
                RentalItemDefinitionCatalog.source(20055);
        assertEquals(19, manaCloak.definition().useType());
        assertEquals(4, manaCloak.definition().sourceArmorStatus().armorType());
        assertEquals(5, manaCloak.integer("add_mpr"));

        RentalItemDefinitionCatalog.SourceTemplate stoneGlove =
                RentalItemDefinitionCatalog.source(42092);
        assertEquals(20, stoneGlove.definition().useType());
        assertEquals(1, stoneGlove.definition().sourceArmorStatus().strength());
        assertEquals(5, stoneGlove.definition().sourceArmorStatus().armorType());
        assertEquals(2, stoneGlove.integer("damage_reduction"));

        InventoryCatalog.NativeArmorStatus ancientLeather =
                RentalItemDefinitionCatalog.source(20092)
                        .definition().sourceArmorStatus();
        assertEquals(-1, ancientLeather.safeEnchant());
        assertEquals(4, RentalItemDefinitionCatalog.source(20092).integer("add_hpr"));
        assertEquals(8, RentalItemDefinitionCatalog.source(20092).integer("add_mpr"));
    }

    @Test void runtimeWeaponsRetainSelectedRangeMaterialAndCriticalRules() {
        RuntimeInventory edoryuInventory = RuntimeInventory.fromCatalogCounts(Map.of(84L, 1));
        RuntimeInventory.Item edoryuItem = edoryuInventory.items().get(0);
        assertEquals(1, edoryuInventory.toggleWeapon(edoryuItem.objectId()).size());
        SourceWeaponPhysicalStats edoryu = edoryuInventory.equippedPhysicalWeapon();
        assertEquals(1, edoryu.range());
        assertEquals(16, edoryu.combatTraits().material());
        assertEquals(10, edoryu.combatTraits().doubleDamageChance());
        assertEquals(1.2D, edoryu.combatTraits().doubleDamageMultiplier());
        assertEquals(1078, edoryu.combatTraits().inventoryGfx());

        RuntimeInventory clawInventory = RuntimeInventory.fromCatalogCounts(Map.of(164L, 1));
        RuntimeInventory.Item clawItem = clawInventory.items().get(0);
        clawInventory.toggleWeapon(clawItem.objectId());
        SourceWeaponPhysicalStats claw = clawInventory.equippedPhysicalWeapon();
        assertEquals(11, claw.weaponType());
        assertEquals(33, claw.combatTraits().maximumDamageChance());

        RuntimeInventory gauntletInventory = RuntimeInventory.fromCatalogCounts(
                Map.of(193L, 1));
        RuntimeInventory.Item gauntletItem = gauntletInventory.items().get(0);
        gauntletInventory.toggleWeapon(gauntletItem.objectId());
        SourceWeaponPhysicalStats gauntlet = gauntletInventory.equippedPhysicalWeapon();
        assertEquals(10, gauntlet.weaponType());
        assertEquals(10, gauntlet.range());
    }

    @Test void aggregatesOnlyEquippedSessionOwnedSelectedTemplateEffects() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(
                115L, 1, 20019L, 1, 20107L, 1, 42092L, 1));
        for (RuntimeInventory.Item item : inventory.items()) {
            if (item.definition().weapon()) inventory.toggleWeapon(item.objectId());
            else inventory.toggleNativeArmor(item.objectId());
        }
        RentalEquipmentBonuses bonuses = inventory.equippedRentalBonuses();
        assertEquals(4, bonuses.matchedItems());
        assertEquals(1, bonuses.matchedWeapons());
        assertEquals(3, bonuses.matchedArmors());
        assertEquals(1, bonuses.strength());
        assertEquals(0, bonuses.constitution());
        assertEquals(0, bonuses.dexterity());
        assertEquals(0, bonuses.intelligence());
        assertEquals(1, bonuses.wisdom());
        assertEquals(2, bonuses.charisma());
        assertEquals(50, bonuses.maximumMp());
        assertEquals(25, bonuses.mpRegen());
        assertEquals(2, bonuses.damageReduction());

        RuntimeInventory.Item manaStaff = inventory.items().stream()
                .filter(item -> item.definition().catalogId() == 115L)
                .findFirst().orElseThrow();
        inventory.toggleWeapon(manaStaff.objectId());
        assertEquals(15, inventory.equippedRentalBonuses().mpRegen());
    }
}
