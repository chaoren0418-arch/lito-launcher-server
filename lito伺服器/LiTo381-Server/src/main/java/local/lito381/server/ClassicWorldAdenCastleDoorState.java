package local.lito381.server;

import java.util.List;

/** Server-owned state for the selected-source Aden inner gate on map4. */
final class ClassicWorldAdenCastleDoorState {
    record Snapshot(
            ClassicWorldAdenToiCatalog.Door definition,
            int currentHp, boolean open) {
        Snapshot {
            if (definition == null || currentHp != 0) {
                throw new IllegalArgumentException("invalid Aden inner-gate snapshot");
            }
        }

        boolean destructible() {
            return false;
        }
    }

    private boolean open;

    synchronized List<Snapshot> snapshots() {
        return List.of(snapshot());
    }

    synchronized List<Snapshot> setOpenForKeeper(int keeperNpcId, boolean nextOpen) {
        ClassicWorldAdenToiCatalog.Door door = ClassicWorldAdenToiCatalog.innerGate();
        if (door.keeperNpcId() != keeperNpcId) return List.of();
        open = nextOpen;
        return List.of(snapshot());
    }

    synchronized int repairAll() {
        open = false;
        return 1;
    }

    synchronized boolean blocks(int x, int y) {
        return !open && ClassicWorldAdenToiCatalog.innerGate().controls(x, y);
    }

    private Snapshot snapshot() {
        return new Snapshot(ClassicWorldAdenToiCatalog.innerGate(), 0, open);
    }
}
