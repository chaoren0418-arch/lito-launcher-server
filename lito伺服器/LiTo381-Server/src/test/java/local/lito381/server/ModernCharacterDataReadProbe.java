package local.lito381.server;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TEST CLASSPATH ONLY, opt-in read-only inspection. No native bootstrap, migration,
 * sidecar import or effective combat-stat calculation. Credentials come only from
 * LITO_MODERN_PROBE_ACCOUNT/PASSWORD. Argument: absolute native server.properties.
 * Raw stored AC already includes unresolved selection-time equipment layers;
 * stored maximum vitals and current vitals must not be assumed to share that rule.
 */
public final class ModernCharacterDataReadProbe {
    static final int MAX_ITEMS = 512;
    static final int DEADLINE_MILLIS = 5000;
    private ModernCharacterDataReadProbe() {}

    enum Field {
        TYPE("Type"), SEX("Sex"), CLASS_SPRITE("Class"), LEVEL("level"),
        MAX_HP("MaxHp"), MAX_MP("MaxMp"), CURRENT_HP("CurHp"), CURRENT_MP("CurMp"),
        AC("Ac"), STR("Str"), CON("Con"), DEX("Dex"), CHA("Cha"), INT("Intel"), WIS("Wis"),
        ORIGINAL_STR("OriginalStr"), ORIGINAL_DEX("OriginalDex"), ORIGINAL_CON("OriginalCon"),
        ORIGINAL_INT("OriginalInt"), ORIGINAL_WIS("OriginalWis"), EXP("Exp"), LAWFUL("Lawful"), FOOD225("Food");
        final String column;
        Field(String column) { this.column = column; }
    }
    enum Stage { CONFIGURATION, ROUTING, CHARACTER_SCHEMA, ITEM_SCHEMA, WEAPON_SCHEMA, ARMOR_SCHEMA,
        ETC_SCHEMA, AUTHENTICATION, ROSTER, CHARACTER_VALUES, INVENTORY, VERIFY_ROSTER, COMPLETE }
    enum Failure { NONE, CONFIGURATION, AUTHENTICATION, JDBC, NULL_NUMBER, NUMBER_RANGE, STRING_BOUNDS,
        SCHEMA_COUNT, SCHEMA_TYPE, UNEXPECTED_ROW, MISSING_CHARACTER, DUPLICATE_CHARACTER,
        IDENTITY_CHANGED, ITEM_OVERFLOW, ITEM_ORDER, UNKNOWN_CATALOG, AMBIGUOUS_CATALOG,
        ROUTING_MISMATCH, DEADLINE, CANCELLED, CLOSED, OVERLOADED, BUSY, BACKEND, WORKER_NOT_RETIRED }
    enum Kind { WEAPON, ARMOR, ETC }
    static final class Audit {
        volatile Stage stage = Stage.CONFIGURATION;
        volatile Failure failure = Failure.NONE;
        volatile boolean metadata;
        volatile Report report;
    }
    /** Stored values, NOT bare attributes and NOT a live/effective battle snapshot. */
    record PersistedCharacterValues(Map<Field, Long> values) {
        PersistedCharacterValues { values = Map.copyOf(values); }
    }
    record Item(long objectId, long catalogId, long quantity, boolean equipped,
                long enchantLevel, long durability, boolean identified, long blessing,
                boolean sealed, long chargeCount, Kind kind, String catalogNameId,
                long inventoryGfx, long groundGfx, long weightUnits) {}
    record CharacterData(long objectId, PersistedCharacterValues persisted, List<Item> items) {
        CharacterData { items = List.copyOf(items); }
    }
    record Report(List<CharacterData> characters) {
        Report { characters = List.copyOf(characters); }
    }
    record Plan(String canonical, List<ModernApplicationProtocol.CharacterRow> roster) {
        Plan { roster = List.copyOf(roster); }
        @Override public String toString() { return "CharacterDataPlan[redacted]"; }
    }

