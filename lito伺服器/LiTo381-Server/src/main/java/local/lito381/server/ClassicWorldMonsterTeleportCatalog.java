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

/** Selected-database projection of retained {@code L1Npc.is_teleport()}. */
final class ClassicWorldMonsterTeleportCatalog {
    static final int ROW_COUNT = 430;
    static final int TELEPORT_ENABLED_COUNT = 3;
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SOURCE_SHA256 =
            "194CAF0783AF88BC0438C9930B1FF86E7F0584426007FD4AB6DE240E462C5814";
    static final String RESOURCE_SHA256 =
            "5B0E0A2424603EBFCC6F35B4F0D6C3C8437AC117245B4AB04278D52F60719357";
    static final String RETAINED_NPC_INSTANCE_SHA256 =
            "72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2";
    static final String RETAINED_NPC_TABLE_SHA256 =
            "0328255B6B4E70DB1AB7647A8E517F95CE80579F31B03F622DD6BDE0B7A7D076";

    private static final int MAGIC = 0x4C4D5431; // LMT1
    private static final String RESOURCE =
            "/world/classic-world-monster-teleport.lmt.gz";
    private static final String TSV_HEADER = "npc_id\tteleport";
    private static final Map<Integer, Boolean> BY_NPC = load();

    private ClassicWorldMonsterTeleportCatalog() {}

    static boolean canTeleport(ClassicWorldMonsterCatalog.MonsterTemplate template) {
        if (template == null) throw new IllegalArgumentException("missing monster template");
        // Runtime-only quest actors have no selected npc-table projection and
        // therefore cannot inherit a database-only teleport capability.
        return BY_NPC.getOrDefault(template.npcId(), false);
    }

    static Boolean forNpc(int npcId) {
        return BY_NPC.get(npcId);
    }

    static int size() {
        return BY_NPC.size();
    }

    static long enabledCount() {
        return BY_NPC.values().stream().filter(Boolean::booleanValue).count();
    }

    private static Map<Integer, Boolean> load() {
        byte[] packed;
        try (InputStream resource = ClassicWorldMonsterTeleportCatalog.class
                .getResourceAsStream(RESOURCE)) {
            if (resource == null) throw new IOException("missing " + RESOURCE);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError("monster teleport resource hash mismatch");
        }

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid teleport magic");
            int rows = in.readInt();
            String databaseHash = hex(in.readNBytes(32));
            String sourceHash = hex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (rows != ROW_COUNT || !SELECTED_DATABASE_SHA256.equals(databaseHash)
                    || !SOURCE_SHA256.equals(sourceHash)
                    || !SOURCE_SHA256.equals(sha256(table))) {
                throw new IOException("monster teleport source contract changed");
            }
            Map<Integer, Boolean> result = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(table), StandardCharsets.US_ASCII))) {
                if (!TSV_HEADER.equals(reader.readLine())) {
                    throw new IOException("invalid teleport table header");
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 2) throw new IOException("invalid teleport row");
                    int npcId = Integer.parseInt(fields[0]);
                    boolean enabled = flag(fields[1]);
                    if (npcId <= 0 || result.put(npcId, enabled) != null) {
                        throw new IOException("invalid or duplicate teleport NPC " + npcId);
                    }
                }
            }
            if (result.size() != rows
                    || result.values().stream().filter(Boolean::booleanValue).count()
                            != TELEPORT_ENABLED_COUNT) {
                throw new IOException("teleport row count mismatch");
            }
            return Map.copyOf(result);
        } catch (IOException | NumberFormatException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static boolean flag(String value) throws IOException {
        if (value.equals("0")) return false;
        if (value.equals("1")) return true;
        throw new IOException("invalid teleport flag " + value);
    }

    private static String hex(byte[] value) throws IOException {
        if (value.length != 32) throw new IOException("truncated teleport hash");
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
