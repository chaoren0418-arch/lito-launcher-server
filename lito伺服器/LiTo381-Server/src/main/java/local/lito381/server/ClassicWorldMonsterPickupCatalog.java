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
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Selected {@code npc.picupitem} projection used by retained monster AI. */
final class ClassicWorldMonsterPickupCatalog {
    static final int ROW_COUNT = 68;
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SOURCE_SHA256 =
            "76EC5C9E02D65E2C0C1A0CB719C8215BF8D302AC2660048B3A38F58E2AB2A250";
    static final String RESOURCE_SHA256 =
            "A1F83383CA5C2A6C3CB0168EDFB8183F2F143E11A5D40EF4CB20C6E345B9728C";
    static final int MAX_NPC_ITEMS = 8;
    static final int MAX_NPC_WEIGHT = 1500;

    private static final int MAGIC = 0x4C575031; // LWP1
    private static final String RESOURCE = "/world/classic-monster-pickup.lwp.gz";
    private static final String TSV_HEADER = "npc_id\tpickup_item";
    private static final Set<Integer> PICKUP_NPCS = load();

    private ClassicWorldMonsterPickupCatalog() {}

    static boolean enabled(int npcId) {
        return PICKUP_NPCS.contains(npcId);
    }

    static boolean enabled(ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return template != null && enabled(template.npcId());
    }

    static int size() {
        return PICKUP_NPCS.size();
    }

    private static Set<Integer> load() {
        byte[] packed;
        try (InputStream resource = ClassicWorldMonsterPickupCatalog.class
                .getResourceAsStream(RESOURCE)) {
            if (resource == null) throw new IOException("missing " + RESOURCE);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError("monster pickup resource hash mismatch");
        }

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid pickup magic");
            int rows = in.readInt();
            String sourceHash = hex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (rows != ROW_COUNT || !SOURCE_SHA256.equals(sourceHash)
                    || !SOURCE_SHA256.equals(sha256(table))) {
                throw new IOException("monster pickup source contract changed");
            }
            Set<Integer> result = new LinkedHashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(table), StandardCharsets.US_ASCII))) {
                if (!TSV_HEADER.equals(reader.readLine())) {
                    throw new IOException("invalid monster pickup table header");
                }
                int previous = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 2 || !fields[1].equals("1")) {
                        throw new IOException("invalid monster pickup row");
                    }
                    int npcId = Integer.parseInt(fields[0]);
                    if (npcId <= previous || !result.add(npcId)) {
                        throw new IOException("unordered monster pickup NPC " + npcId);
                    }
                    previous = npcId;
                }
            }
            if (result.size() != rows) {
                throw new IOException("monster pickup row count mismatch");
            }
            return Set.copyOf(result);
        } catch (IOException | NumberFormatException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String hex(byte[] value) throws IOException {
        if (value.length != 32) throw new IOException("truncated pickup source hash");
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
