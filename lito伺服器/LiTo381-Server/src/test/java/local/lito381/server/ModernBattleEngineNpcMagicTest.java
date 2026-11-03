package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcMagic.Reason.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Real frozen room capability, one engine and separate human/NPC identities. */
class ModernBattleEngineNpcMagicTest {
    static ModernPhysicalCombat.Source magic(ModernPhysicalCombat.Source source) {
        return new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
                ModernMagicCombatTest.growth(source.growth().sourceId()),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth());
    }
    static ModernBattleEngineNpcTest.Fixture fixture()throws Exception {
        return fixture(false,true,100,2,0,true,ModernNpcMeleeTest.catalog(),UnaryOperator.identity());
    }
    static ModernBattleEngineNpcTest.Fixture fixture(boolean quest,boolean regen,long hp,int humans,int bots,boolean learned,
            ModernNpcCatalog.Catalog catalog,UnaryOperator<ModernPhysicalCombat.Source> transform)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(humans,bots,regen,hp,0,s->magic(transform.apply(s)),catalog,false);
        try {
            var arrow=new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW);
            var prepared=f.prepared.stream().map(p->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),learned?List.of(arrow,ModernSkillsTest.teleport()):List.of(ModernSkillsTest.teleport())),
                    1200*ModernBattleEngineNpcTest.MS,false,p.matchItems(),p.weightBasis(),false,false,regen)).toList();
            ModernQuestProgress.Plan plan=null;
            if(quest){var base=ModernQuestProgressTest.plan(f.binding.key(),catalog);
                plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();}
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,plan,true),quest,prepared);
            return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernNpcMagic.Result cast(ModernBattleEngineNpcTest.Fixture f,int player,long sequence,ModernNpcRoster.Actor target) {
        var p=f.players.get(player);var lives=f.view().world().quest().lives();long life=lives.isEmpty()?1:lives.get(player).incarnation();
        return f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,life,target.id(),target.life());
    }
    static ModernNpcMagic.CastState state(ModernNpcBattle.Frame frame){return frame.world().npcSpells().get(0);}
    static void noCast(ModernNpcBattle.Frame before,ModernNpcBattle.Frame after) {
        assertEquals(state(before),state(after));assertEquals(before.world().vitals().get(0).mp(),after.world().vitals().get(0).mp());
    }
    @Test void atomicMpNpcHpAndTypedReceiptDoNotAliasPvpOrTeleport()throws Exception {
        for(boolean regen:List.of(false,true))try(var f=fixture(false,regen,100,2,0,true,ModernNpcMeleeTest.catalog(),UnaryOperator.identity())) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();var result=cast(f,0,1,npc);assertEquals(NONE,result.reason());var after=result.frame();var s=state(after);
            assertEquals(before.world().revision()+1,after.world().revision());assertEquals(after.world().revision(),s.revision());
            assertEquals(npc.id(),s.target());assertEquals(1,s.casterLife());assertEquals(1,s.targetLife());assertEquals(0,s.targetLane());
            assertEquals(before.world().actors().get(0).position(),s.from());assertEquals(npc.cell(),s.to());assertEquals(1,s.castCount());
            assertEquals(before.world().vitals().get(0).mp()-s.mpCost(),after.world().vitals().get(0).mp());assertEquals(s.mpCost(),s.mpSpent());
            assertEquals(npc.hp()-s.outcome().appliedDamage(),after.npcs().actors().get(0).hp());assertTrue(s.outcome().appliedDamage()>0);
            assertEquals(1,after.world().casts().get(0).requestSequence());assertEquals(0,after.world().casts().get(0).castCount());assertEquals(0,after.world().casts().get(0).mpSpent());
            assertEquals(0,after.world().vitals().get(0).lastTargetEntityId());assertEquals(0,after.world().vitals().get(0).attackSequence());
            assertEquals(before.world().vitals().get(1),after.world().vitals().get(1));assertTrue(after.world().damages().isEmpty());assertTrue(after.exchanges().isEmpty());
            assertThrows(UnsupportedOperationException.class,()->after.world().npcSpells().clear());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(after.world(),after.world().revision()+1,ModernNpcTeleportTest.descriptor()));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(after.world(),null));
        }
    }
    @Test void sharedSpellSequenceAndAllMovementMeleeCooldownsRemainCoherent()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var first=cast(f,0,1,npc);assertEquals(NONE,first.reason());
            assertEquals(TOO_FAST,cast(f,0,2,npc).reason());assertEquals(ModernSpellProtocol.Reason.TOO_FAST,ModernNpcTeleportTest.cast(f,0,3).reason());
            assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.attack(0,1,npc).reason());assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,10,10,0).status());
            assertEquals(state(first.frame()),state(f.view()));f.elapsed(1199);assertEquals(TOO_FAST,cast(f,0,4,npc).reason());
            f.elapsed(1200);assertEquals(NONE,cast(f,0,5,npc).reason());assertEquals(2,state(f.view()).castCount());
            f.elapsed(2400);assertEquals(ModernSpellProtocol.Reason.NONE,ModernNpcTeleportTest.cast(f,0,6).reason());
            var after=f.view();assertEquals(1,after.world().casts().get(0).castCount());assertEquals(6,after.world().casts().get(0).requestSequence());
            assertEquals(30-10-state(after).mpSpent(),after.world().vitals().get(0).mp());
        }
    }
    @Test void currentV23TeleportCannotBypassNpcMeleeCooldownWithoutMagicOptIn()throws Exception {
        try(var f=ModernNpcTeleportTest.fixture(true,false)) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,ModernNpcTeleportTest.cast(f,0,1).reason());
            var from=f.view().world().actors().get(0).position();var npc=f.spawn(0,new ModernWorldMap.Cell(from.x()+1,from.y()));
            assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.attack(0,1,npc).reason());f.elapsed(1199);assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.attack(0,2,npc).reason());
            f.elapsed(1200);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(0,3,npc).reason());assertTrue(f.view().world().npcSpells().isEmpty());
        }
    }
    @Test void ownerSourceEpochChannelMatchNpcScopeLifeAndReplayRejectWithoutMutation()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var p=f.players.get(0);var q=f.players.get(1);var before=f.view();
            assertThrows(SecurityException.class,()->f.engine.castNpc(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,1,npc.id(),1));
            for(var scope:List.of(q.scope(),new ModernRooms.Scope(7,1,2,0),new ModernRooms.Scope(7,1,1,1)))
                assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),scope,f.ticket.matchId(),f.ticket.generation(),1,1,npc.id(),1));
            assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId()+1,f.ticket.generation(),1,1,npc.id(),1));
            assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1,1,1,npc.id(),1));
            assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,2,npc.id(),1));
            assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,1,npc.id(),2));
            assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,1,new ModernNpcRoster.Id(new ModernNpcRoster.Scope(f.scope.roomId()+1,f.scope.matchId(),f.scope.generation()),1),1));
            assertThrows(SecurityException.class,()->cast(f,0,2,npc));assertEquals(before,f.view());assertEquals(NONE,cast(f,0,1,npc).reason());
            var after=f.view();assertThrows(SecurityException.class,()->cast(f,0,1,npc));assertEquals(after,f.view());
        }
    }
    @Test void unlearnedWrongLaneDistanceAndNpcCornerOcclusionSpendNoManaOrRandom()throws Exception {
        try(var f=fixture(false,true,100,2,0,false,ModernNpcMeleeTest.catalog(),UnaryOperator.identity())) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();assertEquals(UNLEARNED,cast(f,0,1,npc).reason());noCast(before,f.view());assertEquals(0,f.draws.get());
        }
        try(var f=fixture()) {
            f.active();var other=f.spawn(1,12,10);assertEquals(WRONG_LANE,cast(f,0,1,other).reason());
            var far=f.spawn(0,22,10);assertEquals(OUT_OF_RANGE,cast(f,0,2,far).reason());
            var target=f.spawn(0,11,9);f.spawn(0,10,9);f.spawn(0,11,10);f.spawn(0,9,10);
            var before=f.view();assertEquals(BLOCKED,cast(f,0,3,target).reason());noCast(before,f.view());assertEquals(0,f.draws.get());
        }
    }
    @Test void noMpAndInvalidRandomNeverDamageOrStartCooldown()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();f.invalidRandom=true;
            assertEquals(NUMERIC,cast(f,0,1,npc).reason());noCast(before,f.view());assertEquals(before.npcs(),f.view().npcs());
            f.invalidRandom=false;assertEquals(NONE,cast(f,0,2,npc).reason());
        }
        try(var f=fixture(false,false,100,2,0,true,ModernNpcMeleeTest.catalog(),s->ModernMagicCombatTest.stats(s,12,12,0,0,0))) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();assertEquals(NO_MP,cast(f,0,1,npc).reason());noCast(before,f.view());assertEquals(0,f.draws.get());
        }
    }
    @Test void zeroDamagePaysManaWithoutInventingNpcHurtAndI64HpNeverRounds()throws Exception {
        for(long mr:List.of(0L,Long.MAX_VALUE))try(var f=fixture(false,true,100,2,0,true,ModernNpcMeleeTest.catalog(Map.of(HP,9007199254740993L,MR,mr),true,0),UnaryOperator.identity())) {
            f.active();var npc=f.spawn(0,11,10);var result=cast(f,0,1,npc);assertEquals(NONE,result.reason());var s=state(result.frame());
            assertEquals(9007199254740993L-s.outcome().appliedDamage(),result.frame().npcs().actors().get(0).hp());assertTrue(s.mpSpent()>0);
            if(mr==Long.MAX_VALUE){assertEquals(0,s.outcome().appliedDamage());assertEquals(npc,result.frame().npcs().actors().get(0));}
            assertEquals(TOO_FAST,cast(f,0,2,npc).reason());
        }
    }
    @Test void randomMayAdvanceRosterClockWithoutInvalidatingUnchangedActors()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);
            assertEquals(NONE,cast(f,0,1,npc).reason());assertEquals(1,f.view().npcs().elapsedMillis());assertEquals(1,state(f.view()).castCount());
        }
    }
    @Test void randomCancellationTimeoutClockRegressionAndExpiryNeverHalfCommit()throws Exception {
        for(int kind=0;kind<4;kind++)try(var f=fixture(false,false,100,2,0,true,ModernNpcMeleeTest.catalog(),kind==3?ModernBattleEngineNpcTest::expires:UnaryOperator.identity())) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();int k=kind;
            f.duringRandom=()->{if(k==0)f.rooms.releasePreparation(f.ticket);else if(k==1)f.elapsed(60000);else if(k==2)f.clock.decrementAndGet();else f.epoch.set(2000);};
            var denied=cast(f,0,1,npc);assertEquals(NOT_ACTIVE,denied.reason());noCast(before,denied.frame());
            assertEquals(before.world().casts(),denied.frame().world().casts());assertEquals(ModernBattleEngine.Phase.ABORTED,denied.frame().world().phase());
        }
    }
    @Test void reentrantMoveMeleeAndCasterDamageInvalidateWithoutOverwritingNewState()throws Exception {
        for(int kind=0;kind<3;kind++)try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();int k=kind;
            f.duringRandom=()->{if(k==0)assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
                else if(k==1)assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(0,1,npc).reason());
                else assertEquals(ModernNpcBattle.Reason.ATTACKED,f.step(npc).reason());};
            var result=cast(f,0,1,npc);assertEquals(STALE_STATE,result.reason());noCast(before,result.frame());
            if(k==0)assertNotEquals(before.world().actors().get(0).position(),result.frame().world().actors().get(0).position());
            if(k==2)assertTrue(result.frame().world().vitals().get(0).hp()<100);
        }
    }
    @Test void reentrantOrConcurrentDuplicateCastCommitsExactlyOnce()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.duringRandom=()->assertEquals(NONE,cast(f,0,1,npc).reason());
            assertEquals(STALE_STATE,cast(f,0,1,npc).reason());assertEquals(1,state(f.view()).castCount());assertEquals(1,f.view().world().casts().get(0).requestSequence());
        }
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var pool=Executors.newFixedThreadPool(2);
            try {
                var tasks=List.of(pool.submit(()->{try{return cast(f,0,1,npc).reason()==NONE;}catch(SecurityException e){return false;}}),
                        pool.submit(()->{try{return cast(f,0,1,npc).reason()==NONE;}catch(SecurityException e){return false;}}));
                int count=0;for(var task:tasks)if(task.get(2,TimeUnit.SECONDS))count++;assertEquals(1,count);assertEquals(1,state(f.view()).castCount());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }
    @Test void questNpcKillAdvancesOnlyOwnWaveAndReceiptSurvivesCorpseRetirement()throws Exception {
        try(var f=fixture(true,true,100,2,0,true,ModernNpcMeleeTest.catalog(Map.of(HP,1L),true,0),UnaryOperator.identity())) {
            var active=f.active();var other=ModernQuestEngineTest.living(f,1);var npc=ModernQuestEngineTest.living(f,0);
            assertEquals(TARGET_DEAD,cast(f,0,1,other).reason());var result=cast(f,0,2,npc);assertEquals(NONE,result.reason());assertTrue(state(result.frame()).outcome().death());
            assertEquals(1,result.frame().npcs().actors().stream().filter(n->n.lane()==1&&n.alive()).count());assertNull(result.frame().world().result());
            var retained=state(result.frame());f.elapsed(1200);var next=f.view();assertEquals(retained,state(next));
            assertTrue(next.npcs().actors().stream().noneMatch(n->n.id().equals(npc.id())));assertEquals(2,ModernQuestEngineTest.living(f,0).stage());
            assertEquals(1,ModernQuestEngineTest.living(f,1).stage());assertEquals(TARGET_DEAD,cast(f,0,3,npc).reason());
            assertEquals(0,active.world().quest().lives().get(0).deaths());
        }
    }
    @Test void playerDeathRespawnRequiresNewCasterLifeAndKeepsCumulativeManaLedger()throws Exception {
        try(var f=fixture(true,true,1,2,0,true,ModernNpcMeleeTest.catalog(),UnaryOperator.identity())) {
            f.active();var npc=ModernQuestEngineTest.living(f,0);assertEquals(NONE,cast(f,0,1,npc).reason());var first=state(f.view());
            var dead=ModernQuestLifeTest.die(f,100);assertEquals(DEAD,cast(f,0,2,npc).reason());
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            var p=f.players.get(0);assertThrows(SecurityException.class,()->f.engine.castNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),3,1,npc.id(),1));
            assertEquals(first,state(f.view()));f.elapsed(1300);assertEquals(NONE,cast(f,0,3,npc).reason());
            assertEquals(2,state(f.view()).casterLife());assertEquals(2,state(f.view()).castCount());assertEquals(first.mpSpent()*2,state(f.view()).mpSpent());
        }
    }
    @Test void passiveBotHasNoSourceOwnerSkillsOrMagicCostAndDisabledConfigStaysClosed()throws Exception {
        try(var f=fixture(false,true,100,1,1,true,ModernNpcMeleeTest.catalog(),UnaryOperator.identity())) {
            f.active();assertEquals(1,f.engine.ownerCount());assertEquals(ModernNpcMagic.CastState.initial(2,0),f.view().world().npcSpells().get(1));
            var npc=f.spawn(0,11,10);assertEquals(NONE,cast(f,0,1,npc).reason());assertEquals(ModernNpcMagic.CastState.initial(2,0),f.view().world().npcSpells().get(1));
        }
        try(var f=ModernNpcTeleportTest.fixture(true,false)) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();assertEquals(NOT_ACTIVE,cast(f,0,1,npc).reason());assertEquals(before,f.view());assertFalse(f.config.lightArrow());
        }
    }
    @Test void mpRegenerationDuringRandomIsKeptWithoutLateManaDebitOrNpcDamage()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);assertEquals(NONE,cast(f,0,1,npc).reason());f.elapsed(15999);var before=f.view();
            f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);var result=cast(f,0,2,npc);assertEquals(STALE_REGEN,result.reason());
            assertEquals(state(before),state(result.frame()));assertTrue(result.frame().world().vitals().get(0).mp()>before.world().vitals().get(0).mp());
            assertEquals(before.npcs().actors(),result.frame().npcs().actors());assertEquals(2,result.frame().world().casts().get(0).requestSequence());
        }
    }
    @Test void npcMotionDuringRandomInvalidatesCapturedCellEvenWhenClockAdvanceIsOtherwiseAllowed()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,14,10);f.elapsed(100);var before=f.view();
            f.duringRandom=()->assertEquals(ModernNpcBattle.Reason.MOVED,f.step(npc).reason());
            var result=cast(f,0,1,npc);assertEquals(STALE_STATE,result.reason());noCast(before,result.frame());
            assertNotEquals(npc.cell(),result.frame().npcs().actors().get(0).cell());assertEquals(npc.hp(),result.frame().npcs().actors().get(0).hp());
        }
    }
    @Test void fourMagicWavesStillNeedFinalGuardianAndPreserveResultIdentity()throws Exception {
        try(var f=fixture(true,true,100,2,0,true,ModernNpcMeleeTest.catalog(Map.of(HP,1L),true,0),UnaryOperator.identity())) {
            f.active();
            for(int stage=1;stage<=4;stage++) {
                f.elapsed((stage-1)*1200L);var npc=ModernQuestEngineTest.living(f,0);assertEquals(stage,npc.stage());
                assertEquals(NONE,cast(f,0,stage,npc).reason());assertTrue(state(f.view()).outcome().death());assertNull(f.view().world().result());
            }
            var guardian=ModernQuestObjectivesTest.objective(f,0);
            assertEquals(ModernNpcBattle.Reason.TOO_FAST,ModernQuestObjectivesTest.strike(f,0,1,guardian.id()).reason());
            f.elapsed(4800);f.randomValue=19;var finished=ModernQuestObjectivesTest.strike(f,0,2,guardian.id()).frame();
            assertEquals(ModernBattleEngine.Phase.FINISHED,finished.world().phase());assertEquals(4,state(finished).castCount());
            var result=finished.world().result();assertEquals(2,result.mode());assertEquals(ModernMatchSettlement.Verdict.WIN,result.participants().get(0).verdict());
            assertEquals(ModernParticipantId.human(7,1),result.participants().get(0).identity());assertEquals(ModernParticipantId.human(42,1),result.participants().get(1).identity());
            assertEquals(ModernMatchSettlement.Verdict.LOSS,result.participants().get(1).verdict());
            assertEquals(NOT_ACTIVE,cast(f,0,5,ModernQuestEngineTest.living(f,1)).reason());assertEquals(result,f.view().world().result());
        }
    }
    @Test void unsafeAdmissionAndMalformedManaReceiptsAreRejectedBeforeTheyCanHideInOldFrames()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false)) {
            var enabled=new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,null,true);
            assertThrows(IllegalArgumentException.class,()->f.build(enabled));assertTrue(f.ticket.isLive());f.engine=f.build(f.config);
        }
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var after=cast(f,0,1,npc).frame();var s=state(after);var v=after.world().vitals().get(0);var r=after.world().regen().get(0);
            ModernRegenProtocol.validateTotals(r,v,0,0,0,0,0,s.mpSpent());
            assertThrows(IllegalArgumentException.class,()->ModernRegenProtocol.validateTotals(r,v,0,0,0,0,0,0));
            assertThrows(IllegalArgumentException.class,()->ModernRegenProtocol.validateTotals(r,v,0,0,0,0,0,-1));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcMagic.CastState(s.entityId(),s.requestSequence(),s.castCount(),s.revision(),0,s.mpCost(),s.casterLife(),s.target(),s.targetLife(),s.targetLane(),s.from(),s.to(),s.outcome()));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcMagic.CastState(s.entityId(),s.requestSequence(),s.castCount(),s.revision(),s.mpSpent(),s.mpCost(),0,s.target(),s.targetLife(),s.targetLane(),s.from(),s.to(),s.outcome()));
            assertThrows(ArithmeticException.class,()->new ModernNpcMagic.CastState(1,Long.MAX_VALUE,Long.MAX_VALUE,2,Long.MAX_VALUE,3,1,s.target(),1,0,s.from(),s.to(),s.outcome()));
        }
    }
}
