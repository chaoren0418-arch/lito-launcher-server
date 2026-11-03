package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernHonorTest {
    static ModernHonor.Objective tower(){return ModernHonor.Objective.initial(2,new ModernHonor.Tower(0,ModernBattleEngineHonorTest.cell(10,9),2665,3000));}
    @Test void nativeActionThresholdsAndSignedWideHpNeverUseNarrowedMultiplication(){
        assertEquals(0,ModernHonor.action(2250,3000));assertEquals(32,ModernHonor.action(2249,3000));assertEquals(32,ModernHonor.action(1500,3000));
        assertEquals(33,ModernHonor.action(1499,3000));assertEquals(33,ModernHonor.action(750,3000));assertEquals(34,ModernHonor.action(749,3000));assertEquals(35,ModernHonor.action(0,3000));
        assertEquals(0,ModernHonor.action(Long.MAX_VALUE,Long.MAX_VALUE));assertEquals(33,ModernHonor.action(Long.MAX_VALUE/3,Long.MAX_VALUE));
    }
    @Test void fullHpHealingIsAnInteractionAndDoesNotInventDamageOrHealGain(){
        var old=tower();var next=old.healed(0,1,1);assertEquals(old.hp(),next.hp());assertEquals(1,next.revision());assertEquals(3,next.lastKind());assertEquals(1,next.lastActorEntityId());
        assertEquals(0,next.healGain());assertEquals(0,next.hurtRevision());assertEquals(0,next.damageLoss());
        assertThrows(IllegalArgumentException.class,()->next.healed(0,1,1));assertThrows(IllegalArgumentException.class,()->next.healed(1,1,2));
    }
    @Test void damageHealingAndDeathHaveExactIndependentCumulativeConservation(){
        var a=tower().damaged(2000,2,2,1);var b=a.healed(50,1,2);var c=b.damaged(1050,2,1,3);
        assertEquals(1050,b.hp());assertEquals(2000,b.damageLoss());assertEquals(50,b.healGain());assertEquals(1,b.hurtRevision());
        assertEquals(0,c.hp());assertEquals(3050,c.damageLoss());assertEquals(3,c.deathRevision());assertThrows(IllegalArgumentException.class,()->c.healed(0,1,4));
    }
    @Test void deathRespawnKeepsHistoryAndTerminalOnlyDropsThePendingTimer(){
        var a=ModernHonor.Life.initial(1).died(10,123,ModernBattleEngineHonorTest.cell(10,10));assertEquals(5123,a.pendingDueMillis());
        var b=a.respawn(11,100);assertEquals(1,b.deathCount());assertEquals(1,b.respawnCount());assertEquals(10,b.lastDeathRevision());assertEquals(100,b.respawnHpGain());
        var c=b.died(12,10000,ModernBattleEngineHonorTest.cell(11,11)).terminal();assertEquals(2,c.deathCount());assertEquals(1,c.respawnCount());assertEquals(0,c.pendingDueMillis());assertEquals(100,c.respawnHpGain());
        assertThrows(IllegalArgumentException.class,()->b.respawn(12,100));assertThrows(IllegalArgumentException.class,()->a.died(12,124,ModernBattleEngineHonorTest.cell(10,10)));
    }
    @Test void objectiveHandlesAreDisjointAndFullRosterIsOrderedWithoutSourceIdentities(){
        var a=tower();var b=ModernHonor.Objective.initial(2,new ModernHonor.Tower(1,ModernBattleEngineHonorTest.cell(11,9),2669,3000));
        var state=new ModernHonor.Snapshot(List.of(a,b),List.of(ModernHonor.Life.initial(1),ModernHonor.Life.initial(2)),List.of(ModernHonor.Heal.initial(1,4),ModernHonor.Heal.initial(2,4)));
        assertNull(state.objective(1));assertNull(state.objective(Long.MAX_VALUE));assertEquals(a,state.objective(3));
        assertThrows(IllegalArgumentException.class,()->new ModernHonor.Snapshot(List.of(a,a),state.lives(),state.heals()));
        assertThrows(IllegalArgumentException.class,()->new ModernHonor.Snapshot(state.objectives(),List.of(state.lives().get(1),state.lives().get(0)),state.heals()));
    }
    @Test void appendixExactWidthsReservedBytesAndTruncationAreStrict()throws Exception{
        var state=new ModernHonor.Snapshot(List.of(tower(),ModernHonor.Objective.initial(2,new ModernHonor.Tower(1,ModernBattleEngineHonorTest.cell(11,9),2669,3000))),
                List.of(ModernHonor.Life.initial(1),ModernHonor.Life.initial(2)),List.of(ModernHonor.Heal.initial(1,4),ModernHonor.Heal.initial(2,4)));
        var out=ByteBuffer.allocate(8+2*96+2*64+2*88);ModernHonorProtocol.put(out,state);assertEquals(out.capacity(),out.position());assertEquals(state,ModernHonorProtocol.get(ByteBuffer.wrap(out.array()),2));
        for(int offset:new int[]{6,7,8+10,8+11,8+95}){var bytes=out.array().clone();bytes[offset]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernHonorProtocol.get(ByteBuffer.wrap(bytes),2));}
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernHonorProtocol.get(ByteBuffer.wrap(java.util.Arrays.copyOf(out.array(),out.capacity()-1)),2));
        var disabled=ByteBuffer.allocate(8);ModernHonorProtocol.put(disabled,ModernHonor.Snapshot.disabled());assertEquals(ModernHonor.Snapshot.disabled(),ModernHonorProtocol.get(ByteBuffer.wrap(disabled.array()),2));
    }
    @Test void historicalHealCannotClaimRestorationAgainstAnUnchangedObjective()throws Exception{
        try(var f=new ModernBattleEngineHonorTest.Fixture()){f.active();var after=ModernBattleEngineHonorTest.frame(f.cast(0,1,1,3).snapshot());var w=after.world();
            var later=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts());
            var towers=new ArrayList<>(after.honor().objectives());towers.set(0,ModernHonor.Objective.initial(2,f.honor.towers().get(0)));
            var fake=new ModernHonor.Snapshot(towers,after.honor().lives(),after.honor().heals());
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(later,after.vitals(),after.damages(),after.items(),after.weights(),after.buffs(),after.ammo(),after.regen(),fake));}
    }
    @Test void honorCapabilityRequiresV13AndOlderWorldPolicyDoesNotAdvertiseIt()throws Exception{
        int base=(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT))&~(ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_NPC_DISINTEGRATE|ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_PET_RACE|ModernSessionProtocol.CAP_FIRE_STORM|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR);
        assertEquals(32767,base);assertEquals(0,ModernSessionBoundary.Policy.world(true,true,true,true,true,true,true).supportedCapabilities()&ModernSessionProtocol.CAP_HONOR);
        assertThrows(IllegalArgumentException.class,()->ModernSessionBoundary.Policy.world(true,true,true,true,true,true,false,true));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,12,1,base)));
        var bytes=ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,13,1,base));assertEquals(base,((ModernSessionProtocol.Ack)ModernSessionProtocol.decode(bytes)).capabilities());
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(base&~ModernSessionProtocol.CAP_REGEN,0));
    }
}
