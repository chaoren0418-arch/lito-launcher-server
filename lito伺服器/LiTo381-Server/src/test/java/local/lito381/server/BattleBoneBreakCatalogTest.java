package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleBoneBreakCatalogTest {
    @Test void selectedRowsPinSelectorDamageStatusAndDormantControls() {
        BattleSpellCatalog.BoneBreakSpell spell =
                BattleSpellCatalog.boneBreakSpell(BattleSpellCatalog.BONE_BREAK);
        BattleSpellCatalog.AttackSpell attack = spell.attackSpell();

        assertEquals(208, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(7, spell.column());
        assertEquals(18, attack.actionId());
        assertEquals(7_020, attack.effectGfx());
        assertEquals(20, attack.mpCost());
        assertEquals(2_000, attack.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(10, attack.baseDamage());
        assertEquals(0, attack.diceCount());
        assertEquals(0, attack.dieSides());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(3, spell.targetTo());
        assertEquals(64, spell.type());
        assertEquals(128, spell.nativeSkillId());
        assertEquals(45, spell.lawfulDelta());
        assertEquals(2, spell.durationSeconds());
        assertEquals(86_123, spell.effectNpcId());
        assertEquals(6_552, spell.effectNpcGfx());
        assertEquals(49_124L, spell.skillBookItemId());
        assertEquals("$5688", spell.skillBookNameId());
        assertEquals("$5942", spell.skillRowNameId());
        assertEquals(55, spell.dormantProbabilityHigh());
        assertEquals(45, spell.dormantProbabilityEqual());
        assertEquals(35, spell.dormantProbabilityLow());
        assertEquals(0.35D, spell.dormantIntelligenceMultiplier());
        assertEquals(5, spell.dormantMagicLevelMultiplier());
        assertEquals(0.05D, spell.dormantMagicResistanceMultiplier());
        assertEquals(10, spell.primaryBossModifier());
        assertEquals(0, spell.secondaryBossModifier());
        assertFalse(spell.counterMagicBypass());
        assertEquals("bone-break", spell.key());
        assertEquals(attack,
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.BONE_BREAK));
        assertNull(BattleSpellCatalog.boneBreakSpell(207));
    }

    @Test void retainedPrivateRollHasExactTwentyPercentThreshold() {
        BattleSpellCatalog.BoneBreakSpell spell =
                BattleSpellCatalog.boneBreakSpell(BattleSpellCatalog.BONE_BREAK);
        assertEquals(200, spell.successPermille());
        assertFalse(spell.succeeds(1));
        assertFalse(spell.succeeds(800));
        assertTrue(spell.succeeds(801));
        assertTrue(spell.succeeds(1_000));
        assertThrows(IllegalArgumentException.class, () -> spell.succeeds(0));
        assertThrows(IllegalArgumentException.class, () -> spell.succeeds(1_001));
    }
}
