// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeTerrainResources;
import local.lito381.server.NativeDeTerrainV1;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldMap;

final class NativeDeTerrainWorld {
    static final String FIELD = "sourceDeTerrain";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner owner(NativeDeWorldContext nativeDeWorldContext) {
        NativeDeWorldContext nativeDeWorldContext2 = nativeDeWorldContext;
        synchronized (nativeDeWorldContext2) {
            Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
            if (owner == null) {
                nativeDeWorldContext.admit();
                owner = new Owner(nativeDeWorldContext);
                CombatAccess.set(nativeDeWorldContext, FIELD, owner);
            }
            return owner;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDeWorldContext nativeDeWorldContext, Supplier<T> supplier) {
        ReentrantReadWriteLock.ReadLock readLock = nativeDeWorldContext.admission.readLock();
        readLock.lock();
        try {
            nativeDeWorldContext.admit();
            T t = supplier.get();
            return t;
        }
        finally {
            readLock.unlock();
        }
    }

    static NativeDeTerrainV1 classic(NativeDeWorldContext nativeDeWorldContext, int n, int n2) {
        NativeDeTerrainResources.Definition definition = NativeDeTerrainResources.snapshot().maps().get(n2);
        if (definition == null) {
            throw new IllegalArgumentException("unknown source classic terrain " + n2);
        }
        return NativeDeTerrainWorld.owner((NativeDeWorldContext)nativeDeWorldContext).classic.computeIfAbsent(new Key(Domain.CLASSIC, n, n2), key -> NativeDeTerrainResources.create(definition, nativeDeWorldContext.log));
    }

    static boolean passable(NativeDeWorldContext nativeDeWorldContext, Domain domain, int n, NativeDeWorldMap nativeDeWorldMap, int n2, int n3, NativeDeCharacterFields nativeDeCharacterFields) {
        return NativeDeTerrainWorld.call(nativeDeWorldContext, () -> {
            if (nativeDeWorldMap.isNull()) {
                return false;
            }
            if (domain == Domain.CLASSIC) {
                return NativeDeTerrainWorld.classic(nativeDeWorldContext, n, nativeDeWorldMap.getId()).isPassable(n2, n3, nativeDeCharacterFields);
            }
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(nativeDeWorldMap.getId());
            return arena.nativeCanStep(n2, n3 - 1, n2, n3) || arena.nativeCanStep(n2 + 1, n3, n2, n3) || arena.nativeCanStep(n2, n3 + 1, n2, n3) || arena.nativeCanStep(n2 - 1, n3, n2, n3);
        });
    }

    static boolean step(NativeDeWorldContext nativeDeWorldContext, Domain domain, int n, int n2, int n3, int n4, int n5, NativeDeCharacterFields nativeDeCharacterFields) {
        return NativeDeTerrainWorld.call(nativeDeWorldContext, () -> {
            if (n5 < 0 || n5 > 7) {
                return false;
            }
            if (domain == Domain.CLASSIC) {
                return NativeDeTerrainWorld.classic(nativeDeWorldContext, n, n2).isPassable(n3, n4, n5, nativeDeCharacterFields);
            }
            int[] nArray = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
            int[] nArray2 = new int[]{-1, -1, 0, 1, 1, 1, 0, -1};
            return ArenaMapCatalog.forMap(n2).nativeCanStep(n3, n4, n3 + nArray[n5], n4 + nArray2[n5]);
        });
    }

    static void door(NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4, boolean bl, int n5) {
        NativeDeTerrainWorld.call(nativeDeWorldContext, () -> {
            NativeDeTerrainWorld.classic(nativeDeWorldContext, n, n2).setPassable(n3, n4, bl, n5);
            return null;
        });
    }

    static void arrow(NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4) {
        NativeDeTerrainWorld.call(nativeDeWorldContext, () -> {
            NativeDeTerrainWorld.classic(nativeDeWorldContext, n, n2).setArrowPassableOverride(n3, n4);
            return null;
        });
    }

    static boolean arrowPassable(NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4, int n5) {
        return NativeDeTerrainWorld.call(nativeDeWorldContext, () -> NativeDeTerrainWorld.classic(nativeDeWorldContext, n, n2).isArrowPassable(n3, n4, n5));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void close(NativeDeWorldContext nativeDeWorldContext) {
        NativeDeWorldContext nativeDeWorldContext2 = nativeDeWorldContext;
        synchronized (nativeDeWorldContext2) {
            Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
            if (owner != null) {
                owner.classic.clear();
            }
            CombatAccess.set(nativeDeWorldContext, FIELD, null);
        }
    }

    private NativeDeTerrainWorld() {
    }

    static final class Owner {
        final NativeDeWorldContext context;
        final Map<Key, NativeDeTerrainV1> classic = new ConcurrentHashMap<Key, NativeDeTerrainV1>();

        Owner(NativeDeWorldContext nativeDeWorldContext) {
            this.context = nativeDeWorldContext;
        }
    }

    record Key(Domain domain, int show, int map) {
    }

    static enum Domain {
        CLASSIC,
        ARENA;

    }
}
