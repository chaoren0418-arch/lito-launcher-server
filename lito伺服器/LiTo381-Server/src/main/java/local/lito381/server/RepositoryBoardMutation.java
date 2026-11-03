// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryBoardMutationStatus;

record RepositoryBoardMutation(RepositoryBoardMutationStatus status, GameStateStore.BoardTopic topic) {
    RepositoryBoardMutation {
        if (status == null || status == RepositoryBoardMutationStatus.SUCCESS && topic == null) {
            throw new IllegalArgumentException("invalid repository board mutation");
        }
    }

    static RepositoryBoardMutation unavailable() {
        return new RepositoryBoardMutation(RepositoryBoardMutationStatus.UNAVAILABLE, null);
    }

    static RepositoryBoardMutation readOnly(GameStateStore.BoardTopic topic) {
        return new RepositoryBoardMutation(RepositoryBoardMutationStatus.READ_ONLY, topic);
    }
}
