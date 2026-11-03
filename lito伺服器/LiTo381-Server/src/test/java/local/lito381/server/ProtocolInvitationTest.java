package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ProtocolInvitationTest {
    @Test
    void invitationMatchesNativeSdsContract() {
        byte[] packet = Protocol.roomInvitation("Inviter", 0x10203L, "1234");
        Protocol.Reader reader = new Protocol.Reader(packet);

        assertEquals(Protocol.S_LITO_INVITE_USER, reader.c());
        assertEquals("Inviter", reader.s());
        assertEquals(0x10203L, reader.d());
        assertEquals("1234", reader.s());
        assertEquals(0, reader.remaining());
    }

    @Test
    void invitationReplyMatchesNativeScContract() {
        byte[] packet = Protocol.roomInvitationReply(
                "Invitee", Protocol.LitoInvitationReply.ACCEPTED);
        Protocol.Reader reader = new Protocol.Reader(packet);

        assertEquals(Protocol.S_LITO_INVITE_REPLY, reader.c());
        assertEquals("Invitee", reader.s());
        assertEquals(1, reader.c());
        assertEquals(0, reader.remaining());
    }
}
