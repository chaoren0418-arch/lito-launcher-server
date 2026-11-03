package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class SpellShopCatalogTest {
    @Test
    void appliesOriginalClassLevelCircleRulesAndExcludesLearnedSkills() {
        CharacterSummary crown = character(0, 52);
        assertEquals(List.of(1, 2, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15),
                SpellShopCatalog.availableWireSkillIds(crown, Set.of(1, 4, 11)));
        assertEquals(8, SpellShopCatalog.availableWireSkillIds(character(1, 50), Set.of()).size());
        assertEquals(24, SpellShopCatalog.availableWireSkillIds(character(2, 24), Set.of()).size());
        assertEquals(24, SpellShopCatalog.availableWireSkillIds(character(3, 12), Set.of()).size());
        assertEquals(16, SpellShopCatalog.availableWireSkillIds(character(4, 24), Set.of()).size());
        assertEquals(List.of(), SpellShopCatalog.availableWireSkillIds(character(5, 99), Set.of()));
    }

    @Test
    void usesSixThousandTimesCircleSquaredAndRejectsDuplicateRows() {
        assertEquals(6_000, SpellShopCatalog.priceForWireSkillId(0));
        assertEquals(24_000, SpellShopCatalog.priceForWireSkillId(8));
        assertEquals(30_000, SpellShopCatalog.totalPrice(List.of(0, 8)));
        assertThrows(IllegalArgumentException.class,
                () -> SpellShopCatalog.totalPrice(List.of(1, 1)));
    }

    @Test
    void nativeHomeTownTeacherUsesRetainedDragonKnightAndIllusionistRows() {
        CharacterSummary dragonKnight = character(5, 45);
        assertEquals(15, SpellShopCatalog.availableNativeTeacherWireSkillIds(
                dragonKnight, Set.of()).size());
        assertEquals(List.of(180, 181, 182, 183, 184, 185, 186, 187,
                        188, 189, 190, 191, 192, 193, 194),
                SpellShopCatalog.availableNativeTeacherWireSkillIds(
                        dragonKnight, Set.of()));
        assertEquals(620, SpellShopCatalog.nativeTeacherBasePrice(dragonKnight));
        assertEquals(620L * 529L,
                SpellShopCatalog.totalPrice(List.of(180), 620));

        CharacterSummary illusionist = character(6, 40);
        assertEquals(20, SpellShopCatalog.availableNativeTeacherWireSkillIds(
                illusionist, Set.of()).size());
        assertEquals(470, SpellShopCatalog.nativeTeacherBasePrice(illusionist));
        assertEquals(470L * 784L,
                SpellShopCatalog.totalPrice(List.of(216), 470));
        assertEquals(19, SpellShopCatalog.availableNativeTeacherWireSkillIds(
                illusionist, Set.of(201)).size());
    }

    private static CharacterSummary character(int type, int level) {
        return new CharacterSummary("Tester", "", type, 0, level, 100, 10,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 32600, 32700, 4);
    }
}
