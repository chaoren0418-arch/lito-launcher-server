package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** One bounded, owned, SELECT-only repeatable-read startup batch; no source DAO, commit or fallback. */
final class ModernWorldPolicyJdbc {
    static final String HEADER = "lito_modern_world_policy", LAYOUT = "lito_modern_world_layout";
    private record Column(String type, int length, boolean nullable) {}
    private static final Map<String, Map<String, Column>> SCHEMA = schema();
    private static final Map<String, List<String>> KEYS = Map.of(HEADER + ".primary", List.of("namespace_sha256", "revision"),
            LAYOUT + ".primary", List.of("namespace_sha256", "revision", "mode", "logical_map_id"));
    private static final String FILTER = " TABLE_SCHEMA=? AND TABLE_NAME IN ('" + HEADER + "','" + LAYOUT + "')";
    static final String TABLES = "SELECT TABLE_NAME,ENGINE,TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE" + FILTER + " LIMIT 3";
    static final String COLUMNS = "SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH,EXTRA"
            + " FROM INFORMATION_SCHEMA.COLUMNS WHERE" + FILTER + " ORDER BY TABLE_NAME,ORDINAL_POSITION LIMIT 22";
    static final String INDEXES = "SELECT TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM INFORMATION_SCHEMA.STATISTICS WHERE"
            + FILTER + " AND NON_UNIQUE=0 ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX LIMIT 7";
    private final ModernMatchSettlementJdbc.ConnectionFactory factory;
    private final String database;
    private final LongSupplier nanoClock;

