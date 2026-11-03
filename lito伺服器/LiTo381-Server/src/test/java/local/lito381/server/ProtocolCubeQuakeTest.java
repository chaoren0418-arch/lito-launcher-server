package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class ProtocolCubeQuakeTest {
    @Test void cubeUsesExactNpcShapedPutObjectWithGfx6712() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        BattleCubeQuakeEffects.Cube cube = effects.place(15, 32768, 32800, 0).cube();
        byte[] expected = new Protocol.Writer().c(Protocol.S_PUT_OBJECT)
                .h(32768).h(32800).d(cube.objectId())
                .h(BattleCubeQuakeEffects.CUBE_GFX)
                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)
                .d(0).h(0).s("").s("")
                .c(0).d(0).s("").s("")
                .c(0).c(0xFF).c(0).c(0)
                .s("").h(0).c(0).bytes();
        assertArrayEquals(expected, Protocol.cubeQuake(cube));
    }

    @Test void bindUsesRetainedTypeBindModesTwentyFourAndTwentyFive() {
        assertArrayEquals(new byte[] {0x25, 0x18}, Protocol.paralysis(
                Protocol.ParalysisMode.BIND_APPLY));
        assertArrayEquals(new byte[] {0x25, 0x19}, Protocol.paralysis(
                Protocol.ParalysisMode.BIND_RELEASE));
    }
}
