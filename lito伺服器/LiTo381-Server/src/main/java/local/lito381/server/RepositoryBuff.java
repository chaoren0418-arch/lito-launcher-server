// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryBuff(int skillId, int remainingSeconds, int polyId) {
    RepositoryBuff {
        if (skillId <= 0 || remainingSeconds == 0) {
            throw new IllegalArgumentException("invalid repository buff");
        }
    }
}
