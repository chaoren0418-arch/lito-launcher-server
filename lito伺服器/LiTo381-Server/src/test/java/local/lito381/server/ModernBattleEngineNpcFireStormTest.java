package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Internal engine transactions; no production wire or UI feature claim. */
class ModernBattleEngineNpcFireStormTest {
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,boolean learned,boolean disintegrate) {
        if(disintegrate)p=ModernBattleEngineNpcDisintegrateTest.enable(p,ModernDisintegrateCatalog.Status.QUALIFIED);
        var skills=new ArrayList<ModernSkills.Skill>();
        if(p.skills()!=null)skills.addAll(p.skills().skills());
        else {skills.add(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW));skills.add(ModernSkillsTest.teleport());}
        if(learned)skills.add(new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM));
        skills.sort(java.util.Comparator.comparingInt(ModernSkills.Skill::skillId));
        var next=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),1_200_000_000,false,p.matchItems(),p.weightBasis(),false,false,true);
        return disintegrate?next.withNpcDisintegrate(p.npcDisintegrate()):next;
    }
    static ModernBattleEngineNpcTest.Fixture fixture()throws Exception {return fixture(1000,10000,0,true,false,false,0);}
    static ModernBattleEngineNpcTest.Fixture fixture(long mp,long npcHp,long mr,boolean learned,boolean disintegrate,boolean quest,int bots)throws Exception {
        return fixture(mp,npcHp,mr,learned,disintegrate,quest,bots,100);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(long mp,long npcHp,long mr,boolean learned,boolean disintegrate,boolean quest,int bots,long hp)throws Exception {
        var catalog=ModernNpcMeleeTest.catalog(Map.of(HP,npcHp,MR,mr),true,0);
        var f=new ModernBattleEngineNpcTest.Fixture(2,bots,true,hp,0,s->ModernBattleEngineNpcDisintegrateTest.source(s,mp,500),catalog,false);
        try {
            ModernQuestProgress.Plan plan=null;
            if(quest){var base=ModernQuestProgressTest.plan(f.binding.key(),catalog);
                plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();}
            var cfg=new ModernNpcBattle.Config(f.config.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,plan,true)
                    .withDisintegrate(disintegrate).withFireStorm(true);
            f.engine=f.build(cfg,quest,f.prepared.stream().map(p->enable(p,learned,disintegrate)).toList());return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernNpcFireStormState.Request request(ModernBattleEngineNpcTest.Fixture f,long sequence) {
        var lives=f.view().world().quest().lives();return new ModernNpcFireStormState.Request(f.players.get(0).scope(),f.scope,sequence,lives.isEmpty()?1:lives.get(0).incarnation());
    }
    static ModernNpcFireStormState.Result cast(ModernBattleEngineNpcTest.Fixture f,long sequence) {
        return f.engine.fireStormNpc(f.players.get(0).owner(),request(f,sequence));
    }
    static ModernNpcFireStormState.State state(ModernNpcBattle.Frame f){return f.world().npcFireStorms().get(0);}
    static void noCast(ModernNpcBattle.Frame before,ModernNpcBattle.Frame after) {
        assertEquals(before.world().npcFireStorms(),after.world().npcFireStorms());
        assertEquals(before.world().vitals().get(0).mp(),after.world().vitals().get(0).mp());
    }
    @Test void twoVictimsCommitWithOneManaDebitAndTypedIdentityNotPlayerAlias()throws Exception {
        try(var f=fixture()) {
            f.active();var a=f.spawn(0,11,10);var b=f.spawn(0,10,11);var other=f.spawn(1,9,10);var far=f.spawn(0,14,10);var before=f.view();
            var result=cast(f,1);assertEquals(NONE,result.reason());var after=result.frame();var s=state(after);
            assertEquals(before.world().revision()+1,after.world().revision());assertEquals(after.world().revision(),s.revision());
            assertEquals(List.of(a.id(),b.id()),s.targets().stream().map(ModernNpcFireStormState.Target::id).toList());
            assertEquals(1,a.id().serial());assertEquals(1,s.entityId());assertEquals(1,s.castCount());assertEquals(48,s.mpSpent());
            assertEquals(952,after.world().vitals().get(0).mp());assertEquals(before.world().vitals().get(1),after.world().vitals().get(1));
            assertEquals(List.of(9965L,9965L,other.hp(),far.hp()),after.npcs().actors().stream().map(ModernNpcRoster.Actor::hp).toList());
            assertEquals(18,f.draws.get());assertTrue(after.world().fireStorms().isEmpty());assertEquals(0,after.world().npcSpells().get(0).castCount());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(after.world(),null));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(after.world(),after.world().revision()+1,ModernNpcTeleportTest.descriptor()));
            assertThrows(UnsupportedOperationException.class,()->s.targets().clear());
        }
    }
    @Test void reuseSharesPaceAndSequenceWithArrowDisintegrateTeleportMoveAndMelee()throws Exception {
        try(var f=fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);assertEquals(NONE,cast(f,1).reason());
            assertEquals(TOO_FAST,cast(f,2).reason());assertEquals(ModernNpcMagic.Reason.TOO_FAST,ModernBattleEngineNpcMagicTest.cast(f,0,3,n).reason());
            assertEquals(ModernNpcDisintegrateState.Reason.TOO_FAST,ModernBattleEngineNpcDisintegrateTest.cast(f,0,4,n).reason());
            assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.attack(0,1,n).reason());assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,10,10,0).status());
            f.elapsed(1200);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,5,n).reason());
            f.elapsed(2000);assertEquals(TOO_FAST,cast(f,6).reason());f.elapsed(2400);assertEquals(NONE,cast(f,7).reason());
            f.elapsed(3600);assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,8,n).reason());
            f.elapsed(4800);assertEquals(ModernSpellProtocol.Reason.NONE,ModernNpcTeleportTest.cast(f,0,9).reason());
            assertEquals(1000-96-3-70-10,f.view().world().vitals().get(0).mp());assertEquals(9,f.view().world().casts().get(0).requestSequence());
            assertThrows(SecurityException.class,()->cast(f,9));
        }
    }
    @Test void emptyResistedAndBotLedgersStayValidWithoutFakeVictimAnimations()throws Exception {
        for(boolean empty:List.of(false,true))try(var f=fixture(1000,10000,Long.MAX_VALUE,true,false,false,1)) {
            f.active();if(!empty)f.spawn(0,11,10);var before=f.view();assertEquals(NONE,cast(f,1).reason());var after=f.view();
            assertEquals(before.npcs().actors(),after.npcs().actors());assertEquals(952,after.world().vitals().get(0).mp());
            assertEquals(empty?0:1,state(after).targets().size());
            assertEquals(ModernNpcFireStormState.State.initial(3,0),after.world().npcFireStorms().get(2));
            assertEquals(TOO_FAST,cast(f,2).reason());
        }
    }
    @Test void unlearnedAndNoManaRejectBeforeEntropyAndDoNotStartReuse()throws Exception {
        for(boolean learned:List.of(false,true))try(var f=fixture(learned?47:1000,10000,0,learned,false,false,0)) {
            f.active();f.spawn(0,11,10);var before=f.view();assertEquals(learned?NO_MP:UNLEARNED,cast(f,1).reason());
            noCast(before,f.view());assertEquals(before.npcs(),f.view().npcs());assertEquals(0,f.draws.get());
        }
    }
    @Test void invalidSecondVictimEntropyCannotHalfCommitFirstVictimOrMana()throws Exception {
        try(var f=fixture()) {
            f.active();f.spawn(0,11,10);f.spawn(0,10,11);var before=f.view();f.scripted=new long[18];f.scripted[17]=-1;
            assertEquals(NUMERIC,cast(f,1).reason());noCast(before,f.view());assertEquals(before.npcs(),f.view().npcs());
            f.scripted=null;assertEquals(NONE,cast(f,2).reason());assertEquals(1,state(f.view()).castCount());
        }
    }
    @Test void foreignOwnerWorldLifeAndReplayCannotConsumeSequenceOrEntropy()throws Exception {
        try(var f=fixture()) {
            f.active();f.spawn(0,11,10);var req=request(f,1);var before=f.view();var p=f.players.get(0);
            assertThrows(SecurityException.class,()->f.engine.fireStormNpc(f.players.get(1).owner(),req));
            assertThrows(SecurityException.class,()->f.engine.fireStormNpc(p.owner(),new ModernNpcFireStormState.Request(p.scope(),new ModernNpcRoster.Scope(f.scope.roomId()+1,f.scope.matchId(),f.scope.generation()),1,1)));
            assertThrows(SecurityException.class,()->f.engine.fireStormNpc(p.owner(),new ModernNpcFireStormState.Request(p.scope(),f.scope,1,2)));
            assertThrows(SecurityException.class,()->cast(f,2));assertEquals(before,f.view());assertEquals(0,f.draws.get());
            assertEquals(NONE,cast(f,1).reason());var after=f.view();assertThrows(SecurityException.class,()->cast(f,1));assertEquals(after,f.view());
        }
    }
    @Test void nestedMovementAttackAndNpcAttackInvalidateOnlyOuterBatch()throws Exception {
        for(int kind=0;kind<3;kind++)try(var f=fixture()) {
            int k=kind;f.active();var n=f.spawn(0,11,10);f.spawn(0,10,11);f.elapsed(100);var before=f.view();
            f.duringRandom=()->{if(k==0)assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
                else if(k==1)assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(0,1,n).reason());
                else assertEquals(ModernNpcBattle.Reason.ATTACKED,f.step(n).reason());};
            var result=cast(f,1);assertEquals(STALE_STATE,result.reason());noCast(before,result.frame());
            assertEquals(10000,result.frame().npcs().actors().get(1).hp());
        }
    }
    @Test void duplicateNestedAndConcurrentRequestsDebitOnlyOnce()throws Exception {
        try(var f=fixture()) {
            f.active();f.spawn(0,11,10);f.duringRandom=()->assertEquals(NONE,cast(f,1).reason());
            assertEquals(STALE_STATE,cast(f,1).reason());assertEquals(1,state(f.view()).castCount());assertEquals(952,f.view().world().vitals().get(0).mp());
        }
        try(var f=fixture()) {
            f.active();f.spawn(0,11,10);var pool=Executors.newFixedThreadPool(2);
            try {java.util.concurrent.Callable<Boolean> work=()->{try{return cast(f,1).reason()==NONE;}catch(SecurityException stale){return false;}};
                var a=pool.submit(work);var b=pool.submit(work);assertNotEquals(a.get(2,TimeUnit.SECONDS),b.get(2,TimeUnit.SECONDS));
                assertEquals(1,state(f.view()).castCount());assertEquals(952,f.view().world().vitals().get(0).mp());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }
    @Test void timeoutAndCancelledRoomCannotPublishLateDamage()throws Exception {
        for(boolean timeout:List.of(false,true))try(var f=fixture()) {
            f.active();f.spawn(0,11,10);var before=f.view();f.duringRandom=()->{if(timeout)f.elapsed(60_000);else f.rooms.releasePreparation(f.ticket);};
            var result=cast(f,1);assertEquals(NOT_ACTIVE,result.reason());noCast(before,result.frame());
            assertEquals(before.world().casts(),result.frame().world().casts());assertEquals(ModernBattleEngine.Phase.ABORTED,result.frame().world().phase());
            assertTrue(result.frame().npcs().closed());assertTrue(result.frame().npcs().actors().isEmpty());
        }
    }
    @Test void questWaveKillLeavesIndependentEnemyLaneAndNeverInventsAnObjectiveWin()throws Exception {
        try(var f=fixture(1000,1,0,true,false,true,0)) {
            f.active();var before=f.view();assertEquals(NONE,cast(f,1).reason());var after=f.view();
            assertTrue(state(after).targets().get(0).outcome().death());
            assertEquals(before.npcs().actors().stream().filter(a->a.lane()==1).toList(),after.npcs().actors().stream().filter(a->a.lane()==1).toList());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,after.world().phase());assertNull(after.world().result());
            f.elapsed(1001);assertFalse(f.view().npcs().actors().stream().anyMatch(a->a.id().equals(state(after).targets().get(0).id())));
        }
    }
    @Test void clockOnlyAdvanceIsSafeButRegeneratedManaIsNeverOverwritten()throws Exception {
        try(var f=fixture()) {
            f.active();f.spawn(0,11,10);f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);
            assertEquals(NONE,cast(f,1).reason());assertEquals(1,f.view().npcs().elapsedMillis());
            f.elapsed(15999);var before=f.view();f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);
            var result=cast(f,2);assertEquals(STALE_REGEN,result.reason());assertEquals(state(before),state(result.frame()));
            assertTrue(result.frame().world().vitals().get(0).mp()>before.world().vitals().get(0).mp());
            assertEquals(before.npcs().actors(),result.frame().npcs().actors());
        }
    }
    @Test void lateSpawnDoesNotJoinCapturedBatchAndBlockedCornerUsesExplicitModernLosPolicy()throws Exception {
        try(var f=fixture()) {
            f.active();var first=f.spawn(0,11,10);f.duringRandom=()->f.spawn(0,9,10);
            assertEquals(NONE,cast(f,1).reason());var after=f.view();
            assertEquals(List.of(first.id()),state(after).targets().stream().map(ModernNpcFireStormState.Target::id).toList());
            assertEquals(10000,after.npcs().actors().get(1).hp());
        }
        try(var f=fixture()) {
            f.active();var diagonal=f.spawn(0,11,9);var north=f.spawn(0,10,9);var east=f.spawn(0,11,10);
            assertEquals(NONE,cast(f,1).reason());var after=f.view();
            assertEquals(List.of(north.id(),east.id()),state(after).targets().stream().map(ModernNpcFireStormState.Target::id).toList());
            assertEquals(diagonal,after.npcs().actors().get(0));
        }
    }
    @Test void hpBeyondBinary64ExactRangeRemainsExactIntegerAfterAreaDamage()throws Exception {
        try(var f=fixture(1000,9007199254740993L,0,true,false,false,0)) {
            f.active();f.spawn(0,11,10);f.spawn(0,10,11);assertEquals(NONE,cast(f,1).reason());
            assertEquals(List.of(9007199254740958L,9007199254740958L),f.view().npcs().actors().stream().map(ModernNpcRoster.Actor::hp).toList());
        }
    }
    @Test void questDeathRespawnPreservesSpentManaAndRequiresFreshCasterLife()throws Exception {
        try(var f=fixture(1000,10000,0,true,false,true,0,1)) {
            f.active();assertEquals(NONE,cast(f,1).reason());var first=state(f.view());var old=request(f,3);
            var dead=ModernQuestLifeTest.die(f,100);assertEquals(DEAD,cast(f,2).reason());
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            assertThrows(SecurityException.class,()->f.engine.fireStormNpc(f.players.get(0).owner(),old));assertEquals(first,state(f.view()));
            f.elapsed(2000);assertEquals(NONE,cast(f,3).reason());assertEquals(2,state(f.view()).casterLife());
            assertEquals(96,state(f.view()).mpSpent());assertEquals(904,f.view().world().vitals().get(0).mp());
        }
    }
    @Test void disabledConfigurationAndUnqualifiedMembersCannotGainTheInternalFeature()throws Exception {
        try(var f=ModernBattleEngineNpcMagicTest.fixture()) {
            f.active();var before=f.view();assertEquals(NOT_ACTIVE,cast(f,1).reason());assertEquals(before,f.view());
        }
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            assertThrows(IllegalArgumentException.class,()->f.build(f.config.withFireStorm(true)));
        }
    }
}
