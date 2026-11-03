package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernNaturalRegenSourceJdbcTest {
    static final String TABLE="角色_體質成長";
    static ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc() {
        var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(ModernPhysicalCombatSourceJdbcTest.fixture());
        var rows=new ArrayList<Map<String,Object>>();for(int id=1;id<=50;id++){var row=new HashMap<String,Object>();row.put("id",(long)id);row.put("hp_regen_maximum",(long)id);rows.add(row);}
        jdbc.rows.put(TABLE,rows);return jdbc;
    }
    static ModernNaturalRegen.Growth read(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernNaturalRegenSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog"));
    }
    @Test void optInReadsOneAdditionalCompleteBoundedTableAndOldReadKeepsItsQueryCount()throws Exception {
        var source=ModernPhysicalCombatSourceJdbcTest.fixture();var old=jdbc();assertNull(ModernPhysicalCombatSourceJdbcTest.read(old,source).regenGrowth());assertEquals(7,old.statements.size());
        var enabled=jdbc();var result=ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(enabled.connection(),ModernBackendScope.current(),source.input(),false,false,true));
        assertNotNull(result.regenGrowth());assertEquals(8,enabled.statements.size());var query=enabled.statements.get(7);
        assertEquals("SELECT id,`回血` AS hp_regen_maximum FROM `catalog`.`角色_體質成長` ORDER BY id LIMIT 51",query.sql);
        assertEquals(51,query.maxRows);assertTrue(query.parameters.isEmpty());enabled.assertClosed();assertSame(source.input(),result.input());
    }
    @Test void everyFiftySourceRowIsRequiredAndNoNullFractionTextOrFloatingPointIsDefaulted() {
        for(int mode=0;mode<4;mode++){var jdbc=jdbc();var rows=jdbc.rows.get(TABLE);
            switch(mode){case 0->rows.remove(49);case 1->rows.get(1).put("id",1L);case 2->rows.get(0).put("id",0L);default->rows.add(new HashMap<>(rows.get(49)));}
            assertThrows(SQLException.class,()->read(jdbc));jdbc.assertClosed();}
        for(String column:List.of("id","hp_regen_maximum"))for(Object value:new Object[]{null,0L,-1L,1.0,"1",new BigDecimal("1.5"),new BigDecimal("9223372036854775808")}) {
            var jdbc=jdbc();jdbc.rows.get(TABLE).get(0).put(column,value);assertThrows(SQLException.class,()->read(jdbc));jdbc.assertClosed();}
    }
    @Test void fullI64AndIntegralSqlDecimalAreExactWithoutNarrowing()throws Exception {
        var jdbc=jdbc();jdbc.rows.get(TABLE).get(49).put("hp_regen_maximum",new BigDecimal(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE,read(jdbc).maximum(Long.MAX_VALUE));assertEquals(1,read(jdbc).maximum(0));jdbc.assertClosed();
    }
    @Test void cancellationAtAnyRowOrFinalNextWinsAndAllActualResourcesClose() {
        for(int row:new int[]{1,25,51}) {var jdbc=jdbc();jdbc.cancelTable=TABLE;jdbc.cancelRow=row;
            var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());jdbc.assertClosed();}
    }
    @Test void setupPermissionReadOrCloseFailureNeverReturnsAQualifiedPartialTable() {
        for(String step:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var jdbc=jdbc();jdbc.failureTable=TABLE;jdbc.failureStage=step;var failure=assertThrows(SQLException.class,()->read(jdbc));
            assertEquals("modern natural regeneration growth unavailable",failure.getMessage());assertNull(failure.getCause());assertNull(failure.getNextException());jdbc.assertClosed();}
    }
    @Test void readOnlyScopeAndIdentifierChecksHappenBeforeSql() {
        var jdbc=jdbc();jdbc.readOnly=false;assertThrows(SQLException.class,()->read(jdbc));assertTrue(jdbc.statements.isEmpty());
        var other=jdbc();assertThrows(IllegalArgumentException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->ModernNaturalRegenSourceJdbc.read(other.connection(),ModernBackendScope.current(),7,"catalog`.`other")));
        assertTrue(other.statements.isEmpty());
    }
}
