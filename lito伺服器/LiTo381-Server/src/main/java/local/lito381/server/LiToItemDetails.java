// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import local.lito381.server.AccountRepository;
import local.lito381.server.ClientSession;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.LiToItemTemplates;
import local.lito381.server.NativeDeItemPublication;
import local.lito381.server.Protocol;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.TournamentShopCatalog;

final class LiToItemDetails {
    static final Charset BIG5 = Charset.forName("MS950");
    static final String PREFIX = "LITOITEM1:";
    static Field INVENTORY;
    static Field SHOP;
    static Method SEND;
    static Method OFFERS;
    static Method SOURCE;

    LiToItemDetails() {
    }

    static void row(StringBuilder stringBuilder, String string, long l) {
        if (l != 0L) {
            stringBuilder.append(string).append(": ").append(l > 0L ? "+" : "").append(l).append('\n');
        }
    }

    static String describe(RuntimeInventory.Item item) {
        InventoryCatalog.Definition definition = item.definition();
        StringBuilder stringBuilder = new StringBuilder(item.nativeViewName()).append('\n');
        if (!item.identified()) {
            return stringBuilder.append("\u5c1a\u672a\u9451\u5b9a").toString();
        }
        InventoryCatalog.NativeArmorStatus nativeArmorStatus = definition.sourceArmorStatus();
        InventoryCatalog.NativeWeaponStatus nativeWeaponStatus = definition.effectiveWeaponStatus();
        InventoryCatalog.NativeProjectileStatus nativeProjectileStatus = InventoryCatalog.nativeProjectileStatus(definition.catalogId());
        if (nativeArmorStatus != null) {
            stringBuilder.append("\u9632\u79a6\u529b: ").append(-nativeArmorStatus.displayArmorClass()).append('\n');
            LiToItemDetails.row(stringBuilder, "\u529b\u91cf", nativeArmorStatus.strength());
            LiToItemDetails.row(stringBuilder, "\u654f\u6377", nativeArmorStatus.dexterity());
            LiToItemDetails.row(stringBuilder, "\u9ad4\u8cea", nativeArmorStatus.constitution());
            LiToItemDetails.row(stringBuilder, "\u7cbe\u795e", nativeArmorStatus.wisdom());
            LiToItemDetails.row(stringBuilder, "\u667a\u529b", nativeArmorStatus.intelligence());
            LiToItemDetails.row(stringBuilder, "\u9b45\u529b", nativeArmorStatus.charisma());
            LiToItemDetails.row(stringBuilder, "\u9ad4\u529b\u4e0a\u9650", nativeArmorStatus.hitPoints());
            LiToItemDetails.row(stringBuilder, "\u9b54\u529b\u4e0a\u9650", nativeArmorStatus.manaPoints());
            LiToItemDetails.row(stringBuilder, "\u9b54\u6cd5\u9632\u79a6", nativeArmorStatus.magicResistance());
            LiToItemDetails.row(stringBuilder, "\u9b54\u653b", nativeArmorStatus.spellPower());
            LiToItemDetails.row(stringBuilder, "\u706b\u5c6c\u6027\u9632\u79a6", nativeArmorStatus.fireResistance());
            LiToItemDetails.row(stringBuilder, "\u6c34\u5c6c\u6027\u9632\u79a6", nativeArmorStatus.waterResistance());
            LiToItemDetails.row(stringBuilder, "\u98a8\u5c6c\u6027\u9632\u79a6", nativeArmorStatus.windResistance());
            LiToItemDetails.row(stringBuilder, "\u5730\u5c6c\u6027\u9632\u79a6", nativeArmorStatus.earthResistance());
            LiToItemDetails.row(stringBuilder, "\u6697\u9ed1\u8010\u6027", nativeArmorStatus.blindResistance());
            LiToItemDetails.row(stringBuilder, "\u7761\u7720\u8010\u6027", nativeArmorStatus.sleepResistance());
            LiToItemDetails.row(stringBuilder, "\u51b0\u51cd\u8010\u6027", nativeArmorStatus.freezeResistance());
            LiToItemDetails.row(stringBuilder, "\u77f3\u5316\u8010\u6027", nativeArmorStatus.stoneResistance());
            LiToItemDetails.row(stringBuilder, "\u652f\u6490\u8010\u6027", nativeArmorStatus.sustainResistance());
            LiToItemDetails.row(stringBuilder, "\u660f\u8ff7\u8010\u6027", nativeArmorStatus.stunResistance());
            if (nativeArmorStatus.haste()) {
                stringBuilder.append("\u52a0\u901f\u6548\u679c\n");
            }
            stringBuilder.append("\u5b89\u5168\u5f37\u5316: ").append(nativeArmorStatus.safeEnchant()).append('\n');
            LiToItemDetails.classes(stringBuilder, nativeArmorStatus.classMask());
        } else if (definition.projectile() && nativeProjectileStatus != null) {
            stringBuilder.append("\u653b\u64ca\u529b (\u5c0f\u578b/\u5927\u578b): ").append(nativeProjectileStatus.damageSmall()).append('/').append(nativeProjectileStatus.damageLarge()).append('\n');
        } else if (nativeWeaponStatus != null) {
            stringBuilder.append("\u653b\u64ca\u529b (\u5c0f\u578b/\u5927\u578b): ").append(nativeWeaponStatus.damageSmall()).append('/').append(nativeWeaponStatus.damageLarge()).append('\n');
            LiToItemDetails.row(stringBuilder, "\u653b\u64ca\u547d\u4e2d", nativeWeaponStatus.hitModifier());
            LiToItemDetails.row(stringBuilder, "\u984d\u5916\u50b7\u5bb3", nativeWeaponStatus.damageModifier());
            stringBuilder.append("\u5b89\u5168\u5f37\u5316: ").append(nativeWeaponStatus.safeEnchant()).append('\n');
            LiToItemDetails.classes(stringBuilder, nativeWeaponStatus.classMask());
        }
        if (item.enchantLevel() != 0) {
            LiToItemDetails.row(stringBuilder, "\u5f37\u5316\u503c", item.enchantLevel());
        }
        if (item.durability() != 0) {
            LiToItemDetails.row(stringBuilder, "\u640d\u58de\u5ea6", item.durability());
        }
        if (definition.maximumChargeCount() > 0) {
            stringBuilder.append("\u5269\u9918\u6b21\u6578: ").append(item.chargeCount()).append('\n');
        }
        stringBuilder.append("\u6578\u91cf: ").append(item.quantity()).append('\n');
        stringBuilder.append("\u91cd\u91cf: ").append((long)definition.unitWeight() * (long)item.quantity() / 1000L).append('\n');
        if (definition.detail() != null && !definition.detail().isBlank()) {
            stringBuilder.append(definition.detail()).append('\n');
        }
        return stringBuilder.toString();
    }

