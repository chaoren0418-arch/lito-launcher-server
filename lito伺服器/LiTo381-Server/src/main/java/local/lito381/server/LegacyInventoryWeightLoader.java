// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
package local.lito381.server;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import local.lito381.server.BattleInventoryWeight;
import local.lito381.server.LegacyEquipmentMagicLoader;
import local.lito381.server.SourceInventoryWeightStats;


/** Read-only reconstruction of L1Inventory.getWeight() from a selected source. */
final class LegacyInventoryWeightLoader {
    private LegacyInventoryWeightLoader() {}

    static void healthCheck(Connection connection, String resourceDatabase)
            throws SQLException {
        if (connection == null) throw new IllegalArgumentException("legacy connection is required");
        check(connection, "SELECT item_id,count FROM character_items LIMIT 0");
        for (String table : new String[] {"armor", "weapon", "etcitem"}) {
            check(connection, "SELECT item_id,weight FROM "
                    + LegacyEquipmentMagicLoader.qualified(resourceDatabase, table)
                    + " LIMIT 0");
        }
    }

    static SourceInventoryWeightStats load(Connection connection, String string, long l, double d) throws SQLException {
        if (connection == null || l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid legacy inventory-weight request");
        }
        String string2 = "SELECT i.id, i.item_id, i.count,\n       a.item_id AS armor_id, a.weight AS armor_weight,\n       w.item_id AS weapon_id, w.weight AS weapon_weight,\n       e.item_id AS etc_id, e.weight AS etc_weight\n  FROM character_items i\n  LEFT JOIN (SELECT `\u5099\u8a3b` AS `note`,`item_id` AS `item_id`,`\u5957\u88dd\u7de8\u865f` AS `set_id`,`\u540d\u7a31` AS `name`,`\u57f7\u884c\u985e\u5225` AS `classname`,`\u540d\u7a31\u5b57\u4e32\u7de8\u865f` AS `name_id`,`\u88dd\u5099\u985e\u578b` AS `type`,`\u6750\u8cea` AS `material`,`\u91cd\u91cf` AS `weight`,`\u80cc\u5305\u5716\u793a\u7de8\u865f` AS `invgfx`,`\u5730\u9762\u5716\u793a\u7de8\u865f` AS `grdgfx`,`\u9053\u5177\u8aaa\u660e\u7de8\u865f` AS `itemdesc_id`,`\u9632\u79a6\u503c` AS `ac`,`\u5b89\u5b9a\u5f37\u5316\u503c` AS `safenchant`,`\u738b\u65cf\u53ef\u7528` AS `use_royal`,`\u9a0e\u58eb\u53ef\u7528` AS `use_knight`,`\u6cd5\u5e2b\u53ef\u7528` AS `use_mage`,`\u5996\u7cbe\u53ef\u7528` AS `use_elf`,`\u9ed1\u6697\u5996\u7cbe\u53ef\u7528` AS `use_darkelf`,`\u9f8d\u9a0e\u58eb\u53ef\u7528` AS `use_dragonknight`,`\u5e7b\u8853\u5e2b\u53ef\u7528` AS `use_illusionist`,`\u529b\u91cf\u52a0\u6210` AS `add_str`,`\u9ad4\u8cea\u52a0\u6210` AS `add_con`,`\u654f\u6377\u52a0\u6210` AS `add_dex`,`\u667a\u529b\u52a0\u6210` AS `add_int`,`\u7cbe\u795e\u52a0\u6210` AS `add_wis`,`\u9b45\u529b\u52a0\u6210` AS `add_cha`,`\u9ad4\u529b\u52a0\u6210` AS `add_hp`,`\u9b54\u529b\u52a0\u6210` AS `add_mp`,`\u9ad4\u529b\u56de\u5fa9\u52a0\u6210` AS `add_hpr`,`\u9b54\u529b\u56de\u5fa9\u52a0\u6210` AS `add_mpr`,`\u9b54\u653b\u52a0\u6210` AS `add_sp`,`\u6700\u4f4e\u4f7f\u7528\u7b49\u7d1a` AS `min_lvl`,`\u6700\u9ad8\u4f7f\u7528\u7b49\u7d1a` AS `max_lvl`,`\u9b54\u6cd5\u9632\u79a6` AS `m_def`,`\u52a0\u901f\u6548\u679c` AS `haste_item`,`\u50b7\u5bb3\u6e1b\u514d` AS `damage_reduction`,`\u9b54\u6cd5\u50b7\u5bb3\u4fee\u6b63` AS `magicdmgmodifier`,`\u8fd1\u6230\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_melee_redux`,`\u9060\u8ddd\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_bow_redux`,`\u8ca0\u91cd\u6e1b\u8f15` AS `weight_reduction`,`\u8fd1\u6230\u547d\u4e2d\u4fee\u6b63` AS `hit_modifier`,`\u8fd1\u6230\u50b7\u5bb3\u4fee\u6b63` AS `dmg_modifier`,`\u9060\u8ddd\u547d\u4e2d\u4fee\u6b63` AS `bow_hit_modifier`,`\u9060\u8ddd\u50b7\u5bb3\u4fee\u6b63` AS `bow_dmg_modifier`,`\u795d\u798f\u72c0\u614b` AS `bless`,`\u7981\u6b62\u4ea4\u6613` AS `trade`,`\u7981\u6b62\u522a\u9664` AS `cant_delete`,`\u6700\u5927\u4f7f\u7528\u6642\u9593` AS `max_use_time`,`\u6c34\u5c6c\u6027\u9632\u79a6` AS `defense_water`,`\u98a8\u5c6c\u6027\u9632\u79a6` AS `defense_wind`,`\u706b\u5c6c\u6027\u9632\u79a6` AS `defense_fire`,`\u5730\u5c6c\u6027\u9632\u79a6` AS `defense_earth`,`\u660f\u8ff7\u8010\u6027` AS `regist_stun`,`\u77f3\u5316\u8010\u6027` AS `regist_stone`,`\u7761\u7720\u8010\u6027` AS `regist_sleep`,`\u51b0\u51cd\u8010\u6027` AS `regist_freeze`,`\u652f\u6490\u8010\u6027` AS `regist_sustain`,`\u9ed1\u6697\u8010\u6027` AS `regist_blind`,`\u6d3b\u52d5\u98fe\u54c1` AS `is_activity`,`\u73a9\u5bb6\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_pvp_redux`,`\u7121\u8996\u50b7\u5bb3\u6e1b\u514d` AS `add_ignore_reduction`,`\u5c0d\u73a9\u5bb6\u50b7\u5bb3\u52a0\u6210` AS `add_pvp_dmg`,`\u5c0d\u9996\u9818\u50b7\u5bb3\u52a0\u6210` AS `add_boss_dmg`,`\u9996\u9818\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_boss_redux`,`\u9583\u907f\u52a0\u6210` AS `add_dodge`,`\u9060\u8ddd\u8ff4\u907f\u52a0\u6210` AS `add_er`,`\u8fd1\u6230\u547d\u4e2d\u8ffd\u52a0` AS `add_hitup`,`\u8fd1\u6230\u50b7\u5bb3\u8ffd\u52a0` AS `add_dmgup`,`\u9060\u8ddd\u547d\u4e2d\u8ffd\u52a0` AS `add_bow_hitup`,`\u9060\u8ddd\u50b7\u5bb3\u8ffd\u52a0` AS `add_bow_dmgup`,`\u660f\u8ff7\u6210\u529f\u7387\u52a0\u6210` AS `add_stun_success`,`\u85e5\u6c34\u56de\u5fa9\u52a0\u6210` AS `add_potion_recovery`,`\u6bd2\u6027\u62b5\u6297` AS `add_poison_resist`,`\u91d1\u5e63\u500d\u7387\u52a0\u6210` AS `add_adena_rate`,`\u6389\u5bf6\u500d\u7387\u52a0\u6210` AS `add_drop_rate`,`\u7d93\u9a57\u500d\u7387\u52a0\u6210` AS `add_exp_rate`,`\u9b54\u6cd5\u547d\u4e2d\u52a0\u6210` AS `add_magic_hit`,`\u521d\u59cb\u9ad8\u968e\u7b49\u7d1a` AS `greater_tier` FROM %s) a ON a.item_id=i.item_id\n  LEFT JOIN (SELECT `item_id` AS `item_id`,`\u5957\u88dd\u7de8\u865f` AS `set_id`,`\u540d\u7a31` AS `name`,`\u57f7\u884c\u985e\u5225` AS `classname`,`\u540d\u7a31\u5b57\u4e32\u7de8\u865f` AS `name_id`,`\u88dd\u5099\u985e\u578b` AS `type`,`\u6750\u8cea` AS `material`,`\u91cd\u91cf` AS `weight`,`\u80cc\u5305\u5716\u793a\u7de8\u865f` AS `invgfx`,`\u5730\u9762\u5716\u793a\u7de8\u865f` AS `grdgfx`,`\u9053\u5177\u8aaa\u660e\u7de8\u865f` AS `itemdesc_id`,`\u5c0f\u578b\u76ee\u6a19\u50b7\u5bb3` AS `dmg_small`,`\u5927\u578b\u76ee\u6a19\u50b7\u5bb3` AS `dmg_large`,`\u653b\u64ca\u8ddd\u96e2` AS `range`,`\u5b89\u5b9a\u5f37\u5316\u503c` AS `safenchant`,`\u738b\u65cf\u53ef\u7528` AS `use_royal`,`\u9a0e\u58eb\u53ef\u7528` AS `use_knight`,`\u6cd5\u5e2b\u53ef\u7528` AS `use_mage`,`\u5996\u7cbe\u53ef\u7528` AS `use_elf`,`\u9ed1\u6697\u5996\u7cbe\u53ef\u7528` AS `use_darkelf`,`\u9f8d\u9a0e\u58eb\u53ef\u7528` AS `use_dragonknight`,`\u5e7b\u8853\u5e2b\u53ef\u7528` AS `use_illusionist`,`\u8fd1\u6230\u547d\u4e2d\u4fee\u6b63` AS `hitmodifier`,`\u8fd1\u6230\u50b7\u5bb3\u4fee\u6b63` AS `dmgmodifier`,`\u8fd1\u6230\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_melee_redux`,`\u9060\u8ddd\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_bow_redux`,`\u529b\u91cf\u52a0\u6210` AS `add_str`,`\u9ad4\u8cea\u52a0\u6210` AS `add_con`,`\u654f\u6377\u52a0\u6210` AS `add_dex`,`\u667a\u529b\u52a0\u6210` AS `add_int`,`\u7cbe\u795e\u52a0\u6210` AS `add_wis`,`\u9b45\u529b\u52a0\u6210` AS `add_cha`,`\u9ad4\u529b\u52a0\u6210` AS `add_hp`,`\u9b54\u529b\u52a0\u6210` AS `add_mp`,`\u9ad4\u529b\u56de\u5fa9\u52a0\u6210` AS `add_hpr`,`\u9b54\u529b\u56de\u5fa9\u52a0\u6210` AS `add_mpr`,`\u9b54\u653b\u52a0\u6210` AS `add_sp`,`\u9b54\u6cd5\u9632\u79a6` AS `m_def`,`\u52a0\u901f\u6548\u679c` AS `haste_item`,`\u9b54\u6cd5\u50b7\u5bb3\u4fee\u6b63` AS `magicdmgmodifier`,`\u53ef\u640d\u58de` AS `canbedmg`,`\u6700\u4f4e\u4f7f\u7528\u7b49\u7d1a` AS `min_lvl`,`\u6700\u9ad8\u4f7f\u7528\u7b49\u7d1a` AS `max_lvl`,`\u795d\u798f\u72c0\u614b` AS `bless`,`\u7981\u6b62\u4ea4\u6613` AS `trade`,`\u7981\u6b62\u522a\u9664` AS `cant_delete`,`\u6700\u5927\u4f7f\u7528\u6642\u9593` AS `max_use_time`,`\u5c0d\u73a9\u5bb6\u50b7\u5bb3\u52a0\u6210` AS `add_pvp_dmg`,`\u7121\u8996\u50b7\u5bb3\u6e1b\u514d` AS `add_ignore_reduction`,`\u73a9\u5bb6\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_pvp_redux`,`\u5c0d\u9996\u9818\u50b7\u5bb3\u52a0\u6210` AS `add_boss_dmg`,`\u9996\u9818\u50b7\u5bb3\u6e1b\u514d\u52a0\u6210` AS `add_boss_redux`,`\u9583\u907f\u52a0\u6210` AS `add_dodge`,`\u9060\u8ddd\u8ff4\u907f\u52a0\u6210` AS `add_er`,`\u8fd1\u6230\u547d\u4e2d\u8ffd\u52a0` AS `add_hitup`,`\u8fd1\u6230\u50b7\u5bb3\u8ffd\u52a0` AS `add_dmgup`,`\u9060\u8ddd\u547d\u4e2d\u8ffd\u52a0` AS `add_bow_hitup`,`\u9060\u8ddd\u50b7\u5bb3\u8ffd\u52a0` AS `add_bow_dmgup`,`\u660f\u8ff7\u6210\u529f\u7387\u52a0\u6210` AS `add_stun_success`,`\u85e5\u6c34\u56de\u5fa9\u52a0\u6210` AS `add_potion_recovery`,`\u6bd2\u6027\u62b5\u6297` AS `add_poison_resist`,`\u6389\u5bf6\u500d\u7387\u52a0\u6210` AS `add_drop_rate`,`\u91d1\u5e63\u500d\u7387\u52a0\u6210` AS `add_adena_rate`,`\u7d93\u9a57\u500d\u7387\u52a0\u6210` AS `add_exp_rate`,`\u9b54\u6cd5\u547d\u4e2d\u52a0\u6210` AS `add_magic_hit`,`\u8cab\u7a7f\u6548\u679c` AS `add_pierce`,`\u9b54\u6cd5\u5f13` AS `magic_bow`,`\u7bad\u77e2\u52d5\u756b\u7de8\u865f` AS `arrow_spr`,`\u521d\u59cb\u9ad8\u968e\u7b49\u7d1a` AS `greater_tier`,`\u66b4\u64ca\u6a5f\u7387` AS `dmg_cri_chance`,`\u66b4\u64ca\u500d\u7387` AS `dmg_cri`,`\u6700\u5927\u50b7\u5bb3\u6a5f\u7387` AS `dmg_max_chance`,`\u66b4\u64ca\u6a19\u8a18\u7279\u6548` AS `dmg_cri_effect`,`\u6700\u5927\u50b7\u5bb3\u6a19\u8a18\u7279\u6548` AS `dmg_max_effect` FROM %s) w ON w.item_id=i.item_id\n  LEFT JOIN %s e ON e.item_id=i.item_id\n WHERE i.char_id=?\n ORDER BY i.id\n".formatted(LegacyEquipmentMagicLoader.qualified(string, "armor"), LegacyEquipmentMagicLoader.qualified(string, "weapon"), LegacyEquipmentMagicLoader.qualified(string, "etcitem"));
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement(string2);){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    int n6;
                    long l2 = resultSet.getLong("count");
                    boolean bl = resultSet.getObject("armor_id") != null;
                    boolean bl2 = resultSet.getObject("weapon_id") != null;
                    boolean bl3 = resultSet.getObject("etc_id") != null;
                    int n7 = (bl ? 1 : 0) + (bl2 ? 1 : 0) + (bl3 ? 1 : 0);
                    if (l2 <= 0L || n7 != 1) {
                        throw new SQLException("inventory item has invalid count or template: object=" + resultSet.getLong("id") + " item=" + resultSet.getInt("item_id"));
                    }
                    if (bl) {
                        n6 = resultSet.getInt("armor_weight");
                        ++n3;
                    } else if (bl2) {
                        n6 = resultSet.getInt("weapon_weight");
                        ++n4;
                    } else {
                        n6 = resultSet.getInt("etc_weight");
                        ++n5;
                    }
                    if (n6 < 0) {
                        throw new SQLException("inventory template has negative weight: item=" + resultSet.getInt("item_id"));
                    }
                    n = BattleInventoryWeight.addWeight(n, BattleInventoryWeight.itemWeight(l2, n6));
                    ++n2;
                }
            }
        }
        return new SourceInventoryWeightStats(n, n2, n3, n4, n5, d);
    }

    private static void check(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet ignored = statement.executeQuery()) {
            // Metadata-only startup validation.
        }
    }
}
