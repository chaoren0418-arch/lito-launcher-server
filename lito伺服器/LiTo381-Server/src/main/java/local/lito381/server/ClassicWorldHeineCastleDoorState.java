package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-owned selected-source state for the three map4 Heine Castle gates. */
final class ClassicWorldHeineCastleDoorState {
    record Snapshot(ClassicWorldHeineCatalog.Door definition,
                    int currentHp, boolean open, int crackStage) {
        Snapshot {
            if (definition == null || currentHp < 0
                    || currentHp > definition.maximumHp()
                    || crackStage < 0 || crackStage > 5) {
                throw new IllegalArgumentException("invalid Heine Castle door snapshot");
            }
        }

        boolean destructible() { return definition.maximumHp() > 1; }
    }

    private record Mutable(int currentHp, boolean open, int crackStage) {}
    private final Map<Integer, Mutable> states = new LinkedHashMap<>();

    ClassicWorldHeineCastleDoorState() {
        for (ClassicWorldHeineCatalog.Door door : ClassicWorldHeineCatalog.doors()) {
            states.put(door.doorId(), new Mutable(door.maximumHp(), false, 0));
        }
    }

    synchronized List<Snapshot> snapshots() {
        return ClassicWorldHeineCatalog.doors().stream().map(door -> {
            Mutable state = states.get(door.doorId());
            return snapshot(door, state);
        }).toList();
    }

    synchronized List<ClassicWorldCastleDoorCombat.View> views() {
        return ClassicWorldHeineCatalog.doors().stream()
                .map(door -> view(door, states.get(door.doorId()))).toList();
    }

    synchronized ClassicWorldCastleDoorCombat.Target combatSnapshot(
            int mapId, long objectId, int attackerX, int attackerY) {
        ClassicWorldHeineCatalog.Door door = door(objectId);
        if (door == null || door.mapId() != mapId) return null;
        return ClassicWorldCastleDoorCombat.target(
                view(door, states.get(door.doorId())), attackerX, attackerY);
    }

    synchronized ClassicWorldCastleDoorCombat.Hit damage(
            int mapId, long objectId, int damage) {
        ClassicWorldHeineCatalog.Door door = door(objectId);
        if (door == null || door.mapId() != mapId) return null;
        Mutable beforeState = states.get(door.doorId());
        ClassicWorldCastleDoorCombat.View before = view(door, beforeState);
        ClassicWorldCastleDoorCombat.DamageResult result =
                ClassicWorldCastleDoorCombat.damage(before, damage);
        if (result == null) return null;
        Mutable afterState = new Mutable(result.nextHp(), result.nextOpen(),
                result.nextCrackStage());
        states.put(door.doorId(), afterState);
        return new ClassicWorldCastleDoorCombat.Hit(
                before, view(door, afterState), result.appliedDamage(), result.action());
    }

    synchronized List<Snapshot> setOpenForKeeper(int keeperNpcId, boolean open) {
        List<ClassicWorldHeineCatalog.Door> matching =
                ClassicWorldHeineCatalog.doors().stream()
                        .filter(door -> door.keeperNpcId() == keeperNpcId).toList();
        for (ClassicWorldHeineCatalog.Door door : matching) {
            Mutable previous = states.get(door.doorId());
            states.put(door.doorId(), new Mutable(
                    previous.currentHp(), previous.currentHp() == 0 || open,
                    previous.crackStage()));
        }
        return matching.stream().map(door -> {
            Mutable state = states.get(door.doorId());
            return snapshot(door, state);
        }).toList();
    }

    synchronized int repairAll() {
        int repaired = 0;
        for (ClassicWorldHeineCatalog.Door door : ClassicWorldHeineCatalog.doors()) {
            Mutable previous = states.get(door.doorId());
            states.put(door.doorId(), new Mutable(door.maximumHp(), false, 0));
            if (previous.currentHp() != door.maximumHp() || previous.open()) repaired++;
        }
        return repaired == 0 ? states.size() : repaired;
    }

    synchronized boolean blocks(int x, int y) {
        for (ClassicWorldHeineCatalog.Door door : ClassicWorldHeineCatalog.doors()) {
            Mutable state = states.get(door.doorId());
            if (!state.open() && door.controls(x, y)) return true;
        }
        return false;
    }

    private static Snapshot snapshot(
            ClassicWorldHeineCatalog.Door door, Mutable state) {
        return new Snapshot(door, state.currentHp(), state.open(), state.crackStage());
    }

    private static ClassicWorldCastleDoorCombat.View view(
            ClassicWorldHeineCatalog.Door door, Mutable state) {
        return new ClassicWorldCastleDoorCombat.View(
                definition(door), state.currentHp(), state.open(), state.crackStage());
    }

    private static ClassicWorldCastleDoorCombat.Definition definition(
            ClassicWorldHeineCatalog.Door door) {
        return new ClassicWorldCastleDoorCombat.Definition(
                ClassicWorldCastleDoorCombat.Family.HEINE,
                ClassicWorldHeineCatalog.CASTLE_ID, door.doorId(), door.objectId(),
                door.gfxId(), door.x(), door.y(), door.mapId(), door.direction(),
                door.edgeLow(), door.edgeHigh(), door.maximumHp(), door.keeperNpcId());
    }

    private static ClassicWorldHeineCatalog.Door door(long objectId) {
        return ClassicWorldHeineCatalog.doors().stream()
                .filter(door -> door.objectId() == objectId).findFirst().orElse(null);
    }
}
