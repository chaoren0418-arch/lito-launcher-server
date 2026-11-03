package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolClanWarTest {
    @Test void parsesEveryRecoveredNativeCWarProducerType() {
        for (Protocol.ClanWarRequestType type : Protocol.ClanWarRequestType.values()) {
            Protocol.ClanWarRequest request = Protocol.clanWarRequest(new byte[] {
                    0x31, (byte) type.wireValue(),
                    'R', 'a', 'i', 'd', 'e', 'r', 's', 0
            });
            assertEquals(type, request.type());
            assertEquals("Raiders", request.targetClan());
        }
        Protocol.ClanWarRequest unowned = Protocol.clanWarRequest(new byte[] {
                0x31, 0, ' ', 0
        });
        assertEquals(Protocol.ClanWarRequestType.DECLARE, unowned.type());
        assertEquals(" ", unowned.targetClan());
    }

    @Test void rejectsUnknownMalformedAndNonCanonicalCWarFrames() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.clanWarRequest(new byte[] {0x31, 1, 'A', 'B', 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.clanWarRequest(new byte[] {0x31, 0, 'A', 'B'}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.clanWarRequest(new byte[] {0x31, 0, 'A', 'B', 0, 1}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.clanWarRequest(new byte[] {0x31, 0, 'A', 'B', ' ', 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.clanWarRequest(new byte[] {0x31, 0, ' ', ' ', 0}));
    }

    @Test void writesExactNativeClanWarQuestions() {
        assertArrayEquals(new byte[] {
                0x24, (byte) 0xD9, 0x00,
                'R', 'a', 'i', 'd', 'e', 'r', 's', 0,
                'C', 'r', 'o', 'w', 'n', 0
        }, Protocol.clanWarAsk(
                Protocol.ClanWarRequestType.DECLARE, "Raiders", "Crown"));
        assertArrayEquals(new byte[] {
                0x24, (byte) 0xDD, 0x00,
                'R', 'a', 'i', 'd', 'e', 'r', 's', 0
        }, Protocol.clanWarAsk(
                Protocol.ClanWarRequestType.SURRENDER, "Raiders", ""));
        assertArrayEquals(new byte[] {
                0x24, (byte) 0xDE, 0x00,
                'R', 'a', 'i', 'd', 'e', 'r', 's', 0
        }, Protocol.clanWarAsk(
                Protocol.ClanWarRequestType.CEASE, "Raiders", ""));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.clanWarAsk(
                        Protocol.ClanWarRequestType.ALLIANCE, "Raiders", "Crown"));
        assertThrows(NullPointerException.class,
                () -> Protocol.clanWarAsk(null, "Raiders", "Crown"));
    }

    @Test void writesEveryNativeSWarReceiverTypeAsCcss() {
        for (Protocol.ClanWarNoticeType type : Protocol.ClanWarNoticeType.values()) {
            assertArrayEquals(new byte[] {
                    0x36, (byte) type.wireValue(),
                    'R', 'a', 'i', 'd', 'e', 'r', 's', 0,
                    'L', 'e', 'g', 'a', 'c', 'y', 0
            }, Protocol.clanWar(type, "Raiders", "Legacy"));
        }
        assertThrows(NullPointerException.class,
                () -> Protocol.clanWar(null, "Raiders", "Legacy"));
    }
}
