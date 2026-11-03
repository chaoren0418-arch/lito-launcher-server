package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Central settlement totals only: five bounded SELECT statements, no DDL, write,
 * commit or source-schema access. A read-only transaction pins the table metadata
 * until the owned connection closes; closing discards this read-only transaction.
 * The explicit factory must bound connect AND physical socket retirement at the
 * scope's absolute deadline. setNetworkTimeout alone is not that guarantee.
 * Once open, the session scope registers the owned connection for cancellation.
 * Cancellation while factory.open is still connecting remains bounded by that
 * original deadline; no worker is declared retired before open/close really exit.
 */
final class ModernCharacterRecordJdbc {
    static final String TABLE = "lito_modern_character_record";
    static final String TABLES = "SELECT TABLE_NAME,ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME='"
            + TABLE + "' LIMIT 2";
    static final String COLUMNS = "SELECT COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH"
            + " FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME='" + TABLE + "' ORDER BY ORDINAL_POSITION LIMIT 6";
    static final String KEYS = "SELECT INDEX_NAME,SEQ_IN_INDEX,COLUMN_NAME FROM INFORMATION_SCHEMA.STATISTICS"
            + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='" + TABLE + "' AND NON_UNIQUE=0 ORDER BY INDEX_NAME,SEQ_IN_INDEX LIMIT 3";
    private static final Map<String,String> TYPES = Map.of("source_id", "int", "object_id", "bigint",
            "wins", "bigint", "losses", "bigint", "revision", "bigint");
    private final ModernMatchSettlementJdbc.ConnectionFactory factory;
    private final String database, qualifiedTable;

    ModernCharacterRecordJdbc(ModernMatchSettlementJdbc.ConnectionFactory factory, String database) {
        this.factory = Objects.requireNonNull(factory);
        if (database == null || !database.matches("[A-Za-z0-9_]{1,64}"))
            throw new IllegalArgumentException("explicit character record database required");
        this.database = database; qualifiedTable = "`" + database + "`.`" + TABLE + "`";
    }

    ModernCharacterRecord.Snapshot read(ModernBackendScope scope, TournamentPresence.CharacterKey key) throws Exception {
        Objects.requireNonNull(scope); Objects.requireNonNull(key); scope.checkActive();
        ModernCharacterRecord.Snapshot result;
        try {
            Connection opened = factory.open(scope.deadlineNanos());
            if (opened == null) throw unavailable();
            try (Connection connection = opened) {
                try (var lease = scope.register(connection)) {
                    scope.checkActive();
                    if (connection.isClosed() || !connection.getAutoCommit()) throw unavailable();
                    connection.setNetworkTimeout(Runnable::run, scope.remainingMillis());
                    connection.setReadOnly(true);
                    if (!connection.isReadOnly()) throw unavailable();
                    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    connection.setAutoCommit(false);
                    if (connection.getAutoCommit()) throw unavailable();
                    pin(connection, scope);
                    schema(connection, scope);
                    result = totals(connection, scope, key);
                    scope.checkActive();
                } // Retire this operation's abort lease before physical owner.close.
            } // No publication if closing this actual owned connection fails.
            scope.checkActive();
            return result;
        } catch (Exception failure) {
            scope.checkActive(); // Preserve actual session cancellation/deadline.
            throw unavailable(); // Do not expose SQL, identifiers, credentials or causes.
        }
    }

