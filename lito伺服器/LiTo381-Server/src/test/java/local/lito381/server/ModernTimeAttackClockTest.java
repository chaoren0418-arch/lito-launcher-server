package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exact virtual monotonic clock through the real engine and wire codec.
 * Synthetic geometry/NPCs; not a seven-minute wall-clock, SQL or UI test. */
class ModernTimeAttackClockTest {
    @TempDir Path temporary;

    private static ModernTimeAttackEngineTest.Fixture fixture(int duration)throws Exception {
        return new ModernTimeAttackEngineTest.Fixture(1,8,100,false,1,false,false,false,1,false,false,duration);
    }

    private static void canonical(ModernTimeAttackEngineTest.Fixture f)throws Exception {
        var publication=ModernBattleWireTest.publication(f.engine.serviceObservation(),f.catalog);
        var view=publication.audience(f.players.get(0).scope());
        assertEquals(60_000,publication.descriptor().activeTimeoutMillis());
        assertEquals(f.plan.durationMillis(),publication.world().modes().timeAttack().state().plan().durationMillis());
        assertFalse(ModernBattleWireTest.encoded(view).isEmpty());
    }

    @Test void configuredSevenMinutesRemainSeparateFromSixtySecondTowerWait()throws Exception {
        var p=ModernTimeAttackConfigurationTest.world(temporary);
        int duration=BattleRules.Mode.TIME_ATTACK.defaultSeconds()*1000;
        assertEquals(420_000,duration);
        p.setProperty(ModernTimeAttackConfigurationTest.PREFIX+"timeAttackDurationMillis",Integer.toString(duration));
        var mapping=ModernTimeAttackConfigurationTest.mapping(temporary,p);
        assertEquals(duration,mapping.timeAttack().durationMillis());
        assertEquals(60_000,mapping.clockOverrides().activeTimeoutMillis());
        // Time Attack is deliberately human-only, but one human can start.
        assertEquals(1,TournamentRoomPolicy.rules(4,540).minimumPlayers());
    }

    @Test void delayedTowerStartKeepsFullSevenMinutesAndThirtyMinuteBoundaryThenSettlesLoss()throws Exception {
        for(int duration:List.of(420_000,1_800_000))try(var f=fixture(duration)) {
            f.active();f.elapsed(59_999);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
            long deadline=59_999L+duration;
            for(long elapsed:List.of(60_000L,420_000L,deadline-1)) {
                f.elapsed(elapsed);var w=f.view().world();
                assertEquals(ModernBattleEngine.Phase.ACTIVE,w.phase());
                assertEquals(59_999,w.timeAttack().state().startedMillis());
                assertEquals(duration,w.timeAttack().state().plan().durationMillis());
                assertNull(f.engine.timeAttackResult());
                canonical(f);
            }
            var npc=f.living();f.elapsed(deadline);
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.hit(0,2,npc).reason());
            var frame=f.view();var world=frame.world();
            assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());
            assertEquals(ModernBattleEngine.AbortReason.NONE,world.abortReason());
            assertEquals(ModernTimeAttack.End.TIMEOUT,world.timeAttack().state().end());
            assertEquals(deadline,world.timeAttack().state().observedMillis());
            assertTrue(frame.npcs().actors().get(0).alive(),"no damage may commit at the deadline");
            var result=f.engine.timeAttackResult();assertNotNull(result);
            assertEquals(List.of(ModernMatchSettlement.Verdict.LOSS),result.participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertEquals(duration,result.timeAttack().durationMillis());
            assertEquals(59_999,result.timeAttack().startedMillis());
            assertEquals(deadline,result.timeAttack().observedMillis());
            var wire=ModernTimeAttackProtocolTest.read(f,0);
            assertEquals(wire,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(wire,25),25));
            canonical(f);
            f.elapsed(3_000_000);assertEquals(world,f.view().world());
            assertEquals(result,f.engine.timeAttackResult());
        }
    }

    @Test void towerHitAtWaitingDeadlineAbortsWithoutGeneratingBattleLoss()throws Exception {
        try(var f=fixture(420_000)) {
            f.active();f.elapsed(60_000);
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.tower(0,1).reason());
            var world=f.view().world();
            assertEquals(ModernBattleEngine.Phase.ABORTED,world.phase());
            assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,world.abortReason());
            assertEquals(-1,world.timeAttack().state().startedMillis());
            assertNull(f.engine.timeAttackResult());
        }
    }
}
