package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleFoeSlayerCatalogTest {
    @Test void pinsSelectedPrimaryRowAndRetainedThreeStrikeContract() {
        LocalResources.assumeNativeCatalogPins();
        BattleSpellCatalog.FoeSlayerSpell spell =
                BattleSpellCatalog.foeSlayerSpell(
                        BattleSpellCatalog.FOE_SLAYER);
        assertEquals(187, spell.skillId());
        assertEquals(23, spell.row());
        assertEquals(2, spell.column());
        assertEquals(24, spell.level());
        assertEquals(2, spell.number());
        assertEquals(18, spell.actionId());
        assertEquals(0, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(20, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(2, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(64, spell.type());
        assertEquals(4, spell.nativeId());
        assertEquals(3, spell.hitCount());
        assertEquals(20, spell.exposureDamageStep());
        assertEquals(7_020, spell.attackerEffectGfx());
        assertEquals(12_119, spell.targetEffectGfx());
        assertEquals(0, spell.damageForExposureLevel(0));
        assertEquals(20, spell.damageForExposureLevel(1));
        assertEquals(40, spell.damageForExposureLevel(2));
        assertEquals(60, spell.damageForExposureLevel(3));
        assertThrows(IllegalArgumentException.class,
                () -> spell.damageForExposureLevel(4));
        assertTrue(spell.counterMagicBypass());
        assertEquals("foe-slayer", spell.key());
        assertNull(BattleSpellCatalog.foeSlayerSpell(188));
    }
}