    private void pin(Connection connection, ModernBackendScope scope) throws Exception {
        try (var statement = prepare(connection, "SELECT source_id,object_id,wins,losses,revision FROM " + qualifiedTable + " LIMIT 0", scope, 1);
             var rows = statement.executeQuery()) {
            if (rows.next()) throw unavailable();
            scope.checkActive();
        }
    }
    private void schema(Connection connection, ModernBackendScope scope) throws Exception {
        try (var statement = metadata(connection, TABLES, scope, 2); var rows = statement.executeQuery()) {
            if (!rows.next() || !TABLE.equalsIgnoreCase(word(rows, "TABLE_NAME", 64))
                    || !"InnoDB".equalsIgnoreCase(word(rows, "ENGINE", 16)) || rows.next()) throw unavailable();
            scope.checkActive();
        }
        var found = new HashSet<String>();
        try (var statement = metadata(connection, COLUMNS, scope, 6); var rows = statement.executeQuery()) {
            while (rows.next()) {
                scope.checkActive();
                String column = word(rows, "COLUMN_NAME", 64).toLowerCase(Locale.ROOT), expected = TYPES.get(column);
                if (found.size() >= TYPES.size() || expected == null || !found.add(column)
                        || !"NO".equals(word(rows, "IS_NULLABLE", 3))) throw unavailable();
                String type = word(rows, "DATA_TYPE", 16).toLowerCase(Locale.ROOT);
                String full = word(rows, "COLUMN_TYPE", 32).toLowerCase(Locale.ROOT);
                if (!expected.equals(type) || !full.matches(expected + "(?:\\([0-9]{1,2}\\))?")
                        || ModernJdbcNumbers.nullable(rows, "CHARACTER_MAXIMUM_LENGTH").isPresent()) throw unavailable();
            }
        }
        if (!found.equals(TYPES.keySet())) throw unavailable();
        List<String> primary = List.of("source_id", "object_id"); int count = 0;
        try (var statement = metadata(connection, KEYS, scope, 3); var rows = statement.executeQuery()) {
            while (rows.next()) {
                scope.checkActive();
                if (count >= primary.size() || !"PRIMARY".equalsIgnoreCase(word(rows, "INDEX_NAME", 64))
                        || ModernJdbcNumbers.required(rows, "SEQ_IN_INDEX") != count + 1L
                        || !primary.get(count).equalsIgnoreCase(word(rows, "COLUMN_NAME", 64))) throw unavailable();
                count++;
            }
        }
        if (count != primary.size()) throw unavailable();
    }
    private ModernCharacterRecord.Snapshot totals(Connection connection, ModernBackendScope scope,
                                                    TournamentPresence.CharacterKey key) throws Exception {
        try (var statement = prepare(connection, "SELECT source_id,object_id,wins,losses,revision FROM " + qualifiedTable
                + " WHERE source_id=? AND object_id=? LIMIT 2", scope, 2)) {
            statement.setInt(1, key.sourceId()); statement.setLong(2, key.objectId()); scope.checkActive();
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) { scope.checkActive(); return ModernCharacterRecord.Snapshot.absent(key); }
                if (ModernJdbcNumbers.required(rows, "source_id") != key.sourceId()
                        || ModernJdbcNumbers.required(rows, "object_id") != key.objectId()) throw unavailable();
                var value = new ModernCharacterRecord.Snapshot(key, true, ModernJdbcNumbers.required(rows, "wins"),
                        ModernJdbcNumbers.required(rows, "losses"), ModernJdbcNumbers.required(rows, "revision"));
                if (rows.next()) throw unavailable();
                scope.checkActive(); return value;
            }
        }
    }
    private PreparedStatement metadata(Connection connection, String sql, ModernBackendScope scope, int maximum) throws Exception {
        var statement = prepare(connection, sql, scope, maximum);
        try { statement.setString(1, database); return statement; }
        catch (Exception failure) { try { statement.close(); } catch (Exception ignored) {} throw failure; }
    }
    private static PreparedStatement prepare(Connection connection, String sql, ModernBackendScope scope, int maximum) throws Exception {
        scope.checkActive(); connection.setNetworkTimeout(Runnable::run, scope.remainingMillis());
        var statement = connection.prepareStatement(sql);
        try {
            statement.setMaxRows(maximum);
            statement.setQueryTimeout((int)Math.max(1L, ((long)scope.remainingMillis() + 999L) / 1000L));
            scope.checkActive(); return statement;
        } catch (Exception failure) { try { statement.close(); } catch (Exception ignored) {} throw failure; }
    }
    private static String word(ResultSet rows, String column, int maximum) throws SQLException {
        String value = rows.getString(column);
        if (value == null || rows.wasNull() || value.isEmpty() || value.length() > maximum
                || value.chars().anyMatch(c -> c < 0x20 || c > 0x7e)) throw unavailable();
        return value;
    }
    private static SQLException unavailable() { return new SQLException("modern character record unavailable"); }
}
