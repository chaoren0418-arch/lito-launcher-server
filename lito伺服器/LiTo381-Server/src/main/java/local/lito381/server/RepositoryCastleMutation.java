// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryCastleMutationStatus;
import local.lito381.server.RepositoryCastleSnapshot;

record RepositoryCastleMutation(RepositoryCastleMutationStatus status, RepositoryCastleSnapshot snapshot) {
    RepositoryCastleMutation {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("castle mutation result is incomplete");
        }
    }

    static RepositoryCastleMutation unavailable() {
        return new RepositoryCastleMutation(RepositoryCastleMutationStatus.UNAVAILABLE, RepositoryCastleSnapshot.unavailable());
    }

    static RepositoryCastleMutation readOnly(RepositoryCastleSnapshot snapshot) {
        return new RepositoryCastleMutation(RepositoryCastleMutationStatus.READ_ONLY, snapshot == null ? RepositoryCastleSnapshot.unavailable() : snapshot);
    }
}
