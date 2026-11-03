package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernGmPolicyTest {
    private static ModernSessionProtocol.Ack negotiate(ModernSessionBoundary.Policy policy,int max,int offered,int required) throws Exception {
        var boundary=new ModernSessionBoundary(policy);
        return (ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(
                new ModernSessionProtocol.Hello(1,1,max,offered,required,1))));
    }
    @Test void readOnlyAndEquipmentOnlyPoliciesDoNotAdvertiseOrRequireRoomsOrGm() throws Exception {
        assertEquals(15,ModernSessionBoundary.Policy.current().supportedCapabilities());
        assertEquals(31,ModernSessionBoundary.Policy.equipment().supportedCapabilities());
        for(var policy:List.of(ModernSessionBoundary.Policy.current(),ModernSessionBoundary.Policy.equipment())) {
            var ack=negotiate(policy,7,511,7);assertEquals(policy.supportedCapabilities(),ack.capabilities());
            assertEquals(0,ack.capabilities() & ModernSessionProtocol.CAP_GM_BOTS);
        }
    }
    @Test void allOldVersionsMaskOnlyNewOptionalBitWhileVersionSevenCanSelectIt() throws Exception {
        for(var policy:List.of(ModernSessionBoundary.Policy.rooms(false),ModernSessionBoundary.Policy.rooms(true),
                ModernSessionBoundary.Policy.world(true,false),ModernSessionBoundary.Policy.world(true,true))) {
            for(int version=1;version<=6;version++) {
                var ceiling=negotiate(policy.withMaxVersion(version),7,511,7);
                var peer=negotiate(policy,version,511,7);
                assertEquals(version,ceiling.version());assertEquals(ceiling,peer);
                assertEquals(policy.supportedCapabilities() & 511 & ~256,ceiling.capabilities());
                assertEquals(0,ceiling.capabilities() & ModernSessionProtocol.CAP_MATCH_DESCRIPTOR);
            }
            assertEquals(policy.supportedCapabilities() & 511,negotiate(policy,7,511,7).capabilities());
            assertEquals(policy.supportedCapabilities() & 255,negotiate(policy,7,255,7).capabilities());
        }
    }
    @Test void requiredGmCannotBeSilentlyDroppedWhenVersionOrImplementationIsUnavailable() throws Exception {
        int required=1|2|4|32|256;
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(511,required,6));
        for(var policy:List.of(ModernSessionBoundary.Policy.world(true,true).withMaxVersion(6),ModernSessionBoundary.Policy.current())) {
            var boundary=new ModernSessionBoundary(policy);
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->boundary.acceptHello(ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Hello(1,1,7,511,required,1))));
            assertEquals(ModernSessionBoundary.State.CLOSED,boundary.state());assertTrue(boundary.negotiated().isEmpty());
        }
        assertTrue((negotiate(ModernSessionBoundary.Policy.rooms(false),7,303,required).capabilities()&256)!=0);
    }
    @Test void capabilityBitWithoutRoomNavigationAndOldAckAreRejectedByWire() {
        for(int capabilities:new int[]{256,257,263,288}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(capabilities,0));
        }
        for(int version=1;version<=6;version++) {
            int old=version;
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,old,1,303)));
        }
    }
}
