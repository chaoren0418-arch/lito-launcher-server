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
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Selected-database projection of retained L1MonsterInstance.searchTarget(). */
final class ClassicWorldMonsterAggressionCatalog {
    static final int ROW_COUNT = 430;
    static final int LOS_REQUIRED_BEYOND_TILES = 12;
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SOURCE_SHA256 =
            "A67A188562AE08DD4FBBCFDDFDBEA7F12F4AC8F94E3F8E52E047BBFD3104767C";
    static final String RESOURCE_SHA256 =
            "E90ECA3FD7B145AA53BF7B5A7CB9F8AD56752637BA7E3AE44E35C4F612F721BE";
    static final String RETAINED_MONSTER_SOURCE_SHA256 =
            "B6A8694D897FC28F8BD7779541E78778D99E8E13842DB6175D58CB57CA2351A4";
    static final String RETAINED_OTHER_CONFIG_SHA256 =
            "0B8140627253EEC8E03B771C51B508AE651ACC374EE19145BC0EF006206CE3E5";

    private static final int MAGIC = 0x4C4D4131; // LMA1
    private static final String RESOURCE =
            "/world/classic-world-monster-aggression.lma.gz";
    private static final String TSV_HEADER = String.join("\t",
            "npc_id", "agro", "agrososc", "agrocoi", "family",
            "agrofamily", "agrogfxid1", "agrogfxid2", "karma");
    private static final Set<Integer> ARENA_MAPS = Set.of(88, 91, 92, 95, 98);
    private static final Map<Integer, Profile> BY_NPC = load();

    enum Acquisition {
        NONE,
        ARENA,
        KURTZ_CROWN,
        KURTZ_DARK_ELF,
        CHAOTIC_PLAYER,
        SHAPE_CHANGE,
        AGGRESSIVE,
        BASE_GFX_1,
        BASE_GFX_2
    }

    record Profile(
            int npcId, boolean aggressive, boolean agroSosc, boolean agroCoi,
            String family, int agroFamily, int agroGfxId1, int agroGfxId2,
            int karma) {
        Profile {
            if (npcId <= 0 || family == null || agroFamily < 0
                    || agroGfxId1 < -1
                    || agroGfxId2 < -1) {
                throw new IllegalArgumentException("invalid monster aggression profile");
            }
        }

        boolean orcFamily() {
            return family.equals("orc");
        }

        boolean completelyPassive() {
            return !aggressive && !agroSosc && agroGfxId1 < 0 && agroGfxId2 < 0;
        }
    }

    record PlayerDisposition(
            int characterType, int baseGfx, int currentGfx,
            boolean shapeChanged, boolean invisible, int lawful,
            int karmaLevel, boolean orcCastleOwner) {
        PlayerDisposition {
            if (characterType < 0 || characterType > 6 || baseGfx < 0
                    || currentGfx < 0 || lawful < Short.MIN_VALUE
                    || lawful > Short.MAX_VALUE || karmaLevel < -8
                    || karmaLevel > 8) {
                throw new IllegalArgumentException("invalid monster target disposition");
            }
        }
    }

    private ClassicWorldMonsterAggressionCatalog() {}

    static Profile forTemplate(ClassicWorldMonsterCatalog.MonsterTemplate template) {
        if (template == null) throw new IllegalArgumentException("missing monster template");
        Profile selected = BY_NPC.get(template.npcId());
        if (selected == null) {
            // Runtime-only quest actors have no selected spawn-table projection.
            // Preserve their explicitly supplied basic agro bit without inventing
            // any of the database-only extended selectors.
            return new Profile(template.npcId(), template.aggressive(), false, false,
                    "", 0, -1, -1, 0);
        }
        if (selected.aggressive() != template.aggressive()) {
            throw new IllegalStateException("monster agro source mismatch " + template.npcId());
        }
        return selected;
    }

    static Profile forNpc(int npcId) {
        return BY_NPC.get(npcId);
    }

    static int size() {
        return BY_NPC.size();
    }

