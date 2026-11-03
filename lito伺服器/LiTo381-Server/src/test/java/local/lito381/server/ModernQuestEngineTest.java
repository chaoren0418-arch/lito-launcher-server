package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ModernQuestEngineTest {
    static ModernBattleEngineNpcTest.Fixture fixture(int capacity,boolean blockedSecond)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(2,0,false,100,0,UnaryOperator.identity(),
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),false);
        try {
            var plan=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            if(blockedSecond) {
                var waves=new ArrayList<>(plan.waves());var t=plan.wave(0,2).spawns().get(0).template();
                waves.set(1,new ModernQuestProgress.Wave(0,2,List.of(new ModernNpcRoster.Spawn(t,0,2,ModernQuestProgressTest.cell(10,9),2))));
                plan=new ModernQuestProgress.Plan(f.binding.key(),waves);
            }
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),capacity,1000,plan));
            f.randomValue=19;return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernQuestProgress.State progress(ModernBattleEngineNpcTest.Fixture f) {
        var p=f.players.get(0);return f.engine.questProgress(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation());
    }
    static ModernNpcRoster.Actor living(ModernBattleEngineNpcTest.Fixture f,int lane) {
        return f.view().npcs().actors().stream().filter(a->a.alive()&&a.lane()==lane).findFirst().orElseThrow();
    }
    @Test void sharedStagedBirthsNeedActualEgressQualificationNotJustStandingCells()throws Exception {
        var map=ModernQuestMapQualificationTest.shared();var candidates=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,8);
        var camps=List.of(candidates.get(0),candidates.get(2));var roster=new ArrayList<ModernRooms.Member>();
        for(int i=0;i<2;i++)roster.add(new ModernRooms.Member(ModernParticipantId.human(1,i+1L),"qualification","qualification",80,1,0,i,true));
        var failure=assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(map,roster,camps,java.util.Set.of(candidates.get(1),candidates.get(3)),()->false));
        assertEquals(ModernWorldMap.Failure.INVALID_SPAWNS,failure.failure());
        var births=List.of(candidates.get(1),candidates.get(7));
        assertEquals(List.of(new ModernWorldMap.Cell(32778,32822),new ModernWorldMap.Cell(32780,32822)),births);
        var obstacles=java.util.Set.copyOf(births);
        assertEquals(camps,ModernWorldSpawns.allocate(map,roster,camps,obstacles,()->false).positions());
        for(int i=2;i<4;i++)roster.add(new ModernRooms.Member(ModernParticipantId.human(1,i+1L),"qualification","qualification",80,1,0,i%2,true));
        var result=ModernWorldSpawns.allocate(map,roster,camps,obstacles,()->false);
        ModernWorldSpawns.qualify(map,roster,result.positions(),obstacles,()->false);
        var binding=new ModernWorldBinding(2,554,map);var catalog=ModernQuestProgressTest.encounterCatalog();
        ModernQuestProgressTest.encounterPlan(binding.key(),catalog,births).qualify(binding,catalog,camps,8);
    }
    @Test void actualSharedCombatKillsAdvanceFourWavesButNeverInventFinalTowerVictory()throws Exception {
        try(var f=fixture(128,false)) {
            assertTrue(f.view().npcs().actors().isEmpty());f.ack(0);assertTrue(f.view().npcs().actors().isEmpty());
            f.ack(1);assertEquals(2,f.view().npcs().spawned());
            assertThrows(IllegalStateException.class,()->f.spawn(0,12,10));
            long lastSerial=0;
            for(int stage=1;stage<=4;stage++) {
                f.elapsed(stage*800);var npc=living(f,0);assertEquals(stage,npc.stage());assertTrue(npc.id().serial()>lastSerial);lastSerial=npc.id().serial();
                var attack=f.attack(0,stage,npc);assertEquals(ModernNpcBattle.Reason.ATTACKED,attack.reason());
                int clearedStage=stage;assertTrue(attack.frame().exchanges().stream().anyMatch(e->e.requestSequence()==clearedStage&&e.outcome().death()));
                assertEquals(1,progress(f).lanes().get(1).stage());assertEquals(1,living(f,1).stage());
                assertNull(f.view().world().result());assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());
            }
            var waiting=progress(f);assertEquals(4,waiting.lanes().get(0).stage());assertEquals(ModernQuestProgress.Phase.OBJECTIVE,waiting.lanes().get(0).phase());
            assertEquals(-1,waiting.winningLane());long spawned=f.view().npcs().spawned();
            for(int i=0;i<10;i++)assertEquals(spawned,f.view().npcs().spawned());
            f.elapsed(60_000);assertEquals(ModernBattleEngine.Phase.ABORTED,f.view().world().phase());assertNull(f.view().world().result());
            assertEquals(ModernQuestProgress.Status.ABORTED,progress(f).status());assertEquals(-1,progress(f).winningLane());
        }
    }
    @Test void futureOccupiedCellDelaysWholeWaveWithoutAbortingOrConsumingSerials()throws Exception {
        try(var f=fixture(128,true)) {
            f.active();f.elapsed(100);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            f.elapsed(300);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(0,1,living(f,0)).reason());
            long spawned=f.view().npcs().spawned();assertEquals(2,spawned);
            for(int i=0;i<5;i++){assertEquals(ModernQuestProgress.Phase.SPAWN_PENDING,progress(f).lanes().get(0).phase());assertEquals(spawned,f.view().npcs().spawned());}
            f.elapsed(1000);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,2,10,9,4).status());
            assertEquals(2,living(f,0).stage());assertEquals(spawned+1,f.view().npcs().spawned());assertNull(f.view().world().result());
        }
    }
    @Test void residentCorpsesDelayNextWaveUntilTheirOwnRetirementWithoutDroppingThemEarly()throws Exception {
        try(var f=fixture(2,false)) {
            f.active();assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(0,1,living(f,0)).reason());
            assertEquals(ModernQuestProgress.Phase.SPAWN_PENDING,progress(f).lanes().get(0).phase());assertEquals(2,f.view().npcs().spawned());
            f.elapsed(999);assertEquals(2,f.view().npcs().spawned());
            f.elapsed(1000);assertEquals(3,f.view().npcs().spawned());assertEquals(2,living(f,0).stage());
        }
    }
    @Test void cancellationDuringRandomCannotCommitDeathOrReleaseAFutureWave()throws Exception {
        try(var f=fixture(128,false)) {
            f.active();var npc=living(f,0);f.duringRandom=()->f.rooms.releasePreparation(f.ticket);
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.attack(0,1,npc).reason());
            assertEquals(ModernQuestProgress.Status.ABORTED,progress(f).status());assertEquals(1,progress(f).lanes().get(0).stage());
            assertEquals(2,f.view().npcs().spawned());assertTrue(f.view().npcs().closed());assertNull(f.view().world().result());
        }
    }
}
