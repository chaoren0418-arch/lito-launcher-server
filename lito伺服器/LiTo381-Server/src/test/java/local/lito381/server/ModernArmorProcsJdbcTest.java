package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernArmorProcsJdbcTest {
    static final String TABLE="裝備_armor_魔法";
    static Map<String,Object> row(long id,long item) {
        var row=new HashMap<String,Object>();for(var name:ModernArmorProcs.COLUMNS)row.put(name,0L);
        row.putAll(Map.of("id",id,"item_id",item,"enchant_level",-1L,"proc_group",88L,"on_hurt_chance",10L,
                "trigger_skill",68L,"skill_time_sec",3L,"skill_target",1L,"gfx_id",11101L,"gfx_mode",1L));return row;
    }
    static ModernPhysicalCombatSourceJdbcTest.Jdbc fixture() {
        var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(ModernPhysicalCombatSourceJdbcTest.fixture());
        jdbc.rows.get(TABLE).add(row(50,88889));return jdbc;
    }
    static ModernArmorProcs.Catalog read(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc,List<Long> ids)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernArmorProcsJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog",ids));
    }
    @Test void complete18FieldsAreWideImmutableAndUseOneBoundedSelect()throws Exception {
        var jdbc=fixture();jdbc.rows.get(TABLE).get(0).put("heal_mp",5_000_000_000L);
        var catalog=read(jdbc,List.of(88889L));var row=catalog.selected(88889,0).get(0);
        assertEquals(5_000_000_000L,row.mp());assertEquals(68,row.skill());assertEquals(3,row.seconds());assertEquals(11101,row.graphics());
        assertEquals(1,jdbc.statements.size());var statement=jdbc.statements.get(0);
        assertEquals(1025,statement.maxRows);assertEquals(Map.of(1,88889L),statement.parameters);
        for(var name:ModernArmorProcs.COLUMNS)assertTrue(statement.sql.contains("`"+name+"`"));
        jdbc.assertClosed();
    }
    @Test void certifiedAbsenceDiffersFromMissingOrUnrequestedCatalog()throws Exception {
        var jdbc=fixture();jdbc.rows.get(TABLE).clear();var catalog=read(jdbc,List.of(88889L));
        assertTrue(catalog.selected(88889,0).isEmpty());assertTrue(catalog.presentIds().isEmpty());jdbc.assertClosed();
        assertThrows(IllegalArgumentException.class,()->catalog.selected(2,0));
        var empty=fixture();assertTrue(read(empty,List.of()).byItem().isEmpty());assertTrue(empty.statements.isEmpty());
    }
    @Test void everyFieldRejectsMissingNullTextFloatingFractionalAndOverflowData() {
        for(var field:ModernArmorProcs.COLUMNS)for(int mode=0;mode<6;mode++) {
            var jdbc=fixture();var row=jdbc.rows.get(TABLE).get(0);
            if(mode==0)row.remove(field);else row.put(field,new Object[]{null,"1",1.0,new BigDecimal("1.5"),new BigDecimal("9223372036854775808")}[mode-1]);
            rejected(jdbc,List.of(88889L));
        }
    }
    @Test void failedQueryIterationAndCleanupAreRedactedAndNeverPublishPartialRows() {
        for(var stage:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var jdbc=fixture();jdbc.failureTable=TABLE;jdbc.failureStage=stage;rejected(jdbc,List.of(88889L));
        }
        var cancel=fixture();cancel.cancelTable=TABLE;cancel.cancelRow=1;
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,assertThrows(ModernBackendExecutor.BackendException.class,
                ()->read(cancel,List.of(88889L))).failure());cancel.assertClosed();
    }
    @Test void queryInputAndResultIdentitiesAreBoundedAndExact()throws Exception {
        for(int mode=0;mode<4;mode++) {
            var jdbc=fixture();var rows=jdbc.rows.get(TABLE);
            if(mode==0)rows.add(new HashMap<>(rows.get(0)));
            if(mode==1)rows.get(0).put("item_id",99L);
            if(mode==2)rows.get(0).put("enchant_level",-2L);
            if(mode==3)for(int i=51;i<1075;i++)rows.add(row(i,88889));
            rejected(jdbc,List.of(88889L));
        }
        for(var ids:List.of(List.of(0L),List.of(88889L,88889L),java.util.Collections.nCopies(513,88889L))) {
            var jdbc=fixture();rejected(jdbc,ids);assertTrue(jdbc.statements.isEmpty());
        }
        var write=fixture();write.readOnly=false;rejected(write,List.of(88889L));assertTrue(write.statements.isEmpty());
    }
    @Test void productionSourceCarriesFullFactsButDoesNotGrantUnimplementedCombat()throws Exception {
        var source=ModernPhysicalCombatSourceJdbcTest.fixture();var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(source);
        jdbc.rows.get(TABLE).add(row(50,2));
        var result=ModernPhysicalCombatSourceJdbcTest.read(jdbc,source);
        assertEquals(1,result.armorProcs().selected(2,0).size());assertEquals(8,jdbc.statements.size());
        var fighter=ModernPhysicalCombatTest.fighter(result);
        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,fighter.unsupportedReason());jdbc.assertClosed();
        var wrong=new ModernArmorProcs.Catalog(42,"catalog",result.armorProcs().byItem());
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(result.input(),result.growth(),result.templates(),result.procCatalogIds(),
                result.magicGrowth(),result.magicBows(),result.weaponArrowSprites(),result.regenGrowth(),result.healingGrowth(),result.vipCatalog(),wrong));
    }
    static void rejected(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc,List<Long> ids) {
        var failure=assertThrows(SQLException.class,()->read(jdbc,ids));assertEquals("modern armor proc source unavailable",failure.getMessage());
        assertNull(failure.getCause());assertNull(failure.getNextException());assertEquals(0,failure.getSuppressed().length);jdbc.assertClosed();
    }
}
