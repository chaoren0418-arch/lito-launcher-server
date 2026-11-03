package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleConfusionCatalogTest {
    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x19, 0x01}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.CONFUSION, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(25).c(1).d(17).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedModeContract() {
        BattleSpellCatalog.ConfusionSpell spell =
                BattleSpellCatalog.confusionSpell(BattleSpellCatalog.CONFUSION);
        BattleSpellCatalog.AttackSpell attack = spell.attackSpell();
        assertEquals(202, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, attack.actionId());
        assertEquals(6_525, attack.effectGfx());
        assertEquals(15, attack.mpCost());
        assertEquals(10, spell.hpCost());
        assertEquals(3_000, attack.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(8, spell.durationSeconds());
        assertEquals(25, spell.successChancePercent());
        assertEquals(0, spell.bossModifier());
        assertFalse(spell.counterMagicBypass());
        assertEquals(1_339, spell.targetMessageCode());
        assertEquals(11, spell.dormantDatabaseDamageValue());
        assertEquals(49_118L, spell.skillBookItemId());
        assertEquals("$5682", spell.skillBookNameId());
        assertEquals("$5936", spell.skillRowNameId());
        assertEquals(0, attack.baseDamage());
        assertEquals(0, attack.diceCount());
        assertEquals(0, attack.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_NONE, attack.attribute());
        assertEquals("confusion", spell.key());
        assertEquals(attack, BattleSpellCatalog.attackSpell(202));
        assertNull(BattleSpellCatalog.confusionSpell(201));
    }

    @Test void fixedChanceIncludesTwentyFiveAndBossesAreImmune() {
        BattleSpellCatalog.ConfusionSpell spell =
                BattleSpellCatalog.confusionSpell(BattleSpellCatalog.CONFUSION);
        assertEquals(25, spell.chancePercent(false));
        assertTrue(spell.succeeds(false, 1));
        assertTrue(spell.succeeds(false, 25));
        assertFalse(spell.succeeds(false, 26));
        assertEquals(0, spell.chancePercent(true));
        assertFalse(spell.succeeds(true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.succeeds(false, 0));
        assertThrows(IllegalArgumentException.class,
                () -> spell.succeeds(false, 101));
    }
}
