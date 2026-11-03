// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryTournamentMutationStatus;
import local.lito381.server.RepositoryTournamentProfile;

record RepositoryTournamentMutation(RepositoryTournamentMutationStatus status, RepositoryTournamentProfile profile) {
    RepositoryTournamentMutation {
        if (status == null || status == RepositoryTournamentMutationStatus.SUCCESS && profile == null) {
            throw new IllegalArgumentException("invalid repository Tournament mutation");
        }
    }

    static RepositoryTournamentMutation unavailable() {
        return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.UNAVAILABLE, null);
    }
}
