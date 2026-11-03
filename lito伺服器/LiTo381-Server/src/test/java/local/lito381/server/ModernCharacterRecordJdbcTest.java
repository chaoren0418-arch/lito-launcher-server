package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Synthetic owned JDBC only. Every unexpected/write/commit operation is an error. */
class ModernCharacterRecordJdbcTest {
    private static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(7, 3333);
    @FunctionalInterface private interface Work<T> { T read(ModernBackendScope scope) throws Exception; }
    static <T> T scoped(Work<T> work) throws Exception {
        var scope = new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(10), new ModernBackendScope.AbortScheduler() {
            public boolean execute(Runnable task) { task.run(); return true; }
            public void remove(Runnable task) { }
        });
        ModernBackendScope.bind(scope);
        try { return work.read(scope); } finally { scope.finish(); ModernBackendScope.unbind(); }
    }
    private static ModernCharacterRecord.Snapshot read(Jdbc jdbc) throws Exception {
        return scoped(scope -> new ModernCharacterRecordJdbc(jdbc, "central_records").read(scope, KEY));
    }
    private static void rejected(Jdbc jdbc) {
        var failure = assertThrows(SQLException.class, () -> read(jdbc));
        assertEquals("modern character record unavailable", failure.getMessage()); assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length); assertEquals(1, jdbc.closes);
    }

    @Test void exactCentralSourceObjectQueryFiveReadOnlyStatementsAndActualClose() throws Exception {
        var jdbc = new Jdbc(); jdbc.data = List.of(total(7, Long.MAX_VALUE, Long.MAX_VALUE - 1, 1, Long.MAX_VALUE));
        var value = scoped(scope -> {
            long deadline = scope.deadlineNanos();
            var result = new ModernCharacterRecordJdbc(jdbc, "central_records").read(scope, new TournamentPresence.CharacterKey(7, Long.MAX_VALUE));
            assertEquals(deadline, jdbc.deadline); return result;
        });
        assertTrue(value.rowPresent()); assertEquals(Long.MAX_VALUE, value.key().objectId()); assertEquals(Long.MAX_VALUE, value.revision());
        assertEquals(Map.of(1,7,2,Long.MAX_VALUE), jdbc.bound.get("TOTAL"));
        for (String query : List.of("TABLES","COLUMNS","KEYS")) assertEquals(Map.of(1,"central_records"),jdbc.bound.get(query));
        assertEquals(5, jdbc.sql.size()); assertTrue(jdbc.sql.stream().allMatch(s -> s.startsWith("SELECT ")));
        assertTrue(jdbc.sql.get(0).contains("`central_records`.`lito_modern_character_record` LIMIT 0"));
        assertTrue(jdbc.sql.get(4).endsWith(" WHERE source_id=? AND object_id=? LIMIT 2"));
        assertFalse(jdbc.sql.stream().anyMatch(s -> s.contains("FOR UPDATE") || s.contains("characters")));
        assertEquals(List.of(1,2,6,3,2), jdbc.maxRows); assertTrue(jdbc.timeouts.stream().allMatch(n -> n >= 1 && n <= 10));
        assertTrue(jdbc.readOnly); assertFalse(jdbc.autoCommit); assertEquals(Connection.TRANSACTION_READ_COMMITTED, jdbc.isolation);
        assertEquals(5, jdbc.statementCloses); assertEquals(5, jdbc.resultCloses); assertEquals(1, jdbc.closes); assertEquals(0, jdbc.aborts);
    }
    @Test void onlySuccessfulSchemaQualifiedNoRowMeansAbsentAndExistingZerosRemainPresent() throws Exception {
        var empty = new Jdbc(); empty.data = List.of();
        assertEquals(ModernCharacterRecord.Snapshot.absent(KEY), read(empty)); assertEquals(5, empty.sql.size());
        var zero = new Jdbc(); zero.data = List.of(total(7,3333,0,0,0)); assertTrue(read(zero).rowPresent());
    }
    @Test void missingTablePermissionAndDriverFailureNeverBecomeZero() {
        for (String phase : List.of("PIN","TABLES","COLUMNS","KEYS","TOTAL")) {
            var jdbc = new Jdbc(); jdbc.executeFailure = phase; rejected(jdbc);
        }
        var missing = new Jdbc(); missing.tables = List.of(); rejected(missing); assertEquals(2, missing.sql.size());
    }
    @Test void engineTableIdentityDuplicateAndMetadataBoundsReject() {
        for (String engine : new String[]{null,"MyISAM","", "InnoDB\0", "X".repeat(17)}) {
            var jdbc = new Jdbc(); jdbc.tables.get(0).put("ENGINE",engine); rejected(jdbc);
        }
        var wrong = new Jdbc(); wrong.tables.get(0).put("TABLE_NAME","native_record"); rejected(wrong);
        var duplicate = new Jdbc(); duplicate.tables = List.of(duplicate.tables.get(0), duplicate.tables.get(0)); rejected(duplicate);
        var pinRows = new Jdbc(); pinRows.pinRows = true; rejected(pinRows); assertEquals(1,pinRows.sql.size());
    }
    @Test void allRequiredSignedColumnsMustExistNotNullAndExactlyMatchSchema() {
        for (int index=0;index<5;index++) {
            var missing=new Jdbc(); missing.columns.remove(index); rejected(missing);
            for (String field:List.of("COLUMN_NAME","DATA_TYPE","COLUMN_TYPE","IS_NULLABLE")) {
                var nullField=new Jdbc(); nullField.columns.get(index).put(field,null); rejected(nullField);
            }
            for (String type:List.of("bigint unsigned","bigint zerofill","decimal(20,0)","varchar(20)","bigint(999)")) {
                var invalid=new Jdbc(); invalid.columns.get(index).put("COLUMN_TYPE",type); rejected(invalid);
            }
            var nullable=new Jdbc(); nullable.columns.get(index).put("IS_NULLABLE","YES"); rejected(nullable);
            var characterLength=new Jdbc(); characterLength.columns.get(index).put("CHARACTER_MAXIMUM_LENGTH",20L); rejected(characterLength);
        }
        var duplicate=new Jdbc(); duplicate.columns.add(new HashMap<>(duplicate.columns.get(0))); rejected(duplicate);
        var unknown=new Jdbc(); unknown.columns.get(0).put("COLUMN_NAME","extra"); rejected(unknown);
        var wrongType=new Jdbc(); wrongType.columns.get(0).put("DATA_TYPE","bigint"); rejected(wrongType);
    }
    @Test void primaryKeyOrderUniquenessPresenceAndNoAdditionalUniqueIndexAreRequired() {
        var missing=new Jdbc(); missing.keys.remove(1); rejected(missing);
        for (String field:List.of("INDEX_NAME","COLUMN_NAME","SEQ_IN_INDEX")) {
            var invalid=new Jdbc(); invalid.keys.get(0).put(field,null); rejected(invalid);
        }
        var reverse=new Jdbc(); java.util.Collections.reverse(reverse.keys); rejected(reverse);
        var duplicate=new Jdbc(); duplicate.keys.add(new HashMap<>(duplicate.keys.get(0))); rejected(duplicate);
        var extra=new Jdbc(); extra.keys.get(0).put("INDEX_NAME","another_unique"); rejected(extra);
        var wrong=new Jdbc(); wrong.keys.get(1).put("COLUMN_NAME","wins"); rejected(wrong);
        var badSequence=new Jdbc(); badSequence.keys.get(1).put("SEQ_IN_INDEX",new BigDecimal("2.1")); rejected(badSequence);
    }
    @Test void caseInsensitiveMetadataAndSignedDisplayWidthsAreAccepted() throws Exception {
        var jdbc=new Jdbc();
        for (var row:jdbc.tables) row.replaceAll((k,v)->v instanceof String s?s.toUpperCase(Locale.ROOT):v);
        for (var row:jdbc.columns) row.replaceAll((k,v)->v instanceof String s?s.toUpperCase(Locale.ROOT):v);
        for (var row:jdbc.keys) row.replaceAll((k,v)->v instanceof String s?s.toUpperCase(Locale.ROOT):v);
        assertEquals(3,read(jdbc).revision());
        for (var row:jdbc.columns) row.put("COLUMN_TYPE",row.get("DATA_TYPE"));
        assertEquals(3,read(jdbc).revision());
    }
    @Test void totalRowSourceObjectAndDuplicateAreNeverTrustedFromPredicateAlone() {
        for (String field:List.of("source_id","object_id")) {
            var wrong=new Jdbc(); wrong.data.get(0).put(field,999L); rejected(wrong);
        }
        var duplicate=new Jdbc(); duplicate.data=List.of(duplicate.data.get(0),duplicate.data.get(0)); rejected(duplicate);
    }
    @Test void exactNumericNullFractionOverflowApproximateAndStringValuesFailClosed() {
        for (String field:List.of("source_id","object_id","wins","losses","revision")) {
            var nullField=new Jdbc(); nullField.data.get(0).put(field,null); rejected(nullField);
            for (Object value:List.of(new BigDecimal("1.1"),new BigDecimal("9223372036854775808"),1.0d,1.0f,"1")) {
                var invalid=new Jdbc(); invalid.data.get(0).put(field,value); rejected(invalid);
            }
        }
    }
    @Test void negativeCountersInconsistentRevisionAndCheckedSumOverflowFailClosed() {
        for (long[] values:new long[][]{{-1,0,0},{0,-1,0},{0,0,-1},{1,1,1},{0,0,1},{Long.MAX_VALUE,1,Long.MAX_VALUE}}) {
            var jdbc=new Jdbc(); jdbc.data=List.of(total(7,3333,values[0],values[1],values[2])); rejected(jdbc);
        }
    }
    @Test void callerConnectionStateMustBeNewAutocommitAndVerifiedReadOnlyBeforeQueries() {
        var closed=new Jdbc(); closed.initiallyClosed=true; rejected(closed); assertTrue(closed.sql.isEmpty());
        var transaction=new Jdbc(); transaction.autoCommit=false; rejected(transaction); assertTrue(transaction.sql.isEmpty());
        var refusesReadonly=new Jdbc(); refusesReadonly.refuseReadOnly=true; rejected(refusesReadonly); assertTrue(refusesReadonly.sql.isEmpty());
        var refusesTransaction=new Jdbc(); refusesTransaction.refuseTransaction=true; rejected(refusesTransaction); assertTrue(refusesTransaction.sql.isEmpty());
    }
    @Test void setupResultStatementAndPhysicalCloseFailureCannotPublishSuccessOrAbsence() {
        for (String phase:List.of("network","setup","result","statement","connection")) {
            var jdbc=new Jdbc(); jdbc.cleanupFailure=phase; rejected(jdbc);
        }
        var empty=new Jdbc(); empty.data=List.of(); empty.cleanupFailure="connection"; rejected(empty);
    }
    @Test void badDatabaseIdentifierAndMissingFactoryNeverOpenConnections() {
        for (String database:new String[]{null,"","x`.`native","x;DROP TABLE y","a".repeat(65)}) {
            var jdbc=new Jdbc(); assertThrows(IllegalArgumentException.class,()->new ModernCharacterRecordJdbc(jdbc,database)); assertEquals(0,jdbc.opens);
        }
        assertThrows(NullPointerException.class,()->new ModernCharacterRecordJdbc(null,"central_records"));
    }
    @Test void cancellationBeforeFactoryAndDuringDataNextUsesActualLeaseAbort() throws Exception {
        var before=new Jdbc();
        assertThrows(ModernBackendExecutor.BackendException.class,()->scoped(scope->{
            scope.cancel(ModernBackendExecutor.Failure.CANCELLED); return new ModernCharacterRecordJdbc(before,"central_records").read(scope,KEY);
        }));
        assertEquals(0,before.opens);
        for (int next:new int[]{1,2}) {
            var jdbc=new Jdbc(); jdbc.cancelDataNext=next;
            var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());
            assertEquals(1,jdbc.aborts); assertEquals(1,jdbc.closes); assertEquals(5,jdbc.statementCloses); assertEquals(5,jdbc.resultCloses);
        }
    }
    @Test void cancellationDuringFactoryReturnClosesReturnedConnectionWithoutPublishing() throws Exception {
        var jdbc=new Jdbc(); jdbc.cancelOpen=true;
        assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
        assertEquals(1,jdbc.opens); assertEquals(1,jdbc.closes); assertTrue(jdbc.sql.isEmpty());
    }
    @Test void blockedPhysicalCloseKeepsBackendWorkerCountedUntilActuallyRetired() throws Exception {
        var jdbc=new Jdbc(); jdbc.closeStarted=new CountDownLatch(1); jdbc.closeRelease=new CountDownLatch(1);
        var callers=Executors.newSingleThreadExecutor(); var backend=new ModernBackendExecutor(1,1); var gate=backend.openSession();
        try {
            var result=callers.submit(()->gate.call(5000,scope->new ModernCharacterRecordJdbc(jdbc,"central_records").read(scope,KEY)));
            assertTrue(jdbc.closeStarted.await(2,TimeUnit.SECONDS)); gate.close(); backend.close();
            assertFalse(backend.awaitTermination(50)); assertEquals(1,backend.activeCount());
            jdbc.closeRelease.countDown(); assertTrue(backend.awaitTermination(2000));
            assertThrows(java.util.concurrent.ExecutionException.class,()->result.get(2,TimeUnit.SECONDS));
            assertEquals(1,jdbc.closes);
        } finally { jdbc.closeRelease.countDown(); gate.close(); backend.close(); backend.awaitTermination(2000); callers.shutdownNow(); assertTrue(callers.awaitTermination(2,TimeUnit.SECONDS)); }
    }
    @Test void repositoryAuthorizesExactSourceAndNeverReadsCanonicalAccountOrFallsBack() throws Exception {
        var jdbc=new Jdbc(); var repo=ModernCharacterRecordRepository.jdbc(jdbc,"central_records");
        var principal=new ModernAccountAuthentication.Principal("sun-owner",Set.of(new ModernAccountAuthentication.Source(7,"Sun"),
                new ModernAccountAuthentication.Source(42,"Moon")),Map.of(7,"sun-owner",42,"moon-owner"));
        assertEquals(7,scoped(scope->repo.character(principal,7,3333)).key().sourceId());
        jdbc.data=List.of(total(42,3333,0,7,7));
        var moon=scoped(scope->repo.character(principal,42,3333)); assertEquals(7,moon.losses()); assertEquals(42,moon.key().sourceId());
        int opened=jdbc.opens; assertThrows(SecurityException.class,()->scoped(scope->repo.character(principal,8,3333))); assertEquals(opened,jdbc.opens);
        assertThrows(IllegalStateException.class,()->ModernCharacterRecordRepository.unavailable().character(principal,7,3333));
        assertTrue(jdbc.sql.stream().noneMatch(s->s.contains("account")||s.contains("sun-owner")||s.contains("moon-owner")));
    }

    private static Map<String,Object> total(long source,long object,long wins,long losses,long revision) {
        return new HashMap<>(Map.of("source_id",source,"object_id",object,"wins",wins,"losses",losses,"revision",revision));
    }
    private static final class Jdbc implements ModernMatchSettlementJdbc.ConnectionFactory {
        List<Map<String,Object>> tables=new ArrayList<>(),columns=new ArrayList<>(),keys=new ArrayList<>(),data=new ArrayList<>();
        final List<String> sql=new ArrayList<>(); final List<Integer> maxRows=new ArrayList<>(),timeouts=new ArrayList<>();
        final Map<String,Map<Integer,Object>> bound=new HashMap<>();
        int opens,closes,aborts,statementCloses,resultCloses,isolation,cancelDataNext; long deadline;
        boolean initiallyClosed,autoCommit=true,readOnly,refuseReadOnly,refuseTransaction,pinRows,cancelOpen;
        String executeFailure,cleanupFailure; CountDownLatch closeStarted,closeRelease;
        Jdbc() {
            tables.add(new HashMap<>(Map.of("TABLE_NAME",ModernCharacterRecordJdbc.TABLE,"ENGINE","InnoDB")));
            for(String name:List.of("source_id","object_id","wins","losses","revision")) {
                String type=name.equals("source_id")?"int":"bigint";
                var row=new HashMap<String,Object>(); row.put("COLUMN_NAME",name);row.put("DATA_TYPE",type);row.put("COLUMN_TYPE",type+"("+(name.equals("source_id")?11:20)+")");
                row.put("IS_NULLABLE","NO"); row.put("CHARACTER_MAXIMUM_LENGTH",null); columns.add(row);
            }
            keys.add(new HashMap<>(Map.of("INDEX_NAME","PRIMARY","SEQ_IN_INDEX",1L,"COLUMN_NAME","source_id")));
            keys.add(new HashMap<>(Map.of("INDEX_NAME","PRIMARY","SEQ_IN_INDEX",2L,"COLUMN_NAME","object_id")));
            data.add(total(7,3333,2,1,3));
        }
        @Override public Connection open(long deadline) {
            opens++; this.deadline=deadline;
            // Each factory call owns a new, initially auto-commit connection.
            if(opens>1){autoCommit=true;readOnly=false;}
            if(cancelOpen)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
            return proxy(Connection.class,(method,args)->switch(method) {
                case "isClosed"->initiallyClosed;case "getAutoCommit"->autoCommit;case "isReadOnly"->readOnly;
                case "setAutoCommit"->{if(!refuseTransaction)autoCommit=(boolean)args[0];yield null;}
                case "setReadOnly"->{if(!refuseReadOnly)readOnly=(boolean)args[0];yield null;}
                case "setTransactionIsolation"->{isolation=(int)args[0];yield null;}
                case "setNetworkTimeout"->{fail("network");assertTrue((int)args[1]>=1);yield null;}
                case "abort"->{aborts++;yield null;}
                case "close"->{closes++;if(closeStarted!=null){closeStarted.countDown();await(closeRelease);}fail("connection");yield null;}
                case "prepareStatement"->{String query=(String)args[0];sql.add(query);yield statement(classify(query));}
                default->throw new AssertionError("unexpected record connection operation: "+method);
            });
        }
        private String classify(String query) {
            if(query.equals(ModernCharacterRecordJdbc.TABLES))return "TABLES";
            if(query.equals(ModernCharacterRecordJdbc.COLUMNS))return "COLUMNS";
            if(query.equals(ModernCharacterRecordJdbc.KEYS))return "KEYS";
            assertTrue(query.startsWith("SELECT source_id,object_id,wins,losses,revision FROM `central_records`.`lito_modern_character_record`"));
            if(query.endsWith(" LIMIT 0"))return "PIN";
            assertTrue(query.endsWith(" WHERE source_id=? AND object_id=? LIMIT 2"));return "TOTAL";
        }
        private PreparedStatement statement(String query) {
            var parameters=new HashMap<Integer,Object>();bound.put(query,parameters);
            return proxy(PreparedStatement.class,(method,args)->switch(method) {
                case "setMaxRows"->{maxRows.add((int)args[0]);fail("setup");yield null;}
                case "setQueryTimeout"->{timeouts.add((int)args[0]);yield null;}
                case "setString","setInt","setLong"->{parameters.put((int)args[0],args[1]);yield null;}
                case "executeQuery"->{if(query.equals(executeFailure))throw new SQLException("synthetic secret query/owner","42000",1142);
                    yield result(query,switch(query){case "TABLES"->tables;case "COLUMNS"->columns;case "KEYS"->keys;case "TOTAL"->data;default->pinRows?List.of(Map.of()):List.of();});}
                case "close"->{statementCloses++;fail("statement");yield null;}
                default->throw new AssertionError("unexpected record statement operation: "+method);
            });
        }
        private ResultSet result(String query,List<Map<String,Object>> values) {
            int[] index={-1};boolean[] wasNull={false};var numeric=new ModernJdbcNumbersTest.Columns(column->values.get(index[0]).get(column));
            return proxy(ResultSet.class,(method,args)->switch(method) {
                case "next"->{index[0]++;if(query.equals("TOTAL")&&cancelDataNext==index[0]+1)
                    ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<values.size();}
                case "findColumn"->numeric.find((String)args[0]);case "getMetaData"->numeric.metadata();case "wasNull"->wasNull[0];
                case "getString","getBigDecimal"->{Object value=values.get(index[0]).get(numeric.name(args[0]));wasNull[0]=value==null;
                    yield method.equals("getString")?value:ModernJdbcNumbersTest.Columns.decimal(value);}
                case "close"->{resultCloses++;fail("result");yield null;}
                default->throw new AssertionError("unexpected record result operation: "+method);
            });
        }
        private void fail(String phase)throws SQLException {if(phase.equals(cleanupFailure))throw new SQLException("synthetic private failure","HY000",90000);}
    }
    private static void await(CountDownLatch latch) {
        boolean interrupted=false;
        while(latch.getCount()!=0)try{latch.await();}catch(InterruptedException ignored){interrupted=true;}
        if(interrupted)Thread.currentThread().interrupt();
    }
    @FunctionalInterface private interface Invocation { Object call(String method,Object[] args)throws Throwable; }
    private static <T>T proxy(Class<T> type,Invocation call) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
            (proxy,method,args)->call.call(method.getName(),args))); }
}
