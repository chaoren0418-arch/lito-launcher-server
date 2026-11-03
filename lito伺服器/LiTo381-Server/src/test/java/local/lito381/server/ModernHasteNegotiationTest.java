package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ModernHasteNegotiationTest {
    @Test void explicitVersionCapabilityAndAllDependenciesAreRequired()throws Exception {
        int caps=ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_SLOW_DATA;
        var hello=new ModernSessionProtocol.Hello(1,1,44,caps,7,1);
        var wire=ModernSessionProtocol.encode(hello);
        assertEquals(hello,ModernSessionProtocol.decode(wire));
        for(int v=1;v<44;v++) {
            int version=v;
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,version,caps,7,1)));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,version,1,caps)));
        }
        for(int bit:new int[]{ModernSessionProtocol.CAP_TIMED,ModernSessionProtocol.CAP_BATTLE_DATA,
                ModernSessionProtocol.CAP_ACTION_LIFE,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps&~bit,7));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps,caps&~bit));
        }
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_HASTE);
        var current=new ModernSessionBoundary(new ModernSessionBoundary.Policy(caps,7,44));
        current.acceptHello(wire);assertTrue(ModernBattleHasteWire.negotiated(current.negotiated().orElseThrow()));
        var old=new ModernSessionBoundary(new ModernSessionBoundary.Policy(caps,7,43));
        old.acceptHello(wire);assertEquals(43,old.negotiated().orElseThrow().version());
        assertEquals(caps&~ModernSessionProtocol.CAP_HASTE,old.negotiated().orElseThrow().capabilities());
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(caps,caps,43));
    }
}
