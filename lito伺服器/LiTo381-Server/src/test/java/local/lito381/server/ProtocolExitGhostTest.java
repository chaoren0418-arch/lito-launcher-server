package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolExitGhostTest {
    @Test void acceptsOnlyExactOpcodeOnlyEnvelope() {
        Protocol.requireExitGhost(new byte[] {(byte) Protocol.C_EXIT_GHOST});

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireExitGhost(new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireExitGhost(new byte[] {(byte) Protocol.C_EXIT_GHOST, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireExitGhost(new byte[] {(byte) Protocol.C_CHECK_PK}));
    }
}
