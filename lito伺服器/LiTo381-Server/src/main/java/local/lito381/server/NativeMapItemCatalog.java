package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact 3.81C map-item to native S_SHOW_MAP page mapping. */
final class NativeMapItemCatalog {
    record Item(long catalogId, int page, int icon, int groundGfx, String nameId) {
        Item {
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL
                    || page < 1 || page > 18
                    || icon < 0 || icon > 0xFFFF
                    || groundGfx < 0 || groundGfx > 0xFFFF
                    || nameId == null || nameId.isBlank()) {
                throw new IllegalArgumentException("invalid native map-item metadata");
            }
        }
    }

    private static final List<Item> ITEMS = List.of(
            new Item(40373, 16, 1138, 2982, "$1101"),
            new Item(40374, 1, 479, 1275, "$1102"),
            new Item(40375, 2, 479, 1275, "$1103"),
            new Item(40376, 3, 479, 1275, "$1104"),
            new Item(40377, 4, 479, 1275, "$1105"),
            new Item(40378, 5, 479, 1275, "$1106"),
            new Item(40379, 6, 479, 1275, "$1107"),
            new Item(40380, 7, 479, 1275, "$1108"),
            new Item(40381, 8, 479, 1275, "$1109"),
            new Item(40382, 9, 479, 1275, "$1188"),
            new Item(40383, 10, 479, 1275, "$1533"),
            new Item(40384, 11, 479, 1275, "$1534"),
            new Item(40385, 12, 479, 1275, "$1535"),
            new Item(40386, 13, 479, 1275, "$1607"),
            new Item(40387, 14, 479, 1275, "$1889"),
            new Item(40388, 15, 479, 1275, "$2203"),
            new Item(40389, 17, 479, 1275, "$2543"),
            new Item(40390, 18, 479, 1275, "$3616"));
    private static final Map<Long, Item> BY_CATALOG_ID;

    static {
        Map<Long, Item> byCatalogId = new LinkedHashMap<>();
        for (Item item : ITEMS) {
            if (byCatalogId.put(item.catalogId(), item) != null) {
                throw new IllegalStateException("duplicate native map item " + item.catalogId());
            }
        }
        BY_CATALOG_ID = Map.copyOf(byCatalogId);
    }

    private NativeMapItemCatalog() {}

    static List<Item> all() {
        return ITEMS;
    }

    static Item find(long catalogId) {
        return BY_CATALOG_ID.get(catalogId);
    }

    static int requirePage(long catalogId) {
        Item item = find(catalogId);
        if (item == null) throw new IllegalArgumentException("unknown native map item");
        return item.page();
    }
}
