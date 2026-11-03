package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcRosterTest.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Actual target roster mutations and action clocks, NOT a player cast/TLS claim. */
class ModernNpcHasteTest {
    static ModernNpcRoster.Actor haste(ModernNpcRoster r, ModernNpcRoster.Actor a) {
        return r.commitIncoming(ModernNpcHaste.plan(r.snapshot(), a.id(), a.life()));
    }
    static ModernNpcRoster.Actor current(ModernNpcRoster r) { return r.snapshot().actors().get(0); }
    static void noOldProjection(ModernNpcRoster.Actor a) {
        assertThrows(IllegalArgumentException.class, () -> ModernNpcProtocol.Actor.from(a));
        assertThrows(IllegalArgumentException.class, () -> ModernBattleAudience.Npc.from(a));
        assertThrows(IllegalArgumentException.class, () -> ModernNpcMotion.View.from(a));
    }
    @Test void leaseChangesNextMoveWithoutRewritingAlreadyCommittedStepOrTemplate() throws Exception {
        try (var r = roster()) {
            var first = ModernNpcMotionTest.moved(r); var walk = first.walk();
            r.advance(SCOPE, 150); var h = haste(r, first);
            assertSame(walk, h.walk()); assertSame(first.template(), h.template());
            assertEquals(100, walk.durationMillis()); assertEquals(200, h.nextMoveMillis());
            assertEquals(new ModernNpcMotion.Pace(75, 566), h.pace());
            assertEquals(1_200_150, h.speed().hasteUntilMillis());
            assertEquals(first.hp(), h.hp()); assertEquals(first.mp(), h.mp());
            rejectedUnchanged(r, () -> r.move(SCOPE, plan(r, h, 2), Set.of()));
            r.advance(SCOPE, 200); var step = r.move(SCOPE, plan(r, h, 2), Set.of());
            assertEquals(75, step.walk().durationMillis()); assertEquals(275, step.nextMoveMillis());
            assertSame(h.speed(), step.walkSpeed()); assertEquals(2, step.walk().sequence());
            r.advance(SCOPE, 220); var refreshed = haste(r, step);
            assertSame(step.walk(), refreshed.walk()); assertEquals(275, refreshed.nextMoveMillis());
            assertEquals(1_200_220, refreshed.speed().hasteUntilMillis());
            assertEquals(h.pace(), refreshed.pace());
            r.advance(SCOPE, 1_200_219); assertTrue(current(r).speed().hasted());
            r.advance(SCOPE, 1_200_220); var expired = current(r);
            assertFalse(expired.speed().hasted()); assertSame(step.walk(), expired.walk());
            assertEquals(75, expired.walk().durationMillis()); assertEquals(100, expired.pace().walkMillis());
            var normal = r.move(SCOPE, plan(r, expired, 2), Set.of());
            assertEquals(100, normal.walk().durationMillis()); assertEquals(1_200_320, normal.nextMoveMillis());
            assertEquals(3, normal.walk().sequence()); noOldProjection(normal);
        }
    }
    @Test void ordinaryAttackClockUsesEffectivePaceAndRefreshCannotGrantAnEarlyHit() throws Exception {
        try (var r = roster()) {
            var a = haste(r, one(r)); r.advance(SCOPE, 100);
            var t = target(7, 0, 11, 10); var players = Set.of(t.cell());
            var p = r.pursue(SCOPE, a.id(), a.life(), List.of(t), players, 4096, () -> false);
            var hit = r.attack(SCOPE, p, t, players); assertEquals(666, hit.nextAttackMillis());
            r.advance(SCOPE, 101); var refreshed = haste(r, hit); assertEquals(666, refreshed.nextAttackMillis());
            var early = r.pursue(SCOPE, a.id(), a.life(), List.of(t), players, 4096, () -> false);
            rejectedUnchanged(r, () -> r.attack(SCOPE, early, t, players));
            rejectedUnchanged(r, () -> r.move(SCOPE, plan(r, refreshed, 4), players));
            r.advance(SCOPE, 666);
            var next = r.pursue(SCOPE, a.id(), a.life(), List.of(t), players, 4096, () -> false);
            assertEquals(1232, r.attack(SCOPE, next, t, players).nextAttackMillis());
            r.advance(SCOPE, 1_200_101);
            var ended = r.pursue(SCOPE, a.id(), a.life(), List.of(t), players, 4096, () -> false);
            assertEquals(1_200_851, r.attack(SCOPE, ended, t, players).nextAttackMillis());
        }
    }
    @Test void deadRevivedAndRetiredIdentityCannotInheritOrReceiveAnOldLease() throws Exception {
        try (var r = roster()) {
            var a = haste(r, one(r)); r.advance(SCOPE, 100);
            var step = r.move(SCOPE, plan(r, a, 2), Set.of());
            var old = ModernNpcHaste.plan(r.snapshot(), a.id(), 1);
            var dead = r.damage(SCOPE, a.id(), 1, Long.MAX_VALUE);
            assertFalse(dead.speed().hasted()); assertSame(step.walk(), dead.walk()); noOldProjection(dead);
            rejectedUnchanged(r, () -> r.commitIncoming(old));
            rejectedUnchanged(r, () -> ModernNpcHaste.plan(r.snapshot(), a.id(), 1));
            var revived = r.resurrect(SCOPE, a.id(), 1, 280, 10, Set.of());
            assertEquals(2, revived.life()); assertFalse(revived.speed().hasted());
            assertEquals(200, revived.nextMoveMillis()); assertEquals(850, revived.nextAttackMillis());
            assertSame(step.walk(), revived.walk());
            rejectedUnchanged(r, () -> r.commitIncoming(old));
            rejectedUnchanged(r, () -> ModernNpcHaste.plan(r.snapshot(), a.id(), 1));
            var fresh = haste(r, revived); assertEquals(2, fresh.life());
            r.damage(SCOPE, a.id(), 2, Long.MAX_VALUE); r.advance(SCOPE, 3100);
            assertTrue(r.snapshot().actors().isEmpty());
            var newborn = one(r); assertNotEquals(a.id(), newborn.id());
            assertEquals(ModernNpcSpeed.initial(), newborn.speed()); assertNull(newborn.walk());
            assertDoesNotThrow(() -> ModernNpcMotion.View.from(newborn));
            rejectedUnchanged(r, () -> r.commitIncoming(old));
        }
    }
    @Test void allStaleOrForeignCasPlansAreRejectedWithoutAClockLifeOrHpChange() throws Exception {
        try (var r = roster()) {
            var a = one(r); var plan = ModernNpcHaste.plan(r.snapshot(), a.id(), 1);
            r.advance(SCOPE, 1); rejectedUnchanged(r, () -> r.commitIncoming(plan));
            var other = new ModernNpcRoster.Id(new ModernNpcRoster.Scope(11, 22, 34), a.id().serial());
            rejectedUnchanged(r, () -> ModernNpcHaste.plan(r.snapshot(), other, 1));
            rejectedUnchanged(r, () -> ModernNpcHaste.plan(r.snapshot(), new ModernNpcRoster.Id(SCOPE, 99), 1));
            rejectedUnchanged(r, () -> ModernNpcHaste.plan(r.snapshot(), a.id(), 2));
            var beforeHurt = ModernNpcHaste.plan(r.snapshot(), a.id(), 1);
            r.damage(SCOPE, a.id(), 1, 1); rejectedUnchanged(r, () -> r.commitIncoming(beforeHurt));
            var valid = ModernNpcHaste.plan(r.snapshot(), a.id(), 1); r.commitIncoming(valid);
            rejectedUnchanged(r, () -> r.commitIncoming(valid));
            var beforeMove = ModernNpcHaste.plan(r.snapshot(), a.id(), 1);
            r.advance(SCOPE, 100); r.move(SCOPE, plan(r, current(r), 2), Set.of());
            rejectedUnchanged(r, () -> r.commitIncoming(beforeMove));
            r.close(); rejectedUnchanged(r, () -> r.commitIncoming(valid));
            rejectedUnchanged(r, () -> ModernNpcHaste.plan(r.snapshot(), a.id(), 1));
        }
    }
    @Test void duplicateConcurrentCasHasOneWinnerAndNoDoubleRevision() throws Exception {
        try (var r = roster()) {
            var a = one(r); var before = r.snapshot(); var plan = ModernNpcHaste.plan(before, a.id(), 1);
            var go = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Callable<Boolean> task = () -> { go.await(); try { r.commitIncoming(plan); return true; } catch (IllegalArgumentException e) { return false; } };
                var first = pool.submit(task); var second = pool.submit(task); go.countDown();
                assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
                assertEquals(before.revision() + 1, r.snapshot().revision());
                assertEquals(1_200_000, current(r).speed().hasteUntilMillis());
            } finally { go.countDown(); pool.shutdown(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
        }
    }
    @Test void hurtHealManaAoeAndWalkRetainTheLeaseButAoeDeathClearsIt() throws Exception {
        for (long hp : new long[]{1, 1000}) try (var r = roster()) {
            var c = ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP, hp, ModernNpcCatalog.Field.MR, 0L), true, 0);
            var rows = ModernNpcFireStormTest.spawn(r, c);
            for (var a : rows) haste(r, a);
            var before = r.snapshot().actors();
            var batch = ModernNpcFireStormTest.plan(r, c, before); var after = r.commitIncoming(batch);
            for (int i = 0; i < after.size(); i++) {
                assertEquals(hp > 1, after.get(i).alive()); assertEquals(hp > 1, after.get(i).speed().hasted());
                if (hp > 1) assertSame(before.get(i).speed(), after.get(i).speed());
                else assertEquals(r.snapshot().revision(), after.get(i).speed().revision());
            }
        }
        try (var r = roster()) {
            var a = haste(r, one(r)); var speed = a.speed();
            assertSame(speed, r.damage(SCOPE, a.id(), 1, 1).speed());
            assertSame(speed, r.heal(SCOPE, a.id(), 1, 1).speed());
            assertSame(speed, r.drainMana(SCOPE, a.id(), 1, 1).speed());
        }
    }
    @Test void oldViewsRejectActiveExpiredAndClearedSpeedInsteadOfSilentlyDroppingIt() throws Exception {
        try (var r = roster()) {
            var a = haste(r, one(r)); noOldProjection(a);
            r.advance(SCOPE, 100); var moved = r.move(SCOPE, plan(r, a, 2), Set.of()); noOldProjection(moved);
            assertSame(a.speed(), moved.walkSpeed());
            r.advance(SCOPE, 1_200_000); noOldProjection(current(r));
            noOldProjection(r.damage(SCOPE, a.id(), 1, Long.MAX_VALUE));
            noOldProjection(r.resurrect(SCOPE, a.id(), 1, 280, 10, Set.of()));
        }
    }
    @Test void leaseMutationDoesNotInvalidateOccupiedGridProofOrPermitOccupiedAndEarlyMoves() throws Exception {
        try (var r = roster()) {
            var a = one(r); r.advance(SCOPE, 100);
            var t = target(7, 0, 15, 10); var players = Set.of(t.cell());
            var p = r.pursue(SCOPE, a.id(), 1, List.of(t), players, 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.MOVE, p.kind()); var h = haste(r, a);
            rejectedUnchanged(r, () -> r.move(SCOPE, p, players));
            var next = r.pursue(SCOPE, a.id(), 1, List.of(t), players, 4096, () -> false);
            assertEquals(p.heading(), next.heading());
            var moved = r.move(SCOPE, next, players); assertEquals(75, moved.walk().durationMillis());
            rejectedUnchanged(r, () -> r.move(SCOPE, plan(r, moved, 2), players));
            r.advance(SCOPE, 175);
            var blocked = new ModernWorldMap.Cell(moved.cell().x() + 1, moved.cell().y());
            rejectedUnchanged(r, () -> r.move(SCOPE, plan(r, moved, 2), Set.of(blocked)));
            assertEquals(h.speed(), current(r).speed());
        }
    }
    @Test void exactExpiryUpdatesOnlyExpiredActorsAndIdleReadsAreImmutable() throws Exception {
        try (var r = roster()) {
            var a = one(r); var second = r.spawn(SCOPE, List.of(spawn(12, 10)), Set.of()).get(0);
            var h = haste(r, a); var original = r.snapshot();
            assertEquals(original, r.advance(SCOPE, 0));
            r.advance(SCOPE, 1_199_999); assertSame(h, current(r));
            var expired = r.advance(SCOPE, 1_200_000);
            assertEquals(expired.revision(), expired.actors().get(0).changedRevision());
            assertEquals(expired.revision(), expired.actors().get(0).speed().revision());
            assertSame(second, expired.actors().get(1));
            assertEquals(expired, r.advance(SCOPE, 1_200_000));
            assertTrue(original.actors().get(0).speed().hasted());
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(original.scope(), original.manifest(),
                    expired.revision(), 1_200_000, original.spawned(), false, original.actors()));
        }
    }
    @Test void allQualifiedIntervalsMatchNativeTruncationWithoutMutatingFrozenSmallBases() {
        var s = ModernNpcSpeed.haste(ModernNpcSpeed.initial(), 0, 1);
        for (int millis = 1; millis <= 60_000; millis++) {
            var base = new ModernNpcMotion.Pace(millis, 60_001 - millis);
            var actual = ModernNpcSpeed.pace(base, s);
            assertEquals(Math.max(1, (int) (millis * 0.755d)), actual.walkMillis());
            if (millis >= 40) assertEquals(BattleActionInterval.npcSourceMillis(millis, true, false), actual.walkMillis());
            assertEquals(Math.max(1, (int) ((60_001 - millis) * 0.755d)), actual.attackMillis());
            assertSame(base, ModernNpcSpeed.pace(base, ModernNpcSpeed.expire(s, 1_200_000, 2)));
        }
    }
    @Test void invalidStateClockRevisionDurationAndHistoricalStepAreRejected() throws Exception {
        var empty = ModernNpcSpeed.initial(); var s = ModernNpcSpeed.haste(empty, 100, 5);
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcSpeed.State(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcSpeed.State(1, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcSpeed.State(1, 100, 1_200_101));
        assertThrows(IllegalArgumentException.class, () -> ModernNpcSpeed.haste(s, 99, 6));
        assertThrows(IllegalArgumentException.class, () -> ModernNpcSpeed.haste(s, 101, 5));
        assertThrows(IllegalArgumentException.class, () -> ModernNpcSpeed.expire(s, ModernNpcRoster.MAX_ELAPSED_MILLIS + 1, 6));
        assertThrows(IllegalArgumentException.class, () -> s.at(1_200_100));
        var late = ModernNpcSpeed.haste(s, ModernNpcRoster.MAX_ELAPSED_MILLIS, 6);
        assertEquals(ModernNpcRoster.MAX_ELAPSED_MILLIS + 1_200_000, late.hasteUntilMillis());
        try (var r = roster()) {
            var a = haste(r, one(r)); r.advance(SCOPE, 100); var step = r.move(SCOPE, plan(r, a, 2), Set.of());
            assertThrows(IllegalArgumentException.class, () -> ModernNpcMotionTest.copy(step, step.walk()));
            var future = new ModernNpcSpeed.State(step.walk().rosterRevision(), 0, 1_200_000);
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Actor(step.id(), step.template(), step.lane(), step.stage(),
                    step.cell(), step.heading(), step.hp(), step.mp(), step.life(), step.action(), step.bornRevision(), step.changedRevision(),
                    step.corpseUntilMillis(), step.nextMoveMillis(), step.nextAttackMillis(), step.walk(), future, future));
        }
    }
    @Test void terminalClearIsScopedIdempotentAndNeverRewritesAnAlreadyCommittedWalk() throws Exception {
        try(var r=roster()) {
            var a=haste(r,one(r));r.advance(SCOPE,100);var step=r.move(SCOPE,plan(r,a,2),Set.of());
            var before=r.snapshot();rejectedUnchanged(r,()->r.clearHaste(new ModernNpcRoster.Scope(11,22,34)));
            r.clearHaste(SCOPE);var after=r.snapshot();var cleared=current(r);
            assertEquals(before.revision()+1,after.revision());assertEquals(after.revision(),cleared.speed().revision());
            assertFalse(cleared.speed().hasted());assertSame(step.walk(),cleared.walk());assertSame(step.walkSpeed(),cleared.walkSpeed());
            assertEquals(step.hp(),cleared.hp());assertEquals(step.mp(),cleared.mp());assertEquals(step.life(),cleared.life());
            assertEquals(step.action(),cleared.action());assertEquals(step.nextMoveMillis(),cleared.nextMoveMillis());assertEquals(step.nextAttackMillis(),cleared.nextAttackMillis());
            r.clearHaste(SCOPE);assertEquals(after,r.snapshot());assertTrue(before.actors().get(0).speed().hasted());
            r.close();rejectedUnchanged(r,()->r.clearHaste(SCOPE));
        }
    }
    @Test void terminalSingleDamageRetiresAllLeasesInOneRevisionWithoutPartialInvalidMutation()throws Exception {
        for(long amount:new long[]{0,1,Long.MAX_VALUE})try(var r=roster()) {
            var a=haste(r,one(r));var other=r.spawn(SCOPE,List.of(spawn(12,10)),Set.of()).get(0);haste(r,other);
            rejectedUnchanged(r,()->r.damage(SCOPE,a.id(),2,amount,true));
            rejectedUnchanged(r,()->r.damage(SCOPE,a.id(),1,-1,true));
            rejectedUnchanged(r,()->r.damage(new ModernNpcRoster.Scope(11,22,34),a.id(),1,amount,true));
            var before=r.snapshot();var hit=r.damage(SCOPE,a.id(),1,amount,true);var after=r.snapshot();
            assertEquals(before.revision()+1,after.revision());assertSame(after.actors().get(0),hit);
            assertEquals(Math.max(0,a.hp()-Math.min(a.hp(),amount)),hit.hp());
            assertTrue(after.actors().stream().noneMatch(x->x.speed().hasted()));
            assertEquals(other.hp(),after.actors().get(1).hp());assertEquals(other.action(),after.actors().get(1).action());
            assertTrue(before.actors().stream().allMatch(x->x.speed().hasted()));
        }
    }
    @Test void terminalNpcAttackRejectsStalePlanThenClearsInTheSameCommittedAttackRevision()throws Exception {
        try(var r=roster()) {
            var a=haste(r,one(r));var other=r.spawn(SCOPE,List.of(spawn(12,10)),Set.of()).get(0);haste(r,other);
            r.advance(SCOPE,100);var t=target(7,0,11,10);var players=Set.of(t.cell());
            var stale=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);r.damage(SCOPE,a.id(),1,1);
            rejectedUnchanged(r,()->r.attack(SCOPE,stale,t,players,true));
            var before=r.snapshot();var plan=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);
            var hit=r.attack(SCOPE,plan,t,players,true);var after=r.snapshot();
            assertEquals(before.revision()+1,after.revision());assertSame(after.actors().get(0),hit);
            assertEquals(666,hit.nextAttackMillis());assertEquals(ModernNpcRoster.Action.ATTACK,hit.action());
            assertEquals(before.actors().get(0).hp(),hit.hp());assertTrue(after.actors().stream().noneMatch(x->x.speed().hasted()));
            assertTrue(before.actors().stream().allMatch(x->x.speed().hasted()));
        }
    }
    @Test void terminalAoeRetiresOtherLaneSpeedInTheSameCasAndReturnsFinalVictims() throws Exception {
        try(var r=roster()) {
            var c=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1000L,ModernNpcCatalog.Field.MR,0L),true,0);
            var victims=ModernNpcFireStormTest.spawn(r,c);
            var other=r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(victims.get(0).template(),1,1,cell(20,10),2)),Set.of()).get(0);
            for(var a:r.snapshot().actors())haste(r,a);
            var stale=ModernNpcFireStormTest.plan(r,c,r.snapshot().actors().subList(0,2));
            r.damage(SCOPE,victims.get(1).id(),1,1);rejectedUnchanged(r,()->r.commitIncoming(stale,true));
            assertTrue(r.snapshot().actors().stream().allMatch(a->a.speed().hasted()));
            var before=r.snapshot();var batch=ModernNpcFireStormTest.plan(r,c,before.actors().subList(0,2));var changed=r.commitIncoming(batch,true);
            var after=r.snapshot();assertEquals(before.revision()+1,after.revision());assertEquals(after.actors().subList(0,2),changed);
            assertTrue(after.actors().stream().noneMatch(a->a.speed().hasted()));
            assertTrue(after.actors().stream().allMatch(a->a.changedRevision()==after.revision()&&a.speed().revision()==after.revision()));
            assertEquals(List.of(965L,964L),changed.stream().map(ModernNpcRoster.Actor::hp).toList());
            assertEquals(other.hp(),after.actors().get(2).hp());assertEquals(other.action(),after.actors().get(2).action());
            assertEquals(other.nextMoveMillis(),after.actors().get(2).nextMoveMillis());assertTrue(before.actors().stream().allMatch(a->a.speed().hasted()));
            rejectedUnchanged(r,()->r.commitIncoming(batch,true));
        }
    }
}
