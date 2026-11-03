package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validated runtime view of the three lito_static battle-control tables.
 * Empty tables preserve the evidence-backed built-ins used by unit fixtures;
 * production SQL rows replace only the represented rule, object or wave.
 */
final class BattleControlCatalog {
    record Rule(boolean enabled, int timeLimitSeconds, int goal, int minimumPlayers) {}
    record ObjectPlacement(String role, int x, int y, int gfx, int maximumHp) {}
    record Spawn(int wave, int sortOrder, int npcId, int quantity,
                 Integer x, Integer y) {}
    record PlayerSpawn(int spawnIndex, int x, int y) {}

    private record Key(int modeId, int mapId) {}
    private record ObjectKey(int modeId, int mapId, String role) {}

    private static final BattleControlCatalog EMPTY = new BattleControlCatalog(
            Map.of(), Map.of(), Map.of(), Map.of());

    private final Map<Key, Rule> rules;
    private final Map<ObjectKey, ObjectPlacement> objects;
    private final Map<Key, List<Spawn>> spawns;
    private final Map<Key, List<PlayerSpawn>> playerSpawns;

    private BattleControlCatalog(Map<Key, Rule> rules,
                                 Map<ObjectKey, ObjectPlacement> objects,
                                 Map<Key, List<Spawn>> spawns,
                                 Map<Key, List<PlayerSpawn>> playerSpawns) {
        this.rules = Map.copyOf(rules);
        this.objects = Map.copyOf(objects);
        Map<Key, List<Spawn>> immutableSpawns = new LinkedHashMap<>();
        spawns.forEach((key, value) -> immutableSpawns.put(key, List.copyOf(value)));
        this.spawns = Map.copyOf(immutableSpawns);
        Map<Key, List<PlayerSpawn>> immutablePlayerSpawns = new LinkedHashMap<>();
        playerSpawns.forEach((key, value) ->
                immutablePlayerSpawns.put(key, List.copyOf(value)));
        this.playerSpawns = Map.copyOf(immutablePlayerSpawns);
    }

    static BattleControlCatalog defaults() {
        return EMPTY;
    }

