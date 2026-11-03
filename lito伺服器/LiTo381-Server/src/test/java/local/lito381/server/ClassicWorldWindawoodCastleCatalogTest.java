package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ClassicWorldWindawoodCastleCatalogTest {
    private static final Map<Integer, String> MONSTER_RESOURCES = Map.of(
            23, "68A9C5D5E4D7F60C6C0F11D47EAC6D452CCA999DDFC225C5E796635248C5E220",
            24, "66B877568A50BBD83066668FE71532E7C5B5C44319813E1997EFB1526EE86195",
            29, "106355EEE32C45201923871A4A0A010C4CE6134D4B4F0A3294F754BB1901BC63");
    private static final Map<Integer, String> DROP_RESOURCES = Map.of(
            23, "905457DE62F89EDFF6BE5A7A9986DE2184AD934466B13DE2BD160B1B1BA5612B",
            24, "1C98BB0783E3B5303E851EB0EF27B62269D1418DA2CE4AFB952483C08E53D05A",
            29, "41CED2FD9273C0B6611684C8B35D083FD4154ADA583C19F34F709921BE40D498");

    @Test
    void componentPinsThreeCollisionMapsAndPackedWorldResources() throws Exception {
        int[] ids = {23, 24, 29};
        int[] widths = {128, 128, 64};
        int[] heights = {128, 128, 64};
        for (int index = 0; index < ids.length; index++) {
            int mapId = ids[index];
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(widths[index], map.width());
            assertEquals(heights[index], map.height());
            assertEquals(
                    ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_SOURCE_SHA256
                            .get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.allowsRandomTeleport());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_RESOURCE_SHA256
                            .get(mapId));
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    ClassicWorldNpcCatalog.WINDAWOOD_CASTLE_RESOURCE_SHA256
                            .get(mapId));
            assertResource("/world/map" + mapId + "-monsters.lwm.gz",
                    MONSTER_RESOURCES.get(mapId));
            assertResource("/world/map" + mapId + "-drops.lwd.gz",
                    DROP_RESOURCES.get(mapId));
        }
        assertEquals(List.of(9, 7, 0), List.of(
                ClassicWorldMonsterCatalog.definitions(23).size(),
                ClassicWorldMonsterCatalog.definitions(24).size(),
                ClassicWorldMonsterCatalog.definitions(29).size()));
        assertEquals(List.of(85, 80, 0), List.of(
                population(23), population(24), population(29)));
        assertEquals(List.of(93, 71, 0), List.of(
                ClassicWorldDropCatalog.dropCount(23),
                ClassicWorldDropCatalog.dropCount(24),
                ClassicWorldDropCatalog.dropCount(29)));
        assertEquals(List.of(59, 53, 0), List.of(
                ClassicWorldDropCatalog.items(23).size(),
                ClassicWorldDropCatalog.items(24).size(),
                ClassicWorldDropCatalog.items(29).size()));
    }

    @Test
    void interiorRetainsAllThirteenNpcsAndNativeRoles() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows = ClassicWorldNpcCatalog.all(29);
        assertEquals(List.of(93, 282, 477, 478, 479, 480, 481, 482, 483, 484,
                        765, 767, 770),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertTrue(row(rows, 282).fixedWarehouse());
        for (int spawn = 477; spawn <= 484; spawn++) {
            ClassicWorldNpcCatalog.StaticNpc guard = row(rows, spawn);
            assertEquals(60551, guard.npcId());
            assertEquals("L1Guard", guard.implementation());
            assertEquals(70, guard.level());
        }
        assertTrue(ClassicWorldWindawoodCastleCatalog.isWeaponEnchanter(
                row(rows, 765)));
        assertTrue(ClassicWorldWindawoodCastleCatalog.isArmorEnchanter(
                row(rows, 767)));
        assertTrue(ClassicWorldWindawoodCastleCatalog.isSteward(row(rows, 770)));
        assertEquals(List.of("encwc", "encac", "othmond1"), List.of(
                row(rows, 765).normalAction(), row(rows, 767).normalAction(),
                row(rows, 770).normalAction()));
    }

    @Test
    void tenGroundRoutesFourNpcTeleportsAndGetbackCloseTheComponent() {
        int[][] ground = {
                {251,4,32677,33392,29,32734,32816,0},
                {252,4,32678,33392,29,32735,32816,0},
                {253,4,32686,33376,29,32741,32783,6},
                {254,23,32727,32814,24,32808,32814,4},
                {255,23,32811,32722,29,32742,32790,4},
                {256,24,32808,32814,23,32727,32814,4},
                {257,29,32734,32816,4,32677,33392,4},
                {258,29,32735,32816,4,32678,33392,4},
                {259,29,32741,32783,4,32686,33376,6},
                {260,29,32742,32790,23,32811,32722,4}
        };
        for (int[] expected : ground) {
            ClassicWorldDungeonCatalog.Route route = ClassicWorldDungeonCatalog.find(
                    expected[1], expected[2], expected[3]);
            assertNotNull(route);
            assertEquals(expected[0], route.routeId());
            assertEquals(expected[4], route.destinationMapId());
            assertEquals(expected[5], route.destinationX());
            assertEquals(expected[6], route.destinationY());
            assertEquals(expected[7], route.destinationHeading());
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
        }
        assertEquals(ClassicWorldDungeonCatalog.WINDAWOOD_CASTLE_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.WINDAWOOD_CASTLE_SECONDARY_SOURCE_SHA256);

        List<ClassicWorldTeleportCatalog.Route> teleports =
                ClassicWorldTeleportCatalog.windawoodCastle();
        assertEquals(List.of(598, 599, 600, 601), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(List.of(4, 27, 4, 4), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        long teleporter = ClassicWorldNpcCatalog.objectId(93);
        assertEquals("telecwoods2", ClassicWorldTeleportCatalog.followUpHtml(
                29, teleporter, "teleportURL"));
        assertEquals(List.of("0", "0", "0", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        29, teleporter, "teleportURL"));

        for (int mapId : List.of(23, 24, 29)) {
            ClassicWorldGetbackCatalog.Destination getback =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32740, 32790, 0, 0);
            assertEquals(4, getback.mapId());
            assertEquals(32608, getback.x());
            assertEquals(33178, getback.y());
            assertEquals(5, getback.townId());
        }
    }

    @Test
    void threeExteriorDoorsAndSharedNpcSkillsAreHashLocked() {
        assertEquals("3F02423AA89057BF71916DF30BF86A1350C83A521155CEB78CCEDD7390347475",
                ClassicWorldWindawoodCastleCatalog.DOOR_SOURCE_SHA256);
        Map<Integer, ClassicWorldWindawoodCastleCatalog.Door> doors =
                ClassicWorldWindawoodCastleCatalog.doors().stream().collect(
                        Collectors.toMap(
                                ClassicWorldWindawoodCastleCatalog.Door::doorId,
                                Function.identity()));
        assertEquals(3, doors.size());
        assertEquals(List.of(339,32678,33392,4,0,32677,32678,0,70778),
                doorValues(doors.get(2020)));
        assertEquals(List.of(2736,32590,33409,4,1,33407,33410,10000,70987),
                doorValues(doors.get(2021)));
        assertEquals(List.of(3234,32625,33436,4,0,32623,32626,10000,70687),
                doorValues(doors.get(2022)));
        ClassicWorldWindawoodCastleDoorState state =
                new ClassicWorldWindawoodCastleDoorState();
        assertEquals(3, state.repairAll());
        assertTrue(state.snapshots().stream().allMatch(snapshot ->
                snapshot.currentHp() == snapshot.definition().maximumHp()));
        assertEquals(ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_SKILL_ID,
                ClassicWorldWindawoodCastleCatalog.ENCHANT_WEAPON_SKILL_ID);
        assertEquals(ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                ClassicWorldWindawoodCastleCatalog.ENCHANT_ARMOR_SKILL_ID);
    }

    @Test
    void selectedTextGateActionsRemainClassifiedAsUnreachableResources() {
        assertEquals("castlegate",
                ClassicWorldWindawoodCastleCatalog.CASTLE_GATE_ACTION);
        assertEquals("othmond5",
                ClassicWorldWindawoodCastleCatalog.TEXT_GATE_PAGE);
        assertEquals(List.of(
                        "healegate_winda ogl",
                        "healegate_winda ogf",
                        "healigate_windawood inner gate",
                        "autorepairon",
                        "autorepairoff"),
                ClassicWorldWindawoodCastleCatalog.TEXT_ONLY_GATE_ACTIONS);
        for (String action
                : ClassicWorldWindawoodCastleCatalog.TEXT_ONLY_GATE_ACTIONS) {
            assertTrue(ClassicWorldWindawoodCastleCatalog
                    .isTextOnlyGateAction(action.toUpperCase()));
        }
        assertFalse(ClassicWorldWindawoodCastleCatalog
                .isTextOnlyGateAction("castlegate"));
        assertFalse(ClassicWorldWindawoodCastleCatalog
                .isTextOnlyGateAction(null));
    }

    @Test
    void populationAppendsAll165ActorsAfterEveryPointSevenThreeSevenObject() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_336_TOTAL_POPULATION
                    + ClassicWorldMonsterCatalog.GIRAN_DUNGEON_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            population.actor(mapId, objectId++);
                    assertNotNull(actor);
                    assertEquals(definition.spawnId(), actor.spawnId());
                    assertEquals(slot, actor.spawnNumber());
                    assertTrue(ClassicWorldMapCatalog.forMap(mapId)
                            .standingPassable(actor.x(), actor.y()));
                    observed++;
                }
            }
        }
        assertEquals(165, observed);
    }

    private static int population(int mapId) {
        return ClassicWorldMonsterCatalog.definitions(mapId).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
    }

    private static ClassicWorldNpcCatalog.StaticNpc row(
            List<ClassicWorldNpcCatalog.StaticNpc> rows, int spawnId) {
        return rows.stream().filter(row -> row.spawnId() == spawnId)
                .findFirst().orElseThrow();
    }

    private static List<Integer> doorValues(
            ClassicWorldWindawoodCastleCatalog.Door door) {
        return List.of(door.gfxId(), door.x(), door.y(), door.mapId(),
                door.direction(), door.edgeLow(), door.edgeHigh(),
                door.maximumHp(), door.keeperNpcId());
    }

    private void assertResource(String path, String expectedSha256)
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, path);
            digest.update(in.readAllBytes());
        }
        assertEquals(expectedSha256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()), path);
    }
}
