package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-owned hit-point state for the four retained Kent Castle gates. */
final class ClassicWorldKentCastleDoorState {
    record Snapshot(ClassicWorldKentCastleCatalog.Door definition, int currentHp,
                    boolean open, int crackStage) {
        Snapshot {
            if (definition == null || currentHp < 0
                    || currentHp > definition.maximumHp()
                    || crackStage < 0 || crackStage > 5) {
                throw new IllegalArgumentException("invalid Kent door snapshot");
            }
        }

        boolean destructible() {
            return definition.maximumHp() > 1;
        }
    }

    private record Mutable(int currentHp, boolean open, int crackStage) {}
    private final Map<Integer, Mutable> states = new LinkedHashMap<>();

    ClassicWorldKentCastleDoorState() {
        for (ClassicWorldKentCastleCatalog.Door door
                : ClassicWorldKentCastleCatalog.doors()) {
            states.put(door.doorId(), new Mutable(door.maximumHp(), false, 0));
        }
    }

    synchronized List<Snapshot> snapshots() {
        return ClassicWorldKentCastleCatalog.doors().stream()
                .map(door -> snapshot(door, states.get(door.doorId())))
                .toList();
    }

    synchronized List<ClassicWorldCastleDoorCombat.View> views() {
        return ClassicWorldKentCastleCatalog.doors().stream()
                .map(door -> view(door, states.get(door.doorId()))).toList();
    }

    synchronized ClassicWorldCastleDoorCombat.Target combatSnapshot(
            int mapId, long objectId, int attackerX, int attackerY) {
        ClassicWorldKentCastleCatalog.Door door = door(objectId);
        if (door == null || door.mapId() != mapId) return null;
        return ClassicWorldCastleDoorCombat.target(
                view(door, states.get(door.doorId())), attackerX, attackerY);
    }

    synchronized ClassicWorldCastleDoorCombat.Hit damage(
            int mapId, long objectId, int damage) {
        ClassicWorldKentCastleCatalog.Door door = door(objectId);
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

    synchronized boolean blocks(int mapId, int x, int y) {
        return ClassicWorldKentCastleCatalog.doors().stream().anyMatch(door -> {
            Mutable state = states.get(door.doorId());
            return door.mapId() == mapId && !state.open()
                    && definition(door).controls(x, y);
        });
    }

    synchronized int repairAll() {
        int repaired = 0;
        for (ClassicWorldKentCastleCatalog.Door door
                : ClassicWorldKentCastleCatalog.doors()) {
            Mutable previous = states.get(door.doorId());
            states.put(door.doorId(), new Mutable(door.maximumHp(), false, 0));
            if (previous.currentHp() != door.maximumHp() || previous.open()) repaired++;
        }
        return repaired == 0 ? states.size() : repaired;
    }

    private static Snapshot snapshot(
            ClassicWorldKentCastleCatalog.Door door, Mutable state) {
        return new Snapshot(door, state.currentHp(), state.open(), state.crackStage());
    }

    private static ClassicWorldCastleDoorCombat.View view(
            ClassicWorldKentCastleCatalog.Door door, Mutable state) {
        return new ClassicWorldCastleDoorCombat.View(
                definition(door), state.currentHp(), state.open(), state.crackStage());
    }

    private static ClassicWorldCastleDoorCombat.Definition definition(
            ClassicWorldKentCastleCatalog.Door door) {
        return new ClassicWorldCastleDoorCombat.Definition(
                ClassicWorldCastleDoorCombat.Family.KENT,
                ClassicWorldKentCastleCatalog.CASTLE_ID, door.doorId(), door.objectId(),
                door.gfxId(), door.x(), door.y(), door.mapId(), door.direction(),
                door.edgeLow(), door.edgeHigh(), door.maximumHp(), door.keeperNpcId());
    }

    private static ClassicWorldKentCastleCatalog.Door door(long objectId) {
        return ClassicWorldKentCastleCatalog.doors().stream()
                .filter(door -> door.objectId() == objectId).findFirst().orElse(null);
    }
}
