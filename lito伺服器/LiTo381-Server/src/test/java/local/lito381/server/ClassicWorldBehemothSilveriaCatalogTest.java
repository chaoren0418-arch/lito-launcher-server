package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldBehemothSilveriaCatalogTest {
    @Test
    void selectedHomeTownMapsAppendWithExactFlagsAndResources() throws Exception {
        assertEquals(List.of(1000, 1001),
                ClassicWorldBehemothSilveriaCatalog.MAP_IDS);
        int last = ClassicWorldMapCatalog.supportedMapIds().size();
        assertEquals(List.of(1000, 1001), ClassicWorldMapCatalog.supportedMapIds()
                .subList(last - 3, last - 1));
        assertEquals(ClassicWorldHell.MAP_ID,
                ClassicWorldMapCatalog.supportedMapIds().get(last - 1));
        for (int mapId : ClassicWorldBehemothSilveriaCatalog.MAP_IDS) {
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            ClassicWorldBehemothSilveriaCatalog.MapFlags flags =
                    ClassicWorldBehemothSilveriaCatalog.flags(mapId);
            assertEquals(192, map.width());
            assertEquals(192, map.height());
            assertEquals(flags.startX(), map.startX());
            assertEquals(flags.endX(), map.endX());
            assertEquals(flags.startY(), map.startY());
            assertEquals(flags.endY(), map.endY());
            assertEquals(ClassicWorldBehemothSilveriaCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertFalse(map.allowsRandomTeleport());
            assertEquals(ClassicWorldBehemothSilveriaCatalog.MAP_RESOURCE_SHA256.get(mapId),
                    resourceHash("/maps/" + mapId + ".l38.gz"));
        }
    }

    @Test
    void exactSelectedActorsAndExplicitlyEmptyPopulationsAreLoaded() throws Exception {
        assertEquals(12, ClassicWorldNpcCatalog.all(1000).size());
        assertEquals(16, ClassicWorldNpcCatalog.all(1001).size());
        assertTrue(ClassicWorldMonsterCatalog.definitions(1000).isEmpty());
        assertTrue(ClassicWorldMonsterCatalog.definitions(1001).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(1000));
        assertEquals(0, ClassicWorldDropCatalog.dropCount(1001));

        for (int mapId : ClassicWorldBehemothSilveriaCatalog.MAP_IDS) {
            var npcContract = ClassicWorldBehemothSilveriaCatalog.NPC_CONTRACTS.get(mapId);
            var monsterContract =
                    ClassicWorldBehemothSilveriaCatalog.MONSTER_CONTRACTS.get(mapId);
            var dropContract = ClassicWorldBehemothSilveriaCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npcContract.resourceSha256(), resourceHash(
                    "/world/map" + mapId + "-static-npcs.lwn.gz"));
            assertEquals(monsterContract.resourceSha256(), resourceHash(
                    "/world/map" + mapId + "-monsters.lwm.gz"));
            assertEquals(dropContract.resourceSha256(), resourceHash(
                    "/world/map" + mapId + "-drops.lwd.gz"));
        }
    }

    @Test
    void nativeServicesAreClassifiedWithoutInventingDatabaseTeleportRoutes() {
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isSkillTutor(npc(1000, 1060)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isSkillTutor(npc(1001, 1057)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isRepairer(npc(1000, 1059)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isRepairer(npc(1001, 1061)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isStoryNpc(npc(1000, 1130)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isStoryNpc(npc(1001, 1054)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isDialogueNpc(npc(1000, 1058)));
        assertTrue(ClassicWorldBehemothSilveriaCatalog.isDialogueNpc(npc(1001, 1056)));
        assertEquals("sharial2", ClassicWorldBehemothSilveriaCatalog.linkedTeleportHtml(
                npc(1000, 1058), "teleportURL"));
        assertEquals("sharial3", ClassicWorldBehemothSilveriaCatalog.linkedTeleportHtml(
                npc(1000, 1058), "teleportURLA"));
        assertEquals("dekabia2", ClassicWorldBehemothSilveriaCatalog.linkedTeleportHtml(
                npc(1001, 1056), "teleportURL"));
        assertEquals("dekabia3", ClassicWorldBehemothSilveriaCatalog.linkedTeleportHtml(
                npc(1001, 1056), "teleportURLA"));
        assertNotNull(npc(1000, 1048));
        assertNotNull(npc(1001, 1047));

        assertNull(ClassicWorldTeleportCatalog.source(
                1000, ClassicWorldNpcCatalog.objectId(1058)));
        assertNull(ClassicWorldTeleportCatalog.source(
                1001, ClassicWorldNpcCatalog.objectId(1056)));
    }

    @Test
    void textLinkedBlacksmithsUseOnlySelectedDatabaseCraftRows() throws Exception {
        assertEquals(List.of("request sapphire kiringku", "request obsidian kiringku",
                        "request cold of kiringku"),
                ClassicWorldCraftCatalog.recipes(1000, 85025).stream()
                        .map(ClassicWorldCraftCatalog.Recipe::action).toList());
        assertEquals(List.of("request chainsword destroyer",
                        "request guarder of ancient archer",
                        "request guarder of ancient champion", "request lump of steel",
                        "request halpas symbol", "request chainsword extinctioner",
                        "request cold of chainsword"),
                ClassicWorldCraftCatalog.recipes(1001, 85028).stream()
                        .map(ClassicWorldCraftCatalog.Recipe::action).toList());
        assertTrue(ClassicWorldCraftCatalog.matchesSource(npc(1000, 1059),
                ClassicWorldCraftCatalog.find(
                        1000, 85025, "request obsidian kiringku")));
        assertTrue(ClassicWorldCraftCatalog.matchesSource(npc(1001, 1061),
                ClassicWorldCraftCatalog.find(
                        1001, 85028, "request chainsword destroyer")));
        assertEquals("3ED610BEEB61BFCF4143BA7D6AC4F340A64834977E2D6C6C35A426EDF9380066",
                resourceHash("/world/map1000-npc85025-crafting.lwc.gz"));
        assertEquals("42301780D3144244FF11EEE8D97CD0D49D1015078D0248E212904D0599F6243C",
                resourceHash("/world/map1001-npc85028-crafting.lwc.gz"));
    }

    private static ClassicWorldNpcCatalog.StaticNpc npc(int mapId, int spawnId) {
        return ClassicWorldNpcCatalog.npc(mapId, ClassicWorldNpcCatalog.objectId(spawnId));
    }

    private static String resourceHash(String name) throws Exception {
        try (InputStream in = ClassicWorldBehemothSilveriaCatalogTest.class
                .getResourceAsStream(name)) {
            assertNotNull(in, name);
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }
}
