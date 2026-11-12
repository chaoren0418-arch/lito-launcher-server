package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
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

    @AfterEach
    void restoreBuiltIn() {
        ConsumableItemParameters.resetForTesting();
    }

    @Test
    void builtInValuesEqualServer1Classnames() {
        assertEquals(SERVER1_CLASSNAMES.keySet(), ConsumableItemParameters.BUILT_IN.keySet());
        List<String> log = new ArrayList<>();
        var snapshot = ConsumableItemParameters.resolve(SERVER1_CLASSNAMES, "fixture", log::add);
        assertEquals(List.of(), snapshot.fallbacks());
        assertEquals(ConsumableItemParameters.BUILT_IN, snapshot.values());
        assertTrue(log.get(log.size() - 1).contains("differing-from-built-in=0"), log.toString());
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
        ConsumableItemParameters.install(ConsumableItemParameters.resolve(rows, "fixture", line -> { }));
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
        var snapshot = ConsumableItemParameters.resolve(rows, "fixture", log::add);
        assertEquals(3, snapshot.fallbacks().size(), snapshot.fallbacks().toString());
        assertEquals(ConsumableItemParameters.BUILT_IN, snapshot.values());
        assertTrue(log.stream().anyMatch(line -> line.contains("item 40011") && line.contains("row missing")));
        assertTrue(log.stream().anyMatch(line -> line.contains("item 40012") && line.contains("executor")));
        assertTrue(log.stream().anyMatch(line -> line.contains("item 40014") && line.contains("non-consuming")));
        var empty = ConsumableItemParameters.resolve(null, "fixture", line -> { });
        assertEquals(ConsumableItemParameters.BUILT_IN.size(), empty.fallbacks().size());
        assertEquals(ConsumableItemParameters.BUILT_IN, empty.values());
    }

    @Test
    void loadWithoutDatabaseNeverThrowsAndKeepsBuiltIn() throws Exception {
        ServerConfig memory = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        assertDoesNotThrow(() -> ConsumableItemParameters.load(memory));
        assertEquals("built-in", ConsumableItemParameters.snapshot().source());
        assertEquals(ConsumableItemParameters.BUILT_IN, ConsumableItemParameters.snapshot().values());
        assertArrayEquals(new int[] {30, 50}, InventoryCatalog.healingRange(40011));
    }
}
