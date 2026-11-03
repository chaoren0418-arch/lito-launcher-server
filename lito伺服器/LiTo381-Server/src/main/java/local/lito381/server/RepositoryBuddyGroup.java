// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryBuddyGroup(int id, String name) {
    RepositoryBuddyGroup {
        String string = name = name == null ? "" : name.trim();
        if (id < 1 || id > 255 || name.isBlank() || name.length() > 45) {
            throw new IllegalArgumentException("invalid repository buddy group");
        }
    }
}
