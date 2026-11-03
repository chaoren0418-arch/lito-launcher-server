package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import java.sql.*;
import static local.lito381.server.ModernDisintegrateCatalogTest.proxy;

final class ModernSlowCatalogTest {
    static Map<String, Object> row() {
        // Independent selected row29 literal; never read the implementation's arrays.
        String[] fields = ("objid learned_owner learned_id template_id skill_level skill_number mp_cost hp_cost item_id item_count reuse_millis "
                + "buff_duration target_to damage_value damage_dice damage_dice_count probability_value probability_dice attr spell_type lawful "
                + "maximum_range area through_flag template_action action_id castgfx castgfx2 sysmsg_happen sysmsg_stop sysmsg_fail effect_skill_id target_bytes").split(" ");
        long[] values = {3333,3333,29,29,4,4,20,0,40318,1,500,64,3,0,0,0,0,12,8,1,0,-1,0,0,16,19,752,0,277,0,280,0,4};
        assertEquals(fields.length, values.length);
        var result = new HashMap<String, Object>();
        for (int i = 0; i < fields.length; i++) result.put(fields[i], values[i]);
        result.put("access_level", 0L); result.put("target", "buff"); return result;
    }
    static Optional<ModernSlowCatalog.Snapshot> read(Jdbc jdbc, int source, String database) throws Exception {
        var scope = ModernDisintegrateCatalogTest.scope(); ModernBackendScope.bind(scope);
        try { return ModernSlowCatalog.read(jdbc.connection(), scope, source, "owner", 3333, database); }
        finally { scope.finish(); ModernBackendScope.unbind(); }
    }
    static Optional<ModernSlowCatalog.Snapshot> read(Jdbc jdbc) throws Exception { return read(jdbc, 7, "catalog"); }