    static final String FIELD_PROJECTION = Arrays.stream(Field.values())
            .map(field -> "c.`" + field.column + "` AS `" + field.column + "`")
            .collect(java.util.stream.Collectors.joining(","));
    static final String CHARACTER_SQL = "SELECT c.objid,LEFT(c.char_name,257) AS character_name,"
            + "OCTET_LENGTH(c.char_name) AS character_name_bytes," + FIELD_PROJECTION
            + " FROM characters c WHERE c.objid=? AND c.account_name=? AND c.Banned=0 LIMIT 2";
    static final String CHARACTER_SCHEMA_SQL = "SELECT objid,char_name,account_name,Banned,"
            + Arrays.stream(Field.values()).map(field -> "`" + field.column + "`")
                    .collect(java.util.stream.Collectors.joining(",")) + " FROM characters LIMIT 0";
    static final String ITEM_SCHEMA_SQL = "SELECT id,item_id,char_id,item_name,count,is_equipped,enchantlvl,"
            + "durability,is_id,bless,is_sealed,charge_count FROM character_items LIMIT 0";
    static final String DATABASE_SQL = "SELECT DATABASE()";

    static String catalogSchemaSql(String catalog, Kind kind) {
        return "SELECT item_id,name_id,invgfx,grdgfx,weight FROM " + table(catalog, kind) + " LIMIT 0";
    }
    static String inventorySql(String catalog) {
        return "SELECT i.id,i.item_id,i.char_id,i.count,i.is_equipped,i.enchantlvl,i.durability,i.is_id,i.bless,"
                + "i.is_sealed,i.charge_count,"
                + "w.item_id AS weapon_id,a.item_id AS armor_id,e.item_id AS etc_id,"
                + "LEFT(COALESCE(w.name_id,a.name_id,e.name_id),257) AS catalog_name,"
                + "OCTET_LENGTH(COALESCE(w.name_id,a.name_id,e.name_id)) AS catalog_name_bytes,"
                + "COALESCE(w.invgfx,a.invgfx,e.invgfx) AS inventory_gfx,"
                + "COALESCE(w.grdgfx,a.grdgfx,e.grdgfx) AS ground_gfx,"
                + "COALESCE(w.weight,a.weight,e.weight) AS weight_units FROM character_items i "
                + "JOIN characters c ON c.objid=i.char_id LEFT JOIN " + table(catalog, Kind.WEAPON) + " w ON w.item_id=i.item_id "
                + "LEFT JOIN " + table(catalog, Kind.ARMOR) + " a ON a.item_id=i.item_id "
                + "LEFT JOIN " + table(catalog, Kind.ETC) + " e ON e.item_id=i.item_id "
                + "WHERE i.char_id=? AND c.account_name=? AND c.Banned=0 ORDER BY i.id LIMIT 513";
    }
    private static String table(String catalog, Kind kind) {
        if (catalog == null || !catalog.matches("[A-Za-z0-9_]{1,64}")) throw new IllegalArgumentException("invalid explicit catalog binding");
        return "`" + catalog + "`." + switch (kind) { case WEAPON -> "weapon"; case ARMOR -> "armor"; case ETC -> "etcitem"; };
    }

