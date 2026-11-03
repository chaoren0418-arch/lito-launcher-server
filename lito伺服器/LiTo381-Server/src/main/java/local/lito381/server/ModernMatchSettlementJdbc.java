package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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

/**
 * Dedicated modern InnoDB ledger. Never writes native/source character tables,
 * never runs DDL, and never joins independent source databases in a transaction.
 * A factory is an explicit trust boundary: it must provide a NEW owned writer
 * connection with bounded connect and physical-socket abort at the supplied
 * absolute deadline (including close/rollback/commit). Ordinary JDBC per-call
 * timeouts alone are not a hard slow-drip deadline. No production factory is
 * silently installed here; the modern source DAO remains read-only.
 */
final class ModernMatchSettlementJdbc implements Store {
    @FunctionalInterface interface ConnectionFactory {
        Connection open(long absoluteDeadlineNanos) throws Exception;
    }
    static final int MAX_EVENT_BYTES = 4096;
    static final long MAX_DEADLINE_NANOS = TimeUnit.SECONDS.toNanos(60);
    static final String MATCH = "lito_modern_match";
    static final String MEMBER = "lito_modern_match_participant";
    static final String TOTAL = "lito_modern_character_record";
    static final String BOT = "lito_modern_match_bot";
    private static final String TABLE_FILTER = "TABLE_SCHEMA=? AND TABLE_NAME IN ('"+MATCH+"','"+MEMBER+"','"+TOTAL+"')";
    private static final String TABLES = "SELECT TABLE_NAME,ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE "+TABLE_FILTER+" ORDER BY TABLE_NAME LIMIT 4";
    private static final String COLUMNS = "SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE "+TABLE_FILTER+" ORDER BY TABLE_NAME,ORDINAL_POSITION LIMIT 65";
    private static final String KEYS = "SELECT TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM INFORMATION_SCHEMA.STATISTICS WHERE "+TABLE_FILTER+" AND NON_UNIQUE=0 ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX LIMIT 17";
    private record Column(String type, int bytes) {}
    private static final Map<String,Map<String,Column>> SCHEMA = schema();
    private static final Map<String,List<String>> UNIQUE_KEYS = Map.of(
            MATCH+".primary",List.of("result_id"),
            MEMBER+".primary",List.of("result_id","source_id","object_id"),
            MEMBER+".uq_modern_match_entity",List.of("result_id","entity_id"),
            TOTAL+".primary",List.of("source_id","object_id"));

    private final ConnectionFactory factory;
    private final String database;
    private final LongSupplier nanoClock;

    ModernMatchSettlementJdbc(ConnectionFactory factory,String settlementDatabase) {
        this(factory,settlementDatabase,System::nanoTime);
    }
    ModernMatchSettlementJdbc(ConnectionFactory factory,String settlementDatabase,LongSupplier nanoClock) {
        this.factory=Objects.requireNonNull(factory);this.nanoClock=Objects.requireNonNull(nanoClock);
        if(settlementDatabase==null||!settlementDatabase.matches("[A-Za-z0-9_]{1,64}"))throw new IllegalArgumentException("invalid settlement database");
        database=settlementDatabase;
    }

