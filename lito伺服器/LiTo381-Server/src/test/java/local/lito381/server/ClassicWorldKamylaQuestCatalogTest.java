package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ClassicWorldKamylaQuestCatalogTest {
    private static final int KAMYLA = ClassicWorldPirateBackHalfCatalog.KAMYLA_NPC_ID;
    private static final int FRANCU = ClassicWorldPirateBackHalfCatalog.FRANCU_NPC_ID;

    @Test
    void retainedDialogueBranchOrderIsExact() {
        assertEquals("kamyla000", dialogue(KAMYLA, Map.of(), Map.of()));
        assertEquals("kamyla1", dialogue(KAMYLA,
                Map.of(31, 255), Map.of(40621L, 1)));
        assertEquals("kamyla8", dialogue(KAMYLA, Map.of(32, 1), Map.of()));
        assertEquals("kamyla9", dialogue(KAMYLA,
                Map.of(32, 2), Map.of(40644L, 1)));
        assertEquals("kamyla11", dialogue(KAMYLA, Map.of(32, 3), Map.of()));
        assertEquals("kamyla12", dialogue(KAMYLA,
                Map.of(32, 3), Map.of(40630L, 1)));
        assertEquals("kamyla14", dialogue(KAMYLA, Map.of(32, 4), Map.of()));
        assertEquals("kamyla15", dialogue(KAMYLA,
                Map.of(32, 4), Map.of(40717L, 1)));
        assertEquals("kamyla26", dialogue(KAMYLA, Map.of(32, 255), Map.of()));
        assertEquals("francu1", dialogue(FRANCU, Map.of(32, 1), Map.of()));
        assertEquals("francu12", dialogue(FRANCU, Map.of(32, 2), Map.of()));
        assertNull(dialogue(12345, Map.of(), Map.of()));
    }

    @Test
    void retainedActionsMutateOnlyTheProvedQuestState() {
        ClassicWorldKamylaQuestCatalog.ActionResult start = action(
                KAMYLA, "a", Map.of(31, 255), Map.of(40621L, 1));
        assertEquals(List.of("kamyla7", 1, 0, 0, false), List.of(
                start.html(), start.nextStep(), start.consumedItemId(),
                start.givenItemId(), start.teleports()));

        assertPortal(action(KAMYLA, "b", Map.of(32, 1), Map.of()),
                482, 32679, 32742, 5);
        ClassicWorldKamylaQuestCatalog.ActionResult map = action(
                FRANCU, "a", Map.of(32, 1), Map.of());
        assertEquals(List.of("francu10", 2, 0, 40644), List.of(
                map.html(), map.nextStep(), map.consumedItemId(), map.givenItemId()));
        assertPortal(action(FRANCU, "b", Map.of(32, 2), Map.of()),
                481, 32735, 32845, 5);

        ClassicWorldKamylaQuestCatalog.ActionResult mapConsumed = action(
                KAMYLA, "c", Map.of(32, 2), Map.of(40644L, 1));
        assertEquals(List.of("kamyla10", 3, 40644), List.of(
                mapConsumed.html(), mapConsumed.nextStep(),
                mapConsumed.consumedItemId()));
        assertPortal(action(KAMYLA, "d", Map.of(32, 3), Map.of()),
                483, 32736, 32800, 5);
        ClassicWorldKamylaQuestCatalog.ActionResult diary = action(
                KAMYLA, "e", Map.of(32, 3), Map.of(40630L, 1));
        assertEquals(List.of("kamyla13", 4, 40630), List.of(
                diary.html(), diary.nextStep(), diary.consumedItemId()));
        assertPortal(action(KAMYLA, "f", Map.of(32, 4), Map.of()),
                484, 32746, 32807, 5);

        assertNull(action(KAMYLA, "c", Map.of(32, 2), Map.of()));
        assertNull(action(FRANCU, "a", Map.of(32, 2), Map.of()));
    }

    @Test
    void nativeFiveChoiceRewardSurfaceIsAtomicAndOneShot() {
        Map<String, Integer> rewards = Map.of(
                "g", 33, "h", 79, "i", 178, "j", 118, "k", 20234);
        assertEquals(rewards, ClassicWorldKamylaQuestCatalog.finalRewards());
        for (Map.Entry<String, Integer> reward : rewards.entrySet()) {
            ClassicWorldKamylaQuestCatalog.ActionResult result = action(
                    KAMYLA, reward.getKey(), Map.of(32, 4), Map.of(40717L, 1));
            assertNotNull(result);
            assertEquals(List.of("kamyla25", 255, 40717, reward.getValue()),
                    List.of(result.html(), result.nextStep(),
                            result.consumedItemId(), result.givenItemId()));
            assertTrue(result.mutatesState());
            assertNull(action(KAMYLA, reward.getKey(), Map.of(32, 4), Map.of()));
            assertNull(action(KAMYLA, reward.getKey(), Map.of(32, 255),
                    Map.of(40717L, 1)));
        }
    }

    @Test
    void reconstructedFrancuAndRoute1160StayCrossChecked() {
        ClassicWorldNpcCatalog.StaticNpc francu = ClassicWorldNpcCatalog.npc(
                482, ClassicWorldNpcCatalog.objectId(71089));
        assertNotNull(francu);
        assertTrue(ClassicWorldKamylaQuestCatalog.isDialogueNpc(francu));
        assertEquals(List.of(71089, 32679, 32860, "francu1", "francu001"),
                List.of(francu.npcId(), francu.x(), francu.y(),
                        francu.normalAction(), francu.chaoticAction()));

        ClassicWorldTeleportCatalog.Route route =
                ClassicWorldTeleportCatalog.pirateBackHalfFrancuReturnRoute();
        assertEquals(List.of(1160, 482, 71089, "b", 481, 32735, 32845, 5),
                List.of(route.routeId(), route.sourceMapId(), route.sourceNpcId(),
                        route.action(), route.destinationMapId(), route.destinationX(),
                        route.destinationY(), route.heading()));
        assertEquals(route, ClassicWorldTeleportCatalog.route(
                482, francu.objectId(), "b"));
    }

    @Test
    void questOnlyItemsHaveCompleteInventoryWireDefinitions() {
        for (int itemId : List.of(40644, 40717, 33, 79, 178, 118, 20234)) {
            InventoryCatalog.Definition definition = InventoryCatalog.find(itemId);
            assertNotNull(definition);
            assertEquals(itemId, definition.catalogId());
            assertNotNull(definition.name());
        }
        assertRewardWeapon(33, "$3790", 1871, 4759, 1807,
                9, 12, 11, 0x22, 6, 0, 2);
        assertRewardWeapon(79, "$3791", 1875, 4763, 1808,
                14, 10, 12, 0x10, 6, 0, 0);
        assertRewardWeapon(178, "$3792", 1873, 4761, 1809,
                3, 2, 8, 0x44, 6, 1, 1);
        assertRewardWeapon(118, "$3793", 1878, 773, 1810,
                1, 1, 12, 0x48, 6, 0, 0);

        InventoryCatalog.Definition shield = InventoryCatalog.find(20234);
        assertEquals(List.of(25, 1874, 23, "$3794", 1811, 50_000),
                List.of(shield.useType(), shield.icon(), shield.groundGfx(),
                        shield.name(), shield.itemDescriptionId(), shield.unitWeight()));
        assertTrue(shield.sourceArmor());
        InventoryCatalog.NativeArmorStatus armor = shield.sourceArmorStatus();
        assertEquals(List.of(3, 11, 50_000, 1, 6, 2, 7),
                List.of(armor.displayArmorClass(), armor.material(), armor.unitWeight(),
                        armor.classMask(), armor.safeEnchant(), armor.charisma(),
                        armor.armorType()));
    }

    private static void assertRewardWeapon(
            int itemId, String name, int icon, int groundGfx, int description,
            int damageSmall, int damageLarge, int material, int classMask,
            int safeEnchant, int hitModifier, int damageModifier) {
        InventoryCatalog.Definition definition = InventoryCatalog.find(itemId);
        assertEquals(List.of(1, icon, groundGfx, name, description),
                List.of(definition.useType(), definition.icon(), definition.groundGfx(),
                        definition.name(), definition.itemDescriptionId()));
        InventoryCatalog.NativeWeaponStatus weapon = definition.sourceWeaponStatus();
        assertNotNull(weapon);
        assertEquals(List.of(damageSmall, damageLarge, material, classMask,
                        safeEnchant, hitModifier, damageModifier),
                List.of(weapon.damageSmall(), weapon.damageLarge(), weapon.material(),
                        weapon.classMask(), weapon.safeEnchant(), weapon.hitModifier(),
                        weapon.damageModifier()));
    }

    private static String dialogue(int npcId, Map<Integer, Integer> quests,
                                   Map<Long, Integer> inventory) {
        return ClassicWorldKamylaQuestCatalog.dialogue(npcId, quests, inventory);
    }

    private static ClassicWorldKamylaQuestCatalog.ActionResult action(
            int npcId, String action, Map<Integer, Integer> quests,
            Map<Long, Integer> inventory) {
        return ClassicWorldKamylaQuestCatalog.action(
                npcId, action, quests, inventory);
    }

    private static void assertPortal(
            ClassicWorldKamylaQuestCatalog.ActionResult result,
            int mapId, int x, int y, int heading) {
        assertNotNull(result);
        assertTrue(result.teleports());
        assertEquals(new Protocol.PortalDestination(mapId, x, y, true),
                result.destination());
        assertEquals(heading, result.heading());
    }
}
