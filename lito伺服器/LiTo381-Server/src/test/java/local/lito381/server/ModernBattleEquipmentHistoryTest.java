package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernBattleEquipmentHistoryTest {
    static ModernEquipmentHistory.State read(ModernBattleEngine e,ModernRooms.FrozenPreparation t,ModernBattleEngineBotTest.Player p) {
        return e.equipmentHistory(p.owner(),p.scope(),t.matchId(),t.generation());
    }
    static ModernBattleLoadout loadout(ModernBattleEngine e,int index)throws Exception {
        var a=ModernBattleVitalBalanceTest.actor(e,index);var f=a.getClass().getDeclaredField("loadout");f.setAccessible(true);return (ModernBattleLoadout)f.get(a);
    }
    static ModernPhysicalCombat.Source source(ModernBattleEngine.PreparedMember p) {
        var s=ModernBattleLoadoutTest.source(p.scope().sourceId());var old=s.input().equipment();
        var snapshot=new ModernCharacterData.Snapshot(p.scope().sourceId(),p.character(),old.source().persisted(),old.source().inventory());
        var q=new ModernEquipmentFacts.Qualified(snapshot,old.items(),old.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,s.input().weapons()),s.growth(),s.templates(),s.procCatalogIds(),
                s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    @Test void realHonorCasterCandidateUsesOneQualifiedReplacementAndPreservesEveryCommittedReceipt()throws Exception {
        var sources=new java.util.HashMap<Integer,ModernPhysicalCombat.Source>();
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true,p->{
            var s=source(p);sources.put(p.scope().sourceId(),s);var fighter=ModernPhysicalCombatTest.fighter(s);
            return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),true,p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true,false,true,true);
        })) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());f.elapsed(2_000_000_000L);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,70,0).reason());var before=read(f.engine,f.ticket,f.players.get(0));
            var old=loadout(f.engine,0);var source=sources.get(7);var q=source.input().equipment();
            var next=ModernPhysicalCombat.qualifyEquipmentChange(source,ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,1,Map.of(1L,false)),
                    1000,old.fighter(),before.resources().vitals().hp(),before.resources().vitals().mp());
            var replacement=old.replacement(next);var candidate=before.reprofile(before.worldRevision()+1,replacement.historyProfile(before.spells().keySet(),true));
            assertTrue(ModernEquipmentHistory.followsReprofile(before,candidate));assertEquals(before.targets(),candidate.targets());
            assertEquals(before.items().last(),candidate.items().last());assertEquals(8,before.items().fixedRecovery());assertEquals(0,candidate.items().fixedRecovery());
            assertTrue(candidate.spells().get(ModernUseHistory.Lane.HEALING).current().cost()>before.spells().get(ModernUseHistory.Lane.HEALING).current().cost());
            assertTrue(candidate.spells().get(ModernUseHistory.Lane.FIRE_STORM).current().cost()>before.spells().get(ModernUseHistory.Lane.FIRE_STORM).current().cost());
            assertEquals(before.resources().flows().spent(),candidate.resources().flows().spent());assertEquals(100,candidate.resources().vitals().maximumHp());
            assertEquals(50,candidate.resources().vitals().maximumMp());assertEquals(before,read(f.engine,f.ticket,f.players.get(0)),"pure candidate cannot publish an unnegotiated advanced SET");
            var lane=ModernUseHistory.Lane.HEALING;var oldUse=before.spells().get(lane).last();
            var recast=candidate.cast(lane,candidate.worldRevision()+1,3,new ModernWorldMap.Cell(oldUse.before().x(),oldUse.before().y()),before.targets().get(lane).last().targets());
            var runtime=ModernBattleSpellRuntimeTest.read(f.engine,f.ticket,f.players.get(0)).spells();
            runtime.validate(candidate);
            var runtimeRecast=runtime.cast(candidate,recast,lane,runtime.latest().atNanos()+2_000_000_000L,ModernSpellRuntime.Ordinary.INSTANCE);
            assertEquals(oldUse.cost()+replacement.healing().mpCost(),runtimeRecast.receipts().get(lane).spent());
            assertEquals(3,runtimeRecast.sequence());assertEquals(runtime.lawful(),runtimeRecast.lawful());
            assertEquals(runtime,ModernBattleSpellRuntimeTest.read(f.engine,f.ticket,f.players.get(0)).spells(),"candidate is not a live SET");
            assertTrue(ModernEquipmentHistory.followsCast(candidate,recast,lane));
            assertEquals(before.resources().flows().spent()+replacement.healing().mpCost(),recast.resources().flows().spent());
            assertEquals(oldUse.cost()+replacement.healing().mpCost(),recast.spells().get(lane).current().spent());
            assertEquals(recast,ModernEquipmentHistoryWire.decodeBound(ModernEquipmentHistoryWire.encode(recast),recast.identity(),recast.worldRevision()));
            var p=f.players.get(0);assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
    @Test void npcThreeLanesAndRetainedQuestStructureEnterTheActualOwnerBoundBundle()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);ModernBattleEngineNpcMagicTest.cast(f,0,1,n);f.elapsed(1200);ModernBattleEngineNpcFireStormTest.cast(f,2);
            f.elapsed(2400);ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n);var state=read(f.engine,f.ticket,f.players.get(0));
            assertEquals(3,state.targets().size());assertEquals(state.resources().flows().spent(),state.spells().values().stream().mapToLong(h->h.current().spent()).sum());
        }
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);
            var old=read(f.engine,f.ticket,f.players.get(0));var receipt=old.targets().get(ModernUseHistory.Lane.NPC_FIRE_STORM).structure();assertNotNull(receipt);
            f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);var after=read(f.engine,f.ticket,f.players.get(0));
            assertEquals(receipt,after.targets().get(ModernUseHistory.Lane.NPC_FIRE_STORM).structure());
            var p=after.profile();var repriced=after.reprofile(after.worldRevision()+1,new ModernEquipmentHistory.Profile(p.equipmentRevision()+1,p.maximumHp(),p.maximumMp(),p.costs(),p.fixedRecovery()));
            var targets=new java.util.EnumMap<>(repriced.targets());var h=targets.get(ModernUseHistory.Lane.NPC_FIRE_STORM);
            targets.put(ModernUseHistory.Lane.NPC_FIRE_STORM,new ModernTargetHistory.History(h.last(),null));
            var erased=new ModernEquipmentHistory.State(repriced.identity(),repriced.worldRevision(),repriced.resources(),repriced.spells(),repriced.items(),targets);
            assertFalse(ModernEquipmentHistory.followsReprofile(after,erased));assertTrue(ModernEquipmentHistory.followsReprofile(after,repriced));
        }
    }
    @Test void realRegenPotionAndNewLifeStillKeepHistoricalCostsInCompleteBundle()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();f.cast(1,1,4);f.heal(0);f.elapsed(16_000_000_000L);var state=read(f.engine,f.ticket,f.room.players.get(0));
            assertEquals(state.items().restored(),state.resources().flows().restored());assertEquals(f.engine.tick().regen().get(0).hpGain(),state.resources().flows().hpGain());
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();f.cast(1,1,1,4);f.cast(0,1,4,2);var dead=read(f.engine,f.ticket,f.players.get(1));
            f.elapsed(5_000_000_000L);var alive=read(f.engine,f.ticket,f.players.get(1));assertEquals(1,alive.resources().life());
            assertEquals(dead.spells(),alive.spells());assertEquals(dead.targets(),alive.targets());assertEquals(4,alive.resources().flows().spent());
        }
    }
    @Test void completeObservationRejectsWrongOwnerScopeOrGenerationAndDoesNotEnableCapabilities()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var p=f.players.get(0);var q=f.players.get(1);var before=read(f.engine,f.ticket,p);
            assertEquals(p.scope(),before.identity().source());assertEquals(f.ticket.snapshot().summary().id(),before.identity().world().roomId());
            assertThrows(SecurityException.class,()->f.engine.equipmentHistory(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.equipmentHistory(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1));
            assertEquals(before,read(f.engine,f.ticket,p));assertFalse(before.spells().containsKey(ModernUseHistory.Lane.NPC_FIRE_STORM));
        }
    }
}
