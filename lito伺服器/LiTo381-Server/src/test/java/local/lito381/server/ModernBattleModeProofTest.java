package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

/** Canonical ENGINE state only: no old wire frame is used to reject these mutations. */
class ModernBattleModeProofTest {
    @TempDir Path temporary;
    static ModernBattleObservation.Capture siege(ModernBattleObservation.Capture c,ModernSiege.State state) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),m.timeAttack(),
                new ModernSiege.Snapshot(state,m.siege().lives()),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static ModernBattleObservation.Capture time(ModernBattleObservation.Capture c,ModernTimeAttack.State state) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),
                new ModernTimeAttack.Snapshot(state,m.timeAttack().exchanges()),m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static ModernSiege.State towers(ModernSiege.State s,List<ModernSiege.Objective> rows) {
        return new ModernSiege.State(s.scope(),s.config(),s.attacker(),s.phase(),rows,s.revision(),s.gateRevision(),s.finishRevision(),s.finishedAtMillis(),s.winner());
    }
    static ModernTimeAttack.State timeState(ModernTimeAttack.State s,long leader,Set<Long> departed,Set<ModernNpcRoster.Id> living) {
        return new ModernTimeAttack.State(s.scope(),s.plan(),s.revision(),s.phase(),s.wave(),s.startedMillis(),s.observedMillis(),leader,departed,living,
                s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),s.end());
    }
    @Test void defenderCannotOwnAnAttackOnTheSiegeTower()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();var c=f.tower(0,1,ModernSiege.Slot.OUTER_LEFT).observation();var s=c.modes().siege().state();var o=s.objectives().get(0);
            var forged=new ModernSiege.Objective(o.tower(),o.hp(),o.damageLoss(),o.hitCount(),o.revision(),o.deathRevision(),2);
            assertThrows(IllegalArgumentException.class,()->siege(c,towers(s,ModernBattleLifeProofTest.replace(s.objectives(),0,forged))).world());
        }
    }
    @Test void innerTowerCannotHaveBeenHitBeforeTheOuterGateOpens()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1000,60_000,1000,0,true)) {
            f.active();var c=f.tower(0,1,ModernSiege.Slot.OUTER_LEFT).observation();var s=c.modes().siege().state();var o=s.objectives().get(2);
            var forged=new ModernSiege.Objective(o.tower(),999,1,1,s.revision(),0,1);
            assertThrows(IllegalArgumentException.class,()->siege(c,towers(s,ModernBattleLifeProofTest.replace(s.objectives(),2,forged))).world());
        }
    }
    @Test void closedGateCannotContainAnActorInTheCanonicalWorld()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();var w=f.engine.serviceObservation().world();var actor=ModernBattleLifeProofTest.position(w.actors().get(0),ModernBattleEngineSiegeTest.cell(30,10));
            assertThrows(IllegalArgumentException.class,()->ModernBattleLifeProofTest.actors(w,ModernBattleLifeProofTest.replace(w.actors(),0,actor)));
        }
    }
    @Test void timeAttackCannotNameAnAbsentLeaderOrKeepADepartedLeaderActive()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();var c=f.engine.serviceObservation();var s=c.modes().timeAttack().state();
            assertThrows(IllegalArgumentException.class,()->time(c,timeState(s,2,Set.of(),s.living())).world());
            assertThrows(IllegalArgumentException.class,()->time(c,timeState(s,1,Set.of(1L),s.living())).world());
        }
    }
    @Test void waveLivingSetMustBeTheActualNpcRoster()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();f.tower(0,1);var c=f.engine.serviceObservation();var s=c.modes().timeAttack().state();
            var foreign=Set.of(new ModernNpcRoster.Id(s.scope(),999));
            assertThrows(IllegalArgumentException.class,()->time(c,timeState(s,s.leader(),s.departed(),foreign)).world());
        }
    }
    @Test void deathmatchCannotReverseTheActuallySurvivingTeam()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture(1,100,100,true,0)) {
            f.active();for(long seq=1;seq<=10&&f.engine.serviceObservation().result()==null;seq++){f.attack(seq);f.clock.addAndGet(1_000_000_000L);}
            var c=f.engine.serviceObservation();var e=c.result();assertNotNull(e);
            var rows=e.participants().stream().map(p->new ModernMatchSettlement.Participant(p.entityId(),p.identity(),p.team(),
                    p.verdict()==ModernMatchSettlement.Verdict.WIN?ModernMatchSettlement.Verdict.LOSS:ModernMatchSettlement.Verdict.WIN)).toList();
            var forged=new ModernMatchSettlement.Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),rows);
            assertThrows(IllegalArgumentException.class,()->ModernBattleHonorProofTest.result(c,forged).world());
        }
    }
    static ModernBattleObservation.Capture events(ModernBattleObservation.Capture c,ModernMatchSettlement.Event time,ModernMatchSettlement.Event siege) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),m.timeAttack(),m.siege(),m.race(),m.raceResultId(),time,siege));
    }
    static ModernBattlePublication republish(ModernBattlePublication original,ModernBattleObservation.Capture candidate) {
        return new ModernBattlePublication(candidate,original.descriptor(),original.resources(),original.names());
    }
    @Test void timeAttackPublicationRejectsAValidButUnrelatedPlanProofAndTheInternalBootstrap()throws Exception {
        try(var f=new ModernTimeAttackServiceTest.Fixture(temporary,1,new ModernTimeAttackServiceTest.Journal())) {
            var w=f.active();ModernBattlePublicationTest.stopScheduler(f.service);var player=f.players.get(0);
            f.tower(player,w);for(int n=0;n<3;n++)f.hit(player,w,n+2);
            var p=ModernBattleAudienceTest.published(f.service,player.owner());var c=p.capture();var e=c.modes().timeAttackResult();assertNotNull(e);
            var t=e.timeAttack();var altered=new ModernTimeAttackSettlement.Proof(t.roomId(),t.physicalMapId(),t.terrainSha256(),"1".repeat(64),t.end(),t.wave(),t.waveCount(),
                    t.leader(),t.durationMillis(),t.startedMillis(),t.observedMillis(),t.aliveMask(),t.departedMask(),t.towerDeathRevision());
            assertNotEquals(t,altered);
            var foreign=new ModernMatchSettlement.Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants(),altered);
            assertThrows(IllegalArgumentException.class,()->republish(p,events(c,foreign,null)));
            var bootstrap=events(c,null,null);assertEquals(ModernBattleEngine.Phase.FINISHED,bootstrap.world().phase());
            assertThrows(IllegalArgumentException.class,()->republish(p,bootstrap));
            assertEquals(e,republish(p,c).audience(player.scope()).world().result());
        }
    }
    @Test void siegePublicationRejectsAValidButUnrelatedConfigProofAndTheInternalBootstrap()throws Exception {
        try(var f=new ModernSiegeServiceTest.Fixture(0,500,new ModernTimeAttackServiceTest.Journal())) {
            f.active();ModernBattlePublicationTest.stopScheduler(f.service);
            ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.FINISHED);
            var player=f.players.get(0);var p=ModernBattleAudienceTest.published(f.service,player.owner());var c=p.capture();var e=c.modes().siegeResult();assertNotNull(e);
            var s=e.siege();var altered=new ModernSiegeSettlement.Proof(s.roomId(),s.physicalMapId(),s.terrainSha256(),"1".repeat(64),s.attacker(),s.winner(),s.end(),
                    s.durationMillis(),s.respawnMillis(),s.elapsedMillis(),s.gateRevision(),s.towers());
            assertNotEquals(s,altered);
            var foreign=new ModernMatchSettlement.Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants(),null,altered);
            assertThrows(IllegalArgumentException.class,()->republish(p,events(c,null,foreign)));
            var bootstrap=events(c,null,null);assertEquals(ModernBattleEngine.Phase.FINISHED,bootstrap.world().phase());
            assertThrows(IllegalArgumentException.class,()->republish(p,bootstrap));
            assertEquals(e,republish(p,c).audience(player.scope()).world().result());
        }
    }
    @Test void towerDeathNeedsActualMeleeOrRetainedFireReceipt()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();f.tower(0,1);var c=f.engine.serviceObservation();var m=c.modes();
            var forged=c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),new ModernTimeAttack.Snapshot(m.timeAttack().state(),List.of()),
                    m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
            assertThrows(IllegalArgumentException.class,forged::world);
        }
        try(var f=ModernNpcFireStormStructuresTest.time(1,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);var w=f.engine.serviceObservation().world();var a=w.actors().get(0);var s=a.spells();
            assertNotNull(s.structure());assertTrue(w.modes().timeAttack().exchanges().isEmpty());
            var stripped=new ModernBattleObservation.Spells(s.sequence(),s.requestRevision(),s.initialLawful(),s.lawful(),s.histories(),s.receipts(),null);
            var forged=new ModernBattleObservation.Actor(a.pose(),a.vital(),a.resources(),stripped,a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
            assertThrows(IllegalArgumentException.class,()->ModernBattleLifeProofTest.actors(w,ModernBattleLifeProofTest.replace(w.actors(),0,forged)));
            var receipt=s.structure();
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed((wave+1)*2000L);ModernNpcFireStormStructuresTest.cast(f,0,wave+2);
                assertEquals(receipt,f.engine.serviceObservation().world().actors().get(0).spells().structure());
            }
            assertEquals(ModernBattleEngine.Phase.FINISHED,f.engine.serviceObservation().world().phase());
        }
    }
    @Test void modeClockAndSubmillisecondReadDoNotInventNewModeOrWorldRevisions()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();var before=f.engine.serviceObservation().world();f.elapsed(100);var after=f.engine.serviceObservation().world();
            assertEquals(before.revision()+1,after.revision());assertEquals(before.modes().timeAttack().state().revision(),after.modes().timeAttack().state().revision());
            assertEquals(100,after.modes().timeAttack().state().observedMillis());
            assertTrue(after.clock().activeElapsedNanos()>before.clock().activeElapsedNanos());
            f.clock.addAndGet(500_000);var finer=f.engine.serviceObservation().world();
            assertEquals(after.revision(),finer.revision());assertEquals(after.modes(),finer.modes());
            assertTrue(finer.clock().activeElapsedNanos()>after.clock().activeElapsedNanos());
        }
    }
    @Test void towerExchangeCannotClaimDamageGreaterThanItsPossiblePreHitHp()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false,1000)) {
            f.active();f.tower(0,1);var c=f.engine.serviceObservation();var m=c.modes();var e=m.timeAttack().exchanges().get(0);var hit=e.outcome();
            assertTrue(hit.remainingHp()>0);
            var impossible=new ModernPhysicalCombat.Outcome(true,1000,1000,hit.remainingHp(),false,false,false);
            var exchange=new ModernTimeAttack.Exchange(e.entityId(),e.target(),e.sequence(),e.revision(),impossible);
            var forged=c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),new ModernTimeAttack.Snapshot(m.timeAttack().state(),List.of(exchange)),
                    m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
            assertThrows(IllegalArgumentException.class,forged::world);
        }
    }
    @Test void siegeActiveStateCannotClaimToBeWaitingForLoadAgain()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();var c=f.engine.serviceObservation();var s=c.modes().siege().state();
            var waiting=new ModernSiege.State(s.scope(),s.config(),s.attacker(),ModernSiege.Phase.WAITING,s.objectives(),s.revision(),0,0,0,-1);
            assertThrows(IllegalArgumentException.class,()->siege(c,waiting).world());
        }
    }
}
