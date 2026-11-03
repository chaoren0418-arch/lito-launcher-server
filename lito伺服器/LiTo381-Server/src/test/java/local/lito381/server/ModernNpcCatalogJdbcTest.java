package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernNpcCatalogJdbcTest {
    static EnumMap<ModernNpcCatalog.Field, Long> numbers() {
        var values = new EnumMap<ModernNpcCatalog.Field, Long>(ModernNpcCatalog.Field.class);
        for (var field : ModernNpcCatalog.Field.values()) values.put(field, 0L);
        values.put(ModernNpcCatalog.Field.GRAPHIC, 1649L); values.put(ModernNpcCatalog.Field.LEVEL, 26L);
        values.put(ModernNpcCatalog.Field.HP, 280L); values.put(ModernNpcCatalog.Field.MP, 10L);
        values.put(ModernNpcCatalog.Field.STR, 20L); values.put(ModernNpcCatalog.Field.DEX, 10L);
        values.put(ModernNpcCatalog.Field.INT, 12L); values.put(ModernNpcCatalog.Field.AC, -4L);
        values.put(ModernNpcCatalog.Field.WALK_MILLIS, 100L); values.put(ModernNpcCatalog.Field.ATTACK_MILLIS, 750L);
        values.put(ModernNpcCatalog.Field.RANGE, 1L); values.put(ModernNpcCatalog.Field.TRANSFORM_ID, -1L);
        return values;
    }
    static Map<String, Object> npc(int id) {
        var row = new HashMap<String, Object>(); row.put("npcid", (long) id); row.put("has_mobskills", 0L);
        for (var entry : numbers().entrySet()) row.put(entry.getKey().alias(), entry.getValue());
        text(row, "name", "龜龍"); text(row, "nameid", "$1498"); text(row, "impl", "L1Monster"); text(row, "size", "large");
        return row;
    }
    static void text(Map<String, Object> row, String key, String value) {
        row.put("t_" + key, value); row.put("b_" + key, value == null ? null : (long) value.getBytes(StandardCharsets.UTF_8).length);
    }
    static ModernNpcCatalog.Catalog read(Jdbc jdbc) throws Exception { return read(jdbc, 7, "catalog", List.of(45262)); }
    static ModernNpcCatalog.Catalog read(Jdbc jdbc, int source, String database, List<Integer> ids) throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(() -> ModernNpcCatalogJdbc.read(jdbc.connection(), ModernBackendScope.current(), source, database, ids));
    }
    static void rejected(Jdbc jdbc) {
        var failure = assertThrows(SQLException.class, () -> read(jdbc));
        assertEquals("modern NPC catalog unavailable", failure.getMessage()); assertNull(failure.getCause());
        assertNull(failure.getNextException()); assertEquals(0, failure.getSuppressed().length); jdbc.assertClosed();
    }

    @Test void threeStatementsAreBoundedReadonlySourceQualifiedAndMobskillPresenceIsNotAGuess() throws Exception {
        var jdbc = new Jdbc(); var catalog = read(jdbc); var npc = catalog.require(new ModernNpcCatalog.Key(7, "catalog", 45262));
        assertEquals(3, jdbc.statements.size()); assertEquals(280, npc.get(ModernNpcCatalog.Field.HP));
        assertTrue(npc.pendingCleanMelee().isEmpty()); assertTrue(npc.large()); assertEquals("龜龍", npc.name());
        assertEquals(Map.of(1, 45262L), jdbc.statements.get(0).parameters);
        assertTrue(jdbc.statements.get(0).sql.contains("EXISTS(SELECT 1 FROM `catalog`.`mobskill`"));
        for (var s : jdbc.statements) { assertTrue(s.sql.startsWith("SELECT ")); assertFalse(s.sql.contains(" FOR UPDATE")); }
        assertEquals(127, catalog.growth().strength().size()); jdbc.assertClosed();
    }

    @Test void wideExactValuesAndGrowthIndexSaturationPreserveTheOriginalStat() throws Exception {
        var jdbc = new Jdbc(); var row = jdbc.rows.get("npc").get(0);
        for (var f : List.of(ModernNpcCatalog.Field.HP, ModernNpcCatalog.Field.MP, ModernNpcCatalog.Field.STR, ModernNpcCatalog.Field.LEVEL))
            row.put(f.alias(), new BigDecimal(Long.MAX_VALUE));
        row.put(ModernNpcCatalog.Field.AC.alias(), new BigDecimal(Long.MIN_VALUE));
        var catalog = read(jdbc); var npc = catalog.npcs().get(45262);
        assertEquals(Long.MAX_VALUE, npc.get(ModernNpcCatalog.Field.HP)); assertEquals(Long.MAX_VALUE, npc.rosterTemplate().maximumMp());
        assertEquals(Long.MIN_VALUE, npc.get(ModernNpcCatalog.Field.AC));
        assertEquals(127, catalog.growth().at(false, npc.get(ModernNpcCatalog.Field.STR)).id());
        assertEquals(Long.MAX_VALUE, npc.get(ModernNpcCatalog.Field.STR)); jdbc.assertClosed();
    }

    @Test void everyNumericNpcFieldRejectsMissingNullApproximateFractionTextOrOutOfRangeWithoutFallback() {
        for (var field : ModernNpcCatalog.Field.values()) {
            for (Object invalid : new Object[]{null, 1.0, "1", new BigDecimal("1.1"), new BigDecimal("9223372036854775808")}) {
                var jdbc = new Jdbc(); jdbc.rows.get("npc").get(0).put(field.alias(), invalid); rejected(jdbc);
            }
            var missing = new Jdbc(); missing.rows.get("npc").get(0).remove(field.alias()); rejected(missing);
        }
    }

    @Test void nonBooleanFlagsUnknownWeaknessAndMalformedIdentifiersNeverCoerce() {
        for (var field : List.of(ModernNpcCatalog.Field.HARD, ModernNpcCatalog.Field.BOSS, ModernNpcCatalog.Field.TURN_UNDEAD,
                ModernNpcCatalog.Field.ERASE, ModernNpcCatalog.Field.CANNOT_RESURRECT, ModernNpcCatalog.Field.BRAVE,
                ModernNpcCatalog.Field.TELEPORT, ModernNpcCatalog.Field.DOPPEL)) {
            var jdbc = new Jdbc(); jdbc.rows.get("npc").get(0).put(field.alias(), 2L); rejected(jdbc);
        }
        for (String column : List.of("npcid", "has_mobskills")) for (Object value : new Object[]{null, 1.0, "1", -1L, Long.MAX_VALUE}) {
            var jdbc = new Jdbc(); jdbc.rows.get("npc").get(0).put(column, value); rejected(jdbc);
        }
        var weak = new Jdbc(); weak.rows.get("npc").get(0).put(ModernNpcCatalog.Field.WEAK_ATTRIBUTE.alias(), 3L); rejected(weak);
    }

    @Test void exactNpcSelectionRejectsMissingExtraDuplicateUnorderedAndDifferentRows() throws Exception {
        for (int mode = 0; mode < 5; mode++) {
            var jdbc = new Jdbc(); var rows = jdbc.rows.get("npc");
            switch (mode) {
                case 0 -> rows.clear(); case 1 -> rows.add(npc(45262)); case 2 -> rows.add(npc(45263));
                case 3 -> rows.get(0).put("npcid", 45261L);
                default -> { rows.add(0, npc(45263)); }
            }
            rejected(jdbc);
        }
        var jdbc = new Jdbc(); jdbc.rows.get("npc").add(npc(45263));
        var both = read(jdbc, 7, "catalog", List.of(45263, 45262)); assertEquals(2, both.npcs().size());
        assertEquals(Map.of(1, 45262L, 2, 45263L), jdbc.statements.get(0).parameters); jdbc.assertClosed();
    }

    @Test void maximum128TemplatesAndGrowthTablesRemainBounded() throws Exception {
        var jdbc = new Jdbc(); jdbc.rows.get("npc").clear(); var ids = new ArrayList<Integer>();
        for (int id = 1; id <= 128; id++) { ids.add(id); jdbc.rows.get("npc").add(npc(id)); }
        assertEquals(128, read(jdbc, 7, "catalog", ids).npcs().size()); assertEquals(128, jdbc.statements.get(0).parameters.size());
        assertEquals(129, jdbc.statements.get(0).maxRows); jdbc.assertClosed();
        var extra = new Jdbc(); extra.rows.get("npc").clear(); extra.rows.get("npc").addAll(jdbc.rows.get("npc")); extra.rows.get("npc").add(npc(129));
        assertThrows(SQLException.class, () -> read(extra, 7, "catalog", ids)); extra.assertClosed();
        for (String table : List.of("角色_力量成長", "角色_敏捷成長")) for (int mode = 0; mode < 4; mode++) {
            var bad = new Jdbc(); var rows = bad.rows.get(table);
            switch (mode) { case 0 -> rows.remove(126); case 1 -> rows.get(1).put("id", 1L); case 2 -> rows.get(0).put("damage", 1.0); default -> rows.add(new HashMap<>(rows.get(126))); }
            rejected(bad);
        }
    }

    @Test void malformedSelectionDatabaseOrWritableConnectionFailsBeforePreparingAnySql() {
        for (List<Integer> ids : List.of(List.<Integer>of(), List.of(0), List.of(-1), List.of(45262, 45262), java.util.Collections.nCopies(129, 45262))) {
            var jdbc = new Jdbc(); assertThrows(SQLException.class, () -> read(jdbc, 7, "catalog", ids)); assertTrue(jdbc.statements.isEmpty());
        }
        for (String database : List.of("", "catalog.other", "x`", "x; SELECT 1", "a".repeat(65))) {
            var jdbc = new Jdbc(); assertThrows(SQLException.class, () -> read(jdbc, 7, database, List.of(45262))); assertTrue(jdbc.statements.isEmpty());
        }
        var writable = new Jdbc(); writable.readOnly = false; rejected(writable); assertTrue(writable.statements.isEmpty());
        var zero = new Jdbc(); assertThrows(SQLException.class, () -> read(zero, 0, "catalog", List.of(45262))); assertTrue(zero.statements.isEmpty());
    }

    @Test void boundedUtf8TextIsTypedExactAndNeverSilentlyTruncated() {
        for (String key : List.of("name", "nameid", "impl", "size")) {
            for (String value : new String[]{null, "\u0000", "\ud800", "界".repeat(100)}) {
                var jdbc = new Jdbc(); text(jdbc.rows.get("npc").get(0), key, value); rejected(jdbc);
            }
            var mismatch = new Jdbc(); mismatch.rows.get("npc").get(0).put("b_" + key, 0L); rejected(mismatch);
            var wrongType = new Jdbc(); wrongType.rows.get("npc").get(0).put("t_" + key, 123L); wrongType.rows.get("npc").get(0).put("b_" + key, 3L); rejected(wrongType);
        }
        var size = new Jdbc(); text(size.rows.get("npc").get(0), "size", "LARGE"); rejected(size);
    }

    @Test void absencePresenceAndEveryUnimplementedBehaviorRemainVisible() throws Exception {
        var jdbc = new Jdbc(); var row = jdbc.rows.get("npc").get(0); row.put("has_mobskills", 1L);
        for (var field : List.of(ModernNpcCatalog.Field.POISON, ModernNpcCatalog.Field.PARALYSIS, ModernNpcCatalog.Field.BRAVE,
                ModernNpcCatalog.Field.TELEPORT, ModernNpcCatalog.Field.DOPPEL, ModernNpcCatalog.Field.HP_REGEN,
                ModernNpcCatalog.Field.RANDOM_HP, ModernNpcCatalog.Field.TRANSFORM_ID)) row.put(field.alias(), 1L);
        row.put(ModernNpcCatalog.Field.RANGE.alias(), 10L); text(row, "impl", "CustomMonster");
        row.put(ModernNpcCatalog.Field.GRAPHIC.alias(), 10000L); row.put(ModernNpcCatalog.Field.WALK_MILLIS.alias(), 0L);
        row.put(ModernNpcCatalog.Field.ATTACK_MILLIS.alias(), 0L);
        var npc = read(jdbc).npcs().get(45262); assertEquals(Set.of(ModernNpcCatalog.Pending.values()), npc.pendingCleanMelee());
        assertThrows(IllegalArgumentException.class, npc::rosterTemplate); jdbc.assertClosed();
        var none = new Jdbc(); text(none.rows.get("npc").get(0), "nameid", ""); assertEquals("", read(none).npcs().get(45262).nameId()); none.assertClosed();
    }

    @Test void sourceAndDatabaseCannotBeMixedOrReboundEvenWithSameNpcAndVitals() throws Exception {
        var first = read(new Jdbc()); var second = read(new Jdbc("other"), 7, "other", List.of(45262));
        var third = read(new Jdbc(), 42, "catalog", List.of(45262));
        var a = first.npcs().get(45262); var b = second.npcs().get(45262); var c = third.npcs().get(45262);
        assertNotEquals(a.rosterTemplate(), b.rosterTemplate()); assertNotEquals(a.rosterTemplate(), c.rosterTemplate());
        assertThrows(IllegalArgumentException.class, () -> first.require(b.key()));
        assertThrows(IllegalArgumentException.class, () -> first.require(c.key()));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcCatalog.Catalog(7, "catalog", Map.of(45262, b), first.growth()));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcCatalog.Catalog(7, "catalog", first.npcs(), second.growth()));
        try (var roster = ModernNpcRosterTest.roster()) {
            var actor = roster.spawn(ModernNpcRosterTest.SCOPE, List.of(new ModernNpcRoster.Spawn(a.rosterTemplate(), 0, 1, ModernNpcRosterTest.cell(10, 10), 0)), Set.of()).get(0);
            a.validateActor(actor); assertThrows(IllegalArgumentException.class, () -> b.validateActor(actor));
            assertThrows(IllegalArgumentException.class, () -> c.validateActor(actor));
        }
        assertFalse(a.toString().contains("catalog")); assertFalse(a.rosterTemplate().toString().contains("catalog"));
    }

    @Test void noNpcIdOnlyCacheCanHideChangedFactsBetweenReads() throws Exception {
        var jdbc = new Jdbc(); var first = read(jdbc); jdbc.rows.get("npc").get(0).put(ModernNpcCatalog.Field.HP.alias(), 90000L);
        var next = read(jdbc); assertEquals(280, first.npcs().get(45262).get(ModernNpcCatalog.Field.HP));
        assertEquals(90000, next.npcs().get(45262).get(ModernNpcCatalog.Field.HP)); assertEquals(6, jdbc.statements.size()); jdbc.assertClosed();
        assertThrows(UnsupportedOperationException.class, () -> first.npcs().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.npcs().get(45262).facts().values().clear());
    }

    @Test void anyQueryReadOrCloseFailureIsRedactedAndCannotPublishPartialCatalog() {
        for (String table : List.of("npc", "角色_力量成長", "角色_敏捷成長"))
            for (String stage : List.of("setMaxRows", "setQueryTimeout", "executeQuery", "next", "getBigDecimal", "result-close", "close")) {
                var jdbc = new Jdbc(); jdbc.failureTable = table; jdbc.failureStage = stage; rejected(jdbc);
            }
    }

    @Test void cancellationBeforeDuringAndAfterRowsWinsOverSqlErrors() {
        for (String table : List.of("npc", "角色_力量成長", "角色_敏捷成長")) for (int at : List.of(1, table.equals("npc") ? 2 : 128)) {
            var jdbc = new Jdbc(); jdbc.cancelTable = table; jdbc.cancelRow = at;
            var failure = assertThrows(ModernBackendExecutor.BackendException.class, () -> read(jdbc));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED, failure.failure()); jdbc.assertClosed();
        }
        var jdbc = new Jdbc();
        var failure = assertThrows(ModernBackendExecutor.BackendException.class, () -> ModernEquipmentSourceJdbcTest.scoped(() -> {
            var scope = ModernBackendScope.current(); scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
            return ModernNpcCatalogJdbc.read(jdbc.connection(), scope, 7, "catalog", List.of(45262));
        }));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED, failure.failure()); assertTrue(jdbc.statements.isEmpty());
    }

    static final class Jdbc {
        final Map<String, List<Map<String, Object>>> rows = new HashMap<>();
        final List<Statement> statements = new ArrayList<>(); final String database;
        boolean readOnly = true, connectionClosed; String failureTable, failureStage, cancelTable; int cancelRow;
        Jdbc() { this("catalog"); }
        Jdbc(String database) {
            this.database = database; rows.put("npc", new ArrayList<>(List.of(npc(45262))));
            for (String table : List.of("角色_力量成長", "角色_敏捷成長")) {
                var growth = new ArrayList<Map<String, Object>>();
                for (int i = 1; i <= 127; i++) growth.add(new HashMap<>(Map.of("id", (long) i, "hit", (long) i, "damage", (long) i)));
                rows.put(table, growth);
            }
        }
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (p, m, a) -> switch (m.getName()) {
                case "isReadOnly" -> readOnly; case "close" -> { connectionClosed = true; yield null; }
                case "prepareStatement" -> { var s = new Statement((String) a[0]); statements.add(s); yield s.proxy(); }
                default -> throw new AssertionError("unexpected connection operation " + m.getName());
            });
        }
        void assertClosed() {
            assertFalse(connectionClosed);
            for (var s : statements) {
                assertTrue(s.closed); if (s.resultOpened) assertTrue(s.resultClosed);
                assertEquals(s.table.equals("npc") ? 129 : 128, s.maxRows);
                if (!"setMaxRows".equals(failureStage) && !"setQueryTimeout".equals(failureStage)) assertTrue(s.timeout > 0 && s.timeout <= 30);
            }
        }
        final class Statement {
            final String sql, table; final Map<Integer, Object> parameters = new LinkedHashMap<>();
            int maxRows, timeout; boolean closed, resultOpened, resultClosed;
            Statement(String sql) {
                this.sql = sql;
                table = sql.contains("FROM `" + database + "`.`npc` n") ? "npc" : sql.contains("`角色_力量成長`") ? "角色_力量成長" : "角色_敏捷成長";
                assertTrue(sql.contains("`" + database + "`.")); assertTrue(sql.contains(" ORDER BY ")); assertTrue(sql.contains(" LIMIT "));
            }
            void fail(String stage) throws SQLException {
                if (table.equals(failureTable) && stage.equals(failureStage)) throw new SQLException("fixture-secret SQL credential", "42000", 1142);
            }
            PreparedStatement proxy() {
                return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (p, m, a) -> switch (m.getName()) {
                    case "setMaxRows" -> { maxRows = (Integer) a[0]; fail(m.getName()); yield null; }
                    case "setQueryTimeout" -> { timeout = (Integer) a[0]; fail(m.getName()); yield null; }
                    case "setLong" -> { parameters.put((Integer) a[0], a[1]); yield null; }
                    case "executeQuery" -> { fail(m.getName()); resultOpened = true; yield result(); }
                    case "close" -> { closed = true; fail(m.getName()); yield null; }
                    default -> throw new AssertionError("unexpected statement operation " + m.getName());
                });
            }
            ResultSet result() {
                int[] at = {-1}; boolean[] wasNull = {false}; var output = rows.get(table);
                ModernJdbcNumbersTest.Value values = column -> {
                    if (!output.get(at[0]).containsKey(column)) throw new SQLException("missing fixture column"); return output.get(at[0]).get(column);
                };
                var columns = new ModernJdbcNumbersTest.Columns(values);
                return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, (p, m, a) -> switch (m.getName()) {
                    case "next" -> { fail(m.getName()); at[0]++; if (table.equals(cancelTable) && at[0] + 1 == cancelRow) ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED); yield at[0] < output.size(); }
                    case "close" -> { resultClosed = true; fail("result-close"); yield null; }
                    case "wasNull" -> wasNull[0]; case "findColumn" -> columns.find((String) a[0]); case "getMetaData" -> columns.metadata();
                    case "getString", "getBigDecimal" -> {
                        fail(m.getName()); Object value = values.read(columns.name(a[0])); wasNull[0] = value == null;
                        yield m.getName().equals("getString") ? value == null ? null : value.toString() : ModernJdbcNumbersTest.Columns.decimal(value);
                    }
                    default -> throw new AssertionError("unexpected result operation " + m.getName());
                });
            }
        }
    }
}
