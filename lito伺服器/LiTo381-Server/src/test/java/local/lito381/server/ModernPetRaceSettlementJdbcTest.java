package local.lito381.server;

import static local.lito381.server.ModernPetRaceSettlement.*;
import static local.lito381.server.ModernPetRacePersistenceTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

/** Synthetic transactional JDBC only; no server, database, network or source rewards. */
class ModernPetRaceSettlementJdbcTest {
    static final long DEADLINE=TimeUnit.SECONDS.toNanos(10);
    @Test void firstInsertStoresAllOrderedRowsWithoutTotalsOrSourceRewards()throws Exception {
        var db=new Database();db.persist(event());assertEquals(1,db.data.results.size());
        var rows=db.data.members.values().iterator().next();assertEquals(2,rows.size());
        assertEquals(List.of(7,42),rows.stream().map(r->r.get("source_id")).toList());
        assertEquals(List.of(3333L,3333L),rows.stream().map(r->r.get("object_id")).toList());
        assertEquals(1,db.commits);assertEquals(3,db.writes);assertEquals(List.of(DEADLINE),db.deadlines);
        assertTrue(db.sql.stream().noneMatch(s->s.startsWith("UPDATE")||s.contains("character_record")||s.contains("character_items")||s.startsWith("CREATE")));
        assertTrue(db.sql.stream().filter(s->s.startsWith("INSERT")).allMatch(s->s.contains("\u0060race_db\u0060.\u0060lito_modern_pet_race_")));
        db.retired();
    }
    @Test void restartReplayChecksWholeBodyAndRowsButRetainsFirstRecordedAt()throws Exception {
        var db=new Database();var first=event();db.persist(first);
        for(int i=1;i<4;i++)db.persist(recorded(first,first.recordedAtMillis()+i));
        assertEquals(1,db.data.results.size());assertEquals(first.recordedAtMillis(),db.data.results.values().iterator().next().get("recorded_at_millis"));
        assertEquals(2,db.data.members.values().iterator().next().size());assertEquals(4,db.commits);db.retired();
    }
    @Test void sameUuidDifferentAuthorityConflictsWithoutChangingEarlierRows()throws Exception {
        var db=new Database();db.persist(event());failure(Failure.CONFLICT,()->db.persist(event(ID,8,123456789)));
        assertEquals(1,db.data.results.size());assertEquals(2,db.data.members.values().iterator().next().size());db.retired();
    }
    @Test void concurrentDuplicateTransactionsInsertOnlyOneFullResult()throws Exception {
        var db=new Database();var workers=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a=workers.submit(()->{db.persist(event());return true;});
            var b=workers.submit(()->{db.persist(recorded(event(),999));return true;});
            assertTrue(a.get(3,TimeUnit.SECONDS));assertTrue(b.get(3,TimeUnit.SECONDS));
        }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}
        assertEquals(1,db.data.results.size());assertEquals(2,db.data.members.values().iterator().next().size());db.retired();
    }
    @Test void failureAfterOneParticipantRollsBackResultAndEveryParticipant() {
        var db=new Database();db.failMember=2;failure(Failure.UNAVAILABLE,()->db.persist(event()));
        assertTrue(db.data.results.isEmpty());assertTrue(db.data.members.isEmpty());assertEquals(1,db.rollbacks);db.retired();
    }
    @Test void unknownCommitBeforeOrAfterApplyOnlyRetriesTheExactUuid()throws Exception {
        for(String fault:List.of("before","after")) {
            var db=new Database();db.commitFault=fault;failure(Failure.COMMIT_UNKNOWN,()->db.persist(event()));
            assertEquals(fault.equals("after")?1:0,db.data.results.size());assertEquals(0,db.rollbacks);
            db.commitFault="";db.persist(event());db.persist(event());
            assertEquals(1,db.data.results.size());assertEquals(2,db.data.members.values().iterator().next().size());db.retired();
        }
    }
    @Test void closeFailureAfterReturnedCommitRemainsUnknownAndReplayPreservesAudit()throws Exception {
        var db=new Database();db.failClose=true;failure(Failure.COMMIT_UNKNOWN,()->db.persist(event()));
        db.failClose=false;db.persist(recorded(event(),999));assertEquals(event().recordedAtMillis(),db.data.results.values().iterator().next().get("recorded_at_millis"));db.retired();
    }
    @Test void corruptFullRowsOrStoredHeadersAreNeverDuplicateSuccess()throws Exception {
        for(String kind:List.of("hash","body-length","header","audit","missing","source","extra","placement")) {
            var db=new Database();db.persist(event());var ledger=db.data.results.values().iterator().next();var rows=db.data.members.values().iterator().next();
            switch(kind) {
                case "hash"->ledger.put("content_hash",new byte[32]);case "body-length"->ledger.put("body_length",4097L);
                case "header"->ledger.put("terminal_revision",999L);case "audit"->ledger.put("recorded_at_millis",-1L);
                case "missing"->rows.remove(1);case "source"->rows.get(1).put("source_id",7);
                case "extra"->rows.add(copy(rows.get(1)));case "placement"->rows.get(0).put("placement",2L);
            }
            failure(Failure.INVALID_STORAGE,()->db.persist(event()));db.retired();
        }
    }
    @Test void malformedSchemaOrBinaryPrefixKeyFailsBeforeAnyWrites() {
        for(String kind:List.of("myisam","missing-table","unknown-column","unsigned","nullable","width","missing-column","extra-key","prefix")) {
            var db=new Database();
            switch(kind) {
                case "myisam"->db.tables.get(0).put("ENGINE","MyISAM");case "missing-table"->db.tables.remove(0);
                case "unknown-column"->db.columns.get(0).put("COLUMN_NAME","wins");
                case "unsigned"->db.columns.stream().filter(r->r.get("DATA_TYPE").equals("bigint")).findFirst().orElseThrow().put("COLUMN_TYPE","bigint unsigned");
                case "nullable"->db.columns.get(0).put("IS_NULLABLE","YES");case "width"->db.columns.get(0).put("CHARACTER_MAXIMUM_LENGTH",8L);
                case "missing-column"->db.columns.remove(0);case "extra-key"->db.keys.get(0).put("INDEX_NAME","wrong");
                case "prefix"->db.keys.get(0).put("SUB_PART",8L);
            }
            failure(Failure.INVALID_STORAGE,()->db.persist(event()));assertEquals(0,db.writes);db.retired();
        }
    }
    @Test void duplicateOnlyMeansCleanResultInsertNotMemberOrSetupFailure() {
        for(String fault:List.of("setup","wrong-state","suppressed","member-duplicate")) {
            var db=new Database();db.fault=fault;failure(Failure.UNAVAILABLE,()->db.persist(event()));assertTrue(db.data.results.isEmpty());db.retired();
        }
    }
    @Test void deadlineIsOneBudgetIncludingMetadataAndNeverOpensWhenAlreadyExpired() {
        var db=new Database();failure(Failure.DEADLINE,()->db.store().persist(event(),0));
        failure(Failure.DEADLINE,()->db.store().persist(event(),TimeUnit.SECONDS.toNanos(61)));assertEquals(0,db.opens);
        db.expireAfter=5;failure(Failure.DEADLINE,()->db.persist(event()));assertTrue(db.data.results.isEmpty());db.retired();
    }
    @Test void invalidDestinationIsRejectedBeforeOpeningAndNoExistingTransactionIsBorrowed() {
        var db=new Database();assertThrows(IllegalArgumentException.class,()->new ModernPetRaceSettlementJdbc(db,"bad\u0060db",db.clock::get));
        assertEquals(0,db.opens);db.fault="transaction";failure(Failure.INVALID_STORAGE,()->db.persist(event()));db.retired();
    }
    @Test void fullTenRunnerRosterAndI64SourceObjectsAreNeverNarrowed()throws Exception {
        var sample=event();var rows=new ArrayList<ModernPetRaceProtocol.ResultRow>();
        for(int i=0;i<10;i++)rows.add(new ModernPetRaceProtocol.ResultRow(
                new TournamentPresence.CharacterKey(i<5?7:42,Long.MAX_VALUE-4+i%5),
                new ModernPetRace.Runner(i+1,i==0?11:20,i==0?24:0,i==0?4:0,0,i==0?1:0,
                        i==0?1_000_000_000L:0,i==0?ModernPetRace.Dnf.NONE:ModernPetRace.Dnf.WINDOW_CLOSED,2145)));
        String hash=ModernPetRaceProtocol.resultHash(ID,Long.MAX_VALUE,Long.MAX_VALUE,20,new ModernWorldBinding.Key(0,565),
                sample.manifest(),sample.courseSha256(),1,11_000_000_000L,rows);
        var large=new Event(ID,Long.MAX_VALUE,Long.MAX_VALUE,20,565,sample.manifest(),sample.stepIntervalNanos(),sample.gates(),
                sample.courseSha256(),1,11_000_000_000L,rows,hash,Long.MAX_VALUE);
        var db=new Database();db.persist(large);db.persist(recorded(large,0));
        assertEquals(10,db.data.members.values().iterator().next().size());
        assertEquals(Long.MAX_VALUE,db.data.results.values().iterator().next().get("recorded_at_millis"));db.retired();
    }
    @FunctionalInterface interface Checked{void run()throws Exception;}
    static void failure(Failure value,Checked action){var e=assertThrows(SettlementException.class,action::run);assertEquals(value,e.failure());assertNull(e.getCause());assertFalse(e.getMessage().contains("SECRET"));}
    static Map<String,Object> copy(Map<String,Object> old){var next=new LinkedHashMap<String,Object>();old.forEach((k,v)->next.put(k,v instanceof byte[] b?b.clone():v));return next;}
    static String id(Object value){return java.util.HexFormat.of().formatHex((byte[])value);}
    static final class Data {
        final Map<String,Map<String,Object>> results=new HashMap<>();final Map<String,List<Map<String,Object>>> members=new HashMap<>();
        Data copy(){var n=new Data();results.forEach((k,v)->n.results.put(k,ModernPetRaceSettlementJdbcTest.copy(v)));
            members.forEach((k,v)->n.members.put(k,new ArrayList<>(v.stream().map(ModernPetRaceSettlementJdbcTest::copy).toList())));return n;}
    }
    static final class Database implements ModernPetRaceSettlementJdbc.ConnectionFactory {
        Data data=new Data();final AtomicLong clock=new AtomicLong();final ReentrantLock lock=new ReentrantLock();
        final List<String> sql=java.util.Collections.synchronizedList(new ArrayList<>());final List<Long> deadlines=new ArrayList<>();final List<Owned> owned=new ArrayList<>();
        final List<Map<String,Object>> tables=new ArrayList<>(),columns=new ArrayList<>(),keys=new ArrayList<>();
        int opens,commits,rollbacks,writes,operations,statements,closedStatements,results,closedResults;
        int failMember,expireAfter;String commitFault="",fault="";boolean failClose;
        Database(){
            String r=ModernPetRaceSettlementJdbc.RESULT,m=ModernPetRaceSettlementJdbc.MEMBER;
            for(String t:List.of(r,m))tables.add(new HashMap<>(Map.of("TABLE_NAME",t,"ENGINE","InnoDB")));
            column(r,"result_id","binary",16);
            for(String n:List.of("match_id","generation","terminal_revision","recorded_at_millis"))column(r,n,"bigint",0);
            for(String n:List.of("logical_map_id","physical_map_id","participant_count"))column(r,n,"int",0);
            column(r,"content_hash","binary",32);column(r,"event_body","varbinary",4096);
            column(m,"result_id","binary",16);column(m,"source_id","int",0);
            for(String n:List.of("entity_id","object_id","runner_revision","progress_move_sequence","completed_laps","next_checkpoint","placement","finish_elapsed_nanos","dnf_reason","base_graphics_id"))column(m,n,"bigint",0);
            index(r,"PRIMARY","result_id");index(m,"PRIMARY","result_id","source_id","object_id");index(m,"uq_pet_race_entity","result_id","entity_id");
        }
        void column(String table,String name,String type,int length){var row=new HashMap<String,Object>(Map.of("TABLE_NAME",table,"COLUMN_NAME",name,"DATA_TYPE",type,
                "COLUMN_TYPE",length==0?type+"(20)":type+"("+length+")","IS_NULLABLE","NO"));row.put("CHARACTER_MAXIMUM_LENGTH",length==0?null:(long)length);columns.add(row);}
        void index(String table,String name,String... fields){for(int i=0;i<fields.length;i++){var row=new HashMap<String,Object>(Map.of("TABLE_NAME",table,"INDEX_NAME",name,"SEQ_IN_INDEX",(long)i+1,"COLUMN_NAME",fields[i]));row.put("SUB_PART",null);keys.add(row);}}
        ModernPetRaceSettlementJdbc store(){return new ModernPetRaceSettlementJdbc(this,"race_db",clock::get);}
        void persist(Event e)throws Exception{store().persist(e,DEADLINE);}
        @Override public synchronized Connection open(long deadline){opens++;deadlines.add(deadline);var c=new Owned();owned.add(c);return c.proxy();}
        void retired(){assertEquals(statements,closedStatements);assertEquals(results,closedResults);assertTrue(owned.stream().allMatch(c->c.closed));assertFalse(lock.isLocked());}
        SQLException secret(){return new SQLException("SECRET synthetic JDBC failure");}SQLException duplicate(){return new SQLException("SECRET duplicate","23000",1062);}
        final class Owned {
            boolean closed,readOnly=true,auto=!fault.equals("transaction"),transaction;Data work;int pins,memberWrites;
            Connection proxy(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->switch(m.getName()){
                case "isClosed"->closed;case "getAutoCommit"->auto;case "isReadOnly"->readOnly;case "setReadOnly"->{readOnly=(Boolean)a[0];yield null;}
                case "setTransactionIsolation"->{assertEquals(Connection.TRANSACTION_READ_COMMITTED,a[0]);yield null;}
                case "setNetworkTimeout"->{assertTrue((Integer)a[1]>0&&(Integer)a[1]<=10000);yield null;}
                case "setAutoCommit"->{assertFalse((Boolean)a[0]);assertTrue(lock.tryLock(3,TimeUnit.SECONDS));transaction=true;auto=false;work=data.copy();yield null;}
                case "prepareStatement"->{String text=(String)a[0];sql.add(text);synchronized(Database.this){statements++;}yield statement(text);}
                case "commit"->{commits++;if(commitFault.equals("before"))throw secret();data=work.copy();release();if(commitFault.equals("after"))throw secret();yield null;}
                case "rollback"->{rollbacks++;release();yield null;}
                case "close"->{closed=true;release();if(failClose)throw secret();yield null;}
                default->throw new AssertionError("unexpected connection method "+m.getName());
            });}
            void release(){if(transaction){transaction=false;lock.unlock();}}
            PreparedStatement statement(String text){var params=new HashMap<Integer,Object>();boolean[] closed={false};
                return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()){
                    case "setInt","setLong","setString","setBytes"->{params.put((Integer)a[0],a[1] instanceof byte[] b?b.clone():a[1]);yield null;}
                    case "setMaxRows"->{assertTrue((Integer)a[0]>0&&(Integer)a[0]<=33);if(fault.equals("setup")&&text.startsWith("INSERT"))throw duplicate();yield null;}
                    case "setQueryTimeout"->{assertTrue((Integer)a[0]>=1&&(Integer)a[0]<=10);yield null;}
                    case "executeQuery"->{tick();yield result(query(text,params));}
                    case "executeUpdate"->{tick();writes++;yield update(text,params);}
                    case "close"->{assertFalse(closed[0]);closed[0]=true;synchronized(Database.this){closedStatements++;}yield null;}
                    default->throw new AssertionError("unexpected statement method "+m.getName());
                });}
            void tick(){operations++;if(expireAfter>0&&operations>=expireAfter)clock.set(DEADLINE);}
            int update(String text,Map<Integer,Object> p)throws SQLException {
                assertTrue(transaction);assertFalse(readOnly);assertEquals(2,pins);
                if(text.startsWith("INSERT INTO \u0060race_db\u0060.\u0060lito_modern_pet_race_result\u0060")){
                    if(fault.equals("wrong-state"))throw new SQLException("SECRET","42000",1062);
                    if(fault.equals("suppressed")){var e=duplicate();e.addSuppressed(secret());throw e;}
                    String key=id(p.get(1));if(work.results.containsKey(key))throw duplicate();
                    var row=new HashMap<String,Object>();String[] names={"match_id","generation","terminal_revision","logical_map_id","physical_map_id","recorded_at_millis","participant_count","content_hash","event_body"};
                    for(int i=0;i<names.length;i++)row.put(names[i],p.get(i+2));row.put("body_length",(long)((byte[])p.get(10)).length);work.results.put(key,copy(row));return 1;
                }
                if(text.startsWith("INSERT INTO \u0060race_db\u0060.\u0060lito_modern_pet_race_participant\u0060")){
                    if(++memberWrites==failMember)throw secret();if(fault.equals("member-duplicate"))throw duplicate();
                    var row=new HashMap<String,Object>();String[] names={"entity_id","source_id","object_id","runner_revision","progress_move_sequence","completed_laps","next_checkpoint","placement","finish_elapsed_nanos","dnf_reason","base_graphics_id"};
                    for(int i=0;i<names.length;i++)row.put(names[i],p.get(i+2));work.members.computeIfAbsent(id(p.get(1)),ignored->new ArrayList<>()).add(copy(row));return 1;
                }
                throw new AssertionError("unexpected update "+text);
            }
            List<Map<String,Object>> query(String text,Map<Integer,Object> p){
                assertTrue(transaction);
                if(text.endsWith(" LIMIT 0")){assertTrue(p.isEmpty());String expected=List.of(ModernPetRaceSettlementJdbc.RESULT,ModernPetRaceSettlementJdbc.MEMBER).get(pins++);assertTrue(text.contains(expected));return List.of();}
                if(text.contains("INFORMATION_SCHEMA")){assertEquals(2,pins);assertEquals("race_db",p.get(1));
                    if(text.contains("INFORMATION_SCHEMA.TABLES"))return tables;if(text.contains("INFORMATION_SCHEMA.COLUMNS"))return columns;if(text.contains("INFORMATION_SCHEMA.STATISTICS"))return keys;}
                if(text.startsWith("SELECT match_id")){var row=work.results.get(id(p.get(1)));return row==null?List.of():List.of(row);}
                if(text.startsWith("SELECT entity_id"))return work.members.getOrDefault(id(p.get(1)),List.of());
                throw new AssertionError("unexpected query "+text);
            }
            ResultSet result(List<Map<String,Object>> rows){synchronized(Database.this){results++;}int[] index={-1};boolean[] nullValue={false},closed={false};
                var columns=new ModernJdbcNumbersTest.Columns(name->{if(!rows.get(index[0]).containsKey(name))throw new SQLException("missing fixture column");return rows.get(index[0]).get(name);});
                return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                    case "next"->++index[0]<rows.size();case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();case "wasNull"->nullValue[0];
                    case "getString","getBytes","getBigDecimal"->{Object value=rows.get(index[0]).get(columns.name(a[0]));nullValue[0]=value==null;
                        if(m.getName().equals("getString"))yield value==null?null:value.toString();if(m.getName().equals("getBytes"))yield value==null?null:((byte[])value).clone();yield ModernJdbcNumbersTest.Columns.decimal(value);}
                    case "close"->{assertFalse(closed[0]);closed[0]=true;synchronized(Database.this){closedResults++;}yield null;}
                    default->throw new AssertionError("unexpected result method "+m.getName());
                });}
        }
    }
}
