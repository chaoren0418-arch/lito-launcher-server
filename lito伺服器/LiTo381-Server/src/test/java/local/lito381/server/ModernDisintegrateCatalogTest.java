package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

class ModernDisintegrateCatalogTest {
    @TempDir Path temporary;
    @Test void ownedDaoAndRepositoryUseCanonicalSourceWithoutFallbackOrLeakedConnections()throws Exception {
        String text="schema=1\nbind.port=0\ntls.pkcs12="+temporary.resolve("unused.p12").toString().replace('\\','/')
                +"\ntls.password.env=TLS\nsources=7\nsource.7.name=Sun\nsource.7.catalog.database=catalog\n"
                +"source.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\nsource.7.mysql.database=characters_only\n"
                +"source.7.mysql.username.env=USER\nsource.7.mysql.password.env=PASSWORD\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
        try(var config=ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),key->"synthetic")) {
            var sun=new Jdbc(List.of(row()));var moon=new Jdbc(List.of(row()));int[] connections={0};
            var sunDao=new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,(u,p)->{connections[0]++;return sun.connection();});
            var moonDao=new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,(u,p)->{connections[0]++;return moon.connection();});
            var sources=new HashMap<Integer,ModernDisintegrateRepository.Source>();sources.put(7,new ModernDisintegrateRepository.Source(sunDao,"sun_catalog"));sources.put(42,new ModernDisintegrateRepository.Source(moonDao,"moon_catalog"));
            var repository=ModernDisintegrateRepository.jdbc(sources);sources.clear();
            var principal=new ModernAccountAuthentication.Principal("display",Set.of(new ModernAccountAuthentication.Source(7,"Sun"),new ModernAccountAuthentication.Source(42,"Moon")),Map.of(7,"sun-owner",42,"moon-owner"));
            var scope=scope();ModernBackendScope.bind(scope);
            try {
                assertEquals(7,repository.character(principal,7,3333).orElseThrow().key().sourceId());
                assertEquals(42,repository.character(principal,42,3333).orElseThrow().key().sourceId());
                assertEquals(2,connections[0]);assertEquals(1,sun.closedConnections);assertEquals(1,moon.closedConnections);assertTrue(sun.readOnly&&moon.readOnly);
                assertEquals("sun-owner",sun.parameters.get(2));assertEquals("moon-owner",moon.parameters.get(2));
                assertTrue(sun.queries.get(1).contains("`sun_catalog`.`skills`"));assertTrue(moon.queries.get(1).contains("`moon_catalog`.`skills`"));
                assertThrows(SecurityException.class,()->repository.character(principal,9,3333));assertEquals(2,connections[0]);
                var missing=ModernDisintegrateRepository.jdbc(Map.of(7,new ModernDisintegrateRepository.Source(sunDao,"sun_catalog")));
                assertThrows(IllegalStateException.class,()->missing.character(principal,42,3333));assertEquals(2,connections[0]);
                scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
                assertThrows(ModernBackendExecutor.BackendException.class,()->repository.character(principal,7,3333));assertEquals(2,connections[0]);
            }finally{scope.finish();ModernBackendScope.unbind();}
        }
    }
    static Map<String,Object> row() {
        // Independent literal selected SQL row, not the production arrays.
        String[] fields=("objid learned_owner learned_id template_id skill_level skill_number mp_cost hp_cost item_id item_count reuse_millis "
                +"buff_duration target_to damage_value damage_dice damage_dice_count probability_value probability_dice attr spell_type lawful "
                +"maximum_range area through_flag template_action action_id castgfx castgfx2 sysmsg_happen sysmsg_stop sysmsg_fail effect_skill_id target_bytes").split(" ");
        long[] values={3333,3333,77,77,10,4,70,0,0,0,5000,0,3,190,15,8,0,0,16,64,1,10,0,0,16,18,11746,0,0,0,0,0,6};
        assertEquals(fields.length,values.length);var row=new HashMap<String,Object>();for(int i=0;i<fields.length;i++)row.put(fields[i],values[i]);row.put("target","attack");return row;
    }
    static ModernBackendScope scope() {
        return new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),new ModernBackendScope.AbortScheduler(){
            public boolean execute(Runnable r){r.run();return true;}public void remove(Runnable r){}
        });
    }
    static Optional<ModernDisintegrateCatalog.Snapshot> read(Jdbc jdbc)throws Exception {
        var scope=scope();ModernBackendScope.bind(scope);try{return ModernDisintegrateCatalog.read(jdbc.connection(),scope,7,"owner",3333,"catalog");}
        finally{scope.finish();ModernBackendScope.unbind();}
    }
    @Test void exactSourceScopedTupleUsesOnlyBoundedSelectsAndClosesEveryResult()throws Exception {
        var jdbc=new Jdbc(List.of(row()));var result=read(jdbc).orElseThrow();
        assertEquals(new TournamentPresence.CharacterKey(7,3333),result.key());assertEquals(ModernDisintegrateCatalog.Status.QUALIFIED,result.status());
        assertEquals(List.of(1,2,4),jdbc.limits);assertEquals(3,jdbc.closedStatements);assertEquals(3,jdbc.closedResults);
        assertEquals(Map.of(1,3333L,2,"owner"),jdbc.parameters);assertTrue(jdbc.queries.get(1).contains("`catalog`.`skills`"));
        assertEquals(ModernDisintegrateCatalog.extrasSql("catalog"),jdbc.queries.get(2));assertTrue(jdbc.timeouts.stream().allMatch(n->n>0&&n<=10));
        assertFalse(result.toString().contains("3333"));
    }
    @Test void everyTupleDifferenceRefusesQualificationAndNullOrLossyNumericNeverDefaults()throws Exception {
        for(String field:row().keySet())if(!Set.of("objid","learned_owner","learned_id","template_id","target","target_bytes").contains(field)) {
            var changed=row();changed.put(field,((Number)changed.get(field)).longValue()+1);
            assertEquals(ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(changed))).orElseThrow().status(),field);
            for(Object invalid:new Object[]{null,new BigDecimal("0.5"),new BigDecimal("9223372036854775808"),1.0,"1"}) {
                var bad=row();bad.put(field,invalid);assertThrows(SQLException.class,()->read(new Jdbc(List.of(bad))),field);
            }
        }
    }
    @Test void unknownOwnerMissingTemplateDuplicateRowsAndUnlearnedAreDistinct()throws Exception {
        for(String field:List.of("objid","learned_owner","learned_id","template_id"))for(Object bad:new Object[]{null,42L}) {
            var r=row();r.put(field,bad);assertThrows(SQLException.class,()->read(new Jdbc(List.of(r))));
        }
        assertThrows(SQLException.class,()->read(new Jdbc(List.of(row(),row()))));assertTrue(read(new Jdbc(List.of())).isEmpty());
        var unlearned=new HashMap<String,Object>();unlearned.put("objid",3333L);
        var j=new Jdbc(List.of(unlearned));assertEquals(ModernDisintegrateCatalog.Status.UNLEARNED,read(j).orElseThrow().status());assertEquals(2,j.queries.size());
    }
    @Test void extensionMissingDuplicateMaterialBuffAndNegativeOverridesNeverBecomeNoOps()throws Exception {
        for(var rows:List.of(List.<Map<String,Object>>of(),List.of(Jdbc.EMPTY_ITEM,Jdbc.EMPTY_ITEM),
                List.of(Map.<String,Object>of("extension_kind",0L,"ids_null",0L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",1L,"ids_null",1L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",2L,"ids_null",1L,"counts_null",1L)))) {
            var j=new Jdbc(List.of(row()));j.extras=rows;assertEquals(ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE,read(j).orElseThrow().status());
        }
        var j=new Jdbc(List.of(row()));j.extras=List.of(Map.of("extension_kind",0L,"ids_null",2L,"counts_null",1L));assertThrows(SQLException.class,()->read(j));
    }
    @Test void absentEffectRequiresOnlyExactUnknownColumnProofAndNoPermissionFallback()throws Exception {
        var absent=new Jdbc(List.of(row()));absent.guardFailure=new SQLException("private","42S22",1054);
        assertEquals(ModernDisintegrateCatalog.Status.QUALIFIED,read(absent).orElseThrow().status());assertTrue(absent.queries.get(1).contains("0 AS effect_skill_id"));
        for(var failure:List.of(new SQLException("private","42000",1142),new SQLException("private","42S22",1054,new Exception("private")))) {
            var j=new Jdbc(List.of(row()));j.guardFailure=failure;
            // A nested cause is not interpreted by the shared guard; use an
            // actual next-exception chain to distinguish ambiguous absence.
            if(failure.getErrorCode()==1054)failure.setNextException(new SQLException("private"));
            var error=assertThrows(SQLException.class,()->read(j));assertEquals("modern disintegrate source unavailable",error.getMessage());assertNull(error.getCause());
        }
    }
    @Test void cancelAtEveryQueryOrLastNextAndInvalidIdentifierRejectWithoutPartialResult()throws Exception {
        for(int query=1;query<=3;query++)for(int next=1;next<=2;next++) {
            var j=new Jdbc(List.of(row()));j.cancelQuery=query;j.cancelNext=next;
            // Guard has a single false next; its hypothetical second next is never reached.
            if(query==1&&next==2)continue;
            assertThrows(ModernBackendExecutor.BackendException.class,()->read(j));assertEquals(j.queries.size(),j.closedStatements);assertEquals(j.queries.size(),j.closedResults);
        }
        for(String db:new String[]{null,"","a;drop", "a".repeat(65)}) {
            var j=new Jdbc(List.of(row()));var scope=scope();
            assertThrows(IllegalArgumentException.class,()->ModernDisintegrateCatalog.read(j.connection(),scope,7,"owner",3333,db));assertTrue(j.queries.isEmpty());scope.finish();
        }
    }
    static final class Jdbc {
        static final Map<String,Object> EMPTY_ITEM=Map.of("extension_kind",0L,"ids_null",1L,"counts_null",1L);
        final List<Map<String,Object>> data;List<Map<String,Object>> extras=List.of(EMPTY_ITEM);
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
                case "executeQuery"->{if(guard&&guardFailure!=null)throw guardFailure;yield rows(guard?List.of():sql.contains(" AS extension_kind")?extras:data,query);}
                case "close"->{closedStatements++;yield null;}default->throw new AssertionError("unexpected statement operation "+s);
            });
        });}
        ResultSet rows(List<Map<String,Object>> rows,int query) {
            int[] index={-1};boolean[] wasNull={false};var columns=new ModernJdbcNumbersTest.Columns(c->rows.get(index[0]).get(c),Map.of());
            return proxy(ResultSet.class,(m,a)->switch(m){
                case "next"->{index[0]++;if(query==cancelQuery&&index[0]+1==cancelNext)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<rows.size();}
                case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();case "wasNull"->wasNull[0];
                case "getString","getBigDecimal"->{Object v=rows.get(index[0]).get(columns.name(a[0]));wasNull[0]=v==null;yield m.equals("getString")?v:ModernJdbcNumbersTest.Columns.decimal(v);}
                case "close"->{closedResults++;yield null;}default->throw new AssertionError("unexpected result operation "+m);
            });
        }
    }
    interface Call {Object run(String method,Object[] args)throws Throwable;}
    static <T>T proxy(Class<T> type,Call call){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.run(m.getName(),a)));}
}
