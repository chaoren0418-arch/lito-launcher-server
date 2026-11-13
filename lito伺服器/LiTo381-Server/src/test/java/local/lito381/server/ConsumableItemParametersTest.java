package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * T111 (#352): potion/consumable numbers read from etcitem.classname, compared against
 * 1-伺服器. Classname literals are copied verbatim from {@code 1-伺服器/db/380_db.sql}
 * (line numbers beside each row); the repo's CI dump {@code db/lito_static.sql} has the
 * same strings. Mapping and verdicts: {@code docs/design/items-consumables.md}.
 */
final class ConsumableItemParametersTest {
    /** 1-伺服器/db/380_db.sql etcitem rows (classname column only). */
    static final Map<Long, String> SERVER1_CLASSNAMES = Map.ofEntries(
            Map.entry(40010L, "hp.UserAddHp 10 20 189 true 0 0"),          // :26733
            Map.entry(40011L, "hp.UserAddHp 30 50 194 true 0 0"),          // :26734
            Map.entry(40012L, "hp.UserAddHp 60 80 197 true"),              // :26735
            Map.entry(40013L, "brave.StatusHaste 300 191 true 40308 10"),  // :26736
            Map.entry(40014L, "brave.StatusBrave 300 751 true"),           // :26737
            Map.entry(40015L, "mp.UserMprBlue 600 190"),                   // :26738
            Map.entry(40016L, "brave.StatusWisdomPotion 300 750"),         // :26739
            Map.entry(40068L, "brave.StatusElfBrave 480 751 true"),        // :26791
            Map.entry(40506L, "hp.UserAddHp 30 50 197 true"),              // :27229
            Map.entry(40736L, "mp.UserMprBlue 600 190"),                   // :27459
            Map.entry(140015L, "mp.UserMprBlue 700 190"));                 // :28493

    /** T134 (#385): same dump, delay_id / delay_time columns for the 14 LiTo potions. */
    static final Map<Long, int[]> SERVER1_DELAYS = Map.ofEntries(
            Map.entry(40010L, new int[] {2, 500}),    // :26733
            Map.entry(40011L, new int[] {2, 1000}),   // :26734
            Map.entry(40012L, new int[] {2, 1000}),   // :26735
            Map.entry(40506L, new int[] {2, 1000}),   // :27229
            Map.entry(40013L, new int[] {0, 0}),      // :26736
            Map.entry(40014L, new int[] {0, 0}),      // :26737
            Map.entry(40015L, new int[] {0, 0}),      // :26738
            Map.entry(40016L, new int[] {0, 0}),      // :26739
            Map.entry(40017L, new int[] {0, 0}),      // :26740
            Map.entry(40025L, new int[] {0, 0}),      // :26748
            Map.entry(40032L, new int[] {0, 0}),      // :26755
            Map.entry(40068L, new int[] {0, 0}),      // :26791
            Map.entry(40736L, new int[] {0, 0}),      // :27459
            Map.entry(140015L, new int[] {0, 0}));    // :28493

    /** Full 1-伺服器 rows (classname + delay columns); 40017/40025/40032 keep their executor names. */
    static Map<Long, ConsumableItemParameters.Row> server1Rows() {
        return rows(SERVER1_CLASSNAMES);
    }

    static final Map<Long, String> SERVER1_FIXED_CLASSNAMES = Map.of(
            40017L, "Disintoxicat_Potion", 40025L, "Blindness_Potion", 40032L, "Water_Essence");

    /**
     * Rows for every delay item; a classname absent from {@code classnames} becomes a
     * {@code null} classname, which resolves exactly like a missing row for T111.
     */
    static Map<Long, ConsumableItemParameters.Row> rows(Map<Long, String> classnames) {
        Map<Long, ConsumableItemParameters.Row> rows = new HashMap<>();
        SERVER1_DELAYS.forEach((id, d) -> rows.put(id, new ConsumableItemParameters.Row(
                classnames.getOrDefault(id, SERVER1_FIXED_CLASSNAMES.get(id)), d[0], d[1])));
        return rows;
    }

    @AfterEach
    void restoreBuiltIn() {
        ConsumableItemParameters.resetForTesting();
    }

    @Test
    void builtInValuesEqualServer1Classnames() {
        assertEquals(SERVER1_CLASSNAMES.keySet(), ConsumableItemParameters.BUILT_IN.keySet());
        List<String> log = new ArrayList<>();
        var snapshot = ConsumableItemParameters.resolve(server1Rows(), "fixture", log::add);
        assertEquals(List.of(), snapshot.fallbacks());
        assertEquals(ConsumableItemParameters.BUILT_IN, snapshot.values());
        assertTrue(log.get(log.size() - 1).contains("differing-from-built-in=0"), log.toString());
        // T134: built-in cooldowns equal the 1-伺服器 delay columns for all 14 potions.
        assertEquals(SERVER1_DELAYS.keySet(), ConsumableItemParameters.BUILT_IN_DELAYS.keySet());
        SERVER1_DELAYS.forEach((id, d) -> assertEquals(new ConsumableItemParameters.Delay(d[0], d[1]),
                ConsumableItemParameters.BUILT_IN_DELAYS.get(id), "item " + id));
        assertEquals(List.of(), snapshot.delayFallbacks());
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS, snapshot.delays());
        assertTrue(log.get(log.size() - 1).contains("cooldown-differing=0"), log.toString());
    }

    @Test
    void healingPotionsShareCooldownGroupTwoOthersHaveNone() {
        for (long id : new long[] {40010, 40011, 40012, 40506}) {
            var delay = ConsumableItemParameters.delay(id);
            assertTrue(delay.active(), "item " + id);
            assertEquals(2, delay.id());
        }
        assertEquals(500, ConsumableItemParameters.delay(40010).millis());
        assertEquals(1000, ConsumableItemParameters.delay(40011).millis());
        for (long id : new long[] {40013, 40014, 40015, 40016, 40017, 40025, 40032, 40068, 40736, 140015}) {
            assertFalse(ConsumableItemParameters.delay(id).active(), "item " + id);
        }
        // The LiTo-only compatibility potion has no 1-伺服器 row and no cooldown.
        assertNull(ConsumableItemParameters.delay(InventoryCatalog.HEALING_POTION));
    }

    @Test
    void groupCooldownIsSharedAcrossHealingPotions() {
        ItemUseDelays delays = new ItemUseDelays();
        long t0 = 1_000_000_000L;
        delays.start(ConsumableItemParameters.delay(40010), t0);              // 500 ms, group 2
        // A different healing potion in the same group is refused.
        assertTrue(delays.blocked(ConsumableItemParameters.delay(40011).id(), t0 + ms(1)));
        assertTrue(delays.blocked(ConsumableItemParameters.delay(40506).id(), t0 + ms(250)));
        // Haste/brave/blue (no group) are never refused by group 2.
        assertFalse(delays.blocked(ConsumableItemParameters.delay(40013).id(), t0 + ms(1)));
        assertFalse(delays.blocked(0, t0));
        // An unrelated group is independent.
        assertFalse(delays.blocked(3, t0 + ms(1)));
        // Starting a no-cooldown item installs nothing.
        delays.start(ConsumableItemParameters.delay(40014), t0);
        assertEquals(1, delays.activeGroups());
    }

    @Test
    void cooldownBoundaryIsExclusiveOfDelayTime() {
        ItemUseDelays delays = new ItemUseDelays();
        long t0 = 5_000_000_000L;
        delays.start(new ConsumableItemParameters.Delay(2, 500), t0);
        assertTrue(delays.blocked(2, t0));
        assertTrue(delays.blocked(2, t0 + ms(500) - 1));
        // L1ItemDelay: expired when deadline - now <= 0, i.e. exactly delay_time later.
        assertFalse(delays.blocked(2, t0 + ms(500)));
        assertEquals(0, delays.activeGroups(), "expired lease is dropped like hasItemDelay");

        // 1000 ms potion: 999 ms refused, 1000 ms allowed.
        delays.start(new ConsumableItemParameters.Delay(2, 1000), t0);
        assertTrue(delays.blocked(2, t0 + ms(999)));
        assertFalse(delays.blocked(2, t0 + ms(1000)));
    }

    @Test
    void sameGroupLeaseKeepsTheLaterDeadline() {
        ItemUseDelays delays = new ItemUseDelays();
        long t0 = 0L;
        delays.start(new ConsumableItemParameters.Delay(2, 1000), t0);
        // A shorter lease started later must not shorten the running one (ItemDelayTimer.merge).
        delays.start(new ConsumableItemParameters.Delay(2, 500), t0 + ms(100));
        assertTrue(delays.blocked(2, t0 + ms(999)));
        assertFalse(delays.blocked(2, t0 + ms(1000)));
        // Zero / negative time is ignored (L1ItemDelay.java:87-90, :99).
        delays.start(new ConsumableItemParameters.Delay(2, 0), t0);
        delays.start(new ConsumableItemParameters.Delay(2, -5), t0);
        delays.start(null, t0);
        assertEquals(0, delays.activeGroups());
    }

    @Test
    void sqlCooldownsReplaceBuiltInAndBadRowsFallBack() {
        Map<Long, ConsumableItemParameters.Row> rows = server1Rows();
        rows.put(40010L, new ConsumableItemParameters.Row(SERVER1_CLASSNAMES.get(40010L), 2, 800));
        rows.put(40013L, new ConsumableItemParameters.Row(SERVER1_CLASSNAMES.get(40013L), 5, 1500));
        rows.remove(40025L);                                                     // missing row
        rows.put(40011L, ConsumableItemParameters.Row.classnameOnly(SERVER1_CLASSNAMES.get(40011L)));
        List<String> log = new ArrayList<>();
        var snapshot = ConsumableItemParameters.resolve(rows, "fixture", log::add);
        ConsumableItemParameters.install(snapshot);
        assertEquals(new ConsumableItemParameters.Delay(2, 800), ConsumableItemParameters.delay(40010));
        assertEquals(new ConsumableItemParameters.Delay(5, 1500), ConsumableItemParameters.delay(40013));
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS.get(40011L), ConsumableItemParameters.delay(40011));
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS.get(40025L), ConsumableItemParameters.delay(40025));
        assertEquals(2, snapshot.delayFallbacks().size(), snapshot.delayFallbacks().toString());
        // Classname parameters are unaffected by the cooldown columns.
        assertEquals(List.of(), snapshot.fallbacks());
        assertTrue(log.stream().anyMatch(l -> l.contains("item 40011") && l.contains("delay columns unavailable")));
        assertTrue(log.stream().anyMatch(l -> l.contains("item 40025") && l.contains("row missing")));
        assertTrue(log.get(log.size() - 1).contains("cooldown-differing=2"), log.toString());
    }

    @Test
    void missingDelayColumnsRetryClassnameOnlyAndKeepBuiltInCooldowns() throws Exception {
        List<String> queries = new ArrayList<>();
        Connection connection = fakeConnection(queries, true);
        List<String> log = new ArrayList<>();
        var rows = ConsumableItemParameters.read(connection, log::add);
        assertEquals(2, queries.size(), queries.toString());
        assertTrue(queries.get(0).contains("delay_id,delay_time"), queries.get(0));
        assertFalse(queries.get(1).contains("delay_id"), queries.get(1));
        assertTrue(log.stream().anyMatch(l -> l.contains("delay_id/delay_time unavailable")), log.toString());
        var snapshot = ConsumableItemParameters.resolve(rows, "fixture", line -> { });
        assertEquals(ConsumableItemParameters.BUILT_IN, snapshot.values());
        assertEquals(List.of(), snapshot.fallbacks());
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS, snapshot.delays());
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS.size(), snapshot.delayFallbacks().size());

        // With the columns present the single query returns both parts.
        queries.clear();
        var full = ConsumableItemParameters.read(fakeConnection(queries, false), line -> { });
        assertEquals(1, queries.size());
        var fullSnapshot = ConsumableItemParameters.resolve(full, "fixture", line -> { });
        assertEquals(List.of(), fullSnapshot.delayFallbacks());
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS, fullSnapshot.delays());
    }

    private static long ms(long millis) {
        return TimeUnit.MILLISECONDS.toNanos(millis);
    }

    /** JDBC stand-in serving {@link #server1Rows()}; optionally rejects the delay columns. */
    private static Connection fakeConnection(List<String> queries, boolean rejectDelayColumns) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "createStatement" -> fakeStatement(queries, rejectDelayColumns);
                    case "close" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Statement fakeStatement(List<String> queries, boolean rejectDelayColumns) {
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                new Class<?>[] {Statement.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "executeQuery": {
                            String sql = (String) args[0];
                            queries.add(sql);
                            boolean delays = sql.contains("delay_id");
                            if (delays && rejectDelayColumns) {
                                throw new SQLException("Unknown column 'delay_id' in 'field list'", "42S22", 1054);
                            }
                            return fakeResultSet(new ArrayList<>(server1Rows().entrySet()), delays);
                        }
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static ResultSet fakeResultSet(List<Map.Entry<Long, ConsumableItemParameters.Row>> rows,
            boolean delays) {
        int[] cursor = {-1};
        boolean[] lastNull = {false};
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next":
                            return ++cursor[0] < rows.size();
                        case "close":
                            return null;
                        case "wasNull":
                            return lastNull[0];
                        case "getLong":
                            lastNull[0] = false;
                            return rows.get(cursor[0]).getKey();
                        case "getString":
                            lastNull[0] = false;
                            return rows.get(cursor[0]).getValue().classname();
                        case "getInt": {
                            if (!delays) throw new SQLException("column out of range");
                            var row = rows.get(cursor[0]).getValue();
                            Integer value = (int) args[0] == 3 ? row.delayId() : row.delayTime();
                            lastNull[0] = value == null;
                            return value == null ? 0 : value;
                        }
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    @Test
    void inventoryCatalogKeepsBuiltInNumbersWithoutDatabase() {
        assertArrayEquals(new int[] {25, 25}, InventoryCatalog.healingRange(InventoryCatalog.HEALING_POTION));
        assertEquals(0, InventoryCatalog.healingEffectGfx(InventoryCatalog.HEALING_POTION));
        assertArrayEquals(new int[] {10, 20}, InventoryCatalog.healingRange(40010));
        assertArrayEquals(new int[] {60, 80}, InventoryCatalog.healingRange(40012));
        assertEquals(194, InventoryCatalog.healingEffectGfx(40011));
        assertEquals(300, InventoryCatalog.selfHasteDurationSeconds());
        assertEquals(191, InventoryCatalog.selfHasteEffectGfx());
        assertEquals(300, InventoryCatalog.braveDurationSeconds());
        assertEquals(751, InventoryCatalog.braveEffectGfx());
        assertEquals(300, InventoryCatalog.wisdomPotionDurationSeconds());
        assertEquals(750, InventoryCatalog.wisdomPotionEffectGfx());
        assertEquals(480, InventoryCatalog.elvenWaferDurationSeconds());
        assertEquals(751, InventoryCatalog.elvenWaferEffectGfx());
        assertEquals(600, InventoryCatalog.bluePotionDurationSeconds(40015));
        assertEquals(700, InventoryCatalog.bluePotionDurationSeconds(140015));
        assertEquals(600, InventoryCatalog.bluePotionDurationSeconds(40736));
        assertEquals(190, InventoryCatalog.bluePotionEffectGfx(140015));
        assertThrows(IllegalArgumentException.class, () -> InventoryCatalog.healingRange(40015));
        assertThrows(IllegalArgumentException.class, () -> InventoryCatalog.bluePotionDurationSeconds(40010));
    }

    @Test
    void sqlValuesReplaceBuiltInAtRuntime() {
        Map<Long, String> rows = new HashMap<>(SERVER1_CLASSNAMES);
        rows.put(40010L, "hp.UserAddHp 15 25 189 true");
        rows.put(40013L, "brave.StatusHaste 1800 191 true");
        rows.put(140015L, "mp.UserMprBlue 650 191");
        ConsumableItemParameters.install(ConsumableItemParameters.resolve(rows(rows), "fixture", line -> { }));
        assertArrayEquals(new int[] {15, 25}, InventoryCatalog.healingRange(40010));
        assertEquals(1800, InventoryCatalog.selfHasteDurationSeconds());
        assertEquals(650, InventoryCatalog.bluePotionDurationSeconds(140015));
        assertEquals(191, InventoryCatalog.bluePotionEffectGfx(140015));
        assertArrayEquals(new int[] {25, 25}, InventoryCatalog.healingRange(InventoryCatalog.HEALING_POTION));
    }

    @Test
    void parsingMirrorsServer1SetSetDefaults() {
        var hp = ConsumableItemParameters.Executor.USER_ADD_HP;
        // UserAddHp.java:143-148: min<=0 -> 1.
        assertEquals(ConsumableItemParameters.Value.heal(1, 5, 0),
                ConsumableItemParameters.parse(hp, "hp.UserAddHp 0 5 0"));
        // UserAddHp.java:155-161: max<min -> _max_addhp=0, fixed min.
        assertEquals(ConsumableItemParameters.Value.heal(30, 30, 197),
                ConsumableItemParameters.parse(hp, "hp.UserAddHp 30 10 197 true"));
        // Unparsable numbers keep the executor field defaults (min 1, no range, no gfx).
        assertEquals(ConsumableItemParameters.Value.heal(1, 1, 0),
                ConsumableItemParameters.parse(hp, "hp.UserAddHp x"));
        // StatusHaste.java:155-158 / UserMprBlue.java:92-95: time<=0 -> 300 / 600.
        assertEquals(300, ConsumableItemParameters.parse(
                ConsumableItemParameters.Executor.STATUS_HASTE, "brave.StatusHaste 0 191").seconds());
        assertEquals(600, ConsumableItemParameters.parse(
                ConsumableItemParameters.Executor.USER_MPR_BLUE, "mp.UserMprBlue -1 190").seconds());
        // StatusWisdomPotion has no consume flag, so a trailing token is ignored.
        assertEquals(300, ConsumableItemParameters.parse(
                ConsumableItemParameters.Executor.STATUS_WISDOM_POTION,
                "brave.StatusWisdomPotion 300 750 false").seconds());
    }

    @Test
    void unusableRowsFallBackToBuiltInAndAreLogged() {
        Map<Long, String> rows = new HashMap<>(SERVER1_CLASSNAMES);
        rows.remove(40011L);                                              // missing row
        rows.put(40012L, "brave.StatusBrave 300 751 true");               // other executor
        rows.put(40014L, "brave.StatusBrave 300 751 false 40308 10");     // charge-other mode
        List<String> log = new ArrayList<>();
        var snapshot = ConsumableItemParameters.resolve(rows(rows), "fixture", log::add);
        assertEquals(3, snapshot.fallbacks().size(), snapshot.fallbacks().toString());
        assertEquals(ConsumableItemParameters.BUILT_IN, snapshot.values());
        assertTrue(log.stream().anyMatch(line -> line.contains("item 40011") && line.contains("row missing")));
        assertTrue(log.stream().anyMatch(line -> line.contains("item 40012") && line.contains("executor")));
        assertTrue(log.stream().anyMatch(line -> line.contains("item 40014") && line.contains("non-consuming")));
        var empty = ConsumableItemParameters.resolve(null, "fixture", line -> { });
        assertEquals(ConsumableItemParameters.BUILT_IN.size(), empty.fallbacks().size());
        assertEquals(ConsumableItemParameters.BUILT_IN, empty.values());
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS.size(), empty.delayFallbacks().size());
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS, empty.delays());
    }

    @Test
    void loadWithoutDatabaseNeverThrowsAndKeepsBuiltIn() throws Exception {
        ServerConfig memory = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        assertDoesNotThrow(() -> ConsumableItemParameters.load(memory));
        assertEquals("built-in", ConsumableItemParameters.snapshot().source());
        assertEquals(ConsumableItemParameters.BUILT_IN, ConsumableItemParameters.snapshot().values());
        assertArrayEquals(new int[] {30, 50}, InventoryCatalog.healingRange(40011));
        // T134: a failed read keeps the built-in cooldowns too.
        assertEquals(ConsumableItemParameters.BUILT_IN_DELAYS, ConsumableItemParameters.snapshot().delays());
        assertEquals(new ConsumableItemParameters.Delay(2, 1000), ConsumableItemParameters.delay(40011));
    }
}
