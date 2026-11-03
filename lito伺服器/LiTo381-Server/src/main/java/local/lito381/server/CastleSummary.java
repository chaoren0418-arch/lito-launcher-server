// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record CastleSummary(int castleId, String clanName, String leaderName, String castleName, long warTimeMillis, int taxRate, long publicMoney, long leaderObjectId) {
    CastleSummary {
        if (castleId <= 0 || clanName == null || clanName.isBlank() || leaderName == null || leaderName.isBlank() || castleName == null || castleName.isBlank() || warTimeMillis < 0L || taxRate < 0 || taxRate > 100 || publicMoney < 0L || leaderObjectId < 0L || leaderObjectId > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid legacy castle summary");
        }
    }

    CastleSummary(int castleId, String clanName, String leaderName, String castleName, long warTimeMillis, int taxRate, long publicMoney) {
        this(castleId, clanName, leaderName, castleName, warTimeMillis, taxRate, publicMoney, 0L);
    }
}