    public static void main(String[] args) {
        PrintWriter output = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        ModernDatabaseReadProbe.Settings settings = null;
        ModernServerConfig config = null;
        ModernBackendExecutor executor = null;
        int exit = 2;
        boolean retired = true;
        try {
            if (args.length != 1) throw new IllegalArgumentException("explicit config required");
            Path serverFile = Path.of(args[0]);
            settings = ModernDatabaseReadProbe.load(serverFile);
            config = ModernDatabaseReadProbe.modernConfig(settings);
            String catalog = catalogDatabase(serverFile);
            String account = System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String password = System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if (!boundedText(account, 64, false) || !boundedText(password, 256, true)) throw new IllegalArgumentException("test credentials required");
            executor = new ModernBackendExecutor(1, 0);
            retired = false;
            boolean allPassed = true;
            for (var source : config.sources()) {
                Audit audit = new Audit();
                AtomicReference<Plan> plan = new AtomicReference<>();
                int networkMillis = config.dbNetworkMillis();
                ModernJdbcBackend.Connector connector = (url, properties) -> {
                    Connection connection = new com.mysql.cj.jdbc.Driver().connect(url, properties);
                    if (connection == null) throw bad(audit, Failure.JDBC);
                    return inspectOwned(connection, ModernBackendScope.current(), source.database().database(), catalog,
                            networkMillis, plan.get(), audit);
                };
                var backend = new ModernJdbcBackend(source.database(), config.dbConnectMillis(), networkMillis, connector);
                var verifier = ModernAccountAuthentication.repositories(Map.of(source.id(),
                        ModernAccountAuthentication.SourceBinding.direct(source.name(), backend::authenticate)), ignored -> false);
                try (var gate = executor.openSession()) {
                    char[] ownedPassword = password.toCharArray();
                    try {
                        gate.call(DEADLINE_MILLIS, new ModernBackendExecutor.Work<Void>() {
                            @Override public Void run(ModernBackendScope scope) throws Exception {
                                try {
                                    audit.stage = Stage.AUTHENTICATION;
                                    var principal = ModernAccountAuthentication.verify(verifier, account, ownedPassword);
                                    audit.stage = Stage.ROSTER;
                                    var roster = backend.characters(principal.accountForSource(source.id()));
                                    plan.set(new Plan(principal.accountForSource(source.id()), roster));
                                    var checked = backend.characters(principal.accountForSource(source.id()));
                                    audit.stage = Stage.VERIFY_ROSTER;
                                    if (!checked.equals(roster) || audit.report == null) throw bad(audit, Failure.IDENTITY_CHANGED);
                                    audit.stage = Stage.COMPLETE;
                                    return null;
                                } catch (ModernAccountAuthentication.AuthenticationException failed) {
                                    if (audit.failure == Failure.NONE) audit.failure = Failure.AUTHENTICATION;
                                    throw failed;
                                } finally { plan.set(null); }
                            }
                            @Override public void close() { Arrays.fill(ownedPassword, '\0'); }
                        });
                    } catch (ModernBackendExecutor.BackendException failed) {
                        if (audit.failure == Failure.NONE || failed.failure() != ModernBackendExecutor.Failure.BACKEND) {
                            audit.failure = Failure.valueOf(failed.failure().name());
                        }
                    }
                }
                if (!awaitQuiescent(executor, 3000)) {
                    audit.failure = Failure.WORKER_NOT_RETIRED;
                    output.println(renderFailure(source.id(), audit));
                    allPassed = false;
                    break;
                }
                output.println("{\"phase\":\"metadata\",\"source_id\":" + source.id() + ",\"status\":\""
                        + (audit.metadata ? "OK" : "FAILED") + "\"}");
                if (audit.failure == Failure.NONE && audit.stage == Stage.COMPLETE && audit.report != null) {
                    for (var character : audit.report.characters()) output.println(render(source.id(), character));
                } else {
                    output.println(renderFailure(source.id(), audit));
                    allPassed = false;
                }
            }
            exit = allPassed ? 0 : 1;
        } catch (Throwable failure) {
            // No original SQL exception, URL, credential, or arbitrary input is printed.
            output.println("{\"phase\":\"probe\",\"status\":\"FAILED\",\"reason\":\"CONFIGURATION_OR_STARTUP\"}");
        } finally {
            if (executor != null) {
                try {
                    executor.close();
                    retired = executor.awaitTermination(3000);
                } catch (Throwable failed) { retired = false; }
            }
            if (retired) {
                try {
                    try { if (config != null) config.close(); }
                    finally { if (settings != null) settings.close(); }
                } catch (Throwable failure) {
                    output.println("{\"phase\":\"cleanup\",\"status\":\"FAILED\",\"reason\":\"OWNER_CLOSE_FAILED\"}");
                    exit = 2;
                }
            } else {
                // Never erase a config still owned by a JDBC worker. This test
                // process fails and exits instead of claiming/detaching cleanup.
                output.println("{\"phase\":\"cleanup\",\"status\":\"FAILED\",\"reason\":\"WORKER_NOT_RETIRED\"}");
                exit = 2;
            }
        }
        output.flush();
        if (exit != 0) System.exit(exit);
    }

    static String catalogDatabase(Path mainFile) throws Exception {
        Properties properties = ModernDatabaseReadProbe.readProperties(ModernServerConfig.checkedLocalPath(mainFile, true));
        try {
            if (!"lito_mysql".equals(properties.getProperty("database.mode"))) throw new IllegalArgumentException("explicit lito mode required");
            String value = properties.getProperty("database.static.database");
            table(value, Kind.WEAPON); // Only this explicit binding; never account-schema fallback.
            return value;
        } finally { properties.clear(); }
    }

