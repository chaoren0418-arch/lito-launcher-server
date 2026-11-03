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

final class ClassicWorldGiranCastleCatalogTest {
    @Test
    void map52PinsCollisionFlagsGetbackAndPackedResources() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(52);
        assertEquals(32640, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(128, map.width());
        assertEquals(128, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP52_SOURCE_SHA256, map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.allowsRandomTeleport());
        assertResource("/maps/52.l38.gz", ClassicWorldMapCatalog.MAP52_RESOURCE_SHA256);
        assertResource("/world/map52-static-npcs.lwn.gz",
                ClassicWorldNpcCatalog.MAP52_RESOURCE_SHA256);
        assertResource("/world/map52-monsters.lwm.gz",
                "45E9941B3B5AF39C2D5599B32B854D6E5546A248C7B89248443B5083443BE6A1");
        assertResource("/world/map52-drops.lwd.gz",
                "7612D907ACB2BBE439F8E8CE72CD42D5F37CDB4A190639D44D78A76B8CE4AE84");
        assertEquals(List.of(), ClassicWorldMonsterCatalog.definitions(52));
        assertEquals(0, ClassicWorldDropCatalog.dropCount(52));
        assertEquals(List.of(), ClassicWorldDropCatalog.items(52));

        assertEquals(ClassicWorldMapCatalog.MAP52_GETBACK_SOURCE_SHA256,
                ClassicWorldGetbackCatalog.MAP52_SOURCE_SHA256);
        ClassicWorldGetbackCatalog.Destination destination =
                ClassicWorldGetbackCatalog.resolve(52, 32728, 32828, 0, 0);
        assertEquals(4, destination.mapId());
        assertEquals(33435, destination.x());
        assertEquals(32803, destination.y());
        assertEquals(7, destination.townId());
    }

    @Test
    void allThirteenStaticNpcsRetainTheirSelectedRows() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows = ClassicWorldNpcCatalog.all(52);
        assertEquals(13, rows.size());
        assertEquals(List.of(77, 273, 401, 402, 403, 404, 405, 406, 407, 408,
                        717, 719, 725),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals("5B400EC1C2454846FD29572C738AEEB4D7701DAFA15C26DBE07AED931ED61089",
                ClassicWorldNpcCatalog.MAP52_SOURCE_SHA256);

        ClassicWorldNpcCatalog.StaticNpc teleporter = row(rows, 77);
        assertEquals(List.of(50035, 32733, 32789, 5, 940), List.of(
                teleporter.npcId(), teleporter.x(), teleporter.y(),
                teleporter.heading(), teleporter.gfxId()));
        assertEquals("telecgiran1", teleporter.normalAction());
        assertEquals("L1Teleporter", teleporter.implementation());

        ClassicWorldNpcCatalog.StaticNpc warehouse = row(rows, 273);
        assertTrue(warehouse.fixedWarehouse());
        assertEquals("storage", warehouse.normalAction());
        for (int spawn = 401; spawn <= 408; spawn++) {
            ClassicWorldNpcCatalog.StaticNpc guard = row(rows, spawn);
            assertEquals(60528, guard.npcId());
            assertEquals("L1Guard", guard.implementation());
            assertEquals(335, guard.gfxId());
            assertEquals(70, guard.level());
        }
        assertTrue(ClassicWorldGiranCastleCatalog.isWeaponEnchanter(row(rows, 717)));
        assertTrue(ClassicWorldGiranCastleCatalog.isArmorEnchanter(row(rows, 719)));
        assertTrue(ClassicWorldGiranCastleCatalog.isSteward(row(rows, 725)));
        assertEquals(List.of("encwc", "encac", "orville1"),
                List.of(row(rows, 717).normalAction(), row(rows, 719).normalAction(),
                        row(rows, 725).normalAction()));
    }

