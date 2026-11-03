package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcRosterTest.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Actual roster commits/publication facts, not enabled wire or client pixels. */
class ModernNpcMotionTest {
    static ModernNpcRoster.Actor moved(ModernNpcRoster r) {
        var a = one(r);
        assertNull(a.walk());
        r.advance(SCOPE, 100);
        return r.move(SCOPE, plan(r, a, 2), Set.of());
    }
    static ModernNpcRoster.Actor copy(ModernNpcRoster.Actor a, ModernNpcMotion.Walk walk) {
        return new ModernNpcRoster.Actor(a.id(), a.template(), a.lane(), a.stage(), a.cell(), a.heading(),
                a.hp(), a.mp(), a.life(), a.action(), a.bornRevision(), a.changedRevision(),
                a.corpseUntilMillis(), a.nextMoveMillis(), a.nextAttackMillis(), walk);
    }
    @Test void successCapturesExactSourcePaceIdentityLifeCellsAndCommitRevision() throws Exception {
        try (var r = roster()) {
            var a = moved(r); var w = a.walk();
            assertEquals(a.id(), w.actor()); assertEquals(1, w.sequence()); assertEquals(1, w.life());
            assertEquals(r.snapshot().revision(), w.rosterRevision());
            assertEquals(100, w.elapsedMillis()); assertEquals(100, w.durationMillis());
            assertEquals(cell(10, 10), w.from()); assertEquals(cell(11, 10), w.to()); assertEquals(2, w.heading());
            assertEquals(200, a.nextMoveMillis());
            var v = ModernNpcMotion.View.from(a);
            assertSame(w, v.walk()); assertEquals(new ModernNpcMotion.Pace(100, 750), v.pace());
            assertEquals(a.id(), v.actor()); assertEquals(a.life(), v.life());
        }
    }
    @Test void earlyStaleOccupiedAndForeignMovesNeverConsumeOrEraseSequence() throws Exception {
        try (var r = roster()) {
            var born = one(r); var early = plan(r, born, 2);
            rejectedUnchanged(r, () -> r.move(SCOPE, early, Set.of()));
            r.advance(SCOPE, 100);
            rejectedUnchanged(r, () -> r.move(SCOPE, early, Set.of()));
            var valid = plan(r, born, 2);
            rejectedUnchanged(r, () -> r.move(SCOPE, valid, Set.of(cell(11, 10))));
            var other = new ModernNpcRoster.Scope(11, 22, 34);
            rejectedUnchanged(r, () -> r.move(other, valid, Set.of()));
            var first = r.move(SCOPE, valid, Set.of()); assertEquals(1, first.walk().sequence());
            rejectedUnchanged(r, () -> r.move(SCOPE, valid, Set.of()));
            var tooEarly = plan(r, first, 2);
            rejectedUnchanged(r, () -> r.move(SCOPE, tooEarly, Set.of()));
            assertSame(first.walk(), r.snapshot().actors().get(0).walk());
        }
    }
    @Test void lateTicksDoNotGenerateCatchupStepsAndOnlyLatestSuccessfulStepIsRetained() throws Exception {
        try (var r = roster()) {
            var first = moved(r); r.advance(SCOPE, 10_000);
            assertSame(first.walk(), r.snapshot().actors().get(0).walk());
            var second = r.move(SCOPE, plan(r, first, 4), Set.of());
            assertEquals(2, second.walk().sequence()); assertEquals(10_000, second.walk().elapsedMillis());
            assertEquals(first.cell(), second.walk().from()); assertEquals(cell(11, 11), second.walk().to());
            assertEquals(10_100, second.nextMoveMillis()); assertEquals(1, first.walk().sequence());
            assertEquals(1, r.snapshot().actors().size());
            var before = r.snapshot(); assertEquals(before, r.advance(SCOPE, 10_000));
            var noBurst = plan(r, second, 4);
            rejectedUnchanged(r, () -> r.move(SCOPE, noBurst, Set.of()));
        }
    }
    @Test void hurtHealManaAndAttackKeepOriginalMoveTimeWithoutReplayingIt() throws Exception {
        try (var r = roster()) {
            var first = moved(r); var w = first.walk();
            assertSame(w, r.damage(SCOPE, first.id(), 1, 1).walk());
            assertSame(w, r.heal(SCOPE, first.id(), 1, 1).walk());
            assertSame(w, r.drainMana(SCOPE, first.id(), 1, 1).walk());
            r.advance(SCOPE, 200);
            var t = target(7, 0, 11, 11); var players = Set.of(t.cell());
            var p = r.pursue(SCOPE, first.id(), 1, List.of(t), players, 4096, () -> false);
            var attack = r.attack(SCOPE, p, t, players);
            assertEquals(4, attack.heading()); assertSame(w, attack.walk());
            assertEquals(ModernNpcRoster.Action.ATTACK, attack.action());
            assertEquals(950, attack.nextAttackMillis()); assertEquals(100, w.elapsedMillis());
        }
    }
    @Test void deathRevivalKeepsHistoricalLifeAndNextMoveUsesNewLifeWithoutSequenceReuse() throws Exception {
        try (var r = roster()) {
            var first = moved(r); var w = first.walk();
            var oldPlan = plan(r, first, 2);
            var dead = r.damage(SCOPE, first.id(), 1, Long.MAX_VALUE);
            assertSame(w, dead.walk()); assertEquals(0, dead.nextMoveMillis());
            var revived = r.resurrect(SCOPE, first.id(), 1, 280, 10, Set.of());
            assertEquals(2, revived.life()); assertSame(w, revived.walk()); assertEquals(1, revived.walk().life());
            rejectedUnchanged(r, () -> r.move(SCOPE, oldPlan, Set.of()));
            r.advance(SCOPE, 850);
            var next = r.move(SCOPE, plan(r, revived, 2), Set.of());
            assertEquals(2, next.walk().sequence()); assertEquals(2, next.walk().life());
            assertEquals(revived.cell(), next.walk().from());
        }
    }
    @Test void corpseRetirementAndNewSerialDoNotInheritPriorNpcMovement() throws Exception {
        try (var r = roster()) {
            var first = moved(r); r.damage(SCOPE, first.id(), 1, Long.MAX_VALUE);
            r.advance(SCOPE, 3100); assertTrue(r.snapshot().actors().isEmpty());
            var next = one(r); assertNotEquals(first.id(), next.id()); assertNull(next.walk());
            r.advance(SCOPE, 3200);
            var step = r.move(SCOPE, plan(r, next, 2), Set.of());
            assertEquals(1, step.walk().sequence()); assertEquals(next.id(), step.walk().actor());
        }
    }
    @Test void allEightDirectionsAndFullQualifiedSourcePaceRangeAreExact() throws Exception {
        for (int pace : new int[]{1, 100, 60_000}) for (int heading = 0; heading < 8; heading++) try (var r = roster()) {
            var t = new ModernNpcRoster.Template(42, "separate_catalog", 45262, 1649, 280, 10, pace, 60_001 - pace);
            var a = r.spawn(SCOPE, List.of(new ModernNpcRoster.Spawn(t, 0, 1, cell(10, 10), 0)), Set.of()).get(0);
            r.advance(SCOPE, pace);
            var step = r.move(SCOPE, plan(r, a, heading), Set.of());
            assertEquals(heading, step.walk().heading()); assertEquals(pace, step.walk().durationMillis());
            assertEquals(2L * pace, step.nextMoveMillis());
            assertEquals(ModernNpcMotion.Pace.from(t), ModernNpcMotion.View.from(step).pace());
        }
    }
    @Test void incomingAoeCommitsPreserveWalkAndStaleBatchCannotChangeIt() throws Exception {
        try (var r = roster()) {
            var c = ModernNpcMeleeTest.catalog();
            var rows = ModernNpcFireStormTest.spawn(r, c); var first = rows.get(0);
            r.advance(SCOPE, first.template().walkMillis());
            var step = r.move(SCOPE, plan(r, first, 2), Set.of(cell(10, 10)));
            var batch = ModernNpcFireStormTest.plan(r, c, List.of(step, rows.get(1)));
            var after = r.commitIncoming(batch);
            assertSame(step.walk(), after.get(0).walk()); assertNull(after.get(1).walk());
            rejectedUnchanged(r, () -> r.commitIncoming(batch));
        }
    }
    @Test void invalidBoundsHeadingOwnerLifeAndActorBindingAreRejected() throws Exception {
        for (int bad : new int[]{-1, 0, 120_001, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcMotion.Pace(bad, 100));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcMotion.Pace(100, bad));
        }
        try (var r = roster()) {
            var a = moved(r); var w = a.walk();
            for (int field = 0; field < 12; field++) {
                int f = field;
                assertThrows(IllegalArgumentException.class, () -> {
                    var changed = new ModernNpcMotion.Walk(
                            f == 0 ? new ModernNpcRoster.Id(SCOPE, 99) : w.actor(),
                            f == 1 ? 0 : f == 2 ? Long.MAX_VALUE : w.sequence(),
                            f == 3 ? 0 : f == 4 ? 2 : w.life(),
                            f == 5 ? a.bornRevision() : f == 6 ? w.rosterRevision() + 1 : w.rosterRevision(),
                            f == 7 ? -1 : f == 8 ? ModernNpcRoster.MAX_ELAPSED_MILLIS + 1 : w.elapsedMillis(),
                            f == 9 ? 101 : w.durationMillis(), w.from(),
                            f == 10 ? cell(12, 10) : w.to(), f == 11 ? 8 : w.heading());
                    copy(a, changed);
                }, "field " + field);
            }
            assertThrows(IllegalArgumentException.class, () -> copy(a, null));
        }
    }
    static byte[] oldNpcSection(ModernNpcRoster.Snapshot snapshot, ModernNpcRoster.Actor actor) {
        var t = actor.template();
        var rows = new ModernBattleAudience.Npcs(snapshot.revision(), snapshot.elapsedMillis(), snapshot.spawned(), false,
                List.of(ModernBattleAudience.Npc.from(actor)), List.of(), new ModernNpcProtocol.Resources(List.of(t.graphic())),
                new ModernNpcNames.Table(List.of(new ModernNpcNames.Name(t.catalogSourceId(), t.npcId(), t.graphic(), "Npc", "$1"))));
        var buffer = ByteBuffer.allocate(4096);
        ModernBattleWireModes.npcs(new ModernBattleWire.Writer(buffer), rows);
        return java.util.Arrays.copyOf(buffer.array(), buffer.position());
    }
    @Test void retainedMotionDoesNotSilentlyAlterExistingNpcOrCanonicalWireFields() throws Exception {
        try (var r = roster()) {
            var a = moved(r); var hurt = r.damage(SCOPE, a.id(), 1, 1);
            var withoutTrace = copy(hurt, null);
            assertNotEquals(ModernNpcMotion.View.from(hurt), ModernNpcMotion.View.from(withoutTrace));
            assertEquals(ModernBattleAudience.Npc.from(hurt), ModernBattleAudience.Npc.from(withoutTrace));
            assertEquals(ModernNpcProtocol.Actor.from(hurt), ModernNpcProtocol.Actor.from(withoutTrace));
            assertArrayEquals(oldNpcSection(r.snapshot(), hurt), oldNpcSection(r.snapshot(), withoutTrace));
        }
    }
    @Test void candidateProjectionRetainsNoCatalogAndCannotInventAnIsolatedLane() throws Exception {
        try (var r = roster()) {
            var a = moved(r); var snapshot = r.snapshot();
            var views = ModernNpcMotion.project(snapshot, -1);
            assertEquals(List.of(ModernNpcMotion.View.from(a)), views);
            assertThrows(UnsupportedOperationException.class, () -> views.clear());
            for (int lane : new int[]{-2, 0, 1, 2})
                assertThrows(IllegalArgumentException.class, () -> ModernNpcMotion.project(snapshot, lane));
            var v = views.get(0);
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcMotion.View(new ModernNpcRoster.Id(SCOPE, 99), v.life(), v.pace(), v.walk()));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcMotion.View(v.actor(), 0, v.pace(), v.walk()));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcMotion.View(v.actor(), v.life(), new ModernNpcMotion.Pace(101, 750), v.walk()));
            r.advance(SCOPE, 200); r.move(SCOPE, plan(r, a, 2), Set.of());
            assertEquals(1, v.walk().sequence(), "published immutable evidence is not rewritten");
            assertEquals(2, ModernNpcMotion.project(r.snapshot(), -1).get(0).walk().sequence());
        }
    }
}
