// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import local.lito381.server.RepositoryLetter;

record RepositoryLetterSnapshot(boolean authoritative, List<RepositoryLetter> letters) {
    RepositoryLetterSnapshot {
        letters = letters == null ? List.of() : letters.stream().sorted(Comparator.comparingLong(RepositoryLetter::itemObjectId)).toList();
        LinkedHashSet identities = new LinkedHashSet();
        if (letters.stream().anyMatch(letter -> letter == null || !identities.add(letter.itemObjectId()))) {
            throw new IllegalArgumentException("duplicate/null repository physical letter");
        }
    }

    static RepositoryLetterSnapshot unavailable() {
        return new RepositoryLetterSnapshot(false, List.of());
    }
}
