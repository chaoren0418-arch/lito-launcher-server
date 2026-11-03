package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;
import static local.lito381.server.ModernMatchSettlementTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

/** Transactional map-backed JDBC seam only; no server, database or network dependency. */
class ModernMatchSettlementJdbcTest {
    private static final long DEADLINE=TimeUnit.SECONDS.toNanos(10);

    @Test void firstSettlementAtomicallyStoresLedgerAllMembersAndQualifiedTotals() throws Exception {
        var db=new Database();db.persist(event());
        assertEquals(1,db.data.ledgers.size());assertEquals(2,db.data.members.get(key(ModernMatchSettlementJdbc.uuid(event()))).size());
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));assertArrayEquals(new long[]{0,1,1},db.total(42,3333));
        assertEquals(1,db.commits);assertEquals(0,db.rollbacks);assertEquals(List.of(DEADLINE),db.deadlines);
        assertTrue(db.sql.stream().noneMatch(s->s.contains("characters ")||s.contains("character_items")||s.contains("lito_tournament")||s.startsWith("CREATE")||s.startsWith("ALTER")));
        assertTrue(db.sql.stream().filter(s->s.startsWith("INSERT")||s.startsWith("UPDATE")).allMatch(s->s.contains("`settlement`.`lito_modern_")));
        db.retired();
    }
    @Test void processRestartAndRepeatedSameEventNeverIncrementAgain() throws Exception {
        var db=new Database();db.persist(event());
        for(int i=0;i<4;i++)db.persist(event()); // Each call creates a new Store and a fresh owned connection.
        assertEquals(1,db.data.ledgers.size());assertEquals(5,db.opens);assertArrayEquals(new long[]{1,0,1},db.total(7,3333));
        assertArrayEquals(new long[]{0,1,1},db.total(42,3333));db.retired();
    }
    @Test void anotherMatchAccumulatesAndOlderIdempotentRetryDoesNotRequireOldTotals() throws Exception {
        var db=new Database();db.persist(event());db.persist(event(UUID.randomUUID()));db.persist(event());
        assertArrayEquals(new long[]{2,0,2},db.total(7,3333));assertArrayEquals(new long[]{0,2,2},db.total(42,3333));db.retired();
    }
    @Test void identicalUuidWithAnyChangedEventFieldFailsClosed() throws Exception {
        var db=new Database();var original=event();db.persist(original);
        var changed=List.of(
                new Event(ID,8,11,19,1,514,123456789,original.participants()),
                new Event(ID,7,12,19,1,514,123456789,original.participants()),
                new Event(ID,7,11,20,1,514,123456789,original.participants()),
                new Event(ID,7,11,19,1,514,123456790,original.participants()),
                new Event(ID,7,11,19,1,514,123456789,List.of(member(1,7,3333,0,Verdict.LOSS),member(2,42,3333,1,Verdict.WIN))));
        for(var value:changed)failure(Failure.CONFLICT,()->db.persist(value));
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));assertEquals(1,db.data.ledgers.size());db.retired();
    }
    @Test void concurrentDuplicateTransactionsOnlyCreditOneResult() throws Exception {
        var db=new Database();var workers=Executors.newFixedThreadPool(2);
        try {
            var first=workers.submit(()->{db.persist(event());return true;});var second=workers.submit(()->{db.persist(event());return true;});
            assertTrue(first.get(3,TimeUnit.SECONDS));assertTrue(second.get(3,TimeUnit.SECONDS));
        }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}
        assertEquals(1,db.data.ledgers.size());assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void failureAfterFirstMemberRollsBackEntireMatchBeforeRetry() throws Exception {
        var db=new Database();db.failMember=2;failure(Failure.UNAVAILABLE,()->db.persist(event()));
        assertEquals(1,db.rollbacks);assertTrue(db.data.ledgers.isEmpty());assertTrue(db.data.totals.isEmpty());assertTrue(db.data.members.isEmpty());
        db.failMember=0;db.persist(event());assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void commitLostBeforeApplyingRemainsUnknownAndSameKeyRetryAppliesOnce() throws Exception {
        var db=new Database();db.commitFault="before";failure(Failure.COMMIT_UNKNOWN,()->db.persist(event()));
        assertTrue(db.data.ledgers.isEmpty());assertEquals(0,db.rollbacks); // No rollback claim after commit attempt.
        db.commitFault="";db.persist(event());db.persist(event());assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void commitAppliedButReplyLostRetriesExistingLedgerWithoutDoubleCount() throws Exception {
        var db=new Database();db.commitFault="after";failure(Failure.COMMIT_UNKNOWN,()->db.persist(event()));
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));assertEquals(0,db.rollbacks);
        db.commitFault="";db.persist(event());assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void connectionCloseFailureAfterCommitIsNotReportedAsRolledBack() throws Exception {
        var db=new Database();db.failConnectionClose=true;failure(Failure.COMMIT_UNKNOWN,()->db.persist(event()));
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.failConnectionClose=false;db.persist(event());
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void failedRollbackStillRetiresOwnedConnectionAndDoesNotPublishSuccess() {
        var db=new Database();db.failMember=2;db.failRollback=true;failure(Failure.UNAVAILABLE,()->db.persist(event()));
        assertTrue(db.data.ledgers.isEmpty());assertTrue(db.data.totals.isEmpty());db.retired();
    }
    @Test void checkedCounterOverflowCannotPartiallyCreditOtherPlayers() {
        var db=new Database();db.seed(42,3333,0,Long.MAX_VALUE,Long.MAX_VALUE);
        failure(Failure.OVERFLOW,()->db.persist(event()));assertTrue(db.data.ledgers.isEmpty());assertFalse(db.data.totals.containsKey("7:3333"));
        assertArrayEquals(new long[]{0,Long.MAX_VALUE,Long.MAX_VALUE},db.total(42,3333));db.retired();
    }
    @Test void fullI64AndTwoSourcesWithIdenticalObjectIdsAreNotNarrowed() throws Exception {
        var db=new Database();var value=new Event(ID,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,1,514,Long.MAX_VALUE,
                List.of(member(1,7,Long.MAX_VALUE,0,Verdict.WIN),member(2,Integer.MAX_VALUE,Long.MAX_VALUE,1,Verdict.LOSS)));
        db.persist(value);db.persist(value);assertArrayEquals(new long[]{1,0,1},db.total(7,Long.MAX_VALUE));
        assertArrayEquals(new long[]{0,1,1},db.total(Integer.MAX_VALUE,Long.MAX_VALUE));db.retired();
    }
    @Test void oneHundredMemberTransactionAndDuplicateAreComplete() throws Exception {
        var rows=new ArrayList<Participant>();for(int i=1;i<=100;i++)rows.add(member(i,7,i,i%2,i%2==0?Verdict.WIN:Verdict.LOSS));
        var db=new Database();var event=new Event(ID,1,1,1,1,514,0,rows);db.persist(event);db.persist(event);
        assertEquals(100,db.data.totals.size());assertEquals(100,db.data.members.get(key(ModernMatchSettlementJdbc.uuid(event))).size());db.retired();
    }
    @Test void corruptedLedgerHashHeadersOrParticipantRowsAreNeverAcceptedAsDuplicateSuccess() throws Exception {
        for(String corruption:List.of("hash","body-length","header","missing-member","different-member","extra-member")) {
            var db=new Database();db.persist(event());String id=key(ModernMatchSettlementJdbc.uuid(event()));var ledger=db.data.ledgers.get(id);
            switch(corruption) {
                case "hash"->ledger.put("event_hash",new byte[32]);case "body-length"->ledger.put("body_length",4097L);
                case "header"->ledger.put("match_id",999L);case "missing-member"->db.data.members.get(id).remove(1);
                case "different-member"->db.data.members.get(id).get(1).put("source_id",7L);
                case "extra-member"->db.data.members.get(id).add(copy(db.data.members.get(id).get(1)));
            }
            failure(Failure.INVALID_STORAGE,()->db.persist(event()));assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
        }
    }
    @Test void missingOrImpossibleTotalsOnDuplicateAreStorageFailure() throws Exception {
        var missing=new Database();missing.persist(event());missing.data.totals.remove("7:3333");failure(Failure.INVALID_STORAGE,()->missing.persist(event()));missing.retired();
        var broken=new Database();broken.persist(event());broken.seed(7,3333,0,0,0);
        failure(Failure.INVALID_STORAGE,()->broken.persist(event()));broken.retired();
    }
    @Test void malformedNumericCountersRejectRatherThanRoundOrClamp() {
        for(Object value:Arrays.asList(-1L,null,new BigDecimal("1.5"),1.0,new BigInteger("9223372036854775808"))) {
            var db=new Database();db.seed(7,3333,0,0,0);db.data.totals.get("7:3333").put("wins",value);
            assertThrows(SettlementException.class,()->db.persist(event()));assertTrue(db.data.ledgers.isEmpty());db.retired();
        }
    }
    @Test void metadataFencePinsAllThreeTablesInsideTransactionBeforeSchemaAndWrites() throws Exception {
        var db=new Database();db.persist(event());db.persist(event());
        assertEquals(2,db.connections.size());
        for(var owned:db.connections)assertEquals(3,owned.pinnedTables);
        db.retired();
    }
    @Test void schemaRequiresAllThreeInnoDbTablesBeforeAnyWrite() {
        for(String error:List.of("myisam","missing","duplicate","unknown")) {
            var db=new Database();switch(error) {
                case "myisam"->db.tableRows.get(0).put("ENGINE","MyISAM");case "missing"->db.tableRows.remove(0);
                case "duplicate"->db.tableRows.add(copy(db.tableRows.get(0)));case "unknown"->db.tableRows.get(0).put("TABLE_NAME","native_table");
            }
            failure(Failure.INVALID_STORAGE,()->db.persist(event()));assertEquals(0,db.writes);assertEquals(1,db.begins);assertEquals(1,db.rollbacks);db.retired();
        }
    }
    @Test void metadataColumnTypesNullabilityBoundsAndUniqueKeysAreStrict() {
        for(String error:List.of("unsigned","decimal","null","width","missing","duplicate","oversize","wrong-key","extra-unique","missing-key")) {
            var db=new Database();switch(error) {
                case "unsigned"->db.columnRows.stream().filter(r->r.get("DATA_TYPE").equals("bigint")).findFirst().orElseThrow().put("COLUMN_TYPE","bigint(20) unsigned");
                case "decimal"->db.columnRows.get(0).put("DATA_TYPE","decimal");case "null"->db.columnRows.get(0).put("IS_NULLABLE","YES");
                case "width"->db.columnRows.get(0).put("CHARACTER_MAXIMUM_LENGTH",17L);case "missing"->db.columnRows.remove(0);
                case "duplicate"->db.columnRows.add(copy(db.columnRows.get(0)));case "oversize"->db.columnRows.get(0).put("COLUMN_TYPE","a".repeat(65));
                case "wrong-key"->db.keyRows.get(0).put("COLUMN_NAME","object_id");
                case "extra-unique"->{var extra=copy(db.keyRows.get(0));extra.put("INDEX_NAME","unexpected_unique");db.keyRows.add(extra);}
                case "missing-key"->db.keyRows.remove(0);
            }
            failure(Failure.INVALID_STORAGE,()->db.persist(event()));assertEquals(0,db.writes);db.retired();
        }
    }
    @Test void metadataNamesMayBeCaseInsensitiveButCannotIntroduceAnotherTable() throws Exception {
        var db=new Database();for(var rows:List.of(db.tableRows,db.columnRows,db.keyRows))for(var row:rows)
            row.replaceAll((k,v)->v instanceof String text?text.toUpperCase(java.util.Locale.ROOT):v);
        db.persist(event());assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void binaryUuidPrefixIndexesAreNotEquivalentToTheFullPrimaryOrUniqueKeys() {
        for(String index:List.of(ModernMatchSettlementJdbc.MATCH+".PRIMARY",
                ModernMatchSettlementJdbc.MEMBER+".PRIMARY",ModernMatchSettlementJdbc.MEMBER+".uq_modern_match_entity")) {
            for(long prefix:new long[]{0,1,8,16,Long.MAX_VALUE}) {
                var db=new Database();
                db.keyRows.stream().filter(row->(row.get("TABLE_NAME")+"."+row.get("INDEX_NAME")).equals(index)
                        && row.get("COLUMN_NAME").equals("result_id")).findFirst().orElseThrow().put("SUB_PART",prefix);
                failure(Failure.INVALID_STORAGE,()->db.persist(event()));assertEquals(0,db.writes);assertEquals(1,db.rollbacks);db.retired();
            }
        }
    }
    @Test void absentOrMalformedSubPartMetadataFailsBeforeAnyWriteRatherThanMeaningFullKey() {
        for(String scenario:List.of("missing","fractional","text","approximate")) {
            var db=new Database();var row=db.keyRows.get(0);
            switch(scenario) {
                case "missing"->row.remove("SUB_PART");
                case "fractional"->row.put("SUB_PART",new java.math.BigDecimal("1.5"));
                case "text"->row.put("SUB_PART","NULL");
                case "approximate"->row.put("SUB_PART",0.0d);
            }
            failure(Failure.UNAVAILABLE,()->db.persist(event()));assertEquals(0,db.writes);assertEquals(1,db.rollbacks);db.retired();
        }
    }
    @Test void duplicateSqlStateIsRecognizedOnlyForCleanLedgerExecuteNotSetupOrOtherWrites() {
        for(String fault:List.of("setup-duplicate","ledger-wrong-state","ledger-wrong-code","ledger-suppressed","ledger-next","member-duplicate")) {
            var db=new Database();db.fault=fault;failure(Failure.UNAVAILABLE,()->db.persist(event()));assertTrue(db.data.ledgers.isEmpty());db.retired();
        }
    }
    @Test void duplicateLedgerStatementCloseFailureCannotBeSwallowedAsSuccess() throws Exception {
        var db=new Database();db.persist(event());db.fault="duplicate-close";failure(Failure.UNAVAILABLE,()->db.persist(event()));
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));db.retired();
    }
    @Test void deadlineNeverResetsAcrossQueriesAndExpiredAdmissionDoesNotOpenConnection() {
        var db=new Database();failure(Failure.DEADLINE,()->db.store().persist(event(),0));assertEquals(0,db.opens);
        failure(Failure.DEADLINE,()->db.store().persist(event(),TimeUnit.SECONDS.toNanos(61)));assertEquals(0,db.opens);
        db.expireAfter=5;failure(Failure.DEADLINE,()->db.persist(event()));assertTrue(db.data.ledgers.isEmpty());
        assertEquals(List.of(DEADLINE),db.deadlines);db.retired();
    }
    @Test void factoryFailureReadOnlyOrExistingTransactionAreNotAccepted() {
        var db=new Database();db.factoryFailure=true;failure(Failure.UNAVAILABLE,()->db.persist(event()));
        for(String fault:List.of("read-only","existing-transaction")) {
            var instance=new Database();instance.fault=fault;failure(Failure.INVALID_STORAGE,()->instance.persist(event()));
            assertEquals(0,instance.writes);instance.retired();
        }
        assertThrows(IllegalArgumentException.class,()->new ModernMatchSettlementJdbc(db,"bad`schema"));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchSettlementJdbc(db,"db.other"));
    }

    @FunctionalInterface private interface Checked {void run() throws Exception;}
    private static void failure(Failure expected,Checked action) {
        var failure=assertThrows(SettlementException.class,action::run);assertEquals(expected,failure.failure());
        assertNull(failure.getCause());assertEquals(0,failure.getSuppressed().length);
        assertFalse(failure.getMessage().contains("SYNTHETIC-SECRET"));
    }
    private static String key(byte[] bytes){return java.util.HexFormat.of().formatHex(bytes);}
    private static Map<String,Object> copy(Map<String,Object> source) {
        var result=new LinkedHashMap<String,Object>();source.forEach((key,value)->result.put(key,value instanceof byte[] bytes?bytes.clone():value));return result;
    }
    static final class Data {
        final Map<String,Map<String,Object>> ledgers=new HashMap<>(),totals=new HashMap<>();
        final Map<String,List<Map<String,Object>>> members=new HashMap<>();
        Data copy() {var value=new Data();ledgers.forEach((k,v)->value.ledgers.put(k,ModernMatchSettlementJdbcTest.copy(v)));
            totals.forEach((k,v)->value.totals.put(k,ModernMatchSettlementJdbcTest.copy(v)));
            members.forEach((k,v)->value.members.put(k,new ArrayList<>(v.stream().map(ModernMatchSettlementJdbcTest::copy).toList())));return value;}
    }
    static final class Database implements ModernMatchSettlementJdbc.ConnectionFactory {
        volatile Data data=new Data();final AtomicLong clock=new AtomicLong();final ReentrantLock lock=new ReentrantLock();
        final List<String> sql=java.util.Collections.synchronizedList(new ArrayList<>());
        final List<Long> deadlines=java.util.Collections.synchronizedList(new ArrayList<>());
        final List<Owned> connections=java.util.Collections.synchronizedList(new ArrayList<>());
        final List<Map<String,Object>> tableRows=new ArrayList<>(),columnRows=new ArrayList<>(),keyRows=new ArrayList<>();
        int opens,begins,commits,rollbacks,writes,queries,statementsOpened,statementsClosed,resultsOpened,resultsClosed;
        int failMember,expireAfter;String commitFault="",fault="";boolean failRollback,failConnectionClose,factoryFailure;
        Database() {
            for(String name:List.of(ModernMatchSettlementJdbc.MATCH,ModernMatchSettlementJdbc.MEMBER,ModernMatchSettlementJdbc.TOTAL))tableRows.add(new HashMap<>(Map.of("TABLE_NAME",name,"ENGINE","InnoDB")));
            String match=ModernMatchSettlementJdbc.MATCH,member=ModernMatchSettlementJdbc.MEMBER,total=ModernMatchSettlementJdbc.TOTAL;
            column(match,"result_id","binary",16);for(String n:List.of("match_id","generation","engine_revision","finished_at_millis"))column(match,n,"bigint",0);
            for(String n:List.of("mode","map_id","participant_count"))column(match,n,"int",0);column(match,"event_hash","binary",32);column(match,"event_body","varbinary",4096);
            column(member,"result_id","binary",16);column(member,"entity_id","bigint",0);column(member,"source_id","int",0);column(member,"object_id","bigint",0);
            column(member,"team","tinyint",0);column(member,"verdict","tinyint",0);
            column(total,"source_id","int",0);for(String n:List.of("object_id","wins","losses","revision"))column(total,n,"bigint",0);
            index(match,"PRIMARY","result_id");index(member,"PRIMARY","result_id","source_id","object_id");
            index(member,"uq_modern_match_entity","result_id","entity_id");index(total,"PRIMARY","source_id","object_id");
        }
        void column(String table,String name,String type,int length) {var row=new HashMap<String,Object>();row.put("TABLE_NAME",table);row.put("COLUMN_NAME",name);row.put("DATA_TYPE",type);
            row.put("COLUMN_TYPE",length>0?type+"("+length+")":type+"(20)");row.put("IS_NULLABLE","NO");row.put("CHARACTER_MAXIMUM_LENGTH",length>0?(long)length:null);columnRows.add(row);}
        void index(String table,String index,String... columns){for(int i=0;i<columns.length;i++){
            var row=new HashMap<String,Object>(Map.of("TABLE_NAME",table,"INDEX_NAME",index,"SEQ_IN_INDEX",(long)i+1,"COLUMN_NAME",columns[i]));
            row.put("SUB_PART",null);keyRows.add(row);
        }}
        ModernMatchSettlementJdbc store(){return new ModernMatchSettlementJdbc(this,"settlement",clock::get);}
        void persist(Event event) throws Exception {store().persist(event,DEADLINE);}
        void seed(int source,long object,long wins,long losses,long revision){data.totals.put(source+":"+object,new HashMap<>(Map.of("wins",wins,"losses",losses,"revision",revision)));}
        long[] total(int source,long object){var row=data.totals.get(source+":"+object);return new long[]{((Number)row.get("wins")).longValue(),((Number)row.get("losses")).longValue(),((Number)row.get("revision")).longValue()};}
        @Override public synchronized Connection open(long deadline) throws Exception {
            opens++;deadlines.add(deadline);if(factoryFailure)throw secret();var owned=new Owned();connections.add(owned);return owned.proxy();
        }
        void retired(){assertEquals(statementsOpened,statementsClosed);assertEquals(resultsOpened,resultsClosed);assertTrue(connections.stream().allMatch(c->c.closed));assertFalse(lock.isLocked());}
        private SQLException secret(){return new SQLException("SYNTHETIC-SECRET database address/password");}
        private SQLException duplicate(){return new SQLException("SYNTHETIC-SECRET duplicate","23000",1062);}
        final class Owned {
            boolean closed,readonly=true,auto=!fault.equals("existing-transaction"),transaction;Data work;int memberWrites,pinnedTables,ownedWrites;
            Connection proxy(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->switch(m.getName()){
                case "isClosed"->closed;case "isReadOnly"->readonly;
                case "setReadOnly"->{if(!fault.equals("read-only"))readonly=(Boolean)a[0];yield null;}
                case "getAutoCommit"->auto;
                case "setAutoCommit"->{assertFalse((Boolean)a[0]);assertTrue(lock.tryLock(3,TimeUnit.SECONDS));transaction=true;auto=false;work=data.copy();begins++;yield null;}
                case "setTransactionIsolation"->{assertEquals(Connection.TRANSACTION_READ_COMMITTED,a[0]);yield null;}
                case "setNetworkTimeout"->{assertTrue((Integer)a[1]>0);assertTrue((Integer)a[1]<=10000);yield null;}
                case "prepareStatement"->{String text=(String)a[0];sql.add(text);synchronized(Database.this){statementsOpened++;}yield statement(text);}
                case "commit"->{commits++;if(commitFault.equals("before"))throw secret();data=work.copy();release();if(commitFault.equals("after"))throw secret();yield null;}
                case "rollback"->{rollbacks++;if(failRollback)throw secret();release();yield null;}
                case "close"->{closed=true;release();if(failConnectionClose)throw secret();yield null;}
                default->throw new AssertionError("unexpected JDBC connection method "+m.getName());
            });}
            void release(){if(transaction){transaction=false;lock.unlock();}}
            PreparedStatement statement(String text) {
                var params=new HashMap<Integer,Object>();boolean[] wasClosed={false};
                return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()){
                    case "setInt","setLong","setString","setBytes"->{params.put((Integer)a[0],a[1] instanceof byte[] bytes?bytes.clone():a[1]);yield null;}
                    case "setMaxRows"->{assertTrue((Integer)a[0]>=1&&(Integer)a[0]<=101);if(fault.equals("setup-duplicate")&&text.startsWith("INSERT INTO `settlement`.`lito_modern_match`"))throw duplicate();yield null;}
                    case "setQueryTimeout"->{assertTrue((Integer)a[0]>=1&&(Integer)a[0]<=10);yield null;}
                    case "executeQuery"->{tick();yield result(query(text,params));}
                    case "executeUpdate"->{tick();writes++;ownedWrites++;yield update(text,params);}
                    case "close"->{assertFalse(wasClosed[0]);wasClosed[0]=true;synchronized(Database.this){statementsClosed++;}
                        if(fault.equals("duplicate-close")&&text.startsWith("INSERT INTO `settlement`.`lito_modern_match`"))throw secret();yield null;}
                    default->throw new AssertionError("unexpected JDBC statement method "+m.getName());
                });
            }
            void tick(){queries++;if(expireAfter>0&&queries>=expireAfter)clock.set(DEADLINE);}
            int update(String text,Map<Integer,Object> p) throws SQLException {
                assertTrue(transaction);assertFalse(readonly);assertEquals(3,pinnedTables);
                if(text.startsWith("INSERT INTO `settlement`.`lito_modern_match`")) {
                    if(fault.equals("ledger-wrong-state"))throw new SQLException("SYNTHETIC-SECRET","42000",1062);
                    if(fault.equals("ledger-wrong-code"))throw new SQLException("SYNTHETIC-SECRET","23000",1022);
                    if(fault.equals("ledger-suppressed")){var ex=duplicate();ex.addSuppressed(secret());throw ex;}
                    if(fault.equals("ledger-next")){var ex=duplicate();ex.setNextException(secret());throw ex;}
                    String key=key((byte[])p.get(1));if(work.ledgers.containsKey(key))throw duplicate();
                    var row=new HashMap<String,Object>();String[] fields={"match_id","generation","engine_revision","mode","map_id","finished_at_millis","participant_count","event_hash","event_body"};
                    for(int i=0;i<fields.length;i++)row.put(fields[i],p.get(i+2));row.put("body_length",(long)((byte[])p.get(10)).length);work.ledgers.put(key,copy(row));return 1;
                }
                if(text.startsWith("INSERT INTO `settlement`.`lito_modern_character_record`")) {
                    String key=p.get(1)+":"+p.get(2);if(work.totals.containsKey(key))return 0;
                    work.totals.put(key,new HashMap<>(Map.of("wins",0L,"losses",0L,"revision",0L)));return 1;
                }
                if(text.startsWith("UPDATE `settlement`.`lito_modern_character_record`")) {
                    String key=p.get(4)+":"+p.get(5);var row=work.totals.get(key);
                    if(row==null||!row.get("revision").equals(p.get(6)))return 0;
                    row.put("wins",p.get(1));row.put("losses",p.get(2));row.put("revision",p.get(3));return 1;
                }
                if(text.startsWith("INSERT INTO `settlement`.`lito_modern_match_participant`")) {
                    if(++memberWrites==failMember)throw secret();if(fault.equals("member-duplicate"))throw duplicate();
                    var row=new HashMap<String,Object>();String[] fields={"entity_id","source_id","object_id","team","verdict"};
                    for(int i=0;i<fields.length;i++)row.put(fields[i],p.get(i+2));
                    work.members.computeIfAbsent(key((byte[])p.get(1)),ignored->new ArrayList<>()).add(row);return 1;
                }
                throw new AssertionError("unexpected update SQL "+text);
            }
            List<Map<String,Object>> query(String text,Map<Integer,Object> p) {
                assertTrue(transaction);
                if(text.endsWith(" LIMIT 0")) {
                    assertTrue(p.isEmpty());
                    assertEquals(0,ownedWrites);
                    assertTrue(text.contains("`settlement`.`lito_modern_"));
                    String name=List.of(ModernMatchSettlementJdbc.MATCH,ModernMatchSettlementJdbc.MEMBER,ModernMatchSettlementJdbc.TOTAL).get(pinnedTables++);
                    assertTrue(text.contains("`settlement`.`"+name+"`"));
                    return List.of();
                }
                if(text.contains("INFORMATION_SCHEMA")) {
                    assertEquals(3,pinnedTables);
                    assertEquals("settlement",p.get(1));
                    if(text.contains("INFORMATION_SCHEMA.TABLES"))return tableRows;
                    if(text.contains("INFORMATION_SCHEMA.COLUMNS"))return columnRows;
                    if(text.contains("INFORMATION_SCHEMA.STATISTICS"))return keyRows;
                }
                if(text.startsWith("SELECT wins,losses,revision")){var row=work.totals.get(p.get(1)+":"+p.get(2));return row==null?List.of():List.of(row);}
                if(text.startsWith("SELECT match_id")){var row=work.ledgers.get(key((byte[])p.get(1)));return row==null?List.of():List.of(row);}
                if(text.startsWith("SELECT entity_id"))return work.members.getOrDefault(key((byte[])p.get(1)),List.of());
                throw new AssertionError("unexpected query SQL "+text);
            }
            ResultSet result(List<Map<String,Object>> rows) {
                synchronized(Database.this){resultsOpened++;}int[] index={-1};boolean[] wasNull={false},closed={false};
                var columns=new ModernJdbcNumbersTest.Columns(name->{if(!rows.get(index[0]).containsKey(name))throw new SQLException("missing fixture column");return rows.get(index[0]).get(name);});
                return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                    case "next"->++index[0]<rows.size();case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();case "wasNull"->wasNull[0];
                    case "getString","getBytes","getBigDecimal"->{Object value=rows.get(index[0]).get(columns.name(a[0]));wasNull[0]=value==null;
                        if(m.getName().equals("getString"))yield value==null?null:value.toString();
                        if(m.getName().equals("getBytes"))yield value==null?null:((byte[])value).clone();yield ModernJdbcNumbersTest.Columns.decimal(value);}
                    case "close"->{assertFalse(closed[0]);closed[0]=true;synchronized(Database.this){resultsClosed++;}yield null;}
                    default->throw new AssertionError("unexpected JDBC rows method "+m.getName());
                });
            }
        }
    }
}
