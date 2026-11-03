package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Three bounded SELECTs on one caller-owned read-only scoped connection: NPC
 * facts plus mobskill presence in the SAME statement, then both complete growth
 * tables. No fallback table, getInt/getBoolean coercion, process-global cache,
 * DDL, source writes, extra connection, or claim of a multi-statement MyISAM
 * transaction. The caller selects the catalog independently of a player source.
 * This is data qualification, not authorization to spawn or enable NPC combat.
 */
final class ModernNpcCatalogJdbc {
    private ModernNpcCatalogJdbc() {}
    static ModernNpcCatalog.Catalog read(Connection connection, ModernBackendScope scope, int sourceId,
                                         String database, List<Integer> requested) throws Exception {
        scope.checkActive();
        try {
            ModernPhysicalCombat.identifier(database);
            if (sourceId < 1 || connection == null || ModernBackendScope.current() != scope || !connection.isReadOnly()
                    || requested == null || requested.isEmpty() || requested.size() > ModernNpcCatalog.MAX_NPCS) throw unavailable();
            var ids = new TreeSet<Integer>();
            for (Integer id : List.copyOf(requested)) if (id == null || id < 1 || !ids.add(id)) throw unavailable();
            var rows = npcs(connection, scope, sourceId, database, List.copyOf(ids));
            var growth = new ModernPhysicalCombat.Growth(sourceId, database,
                    growth(connection, scope, database, "角色_力量成長"), growth(connection, scope, database, "角色_敏捷成長"));
            scope.checkActive(); return new ModernNpcCatalog.Catalog(sourceId, database, rows, growth);
        } catch (Exception failure) {
            scope.checkActive(); // Cancellation/deadline takes precedence over redacted driver detail.
            throw unavailable();
        }
    }
    private static Map<Integer, ModernNpcCatalog.Npc> npcs(Connection connection, ModernBackendScope scope,
                                                          int sourceId, String database, List<Integer> ids) throws Exception {
        var sql = new StringBuilder("SELECT n.npcid");
        for (var field : ModernNpcCatalog.Field.values()) sql.append(",n.`").append(field.column()).append("` AS ").append(field.alias());
        for (var item : List.of("name", "nameid", "impl", "size")) {
            int limit = item.equals("impl") ? 96 : item.equals("size") ? 10 : 256;
            sql.append(",LEFT(CONVERT(n.`").append(item).append("` USING utf8mb4),").append(limit + 1).append(") AS t_").append(item)
                    .append(",OCTET_LENGTH(CONVERT(n.`").append(item).append("` USING utf8mb4)) AS b_").append(item);
        }
        sql.append(",EXISTS(SELECT 1 FROM `").append(database).append("`.`mobskill` m WHERE m.mobid=n.npcid) AS has_mobskills")
                .append(" FROM `").append(database).append("`.`npc` n WHERE n.npcid IN (")
                .append(String.join(",", java.util.Collections.nCopies(ids.size(), "?")))
                .append(") ORDER BY n.npcid LIMIT 129");
        var result = new TreeMap<Integer, ModernNpcCatalog.Npc>();
        try (var statement = prepare(connection, scope, sql.toString(), 129)) {
            for (int i = 0; i < ids.size(); i++) statement.setLong(i + 1, ids.get(i));
            try (var rows = statement.executeQuery()) {
                int previous = 0;
                while (rows.next()) {
                    scope.checkActive();
                    long rawId = ModernJdbcNumbers.required(rows, "npcid");
                    if (rawId < 1 || rawId > Integer.MAX_VALUE || rawId <= previous || result.size() == ModernNpcCatalog.MAX_NPCS
                            || !ids.contains((int) rawId)) throw unavailable();
                    int id = Math.toIntExact(rawId); previous = id;
                    var numbers = new EnumMap<ModernNpcCatalog.Field, Long>(ModernNpcCatalog.Field.class);
                    for (var field : ModernNpcCatalog.Field.values()) numbers.put(field, ModernJdbcNumbers.required(rows, field.alias()));
                    long skills = ModernJdbcNumbers.required(rows, "has_mobskills");
                    if (skills != 0 && skills != 1) throw unavailable();
                    String size = text(rows, "size", 10, false);
                    if (!size.equals("small") && !size.equals("large")) throw unavailable();
                    var npc = new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(sourceId, database, id),
                            text(rows, "name", 256, false), text(rows, "nameid", 256, true), text(rows, "impl", 96, false),
                            size.equals("large"), new ModernNpcCatalog.Facts(numbers), skills == 1);
                    result.put(id, npc);
                }
            }
        }
        scope.checkActive(); if (!result.keySet().equals(new TreeSet<>(ids))) throw unavailable(); return Map.copyOf(result);
    }
    private static List<ModernPhysicalCombat.GrowthRow> growth(Connection connection, ModernBackendScope scope,
                                                               String database, String table) throws Exception {
        String sql = "SELECT id,`命中` AS hit,`傷害` AS damage FROM `" + database + "`.`" + table + "` ORDER BY id LIMIT 128";
        var result = new ArrayList<ModernPhysicalCombat.GrowthRow>(127);
        try (var statement = prepare(connection, scope, sql, 128); var rows = statement.executeQuery()) {
            while (rows.next()) {
                scope.checkActive();
                long id = ModernJdbcNumbers.required(rows, "id");
                if (result.size() == 127 || id != result.size() + 1) throw unavailable();
                result.add(new ModernPhysicalCombat.GrowthRow(id, ModernJdbcNumbers.required(rows, "hit"), ModernJdbcNumbers.required(rows, "damage")));
            }
        }
        scope.checkActive(); if (result.size() != 127) throw unavailable(); return List.copyOf(result);
    }
    private static String text(ResultSet rows, String column, int maximum, boolean empty) throws SQLException {
        int index = rows.findColumn("t_" + column);
        switch (rows.getMetaData().getColumnType(index)) {
            case java.sql.Types.CHAR, java.sql.Types.VARCHAR, java.sql.Types.LONGVARCHAR,
                    java.sql.Types.NCHAR, java.sql.Types.NVARCHAR, java.sql.Types.LONGNVARCHAR -> { }
            default -> throw unavailable();
        }
        String text = rows.getString("t_" + column); boolean nullValue = rows.wasNull();
        long bytes = ModernJdbcNumbers.required(rows, "b_" + column);
        if (nullValue || text == null || bytes < 0 || bytes > maximum || text.getBytes(StandardCharsets.UTF_8).length != bytes) throw unavailable();
        try { ModernNpcCatalog.text(text, maximum, empty); } catch (IllegalArgumentException invalid) { throw unavailable(); }
        return text;
    }
    private static PreparedStatement prepare(Connection connection, ModernBackendScope scope, String sql, int rows) throws Exception {
        scope.checkActive(); var result = connection.prepareStatement(sql);
        try {
            result.setMaxRows(rows); result.setQueryTimeout((int) Math.max(1, ((long) scope.remainingMillis() + 999) / 1000));
            scope.checkActive(); return result;
        } catch (Exception failure) {
            try { result.close(); } catch (Exception ignored) { }
            scope.checkActive(); throw unavailable();
        }
    }
    private static SQLException unavailable() { return new SQLException("modern NPC catalog unavailable"); }
}
