package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldDeathPenaltyCatalogTest {
    @Test void exactSelectedItemLawfulBandsAreCompleteAndBoundarySafe() {
        assertEquals(List.of(
                new ClassicWorldDeathPenaltyCatalog.Rule(32767, 20000, 0, 0),
                new ClassicWorldDeathPenaltyCatalog.Rule(19999, 10000, 0, 0),
                new ClassicWorldDeathPenaltyCatalog.Rule(9999, 0, 5, 1),
                new ClassicWorldDeathPenaltyCatalog.Rule(-1, -10000, 10, 2),
                new ClassicWorldDeathPenaltyCatalog.Rule(-10001, -20000, 25, 3),
                new ClassicWorldDeathPenaltyCatalog.Rule(-20001, -32768, 50, 3)),
                ClassicWorldDeathPenaltyCatalog.itemRules());
        assertEquals(5, ClassicWorldDeathPenaltyCatalog.itemRule(0).chancePercent());
        assertEquals(10, ClassicWorldDeathPenaltyCatalog.itemRule(-1).chancePercent());
        assertEquals(25, ClassicWorldDeathPenaltyCatalog.itemRule(-10001).chancePercent());
        assertEquals(50, ClassicWorldDeathPenaltyCatalog.itemRule(-32768).chancePercent());
    }

    @Test void exactSelectedSkillLawfulBandsAreCompleteAndBoundarySafe() {
        assertEquals(List.of(
                new ClassicWorldDeathPenaltyCatalog.Rule(32767, 20000, 0, 0),
                new ClassicWorldDeathPenaltyCatalog.Rule(19999, 10000, 0, 0),
                new ClassicWorldDeathPenaltyCatalog.Rule(9999, 0, 0, 0),
                new ClassicWorldDeathPenaltyCatalog.Rule(-1, -10000, 5, 1),
                new ClassicWorldDeathPenaltyCatalog.Rule(-10001, -20000, 25, 3),
                new ClassicWorldDeathPenaltyCatalog.Rule(-20001, -32768, 99, 3)),
                ClassicWorldDeathPenaltyCatalog.skillRules());
        assertEquals(0, ClassicWorldDeathPenaltyCatalog.skillRule(0).chancePercent());
        assertEquals(5, ClassicWorldDeathPenaltyCatalog.skillRule(-1).chancePercent());
        assertEquals(25, ClassicWorldDeathPenaltyCatalog.skillRule(-10001).chancePercent());
        assertEquals(99, ClassicWorldDeathPenaltyCatalog.skillRule(-32768).chancePercent());
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldDeathPenaltyCatalog.itemRule(32768));
    }

    @Test void exactSelectedControlTableHasFortyOneNeverAndSevenRandomVanishRows() {
        assertEquals(48, ClassicWorldDeathPenaltyCatalog.controls().size());
        assertEquals(41, ClassicWorldDeathPenaltyCatalog.controls().values().stream()
                .filter(value -> value == ClassicWorldDeathPenaltyCatalog.Control.NEVER_DROP)
                .count());
        assertEquals(7, ClassicWorldDeathPenaltyCatalog.controls().values().stream()
                .filter(value -> value
                        == ClassicWorldDeathPenaltyCatalog.Control.VANISH_IF_DROPPED)
                .count());
        assertEquals(ClassicWorldDeathPenaltyCatalog.Control.NEVER_DROP,
                ClassicWorldDeathPenaltyCatalog.control(40308));
        assertEquals(ClassicWorldDeathPenaltyCatalog.Control.VANISH_IF_DROPPED,
                ClassicWorldDeathPenaltyCatalog.control(300087));
        assertEquals(ClassicWorldDeathPenaltyCatalog.Control.VANISH_IF_DROPPED,
                ClassicWorldDeathPenaltyCatalog.control(300093));
        assertEquals(ClassicWorldDeathPenaltyCatalog.Control.NONE,
                ClassicWorldDeathPenaltyCatalog.control(40001));
    }

    @Test void exactDeathSafeMasksConsumptionAndSevenDayLifetimeAreRetained() {
        assertEquals(4, ClassicWorldDeathPenaltyCatalog.deathSafeItems().size());
        var experience = ClassicWorldDeathPenaltyCatalog.deathSafe(
                InventoryCatalog.DEATH_SAFE_EXPERIENCE);
        assertTrue(experience.consume());
        assertTrue(experience.protects(
                ClassicWorldDeathPenaltyCatalog.EXPERIENCE_CATEGORY));
        assertFalse(experience.protects(ClassicWorldDeathPenaltyCatalog.ITEM_CATEGORY));
        var all = ClassicWorldDeathPenaltyCatalog.deathSafe(
                InventoryCatalog.DEATH_SAFE_ALL);
        assertFalse(all.consume());
        assertEquals(168, all.timedHours());
        assertTrue(all.protects(ClassicWorldDeathPenaltyCatalog.EXPERIENCE_CATEGORY));
        assertTrue(all.protects(ClassicWorldDeathPenaltyCatalog.ITEM_CATEGORY));
        assertTrue(all.protects(ClassicWorldDeathPenaltyCatalog.SKILL_CATEGORY));
    }

    @Test void exactDeathSafeInventoryRowsAreClientRenderable() {
        assertDefinition(InventoryCatalog.DEATH_SAFE_EXPERIENCE,
                5576, "經驗保護符");
        assertDefinition(InventoryCatalog.DEATH_SAFE_ITEM,
                5563, "道具保護符");
        assertDefinition(InventoryCatalog.DEATH_SAFE_SKILL,
                5567, "技能保護符");
        assertDefinition(InventoryCatalog.DEATH_SAFE_ALL,
                5565, "全能保護符(7日)");
    }

    private static void assertDefinition(long itemId, int icon, String name) {
        InventoryCatalog.Definition definition = InventoryCatalog.find(itemId);
        assertEquals(itemId, definition.catalogId());
        assertEquals(0xFF, definition.useType());
        assertEquals(icon, definition.icon());
        assertEquals(22, definition.groundGfx());
        assertEquals(630, definition.unitWeight());
        assertEquals(name, definition.name());
        assertTrue(definition.stackable());
        assertTrue(definition.tradable());
    }
}
