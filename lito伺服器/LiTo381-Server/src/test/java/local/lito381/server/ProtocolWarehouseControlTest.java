package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolWarehouseControlTest {
    @Test
    void parsesTheTwoExactCcddProducerShapes() {
        Protocol.WarehouseControlRequest change = Protocol.warehouseControlRequest(
                new Protocol.Writer().c(0x7E).c(0).d(123456).d(654321).bytes());
        assertEquals(0, change.mode());
        assertEquals(123456, change.first());
        assertEquals(654321, change.second());

        Protocol.WarehouseControlRequest personal = Protocol.warehouseControlRequest(
                new Protocol.Writer().c(0x7E).c(1).d(654321).d(0x6700_0001L).bytes());
        assertEquals(1, personal.mode());
        assertEquals(654321, personal.first());
        assertEquals(0x6700_0001L, personal.second());

        Protocol.WarehouseControlRequest pledge = Protocol.warehouseControlRequest(
                new Protocol.Writer().c(0x7E).c(2).d(0).d(0x6700_0002L).bytes());
        assertEquals(2, pledge.mode());
        assertEquals(0, pledge.first());
    }

    @Test
    void rejectsUnknownModesInvalidPasswordsAndNonExactLengths() {
        assertInvalid(new Protocol.Writer().c(0x7E).c(3).d(1).d(2).bytes());
        assertInvalid(new Protocol.Writer().c(0x7E).c(0).d(1_000_000).d(2).bytes());
        assertInvalid(new Protocol.Writer().c(0x7E).c(0).d(1).d(1_000_000).bytes());
        assertInvalid(new Protocol.Writer().c(0x7E).c(1).d(1).d(0).bytes());
        assertInvalid(new Protocol.Writer().c(0x7D).c(0).d(1).d(2).bytes());
        assertInvalid(new Protocol.Writer().c(0x7E).c(0).d(1).bytes());
        assertInvalid(new Protocol.Writer().c(0x7E).c(0).d(1).d(2).c(0).bytes());
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.warehouseControlRequest(payload));
    }
}
