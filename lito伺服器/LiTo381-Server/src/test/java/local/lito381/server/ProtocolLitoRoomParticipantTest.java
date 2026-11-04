package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolLitoRoomParticipantTest {
    @Test void selectedEventBuildReceivesIdentityStatisticsTeamAndRankInNativeScdSlots() {
        byte[] packet = Protocol.litoRoomParticipant(
                0x10203040L, "TournamentKnight", Protocol.LitoForce.FORCE_1, 3, 1,
                new GameStateStore.TournamentStatistics(14, 9, 5, 2),
                "Blue Dragons", 2, 321, Protocol.LitoParticipantState.READY, 10,
                new GameStateStore.TournamentAwards(
                        List.of(0x31, 0x72, 0xA4, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        List.of(2, 4, 6, 8, 10), true));
        Protocol.Reader in = new Protocol.Reader(packet);

        assertEquals(Protocol.S_LITO_ROOM_USER, in.c());
        assertEquals(0x10203040L, in.d());
        assertEquals("TournamentKnight", in.s());
        assertEquals(1, in.c());
        assertEquals(3, in.c());
        assertEquals(1, in.c());
        assertEquals(14, in.d());
        assertEquals(9, in.d());
        assertEquals(5, in.d());
        assertEquals(1, in.c());
        assertEquals("Blue Dragons", in.s());
        // Field 11 is parsed but discarded by every retained native branch.
        assertEquals(0, in.c());
        assertEquals("Ready", in.s());
        // Field 13 completes the native decided/win/loss/disconnect tuple.
        assertEquals(2, in.d());
        assertEquals(2, in.c());
        assertEquals(1, in.c());
        assertEquals(321, in.d());
        assertEquals(10, in.c());
        assertEquals(List.of(0x31, 0x72, 0xA4, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                java.util.stream.IntStream.range(0, 12).map(index -> in.c()).boxed().toList());
        assertEquals(List.of(2, 4, 6, 8, 10),
                java.util.stream.IntStream.range(0, 5).map(index -> in.c()).boxed().toList());
        assertEquals(1, in.d());
        assertEquals(0, in.remaining());
    }

    @Test void nativeAffiliationFieldRejectsOverlongOrInvalidRows() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                        "12345678901234567", 0, 0,
                        Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(0, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0), "", 0, 0,
                        Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight", null, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0), "", 0, 0,
                        Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 7, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0), "", 0, 0,
                        Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0), "", 0, -1,
                        Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        null, "", 0, 0, Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0), "", 5, 0,
                        Protocol.LitoParticipantState.NOT_READY, 0,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                        "", 0, 0, null, 0, GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                        "", 0, 0, Protocol.LitoParticipantState.NOT_READY, 15,
                        GameStateStore.TournamentAwards.none()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomParticipant(1, "Knight",
                        Protocol.LitoForce.FORCE_0, 0, 0,
                        new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                        "", 0, 0, Protocol.LitoParticipantState.NOT_READY, 0, null));
    }

    @Test void nativeTournamentAwardRowsRejectUnrenderableValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TournamentAwards(List.of(0),
                        List.of(0, 0, 0, 0, 0), false));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TournamentAwards(
                        List.of(0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        List.of(0, 0, 0, 0, 0), false));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TournamentAwards(
                        List.of(0x13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        List.of(0, 0, 0, 0, 0), false));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TournamentAwards(
                        List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        List.of(0, 0, 0, 0, 128), false));
    }

    @Test void selectedRendererBlankChannelSentinelIsPreservedAsSignedByte() {
        byte[] packet = Protocol.litoRoomParticipant(
                1, "TownKnight", Protocol.LitoForce.FORCE_0, 0, 0,
                new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                "", -1, 0, Protocol.LitoParticipantState.NOT_READY, 0,
                GameStateStore.TournamentAwards.none());
        Protocol.Reader in = new Protocol.Reader(packet);
        in.c();
        in.d();
        in.s();
        in.c();
        in.c();
        in.c();
        in.d();
        in.d();
        in.d();
        in.c();
        in.s();
        in.c();
        in.s();
        in.d();
        assertEquals(0xFF, in.c());
    }
}
