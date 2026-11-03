package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

final class ProtocolLitoCharacterDirectoryTest {
    private static Protocol.LitoCharacterDirectoryRow testMage() {
        return new Protocol.LitoCharacterDirectoryRow(
                "TestMage", "", 1, 0, -1234,
                110, 50, 10, 47,
                13, 10, 10, 11, 9, 8, 0,
                new Protocol.LitoCharacterRankings(
                        1, 2, 3, 4, 5,
                        -1, 2, -3, 4, -5),
                new Protocol.LitoCharacterBattleStatistics(
                        3, 14, 10, 4, 2));
    }

    @Test void writesExactNativeDirectoryEnvelope() {
        assertArrayEquals(new byte[] {0x03, 0x01},
                Protocol.litoCharacterDirectoryCount(1));
        assertArrayEquals(new byte[] {0x04}, Protocol.specialCharacterServerEvent());
        assertArrayEquals(new byte[] {0x05}, Protocol.newCharacterInfo());

        byte[] packet = Protocol.litoCharacterDirectoryRow(testMage());
        Protocol.Reader in = new Protocol.Reader(packet);
        assertEquals(Protocol.S_LITO_CHARACTER_INFO, in.c());
        assertEquals("TestMage", in.s());
        assertEquals("", in.s());
        assertEquals(1, in.c());
        assertEquals(0, in.c());
        assertEquals(0xFB2E, in.h());
        assertEquals(110, in.h());
        assertEquals(50, in.h());
        assertEquals(10, in.c());
        assertEquals(47, in.c());
        assertEquals(13, in.c());
        assertEquals(10, in.c());
        assertEquals(10, in.c());
        assertEquals(11, in.c());
        assertEquals(9, in.c());
        assertEquals(8, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals(1, in.d());
        assertEquals(4, in.d());
        assertEquals(2, in.d());
        assertEquals(3, in.d());
        assertEquals(5, in.d());
        assertEquals(3, in.d());
        assertEquals(14, in.d());
        assertEquals(10, in.d());
        assertEquals(4, in.d());
        assertEquals(2, in.c());
        assertEquals(0xFFFF_FFFFL, in.d());
        assertEquals(4, in.d());
        assertEquals(2, in.d());
        assertEquals(0xFFFF_FFFDL, in.d());
        assertEquals(0xFFFF_FFFBL, in.d());
        assertEquals(47 ^ 13 ^ 10 ^ 10 ^ 11 ^ 9 ^ 8, in.c());
        assertEquals(0, in.remaining());
    }

    @Test void rejectsRowsThatWouldOverflowOrPoisonNativeSlots() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterDirectoryCount(9));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterDirectoryRow(new Protocol.LitoCharacterDirectoryRow(
                        "A".repeat(28), "", 1, 0, 0,
                        50, 10, 10, 1, 12, 12, 12, 12, 12, 12, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterDirectoryRow(new Protocol.LitoCharacterDirectoryRow(
                        "Hero", "C".repeat(17), 1, 0, 0,
                        50, 10, 10, 1, 12, 12, 12, 12, 12, 12, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterDirectoryRow(new Protocol.LitoCharacterDirectoryRow(
                        "Hero", "", 7, 0, 0,
                        50, 10, 10, 1, 12, 12, 12, 12, 12, 12, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterDirectoryRow(new Protocol.LitoCharacterDirectoryRow(
                        "Hero", "", 1, 0, 0,
                        32768, 10, 10, 1, 12, 12, 12, 12, 12, 12, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoCharacterBattleStatistics(2, 2, 1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoCharacterBattleStatistics(0, 2, 1, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoCharacterBattleStatistics(0, 0, 0, 0, 5));
    }

    @Test void acceptsNative381DragonKnightAndIllusionistClassBytes() {
        for (int type : new int[] {5, 6}) {
            byte[] packet = Protocol.litoCharacterDirectoryRow(
                    new Protocol.LitoCharacterDirectoryRow(
                            "Hero", "", type, 0, 0,
                            50, 10, 10, 52, 13, 10, 10, 11, 9, 13, 0));
            Protocol.Reader in = new Protocol.Reader(packet);
            assertEquals(Protocol.S_LITO_CHARACTER_INFO, in.c());
            assertEquals("Hero", in.s());
            assertEquals("", in.s());
            assertEquals(type, in.c());
        }
    }

    @Test void specialCharacterServerEventHasOnlyTheGuardedRuntimeFixtureRoute()
            throws Exception {
        Path main = Path.of("src/main/java");
        int calls = 0;
        int symbolicRoutes = 0;
        try (var sources = Files.walk(main)) {
            for (Path path : sources.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .endsWith(".java"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                calls += occurrences(source, "Protocol.specialCharacterServerEvent(");
                symbolicRoutes += occurrences(source, "Protocol.S_CHARACTER_INFO");
            }
        }
        assertEquals(1, calls,
                "S_04 must have exactly one guarded runtime-fixture call site");
        assertEquals(0, symbolicRoutes,
                "normal production symbolically routes the special S_04 event");

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int fixtureStart = session.indexOf(
                "private void sendNativeCharacterEventProbe()");
        int fixtureEnd = session.indexOf("private void sendLitoServerList()", fixtureStart);
        assertFalse(fixtureStart < 0 || fixtureEnd <= fixtureStart);
        String fixture = session.substring(fixtureStart, fixtureEnd);
        assertEquals(1, occurrences(fixture,
                "Protocol.specialCharacterServerEvent("));
        assertFalse(!fixture.contains("config.nativeCharacterEventProbeEnabled()")
                || !fixture.contains("\"memory\".equals(config.databaseMode())")
                || !fixture.contains("socket.getInetAddress().isLoopbackAddress()")
                || !fixture.contains("endpoint != Endpoint.LOGIN")
                || !fixture.contains("phase != Phase.AUTHENTICATED"));

        String protocol = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Protocol.java"),
                StandardCharsets.UTF_8);
        assertEquals(1, occurrences(protocol,
                "static byte[] specialCharacterServerEvent()"));
        assertEquals(1, occurrences(protocol, ".c(S_CHARACTER_INFO)"));
        assertFalse(protocol.contains("characterInfoChanged"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int offset = 0; (offset = text.indexOf(needle, offset)) >= 0;
             offset += needle.length()) {
            count++;
        }
        return count;
    }
}
