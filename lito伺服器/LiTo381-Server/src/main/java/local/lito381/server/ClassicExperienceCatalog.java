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
import java.util.HexFormat;
import java.util.List;
import java.util.zip.GZIPInputStream;

/** Hash-locked selected-source classic experience thresholds and penalties. */
final class ClassicExperienceCatalog {
    static final int ROW_COUNT = 101;
    static final int MAXIMUM_LEVEL = 99;
    static final String SOURCE_SHA256 =
            "3FF1962B807E29AA8D29F7067C2419A1BFCC71017E4710508B8101BB0458E1D2";
    static final String RESOURCE_SHA256 =
            "16BD81D9BE429D7B8DAEFE34F8CD0E20CC8CC1F30EB3A9C03318C2942A4B9BB7";

    private static final int MAGIC = 0x4C455831; // LEX1
    private static final String TSV_HEADER =
            "level\texp\texp_penalty\tdeath_penalty_rate";
    private static final List<Row> ROWS = load();

    record Row(int sourceLevel, long cumulativeExperience, double experiencePenalty,
               double deathPenaltyRate) {
        Row {
            if (sourceLevel < 0 || cumulativeExperience < 0
                    || !Double.isFinite(experiencePenalty) || experiencePenalty <= 0
                    || !Double.isFinite(deathPenaltyRate)
                    || deathPenaltyRate < 0 || deathPenaltyRate > 1) {
                throw new IllegalArgumentException("invalid classic experience row");
            }
        }
    }

    private ClassicExperienceCatalog() {}

    static List<Row> rows() {
        return ROWS;
    }

    /** Retained ExpTable.getExpByLevel: database row {@code level - 1}. */
    static long minimumExperienceForLevel(int level) {
        if (level < 1 || level > MAXIMUM_LEVEL) {
            throw new IllegalArgumentException("invalid classic level " + level);
        }
        return ROWS.get(level - 1).cumulativeExperience();
    }

    /** Retained ExpTable.getPenaltyRate: database row {@code level}. */
    static double penaltyRate(int level) {
        if (level < 1 || level > MAXIMUM_LEVEL) {
            throw new IllegalArgumentException("invalid classic level " + level);
        }
        return 1.0D / ROWS.get(level).experiencePenalty();
    }

    /** Selected exp.death_penalty_rate indexed by the current level row. */
    static double deathPenaltyRate(int level) {
        if (level < 1 || level > MAXIMUM_LEVEL) {
            throw new IllegalArgumentException("invalid classic level " + level);
        }
        return ROWS.get(level).deathPenaltyRate();
    }

    /** Retained ExpTable.getNeedExpNextLevel. */
    static long experienceNeededForNextLevel(int level) {
        if (level < 1 || level > MAXIMUM_LEVEL) {
            throw new IllegalArgumentException("invalid classic level " + level);
        }
        return ROWS.get(level).cumulativeExperience()
                - ROWS.get(level - 1).cumulativeExperience();
    }

    /** Retained L1PcInstance.deathPenalty: one Java narrowing cast. */
    static long deathPenalty(int level) {
        return (long) (experienceNeededForNextLevel(level) * deathPenaltyRate(level));
    }

    static long adjustedReward(int level, long baseExperience) {
        return adjustedReward(level, baseExperience, 1.0D);
    }

    /** Exact CalcExp order: shared server multiplier, then level penalty, one cast. */
    static long adjustedReward(int level, long baseExperience, double sharedRate) {
        if (baseExperience < 0) {
            throw new IllegalArgumentException("negative classic experience reward");
        }
        if (!Double.isFinite(sharedRate) || sharedRate < 0.0D) {
            throw new IllegalArgumentException("invalid classic experience rate");
        }
        if (level >= MAXIMUM_LEVEL) return 0;
        return (long) (baseExperience * sharedRate * penaltyRate(level));
    }

    static int levelForExperience(long experience) {
        if (experience < 0 || experience > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid classic experience " + experience);
        }
        int level = 1;
        for (int candidate = 2; candidate <= MAXIMUM_LEVEL; candidate++) {
            if (experience < minimumExperienceForLevel(candidate)) break;
            level = candidate;
        }
        return level;
    }

    static boolean isConsistent(int level, long experience) {
        if (level < 1 || level > MAXIMUM_LEVEL
                || experience < 0 || experience > 0xFFFF_FFFFL) return false;
        if (experience < minimumExperienceForLevel(level)) return false;
        return level == MAXIMUM_LEVEL
                || experience < minimumExperienceForLevel(level + 1);
    }

    private static List<Row> load() {
        byte[] packed;
        try (InputStream resource = ClassicExperienceCatalog.class
                .getResourceAsStream("/world/classic-experience.lex.gz")) {
            if (resource == null) throw new IOException("missing classic experience resource");
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError("classic experience resource hash changed");
        }

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid experience magic");
            int expectedRows = in.readUnsignedShort();
            int maximumLevel = in.readUnsignedShort();
            byte[] sourceHash = in.readNBytes(32);
            if (sourceHash.length != 32) throw new IOException("truncated experience hash");
            byte[] table = in.readAllBytes();
            String observedSource = sha256(table);
            String headerSource = HexFormat.of().withUpperCase().formatHex(sourceHash);
            if (!observedSource.equals(headerSource) || !SOURCE_SHA256.equals(headerSource)
                    || expectedRows != ROW_COUNT || maximumLevel != MAXIMUM_LEVEL) {
                throw new IOException("classic experience source contract changed");
            }
            List<Row> result = parse(table);
            if (result.size() != expectedRows) {
                throw new IOException("classic experience row count changed");
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load classic experience resource: " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static List<Row> parse(byte[] table) throws IOException {
        List<Row> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.US_ASCII))) {
            if (!TSV_HEADER.equals(reader.readLine())) {
                throw new IOException("classic experience TSV header changed");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] field = line.split("\t", -1);
                if (field.length != 4) throw new IOException("invalid experience row");
                int sourceLevel = Integer.parseInt(field[0]);
                if (sourceLevel != result.size()) {
                    throw new IOException("experience levels are not contiguous");
                }
                result.add(new Row(sourceLevel, Long.parseLong(field[1]),
                        Double.parseDouble(field[2]), Double.parseDouble(field[3])));
            }
        }
        return result;
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
