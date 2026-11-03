package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** GO-owned true TLS + qualified shared geometry + separate ranked SQL. No source rewards or combat. */
public final class ModernPetRaceTestPeer {
    static final String DATABASE="lito_race_fixture",USER="race_writer";
    static final int CAPABILITIES=196975;
    static final ModernWorldService.Limits LIMITS=new ModernWorldService.Limits(4,2,2,30000,20,new ModernBattleEngine.Limits(30000,1_000_000_000L,120000));
    static final String MANIFEST="f6c171c57eeae9e9223af51b12e89a1f0640086150abb41252e5e742c6feffde";
    private ModernPetRaceTestPeer(){}
    /** Retained candidate bands clipped to the explicitly pinned four shared sectors, not an LT map lookup. */
    static List<ModernPetRace.Gate> gates(){return List.of(
            g(32762,32762,32845,32895),g(32754,32754,32845,32895),g(32748,32748,32845,32895),
            g(32704,32743,32844,32844),g(32704,32742,32840,32840),g(32704,32742,32835,32835),g(32704,32742,32830,32830),g(32704,32742,32826,32826),g(32704,32742,32822,32822),
            g(32749,32749,32768,32818),g(32755,32755,32768,32818),g(32760,32760,32768,32818),g(32765,32765,32768,32818),g(32770,32770,32768,32818),
            g(32775,32775,32768,32818),g(32780,32780,32768,32818),g(32785,32785,32768,32818),g(32789,32789,32768,32818),
            g(32792,32831,32821,32821),g(32793,32831,32826,32826),g(32793,32831,32831,32831),g(32793,32831,32836,32836),g(32793,32831,32842,32842),
            g(32790,32790,32845,32895),g(32785,32785,32845,32895),g(32780,32780,32845,32895),g(32775,32775,32845,32895),g(32770,32770,32845,32895),g(32764,32764,32845,32895));}
    private static ModernPetRace.Gate g(int a,int b,int c,int d){return new ModernPetRace.Gate(a,b,c,d);}
    static Properties properties(Path root){
        var p=new Properties();String prefix="world.0.565.";p.setProperty("world.sharedRoot",root.toAbsolutePath().toString().replace('\\','/'));p.setProperty("world.bindings","0:565");
        p.setProperty(prefix+"physicalMap","5143");p.setProperty(prefix+"sectors","7fff8000,7fff8001,80008000,80008001");p.setProperty(prefix+"spawnX","32768");p.setProperty(prefix+"spawnY","32849");
        p.setProperty(prefix+"stepMillis","1");p.setProperty(prefix+"manifestSha256",MANIFEST);p.setProperty(prefix+"sectorPins",ModernPetRaceMapQualificationTest.PINS.stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"raceRules",ModernPetRace.RULESET);p.setProperty(prefix+"raceGraphicsId","2145");p.setProperty(prefix+"raceGates",gates().stream().map(g->g.minX()+","+g.maxX()+","+g.minY()+","+g.maxY()).collect(java.util.stream.Collectors.joining(";")));
        p.setProperty(prefix+"raceCourseSha256",ModernPetRace.courseHash(new ModernWorldBinding.Key(0,565),new ModernWorldMap.Manifest(5143,ModernPetRaceMapQualificationTest.PINS,MANIFEST),gates(),2145,1_000_000));return p;
    }
    public static void main(String[] args){
        serve(args,16);
    }
    static void serve(String[] args,int protocol){
        try{
            if(protocol!=16&&protocol!=41&&protocol!=47)throw new IllegalArgumentException("test protocol");
            if(args.length!=4||!args[0].equals("serve")||!List.of("race-finish","race-withdraw","race-disconnect").contains(args[2]))throw new IllegalArgumentException();
            int lifetime=Integer.parseInt(args[3]);if(lifetime<5000||lifetime>180000)throw new IllegalArgumentException();
            String shared=System.getenv("LITO_SHARED_RESOURCE_ROOT"),mysql=System.getenv(ModernSettlementMysqlFixture.ENV);if(shared==null||shared.isBlank()||mysql==null||mysql.isBlank())throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalArgumentException("missing owner gate");
            try(var sql=ModernSettlementMysqlFixture.open(Path.of(mysql))){run(Path.of(args[1]),Path.of(shared),args[2],lifetime,sql,protocol);sql.successful();}
        }catch(Exception failure){System.err.println("MODERN_PET_RACE_PEER_FAILED");System.exit(2);}
    }
    private static void run(Path tls,Path shared,String mode,int lifetime,ModernSettlementMysqlFixture fixture,int protocol)throws Exception{
        String password=UUID.randomUUID().toString();install(fixture,password);var outboxDirectory=Files.createDirectory(fixture.artifactDirectory().resolve("race-outbox"));
        String text=config(tls,fixture.port(),outboxDirectory,properties(shared));
        try(var config=ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),key->switch(key){case "R_U"->USER;case "R_P"->password;case "T"->"modern-lito-fixture-only";default->"source-not-used";})){
            if(config.settlement()!=null||config.shopAllowance()!=0||!config.worlds().raceEnabled())throw new IllegalStateException("wrong race configuration");
            var mappings=ModernMain.loadMappings(config);if(mappings.size()!=1)throw new IllegalStateException("wrong race bindings");
            var mapping=mappings.values().iterator().next();if(mapping.race()==null||mapping.race().gates().size()!=29||!mapping.anchor().equals(ModernPetRaceMapQualificationTest.ANCHOR))throw new IllegalStateException("course bypass");
            var presence=new TournamentPresence();var rooms=new ModernRooms(mappings.keySet());var backend=new ModernApplicationTestBackend("auth",80);
            var reads=new int[2];var recordsRead=new int[2];
            ModernCharacterRepository characters=(principal,source)->{if(!principal.authorizesSource(source)||source!=7&&source!=42)throw new SecurityException();
                synchronized(reads){reads[source==7?0:1]++;}return backend.characters.characters(principal,source);};
            var recordStore=ModernCharacterRecordRepository.jdbc(fixture::openConnection,ModernSettlementMysqlFixture.DATABASE);
            ModernCharacterRecordRepository records=(principal,source,object)->{if(object!=3333||source!=7&&source!=42)throw new SecurityException();
                var value=recordStore.character(principal,source,object);synchronized(recordsRead){recordsRead[source==7?0:1]++;}return value;};
            var saved=new AtomicReference<ModernPetRaceSettlement.Event>();var connections=new ModernSettlementConnections(config.raceSettlement().database(),1000,2000,2);
            try(var outbox=new ModernPetRaceOutbox(outboxDirectory,ModernPetRaceSettlement.binding(config))){
                var sql=new ModernPetRaceSettlementJdbc(connections,DATABASE);
                try(var journal=new ModernPetRaceSettlementCoordinator(outbox,(event,deadline)->{sql.persist(event,deadline);var previous=saved.get();if(previous!=null&&!previous.sameAuthority(event))throw new IllegalStateException("different race result");saved.set(event);},2000,connections);
                    var worlds=new ModernWorldService(rooms,mappings,LIMITS,bound->{throw new IllegalStateException("race cannot borrow combat RNG");},null,java.util.Map.of(),journal)){
                    ModernServer server=null;
                    try{
                        server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),ModernTlsTestMaterial.serverContext(tls,false),new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),
                                backend.verifier,characters,presence,ModernCharacterDataRepository.unavailable(),null,null,rooms,worlds,protocol,ModernSkillsRepository.unavailable(),records,null);
                        System.out.println("READY "+server.address().getPort());System.out.flush();System.err.println("PET_RACE courseSha256="+mapping.race().digest()+" manifest="+MANIFEST+" resultNamespace=ranked-only");System.err.flush();
                        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                        for(;;){var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                            if(state.accepted()>2||server.listenerFailed()||deadline-System.nanoTime()<=0)throw new IllegalStateException("bounded race peer incomplete");Thread.sleep(10);}
                        if(server.snapshot().failed()!=0||backend.authenticationCalls.get()!=4||reads[0]<2||reads[1]<2||recordsRead[0]<1||recordsRead[1]<1||saved.get()==null)throw new IllegalStateException("race source/result not exercised");
                        var event=saved.get();int winner=mode.equals("race-withdraw")?1:0,loser=1-winner;
                        if(event.rows().size()!=2||event.rows().get(winner).runner().placement()!=1||event.rows().get(winner).runner().completedLaps()!=4||event.rows().get(loser).runner().dnf()!=(mode.equals("race-finish")?ModernPetRace.Dnf.WINDOW_CLOSED:mode.equals("race-withdraw")?ModernPetRace.Dnf.LEFT:ModernPetRace.Dnf.DISCONNECTED))throw new IllegalStateException("race result differs from scenario");
                        // A second real SQL replay must retain one exact body and the first audit timestamp.
                        sql.persist(event,ModernSettlementMysqlFixture.deadline(5));audit(fixture,event);
                    }finally{
                        if(server!=null){server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("race TLS owner did not retire");}
                        worlds.close();if(!worlds.awaitTermination(5000)||!worlds.isQuiescent()||worlds.registrationCount()!=0||presence.ownerCount()!=0||presence.userCount()!=0||rooms.ownerCount()!=0||rooms.roomCount()!=0||rooms.memberCount()!=0)throw new IllegalStateException("race world owner did not retire");
                    }
                }
            }finally{connections.close();if(!connections.awaitTermination(5000))throw new IllegalStateException("race SQL owner did not retire");}
        }
        if(!Arrays.equals(new long[]{0,0,0},fixture.counts())||fixture.botCount()!=0||fixture.activeConnections()!=0||fixture.forcedTermination())throw new IllegalStateException("race wrote DM/source result or leaked owner");
    }
    private static String config(Path tls,int port,Path outbox,Properties properties){
        var text=new StringBuilder("schema=1\nbind.port=0\ntls.pkcs12="+tls.resolve("server.p12").toAbsolutePath().toString().replace('\\','/')+"\ntls.password.env=T\nsources=7,42\n");
        for(int id:List.of(7,42))text.append("source.").append(id).append(".name=Source").append(id).append('\n').append("source.").append(id).append(".mysql.host=127.0.0.1\nsource.").append(id).append(".mysql.port=").append(port)
                .append("\nsource.").append(id).append(".mysql.database=unused_source_").append(id).append("\nsource.").append(id).append(".mysql.username.env=U\nsource.").append(id).append(".mysql.password.env=P\nsource.").append(id).append(".mysql.passwordScheme=plain\nsource.").append(id).append(".mysql.tls=disabled\n");
        properties.stringPropertyNames().stream().sorted().forEach(k->text.append(k).append('=').append(properties.getProperty(k)).append('\n'));
        return text+"raceSettlement.enabled=true\nraceSettlement.outboxDirectory="+outbox.toString().replace('\\','/')+"\nraceSettlement.mysql.host=127.0.0.1\nraceSettlement.mysql.port="+port+"\nraceSettlement.mysql.database="+DATABASE+"\nraceSettlement.mysql.username.env=R_U\nraceSettlement.mysql.password.env=R_P\nraceSettlement.mysql.tls=disabled\n";
    }
    static void install(ModernSettlementMysqlFixture fixture,String password)throws Exception{
        String ddl=Files.readString(Path.of("db","modern_pet_race_settlement.sql"),StandardCharsets.UTF_8);if(ddl.length()>16384)throw new IllegalStateException("DDL bound");
        String clean=ddl.lines().filter(l->!l.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(10))){execute(connection,"CREATE DATABASE `"+DATABASE+"`");execute(connection,"USE `"+DATABASE+"`");int count=0;
            for(String command:clean.split(";")){if(command.isBlank())continue;if(!command.stripLeading().startsWith("CREATE TABLE `lito_modern_pet_race_"))throw new IllegalStateException("DDL scope");execute(connection,command);count++;}if(count!=2)throw new IllegalStateException("DDL count");
            try(var s=connection.prepareStatement("CREATE USER '"+USER+"'@'127.0.0.1' IDENTIFIED BY ?")){s.setString(1,password);s.setQueryTimeout(2);s.executeUpdate();}
            for(String table:List.of(ModernPetRaceSettlementJdbc.RESULT,ModernPetRaceSettlementJdbc.MEMBER))execute(connection,"GRANT SELECT,INSERT ON `"+DATABASE+"`.`"+table+"` TO '"+USER+"'@'127.0.0.1'");
        }
    }
    private static void execute(java.sql.Connection connection,String sql)throws Exception{try(var statement=connection.prepareStatement(sql)){statement.setQueryTimeout(2);statement.execute();}}
    private static void audit(ModernSettlementMysqlFixture fixture,ModernPetRaceSettlement.Event event)throws Exception{
        try(var connection=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5))){
            for(String table:List.of(ModernPetRaceSettlementJdbc.RESULT,ModernPetRaceSettlementJdbc.MEMBER))try(var statement=connection.prepareStatement("SELECT COUNT(*) FROM `"+DATABASE+"`.`"+table+"`")){
                statement.setQueryTimeout(2);try(var rows=statement.executeQuery()){if(!rows.next()||rows.getLong(1)!=(table.equals(ModernPetRaceSettlementJdbc.RESULT)?1:2)||rows.next())throw new IllegalStateException("ranked SQL count");}}
            try(var statement=connection.prepareStatement("SELECT recorded_at_millis,content_hash,event_body FROM `"+DATABASE+"`.`"+ModernPetRaceSettlementJdbc.RESULT+"`")){
                statement.setQueryTimeout(2);try(var rows=statement.executeQuery()){if(!rows.next()||rows.getLong(1)!=event.recordedAtMillis()||!Arrays.equals(rows.getBytes(2),java.util.HexFormat.of().parseHex(event.contentSha256()))||!Arrays.equals(rows.getBytes(3),event.authoritativeBody())||rows.next())throw new IllegalStateException("ranked SQL authority");}}
        }
    }
}
