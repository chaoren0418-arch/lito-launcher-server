// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import java.util.TreeMap;
import local.lito381.server.RepositoryBuff;

record RepositoryBuffSnapshot(boolean authoritative, long completedVersion, List<RepositoryBuff> buffs) {
    RepositoryBuffSnapshot {
        if (completedVersion < 0L) {
            throw new IllegalArgumentException("invalid repository buff version");
        }
        buffs = RepositoryBuffSnapshot.normalize(buffs);
    }

    static RepositoryBuffSnapshot unavailable() {
        return new RepositoryBuffSnapshot(false, 0L, List.of());
    }

    private static List<RepositoryBuff> normalize(List<RepositoryBuff> source) {
        List<Object> rows;
        List<Object> list = rows = source == null ? List.of() : source;
        if (rows.size() > 512) {
            throw new IllegalArgumentException("repository buff snapshot exceeds 512 rows");
        }
        TreeMap<Integer, RepositoryBuff> unique = new TreeMap<Integer, RepositoryBuff>();
        for (RepositoryBuff repositoryBuff : rows) {
            if (repositoryBuff != null && unique.putIfAbsent(repositoryBuff.skillId(), repositoryBuff) == null) continue;
            throw new IllegalArgumentException("duplicate or null repository buff row");
        }
        return List.copyOf(unique.values());
    }
}
