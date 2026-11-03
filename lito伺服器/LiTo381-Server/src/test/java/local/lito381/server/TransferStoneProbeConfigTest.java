package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TransferStoneProbeConfigTest {
    @Test void remainsDisabledOutsideTheDedicatedLoopbackFixture() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .nativeTransferStoneProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.perfect2.properties"))
                .nativeTransferStoneProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"))
                .nativeTransferStoneProbeEnabled());

        ServerConfig fixture = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.transfer-stone-probe.properties"));
        assertTrue(fixture.nativeTransferStoneProbeEnabled());
        assertTrue(InetAddress.getByName(fixture.bind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.gameBind()).isLoopbackAddress());
        assertEquals(32100, fixture.port());
        assertEquals("memory", fixture.databaseMode());
        assertTrue(fixture.databaseReadOnly());
        assertFalse(fixture.statePersistenceEnabled());
        assertFalse(fixture.rentalTownDirectoryEnabled());
    }
}
