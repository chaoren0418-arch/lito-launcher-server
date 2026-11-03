package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Passive GM dummies fill real room seats; they never mint owner/SQL authority. */
class ModernQuestBotTest {
    static ModernBattleEngineNpcTest.Fixture fixture(int count,int logical)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(1,count,true,100,500_000_000L,UnaryOperator.identity(),
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),false,
                ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false),
                java.util.stream.IntStream.range(0,1+count).mapToObj(i->ModernQuestLifeTest.cell(10+10*i,10)).toList(),logical);
        try {
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(logical,1,false)).withParticipantLives();
            f.engine=f.build(new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan),true);
            f.randomValue=19;return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    @Test void oneHumanAndReadyGmBotsCanStartClearAllQuestFlowsAndReturnWithoutFakeOwners()throws Exception {
        for(int logical:List.of(554,513,527))try(var f=fixture(3,logical)) {
            var room=f.room();assertEquals(4,room.members().size());assertEquals(1,f.ticket.participants().size());
            assertEquals(2,room.members().stream().filter(m->m.team()==0).count());
            for(var bot:f.ticket.bots()){assertTrue(bot.ready());assertEquals("server-7",bot.sourceName());assertFalse(bot.identity().isHuman());}
            assertEquals(ModernBattleEngine.Phase.LOADING,f.view().world().phase());
            assertTrue(f.view().world().actors().stream().filter(a->!a.member().identity().isHuman()).allMatch(ModernBattleEngine.Actor::loaded));
            f.ack(0);assertEquals(ModernBattleEngine.Phase.COUNTDOWN,f.view().world().phase());
            f.elapsed(0);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());
            long sequence=0,elapsed=0;
            for(int stage=1;stage<=4;stage++) {
                ModernQuestObjectivesTest.clear(f,0,stage,++sequence,elapsed+=1000);
                if(stage==4||logical!=554) {
                    f.elapsed(elapsed+=1000);var target=ModernQuestObjectivesTest.objective(f,0);
                    assertEquals(ModernNpcBattle.Reason.ATTACKED,ModernQuestObjectivesTest.strike(f,0,++sequence,target.id()).reason());
                }
            }
            var world=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());
            assertEquals(4,world.result().participants().size());assertTrue(world.result().hasBots());
            for(var member:world.result().participants())assertEquals(member.team()==0?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS,member.verdict());
            assertTrue(world.vitals().stream().skip(1).allMatch(v->v.attackSequence()==0));
            assertTrue(f.rooms.releasePreparation(f.ticket));assertFalse(f.room().members().get(0).ready());
            assertTrue(f.room().members().stream().skip(1).allMatch(ModernRooms.Member::ready));
        }
    }
    @Test void deadPassiveBotKeepsAnExactLifeLedgerAndCannotBeResurrectedByHumanProof()throws Exception {
        try(var f=fixture(1,554)) {
            f.active();var npc=ModernQuestEngineTest.living(f,1);var bot=f.view().world().actors().get(1);
            // Advance only this lane's production NPC planner until the dummy dies.
            for(int step=1;step<=30&&f.view().world().vitals().get(1).hp()>0;step++) {
                f.elapsed(step*1000L);f.step(npc);
            }
            var dead=f.view();var life=dead.world().quest().lives().get(1);assertTrue(life.dead());
            assertEquals(0,dead.world().vitals().get(1).hp());assertEquals(1,life.incarnation());
            assertTrue(dead.exchanges().stream().anyMatch(e->e.participant().equals(bot.member().identity())&&e.outcome().death()));
            assertNotEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,life).reason());
            f.elapsed(40_000);var after=f.view();assertEquals(life,after.world().quest().lives().get(1));
            assertEquals(0,after.world().vitals().get(1).hp());assertNull(after.world().result());
            assertEquals(40,after.world().regen().get(1).damageLoss());
        }
    }
    @Test void isolatedBotOnlyForceCanBePlacedButAnEntireBotOnlyRoomStillCannot()throws Exception {
        try(var f=fixture(3,554)) {
            var q=f.view().world().quest().progress().plan();var camps=f.config.laneAnchors();
            var npc=new ModernNpcEncounter(f.binding.key(),f.config.catalog(),
                    Map.of(7,f.config.players().values().iterator().next()),camps,128,1000,q.firstWaves(),16,()->false,100,q);
            var mapping=new ModernWorldService.Mapping(f.binding,camps.get(0),200_000_000,700_000_000,0,
                    false,true,true,true,false,false,true,null,false,null,camps,null,npc);
            var roster=f.room().members();var placed=ModernQuestSpace.allocate(mapping,roster,Set.of(),()->false);
            ModernQuestSpace.qualify(f.map,roster,placed,Set.of(),()->false,true);
            assertEquals(4,placed.size());
            for(int i=0;i<placed.size();i++) {
                int lane=roster.get(i).team();var cell=placed.get(i);
                assertTrue(f.map.standing(cell));
                assertTrue(q.waves().stream().filter(w->w.lane()==lane).flatMap(w->w.spawns().stream()).noneMatch(s->s.cell().equals(cell)));
                assertTrue(q.objectives().targets().stream().filter(t->t.lane()==lane).noneMatch(t->t.cell().equals(cell)));
            }
            var bots=roster.stream().filter(m->!m.identity().isHuman()).toList();
            var cells=placed.subList(1,placed.size());
            assertThrows(ModernWorldMap.MapException.class,()->ModernQuestSpace.allocate(mapping,bots,Set.of(),()->false));
            assertThrows(ModernWorldMap.MapException.class,()->ModernQuestSpace.qualify(f.map,bots,cells,Set.of(),()->false,true));
            assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(f.map,bots,List.of(camps.get(0)),Set.of(),()->false));
            assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(f.map,bots,cells,Set.of(),()->false));
            var force=List.of(bots.get(0));var one=List.of(cells.get(0));
            assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualifyQuestForce(f.map,force,one,Set.copyOf(one),()->false));
            assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocateQuestForce(f.map,force,List.of(camps.get(1)),Set.of(),()->true));
            assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualifyQuestForce(f.map,List.of(bots.get(0),bots.get(1)),List.of(one.get(0),one.get(0)),Set.of(),()->false));
        }
    }
}
