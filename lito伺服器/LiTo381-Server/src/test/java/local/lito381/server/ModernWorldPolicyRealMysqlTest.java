package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Actual fresh owned MySQL + SELECT-only runtime principal + production startup/CLI. No installed database is touched. */
class ModernWorldPolicyRealMysqlTest {
    static final String DATABASE="world_policy_fixture", USER="wp_reader";
    @TempDir Path temporary;
    @Test @Timeout(value=180,unit=TimeUnit.SECONDS)
    void explicitDraftHashPublicationRepeatableReadAndSuccessiveImmutableStartsUseRealDedicatedSql()throws Exception{
        String mysql=System.getenv(ModernSettlementMysqlFixture.ENV),shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&!mysql.isBlank()&&shared!=null&&!shared.isBlank(),"explicit owned MySQL installation and shared resource root required");
        var material=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(material);
        String secret=UUID.randomUUID().toString();Function<String,String> environment=key->switch(key){case "WP_U"->USER;case "WP_P"->secret;case "T"->"modern-lito-fixture-only";default->"source-not-used";};
        try(var fixture=ModernSettlementMysqlFixture.open(Path.of(mysql))){
            install(fixture,secret);
            var properties=ModernWorldClockTestPeer.properties(Path.of(shared));
            String base=ModernWorldPolicyConfigurationTest.base(temporary,properties)
                    .replace(temporary.resolve("unused.p12").toString().replace('\\','/'),material.resolve("server.p12").toString().replace('\\','/'))
                    .replace("source.7.mysql.port=3306","source.7.mysql.port="+fixture.port());
            var rows=rows();var initial=ModernWorldPolicyTest.snapshot(1,rows);seed(fixture,initial,false);
            Path configFile=temporary.resolve("candidate.properties");writeConfig(configFile,base,fixture.port(),initial.pin());
            try(var draft=ModernServerConfig.load(configFile,environment)){
                assertNull(draft.settlement());assertThrows(Exception.class,()->ModernMain.loadMappings(draft));
                assertThrows(Exception.class,()->ModernMain.start(draft),"a draft is never listener authority");
            }
            String generated=candidate(configFile,environment);
            assertEquals(initial.pin().contentSha256(),generated);publish(fixture,initial.pin(),generated);
            assertEquals(2,probe(configFile,environment,true).code(),"published revision cannot masquerade as a candidate");
            assertEquals(0,probe(configFile,environment,false).code());
            try(var configured=ModernServerConfig.load(configFile,environment)){
                var old=ModernMain.loadMappings(configured);var key=ModernWorldPolicyTest.KEY;
                assertEquals(new ModernWorldMap.Cell(32725,32784),old.get(key).anchor());assertEquals(8000,old.get(key).clockOverrides().activeTimeoutMillis());
                var server=ModernMain.start(configured);
                try{
                    hello(server,material);
                    assertEquals(old.get(key).anchor(),serverMappings(server).get(key).anchor());
                    var nextRows=new ArrayList<>(rows);var old514=nextRows.get(1);
                    nextRows.set(1,new ModernWorldPolicy.Row(key,514,old514.manifestSha256(),ModernWorldTestPeer.ANCHOR,
                            List.of(new ModernWorldMap.Cell(32725,32784),new ModernWorldMap.Cell(32724,32785)),new ModernWorldService.ClockOverrides(null,1L,9000)));
                    var next=ModernWorldPolicyTest.snapshot(2,nextRows);seed(fixture,next,false);
                    Path secondFile=temporary.resolve("next.properties");writeConfig(secondFile,base,fixture.port(),next.pin());
                    publish(fixture,next.pin(),candidate(secondFile,environment));
                    try(var second=ModernServerConfig.load(secondFile,environment)){
                        var fresh=ModernMain.loadMappings(second);assertEquals(ModernWorldTestPeer.ANCHOR,fresh.get(key).anchor());
                        assertEquals(9000,fresh.get(key).clockOverrides().activeTimeoutMillis());assertEquals(2,fresh.get(key).spawnCamps().size());
                    }
                    assertEquals(old.get(key).anchor(),serverMappings(server).get(key).anchor());
                    assertEquals(8000,serverMappings(server).get(key).clockOverrides().activeTimeoutMillis(),"no live reload into an existing server");
                }finally{server.close();assertTrue(server.awaitTermination(5000));}
                // The runtime principal has only two SELECT grants; no hidden source/admin credential fallback.
                try(var connections=new ModernSettlementConnections(configured.worldPolicy().database(),1000,2000,1)){
                    try(var connection=connections.open(ModernSettlementMysqlFixture.deadline(5));var statement=connection.prepareStatement("UPDATE `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.HEADER+"` SET published=0")){
                        assertThrows(SQLException.class,statement::executeUpdate);
                    }finally{connections.close();assertTrue(connections.awaitTermination(5000));}
                }
                repeatableRead(fixture,configured,initial);
                invalidBatches(fixture,configured,initial,base,environment);
            }
            assertArrayEquals(new long[]{0,0,0},fixture.counts());assertEquals(0,fixture.botCount());assertEquals(0,fixture.activeConnections());assertFalse(fixture.forcedTermination());fixture.successful();
        }
    }
    static List<ModernWorldPolicy.Row> rows(){return List.of(
            new ModernWorldPolicy.Row(new ModernWorldBinding.Key(1,509),509,"1178261c2d6a968251f2eadfb2029bd0febade95b7151c15872f355784df87f8",
                    new ModernWorldMap.Cell(32740,32795),List.of(),new ModernWorldService.ClockOverrides(23000,null,7000)),
            new ModernWorldPolicy.Row(ModernWorldPolicyTest.KEY,514,ModernWorldTestPeer.MANIFEST_SHA,new ModernWorldMap.Cell(32725,32784),List.of(),new ModernWorldService.ClockOverrides(null,0L,8000)));
    }
    private static void repeatableRead(ModernSettlementMysqlFixture fixture,ModernServerConfig config,ModernWorldPolicy.Snapshot initial)throws Exception{
        var changedRows=new ArrayList<>(initial.rows());var first=changedRows.get(0);
        changedRows.set(0,new ModernWorldPolicy.Row(first.key(),first.physicalMapId(),first.manifestSha256(),new ModernWorldMap.Cell(32740,32796),first.teamCamps(),first.clocks()));
        var changed=ModernWorldPolicyTest.snapshot(initial.pin().revision(),changedRows);var fired=new AtomicBoolean();
        var connections=new ModernSettlementConnections(config.worldPolicy().database(),1000,2000,1);
        try{
            ModernMatchSettlementJdbc.ConnectionFactory interleaved=deadline->{
                Connection actual=connections.open(deadline);
                return proxy(Connection.class,actual,(method,args,result)->{
                    if(method.equals("prepareStatement")&&((String)args[0]).contains("SELECT namespace_sha256,revision,schema_version"))
                        return proxy(PreparedStatement.class,result,(operation,values,returned)->{
                            if(operation.equals("executeQuery")&&fired.compareAndSet(false,true))seed(fixture,changed,true);
                            return returned;
                        });
                    return result;
                });
            };
            assertEquals(initial,new ModernWorldPolicyJdbc(interleaved,DATABASE).read(initial.pin(),ModernSettlementMysqlFixture.deadline(10)),
                    "header and body must remain in one actual InnoDB read view while another transaction publishes changes");
            assertTrue(fired.get());assertThrows(Exception.class,()->ModernMain.loadMappings(config),"next startup rejects the changed body under its old pin");
        }finally{connections.close();assertTrue(connections.awaitTermination(5000));seed(fixture,initial,true);}
    }
    private void invalidBatches(ModernSettlementMysqlFixture fixture,ModernServerConfig config,ModernWorldPolicy.Snapshot initial,String base,Function<String,String> environment)throws Exception{
        // All mutations below are explicit test-administrator transactions in the owned disposable schema.
        for(String change:List.of("DELETE FROM `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.LAYOUT+"` WHERE logical_map_id=509 AND revision=1",
                "UPDATE `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.LAYOUT+"` SET camp0_x=32725 WHERE logical_map_id=514 AND revision=1",
                "UPDATE `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.HEADER+"` SET content_sha256=UNHEX(REPEAT('33',32)) WHERE revision=1")){
            try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5))){execute(connection,change);}
            assertThrows(Exception.class,()->ModernMain.loadMappings(config));seed(fixture,initial,true);
        }
        for(int variant=0;variant<3;variant++){
            var rows=new ArrayList<>(initial.rows());var selected=rows.get(1);
            if(variant==0)rows.add(new ModernWorldPolicy.Row(new ModernWorldBinding.Key(5,532),514,selected.manifestSha256(),selected.anchor(),List.of(),null));
            else rows.set(1,new ModernWorldPolicy.Row(selected.key(),variant==1?509:514,variant==2?"4".repeat(64):selected.manifestSha256(),selected.anchor(),List.of(),null));
            var invalid=ModernWorldPolicyTest.snapshot(10+variant,rows);seed(fixture,invalid,true);
            Path file=temporary.resolve("invalid-"+variant+".properties");writeConfig(file,base,fixture.port(),invalid.pin());
            try(var candidate=ModernServerConfig.load(file,environment)){assertThrows(Exception.class,()->ModernMain.loadMappings(candidate));}
        }
        // Header key prefixes are a real MySQL metadata failure, not merely mocked SUB_PART.
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5))){execute(connection,"ALTER TABLE `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.HEADER+"` DROP PRIMARY KEY, ADD PRIMARY KEY(namespace_sha256(8),revision)");}
        assertThrows(Exception.class,()->ModernMain.loadMappings(config));
    }
    private static void install(ModernSettlementMysqlFixture fixture,String secret)throws Exception{
        String ddl=Files.readString(Path.of("db","modern_world_policy.sql"),StandardCharsets.UTF_8);
        assertTrue(ddl.length()<16384);String stripped=ddl.lines().filter(line->!line.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(10))){
            execute(connection,"CREATE DATABASE `"+DATABASE+"`");execute(connection,"USE `"+DATABASE+"`");int statements=0;
            for(String command:stripped.split(";")){if(command.isBlank())continue;assertTrue(command.stripLeading().startsWith("CREATE TABLE lito_modern_world_"));execute(connection,command);statements++;}assertEquals(2,statements);
            try(var statement=connection.prepareStatement("CREATE USER '"+USER+"'@'127.0.0.1' IDENTIFIED BY ?")){statement.setString(1,secret);statement.setQueryTimeout(2);statement.executeUpdate();}
            for(String table:List.of(ModernWorldPolicyJdbc.HEADER,ModernWorldPolicyJdbc.LAYOUT))execute(connection,"GRANT SELECT ON `"+DATABASE+"`.`"+table+"` TO '"+USER+"'@'127.0.0.1'");
        }
    }
    private static void seed(ModernSettlementMysqlFixture fixture,ModernWorldPolicy.Snapshot snapshot,boolean published)throws Exception{
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(10))){connection.setAutoCommit(false);
            try{
                for(String table:List.of(ModernWorldPolicyJdbc.LAYOUT,ModernWorldPolicyJdbc.HEADER))try(var statement=connection.prepareStatement("DELETE FROM `"+DATABASE+"`.`"+table+"` WHERE namespace_sha256=? AND revision=?")){
                    identity(statement,snapshot.pin());statement.setQueryTimeout(2);statement.executeUpdate();}
                try(var statement=connection.prepareStatement("INSERT INTO `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.HEADER+"` (namespace_sha256,revision,schema_version,binding_count,content_sha256,published) VALUES (?,?,1,?,?,?)")){
                    identity(statement,snapshot.pin());statement.setInt(3,snapshot.rows().size());statement.setBytes(4,published?HexFormat.of().parseHex(snapshot.pin().contentSha256()):new byte[32]);statement.setInt(5,published?1:0);statement.setQueryTimeout(2);assertEquals(1,statement.executeUpdate());}
                for(var row:snapshot.rows())try(var statement=connection.prepareStatement("INSERT INTO `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.LAYOUT+"` (namespace_sha256,revision,mode,logical_map_id,physical_map_id,manifest_sha256,anchor_x,anchor_y,camp0_x,camp0_y,camp1_x,camp1_y,load_timeout_millis,countdown_nanos,active_timeout_millis) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                    identity(statement,snapshot.pin());statement.setInt(3,row.key().mode());statement.setInt(4,row.key().logicalMapId());statement.setInt(5,row.physicalMapId());statement.setBytes(6,HexFormat.of().parseHex(row.manifestSha256()));statement.setInt(7,row.anchor().x());statement.setInt(8,row.anchor().y());
                    for(int i=0;i<4;i++){Integer value=row.teamCamps().isEmpty()?null:i%2==0?row.teamCamps().get(i/2).x():row.teamCamps().get(i/2).y();nullable(statement,9+i,value,Types.INTEGER);}
                    var clock=row.clocks();nullable(statement,13,clock==null?null:clock.loadTimeoutMillis(),Types.INTEGER);nullable(statement,14,clock==null?null:clock.countdownNanos(),Types.BIGINT);nullable(statement,15,clock==null?null:clock.activeTimeoutMillis(),Types.INTEGER);statement.setQueryTimeout(2);assertEquals(1,statement.executeUpdate());
                }connection.commit();
            }catch(Exception|Error failure){connection.rollback();throw failure;}
        }
    }
    private static void publish(ModernSettlementMysqlFixture fixture,ModernWorldPolicy.Pin pin,String hash)throws Exception{
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5));var statement=connection.prepareStatement("UPDATE `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.HEADER+"` SET content_sha256=?,published=1 WHERE namespace_sha256=? AND revision=? AND published=0")){
            statement.setBytes(1,HexFormat.of().parseHex(hash));statement.setBytes(2,HexFormat.of().parseHex(pin.namespaceSha256()));statement.setLong(3,pin.revision());statement.setQueryTimeout(2);assertEquals(1,statement.executeUpdate());}
    }
    private static void nullable(PreparedStatement statement,int index,Number value,int type)throws SQLException{if(value==null)statement.setNull(index,type);else if(type==Types.INTEGER)statement.setInt(index,value.intValue());else statement.setLong(index,value.longValue());}
    private static void identity(PreparedStatement statement,ModernWorldPolicy.Pin pin)throws SQLException{statement.setBytes(1,HexFormat.of().parseHex(pin.namespaceSha256()));statement.setLong(2,pin.revision());}
    private static void execute(Connection connection,String sql)throws SQLException{try(var statement=connection.prepareStatement(sql)){statement.setQueryTimeout(3);statement.execute();}}
    private static void writeConfig(Path file,String base,int port,ModernWorldPolicy.Pin pin)throws Exception{Files.writeString(file,base+ModernWorldPolicyConfigurationTest.policy(port,pin),StandardCharsets.UTF_8);}
    private record Probe(int code,String output,String error){}
    private static Probe probe(Path file,Function<String,String> environment,boolean candidate){var out=new ByteArrayOutputStream();var error=new ByteArrayOutputStream();
        int code=ModernWorldPolicyReadProbe.run(candidate?new String[]{file.toString(),"--candidate"}:new String[]{file.toString()},environment,new PrintStream(out),new PrintStream(error));return new Probe(code,out.toString(StandardCharsets.UTF_8),error.toString(StandardCharsets.UTF_8));}
    private static String candidate(Path file,Function<String,String> environment){var result=probe(file,environment,true);assertEquals(0,result.code(),result.error());assertTrue(result.output().contains("candidate=true bindings=2 sql_writes=0 listener_started=false"));
        return result.output().lines().filter(line->line.startsWith("worldPolicy.contentSha256=")).findFirst().orElseThrow().substring("worldPolicy.contentSha256=".length());}
    @SuppressWarnings("unchecked")private static Map<ModernWorldBinding.Key,ModernWorldService.Mapping> serverMappings(ModernServer server)throws Exception{
        var field=ModernServer.class.getDeclaredField("worlds");field.setAccessible(true);Object world=field.get(server);var maps=ModernWorldService.class.getDeclaredField("mappings");maps.setAccessible(true);return (Map<ModernWorldBinding.Key,ModernWorldService.Mapping>)maps.get(world);
    }
    private static void hello(ModernServer server,Path material)throws Exception{
        try(var socket=(SSLSocket)ModernTlsTestMaterial.clientContext(material).getSocketFactory().createSocket()){
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);socket.setSoTimeout(3000);socket.connect(server.address(),1000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,15,15,131071,7,ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1)));socket.getOutputStream().flush();
            var ack=assertInstanceOf(ModernSessionProtocol.Ack.class,ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));assertEquals(15,ack.version());assertEquals(65903,ack.capabilities());
        }
    }
    @FunctionalInterface private interface After{Object apply(String name,Object[] args,Object returned)throws Throwable;}
    @SuppressWarnings("unchecked")private static <T>T proxy(Class<T> type,Object actual,After after){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{
        try{return after.apply(method.getName(),args==null?new Object[0]:args,method.invoke(actual,args));}catch(InvocationTargetException failure){throw failure.getCause();}
    });}
}
