package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Real command commits. Test-only qualified profile installation below is
 * NOT authorization for live advanced SET, a negotiated wire or desktop QA. */
class ModernBattleCommandTest {
    static void same(ModernBattleEngine.Observed<?> observed) {
        assertSame(observed.outcome().observation(),observed.observation());
    }
    static void legacy(ModernBattleCommand command,ModernBattleEngine.Snapshot snapshot) {
        assertEquals(ModernLegacyBattleProjection.from(command.observation()),snapshot);
    }
    static void npc(ModernBattleCommand command,ModernNpcBattle.Frame frame) {
        assertEquals(ModernLegacyBattleProjection.npcs(command.observation()),frame);
    }
    @Test void everyTypedResultStoresCanonicalFactsAndNoLegacyFrame() {
        var types=List.of(ModernBattleCommand.class.getPermittedSubclasses());
        assertEquals(java.util.Set.of(ModernBattleObservation.Capture.class,
                ModernBattleEngine.MoveResult.class,ModernBattleEngine.AttackResult.class,ModernBattleEngine.CastResult.class,
                ModernBattleEngine.DisintegrateResult.class,ModernBattleEngine.GreaterHasteResult.class,ModernBattleEngine.SlowResult.class,
                ModernBattleEngine.HasteResult.class,ModernBattleEngine.PlayerHealingResult.class,ModernBattleEngine.ItemResult.class,
                ModernBattleEngine.AmmoResult.class,ModernBattleEngine.SiegePortalResult.class,ModernNpcBattle.Result.class,
                ModernNpcMagic.Result.class,ModernNpcDisintegrateState.Result.class,ModernNpcFireStormState.Result.class,
                ModernQuestLife.Result.class),java.util.Set.copyOf(types));
        for(var type:types) {
            assertTrue(type.isRecord());
            if(type==ModernBattleObservation.Capture.class)continue;
            assertTrue(List.of(type.getRecordComponents()).stream().anyMatch(c->c.getName().equals("observation")&&c.getType()==ModernBattleObservation.Capture.class));
            for(var c:type.getRecordComponents()) {
                assertNotEquals(ModernBattleEngine.Snapshot.class,c.getType());assertNotEquals(ModernNpcBattle.Frame.class,c.getType());
            }
        }
    }
    @Test void serviceReusesLegacyProjectionInsteadOfRepeatingFormerRecordAccessors()throws Exception {
        var source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/local/lito381/server/ModernWorldService.java"));
        assertFalse(source.contains("fromCombat(result.snapshot()"));
        assertFalse(source.contains("fromEngine(result.snapshot()"));
        assertFalse(source.contains("result.snapshot().revision()"));
    }
    @Test void loadingAcknowledgmentReturnsItsExactCaptureAndAbortIsAlsoCanonical()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var p=f.players.get(0);var ack=f.engine.observeCommand(()->f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest()));
            same(ack);assertSame(ack.outcome(),ack.observation());assertEquals(ModernBattleEngine.Phase.LOADING,ack.observation().phase());
            assertTrue(ack.observation().actors().get(0).pose().loaded());assertFalse(ack.observation().actors().get(1).pose().loaded());
            legacy(ack.outcome(),f.engine.tick());var aborted=f.engine.observeCommand(f.engine::abortObservation);same(aborted);
            assertEquals(ModernBattleEngine.AbortReason.SERVER_ABORT,aborted.observation().abortReason());legacy(aborted.outcome(),f.engine.tick());
            assertEquals(ModernBattleEngine.Phase.LOADING,ack.observation().phase());
        }
    }
    @Test void observerNeverRecapturesOrAdvancesAfterItsCommandResult()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var result=f.engine.observeCommand(()->{var cast=f.cast(0,1,1,3);f.elapsed(2_000_000_000L);return cast;});same(result);
            assertEquals(ModernSpellProtocol.Reason.NONE,result.outcome().reason());assertEquals(0,result.observation().clock().loadingElapsedNanos());
            var retained=result.observation().forOwner(f.players.get(0).scope());f.engine.serviceObservation();f.engine.close();
            assertEquals(retained,result.observation().forOwner(f.players.get(0).scope()));legacy(result.outcome(),result.outcome().snapshot());
        }
    }
    @Test void observerRejectsStaleCommandAndForeignRoomEvenWhenNumericIdsAndClocksMatch()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture();var other=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();other.active();var foreign=other.engine.serviceObservation();var own=f.engine.serviceObservation();
            assertEquals(own.identity(),foreign.identity());assertEquals(own.revision(),foreign.revision());assertEquals(own.clock(),foreign.clock());
            assertThrows(IllegalArgumentException.class,()->f.engine.observeCommand(()->foreign));
            assertThrows(IllegalArgumentException.class,()->f.engine.observeCommand(()->{var first=f.cast(0,1,1,3);f.cast(0,2,1,3);return first;}));
            var current=f.engine.serviceObservation();assertEquals(2,current.actors().get(0).runtime().sequence());
            assertEquals(1,current.actors().get(0).histories().get(ModernUseHistory.Lane.HEALING).current().casts());
            assertThrows(NullPointerException.class,()->f.engine.observeCommand(()->null));assertEquals(current,f.engine.serviceObservation());
        }
    }
    @Test void closingInsideCallbackCannotPublishAFormerlyLiveResult()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();assertThrows(IllegalStateException.class,()->f.engine.observeCommand(()->{var result=f.cast(0,1,1,3);f.engine.close();return result;}));
            assertThrows(IllegalStateException.class,()->f.engine.observeCommand(()->{fail("closed observer invoked command");return null;}));
        }
    }
    @Test void actualMoveAttackHealingAndFireStormKeepTypedReasonAndSnapshotParity()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true)) {
            f.active();var move=f.engine.observeCommand(()->f.move(0,1,4));same(move);assertEquals(ModernBattleEngine.MoveStatus.MOVED,move.outcome().status());legacy(move.outcome(),move.outcome().snapshot());
            var attack=f.engine.observeCommand(()->f.attack(1,1,1));same(attack);legacy(attack.outcome(),attack.outcome().snapshot());
            f.elapsed(1_000_000_000L);var heal=f.engine.observeCommand(()->f.cast(0,1,1,3));same(heal);assertEquals(ModernSpellProtocol.Reason.NONE,heal.outcome().reason());legacy(heal.outcome(),heal.outcome().snapshot());
            f.elapsed(3_000_000_000L);var fire=f.engine.observeCommand(()->f.cast(0,2,70,0));same(fire);assertEquals(ModernSpellProtocol.Reason.NONE,fire.outcome().reason());legacy(fire.outcome(),fire.outcome().snapshot());
            assertEquals(1,heal.observation().actors().get(0).runtime().sequence());
        }
    }
    @Test void actualDisintegrateHasOneCanonicalCriticalReceipt()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1000,1000,500,true)) {
            f.active();var result=f.engine.observeCommand(()->ModernBattleEngineDisintegrateTest.cast(f,1,2));same(result);
            assertEquals(ModernDisintegrateState.Reason.NONE,result.outcome().reason());legacy(result.outcome(),result.outcome().snapshot());
            var owner=result.observation().forOwner(f.players.get(0).scope());
            assertInstanceOf(ModernSpellRuntime.Disintegrate.class,owner.owner().history().spells().receipts().get(ModernUseHistory.Lane.DISINTEGRATE).detail());
        }
    }
    @Test void actualAmmoAndPotionResultsKeepStocksAndResourceChangesTogether()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var select=f.engine.observeCommand(()->f.select(1,40746));same(select);assertEquals(ModernAmmoProtocol.Reason.NONE,select.outcome().reason());legacy(select.outcome(),select.outcome().snapshot());
            var shot=f.engine.observeCommand(()->f.attack(1));same(shot);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,shot.outcome().status());legacy(shot.outcome(),shot.outcome().snapshot());
            assertEquals(select.observation().actors().get(0).ammo().selected().remaining()-1,shot.observation().actors().get(0).ammo().selected().remaining());
        }
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();f.cast(1,1,4);var potion=f.engine.observeCommand(()->f.heal(0));same(potion);assertEquals(ModernItemProtocol.Reason.NONE,potion.outcome().reason());legacy(potion.outcome(),potion.outcome().snapshot());
            var owner=potion.observation().forOwner(f.room.players.get(0).scope()).owner();assertEquals(1,owner.supplies().history().uses());
            assertEquals(owner.supplies().history().restored(),owner.history().history().resources().flows().restored());
        }
    }
    @Test void npcSpawnMeleeAndAllThreeSpellResultsKeepExactRosterAndOwnerFacts()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var template=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var spawned=f.engine.observeCommand(()->f.engine.spawnNpcsObservation(f.scope,List.of(new ModernNpcRoster.Spawn(template,0,1,ModernBattleEngineNpcTest.cell(11,10),2))));same(spawned);
            var n=spawned.observation().npcs().roster().actors().get(0);legacy(spawned.outcome(),f.engine.tick());
            var magic=f.engine.observeCommand(()->ModernBattleEngineNpcMagicTest.cast(f,0,1,n));same(magic);assertEquals(ModernNpcMagic.Reason.NONE,magic.outcome().reason());npc(magic.outcome(),magic.outcome().frame());
            f.elapsed(1200);var fire=f.engine.observeCommand(()->ModernBattleEngineNpcFireStormTest.cast(f,2));same(fire);assertEquals(ModernNpcFireStormState.Reason.NONE,fire.outcome().reason());npc(fire.outcome(),fire.outcome().frame());
            f.elapsed(2400);var dis=f.engine.observeCommand(()->ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n));same(dis);assertEquals(ModernNpcDisintegrateState.Reason.NONE,dis.outcome().reason());npc(dis.outcome(),dis.outcome().frame());
            var step=f.engine.observeCommand(()->f.step(n));same(step);npc(step.outcome(),step.outcome().frame());
            assertEquals(0,spawned.observation().actors().get(0).runtime().sequence());
        }
    }
    @Test void portalAndQuestDeathCommandsKeepTheirModeSpecificReasons()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();var portal=f.engine.observeCommand(()->f.portal(0));same(portal);legacy(portal.outcome(),portal.outcome().snapshot());
        }
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            var dead=ModernQuestLifeTest.die(f,100);var life=ModernQuestLifeTest.life(dead,0);
            var confirm=f.engine.observeCommand(()->ModernQuestLifeTest.confirm(f,0,life));same(confirm);
            assertEquals(ModernQuestLife.Reason.REVIVED,confirm.outcome().reason());npc(confirm.outcome(),confirm.outcome().frame());
            var stale=f.engine.observeCommand(()->ModernQuestLifeTest.confirm(f,0,life));same(stale);assertEquals(ModernQuestLife.Reason.STALE_LIFE,stale.outcome().reason());
            assertEquals(confirm.observation(),stale.observation());assertTrue(life.dead());
        }
    }
    @Test void individualDeparturesReturnCanonicalRaceAndCooperativeState()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            f.active();var left=f.engine.observeCommand(()->f.engine.withdrawRacerObservation(f.a.owner(),ModernPetRace.Dnf.LEFT));same(left);legacy(left.outcome(),f.engine.tick());
            assertEquals(1,f.engine.ownerCount());assertThrows(SecurityException.class,()->f.engine.observation(f.a.owner(),f.a.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
        try(var f=ModernNpcFireStormStructuresTest.time(1,1)) {
            f.active();var left=f.engine.observeCommand(()->f.engine.withdrawTimeAttackObservation(f.players.get(0).owner()));same(left);legacy(left.outcome(),f.engine.tick());
            assertTrue(left.observation().modes().timeAttack().state().departed().contains(1L));
        }
    }
    static void set(Object target,String name,Object value)throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    @Test void actualCommandsAfterQualifiedReprofileDoNotConstructUnrepresentableLegacySnapshots()throws Exception {
        var sources=new java.util.HashMap<Integer,ModernPhysicalCombat.Source>();
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true,p->{
            var source=ModernBattleEquipmentHistoryTest.source(p);sources.put(p.scope().sourceId(),source);var fighter=ModernPhysicalCombatTest.fighter(source);
            return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),
                    true,p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true,false,true,true);
        })) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());f.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,70,0).reason());
            var p=f.players.get(0);var before=f.engine.serviceObservation();var old=before.actors().get(0);var h=before.forOwner(p.scope()).owner().history().history();
            assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            var source=sources.get(7);var q=source.input().equipment();
            var next=ModernPhysicalCombat.qualifyEquipmentChange(source,ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,1,Map.of(1L,false)),
                    1000,old.loadout().fighter(),h.resources().vitals().hp(),h.resources().vitals().mp());
            var loadout=old.loadout().replacement(next);var history=h.reprofile(before.revision()+1,loadout.historyProfile(h.spells().keySet(),true));
            assertTrue(ModernEquipmentHistory.followsReprofile(h,history));
            // Deliberate fixture-only installation, never a server SET API.
            synchronized(f.engine) {
                var actor=ModernBattleVitalBalanceTest.actor(f.engine,0);set(actor,"loadout",loadout);set(actor,"equipmentResources",history.resources());
                set(actor,"spellHistory",history.spells());set(actor,"targetHistory",history.targets());set(actor,"itemHistory",history.items());
                set(actor,"hp",history.resources().vitals().hp());set(actor,"mp",history.resources().vitals().mp());
                set(actor,"weight",loadout.reweight(old.supplies().stocks(),List.of(),old.weight().units()));set(f.engine,"revision",history.worldRevision());
            }
            var lane=ModernUseHistory.Lane.HEALING;var prior=h.spells().get(lane);var cost=loadout.healing().mpCost();assertTrue(cost>prior.current().cost());
            f.elapsed(4_000_000_000L);var cast=f.engine.observeCommand(()->f.cast(0,3,1,3));same(cast);assertEquals(ModernSpellProtocol.Reason.NONE,cast.outcome().reason());
            var observed=cast.observation();var owner=observed.forOwner(p.scope());var after=owner.owner().history().history();
            assertEquals(prior.current().spent()+cost,after.spells().get(lane).current().spent());assertEquals(history.resources().vitals().mp()-cost,after.resources().vitals().mp());
            assertEquals(h.spells().get(ModernUseHistory.Lane.FIRE_STORM).last(),after.spells().get(ModernUseHistory.Lane.FIRE_STORM).last());
            var d=ModernBattlePublicationTest.honor(observed);d=new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.FIRE_STORM,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            assertEquals(after,new ModernBattlePublication(observed,d,null,null).audience(p.scope()).personal().history());
            assertThrows(IllegalArgumentException.class,cast.outcome()::snapshot);assertEquals(owner,f.engine.serviceObservation().forOwner(p.scope()));
            assertThrows(SecurityException.class,()->f.cast(0,3,1,3));assertEquals(owner,f.engine.serviceObservation().forOwner(p.scope()));
            f.elapsed(6_000_000_000L);var again=f.engine.observeCommand(()->f.cast(0,4,1,3));same(again);assertEquals(ModernSpellProtocol.Reason.NONE,again.outcome().reason());
            assertEquals(after.resources().flows().spent()+cost,again.observation().forOwner(p.scope()).owner().history().history().resources().flows().spent());
            assertThrows(IllegalArgumentException.class,again.outcome()::snapshot);assertEquals(owner,observed.forOwner(p.scope()));
            assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
}
