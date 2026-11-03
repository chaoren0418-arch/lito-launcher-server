package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProtocolWantedTest {
    @Test
    void parsesAllThreeExactNativeWantedCommandEnvelopes() {
        Protocol.WantedRequest list = parse(new Protocol.Writer()
                .c(Protocol.C_WANTED).c(0).s("").bytes());
        assertEquals(0, list.mode());
        assertEquals("", list.operand());

        Protocol.WantedRequest add = parse(new Protocol.Writer()
                .c(Protocol.C_WANTED).c(1).s("TestMage").bytes());
        assertEquals(1, add.mode());
        assertEquals("TestMage", add.operand());

        Protocol.WantedRequest remove = parse(new Protocol.Writer()
                .c(Protocol.C_WANTED).c(2).s("測試者").bytes());
        assertEquals(2, remove.mode());
        assertEquals("測試者", remove.operand());

        assertArrayEquals(new Protocol.Writer().c(Protocol.S_WANTED_LOGIN)
                        .s("Account").s("127.0.0.1").bytes(),
                Protocol.wantedLogin("Account", "127.0.0.1"));
    }

    @Test
    void rejectsShapesAndStringsNoExactProducerOrReceiverCanUse() {
        assertInvalid(new Protocol.Writer().c(Protocol.C_MOVE).c(0).s("").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(0).s("Target").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(1).s("").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(2).s("").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(3).s("Target").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(1)
                .s("X".repeat(0x80)).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(1).s(" Target").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(2).s("Target ").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(1)
                .s("Tar\nget").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_WANTED).c(1)
                .s("Target").c(1).bytes());
        assertInvalid(new byte[] {(byte) Protocol.C_WANTED, 1, 'T'});

        assertThrows(IllegalArgumentException.class, () -> Protocol.wantedLogin("", "source"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.wantedLogin("A".repeat(0x40), "source"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.wantedLogin("Account", "S".repeat(0x40)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.wantedLogin("Account", "bad\nsource"));
    }

    private static Protocol.WantedRequest parse(byte[] payload) {
        return Protocol.wantedRequest(payload);
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class, () -> parse(payload));
    }
}