    @Test
    void fourGroundRoutesAndFourNpcTeleportsCloseTheNativeComponent() {
        assertEquals("3C7942B13CDC32AB8B5671DCB12143FF1D5C6069F09DE99C966B94381172727D",
                ClassicWorldDungeonCatalog.GIRAN_CASTLE_SOURCE_SHA256);
        assertEquals(ClassicWorldDungeonCatalog.GIRAN_CASTLE_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.GIRAN_CASTLE_SECONDARY_SOURCE_SHA256);
        int[][] ground = {
                {247,4,33631,32660,52,32728,32828,0},
                {248,4,33632,32660,52,32729,32828,0},
                {249,52,32728,32828,4,33631,32660,4},
                {250,52,32729,32828,4,33632,32660,4}
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

        List<ClassicWorldTeleportCatalog.Route> teleports =
                ClassicWorldTeleportCatalog.giranCastle();
        assertEquals(List.of(182, 183, 184, 185), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(List.of(
                        "teleport giranc-village", "teleport giranc-girandun",
                        "teleport giranc-dragonvalley", "teleport giranc-littlebb"),
                teleports.stream().map(ClassicWorldTeleportCatalog.Route::action).toList());
        assertEquals(List.of(4, 55, 4, 4), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        for (ClassicWorldTeleportCatalog.Route route : teleports) {
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    52, route.sourceObjectId(), route.action()));
            assertEquals(0, route.priceAmount());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
        }
        long teleporterObject = ClassicWorldNpcCatalog.objectId(77);
        assertEquals("telecgiran2", ClassicWorldTeleportCatalog.followUpHtml(
                52, teleporterObject, "teleportURL"));
        assertEquals(List.of("0", "0", "0", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        52, teleporterObject, "teleportURL"));
    }

    @Test
    void sixExteriorDoorsAndTwoNpcSkillsAreHashLocked() {
        assertEquals("25FF3C137D0E4131B74E5A0843697AA7B31186EAF8F33D93C55D0A7D1D905872",
                ClassicWorldGiranCastleCatalog.DOOR_SOURCE_SHA256);
        Map<Integer, ClassicWorldGiranCastleCatalog.Door> doors =
                ClassicWorldGiranCastleCatalog.doors().stream().collect(
                        Collectors.toMap(ClassicWorldGiranCastleCatalog.Door::doorId,
                                Function.identity()));
        assertEquals(6, doors.size());
        assertEquals(List.of(1336, 33632, 32660, 4, 0, 33631, 33632, 0, 70817),
                doorValues(doors.get(2030)));
        assertEquals(List.of(2745, 33631, 32734, 4, 0, 33630, 33633, 10000, 70800),
                doorValues(doors.get(2031)));
        assertEquals(List.of(2746, 33580, 32678, 4, 1, 32676, 32679, 10000, 70988),
                doorValues(doors.get(2032)));
        assertEquals(List.of(2744, 33631, 32702, 4, 0, 33631, 33634, 10000, 70990),
                doorValues(doors.get(2033)));
        assertEquals(List.of(1338, 33609, 32677, 4, 1, 32677, 32678, 3000, 70989),
                doorValues(doors.get(2034)));
        assertEquals(List.of(1338, 33656, 32677, 4, 1, 32677, 32678, 3000, 70991),
                doorValues(doors.get(2035)));

        ClassicWorldCastleDoorState state = new ClassicWorldCastleDoorState();
        assertEquals(6, state.snapshots().size());
        assertEquals(6, state.repairAll());
        assertTrue(state.snapshots().stream().allMatch(snapshot ->
                snapshot.currentHp() == snapshot.definition().maximumHp()));

        assertEquals("05B09D30D06D722E9387C70B75CC714386242BB03F79E2B0AD5F0279BFC7F484",
                ClassicWorldGiranCastleCatalog.SELECTED_SKILL_SOURCE_SHA256);
        assertEquals("4E6135F21D04587C1771312C102AAE7EC17402E87F71E2A719C5E0A1D7516EDD",
                ClassicWorldGiranCastleCatalog.SECONDARY_SKILL_SOURCE_SHA256);
        assertEquals(List.of(12, 1800, 19, 747, 2), List.of(
                ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_SKILL_ID,
                ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_DURATION_SECONDS,
                ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_ACTION_ID,
                ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_EFFECT_GFX,
                ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_DAMAGE));
        assertEquals(List.of(21, 1800, 19, 748, -3), List.of(
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID,
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_DURATION_SECONDS,
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_ACTION_ID,
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_EFFECT_GFX,
                ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_CLASS));
    }

    private static ClassicWorldNpcCatalog.StaticNpc row(
            List<ClassicWorldNpcCatalog.StaticNpc> rows, int spawnId) {
        return rows.stream().filter(row -> row.spawnId() == spawnId)
                .findFirst().orElseThrow();
    }

    private static List<Integer> doorValues(ClassicWorldGiranCastleCatalog.Door door) {
        return List.of(door.gfxId(), door.x(), door.y(), door.mapId(), door.direction(),
                door.edgeLow(), door.edgeHigh(), door.maximumHp(), door.keeperNpcId());
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
