// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.LinkedHashSet;
import java.util.List;
import local.lito381.server.GameStateStore;

record RepositoryBoardSnapshot(boolean authoritative, List<GameStateStore.BoardTopic> topics) {
    RepositoryBoardSnapshot {
        topics = topics == null ? List.of() : List.copyOf(topics);
        LinkedHashSet identities = new LinkedHashSet();
        if (!authoritative && !topics.isEmpty() || topics.stream().anyMatch(topic -> topic == null || !identities.add(topic.id()))) {
            throw new IllegalArgumentException("invalid repository board snapshot");
        }
    }

    static RepositoryBoardSnapshot unavailable() {
        return new RepositoryBoardSnapshot(false, List.of());
    }
}
