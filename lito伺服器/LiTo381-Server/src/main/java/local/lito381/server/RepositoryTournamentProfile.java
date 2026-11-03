// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryTournamentProfile(long characterObjectId, String characterName, long snapshotVersion, GameStateStore.TournamentProfileState state) {
    RepositoryTournamentProfile {
        String string = characterName = characterName == null ? "" : characterName.trim();
        if (characterObjectId <= 0L || characterObjectId > 0xFFFFFFFFL || characterName.isBlank() || characterName.length() > 45 || snapshotVersion < 0L || snapshotVersion > 0xFFFFFFFFL || state == null) {
            throw new IllegalArgumentException("invalid repository Tournament profile");
        }
    }
}
