package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleAwakenAntharasCatalogTest {
    @Test void pinsSelectedDatabaseClientAndRetainedLifecycleContract() {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(
                BattleSpellCatalog.AWAKEN_ANTHARAS);
        assertEquals(185, spell.skillId());
        assertEquals(23, spell.row());
        assertEquals(0, spell.column());
        assertEquals(24, spell.level());
        assertEquals(0, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_975, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(1, spell.hpCost());
        assertEquals(InventoryCatalog.AWAKENING_BONE_FRAGMENT,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(-12, spell.armorClassModifier());
        assertEquals(127, spell.maximumHpBonus());
        assertEquals(9_362, spell.dragonGfx());
        assertEquals(8, spell.mpDrainAmount());
        assertEquals(4, spell.mpDrainIntervalSeconds());
        assertEquals(0, spell.magicResistanceModifier());
        assertEquals(0, spell.allElementalResistance());
        assertEquals(0, spell.allAbilityScoreBonus());
        assertTrue(spell.counterMagicBypass());
        assertTrue(spell.cancellationImmune());
        assertEquals("awaken-antharas", spell.key());
        assertNull(BattleSpellCatalog.awakenSpell(184));
    }

    @Test void classicAwakeningWhitelistIsExact() {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(185);
        for (int skillId : new int[] {185, 184, 189, 194, 192}) {
            assertTrue(spell.permitsSkill(skillId));
        }
        assertFalse(spell.permitsSkill(181));
        assertFalse(spell.permitsSkill(35));
        assertFalse(spell.permitsSkill(190));
    }
}
