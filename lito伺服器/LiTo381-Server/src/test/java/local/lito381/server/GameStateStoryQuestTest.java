package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateStoryQuestTest {
    @TempDir Path temporary;

    @Test void unchangedReadOnlySourceKeepsOverlayAndChangedSourceRebasesIt()
            throws Exception {
        Path file = temporary.resolve("story-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        assertEquals(Map.of(10701, 1), state.reconcileStoryQuests(
                "ServerA::Knight", Map.of(10701, 1)).effective());
        state.saveStoryQuestSteps("ServerA::Knight",
                Map.of(10701, 1, 10702, 1, 10703, 0));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(Map.of(10701, 1, 10702, 1, 10703, 0),
                reloaded.reconcileStoryQuests(
                        "ServerA::Knight", Map.of(10701, 1)).effective());
        assertEquals(Map.of(10701, 2), reloaded.reconcileStoryQuests(
                "ServerA::Knight", Map.of(10701, 2)).effective());
        assertTrue(Files.readString(file).contains("format.version=43"));
    }

    @Test void rewardInventoryAndQuestCompletionCommitAtomically() throws Exception {
        Path file = temporary.resolve("story-atomic.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.reconcileStoryQuests("ServerA::Knight", Map.of(10701, 1, 10702, 0));
        GameStateStore.Economy before = state.economy("ServerA::Knight");
        RuntimeInventory inventory = RuntimeInventory.fromDurableState(
                before.inventory(), before.itemStates());
        assertTrue(inventory.add(InventoryCatalog.require(
                InventoryCatalog.RED_KNIGHT_SWORD), 1));
        state.saveEconomyStateAndStoryQuests(
                "ServerA::Knight", before.points(), inventory.catalogCounts(),
                inventory.catalogItemStates(),
                Map.of(10701, 1, 10702, 1, 10703, 0));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(1, reloaded.economy("ServerA::Knight").inventory()
                .get(InventoryCatalog.RED_KNIGHT_SWORD));
        assertEquals(1, reloaded.storyQuests("ServerA::Knight").step(10702));
        assertTrue(reloaded.storyQuests("ServerA::Knight").contains(10703));
    }

    @Test void zeroExchangeAndChainedCrownStageSurviveRestartTogether()
            throws Exception {
        Path file = temporary.resolve("zero-chain-atomic.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.reconcileStoryQuests("ServerA::Crown", Map.of());
        RuntimeInventory before = new RuntimeInventory();
        assertTrue(before.add(InventoryCatalog.require(
                InventoryCatalog.SEARCH_DOCUMENT), 1));
        state.saveEconomyStateAndStoryQuests(
                "ServerA::Crown", 0, before.catalogCounts(),
                before.catalogItemStates(), Map.of(10101, 0));

        RuntimeInventory after = before.copy();
        assertTrue(after.removeCatalog(InventoryCatalog.SEARCH_DOCUMENT, 1));
        assertTrue(after.add(InventoryCatalog.require(
                InventoryCatalog.RED_CLOAK), 1));
        state.saveEconomyStateAndStoryQuests(
                "ServerA::Crown", 0, after.catalogCounts(),
                after.catalogItemStates(), Map.of(10101, 1, 10102, 0));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.Economy economy = reloaded.economy("ServerA::Crown");
        assertEquals(0, economy.inventory().getOrDefault(
                InventoryCatalog.SEARCH_DOCUMENT, 0));
        assertEquals(1, economy.inventory().get(InventoryCatalog.RED_CLOAK));
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates());
        assertTrue(restored.items().stream().anyMatch(item ->
                item.definition().catalogId() == InventoryCatalog.RED_CLOAK
                        && item.definition().sourceArmor()
                        && item.definition().sourceArmorStatus().charisma() == 1));
        assertEquals(1, reloaded.storyQuests("ServerA::Crown").step(10101));
        assertTrue(reloaded.storyQuests("ServerA::Crown").contains(10102));
    }

    @Test void desireInventoryQuestAndKarmaCommitAtomically() throws Exception {
        Path file = temporary.resolve("desire-atomic.properties");
        String character = "ServerA::DesireKnight";
        GameStateStore state = new GameStateStore(file, true);
        state.reconcileStoryQuests(character, Map.of(36, 0));
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(InventoryCatalog.require(41130), 1));
        state.saveDesireCaveState(character, 77, inventory.catalogCounts(),
                inventory.catalogItemStates(), Map.of(36, 255), 10_000_000);

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(77, reloaded.economy(character).points());
        assertEquals(1, reloaded.economy(character).inventory().get(41130L));
        assertEquals(255, reloaded.storyQuests(character).step(36));
        assertEquals(10_000_000, reloaded.factionKarma(character).value());
        assertEquals(8, reloaded.factionKarma(character).level());
    }

    @Test void failedElfReportCannotConsume49163WithoutBothCkewQuestViews()
            throws Exception {
        Path file = temporary.resolve("elf-ckew-rollback.properties");
        String character = "ServerA::Elf";
        GameStateStore state = new GameStateStore(file, true);
        RuntimeInventory before = new RuntimeInventory();
        assertTrue(before.add(InventoryCatalog.require(49163), 1));
        Map<Integer, Integer> beforeQuests = Map.of(
                11601, 1, 11603, 1, 11604, 0);
        state.saveEconomyStateAndStoryQuests(
                character, 0, before.catalogCounts(),
                before.catalogItemStates(), beforeQuests);

        ClassicWorldStoryQuestCatalog.Event report =
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(event -> event.id() == 40000262)
                        .findFirst().orElseThrow();
        Map<Integer, Integer> nextQuests =
                ClassicWorldCkewCatalog.bridgePredecessorCompletion(
                        report, 2,
                        ClassicWorldStoryQuestCatalog.applyQuestMutation(
                                report, beforeQuests));
        RuntimeInventory after = before.copy();
        assertTrue(after.removeCatalog(49163, 1));

        // GameStateStore writes this sibling before the atomic replace.  A
        // directory at that exact temporary path deterministically forces the
        // persistence boundary to fail without touching the committed file.
        Files.createDirectory(file.resolveSibling(file.getFileName() + ".tmp"));
        assertThrows(IOException.class, () ->
                state.saveEconomyStateAndStoryQuests(
                        character, 0, after.catalogCounts(),
                        after.catalogItemStates(), nextQuests));

        assertEquals(1, state.economy(character).inventory().get(49163L));
        assertEquals(beforeQuests, state.storyQuests(character).effective());
        // Read the committed file through a clean persistent instance after
        // moving the failure sentinel out of the writer path.
        Files.move(file.resolveSibling(file.getFileName() + ".tmp"),
                temporary.resolve("elf-ckew-write-blocker"));
        GameStateStore committed = new GameStateStore(file, true);
        assertEquals(1, committed.economy(character).inventory().get(49163L));
        assertEquals(beforeQuests,
                committed.storyQuests(character).effective());
    }
}
