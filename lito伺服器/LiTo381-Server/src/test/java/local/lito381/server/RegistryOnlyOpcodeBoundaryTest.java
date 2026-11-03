package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RegistryOnlyOpcodeBoundaryTest {
    private static final int[] REGISTRY_ONLY_DEFAULT_IGNORED = {
            0x04, 0x12, 0x15, 0x16, 0x19, 0x2F, 0x32, 0x3D, 0x40, 0x5B,
            0x67, 0x69, 0x6B, 0x74, 0x78, 0x79, 0x7B, 0x7D, 0xF2, 0xF3
    };
    private static final int ORPHAN_UNREACHABLE = 0xF4;

    @Test void ignoresTwentyRegistryOnlyAndOneUnreachableOpcodeWithoutMutationOrResponse()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession session = new ClientSession(null, ClientSession.Endpoint.GAME,
                    config, null, null, null, hub);
            Method handle = ClientSession.class.getDeclaredMethod("handle", byte[].class);
            handle.setAccessible(true);

            for (int opcode : REGISTRY_ONLY_DEFAULT_IGNORED) {
                handle.invoke(session, (Object) new byte[] {(byte) opcode});
                assertPristine(session, String.format("0x%02X", opcode));
            }
            handle.invoke(session, (Object) new byte[] {(byte) ORPHAN_UNREACHABLE});
            assertPristine(session, "0xF4");
        }
    }

    private static void assertPristine(ClientSession session, String opcode) throws Exception {
        assertEquals("KEY_SENT", field(session, "phase").toString(), opcode);
        assertEquals("", field(session, "selectedCharacter"), opcode);
        assertEquals(0L, field(session, "currentRoomId"), opcode);
        assertEquals(false, field(session, "quitRequested"), opcode);
    }

    private static Object field(ClientSession session, String name) throws Exception {
        Field field = ClientSession.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(session);
    }
}
