package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Real room tickets + one engine, synthetic shared geometry and HP tuning.
 * These tests do not claim that the new skill is exposed by wire or UI. */
class ModernNpcFireStormStructuresTest {
    static ModernBattleEngineNpcTest.Fixture quest(int map,long objectiveHp)throws Exception{return quest(map,objectiveHp,100,false);}
    static ModernBattleEngineNpcTest.Fixture quest(int map,long objectiveHp,long playerHp,boolean sharedObjectiveCell)throws Exception {
        var catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L,ModernNpcCatalog.Field.MR,0L),true,0);
        var geometry=ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,playerHp,0,s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500),catalog,false,
                geometry,List.of(ModernQuestObjectivesTest.cell(10,10),ModernQuestObjectivesTest.cell(20,10)),map);
        try {
            var base=ModernQuestProgressTest.plan(f.binding.key(),catalog);var config=ModernQuestObjectivesTest.objectives(map,objectiveHp,false);
            if(sharedObjectiveCell)config=new ModernQuestObjectives.Config(config.targets().stream().map(t->new ModernQuestObjectives.Target(
                    t.lane(),t.stage(),ModernQuestObjectivesTest.cell(11,11),t.graphic(),t.maximumHp())).toList());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),config).withParticipantLives();
            var prepared=f.prepared.stream().map(p->ModernBattleEngineNpcFireStormTest.enable(p,true,false)).toList();
            f.engine=f.build(new ModernNpcBattle.Config(f.binding.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,plan,true).withFireStorm(true),true,prepared);
            return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernNpcFireStormState.Result cast(ModernBattleEngineNpcTest.Fixture f,int who,long sequence) {
        var p=f.players.get(who);var lives=f.view().world().quest().lives();
        return f.engine.fireStormNpc(p.owner(),new ModernNpcFireStormState.Request(p.scope(),f.scope,sequence,lives.get(who).incarnation()));
    }
    static ModernNpcFireStormState.State state(ModernNpcBattle.Frame f,int who){return f.world().npcFireStorms().get(who);}
    static ModernTimeAttackEngineTest.Fixture time(long hp,int humans)throws Exception {
        var f=new ModernTimeAttackEngineTest.Fixture(humans,16,100,true,hp,false,true,true,2);f.random=0;return f;
    }
    static ModernNpcFireStormState.Result cast(ModernTimeAttackEngineTest.Fixture f,int who,long sequence) {
        return ModernNpcFireStormTimeAttackTest.cast(f,who,sequence);
    }
    static ModernNpcFireStormState.State replace(ModernNpcFireStormState.State s,ModernNpcFireStormStructures.Receipt receipt) {
        return new ModernNpcFireStormState.State(s.entityId(),s.castCount(),s.revision(),s.lastSequence(),s.mpSpent(),s.mpCost(),
                s.casterLife(),s.scope(),s.origin(),s.lane(),s.targets(),receipt);
    }
    static ModernNpcBattle.Frame replace(ModernNpcBattle.Frame f,List<ModernNpcFireStormState.State> states) {
        var w=f.world();var copy=new ModernBattleEngine.Snapshot(w.matchId(),w.generation(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),
                w.actors(),w.vitals(),w.result(),w.casts(),w.damages(),w.items(),w.weights(),w.buffs(),w.ammo(),w.regen(),w.honor(),w.fireStorms(),w.race(),
                w.isolatedForces(),w.quest(),w.npcSpells(),w.timeAttack(),w.siege(),w.disintegrations(),w.npcDisintegrations(),states);
        return new ModernNpcBattle.Frame(copy,f.npcs(),f.exchanges());
    }
    @Test void everyLogicalQuestFlowNeedsActualStructureHitAndCanFinishWithoutFakeMeleeReceipt()throws Exception {
        for(int logical:BattleRules.Mode.QUEST_BATTLE.maps())try(var f=quest(logical,1)) {
            f.active();long sequence=0,elapsed=0;boolean symbols=logical==513||logical==527;
            for(int stage=1;stage<=4;stage++) {
                f.elapsed(elapsed);var before=state(f.view(),0).structure();assertEquals(NONE,cast(f,0,++sequence).reason());
                assertEquals(before,state(f.view(),0).structure(),"NPC-only hit retains old proof, never hits the just-unlocked structure");
                assertNull(f.view().world().result());assertEquals(1,ModernQuestEngineTest.living(f,1).stage());
                if(symbols||stage==4) {
                    var target=ModernQuestObjectivesTest.objective(f,0);assertEquals(1,target.hp());f.elapsed(elapsed+=2000);
                    var result=cast(f,0,++sequence);assertEquals(NONE,result.reason());var state=state(result.frame(),0);var proof=state.structure();
                    assertTrue(state.targets().isEmpty());var hit=assertInstanceOf(ModernNpcFireStormStructures.QuestHit.class,proof.hit());
                    assertEquals(target.id(),hit.id());assertEquals(target.bornRevision(),hit.bornRevision());assertEquals(35,hit.outcome().rawDamage());
                    assertEquals(1,hit.outcome().appliedDamage());assertEquals(sequence,proof.castCount());assertEquals(state.revision(),proof.revision());
                    assertTrue(result.frame().world().quest().exchanges().isEmpty());assertEquals(0,result.frame().world().vitals().get(0).attackSequence());
                }
                elapsed+=2000;
            }
            var world=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());
            assertEquals(symbols?8:5,state(f.view(),0).castCount());assertEquals(1000-48*(symbols?8:5),world.vitals().get(0).mp());
            assertEquals(2,world.result().mode());assertEquals(logical,world.result().mapId());assertEquals(world.revision(),world.result().engineRevision());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.LOSS),world.result().participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertThrows(IllegalArgumentException.class,()->ModernQuestProtocol.from(world));
            assertThrows(IllegalArgumentException.class,()->world.quest().validate(world.room().summary().id(),world.matchId(),world.generation(),world.revision(),world.phase(),world.actors(),world.vitals(),world.result()));
            assertEquals(NOT_ACTIVE,cast(f,0,sequence+1).reason());f.elapsed(60_000);assertEquals(world,f.view().world());
        }
    }
    @Test void partialObjectiveFireUsesExactI64HpWithoutAdvancingWaveOrAttackSequence()throws Exception {
        try(var f=quest(513,9007199254740993L)) {
            f.active();assertEquals(NONE,cast(f,0,1).reason());f.elapsed(2000);var target=ModernQuestObjectivesTest.objective(f,0);
            var result=cast(f,0,2);assertEquals(NONE,result.reason());var after=ModernQuestObjectivesTest.objective(f,0);
            assertEquals(9007199254740958L,after.hp());assertEquals(after.revision(),state(result.frame(),0).structure().revision());
            assertEquals(ModernQuestProgress.Phase.OBJECTIVE,result.frame().world().quest().progress().lanes().get(0).phase());
            assertEquals(1,after.id().stage());assertNull(result.frame().world().result());assertEquals(0,result.frame().world().vitals().get(0).attackSequence());
            assertEquals(TOO_FAST,cast(f,0,3).reason());assertEquals(after,ModernQuestObjectivesTest.objective(f,0));
            assertEquals(target.hp()-35,after.hp());assertEquals(904,result.frame().world().vitals().get(0).mp());
        }
    }
    @Test void identicalCoordinatesInOpposingForceDoNotCauseCrossLaneGuardianDamage()throws Exception {
        try(var f=quest(554,1,100,true)) {
            f.active();for(int stage=1;stage<=4;stage++){f.elapsed((stage-1)*2000L);assertEquals(NONE,cast(f,0,stage).reason());assertEquals(NONE,cast(f,1,stage).reason());}
            var left=ModernQuestObjectivesTest.objective(f,0);var right=ModernQuestObjectivesTest.objective(f,1);assertEquals(left.cell(),right.cell());
            f.elapsed(8000);assertEquals(NONE,cast(f,0,5).reason());assertEquals(right,ModernQuestObjectivesTest.objective(f,1));
            assertEquals(0,f.view().world().quest().progress().winningLane());assertNull(state(f.view(),1).structure());
        }
    }
    @Test void questStructureProofSurvivesDeathRespawnAndNextNpcCastInNewLife()throws Exception {
        try(var f=quest(513,1,1,false)) {
            f.active();assertEquals(NONE,cast(f,0,1).reason());f.elapsed(2000);assertEquals(NONE,cast(f,0,2).reason());
            var proof=state(f.view(),0).structure();f.randomValue=19;var dead=ModernQuestLifeTest.die(f,2100);
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            f.elapsed(4000);f.randomValue=0;assertEquals(NONE,cast(f,0,3).reason());
            assertEquals(2,state(f.view(),0).casterLife());assertEquals(proof,state(f.view(),0).structure());assertEquals(1,proof.casterLife());
            assertEquals(144,state(f.view(),0).mpSpent());
        }
    }
    @Test void invalidStructureDrawOrReentrantMeleeCannotCommitManaOrStructureTwice()throws Exception {
        for(boolean nested:List.of(false,true))try(var f=quest(513,1000)) {
            f.active();cast(f,0,1);f.elapsed(2000);var target=ModernQuestObjectivesTest.objective(f,0);var before=f.view();
            if(nested){f.randomValue=19;f.duringRandom=()->assertEquals(ModernNpcBattle.Reason.ATTACKED,ModernQuestObjectivesTest.strike(f,0,1,target.id()).reason());}
            else f.invalidRandom=true;
            assertEquals(nested?STALE_STATE:NUMERIC,cast(f,0,2).reason());assertEquals(state(before,0),state(f.view(),0));
            assertEquals(before.world().vitals().get(0).mp(),f.view().world().vitals().get(0).mp());
            if(!nested)assertEquals(target,ModernQuestObjectivesTest.objective(f,0));
            assertNull(f.view().world().result());
        }
    }
    @Test void towerPartialHitsStartTheTimerOnlyAtActualDestruction()throws Exception {
        try(var f=time(100,1)) {
            f.active();for(int i=0;i<3;i++) {
                f.elapsed(i*2000);var result=cast(f,0,i+1);assertEquals(NONE,result.reason());var after=result.frame();
                var ledger=state(after,0);var hit=assertInstanceOf(ModernNpcFireStormStructures.TowerHit.class,ledger.structure().hit());
                assertEquals(i==2?0:100-35*(i+1),after.world().timeAttack().state().towerHp());
                assertEquals(i==2?4000:-1,after.world().timeAttack().state().startedMillis());assertEquals(35,hit.outcome().rawDamage());
                assertEquals(i==2?30:35,hit.outcome().appliedDamage());assertTrue(after.world().timeAttack().exchanges().isEmpty());
                assertEquals(0,after.world().vitals().get(0).attackSequence());assertTrue(ledger.targets().isEmpty());
            }
            assertEquals(2,f.view().npcs().actors().stream().filter(ModernNpcRoster.Actor::alive).count());
            f.elapsed(23999);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());
            f.elapsed(24000);assertEquals(ModernTimeAttack.End.TIMEOUT,f.view().world().timeAttack().state().end());
        }
    }
    @Test void towerFireProofIsRetainedAcrossAllNpcBatchesIncludingDragonAndFinalSettlement()throws Exception {
        try(var f=time(1,1)) {
            f.active();var initial=cast(f,0,1);assertEquals(NONE,initial.reason());var proof=state(initial.frame(),0).structure();
            assertTrue(state(initial.frame(),0).targets().isEmpty());assertTrue(proof.hit().outcome().death());
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed((wave+1)*2000L);var result=cast(f,0,wave+2);assertEquals(NONE,result.reason());var s=state(result.frame(),0);
                assertEquals(proof,s.structure());assertEquals(2,s.targets().size());assertTrue(s.targets().stream().allMatch(n->n.outcome().death()));
                assertTrue(result.frame().world().timeAttack().exchanges().isEmpty());
            }
            var frame=f.view();assertEquals(ModernTimeAttack.End.CLEARED,frame.world().timeAttack().state().end());
            assertEquals(ModernMatchSettlement.Verdict.WIN,frame.world().timeAttack().outcomes(frame.world()).get(0).verdict());
            assertEquals(1,proof.castCount());assertEquals(5,state(frame,0).castCount());
            assertThrows(IllegalArgumentException.class,()->replace(frame,List.of(replace(state(frame,0),null))));
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttackProtocol.from(frame.world(),null,ModernWorldProtocol.SettlementState.PENDING));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(frame.world(),null));
        }
    }
    @Test void otherCastersTowerDestructionMakesOuterCastStaleWithoutHittingNewWave()throws Exception {
        try(var f=time(1,2)) {
            f.active();var before=f.view();f.duringRandom=()->assertEquals(NONE,cast(f,1,1).reason());
            var result=cast(f,0,1);assertEquals(STALE_STATE,result.reason());assertEquals(state(before,0),state(result.frame(),0));
            assertEquals(1000,result.frame().world().vitals().get(0).mp());assertEquals(952,result.frame().world().vitals().get(1).mp());
            assertTrue(result.frame().npcs().actors().stream().allMatch(ModernNpcRoster.Actor::alive));
            assertEquals(1,state(result.frame(),1).structure().hit().maximumHp());assertEquals(2,result.frame().npcs().actors().size());
        }
    }
    @Test void clockAdvanceDuringTowerRollUsesCommitTimeAndKeepsCapturedTarget()throws Exception {
        try(var f=time(1,1)) {
            f.active();f.duringRandom=()->f.clock.addAndGet(1_000_000);
            assertEquals(NONE,cast(f,0,1).reason());assertEquals(1,f.view().world().timeAttack().state().startedMillis());
            assertTrue(state(f.view(),0).targets().isEmpty());
        }
    }
    @Test void towerCancellationTimeoutOrDepartureDuringRollCannotCreateStartProof()throws Exception {
        for(int kind=0;kind<3;kind++)try(var f=time(1,2)) {
            int k=kind;f.active();var before=f.view();f.duringRandom=()->{if(k==0)f.rooms.releasePreparation(f.ticket);else if(k==1)f.elapsed(60_000);else f.engine.withdrawTimeAttack(f.players.get(0).owner());};
            var result=cast(f,0,1);assertEquals(NOT_ACTIVE,result.reason());assertEquals(state(before,0),state(result.frame(),0));
            assertEquals(1,result.frame().world().timeAttack().state().towerHp());assertEquals(-1,result.frame().world().timeAttack().state().startedMillis());
            assertEquals(1000,result.frame().world().vitals().get(0).mp());
        }
    }
    @Test void forgedStructureScopeCountLifeAndLossCannotBecomeVictoryProof()throws Exception {
        try(var f=time(100,1)) {
            f.active();cast(f,0,1);var frame=f.view();var s=state(frame,0);var e=s.structure();var h=(ModernNpcFireStormStructures.TowerHit)e.hit();
            var foreign=new ModernNpcFireStormStructures.TowerHit(new ModernTimeAttack.TowerId(new ModernNpcRoster.Scope(h.scope().roomId(),h.scope().matchId()+1,h.scope().generation())),h.cell(),h.maximumHp(),h.hpBefore(),h.outcome());
            assertThrows(IllegalArgumentException.class,()->replace(s,new ModernNpcFireStormStructures.Receipt(e.castCount(),e.revision(),e.sequence(),e.casterLife(),e.origin(),foreign)));
            assertThrows(IllegalArgumentException.class,()->replace(s,new ModernNpcFireStormStructures.Receipt(e.castCount()+1,e.revision(),e.sequence()+1,e.casterLife(),e.origin(),h)));
            assertThrows(IllegalArgumentException.class,()->replace(s,new ModernNpcFireStormStructures.Receipt(e.castCount(),e.revision(),e.sequence(),e.casterLife()+1,e.origin(),h)));
            var forged=new ModernNpcFireStormStructures.TowerHit(h.id(),h.cell(),100,100,new ModernFireStorm.Outcome(100,100,0,true));
            var forgedState=replace(s,new ModernNpcFireStormStructures.Receipt(e.castCount(),e.revision(),e.sequence(),e.casterLife(),e.origin(),forged));
            assertThrows(IllegalArgumentException.class,()->replace(frame,List.of(forgedState)));
        }
    }
    @Test void zeroDamageStructureOutcomeDoesNotStartEncounterOrInventHurt()throws Exception {
        try(var f=time(100,1)) {
            f.active();var state=f.view().world().timeAttack().state();var next=ModernTimeAttack.towerHit(state,new ModernFireStorm.Outcome(0,0,100,false),state.revision()+1);
            assertEquals(-1,next.startedMillis());assertEquals(0,next.towerHurtRevision());assertEquals(ModernTimeAttack.Phase.WAITING_FOR_TOWER,next.phase());
        }
        try(var f=quest(513,100)) {
            f.active();cast(f,0,1);var before=ModernQuestObjectivesTest.objective(f,0);var after=before.damaged(new ModernFireStorm.Outcome(0,0,100,false),before.revision()+1);
            assertEquals(100,after.hp());assertEquals(0,after.hurtRevision());assertEquals(0,after.deathRevision());assertEquals(before.action(),after.action());
        }
    }
    @Test void partialQuestSpellThenMeleeKillKeepsBothTypedHistoriesAndNormalVictory()throws Exception {
        try(var f=quest(554,36)) {
            f.active();for(int stage=1;stage<=4;stage++){f.elapsed((stage-1)*2000L);assertEquals(NONE,cast(f,0,stage).reason());}
            f.elapsed(8000);assertEquals(NONE,cast(f,0,5).reason());var target=ModernQuestObjectivesTest.objective(f,0);assertEquals(1,target.hp());
            var proof=state(f.view(),0).structure();f.elapsed(9200);f.randomValue=19;
            var result=ModernQuestObjectivesTest.strike(f,0,1,target.id());assertEquals(ModernNpcBattle.Reason.ATTACKED,result.reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,result.frame().world().phase());assertEquals(proof,state(result.frame(),0).structure());
            assertFalse(proof.hit().outcome().death());assertEquals(1,result.frame().world().quest().exchanges().size());
            assertEquals(1,result.frame().world().vitals().get(0).attackSequence());assertEquals(240,state(result.frame(),0).mpSpent());
        }
    }
    @Test void partialTowerSpellThenMeleeKillStartsWithoutInventingMagicDeath()throws Exception {
        try(var f=time(36,1)) {
            f.active();assertEquals(NONE,cast(f,0,1).reason());var proof=state(f.view(),0).structure();assertEquals(1,f.view().world().timeAttack().state().towerHp());
            f.elapsed(1200);f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());var after=f.view();
            assertEquals(1200,after.world().timeAttack().state().startedMillis());assertEquals(1,after.world().timeAttack().exchanges().size());
            assertEquals(proof,state(after,0).structure());assertFalse(proof.hit().outcome().death());assertEquals(48,state(after,0).mpSpent());
        }
    }
    @Test void departedTowerCasterProofStillSupportsSurvivorClearingEveryWave()throws Exception {
        try(var f=time(1,2)) {
            f.active();assertEquals(NONE,cast(f,0,1).reason());var proof=state(f.view(),0).structure();f.engine.withdrawTimeAttack(f.players.get(0).owner());
            assertEquals(2,f.view().world().timeAttack().state().leader());
            for(int wave=0;wave<f.plan.waves().size();wave++){f.elapsed((wave+1)*2000L);assertEquals(NONE,cast(f,1,wave+1).reason());assertEquals(proof,state(f.view(),0).structure());}
            var world=f.view().world();assertEquals(ModernTimeAttack.End.CLEARED,world.timeAttack().state().end());
            assertEquals(List.of(ModernMatchSettlement.Verdict.LOSS,ModernMatchSettlement.Verdict.WIN),world.timeAttack().outcomes(world).stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertEquals(1,state(f.view(),0).castCount());assertEquals(4,state(f.view(),1).castCount());assertNull(state(f.view(),1).structure());
        }
    }
    @Test void regenerationInsideStructureRollCannotOverwriteManaOrDamageTower()throws Exception {
        try(var f=time(1000,1)) {
            f.active();assertEquals(NONE,cast(f,0,1).reason());f.elapsed(15999);var before=f.view();
            f.duringRandom=()->f.clock.addAndGet(1_000_000);var result=cast(f,0,2);assertEquals(STALE_REGEN,result.reason());
            assertEquals(state(before,0),state(result.frame(),0));assertEquals(965,result.frame().world().timeAttack().state().towerHp());
            assertTrue(result.frame().world().vitals().get(0).mp()>before.world().vitals().get(0).mp());assertEquals(-1,result.frame().world().timeAttack().state().startedMillis());
        }
    }
}
