package local.lito381.server;

import static local.lito381.server.ModernPetRaceSettlement.*;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Independent ranked-race ledger: no DM totals, source-table writes, rewards or runtime DDL. */
final class ModernPetRaceSettlementJdbc implements Store {
    @FunctionalInterface interface ConnectionFactory { Connection open(long absoluteDeadlineNanos) throws Exception; }
    static final int MAX_EVENT_BYTES = 4096;
    static final long MAX_DEADLINE_NANOS = TimeUnit.SECONDS.toNanos(60);
    static final String RESULT = "lito_modern_pet_race_result", MEMBER = "lito_modern_pet_race_participant";
    private static final String FILTER = "TABLE_SCHEMA=? AND TABLE_NAME IN ('"+RESULT+"','"+MEMBER+"')";
    private record Column(String type, int bytes) {}
    private static final Map<String, Map<String, Column>> SCHEMA = schema();
    private static final Map<String, List<String>> KEYS = Map.of(
            RESULT+".primary", List.of("result_id"), MEMBER+".primary", List.of("result_id","source_id","object_id"),
            MEMBER+".uq_pet_race_entity", List.of("result_id","entity_id"));
    private final ConnectionFactory factory;
    private final String database;
    private final LongSupplier clock;

    ModernPetRaceSettlementJdbc(ModernSettlementConnections owned, String database) {
        this(Objects.requireNonNull(owned)::open, database, System::nanoTime);
    }
    /** Only synthetic tests supply a factory; production uses the physically owned connection provider. */
    ModernPetRaceSettlementJdbc(ConnectionFactory factory, String database, LongSupplier clock) {
        this.factory=Objects.requireNonNull(factory); this.clock=Objects.requireNonNull(clock);
        if(database==null||!database.matches("[A-Za-z0-9_]{1,64}"))throw new IllegalArgumentException("invalid race settlement database");
        this.database=database;
    }
    @Override public void persist(Event event, long deadline) throws Exception {
        Objects.requireNonNull(event);
        long left=deadline-clock.getAsLong();
        if(left<=0||left>MAX_DEADLINE_NANOS||Thread.currentThread().isInterrupted())throw error(Failure.DEADLINE);
        // Validate the complete bounded replay payload before any connection I/O.
        byte[] envelope=ModernPetRaceOutbox.encodeEvent(event);
        Arrays.fill(envelope,(byte)0);
        byte[] body=event.authoritativeBody(), hash=java.util.HexFormat.of().parseHex(event.contentSha256());
        byte[] id=ByteBuffer.allocate(16).putLong(event.resultId().getMostSignificantBits()).putLong(event.resultId().getLeastSignificantBits()).array();
        if(body.length<1||body.length>MAX_EVENT_BYTES||!MessageDigest.isEqual(digest(body),hash))throw error(Failure.INVALID_STORAGE);
        boolean commitAttempted=false;
        try {
            check(deadline); Connection opened=factory.open(deadline);
            if(opened==null)throw error(Failure.UNAVAILABLE);
            try(Connection connection=opened) {
                check(deadline);
                if(connection.isClosed()||!connection.getAutoCommit())throw error(Failure.INVALID_STORAGE);
                connection.setReadOnly(false);
                if(connection.isReadOnly())throw error(Failure.INVALID_STORAGE);
                network(connection,deadline); connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
                if(connection.getAutoCommit())throw error(Failure.INVALID_STORAGE);
                try {
                    pinTables(connection,deadline); verifySchema(connection,deadline);
                    if(insert(connection,event,id,hash,body,deadline)) {
                        for(var row:event.rows()) insertMember(connection,id,row,deadline);
                    } else verifyExisting(connection,event,id,hash,body,deadline);
                    check(deadline); network(connection,deadline); commitAttempted=true; connection.commit();
                    // A close/transport failure after commit remains uncertain, never a rollback assertion.
                } catch(Exception failure) {
                    if(!commitAttempted)try {network(connection,deadline);connection.rollback();}catch(Exception ignored){}
                    throw failure;
                }
            }
        } catch(Exception failure) {
            if(commitAttempted)throw error(Failure.COMMIT_UNKNOWN);
            if(failure instanceof SettlementException typed)throw error(typed.failure());
            if(Thread.currentThread().isInterrupted()||deadline-clock.getAsLong()<=0)throw error(Failure.DEADLINE);
            throw error(Failure.UNAVAILABLE);
        } finally {Arrays.fill(body,(byte)0);Arrays.fill(hash,(byte)0);Arrays.fill(id,(byte)0);}
    }
    private boolean insert(Connection c,Event e,byte[] id,byte[] hash,byte[] body,long deadline)throws Exception {
        String sql="INSERT INTO "+table(RESULT)+" (result_id,match_id,generation,terminal_revision,logical_map_id,physical_map_id,recorded_at_millis,participant_count,content_hash,event_body) VALUES (?,?,?,?,?,?,?,?,?,?)";
        boolean duplicate=false;
        try(var s=prepare(c,sql,deadline,1)) {
            s.setBytes(1,id);s.setLong(2,e.matchId());s.setLong(3,e.generation());s.setLong(4,e.terminalRevision());
            s.setInt(5,e.logicalMapId());s.setInt(6,e.manifest().mapId());s.setLong(7,e.recordedAtMillis());
            s.setInt(8,e.rows().size());s.setBytes(9,hash);s.setBytes(10,body);check(deadline);
            try {one(s.executeUpdate());}
            catch(SQLException failure) {
                if(failure.getErrorCode()!=1062||!"23000".equals(failure.getSQLState())||failure.getSuppressed().length!=0||failure.getNextException()!=null)throw failure;
                duplicate=true;
            }
        }
        check(deadline);return !duplicate;
    }
    private void insertMember(Connection c,byte[] id,ModernPetRaceProtocol.ResultRow row,long deadline)throws Exception {
        String sql="INSERT INTO "+table(MEMBER)+" (result_id,entity_id,source_id,object_id,runner_revision,progress_move_sequence,completed_laps,next_checkpoint,placement,finish_elapsed_nanos,dnf_reason,base_graphics_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var s=prepare(c,sql,deadline,1)) {
            s.setBytes(1,id);s.setLong(2,row.runner().entityId());s.setInt(3,row.source().sourceId());s.setLong(4,row.source().objectId());
            long[] tail=tail(row.runner());for(int i=0;i<tail.length;i++)s.setLong(i+5,tail[i]);
            one(s.executeUpdate());check(deadline);
        }
    }
    private void verifyExisting(Connection c,Event e,byte[] id,byte[] hash,byte[] body,long deadline)throws Exception {
        String sql="SELECT match_id,generation,terminal_revision,logical_map_id,physical_map_id,recorded_at_millis,participant_count,content_hash,LEFT(event_body,4097) AS event_body,OCTET_LENGTH(event_body) AS body_length FROM "+table(RESULT)+" WHERE result_id=? FOR UPDATE";
        try(var s=prepare(c,sql,deadline,2)) {
            s.setBytes(1,id);
            try(var rows=s.executeQuery()) {
                if(!rows.next())throw error(Failure.INVALID_STORAGE);
                byte[] stored=rows.getBytes("event_body"),storedHash=rows.getBytes("content_hash");
                try {
                    if(stored==null||stored.length<1||stored.length>MAX_EVENT_BYTES||storedHash==null||storedHash.length!=32
                            ||number(rows,"body_length")!=stored.length||!MessageDigest.isEqual(digest(stored),storedHash))throw error(Failure.INVALID_STORAGE);
                    if(!MessageDigest.isEqual(hash,storedHash)||!Arrays.equals(body,stored))throw error(Failure.CONFLICT);
                    // recordedAt is ingestion metadata: keep the first nonnegative stored value, do not compare/rewrite on replay.
                    if(number(rows,"match_id")!=e.matchId()||number(rows,"generation")!=e.generation()
                            ||number(rows,"terminal_revision")!=e.terminalRevision()||number(rows,"logical_map_id")!=e.logicalMapId()
                            ||number(rows,"physical_map_id")!=e.manifest().mapId()||number(rows,"recorded_at_millis")<0
                            ||number(rows,"participant_count")!=e.rows().size()||rows.next())throw error(Failure.INVALID_STORAGE);
                } finally {if(stored!=null)Arrays.fill(stored,(byte)0);if(storedHash!=null)Arrays.fill(storedHash,(byte)0);}
            }
        }
        String columns="runner_revision,progress_move_sequence,completed_laps,next_checkpoint,placement,finish_elapsed_nanos,dnf_reason,base_graphics_id";
        try(var s=prepare(c,"SELECT entity_id,source_id,object_id,"+columns+" FROM "+table(MEMBER)+" WHERE result_id=? ORDER BY entity_id LIMIT 11",deadline,11)) {
            s.setBytes(1,id);int index=0;
            try(var rows=s.executeQuery()) {
                while(rows.next()) {
                    check(deadline);if(index>=e.rows().size())throw error(Failure.INVALID_STORAGE);
                    var expected=e.rows().get(index++);
                    if(number(rows,"entity_id")!=expected.runner().entityId()||number(rows,"source_id")!=expected.source().sourceId()
                            ||number(rows,"object_id")!=expected.source().objectId())throw error(Failure.INVALID_STORAGE);
                    long[] values=tail(expected.runner());String[] names=columns.split(",");
                    for(int i=0;i<values.length;i++)if(number(rows,names[i])!=values[i])throw error(Failure.INVALID_STORAGE);
                }
            }
            if(index!=e.rows().size())throw error(Failure.INVALID_STORAGE);
        }
    }
    private static long[] tail(ModernPetRace.Runner r) {return new long[]{r.revision(),r.progressMoveSequence(),r.completedLaps(),r.nextCheckpoint(),r.placement(),r.finishElapsedNanos(),r.dnf().ordinal(),r.baseGraphicsId()};}
    private void pinTables(Connection c,long deadline)throws Exception {
        // Hold transaction metadata locks before inspecting engines/columns/keys.
        for(String table:List.of(RESULT,MEMBER))try(var s=prepare(c,"SELECT result_id FROM "+table(table)+" LIMIT 0",deadline,1);var rows=s.executeQuery()) {
            if(rows.next())throw error(Failure.INVALID_STORAGE);check(deadline);
        }
    }
    private void verifySchema(Connection c,long deadline)throws Exception {
        var found=new HashSet<String>();
        try(var s=metadata(c,"SELECT TABLE_NAME,ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE "+FILTER+" ORDER BY TABLE_NAME LIMIT 3",deadline,3);var rows=s.executeQuery()) {
            while(rows.next()) {
                check(deadline);String name=word(rows,"TABLE_NAME",64).toLowerCase(Locale.ROOT);
                if(!SCHEMA.containsKey(name)||!found.add(name)||!"innodb".equalsIgnoreCase(word(rows,"ENGINE",16)))throw error(Failure.INVALID_STORAGE);
            }
        }
        if(!found.equals(SCHEMA.keySet()))throw error(Failure.INVALID_STORAGE);
        var columns=new HashSet<String>();
        try(var s=metadata(c,"SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE "+FILTER+" ORDER BY TABLE_NAME,ORDINAL_POSITION LIMIT 33",deadline,33);var rows=s.executeQuery()) {
            while(rows.next()) {
                check(deadline);String table=word(rows,"TABLE_NAME",64).toLowerCase(Locale.ROOT),name=word(rows,"COLUMN_NAME",64).toLowerCase(Locale.ROOT);
                Column expected=SCHEMA.getOrDefault(table,Map.of()).get(name);
                if(columns.size()>=32||expected==null||!columns.add(table+"."+name)||!"NO".equals(word(rows,"IS_NULLABLE",3)))throw error(Failure.INVALID_STORAGE);
                String type=word(rows,"DATA_TYPE",16).toLowerCase(Locale.ROOT),full=word(rows,"COLUMN_TYPE",32).toLowerCase(Locale.ROOT);
                if(!type.equals(expected.type()))throw error(Failure.INVALID_STORAGE);
                var length=ModernJdbcNumbers.nullable(rows,"CHARACTER_MAXIMUM_LENGTH");
                if(expected.bytes()>0) {
                    if(length.isEmpty()||length.getAsLong()!=expected.bytes()||!full.equals(type+"("+expected.bytes()+")"))throw error(Failure.INVALID_STORAGE);
                } else if(length.isPresent()||!full.matches(type+"(?:\\([0-9]{1,2}\\))?"))throw error(Failure.INVALID_STORAGE);
            }
        }
        if(columns.size()!=SCHEMA.values().stream().mapToInt(Map::size).sum())throw error(Failure.INVALID_STORAGE);
        var keys=new HashMap<String,List<String>>();int count=0;
        try(var s=metadata(c,"SELECT TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM INFORMATION_SCHEMA.STATISTICS WHERE "+FILTER+" AND NON_UNIQUE=0 ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX LIMIT 9",deadline,9);var rows=s.executeQuery()) {
            while(rows.next()) {
                check(deadline);if(++count>8)throw error(Failure.INVALID_STORAGE);
                String key=word(rows,"TABLE_NAME",64).toLowerCase(Locale.ROOT)+"."+word(rows,"INDEX_NAME",64).toLowerCase(Locale.ROOT);
                var expected=KEYS.get(key);var actual=keys.computeIfAbsent(key,ignored->new java.util.ArrayList<>());
                if(expected==null||actual.size()>=expected.size()||number(rows,"SEQ_IN_INDEX")!=actual.size()+1L
                        ||!word(rows,"COLUMN_NAME",64).equalsIgnoreCase(expected.get(actual.size()))
                        ||ModernJdbcNumbers.nullable(rows,"SUB_PART").isPresent())throw error(Failure.INVALID_STORAGE);
                actual.add(expected.get(actual.size()));
            }
        }
        if(!keys.equals(KEYS))throw error(Failure.INVALID_STORAGE);
    }
    private PreparedStatement metadata(Connection c,String sql,long deadline,int maximum)throws Exception {
        var s=prepare(c,sql,deadline,maximum);
        try{s.setString(1,database);return s;}catch(Exception failure){try{s.close();}catch(Exception ignored){}throw failure;}
    }
    private PreparedStatement prepare(Connection c,String sql,long deadline,int maximum)throws Exception {
        network(c,deadline);var s=c.prepareStatement(sql);
        try{s.setMaxRows(maximum);s.setQueryTimeout((int)Math.max(1L,(remainingMillis(deadline)+999L)/1000L));check(deadline);return s;}
        catch(Exception failure){try{s.close();}catch(Exception ignored){}throw failure;}
    }
    private void network(Connection c,long deadline)throws Exception {c.setNetworkTimeout(Runnable::run,remainingMillis(deadline));}
    private int remainingMillis(long deadline)throws SettlementException {check(deadline);return (int)Math.max(1,TimeUnit.NANOSECONDS.toMillis(Math.max(1,deadline-clock.getAsLong())-1)+1);}
    private void check(long deadline)throws SettlementException {if(Thread.currentThread().isInterrupted()||deadline-clock.getAsLong()<=0)throw error(Failure.DEADLINE);}
    private String table(String name){return "`"+database+"`.`"+name+"`";}
    private static long number(ResultSet rows,String name)throws SQLException {return ModernJdbcNumbers.required(rows,name);}
    private static String word(ResultSet rows,String name,int max)throws SQLException,SettlementException {
        String value=rows.getString(name);if(value==null||rows.wasNull()||value.isEmpty()||value.length()>max||value.chars().anyMatch(c->c<32||c>126))throw error(Failure.INVALID_STORAGE);return value;
    }
    private static void one(int changed)throws SettlementException {if(changed!=1)throw error(Failure.INVALID_STORAGE);}
    private static byte[] digest(byte[] bytes)throws Exception {return MessageDigest.getInstance("SHA-256").digest(bytes);}
    private static SettlementException error(Failure failure){return new SettlementException(failure);}
    private static Map<String,Map<String,Column>> schema() {
        var result=new LinkedHashMap<String,Column>();result.put("result_id",new Column("binary",16));
        for(String name:List.of("match_id","generation","terminal_revision","recorded_at_millis"))result.put(name,new Column("bigint",0));
        for(String name:List.of("logical_map_id","physical_map_id","participant_count"))result.put(name,new Column("int",0));
        result.put("content_hash",new Column("binary",32));result.put("event_body",new Column("varbinary",MAX_EVENT_BYTES));
        var member=new LinkedHashMap<String,Column>();member.put("result_id",new Column("binary",16));member.put("source_id",new Column("int",0));
        for(String name:List.of("entity_id","object_id","runner_revision","progress_move_sequence","completed_laps","next_checkpoint","placement","finish_elapsed_nanos","dnf_reason","base_graphics_id"))member.put(name,new Column("bigint",0));
        return Map.of(RESULT,Map.copyOf(result),MEMBER,Map.copyOf(member));
    }
}
