package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Synthetic JDBC only; same real scope/physical qualifier, no database connection. */
class ModernMagicCombatSourceJdbcTest {
    static final String TABLE="角色_精神成長";
    @Test void optInAddsExactlyOneReadonlyOwnedQueryAndFalsePreservesAllLegacyQueries() throws Exception {
        var source=ModernPhysicalCombatSourceJdbcTest.fixture();var disabled=new ModernPhysicalCombatSourceJdbcTest.Jdbc(source);
        var old=ModernPhysicalCombatSourceJdbcTest.read(disabled,source);
        assertEquals(ModernPhysicalCombatSourceJdbcTest.withWeaponProvenance(source,java.util.Set.of()),old);
        assertNull(old.magicGrowth());assertEquals(7,disabled.statements.size());disabled.assertClosed();
        var enabled=jdbc();
        var loaded=ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(enabled.connection(),ModernBackendScope.current(),source.input(),true));
        assertEquals(source.input(),loaded.input());assertEquals(source.growth(),loaded.growth());
        assertEquals(ModernMagicCombatTest.growth(7),loaded.magicGrowth());assertEquals(8,enabled.statements.size());
        var query=enabled.statements.get(7);assertEquals("SELECT id,`魔防` AS magic_resistance FROM `catalog`.`角色_精神成長` ORDER BY id LIMIT 51",query.sql);
        assertEquals(51,query.maxRows);assertTrue(query.parameters.isEmpty());enabled.assertClosed();
        var fighter=ModernPhysicalCombatTest.fighter(loaded);assertSame(fighter.profile(),fighter.magic().profile());
    }
    @Test void completeSourceTableHasFiftyRowsNotMadeUpZeroOr127AndPreservesActualMr() throws Exception {
        var jdbc=jdbc();jdbc.rows.get(TABLE).get(49).put("magic_resistance",123L);
        var table=read(jdbc);assertEquals(50,table.wisdom().size());assertEquals(123,table.bonus(50));assertEquals(0,table.bonus(0));
        assertEquals(1,jdbc.statements.size());jdbc.assertClosed();
    }
    @Test void missingDuplicateUnorderedZeroAndExcessRowsFailWithoutPartialPublication() {
        for(int mode=0;mode<5;mode++) {
            var jdbc=jdbc();var rows=jdbc.rows.get(TABLE);
            switch(mode){case 0->rows.remove(49);case 1->rows.get(1).put("id",1L);case 2->rows.get(0).put("id",2L);
                case 3->rows.get(0).put("id",0L);default->rows.add(row(51,1));}
            reject(jdbc);jdbc.assertClosed();
        }
    }
    @Test void exactNumbersRejectNullFractionApproximateTextUnsignedOverflowAndNegativeMr() {
        for(String column:List.of("id","magic_resistance"))for(Object value:new Object[]{null,new BigDecimal("1.25"),
                new BigDecimal("9223372036854775808"),1.0d,1.0f,"1",-1L}) {
            var jdbc=jdbc();jdbc.rows.get(TABLE).get(0).put(column,value);reject(jdbc);jdbc.assertClosed();
        }
    }
    @Test void fullPositiveI64AndIntegralDecimalAreNotClampedOrRounded() throws Exception {
        var jdbc=jdbc();jdbc.rows.get(TABLE).get(49).put("magic_resistance",new BigDecimal(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE,read(jdbc).bonus(50));
        jdbc=jdbc();for(var row:jdbc.rows.get(TABLE))row.replaceAll((key,value)->new BigDecimal(value.toString()).setScale(3));
        assertEquals(ModernMagicCombatTest.growth(7),read(jdbc));
    }
    @Test void permissionAndMissingTableNeverBecomeDefaultMagicDefense() {
        var jdbc=jdbc();jdbc.failureTable=TABLE;jdbc.failureStage="executeQuery";
        reject(jdbc);jdbc.assertClosed();assertEquals(1,jdbc.statements.size());
    }
    @Test void setupReadAndCloseFailuresAreRedactedAndCannotReturnSuccess() {
        for(String phase:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var jdbc=jdbc();jdbc.failureTable=TABLE;jdbc.failureStage=phase;reject(jdbc);jdbc.assertClosed();
        }
    }
    @Test void cancellationDuringRowsAndFinalNextWinsAndClosesActualStatementResources() {
        for(int row:new int[]{1,25,51}) {
            var jdbc=jdbc();jdbc.cancelTable=TABLE;jdbc.cancelRow=row;
            var error=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,error.failure());jdbc.assertClosed();
        }
    }
    @Test void invalidIdentifiersAndNonReadonlyConnectionFailBeforeAnyQuery() throws Exception {
        var jdbc=jdbc();
        for(String value:new String[]{null,"","catalog`.`x","x;DROP","x".repeat(65)})
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->
                    ModernMagicCombatSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,value)));
        assertTrue(jdbc.statements.isEmpty());jdbc.readOnly=false;reject(jdbc);assertTrue(jdbc.statements.isEmpty());
    }
    @Test void sourceIdIsPreservedNotPrimaryAndScopeCancellationPrecedesQueries() throws Exception {
        var jdbc=jdbc();var result=ModernEquipmentSourceJdbcTest.scoped(()->ModernMagicCombatSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),42,"catalog"));
        assertEquals(42,result.sourceId());assertEquals("catalog",result.database());
        var cancelled=jdbc();assertThrows(ModernBackendExecutor.BackendException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->{
            var scope=ModernBackendScope.current();scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
            return ModernMagicCombatSourceJdbc.read(cancelled.connection(),scope,7,"catalog");}));
        assertTrue(cancelled.statements.isEmpty());
    }
    static ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc() {
        var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(ModernPhysicalCombatSourceJdbcTest.fixture());
        var rows=new ArrayList<Map<String,Object>>();for(var entry:ModernMagicCombatTest.growth(7).wisdom())rows.add(row(entry.id(),entry.magicResistance()));
        jdbc.rows.put(TABLE,rows);return jdbc;
    }
    private static Map<String,Object> row(long id,long mr) {var row=new HashMap<String,Object>();row.put("id",id);row.put("magic_resistance",mr);return row;}
    private static ModernMagicCombat.Growth read(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc) throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernMagicCombatSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog"));
    }
    private static void reject(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc) {
        var failure=assertThrows(SQLException.class,()->read(jdbc));assertEquals("modern magic growth unavailable",failure.getMessage());
        assertNull(failure.getCause());assertEquals(0,failure.getSuppressed().length);assertNull(failure.getNextException());
    }
}
