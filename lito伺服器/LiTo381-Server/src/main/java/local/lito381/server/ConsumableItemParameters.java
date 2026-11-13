package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * T111 (#352): potion/consumable numbers read from {@code 380_db.etcitem.classname},
 * the same parameter strings 1-伺服器's ItemExecutors parse in {@code set_set}.
 *
 * <p>Read-only, once at startup. Every row that is missing, names another executor or
 * cannot be parsed keeps LiTo's built-in value and is logged; a database failure keeps
 * every built-in value. Loading never throws, so it cannot stop the server.</p>
 *
 * <p>Parsing mirrors 1-伺服器 ({@code data/item_etcitem/hp/UserAddHp.java:141-204},
 * {@code brave/StatusHaste.java:151-202}, {@code brave/StatusBrave.java:115-161},
 * {@code brave/StatusElfBrave.java:94-127}, {@code brave/StatusWisdomPotion.java:72-86},
 * {@code mp/UserMprBlue.java:88-102}). Only the consume-the-item mode is supported:
 * a row configured {@code false} (charge another item instead) keeps the built-in value,
 * because LiTo always removes the potion itself.</p>
 *
 * <p>T134 (#385): the same SELECT also reads {@code delay_id, delay_time} for the 14 LiTo
 * potions (shared use cooldown, enforced per character by {@link ItemUseDelays}). Missing
 * columns retry the classname-only query; any unreadable cooldown keeps its built-in value.</p>
 */
final class ConsumableItemParameters {
    enum Executor {
        USER_ADD_HP("hp.UserAddHp", true),
        STATUS_HASTE("brave.StatusHaste", true),
        STATUS_BRAVE("brave.StatusBrave", true),
        STATUS_ELF_BRAVE("brave.StatusElfBrave", true),
        STATUS_WISDOM_POTION("brave.StatusWisdomPotion", false),
        USER_MPR_BLUE("mp.UserMprBlue", false);

        final String className;
        /** Executors whose {@code set_set} reads a consume flag after the numeric fields. */
        final boolean consumeFlag;

        Executor(String className, boolean consumeFlag) {
            this.className = className;
            this.consumeFlag = consumeFlag;
        }
    }

    /** Healing range [min,max] and drink gfx (0 = none), or a timed buff (seconds, gfx). */
    record Value(Executor executor, int min, int max, int gfx) {
        Value {
            if (executor == null || min <= 0 || max < min || gfx < 0 || gfx > 0xFFFF) {
                throw new IllegalArgumentException("invalid consumable parameters");
            }
        }

        static Value heal(int min, int max, int gfx) {
            return new Value(Executor.USER_ADD_HP, min, max, gfx);
        }

        static Value timed(Executor executor, int seconds, int gfx) {
            if (executor == Executor.USER_ADD_HP) throw new IllegalArgumentException("not timed");
            return new Value(executor, seconds, seconds, gfx);
        }

        int seconds() {
            return min;
        }
    }

    /**
     * T134 (#385): shared use cooldown, {@code etcitem.delay_id} / {@code delay_time} (ms).
     * 1-伺服器 {@code L1ItemDelay.java:87-90} ignores a zero group or zero time, and
     * {@code :99} clamps a negative time to zero, so only positive pairs are active.
     */
    record Delay(int id, int millis) {
        static final Delay NONE = new Delay(0, 0);

        boolean active() {
            return id > 0 && millis > 0;
        }
    }

    /** One etcitem row; a {@code null} delay means the columns could not be read. */
    record Row(String classname, Integer delayId, Integer delayTime) {
        static Row classnameOnly(String classname) {
            return new Row(classname, null, null);
        }
    }

    record Snapshot(Map<Long, Value> values, List<String> fallbacks, String source,
            Map<Long, Delay> delays, List<String> delayFallbacks) {
        Snapshot {
            values = Map.copyOf(values);
            fallbacks = List.copyOf(fallbacks);
            delays = Map.copyOf(delays);
            delayFallbacks = List.copyOf(delayFallbacks);
        }
    }

    /**
     * Built-in values (the previous hard-coded InventoryCatalog numbers). All equal the
     * 1-伺服器 {@code db/380_db.sql} etcitem rows; see docs/design/items-consumables.md.
     */
    static final Map<Long, Value> BUILT_IN;

    static {
        Map<Long, Value> m = new LinkedHashMap<>();
        m.put(InventoryCatalog.NATIVE_HEALING_POTION, Value.heal(10, 20, 189));
        m.put(InventoryCatalog.EXTRA_HEALING_POTION, Value.heal(30, 50, 194));
        m.put(InventoryCatalog.GREATER_HEALING_POTION, Value.heal(60, 80, 197));
        m.put(InventoryCatalog.ENT_FRUIT, Value.heal(30, 50, 197));
        m.put(InventoryCatalog.SELF_HASTE_POTION, Value.timed(Executor.STATUS_HASTE,
                InventoryCatalog.SELF_HASTE_DURATION_SECONDS, InventoryCatalog.SELF_HASTE_EFFECT_GFX));
        m.put(InventoryCatalog.BRAVE_POTION, Value.timed(Executor.STATUS_BRAVE,
                InventoryCatalog.BRAVE_DURATION_SECONDS, InventoryCatalog.BRAVE_EFFECT_GFX));
        m.put(InventoryCatalog.ELVEN_WAFER, Value.timed(Executor.STATUS_ELF_BRAVE,
                InventoryCatalog.ELVEN_WAFER_DURATION_SECONDS, InventoryCatalog.ELVEN_WAFER_EFFECT_GFX));
        m.put(InventoryCatalog.WISDOM_POTION, Value.timed(Executor.STATUS_WISDOM_POTION,
                InventoryCatalog.WISDOM_POTION_DURATION_SECONDS, InventoryCatalog.WISDOM_POTION_EFFECT_GFX));
        m.put(InventoryCatalog.BLUE_POTION, Value.timed(Executor.USER_MPR_BLUE,
                InventoryCatalog.BLUE_POTION_DURATION_SECONDS, InventoryCatalog.BLUE_POTION_EFFECT_GFX));
        m.put(InventoryCatalog.BLESSED_BLUE_POTION, Value.timed(Executor.USER_MPR_BLUE,
                InventoryCatalog.BLESSED_BLUE_POTION_DURATION_SECONDS, InventoryCatalog.BLUE_POTION_EFFECT_GFX));
        m.put(InventoryCatalog.WISDOM_COIN, Value.timed(Executor.USER_MPR_BLUE,
                InventoryCatalog.BLUE_POTION_DURATION_SECONDS, InventoryCatalog.BLUE_POTION_EFFECT_GFX));
        BUILT_IN = java.util.Collections.unmodifiableMap(m);
    }

    /**
     * T134 (#385): built-in cooldowns for every LiTo potion, equal to 1-伺服器
     * {@code db/380_db.sql} etcitem rows (line numbers in docs/design/items-consumables.md).
     * Only the healing potions share group 2; the other ten have no cooldown.
     */
    static final Map<Long, Delay> BUILT_IN_DELAYS;

    static {
        Map<Long, Delay> d = new LinkedHashMap<>();
        d.put(InventoryCatalog.NATIVE_HEALING_POTION, new Delay(2, 500));
        d.put(InventoryCatalog.EXTRA_HEALING_POTION, new Delay(2, 1000));
        d.put(InventoryCatalog.GREATER_HEALING_POTION, new Delay(2, 1000));
        d.put(InventoryCatalog.ENT_FRUIT, new Delay(2, 1000));
        for (long id : new long[] {InventoryCatalog.SELF_HASTE_POTION, InventoryCatalog.BRAVE_POTION,
                InventoryCatalog.WISDOM_POTION, InventoryCatalog.ELVEN_WAFER, InventoryCatalog.BLUE_POTION,
                InventoryCatalog.BLESSED_BLUE_POTION, InventoryCatalog.WISDOM_COIN,
                InventoryCatalog.CURE_POISON_POTION, InventoryCatalog.BLINDNESS_POTION,
                InventoryCatalog.BLESS_OF_EVA}) {
            d.put(id, Delay.NONE);
        }
        BUILT_IN_DELAYS = java.util.Collections.unmodifiableMap(d);
    }

    private static final Snapshot BUILT_IN_SNAPSHOT =
            new Snapshot(BUILT_IN, List.of(), "built-in", BUILT_IN_DELAYS, List.of());
    private static volatile Snapshot current = BUILT_IN_SNAPSHOT;
    private static volatile boolean loaded;

    private ConsumableItemParameters() {}

    static Value value(long catalogId) {
        return current.values().get(catalogId);
    }

    /** Shared cooldown for a supported potion, or {@code null} for any other catalog id. */
    static Delay delay(long catalogId) {
        return current.delays().get(catalogId);
    }

    static Snapshot snapshot() {
        return current;
    }

    /** Startup hook: read 380_db once; any failure keeps the built-in values. Never throws. */
    static synchronized void load(ServerConfig config) {
        if (loaded) return;
        loaded = true;
        try {
            var p = UnifiedConfiguration.read(config.source());
            String db = SharedDatabase.identifier(p.getProperty("database.static.database"));
            Map<Long, Row> rows;
            try (var c = DriverManager.getConnection(p.getProperty("database.jdbc.base.url") + db
                    + p.getProperty("database.jdbc.options"), p.getProperty("database.user"),
                    p.getProperty("database.password"))) {
                c.setReadOnly(true);
                rows = read(c, ConsumableItemParameters::log);
            }
            install(resolve(rows, db + ".etcitem", ConsumableItemParameters::log));
        } catch (Exception | LinkageError e) {
            current = BUILT_IN_SNAPSHOT;
            log("[LiTo] consumable parameters: etcitem unavailable (" + e + "); using built-in values for "
                    + BUILT_IN.size() + " items");
        }
    }

    /**
     * Read-only lookup of classname, delay_id and delay_time for every supported item.
     * If the delay columns cannot be selected (older schema), the classname-only query
     * is retried so the T111 parameters still load; every cooldown then keeps its
     * built-in value and is logged by {@link #resolve}.
     */
    static Map<Long, Row> read(Connection connection, Consumer<String> log) throws SQLException {
        String ids = BUILT_IN_DELAYS.keySet().stream().map(String::valueOf)
                .collect(Collectors.joining(","));
        Map<Long, Row> rows = new HashMap<>();
        try (var st = connection.createStatement(); var r = st.executeQuery(
                "SELECT item_id,classname,delay_id,delay_time FROM etcitem WHERE item_id IN ("
                        + ids + ")")) {
            while (r.next()) {
                long id = r.getLong(1);
                String classname = r.getString(2);
                int delayId = r.getInt(3);
                Integer delayIdValue = r.wasNull() ? null : delayId;
                int delayTime = r.getInt(4);
                Integer delayTimeValue = r.wasNull() ? null : delayTime;
                rows.put(id, new Row(classname, delayIdValue, delayTimeValue));
            }
            return rows;
        } catch (SQLException e) {
            log.accept("[LiTo] consumable parameters: etcitem delay_id/delay_time unavailable ("
                    + e + "); using built-in cooldowns");
        }
        rows.clear();
        try (var st = connection.createStatement(); var r = st.executeQuery(
                "SELECT item_id,classname FROM etcitem WHERE item_id IN (" + ids + ")")) {
            while (r.next()) rows.put(r.getLong(1), Row.classnameOnly(r.getString(2)));
        }
        return rows;
    }

    /** Resolves SQL rows against the built-in tables; unresolvable values fall back. */
    static Snapshot resolve(Map<Long, Row> rows, String source, Consumer<String> log) {
        Map<Long, Value> values = new LinkedHashMap<>();
        List<String> fallbacks = new java.util.ArrayList<>();
        int changed = 0;
        for (var entry : BUILT_IN.entrySet()) {
            long id = entry.getKey();
            Value builtIn = entry.getValue();
            Row row = rows == null ? null : rows.get(id);
            String classname = row == null ? null : row.classname();
            Value parsed;
            String problem;
            if (classname == null) {
                parsed = null;
                problem = "row missing";
            } else {
                try {
                    parsed = parse(builtIn.executor(), classname);
                    problem = null;
                } catch (IllegalArgumentException e) {
                    parsed = null;
                    problem = e.getMessage();
                }
            }
            if (parsed == null) {
                fallbacks.add(id + ": " + problem);
                log.accept("[LiTo] consumable parameters: item " + id + " classname='" + classname
                        + "' " + problem + "; using built-in " + builtIn);
                values.put(id, builtIn);
            } else {
                if (!parsed.equals(builtIn)) {
                    changed++;
                    log.accept("[LiTo] consumable parameters: item " + id + " from " + source
                            + " " + parsed + " (built-in " + builtIn + ")");
                }
                values.put(id, parsed);
            }
        }
        Map<Long, Delay> delays = new LinkedHashMap<>();
        List<String> delayFallbacks = new java.util.ArrayList<>();
        int delayChanged = 0;
        for (var entry : BUILT_IN_DELAYS.entrySet()) {
            long id = entry.getKey();
            Delay builtIn = entry.getValue();
            Row row = rows == null ? null : rows.get(id);
            String problem = row == null ? "row missing"
                    : row.delayId() == null || row.delayTime() == null ? "delay columns unavailable"
                    : row.delayId() < 0 ? "negative delay_id" : null;
            if (problem != null) {
                delayFallbacks.add(id + ": " + problem);
                log.accept("[LiTo] consumable parameters: item " + id + " cooldown " + problem
                        + "; using built-in " + builtIn);
                delays.put(id, builtIn);
                continue;
            }
            Delay parsed = new Delay(row.delayId(), row.delayTime());
            if (!parsed.equals(builtIn)) {
                delayChanged++;
                log.accept("[LiTo] consumable parameters: item " + id + " cooldown from " + source
                        + " " + parsed + " (built-in " + builtIn + ")");
            }
            delays.put(id, parsed);
        }
        log.accept("[LiTo] consumable parameters loaded from " + source + ": cooldowns=" + delays.size()
                + " cooldown-differing=" + delayChanged + " cooldown-fallbacks=" + delayFallbacks.size()
                + "; items=" + values.size()
                + " differing-from-built-in=" + changed + " fallbacks=" + fallbacks.size());
        return new Snapshot(values, fallbacks, source, delays, delayFallbacks);
    }

    /** Mirrors the 1-伺服器 {@code set_set} parsing of one classname string. */
    static Value parse(Executor executor, String classname) {
        String[] set = classname.trim().split("\\s+");
        if (!set[0].equals(executor.className)) {
            throw new IllegalArgumentException("executor " + set[0] + " is not " + executor.className);
        }
        if (executor == Executor.USER_ADD_HP) {
            // UserAddHp.java:141-169: min<=0 -> 1; max<min -> fixed min; gfx default 0.
            int min = integer(set, 1, 1);
            if (min <= 0) min = 1;
            int max = integer(set, 2, min);
            if (max < min) max = min;
            int gfx = integer(set, 3, 0);
            requireConsume(set, 4);
            return Value.heal(min, max, Math.max(0, gfx));
        }
        int fallbackSeconds = executor == Executor.USER_MPR_BLUE ? 600 : 300;
        int seconds = integer(set, 1, fallbackSeconds);
        if (seconds <= 0) seconds = fallbackSeconds;
        int gfx = integer(set, 2, 0);
        if (executor.consumeFlag) requireConsume(set, 3);
        return Value.timed(executor, seconds, Math.max(0, gfx));
    }

    /** Executors default to consuming; an explicit false means "charge something else". */
    private static void requireConsume(String[] set, int index) {
        if (set.length <= index) return;
        String flag = set[index];
        if (flag.equalsIgnoreCase("false") || flag.equals("0")) {
            throw new IllegalArgumentException("non-consuming mode is not supported by LiTo");
        }
    }

    /** 1-伺服器 swallows NumberFormatException and keeps its field default. */
    private static int integer(String[] set, int index, int fallback) {
        if (set.length <= index) return fallback;
        try {
            return Integer.parseInt(set[index]);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static void install(Snapshot snapshot) {
        current = snapshot == null ? BUILT_IN_SNAPSHOT : snapshot;
    }

    /** Test hook: restore built-in values and allow another {@link #load}. */
    static synchronized void resetForTesting() {
        current = BUILT_IN_SNAPSHOT;
        loaded = false;
    }

    private static void log(String line) {
        System.out.println(line);
    }
}
