package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Synthetic JDBC contracts only: no real database, listener, or SQL mutation. */
class ModernCharacterDataJdbcTest {
    @TempDir Path temporary;
    private static final String[] VALUE_FIELDS = {"MaxHp","MaxMp","CurHp","CurMp","Ac","Str","Dex","Con",
            "Intel","Wis","Cha","Exp","Lawful","OriginalStr","OriginalDex","OriginalCon","OriginalInt","OriginalWis","Food"};
    private static final long[] VALUES = {100,50,4_000_000_000L,5_000_000_000L,Long.MIN_VALUE,301,302,303,304,305,306,
            Long.MAX_VALUE,-32768,401,402,403,404,405,225};
    private static final List<String> CLOSED = List.of("result-close", "statement-close", "connection-close");

    private ModernServerConfig config() throws Exception {
        String identity = temporary.resolve("unused-listener.p12").toAbsolutePath().toString().replace('\\', '/');
        String text = "schema=1\nbind.port=0\ntls.pkcs12=" + identity + "\ntls.password.env=TLS\nsources=7\n"
                + "source.7.name=Sun\nsource.7.catalog.database=fixture_catalog\n"
                + "source.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\nsource.7.mysql.database=fixture_characters\n"
                + "source.7.mysql.username.env=USER\nsource.7.mysql.password.env=PASSWORD\n"
                + "source.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
        return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8), name -> switch (name) {
            case "TLS" -> "synthetic-listener-secret";
            case "USER" -> "synthetic-readonly-user";
            case "PASSWORD" -> "synthetic-database-secret";
            default -> null;
        });
    }
    private static ModernJdbcBackend dao(ModernServerConfig config, Jdbc jdbc) throws Exception {
        return new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, jdbc);
    }
    @FunctionalInterface private interface Scoped<T> { T run(ModernBackendScope scope) throws Exception; }
    private static <T> T scoped(Scoped<T> work) throws Exception {
        var scope = new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(10),
                new ModernBackendScope.AbortScheduler() {
                    public boolean execute(Runnable action) { action.run(); return true; }
                    public void remove(Runnable action) {}
                });
        ModernBackendScope.bind(scope);
        try { return work.run(scope); }
        finally { scope.finish(); ModernBackendScope.unbind(); }
    }

    @Test void oneReadOnlyStatementBindsCanonicalAccountAndObjectAndRetainsEveryWideField() throws Exception {
        try (var config = config()) {
            var rows = List.of(item(4_000_000_011L, "w"), item(4_000_000_012L, "a"), item(Long.MAX_VALUE, "e"));
            rows.forEach(row -> row.put("objid", Long.MAX_VALUE));
            Jdbc jdbc = new Jdbc(rows);
            var backend = dao(config, jdbc);
            String canonical = "owner' OR '1'='1";
            var snapshot = scoped(scope -> backend.persistedCharacter(7, canonical, Long.MAX_VALUE, "fixture_catalog")).orElseThrow();
            assertEquals(7, snapshot.sourceId()); assertEquals(Long.MAX_VALUE, snapshot.character().objectId());
            assertEquals(4_000_000_000L, snapshot.character().level());
            assertEquals(100, snapshot.persisted().maxHp()); assertEquals(50, snapshot.persisted().maxMp());
            assertEquals(4_000_000_000L, snapshot.persisted().savedHp()); assertEquals(5_000_000_000L, snapshot.persisted().savedMp());
            assertEquals(Long.MIN_VALUE, snapshot.persisted().armorClass()); assertEquals(Long.MAX_VALUE, snapshot.persisted().experience());
            assertEquals(-32768, snapshot.persisted().lawful());
            assertEquals(301, snapshot.persisted().strength()); assertEquals(302, snapshot.persisted().dexterity());
            assertEquals(303, snapshot.persisted().constitution()); assertEquals(304, snapshot.persisted().intelligence());
            assertEquals(305, snapshot.persisted().wisdom()); assertEquals(306, snapshot.persisted().charisma());
            assertEquals(401, snapshot.persisted().originalStrength()); assertEquals(402, snapshot.persisted().originalDexterity());
            assertEquals(403, snapshot.persisted().originalConstitution()); assertEquals(404, snapshot.persisted().originalIntelligence());
            assertEquals(405, snapshot.persisted().originalWisdom()); assertEquals(225, snapshot.persisted().food225());
            assertEquals(100, snapshot.persisted().foodPercent());
            assertEquals(List.of(ModernCharacterData.ItemKind.WEAPON, ModernCharacterData.ItemKind.ARMOR, ModernCharacterData.ItemKind.ETC),
                    snapshot.inventory().stream().map(instance -> instance.template().kind()).toList());
            var first = snapshot.inventory().get(0);
            assertEquals(4_000_000_011L, first.objectId()); assertEquals(Long.MAX_VALUE, snapshot.inventory().get(2).objectId());
            assertEquals(Long.MAX_VALUE, first.quantity()); assertEquals(Long.MIN_VALUE, first.enchantment());
            assertEquals(Long.MAX_VALUE, first.durability()); assertEquals(Long.MAX_VALUE, first.chargeCount());
            assertEquals(255, first.blessing()); assertTrue(first.equipped()); assertFalse(first.identified()); assertTrue(first.sealed());
            assertEquals(Long.MAX_VALUE, first.template().inventoryGraphic());
            assertEquals(4_000_000_001L, first.template().groundGraphic()); assertEquals(Long.MAX_VALUE, first.template().unitWeight());
            assertEquals(1, jdbc.connections); assertEquals(1, jdbc.prepares); assertEquals(1, jdbc.executions);
            assertEquals(ModernCharacterDataJdbc.sql("fixture_catalog"), jdbc.sql);
            assertEquals(2, jdbc.sql.chars().filter(value -> value == '?').count());
            assertTrue(jdbc.sql.contains("WHERE c.objid=? AND c.account_name=? AND c.Banned=0"));
            for (String table : List.of("weapon", "armor", "etcitem")) assertTrue(jdbc.sql.contains("`fixture_catalog`.`" + table + "`"));
            assertTrue(jdbc.sql.endsWith("ORDER BY i.id LIMIT 513")); assertFalse(jdbc.sql.contains(canonical));
            assertEquals(Map.of(1, Long.MAX_VALUE, 2, canonical), jdbc.parameters); assertEquals(513, jdbc.maxRows);
            assertTrue(jdbc.querySeconds >= 1 && jdbc.querySeconds <= 10); assertTrue(jdbc.readOnly);
            assertTrue(jdbc.networkMillis > 0 && jdbc.networkMillis <= 3000);
            assertTrue(jdbc.receivedProperties.isEmpty()); assertEquals(CLOSED, jdbc.events);
            assertEquals(0, jdbc.aborts, "normal cleanup retires its lease rather than aborting a later owner");
        }
    }

    @Test void zeroInventoryAndMissingCharacterAreDistinctAndCloseTheirResources() throws Exception {
        try (var config = config()) {
            Jdbc empty = new Jdbc(List.of(character()));
            var found = scoped(scope -> dao(config, empty).persistedCharacter(7, "owner", 3333, "fixture_catalog"));
            assertTrue(found.isPresent()); assertTrue(found.orElseThrow().inventory().isEmpty()); assertEquals(CLOSED, empty.events);
            Jdbc missing = new Jdbc(List.of());
            assertTrue(scoped(scope -> dao(config, missing).persistedCharacter(7, "owner", 3333, "fixture_catalog")).isEmpty());
            assertEquals(CLOSED, missing.events);
        }
    }

    @Test void complete512ItemsIncludingRepeatedTemplateInstancesPassBut513FailsWithoutPartialSuccess() throws Exception {
        try (var config = config()) {
            var rows = new ArrayList<Map<String,Object>>();
            for (long id = 1; id <= 512; id++) rows.add(item(id, "w"));
            Jdbc full = new Jdbc(rows);
            var result = scoped(scope -> dao(config, full).persistedCharacter(7, "owner", 3333, "fixture_catalog")).orElseThrow();
            assertEquals(512, result.inventory().size()); assertEquals(512, result.inventory().get(511).objectId());
            assertEquals(1, full.executions); assertEquals(CLOSED, full.events);
            rows.add(item(513, "w"));
            assertRejected(config, rows);
        }
    }

    @Test void missingAmbiguousMismatchedAndRepeatedCatalogJoinRowsAreRejected() throws Exception {
        try (var config = config()) {
            var missing = item(1, "w"); missing.put("w_id", null);
            var ambiguous = item(1, "w"); ambiguous.put("a_id", ambiguous.get("catalog_id"));
            var mismatched = item(1, "w"); mismatched.put("w_id", 999L);
            for (var rows : List.of(List.of(missing), List.of(ambiguous), List.of(mismatched),
                    List.of(item(1, "w"), item(1, "w")), List.of(item(2, "w"), item(1, "w")),
                    List.of(character(), character()), List.of(character(), item(1, "w")), List.of(item(1, "w"), character()))) {
                assertRejected(config, rows);
            }
        }
    }

    @Test void everyRequiredNumericAndTextColumnRejectsSqlNullRatherThanDefaultingToZero() throws Exception {
        try (var config = config()) {
            var fields = new ArrayList<>(List.of(VALUE_FIELDS));
            fields.addAll(List.of("objid","character_type","character_sex","level","catalog_id","item_count","is_equipped",
                    "enchantlvl","durability","is_id","bless","is_sealed","charge_count","w_inventory_graphic",
                    "w_ground_graphic","w_weight","char_name","char_name_bytes","w_name","w_name_bytes"));
            for (String field : fields) {
                var invalid = item(1, "w"); invalid.put(field, null);
                assertRejected(config, List.of(invalid));
            }
        }
    }

    @Test void booleanFlagsAreStrictZeroOrOneAndNumericDomainsAreNotSilentlyCoerced() throws Exception {
        try (var config = config()) {
            for (String flag : List.of("is_equipped", "is_id", "is_sealed")) {
                for (long value : new long[] {-1,2,255,Long.MAX_VALUE}) {
                    var invalid = item(1, "w"); invalid.put(flag, value);
                    assertRejected(config, List.of(invalid));
                }
            }
            for (var invalidValue : Map.<String,Long>ofEntries(Map.entry("item_objid", 0L), Map.entry("catalog_id", 0L),
                    Map.entry("item_count", 0L), Map.entry("durability", -1L), Map.entry("bless", 256L),
                    Map.entry("charge_count", -1L), Map.entry("w_inventory_graphic", -1L), Map.entry("w_ground_graphic", -1L),
                    Map.entry("w_weight", -1L), Map.entry("Food", 226L), Map.entry("MaxHp", 0L), Map.entry("CurHp", -1L),
                    Map.entry("character_type", 65536L), Map.entry("character_sex", 2L), Map.entry("level", 0L)).entrySet()) {
                var invalid = item(1, "w"); invalid.put(invalidValue.getKey(), invalidValue.getValue());
                assertRejected(config, List.of(invalid));
            }
            var falseFlags = item(1, "w");
            falseFlags.put("is_equipped", 0L); falseFlags.put("is_id", 1L); falseFlags.put("is_sealed", 0L);
            var decoded = scoped(scope -> dao(config, new Jdbc(List.of(falseFlags))).persistedCharacter(7, "owner", 3333, "fixture_catalog"))
                    .orElseThrow().inventory().get(0);
            assertFalse(decoded.equipped()); assertTrue(decoded.identified()); assertFalse(decoded.sealed());
        }
    }

    @Test void textColumnsRejectMalformedUnicodeControlsBlankTokensAndOriginalOversizeLengths() throws Exception {
        try (var config = config()) {
            // A JDBC String mock can exercise malformed Unicode and byte bounds,
            // not prove how a real driver handles malformed source-charset bytes.
            for (String column : List.of("char_name", "w_name")) {
                for (String text : List.of("", "\u00a0", "\u2007", "\u202f", "\ud800", "\udc00", "x\0y", "x\u0085y", "界".repeat(86))) {
                    var invalid = item(1, "w"); invalid.put(column, text);
                    invalid.put(column + "_bytes", text.getBytes(StandardCharsets.UTF_8).length);
                    assertRejected(config, List.of(invalid));
                }
                for (long originalBytes : new long[] {0,257,Long.MAX_VALUE}) {
                    var invalid = item(1, "w"); invalid.put(column + "_bytes", originalBytes);
                    assertRejected(config, List.of(invalid));
                }
            }
            var valid = item(1, "w"); valid.put("w_name", "界".repeat(85)); valid.put("w_name_bytes", 255L);
            assertTrue(scoped(scope -> dao(config, new Jdbc(List.of(valid))).persistedCharacter(7, "owner", 3333, "fixture_catalog")).isPresent());
        }
    }

    @Test void exactDecimalReadsPreserveSignedValuesAndTinyintFlagsKeepTheirRawDomain() throws Exception {
        try (var config = config()) {
            var row=item(1,"w");
            row.replaceAll((column,value)->value instanceof Number number?new BigDecimal(number.toString()).setScale(3):value);
            row.put("item_count",BigInteger.valueOf(Long.MAX_VALUE));
            Jdbc jdbc=new Jdbc(List.of(row));jdbc.numericTypes.put("is_sealed",java.sql.Types.BIT);
            var value=scoped(scope->dao(config,jdbc).persistedCharacter(7,"owner",3333,"fixture_catalog")).orElseThrow();
            assertEquals(Long.MIN_VALUE,value.persisted().armorClass()); assertEquals(Long.MAX_VALUE,value.persisted().experience());
            assertEquals(Long.MAX_VALUE,value.inventory().get(0).quantity()); assertTrue(value.inventory().get(0).sealed());
            assertEquals(CLOSED,jdbc.events);
            for(int type:new int[]{java.sql.Types.BIT,java.sql.Types.BOOLEAN}) {
                var invalid=item(1,"w");invalid.put("is_sealed",new BigDecimal("2"));
                Jdbc raw=new Jdbc(List.of(invalid));raw.numericTypes.put("is_sealed",type);
                assertThrows(SQLException.class,()->scoped(scope->dao(config,raw).persistedCharacter(7,"owner",3333,"fixture_catalog")));
                assertEquals(CLOSED,raw.events);
            }
        }
    }

    @Test void fractionalOutOfRangeApproximateAndTextNumericColumnsRejectTheWholeSnapshot() throws Exception {
        try (var config = config()) {
            for(String column:List.of("Str","item_objid","w_id","char_name_bytes")) {
                for(Object value:List.of(new BigDecimal("1.25"),new BigDecimal("9223372036854775808"),
                        new BigInteger("18446744073709551615"),Double.valueOf(1),Float.valueOf(1),"1")) {
                    var invalid=item(1,"w");invalid.put(column,value);assertRejected(config,List.of(invalid));
                }
            }
        }
    }

    @Test void wrongObjectAndInconsistentCharacterColumnsAcrossItemRowsRejectTheWholeSnapshot() throws Exception {
        try (var config = config()) {
            var wrongObject = item(1, "w"); wrongObject.put("objid", 3334L);
            assertRejected(config, List.of(wrongObject));
            for (String column : VALUE_FIELDS) {
                var changed = item(2, "w");
                long old = ((Number) changed.get(column)).longValue();
                changed.put(column, old == Long.MAX_VALUE ? old - 1 : old == 225 ? 224L : old + 1);
                assertRejected(config, List.of(item(1, "w"), changed));
            }
            for (String column : List.of("char_name", "character_type", "character_sex", "level")) {
                var changed = item(2, "w");
                changed.put(column, column.equals("char_name") ? "different" : column.equals("character_sex") ? 1L : 2L);
                assertRejected(config, List.of(item(1, "w"), changed));
            }
        }
    }

    @Test void sourceBindingsPreserveDistinctCanonicalAccountsAndSameObjectNeverFallsBack() throws Exception {
        try (var config = config()) {
            Jdbc sun = new Jdbc(List.of(item(1, "w")));
            var moonRow = item(1, "a"); moonRow.put("char_name", "Moon"); moonRow.put("char_name_bytes", 4L); moonRow.put("Str", 9876543210L);
            Jdbc moon = new Jdbc(List.of(moonRow));
            var sunDao = dao(config, sun); var moonDao = dao(config, moon);
            var principal = new ModernAccountAuthentication.Principal("sun-owner", Set.of(
                    new ModernAccountAuthentication.Source(7, "Sun"), new ModernAccountAuthentication.Source(42, "Moon")),
                    Map.of(7, "sun-owner", 42, "moon-owner"));
            var configured = new HashMap<Integer,ModernCharacterDataRepository.Source>();
            configured.put(7, new ModernCharacterDataRepository.Source(sunDao, "sun_catalog"));
            configured.put(42, new ModernCharacterDataRepository.Source(moonDao, "moon_catalog"));
            var repository = ModernCharacterDataRepository.jdbc(configured); configured.clear();
            var first = scoped(scope -> repository.character(principal, 7, 3333)).orElseThrow();
            var second = scoped(scope -> repository.character(principal, 42, 3333)).orElseThrow();
            assertEquals(first.character().objectId(), second.character().objectId()); assertNotEquals(first.sourceId(), second.sourceId());
            assertEquals("sun-owner", sun.parameters.get(2)); assertEquals("moon-owner", moon.parameters.get(2));
            assertEquals(301, first.persisted().strength()); assertEquals(9876543210L, second.persisted().strength());
            assertTrue(sun.sql.contains("`sun_catalog`.`weapon`")); assertTrue(moon.sql.contains("`moon_catalog`.`weapon`"));
            assertThrows(SecurityException.class, () -> scoped(scope -> repository.character(principal, 99, 3333)));
            var missing = ModernCharacterDataRepository.jdbc(Map.of(7, new ModernCharacterDataRepository.Source(sunDao, "sun_catalog")));
            assertThrows(IllegalStateException.class, () -> scoped(scope -> missing.character(principal, 42, 3333)));
            assertThrows(IllegalStateException.class, () -> scoped(scope -> ModernCharacterDataRepository.unavailable().character(principal, 7, 3333)));
            assertEquals(1, sun.connections); assertEquals(1, moon.connections);
        }
    }

    @Test void badIdentityAndCatalogIdentifiersFailBeforeOpeningAnyConnection() throws Exception {
        try (var config = config()) {
            Jdbc jdbc = new Jdbc(List.of(character())); var backend = dao(config, jdbc);
            for (String identifier : new String[] {null,""," ","x.y","x` JOIN accounts --","x;DROP TABLE x","x-y","../x","資料庫","x".repeat(65)}) {
                assertThrows(IllegalArgumentException.class, () -> ModernCharacterDataJdbc.sql(identifier));
                assertThrows(IllegalArgumentException.class, () -> scoped(scope -> backend.persistedCharacter(7, "owner", 3333, identifier)));
                assertThrows(IllegalArgumentException.class, () -> new ModernCharacterDataRepository.Source(backend, identifier));
            }
            for (int source : new int[] {0,-1}) assertThrows(SQLException.class,
                    () -> scoped(scope -> backend.persistedCharacter(source, "owner", 3333, "catalog")));
            for (long object : new long[] {0,-1}) assertThrows(SQLException.class,
                    () -> scoped(scope -> backend.persistedCharacter(7, "owner", object, "catalog")));
            for (String owner : new String[] {null,"","\u00a0","\ud800","bad\nowner","x".repeat(65)}) assertThrows(SQLException.class,
                    () -> scoped(scope -> backend.persistedCharacter(7, owner, 3333, "catalog")));
            assertEquals(0, jdbc.connections); assertEquals(0, jdbc.prepares);
            assertThrows(IllegalStateException.class, () -> backend.persistedCharacter(7, "owner", 3333, "catalog"));
        }
    }

    @Test void cancellationBeforeOrDuringDecodingNeverPublishesPartialOrEmptySuccess() throws Exception {
        try (var config = config()) {
            Jdbc before = new Jdbc(List.of(character())); var beforeDao = dao(config, before);
            var prior = assertThrows(ModernBackendExecutor.BackendException.class, () -> scoped(scope -> {
                scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
                return beforeDao.persistedCharacter(7, "owner", 3333, "catalog");
            }));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED, prior.failure()); assertEquals(0, before.connections);
            for (var rows : List.<List<Map<String,Object>>>of(List.of(item(1, "w"), item(2, "w")), List.of())) {
                Jdbc jdbc = new Jdbc(rows); jdbc.cancelOnNext = rows.isEmpty() ? 1 : 2;
                var failure = assertThrows(ModernBackendExecutor.BackendException.class,
                        () -> scoped(scope -> dao(config, jdbc).persistedCharacter(7, "owner", 3333, "catalog")));
                assertEquals(ModernBackendExecutor.Failure.CANCELLED, failure.failure()); assertEquals(1, jdbc.aborts);
                assertEquals(CLOSED, jdbc.events); assertTrue(jdbc.receivedProperties.isEmpty());
            }
        }
    }

    @Test void backendExceptionsCannotExposeSqlAccountsOrDriverSecrets() throws Exception {
        try (var config = config()) {
            Jdbc jdbc = new Jdbc(List.of());
            jdbc.executeFailure = new SQLException("synthetic SQL owner secret", new Exception("synthetic database secret"));
            var failure = assertThrows(SQLException.class,
                    () -> scoped(scope -> dao(config, jdbc).persistedCharacter(7, "owner", 3333, "catalog")));
            assertEquals("modern JDBC backend unavailable", failure.getMessage()); assertNull(failure.getCause());
            assertEquals(0, failure.getSuppressed().length);
            assertEquals(List.of("statement-close", "connection-close"), jdbc.events);
            assertTrue(jdbc.receivedProperties.isEmpty());
        }
    }

    private static void assertRejected(ModernServerConfig config, List<Map<String,Object>> rows) throws Exception {
        Jdbc jdbc = new Jdbc(rows);
        assertThrows(SQLException.class, () -> scoped(scope -> dao(config, jdbc).persistedCharacter(7, "owner", 3333, "fixture_catalog")));
        assertEquals(1, jdbc.executions); assertEquals(CLOSED, jdbc.events);
    }
    private static Map<String,Object> character() {
        Map<String,Object> row = new HashMap<>();
        row.put("objid", 3333L); row.put("char_name", "Sun"); row.put("char_name_bytes", 3L);
        row.put("character_type", 1L); row.put("character_sex", 0L); row.put("level", 4_000_000_000L);
        for (int index = 0; index < VALUE_FIELDS.length; index++) row.put(VALUE_FIELDS[index], VALUES[index]);
        return row;
    }
    private static Map<String,Object> item(long id, String alias) {
        var row = character(); long catalog = switch (alias) { case "w" -> 100; case "a" -> 200; default -> 300; };
        row.put("item_objid", id); row.put("catalog_id", catalog); row.put("item_count", Long.MAX_VALUE);
        row.put("is_equipped", 1L); row.put("enchantlvl", Long.MIN_VALUE); row.put("durability", Long.MAX_VALUE);
        row.put("is_id", 0L); row.put("bless", 255L); row.put("is_sealed", 1L); row.put("charge_count", Long.MAX_VALUE);
        row.put(alias + "_id", catalog); row.put(alias + "_name", "$123"); row.put(alias + "_name_bytes", 4L);
        row.put(alias + "_inventory_graphic", Long.MAX_VALUE); row.put(alias + "_ground_graphic", 4_000_000_001L);
        row.put(alias + "_weight", Long.MAX_VALUE);
        return row;
    }

    /** Unknown/write JDBC calls fail the test; the fixture never opens a socket. */
    private static final class Jdbc implements ModernJdbcBackend.Connector {
        final List<Map<String,Object>> rows; final List<String> events = new ArrayList<>();
        final Map<Integer,Object> parameters = new HashMap<>();
        final Map<String,Integer> numericTypes = new HashMap<>();
        int connections, prepares, executions, maxRows, querySeconds, networkMillis, aborts, cancelOnNext;
        boolean readOnly; String sql; Properties receivedProperties; SQLException executeFailure;
        Jdbc(List<Map<String,Object>> rows) { this.rows = List.copyOf(rows); }
        @Override public Connection connect(String url, Properties properties) {
            connections++; receivedProperties = properties;
            return proxy(Connection.class, (method, arguments) -> switch (method) {
                case "setReadOnly" -> { readOnly = (boolean) arguments[0]; yield null; }
                case "setNetworkTimeout" -> { networkMillis = (int) arguments[1]; yield null; }
                case "prepareStatement" -> { prepares++; sql = (String) arguments[0]; yield statement(); }
                case "abort" -> { aborts++; yield null; }
                case "close" -> { events.add("connection-close"); yield null; }
                default -> throw new AssertionError("unexpected connection operation: " + method);
            });
        }
        private PreparedStatement statement() {
            return proxy(PreparedStatement.class, (method, arguments) -> switch (method) {
                case "setMaxRows" -> { maxRows = (int) arguments[0]; yield null; }
                case "setQueryTimeout" -> { querySeconds = (int) arguments[0]; yield null; }
                case "setLong", "setString" -> { parameters.put((int) arguments[0], arguments[1]); yield null; }
                case "executeQuery" -> { executions++; if (executeFailure != null) throw executeFailure; yield result(); }
                case "close" -> { events.add("statement-close"); yield null; }
                default -> throw new AssertionError("unexpected statement operation: " + method);
            });
        }
        private ResultSet result() {
            int[] index = {-1}; boolean[] wasNull = {false};
            var columns = new ModernJdbcNumbersTest.Columns(column -> rows.get(index[0]).get(column),numericTypes);
            return proxy(ResultSet.class, (method, arguments) -> switch (method) {
                case "next" -> {
                    index[0]++;
                    if (cancelOnNext == index[0] + 1) ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
                    yield index[0] < rows.size();
                }
                case "findColumn" -> columns.find((String)arguments[0]);
                case "getMetaData" -> columns.metadata();
                case "wasNull" -> wasNull[0];
                case "getString", "getLong", "getBigDecimal" -> {
                    Object value = rows.get(index[0]).get(columns.name(arguments[0])); wasNull[0] = value == null;
                    yield switch (method) {
                        case "getString" -> value;
                        case "getBigDecimal" -> ModernJdbcNumbersTest.Columns.decimal(value);
                        default -> value == null ? 0L : ((Number) value).longValue();
                    };
                }
                case "close" -> { events.add("result-close"); yield null; }
                default -> throw new AssertionError("unexpected result operation: " + method);
            });
        }
    }
    @FunctionalInterface private interface Invocation { Object run(String method, Object[] arguments) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation implementation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, arguments) -> implementation.run(method.getName(), arguments)));
    }
}
