package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Actual cooperative engine state, synthetic qualified pattern. Not proof of
 * native boss behaviors, a production spawn catalog, client wire or UI. */
class ModernNpcDisintegrateTimeAttackTest {
    static ModernNpcDisintegrateState.Result cast(ModernTimeAttackEngineTest.Fixture f,int who,long sequence,ModernNpcRoster.Actor target) {
        var p=f.players.get(who);return f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(p.scope(),target.id(),sequence,1,target.life()));
    }
    @Test void skill77DeathsAdvanceEveryConfiguredWaveAndProduceOneCooperativeClear()throws Exception {
        for(boolean dragon:List.of(false,true))try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,dragon,1,true)) {
            f.active();assertTrue(f.view().world().npcSpells().isEmpty());assertEquals(0,f.draws.get());
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
            var first=f.living();assertEquals(TOO_FAST,cast(f,0,1,first).reason());
            for(int i=0;i<f.plan.waves().size();i++) {
                f.elapsed(1000+5000L*i);var npc=f.living();var before=f.view();
                var reply=cast(f,0,i+2,npc);assertEquals(NONE,reply.reason());var after=reply.frame();
                var s=after.world().npcDisintegrations().get(0);
                assertTrue(s.hit().outcome().death());assertEquals(after.world().revision(),s.revision());
                assertFalse(after.world().timeAttack().state().living().contains(npc.id()));
                assertEquals(before.world().vitals().get(0).mp()-70,after.world().vitals().get(0).mp());
                assertEquals(501+i,s.lawful());assertEquals(i+1,s.castCount());
                assertTrue(after.world().disintegrations().isEmpty());assertTrue(after.world().quest().lives().isEmpty());
                if(i+1<f.plan.waves().size())assertEquals(ModernBattleEngine.Phase.ACTIVE,after.world().phase());
            }
            var finalFrame=f.view();var w=finalFrame.world();assertEquals(ModernBattleEngine.Phase.FINISHED,w.phase());
            assertEquals(ModernTimeAttack.End.CLEARED,w.timeAttack().state().end());assertNull(w.result());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.WIN),w.timeAttack().outcomes(w).stream().map(ModernMatchSettlement.Participant::verdict).toList());
            var receipt=w.npcDisintegrations().get(0);var corpse=finalFrame.npcs().actors().stream().filter(n->n.id().equals(receipt.hit().target())).findFirst().orElseThrow();
            assertEquals(NOT_ACTIVE,cast(f,0,999,corpse).reason());f.elapsed(100000);assertEquals(w,f.view().world());
        }
    }
    @Test void departedCasterCannotCommitAnAlreadyCalculatedNpcSpell()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,false,1,true)) {
            f.active();f.tower(0,1);f.elapsed(1000);var npc=f.living();var before=f.view();
            f.duringRandom=()->f.engine.withdrawTimeAttack(f.players.get(0).owner());
            assertEquals(NOT_ACTIVE,cast(f,0,1,npc).reason());var after=f.view();
            assertEquals(before.world().npcDisintegrations(),after.world().npcDisintegrations());
            assertEquals(before.world().vitals(),after.world().vitals());assertEquals(before.npcs().actors(),after.npcs().actors());
            assertEquals(2,after.world().timeAttack().state().leader());assertTrue(after.world().timeAttack().state().departed().contains(1L));
            assertThrows(SecurityException.class,()->cast(f,0,1,npc));
        }
    }
    @Test void timeoutAndLeaderDeathDuringEntropyWinOverAnUncommittedCast()throws Exception {
        for(boolean timeout:List.of(false,true))try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,timeout?100:1,false,1,true)) {
            f.active();f.tower(0,1);var npc=f.living();f.elapsed(1000);var before=f.view();
            f.duringRandom=()->{if(timeout)f.elapsed(20000);else assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpc(npc.id(),npc.life(),4096,false).reason());};
            assertEquals(NOT_ACTIVE,cast(f,0,1,npc).reason());var after=f.view();
            assertEquals(before.world().npcDisintegrations(),after.world().npcDisintegrations());
            assertEquals(before.world().casts(),after.world().casts());assertTrue(after.npcs().actors().get(0).alive());
            assertEquals(ModernBattleEngine.Phase.FINISHED,after.world().phase());
            assertEquals(timeout?ModernTimeAttack.End.TIMEOUT:ModernTimeAttack.End.LEADER_DEFEATED,after.world().timeAttack().state().end());
            assertEquals(ModernMatchSettlement.Verdict.LOSS,after.world().timeAttack().outcomes(after.world()).get(0).verdict());
        }
    }
}
