package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Real startup config/catalog/shared geometry/TLS and durable replay. Login
 * and the live battle are NOT supplied by this database fixture. */
class ModernQuestStartupTest {
    @TempDir Path temporary;
    private Path installation(){var value=System.getenv(ModernSettlementMysqlFixture.ENV);Assumptions.assumeTrue(value!=null&&System.getenv("LITO_SHARED_RESOURCE_ROOT")!=null);return Path.of(value);}
    private Properties shared()throws Exception {
        Files.createDirectory(temporary.resolve("quest-results"));
        var ordinary=new ModernNpcStartupTest();ordinary.temporary=temporary;var p=ordinary.shared();String prefix=ModernNpcConfigurationTest.PREFIX;
        var spawn=p.getProperty(prefix+"npcSpawns").split(",");p.remove(prefix+"npcSpawns");
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++)
            p.setProperty(prefix+ModernQuestConfiguration.waveKey(lane,stage),(stage==1?45262:45263)+","+spawn[3]+","+spawn[4]+",2");
        p.setProperty(prefix+"npcIds","45262,45263");p.setProperty(prefix+"npcCamp1",p.getProperty(prefix+"npcCamp0"));p.setProperty(prefix+"npcPlayerCapacity","4");
        p.setProperty(prefix+"questRules",ModernQuestConfiguration.RULESET);p.setProperty(prefix+"questLives",ModernQuestConfiguration.LIVES);
        var map=ModernQuestMapQualificationTest.shared();var camp=ModernQuestMapQualificationTest.ANCHOR;
        var target=map.serverChosenSpawns(camp,8).stream().filter(c->!c.equals(camp)&&!(c.x()==Integer.parseInt(spawn[3])&&c.y()==Integer.parseInt(spawn[4]))
                &&Math.max(Math.abs(c.x()-camp.x()),Math.abs(c.y()-camp.y()))==1).findFirst().orElseThrow();
        p.setProperty(prefix+"questObjectives","0,4,"+target.x()+","+target.y()+",2665,9007199254740993;1,4,"+target.x()+","+target.y()+",2669,9007199254740993");
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);p.setProperty(prefix+"regenRules",ModernWorldConfiguration.REGEN_RULESET);
        return p;
    }
    private ModernServerConfig config(ModernSettlementMysqlFixture mysql,String secret,Properties p,int port)throws Exception {
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        String base=ModernQuestConfigurationTest.text(temporary,p)
                .replace("bind.port=0","bind.port="+port)
                .replace(temporary.resolve("unused.p12").toString().replace('\\','/'),tls.resolve("server.p12").toString().replace('\\','/'))
                .replace("mysql.port=3306","mysql.port="+mysql.port())
                .replace("mysql.database=not_a_live_database","mysql.database="+ModernNpcCatalogRealMysqlTest.FIRST)
                .replace("catalog.database=catalog","catalog.database="+ModernNpcCatalogRealMysqlTest.FIRST)
                .replace("settlement.mysql.database=modern_results","settlement.mysql.database="+ModernSettlementMysqlFixture.DATABASE);
        // Both source identities present in the actual engine replay event.
        String second=base.lines().filter(s->s.startsWith("source.7.")).map(s->s.replace("source.7.","source.42.").replace("name=Configured","name=Configured42")).collect(java.util.stream.Collectors.joining("\n"));
        base=base.replace("sources=7\n","sources=7,42\n")+second+"\n";
        return ModernServerConfig.parse(base.getBytes(StandardCharsets.UTF_8),name->switch(name){
            case "T"->"modern-lito-fixture-only";case "U"->ModernNpcCatalogRealMysqlTest.USER;case "P","WP"->secret;case "WU"->"quest_writer";default->null;
        });
    }
    private void install(ModernSettlementMysqlFixture mysql,String secret)throws Exception {
        ModernNpcCatalogRealMysqlTest.install(mysql,secret);
        try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
            s.setQueryTimeout(3);
            String columns="name,nameid,impl,size,"+java.util.Arrays.stream(ModernNpcCatalog.Field.values()).map(f->"`"+f.column()+"`").collect(java.util.stream.Collectors.joining(","));
            for(String db:List.of(ModernNpcCatalogRealMysqlTest.FIRST,ModernNpcCatalogRealMysqlTest.SECOND))
                s.executeUpdate("INSERT INTO `"+db+"`.`npc` (npcid,"+columns+") SELECT 45263,"+columns+" FROM `"+db+"`.`npc` WHERE npcid=45262");
            try(var create=c.prepareStatement("CREATE USER 'quest_writer'@'127.0.0.1' IDENTIFIED BY ?")){create.setString(1,secret);create.setQueryTimeout(3);create.executeUpdate();}
            s.executeUpdate("GRANT SELECT,INSERT,UPDATE ON `"+ModernSettlementMysqlFixture.DATABASE+"`.* TO 'quest_writer'@'127.0.0.1'");
        }
    }
    private static void hello(ModernServer server)throws Exception {
        try(var s=(SSLSocket)ModernTlsTestMaterial.clientContext(ModernTlsTestMaterial.defaultDirectory()).getSocketFactory().createSocket()) {
            var parameters=s.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");s.setSSLParameters(parameters);s.setSoTimeout(3000);s.connect(server.address(),1000);s.startHandshake();
            s.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,22,22,ModernNpcStartupTest.LEGACY_NPC_CAPABILITIES,0,1)));s.getOutputStream().flush();
            var ack=assertInstanceOf(ModernSessionProtocol.Ack.class,ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(s.getInputStream())));
            assertEquals(22,ack.version());assertEquals(0,ack.capabilities()&~ModernNpcStartupTest.LEGACY_NPC_CAPABILITIES);
        }
    }
    @Test @Timeout(value=150,unit=TimeUnit.SECONDS)
    void configuredQuestOnlyMainLoadsFutureCatalogStartsV22AndReplaysRealEngineResultExactlyOnce()throws Exception {
        String secret=UUID.randomUUID().toString();
        try(var mysql=ModernSettlementMysqlFixture.open(installation())) {
            install(mysql,secret);var p=shared();
            try(var config=config(mysql,secret,p,0)) {
                var mapping=ModernMain.loadMappings(config).get(ModernNpcConfigurationTest.KEY);var plan=mapping.npc().questPlan();
                assertTrue(plan.participantLives());assertEquals(45263,plan.wave(1,4).spawns().get(0).template().npcId());
                assertEquals(280,mapping.npc().catalog().npcs().get(45263).get(ModernNpcCatalog.Field.HP));
                assertEquals(0,mysql.activeConnections());
                var event=ModernQuestSettlementTest.victories().get(0);byte[] binding=ModernSettlementCoordinator.binding(config);
                try(var box=new ModernMatchOutbox(config.settlement().outboxDirectory(),binding)){box.record(event,ModernQuestSettlementTest.deadline());}
                for(int restart=0;restart<2;restart++) {
                    var server=ModernMain.start(config);
                    try {
                        hello(server);long deadline=ModernQuestSettlementTest.deadline();
                        while(mysql.counts()[0]!=1){assertTrue(System.nanoTime()<deadline);Thread.sleep(10);}
                    }finally{server.close();assertTrue(server.awaitTermination(5000));}
                    assertArrayEquals(new long[]{1,2,2},mysql.counts());
                    try(var box=new ModernMatchOutbox(config.settlement().outboxDirectory(),binding)){assertTrue(box.pending(ModernQuestSettlementTest.deadline()).isEmpty());}
                }
                var changed=(Properties)p.clone();changed.setProperty(ModernNpcConfigurationTest.PREFIX+"npcCatalogDatabase",ModernNpcCatalogRealMysqlTest.SECOND);
                try(var next=config(mysql,secret,changed,0)) {
                    var later=ModernMain.loadMappings(next).get(ModernNpcConfigurationTest.KEY);
                    assertEquals(420,later.npc().catalog().npcs().get(45263).get(ModernNpcCatalog.Field.HP));
                    assertEquals(280,mapping.npc().catalog().npcs().get(45263).get(ModernNpcCatalog.Field.HP));
                    assertThrows(java.io.IOException.class,()->new ModernMatchOutbox(next.settlement().outboxDirectory(),ModernSettlementCoordinator.binding(next)));
                }
            }
            assertEquals(0,mysql.activeConnections());assertFalse(mysql.forcedTermination());mysql.successful();
        }
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void unsupportedFutureOnlyNpcPreventsListenerEquipmentAndOutboxCreation()throws Exception {
        String secret=UUID.randomUUID().toString();
        try(var mysql=ModernSettlementMysqlFixture.open(installation())) {
            install(mysql,secret);var p=shared();int port;try(var reservation=new ServerSocket(0)){port=reservation.getLocalPort();}
            for(String mutation:List.of("UPDATE `npc_catalog_a`.`npc` SET boss=1 WHERE npcid=45263",
                    "UPDATE `npc_catalog_a`.`npc` SET boss=0 WHERE npcid=45263", "INSERT INTO `npc_catalog_a`.`mobskill` VALUES(45263,0)")) {
                try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()){s.setQueryTimeout(2);s.executeUpdate(mutation);}
                if(mutation.contains("boss=0"))continue;
                try(var config=config(mysql,secret,p,port)) {
                    var failure=assertThrows(Exception.class,()->ModernMain.start(config));assertFalse(failure.toString().contains(secret));
                    assertFalse(Files.exists(temporary.resolve(".modern-equipment.lock")));
                    try(var contents=Files.list(config.settlement().outboxDirectory())){assertEquals(0,contents.count());}
                    try(var free=new ServerSocket(port)){assertEquals(port,free.getLocalPort());}
                }
            }
            assertArrayEquals(new long[]{0,0,0},mysql.counts());assertEquals(0,mysql.activeConnections());assertFalse(mysql.forcedTermination());mysql.successful();
        }
    }
}
