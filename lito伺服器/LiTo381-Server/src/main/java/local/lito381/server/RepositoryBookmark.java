// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryBookmark(String name, int x, int y, int mapId) {
    RepositoryBookmark {
        String string = name = name == null ? "" : name.trim();
        if (name.isBlank() || name.length() > 45 || x < 0 || x > 65535 || y < 0 || y > 65535 || mapId < 0 || mapId > 65535) {
            throw new IllegalArgumentException("invalid repository bookmark");
        }
    }

    static RepositoryBookmark from(GameStateStore.Bookmark bookmark) {
        if (bookmark == null) {
            throw new IllegalArgumentException("bookmark is required");
        }
        return new RepositoryBookmark(bookmark.name(), bookmark.x(), bookmark.y(), bookmark.mapId());
    }

    GameStateStore.Bookmark gameStateBookmark() {
        return new GameStateStore.Bookmark(this.name, this.x, this.y, this.mapId);
    }
}
