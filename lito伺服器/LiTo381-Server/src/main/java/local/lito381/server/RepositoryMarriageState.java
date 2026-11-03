// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryMarriageStateStatus;

record RepositoryMarriageState(RepositoryMarriageStateStatus status, long characterObjectId, String characterName, long partnerObjectId, String partnerName) {
    RepositoryMarriageState {
        characterName = characterName == null ? "" : characterName.trim();
        String string = partnerName = partnerName == null ? "" : partnerName.trim();
        if (!(status != null && characterObjectId >= 0L && partnerObjectId >= 0L && characterObjectId <= Integer.MAX_VALUE && partnerObjectId <= Integer.MAX_VALUE && characterName.length() <= 45 && partnerName.length() <= 45 && (status != RepositoryMarriageStateStatus.AVAILABLE || characterObjectId != 0L && !characterName.isBlank() && partnerObjectId == 0L == partnerName.isBlank()) && (status != RepositoryMarriageStateStatus.NOT_FOUND || characterObjectId == 0L && characterName.isEmpty() && partnerObjectId == 0L && partnerName.isEmpty()) && (status != RepositoryMarriageStateStatus.UNAVAILABLE || characterObjectId == 0L && characterName.isEmpty() && partnerObjectId == 0L && partnerName.isEmpty()))) {
            throw new IllegalArgumentException("invalid repository marriage state");
        }
    }

    boolean authoritative() {
        return this.status != RepositoryMarriageStateStatus.UNAVAILABLE;
    }

    boolean consistent() {
        return this.status == RepositoryMarriageStateStatus.AVAILABLE;
    }

    boolean married() {
        return this.consistent() && this.partnerObjectId != 0L;
    }

    static RepositoryMarriageState unavailable() {
        return new RepositoryMarriageState(RepositoryMarriageStateStatus.UNAVAILABLE, 0L, "", 0L, "");
    }

    static RepositoryMarriageState notFound() {
        return new RepositoryMarriageState(RepositoryMarriageStateStatus.NOT_FOUND, 0L, "", 0L, "");
    }

    static RepositoryMarriageState available(long characterObjectId, String characterName, long partnerObjectId, String partnerName) {
        return new RepositoryMarriageState(RepositoryMarriageStateStatus.AVAILABLE, characterObjectId, characterName, partnerObjectId, partnerName);
    }

    static RepositoryMarriageState conflict(long characterObjectId, String characterName, long partnerObjectId, String partnerName) {
        return new RepositoryMarriageState(RepositoryMarriageStateStatus.CONFLICT, characterObjectId, characterName, partnerObjectId, partnerName);
    }
}
