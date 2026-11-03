package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CloneAppearanceProbeConfigTest {
    @Test void remainsDisabledOutsideTheDedicatedLoopbackFixture() throws Exception {
        ServerConfig production = ServerConfig.load(Path.of("config/server.properties"));
        assertFalse(production.cloneAppearanceProbeEnabled());
        assertFalse(production.battleCompatibilityServiceActorsEnabled(),
                "native production profile must not inject classic service actors into battle");
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.perfect2.properties"))
                .cloneAppearanceProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"))
                .cloneAppearanceProbeEnabled());

        ServerConfig fixture = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.clone-appearance-probe.properties"));
        assertTrue(fixture.cloneAppearanceProbeEnabled());
        assertTrue(InetAddress.getByName(fixture.bind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.gameBind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.advertisedHost()).isLoopbackAddress());
        assertEquals(32100, fixture.port());
        assertEquals("memory", fixture.databaseMode());
        assertTrue(fixture.databaseReadOnly());
        assertFalse(fixture.statePersistenceEnabled());
        assertFalse(fixture.battleCompatibilityServiceActorsEnabled());
        assertTrue(fixture.administrationAccounts().contains("test"));
        assertTrue(fixture.administrationAccounts().contains("test2"));

        ServerConfig physicalFixture = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.clone-appearance-physical-probe.properties"));
        assertTrue(physicalFixture.cloneAppearanceProbeEnabled());
        assertTrue(InetAddress.getByName(physicalFixture.bind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(physicalFixture.gameBind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(
                physicalFixture.advertisedHost()).isLoopbackAddress());
        assertEquals(32100, physicalFixture.port());
        assertEquals("memory", physicalFixture.databaseMode());
        assertTrue(physicalFixture.databaseReadOnly());
        assertFalse(physicalFixture.statePersistenceEnabled());
        assertFalse(physicalFixture.battleCompatibilityServiceActorsEnabled());
        assertTrue(physicalFixture.nativeSourceSelectionProbeEnabled());
        assertEquals(2, physicalFixture.classicServerNames().size());
    }

    @Test void productionSourceHasOnlyMutationAndExactRestoreInsideFixture()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertEquals(2, occurrences(session, "Protocol.cloneAppearance("));
        int start = session.indexOf("private boolean sendCloneAppearanceProbe(");
        int end = session.indexOf("private boolean sendAttackAllProbe(", start);
        assertTrue(start >= 0 && end > start);
        String fixture = session.substring(start, end);
        assertTrue(fixture.contains("config.cloneAppearanceProbeEnabled()"));
        assertTrue(fixture.contains("\"memory\".equals(config.databaseMode())"));
        assertTrue(fixture.contains("config.databaseReadOnly()"));
        assertTrue(fixture.contains("!config.statePersistenceEnabled()"));
        assertTrue(fixture.contains("config.port() == 32100"));
        assertTrue(fixture.contains("socket.getInetAddress().isLoopbackAddress()"));
        assertTrue(fixture.contains("!isAdministrator() || phase != Phase.BATTLE"));
        assertTrue(fixture.contains("playerObjectId(), hub.clanEmblemId"));
        assertTrue(fixture.contains("138, nativeLawful(characterLawful())"));
        assertTrue(fixture.contains("battleGfx(), nativeLawful(characterLawful())"));
        assertTrue(fixture.contains("scheduleCloneAppearanceProbeRestore"));
        assertTrue(fixture.contains("scheduleDelayedRestore ? 5000 : 1500"));
        assertTrue(fixture.contains("if (scheduleDelayedRestore)"));
        assertTrue(session.contains("scheduleCloneAppearanceProbeAutoBattle"));
        assertTrue(session.contains("scheduleCloneAppearanceProbeAutoReady"));
        assertTrue(session.contains("native-room-handoff-ms="));
        assertEquals(1_500, GameHub.CLONE_PROBE_NATIVE_ROOM_HANDOFF_MILLIS);
        assertTrue(session.contains("S_CLONE native receiver dwell"));
        assertTrue(session.contains("serverCloneAppearanceProbeRestore(currentRoomId)"));
        assertTrue(fixture.contains("serverCloneAppearanceProbeAutoBattle"));

        try (var configs = Files.list(Path.of("config"))) {
            for (Path path : configs.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .endsWith(".properties"))
                    .filter(candidate -> !java.util.Set.of(
                            "server.clone-appearance-probe.properties",
                            "server.clone-appearance-physical-probe.properties")
                            .contains(candidate.getFileName().toString()))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                assertFalse(source.contains("clone.appearance.probe.enabled=true"),
                        "non-fixture config enables clone appearance: " + path);
            }
        }
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }
}
