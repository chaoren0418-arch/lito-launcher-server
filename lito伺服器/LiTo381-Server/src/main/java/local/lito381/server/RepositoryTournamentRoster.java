// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.RepositoryTournamentProfile;

record RepositoryTournamentRoster(boolean authoritative, List<RepositoryTournamentProfile> profiles) {
    RepositoryTournamentRoster {
        List<Object> list = profiles = profiles == null ? List.of() : List.copyOf(profiles);
        if (!authoritative && !profiles.isEmpty()) {
            throw new IllegalArgumentException("unavailable Tournament roster contains rows");
        }
    }

    static RepositoryTournamentRoster unavailable() {
        return new RepositoryTournamentRoster(false, List.of());
    }
}
