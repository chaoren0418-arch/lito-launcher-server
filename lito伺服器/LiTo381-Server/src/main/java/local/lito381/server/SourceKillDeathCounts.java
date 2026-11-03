// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceKillDeathCounts(int kills, int deaths) {
    SourceKillDeathCounts {
        if (kills < 0 || deaths < 0) {
            throw new IllegalArgumentException("invalid source kill/death counts");
        }
    }

    static SourceKillDeathCounts none() {
        return new SourceKillDeathCounts(0, 0);
    }
}
