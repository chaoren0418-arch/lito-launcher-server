package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldNpcDamageTest {
    @Test
    void retainedNpcDamageOrderKeepsCriticalUndeadAndBossTruncation() {
        assertEquals("7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8",
                ClassicWorldNpcDamage.L1_ATTACK_NPC_SOURCE_SHA256);
        // Retained L1AttackList STRD table gives STR 6 a -3 modifier.
        assertEquals(2, ClassicWorldNpcDamage.sourceDamage(
                2, 6, -3, 1, 99, false, false));
        assertEquals(5, ClassicWorldNpcDamage.sourceDamage(
                2, 6, -3, 1, 0, false, false));
        assertEquals(6, ClassicWorldNpcDamage.sourceDamage(
                2, 6, -3, 1, 0, true, false));
        assertEquals(10, ClassicWorldNpcDamage.sourceDamage(
                2, 6, -3, 1, 0, true, true));
        // Ogre Type=1 row applies retained leverage 20 before boss scaling.
        assertEquals(42, ClassicWorldNpcDamage.sourceDamage(
                28, 20, 4, 1, 99, false, false, 20));
        assertEquals(31, ClassicWorldNpcDamage.sourceDamage(
                29, 15, 4, 5, 99, false, false, 15));
    }

    @Test
    void retainedClockUsesSixTimesGameTimeAndSixToSeventeenDaylight() {
        long base = 1_735_689_600_000L;
        assertTrue(ClassicWorldNpcDamage.retainedNight(base));
        assertFalse(ClassicWorldNpcDamage.retainedNight(
                base + 3_600_000L)); // six game hours
        assertTrue(ClassicWorldNpcDamage.retainedNight(
                base + 3 * 3_600_000L)); // eighteen game hours
    }
}
