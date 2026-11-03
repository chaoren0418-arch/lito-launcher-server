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

final class ClassicWorldKentCastleCatalogTest {
    private static final Map<Integer, String> MONSTER_RESOURCES = Map.of(
            15, "734A91D7BB05A4309920F691C07F42618B2BA0EB3906964E3DC2D4F7EF6F9379",
            240, "F3BAE8D6FC787B96E11EE6F9F57518C9627D75B78883534013DBCC9D88EA46B2",
            241, "F97929DC92993D968821246E109071D7FB7AB90565E84EC9AB9AFCDB78A400BE",
            242, "04801523A5F618AEB77DABE6DDFC8ACAB64DF45D8D8AC6F5ECA5AE5924762A9D",
            243, "D4FB71C4218FE37786D7B3D98BC73854F7680657DD5B04CCC9770B345F842BDA");
    private static final Map<Integer, String> DROP_RESOURCES = Map.of(
            15, "339EE654FC297C15B5FD00B62090B10D640251AAD5030ADEB865E7C897E8292C",
            240, "352BF54C5D7A6A1DC92E16E06FF61E615C3FA61717571D646DFFBF1196DB728F",
            241, "D7CD5A44FC91311F4EC9DDB0F8FCF2BC4EFB3689214C71560FAA6A93D525DC7B",
            242, "E6844976CC12665DE8D3C38AB6CEDB9BAC3F778EAD5CF36CEB59C2D365153AD9",
            243, "171F5E526BD203515F9D08547BCC9F9A5B91E20639F5AEDDA0EAB8990E9EE599");

