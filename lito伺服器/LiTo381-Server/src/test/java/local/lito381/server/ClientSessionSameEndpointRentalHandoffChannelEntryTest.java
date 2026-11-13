package local.lito381.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #369: on a same-endpoint server the rental directory stays enabled on
 * the GAME session that continues a TeleportMe handoff.  The handoff record is
 * one-shot, so once the automatic character continuation has consumed it the
 * following C_CA must still be a tournament channel entry, not an LTItemSet
 * re-selection that would replace the continued loadout.
 */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionSameEndpointRentalHandoffChannelEntryTest {
    private static final String ACCOUNT = "same-endpoint-account";

    @TempDir Path temporary;

    @Test
    void channelEntryAfterContinuedHandoffReturnsChannelInfoAndKeepsRentalObjects()
            throws Exception {
        ServerConfig config = sameEndpointConfig("continued.properties");
        GameStateStore state = new GameStateStore(
                temporary.resolve("continued-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (MemoryAccountRepository accounts =
                    new MemoryAccountRepository("test", "test");
             ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("continued-capture")), hub);
            CharacterSummary knight = character("SameEndpointKnight", 0x3810_0369L);
            initialize(session, knight);
            hub.connect(session);
            assertTrue(hub.acceptAuthenticatedAccount(
                    session, ClientSession.Endpoint.GAME, ACCOUNT));

            RentalLoadoutPlan plan = RentalLoadoutPlan.require(1, 1);
            RentalLoadoutPlan.Staged staged = plan.stage(
                    RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                    new AtomicLong(0x7300_0000L)::getAndIncrement);
            assertTrue(hub.stageRentalBattleHandoff(
                    ACCOUNT, session.sourceCharacterIdentity(), staged.inventory()));

            // New GAME socket on the same endpoint: beginGameBootstrap re-arms
            // the directory capability, completeGameBootstrap continues the
            // staged character and loadout.
            setField(session, "selectedCharacter", "");
            setField(session, "inventoryCharacter",
                    session.sourceCharacterIdentity(knight.name()));
            setPhase(session, "LOBBY");
            setField(session, "rentalTownDirectoryAdvertised", true);
            int seed = 0x3810_0369;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            assertTrue((boolean) invokeResult(session,
                    "autoSelectPendingRentalBattleHandoff"));
            assertFalse(hub.hasRentalBattleHandoff(
                            ACCOUNT, session.sourceCharacterIdentity()),
                    "the continuation consumes the one-shot handoff record");
            assertTrue((boolean) field(session, "rentalBattleLoadoutActive"));
            assertTrue((boolean) field(session, "rentalTownDirectoryAdvertised"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(plan.expectedCatalogCounts(),
                    inventory(session).catalogCounts());
            List<Long> continuedObjects = objectIds(session);
            assertFalse(continuedObjects.isEmpty());

            handle(session, new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes());

            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_0),
                    receiveUntilOpcode(client, receiver,
                            Protocol.S_LITO_ENTER_CHANNEL_RESULT, 256));
            assertArrayEquals(Protocol.litoChannelInfo(
                            Protocol.LitoChannelSelection.CHANNEL_0, 1, 100),
                    receiveUntilOpcode(client, receiver,
                            Protocol.S_LITO_CHANNEL_INFO, 16),
                    "the channel request must be answered with channel counters");
            assertEquals("CHANNEL", field(session, "phase").toString());
            assertEquals(0, field(session, "currentChannelId"));
            assertEquals(1, hub.channelSnapshot(0).users());
            assertEquals(-1, field(session, "rentalItemSetSelection"),
                    "C_CA must not be consumed as an LTItemSet selection");
            assertTrue((boolean) field(session, "rentalBattleLoadoutActive"));
            assertEquals(continuedObjects, objectIds(session),
                    "continued rental objects must survive channel entry");
            assertEquals(plan.expectedCatalogCounts(),
                    inventory(session).catalogCounts());
        } finally {
            hub.close();
        }
    }

    @Test
    void pendingUnconsumedHandoffStillEntersChannelAndActivatesLoadout()
            throws Exception {
        ServerConfig config = sameEndpointConfig("pending.properties");
        GameStateStore state = new GameStateStore(
                temporary.resolve("pending-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (MemoryAccountRepository accounts =
                    new MemoryAccountRepository("test", "test");
             ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("pending-capture")), hub);
            CharacterSummary knight = character("PendingHandoffKnight", 0x3810_036AL);
            initialize(session, knight);
            hub.connect(session);
            RentalLoadoutPlan plan = RentalLoadoutPlan.require(1, 1);
            RentalLoadoutPlan.Staged staged = plan.stage(
                    RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                    new AtomicLong(0x7300_1000L)::getAndIncrement);
            assertTrue(hub.stageRentalBattleHandoff(
                    ACCOUNT, session.sourceCharacterIdentity(), staged.inventory()));
            setPhase(session, "LOBBY");
            setField(session, "rentalTownDirectoryAdvertised", true);
            assertFalse((boolean) field(session, "rentalBattleLoadoutActive"));
            int seed = 0x3810_036A;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            handle(session, new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes());

            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_0),
                    receiveUntilOpcode(client, receiver,
                            Protocol.S_LITO_ENTER_CHANNEL_RESULT, 16));
            assertArrayEquals(Protocol.litoChannelInfo(
                            Protocol.LitoChannelSelection.CHANNEL_0, 1, 100),
                    receiveUntilOpcode(client, receiver,
                            Protocol.S_LITO_CHANNEL_INFO, 16));
            assertEquals("CHANNEL", field(session, "phase").toString());
            assertEquals(-1, field(session, "rentalItemSetSelection"));
            assertTrue((boolean) field(session, "rentalBattleLoadoutActive"),
                    "channel entry is the fallback consumer of a pending handoff");
            assertFalse(hub.hasRentalBattleHandoff(
                    ACCOUNT, session.sourceCharacterIdentity()));
            assertEquals(plan.expectedCatalogCounts(),
                    inventory(session).catalogCounts());
        } finally {
            hub.close();
        }
    }

    @Test
    void ordinaryLoginWithoutHandoffStillSelectsRentalItemSet() throws Exception {
        ServerConfig config = sameEndpointConfig("ordinary.properties");
        GameStateStore state = new GameStateStore(
                temporary.resolve("ordinary-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (MemoryAccountRepository accounts =
                    new MemoryAccountRepository("test", "test");
             ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("ordinary-capture")), hub);
            CharacterSummary knight = character("OrdinaryRentalKnight", 0x3810_036BL);
            initialize(session, knight);
            hub.connect(session);
            setPhase(session, "LOBBY");
            setField(session, "rentalTownDirectoryAdvertised", true);
            List<Long> sourceObjects = objectIds(session);
            int seed = 0x3810_036B;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            handle(session, new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(1).bytes());

            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_1),
                    receive(client, receiver));
            assertEquals(1, field(session, "rentalItemSetSelection"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(-1, field(session, "currentChannelId"));
            assertEquals(0, hub.channelSnapshot(1).users());
            assertFalse((boolean) field(session, "rentalBattleLoadoutActive"));
            assertEquals(sourceObjects, objectIds(session));
        } finally {
            hub.close();
        }
    }

    /** One server owns both the rental town directory and the battle lobby. */
    private ServerConfig sameEndpointConfig(String name) throws Exception {
        Path path = temporary.resolve(name);
        Files.writeString(path, """
                bind=127.0.0.1
                game.bind=127.0.0.2
                database.mode=memory
                database.read.only=true
                state.persistence.enabled=false
                lobby.enter.channel.response.enabled=true
                lobby.enter.channel.response.delay.ms=0
                lito.rental.town.directory.enabled=true
                lito.rental.town.room.id=1342177281
                lito.rental.town.directory.name=Rental Village Probe
                lito.rental.town.directory.host=127.0.0.1
                lito.rental.town.directory.port=32000
                lito.rental.town.map=500
                """, StandardCharsets.UTF_8);
        ServerConfig config = ServerConfig.load(path);
        assertTrue(config.rentalTownDirectoryEnabled());
        return config;
    }

    private static void initialize(ClientSession session, CharacterSummary character)
            throws Exception {
        RuntimeInventory source = new RuntimeInventory();
        source.addDistinctState(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                new RuntimeInventory.PersistentItem(
                        0x7000_0369L, 1, 0, 0, true, 1, true, true));
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
        SourceEquippedFireResistance fire = new SourceEquippedFireResistance(17, 1);
        setField(session, "account", ACCOUNT);
        setField(session, "selectedCharacter", character.name());
        setField(session, "inventoryCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", source);
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats", vital);
        setField(session, "sourceEquippedFireResistance", fire);
        setField(session, "nativeTournamentMagicBaseline", magic);
        setField(session, "nativeTournamentVitalBaseline", vital);
        setField(session, "nativeTournamentFireResistanceBaseline", fire);
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "sourceInitialArmorClassModifier", 7);
        setField(session, "nativeTournamentEconomyInitialized", true);
    }

    private static List<Long> objectIds(ClientSession session) throws Exception {
        return inventory(session).items().stream()
                .map(RuntimeInventory.Item::objectId)
                .toList();
    }

    private static RuntimeInventory inventory(ClientSession session) throws Exception {
        return (RuntimeInventory) field(session, "inventory");
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver,
            int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void handle(ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static Object invokeResult(ClientSession session, String name)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(session);
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

    private static CharacterSummary character(String name, long objectId) {
        return new CharacterSummary(name, "", 1, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }
}
