package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NumericBreakthroughE2eFixtureTest {
    @Test
    void isolatedReadOnlyFixturePreservesAllValuesBeyondNativeWidths() throws Exception {
        assertFixture(Path.of("src/test/resources/server-config/config/server.numeric-schema2-e2e.properties"));
        assertFixture(Path.of("src/test/resources/server-config/config/server.numeric-schema2-source-e2e.properties"));
    }

    private static void assertFixture(Path path) throws Exception {
        ServerConfig config = ServerConfig.load(path);

        assertEquals("memory", config.databaseMode());
        assertTrue(config.databaseReadOnly());
        assertTrue(!config.statePersistenceEnabled());

        try (AccountRepository repository = AccountRepository.open(config)) {
            CharacterSummary character = repository.characters("test").get(0);
            assertEquals(345, character.level());
            assertEquals(100_000, character.maxHp());
            assertEquals(80_000, character.maxMp());
            assertEquals(-500, character.armorClass());
            assertEquals(301, character.strength());
            assertEquals(302, character.constitution());
            assertEquals(303, character.dexterity());
            assertEquals(304, character.charisma());
            assertEquals(305, character.intelligence());
            assertEquals(306, character.wisdom());
        }
    }
}
