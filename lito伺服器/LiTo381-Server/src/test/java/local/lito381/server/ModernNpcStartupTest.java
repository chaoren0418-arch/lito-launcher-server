package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Real SELECT-only NPC SQL + shared map + production startup/TLS. Player
 * login/character tables and HWND input are deliberately outside this fixture. */
class ModernNpcStartupTest {
    // Frozen v18-v25 offer: bits 0..18, through NPC. Never inherit future
    // capabilities by using KNOWN_CAPABILITIES with an old maximum version.
    static final int LEGACY_NPC_CAPABILITIES=0x0007ffff;
    @TempDir Path temporary;
    @Test void legacyStartupOffersRemainValidAndRejectNewVersionFlags()throws Exception {
        for(int version:new int[]{18,19,22,25}) {
            var hello=new ModernSessionProtocol.Hello(1,version,version,LEGACY_NPC_CAPABILITIES,7,1);
            assertEquals(hello,ModernSessionProtocol.decode(ModernSessionProtocol.encode(hello)));
            for(int incompatible:new int[]{ModernSessionProtocol.CAP_GREATER_HASTE,ModernSessionProtocol.CAP_NPC_FIRE_STORM,
                    ModernSessionProtocol.CAP_NPC_DISINTEGRATE,ModernSessionProtocol.CAP_BATTLE_DATA}) {
                assertEquals(0,LEGACY_NPC_CAPABILITIES&incompatible);
                assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(
                        new ModernSessionProtocol.Hello(1,version,version,LEGACY_NPC_CAPABILITIES|incompatible,7,1)));
            }
        }
    }
    @Test void sharedCandidateNpcLayoutCapacityEvidence()throws Exception {
        installation();var properties=shared();var parsed=ModernWorldConfiguration.parse(properties);
        var npc=ModernNpcConfigurationTest.encounter(parsed).get(ModernNpcConfigurationTest.KEY);
        var map=ModernQuestMapQualificationTest.shared();
        var mapping=new ModernWorldService.Mapping(new ModernWorldBinding(2,554,map),ModernQuestMapQualificationTest.ANCHOR,
                100_000_000,1_000_000_000,0,false,false,false,false,false,false,false,null,false,null,npc.lanes(),null,npc);
        var qualified=new java.util.ArrayList<Integer>();
        for(int capacity:new int[]{2,4,8,16,24,32,48,64,100}) {
            try{ModernWorldPolicy.qualifyLayout(mapping,capacity,()->false);qualified.add(capacity);}
            catch(ModernWorldMap.MapException rejected){System.out.println("shared554 NPC layout rejected capacity="+capacity+" reason="+rejected.getMessage());}
        }
        System.out.println("shared554 NPC configured camps qualified capacities="+qualified);
        assertTrue(qualified.contains(2));assertFalse(qualified.contains(100));
    }
    private Path installation() {
        String mysql=System.getenv(ModernSettlementMysqlFixture.ENV),shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&!mysql.isBlank()&&shared!=null&&!shared.isBlank(),"owned MySQL and shared resources required");
        return Path.of(mysql);
    }
    Properties shared()throws Exception {
        var p=ModernNpcConfigurationTest.world(temporary);var map=ModernQuestMapQualificationTest.shared();
        String prefix=ModernNpcConfigurationTest.PREFIX;var points=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,3);
        p.setProperty("world.sharedRoot",Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT")).toString().replace('\\','/'));
        var sectors=map.manifest().sectors();
        p.setProperty(prefix+"sectors",sectors.stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"sectorPins",sectors.stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"manifestSha256",map.digest());p.setProperty(prefix+"spawnX",Integer.toString(ModernQuestMapQualificationTest.ANCHOR.x()));
        p.setProperty(prefix+"spawnY",Integer.toString(ModernQuestMapQualificationTest.ANCHOR.y()));
        p.setProperty(prefix+"npcCamp0",points.get(0).x()+","+points.get(0).y());p.setProperty(prefix+"npcCamp1",points.get(2).x()+","+points.get(2).y());
        p.setProperty(prefix+"npcSpawns","45262,0,1,"+points.get(1).x()+","+points.get(1).y()+",2");
        p.setProperty(prefix+"npcCatalogDatabase",ModernNpcCatalogRealMysqlTest.FIRST);
        p.setProperty(prefix+"npcPlayerCapacity","48");return p;
    }
    private ModernServerConfig config(ModernSettlementMysqlFixture fixture,String secret,Properties p,int port)throws Exception {
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        String text=ModernNpcConfigurationTest.base(temporary)
                .replace("bind.port=0","bind.port="+port)
                .replace(temporary.resolve("unused.p12").toString().replace('\\','/'),tls.resolve("server.p12").toString().replace('\\','/'))
                .replace("mysql.port=3306","mysql.port="+fixture.port())
                .replace("mysql.database=not_a_live_database","mysql.database="+ModernNpcCatalogRealMysqlTest.FIRST)
                .replace("catalog.database=catalog","catalog.database="+ModernNpcCatalogRealMysqlTest.FIRST)
                +ModernNpcConfigurationTest.lines(p);
        return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),key->switch(key){
            case "T"->"modern-lito-fixture-only";case "U"->ModernNpcCatalogRealMysqlTest.USER;case "P"->secret;default->null;
        });
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void productionMainReadsChosenCatalogBeforeListeningAndRetainsSourceQualifiedPlan()throws Exception {
        var install=installation();String secret=UUID.randomUUID().toString();
        try(var fixture=ModernSettlementMysqlFixture.open(install)) {
            ModernNpcCatalogRealMysqlTest.install(fixture,secret);var properties=shared();
            try(var config=config(fixture,secret,properties,0)) {
                var first=ModernMain.loadMappings(config).get(ModernNpcConfigurationTest.KEY);
                assertEquals(280,first.npc().catalog().npcs().get(45262).get(ModernNpcCatalog.Field.HP));
                assertEquals(48,first.npc().playerCapacity());
                assertEquals(ModernNpcCatalogRealMysqlTest.FIRST,first.npc().catalog().database());
                assertEquals(java.util.List.of(1649),ModernNpcProtocol.Resources.from(first.npc().catalog()).graphics());
                assertEquals(0,fixture.activeConnections());
                assertThrows(Exception.class,()->ModernNpcConfiguration.load(config,System.nanoTime()-1));
                assertEquals(0,fixture.activeConnections());
                properties.setProperty(ModernNpcConfigurationTest.PREFIX+"npcCatalogDatabase",ModernNpcCatalogRealMysqlTest.SECOND);
                try(var secondConfig=config(fixture,secret,properties,0)) {
                    var second=ModernMain.loadMappings(secondConfig).get(ModernNpcConfigurationTest.KEY);
                    assertEquals(420,second.npc().catalog().npcs().get(45262).get(ModernNpcCatalog.Field.HP));
                    assertEquals(ModernNpcCatalogRealMysqlTest.FIRST,second.npc().sources().get(7).catalogDatabase(),"player equipment convention is not the NPC catalog");
                    assertEquals(280,first.npc().catalog().npcs().get(45262).get(ModernNpcCatalog.Field.HP),"earlier generation remains immutable");
                }
                var server=ModernMain.start(config);
                try {
                    for(int version:new int[]{18,19})try(var socket=(SSLSocket)ModernTlsTestMaterial.clientContext(ModernTlsTestMaterial.defaultDirectory()).getSocketFactory().createSocket()) {
                        var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);
                        socket.setSoTimeout(3000);socket.connect(server.address(),1000);socket.startHandshake();
                        int caps=LEGACY_NPC_CAPABILITIES;
                        int required=ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_COMBAT
                                |ModernSessionProtocol.CAP_EXPLICIT_I64_STATS|ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES
                                |ModernSessionProtocol.CAP_TOURNAMENT_NAVIGATION|ModernSessionProtocol.CAP_CHARACTER_DATA
                                |ModernSessionProtocol.CAP_EQUIPMENT|ModernSessionProtocol.CAP_ROOMS|ModernSessionProtocol.CAP_WORLD;
                        socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,caps,required,1)));
                        socket.getOutputStream().flush();
                        var ack=assertInstanceOf(ModernSessionProtocol.Ack.class,ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));
                        assertEquals(version,ack.version());assertNotEquals(0,ack.capabilities()&ModernSessionProtocol.CAP_NPC);
                        assertEquals(0,ack.capabilities()&~LEGACY_NPC_CAPABILITIES);
                    }
                    assertTrue(Files.exists(temporary.resolve(".modern-equipment.lock")));
                } finally {server.close();assertTrue(server.awaitTermination(5000));}
                try(var reopened=new ModernEquipmentOverrideStore(temporary)){assertFalse(reopened.isQuiescent());}
            }
            assertArrayEquals(new long[]{0,0,0},fixture.counts());assertEquals(0,fixture.activeConnections());assertFalse(fixture.forcedTermination());fixture.successful();
        }
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void missingOrUnsupportedSelectedCatalogStopsStartupWithoutListenerStoreOrFallback()throws Exception {
        var install=installation();String secret=UUID.randomUUID().toString();
        try(var fixture=ModernSettlementMysqlFixture.open(install)) {
            ModernNpcCatalogRealMysqlTest.install(fixture,secret);var properties=shared();
            int port;try(var reservation=new ServerSocket(0)){port=reservation.getLocalPort();}
            try(var c=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                s.setQueryTimeout(2);s.executeUpdate("INSERT INTO `"+ModernNpcCatalogRealMysqlTest.FIRST+"`.`mobskill` VALUES(45262,0)");
            }
            try(var config=config(fixture,secret,properties,port)) {
                var error=assertThrows(Exception.class,()->ModernMain.start(config));assertFalse(error.toString().contains(secret));
                assertFalse(Files.exists(temporary.resolve(".modern-equipment.lock")));assertEquals(0,fixture.activeConnections());
                try(var free=new ServerSocket(port)){assertEquals(port,free.getLocalPort());}
            }
            try(var c=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                s.setQueryTimeout(2);s.executeUpdate("DROP TABLE `"+ModernNpcCatalogRealMysqlTest.FIRST+"`.`npc`");
            }
            try(var config=config(fixture,secret,properties,port)) {
                assertThrows(Exception.class,()->ModernMain.start(config));assertFalse(Files.exists(temporary.resolve(".modern-equipment.lock")));
                try(var free=new ServerSocket(port)){assertEquals(port,free.getLocalPort());}
            }
            assertEquals(0,fixture.activeConnections());assertFalse(fixture.forcedTermination());fixture.successful();
        }
    }
}
