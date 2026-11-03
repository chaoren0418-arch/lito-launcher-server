package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolJoinResultTest {
    @Test void rejectedJoinUsesTheNativeCleanupOnlyResult() {
        assertArrayEquals(new byte[] {(byte) 0xCE, 0x01}, Protocol.joinRejected());
    }

    @Test void rejectedJoinSerializesOnlyRecoveredProductionFailures() {
        for (Protocol.LitoJoinFailure failure : Protocol.LitoJoinFailure.values()) {
            assertArrayEquals(new byte[] {(byte) 0xCE, (byte) failure.wireValue()},
                    Protocol.joinRejected(failure));
        }
        assertEquals(List.of(1, 3, 4, 5, 7, 10, 11, 12, 13, 14, 20, 21),
                Arrays.stream(Protocol.LitoJoinFailure.values())
                        .map(Protocol.LitoJoinFailure::wireValue).toList());
    }

    @Test void eventRestrictionsUseTheRecoveredNativeResultCodes() {
        assertArrayEquals(new byte[] {(byte) 0xCE, 20},
                Protocol.joinRejected(Protocol.LitoJoinFailure.CLASS_RESTRICTED));
        assertArrayEquals(new byte[] {(byte) 0xCE, 21},
                Protocol.joinRejected(Protocol.LitoJoinFailure.SERVER_RESTRICTED));
    }

    @Test void roomLifecycleUsesRecoveredKickAndEndedResultCodes() {
        assertArrayEquals(new byte[] {(byte) 0xCE, 7},
                Protocol.joinRejected(Protocol.LitoJoinFailure.KICKED));
        assertArrayEquals(new byte[] {(byte) 0xCE, 12},
                Protocol.joinRejected(Protocol.LitoJoinFailure.BATTLE_ENDED));
    }

    @Test void eventTeamRoomRestrictionUsesRecoveredNativeResultCode() {
        assertArrayEquals(new byte[] {(byte) 0xCE, 13},
                Protocol.joinRejected(Protocol.LitoJoinFailure.TEAM_ROOM_ONLY));
    }
}
