package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolMarriageTest {
    @Test
    void parsesOnlyTheTwoRecoveredCcProducerModes() {
        assertEquals(0, Protocol.marriageRequest(new byte[] {0x75, 0}));
        assertEquals(1, Protocol.marriageRequest(new byte[] {0x75, 1}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.marriageRequest(new byte[] {0x75, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.marriageRequest(new byte[] {0x75, 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.marriageRequest(new byte[] {0x74, 0}));
    }

    @Test
    void emitsExactGenericAskAndBodylessMarriedEventShapes() {
        assertArrayEquals(new byte[] {0x24, (byte) 0x8E, 0x02,
                        'A', 'l', 'i', 'c', 'e', 0},
                Protocol.marriageProposalAsk("Alice"));
        assertArrayEquals(new byte[] {0x24, (byte) 0x8D, 0x02, 0},
                Protocol.marriageDivorceAsk());
        assertArrayEquals(new byte[] {0x7B, 9}, Protocol.marriedEvent());
    }
}
