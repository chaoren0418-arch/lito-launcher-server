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

/** Synthetic owned JDBC only. Every unexpected/write call fails, never reaches MySQL. */
class ModernGmCommandJdbcTest {
    @TempDir Path temporary;
    private ModernServerConfig config() throws Exception {
        StringBuilder input=new StringBuilder("schema=1\nbind.port=0\ntls.pkcs12=")
                .append(temporary.resolve("listener.p12").toAbsolutePath().toString().replace('\\','/'))
                .append("\ntls.password.env=T\nsources=7,42\n");
        for (int id : List.of(7,42)) input.append("source.").append(id).append(".name=Source").append(id).append('\n')
                .append("source.").append(id).append(".catalog.database=catalog_").append(id).append('\n')
                .append("source.").append(id).append(".mysql.host=127.0.0.1\n")
                .append("source.").append(id).append(".mysql.port=3306\n")
                .append("source.").append(id).append(".mysql.database=characters_").append(id).append('\n')
                .append("source.").append(id).append(".mysql.username.env=U\n")
                .append("source.").append(id).append(".mysql.password.env=P\n")
                .append("source.").append(id).append(".mysql.passwordScheme=plain\n")
                .append("source.").append(id).append(".mysql.tls=disabled\n");
        return ModernServerConfig.parse(input.toString().getBytes(StandardCharsets.UTF_8),key -> "synthetic");
    }
    @FunctionalInterface private interface Scoped<T> { T run(ModernBackendScope scope) throws Exception; }
    private static <T>T scoped(Scoped<T> body) throws Exception {
        var scope=new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),new ModernBackendScope.AbortScheduler(){
            public boolean execute(Runnable action){action.run();return true;}public void remove(Runnable action){}
        });
        ModernBackendScope.bind(scope);
        try{return body.run(scope);}finally{scope.finish();ModernBackendScope.unbind();}
    }
    private static ModernJdbcBackend dao(ModernServerConfig.SourceConfig source,Jdbc jdbc) throws Exception {
        return new ModernJdbcBackend(source.database(),1000,3000,jdbc);
    }
    private ModernGmCommands.Snapshot read(Jdbc jdbc) throws Exception {
        try(var config=config()) {return scoped(scope -> dao(config.sources().get(0),jdbc).gmCommandCharacter(7,"owner",3333,"catalog_7")).orElseThrow();}
    }
    @Test void oneBoundedJoinVerifiesSourceCanonicalOwnerCharacterAndActualDefinition() throws Exception {
        var jdbc=new Jdbc(List.of(row()));var result=read(jdbc);
        assertEquals(ModernGmCommands.Decision.ALLOWED,result.decision());assertEquals(7,result.sourceId());
        assertEquals(3333,result.character().objectId());assertEquals("測試",result.character().name());
        assertEquals("owner",result.canonicalAccount());assertEquals(200,result.accessLevel());
        assertEquals(List.of(ModernGmCommandJdbc.sql("catalog_7")),jdbc.sql);
        assertTrue(jdbc.sql.get(0).contains("JOIN characters c ON c.account_name=a.login"));
        assertTrue(jdbc.sql.get(0).contains("`catalog_7`.`commands`"));
        assertFalse(jdbc.sql.get(0).contains("password"));assertFalse(jdbc.sql.get(0).contains("owner"));
        assertEquals(Map.of(1,"de",2,"owner",3,3333L),jdbc.parameters);
        assertEquals(List.of(2),jdbc.maxRows);assertTrue(jdbc.timeouts.stream().allMatch(v -> v>=1 && v<=10));
        assertEquals(1,jdbc.connections);assertEquals(1,jdbc.connectionCloses);assertEquals(1,jdbc.statementCloses);assertEquals(1,jdbc.resultCloses);
        assertTrue(jdbc.readOnly);assertTrue(jdbc.properties.isEmpty());
    }
    @Test void missingOwnerDiffersFromMissingCommandAndMissingTables() throws Exception {
        try(var config=config()) {
            var jdbc=new Jdbc(List.of());assertTrue(scoped(scope -> dao(config.sources().get(0),jdbc)
                    .gmCommandCharacter(7,"owner",3333,"catalog_7")).isEmpty());assertEquals(1,jdbc.connectionCloses);
        }
        var absent=row();for (String key : List.of("command_name","command_name_bytes","required_access","executor","executor_bytes","command_system")) absent.put(key,null);
        assertEquals(ModernGmCommands.Decision.COMMAND_DISABLED,read(new Jdbc(List.of(absent))).decision());
        for (int code : new int[]{1142,1146,1054}) {var jdbc=new Jdbc(List.of());jdbc.failure=new SQLException("sensitive schema","42000",code);reject(jdbc);}
    }
    @Test void maximumGmDoesNotBypassBansOrOtherExecutors() throws Exception {
        for(String flag:List.of("account_banned","character_banned")) {
            var value=row();value.put(flag,1L);assertEquals(flag.equals("account_banned")?ModernGmCommands.Decision.ACCOUNT_BANNED:
                    ModernGmCommands.Decision.CHARACTER_BANNED,read(new Jdbc(List.of(value))).decision());
        }
        var system=row();system.put("command_system",1L);assertEquals(ModernGmCommands.Decision.UNSUPPORTED_EXECUTOR,read(new Jdbc(List.of(system))).decision());
        var other=row();other.put("executor","Other");other.put("executor_bytes",5L);
        assertEquals(ModernGmCommands.Decision.UNSUPPORTED_EXECUTOR,read(new Jdbc(List.of(other))).decision());
    }
    @Test void permissionChangesAreReadForEveryOperationNotCachedFromAuthentication() throws Exception {
        var value=row();var jdbc=new Jdbc(List.of(value));
        try(var config=config()) {
            var source=config.sources().get(0);var backend=dao(source,jdbc);
            var repository=ModernGmCommandRepository.jdbc(Map.of(7,new ModernGmCommandRepository.Source(source,backend)));
            var principal=new ModernAccountAuthentication.Principal("owner",Set.of(new ModernAccountAuthentication.Source(7,"Source7")));
            // Reuse the actual provider and DAO, but create a fresh deadline for
            // every call. Rebuilding the DAO would not detect a per-DAO cache.
            assertEquals(ModernGmCommands.Decision.ALLOWED,scoped(scope -> repository.character(principal,7,3333)).orElseThrow().decision());
            value.put("access_level",199L);
            assertEquals(ModernGmCommands.Decision.INSUFFICIENT_ACCESS,scoped(scope -> repository.character(principal,7,3333)).orElseThrow().decision());
            value.put("required_access",100L);
            assertEquals(ModernGmCommands.Decision.ALLOWED,scoped(scope -> repository.character(principal,7,3333)).orElseThrow().decision());
            value.put("account_banned",1L);
            assertEquals(ModernGmCommands.Decision.ACCOUNT_BANNED,scoped(scope -> repository.character(principal,7,3333)).orElseThrow().decision());
        }
        assertEquals(4,jdbc.connections);assertEquals(4,jdbc.connectionCloses);assertEquals(4,jdbc.sql.size());
    }
    @Test void nullFractionsStringsOverflowAndOutOfRangePermissionsNeverGrant() throws Exception {
        for (String column : List.of("objid","level","character_type","character_sex","access_level","account_banned","character_banned","required_access","command_system"))
            for(Object malformed:new Object[]{null,new BigDecimal("1.25"),new BigDecimal("9223372036854775808"),1.0,"1"}) {
                var value=row();value.put(column,malformed);reject(new Jdbc(List.of(value)));
            }
        for(String column:List.of("access_level","required_access")) for(long invalid:new long[]{-1,201,Long.MAX_VALUE}) {
            var value=row();value.put(column,invalid);reject(new Jdbc(List.of(value)));
        }
        for(String column:List.of("account_banned","character_banned","command_system")) for(long invalid:new long[]{-1,2}) {
            var value=row();value.put(column,invalid);reject(new Jdbc(List.of(value)));
        }
    }
    @Test void ambiguousRowsAndMismatchedOwnerOrCharacterAreNotLimitOneSuccess() throws Exception {
        reject(new Jdbc(List.of(row(),row())));
        for(String column:List.of("canonical_account","command_name")) {
            var value=row();String text=column.equals("canonical_account")?"other":"DE";
            value.put(column,text);value.put(column+"_bytes",(long)text.length());reject(new Jdbc(List.of(value)));
        }
        for(var entry:Map.of("objid",3334L,"level",0L,"character_type",65536L,"character_sex",2L).entrySet()) {
            var value=row();value.put(entry.getKey(),entry.getValue());reject(new Jdbc(List.of(value)));
        }
    }
    @Test void partialCommandNullsAndContradictoryUtf8LengthsFailClosed() throws Exception {
        var partial=row();partial.put("command_name",null);reject(new Jdbc(List.of(partial)));
        for(String column:List.of("canonical_account","char_name","command_name","executor")) {
            for(Object bad:new Object[]{null,0L,257L}) {var value=row();value.put(column+"_bytes",bad);reject(new Jdbc(List.of(value)));}
            for(String bad:List.of("\ud800","a\0b"," ","x".repeat(257))) {
                var value=row();value.put(column,bad);value.put(column+"_bytes",(long)bad.getBytes(StandardCharsets.UTF_8).length);reject(new Jdbc(List.of(value)));
            }
        }
    }
    @Test void identifiersAndRequestValuesRejectBeforeConnecting() throws Exception {
        try(var config=config()) {
            for(String catalog:new String[]{null,"","x`.`commands","x;DROP TABLE x","x".repeat(65)}) {
                var jdbc=new Jdbc(List.of());assertThrows(IllegalArgumentException.class,() -> scoped(scope -> dao(config.sources().get(0),jdbc)
                        .gmCommandCharacter(7,"owner",3333,catalog)));assertEquals(0,jdbc.connections);
            }
            var jdbc=new Jdbc(List.of());assertThrows(SQLException.class,() -> scoped(scope -> dao(config.sources().get(0),jdbc)
                    .gmCommandCharacter(7,"owner",0,"catalog_7")));assertEquals(0,jdbc.connections);
        }
    }
    @Test void wideCharacterValuesAndParameterizedAccountRemainLossless() throws Exception {
        var value=row();String account="owner' OR '1'='1";value.put("canonical_account",account);value.put("canonical_account_bytes",(long)account.length());
        value.put("objid",Long.MAX_VALUE);value.put("level",Long.MAX_VALUE);var jdbc=new Jdbc(List.of(value));
        try(var config=config()) {
            var result=scoped(scope -> dao(config.sources().get(0),jdbc).gmCommandCharacter(7,account,Long.MAX_VALUE,"catalog_7")).orElseThrow();
            assertEquals(Long.MAX_VALUE,result.character().level());assertEquals(Long.MAX_VALUE,result.character().objectId());
            assertEquals(account,jdbc.parameters.get(2));assertFalse(jdbc.sql.get(0).contains(account));
        }
    }
    @Test void cancellationAtFirstOrFinalNextOrBeforeConnectCannotReturnAGrant() throws Exception {
        try(var config=config()) {
            var jdbc=new Jdbc(List.of(row()));assertThrows(ModernBackendExecutor.BackendException.class,() -> scoped(scope -> {
                scope.cancel(ModernBackendExecutor.Failure.CANCELLED);return dao(config.sources().get(0),jdbc).gmCommandCharacter(7,"owner",3333,"catalog_7");}));
            assertEquals(0,jdbc.connections);
        }
        for(int next:new int[]{1,2}) {
            var jdbc=new Jdbc(List.of(row()));jdbc.cancelNext=next;
            var error=assertThrows(ModernBackendExecutor.BackendException.class,() -> read(jdbc));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,error.failure());assertEquals(1,jdbc.statementCloses);assertEquals(1,jdbc.resultCloses);
        }
    }
    @Test void setupQueryAndCleanupErrorsCannotPublishAndAreSanitized() throws Exception {
        for(String point:List.of("prepare","setup","query","resultClose","statementClose","connectionClose")) {
            var jdbc=new Jdbc(List.of(row()));jdbc.failurePoint=point;jdbc.failure=new SQLException("password host SQL owner",new Exception("secret"));
            var error=assertThrows(SQLException.class,() -> read(jdbc));assertEquals("modern JDBC backend unavailable",error.getMessage());
            assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
        }
    }
    @Test void sameObjectAcrossSourcesUsesEachCanonicalAccountCatalogAndThreshold() throws Exception {
        try(var config=config()) {
            var a=row();var b=row();a.put("canonical_account","sun");a.put("canonical_account_bytes",3L);
            b.put("canonical_account","moon");b.put("canonical_account_bytes",4L);b.put("access_level",0L);
            var sun=new Jdbc(List.of(a));var moon=new Jdbc(List.of(b));
            var map=new HashMap<Integer,ModernGmCommandRepository.Source>();map.put(7,new ModernGmCommandRepository.Source(config.sources().get(0),dao(config.sources().get(0),sun)));
            map.put(42,new ModernGmCommandRepository.Source(config.sources().get(1),dao(config.sources().get(1),moon)));
            var repo=ModernGmCommandRepository.jdbc(map);map.clear();var principal=principal();
            assertEquals(ModernGmCommands.Decision.ALLOWED,scoped(scope -> repo.character(principal,7,3333)).orElseThrow().decision());
            assertEquals(ModernGmCommands.Decision.INSUFFICIENT_ACCESS,scoped(scope -> repo.character(principal,42,3333)).orElseThrow().decision());
            assertEquals("sun",sun.parameters.get(2));assertEquals("moon",moon.parameters.get(2));
            assertTrue(sun.sql.get(0).contains("`catalog_7`.`commands`"));assertTrue(moon.sql.get(0).contains("`catalog_42`.`commands`"));
            assertThrows(SecurityException.class,() -> repo.character(principal,8,3333));assertThrows(SecurityException.class,() -> repo.character(principal,7,0));
            assertEquals(1,sun.connections);assertEquals(1,moon.connections);
        }
    }
    @Test void missingSourceConfigAndSpoofedSourceNameCannotUsePrimaryOrOpenSocket() throws Exception {
        try(var config=config()) {
            var jdbc=new Jdbc(List.of(row()));var source=config.sources().get(0);var backend=dao(source,jdbc);
            var repo=ModernGmCommandRepository.jdbc(Map.of(7,new ModernGmCommandRepository.Source(source,backend)));
            assertThrows(IllegalStateException.class,() -> repo.character(principal(),42,3333));
            var spoof=new ModernAccountAuthentication.Principal("owner",Set.of(new ModernAccountAuthentication.Source(7,"Different")));
            assertThrows(SecurityException.class,() -> repo.character(spoof,7,3333));assertEquals(0,jdbc.connections);
            assertThrows(IllegalArgumentException.class,() -> new ModernGmCommandRepository.Source(config.sources().get(1),backend));
            assertThrows(IllegalArgumentException.class,() -> ModernGmCommandRepository.jdbc(Map.of(42,new ModernGmCommandRepository.Source(source,backend))));
        }
        assertThrows(IllegalArgumentException.class,() -> ModernGmCommandRepository.jdbc(Map.of()));
        assertThrows(IllegalStateException.class,() -> ModernGmCommandRepository.unavailable().character(principal(),7,3333));
    }
    private static ModernAccountAuthentication.Principal principal() {
        return new ModernAccountAuthentication.Principal("sun",Set.of(new ModernAccountAuthentication.Source(7,"Source7"),
                new ModernAccountAuthentication.Source(42,"Source42")),Map.of(7,"sun",42,"moon"));
    }
    private void reject(Jdbc jdbc) throws Exception {assertThrows(SQLException.class,() -> read(jdbc));assertEquals(1,jdbc.connectionCloses);}
    private static Map<String,Object> row() {
        var row=new HashMap<String,Object>();row.put("canonical_account","owner");row.put("canonical_account_bytes",5L);
        row.put("objid",3333L);row.put("char_name","測試");row.put("char_name_bytes",6L);row.put("character_type",1L);row.put("character_sex",0L);row.put("level",80L);
        row.put("access_level",200L);row.put("account_banned",0L);row.put("character_banned",0L);
        row.put("command_name","de");row.put("command_name_bytes",2L);row.put("required_access",200L);row.put("executor","L1SpawnDe");row.put("executor_bytes",9L);row.put("command_system",0L);
        return row;
    }
    private static final class Jdbc implements ModernJdbcBackend.Connector {
        final List<Map<String,Object>> rows;final List<String> sql=new ArrayList<>();final List<Integer> maxRows=new ArrayList<>(),timeouts=new ArrayList<>();
        final Map<Integer,Object> parameters=new HashMap<>();int connections,connectionCloses,statementCloses,resultCloses,cancelNext;
        boolean readOnly;String failurePoint="query";SQLException failure;Properties properties;
        Jdbc(List<Map<String,Object>> rows){this.rows=List.copyOf(rows);}
        private void fail(String point) throws SQLException {if(failure!=null && failurePoint.equals(point))throw failure;}
        public Connection connect(String url,Properties properties){connections++;this.properties=properties;
            return proxy(Connection.class,(method,args) -> switch(method) {
                case "setReadOnly" -> {readOnly=(boolean)args[0];yield null;}
                case "setNetworkTimeout","abort" -> null;
                case "close" -> {connectionCloses++;fail("connectionClose");yield null;}
                case "prepareStatement" -> {sql.add((String)args[0]);fail("prepare");yield statement();}
                default -> throw new AssertionError("unexpected connection "+method);
            });
        }
        private PreparedStatement statement(){return proxy(PreparedStatement.class,(method,args) -> switch(method){
            case "setMaxRows" -> {maxRows.add((int)args[0]);fail("setup");yield null;}
            case "setQueryTimeout" -> {timeouts.add((int)args[0]);yield null;}
            case "setString","setLong" -> {parameters.put((int)args[0],args[1]);yield null;}
            case "executeQuery" -> {fail("query");yield result();}
            case "close" -> {statementCloses++;fail("statementClose");yield null;}
            default -> throw new AssertionError("unexpected statement "+method);
        });}
        private ResultSet result(){
            int[] index={-1};boolean[] wasNull={false};var columns=new ModernJdbcNumbersTest.Columns(column -> rows.get(index[0]).get(column),Map.of());
            return proxy(ResultSet.class,(method,args) -> switch(method){
                case "next" -> {index[0]++;if(cancelNext==index[0]+1)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<rows.size();}
                case "findColumn" -> columns.find((String)args[0]);case "getMetaData" -> columns.metadata();case "wasNull" -> wasNull[0];
                case "getString","getBigDecimal" -> {Object value=rows.get(index[0]).get(columns.name(args[0]));wasNull[0]=value==null;
                    yield method.equals("getString")?value:ModernJdbcNumbersTest.Columns.decimal(value);}
                case "close" -> {resultCloses++;fail("resultClose");yield null;}
                default -> throw new AssertionError("unexpected result "+method);
            });
        }
    }
    @FunctionalInterface private interface Invocation {Object call(String method,Object[] args)throws Throwable;}
    private static <T>T proxy(Class<T> type,Invocation call){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
            (instance,method,args) -> call.call(method.getName(),args)));}
}
