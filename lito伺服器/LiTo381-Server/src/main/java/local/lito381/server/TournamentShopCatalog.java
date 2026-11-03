package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-owned LiTo Tournament shop rows.
 *
 * <p>The selected 3.81C client proves that the allowance is fixed per match,
 * cannot accumulate, and buys match-local consumables. It does not embed the
 * amount or catalog. The 20,000 allowance and these sixteen rows/prices are
 * preserved same-family 2003 LiTo evidence, so they remain separately named
 * and auditable rather than being presented as a captured 3.81C server list.</p>
 */
final class TournamentShopCatalog {
    static final int SAME_FAMILY_ALLOWANCE = 20_000;

    record Offer(InventoryCatalog.Definition definition, long buyPrice, long sellPrice) {
        Offer {
            if (definition == null || buyPrice <= 0 || sellPrice <= 0) {
                throw new IllegalArgumentException("invalid Tournament shop offer");
            }
        }

        long catalogId() {
            return definition.catalogId();
        }
    }

    private static final List<Offer> COMPATIBILITY = InventoryCatalog.buyItems().stream()
            .map(definition -> new Offer(
                    definition, definition.buyPrice(), definition.sellPrice()))
            .toList();

    private static final List<Offer> SAME_FAMILY_NATIVE = List.of(
            offer(InventoryCatalog.NATIVE_HEALING_POTION, 40),
            offer(InventoryCatalog.EXTRA_HEALING_POTION, 200),
            offer(InventoryCatalog.SELF_HASTE_POTION, 200),
            offer(InventoryCatalog.LIGHTNING_WAND, 5_000),
            offer(InventoryCatalog.SILVER_ARROW, 20),
            offer(InventoryCatalog.ELVEN_WAFER, 1_000),
            offer(InventoryCatalog.CURE_POISON_POTION, 70),
            offer(InventoryCatalog.GREATER_HEALING_POTION, 600),
            offer(InventoryCatalog.MAGIC_GEM, 900),
            offer(InventoryCatalog.MITHRIL_ARROW, 30),
            offer(InventoryCatalog.BRAVE_POTION, 800),
            offer(InventoryCatalog.WISDOM_POTION, 600),
            offer(InventoryCatalog.POLYMORPH_SCROLL, 1_300),
            offer(InventoryCatalog.WHETSTONE, 150),
            offer(InventoryCatalog.BLUE_POTION, 700),
            offer(InventoryCatalog.SPIRIT_CRYSTAL, 700));

    private static final Map<Long, Offer> SAME_FAMILY_NATIVE_BY_ID = index(
            SAME_FAMILY_NATIVE);
    private static final Map<Long, Offer> COMPATIBILITY_BY_ID = index(COMPATIBILITY);

    private TournamentShopCatalog() {}

    static List<Offer> offers(boolean nativeEconomy) {
        return nativeEconomy ? SAME_FAMILY_NATIVE : COMPATIBILITY;
    }

    static Offer find(boolean nativeEconomy, long catalogId) {
        return (nativeEconomy ? SAME_FAMILY_NATIVE_BY_ID : COMPATIBILITY_BY_ID)
                .get(catalogId);
    }

    static List<RepositoryTournamentShopOffer> defaultSourceRows() {
        List<Offer> offers = SAME_FAMILY_NATIVE;
        java.util.ArrayList<RepositoryTournamentShopOffer> rows = new java.util.ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            Offer offer = offers.get(index);
            rows.add(new RepositoryTournamentShopOffer(
                    offer.catalogId(), index, offer.buyPrice(), offer.sellPrice()));
        }
        return List.copyOf(rows);
    }

    /** Validates and resolves SQL rows against the selected static item catalog. */
    static List<Offer> fromDatabase(List<RepositoryTournamentShopOffer> rows) {
        if (rows == null) throw new IllegalArgumentException("null Tournament shop rows");
        java.util.ArrayList<Offer> offers = new java.util.ArrayList<>();
        Set<Long> catalogIds = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        for (RepositoryTournamentShopOffer row : rows) {
            if (row == null || !catalogIds.add(row.itemId())
                    || !sortOrders.add(row.sortOrder())) {
                throw new IllegalArgumentException("duplicate/null Tournament shop database row");
            }
            offers.add(new Offer(InventoryCatalog.require(row.itemId()),
                    row.buyPrice(), row.sellPrice()));
        }
        return List.copyOf(offers);
    }

    static Offer find(List<Offer> offers, long catalogId) {
        if (offers == null) return null;
        for (Offer offer : offers) {
            if (offer.catalogId() == catalogId) return offer;
        }
        return null;
    }

    private static Offer offer(long catalogId, long price) {
        // A match-local item can be returned without manufacturing points.
        // Exact 3.81C sell prices remain uncaptured, so the conservative
        // reversible policy is a full refund of the same server-owned price.
        return new Offer(InventoryCatalog.require(catalogId), price, price);
    }

    private static Map<Long, Offer> index(List<Offer> offers) {
        Map<Long, Offer> result = new LinkedHashMap<>();
        for (Offer offer : offers) {
            if (result.put(offer.catalogId(), offer) != null) {
                throw new IllegalStateException("duplicate Tournament catalog id");
            }
        }
        return Map.copyOf(result);
    }
}
