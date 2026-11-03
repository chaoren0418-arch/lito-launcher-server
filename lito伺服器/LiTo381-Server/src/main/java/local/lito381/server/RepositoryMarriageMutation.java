// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryMarriageMutationStatus;
import local.lito381.server.RepositoryMarriageState;

record RepositoryMarriageMutation(RepositoryMarriageMutationStatus status, RepositoryMarriageState first, RepositoryMarriageState second) {
    RepositoryMarriageMutation {
        if (status == null || first == null || second == null) {
            throw new IllegalArgumentException("repository marriage mutation is incomplete");
        }
    }

    boolean succeeded() {
        return this.status == RepositoryMarriageMutationStatus.SUCCESS;
    }

    static RepositoryMarriageMutation unavailable() {
        return new RepositoryMarriageMutation(RepositoryMarriageMutationStatus.UNAVAILABLE, RepositoryMarriageState.unavailable(), RepositoryMarriageState.unavailable());
    }

    static RepositoryMarriageMutation invalid() {
        return new RepositoryMarriageMutation(RepositoryMarriageMutationStatus.INVALID, RepositoryMarriageState.unavailable(), RepositoryMarriageState.unavailable());
    }
}
