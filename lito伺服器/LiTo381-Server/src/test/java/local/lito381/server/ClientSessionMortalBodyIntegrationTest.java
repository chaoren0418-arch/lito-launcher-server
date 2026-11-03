package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionMortalBodyIntegrationTest {
    @TempDir Path temporary;

    @Test void battleCastConsumesHpAndAcceptedPlayerDamageReflectsOnce()
            throws Exception {
        Path configuration = temporary.resolve("server.properties");
        Files.writeString(configuration,
                Files.readString(Path.of("config/server.properties"))
                        + System.lineSeparator()
                        + "mortal.body.fixture.d100=1"
                        + System.lineSeparator());
        ServerConfig config = ServerConfig.load(configuration);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket defenderListener = listener();
             Socket defenderClient = connect(defenderListener);
             Socket defenderServer = defenderListener.accept();
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept()) {
            ClientSession defender = new ClientSession(
                    defenderServer, ClientSession.Endpoint.GAME, config,
                    null, state,
                    new CaptureLog(temporary.resolve("defender-capture")), hub);
            ClientSession attacker = new ClientSession(
                    attackerServer, ClientSession.Endpoint.GAME, config,
                    null, state,
                    new CaptureLog(temporary.resolve("attacker-capture")), hub);
            initialize(defender, character(
                    "MortalDefender", 5, 0x3810_1910L));
            initialize(attacker, character(
                    "MortalAttacker", 1, 0x3810_1911L));
            setField(defender, "learnedSkills", Set.of(
                    BattleSpellCatalog.MORTAL_BODY));
            hub.connect(defender);
            hub.connect(attacker);
            hub.enterChannel(0, defender);
            hub.enterChannel(0, attacker);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Mortal Body", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, defender);
            hub.joinRoom(roomId, attacker);
            prepareBattle(defender, roomId, 32740, 32798);
            prepareBattle(attacker, roomId, 32741, 32798);
            setField(defender, "outgoing", new Protocol.CipherState(0x1910));
            setField(attacker, "outgoing", new Protocol.CipherState(0x1911));

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(23).c(6).bytes();
            invoke(defender, "useMortalBodySpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.MortalBodySpell.class},
                    request, BattleSpellCatalog.mortalBodySpell(
                            BattleSpellCatalog.MORTAL_BODY));

            assertEquals(50, field(defender, "playerHp"));
            BattleAbilityStatus status = (BattleAbilityStatus)
                    field(defender, "abilityStatus");
            assertTrue(status.mortalBody(System.nanoTime())
                    .remainingSeconds() > 0);

            invoke(defender, "takePlayerDamage",
                    new Class<?>[] {int.class, ClientSession.class},
                    10, attacker);
            assertEquals(40, field(defender, "playerHp"));
            assertEquals(80, field(attacker, "playerHp"));
        }
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
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        setField(session, "sourceEquipmentMagicStats", magic);
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
            ClientSession session, long roomId, int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(
            String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
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
