package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

final class AttackAllProbeConfigTest {
    @Test void remainsDisabledOutsideTheDedicatedLoopbackFixture() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .attackAllProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.perfect2.properties"))
                .attackAllProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"))
                .attackAllProbeEnabled());

        ServerConfig fixture = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.attack-all-probe.properties"));
        assertTrue(fixture.attackAllProbeEnabled());
        assertTrue(InetAddress.getByName(fixture.bind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.gameBind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.advertisedHost()).isLoopbackAddress());
        assertEquals(32100, fixture.port());
        assertEquals("memory", fixture.databaseMode());
        assertTrue(fixture.databaseReadOnly());
        assertFalse(fixture.statePersistenceEnabled());
        assertTrue(fixture.administrationAccounts().contains("test"));
    }

    @Test void rejectsUnsafeProbeConfigurationsAtLoadTime() throws Exception {
        String fixture = Files.readString(
                Path.of("src/test/resources/server-config/config/server.attack-all-probe.properties"),
                StandardCharsets.UTF_8);
        assertRejected(fixture.replace("database.mode=memory",
                "database.mode=legacy_mysql"));
        assertRejected(fixture.replace("database.read.only=true",
                "database.read.only=false"));
        assertRejected(fixture.replace("state.persistence.enabled=false",
                "state.persistence.enabled=true"));
        assertRejected(fixture.replace("port=32100", "port=32099"));
        assertRejected(fixture.replace("bind=127.0.0.1", "bind=0.0.0.0"));
        assertRejected(fixture.replace("game.bind=127.0.0.2", "game.bind=0.0.0.0"));
        assertRejected(fixture.replace("advertised.host=127.0.0.2",
                "advertised.host=192.0.2.1"));
        assertRejected(fixture.replace("login.bootstrap.sequence="
                        + "login_check,classic_server_list,lito_server_list",
                "login.bootstrap.sequence=login_check,lito_server_list"));
    }

    @Test void productionSourceHasOnlyTheGuardedFixtureCallSite() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertEquals(1, occurrences(session, "Protocol.attackAll("));
        int start = session.indexOf("private boolean sendAttackAllProbe(");
        int end = session.indexOf("private void tell(", start);
        assertTrue(start >= 0 && end > start);
        String fixture = session.substring(start, end);
        assertTrue(fixture.contains("config.attackAllProbeEnabled()"));
        assertTrue(fixture.contains("\"memory\".equals(config.databaseMode())"));
        assertTrue(fixture.contains("config.databaseReadOnly()"));
        assertTrue(fixture.contains("!config.statePersistenceEnabled()"));
        assertTrue(fixture.contains("config.port() == 32100"));
        assertTrue(fixture.contains("socket.getInetAddress().isLoopbackAddress()"));
        assertTrue(fixture.contains("!isAdministrator() || phase != Phase.BATTLE"));
        assertTrue(fixture.contains("Protocol.attackAll("));

        try (var configs = Files.list(Path.of("config"))) {
            for (Path path : configs.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .endsWith(".properties"))
                    .filter(candidate -> !candidate.getFileName().toString()
                            .equals("server.attack-all-probe.properties"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                assertFalse(source.contains("attack.all.probe.enabled=true"),
                        "non-fixture config enables attack-all: " + path);
            }
        }
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static void assertRejected(String source) throws Exception {
        Path path = Files.createTempFile("lito-attack-all-unsafe-", ".properties");
        try {
            Files.writeString(path, source, StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(path));
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
