package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #138 B-land (主控裁定第 4 點): replaces the text audit
 * {@code WaterDragonSqlFirstPersistenceSourceAuditTest#rentalPublishesOnlyAfterRouteQualifiedSqlCommit}.
 * Protected behavior, driven through the real Water Dragon gate dialog on the CI self-test
 * profile with an SQL-authoritative private inventory:
 * <ul>
 *   <li>the pass lease is committed to the selected source repository (with a pass change)
 *       before anything is published — the visible inventory still lacks the pass while the
 *       SQL call runs;</li>
 *   <li>a refused SQL commit cancels the hub room reservation and leaves inventory and Adena
 *       untouched.</li>
 * </ul>
 * The repository-side lock group itself is exercised against MySQL by
 * {@code LitoMySqlWaterDragonPassIntegrationTest}.
 */
final class WaterDragonSqlFirstPersistenceBehaviorTest {
    private static final Path CONFIG = Path.of(
            "src/test/resources/server-config/config/server.selftest.properties");

    @TempDir Path temporary;

    @Test
    void acceptedSqlCommitPrecedesPublishingThePass() throws Exception {
        Result result = rent(RepositoryPersistenceResult.SUCCESS);
        assertEquals(null, result.failure());
        assertEquals(1, result.persistCalls());
        assertTrue(result.passChangePresent());
        assertEquals(0, result.passesDuringCommit(), "pass must not be visible before SQL commit");
        assertEquals(100_000, result.adenaDuringCommit());
        assertEquals(1, result.roomsDuringCommit(), "room is reserved before the SQL commit");
        assertEquals(1, result.passesAfter());
        assertEquals(90_400, result.adenaAfter());
        assertEquals(1, result.roomsAfter());
        assertTrue(result.addInventoryPublished());
    }

    @Test
    void refusedSqlCommitCancelsReservationAndPublishesNothing() throws Exception {
        Result result = rent(RepositoryPersistenceResult.CONFLICT);
        assertNotNull(result.failure());
        assertInstanceOf(IOException.class, result.failure());
        assertTrue(result.failure().getMessage().contains("Water Dragon"),
                result.failure().getMessage());
        assertEquals(1, result.persistCalls());
        assertEquals(0, result.passesAfter());
        assertEquals(100_000, result.adenaAfter());
        assertEquals(0, result.roomsAfter(), "refused SQL commit must cancel the room reservation");
        assertTrue(!result.addInventoryPublished());
    }

    private record Result(Throwable failure, int persistCalls, boolean passChangePresent,
                          int passesDuringCommit, int adenaDuringCommit, int roomsDuringCommit,
                          int passesAfter, int adenaAfter, int roomsAfter,
                          boolean addInventoryPublished) {}

    private Result rent(RepositoryPersistenceResult outcome) throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        String source = config.classicServerNames().get(0);
        GameStateStore state = new GameStateStore(
                temporary.resolve("water-dragon-sql-" + outcome + ".properties"), false);
        AtomicInteger persistCalls = new AtomicInteger();
        boolean[] passChange = new boolean[1];
        int[] during = new int[3];
        ClientSession[] holder = new ClientSession[1];
        GameHub[] hubHolder = new GameHub[1];
        try (AccountRepository delegate = AccountRepository.open(config);
             GameHub hub = new GameHub(config);
             CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + outcome));
             ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            hubHolder[0] = hub;
            client.setSoTimeout(100);
            AccountRepository[] self = new AccountRepository[1];
            self[0] = (AccountRepository) Proxy.newProxyInstance(
                    AccountRepository.class.getClassLoader(),
                    new Class<?>[] {AccountRepository.class},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "repositoryForClassicSource" -> self[0];
                        case "persistLitoWaterDragonPassInventory" -> {
                            persistCalls.incrementAndGet();
                            passChange[0] = arguments[6] != null;
                            RuntimeInventory visible =
                                    (RuntimeInventory) field(holder[0], "inventory");
                            during[0] = passCount(visible);
                            during[1] = visible.catalogCounts()
                                    .getOrDefault(InventoryCatalog.ADENA, 0).intValue();
                            during[2] = hubHolder[0].classicWorldWaterDragonRentedRooms(
                                    source, System.nanoTime());
                            yield outcome;
                        }
                        case "litoPrivateInventory" ->
                                new RepositoryPrivateInventorySnapshot(true, 2, List.of());
                        case "toString" -> "water-dragon-sql-behavior-repository";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == arguments[0];
                        case "close" -> null;
                        default -> {
                            try {
                                yield method.invoke(delegate, arguments);
                            } catch (InvocationTargetException failure) {
                                throw failure.getCause();
                            }
                        }
                    });

            ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                    config, self[0], state, capture, hub);
            holder[0] = session;
            int seed = 0x6131_3800;
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            setField(session, "outgoing", new Protocol.CipherState(seed));
            setEnumField(session, "phase", "WORLD");
            setField(session, "account", "water-dragon-sql");
            setField(session, "selectedCharacter", "FafurionSqlVerifier");
            setField(session, "selectedClassicServerName", source);
            CharacterSummary selected = new CharacterSummary("FafurionSqlVerifier", "", 1, 0, 80,
                    300, 100, 0, 14, 14, 12, 12, 12, 12, 14, 14,
                    0x7138_0117L, 0, 0, 0, 2, 32809, 32896, 613,
                    8, 40, 1, 8, 8, 0);
            setField(session, "characters", List.of(selected));
            setField(session, "sourceBaseMagicResistance", SourceBaseMagicResistance.from(selected));
            setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
            setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
            setField(session, "sourcePhysicalGrowthStats", SourcePhysicalGrowthStats.classic381());
            setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
            setField(session, "retainedSourceInventoryWeight", 0);
            AtomicLong objectIds = new AtomicLong(0x7138_7000L);
            setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                    Map.of(InventoryCatalog.ADENA, 100_000), objectIds::incrementAndGet));
            setField(session, "sqlPrivateInventorySnapshot",
                    new RepositoryPrivateInventorySnapshot(true, 1, List.of()));
            setField(session, "playerHp", 300);
            setField(session, "playerMaxHp", 300);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "playerClassicLevel", 80);
            setField(session, "classicWorldMapId", 613);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", 32809);
            setField(session, "positionY", 32896);
            hub.connect(session);
            hub.enterClassicWorld(session, 613, 32809, 32896, 0, -1);

            ClassicWorldNpcCatalog.StaticNpc gate = ClassicWorldNpcCatalog.all(613).stream()
                    .filter(npc -> npc.npcId() == ClassicWorldWaterDragonCatalog.GATE_NPC_ID)
                    .findFirst().orElseThrow();
            invoke(session, "openClassicWorldHypertext", new Class<?>[] {long.class},
                    gate.objectId());
            drain(client, receiver);

            Throwable failure = null;
            try {
                invoke(session, "handleClassicWorldNpcAction",
                        new Class<?>[] {Protocol.HypertextActionRequest.class},
                        new Protocol.HypertextActionRequest(gate.objectId(), "1", "",
                                Protocol.HypertextActionKind.GENERIC));
            } catch (InvocationTargetException thrown) {
                failure = thrown.getCause();
            }
            List<byte[]> packets = drain(client, receiver);
            RuntimeInventory after = (RuntimeInventory) field(session, "inventory");
            int roomsAfter = hub.classicWorldWaterDragonRentedRooms(source, System.nanoTime());
            hub.leaveClassicWorld(session);
            return new Result(failure, persistCalls.get(), passChange[0],
                    during[0], during[1], during[2],
                    passCount(after),
                    after.catalogCounts().getOrDefault(InventoryCatalog.ADENA, 0).intValue(),
                    roomsAfter,
                    packets.stream().anyMatch(packet ->
                            (packet[0] & 0xFF) == Protocol.S_ADD_INVENTORY));
        }
    }

    private static int passCount(RuntimeInventory inventory) {
        return (int) inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == ClassicWorldWaterDragonCatalog.PASS_ITEM_ID)
                .count();
    }

    private static List<byte[]> drain(Socket client, Protocol.CipherState receiver)
            throws Exception {
        List<byte[]> result = new ArrayList<>();
        while (true) {
            try {
                byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
                if (encrypted == null) return result;
                result.add(receiver.decrypt(encrypted));
            } catch (SocketTimeoutException done) {
                return result;
            }
        }
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... arguments)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, arguments);
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
    private static void setEnumField(Object target, String name, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }
}
