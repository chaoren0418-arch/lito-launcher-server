package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GameHubVirtualPlayerTest {
    @Test void deRosterHonorsCapacityServerNamesBalanceAndAutomaticReadiness()
            throws Exception {
        LocalResources.assumeNativeCatalogPins();
        ServerConfig config = config();
        GameHub.RoomSettings settings = new GameHub.RoomSettings(
                "GM test", "", 8, GameHub.RoomSettings.EQUAL_SIDES,
                BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of());
        try (GameHub hub = new GameHub(config)) {
            ClientSession owner = session(config, hub);
            hub.connect(owner);
            hub.enterChannel(0, owner);
            long roomId = hub.createRoom(0, settings);
            assertTrue(hub.tryJoinRoom(roomId, owner).joined());

            GameHub.VirtualPlayerUpdate update = hub.addRoomVirtualPlayers(
                    roomId, owner, "太陽神", 20);

            assertTrue(update.accepted());
            assertEquals("capacity-limited", update.reason());
            assertEquals(7, update.added().size());
            assertEquals(8, hub.roomCombatantCount(roomId));
            assertTrue(update.added().stream().allMatch(player ->
                    player.name().endsWith("(太陽神)")
                            && player.name().getBytes(Protocol.LEGACY_TEXT).length <= 27));
            long force0 = update.added().stream().filter(player -> player.force() == 0).count();
            long force1 = update.added().stream().filter(player -> player.force() == 1).count();
            assertTrue(Math.abs(force0 - force1) <= 1);
            assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED,
                    hub.requestOwnerStart(roomId, owner).transition());
        }
    }

    @Test void zeroClearsVirtualRosterAndRealJoinUsesFreedSeat() throws Exception {
        ServerConfig config = config();
        try (GameHub hub = new GameHub(config)) {
            ClientSession owner = session(config, hub);
            ClientSession peer = session(config, hub);
            for (ClientSession player : List.of(owner, peer)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "GM test", "", 2, BattleRules.Mode.DEATHMATCH.wireId(),
                    0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, owner).joined());
            assertEquals(1, hub.addRoomVirtualPlayers(roomId, owner, "月亮神", 1)
                    .added().size());
            assertFalse(hub.tryJoinRoom(roomId, peer).joined());

            GameHub.VirtualPlayerUpdate cleared = hub.addRoomVirtualPlayers(
                    roomId, owner, "月亮神", 0);
            assertEquals(1, cleared.removed().size());
            assertEquals(0, hub.roomVirtualPlayers(roomId).size());
            assertTrue(hub.tryJoinRoom(roomId, peer).joined());
        }
    }

    @Test void siegeVirtualRosterSatisfiesNativeFifteenPerForceMinimum()
            throws Exception {
        LocalResources.assumeNativeCatalogPins();
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/"
                        + "server.pet-race-multiplayer-selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession owner = session(config, hub);
            hub.connect(owner);
            hub.enterChannel(0, owner);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Siege GM test", "", 100, GameHub.RoomSettings.EQUAL_SIDES,
                    BattleRules.Mode.SIEGE_WAR.wireId(), 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, owner).joined());
            assertEquals(29, hub.addRoomVirtualPlayers(
                    roomId, owner, "太陽神", 29).added().size());

            assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED,
                    hub.requestOwnerStart(roomId, owner).transition());
        }
    }

    @Test void databaseDumpsGrantTestGmAndRetainDatabaseCommandPolicy() throws Exception {
        for (String name : List.of("380_ac_1.sql", "380_ac_2.sql")) {
            String sql = Files.readString(Path.of("db", name));
            assertTrue(sql.contains("INSERT INTO `accounts` VALUES (200, 0, '3333', '3333'"),
                    name);
        }
        String staticSql = Files.readString(Path.of("db", "lito_static.sql"));
        assertTrue(staticSql.contains("CREATE TABLE `commands`"));
        assertTrue(staticSql.contains(
                "INSERT INTO `commands` VALUES ('de', 200, 'L1SpawnDe'"));
    }

    @Test void everyNativeBattleModeAndMapStartsWithDatabaseCommandVirtualRoster()
            throws Exception {
        LocalResources.assumeNativeCatalogPins();
        ServerConfig config = config();
        Method countdownWork = GameHub.class.getDeclaredMethod("countdownWork", long.class);
        countdownWork.setAccessible(true);
        int verifiedPairs = 0;
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int mapId : mode.maps()) {
                try (GameHub hub = new GameHub(config)) {
                    ClientSession owner = session(config, hub);
                    hub.connect(owner);
                    hub.enterChannel(0, owner);
                    BattleRules.RuleSet defaults = BattleRules.decode(new GameHub.RoomSettings(
                            "GM matrix", "", 100,
                            mode.teamBased() ? GameHub.RoomSettings.EQUAL_SIDES : 0,
                            mode.wireId(), 0, 0, 0, 0, List.of()));
                    int capacity = Math.max(2, defaults.minimumPlayers());
                    long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                            "GM matrix", "", capacity,
                            mode.teamBased() ? GameHub.RoomSettings.EQUAL_SIDES : 0,
                            mode.wireId(), 0, 0, 0, 0, List.of()));
                    assertTrue(hub.tryJoinRoom(roomId, owner).joined(), mode + "/" + mapId);
                    assertTrue(hub.updateMap(roomId, owner, mapId), mode + "/" + mapId);

                    int virtualCount = Math.max(1, defaults.minimumPlayers() - 1);
                    GameHub.VirtualPlayerUpdate update = hub.addRoomVirtualPlayers(
                            roomId, owner, "太陽神", virtualCount);
                    assertTrue(update.accepted(), mode + "/" + mapId);
                    assertEquals(virtualCount, update.added().size(), mode + "/" + mapId);
                    assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED,
                            hub.requestOwnerStart(roomId, owner).transition(), mode + "/" + mapId);

                    long future = System.nanoTime() + 10_000_000_000L;
                    countdownWork.invoke(hub, future);
                    countdownWork.invoke(hub, future + 2_000_000_000L);
                    countdownWork.invoke(hub, future + 4_000_000_000L);
                    assertTrue(hub.battleStarted(roomId), mode + "/" + mapId);
                    BattleRules.Snapshot snapshot = hub.matchSnapshot(roomId);
                    assertEquals(BattleRules.Phase.RUNNING, snapshot.phase(), mode + "/" + mapId);
                    assertEquals(mode, snapshot.rules().mode(), mode + "/" + mapId);
                    assertEquals(mapId, snapshot.rules().mapId(), mode + "/" + mapId);
                    assertEquals(virtualCount, hub.battleVirtualPlayers(roomId).size(),
                            mode + "/" + mapId);

                    Protocol.BattleBootstrap bootstrap = Protocol.battleBootstrap(
                            mapId, snapshot.rules().timeLimitSeconds());
                    assertEquals(Protocol.S_WORLD, bootstrap.world()[0] & 0xFF,
                            mode + "/" + mapId);
                    assertEquals(mapId, (bootstrap.world()[1] & 0xFF)
                            | ((bootstrap.world()[2] & 0xFF) << 8), mode + "/" + mapId);
                    assertEquals(Protocol.S_LITO_START_GAME, bootstrap.start()[0] & 0xFF,
                            mode + "/" + mapId);
                    ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
                    ArenaMapCatalog.Point playerSpawn = hub.battleSpawn(roomId, 0);
                    assertTrue(arena.nativeStandingFloor(
                            playerSpawn.x(), playerSpawn.y()), mode + "/" + mapId);
                    verifiedPairs++;
                }
            }
        }
        assertEquals(40, verifiedPairs);
    }

    private static ServerConfig config() throws Exception {
        return ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
    }

    private static ClientSession session(ServerConfig config, GameHub hub) {
        return new ClientSession(null, ClientSession.Endpoint.GAME,
                config, null, null, LocalResources.discardCapture(), hub);
    }
}
