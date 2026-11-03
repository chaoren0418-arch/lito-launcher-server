package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterPoisonAttackTest {
    @Test
    void retainedFifteenPercentProcMapsOnlyImplementedPoisonAtkValues() {
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                ClassicWorldMonsterPoisonAttack.resolve(1, 1));
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                ClassicWorldMonsterPoisonAttack.resolve(1, 15));
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.NONE,
                ClassicWorldMonsterPoisonAttack.resolve(1, 16));
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.SILENCE,
                ClassicWorldMonsterPoisonAttack.resolve(2, 15));
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS,
                ClassicWorldMonsterPoisonAttack.resolve(4, 15));
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.NONE,
                ClassicWorldMonsterPoisonAttack.resolve(0, 1));
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.NONE,
                ClassicWorldMonsterPoisonAttack.resolve(3, 1));
    }

    @Test
    void retainedTimingAndDamageConstantsRemainExact() {
        assertEquals(15, ClassicWorldMonsterPoisonAttack.PROC_PERCENT);
        assertEquals(30, ClassicWorldMonsterPoisonAttack.DAMAGE_DURATION_SECONDS);
        assertEquals(3_000, ClassicWorldMonsterPoisonAttack.DAMAGE_TICK_MILLIS);
        assertEquals(5, ClassicWorldMonsterPoisonAttack.DAMAGE_PER_TICK);
        assertEquals(20_000,
                ClassicWorldMonsterPoisonAttack.PARALYSIS_WARNING_MILLIS);
        assertEquals(45_000,
                ClassicWorldMonsterPoisonAttack.PARALYSIS_DURATION_MILLIS);
    }

    @Test
    void fixtureRollMustRemainOneBased() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldMonsterPoisonAttack.resolve(1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldMonsterPoisonAttack.resolve(1, 101));
    }
}
