package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolCastleTest {
    @Test
    void parsesExactNativeCastleRequests() {
        Protocol.CastleTaxRequest tax = Protocol.castleTaxRequest(bytes(
                0x41, 0x44, 0x33, 0x22, 0x11, 0x23, 0x00));
        assertEquals(0x1122_3344L, tax.playerObjectId());
        assertEquals(35, tax.taxRate());

        Protocol.CastleAmountRequest withdraw = Protocol.castleAmountRequest(bytes(
                0x42, 0x44, 0x33, 0x22, 0x11, 0x39, 0x30, 0x00, 0x00),
                Protocol.C_WITHDRAW);
        assertEquals(12_345, withdraw.amount());
        Protocol.CastleAmountRequest deposit = Protocol.castleAmountRequest(bytes(
                0x47, 0x44, 0x33, 0x22, 0x11, 0x40, 0xE2, 0x01, 0x00),
                Protocol.C_DEPOSIT);
        assertEquals(123_456, deposit.amount());

        Protocol.CastleSecurityQuery security = Protocol.castleSecurityQuery(bytes(
                0x64, 0x44, 0x33, 0x22, 0x11));
        assertEquals(0x1122_3344L, security.stewardObjectId());

        Protocol.CastleSecurityChange enable = Protocol.castleSecurityChange(bytes(
                0x65, 0x44, 0x33, 0x22, 0x11, 1, 1, 0, 0, 0));
        assertEquals(0x1122_3344L, enable.stewardObjectId());
        assertEquals(true, enable.enabled());
        assertEquals(1, enable.nativeSelector());

        Protocol.CastleSecurityChange disable = Protocol.castleSecurityChange(bytes(
                0x65, 0x44, 0x33, 0x22, 0x11, 0, 1, 0, 0, 0));
        assertEquals(false, disable.enabled());

        Protocol.ClanWarRequest unowned = Protocol.clanWarRequest(bytes(
                Protocol.C_WAR, 0, ' ', 0));
        assertEquals(Protocol.ClanWarRequestType.DECLARE, unowned.type());
        assertEquals(" ", unowned.targetClan());
    }

    @Test
    void buildsExactNativeCastleResponses() {
        assertArrayEquals(bytes(0x45, 0x44, 0x33, 0x22, 0x11, 10, 10, 50),
                Protocol.castleTax(0x1122_3344L, 10, 10, 50));
        assertArrayEquals(bytes(0x46, 0x44, 0x33, 0x22, 0x11,
                        0x39, 0x30, 0x00, 0x00),
                Protocol.castleWithdrawal(0x1122_3344L, 12_345));
        assertArrayEquals(bytes(0x4C, 0x44, 0x33, 0x22, 0x11),
                Protocol.castleDeposit(0x1122_3344L));
        assertArrayEquals(bytes(0x47, 5, 0x44, 0x33, 0x22, 0x11),
                Protocol.castleOwner(5, 0x1122_3344L));
        assertArrayEquals(bytes(0x47, 8, 0x44, 0x33, 0x22, 0x11),
                Protocol.castleOwner(8, 0x1122_3344L));
        assertArrayEquals(bytes(0x7B, 3),
                Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.CASTLE_WAR_INITIATIVE));
        assertArrayEquals(bytes(0x7B, 4),
                Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.CASTLE_WAR_OCCUPIED));
    }

    @Test
    void implementsExactDormantCastleTimeWireFamily() {
        Protocol.requireSelectableTime(bytes(0x4C));
        assertEquals(0x7F, Protocol.selectedTimeIdentifier(bytes(
                0x4D, 0x7F, 0x00, 0x00, 0x00)));
        assertEquals(0x80, Protocol.selectedTimeIdentifier(bytes(
                0x4D, 0x80, 0xFF, 0xFF, 0xFF)));

        assertArrayEquals(bytes(
                        0x54, 0x02, 0x00, 'C', 'S', 'T', 0x00,
                        0x07, 0x78, 0x56, 0x34, 0x12,
                        0x80, 0xFF, 0xFF, 0xFF, 0xFF),
                Protocol.selectableTimeList("CST", java.util.List.of(
                        new Protocol.SelectableTime(7, 0x1234_5678L),
                        new Protocol.SelectableTime(0x80, 0xFFFF_FFFFL))));
        assertArrayEquals(bytes(0x54, 0x00, 0x00, 'C', 'S', 'T', 0x00),
                Protocol.selectableTimeList("CST", java.util.List.of()));
    }

    @Test
    void parsesExactNativeMercenaryControllerRequests() {
        Protocol.MercenaryEmployRequest employ = Protocol.mercenaryEmployRequest(bytes(
                0x3C, 0x44, 0x33, 0x22, 0x11,
                0x01, 0x00, 0x02, 0x00, 0x03, 0x00, 0x04, 0x00));
        assertEquals(0x1122_3344L, employ.stewardObjectId());
        assertEquals(1, employ.selectedRowCount());
        assertEquals(2, employ.entryId());
        assertEquals(3, employ.quantity());
        assertEquals(4, employ.submittedTotalPrice());

        Protocol.MercenarySelectRequest select = Protocol.mercenarySelectRequest(bytes(
                0x3E, 0x44, 0x33, 0x22, 0x11, 0x06, 0x00));
        assertEquals(0x1122_3344L, select.stewardObjectId());
        assertEquals(6, select.identifier());

        Protocol.MercenaryArrangeRequest arrange = Protocol.mercenaryArrangeRequest(bytes(
                0x3F, 0x44, 0x33, 0x22, 0x11,
                0x01, 0x00, 0x02, 0x00,
                0x88, 0x77, 0x66, 0x55, 0x03, 0x00));
        assertEquals(0x1122_3344L, arrange.stewardObjectId());
        assertEquals(1, arrange.command());
        assertEquals(2, arrange.selector());
        assertEquals(0x5566_7788L, arrange.callbackToken());
        assertEquals(3, arrange.quantity());
    }

    @Test
    void buildsExactNativeMercenaryControllerResponses() {
        assertArrayEquals(bytes(
                        0x40, 0x44, 0x33, 0x22, 0x11,
                        0x88, 0x77, 0x66, 0x55, 0x01, 0x00,
                        0x02, 0x00, 'G', 'u', 'a', 'r', 'd', 0x00, 0x2C, 0x01),
                Protocol.mercenaryEmploy(0x1122_3344L, 0x5566_7788L,
                        java.util.List.of(new Protocol.MercenaryEmployRow(2, "Guard", 300))));
        assertArrayEquals(bytes(
                        0x42, 0x44, 0x33, 0x22, 0x11, 0x02, 0x00,
                        0x07, 0x00, 'S', 'p', 'e', 'a', 'r', 0x00,
                        0x08, 0x00, 'B', 'o', 'w', 0x00),
                Protocol.mercenarySelect(0x1122_3344L, java.util.List.of(
                        new Protocol.MercenarySelectRow(7, "Spear"),
                        new Protocol.MercenarySelectRow(8, "Bow"))));
        assertArrayEquals(bytes(
                        0x43, 0x44, 0x33, 0x22, 0x11,
                        0x34, 0x12, 0x07, 0x00, 0x03, 0x00,
                        'A', 'l', 'l', 'y', 0x00,
                        0x88, 0x77, 0x66, 0x55, 0x09, 0x00),
                Protocol.mercenaryArrange(0x1122_3344L, 0x1234, 7, 3,
                        "Ally", 0x5566_7788L, 9));
        assertArrayEquals(bytes(
                        0x44, 0x44, 0x33, 0x22, 0x11,
                        0x01, 0x00, 0x08, 0x00, 0x01, 0x00,
                        0x02, 0x00, 'A', 'r', 'c', 'h', 'e', 'r', 0x00, 0x03, 0x00),
                Protocol.archerArrange(0x1122_3344L, 1, 8,
                        java.util.List.of(new Protocol.ArcherArrangeRow(2, "Archer", 3))));

        assertArrayEquals(bytes(0x42, 0x44, 0x33, 0x22, 0x11, 0x00, 0x00),
                Protocol.mercenarySelect(0x1122_3344L, java.util.List.of()));
        assertArrayEquals(bytes(
                        0x43, 0x44, 0x33, 0x22, 0x11,
                        0, 0, 0, 0, 0, 0,
                        'T', 'e', 's', 't', 0,
                        0, 0, 0, 0, 0, 0),
                Protocol.mercenaryArrange(0x1122_3344L, 0, 0, 0, "Test", 0, 0));
        assertArrayEquals(bytes(
                        0x44, 0x44, 0x33, 0x22, 0x11,
                        0, 0, 0, 0, 0, 0),
                Protocol.archerArrange(0x1122_3344L, 0, 0, java.util.List.of()));
    }

    @Test
    void rejectsMalformedOrUnsafeCastleFrames() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleTaxRequest(bytes(0x41, 1, 0, 0, 0, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleAmountRequest(bytes(0x42, 1, 0, 0, 0, 0, 0, 0, 0),
                        Protocol.C_WITHDRAW));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleTax(1, 9, 10, 50));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWithdrawal(1, (long) Integer.MAX_VALUE + 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleOwner(9, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleOwner(5, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.CASTLE_WAR_GOING));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.MARRIED));
        assertThrows(NullPointerException.class,
                () -> Protocol.castleWarOwnershipEvent(null));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleSecurityQuery(bytes(0x64, 0, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleSecurityChange(bytes(
                        0x65, 1, 0, 0, 0, 2, 1, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleSecurityChange(bytes(
                        0x65, 1, 0, 0, 0, 1, 0, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.requireSelectableTime(bytes(0x4C, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectedTimeIdentifier(bytes(0x4D, 0x80, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectableTimeList("12345678", java.util.List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectableTimeList("CST", java.util.List.of(
                        new Protocol.SelectableTime(1, 0),
                        new Protocol.SelectableTime(1, 1))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.mercenaryEmployRequest(bytes(
                        0x3C, 1, 0, 0, 0, 0, 0, 2, 0, 3, 0, 4, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.mercenarySelectRequest(bytes(
                        0x3E, 1, 0, 0, 0, 2, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.mercenaryArrangeRequest(bytes(
                        0x3F, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.mercenarySelect(1, java.util.List.of(
                        new Protocol.MercenarySelectRow(1, "A"),
                        new Protocol.MercenarySelectRow(1, "B"))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.mercenarySelect(1, java.util.List.of(
                        new Protocol.MercenarySelectRow(0, "0"),
                        new Protocol.MercenarySelectRow(1, "1"),
                        new Protocol.MercenarySelectRow(2, "2"),
                        new Protocol.MercenarySelectRow(3, "3"),
                        new Protocol.MercenarySelectRow(4, "4"),
                        new Protocol.MercenarySelectRow(5, "5"),
                        new Protocol.MercenarySelectRow(6, "6"))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.mercenaryArrange(1, 0, 0, 0x1_0000, "Test", 0, 0));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) result[index] = (byte) values[index];
        return result;
    }
}
