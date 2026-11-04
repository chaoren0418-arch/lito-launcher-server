package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;

/** Locks the one-config service-mode overlays to the production dual-JVM contract. */
final class LauncherRentalDualServerConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void oneProductionConfigRoutesRentalSourceToBattleJvm() throws Exception {
        Path file = Path.of("config", "server.properties");
        ServerConfig source = ServerConfig.load(file, "rental-source");
        ServerConfig battle = ServerConfig.load(file, "rental-battle");

            assertEquals("127.0.0.1", source.bind());
            assertEquals("127.0.0.2", source.gameBind());
            assertEquals(32000, source.port());
            assertEquals("127.0.0.2", source.advertisedHost());
            assertEquals("127.0.0.1", source.rentalTownDirectoryHost());
            assertEquals(32000, source.rentalTownDirectoryPort());
            assertTrue(source.rentalTownDirectoryEnabled());
            assertFalse(source.statePersistenceEnabled());
            assertEquals(2, source.rentalTeleportEndpoints().size());
            assertTrue(source.rentalTeleportEndpoints().stream().allMatch(endpoint ->
                    endpoint.host().equals("127.0.0.3") && endpoint.port() == 32001));

            assertEquals("127.0.0.3", battle.bind());
            assertEquals("127.0.0.4", battle.gameBind());
            assertEquals(32001, battle.port());
            assertFalse(battle.rentalTownDirectoryEnabled());
            assertTrue(battle.rentalTeleportEndpoints().isEmpty());
            assertTrue(battle.statePersistenceEnabled());
            assertTrue(source.rentalTeleportEndpoints().stream().allMatch(endpoint ->
                    endpoint.host().equals(battle.bind())
                            && endpoint.port() == battle.port()));

            assertTrue(source.databaseReadOnly());
            assertTrue(battle.databaseReadOnly());
            assertEquals(source.originSourceId(), battle.originSourceId());
            assertEquals(source.originSourceName(), battle.originSourceName());
            assertEquals(source.classicServerNames(), battle.classicServerNames());
            assertEquals(source.rentalBattleHandoffFile(), battle.rentalBattleHandoffFile());
            assertNotEquals(source.stateFile(), battle.stateFile());
    }
}
