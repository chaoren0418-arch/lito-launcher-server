package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleIllusionOgreCatalogTest {
    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(3).bytes();
        assertArrayEquals(new byte[] {0x14, 0x19, 0x03}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ILLUSION_OGRE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(25).c(3).d(17).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedAbilityContract() {
        BattleSpellCatalog.IllusionOgreSpell spell =
                BattleSpellCatalog.illusionOgreSpell(
                        BattleSpellCatalog.ILLUSION_OGRE);
        assertEquals(204, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_578, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(8, spell.nativeSkillId());
        assertEquals(4, spell.meleeHit());
        assertEquals(4, spell.meleeDamage());
        assertEquals(49_120L, spell.skillBookItemId());
        assertEquals("$5684", spell.skillBookNameId());
        assertEquals("$5938", spell.skillRowNameId());
        assertEquals(true, spell.counterMagicBypass());
        assertEquals("illusion-ogre", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.illusionOgreSpell(203));
    }

    @Test void appliesOnlyRetainedOriginalIntMpReduction() {
        BattleSpellCatalog.IllusionOgreSpell spell =
                BattleSpellCatalog.illusionOgreSpell(
                        BattleSpellCatalog.ILLUSION_OGRE);
        assertEquals(15, spell.effectiveMpCost(0));
        assertEquals(14, spell.effectiveMpCost(1));
        assertEquals(13, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(-1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(3));
    }
}
