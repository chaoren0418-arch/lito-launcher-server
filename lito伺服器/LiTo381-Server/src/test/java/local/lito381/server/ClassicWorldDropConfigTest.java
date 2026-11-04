package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ClassicWorldDropConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test
    void productionKeepsRetainedAutoLootTwoAndGroundAcceptanceUsesZero()
            throws Exception {
        ServerConfig production = ServerConfig.load(Path.of("config/server.properties"));
        ServerConfig reward = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-npc-reward-selftest.properties"));
        ServerConfig ground = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-drop-selftest.properties"));
        ServerConfig characterSelftest = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        ServerConfig party = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-party-reward-selftest.properties"));

        assertEquals(2, production.classicWorldDropAutoLoot());
        assertEquals(15, production.classicWorldDropLootingRange());
        assertEquals(1.0, production.classicWorldDropItemRate());
        assertEquals(1.0, production.classicWorldDropAdenaRate());
        assertFalse(production.nativePlayerDropEnabled());
        assertEquals(true, characterSelftest.nativePlayerDropEnabled());
        assertEquals(1.0, production.classicWorldExperienceRate());
        assertEquals(1.0, production.classicWorldLawfulRate());
        assertEquals(14, production.classicWorldScarecrowMaxLevel());
        assertEquals(2, reward.classicWorldDropAutoLoot());
        assertEquals(0, ground.classicWorldDropAutoLoot());
        assertEquals(1.0, ground.classicWorldDropItemRate());
        assertEquals(1.0, ground.classicWorldDropAdenaRate());
        assertEquals(0.0, party.classicWorldDropItemRate());
        assertEquals(0.0, party.classicWorldDropAdenaRate());
        assertEquals(1.0, party.classicWorldExperienceRate());
        assertEquals(1.0, party.classicWorldLawfulRate());
    }
}
