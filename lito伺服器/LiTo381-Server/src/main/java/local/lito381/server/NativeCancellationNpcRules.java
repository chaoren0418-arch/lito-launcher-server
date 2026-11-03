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
import local.lito381.server.ServerConfig;

final class NativeCancellationNpcRules {
    private static volatile Snapshot current;

    static Snapshot read(Connection connection, ServerConfig serverConfig) throws SQLException {
        String string = connection.getCatalog();
        for (String string2 : serverConfig.legacySourceResourceDatabases()) {
            if (Objects.equals(string, string2)) continue;
            throw new SQLException("Cancellation NPC rules require the configured common static database: " + string2);
        }
        return NativeCancellationNpcRules.read(connection);
    }

    static Snapshot read(Connection connection) throws SQLException {
        String string = connection.getCatalog();
        if (string == null || !string.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Cancellation NPC rules have no qualified static catalog");
        }
        HashMap<Integer, Rule> hashMap = new HashMap<Integer, Rule>();
        HashMap<Integer, String> hashMap2 = new HashMap<Integer, String>();
        HashSet<Integer> hashSet = new HashSet<Integer>();
        String string2 = "SELECT r.npc_id,r.transform_type,r.target_id,r.duration_seconds,r.new_name_id,r.new_passispeed,s.npcid AS source_exists,t.npcid AS target_exists FROM `skills_\u76f8\u6d88\u602a\u7269\u8f49\u63db` r LEFT JOIN npc s ON s.npcid=r.npc_id LEFT JOIN npc t ON t.npcid=r.target_id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string2);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                int n = resultSet.getInt("npc_id");
                if (!hashSet.add(n)) {
                    throw new SQLException("duplicate cancellation NPC transform: " + n);
                }
                Rule rule = new Rule(n, resultSet.getInt("transform_type"), resultSet.getInt("target_id"), resultSet.getInt("duration_seconds"), resultSet.getString("new_name_id"), resultSet.getInt("new_passispeed"));
                String string3 = NativeCancellationNpcRules.invalidReason(rule, resultSet.getObject("source_exists") != null, resultSet.getObject("target_exists") != null);
                if (string3 == null) {
                    hashMap.put(n, rule);
                    continue;
                }
                hashMap2.put(n, string3);
            }
        }
        return new Snapshot(string + ".skills_\u76f8\u6d88\u602a\u7269\u8f49\u63db", hashMap, hashMap2);
    }

    static String invalidReason(Rule rule, boolean bl, boolean bl2) {
        if (!bl) {
            return "source NPC does not exist";
        }
        if (rule.type() != 1 && rule.type() != 2) {
            return "transform type must be 1 or 2";
        }
        if (rule.targetId() <= 0) {
            return "target must be positive";
        }
        if (rule.durationSeconds() < 0 || rule.passispeed() < 0) {
            return "duration or passive speed is negative";
        }
        if (rule.type() == 2 && rule.targetId() == rule.npcId()) {
            return "replacement cannot be the source NPC";
        }
        if (rule.type() == 2 && !bl2) {
            return "replacement NPC does not exist";
        }
        return null;
    }

    static void publish(Snapshot snapshot) {
        Objects.requireNonNull(snapshot);
        snapshot.invalid().forEach((n, string) -> System.err.println("[LiTo] cancellation NPC rule skipped: npc=" + n + " reason=" + string));
        current = snapshot;
    }

    static Rule lookup(int n) {
        Snapshot snapshot = current;
        return snapshot == null ? null : snapshot.rows().get(n);
    }

    private NativeCancellationNpcRules() {
    }

    record Snapshot(String table, Map<Integer, Rule> rows, Map<Integer, String> invalid) {
        Snapshot {
            map = Map.copyOf(map);
            map2 = Map.copyOf(map2);
        }
    }

    record Rule(int npcId, int type, int targetId, int durationSeconds, String nameId, int passispeed) {
        Rule {
            string = string == null ? "" : string.trim();
        }

        boolean changesAppearance(int n, int n2) {
            return this.type == 1 && n2 == n && n2 != this.targetId;
        }
    }
}
