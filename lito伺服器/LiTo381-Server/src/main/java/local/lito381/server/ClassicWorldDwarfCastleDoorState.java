package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-owned open/closed and hit-point state for the three map66 gates. */
final class ClassicWorldDwarfCastleDoorState {
    record Snapshot(
            ClassicWorldDwarfCastleCatalog.Door definition,
            int currentHp, boolean open, int crackStage) {
        Snapshot {
            if (definition == null || currentHp < 0
                    || currentHp > definition.maximumHp()
                    || crackStage < 0 || crackStage > 5
                    || currentHp == 0 && definition.maximumHp() > 1 && !open) {
                throw new IllegalArgumentException("invalid Dwarf Castle door snapshot");
            }
        }

        boolean destructible() {
            return definition.maximumHp() > 1;
        }
    }

    record DoorHit(Snapshot before, Snapshot after, int appliedDamage,
                   int action, boolean diedNow) {
        DoorHit {
            if (before == null || after == null || appliedDamage < 0
                    || before.definition().objectId()
                        != after.definition().objectId()
                    || before.currentHp() - after.currentHp() != appliedDamage
                    || action < 0 || action > 0xFF
                    || diedNow != (before.currentHp() > 0
                        && after.currentHp() == 0)) {
                throw new IllegalArgumentException(
                        "invalid Dwarf Castle door damage transition");
            }
        }
    }

    private record Mutable(int currentHp, boolean open, int crackStage) {}

    private final Map<Integer, Mutable> states = new LinkedHashMap<>();

    ClassicWorldDwarfCastleDoorState() {
        for (ClassicWorldDwarfCastleCatalog.Door door
                : ClassicWorldDwarfCastleCatalog.doors()) {
            // Retained L1DoorInstance.initialize closes every keeper-owned door.
            states.put(door.doorId(), new Mutable(door.maximumHp(), false, 0));
        }
    }

    synchronized List<Snapshot> snapshots() {
        return ClassicWorldDwarfCastleCatalog.doors().stream()
                .map(door -> {
                    Mutable state = states.get(door.doorId());
                    return snapshot(door, state);
                }).toList();
    }

    synchronized Snapshot snapshot(int mapId, long objectId) {
        if (mapId != ClassicWorldDwarfCastleCatalog.MAP_ID) return null;
        ClassicWorldDwarfCastleCatalog.Door door = door(objectId);
        return door == null ? null : snapshot(door, states.get(door.doorId()));
    }

