package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact native {@code LTItemSet<class>-<selection>-c.tbl} catalog.
 *
 * <p>This is deliberately a read-only identity catalog.  It does not grant,
 * equip, persist, or return anything by itself.  {@link ClientSession}
 * orchestrates the session-local inventory after {@code S_WORLD} and removes
 * it on every town exit path.  The resource-derived spawn and the selected
 * identification/auto-equip policy remain explicit server policy rather than
 * historical upstream packet claims.
 */
final class RentalItemSetCatalog {
    record Key(int characterClass, int selection) {
        Key {
            if (characterClass < 0 || characterClass > 4 || selection < 0 || selection > 2) {
                throw new IllegalArgumentException("invalid rental item-set key");
            }
        }
    }

    record Item(long catalogId, int enchantLevel, int quantity, String slot) {
        Item {
            if (catalogId <= 0 || enchantLevel < 0 || enchantLevel > 10
                    || quantity <= 0 || slot == null || slot.isBlank()) {
                throw new IllegalArgumentException("invalid rental item-set row");
            }
        }

        InventoryCatalog.Definition definitionOrNull() {
            return InventoryCatalog.find(catalogId);
        }
    }

    record Set(Key key, String label, List<Item> items) {
        Set {
            if (key == null || label == null || label.isBlank()
                    || items == null || items.isEmpty()) {
                throw new IllegalArgumentException("invalid rental item set");
            }
            items = List.copyOf(items);
        }
    }

    private static final Map<Key, Set> SETS;