    static void classes(StringBuilder stringBuilder, int n) {
        String[] stringArray = new String[]{"\u738b\u65cf", "\u9a0e\u58eb", "\u5996\u7cbe", "\u6cd5\u5e2b", "\u9ed1\u6697\u5996\u7cbe", "\u9f8d\u9a0e\u58eb", "\u5e7b\u8853\u58eb"};
        stringBuilder.append("\u53ef\u7528\u8077\u696d: ");
        for (int i = 0; i < stringArray.length; ++i) {
            if ((n & 1 << i) == 0) continue;
            stringBuilder.append(stringArray[i]).append(' ');
        }
        stringBuilder.append('\n');
    }

    static byte[] packet(long l, String string) {
        byte[] byArray = string.getBytes(BIG5);
        if (byArray.length > 8192) {
            throw new IllegalArgumentException("LiTo item detail exceeds 8192 bytes");
        }
        return new Protocol.Writer().c(15).c(254).s(PREFIX + Long.toUnsignedString(l, 16) + ":" + HexFormat.of().formatHex(byArray)).bytes();
    }

    static String describe(ClientSession clientSession, RuntimeInventory.Item item) throws ReflectiveOperationException {
        if (!item.identified()) {
            return "";
        }
        AccountRepository accountRepository = (AccountRepository)SOURCE.invoke(clientSession, new Object[0]);
        Map<String, String> map = LiToItemTemplates.get(accountRepository, item.definition().catalogId());
        String string = LiToItemTemplates.custom(accountRepository, item.definition().catalogId());
        if (map != null && Set.of("armor", "weapon").contains(map.get("kind"))) {
            return LiToItemTemplates.needsExtension(map) || !string.isEmpty() ? LiToItemTemplates.describe(item, map) + string : "";
        }
        if (!string.isEmpty()) {
            return LiToItemDetails.describe(item) + string;
        }
        InventoryCatalog.NativeArmorStatus nativeArmorStatus = item.definition().sourceArmorStatus();
        if (nativeArmorStatus == null) {
            return "";
        }
        String string2 = LiToItemDetails.describe(item);
        return string2.lines().count() > 19L || Math.abs((long)nativeArmorStatus.hitPoints()) > 32767L || Math.abs((long)nativeArmorStatus.manaPoints()) > 32767L || Math.abs((long)nativeArmorStatus.magicResistance()) > 127L || Math.abs((long)nativeArmorStatus.spellPower()) > 127L ? string2 : "";
    }

