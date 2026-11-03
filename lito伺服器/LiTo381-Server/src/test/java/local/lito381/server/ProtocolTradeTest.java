package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolTradeTest {
    @Test void parsesExactNativeTradeClientPackets() {
        Protocol.ExchangeRequest ask = Protocol.exchangeRequest(new Protocol.Writer()
                .c(Protocol.C_ASK_XCHG).d(0x1122_3344L).bytes());
        assertEquals(0x1122_3344L, ask.targetObjectId());

        Protocol.ExchangeItemRequest item = Protocol.exchangeItemRequest(new Protocol.Writer()
                .c(Protocol.C_ADD_XCHG).d(0x7000_0001L).d(2).bytes());
        assertEquals(0x7000_0001L, item.inventoryObjectId());
        assertEquals(2, item.quantity());

        Protocol.requireExchangeOpcodeOnly(new byte[] {0x36}, Protocol.C_CANCEL_XCHG);
        Protocol.requireExchangeOpcodeOnly(new byte[] {0x37}, Protocol.C_ACCEPT_XCHG);
    }

    @Test void writesExactQuestionStartItemAndTerminalPackets() {
        assertArrayEquals(new byte[] {
                0x24, (byte) 0xFC, 0x00,
                'T', 'e', 's', 't', 'K', 'n', 'i', 'g', 'h', 't', 0x00
        }, Protocol.exchangeAsk("TestKnight"));
        assertArrayEquals(new byte[] {
                0x3C, 'T', 'e', 's', 't', 'M', 'a', 'g', 'e', 0x00
        }, Protocol.exchangeStart("TestMage"));
        assertArrayEquals(new byte[] {
                0x3D, 0x01, 0x1B, 0x00,
                'H', 'e', 'a', 'l', 'i', 'n', 'g', ' ',
                'P', 'o', 't', 'i', 'o', 'n', ' ', '(', '2', ')', 0x00,
                0x01, 0x00
        }, Protocol.exchangeAddItem(1, 27, "Healing Potion (2)", 1));
        assertArrayEquals(new byte[] {0x3E, 0x00}, Protocol.exchangeResult(
                Protocol.ExchangeTerminalResult.COMPLETED));
        assertArrayEquals(new byte[] {0x3E, 0x01}, Protocol.exchangeResult(
                Protocol.ExchangeTerminalResult.CANCELLED_OR_FAILED));
    }

    @Test void rejectsMalformedOrOutOfRangeTradePackets() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.exchangeRequest(
                new byte[] {0x34, 0, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.exchangeItemRequest(
                new Protocol.Writer().c(Protocol.C_ADD_XCHG).d(1).d(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.requireExchangeOpcodeOnly(
                new byte[] {0x37, 0}, Protocol.C_ACCEPT_XCHG));
        assertThrows(IllegalArgumentException.class, () -> Protocol.exchangeAddItem(
                2, 27, "Healing Potion", 1));
        assertThrows(NullPointerException.class, () -> Protocol.exchangeResult(null));
    }
}
