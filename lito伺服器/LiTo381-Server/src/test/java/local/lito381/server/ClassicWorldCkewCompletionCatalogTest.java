package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldCkewCompletionCatalogTest {
    @Test
    void retainsEveryClassDialogueActionRewardAndCleanupInSourceOrder() {
        assertEquals(64,
                ClassicWorldCkewCompletionCatalog.NPC_SOURCE_SHA256.length());
        assertEquals("dicardingp13",
                ClassicWorldCkewCompletionCatalog.dialogue(0, 3));
        assertEquals("dicardingk14",
                ClassicWorldCkewCompletionCatalog.dialogue(1, 3));
        assertEquals("dicardinge15",
                ClassicWorldCkewCompletionCatalog.dialogue(2, 3));
        assertEquals("dicardingw13",
                ClassicWorldCkewCompletionCatalog.dialogue(3, 3));
        assertEquals("dicardingp15",
                ClassicWorldCkewCompletionCatalog.dialogue(0, 255));
        assertEquals("dicardingk16",
                ClassicWorldCkewCompletionCatalog.dialogue(1, 255));
        assertEquals("dicardinge17",
                ClassicWorldCkewCompletionCatalog.dialogue(2, 255));
        assertEquals("dicardingw15",
                ClassicWorldCkewCompletionCatalog.dialogue(3, 255));
        assertEquals("dicardingp12",
                ClassicWorldCkewCompletionCatalog.retryHtml(0, "B"));
        assertEquals("dicardingk13",
                ClassicWorldCkewCompletionCatalog.retryHtml(1, "k"));
        assertEquals("dicardinge14",
                ClassicWorldCkewCompletionCatalog.retryHtml(2, "q"));
        assertEquals("dicardingw12",
                ClassicWorldCkewCompletionCatalog.retryHtml(3, "w"));
        assertNull(ClassicWorldCkewCompletionCatalog.completion(0, "x"));
        assertEquals(51,
                ClassicWorldCkewCompletionCatalog.completion(0, "a").rewardItemId());
        assertEquals(56,
                ClassicWorldCkewCompletionCatalog.completion(1, "l").rewardItemId());
        assertEquals(184,
                ClassicWorldCkewCompletionCatalog.completion(2, "y").rewardItemId());
        assertEquals(50,
                ClassicWorldCkewCompletionCatalog.completion(2, "s").rewardItemId());
        assertEquals(20225,
                ClassicWorldCkewCompletionCatalog.completion(3, "x").rewardItemId());
        assertEquals(List.of(40742L, 49165L, 49166L, 49167L, 49168L,
                        49239L, 65L, 133L, 191L, 192L),
                ClassicWorldCkewCompletionCatalog.CLEANUP_ITEM_IDS);
    }

    @Test
    void exactRewardDefinitionsCarryNativeClassDamageAndArmorStatus() {
        InventoryCatalog.NativeWeaponStatus crown = weapon(51);
        assertEquals(List.of(14, 14, 15, 110_000, 1, 6, 1, 0),
                List.of(crown.damageSmall(), crown.damageLarge(), crown.material(),
                        crown.unitWeight(), crown.classMask(), crown.safeEnchant(),
                        crown.hitModifier(), crown.damageModifier()));
        InventoryCatalog.NativeWeaponStatus knight = weapon(56);
        assertEquals(List.of(16, 8, 11, 100_000, 2, 6, 2, 0),
                List.of(knight.damageSmall(), knight.damageLarge(), knight.material(),
                        knight.unitWeight(), knight.classMask(), knight.safeEnchant(),
                        knight.hitModifier(), knight.damageModifier()));
        InventoryCatalog.NativeWeaponStatus sword = weapon(50);
        assertEquals(List.of(14, 6, 14, 80_000, 4, 6, 0, 0),
                List.of(sword.damageSmall(), sword.damageLarge(), sword.material(),
                        sword.unitWeight(), sword.classMask(), sword.safeEnchant(),
                        sword.hitModifier(), sword.damageModifier()));
        InventoryCatalog.NativeWeaponStatus bow = weapon(184);
        assertEquals(List.of(3, 3, 14, 30_000, 4, 6, 2, 4),
                List.of(bow.damageSmall(), bow.damageLarge(), bow.material(),
                        bow.unitWeight(), bow.classMask(), bow.safeEnchant(),
                        bow.hitModifier(), bow.damageModifier()));

        InventoryCatalog.Definition crystal = InventoryCatalog.require(20225);
        assertTrue(crystal.sourceArmor());
        assertEquals(25, crystal.useType());
        assertEquals(2, crystal.sourceArmorStatus().displayArmorClass());
        assertEquals(100, crystal.sourceArmorStatus().manaPoints());
        assertEquals(8, crystal.sourceArmorStatus().classMask());
        assertEquals(7, crystal.sourceArmorStatus().armorType());
    }

    private static InventoryCatalog.NativeWeaponStatus weapon(long itemId) {
        InventoryCatalog.Definition definition = InventoryCatalog.require(itemId);
        assertTrue(definition.weapon());
        return definition.effectiveWeaponStatus();
    }
}
