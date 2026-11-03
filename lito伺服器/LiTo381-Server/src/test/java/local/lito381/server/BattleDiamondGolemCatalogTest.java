package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleDiamondGolemCatalogTest {
    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(26).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x1A, 0x05}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM,
                request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(26).c(5).d(17).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedAbilityContract() {
        BattleSpellCatalog.DiamondGolemSpell spell =
                BattleSpellCatalog.diamondGolemSpell(
                        BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM);
        assertEquals(214, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_576, spell.effectGfx());
        assertEquals(25, spell.mpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(32, spell.nativeSkillId());
        assertEquals(-20, spell.armorClass());
        assertEquals(49_130L, spell.skillBookItemId());
        assertEquals("$5694", spell.skillBookNameId());
        assertEquals("$5948", spell.skillRowNameId());
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.persisted());
        assertEquals("illusion-diamond-golem", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.diamondGolemSpell(213));
    }

    @Test void appliesOnlyRetainedOriginalIntMpReduction() {
        BattleSpellCatalog.DiamondGolemSpell spell =
                BattleSpellCatalog.diamondGolemSpell(
                        BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM);
        assertEquals(25, spell.effectiveMpCost(0));
        assertEquals(24, spell.effectiveMpCost(1));
        assertEquals(23, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(-1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(3));
    }
}
