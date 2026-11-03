package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernCharacterDataReadProbeTest {
    @TempDir Path temporary;
    private static final String SECRET = "synthetic-private-text";
    private static final String DATABASE = "synthetic_account";
    private static final String CATALOG = "synthetic_catalog";
    private static final String OWNER = "source-specific-owner";
    private static final long CHARACTER_ID = 3333;
    private static final ModernApplicationProtocol.CharacterRow CHARACTER =
            new ModernApplicationProtocol.CharacterRow(CHARACTER_ID, SECRET, 1, 0, 80);

    @Test void explicitCatalogBindingHasNoAccountSchemaOrLegacyFallback() throws Exception {
        Path main = temporary.resolve("server.properties");
        Files.writeString(main, "database.mode=lito_mysql\ndatabase.static.database=" + CATALOG + "\n", StandardCharsets.UTF_8);
        assertEquals(CATALOG, ModernCharacterDataReadProbe.catalogDatabase(main));
        for (String content : List.of("database.mode=lito_mysql\ndatabase.dynamic.databases=" + DATABASE + "\n",
                "database.mode=legacy_mysql\ndatabase.static.database=" + CATALOG + "\n",
                "database.mode=lito_mysql\ndatabase.static.database=unsafe;" + SECRET + "\n")) {
            Files.writeString(main, content, StandardCharsets.UTF_8);
            var failure = assertThrows(Exception.class, () -> ModernCharacterDataReadProbe.catalogDatabase(main));
            assertFalse(failure.toString().contains(SECRET));
        }
    }

    @Test void queriesAreBoundedSelectsWithOwnershipAndThreeIndependentLeftJoins() {
        String inventory = ModernCharacterDataReadProbe.inventorySql(CATALOG);
        assertTrue(inventory.contains("LIMIT 513"));
        assertTrue(inventory.contains("i.char_id=? AND c.account_name=? AND c.Banned=0"));
        assertFalse(inventory.contains("i.item_name"));
        assertTrue(inventory.contains("LEFT(COALESCE(w.name_id,a.name_id,e.name_id),257)"));
        assertTrue(inventory.contains("OCTET_LENGTH(COALESCE(w.name_id,a.name_id,e.name_id))"));
        for (String table : List.of("weapon", "armor", "etcitem")) assertTrue(inventory.contains("LEFT JOIN `" + CATALOG + "`." + table));
        assertTrue(inventory.contains("w.item_id AS weapon_id,a.item_id AS armor_id,e.item_id AS etc_id"));
        assertFalse(inventory.contains("SELECT *"));
        assertTrue(ModernCharacterDataReadProbe.CHARACTER_SQL.contains("c.objid=? AND c.account_name=? AND c.Banned=0 LIMIT 2"));
        assertTrue(ModernCharacterDataReadProbe.CHARACTER_SQL.contains("LEFT(c.char_name,257)"));
        assertThrows(IllegalArgumentException.class, () -> ModernCharacterDataReadProbe.inventorySql("bad;" + SECRET));
    }

    @Test void completeInspectionTransfersSameConnectionAndPreservesWideStoredValues() throws Exception {
        Jdbc jdbc = new Jdbc();
        var audit = new ModernCharacterDataReadProbe.Audit();
        scoped(() -> {
            Connection returned = inspect(jdbc, audit);
            assertSame(jdbc.connection, returned);
            assertFalse(jdbc.closed);
            assertTrue(jdbc.readOnly);
            assertTrue(audit.metadata);
            assertEquals(ModernCharacterDataReadProbe.Failure.NONE, audit.failure);
            var character = audit.report.characters().get(0);
            assertEquals(4_000_000_000L, character.persisted().values().get(ModernCharacterDataReadProbe.Field.MAX_HP));
            assertEquals(4_000_000_100L, character.persisted().values().get(ModernCharacterDataReadProbe.Field.CURRENT_HP));
            assertEquals(-4_000_000_000L, character.persisted().values().get(ModernCharacterDataReadProbe.Field.AC));
            assertEquals(4_000_000_000L, character.items().get(0).quantity());
            assertEquals(-2, character.items().get(0).enchantLevel());
            assertTrue(character.items().get(0).equipped());
            assertEquals(ModernCharacterDataReadProbe.Kind.WEAPON, character.items().get(0).kind());
            String output = ModernCharacterDataReadProbe.render(7, character);
            assertTrue(output.contains("persisted_not_effective"));
            assertTrue(output.contains("character_items_only_no_private_sidecar"));
            assertFalse(output.contains(SECRET));
            assertFalse(output.contains(OWNER));
            assertFalse(output.contains("jdbc:"));
            try (var replacement = ModernBackendScope.current().register(returned)) {
                assertEquals(8, jdbc.queries.size());
            }
            returned.close();
            return null;
        });
        assertTrue(jdbc.closed);
        assertEquals(jdbc.queries.size(), jdbc.closedStatements);
        assertEquals(jdbc.queries.size(), jdbc.closedResults);
        assertEquals(0, jdbc.aborts);
    }

    @Test void metadataOnlyNeverReadsAnAccountCharacterOrItemRow() throws Exception {
        Jdbc jdbc = new Jdbc();
        var audit = new ModernCharacterDataReadProbe.Audit();
        scoped(() -> {
            Connection returned = ModernCharacterDataReadProbe.inspectOwned(jdbc.connection, ModernBackendScope.current(), DATABASE,
                    CATALOG, 3000, null, audit);
            assertTrue(audit.metadata);
            assertNull(audit.report);
            assertEquals(6, jdbc.queries.size());
            assertFalse(jdbc.queries.contains(ModernCharacterDataReadProbe.CHARACTER_SQL));
            returned.close();
            return null;
        });
    }

    @Test void unknownAmbiguousAndDuplicateCatalogRowsFailWithoutDroppingInventory() throws Exception {
        Jdbc unknown = new Jdbc(); unknown.items.get(0).put("weapon_id", null);
        rejected(unknown, ModernCharacterDataReadProbe.Failure.UNKNOWN_CATALOG);
        Jdbc ambiguous = new Jdbc(); ambiguous.items.get(0).put("armor_id", 7L);
        rejected(ambiguous, ModernCharacterDataReadProbe.Failure.AMBIGUOUS_CATALOG);
        Jdbc duplicate = new Jdbc(); duplicate.items.add(new HashMap<>(duplicate.items.get(0)));
        rejected(duplicate, ModernCharacterDataReadProbe.Failure.ITEM_ORDER);
        Jdbc wrongTemplate = new Jdbc(); wrongTemplate.items.get(0).put("weapon_id", 8L);
        rejected(wrongTemplate, ModernCharacterDataReadProbe.Failure.IDENTITY_CHANGED);
    }

    @Test void fiveHundredTwelveItemsAreAcceptedButTheSentinelRowFails() throws Exception {
        Jdbc exact = new Jdbc(); exact.items.clear();
        for (int index = 1; index <= 512; index++) exact.items.add(item(index));
        var audit = new ModernCharacterDataReadProbe.Audit();
        scoped(() -> { inspect(exact, audit).close(); return null; });
        assertEquals(512, audit.report.characters().get(0).items().size());
        Jdbc overflow = new Jdbc(); overflow.items.clear();
        for (int index = 1; index <= 513; index++) overflow.items.add(item(index));
        rejected(overflow, ModernCharacterDataReadProbe.Failure.ITEM_OVERFLOW);
    }

    @Test void nullNumbersBadFlagsFoodAndTruncatedStringsAreNotClampedOrAccepted() throws Exception {
        Jdbc nullNumber = new Jdbc(); nullNumber.character.put("MaxHp", null);
        rejected(nullNumber, ModernCharacterDataReadProbe.Failure.NULL_NUMBER);
        Jdbc food = new Jdbc(); food.character.put("Food", 226L);
        rejected(food, ModernCharacterDataReadProbe.Failure.NUMBER_RANGE);
        Jdbc flag = new Jdbc(); flag.items.get(0).put("is_equipped", 2L);
        rejected(flag, ModernCharacterDataReadProbe.Failure.NUMBER_RANGE);
        Jdbc longName = new Jdbc(); longName.items.get(0).put("catalog_name_bytes", 100_000L);
        rejected(longName, ModernCharacterDataReadProbe.Failure.STRING_BOUNDS);
        Jdbc malformed = new Jdbc(); malformed.items.get(0).put("catalog_name", "bad\u0000name");
        rejected(malformed, ModernCharacterDataReadProbe.Failure.STRING_BOUNDS);
        Jdbc current = new Jdbc(); current.character.put("CurHp", -1L);
        rejected(current, ModernCharacterDataReadProbe.Failure.NUMBER_RANGE);
    }

    @Test void wrongDatabaseSchemaAndStaleCharacterIdentitiesCloseWithoutLeakingErrors() throws Exception {
        Jdbc wrongDatabase = new Jdbc(); wrongDatabase.selectedDatabase = SECRET;
        rejected(wrongDatabase, ModernCharacterDataReadProbe.Failure.ROUTING_MISMATCH);
        Jdbc wrongSchema = new Jdbc(); wrongSchema.invalidSchema = true;
        rejected(wrongSchema, ModernCharacterDataReadProbe.Failure.SCHEMA_TYPE);
        Jdbc stale = new Jdbc(); stale.character.put("objid", CHARACTER_ID + 1);
        rejected(stale, ModernCharacterDataReadProbe.Failure.IDENTITY_CHANGED);
        Jdbc wrongOwner = new Jdbc(); wrongOwner.items.get(0).put("char_id", CHARACTER_ID + 1);
        rejected(wrongOwner, ModernCharacterDataReadProbe.Failure.IDENTITY_CHANGED);
        Jdbc duplicateCharacter = new Jdbc(); duplicateCharacter.duplicateCharacter = true;
        rejected(duplicateCharacter, ModernCharacterDataReadProbe.Failure.DUPLICATE_CHARACTER);
        Jdbc missing = new Jdbc(); missing.missingCharacter = true;
        rejected(missing, ModernCharacterDataReadProbe.Failure.MISSING_CHARACTER);
        Jdbc sqlFailure = new Jdbc(); sqlFailure.queryFailure = true;
        rejected(sqlFailure, ModernCharacterDataReadProbe.Failure.JDBC);
    }

    @Test void rawBlessingCodesArePreservedWithoutInventingNativeEffects() throws Exception {
        for (long code : new long[] {0, 3, 4, 127, 128, 255}) {
            Jdbc fixture = new Jdbc(); fixture.items.get(0).put("bless", code);
            var audit = new ModernCharacterDataReadProbe.Audit();
            scoped(() -> { inspect(fixture, audit).close(); return null; });
            assertEquals(code, audit.report.characters().get(0).items().get(0).blessing());
            assertEquals(fixture.queries.size(), fixture.closedStatements);
            assertEquals(fixture.openedResults, fixture.closedResults);
        }
        for (long code : new long[] {-1, 256, Long.MAX_VALUE}) {
            Jdbc fixture = new Jdbc(); fixture.items.get(0).put("bless", code);
            rejected(fixture, ModernCharacterDataReadProbe.Failure.NUMBER_RANGE);
        }
    }

    @Test void nullableUnusedLegacyItemNameIsNotMaterializedOrValidated() throws Exception {
        Jdbc fixture = new Jdbc();
        fixture.items.get(0).put("item_name", null);
        fixture.items.get(0).put("item_name_bytes", null);
        var audit = new ModernCharacterDataReadProbe.Audit();
        scoped(() -> { inspect(fixture, audit).close(); return null; });
        assertEquals(1, audit.report.characters().get(0).items().size());
        assertEquals(SECRET, audit.report.characters().get(0).items().get(0).catalogNameId());
        assertFalse(fixture.queries.get(fixture.queries.size() - 1).contains("i.item_name"));
        assertEquals(fixture.queries.size(), fixture.closedStatements);
        assertEquals(fixture.openedResults, fixture.closedResults);
    }

    @Test void UnicodeBlankNamesAndAccountsAreRejectedButPasswordsAreNeverTrimmed() throws Exception {
        for (String blank : List.of(" ", "\u00a0", "\u2007", "\u202f", " \u00a0\u2007\u202f")) {
            assertFalse(ModernCharacterDataReadProbe.boundedText(blank, 64, false));
            assertTrue(ModernCharacterDataReadProbe.boundedText(blank, 256, true));
            assertTrue(ModernCharacterDataReadProbe.boundedText("x" + blank, 64, false));
            Jdbc fixture = new Jdbc();
            fixture.items.get(0).put("catalog_name", blank);
            fixture.items.get(0).put("catalog_name_bytes", (long) blank.getBytes(StandardCharsets.UTF_8).length);
            rejected(fixture, ModernCharacterDataReadProbe.Failure.STRING_BOUNDS);
        }
        assertFalse(ModernCharacterDataReadProbe.boundedText("\u0000", 256, true));
        assertFalse(ModernCharacterDataReadProbe.boundedText("", 256, true));
    }

    @Test void food225AndSignedAcUsePersistedDomainsNotNativeGaugeOrWidths() throws Exception {
        var audit = new ModernCharacterDataReadProbe.Audit();
        EnumMap<ModernCharacterDataReadProbe.Field, Long> values = new EnumMap<>(ModernCharacterDataReadProbe.Field.class);
        for (var field : ModernCharacterDataReadProbe.Field.values()) values.put(field, 0L);
        values.put(ModernCharacterDataReadProbe.Field.LEVEL, Long.MAX_VALUE);
        values.put(ModernCharacterDataReadProbe.Field.MAX_HP, Long.MAX_VALUE);
        values.put(ModernCharacterDataReadProbe.Field.CURRENT_HP, Long.MAX_VALUE);
        values.put(ModernCharacterDataReadProbe.Field.AC, Long.MIN_VALUE);
        values.put(ModernCharacterDataReadProbe.Field.LAWFUL, Long.MIN_VALUE);
        values.put(ModernCharacterDataReadProbe.Field.FOOD225, 225L);
        ModernCharacterDataReadProbe.validateValues(values, audit);
        assertEquals(225L, values.get(ModernCharacterDataReadProbe.Field.FOOD225));
        values.put(ModernCharacterDataReadProbe.Field.FOOD225, -1L);
        assertThrows(SQLException.class, () -> ModernCharacterDataReadProbe.validateValues(values, audit));
    }

    private static Connection inspect(Jdbc fixture, ModernCharacterDataReadProbe.Audit audit) throws SQLException {
        return ModernCharacterDataReadProbe.inspectOwned(fixture.connection, ModernBackendScope.current(), DATABASE,
                CATALOG, 3000, new ModernCharacterDataReadProbe.Plan(OWNER, List.of(CHARACTER)), audit);
    }
    private static void rejected(Jdbc fixture, ModernCharacterDataReadProbe.Failure expected) throws Exception {
        var audit = new ModernCharacterDataReadProbe.Audit();
        scoped(() -> {
            var error = assertThrows(SQLException.class, () -> inspect(fixture, audit));
            assertEquals(expected, audit.failure);
            assertTrue(fixture.closed);
            assertNull(audit.report);
            assertFalse(error.toString().contains(SECRET));
            assertNull(error.getCause());
            assertEquals(0, error.getSuppressed().length);
            assertFalse(ModernCharacterDataReadProbe.renderFailure(7, audit).contains(SECRET));
            ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
            assertEquals(0, fixture.aborts, "temporary lease must have retired before owned close");
            return null;
        });
        assertEquals(fixture.queries.size(), fixture.closedStatements);
        assertEquals(fixture.openedResults, fixture.closedResults);
    }
    private static <T> T scoped(Callable<T> action) throws Exception {
        var scope = new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(5), new ModernBackendScope.AbortScheduler() {
            public boolean execute(Runnable work) { work.run(); return true; }
            public void remove(Runnable work) {}
        });
        ModernBackendScope.bind(scope);
        try { return action.call(); }
        finally { scope.finish(); ModernBackendScope.unbind(); }
    }
    private static Map<String, Object> item(long objectId) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", objectId); row.put("item_id", 7L); row.put("char_id", CHARACTER_ID);
        row.put("count", 4_000_000_000L); row.put("is_equipped", 1L); row.put("enchantlvl", -2L);
        row.put("durability", 0L); row.put("is_id", 1L); row.put("bless", 1L); row.put("is_sealed", 0L);
        row.put("charge_count", 0L); row.put("item_name", SECRET); row.put("item_name_bytes", (long) SECRET.length());
        row.put("weapon_id", 7L); row.put("armor_id", null); row.put("etc_id", null);
        row.put("catalog_name", SECRET); row.put("catalog_name_bytes", (long) SECRET.length());
        row.put("inventory_gfx", 10L); row.put("ground_gfx", 20L); row.put("weight_units", 1000L);
        return row;
    }
    private static final class Jdbc {
        final Connection connection;
        final List<String> queries = new ArrayList<>();
        final List<Map<String, Object>> items = new ArrayList<>(List.of(item(9001)));
        final Map<String, Object> character = new HashMap<>();
        String selectedDatabase = DATABASE;
        boolean closed, readOnly, invalidSchema, queryFailure, missingCharacter, duplicateCharacter;
        int aborts, closedStatements, openedResults, closedResults;
        Jdbc() {
            character.put("objid", CHARACTER_ID); character.put("character_name", SECRET);
            character.put("character_name_bytes", (long) SECRET.length());
            for (var field : ModernCharacterDataReadProbe.Field.values()) character.put(field.column, 17L);
            character.put("Type", 1L); character.put("Sex", 0L); character.put("level", 80L);
            character.put("MaxHp", 4_000_000_000L); character.put("CurHp", 4_000_000_100L);
            character.put("MaxMp", 0L); character.put("CurMp", 100L);
            character.put("Ac", -4_000_000_000L); character.put("Food", 225L);
            connection = proxy(Connection.class, (name, args) -> switch (name) {
                case "setNetworkTimeout" -> { assertTrue((int) args[1] >= 1 && (int) args[1] <= 3000); yield null; }
                case "setReadOnly" -> { readOnly = (boolean) args[0]; yield null; }
                case "prepareStatement" -> statement((String) args[0]);
                case "close" -> { closed = true; yield null; }
                case "abort" -> { aborts++; yield null; }
                default -> throw new AssertionError("unexpected JDBC call");
            });
        }
        PreparedStatement statement(String sql) {
            assertTrue(sql.startsWith("SELECT "), "probe must never write SQL");
            queries.add(sql);
            Map<Integer, Object> parameters = new HashMap<>();
            return proxy(PreparedStatement.class, (method, args) -> switch (method) {
                case "setMaxRows" -> { assertTrue((int) args[0] >= 1 && (int) args[0] <= 513); yield null; }
                case "setQueryTimeout" -> { assertTrue((int) args[0] >= 1 && (int) args[0] <= 5); yield null; }
                case "setLong", "setString" -> { parameters.put((int) args[0], args[1]); yield null; }
                case "executeQuery" -> {
                    if (queryFailure) throw new SQLException(SECRET, new SQLException(SECRET));
                    if (sql.equals(ModernCharacterDataReadProbe.CHARACTER_SQL) || sql.equals(ModernCharacterDataReadProbe.inventorySql(CATALOG))) {
                        assertEquals(CHARACTER_ID, parameters.get(1)); assertEquals(OWNER, parameters.get(2));
                    }
                    yield result(sql);
                }
                case "close" -> { closedStatements++; yield null; }
                default -> throw new AssertionError("unexpected statement call");
            });
        }
        ResultSet result(String sql) {
            List<Map<String, Object>> rows;
            int[] types;
            if (sql.equals(ModernCharacterDataReadProbe.DATABASE_SQL)) { rows = List.of(Map.of("1", selectedDatabase)); types = new int[] {Types.VARCHAR}; }
            else if (sql.equals(ModernCharacterDataReadProbe.CHARACTER_SQL)) {
                rows = missingCharacter ? List.of() : duplicateCharacter ? List.of(character, character) : List.of(character); types = new int[0];
            } else if (sql.equals(ModernCharacterDataReadProbe.inventorySql(CATALOG))) { rows = items; types = new int[0]; }
            else if (sql.equals(ModernCharacterDataReadProbe.CHARACTER_SCHEMA_SQL)) {
                rows = List.of(); types = new int[4 + ModernCharacterDataReadProbe.Field.values().length];
                Arrays.fill(types, Types.BIGINT); types[1] = Types.VARCHAR; types[2] = Types.VARCHAR;
            } else if (sql.equals(ModernCharacterDataReadProbe.ITEM_SCHEMA_SQL)) {
                rows = List.of(); types = new int[12]; Arrays.fill(types, Types.BIGINT); types[3] = Types.VARCHAR;
            } else {
                assertTrue(List.of(ModernCharacterDataReadProbe.Kind.values()).stream()
                        .anyMatch(kind -> sql.equals(ModernCharacterDataReadProbe.catalogSchemaSql(CATALOG, kind))));
                rows = List.of(); types = new int[] {Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.BIGINT, Types.BIGINT};
            }
            openedResults++;
            AtomicInteger index = new AtomicInteger(-1);
            AtomicBoolean wasNull = new AtomicBoolean();
            return proxy(ResultSet.class, (method, args) -> switch (method) {
                case "next" -> index.incrementAndGet() < rows.size();
                case "getLong" -> { Object value = rows.get(index.get()).get(args[0].toString()); wasNull.set(value == null); yield value == null ? 0L : ((Number) value).longValue(); }
                case "getString" -> { Object value = rows.get(index.get()).get(args[0].toString()); wasNull.set(value == null); yield value == null ? null : value.toString(); }
                case "wasNull" -> wasNull.get();
                case "getMetaData" -> proxy(ResultSetMetaData.class, (name, values) -> switch (name) {
                    case "getColumnCount" -> types.length;
                    case "getColumnType" -> invalidSchema ? Types.BLOB : types[(int) values[0] - 1];
                    default -> throw new AssertionError("unexpected metadata call");
                });
                case "close" -> { closedResults++; yield null; }
                default -> throw new AssertionError("unexpected result call");
            });
        }
    }
    @FunctionalInterface private interface Invocation { Object invoke(String method, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, args) -> invocation.invoke(method.getName(), args)));
    }
}
