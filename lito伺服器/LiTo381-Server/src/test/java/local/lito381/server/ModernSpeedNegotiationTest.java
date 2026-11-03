package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
class ModernSpeedNegotiationTest {
    @Test void unsignedHighBitNeedsExplicitVersionAndDependenciesForEncodeAndDecode()throws Exception {
        int caps=ModernSessionProtocol.KNOWN_CAPABILITIES;
        assertEquals(Integer.MIN_VALUE,ModernSessionProtocol.CAP_SLOW_DATA);assertEquals(-1,caps);
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_SLOW_DATA);
        assertEquals(0,ModernSessionBoundary.Policy.current().supportedCapabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
        var h=new ModernSessionProtocol.Hello(0xfedcba98,1,46,caps,7,1);
        var hb=ModernSessionProtocol.encode(h);assertEquals(h,ModernSessionProtocol.decode(hb));
        assertEquals(caps,ByteBuffer.wrap(hb).getInt(20));
        var a=new ModernSessionProtocol.Ack(h.requestId(),46,1,caps);
        var ab=ModernSessionProtocol.encode(a);assertEquals(a,ModernSessionProtocol.decode(ab));
        for(int version=1;version<46;version++) {
            int v=version;
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,v,caps,7,1)));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,v,1,caps)));
            var bad=hb.clone();ByteBuffer.wrap(bad).putShort(18,(short)v);
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.decode(bad));
            var badAck=ab.clone();ByteBuffer.wrap(badAck).putShort(16,(short)v);
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.decode(badAck));
        }
        for(int bit:new int[]{ModernSessionProtocol.CAP_TIMED,ModernSessionProtocol.CAP_BATTLE_DATA,ModernSessionProtocol.CAP_ACTION_LIFE,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps&~bit,7));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps,caps&~bit));
        }
    }
    @Test void optionalDowngradeRemovesSpeedButRequiredSpeedClosesTheConnection()throws Exception {
        int caps=ModernSessionProtocol.KNOWN_CAPABILITIES;
        var hello=new ModernSessionProtocol.Hello(1,1,46,caps,7,1);
        var current=new ModernSessionBoundary(new ModernSessionBoundary.Policy(caps&~ModernSessionProtocol.CAP_SLOW_DATA,7,46).withSlowData());
        current.acceptHello(ModernSessionProtocol.encode(hello));
        assertTrue(ModernBattleSpeedWire.negotiated(current.negotiated().orElseThrow()));
        for(int v:new int[]{44,45}) {
            var old=new ModernSessionBoundary(new ModernSessionBoundary.Policy(caps,7,v));
            old.acceptHello(ModernSessionProtocol.encode(hello));
            assertEquals(v,old.negotiated().orElseThrow().version());
            assertEquals(0,old.negotiated().orElseThrow().capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
            assertFalse(ModernBattleSpeedWire.negotiated(old.negotiated().orElseThrow()));
            var required=new ModernSessionBoundary(new ModernSessionBoundary.Policy(caps,7,v));
            var request=new ModernSessionProtocol.Hello(1,1,46,caps,caps,1);
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->required.acceptHello(ModernSessionProtocol.encode(request)));
            assertEquals(ModernSessionBoundary.State.CLOSED,required.state());
            assertTrue(required.negotiated().isEmpty());
        }
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(caps,caps,45));
    }
}
