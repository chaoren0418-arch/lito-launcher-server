package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProtocolShopTransactionTest {
    @Test void parsesNativeOrdinaryMultiRowBuy() {
        byte[] payload = new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                .c(0).h(2)
                .d(1001).d(3)
                .d(1002).d(7)
                .bytes();

        Protocol.ShopTransaction transaction = Protocol.shopTransaction(payload);

        assertFalse(transaction.hasContext());
        assertEquals(0, transaction.operation());
        assertEquals(2, transaction.lines().size());
        assertEquals(new Protocol.ShopLine(1001, 3), transaction.lines().get(0));
        assertEquals(new Protocol.ShopLine(1002, 7), transaction.lines().get(1));
    }

    @Test void parsesNativePhaseFourSellContext() {
        byte[] payload = new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                .d(0x11223344L).c(1).h(1).d(2001).d(2)
                .raw(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).bytes();

        Protocol.ShopTransaction transaction = Protocol.shopTransaction(payload);

        assertTrue(transaction.hasContext());
        assertEquals(0x11223344L, transaction.contextId());
        assertEquals(1, transaction.operation());
        assertEquals(new Protocol.ShopLine(2001, 2), transaction.lines().get(0));
        assertTrue(transaction.exactNativeTrailer());
        assertEquals(8, transaction.nativeTrailer().length);
    }

    @Test void acceptsContextualPersonalShopSellIndexZero() {
        Protocol.ShopTransaction transaction = Protocol.shopTransaction(
                new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                        .d(3).c(0).h(1).d(0).d(1).raw(new byte[8]).bytes());
        assertTrue(transaction.hasContext());
        assertEquals(new Protocol.ShopLine(0, 1), transaction.lines().get(0));
        assertTrue(transaction.exactNativeTrailer());
    }

    @Test void rejectsSyntheticAndMalformedLayouts() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.shopTransaction(
                new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL).d(1002).c(0).h(2).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.shopTransaction(
                new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL).c(2).h(1).d(1002).d(1).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.shopTransaction(
                new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL).c(0).h(2).d(1002).d(1).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.shopTransaction(
                new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL).c(0).h(1).d(0).d(1).bytes()));
    }
}
