package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Internal cooperative engine qualification; not Main, map540 assets or UI acceptance. */
class ModernHasteTimeAttackTest {
    static ModernTimeAttackEngineTest.Fixture fixture(int humans,boolean dragon,long hp)throws Exception {
        return new ModernTimeAttackEngineTest.Fixture(humans,16,hp,dragon,1,true,true,true,1,true);
    }
    static ModernBattleObservation.Capture view(ModernTimeAttackEngineTest.Fixture f) {return f.engine.serviceObservation();}
    static void active(ModernTimeAttackEngineTest.Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.binding.map().digest());
        assertEquals(ModernBattleEngine.Phase.COUNTDOWN,view(f).phase());f.elapsed(0);
        assertEquals(ModernBattleEngine.Phase.ACTIVE,view(f).phase());
    }
    static ModernBattleEngine.HasteResult cast(ModernTimeAttackEngineTest.Fixture f,int who,ModernHasteCast.Target target) {
        var before=view(f);long sequence=before.actors().get(who).runtime().sequence()+1;
        var r=f.engine.haste(f.players.get(who).owner(),ModernBattleHasteTest.request(before,who,sequence,target));
        assertEquals(ModernHasteCast.Reason.NONE,r.reason());return r;
    }
    static void player(ModernTimeAttackEngineTest.Fixture f,int who,int target) {cast(f,who,ModernBattleHasteTest.target(view(f),target));}
    static ModernNpcRoster.Actor living(ModernTimeAttackEngineTest.Fixture f) {
        return view(f).npcs().roster().actors().stream().filter(ModernNpcRoster.Actor::alive).findFirst().orElseThrow();
    }
    static void npc(ModernTimeAttackEngineTest.Fixture f,int who) {cast(f,who,ModernBattleNpcHasteTest.target(living(f)));}
    static void cleared(ModernBattleObservation.Capture c) {
        assertTrue(c.actors().stream().allMatch(a->!a.buffs().hasEffects()));
        assertTrue(c.npcs().roster().actors().stream().noneMatch(a->a.speed().hasted()));
        assertTrue(c.hasteTargets().targets().values().stream().allMatch(e->e.active()==null));
        assertDoesNotThrow(()->c.hasteTargets().validate(c));
        assertDoesNotThrow(c::world);
        for(var a:c.actors())assertDoesNotThrow(()->c.forOwner(a.scope()));
    }
    static void setup(ModernTimeAttackEngineTest.Fixture f) {
        active(f);player(f,0,0);f.elapsed(2000);player(f,0,1);f.elapsed(4000);
        assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
        f.elapsed(6000);npc(f,0);f.elapsed(8000);
    }
    static void outcome(ModernBattleObservation.Capture c,ModernTimeAttack.End end,ModernMatchSettlement.Verdict verdict) {
        assertEquals(ModernBattleEngine.Phase.FINISHED,c.phase());assertEquals(end,c.modes().timeAttack().state().end());
        cleared(c);var event=c.modes().timeAttackResult();assertNotNull(event);assertNull(c.result());
        assertEquals(4,event.mode());assertEquals(540,event.mapId());
        assertEquals(List.of(ModernParticipantId.human(7,3333),ModernParticipantId.human(42,3333)),event.participants().stream().map(ModernMatchSettlement.Participant::identity).toList());
        assertTrue(event.participants().stream().allMatch(p->p.verdict()==verdict));
    }
    static void kill(ModernTimeAttackEngineTest.Fixture f,int skill) {
        var c=view(f);var a=c.actors().get(0);var p=f.players.get(0);var n=living(f);
        long sequence=a.runtime().sequence()+1;
        switch(skill) {
            case 0 -> {f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.hit(0,a.vital().attackSequence()+1,n).reason());}
            case 4 -> {f.random=0;assertEquals(ModernNpcMagic.Reason.NONE,f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,1,n.id(),n.life()).reason());}
            case 70 -> {f.random=0;assertEquals(ModernNpcFireStormState.Reason.NONE,f.engine.fireStormNpc(p.owner(),new ModernNpcFireStormState.Request(p.scope(),n.id().scope(),sequence,1)).reason());}
            case 77 -> {f.random=0;assertEquals(ModernNpcDisintegrateState.Reason.NONE,f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(p.scope(),n.id(),sequence,1,n.life())).reason());}
            default -> throw new AssertionError("unknown test skill");
        }
    }
    @Test void timeoutClearsBothPlayerAndNpcWithoutChangingTheirCommittedReceipts()throws Exception {
        try(var f=fixture(2,false,100)) {
            active(f);player(f,0,0);f.elapsed(2000);player(f,0,1);f.elapsed(4000);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());f.elapsed(6000);npc(f,0);
            var before=view(f);assertTrue(before.actors().stream().allMatch(a->a.buffs().hasEffects()));
            assertTrue(living(f).speed().hasted());f.elapsed(24000);var terminal=view(f);
            assertEquals(ModernTimeAttack.End.TIMEOUT,terminal.modes().timeAttack().state().end());
            cleared(terminal);
            assertEquals(before.actors().get(0).runtime().receipts(),terminal.actors().get(0).runtime().receipts());
            assertEquals(before.actors().get(0).resources().flows().spent(),terminal.actors().get(0).resources().flows().spent());
        }
    }
    @Test void finalMeleeAndAllThreeQualifiedDamageSpellsCommitClearAndOneCooperativeResult()throws Exception {
        for(int skill:List.of(0,4,70,77))for(boolean dragon:List.of(false,true))try(var f=fixture(2,dragon,100)) {
            setup(f);
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                if(wave>0){f.elapsed(6000+wave*5000);npc(f,0);f.elapsed(8000+wave*5000);}
                assertTrue(living(f).speed().hasted());kill(f,skill);
                if(wave+1<f.plan.waves().size()) {
                    var c=view(f);assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());assertNull(c.modes().timeAttackResult());
                    assertTrue(c.actors().stream().allMatch(a->a.buffs().hasEffects()));assertFalse(living(f).speed().hasted(),"new wave does not inherit a retired NPC's lease");
                }
            }
            var terminal=view(f);outcome(terminal,ModernTimeAttack.End.CLEARED,ModernMatchSettlement.Verdict.WIN);
            long hasteCost=ModernHaste.qualify(f.prepared.get(0).fighter()).mpCost();
            assertEquals(2+f.plan.waves().size(),terminal.actors().get(0).runtime().receipts().get(ModernBattleHasteTest.LANE).casts());
            assertTrue(terminal.actors().get(0).resources().flows().spent()>=(2+f.plan.waves().size())*hasteCost);
            assertSame(terminal.modes().timeAttackResult(),f.engine.timeAttackResult());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(terminal));
            f.elapsed(100_000);assertEquals(terminal,view(f));assertSame(terminal.modes().timeAttackResult(),f.engine.timeAttackResult());
        }
    }
    @Test void departureClearsOnlyItsRecipientUntilLastOwnerLeavesAndCannotDebitRetiredTarget()throws Exception {
        try(var f=fixture(2,false,100)) {
            setup(f);var before=view(f);var departed=f.engine.withdrawTimeAttackObservation(f.players.get(0).owner());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,departed.phase());assertEquals(2,departed.modes().timeAttack().state().leader());
            assertFalse(departed.actors().get(0).buffs().hasEffects());assertTrue(departed.actors().get(1).buffs().hasEffects());
            assertTrue(living(f).speed().hasted());assertEquals(before.actors().get(0).runtime(),departed.actors().get(0).runtime());
            assertThrows(SecurityException.class,()->cast(f,0,ModernBattleHasteTest.target(departed,1)));
            var rejected=f.engine.haste(f.players.get(1).owner(),ModernBattleHasteTest.request(departed,1,1,ModernBattleHasteTest.target(departed,0)));
            assertEquals(ModernHasteCast.Reason.WRONG_INSTANCE,rejected.reason());assertEquals(0,rejected.observation().actors().get(1).resources().flows().spent());
            var terminal=f.engine.withdrawTimeAttackObservation(f.players.get(1).owner());
            outcome(terminal,ModernTimeAttack.End.PARTY_DEFEATED,ModernMatchSettlement.Verdict.LOSS);
            assertEquals(0,f.engine.ownerCount());assertEquals(before.actors().get(0).runtime(),terminal.actors().get(0).runtime());
            assertSame(terminal.modes().timeAttackResult(),f.engine.timeAttackResult());
        }
    }
    @Test void finalFireStormBatchClearsEveryHastedVictimAlongsideBothParticipants()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,16,100,true,1,true,true,true,2,true)) {
            setup(f);
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                if(wave>0){f.elapsed(6000+wave*5000);npc(f,0);}
                var last=view(f).npcs().roster().actors().stream().filter(ModernNpcRoster.Actor::alive).reduce((a,b)->b).orElseThrow();
                cast(f,1,ModernBattleNpcHasteTest.target(last));
                assertEquals(2,view(f).npcs().roster().actors().stream().filter(a->a.alive()&&a.speed().hasted()).count());
                f.elapsed(8000+wave*5000);kill(f,70);
            }
            var c=view(f);outcome(c,ModernTimeAttack.End.CLEARED,ModernMatchSettlement.Verdict.WIN);
            assertEquals(8,c.npcs().roster().spawned());assertTrue(c.npcs().roster().actors().stream().noneMatch(ModernNpcRoster.Actor::alive));
            assertEquals(4,c.actors().get(1).runtime().receipts().get(ModernBattleHasteTest.LANE).casts());
        }
    }
    @Test void leaderDeathAtomicallyRetiresAllLeasesButKeepsTheCommittedAcceleratedAttackClock()throws Exception {
        try(var f=fixture(2,false,1)) {
            setup(f);var n=living(f);long interval=n.pace().attackMillis();f.random=19;
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(n.id(),n.life(),4096,false));
            var c=view(f);outcome(c,ModernTimeAttack.End.LEADER_DEFEATED,ModernMatchSettlement.Verdict.LOSS);
            assertEquals(0,c.actors().get(0).vital().values().hp());assertEquals(1,c.actors().get(1).vital().values().hp());
            var attacker=c.npcs().roster().actors().get(0);assertEquals(8000+interval,attacker.nextAttackMillis());
            assertEquals(n.hp(),attacker.hp());assertEquals(n.mp(),attacker.mp());
        }
    }
    @Test void ordinaryMemberDeathRetiresOnlyTheDeadRecipientUntilTheRealDeadline()throws Exception {
        try(var f=fixture(2,false,1)) {
            setup(f);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,6).status());
            f.elapsed(8200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,2,9,10,6).status());
            f.elapsed(8400);var n=living(f);f.random=19;
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(n.id(),n.life(),4096,false));
            var c=view(f);assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());assertNull(c.modes().timeAttackResult());
            assertEquals(1,c.actors().get(0).vital().values().hp());assertEquals(0,c.actors().get(1).vital().values().hp());
            assertTrue(c.actors().get(0).buffs().hasEffects());assertFalse(c.actors().get(1).buffs().hasEffects());assertTrue(living(f).speed().hasted());
            f.elapsed(24_000);outcome(view(f),ModernTimeAttack.End.TIMEOUT,ModernMatchSettlement.Verdict.LOSS);
        }
    }
    @Test void abortAndWaitingSafetyClearEffectsWithoutInventingACompetitiveResult()throws Exception {
        for(boolean safety:List.of(false,true))try(var f=fixture(2,false,100)) {
            ModernBattleObservation.Capture c;
            if(safety){active(f);player(f,0,0);f.elapsed(2000);player(f,0,1);f.elapsed(60_000);c=view(f);}
            else {setup(f);c=f.engine.abortObservation();}
            assertEquals(ModernBattleEngine.Phase.ABORTED,c.phase());cleared(c);
            assertTrue(c.npcs().roster().closed());assertNull(c.modes().timeAttackResult());assertNull(c.result());
            assertTrue(c.actors().get(0).resources().flows().spent()>0);
        }
    }
    @Test void timeoutAndDepartureDuringDamageRollCannotCommitAStaleKillOrMagicDebit()throws Exception {
        for(int skill:List.of(0,4,70,77))for(boolean timeout:List.of(false,true))try(var f=fixture(2,false,100)) {
            setup(f);var before=view(f);var p=f.players.get(0);var n=living(f);var a=before.actors().get(0);long seq=a.runtime().sequence()+1;
            f.duringRandom=()->{if(timeout)f.elapsed(24_000);else f.engine.withdrawTimeAttackObservation(p.owner());};
            switch(skill) {
                case 0 -> assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.hit(0,a.vital().attackSequence()+1,n).reason());
                case 4 -> assertEquals(ModernNpcMagic.Reason.NOT_ACTIVE,f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),seq,1,n.id(),n.life()).reason());
                case 70 -> assertEquals(ModernNpcFireStormState.Reason.NOT_ACTIVE,f.engine.fireStormNpc(p.owner(),new ModernNpcFireStormState.Request(p.scope(),n.id().scope(),seq,1)).reason());
                case 77 -> assertEquals(ModernNpcDisintegrateState.Reason.NOT_ACTIVE,f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(p.scope(),n.id(),seq,1,n.life())).reason());
            }
            var c=view(f);assertEquals(a.runtime(),c.actors().get(0).runtime());assertEquals(a.resources().flows().spent(),c.actors().get(0).resources().flows().spent());
            assertEquals(a.vital().attackSequence(),c.actors().get(0).vital().attackSequence());assertTrue(living(f).alive());assertEquals(0,c.modes().timeAttack().state().wave());
            if(timeout)outcome(c,ModernTimeAttack.End.TIMEOUT,ModernMatchSettlement.Verdict.LOSS);
            else {assertEquals(2,c.modes().timeAttack().state().leader());assertFalse(c.actors().get(0).buffs().hasEffects());assertTrue(c.actors().get(1).buffs().hasEffects());assertTrue(living(f).speed().hasted());}
        }
    }
    @Test void timeoutRetainsAlreadyCommittedNpcWalkProvenanceWithoutKeepingAnActiveLease()throws Exception {
        try(var f=fixture(2,false,100)) {
            setup(f);
            for(int who=0;who<2;who++) {
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(who,1,10,10+who,6).status());
            }
            f.elapsed(8200);for(int who=0;who<2;who++)assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(who,2,9,10+who,6).status());
            f.elapsed(8400);var n=living(f);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),n.life(),4096,false));
            var walked=living(f);assertTrue(walked.walkSpeed().hasted());assertEquals(75,walked.walk().durationMillis());
            f.elapsed(24000);var c=view(f);outcome(c,ModernTimeAttack.End.TIMEOUT,ModernMatchSettlement.Verdict.LOSS);
            var stopped=living(f);assertEquals(walked.walk(),stopped.walk());assertEquals(walked.walkSpeed(),stopped.walkSpeed());assertEquals(walked.nextMoveMillis(),stopped.nextMoveMillis());
            assertThrows(IllegalArgumentException.class,()->ModernNpcProtocol.Actor.from(stopped));
        }
    }
}
