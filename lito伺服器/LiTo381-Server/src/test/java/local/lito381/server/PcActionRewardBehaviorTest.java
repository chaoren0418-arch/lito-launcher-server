package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #138 B-land (主控裁定第 4 點): replaces the text audit
 * {@code PcActionRewardSqlFirstSourceAuditTest#helperCapabilityPrecedesWorldCompanionRoutingAndClaimIsSqlFirst}.
 * Protected behavior, driven through the real C_USE_ITEM / C_HACTION handlers on the CI
 * self-test profile:
 * <ul>
 *   <li>the PC action helper item opens {@code pc_action} in WORLD (it is routed to the helper
 *       capability, not to the ordinary world/companion item paths);</li>
 *   <li>the reward reaches the visible inventory only after the selected-source SQL claim
 *       returns SUCCESS, and a refused claim leaves inventory and pending row untouched;</li>
 *   <li>the one-shot capability cannot be replayed into a second claim.</li>
 * </ul>
 */
final class PcActionRewardBehaviorTest {
    private static final Path CONFIG = Path.of(
            "src/test/resources/server-config/config/server.selftest.properties");
    private static final Charset BIG5 = Charset.forName("windows-950");

    @TempDir Path temporary;

    @Test
    void helperOpensPcActionAndClaimPublishesOnlyAfterSqlAcceptance() throws Exception {
        Run run = run(RepositoryPendingRewardClaimStatus.SUCCESS);
        assertEquals(1, run.claimCalls.get());
        assertFalse(run.pending.get());
        assertEquals(7, run.inventoryAfterClaim.catalogCounts().getOrDefault(
                InventoryCatalog.ADENA, 0));
        assertEquals(0, run.inventoryDuringClaim.catalogCounts().getOrDefault(
                InventoryCatalog.ADENA, 0), "nothing is published before the SQL claim returns");
        assertTrue(run.claimPackets.stream().anyMatch(packet ->
                (packet[0] & 0xFF) == Protocol.S_ADD_INVENTORY));
        // Replaying the callback cannot claim or add another reward.
        assertEquals(1, run.claimCallsAfterReplay);
        assertEquals(7, run.inventoryAfterReplay.catalogCounts().getOrDefault(
                InventoryCatalog.ADENA, 0));
    }

    @Test
    void refusedSqlClaimKeepsInventoryAndPendingRewardUntouched() throws Exception {
        Run run = run(RepositoryPendingRewardClaimStatus.CONFLICT);
        assertEquals(1, run.claimCalls.get());
        assertTrue(run.pending.get(), "a refused claim must leave the native row unclaimed");
        assertEquals(0, run.inventoryAfterClaim.catalogCounts().getOrDefault(
                InventoryCatalog.ADENA, 0));
        assertTrue(run.claimPackets.stream().noneMatch(packet ->
                (packet[0] & 0xFF) == Protocol.S_ADD_INVENTORY));
    }

    private record Run(AtomicBoolean pending, AtomicInteger claimCalls,
                       RuntimeInventory inventoryDuringClaim,
                       RuntimeInventory inventoryAfterClaim, List<byte[]> claimPackets,
                       int claimCallsAfterReplay, RuntimeInventory inventoryAfterReplay) {}

    private Run run(RepositoryPendingRewardClaimStatus outcome) throws Exception {
        String account = "pc-action-test";
        long characterObjectId = 0x3813_8300L;
        RepositoryPendingReward reward = new RepositoryPendingReward(
                880_026L, account, InventoryCatalog.require(InventoryCatalog.ADENA), 7, 0);
        AtomicBoolean pending = new AtomicBoolean(true);
        AtomicInteger claimCalls = new AtomicInteger();
        RuntimeInventory[] duringClaim = new RuntimeInventory[1];
        ClientSession[] holder = new ClientSession[1];

        ServerConfig config = ServerConfig.load(CONFIG);
        GameStateStore state = new GameStateStore(
                temporary.resolve("pc-action-" + outcome + ".properties"), false);
        try (AccountRepository delegate = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
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
                            duringClaim[0] = ((RuntimeInventory) field(holder[0], "inventory")).copy();
                            RuntimeInventory after = (RuntimeInventory) arguments[6];
                            assertEquals(7, after.catalogCounts().getOrDefault(
                                    InventoryCatalog.ADENA, 0));
                            if (outcome != RepositoryPendingRewardClaimStatus.SUCCESS
                                    || !pending.compareAndSet(true, false)) {
                                yield new RepositoryPendingRewardClaim(
                                        outcome == RepositoryPendingRewardClaimStatus.SUCCESS
                                                ? RepositoryPendingRewardClaimStatus.NOT_FOUND
                                                : outcome,
                                        reward, false);
                            }
                            yield new RepositoryPendingRewardClaim(
                                    RepositoryPendingRewardClaimStatus.SUCCESS, reward, false);
                        }
                        case "toString" -> "pc-action-behavior-repository";
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

            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, self[0], state,
                    new CaptureLog(temporary.resolve("pc-action-capture-" + outcome)), hub);
            holder[0] = session;
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(
                    InventoryCatalog.require(InventoryCatalog.PC_ACTION_HELPER), 1));
            RuntimeInventory.Item helper = inventory.items().get(0);
            initializeWorld(session, config, account, characterObjectId, inventory);
            hub.connect(session);
            hub.enterClassicWorld(session, 4, ClassicWorldMapCatalog.WORLD_TREE_X,
                    ClassicWorldMapCatalog.WORLD_TREE_Y, 0);

            int seed = 0x3813_8300;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invoke(session, "useInventoryItem", new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(helper.objectId()).s("").bytes());
            byte[] root = receiveUntilOpcode(client, receiver, Protocol.S_HYPERTEXT, 8);
            assertEquals("pc_action", nativeString(root, 5));
            assertEquals(0, claimCalls.get());

            invoke(session, "hypertextAction", hypertextAction(characterObjectId, "long_send"));
            byte[] list = receiveUntilOpcode(client, receiver, Protocol.S_HYPERTEXT, 8);
            assertEquals("send_s_1", nativeString(list, 5));
            assertTrue(new String(list, BIG5).contains("Adena (7)"));
            assertEquals(0, claimCalls.get());

            invoke(session, "hypertextAction", hypertextAction(characterObjectId, "send_1"));
            List<byte[]> claimPackets = drain(client, receiver);
            RuntimeInventory afterClaim = ((RuntimeInventory) field(session, "inventory")).copy();

            invoke(session, "hypertextAction", hypertextAction(characterObjectId, "send_1"));
            drain(client, receiver);
            int callsAfterReplay = claimCalls.get();
            RuntimeInventory afterReplay = ((RuntimeInventory) field(session, "inventory")).copy();
            hub.leaveClassicWorld(session);
            return new Run(pending, claimCalls, duringClaim[0], afterClaim, claimPackets,
                    callsAfterReplay, afterReplay);
        }
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
                objectId, 0, 0, 0, 3, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 4,
                8, 40, 1, 8, 8, 0)));
        setField(session, "inventory", inventory);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "classicWorldMapId", 4);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", ClassicWorldMapCatalog.WORLD_TREE_X);
        setField(session, "positionY", ClassicWorldMapCatalog.WORLD_TREE_Y);
        setField(session, "selectedClassicServerName", config.classicServerNames().get(0));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance", new SourceEquippedFireResistance(0, 0));
        setField(session, "sourcePhysicalGrowthStats", SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static byte[] hypertextAction(long objectId, String action) {
        return new Protocol.Writer().c(Protocol.C_HACTION).d(objectId).s(action).s("").bytes();
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver) throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 65_535));
    }

    private static byte[] receiveUntilOpcode(Socket client, Protocol.CipherState receiver,
                                             int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode
                + " within " + maximumFrames + " frames");
    }

    private static List<byte[]> drain(Socket client, Protocol.CipherState receiver)
            throws Exception {
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

    private static void invoke(ClientSession session, String methodName, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(methodName, byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
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
