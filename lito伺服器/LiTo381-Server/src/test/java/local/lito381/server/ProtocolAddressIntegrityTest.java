package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProtocolAddressIntegrityTest {
    @Test void writesExactNativeEvent33ChallengeAndParsesAddressProof() {
        assertArrayEquals(new byte[] {
                0x7B, 0x21,
                0x04, 0x03, 0x02, 0x01,
                0x40, (byte) 0xD8, 0x4E, 0x00,
                0x05, 0x00, 0x00, 0x00
        }, Protocol.addressChallenge(0x0102_0304L,
                NativeClientIntegrity.RANGE_START, NativeClientIntegrity.RANGE_LENGTH));

        Protocol.AddressProof proof = Protocol.addressProof(new Protocol.Writer()
                .c(Protocol.C_ADDR).d(0x0102_0304L)
                .d(NativeClientIntegrity.EXPECTED_CHECKSUM).d(1).bytes());
        assertEquals(0x0102_0304L, proof.challengeId());
        assertEquals(NativeClientIntegrity.EXPECTED_CHECKSUM, proof.checksum());
        assertEquals(1, proof.protectedState());
    }

    @Test void reproducesNativeSeededCrcOverRelocationFreeAnchor() {
        assertEquals(NativeClientIntegrity.EXPECTED_CHECKSUM,
                NativeClientIntegrity.seededCrc(
                        new byte[] {(byte) 0xE8, 0, 0, 0, 0},
                        NativeClientIntegrity.CRC_SEED));
        assertTrue(NativeClientIntegrity.checksumMatches(0x1569_D958L));
    }

    @Test void rejectsMalformedChallengesAndProofs() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.addressChallenge(0, 0x004E_D840L, 5));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.addressProof(new byte[] {0x2A}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.addressProof(new Protocol.Writer()
                        .c(Protocol.C_ADDR).d(1).d(2).d(2).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.addressProof(new Protocol.Writer()
                        .c(Protocol.C_ADDR).d(0).d(2).d(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.addressProof(new Protocol.Writer()
                        .c(Protocol.C_ADDR).d(1).d(2).d(0).c(0).bytes()));
    }
}
