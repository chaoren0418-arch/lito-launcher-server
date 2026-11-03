package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleItemArmorEnchantStatusTest {
    @Test
    void npcEnchantArmorBindsMinusThreeAcToOneItemAndExpires() {
        BattleItemArmorEnchantStatus status = new BattleItemArmorEnchantStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        assertEquals(0, status.activate(start, 1800,
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                201, "armor", -3));
        BattleItemArmorEnchantStatus.Modifier active = status.modifier(start, 201);
        assertTrue(active.active());
        assertEquals(-3, active.armorClass());
        assertEquals(1800, active.remainingSeconds());
        assertFalse(status.modifier(start, 202).active());
        assertEquals(List.of(), status.expire(
                start + TimeUnit.SECONDS.toNanos(1800) - 1));
        assertEquals(List.of(new BattleItemArmorEnchantStatus.Expiration(
                        ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                        201, "armor")),
                status.expire(start + TimeUnit.SECONDS.toNanos(1800)));
    }

    @Test
    void sameArmorRecastRefreshesAndInvalidLeasesAreRejected() {
        BattleItemArmorEnchantStatus status = new BattleItemArmorEnchantStatus();
        assertEquals(0, status.activate(0, 1800,
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                201, "armor", -3));
        assertEquals(ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                status.activate(TimeUnit.SECONDS.toNanos(100), 1800,
                        ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                        201, "armor", -3));
        assertEquals(1800, status.modifier(
                TimeUnit.SECONDS.toNanos(100), 201).remainingSeconds());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1800, 12, 201, "armor", -3));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1800,
                        ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                        201, "armor", 0));
        status.clear();
        assertEquals(0, status.activeLeaseCount(0));
    }

    @Test
    void namedRemovalClearsAllAndOnlyMatchingArmorSkillLeases() {
        BattleItemArmorEnchantStatus status = new BattleItemArmorEnchantStatus();
        status.activate(0, 1_800, BattleSpellCatalog.BLESSED_ARMOR,
                201, "armor-one", -3);
        status.activate(0, 1_800, BattleSpellCatalog.BLESSED_ARMOR,
                202, "armor-two", -3);
        assertFalse(status.removeSkill(999));
        assertEquals(2, status.activeLeaseCount(0));
        assertTrue(status.removeSkill(BattleSpellCatalog.BLESSED_ARMOR));
        assertEquals(0, status.activeLeaseCount(0));
    }
}
