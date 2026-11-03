package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exact synthetic JDBC; unexpected calls fail without reaching a driver or SQL server. */
class ModernShopCatalogJdbcTest {
    @TempDir Path temporary;
    private ModernServerConfig config() throws Exception {
        var input=new StringBuilder("schema=1\nbind.port=0\ntls.pkcs12=")
                .append(temporary.resolve("listener.p12").toAbsolutePath().toString().replace('\\','/'))
                .append("\ntls.password.env=T\nsources=7,42\n");
        for(int id:List.of(7,42))input.append("source.").append(id).append(".name=Source").append(id).append('\n')
                .append("source.").append(id).append(".catalog.database=catalog_").append(id).append('\n')
                .append("source.").append(id).append(".mysql.host=127.0.0.1\n")
                .append("source.").append(id).append(".mysql.port=3306\n")
                .append("source.").append(id).append(".mysql.database=characters_").append(id).append('\n')
                .append("source.").append(id).append(".mysql.username.env=U\n")
                .append("source.").append(id).append(".mysql.password.env=P\n")
                .append("source.").append(id).append(".mysql.passwordScheme=plain\n")
                .append("source.").append(id).append(".mysql.tls=disabled\n");
        return ModernServerConfig.parse(input.toString().getBytes(StandardCharsets.UTF_8),key->"synthetic");
    }
    @FunctionalInterface private interface Scoped<T>{T call(ModernBackendScope scope)throws Exception;}
    private static <T>T scoped(Scoped<T> work)throws Exception {
        var scope=new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),new ModernBackendScope.AbortScheduler(){
            public boolean execute(Runnable action){action.run();return true;}public void remove(Runnable action){}
        });ModernBackendScope.bind(scope);
        try{return work.call(scope);}finally{scope.finish();ModernBackendScope.unbind();}
    }
    private static ModernJdbcBackend dao(ModernServerConfig.SourceConfig source,Jdbc jdbc)throws Exception {
        return new ModernJdbcBackend(source.database(),1000,3000,jdbc);
    }
    private ModernShopCatalog.Snapshot read(Jdbc jdbc)throws Exception {
        try(var config=config()){return scoped(scope->dao(config.sources().get(0),jdbc).shopCharacter(7,"owner",3333,"catalog_7")).orElseThrow();}
    }
    private ModernShopCatalog.Snapshot readArrows(Jdbc jdbc)throws Exception {
        try(var config=config()){return scoped(scope->dao(config.sources().get(0),jdbc).shopCharacter(7,"owner",3333,"catalog_7",true)).orElseThrow();}
    }
    private static Map<String,Object> arrowRow(long id,int sort) {
        var row=row(id,sort);var t=ModernCombatTestPeer.arrow(id,sort,20).template();
        putText(row,"e_name",t.nameToken());row.put("e_invgfx",96L);row.put("e_grdgfx",66L);row.put("e_weight",47L);
        putText(row,"executor","0");putText(row,"item_type","arrow");putText(row,"use_type","arrow");putText(row,"material",t.etc().material());
        row.put("delay_id",0L);row.put("delay_time",0L);row.put("ammo_id",id);row.put("ammo_small",t.arrow().small());
        row.put("ammo_large",t.arrow().large());row.put("ammo_sprite",-1L);row.put("ammo_consume",1L);return row;
    }
    @Test void optInArrowJoinKeepsOneReadonlyStatementAndBothSourceDice()throws Exception {
        var jdbc=new Jdbc(List.of(arrowRow(40744,0),arrowRow(40746,1)));var result=readArrows(jdbc);
        assertEquals(List.of(7L,10L),result.offers().stream().map(o->o.ammo().small()).toList());
        assertEquals(List.of(6L,9L),result.offers().stream().map(o->o.ammo().large()).toList());
        assertEquals(List.of(ModernShopCatalogJdbc.sql("catalog_7",true)),jdbc.sql);assertEquals(1,jdbc.connections);assertEquals(1,jdbc.connectionCloses);
        assertEquals(Map.of(1,"owner",2,3333L),jdbc.parameters);assertTrue(jdbc.readOnly);
        assertFalse(ModernShopCatalogJdbc.sql("catalog_7").contains("etcitem_arrow"));
    }
    @Test void arrowRequiredJoinCoverageDuplicateAndExactNumericFailuresNeverGrant()throws Exception {
        for(String field:List.of("ammo_id","ammo_small","ammo_large","ammo_sprite","ammo_consume"))for(Object invalid:new Object[]{null,new BigDecimal("1.1"),1.0,"1"}) {
            var row=arrowRow(40744,0);row.put(field,invalid);var jdbc=new Jdbc(List.of(row));
            assertThrows(SQLException.class,()->readArrows(jdbc));assertEquals(1,jdbc.connectionCloses);assertEquals(1,jdbc.statementCloses);
        }
        assertThrows(SQLException.class,()->readArrows(new Jdbc(List.of(arrowRow(40744,0),arrowRow(40744,0)))));
        var wrong=arrowRow(40744,0);wrong.put("ammo_id",40746L);assertThrows(SQLException.class,()->readArrows(new Jdbc(List.of(wrong))));
        var wide=arrowRow(40744,0);wide.put("ammo_small",Long.MAX_VALUE);assertEquals(Long.MAX_VALUE,readArrows(new Jdbc(List.of(wide))).offers().get(0).ammo().small());
    }
    @Test void exactSelectedOwnerCatalogAndOneBoundedReadReturnQualifiedDefinitions()throws Exception {
        var jdbc=new Jdbc(List.of(row(40010,0),row(40011,1),row(40012,2)));var result=read(jdbc);
        assertEquals(3,result.offers().size());assertTrue(result.offers().stream().allMatch(o->o.support()==ModernShopCatalog.Support.HEALING));
        assertEquals(Map.of(1,"owner",2,3333L),jdbc.parameters);assertEquals(List.of(257),jdbc.maxRows);
        assertEquals(List.of(ModernShopCatalogJdbc.sql("catalog_7")),jdbc.sql);assertFalse(jdbc.sql.get(0).contains("password"));
        assertTrue(jdbc.sql.get(0).contains("LEFT JOIN `catalog_7`.`lito_tournament_shop` s ON 1=1"));
        assertFalse(jdbc.sql.get(0).contains("WHERE enabled"));assertTrue(jdbc.timeouts.stream().allMatch(v->v>0&&v<=10));
        assertEquals(1,jdbc.connections);assertEquals(1,jdbc.connectionCloses);assertEquals(1,jdbc.statementCloses);assertEquals(1,jdbc.resultCloses);
        assertTrue(jdbc.readOnly);assertTrue(jdbc.properties.isEmpty());
    }
    @Test void disabledRowsAreValidatedAndMissingOwnerDiffersFromEmptyShop()throws Exception {
        var disabled=row(40010,0);disabled.put("enabled",0L);assertTrue(read(new Jdbc(List.of(disabled))).offers().isEmpty());
        var empty=row(40010,0);for(String key:List.of("shop_item","sort_order","buy_price","sell_price","enabled","w_id","a1_id","e_id"))empty.put(key,null);
        assertTrue(read(new Jdbc(List.of(empty))).offers().isEmpty());
        try(var config=config()){var jdbc=new Jdbc(List.of());assertTrue(scoped(scope->dao(config.sources().get(0),jdbc).shopCharacter(7,"owner",3333,"catalog_7")).isEmpty());}
        for(Object invalid:new Object[]{null,-1L,2L}){var value=row(40010,0);value.put("enabled",invalid);reject(new Jdbc(List.of(value)));}
        disabled.put("buy_price",0L);reject(new Jdbc(List.of(disabled)));
        empty.put("enabled",1L);reject(new Jdbc(List.of(empty)));
    }
    @Test void allRawRowsIncludingDisabledAreBoundedWithoutTruncation()throws Exception {
        var rows=new ArrayList<Map<String,Object>>();for(int i=0;i<256;i++){var r=row(100000+i,i);r.put("enabled",0L);rows.add(r);}
        assertTrue(read(new Jdbc(rows)).offers().isEmpty());rows.add(row(100256,256));reject(new Jdbc(rows));
    }
    @Test void duplicatesOrderingMissingTemplatesAndCrossTableAliasesFailClosed()throws Exception {
        reject(new Jdbc(List.of(row(40010,0),row(40010,1))));reject(new Jdbc(List.of(row(40010,0),row(40011,0))));
        reject(new Jdbc(List.of(row(40011,1),row(40010,0))));
        for(Object id:new Object[]{null,40011L}){var value=row(40010,0);value.put("e_id",id);reject(new Jdbc(List.of(value)));}
        var alias=row(40010,0);alias.put("w_id",40010L);reject(new Jdbc(List.of(alias)));
    }
    @Test void sourceOwnerFreshBansAndCharacterChangesCannotBecomeAValidSnapshot()throws Exception {
        for(String column:List.of("account_banned","character_banned")){var value=row(40010,0);value.put(column,1L);reject(new Jdbc(List.of(value)));}
        var wrong=row(40010,0);putText(wrong,"canonical_account","other");reject(new Jdbc(List.of(wrong)));
        wrong=row(40010,0);wrong.put("objid",3334L);reject(new Jdbc(List.of(wrong)));
        var changed=row(40011,1);changed.put("level",81L);reject(new Jdbc(List.of(row(40010,0),changed)));
    }
    @Test void numericMetadataNullFractionOverflowAndStringCoercionAreRejected()throws Exception {
        for(String column:List.of("objid","level","character_type","character_sex","shop_item","sort_order","buy_price","sell_price","enabled",
                "e_invgfx","e_grdgfx","e_weight","stackable","max_charge_count","min_lvl","max_lvl","bless","trade","cant_delete","delay_id","delay_time","delay_effect","food_volume","save_at_once","limit_class"))
            for(Object invalid:new Object[]{null,new BigDecimal("1.25"),new BigDecimal("9223372036854775808"),1.0,"1"}) {
                var value=row(40010,0);value.put(column,invalid);reject(new Jdbc(List.of(value)));
            }
    }
    @Test void unknownOrChangedHealingTemplatesRemainVisibleButUnusable()throws Exception {
        var missing=row(40010,0);putText(missing,"executor",null);
        assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(missing))).offers().get(0).support());
        for(String column:List.of("executor","e_name","use_type","item_type","material")) {
            var value=row(40010,0);putText(value,column,"other");
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(value))).offers().get(0).support());
        }
        var graphic=row(40010,0);graphic.put("e_invgfx",9999L);
        assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(graphic))).offers().get(0).support());
        var other=row(50000,0);assertEquals(ModernShopCatalog.Support.UNIMPLEMENTED,read(new Jdbc(List.of(other))).offers().get(0).support());
    }
    @Test void textLengthsUnicodeAndPartialNullExecutorAreExact()throws Exception {
        for(String column:List.of("canonical_account","char_name","e_name","executor","item_type","use_type","material")) {
            for(String value:List.of("\u00a0","\u2007","\u202f","\ud800","x\0y","x".repeat(257))) {
                var row=row(40010,0);putText(row,column,value);reject(new Jdbc(List.of(row)));
            }
            var row=row(40010,0);row.put(column+"_bytes",999L);reject(new Jdbc(List.of(row)));
        }
        var row=row(40010,0);row.put("executor",null);reject(new Jdbc(List.of(row)));
    }
    @Test void sameObjectAcrossSourcesUsesExplicitCanonicalAndCatalogWithoutCache()throws Exception {
        try(var config=config()) {
            var a=row(40010,0);putText(a,"canonical_account","sun");var b=row(40010,0);putText(b,"canonical_account","moon");
            var first=new Jdbc(List.of(a));var second=new Jdbc(List.of(b));var sources=new HashMap<Integer,ModernShopCatalogRepository.Source>();
            sources.put(7,new ModernShopCatalogRepository.Source(config.sources().get(0),dao(config.sources().get(0),first)));
            sources.put(42,new ModernShopCatalogRepository.Source(config.sources().get(1),dao(config.sources().get(1),second)));
            var repo=ModernShopCatalogRepository.jdbc(sources);sources.clear();var principal=principal();
            var sun=scoped(s->repo.character(principal,7,3333)).orElseThrow();var moon=scoped(s->repo.character(principal,42,3333)).orElseThrow();
            assertNotEquals(sun.token(),moon.token());assertEquals("sun",first.parameters.get(1));assertEquals("moon",second.parameters.get(1));
            assertTrue(second.sql.get(0).contains("`catalog_42`.`etcitem`"));
            a.put("buy_price",99L);assertNotEquals(sun.token(),scoped(s->repo.character(principal,7,3333)).orElseThrow().token());
            assertEquals(2,first.connections);assertEquals(2,first.connectionCloses);
            assertThrows(SecurityException.class,()->repo.character(principal,8,3333));
        }
    }
    @Test void invalidBindingOrIdentifierFailsBeforeSocketAndUnavailableNeverMeansEmpty()throws Exception {
        try(var config=config()) {
            var source=config.sources().get(0);var jdbc=new Jdbc(List.of());var backend=dao(source,jdbc);
            for(String catalog:new String[]{null,"","x`.`etcitem","x;drop","x".repeat(65)})
                assertThrows(IllegalArgumentException.class,()->scoped(scope->backend.shopCharacter(7,"owner",3333,catalog)));
            var repo=ModernShopCatalogRepository.jdbc(Map.of(7,new ModernShopCatalogRepository.Source(source,backend)));
            assertThrows(IllegalStateException.class,()->repo.character(principal(),42,3333));
            var spoof=new ModernAccountAuthentication.Principal("owner",Set.of(new ModernAccountAuthentication.Source(7,"other")));
            assertThrows(SecurityException.class,()->repo.character(spoof,7,3333));
            assertThrows(IllegalArgumentException.class,()->new ModernShopCatalogRepository.Source(config.sources().get(1),backend));
            assertEquals(0,jdbc.connections);
        }
        assertThrows(IllegalArgumentException.class,()->ModernShopCatalogRepository.jdbc(Map.of()));
        assertThrows(IllegalStateException.class,()->ModernShopCatalogRepository.unavailable().character(principal(),7,3333));
    }
    @Test void cancellationAtFirstAndFinalNextNeverPublishesAndCleanupErrorsStaySanitized()throws Exception {
        for(int next:new int[]{1,2}) {
            var jdbc=new Jdbc(List.of(row(40010,0)));jdbc.cancelNext=next;
            var error=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,error.failure());assertEquals(1,jdbc.statementCloses);assertEquals(1,jdbc.resultCloses);
        }
        for(String point:List.of("prepare","setup","query","resultClose","statementClose","connectionClose")) {
            var jdbc=new Jdbc(List.of(row(40010,0)));jdbc.failurePoint=point;jdbc.failure=new SQLException("secret SQL URL owner",new Exception("secret"));
            var error=assertThrows(SQLException.class,()->read(jdbc));assertEquals("modern JDBC backend unavailable",error.getMessage());
            assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
        }
        for(int code:new int[]{1054,1142,1146}) {var jdbc=new Jdbc(List.of());jdbc.failure=new SQLException("hidden","42000",code);reject(jdbc);}
    }
    private static ModernAccountAuthentication.Principal principal() {
        return new ModernAccountAuthentication.Principal("sun",Set.of(new ModernAccountAuthentication.Source(7,"Source7"),
                new ModernAccountAuthentication.Source(42,"Source42")),Map.of(7,"sun",42,"moon"));
    }
    private void reject(Jdbc jdbc)throws Exception {assertThrows(SQLException.class,()->read(jdbc));assertEquals(1,jdbc.connectionCloses);}
    private static void putText(Map<String,Object> row,String key,String value){row.put(key,value);row.put(key+"_bytes",value==null?null:(long)value.getBytes(StandardCharsets.UTF_8).length);}
    private static Map<String,Object> row(long item,int sort) {
        var row=new HashMap<String,Object>();putText(row,"canonical_account","owner");putText(row,"char_name","測試");
        row.put("account_banned",0L);row.put("character_banned",0L);row.put("objid",3333L);row.put("character_type",1L);row.put("character_sex",0L);row.put("level",80L);
        row.put("shop_item",item);row.put("sort_order",(long)sort);row.put("buy_price",40L);row.put("sell_price",20L);row.put("enabled",1L);
        row.put("w_id",null);row.put("a1_id",null);row.put("e_id",item);
        var t=ModernShopCatalogTest.template(item);putText(row,"e_name",t.nameToken());row.put("e_invgfx",t.inventoryGraphic());row.put("e_grdgfx",t.groundGraphic());row.put("e_weight",t.weight());
        var e=t.etc();putText(row,"executor",e.executor());putText(row,"item_type",e.itemType());putText(row,"use_type",e.useType());putText(row,"material",e.material());
        String[] names={"stackable","max_charge_count","min_lvl","max_lvl","bless","trade","cant_delete","delay_id","delay_time","delay_effect","food_volume","save_at_once","limit_class"};
        long[] values={e.stackable(),e.maxChargeCount(),e.minLevel(),e.maxLevel(),e.blessing(),e.trade(),e.cantDelete(),e.delayGroup(),e.delayMillis(),e.delayEffect(),e.foodVolume(),e.saveAtOnce(),e.limitClass()};
        for(int i=0;i<names.length;i++)row.put(names[i],values[i]);return row;
    }
    private static final class Jdbc implements ModernJdbcBackend.Connector {
        final List<Map<String,Object>> rows;final List<String> sql=new ArrayList<>();final List<Integer> maxRows=new ArrayList<>(),timeouts=new ArrayList<>();
        final Map<Integer,Object> parameters=new HashMap<>();int connections,connectionCloses,statementCloses,resultCloses,cancelNext;
        boolean readOnly;String failurePoint="query";SQLException failure;Properties properties;
        Jdbc(List<Map<String,Object>> rows){this.rows=List.copyOf(rows);}
        private void fail(String point)throws SQLException {if(failure!=null&&failurePoint.equals(point))throw failure;}
        public Connection connect(String url,Properties properties){connections++;this.properties=properties;
            return proxy(Connection.class,(method,args)->switch(method) {
                case "setReadOnly"->{readOnly=(boolean)args[0];yield null;}case "setNetworkTimeout","abort"->null;
                case "close"->{connectionCloses++;fail("connectionClose");yield null;}
                case "prepareStatement"->{sql.add((String)args[0]);fail("prepare");yield statement();}
                default->throw new AssertionError("unexpected connection "+method);
            });
        }
        private PreparedStatement statement(){return proxy(PreparedStatement.class,(method,args)->switch(method){
            case "setMaxRows"->{maxRows.add((int)args[0]);fail("setup");yield null;}case "setQueryTimeout"->{timeouts.add((int)args[0]);yield null;}
            case "setString","setLong"->{parameters.put((int)args[0],args[1]);yield null;}
            case "executeQuery"->{fail("query");yield result();}case "close"->{statementCloses++;fail("statementClose");yield null;}
            default->throw new AssertionError("unexpected statement "+method);
        });}
        private ResultSet result(){int[] index={-1};boolean[] wasNull={false};var columns=new ModernJdbcNumbersTest.Columns(c->rows.get(index[0]).get(c),Map.of());
            return proxy(ResultSet.class,(method,args)->switch(method){
                case "next"->{index[0]++;if(cancelNext==index[0]+1)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<rows.size();}
                case "findColumn"->columns.find((String)args[0]);case "getMetaData"->columns.metadata();case "wasNull"->wasNull[0];
                case "getString","getBigDecimal"->{Object value=rows.get(index[0]).get(columns.name(args[0]));wasNull[0]=value==null;
                    yield method.equals("getString")?value:ModernJdbcNumbersTest.Columns.decimal(value);}
                case "close"->{resultCloses++;fail("resultClose");yield null;}
                default->throw new AssertionError("unexpected result "+method);
            });
        }
    }
    @FunctionalInterface private interface Invocation{Object call(String method,Object[] args)throws Throwable;}
    private static <T>T proxy(Class<T> type,Invocation call){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
            (instance,method,args)->call.call(method.getName(),args)));}
}
