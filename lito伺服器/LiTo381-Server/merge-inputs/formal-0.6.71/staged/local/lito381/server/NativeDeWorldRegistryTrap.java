// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeWorldAccess;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryTrap {
    private final ConcurrentHashMap<Integer, NativeDeWorldKinds.Trap> _isTrap = new ConcurrentHashMap();
    private final ConcurrentHashMap<TrapCell, Set<NativeDeWorldKinds.Trap>> _byCell = new ConcurrentHashMap();
    private final ConcurrentHashMap<Integer, TrapCell> _indexedCellById = new ConcurrentHashMap();

    private NativeDeWorldRegistryTrap() {
    }

    public Map<Integer, NativeDeWorldKinds.Trap> map() {
        return Collections.unmodifiableMap(this._isTrap);
    }

    public void put(Integer key, NativeDeWorldKinds.Trap value) {
        try {
            if (key == null || value == null) {
                return;
            }
            NativeDeWorldKinds.Trap previous = this._isTrap.put(key, value);
            if (previous != null && previous != value) {
                this.unindex(key, previous);
            }
            this.reindex(value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            if (key == null) {
                return;
            }
            NativeDeWorldKinds.Trap removed = this._isTrap.remove(key);
            if (removed != null) {
                this.unindex(key, removed);
            }
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public boolean remove(Integer key, NativeDeWorldKinds.Trap expected) {
        if (key == null || expected == null || !this._isTrap.remove(key, expected)) {
            return false;
        }
        this.unindex(key, expected);
        return true;
    }

    public void reindex(NativeDeWorldKinds.Trap trap) {
        if (trap == null || this._isTrap.get(NativeDeWorldAccess.L1TrapInstance$getId(trap)) != trap) {
            return;
        }
        TrapCell next = TrapCell.of(trap);
        this._indexedCellById.compute(NativeDeWorldAccess.L1TrapInstance$getId(trap), (id, previous) -> {
            if (this._isTrap.get(id) != trap) {
                this.removeFromCell((TrapCell)previous, trap);
                return null;
            }
            if (previous != null && !previous.equals(next)) {
                this.removeFromCell((TrapCell)previous, trap);
            }
            this._byCell.computeIfAbsent(next, ignored -> ConcurrentHashMap.newKeySet()).add(trap);
            return next;
        });
    }

    private void unindex(Integer key, NativeDeWorldKinds.Trap trap) {
        this._indexedCellById.compute(key, (id, previous) -> {
            this.removeFromCell((TrapCell)previous, trap);
            return null;
        });
    }

    private void removeFromCell(TrapCell cell, NativeDeWorldKinds.Trap trap) {
        if (cell == null || trap == null) {
            return;
        }
        this._byCell.computeIfPresent(cell, (ignored, traps) -> {
            traps.remove(trap);
            return traps.isEmpty() ? null : traps;
        });
    }

    public static NativeDeWorldRegistryTrap get() {
        return (NativeDeWorldRegistryTrap)NativeDeWorldScope.registry("Trap");
    }

    private record TrapCell(int mapId, int x, int y) {
        private static TrapCell of(NativeDeWorldKinds.Trap trap) {
            return new TrapCell(NativeDeWorldAccess.L1TrapInstance$getMapId(trap), NativeDeWorldAccess.L1TrapInstance$getX(trap), NativeDeWorldAccess.L1TrapInstance$getY(trap));
        }
    }
}

