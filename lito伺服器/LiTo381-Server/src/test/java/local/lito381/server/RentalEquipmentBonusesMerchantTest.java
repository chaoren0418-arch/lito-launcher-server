package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Issue #371: the 78 retained rental-merchant rows must contribute their
 * TSV attributes once equipped, without changing the 57 selected-3.80
 * templates and without counting unequipped or source-bound objects.
 *
 * <p>The expected values are parsed from the TSV by this test, independently
 * of {@link NativeLitoRentalMerchantCatalog}'s own column mapping.</p>
 */
final class RentalEquipmentBonusesMerchantTest {
    private static final String MERCHANT_RESOURCE =
            "/rental/retained-lito-rental-merchant-2010.tsv";

    private record Row(String section, Map<String, String> fields) {
        int integer(String name) {
            String value = fields.get(name);
            assertNotNull(value, "missing TSV column " + name);
            return Integer.parseInt(value);
        }
    }

    @Test void merchantAndSelectedCatalogsDoNotShareItemIds() {
        assertEquals(78, NativeLitoRentalMerchantCatalog.all().size());
        for (NativeLitoRentalMerchantCatalog.Item item
                : NativeLitoRentalMerchantCatalog.all()) {
            assertNull(RentalItemDefinitionCatalog.source(item.sourceItemId()),
                    "merchant id is also a selected-3.80 template: "
                            + item.sourceItemId());
        }
    }

    @Test void everyEquippedMerchantItemContributesExactlyItsTsvRow()
            throws Exception {
        List<Row> rows = merchantRows();
        assertEquals(78, rows.size());
        int weapons = 0;
        int armors = 0;
        for (Row row : rows) {
            int itemId = row.integer("source_item_id");
            NativeLitoRentalMerchantCatalog.Item merchant =
                    NativeLitoRentalMerchantCatalog.findBySourceItemId(itemId);
            assertNotNull(merchant, "catalog row " + itemId);
            RentalEquipmentBonuses expected;
            if (row.section().equals("weapon")) {
                weapons++;
                // Weapon rows carry only native damage/hit columns, which the
                // equipped-weapon evaluator owns.
                expected = bonuses(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            } else {
                armors++;
                expected = bonuses(1, 0, 1,
                        row.integer("strength"), row.integer("constitution"),
                        row.integer("dexterity"), row.integer("intelligence"),
                        row.integer("wisdom"), row.integer("charisma"),
                        row.integer("hit_points"), row.integer("mana_points"),
                        row.integer("spell_power"),
                        row.integer("magic_resistance"),
                        row.integer("haste") == 1,
                        row.integer("water"), row.integer("wind"),
                        row.integer("fire"), row.integer("earth"),
                        row.integer("stun"), row.integer("stone"),
                        row.integer("sleep"), row.integer("freeze"),
                        row.integer("sustain"), row.integer("blind"));
            }
            assertEquals(expected, RentalEquipmentBonuses.from(List.of(
                            item(merchant.definition(), true, false))),
                    "equipped merchant item " + itemId);
            assertEquals(RentalEquipmentBonuses.none(),
                    RentalEquipmentBonuses.from(List.of(
                            item(merchant.definition(), false, false))),
                    "unequipped merchant item " + itemId);
            assertEquals(RentalEquipmentBonuses.none(),
                    RentalEquipmentBonuses.from(List.of(
                            item(merchant.definition(), true, true))),
                    "source-bound merchant item " + itemId);
        }
        assertEquals(25, weapons);
        assertEquals(53, armors);
    }

    @Test void fireSalamanderRingAddsTenFireResistanceOnlyWhileEquipped() {
        NativeLitoRentalMerchantCatalog.Item ring =
                NativeLitoRentalMerchantCatalog.findBySourceItemId(29047);
        assertNotNull(ring);
        RuntimeInventory inventory = new RuntimeInventory();
        long objectId = inventory.addDistinctState(ring.definition(),
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL));
        assertTrue(objectId != 0);
        assertEquals(0, inventory.equippedRentalBonuses().fireResistance());
        assertEquals(1, inventory.toggleNativeArmor(objectId).size());
        RentalEquipmentBonuses equipped = inventory.equippedRentalBonuses();
        assertEquals(10, equipped.fireResistance());
        assertEquals(1, equipped.matchedItems());
        assertEquals(1, equipped.matchedArmors());
        assertEquals(1, inventory.toggleNativeArmor(objectId).size());
        assertEquals(RentalEquipmentBonuses.none(),
                inventory.equippedRentalBonuses());
    }

    @Test void merchantAndSelectedItemsAggregateTogether() {
        // 29031 (+1 to all six abilities), 29032 (SP 10), 29047 (fire 10)
        // and the selected-3.80 stone glove 42092 (STR 1, reduction 2).
        List<RuntimeInventory.Item> items = new ArrayList<>();
        for (int itemId : new int[] {29031, 29032, 29047}) {
            items.add(item(NativeLitoRentalMerchantCatalog
                    .findBySourceItemId(itemId).definition(), true, false));
        }
        items.add(item(RentalItemDefinitionCatalog.find(42092), true, false));
        RentalEquipmentBonuses bonuses = RentalEquipmentBonuses.from(items);
        assertEquals(4, bonuses.matchedItems());
        assertEquals(0, bonuses.matchedWeapons());
        assertEquals(4, bonuses.matchedArmors());
        assertEquals(2, bonuses.strength());
        assertEquals(1, bonuses.constitution());
        assertEquals(1, bonuses.dexterity());
        assertEquals(1, bonuses.intelligence());
        assertEquals(1, bonuses.wisdom());
        assertEquals(1, bonuses.charisma());
        assertEquals(10, bonuses.spellPower());
        assertEquals(0, bonuses.magicResistance());
        assertEquals(10, bonuses.fireResistance());
        assertEquals(2, bonuses.damageReduction());
    }

