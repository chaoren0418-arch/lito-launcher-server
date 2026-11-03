package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateBoardTest {
    @TempDir Path temporary;

    @Test
    void persistsTopicsAndPagesNewestFirstWithCursor() throws Exception {
        Path file = temporary.resolve("board-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> inventory = new LinkedHashMap<>(state.economy("TestKnight").inventory());
        inventory.computeIfPresent(InventoryCatalog.ADENA, (ignored, count) -> count - 300);

        for (int index = 1; index <= 10; index++) {
            state.postBoardTopic("TestKnight", 5_000, inventory,
                    "26/08/09", "Topic " + index, "Body " + index);
        }
        List<GameStateStore.BoardTopic> first = state.boardTopicsBefore(
                Protocol.BOARD_INITIAL_CURSOR, Protocol.BOARD_PAGE_SIZE);
        assertEquals(List.of(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L),
                first.stream().map(GameStateStore.BoardTopic::id).toList());
        assertEquals(List.of(2L, 1L), state.boardTopicsBefore(3, 8).stream()
                .map(GameStateStore.BoardTopic::id).toList());
        assertEquals(10, state.boardTopic(10).id());
        assertNull(state.boardTopic(11));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.BoardTopic topic = reloaded.boardTopic(10);
        assertNotNull(topic);
        assertEquals("TestKnight", topic.author());
        assertEquals("Topic 10", topic.title());
        assertEquals("Body 10", topic.content());
        assertEquals(99_700, reloaded.economy("TestKnight").inventory()
                .get(InventoryCatalog.ADENA));
    }

    @Test
    void keepsNonPersistentBoardStateInMemoryOnly() throws Exception {
        Path file = temporary.resolve("unused.properties");
        GameStateStore state = new GameStateStore(file, false);
        state.postBoardTopic("TestKnight", 5_000, Map.of(InventoryCatalog.ADENA, 99_700),
                "26/08/09", "Memory topic", "Body");
        assertEquals("Memory topic", state.boardTopic(1).title());
        assertFalse(file.toFile().exists());
    }

    @Test
    void boardAuthorIdentityIsSourceQualifiedWhileWireAuthorStaysUnqualified()
            throws Exception {
        Path file = temporary.resolve("source-board-state.properties");
        String sun = GameStateStore.sourceCharacter("Sun", "SameHero");
        String moon = GameStateStore.sourceCharacter("Moon", "SameHero");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.BoardTopic topic = state.postBoardTopic(
                sun, 5_000, state.economy(sun).inventory(),
                "26/08/13", "Source topic", "Body");
        assertEquals("SameHero", topic.author());
        assertEquals(sun, topic.authorIdentity());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(sun, reloaded.boardTopic("Sun", topic.id()).authorIdentity());
        assertNull(reloaded.boardTopic("Moon", topic.id()));
        assertFalse(reloaded.deleteBoardTopic(moon, topic.id()));
        GameStateStore.BoardTopic moonTopic = reloaded.postBoardTopic(
                moon, 5_000, reloaded.economy(moon).inventory(),
                "26/08/13", "Moon topic", "Moon body");
        assertEquals(topic.id(), moonTopic.id());
        assertEquals(moon, reloaded.boardTopic("Moon", moonTopic.id()).authorIdentity());
        assertEquals("Source topic", reloaded.boardTopic("Sun", topic.id()).title());
        assertNotNull(reloaded.boardTopic("Sun", topic.id()));
        org.junit.jupiter.api.Assertions.assertTrue(
                reloaded.deleteBoardTopic(sun, topic.id()));
        assertNull(reloaded.boardTopic("Sun", topic.id()));
        assertNotNull(reloaded.boardTopic("Moon", moonTopic.id()));
    }
}
