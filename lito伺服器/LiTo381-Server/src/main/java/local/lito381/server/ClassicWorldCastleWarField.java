package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Source-qualified runtime mirror of the retained {@code L1WarSpawn} field.
 *
 * <p>The eight rectangles and return points are copied from the selected
 * {@code L1CastleLocation}/{@code L1TownLocation}.  A BEGIN first replaces the
 * four server-owned trebuchets, then creates every eight-tile perimeter flag,
 * including the source's independently generated corner entries.  END removes
 * the castle's field flags and objective actors, then restores the native tower
 * set; trebuchets are ordinary monsters in the source and therefore do not
 * belong to the field-object cleanup.</p>
 */
final class ClassicWorldCastleWarField {
    static final String WAR_SPAWN_SOURCE_SHA256 =
            "4E0B753EC1E70EA20FB5B3ACF9621BC51DA387DE9C4DD46B16DDA1916C816A51";
    static final String CASTLE_LOCATION_SOURCE_SHA256 =
            "DEFE3746B3C7F728DB8BAB1E9A756673FBB45EAD0EB94847DD486303D0AD4CFA";
    static final String TOWN_LOCATION_SOURCE_SHA256 =
            "DAEF00602E793678BFD1C9BC0E210D2E9856F2A7C45FC38D9D322A5A22A5EDE3";
    static final String FIELD_PACKET_SOURCE_SHA256 =
            "DE4D7BC82922B581EB042CA772036500F9A01E5257CEA2DB0022D4E2CFC0A247";
    static final String MONSTER_PACKET_SOURCE_SHA256 =
            "DEE24B66CD725B3C70A110DA0DBFBB61F9E20D6A13B00A798DFF046E0251F0AA";

    static final int FLAG_NPC_ID = 81122;
    static final int FLAG_GFX_ID = 1284;
    static final String FLAG_NAME_ID = "$5184";
    static final int ATTACK_TREBUCHET_NPC_ID = 20000;
    static final int ATTACK_TREBUCHET_GFX_ID = 12159;
    static final int DEFENCE_TREBUCHET_NPC_ID = 20001;
    static final int DEFENCE_TREBUCHET_GFX_ID = 12001;
    static final String TREBUCHET_NAME_ID = "投石車";
    static final int TREBUCHET_LEVEL = 70;
    static final int TREBUCHET_MAX_HP = 30_000;
    static final int TREBUCHET_MAX_MP = 20;
    static final int TREBUCHET_ARMOR_CLASS = -10;

    private static final long FIRST_OBJECT_ID = 0x7600_0000L;
    private static final long LAST_OBJECT_ID = 0x76FF_FFFFL;

    enum Kind { FLAG, TREBUCHET, TOWER, CROWN }

    static final int TOWER_NPC_ID = 81111;
    static final int TOWER_GFX_ID = 1672;
    static final int ADEN_TOWER_NPC_ID = 81189;
    static final int ADEN_TOWER_GFX_ID = 2674;
    static final int CROWN_NPC_ID = 81125;
    static final int CROWN_GFX_ID = 1482;
    static final String TOWER_NAME_ID = "$1435";
    static final String CROWN_NAME_ID = "$413";
    static final int TOWER_LEVEL = 1;
    static final int TOWER_MAX_HP = 10_000;
    static final int ADEN_SUB_TOWER_MAX_HP = 3_000;
    static final int TOWER_CRACK_1 = 32;
    static final int TOWER_CRACK_2 = 33;
    static final int TOWER_CRACK_3 = 34;
    static final int TOWER_DIE = 35;

    record Point(int x, int y, int mapId) {
        Point {
            if (x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF || mapId < 0) {
                throw new IllegalArgumentException("invalid castle-war point");
            }
        }
    }

    record Area(int castleId, int x1, int x2, int y1, int y2, int mapId) {
        Area {
            if (castleId < 1 || castleId > 8 || x1 <= 0 || x2 < x1
                    || y1 <= 0 || y2 < y1 || mapId < 0) {
                throw new IllegalArgumentException("invalid castle-war area");
            }
        }

