package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProtocolSilenceTest {
    @Test
    void parsesExactNativeNamedSilenceEnvelopeAndPreservesUnsignedWord() {
        Protocol.SilenceRequest standard = parse(new Protocol.Writer()
                .c(Protocol.C_SILENCE).h(3600).s("TestMage").bytes());
        assertEquals(3600, standard.durationSeconds());
        assertEquals("TestMage", standard.target());

        assertEquals(0, parse(new Protocol.Writer().c(Protocol.C_SILENCE)
                .h(0).s("測試者").bytes()).durationSeconds());
        assertEquals(0x7D00, parse(new Protocol.Writer().c(Protocol.C_SILENCE)
                .h(0x7D00).s("Cap").bytes()).durationSeconds());
        assertEquals(0xFFFF, parse(new Protocol.Writer().c(Protocol.C_SILENCE)
                .h(0xFFFF).s("Wrapped").bytes()).durationSeconds());
    }

    @Test
    void rejectsLayoutsNoExactNativeProducerCanEmit() {
        assertInvalid(new Protocol.Writer().c(Protocol.C_MOVE).h(60).s("Target").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SILENCE).h(60).s("").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SILENCE).h(60)
                .s("X".repeat(0x80)).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SILENCE).h(60).s(" Target").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SILENCE).h(60).s("Target ").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SILENCE).h(60)
                .s("Tar\nget").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SILENCE).h(60)
                .s("Target").c(1).bytes());
        assertInvalid(new byte[] {(byte) Protocol.C_SILENCE, 60, 0, 'T'});
    }

    private static Protocol.SilenceRequest parse(byte[] payload) {
        return Protocol.silenceRequest(payload);
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class, () -> parse(payload));
    }
}
