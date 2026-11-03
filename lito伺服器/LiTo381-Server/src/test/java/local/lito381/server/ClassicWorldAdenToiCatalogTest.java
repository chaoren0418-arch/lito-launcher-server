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
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class ClassicWorldAdenToiCatalogTest {
    @Test
    void allTwelveMapsAndPackedResourcesMatchSelectedSource() throws Exception {
        assertEquals(List.of(101, 102, 103, 104, 105, 106,
                        107, 108, 109, 110, 300, 301),
                ClassicWorldAdenToiCatalog.MAP_IDS);
        for (int mapId : ClassicWorldAdenToiCatalog.MAP_IDS) {
            ClassicWorldAdenToiCatalog.MapFlags flags =
                    ClassicWorldAdenToiCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(flags.startX(), map.startX());
            assertEquals(flags.endX(), map.endX());
            assertEquals(flags.startY(), map.startY());
            assertEquals(flags.endY(), map.endY());
            assertEquals(ClassicWorldAdenToiCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.allowsRandomTeleport());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldAdenToiCatalog.MAP_RESOURCE_SHA256.get(mapId));

            ClassicWorldAdenToiCatalog.ResourceContract npc =
                    ClassicWorldAdenToiCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldAdenToiCatalog.ResourceContract monster =
                    ClassicWorldAdenToiCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldAdenToiCatalog.ResourceContract drop =
                    ClassicWorldAdenToiCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npc.rows(), ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(),
                    ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(monster.secondaryCount(), population(mapId));
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(drop.secondaryCount(),
                    ClassicWorldDropCatalog.items(mapId).size());
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    npc.resourceSha256());
            assertResource("/world/map" + mapId + "-monsters.lwm.gz",
                    monster.resourceSha256());
            assertResource("/world/map" + mapId + "-drops.lwd.gz",
                    drop.resourceSha256());
        }
    }

    @Test
    void adenInteriorActorsAndMainlandGatekeeperKeepNativeRoles() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows =
                ClassicWorldNpcCatalog.all(300);
        assertEquals(List.of(84, 276, 743, 746, 747, 751),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals(List.of("telecaden1", "storage", "encwc", "encac",
                        "biust1", "timon1"),
                rows.stream().map(ClassicWorldNpcCatalog.StaticNpc::normalAction).toList());
        assertTrue(row(rows, 276).fixedWarehouse());
        assertTrue(ClassicWorldAdenToiCatalog.isWeaponEnchanter(row(rows, 743)));
        assertTrue(ClassicWorldAdenToiCatalog.isArmorEnchanter(row(rows, 746)));
        assertTrue(ClassicWorldAdenToiCatalog.isCommander(row(rows, 747)));
        assertTrue(ClassicWorldAdenToiCatalog.isSteward(row(rows, 751)));

        ClassicWorldNpcCatalog.StaticNpc keeper = ClassicWorldNpcCatalog.npc(
                4, ClassicWorldNpcCatalog.objectId(932));
        assertNotNull(keeper);
        assertTrue(ClassicWorldAdenToiCatalog.isGatekeeper(keeper));
        assertEquals(70996, keeper.npcId());
        assertEquals("gatekeeperop", keeper.normalAction());
    }

    @Test
    void allFiftyThreeGroundRoutesAreAppendedAndLandSafely() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().subList(318, 371);
        assertEquals(IntStream.rangeClosed(319, 371).boxed().toList(),
                routes.stream().map(
                        ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::isAdenToiRoute));
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));

        ClassicWorldDungeonCatalog.Route entry =
                ClassicWorldDungeonCatalog.find(4, 34090, 33203);
        assertNotNull(entry);
        assertEquals(List.of(319, 300, 32891, 32580, 0), List.of(
                entry.routeId(), entry.destinationMapId(), entry.destinationX(),
                entry.destinationY(), entry.destinationHeading()));
        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(301, 32678, 32789);
        assertNotNull(exit);
        assertEquals(List.of(371, 4, 34150, 33382, 6), List.of(
                exit.routeId(), exit.destinationMapId(), exit.destinationX(),
                exit.destinationY(), exit.destinationHeading()));
    }

    @Test
    void nativeNpcTeleportsAndGetbackPreserveDatabaseBoundaries() {
        List<ClassicWorldTeleportCatalog.Route> teleports =
                ClassicWorldTeleportCatalog.adenCastle();
        assertEquals(List.of(510, 511, 512), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(List.of(4, 101, 4), teleports.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        assertEquals(List.of("teleport adenc-village",
                        "teleport adenc-omantower", "teleport adenc-gypsy"),
                teleports.stream().map(
                        ClassicWorldTeleportCatalog.Route::action).toList());
        long teleporter = ClassicWorldNpcCatalog.objectId(84);
        assertEquals("telecaden2", ClassicWorldTeleportCatalog.followUpHtml(
                300, teleporter, "teleportURL"));
        assertEquals(List.of("0", "0", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        300, teleporter, "teleportURL"));
        assertNull(ClassicWorldTeleportCatalog.route(
                300, teleporter, "teleportURL"));

        for (int mapId : ClassicWorldAdenToiCatalog.TOI_MAP_IDS) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(
                            mapId, 32768, 32768, 0, 0);
            assertEquals(List.of(4, 33915, 33114, 12), List.of(
                    destination.mapId(), destination.x(), destination.y(),
                    destination.townId()));
        }
        // The selected source has no getback row for map300 or map301.
        for (int mapId : List.of(300, 301)) {
            ClassicWorldGetbackCatalog.Destination fallback =
                    ClassicWorldGetbackCatalog.resolve(
                            mapId, 32800, 32800, 0, 0);
            assertEquals(List.of(4, 33089, 33397, 2), List.of(
                    fallback.mapId(), fallback.x(), fallback.y(),
                    fallback.townId()));
        }
    }

    @Test
    void populationAppendsSixHundredTwentyFiveActorsAfterRelease341() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION
                    - ClassicWorldMonsterCatalog.DEMON_TEMPLE_POPULATION
                    - ClassicWorldMonsterCatalog.RASTABAD_OUM_POPULATION
                    - ClassicWorldMonsterCatalog.SILENT_CAVERN_POPULATION
                    - ClassicWorldMonsterCatalog.HEINE_EVA_POPULATION
                    - ClassicWorldMonsterCatalog.ADEN_TOI_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldAdenToiCatalog.MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            population.actor(mapId, objectId++);
                    assertNotNull(actor, "map=" + mapId + " spawn="
                            + definition.spawnId() + " slot=" + slot);
                    assertEquals(definition.spawnId(), actor.spawnId());
                    assertEquals(slot, actor.spawnNumber());
                    assertTrue(ClassicWorldMapCatalog.forMap(mapId)
                            .standingPassable(actor.x(), actor.y()));
                    observed++;
                }
            }
        }
        assertEquals(625, observed);
    }

    @Test
    void adenInnerGateIsClosedNonDestructibleAndKeeperControlled() {
        ClassicWorldAdenToiCatalog.Door door =
                ClassicWorldAdenToiCatalog.innerGate();
        assertEquals(List.of(2060, 1336, 34091, 33203, 4, 0,
                        34090, 34091, 0, 70996),
                List.of(door.doorId(), door.gfxId(), door.x(), door.y(),
                        door.mapId(), door.direction(), door.edgeLow(),
                        door.edgeHigh(), door.maximumHp(), door.keeperNpcId()));
        ClassicWorldAdenCastleDoorState state =
                new ClassicWorldAdenCastleDoorState();
        assertFalse(state.snapshots().get(0).open());
        assertFalse(state.snapshots().get(0).destructible());
        assertTrue(state.blocks(34090, 33204));
        assertTrue(state.setOpenForKeeper(70995, true).isEmpty());
        assertEquals(1, state.setOpenForKeeper(70996, true).size());
        assertFalse(state.blocks(34090, 33204));
        assertEquals(1, state.repairAll());
        assertTrue(state.blocks(34091, 33204));

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

    private static int population(int mapId) {
        return ClassicWorldMonsterCatalog.definitions(mapId).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
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
