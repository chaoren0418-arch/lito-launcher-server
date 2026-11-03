package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleExperienceGemTest {
    @TempDir Path temporary;

    @Test void pinsRetainedRowsAndExactNativeDefaultItemRequest() {
        List<Expected> expected = List.of(
                new Expected(49_549L, "$7969", 3_721, 3_000),
                new Expected(49_550L, "$7970", 3_727, 2_000),
                new Expected(49_551L, "$7971", 3_725, 1_000));
        assertEquals(3, BattleExperienceGemCatalog.all().size());
        for (Expected row : expected) {
            BattleExperienceGemCatalog.Gem gem =
                    BattleExperienceGemCatalog.require(row.itemId());
            assertEquals(row.nameId(), gem.nameId());
            assertEquals(row.inventoryGfx(), gem.inventoryGfx());
            assertEquals(3_963, gem.groundGfx());
            assertEquals(row.bonusExperience(), gem.bonusExperience());
            assertEquals(600, gem.durationSeconds());

            InventoryCatalog.Definition definition = InventoryCatalog.require(row.itemId());
            assertEquals(0, definition.useType());
            assertEquals(InventoryCatalog.Effect.EXPERIENCE_GEM, definition.effect());
            assertTrue(definition.stackable());
            assertTrue(definition.tradable());
        }

        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.DRAGON_DIAMOND, 1), () -> 0x7000_0796L);
        RuntimeInventory.Item item = inventory.items().get(0);
        assertArrayEquals(hex("1C 96 07 00 70 00"),
                new Protocol.Writer().c(Protocol.C_USE_ITEM)
                        .d(item.objectId()).s("").bytes());
    }

    @Test void refreshReplacesTheGemAndExpiryClearsTheFixedAward() {
        BattleExperienceGemStatus status = new BattleExperienceGemStatus();
        long now = TimeUnit.SECONDS.toNanos(10);
        BattleExperienceGemStatus.Snapshot diamond = status.activate(
                BattleExperienceGemCatalog.require(49_549L), now);
        assertEquals(3_000, diamond.bonusExperience());
        assertEquals(600, diamond.remainingSeconds());

        long refreshedAt = now + TimeUnit.SECONDS.toNanos(300);
        BattleExperienceGemStatus.Snapshot ruby = status.activate(
                BattleExperienceGemCatalog.require(49_551L), refreshedAt);
        assertEquals(1_000, ruby.bonusExperience());
        assertEquals(600, ruby.remainingSeconds());
        assertFalse(status.snapshot(refreshedAt + TimeUnit.SECONDS.toNanos(599)).remainingSeconds()
                == 0);
        assertEquals(BattleExperienceGemCatalog.DRAGON_RUBY,
                status.expire(refreshedAt + TimeUnit.SECONDS.toNanos(600)).itemId());
        assertFalse(status.snapshot(refreshedAt + TimeUnit.SECONDS.toNanos(600)).active());
    }

    @Test void durableRemainingTimeRestoresTheExactGemTier() {
        BattleExperienceGemStatus status = new BattleExperienceGemStatus();
        long now = TimeUnit.SECONDS.toNanos(50);
        BattleExperienceGemStatus.Snapshot restored = status.restore(2_000, 123, now);
        assertTrue(restored.active());
        assertEquals(BattleExperienceGemCatalog.DRAGON_SAPPHIRE,
                restored.gem().itemId());
        assertEquals(123, restored.remainingSeconds());
    }

    @Test void inventoryConsumptionAndBuffLeaseShareOneDurableSnapshot() throws Exception {
        Path file = temporary.resolve("experience-gem.properties");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.Economy economy = state.economy("GemTester");
        RuntimeInventory inventory = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates());
        RuntimeInventory.Item diamond = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.DRAGON_DIAMOND)
                .findFirst().orElseThrow();
        assertTrue(inventory.remove(diamond.objectId(), 1));
        state.saveEconomyStateActivatingExperienceBuff(
                "GemTester", economy.points(), inventory.catalogCounts(),
                inventory.catalogItemStates(),
                new GameStateStore.TimedExperienceBuff(3_000, 417));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(0, reloaded.economy("GemTester").inventory()
                .getOrDefault(InventoryCatalog.DRAGON_DIAMOND, 0));
        assertEquals(new GameStateStore.TimedExperienceBuff(3_000, 417),
                reloaded.timedExperienceBuff("GemTester"));
        reloaded.saveTimedExperienceBuff(
                "GemTester", GameStateStore.TimedExperienceBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedExperienceBuff("GemTester").active());
    }

    @Test void fixedGemAwardIsAddedAfterTheSharedRateAndPenalty() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("fixed-award.properties"), false);
        GameStateStore.ClassicExperienceMutation mutation =
                state.addClassicExperience("GemTester", 0, 52, 5, 2.0D, 3_000);
        // Retained level-52 penalty 0.5 is applied before the shared x2 rate;
        // the fixed Dragon Gem award is then added without scaling.
        assertEquals(3_005, mutation.adjustedExperience());
        assertEquals(3_005, mutation.afterExperience());
    }

    private record Expected(long itemId, String nameId, int inventoryGfx,
                            int bonusExperience) {}

    private static byte[] hex(String value) {
        String compact = value.replace(" ", "");
        byte[] bytes = new byte[compact.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(
                    compact.substring(index * 2, index * 2 + 2), 16);
        }
        return bytes;
    }
}
