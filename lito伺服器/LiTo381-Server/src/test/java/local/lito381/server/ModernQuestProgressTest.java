package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernQuestProgressTest {
    static ModernWorldMap.Cell cell(int x,int y){return ModernBattleEngineNpcTest.cell(x,y);}
    /** Test-only one-hit forms. Both graphics are preflighted before wave one;
     * later waves really use the second form instead of merely listing it. */
    static ModernNpcCatalog.Catalog encounterCatalog() {
        var original=ModernNpcProtocolTest.futureCatalog();var rows=new java.util.HashMap<Integer,ModernNpcCatalog.Npc>();
        for(var row:original.npcs().values()) {
            var facts=new java.util.HashMap<>(row.facts().values());facts.put(ModernNpcCatalog.Field.HP,1L);
            rows.put(row.key().npcId(),new ModernNpcCatalog.Npc(row.key(),row.name(),row.nameId(),row.implementation(),row.large(),new ModernNpcCatalog.Facts(facts),false));
        }
        return new ModernNpcCatalog.Catalog(original.sourceId(),original.database(),rows,original.growth());
    }
    static ModernQuestProgress.Plan encounterPlan(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,List<ModernWorldMap.Cell> births) {
        var waves=new ArrayList<ModernQuestProgress.Wave>();
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++) {
            var template=catalog.npcs().get(ModernNpcMeleeTest.NPC+(stage-1)%2).rosterTemplate();
            waves.add(new ModernQuestProgress.Wave(lane,stage,List.of(new ModernNpcRoster.Spawn(template,lane,stage,births.get(lane),2))));
        }
        return new ModernQuestProgress.Plan(key,waves);
    }
    static ModernQuestProgress.Plan plan(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog) {
        var template=catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();var waves=new ArrayList<ModernQuestProgress.Wave>();
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++)waves.add(new ModernQuestProgress.Wave(lane,stage,
                List.of(new ModernNpcRoster.Spawn(template,lane,stage,cell(11+lane*10,10),2))));
        return new ModernQuestProgress.Plan(key,waves);
    }
    @Test void retainedMapSpecificFlowMatchesEveryQuestMapButNeverImportsTheSiegeBoundaryExperiment() {
        for(int map:BattleRules.Mode.QUEST_BATTLE.maps()) {
            var plan=plan(new ModernWorldBinding.Key(2,map),ModernNpcMeleeTest.catalog());
            assertEquals(BattleQuestEncounter.usesPerRoomSymbols(map),plan.flow()==ModernQuestProgress.Flow.ROOM_SYMBOLS);
        }
        assertThrows(IllegalArgumentException.class,()->plan(new ModernWorldBinding.Key(2,573),ModernNpcMeleeTest.catalog()));
        assertThrows(IllegalArgumentException.class,()->plan(new ModernWorldBinding.Key(3,573),ModernNpcMeleeTest.catalog()));
    }
    @Test void fourWavesAreIndependentAndOnlyQualifiedFinalObjectiveEventCanProduceAWinner()throws Exception {
        var catalog=ModernNpcMeleeTest.catalog();var map=ModernWorldMap.fromSectorBytes(554,
                Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var scope=ModernNpcRosterTest.SCOPE;
        for(int logical:List.of(554,513,527))try(var roster=new ModernNpcRoster(scope,map,List.of(cell(10,10),cell(20,10)),128,1000)) {
            var plan=plan(new ModernWorldBinding.Key(2,logical),catalog);var state=ModernQuestProgress.initial(scope,plan);
            for(int stage=1;stage<=4;stage++) {
                var step=ModernQuestProgress.spawn(state,0,roster.snapshot());
                var born=roster.spawn(scope,step.batch(),Set.of());state=step.after();
                var fighting=state;
                assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.objectiveDestroyed(fighting,scope,0,fighting.lanes().get(0).stage()));
                state=ModernQuestProgress.defeated(state,born.get(0));roster.damage(scope,born.get(0).id(),1,Long.MAX_VALUE);
                assertEquals(ModernQuestProgress.Status.RUNNING,state.status());assertEquals(-1,state.winningLane());
                assertEquals(1,state.lanes().get(1).stage());assertEquals(ModernQuestProgress.Phase.SPAWN_PENDING,state.lanes().get(1).phase());
                if(stage<4&&plan.flow()==ModernQuestProgress.Flow.ROOM_SYMBOLS) {
                    assertEquals(ModernQuestProgress.Phase.OBJECTIVE,state.lanes().get(0).phase());
                    var waiting=state;assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.spawn(waiting,0,roster.snapshot()));
                    state=ModernQuestProgress.objectiveDestroyed(state,scope,0,stage);
                }
            }
            assertEquals(ModernQuestProgress.Phase.OBJECTIVE,state.lanes().get(0).phase());
            var finalWaiting=state;
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.objectiveDestroyed(finalWaiting,new ModernNpcRoster.Scope(scope.roomId(),scope.matchId(),scope.generation()+1),0,4));
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.objectiveDestroyed(finalWaiting,scope,0,3));
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.objectiveDestroyed(finalWaiting,scope,1,4));
            var won=ModernQuestProgress.objectiveDestroyed(state,scope,0,4);assertEquals(0,won.winningLane());
            assertEquals(ModernQuestProgress.Status.FINISHED,won.status());assertSame(won,ModernQuestProgress.abort(won));
            assertSame(won,ModernQuestProgress.objectiveDestroyed(won,scope,0,4));
            assertSame(won,ModernQuestProgress.objectiveDestroyed(won,scope,1,4));
        }
    }
    @Test void plansRejectMissingReorderedWavesDuplicatesForeignTemplatesAndInsufficientResidentCapacity()throws Exception {
        var catalog=ModernNpcMeleeTest.catalog();var key=new ModernWorldBinding.Key(2,554);var plan=plan(key,catalog);
        assertThrows(UnsupportedOperationException.class,()->plan.waves().clear());
        assertThrows(IllegalArgumentException.class,()->new ModernQuestProgress.Plan(key,plan.waves().subList(0,7)));
        var swapped=new ArrayList<>(plan.waves());java.util.Collections.swap(swapped,0,1);
        assertThrows(IllegalArgumentException.class,()->new ModernQuestProgress.Plan(key,swapped));
        var wave=plan.wave(0,1);assertThrows(IllegalArgumentException.class,()->new ModernQuestProgress.Wave(0,1,List.of(wave.spawns().get(0),wave.spawns().get(0))));
        var map=ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var binding=new ModernWorldBinding(2,554,map);var anchors=List.of(cell(10,10),cell(20,10));
        plan.qualify(binding,catalog,anchors,2);
        assertThrows(IllegalArgumentException.class,()->plan.qualify(binding,catalog,anchors,1));
        assertThrows(IllegalArgumentException.class,()->plan.qualify(binding,ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),anchors,128));
        assertThrows(IllegalArgumentException.class,()->plan.qualify(binding,catalog,List.of(cell(11,10),cell(20,10)),128));
        var blocked=new ArrayList<>(plan.waves());var t=wave.spawns().get(0).template();
        blocked.set(3,new ModernQuestProgress.Wave(0,4,List.of(new ModernNpcRoster.Spawn(t,0,4,cell(200,200),0))));
        assertThrows(IllegalArgumentException.class,()->new ModernQuestProgress.Plan(key,blocked).qualify(binding,catalog,anchors,128));
    }
    @Test void staleIdentityLifeDuplicateDeathsAndAbortCannotAdvanceOrCreateVictory()throws Exception {
        var catalog=ModernNpcMeleeTest.catalog();var map=ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var scope=ModernNpcRosterTest.SCOPE;var state=ModernQuestProgress.initial(scope,plan(new ModernWorldBinding.Key(2,554),catalog));
        try(var roster=new ModernNpcRoster(scope,map,List.of(cell(10,10),cell(20,10)),128,1000)) {
            var step=ModernQuestProgress.spawn(state,0,roster.snapshot());var born=roster.spawn(scope,step.batch(),Set.of()).get(0);state=step.after();
            var fighting=state;
            var foreign=new ModernNpcRoster.Actor(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(scope.roomId(),scope.matchId(),scope.generation()+1),born.id().serial()),born.template(),born.lane(),born.stage(),born.cell(),born.heading(),born.hp(),born.mp(),1,born.action(),born.bornRevision(),born.changedRevision(),0,born.nextMoveMillis(),0,null);
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.defeated(fighting,foreign));
            var revived=new ModernNpcRoster.Actor(born.id(),born.template(),born.lane(),born.stage(),born.cell(),born.heading(),born.hp(),born.mp(),2,born.action(),born.bornRevision(),born.changedRevision(),0,born.nextMoveMillis(),0,null);
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.defeated(fighting,revived));
            var cleared=ModernQuestProgress.defeated(state,born);
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.defeated(cleared,born));
            var aborted=ModernQuestProgress.abort(cleared);assertEquals(-1,aborted.winningLane());assertSame(aborted,ModernQuestProgress.abort(aborted));
            assertThrows(IllegalArgumentException.class,()->ModernQuestProgress.spawn(aborted,0,roster.snapshot()));
            assertSame(aborted,ModernQuestProgress.objectiveDestroyed(aborted,scope,0,4));
            assertEquals(ModernQuestProgress.Status.ABORTED,aborted.status());
        }
    }
}
