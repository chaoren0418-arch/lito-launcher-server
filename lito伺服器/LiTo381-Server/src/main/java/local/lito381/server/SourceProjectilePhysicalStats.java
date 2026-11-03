// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceProjectilePhysicalStats(long objectId, int itemId, int useType, int damageSmall, int damageLarge, int material, int blessing, int arrowGfx, boolean consumeOnAttack, long count) {
    SourceProjectilePhysicalStats(int itemId, int useType, int damageSmall, int damageLarge, int arrowGfx, boolean consumeOnAttack, long count) {
        this(itemId, itemId, useType, damageSmall, damageLarge, 0, 1, arrowGfx, consumeOnAttack, count);
    }

    SourceProjectilePhysicalStats(long objectId, int itemId, int useType, int damageSmall, int damageLarge, int arrowGfx, boolean consumeOnAttack, long count) {
        this(objectId, itemId, useType, damageSmall, damageLarge, 0, 1, arrowGfx, consumeOnAttack, count);
    }

    SourceProjectilePhysicalStats {
        if (objectId < 0L || objectId > 0xFFFFFFFFL || itemId < 0 || useType != 0 && useType != -2 && useType != -3 || damageSmall < 0 || damageLarge < 0 || material < 0 || material > 22 || blessing < 0 || blessing > 2 || arrowGfx < -1 || arrowGfx > 65535 || count < 0L) {
            throw new IllegalArgumentException("invalid source projectile snapshot");
        }
        if (objectId == 0L != (itemId == 0) || itemId == 0 != (useType == 0) || itemId == 0 != (count == 0L)) {
            throw new IllegalArgumentException("incomplete source projectile snapshot");
        }
    }

    static SourceProjectilePhysicalStats none() {
        return new SourceProjectilePhysicalStats(0L, 0, 0, 0, 0, 0, 1, -1, false, 0L);
    }

    boolean present() {
        return this.itemId != 0;
    }

    int damageMaximum(boolean targetLarge) {
        return Math.max(1, targetLarge ? this.damageLarge : this.damageSmall);
    }

    int effectGfx() {
        if (!this.present()) {
            throw new IllegalStateException("empty projectile has no effect gfx");
        }
        if (this.arrowGfx > 0) {
            return this.arrowGfx;
        }
        return this.useType == -3 ? 2989 : 66;
    }

    SourceProjectilePhysicalStats withObjectId(long value) {
        if (!this.present()) {
            throw new IllegalStateException("empty projectile has no object id");
        }
        return new SourceProjectilePhysicalStats(value, this.itemId, this.useType, this.damageSmall, this.damageLarge, this.material, this.blessing, this.arrowGfx, this.consumeOnAttack, this.count);
    }
}
