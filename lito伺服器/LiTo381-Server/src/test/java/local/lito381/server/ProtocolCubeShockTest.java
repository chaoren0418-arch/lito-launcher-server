package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class ProtocolCubeShockTest {
    @Test void cubeUsesExactNpcShapedPutObjectWithGfx6718() {
        BattleCubeShockEffects effects = new BattleCubeShockEffects();
        BattleCubeShockEffects.Cube cube = effects.place(
                15, 32768, 32800, 0).cube();
        byte[] expected = new Protocol.Writer().c(Protocol.S_PUT_OBJECT)
                .h(32768).h(32800).d(cube.objectId())
                .h(BattleCubeShockEffects.CUBE_GFX)
                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)
                .d(0).h(0).s("").s("")
                .c(0).d(0).s("").s("")
                .c(0).c(0xFF).c(0).c(0)
                .s("").h(0).c(0).bytes();
        assertArrayEquals(expected, Protocol.cubeShock(cube));
    }
}
