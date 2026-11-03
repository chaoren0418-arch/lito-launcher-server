package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Source qualification independent of Honor; no network capability implied. */
class ModernPlayerHealingSourceJdbcTest {
    static final String TABLE=ModernHonorHealingJdbcTest.TABLE;
    static ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc(ModernPhysicalCombat.Source source) {
        var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(source);
        jdbc.rows.put(TABLE,ModernHonorHealingJdbcTest.jdbc().rows.get(TABLE));
        jdbc.rows.put(ModernMagicCombatSourceJdbcTest.TABLE,ModernMagicCombatSourceJdbcTest.jdbc().rows.get(ModernMagicCombatSourceJdbcTest.TABLE));
        return jdbc;
    }
    static ModernPhysicalCombat.Source read(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc,ModernPhysicalCombat.Source source,boolean honor,boolean healing)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(jdbc.connection(),ModernBackendScope.current(),source.input(),true,false,false,honor,false,healing));
    }
    @Test void participantOptInRequiresOneGrowthReadWithoutAnyHonorDependencyOrDuplicateQuery()throws Exception {
        for(boolean honor:List.of(false,true)) {
            var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=jdbc(source);var result=read(jdbc,source,honor,true);
            assertSame(source.input(),result.input());assertEquals(source.growth(),result.growth());assertNotNull(result.magicGrowth());assertNotNull(result.healingGrowth());
            assertEquals(7,result.healingGrowth().sourceId());assertEquals("catalog",result.healingGrowth().database());
            assertEquals(9,jdbc.statements.size());assertEquals(1,jdbc.statements.stream().filter(s->s.table.equals(TABLE)).count());
            var q=jdbc.statements.get(8);assertEquals(128,q.maxRows);assertTrue(q.sql.startsWith("SELECT "));jdbc.assertClosed();
        }
    }
    @Test void legacyEntryStillDoesNotRequireTheHealingTableAndDoesNotImplicitlyOptIn()throws Exception {
        var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=jdbc(source);jdbc.failureTable=TABLE;jdbc.failureStage="executeQuery";
        var old=ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(jdbc.connection(),ModernBackendScope.current(),source.input(),true,false,false,false,false));
        assertNull(old.healingGrowth());assertEquals(8,jdbc.statements.size());jdbc.assertClosed();
        var explicit=jdbc(source);assertNull(read(explicit,source,false,false).healingGrowth());assertEquals(8,explicit.statements.size());explicit.assertClosed();
    }
    @Test void sourceIdentityAndCompleteSignedWideGrowthRemainExact()throws Exception {
        var source=ModernPhysicalCombatTest.source(42,0,30,List.of());var jdbc=jdbc(source);
        jdbc.rows.get(TABLE).get(126).put("healing_bonus",new BigDecimal(Long.MAX_VALUE));
        var result=read(jdbc,source,false,true);assertEquals(42,result.healingGrowth().sourceId());assertEquals(42,result.magicGrowth().sourceId());
        assertEquals(-1,result.healingGrowth().rows().get(0).healingBonus());assertEquals(Long.MAX_VALUE,result.healingGrowth().rows().get(126).healingBonus());
        assertEquals(4,jdbc.statements.size());jdbc.assertClosed();
    }
    @Test void incompleteDuplicateExtraAndLossyRowsCannotPublishAFallbackProfile() {
        for(int mode=0;mode<8;mode++) {
            var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=jdbc(source);var rows=jdbc.rows.get(TABLE);
            if(mode==0)rows.remove(126);else if(mode==1)rows.get(1).put("id",1L);else if(mode==2)rows.add(new java.util.HashMap<>(rows.get(126)));
            else rows.get(0).put("healing_bonus",new Object[]{null,"1",1.0,new BigDecimal("1.5"),new BigDecimal("9223372036854775808")}[mode-3]);
            var error=assertThrows(SQLException.class,()->read(jdbc,source,false,true));assertEquals("modern physical source unavailable",error.getMessage());
            assertNull(error.getCause());assertNull(error.getNextException());assertEquals(0,error.getSuppressed().length);jdbc.assertClosed();
        }
    }
    @Test void cancelledAndFailedLastQueryClosesResourcesWithoutReturningPartialSource() {
        for(int row:new int[]{1,64,128}) {
            var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=jdbc(source);jdbc.cancelTable=TABLE;jdbc.cancelRow=row;
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc,source,false,true)).failure());jdbc.assertClosed();
        }
        for(String stage:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=jdbc(source);jdbc.failureTable=TABLE;jdbc.failureStage=stage;
            var error=assertThrows(SQLException.class,()->read(jdbc,source,false,true));assertEquals("modern physical source unavailable",error.getMessage());
            assertNull(error.getCause());assertNull(error.getNextException());jdbc.assertClosed();
        }
    }
    @Test void magicQualificationReadonlyAndActiveScopeAreRequiredBeforeAnyQuery() {
        var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=jdbc(source);
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(
                jdbc.connection(),ModernBackendScope.current(),source.input(),false,false,false,false,false,true)));
        assertTrue(jdbc.statements.isEmpty());jdbc.readOnly=false;assertThrows(SQLException.class,()->read(jdbc,source,false,true));assertTrue(jdbc.statements.isEmpty());
        var cancelled=jdbc(source);assertThrows(ModernBackendExecutor.BackendException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->{
            var scope=ModernBackendScope.current();scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
            return ModernPhysicalCombatSourceJdbc.qualify(cancelled.connection(),scope,source.input(),true,false,false,false,false,true);
        }));assertTrue(cancelled.statements.isEmpty());
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentRepository.physicalJdbc(java.util.Map.of(),false,false,false,false,false,true));
    }
}