    /** Same Attempt/socket and lease-transfer contract as the existing read probes. */
    static Connection inspectOwned(Connection connection, ModernBackendScope scope, String expectedDatabase,
            String catalog, int networkMillis, Plan plan, Audit audit) throws SQLException {
        boolean transferred = false;
        Stage requestedStage = audit.stage;
        try {
            try (var lease = scope.register(connection)) {
                scope.checkActive();
                connection.setNetworkTimeout(Runnable::run, Math.min(networkMillis, scope.remainingMillis()));
                connection.setReadOnly(true);
                audit.stage = Stage.ROUTING;
                try (var statement = prepare(connection, scope, DATABASE_SQL, 2); var rows = statement.executeQuery()) {
                    if (!rows.next() || !expectedDatabase.equals(rows.getString(1)) || rows.next()) throw bad(audit, Failure.ROUTING_MISMATCH);
                }
                if (!audit.metadata) {
                    audit.stage = Stage.CHARACTER_SCHEMA;
                    schema(connection, scope, CHARACTER_SCHEMA_SQL, 4 + Field.values().length, List.of(2, 3), audit);
                    audit.stage = Stage.ITEM_SCHEMA;
                    schema(connection, scope, ITEM_SCHEMA_SQL, 12, List.of(4), audit);
                    for (Kind kind : Kind.values()) {
                        audit.stage = switch (kind) { case WEAPON -> Stage.WEAPON_SCHEMA; case ARMOR -> Stage.ARMOR_SCHEMA; case ETC -> Stage.ETC_SCHEMA; };
                        schema(connection, scope, catalogSchemaSql(catalog, kind), 5, List.of(2), audit);
                    }
                    audit.metadata = true;
                }
                if (plan != null) {
                    if (plan.roster().size() > ModernApplicationProtocol.MAX_ROWS) throw bad(audit, Failure.NUMBER_RANGE);
                    var data = new ArrayList<CharacterData>();
                    long previous = 0;
                    for (var row : plan.roster()) {
                        if (row.objectId() <= previous) throw bad(audit, Failure.IDENTITY_CHANGED);
                        audit.stage = Stage.CHARACTER_VALUES;
                        PersistedCharacterValues values = character(connection, scope, plan.canonical(), row, audit);
                        audit.stage = Stage.INVENTORY;
                        var items = inventory(connection, scope, catalog, plan.canonical(), row.objectId(), audit);
                        data.add(new CharacterData(row.objectId(), values, items));
                        previous = row.objectId();
                    }
                    audit.report = new Report(data);
                }
                scope.checkActive();
            }
            scope.checkActive();
            audit.stage = plan == null ? requestedStage : Stage.VERIFY_ROSTER;
            transferred = true;
            return connection;
        } catch (Exception failure) {
            if (audit.failure == Failure.NONE) audit.failure = Failure.JDBC;
            throw new SQLException("bounded character data inspection failed");
        } finally {
            if (!transferred) try { connection.close(); } catch (SQLException | RuntimeException ignored) {}
        }
    }

