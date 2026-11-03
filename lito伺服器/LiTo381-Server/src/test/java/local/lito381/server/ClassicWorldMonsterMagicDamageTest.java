package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterMagicDamageTest {
    @Test
    void iceConeRetainsNpcIntElementLeverageArmorAndMrOrder() {
        ClassicWorldMonsterMagicSkillCatalog.Definition rabbit =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45049);
        int source = ClassicWorldMonsterMagicDamage.rollSourceDamage(
                rabbit, sides -> 1);
        assertEquals(37, source);
        int leveraged = ClassicWorldMonsterMagicDamage.afterElementAndLeverage(
                source, 16, 0, rabbit.effectiveLeverage());
        assertEquals(25, leveraged);
        assertEquals(20, ClassicWorldMonsterMagicDamage.afterArmorAndResistance(
                leveraged, 5, 0, 16));

        ClassicWorldMonsterMagicSkillCatalog.Definition mage =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45224);
        assertEquals(40,
                ClassicWorldMonsterMagicDamage.afterElementAndLeverage(
                        source, 10, 0, mage.effectiveLeverage()));
        assertEquals(28, ClassicWorldMonsterMagicDamage.afterArmorAndResistance(
                40, 3, 50, 10));
    }

    @Test
    void tornadoRetainsPerTargetDiceAndNpcWeakAttributeResistance() {
        ClassicWorldMonsterMagicSkillCatalog.Definition tornado =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45265);
        int source = ClassicWorldMonsterMagicDamage.rollSourceDamage(
                tornado, sides -> 1);
        assertEquals(21, source);
        assertEquals(0, ClassicWorldMonsterMagicDamage
                .companionElementalResistance(2, tornado.attribute()));
        assertEquals(-50, ClassicWorldMonsterMagicDamage
                .companionElementalResistance(8, tornado.attribute()));
        assertEquals(50, ClassicWorldMonsterMagicDamage
                .companionElementalResistance(-8, tornado.attribute()));
        assertEquals(42, ClassicWorldMonsterMagicDamage.afterElementAndLeverage(
                source, 16, 0, tornado.effectiveLeverage()));
        assertEquals(58, ClassicWorldMonsterMagicDamage.afterElementAndLeverage(
                source, 16, -50, tornado.effectiveLeverage()));
    }

    @Test
    void playerAttackMagicCombinesNpcWeakAttributeAndLiveStatusModifier() {
        assertEquals(-50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(8, BattleSpellCatalog.ATTRIBUTE_WIND, 0));
        assertEquals(50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(-8, BattleSpellCatalog.ATTRIBUTE_WIND, 0));
        assertEquals(-100, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(8, BattleSpellCatalog.ATTRIBUTE_WIND, -50));
        assertEquals(-50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(4, BattleSpellCatalog.ATTRIBUTE_WIND, -50));
    }

    @Test
    void fixtureRollAndDefenseInputsRejectImpossibleValues() {
        ClassicWorldMonsterMagicSkillCatalog.Definition ice =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45224);
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldMonsterMagicDamage.rollSourceDamage(
                        ice, sides -> sides + 1));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldMonsterMagicDamage.afterArmorAndResistance(
                        -1, 0, 0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldMonsterMagicDamage
                        .companionElementalResistance(3, 4));
    }
}
