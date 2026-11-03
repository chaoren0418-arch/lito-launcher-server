// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Locale;
import local.lito381.server.InventoryCatalog;

record RepositoryPendingReward(long sequence, String account, InventoryCatalog.Definition definition, int count, int enchant) {
    RepositoryPendingReward {
        String string = account = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        if (sequence <= 0L || sequence > Integer.MAX_VALUE || account.isBlank() || definition == null || count <= 0 || enchant < -128 || enchant > 127 || enchant != 0 && !definition.weapon() && !definition.armor()) {
            throw new IllegalArgumentException("invalid pending reward row");
        }
    }

    int itemId() {
        return Math.toIntExact(this.definition.catalogId());
    }
}
