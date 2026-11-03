package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class ProtocolCookingFireTest {
    @Test void writesExactEmptyNameNpcShapedCookingFire() {
        BattleCookingFires.Fire fire = new BattleCookingFires.Fire(
                0x8000_0001L, 0x1234, 0x5678, 1);
        assertArrayEquals(new byte[] {
                0x0B,
                0x34, 0x12, 0x78, 0x56,
                0x01, 0x00, 0x00, (byte) 0x80,
                0x37, 0x17,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00,
                0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00,
                0x00, (byte) 0xFF, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        }, Protocol.cookingFire(fire));
    }
}
