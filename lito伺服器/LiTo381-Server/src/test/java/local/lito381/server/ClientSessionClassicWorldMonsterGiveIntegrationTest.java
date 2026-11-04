package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionClassicWorldMonsterGiveIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void worldCGiveCommitsPlayerDebitAndMonsterOnGetItemLifecycle() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("monster-give-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("capture"))) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state, log, hub);
            ClassicWorldMonsterPopulation population =
                    (ClassicWorldMonsterPopulation) field(hub, "classicWorldMonsters");
            Target target = target(population);
            initialize(session, target.playerX(), target.playerY());
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, 21, target.playerX(), target.playerY(), 2);

            RuntimeInventory before = (RuntimeInventory) field(session, "inventory");
            RuntimeInventory.Item item = before.items().get(0);
            invokeGive(session, new Protocol.Writer().c(Protocol.C_GIVE)
                    .d(target.monster().objectId())
                    .h(target.monster().x()).h(target.monster().y())
                    .d(item.objectId()).d(7).bytes());

            RuntimeInventory after = (RuntimeInventory) field(session, "inventory");
            assertTrue(after.items().isEmpty());
            List<RuntimeInventory.Item> carried = population.carriedItems(
                    21, target.monster().objectId());
            assertEquals(1, carried.size());
            assertEquals(40508, carried.get(0).definition().catalogId());
            assertEquals(7, carried.get(0).quantity());
            assertEquals(30, population.digestRemainingSeconds(
                    21, target.monster().objectId(), carried.get(0).objectId()));
            assertTrue(logContains(log, "S_REMOVE_INVENTORY"));
        } finally {
            hub.close();
        }
    }

    @Test
    void worldCGiveRoutesRetainedStaticTeleporterToInstanceScopedNpcBag()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("static-npc-give-state.properties"), false);
        GameHub hub = new GameHub(config);
        StaticTarget target = staticTarget("L1Teleporter");
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("static-npc-capture"))) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state, log, hub);
            initialize(session, target.playerX(), target.playerY(), target.npc().mapId());
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, target.npc().mapId(),
                    target.playerX(), target.playerY(), 2);

            RuntimeInventory before = (RuntimeInventory) field(session, "inventory");
            RuntimeInventory.Item item = before.items().get(0);
            invokeGive(session, new Protocol.Writer().c(Protocol.C_GIVE)
                    .d(target.npc().objectId())
                    .h(target.npc().x()).h(target.npc().y())
                    .d(item.objectId()).d(7).bytes());

            RuntimeInventory after = (RuntimeInventory) field(session, "inventory");
            assertTrue(after.items().isEmpty());
            List<RuntimeInventory.Item> carried =
                    hub.classicWorldStaticNpcCarriedItems(
                            target.npc().mapId(), -1, target.npc().objectId());
            assertEquals(1, carried.size());
            assertEquals(40508, carried.get(0).definition().catalogId());
            assertEquals(7, carried.get(0).quantity());
            assertTrue(hub.classicWorldStaticNpcCarriedItems(
                    target.npc().mapId(), 72, target.npc().objectId()).isEmpty());
            assertTrue(logContains(log, "S_REMOVE_INVENTORY"));
        } finally {
            hub.close();
        }
    }

    @Test
    void worldCGiveAllowsLitLanternAndPublishesTargetThenPlayerLight()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("static-light-give-state.properties"), false);
        GameHub hub = new GameHub(config);
        StaticTarget target = staticTarget("L1Teleporter");
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("static-light-capture"))) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state, log, hub);
            initialize(session, target.playerX(), target.playerY(), target.npc().mapId());
            setField(session, "account", "test");
            RuntimeInventory player = RuntimeInventory.fromCatalogCounts(
                    Map.of(InventoryCatalog.MAGIC_LANTERN, 1));
            long objectId = player.items().get(0).objectId();
            player.toggleLight(objectId);
            setField(session, "inventory", player);
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, target.npc().mapId(),
                    target.playerX(), target.playerY(), 2);

            invokeGive(session, new Protocol.Writer().c(Protocol.C_GIVE)
                    .d(target.npc().objectId())
                    .h(target.npc().x()).h(target.npc().y())
                    .d(objectId).d(1).bytes());

            assertTrue(((RuntimeInventory) field(session, "inventory"))
                    .items().isEmpty());
            RuntimeInventory.Item carried = hub.classicWorldStaticNpcCarriedItems(
                    target.npc().mapId(), -1, target.npc().objectId()).get(0);
            assertTrue(carried.equipped());
            assertEquals(InventoryCatalog.MAGIC_LANTERN,
                    carried.definition().catalogId());
            String capture = java.nio.file.Files.readString(log.path());
            String targetLight = "S_CHANGE_LIGHT object=" + target.npc().objectId()
                    + " range=22 reason=C_GIVE-target";
            String playerLight = "S_CHANGE_LIGHT object="
                    + Long.toUnsignedString(0x3810_2778L)
                    + " range=0 reason=C_GIVE-player";
            assertTrue(capture.contains(targetLight));
            assertTrue(capture.contains(playerLight));
            assertTrue(capture.indexOf(targetLight) < capture.indexOf(playerLight));
        } finally {
            hub.close();
        }
    }

    @Test
    void worldCDropAndCGetKeepLitLanternStateAndRecomputePlayerLight()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("ground-light-state.properties"), false);
        GameHub hub = new GameHub(config);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("ground-light-capture"))) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state, log, hub);
            initialize(session, x, y, 4);
            setField(session, "account", "test");
            RuntimeInventory player = RuntimeInventory.fromCatalogCounts(
                    Map.of(InventoryCatalog.MAGIC_LANTERN, 1));
            long inventoryObjectId = player.items().get(0).objectId();
            player.toggleLight(inventoryObjectId);
            setField(session, "inventory", player);
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            invokeDrop(session, new Protocol.Writer().c(Protocol.C_DROP)
                    .h(x).h(y).d(inventoryObjectId).d(1).bytes());

            RuntimeInventory afterDrop = (RuntimeInventory) field(session, "inventory");
            assertTrue(afterDrop.items().isEmpty());
            ClassicWorldGroundItems.Item dropped = hub.classicWorldGroundItemsInScreen(
                    4, -1, x, y).stream()
                    .filter(candidate -> candidate.definition().catalogId()
                            == InventoryCatalog.MAGIC_LANTERN)
                    .findFirst().orElseThrow();
            assertTrue(dropped.state().equipped());
            assertTrue(logContains(log,
                    "range=0 reason=C_DROP-player"));

            invokeGet(session, new Protocol.Writer().c(Protocol.C_GET)
                    .h(Protocol.groundPickupWireX(x)).h(y)
                    .d(dropped.objectId()).d(1).c(0).bytes());

            RuntimeInventory afterPickup = (RuntimeInventory) field(session, "inventory");
            assertEquals(1, afterPickup.items().size());
            assertTrue(afterPickup.items().get(0).equipped());
            assertEquals(0x16, afterPickup.activeLightRange());
            assertTrue(logContains(log,
                    "range=22 reason=C_GET-player"));
            assertTrue(logContains(log,
                    "S_MESSAGE_CODE classic-world-pickup=403"));
            assertTrue(logContains(log,
                    "S_ACTION classic-world-pickup"));
        } finally {
            hub.close();
        }
    }

    @Test
    void worldCGiveToPetPublishesImmediateNativeHealingUse() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("pet-heal-give-state.properties"), false);
        GameHub hub = new GameHub(config);
        ClassicWorldMonsterPopulation population =
                (ClassicWorldMonsterPopulation) field(hub, "classicWorldMonsters");
        Target fixture = target(population, 4, 45034);
        int playerX = fixture.playerX();
        int playerY = fixture.playerY();
        int petX = fixture.monster().x();
        int petY = fixture.monster().y();
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("pet-heal-capture"))) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state, log, hub);
            initialize(session, playerX, playerY, 4);
            setField(session, "account", "test");
            setField(session, "inventory",
                    RuntimeInventory.fromCatalogCounts(Map.of(40010L, 1)));
            BattleCompanion pet = BattleCompanion.tamed(
                    0x6000_0781L, 45207, 2158, "$1791", 26,
                    100, 200, 20, -20, 17, 17, petX, petY);
            assertTrue(pet.receiveItem(sourceItem(40012), 1));
            @SuppressWarnings("unchecked")
            List<BattleCompanion> companions =
                    (List<BattleCompanion>) field(session, "tamedCompanions");
            companions.add(pet);
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, playerX, playerY, 2);

            RuntimeInventory.Item potion = ((RuntimeInventory) field(
                    session, "inventory")).items().get(0);
            invokeGive(session, new Protocol.Writer().c(Protocol.C_GIVE)
                    .d(pet.snapshot().objectId()).h(petX).h(petY)
                    .d(potion.objectId()).d(1).bytes());

            assertEquals(175, pet.snapshot().hp());
            assertEquals(1, pet.carriedItems().size());
            assertEquals(40010, pet.carriedItems().get(0)
                    .definition().catalogId());
            assertTrue(logContains(log, "S_EFFECT companion-onGetItem="
                    + pet.snapshot().objectId() + " gfx=197 kind=HEAL"));
            assertTrue(logContains(log, "S_HIT_RATIO companion-onGetItem="
                    + pet.snapshot().objectId() + " hp=175/200"));
            assertTrue(logContains(log, "reason=C_GIVE-target"));
            assertTrue(logContains(log, "reason=C_GIVE-player"));
        } finally {
            hub.close();
        }
    }

    @Test
    void worldCGiveRoutesPromotedLiveGuardToMovingNpcBag() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("guard-give-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("guard-capture"))) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state, log, hub);
            ClassicWorldMonsterPopulation population =
                    (ClassicWorldMonsterPopulation) field(hub, "classicWorldMonsters");
            Target target = movingGuardTarget(population);
            initialize(session, target.playerX(), target.playerY(), 4);
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, target.playerX(), target.playerY(), 2);

            RuntimeInventory before = (RuntimeInventory) field(session, "inventory");
            RuntimeInventory.Item item = before.items().get(0);
            invokeGive(session, new Protocol.Writer().c(Protocol.C_GIVE)
                    .d(target.monster().objectId())
                    .h(target.monster().x()).h(target.monster().y())
                    .d(item.objectId()).d(7).bytes());

            assertTrue(((RuntimeInventory) field(session, "inventory")).items().isEmpty());
            List<RuntimeInventory.Item> carried = population.carriedItems(
                    4, target.monster().objectId());
            assertEquals(1, carried.size());
            assertEquals(40508, carried.get(0).definition().catalogId());
            assertEquals(7, carried.get(0).quantity());
            assertTrue(logContains(log, "S_REMOVE_INVENTORY"));
        } finally {
            hub.close();
        }
    }

    @Test
    void worldCGiveTamingFoodCreatesDurablePetCollarAndReplacesMonster()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("monster-taming-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("taming-capture"));
             MemoryAccountRepository accounts =
                     new MemoryAccountRepository("test", "test")) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config,
                    accounts, state, log, hub);
            ClassicWorldMonsterPopulation population =
                    (ClassicWorldMonsterPopulation) field(hub, "classicWorldMonsters");
            Target target = target(population, 4, 45034);
            initialize(session, target.playerX(), target.playerY(), 4);
            setField(session, "classicPetTamingRollOverride", 0);
            setField(session, "outgoing", new Protocol.CipherState(0x2778));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, target.playerX(), target.playerY(), 2);
            population.damage(4, target.monster().objectId(),
                    target.monster().currentHp() - 1);

            RuntimeInventory before = (RuntimeInventory) field(session, "inventory");
            setField(session, "inventory",
                    RuntimeInventory.fromCatalogCounts(Map.of(40057L, 1)));
            RuntimeInventory baitInventory =
                    (RuntimeInventory) field(session, "inventory");
            RuntimeInventory.Item bait = baitInventory.items().get(0);
            invokeGive(session, new Protocol.Writer().c(Protocol.C_GIVE)
                    .d(target.monster().objectId())
                    .h(target.monster().x()).h(target.monster().y())
                    .d(bait.objectId()).d(1).bytes());

            RuntimeInventory after = (RuntimeInventory) field(session, "inventory");
            assertEquals(1, after.items().size());
            RuntimeInventory.Item collar = after.items().get(0);
            assertEquals(InventoryCatalog.PET_COLLAR,
                    collar.definition().catalogId());
            GameStateStore.PetState durable = state.petForCollar(collar.objectId());
            assertNotNull(durable);
            assertEquals(45034, durable.npcId());
            @SuppressWarnings("unchecked")
            List<BattleCompanion> companions =
                    (List<BattleCompanion>) field(session, "tamedCompanions");
            assertEquals(1, companions.size());
            BattleCompanion.Snapshot pet = companions.get(0).snapshot();
            assertEquals(BattleCompanion.Kind.PET, pet.kind());
            assertEquals(durable.objectId(), pet.objectId());
            assertEquals(1, pet.hp());
            assertEquals(1, companions.get(0).carriedItems().size());
            assertEquals(40057, companions.get(0).carriedItems().get(0)
                    .definition().catalogId());
            ClassicWorldMonsterPopulation.Snapshot original =
                    population.actor(4, target.monster().objectId());
            assertNotNull(original);
            assertFalse(original.present());
            assertFalse(original.alive());
            assertTrue(logContains(log, "reason=item-tamed"));
            assertTrue(logContains(log, "S_PUT_OBJECT persistent-pet="));
            assertTrue(before.items().size() > 0);
        } finally {
            hub.close();
        }
    }

    private static Target target(ClassicWorldMonsterPopulation population) {
        return target(population, 21, 45032);
    }

    private static Target target(
            ClassicWorldMonsterPopulation population, int mapId, int npcId) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        for (ClassicWorldMonsterPopulation.Snapshot monster
                : population.withinTileDistance(mapId, 1, 1, Integer.MAX_VALUE)) {
            if (monster.npcId() != npcId || !monster.alive() || !monster.present()) continue;
            for (int direction = 0; direction < 8; direction++) {
                int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
                int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
                int x = monster.x() + dx[direction];
                int y = monster.y() + dy[direction];
                if (map.contains(x, y) && map.standingPassable(x, y)
                        && !population.occupies(mapId, x, y)
                        && !ClassicWorldNpcCatalog.occupies(mapId, x, y)) {
                    return new Target(monster, x, y);
                }
            }
        }
        throw new AssertionError("no adjacent selected NPC" + npcId
                + " C_GIVE fixture on map " + mapId);
    }

    private static Target movingGuardTarget(
            ClassicWorldMonsterPopulation population) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(4);
        for (ClassicWorldMonsterPopulation.Snapshot actor
                : population.withinTileDistance(4, 1, 1, Integer.MAX_VALUE)) {
            if (!ClassicWorldGuardCatalog.isGuard(actor)
                    || !actor.alive() || !actor.present()) continue;
            int[] adjacent = adjacent(map, population, 4, actor.x(), actor.y());
            if (adjacent != null) return new Target(actor, adjacent[0], adjacent[1]);
        }
        throw new AssertionError("no adjacent promoted L1Guard C_GIVE fixture on map 4");
    }

    private static StaticTarget staticTarget(String implementation) {
        for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            for (ClassicWorldNpcCatalog.StaticNpc npc
                    : ClassicWorldNpcCatalog.all(mapId)) {
                if (!npc.implementation().equals(implementation)
                        || !ClassicWorldStaticNpcInventories.staticReceiver(npc)) continue;
                int[] adjacent = adjacent(map, null, mapId, npc.x(), npc.y());
                if (adjacent != null) return new StaticTarget(npc, adjacent[0], adjacent[1]);
            }
        }
        throw new AssertionError("no adjacent static " + implementation
                + " C_GIVE fixture");
    }

    private static int[] adjacent(
            ClassicWorldMapCatalog.ClassicMap map,
            ClassicWorldMonsterPopulation population,
            int mapId, int targetX, int targetY) {
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int direction = 0; direction < 8; direction++) {
            int x = targetX + dx[direction];
            int y = targetY + dy[direction];
            if (map.contains(x, y) && map.standingPassable(x, y)
                    && (population == null || !population.occupies(mapId, x, y))
                    && !ClassicWorldNpcCatalog.occupies(mapId, x, y)) {
                return new int[] {x, y};
            }
        }
        return null;
    }

    private static void initialize(ClientSession session, int x, int y) throws Exception {
        initialize(session, x, y, 21);
    }

    private static void initialize(
            ClientSession session, int x, int y, int mapId) throws Exception {
        CharacterSummary character = new CharacterSummary(
                "MonsterGive", "", 3, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 19, 12, 18, 12,
                0x3810_2778L, 100, 0, 0, 0, 0, 0, x, y, mapId,
                12, 40, 1, 12, 12, 0);
        setField(session, "account", "monster-give-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(Map.of(40508L, 7)));
        setField(session, "learnedSkills", Set.of());
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance", new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance", SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setPhase(session, "WORLD");
        setField(session, "currentRoomId", 0L);
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void invokeGive(ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "giveInventoryItem", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static void invokeDrop(ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "dropInventoryItem", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static void invokeGet(ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "getGroundItem", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static boolean logContains(CaptureLog log, String text) throws Exception {
        return java.nio.file.Files.readString(log.path()).contains(text);
    }

    private static RuntimeInventory.Item sourceItem(long catalogId) {
        InventoryCatalog.Definition definition =
                ClassicWorldDropCatalog.inventoryDefinition((int) catalogId);
        if (definition == null) definition = InventoryCatalog.builtIn(catalogId);
        if (definition == null) {
            throw new IllegalArgumentException("missing source item " + catalogId);
        }
        RuntimeInventory source = new RuntimeInventory();
        assertTrue(source.add(definition, 1));
        return source.items().get(0);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }

    private record Target(
            ClassicWorldMonsterPopulation.Snapshot monster, int playerX, int playerY) {}

    private record StaticTarget(
            ClassicWorldNpcCatalog.StaticNpc npc, int playerX, int playerY) {}
}
