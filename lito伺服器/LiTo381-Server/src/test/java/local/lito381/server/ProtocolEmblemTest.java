package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolEmblemTest {
    @Test
    void parsesExactNativeEmblemIdRequest() {
        assertEquals(0x1122_3344L, Protocol.emblemRequest(
                new Protocol.Writer().c(Protocol.C_EMBLEM).d(0x1122_3344L).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.emblemRequest(
                new Protocol.Writer().c(Protocol.C_EMBLEM).d(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.emblemRequest(
                new Protocol.Writer().c(Protocol.C_EMBLEM).d(1).c(0).bytes()));
    }

    @Test
    void writesExactNativeIdAnd384RawBytes() {
        byte[] pixels = pixels();
        byte[] expected = new Protocol.Writer().c(Protocol.S_EMBLEM)
                .d(0x1122_3344L).raw(pixels).bytes();
        byte[] packet = Protocol.emblem(0x1122_3344L, pixels);

        assertEquals(1 + 4 + Protocol.EMBLEM_BYTES, packet.length);
        assertArrayEquals(expected, packet);
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.emblem(1, new byte[Protocol.EMBLEM_BYTES - 1]));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.emblem(0, pixels));
    }

    @Test
    void actorPublishesClanIdentityThatTriggersNativeRequest() {
        Protocol.Reader actor = new Protocol.Reader(Protocol.battleActor(
                1, 32730, 32798, 61, 4, 0, BattleTimedStatus.NORMAL,
                "TestEmblem", "", 4, 0x1020_3040L,
                "LegacyEmblem", 100, 52));
        actor.c(); actor.h(); actor.h(); actor.d(); actor.h();
        actor.c(); actor.c(); actor.c(); actor.c(); actor.d(); actor.h();
        assertEquals("TestEmblem", actor.s());
        assertEquals("", actor.s());
        assertEquals(4, actor.c());
        assertEquals(0x1020_3040L, actor.d());
        assertEquals("LegacyEmblem", actor.s());
    }

    private static byte[] pixels() {
        byte[] pixels = new byte[Protocol.EMBLEM_BYTES];
        for (int index = 0; index < pixels.length; index++) {
            pixels[index] = (byte) index;
        }
        return pixels;
    }
}
