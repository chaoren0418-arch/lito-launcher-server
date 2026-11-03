package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolPersonalShopTest {
    @Test void parsesManualNativeOpenAndExactClose() {
        byte[] announcement = new byte[] {'s', 'e', 'l', 'l', (byte) 0xFF, 'b', 'u', 'y', 0};
        byte[] open = new Protocol.Writer().c(Protocol.C_PERSONAL_SHOP).c(0).h(1)
                .d(0x7000_0010L).d(125).d(3)
                .h(1).d(0x7000_0020L).d(75).d(2)
                .raw(announcement).bytes();

        Protocol.PersonalShopRequest request = Protocol.personalShopRequest(open);

        assertEquals(0, request.type());
        assertEquals(List.of(new Protocol.PersonalShopOffer(0x7000_0010L, 125, 3)),
                request.sellOffers());
        assertEquals(List.of(new Protocol.PersonalShopOffer(0x7000_0020L, 75, 2)),
                request.buyOffers());
        assertArrayEquals(announcement, request.announcement());
        assertEquals(1, Protocol.personalShopRequest(
                new Protocol.Writer().c(Protocol.C_PERSONAL_SHOP).c(1).bytes()).type());
    }

    @Test void parsesExactNativeQuery() {
        Protocol.PersonalShopQuery query = Protocol.personalShopQuery(
                new Protocol.Writer().c(Protocol.C_QUERY_PERSONAL_SHOP)
                        .c(1).d(0x6200_0100L).bytes());
        assertEquals(1, query.type());
        assertEquals(0x6200_0100L, query.targetObjectId());
    }

    @Test void buildsExactSellAndBuyLists() {
        byte[] sell = Protocol.personalShopSellList(0x6200_0100L, List.of(
                new Protocol.PersonalShopSellRow(2, 0, 17, 3, 125,
                        "Potion", new byte[] {4, 5})));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_PERSONAL_SHOP_LIST).c(0)
                .d(0x6200_0100L).h(1).c(2).c(0).h(17).d(3).d(125)
                .s("Potion").c(2).raw(new byte[] {4, 5}).bytes(), sell);

        byte[] buy = Protocol.personalShopBuyList(0x6200_0100L, List.of(
                new Protocol.PersonalShopBuyRow(3, 0x7000_0020L, 2, 75)));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_PERSONAL_SHOP_LIST).c(1)
                .d(0x6200_0100L).h(1).c(3).d(0x7000_0020L).d(2).d(75).bytes(), buy);
    }

    @Test void buildsNativeShopActorAction() {
        byte[] message = new byte[] {'x', (byte) 0xFF, 'y', 0};
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_ACTION).d(0x6200_0100L)
                        .c(70).raw(message).bytes(),
                Protocol.personalShopAction(0x6200_0100L, message));
    }

    @Test void rejectsMalformedOrUnboundedRequests() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.personalShopRequest(
                new Protocol.Writer().c(Protocol.C_PERSONAL_SHOP).c(1).c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.personalShopRequest(
                new Protocol.Writer().c(Protocol.C_PERSONAL_SHOP).c(0).h(9).h(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.personalShopQuery(
                new Protocol.Writer().c(Protocol.C_QUERY_PERSONAL_SHOP).c(2).d(1).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.personalShopAction(
                1, new byte[81]));
    }
}
