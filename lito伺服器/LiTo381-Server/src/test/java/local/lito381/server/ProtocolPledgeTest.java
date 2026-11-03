package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProtocolPledgeTest {
    @Test void parsesExactNativeCreateAndJoinPledgeRequests() {
        assertEquals("Raiders", Protocol.createPledgeRequest(new byte[] {
                0x21, 'R', 'a', 'i', 'd', 'e', 'r', 's', 0
        }));
        Protocol.requireJoinPledge(new byte[] {0x22});
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.createPledgeRequest(new byte[] {0x21, 'R', 0, 1}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireJoinPledge(new byte[] {0x22, 0}));
    }

    @Test void appliesRecoveredTaiwanClanNameValidator() {
        assertTrue(Protocol.validClanName("AB"));
        assertTrue(Protocol.validClanName("Raiders123"));
        assertFalse(Protocol.validClanName("A"));
        assertFalse(Protocol.validClanName("A".repeat(17)));
        assertFalse(Protocol.validClanName("bad\nname"));
    }

    @Test void writesExactQuestion97ClanApplicationPrompt() {
        assertArrayEquals(new byte[] {
                0x24, 0x61, 0x00,
                'T', 'e', 's', 't', 'M', 'a', 'g', 'e', 0
        }, Protocol.clanJoinAsk("TestMage"));
    }

    @Test void acceptsOnlyExactOpcodeOnlyWhoPledgeRequest() {
        Protocol.requireWhoPledge(new byte[] {0x48});
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireWhoPledge(new byte[] {0x48, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireWhoPledge(new byte[] {0x47}));
    }

    @Test void writesExactNativePledgeMetadata() {
        assertArrayEquals(new byte[] {
                0x69, 0x04, 0x03, 0x02, 0x01,
                'R', 'a', 'i', 'd', 'e', 'r', 's', 0,
                'C', 'h', 'a', 'm', 'p', 'i', 'o', 'n', 0,
                0x44, 0x33, 0x22, 0x11
        }, Protocol.pledgeMetadata(0x0102_0304L, "Raiders", "Champion", 0x1122_3344L));
    }

    @Test void rejectsInvalidNativePledgeMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.pledgeMetadata(0, "Raiders", "", 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.pledgeMetadata(1, "Raiders", "", 0x1_0000_0000L));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.pledgeMetadata(1, "x".repeat(256), "", 0));
    }
}