        boolean contains(int map, int x, int y) {
            return map == mapId && x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    record WarObject(long objectId, Kind kind, int castleId, int mapId,
                     int x, int y, int heading, int npcId, int gfxId,
                     String nameId, int level, int currentHp, int maximumHp) {
        WarObject {
            if (objectId < FIRST_OBJECT_ID || objectId > LAST_OBJECT_ID
                    || kind == null || castleId < 0 || castleId > 8
                    || mapId < 0 || x <= 0 || x > 0xFFFF
                    || y <= 0 || y > 0xFFFF || heading < 0 || heading > 7
                    || npcId <= 0 || gfxId <= 0 || gfxId > 0xFFFF
                    || nameId == null || level < 0 || level > 0xFF
                    || currentHp < 0 || maximumHp < 0 || currentHp > maximumHp
                    || (kind == Kind.FLAG && (castleId == 0 || maximumHp != 0))
                    || (kind == Kind.TREBUCHET
                        && (castleId != 0 || maximumHp != TREBUCHET_MAX_HP))
                    || (kind == Kind.TOWER
                        && (castleId == 0 || maximumHp <= 0))
                    || (kind == Kind.CROWN
                        && (castleId == 0 || currentHp != 0 || maximumHp != 0))) {
                throw new IllegalArgumentException("invalid castle-war object");
            }
        }

        boolean inScreen(int map, int viewerX, int viewerY) {
            return mapId == map && BattleSpellCatalog.isInNativeScreen(
                    viewerX, viewerY, x, y);
        }
    }

    record Transition(List<WarObject> removed, List<WarObject> spawned) {
        Transition {
            removed = removed == null ? List.of() : List.copyOf(removed);
            spawned = spawned == null ? List.of() : List.copyOf(spawned);
        }
    }

    record TowerHit(WarObject before, WarObject after, int appliedDamage,
                    int action, WarObject spawnedCrown) {
        TowerHit {
            if (before == null || after == null || before.kind() != Kind.TOWER
                    || after.kind() != Kind.TOWER
                    || before.objectId() != after.objectId()
                    || appliedDamage < 0 || after.currentHp() > before.currentHp()
                    || before.currentHp() - after.currentHp() != appliedDamage
                    || action != 0 && (action < TOWER_CRACK_1 || action > TOWER_DIE)
                    || spawnedCrown != null
                        && (action != TOWER_DIE || spawnedCrown.kind() != Kind.CROWN
                            || spawnedCrown.castleId() != after.castleId())) {
                throw new IllegalArgumentException("invalid castle tower hit");
            }
        }

        boolean diedNow() {
            return before.currentHp() > 0 && after.currentHp() == 0;
        }
    }

    record TrebuchetHit(WarObject before, WarObject after, int appliedDamage) {
        TrebuchetHit {
            if (before == null || after == null
                    || before.kind() != Kind.TREBUCHET
                    || after.kind() != Kind.TREBUCHET
                    || before.objectId() != after.objectId()
                    || appliedDamage < 0 || after.currentHp() > before.currentHp()
                    || before.currentHp() - after.currentHp() != appliedDamage) {
                throw new IllegalArgumentException("invalid castle trebuchet hit");
            }
        }

        boolean diedNow() {
            return before.currentHp() > 0 && after.currentHp() == 0;
        }
    }

    private static final Map<Integer, Area> AREAS = Map.of(
            1, new Area(1, 33089, 33219, 32717, 32827, 4),
            2, new Area(2, 32750, 32850, 32250, 32350, 4),
            3, new Area(3, 32571, 32721, 33350, 33460, 4),
            4, new Area(4, 33559, 33686, 32615, 32755, 4),
            5, new Area(5, 33458, 33583, 33315, 33490, 4),
            6, new Area(6, 32755, 32870, 32790, 32920, 66),
            7, new Area(7, 34007, 34162, 33172, 33332, 4),
            8, new Area(8, 32888, 33070, 32839, 32953, 320));

    private static final Map<Integer, List<Point>> RETURN_POINTS = Map.of(
            1, points(4, 33048,32750, 33059,32768, 33047,32761, 33059,32759,
                    33051,32775, 33048,32778, 33064,32773, 33057,32748),
            2, points(4, 32750,32435, 32745,32447, 32738,32452, 32741,32436,
                    32749,32446),
            3, points(4, 32608,33178, 32626,33185, 32630,33179, 32625,33207,
                    32638,33203, 32621,33179),
            4, points(4, 33435,32803, 33439,32817, 33440,32809, 33419,32810,
                    33426,32823, 33418,32818, 33432,32824),
            5, points(4, 33593,33242, 33593,33248, 33604,33236, 33599,33236,
                    33610,33247, 33610,33241, 33599,33252, 33605,33252),
            6, points(4, 33702,32492, 33747,32508, 33696,32498, 33723,32512,
                    33710,32521, 33724,32488, 33693,32513),
            7, points(4, 33915,33114, 34061,33115, 34090,33168, 34011,33136,
                    34093,33117, 33959,33156, 33992,33120, 34047,33156),
            8, points(310, 32792,32807, 32816,32820, 32823,32797));

    private record TowerDefinition(int castleId, int npcId, int gfxId,
                                   String nameId, int x, int y, int mapId,
                                   int maximumHp, boolean subTower) {}

    private static final List<TowerDefinition> TOWERS = List.of(
            tower(1, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    33139, 32768, 4, TOWER_MAX_HP, false),
            tower(2, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    32798, 32291, 4, TOWER_MAX_HP, false),
            tower(3, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    32623, 33379, 4, TOWER_MAX_HP, false),
            tower(4, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    33631, 32678, 4, TOWER_MAX_HP, false),
            tower(5, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    33524, 33396, 4, TOWER_MAX_HP, false),
            tower(6, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    32828, 32818, 66, TOWER_MAX_HP, false),
            tower(7, ADEN_TOWER_NPC_ID, ADEN_TOWER_GFX_ID, TOWER_NAME_ID,
                    34090, 33260, 4, TOWER_MAX_HP, false),
            tower(7, 81190, 2669, "$2204",
                    34057, 33291, 4, ADEN_SUB_TOWER_MAX_HP, true),
            tower(7, 81191, 2665, "$2205",
                    34123, 33291, 4, ADEN_SUB_TOWER_MAX_HP, true),
            tower(7, 81192, 2667, "$2206",
                    34057, 33230, 4, ADEN_SUB_TOWER_MAX_HP, true),
            tower(7, 81193, 2671, "$2207",
                    34123, 33230, 4, ADEN_SUB_TOWER_MAX_HP, true),
            tower(8, TOWER_NPC_ID, TOWER_GFX_ID, TOWER_NAME_ID,
                    33033, 32895, 320, TOWER_MAX_HP, false));

    private final Map<String, LinkedHashMap<Long, WarObject>> objectsBySource =
            new LinkedHashMap<>();
    private final Map<Long, Integer> towerCrackStages = new LinkedHashMap<>();
    /** Runtime-only L1Monster fields owned by each spawned siege trebuchet. */
    private final Map<Long, TrebuchetState> trebuchetStates =
            new LinkedHashMap<>();
    private long nextObjectId = FIRST_OBJECT_ID;

    synchronized Transition ensureTowers(String source) {
        String sourceKey = requireSource(source);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.computeIfAbsent(
                sourceKey, ignored -> new LinkedHashMap<>());
        List<WarObject> spawned = new ArrayList<>();
        for (int castleId = 1; castleId <= 8; castleId++) {
            int currentCastleId = castleId;
            if (objects.values().stream().noneMatch(object ->
                    object.kind() == Kind.TOWER
                            && object.castleId() == currentCastleId)) {
                List<WarObject> towers = spawnTowers(currentCastleId);
                towers.forEach(object -> objects.put(object.objectId(), object));
                spawned.addAll(towers);
            }
        }
        return new Transition(List.of(), spawned);
    }

    synchronized Transition begin(String source, int castleId) {
        String sourceKey = requireSource(source);
        Area area = area(castleId);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.computeIfAbsent(
                sourceKey, ignored -> new LinkedHashMap<>());
        List<WarObject> removed = removeMatching(objects,
                object -> object.kind() == Kind.TREBUCHET
                        || object.kind() == Kind.FLAG
                        && object.castleId() == castleId);
        removed.stream().filter(object -> object.kind() == Kind.TREBUCHET)
                .forEach(object -> trebuchetStates.remove(object.objectId()));
        List<WarObject> spawned = new ArrayList<>();
        spawned.add(trebuchet(DEFENCE_TREBUCHET_NPC_ID,
                DEFENCE_TREBUCHET_GFX_ID, 33638, 32691));
        spawned.add(trebuchet(DEFENCE_TREBUCHET_NPC_ID,
                DEFENCE_TREBUCHET_GFX_ID, 33634, 32691));
        spawned.add(trebuchet(ATTACK_TREBUCHET_NPC_ID,
                ATTACK_TREBUCHET_GFX_ID, 33631, 32749));
        spawned.add(trebuchet(ATTACK_TREBUCHET_NPC_ID,
                ATTACK_TREBUCHET_GFX_ID, 33152, 32812));
        spawned.addAll(flags(area));
        for (WarObject object : spawned) objects.put(object.objectId(), object);
        return new Transition(removed, spawned);
    }

    synchronized Transition end(String source, int castleId) {
        String sourceKey = requireSource(source);
        Area area = area(castleId);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.computeIfAbsent(
                sourceKey, ignored -> new LinkedHashMap<>());
        List<WarObject> removed = removeMatching(objects,
                object -> (object.kind() == Kind.FLAG
                            && area.contains(object.mapId(), object.x(), object.y()))
                        || (object.castleId() == castleId
                            && (object.kind() == Kind.TOWER
                                || object.kind() == Kind.CROWN)));
        removed.stream().filter(object -> object.kind() == Kind.TOWER)
                .forEach(object -> towerCrackStages.remove(object.objectId()));
        List<WarObject> spawned = spawnTowers(castleId);
        spawned.forEach(object -> objects.put(object.objectId(), object));
        return new Transition(removed, spawned);
    }

    synchronized WarObject object(String source, int mapId, long objectId) {
        LinkedHashMap<Long, WarObject> objects = objectsBySource.get(normalize(source));
        WarObject object = objects == null ? null : objects.get(objectId);
        return object != null && object.mapId() == mapId ? object : null;
    }

    synchronized ClassicWorldMonsterPopulation.Snapshot towerCombatSnapshot(
            String source, int mapId, long objectId) {
        WarObject object = object(source, mapId, objectId);
        if (object == null || object.kind() != Kind.TOWER
                || object.currentHp() <= 0) return null;
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        object.npcId(), object.nameId(), "L1Monster", object.gfxId(),
                        TOWER_LEVEL, object.maximumHp(), 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        "large", 0, 0, 0, 0, 0, 0,
                        0, 0, false, 0, false, false, false, 0, false);
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        object.mapId(), object.npcId(), "castle-war-tower", 1, 0,
                        object.x(), object.y(), 0, 0, 0, 0, 0, 0,
                        object.heading(), 0, 0, false, 0, true, 0, template);
        return new ClassicWorldMonsterPopulation.Snapshot(
                object.mapId(), object.npcId(), 1, object.objectId(), object.npcId(),
                object.x(), object.y(), object.heading(), object.currentHp(),
                object.maximumHp(), 0, 0, object.currentHp() > 0, true, definition);
    }