    /** Exact selected retained-source decision order after visibility/LOS checks. */
    static Acquisition acquisition(
            int mapId, Profile monster, PlayerDisposition player) {
        if (monster == null || player == null) return Acquisition.NONE;
        if (mapId == 410 && player.currentGfx() == 4261) return Acquisition.NONE;
        if (monster.orcFamily() && player.orcCastleOwner()) return Acquisition.NONE;

        if (ARENA_MAPS.contains(mapId)
                && (!player.invisible() || monster.agroCoi())) {
            return Acquisition.ARENA;
        }
        if (monster.npcId() == 45600) {
            if (player.characterType() == 0
                    && player.currentGfx() == player.baseGfx()) {
                return Acquisition.KURTZ_CROWN;
            }
            if (player.characterType() == 4) return Acquisition.KURTZ_DARK_ELF;
        }

        if ((monster.karma() < 0 && player.karmaLevel() >= 1)
                || (monster.karma() > 0 && player.karmaLevel() <= -1)) {
            return Acquisition.NONE;
        }
        if ((player.currentGfx() == 6034 && monster.karma() < 0)
                || (player.currentGfx() == 6035 && monster.karma() > 0)
                || (player.currentGfx() == 6035
                    && (monster.npcId() == 46070 || monster.npcId() == 46072))) {
            return Acquisition.NONE;
        }

        // The selected server has kill_red=true. As in targetPlayer1000(),
        // agrocoi deliberately does not make a monster non-passive here.
        if (monster.completelyPassive() && player.lawful() < -1000) {
            return Acquisition.CHAOTIC_PLAYER;
        }
        if (player.invisible() && !monster.agroCoi()) return Acquisition.NONE;
        if (player.shapeChanged() && monster.agroSosc()) {
            return Acquisition.SHAPE_CHANGE;
        }
        if (monster.aggressive()) return Acquisition.AGGRESSIVE;
        if (monster.agroGfxId1() >= 0
                && player.baseGfx() == monster.agroGfxId1()) {
            return Acquisition.BASE_GFX_1;
        }
        if (monster.agroGfxId2() >= 0
                && player.baseGfx() == monster.agroGfxId2()) {
            return Acquisition.BASE_GFX_2;
        }
        return Acquisition.NONE;
    }

    private static Map<Integer, Profile> load() {
        byte[] packed;
        try (InputStream resource = ClassicWorldMonsterAggressionCatalog.class
                .getResourceAsStream(RESOURCE)) {
            if (resource == null) throw new IOException("missing " + RESOURCE);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError("monster aggression resource hash mismatch");
        }

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid aggression magic");
            int rows = in.readInt();
            String databaseHash = hex(in.readNBytes(32));
            String sourceHash = hex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (rows != ROW_COUNT || !SELECTED_DATABASE_SHA256.equals(databaseHash)
                    || !SOURCE_SHA256.equals(sourceHash)
                    || !SOURCE_SHA256.equals(sha256(table))) {
                throw new IOException("monster aggression source contract changed");
            }
            Map<Integer, Profile> result = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(table), StandardCharsets.US_ASCII))) {
                if (!TSV_HEADER.equals(reader.readLine())) {
                    throw new IOException("invalid aggression table header");
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 9) throw new IOException("invalid aggression row");
                    Profile profile = new Profile(
                            integer(fields[0]), flag(fields[1]), flag(fields[2]),
                            flag(fields[3]), fields[4], integer(fields[5]),
                            integer(fields[6]), integer(fields[7]), integer(fields[8]));
                    if (result.put(profile.npcId(), profile) != null) {
                        throw new IOException("duplicate aggression NPC " + profile.npcId());
                    }
                }
            }
            if (result.size() != rows) throw new IOException("aggression row count mismatch");
            return Map.copyOf(result);
        } catch (IOException | NumberFormatException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static boolean flag(String value) throws IOException {
        if (value.equals("0")) return false;
        if (value.equals("1")) return true;
        throw new IOException("invalid aggression flag " + value);
    }

    private static int integer(String value) {
        return Integer.parseInt(value);
    }

    private static String hex(byte[] value) throws IOException {
        if (value.length != 32) throw new IOException("truncated aggression hash");
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
