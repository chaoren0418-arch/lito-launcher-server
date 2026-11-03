package local.lito381.server;

import java.util.Map;
import java.util.Objects;

/** Per-evaluation room door layer; never publishes dynamic state into an arena's shared terrain. */
final class NativeCloseoutNpcAreaDoors {
    static NativeDeTerrainV1 overlay(NativeDeTerrainV1 base, BattleDoor.Snapshot door, BattleDoor.Snapshot gate) {
        var terrain = new NativeDeTerrainV1();
        // Only readonly tile/arrow arrays and their scalar bounds are shared. The constructor owns a fresh door map.
        for (String field : new String[]{"_mapId", "_worldTopLeftX", "_worldTopLeftY", "_worldBottomRightX",
                "_worldBottomRightY", "_map", "_arrowPassableOverrideMap"})
            CombatAccess.set(terrain, field, CombatAccess.field(base, field));
        @SuppressWarnings("unchecked") var own = (Map<Integer, Byte>) CombatAccess.field(terrain, "_doorOverrides");
        @SuppressWarnings("unchecked") var original = (Map<Integer, Byte>) CombatAccess.field(base, "_doorOverrides");
        own.putAll(original);
        close(terrain, door); close(terrain, gate);
        return terrain;
    }
    static void close(NativeDeTerrainV1 terrain, BattleDoor.Snapshot door) {
        if (door != null && !door.open()) {
            // Source setPassable supplies the center and two side cells for this door direction.
            // Open/inactive doors contribute no override: original terrain decides passability.
            terrain.setPassable(door.x(), door.y(), false, door.direction());
        }
    }
    static boolean lineOfSight(ArenaMapCatalog.Arena arena, int x, int y, int tx, int ty,
                               NativeCloseoutNpcAreaTargets.Selection proof) {
        if (arena == null || !arena.contains(x, y) || !arena.contains(tx, ty)) return false;
        // Initialize/reuse the original readonly cache without applying the unoverlaid ray as a veto.
        Boolean ready = NativeCloseoutBattleLineOfSight.source(arena, x, y, x, y);
        if (ready == null) return BattleSpellCatalog.hasNativeLineOfSight(arena, x, y, tx, ty);
        if (!ready) return false;
        Object cache = CombatAccess.field(arena, NativeCloseoutBattleLineOfSight.FIELD);
        if (cache == null) return false;
        var base = (NativeDeTerrainV1) CombatAccess.field(cache, "terrain");
        var hub = proof.scope.origin().hub(); long id = proof.scope.origin().room();
        Object room; BattleDoor.Snapshot door, gate;
        synchronized (hub) {
            room = ((Map<?, ?>) CombatAccess.field(hub, "rooms")).get(id);
            if (room == null || !hub.battleStarted(id)) return false;
            // Door placement belongs to the room's base arena, not another Quest force's map.
            if (hub.roomRules(id).mapId() != arena.mapId())
                return NativeCloseoutSourceLineOfSight.hasLineOfSight(base, x, y, tx, ty);
            door = hub.battleDoor(id); gate = hub.siegeGate(id);
        }
        if (!direction(door) || !direction(gate)) return false;
        boolean result = NativeCloseoutSourceLineOfSight.hasLineOfSight(overlay(base, door, gate), x, y, tx, ty);
        synchronized (hub) {
            return result && ((Map<?, ?>) CombatAccess.field(hub, "rooms")).get(id) == room
                    && hub.battleStarted(id) && hub.roomRules(id).mapId() == arena.mapId()
                    && Objects.equals(door, hub.battleDoor(id)) && Objects.equals(gate, hub.siegeGate(id));
        }
    }
    static boolean direction(BattleDoor.Snapshot door) {
        return door == null || door.open() || door.direction() == 0 || door.direction() == 1;
    }
    private NativeCloseoutNpcAreaDoors() {}
}
