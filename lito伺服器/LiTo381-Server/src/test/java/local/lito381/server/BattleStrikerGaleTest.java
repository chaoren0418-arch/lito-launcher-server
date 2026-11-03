package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleStrikerGaleTest {
    @Test void onlyRetainedBowAndGauntletWeaponTypesQualify() {
        assertTrue(BattleStrikerGale.playerWeaponEligible(20));
        assertTrue(BattleStrikerGale.playerWeaponEligible(62));
        assertFalse(BattleStrikerGale.playerWeaponEligible(0));
        assertFalse(BattleStrikerGale.playerWeaponEligible(11));
    }

    @Test void npcRangeRequiresDistanceTwoAndBowAction() {
        assertFalse(BattleStrikerGale.npcAttackEligible(1, 66));
        assertFalse(BattleStrikerGale.npcAttackEligible(2, 0));
        assertTrue(BattleStrikerGale.npcAttackEligible(2, 66));
        assertThrows(IllegalArgumentException.class,
                () -> BattleStrikerGale.npcAttackEligible(-1, 66));
    }

    @Test void retainedDoubleCastTruncatesPositiveDamageAtOnePointTwo() {
        assertEquals(0, BattleStrikerGale.damageAfterStatus(0, true, true));
        assertEquals(10, BattleStrikerGale.damageAfterStatus(10, false, true));
        assertEquals(10, BattleStrikerGale.damageAfterStatus(10, true, false));
        assertEquals(12, BattleStrikerGale.damageAfterStatus(10, true, true));
        assertEquals(13, BattleStrikerGale.damageAfterStatus(11, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> BattleStrikerGale.damageAfterStatus(-1, true, true));
    }
}