    @Override public void persist(Event event,long deadline) throws Exception {
        Objects.requireNonNull(event);
        long remaining=deadline-nanoClock.getAsLong();
        if(remaining<=0||remaining>MAX_DEADLINE_NANOS||Thread.currentThread().isInterrupted())throw error(Failure.DEADLINE);
        byte[] body=canonical(event),hash=digest(body),id=uuid(event);
        boolean commitAttempted=false;
        try {
            check(deadline);
            Connection opened=factory.open(deadline);
            if(opened==null)throw error(Failure.UNAVAILABLE);
            try(Connection connection=opened) {
                check(deadline);
                if(connection.isClosed()||!connection.getAutoCommit())throw error(Failure.INVALID_STORAGE);
                connection.setReadOnly(false);
                if(connection.isReadOnly())throw error(Failure.INVALID_STORAGE);
                network(connection,deadline);
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
                if(connection.getAutoCommit())throw error(Failure.INVALID_STORAGE);
                try {
                    pinTables(connection,deadline,event.hasBots());
                    verifySchema(connection,deadline,event.hasBots());
                    boolean inserted=insertLedger(connection,event,id,hash,body,deadline);
                    if(inserted) {
                        for(Participant participant:event.participants())applyParticipant(connection,participant,id,deadline);
                    } else {
                        verifyExisting(connection,event,id,hash,body,deadline);
                    }
                    check(deadline);network(connection,deadline);
                    commitAttempted=true;
                    connection.commit();
                    // A returned commit is durable even if cancellation arrives now.
                    // Cleanup failure is still reported conservatively as uncertain;
                    // the only safe retry is this same immutable event/resultId.
                } catch(Exception failure) {
                    if(!commitAttempted) {
                        try { network(connection,deadline);connection.rollback(); }
                        catch(Exception cleanup) { /* ConnectionFactory still owns physical retirement. */ }
                    }
                    throw failure;
                }
            }
        } catch(Exception failure) {
            if(commitAttempted)throw error(Failure.COMMIT_UNKNOWN);
            if(failure instanceof SettlementException settlement)throw error(settlement.failure());
            if(failure instanceof ArithmeticException)throw error(Failure.OVERFLOW);
            if(Thread.currentThread().isInterrupted()||deadline-nanoClock.getAsLong()<=0)throw error(Failure.DEADLINE);
            throw error(Failure.UNAVAILABLE);
        } finally {Arrays.fill(body,(byte)0);Arrays.fill(hash,(byte)0);Arrays.fill(id,(byte)0);}
    }

    private boolean insertLedger(Connection connection,Event event,byte[] id,byte[] hash,byte[] body,long deadline) throws Exception {
        String sql="INSERT INTO "+table(MATCH)+" (result_id,match_id,generation,engine_revision,mode,map_id,finished_at_millis,participant_count,event_hash,event_body) VALUES (?,?,?,?,?,?,?,?,?,?)";
        boolean duplicate=false;
        try(PreparedStatement statement=prepare(connection,sql,deadline,1)) {
            statement.setBytes(1,id);statement.setLong(2,event.matchId());statement.setLong(3,event.generation());
            statement.setLong(4,event.engineRevision());statement.setInt(5,event.mode());statement.setInt(6,event.mapId());
            statement.setLong(7,event.finishedAtMillis());statement.setInt(8,event.participants().size());
            statement.setBytes(9,hash);statement.setBytes(10,body);check(deadline);
            try {requireOne(statement.executeUpdate());}
            catch(SQLException failure) {
                if(failure.getErrorCode()!=1062||!"23000".equals(failure.getSQLState())||failure.getSuppressed().length!=0||failure.getNextException()!=null)throw failure;
                duplicate=true;
            }
        }
        // prepare/setup/close failures never masquerade as a duplicate. The
        // unique InnoDB insert waits for an earlier same-key transaction; an
        // empty, racy lookup alone is never treated as proof of rollback.
        check(deadline);return !duplicate;
    }

