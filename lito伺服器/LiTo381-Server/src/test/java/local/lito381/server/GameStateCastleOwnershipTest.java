package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateCastleOwnershipTest {
    @TempDir Path temporary;

    @Test
    void atomicallyPersistsReplacementOwnershipPerSourceAndRevision()
            throws Exception {
        Path file = temporary.resolve("castle-ownership.properties");
        GameStateStore state = new GameStateStore(file, true);

        GameStateStore.CastleOwnershipState first =
                state.transferCastleOwnership(
                        "LegacySun", 1, "NewClan", "NewLeader", 0x1122_3344L);
        assertEquals("LegacySun", first.sourceId());
        assertEquals(1, first.castleId());
        assertEquals("NewClan", first.clanName());
        assertEquals("NewLeader", first.leaderName());
        assertEquals(0x1122_3344L, first.leaderObjectId());
        assertEquals(1, first.revision());

        GameStateStore.CastleOwnershipState replaced =
                state.transferCastleOwnership(
                        "LEGACYSUN", 1, "SecondClan", "SecondLeader", 27);
        assertEquals(2, replaced.revision());
        assertEquals("SecondClan", replaced.clanName());

        GameStateStore.CastleOwnershipState other =
                state.transferCastleOwnership(
                        "LegacyMoon", 1, "MoonClan", "MoonLeader", 31);
        assertEquals(1, other.revision());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(replaced, reloaded.castleOwnership("legacysun", 1));
        assertEquals(other, reloaded.castleOwnership("LEGACYMOON", 1));
        assertNull(reloaded.castleOwnership("LegacySun", 2));
    }

    @Test
    void rejectsInvalidOwnershipWithoutChangingDurableState() throws Exception {
        Path file = temporary.resolve("invalid-castle-ownership.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.transferCastleOwnership("LegacySun", 1, "Clan", "Leader", 27);

        assertThrows(IllegalArgumentException.class,
                () -> state.transferCastleOwnership(
                        "LegacySun", 0, "Clan", "Leader", 27));
        assertThrows(IllegalArgumentException.class,
                () -> state.transferCastleOwnership(
                        "LegacySun", 1, "", "Leader", 27));
        assertThrows(IllegalArgumentException.class,
                () -> state.transferCastleOwnership(
                        "LegacySun", 1, "Clan", "Leader", 0));

        assertEquals("Clan", new GameStateStore(file, true)
                .castleOwnership("LegacySun", 1).clanName());
        assertEquals(1, new GameStateStore(file, true)
                .castleOwnership("LegacySun", 1).revision());
    }
}
