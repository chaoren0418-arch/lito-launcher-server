package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernShopConfigTest {
    @TempDir Path directory;
    String portable(Path path){return path.toAbsolutePath().toString().replace('\\','/');}
    String base(){return "schema=1\nbind.port=0\ntls.pkcs12="+portable(directory.resolve("server.p12"))+"\ntls.password.env=T\nsources=7\n"
            +"source.7.name=Sun\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\nsource.7.mysql.database=readonly_source\n"
            +"source.7.mysql.username.env=U\nsource.7.mysql.password.env=P\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";}
    String world(){return "source.7.catalog.database=readonly_catalog\nequipment.storeDirectory="+portable(directory)+"\nworld.sharedRoot="+portable(directory)
            +"\nworld.bindings=1:514\nworld.1.514.physicalMap=514\nworld.1.514.sectors=7fff8000\nworld.1.514.spawnX=32724\nworld.1.514.spawnY=32784\nworld.1.514.stepMillis=50\n"
            +"world.1.514.combatRules="+ModernPhysicalCombat.RULESET+"\nworld.1.514.attackMillis=1000\nworld.1.514.itemRules="+ModernWorldConfiguration.ITEM_RULESET+"\n";}
    ModernServerConfig parse(String source)throws Exception{return ModernServerConfig.parse(source.getBytes(StandardCharsets.UTF_8),name->"fixture-only");}
    void rejectedBeforeSecrets(String source){var count=new AtomicInteger();assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(source.getBytes(StandardCharsets.UTF_8),name->{count.incrementAndGet();return "fixture-only";}));assertEquals(0,count.get());}
    @Test void noAllowanceMeansDisabledWithoutImplicitPointOrItemRulesDefaults()throws Exception {
        try(var config=parse(base())){assertEquals(0,config.shopAllowance());assertFalse(config.worlds().itemsEnabled());}
        rejectedBeforeSecrets(base()+"shop.allowance=20000\n");rejectedBeforeSecrets(base()+world());
    }
    @Test void explicitPositiveI64AllowanceRequiresEveryWorldItemBindingAndCatalogBeforeSecrets()throws Exception {
        for(String amount:List.of("1","20000",Long.toString(Long.MAX_VALUE)))try(var config=parse(base()+world()+"shop.allowance="+amount+"\n")){
            assertEquals(Long.parseLong(amount),config.shopAllowance());assertTrue(config.worlds().itemsEnabled());
            assertEquals("readonly_source",config.sources().get(0).database().database());assertEquals("readonly_catalog",config.sources().get(0).catalogDatabase());
        }
        rejectedBeforeSecrets((base()+world()+"shop.allowance=20000\n").replace("source.7.catalog.database=readonly_catalog\n",""));
        rejectedBeforeSecrets((base()+world()+"shop.allowance=20000\n").replace("world.1.514.itemRules="+ModernWorldConfiguration.ITEM_RULESET+"\n",""));
    }
    @Test void malformedZeroNegativeOverflowAndUnknownKeysNeverConsultCredentials(){
        for(String amount:List.of("0","-1","+1","01","1.0","1 ","9223372036854775808","99999999999999999999",""))
            rejectedBeforeSecrets(base()+world()+"shop.allowance="+amount+"\n");
        rejectedBeforeSecrets(base()+"shop.points=20000\n");
        rejectedBeforeSecrets(base()+world()+"shop.allowance=20\nshop.allowance=20\n");
    }
    @Test void actualMainAdvertisesShopOnlyAfterExplicitRepositoryAndMappingWiringAndOwnsShutdown()throws Exception {
        var fixtures=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(fixtures);
        Files.createDirectories(directory.resolve("map/514"));Files.write(directory.resolve("map/514/7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        String text=(base()+world()+"shop.allowance=20000\n").replace(portable(directory.resolve("server.p12")),portable(fixtures.resolve("server.p12")));
        try(var config=ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),name->name.equals("T")?"modern-lito-fixture-only":"synthetic-only")) {
            ModernServer server=ModernMain.start(config);
            try(var socket=(SSLSocket)ModernTlsTestMaterial.clientContext(fixtures).getSocketFactory().createSocket()) {
                var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(3000);socket.connect(server.address(),1000);socket.startHandshake();
                socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,8,8,1023,7,1)));socket.getOutputStream().flush();
                var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(512,ack.capabilities()&512);
                // No AUTH: neither source database nor catalog is contacted by startup/HELLO.
            } finally {server.close();assertTrue(server.awaitTermination(5000));}
            try(var store=new ModernEquipmentOverrideStore(directory)){assertNotNull(store);}
        }
    }
}
