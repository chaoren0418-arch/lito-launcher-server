package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolNpcItemControlTest {
    @Test
    void parsesTheExactCcdcPetInventoryProducer() {
        Protocol.NpcItemControlRequest request = Protocol.npcItemControlRequest(
                new Protocol.Writer().c(0x7F).c(0).d(0x6000_0042L).c(17).bytes());
        assertEquals(0, request.selector());
        assertEquals(0x6000_0042L, request.npcObjectId());
        assertEquals(17, request.listIndex());
    }

    @Test
    void rejectsUnprovedSelectorZeroNpcAndNonExactLengths() {
        assertInvalid(new Protocol.Writer().c(0x7F).c(1).d(1).c(0).bytes());
        assertInvalid(new Protocol.Writer().c(0x7F).c(0).d(0).c(0).bytes());
        assertInvalid(new Protocol.Writer().c(0x7E).c(0).d(1).c(0).bytes());
        assertInvalid(new Protocol.Writer().c(0x7F).c(0).d(1).bytes());
        assertInvalid(new Protocol.Writer().c(0x7F).c(0).d(1).c(0).c(0).bytes());
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.npcItemControlRequest(payload));
    }
}
