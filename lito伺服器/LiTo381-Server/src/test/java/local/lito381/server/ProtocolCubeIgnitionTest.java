package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class ProtocolCubeIgnitionTest {
    @Test void exactNpcShapedPutObjectUses80149TemplateGfx6706() {
        BattleCubeIgnitionEffects effects = new BattleCubeIgnitionEffects();
        BattleCubeIgnitionEffects.Cube cube =
                effects.place(0x3810_2050L, 32749, 32813, 0).cube();
        byte[] expected = new Protocol.Writer().c(Protocol.S_PUT_OBJECT)
                .h(32749).h(32813).d(cube.objectId())
                .h(BattleCubeIgnitionEffects.CUBE_GFX)
                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)
                .d(0).h(0).s("").s("")
                .c(0).d(0).s("").s("")
                .c(0).c(0xFF).c(0).c(0)
                .s("").h(0).c(0).bytes();
        assertArrayEquals(expected, Protocol.cubeIgnition(cube));
    }
}
