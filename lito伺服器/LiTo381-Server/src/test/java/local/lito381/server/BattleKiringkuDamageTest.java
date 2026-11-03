package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleKiringkuDamageTest {
    @Test
    void replaysTheNativeTwoD5IntSpMrAndPointNineOrder() {
        BattleKiringkuDamage.Outcome minimum = BattleKiringkuDamage.roll(
                1, 1, 13, 0, 0, 27, 1, 3018);
        assertEquals(16, minimum.diceTotal());
        assertEquals(1, minimum.intelligenceTerm());
        assertEquals(1.09375D, minimum.intelligenceCoefficient());
        assertEquals(17, minimum.scaledDiceDamage());
        assertEquals(17, minimum.preResistanceDamage());
        assertEquals(13, minimum.resistanceFloor());
        assertEquals(0.87D, minimum.resistanceCoefficient(), 0.0000001D);
        assertEquals(13, minimum.damage());
        assertEquals(6983, minimum.effectGfx());

        BattleKiringkuDamage.Outcome maximum = BattleKiringkuDamage.roll(
                5, 5, 13, 0, 0, 27, 1, 3020);
        assertEquals(24, maximum.diceTotal());
        assertEquals(26, maximum.scaledDiceDamage());
        assertEquals(20, maximum.damage());
        assertEquals(7049, maximum.effectGfx());
    }

    @Test
    void preservesTheNativeMrBoundaryAndNegativeFloorBenefit() {
        assertEquals(7, BattleKiringkuDamage.roll(
                1, 1, 13, 0, 0, 100, 0, 3018).damage());
        assertEquals(7, BattleKiringkuDamage.roll(
                1, 1, 13, 0, 0, 101, 0, 3018).damage());
        BattleKiringkuDamage.Outcome originalHit = BattleKiringkuDamage.roll(
                1, 1, 13, 0, 0, 0, 4, 3018);
        assertEquals(-2, originalHit.resistanceFloor());
        assertEquals(1.02D, originalHit.resistanceCoefficient(), 0.0000001D);
        assertEquals(15, originalHit.damage());
    }

    @Test
    void exposesEveryOriginalClassAttackCorrection() {
        assertEquals(5, BattleKiringkuDamage.classAttackLevel(0, 52));
        assertEquals(8, BattleKiringkuDamage.classAttackLevel(1, 52));
        assertEquals(4, BattleKiringkuDamage.classAttackLevel(2, 52));
        assertEquals(2, BattleKiringkuDamage.classAttackLevel(3, 52));
        assertEquals(3, BattleKiringkuDamage.classAttackLevel(4, 52));
        assertEquals(4, BattleKiringkuDamage.classAttackLevel(5, 52));
        assertEquals(3, BattleKiringkuDamage.classAttackLevel(6, 52));
    }

    @Test
    void rejectsOutOfContractDiceAndGfx() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleKiringkuDamage.roll(0, 1, 13, 0, 0, 0, 0, 3018));
        assertThrows(IllegalArgumentException.class,
                () -> BattleKiringkuDamage.roll(1, 1, 13, 0, 0, 0, 0, 0x1_0000));
    }
}
