package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattlePhysicalOffenseTest {
    @Test void preservesOriginal381MaterialAndBlessingFamilies() {
        SourceNpcPhysicalStats undead = npc(1, 0, 0);
        SourceNpcPhysicalStats armor = npc(2, 0, 0);
        SourceNpcPhysicalStats demon = npc(3, 0, 0);
        SourceNpcPhysicalStats ghost = npc(5, 0, 0);

        SourceWeaponPhysicalStats normalSilver = weapon(14, 1, 0, 0, 0);
        assertEquals(20, BattlePhysicalOffense.materialMaximum(normalSilver, undead));
        assertEquals(0, BattlePhysicalOffense.blessingMaximum(normalSilver, undead));

        SourceWeaponPhysicalStats blessedIron = weapon(11, 0, 0, 0, 0);
        assertEquals(0, BattlePhysicalOffense.materialMaximum(blessedIron, undead));
        assertEquals(4, BattlePhysicalOffense.blessingMaximum(blessedIron, undead));
        assertEquals(4, BattlePhysicalOffense.blessingMaximum(blessedIron, armor));
        assertEquals(4, BattlePhysicalOffense.blessingMaximum(blessedIron, demon));
        assertEquals(0, BattlePhysicalOffense.blessingMaximum(blessedIron, ghost));

        SourceWeaponPhysicalStats blessedMithril = weapon(17, 0, 0, 0, 0);
        BattlePhysicalOffense.NpcOutcome outcome = BattlePhysicalOffense.againstNpc(
                blessedMithril, armor, 3, 4);
        assertEquals(7, outcome.totalBonus());
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalOffense.againstNpc(
                        blessedMithril, armor, 4, 1));
    }

    @Test void usesArrowOrStingMaterialAndBlessingForRangedAttacks() {
        SourceProjectilePhysicalStats arrow = new SourceProjectilePhysicalStats(
                0x1234L, 40743, -2, 5, 7, 22, 0, 66, true, 20);
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 4, 0, 0, 15, 0, 0, 0, 0).withProjectile(arrow);
        SourceNpcPhysicalStats armor = npc(2, 0, 0);
        assertEquals(22, bow.attackMaterial());
        assertEquals(0, bow.attackBlessing());
        assertEquals(3, BattlePhysicalOffense.materialMaximum(bow, armor));
        assertEquals(4, BattlePhysicalOffense.blessingMaximum(bow, armor));
    }

    @Test void appliesWeakResistanceAndMagicResistanceWithRetainedTruncation() {
        SourceWeaponPhysicalStats earth = weapon(11, 1, 1, 10, 30);
        assertEquals(45, BattlePhysicalOffense.attributeDamage(earth, npc(0, 1, 0)));
        assertEquals(15, BattlePhysicalOffense.attributeDamage(earth, npc(0, -1, 0)));
        assertEquals(30, BattlePhysicalOffense.attributeDamage(earth, npc(0, 0, 0)));

        SourceWeaponPhysicalStats spirit = weapon(11, 1, 16, 10, 30);
        assertEquals(0, BattlePhysicalOffense.attributeDamage(spirit, npc(0, 0, 100)));
        assertEquals(-15, BattlePhysicalOffense.attributeDamage(spirit, npc(0, 0, 150)));
    }

    @Test void appliesTheSameRetainedCoefficientToPlayerElementalResistance() {
        SourceWeaponPhysicalStats fire = weapon(11, 1, 2, 5, 30);
        BattlePhysicalOffense.PlayerOutcome neutral =
                BattlePhysicalOffense.againstPlayer(fire, 0);
        BattlePhysicalOffense.PlayerOutcome resistant =
                BattlePhysicalOffense.againstPlayer(fire, 50);
        BattlePhysicalOffense.PlayerOutcome weak =
                BattlePhysicalOffense.againstPlayer(fire, -50);
        assertEquals(30, neutral.attributeDamage());
        assertEquals(15, resistant.attributeDamage());
        assertEquals(45, weak.attributeDamage());
        assertEquals(50, resistant.attributeResistance());
    }

    private static SourceNpcPhysicalStats npc(
            int undead, int weakAttribute, int magicResistance) {
        return new SourceNpcPhysicalStats(
                1, 10, false, 0, false, undead, weakAttribute, magicResistance);
    }

    private static SourceWeaponPhysicalStats weapon(
            int material, int blessing,
            int attributeKind, int attributeLevel, int attributeDamage) {
        return new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(
                        material, blessing,
                        attributeKind, attributeLevel, attributeDamage));
    }
}
