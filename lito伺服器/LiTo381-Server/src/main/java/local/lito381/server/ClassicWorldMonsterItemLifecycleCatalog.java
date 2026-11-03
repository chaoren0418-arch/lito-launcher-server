package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Selected npc.digestitem and non-GM NPC-give whitelist projection. */
final class ClassicWorldMonsterItemLifecycleCatalog {
    static final int DIGEST_ROW_COUNT = 430;
    static final int DIGEST_ENABLED_COUNT = 3;
    static final int GIVE_ROW_COUNT = 31;
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SOURCE_SHA256 =
            "88480EEEE56D68112502991A8055B554AC091EE908990B7B8F8E39F81AF36B6D";
    static final String RESOURCE_SHA256 =
            "5BB9656CEEBBBB8EED58EA6845A9CF2501F07F061F31C0109C9BA9DF5374BF1F";
    static final String RETAINED_NPC_INSTANCE_SHA256 =
            "72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2";
    static final String RETAINED_NPC_TABLE_SHA256 =
            "0328255B6B4E70DB1AB7647A8E517F95CE80579F31B03F622DD6BDE0B7A7D076";
    static final String RETAINED_GIVE_HANDLER_SHA256 =
            "21F4BD7FDAE6D6F30284AE0834E105D5158A82FF9C437B369D319D4178758212";
    static final String RETAINED_GIVE_TABLE_SHA256 =
            "D48F6587A2DE18466ADCC8C457160202851D383774DE32FBB055EC1DB4018BF4";
    static final String RETAINED_DIGEST_TIMER_SHA256 =
            "30297C05D6B45F8EAE46A5C738ECF16C75FBA7DF4650B05A3AEB969FEE2ADA2B";

    private static final int MAGIC = 0x4C4D4931; // LMI1
    private static final String RESOURCE =
            "/world/classic-world-monster-item-lifecycle.lmi.gz";
    private static final Tables TABLES = load();

    private ClassicWorldMonsterItemLifecycleCatalog() {}

    static int digestSeconds(ClassicWorldMonsterCatalog.MonsterTemplate template) {
        if (template == null) throw new IllegalArgumentException("missing monster template");
        return TABLES.digestSeconds().getOrDefault(template.npcId(), 0);
    }

    static Integer digestSecondsForNpc(int npcId) {
        return TABLES.digestSeconds().get(npcId);
    }

    /** Returns -1 when absent, zero when unlimited, otherwise the selected maximum. */
    static int giveMaximum(int itemId) {
        return TABLES.giveMaximum().getOrDefault(itemId, -1);
    }

    static int digestSize() {
        return TABLES.digestSeconds().size();
    }

    static int giveSize() {
        return TABLES.giveMaximum().size();
    }

    static long digestEnabledCount() {
        return TABLES.digestSeconds().values().stream()
                .filter(value -> value > 0).count();
    }

    private record Tables(
            Map<Integer, Integer> digestSeconds,
            Map<Integer, Integer> giveMaximum) {}

    private static Tables load() {
        byte[] packed;
        try (InputStream resource = ClassicWorldMonsterItemLifecycleCatalog.class
                .getResourceAsStream(RESOURCE)) {
            if (resource == null) throw new IOException("missing " + RESOURCE);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError(
                    "monster item lifecycle resource hash mismatch");
        }

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid item lifecycle magic");
            int digestRows = in.readInt();
            int giveRows = in.readInt();
            String databaseHash = hex(in.readNBytes(32));
            String sourceHash = hex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (digestRows != DIGEST_ROW_COUNT || giveRows != GIVE_ROW_COUNT
                    || !SELECTED_DATABASE_SHA256.equals(databaseHash)
                    || !SOURCE_SHA256.equals(sourceHash)
                    || !SOURCE_SHA256.equals(sha256(table))) {
                throw new IOException("monster item lifecycle source contract changed");
            }
            Map<Integer, Integer> digest = new HashMap<>();
            Map<Integer, Integer> give = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(table), StandardCharsets.US_ASCII))) {
                requireLine(reader, "[digest]");
                requireLine(reader, "npc_id\tdigest_seconds");
                String line;
                while ((line = reader.readLine()) != null && !line.equals("[give]")) {
                    putRow(digest, line, "digest");
                }
                if (line == null) throw new IOException("missing give section");
                requireLine(reader, "item_id\tmax_count");
                while ((line = reader.readLine()) != null) {
                    putRow(give, line, "give");
                }
            }
            if (digest.size() != digestRows || give.size() != giveRows
                    || digest.values().stream().filter(value -> value > 0).count()
                            != DIGEST_ENABLED_COUNT) {
                throw new IOException("monster item lifecycle row count mismatch");
            }
            return new Tables(Map.copyOf(digest), Map.copyOf(give));
        } catch (IOException | NumberFormatException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static void requireLine(BufferedReader reader, String expected)
            throws IOException {
        if (!expected.equals(reader.readLine())) {
            throw new IOException("invalid monster item lifecycle table header");
        }
    }

    private static void putRow(
            Map<Integer, Integer> target, String line, String section)
            throws IOException {
        if (line == null || line.isEmpty()) {
            throw new IOException("invalid " + section + " row");
        }
        String[] fields = line.split("\t", -1);
        if (fields.length != 2) throw new IOException("invalid " + section + " row");
        int key = Integer.parseInt(fields[0]);
        int value = Integer.parseInt(fields[1]);
        if (key <= 0 || value < 0 || target.put(key, value) != null) {
            throw new IOException("invalid or duplicate " + section + " row " + key);
        }
    }

    private static String hex(byte[] value) throws IOException {
        if (value.length != 32) throw new IOException("truncated item lifecycle hash");
        return HexFormat.of().withUpperCase().formatHex(value);
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
