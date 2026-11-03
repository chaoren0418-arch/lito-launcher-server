// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.CharacterSummary;
import local.lito381.server.RepositoryCharacterMutationStatus;

record RepositoryCharacterMutation(RepositoryCharacterMutationStatus status, CharacterSummary character) {
    static RepositoryCharacterMutation readOnly() {
        return new RepositoryCharacterMutation(RepositoryCharacterMutationStatus.READ_ONLY, null);
    }
}
