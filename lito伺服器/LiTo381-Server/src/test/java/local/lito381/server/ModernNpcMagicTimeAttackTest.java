package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcMagic.Reason.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Real cooperative engine transactions; synthetic pattern, not native map540 placement. */
class ModernNpcMagicTimeAttackTest {
    static ModernNpcMagic.Result cast(ModernTimeAttackEngineTest.Fixture f,int who,long sequence,ModernNpcRoster.Actor target) {
        var p=f.players.get(who);return f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,1,target.id(),target.life());
    }
    static ModernNpcProtocol.Frame frame(ModernTimeAttackEngineTest.Fixture f) {
        var value=f.view();var w=value.world();var result=f.engine.timeAttackResult();
        if(result!=null)result=new ModernMatchSettlement.Event(UUID.fromString("12345678-1234-5678-9abc-def012345678"),result.matchId(),result.generation(),result.engineRevision(),result.mode(),result.mapId(),result.finishedAtMillis(),result.participants(),result.timeAttack());
        var d=ModernNpcDisintegrateFrameTest.descriptor(w);
        if(w.npcDisintegrations().isEmpty())d=new ModernMatchDescriptor(d.ruleset(),d.features()&~ModernMatchDescriptor.NPC_DISINTEGRATE,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
        return ModernTimeAttackProtocol.frame(value,d,result,ModernWorldProtocol.SettlementState.PENDING,new ModernNpcProtocol.Resources(List.of(value.npcs().actors().get(0).template().graphic()))).withNames(ModernNpcNames.Table.from(f.catalog));
    }
    @Test void everyConfiguredWaveIncludingDragonCommitsManaKillAndOneCooperativeResult()throws Exception {
        for(boolean dragon:List.of(false,true))try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,dragon,1,false,true)) {
            f.active();f.tower(0,1);assertEquals(TOO_FAST,cast(f,0,1,f.living()).reason());
            for(int i=0;i<f.plan.waves().size();i++) {
                f.elapsed(1000+1200L*i);var target=f.living();var before=f.view();var r=cast(f,0,i+2,target);assertEquals(NONE,r.reason());
                var w=r.frame().world();var s=w.npcSpells().get(0);assertEquals(i+1,s.castCount());assertTrue(s.outcome().death());
                assertEquals(w.revision(),s.revision());assertFalse(w.timeAttack().state().living().contains(target.id()));
                assertEquals(before.world().vitals().get(0).mp()-s.mpCost(),w.vitals().get(0).mp());
                assertTrue(w.damages().isEmpty());assertTrue(w.npcDisintegrations().isEmpty());assertEquals(0,w.casts().get(0).castCount());
            }
            var w=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,w.phase());assertEquals(ModernTimeAttack.End.CLEARED,w.timeAttack().state().end());
            assertTrue(w.timeAttack().outcomes(w).stream().allMatch(p->p.verdict()==ModernMatchSettlement.Verdict.WIN));
            var corpse=f.view().npcs().actors().get(0);assertEquals(NOT_ACTIVE,cast(f,0,999,corpse).reason());f.elapsed(100000);assertEquals(w,f.view().world());
        }
    }
    @Test void departureAndTimeoutDuringEntropyPreventAnyUncommittedManaOrWaveMutation()throws Exception {
        for(boolean timeout:List.of(false,true))try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,false,1,false,true)) {
            f.active();f.tower(0,1);f.elapsed(1000);var target=f.living();var before=f.view();
            f.duringRandom=()->{if(timeout)f.elapsed(20000);else f.engine.withdrawTimeAttack(f.players.get(0).owner());};
            assertEquals(NOT_ACTIVE,cast(f,0,1,target).reason());var after=f.view();
            assertEquals(before.world().npcSpells(),after.world().npcSpells());assertEquals(before.world().casts(),after.world().casts());
            assertEquals(before.world().vitals(),after.world().vitals());assertTrue(after.npcs().actors().get(0).alive());
            if(timeout)assertEquals(ModernTimeAttack.End.TIMEOUT,after.world().timeAttack().state().end());
            else {assertEquals(2,after.world().timeAttack().state().leader());assertThrows(SecurityException.class,()->cast(f,0,1,target));}
        }
    }
    @Test void mixedLightArrowAndNpc77ShareSequenceAndAdvanceTheSamePattern()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false,1,true,true)) {
            f.active();f.tower(0,1);f.elapsed(1000);assertEquals(NONE,cast(f,0,1,f.living()).reason());
            f.elapsed(2200);assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernNpcDisintegrateTimeAttackTest.cast(f,0,2,f.living()).reason());
            f.elapsed(3400);assertEquals(NONE,cast(f,0,3,f.living()).reason());
            var w=f.view().world();assertEquals(3,w.casts().get(0).requestSequence());assertEquals(2,w.npcSpells().get(0).castCount());assertEquals(1,w.npcDisintegrations().get(0).castCount());
            var wire=frame(f);assertEquals(wire.combat().world().revision(),wire.combat().npcSpells().get(0).revision());
        }
    }
    @Test void finalSpellAckAndSavedSnapshotHaveAnImmutableEndAnchorAndRejectOldPeers()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false,1,false,true)) {
            f.active();f.tower(0,1);for(int i=0;i<3;i++){f.elapsed(1000+1200L*i);assertEquals(NONE,cast(f,0,i+1,f.living()).reason());}
            var nf=frame(f);var c=nf.combat();var w=c.world();var s=c.npcSpells().get(0);
            var q=new ModernNpcSpellProtocol.Request(f.players.get(0).scope(),nf.scope(),s.requestSequence(),s.casterLife(),s.target().serial(),s.targetLife());
            for(var state:List.of(ModernWorldProtocol.SettlementState.PENDING,ModernWorldProtocol.SettlementState.SAVED,ModernWorldProtocol.SettlementState.FAILED)) {
                var end=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+(state==ModernWorldProtocol.SettlementState.PENDING?0:1),w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),null,w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest(),w.timeAttack().withSettlement(state));
                var frame=nf.withCombat(new ModernCombatProtocol.Frame(end,c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells()));
                var m=new ModernApplicationProtocol.NpcSpellResult(99,q,new ModernNpcSpellProtocol.Reply(ModernNpcProtocol.Status.OK,NONE,frame));
                var bytes=ModernApplicationProtocol.encode(m,30);assertEquals(m,ModernApplicationProtocol.decode(bytes,30));
                for(int old:List.of(24,25,29)) {
                    assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,old));
                    assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,old));
                    assertFalse(w.descriptor().supportedBy(old,ModernSessionProtocol.KNOWN_CAPABILITIES));
                }
                assertTrue(w.descriptor().supportedBy(30,ModernSessionProtocol.KNOWN_CAPABILITIES));
            }
        }
    }
}
