package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernTimedIntegrationConfigTest {
    @TempDir Path directory;
    ModernWeightConfigTest base(){var value=new ModernWeightConfigTest();value.directory=directory;return value;}
    String timed(){return base().weighted("100")+"world.1.514.buffRules="+ModernWorldConfiguration.TIMED_RULESET+"\n";}
    @Test void explicitPolicyEnablesOnlyQualifiedWeightedItemWorldsWithoutChangingLegacyDefaults()throws Exception {
        try(var config=base().source().parse(timed())){
            assertTrue(config.worlds().timedEnabled());assertTrue(config.worlds().weightEnabled());
            assertEquals(List.of(new ModernWorldBinding.Key(1,514)),config.worlds().timedBindings());
            assertEquals(100.0,config.sources().get(0).weightRate().doubleValue());
        }
        try(var config=base().source().parse(base().weighted("1"))){assertFalse(config.worlds().timedEnabled());assertTrue(config.worlds().timedBindings().isEmpty());}
    }
    @Test void missingDependenciesUnknownRulesAndMixedBindingsFailBeforeCredentials() {
        var source=base().source();
        source.rejectedBeforeSecrets(timed().replace(ModernWorldConfiguration.TIMED_RULESET,"UNKNOWN"));
        source.rejectedBeforeSecrets(timed().replace("world.1.514.weightRules="+ModernWorldConfiguration.WEIGHT_RULESET+"\n",""));
        source.rejectedBeforeSecrets(timed().replace("world.1.514.itemRules="+ModernWorldConfiguration.ITEM_RULESET+"\n",""));
        source.rejectedBeforeSecrets(timed().replace("source.7.weight.rate=100\n",""));
        String second=base().source().world().lines().filter(s->s.startsWith("world.1.514.")).map(s->s.replace("world.1.514.","world.1.515.")).reduce("",(a,b)->a+b+"\n");
        source.rejectedBeforeSecrets(timed().replace("world.bindings=1:514","world.bindings=1:514,1:515")+second);
    }
    @Test void versionTenCapabilityDependsOnTheEntireChainAndOldPoliciesNeverAdvertiseIt()throws Exception {
        for(int cap:List.of(2048,2049,3071,3583,3967))assertThrows(ModernSessionProtocol.ProtocolException.class,
                ()->ModernSessionProtocol.validateCapabilities(cap,0));
        var policy=ModernSessionBoundary.Policy.world(true,true,true,true,true);
        for(int version=1;version<10;version++){
            var b=new ModernSessionBoundary(policy);var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(b.acceptHello(
                    ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,4095,7,1))));
            assertEquals(0,ack.capabilities()&2048);
        }
        var b=new ModernSessionBoundary(policy);var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(b.acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,10,10,4095,7,1))));
        assertEquals(4095,ack.capabilities());
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(4095,4095,9));
        for(var old:List.of(ModernSessionBoundary.Policy.current(),ModernSessionBoundary.Policy.equipment(),ModernSessionBoundary.Policy.rooms(true),
                ModernSessionBoundary.Policy.world(true,true,true,true)))assertEquals(0,old.supportedCapabilities()&2048);
    }
    @Test void longActiveDeadlineIsExplicitAndHasAThirtyMinuteCeiling() {
        assertEquals(1_800_000,new ModernBattleEngine.Limits(2000,0,1_800_000).activeTimeoutMillis());
        assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.Limits(2000,0,1_800_001));
        assertEquals(60000,ModernBattleEngineBotTest.LIMITS.activeTimeoutMillis());
    }
    @Test void mappingCannotEnableTimedWithoutWeightOrWithSubMillisecondSpeedRules()throws Exception {
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService.Mapping(f.binding,ModernBattleEngineBotTest.cell(10,10),200000000,700000000,
                    1200000000,true,false,true,false,true));
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService.Mapping(f.binding,ModernBattleEngineBotTest.cell(10,10),200000001,700000000,
                    1200000000,true,false,true,true,true));
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService.Mapping(f.binding,ModernBattleEngineBotTest.cell(10,10),200000000,700000001,
                    1200000000,true,false,true,true,true));
            assertFalse(new ModernWorldService.Mapping(f.binding,ModernBattleEngineBotTest.cell(10,10),200000000,700000000,1200000000,true,false,true,true).timedEnabled());
        }
    }
}
