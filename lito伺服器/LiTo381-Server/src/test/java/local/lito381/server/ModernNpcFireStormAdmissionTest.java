package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernNpcFireStormAdmissionTest {
    static final int BIT=ModernSessionProtocol.CAP_NPC_FIRE_STORM;
    static final int CAPS=ModernSessionProtocol.RUNTIME_CAPABILITIES|ModernSessionProtocol.CAP_NPC|BIT;
    @Test void versionThirtyOneIsExplicitAndCannotLeakIntoRuntimeOrOldAcks()throws Exception {
        assertEquals(2097152,BIT);assertEquals(4194303,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT));
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&BIT);
        var hello=new ModernSessionProtocol.Hello(1,1,31,CAPS,3,1);
        for(int version=17;version<=31;version++) {
            var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(CAPS,3,version));
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(hello)));
            assertEquals(version,ack.version());assertEquals(version==31,ModernNpcFireStormProtocol.negotiated(boundary.negotiated().orElseThrow()));
            assertEquals(version==31?BIT:0,ack.capabilities()&BIT);
            if(version<31){int v=version;
                assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,v,CAPS,3,1)));
                assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,v,1,CAPS)));
            }
        }
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(CAPS,CAPS,30));
        var b=new ModernSessionBoundary(new ModernSessionBoundary.Policy(CAPS,3,30));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->b.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,31,CAPS,CAPS,1))));
        assertEquals(ModernSessionBoundary.State.CLOSED,b.state());
    }
    @Test void configuredNpc70CannotStartUnderVersionThirtyOne()throws Exception {
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        var backend=new ModernApplicationTestBackend("auth",80);
        try(var f=new ModernNpcFireStormServiceTest.Fixture(true,0,false)){
            var error=assertThrows(IllegalArgumentException.class,()->new ModernServer(new java.net.InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(tls,false),
                    new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),backend.verifier,backend.characters,f.presence,
                    ModernCharacterDataRepository.unavailable(),null,null,f.rooms,f.service,30,ModernSkillsRepository.unavailable()));
            assertEquals("NPC70 requires explicit source authority and v31",error.getMessage());
            assertEquals(0,f.service.registrationCount());assertEquals(0,f.presence.ownerCount());
        }
    }
    @Test void bothSupportedAndRequiredDeclarationsMustCarryCompleteDependencies()throws Exception {
        for(int dependency:List.of(ModernSessionProtocol.CAP_NPC,ModernSessionProtocol.CAP_REGEN,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR,ModernSessionProtocol.CAP_COMBAT)) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(CAPS&~dependency,3));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(CAPS,CAPS&~dependency));
        }
        assertDoesNotThrow(()->ModernSessionProtocol.validateCapabilities(CAPS,CAPS));
        assertEquals(0,CAPS&ModernSessionProtocol.CAP_NPC_DISINTEGRATE);
    }
    @Test void npcAndPvpSkillPermissionsAreIndependentAndNeverBypassEachOther()throws Exception {
        var skills=new ModernApplicationProtocol.SkillsResult(91,new ModernSpellProtocol.Context(7,3333,1),new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(
                new ModernSkills.Skill(70,"$1478",48,2000,ModernSkills.Support.FIRE_STORM),new ModernSkills.Skill(77,"$1485",70,5000,ModernSkills.Support.DISINTEGRATE))));
        int base=ModernSessionProtocol.RUNTIME_CAPABILITIES&~ModernSessionProtocol.CAP_FIRE_STORM;
        for(int magic:List.of(0,BIT,ModernSessionProtocol.CAP_FIRE_STORM,ModernSessionProtocol.CAP_NPC_DISINTEGRATE,ModernSessionProtocol.CAP_DISINTEGRATE,BIT|ModernSessionProtocol.CAP_NPC_DISINTEGRATE)) {
            int caps=base|ModernSessionProtocol.CAP_NPC|magic;
            var result=(ModernApplicationProtocol.SkillsResult)ModernMatchDescriptor.forPeer(skills,new ModernSessionBoundary.Negotiated(1,31,1,caps));
            assertEquals((magic&(BIT|ModernSessionProtocol.CAP_FIRE_STORM))!=0?ModernSkills.Support.FIRE_STORM:ModernSkills.Support.UNIMPLEMENTED,result.reply().skills().get(0).support());
            assertEquals((magic&(ModernSessionProtocol.CAP_NPC_DISINTEGRATE|ModernSessionProtocol.CAP_DISINTEGRATE))!=0?ModernSkills.Support.DISINTEGRATE:ModernSkills.Support.UNIMPLEMENTED,result.reply().skills().get(1).support());
            assertEquals(48,result.reply().skills().get(0).mpCost());assertEquals(2000,result.reply().skills().get(0).reuseMillis());
            ModernNpcFireStormProtocolTest.roundtrip(result);
        }
    }
}
