package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class BattleDragonEyeCatalogTest {
    @Test void pinsAllSevenExactItemsBuffsAbilitiesAndInventoryDefinitions() {
        List<Expected> expected = List.of(
                expected(42_518L, "$7885", 3_642, 6_684, 7_671, 46,
                        ability(0, 0, 0, 10, 3, 0, 0, 0), "地龍之魔眼效果消失了。"),
                expected(42_519L, "$7886", 3_641, 6_685, 7_671, 47,
                        ability(0, 0, 3, 0, 0, 0, 3, 0), "水龍之魔眼效果消失了。"),
                expected(42_520L, "$7887", 3_643, 6_686, 7_673, 48,
                        ability(0, 3, 0, 0, 0, 3, 0, 0), "風龍之魔眼效果消失了。"),
                expected(42_521L, "$7888", 3_644, 6_683, 7_674, 49,
                        ability(2, 0, 0, 0, 0, 0, 0, 0), "火龍之魔眼效果消失了。"),
                expected(42_522L, "$7889", 3_663, 6_688, 7_675, 50,
                        ability(0, 0, 50, 10, 0, 0, 0, 0), "誕生之魔眼效果消失了。"),
                expected(42_523L, "$7890", 3_666, 6_689, 7_676, 51,
                        ability(0, 1, 50, 10, 0, 0, 0, 0), "形象之魔眼效果消失了。"),
                expected(42_524L, "$7891", 3_645, 6_687, 7_678, 52,
                        ability(2, 1, 50, 10, 0, 0, 0, 1), "生命之魔眼效果消失了。"));

        assertEquals(7, BattleDragonEyeCatalog.all().size());
        for (Expected source : expected) {
            BattleDragonEyeCatalog.Eye eye = BattleDragonEyeCatalog.require(source.itemId());
            assertEquals(source.nameId(), eye.nameId());
            assertEquals(0, eye.useType());
            assertEquals(source.inventoryGfx(), eye.inventoryGfx());
            assertEquals(3_963, eye.groundGfx());
            assertEquals(source.buffId(), eye.buffId());
            assertEquals(600, eye.durationSeconds());
            assertEquals(source.castGfx(), eye.castGfx());
            assertEquals(source.statusIcon(), eye.statusIcon());
            assertEquals(8, eye.sharedDelayId());
            assertEquals(600, eye.sharedDelayMillis());
            assertEquals(60_000, eye.itemDelayMillis());
            assertEquals("DRAGON_EYE", eye.group());
            assertFalse(eye.canOverwrite());
            assertEquals(source.ability(), eye.ability());
            assertEquals(source.endMessage(), eye.endMessage());

            InventoryCatalog.Definition definition = InventoryCatalog.require(source.itemId());
            assertEquals(InventoryCatalog.Effect.DRAGON_EYE, definition.effect());
            assertEquals(0, definition.useType());
            assertEquals(source.inventoryGfx(), definition.icon());
            assertEquals(3_963, definition.groundGfx());
            assertEquals(0, definition.unitWeight());
            assertTrue(definition.stackable());
            assertFalse(definition.tradable());
        }
    }

    @Test void preservesTheExactNativeNormalItemRequestAndUseType() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.WIND_DRAGON_EYE, 1), () -> 0x7000_0686L);
        RuntimeInventory.Item item = inventory.items().get(0);
        assertArrayEquals(hex("1C 86 06 00 70 00"),
                new Protocol.Writer().c(Protocol.C_USE_ITEM)
                        .d(item.objectId()).s("").bytes());
        assertEquals(new Protocol.InventoryUse(0x7000_0686L, ""),
                Protocol.defaultInventoryUse(new Protocol.Writer()
                        .c(Protocol.C_USE_ITEM).d(item.objectId()).s("").bytes()));
        byte[] batch = Protocol.inventoryBatch(List.of(item.wireItem()));
        assertEquals(0, Byte.toUnsignedInt(batch[6]));
    }

    private static BattleDragonEyeCatalog.Ability ability(
            int meleeDamage, int magicHit, int damageReduction, int evasionRate,
            int stoneResistance, int sleepResistance, int freezeResistance,
            int poisonResistance) {
        return new BattleDragonEyeCatalog.Ability(
                meleeDamage, magicHit, damageReduction, evasionRate,
                stoneResistance, sleepResistance, freezeResistance, poisonResistance);
    }

    private static Expected expected(
            long itemId, String nameId, int inventoryGfx, int buffId,
            int castGfx, int statusIcon, BattleDragonEyeCatalog.Ability ability,
            String endMessage) {
        return new Expected(itemId, nameId, inventoryGfx, buffId,
                castGfx, statusIcon, ability, endMessage);
    }

    private record Expected(
            long itemId, String nameId, int inventoryGfx, int buffId,
            int castGfx, int statusIcon, BattleDragonEyeCatalog.Ability ability,
            String endMessage) {}

    private static byte[] hex(String value) {
        String compact = value.replace(" ", "");
        byte[] bytes = new byte[compact.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(compact.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
