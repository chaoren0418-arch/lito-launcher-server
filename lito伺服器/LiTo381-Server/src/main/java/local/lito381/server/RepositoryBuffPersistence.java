// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryBuffPersistenceStatus;
import local.lito381.server.RepositoryBuffSnapshot;

record RepositoryBuffPersistence(RepositoryBuffPersistenceStatus status, RepositoryBuffSnapshot snapshot) {
    RepositoryBuffPersistence {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("buff persistence result is incomplete");
        }
    }

    static RepositoryBuffPersistence readOnly(RepositoryBuffSnapshot expected) {
        return new RepositoryBuffPersistence(RepositoryBuffPersistenceStatus.READ_ONLY, expected == null ? RepositoryBuffSnapshot.unavailable() : expected);
    }
}
