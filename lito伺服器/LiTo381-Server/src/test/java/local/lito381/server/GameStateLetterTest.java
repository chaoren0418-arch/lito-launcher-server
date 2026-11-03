package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateLetterTest {
    @TempDir Path temporary;

    @Test
    void persistsDeliveryOpenReplayAndDestroyWithOneStableBodyRow() throws Exception {
        Path file = temporary.resolve("letter-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> sender = new LinkedHashMap<>(
                state.economy("TestKnight").inventory());
        sender.put(InventoryCatalog.LETTER_PAPER, 2);
        state.saveEconomy("TestKnight", 5_000, sender);

        int recipientClosedBefore = state.economy("TestMage").inventory()
                .getOrDefault(InventoryCatalog.LETTER_CLOSED, 0);
        GameStateStore.LetterDelivery delivery = state.deliverLetter(
                "TestKnight", "TestMage", 0x1234, "26/08/11", "Subject", "Body");
        assertTrue(delivery.delivered());
        assertEquals(1, delivery.senderInventory().get(InventoryCatalog.LETTER_PAPER));
        assertEquals(recipientClosedBefore + 1,
                delivery.receiverInventory().get(InventoryCatalog.LETTER_CLOSED));
        assertEquals("Subject", delivery.letter().subject());
        assertFalse(delivery.letter().opened());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(1, reloaded.letters("TestMage").size());
        GameStateStore.Letter durable = reloaded.letters("TestMage").get(0);
        assertEquals(delivery.letter().id(), durable.id());
        assertEquals("Body", durable.content());

        GameStateStore.LetterOpenMutation opened = reloaded.openLetter(
                "TestMage", durable.id());
        assertEquals(GameStateStore.LetterOpenStatus.OPENED, opened.status());
        assertTrue(opened.letter().opened());
        assertEquals(recipientClosedBefore,
                opened.inventory().getOrDefault(InventoryCatalog.LETTER_CLOSED, 0));
        assertEquals(1, opened.inventory().get(InventoryCatalog.LETTER_OPEN));

        Map<Long, Integer> afterFirstOpen = opened.inventory();
        GameStateStore.LetterOpenMutation replay = reloaded.openLetter(
                "TestMage", durable.id());
        assertEquals(GameStateStore.LetterOpenStatus.ALREADY_OPEN, replay.status());
        assertEquals(afterFirstOpen, replay.inventory());

        GameStateStore afterRestart = new GameStateStore(file, true);
        assertTrue(afterRestart.letters("TestMage").get(0).opened());
        GameStateStore.LetterDeleteMutation deleted = afterRestart.deleteLetter(
                "TestMage", durable.id());
        assertTrue(deleted.deleted());
        assertEquals(0, deleted.inventory().getOrDefault(InventoryCatalog.LETTER_OPEN, 0));
        assertTrue(afterRestart.letters("TestMage").isEmpty());
        assertTrue(Files.readString(file).contains("format.version=43"));
    }

    @Test
    void supportsSelfDeliveryAndRejectsMissingPaperOrFullRecipient() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("memory.properties"), false);
        assertEquals(GameStateStore.LetterDeliveryStatus.NO_PAPER,
                state.deliverLetter("TestKnight", "TestMage", 0,
                        "26/08/11", "", "").status());

        Map<Long, Integer> self = new LinkedHashMap<>(
                state.economy("TestKnight").inventory());
        self.put(InventoryCatalog.LETTER_PAPER, 1);
        state.saveEconomy("TestKnight", 5_000, self);
        GameStateStore.LetterDelivery delivered = state.deliverLetter(
                "TestKnight", "TestKnight", 1, "26/08/11", "", "");
        assertTrue(delivered.delivered());
        assertEquals(0, delivered.senderInventory()
                .getOrDefault(InventoryCatalog.LETTER_PAPER, 0));
        assertEquals(1, delivered.senderInventory().get(InventoryCatalog.LETTER_CLOSED));

        Map<Long, Integer> sender = new LinkedHashMap<>(
                state.economy("TestMage").inventory());
        sender.put(InventoryCatalog.LETTER_PAPER, 1);
        state.saveEconomy("TestMage", 5_000, sender);
        Map<Long, Integer> full = new LinkedHashMap<>();
        for (long id = 60_000; id < 60_000 + RuntimeInventory.MAX_SLOTS; id++) {
            full.put(id, 1);
        }
        state.saveEconomy("FullTarget", 5_000, full);
        assertEquals(GameStateStore.LetterDeliveryStatus.RECIPIENT_FULL,
                state.deliverLetter("TestMage", "FullTarget", 0,
                        "26/08/11", "", "").status());
        assertEquals(1, state.economy("TestMage").inventory()
                .get(InventoryCatalog.LETTER_PAPER));
        assertTrue(state.letters("FullTarget").isEmpty());
    }
}
