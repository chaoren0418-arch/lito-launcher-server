package local.lito381.server;

import static local.lito381.server.ModernPhysicalCombatTest.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModernPhysicalCombatSourceJdbcTest {
    @Test void equipmentHasteIsOptInAndReusesExactBoundedTemplateQueries() throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);
        jdbc.rows.get("weapon").get(0).put("haste_item",5_000_000_000L);
        jdbc.rows.get("armor").get(0).put("haste_item",Long.MIN_VALUE);
        var result=readHaste(jdbc,source);
        assertEquals(5_000_000_000L,result.templates().get(1L).hasteItem().orElseThrow());
        assertEquals(Long.MIN_VALUE,result.templates().get(2L).hasteItem().orElseThrow());
        assertTrue(result.templates().get(3L).hasteItem().isEmpty());
        assertEquals(7,jdbc.statements.size());
        for(var statement:jdbc.statements) {
            assertEquals(Set.of("weapon","armor").contains(statement.table),statement.sql.contains("haste_item"));
            assertTrue(statement.sql.startsWith("SELECT "));
        }
        jdbc.assertClosed();
        var old=new Jdbc(source);var legacy=read(old,source);
        assertTrue(legacy.templates().values().stream().allMatch(p->p.hasteItem().isEmpty()));
        assertTrue(old.statements.stream().noneMatch(p->p.sql.contains("haste_item")));old.assertClosed();
    }

    @Test void equipmentHasteNeverDefaultsMissingNullFractionTextFloatOrOverflow() {
        for(String table:List.of("weapon","armor"))for(int mode=0;mode<7;mode++) {
            var source=fixture();var jdbc=new Jdbc(source);
            jdbc.rows.get("weapon").get(0).put("haste_item",0L);jdbc.rows.get("armor").get(0).put("haste_item",0L);
            var row=jdbc.rows.get(table).get(0);
            if(mode==0)row.remove("haste_item");
            else row.put("haste_item",new Object[]{null,new BigDecimal("1.5"),"1",1.0,
                    new BigDecimal("9223372036854775808"),new BigDecimal("-9223372036854775809")}[mode-1]);
            var failure=assertThrows(SQLException.class,()->readHaste(jdbc,source));
            assertEquals("modern physical source unavailable",failure.getMessage());
            assertNull(failure.getCause());assertNull(failure.getNextException());jdbc.assertClosed();
        }
    }

    @Test void equipmentHasteLateFailureAndCancellationCannotPublishPartialFacts() {
        for(boolean cancel:new boolean[]{false,true}) {
            var source=fixture();var jdbc=new Jdbc(source);
            jdbc.rows.get("weapon").get(0).put("haste_item",0L);jdbc.rows.get("armor").get(0).put("haste_item",1L);
            if(cancel){jdbc.cancelTable="armor";jdbc.cancelRow=1;}
            else {jdbc.failureTable="armor";jdbc.failureStage="executeQuery";}
            if(cancel)assertEquals(ModernBackendExecutor.Failure.CANCELLED,
                    assertThrows(ModernBackendExecutor.BackendException.class,()->readHaste(jdbc,source)).failure());
            else assertThrows(SQLException.class,()->readHaste(jdbc,source));
            assertTrue(jdbc.statements.stream().noneMatch(s->s.table.startsWith("裝備_")));
            jdbc.assertClosed();
        }
    }

    private static ModernPhysicalCombat.Source readHaste(Jdbc jdbc,ModernPhysicalCombat.Source source) throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(
                jdbc.connection(),ModernBackendScope.current(),source.input(),false,false,false,false,true));
    }

    @Test void bowFieldsAreOptInSameSourceAndOneAdditionalBoundedStatement()throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);var weapon=jdbc.rows.get("weapon").get(0);weapon.put("magic_bow",0L);weapon.put("arrow_spr",-1L);
        var result=ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(jdbc.connection(),ModernBackendScope.current(),source.input(),false,true));
        assertEquals(Map.of(1L,0L),result.magicBows());assertEquals(Map.of(1L,-1L),result.weaponArrowSprites());
        assertEquals(8,jdbc.statements.size());var query=jdbc.statements.get(7);assertTrue(query.sql.contains("magic_bow,arrow_spr"));
        assertEquals(Map.of(1,1L),query.parameters);assertEquals(513,query.maxRows);jdbc.assertClosed();
        var old=new Jdbc(source);assertNull(read(old,source).magicBows());assertEquals(7,old.statements.size());old.assertClosed();
    }
    @Test void bowNumericFieldsNeverDefaultNullFractionApproximateOrMissing() {
        for(String column:List.of("magic_bow","arrow_spr"))for(Object invalid:new Object[]{null,new BigDecimal("1.5"),1.0,"1",new BigDecimal("9223372036854775808"),-2L}) {
            var source=fixture();var jdbc=new Jdbc(source);var row=jdbc.rows.get("weapon").get(0);row.put("magic_bow",0L);row.put("arrow_spr",-1L);row.put(column,invalid);
            assertThrows(SQLException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(jdbc.connection(),ModernBackendScope.current(),source.input(),false,true)));jdbc.assertClosed();
        }
    }
    @Test void completeGrowthTemplateAndProcProofAreBoundedReadonlySameConnection() throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);
        var result=read(jdbc,source);
        assertEquals(withWeaponProvenance(source,Set.of()),result);assertEquals(7,jdbc.statements.size());jdbc.assertClosed();
        assertTrue(jdbc.statements.stream().allMatch(s->s.sql.startsWith("SELECT ")&&!s.sql.contains(" FOR UPDATE")&&!s.sql.contains("fixture")));
        for(var s:jdbc.statements)if(!s.table.startsWith("角色"))assertFalse(s.parameters.isEmpty());
    }

    @Test void bothGrowthTablesMustHaveEveryExactOrderedIdWithoutMissingDuplicateOrExtraRows() {
        for(String table:List.of("角色_力量成長","角色_敏捷成長"))for(int mode=0;mode<4;mode++) {
            var source=fixture();var jdbc=new Jdbc(source);var rows=jdbc.rows.get(table);
            switch(mode) {
                case 0->rows.remove(126);
                case 1->rows.get(1).put("id",1L);
                case 2->rows.get(0).put("id",2L);
                default->rows.add(row("id",128L,"hit",0L,"damage",0L));
            }
            rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void growthNullFractionApproximateTextAndOutOfI64AreNeverLossyDefaults() {
        for(Object value:new Object[]{null,new BigDecimal("1.1"),new BigDecimal("9223372036854775808"),1.0,"1"}) {
            var source=fixture();var jdbc=new Jdbc(source);jdbc.rows.get("角色_力量成長").get(0).put("hit",value);
            rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void growthAcceptsExactSignedLongBoundariesAndIntegralDecimal() throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);
        jdbc.rows.get("角色_力量成長").get(0).put("hit",new BigDecimal(Long.MAX_VALUE));
        jdbc.rows.get("角色_敏捷成長").get(126).put("damage",new BigDecimal(Long.MIN_VALUE));
        var result=read(jdbc,source);
        assertEquals(Long.MAX_VALUE,result.growth().strength().get(0).hit());assertEquals(Long.MIN_VALUE,result.growth().dexterity().get(126).damage());
    }

    @Test void templateCoverageIsExactAndClassStringsAreBoundedUnambiguousUtf8() {
        for(int mode=0;mode<9;mode++) {
            var source=fixture();var jdbc=new Jdbc(source);var rows=jdbc.rows.get("weapon");var row=rows.get(0);
            switch(mode) {
                case 0->rows.clear();case 1->rows.add(new HashMap<>(row));case 2->row.put("item_id",99L);
                case 3->row.put("class_name",null);case 4->row.put("class_name","\u0000");
                case 5->row.put("class_name","\ud800");case 6->row.put("class_bytes",2L);
                case 7->{row.put("class_name","a".repeat(257));row.put("class_bytes",257L);}
                default->{row.put("class_name","\u00a0");row.put("class_bytes",2L);}
            }
            rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void unknownProcTablesPermissionAndAnyQueryFailureNeverMeanNoProc() {
        for(String table:List.of("裝備_weapon_魔法","裝備_armor_魔法","角色_力量成長","weapon")) {
            var source=fixture();var jdbc=new Jdbc(source);jdbc.failureTable=table;jdbc.failureStage="executeQuery";
            rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void multipleProcStagesForOneCatalogAreRecordedButDuplicateRowIdOrForeignItemIsRejected() throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);
        jdbc.rows.get("裝備_weapon_魔法").addAll(List.of(row("id",1L,"item_id",1L),row("id",2L,"item_id",1L)));
        assertEquals(Set.of(1L),read(jdbc,source).procCatalogIds());
        for(int mode=0;mode<3;mode++) {
            jdbc=new Jdbc(source);var rows=jdbc.rows.get("裝備_armor_魔法");rows.add(row("id",1L,"item_id",2L));
            rows.add(row("id",mode==0?1L:2L,"item_id",mode==1?99L:mode==2?(Object)null:2L));rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void procRowsAreHardBoundedEvenIfDriverIgnoresSetMaxRows() throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);
        for(int i=1;i<=1024;i++)jdbc.rows.get("裝備_weapon_魔法").add(row("id",(long)i,"item_id",1L));
        assertEquals(Set.of(1L),read(jdbc,source).procCatalogIds());
        jdbc.rows.get("裝備_weapon_魔法").add(row("id",1025L,"item_id",1L));rejected(jdbc,source);jdbc.assertClosed();
    }

    @Test void exactMultiplierRejectsApproximateTextNullTooPreciseAndSourceChange() {
        for(Object value:new Object[]{1.5f,1.5,"0",null,new BigDecimal("0.0000001"),new BigDecimal("-1"),BigDecimal.ONE}) {
            var source=fixture();var jdbc=new Jdbc(source);jdbc.rows.get("weapon").get(0).put("critical_multiplier",value);
            rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void unchangedExactDecimalCriticalMultiplierIsPreservedWithoutDoubleArithmetic() throws Exception {
        var source=fixture();source=weaponFacts(source,new ModernCombatProfile.WeaponFacts(1,1,1,8,12,1,11,1,0,30,1.5,0,0));
        var jdbc=new Jdbc(source);var result=read(jdbc,source);
        assertEquals(new ModernPhysicalCombat.Rational(15,10),result.templates().get(1L).criticalMultiplier());
    }

    @Test void everySetupReadAndCleanupFailureClosesResourcesAndReturnsOnlyStaticError() {
        for(String stage:List.of("setMaxRows","setQueryTimeout","executeQuery","next","getBigDecimal","result-close","close")) {
            var source=fixture();var jdbc=new Jdbc(source);jdbc.failureTable="角色_力量成長";jdbc.failureStage=stage;
            rejected(jdbc,source);jdbc.assertClosed();
        }
    }

    @Test void cancellationDuringASecondGrowthTablePreventsAnyLaterQueriesOrPartialPublication() {
        var source=fixture();var jdbc=new Jdbc(source);jdbc.cancelTable="角色_敏捷成長";jdbc.cancelRow=50;
        var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc,source));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());assertEquals(2,jdbc.statements.size());jdbc.assertClosed();
    }

    @Test void readonlyAndScopeAreRequiredBeforeReadingAndNoEquipmentStillRequiresBothGrowthTables() throws Exception {
        var source=source(7,0,30,List.of());var jdbc=new Jdbc(source);
        assertEquals(withWeaponProvenance(source,Set.of()),read(jdbc,source));assertEquals(2,jdbc.statements.size());jdbc.assertClosed();
        jdbc=new Jdbc(source);jdbc.readOnly=false;rejected(jdbc,source);assertTrue(jdbc.statements.isEmpty());
    }

    @Test void all512LargeObjectAndCatalogIdsUseBoundParametersNotInterpolatedValues() throws Exception {
        var items=new ArrayList<Instance>();
        for(int i=0;i<512;i++)items.add(weapon(0x1_0000_0000L+i,0x2_0000_0000L+i,i==0,0,Ability.zero(),TemplatePhysical.zero()));
        var source=source(7,0,30,items);var jdbc=new Jdbc(source);var result=read(jdbc,source);
        assertEquals(512,result.templates().size());
        for(var s:jdbc.statements)if(!s.table.startsWith("角色")) {
            assertEquals(512,s.parameters.size());assertEquals(0x2_0000_0000L,s.parameters.get(1));
            assertEquals(0x2_0000_01ffL,s.parameters.get(512));assertFalse(s.sql.contains("8589934592"));
        }
        jdbc.assertClosed();
    }

    @Test void previouslyUnqualifiedInputCannotBecomePhysicalFromOnlyGrowthProof() {
        var original=fixture();var source=new ModernPhysicalCombat.Source(ModernCombatProfile.Input.unqualified(original.input().equipment()),
                original.growth(),original.templates(),Set.of());var jdbc=new Jdbc(source);
        rejected(jdbc,source);assertTrue(jdbc.statements.isEmpty());
    }

    static ModernPhysicalCombat.Source fixture() {
        return source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero()),
                ModernEquipmentEvaluatorTest.simpleArmor(2,2,Ability.zero()),
                ModernEquipmentEvaluatorTest.instance(3,3,ModernCharacterData.ItemKind.ETC,false,0,"$3",List.of(),Ability.zero(),Ability.zero(),
                        ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero())));
    }
    static ModernPhysicalCombat.Source read(Jdbc jdbc,ModernPhysicalCombat.Source source) throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->ModernPhysicalCombatSourceJdbc.qualify(jdbc.connection(),ModernBackendScope.current(),source.input()));
    }
    static ModernPhysicalCombat.Source withWeaponProvenance(ModernPhysicalCombat.Source s,Set<Long> ids) {
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),
                s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),s.armorProcs(),ids);
    }
    @Test void weaponProcProvenanceSurvivesTheUnionWithoutAnyAdditionalQueries()throws Exception {
        var source=fixture();var jdbc=new Jdbc(source);jdbc.rows.get("裝備_weapon_魔法").add(row("id",1L,"item_id",1L));
        var result=read(jdbc,source);
        assertEquals(Set.of(1L),result.weaponProcCatalogIds());assertEquals(Set.of(1L),result.procCatalogIds());
        assertEquals(7,jdbc.statements.size());jdbc.assertClosed();
        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,fighter(result).unsupportedReason());
    }
    private static void rejected(Jdbc jdbc,ModernPhysicalCombat.Source source) {
        var failure=assertThrows(SQLException.class,()->read(jdbc,source));assertEquals("modern physical source unavailable",failure.getMessage());
        assertNull(failure.getCause());assertNull(failure.getNextException());assertEquals(0,failure.getSuppressed().length);
    }
    private static Map<String,Object> row(Object... fields) {var result=new HashMap<String,Object>();for(int i=0;i<fields.length;i+=2)result.put((String)fields[i],fields[i+1]);return result;}

    static final class Jdbc {
        final Map<String,List<Map<String,Object>>> rows=new HashMap<>();final List<Statement> statements=new ArrayList<>();
        boolean readOnly=true,connectionClosed;String failureTable,failureStage,cancelTable;int cancelRow;
        Jdbc(ModernPhysicalCombat.Source source) {
            for(String table:List.of("角色_力量成長","角色_敏捷成長","weapon","armor","etcitem","裝備_weapon_魔法","裝備_armor_魔法"))rows.put(table,new ArrayList<>());
            for(int i=0;i<127;i++) {
                var a=source.growth().strength().get(i);var b=source.growth().dexterity().get(i);
                rows.get("角色_力量成長").add(row("id",a.id(),"hit",a.hit(),"damage",a.damage()));
                rows.get("角色_敏捷成長").add(row("id",b.id(),"hit",b.hit(),"damage",b.damage()));
            }
            for(var e:source.templates().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                var p=e.getValue();String table=switch(p.kind()){case WEAPON->"weapon";case ARMOR->"armor";case ETC->"etcitem";};
                var value=row("item_id",e.getKey(),"class_name",p.className(),"class_bytes",(long)p.className().getBytes(StandardCharsets.UTF_8).length);
                if(p.criticalMultiplier()!=null)value.put("critical_multiplier",BigDecimal.valueOf(p.criticalMultiplier().numerator()).divide(BigDecimal.valueOf(p.criticalMultiplier().denominator())));
                rows.get(table).add(value);
            }
        }
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->switch(m.getName()) {
            case "isReadOnly"->readOnly;case "close"->{connectionClosed=true;yield null;}
            case "prepareStatement"->{var s=new Statement((String)a[0]);statements.add(s);yield s.proxy();}
            default->throw new AssertionError("unexpected connection method "+m.getName());
        });}
        void assertClosed() {assertFalse(connectionClosed);for(var s:statements){assertTrue(s.closed,s.table);if(s.resultOpened)assertTrue(s.resultClosed,s.table);
            assertTrue(s.maxRows==51||s.maxRows==128||s.maxRows==513||s.maxRows==1025);if(!"setMaxRows".equals(failureStage)&&!"setQueryTimeout".equals(failureStage))assertTrue(s.timeout>=1&&s.timeout<=30);}}
        final class Statement {
            final String sql,table;final Map<Integer,Object> parameters=new LinkedHashMap<>();
            int maxRows,timeout;boolean closed,resultOpened,resultClosed;
            Statement(String sql){this.sql=sql;var match=Pattern.compile("FROM `catalog`\\.`([^`]+)`").matcher(sql);assertTrue(match.find());table=match.group(1);assertTrue(rows.containsKey(table));}
            void fail(String stage)throws SQLException {if(table.equals(failureTable)&&stage.equals(failureStage))throw new SQLException("SYNTHETIC-SECRET SQL credential", "42000",1142);}
            PreparedStatement proxy(){return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()) {
                case "setMaxRows"->{maxRows=(Integer)a[0];fail(m.getName());yield null;}case "setQueryTimeout"->{timeout=(Integer)a[0];fail(m.getName());yield null;}
                case "setLong"->{parameters.put((Integer)a[0],a[1]);yield null;}
                case "executeQuery"->{fail(m.getName());resultOpened=true;yield result();}
                case "close"->{closed=true;fail(m.getName());yield null;}
                default->throw new AssertionError("unexpected statement method "+m.getName());
            });}
            ResultSet result(){final int[] at={-1};final boolean[] wasNull={false};var output=rows.get(table);
                ModernJdbcNumbersTest.Value values=column->{if(!output.get(at[0]).containsKey(column))throw new SQLException("missing fixture column");return output.get(at[0]).get(column);};
                var columns=new ModernJdbcNumbersTest.Columns(values);
                return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()) {
                    case "next"->{fail(m.getName());at[0]++;if(table.equals(cancelTable)&&at[0]+1==cancelRow)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield at[0]<output.size();}
                    case "close"->{resultClosed=true;fail("result-close");yield null;}case "wasNull"->wasNull[0];
                    case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();
                    case "getString","getBigDecimal"->{fail(m.getName());Object value=values.read(columns.name(a[0]));wasNull[0]=value==null;
                        yield m.getName().equals("getString")?(value==null?null:value.toString()):ModernJdbcNumbersTest.Columns.decimal(value);}
                    default->throw new AssertionError("unexpected result method "+m.getName());
                });
            }
        }
    }
}
