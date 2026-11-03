package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleAwakenFafurionCatalogTest {
    @Test void pinsSelectedDatabaseClientAndRetainedLifecycleContract() {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(
                BattleSpellCatalog.AWAKEN_FAFURION);
        assertEquals(190, spell.skillId());
        assertEquals(23, spell.row());
        assertEquals(5, spell.column());
        assertEquals(24, spell.level());
        assertEquals(5, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_976, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(1, spell.hpCost());
        assertEquals(InventoryCatalog.AWAKENING_BONE_FRAGMENT,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(0, spell.armorClassModifier());
        assertEquals(0, spell.maximumHpBonus());
        assertEquals(9_364, spell.dragonGfx());
        assertEquals(8, spell.mpDrainAmount());
        assertEquals(4, spell.mpDrainIntervalSeconds());
        assertEquals(30, spell.magicResistanceModifier());
        assertEquals(30, spell.allElementalResistance());
        assertEquals(0, spell.allAbilityScoreBonus());
        assertTrue(spell.counterMagicBypass());
        assertTrue(spell.cancellationImmune());
        assertEquals("awaken-fafurion", spell.key());
    }

    @Test void classicAwakeningWhitelistIsExactAndOtherAwakeningIsRejected() {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(190);
        for (int skillId : new int[] {190, 184, 189, 194, 192}) {
            assertTrue(spell.permitsSkill(skillId));
        }
        assertFalse(spell.permitsSkill(181));
        assertFalse(spell.permitsSkill(35));
        assertFalse(spell.permitsSkill(185));
    }
}
