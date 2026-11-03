package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

final class ProtocolLitoFindUserTest {
    private static byte[] request(int reserved1, int reserved2, String query) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Protocol.C_LITO_FIND_USER);
        out.write(reserved1);
        out.write(reserved2);
        out.writeBytes(query.getBytes(Protocol.LEGACY_TEXT));
        out.write(0);
        return out.toByteArray();
    }

    @Test void parsesTheExactZeroReservedNativeLayout() {
        assertEquals(new Protocol.LitoFindUserRequest("TestKnight"),
                Protocol.litoFindUserRequest(request(0, 0, "TestKnight")));
        assertEquals(new Protocol.LitoFindUserRequest(""),
                Protocol.litoFindUserRequest(request(0, 0, "")));
        assertEquals(31, Protocol.litoFindUserRequest(
                request(0, 0, "A".repeat(31))).query().length());
    }

    @Test void rejectsNonNativeReservedValuesAndThe32ByteOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoFindUserRequest(request(1, 0, "TestKnight")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoFindUserRequest(request(0, 1, "TestKnight")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoFindUserRequest(request(0, 0, "A".repeat(32))));
    }

    @Test void rejectsWrongOpcodeUnterminatedAndTrailingLayouts() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoFindUserRequest(new byte[] {(byte) 0xDD, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoFindUserRequest(new byte[] {(byte) 0xDC, 0, 0, 'A'}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoFindUserRequest(new byte[] {(byte) 0xDC, 0, 0, 0, 0}));
    }
}
