package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Source-only item lifetime evidence, not permission to equip or a database expiry worker.
 * One bounded SELECT samples the DB local clock and item local DATETIME together. It neither
 * guesses a timestamp zone nor adds query latency to the conservative JVM deadline.
 * The retained MySQL 5.5 clock has second granularity: every observation deducts
 * a full 1000 ms uncertainty instead of extending an item until its rounded clock.
 */
final class ModernItemLifetimeJdbc {
    private static final int MAX_ROWS = ModernCharacterData.MAX_ITEMS + 1;
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 6, true).optionalEnd()
            .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter CANONICAL = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 6, 6, true)
            .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final List<String> ETC_TYPES = List.of("arrow", "wand", "light", "gem", "totem",
            "firecracker", "potion", "food", "scroll", "questitem", "spellbook", "petitem", "other",
            "material", "event", "sting", "treasure_box");

    private ModernItemLifetimeJdbc() {}

    static String sql(String catalog) throws SQLException {
        if (catalog == null || !catalog.matches("[A-Za-z0-9_]{1,64}")) throw unavailable();
        String prefix = "`" + catalog + "`.";
        return "SELECT c.objid AS owner_object,"
                + "LEFT(CAST(CURRENT_TIMESTAMP AS CHAR),27) AS database_now,"
                + "OCTET_LENGTH(CAST(CURRENT_TIMESTAMP AS CHAR)) AS database_now_bytes,"
                + "i.id AS item_object,i.item_id AS catalog_id,i.is_equipped,i.remaining_time,"
                + "t.itemr_obj_id AS timed_object,LEFT(CAST(t.usertime AS CHAR),27) AS item_deadline,"
                + "OCTET_LENGTH(CAST(t.usertime AS CHAR)) AS item_deadline_bytes,"
                + "r.itemid AS rule_catalog,r.h AS rule_hours,"
                + "w.item_id AS weapon_catalog,w.max_use_time AS weapon_maximum,"
                + "a.item_id AS armor_catalog,a.max_use_time AS armor_maximum,"
                + "e.item_id AS etc_catalog,LEFT(e.item_type,33) AS etc_type,OCTET_LENGTH(e.item_type) AS etc_type_bytes"
                + " FROM characters c LEFT JOIN character_items i ON i.char_id=c.objid"
                + " LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id"
                + " LEFT JOIN " + prefix + "`道具_物品時間限制` r ON r.itemid=i.item_id"
                + " LEFT JOIN " + prefix + "`weapon` w ON w.item_id=i.item_id"
                + " LEFT JOIN " + prefix + "`armor` a ON a.item_id=i.item_id"
                + " LEFT JOIN " + prefix + "`etcitem` e ON e.item_id=i.item_id"
                + " WHERE c.objid=? AND c.account_name=? AND c.Banned=0 ORDER BY i.id LIMIT " + MAX_ROWS;
    }

    static ModernItemLifetime.Observation read(Connection connection, ModernBackendScope scope,
            ModernCharacterData.Snapshot expected, String canonicalAccount, String catalog) throws Exception {
        return read(connection, scope, expected, canonicalAccount, catalog,
                System::currentTimeMillis, System::nanoTime);
    }

    static ModernItemLifetime.Observation read(Connection connection, ModernBackendScope scope,
            ModernCharacterData.Snapshot expected, String canonicalAccount, String catalog,
            LongSupplier epochClock, LongSupplier nanoClock) throws Exception {
        String query = sql(catalog);
        if (connection == null || scope == null || scope != ModernBackendScope.current() || expected == null
                || !ModernCharacterData.validText(canonicalAccount)
                || canonicalAccount.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 64
                || epochClock == null || nanoClock == null) throw unavailable();
        scope.checkActive();
        try {
            if (!connection.isReadOnly()) throw unavailable();
            ModernItemLifetime.Observation observation;
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setMaxRows(MAX_ROWS);
                statement.setQueryTimeout((int) Math.max(1L, ((long) scope.remainingMillis() + 999L) / 1000L));
                statement.setLong(1, expected.character().objectId()); statement.setString(2, canonicalAccount);
                scope.checkActive();
                long startEpoch = epochClock.getAsLong(), startNanos = nanoClock.getAsLong();
                if (startEpoch < 0) throw unavailable();
                try (ResultSet rows = statement.executeQuery()) {
                    observation = decode(rows, scope, expected, startEpoch, startNanos);
                }
            }
            scope.checkActive();
            return observation;
        } catch (SQLException | ArithmeticException | IllegalArgumentException failure) {
            throw unavailable();
        }
    }

    private static ModernItemLifetime.Observation decode(ResultSet rows, ModernBackendScope scope, ModernCharacterData.Snapshot expected,
            long startEpoch, long startNanos) throws Exception {
        LocalDateTime databaseNow = null;
        var items = new LinkedHashMap<Long, ModernItemLifetime.Fact>();
        boolean emptyInventory = false;
        int expectedIndex = 0;
        while (rows.next()) {
            scope.checkActive();
            if (ModernJdbcNumbers.required(rows, "owner_object") != expected.character().objectId()) throw unavailable();
            LocalDateTime rowNow = localDateTime(text(rows, "database_now", false, 26));
            if (databaseNow == null) databaseNow = rowNow; else if (!databaseNow.equals(rowNow)) throw unavailable();
            var optionalId = ModernJdbcNumbers.nullable(rows, "item_object");
            if (optionalId.isEmpty()) {
                if (emptyInventory || expectedIndex != 0 || !expected.inventory().isEmpty()) throw unavailable();
                for (String column : List.of("catalog_id", "is_equipped", "remaining_time", "timed_object",
                        "rule_catalog", "rule_hours", "weapon_catalog", "weapon_maximum", "armor_catalog", "armor_maximum", "etc_catalog")) {
                    if (ModernJdbcNumbers.nullable(rows, column).isPresent()) throw unavailable();
                }
                if (text(rows, "item_deadline", true, 26) != null || text(rows, "etc_type", true, 32) != null) throw unavailable();
                emptyInventory = true;
                continue;
            }
            if (emptyInventory || expectedIndex >= expected.inventory().size()) throw unavailable();
            var original = expected.inventory().get(expectedIndex++);
            long objectId = optionalId.getAsLong(), catalogId = ModernJdbcNumbers.required(rows, "catalog_id");
            long equipped = ModernJdbcNumbers.required(rows, "is_equipped");
            if (objectId != original.objectId() || catalogId != original.template().catalogId()
                    || (equipped != 0 && equipped != 1) || (equipped == 1) != original.equipped()) throw unavailable();
            String chosen = null;
            for (String candidate : List.of("weapon", "armor", "etc")) {
                var id = ModernJdbcNumbers.nullable(rows, candidate + "_catalog");
                if (id.isPresent()) {
                    if (chosen != null || id.getAsLong() != catalogId) throw unavailable();
                    chosen = candidate;
                }
            }
            if (chosen == null) throw unavailable();
            var kind = switch (chosen) { case "weapon" -> ModernCharacterData.ItemKind.WEAPON;
                case "armor" -> ModernCharacterData.ItemKind.ARMOR; default -> ModernCharacterData.ItemKind.ETC; };
            if (kind != original.template().kind()) throw unavailable();
            boolean lighting = false;
            long maximum;
            if (kind == ModernCharacterData.ItemKind.ETC) {
                // ItemTable's ETC loader never sets L1Item._maxUseTime: its constructor value is exactly 0.
                String type = text(rows, "etc_type", false, 32);
                if (!ETC_TYPES.contains(type)) throw unavailable();
                lighting = type.equals("light"); maximum = 0;
            } else maximum = ModernJdbcNumbers.required(rows, chosen + "_maximum");
            if (maximum < 0) throw unavailable();
            var remaining = ModernJdbcNumbers.nullable(rows, "remaining_time");
            if (remaining.isPresent() && remaining.getAsLong() < 0) throw unavailable();
            var timedObject = ModernJdbcNumbers.nullable(rows, "timed_object");
            String deadlineText = text(rows, "item_deadline", true, 26);
            if (timedObject.isPresent() != (deadlineText != null)
                    || timedObject.isPresent() && timedObject.getAsLong() != objectId) throw unavailable();
            String canonicalDeadline = null;
            if (deadlineText != null) {
                LocalDateTime deadline = localDateTime(deadlineText);
                canonicalDeadline = canonical(deadline);
            }
            var rule = ModernJdbcNumbers.nullable(rows, "rule_catalog");
            var hours = ModernJdbcNumbers.nullable(rows, "rule_hours");
            if (rule.isPresent() && rule.getAsLong() != catalogId || rule.isEmpty() && hours.isPresent()) throw unavailable();
            if (hours.isPresent() && hours.getAsLong() < 0) throw unavailable();
            items.put(objectId, new ModernItemLifetime.Fact(objectId, catalogId, kind, equipped == 1, remaining,
                    rule.isPresent(), hours, Optional.ofNullable(canonicalDeadline), maximum, lighting));
        }
        scope.checkActive();
        if (databaseNow == null || expectedIndex != expected.inventory().size()) throw unavailable();
        return new ModernItemLifetime.Observation(expected.sourceId(), expected.character().objectId(), canonical(databaseNow),
                startEpoch, startNanos, items, 1000);
    }

    static LocalDateTime localDateTime(String value) throws SQLException {
        try {
            if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,6})?")) throw unavailable();
            LocalDateTime result = LocalDateTime.parse(value, PARSER);
            if (result.getYear() < 1 || result.getYear() > 9999) throw unavailable();
            return result;
        } catch (java.time.DateTimeException failure) { throw unavailable(); }
    }
    static String canonical(LocalDateTime value) { return CANONICAL.format(value); }
    private static String text(ResultSet rows, String field, boolean nullable, int maximum) throws SQLException {
        int type = rows.getMetaData().getColumnType(rows.findColumn(field));
        if (type != Types.CHAR && type != Types.VARCHAR && type != Types.LONGVARCHAR) throw unavailable();
        String value = rows.getString(field); boolean wasNull = rows.wasNull();
        var bytes = ModernJdbcNumbers.nullable(rows, field + "_bytes");
        if ((value == null) != wasNull || (value == null) != bytes.isEmpty()) throw unavailable();
        if (value == null) { if (!nullable) throw unavailable(); return null; }
        if (value.length() < 1 || value.length() > maximum || bytes.getAsLong() != value.length()
                || value.chars().anyMatch(c -> c < 0x20 || c > 0x7e)) throw unavailable();
        return value;
    }
    private static SQLException unavailable() { return new SQLException("modern item lifetime unavailable"); }
}
