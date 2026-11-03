// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryCastleSnapshot(boolean authoritative, int castleId, int taxRate, long publicMoney, long warTimeMillis, boolean securityEnabled, GameStateStore.MercenaryRoster mercenaries, long snapshotVersion) {
    RepositoryCastleSnapshot {
        if (castleId < 0 || castleId > 8 || taxRate < 0 || taxRate > 50 || publicMoney < 0L || publicMoney > Integer.MAX_VALUE || warTimeMillis < 0L || mercenaries == null || snapshotVersion < 0L || authoritative && castleId == 0 || !authoritative && (castleId != 0 || taxRate != 0 || publicMoney != 0L || warTimeMillis != 0L || securityEnabled || mercenaries.total() != 0 || snapshotVersion != 0L)) {
            throw new IllegalArgumentException("invalid repository castle snapshot");
        }
        if (authoritative && taxRate < 10) {
            throw new IllegalArgumentException("repository castle tax is outside 10..50");
        }
    }

    static RepositoryCastleSnapshot unavailable() {
        return new RepositoryCastleSnapshot(false, 0, 0, 0L, 0L, false, new GameStateStore.MercenaryRoster(0, 0, 0, 0), 0L);
    }
}
