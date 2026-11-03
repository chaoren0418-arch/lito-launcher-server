// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleInventoryWeight;
import local.lito381.server.ClassicWorldDropCatalog;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.SourceMagicDollType;

record SourceInventoryItem(long sourceObjectId, int catalogId, int quantity, boolean equipped, int sourceEnchantLevel, int durability, boolean identified, int blessing, boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire, InventoryCatalog.NativeArmorStatus sourceArmorStatus, long expiresAtEpochMillis, boolean sealed, SourceMagicDollType magicDollType, boolean litoOwned, boolean tournamentReward, int chargeCount) {
    SourceInventoryItem(long sourceObjectId, int catalogId, int quantity, boolean equipped, int sourceEnchantLevel, int durability, boolean identified, int blessing, boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire, InventoryCatalog.NativeArmorStatus sourceArmorStatus, long expiresAtEpochMillis, boolean sealed, SourceMagicDollType magicDollType, boolean litoOwned, boolean tournamentReward) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel, durability, identified, blessing, linkedPetCollar, wire, sourceArmorStatus, expiresAtEpochMillis, sealed, magicDollType, litoOwned, tournamentReward, 0);
    }

    SourceInventoryItem(long sourceObjectId, int catalogId, int quantity, boolean equipped, int sourceEnchantLevel, int durability, boolean identified, int blessing, boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire, InventoryCatalog.NativeArmorStatus sourceArmorStatus, long expiresAtEpochMillis, boolean sealed, SourceMagicDollType magicDollType) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel, durability, identified, blessing, linkedPetCollar, wire, sourceArmorStatus, expiresAtEpochMillis, sealed, magicDollType, false, false, 0);
    }

    SourceInventoryItem(long sourceObjectId, int catalogId, int quantity, boolean equipped, int sourceEnchantLevel, int durability, boolean identified, int blessing, boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel, durability, identified, blessing, linkedPetCollar, wire, null, 0L, false, null);
    }

    SourceInventoryItem(long sourceObjectId, int catalogId, int quantity, boolean equipped, int sourceEnchantLevel, int durability, boolean identified, int blessing, boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire, InventoryCatalog.NativeArmorStatus sourceArmorStatus) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel, durability, identified, blessing, linkedPetCollar, wire, sourceArmorStatus, 0L, false, null);
    }

    SourceInventoryItem(long sourceObjectId, int catalogId, int quantity, boolean equipped, int sourceEnchantLevel, int durability, boolean identified, int blessing, boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire, InventoryCatalog.NativeArmorStatus sourceArmorStatus, long expiresAtEpochMillis, boolean sealed) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel, durability, identified, blessing, linkedPetCollar, wire, sourceArmorStatus, expiresAtEpochMillis, sealed, null);
    }

    SourceInventoryItem {
        if (sourceObjectId <= 0L || sourceObjectId > 0xFFFFFFFFL || catalogId <= 0 || quantity <= 0 || durability < 0 || durability > 127 || blessing < 0 || blessing > 3 || chargeCount < 0 || chargeCount > 255 || expiresAtEpochMillis < 0L || tournamentReward && !litoOwned || wire == null || wire.itemId() != catalogId) {
            throw new IllegalArgumentException("invalid selected-source inventory row");
        }
        if (sourceArmorStatus != null != wire.kind().equals("armor")) {
            throw new IllegalArgumentException("selected-source armor status/template kind mismatch");
        }
        if (magicDollType != null && (magicDollType.itemId() != catalogId || !wire.kind().equals("etc") || wire.stackable() || equipped || linkedPetCollar)) {
            throw new IllegalArgumentException("selected-source magic doll/template state mismatch");
        }
    }

    int nativeEnchantLevel() {
        return (byte)this.sourceEnchantLevel;
    }

    InventoryCatalog.Definition definition() {
        InventoryCatalog.Definition definition = this.wire.inventoryDefinition(this.sourceArmorStatus);
        return this.magicDollType == null ? definition : definition.withEffect(InventoryCatalog.Effect.MAGIC_DOLL);
    }

    int nativeWeight() {
        return BattleInventoryWeight.itemWeight(this.quantity, this.wire.unitWeight());
    }

    boolean ordinarySidecarImportable() {
        return this.wire.kind().equals("etc") && !this.equipped && !this.linkedPetCollar && !this.wire.useToken().equals("arrow") && !this.wire.useToken().equals("sting") && this.sourceEnchantLevel == 0 && this.durability == 0 && (this.wire.stackable() || this.quantity == 1);
    }

    boolean litoOwnedPetCollarImportable() {
        return this.litoOwned && this.linkedPetCollar && this.wire.kind().equals("etc") && !this.equipped && this.quantity == 1 && this.sourceEnchantLevel == 0 && this.durability == 0;
    }

    boolean sourceWeaponImportable() {
        return this.wire.kind().equals("weapon") && !this.linkedPetCollar && this.quantity == 1;
    }

    boolean sourceArmorImportable() {
        return this.wire.kind().equals("armor") && !this.linkedPetCollar && this.quantity == 1 && this.sourceArmorStatus != null;
    }

    boolean sourceProjectileImportable() {
        return !(!this.wire.kind().equals("etc") || this.equipped || this.linkedPetCollar || !this.wire.useToken().equals("arrow") && !this.wire.useToken().equals("sting") || this.sourceEnchantLevel != 0 || this.durability != 0 || !this.wire.stackable() && this.quantity != 1);
    }

    boolean sidecarImportable() {
        return this.ordinarySidecarImportable() || this.sourceWeaponImportable() || this.sourceArmorImportable() || this.sourceProjectileImportable() || this.litoOwnedPetCollarImportable();
    }
}
