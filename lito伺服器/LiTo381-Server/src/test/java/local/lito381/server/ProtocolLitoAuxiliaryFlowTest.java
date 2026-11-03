package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolLitoAuxiliaryFlowTest {
    @Test void rankingAcceptsBothNativeProducersAndOnlyFiveExplicitRows() {
        assertEquals(Protocol.LitoRankingScope.OPEN,
                Protocol.litoRankingRequest(bytes(0xD7)));
        assertEquals(Protocol.LitoRankingScope.MONTHLY,
                Protocol.litoRankingRequest(bytes(0xD7, 0)));
        assertEquals(Protocol.LitoRankingScope.OVERALL,
                Protocol.litoRankingRequest(bytes(0xD7, 1)));
        assertEquals(Protocol.LitoRankingScope.CHANNEL,
                Protocol.litoRankingRequest(bytes(0xD7, 2)));
        assertEquals(Protocol.LitoRankingScope.CLASSIC_SERVER,
                Protocol.litoRankingRequest(bytes(0xD7, 3)));
        assertEquals(Protocol.LitoRankingScope.RACE,
                Protocol.litoRankingRequest(bytes(0xD7, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRankingRequest(bytes(0xD7, 5)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRankingRequest(bytes(0xD7, 0, 0)));
    }

    @Test void observerRequiresExactCcBooleanInsteadOfSyntheticEmptyToggle() {
        assertEquals(Protocol.LitoObserverState.DISABLED,
                Protocol.litoObserverRequest(bytes(0xE1, 0)));
        assertEquals(Protocol.LitoObserverState.ENABLED,
                Protocol.litoObserverRequest(bytes(0xE1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverRequest(bytes(0xE1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverRequest(bytes(0xE1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverRequest(bytes(0xE1, 1, 0)));
    }

    @Test void deathNewsAndMatchmakingCancelAreOpcodeOnly() {
        Protocol.litoConfirmDeathRequest(bytes(0xE0));
        Protocol.litoNewsRequest(bytes(0xE2));
        Protocol.litoQuitMatchMakingRequest(bytes(0xE4));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoConfirmDeathRequest(bytes(0xE0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoNewsRequest(bytes(0xE2, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoQuitMatchMakingRequest(bytes(0xE4, 0)));
    }

    @Test void buddyQueryNewsTriggerAndLoginTransitionUseExactOpcodeOnlyFrames() {
        Protocol.litoBuddyGroupQuery(bytes(0xE5));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuddyGroupQuery(bytes(0xE5, 0)));
        assertArrayEquals(bytes(0xE6), Protocol.litoNewsTrigger());
        assertArrayEquals(bytes(0xE7), Protocol.litoLoginOk());
    }

    @Test void rankingUsesCurrentVersionScdddRecordsAndNativeDwordCount() {
        byte[] encoded = Protocol.litoRanking(List.of(
                new GameStateStore.Ranking("Alice", 1, 1, 2, -3),
                new GameStateStore.Ranking("Bob", 4, 4, 5, 6)));
        Protocol.Reader in = new Protocol.Reader(encoded);
        assertEquals(0xD9, in.c());
        assertEquals(2, in.d());
        assertEquals("Alice", in.s());
        assertEquals(1, in.c());
        assertEquals(1, in.d());
        assertEquals(2, in.d());
        assertEquals(0xFFFF_FFFDL, in.d());
        assertEquals("Bob", in.s());
        assertEquals(4, in.c());
        assertEquals(4, in.d());
        assertEquals(5, in.d());
        assertEquals(6, in.d());
        assertEquals(0, in.remaining());
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoRanking(
                Collections.nCopies(257,
                        new GameStateStore.Ranking("TooMany", 0, 0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoRanking(List.of(
                new GameStateStore.Ranking("Invalid", -1, 0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoRanking(List.of(
                new GameStateStore.Ranking("Invalid", 7, 0, 0, 0))));
    }

    @Test void observerAndKickResultsMatchNativeSingleFieldReceivers() {
        assertArrayEquals(bytes(0xE5, 0), Protocol.litoObserverResult(
                Protocol.LitoObserverState.DISABLED));
        assertArrayEquals(bytes(0xE5, 1), Protocol.litoObserverResult(
                Protocol.LitoObserverState.ENABLED));
        assertArrayEquals(bytes(0xFC, 0, 0, 0, 0),
                Protocol.litoKickStatus(Protocol.LitoKickStatusResult.ACCEPTED));
        assertArrayEquals(bytes(0xFC, 1, 0, 0, 0),
                Protocol.litoKickStatus(Protocol.LitoKickStatusResult.REJECTED));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoKickStatus(null));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
