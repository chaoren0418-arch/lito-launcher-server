// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryCharacterTitle(boolean authoritative, boolean present, String title) {
    RepositoryCharacterTitle {
        String string = title = title == null ? "" : title;
        if (!authoritative && (present || !title.isEmpty()) || present && title.length() > 35) {
            throw new IllegalArgumentException("invalid repository character title");
        }
    }

    static RepositoryCharacterTitle unavailable() {
        return new RepositoryCharacterTitle(false, false, "");
    }

    static RepositoryCharacterTitle missing() {
        return new RepositoryCharacterTitle(true, false, "");
    }

    static RepositoryCharacterTitle present(String title) {
        return new RepositoryCharacterTitle(true, true, title);
    }
}
