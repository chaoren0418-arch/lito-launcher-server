// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryLetter;
import local.lito381.server.RepositoryLetterMutationStatus;

record RepositoryLetterMutation(RepositoryLetterMutationStatus status, RepositoryLetter letter) {
    static RepositoryLetterMutation readOnly(RepositoryLetter expected) {
        return new RepositoryLetterMutation(RepositoryLetterMutationStatus.READ_ONLY, expected);
    }
}
