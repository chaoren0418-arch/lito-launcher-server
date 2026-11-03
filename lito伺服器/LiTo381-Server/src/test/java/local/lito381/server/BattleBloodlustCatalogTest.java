package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleBloodlustCatalogTest {
    @Test void pinsSelectedPrimaryRowAndRetainedSpeedContract() {
        BattleSpellCatalog.BloodlustSpell spell =
                BattleSpellCatalog.bloodlustSpell(BattleSpellCatalog.BLOODLUST);
        assertEquals(186, spell.skillId());
        assertEquals(23, spell.row());
        assertEquals(1, spell.column());
        assertEquals(24, spell.level());
        assertEquals(1, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_523, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(30, spell.hpCost());
        assertEquals(InventoryCatalog.AWAKENING_BONE_FRAGMENT,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(60, spell.durationSeconds());
        assertEquals(2, spell.nativeId());
        assertEquals(BattleTimedStatus.BRAVE_BLOODLUST, spell.braveMode());
        assertEquals(0.775D, spell.attackMultiplier());
        assertEquals(775, spell.attackIntervalMillis(1_000));
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.cancellationImmune());
        assertEquals("bloodlust", spell.key());
        assertNull(BattleSpellCatalog.bloodlustSpell(185));
    }
}