    private void applyParticipant(Connection connection,Participant participant,byte[] id,long deadline) throws Exception {
        if(!participant.identity().isHuman()) {
            try(var statement=prepare(connection,"INSERT INTO "+table(BOT)+" (result_id,entity_id,source_id,bot_id,team,verdict) VALUES (?,?,?,?,?,?)",deadline,1)) {
                statement.setBytes(1,id);statement.setLong(2,participant.entityId());statement.setInt(3,participant.identity().sourceId());
                statement.setLong(4,participant.identity().id());statement.setInt(5,participant.team());
                statement.setInt(6,participant.verdict().ordinal());requireOne(statement.executeUpdate());
            }
            check(deadline);return;
        }
        var key=participant.key();
        try(var statement=prepare(connection,"INSERT INTO "+table(TOTAL)+" (source_id,object_id,wins,losses,revision) VALUES (?,?,0,0,0) ON DUPLICATE KEY UPDATE source_id=source_id",deadline,1)) {
            statement.setInt(1,key.sourceId());statement.setLong(2,key.objectId());
            int changed=statement.executeUpdate();if(changed<0||changed>2)throw error(Failure.INVALID_STORAGE);
        }
        long[] before=totals(connection,key,deadline);
        long wins=before[0],losses=before[1],revision=Math.incrementExact(before[2]);
        if(participant.verdict()==Verdict.WIN)wins=Math.incrementExact(wins);else losses=Math.incrementExact(losses);
        if(Math.addExact(wins,losses)!=revision)throw error(Failure.INVALID_STORAGE);
        try(var statement=prepare(connection,"UPDATE "+table(TOTAL)+" SET wins=?,losses=?,revision=? WHERE source_id=? AND object_id=? AND revision=?",deadline,1)) {
            statement.setLong(1,wins);statement.setLong(2,losses);statement.setLong(3,revision);
            statement.setInt(4,key.sourceId());statement.setLong(5,key.objectId());statement.setLong(6,before[2]);requireOne(statement.executeUpdate());
        }
        try(var statement=prepare(connection,"INSERT INTO "+table(MEMBER)+" (result_id,entity_id,source_id,object_id,team,verdict) VALUES (?,?,?,?,?,?)",deadline,1)) {
            statement.setBytes(1,id);statement.setLong(2,participant.entityId());statement.setInt(3,key.sourceId());
            statement.setLong(4,key.objectId());statement.setInt(5,participant.team());statement.setInt(6,participant.verdict().ordinal());requireOne(statement.executeUpdate());
        }
        check(deadline);
    }

    private long[] totals(Connection connection,TournamentPresence.CharacterKey key,long deadline) throws Exception {
        try(var statement=prepare(connection,"SELECT wins,losses,revision FROM "+table(TOTAL)+" WHERE source_id=? AND object_id=? FOR UPDATE",deadline,2)) {
            statement.setInt(1,key.sourceId());statement.setLong(2,key.objectId());
            try(var rows=statement.executeQuery()) {
                if(!rows.next())throw error(Failure.INVALID_STORAGE);
                long wins=ModernJdbcNumbers.required(rows,"wins"),losses=ModernJdbcNumbers.required(rows,"losses"),revision=ModernJdbcNumbers.required(rows,"revision");
                if(wins<0||losses<0||revision<0||Math.addExact(wins,losses)!=revision||rows.next())throw error(Failure.INVALID_STORAGE);
                check(deadline);return new long[]{wins,losses,revision};
            }
        }
    }

    private void verifyExisting(Connection connection,Event expected,byte[] id,byte[] hash,byte[] body,long deadline) throws Exception {
        String sql="SELECT match_id,generation,engine_revision,mode,map_id,finished_at_millis,participant_count,event_hash,LEFT(event_body,4097) AS event_body,OCTET_LENGTH(event_body) AS body_length FROM "+table(MATCH)+" WHERE result_id=? FOR UPDATE";
        try(var statement=prepare(connection,sql,deadline,2)) {
            statement.setBytes(1,id);
            try(var rows=statement.executeQuery()) {
                if(!rows.next())throw error(Failure.INVALID_STORAGE);
                byte[] stored=rows.getBytes("event_body"),storedHash=rows.getBytes("event_hash");
                try {
                    if(stored==null||stored.length<1||stored.length>MAX_EVENT_BYTES||storedHash==null||storedHash.length!=32
                            ||ModernJdbcNumbers.required(rows,"body_length")!=stored.length||!MessageDigest.isEqual(digest(stored),storedHash))throw error(Failure.INVALID_STORAGE);
                    if(!MessageDigest.isEqual(hash,storedHash)||!Arrays.equals(body,stored))throw error(Failure.CONFLICT);
                    if(ModernJdbcNumbers.required(rows,"match_id")!=expected.matchId()||ModernJdbcNumbers.required(rows,"generation")!=expected.generation()
                            ||ModernJdbcNumbers.required(rows,"engine_revision")!=expected.engineRevision()||ModernJdbcNumbers.required(rows,"mode")!=expected.mode()
                            ||ModernJdbcNumbers.required(rows,"map_id")!=expected.mapId()||ModernJdbcNumbers.required(rows,"finished_at_millis")!=expected.finishedAtMillis()
                            ||ModernJdbcNumbers.required(rows,"participant_count")!=expected.participants().size()||rows.next())throw error(Failure.INVALID_STORAGE);
                } finally {if(stored!=null)Arrays.fill(stored,(byte)0);if(storedHash!=null)Arrays.fill(storedHash,(byte)0);}
            }
        }
        verifyParticipants(connection,expected.participants().stream().filter(p->p.identity().isHuman()).toList(),id,deadline,false);
        if(expected.hasBots())verifyParticipants(connection,expected.participants().stream().filter(p->!p.identity().isHuman()).toList(),id,deadline,true);
        for(var participant:expected.participants()) {
            if(!participant.identity().isHuman())continue;
            long[] current=totals(connection,participant.key(),deadline);
            if(current[participant.verdict()==Verdict.WIN?0:1]<1)throw error(Failure.INVALID_STORAGE);
        }
    }

