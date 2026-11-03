package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWorldPolicyConfigurationTest {
    @TempDir Path temporary;
    static String base(Path path,Properties worlds){
        return "schema=1\nbind.port=0\ntls.pkcs12="+path.resolve("unused.p12").toString().replace('\\','/')+
                "\ntls.password.env=T\nsources=7\nsource.7.name=Source\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\n"+
                "source.7.mysql.database=source_auth\nsource.7.mysql.username.env=U\nsource.7.mysql.password.env=P\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n"+
                worlds.stringPropertyNames().stream().sorted().map(k->k+"="+worlds.getProperty(k).replace(" ","\\ ")+"\n").reduce("",String::concat);
    }
    static String policy(int port,ModernWorldPolicy.Pin pin){
        return "worldPolicy.enabled=true\nworldPolicy.namespaceSha256="+pin.namespaceSha256()+"\nworldPolicy.revision="+pin.revision()+
                "\nworldPolicy.contentSha256="+pin.contentSha256()+"\nworldPolicy.mysql.host=127.0.0.1\nworldPolicy.mysql.port="+port+
                "\nworldPolicy.mysql.database=world_policy_fixture\nworldPolicy.mysql.username.env=WP_U\nworldPolicy.mysql.password.env=WP_P\nworldPolicy.mysql.tls=disabled\n";
    }
    String base()throws Exception{return base(temporary,ModernWorldPolicyTest.properties(temporary,ModernWorldClockTest.mapping().binding().map(),List.of(ModernWorldPolicyTest.KEY)));}
    String policy(){return policy(3307,ModernWorldPolicyJdbcTest.snapshot().pin());}
    void rejected(String text){var reads=new AtomicInteger();var failure=assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),key->{reads.incrementAndGet();return "private-secret";}));assertEquals(0,reads.get());assertNull(failure.getCause());}
    @Test void optionalPolicyIsIndependentOfSettlementAndOwnsCredentialsUntilClosed()throws Exception{
        try(var absent=ModernServerConfig.parse(base().getBytes(StandardCharsets.UTF_8),key->"private-secret")){assertNull(absent.worldPolicy());assertNull(absent.settlement());}
        var config=ModernServerConfig.parse((base()+policy()).getBytes(StandardCharsets.UTF_8),key->key.startsWith("WP_")?"only-policy-secret":"source-secret");
        assertNull(config.settlement());assertEquals(30000,config.worldPolicy().deadlineMillis());assertEquals(7,config.worldPolicy().pin().revision());
        assertNotSame(config.sources().get(0).database(),config.worldPolicy().database());assertEquals("only-policy-secret",config.worldPolicy().database().username());
        assertEquals("ModernWorldPolicyConfig[redacted]",config.worldPolicy().toString());var db=config.worldPolicy().database();config.close();config.close();assertThrows(IllegalStateException.class,db::password);
    }
    @Test void disabledMissingFieldsUnpinnedAndInvalidPinsFailBeforeEverySecret()throws Exception{
        rejected(base()+"worldPolicy.mysql.database=world_policy_fixture\n");rejected(base()+policy().replace("enabled=true","enabled=false"));
        for(String field:List.of("namespaceSha256","revision","contentSha256","mysql.host","mysql.port","mysql.database","mysql.username.env","mysql.password.env","mysql.tls")){
            String partial=policy().lines().filter(s->!s.startsWith("worldPolicy."+field+"=")).collect(java.util.stream.Collectors.joining("\n"))+"\n";rejected(base()+partial);
        }
        for(String bad:List.of("0","-1","+1","01","9223372036854775808"))rejected(base()+policy().replace("revision=7","revision="+bad));
        for(String bad:List.of("0".repeat(64),"A".repeat(64),"123"))rejected(base()+policy().replace(ModernWorldPolicyTest.NAMESPACE,bad));
        rejected(base().lines().filter(s->!s.contains("sectorPins=")&&!s.contains("manifestSha256=")).collect(java.util.stream.Collectors.joining("\n"))+"\n"+policy());
        for(String value:List.of("99","30001","1e3"))rejected(base()+policy()+"worldPolicy.deadlineMillis="+value+"\n");
    }
    @Test void policyCannotAliasAnySourceOrCatalogAndCannotDowngradeRemoteTls()throws Exception{
        rejected(base()+policy().replace("database=world_policy_fixture","database=SOURCE_AUTH"));
        rejected(base()+"source.7.catalog.database=catalog\n"+policy().replace("database=world_policy_fixture","database=CATALOG"));
        rejected(base()+policy().replace("worldPolicy.mysql.host=127.0.0.1","worldPolicy.mysql.host=192.0.2.7"));
        for(String mode:List.of("preferred","verify_ca","required"))rejected(base()+policy().replace("worldPolicy.mysql.tls=disabled","worldPolicy.mysql.tls="+mode));
        rejected(base()+policy()+"worldPolicy.mysql.trustStore.pkcs12=relative\n");
        rejected(base()+policy()+"worldPolicy.reloadMillis=1000\n");
    }
    @Test void layoutPinsAndSqlConnectionNeverChangePermanentSettlementOutboxRouting()throws Exception{
        String settlement="settlement.enabled=true\nsettlement.outboxDirectory="+temporary.toString().replace('\\','/')+
                "\nsettlement.mysql.host=127.0.0.1\nsettlement.mysql.port=3308\nsettlement.mysql.database=results\nsettlement.mysql.username.env=S_U\nsettlement.mysql.password.env=S_P\nsettlement.mysql.tls=disabled\n";
        try(var local=ModernServerConfig.parse((base()+settlement).getBytes(StandardCharsets.UTF_8),k->"secret");
            var first=ModernServerConfig.parse((base()+settlement+policy()).getBytes(StandardCharsets.UTF_8),k->"secret");
            var next=ModernServerConfig.parse((base()+settlement+policy().replace("revision=7","revision=8").replace("worldPolicy.mysql.port=3307","worldPolicy.mysql.port=3310")).getBytes(StandardCharsets.UTF_8),k->"secret")){
            assertArrayEquals(ModernSettlementCoordinator.binding(local),ModernSettlementCoordinator.binding(first));
            assertArrayEquals(ModernSettlementCoordinator.binding(first),ModernSettlementCoordinator.binding(next));
        }
    }
    @Test void probeArgumentsAndErrorsAreBoundedAnonymousAndDoNotReadSecrets(){
        assertFalse(ModernWorldPolicyReadProbe.candidate(new String[]{temporary.resolve("candidate.properties").toString()}));
        assertTrue(ModernWorldPolicyReadProbe.candidate(new String[]{temporary.resolve("candidate.properties").toString(),"--candidate"}));
        for(String[] args:List.of(new String[]{},new String[]{"relative"},new String[]{temporary.toString(),"--publish"},new String[]{temporary.toString(),"--candidate","extra"})){
            var output=new java.io.ByteArrayOutputStream();var errors=new java.io.ByteArrayOutputStream();var reads=new AtomicInteger();
            assertEquals(2,ModernWorldPolicyReadProbe.run(args,key->{reads.incrementAndGet();return "DO-NOT-PRINT";},new java.io.PrintStream(output),new java.io.PrintStream(errors)));
            assertEquals(0,reads.get());assertEquals(0,output.size());assertEquals("MODERN_WORLD_POLICY_READ_FAILED",errors.toString(StandardCharsets.UTF_8).strip());
        }
    }
}
