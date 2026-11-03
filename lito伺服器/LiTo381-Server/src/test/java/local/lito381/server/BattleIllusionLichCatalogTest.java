package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleIllusionLichCatalogTest {
    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(26).c(0).bytes();
        assertArrayEquals(new byte[] {0x14, 0x1A, 0x00}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ILLUSION_LICH, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(26).c(0).d(17).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedAbilityContract() {
        BattleSpellCatalog.IllusionLichSpell spell =
                BattleSpellCatalog.illusionLichSpell(
                        BattleSpellCatalog.ILLUSION_LICH);
        assertEquals(209, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_577, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(64, spell.clientCastClockMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(1, spell.nativeSkillId());
        assertEquals(2, spell.spellPower());
        assertEquals(49_125L, spell.skillBookItemId());
        assertEquals("$5689", spell.skillBookNameId());
        assertEquals("$5943", spell.skillRowNameId());
        assertEquals(true, spell.counterMagicBypass());
        assertEquals("illusion-lich", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.illusionLichSpell(208));
    }

    @Test void appliesOnlyRetainedOriginalIntMpReduction() {
        BattleSpellCatalog.IllusionLichSpell spell =
                BattleSpellCatalog.illusionLichSpell(
                        BattleSpellCatalog.ILLUSION_LICH);
        assertEquals(15, spell.effectiveMpCost(0));
        assertEquals(14, spell.effectiveMpCost(1));
        assertEquals(13, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(-1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(3));
    }
}
