package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolLetterTest {
    @Test
    void parsesExactCdhsWideWideComposeEnvelope() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(0x1122_3344L).h(0x1234).s("TestMage")
                .S("Hi").S("Line\r\nTwo").bytes();
        assertEquals(new Protocol.LetterComposeInventoryUse(
                        0x1122_3344L, 0x1234, "TestMage", "Hi", "Line\r\nTwo"),
                Protocol.letterComposeInventoryUse(packet));
        assertArrayEquals(hex(
                "1C 44 33 22 11 34 12 54 65 73 74 4D 61 67 65 00 "
                        + "48 00 69 00 00 00 4C 00 69 00 6E 00 65 00 0D 00 0A 00 "
                        + "54 00 77 00 6F 00 00 00"), packet);
    }

    @Test
    void writesExactDhhSsWideWideCsReadMailEnvelope() {
        byte[] packet = Protocol.readMail(
                0x1122_3344L, 466, 0x1234,
                "A", "B", "Hi", "X", 0, "26/08/11");
        assertArrayEquals(hex(
                "5E 44 33 22 11 D2 01 34 12 41 00 42 00 "
                        + "48 00 69 00 00 00 58 00 00 00 00 "
                        + "32 36 2F 30 38 2F 31 31 00"), packet);

        Protocol.Reader in = new Protocol.Reader(packet);
        assertEquals(Protocol.S_READ_MAIL, in.c());
        assertEquals(0x1122_3344L, in.d());
        assertEquals(466, in.h());
        assertEquals(0x1234, in.h());
        assertEquals("A", in.s());
        assertEquals("B", in.s());
        assertEquals("Hi", in.S());
        assertEquals("X", in.S());
        assertEquals(0, in.c());
        assertEquals("26/08/11", in.s());
        assertEquals(0, in.remaining());
    }

    @Test
    void enforcesRecoveredReceiverCapacitiesAndWideStringIntegrity() {
        Protocol.letterComposeInventoryUse(new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(1).h(0).s("R").S("S".repeat(20)).S("C".repeat(256)).bytes());
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.letterComposeInventoryUse(new Protocol.Writer().c(Protocol.C_USE_ITEM)
                        .d(1).h(0).s("R").S("S".repeat(21)).S("").bytes()));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.letterComposeInventoryUse(new Protocol.Writer().c(Protocol.C_USE_ITEM)
                        .d(1).h(0).s("A;B").S("").S("").bytes()));
        assertThrows(IllegalArgumentException.class, () ->
                new Protocol.Writer().S("\uD800"));
        assertThrows(IllegalArgumentException.class, () ->
                new Protocol.Reader(hex("41 00 42")).S());
    }

    private static byte[] hex(String value) {
        String clean = value.replace(" ", "");
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
