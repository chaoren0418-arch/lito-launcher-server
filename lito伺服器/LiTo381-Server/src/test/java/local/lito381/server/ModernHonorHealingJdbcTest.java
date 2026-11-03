package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernHonorHealingJdbcTest {
    static final String TABLE="角色_智力成長";
    static ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc(){
        var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(ModernPhysicalCombatSourceJdbcTest.fixture());var rows=new ArrayList<Map<String,Object>>();
        for(int n=1;n<=127;n++){var r=new HashMap<String,Object>();r.put("id",(long)n);r.put("healing_bonus",(long)n-2);rows.add(r);}jdbc.rows.put(TABLE,rows);return jdbc;
    }
    static ModernHonorHealing.Growth read(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc)throws Exception{
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernHonorHealingJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog"));
    }
    @Test void oneBoundedSameSourceSelectRetainsSignedAndWideRowsAndClosesItsResources()throws Exception{
        var jdbc=jdbc();jdbc.rows.get(TABLE).get(126).put("healing_bonus",new BigDecimal(Long.MAX_VALUE));var growth=read(jdbc);assertEquals(-1,growth.rows().get(0).healingBonus());assertEquals(Long.MAX_VALUE,growth.rows().get(126).healingBonus());
        assertEquals(1,jdbc.statements.size());assertEquals("SELECT id,`治癒骰加成` AS healing_bonus FROM `catalog`.`角色_智力成長` ORDER BY id LIMIT 128",jdbc.statements.get(0).sql);assertEquals(128,jdbc.statements.get(0).maxRows);jdbc.assertClosed();
    }
    @Test void missingDuplicateOrExtraRowsCannotQualify(){
        for(int mode=0;mode<3;mode++){var jdbc=jdbc();var rows=jdbc.rows.get(TABLE);if(mode==0)rows.remove(126);else if(mode==1)rows.get(1).put("id",1L);else rows.add(new HashMap<>(rows.get(126)));
            assertThrows(SQLException.class,()->read(jdbc));jdbc.assertClosed();}
    }
    @Test void nullLossyFractionAndOverflowAreRejectedWithoutDefaultingTheBonus(){
        for(Object invalid:new Object[]{null,1.0,"1",new BigDecimal("0.5"),new BigDecimal("9223372036854775808")}){var jdbc=jdbc();jdbc.rows.get(TABLE).get(0).put("healing_bonus",invalid);assertThrows(SQLException.class,()->read(jdbc));jdbc.assertClosed();}
    }
    @Test void cancellationWinsIncludingFinalNextAndActualCloseFailuresAreNotSuccessful(){
        for(int row:new int[]{1,64,128}){var jdbc=jdbc();jdbc.cancelTable=TABLE;jdbc.cancelRow=row;assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));jdbc.assertClosed();}
        for(String step:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")){
            var jdbc=jdbc();jdbc.failureTable=TABLE;jdbc.failureStage=step;var e=assertThrows(SQLException.class,()->read(jdbc));assertEquals("honor healing unavailable",e.getMessage());assertNull(e.getCause());assertNull(e.getNextException());jdbc.assertClosed();}
    }
    @Test void legacyPhysicalReadDoesNotAcquireANewRequiredTable()throws Exception{
        var source=ModernPhysicalCombatSourceJdbcTest.fixture();var old=jdbc();assertNull(ModernPhysicalCombatSourceJdbcTest.read(old,source).healingGrowth());assertEquals(7,old.statements.size());
        var enabled=jdbc();var result=ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(enabled.connection(),ModernBackendScope.current(),source.input(),false,false,false,true));
        assertEquals(8,enabled.statements.size());assertNotNull(result.healingGrowth());assertSame(source.input(),result.input());enabled.assertClosed();
    }
}
