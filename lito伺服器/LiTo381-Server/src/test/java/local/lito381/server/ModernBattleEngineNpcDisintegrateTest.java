package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;
import static local.lito381.server.ModernNpcCatalog.Field.HP;
import static local.lito381.server.ModernNpcCatalog.Field.MR;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Frozen real room tickets and one combat engine. No wire/UI completion claim. */
class ModernBattleEngineNpcDisintegrateTest {
    static ModernPhysicalCombat.Source source(ModernPhysicalCombat.Source s,long mp,long lawful) {
        var q=s.input().equipment();var p=q.source().persisted();
        var values=new ModernCharacterData.PersistedValues(p.maxHp(),mp,p.savedHp(),mp,p.armorClass(),p.strength(),p.dexterity(),p.constitution(),
                12,12,p.charisma(),p.experience(),lawful,p.originalStrength(),p.originalDexterity(),p.originalConstitution(),0,0,p.food225());
        var snapshot=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),values,q.source().inventory());
        var qualified=new ModernEquipmentFacts.Qualified(snapshot,q.items(),q.catalog(),q.lifetimes());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,s.input().weaponsQualified(),s.input().weapons()),
                s.growth(),s.templates(),s.procCatalogIds(),ModernMagicCombatTest.growth(s.growth().sourceId()),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,ModernDisintegrateCatalog.Status status) {
        var skills=new ArrayList<ModernSkills.Skill>();skills.add(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW));
        skills.add(ModernSkillsTest.teleport());
        if(status!=ModernDisintegrateCatalog.Status.UNLEARNED)skills.add(new ModernSkills.Skill(77,"$1485",70,5000,
                status==ModernDisintegrateCatalog.Status.QUALIFIED?ModernSkills.Support.DISINTEGRATE:ModernSkills.Support.UNSUPPORTED_TEMPLATE));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),1200*ModernBattleEngineNpcTest.MS,false,
                p.matchItems(),p.weightBasis(),false,false,true).withNpcDisintegrate(new ModernDisintegrateCatalog.Snapshot(p.fighter().key(),status));
    }
    static ModernPhysicalCombat.Source expiring(ModernPhysicalCombat.Source s) {
        var e=ModernBattleEngineNpcTest.expires(s);
        return new ModernPhysicalCombat.Source(e.input(),e.growth(),e.templates(),e.procCatalogIds(),s.magicGrowth(),s.magicBows(),
                s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    static ModernBattleEngineNpcTest.Fixture fixture()throws Exception {
        return fixture(false,100,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(Map.of(HP,10000L,MR,0L),true,0),s->source(s,1000,500));
    }
    static ModernBattleEngineNpcTest.Fixture fixture(boolean quest,long hp,int humans,int bots,boolean arrow,
            ModernDisintegrateCatalog.Status status,ModernNpcCatalog.Catalog catalog,UnaryOperator<ModernPhysicalCombat.Source> source)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(humans,bots,true,hp,0,source,catalog,false);
        try {
            var prepared=f.prepared.stream().map(p->enable(p,status)).toList();ModernQuestProgress.Plan plan=null;
            if(quest){var base=ModernQuestProgressTest.plan(f.binding.key(),catalog);
                plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();}
            var config=new ModernNpcBattle.Config(f.config.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,plan,arrow).withDisintegrate(true);
            f.engine=f.build(config,quest,prepared);return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernNpcDisintegrateState.Request request(ModernBattleEngineNpcTest.Fixture f,int who,long sequence,ModernNpcRoster.Actor target) {
        var life=f.view().world().quest().lives();return new ModernNpcDisintegrateState.Request(f.players.get(who).scope(),target.id(),sequence,life.isEmpty()?1:life.get(who).incarnation(),target.life());
    }
    static ModernNpcDisintegrateState.Result cast(ModernBattleEngineNpcTest.Fixture f,int who,long sequence,ModernNpcRoster.Actor target) {
        return f.engine.disintegrateNpc(f.players.get(who).owner(),request(f,who,sequence,target));
    }
    static ModernNpcDisintegrateState.State state(ModernNpcBattle.Frame frame){return frame.world().npcDisintegrations().get(0);}
    static void noCast(ModernNpcBattle.Frame before,ModernNpcBattle.Frame after) {
        assertEquals(before.world().npcDisintegrations(),after.world().npcDisintegrations());
        assertEquals(before.world().vitals().get(0).mp(),after.world().vitals().get(0).mp());
    }
    @Test void atomicNpcHpManaLawfulAndCriticalReceiptNeverAliasTheEqualPlayerEntity()throws Exception {
        for(boolean critical:List.of(false,true))try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();f.randomValue=critical?0:99;
            var result=cast(f,0,1,npc);assertEquals(NONE,result.reason());var after=result.frame();var s=state(after);var h=s.hit();
            assertEquals(1,npc.id().serial());assertEquals(1,s.entityId(),"equal numbers remain different namespaces");
            assertEquals(before.world().revision()+1,after.world().revision());assertEquals(after.world().revision(),s.revision());
            assertEquals(1,s.castCount());assertEquals(1,s.lastSequence());assertEquals(70,s.mpSpent());assertEquals(930,after.world().vitals().get(0).mp());
            assertEquals(500,s.initialLawful());assertEquals(501,s.lawful());assertEquals(500,f.prepared.get(0).fighter().profile().source().persisted().lawful());
            assertEquals(npc.id(),h.target());assertEquals(npc.life(),h.targetLife());assertEquals(1,h.casterLife());
            assertEquals(critical,h.outcome().critical());assertEquals(critical?11748:11746,h.outcome().effect());
            assertEquals(critical?432:339,h.outcome().rawDamage()); // max ordinary dice vs minimum critical dice
            assertEquals(npc.hp()-h.outcome().appliedDamage(),after.npcs().actors().get(0).hp());
            assertEquals(before.world().npcSpells(),after.world().npcSpells());assertTrue(after.world().disintegrations().isEmpty());
            assertEquals(0,after.world().casts().get(0).castCount());assertEquals(1,after.world().casts().get(0).requestSequence());
            assertEquals(0,after.world().vitals().get(0).lastTargetEntityId());assertEquals(before.world().vitals().get(1),after.world().vitals().get(1));
            assertThrows(UnsupportedOperationException.class,()->after.world().npcDisintegrations().clear());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(after.world(),after.world().revision()+1,ModernNpcTeleportTest.descriptor()));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(after.world(),null));
        }
    }
    @Test void fiveSecondReuseSharesSequenceAndPaceWithNpcArrowTeleportMeleeAndMove()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);assertEquals(NONE,cast(f,0,1,npc).reason());
            assertEquals(TOO_FAST,cast(f,0,2,npc).reason());assertEquals(ModernNpcMagic.Reason.TOO_FAST,ModernBattleEngineNpcMagicTest.cast(f,0,3,npc).reason());
            assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.attack(0,1,npc).reason());assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,10,10,0).status());
            f.elapsed(1200);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,4,npc).reason());
            f.elapsed(2400);assertEquals(TOO_FAST,cast(f,0,5,npc).reason());
            f.elapsed(4999);assertEquals(TOO_FAST,cast(f,0,6,npc).reason());f.elapsed(5000);assertEquals(NONE,cast(f,0,7,npc).reason());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,ModernNpcTeleportTest.cast(f,0,8).reason());
            f.elapsed(6200);assertEquals(ModernSpellProtocol.Reason.NONE,ModernNpcTeleportTest.cast(f,0,9).reason());
            var after=f.view();assertEquals(2,state(after).castCount());assertEquals(502,state(after).lawful());
            assertEquals(1000-140-3-10,after.world().vitals().get(0).mp());assertEquals(9,after.world().casts().get(0).requestSequence());
            assertThrows(SecurityException.class,()->cast(f,0,9,npc));
        }
    }
    @Test void scopeLifeOwnerAndReplayChecksDoNotMutateStateOrConsumeEntropy()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var p=f.players.get(0);var req=request(f,0,1,npc);var before=f.view();
            assertThrows(SecurityException.class,()->f.engine.disintegrateNpc(f.players.get(1).owner(),req));
            for(var scope:List.of(f.players.get(1).scope(),new ModernRooms.Scope(7,1,2,0),new ModernRooms.Scope(7,1,1,1)))
                assertThrows(SecurityException.class,()->f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(scope,npc.id(),1,1,1)));
            for(var scope:List.of(new ModernNpcRoster.Scope(f.scope.roomId()+1,f.scope.matchId(),f.scope.generation()),
                    new ModernNpcRoster.Scope(f.scope.roomId(),f.scope.matchId()+1,f.scope.generation()),new ModernNpcRoster.Scope(f.scope.roomId(),f.scope.matchId(),f.scope.generation()+1)))
                assertThrows(SecurityException.class,()->f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(p.scope(),new ModernNpcRoster.Id(scope,1),1,1,1)));
            assertThrows(SecurityException.class,()->f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(p.scope(),npc.id(),1,2,1)));
            assertThrows(SecurityException.class,()->f.engine.disintegrateNpc(p.owner(),new ModernNpcDisintegrateState.Request(p.scope(),npc.id(),1,1,2)));
            assertThrows(SecurityException.class,()->cast(f,0,2,npc));assertEquals(before,f.view());assertEquals(0,f.draws.get());
            assertEquals(NONE,cast(f,0,1,npc).reason());var after=f.view();assertThrows(SecurityException.class,()->cast(f,0,1,npc));assertEquals(after,f.view());
        }
    }
    @Test void unlearnedUnsupportedLowLawfulAndNoManaCannotDamageOrStartCooldown()throws Exception {
        for(int kind=0;kind<4;kind++) {
            int k=kind;var status=kind==0?ModernDisintegrateCatalog.Status.UNLEARNED:kind==1?ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE:ModernDisintegrateCatalog.Status.QUALIFIED;
            try(var f=fixture(false,100,2,0,false,status,ModernNpcMeleeTest.catalog(),s->source(s,k==3?69:1000,k==2?499:500))) {
                f.active();var npc=f.spawn(0,11,10);var before=f.view();assertEquals(List.of(UNLEARNED,UNSUPPORTED,LAWFUL,NO_MP).get(kind),cast(f,0,1,npc).reason());
                noCast(before,f.view());assertEquals(before.npcs(),f.view().npcs());assertEquals(0,f.draws.get());assertTrue(f.view().world().npcSpells().isEmpty());
            }
        }
    }
    @Test void wrongLaneRangeAndBlockedCornerCannotConsumeMana()throws Exception {
        try(var f=fixture()) {
            f.active();var other=f.spawn(1,12,10);assertEquals(WRONG_LANE,cast(f,0,1,other).reason());
            var far=f.spawn(0,22,10);assertEquals(OUT_OF_RANGE,cast(f,0,2,far).reason());
            var corner=f.spawn(0,11,9);f.spawn(0,10,9);f.spawn(0,11,10);var before=f.view();
            assertEquals(BLOCKED,cast(f,0,3,corner).reason());noCast(before,f.view());assertEquals(0,f.draws.get());
        }
    }
    @Test void zeroAndFullWidthDamagePreserveExactHpAndDoNotInventHurt()throws Exception {
        for(long mr:List.of(0L,Long.MAX_VALUE))try(var f=fixture(false,100,2,0,false,ModernDisintegrateCatalog.Status.QUALIFIED,
                ModernNpcMeleeTest.catalog(Map.of(HP,9007199254740993L,MR,mr),true,0),s->source(s,1000,500))) {
            f.active();var npc=f.spawn(0,11,10);assertEquals(NONE,cast(f,0,1,npc).reason());var after=f.view();var s=state(after);
            assertEquals(9007199254740993L-s.hit().outcome().appliedDamage(),after.npcs().actors().get(0).hp());
            assertEquals(930,after.world().vitals().get(0).mp());assertEquals(501,s.lawful());
            if(mr==Long.MAX_VALUE){assertEquals(0,s.hit().outcome().appliedDamage());assertEquals(npc,after.npcs().actors().get(0));}
            assertEquals(TOO_FAST,cast(f,0,2,npc).reason());
        }
    }
    @Test void invalidEntropyAndLawfulOverflowNeverHalfCommit()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();f.invalidRandom=true;
            assertEquals(NUMERIC,cast(f,0,1,npc).reason());noCast(before,f.view());assertEquals(before.npcs(),f.view().npcs());
            f.invalidRandom=false;assertEquals(NONE,cast(f,0,2,npc).reason());
        }
        try(var f=fixture(false,100,2,0,false,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(),s->source(s,1000,Long.MAX_VALUE))) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();assertEquals(NUMERIC,cast(f,0,1,npc).reason());
            noCast(before,f.view());assertEquals(before.npcs(),f.view().npcs());assertEquals(0,f.draws.get());
        }
    }
    @Test void reentrantMovementAttackCasterDamageAndNpcMotionInvalidateOnlyTheOuterCast()throws Exception {
        for(int kind=0;kind<4;kind++)try(var f=fixture()) {
            int k=kind;f.active();var npc=f.spawn(0,kind==3?14:11,10);f.elapsed(100);var before=f.view();
            f.duringRandom=()->{if(k==0)assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
                else if(k==1)assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(0,1,npc).reason());
                else assertEquals(k==3?ModernNpcBattle.Reason.MOVED:ModernNpcBattle.Reason.ATTACKED,f.step(npc).reason());};
            var after=cast(f,0,1,npc);assertEquals(STALE_STATE,after.reason());noCast(before,after.frame());
            if(k==0)assertNotEquals(before.world().actors().get(0).position(),after.frame().world().actors().get(0).position());
            if(k==2)assertTrue(after.frame().world().vitals().get(0).hp()<100);
            if(k==3)assertNotEquals(npc.cell(),after.frame().npcs().actors().get(0).cell());
        }
    }
    @Test void cancellationTimeoutClockRegressionAndExpiredEquipmentCannotCommitLateDamage()throws Exception {
        for(int kind=0;kind<4;kind++) {
            int k=kind;try(var f=fixture(false,100,2,0,false,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(),
                    s->source(k==3?expiring(s):s,1000,500))) {
                f.active();var npc=f.spawn(0,11,10);var before=f.view();
                f.duringRandom=()->{if(k==0)f.rooms.releasePreparation(f.ticket);else if(k==1)f.elapsed(60000);else if(k==2)f.clock.decrementAndGet();else f.epoch.set(2000);};
                var after=cast(f,0,1,npc);assertEquals(NOT_ACTIVE,after.reason());noCast(before,after.frame());
                assertEquals(before.world().casts(),after.frame().world().casts());assertEquals(ModernBattleEngine.Phase.ABORTED,after.frame().world().phase());
            }
        }
    }
    @Test void duplicateReentrantAndConcurrentRequestsCommitManaAndLawfulExactlyOnce()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.duringRandom=()->assertEquals(NONE,cast(f,0,1,npc).reason());
            assertEquals(STALE_STATE,cast(f,0,1,npc).reason());assertEquals(1,state(f.view()).castCount());assertEquals(501,state(f.view()).lawful());
        }
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);var pool=Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Callable<Boolean> action=()->{try{return cast(f,0,1,npc).reason()==NONE;}catch(SecurityException e){return false;}};
                var a=pool.submit(action);var b=pool.submit(action);assertNotEquals(a.get(2,TimeUnit.SECONDS),b.get(2,TimeUnit.SECONDS));
                assertEquals(1,state(f.view()).castCount());assertEquals(930,f.view().world().vitals().get(0).mp());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }
    @Test void clockAdvanceWithoutNpcChangeIsAllowedButMpRegenCannotBeOverwritten()throws Exception {
        try(var f=fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);
            assertEquals(NONE,cast(f,0,1,npc).reason());assertEquals(1,f.view().npcs().elapsedMillis());
            f.elapsed(15999);var before=f.view();f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);
            var after=cast(f,0,2,npc);assertEquals(STALE_REGEN,after.reason());assertEquals(state(before),state(after.frame()));
            assertTrue(after.frame().world().vitals().get(0).mp()>before.world().vitals().get(0).mp());assertEquals(before.npcs().actors(),after.frame().npcs().actors());
        }
    }
    @Test void questFourKillsAdvanceOnlyOwnLaneThenRequireGuardianBeforeVictory()throws Exception {
        try(var f=fixture(true,100,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(Map.of(HP,1L),true,0),s->source(s,1000,500))) {
            f.active();var other=ModernQuestEngineTest.living(f,1);assertEquals(TARGET_DEAD,cast(f,0,1,other).reason());
            for(int stage=1;stage<=4;stage++) {
                f.elapsed((stage-1)*5000L);var npc=ModernQuestEngineTest.living(f,0);assertEquals(stage,npc.stage());
                var result=cast(f,0,stage+1,npc);assertEquals(NONE,result.reason());assertTrue(state(result.frame()).hit().outcome().death());
                assertNull(result.frame().world().result());assertEquals(1,ModernQuestEngineTest.living(f,1).stage());
            }
            assertEquals(4,state(f.view()).castCount());assertEquals(504,state(f.view()).lawful());
            var guardian=ModernQuestObjectivesTest.objective(f,0);assertEquals(ModernNpcBattle.Reason.TOO_FAST,ModernQuestObjectivesTest.strike(f,0,1,guardian.id()).reason());
            f.elapsed(16200);f.randomValue=19;var finished=ModernQuestObjectivesTest.strike(f,0,2,guardian.id()).frame();
            assertEquals(ModernBattleEngine.Phase.FINISHED,finished.world().phase());assertEquals(2,finished.world().result().mode());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.LOSS),finished.world().result().participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertEquals(NOT_ACTIVE,cast(f,0,6,other).reason());
        }
    }
    @Test void questDeathRespawnRetainsLawfulManaHistoryAndRequiresNewCasterLife()throws Exception {
        try(var f=fixture(true,1,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(Map.of(HP,10000L),true,0),s->source(s,1000,500))) {
            f.active();var npc=ModernQuestEngineTest.living(f,0);assertEquals(NONE,cast(f,0,1,npc).reason());var first=state(f.view());
            var old=request(f,0,3,npc);var dead=ModernQuestLifeTest.die(f,100);assertEquals(DEAD,cast(f,0,2,npc).reason());
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            assertThrows(SecurityException.class,()->f.engine.disintegrateNpc(f.players.get(0).owner(),old));assertEquals(first,state(f.view()));
            f.elapsed(5000);assertEquals(NONE,cast(f,0,3,npc).reason());assertEquals(2,state(f.view()).hit().casterLife());
            assertEquals(140,state(f.view()).mpSpent());assertEquals(502,state(f.view()).lawful());
        }
    }
    @Test void npcCorpseRetirementKeepsReceiptButCannotReuseItsOldTarget()throws Exception {
        try(var f=fixture(true,100,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(Map.of(HP,1L),true,0),s->source(s,1000,500))) {
            f.active();var npc=ModernQuestEngineTest.living(f,0);assertEquals(NONE,cast(f,0,1,npc).reason());var receipt=state(f.view());
            f.elapsed(1000);var before=f.view();assertTrue(before.npcs().actors().stream().noneMatch(n->n.id().equals(npc.id())));
            assertEquals(receipt,state(before));assertEquals(2,ModernQuestEngineTest.living(f,0).stage());
            assertEquals(TARGET_DEAD,cast(f,0,2,npc).reason());noCast(before,f.view());
        }
    }
    @Test void botsRemainInertAndDisabledConfigCannotGainSkill77()throws Exception {
        try(var f=fixture(false,100,1,1,false,ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(),s->source(s,1000,500))) {
            var loading=f.view().world();assertEquals(ModernBattleEngine.Phase.LOADING,loading.phase());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(loading,loading.revision()+1,ModernNpcTeleportTest.descriptor()));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(loading,null));
            f.active();assertEquals(1,f.engine.ownerCount());assertEquals(ModernNpcDisintegrateState.State.initial(2,0,0),f.view().world().npcDisintegrations().get(1));
            var npc=f.spawn(0,11,10);assertEquals(NONE,cast(f,0,1,npc).reason());assertEquals(ModernNpcDisintegrateState.State.initial(2,0,0),f.view().world().npcDisintegrations().get(1));
        }
        try(var f=ModernBattleEngineNpcMagicTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();assertEquals(NOT_ACTIVE,cast(f,0,1,npc).reason());assertEquals(before,f.view());
        }
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,s->source(s,1000,500),ModernNpcMeleeTest.catalog(),false)) {
            assertThrows(IllegalArgumentException.class,()->f.build(f.config.withDisintegrate(true)));
            assertThrows(IllegalArgumentException.class,()->f.build(f.config,false,f.prepared.stream().map(p->enable(p,ModernDisintegrateCatalog.Status.QUALIFIED)).toList()));
            assertTrue(f.ticket.isLive());f.engine=f.build(f.config);
        }
    }
}
