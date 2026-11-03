package local.lito381.server;

/** Static Source terrain for the existing BATTLE LOS entry. Dynamic room door overlays remain separate. */
final class NativeCloseoutBattleLineOfSight {
    static final String FIELD = "nativeCloseoutSourceTerrain";
    private record Cache(NativeDeTerrainResources.Snapshot snapshot, NativeDeTerrainV1 terrain) {}
    private static final NativeDeWorldLog LOG = (level, message, values) ->
            System.err.println("Source BATTLE terrain " + level + ": " + message + " " + java.util.Arrays.toString(values));

    static Boolean source(ArenaMapCatalog.Arena arena, int x, int y, int tx, int ty) {
        if (!CombatRules.loaded()) return null;
        NativeDeTerrainResources.Snapshot snapshot;
        // Existing legacy mode remains available only before the original Source terrain loader publishes.
        try { snapshot = NativeDeTerrainResources.snapshot(); }
        catch (NullPointerException notLoaded) { return null; }
        if (arena == null || !arena.contains(x, y) || !arena.contains(tx, ty)) return false;
        var definition = snapshot.maps().get(arena.mapId());
        if (definition == null || definition.id() != arena.mapId()) return false;
        Cache cache;
        synchronized (arena) {
            Object previous = CombatAccess.field(arena, FIELD);
            if (previous instanceof Cache kept && kept.snapshot() == snapshot) cache = kept;
            else {
                // create() reads the existing hash-verified resource with the original copy-map definition;
                // it opens no SQL connection. No actor, room, scope or timer is retained by this cache.
                cache = new Cache(snapshot, NativeDeTerrainResources.create(definition, LOG));
                CombatAccess.set(arena, FIELD, cache);
            }
        }
        return NativeCloseoutSourceLineOfSight.hasLineOfSight(cache.terrain(), x, y, tx, ty);
    }
    private NativeCloseoutBattleLineOfSight() {}
}
