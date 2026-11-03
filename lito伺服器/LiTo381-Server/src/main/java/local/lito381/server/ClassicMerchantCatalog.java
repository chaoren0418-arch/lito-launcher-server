package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-owned Pandora subset for the native 0x2B/0x2C shop controllers. */
final class ClassicMerchantCatalog {
    record Offer(long orderId, InventoryCatalog.Definition definition,
                 long unitPrice) {
        Offer {
            if (orderId < 0 || orderId > 0xFFFF_FFFFL || definition == null
                    || unitPrice <= 0 || unitPrice > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException("invalid classic merchant offer");
            }
        }

        Protocol.ClassicBuyListRow wireRow() {
            return new Protocol.ClassicBuyListRow(
                    orderId, definition.icon(), unitPrice, definition.name(),
                    definition.detail().getBytes(Protocol.LEGACY_TEXT));
        }
    }

    private static final List<Offer> OFFERS = List.of(
            offer(0, InventoryCatalog.SHORT_SWORD, 110),
            offer(1, InventoryCatalog.NATIVE_HEALING_POTION, 37),
            offer(2, InventoryCatalog.SELF_HASTE_POTION, 200),
            offer(3, InventoryCatalog.CURE_POISON_POTION, 70));
    private static final Map<Long, Offer> BY_ORDER_ID;
    private static final Map<Long, Long> SELL_PRICES = Map.of(
            InventoryCatalog.SHORT_SWORD, 55L,
            InventoryCatalog.NATIVE_HEALING_POTION, 18L,
            InventoryCatalog.SELF_HASTE_POTION, 100L,
            InventoryCatalog.CURE_POISON_POTION, 35L);

    static {
        Map<Long, Offer> indexed = new LinkedHashMap<>();
        for (Offer offer : OFFERS) {
            if (indexed.put(offer.orderId(), offer) != null) {
                throw new IllegalStateException("duplicate classic merchant order id");
            }
        }
        BY_ORDER_ID = Map.copyOf(indexed);
    }

    private ClassicMerchantCatalog() {}

    static List<Offer> offers() {
        return OFFERS;
    }

    static Offer find(long orderId) {
        return BY_ORDER_ID.get(orderId);
    }

    static long sellPrice(InventoryCatalog.Definition definition) {
        if (definition == null || !definition.tradable()) return 0;
        // Standard NPC commerce is a separate Adena economy. Never expose
        // these prices through the Tournament point-shop definition fields.
        return SELL_PRICES.getOrDefault(definition.catalogId(), 0L);
    }

    private static Offer offer(long orderId, long catalogId, long unitPrice) {
        return new Offer(orderId, InventoryCatalog.require(catalogId), unitPrice);
    }
}
