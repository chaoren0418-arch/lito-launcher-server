package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NativeMagicDollTest {
    private static final SourceMagicDollType TYPE = new SourceMagicDollType(
            55_000, 0, 0, List.of(102), List.of(40_308), List.of(1_000),
            1_800, 6_100, "魔法娃娃：肥肥");

    @Test
    void exactRetainedActorAndLegacyIconPackets() {
        NativeMagicDoll doll = new NativeMagicDoll(
                0x7A10_0001L, 0x7000_0001L, TYPE,
                33051, 32338, 4, 9, 5, 2_000_000_000L);
        byte[] expected = new Protocol.Writer().c(Protocol.S_PUT_OBJECT)
                .h(33051).h(32338).d(0x7A10_0001L).h(6100)
                .c(0).c(5).c(0).c(0).d(0).h(0)
                .s("魔法娃娃：肥肥").s("")
                .c(0).d(0).s("").s("測試玩家")
                .c(0).c(0xFF).c(0).c(0).c(0).c(0xFF).c(0xFF).bytes();
        assertArrayEquals(expected, Protocol.magicDoll(doll, "測試玩家"));
        assertArrayEquals(new byte[] {(byte) 250, 56, 44, 1},
                Protocol.magicDollIcon(300));
    }

    @Test
    void customIconAndSecondCountdownMatchRetainedTimer() {
        NativeMagicDoll doll = new NativeMagicDoll(
                0x7A10_0002L, 0x7000_0002L, TYPE,
                1, 2, 4, 0, 5, 2_000_000_000L);
        assertEquals(2, doll.remainingSeconds(1));
        assertEquals(1, doll.remainingSeconds(1_000_000_001L));
        assertEquals(0, doll.remainingSeconds(2_000_000_000L));
        assertArrayEquals(new Protocol.Writer().c(250).c(154)
                        .h(300).h(2184).h(0).h(0).bytes(),
                Protocol.magicDollCustomIcon(2184, 300, false));
        assertArrayEquals(new Protocol.Writer().c(250).c(154)
                        .h(0).h(2184).h(0).h(1).bytes(),
                Protocol.magicDollCustomIcon(2184, 0, true));
    }

    @Test
    void rejectsMismatchedMaterialAndInvalidNativeFields() {
        assertThrows(IllegalArgumentException.class, () -> new SourceMagicDollType(
                55_000, 0, 0, List.of(102), List.of(40_308), List.of(),
                1_800, 6_100, "肥肥"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.magicDollIcon(65_536));
    }
}
