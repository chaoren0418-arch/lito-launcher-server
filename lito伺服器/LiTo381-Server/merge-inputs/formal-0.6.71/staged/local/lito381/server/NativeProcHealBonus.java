// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeProcHealBonus {
    private static volatile Catalog current;

    NativeProcHealBonus() {
    }

    static Catalog read(Connection connection) throws SQLException {
        LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<Integer, Integer>();
        LinkedHashMap<Integer, Integer> linkedHashMap2 = new LinkedHashMap<Integer, Integer>();
        boolean bl = false;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM `\u89d2\u8272_\u667a\u529b\u6210\u9577`");){
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            for (int n = 1; n <= resultSetMetaData.getColumnCount(); ++n) {
                if (resultSetMetaData.getColumnLabel(n).equals("\u6cbb\u7652\u9ab0\u52a0\u6210")) { bl = true; }
            }
            while (resultSet.next()) {
                int n = resultSet.getInt("id");
                int n2 = resultSet.getInt("\u9b54\u6cd5\u52a0\u6210");
                linkedHashMap.put(n, n2);
                linkedHashMap2.put(n, bl ? resultSet.getInt("\u6cbb\u7652\u9ab0\u52a0\u6210") : Math.min(n2, 10));
            }
        }
        return new Catalog(linkedHashMap, linkedHashMap2, bl);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static synchronized void load(ServerConfig serverConfig) throws Exception {
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        Catalog catalog;
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
            try {
                catalog = NativeProcHealBonus.read(connection);
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
            throw new IllegalStateException("Proc healing INT catalog not loaded at startup");
        }
        return catalog;
    }

    record Catalog(Map<Integer, Integer> magic, Map<Integer, Integer> healing, boolean explicitColumn) {
        Catalog {
            magic = Map.copyOf(magic);
            healing = Map.copyOf(healing);
        }

        int bonus(int n) {
            if (n < 1) {
                return -2;
            }
            int n2 = Math.min(n, 127);
            return this.healing.getOrDefault(n2, Math.min(this.magic.getOrDefault(n2, 0), 10));
        }
    }
}
