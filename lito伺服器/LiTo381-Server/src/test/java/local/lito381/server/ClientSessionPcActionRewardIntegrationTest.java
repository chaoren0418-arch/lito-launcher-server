package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionPcActionRewardIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    private static final Charset BIG5 = Charset.forName("windows-950");
    @TempDir Path temporary;

    @Test
    void encryptedHelperFlowListsAndClaimsOnlyAfterSqlAcceptance() throws Exception {
        String account = "pc-action-test";
        long characterObjectId = 0x3810_3000L;
        InventoryCatalog.Definition rewardDefinition =
                InventoryCatalog.require(InventoryCatalog.ADENA);
        RepositoryPendingReward reward = new RepositoryPendingReward(
                880_026L, account, rewardDefinition, 7, 0);
        AtomicBoolean pending = new AtomicBoolean(true);
        AtomicInteger claimCalls = new AtomicInteger();
        AccountRepository repository = rewardRepository(
                reward, pending, claimCalls, characterObjectId);

        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("pc-action-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, repository, state,
                    new CaptureLog(temporary.resolve("pc-action-capture")), hub);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(
                    InventoryCatalog.require(InventoryCatalog.PC_ACTION_HELPER), 1));
            RuntimeInventory.Item helper = inventory.items().get(0);
            initializeWorld(session, config, account, characterObjectId, inventory);

            int seed = 0x3810_3026;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(helper.objectId()).s("").bytes();
            invoke(session, "useInventoryItem", use);
            byte[] root = receive(client, receiver);
            assertEquals(Protocol.S_HYPERTEXT, root[0] & 0xFF);
            assertEquals("pc_action", nativeString(root, 5));
            assertEquals(0, claimCalls.get());

            invoke(session, "hypertextAction", hypertextAction(
                    characterObjectId, "long_send"));
            byte[] list = receive(client, receiver);
            assertEquals(Protocol.S_HYPERTEXT, list[0] & 0xFF);
            assertEquals("send_s_1", nativeString(list, 5));
            assertTrue(new String(list, BIG5).contains("Adena (7)"));
            assertEquals(0, claimCalls.get());

            invoke(session, "hypertextAction", hypertextAction(
                    characterObjectId, "send_1"));
            List<byte[]> result = drain(client, receiver);
            assertEquals(1, claimCalls.get());
            assertFalse(pending.get());
            RuntimeInventory committed = (RuntimeInventory) field(session, "inventory");
            assertEquals(7, committed.catalogCounts().getOrDefault(
                    InventoryCatalog.ADENA, 0));
            assertTrue(result.stream().anyMatch(packet ->
                    (packet[0] & 0xFF) == Protocol.S_ADD_INVENTORY));
            assertTrue(result.stream().anyMatch(packet ->
                    (packet[0] & 0xFF) == Protocol.S_HYPERTEXT
                            && nativeString(packet, 5).isEmpty()));

            // The one-shot capability is consumed before the SQL mutation.
            // Replaying the callback cannot claim or add another reward.
            invoke(session, "hypertextAction", hypertextAction(
                    characterObjectId, "send_1"));
            assertEquals(1, claimCalls.get());
            assertEquals(7, ((RuntimeInventory) field(session, "inventory"))
                    .catalogCounts().getOrDefault(InventoryCatalog.ADENA, 0));
        }
    }

    private static AccountRepository rewardRepository(
            RepositoryPendingReward reward, AtomicBoolean pending,
            AtomicInteger claimCalls, long characterObjectId) {
        AccountRepository[] self = new AccountRepository[1];
        self[0] = (AccountRepository) Proxy.newProxyInstance(
                AccountRepository.class.getClassLoader(),
                new Class<?>[] {AccountRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "repositoryForClassicSource" -> self[0];
                    case "pendingRewards" -> new RepositoryPendingRewardSnapshot(
                            true, pending.get() ? List.of(reward) : List.of());
                    case "claimPendingReward" -> {
                        claimCalls.incrementAndGet();
                        assertEquals(characterObjectId, arguments[0]);
                        assertEquals(reward, arguments[2]);
                        RuntimeInventory before = (RuntimeInventory) arguments[5];
                        RuntimeInventory after = (RuntimeInventory) arguments[6];
                        assertEquals(0, before.catalogCounts().getOrDefault(
                                InventoryCatalog.ADENA, 0));
                        assertEquals(7, after.catalogCounts().getOrDefault(
                                InventoryCatalog.ADENA, 0));
                        if (!pending.compareAndSet(true, false)) {
                            yield new RepositoryPendingRewardClaim(
                                    RepositoryPendingRewardClaimStatus.NOT_FOUND,
                                    reward, false);
                        }
                        yield new RepositoryPendingRewardClaim(
                                RepositoryPendingRewardClaimStatus.SUCCESS,
                                reward, false);
                    }
                    case "description" -> "pc-action-test";
                    case "close", "healthCheck" -> null;
                    case "toString" -> "pc-action-test-repository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new AssertionError(
                            "unexpected repository call: " + method.getName());
                });
        return self[0];
    }

    private static void initializeWorld(
            ClientSession session, ServerConfig config, String account,
            long objectId, RuntimeInventory inventory) throws Exception {
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", account);
        setField(session, "selectedCharacter", "PcActionKnight");
        setField(session, "characters", List.of(new CharacterSummary(
                "PcActionKnight", "", 0, 0, 27, 100, 100, 0,
                10, 10, 10, 10, 10, 10, 10, 10,
                objectId, 0, 0, 0, 3, 33051, 32337, 4,
                8, 40, 1, 8, 8, 0)));
        setField(session, "inventory", inventory);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "classicWorldMapId", 4);
        setField(session, "selectedClassicServerName",
                config.classicServerNames().get(0));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static byte[] hypertextAction(long objectId, String action) {
        return new Protocol.Writer().c(Protocol.C_HACTION).d(objectId)
                .s(action).s("").bytes();
    }

    private static byte[] receive(
            Socket client, Protocol.CipherState receiver) throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 65_535));
    }

    private static List<byte[]> drain(
            Socket client, Protocol.CipherState receiver) throws Exception {
        List<byte[]> packets = new ArrayList<>();
        while (true) {
            try {
                packets.add(receive(client, receiver));
            } catch (SocketTimeoutException done) {
                return packets;
            }
        }
    }

    private static String nativeString(byte[] packet, int offset) {
        int end = offset;
        while (end < packet.length && packet[end] != 0) end++;
        return new String(packet, offset, end - offset, BIG5);
    }

    private static void invoke(
            ClientSession session, String methodName, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(methodName, byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }
}
