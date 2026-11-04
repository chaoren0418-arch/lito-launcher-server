package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameHubCharacterTest {
    @Test void lateNativeCharacterSelectionIsLimitedToOwnedLoginRoster() {
        List<String> roster = List.of("3331", "3332", "3333", "3366");
        assertTrue(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, false,
                "3333", "3331", roster, "太陽神"));
        assertTrue(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, false,
                "3333", "", roster, "太陽神"),
                "the native empty cached-name producer may select only the account-name row");

        assertFalse(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, false,
                "3333", "foreign", roster, "太陽神"));
        assertFalse(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.GAME, false,
                "3333", "3331", roster, "太陽神"),
                "an early GAME C_C9 must not bypass the S_E6/C_E2 readiness barrier");
        assertFalse(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, true,
                "3333", "3331", roster, "太陽神"),
                "the authenticated compatibility path remains one-shot");
        assertFalse(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, false,
                "3333", "", List.of("3331", "3332"), "太陽神"),
                "an empty request cannot fall back to an arbitrary first row");

        assertTrue(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, false,
                "single", "single(太陽神)", List.of("single"), "太陽神"));
        assertFalse(ClientSession.isLateAuthenticatedLoginCharacterSelection(
                ClientSession.Endpoint.LOGIN, false,
                "single", "single(月神)", List.of("single"), "太陽神"));
    }

    @Test void characterSelectionRequiresTheExactNativeCsEnvelope() {
        assertEquals("TestKnight", Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("TestKnight").bytes()));
        assertEquals("A".repeat(27), Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("A".repeat(27)).bytes()));
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_SELECT_CHAR_RESULT, 0},
                Protocol.litoCharacterSelectResult(
                        Protocol.LitoCharacterSelectResult.ACCEPTED));
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_SELECT_CHAR_RESULT, 1},
                Protocol.litoCharacterSelectResult(
                        Protocol.LitoCharacterSelectResult.SILENT_REJECTED));
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_SELECT_CHAR_RESULT, 7},
                Protocol.litoCharacterSelectResult(
                        Protocol.LitoCharacterSelectResult.PARTICIPATION_RESTRICTED));
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_SELECT_CHAR_RESULT, 8},
                Protocol.litoCharacterSelectResult(
                        Protocol.LitoCharacterSelectResult.INVALID_CHARACTER_NAME));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSelectResult(null));

        assertThrows(IllegalArgumentException.class, () -> Protocol.litoCharacterSelectRequest(
                new byte[] {(byte) Protocol.C_LITO_SELECT_CHAR}));
        assertEquals("", Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("A".repeat(28)).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("Bad\nName").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoCharacterSelectRequest(
                new byte[] {(byte) Protocol.C_LITO_SELECT_CHAR, (byte) 0x81, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("TestKnight").c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoCharacterSelectRequest(
                new Protocol.Writer().c(Protocol.C_LITO_ENTER_CHANNEL).s("TestKnight").bytes()));
    }

    @Test void serializesCharacterSelectionAndDeletion() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession second = session(config, hub);

            assertFalse(hub.selectCharacter(first, "CompatHero"));
            hub.connect(first);
            hub.connect(second);

            assertTrue(hub.selectCharacter(first, "CompatHero"));
            assertFalse(hub.selectCharacter(second, "compathero"));
            assertFalse(hub.beginCharacterDeletion("CompatHero", second));

            hub.disconnect(first);
            assertTrue(hub.beginCharacterDeletion("COMPATHERO", second));
            assertFalse(hub.selectCharacter(second, "CompatHero"));
            hub.endCharacterDeletion("compathero");
            assertTrue(hub.selectCharacter(second, "CompatHero"));
        }
    }

    @Test void sameDisplayNameOnDifferentClassicSourcesHasIndependentLocks() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession second = session(config, hub);
            hub.connect(first);
            hub.connect(second);

            String sun = GameStateStore.sourceCharacter("Sun", "SameHero");
            String moon = GameStateStore.sourceCharacter("Moon", "SameHero");
            assertTrue(hub.selectCharacter(first, sun));
            assertTrue(hub.selectCharacter(second, moon));
            assertFalse(hub.beginCharacterDeletion(sun, second));
            assertTrue(hub.beginCharacterDeletion(
                    GameStateStore.sourceCharacter("Star", "SameHero"), second));
        }
    }

    @Test void nameOnlyOnlineLookupsPreferRequesterSourceAndRejectGlobalAmbiguity(
            @TempDir Path directory) throws Exception {
        ServerConfig hubConfig = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        ServerConfig sunConfig = sourceConfig(directory.resolve("sun.properties"), "Sun");
        ServerConfig moonConfig = sourceConfig(directory.resolve("moon.properties"), "Moon");
        try (GameHub hub = new GameHub(hubConfig)) {
            ClientSession sun = session(sunConfig, hub);
            ClientSession moon = session(moonConfig, hub);
            hub.connect(sun);
            hub.connect(moon);
            assertEquals(0, hub.enterChannel(0, sun).id());
            assertEquals(0, hub.enterChannel(0, moon).id());

            assertNull(hub.activeMemberByName("TestKnight"));
            assertSame(sun, hub.activeMemberByName(sun, "TestKnight"));
            assertSame(moon, hub.activeMemberByName(moon, "TestKnight"));
            assertSame(sun, hub.activeMemberByIdentity(
                    GameStateStore.sourceCharacter("Sun", "TestKnight")));

            long room = hub.createRoom(0, new GameHub.RoomSettings(
                    "Source Lookup", "", 4, 0, 0, 0, 0, 0));
            assertTrue(hub.tryJoinRoom(room, sun).joined());
            assertTrue(hub.tryJoinRoom(room, moon).joined());
            assertNull(hub.memberByName(room, "TestKnight"));
            assertSame(sun, hub.memberByName(room, sun, "TestKnight"));
            assertSame(moon, hub.memberByName(room, moon, "TestKnight"));
        }
    }

    private static ServerConfig sourceConfig(Path path, String source) throws Exception {
        Files.writeString(path, """
                database.mode=memory
                login.classic.server.names=%s
                """.formatted(source));
        return ServerConfig.load(path);
    }

    private static ClientSession session(ServerConfig config, GameHub hub) {
        return new ClientSession(null, ClientSession.Endpoint.GAME, config,
                null, null, LocalResources.discardCapture(), hub);
    }
}
