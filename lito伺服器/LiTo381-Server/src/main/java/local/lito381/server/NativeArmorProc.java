// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeArmorProc {
    static final String COLUMNS = "id,item_id,enchant_level,proc_group,on_attack_chance,on_hurt_chance,flat_damage,rand_damage,heal_hp,heal_hp_rand,heal_mp,heal_mp_rand,heal_mode,trigger_skill,skill_time_sec,skill_target,gfx_id,gfx_mode";
    private static volatile Catalog current;

    NativeArmorProc() {
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        LinkedHashMap<Integer, List<Rule>> linkedHashMap = new LinkedHashMap<Integer, List<Rule>>();
        int n2 = 0;
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT id,item_id,enchant_level,proc_group,on_attack_chance,on_hurt_chance,flat_damage,rand_damage,heal_hp,heal_hp_rand,heal_mp,heal_mp_rand,heal_mode,trigger_skill,skill_time_sec,skill_target,gfx_id,gfx_mode FROM `\u88dd\u5099_armor_\u9b54\u6cd5` ORDER BY item_id,enchant_level,proc_group,id");){
                while (resultSet.next()) {
                    Rule rule = new Rule(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getInt(4), resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), resultSet.getInt(9), resultSet.getInt(10), resultSet.getInt(11), resultSet.getInt(12), resultSet.getInt(13), resultSet.getInt(14), resultSet.getInt(15), resultSet.getInt(16), resultSet.getInt(17), resultSet.getInt(18));
                    linkedHashMap.computeIfAbsent(rule.item, n -> new ArrayList()).add(rule);
                    ++n2;
                }
            }
            connection.rollback();
        }
        current = new Catalog(string, linkedHashMap, n2);
        System.out.println("[LiTo development] shared static armor Proc rows=" + n2 + "; per-hit SQL=0");
    }

    static Catalog catalog() {
        Catalog catalog = current;
        if (catalog == null) {
            throw new IllegalStateException("armor Proc catalog not loaded at startup");
        }
        return catalog;
    }

    record Catalog(String database, Map<Integer, List<Rule>> items, int rows) {
        Catalog {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            map.forEach((n, list) -> linkedHashMap.put(n, List.copyOf(list)));
            map = Map.copyOf(linkedHashMap);
        }

        List<Rule> selected(int n, int n2) {
            List list = this.items.getOrDefault(n, List.of());
            List<Rule> list2 = n2 < 0 ? List.of() : list.stream().filter(rule -> rule.enchant == n2).toList();
            return list2.isEmpty() ? list.stream().filter(rule -> rule.enchant == -1).toList() : list2;
        }
    }

    record Rule(int id, int item, int enchant, int group, int attack, int hurt, int damage, int randomDamage, int hp, int randomHp, int mp, int randomMp, int healMode, int skill, int seconds, int target, int gfx, int gfxMode) {
        boolean selfHeal() {
            return this.healMode == 1;
        }

        boolean selfSkill() {
            return this.target == 1;
        }

        boolean needsTarget() {
            return this.damage > 0 || this.randomDamage > 0 || (this.hp > 0 || this.randomHp > 0 || this.mp > 0 || this.randomMp > 0) && !this.selfHeal() || this.skill > 0 && !this.selfSkill();
        }
    }
}
