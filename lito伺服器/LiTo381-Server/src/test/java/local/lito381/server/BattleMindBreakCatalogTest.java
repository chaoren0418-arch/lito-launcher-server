package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleMindBreakCatalogTest {
    @Test void metadataMatchesSelectedPrimaryRowAndClientClock() {
        BattleSpellCatalog.MindBreakSpell spell =
                BattleSpellCatalog.mindBreakSpell(BattleSpellCatalog.MIND_BREAK);
        assertEquals(207, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(6, spell.column());
        assertEquals(15, spell.hpCost());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(3, spell.targetTo());
        assertEquals(64, spell.type());
        assertEquals(64, spell.nativeSkillId());
        assertEquals(5, spell.minimumTargetMpDrain());
        assertEquals(50, spell.maximumTargetMpDrain());
        assertEquals(4, spell.damageMultiplier());
        assertEquals(49_123L, spell.skillBookItemId());
        assertEquals("$5687", spell.skillBookNameId());
        assertEquals("$5941", spell.skillRowNameId());
        assertFalse(spell.counterMagicBypass());
        assertEquals(20, spell.attackSpell().mpCost());
        assertEquals(3_000, spell.attackSpell().reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(5, spell.attackSpell().baseDamage());
        assertEquals(0, spell.attackSpell().diceCount());
        assertEquals(0, spell.attackSpell().dieSides());
        assertEquals(6_553, spell.attackSpell().effectGfx());
        assertNull(BattleSpellCatalog.mindBreakSpell(206));
    }

    @Test void strictTargetMpGateAndX4AreOverflowSafe() {
        BattleSpellCatalog.MindBreakSpell spell =
                BattleSpellCatalog.mindBreakSpell(BattleSpellCatalog.MIND_BREAK);
        assertTrue(spell.targetHasRequiredMp(6, 5));
        assertFalse(spell.targetHasRequiredMp(5, 5));
        assertFalse(spell.targetHasRequiredMp(50, 50));
        assertEquals(24, spell.damageAfterMultiplier(6));
        assertEquals(Integer.MAX_VALUE,
                spell.damageAfterMultiplier(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> spell.targetHasRequiredMp(100, 4));
        assertThrows(IllegalArgumentException.class,
                () -> spell.targetHasRequiredMp(100, 51));
        assertThrows(IllegalArgumentException.class,
                () -> spell.damageAfterMultiplier(-1));
    }
}
