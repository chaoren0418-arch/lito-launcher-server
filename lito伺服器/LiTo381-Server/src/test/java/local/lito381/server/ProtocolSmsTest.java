package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProtocolSmsTest {
    @Test
    void parsesBothStableLayoutsAndTheExactBrokenUiProducerArtifact() {
        Protocol.SmsRequest explicit = parse(new Protocol.Writer()
                .c(Protocol.C_SMS).c(0).s("Arena starts at 8").s("0912345678").bytes());
        assertEquals(0, explicit.mode());
        assertEquals("Arena starts at 8", explicit.message());
        assertEquals("0912345678", explicit.destination());
        assertArrayEquals(new byte[0], explicit.nativeProducerArtifact());

        Protocol.SmsRequest implicit = parse(new Protocol.Writer()
                .c(Protocol.C_SMS).c(1).s("Arena starts at 8").bytes());
        assertEquals(1, implicit.mode());
        assertEquals("Arena starts at 8", implicit.message());
        assertEquals("", implicit.destination());
        assertArrayEquals(new byte[0], implicit.nativeProducerArtifact());

        Protocol.SmsRequest brokenUi = parse(new Protocol.Writer()
                .c(Protocol.C_SMS).c(1).s("Arena starts at 8")
                .raw(new byte[] {(byte) 0x9C, (byte) 0x93, 0x4F, 0}).bytes());
        assertArrayEquals(new byte[] {(byte) 0x9C, (byte) 0x93, 0x4F},
                brokenUi.nativeProducerArtifact());

        assertArrayEquals(new byte[] {(byte) Protocol.S_EVENT, 6, 0},
                Protocol.smsResult(0));
        assertArrayEquals(new byte[] {(byte) Protocol.S_EVENT, 6, (byte) 0xFF},
                Protocol.smsResult(0xFF));
    }

    @Test
    void rejectsShapesNoStableOrExactNativeProducerCanUse() {
        assertInvalid(new Protocol.Writer().c(Protocol.C_MOVE).c(0).s("message")
                .s("0912345678").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(2).s("message").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(0).s("")
                .s("0912345678").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(0).s("message")
                .s("").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(0)
                .s("X".repeat(0xC8)).s("0912345678").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(0).s("message")
                .s("1".repeat(0x1C)).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(1).s("message")
                .raw(new byte[] {1, 2, 3, 4, 0}).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(1).s("message")
                .raw(new byte[] {1, 2, 3, 0}).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SMS).c(1).s("message\nbody").bytes());
        assertInvalid(new byte[] {(byte) Protocol.C_SMS, 1, 'm'});
        assertThrows(IllegalArgumentException.class, () -> Protocol.smsResult(-1));
        assertThrows(IllegalArgumentException.class, () -> Protocol.smsResult(0x100));
    }

    private static Protocol.SmsRequest parse(byte[] payload) {
        return Protocol.smsRequest(payload);
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class, () -> parse(payload));
    }
}
