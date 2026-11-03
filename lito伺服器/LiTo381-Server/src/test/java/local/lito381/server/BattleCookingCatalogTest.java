package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleCookingCatalogTest {
    @Test void pinsAllSixteenLevelOneStatusesAndEffects() {
        long[] normal = {41277, 41278, 41279, 41280, 41281, 41282, 41283, 41284};
        long[] special = {41285, 41286, 41287, 41288, 41289, 41290, 41291, 41292};
        for (int type = 0; type < 8; type++) {
            BattleCookingCatalog.Recipe ordinary = BattleCookingCatalog.require(normal[type]);
            BattleCookingCatalog.Recipe rare = BattleCookingCatalog.require(special[type]);
            assertEquals(3000 + type, ordinary.statusId());
            assertEquals(3008 + type, rare.statusId());
            assertEquals(type, ordinary.cookingType());
            assertEquals(type == 7, ordinary.dessert());
            assertEquals(0, ordinary.damageReduction());
            assertEquals(5, rare.damageReduction());
            assertFalse(ordinary.special());
            assertTrue(rare.special());
        }

        assertEquals(10, BattleCookingCatalog.require(41277).allElementResistance());
        assertEquals(30, BattleCookingCatalog.require(41278).maxHp());
        assertEquals(3, BattleCookingCatalog.require(41279).mpRegen());
        assertEquals(-1, BattleCookingCatalog.require(41280).armorClass());
        assertEquals(20, BattleCookingCatalog.require(41281).maxMp());
        assertEquals(3, BattleCookingCatalog.require(41282).hpRegen());
        assertEquals(5, BattleCookingCatalog.require(41283).magicResistance());
        assertEquals(1, BattleCookingCatalog.require(41284).experiencePercent());
        assertEquals(52, BattleCookingCatalog.size());
        assertEquals(900, BattleCookingCatalog.DURATION_SECONDS);
    }

    @Test void pinsAllLevelTwoAndThreePairsAndTheirCompleteEffects() {
        long[] levelTwoNormal = {49049, 49050, 49051, 49052,
                49053, 49054, 49055, 49056};
        long[] levelTwoSpecial = {49057, 49058, 49059, 49060,
                49061, 49062, 49063, 49064};
        long[] levelThreeNormal = {49244, 49245, 49246, 49247,
                49248, 49249, 49250, 49251};
        long[] levelThreeSpecial = {49252, 49253, 49254, 49255,
                49256, 49257, 49258, 49259};
        assertTierPairs(levelTwoNormal, levelTwoSpecial, 8, 3016, 3024);
        assertTierPairs(levelThreeNormal, levelThreeSpecial, 16, 3032, 3040);

        assertEffect(49049, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 1, 0, 0, 0);
        assertEffect(49050, 30, 30, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49051, 0, 0, -2, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49052, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 1, 0);
        assertEffect(49053, 0, 0, 0, 0, 0, 2, 2, 0,
                0, 0, 0, 0, 0);
        assertEffect(49054, 0, 0, 0, 0, 10, 0, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49055, 0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0);
        assertEffect(49056, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 2);

        assertEffect(49244, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 1, 0);
        assertEffect(49245, 50, 50, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49246, 0, 0, 0, 0, 0, 0, 0, 0,
                2, 2, 0, 0, 0);
        assertEffect(49247, 0, 0, -3, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49248, 0, 0, 0, 10, 15, 0, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49249, 0, 0, 0, 0, 0, 0, 2, 2,
                0, 0, 0, 0, 0);
        assertEffect(49250, 30, 0, 0, 0, 0, 2, 0, 0,
                0, 0, 0, 0, 0);
        assertEffect(49251, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 3);
    }

    @Test void pinsAllFourLevelFourFood3StatusesAndCompoundEffects() {
        BattleCookingCatalog.Recipe strength = BattleCookingCatalog.require(49825);
        assertEquals(3048, strength.statusId());
        assertEquals(157, strength.cookingType());
        assertEquals(1_800, strength.durationSeconds());
        assertEffect(49825, 0, 0, 0, 10, 10, 2, 2, 0,
                1, 2, 0, 0, 2);
        assertEffect(49826, 0, 0, 0, 10, 10, 2, 2, 0,
                0, 0, 1, 2, 2);
        assertEffect(49827, 0, 0, 0, 10, 10, 2, 3, 2,
                0, 0, 0, 0, 2);
        assertEffect(49828, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 4);
        for (int index = 0; index < 4; index++) {
            BattleCookingCatalog.Recipe recipe =
                    BattleCookingCatalog.require(49825 + index);
            assertEquals(3048 + index, recipe.statusId());
            assertEquals(157 + index, recipe.cookingType());
            assertEquals(2, recipe.damageReduction());
            assertEquals(index == 3, recipe.dessert());
            assertFalse(recipe.special());
        }
    }

    @Test void specialCookingReducesButNeverMakesDamageNegative() {
        assertEquals(10, BattleCookingCatalog.damageAfterReduction(15, 5));
        assertEquals(0, BattleCookingCatalog.damageAfterReduction(5, 5));
        assertEquals(0, BattleCookingCatalog.damageAfterReduction(2, 5));
    }

    @Test void wyvernSpellPowerJoinsTheQualifiedMagicIntelligenceTerm() {
        assertEquals(15, BattleCookingCatalog.magicIntelligenceAfterCooking(13, 2));
        assertEquals(13, BattleCookingCatalog.magicIntelligenceAfterCooking(13, 0));
    }

    @Test void cookingExperienceUsesTheSameIntegerAwardBoundary() {
        assertEquals(101, BattleCookingCatalog.experienceAfterBonus(100, 1));
        assertEquals(25, BattleCookingCatalog.experienceAfterBonus(25, 1));
        assertEquals(104, BattleCookingCatalog.experienceAfterBonus(100, 4));
    }

    @Test void mainCourseAndDessertUseIndependentMonotonicLeases() {
        BattleCookingStatus status = new BattleCookingStatus();
        long start = TimeUnit.SECONDS.toNanos(40);
        BattleCookingCatalog.Recipe pancake = BattleCookingCatalog.require(41279);
        BattleCookingCatalog.Recipe soup = BattleCookingCatalog.require(41292);
        BattleCookingCatalog.Recipe wyvern = BattleCookingCatalog.require(49257);

        assertEquals(null, status.activate(start, pancake, 900));
        assertEquals(null, status.activate(start, soup, 900));
        BattleCookingStatus.Snapshot both = status.active(start);
        assertEquals(pancake, both.mainCourse().recipe());
        assertEquals(soup, both.dessert().recipe());
        assertEquals(3, both.mpRegen());
        assertEquals(5, both.damageReduction());
        assertEquals(1, both.experiencePercent());

        assertEquals(pancake, status.activate(
                start + TimeUnit.SECONDS.toNanos(100), wyvern, 900));
        BattleCookingStatus.Snapshot replaced = status.active(
                start + TimeUnit.SECONDS.toNanos(100));
        assertEquals(wyvern, replaced.mainCourse().recipe());
        assertEquals(soup, replaced.dessert().recipe());
        assertEquals(2, replaced.mpRegen());
        assertEquals(2, replaced.spellPower());
        assertEquals(10, replaced.damageReduction());

        List<BattleCookingCatalog.Recipe> firstExpiry = status.expire(
                start + TimeUnit.SECONDS.toNanos(900));
        assertEquals(List.of(soup), firstExpiry);
        assertFalse(status.active(start + TimeUnit.SECONDS.toNanos(900))
                .dessert().active());
        assertTrue(status.active(start + TimeUnit.SECONDS.toNanos(900))
                .mainCourse().active());
        assertEquals(List.of(wyvern), status.expire(
                start + TimeUnit.SECONDS.toNanos(1_000)));
        assertTrue(status.active(start + TimeUnit.SECONDS.toNanos(1_000))
                .recipes().isEmpty());
    }

    @Test void cookingSnapshotSeparatesMeleeAndRangedAttackBonuses() {
        BattleCookingStatus status = new BattleCookingStatus();
        long start = TimeUnit.SECONDS.toNanos(5);
        status.activate(start, BattleCookingCatalog.require(49246), 900);
        BattleCookingStatus.Snapshot melee = status.active(start);
        assertEquals(2, melee.physicalHit(false));
        assertEquals(2, melee.physicalDamage(false));
        assertEquals(0, melee.physicalHit(true));
        assertEquals(0, melee.physicalDamage(true));

        status.activate(start, BattleCookingCatalog.require(49826), 1_800);
        BattleCookingStatus.Snapshot ranged = status.active(start);
        assertEquals(0, ranged.physicalHit(false));
        assertEquals(0, ranged.physicalDamage(false));
        assertEquals(1, ranged.physicalHit(true));
        assertEquals(2, ranged.physicalDamage(true));
        assertEquals(2, ranged.damageReduction());
        assertEquals(2, ranged.experiencePercent());
    }

    private static void assertTierPairs(long[] normal, long[] special,
                                        int firstType, int firstNormalStatus,
                                        int firstSpecialStatus) {
        for (int index = 0; index < 8; index++) {
            BattleCookingCatalog.Recipe ordinary =
                    BattleCookingCatalog.require(normal[index]);
            BattleCookingCatalog.Recipe rare =
                    BattleCookingCatalog.require(special[index]);
            assertEquals(firstType + index, ordinary.cookingType());
            assertEquals(firstNormalStatus + index, ordinary.statusId());
            assertEquals(firstSpecialStatus + index, rare.statusId());
            assertEquals(index == 7, ordinary.dessert());
            assertEquals(0, ordinary.damageReduction());
            assertEquals(5, rare.damageReduction());
            assertFalse(ordinary.special());
            assertTrue(rare.special());
        }
    }

    private static void assertEffect(
            long itemId, int maxHp, int maxMp, int armorClass,
            int allElementResistance, int magicResistance,
            int hpRegen, int mpRegen, int spellPower,
            int meleeHit, int meleeDamage, int rangedHit, int rangedDamage,
            int experiencePercent) {
        BattleCookingCatalog.Recipe recipe = BattleCookingCatalog.require(itemId);
        assertEquals(maxHp, recipe.maxHp(), "max HP item=" + itemId);
        assertEquals(maxMp, recipe.maxMp(), "max MP item=" + itemId);
        assertEquals(armorClass, recipe.armorClass(), "AC item=" + itemId);
        assertEquals(allElementResistance, recipe.allElementResistance(),
                "element item=" + itemId);
        assertEquals(magicResistance, recipe.magicResistance(), "MR item=" + itemId);
        assertEquals(hpRegen, recipe.hpRegen(), "HPR item=" + itemId);
        assertEquals(mpRegen, recipe.mpRegen(), "MPR item=" + itemId);
        assertEquals(spellPower, recipe.spellPower(), "SP item=" + itemId);
        assertEquals(meleeHit, recipe.meleeHit(), "melee hit item=" + itemId);
        assertEquals(meleeDamage, recipe.meleeDamage(), "melee damage item=" + itemId);
        assertEquals(rangedHit, recipe.rangedHit(), "ranged hit item=" + itemId);
        assertEquals(rangedDamage, recipe.rangedDamage(), "ranged damage item=" + itemId);
        assertEquals(experiencePercent, recipe.experiencePercent(),
                "EXP item=" + itemId);
    }
}
