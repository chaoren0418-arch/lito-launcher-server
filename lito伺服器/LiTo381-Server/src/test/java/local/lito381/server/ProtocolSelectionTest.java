package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProtocolSelectionTest {
    @Test
    void vsServerStatusUsesNativeCountAndDwordTable() {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.vsServerStatus(
                        Protocol.LitoVsServerAvailability.AVAILABLE,
                        Protocol.LitoVsServerAvailability.UNAVAILABLE));

        assertEquals(Protocol.S_LITO_VS_SERVER_STATUS, packet.c());
        assertEquals(2, packet.c());
        assertEquals(0, packet.d());
        assertEquals(0xFFFF_FFFFL, packet.d());
        assertEquals(0, packet.remaining());
    }

    @Test
    void vsServerStatusProtectsTheExactTenDwordNativeTable() {
        Protocol.LitoVsServerAvailability[] ten =
                new Protocol.LitoVsServerAvailability[Protocol.MAX_LITO_VS_SERVERS];
        java.util.Arrays.fill(ten, Protocol.LitoVsServerAvailability.AVAILABLE);
        Protocol.Reader packet = new Protocol.Reader(Protocol.vsServerStatus(ten));
        assertEquals(Protocol.S_LITO_VS_SERVER_STATUS, packet.c());
        assertEquals(10, packet.c());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.vsServerStatus(
                        new Protocol.LitoVsServerAvailability[11]));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.vsServerStatus(
                        (Protocol.LitoVsServerAvailability[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.vsServerStatus(
                        new Protocol.LitoVsServerAvailability[] {null}));
    }

    @Test
    void restartAndPostTransitionPacketsAreNativeOpcodeOnlyFrames() {
        Protocol.Reader restart = new Protocol.Reader(Protocol.restart());
        assertEquals(Protocol.S_RESTART, restart.c());
        assertEquals(0, restart.remaining());

        Protocol.Reader allowed = new Protocol.Reader(Protocol.allowPostTransition());
        assertEquals(Protocol.S_LITO_VS_SERVER_ALLOW_JOIN, allowed.c());
        assertEquals(0, allowed.remaining());
    }
}