    static {
        Map<Key, Set> sets = new LinkedHashMap<>();
        put(sets, 0, 0, "平衡型", List.of(
                new Item(51L, 10, 1, "武器"), new Item(20229L, 8, 1, "盾牌"),
                new Item(20019L, 8, 1, "頭盔"), new Item(20094L, 0, 1, "盔甲"),
                new Item(20063L, 8, 1, "斗篷"), new Item(42092L, 8, 1, "手套"),
                new Item(20194L, 8, 1, "鞋類"), new Item(20085L, 8, 1, "衣類"),
                new Item(20263L, 0, 1, "項鍊"), new Item(20312L, 0, 1, "腰帶"),
                new Item(20287L, 0, 1, "戒指"), new Item(20287L, 0, 1, "戒指"),
                new Item(2L, 0, 3, "輔助")));
        put(sets, 0, 1, "攻擊型", List.of(
                new Item(219L, 10, 1, "武器"), new Item(20019L, 10, 1, "頭盔"),
                new Item(20094L, 0, 1, "盔甲"), new Item(20051L, 10, 1, "斗篷"),
                new Item(20187L, 8, 1, "手套"), new Item(20194L, 8, 1, "鞋類"),
                new Item(20085L, 8, 1, "衣類"), new Item(20264L, 0, 1, "項鍊"),
                new Item(20312L, 0, 1, "腰帶"), new Item(20287L, 0, 1, "戒指"),
                new Item(20287L, 0, 1, "戒指"), new Item(2L, 0, 3, "輔助")));
        put(sets, 0, 2, "輔助型", List.of(
                new Item(104L, 10, 1, "武器"), new Item(20019L, 10, 1, "頭盔"),
                new Item(20094L, 0, 1, "盔甲"), new Item(20051L, 10, 1, "斗篷"),
                new Item(20187L, 8, 1, "手套"), new Item(20194L, 8, 1, "鞋類"),
                new Item(20085L, 8, 1, "衣類"), new Item(20264L, 0, 1, "項鍊"),
                new Item(20312L, 0, 1, "腰帶"), new Item(20287L, 0, 1, "戒指"),
                new Item(20287L, 0, 1, "戒指"), new Item(2L, 0, 3, "輔助")));
        put(sets, 1, 0, "平衡型", List.of(
                new Item(57L, 10, 1, "武器"), new Item(20230L, 8, 1, "盾牌"),
                new Item(20006L, 8, 1, "頭盔"), new Item(20095L, 0, 1, "盔甲"),
                new Item(20063L, 8, 1, "斗篷"), new Item(20175L, 8, 1, "手套"),
                new Item(20194L, 8, 1, "鞋類"), new Item(20085L, 8, 1, "衣類"),
                new Item(20264L, 0, 1, "項鍊"), new Item(20320L, 0, 1, "腰帶"),
                new Item(20303L, 0, 1, "戒指"), new Item(20303L, 0, 1, "戒指")));
        put(sets, 1, 1, "攻擊型", List.of(
                new Item(66L, 10, 1, "武器"), new Item(20027L, 8, 1, "頭盔"),
                new Item(20095L, 0, 1, "盔甲"), new Item(20063L, 8, 1, "斗篷"),
                new Item(20187L, 8, 1, "手套"), new Item(20194L, 8, 1, "鞋類"),
                new Item(20085L, 8, 1, "衣類"), new Item(20264L, 0, 1, "項鍊"),
                new Item(20314L, 0, 1, "腰帶"), new Item(20303L, 0, 1, "戒指"),
                new Item(20303L, 0, 1, "戒指")));
        put(sets, 1, 2, "輔助型", List.of(
                new Item(106L, 10, 1, "武器"), new Item(20027L, 8, 1, "頭盔"),
                new Item(20095L, 0, 1, "盔甲"), new Item(20063L, 8, 1, "斗篷"),
                new Item(20187L, 8, 1, "手套"), new Item(20194L, 8, 1, "鞋類"),
                new Item(20085L, 8, 1, "衣類"), new Item(20264L, 0, 1, "項鍊"),
                new Item(20318L, 0, 1, "腰帶"), new Item(20303L, 0, 1, "戒指"),
                new Item(20303L, 0, 1, "戒指")));
        put(sets, 2, 0, "遠距離型", List.of(
                new Item(50L, 10, 1, "武器"), new Item(20033L, 8, 1, "頭盔"),
                new Item(20092L, 0, 1, "盔甲"), new Item(20063L, 8, 1, "斗篷"),
                new Item(20191L, 8, 1, "手套"), new Item(20194L, 8, 1, "鞋類"),
                new Item(20084L, 8, 1, "衣類"), new Item(20256L, 0, 1, "項鍊"),
                new Item(20316L, 0, 1, "腰帶"), new Item(20303L, 0, 1, "戒指"),
                new Item(20303L, 0, 1, "戒指"), new Item(40746L, 0, 2000, "輔助")));
        put(sets, 2, 1, "近距離型", List.of(
                new Item(50L, 10, 1, "武器"), new Item(20236L, 8, 1, "盾牌"),
                new Item(20033L, 8, 1, "頭盔"), new Item(20092L, 0, 1, "盔甲"),
                new Item(20063L, 8, 1, "斗篷"), new Item(20187L, 8, 1, "手套"),
                new Item(20194L, 8, 1, "鞋類"), new Item(20084L, 8, 1, "衣類"),
                new Item(20264L, 0, 1, "項鍊"), new Item(20316L, 0, 1, "腰帶"),
                new Item(20303L, 0, 1, "戒指"), new Item(20303L, 0, 1, "戒指")));
        put(sets, 3, 0, "法師型", List.of(
                new Item(115L, 10, 1, "武器"), new Item(20225L, 8, 1, "盾牌"),
                new Item(20023L, 8, 1, "頭盔"), new Item(20107L, 8, 1, "盔甲"),
                new Item(20055L, 8, 1, "斗篷"), new Item(42092L, 8, 1, "手套"),
                new Item(20194L, 8, 1, "鞋類"), new Item(20085L, 8, 1, "衣類"),
                new Item(20266L, 0, 1, "項鍊"), new Item(20319L, 0, 1, "腰帶"),
                new Item(20284L, 0, 1, "戒指"), new Item(20303L, 0, 1, "戒指")));
        put(sets, 3, 1, "魔戰士型", List.of(
                new Item(131L, 10, 1, "武器"), new Item(20225L, 8, 1, "盾牌"),
                new Item(20011L, 8, 1, "頭盔"), new Item(20093L, 0, 1, "盔甲"),
                new Item(20056L, 8, 1, "斗篷"), new Item(20187L, 8, 1, "手套"),
                new Item(20194L, 8, 1, "鞋類"), new Item(20085L, 8, 1, "衣類"),
                new Item(20264L, 0, 1, "項鍊"), new Item(20316L, 0, 1, "腰帶"),
                new Item(20303L, 0, 1, "戒指"), new Item(20303L, 0, 1, "戒指")));
        put(sets, 4, 0, "雙刀型", List.of(
                new Item(84L, 10, 1, "武器"), new Item(20032L, 8, 1, "頭盔"),
                new Item(20132L, 8, 1, "盔甲"), new Item(20070L, 8, 1, "斗篷"),
                new Item(20187L, 8, 1, "手套"), new Item(20195L, 8, 1, "鞋類"),
                new Item(20085L, 8, 1, "衣類"), new Item(20264L, 0, 1, "項鍊"),
                new Item(20316L, 0, 1, "腰帶"), new Item(20303L, 0, 1, "戒指"),
                new Item(20303L, 0, 1, "戒指")));
        put(sets, 4, 1, "鋼爪型", List.of(
                new Item(164L, 10, 1, "武器"), new Item(20032L, 8, 1, "頭盔"),
                new Item(20132L, 8, 1, "盔甲"), new Item(20070L, 8, 1, "斗篷"),
                new Item(20187L, 8, 1, "手套"), new Item(20195L, 8, 1, "鞋類"),
                new Item(20085L, 8, 1, "衣類"), new Item(20264L, 0, 1, "項鍊"),
                new Item(20316L, 0, 1, "腰帶"), new Item(20303L, 0, 1, "戒指"),
                new Item(20303L, 0, 1, "戒指")));
        put(sets, 4, 2, "遠距離型", List.of(
                new Item(193L, 10, 1, "武器"), new Item(20236L, 8, 1, "盾牌"),
                new Item(20032L, 8, 1, "頭盔"), new Item(20132L, 8, 1, "盔甲"),
                new Item(20070L, 8, 1, "斗篷"), new Item(20180L, 8, 1, "手套"),
                new Item(20195L, 8, 1, "鞋子"), new Item(20085L, 8, 1, "衣類"),
                new Item(20256L, 0, 1, "項鍊"), new Item(20316L, 0, 1, "腰帶"),
                new Item(20303L, 0, 1, "戒指"), new Item(20303L, 0, 1, "戒指"),
                new Item(40740L, 0, 2000, "輔助")));
        SETS = Map.copyOf(sets);
    }

    private RentalItemSetCatalog() {}

    static Set require(int characterClass, int selection) {
        Set result = SETS.get(new Key(characterClass, selection));
        if (result == null) throw new IllegalArgumentException("unknown rental item set");
        return result;
    }

    static List<Set> all() {
        return List.copyOf(SETS.values());
    }

    static int choices(int characterClass) {
        return switch (characterClass) {
            case 0, 1, 4 -> 3;
            case 2, 3 -> 2;
            default -> 0;
        };
    }

    private static void put(Map<Key, Set> sets, int characterClass, int selection,
                            String label, List<Item> items) {
        Key key = new Key(characterClass, selection);
        if (sets.put(key, new Set(key, label, items)) != null) {
            throw new IllegalStateException("duplicate rental item set " + key);
        }
    }
}
