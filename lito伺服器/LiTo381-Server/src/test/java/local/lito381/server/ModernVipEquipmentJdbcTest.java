package local.lito381.server;

import static local.lito381.server.ModernVipEquipment.Field.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernVipEquipmentJdbcTest {
    static final String VIP="道具_自訂穿脫能力", POLY="poly_變身賦予能力";

    @Test void fullI64VipAndPolyFieldsRetainDistinctArmorSignsOnOneReadOnlyConnection() throws Exception {
        var jdbc=fixture();var vip=jdbc.rows.get(VIP).get(0);var poly=jdbc.rows.get(POLY).get(0);
        vip.put("CHA",4_294_967_299L);vip.put("MAX_HP",5_000_000_000L);vip.put("AC",-15L);
        poly.put("AC",7L);poly.put("MAX_HP",8_000_000_000L);poly.put("DEX",new BigDecimal("999999"));
        var result=read(jdbc,List.of(300086L));
        assertEquals(7,result.sourceId());assertEquals("catalog",result.database());
        assertEquals(4_294_967_299L,result.vips().get(300086L).abilities().get(CHA));
        assertEquals(5_000_000_000L,result.vips().get(300086L).abilities().get(MAX_HP));
        assertEquals(-15,result.vips().get(300086L).abilities().get(AC));
        assertEquals(-7,result.polymorphs().get(5641L).abilities().get(AC));
        assertEquals(999999,result.polymorphs().get(5641L).abilities().get(DEX));
        assertEquals(0,result.polymorphs().get(5641L).abilities().get(ADENA_RATE));
        assertEquals(-22,result.configuredArmorContribution(300086));
        assertEquals(2,jdbc.statements.size());jdbc.assertClosed();
        assertTrue(jdbc.statements.stream().allMatch(s->s.sql.startsWith("SELECT ")&&s.maxRows==513));
        assertEquals(Map.of(1,300086L),jdbc.statements.get(0).parameters);
        assertEquals(Map.of(1,5641L),jdbc.statements.get(1).parameters);
        assertTrue(jdbc.statements.get(0).sql.contains("JOIN `catalog`.`etcitem`"));
        assertThrows(UnsupportedOperationException.class,()->result.vips().clear());
        assertFalse(result.toString().contains("catalog"));
    }

    @Test void absentPolyIsOnlyKnownAfterSuccessfulCompleteQueryAndZeroPolySkipsTable() throws Exception {
        var absent=fixture();absent.rows.get(POLY).clear();
        var result=read(absent,List.of(300086L));
        assertFalse(result.polymorphs().get(5641L).present());
        assertEquals(ModernVipEquipment.Abilities.zero(),result.polymorphs().get(5641L).abilities());
        assertEquals(2,absent.statements.size());absent.assertClosed();
        var none=fixture();none.rows.get(VIP).get(0).put("polymorph_id",0L);
        assertTrue(read(none,List.of(300086L)).polymorphs().isEmpty());
        assertEquals(1,none.statements.size());none.assertClosed();
        var empty=fixture();assertTrue(read(empty,List.of()).vips().isEmpty());assertTrue(empty.statements.isEmpty());
    }

    @Test void everyAppliedAbilityRejectsMissingNullTextFloatingFractionalAndOverflowData() {
        for(boolean poly:new boolean[]{false,true})for(var field:ModernVipEquipment.Field.values()) {
            if(poly&&field.polyColumn==null)continue;
            for(int mode=0;mode<6;mode++) {
                var jdbc=fixture();var row=jdbc.rows.get(poly?POLY:VIP).get(0);
                if(mode==0)row.remove(field.name());
                else row.put(field.name(),new Object[]{null,"1",1.0,new BigDecimal("1.5"),new BigDecimal("9223372036854775808")}[mode-1]);
                rejected(jdbc,List.of(300086L));
            }
        }
    }

    @Test void absenceDuplicatesUnexpectedIdsAndUnboundedRowsCannotPublishCatalog() {
        for(String table:List.of(VIP,POLY))for(int mode=0;mode<3;mode++) {
            var jdbc=fixture();var rows=jdbc.rows.get(table);var row=rows.get(0);
            if(mode==0) {
                if(table.equals(POLY))row.put("graphics_id",999L);else rows.clear();
            } else if(mode==1)rows.add(new HashMap<>(row));
            else row.put(table.equals(VIP)?"catalog_id":"graphics_id",Long.MAX_VALUE);
            rejected(jdbc,List.of(300086L));
        }
        var jdbc=fixture();assertThrows(SQLException.class,()->read(jdbc,java.util.Collections.nCopies(513,300086L)));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test void classnameIsExactAndDoesNotAcceptArgumentsOrLookalikes() {
        for(String executor:new String[]{"0","add.VIP 1","add.vip","com.lineage.data.item_etcitem.add.VIP","add.VIP\u0000"}) {
            var jdbc=fixture();jdbc.rows.get(VIP).get(0).put("executor_name",executor);rejected(jdbc,List.of(300086L));
        }
        var jdbc=fixture();jdbc.rows.get(VIP).get(0).put("executor_bytes",8L);rejected(jdbc,List.of(300086L));
    }

    @Test void deniedTableIterationOrCloseFailureCannotBecomeAbsentPolyAndErrorsAreRedacted() {
        for(String table:List.of(VIP,POLY))for(String stage:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var jdbc=fixture();jdbc.failureTable=table;jdbc.failureStage=stage;rejected(jdbc,List.of(300086L));
        }
    }

    @Test void cancellationRetiresResourcesWithoutReturningPartialCatalog() {
        for(String table:List.of(VIP,POLY)) {
            var jdbc=fixture();jdbc.cancelTable=table;jdbc.cancelRow=1;
            var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc,List.of(300086L)));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());jdbc.assertClosed();
        }
    }

    @Test void invalidSourceReadWriteConnectionDuplicateIdsAndNegativeMetadataAreRejected() {
        var write=fixture();write.readOnly=false;rejected(write,List.of(300086L));assertTrue(write.statements.isEmpty());
        for(List<Long> ids:List.of(List.of(300086L,300086L),List.of(0L),List.of(-1L))) {
            var jdbc=fixture();rejected(jdbc,ids);assertTrue(jdbc.statements.isEmpty());
        }
        for(String key:List.of("exclusive_type","icon_id","polymorph_id")) {
            var jdbc=fixture();jdbc.rows.get(VIP).get(0).put(key,-1L);rejected(jdbc,List.of(300086L));
        }
        var jdbc=fixture();
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->ModernVipEquipmentJdbc.read(
                jdbc.connection(),ModernBackendScope.current(),7,"catalog` injected",List.of(300086L))));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test void currentConfiguredArmorIsCheckedArithmeticAndNeverUsesNativeWrapping() throws Exception {
        var overflow=fixture();overflow.rows.get(POLY).get(0).put("AC",Long.MIN_VALUE);rejected(overflow,List.of(300086L));
        var sum=fixture();sum.rows.get(VIP).get(0).put("AC",Long.MAX_VALUE);sum.rows.get(POLY).get(0).put("AC",-1L);
        var facts=read(sum,List.of(300086L));
        assertThrows(ArithmeticException.class,()->facts.configuredArmorContribution(300086));
    }

    @Test void catalogRejectsMismatchedIdentityOrIncompletePolyCoverage() throws Exception {
        var facts=read(fixture(),List.of(300086L));
        assertThrows(IllegalArgumentException.class,()->new ModernVipEquipment.Catalog(7,"catalog",facts.vips(),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new ModernVipEquipment.Catalog(7,"catalog",Map.of(99L,facts.vips().get(300086L)),facts.polymorphs()));
        assertThrows(IllegalArgumentException.class,()->new ModernVipEquipment.Abilities(Map.of(AC,0L)));
    }

    @Test void retainedReaderReportsExplicitAbsenceButStrictDefaultStillRejects()throws Exception {
        var jdbc=fixture();jdbc.rows.get(VIP).clear();
        var value=readRetained(jdbc);assertTrue(value.vips().isEmpty());assertTrue(value.polymorphs().isEmpty());
        assertEquals(java.util.Set.of(300086L),value.absentVips());assertEquals(1,jdbc.statements.size());jdbc.assertClosed();
        var strict=fixture();strict.rows.get(VIP).clear();rejected(strict,List.of(300086L));
        assertThrows(UnsupportedOperationException.class,()->value.absentVips().clear());
        var known=read(fixture(),List.of(300086L));
        assertThrows(IllegalArgumentException.class,()->new ModernVipEquipment.Catalog(7,"catalog",known.vips(),known.polymorphs(),java.util.Set.of(300086L)));
        var source=ModernVipCombatProfileTest.source(Map.of(MAX_HP,100L),false);
        assertThrows(IllegalArgumentException.class,()->ModernVipLoadoutTest.withCatalog(source,new ModernVipEquipment.Catalog(7,"catalog",Map.of(),Map.of())));
        var absent=ModernVipLoadoutTest.withCatalog(source,value);
        assertThrows(IllegalArgumentException.class,()->ModernVipLoadout.qualify(absent,ModernCombatProfileTest.binding(absent.input().equipment()),java.util.Optional.empty(),1000,0));
    }

    @Test void retainedReaderNeverConvertsDeniedPartialCancelledOrMalformedRowsToAbsence() {
        for(String stage:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var jdbc=fixture();jdbc.failureTable=VIP;jdbc.failureStage=stage;
            var failure=assertThrows(SQLException.class,()->readRetained(jdbc));assertNull(failure.getCause());jdbc.assertClosed();
        }
        var malformed=fixture();malformed.rows.get(VIP).get(0).remove("CHA");assertThrows(SQLException.class,()->readRetained(malformed));malformed.assertClosed();
        var cancelled=fixture();cancelled.cancelTable=VIP;cancelled.cancelRow=1;
        assertThrows(ModernBackendExecutor.BackendException.class,()->readRetained(cancelled));cancelled.assertClosed();
    }

    private static ModernVipEquipment.Catalog readRetained(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernVipEquipmentJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog",List.of(300086L),true));
    }

    private static ModernPhysicalCombatSourceJdbcTest.Jdbc fixture() {
        var jdbc=new ModernPhysicalCombatSourceJdbcTest.Jdbc(ModernPhysicalCombatSourceJdbcTest.fixture());
        var vip=new HashMap<String,Object>();var poly=new HashMap<String,Object>();
        for(var field:ModernVipEquipment.Field.values()) {
            vip.put(field.name(),0L);if(field.polyColumn!=null)poly.put(field.name(),0L);
        }
        vip.putAll(Map.of("catalog_id",300086L,"exclusive_type",0L,"icon_id",505L,"polymorph_id",5641L,
                "executor_name","add.VIP","executor_bytes",7L));
        poly.putAll(Map.of("graphics_id",5641L,"no_death_exp",0L,"keep_shape",1L));
        jdbc.rows.put(VIP,new ArrayList<>(List.of(vip)));jdbc.rows.put(POLY,new ArrayList<>(List.of(poly)));
        return jdbc;
    }
    private static ModernVipEquipment.Catalog read(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc,List<Long> ids)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernVipEquipmentJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog",ids));
    }
    private static void rejected(ModernPhysicalCombatSourceJdbcTest.Jdbc jdbc,List<Long> ids) {
        var failure=assertThrows(SQLException.class,()->read(jdbc,ids));
        assertEquals("modern VIP source unavailable",failure.getMessage());assertNull(failure.getCause());
        assertNull(failure.getNextException());assertEquals(0,failure.getSuppressed().length);jdbc.assertClosed();
    }
}
