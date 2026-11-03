package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernTimedEffects.*;
import static local.lito381.server.ModernTimedEffectsTest.sec;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Target candidates and rejection by old contracts, not a cast admission/UI test. */
class ModernSlowSpeedTest {
    @Test void ordinarySlowRefreshAndExactExpiryPreserveIndependentLeases() {
        var original = apply(apply(initial(1), Kind.WISDOM, sec(1), 1, 3), Kind.BRAVE, sec(2), 2, 3);
        var slowed = applySlow(original, sec(10), 3);
        assertEquals(sec(74), slowed.slowUntilNanos()); assertEquals(3, slowed.slowRevision());
        assertEquals(original.braveUntilNanos(), slowed.braveUntilNanos());
        assertEquals(original.wisdomUntilNanos(), slowed.wisdomUntilNanos()); assertEquals(2, slowed.spBonus());
        var refreshed = applySlow(slowed, sec(20), 4);
        assertEquals(sec(84), refreshed.slowUntilNanos()); assertEquals(sec(74), slowed.slowUntilNanos());
        assertSame(refreshed, expire(refreshed, sec(84)-1, 5));
        var expired = expire(refreshed, sec(84), 5);
        assertEquals(0, expired.slowUntilNanos()); assertEquals(4, expired.slowRevision());
        assertEquals(5, expired.revision()); assertEquals(1, expired.braveKind()); assertEquals(2, expired.spBonus());
        assertSame(expired, expire(expired, sec(85), 6));
        assertEquals(0, original.slowRevision());
    }
    @Test void eachHasteOriginIsNeutralizedByOneSlowThenSecondCastSlows() {
        var first = initial(1);
        for (var haste : List.of(apply(first, Kind.HASTE, 0, 1, 0), applyHaste(first, 0, 1), applyGreaterHaste(first, 0, 1))) {
            var normal = applySlow(haste, sec(1), 2);
            assertFalse(normal.hasEffects()); assertEquals(HasteOrigin.NONE, normal.hasteOrigin());
            assertEquals(2, normal.slowRevision()); assertEquals(2, normal.revision());
            var slowed = applySlow(normal, sec(2), 3);
            assertEquals(sec(66), slowed.slowUntilNanos());
            assertEquals(2*sec(1), intervalNanos(sec(1), Action.MOVE, slowed));
            if (haste.hasteUntilNanos() <= MAX_OBSERVED_NANOS) {
                var atExpiry = applySlow(haste, haste.hasteUntilNanos(), 2);
                assertEquals(haste.hasteUntilNanos()+sec(64), atExpiry.slowUntilNanos());
            } else {
                // Greater Haste outlives the match; expiry cannot extend its clock.
                assertThrows(IllegalArgumentException.class, () -> applySlow(haste,haste.hasteUntilNanos(),2));
                assertFalse(applySlow(haste,MAX_OBSERVED_NANOS,2).hasEffects());
            }
        }
    }
    @Test void spellHasteNeutralizesSlowButPotionRemovesItAndStillAppliesHaste() {
        var slow = applySlow(initial(1), 0, 1);
        for (var normal : List.of(applyHaste(slow, sec(1), 2), applyGreaterHaste(slow, sec(1), 2))) {
            assertFalse(normal.hasEffects()); assertEquals(HasteOrigin.NONE, normal.hasteOrigin());
            assertEquals(1, normal.slowRevision());
        }
        var potion = apply(slow, Kind.HASTE, sec(1), 2, 0);
        assertEquals(sec(301), potion.hasteUntilNanos()); assertEquals(HasteOrigin.POTION, potion.hasteOrigin());
        assertEquals(0, potion.slowUntilNanos()); assertEquals(1, potion.slowRevision());
        assertEquals(sec(64), slow.slowUntilNanos());
        assertEquals(sec(1264), applyHaste(slow, sec(64), 2).hasteUntilNanos());
        assertEquals(sec(2464), applyGreaterHaste(slow, sec(64), 2).hasteUntilNanos());
    }
    @Test void otherPotionsPreserveSlowAndNativeOrderIsTruncatedAtEveryStep() {
        var slow = applySlow(initial(1), 0, 1);
        var brave = apply(slow, Kind.BRAVE, 0, 2, 2);
        var elf = apply(slow, Kind.ELF_BRAVE, 0, 2, 2);
        var wisdom = apply(slow, Kind.WISDOM, 0, 2, 3);
        for (var s : List.of(brave, elf, wisdom)) assertEquals(slow.slowUntilNanos(), s.slowUntilNanos());
        for (int millis = 1; millis <= 10_000; millis++) for (var action : Action.values()) {
            long nanos = millis * 1_000_000L;
            assertEquals(nanos*2, intervalNanos(nanos, action, slow));
            int stage = (int)(millis*2*0.755d);
            assertEquals(Math.max(1,stage)*1_000_000L, intervalNanos(nanos, action, brave));
            stage = (int)(stage*0.855d);
            if (action==Action.ATTACK) stage = (int)(stage*0.9d);
            assertEquals(Math.max(1,stage)*1_000_000L, intervalNanos(nanos, action, elf));
            assertEquals(nanos*2, intervalNanos(nanos, action, wisdom));
        }
        assertThrows(IllegalArgumentException.class, () -> intervalNanos(1_000_001, Action.MOVE, slow));
        assertEquals(1_000_001, intervalNanos(1_000_001, Action.MOVE, clear(slow,1,2)));
        assertEquals(0, apply(slow, Kind.BRAVE, sec(64), 2, 0).slowUntilNanos());
    }
    @Test void activeExpiredClearedNeutralizedAndOverwrittenHistoryCannotLeakOntoOldWire() {
        var slow = applySlow(initial(1), 0, 1);
        for (var s : List.of(slow, expire(slow,sec(64),2), clear(slow,1,2), applyHaste(slow,1,2),
                applyGreaterHaste(slow,1,2), apply(slow,Kind.HASTE,1,2,0))) {
            assertThrows(IllegalArgumentException.class, () -> requireNoSlow(s));
            var bytes = ByteBuffer.allocate(1000);
            assertThrows(IllegalArgumentException.class, () -> ModernBuffProtocol.putStates(bytes,List.of(initial(2),s)));
            assertEquals(0, bytes.position(), "reject before writing list prefix");
            for (boolean origin : new boolean[]{false,true}) {
                assertThrows(IllegalArgumentException.class, () -> ModernBattleWire.buff(new ModernBattleWire.Writer(bytes),s,origin));
                assertEquals(0,bytes.position());
            }
        }
        var gone = clear(slow, 1, 2);
        assertFalse(gone.hasEffects()); assertSame(gone,clear(gone,2,3));
        assertEquals(1,gone.slowRevision()); assertEquals(2,gone.wireRevision().slowRevision());
        assertEquals(3,gone.wireRevision().revision());
    }
    @Test void invalidLeaseRevisionAndClockAreRejectedAndOldConstructorDefaultsRemainExact() {
        var empty = initial(1); var slow = applySlow(empty,100,5);
        assertEquals(new State(1,0,0,0,0,0,0,HasteOrigin.NONE,0,0),empty);
        assertEquals(new State(1,1,0,sec(300),0,0,0),new State(1,1,0,sec(300),0,0,0,HasteOrigin.POTION,0,0));
        assertThrows(IllegalArgumentException.class, () -> new State(1,1,0,0,0,0,0,HasteOrigin.NONE,sec(64),0));
        assertThrows(IllegalArgumentException.class, () -> new State(1,1,0,0,0,0,0,HasteOrigin.NONE,sec(64)+1,1));
        assertThrows(IllegalArgumentException.class, () -> new State(1,1,0,1,0,0,0,HasteOrigin.POTION,2,1));
        assertThrows(IllegalArgumentException.class, () -> new State(1,1,0,0,0,0,0,HasteOrigin.NONE,0,2));
        assertThrows(IllegalArgumentException.class, () -> new State(1,1,0,0,0,0,0,HasteOrigin.NONE,-1,1));
        assertThrows(IllegalArgumentException.class, () -> applySlow(slow,99,6));
        assertThrows(IllegalArgumentException.class, () -> applySlow(slow,101,5));
        assertThrows(IllegalArgumentException.class, () -> applySlow(slow,MAX_OBSERVED_NANOS+1,6));
        var late = applySlow(slow,MAX_OBSERVED_NANOS,6);
        assertEquals(MAX_OBSERVED_NANOS+sec(64),late.slowUntilNanos());
        assertEquals(sec(20),intervalNanos(sec(10),Action.ATTACK,late));
        assertThrows(ArithmeticException.class, () -> new State(1,Long.MAX_VALUE,0,0,0,0,0,HasteOrigin.NONE,1,Long.MAX_VALUE).wireRevision());
    }
    @Test void retainedSourceProvesPotionSpellAsymmetryAndSlowFirstIntervalOrder() throws Exception {
        Path root = Path.of("../1-伺服器/src/com/lineage");
        String potion = Files.readString(root.resolve("data/item_etcitem/brave/StatusHaste.java"));
        String spell = Files.readString(root.resolve("server/model/skill/skillmode/HASTE.java"));
        String interval = Files.readString(root.resolve("server/model/ActionIntervalCalculator.java"));
        assertTrue(potion.contains("pc.killSkillEffectTimer(SLOW)"));
        assertTrue(potion.contains("pc.setSkillEffect(STATUS_HASTE"));
        assertTrue(potion.indexOf("pc.killSkillEffectTimer(SLOW)") < potion.indexOf("pc.setSkillEffect(STATUS_HASTE"));
        assertTrue(spell.contains("cha.removeSkillEffect(skillNum)"));
        assertTrue(spell.contains("cha.setMoveSpeed(0)"));
        assertTrue(interval.contains("interval = multiply(interval, 2.0)"));
        assertTrue(interval.contains("interval = multiply(interval, 0.755)"));
        assertTrue(interval.contains("if (state.slowed())"));
        assertTrue(interval.contains("else if (state.haste())"));
    }
}