    private void verifyParticipants(Connection connection,List<Participant> expected,byte[] id,long deadline,boolean bots) throws Exception {
        String idColumn=bots?"bot_id":"object_id";
        try(var statement=prepare(connection,"SELECT entity_id,source_id,"+idColumn+",team,verdict FROM "+table(bots?BOT:MEMBER)+" WHERE result_id=? ORDER BY entity_id LIMIT 101",deadline,101)) {
            statement.setBytes(1,id);int index=0;
            try(var rows=statement.executeQuery()) {
                while(rows.next()) {
                    check(deadline);if(index>=expected.size())throw error(Failure.INVALID_STORAGE);
                    var p=expected.get(index++);
                    if(ModernJdbcNumbers.required(rows,"entity_id")!=p.entityId()||ModernJdbcNumbers.required(rows,"source_id")!=p.identity().sourceId()
                            ||ModernJdbcNumbers.required(rows,idColumn)!=p.identity().id()||ModernJdbcNumbers.required(rows,"team")!=p.team()
                            ||ModernJdbcNumbers.required(rows,"verdict")!=p.verdict().ordinal())throw error(Failure.INVALID_STORAGE);
                }
            }
            if(index!=expected.size())throw error(Failure.INVALID_STORAGE);
        }
    }

    private void pinTables(Connection connection,long deadline,boolean bots) throws Exception {
        // MySQL retains these transaction metadata locks through commit/rollback,
        // so concurrent ALTER cannot change an inspected engine before the writes.
        // No row lock or source-table query is needed for this fence.
        for(String name:bots?List.of(MATCH,MEMBER,TOTAL,BOT):List.of(MATCH,MEMBER,TOTAL)) {
            String projection=name.equals(TOTAL)?"source_id,object_id,wins,losses,revision":"result_id";
            try(var statement=prepare(connection,"SELECT "+projection+" FROM "+table(name)+" LIMIT 0",deadline,1);
                var rows=statement.executeQuery()) {
                if(rows.next())throw error(Failure.INVALID_STORAGE);
                check(deadline);
            }
        }
    }

