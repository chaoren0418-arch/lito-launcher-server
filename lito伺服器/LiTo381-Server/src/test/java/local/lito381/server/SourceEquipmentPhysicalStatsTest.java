package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SourceEquipmentPhysicalStatsTest {
    @Test void keepsWeaponTemplateEnchantAndEquipmentLayersSeparate() {
        SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 2, 3, 7, 2)
                .withAbility(10, 20, 30, 40)
                .withNameId("$100");
        SourceEquipmentPhysicalStats stats = new SourceEquipmentPhysicalStats(
                weapon,
                1, 7, 4, 10,
                2, 8, 5, 11,
                3, 9, 6, 12);

        assertEquals(22, stats.totalHit(false));
        assertEquals(51, stats.totalHit(true));
        assertEquals(52, stats.totalDamageBonus(false));
        assertEquals(83, stats.totalDamageBonus(true));
        assertEquals(6, stats.totalHit(false, false));
        assertEquals(24, stats.totalDamageBonus(false, false));
        assertEquals(BattleActionCodes.SWORD_WALK, weapon.walkAction());
        assertEquals("$100", weapon.nameId());
        assertFalse(weapon.ranged());
    }

    @Test void mapsEveryNativeRangedWeaponFamilyWithoutGuessingFromTheEnvelope() {
        for (int type : new int[] {4, 10, 13}) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    1, type, 1, 1, 1, 0, 0, 0, 0);
            assertTrue(weapon.ranged());
        }
        assertFalse(new SourceWeaponPhysicalStats(
                1, 8, 1, 1, 1, 0, 0, 0, 0).ranged());
        assertTrue(new SourceWeaponPhysicalStats(
                1, 4, 1, 1, 1, 0, 0, 0, 0).bow());
        assertTrue(new SourceWeaponPhysicalStats(
                1, 13, 1, 1, 1, 0, 0, 0, 0).bow());
        assertFalse(new SourceWeaponPhysicalStats(
                1, 10, 1, 1, 1, 0, 0, 0, 0).bow());
        assertEquals(BattleActionCodes.BOW_WALK,
                new SourceWeaponPhysicalStats(
                        1, 13, 1, 1, 1, 0, 0, 0, 0).walkAction());
        assertEquals(BattleActionCodes.THROWING_KNIFE_WALK,
                new SourceWeaponPhysicalStats(
                        1, 10, 1, 1, 1, 0, 0, 0, 0).walkAction());
        assertEquals(BattleActionCodes.CLAW_WALK,
                new SourceWeaponPhysicalStats(
                        270, 17, 16, 16, 1, 1, 2, 0, 0).walkAction());
    }

    @Test void qualifiesArrowAndStingRowsByNativeInventoryObjectIdentity() {
        SourceProjectilePhysicalStats arrow = new SourceProjectilePhysicalStats(
                0x1234_5678L, 40743, -2, 5, 7, 66, true, 20);
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 4, 0, 0, 15, 0, 0, 0, 0).withProjectile(arrow);
        assertEquals(0x1234_5678L, bow.projectile().objectId());
        assertEquals(5, bow.projectile().damageMaximum(false));
        assertEquals(7, bow.projectile().damageMaximum(true));
        assertEquals(66, bow.projectile().effectGfx());
        assertThrows(IllegalArgumentException.class,
                () -> new SourceWeaponPhysicalStats(
                        177, 10, 0, 0, 10, 0, 0, 0, 0).withProjectile(arrow));
    }

    @Test void retainsWeaponTraitsDurabilityAndEquipmentPierce() {
        SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 0, 0, 7, 2)
                .withCombatTraits(new SourceWeaponCombatTraits(14, 0, 1, 3, 9));
        SourceEquipmentPhysicalStats stats = new SourceEquipmentPhysicalStats(
                weapon,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 1, 2);
        assertEquals(5, weapon.effectiveEnchant(false));
        assertEquals(7, weapon.effectiveEnchant(true));
        assertEquals(14, weapon.attackMaterial());
        assertEquals(0, weapon.attackBlessing());
        assertEquals(3, stats.totalPierce());
    }

    @Test void keepsRetainedPvpIgnoreAndDirectionalDefenseSemanticsSeparate() {
        SourceEquipmentPhysicalStats stats = new SourceEquipmentPhysicalStats(
                SourceWeaponPhysicalStats.none(),
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                4, 3, 5, 9, -12, 4, 6, 8,
                new SourceEquipmentElementalStats(
                        1, 2, 3, 4, 10, 20, 30, 40));
        assertEquals(7, stats.totalIgnoreReduction());
        assertEquals(5, stats.directionalReduction(false));
        assertEquals(9, stats.directionalReduction(true));
        assertEquals(-16, stats.armorClassModifier());
        assertEquals(14, stats.totalEvasionRate());
        assertEquals(11, stats.elemental().resistance(1));
        assertEquals(22, stats.elemental().resistance(2));
        assertEquals(33, stats.elemental().resistance(4));
        assertEquals(44, stats.elemental().resistance(8));
    }

    @Test void retainsElementalTotalsBeyondTheNativeSignedByteRange() {
        SourceEquipmentElementalStats elemental =
                new SourceEquipmentElementalStats(
                        200, 300, 400, 500,
                        20, 30, 40, 50);
        assertEquals(220, elemental.resistance(BattleSpellCatalog.ATTRIBUTE_EARTH));
        assertEquals(330, elemental.resistance(BattleSpellCatalog.ATTRIBUTE_FIRE));
        assertEquals(440, elemental.resistance(BattleSpellCatalog.ATTRIBUTE_WATER));
        assertEquals(550, elemental.resistance(BattleSpellCatalog.ATTRIBUTE_WIND));
    }

    @Test void rejectsPartialOrImpossibleWeaponSnapshots() {
        assertThrows(IllegalArgumentException.class,
                () -> new SourceWeaponPhysicalStats(
                        0, 1, 0, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceWeaponPhysicalStats(
                        24, 0, 6, 8, 1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceEquipmentPhysicalStats(
                        null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test void retainsExactShieldAndGuarderEligibilityWithoutConflatingOtherArmor() {
        SourceEquipmentPhysicalStats shield = new SourceEquipmentPhysicalStats(
                SourceWeaponPhysicalStats.none(), true, false);
        SourceEquipmentPhysicalStats guarder = new SourceEquipmentPhysicalStats(
                SourceWeaponPhysicalStats.none(), false, true);
        SourceEquipmentPhysicalStats neither = SourceEquipmentPhysicalStats.none();

        assertTrue(shield.shieldEquipped());
        assertFalse(shield.guarderEquipped());
        assertTrue(shield.supportsSolidCarriage());
        assertTrue(guarder.supportsSolidCarriage());
        assertEquals(7, shield.defensiveEquipment().armorType());
        assertEquals(13, guarder.defensiveEquipment().armorType());
        assertEquals(25, shield.defensiveEquipment().useType());
        assertFalse(neither.supportsSolidCarriage());
    }
}