    /** Locks the pre-#371 selected-template aggregation for all 57 rows. */
    @Test void selectedTemplatesKeepTheirOriginalContribution() {
        List<RentalItemDefinitionCatalog.SourceTemplate> templates =
                RentalItemDefinitionCatalog.all();
        assertEquals(57, templates.size());
        for (RentalItemDefinitionCatalog.SourceTemplate source : templates) {
            boolean armor = source.kind().equals("armor");
            RentalEquipmentBonuses expected = new RentalEquipmentBonuses(
                    1, armor ? 0 : 1, armor ? 1 : 0,
                    source.integer("add_str"), source.integer("add_con"),
                    source.integer("add_dex"), source.integer("add_int"),
                    source.integer("add_wis"), source.integer("add_cha"),
                    source.integer("add_hp"), source.integer("add_mp"),
                    source.integer("add_hpr"), source.integer("add_mpr"),
                    source.integer("add_sp"), source.integer("m_def"),
                    source.integer("haste_item") == 1,
                    source.integer("magicdmgmodifier"),
                    armor ? source.integer("damage_reduction") : 0,
                    armor ? source.integer("add_melee_redux") : 0,
                    armor ? source.integer("add_bow_redux") : 0,
                    armor ? source.integer("weight_reduction") : 0,
                    armor ? source.integer("hit_modifier") : 0,
                    armor ? source.integer("dmg_modifier") : 0,
                    armor ? source.integer("bow_hit_modifier") : 0,
                    armor ? source.integer("bow_dmg_modifier") : 0,
                    armor ? source.integer("defense_water") : 0,
                    armor ? source.integer("defense_wind") : 0,
                    armor ? source.integer("defense_fire") : 0,
                    armor ? source.integer("defense_earth") : 0,
                    armor ? source.integer("regist_stun") : 0,
                    armor ? source.integer("regist_stone") : 0,
                    armor ? source.integer("regist_sleep") : 0,
                    armor ? source.integer("regist_freeze") : 0,
                    armor ? source.integer("regist_sustain") : 0,
                    armor ? source.integer("regist_blind") : 0);
            long itemId = source.definition().catalogId();
            assertEquals(expected, RentalEquipmentBonuses.from(List.of(
                            item(source.definition(), true, false))),
                    "equipped selected template " + itemId);
            assertEquals(RentalEquipmentBonuses.none(),
                    RentalEquipmentBonuses.from(List.of(
                            item(source.definition(), false, false))),
                    "unequipped selected template " + itemId);
            assertEquals(RentalEquipmentBonuses.none(),
                    RentalEquipmentBonuses.from(List.of(
                            item(source.definition(), true, true))),
                    "source-bound selected template " + itemId);
        }
    }

    private static RuntimeInventory.Item item(
            InventoryCatalog.Definition definition,
            boolean equipped, boolean sourceBound) {
        assertNotNull(definition);
        return new RuntimeInventory.Item(
                0x7371_0001L, definition, 1, equipped, 0, 0,
                true, RuntimeInventory.BLESSING_NORMAL, sourceBound);
    }

    private static RentalEquipmentBonuses bonuses(
            int items, int weapons, int armors,
            int strength, int constitution, int dexterity,
            int intelligence, int wisdom, int charisma,
            int hitPoints, int manaPoints, int spellPower,
            int magicResistance, boolean haste,
            int water, int wind, int fire, int earth,
            int stun, int stone, int sleep, int freeze,
            int sustain, int blind) {
        // Regeneration, magic damage, reductions, weight and armor hit/damage
        // have no column in the retained merchant schema and stay zero.
        return new RentalEquipmentBonuses(
                items, weapons, armors,
                strength, constitution, dexterity, intelligence, wisdom, charisma,
                hitPoints, manaPoints, 0, 0, spellPower, magicResistance, haste,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                water, wind, fire, earth,
                stun, stone, sleep, freeze, sustain, blind);
    }

    private static List<Row> merchantRows() throws Exception {
        List<Row> rows = new ArrayList<>();
        try (InputStream input = RentalEquipmentBonusesMerchantTest.class
                .getResourceAsStream(MERCHANT_RESOURCE)) {
            assertNotNull(input, MERCHANT_RESOURCE);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            String section = null;
            String[] header = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1);
                    header = null;
                    continue;
                }
                String[] values = line.split("\\t", -1);
                if (header == null) {
                    header = values;
                    continue;
                }
                assertEquals(header.length, values.length);
                Map<String, String> fields = new LinkedHashMap<>();
                for (int index = 0; index < header.length; index++) {
                    fields.put(header[index], values[index]);
                }
                rows.add(new Row(section, fields));
            }
        }
        return rows;
    }
}
