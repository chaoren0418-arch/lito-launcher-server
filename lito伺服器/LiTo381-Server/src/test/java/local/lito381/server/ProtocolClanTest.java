package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolClanTest {
    @Test
    void emptyClanUsesNativeScHeaderWithoutSyntheticMembers() {
        assertArrayEquals(new byte[] {(byte) 0xF6, 0, 0},
                Protocol.clanInfo(GameHub.ClanData.empty()));
    }

    @Test
    void fullClanInfoMatchesNativeScAndShccscRecords() {
        GameHub.ClanData data = new GameHub.ClanData("Raiders", List.of(
                new GameHub.ClanMemberSnapshot("TestKnight", Protocol.LitoClanRole.ROYAL,
                        0, 0, "", Protocol.LitoClanActivity.WAITING),
                new GameHub.ClanMemberSnapshot("TestMage", Protocol.LitoClanRole.CANDIDATE,
                        4, 5, "Candidate", Protocol.LitoClanActivity.OFFLINE)), false);
        Protocol.Reader reader = new Protocol.Reader(Protocol.clanInfo(data));

        assertEquals(Protocol.S_LITO_CLAN_INFO, reader.c());
        assertEquals("Raiders", reader.s());
        assertEquals(2, reader.c());
        assertClanRecord(reader, "TestKnight", 1, 0, 0, "", 0);
        assertClanRecord(reader, "TestMage", 3, 4, 5, "Candidate", 2);
        assertEquals(0, reader.remaining());
    }

    @Test
    void incrementalAddAndDeleteMatchNativeContracts() {
        GameHub.ClanMemberSnapshot member =
                new GameHub.ClanMemberSnapshot("TestMage", Protocol.LitoClanRole.CANDIDATE,
                        0, 0, "", Protocol.LitoClanActivity.OFFLINE);
        Protocol.Reader added = new Protocol.Reader(Protocol.clanMemberAdded(member));
        assertEquals(Protocol.S_LITO_CLAN_ADD, added.c());
        assertClanRecord(added, "TestMage", 3, 0, 0, "", 2);
        assertEquals(0, added.remaining());

        Protocol.Reader deleted = new Protocol.Reader(Protocol.clanMemberDeleted("TestMage"));
        assertEquals(Protocol.S_LITO_CLAN_DELETE, deleted.c());
        assertEquals("TestMage", deleted.s());
        assertEquals(0, deleted.remaining());
    }

    @Test
    void mapsReachableLitoClanCreateFailuresToRetainedNativeMessages() {
        assertArrayEquals(new byte[] {0x10, 86, 0, 0},
                Protocol.litoClanCreateFailure(
                        GameHub.ClassicClanCreateStatus.OWNER_ALREADY_MEMBER));
        assertArrayEquals(new byte[] {0x10, 99, 0, 0},
                Protocol.litoClanCreateFailure(
                        GameHub.ClassicClanCreateStatus.DUPLICATE_NAME));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoClanCreateFailure(
                        GameHub.ClassicClanCreateStatus.CREATED));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoClanCreateFailure(
                        GameHub.ClassicClanCreateStatus.INVALID));
    }

    @Test
    void battleObjectClanMetadataMatchesNativeDdsdContract() {
        Protocol.Reader reader = new Protocol.Reader(
                Protocol.battleClanMetadata(1, 0, "Raiders", true));
        assertEquals(Protocol.S_LITO_CLAN, reader.c());
        assertEquals(1, reader.d());
        assertEquals(0, reader.d());
        assertEquals("Raiders", reader.s());
        assertEquals(1, reader.d());
        assertEquals(0, reader.remaining());
    }

    @Test
    void rankControlParsesNativeCloseAndRoleAssignmentLayouts() {
        Protocol.ClanRankControlRequest close = Protocol.clanRankControlRequest(
                new byte[] {(byte) Protocol.C_RANK_CONTROL, 0});
        assertEquals(0, close.command());
        assertEquals(0, close.role());
        assertEquals("", close.characterName());

        Protocol.ClanRankControlRequest assignment = Protocol.clanRankControlRequest(
                new Protocol.Writer().c(Protocol.C_RANK_CONTROL).c(1).c(3)
                        .s("TestMage").bytes());
        assertEquals(1, assignment.command());
        assertEquals(3, assignment.role());
        assertEquals("TestMage", assignment.characterName());

        assertThrows(IllegalArgumentException.class, () -> Protocol.clanRankControlRequest(
                new Protocol.Writer().c(Protocol.C_RANK_CONTROL).c(1).c(4)
                        .s("TestMage").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.clanRankControlRequest(
                new byte[] {(byte) Protocol.C_RANK_CONTROL, 0, 1}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.clanRankControlRequest(
                new Protocol.Writer().c(Protocol.C_RANK_CONTROL).c(2).bytes()));
    }

    private static void assertClanRecord(Protocol.Reader reader, String name, int role,
                                         int value1, int value2, String title, int status) {
        assertEquals(name, reader.s());
        assertEquals(role, reader.h());
        assertEquals(value1, reader.c());
        assertEquals(value2, reader.c());
        assertEquals(title, reader.s());
        assertEquals(status, reader.c());
    }
}