    /**
     * Complete built-in player deployment used to seed the operator-editable
     * static table.  The database migration inserts only missing primary keys,
     * so later coordinate edits remain authoritative across restarts.
     */
    static List<RepositoryBattlePlayerSpawn> defaultPlayerSpawns() {
        List<RepositoryBattlePlayerSpawn> rows = new ArrayList<>();
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int mapId : mode.maps()) {
                List<ArenaMapCatalog.Point> points;
                if (mode == BattleRules.Mode.PET_RACE
                        && BattlePetRaceCourseCatalog.supports(mapId)) {
                    points = BattlePetRaceCourseCatalog.forMap(mapId).spawns();
                } else if (mode == BattleRules.Mode.TIME_ATTACK
                        && BattleTimeAttackSpawnCatalog.supports(mapId)) {
                    points = BattleTimeAttackSpawnCatalog.spawns();
                } else {
                    points = ArenaMapCatalog.forMap(mapId).spawns();
                }
                for (int index = 0; index < points.size(); index++) {
                    ArenaMapCatalog.Point point = points.get(index);
                    rows.add(new RepositoryBattlePlayerSpawn(
                            mode.wireId(), mapId, index, point.x(), point.y()));
                }
            }
        }
        return List.copyOf(rows);
    }

    static BattleControlCatalog load(AccountRepository accounts) {
        if (accounts == null) return EMPTY;
        try {
            return fromRepository(accounts.battleConfiguration());
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "cannot load lito_static battle-control tables", failure);
        }
    }

    static BattleControlCatalog fromRepository(RepositoryBattleConfiguration source) {
        if (source == null) throw new IllegalArgumentException("null battle configuration");
        Map<Key, Rule> rules = new LinkedHashMap<>();
        for (RepositoryBattleRule row : source.rules()) {
            BattleRules.Mode mode = requireModeMap(row.modeId(), row.mapId(), false);
            if (row.timeLimitSeconds() != mode.defaultSeconds()
                    && (row.timeLimitSeconds() < 1 || row.timeLimitSeconds() > 86_400)
                    || row.goal() < 0 || row.goal() > 10_000
                    || row.minimumPlayers() < 0 || row.minimumPlayers() > 100) {
                throw new IllegalArgumentException("invalid battle rule row " + row);
            }
            Key key = new Key(row.modeId(), row.mapId());
            if (rules.put(key, new Rule(row.enabled(), row.timeLimitSeconds(),
                    row.goal(), row.minimumPlayers())) != null) {
                throw new IllegalArgumentException("duplicate battle rule row " + key);
            }
        }

        Map<ObjectKey, ObjectPlacement> objects = new LinkedHashMap<>();
        for (RepositoryBattleObject row : source.objects()) {
            requireModeMap(row.modeId(), row.mapId(), false);
            String role = normalizeRole(row.role());
            if (role.isEmpty() || row.gfx() <= 0 || row.maximumHp() < 1
                    || row.maximumHp() > 10_000_000) {
                throw new IllegalArgumentException("invalid battle object row " + row);
            }
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(row.mapId());
            if (!arena.nativeStandingFloor(row.x(), row.y())) {
                throw new IllegalArgumentException("battle object is not on native floor " + row);
            }
            ObjectKey key = new ObjectKey(row.modeId(), row.mapId(), role);
            if (objects.put(key, new ObjectPlacement(
                    role, row.x(), row.y(), row.gfx(), row.maximumHp())) != null) {
                throw new IllegalArgumentException("duplicate battle object row " + key);
            }
        }

        Map<Key, List<Spawn>> spawns = new LinkedHashMap<>();
        Set<String> spawnKeys = new LinkedHashSet<>();
        for (RepositoryBattleSpawn row : source.spawns()) {
            if (row.mapId() != 0) requireModeMap(row.modeId(), row.mapId(), false);
            else BattleRules.Mode.fromWire(row.modeId());
            if (row.wave() < 1 || row.wave() > 255 || row.sortOrder() < 0
                    || row.npcId() <= 0 || row.quantity() < 1 || row.quantity() > 100
                    || (row.x() == null) != (row.y() == null)
                    || row.quantity() != 1 && row.x() != null) {
                throw new IllegalArgumentException("invalid battle spawn row " + row);
            }
            if (row.x() != null && row.mapId() == 0) {
                throw new IllegalArgumentException(
                        "wildcard battle spawn cannot use absolute coordinates " + row);
            }
            if (row.x() != null && !ArenaMapCatalog.forMap(row.mapId())
                    .nativeStandingFloor(row.x(), row.y())) {
                throw new IllegalArgumentException("battle spawn is not on native floor " + row);
            }
            String duplicateKey = row.modeId() + ":" + row.mapId() + ":"
                    + row.wave() + ":" + row.sortOrder();
            if (!spawnKeys.add(duplicateKey)) {
                throw new IllegalArgumentException("duplicate battle spawn order " + duplicateKey);
            }
            spawns.computeIfAbsent(new Key(row.modeId(), row.mapId()), ignored ->
                    new ArrayList<>()).add(new Spawn(row.wave(), row.sortOrder(),
                    row.npcId(), row.quantity(), row.x(), row.y()));
        }
        spawns.values().forEach(rows -> rows.sort(Comparator
                .comparingInt(Spawn::wave).thenComparingInt(Spawn::sortOrder)));

        Map<Key, List<PlayerSpawn>> playerSpawns = new LinkedHashMap<>();
        Set<String> playerSpawnKeys = new LinkedHashSet<>();
        for (RepositoryBattlePlayerSpawn row : source.playerSpawns()) {
            BattleRules.Mode mode = requireModeMap(row.modeId(), row.mapId(), false);
            if (!mode.isBattleScene() || row.spawnIndex() < 0 || row.spawnIndex() > 99) {
                throw new IllegalArgumentException("invalid player spawn row " + row);
            }
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(row.mapId());
            if (!arena.nativeStandingFloor(row.x(), row.y())) {
                throw new IllegalArgumentException(
                        "player spawn is not on native floor " + row);
            }
            String duplicateKey = row.modeId() + ":" + row.mapId() + ":"
                    + row.spawnIndex();
            if (!playerSpawnKeys.add(duplicateKey)) {
                throw new IllegalArgumentException(
                        "duplicate player spawn index " + duplicateKey);
            }
            playerSpawns.computeIfAbsent(
                    new Key(row.modeId(), row.mapId()), ignored -> new ArrayList<>())
                    .add(new PlayerSpawn(
                            row.spawnIndex(), row.x(), row.y()));
        }
        for (Map.Entry<Key, List<PlayerSpawn>> entry : playerSpawns.entrySet()) {
            List<PlayerSpawn> rows = entry.getValue();
            rows.sort(Comparator.comparingInt(PlayerSpawn::spawnIndex));
            Set<ArenaMapCatalog.Point> occupied = new LinkedHashSet<>();
            for (int index = 0; index < rows.size(); index++) {
                PlayerSpawn row = rows.get(index);
                if (row.spawnIndex() != index) {
                    throw new IllegalArgumentException(
                            "player spawn indices must be contiguous from zero "
                                    + entry.getKey());
                }
                if (!occupied.add(new ArenaMapCatalog.Point(row.x(), row.y()))) {
                    throw new IllegalArgumentException(
                            "duplicate player spawn coordinate " + entry.getKey());
                }
            }
        }
        return new BattleControlCatalog(rules, objects, spawns, playerSpawns);
    }

    BattleRules.RuleSet apply(BattleRules.RuleSet fallback) {
        Rule rule = rules.get(new Key(fallback.mode().wireId(), fallback.mapId()));
        if (rule == null) return fallback;
        if (!rule.enabled()) {
            throw new IllegalArgumentException("database-disabled LiTo battle map "
                    + fallback.mode().wireId() + "/" + fallback.mapId());
        }
        return new BattleRules.RuleSet(fallback.mode(), fallback.mapId(),
                fallback.mapPool(), rule.timeLimitSeconds(), rule.goal(),
                rule.minimumPlayers(), fallback.teamBased(), fallback.respawn(),
                fallback.objective());
    }

    int firstEnabledMap(BattleRules.Mode mode) {
        for (int mapId : mode.maps()) {
            Rule rule = rules.get(new Key(mode.wireId(), mapId));
            if (rule == null || rule.enabled()) return mapId;
        }
        throw new IllegalArgumentException("all database maps disabled for mode " + mode.wireId());
    }

    ObjectPlacement object(BattleRules.Mode mode, int mapId, String role) {
        return objects.get(new ObjectKey(mode.wireId(), mapId, normalizeRole(role)));
    }

    List<Spawn> spawns(BattleRules.Mode mode, int mapId) {
        List<Spawn> exact = spawns.get(new Key(mode.wireId(), mapId));
        if (exact != null && !exact.isEmpty()) return exact;
        return spawns.getOrDefault(new Key(mode.wireId(), 0), List.of());
    }

    /** Database rows override every built-in deployment for that exact mode/map. */
    ArenaMapCatalog.Point playerSpawn(
            BattleRules.Mode mode, ArenaMapCatalog.Arena arena, int spawnIndex) {
        if (mode == null || arena == null) {
            throw new IllegalArgumentException("battle mode and arena are required");
        }
        List<PlayerSpawn> configured = playerSpawns.get(
                new Key(mode.wireId(), arena.mapId()));
        if (configured != null && !configured.isEmpty()) {
            PlayerSpawn selected = configured.get(Math.floorMod(
                    spawnIndex, configured.size()));
            return new ArenaMapCatalog.Point(selected.x(), selected.y());
        }
        if (mode == BattleRules.Mode.PET_RACE
                && BattlePetRaceCourseCatalog.supports(arena.mapId())) {
            return BattlePetRaceCourseCatalog.forMap(arena.mapId()).spawn(spawnIndex);
        }
        if (mode == BattleRules.Mode.TIME_ATTACK
                && BattleTimeAttackSpawnCatalog.supports(arena.mapId())) {
            return BattleTimeAttackSpawnCatalog.spawn(spawnIndex);
        }
        return arena.spawn(spawnIndex);
    }

    int configuredRuleCount() { return rules.size(); }
    int configuredObjectCount() { return objects.size(); }
    int configuredSpawnRowCount() {
        return spawns.values().stream().mapToInt(List::size).sum();
    }
    int configuredPlayerSpawnRowCount() {
        return playerSpawns.values().stream().mapToInt(List::size).sum();
    }

    private static BattleRules.Mode requireModeMap(int modeId, int mapId,
                                                    boolean wildcardAllowed) {
        BattleRules.Mode mode = BattleRules.Mode.fromWire(modeId);
        if (mapId == 0 && wildcardAllowed) return mode;
        if (!mode.maps().contains(mapId)) {
            throw new IllegalArgumentException("map does not belong to mode "
                    + modeId + "/" + mapId);
        }
        return mode;
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
