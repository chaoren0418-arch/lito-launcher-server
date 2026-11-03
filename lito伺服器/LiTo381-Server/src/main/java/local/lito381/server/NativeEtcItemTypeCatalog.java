package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Hash-locked retained {@code etcitem.item_type} subset used by NPC AI. */
final class NativeEtcItemTypeCatalog {
    static final int POTION = 6;
    static final int FOOD = 7;
    static final int POTION_COUNT = 126;
    static final int FOOD_COUNT = 98;
    static final String SOURCE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String RESOURCE_SHA256 =
            "FC0DBEB20E15109889422A84C1521BD2A6BB095516847E54415C0F898EEEE924";
    private static final String RESOURCE = "/world/native-etcitem-types.tsv.gz";
    private static final Map<Long, Integer> TYPES = load();

    record ResourceContract(int potionCount, int foodCount,
                            String sourceSha256, String resourceSha256) {}

    private NativeEtcItemTypeCatalog() {}

    static int typeOf(long catalogId) {
        return TYPES.getOrDefault(catalogId, -1);
    }

    static boolean airbornePickupEligible(InventoryCatalog.Definition definition) {
        if (definition == null) return false;
        int type = typeOf(definition.catalogId());
        return type == POTION || type == FOOD;
    }

    static ResourceContract resourceContract() {
        return new ResourceContract(
                POTION_COUNT, FOOD_COUNT, SOURCE_SHA256, RESOURCE_SHA256);
    }

    private static Map<Long, Integer> load() {
        byte[] packed;
        try (InputStream resource = NativeEtcItemTypeCatalog.class
                .getResourceAsStream(RESOURCE)) {
            if (resource == null) {
                throw new IOException("missing native etcitem type resource");
            }
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError(
                    "native etcitem type resource hash changed");
        }

        Map<Long, Integer> result = new LinkedHashMap<>();
        int potionCount = 0;
        int foodCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ByteArrayInputStream(packed)),
                StandardCharsets.UTF_8))) {
            if (!"LITO_NATIVE_ETCITEM_TYPES_1".equals(reader.readLine())) {
                throw new IOException("native etcitem type magic changed");
            }
            String source = reader.readLine();
            if (!String.valueOf("source_sha256\t" + SOURCE_SHA256).equals(source)) {
                throw new IOException("native etcitem type source hash changed");
            }
            if (!"item_id\titem_type".equals(reader.readLine())) {
                throw new IOException("native etcitem type header changed");
            }
            long prior = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 2) {
                    throw new IOException("native etcitem type row changed");
                }
                long itemId = Long.parseLong(fields[0]);
                int type = Integer.parseInt(fields[1]);
                if (itemId <= prior || type != POTION && type != FOOD
                        || result.put(itemId, type) != null) {
                    throw new IOException("invalid native etcitem type row");
                }
                prior = itemId;
                if (type == POTION) potionCount++;
                else foodCount++;
            }
        } catch (IOException | NumberFormatException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (potionCount != POTION_COUNT || foodCount != FOOD_COUNT) {
            throw new ExceptionInInitializerError(
                    "native etcitem type count changed");
        }
        return Map.copyOf(result);
    }

    private static String sha256(byte[] data) {
        try {
            return java.util.HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
