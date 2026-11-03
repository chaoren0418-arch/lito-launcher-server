package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernDisintegrateState.Reason.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Real frozen room tickets and the actual engine, with isolated clocks/RNG.
 * No production listener, source SQL writes, native BIN or runtime UI claims.
 */
class ModernBattleEngineDisintegrateTest {
    static final long SECOND=1_000_000_000L;
    static ModernBattleEngine.PreparedMember upgrade(ModernBattleEngine.PreparedMember p,long lawful,long intelligence,
            boolean fed,ModernDisintegrateCatalog.Status status) {
        return upgrade(p,lawful,intelligence,fed,status,false);
    }
    static ModernBattleEngine.PreparedMember upgrade(ModernBattleEngine.PreparedMember p,long lawful,long intelligence,
            boolean fed,ModernDisintegrateCatalog.Status status,boolean expires) {
        var s=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.character(),p.fighter().profile().source().persisted().maxHp(),
                p.fighter().profile().source().persisted().maxMp(),fed?225:0);
        var q=s.input().equipment();var old=q.source().persisted();
        var values=new ModernCharacterData.PersistedValues(old.maxHp(),old.maxMp(),old.savedHp(),old.savedMp(),old.armorClass(),
                old.strength(),old.dexterity(),old.constitution(),intelligence,old.wisdom(),old.charisma(),old.experience(),lawful,
                old.originalStrength(),old.originalDexterity(),old.originalConstitution(),old.originalIntelligence(),old.originalWisdom(),old.food225());
        var snapshot=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),values,q.source().inventory());
        var observation=q.lifetimes();
        if(expires) {
            var facts=new java.util.TreeMap<Long,ModernItemLifetime.Fact>();
            for(var item:snapshot.inventory())facts.put(item.objectId(),new ModernItemLifetime.Fact(item.objectId(),item.template().catalogId(),item.template().kind(),item.equipped(),
                    java.util.OptionalLong.of(0),false,java.util.OptionalLong.empty(),java.util.Optional.of("1970-01-01 00:00:02.000000"),0,false));
            observation=new ModernItemLifetime.Observation(snapshot.sourceId(),snapshot.character().objectId(),"1970-01-01 00:00:00.000000",0,0,facts);
        }
        var qualified=new ModernEquipmentFacts.Qualified(snapshot,q.items(),q.catalog(),observation);
        var source=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,s.input().weaponsQualified(),s.input().weapons()),
                s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
        var fighter=ModernPhysicalCombatTest.fighter(source);
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,
                p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),
                ModernInventoryWeight.qualify(fighter.profile(),100),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),
                new ModernDisintegrateCatalog.Snapshot(fighter.key(),status));
    }
    static ModernBattleEngineHonorTest.Fixture fixture(boolean honor,int bots,long hp,long mp,long lawful,boolean fed)throws Exception {
        return new ModernBattleEngineHonorTest.Fixture(1000,hp,mp,SECOND,bots,true,honor,
                p->upgrade(p,lawful,12,fed,ModernDisintegrateCatalog.Status.QUALIFIED));
    }
    static ModernDisintegrateState.Request request(ModernBattleEngineHonorTest.Fixture f,int who,long seq,long target,long casterLife,long targetLife) {
        return new ModernDisintegrateState.Request(f.players.get(who).scope(),f.ticket.matchId(),f.ticket.generation(),seq,casterLife,target,targetLife);
    }
    static ModernBattleEngine.DisintegrateResult cast(ModernBattleEngineHonorTest.Fixture f,int who,long seq,long target,long casterLife,long targetLife) {
        return f.engine.disintegrate(f.players.get(who).owner(),request(f,who,seq,target,casterLife,targetLife));
    }
    static ModernBattleEngine.DisintegrateResult cast(ModernBattleEngineHonorTest.Fixture f,long seq,long target) {return cast(f,0,seq,target,1,1);}
    static void unchanged(ModernBattleEngine.Snapshot before,ModernBattleEngine.Snapshot after) {
        assertEquals(before.vitals(),after.vitals());assertEquals(before.disintegrations(),after.disintegrations());
        assertEquals(before.regen(),after.regen());assertEquals(before.honor().objectives(),after.honor().objectives());
        assertEquals(before.damages(),after.damages());assertEquals(before.fireStorms(),after.fireStorms());
        assertEquals(before.items(),after.items());assertEquals(before.result(),after.result());
    }
    @Test void fullStateCommitsDamageMpLawfulCriticalAndOneDeathmatchResultTogether()throws Exception {
        for(boolean critical:new boolean[]{false,true})try(var f=fixture(false,0,100,300,500,false)) {
            var before=f.active();f.randomOverride=b->b==100?(critical?0:99):0;
            var reply=cast(f,1,2);assertEquals(NONE,reply.reason());var after=reply.snapshot();var s=after.disintegrations().get(0);var h=s.hit();
            assertEquals(ModernBattleEngine.Phase.FINISHED,after.phase());assertEquals(1,s.castCount());assertEquals(501,s.lawful());
            assertEquals(after.revision(),s.revision());assertEquals(after.revision(),after.vitals().get(1).deathRevision());
            assertEquals(after.revision(),after.result().engineRevision());assertEquals(critical,h.critical());assertEquals(critical?11748:11746,h.effect());
            assertEquals(100,h.appliedDamage());assertEquals(0,h.hpAfter());assertEquals(300-s.mpCost(),after.vitals().get(0).mp());
            assertEquals(500,f.prepared.get(0).fighter().profile().source().persisted().lawful(),"imported source is immutable");
            assertEquals(before.damages(),after.damages());assertEquals(before.fireStorms(),after.fireStorms());
            assertEquals(0,after.casts().get(0).castCount());assertEquals(1,after.casts().get(0).requestSequence());
            assertEquals(List.of(7,42),after.result().participants().stream().map(p->p.key().sourceId()).toList());
            assertEquals(after,f.engine.tick());assertEquals(after,cast(f,2,2).snapshot());
            assertEquals(after.disintegrations(),ModernDisintegrateState.decode(ModernDisintegrateState.encode(after.disintegrations())));
        }
    }
    @Test void fiveSecondReuseAndSharedPacingSequenceCoverArrowFireHealingAndTeleport()throws Exception {
        try(var f=fixture(true,0,10000,1000,500,false)) {
            f.active();assertEquals(NONE,cast(f,1,2).reason());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,4,2).reason());
            assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,6).status());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,1,2).status());
            f.elapsed(2*SECOND);assertEquals(TOO_FAST,cast(f,3,2).reason());assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,4,4,2).reason());
            f.elapsed(4*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,5,70,0).reason());
            f.elapsed(5*SECOND);assertEquals(TOO_FAST,cast(f,6,2).reason(),"shared animation pace still applies");
            f.elapsed(5*SECOND+200_000_000);var second=cast(f,7,2);assertEquals(NONE,second.reason());
            assertEquals(2,second.snapshot().disintegrations().get(0).castCount());assertEquals(502,second.snapshot().disintegrations().get(0).lawful());
            f.elapsed(7*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,8,1,3).reason());
            f.elapsed(9*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,9,5,0).reason());
            var finalState=f.engine.tick();long spent=finalState.disintegrations().get(0).mpSpent()+finalState.damages().get(0).mpSpent()
                    +finalState.fireStorms().get(0).mpSpent()+finalState.honor().heals().get(0).mpSpent()+finalState.casts().get(0).mpSpent();
            assertEquals(1000-spent,finalState.vitals().get(0).mp());
            assertThrows(SecurityException.class,()->cast(f,9,2));
        }
    }
    @Test void honorDeathAndRespawnRetainHistoryAndRejectOldCasterOrTargetLife()throws Exception {
        try(var f=fixture(true,0,1,1000,500,false)) {
            var before=f.active();var killed=cast(f,1,2).snapshot();assertEquals(ModernBattleEngine.Phase.ACTIVE,killed.phase());
            assertEquals(1,killed.honor().lives().get(1).deathCount());assertEquals(before.honor().objectives(),killed.honor().objectives());
            assertEquals(TARGET_DEAD,cast(f,2,2).reason());f.elapsed(5*SECOND);var respawn=f.engine.tick();
            assertEquals(1,respawn.vitals().get(1).hp());assertEquals(killed.disintegrations(),respawn.disintegrations());
            assertEquals(STALE_LIFE,cast(f,3,2).reason());assertEquals(STALE_LIFE,cast(f,1,1,1,1,1).reason());
            var next=cast(f,0,4,2,1,2);assertEquals(NONE,next.reason());assertEquals(2,next.snapshot().disintegrations().get(0).hit().targetLife());
            assertEquals(2,next.snapshot().regen().get(1).damageLoss());assertEquals(502,next.snapshot().disintegrations().get(0).lawful());
        }
    }
    @Test void botUsesTypedIdentityNoSourceOrCasterPermissionAndCanBeKilledAndRespawned()throws Exception {
        try(var f=fixture(true,2,1000,1000,500,false)) {
            var before=f.active();assertFalse(before.actors().get(3).member().identity().isHuman());
            assertEquals(SAME_TEAM,cast(f,1,3).reason());var result=cast(f,2,4);assertEquals(NONE,result.reason());
            assertEquals(0,result.snapshot().vitals().get(3).hp());assertEquals(ModernDisintegrateState.State.initial(4,0,0),result.snapshot().disintegrations().get(3));
            f.elapsed(5*SECOND);assertEquals(40,f.engine.tick().vitals().get(3).hp());
            assertEquals(NONE,cast(f,0,3,4,1,2).reason());assertEquals(80,f.engine.tick().regen().get(3).damageLoss());
        }
    }
    @Test void everyInvalidRandomDrawRejectsWholeCastAndConsumesOnlyTheRequestSequence()throws Exception {
        for(int position=1;position<=10;position++)try(var f=fixture(true,0,1000,300,500,false)) {
            var before=f.active();f.invalidAt=position;var result=cast(f,1,2);assertEquals(NUMERIC,result.reason());unchanged(before,result.snapshot());
            assertEquals(position,f.draws);assertEquals(1,result.snapshot().casts().get(0).requestSequence());
            assertThrows(SecurityException.class,()->cast(f,1,2));
        }
    }
    @Test void lawGateOverflowAndNoMpNeverConsumeEntropyOrPartialFields()throws Exception {
        for(long lawful:new long[]{Long.MIN_VALUE,499,Long.MAX_VALUE,500})try(var f=fixture(true,0,1000,lawful==500?58:300,lawful,false)) {
            var before=f.active();var result=cast(f,1,2);assertEquals(lawful<500?LAWFUL:lawful==Long.MAX_VALUE?NUMERIC:NO_MP,result.reason());
            unchanged(before,result.snapshot());assertEquals(0,f.draws);assertEquals(1,result.snapshot().casts().get(0).requestSequence());
        }
    }
    @Test void sourceQualificationIsNotInferredFromAnotherSkillOrIdenticalObjectId()throws Exception {
        for(var status:List.of(ModernDisintegrateCatalog.Status.UNLEARNED,ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE))
            try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,300,SECOND,0,true,true,p->upgrade(p,500,12,false,status))) {
                var before=f.active();var result=cast(f,1,2);assertEquals(status==ModernDisintegrateCatalog.Status.UNLEARNED?UNLEARNED:UNSUPPORTED,result.reason());
                unchanged(before,result.snapshot());assertEquals(0,f.draws);
                var p=f.prepared.get(0);
                assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),
                        p.fighter(),p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),
                        new ModernDisintegrateCatalog.Snapshot(new TournamentPresence.CharacterKey(42,3333),ModernDisintegrateCatalog.Status.QUALIFIED)));
            }
        try(var f=new ModernBattleEngineHonorTest.Fixture()){var before=f.active();var denied=cast(f,1,2);assertEquals(UNSUPPORTED,denied.reason());unchanged(before,denied.snapshot());}
    }
    @Test void wrongOwnerScopeGenerationSelfObjectiveAndOutOfOrderRequestsCannotMutateState()throws Exception {
        try(var f=fixture(true,0,1000,300,500,false)) {
            var before=f.active();var owner=f.players.get(0).owner();var r=request(f,0,1,2,1,1);
            assertThrows(SecurityException.class,()->f.engine.disintegrate(f.players.get(1).owner(),r));
            assertThrows(SecurityException.class,()->f.engine.disintegrate(owner,new ModernDisintegrateState.Request(r.scope(),r.matchId(),r.generation()+1,1,1,2,1)));
            assertThrows(SecurityException.class,()->cast(f,2,2));assertThrows(SecurityException.class,()->cast(f,1,1));assertThrows(SecurityException.class,()->cast(f,1,4));
            assertEquals(before,f.engine.tick());assertEquals(0,f.draws);
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromCombat(before,null));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(before,before.revision()+1,null));
            assertThrows(IllegalArgumentException.class,()->new ModernSpellProtocol.Request(r.scope(),r.matchId(),r.generation(),1,77,2),"v1-26 cannot carry new target contract");
        }
    }
    @Test void slowInvalidAndCancelledRngCannotCommitAfterAbort()throws Exception {
        for(int mode=0;mode<4;mode++)try(var f=fixture(true,0,1000,300,500,false)) {
            var before=f.active();if(mode<2){f.jump=1800*SECOND;f.invalid=mode==1;}else if(mode==2)f.cancel=true;else f.randomOverride=b->{f.epoch.set(999);return b-1;};
            var result=cast(f,1,2);assertEquals(NOT_ACTIVE,result.reason());assertEquals(ModernBattleEngine.Phase.ABORTED,result.snapshot().phase());
            unchanged(before,result.snapshot());assertEquals(0,result.snapshot().casts().get(0).requestSequence());
        }
    }
    @Test void expiryAndIndependentHpRecoveryRejectStaleOutcomeButKeepTheirOwnChanges()throws Exception {
        try(var f=fixture(true,0,1000,300,500,false)) {
            f.active();var p=f.players.get(0);assertEquals(ModernItemProtocol.Reason.NONE,f.engine.item(p.owner(),
                    new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40013)).reason());
            f.elapsed(300*SECOND-1);var before=f.engine.tick();f.jump=1;var r=cast(f,1,2);assertEquals(STALE_EFFECT,r.reason());
            unchanged(before,r.snapshot());assertFalse(r.snapshot().buffs().get(0).hasEffects());assertEquals(1,r.snapshot().casts().get(0).requestSequence());
        }
        try(var f=fixture(true,0,1000,300,500,true)) {
            f.active();f.cast(0,1,4,2);f.elapsed(2*SECOND-1);var before=f.engine.tick();f.jump=1;
            var r=cast(f,2,2);assertEquals(STALE_REGEN,r.reason());assertEquals(before.disintegrations(),r.snapshot().disintegrations());
            assertEquals(before.regen().get(1).damageLoss(),r.snapshot().regen().get(1).damageLoss());
            assertTrue(r.snapshot().vitals().get(1).hp()>before.vitals().get(1).hp());assertEquals(before.vitals().get(0).mp(),r.snapshot().vitals().get(0).mp());
        }
    }
    @Test void signedNanoWrapMaintainsExactFiveSecondReuse()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,10000,1000,Long.MAX_VALUE-SECOND,0,true,true,p->upgrade(p,500,12,false,ModernDisintegrateCatalog.Status.QUALIFIED))) {
            f.active();assertEquals(NONE,cast(f,1,2).reason());f.elapsed(5*SECOND-1);assertEquals(TOO_FAST,cast(f,2,2).reason());
            f.elapsed(5*SECOND);assertTrue(f.clock.get()<0);assertEquals(NONE,cast(f,3,2).reason());assertEquals(502,f.engine.tick().disintegrations().get(0).lawful());
        }
    }
    @Test void fullI64HpMpAndLawfulDoNotNarrowAtCommit()throws Exception {
        long wide=1L<<34;
        try(var f=fixture(true,0,wide,wide,wide,false)) {
            var before=f.active();var r=cast(f,1,2);assertEquals(NONE,r.reason());var s=r.snapshot().disintegrations().get(0);
            assertEquals(wide,s.hit().hpBefore());assertEquals(wide-s.hit().appliedDamage(),r.snapshot().vitals().get(1).hp());
            assertEquals(wide-s.mpCost(),r.snapshot().vitals().get(0).mp());assertEquals(wide+1,s.lawful());
            assertEquals(before.honor().objectives(),r.snapshot().honor().objectives());
        }
    }
    @Test void lethalHitClearsVictimAndTerminalBuffsAndSubsequentDeadCasterCannotSpend()throws Exception {
        for(boolean honor:new boolean[]{false,true})try(var f=fixture(honor,0,1,300,500,false)) {
            f.active();for(var p:f.players)assertEquals(ModernItemProtocol.Reason.NONE,f.engine.item(p.owner(),
                    new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40013)).reason());
            var after=cast(f,1,2).snapshot();assertFalse(after.buffs().get(1).hasEffects());assertEquals(honor,after.buffs().get(0).hasEffects());
            var denied=cast(f,1,1,1,1,1);assertEquals(honor?DEAD:NOT_ACTIVE,denied.reason());unchanged(after,denied.snapshot());
        }
    }
    @Test void rangeAndWallChecksPrecedeEntropyAndNoPacketCanFireDuringLoading()throws Exception {
        try(var f=fixture(true,0,10000,1000,500,false)) {
            var before=f.engine.tick();var denied=cast(f,1,2);assertEquals(NOT_ACTIVE,denied.reason());unchanged(before,denied.snapshot());
            f.active();for(int n=1;n<=10;n++){f.elapsed(n*SECOND);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,n,6).status());}
            f.elapsed(11*SECOND);before=f.engine.tick();assertEquals(OUT_OF_RANGE,cast(f,2,2).reason());assertEquals(0,f.draws);unchanged(before,f.engine.tick());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,11,2).status());f.elapsed(12*SECOND);assertEquals(NONE,cast(f,3,2).reason(),"exact range10 is valid");
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,300,SECOND,0,true,true,
                p->upgrade(p,500,12,false,ModernDisintegrateCatalog.Status.QUALIFIED),
                bytes->{ModernWorldMapTest.setAttributes(bytes,10,10,(short)0,(short)1);return bytes;})) {
            var before=f.active();var r=cast(f,1,2);assertEquals(BLOCKED,r.reason());unchanged(before,r.snapshot());assertEquals(0,f.draws);
        }
    }
    @Test void expiredEquipmentDuringRollRejectsEvenOtherwiseValidCriticalWithoutSpending()throws Exception {
        for(boolean expireBefore:new boolean[]{false,true})try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,300,SECOND,0,true,true,
                p->upgrade(p,500,12,false,ModernDisintegrateCatalog.Status.QUALIFIED,true))) {
            var before=f.active();assertEquals(2000,f.prepared.get(0).fighter().profile().validUntilMillis());
            if(expireBefore)f.epoch.set(2000);else f.randomOverride=b->{f.epoch.set(2000);return 0;};
            var r=cast(f,1,2);assertEquals(NOT_ACTIVE,r.reason());unchanged(before,r.snapshot());
            assertEquals(ModernBattleEngine.AbortReason.SERVER_ABORT,r.snapshot().abortReason());assertEquals(expireBefore?0:10,f.draws);
        }
    }
    @Test void finalDamageOverflowAndLastRepresentableLawfulAreCheckedBeforeCommit()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,300,SECOND,0,true,true,
                // The shared source qualifier already rejects a coefficient
                // beyond i64. This still-qualified INT makes skill77's final
                // damage overflow, exercising the COMMIT guard instead.
                p->upgrade(p,500,Long.MAX_VALUE/4,false,ModernDisintegrateCatalog.Status.QUALIFIED))) {
            var before=f.active();var r=cast(f,1,2);assertEquals(NUMERIC,r.reason());unchanged(before,r.snapshot());assertEquals(10,f.draws);
        }
        try(var f=fixture(true,0,10000,1000,Long.MAX_VALUE-1,false)) {
            f.active();var first=cast(f,1,2);assertEquals(NONE,first.reason());assertEquals(Long.MAX_VALUE,first.snapshot().disintegrations().get(0).lawful());
            f.elapsed(5*SECOND);var before=f.engine.tick();int draws=f.draws;var last=cast(f,2,2);assertEquals(NUMERIC,last.reason());
            unchanged(before,last.snapshot());assertEquals(draws,f.draws);
        }
    }
    @Test void concurrentDuplicateHasExactlyOneSuccessfulCommitAndOneConsumedMpCost()throws Exception {
        try(var f=fixture(true,0,10000,300,500,false)) {
            f.active();var workers=java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var tasks=new ArrayList<java.util.concurrent.Future<Boolean>>();
                for(int i=0;i<2;i++)tasks.add(workers.submit(()->{try{return cast(f,1,2).reason()==NONE;}catch(SecurityException expected){return false;}}));
                int accepted=0;for(var task:tasks)if(task.get(5,java.util.concurrent.TimeUnit.SECONDS))accepted++;
                assertEquals(1,accepted);var s=f.engine.tick();assertEquals(1,s.disintegrations().get(0).castCount());assertEquals(501,s.disintegrations().get(0).lawful());
                assertEquals(300-s.disintegrations().get(0).mpCost(),s.vitals().get(0).mp());assertEquals(10,f.draws);
            }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));}
        }
    }
    @Test void reentrantTargetMovementKeepsItsOwnStateButRejectsTheOldCoordinates()throws Exception {
        try(var f=fixture(true,0,1000,300,500,false)) {
            var before=f.active();f.randomOverride=b->{f.randomOverride=null;assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,1,2).status());return b-1;};
            var r=cast(f,1,2);assertEquals(STALE_STATE,r.reason());unchanged(before,r.snapshot());
            assertNotEquals(before.actors().get(1).position(),r.snapshot().actors().get(1).position());assertEquals(1,r.snapshot().casts().get(0).requestSequence());
        }
    }
    @Test void independentMpTickDuringRngCannotBeOverwrittenAndFutureCastUsesTheRecoveredBalance()throws Exception {
        try(var f=fixture(true,0,10000,300,500,true)) {
            f.active();assertEquals(NONE,cast(f,1,2).reason());f.elapsed(16*SECOND-1);var before=f.engine.tick();f.jump=1;
            var denied=cast(f,2,2);assertEquals(STALE_REGEN,denied.reason());var after=denied.snapshot();
            assertTrue(after.vitals().get(0).mp()>before.vitals().get(0).mp());
            assertEquals(before.disintegrations(),after.disintegrations());assertEquals(before.regen().get(1).damageLoss(),after.regen().get(1).damageLoss());
            var accepted=cast(f,3,2);assertEquals(NONE,accepted.reason());assertEquals(after.vitals().get(0).mp()-after.disintegrations().get(0).mpCost(),accepted.snapshot().vitals().get(0).mp());
        }
    }
    @Test void casterDeathAndRevivalPreserveItsOwnOldReceiptWithoutGrantingOldLifePermission()throws Exception {
        try(var f=fixture(true,0,10000,1000,500,false)) {
            f.active();var first=cast(f,1,2).snapshot();assertEquals(NONE,cast(f,1,1,1,1,1).reason());
            f.elapsed(5*SECOND);var revived=f.engine.tick();assertEquals(1,revived.honor().lives().get(0).respawnCount());
            assertEquals(first.disintegrations().get(0),revived.disintegrations().get(0));assertEquals(STALE_LIFE,cast(f,2,2).reason());
            var second=cast(f,0,3,2,2,1);assertEquals(NONE,second.reason());assertEquals(2,second.snapshot().disintegrations().get(0).hit().casterLife());
            assertEquals(502,second.snapshot().disintegrations().get(0).lawful());
        }
    }
    static ModernBattleEngine.Snapshot replaceStates(ModernBattleEngine.Snapshot s,List<ModernDisintegrateState.State> states) {
        return new ModernBattleEngine.Snapshot(s.matchId(),s.generation(),s.revision(),s.phase(),s.abortReason(),s.room(),s.manifest(),s.actors(),s.vitals(),s.result(),
                s.casts(),s.damages(),s.items(),s.weights(),s.buffs(),s.ammo(),s.regen(),s.honor(),s.fireStorms(),s.race(),s.isolatedForces(),s.quest(),s.npcSpells(),s.timeAttack(),s.siege(),states);
    }
    @Test void snapshotRejectsOmittedSpendAndForgedFutureLifeOrCurrentDamageLoss()throws Exception {
        try(var f=fixture(true,0,10000,300,500,false)) {
            f.active();var snapshot=cast(f,1,2).snapshot();var state=snapshot.disintegrations().get(0);var h=state.hit();
            assertEquals(snapshot,replaceStates(snapshot,snapshot.disintegrations()));
            assertThrows(IllegalArgumentException.class,()->replaceStates(snapshot,List.of()));
            assertThrows(IllegalArgumentException.class,()->replaceStates(snapshot,List.of(state)));
            var hits=List.of(
                    new ModernDisintegrateState.Hit(h.targetEntityId(),2,h.targetLife(),h.fromX(),h.fromY(),h.toX(),h.toY(),h.hpBefore(),h.hpAfter(),h.rawDamage(),h.appliedDamage(),h.damageLossAfter(),h.critical()),
                    new ModernDisintegrateState.Hit(h.targetEntityId(),h.casterLife(),2,h.fromX(),h.fromY(),h.toX(),h.toY(),h.hpBefore(),h.hpAfter(),h.rawDamage(),h.appliedDamage(),h.damageLossAfter(),h.critical()),
                    new ModernDisintegrateState.Hit(h.targetEntityId(),h.casterLife(),h.targetLife(),h.fromX(),h.fromY(),h.toX(),h.toY(),h.hpBefore(),h.hpAfter(),h.rawDamage(),h.appliedDamage(),h.damageLossAfter()+1,h.critical()),
                    new ModernDisintegrateState.Hit(h.targetEntityId(),h.casterLife(),h.targetLife(),h.fromX()-1,h.fromY(),h.toX(),h.toY(),h.hpBefore(),h.hpAfter(),h.rawDamage(),h.appliedDamage(),h.damageLossAfter(),h.critical()));
            for(var hit:hits) {
                var states=new ArrayList<>(snapshot.disintegrations());states.set(0,new ModernDisintegrateState.State(state.entityId(),state.castCount(),state.revision(),
                        state.mpSpent(),state.lastSequence(),state.initialLawful(),state.lawful(),state.mpCost(),state.reuseMillis(),hit));
                assertThrows(IllegalArgumentException.class,()->replaceStates(snapshot,states));
            }
        }
    }
}
