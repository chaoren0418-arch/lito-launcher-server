// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryCharacterTitle;
import local.lito381.server.RepositoryTitleMutationStatus;

record RepositoryTitleMutation(RepositoryTitleMutationStatus status, RepositoryCharacterTitle title) {
    RepositoryTitleMutation {
        if (status == null || title == null) {
            throw new IllegalArgumentException("character-title mutation is incomplete");
        }
    }

    static RepositoryTitleMutation unavailable() {
        return new RepositoryTitleMutation(RepositoryTitleMutationStatus.UNAVAILABLE, RepositoryCharacterTitle.unavailable());
    }

    static RepositoryTitleMutation readOnly(RepositoryCharacterTitle title) {
        return new RepositoryTitleMutation(RepositoryTitleMutationStatus.READ_ONLY, title == null ? RepositoryCharacterTitle.unavailable() : title);
    }
}
