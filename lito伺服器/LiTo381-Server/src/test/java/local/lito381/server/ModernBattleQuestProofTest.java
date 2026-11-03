package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ModernBattleQuestProofTest {
    static ModernBattleObservation.Capture quest(ModernBattleObservation.Capture c,ModernBattleObservation.Quest q) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),q,m.timeAttack(),m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static ModernBattleObservation.Capture finish(ModernBattleEngineNpcTest.Fixture f,int map,int winner) {
        f.active();long sequence=0;
        for(int stage=1;stage<=4;stage++) {
            ModernQuestObjectivesTest.clear(f,winner,stage,++sequence,stage*2000L);
            if(map==513||map==527||stage==4) {
                var target=ModernQuestObjectivesTest.objective(f,winner);f.elapsed(stage*2000L+700);
                assertEquals(ModernNpcBattle.Reason.ATTACKED,ModernQuestObjectivesTest.strike(f,winner,++sequence,target.id()).reason());
            }
        }
        return f.engine.serviceObservation();
    }
    @Test void resultCannotSwapWinnerAndLoserWhileKeepingTheDestroyedGuardian()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(554,1,false)) {
            var c=finish(f,554,0);var e=c.result();assertNotNull(e);
            var rows=e.participants().stream().map(p->new ModernMatchSettlement.Participant(p.entityId(),p.identity(),p.team(),
                    p.verdict()==ModernMatchSettlement.Verdict.WIN?ModernMatchSettlement.Verdict.LOSS:ModernMatchSettlement.Verdict.WIN)).toList();
            var forged=new ModernMatchSettlement.Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),rows);
            assertThrows(IllegalArgumentException.class,()->ModernBattleHonorProofTest.result(c,forged).world());
        }
    }
    @Test void finishedWorldCannotLoseItsActualFinalObjective()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(554,1,false)) {
            var c=finish(f,554,0);var q=c.modes().quest();
            assertThrows(IllegalArgumentException.class,()->quest(c,new ModernBattleObservation.Quest(q.progress(),List.of(),q.exchanges(),q.lives())).world());
        }
    }
    @Test void laneLivingIdsMustBelongToTheActualNpcRosterEvenWithoutObjectives()throws Exception {
        try(var f=ModernQuestEngineTest.fixture(128,false)) {
            f.active();var c=f.engine.serviceObservation();var q=c.modes().quest();var p=q.progress();var lane=p.lanes().get(0);
            var fake=new ModernQuestProgress.Lane(lane.stage(),lane.phase(),Set.of(new ModernNpcRoster.Id(p.scope(),999)));
            var changed=new ModernQuestProgress.State(p.scope(),p.plan(),p.revision(),p.status(),ModernBattleLifeProofTest.replace(p.lanes(),0,fake),p.winningLane());
            assertThrows(IllegalArgumentException.class,()->quest(c,new ModernBattleObservation.Quest(changed,q.objectives(),q.exchanges(),q.lives())).world());
        }
    }
    @Test void objectiveReceiptCannotBeAttributedToTheOpposingSource()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(554,1,false)) {
            var c=finish(f,554,0);var q=c.modes().quest();var e=q.exchanges().get(0);
            var foreign=new ModernQuestObjectives.Exchange(c.actors().get(1).pose().member().identity(),e.target(),e.requestSequence(),e.revision(),e.outcome());
            assertThrows(IllegalArgumentException.class,()->quest(c,new ModernBattleObservation.Quest(q.progress(),q.objectives(),List.of(foreign),q.lives())).world());
        }
    }
    @Test void everyConfiguredQuestFlowCanFinishForEitherForceWithoutKillingOpposingPlayers()throws Exception {
        for(int map:BattleRules.Mode.QUEST_BATTLE.maps())for(int winner=0;winner<2;winner++)try(var f=ModernQuestObjectivesTest.fixture(map,1,false)) {
            var c=finish(f,map,winner);var w=c.world();assertEquals(winner,w.modes().quest().progress().winningLane());
            assertTrue(w.actors().stream().allMatch(a->a.vital().values().hp()>0));
            for(var p:f.players)assertEquals(w,c.forOwner(p.scope()).world());
        }
    }
    @Test void waveOnlyQuestCanReachItsUnimplementedGuardianBoundaryButCannotClaimVictory()throws Exception {
        try(var f=ModernQuestEngineTest.fixture(128,false)) {
            f.active();
            for(int stage=1;stage<=4;stage++) {
                ModernQuestObjectivesTest.clear(f,0,stage,stage,stage*1000L);
                var w=f.engine.serviceObservation().world();assertNull(w.result());assertEquals(ModernBattleEngine.Phase.ACTIVE,w.phase());
            }
            var c=f.engine.serviceObservation();assertNull(c.modes().quest().progress().plan().objectives());
            assertEquals(ModernQuestProgress.Phase.OBJECTIVE,c.world().modes().quest().progress().lanes().get(0).phase());
            f.elapsed(60_000);assertEquals(ModernBattleEngine.Phase.ABORTED,f.engine.serviceObservation().world().phase());
        }
    }
    @Test void occupiedBirthCanDelayTheObjectiveWithoutAFalseTerminalOrMissingTargetError()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(554,1,true)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            for(int stage=1;stage<=4;stage++)ModernQuestObjectivesTest.clear(f,0,stage,stage,stage*1000L);
            var waiting=f.engine.serviceObservation().world();assertTrue(waiting.modes().quest().objectives().isEmpty());
            assertEquals(ModernQuestProgress.Phase.OBJECTIVE,waiting.modes().quest().progress().lanes().get(0).phase());
            f.elapsed(5000);f.move(0,2,10,9,4);
            assertEquals(1,f.engine.serviceObservation().world().modes().quest().objectives().size());
        }
    }
    @Test void oneRealFireCastMayAdvanceTheModeCounterPastTheEngineRevision()throws Exception {
        var catalog=ModernNpcMeleeTest.catalog(java.util.Map.of(ModernNpcCatalog.Field.HP,1L,ModernNpcCatalog.Field.MR,0L),true,0);
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500),catalog,false)) {
            var base=ModernQuestProgressTest.plan(f.binding.key(),catalog);
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false),true);
            var waves=new ArrayList<>(plan.waves());var template=waves.get(0).spawns().get(0).template();
            // Separate rays keep NPCs from blocking one another's corner/LOS gates.
            var cells=List.of(ModernQuestObjectivesTest.cell(13,10),ModernQuestObjectivesTest.cell(13,13),ModernQuestObjectivesTest.cell(10,13),ModernQuestObjectivesTest.cell(7,13),
                    ModernQuestObjectivesTest.cell(7,10),ModernQuestObjectivesTest.cell(7,7),ModernQuestObjectivesTest.cell(10,7),ModernQuestObjectivesTest.cell(13,7));
            waves.set(0,new ModernQuestProgress.Wave(0,1,cells.stream().map(c->new ModernNpcRoster.Spawn(template,0,1,c,2)).toList()));
            var dense=new ModernQuestProgress.Plan(plan.key(),waves,plan.objectives(),true);
            var config=new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,dense,true).withFireStorm(true);
            f.engine=f.build(config,true,f.prepared.stream().map(p->ModernBattleEngineNpcFireStormTest.enable(p,true,false)).toList());
            f.randomValue=0;f.active();var result=ModernNpcFireStormStructuresTest.cast(f,0,1);assertEquals(ModernNpcFireStormState.Reason.NONE,result.reason());
            var c=result.observation();var w=c.world();
            assertEquals(8,w.actors().get(0).spells().receipts().get(ModernUseHistory.Lane.NPC_FIRE_STORM).impact().targets().size());
            assertEquals(2,w.modes().quest().progress().lanes().get(0).stage());
            assertTrue(w.modes().quest().progress().revision()>w.revision());
            assertEquals(w,c.forOwner(f.players.get(0).scope()).world());
        }
    }
    @Test void realSymbolFireReceiptSurvivesPlayerDeathRevivalAndALaterNpcOnlyCast()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1,1,false)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);
            var old=f.engine.serviceObservation().world().actors().get(0).spells().structure();assertNotNull(old);
            f.randomValue=19;var dead=ModernQuestLifeTest.die(f,2100);
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            f.elapsed(4000);f.randomValue=0;ModernNpcFireStormStructuresTest.cast(f,0,3);
            var w=f.engine.serviceObservation().world();assertEquals(old,w.actors().get(0).spells().structure());assertEquals(1,w.actors().get(0).resources().life());
        }
    }
    @Test void fireOnlyTerminalNeedsItsRetainedCorrectlyTypedStructureReceipt()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(554,1)) {
            f.active();for(int stage=1;stage<=4;stage++){f.elapsed((stage-1)*2000L);ModernNpcFireStormStructuresTest.cast(f,0,stage);}
            f.elapsed(8000);ModernNpcFireStormStructuresTest.cast(f,0,5);var w=f.engine.serviceObservation().world();assertNotNull(w.result());
            var a=w.actors().get(0);var s=a.spells();var proof=s.structure();
            var wrong=new ModernBattleObservation.Structure(proof.casts(),proof.impact(),ModernQuestObjectives.Kind.FINAL_SYMBOL);
            for(var candidate:java.util.Arrays.asList(null,wrong)) {
                var spells=new ModernBattleObservation.Spells(s.sequence(),s.requestRevision(),s.initialLawful(),s.lawful(),s.histories(),s.receipts(),candidate);
                var changed=new ModernBattleObservation.Actor(a.pose(),a.vital(),a.resources(),spells,a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
                assertThrows(IllegalArgumentException.class,()->ModernBattleLifeProofTest.actors(w,ModernBattleLifeProofTest.replace(w.actors(),0,changed)));
            }
        }
    }
}
