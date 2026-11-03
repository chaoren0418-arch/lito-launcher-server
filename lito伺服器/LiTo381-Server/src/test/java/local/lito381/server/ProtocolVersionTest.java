package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProtocolVersionTest {
    @Test
    void parsesExactNativeChdchForDirectAndConnectorLaunches() {
        Protocol.VersionRequest direct = Protocol.versionRequest(new Protocol.Writer()
                .c(Protocol.C_VERSION).h(0x8033).d(0x3B6).c(0).h(0x14F8).bytes());
        Protocol.VersionRequest connector = Protocol.versionRequest(new Protocol.Writer()
                .c(Protocol.C_VERSION).h(0x8033).d(0x3B6).c(2).h(0x14F8).bytes());

        assertEquals(0, direct.fromCd());
        assertEquals(2, connector.fromCd());
        assertTrue(Protocol.supportedVersion(direct));
        assertTrue(Protocol.supportedVersion(connector));
    }

    @Test
    void rejectsUnderflowTrailingAndUnsupportedVersionTuple() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.versionRequest(new byte[] {0, 0x33, (byte) 0x80}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.versionRequest(new Protocol.Writer()
                        .c(0).h(0x8033).d(0x3B6).c(0).h(0x14F8).c(0).bytes()));
        assertFalse(Protocol.supportedVersion(new Protocol.VersionRequest(
                0x8033, 0x3B6, 0, 0x14F7)));
    }

    @Test
    void writesExactNativeSuccessUnionAndShortResultOneRejection() {
        assertArrayEquals(new byte[] {
                0x3A, 0x00, 0x0D,
                0x33, (byte) 0xFD, 0x34, 0x07,
                0x30, (byte) 0xFD, 0x34, 0x07,
                (byte) 0xBA, 0x6E, (byte) 0xCF, 0x77,
                0x31, (byte) 0xFD, 0x34, 0x07,
                0, 0, 0, 0,
                0, 0, 3,
                (byte) 0x82, 0x7D, 0x07, 0
        }, Protocol.versionAccepted());
        assertArrayEquals(new byte[] {0x3A, 0x01}, Protocol.versionRejected());
    }
}
