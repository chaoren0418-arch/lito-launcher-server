package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolPartyTest {
    @Test
    void parsesExactOpcodeOnlyPartyCommands() {
        Protocol.requireClassicPartyOpcodeOnly(new byte[] {0x5D}, Protocol.C_INVITE_PARTY);
        Protocol.requireClassicPartyOpcodeOnly(new byte[] {0x5F}, Protocol.C_LEAVE_PARTY);
        Protocol.requireClassicPartyOpcodeOnly(new byte[] {0x60}, Protocol.C_WHO_PARTY);
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.requireClassicPartyOpcodeOnly(
                        new byte[] {0x5D, 0}, Protocol.C_INVITE_PARTY));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.requireClassicPartyOpcodeOnly(
                        new byte[] {0x5E}, Protocol.C_BANISH_PARTY));
    }

    @Test
    void parsesExactBanishPartyCsEnvelope() {
        assertEquals("TestMage", Protocol.partyBanishRequest(
                new Protocol.Writer().c(Protocol.C_BANISH_PARTY).s("TestMage").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.partyBanishRequest(
                new Protocol.Writer().c(Protocol.C_BANISH_PARTY).s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.partyBanishRequest(
                new Protocol.Writer().c(Protocol.C_BANISH_PARTY)
                        .s("TestMage").c(0).bytes()));
    }

    @Test
    void writesQuestion953PartyInvitation() {
        assertArrayEquals(new byte[] {
                0x24, (byte) 0xB9, 0x03,
                'L', 'e', 'a', 'd', 'e', 'r', 0
        }, Protocol.partyInviteAsk("Leader"));
    }

    @Test
    void writesNativePartyHypertextSubstitutions() {
        assertArrayEquals(new byte[] {
                0x2A, 0x44, 0x33, 0x22, 0x11,
                'p', 'a', 'r', 't', 'y', 0,
                0, 0x02, 0,
                'L', 'e', 'a', 'd', 'e', 'r', 0,
                'L', 'e', 'a', 'd', 'e', 'r', ' ',
                'M', 'a', 'g', 'e', ' ', 0
        }, Protocol.partyList(0x1122_3344L, "Leader", List.of("Leader", "Mage")));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.partyList(1, "Leader", List.of("Mage", "Other")));
    }
}
