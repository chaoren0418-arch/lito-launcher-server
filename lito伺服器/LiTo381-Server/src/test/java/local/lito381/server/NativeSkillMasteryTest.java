package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NativeSkillMasteryTest {
    @Test void importRequiresAnExistingPositiveClassicTemplateLevel() {
        assertTrue(NativeSkillMastery.importable(1, 1));
        assertTrue(NativeSkillMastery.importable(91, 12));
        assertTrue(NativeSkillMastery.importable(220, 28));

        assertFalse(NativeSkillMastery.importable(0, 1));
        assertFalse(NativeSkillMastery.importable(221, 1));
        assertFalse(NativeSkillMastery.importable(92, null));
        assertFalse(NativeSkillMastery.importable(92, 0));
        assertFalse(NativeSkillMastery.importable(92, 29));
    }

    @Test void retainedKnightGapCannotBecomeMasteryFromHistoricalGfxRows() {
        for (int skillId = 92; skillId <= 96; skillId++) {
            assertFalse(NativeSkillMastery.importable(skillId, 0));
        }
    }

    @Test void retainedGeneralGapCannotBecomeMasteryFromHistoricalMetadata() {
        for (int skillId = 81; skillId <= 86; skillId++) {
            assertFalse(NativeSkillMastery.importable(skillId, 0));
            assertFalse(NativeSkillMastery.importable(skillId, 15));
        }
        assertTrue(NativeSkillMastery.importable(80, 10));
        assertTrue(NativeSkillMastery.importable(87, 10));
    }

    @Test void retainedWizard24NoneRowCannotBecomeMasteryFromGfxResidue() {
        assertFalse(NativeSkillMastery.importable(24, 0));
        assertFalse(NativeSkillMastery.importable(24, 3));
        assertFalse(NativeSkillMastery.importable(24, 15));
        assertTrue(NativeSkillMastery.importable(23, 3));
        assertTrue(NativeSkillMastery.importable(25, 4));
    }

    @Test void everyCataloguedReservedSlotIsRejectedEvenWithMalformedLevel() {
        for (int skillId = 1; skillId <= NativeSkillMastery.MAX_SKILL_ID;
                skillId++) {
            if (BattleSpellCatalog.reservedSkillSlot(skillId) != null) {
                assertFalse(NativeSkillMastery.importable(skillId, 15));
            }
        }
    }

    @Test void positiveLevelNoneRows119And120CannotLeakIntoMastery() {
        for (int skillId = 119; skillId <= 128; skillId++) {
            assertFalse(NativeSkillMastery.importable(skillId,
                    skillId <= 120 ? 15 : 0));
            assertFalse(NativeSkillMastery.importable(skillId, 15));
        }
        assertTrue(NativeSkillMastery.importable(129, 17));
    }
}
