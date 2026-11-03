package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Real captured room capabilities + qualified fighters; no socket/DB/native adapter bypass. */
class ModernBattleEngineCombatTest {
    private static final ModernWorldMap.SectorKey SECTOR=new ModernWorldMap.SectorKey(0x8000,0x8000);
    private static final long STEP=TimeUnit.MILLISECONDS.toNanos(200),ATTACK=TimeUnit.MILLISECONDS.toNanos(700);
    private static final ModernBattleEngine.Limits IMMEDIATE=new ModernBattleEngine.Limits(10_000,0,60_000);
    private static ModernWorldMap.Cell cell(int x,int y){return new ModernWorldMap.Cell(SECTOR.origin().x()+x,SECTOR.origin().y()+y);}
    private static ModernWorldMap map(boolean blocked)throws Exception {
        byte[] bytes=ModernWorldMapTest.openSectorBytes();
        if(blocked)ModernWorldMapTest.setAttributes(bytes,2,2,(short)0,(short)1); // Standing, north route remains connected; east strike closed.
        return ModernWorldMap.fromSectorBytes(514,Map.of(SECTOR,bytes),()->false);
    }

    @Test void qualifiedInitialVitalsPreserveI64HpAndDistinctSourceForSameObjectId() throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,0x2_0000_0000L,false,IMMEDIATE,bound->0)) {
            var snapshot=f.view();assertEquals(ModernBattleEngine.Phase.LOADING,snapshot.phase());
            assertEquals(List.of(1L,2L),snapshot.vitals().stream().map(ModernCombatProtocol.Vital::entityId).toList());
            assertEquals(List.of(7,42),snapshot.actors().stream().map(a->a.member().key().sourceId()).toList());
            assertEquals(snapshot.actors().get(0).member().key().objectId(),snapshot.actors().get(1).member().key().objectId());
            assertTrue(snapshot.vitals().stream().allMatch(v->v.hp()==0x2_0000_0000L&&v.maxHp()==0x2_0000_0000L&&v.deathRevision()==0));
            assertThrows(UnsupportedOperationException.class,()->snapshot.vitals().clear());
        }
    }

    @Test void onlyLoadedBarrierThenServerCountdownPermitsAttack() throws Exception {
        var limits=new ModernBattleEngine.Limits(10_000,TimeUnit.SECONDS.toNanos(3),60_000);
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,100,false,limits,bound->0)) {
            var loading=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,loading.status());assertNoAttack(loading.snapshot(),1);
            f.ack(0);f.ack(1);var countdown=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,countdown.status());assertNoAttack(countdown.snapshot(),2);
            f.clock.addAndGet(limits.countdownNanos());assertEquals(ModernBattleEngine.Phase.ACTIVE,f.engine.tick().phase());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,3,2).status());
        }
    }

    @Test void legalAttackCommitsDamageTargetAndEventsAtomicallyWithoutBreakingWorldMoveHeading() throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();var reply=f.attack(0,1,2);var after=reply.snapshot();
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,reply.status());assertEquals(before.revision()+1,after.revision());
            var attacker=after.vitals().get(0);var target=after.vitals().get(1);
            assertEquals(1,attacker.attackSequence());assertEquals(after.revision(),attacker.attackRevision());assertEquals(2,attacker.lastTargetEntityId());
            assertEquals(after.revision(),target.hurtRevision());assertTrue(target.hp()<before.vitals().get(1).hp());assertEquals(0,target.deathRevision());
            assertEquals(before.actors().get(0),after.actors().get(0)); // Combat art faces lastTarget; WORLD movement sequence/heading stays stable.
            assertEquals(100,before.vitals().get(1).hp());assertEquals(100,attacker.hp());
        }
    }

    @Test void missIsAnAuthoritativeAttackButNeverAHurtEventOrHpChange() throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,100,false,IMMEDIATE,new Rolls(0,1499,99),-1000)) {
            f.active();var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,result.status());
            var a=result.snapshot().vitals().get(0);var b=result.snapshot().vitals().get(1);
            assertEquals(result.snapshot().revision(),a.attackRevision());assertEquals(100,b.hp());assertEquals(0,b.hurtRevision());assertEquals(0,b.deathRevision());
        }
    }

    @Test void sameTeamDenialConsumesOnlyAttackSequenceAndCannotDamageOrAnimate() throws Exception {
        try(var f=new Fixture(3,map(false),List.of(cell(2,2),cell(3,2),cell(2,3)),0,100,false,IMMEDIATE,bound->0)) {
            f.active();assertEquals(f.view().actors().get(0).member().team(),f.view().actors().get(2).member().team());
            var result=f.attack(0,1,3);assertEquals(ModernBattleEngine.AttackStatus.SAME_TEAM,result.status());assertNoAttack(result.snapshot(),1);
            assertTrue(result.snapshot().vitals().stream().allMatch(v->v.hp()==100));
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status()); // A denial did not start cooldown.
        }
    }

    @Test void outOfRangeAndActualBlockedEdgeAreDifferentDenialsWithNoDamage() throws Exception {
        try(var far=new Fixture(2,map(false),List.of(cell(2,2),cell(5,2)),0,100,false,IMMEDIATE,bound->0)) {
            far.active();var result=far.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.OUT_OF_RANGE,result.status());assertNoAttack(result.snapshot(),1);
        }
        try(var wall=new Fixture(2,map(true),List.of(cell(2,2),cell(3,2)),0,100,false,IMMEDIATE,bound->0)) {
            wall.active();assertFalse(wall.map.canStep(cell(2,2),cell(3,2)));
            var result=wall.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.BLOCKED,result.status());assertNoAttack(result.snapshot(),1);
        }
    }

    @Test void serverCooldownDenialDoesNotRestartAttackEventOrCooldown() throws Exception {
        try(var f=new Fixture()) {
            f.active();var first=f.attack(0,1,2);long hp=first.snapshot().vitals().get(1).hp(),event=first.snapshot().vitals().get(0).attackRevision();
            var denied=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,denied.status());
            assertEquals(hp,denied.snapshot().vitals().get(1).hp());assertEquals(event,denied.snapshot().vitals().get(0).attackRevision());
            f.clock.addAndGet(ATTACK-1);assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,3,2).status());
            f.clock.incrementAndGet();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,4,2).status());
        }
    }

    @Test void movementAndAttackIntervalsBothPreventCrossActionSpeedBypass() throws Exception {
        try(var f=new Fixture()) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,cell(2,2),0).status());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,1,2).status());
            f.clock.addAndGet(STEP);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status());
            assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,2,cell(2,1),6).status());
            f.clock.addAndGet(ATTACK);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,3,cell(2,1),6).status());
        }
    }

    @Test void sequenceGapsReplaysSelfAndUnknownTargetCannotPartiallyMutate() throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();
            assertThrows(SecurityException.class,()->f.attack(0,0,2));assertThrows(SecurityException.class,()->f.attack(0,2,2));
            assertThrows(SecurityException.class,()->f.attack(0,1,1));assertThrows(SecurityException.class,()->f.attack(0,1,3));assertEquals(before,f.view());
            f.attack(0,1,2);var after=f.view();assertThrows(SecurityException.class,()->f.attack(0,1,2));assertEquals(after,f.view());
        }
    }

    @Test void fullOwnerSourceObjectEpochChannelMatchAndGenerationAreValidatedBeforeAttack() throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();var p=f.players.get(0);var s=p.scope();
            for(var wrong:List.of(new ModernRooms.Scope(42,s.objectId(),s.epoch(),s.channelId()),new ModernRooms.Scope(s.sourceId(),s.objectId()+1,s.epoch(),s.channelId()),
                    new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch()+1,s.channelId()),new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch(),1)))
                assertThrows(SecurityException.class,()->f.engine.attack(p.owner(),wrong,f.ticket.matchId(),f.ticket.generation(),1,2));
            assertThrows(SecurityException.class,()->f.engine.attack(f.players.get(1).owner(),s,f.ticket.matchId(),f.ticket.generation(),1,2));
            assertThrows(SecurityException.class,()->f.engine.attack(p.owner(),s,f.ticket.matchId()+1,f.ticket.generation(),1,2));
            assertThrows(SecurityException.class,()->f.engine.attack(p.owner(),s,f.ticket.matchId(),f.ticket.generation()+1,1,2));assertEquals(before,f.view());
        }
    }

    @Test void deathCannotMoveAttackBeHitAgainOrMagicallyFinishTheMatch() throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,1,false,IMMEDIATE,bound->0)) {
            f.active();var kill=f.attack(0,1,2).snapshot();var victim=kill.vitals().get(1);
            assertEquals(0,victim.hp());assertEquals(kill.revision(),victim.deathRevision());assertEquals(kill.revision(),victim.hurtRevision());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,kill.phase()); // No fabricated win/result.
            var moved=f.move(1,1,cell(3,2),2);assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE,moved.status());assertEquals(cell(3,2),moved.snapshot().actors().get(1).position());
            assertEquals(ModernBattleEngine.AttackStatus.DEAD,f.attack(1,1,1).status());
            var again=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.TARGET_DEAD,again.status());
            assertEquals(victim.deathRevision(),again.snapshot().vitals().get(1).deathRevision());assertEquals(0,again.snapshot().vitals().get(1).hp());
        }
    }

    @Test void unsupportedQualifiedWeaponCannotAttackOrReceiveSimplifiedDamage() throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),4,100,false,IMMEDIATE,bound->0)) {
            f.active();var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.UNSUPPORTED,result.status());assertNoAttack(result.snapshot(),1);
            assertEquals(4,result.snapshot().vitals().get(0).weaponType());
        }
    }

    @Test void oldMovementOnlyConstructorDoesNotInventCombatVitals() throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,100,true,IMMEDIATE,bound->0)) {
            var before=f.active();assertTrue(before.vitals().isEmpty());
            var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());assertEquals(before,result.snapshot());
        }
    }

    @Test void checkedDamageOverflowAndBadRngConsumeNoHpEventOrCooldown() throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),1,100,false,IMMEDIATE,bound->bound-1,10,true)) {
            f.active();var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,result.status());assertNoAttack(result.snapshot(),1);
            assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,f.attack(0,2,2).status()); // No attack cooldown on arithmetic rejection.
        }
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,100,false,IMMEDIATE,bound->bound)) {
            f.active();var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,result.status());assertNoAttack(result.snapshot(),1);
        }
    }

    @Test void cancelledTicketAndDisconnectFenceAllSubsequentDamage() throws Exception {
        try(var f=new Fixture()) {
            f.active();f.rooms.rooms.releasePreparation(f.ticket);var result=f.attack(0,1,2);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());assertEquals(ModernBattleEngine.Phase.ABORTED,result.snapshot().phase());
            assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,result.snapshot().abortReason());assertTrue(result.snapshot().vitals().stream().allMatch(v->v.hp()==100&&v.attackRevision()==0));
        }
        try(var f=new Fixture()) {
            f.active();f.engine.disconnect(f.players.get(1).owner());var result=f.attack(0,1,2);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());assertEquals(ModernBattleEngine.AbortReason.DISCONNECTED,result.snapshot().abortReason());
            assertEquals(100,result.snapshot().vitals().get(1).hp());
        }
    }

    @Test void fighterRowAndSourceMustMatchCapturedPreparedMemberBeforeEngineCanClaim() throws Exception {
        try(var f=new Fixture()) {
            var p=f.prepared.get(0);var c=p.character();
            var wrongSource=ModernPhysicalCombatTest.createFighter(42,c,100,10,0);
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),c,p.spawn(),STEP,wrongSource,ATTACK));
            var changed=new ModernApplicationProtocol.CharacterRow(c.objectId(),c.name(),c.characterClass(),c.sex(),c.level()+1);
            var wrongRow=ModernPhysicalCombatTest.createFighter(7,changed,100,10,0);
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),c,p.spawn(),STEP,wrongRow,ATTACK));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),c,p.spawn(),STEP,p.fighter(),0));
        }
    }

    @Test void frozenEquipmentExpiresAtExactCutoffForBothSnapshotAndServerTick() throws Exception {
        for(boolean useTick:List.of(false,true)) {
            var epoch=new AtomicLong(1_000);
            try(var f=lifetimeFixture(IMMEDIATE,epoch,bound->0)) {
                var active=f.active();
                assertTrue(f.prepared.stream().allMatch(p->p.fighter().profile().evaluatedAtMillis()==1_000
                        &&p.fighter().profile().validUntilMillis()==2_000));
                epoch.set(1_999);assertEquals(ModernBattleEngine.Phase.ACTIVE,(useTick?f.engine.tick():f.view()).phase());
                epoch.set(2_000);var expired=useTick?f.engine.tick():f.view();
                assertLifetimeAbort(active,expired,ModernBattleEngine.AbortReason.SERVER_ABORT);
                // Restoring wall time does not resurrect an aborted frozen profile.
                epoch.set(1_999);assertEquals(expired,f.engine.tick());
            }
        }
    }

    @Test void expiredLoadingCannotAcceptEvenOneLoadedAcknowledgement() throws Exception {
        var epoch=new AtomicLong(1_000);
        try(var f=lifetimeFixture(IMMEDIATE,epoch,bound->0)) {
            var before=f.view();assertEquals(ModernBattleEngine.Phase.LOADING,before.phase());
            epoch.set(2_000);f.ack(0);var expired=f.view();
            assertLifetimeAbort(before,expired,ModernBattleEngine.AbortReason.SERVER_ABORT);
            assertTrue(expired.actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));
            f.ack(1);assertEquals(expired,f.view());
        }
    }

    @Test void countdownExpiryWinsOverTheSameTickThatWouldActivateTheWorld() throws Exception {
        var epoch=new AtomicLong(1_000);
        var limits=new ModernBattleEngine.Limits(10_000,TimeUnit.SECONDS.toNanos(3),60_000);
        try(var f=lifetimeFixture(limits,epoch,bound->0)) {
            f.ack(0);f.ack(1);var before=f.view();assertEquals(ModernBattleEngine.Phase.COUNTDOWN,before.phase());
            epoch.set(1_999);assertEquals(ModernBattleEngine.Phase.COUNTDOWN,f.engine.tick().phase());
            f.clock.addAndGet(limits.countdownNanos());epoch.set(2_000);
            assertLifetimeAbort(before,f.engine.tick(),ModernBattleEngine.AbortReason.SERVER_ABORT);
        }
    }

    @Test void movementAndAttackAtEquipmentDeadlineConsumeNoSequenceOrGameplayMutation() throws Exception {
        for(boolean attack:List.of(false,true)) {
            var epoch=new AtomicLong(1_000);
            try(var f=lifetimeFixture(IMMEDIATE,epoch,bound->0)) {
                var before=f.active();epoch.set(2_000);ModernBattleEngine.Snapshot expired;
                if(attack) {
                    var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());expired=result.snapshot();
                } else {
                    var result=f.move(0,1,cell(2,2),0);assertEquals(ModernBattleEngine.MoveStatus.ABORTED,result.status());expired=result.snapshot();
                }
                assertLifetimeAbort(before,expired,ModernBattleEngine.AbortReason.SERVER_ABORT);
                assertNoAttack(expired,0);assertEquals(0,expired.actors().get(0).lastMoveSequence());
            }
        }
    }

    @Test void slowInvalidRandomCannotBypassActiveTimeoutOrMonotonicRegression() throws Exception {
        for(boolean regress:List.of(false,true))for(int randomReply:List.of(0,1,2)) {
            var clock=new java.util.concurrent.atomic.AtomicReference<AtomicLong>();var draws=new AtomicLong();
            ModernPhysicalCombat.BoundedRandom random=bound->{
                if(draws.incrementAndGet()==1)clock.get().addAndGet(regress?-1:TimeUnit.MILLISECONDS.toNanos(IMMEDIATE.activeTimeoutMillis()));
                return randomReply==0?0:randomReply==1?-1:bound;
            };
            try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),0,100,false,IMMEDIATE,random)) {
                clock.set(f.clock);var before=f.active();var result=f.attack(0,1,2);
                assertTrue(draws.get()>0,"the physical resolver must actually have run");
                if(randomReply!=0)assertEquals(1,draws.get());
                assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());
                assertLifetimeAbort(before,result.snapshot(),regress?ModernBattleEngine.AbortReason.CLOCK_REGRESSED:ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT);
                assertNoAttack(result.snapshot(),0); // NUMERIC must not consume sequence after deadline.
                assertEquals(result.snapshot(),f.attack(0,1,2).snapshot());
            }
        }
    }

    @Test void deadlineCrossedInsideResolverCannotCommitHpAttackSequenceOrVisualEvents() throws Exception {
        var epoch=new AtomicLong(1_000);var draws=new AtomicLong();
        // Deterministically models a delayed RNG/resolver returning after the
        // deadline, without a sleeping thread or a nondeterministic scheduler.
        ModernPhysicalCombat.BoundedRandom delayed=bound->{draws.incrementAndGet();epoch.set(2_000);return 0;};
        try(var f=lifetimeFixture(IMMEDIATE,epoch,delayed)) {
            var before=f.active();var result=f.attack(0,1,2);
            assertTrue(draws.get()>0,"the physical resolver must actually have run");
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());
            assertLifetimeAbort(before,result.snapshot(),ModernBattleEngine.AbortReason.SERVER_ABORT);
            assertNoAttack(result.snapshot(),0);
        }
    }

    @Test void epochRegressionBeforeOrDuringResolverCannotExtendTheFrozenProfile() throws Exception {
        for(boolean duringResolver:List.of(false,true)) {
            var epoch=new AtomicLong(1_000);var draws=new AtomicLong();
            ModernPhysicalCombat.BoundedRandom random=bound->{draws.incrementAndGet();if(duringResolver)epoch.set(999);return 0;};
            try(var f=lifetimeFixture(IMMEDIATE,epoch,random)) {
                var before=f.active();f.clock.incrementAndGet(); // Monotonic clock did not regress.
                if(!duringResolver)epoch.set(999);
                var result=f.attack(0,1,2);
                assertEquals(duringResolver,draws.get()>0);
                assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());
                assertLifetimeAbort(before,result.snapshot(),ModernBattleEngine.AbortReason.CLOCK_REGRESSED);
                assertNoAttack(result.snapshot(),0);
            }
        }
        var epoch=new AtomicLong(999);
        try(var f=lifetimeFixture(IMMEDIATE,epoch,bound->0)) {
            // The very first observation must also reject time before profile qualification.
            assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,f.view().abortReason());
            assertNoAttack(f.view(),0);
        }
    }

    @Test void unarmedAndExplicitlyPermanentProfilesHaveNoInventedLifetimeDeadline() throws Exception {
        for(long type:List.of(0L,1L)) {
            var epoch=new AtomicLong(1_000);
            try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),type,100,false,IMMEDIATE,
                    bound->0,10,false,epoch::get,UnaryOperator.identity(),1_000)) {
                assertTrue(f.prepared.stream().allMatch(p->p.fighter().profile().validUntilMillis()==-1));
                f.active();epoch.set(1_000_000);
                assertEquals(ModernBattleEngine.Phase.ACTIVE,f.engine.tick().phase());
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
                assertEquals(ModernBattleEngine.AbortReason.NONE,f.view().abortReason());
            }
        }
    }

    @Test void ExpiryOfTheOtherSourceActorAlsoFencesTheWholeMatch() throws Exception {
        var epoch=new AtomicLong(1_000);
        UnaryOperator<ModernPhysicalCombat.Source> targetOnly=source->
                source.input().equipment().source().sourceId()==42?absoluteLifetime(source):source;
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),1,100,false,IMMEDIATE,
                bound->0,10,false,epoch::get,targetOnly,1_000)) {
            assertEquals(-1,f.prepared.get(0).fighter().profile().validUntilMillis());
            assertEquals(2_000,f.prepared.get(1).fighter().profile().validUntilMillis());
            var before=f.active();epoch.set(2_000);var result=f.attack(0,1,2);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());
            assertLifetimeAbort(before,result.snapshot(),ModernBattleEngine.AbortReason.SERVER_ABORT);
        }
    }

    private static Fixture lifetimeFixture(ModernBattleEngine.Limits limits,AtomicLong epoch,
            ModernPhysicalCombat.BoundedRandom random)throws Exception {
        return new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),1,100,false,limits,random,10,false,
                epoch::get,ModernBattleEngineCombatTest::absoluteLifetime,1_000);
    }
    private static ModernPhysicalCombat.Source absoluteLifetime(ModernPhysicalCombat.Source source) {
        var q=source.input().equipment();var facts=new TreeMap<Long,ModernItemLifetime.Fact>();
        for(var item:q.source().inventory())facts.put(item.objectId(),new ModernItemLifetime.Fact(item.objectId(),
                item.template().catalogId(),item.template().kind(),item.equipped(),OptionalLong.of(0),false,
                OptionalLong.empty(),Optional.of("1970-01-01 00:00:01.000000"),0,false));
        var observation=new ModernItemLifetime.Observation(q.source().sourceId(),q.source().character().objectId(),
                "1970-01-01 00:00:00.000000",1_000,0,facts);
        var qualified=new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),observation);
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,source.input().weaponsQualified(),source.input().weapons()),
                source.growth(),source.templates(),source.procCatalogIds());
    }
    private static void assertLifetimeAbort(ModernBattleEngine.Snapshot before,ModernBattleEngine.Snapshot after,
            ModernBattleEngine.AbortReason reason) {
        assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertEquals(reason,after.abortReason());
        assertEquals(before.revision()+1,after.revision()); // Only the terminal transition publishes a revision.
        assertEquals(before.actors(),after.actors());assertEquals(before.vitals(),after.vitals());
    }

    private static void assertNoAttack(ModernBattleEngine.Snapshot snapshot,long expectedSequence) {
        assertEquals(expectedSequence,snapshot.vitals().get(0).attackSequence());
        assertTrue(snapshot.vitals().stream().allMatch(v->v.attackRevision()==0&&v.hurtRevision()==0&&v.deathRevision()==0&&v.hp()==v.maxHp()));
    }
    private static final class Rolls implements ModernPhysicalCombat.BoundedRandom {
        private final long[] values;private int at;
        Rolls(long... values){this.values=values;}
        @Override public long nextLong(long bound){assertTrue(at<values.length);long value=values[at++];assertTrue(value>=0&&value<bound);return value;}
    }
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernWorldMap map;final ModernWorldBinding binding;
        final AtomicLong clock=new AtomicLong(1_000_000_000L);final List<ModernBattleEngine.PreparedMember> prepared;
        final ModernBattleEngine.Limits limits;final ModernBattleEngine engine;
        Fixture()throws Exception{this(2,map(false),List.of(cell(2,2),cell(3,2)),0,100,false,IMMEDIATE,bound->0);}
        Fixture(int count,ModernWorldMap map,List<ModernWorldMap.Cell> positions,long type,long hp,boolean old,ModernBattleEngine.Limits limits,ModernPhysicalCombat.BoundedRandom random)throws Exception {
            this(count,map,positions,type,hp,old,limits,random,10);
        }
        Fixture(int count,ModernWorldMap map,List<ModernWorldMap.Cell> positions,long type,long hp,boolean old,ModernBattleEngine.Limits limits,ModernPhysicalCombat.BoundedRandom random,long ac)throws Exception {
            this(count,map,positions,type,hp,old,limits,random,ac,false);
        }
        Fixture(int count,ModernWorldMap map,List<ModernWorldMap.Cell> positions,long type,long hp,boolean old,ModernBattleEngine.Limits limits,
                ModernPhysicalCombat.BoundedRandom random,long ac,boolean overflow)throws Exception {
            this(count,map,positions,type,hp,old,limits,random,ac,overflow,System::currentTimeMillis,UnaryOperator.identity(),0);
        }
        Fixture(int count,ModernWorldMap map,List<ModernWorldMap.Cell> positions,long type,long hp,boolean old,ModernBattleEngine.Limits limits,
                ModernPhysicalCombat.BoundedRandom random,long ac,boolean overflow,LongSupplier epochClock,
                UnaryOperator<ModernPhysicalCombat.Source> transform,long evaluatedAtMillis)throws Exception {
            this.map=map;this.limits=limits;binding=new ModernWorldBinding(1,514,map);
            for(int i=0;i<count;i++)players.add(rooms.player(i==0?7:i==1?42:99,3333,30,0));
            ModernRoomsTest.accepted(rooms.create(players.get(0),ModernRoomsTest.draft(100,1,514,0)));
            for(int i=1;i<count;i++)ModernRoomsTest.accepted(rooms.join(players.get(i),rooms.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var summary=rooms.view(players.get(0)).joined().summary();var prep=rooms.rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(prep.result());ticket=prep.ticket();var values=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<ticket.participants().size();i++) {
                var participant=ticket.participants().get(i);var m=participant.member();
                var row=new ModernApplicationProtocol.CharacterRow(m.key().objectId(),m.name(),m.characterClass(),m.sex(),m.level());
                var source=ModernPhysicalCombatTest.fixtureSource(m.key().sourceId(),row,hp,ac,type);
                if(overflow)source=ModernPhysicalCombatTest.weaponFacts(source,new ModernCombatProfile.WeaponFacts(1,1,1,Long.MAX_VALUE,1,1,11,1,0,0,0,0,0));
                source=transform.apply(source);
                var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),evaluatedAtMillis);
                values.add(old?new ModernBattleEngine.PreparedMember(participant.owner(),participant.scope(),row,positions.get(i),STEP)
                        :new ModernBattleEngine.PreparedMember(participant.owner(),participant.scope(),row,positions.get(i),STEP,fighter,ATTACK));
            }
            prepared=List.copyOf(values);engine=new ModernBattleEngine(ticket,binding,prepared,limits,clock::get,epochClock,random);
        }
        ModernBattleEngine.Snapshot view(){var p=players.get(0);return engine.snapshot(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        void ack(int index){var p=players.get(index);engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());}
        ModernBattleEngine.Snapshot active(){for(int i=0;i<players.size();i++)ack(i);clock.addAndGet(limits.countdownNanos());return engine.tick();}
        ModernBattleEngine.AttackResult attack(int index,long seq,long target){var p=players.get(index);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,target);}
        ModernBattleEngine.MoveResult move(int index,long seq,ModernWorldMap.Cell from,int heading){var p=players.get(index);return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,from.x(),from.y(),heading);}
        @Override public void close(){engine.close();rooms.rooms.releasePreparation(ticket);for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
    }
}