    ModernWorldPolicyJdbc(ModernMatchSettlementJdbc.ConnectionFactory factory, String database) { this(factory, database, System::nanoTime); }
    ModernWorldPolicyJdbc(ModernMatchSettlementJdbc.ConnectionFactory factory, String database, LongSupplier nanoClock) {
        this.factory = Objects.requireNonNull(factory); this.nanoClock = Objects.requireNonNull(nanoClock);
        if (database == null || !database.matches("[A-Za-z0-9_]{1,64}")) throw new IllegalArgumentException("explicit world policy database required");
        this.database = database;
    }
    ModernWorldPolicy.Snapshot read(ModernWorldPolicy.Pin pin, long absoluteDeadlineNanos) throws Exception {
        return read(pin,absoluteDeadlineNanos,false);
    }
    /** Explicit administrator inspection of published=0 only. Its output is never listener authority. */
    ModernWorldPolicy.Snapshot readCandidate(ModernWorldPolicy.Pin selection,long absoluteDeadlineNanos)throws Exception {
        return read(selection,absoluteDeadlineNanos,true);
    }
    private ModernWorldPolicy.Snapshot read(ModernWorldPolicy.Pin pin,long absoluteDeadlineNanos,boolean candidate)throws Exception {
        Objects.requireNonNull(pin); check(absoluteDeadlineNanos);
        if (absoluteDeadlineNanos - nanoClock.getAsLong() > TimeUnit.SECONDS.toNanos(30)) throw unavailable();
        ModernWorldPolicy.Snapshot snapshot;
        try {
            Connection opened = factory.open(absoluteDeadlineNanos);
            if (opened == null) throw unavailable();
            try (Connection connection = opened) {
                check(absoluteDeadlineNanos);
                if (connection.isClosed() || !connection.getAutoCommit()) throw unavailable();
                connection.setNetworkTimeout(Runnable::run, remaining(absoluteDeadlineNanos));
                connection.setReadOnly(true);
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                connection.setAutoCommit(false);
                if (!connection.isReadOnly() || connection.getAutoCommit()
                        || connection.getTransactionIsolation() != Connection.TRANSACTION_REPEATABLE_READ) throw unavailable();
                // Both physical tables are pinned before inspecting metadata. No row-locking reads or DDL.
                for (String table : List.of(HEADER, LAYOUT)) {
                    try (var statement = prepare(connection, "SELECT namespace_sha256,revision FROM " + table(table) + " LIMIT 0", absoluteDeadlineNanos, 1);
                         var rows = statement.executeQuery()) { if (rows.next()) throw unavailable(); check(absoluteDeadlineNanos); }
                }
                schema(connection, absoluteDeadlineNanos);
                int count = header(connection, pin, absoluteDeadlineNanos,candidate);
                var layouts = layouts(connection, pin, absoluteDeadlineNanos);
                if (layouts.size() != count) throw unavailable();
                var verified=candidate?new ModernWorldPolicy.Pin(pin.namespaceSha256(),pin.revision(),ModernWorldPolicy.contentHash(pin.namespaceSha256(),pin.revision(),layouts)):pin;
                snapshot = new ModernWorldPolicy.Snapshot(verified, layouts);
                check(absoluteDeadlineNanos);
            } // No publication before the owned physical connection really retires (including close failure).
            check(absoluteDeadlineNanos);
            return snapshot;
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            throw unavailable(); // Never expose SQL, database names, row values or credentials.
        }
    }
    private int header(Connection connection, ModernWorldPolicy.Pin pin, long deadline,boolean candidate) throws Exception {
        try (var statement = selected(connection, "SELECT namespace_sha256,revision,schema_version,binding_count,content_sha256,published FROM "
                + table(HEADER) + " WHERE namespace_sha256=? AND revision=? LIMIT 2", pin, deadline, 2);
             var rows = statement.executeQuery()) {
            if (!rows.next()) throw unavailable();
            identity(rows, pin);
            if (number(rows, "schema_version") != 1 || number(rows, "published") != (candidate?0:1)) throw unavailable();
            if(candidate){byte[] bytes=rows.getBytes("content_sha256");if(bytes==null||rows.wasNull()||bytes.length!=32)throw unavailable();}
            else if(!binary(rows,"content_sha256").equals(pin.contentSha256()))throw unavailable();
            int count = Math.toIntExact(number(rows, "binding_count"));
            if (count < 1 || count > ModernWorldPolicy.MAX_BINDINGS || rows.next()) throw unavailable();
            check(deadline); return count;
        }
    }
    private List<ModernWorldPolicy.Row> layouts(Connection connection, ModernWorldPolicy.Pin pin, long deadline) throws Exception {
        String projection = "namespace_sha256,revision,mode,logical_map_id,physical_map_id,manifest_sha256,anchor_x,anchor_y,"
                + "camp0_x,camp0_y,camp1_x,camp1_y,load_timeout_millis,countdown_nanos,active_timeout_millis";
        try (var statement = selected(connection, "SELECT " + projection + " FROM " + table(LAYOUT)
                + " WHERE namespace_sha256=? AND revision=? ORDER BY mode,logical_map_id LIMIT 41", pin, deadline, 41);
             var rows = statement.executeQuery()) {
            var result = new ArrayList<ModernWorldPolicy.Row>();
            while (rows.next()) {
                check(deadline); if (result.size() >= ModernWorldPolicy.MAX_BINDINGS) throw unavailable(); identity(rows, pin);
                Integer x0 = integer(rows, "camp0_x"), y0 = integer(rows, "camp0_y"), x1 = integer(rows, "camp1_x"), y1 = integer(rows, "camp1_y");
                int present = (x0 != null ? 1 : 0) + (y0 != null ? 1 : 0) + (x1 != null ? 1 : 0) + (y1 != null ? 1 : 0);
                if (present != 0 && present != 4) throw unavailable();
                var camps = present == 0 ? List.<ModernWorldMap.Cell>of()
                        : List.of(new ModernWorldMap.Cell(x0, y0), new ModernWorldMap.Cell(x1, y1));
                var countdown = ModernJdbcNumbers.nullable(rows, "countdown_nanos");
                var clocks = new ModernWorldService.ClockOverrides(integer(rows, "load_timeout_millis"),
                        countdown.isPresent() ? countdown.getAsLong() : null, integer(rows, "active_timeout_millis"));
                result.add(new ModernWorldPolicy.Row(new ModernWorldBinding.Key(Math.toIntExact(number(rows, "mode")), Math.toIntExact(number(rows, "logical_map_id"))),
                        Math.toIntExact(number(rows, "physical_map_id")), binary(rows, "manifest_sha256"),
                        new ModernWorldMap.Cell(Math.toIntExact(number(rows, "anchor_x")), Math.toIntExact(number(rows, "anchor_y"))), camps, clocks));
            }
            check(deadline); return List.copyOf(result);
        }
    }
    private static void identity(ResultSet rows, ModernWorldPolicy.Pin pin) throws Exception {
        if (!binary(rows, "namespace_sha256").equals(pin.namespaceSha256()) || number(rows, "revision") != pin.revision()) throw unavailable();
    }
    private void schema(Connection connection, long deadline) throws Exception {
        var tables = new HashSet<String>();
        try (var statement = metadata(connection, TABLES, deadline, 3); var rows = statement.executeQuery()) {
            while (rows.next()) {
                check(deadline); String table = word(rows, "TABLE_NAME", 64).toLowerCase(Locale.ROOT);
                if (!SCHEMA.containsKey(table) || !tables.add(table) || !"InnoDB".equalsIgnoreCase(word(rows, "ENGINE", 16))
                        || !"BASE TABLE".equals(word(rows, "TABLE_TYPE", 16))) throw unavailable();
            }
        }
        if (!tables.equals(SCHEMA.keySet())) throw unavailable();
        var columns = new HashSet<String>();
        try (var statement = metadata(connection, COLUMNS, deadline, 22); var rows = statement.executeQuery()) {
            while (rows.next()) {
                check(deadline); String table = word(rows, "TABLE_NAME", 64).toLowerCase(Locale.ROOT), name = word(rows, "COLUMN_NAME", 64).toLowerCase(Locale.ROOT);
                Column expected = SCHEMA.getOrDefault(table, Map.of()).get(name);
                if (expected == null || !columns.add(table + "." + name)
                        || !(expected.nullable() ? "YES" : "NO").equals(word(rows, "IS_NULLABLE", 3))) throw unavailable();
                String type = word(rows, "DATA_TYPE", 16).toLowerCase(Locale.ROOT), full = word(rows, "COLUMN_TYPE", 32).toLowerCase(Locale.ROOT);
                String extra = rows.getString("EXTRA"); if (extra == null || rows.wasNull() || !extra.isEmpty()) throw unavailable();
                var length = ModernJdbcNumbers.nullable(rows, "CHARACTER_MAXIMUM_LENGTH");
                if (!type.equals(expected.type())) throw unavailable();
                if (expected.length() > 0) {
                    if (length.isEmpty() || length.getAsLong() != expected.length() || !full.equals(type + "(" + expected.length() + ")")) throw unavailable();
                } else if (length.isPresent() || !full.matches(type + "(?:\\([0-9]{1,2}\\))?")) throw unavailable();
            }
        }
        if (columns.size() != SCHEMA.values().stream().mapToInt(Map::size).sum()) throw unavailable();
        var keys = new HashMap<String, List<String>>(); int count = 0;
        try (var statement = metadata(connection, INDEXES, deadline, 7); var rows = statement.executeQuery()) {
            while (rows.next()) {
                check(deadline); if (++count > 6) throw unavailable();
                String key = word(rows, "TABLE_NAME", 64).toLowerCase(Locale.ROOT) + "." + word(rows, "INDEX_NAME", 64).toLowerCase(Locale.ROOT);
                var expected = KEYS.get(key); var actual = keys.computeIfAbsent(key, ignored -> new ArrayList<>());
                if (expected == null || actual.size() >= expected.size() || number(rows, "SEQ_IN_INDEX") != actual.size() + 1
                        || !expected.get(actual.size()).equalsIgnoreCase(word(rows, "COLUMN_NAME", 64))
                        || ModernJdbcNumbers.nullable(rows, "SUB_PART").isPresent()) throw unavailable();
                actual.add(expected.get(actual.size()));
            }
        }
        if (!keys.equals(KEYS)) throw unavailable(); check(deadline);
    }
    private PreparedStatement selected(Connection connection, String sql, ModernWorldPolicy.Pin pin, long deadline, int maximum) throws Exception {
        var statement = prepare(connection, sql, deadline, maximum);
        try { statement.setBytes(1, HexFormat.of().parseHex(pin.namespaceSha256())); statement.setLong(2, pin.revision()); return statement; }
        catch (Exception failure) { try { statement.close(); } catch (Exception ignored) {} throw failure; }
    }
    private PreparedStatement metadata(Connection connection, String sql, long deadline, int maximum) throws Exception {
        var statement = prepare(connection, sql, deadline, maximum);
        try { statement.setString(1, database); return statement; }
        catch (Exception failure) { try { statement.close(); } catch (Exception ignored) {} throw failure; }
    }
    private PreparedStatement prepare(Connection connection, String sql, long deadline, int maximum) throws Exception {
        connection.setNetworkTimeout(Runnable::run, remaining(deadline));
        var statement = connection.prepareStatement(sql);
        try { statement.setMaxRows(maximum); statement.setQueryTimeout((remaining(deadline) + 999) / 1000); check(deadline); return statement; }
        catch (Exception failure) { try { statement.close(); } catch (Exception ignored) {} throw failure; }
    }
    private int remaining(long deadline) throws SQLException { check(deadline); return (int)Math.max(1, (deadline - nanoClock.getAsLong() + 999_999) / 1_000_000); }
    private void check(long deadline) throws SQLException { if (Thread.currentThread().isInterrupted() || deadline - nanoClock.getAsLong() <= 0) throw unavailable(); }
    private String table(String name) { return "`" + database + "`.`" + name + "`"; }
    private static long number(ResultSet rows, String column) throws SQLException { return ModernJdbcNumbers.required(rows, column); }
    private static Integer integer(ResultSet rows, String column) throws SQLException {
        var value = ModernJdbcNumbers.nullable(rows, column); return value.isEmpty() ? null : Math.toIntExact(value.getAsLong());
    }
    private static String binary(ResultSet rows, String column) throws SQLException {
        byte[] value = rows.getBytes(column); if (value == null || rows.wasNull() || value.length != 32) throw unavailable();
        String hash = HexFormat.of().formatHex(value); ModernWorldPolicy.hash(hash); return hash;
    }
    private static String word(ResultSet rows, String column, int maximum) throws SQLException {
        String value = rows.getString(column);
        if (value == null || rows.wasNull() || value.isEmpty() || value.length() > maximum || value.chars().anyMatch(c -> c < 32 || c > 126)) throw unavailable();
        return value;
    }
    private static Map<String, Map<String, Column>> schema() {
        var header = new LinkedHashMap<String, Column>(); header.put("namespace_sha256", new Column("binary", 32, false)); header.put("revision", new Column("bigint", 0, false));
        for (String name : List.of("schema_version", "binding_count")) header.put(name, new Column("int", 0, false));
        header.put("content_sha256", new Column("binary", 32, false)); header.put("published", new Column("tinyint", 0, false));
        var layout = new LinkedHashMap<String, Column>(); layout.put("namespace_sha256", new Column("binary", 32, false)); layout.put("revision", new Column("bigint", 0, false));
        for (String name : List.of("mode", "logical_map_id", "physical_map_id")) layout.put(name, new Column("int", 0, false));
        layout.put("manifest_sha256", new Column("binary", 32, false));
        for (String name : List.of("anchor_x", "anchor_y")) layout.put(name, new Column("int", 0, false));
        for (String name : List.of("camp0_x", "camp0_y", "camp1_x", "camp1_y", "load_timeout_millis", "active_timeout_millis")) layout.put(name, new Column("int", 0, true));
        layout.put("countdown_nanos", new Column("bigint", 0, true));
        return Map.of(HEADER, Map.copyOf(header), LAYOUT, Map.copyOf(layout));
    }
    private static SQLException unavailable() { return new SQLException("dedicated world policy unavailable"); }
}
