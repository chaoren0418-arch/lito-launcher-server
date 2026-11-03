package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolKickTest {
    @Test void writesExactNativeGenericKick() {
        assertArrayEquals(new byte[] {(byte) 0x66, 0}, Protocol.kick(0));
    }

    @Test void preservesFullNativeReasonByte() {
        assertArrayEquals(new byte[] {(byte) 0x66, (byte) 0xFF}, Protocol.kick(0xFF));
    }

    @Test void rejectsValuesOutsideNativeReasonByte() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.kick(-1));
        assertThrows(IllegalArgumentException.class, () -> Protocol.kick(0x100));
    }
}