    synchronized ClassicWorldMonsterPopulation.Snapshot trebuchetCombatSnapshot(
            String source, int mapId, long objectId) {
        WarObject object = object(source, mapId, objectId);
        TrebuchetState state = trebuchetStates.get(objectId);
        if (object == null || object.kind() != Kind.TREBUCHET
                || object.currentHp() <= 0 || state == null) return null;
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        object.npcId(), object.nameId(), "L1Monster", object.gfxId(),
                        TREBUCHET_LEVEL, object.maximumHp(), TREBUCHET_MAX_MP,
                        TREBUCHET_ARMOR_CLASS,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        "large", 0, 0, 0, 0, 0, 0,
                        0, 0, false, 0, false, false, false, 0, false);
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        object.mapId(), object.npcId(), "castle-war-trebuchet", 1, 0,
                        object.x(), object.y(), 0, 0, 0, 0, 0, 0,
                        object.heading(), 0, 0, false, 0, true, 0, template);
        return new ClassicWorldMonsterPopulation.Snapshot(
                object.mapId(), object.npcId(), 1, object.objectId(), object.npcId(),
                object.x(), object.y(), object.heading(), object.currentHp(),
                object.maximumHp(), state.currentMp, TREBUCHET_MAX_MP,
                object.currentHp() > 0, true, definition);
    }

    synchronized ClassicWorldMonsterPopulation.ManaDrain
            drainTrebuchetManaIfStrictlyAbove(
                    String source, int mapId, long objectId,
                    int requestedDrain) {
        if (requestedDrain <= 0) return null;
        ClassicWorldMonsterPopulation.Snapshot before =
                trebuchetCombatSnapshot(source, mapId, objectId);
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        if (before == null || state == null) return null;
        int actualDrain = state.currentMp > requestedDrain ? requestedDrain : 0;
        state.currentMp -= actualDrain;
        ClassicWorldMonsterPopulation.Snapshot after =
                trebuchetCombatSnapshot(source, mapId, objectId);
        return new ClassicWorldMonsterPopulation.ManaDrain(
                before, after, requestedDrain, actualDrain);
    }

    synchronized boolean confusionSilenceTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.timedStatus.activateSilenceIfAbsent(
                nowNanos, durationSeconds);
    }

    synchronized boolean trebuchetSilenceActive(
            String source, int mapId, long objectId, long nowNanos) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.timedStatus.silenceActive(nowNanos);
    }

    synchronized boolean thunderGrabTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.timedStatus.activateThunderGrab(
                nowNanos, durationSeconds);
    }

    synchronized boolean trebuchetThunderGrabActive(
            String source, int mapId, long objectId, long nowNanos) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.timedStatus.thunderGrabActive(nowNanos);
    }

    synchronized boolean boneBreakTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.timedStatus.activateBoneBreak(
                nowNanos, durationSeconds);
    }

    synchronized boolean trebuchetBoneBreakActive(
            String source, int mapId, long objectId, long nowNanos) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.timedStatus.boneBreakActive(nowNanos);
    }

    synchronized BattleArmBreakerStatus.Activation armBreakerTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state == null ? BattleArmBreakerStatus.Activation.rejected()
                : state.armBreakerStatus.activateIfAbsent(
                        nowNanos, durationSeconds);
    }

    synchronized boolean trebuchetArmBreakerActive(
            String source, int mapId, long objectId, long nowNanos) {
        TrebuchetState state = liveTrebuchetState(source, mapId, objectId);
        return state != null && state.armBreakerStatus.active(nowNanos);
    }

    synchronized TowerHit damageTower(
            String source, int mapId, long objectId, int damage) {
        if (damage <= 0) return null;
        String sourceKey = requireSource(source);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.get(sourceKey);
        WarObject before = objects == null ? null : objects.get(objectId);
        if (before == null || before.mapId() != mapId
                || before.kind() != Kind.TOWER || before.currentHp() <= 0) return null;
        if (before.castleId() == 7 && before.npcId() == ADEN_TOWER_NPC_ID
                && objects.values().stream().anyMatch(object ->
                    object.kind() == Kind.TOWER && object.castleId() == 7
                            && isAdenSubTower(object.npcId())
                            && object.currentHp() > 0)) {
            return new TowerHit(before, before, 0, 0, null);
        }
        int nextHp = Math.max(0, before.currentHp() - damage);
        int applied = before.currentHp() - nextHp;
        WarObject after = new WarObject(
                before.objectId(), before.kind(), before.castleId(), before.mapId(),
                before.x(), before.y(), before.heading(), before.npcId(), before.gfxId(),
                before.nameId(), before.level(), nextHp, before.maximumHp());
        objects.put(objectId, after);
        int action = 0;
        WarObject crown = null;
        if (nextHp == 0) {
            towerCrackStages.remove(objectId);
            action = TOWER_DIE;
            if (!isAdenSubTower(before.npcId())) {
                crown = crown(before.castleId());
                objects.put(crown.objectId(), crown);
            }
        } else {
            int stage = crackStage(nextHp, before.maximumHp());
            int previousStage = towerCrackStages.getOrDefault(objectId, 0);
            if (stage != 0 && stage != previousStage) {
                towerCrackStages.put(objectId, stage);
                action = TOWER_CRACK_1 + stage - 1;
            }
        }
        return new TowerHit(before, after, applied, action, crown);
    }

    synchronized TrebuchetHit damageTrebuchet(
            String source, int mapId, long objectId, int damage) {
        if (damage <= 0) return null;
        String sourceKey = requireSource(source);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.get(sourceKey);
        WarObject before = objects == null ? null : objects.get(objectId);
        if (before == null || before.mapId() != mapId
                || before.kind() != Kind.TREBUCHET
                || before.currentHp() <= 0) return null;
        int nextHp = Math.max(0, before.currentHp() - damage);
        int applied = before.currentHp() - nextHp;
        WarObject after = new WarObject(
                before.objectId(), before.kind(), before.castleId(), before.mapId(),
                before.x(), before.y(), before.heading(), before.npcId(), before.gfxId(),
                before.nameId(), before.level(), nextHp, before.maximumHp());
        objects.put(objectId, after);
        return new TrebuchetHit(before, after, applied);
    }

    synchronized WarObject removeTrebuchetCorpse(
            String source, int mapId, long objectId) {
        String sourceKey = requireSource(source);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.get(sourceKey);
        WarObject object = objects == null ? null : objects.get(objectId);
        if (object == null || object.mapId() != mapId
                || object.kind() != Kind.TREBUCHET || object.currentHp() != 0) {
            return null;
        }
        objects.remove(objectId);
        trebuchetStates.remove(objectId);
        return object;
    }

    synchronized Transition claimCrown(String source, int mapId, long crownObjectId) {
        String sourceKey = requireSource(source);
        LinkedHashMap<Long, WarObject> objects = objectsBySource.get(sourceKey);
        WarObject crown = objects == null ? null : objects.get(crownObjectId);
        if (crown == null || crown.kind() != Kind.CROWN || crown.mapId() != mapId) {
            return null;
        }
        int castleId = crown.castleId();
        List<WarObject> removed = removeMatching(objects,
                object -> object.castleId() == castleId
                        && (object.kind() == Kind.CROWN || object.kind() == Kind.TOWER));
        removed.stream().filter(object -> object.kind() == Kind.TOWER)
                .forEach(object -> towerCrackStages.remove(object.objectId()));
        List<WarObject> spawned = spawnTowers(castleId);
        spawned.forEach(object -> objects.put(object.objectId(), object));
        return new Transition(removed, spawned);
    }

    synchronized List<WarObject> inScreen(
            String source, int mapId, int x, int y) {
        LinkedHashMap<Long, WarObject> objects =
                objectsBySource.get(normalize(source));
        if (objects == null) return List.of();
        return objects.values().stream()
                .filter(object -> object.inScreen(mapId, x, y))
                .toList();
    }

    synchronized List<WarObject> objects(String source) {
        LinkedHashMap<Long, WarObject> objects =
                objectsBySource.get(normalize(source));
        return objects == null ? List.of() : List.copyOf(objects.values());
    }

    static Area area(int castleId) {
        Area area = AREAS.get(castleId);
        if (area == null) throw new IllegalArgumentException("unknown castle " + castleId);
        return area;
    }

    static int castleIdAt(int mapId, int x, int y) {
        for (int castleId = 1; castleId <= 8; castleId++) {
            if (area(castleId).contains(mapId, x, y)) return castleId;
        }
        return 0;
    }

    static List<Point> returnPoints(int castleId) {
        List<Point> result = RETURN_POINTS.get(castleId);
        if (result == null) throw new IllegalArgumentException("unknown castle " + castleId);
        return result;
    }

    static Point returnPoint(int castleId, int selector) {
        List<Point> points = returnPoints(castleId);
        return points.get(Math.floorMod(selector, points.size()));
    }

    static int flagCount(int castleId) {
        return flagPoints(area(castleId)).size();
    }

    static int towerCount(int castleId) {
        return (int) TOWERS.stream().filter(tower -> tower.castleId() == castleId).count();
    }

    static boolean isStaticTower(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && "L1Tower".equals(npc.implementation());
    }

    private List<WarObject> spawnTowers(int castleId) {
        List<WarObject> result = new ArrayList<>();
        for (TowerDefinition tower : TOWERS) {
            if (tower.castleId() != castleId) continue;
            result.add(new WarObject(nextObjectId(), Kind.TOWER, tower.castleId(),
                    tower.mapId(), tower.x(), tower.y(), 0, tower.npcId(),
                    tower.gfxId(), tower.nameId(), TOWER_LEVEL,
                    tower.maximumHp(), tower.maximumHp()));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("unknown castle " + castleId);
        return List.copyOf(result);
    }

    private WarObject crown(int castleId) {
        TowerDefinition tower = TOWERS.stream()
                .filter(candidate -> candidate.castleId() == castleId
                        && !candidate.subTower())
                .findFirst().orElseThrow();
        return new WarObject(nextObjectId(), Kind.CROWN, castleId,
                tower.mapId(), tower.x(), tower.y(), 0,
                CROWN_NPC_ID, CROWN_GFX_ID, CROWN_NAME_ID, 0, 0, 0);
    }

    private static TowerDefinition tower(
            int castleId, int npcId, int gfxId, String nameId,
            int x, int y, int mapId, int maximumHp, boolean subTower) {
        return new TowerDefinition(castleId, npcId, gfxId, nameId,
                x, y, mapId, maximumHp, subTower);
    }

    private static boolean isAdenSubTower(int npcId) {
        return npcId >= 81190 && npcId <= 81193;
    }

    private static int crackStage(int hp, int maximumHp) {
        if (maximumHp / 4 > hp) return 3;
        if (maximumHp * 2 / 4 > hp) return 2;
        if (maximumHp * 3 / 4 > hp) return 1;
        return 0;
    }

    private List<WarObject> flags(Area area) {
        List<WarObject> result = new ArrayList<>();
        for (Point point : flagPoints(area)) {
            result.add(new WarObject(nextObjectId(), Kind.FLAG, area.castleId(),
                    point.mapId(), point.x(), point.y(), 0,
                    FLAG_NPC_ID, FLAG_GFX_ID, FLAG_NAME_ID, 0, 0, 0));
        }
        return result;
    }

    private WarObject trebuchet(int npcId, int gfxId, int x, int y) {
        WarObject object = new WarObject(nextObjectId(), Kind.TREBUCHET, 0, 4,
                x, y, 0, npcId, gfxId, TREBUCHET_NAME_ID,
                TREBUCHET_LEVEL, TREBUCHET_MAX_HP, TREBUCHET_MAX_HP);
        trebuchetStates.put(object.objectId(), new TrebuchetState());
        return object;
    }

    private TrebuchetState liveTrebuchetState(
            String source, int mapId, long objectId) {
        WarObject object = object(source, mapId, objectId);
        return object == null || object.kind() != Kind.TREBUCHET
                || object.currentHp() <= 0 ? null : trebuchetStates.get(objectId);
    }

    private static final class TrebuchetState {
        private int currentMp = TREBUCHET_MAX_MP;
        private final BattleTimedStatus timedStatus = new BattleTimedStatus();
        private final BattleArmBreakerStatus armBreakerStatus =
                new BattleArmBreakerStatus();
    }

    private static List<Point> flagPoints(Area area) {
        List<Point> result = new ArrayList<>();
        for (int x = area.x1(); x <= area.x2(); x += 8) {
            result.add(new Point(x, area.y1(), area.mapId()));
        }
        for (int y = area.y1(); y <= area.y2(); y += 8) {
            result.add(new Point(area.x2(), y, area.mapId()));
        }
        for (int x = area.x2(); x >= area.x1(); x -= 8) {
            result.add(new Point(x, area.y2(), area.mapId()));
        }
        for (int y = area.y2(); y >= area.y1(); y -= 8) {
            result.add(new Point(area.x1(), y, area.mapId()));
        }
        return List.copyOf(result);
    }

    private static List<Point> points(int mapId, int... coordinates) {
        if ((coordinates.length & 1) != 0 || coordinates.length == 0) {
            throw new IllegalArgumentException("return coordinates must be x/y pairs");
        }
        List<Point> result = new ArrayList<>();
        for (int index = 0; index < coordinates.length; index += 2) {
            result.add(new Point(coordinates[index], coordinates[index + 1], mapId));
        }
        return List.copyOf(result);
    }

    private static List<WarObject> removeMatching(
            LinkedHashMap<Long, WarObject> objects,
            java.util.function.Predicate<WarObject> predicate) {
        List<WarObject> removed = new ArrayList<>();
        objects.values().removeIf(object -> {
            if (!predicate.test(object)) return false;
            removed.add(object);
            return true;
        });
        return List.copyOf(removed);
    }

    private long nextObjectId() {
        if (nextObjectId < FIRST_OBJECT_ID || nextObjectId > LAST_OBJECT_ID) {
            nextObjectId = FIRST_OBJECT_ID;
        }
        return nextObjectId++;
    }

    private static String requireSource(String source) {
        String result = normalize(source);
        if (result.isEmpty()) throw new IllegalArgumentException("castle-war source is empty");
        return result;
    }

    private static String normalize(String source) {
        return source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
    }
}