    static void publish(ClientSession clientSession, byte[] byArray) throws IOException {
        if (!Boolean.getBoolean("lito.extendedItemDetails") || byArray.length == 0) {
            return;
        }
        int n = byArray[0] & 0xFF;
        if (n != 22 && n != 124 && n != 111 && n != 24 && n != 23 && n != 223) {
            return;
        }
        try {
            long l;
            if (n == 223) {
                Object object = SHOP.get(clientSession);
                if (object == null) {
                    return;
                }
                for (Object e : (List)OFFERS.invoke(object, new Object[0])) {
                    TournamentShopCatalog.Offer offer = (TournamentShopCatalog.Offer)e;
                    InventoryCatalog.Definition definition = offer.definition();
                    SEND.invoke(clientSession, LiToItemDetails.packet(definition.catalogId(), LiToItemDetails.describe(clientSession, new RuntimeInventory.Item(definition.catalogId(), definition, 1, false))), "S_LITO_ITEM_DETAIL shop=" + definition.catalogId());
                }
                return;
            }
            RuntimeInventory runtimeInventory = NativeDeItemPublication.inventory(clientSession);
            if (runtimeInventory == null) {
                return;
            }
            long l2 = l = byArray.length >= 5 ? (long)byArray[1] & 0xFFL | ((long)byArray[2] & 0xFFL) << 8 | ((long)byArray[3] & 0xFFL) << 16 | ((long)byArray[4] & 0xFFL) << 24 : 0L;
            if (n == 23) {
                SEND.invoke(clientSession, LiToItemDetails.packet(l, ""), "S_LITO_ITEM_DETAIL remove=" + l);
                return;
            }
            for (RuntimeInventory.Item item : runtimeInventory.items()) {
                if (n != 124 && item.objectId() != l) continue;
                SEND.invoke(clientSession, LiToItemDetails.packet(item.objectId(), LiToItemDetails.describe(clientSession, item)), "S_LITO_ITEM_DETAIL v1 object=" + item.objectId());
            }
        }
        catch (InvocationTargetException invocationTargetException) {
            Throwable throwable = invocationTargetException.getCause();
            if (throwable instanceof IOException) {
                IOException iOException = (IOException)throwable;
                throw iOException;
            }
            throw new IOException("LiTo item detail publication", invocationTargetException.getCause());
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IOException(reflectiveOperationException);
        }
    }

    static {
        try {
            INVENTORY = ClientSession.class.getDeclaredField("inventory");
            INVENTORY.setAccessible(true);
            SEND = ClientSession.class.getDeclaredMethod("sendEncryptedFrame", byte[].class, String.class);
            SEND.setAccessible(true);
            SHOP = ClientSession.class.getDeclaredField("pendingTournamentShop");
            SHOP.setAccessible(true);
            OFFERS = Class.forName("local.lito381.server.ClientSession$PendingTournamentShop").getDeclaredMethod("offers", new Class[0]);
            OFFERS.setAccessible(true);
            SOURCE = ClientSession.class.getDeclaredMethod("selectedSourceAccounts", new Class[0]);
            SOURCE.setAccessible(true);
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
