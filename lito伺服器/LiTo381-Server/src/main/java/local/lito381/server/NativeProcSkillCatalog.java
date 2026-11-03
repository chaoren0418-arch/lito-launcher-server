// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
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
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeProcSkillCatalog {
    static final String QUERY = "SELECT skill_id,type,castgfx,damage_value,damage_dice FROM skills ORDER BY skill_id";
    private static volatile Catalog current;

    private NativeProcSkillCatalog() {
    }

    static Catalog read(Connection connection, String string) throws SQLException {
        LinkedHashMap<Integer, Skill> linkedHashMap = new LinkedHashMap<Integer, Skill>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(QUERY);){
            while (resultSet.next()) {
                Skill skill = new Skill(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getInt(4), resultSet.getInt(5));
                if (linkedHashMap.putIfAbsent(skill.id(), skill) == null) continue;
                throw new SQLException("duplicate Proc skill_id=" + skill.id());
            }
        }
        if (linkedHashMap.isEmpty()) {
            throw new SQLException("empty Proc skills catalog");
        }
        return new Catalog(string, linkedHashMap);
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

    record Catalog(String database, Map<Integer, Skill> skills) {
        Catalog {
            map = Map.copyOf(map);
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
    }
}
