package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolDoorWishTest {
    @Test void parsesExactNativeOpenAndWishEnvelopes() {
        Protocol.OpenRequest open = Protocol.openRequest(new Protocol.Writer()
                .c(Protocol.C_OPEN).h(0x8123).h(0x8045).d(0x6200_0001L).bytes());
        assertEquals(0x8123, open.x());
        assertEquals(0x8045, open.y());
        assertEquals(0x6200_0001L, open.worldObjectId());
        Protocol.OpenRequest capturedNativeDoor = Protocol.openRequest(new Protocol.Writer()
                .c(Protocol.C_OPEN).h(32735).h(32799).d(0x6200_0000L).bytes());
        assertEquals(32735, capturedNativeDoor.x());
        assertEquals(32799, capturedNativeDoor.y());
        assertEquals(0x6200_0000L, capturedNativeDoor.worldObjectId());

        assertEquals("魔法燈籠", Protocol.wishRequest(new Protocol.Writer()
                .c(Protocol.C_WISH).s("魔法燈籠").bytes()));
    }

    @Test void writesExactDoorObjectAndActionPackets() {
        assertArrayEquals(new byte[] {
                0x0B,
                0x34, 0x12, 0x78, 0x56,
                0x04, 0x03, 0x02, 0x01,
                0x23, 0x08,
                0x1D, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00,
                0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00,
                0x00, (byte) 0xFF, 0x00, 0x00,
                0x00, (byte) 0xFF, (byte) 0xFF, 0x00
        }, Protocol.battleDoor(0x0102_0304L, 0x1234, 0x5678,
                BattleDoor.ACTION_CLOSED));
        assertArrayEquals(new byte[] {
                0x20, 0x04, 0x03, 0x02, 0x01, 0x1C
        }, Protocol.objectAction(0x0102_0304L, BattleDoor.ACTION_OPEN));
        assertArrayEquals(new byte[] {
                0x22, 0x34, 0x12, 0x78, 0x56, 0x00, 0x41
        }, Protocol.doorAttribute(0x1234, 0x5678,
                BattleDoor.DIRECTION_DIAGONAL, BattleDoor.NOT_PASSABLE));
        assertArrayEquals(new byte[] {
                0x22, 0x34, 0x12, 0x78, 0x56, 0x00, 0x00
        }, Protocol.doorAttribute(0x1234, 0x5678,
                BattleDoor.DIRECTION_DIAGONAL, BattleDoor.PASSABLE));
    }

    @Test void rejectsMalformedOpenWishAndDoorPackets() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.openRequest(
                new byte[] {0x0E, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.openRequest(
                new Protocol.Writer().c(Protocol.C_OPEN).h(1).h(2).d(3).c(4).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.wishRequest(
                new byte[] {0x1E, 0x00}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.wishRequest(
                new Protocol.Writer().c(Protocol.C_WISH).s(" bad").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.wishRequest(
                new Protocol.Writer().c(Protocol.C_WISH).s("bad\nvalue").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.wishRequest(
                new byte[] {0x1E, (byte) 0x81, 0x00}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.battleDoor(
                1, 1, 1, 27));
        assertThrows(IllegalArgumentException.class, () -> Protocol.doorAttribute(
                1, 2, 2, BattleDoor.PASSABLE));
        assertThrows(IllegalArgumentException.class, () -> Protocol.doorAttribute(
                1, 2, BattleDoor.DIRECTION_DIAGONAL, 1));
    }
}