    /**
     * Door combat uses the nearest retained entrance edge.  The database actor
     * coordinate lies behind a closed diagonal gate; using it directly would
     * make a range-one L1AttackPc impossible from the legal standing tile.
     */
    synchronized ClassicWorldMonsterPopulation.Snapshot combatSnapshot(
            int mapId, long objectId, int attackerX, int attackerY) {
        Snapshot door = snapshot(mapId, objectId);
        if (door == null || !door.destructible() || door.currentHp() <= 0) return null;
        ClassicWorldDwarfCastleCatalog.Door definition = door.definition();
        int combatX = definition.direction() == 0
                ? clamp(attackerX, definition.edgeLow(), definition.edgeHigh())
                : definition.entranceX();
        int combatY = definition.direction() == 0
                ? definition.entranceY()
                : clamp(attackerY, definition.edgeLow(), definition.edgeHigh());
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        ClassicWorldCastleDoorCombat.NPC_TEMPLATE_ID,
                        "", "L1Monster", definition.gfxId(),
                        1, definition.maximumHp(), 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        "large", 0, 0, 0, 0, 0, 0,
                        0, 0, false, 0, false, false, false, 0, false);
        ClassicWorldMonsterCatalog.SpawnDefinition spawn =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        definition.mapId(), definition.doorId(),
                        "Dwarf Castle door", 1, 0,
                        combatX, combatY, 0, 0, 0, 0, 0, 0,
                        definition.direction(), 0, 0, false, 0, true, 0, template);
        return new ClassicWorldMonsterPopulation.Snapshot(
                definition.mapId(), definition.doorId(), 1,
                definition.objectId(), ClassicWorldCastleDoorCombat.NPC_TEMPLATE_ID,
                combatX, combatY, definition.direction(),
                door.currentHp(), definition.maximumHp(), 0, 0,
                true, true, spawn);
    }

    synchronized List<Snapshot> setOpenForKeeper(int keeperNpcId, boolean open) {
        List<ClassicWorldDwarfCastleCatalog.Door> matching =
                ClassicWorldDwarfCastleCatalog.doors().stream()
                        .filter(door -> door.keeperNpcId() == keeperNpcId)
                        .toList();
        for (ClassicWorldDwarfCastleCatalog.Door door : matching) {
            Mutable previous = states.get(door.doorId());
            boolean effectiveOpen = previous.currentHp() == 0 || open;
            states.put(door.doorId(), new Mutable(
                    previous.currentHp(), effectiveOpen, previous.crackStage()));
        }
        return matching.stream().map(door -> {
            Mutable state = states.get(door.doorId());
            return snapshot(door, state);
        }).toList();
    }

    /** Exact retained maxHp sixths and actions 32..37 from L1DoorInstance. */
    synchronized DoorHit damage(int mapId, long objectId, int damage) {
        if (damage <= 0 || mapId != ClassicWorldDwarfCastleCatalog.MAP_ID) return null;
        ClassicWorldDwarfCastleCatalog.Door door = door(objectId);
        if (door == null || door.maximumHp() <= 1) return null;
        Mutable previous = states.get(door.doorId());
        if (previous.currentHp() <= 0) return null;
        Snapshot before = snapshot(door, previous);
        int nextHp = Math.max(0, previous.currentHp() - damage);
        int applied = previous.currentHp() - nextHp;
        boolean diedNow = nextHp == 0;
        int nextCrack = diedNow ? previous.crackStage()
                : crackStage(nextHp, door.maximumHp());
        int action = diedNow ? BattleActionCodes.DOOR_DIE
                : nextCrack != 0 && nextCrack != previous.crackStage()
                    ? BattleActionCodes.DOOR_CRACK_1 + nextCrack - 1 : 0;
        Mutable updated = new Mutable(
                nextHp, diedNow || previous.open(), nextCrack);
        states.put(door.doorId(), updated);
        return new DoorHit(before, snapshot(door, updated), applied, action, diedNow);
    }

    synchronized int repairAll() {
        int repaired = 0;
        for (ClassicWorldDwarfCastleCatalog.Door door
                : ClassicWorldDwarfCastleCatalog.doors()) {
            Mutable previous = states.get(door.doorId());
            states.put(door.doorId(), new Mutable(door.maximumHp(), false, 0));
            if (previous.currentHp() != door.maximumHp() || previous.open()) repaired++;
        }
        return repaired == 0 ? states.size() : repaired;
    }

    synchronized boolean blocks(int x, int y) {
        for (ClassicWorldDwarfCastleCatalog.Door door
                : ClassicWorldDwarfCastleCatalog.doors()) {
            Mutable state = states.get(door.doorId());
            if (!state.open() && door.controls(x, y)) return true;
        }
        return false;
    }

    private static Snapshot snapshot(
            ClassicWorldDwarfCastleCatalog.Door door, Mutable state) {
        return new Snapshot(
                door, state.currentHp(), state.open(), state.crackStage());
    }

    private static ClassicWorldDwarfCastleCatalog.Door door(long objectId) {
        return ClassicWorldDwarfCastleCatalog.doors().stream()
                .filter(candidate -> candidate.objectId() == objectId)
                .findFirst().orElse(null);
    }

    private static int crackStage(int hp, int maximumHp) {
        if (maximumHp * 1 / 6 > hp) return 5;
        if (maximumHp * 2 / 6 > hp) return 4;
        if (maximumHp * 3 / 6 > hp) return 3;
        if (maximumHp * 4 / 6 > hp) return 2;
        if (maximumHp * 5 / 6 > hp) return 1;
        return 0;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
