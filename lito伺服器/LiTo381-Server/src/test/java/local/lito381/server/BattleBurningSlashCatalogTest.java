package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleBurningSlashCatalogTest {
    @Test void pinsSelectedResourceCastClockAndOneShotImpactContract() {
        BattleSpellCatalog.BurningSlashSpell spell =
                BattleSpellCatalog.burningSlashSpell(
                        BattleSpellCatalog.BURNING_SLASH);
        assertEquals(182, spell.skillId());
        assertEquals(22, spell.row());
        assertEquals(5, spell.column());
        assertEquals(23, spell.level());
        assertEquals(5, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_590, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(10, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(3_600, spell.clientCastClockMillis());
        assertEquals(60, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(32, spell.nativeId());
        assertEquals(10, spell.nextMeleeDamage());
        assertEquals(6_591, spell.impactGfx());
        assertTrue(spell.counterMagicBypass());
        assertEquals("burning-slash", spell.key());
        assertNull(BattleSpellCatalog.burningSlashSpell(181));
    }
}
