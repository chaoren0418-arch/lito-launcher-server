package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class ModernEquipmentCatalogJdbcTest {
    private static final String BLESS_TABLE="裝備_armor_bless_指定";
    private static final String COMBO_TABLE="裝備_特殊_卡片_組合";
    private static final String SET_TABLE="裝備_特殊_套裝_能力";
    private static final String VIP_TABLE="道具_自訂穿脫能力";
    private static final String OPTIONAL="add_cha,add_hp,add_mp,add_potion_recovery,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,"
            +"add_stun_success,add_hit,add_dmg,add_bow_hit,add_bow_dmg,add_pierce,add_ac,add_er,add_reduction,add_melee_redux,add_bow_redux,add_ignore_reduction,add_pvp_redux,add_water,add_wind,add_earth";
    private static final String VIP="道具編號,力量,敏捷,智力,精神,體質,抗魔,魔攻,回血,回魔,暗黑耐性,睡眠耐性,寒冰耐性,石化耐性,支撐耐性,昏迷耐性,"
            +"火屬性防禦,水屬性防禦,風屬性防禦,地屬性防禦,近戰命中,近戰傷害,遠攻命中,遠攻傷害";
    private static final RuleKey KEY=new RuleKey(RuleKind.BLESS_ITEM,ModernCharacterData.ItemKind.ARMOR,Long.MAX_VALUE,1);
    private static final String STANDARD="add_str,add_dex,add_int,add_wis,add_con,add_sp,add_mr,add_hpr,add_mpr,add_magic_dmg,add_fire";
    private static final String COMBO="combo_id,require_cards,add_str,add_dex,add_int,add_wis,add_con,add_cha,add_hp,add_mp,add_mr,add_hpr,add_mpr,add_potion_recovery,"
            +"add_magic_dmg,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
            +"add_fire,add_water,add_wind,add_earth,add_hit,add_dmg,add_bow_hit,add_bow_dmg,add_pierce,add_ac,add_er,add_melee_redux,"
            +"add_bow_redux,add_ignore_reduction,add_reduction,add_weight_reduction,add_pvp_redux";
    private static final String SET="set_id,piece_count,add_str,add_dex,add_int,add_wis,add_con,add_cha,add_hp,add_mp,m_def,add_hpr,add_mpr,add_potion_recovery,"
            +"magic_dmg_modifier,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
            +"defense_fire,defense_water,defense_wind,defense_earth,add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,add_pierce,"
            +"damage_reduction,weight_reduction,ac,add_er,add_ignore_reduction,add_pvp_redux";

    @Test void actualQueryProjectsExactLongsBindsKeysAndQualifiesOptionalAbsence() throws Exception {
        var jdbc=new Jdbc();
        var row=numbers(STANDARD); row.put("add_str",4_294_967_299L); row.put("add_fire",Long.MAX_VALUE);
        jdbc.table(BLESS_TABLE,STANDARD,List.of(row));
        Catalog catalog=scoped(()->read(jdbc,Set.of(KEY),Set.of()));
        assertEquals(4_294_967_299L,catalog.require(KEY).raw().get(Stat.STR));
        assertEquals(Long.MAX_VALUE,catalog.require(KEY).raw().get(Stat.FIRE));
        assertEquals(0,catalog.require(KEY).raw().get(Stat.MAX_HP)); // metadata verifies optional add_hp absent
        assertEquals(VipTableState.VERIFIED_ABSENT,catalog.vipTableState());
        var statement=jdbc.statements.stream().filter(s->s.sql.contains("FROM `catalog`.`"+BLESS_TABLE+"`")&&s.sql.endsWith("LIMIT 2")).findFirst().orElseThrow();
        assertEquals(Map.of(1,Long.MAX_VALUE,2,1L),statement.parameters);
        assertFalse(statement.sql.contains(Long.toString(Long.MAX_VALUE)));
        assertTrue(statement.sql.contains("0 AS s"+Stat.MAX_HP.ordinal()));
        assertEquals(2,statement.maxRows);
        jdbc.assertClosedAndBounded();
        assertFalse(jdbc.connectionClosed); // lease/connection ownership stays with outer DAO
    }

    @Test void ruleSetAndComboNumbersAcceptOnlyExactSignedI64IncludingIntegralDecimals() throws Exception {
        var jdbc=new Jdbc();
        var rule=numbers(STANDARD+","+OPTIONAL);rule.put("add_str",new BigDecimal("9223372036854775807.000"));
        rule.put("add_ac",new BigDecimal("-9223372036854775808.00"));jdbc.table(BLESS_TABLE,STANDARD+","+OPTIONAL,List.of(rule));
        var set=numbers(SET);set.put("set_id",new BigDecimal("1.000"));set.put("piece_count",new BigDecimal("1.00"));
        set.put("add_hp",BigInteger.valueOf(Long.MAX_VALUE));jdbc.table(SET_TABLE,SET,List.of(set));
        var combo=numbers(COMBO);combo.put("combo_id",new BigDecimal("4294967299.0"));combo.put("require_cards","10");
        combo.put("add_ac",new BigDecimal("-9223372036854775808.0"));jdbc.table(COMBO_TABLE,COMBO,List.of(combo));
        var result=scoped(()->read(jdbc,Set.of(KEY),Set.of(1L)));
        assertEquals(Long.MAX_VALUE,result.require(KEY).raw().get(Stat.STR));assertEquals(Long.MIN_VALUE,result.require(KEY).raw().get(Stat.AC));
        assertEquals(Long.MAX_VALUE,result.sets().get(0).raw().get(Stat.MAX_HP));
        assertEquals(4_294_967_299L,result.combos().get(0).comboId());assertEquals(Long.MIN_VALUE,result.combos().get(0).raw().get(Stat.AC));
        jdbc.assertClosedAndBounded();
    }

    @Test void ruleSetComboAndMetadataNumbersRejectLossyOrTextReadsWithoutPartialCatalog() {
        for(Object value:List.of(new BigDecimal("1.25"),new BigDecimal("9223372036854775808"),
                new BigInteger("18446744073709551615"),Double.valueOf(1),Float.valueOf(1),"1")) {
            for(String target:List.of("rule","set","combo","metadata")) {
                var jdbc=new Jdbc();var rule=numbers(STANDARD);jdbc.table(BLESS_TABLE,STANDARD,List.of(rule));
                var set=numbers(SET);set.put("set_id",1L);set.put("piece_count",1L);jdbc.table(SET_TABLE,SET,List.of(set));
                var combo=numbers(COMBO);combo.put("combo_id",1L);combo.put("require_cards","10");jdbc.table(COMBO_TABLE,COMBO,List.of(combo));
                switch(target) {
                    case "rule" -> rule.put("add_str",value);
                    case "set" -> set.put("piece_count",value);
                    case "combo" -> combo.put("combo_id",value);
                    case "metadata" -> { var column=metadataColumn("add_str");column.put("COLUMN_NAME_BYTES",value);jdbc.metadata.put(BLESS_TABLE,List.of(column)); }
                    default -> throw new AssertionError();
                }
                SQLException failure=assertThrows(SQLException.class,()->scoped(()->read(jdbc,Set.of(KEY),Set.of(1L))));
                assertEquals("modern equipment catalog is unavailable",failure.getMessage());assertNull(failure.getCause());jdbc.assertClosedAndBounded();
            }
        }
    }

    @Test void absentRowIsDistinctFromUnknownAndMissingRequiredColumnNeverBecomesZero() throws Exception {
        var empty=new Jdbc(); empty.table(BLESS_TABLE,STANDARD,List.of());
        assertFalse(scoped(()->read(empty,Set.of(KEY),Set.of())).require(KEY).present());
        var missing=new Jdbc(); missing.table(BLESS_TABLE,STANDARD.replace("add_str,",""),List.of());
        assertThrows(SQLException.class,()->scoped(()->read(missing,Set.of(KEY),Set.of())));
        assertTrue(missing.statements.stream().noneMatch(s->s.sql.contains("FROM `catalog`.`"+BLESS_TABLE+"`")));
        missing.assertClosedAndBounded();
        var noTable=new Jdbc();
        assertThrows(SQLException.class,()->scoped(()->read(noTable,Set.of(KEY),Set.of())));
    }

    @Test void nullNumericAndDuplicateMatchingRowsAreRejectedWithoutDriverDetails() {
        var nullRow=numbers(STANDARD); nullRow.put("add_str",null);
        var nullJdbc=new Jdbc(); nullJdbc.table(BLESS_TABLE,STANDARD,List.of(nullRow));
        SQLException failure=assertThrows(SQLException.class,()->scoped(()->read(nullJdbc,Set.of(KEY),Set.of())));
        assertEquals("modern equipment catalog is unavailable",failure.getMessage());
        assertNull(failure.getCause()); assertEquals(0,failure.getSuppressed().length);
        nullJdbc.assertClosedAndBounded();
        var duplicate=new Jdbc(); var row=numbers(STANDARD);
        duplicate.table(BLESS_TABLE,STANDARD,List.of(row,row));
        assertThrows(SQLException.class,()->scoped(()->read(duplicate,Set.of(KEY),Set.of())));
        duplicate.assertClosedAndBounded();
    }

    @Test void retainedFireColumnMismatchIsExplicitlyUnreadNotAccidentallyReadAsDefFire() throws Exception {
        var key=new RuleKey(RuleKind.BLESS_TYPE,ModernCharacterData.ItemKind.ARMOR,2,1);
        var jdbc=new Jdbc(); var cols=STANDARD.replace(",add_fire","")+",def_fire";
        var row=numbers(cols); row.put("def_fire",9999L);
        jdbc.table("裝備_armor_bless_類型",cols,List.of(row));
        assertEquals(0,scoped(()->read(jdbc,Set.of(key),Set.of())).require(key).raw().get(Stat.FIRE));
        var stmt=jdbc.statements.stream().filter(s->s.sql.contains("FROM `catalog`.`裝備_armor_bless_類型`")&&s.sql.endsWith("LIMIT 2")).findFirst().orElseThrow();
        assertFalse(stmt.sql.contains("def_fire")); assertFalse(stmt.sql.contains("`add_fire`"));
    }

    @Test void setAndComboRowsKeepI64KeysRawSignsAndAllThresholds() throws Exception {
        var jdbc=new Jdbc(); long id=0x1_0000_0000L;
        var first=numbers(SET); first.put("set_id",id); first.put("piece_count",1L); first.put("ac",9L);
        var second=new HashMap<>(first); second.put("piece_count",3L); second.put("add_hp",5_000_000_000L);
        jdbc.table(SET_TABLE,SET,List.of(first,second));
        var combo=numbers(COMBO); combo.put("combo_id",Long.MAX_VALUE); combo.put("require_cards","4294967296, 4294967297,4294967296"); combo.put("add_ac",7L);
        jdbc.table(COMBO_TABLE,COMBO,List.of(combo));
        var result=scoped(()->read(jdbc,Set.of(),Set.of(id)));
        assertEquals(2,result.sets().size()); assertEquals(Set.of(id),result.verifiedSetIds());
        assertEquals(5_000_000_000L,result.sets().get(1).raw().get(Stat.MAX_HP));
        assertEquals(9,result.sets().get(0).raw().get(Stat.AC));
        assertEquals(Long.MAX_VALUE,result.combos().get(0).comboId());
        assertEquals(Set.of(4294967296L,4294967297L),result.combos().get(0).requiredCards());
        assertEquals(7,result.combos().get(0).raw().get(Stat.AC)); // evaluator, not JDBC, applies source combo sign
        jdbc.assertClosedAndBounded();
    }

    @Test void duplicateThresholdWrongSetIdentityAndMalformedComboNeverBecomeEmptySuccess() {
        var duplicate=new Jdbc(); var set=numbers(SET); set.put("set_id",1L); set.put("piece_count",1L);
        duplicate.table(SET_TABLE,SET,List.of(set,set));
        assertThrows(SQLException.class,()->scoped(()->read(duplicate,Set.of(),Set.of(1L))));
        var wrong=new Jdbc(); wrong.table(SET_TABLE,SET,List.of(set));
        assertThrows(SQLException.class,()->scoped(()->read(wrong,Set.of(),Set.of(2L))));
        for(String text:List.of("", "10,", "10,no", "1", "-2", "9223372036854775808", "2,".repeat(64)+"3", "2".repeat(4097))) {
            var jdbc=new Jdbc(); var combo=numbers(COMBO); combo.put("combo_id",1L); combo.put("require_cards",text);
            jdbc.table(COMBO_TABLE,COMBO,List.of(combo));
            assertThrows(SQLException.class,()->scoped(()->read(jdbc,Set.of(),Set.of())));
        }
    }

    @Test void full4096ComboCatalogWorksAnd4097RowsCannotBeTruncatedIntoSuccess() throws Exception {
        var rows=new ArrayList<Map<String,Object>>();
        for(int i=1;i<=4096;i++){var row=numbers(COMBO);row.put("combo_id",(long)i);row.put("require_cards","10");rows.add(row);}
        var jdbc=new Jdbc(); jdbc.table(COMBO_TABLE,COMBO,rows);
        assertEquals(4096,scoped(()->read(jdbc,Set.of(),Set.of())).combos().size());
        var extra=numbers(COMBO);extra.put("combo_id",4097L);extra.put("require_cards","10");rows.add(extra);
        var tooMany=new Jdbc();tooMany.table(COMBO_TABLE,COMBO,rows);
        assertThrows(SQLException.class,()->scoped(()->read(tooMany,Set.of(),Set.of())));
        tooMany.assertClosedAndBounded();
    }

    @Test void cancellationInsideDataReadPreservesCancellationAndClosesOwnedStatements() {
        var jdbc=new Jdbc();jdbc.table(BLESS_TABLE,STANDARD,List.of(numbers(STANDARD)));
        jdbc.cancelOnData=true;
        var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->scoped(()->read(jdbc,Set.of(KEY),Set.of())));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());
        jdbc.assertClosedAndBounded();
    }

    @Test void invalidIdentifierWritableConnectionAndDriverFailureFailBeforePublishingCatalog() {
        var jdbc=new Jdbc();
        assertThrows(SQLException.class,()->scoped(()->ModernEquipmentCatalogJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,
                "catalog`;DROP TABLE x",Set.of(),Set.of())));
        assertTrue(jdbc.statements.isEmpty());
        jdbc.readOnly=false;
        assertThrows(SQLException.class,()->scoped(()->read(jdbc,Set.of(),Set.of())));
        assertTrue(jdbc.statements.isEmpty());
        jdbc.readOnly=true;jdbc.driverFailure=true;
        var error=assertThrows(SQLException.class,()->scoped(()->read(jdbc,Set.of(),Set.of())));
        assertEquals("modern equipment catalog is unavailable",error.getMessage());
        assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
    }

    @Test void vipTableAbsenceNeedsExactTableGuardAndPresentTableReadsRealValues() throws Exception {
        Jdbc missing=new Jdbc();
        assertEquals(VipTableState.VERIFIED_ABSENT,scoped(()->read(missing,Set.of(),Set.of())).vipTableState());
        var proof=missing.statements.stream().filter(statement->statement.guard).toList();
        assertEquals(1,proof.size());assertEquals("SELECT 1 FROM `catalog`.`"+VIP_TABLE+"` LIMIT 0",proof.get(0).sql);
        assertNull(proof.get(0).guardColumn);missing.assertClosedAndBounded();
        Jdbc present=new Jdbc();var row=numbers(VIP);row.put("力量",Long.MAX_VALUE);
        present.table(VIP_TABLE,VIP,List.of(row));
        var key=new RuleKey(RuleKind.VIP,ModernCharacterData.ItemKind.ETC,5,0);
        var result=scoped(()->read(present,Set.of(key),Set.of()));
        assertEquals(VipTableState.VERIFIED_PRESENT,result.vipTableState());assertEquals(Long.MAX_VALUE,result.require(key).raw().get(Stat.STR));
        assertTrue(present.statements.stream().noneMatch(statement->statement.guard));present.assertClosedAndBounded();
    }

    @Test void all27OptionalColumnGuardsAreOncePerReaderTableNotPerLookupOrGlobal() throws Exception {
        Jdbc jdbc=new Jdbc();jdbc.table(BLESS_TABLE,STANDARD,List.of(numbers(STANDARD)));
        Set<RuleKey> keys=Set.of(KEY,new RuleKey(RuleKind.BLESS_ITEM,ModernCharacterData.ItemKind.ARMOR,10,1),
                new RuleKey(RuleKind.BLESS_ITEM,ModernCharacterData.ItemKind.ARMOR,20,1));
        var first=scoped(()->read(jdbc,keys,Set.of()));assertEquals(3,first.lookups().size());
        var proofs=jdbc.statements.stream().filter(statement->statement.guard&&BLESS_TABLE.equals(statement.guardTable)).toList();
        assertEquals(27,proofs.size());assertEquals(Set.of(OPTIONAL.split(",")),proofs.stream().map(statement->statement.guardColumn).collect(java.util.stream.Collectors.toSet()));
        assertEquals(27,proofs.stream().map(statement->statement.sql).distinct().count());
        assertEquals(3,jdbc.statements.stream().filter(statement->!statement.guard&&statement.sql.contains("FROM `catalog`.`"+BLESS_TABLE+"`")).count());
        scoped(()->read(jdbc,keys,Set.of()));
        assertEquals(54,jdbc.statements.stream().filter(statement->statement.guard&&BLESS_TABLE.equals(statement.guardTable)).count());
        jdbc.assertClosedAndBounded();
    }

    @Test void uppercaseMetadataPreservesOptionalValuesAndPresentNullCannotBecomeZero() throws Exception {
        Jdbc jdbc=new Jdbc();String columns=STANDARD+","+OPTIONAL;var row=numbers(columns);row.put("add_hp",Long.MAX_VALUE);
        jdbc.table(BLESS_TABLE,columns,List.of(row));
        jdbc.metadata.put(BLESS_TABLE,List.of(columns.split(",")).stream().map(column->metadataColumn(column.toUpperCase(java.util.Locale.ROOT))).toList());
        var result=scoped(()->read(jdbc,Set.of(KEY),Set.of()));assertEquals(Long.MAX_VALUE,result.require(KEY).raw().get(Stat.MAX_HP));
        assertTrue(jdbc.statements.stream().noneMatch(statement->statement.guard&&BLESS_TABLE.equals(statement.guardTable)));jdbc.assertClosedAndBounded();
        Jdbc nullValue=new Jdbc();var nullable=new HashMap<>(row);nullable.put("add_hp",null);nullValue.table(BLESS_TABLE,columns,List.of(nullable));
        assertThrows(SQLException.class,()->scoped(()->read(nullValue,Set.of(KEY),Set.of())));nullValue.assertClosedAndBounded();
    }

    @Test void tableAndColumnProofsRejectPermissionWrongVendorStateAndWrongElementErrors() {
        for(boolean table:List.of(false,true))for(String mode:List.of("permission","wrong-vendor","wrong-state","other-element")) {
            Jdbc jdbc=guardFixture(table,mode);SQLException failure=assertThrows(SQLException.class,()->scoped(()->read(jdbc,table?Set.of():Set.of(KEY),Set.of())));
            assertEquals("modern equipment catalog is unavailable",failure.getMessage());assertNull(failure.getCause());assertEquals(0,failure.getSuppressed().length);
            assertTrue(jdbc.statements.stream().noneMatch(statement->!statement.guard&&statement.sql.contains(" FROM `catalog`.")));
            jdbc.assertClosedAndBounded();
        }
    }

    @Test void metadataHiddenButSelectableTableOrColumnFailsRatherThanSwitchingProjection() {
        for(boolean table:List.of(false,true))for(String mode:List.of("success","unexpected-row")) {
            Jdbc jdbc=guardFixture(table,mode);
            assertThrows(SQLException.class,()->scoped(()->read(jdbc,table?Set.of():Set.of(KEY),Set.of())));
            var proof=jdbc.statements.stream().filter(statement->statement.guard&&statement.mode.equals(mode)).findFirst().orElseThrow();
            assertTrue(proof.resultOpened);assertTrue(proof.resultClosed);jdbc.assertClosedAndBounded();
        }
    }

    @Test void setupOrCleanupErrorsAndSuppressedFailuresNeverProveTableOrColumnAbsent() {
        for(boolean table:List.of(false,true))for(String mode:List.of("setup-absence","close-absence","result-close-absence","absence-close","suppressed")) {
            Jdbc jdbc=guardFixture(table,mode);
            SQLException failure=assertThrows(SQLException.class,()->scoped(()->read(jdbc,table?Set.of():Set.of(KEY),Set.of())));
            assertNull(failure.getCause());assertEquals(0,failure.getSuppressed().length);jdbc.assertClosedAndBounded();
        }
    }

    @Test void exactQueryPrepareErrorsCanProveAbsenceWithoutInventingCloseableResource() throws Exception {
        for(boolean table:List.of(false,true)) {
            Jdbc jdbc=guardFixture(table,"prepare-absence");
            var result=scoped(()->read(jdbc,table?Set.of():Set.of(KEY),Set.of()));
            assertEquals(VipTableState.VERIFIED_ABSENT,result.vipTableState());
            var prepared=jdbc.statements.stream().filter(statement->statement.mode.equals("prepare-absence")).findFirst().orElseThrow();
            assertFalse(prepared.opened);assertFalse(prepared.closed);jdbc.assertClosedAndBounded();
        }
    }

    @Test void metadataNamesAreBoundedCaseInsensitiveAndMalformedMetadataNeverFallsBack() throws Exception {
        var nullName=metadataColumn("item");nullName.put("COLUMN_NAME",null);
        var nullLength=metadataColumn("item");nullLength.put("COLUMN_NAME_BYTES",null);
        var wrongLength=metadataColumn("item");wrongLength.put("COLUMN_NAME_BYTES",5L);
        for(List<Map<String,Object>> malformed:List.of(List.of(nullName),List.of(nullLength),List.of(wrongLength),
                List.of(metadataColumn("item"),metadataColumn("ITEM")),List.of(metadataColumn("bad\ncolumn")),
                List.of(metadataColumn("\ud800")),List.of(metadataColumn(" item")),List.of(metadataColumn("x".repeat(257))))) {
            Jdbc jdbc=new Jdbc();jdbc.metadata.put(VIP_TABLE,malformed);
            assertThrows(SQLException.class,()->scoped(()->read(jdbc,Set.of(),Set.of())));
            assertTrue(jdbc.statements.stream().noneMatch(statement->statement.guard));jdbc.assertClosedAndBounded();
        }
        var columns=new ArrayList<Map<String,Object>>();for(int index=0;index<256;index++)columns.add(metadataColumn("column_"+index));
        Jdbc full=new Jdbc();full.metadata.put(VIP_TABLE,List.copyOf(columns));
        assertEquals(VipTableState.VERIFIED_PRESENT,scoped(()->read(full,Set.of(),Set.of())).vipTableState());full.assertClosedAndBounded();
        columns.add(metadataColumn("column_256"));Jdbc overflow=new Jdbc();overflow.metadata.put(VIP_TABLE,List.copyOf(columns));
        assertThrows(SQLException.class,()->scoped(()->read(overflow,Set.of(),Set.of())));overflow.assertClosedAndBounded();
        for(var statement:full.statements)if(statement.sql.contains("INFORMATION_SCHEMA")){
            assertEquals(257,statement.maxRows);assertTrue(statement.sql.contains("LEFT(COLUMN_NAME,257)"));assertTrue(statement.sql.contains("OCTET_LENGTH(COLUMN_NAME)"));
            assertEquals("catalog",statement.parameters.get(1));
        }
    }

    @Test void guardCancellationSharesTheOwnerScopeAndNeverReturnsPartialCatalog() {
        for(boolean table:List.of(false,true)) {
            Jdbc jdbc=guardFixture(table,"cancel");
            var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->scoped(()->read(jdbc,table?Set.of():Set.of(KEY),Set.of())));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());jdbc.assertClosedAndBounded();
        }
    }

    @Test void missingRequiredFieldsAreNeverOfferedAnOptionalProbeEvenWhenMetadataIsFiltered() {
        Jdbc jdbc=new Jdbc();jdbc.table(BLESS_TABLE,STANDARD.replace("add_str,",""),List.of());
        assertThrows(SQLException.class,()->scoped(()->read(jdbc,Set.of(KEY),Set.of())));
        assertTrue(jdbc.statements.stream().noneMatch(statement->statement.guard&&BLESS_TABLE.equals(statement.guardTable)));
        jdbc.assertClosedAndBounded();
    }

    private static Jdbc guardFixture(boolean table,String mode) {
        Jdbc jdbc=new Jdbc();jdbc.table(BLESS_TABLE,STANDARD,List.of(numbers(STANDARD)));
        jdbc.guardModes.put(guardKey(table?VIP_TABLE:BLESS_TABLE,table?null:"add_cha"),mode);return jdbc;
    }
    private static String guardKey(String table,String column){return table+"#"+(column==null?"TABLE":column);}
    private static Map<String,Object> metadataColumn(String name){
        var row=new HashMap<String,Object>();row.put("COLUMN_NAME",name);row.put("COLUMN_NAME_BYTES",(long)name.getBytes(StandardCharsets.UTF_8).length);return row;
    }

    private static Catalog read(Jdbc jdbc,Set<RuleKey> keys,Set<Long> sets)throws Exception{
        return ModernEquipmentCatalogJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"catalog",keys,sets);
    }
    private static <T>T scoped(Callable<T> work)throws Exception{
        var scope=new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(30),new ModernBackendScope.AbortScheduler(){
            public boolean execute(Runnable action){action.run();return true;}public void remove(Runnable action){}
        });
        ModernBackendScope.bind(scope);
        try{return work.call();}finally{scope.finish();ModernBackendScope.unbind();}
    }
    private static Map<String,Object> numbers(String columns){
        Map<String,Object> row=new HashMap<>();for(String column:columns.split(","))row.put(column,0L);return row;
    }
    private static final class Jdbc{
        final Map<String,Set<String>> schema=new HashMap<>();final Map<String,List<Map<String,Object>>> rows=new HashMap<>();
        final Map<String,List<Map<String,Object>>> metadata=new HashMap<>();final Map<String,String> guardModes=new HashMap<>();
        final List<Statement> statements=new ArrayList<>();boolean readOnly=true,connectionClosed,cancelOnData,driverFailure;
        Jdbc(){table(COMBO_TABLE,COMBO,List.of());}
        void table(String name,String columns,List<Map<String,Object>> rows){schema.put(name,Set.of(columns.split(",")));this.rows.put(name,List.copyOf(rows));}
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "isReadOnly"->readOnly;case "close"->{connectionClosed=true;yield null;}
            case "prepareStatement"->{if(driverFailure)throw new SQLException("SYNTHETIC-SECRET jdbc://fixture/password=value");
                var statement=new Statement((String)a[0]);statements.add(statement);
                if(statement.guard&&statement.mode.equals("prepare-absence"))throw statement.absence();yield statement.proxy();}
            case "toString"->"MockReadOnlyCatalogConnection";default->throw new UnsupportedOperationException(m.getName());
        });}
        void assertClosedAndBounded(){for(var s:statements){if(!s.opened){assertTrue(s.guard);assertEquals("prepare-absence",s.mode);continue;}
            assertTrue(s.closed,s.sql);if(s.resultOpened)assertTrue(s.resultClosed,s.sql);
            if(!s.mode.equals("setup-absence"))assertTrue(s.timeout>0&&s.timeout<=30);
            assertTrue(s.maxRows>0&&s.maxRows<=4097);assertTrue(s.sql.startsWith("SELECT "));if(s.guard)assertEquals(1,s.maxRows);}
            assertFalse(connectionClosed);}
        final class Statement{
            final String sql;final Map<Integer,Object> parameters=new LinkedHashMap<>();final boolean guard;final String guardTable,guardColumn,mode;
            int maxRows,timeout;boolean opened,closed,resultOpened,resultClosed;
            Statement(String sql){this.sql=sql;
                var match=Pattern.compile("^SELECT (1|`([^`]+)`) FROM `catalog`\\.`([^`]+)` LIMIT 0$").matcher(sql);
                guard=match.matches();guardTable=guard?match.group(3):null;guardColumn=guard?match.group(2):null;
                mode=guard?guardModes.getOrDefault(guardKey(guardTable,guardColumn),"absence"):"data";
            }
            SQLException absence(){return new SQLException("SYNTHETIC-SECRET absence",guardColumn==null?"42S02":"42S22",guardColumn==null?1146:1054);}
            PreparedStatement proxy(){opened=true;return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()){
                case "setString","setLong"->{parameters.put((Integer)a[0],a[1]);yield null;}
                case "setMaxRows"->{maxRows=(Integer)a[0];if(mode.equals("setup-absence"))throw absence();yield null;}case "setQueryTimeout"->{timeout=(Integer)a[0];yield null;}
                case "close"->{closed=true;if(mode.equals("absence-close")||mode.equals("close-absence"))throw absence();yield null;}
                case "executeQuery"->{if(guard){switch(mode){
                    case "absence","absence-close"->throw absence();
                    case "permission"->throw new SQLException("SYNTHETIC-SECRET permission","42000",1142);
                    case "wrong-vendor"->throw new SQLException("SYNTHETIC-SECRET vendor",guardColumn==null?"42S02":"42S22",9999);
                    case "wrong-state"->throw new SQLException("SYNTHETIC-SECRET state","42000",guardColumn==null?1146:1054);
                    case "other-element"->throw new SQLException("SYNTHETIC-SECRET other",guardColumn==null?"42S22":"42S02",guardColumn==null?1054:1146);
                    case "suppressed"->{var failure=absence();failure.addSuppressed(new SQLException("SYNTHETIC-SECRET cleanup"));throw failure;}
                    case "cancel"->{ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);throw absence();}
                    default->{ }
                }}yield result();}
                default->throw new UnsupportedOperationException(m.getName());
            });}
            ResultSet result(){
                resultOpened=true;
                List<Map<String,Object>> output=new ArrayList<>();
                if(guard){if(mode.equals("unexpected-row"))output.add(Map.of("unexpected",1L));}
                else if(sql.contains("INFORMATION_SCHEMA")){
                    assertEquals("catalog",parameters.get(1));
                    String table=(String)parameters.get(2);
                    if(metadata.containsKey(table))output.addAll(metadata.get(table));
                    else for(String column:schema.getOrDefault(table,Set.of()).stream().sorted().toList())output.add(metadataColumn(column));
                }else{
                    var table=Pattern.compile("FROM `catalog`\\.`([^`]+)`").matcher(sql);assertTrue(table.find(),sql);
                    for(var source:rows.getOrDefault(table.group(1),List.of())){
                        var projected=new HashMap<>(source);
                        var projection=Pattern.compile("(`([^`]+)`|0) AS s([0-9]+)").matcher(sql);
                        while(projection.find())projected.put("s"+projection.group(3),projection.group(1).equals("0")?0L:source.get(projection.group(2)));
                        if(source.containsKey("require_cards")){Object text=source.get("require_cards");projected.put("require_cards_bytes",text==null?null:(long)((String)text).getBytes(java.nio.charset.StandardCharsets.UTF_8).length);}
                        output.add(projected);
                    }
                    if(cancelOnData)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
                }
                final int[] index={-1};final boolean[] wasNull={false};
                var columns=new ModernJdbcNumbersTest.Columns(column->output.get(index[0]).get(column));
                return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                    case "next"->++index[0]<output.size();case "close"->{resultClosed=true;if(mode.equals("result-close-absence"))throw absence();yield null;}case "wasNull"->wasNull[0];
                    case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();
                    case "getString","getLong","getBigDecimal"->{String column=columns.name(a[0]);Object value=output.get(index[0]).get(column);wasNull[0]=value==null;
                        if(m.getName().equals("getString"))yield value==null?null:value.toString();
                        if(m.getName().equals("getBigDecimal"))yield ModernJdbcNumbersTest.Columns.decimal(value);yield value==null?0L:((Number)value).longValue();}
                    default->throw new UnsupportedOperationException(m.getName());
                });
            }
        }
    }
}
