package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernTimedEffects.*;
import static local.lito381.server.ModernTimedEffectsTest.sec;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernHasteLeaseTest {
    @Test void exactTargetIdentityOriginAnd1200SecondLeaseDoNotBecomePotionOrSelf54() {
        for (long target : new long[]{1, 100, Long.MAX_VALUE}) {
            var s = applyHaste(initial(target), sec(10), 7);
            assertEquals(target, s.entityId()); assertEquals(7, s.revision());
            assertEquals(sec(10), s.observedNanos()); assertEquals(sec(1210), s.hasteUntilNanos());
            assertEquals(HasteOrigin.HASTE, s.hasteOrigin());
            assertEquals(s.hasteOrigin(), s.wireRevision().hasteOrigin());
        }
        assertEquals(0, HasteOrigin.NONE.ordinal()); assertEquals(1, HasteOrigin.POTION.ordinal());
        assertEquals(2, HasteOrigin.GREATER_HASTE.ordinal());
        assertThrows(IllegalArgumentException.class, () -> new State(1, 1, 0, sec(1200), 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new State(1, 1, 0, sec(1200) + 1, 0, 0, 0, HasteOrigin.HASTE));
        assertThrows(IllegalArgumentException.class, () -> new State(1, 1, 0, 0, 0, 0, 0, HasteOrigin.HASTE));
    }
    @Test void refreshAndCrossFamilyOverwriteReplaceDeadlineEvenWhenShorter() {
        var strong = applyGreaterHaste(initial(1), 0, 1);
        var normal = applyHaste(strong, sec(10), 2);
        assertEquals(sec(1210), normal.hasteUntilNanos()); assertEquals(sec(2400), strong.hasteUntilNanos());
        var refresh = applyHaste(normal, sec(20), 3); assertEquals(sec(1220), refresh.hasteUntilNanos());
        var potion = apply(refresh, Kind.HASTE, sec(21), 4, 0);
        assertEquals(sec(321), potion.hasteUntilNanos()); assertEquals(HasteOrigin.POTION, potion.hasteOrigin());
        var again = applyHaste(potion, sec(22), 5); assertEquals(sec(1222), again.hasteUntilNanos());
        var greater = applyGreaterHaste(again, sec(23), 6);
        assertEquals(sec(2423), greater.hasteUntilNanos()); assertEquals(HasteOrigin.GREATER_HASTE, greater.hasteOrigin());
    }
    @Test void independentBraveWisdomExpiryAndExactHasteBoundary() {
        var wisdom = apply(initial(1), Kind.WISDOM, 0, 1, 3);
        var brave = apply(wisdom, Kind.BRAVE, sec(5), 2, 3);
        var haste = applyHaste(brave, sec(10), 3);
        var first = expire(haste, sec(300), 4);
        assertEquals(0, first.wisdomUntilNanos()); assertEquals(sec(305), first.braveUntilNanos());
        assertEquals(HasteOrigin.HASTE, first.hasteOrigin());
        var second = expire(first, sec(305), 5);
        assertSame(second, expire(second, sec(1210) - 1, 6));
        var end = expire(second, sec(1210), 6);
        assertFalse(end.hasEffects()); assertEquals(HasteOrigin.NONE, end.hasteOrigin());
        assertSame(end, expire(end, sec(1211), 7));
    }
    @Test void clearDeathOrRetirementPreservesEvidenceAndDoesNotImportExpiredEffects() {
        var brave = apply(initial(1), Kind.BRAVE, 0, 1, 0);
        var s = applyHaste(brave, sec(300), 2); assertEquals(0, s.braveKind());
        var cleared = clear(s, sec(301), 3);
        assertEquals(3, cleared.revision()); assertEquals(sec(301), cleared.observedNanos());
        assertFalse(cleared.hasEffects()); assertSame(cleared, clear(cleared, sec(302), 4));
        var late = applyHaste(cleared, MAX_OBSERVED_NANOS, 4);
        assertEquals(sec(3000), late.hasteUntilNanos());
        assertThrows(IllegalArgumentException.class, () -> expire(late, MAX_OBSERVED_NANOS + 1, 5));
    }
    @Test void targetSpeedUsesFrozenWideIntervalAndDoesNotStackOnRefresh() {
        var s = applyHaste(initial(42), 0, 1);
        for (var action : Action.values()) {
            assertEquals(251_000_000L, intervalNanos(333_000_000L, action, s));
            assertEquals(251_000_000L, intervalNanos(333_000_000L, action, applyHaste(s, sec(1), 2)));
            assertEquals(333_000_000L, intervalNanos(333_000_000L, action, expire(s, sec(1200), 2)));
            assertEquals(1_000_000L, intervalNanos(1_000_000L, action, s));
        }
        var elf = apply(s, Kind.ELF_BRAVE, 0, 2, 2);
        assertEquals(487_000_000L, intervalNanos(sec(1), Action.MOVE, elf));
        assertEquals(438_000_000L, intervalNanos(sec(1), Action.ATTACK, elf));
        assertThrows(IllegalArgumentException.class, () -> intervalNanos(333_000_001L, Action.MOVE, s));
    }
    @Test void staleClockRevisionAndInvalidTargetNeverModifyOriginal() {
        var s = applyHaste(initial(1), sec(10), 5);
        assertThrows(IllegalArgumentException.class, () -> applyHaste(s, sec(9), 6));
        assertThrows(IllegalArgumentException.class, () -> applyHaste(s, sec(11), 5));
        assertThrows(IllegalArgumentException.class, () -> applyHaste(s, sec(11), 4));
        assertThrows(IllegalArgumentException.class, () -> applyHaste(s, MAX_OBSERVED_NANOS + 1, 6));
        assertThrows(IllegalArgumentException.class, () -> applyHaste(initial(0), 0, 1));
        assertEquals(sec(1210), s.hasteUntilNanos()); assertEquals(5, s.revision());
    }
    @Test void oldBuffWriterRejectsSkill43BeforeWritingAnyBytesEvenWhenRemainingIsBelow300() {
        var s = applyHaste(initial(1), 0, 1);
        for (var candidate : List.of(s, new State(1, 2, sec(1000), sec(1200), 0, 0, 0, HasteOrigin.HASTE))) {
            var b = ByteBuffer.allocate(1024); b.putInt(123);
            assertThrows(IllegalArgumentException.class, () -> ModernBuffProtocol.putStates(b, List.of(candidate)));
            assertEquals(4, b.position()); assertEquals(123, b.getInt(0));
        }
        assertDoesNotThrow(() -> ModernBuffProtocol.putStates(ByteBuffer.allocate(1024), List.of(clear(s, sec(1), 2))));
    }
    @Test void canonicalWorldCannotTreatTarget43AsTheAlreadyNegotiatedSelf54Receipt() throws Exception {
        try (var f = ModernBattleGreaterHasteTest.fixture()) {
            var initial = ModernBattleGreaterHasteTest.active(f);
            var candidate = applyHaste(initial.actors().get(0).buffs(), 0, initial.revision());
            assertThrows(IllegalArgumentException.class, () -> ModernBattleGreaterHasteTest.withBuff(initial, candidate).world());
            var real = ModernBattleGreaterHasteTest.cast(f, 0, 1).observation();
            var previous = real.actors().get(0).buffs();
            var impostor = new State(previous.entityId(), previous.revision(), previous.observedNanos(), sec(1200),
                    0, 0, 0, HasteOrigin.HASTE);
            assertThrows(IllegalArgumentException.class, () -> ModernBattleGreaterHasteTest.withBuff(real, impostor).world());
            assertDoesNotThrow(real::world); assertEquals(60, real.actors().get(0).resources().flows().spent());
        }
    }
}
