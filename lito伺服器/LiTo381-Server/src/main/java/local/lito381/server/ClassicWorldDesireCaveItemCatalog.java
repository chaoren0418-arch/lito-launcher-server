package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Exact selected item-wire closure for Desire-Cave quests and rewards. */
final class ClassicWorldDesireCaveItemCatalog {
    static final int ITEM_COUNT = 54;
    static final String SOURCE_SHA256 =
            "0506E72527D7768AAA9C7619FB7ED0B960D1E4A5EB4F8FC3A64C467AD34F786A";
    static final String RESOURCE_SHA256 =
            "05BB493FAAB89FAD58ED528DC0C1AF774A434F57401EE7A80A28535EDC49B8A6";

    private static final int MAGIC = 0x4C574931; // LWI1
    private static final String ITEM_HEADER = String.join("\t",
            "item_id", "kind", "name_id", "use_token", "inventory_gfx",
            "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
            "item_description_id", "weapon_type", "damage_small", "damage_large",
            "material", "safe_enchant", "class_mask");
    private static final Map<Integer, ClassicWorldDropCatalog.ItemWire> ITEMS = load();

    private ClassicWorldDesireCaveItemCatalog() {}

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        ClassicWorldDropCatalog.ItemWire wire = ITEMS.get(itemId);
        return wire == null ? null : wire.inventoryDefinition();
    }

    static ClassicWorldDropCatalog.ItemWire item(int itemId) {
        return ITEMS.get(itemId);
    }

    static Map<Integer, ClassicWorldDropCatalog.ItemWire> items() {
        return ITEMS;
    }

    private static Map<Integer, ClassicWorldDropCatalog.ItemWire> load() {
        byte[] packed;
        try (var resource = ClassicWorldDesireCaveItemCatalog.class
                .getResourceAsStream("/world/desire-cave-items.lwi.gz")) {
            if (resource == null) throw new IOException("missing Desire-Cave item resource");
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError(
                    "Desire-Cave item resource hash mismatch");
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid Desire-Cave item magic");
            int expectedCount = in.readInt();
            byte[] sourceHash = in.readNBytes(32);
            if (sourceHash.length != 32) throw new IOException("truncated item source hash");
            byte[] table = in.readAllBytes();
            String headerHash = HexFormat.of().withUpperCase().formatHex(sourceHash);
            if (expectedCount != ITEM_COUNT || !SOURCE_SHA256.equals(headerHash)
                    || !headerHash.equals(sha256(table))) {
                throw new IOException("Desire-Cave item source contract changed");
            }
            Map<Integer, ClassicWorldDropCatalog.ItemWire> result = parse(table);
            if (result.size() != expectedCount) {
                throw new IOException("Desire-Cave item row count changed");
            }
            return Map.copyOf(result);
        } catch (IOException | IllegalArgumentException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static Map<Integer, ClassicWorldDropCatalog.ItemWire> parse(byte[] table)
            throws IOException {
        Map<Integer, ClassicWorldDropCatalog.ItemWire> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!ITEM_HEADER.equals(reader.readLine())) {
                throw new IOException("Desire-Cave item TSV header changed");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] field = line.split("\t", -1);
                if (field.length != 17) throw new IOException("invalid Desire-Cave item row");
                ClassicWorldDropCatalog.ItemWire wire =
                        new ClassicWorldDropCatalog.ItemWire(
                                integer(field, 0), field[1], field[2], field[3],
                                integer(field, 4), integer(field, 5), integer(field, 6),
                                bool(field, 7), bool(field, 8), integer(field, 9),
                                integer(field, 10), field[11], integer(field, 12),
                                integer(field, 13), field[14], integer(field, 15),
                                integer(field, 16));
                if (result.put(wire.itemId(), wire) != null) {
                    throw new IOException("duplicate Desire-Cave item id");
                }
            }
        }
        return result;
    }

    private static int integer(String[] field, int index) {
        return Integer.parseInt(field[index]);
    }

    private static boolean bool(String[] field, int index) {
        int value = integer(field, index);
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("non-boolean Desire-Cave item field");
        }
        return value == 1;
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
