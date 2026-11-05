package local.lito381.server;

import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** #78：以 JDBC 邊界替身執行實際資料表載入，不連資料庫。 */
class ALandItemDefinitionsTest {
    record Event(String level, String message, Object[] args) {}

    @Test void optionalColumnsUseDefaultDebugAndPreserveArguments() throws Exception {
        for (String column : List.of("note", "chance")) {
            SQLException failure = new SQLException("optional " + column);
            List<Event> events = new ArrayList<>();
            var data = data(events);
            var sql = sql(column, failure);
            NativeDeItemDefinitionsScope.loading(data, sql, () -> {
                var table = construct(NativeDeItemGreaterTierTable.class);
                assertNotNull(table.getWeaponTier(7, 1));
                assertNotNull(table.getArmorTier(7, 1));
                return null;
            });
            var debug = events.stream().filter(e -> e.level.equals("debug")).toList();
            assertEquals(2, debug.size());
            for (var event : debug) {
                assertEquals("Optional item greater tier column read failed: {}", event.message);
                assertArrayEquals(new Object[]{column, failure}, event.args);
            }
            assertEquals(0, data.sourceErrors);
            assertFalse(NativeDeItemDefinitionsScope.present());
        }
    }

    @Test void successfulReadDoesNotLogErrorsAndRequiredFailureStillRejectsRow() throws Exception {
        for (boolean failRequired : List.of(false, true)) {
            List<Event> events = new ArrayList<>();
            var data = data(events);
            SQLException failure = new SQLException("required tier_id");
            NativeDeItemDefinitionsScope.loading(data, sql(failRequired ? "tier_id" : "", failure), () -> {
                var table = construct(NativeDeItemGreaterTierTable.class);
                if (failRequired) {
                    assertNull(table.getWeaponTier(7, 1));
                    assertNull(table.getArmorTier(7, 1));
                } else {
                    assertNotNull(table.getWeaponTier(7, 1));
                    assertEquals("note-value", table.getTierNote(7, 1, true));
                }
                return null;
            });
            assertEquals(failRequired ? 2 : 0, data.sourceErrors);
            assertEquals(0, events.stream().filter(e -> e.level.equals("debug")).count());
            if (failRequired) for (var event : events) if (event.level.equals("error")) {
                assertTrue(event.message.contains("tier_id: -1"));
                assertSame(failure, event.args[0]);
            }
        }
    }

    @Test void weaponReloadPreservesMissingFieldAndLoadingGuards() throws Exception {
        var data = data(new ArrayList<>());
        var sql = sql("", null);
        var table = NativeDeItemDefinitionsScope.loading(data, sql, () -> construct(NativeDeItemWeaponProcTable.class));
        assertThrows(IllegalStateException.class, table::reload);
        NativeDeItemDefinitionsScope.loading(data, sql, () -> {
            assertEquals("_instance", assertThrows(NoSuchFieldError.class, table::reload).getMessage());
            return null;
        });
        NativeDeItemDefinitionsScope.loading(data(new ArrayList<>()), sql, () -> {
            assertThrows(IllegalStateException.class, table::reload);
            return null;
        });
        NativeDeItemDefinitionsScope.loading(data, null, () -> {
            assertThrows(IllegalStateException.class, table::reload);
            return null;
        });
        data.loading = false;
        NativeDeItemDefinitionsScope.loading(data, sql, () -> {
            assertThrows(IllegalStateException.class, table::reload);
            return null;
        });
        assertFalse(NativeDeItemDefinitionsScope.present());
    }

    @Test void skillDerivedRetainsFormalMissingFieldFailure() {
        var zero = new NativeDeCharacterFields.CombatSkillDerived(0, 0, 0, 0, 0, false);
        assertEquals(zero, NativeDeCharacterFields.CombatSkillDerived.compute(List.of()));
        assertEquals(zero, NativeDeCharacterFields.CombatSkillDerived.compute(Arrays.asList(null, null)));
        for (var values : List.of(List.of(0), List.of(123), Arrays.asList(null, 1, null))) {
            assertEquals("SKU1", assertThrows(NoSuchFieldError.class,
                    () -> NativeDeCharacterFields.CombatSkillDerived.compute(values)).getMessage());
        }
    }

    private static NativeDeItemDefinitionsScope.Data data(List<Event> events) {
        // 實作 emit，故 debug 呼叫必須經過正式介面的 default 方法。
        return new NativeDeItemDefinitionsScope.Data((level, message, args) -> events.add(new Event(level, message, args)));
    }

    private static NativeDeItemDefinitionsSql sql(String failedColumn, SQLException failure) throws SQLException {
        Connection connection = proxy(Connection.class, (p, m, a) -> {
            if (m.getName().equals("prepareStatement")) {
                return proxy(PreparedStatement.class, (ps, pm, pa) -> {
                    if (pm.getName().equals("executeQuery")) {
                        int[] row = {0};
                        return proxy(ResultSet.class, (rs, rm, ra) -> switch (rm.getName()) {
                            case "next" -> ++row[0] == 1;
                            case "findColumn" -> 1;
                            case "getInt", "getString" -> {
                                String column = (String) ra[0];
                                if (column.equals(failedColumn)) throw failure;
                                if (rm.getName().equals("getString")) yield "note-value";
                                yield column.equals("tier_id") ? 7 : column.endsWith("_type") ? 1 : 0;
                            }
                            case "close" -> null;
                            default -> throw new AssertionError("unexpected ResultSet call: " + rm);
                        });
                    }
                    if (pm.getName().equals("close")) return null;
                    throw new AssertionError("unexpected statement call: " + pm);
                });
            }
            throw new AssertionError("unexpected connection call: " + m);
        });
        return new NativeDeItemDefinitionsSql(connection);
    }

    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    static <T> T construct(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
