package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Types;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** GO-gated true dedicated SQL -> production loader -> two Scene worlds. No production schema, source write or JUnit runtime dependency. */
public final class ModernWorldPolicyTestPeer {
    static final String DATABASE="world_policy_fixture",USER="wp_reader",NAMESPACE="5".repeat(64);
    static final ModernWorldMap.Cell ANCHOR514=new ModernWorldMap.Cell(32725,32784),ANCHOR509=new ModernWorldMap.Cell(32740,32795);
    private ModernWorldPolicyTestPeer(){}
    public static void main(String[] args){
        try{
            if(args.length!=4||!"serve".equals(args[0])||!"sql-world-policy".equals(args[2]))throw new IllegalArgumentException();
            int lifetime=Integer.parseInt(args[3]);if(lifetime<5000||lifetime>180000)throw new IllegalArgumentException();
            String shared=System.getenv("LITO_SHARED_RESOURCE_ROOT"),mysql=System.getenv(ModernSettlementMysqlFixture.ENV);
            if(shared==null||shared.isBlank()||mysql==null||mysql.isBlank())throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalArgumentException("missing owner gate");
            try(var sql=ModernSettlementMysqlFixture.open(Path.of(mysql))){run(Path.of(args[1]),Path.of(shared),lifetime,sql);sql.successful();}
        }catch(Exception failure){System.err.println("MODERN_WORLD_POLICY_PEER_FAILED");System.exit(2);}
    }
    private static void run(Path tls,Path shared,int lifetime,ModernSettlementMysqlFixture sql)throws Exception{
        String password=UUID.randomUUID().toString();var rows=List.of(
                new ModernWorldPolicy.Row(new ModernWorldBinding.Key(1,509),509,"1178261c2d6a968251f2eadfb2029bd0febade95b7151c15872f355784df87f8",ANCHOR509,List.of(),new ModernWorldService.ClockOverrides(25000,2_000_000_000L,6000)),
                new ModernWorldPolicy.Row(new ModernWorldBinding.Key(1,514),514,ModernWorldTestPeer.MANIFEST_SHA,ANCHOR514,List.of(),new ModernWorldService.ClockOverrides(null,1_000_000_000L,4000)));
        var pin=new ModernWorldPolicy.Pin(NAMESPACE,1,ModernWorldPolicy.contentHash(NAMESPACE,1,rows));install(sql,password,pin,rows);
        var properties=ModernWorldClockTestPeer.properties(shared);
        // Local values intentionally differ. A bypass of the SQL overlay must fail the external clock/spawn assertions.
        for(int map:List.of(514,509)){
            String prefix="world.1."+map+".";properties.remove(prefix+"loadTimeoutMillis");
            properties.setProperty(prefix+"countdownNanos","3000000000");properties.setProperty(prefix+"activeTimeoutMillis","45000");
        }
        String configuration=config(tls,sql.port(),pin,properties);
        try(var config=ModernServerConfig.parse(configuration.getBytes(StandardCharsets.UTF_8),key->switch(key){case "WP_U"->USER;case "WP_P"->password;case "T"->"modern-lito-fixture-only";default->"source-not-used";})){
            if(config.settlement()!=null)throw new IllegalStateException("unexpected writer");
            var candidate=ModernMain.loadWorldPolicy(config,true);if(!candidate.snapshot().pin().equals(pin))throw new IllegalStateException("candidate hash differs");
            publish(sql,pin);
            var mappings=ModernMain.loadMappings(config);
            for(var mapping:mappings.values()){
                int map=mapping.binding().key().logicalMapId();var descriptor=ModernMatchDescriptor.of(mapping,ModernWorldClockTestPeer.GLOBAL_LIMITS,false);
                if(!mapping.anchor().equals(map==514?ANCHOR514:ANCHOR509)||descriptor.features()!=0||descriptor.settlementEnabled()
                        ||descriptor.preparationTimeoutMillis()!=3000||descriptor.loadTimeoutMillis()!=(map==514?30000:25000)
                        ||descriptor.countdownNanos()!=(map==514?1_000_000_000L:2_000_000_000L)||descriptor.activeTimeoutMillis()!=(map==514?4000:6000))throw new IllegalStateException("SQL mapping not applied");
            }
            var presence=new TournamentPresence();var rooms=new ModernRooms(mappings.keySet());var backend=new ModernApplicationTestBackend("auth",80);
            var sunReads=new AtomicInteger();var moonReads=new AtomicInteger();var sunRecords=new AtomicInteger();var moonRecords=new AtomicInteger();
            ModernCharacterRepository characters=(principal,source)->{
                if(!principal.authorizesSource(source)||source!=7&&source!=42)throw new SecurityException();
                String expected=source==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT;
                if(!expected.equals(principal.accountForSource(source)))throw new SecurityException();
                (source==7?sunReads:moonReads).incrementAndGet();return backend.characters.characters(principal,source);
            };
            var recordStore=ModernCharacterRecordRepository.jdbc(sql::openConnection,ModernSettlementMysqlFixture.DATABASE);
            ModernCharacterRecordRepository records=(principal,source,object)->{
                if(object!=3333||source!=7&&source!=42)throw new SecurityException();var result=recordStore.character(principal,source,object);
                (source==7?sunRecords:moonRecords).incrementAndGet();return result;
            };
            try(var worlds=new ModernWorldService(rooms,mappings,ModernWorldClockTestPeer.GLOBAL_LIMITS,bound->{throw new IllegalStateException("Scene cannot draw RNG");})){
                ModernServer server=null;
                try{
                    server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),ModernTlsTestMaterial.serverContext(tls,false),
                            new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),backend.verifier,characters,presence,
                            ModernCharacterDataRepository.unavailable(),null,null,rooms,worlds,15,ModernSkillsRepository.unavailable(),records,null);
                    System.out.println("READY "+server.address().getPort());System.out.flush();
                    System.err.println("WORLD_POLICY revision=1 contentSha256="+pin.contentSha256()+" manifest514="+rows.get(1).manifestSha256()+" manifest509="+rows.get(0).manifestSha256());System.err.flush();
                    long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                    for(;;){var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                        if(state.accepted()>2||server.listenerFailed()||deadline-System.nanoTime()<=0)throw new IllegalStateException("policy peer incomplete");Thread.sleep(10);}
                    if(server.snapshot().failed()!=0||backend.authenticationCalls.get()!=4||sunReads.get()<3||moonReads.get()<3||sunRecords.get()<2||moonRecords.get()<2)
                        throw new IllegalStateException("SQL worlds and record reads not exercised");
                }finally{
                    if(server!=null){server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("TLS owner did not retire");}
                    worlds.close();if(!worlds.awaitTermination(5000)||!worlds.isQuiescent()||worlds.registrationCount()!=0||presence.ownerCount()!=0||presence.userCount()!=0
                            ||rooms.ownerCount()!=0||rooms.roomCount()!=0||rooms.memberCount()!=0)throw new IllegalStateException("world owner did not retire");
                }
            }
        }
        if(!Arrays.equals(new long[]{0,0,0},sql.counts())||sql.botCount()!=0||sql.activeConnections()!=0||sql.forcedTermination())throw new IllegalStateException("unexpected SQL result or owner leak");
    }
    private static String config(Path tls,int port,ModernWorldPolicy.Pin pin,Properties worlds){
        return "schema=1\nbind.port=0\ntls.pkcs12="+tls.resolve("server.p12").toAbsolutePath().toString().replace('\\','/')+"\ntls.password.env=T\nsources=7\n"+
                "source.7.name=Source\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port="+port+"\nsource.7.mysql.database=unused_source\nsource.7.mysql.username.env=U\nsource.7.mysql.password.env=P\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n"+
                worlds.stringPropertyNames().stream().sorted().map(k->k+"="+worlds.getProperty(k).replace(" ","\\ ")+"\n").reduce("",String::concat)+
                "worldPolicy.enabled=true\nworldPolicy.namespaceSha256="+pin.namespaceSha256()+"\nworldPolicy.revision=1\nworldPolicy.contentSha256="+pin.contentSha256()+
                "\nworldPolicy.mysql.host=127.0.0.1\nworldPolicy.mysql.port="+port+"\nworldPolicy.mysql.database="+DATABASE+"\nworldPolicy.mysql.username.env=WP_U\nworldPolicy.mysql.password.env=WP_P\nworldPolicy.mysql.tls=disabled\n";
    }
    private static void install(ModernSettlementMysqlFixture fixture,String password,ModernWorldPolicy.Pin pin,List<ModernWorldPolicy.Row> rows)throws Exception{
        String ddl=Files.readString(Path.of("db","modern_world_policy.sql"),StandardCharsets.UTF_8);if(ddl.length()>16384)throw new IllegalStateException("DDL fixture bound");
        String clean=ddl.lines().filter(line->!line.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(10))){
            execute(connection,"CREATE DATABASE `"+DATABASE+"`");execute(connection,"USE `"+DATABASE+"`");int count=0;
            for(String command:clean.split(";")){if(command.isBlank())continue;if(!command.stripLeading().startsWith("CREATE TABLE lito_modern_world_"))throw new IllegalStateException("DDL fixture scope");execute(connection,command);count++;}
            if(count!=2)throw new IllegalStateException("DDL fixture count");
            try(var statement=connection.prepareStatement("CREATE USER '"+USER+"'@'127.0.0.1' IDENTIFIED BY ?")){statement.setString(1,password);statement.setQueryTimeout(2);statement.executeUpdate();}
            for(String table:List.of(ModernWorldPolicyJdbc.HEADER,ModernWorldPolicyJdbc.LAYOUT))execute(connection,"GRANT SELECT ON `"+DATABASE+"`.`"+table+"` TO '"+USER+"'@'127.0.0.1'");
            connection.setAutoCommit(false);
            try{
                try(var statement=connection.prepareStatement("INSERT INTO `"+ModernWorldPolicyJdbc.HEADER+"` (namespace_sha256,revision,schema_version,binding_count,content_sha256,published) VALUES (?,1,1,2,?,0)")){
                    statement.setBytes(1,HexFormat.of().parseHex(pin.namespaceSha256()));statement.setBytes(2,new byte[32]);statement.setQueryTimeout(2);if(statement.executeUpdate()!=1)throw new IllegalStateException();}
                for(var row:rows)try(var statement=connection.prepareStatement("INSERT INTO `"+ModernWorldPolicyJdbc.LAYOUT+"` (namespace_sha256,revision,mode,logical_map_id,physical_map_id,manifest_sha256,anchor_x,anchor_y,camp0_x,camp0_y,camp1_x,camp1_y,load_timeout_millis,countdown_nanos,active_timeout_millis) VALUES (?,1,?,?,?,?,?,?,NULL,NULL,NULL,NULL,?,?,?)")){
                    statement.setBytes(1,HexFormat.of().parseHex(pin.namespaceSha256()));statement.setInt(2,row.key().mode());statement.setInt(3,row.key().logicalMapId());statement.setInt(4,row.physicalMapId());statement.setBytes(5,HexFormat.of().parseHex(row.manifestSha256()));statement.setInt(6,row.anchor().x());statement.setInt(7,row.anchor().y());
                    if(row.clocks().loadTimeoutMillis()==null)statement.setNull(8,Types.INTEGER);else statement.setInt(8,row.clocks().loadTimeoutMillis());statement.setLong(9,row.clocks().countdownNanos());statement.setInt(10,row.clocks().activeTimeoutMillis());statement.setQueryTimeout(2);if(statement.executeUpdate()!=1)throw new IllegalStateException();
                }connection.commit();
            }catch(Exception failure){connection.rollback();throw failure;}
        }
    }
    private static void publish(ModernSettlementMysqlFixture fixture,ModernWorldPolicy.Pin pin)throws Exception{
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5));var statement=connection.prepareStatement("UPDATE `"+DATABASE+"`.`"+ModernWorldPolicyJdbc.HEADER+"` SET content_sha256=?,published=1 WHERE namespace_sha256=? AND revision=1 AND published=0")){
            statement.setBytes(1,HexFormat.of().parseHex(pin.contentSha256()));statement.setBytes(2,HexFormat.of().parseHex(pin.namespaceSha256()));statement.setQueryTimeout(2);if(statement.executeUpdate()!=1)throw new IllegalStateException("draft publish failed");}
    }
    private static void execute(Connection connection,String sql)throws Exception{try(var statement=connection.prepareStatement(sql)){statement.setQueryTimeout(3);statement.execute();}}
}
