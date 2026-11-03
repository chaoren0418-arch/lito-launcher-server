package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentSourceJdbcTest.scoped;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Uses the SAME strict row/metadata fixture as the source pipeline regressions. */
class ModernCombatSourceJdbcTest {
    @Test void sameSourceDataStatementQualifiesActualWeaponAndNeverExecutesAnotherItemQuery() throws Exception {
        var row=weapon(0x1_0000_0000L);row.put("combat_small",Long.MAX_VALUE);row.put("combat_large",Long.MAX_VALUE-1);
        row.put("combat_range",-1L);row.put("combat_critical_multiplier",new BigDecimal("1.5"));
        var jdbc=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(row));
        var source=read(jdbc).orElseThrow();
        assertEquals(3,jdbc.prepares);assertEquals(1,jdbc.metadataPrepares);assertEquals(1,jdbc.dataPrepares);
        assertEquals(3,jdbc.executions);assertEquals(Map.of(1,3333L,2,"owner"),jdbc.parameters);
        assertTrue(jdbc.sql.contains("w.dmg_small AS combat_small"));assertTrue(jdbc.sql.contains("i.attr_enchant_kind AS combat_attribute_kind"));
        assertTrue(jdbc.sql.contains("LEFT(w.material,33)"));assertFalse(jdbc.sql.contains(" FOR UPDATE"));
        assertFalse(ModernEquipmentSourceJdbc.sql("catalog").contains("combat_small"));
        var facts=source.weapons().get(0x1_0000_0000L);
        assertEquals(Long.MAX_VALUE,facts.damageSmall());assertEquals(Long.MAX_VALUE-1,facts.damageLarge());
        assertEquals(-1,facts.range());assertEquals(17,facts.material());assertEquals(1.5,facts.criticalMultiplier());
        assertEquals(source.equipment().snapshot().inventory().get(0).objectId(),facts.objectId());
        jdbc.assertClosed();assertFalse(jdbc.connectionClosed);
    }

    @Test void armorEtcAndEmptyInventoryDoNotInventWeaponFactsOrReadNullWeaponJoinColumns() throws Exception {
        for(var rows:List.of(List.of(ModernEquipmentSourceJdbcTest.character()),
                List.of(ModernEquipmentSourceJdbcTest.item(1,"a","armor"),ModernEquipmentSourceJdbcTest.item(2,"e",null)))) {
            var jdbc=new ModernEquipmentSourceJdbcTest.Jdbc(rows);var source=read(jdbc).orElseThrow();
            assertTrue(source.weapons().isEmpty());jdbc.assertClosed();
        }
        var absent=new ModernEquipmentSourceJdbcTest.Jdbc(List.of());assertTrue(read(absent).isEmpty());absent.assertClosed();
    }

    @Test void sourceBundleQualifiesSameCatalogAndIsImmutableWithNoMissingCandidateFallback() throws Exception {
        var jdbc=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(weapon(1),weapon(2)));
        var source=read(jdbc).orElseThrow();
        var items=source.equipment().instances();
        var catalog=new ModernEquipmentFacts.Catalog(7,"catalog",ModernEquipmentEvaluatorTest.defaults(items,false),
                ModernEquipmentFacts.requiredSetIds(items),List.of(),List.of(),ModernEquipmentFacts.VipTableState.VERIFIED_ABSENT);
        var qualified=source.qualify(catalog);assertTrue(qualified.weaponsQualified());assertEquals(2,qualified.weapons().size());
        assertThrows(UnsupportedOperationException.class,()->source.weapons().clear());
        assertThrows(IllegalArgumentException.class,()->new ModernCombatSourceJdbc.Source(source.equipment(),Map.of()));
        assertThrows(IllegalArgumentException.class,()->source.qualify(new ModernEquipmentFacts.Catalog(42,"catalog",catalog.lookups(),
                catalog.verifiedSetIds(),catalog.sets(),catalog.combos(),catalog.vipTableState())));
    }

    @Test void everyRetainedMaterialNameMapsExactlyAndUnknownOrTruncatedTextFails() throws Exception {
        String[] names={"none","liquid","web","vegetation","animalmatter","paper","cloth","leather","wood","bone",
                "dragonscale","iron","steel","copper","silver","gold","platinum","mithril","blackmithril","glass","gemstone","mineral","oriharukon"};
        for(int index=0;index<names.length;index++) {
            var row=weapon(1);material(row,names[index]);
            assertEquals(index,read(new ModernEquipmentSourceJdbcTest.Jdbc(List.of(row))).orElseThrow().weapons().get(1L).material());
        }
        for(String invalid:new String[]{null,"","IRON","unsupported","iron\n","iron\0","a".repeat(33),"\ud800"}) {
            var row=weapon(1);material(row,invalid);rejected(row);
        }
        var truncated=weapon(1);truncated.put("combat_material_bytes",100L);rejected(truncated);
    }

    @Test void requiredWeaponFieldsRejectNullAndMissingInsteadOfDefaultingToZero() {
        for(String column:List.of("combat_small","combat_large","combat_range","combat_bless","combat_maximum_chance",
                "combat_critical_chance","combat_critical_multiplier","combat_material_bytes")) {
            var row=weapon(1);row.put(column,null);rejected(row);
            row=weapon(1);row.remove(column);rejected(row);
        }
        for(String column:List.of("combat_attribute_kind","combat_attribute_level")) {
            var row=weapon(1);row.remove(column);rejected(row);
        }
    }

    @Test void onlySelectedNullableAttributeColumnsUseProvenNativeNullAsNoEnchant() throws Exception {
        var row=weapon(1);row.put("combat_attribute_kind",null);row.put("combat_attribute_level",null);
        var facts=read(new ModernEquipmentSourceJdbcTest.Jdbc(List.of(row))).orElseThrow().weapons().get(1L);
        assertEquals(0,facts.attributeKind());assertEquals(0,facts.attributeLevel());
        row=weapon(1);row.put("combat_attribute_kind",1L);row.put("combat_attribute_level",null);rejected(row);
    }

    @Test void integerCombatFactsRejectFractionsApproximationTextNegativeAndOverflow() {
        for(String column:List.of("combat_small","combat_large","combat_range","combat_bless","combat_maximum_chance",
                "combat_critical_chance","combat_attribute_kind","combat_attribute_level")) {
            for(Object bad:List.of(new BigDecimal("1.1"),1.0,1.0f,"1",BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))) {
                var row=weapon(1);row.put(column,bad);rejected(row);
            }
        }
        for(String column:List.of("combat_small","combat_large","combat_bless","combat_attribute_kind","combat_attribute_level")) {
            var row=weapon(1);row.put(column,-1L);rejected(row);
        }
        var row=weapon(1);row.put("combat_range",-2L);rejected(row);
    }

    @Test void multiplierIsExplicitRetainedNumericDoubleNotAStringOrSilentNonfiniteValue() throws Exception {
        for(Object value:List.of(2L,new BigDecimal("1.5"),1.5,1.5f,0L)) {
            var row=weapon(1);row.put("combat_critical_multiplier",value);
            assertEquals(((Number)value).doubleValue(),read(new ModernEquipmentSourceJdbcTest.Jdbc(List.of(row)))
                    .orElseThrow().weapons().get(1L).criticalMultiplier());
        }
        for(Object bad:List.of(-1L,"1.5",Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            var row=weapon(1);row.put("combat_critical_multiplier",bad);rejected(row);
        }
        var jdbc=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(weapon(1)));
        jdbc.numericTypes.put("combat_critical_multiplier",Types.VARCHAR);
        assertThrows(SQLException.class,()->read(jdbc));jdbc.assertClosed();
    }

    @Test void chanceAndBlessingDomainBoundsAreNotSilentlySaturated() {
        for(String column:List.of("combat_maximum_chance","combat_critical_chance"))for(long value:new long[]{-1,101,Long.MAX_VALUE}) {
            var row=weapon(1);row.put(column,value);rejected(row);
        }
        var row=weapon(1);row.put("combat_bless",4L);rejected(row);
    }

    @Test void duplicateOrWrongOwnerRowsAbortTheWholeCollectorAfterEarlierValidWeapon() {
        var duplicate=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(weapon(1),weapon(1)));
        assertThrows(SQLException.class,()->read(duplicate));duplicate.assertClosed();
        var bad=weapon(2);bad.put("objid",3334L);
        var wrong=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(weapon(1),bad));
        assertThrows(SQLException.class,()->read(wrong));wrong.assertClosed();
    }

    @Test void cancellationAndResultOrStatementFailureCannotPublishPartialWeaponMap() {
        var cancelled=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(weapon(1),weapon(2)));cancelled.cancelAtRow=2;
        var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(cancelled));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());cancelled.assertClosed();
        var closed=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(weapon(1)));closed.closeFailure=true;
        assertThrows(SQLException.class,()->read(closed));closed.assertClosed();
    }

    @Test void full512WeaponsHaveAllFactsAnd513OrInvalidIdentifierFailsBeforePublication() throws Exception {
        var rows=new ArrayList<Map<String,Object>>();for(int i=0;i<512;i++)rows.add(weapon(0x1_0000_0000L+i));
        var jdbc=new ModernEquipmentSourceJdbcTest.Jdbc(rows);assertEquals(512,read(jdbc).orElseThrow().weapons().size());jdbc.assertClosed();
        rows.add(weapon(0x1_0000_0200L));var excess=new ModernEquipmentSourceJdbcTest.Jdbc(rows);
        assertThrows(SQLException.class,()->read(excess));excess.assertClosed();
        var invalid=new ModernEquipmentSourceJdbcTest.Jdbc(List.of());
        assertThrows(SQLException.class,()->scoped(()->ModernCombatSourceJdbc.read(invalid.connection(),ModernBackendScope.current(),7,"owner",3333,"catalog`;DROP")));
        assertEquals(0,invalid.prepares);
    }

    private static java.util.Optional<ModernCombatSourceJdbc.Source> read(ModernEquipmentSourceJdbcTest.Jdbc jdbc) throws Exception {
        return scoped(()->ModernCombatSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"owner",3333,"catalog"));
    }
    private static void rejected(Map<String,Object> row) {
        var jdbc=new ModernEquipmentSourceJdbcTest.Jdbc(List.of(row));
        var error=assertThrows(SQLException.class,()->read(jdbc));
        assertFalse(error.getMessage().contains("SYNTHETIC"));assertNull(error.getCause());assertNull(error.getNextException());
        assertEquals(0,error.getSuppressed().length);jdbc.assertClosed();
    }
    static Map<String,Object> weapon(long id) {
        var row=ModernEquipmentSourceJdbcTest.item(id,"w","sword");
        row.put("combat_small",8L);row.put("combat_large",12L);row.put("combat_range",1L);material(row,"mithril");
        row.put("combat_bless",1L);row.put("combat_critical_chance",20L);row.put("combat_critical_multiplier",new BigDecimal("1.5"));
        row.put("combat_maximum_chance",10L);row.put("combat_attribute_kind",0L);row.put("combat_attribute_level",0L);return row;
    }
    private static void material(Map<String,Object> row,String value) {
        row.put("combat_material",value);row.put("combat_material_bytes",value==null?null:(long)value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }
}
