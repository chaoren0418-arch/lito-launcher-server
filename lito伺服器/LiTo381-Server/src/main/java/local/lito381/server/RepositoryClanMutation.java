// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryClanMutationStatus;
import local.lito381.server.RepositoryClanSnapshot;

record RepositoryClanMutation(RepositoryClanMutationStatus status, RepositoryClanSnapshot snapshot) {
    RepositoryClanMutation {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("clan mutation result is incomplete");
        }
    }

    static RepositoryClanMutation unavailable() {
        return new RepositoryClanMutation(RepositoryClanMutationStatus.UNAVAILABLE, RepositoryClanSnapshot.unavailable());
    }

    static RepositoryClanMutation readOnly(RepositoryClanSnapshot snapshot) {
        return new RepositoryClanMutation(RepositoryClanMutationStatus.READ_ONLY, snapshot == null ? RepositoryClanSnapshot.unavailable() : snapshot);
    }
}