    private static PersistedCharacterValues character(Connection connection, ModernBackendScope scope,
            String canonical, ModernApplicationProtocol.CharacterRow expected, Audit audit) throws Exception {
        try (var query = prepare(connection, scope, CHARACTER_SQL, 2, expected.objectId(), canonical);
             var rows = query.executeQuery()) {
            if (!rows.next()) throw bad(audit, Failure.MISSING_CHARACTER);
            if (number(rows, "objid", audit) != expected.objectId()) throw bad(audit, Failure.IDENTITY_CHANGED);
            String name = text(rows, "character_name", "character_name_bytes", audit);
            EnumMap<Field, Long> values = new EnumMap<>(Field.class);
            for (Field field : Field.values()) values.put(field, number(rows, field.column, audit));
            validateValues(values, audit);
            if (!name.equals(expected.name()) || values.get(Field.TYPE) != expected.characterClass()
                    || values.get(Field.SEX) != expected.sex() || values.get(Field.LEVEL) != expected.level()) throw bad(audit, Failure.IDENTITY_CHANGED);
            if (rows.next()) throw bad(audit, Failure.DUPLICATE_CHARACTER);
            return new PersistedCharacterValues(values);
        }
    }
    static void validateValues(Map<Field, Long> values, Audit audit) throws SQLException {
        if (values.size() != Field.values().length || Arrays.stream(Field.values()).anyMatch(field -> values.get(field) == null)) throw bad(audit, Failure.NULL_NUMBER);
        for (Field field : Field.values()) {
            long value = values.get(field);
            if (field != Field.AC && field != Field.LAWFUL && value < 0) throw bad(audit, Failure.NUMBER_RANGE);
        }
        if (values.get(Field.LEVEL) < 1 || values.get(Field.MAX_HP) < 1 || values.get(Field.TYPE) > 65_535
                || values.get(Field.SEX) > 1 || values.get(Field.FOOD225) > 225) throw bad(audit, Failure.NUMBER_RANGE);
        // Current vitals may exceed stored base maxima due to persisted equipped
        // bonuses. Do not apply current<=base or native-width clamping here.
    }
    private static List<Item> inventory(Connection connection, ModernBackendScope scope, String catalog,
            String canonical, long characterId, Audit audit) throws Exception {
        List<Item> items = new ArrayList<>();
        try (var query = prepare(connection, scope, inventorySql(catalog), MAX_ITEMS + 1, characterId, canonical);
             var rows = query.executeQuery()) {
            long previous = 0;
            while (rows.next()) {
                scope.checkActive();
                if (items.size() == MAX_ITEMS) throw bad(audit, Failure.ITEM_OVERFLOW);
                long id = number(rows, "id", audit), itemId = number(rows, "item_id", audit);
                if (id <= previous) throw bad(audit, Failure.ITEM_ORDER);
                if (itemId < 1 || number(rows, "char_id", audit) != characterId) throw bad(audit, Failure.IDENTITY_CHANGED);
                int matches = 0;
                Kind kind = null;
                for (Kind candidate : Kind.values()) {
                    String column = switch (candidate) { case WEAPON -> "weapon_id"; case ARMOR -> "armor_id"; case ETC -> "etc_id"; };
                    long templateId = rows.getLong(column);
                    if (!rows.wasNull()) {
                        if (templateId != itemId) throw bad(audit, Failure.IDENTITY_CHANGED);
                        matches++; kind = candidate;
                    }
                }
                if (matches == 0) throw bad(audit, Failure.UNKNOWN_CATALOG);
                if (matches != 1) throw bad(audit, Failure.AMBIGUOUS_CATALOG);
                // character_items.item_name is an unused nullable legacy label;
                // the explicit catalog name_id is the authoritative token.
                String name = text(rows, "catalog_name", "catalog_name_bytes", audit);
                long quantity = positive(number(rows, "count", audit), audit);
                long durability = nonnegative(number(rows, "durability", audit), audit);
                long blessing = number(rows, "bless", audit);
                // Persist the unsigned raw code, not the native runtime's four
                // interpreted blessing categories. Unknown codes gain no effect.
                if (blessing < 0 || blessing > 255) throw bad(audit, Failure.NUMBER_RANGE);
                items.add(new Item(id, itemId, quantity, flag(rows, "is_equipped", audit), number(rows, "enchantlvl", audit),
                        durability, flag(rows, "is_id", audit), blessing, flag(rows, "is_sealed", audit),
                        nonnegative(number(rows, "charge_count", audit), audit), kind, name,
                        nonnegative(number(rows, "inventory_gfx", audit), audit), nonnegative(number(rows, "ground_gfx", audit), audit),
                        nonnegative(number(rows, "weight_units", audit), audit)));
                previous = id;
            }
        }
        return List.copyOf(items);
    }

