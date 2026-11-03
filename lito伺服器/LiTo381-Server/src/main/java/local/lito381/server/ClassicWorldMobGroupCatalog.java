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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Hash-locked selected {@code mobgroup} rows and their complete minion templates. */
final class ClassicWorldMobGroupCatalog {
    static final int GROUP_COUNT = 21;
    static final int ROW_COUNT = 28;
    static final int PROJECTED_MINION_POPULATION = 449;
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SOURCE_SHA256 =
            "B4D38FD38334C4B87ABA542ABA565464C3F18447D8E7CFEBF7FD894A21F74DCE";
    static final String RESOURCE_SHA256 =
            "6714A2546F05262045451CF25026AF5E9964420C8CDD2EFC273E0438F2616B86";
    static final String RETAINED_NPC_SOURCE_SHA256 =
            "72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2";
    static final String RETAINED_GROUP_INFO_SOURCE_SHA256 =
            "CFC9ED16CE83FB741F2F34697BFCB8B8FA8F64C2A93CD627F417B7C472EAF9EE";
    static final String RETAINED_GROUP_SPAWN_SOURCE_SHA256 =
            "7A2FADC5F8AA5D2F79747DE3793415A3A9B7AC4D4B405B21B61315AD303A035D";

    private static final int MAGIC = 0x4C4D4731; // LMG1
    private static final String RESOURCE = "/world/classic-world-mob-groups.lmg.gz";
    private static final String TSV_HEADER = String.join("\t",
            "group_id", "remove_group_if_leader_die", "leader_id", "minion_id",
            "minion_count", "name_id", "impl", "gfx_id", "level", "hp", "mp",
            "ac", "str", "con", "dex", "wis", "intel", "mr", "exp", "lawful",
            "size", "weak_attr", "ranged", "passispeed", "atkspeed", "bow_act_id",
            "undead", "poison_atk", "paralysis_atk", "agro", "damage_reduction",
            "hard", "doppel", "boss", "light_size", "change_head");
    private static final Map<Integer, Group> BY_ID = load();

    record Minion(int npcId, int count,
                  ClassicWorldMonsterCatalog.MonsterTemplate template) {
        Minion {
            if (npcId <= 0 || count <= 0 || template == null
                    || template.npcId() != npcId) {
                throw new IllegalArgumentException("invalid selected mob-group minion");
            }
        }
    }

    record Group(int groupId, boolean removeIfLeaderDies, int leaderNpcId,
                 List<Minion> minions) {
        Group {
            if (groupId <= 0 || leaderNpcId <= 0 || minions == null
                    || minions.isEmpty()) {
                throw new IllegalArgumentException("invalid selected mob group");
            }
            minions = List.copyOf(minions);
        }

        int minionsPerLeader() {
            return minions.stream().mapToInt(Minion::count).sum();
        }
    }

    private ClassicWorldMobGroupCatalog() {}

    static Group forId(int groupId) {
        return BY_ID.get(groupId);
    }

    static int size() {
        return BY_ID.size();
    }

    private static Map<Integer, Group> load() {
        byte[] packed;
        try (InputStream resource = ClassicWorldMobGroupCatalog.class
                .getResourceAsStream(RESOURCE)) {
            if (resource == null) throw new IOException("missing " + RESOURCE);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError("mob-group resource hash mismatch");
        }

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid mob-group magic");
            int groups = in.readInt();
            int rows = in.readInt();
            int projectedPopulation = in.readInt();
            String databaseHash = hex(in.readNBytes(32));
            String sourceHash = hex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (groups != GROUP_COUNT || rows != ROW_COUNT
                    || projectedPopulation != PROJECTED_MINION_POPULATION
                    || !SELECTED_DATABASE_SHA256.equals(databaseHash)
                    || !SOURCE_SHA256.equals(sourceHash)
                    || !SOURCE_SHA256.equals(sha256(table))) {
                throw new IOException("mob-group source contract changed");
            }

            record Builder(boolean remove, int leader, List<Minion> minions) {}
            Map<Integer, Builder> builders = new HashMap<>();
            int observedRows = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
                if (!TSV_HEADER.equals(reader.readLine())) {
                    throw new IOException("invalid mob-group table header");
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] field = line.split("\t", -1);
                    if (field.length != 36) {
                        throw new IOException("invalid mob-group row");
                    }
                    int groupId = integer(field, 0);
                    boolean remove = flag(field, 1);
                    int leader = integer(field, 2);
                    int minionId = integer(field, 3);
                    ClassicWorldMonsterCatalog.MonsterTemplate template =
                            new ClassicWorldMonsterCatalog.MonsterTemplate(
                                    minionId, field[5], field[6], integer(field, 7),
                                    integer(field, 8), integer(field, 9), integer(field, 10),
                                    integer(field, 11), integer(field, 12), integer(field, 13),
                                    integer(field, 14), integer(field, 15), integer(field, 16),
                                    integer(field, 17), longInteger(field, 18), integer(field, 19),
                                    field[20], integer(field, 21), integer(field, 22),
                                    integer(field, 23), integer(field, 24), integer(field, 25),
                                    integer(field, 26), integer(field, 27), integer(field, 28),
                                    flag(field, 29), integer(field, 30), flag(field, 31),
                                    flag(field, 32), flag(field, 33), integer(field, 34),
                                    flag(field, 35));
                    Builder builder = builders.get(groupId);
                    if (builder == null) {
                        builder = new Builder(remove, leader, new ArrayList<>());
                        builders.put(groupId, builder);
                    } else if (builder.remove() != remove || builder.leader() != leader) {
                        throw new IOException("inconsistent mob-group metadata " + groupId);
                    }
                    builder.minions().add(new Minion(
                            minionId, integer(field, 4), template));
                    observedRows++;
                }
            }
            Map<Integer, Group> result = new HashMap<>();
            builders.forEach((groupId, builder) -> result.put(groupId,
                    new Group(groupId, builder.remove(), builder.leader(),
                            builder.minions())));
            if (observedRows != ROW_COUNT || result.size() != GROUP_COUNT) {
                throw new IOException("mob-group row count mismatch");
            }
            return Map.copyOf(result);
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load selected mob groups: " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static int integer(String[] field, int index) {
        return Integer.parseInt(field[index]);
    }

    private static long longInteger(String[] field, int index) {
        return Long.parseLong(field[index]);
    }

    private static boolean flag(String[] field, int index) throws IOException {
        int value = integer(field, index);
        if (value == 0) return false;
        if (value == 1) return true;
        throw new IOException("invalid mob-group flag " + value);
    }

    private static String hex(byte[] value) throws IOException {
        if (value.length != 32) throw new IOException("truncated mob-group hash");
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
