package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionJoyOfPainIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void acceptedPlayerHitUsesPreHitMissingHpThenConsumesExactLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket defenderListener = listener();
             Socket defenderClient = connect(defenderListener);
             Socket defenderServer = defenderListener.accept();
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept()) {
            ClientSession defender = session(
                    defenderServer, config, state, hub, "defender");
            ClientSession attacker = session(
                    attackerServer, config, state, hub, "attacker");
            initialize(defender, character("JoyDefender", 0x3810_2180L));
            initialize(attacker, character("JoyAttacker", 0x3810_2181L));
            hub.connect(defender);
            hub.connect(attacker);
            hub.enterChannel(0, defender);
            hub.enterChannel(0, attacker);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Joy", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, defender);
            hub.joinRoom(roomId, attacker);
            prepareBattle(defender, roomId, 32740, 32798, 60);
            prepareBattle(attacker, roomId, 32741, 32798, 100);
            setField(defender, "outgoing", new Protocol.CipherState(0x2180));
            setField(attacker, "outgoing", new Protocol.CipherState(0x2181));

            BattleJoyOfPainStatus status = (BattleJoyOfPainStatus)
                    field(attacker, "joyOfPainStatus");
            status.activateOrRefresh(System.nanoTime(), 2);
            invoke(defender, "takePlayerDamage",
                    new Class<?>[] {int.class, ClientSession.class},
                    10, attacker);

            assertEquals(50, field(defender, "playerHp"));
            assertEquals(87, field(attacker, "playerHp"));
            assertNull(status.capture(System.nanoTime()));
        }
    }

    @Test void fullHealthDefenderStillConsumesMarkWithoutFeedbackDamage()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("full-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket defenderListener = listener();
             Socket defenderClient = connect(defenderListener);
             Socket defenderServer = defenderListener.accept();
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept()) {
            ClientSession defender = session(
                    defenderServer, config, state, hub, "full-defender");
            ClientSession attacker = session(
                    attackerServer, config, state, hub, "full-attacker");
            initialize(defender, character("FullDefender", 0x3810_2182L));
            initialize(attacker, character("FullAttacker", 0x3810_2183L));
            hub.connect(defender);
            hub.connect(attacker);
            hub.enterChannel(0, defender);
            hub.enterChannel(0, attacker);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "JoyFull", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, defender);
            hub.joinRoom(roomId, attacker);
            prepareBattle(defender, roomId, 32740, 32798, 100);
            prepareBattle(attacker, roomId, 32741, 32798, 100);
            setField(defender, "outgoing", new Protocol.CipherState(0x2182));
            setField(attacker, "outgoing", new Protocol.CipherState(0x2183));

            BattleJoyOfPainStatus status = (BattleJoyOfPainStatus)
                    field(attacker, "joyOfPainStatus");
            status.activateOrRefresh(System.nanoTime(), 2);
            invoke(defender, "takePlayerDamage",
                    new Class<?>[] {int.class, ClientSession.class},
                    10, attacker);

            assertEquals(90, field(defender, "playerHp"));
            assertEquals(100, field(attacker, "playerHp"));
            assertNull(status.capture(System.nanoTime()));
        }
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub,
                                  String capture) throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(capture)), hub);
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static void initialize(
            ClientSession session, CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static void prepareBattle(
            ClientSession session, long roomId, int x, int y, int hp)
            throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", hp);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, long objectId) {
        return new CharacterSummary(name, "", 6, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
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
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