    private static void schema(Connection connection, ModernBackendScope scope, String sql, int count,
            List<Integer> textColumns, Audit audit) throws Exception {
        try (var query = prepare(connection, scope, sql, 1); var rows = query.executeQuery()) {
            var metadata = rows.getMetaData();
            if (rows.next()) throw bad(audit, Failure.UNEXPECTED_ROW);
            if (metadata.getColumnCount() != count) throw bad(audit, Failure.SCHEMA_COUNT);
            for (int index = 1; index <= count; index++) {
                int type = metadata.getColumnType(index);
                boolean supported = textColumns.contains(index)
                        ? List.of(Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR).contains(type)
                        : List.of(Types.BIGINT, Types.INTEGER, Types.SMALLINT, Types.TINYINT, Types.BIT, Types.BOOLEAN).contains(type);
                if (!supported) throw bad(audit, Failure.SCHEMA_TYPE);
            }
            scope.checkActive();
        }
    }
    private static PreparedStatement prepare(Connection connection, ModernBackendScope scope, String sql,
            int maximumRows, Object... parameters) throws Exception {
        scope.checkActive();
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setMaxRows(maximumRows);
            statement.setQueryTimeout(Math.max(1, (scope.remainingMillis() + 999) / 1000));
            for (int index = 0; index < parameters.length; index++) {
                if (parameters[index] instanceof Long number) statement.setLong(index + 1, number);
                else statement.setString(index + 1, (String) parameters[index]);
            }
            scope.checkActive();
            return statement;
        } catch (Exception failure) {
            try { statement.close(); } catch (SQLException ignored) {}
            throw failure;
        }
    }
    private static long number(ResultSet rows, String column, Audit audit) throws SQLException {
        long value = rows.getLong(column);
        if (rows.wasNull()) throw bad(audit, Failure.NULL_NUMBER);
        return value;
    }
    private static long positive(long value, Audit audit) throws SQLException {
        if (value < 1) throw bad(audit, Failure.NUMBER_RANGE); return value;
    }
    private static long nonnegative(long value, Audit audit) throws SQLException {
        if (value < 0) throw bad(audit, Failure.NUMBER_RANGE); return value;
    }
    private static boolean flag(ResultSet rows, String column, Audit audit) throws SQLException {
        long value = number(rows, column, audit);
        if (value != 0 && value != 1) throw bad(audit, Failure.NUMBER_RANGE); return value == 1;
    }
    private static String text(ResultSet rows, String column, String lengthColumn, Audit audit) throws SQLException {
        String value = rows.getString(column);
        long bytes = number(rows, lengthColumn, audit);
        if (bytes <= 0 || bytes > 256
                || value == null || value.length() > 256 || value.chars().anyMatch(Character::isISOControl)
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value) || value.getBytes(StandardCharsets.UTF_8).length > 256
                || !hasNonSpace(value)) throw bad(audit, Failure.STRING_BOUNDS);
        return value;
    }
    static boolean boundedText(String value, int bytes, boolean password) {
        return value != null && !value.isEmpty() && value.length() <= bytes && (password || hasNonSpace(value))
                && value.chars().noneMatch(Character::isISOControl) && StandardCharsets.UTF_8.newEncoder().canEncode(value)
                && value.getBytes(StandardCharsets.UTF_8).length <= bytes;
    }
    private static boolean hasNonSpace(String value) {
        return value.codePoints().anyMatch(point -> !Character.isWhitespace(point) && !Character.isSpaceChar(point));
    }
    private static SQLException bad(Audit audit, Failure failure) {
        audit.failure = failure; return new SQLException("bounded character data probe failed");
    }
    private static boolean awaitQuiescent(ModernBackendExecutor executor, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (!executor.isQuiescent()) {
            if (System.nanoTime() - deadline >= 0) return false;
            Thread.sleep(5);
        }
        return true;
    }
    static String renderFailure(int source, Audit audit) {
        return "{\"phase\":\"character-data\",\"source_id\":" + source + ",\"status\":\"FAILED\",\"stage\":\""
                + audit.stage.name() + "\",\"reason\":\"" + audit.failure.name() + "\"}";
    }
    static String render(int source, CharacterData data) {
        StringBuilder out = new StringBuilder("{\"phase\":\"character-data\",\"source_id\":").append(source)
                .append(",\"object_id\":").append(data.objectId()).append(",\"values_kind\":\"persisted_not_effective\",\"values\":{");
        boolean first = true;
        for (Field field : Field.values()) {
            if (!first) out.append(','); first = false;
            out.append('"').append(field.name()).append("\":").append(data.persisted().values().get(field));
        }
        out.append("},\"inventory_scope\":\"character_items_only_no_private_sidecar\",\"item_count\":")
                .append(data.items().size()).append(",\"items\":[");
        first = true;
        for (Item item : data.items()) {
            if (!first) out.append(','); first = false;
            out.append("{\"object_id\":").append(item.objectId()).append(",\"catalog_id\":").append(item.catalogId())
                    .append(",\"count\":").append(item.quantity()).append(",\"equipped\":").append(item.equipped())
                    .append(",\"enchant\":").append(item.enchantLevel()).append(",\"durability\":").append(item.durability())
                    .append(",\"identified\":").append(item.identified()).append(",\"blessing\":").append(item.blessing())
                    .append(",\"sealed\":").append(item.sealed()).append(",\"charges\":").append(item.chargeCount())
                    .append(",\"kind\":\"").append(item.kind().name()).append("\",\"inventory_gfx\":").append(item.inventoryGfx())
                    .append(",\"ground_gfx\":").append(item.groundGfx()).append(",\"catalog_weight_units\":").append(item.weightUnits()).append('}');
        }
        return out.append("]}").toString();
    }
}
