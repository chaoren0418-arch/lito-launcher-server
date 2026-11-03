package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ServerConfigChannelPortsTest {
    @TempDir Path temporary;

    @Test void productionStandardModeUsesOneGamePortForFourIsolatedLobbies()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"), "standard");
        assertFalse(config.dedicatedChannelPortsEnabled());
        assertEquals(List.of(32000), config.channelPorts());
        assertEquals(32000, config.channelPort(2));
    }

    @Test void rentalModesRemoveStandardChannelPortsToAvoidEndpointOverlap()
            throws Exception {
        ServerConfig source = ServerConfig.load(
                Path.of("config/server.properties"), "rental-source");
        ServerConfig battle = ServerConfig.load(
                Path.of("config/server.properties"), "rental-battle");
        assertFalse(source.dedicatedChannelPortsEnabled());
        assertFalse(battle.dedicatedChannelPortsEnabled());
        assertEquals(List.of(32000), source.channelPorts());
        assertEquals(List.of(32001), battle.channelPorts());
    }

    @Test void rejectsLegacyDedicatedChannelPortConfiguration() throws Exception {
        String baseline = Files.readString(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        assertInvalid(baseline + "\nlito.channel.ports=32100,32101,32102,32103\n");
    }

    private void assertInvalid(String properties) throws Exception {
        Path config = Files.createTempFile(temporary, "server-channel-", ".properties");
        Files.writeString(config, properties);
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(config));
    }
}
