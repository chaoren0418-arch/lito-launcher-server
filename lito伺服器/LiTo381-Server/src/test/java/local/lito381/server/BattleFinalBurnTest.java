package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleFinalBurnTest {
    @Test
    void usesPreConsumeMpThenArmorAndMagicResistance() {
        BattleFinalBurn.Result result = BattleFinalBurn.resolve(
                100, 10, 0, 27, 0);
        assertEquals(100, result.sourceMp());
        assertEquals(10, result.armorReduction());
        assertEquals(90, result.preMrDamage());
        assertEquals(78, result.damage());
        assertEquals(1, result.casterHpAfter());
        assertEquals(0, result.casterMpAfter());
    }

    @Test
    void zeroMpStillHasTheSpecialPostCastResourceState() {
        BattleFinalBurn.Result result = BattleFinalBurn.resolve(0, 0, 0, 0, 0);
        assertEquals(0, result.damage());
        assertEquals(1, result.casterHpAfter());
        assertEquals(0, result.casterMpAfter());
    }

    @Test
    void rejectsInvalidInputsAndArmorRolls() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleFinalBurn.resolve(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleFinalBurn.resolve(1, 12, 2, 0, 0));
    }
}
