package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolNoticeTest {
    @Test
    void implementsExactNativeNoticeWireFamily() {
        assertArrayEquals(bytes(0x4B, 0x11, 0x01), Protocol.notice(273));
        assertArrayEquals(bytes(0x4B, 0xB6, 0x01), Protocol.notice(438));
        assertArrayEquals(bytes(0x4B, 0xFF, 0x7F), Protocol.notice(0x7FFF));

        assertDoesNotThrow(() -> Protocol.readNotice(bytes(0x46)));
        assertDoesNotThrow(() -> Protocol.loginResult(bytes(0x61)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.readNotice(bytes(0x46, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginResult(bytes(0x61, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.readNotice(bytes(0x61)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.notice(-1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.notice(0x8000));
    }

    @Test
    void parsesOptionalNativeLoginNoticeQueue() throws Exception {
        ServerConfig ordinary = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        ServerConfig fixture = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.notice-selftest.properties"));
        assertEquals(List.of(), ordinary.loginNoticeMessageIds());
        assertEquals(List.of(273, 307, 317, 438), fixture.loginNoticeMessageIds());
    }

    @Test
    void implementsExactNativeCommonNewsString() {
        assertArrayEquals(bytes(0x38, 'L', 'i', 'T', 'o', 0),
                Protocol.commonNews("LiTo"));
        assertThrows(IllegalArgumentException.class, () -> Protocol.commonNews(""));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.commonNews("x".repeat(256)));
    }

    @Test
    void keepsCommonNewsOptIn() throws Exception {
        ServerConfig ordinary = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        ServerConfig fixture = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.news-selftest.properties"));
        assertEquals("", ordinary.loginCommonNews());
        assertEquals("LiTo 3.81C native common news", fixture.loginCommonNews());
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) result[index] = (byte) values[index];
        return result;
    }
}