    @Test
    void componentPinsFiveCollisionMapsAndPackedWorldResources() throws Exception {
        int[] ids = {15, 240, 241, 242, 243};
        int[] widths = {64, 128, 128, 128, 128};
        int[] heights = {64, 128, 128, 128, 192};
        for (int index = 0; index < ids.length; index++) {
            int mapId = ids[index];
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(widths[index], map.width());
            assertEquals(heights[index], map.height());
            assertEquals(ClassicWorldMapCatalog.KENT_COMPONENT_MAP_SOURCE_SHA256
                    .get(mapId), map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertEquals(mapId == 15, map.allowsRandomTeleport());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldMapCatalog.KENT_COMPONENT_MAP_RESOURCE_SHA256
                            .get(mapId));
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    ClassicWorldNpcCatalog.KENT_COMPONENT_RESOURCE_SHA256
                            .get(mapId));
            assertResource("/world/map" + mapId + "-monsters.lwm.gz",
                    MONSTER_RESOURCES.get(mapId));
            assertResource("/world/map" + mapId + "-drops.lwd.gz",
                    DROP_RESOURCES.get(mapId));
        }
        assertEquals(List.of(0, 4, 6, 10, 4), ids(ids,
                mapId -> ClassicWorldMonsterCatalog.definitions(mapId).size()));
        assertEquals(List.of(0, 96, 96, 80, 65), ids(ids,
                ClassicWorldKentCastleCatalogTest::population));
        assertEquals(337, ClassicWorldMonsterCatalog.KENT_COMPONENT_POPULATION);
        for (int mapId : ids) {
            assertEquals(0, ClassicWorldDropCatalog.dropCount(mapId));
            assertTrue(ClassicWorldDropCatalog.items(mapId).isEmpty());
        }
    }

    @Test
    void interiorRetainsAllSevenNpcsAndNativeRoles() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows = ClassicWorldNpcCatalog.all(15);
        assertEquals(List.of(46, 48, 126, 263, 673, 675, 676),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals(List.of("teleckent1", "kentober1", "niki", "storage",
                        "encwc", "encac", "ishmael1"),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::normalAction)
                        .toList());
        assertTrue(ClassicWorldKentCastleCatalog.isNiki(row(rows, 126)));
        assertTrue(row(rows, 263).fixedWarehouse());
        assertTrue(ClassicWorldKentCastleCatalog.isWeaponEnchanter(row(rows, 673)));
        assertTrue(ClassicWorldKentCastleCatalog.isArmorEnchanter(row(rows, 675)));
        assertTrue(ClassicWorldKentCastleCatalog.isSteward(row(rows, 676)));
        assertEquals("A7F51095A2B4BD99569CB93C1CD8E6CF02A9F3F4CC206D164ACEB353B4A3D423",
                ClassicWorldKentCastleCatalog.NIKI_SHOP_SOURCE_SHA256);
    }

    @Test
    void groundNpcTeleportAndGetbackContractsCloseTheComponent() {
        int[][] ground = {
                {261,4,33170,32759,15,32735,32812,0},
                {262,4,33171,32759,15,32736,32812,0},
                {263,4,33179,32743,15,32743,32783,6},
                {264,15,32735,32812,4,33170,32759,4},
                {265,15,32736,32812,4,33171,32759,4},
                {266,15,32743,32783,4,33179,32743,6},
                {267,240,32670,33122,241,32760,32882,4},
                {268,241,32732,32928,242,32808,32917,4},
                {269,241,32760,32882,240,32670,33122,0},
                {270,242,32740,33007,243,32745,32852,4},
                {271,242,32808,32917,241,32732,32928,0},
                {272,243,32745,32852,242,32740,33007,0}
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
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
        }
        assertEquals(ClassicWorldDungeonCatalog.KENT_COMPONENT_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.KENT_COMPONENT_SECONDARY_SOURCE_SHA256);

        List<ClassicWorldTeleportCatalog.Route> teleports =
                ClassicWorldTeleportCatalog.kentComponent();
        assertEquals(List.of(85, 86, 87, 88), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(List.of(4, 10, 54, 240), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        assertEquals(List.of(0, 0, 0, 10_000), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::priceAmount).toList());
        long kentTeleporter = ClassicWorldNpcCatalog.objectId(46);
        assertEquals("teleckent2", ClassicWorldTeleportCatalog.followUpHtml(
                15, kentTeleporter, "teleportURL"));
        assertEquals(List.of("0", "0", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        15, kentTeleporter, "teleportURL"));
        long oberTeleporter = ClassicWorldNpcCatalog.objectId(48);
        assertEquals("kentober2", ClassicWorldTeleportCatalog.followUpHtml(
                15, oberTeleporter, "teleportURL"));
        assertEquals(List.of("10000"), ClassicWorldTeleportCatalog.followUpArguments(
                15, oberTeleporter, "teleportURL"));

        for (int mapId : ClassicWorldMapCatalog.KENT_COMPONENT_MAP_IDS) {
            ClassicWorldGetbackCatalog.Destination getback =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32740, 32790, 0, 0);
            assertEquals(4, getback.mapId());
            assertEquals(33048, getback.x());
            assertEquals(32750, getback.y());
            assertEquals(6, getback.townId());
        }
    }

    @Test
    void fourExteriorDoorsAndTextOnlyActionsAreHashLocked() {
        Map<Integer, ClassicWorldKentCastleCatalog.Door> doors =
                ClassicWorldKentCastleCatalog.doors().stream().collect(
                        Collectors.toMap(
                                ClassicWorldKentCastleCatalog.Door::doorId,
                                Function.identity()));
        assertEquals(4, doors.size());
        assertEquals(List.of(339,33171,32759,4,0,33170,33171,0,70656),
                doorValues(doors.get(2000)));
        assertEquals(List.of(11987,33111,32770,4,1,32767,32773,20000,70549),
                doorValues(doors.get(2001)));
        assertEquals(List.of(11989,33112,32763,4,1,33112,33112,25000,70549),
                doorValues(doors.get(2002)));
        assertEquals(List.of(11991,33112,32777,4,1,33112,33112,25000,70549),
                doorValues(doors.get(2003)));
        ClassicWorldKentCastleDoorState state = new ClassicWorldKentCastleDoorState();
        assertEquals(4, state.repairAll());
        assertTrue(state.snapshots().stream().allMatch(snapshot ->
                snapshot.currentHp() == snapshot.definition().maximumHp()));
        assertEquals(List.of(
                        "healegate_kent ogl", "healegate_kent ogf",
                        "healigate_kent inner gate", "autorepairon", "autorepairoff"),
                ClassicWorldKentCastleCatalog.TEXT_ONLY_GATE_ACTIONS);
        for (String action : ClassicWorldKentCastleCatalog.TEXT_ONLY_GATE_ACTIONS) {
            assertTrue(ClassicWorldKentCastleCatalog
                    .isTextOnlyGateAction(action.toUpperCase()));
        }
        assertFalse(ClassicWorldKentCastleCatalog.isTextOnlyGateAction("castlegate"));
    }

    @Test
    void populationAppends337ActorsAfterEveryPointSevenThreeEightObject() {
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_336_TOTAL_POPULATION
                    + ClassicWorldMonsterCatalog.GIRAN_DUNGEON_POPULATION
                    + ClassicWorldMonsterCatalog.WINDAWOOD_CASTLE_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldMapCatalog.KENT_COMPONENT_MAP_IDS) {
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
        assertEquals(337, observed);
    }

    private static List<Integer> ids(
            int[] ids, java.util.function.IntFunction<Integer> value) {
        return java.util.Arrays.stream(ids).mapToObj(value).toList();
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

    private static List<Integer> doorValues(ClassicWorldKentCastleCatalog.Door door) {
        return List.of(door.gfxId(), door.x(), door.y(), door.mapId(),
                door.direction(), door.edgeLow(), door.edgeHigh(),
                door.maximumHp(), door.keeperNpcId());
    }

    private void assertResource(String path, String expectedSha256) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, path);
            digest.update(in.readAllBytes());
        }
        assertEquals(expectedSha256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()), path);
    }
}
