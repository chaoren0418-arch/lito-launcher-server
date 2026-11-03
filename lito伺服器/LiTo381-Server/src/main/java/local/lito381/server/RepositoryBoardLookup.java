// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryBoardLookup(boolean authoritative, GameStateStore.BoardTopic topic) {
    RepositoryBoardLookup {
        if (!authoritative && topic != null) {
            throw new IllegalArgumentException("unavailable board lookup contains a topic");
        }
    }

    static RepositoryBoardLookup unavailable() {
        return new RepositoryBoardLookup(false, null);
    }

    static RepositoryBoardLookup missing() {
        return new RepositoryBoardLookup(true, null);
    }
}
