package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

/** Encrypted-session proof for selected mapids.resurrection=0 behavior. */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionResurrectionMapGateIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void map303ConsumesAndPublishesCastButSuppressesQuestion321()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("resurrection-map-gate-state.properties"), true);
        GameHub hub = new GameHub(config);
        try {
            try (ServerSocket listener = new ServerSocket(
                        0, 2, InetAddress.getByName("127.0.0.1"));
                 Socket casterClient = new Socket(
                         "127.0.0.1", listener.getLocalPort());
                 Socket casterServer = listener.accept();
                 Socket targetClient = new Socket(
                         "127.0.0.1", listener.getLocalPort());
                 Socket targetServer = listener.accept()) {
                casterClient.setSoTimeout(1_000);
                targetClient.setSoTimeout(1_000);
                ClientSession caster = new ClientSession(
                        casterServer, ClientSession.Endpoint.GAME, config, null,
                        state, new CaptureLog(temporary.resolve("map303-caster")), hub);
                ClientSession target = new ClientSession(
                        targetServer, ClientSession.Endpoint.GAME, config, null,
                        state, new CaptureLog(temporary.resolve("map303-target")), hub);
                initialize(caster, "MapGateCaster");
                initialize(target, "MapGateTarget");
                configureCharacter(caster, "map-gate-caster",
                        character("MapGateCaster", 3, 0x3810_0610L));
                configureCharacter(target, "map-gate-target",
                        character("MapGateTarget", 1, 0x3810_0611L));
                setField(caster, "learnedSkills",
                        Set.of(BattleSpellCatalog.RESURRECTION));

                int map = ClassicWorldMapCatalog.PHANTASM_ISLAND_MAP_ID;
                int x = 32635;
                int y = 32817;
                hub.connect(caster);
                hub.connect(target);
                hub.enterClassicWorld(caster, map, x, y, 5);
                hub.enterClassicWorld(target, map, x + 1, y, 5);
                setWorldContext(caster, map, x, y, 100, 100, 100, 100);
                setWorldContext(target, map, x + 1, y, 0, 50, 50, 50);

                int casterSeed = 0x3810_0610;
                int targetSeed = 0x3810_0611;
                setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
                setField(target, "outgoing", new Protocol.CipherState(targetSeed));
                Protocol.CipherState casterReceiver =
                        new Protocol.CipherState(casterSeed);
                Protocol.CipherState targetReceiver =
                        new Protocol.CipherState(targetSeed);

                byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(7).c(4).d(target.playerObjectId()).bytes();
                invoke(caster, "handle", new Class<?>[] {byte[].class}, request);

                byte[] casterAction = receiveUntilOpcode(
                        casterClient, casterReceiver, Protocol.S_ACTION, 8);
                assertEquals(19, casterAction[5] & 0xFF);
                byte[] casterEffect = receiveUntilOpcode(
                        casterClient, casterReceiver, Protocol.S_EFFECT, 8);
                assertEquals(target.playerObjectId(), unsignedInt(casterEffect, 1));
                assertEquals(3944, unsignedShort(casterEffect, 5));
                byte[] mana = receiveUntilOpcode(
                        casterClient, casterReceiver, Protocol.S_MANA_POINT, 8);
                // The fixture's INT18 applies the retained five-point
                // level-column MP reduction to the selected nominal MP50.
                assertEquals(55, unsignedShort(mana, 1));
                assertEquals(100, unsignedShort(mana, 3));

                receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
                receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
                targetClient.setSoTimeout(150);
                assertThrows(SocketTimeoutException.class,
                        () -> receive(targetClient, targetReceiver));
                assertEquals(55, field(caster, "playerMp"));
                assertEquals(0, field(target, "playerHp"));
                assertNull(field(target, "pendingResurrection"));
                assertTrue(ClassicWorldResurrectionCatalog.mapBlocked(map));
            }
        } finally {
            hub.close();
        }
    }

    private static void initialize(ClientSession session, String name)
            throws Exception {
        setField(session, "account", name.toLowerCase());
        setField(session, "selectedCharacter", name);
        setField(session, "inventory", baseline());
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
        SourceEquippedFireResistance fire = new SourceEquippedFireResistance(0, 0);
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats", vital);
        setField(session, "sourceEquippedFireResistance", fire);
        setField(session, "nativeTournamentMagicBaseline", magic);
        setField(session, "nativeTournamentVitalBaseline", vital);
        setField(session, "nativeTournamentFireResistanceBaseline", fire);
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "shopPoints", 20_000L);
        setField(session, "nativeTournamentEconomyInitialized", true);
    }

    private static RuntimeInventory baseline() {
        RuntimeInventory inventory = new RuntimeInventory();
        inventory.addDistinctState(InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD), new RuntimeInventory.PersistentItem(
                        0x7000_0610L, 1, 0, 0, true, 1, true, true));
        return inventory;
    }

    private static void configureCharacter(
            ClientSession session, String account, CharacterSummary character)
            throws Exception {
        setField(session, "account", account);
        setField(session, "selectedCharacter", character.name());
        setField(session, "inventoryCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
    }

    private static void setWorldContext(
            ClientSession session, int mapId, int x, int y,
            int hp, int maxHp, int mp, int maxMp) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", hp);
        setField(session, "playerMaxHp", maxHp);
        setField(session, "playerMp", mp);
        setField(session, "playerMaxMp", maxMp);
    }

    private static CharacterSummary character(String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32635, 32817, 303,
                12, 40, 1, 12, 12, 0);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver,
            int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode
                + " within " + maximumFrames + " frames");
    }

    private static int unsignedShort(byte[] payload, int offset) {
        return (payload[offset] & 0xFF) | ((payload[offset + 1] & 0xFF) << 8);
    }

    private static long unsignedInt(byte[] payload, int offset) {
        return Integer.toUnsignedLong((payload[offset] & 0xFF)
                | ((payload[offset + 1] & 0xFF) << 8)
                | ((payload[offset + 2] & 0xFF) << 16)
                | ((payload[offset + 3] & 0xFF) << 24));
    }

    private static void invoke(
            ClientSession session, String name, Class<?>[] parameters,
            Object... arguments) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
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
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
