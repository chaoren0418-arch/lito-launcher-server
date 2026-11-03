package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ClassicWorldStoryQuestCatalogTest {
    @Test void selectedStoryResourceContainsEveryEnabledRowAndExactGunterSlice() {
        assertEquals(679, ClassicWorldStoryQuestCatalog.all().size());
        assertEquals(17, ClassicWorldStoryQuestCatalog.all().stream()
                .filter(row -> row.npcId() == 70522).count());
        assertEquals(8, ClassicWorldStoryQuestCatalog.all().stream()
                .filter(row -> row.npcId() == 70554).count());
        assertEquals(205, ClassicWorldStoryQuestCatalog.all().stream()
                .filter(row -> row.eventType()
                        == ClassicWorldStoryQuestCatalog.EventType.NPC_ACTION).count());
        assertEquals(474, ClassicWorldStoryQuestCatalog.all().stream()
                .filter(row -> row.eventType()
                        == ClassicWorldStoryQuestCatalog.EventType.NPC_TALK).count());
    }

    @Test void knightThirtyChainStartsTalksTrialsAndAwardsExactSword() {
        Map<Integer, Integer> quests = Map.of(10701, 1);
        ClassicWorldStoryQuestCatalog.Dialogue first =
                ClassicWorldStoryQuestCatalog.dialogue(70522, 1, 30, 61,
                        quests, Map.of());
        assertNotNull(first);
        assertEquals(1070201, first.event().id());
        assertEquals("gunterkE1", first.html());
        assertEquals(0, first.questSteps().get(10702));

        ClassicWorldStoryQuestCatalog.Dialogue trial =
                ClassicWorldStoryQuestCatalog.dialogue(70522, 1, 30, 61,
                        first.questSteps(), Map.of());
        assertEquals(1070202, trial.event().id());
        ClassicWorldStoryQuestCatalog.Event link =
                ClassicWorldStoryQuestCatalog.action(70522, "quest 14 gunterkE2",
                        1, 30, 61, trial.questSteps(), Map.of());
        assertNotNull(link);
        assertEquals(1070204, link.id());
        assertEquals("gunterkE2", link.html());

        Map<Long, Integer> claw = Map.of(InventoryCatalog.YANGGUO_CLAW, 1);
        ClassicWorldStoryQuestCatalog.Dialogue ready =
                ClassicWorldStoryQuestCatalog.dialogue(70522, 1, 30, 61,
                        trial.questSteps(), claw);
        assertEquals(1070203, ready.event().id());
        assertEquals("gunterkE2", ready.html());
        ClassicWorldStoryQuestCatalog.Event reward =
                ClassicWorldStoryQuestCatalog.action(70522,
                        "request sword of red knights", 1, 30, 61,
                        ready.questSteps(), claw);
        assertNotNull(reward);
        assertEquals(1070205, reward.id());
        assertEquals(InventoryCatalog.YANGGUO_CLAW,
                reward.consumedItems().get(0).itemId());
        assertEquals(InventoryCatalog.RED_KNIGHT_SWORD,
                reward.givenItems().get(0).itemId());
        Map<Integer, Integer> completed =
                ClassicWorldStoryQuestCatalog.applyQuestMutation(
                        reward, ready.questSteps());
        assertEquals(1, completed.get(10702));
        assertEquals(0, completed.get(10703));
        ClassicWorldStoryQuestCatalog.Dialogue finished =
                ClassicWorldStoryQuestCatalog.dialogue(70522, 1, 30, 61,
                        completed, Map.of());
        assertEquals(1070206, finished.event().id());
        assertEquals("gunterkEcg", finished.html());
        assertNull(ClassicWorldStoryQuestCatalog.action(70522,
                "request sword of red knights", 1, 30, 61,
                completed, claw));
    }

    @Test void crownChainUsesExactClientActionsAndClassFallbacks() {
        // Zero's immediately preceding chain event has already created 10102.
        Map<Integer, Integer> crown = Map.of(10101, 1, 10102, 0);
        ClassicWorldStoryQuestCatalog.Dialogue test =
                ClassicWorldStoryQuestCatalog.dialogue(
                        70522, 0, 15, 0, crown, Map.of());
        assertEquals(40000070, test.event().id());
        assertEquals("gunterp1", test.html());
        ClassicWorldStoryQuestCatalog.Event passed =
                ClassicWorldStoryQuestCatalog.action(70522, "guntertest",
                        0, 15, 0, crown, Map.of());
        assertEquals(40000073, passed.id());
        Map<Integer, Integer> after =
                ClassicWorldStoryQuestCatalog.applyQuestMutation(passed, crown);
        assertEquals(1, after.get(10102));
        assertEquals(0, after.get(10103));
        assertNull(ClassicWorldStoryQuestCatalog.action(70522, "guntertest",
                0, 15, 0, after, Map.of()));

        Map<Long, Integer> scroll = Map.of(InventoryCatalog.LIFE_SCROLL, 1);
        ClassicWorldStoryQuestCatalog.Event spellbook =
                ClassicWorldStoryQuestCatalog.action(70522, "request spellbook112",
                        0, 15, 0, after, scroll);
        assertNotNull(spellbook);
        assertEquals(InventoryCatalog.PRECISE_TARGET_SPELLBOOK,
                spellbook.givenItems().get(0).itemId());
        assertNull(ClassicWorldStoryQuestCatalog.action(70522,
                "request spellbook112", 0, 15, 0, after, Map.of()));

        assertEquals("guntere1", ClassicWorldStoryQuestCatalog.dialogue(
                70522, 2, 1, 0, Map.of(), Map.of()).html());
        assertEquals("gunterw1", ClassicWorldStoryQuestCatalog.dialogue(
                70522, 3, 1, 0, Map.of(), Map.of()).html());
    }

    @Test void crownFifteenStartsAtZeroOwnsMissingItemsAndChainsAcrossGunter() {
        ClassicWorldStoryQuestCatalog.Dialogue accepted =
                ClassicWorldStoryQuestCatalog.dialogue(
                        70554, 0, 15, 0, 0, 971, Map.of(), Map.of());
        assertNotNull(accepted);
        assertEquals(40000001, accepted.event().id());
        assertEquals("zero1", accepted.html());
        assertEquals(0, accepted.questSteps().get(10101));

        ClassicWorldStoryQuestCatalog.ActionResolution missing =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70554, "request cloak of red", 0, 15, 0, 0, 971,
                        accepted.questSteps(), Map.of());
        assertNotNull(missing);
        assertFalse(missing.admitted());
        assertEquals(40000068, missing.event().id());
        assertEquals(InventoryCatalog.SEARCH_DOCUMENT,
                missing.missingItems().get(0).itemId());
        assertEquals(1, missing.missingItems().get(0).count());

        Map<Long, Integer> document = Map.of(InventoryCatalog.SEARCH_DOCUMENT, 1);
        ClassicWorldStoryQuestCatalog.ActionResolution exchanged =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70554, "request cloak of red", 0, 15, 0, 0, 971,
                        accepted.questSteps(), document);
        assertTrue(exchanged.admitted());
        Map<Integer, Integer> atGunter =
                ClassicWorldStoryQuestCatalog.applyQuestMutation(
                        exchanged.event(), accepted.questSteps());
        assertEquals(1, atGunter.get(10101));
        assertEquals(0, atGunter.get(10102));
        assertEquals("zero5", ClassicWorldStoryQuestCatalog.responseHtml(
                exchanged.event(), atGunter));

        ClassicWorldStoryQuestCatalog.Dialogue gunter =
                ClassicWorldStoryQuestCatalog.dialogue(
                        70522, 0, 15, 0, 0, 100, atGunter, Map.of());
        assertEquals(40000070, gunter.event().id());
        assertEquals("gunterp1", gunter.html());
        ClassicWorldStoryQuestCatalog.ActionResolution tested =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70522, "guntertest", 0, 15, 0, 0, 100,
                        atGunter, Map.of());
        Map<Integer, Integer> needsScroll =
                ClassicWorldStoryQuestCatalog.applyQuestMutation(
                        tested.event(), atGunter);
        assertEquals(1, needsScroll.get(10102));
        assertEquals(0, needsScroll.get(10103));
        assertEquals("gunterpev1", ClassicWorldStoryQuestCatalog.responseHtml(
                tested.event(), needsScroll));

        ClassicWorldStoryQuestCatalog.ActionResolution noScroll =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70522, "request spellbook112", 0, 15, 0, 0, 100,
                        needsScroll, Map.of());
        assertNotNull(noScroll);
        assertFalse(noScroll.admitted());
        assertEquals(InventoryCatalog.LIFE_SCROLL,
                noScroll.missingItems().get(0).itemId());
        ClassicWorldStoryQuestCatalog.ActionResolution learned =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70522, "request spellbook112", 0, 15, 0, 0, 100,
                        needsScroll, Map.of(InventoryCatalog.LIFE_SCROLL, 1));
        assertTrue(learned.admitted());
        Map<Integer, Integer> completed =
                ClassicWorldStoryQuestCatalog.applyQuestMutation(
                        learned.event(), needsScroll);
        assertEquals(1, completed.get(10103));
        assertEquals("", ClassicWorldStoryQuestCatalog.responseHtml(
                learned.event(), completed));
    }

    @Test void exactQuestItemAndRewardWireDefinitionsAreRegistered() {
        InventoryCatalog.Definition sword = InventoryCatalog.require(
                InventoryCatalog.RED_KNIGHT_SWORD);
        assertEquals("$313", sword.name());
        assertEquals(441, sword.icon());
        assertEquals(3, sword.groundGfx());
        assertFalse(sword.stackable());
        assertTrue(sword.weapon());
        assertEquals(8, InventoryCatalog.nativeWeaponStatus(
                InventoryCatalog.RED_KNIGHT_SWORD).damageSmall());
        assertEquals(12, InventoryCatalog.nativeWeaponStatus(
                InventoryCatalog.RED_KNIGHT_SWORD).damageLarge());
        assertEquals(0xF7, InventoryCatalog.require(
                InventoryCatalog.PRECISE_TARGET_SPELLBOOK).useType());
        assertEquals(0xFF, InventoryCatalog.require(
                InventoryCatalog.LIFE_SCROLL).useType());
        InventoryCatalog.Definition document = InventoryCatalog.require(
                InventoryCatalog.SEARCH_DOCUMENT);
        assertEquals("$1908", document.name());
        assertEquals(914, document.icon());
        assertFalse(document.stackable());
        assertFalse(document.tradable());
        InventoryCatalog.Definition cloak = InventoryCatalog.require(
                InventoryCatalog.RED_CLOAK);
        assertEquals("$1918", cloak.name());
        assertEquals(19, cloak.useType());
        assertTrue(cloak.sourceArmor());
        assertEquals(4, cloak.sourceArmorStatus().armorType());
        assertEquals(1, cloak.sourceArmorStatus().classMask());
        assertEquals(1, cloak.sourceArmorStatus().charisma());
    }
}
