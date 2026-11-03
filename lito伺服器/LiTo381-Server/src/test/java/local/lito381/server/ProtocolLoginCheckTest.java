package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolLoginCheckTest {
    @Test
    void writesExactNativeSuccessCcdschEnvelope() {
        assertArrayEquals(new byte[] {
                0x00, 0x00, 0x10, 0x0E, 0x00, 0x00,
                0x4C, 0x69, 0x54, 0x6F, 0x00,
                0x00, 0x00, 0x00
        }, Protocol.loginCheck(Protocol.LoginCheckResult.ACCEPTED,
                3_600, "LiTo", 0, 0));
    }

    @Test
    void writesEveryServerOwnedFailureWithTheSameFullBody() {
        for (Protocol.LoginCheckResult result : new Protocol.LoginCheckResult[] {
                Protocol.LoginCheckResult.SYSTEM_ERROR,
                Protocol.LoginCheckResult.ACCESS_FAILED,
                Protocol.LoginCheckResult.ACCOUNT_BANNED,
                Protocol.LoginCheckResult.ACCOUNT_IN_USE
        }) {
            assertArrayEquals(new byte[] {
                    0x00, (byte) result.wireValue(), 0, 0, 0, 0,
                    0x4C, 0x69, 0x54, 0x6F, 0x00,
                    0x00, 0x00, 0x00
            }, Protocol.loginCheck(result, 0, "LiTo", 0, 0));
        }
    }

    @Test
    void rejectsValuesThatCannotFitTheNativeReceiverStorage() {
        assertThrows(NullPointerException.class,
                () -> Protocol.loginCheck(null, 0, "LiTo", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginCheck(Protocol.LoginCheckResult.ACCEPTED,
                        0x1_0000_0000L, "LiTo", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginCheck(Protocol.LoginCheckResult.ACCEPTED,
                        0, "", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginCheck(Protocol.LoginCheckResult.ACCEPTED,
                        0, "x".repeat(41), 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginCheck(Protocol.LoginCheckResult.ACCEPTED,
                        0, "LiTo", 0x100, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginCheck(Protocol.LoginCheckResult.ACCEPTED,
                        0, "LiTo", 0, 0x1_0000));
    }
}
