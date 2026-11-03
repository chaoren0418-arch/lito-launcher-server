// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import local.lito381.server.ServerConfig;
import local.lito381.server.SourceNpcPhysicalStats;

final class NativeNpcPhysicalSource {
    private static volatile Snapshot current;

    static Snapshot read(Connection connection, ServerConfig serverConfig) throws SQLException {
        String string = connection.getCatalog();
        for (String string2 : serverConfig.legacySourceResourceDatabases()) {
            if (Objects.equals(string, string2)) continue;
            throw new SQLException("NPC snapshot requires the configured common static database: " + string2);
        }
        return NativeNpcPhysicalSource.read(connection);
    }

    static Snapshot read(Connection connection) throws SQLException {
        HashSet<Integer> hashSet;
        HashMap<Integer, SourceNpcPhysicalStats> hashMap;
        String string;
        block18: {
            string = connection.getCatalog();
            if (string == null || !string.matches("[A-Za-z0-9_]+")) {
                throw new SQLException("NPC snapshot has no qualified static catalog");
            }
            hashMap = new HashMap<Integer, SourceNpcPhysicalStats>();
            hashSet = new HashSet<Integer>();
            String string2 = "SELECT npcid,ac,size,damage_reduction,hard,undead,weakAttr,mr,IsTU,IsErase,lawful,tamable,boss,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk FROM npc";
            try (PreparedStatement preparedStatement = connection.prepareStatement(string2);){
                ResultSet resultSet = preparedStatement.executeQuery();
                block12: while (true) {
                    while (resultSet.next()) {
                        int n = resultSet.getInt("npcid");
                        if (hashMap.containsKey(n) || hashSet.contains(n)) {
                            throw new SQLException("duplicate source NPC physical row: " + n);
                        }
                        try {
                            hashMap.put(n, SourceNpcPhysicalStats.fromSourceRow(n, resultSet.getInt("ac"), NativeNpcPhysicalSource.physicalSize(resultSet.getString("size")), resultSet.getInt("damage_reduction"), resultSet.getBoolean("hard"), resultSet.getInt("undead"), resultSet.getInt("weakAttr"), resultSet.getInt("mr"), resultSet.getBoolean("IsTU"), resultSet.getInt("lawful"), resultSet.getBoolean("tamable"), resultSet.getBoolean("boss"), resultSet.getInt("mp"), resultSet.getInt("str"), resultSet.getInt("dex"), resultSet.getBoolean("IsErase"), resultSet.getInt("ranged"), resultSet.getInt("bowActId"), resultSet.getInt("poison_atk"), resultSet.getInt("paralysis_atk")));
                            continue block12;
                        }
                        catch (IllegalArgumentException illegalArgumentException) {
                            hashSet.add(n);
                        }
                    }
                    break block18;
                    {
                        continue block12;
                        break;
                    }
                    break;
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
        }
        return new Snapshot(string + ".npc", hashMap, hashSet);
    }

    static void publish(Snapshot snapshot) {
        current = Objects.requireNonNull(snapshot);
    }

    static SourceNpcPhysicalStats lookup(String string, int n) throws SQLException {
        if (n <= 0) {
            throw new IllegalArgumentException("invalid source NPC id");
        }
        Snapshot snapshot = current;
        if (snapshot == null) {
            return null;
        }
        if (!snapshot.table.equals(string) && !("`" + snapshot.table.replace(".", "`.`") + "`").equals(string)) {
            throw new SQLException("NPC snapshot static source mismatch: " + string);
        }
        if (snapshot.invalid.contains(n)) {
            throw new SQLException("invalid source NPC physical row: " + n, new IllegalArgumentException("invalid source NPC physical snapshot"));
        }
        SourceNpcPhysicalStats sourceNpcPhysicalStats = snapshot.rows.get(n);
        if (sourceNpcPhysicalStats == null) {
            throw new SQLException("source NPC physical row was not found: " + n);
        }
        return sourceNpcPhysicalStats;
    }

    static String physicalSize(String string) {
        return "".equals(string) ? "small" : string;
    }

    private NativeNpcPhysicalSource() {
    }

    record Snapshot(String table, Map<Integer, SourceNpcPhysicalStats> rows, Set<Integer> invalid) {
        Snapshot {
            map = Map.copyOf(map);
            set = Set.copyOf(set);
        }
    }
}
