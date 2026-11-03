// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import java.util.TreeMap;
import local.lito381.server.LitoOwnedInventoryItem;

record RepositoryPrivateInventorySnapshot(boolean authoritative, long snapshotVersion, List<LitoOwnedInventoryItem> items) {
    RepositoryPrivateInventorySnapshot {
        if (snapshotVersion < 0L || snapshotVersion > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid private inventory version");
        }
        TreeMap<Long, LitoOwnedInventoryItem> normalized = new TreeMap<Long, LitoOwnedInventoryItem>();
        for (LitoOwnedInventoryItem litoOwnedInventoryItem : items == null ? List.of() : items) {
            if (litoOwnedInventoryItem != null && normalized.putIfAbsent(litoOwnedInventoryItem.objectId(), litoOwnedInventoryItem) == null) continue;
            throw new IllegalArgumentException("duplicate/null private inventory object");
        }
        items = List.copyOf(normalized.values());
        if (!(authoritative || snapshotVersion == 0L && items.isEmpty())) {
            throw new IllegalArgumentException("unavailable private inventory contains state");
        }
    }

    static RepositoryPrivateInventorySnapshot unavailable() {
        return new RepositoryPrivateInventorySnapshot(false, 0L, List.of());
    }

    static RepositoryPrivateInventorySnapshot empty() {
        return new RepositoryPrivateInventorySnapshot(true, 0L, List.of());
    }
}
