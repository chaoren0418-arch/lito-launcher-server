// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourcePet(long sourceCollarObjectId, long sourcePetObjectId, int collarCatalogId, int npcId, String name, int level, int maxHp, int maxMp, long experience, int lawful, boolean litoOwned) {
    SourcePet(long sourceCollarObjectId, long sourcePetObjectId, int collarCatalogId, int npcId, String name, int level, int maxHp, int maxMp, long experience, int lawful) {
        this(sourceCollarObjectId, sourcePetObjectId, collarCatalogId, npcId, name, level, maxHp, maxMp, experience, lawful, false);
    }

    SourcePet {
        if (sourceCollarObjectId <= 0L || sourceCollarObjectId > 0xFFFFFFFFL || sourcePetObjectId <= 0L || sourcePetObjectId > 0xFFFFFFFFL || (long)collarCatalogId != 40314L && (long)collarCatalogId != 40316L || npcId <= 0 || name == null || name.isBlank() || level <= 0 || level > 255 || maxHp <= 0 || maxMp < 0 || experience < 0L || experience > 0xFFFFFFFFL || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE) {
            throw new IllegalArgumentException("invalid selected-source pet row");
        }
    }
}
