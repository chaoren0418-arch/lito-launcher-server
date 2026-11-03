package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

final class ProtocolLitoBuilderAdminTest {
    private static byte[] packet(int... values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int value : values) out.write(value);
        return out.toByteArray();
    }

    @Test void parsesEveryExactNativeOperationLayout() {
        assertEquals(new Protocol.LitoBuilderAdminRequest(0,
                        Protocol.LitoBuilderAdminOperation.FORCE_WIN, 1, -1, -1, ""),
                Protocol.litoBuilderAdminRequest(packet(0xDF, 0, 0, 1, 0)));
        assertEquals(new Protocol.LitoBuilderAdminRequest(1,
                        Protocol.LitoBuilderAdminOperation.PREPARE,
                        0, 0xFFFF, 0x1234, ""),
                Protocol.litoBuilderAdminRequest(packet(
                        0xDF, 1, 0, 0, 0, 0xFF, 0xFF, 0x34, 0x12)));
        assertEquals(new Protocol.LitoBuilderAdminRequest(2,
                        Protocol.LitoBuilderAdminOperation.LOAD,
                        1, 2, 3, ""),
                Protocol.litoBuilderAdminRequest(packet(
                        0xDF, 2, 0, 1, 0, 2, 0, 3, 0)));
        assertEquals(new Protocol.LitoBuilderAdminRequest(3,
                        Protocol.LitoBuilderAdminOperation.ITEM,
                        0xFFFF, -1, -1, ""),
                Protocol.litoBuilderAdminRequest(packet(0xDF, 3, 0, 0xFF, 0xFF)));
        assertEquals(new Protocol.LitoBuilderAdminRequest(4,
                        Protocol.LitoBuilderAdminOperation.KICK,
                        -1, -1, -1, "TestMage"),
                Protocol.litoBuilderAdminRequest(packet(
                        0xDF, 4, 0, 'T', 'e', 's', 't', 'M', 'a', 'g', 'e', 0)));
        assertEquals(Protocol.LitoBuilderAdminOperation.ARENA_WHO,
                Protocol.litoBuilderAdminRequest(packet(0xDF, 5, 0)).operation());
        assertEquals(Protocol.LitoBuilderAdminOperation.TELEPORT,
                Protocol.litoBuilderAdminRequest(packet(0xDF, 10, 0)).operation());
    }

    @Test void rejectsUnknownUnderflowTrailingAndNonNativeForceValues() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuilderAdminRequest(packet(0xDE, 5, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuilderAdminRequest(packet(0xDF, 1, 0, 1, 0, 2, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuilderAdminRequest(packet(0xDF, 5, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuilderAdminRequest(packet(0xDF, 6, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuilderAdminRequest(packet(0xDF, 0, 0, 2, 0)));
    }

    @Test void kickPreservesTheNative127ByteCommandBufferBoundary() {
        byte[] accepted = new byte[3 + 127 + 1];
        accepted[0] = (byte) 0xDF;
        accepted[1] = 4;
        for (int index = 3; index < accepted.length - 1; index++) accepted[index] = 'A';
        assertEquals(127, Protocol.litoBuilderAdminRequest(accepted)
                .targetName().getBytes(Protocol.LEGACY_TEXT).length);

        byte[] rejected = new byte[3 + 128 + 1];
        rejected[0] = (byte) 0xDF;
        rejected[1] = 4;
        for (int index = 3; index < rejected.length - 1; index++) rejected[index] = 'A';
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBuilderAdminRequest(rejected));
    }
}
