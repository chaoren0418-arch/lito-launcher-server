package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleArmorBreakTest {
    @Test
    void restoresMissingLiToTargetWithoutGuessingAmongMultipleEnemies() {
        List<BattleArmorBreak.Candidate> candidates = List.of(
                new BattleArmorBreak.Candidate(15, 101, 120),
                new BattleArmorBreak.Candidate(16, 100, 121));

        assertEquals(16, BattleArmorBreak.resolveTarget(
                16, 100, 120, candidates));
        assertEquals(0, BattleArmorBreak.resolveTarget(
                0, 100, 120, candidates));
        assertEquals(15, BattleArmorBreak.resolveTarget(
                0, 100, 120, List.of(candidates.get(0))));
        assertEquals(0, BattleArmorBreak.resolveTarget(
                15, 90, 90, candidates));
    }

    @Test
    void reproducesPureLevelTierProbabilityAndBossImmunity() {
        assertEquals(50, BattleArmorBreak.probability(
                53, 52, 50, 40, 30, false));
        assertEquals(40, BattleArmorBreak.probability(
                52, 52, 50, 40, 30, false));
        assertEquals(30, BattleArmorBreak.probability(
                51, 52, 50, 40, 30, false));
        assertEquals(0, BattleArmorBreak.probability(
                99, 1, 50, 40, 30, true));
        assertTrue(BattleArmorBreak.succeeds(40, 40));
        assertFalse(BattleArmorBreak.succeeds(40, 41));
        assertFalse(BattleArmorBreak.succeeds(0, 1));
    }

    @Test
    void appliesExactOnePointFiveEightMultiplierExceptWeaponTypesTwentyAndSixtyTwo() {
        assertEquals(15, BattleArmorBreak.damageAfterBreak(10, true, 11));
        assertEquals(1, BattleArmorBreak.damageAfterBreak(1, true, 0));
        assertEquals(10, BattleArmorBreak.damageAfterBreak(10, false, 11));
        assertEquals(10, BattleArmorBreak.damageAfterBreak(10, true, 20));
        assertEquals(10, BattleArmorBreak.damageAfterBreak(10, true, 62));
        assertTrue(BattleArmorBreak.supportsWeaponType(11));
        assertFalse(BattleArmorBreak.supportsWeaponType(20));
        assertFalse(BattleArmorBreak.supportsWeaponType(62));
    }

    @Test
    void catalogPinsTheExactSelectedSkill112Row() {
        BattleSpellCatalog.ArmorBreakSpell spell =
                BattleSpellCatalog.armorBreakSpell(BattleSpellCatalog.ARMOR_BREAK);
        assertEquals(112, spell.skillId());
        assertEquals(13, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3400, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(30, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(1, spell.maximumRange());
        assertEquals(8, spell.durationSeconds());
        assertEquals(0, spell.bossModifier());
        assertEquals(InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(280, spell.failureMessage());
        assertEquals("armor-break", spell.key());
        assertEquals(40, spell.effectiveMpCost(0));
        assertEquals(38, spell.effectiveMpCost(2));
    }
}