    @Test void exactSourceOwnedTupleHasBoundedSelectsAndDoesNotAliasIdenticalObjectIds() throws Exception {
        var jdbc = new Jdbc(List.of(row())); var result = read(jdbc).orElseThrow();
        assertEquals(ModernSlowCatalog.Status.QUALIFIED, result.status());
        assertEquals(List.of(1,2,4,2,2), jdbc.limits);
        assertEquals(5, jdbc.closedStatements); assertEquals(5, jdbc.closedResults);
        assertEquals(Map.of(1,3333L,2,"owner"), jdbc.parameters);
        assertTrue(jdbc.queries.get(1).contains("cs.skill_id=29"));
        assertTrue(jdbc.queries.get(1).contains("`catalog`.`skills`"));
        assertEquals(ModernSlowCatalog.extrasSql("catalog"), jdbc.queries.get(2));
        assertTrue(jdbc.timeouts.stream().allMatch(n -> n > 0 && n <= 10));
        var sun = new ModernSlow.Profile(new TournamentPresence.CharacterKey(7,3333),0,20);
        var moon = new ModernSlow.Profile(new TournamentPresence.CharacterKey(42,3333),0,20);
        assertTrue(result.accepts(sun)); assertFalse(result.accepts(moon)); assertFalse(result.accepts(null));
        var other = new Jdbc(List.of(row()));
        assertTrue(read(other,42,"moon_catalog").orElseThrow().accepts(moon));
        assertTrue(other.queries.get(1).contains("`moon_catalog`.`skills`"));
        assertFalse(result.toString().contains("3333"));
    }
    @Test void everyTupleDifferenceAndLossyNumberRejectsWithoutDefaulting() throws Exception {
        for (String field : row().keySet()) if (!Set.of("objid","learned_owner","learned_id","template_id","target","target_bytes","access_level").contains(field)) {
            var changed = row(); changed.put(field, ((Number) changed.get(field)).longValue() + 1);
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE, read(new Jdbc(List.of(changed))).orElseThrow().status(), field);
            for (Object invalid : new Object[]{null, new BigDecimal("0.5"), new BigDecimal("9223372036854775808"), 1.0, "1"}) {
                var bad = row(); bad.put(field, invalid);
                assertThrows(SQLException.class, () -> read(new Jdbc(List.of(bad))), field);
            }
        }
        var changed = row(); changed.put("target", "self");
        assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE, read(new Jdbc(List.of(changed))).orElseThrow().status());
    }
    @Test void accessIsExactSourceOwnedSignedI64EvenForUnlearnedCharacters() throws Exception {
        for(long access:new long[]{Long.MIN_VALUE,-1,0,100,199,200,201,4294967296L,Long.MAX_VALUE}) {
            var row=row();row.put("access_level",access);
            var snapshot=read(new Jdbc(List.of(row))).orElseThrow();
            assertEquals(access,snapshot.accessLevel().orElseThrow());assertEquals(access>=200,snapshot.gmTarget());
            var unlearned=read(new Jdbc(List.of(Map.of("objid",3333L,"access_level",access)))).orElseThrow();
            assertEquals(ModernSlowCatalog.Status.UNLEARNED,unlearned.status());assertEquals(access>=200,unlearned.gmTarget());
        }
        for(Object bad:new Object[]{null,"200",200.0,new BigDecimal("200.1"),new BigDecimal("9223372036854775808")}) {
            var row=row();row.put("access_level",bad);assertThrows(SQLException.class,()->read(new Jdbc(List.of(row))));
        }
        var unknown=new ModernSlowCatalog.Snapshot(new TournamentPresence.CharacterKey(7,3333),ModernSlowCatalog.Status.QUALIFIED);
        assertThrows(IllegalArgumentException.class,unknown::gmTarget);
    }
    @Test void ownerTemplateDuplicatesAndUnlearnedAreDistinct() throws Exception {
        for (String field : List.of("objid","learned_owner","learned_id","template_id")) for (Object bad : new Object[]{null,42L}) {
            var changed = row(); changed.put(field,bad);
            assertThrows(SQLException.class, () -> read(new Jdbc(List.of(changed))));
        }
        assertThrows(SQLException.class, () -> read(new Jdbc(List.of(row(),row()))));
        assertTrue(read(new Jdbc(List.of())).isEmpty());
        var unlearned = new Jdbc(List.of(Map.of("objid",3333L,"access_level",0L)));
        assertEquals(ModernSlowCatalog.Status.UNLEARNED, read(unlearned).orElseThrow().status());
        assertEquals(2, unlearned.queries.size());
    }
    @Test void materialExtraBuffCounterMagicRulesMissingRowsAndUnknownColumnErrorsNeverBecomeDefaults() throws Exception {
        for (var extras : List.of(List.<Map<String,Object>>of(), List.of(Jdbc.EMPTY_ITEM,Jdbc.EMPTY_ITEM),
                List.of(Map.<String,Object>of("extension_kind",0L,"ids_null",0L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",1L,"ids_null",1L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",2L,"ids_null",1L,"counts_null",1L)))) {
            var jdbc = new Jdbc(List.of(row())); jdbc.extras = extras;
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE, read(jdbc).orElseThrow().status());
        }
        var absent = new Jdbc(List.of(row())); absent.guardFailure = new SQLException("private","42S22",1054);
        assertEquals(ModernSlowCatalog.Status.QUALIFIED, read(absent).orElseThrow().status());
        assertTrue(absent.queries.get(1).contains("0 AS effect_skill_id"));
        var denied = new Jdbc(List.of(row())); denied.guardFailure = new SQLException("private","42000",1142);
        var error = assertThrows(SQLException.class, () -> read(denied));
        assertEquals("modern slow source unavailable", error.getMessage()); assertNull(error.getCause());
    }
    @Test void cancellationAtQueriesIncludingFinalNextAndInvalidIdentifiersCannotPublishPartialQualification() throws Exception {
        for (int query = 1; query <= 5; query++) for (int next = 1; next <= 2; next++) {
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

    static Map<String,Object> negativeRow() {
        var r = new HashMap<String,Object>();
        r.put("skill_id",29L); r.put("prob_high",55L); r.put("prob_equal",45L); r.put("prob_low",35L);
        r.put("check_resist_type",0L); r.put("check_magic_level",0L); r.put("check_magic_hit",1L);
        for (String column:List.of("bonus_str","bonus_dex","con_multiplier","wis_multiplier")) r.put(column,0d);
        r.put("bonus_int",0.3d); r.put("mr_multiplier",0.05d); return r;
    }

    @Test void negativeAndBossPoliciesMustExistExactlyOnceAndMayNotDefaultOnFailure() throws Exception {
        for (String column:negativeRow().keySet()) {
            var changed=negativeRow();
            if(changed.get(column) instanceof Long n) changed.put(column,n+1); else changed.put(column,Math.nextUp((Double)changed.get(column)));
            var jdbc=new Jdbc(List.of(row())); jdbc.negative=List.of(changed);
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,read(jdbc).orElseThrow().status(),column);
            var missing=negativeRow();missing.put(column,null);
            var nullJdbc=new Jdbc(List.of(row()));nullJdbc.negative=List.of(missing);
            assertThrows(SQLException.class,()->read(nullJdbc),column);
        }
        for (var rows:List.of(List.<Map<String,Object>>of(),List.of(negativeRow(),negativeRow()))) {
            var jdbc=new Jdbc(List.of(row()));jdbc.negative=rows;
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,read(jdbc).orElseThrow().status());
        }
        for(var rows:List.of(List.<Map<String,Object>>of(),List.of(Map.<String,Object>of("skill_id",29L,"modifier",0L)),
                List.of(Map.<String,Object>of("skill_id",29L,"modifier",5L),Map.<String,Object>of("skill_id",29L,"modifier",5L)))) {
            var jdbc=new Jdbc(List.of(row()));jdbc.boss=rows;
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,read(jdbc).orElseThrow().status());
        }
        for(int q=2;q<=5;q++) {
            var jdbc=new Jdbc(List.of(row()));jdbc.failQuery=q;
            var error=assertThrows(SQLException.class,()->read(jdbc));
            assertEquals("modern slow source unavailable",error.getMessage());assertNull(error.getCause());
            assertEquals(jdbc.queries.size(),jdbc.closedStatements);
        }
        // An unsupported extension does not hide a denied mandatory rule table.
        var jdbc=new Jdbc(List.of(row()));jdbc.extras=List.of();jdbc.failQuery=4;
        assertThrows(SQLException.class,()->read(jdbc));
    }

    @Test void fractionalCoefficientsUseAnExplicitTypedBoundaryWithoutLossyCastOrTolerance() throws Exception {
        for(String column:List.of("bonus_int","mr_multiplier")) {
            String value=column.equals("bonus_int")?"0.3":"0.05";
            var decimal=negativeRow();decimal.put(column,new BigDecimal(value+"00000000000000000"));
            var good=new Jdbc(List.of(row()));good.negative=List.of(decimal);
            assertEquals(ModernSlowCatalog.Status.QUALIFIED,read(good).orElseThrow().status());
            for(Object bad:new Object[]{value,Float.valueOf(value),Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
                var changed=negativeRow();changed.put(column,bad);
                var jdbc=new Jdbc(List.of(row()));jdbc.negative=List.of(changed);
                assertThrows(SQLException.class,()->read(jdbc));
            }
            var near=negativeRow();near.put(column,new BigDecimal(value).add(new BigDecimal("0.0000000000000000000000000001")));
            var jdbc=new Jdbc(List.of(row()));jdbc.negative=List.of(near);
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,read(jdbc).orElseThrow().status());
        }
        assertFalse(ModernSlowCatalog.negativeSql("catalog").contains("CAST("));
        assertTrue(ModernSlowCatalog.extrasSql("catalog").contains("skills_魔屏無效技能"));
    }
    static final class Jdbc {
        static final Map<String,Object> EMPTY_ITEM=Map.of("extension_kind",0L,"ids_null",1L,"counts_null",1L);
        final List<Map<String,Object>> data;List<Map<String,Object>> extras=List.of(EMPTY_ITEM);
        List<Map<String,Object>> negative=List.of(negativeRow()),boss=List.of(Map.of("skill_id",29L,"modifier",5L));
        int failQuery;
        final List<String> queries=new ArrayList<>();final List<Integer> limits=new ArrayList<>(),timeouts=new ArrayList<>();
        final Map<Integer,Object> parameters=new HashMap<>();int closedStatements,closedResults,closedConnections,cancelQuery,cancelNext;boolean readOnly;SQLException guardFailure;
        Jdbc(List<Map<String,Object>> data){this.data=data;}
        Connection connection(){return proxy(Connection.class,(m,a)->{
            if(m.equals("setReadOnly")){assertEquals(Boolean.TRUE,a[0]);readOnly=true;return null;}
            if(m.equals("setNetworkTimeout")||m.equals("abort"))return null;
            if(m.equals("close")){closedConnections++;return null;}
            if(!m.equals("prepareStatement"))throw new AssertionError("unexpected connection operation "+m);
            String sql=(String)a[0];assertTrue(sql.startsWith("SELECT "));queries.add(sql);int query=queries.size();boolean guard=sql.endsWith(" LIMIT 0");
            return proxy(PreparedStatement.class,(s,b)->switch(s){
                case "setMaxRows"->{limits.add((int)b[0]);yield null;}case "setQueryTimeout"->{timeouts.add((int)b[0]);yield null;}
                case "setLong","setString"->{parameters.put((int)b[0],b[1]);yield null;}
                case "executeQuery"->{if(guard&&guardFailure!=null)throw guardFailure; if(query==failQuery)throw new SQLException("private SQL endpoint account"); yield rows(guard?List.of():sql.contains(" AS extension_kind")?extras:sql.contains("SELECT skill_id,prob_high")?negative:sql.contains("SELECT skill_id,modifier")?boss:data,query);}
                case "close"->{closedStatements++;yield null;}default->throw new AssertionError("unexpected statement operation "+s);
            });
        });}
        ResultSet rows(List<Map<String,Object>> rows,int query) {
            int[] index={-1};boolean[] wasNull={false};var columns=new ModernJdbcNumbersTest.Columns(c->rows.get(index[0]).get(c),Map.of());
            return proxy(ResultSet.class,(m,a)->switch(m){
                case "next"->{index[0]++;if(query==cancelQuery&&index[0]+1==cancelNext)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<rows.size();}
                case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();case "wasNull"->wasNull[0];
                case "getDouble"->{Object v=rows.get(index[0]).get(columns.name(a[0]));wasNull[0]=v==null;yield v==null?0d:((Number)v).doubleValue();}
                case "getString","getBigDecimal"->{Object v=rows.get(index[0]).get(columns.name(a[0]));wasNull[0]=v==null;yield m.equals("getString")?v:ModernJdbcNumbersTest.Columns.decimal(v);}
                case "close"->{closedResults++;yield null;}default->throw new AssertionError("unexpected result operation "+m);
            });
        }
    }
}
