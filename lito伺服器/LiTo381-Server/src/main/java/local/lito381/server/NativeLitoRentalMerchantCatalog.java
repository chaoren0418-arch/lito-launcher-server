package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selected-client identities plus the preserved 2010 LiTo merchant catalog.
 *
 * <p>The names and special icon indices are exact {@code LTdesc-c.tbl}
 * resources. Prices and equipment templates come from a preserved 2010
 * L1J server package whose shop rows use Light Gem item 49358 for LiTo NPCs
 * 71286/71287. That package is a retained compatibility source, not an
 * original official-server capture; the distinction is deliberately exposed
 * by {@link #HISTORICAL_OFFICIAL_EXACT}.</p>
 */
final class NativeLitoRentalMerchantCatalog {
    static final boolean HISTORICAL_OFFICIAL_EXACT = false;
    static final boolean RETAINED_SERVER_COMPATIBILITY_AVAILABLE = true;
    static final String RETAINED_ARCHIVE_SHA256 =
            "C180BB0356E25AD3530A632423A09593F453381A45290626648C8A488AB9C9F2";
    static final String RETAINED_DATABASE_SHA256 =
            "2877AF741B1B1B467FF1DF6F881DF0F8741F0CA6B304003706ACF1566959E804";
    static final int LIGHT_GEM_SOURCE_ITEM_ID = 49358;
    static final int WEAPON_SOURCE_NPC_ID = 71286;
    static final int ARMOR_SOURCE_NPC_ID = 71287;
    static final int RENTAL_SECONDS = 3_600;

    private static final String RESOURCE =
            "/rental/retained-lito-rental-merchant-2010.tsv";
    private static final String RESOURCE_SHA256 =
            "D1944B0B62C35F3ABC59B8B10489AC90711DB9010B1BB7B6F8275664B4BF0EB9";

    record Item(int resourceIndex, int sourceItemId, int maximumSeconds,
                long lightGemPrice, InventoryCatalog.Definition definition) {
        Item {
            if (resourceIndex < 13 || resourceIndex > 90
                    || sourceItemId <= 0 || maximumSeconds != RENTAL_SECONDS
                    || lightGemPrice <= 0 || definition == null
                    || definition.catalogId() != sourceItemId
                    || definition.icon() != Protocol.LITO_RESOURCE_WIRE_BASE + resourceIndex
                    || !definition.identified() || definition.tradable()
                    || !definition.cantDelete()) {
                throw new IllegalArgumentException("invalid retained LiTo rental item");
            }
        }

        byte[] buyListDetail() {
            return new RuntimeInventory.Item(
                    1L, definition, 1, false, 0, 0,
                    true, RuntimeInventory.BLESSING_NORMAL).nativeStatusDetail();
        }
    }

    private static final List<Item> ITEMS = load();
    private static final Map<Integer, Item> BY_SOURCE_ID = bySourceId();

    private NativeLitoRentalMerchantCatalog() {}

    /**
     * Retained functional split for the four selected-client merchants:
     * Maya=weapons, Fifo=body equipment, Kri=shield/cloak/shirt, Love=accessory.
     * The item identities/prices are retained-source exact; this four-way
     * grouping is server policy because the surviving 2010 package had only
     * one combined armor merchant.
     */
    static List<Item> offeredBy(int merchantResourceIndex) {
        int first;
        int last;
        switch (merchantResourceIndex) {
            case NativeLitoRentalTownNpcCatalog.MAYA_RESOURCE_INDEX -> {
                first = 13; last = 37;
            }
            case NativeLitoRentalTownNpcCatalog.FIFO_RESOURCE_INDEX -> {
                first = 38; last = 61;
            }
            case NativeLitoRentalTownNpcCatalog.KRI_RESOURCE_INDEX -> {
                first = 62; last = 73;
            }
            case NativeLitoRentalTownNpcCatalog.LOVE_RESOURCE_INDEX -> {
                first = 74; last = 90;
            }
            default -> { return List.of(); }
        }
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS) {
            if (item.resourceIndex() >= first && item.resourceIndex() <= last) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    static Item findBySourceItemId(long sourceItemId) {
        if (sourceItemId <= 0 || sourceItemId > Integer.MAX_VALUE) return null;
        return BY_SOURCE_ID.get((int) sourceItemId);
    }

    static List<Item> all() {
        return ITEMS;
    }

    private static Map<Integer, Item> bySourceId() {
        Map<Integer, Item> result = new LinkedHashMap<>();
        for (Item item : ITEMS) {
            if (result.putIfAbsent(item.sourceItemId(), item) != null) {
                throw new ExceptionInInitializerError(
                        "duplicate retained LiTo rental item id " + item.sourceItemId());
            }
        }
        return Map.copyOf(result);
    }

    private static List<Item> load() {
        try {
            byte[] bytes;
            try (InputStream input = NativeLitoRentalMerchantCatalog.class
                    .getResourceAsStream(RESOURCE)) {
                if (input == null) throw new IOException("missing " + RESOURCE);
                bytes = input.readAllBytes();
            }
            if (!RESOURCE_SHA256.equals(sha256(bytes))) {
                throw new IOException("retained LiTo rental catalog hash changed");
            }
            List<Item> result = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
                String section = null;
                String[] header = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (line.startsWith("[") && line.endsWith("]")) {
                        section = line.substring(1, line.length() - 1);
                        if (!section.equals("weapon") && !section.equals("armor")) {
                            throw new IOException("unknown retained LiTo section " + section);
                        }
                        header = null;
                        continue;
                    }
                    if (section == null) throw new IOException("row before section");
                    String[] values = line.split("\\t", -1);
                    if (header == null) {
                        header = values;
                        continue;
                    }
                    if (values.length != header.length) {
                        throw new IOException("retained LiTo field count changed");
                    }
                    Map<String, String> fields = new LinkedHashMap<>();
                    for (int index = 0; index < header.length; index++) {
                        if (fields.putIfAbsent(header[index], values[index]) != null) {
                            throw new IOException("duplicate retained LiTo field");
                        }
                    }
                    result.add(section.equals("weapon")
                            ? weapon(fields) : armor(fields));
                }
            }
            if (result.size() != 78) throw new IOException("rental row count changed");
            for (int index = 0; index < result.size(); index++) {
                if (result.get(index).resourceIndex() != index + 13) {
                    throw new IOException("rental resource order changed");
                }
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load retained LiTo rental merchant catalog: "
                            + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static Item weapon(Map<String, String> fields) {
        int resourceIndex = integer(fields, "resource_index");
        int sourceItemId = integer(fields, "source_item_id");
        int unitWeight = integer(fields, "weight");
        InventoryCatalog.NativeWeaponStatus status =
                new InventoryCatalog.NativeWeaponStatus(
                        integer(fields, "damage_small"), integer(fields, "damage_large"),
                        ClassicWorldDropCatalog.materialCode(fields.get("material")),
                        unitWeight, integer(fields, "class_mask"),
                        integer(fields, "safe_enchant"),
                        integer(fields, "hit_modifier"),
                        integer(fields, "damage_modifier"));
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                sourceItemId, 1, Protocol.LITO_RESOURCE_WIRE_BASE + resourceIndex,
                integer(fields, "ground_gfx"), RuntimeInventory.BLESSING_NORMAL,
                true, fields.get("selected_name"), fields.get("selected_name"),
                0, 0, false, false,
                ClassicWorldDropCatalog.weaponWalkAction("weapon", fields.get("type")),
                0, InventoryCatalog.Effect.NONE, 0, unitWeight, 0,
                status, null, true);
        return new Item(resourceIndex, sourceItemId,
                integer(fields, "maximum_seconds"),
                integer(fields, "light_gem_price"), definition);
    }

    private static Item armor(Map<String, String> fields) {
        int resourceIndex = integer(fields, "resource_index");
        int sourceItemId = integer(fields, "source_item_id");
        int unitWeight = integer(fields, "weight");
        String type = fields.get("type");
        int fire = integer(fields, "fire");
        InventoryCatalog.NativeArmorStatus status =
                new InventoryCatalog.NativeArmorStatus(
                        integer(fields, "armor_class"),
                        ClassicWorldDropCatalog.materialCode(fields.get("material")),
                        unitWeight, integer(fields, "class_mask"),
                        integer(fields, "safe_enchant"),
                        integer(fields, "strength"), integer(fields, "dexterity"),
                        integer(fields, "constitution"), integer(fields, "wisdom"),
                        integer(fields, "intelligence"), integer(fields, "charisma"),
                        integer(fields, "hit_points"), integer(fields, "mana_points"),
                        integer(fields, "magic_resistance"),
                        integer(fields, "spell_power"), integer(fields, "haste") == 1,
                        fire, integer(fields, "water"), integer(fields, "wind"),
                        integer(fields, "earth"), integer(fields, "blind"),
                        integer(fields, "sleep"), integer(fields, "freeze"),
                        integer(fields, "stone"), integer(fields, "sustain"),
                        integer(fields, "stun"),
                        ClassicWorldDropCatalog.armorTypeCode(type));
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                sourceItemId, ClassicWorldDropCatalog.nativeUseType("armor", type),
                Protocol.LITO_RESOURCE_WIRE_BASE + resourceIndex,
                integer(fields, "ground_gfx"), RuntimeInventory.BLESSING_NORMAL,
                true, fields.get("selected_name"), fields.get("selected_name"),
                0, 0, false, false, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 0, unitWeight, fire,
                null, status, true);
        return new Item(resourceIndex, sourceItemId,
                integer(fields, "maximum_seconds"),
                integer(fields, "light_gem_price"), definition);
    }

    private static int integer(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null) throw new IllegalArgumentException("missing " + name);
        return Integer.parseInt(value);
    }

    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02X", value));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
