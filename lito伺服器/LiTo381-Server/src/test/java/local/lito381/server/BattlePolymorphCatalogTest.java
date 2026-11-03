package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattlePolymorphCatalogTest {
    @Test void selectedMonlistPinsAllNinetySixExactForms() {
        assertEquals(96, BattlePolymorphCatalog.standardMenuForms().size());
        assertEquals(104, BattlePolymorphCatalog.allForms().size());

        BattlePolymorphCatalog.Form ant = BattlePolymorphCatalog.require("re giant ant");
        assertEquals(1037, ant.gfx());
        assertEquals(1, ant.minimumLevel());
        assertEquals(0, ant.weaponEquipMask());
        assertEquals(136, ant.armorEquipMask());
        assertFalse(ant.skillUsable());
        assertEquals(7, ant.causeMask());
        assertTrue(ant.standardMenu());

        BattlePolymorphCatalog.Form death = BattlePolymorphCatalog.require("death 52");
        assertEquals(6137, death.gfx());
        assertEquals(52, death.minimumLevel());
        assertEquals(1791, death.weaponEquipMask());
        assertTrue(death.supportsWalkAction(BattleActionCodes.SWORD_WALK));
        assertFalse(death.supportsWalkAction(BattleActionCodes.CHAINSWORD_WALK));

        long noSkill = BattlePolymorphCatalog.standardMenuForms().stream()
                .filter(form -> !form.skillUsable()).count();
        assertEquals(3, noSkill);
    }

    @Test void bowNativeFormsResolveAProvenAttackFamilyWhenUnarmed() {
        BattlePolymorphCatalog.Form archer =
                BattlePolymorphCatalog.require("re skeleton archer");
        assertEquals(BattleActionCodes.BOW_WALK,
                archer.resolveBattleWalkAction(BattleActionCodes.WALK));
        assertTrue(archer.supportsWalkAction(BattleActionCodes.BOW_WALK));
        assertFalse(archer.supportsWalkAction(BattleActionCodes.SWORD_WALK));
    }

    @Test void selectedNoControlRingWeightsCoverExactlyOneMillion() {
        assertEquals("darkelf 75", BattlePolymorphCatalog.randomForm(1).selection());
        assertEquals("darkelf 75", BattlePolymorphCatalog.randomForm(47_600).selection());
        assertEquals("death 75", BattlePolymorphCatalog.randomForm(47_601).selection());
        assertEquals("lv black eagle",
                BattlePolymorphCatalog.randomForm(1_000_000).selection());
    }
}
