package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernQuestLife.Reason.*;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ModernQuestLifeTest {
    static ModernWorldMap.Cell cell(int x,int y){return ModernQuestObjectivesTest.cell(x,y);}
    static ModernBattleEngineNpcTest.Fixture fixture(int humans)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(humans,0,true,1,0,UnaryOperator.identity(),
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),false);
        try {
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();
            f.engine=f.build(new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan),true);
            return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernQuestLife.State life(ModernNpcBattle.Frame f,int player){return f.world().quest().lives().get(player);}
    static ModernQuestLife.Result confirm(ModernBattleEngineNpcTest.Fixture f,int player,ModernQuestLife.State life){
        var p=f.players.get(player);return f.engine.confirmQuestDeath(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),life.incarnation(),life.deathRevision());
    }
    static ModernNpcBattle.Frame die(ModernBattleEngineNpcTest.Fixture f,long elapsed){
        f.elapsed(elapsed);var hit=f.step(ModernQuestEngineTest.living(f,0));assertEquals(ModernNpcBattle.Reason.ATTACKED,hit.reason());
        assertEquals(0,hit.frame().world().vitals().get(0).hp());assertTrue(life(hit.frame(),0).dead());return hit.frame();
    }
    @Test void actualNpcDeathNeedsExactConfirmationAndReturnsToQualifiedSpawnWithoutCreatingVictory()throws Exception {
        try(var f=fixture(2)) {
            var initial=life(f.view(),0);assertEquals(1,initial.incarnation());f.active();
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            var dead=die(f,100);var proof=life(dead,0);assertEquals(cell(10,9),proof.deathCell());assertEquals(cell(10,10),proof.spawn());
            assertEquals(1,proof.deaths());assertEquals(1,dead.exchanges().get(0).participantLife());assertNull(dead.world().result());
            f.elapsed(5000);assertTrue(life(f.view(),0).dead());assertEquals(0,f.view().world().vitals().get(0).hp(),"no Honor timer or natural heal resurrects Quest");
            var p=f.players.get(0);var q=f.players.get(1);
            assertThrows(SecurityException.class,()->f.engine.confirmQuestDeath(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,proof.deathRevision()));
            assertThrows(SecurityException.class,()->f.engine.confirmQuestDeath(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1,1,proof.deathRevision()));
            assertEquals(STALE_LIFE,f.engine.confirmQuestDeath(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),2,proof.deathRevision()).reason());
            var before=f.view();var revived=confirm(f,0,proof);assertEquals(REVIVED,revived.reason());var after=revived.frame();
            assertEquals(before.world().revision()+1,after.world().revision());assertEquals(cell(10,10),after.world().actors().get(0).position());
            assertEquals(2,after.world().actors().get(0).lastMoveSequence());assertEquals(1,after.world().vitals().get(0).hp());
            assertEquals(2,life(after,0).incarnation());assertEquals(1,life(after,0).hpGain());assertEquals(proof.deathRevision(),after.world().vitals().get(0).deathRevision());
            assertEquals(1,after.exchanges().get(0).participantLife(),"retained attack belongs to old life");
            assertEquals(STALE_LIFE,confirm(f,0,proof).reason());assertEquals(after,f.view());
            var second=die(f,5100);assertEquals(2,life(second,0).deaths());assertEquals(2,second.exchanges().get(0).participantLife());
            assertEquals(STALE_LIFE,confirm(f,0,proof).reason());assertEquals(REVIVED,confirm(f,0,life(second,0)).reason());
            assertEquals(2,life(f.view(),0).hpGain());assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());assertNull(f.view().world().result());
            f.randomValue=19;for(int stage=1;stage<=4;stage++)ModernQuestObjectivesTest.clear(f,0,stage,stage,6000+stage*1000L);
            var target=ModernQuestObjectivesTest.objective(f,0);f.elapsed(10700);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,ModernQuestObjectivesTest.strike(f,0,5,target.id()).reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,f.view().world().phase());assertEquals(2,life(f.view(),0).respawns());
            assertEquals(NOT_ACTIVE,confirm(f,0,life(second,0)).reason());
        }
    }
    @Test void anotherForceMayOccupySpawnButSameForceBlocksWithoutHalfResurrection()throws Exception {
        for(boolean ally:List.of(false,true))try(var f=fixture(ally?3:2)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,4).status());var dead=die(f,100);var proof=life(dead,0);
            int mover=ally?2:1,x=ally?30:20;long seq=0,elapsed=1000;
            assertEquals(ally?0:1,f.view().world().actors().get(mover).member().team());
            f.elapsed(elapsed);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(mover,++seq,x,10,0).status());
            while(x>10){f.elapsed(elapsed+=200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(mover,++seq,x--,9,6).status());}
            f.elapsed(elapsed+=200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(mover,++seq,10,9,4).status());
            var before=f.view();var result=confirm(f,0,proof);
            if(ally){assertEquals(SPAWN_BLOCKED,result.reason());assertEquals(before,result.frame());
                f.elapsed(elapsed+200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(mover,++seq,10,10,0).status());
                assertEquals(REVIVED,confirm(f,0,proof).reason());}
            else {assertEquals(REVIVED,result.reason());assertEquals(result.frame().world().actors().get(0).position(),result.frame().world().actors().get(1).position());}
        }
    }
    @Test void cancellationDuringFatalRandomAndPendingDeathCannotPublishAResurrection()throws Exception {
        try(var f=fixture(2)) {
            f.active();f.elapsed(100);f.duringRandom=()->f.rooms.releasePreparation(f.ticket);
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.step(ModernQuestEngineTest.living(f,0)).reason());
            assertEquals(0,life(f.view(),0).deaths());assertEquals(1,f.view().world().vitals().get(0).hp());
        }
        try(var f=fixture(2)) {
            f.active();var proof=life(die(f,100),0);f.rooms.releasePreparation(f.ticket);
            assertEquals(NOT_ACTIVE,confirm(f,0,proof).reason());assertTrue(life(f.view(),0).dead());assertNull(f.view().world().result());
        }
    }
    @Test void lifeArithmeticIsExactAndCannotBeReusedOrOverflowPartially() {
        var initial=ModernQuestLife.State.initial(1,0,cell(10,10));var dead=initial.died(3,cell(10,11));var revived=dead.revived(4,Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE,revived.hpGain());assertEquals(2,revived.incarnation());
        assertThrows(IllegalArgumentException.class,()->initial.revived(2,1));assertThrows(IllegalArgumentException.class,()->dead.died(4,cell(10,11)));
        assertThrows(IllegalArgumentException.class,()->dead.revived(3,1));assertThrows(ArithmeticException.class,()->revived.died(5,cell(10,11)).revived(6,1));
        assertEquals(1,revived.respawns());assertEquals(4,revived.respawnRevision());
    }
}
