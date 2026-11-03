package local.lito381.server;

import java.util.List;

/**
 * Retained Orc-castle {@code L1MonsterGuard} arrow towers.
 *
 * <p>The selected database provides the authoritative NPC 60520 template and
 * the retained {@code L1MonsterGuardInstance} provides its receiver and target
 * rules.  The selected SQL export has no spawn table; spawn ids 356..366 and
 * their point coordinates therefore come from the retained same-3.80-family
 * yiwei backup.  All eleven points are on the already imported map 4.</p>
 */
final class ClassicWorldMonsterGuardCatalog {
    static final int MAP_ID = 4;
    static final int NPC_ID = 60520;
    static final int CASTLE_ID = 2;
    static final int GFX_ID = 816;
    static final int SOURCE_SPAWN_COUNT = 11;
    static final int ATTACK_RANGE = 10;
    static final int ATTACK_INTERVAL_MILLIS = 1880;
    static final int BOW_ACTION_ID = 66;

    static final String RETAINED_INSTANCE_SHA256 =
            "76349C44D2BD4930C7325C939A80F789F169A3B6CF80084DF95CBBA2E7B68140";
    static final String RETAINED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String SECONDARY_SPAWN_DATABASE_SHA256 =
            "3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423";
    static final String SECONDARY_CHARACTER_SOURCE_SHA256 =
            "6BD483D18590BFABCED480475758F8A8D69608449C916DFFC95734CF9ECED669";
    static final String SECONDARY_MAP4_SOURCE_SHA256 =
            "7AD114D4B466EDCC57E07C0880323D1605684C856AB939EE6ADD05403E7701B0";

    private static final List<ClassicWorldNpcCatalog.StaticNpc> ROWS = List.of(
            row(356, 32784, 32321), row(357, 32781, 32320),
            row(358, 32790, 32321), row(359, 32794, 32320),
            row(360, 32827, 32298), row(361, 32827, 32288),
            row(362, 32823, 32273), row(363, 32813, 32263),
            row(364, 32797, 32261), row(365, 32766, 32287),
            row(366, 32764, 32297));

    private ClassicWorldMonsterGuardCatalog() {}

    static List<ClassicWorldNpcCatalog.StaticNpc> all() {
        return ROWS;
    }

    static ClassicWorldNpcCatalog.StaticNpc target(int mapId, long objectId) {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                mapId, objectId);
        return isMonsterGuard(npc) ? npc : null;
    }

    static boolean isMonsterGuard(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID && npc.npcId() == NPC_ID
                && npc.implementation().equals("L1MonsterGuard");
    }

    /**
     * Historical 3.80 {@code L1Character.glanceCheck} stopped before asking the
     * map about an adjacent target.  Every retained arrow-tower origin is raw
     * tile zero in both map-4 sources, so this one-cell admission is the only
     * reachable native receiver/AI boundary; distance two and above still uses
     * the strict imported line-of-sight implementation.
     */
    static boolean attackPosition(
            ClassicWorldMapCatalog.ClassicMap map,
            int fromX, int fromY, int targetX, int targetY,
            int requestedRange) {
        if (map == null || map.mapId() != MAP_ID) return false;
        int range = requestedRange <= 0 ? 1 : requestedRange;
        int dx = Math.abs(targetX - fromX);
        int dy = Math.abs(targetY - fromY);
        int distance = range >= 7 ? dx + dy : Math.max(dx, dy);
        if (distance > range) return false;
        if (Math.max(dx, dy) == 1) return true;
        return map.hasLineOfSight(fromX, fromY, targetX, targetY);
    }

    /**
     * Immutable combat projection. It is deliberately never registered in a
     * monster population, matching the empty HP/MP receiver and mutation gates.
     */
    static ClassicWorldMonsterPopulation.Snapshot combatProjection(
            ClassicWorldNpcCatalog.StaticNpc npc, int heading) {
        if (!isMonsterGuard(npc) || heading < 0 || heading > 7) {
            throw new IllegalArgumentException("invalid monster-guard projection");
        }
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        NPC_ID, "$240", "L1Monster", GFX_ID,
                        25, 10, 10, -50,
                        60, 14, 60, 8, 50, 80, 1970, 0,
                        "small", 0, ATTACK_RANGE,
                        0, ATTACK_INTERVAL_MILLIS, BOW_ACTION_ID,
                        0, 0, 0, true,
                        0, false, false, false, 0, false);
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, npc.spawnId(), "Orc castle arrow tower", 1, 0,
                        npc.x(), npc.y(), 0, 0, 0, 0, 0, 0, heading,
                        0, 0, false, 0, true, 0, template);
        return new ClassicWorldMonsterPopulation.Snapshot(
                MAP_ID, npc.spawnId(), 1, npc.objectId(), NPC_ID,
                npc.x(), npc.y(), heading, 10, 10, 10, 10,
                true, true, definition);
    }

    private static ClassicWorldNpcCatalog.StaticNpc row(
            int spawnId, int x, int y) {
        return new ClassicWorldNpcCatalog.StaticNpc(
                MAP_ID, spawnId, ClassicWorldNpcCatalog.objectId(spawnId),
                NPC_ID, x, y, 4, 0, "$240", "L1MonsterGuard", GFX_ID,
                25, 10, 10, 1970, 0, 0, 0, ATTACK_INTERVAL_MILLIS,
                false, "", "");
    }
}
