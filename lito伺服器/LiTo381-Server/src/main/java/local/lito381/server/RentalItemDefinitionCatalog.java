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

/** Exact selected-3.80 templates for every weapon/armor identity in LTItemSet. */
final class RentalItemDefinitionCatalog {
    private static final String RESOURCE =
            "/rental/rental-item-definitions-380.tsv";
    private static final String RESOURCE_SHA256 =
            "DD090087A581F84CEAF61EA22ED5B5DC71BE059C836B853457B5A589B0A5807C";
    private static final int EXPECTED_WEAPONS = 13;
    private static final int EXPECTED_ARMORS = 44;

    /**
     * Full selected database row subset. Fields not rendered by the native
     * detail pane remain available to later combat/equipment consumers rather
     * than being discarded while the inventory definition is constructed.
     */
    record SourceTemplate(String kind, String selectedName,
                          InventoryCatalog.Definition definition,
                          Map<String, String> fields) {
        SourceTemplate {
            if ((!"weapon".equals(kind) && !"armor".equals(kind))
                    || selectedName == null || selectedName.isBlank()
                    || definition == null || fields == null || fields.isEmpty()) {
                throw new IllegalArgumentException("invalid rental source template");
            }
            fields = Map.copyOf(fields);
        }

        int integer(String name) {
            String value = fields.get(name);
            if (value == null) throw new IllegalArgumentException(
                    "unknown rental source field " + name);
            return Integer.parseInt(value);
        }

        double decimal(String name) {
            String value = fields.get(name);
            if (value == null) throw new IllegalArgumentException(
                    "unknown rental source field " + name);
            return Double.parseDouble(value);
        }
    }

    private static final Map<Integer, SourceTemplate> ITEMS = load();

    private RentalItemDefinitionCatalog() {}

    static InventoryCatalog.Definition find(int itemId) {
        SourceTemplate source = ITEMS.get(itemId);
        return source == null ? null : source.definition();
    }

    static SourceTemplate source(int itemId) {
        return ITEMS.get(itemId);
    }

    static List<SourceTemplate> all() {
        return List.copyOf(ITEMS.values());
    }

    private static Map<Integer, SourceTemplate> load() {
        try {
            byte[] bytes;
            try (InputStream input = RentalItemDefinitionCatalog.class
                    .getResourceAsStream(RESOURCE)) {
                if (input == null) throw new IOException("missing " + RESOURCE);
                bytes = input.readAllBytes();
            }
            if (!RESOURCE_SHA256.equals(sha256(bytes))) {
                throw new IOException("rental definition resource hash changed");
            }
            Map<Integer, SourceTemplate> result = new LinkedHashMap<>();
            int weapons = 0;
            int armors = 0;
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
                            throw new IOException("unknown rental definition section " + line);
                        }
                        header = null;
                        continue;
                    }
                    if (section == null) throw new IOException(
                            "rental definition row before section");
                    String[] values = line.split("\\t", -1);
                    if (header == null) {
                        header = values;
                        continue;
                    }
                    if (values.length != header.length) {
                        throw new IOException("rental definition field count changed");
                    }
                    Map<String, String> fields = new LinkedHashMap<>();
                    for (int index = 0; index < header.length; index++) {
                        if (fields.putIfAbsent(header[index], values[index]) != null) {
                            throw new IOException("duplicate rental definition field");
                        }
                    }
                    SourceTemplate source = section.equals("weapon")
                            ? weapon(fields) : armor(fields);
                    int itemId = Math.toIntExact(source.definition().catalogId());
                    if (result.putIfAbsent(itemId, source) != null) {
                        throw new IOException("duplicate rental definition " + itemId);
                    }
                    if (section.equals("weapon")) weapons++;
                    else armors++;
                }
            }
            if (weapons != EXPECTED_WEAPONS || armors != EXPECTED_ARMORS
                    || result.size() != EXPECTED_WEAPONS + EXPECTED_ARMORS) {
                throw new IOException("rental definition row count changed");
            }
            return Map.copyOf(result);
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load exact rental item definitions: " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static SourceTemplate weapon(Map<String, String> fields) {
        int itemId = integer(fields, "item_id");
        int material = ClassicWorldDropCatalog.materialCode(fields.get("material"));
        int weight = integer(fields, "weight");
        InventoryCatalog.NativeWeaponStatus status =
                new InventoryCatalog.NativeWeaponStatus(
                        integer(fields, "dmg_small"), integer(fields, "dmg_large"),
                        material, weight, integer(fields, "class_mask"),
                        integer(fields, "safenchant"), integer(fields, "hitmodifier"),
                        integer(fields, "dmgmodifier"));
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                itemId, 1, integer(fields, "invgfx"), integer(fields, "grdgfx"),
                integer(fields, "bless"), false, fields.get("name_id"),
                fields.get("name_id"), 0, 0, false,
                integer(fields, "tradable") == 1,
                ClassicWorldDropCatalog.weaponWalkAction("weapon", fields.get("type")),
                0, InventoryCatalog.Effect.NONE, integer(fields, "itemdesc_id"),
                weight, 0, status, null);
        return new SourceTemplate("weapon", fields.get("name"), definition, fields);
    }

    private static SourceTemplate armor(Map<String, String> fields) {
        int itemId = integer(fields, "item_id");
        int weight = integer(fields, "weight");
        String type = fields.get("type");
        int useType = ClassicWorldDropCatalog.nativeUseType("armor", type);
        InventoryCatalog.NativeArmorStatus status =
                new InventoryCatalog.NativeArmorStatus(
                        Math.negateExact(integer(fields, "ac")),
                        ClassicWorldDropCatalog.materialCode(fields.get("material")),
                        weight, integer(fields, "class_mask"),
                        integer(fields, "safenchant"),
                        integer(fields, "add_str"), integer(fields, "add_dex"),
                        integer(fields, "add_con"), integer(fields, "add_wis"),
                        integer(fields, "add_int"), integer(fields, "add_cha"),
                        integer(fields, "add_hp"), integer(fields, "add_mp"),
                        integer(fields, "m_def"), integer(fields, "add_sp"),
                        integer(fields, "haste_item") == 1,
                        integer(fields, "defense_fire"),
                        integer(fields, "defense_water"),
                        integer(fields, "defense_wind"),
                        integer(fields, "defense_earth"),
                        integer(fields, "regist_blind"),
                        integer(fields, "regist_sleep"),
                        integer(fields, "regist_freeze"),
                        integer(fields, "regist_stone"),
                        integer(fields, "regist_sustain"),
                        integer(fields, "regist_stun"),
                        ClassicWorldDropCatalog.armorTypeCode(type));
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                itemId, useType, integer(fields, "invgfx"),
                integer(fields, "grdgfx"), integer(fields, "bless"), false,
                fields.get("name_id"), fields.get("name_id"), 0, 0, false,
                integer(fields, "tradable") == 1, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, integer(fields, "itemdesc_id"),
                weight, integer(fields, "defense_fire"), null, status);
        return new SourceTemplate("armor", fields.get("name"), definition, fields);
    }

    private static int integer(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null) throw new IllegalArgumentException(
                "missing rental definition field " + name);
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