    private void verifySchema(Connection connection,long deadline,boolean bots) throws Exception {
        var schema=new HashMap<>(SCHEMA);var uniqueKeys=new HashMap<>(UNIQUE_KEYS);
        if(bots) {
            var columns=new LinkedHashMap<>(SCHEMA.get(MEMBER));columns.remove("object_id");columns.put("bot_id",new Column("bigint",0));
            schema.put(BOT,Map.copyOf(columns));
            uniqueKeys.put(BOT+".primary",List.of("result_id","source_id","bot_id"));
            uniqueKeys.put(BOT+".uq_modern_match_bot_entity",List.of("result_id","entity_id"));
        }
        String filter=bots?TABLE_FILTER.substring(0,TABLE_FILTER.length()-1)+",'"+BOT+"')":TABLE_FILTER;
        String tablesSql=bots?TABLES.replace(TABLE_FILTER,filter).replace("LIMIT 4","LIMIT 5"):TABLES;
        String columnsSql=bots?COLUMNS.replace(TABLE_FILTER,filter):COLUMNS;
        String keysSql=bots?KEYS.replace(TABLE_FILTER,filter):KEYS;
        var tables=new HashSet<String>();
        try(var statement=metadata(connection,tablesSql,deadline,bots?5:4);var rows=statement.executeQuery()) {
            while(rows.next()) {
                check(deadline);String table=word(rows,"TABLE_NAME",64).toLowerCase(Locale.ROOT);
                if(tables.size()>=schema.size()||!schema.containsKey(table)||!tables.add(table)||!"innodb".equalsIgnoreCase(word(rows,"ENGINE",16)))throw error(Failure.INVALID_STORAGE);
            }
        }
        if(!tables.equals(schema.keySet()))throw error(Failure.INVALID_STORAGE);
        var columns=new HashSet<String>();
        try(var statement=metadata(connection,columnsSql,deadline,65);var rows=statement.executeQuery()) {
            while(rows.next()) {
                check(deadline);String table=word(rows,"TABLE_NAME",64).toLowerCase(Locale.ROOT),name=word(rows,"COLUMN_NAME",64).toLowerCase(Locale.ROOT);
                Column column=schema.getOrDefault(table,Map.of()).get(name);
                if(columns.size()>=64||column==null||!columns.add(table+"."+name)||!"NO".equals(word(rows,"IS_NULLABLE",3)))throw error(Failure.INVALID_STORAGE);
                String type=word(rows,"DATA_TYPE",16).toLowerCase(Locale.ROOT),full=word(rows,"COLUMN_TYPE",32).toLowerCase(Locale.ROOT);
                if(!column.type().equals(type))throw error(Failure.INVALID_STORAGE);
                var length=ModernJdbcNumbers.nullable(rows,"CHARACTER_MAXIMUM_LENGTH");
                if(column.bytes()>0) {
                    if(length.isEmpty()||length.getAsLong()!=column.bytes()||!full.equals(type+"("+column.bytes()+")"))throw error(Failure.INVALID_STORAGE);
                } else if(length.isPresent()||!full.matches(type+"(?:\\([0-9]{1,2}\\))?"))throw error(Failure.INVALID_STORAGE);
            }
        }
        if(columns.size()!=schema.values().stream().mapToInt(Map::size).sum())throw error(Failure.INVALID_STORAGE);
        var keys=new HashMap<String,List<String>>();int count=0;
        try(var statement=metadata(connection,keysSql,deadline,17);var rows=statement.executeQuery()) {
            while(rows.next()) {
                check(deadline);if(++count>16)throw error(Failure.INVALID_STORAGE);
                String key=word(rows,"TABLE_NAME",64).toLowerCase(Locale.ROOT)+"."+word(rows,"INDEX_NAME",64).toLowerCase(Locale.ROOT);
                var expected=uniqueKeys.get(key);var found=keys.computeIfAbsent(key,ignored->new java.util.ArrayList<>());
                if(expected==null||found.size()>=expected.size()||ModernJdbcNumbers.required(rows,"SEQ_IN_INDEX")!=found.size()+1L
                        ||!word(rows,"COLUMN_NAME",64).equalsIgnoreCase(expected.get(found.size()))
                        ||ModernJdbcNumbers.nullable(rows,"SUB_PART").isPresent())throw error(Failure.INVALID_STORAGE);
                // A BINARY result UUID must participate in full, not merely a
                // prefix that makes unrelated UUIDs falsely collide. SQL NULL
                // is the metadata proof of an unprefixed key; missing, numeric
                // zero, fractional, malformed or approximate values are not.
                found.add(expected.get(found.size()));
            }
        }
        if(!keys.equals(uniqueKeys))throw error(Failure.INVALID_STORAGE);
    }

