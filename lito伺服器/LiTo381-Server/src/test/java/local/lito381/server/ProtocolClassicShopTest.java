package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolClassicShopTest {
    @Test
    void parsesExactC28RowsIncludingZeroBasedBuyOrder() {
        byte[] payload = bytes(
                0x28, 0x44, 0x33, 0x22, 0x11, 0x00, 0x02, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00,
                0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00);

        Protocol.ClassicShopTransaction result =
                Protocol.classicShopTransaction(payload);

        assertEquals(0x1122_3344L, result.npcObjectId());
        assertEquals(0, result.mode());
        assertEquals(List.of(new Protocol.ShopLine(0, 2),
                new Protocol.ShopLine(3, 1)), result.lines());
    }

    @Test
    void writesExactS2bBuyListAndCompatibilityTail() {
        byte[] expected = bytes(
                0x2B, 0x44, 0x33, 0x22, 0x11, 0x01, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x6A, 0x00,
                0x6E, 0x00, 0x00, 0x00,
                0x24, 0x31, 0x30, 0x30, 0x00,
                0x02, 0x41, 0x42, 0x07, 0x00);

        assertArrayEquals(expected, Protocol.classicBuyList(0x1122_3344L,
                List.of(new Protocol.ClassicBuyListRow(
                        0, 106, 110, "$100", bytes(0x41, 0x42)))));
    }

    @Test
    void writesRetainedLitoBuyListWithoutClassicAdenaTail() {
        assertArrayEquals(bytes(
                        0x2B, 0x44, 0x33, 0x22, 0x11, 0x01, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x6A, 0x00,
                        0x0A, 0x00, 0x00, 0x00,
                        0x24, 0x31, 0x30, 0x30, 0x00,
                        0x02, 0x41, 0x42),
                Protocol.litoRentalBuyList(0x1122_3344L,
                        List.of(new Protocol.ClassicBuyListRow(
                                0, 106, 10, "$100", bytes(0x41, 0x42)))));
    }

    @Test
    void writesExactS2cSellList() {
        assertArrayEquals(bytes(
                        0x2C, 0x44, 0x33, 0x22, 0x11, 0x01, 0x00,
                        0x88, 0x77, 0x66, 0x55, 0x37, 0x00, 0x00, 0x00),
                Protocol.classicSellList(0x1122_3344L,
                        List.of(new Protocol.ClassicSellListRow(
                                0x5566_7788L, 55))));
    }

    @Test
    void writesExactS31PersonalWarehouseRows() {
        assertArrayEquals(bytes(
                        0x31, 0x44, 0x33, 0x22, 0x11, 0x01, 0x00, 0x03,
                        0x04, 0x03, 0x02, 0x01, 0x01, 0x6A, 0x00, 0x00,
                        0x02, 0x00, 0x00, 0x00, 0x01,
                        0x24, 0x31, 0x30, 0x30, 0x00,
                        0x1E, 0x00, 0x00, 0x00),
                Protocol.retrieveList(0x1122_3344L, 3,
                        List.of(new Protocol.RetrieveListRow(
                                0x0102_0304L, 1, 106, 0, 2, 1, "$100"))));
    }

    @Test
    void writesPledgeFeeTailButNoElvenTail() {
        assertArrayEquals(bytes(
                        0x31, 0x44, 0x33, 0x22, 0x11, 0x00, 0x00, 0x05,
                        0x1E, 0x00, 0x00, 0x00),
                Protocol.retrieveList(0x1122_3344L, 5, List.of()));
        assertArrayEquals(bytes(
                        0x31, 0x44, 0x33, 0x22, 0x11, 0x00, 0x00, 0x09),
                Protocol.retrieveList(0x1122_3344L, 9, List.of()));
    }

    @Test
    void rejectsMalformedC28AndInvalidServerRows() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicShopTransaction(null));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicShopTransaction(bytes(0x28)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicShopTransaction(bytes(
                        0x28, 1, 0, 0, 0, 0, 0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicShopTransaction(bytes(
                        0x28, 1, 0, 0, 0, 17, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicShopTransaction(bytes(
                        0x28, 1, 0, 0, 0, 0, 1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicShopTransaction(bytes(
                        0x28, 1, 0, 0, 0, 0, 1, 0,
                        0, 0, 0, 0, 0, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicBuyList(1, List.of(
                        new Protocol.ClassicBuyListRow(0, 1, 0, "$1", new byte[0]))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.retrieveList(1, 4, List.of()));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
