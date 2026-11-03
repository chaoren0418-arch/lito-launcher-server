// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryClanMember(long objectId, String name, int role, int nativeRank, String title, String note) {
    RepositoryClanMember {
        name = name == null ? "" : name.trim();
        title = title == null ? "" : title;
        String string = note = note == null ? "" : note;
        if (objectId <= 0L || objectId > 0xFFFFFFFFL || name.isBlank() || name.length() > 45 || role < 1 || role > 4 || nativeRank < 1 || nativeRank > 10 || title.length() > 35 || note.length() > 62) {
            throw new IllegalArgumentException("invalid repository clan member");
        }
    }
}
