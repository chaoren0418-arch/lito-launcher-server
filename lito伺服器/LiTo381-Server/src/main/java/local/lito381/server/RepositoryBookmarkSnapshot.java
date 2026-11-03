// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryBookmark;

record RepositoryBookmarkSnapshot(boolean authoritative, List<RepositoryBookmark> bookmarks) {
    RepositoryBookmarkSnapshot {
        bookmarks = bookmarks == null ? List.of() : List.copyOf(bookmarks);
    }

    static RepositoryBookmarkSnapshot unavailable() {
        return new RepositoryBookmarkSnapshot(false, List.of());
    }

    List<GameStateStore.Bookmark> gameStateBookmarks() {
        return this.bookmarks.stream().map(RepositoryBookmark::gameStateBookmark).toList();
    }
}
