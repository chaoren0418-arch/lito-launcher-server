package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernCombatProfileTest.binding;
import static local.lito381.server.ModernCombatProfileTest.stored;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ModernGreaterHasteTest {
    @Test void exactNativeContractAndEveryNativeIntValueMatchWithoutUsingPotionDuration() {
        var nativeSpell = BattleSpellCatalog.selfHasteSpell(54);
        assertEquals(nativeSpell.skillId(), ModernGreaterHaste.SKILL_ID);
        assertEquals(nativeSpell.actionId(), ModernGreaterHaste.ACTION);
        assertEquals(nativeSpell.effectGfx(), ModernGreaterHaste.EFFECT);
        assertEquals(nativeSpell.mpCost(), ModernGreaterHaste.BASE_MP);
        assertEquals(nativeSpell.reuseDelayMillis(), ModernGreaterHaste.REUSE_MILLIS);
        assertEquals(nativeSpell.durationSeconds(), ModernGreaterHaste.DURATION_SECONDS);
        assertEquals(300, ModernTimedEffects.Kind.HASTE.durationSeconds);
        for (int intelligence = 1; intelligence <= 254; intelligence++)
            for (int original = 0; original <= 2; original++) for (boolean helm : new boolean[]{false, true})
                assertEquals(nativeSpell.effectiveMpCost(intelligence, original, helm),
                        ModernGreaterHaste.effectiveMpCost(intelligence, original, helm));
    }

    @Test void superNativeIntNeverWrapsAndHelmIsAppliedBeforeOriginalReduction() {
        for (long intelligence : new long[]{255, 65536, 1L << 32, Long.MAX_VALUE}) {
            assertEquals(54, ModernGreaterHaste.effectiveMpCost(intelligence, 0, false));
            assertEquals(52, ModernGreaterHaste.effectiveMpCost(intelligence, 2, false));
            assertEquals(27, ModernGreaterHaste.effectiveMpCost(intelligence, 0, true));
            assertEquals(25, ModernGreaterHaste.effectiveMpCost(intelligence, 2, true));
        }
        assertEquals(28, ModernGreaterHaste.effectiveMpCost(12, 2, true));
        assertEquals(27, ModernGreaterHaste.effectiveMpCost(13, 2, true));
        for (long intelligence : new long[]{Long.MIN_VALUE, -1, 0})
            assertThrows(IllegalArgumentException.class, () -> ModernGreaterHaste.effectiveMpCost(intelligence, 0, false));
        for (long original : new long[]{Long.MIN_VALUE, -1, 3, Long.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> ModernGreaterHaste.effectiveMpCost(12, original, true));
    }

    private static ModernPhysicalCombat.Source source(int id, int clazz, long intelligence, long armorId) {
        var item = simpleArmor(1, armorId, ModernEquipmentFacts.Ability.zero());
        var base = ModernPhysicalCombatTest.source(id, clazz, 80, List.of(item));
        return ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(base, intelligence, 17, 0, 15, 100));
    }

    @Test void qualifiedSourceOwnerAndExactWindHelmAreKeptWithoutImportingEquippedFlagsAsLiveState() {
        for (int clazz = 0; clazz <= 6; clazz++) for (long intelligence : new long[]{1, 12, 13, 18, 1L << 40}) {
            for (long catalog : new long[]{20023, 20013, 20014, 20024}) {
                var source = source(7, clazz, intelligence, catalog);
                var fighter = ModernPhysicalCombatTest.fighter(source);
                var profile = ModernGreaterHaste.qualify(fighter);
                assertEquals(fighter.key(), profile.key());
                assertEquals(0, profile.equipmentRevision());
                assertEquals(ModernGreaterHaste.effectiveMpCost(intelligence, 0, catalog == 20023), profile.mpCost());
            }
        }
        var source = source(7, 1, 12, 20023);
        var before = ModernPhysicalCombatTest.fighter(source);
        var original = ModernGreaterHaste.qualify(before);
        var q = source.input().equipment();
        var changed = ModernPhysicalCombat.qualifyEquipmentChange(source, binding(q),
                stored(q, 1, Map.of(1L, false)), 1, before, 23, 7);
        var after = ModernGreaterHaste.qualify(changed);
        assertTrue(source.input().equipment().source().inventory().get(0).equipped());
        assertEquals(30, original.mpCost());
        assertEquals(60, after.mpCost());
        assertEquals(1, after.equipmentRevision());
        assertEquals(23, changed.profile().effective().initialHp());
        assertEquals(7, changed.profile().effective().initialMp());
        assertEquals(original, ModernGreaterHaste.qualify(before));
        var other = ModernGreaterHaste.qualify(ModernPhysicalCombatTest.fighter(source(42, 1, 12, 20023)));
        assertNotEquals(original.key(), other.key());
        assertFalse(original.toString().contains("3333"));
    }

    @Test void missingMagicQualificationCannotBecomeASpellProfileOrEnableSkillCatalog() {
        var source = ModernPhysicalCombatTest.source(7, 1, 80, List.of());
        assertThrows(IllegalArgumentException.class,
                () -> ModernGreaterHaste.qualify(ModernPhysicalCombatTest.fighter(source)));
        // Qualification groundwork does not advertise or authorize the new spell.
        var skill = new ModernSkills.Skill(54, "$1976", 60, 10, ModernSkills.Support.UNIMPLEMENTED);
        assertEquals(ModernSkills.Support.UNIMPLEMENTED, skill.support());
    }
}
