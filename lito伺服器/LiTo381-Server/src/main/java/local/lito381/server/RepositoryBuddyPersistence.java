// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryBuddyPersistenceStatus;
import local.lito381.server.RepositoryBuddySnapshot;

record RepositoryBuddyPersistence(RepositoryBuddyPersistenceStatus status, RepositoryBuddySnapshot snapshot) {
    RepositoryBuddyPersistence {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("buddy persistence result is incomplete");
        }
    }

    static RepositoryBuddyPersistence readOnly(RepositoryBuddySnapshot expected) {
        return new RepositoryBuddyPersistence(RepositoryBuddyPersistenceStatus.READ_ONLY, expected == null ? RepositoryBuddySnapshot.unavailable() : expected);
    }
}
