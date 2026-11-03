package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import local.lito381.server.ModernDisintegrateCatalogTest.Jdbc;

final class ModernHasteCatalogTest {
    static Map<String, Object> row() {
        // Independent selected row43 literal; never read the implementation's arrays.
        String[] fields = ("objid learned_owner learned_id template_id skill_level skill_number mp_cost hp_cost item_id item_count reuse_millis "
                + "buff_duration target_to damage_value damage_dice damage_dice_count probability_value probability_dice attr spell_type lawful "
                + "maximum_range area through_flag template_action action_id castgfx castgfx2 sysmsg_happen sysmsg_stop sysmsg_fail effect_skill_id target_bytes").split(" ");
        long[] values = {3333,3333,43,43,6,2,40,0,0,0,10,1200,3,0,0,0,0,0,8,2,0,-1,0,0,4,19,755,0,184,185,280,0,4};
        assertEquals(fields.length, values.length);
        var result = new HashMap<String, Object>();
        for (int i = 0; i < fields.length; i++) result.put(fields[i], values[i]);
        result.put("target", "buff"); return result;
    }
    static Optional<ModernHasteCatalog.Snapshot> read(Jdbc jdbc, int source, String database) throws Exception {
        var scope = ModernDisintegrateCatalogTest.scope(); ModernBackendScope.bind(scope);
        try { return ModernHasteCatalog.read(jdbc.connection(), scope, source, "owner", 3333, database); }
        finally { scope.finish(); ModernBackendScope.unbind(); }
    }
    static Optional<ModernHasteCatalog.Snapshot> read(Jdbc jdbc) throws Exception { return read(jdbc, 7, "catalog"); }

    @Test void exactSourceOwnedTupleHasBoundedSelectsAndDoesNotAliasIdenticalObjectIds() throws Exception {
        var jdbc = new Jdbc(List.of(row())); var result = read(jdbc).orElseThrow();
        assertEquals(ModernHasteCatalog.Status.QUALIFIED, result.status());
        assertEquals(List.of(1,2,4), jdbc.limits);
        assertEquals(3, jdbc.closedStatements); assertEquals(3, jdbc.closedResults);
        assertEquals(Map.of(1,3333L,2,"owner"), jdbc.parameters);
        assertTrue(jdbc.queries.get(1).contains("cs.skill_id=43"));
        assertTrue(jdbc.queries.get(1).contains("`catalog`.`skills`"));
        assertEquals(ModernHasteCatalog.extrasSql("catalog"), jdbc.queries.get(2));
        assertTrue(jdbc.timeouts.stream().allMatch(n -> n > 0 && n <= 10));
        var sun = new ModernHaste.Profile(new TournamentPresence.CharacterKey(7,3333),0,40);
        var moon = new ModernHaste.Profile(new TournamentPresence.CharacterKey(42,3333),0,40);
        assertTrue(result.accepts(sun)); assertFalse(result.accepts(moon)); assertFalse(result.accepts(null));
        var other = new Jdbc(List.of(row()));
        assertTrue(read(other,42,"moon_catalog").orElseThrow().accepts(moon));
        assertTrue(other.queries.get(1).contains("`moon_catalog`.`skills`"));
        assertFalse(result.toString().contains("3333"));
    }
    @Test void everyTupleDifferenceAndLossyNumberRejectsWithoutDefaulting() throws Exception {
        for (String field : row().keySet()) if (!Set.of("objid","learned_owner","learned_id","template_id","target","target_bytes").contains(field)) {
            var changed = row(); changed.put(field, ((Number) changed.get(field)).longValue() + 1);
            assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE, read(new Jdbc(List.of(changed))).orElseThrow().status(), field);
            for (Object invalid : new Object[]{null, new BigDecimal("0.5"), new BigDecimal("9223372036854775808"), 1.0, "1"}) {
                var bad = row(); bad.put(field, invalid);
                assertThrows(SQLException.class, () -> read(new Jdbc(List.of(bad))), field);
            }
        }
        var changed = row(); changed.put("target", "self");
        assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE, read(new Jdbc(List.of(changed))).orElseThrow().status());
    }
    @Test void ownerTemplateDuplicatesAndUnlearnedAreDistinct() throws Exception {
        for (String field : List.of("objid","learned_owner","learned_id","template_id")) for (Object bad : new Object[]{null,42L}) {
            var changed = row(); changed.put(field,bad);
            assertThrows(SQLException.class, () -> read(new Jdbc(List.of(changed))));
        }
        assertThrows(SQLException.class, () -> read(new Jdbc(List.of(row(),row()))));
        assertTrue(read(new Jdbc(List.of())).isEmpty());
        var unlearned = new Jdbc(List.of(Map.of("objid",3333L)));
        assertEquals(ModernHasteCatalog.Status.UNLEARNED, read(unlearned).orElseThrow().status());
        assertEquals(2, unlearned.queries.size());
    }
    @Test void materialExtraBuffNegativeRulesMissingRowsAndUnknownColumnErrorsNeverBecomeDefaults() throws Exception {
        for (var extras : List.of(List.<Map<String,Object>>of(), List.of(Jdbc.EMPTY_ITEM,Jdbc.EMPTY_ITEM),
                List.of(Map.<String,Object>of("extension_kind",0L,"ids_null",0L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",1L,"ids_null",1L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",2L,"ids_null",1L,"counts_null",1L)))) {
            var jdbc = new Jdbc(List.of(row())); jdbc.extras = extras;
            assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE, read(jdbc).orElseThrow().status());
        }
        var absent = new Jdbc(List.of(row())); absent.guardFailure = new SQLException("private","42S22",1054);
        assertEquals(ModernHasteCatalog.Status.QUALIFIED, read(absent).orElseThrow().status());
        assertTrue(absent.queries.get(1).contains("0 AS effect_skill_id"));
        var denied = new Jdbc(List.of(row())); denied.guardFailure = new SQLException("private","42000",1142);
        var error = assertThrows(SQLException.class, () -> read(denied));
        assertEquals("modern haste source unavailable", error.getMessage()); assertNull(error.getCause());
    }
    @Test void cancellationAtQueriesIncludingFinalNextAndInvalidIdentifiersCannotPublishPartialQualification() throws Exception {
        for (int query = 1; query <= 3; query++) for (int next = 1; next <= 2; next++) {
            if (query == 1 && next == 2) continue;
            var jdbc = new Jdbc(List.of(row())); jdbc.cancelQuery = query; jdbc.cancelNext = next;
            assertThrows(ModernBackendExecutor.BackendException.class, () -> read(jdbc));
            assertEquals(jdbc.queries.size(), jdbc.closedStatements); assertEquals(jdbc.queries.size(), jdbc.closedResults);
        }
        for (String db : new String[]{null,"","a;drop","a".repeat(65)}) {
            var jdbc = new Jdbc(List.of(row()));
            assertThrows(IllegalArgumentException.class, () -> read(jdbc,7,db)); assertTrue(jdbc.queries.isEmpty());
        }
    }
}
