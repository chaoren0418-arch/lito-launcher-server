package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolClassicServerHandoffTest {
    private static final long LOOPBACK_127_0_0_2 = 0x0200_007FL;

    @Test void buildsExactNativeServerListSdhRecords() {
        byte[] expected = new Protocol.Writer()
                .c(0x3B).c(2)
                .s("Alpha").d(LOOPBACK_127_0_0_2).h(32100)
                .s("角色來源").d(0x0100_007FL).h(32000)
                .bytes();
        assertArrayEquals(expected, Protocol.classicServerList(List.of(
                new Protocol.ClassicServerEndpoint("Alpha", LOOPBACK_127_0_0_2, 32100),
                new Protocol.ClassicServerEndpoint("角色來源", 0x0100_007FL, 32000))));
    }

    @Test void enforcesSignedCountNameAndEndpointBoundaries() {
        Protocol.ClassicServerEndpoint valid =
                new Protocol.ClassicServerEndpoint("A".repeat(63), 0, 1);
        assertDoesNotThrow(() -> Protocol.classicServerList(List.of(valid)));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(null));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(
                new ArrayList<>(java.util.Collections.nCopies(128, valid))));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(
                List.of(new Protocol.ClassicServerEndpoint("A".repeat(64), 0, 1))));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(
                List.of(new Protocol.ClassicServerEndpoint("bad\tname", 0, 1))));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(
                List.of(new Protocol.ClassicServerEndpoint("A", -1, 1))));
        assertThrows(IllegalArgumentException.class, () -> Protocol.classicServerList(
                List.of(new Protocol.ClassicServerEndpoint("A", 0, 0))));
    }

    @Test void acceptsOnlyTheOpcodeOnlyNativeServerSelect() {
        assertDoesNotThrow(() -> Protocol.requireClassicServerSelect(new byte[] {0x35}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireClassicServerSelect(new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireClassicServerSelect(new byte[] {0x34}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireClassicServerSelect(new byte[] {0x35, 0}));
    }

    @Test void buildsExactNativeVsServerRedirectAndProtectsTenDwordStatusTable() {
        assertArrayEquals(new byte[] {
                        (byte) 0xF4, 0,
                        0x7F, 0, 0, 2,
                        0x64, 0x7D
                },
                Protocol.litoVsServerIp(0, LOOPBACK_127_0_0_2, 32100));
        assertDoesNotThrow(() -> Protocol.litoVsServerIp(9, 0xFFFF_FFFFL, 0xFFFF));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoVsServerIp(10, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoVsServerIp(0, 0x1_0000_0000L, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoVsServerIp(0, 0, 0));
    }

    @Test void keepsRoomsAndSelectableVsServersInTheirNativeFbGroups() {
        byte[] expected = new Protocol.Writer()
                .c(0xFB)
                .d(1).d(1)
                .s("進行中戰鬥").d(LOOPBACK_127_0_0_2).h(32100).d(7).d(3)
                .s("無界擂台").d(LOOPBACK_127_0_0_2).h(32100).d(12).d(100)
                .bytes();
        assertArrayEquals(expected, Protocol.litoServerDirectory(
                List.of(new Protocol.LitoRoomDirectoryEntry(
                        "進行中戰鬥", LOOPBACK_127_0_0_2, 32100, 7, 3)),
                List.of(new Protocol.LitoVsServerDirectoryEntry(
                        "無界擂台", LOOPBACK_127_0_0_2, 32100, 12, 100))));

        byte[] serverOnly = Protocol.litoServerDirectory(List.of(), List.of(
                new Protocol.LitoVsServerDirectoryEntry(
                        "無界擂台", LOOPBACK_127_0_0_2, 32100, 0, 100)));
        Protocol.Reader in = new Protocol.Reader(serverOnly);
        org.junit.jupiter.api.Assertions.assertEquals(0xFB, in.c());
        org.junit.jupiter.api.Assertions.assertEquals(0, in.d());
        org.junit.jupiter.api.Assertions.assertEquals(1, in.d());
    }

    @Test void rejectsFbRowsThatWouldAliasASelectableServerAsRoomZero() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoServerDirectory(
                List.of(new Protocol.LitoRoomDirectoryEntry(
                        "錯誤伺服器列", LOOPBACK_127_0_0_2, 32100, 0, 100)),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoServerDirectory(
                List.of(), List.of(new Protocol.LitoVsServerDirectoryEntry(
                        "無容量", LOOPBACK_127_0_0_2, 32100, 0, 0))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoServerDirectory(null, List.of()));

        Protocol.LitoVsServerDirectoryEntry valid =
                new Protocol.LitoVsServerDirectoryEntry(
                        "無界擂台", LOOPBACK_127_0_0_2, 32100, 0, 100);
        assertDoesNotThrow(() -> Protocol.litoServerDirectory(
                List.of(), java.util.Collections.nCopies(10, valid)));
        assertThrows(IllegalArgumentException.class, () -> Protocol.litoServerDirectory(
                List.of(), java.util.Collections.nCopies(11, valid)));
    }

    @Test void buildsDirectModeSixRentalTownJoinWithoutOrdinaryRoomRow() {
        byte[] packet = Protocol.litoRentalTownJoinResult(
                0x5000_0001L, "租賃村莊", 500, "TestKnight");
        Protocol.Reader in = new Protocol.Reader(packet);
        assertEquals(Protocol.S_LITO_JOIN_RESULT, in.c());
        assertEquals(0, in.c());
        assertEquals(0x5000_0001L, in.d());
        assertEquals("租賃村莊", in.s());
        assertEquals(0, in.c());
        assertEquals("", in.s());
        assertEquals(1, in.h());
        assertEquals(30, in.h());
        assertEquals(6, in.c());
        assertEquals(500, in.h());
        for (int index = 0; index < 5; index++) assertEquals(0, in.c());
        assertEquals("TestKnight", in.s());
        for (int index = 0; index < 5; index++) assertEquals(0, in.c());
        assertEquals(0, in.remaining());

        assertDoesNotThrow(() -> Protocol.litoRentalTownJoinResult(
                7, "Rental", 560, "Player"));
        Protocol.Reader twoUsers = new Protocol.Reader(
                Protocol.litoRentalTownJoinResult(
                        7, "Rental", 560, "Player", 2));
        twoUsers.c(); twoUsers.c(); twoUsers.d(); twoUsers.s();
        twoUsers.c(); twoUsers.s();
        assertEquals(2, twoUsers.h());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRentalTownJoinResult(
                        7, "Rental", 560, "Player", 31));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRentalTownJoinResult(0, "Rental", 500, "Player"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRentalTownJoinResult(7, "Rental", 501, "Player"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRentalTownJoinResult(7, "", 500, "Player"));
    }
}
