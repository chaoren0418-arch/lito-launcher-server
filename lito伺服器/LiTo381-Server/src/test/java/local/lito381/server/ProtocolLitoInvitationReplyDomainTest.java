package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

final class ProtocolLitoInvitationReplyDomainTest {
    @Test void nativeReplyDomainIsExactlyNotAcceptedAndAccepted() {
        assertArrayEquals(new int[] {0, 1},
                Arrays.stream(Protocol.LitoInvitationReply.values())
                        .mapToInt(Protocol.LitoInvitationReply::wireValue).toArray());
        assertEquals(Protocol.LitoInvitationReply.NOT_ACCEPTED,
                Protocol.LitoInvitationReply.fromWire(0));
        assertEquals(Protocol.LitoInvitationReply.ACCEPTED,
                Protocol.LitoInvitationReply.fromWire(1));
        assertFalse(Protocol.LitoInvitationReply.NOT_ACCEPTED.accepted());
        assertTrue(Protocol.LitoInvitationReply.ACCEPTED.accepted());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoInvitationReply.fromWire(2));
    }

    @Test void cD6ParserReturnsTypedReplyAndRejectsNonProducerValues() {
        for (Protocol.LitoInvitationReply reply : Protocol.LitoInvitationReply.values()) {
            Protocol.LitoInviteReplyRequest request = Protocol.litoInviteReplyRequest(
                    new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                            .s("RoomOwner").c(reply.wireValue()).bytes());
            assertEquals("RoomOwner", request.inviterName());
            assertEquals(reply, request.reply());
        }
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoInviteReplyRequest(
                        new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                                .s("RoomOwner").c(2).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoInviteReplyRequest(
                        new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                                .s("RoomOwner").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoInviteReplyRequest(
                        new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                                .s("RoomOwner").c(1).c(0).bytes()));
    }

    @Test void sD8WriterEmitsEachTypedReplyExactly() {
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_INVITE_REPLY,
                        'I', 0, 0},
                Protocol.roomInvitationReply(
                        "I", Protocol.LitoInvitationReply.NOT_ACCEPTED));
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_INVITE_REPLY,
                        'I', 0, 1},
                Protocol.roomInvitationReply(
                        "I", Protocol.LitoInvitationReply.ACCEPTED));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.roomInvitationReply("I", null));
    }

    @Test void productionWriterExposesNoRawIntegerReplyOverload() {
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod(
                        "roomInvitationReply", String.class, int.class));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoInviteReplyRequest(
                        "RoomOwner", null));
    }
}
