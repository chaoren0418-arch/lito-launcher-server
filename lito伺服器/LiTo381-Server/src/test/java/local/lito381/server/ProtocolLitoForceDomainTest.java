package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

final class ProtocolLitoForceDomainTest {
    @Test void selectableForceDomainIsExactlyZeroAndOne() {
        assertArrayEquals(new int[] {0, 1}, Arrays.stream(Protocol.LitoForce.values())
                .mapToInt(Protocol.LitoForce::wireValue).toArray());
        assertEquals(Protocol.LitoForce.FORCE_0, Protocol.LitoForce.fromWire(0));
        assertEquals(Protocol.LitoForce.FORCE_1, Protocol.LitoForce.fromWire(1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoForce.fromWire(2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoForce.fromWire(-1));

        assertEquals(Protocol.LitoForce.FORCE_0,
                Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 0}));
        assertEquals(Protocol.LitoForce.FORCE_1,
                Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 1}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 2}));
    }

    @Test void forceChangeAndParticipantWritersUseTheTypedDomain() {
        assertArrayEquals(new byte[] {(byte) 0xD4, 'A', 0, 0},
                Protocol.litoForceChanged("A", Protocol.LitoForce.FORCE_0));
        assertArrayEquals(new byte[] {(byte) 0xD4, 'A', 0, 1},
                Protocol.litoForceChanged("A", Protocol.LitoForce.FORCE_1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceChanged("A", null));

        for (Protocol.LitoForce force : Protocol.LitoForce.values()) {
            byte[] packet = Protocol.litoRoomParticipant(
                    1, "A", force, 0, 0,
                    new GameStateStore.TournamentStatistics(0, 0, 0, 0),
                    "", -1, 0, Protocol.LitoParticipantState.NOT_READY, 0,
                    GameStateStore.TournamentAwards.none());
            Protocol.Reader in = new Protocol.Reader(packet);
            assertEquals(Protocol.S_LITO_ROOM_USER, in.c());
            assertEquals(1, in.d());
            assertEquals("A", in.s());
            assertEquals(force.wireValue(), in.c());
        }
    }

    @Test void battleResultDomainIsExactlyNoWinnerOrOneOfTwoForces() {
        assertArrayEquals(new int[] {-1, 0, 1},
                Arrays.stream(Protocol.LitoBattleResult.values())
                        .mapToInt(Protocol.LitoBattleResult::wireValue).toArray());
        assertArrayEquals(new byte[] {(byte) 0xD3, (byte) 0xFF, (byte) 0xFF,
                        (byte) 0xFF, (byte) 0xFF},
                Protocol.battleResult(Protocol.LitoBattleResult.NO_WINNER));
        assertArrayEquals(new byte[] {(byte) 0xD3, 0, 0, 0, 0},
                Protocol.battleResult(Protocol.LitoBattleResult.FORCE_0_WON));
        assertArrayEquals(new byte[] {(byte) 0xD3, 1, 0, 0, 0},
                Protocol.battleResult(Protocol.LitoBattleResult.FORCE_1_WON));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoBattleResult.fromWinningForce(2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.battleResult(null));
    }

    @Test void productionWritersExposeNoRawForceOrResultOverload() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod("battleResult", int.class));
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod(
                        "litoForceChanged", String.class, int.class));

        Method request = Protocol.class.getDeclaredMethod("litoForceRequest", byte[].class);
        assertEquals(Protocol.LitoForce.class, request.getReturnType());
        assertTrue(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("litoRoomParticipant"))
                .allMatch(method -> method.getParameterTypes()[2]
                        == Protocol.LitoForce.class));
    }
}
