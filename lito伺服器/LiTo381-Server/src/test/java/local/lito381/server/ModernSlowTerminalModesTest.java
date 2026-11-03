package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSlowCast.Reason.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Full internal terminal modes, with synthetic qualified maps/catalogs, not UI acceptance. */
class ModernSlowTerminalModesTest {
    static ModernBattleEngine.SlowResult slow(ModernTimeAttackEngineTest.Fixture f,int who,ModernSlowCast.Target target) {
        var c=f.engine.serviceObservation();var q=ModernBattleSlowTest.request(c,who,c.actors().get(who).runtime().sequence()+1,target);
        var r=f.engine.slow(f.players.get(who).owner(),q);assertEquals(NONE,r.reason());return r;
    }
    static void cleared(ModernBattleObservation.Capture c) {
        ModernBattleSlowTest.consistent(c);assertTrue(c.actors().stream().noneMatch(a->a.buffs().hasEffects()));
        if(c.npcs()!=null)assertTrue(c.npcs().roster().actors().stream().noneMatch(a->a.speed().slowed()));
        assertTrue(c.slowTargets().targets().values().stream().allMatch(e->e.active()==null));
    }
    @Test void timeAttackTimeoutClearsNpcAndHumanSlowAndPreservesPaidCosts()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,16,100,false,1,true,true,true,1,true,true)) {
            ModernHasteTimeAttackTest.active(f);f.random=0;var c=f.engine.serviceObservation();slow(f,0,ModernBattleSlowTest.target(c,0));
            f.elapsed(3000);f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());f.elapsed(6000);f.random=0;
            slow(f,0,ModernBattleNpcSlowTest.target(ModernHasteTimeAttackTest.living(f)));
            var before=f.engine.serviceObservation();assertTrue(before.actors().get(0).buffs().slowUntilNanos()>0);assertTrue(ModernHasteTimeAttackTest.living(f).speed().slowed());
            f.elapsed(63000);var end=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.FINISHED,end.phase());
            assertEquals(ModernTimeAttack.End.TIMEOUT,end.modes().timeAttack().state().end());assertNotNull(end.modes().timeAttackResult());cleared(end);
            assertEquals(2,end.actors().get(0).materials().uses());assertEquals(before.actors().get(0).resources().flows().spent(),end.actors().get(0).resources().flows().spent());
        }
    }
    @Test void timeAttackFinalKillsClearEachWaveAndFinishOneCooperativeWin()throws Exception {
        for(int skill:List.of(0,4,70,77))try(var f=new ModernTimeAttackEngineTest.Fixture(2,16,100,true,1,true,true,true,1,true,true)) {
            ModernHasteTimeAttackTest.active(f);f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed(3000+wave*7000);f.random=0;slow(f,0,ModernBattleNpcSlowTest.target(ModernHasteTimeAttackTest.living(f)));
                assertTrue(ModernHasteTimeAttackTest.living(f).speed().slowed());f.elapsed(5000+wave*7000);ModernHasteTimeAttackTest.kill(f,skill);
            }
            var end=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.FINISHED,end.phase());assertEquals(ModernTimeAttack.End.CLEARED,end.modes().timeAttack().state().end());
            assertEquals(f.plan.waves().size(),end.actors().get(0).materials().uses());assertNotNull(end.modes().timeAttackResult());cleared(end);
            assertSame(end.modes().timeAttackResult(),f.engine.timeAttackResult());
        }
    }
    @Test void siegeThirtyMemberInternalWorldSettlesTimeoutWithNoLeakedSlowOrRefund()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,60000,1000,0,true,15,true)) {
            for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.binding.map().digest());
            var c=f.engine.serviceObservation();assertEquals(30,c.actors().size());
            f.hitSlow=true;
            var r=f.engine.slow(f.players.get(0).owner(),ModernBattleSlowTest.request(c,0,1,ModernBattleSlowTest.target(c,0)));
            assertEquals(NONE,r.reason());assertEquals(1,r.observation().actors().get(0).materials().uses());
            assertTrue(r.observation().actors().get(0).buffs().slowUntilNanos()>0);
            f.waitMillis(60000);var end=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.FINISHED,end.phase());
            assertEquals(1,end.modes().siege().state().winner());assertEquals(1,end.actors().get(0).materials().uses());cleared(end);
        }
    }
}
