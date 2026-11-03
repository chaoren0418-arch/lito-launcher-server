package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

final class ProtocolLitoParticipantStateDomainTest {
    @Test void participantStateDomainIsExactlyNotReadyReadyAndShopList() {
        assertArrayEquals(new int[] {0, 1, 2},
                Arrays.stream(Protocol.LitoParticipantState.values())
                        .mapToInt(Protocol.LitoParticipantState::wireValue).toArray());
        assertEquals(Protocol.LitoParticipantState.NOT_READY,
                Protocol.LitoParticipantState.fromWire(0));
        assertEquals(Protocol.LitoParticipantState.READY,
                Protocol.LitoParticipantState.fromWire(1));
        assertEquals(Protocol.LitoParticipantState.SHOP_LIST,
                Protocol.LitoParticipantState.fromWire(2));
        assertFalse(Protocol.LitoParticipantState.NOT_READY.ready());
        assertTrue(Protocol.LitoParticipantState.READY.ready());
        assertFalse(Protocol.LitoParticipantState.SHOP_LIST.ready());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoParticipantState.fromWire(3));
    }

    @Test void cD2KeepsOwnerStartSeparateFromThreeParticipantStates() {
        Protocol.LitoReadyRequest owner = Protocol.litoReadyRequest(
                new byte[] {(byte) 0xD2});
        assertTrue(owner.ownerStart());
        assertNull(owner.participantState());

        for (Protocol.LitoParticipantState state : Protocol.LitoParticipantState.values()) {
            Protocol.LitoReadyRequest request = Protocol.litoReadyRequest(new byte[] {
                    (byte) 0xD2, (byte) state.wireValue()
            });
            assertFalse(request.ownerStart());
            assertEquals(Protocol.LitoReadyOperation.PARTICIPANT_STATE,
                    request.operation());
            assertEquals(state, request.participantState());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoReadyRequest(
                        Protocol.LitoReadyOperation.OWNER_START,
                        Protocol.LitoParticipantState.READY));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoReadyRequest(
                        Protocol.LitoReadyOperation.PARTICIPANT_STATE, null));
    }

    @Test void sD1AndInitialScdRosterWriteEveryTypedStateExactly() {
        for (Protocol.LitoParticipantState state : Protocol.LitoParticipantState.values()) {
            assertArrayEquals(new byte[] {(byte) 0xD1, 'A', 0,
                            (byte) state.wireValue()},
                    Protocol.readyState("A", state));

            byte[] participant = Protocol.litoRoomParticipant(
                    1, "A", Protocol.LitoForce.FORCE_0, 0, 0,
                    new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                    "", -1, 0, state, 0,
                    GameStateStore.TournamentAwards.none());
            Protocol.Reader in = new Protocol.Reader(participant);
            in.c();
            in.d();
            in.s();
            in.c();
            in.c();
            in.c();
            in.d();
            in.d();
            in.d();
            assertEquals(state.wireValue(), in.c());
            in.s();
            in.c();
            in.s();
            in.d();
            in.c();
            assertEquals(state.wireValue(), in.c());
        }
        assertArrayEquals(Protocol.readyState("A", Protocol.LitoParticipantState.READY),
                Protocol.readyState("A", true));
        assertArrayEquals(Protocol.readyState("A", Protocol.LitoParticipantState.NOT_READY),
                Protocol.readyState("A", false));
    }

    @Test void productionBoundaryExposesNoRawIntegerStateWriter() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod(
                        "readyState", String.class, int.class));
        Method participantState = Protocol.LitoReadyRequest.class.getDeclaredMethod(
                "participantState");
        assertEquals(Protocol.LitoParticipantState.class,
                participantState.getReturnType());
        assertTrue(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("litoRoomParticipant"))
                .allMatch(method -> method.getParameterTypes()[9]
                        == Protocol.LitoParticipantState.class));
    }
}
