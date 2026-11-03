package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolPkCountTest {
    @Test void acceptsOnlyTheNativeOpcodeOnlyCheckPkProducer() {
        Protocol.requireCheckPk(new byte[] {0x6C});
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireCheckPk(new byte[] {0x6C, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireCheckPk(new byte[] {0x6D}));
    }

    @Test void writesSameFamilyMessage562WithOneDecimalArgument() {
        assertArrayEquals(new byte[] {
                0x10, 0x32, 0x02, 0x01, '0', 0
        }, Protocol.messageCode(562, "0"));
    }
}
