package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** No real SQL in these tests. Any mutation/commit or unbounded JDBC operation fails the fixture. */
class ModernWorldPolicyJdbcTest {
    static ModernWorldPolicy.Row row(){return new ModernWorldPolicy.Row(ModernWorldPolicyTest.KEY,514,"2".repeat(64),new ModernWorldMap.Cell(32790,32790),List.of(),null);}
    static ModernWorldPolicy.Snapshot snapshot(){return ModernWorldPolicyTest.snapshot(7,List.of(row()));}
    static ModernWorldPolicy.Snapshot read(Jdbc jdbc)throws Exception{return new ModernWorldPolicyJdbc(jdbc,"policy",jdbc.clock::get).read(snapshot().pin(),10_000_000_000L);}
    static void rejected(Jdbc jdbc){var error=assertThrows(SQLException.class,()->read(jdbc));assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);assertEquals("dedicated world policy unavailable",error.getMessage());assertEquals(1,jdbc.closes);}
    @Test void sevenSelectsPinBothTablesAndVerifyRepeatableReadBeforePublishingAfterPhysicalClose()throws Exception{
        var jdbc=new Jdbc();assertEquals(snapshot(),read(jdbc));assertEquals(7,jdbc.sql.size());
        assertTrue(jdbc.sql.stream().allMatch(s->s.startsWith("SELECT ")));assertTrue(jdbc.sql.subList(0,2).stream().allMatch(s->s.endsWith(" LIMIT 0")));
        assertEquals(List.of(1,1,3,22,7,2,41),jdbc.maximums);assertEquals(1,jdbc.closes);assertEquals(7,jdbc.statementCloses);assertEquals(7,jdbc.resultCloses);
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ,jdbc.isolation);assertTrue(jdbc.readOnly);assertFalse(jdbc.autoCommit);
        assertEquals(List.of("readonly","isolation","transaction"),jdbc.setup);assertEquals(10_000_000_000L,jdbc.deadline);
        assertArrayEquals(HexFormat.of().parseHex(ModernWorldPolicyTest.NAMESPACE),(byte[])jdbc.bindings.get(5).get(1));assertEquals(7L,jdbc.bindings.get(5).get(2));
    }
    @Test void absentDuplicateUnpublishedWrongNamespaceRevisionAndHashNeverBecomeAnEmptyPolicy(){
        var absent=new Jdbc();absent.header.clear();rejected(absent);
        var duplicate=new Jdbc();duplicate.header.add(new HashMap<>(duplicate.header.get(0)));rejected(duplicate);
        for(String field:List.of("schema_version","revision","binding_count","published")){var j=new Jdbc();j.header.get(0).put(field,0L);rejected(j);}
        for(String field:List.of("namespace_sha256","content_sha256")){var j=new Jdbc();j.header.get(0).put(field,new byte[32]);rejected(j);}
        var changed=new Jdbc();changed.data.get(0).put("anchor_x",32791L);rejected(changed);
        var wrongOwner=new Jdbc();wrongOwner.data.get(0).put("revision",8L);rejected(wrongOwner);
        var extra=new Jdbc();extra.data.add(new HashMap<>(extra.data.get(0)));rejected(extra);
        var missing=new Jdbc();missing.data.clear();rejected(missing);
    }
    @Test void exactMetadataRequiresInnoDbBaseTablesSignedColumnsNullableShapeAndFullUnprefixedPrimaryKeys(){
        for(String field:List.of("ENGINE","TABLE_TYPE")){var j=new Jdbc();j.tables.get(0).put(field,"invalid");rejected(j);}
        var missing=new Jdbc();missing.columns.remove(0);rejected(missing);
        var added=new Jdbc();added.columns.add(new HashMap<>(added.columns.get(0)));rejected(added);
        for(String field:List.of("DATA_TYPE","COLUMN_TYPE","IS_NULLABLE","EXTRA")){var j=new Jdbc();j.columns.get(0).put(field,"invalid");rejected(j);}
        var unsigned=new Jdbc();unsigned.columns.get(1).put("COLUMN_TYPE","bigint unsigned");rejected(unsigned);
        for(Object value:List.of(1L,0L,new BigDecimal("1.5"),1d,"1")){var j=new Jdbc();j.indexes.get(0).put("SUB_PART",value);rejected(j);}
        var badKey=new Jdbc();badKey.indexes.get(0).put("INDEX_NAME","unexpected_unique");rejected(badKey);
        var shortHash=new Jdbc();shortHash.columns.get(0).put("CHARACTER_MAXIMUM_LENGTH",16L);rejected(shortHash);
    }
    @Test void exactNumericDomainsAndPartialCampsFailRatherThanCoerceOrManufactureDefaults(){
        for(String field:List.of("mode","logical_map_id","physical_map_id","anchor_x","revision","countdown_nanos","active_timeout_millis")){
            for(Object value:List.of(1d,"1",new BigDecimal("1.1"),new BigDecimal("9223372036854775808"))){var j=new Jdbc();j.data.get(0).put(field,value);rejected(j);}
        }
        for(String field:List.of("camp0_x","camp0_y","camp1_x","camp1_y")){var j=new Jdbc();j.data.get(0).put(field,32790L);rejected(j);}
        var nullAnchor=new Jdbc();nullAnchor.data.get(0).put("anchor_x",null);rejected(nullAnchor);
        var badClock=new Jdbc();badClock.data.get(0).put("countdown_nanos",30_000_000_001L);rejected(badClock);
    }
    @Test void draftCandidateCalculatesHashWithoutTrustingPlaceholderAndCannotEnterStrictOrPublishedInspection()throws Exception{
        var draft=new Jdbc();draft.header.get(0).put("published",0L);draft.header.get(0).put("content_sha256",new byte[32]);
        var candidate=new ModernWorldPolicyJdbc(draft,"policy",draft.clock::get).readCandidate(snapshot().pin(),10_000_000_000L);
        assertEquals(snapshot(),candidate);var strict=new Jdbc();strict.header.get(0).put("published",0L);rejected(strict);
        var published=new Jdbc();assertThrows(SQLException.class,()->new ModernWorldPolicyJdbc(published,"policy",published.clock::get).readCandidate(snapshot().pin(),10_000_000_000L));
        assertEquals(1,published.closes);
    }
    @Test void refusedTransactionDeadlineAndAnyCleanupFailureCannotPublish(){
        var refused=new Jdbc();refused.refuseIsolation=true;rejected(refused);assertTrue(refused.sql.isEmpty());
        for(String failure:List.of("query","result","statement","connection")){var j=new Jdbc();j.failure=failure;rejected(j);}
        var late=new Jdbc();late.expireOnLayout=true;rejected(late);
        var unopened=new Jdbc();unopened.clock.set(10_000_000_000L);assertThrows(SQLException.class,()->read(unopened));assertEquals(0,unopened.opens);
        var interrupted=new Jdbc();Thread.currentThread().interrupt();try{assertThrows(SQLException.class,()->read(interrupted));assertEquals(0,interrupted.opens);}finally{Thread.interrupted();}
    }

    static final class Jdbc implements ModernMatchSettlementJdbc.ConnectionFactory {
        final AtomicLong clock=new AtomicLong();final List<String> sql=new ArrayList<>(),setup=new ArrayList<>();final List<Integer> maximums=new ArrayList<>();
        final List<Map<Integer,Object>> bindings=new ArrayList<>();
        final List<Map<String,Object>> tables=new ArrayList<>(),columns=new ArrayList<>(),indexes=new ArrayList<>(),header=new ArrayList<>(),data=new ArrayList<>();
        boolean autoCommit=true,readOnly,refuseIsolation,expireOnLayout;int isolation,opens,closes,statementCloses,resultCloses;long deadline;String failure="";
        Jdbc(){
            for(String table:List.of(ModernWorldPolicyJdbc.HEADER,ModernWorldPolicyJdbc.LAYOUT))tables.add(new HashMap<>(Map.of("TABLE_NAME",table,"ENGINE","InnoDB","TABLE_TYPE","BASE TABLE")));
            column(ModernWorldPolicyJdbc.HEADER,"namespace_sha256","binary",32,false);column(ModernWorldPolicyJdbc.HEADER,"revision","bigint",0,false);
            column(ModernWorldPolicyJdbc.HEADER,"schema_version","int",0,false);column(ModernWorldPolicyJdbc.HEADER,"binding_count","int",0,false);
            column(ModernWorldPolicyJdbc.HEADER,"content_sha256","binary",32,false);column(ModernWorldPolicyJdbc.HEADER,"published","tinyint",0,false);
            column(ModernWorldPolicyJdbc.LAYOUT,"namespace_sha256","binary",32,false);column(ModernWorldPolicyJdbc.LAYOUT,"revision","bigint",0,false);
            for(String name:List.of("mode","logical_map_id","physical_map_id"))column(ModernWorldPolicyJdbc.LAYOUT,name,"int",0,false);
            column(ModernWorldPolicyJdbc.LAYOUT,"manifest_sha256","binary",32,false);
            for(String name:List.of("anchor_x","anchor_y"))column(ModernWorldPolicyJdbc.LAYOUT,name,"int",0,false);
            for(String name:List.of("camp0_x","camp0_y","camp1_x","camp1_y","load_timeout_millis","active_timeout_millis"))column(ModernWorldPolicyJdbc.LAYOUT,name,"int",0,true);
            column(ModernWorldPolicyJdbc.LAYOUT,"countdown_nanos","bigint",0,true);
            for(String table:List.of(ModernWorldPolicyJdbc.HEADER,ModernWorldPolicyJdbc.LAYOUT)){
                var keys=table.equals(ModernWorldPolicyJdbc.HEADER)?List.of("namespace_sha256","revision"):List.of("namespace_sha256","revision","mode","logical_map_id");
                for(int i=0;i<keys.size();i++){var m=new HashMap<String,Object>();m.put("TABLE_NAME",table);m.put("INDEX_NAME","PRIMARY");m.put("SEQ_IN_INDEX",(long)i+1);m.put("COLUMN_NAME",keys.get(i));m.put("SUB_PART",null);indexes.add(m);}
            }
            var h=new HashMap<String,Object>();h.put("namespace_sha256",HexFormat.of().parseHex(ModernWorldPolicyTest.NAMESPACE));h.put("revision",7L);
            h.put("schema_version",1L);h.put("binding_count",1L);h.put("content_sha256",HexFormat.of().parseHex(snapshot().pin().contentSha256()));h.put("published",1L);header.add(h);
            var d=new HashMap<String,Object>();d.put("namespace_sha256",h.get("namespace_sha256"));d.put("revision",7L);d.put("mode",1L);d.put("logical_map_id",514L);
            d.put("physical_map_id",514L);d.put("manifest_sha256",HexFormat.of().parseHex(row().manifestSha256()));d.put("anchor_x",32790L);d.put("anchor_y",32790L);
            for(String name:List.of("camp0_x","camp0_y","camp1_x","camp1_y","load_timeout_millis","countdown_nanos","active_timeout_millis"))d.put(name,null);data.add(d);
        }
        void column(String table,String name,String type,int length,boolean nullable){
            var m=new HashMap<String,Object>();m.put("TABLE_NAME",table);m.put("COLUMN_NAME",name);m.put("DATA_TYPE",type);m.put("COLUMN_TYPE",length==0?type:type+"("+length+")");
            m.put("IS_NULLABLE",nullable?"YES":"NO");m.put("CHARACTER_MAXIMUM_LENGTH",length==0?null:(long)length);m.put("EXTRA","");columns.add(m);
        }
        public Connection open(long until){opens++;deadline=until;return proxy(Connection.class,(name,args)->switch(name){
            case "isClosed"->false;case "getAutoCommit"->autoCommit;case "isReadOnly"->readOnly;case "getTransactionIsolation"->isolation;
            case "setReadOnly"->{readOnly=(boolean)args[0];setup.add("readonly");yield null;}
            case "setTransactionIsolation"->{if(!refuseIsolation)isolation=(int)args[0];setup.add("isolation");yield null;}
            case "setAutoCommit"->{autoCommit=(boolean)args[0];setup.add("transaction");yield null;}
            case "setNetworkTimeout"->{assertTrue((int)args[1]>0);yield null;}
            case "prepareStatement"->statement((String)args[0]);case "close"->{closes++;fail("connection");yield null;}
            default->throw new AssertionError("unexpected policy connection operation "+name);
        });}
        PreparedStatement statement(String query){
            assertTrue(query.startsWith("SELECT "));sql.add(query);var bound=new HashMap<Integer,Object>();bindings.add(bound);
            return proxy(PreparedStatement.class,(name,args)->switch(name){
                case "setString","setBytes","setLong"->{bound.put((int)args[0],args[1]);yield null;}
                case "setMaxRows"->{maximums.add((int)args[0]);yield null;}case "setQueryTimeout"->{assertTrue((int)args[0]>0);yield null;}
                case "executeQuery"->{fail("query");var selected=query.endsWith("LIMIT 0")?List.<Map<String,Object>>of():query.equals(ModernWorldPolicyJdbc.TABLES)?tables:
                        query.equals(ModernWorldPolicyJdbc.COLUMNS)?columns:query.equals(ModernWorldPolicyJdbc.INDEXES)?indexes:query.contains("`"+ModernWorldPolicyJdbc.HEADER+"`")?header:data;
                    if(selected==data&&expireOnLayout)clock.set(deadline);yield result(selected);}
                case "close"->{statementCloses++;fail("statement");yield null;}
                default->throw new AssertionError("unexpected policy statement operation "+name);
            });
        }
        ResultSet result(List<Map<String,Object>> rows){
            int[] position={-1};boolean[] wasNull={false};var names=new ArrayList<String>();if(!rows.isEmpty())names.addAll(rows.get(0).keySet());
            return proxy(ResultSet.class,(name,args)->switch(name){
                case "next"->++position[0]<rows.size();case "wasNull"->wasNull[0];case "findColumn"->{int index=names.indexOf((String)args[0]);if(index<0)throw new SQLException();yield index+1;}
                case "getMetaData"->proxy(ResultSetMetaData.class,(method,input)->{if(!method.equals("getColumnType"))throw new AssertionError(method);
                    Object value=rows.get(position[0]).get(names.get((int)input[0]-1));return value instanceof Double?Types.DOUBLE:value instanceof String?Types.VARCHAR:Types.BIGINT;});
                case "getBigDecimal"->{Object value=rows.get(position[0]).get(names.get((int)args[0]-1));wasNull[0]=value==null;
                    yield value==null?null:value instanceof BigDecimal decimal?decimal:new BigDecimal(value.toString());}
                case "getString","getBytes"->{Object value=rows.get(position[0]).get((String)args[0]);wasNull[0]=value==null;yield value;}
                case "close"->{resultCloses++;fail("result");yield null;}
                default->throw new AssertionError("unexpected policy result operation "+name);
            });
        }
        void fail(String stage)throws SQLException{if(failure.equals(stage))throw new SQLException("private SQL detail");}
    }
    @FunctionalInterface interface Invocation{Object call(String name,Object[] args)throws Throwable;}
    @SuppressWarnings("unchecked")static <T>T proxy(Class<T> type,Invocation call){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{
        if(method.getName().equals("toString"))return "policy-fixture";return call.call(method.getName(),args==null?new Object[0]:args);
    });}
}
