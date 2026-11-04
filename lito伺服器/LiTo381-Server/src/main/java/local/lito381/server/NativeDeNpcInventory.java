// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionPorts;
import local.lito381.server.NativeDeNpcInventoryScope;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeWorldKinds;

class NativeDeNpcInventory
extends NativeDeObjectFields
implements NativeDeDeletionPorts.Inventory {
    final NativeDeNpcConstructionPorts.Services nativeServices;
    protected List<NativeDeWorldKinds.Item> _items = new CopyOnWriteArrayList<NativeDeWorldKinds.Item>();
    protected volatile boolean _weightDirty = true;
    protected int _cachedWeight = 0;
    protected final ReentrantLock _weightMutex = new ReentrantLock();
    private final ReentrantLock _invLock = new ReentrantLock();

    @Override
    public void clearItems() {
        NativeDeNpcInventoryScope.run(this.nativeServices, this::sourceClearItems);
    }

    static boolean countAdditionFits(long l, long l2) {
        return l >= 0L && l2 > 0L && l <= Long.MAX_VALUE - l2;
    }

    static long checkedAddedWeight(int n, long l) {
        if (n < 0 || l <= 0L) {
            return -1L;
        }
        if (n == 0) {
            return 0L;
        }
        try {
            return Math.max(Math.multiplyExact((long)n, l) / 1000L, 1L);
        }
        catch (ArithmeticException arithmeticException) {
            return -1L;
        }
    }

    public NativeDeNpcInventory(NativeDeNpcConstructionPorts.Services services) {
        this.nativeServices = Objects.requireNonNull(services);
    }

    public int getSize() {
        if (this._items.isEmpty()) {
            return 0;
        }
        return this._items.size();
    }

    public List<NativeDeWorldKinds.Item> getItems() {
        return this._items;
    }

    private void sourceClearItems() {
        this._weightMutex.lock();
        try {
            this._weightDirty = true;
        }
        finally {
            this._weightMutex.unlock();
        }
        Iterator<NativeDeWorldKinds.Item> iterator = this._items.iterator();
        while (iterator.hasNext()) {
            NativeDeWorldKinds.Item item;
            NativeDeWorldKinds.Item item2 = item = iterator.next();
            NativeDeNpcInventoryScope.World.get().removeObject(item2);
        }
    }

    @Override
    protected int getId() {
        return 0;
    }
}