    private PreparedStatement metadata(Connection connection,String sql,long deadline,int maximum) throws Exception {
        PreparedStatement statement=prepare(connection,sql,deadline,maximum);
        try {statement.setString(1,database);return statement;}
        catch(Exception failure){try{statement.close();}catch(Exception cleanup){}throw failure;}
    }
    private PreparedStatement prepare(Connection connection,String sql,long deadline,int maximum) throws Exception {
        network(connection,deadline);PreparedStatement statement=connection.prepareStatement(sql);
        try {
            statement.setMaxRows(maximum);statement.setQueryTimeout((int)Math.max(1L,((long)remainingMillis(deadline)+999L)/1000L));
            check(deadline);return statement;
        } catch(Exception failure){try{statement.close();}catch(Exception cleanup){}throw failure;}
    }
    private void network(Connection connection,long deadline) throws Exception {connection.setNetworkTimeout(Runnable::run,remainingMillis(deadline));}
    private int remainingMillis(long deadline) throws SettlementException {
        check(deadline);long remaining=deadline-nanoClock.getAsLong();
        if(remaining<=0)throw error(Failure.DEADLINE);
        return (int)Math.min(Integer.MAX_VALUE,Math.max(1,TimeUnit.NANOSECONDS.toMillis(remaining-1)+1));
    }
    private void check(long deadline) throws SettlementException {if(Thread.currentThread().isInterrupted()||deadline-nanoClock.getAsLong()<=0)throw error(Failure.DEADLINE);}
    private String table(String name){return "`"+database+"`.`"+name+"`";}
    private static String word(ResultSet rows,String column,int max) throws SQLException,SettlementException {
        String value=rows.getString(column);
        if(value==null||rows.wasNull()||value.isEmpty()||value.length()>max||value.chars().anyMatch(c->c<0x20||c>0x7e))throw error(Failure.INVALID_STORAGE);
        return value;
    }
    private static void requireOne(int count) throws SettlementException {if(count!=1)throw error(Failure.INVALID_STORAGE);}
    static byte[] canonical(Event event) throws Exception {
        var buffer=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(buffer)) {
            out.writeInt(event.siege()!=null?4:event.timeAttack()!=null?3:event.hasBots()?2:1);out.write(uuid(event));out.writeLong(event.matchId());out.writeLong(event.generation());
            out.writeLong(event.engineRevision());out.writeInt(event.mode());out.writeInt(event.mapId());out.writeLong(event.finishedAtMillis());
            out.writeInt(event.participants().size());
            for(var p:event.participants()) {
                if(event.hasBots()||event.siege()!=null)out.writeByte(p.identity().kind().ordinal());
                out.writeLong(p.entityId());out.writeInt(p.identity().sourceId());out.writeLong(p.identity().id());out.writeByte(p.team());out.writeByte(p.verdict().ordinal());
            }
            if(event.timeAttack()!=null)ModernTimeAttackSettlement.write(out,event.timeAttack());
            if(event.siege()!=null)ModernSiegeSettlement.write(out,event.siege());
        }
        byte[] result=buffer.toByteArray();if(result.length>MAX_EVENT_BYTES)throw error(Failure.INVALID_STORAGE);return result;
    }
    static byte[] uuid(Event event) {return ByteBuffer.allocate(16).putLong(event.resultId().getMostSignificantBits()).putLong(event.resultId().getLeastSignificantBits()).array();}
    private static byte[] digest(byte[] bytes) throws Exception {return MessageDigest.getInstance("SHA-256").digest(bytes);}
    private static SettlementException error(Failure failure){return new SettlementException(failure);}
    private static Map<String,Map<String,Column>> schema() {
        var match=new LinkedHashMap<String,Column>();match.put("result_id",new Column("binary",16));
        for(String name:List.of("match_id","generation","engine_revision","finished_at_millis"))match.put(name,new Column("bigint",0));
        for(String name:List.of("mode","map_id","participant_count"))match.put(name,new Column("int",0));
        match.put("event_hash",new Column("binary",32));match.put("event_body",new Column("varbinary",MAX_EVENT_BYTES));
        var member=new LinkedHashMap<String,Column>();member.put("result_id",new Column("binary",16));
        member.put("entity_id",new Column("bigint",0));member.put("source_id",new Column("int",0));member.put("object_id",new Column("bigint",0));
        member.put("team",new Column("tinyint",0));member.put("verdict",new Column("tinyint",0));
        var total=new LinkedHashMap<String,Column>();total.put("source_id",new Column("int",0));
        for(String name:List.of("object_id","wins","losses","revision"))total.put(name,new Column("bigint",0));
        return Map.of(MATCH,Map.copyOf(match),MEMBER,Map.copyOf(member),TOTAL,Map.copyOf(total));
    }
}
