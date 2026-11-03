package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Candidate atomic frame, before application/TLS/UI admission. */
class ModernNpcFireStormFrameTest {
    static ModernMatchDescriptor descriptor(ModernBattleEngine.Snapshot w) {
        int features=NPC|COMBAT|SPELLS|ITEMS|WEIGHT|REGEN|NPC_FIRE_STORM;
        if(!w.npcSpells().isEmpty())features|=NPC_LIGHT_ARROW;if(!w.npcDisintegrations().isEmpty())features|=NPC_DISINTEGRATE;
        if(w.isolatedForces())features|=ISOLATED_FORCES;if(w.quest().enabled())features|=QUEST_OBJECTIVES|QUEST_LIVES;
        if(w.timeAttack().enabled())features|=TIME_ATTACK;
        return new ModernMatchDescriptor(w.timeAttack().enabled()?Ruleset.COOPERATIVE_TIME_ATTACK:Ruleset.NPC_ENCOUNTER,features,2000,2000,0,60000);
    }
    static ModernNpcProtocol.Frame frame(ModernBattleEngineNpcTest.Fixture f,int who) {
        var value=f.view();return ModernQuestVisibility.npc(ModernNpcProtocol.Frame.from(value,descriptor(value.world()))
                .withResources(ModernNpcProtocol.Resources.from(f.config.catalog())).withNames(ModernNpcNames.Table.from(f.config.catalog())),f.players.get(who).scope());
    }
    static ModernNpcProtocol.Frame frame(ModernTimeAttackEngineTest.Fixture f) {
        var w=f.view();return ModernTimeAttackProtocol.frame(w,descriptor(w.world()),w.world().timeAttack().finished()?f.engine.timeAttackResult():null,
                ModernWorldProtocol.SettlementState.PENDING,ModernNpcProtocol.Resources.from(f.catalog)).withNames(ModernNpcNames.Table.from(f.catalog));
    }
    static byte[] bytes(ModernNpcProtocol.Frame f)throws Exception {
        var b=ByteBuffer.allocate(ModernNpcProtocol.maxFrameBytes(31));ModernNpcProtocol.putFrame(b,f,31);return Arrays.copyOf(b.array(),b.position());
    }
    static ModernNpcProtocol.Frame roundtrip(ModernNpcProtocol.Frame f)throws Exception {
        var b=ByteBuffer.wrap(bytes(f));var decoded=ModernNpcProtocol.getFrame(b,31);assertEquals(f,decoded);assertFalse(b.hasRemaining());return decoded;
    }
    static ModernCombatProtocol.Frame states(ModernCombatProtocol.Frame c,List<ModernNpcFireStormState.State> states) {
        return new ModernCombatProtocol.Frame(c.world(),c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),c.npcDisintegrations(),states);
    }
    static ModernNpcFireStormState.State copy(ModernNpcFireStormState.State s,long revision,long life,ModernWorldMap.Cell origin,List<ModernNpcFireStormState.Target> targets,ModernNpcFireStormStructures.Receipt structure) {
        return new ModernNpcFireStormState.State(s.entityId(),s.castCount(),revision,s.lastSequence(),s.mpSpent(),s.mpCost(),life,s.scope(),origin,s.lane(),targets,structure);
    }
    @Test void initialMultiVictimManaAndAllI64HpAreAtomic()throws Exception {
        for(long hp:List.of(1L,9007199254740993L))try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,hp,0,true,false,false,1)) {
            roundtrip(frame(f,0));f.active();f.spawn(0,11,10);f.spawn(0,10,11);f.spawn(1,21,10);
            assertEquals(ModernNpcFireStormState.Reason.NONE,ModernBattleEngineNpcFireStormTest.cast(f,1).reason());var n=roundtrip(frame(f,0));var s=n.combat().npcFireStorms().get(0);
            assertEquals(2,s.targets().size());assertEquals(hp,s.targets().get(0).hpBefore());assertEquals(952,n.combat().vital(1).mp());
            assertEquals(ModernNpcFireStormState.State.initial(3,0),n.combat().npcFireStorms().get(2));
            assertEquals(0,n.combat().vital(1).attackSequence());
        }
    }
    @Test void allQuestLogicalFlowsRoundtripBothPrivateViewsThroughMagicVictory()throws Exception {
        for(int map:BattleRules.Mode.QUEST_BATTLE.maps())try(var f=ModernNpcFireStormStructuresTest.quest(map,1)) {
            f.active();long seq=0;
            while(f.view().world().phase()==ModernBattleEngine.Phase.ACTIVE) {
                f.elapsed(seq*2000);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,++seq).reason());
                var own=roundtrip(frame(f,0));var other=roundtrip(frame(f,1));
                assertEquals(List.of(1L),own.combat().npcFireStorms().stream().map(ModernNpcFireStormState.State::entityId).toList());
                assertEquals(List.of(2L),other.combat().npcFireStorms().stream().map(ModernNpcFireStormState.State::entityId).toList());
                assertEquals(0,other.combat().npcFireStorms().get(0).castCount());assertEquals(1000,other.combat().vital(2).mp());
                assertTrue(other.combat().world().quest().targets().stream().allMatch(t->t.lane()==1));assertTrue(seq<=8);
            }
            var win=frame(f,0);var lose=frame(f,1);assertEquals(0,lose.combat().world().quest().winningLane());assertTrue(win.combat().world().quest().exchanges().isEmpty());
            var c=win.combat();var s=c.npcFireStorms().get(0);var noProof=copy(s,s.revision(),s.casterLife(),s.origin(),s.targets(),null);
            assertThrows(IllegalArgumentException.class,()->states(c,List.of(noProof)));assertTrue(lose.combat().world().quest().actors().isEmpty());
        }
    }
    @Test void secondLaneUsesSparseGlobalEntityAndOwnStructureOnly()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,1,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,1,2);
            var own=roundtrip(frame(f,1));var other=roundtrip(frame(f,0));var s=own.combat().npcFireStorms().get(0);
            assertEquals(2,s.entityId());assertEquals(1,s.lane());assertEquals(5,((ModernNpcFireStormStructures.QuestHit)s.structure().hit()).id().slot());
            assertNull(other.combat().npcFireStorms().get(0).structure());assertTrue(other.combat().npcFireStorms().get(0).targets().isEmpty());
        }
    }
    @Test void timeTowerProofRemainsAtomicThroughWavesDepartureAndSavedResult()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            f.active();roundtrip(frame(f));ModernNpcFireStormStructuresTest.cast(f,0,1);var initial=roundtrip(frame(f)).combat().npcFireStorms().get(0).structure();
            f.engine.withdrawTimeAttack(f.players.get(0).owner());roundtrip(frame(f));
            for(int i=0;i<4;i++){f.elapsed((i+1)*2000L);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,1,i+1).reason());roundtrip(frame(f));}
            var n=frame(f);var c=n.combat();var w=c.world();assertEquals(ModernWorldProtocol.Phase.FINISHED,w.phase());assertEquals(initial,c.npcFireStorms().get(0).structure());
            var missing=new ArrayList<>(c.npcFireStorms());var s=missing.get(0);missing.set(0,copy(s,s.revision(),s.casterLife(),s.origin(),s.targets(),null));assertThrows(IllegalArgumentException.class,()->states(c,missing));
            var saved=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest(),w.timeAttack().withSettlement(ModernWorldProtocol.SettlementState.SAVED));
            roundtrip(n.withCombat(new ModernCombatProtocol.Frame(saved,c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),c.npcDisintegrations(),c.npcFireStorms())));
        }
    }
    @Test void deathRespawnAndHistoricalStructureUseIndependentLifeEpochs()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1,1,false)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);var prior=roundtrip(frame(f,0)).combat().npcFireStorms().get(0).structure();
            f.randomValue=19;var dead=ModernQuestLifeTest.die(f,2100);roundtrip(frame(f,0));
            ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0));roundtrip(frame(f,0));f.elapsed(4000);f.randomValue=0;ModernNpcFireStormStructuresTest.cast(f,0,3);
            var c=roundtrip(frame(f,0)).combat();assertEquals(2,c.npcFireStorms().get(0).casterLife());assertEquals(prior,c.npcFireStorms().get(0).structure());
        }
    }
    @Test void spellMixAndNaturalRegenKeepCompleteMpBudget()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var target=f.spawn(0,11,10);ModernBattleEngineNpcFireStormTest.cast(f,1);roundtrip(frame(f,0));
            f.elapsed(1200);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,2,target).reason());roundtrip(frame(f,0));
            f.elapsed(2400);assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,target).reason());roundtrip(frame(f,0));
            f.elapsed(4400);ModernBattleEngineNpcFireStormTest.cast(f,4);var c=roundtrip(frame(f,0)).combat();assertEquals(96,c.npcFireStorms().get(0).mpSpent());assertEquals(70,c.npcDisintegrations().get(0).mpSpent());assertEquals(3,c.npcSpells().get(0).mpSpent());
            f.elapsed(16000);roundtrip(frame(f,0));assertThrows(IllegalArgumentException.class,()->states(c,List.of(ModernNpcFireStormState.State.initial(1,48),ModernNpcFireStormState.State.initial(2,48))));
        }
    }
    @Test void missingLedgerWrongManaScopeLifeOriginAndTargetHpAreRejected()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture()) {
            f.active();f.spawn(0,11,10);ModernBattleEngineNpcFireStormTest.cast(f,1);var n=frame(f,0);var c=n.combat();var s=c.npcFireStorms().get(0);
            assertThrows(IllegalArgumentException.class,()->states(c,List.of()));
            for(int change=0;change<4;change++) {
                var rows=new ArrayList<>(c.npcFireStorms());var h=s.targets().get(0);var scope=change==0?new ModernNpcRoster.Scope(n.scope().roomId(),n.scope().matchId(),n.scope().generation()+1):n.scope();
                var target=new ModernNpcFireStormState.Target(new ModernNpcRoster.Id(scope,h.id().serial()),change==1?h.life()+1:h.life(),h.lane(),change==2?new ModernWorldMap.Cell(h.cell().x()+1,h.cell().y()):h.cell(),
                        change==3?h.hpBefore()+1:h.hpBefore(),change==3?new ModernFireStorm.Outcome(h.outcome().rawDamage(),h.outcome().appliedDamage(),h.outcome().remainingHp()+1,false):h.outcome());
                assertThrows(IllegalArgumentException.class,()->{rows.set(0,copy(s,s.revision(),s.casterLife(),s.origin(),List.of(target),null));n.withCombat(states(c,rows));});
            }
            var v=c.vitals().get(0);var vitals=new ArrayList<>(c.vitals());vitals.set(0,new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp()+1,v.maxMp(),v.ac(),v.weaponType(),v.attackSequence(),v.attackRevision(),v.hurtRevision(),v.deathRevision(),v.lastTargetEntityId(),v.attackIntervalNanos()));
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(c.world(),vitals,c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),c.npcDisintegrations(),c.npcFireStorms()));
        }
    }
    @Test void partialTowerDamageThenMeleeStartNeedsNoInventedMagicKill()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(36,2)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);roundtrip(frame(f));f.elapsed(1200);f.random=19;f.tower(0,1);var n=roundtrip(frame(f));
            assertFalse(n.combat().npcFireStorms().get(0).structure().hit().outcome().death());assertEquals(0,n.combat().world().timeAttack().towerHp());
        }
    }
    @Test void legacyVersionsRemainClosedAndOnlyExplicitV31PeersReceiveNewFrames()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            var n=frame(f,0);var encoded=bytes(n);
            for(int v=1;v<=30;v++){int version=v;assertThrows(Exception.class,()->ModernNpcProtocol.putFrame(ByteBuffer.allocate(1024*1024),n,version));assertThrows(Exception.class,()->ModernNpcProtocol.getFrame(ByteBuffer.wrap(encoded),version));}
            var message=ModernNpcDisintegrateFrameTest.message(f.players.get(0).scope(),n);
            assertTrue(n.combat().world().descriptor().supportedBy(31,ModernSessionProtocol.KNOWN_CAPABILITIES));
            assertFalse(n.combat().world().descriptor().supportedBy(31,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT)));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,30,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,31),31));assertEquals(47,ModernSessionProtocol.MAX_VERSION);
            assertEquals(ModernSessionProtocol.MAX_VERSION,ModernWorldProtocol.CODEC_MAX_VERSION);
            for(int version:new int[]{32,33,34}) {
                assertArrayEquals(ModernApplicationProtocol.encode(message,31),ModernApplicationProtocol.encode(message,version),"chat/matchmaking versions do not alter existing world/combat/NPC bytes");
                assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,31),version));
            }
            assertEquals(message,ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,31,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        }
    }
    @Test void everyTruncationAndDroppedFireSectionCannotBecomeACompleteNpcFrame()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);var n=frame(f);var b=bytes(n);
            for(int length=0;length<b.length;length++){var shortBytes=Arrays.copyOf(b,length);assertThrows(Exception.class,()->ModernNpcProtocol.getFrame(ByteBuffer.wrap(shortBytes),31));}
            var c=n.combat();assertThrows(IllegalArgumentException.class,()->states(c,List.of()));
            assertEquals(1024*1024,ModernNpcProtocol.maxFrameBytes(31));assertEquals(256*1024,ModernNpcProtocol.maxFrameBytes(30));
        }
    }
    @Test void historicalObjectiveProofMustPrecedeTheNextObjectivesBirth()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);
            var original=frame(f,0).combat().npcFireStorms().get(0).structure();
            f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);f.elapsed(6000);ModernNpcFireStormStructuresTest.cast(f,0,4);
            var c=roundtrip(frame(f,0)).combat();var s=c.npcFireStorms().get(0);var current=c.world().quest().actor(2);
            assertNotNull(current);assertTrue(current.bornRevision()>original.revision());
            var forged=new ModernNpcFireStormStructures.Receipt(3,current.bornRevision(),3,original.casterLife(),original.origin(),original.hit());
            var altered=copy(s,s.revision(),s.casterLife(),s.origin(),s.targets(),forged);
            assertThrows(IllegalArgumentException.class,()->states(c,List.of(altered)));
        }
    }
    @Test void magicTowerProofCannotBeReinterpretedAsLegacyMeleeData()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);var n=roundtrip(frame(f));var w=n.combat().world();var d=w.timeAttack();
            assertTrue(d.exchanges().isEmpty());assertTrue(d.towerDeathRevision()>0);
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttackProtocol.put(ByteBuffer.allocate(4096),d));
            var b=ByteBuffer.allocate(4096);ModernTimeAttackProtocol.put(b,d,true);b.flip();
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttackProtocol.get(b,n.scope()));
            var old=w.descriptor();var legacy=new ModernMatchDescriptor(old.ruleset(),old.features()&~NPC_FIRE_STORM,
                    old.preparationTimeoutMillis(),old.loadTimeoutMillis(),old.countdownNanos(),old.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->w.withDescriptor(legacy));
        }
    }
    @Test void deniedCastsAndAbortedRoomsRetainValidatedHistoryWithoutInventingAnEvent()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture()) {
            f.active();f.spawn(0,11,10);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernBattleEngineNpcFireStormTest.cast(f,1).reason());
            var before=roundtrip(frame(f,0)).combat().npcFireStorms();
            assertEquals(ModernNpcFireStormState.Reason.TOO_FAST,ModernBattleEngineNpcFireStormTest.cast(f,2).reason());
            assertEquals(before,roundtrip(frame(f,0)).combat().npcFireStorms());
            f.rooms.releasePreparation(f.ticket);var aborted=roundtrip(frame(f,0));
            assertEquals(ModernWorldProtocol.Phase.ABORTED,aborted.combat().world().phase());assertTrue(aborted.closed());
            assertTrue(aborted.actors().isEmpty());assertEquals(before,aborted.combat().npcFireStorms());
        }
    }
    @Test void hundredCastersRetainAllFortyEightHitsBeyondLegacyWholeFrameLimit()throws Exception {
        var n=ModernNpcFireStormFramePeer.wide().get(0).frame();var data=bytes(n);
        assertTrue(data.length>ModernNpcProtocol.maxFrameBytes(30));assertTrue(data.length<ModernNpcProtocol.maxFrameBytes(31)-16);
        assertEquals(100,n.combat().npcFireStorms().size());assertEquals(4800,n.combat().npcFireStorms().stream().mapToInt(s->s.targets().size()).sum());
        assertEquals(96,n.actors().size());roundtrip(n);
        assertThrows(Exception.class,()->ModernNpcProtocol.getFrame(ByteBuffer.wrap(data),30));
    }
}
