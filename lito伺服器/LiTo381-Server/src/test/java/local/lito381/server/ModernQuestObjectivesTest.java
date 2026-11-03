package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcBattle.Reason.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ModernQuestObjectivesTest {
    static ModernWorldMap.Cell cell(int x,int y){return ModernQuestProgressTest.cell(x,y);}
    static ModernQuestObjectives.Config objectives(int map,long hp,boolean blocked) {
        var result=new ArrayList<ModernQuestObjectives.Target>();
        boolean symbols=map==513||map==527;
        for(int lane=0;lane<2;lane++)for(int stage=symbols?1:4;stage<=4;stage++)result.add(new ModernQuestObjectives.Target(lane,stage,
                blocked&&lane==0?cell(10,9):cell(11+lane*10,11),lane==0?2665:2669,hp));
        return new ModernQuestObjectives.Config(result);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(int map,long hp,boolean blocked)throws Exception {
        var geometry=ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,ModernNpcServiceTest.HUGE,0,UnaryOperator.identity(),
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),false,geometry,List.of(cell(10,10),cell(20,10)),map);
        try {
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),objectives(map,hp,blocked));
            f.engine=f.build(new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan),true);
            f.randomValue=19;return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernQuestObjectives.Snapshot snapshot(ModernBattleEngineNpcTest.Fixture f){return f.view().world().quest();}
    static ModernQuestObjectives.Actor objective(ModernBattleEngineNpcTest.Fixture f,int lane) {
        return snapshot(f).actors().stream().filter(a->a.id().lane()==lane).findFirst().orElseThrow();
    }
    static ModernNpcBattle.Result strike(ModernBattleEngineNpcTest.Fixture f,int player,long seq,ModernQuestObjectives.Id id) {
        var p=f.players.get(player);return f.engine.attackQuestObjective(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),seq,id);
    }
    static void clear(ModernBattleEngineNpcTest.Fixture f,int lane,int stage,long sequence,long time) {
        f.elapsed(time);var npc=ModernQuestEngineTest.living(f,lane);assertEquals(stage,npc.stage());assertEquals(ATTACKED,f.attack(lane,sequence,npc).reason());
    }
    static ModernBattleEngine.Snapshot finish(int map,int winner)throws Exception {
        try(var f=fixture(map,1,false)) {
            f.active();long sequence=0;
            for(int stage=1;stage<=4;stage++) {
                clear(f,winner,stage,++sequence,stage*2000L);
                if(map==513||map==527||stage==4) {
                    var target=objective(f,winner);f.elapsed(stage*2000L+700);
                    assertEquals(ATTACKED,strike(f,winner,++sequence,target.id()).reason());
                }
            }
            var world=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());return world;
        }
    }
    @Test void finalGuardianRequiresFourRealWavesAndActualObjectiveHitNotNpcDeath()throws Exception {
        try(var f=fixture(554,1,false)) {
            assertTrue(snapshot(f).actors().isEmpty());f.active();
            for(int stage=1;stage<=4;stage++) {
                clear(f,0,stage,stage,stage*1000L);
                assertNull(f.view().world().result());assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());
                if(stage<4)assertTrue(snapshot(f).actors().isEmpty());
            }
            var target=objective(f,0);assertEquals(ModernQuestObjectives.Kind.FINAL_GUARDIAN,target.kind());
            assertEquals(TOO_FAST,strike(f,0,5,target.id()).reason());assertEquals(1,objective(f,0).hp());
            f.elapsed(4700);var won=strike(f,0,6,target.id());assertEquals(ATTACKED,won.reason());
            var world=won.frame().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());assertNotNull(world.result());assertEquals(2,world.result().mode());
            assertEquals(world.revision(),world.result().engineRevision());assertEquals(0,world.quest().progress().winningLane());
            assertEquals(35,world.quest().actors().get(0).action());assertEquals(world.revision(),world.quest().actors().get(0).deathRevision());
            assertTrue(world.vitals().stream().allMatch(v->v.hp()>0)); // losing players need not die
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.LOSS),world.result().participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertEquals(0,world.vitals().get(0).lastTargetEntityId());assertEquals(0,world.vitals().get(0).attackRevision());
            assertEquals(NOT_ACTIVE,strike(f,0,7,target.id()).reason());f.elapsed(60_000);assertEquals(world,f.view().world());
        }
    }
    @Test void symbolMapsRequireEachSymbolAndOnlyFourthDestructionEndsTheMatch()throws Exception {
        for(int map:List.of(513,527))try(var f=fixture(map,1,false)) {
            f.active();long seq=0;
            for(int stage=1;stage<=4;stage++) {
                clear(f,0,stage,++seq,stage*2000L);
                var target=objective(f,0);assertEquals(stage,target.id().stage());
                assertEquals(stage==4?ModernQuestObjectives.Kind.FINAL_SYMBOL:ModernQuestObjectives.Kind.ROOM_SYMBOL,target.kind());
                assertFalse(f.view().npcs().actors().stream().anyMatch(a->a.alive()&&a.lane()==0));
                f.elapsed(stage*2000L+700);assertEquals(ATTACKED,strike(f,0,++seq,target.id()).reason());
                if(stage<4) {
                    assertNull(f.view().world().result());assertEquals(stage+1,ModernQuestEngineTest.living(f,0).stage());
                    assertEquals(1,ModernQuestEngineTest.living(f,1).stage());assertFalse(objective(f,0).alive());
                }
            }
            assertEquals(ModernBattleEngine.Phase.FINISHED,f.view().world().phase());
            assertEquals(map,f.view().world().result().mapId());
        }
    }
    @Test void lockedOtherForceAndOldObjectiveHandlesNeverDamageOrInventAnAttackPose()throws Exception {
        try(var f=fixture(513,1,false)) {
            f.active();assertEquals(TARGET_DEAD,strike(f,0,1,new ModernQuestObjectives.Id(f.scope,4)).reason());assertEquals(0,f.draws.get());
            clear(f,0,1,2,1000);var target=objective(f,0);var before=f.view();
            assertEquals(TARGET_DEAD,strike(f,1,1,target.id()).reason());assertEquals(target,objective(f,0));
            assertEquals(before.world().vitals().get(0),f.view().world().vitals().get(0));
            f.elapsed(1700);assertEquals(ATTACKED,strike(f,0,3,target.id()).reason());f.elapsed(2500);
            assertEquals(TARGET_DEAD,strike(f,0,4,target.id()).reason());assertEquals(2,ModernQuestEngineTest.living(f,0).stage());
            var p=f.players.get(0);var q=f.players.get(1);
            assertThrows(SecurityException.class,()->f.engine.attackQuestObjective(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),5,target.id()));
            assertThrows(SecurityException.class,()->strike(f,0,5,new ModernQuestObjectives.Id(new ModernNpcRoster.Scope(f.scope.roomId(),f.scope.matchId()+1,f.scope.generation()),1)));
        }
    }
    @Test void occupiedObjectiveBirthWaitsUntilPlayerLeavesWithoutBuryingThem()throws Exception {
        try(var f=fixture(554,1,true)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            for(int stage=1;stage<=4;stage++)clear(f,0,stage,stage,stage*1000L);
            assertTrue(snapshot(f).actors().isEmpty());assertEquals(ModernQuestProgress.Phase.OBJECTIVE,snapshot(f).progress().lanes().get(0).phase());
            f.elapsed(5000);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,2,10,9,4).status());assertEquals(cell(10,9),objective(f,0).cell());
            f.elapsed(5200);assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(0,3,10,10,0).status());
        }
    }
    @Test void cancellationDuringObjectiveRandomCannotCommitDeathOrResult()throws Exception {
        try(var f=fixture(554,1,false)) {
            f.active();for(int stage=1;stage<=4;stage++)clear(f,0,stage,stage,stage*1000L);
            var target=objective(f,0);f.elapsed(4700);f.duringRandom=()->f.rooms.releasePreparation(f.ticket);
            assertEquals(NOT_ACTIVE,strike(f,0,5,target.id()).reason());
            assertEquals(ModernBattleEngine.Phase.ABORTED,f.view().world().phase());assertNull(f.view().world().result());assertEquals(target,objective(f,0));
            assertTrue(snapshot(f).exchanges().isEmpty());assertEquals(4,f.view().world().vitals().get(0).attackSequence());
        }
    }
    @Test void objectiveHealthRemainsI64AndUsesSharedGuardianThresholds()throws Exception {
        try(var f=fixture(554,Long.MAX_VALUE,false)) {
            f.active();for(int stage=1;stage<=4;stage++)clear(f,0,stage,stage,stage*1000L);
            var target=objective(f,0);assertEquals(Long.MAX_VALUE,target.hp());f.elapsed(4700);
            var hit=strike(f,0,5,target.id());assertEquals(ATTACKED,hit.reason());var after=objective(f,0);
            var receipt=snapshot(f).exchanges().get(0);assertEquals(Long.MAX_VALUE-receipt.outcome().appliedDamage(),after.hp());
            assertEquals(ModernHonor.action(after.hp(),Long.MAX_VALUE),after.action());assertNull(hit.frame().world().result());
        }
    }
    @Test void objectiveConfigIsExplicitMapQualifiedAndCannotBeSilentlySentAsOldNpcData()throws Exception {
        var base=ModernQuestProgressTest.plan(new ModernWorldBinding.Key(2,554),ModernNpcMeleeTest.catalog());
        assertThrows(IllegalArgumentException.class,()->new ModernQuestProgress.Plan(base.key(),base.waves(),objectives(513,1,false)));
        assertThrows(IllegalArgumentException.class,()->new ModernQuestObjectives.Target(0,4,cell(11,11),1649,1));
        try(var f=fixture(554,1,false)) {
            assertThrows(IllegalArgumentException.class,()->ModernNpcProtocol.Frame.from(f.view(),ModernQuestVisibilityTest.descriptor(true)));
            var world=f.view().world();
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(world,world.revision()+1,ModernQuestVisibilityTest.descriptor(true)));
            var plan=world.quest().progress().plan();
            var encounter=new ModernNpcEncounter(f.binding.key(),f.config.catalog(),Map.of(7,ModernNpcServiceTest.Fixture.blessing(7),42,ModernNpcServiceTest.Fixture.blessing(42)),
                    f.config.laneAnchors(),128,1000,plan.firstWaves(),16,()->false,100,plan);
            var mapping=new ModernWorldService.Mapping(f.binding,cell(10,10),200_000_000,700_000_000,0,false,false,false,false,false,false,false,null,false,null,f.config.laneAnchors(),null,encounter);
            var limits=new ModernWorldService.Limits(2,1,1,2000,20,f.limits);
            assertTrue(ModernMatchDescriptor.of(mapping,limits,true).has(ModernMatchDescriptor.QUEST_OBJECTIVES));
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService(f.rooms,Map.of(f.binding.key(),mapping),limits));
            assertThrows(IllegalArgumentException.class,()->new ModernQuestObjectives.Config(List.of(new ModernQuestObjectives.Target(0,4,cell(10,10),2665,1),new ModernQuestObjectives.Target(1,4,cell(20,11),2669,1)))
                    .qualify(f.view().world().quest().progress().plan(),f.binding,f.config.laneAnchors()));
        }
    }
    @Test void missingDeathReceiptAndCounterfeitWinnerCannotValidateAFinishedWorld()throws Exception {
        for(int lane=0;lane<2;lane++) {
            var world=finish(554,lane);var q=world.quest();var r=world.result();
            var missing=new ModernQuestObjectives.Snapshot(q.progress(),q.actors(),List.of());
            assertThrows(IllegalArgumentException.class,()->missing.validate(world.room().summary().id(),world.matchId(),world.generation(),world.revision(),world.phase(),world.actors(),world.vitals(),r));
            var flipped=new ModernMatchSettlement.Event(r.resultId(),r.matchId(),r.generation(),r.engineRevision(),r.mode(),r.mapId(),r.finishedAtMillis(),
                    r.participants().stream().map(p->new ModernMatchSettlement.Participant(p.entityId(),p.identity(),p.team(),
                        p.verdict()==ModernMatchSettlement.Verdict.WIN?ModernMatchSettlement.Verdict.LOSS:ModernMatchSettlement.Verdict.WIN)).toList());
            assertThrows(IllegalArgumentException.class,()->q.validate(world.room().summary().id(),world.matchId(),world.generation(),world.revision(),world.phase(),world.actors(),world.vitals(),flipped));
            assertThrows(IllegalArgumentException.class,()->ModernQuestObjectives.Snapshot.disabled().validate(world.room().summary().id(),world.matchId(),world.generation(),world.revision(),world.phase(),world.actors(),world.vitals(),r));
        }
    }
    @Test void malformedObjectiveHistoryAndI64OverflowingReceiptFailClosed()throws Exception {
        try(var f=fixture(554,Long.MAX_VALUE,false)) {
            f.active();for(int s=1;s<=4;s++)clear(f,0,s,s,s*1000L);
            var q=snapshot(f);var a=objective(f,0);
            var dead=new ModernQuestObjectives.Actor(a.id(),a.target(),a.kind(),0,a.bornRevision(),a.revision()+1,a.revision()+1,a.revision()+1);
            assertThrows(IllegalArgumentException.class,()->new ModernQuestObjectives.Snapshot(q.progress(),List.of(dead),List.of()));
            var overflow=new ModernQuestObjectives.Exchange(ModernParticipantId.human(7,1),a.id(),5,a.revision()+1,new ModernPhysicalCombat.Outcome(true,1,1,Long.MAX_VALUE,false,false,false));
            assertThrows(IllegalArgumentException.class,()->new ModernQuestObjectives.Snapshot(q.progress(),q.actors(),List.of(overflow)));
            assertThrows(IllegalArgumentException.class,()->new ModernQuestObjectives.Exchange(ModernParticipantId.human(7,1),a.id(),5,a.revision()+1,new ModernPhysicalCombat.Outcome(true,1,1,0,false,false,false)));
        }
    }
    @Test void invalidRandomAndStaleReentrantMovementConsumeNoDamageOrAcceptedAttackClock()throws Exception {
        try(var f=fixture(554,1,false)) {
            f.active();for(int s=1;s<=4;s++)clear(f,0,s,s,s*1000L);
            var a=objective(f,0);f.elapsed(4700);f.invalidRandom=true;
            assertEquals(NUMERIC,strike(f,0,5,a.id()).reason());assertEquals(a,objective(f,0));assertTrue(snapshot(f).exchanges().isEmpty());
            f.invalidRandom=false;f.duringRandom=()->assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            assertEquals(STALE_STATE,strike(f,0,6,a.id()).reason());assertEquals(a,objective(f,0));assertTrue(snapshot(f).exchanges().isEmpty());
            assertEquals(TOO_FAST,strike(f,0,7,a.id()).reason()); // movement, not failed attack, owns this clock
            f.elapsed(4900);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,2,10,9,4).status());
            f.elapsed(5100);assertEquals(ATTACKED,strike(f,0,8,a.id()).reason());assertNotNull(f.view().world().result());
        }
    }
    @Test void timeoutClockRollbackAndCancellationCannotTurnAnObjectiveWaitIntoVictory()throws Exception {
        for(int fault=0;fault<3;fault++)try(var f=fixture(554,1,false)) {
            f.active();for(int s=1;s<=4;s++)clear(f,0,s,s,s*1000L);
            var a=objective(f,0);f.elapsed(4700);int which=fault;
            f.duringRandom=()->{if(which==0)f.elapsed(60_000);else if(which==1)f.clock.decrementAndGet();else f.rooms.releasePreparation(f.ticket);};
            assertEquals(NOT_ACTIVE,strike(f,0,5,a.id()).reason());var world=f.view().world();
            assertEquals(ModernBattleEngine.Phase.ABORTED,world.phase());assertNull(world.result());assertEquals(a,objective(f,0));
            assertEquals(4,world.vitals().get(0).attackSequence());assertTrue(world.quest().exchanges().isEmpty());
        }
    }
}
