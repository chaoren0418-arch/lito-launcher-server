package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWeightConfigTest {
    @TempDir Path directory;
    ModernShopConfigTest source(){var s=new ModernShopConfigTest();s.directory=directory;return s;}
    String weighted(String rate){var s=source();return s.base()+s.world()+"shop.allowance=20000\nworld.1.514.weightRules="
            +ModernWorldConfiguration.WEIGHT_RULESET+"\nsource.7.weight.rate="+rate+"\n";}
    @Test void explicitDecimalAndScientificPositiveFiniteRateWithoutDefaults()throws Exception {
        for(String rate:List.of("1","0.01","1e2","1.23456789E-2","1e-300"))try(var c=source().parse(weighted(rate))){
            assertEquals(Double.parseDouble(rate),c.sources().get(0).weightRate());assertTrue(c.worlds().weightEnabled());
            assertEquals(List.of(new ModernWorldBinding.Key(1,514)),c.worlds().weightBindings());
        }
        try(var c=source().parse(source().base()+source().world()+"shop.allowance=20000\n")){
            assertNull(c.sources().get(0).weightRate());assertFalse(c.worlds().weightEnabled());
        }
    }
    @Test void missingUnknownAndNonfiniteRatesFailBeforeSecrets() {
        for(String rate:List.of("0","-1","+1","NaN","Infinity","0x1.0p0","1e309","1e-999","1 ","","01","1_0"))
            source().rejectedBeforeSecrets(weighted(rate));
        source().rejectedBeforeSecrets(weighted("1").replace("source.7.weight.rate=1\n",""));
        source().rejectedBeforeSecrets(weighted("1").replace("world.1.514.weightRules="+ModernWorldConfiguration.WEIGHT_RULESET+"\n",""));
        source().rejectedBeforeSecrets(weighted("1").replace(ModernWorldConfiguration.WEIGHT_RULESET,"UNKNOWN"));
        source().rejectedBeforeSecrets(weighted("1").replace("world.1.514.itemRules="+ModernWorldConfiguration.ITEM_RULESET+"\n",""));
    }
    @Test void weightCapabilityRequiresVersionNineAndAllDependencies()throws Exception {
        for(int cap:List.of(1024,1025,1535))assertThrows(ModernSessionProtocol.ProtocolException.class,
                ()->ModernSessionProtocol.validateCapabilities(cap,0));
        var policy=ModernSessionBoundary.Policy.world(true,true,true,true);
        for(int version=1;version<9;version++){
            var boundary=new ModernSessionBoundary(policy);var response=boundary.acceptHello(ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Hello(1,version,version,2047,7,1)));
            assertEquals(0,((ModernSessionProtocol.Ack)ModernSessionProtocol.decode(response)).capabilities()&1024);
        }
        var boundary=new ModernSessionBoundary(policy);
        assertEquals(2047,((ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(
                new ModernSessionProtocol.Hello(1,9,9,2047,7,1))))).capabilities());
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(2047,2047,8));
        assertEquals(0,ModernSessionBoundary.Policy.world(true,true,true).supportedCapabilities()&1024);
    }
}
