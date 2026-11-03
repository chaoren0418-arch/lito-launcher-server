package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernBattleEngineFireStormTest {
    static final long SECOND=1_000_000_000L;
    static ModernBattleEngineHonorTest.Fixture fixture(boolean honor,int bots,long towerHp,long hp)throws Exception{
        return new ModernBattleEngineHonorTest.Fixture(towerHp,hp,300,SECOND,bots,true,honor);
    }
    static ModernCombatProtocol.Frame frame(ModernBattleEngine.Snapshot s){
        var world=new ModernWorldProtocol.Frame(s.matchId(),s.generation(),Math.incrementExact(s.revision()),ModernWorldProtocol.Phase.valueOf(s.phase().name()),
                ModernWorldProtocol.AbortReason.valueOf(s.abortReason().name()),s.room(),s.manifest(),s.actors(),s.result()==null?null:
                new ModernWorldProtocol.Completion(s.result(),ModernWorldProtocol.SettlementState.PENDING),s.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList());
        var v=s.vitals().stream().map(x->new ModernCombatProtocol.Vital(x.entityId(),x.hp(),x.maxHp(),x.mp(),x.maxMp(),x.ac(),x.weaponType(),x.attackSequence(),
                ModernRegenProtocolTest.event(x.attackRevision()),ModernRegenProtocolTest.event(x.hurtRevision()),ModernRegenProtocolTest.event(x.deathRevision()),x.lastTargetEntityId(),x.attackIntervalNanos())).toList();
        return new ModernCombatProtocol.Frame(world,v,s.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),s.items().stream().map(ModernItemProtocol.State::wireRevision).toList(),s.weights(),
                s.buffs().stream().map(ModernTimedEffects.State::wireRevision).toList(),s.ammo().stream().map(ModernAmmo.State::wireRevision).toList(),s.regen().stream().map(ModernRegenProtocol.State::wireRevision).toList(),s.honor().wireRevision(),
                s.fireStorms().stream().map(ModernFireStormProtocol.State::wireRevision).toList());
    }
    static void roundtrip(ModernBattleEngineHonorTest.Fixture f,ModernBattleEngine.Snapshot s)throws Exception{
        var r=new ModernCombatProtocol.Request(f.players.get(0).scope(),ModernCombatProtocol.Action.READ,s.matchId(),s.generation(),0,0);
        var message=new ModernApplicationProtocol.CombatResult(9,r,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame(s)));
        var bytes=ModernApplicationProtocol.encode(message,14);assertEquals(message,ModernApplicationProtocol.decode(bytes,14));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),14));
        for(int v=1;v<14;v++){int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,version));}
    }
    @Test void singleAtomicBatchKillsEnemyHumanAndBotNotFriendAndProducesExactlyOneWholeRosterResult()throws Exception{
        try(var f=fixture(false,2,100,40)){var before=f.active();roundtrip(f,before);var reply=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());var after=reply.snapshot();
            assertEquals(ModernBattleEngine.Phase.FINISHED,after.phase());var event=after.fireStorms().get(0);assertEquals(List.of(2L,4L),event.targets().stream().map(ModernFireStormProtocol.Target::entityId).toList());
            assertTrue(event.targets().stream().allMatch(t->t.hpAfter()==0));assertEquals(before.vitals().get(2).hp(),after.vitals().get(2).hp());
            assertEquals(after.revision(),after.vitals().get(1).deathRevision());assertEquals(after.revision(),after.vitals().get(3).deathRevision());assertEquals(after.revision(),event.castRevision());
            assertEquals(300-event.mpCost(),after.vitals().get(0).mp());assertEquals(4,after.result().participants().size());assertTrue(after.result().hasBots());
            assertEquals(before.items(),after.items());assertEquals(before.damages(),after.damages());assertEquals(0,after.casts().get(0).castCount());roundtrip(f,after);assertEquals(after,f.engine.tick());}
    }
    @Test void zeroTargetIsOneRealCastAndTwoSecondReuseDoesNotReplaceSharedCastSequence()throws Exception{
        try(var f=fixture(true,0,1000,1000)){f.active();for(int n=1;n<=4;n++){f.elapsed(n*SECOND);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,n,6).status());}
            f.elapsed(5*SECOND);var before=f.engine.tick();int draws=f.draws;var reply=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());var after=reply.snapshot();
            assertTrue(after.fireStorms().get(0).targets().isEmpty());assertEquals(draws,f.draws);assertEquals(1,after.fireStorms().get(0).castCount());assertEquals(before.honor(),after.honor());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,70,0).reason());assertThrows(SecurityException.class,()->f.cast(0,2,70,0));
            f.elapsed(7*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,70,0).reason());assertEquals(2,f.engine.tick().fireStorms().get(0).castCount());roundtrip(f,f.engine.tick());}
    }
    @Test void honorPcAndObjectiveShareReceiptRevisionThenRespawnRetainsDamageMpAndCastHistory()throws Exception{
        try(var f=fixture(true,0,1000,40)){f.active();var first=f.cast(0,1,70,0).snapshot();assertEquals(ModernBattleEngine.Phase.ACTIVE,first.phase());
            assertEquals(List.of(2L,4L),first.fireStorms().get(0).targets().stream().map(ModernFireStormProtocol.Target::entityId).toList());assertEquals(4,first.honor().objectives().get(1).lastKind());
            assertEquals(0,first.vitals().get(1).hp());assertTrue(first.honor().objectives().get(1).hp()>0);roundtrip(f,first);
            f.elapsed(5*SECOND);var revived=f.engine.tick();assertEquals(40,revived.vitals().get(1).hp());assertEquals(first.fireStorms(),revived.fireStorms());assertEquals(1,revived.honor().lives().get(1).respawnCount());roundtrip(f,revived);
            var second=f.cast(0,2,70,0).snapshot();assertEquals(1,second.fireStorms().get(0).targets().get(0).respawnCount());assertEquals(80,second.regen().get(1).damageLoss());roundtrip(f,second);}
    }
    @Test void lastTargetNumericFailureCancelsEntireBatchAndDoesNotSpendEarlierTargets()throws Exception{
        try(var f=fixture(true,0,1000,1000)){var before=f.active();f.invalidAt=10;var result=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NUMERIC,result.reason());assertEquals(10,f.draws);
            var after=result.snapshot();assertEquals(before.vitals(),after.vitals());assertEquals(before.honor(),after.honor());assertEquals(before.fireStorms(),after.fireStorms());assertEquals(1,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
    @Test void lateInvalidAndCancelledRandomAbortWithoutSpendingOrPartialTargetDamage()throws Exception{
        for(int mode=0;mode<3;mode++)try(var f=fixture(true,0,1000,1000)){var before=f.active();if(mode<2){f.jump=1800*SECOND;f.invalid=mode==1;}else f.cancel=true;
            var result=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE,result.reason());var after=result.snapshot();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(before.vitals(),after.vitals());assertEquals(before.fireStorms(),after.fireStorms());assertEquals(before.honor().objectives(),after.honor().objectives());assertEquals(0,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
    @Test void legacyPreparedMembersHaveNoFireAuthorityAndNeverConsumeTheirMp()throws Exception{
        try(var f=new ModernBattleEngineHonorTest.Fixture()){var before=f.active();var denied=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.UNLEARNED,denied.reason());assertTrue(denied.snapshot().fireStorms().isEmpty());assertEquals(before.vitals(),denied.snapshot().vitals());}
    }
    @Test void postRandomCasterHasteExpiryRejectsEntireBatchWithoutAnotherRandomPass()throws Exception{
        try(var f=fixture(true,0,1000,1000)){f.active();var p=f.players.get(0);var use=new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40013);
            assertEquals(ModernItemProtocol.Reason.NONE,f.engine.item(p.owner(),use).reason());f.elapsed(300*SECOND-1);var before=f.engine.tick();int draws=f.draws;f.jump=1;
            var reply=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.STALE_EFFECT,reply.reason());var after=reply.snapshot();
            assertEquals(before.vitals(),after.vitals());assertEquals(before.honor(),after.honor());assertEquals(before.fireStorms(),after.fireStorms());assertFalse(after.buffs().get(0).hasEffects());
            assertEquals(18,f.draws-draws);assertEquals(1,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
    @Test void casterMovementDuringRandomCannotPublishOldRadiusAtANewOrigin()throws Exception {
        try(var f=fixture(false,0,1000,1000)) {
            var before=f.active();var once=new java.util.concurrent.atomic.AtomicBoolean();
            f.randomOverride=b->{if(!once.getAndSet(true))assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,0).status());return b-1;};
            var reply=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.STALE_REGEN,reply.reason());
            assertEquals(before.vitals(),reply.snapshot().vitals());assertEquals(before.fireStorms(),reply.snapshot().fireStorms());
            assertNotEquals(before.actors().get(0).position(),reply.snapshot().actors().get(0).position());
            assertEquals(1,reply.snapshot().casts().get(0).requestSequence());
        }
    }
    @Test void casterDeathAndRespawnDuringRandomCannotPublishAsTheNewLife()throws Exception {
        try(var f=fixture(true,0,1000,1)) {
            var before=f.active();var once=new java.util.concurrent.atomic.AtomicBoolean();
            f.randomOverride=b->{if(!once.getAndSet(true)) {
                assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
                f.elapsed(5*SECOND);var respawn=f.engine.tick();
                assertEquals(1,respawn.honor().lives().get(1).respawnCount());
                assertEquals(before.vitals().get(1).hp(),respawn.vitals().get(1).hp());
            }return b-1;};
            var reply=f.cast(1,1,70,0);assertEquals(ModernSpellProtocol.Reason.STALE_REGEN,reply.reason());
            assertEquals(before.fireStorms(),reply.snapshot().fireStorms());
            assertEquals(before.vitals().get(1).mp(),reply.snapshot().vitals().get(1).mp());
            assertEquals(1,reply.snapshot().casts().get(1).requestSequence());
        }
    }
    @Test void postRandomTargetHpRegenCommitsOnlyRecoveryAndNeverOverwritesItWithOldBatchHp()throws Exception{
        try(var f=fixture(true,0,1000,1000)){f.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());f.elapsed(2*SECOND-1);var before=f.engine.tick();f.jump=1;
            var reply=f.cast(0,2,70,0);assertEquals(ModernSpellProtocol.Reason.STALE_REGEN,reply.reason());var after=reply.snapshot();
            assertEquals(before.vitals().get(0).mp(),after.vitals().get(0).mp());assertTrue(after.vitals().get(1).hp()>before.vitals().get(1).hp());
            assertEquals(before.regen().get(1).damageLoss(),after.regen().get(1).damageLoss());assertEquals(before.honor().objectives(),after.honor().objectives());
            assertEquals(before.fireStorms(),after.fireStorms());assertEquals(2,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
}
