package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernFireStormProtocolTest {
    static ModernCombatProtocol.Frame with(ModernCombatProtocol.Frame f,List<ModernFireStormProtocol.State> states){
        return new ModernCombatProtocol.Frame(f.world(),f.vitals(),f.damages(),f.items(),f.weights(),f.buffs(),f.ammo(),f.regen(),f.honor(),states);
    }
    @Test void enabledInitialPrefixPaddingRowsAndTruncationAreStrict()throws Exception{
        try(var f=ModernBattleEngineFireStormTest.fixture(true,0,1000,1000)){var frame=ModernBattleEngineFireStormTest.frame(f.active());
            var req=new ModernCombatProtocol.Request(f.players.get(0).scope(),ModernCombatProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0);
            var m=new ModernApplicationProtocol.CombatResult(1,req,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
            var bytes=ModernApplicationProtocol.encode(m,14);int offset=bytes.length-8-2*64;
            for(int n:new int[]{offset+2,offset+7,offset+8+58,offset+8+63}){var b=bytes.clone();b[n]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(b,14));}
            var truncated=java.util.Arrays.copyOf(bytes,bytes.length-1);ByteBuffer.wrap(truncated).putInt(4,truncated.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,14));
            assertThrows(IllegalArgumentException.class,()->with(frame,List.of(frame.fireStorms().get(0))));
            assertEquals(m,ModernApplicationProtocol.decode(bytes,14));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),14));
        }
    }
    @Test void historicalHpCannotBorrowAnUnrelatedDamageLedgerOrInvisibleObjectiveInteraction()throws Exception{
        try(var f=ModernBattleEngineFireStormTest.fixture(true,0,1000,1000)){f.active();f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,70,0).reason());
            var frame=ModernBattleEngineFireStormTest.frame(f.engine.tick());var s=frame.fireStorms().get(0);var t=s.targets().get(0);
            var targets=new ArrayList<>(s.targets());targets.set(0,new ModernFireStormProtocol.Target(t.entityId(),t.hpBefore()-1,t.hpAfter()-1,t.appliedDamage(),t.damageLossAfter(),t.respawnCount(),t.x(),t.y()));
            var rows=new ArrayList<>(frame.fireStorms());rows.set(0,new ModernFireStormProtocol.State(s.entityId(),s.castCount(),s.castRevision(),s.mpSpent(),s.lastCastSequence(),s.fromX(),s.fromY(),s.mpCost(),s.reuseMillis(),targets));
            assertThrows(IllegalArgumentException.class,()->with(frame,rows));
        }
    }
    @Test void fireSkillLegacyProjectionRetainsActualNameCostReuseAndCannotSmuggleNewOrdinal()throws Exception{
        for(var support:List.of(ModernSkills.Support.FIRE_STORM,ModernSkills.Support.UNSUPPORTED_TEMPLATE)){
            var row=new ModernSkills.Skill(70,"$1975",48,2000,support);var m=new ModernApplicationProtocol.SkillsResult(1,new ModernSpellProtocol.Context(7,3333,1),new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(row)));
            var modern=ModernApplicationProtocol.encode(m,14);assertEquals(m,ModernApplicationProtocol.decode(modern,14));
            for(int version=4;version<=13;version++){int v=version;var b=ModernApplicationProtocol.encode(m,v);var decoded=(ModernApplicationProtocol.SkillsResult)ModernApplicationProtocol.decode(b,v);
                assertEquals(new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.UNIMPLEMENTED),decoded.reply().skills().get(0));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(modern,v));}
        }
    }
    @Test void v14FireCapabilityHasExplicitDependenciesAndOldPoliciesDoNotAdvertiseIt()throws Exception{
        assertEquals(2097151,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT));
        assertEquals(0,ModernSessionBoundary.Policy.current().supportedCapabilities()&ModernSessionProtocol.CAP_FIRE_STORM);
        assertEquals(0,ModernSessionBoundary.Policy.world(true,true,true,true,true,true,true,true).supportedCapabilities()&ModernSessionProtocol.CAP_FIRE_STORM);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(ModernSessionProtocol.CAP_FIRE_STORM,0));
        var p=ModernSessionBoundary.Policy.world(true,true,true,true,true,true,true,false,true);assertNotEquals(0,p.supportedCapabilities()&ModernSessionProtocol.CAP_FIRE_STORM);
        assertEquals(0,p.supportedCapabilities()&ModernSessionProtocol.CAP_HONOR);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,13,ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1,p.supportedCapabilities())));
    }
}
