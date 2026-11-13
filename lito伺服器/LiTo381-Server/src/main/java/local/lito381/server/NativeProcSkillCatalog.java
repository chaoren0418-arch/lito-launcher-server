// DECOMPILED from candidate-50635f7e.jar (CFR 0.152), TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.NativeSourceIcons1Policy;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeProcSkillCatalog {
    static final String QUERY = "SELECT skill_id,type,castgfx,damage_value,damage_dice FROM skills ORDER BY skill_id";
    private static volatile Catalog current;

    private NativeProcSkillCatalog() {
    }

    static Catalog read(Connection connection, String string) throws SQLException {
        Map<Integer, Skill> linkedHashMap = new LinkedHashMap<Integer, Skill>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(QUERY);){
            while (resultSet.next()) {
                Skill skill = new Skill(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getInt(4), resultSet.getInt(5));
                if (linkedHashMap.putIfAbsent(skill.id(), skill) != null) {
                    throw new SQLException("duplicate Proc skill_id=" + skill.id());
                }
            }
        }
        if (linkedHashMap.isEmpty()) {
            throw new SQLException("empty Proc skills catalog");
        }
        return NativeSourceIcons1Policy.catalog(string, linkedHashMap, connection);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static synchronized void load(ServerConfig serverConfig) throws Exception {
        Catalog catalog;
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
            try {
                catalog = NativeProcSkillCatalog.read(connection, string);
            }
            finally {
                connection.rollback();
            }
        }
        current = catalog;
    }

    static Catalog catalog() {
        Catalog catalog = current;
        if (catalog == null) {
            throw new IllegalStateException("Proc skills catalog not loaded at startup");
        }
        return catalog;
    }

    record Skill(int id, int type, int castGraphics, int damageValue, int damageDice) {
    }
@AsRecord(components={"database","skills"}, finalFields={"nativeSourceIcons1Policy"})

    static final class Catalog
    {
        private final String database;
        private final Map<Integer, Skill> skills;
        transient Object nativeSourceIcons1Policy;

        Catalog(String string, Map<Integer, Skill> map) {
            map = Map.copyOf(map);
            this.database = string;
            this.skills = map;
        }

        Skill find(int n) {
            return this.skills.get(n);
        }

        boolean healing(int n2) {
            return NativeProcHealingKernel.isHealSkill(n2, n -> {
                Skill skill = this.find(n);
                return skill == null ? 0 : skill.type();
            });
        }

        public String database() {
            return this.database;
        }

        public Map<Integer, Skill> skills() {
            return this.skills;
        }

        Catalog(String string, Map map, Object object) {
            this(string, map);
            this.nativeSourceIcons1Policy = object;
        }
    }
}
