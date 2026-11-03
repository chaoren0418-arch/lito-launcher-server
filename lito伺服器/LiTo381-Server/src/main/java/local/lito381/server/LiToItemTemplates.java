// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.sql.DataSource;
import local.lito381.server.AccountRepository;
import local.lito381.server.RuntimeInventory;

final class LiToItemTemplates {
    static final Map<AccountRepository, Map<Long, Map<String, String>>> CACHE = Collections.synchronizedMap(new WeakHashMap());

    LiToItemTemplates() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void preload(AccountRepository accountRepository) {
        if (!Boolean.getBoolean("lito.extendedItemDetails") || accountRepository == null) {
            return;
        }
        Map<AccountRepository, Map<Long, Map<String, String>>> map = CACHE;
        synchronized (map) {
            if (CACHE.containsKey(accountRepository)) {
                return;
            }
            if (!accountRepository.getClass().getSimpleName().equals("LegacyMySqlRepository")) {
                CACHE.put(accountRepository, Map.of());
                return;
            }
            try {
                Field field = accountRepository.getClass().getDeclaredField("resourceDatabase");
                Field field2 = accountRepository.getClass().getDeclaredField("dataSource");
                field.setAccessible(true);
                field2.setAccessible(true);
                String string = (String)field.get(accountRepository);
                if (!string.matches("[A-Za-z0-9_]+")) {
                    throw new SQLException("invalid resource schema");
                }
                HashMap hashMap = new HashMap();
                try (Connection connection = ((DataSource)field2.get(accountRepository)).getConnection();){
                    LinkedHashMap<Object, Object> linkedHashMap;
                    Object object;
                    connection.setReadOnly(true);
                    for (String object2 : List.of("armor", "weapon")) {
                        object = connection.createStatement();
                        try {
                            ResultSet resultSet = object.executeQuery("SELECT * FROM `" + string + "`.`" + object2 + "`");
                            try {
                                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                                while (resultSet.next()) {
                                    linkedHashMap = new LinkedHashMap<String, String>();
                                    for (int i = 1; i <= resultSetMetaData.getColumnCount(); ++i) {
                                        String string2 = resultSet.getString(i);
                                        if (string2 == null) continue;
                                        linkedHashMap.put(resultSetMetaData.getColumnLabel(i).toLowerCase(Locale.ROOT), string2);
                                    }
                                    linkedHashMap.put("kind", object2);
                                    hashMap.put(resultSet.getLong("item_id"), Map.copyOf(linkedHashMap));
                                }
                            }
                            finally {
                                if (resultSet == null) continue;
                                resultSet.close();
                            }
                        }
                        finally {
                            if (object == null) continue;
                            object.close();
                        }
                    }
                    try (Statement statement = connection.createStatement();
                         ResultSet resultSet = statement.executeQuery("SELECT Item_Id,Info FROM `" + string + "`.`\u9053\u5177_\u7269\u54c1\u8a73\u7d30\u8aaa\u660e`");){
                        while (resultSet.next()) {
                            object = resultSet.getString("Info");
                            if (object == null || ((String)object).trim().isEmpty()) continue;
                            long l = resultSet.getLong("Item_Id");
                            linkedHashMap = new LinkedHashMap<String, String>(hashMap.getOrDefault(l, Map.of("kind", "etc")));
                            linkedHashMap.put("_custom", linkedHashMap.getOrDefault("_custom", "") + (String)object + "\n");
                            hashMap.put(l, Map.copyOf(linkedHashMap));
                        }
                    }
                    catch (SQLException sQLException) {
                        if (!"42S02".equals(sQLException.getSQLState())) {
                            throw sQLException;
                        }
                        System.err.println("[LiTo] optional item description table absent");
                    }
                }
                CACHE.put(accountRepository, Map.copyOf(hashMap));
                System.out.println("[LiTo] full item templates ready count=" + hashMap.size());
            }
            catch (Exception exception) {
                CACHE.put(accountRepository, Map.of());
                System.err.println("[LiTo] full item templates unavailable: " + exception.getClass().getSimpleName());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Map<String, String> get(AccountRepository accountRepository, long l) {
        Map<AccountRepository, Map<Long, Map<String, String>>> map = CACHE;
        synchronized (map) {
            return (Map)CACHE.getOrDefault(accountRepository, Map.of()).get(l);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static String custom(AccountRepository accountRepository, long l) {
        Map<AccountRepository, Map<Long, Map<String, String>>> map = CACHE;
        synchronized (map) {
            Map map2 = CACHE.getOrDefault(accountRepository, Map.of());
            return map2.getOrDefault(-1L, Map.of()).getOrDefault("_custom", "") + map2.getOrDefault(l, Map.of()).getOrDefault("_custom", "");
        }
    }

    static long number(Map<String, String> map, String string) {
        String string2 = map.get(string);
        return string2 == null || string2.isBlank() ? 0L : Long.parseLong(string2);
    }

    static void row(StringBuilder stringBuilder, Map<String, String> map, String string, String string2, String string3) {
        long l = LiToItemTemplates.number(map, string);
        if (l != 0L) {
            stringBuilder.append(string2).append(' ').append(l > 0L ? "+" : "").append(l).append(string3).append('\n');
        }
    }

    static String describe(RuntimeInventory.Item item, Map<String, String> map) {
        long l;
        String[][] stringArrayArray;
        StringBuilder stringBuilder = new StringBuilder(item.nativeViewName()).append('\n');
        boolean bl = map.get("kind").equals("armor");
        if (!item.identified()) {
            return stringBuilder.append("\u5c1a\u672a\u9451\u5b9a").toString();
        }
        if (bl) {
            stringBuilder.append("\u9632\u79a6\u529b ").append(LiToItemTemplates.number(map, "ac")).append('\n');
        } else {
            stringBuilder.append("\u653b\u64ca\u529b ").append(LiToItemTemplates.number(map, "dmg_small")).append('/').append(LiToItemTemplates.number(map, "dmg_large")).append('\n');
        }
        for (String[] stringArray : stringArrayArray = new String[][]{{"add_str", "\u529b\u91cf"}, {"add_dex", "\u654f\u6377"}, {"add_con", "\u9ad4\u8cea"}, {"add_wis", "\u7cbe\u795e"}, {"add_int", "\u667a\u529b"}, {"add_cha", "\u9b45\u529b"}, {"add_hp", "\u9ad4\u529b\u4e0a\u9650"}, {"add_mp", "\u9b54\u529b\u4e0a\u9650"}, {"add_hpr", "HP\u56de\u5fa9"}, {"add_mpr", "MP\u56de\u5fa9"}, {"m_def", "\u9b54\u6cd5\u9632\u79a6"}, {"add_sp", "\u9b54\u653b"}, {bl ? "hit_modifier" : "hitmodifier", "\u653b\u64ca\u6210\u529f"}, {bl ? "dmg_modifier" : "dmgmodifier", "\u984d\u5916\u653b\u64ca\u9ede\u6578"}, {"bow_hit_modifier", "\u9060\u8ddd\u96e2\u653b\u64ca\u6210\u529f"}, {"bow_dmg_modifier", "\u9060\u8ddd\u96e2\u984d\u5916\u653b\u64ca\u9ede\u6578"}, {"add_hitup", "\u8fd1\u8ddd\u96e2\u547d\u4e2d"}, {"add_dmgup", "\u8fd1\u8ddd\u96e2\u50b7\u5bb3"}, {"add_bow_hitup", "\u9060\u8ddd\u96e2\u547d\u4e2d"}, {"add_bow_dmgup", "\u9060\u8ddd\u96e2\u50b7\u5bb3"}, {"damage_reduction", "\u50b7\u5bb3\u6e1b\u514d"}, {"add_melee_redux", "\u8fd1\u8ddd\u96e2\u6e1b\u514d"}, {"add_bow_redux", "\u9060\u8ddd\u96e2\u6e1b\u514d"}, {"add_ignore_reduction", "\u7121\u8996\u6e1b\u514d"}, {"magicdmgmodifier", "\u9b54\u6cd5\u50b7\u5bb3"}, {"add_magic_hit", "\u9b54\u6cd5\u547d\u4e2d"}, {"add_pvp_dmg", "PVP\u50b7\u5bb3"}, {"add_pvp_redux", "PVP\u6e1b\u514d"}, {"add_boss_dmg", "\u5c0dBOSS\u50b7\u5bb3"}, {"add_boss_redux", "\u53d7\u5230BOSS\u6e1b\u50b7", "%"}, {"add_dodge", "\u9583\u907f\u7387"}, {"add_er", "\u9060\u8ddd\u96e2\u8ff4\u907f"}, {"defense_fire", "\u706b\u5c6c\u6027\u9632\u79a6"}, {"defense_water", "\u6c34\u5c6c\u6027\u9632\u79a6"}, {"defense_wind", "\u98a8\u5c6c\u6027\u9632\u79a6"}, {"defense_earth", "\u5730\u5c6c\u6027\u9632\u79a6"}, {"regist_stun", "\u660f\u8ff7\u8010\u6027"}, {"regist_stone", "\u77f3\u5316\u8010\u6027"}, {"regist_sleep", "\u7761\u7720\u8010\u6027"}, {"regist_freeze", "\u51b0\u51cd\u8010\u6027"}, {"regist_sustain", "\u652f\u6490\u8010\u6027"}, {"regist_blind", "\u6697\u9ed1\u8010\u6027"}, {"add_stun_success", "\u6688\u7729\u6210\u529f\u7387"}, {"add_potion_recovery", "\u85e5\u6c34\u6062\u5fa9", "%"}, {"weight_reduction", "\u8ca0\u91cd\u6e1b\u8f15"}}) {
            LiToItemTemplates.row(stringBuilder, map, stringArray[0], stringArray[1], stringArray.length == 3 ? stringArray[2] : "");
        }
        for (String[] stringArray : new String[][]{{"haste_item", "\u52a0\u901f\u6548\u679c"}, {"add_poison_resist", "\u6bd2\u6027\u62b5\u6297"}, {"add_pierce", "\u8cab\u7a7f"}, {"magic_bow", "\u9b54\u6cd5\u7bad\u77e2"}}) {
            if (LiToItemTemplates.number(map, stringArray[0]) <= 0L) continue;
            stringBuilder.append(stringArray[1]).append('\n');
        }
        for (String[] stringArray : new String[][]{{"add_adena_rate", "\u91d1\u5e63\u7372\u53d6"}, {"add_drop_rate", "\u6389\u5bf6\u7372\u53d6"}, {"add_exp_rate", "\u7d93\u9a57\u7372\u53d6"}}) {
            l = LiToItemTemplates.number(map, stringArray[0]);
            if (l == 0L) continue;
            stringBuilder.append(stringArray[1]).append(' ').append(l > 0L ? "+" : "").append(l).append("%\n");
        }
        long l2 = LiToItemTemplates.number(map, "min_lvl");
        long l3 = LiToItemTemplates.number(map, "max_lvl");
        if (l2 > 0L) {
            stringBuilder.append("\u6700\u4f4e\u7b49\u7d1a ").append(l2).append('\n');
        }
        if (l3 > 0L) {
            stringBuilder.append("\u6700\u9ad8\u7b49\u7d1a ").append(l3).append('\n');
        }
        if (!(bl && Set.of("ring", "amulet", "belt", "earring").contains(map.get("type")) || (l = LiToItemTemplates.number(map, "safenchant")) < 0L)) {
            stringBuilder.append("\u5b89\u5b9a\u503c ").append(l).append('\n');
        }
        String[] stringArray = new String[]{"use_royal", "use_knight", "use_elf", "use_mage", "use_darkelf", "use_dragonknight", "use_illusionist"};
        String[] stringArray2 = new String[]{"\u738b\u65cf", "\u9a0e\u58eb", "\u5996\u7cbe", "\u6cd5\u5e2b", "\u9ed1\u6697\u5996\u7cbe", "\u9f8d\u9a0e\u58eb", "\u5e7b\u8853\u58eb"};
        for (int i = 0; i < stringArray.length; ++i) {
            if (LiToItemTemplates.number(map, stringArray[i]) == 0L) continue;
            stringBuilder.append('[').append(stringArray2[i]).append(']');
        }
        stringBuilder.append('\n');
        String string = (String)Map.ofEntries(Map.entry("gemstone", "\u5bf6\u77f3"), Map.entry("metal", "\u91d1\u5c6c"), Map.entry("steel", "\u92fc\u9435"), Map.entry("silver", "\u9280"), Map.entry("gold", "\u91d1"), Map.entry("wood", "\u6728"), Map.entry("leather", "\u76ae"), Map.entry("cloth", "\u5e03"), Map.entry("bone", "\u9aa8"), Map.entry("iron", "\u9435"), Map.entry("mithril", "\u7c73\u7d22\u8389"), Map.entry("oriharukon", "\u5967\u91cc\u54c8\u9b6f\u6839"), Map.entry("blackmithril", "\u9ed1\u8272\u7c73\u7d22\u8389"), Map.entry("platinum", "\u767d\u91d1"), Map.entry("glass", "\u73bb\u7483"), Map.entry("paper", "\u7d19"), Map.entry("liquid", "\u6db2\u9ad4"), Map.entry("plant", "\u690d\u7269"), Map.entry("mineral", "\u7926\u77f3")).get(map.get("material"));
        if (string != null) {
            stringBuilder.append("\u6750\u8cea: ").append(string).append('\n');
        }
        if (LiToItemTemplates.number(map, "cant_delete") != 0L) {
            stringBuilder.append("\u4e0d\u53ef\u522a\u9664\n");
        }
        if (LiToItemTemplates.number(map, "trade") != 0L) {
            stringBuilder.append("\u4e0d\u53ef\u4ea4\u6613\n");
        }
        if (item.durability() != 0) {
            stringBuilder.append("\u640d\u58de\u5ea6 ").append(item.durability()).append('\n');
        }
        if (item.definition().maximumChargeCount() > 0) {
            stringBuilder.append("\u5269\u9918\u6b21\u6578 ").append(item.chargeCount()).append('\n');
        }
        stringBuilder.append("\u91cd\u91cf: ").append(Math.max(LiToItemTemplates.number(map, "weight") > 0L ? 1L : 0L, Math.multiplyExact(LiToItemTemplates.number(map, "weight"), (long)item.quantity()) / 1000L)).append('\n');
        return stringBuilder.toString();
    }

    static boolean needsExtension(Map<String, String> map) {
        boolean bl = map.get("kind").equals("armor");
        int n = 4;
        for (String string : List.of("add_str", "add_dex", "add_con", "add_wis", "add_int", "add_cha", "add_hp", "add_mp", "m_def", "add_sp", "defense_fire", "defense_water", "defense_wind", "defense_earth", "regist_stun", "regist_stone", "regist_sleep", "regist_freeze", "regist_sustain", "regist_blind")) {
            long l;
            long l2 = LiToItemTemplates.number(map, string);
            if (l2 != 0L) {
                ++n;
            }
            if (l2 != 0L && !bl) {
                return true;
            }
            long l3 = l = string.equals("add_hp") || string.equals("add_mp") ? 32767L : 127L;
            if (l2 <= l && l2 >= -l - 1L) continue;
            return true;
        }
        if (n > 16 || Math.abs(LiToItemTemplates.number(map, "ac")) > 255L) {
            return true;
        }
        for (String string : List.of("add_hpr", "add_mpr", "hit_modifier", "dmg_modifier", "bow_hit_modifier", "bow_dmg_modifier", "add_hitup", "add_dmgup", "add_bow_hitup", "add_bow_dmgup", "damage_reduction", "add_melee_redux", "add_bow_redux", "add_ignore_reduction", "magicdmgmodifier", "add_magic_hit", "add_pvp_dmg", "add_pvp_redux", "add_boss_dmg", "add_boss_redux", "add_dodge", "add_er", "add_stun_success", "add_potion_recovery", "weight_reduction", "add_poison_resist", "add_pierce", "magic_bow", "add_adena_rate", "add_drop_rate", "add_exp_rate")) {
            if (LiToItemTemplates.number(map, string) == 0L) continue;
            return true;
        }
        return Math.abs(LiToItemTemplates.number(map, "hitmodifier")) > 127L || Math.abs(LiToItemTemplates.number(map, "dmgmodifier")) > 127L || LiToItemTemplates.number(map, "dmg_small") > 255L || LiToItemTemplates.number(map, "dmg_large") > 255L;
    }
}
