package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Two victims in ONE AoE must advance a cooperative wave only once. */
class ModernNpcFireStormTimeAttackTest {
    static ModernNpcFireStormState.Result cast(ModernTimeAttackEngineTest.Fixture f,int who,long sequence) {
        var p=f.players.get(who);return f.engine.fireStormNpc(p.owner(),new ModernNpcFireStormState.Request(p.scope(),f.view().npcs().scope(),sequence,1));
    }
    @Test void ordinaryFinalAndDragonBatchesEachAdvanceOnceAndLastBatchFinishes()throws Exception {
        for(boolean dragon:List.of(false,true))try(var f=new ModernTimeAttackEngineTest.Fixture(1,16,100,dragon,1,false,true,true,2)) {
            f.random=0;f.active();assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed(1000+wave*2000L);var before=f.view();assertEquals(2,before.npcs().actors().stream().filter(ModernNpcRoster.Actor::alive).count());
                var result=cast(f,0,wave+1);assertEquals(NONE,result.reason());var after=result.frame();var state=after.world().npcFireStorms().get(0);
                assertEquals(2,state.targets().size());assertTrue(state.targets().stream().allMatch(t->t.outcome().death()));
                assertEquals(before.world().revision()+1,after.world().revision());assertEquals(after.world().revision(),after.world().timeAttack().state().revision());
                assertEquals(wave+1,state.castCount());assertEquals(1000-48*(wave+1),after.world().vitals().get(0).mp());
                assertEquals(Math.min(wave+1,f.plan.waves().size()-1),after.world().timeAttack().state().wave());
            }
            var world=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());
            assertEquals(ModernTimeAttack.End.CLEARED,world.timeAttack().state().end());assertNull(world.result());
            assertEquals(ModernMatchSettlement.Verdict.WIN,world.timeAttack().outcomes(world).get(0).verdict());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(world,null));
            f.elapsed(100_000);assertEquals(world,f.view().world());assertEquals(NOT_ACTIVE,cast(f,0,99).reason());
        }
    }
    @Test void duplicateOrForeignDefeatedVictimsCannotAdvanceBatchPartially()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,16,100,false,1,false,false,true,2)) {
            f.active();f.tower(0,1);var frame=f.view();var before=frame.world().timeAttack().state();var victims=frame.npcs().actors();
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttack.defeated(before,List.of(victims.get(0),victims.get(0)),frame.world().revision()+1));
            assertEquals(frame,f.view());
            var result=ModernTimeAttack.defeated(before,victims,frame.world().revision()+1);
            assertTrue(result.living().isEmpty());assertEquals(1,result.wave());assertEquals(ModernTimeAttack.Phase.SPAWN_PENDING,result.phase());
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttack.defeated(result,victims,result.revision()+1));
            assertEquals(frame,f.view());
        }
    }
    @Test void departureOrTimeoutInsideVictimRollsCannotSpendManaOrCompleteWave()throws Exception {
        for(boolean timeout:List.of(false,true))try(var f=new ModernTimeAttackEngineTest.Fixture(2,16,100,false,1,false,false,true,2)) {
            f.active();f.tower(0,1);f.elapsed(1000);var before=f.view();
            f.duringRandom=()->{if(timeout)f.elapsed(20_000);else f.engine.withdrawTimeAttack(f.players.get(0).owner());};
            var result=cast(f,0,1);assertEquals(NOT_ACTIVE,result.reason());var after=result.frame();
            assertEquals(before.world().npcFireStorms(),after.world().npcFireStorms());assertEquals(before.world().vitals().get(0).mp(),after.world().vitals().get(0).mp());
            assertEquals(before.npcs().actors(),after.npcs().actors());
            if(timeout){assertEquals(ModernTimeAttack.End.TIMEOUT,after.world().timeAttack().state().end());assertEquals(ModernBattleEngine.Phase.FINISHED,after.world().phase());}
            else {assertEquals(2,after.world().timeAttack().state().leader());assertEquals(ModernBattleEngine.Phase.ACTIVE,after.world().phase());}
        }
    }
}
