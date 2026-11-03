package local.lito381.server;

/** Hash-locked item rows referenced only by selected classic-world shops. */
final class ClassicWorldShopItemCatalog {
    static final int BLOOD_SOUL_ITEM_ID = 49013;
    static final String BLOOD_SOUL_SHOP_SOURCE_SHA256 =
            "B3755002BE73B3E22B969E6CDFEED8064C481EEC0261A76EDAA187F648342526";
    static final String BLOOD_SOUL_ITEM_SOURCE_SHA256 =
            "0E165BAC25653080307193DCC2DBBB324B2DB68527EB4B00121FE9E756939BFC";

    private static final ClassicWorldDropCatalog.ItemWire BLOOD_SOUL =
            new ClassicWorldDropCatalog.ItemWire(
                    BLOOD_SOUL_ITEM_ID, "etc", "$2988", "normal",
                    989, 3963, RuntimeInventory.BLESSING_BLESSED,
                    false, true, 1_000, 1_099, "none",
                    0, 0, "paper", -1, 0);

    private ClassicWorldShopItemCatalog() {}

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        return itemId == BLOOD_SOUL_ITEM_ID
                ? BLOOD_SOUL.inventoryDefinition() : null;
    }

    static InventoryCatalog.NativeWeaponStatus nativeWeaponStatus(int itemId) {
        return null;
    }
}
