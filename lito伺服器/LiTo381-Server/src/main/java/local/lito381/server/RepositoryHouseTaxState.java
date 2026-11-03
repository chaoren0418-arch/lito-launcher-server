// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryHouseTaxState(String sourceId, long houseId, String name, int area, String location, int onSale, int purchaseBasement, long taxDeadlineEpochSeconds) {
    RepositoryHouseTaxState {
        sourceId = sourceId == null ? "" : sourceId.trim();
        name = name == null ? "" : name;
        String string = location = location == null ? "" : location;
        if (houseId <= 0L || houseId > 0xFFFFFFFFL || name.isBlank() || name.length() > 45 || area < 0 || location.length() > 45 || onSale < 0 || purchaseBasement < 0 || taxDeadlineEpochSeconds < 0L) {
            throw new IllegalArgumentException("invalid repository house tax state");
        }
    }
}
