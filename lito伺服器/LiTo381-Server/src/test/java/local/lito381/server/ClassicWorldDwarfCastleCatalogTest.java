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

final class ClassicWorldDwarfCastleCatalogTest {
    @Test
    void map66PinsSelectedCollisionAndAllFourPackedResources() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldDwarfCastleCatalog.MAP_ID);
        assertEquals(32704, map.startX());
        assertEquals(32895, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32959, map.endY());
        assertEquals(192, map.width());
        assertEquals(192, map.height());
        assertEquals(ClassicWorldDwarfCastleCatalog.MAP_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());
        assertResource("/maps/66.l38.gz",
                ClassicWorldDwarfCastleCatalog.MAP_RESOURCE_SHA256);
        assertResource("/world/map66-static-npcs.lwn.gz",
                ClassicWorldDwarfCastleCatalog.NPC_RESOURCE_SHA256);
        assertResource("/world/map66-monsters.lwm.gz",
                ClassicWorldDwarfCastleCatalog.MONSTER_RESOURCE_SHA256);
        assertResource("/world/map66-drops.lwd.gz",
                ClassicWorldDwarfCastleCatalog.DROP_RESOURCE_SHA256);
        assertTrue(ClassicWorldMonsterCatalog.definitions(66).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(66));
        assertTrue(ClassicWorldDropCatalog.items(66).isEmpty());
    }

    @Test
    void allThirteenSelectedNpcsKeepTheirNativeRoles() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows =
                ClassicWorldNpcCatalog.all(66);
        assertEquals(List.of(80, 275, 731, 733, 738, 739,
                        926, 927, 928, 929, 930, 931, 1007),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals(List.of("telecdwarf1", "storage", "encwc", "encac",
                        "potempin1", "freckson1", "gatekeeperop",
                        "gatekeeperop", "gatekeeperop", "gatekeeperop",
                        "gatekeeperop", "gatekeeperop", ""),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::normalAction).toList());
        assertTrue(row(rows, 275).fixedWarehouse());
        assertTrue(ClassicWorldDwarfCastleCatalog.isWeaponEnchanter(row(rows, 731)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isArmorEnchanter(row(rows, 733)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isSteward(row(rows, 738)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isCommander(row(rows, 739)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isOuterGuard(row(rows, 926)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isOuterGuard(row(rows, 928)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isInnerGuard(row(rows, 930)));
        assertTrue(ClassicWorldDwarfCastleCatalog.isTower(row(rows, 1007)));
    }

    @Test
    void groundTeleportsGetbackAndNativeTextMismatchAreExplicit() {
        int[][] expectedGround = {
                {315,4,33631,32369,66,32738,32868,2},
                {316,4,33675,32409,66,32810,32934,2},
                {317,66,32737,32868,4,33630,32369,2},
                {318,66,32810,32935,4,33675,32410,2}
        };
        for (int[] expected : expectedGround) {
            ClassicWorldDungeonCatalog.Route route = ClassicWorldDungeonCatalog.find(
                    expected[1], expected[2], expected[3]);
            assertNotNull(route);
            assertEquals(expected[0], route.routeId());
            assertEquals(expected[4], route.destinationMapId());
            assertEquals(expected[5], route.destinationX());
            assertEquals(expected[6], route.destinationY());
            assertEquals(expected[7], route.destinationHeading());
            assertTrue(ClassicWorldDungeonCatalog.destinationLandingSafe(route));
            assertTrue(ClassicWorldDungeonCatalog.isDwarfCastleCaveRoute(route));
        }

        List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.dwarfCastle();
        assertEquals(List.of(346, 347, 348), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(List.of(4, 79, 4), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        long teleporter = ClassicWorldNpcCatalog.objectId(80);
        assertEquals("telecdwarf2", ClassicWorldTeleportCatalog.followUpHtml(
                66, teleporter, "teleportURL"));
        assertEquals(List.of("0", "0", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        66, teleporter, "teleportURL"));
        assertNull(ClassicWorldTeleportCatalog.route(66, teleporter,
                ClassicWorldDwarfCastleCatalog.TEXT_ONLY_GRAVE_ACTION));
        assertNotNull(ClassicWorldTeleportCatalog.route(66, teleporter,
                ClassicWorldDwarfCastleCatalog.DATABASE_CRYSTAL_ACTION));

        ClassicWorldGetbackCatalog.Destination getback =
                ClassicWorldGetbackCatalog.resolve(66, 32800, 32800, 0, 0);
        assertEquals(4, getback.mapId());
        assertEquals(33702, getback.x());
        assertEquals(32492, getback.y());
        assertEquals(9, getback.townId());
    }

    @Test
    void threeClosedDoorsExposeExactKeeperEdgesAndNativePackets() {
        List<ClassicWorldDwarfCastleCatalog.Door> doors =
                ClassicWorldDwarfCastleCatalog.doors();
        assertEquals(3, doors.size());
        assertEquals(List.of(2050, 2051, 2052),
                doors.stream().map(ClassicWorldDwarfCastleCatalog.Door::doorId).toList());
        assertEquals(List.of(443, 1826, 1827),
                doors.stream().map(ClassicWorldDwarfCastleCatalog.Door::gfxId).toList());
        assertEquals(List.of(70995, 70994, 70993),
                doors.stream().map(ClassicWorldDwarfCastleCatalog.Door::keeperNpcId).toList());

        ClassicWorldDwarfCastleDoorState state =
                new ClassicWorldDwarfCastleDoorState();
        assertTrue(state.snapshots().stream().noneMatch(
                ClassicWorldDwarfCastleDoorState.Snapshot::open));
        assertTrue(state.blocks(32844, 32812));
        assertEquals(1, state.setOpenForKeeper(70995, true).size());
        assertFalse(state.blocks(32844, 32812));
        assertEquals(1, state.repairAll());
        assertTrue(state.blocks(32844, 32812));

        ClassicWorldDwarfCastleCatalog.Door door = doors.get(0);
        Protocol.Reader packet = new Protocol.Reader(Protocol.classicCastleDoor(
                door.objectId(), door.x(), door.y(), door.gfxId(),
                BattleDoor.ACTION_CLOSED));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(door.x(), packet.h());
        assertEquals(door.y(), packet.h());
        assertEquals(door.objectId(), packet.d());
        assertEquals(door.gfxId(), packet.h());
        assertEquals(BattleDoor.ACTION_CLOSED, packet.c());
    }

    @Test
    void destructibleOuterGateUsesNativeSixthsDeathAndNearestEntranceCell() {
        ClassicWorldDwarfCastleDoorState state =
                new ClassicWorldDwarfCastleDoorState();
        ClassicWorldDwarfCastleCatalog.Door door =
                ClassicWorldDwarfCastleCatalog.doors().get(1);
        long objectId = door.objectId();

        ClassicWorldMonsterPopulation.Snapshot combat = state.combatSnapshot(
                door.mapId(), objectId, 32778, 32859);
        assertNotNull(combat);
        assertEquals(32779, combat.x());
        assertEquals(32859, combat.y());
        assertNull(state.combatSnapshot(door.mapId(),
                ClassicWorldDwarfCastleCatalog.doors().get(0).objectId(),
                32843, 32812));

        // Native comparisons are strict: equality at 5/6 is not yet cracked.
        ClassicWorldDwarfCastleDoorState.DoorHit equality =
                state.damage(door.mapId(), objectId, 1667);
        assertEquals(8333, equality.after().currentHp());
        assertEquals(0, equality.action());

        int[] damages = {1, 1667, 1666, 1667, 1667, 1665};
        int[] actions = {
            BattleActionCodes.DOOR_CRACK_1,
            BattleActionCodes.DOOR_CRACK_2,
            BattleActionCodes.DOOR_CRACK_3,
            BattleActionCodes.DOOR_CRACK_4,
            BattleActionCodes.DOOR_CRACK_5,
            BattleActionCodes.DOOR_DIE
        };
        ClassicWorldDwarfCastleDoorState.DoorHit hit = null;
        for (int index = 0; index < damages.length; index++) {
            hit = state.damage(door.mapId(), objectId, damages[index]);
            assertNotNull(hit);
            assertEquals(actions[index], hit.action());
        }
        assertNotNull(hit);
        assertTrue(hit.diedNow());
        assertTrue(hit.after().open());
        assertEquals(0, hit.after().currentHp());
        assertFalse(state.blocks(door.entranceX(), door.edgeLow()));
        assertNull(state.combatSnapshot(
                door.mapId(), objectId, 32778, 32859));

        assertEquals(1, state.repairAll());
        ClassicWorldDwarfCastleDoorState.Snapshot repaired =
                state.snapshot(door.mapId(), objectId);
        assertNotNull(repaired);
        assertEquals(10_000, repaired.currentHp());
        assertFalse(repaired.open());
        assertEquals(0, repaired.crackStage());
        assertTrue(state.blocks(door.entranceX(), door.edgeLow()));
    }

    private static ClassicWorldNpcCatalog.StaticNpc row(
            List<ClassicWorldNpcCatalog.StaticNpc> rows, int spawnId) {
        return rows.stream().filter(row -> row.spawnId() == spawnId)
                .findFirst().orElseThrow();
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
