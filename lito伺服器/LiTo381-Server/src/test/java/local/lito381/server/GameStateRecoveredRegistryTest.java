package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateRecoveredRegistryTest {
    @TempDir Path temporary;

    @Test
    void deletesOnlyTheAuthorsBoardTopicAndPersistsTheRemoval() throws Exception {
        Path file = temporary.resolve("recovered-registry-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.BoardTopic topic = state.postBoardTopic(
                "NativeHero", 5_000, Map.of(InventoryCatalog.ADENA, 99_700),
                "26/08/11", "Delete me", "Native C57 body");

        assertFalse(state.deleteBoardTopic("OtherHero", topic.id()));
        assertNotNull(state.boardTopic(topic.id()));
        assertTrue(state.deleteBoardTopic("nativehero", topic.id()));
        assertNull(state.boardTopic(topic.id()));
        assertFalse(state.deleteBoardTopic("NativeHero", topic.id()));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertNull(reloaded.boardTopic(topic.id()));
    }
}
