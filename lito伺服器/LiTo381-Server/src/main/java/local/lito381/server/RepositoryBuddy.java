// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryBuddy(String name, long buddyObjectId, String targetSource, int groupId, int relationshipStyle, int paletteOffset, String origin) {
    RepositoryBuddy {
        name = name == null ? "" : name.trim();
        targetSource = targetSource == null ? "" : targetSource.trim();
        String string = origin = origin == null ? "" : origin.trim();
        if (name.isBlank() || name.length() > 45 || buddyObjectId < 0L || buddyObjectId > 0xFFFFFFFFL || groupId < 1 || groupId > 255 || relationshipStyle < 0 || relationshipStyle > 4 || paletteOffset < 0 || paletteOffset > 194 || targetSource.length() > 45 || origin.length() > 45) {
            throw new IllegalArgumentException("invalid repository buddy");
        }
    }
}
