package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleAwakenValakasCatalogTest {
    @Test void pinsSelectedDatabaseClientAndRetainedLifecycleContract() {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(
                BattleSpellCatalog.AWAKEN_VALAKAS);
        assertEquals(195, spell.skillId());
        assertEquals(24, spell.row());
        assertEquals(2, spell.column());
        assertEquals(25, spell.level());
        assertEquals(2, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_922, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(1, spell.hpCost());
        assertEquals(InventoryCatalog.AWAKENING_BONE_FRAGMENT,
                spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(0, spell.armorClassModifier());
        assertEquals(0, spell.maximumHpBonus());
        assertEquals(9_363, spell.dragonGfx());
        assertEquals(8, spell.mpDrainAmount());
        assertEquals(4, spell.mpDrainIntervalSeconds());
        assertEquals(0, spell.magicResistanceModifier());
        assertEquals(0, spell.allElementalResistance());
        assertEquals(5, spell.allAbilityScoreBonus());
        assertTrue(spell.counterMagicBypass());
        assertTrue(spell.cancellationImmune());
        assertEquals("awaken-valakas", spell.key());
    }

    @Test void classicAwakeningWhitelistIsExactAndOtherAwakeningIsRejected() {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(195);
        for (int skillId : new int[] {195, 184, 189, 194, 192}) {
            assertTrue(spell.permitsSkill(skillId));
        }
        assertFalse(spell.permitsSkill(181));
        assertFalse(spell.permitsSkill(35));
        assertFalse(spell.permitsSkill(185));
        assertFalse(spell.permitsSkill(190));
    }
}
