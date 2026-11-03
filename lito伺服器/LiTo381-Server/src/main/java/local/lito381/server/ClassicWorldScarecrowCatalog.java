package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Retained {@code L1Scarecrow} templates and the same-family point spawns that
 * can be hosted by the currently imported classic maps.
 *
 * <p>The selected 380 database contains the four NPC templates but no
 * {@code spawnlist_npc} rows for them.  Point coordinates therefore come from
 * the separately retained L1JGO-Whale {@code spawn_list.yaml}; only map 4 and
 * map 304 are admitted here.  Its map 68 and map 2005 rows remain an explicit
 * resource dependency until those maps are imported.</p>
 */
final class ClassicWorldScarecrowCatalog {
    static final String RETAINED_INSTANCE_SHA256 =
            "0B7A6806DF02DB90AE87CF3458005A1CC0FF271067671AB033FBB5B732CDDD2C";
    static final String RETAINED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SECONDARY_SPAWN_SOURCE_SHA256 =
            "7721A509A25E798FDE86672A0DA296BC5C4D1A8E3733E649919C8280A6EA3789";
    static final String SECONDARY_NPC_SOURCE_SHA256 =
            "F15D42F3E0B4DDDCC9B2EB3B7E8A38F4253D75E4CF1C3F1F155572E3871B47C1";
    static final String SECONDARY_COMBAT_SOURCE_SHA256 =
            "D6496DFD4AC49F9059CAF1666DD4CD143BA59686D0E4C900CB63036F4F82D58E";

    static final int SOURCE_SPAWN_COUNT = 84;
    static final int SUPPORTED_SPAWN_COUNT = 37;
    static final int DEFERRED_SPAWN_COUNT = 47;
    static final int MAP4_SPAWN_COUNT = 28;
    static final int MAP304_SPAWN_COUNT = 9;
    static final int MAP68_DEFERRED_COUNT = 16;
    static final int MAP2005_DEFERRED_COUNT = 31;
    static final int RETAINED_TRAINING_MAX_LEVEL = 14;

    /** Synthetic identities only; coordinates, template IDs and headings are source-exact. */
    private static final int RECONSTRUCTED_SPAWN_BASE = 9_450_000;
    private static final List<ClassicWorldNpcCatalog.StaticNpc> SUPPORTED = supported();

    private ClassicWorldScarecrowCatalog() {}

    static List<ClassicWorldNpcCatalog.StaticNpc> all() {
        return SUPPORTED;
    }

    static List<ClassicWorldNpcCatalog.StaticNpc> forMap(int mapId) {
        return SUPPORTED.stream().filter(row -> row.mapId() == mapId).toList();
    }

    static ClassicWorldNpcCatalog.StaticNpc target(int mapId, long objectId) {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(mapId, objectId);
        return isScarecrow(npc) ? npc : null;
    }

    static boolean isScarecrow(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.implementation().equals("L1Scarecrow")
                && npc.npcId() >= 45001 && npc.npcId() <= 45004;
    }

    /**
     * Read-only combat projection used to reuse the complete retained NPC hit
     * calculation.  It is never registered with the monster population and
     * therefore cannot gain hate, run AI, lose HP, die, respawn or drop items.
     */
    static ClassicWorldMonsterPopulation.Snapshot combatProjection(
            ClassicWorldNpcCatalog.StaticNpc npc, int heading) {
        if (!isScarecrow(npc) || heading < 0 || heading > 7) {
            throw new IllegalArgumentException("invalid scarecrow combat projection");
        }
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        npc.npcId(), npc.nameId(), "L1Monster", npc.gfxId(),
                        1, 1, 0, 0,
                        0, 0, 0, 0, 99, 0, 2, 0,
                        "small", 0, 1,
                        0, 0, 0, 0,
                        0, 0, false,
                        0, false, false, false, 0, false);
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        npc.mapId(), npc.spawnId(), "L1Scarecrow point", 1, 0,
                        npc.x(), npc.y(), 0, 0, 0, 0, 0, 0, heading,
                        0, 0, false, 0, true, 0, template);
        return new ClassicWorldMonsterPopulation.Snapshot(
                npc.mapId(), npc.spawnId(), 1, npc.objectId(), npc.npcId(),
                npc.x(), npc.y(), heading, 1, 1, 0, 0,
                true, true, definition);
    }

    private static List<ClassicWorldNpcCatalog.StaticNpc> supported() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows = new ArrayList<>(
                SUPPORTED_SPAWN_COUNT);
        int sequence = 1;
        int[][] map4Npc45001 = {
                {33134,33316}, {33114,33318}, {33130,33316}, {33102,33314},
                {33138,33316}, {33110,33322}, {33102,33322}, {33106,33326},
                {33142,33316}, {33130,33324}, {33134,33324}, {33138,33324},
                {33106,33318}, {33142,33324}
        };
        for (int[] point : map4Npc45001) {
            rows.add(row(sequence++, 4, 45001, 114, point[0], point[1], 4));
        }
        int[][] map4Npc45002 = {
                {33134,33320}, {33138,33320}, {33142,33328}, {33134,33328},
                {33130,33328}, {33102,33318}, {33106,33322}, {33102,33326},
                {33110,33318}, {33114,33322}, {33142,33320}, {33138,33328},
                {33114,33314}, {33130,33320}
        };
        for (int[] point : map4Npc45002) {
            rows.add(row(sequence++, 4, 45002, 116, point[0], point[1], 4));
        }
        int[][] map304Npc45004 = {
                {32793,32810}, {32799,32810}, {32795,32821},
                {32792,32821}, {32789,32821}, {32798,32821},
                {32790,32810}, {32796,32810}, {32802,32810}
        };
        for (int[] point : map304Npc45004) {
            rows.add(row(sequence++, 304, 45004, 2992, point[0], point[1], 1));
        }
        if (rows.size() != SUPPORTED_SPAWN_COUNT) {
            throw new IllegalStateException("scarecrow supported-spawn contract drift");
        }
        return List.copyOf(rows);
    }

    private static ClassicWorldNpcCatalog.StaticNpc row(
            int sequence, int mapId, int npcId, int gfxId,
            int x, int y, int heading) {
        int spawnId = RECONSTRUCTED_SPAWN_BASE + sequence;
        return new ClassicWorldNpcCatalog.StaticNpc(
                mapId, spawnId, ClassicWorldNpcCatalog.objectId(spawnId), npcId,
                x, y, heading, 0, "", "L1Scarecrow", gfxId,
                1, 1, 0, 2, 0, 0, 0, 0, false, "", "");
    }
}
