package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionConcentrationIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void battleSelectorRestoresLastPcTargetAndInstallsPlusTwoMprLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket targetSocket = socketPair();
             SessionSocket casterSocket = socketPair()) {
            ClientSession target = session(targetSocket.server(), config, state, hub,
                    "concentration-target");
            ClientSession caster = session(casterSocket.server(), config, state, hub,
                    "concentration-caster");
            initialize(target, character("ConcentrationTarget", 1, 0x3810_2060L));
            initialize(caster, character("ConcentrationCaster", 6, 0x3810_2061L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.CONCENTRATION));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Concentration", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, target);
            hub.joinRoom(roomId, caster);
            prepareBattle(target, roomId, 32741, 32798);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(target, "outgoing", new Protocol.CipherState(0x2060));
            setField(caster, "outgoing", new Protocol.CipherState(0x2061));
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            long now = System.nanoTime();
            BattleTimedStatus casterStatus = status(caster);
            BattleTimedStatus targetStatus = status(target);
            casterStatus.activateMeditation(now, 640);
            targetStatus.activateCounterMagic(now, 16);
            cast(caster);

            assertEquals(80, field(caster, "playerMp"));
            assertFalse(casterStatus.meditationActive(System.nanoTime()),
                    "a committed spell ends the caster's Meditation");
            assertFalse(casterStatus.concentrationActive(System.nanoTime()));
            assertTrue(targetStatus.concentrationActive(System.nanoTime()));
            assertTrue(targetStatus.counterMagicActive(System.nanoTime()),
                    "friendly Concentration does not consume Counter Magic");
            assertEquals(4, BattleNaturalMpRegen.amount(
                    new BattleNaturalMpRegen.Profile(1, 12, 12, 0),
                    false, false,
                    targetStatus.concentrationActive(System.nanoTime()), 0));
        }
    }

    @Test void worldSelectorFallsBackToSelfAndBreaksBarrierAndInvisibility()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket socket = socketPair()) {
            ClientSession caster = session(socket.server(), config, state, hub,
                    "world-concentration");
            initialize(caster, character("WorldConcentration", 6, 0x3810_2062L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.CONCENTRATION));
            setField(caster, "outgoing", new Protocol.CipherState(0x2062));
            hub.connect(caster);
            int mapId = 4;
            int x = 33313;
            int y = 32339;
            hub.enterClassicWorld(caster, mapId, x, y, 5);
            prepareWorld(caster, mapId, x, y);
            setField(caster, "invisible", true);
            BattleTimedStatus status = status(caster);
            long now = System.nanoTime();
            status.activateAbsoluteBarrier(now, 12);

            cast(caster);

            assertEquals(80, field(caster, "playerMp"));
            assertTrue(status.concentrationActive(System.nanoTime()));
            assertFalse(status.absoluteBarrierActive(System.nanoTime()));
            assertEquals(false, field(caster, "invisible"));
        }
    }

    @Test void selectorFailsClosedWithoutSpendingWhenTwoPcsAreAmbiguous()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("ambiguous-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket firstSocket = socketPair();
             SessionSocket secondSocket = socketPair();
             SessionSocket casterSocket = socketPair()) {
            ClientSession first = session(firstSocket.server(), config, state, hub,
                    "concentration-first");
            ClientSession second = session(secondSocket.server(), config, state, hub,
                    "concentration-second");
            ClientSession caster = session(casterSocket.server(), config, state, hub,
                    "concentration-ambiguous");
            initialize(first, character("ConcentrationFirst", 1, 0x3810_2063L));
            initialize(second, character("ConcentrationSecond", 1, 0x3810_2064L));
            initialize(caster, character("ConcentrationAmbiguous", 6, 0x3810_2065L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.CONCENTRATION));
            hub.connect(first);
            hub.connect(second);
            hub.connect(caster);
            hub.enterChannel(0, first);
            hub.enterChannel(0, second);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Ambiguous Concentration", "", 3, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, first);
            hub.joinRoom(roomId, second);
            hub.joinRoom(roomId, caster);
            prepareBattle(first, roomId, 32741, 32798);
            prepareBattle(second, roomId, 32740, 32799);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(caster, "outgoing", new Protocol.CipherState(0x2065));

            cast(caster);

            assertEquals(100, field(caster, "playerMp"));
            assertFalse(status(first).concentrationActive(System.nanoTime()));
            assertFalse(status(second).concentrationActive(System.nanoTime()));
            assertFalse(status(caster).concentrationActive(System.nanoTime()));
        }
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void cast(ClientSession caster) throws Exception {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(5).bytes();
        invoke(caster, "useRegenSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.RegenSpell.class},
                request, BattleSpellCatalog.regenSpell(
                        BattleSpellCatalog.CONCENTRATION));
    }

    private static BattleTimedStatus status(ClientSession session)
            throws Exception {
        return (BattleTimedStatus) field(session, "timedStatus");
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static SessionSocket socketPair() throws Exception {
        ServerSocket listener = listener();
        Socket client = connect(listener);
        Socket server = listener.accept();
        listener.close();
        return new SessionSocket(client, server);
    }

    private record SessionSocket(Socket client, Socket server)
            implements AutoCloseable {
        @Override public void close() throws Exception {
            client.close();
            server.close();
        }
    }

    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
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

    private static void prepareBattle(ClientSession session, long roomId,
                                      int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        prepareResources(session);
    }

    private static void prepareWorld(ClientSession session, int mapId,
                                     int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        prepareResources(session);
    }

    private static void prepareResources(ClientSession session) throws Exception {
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int type,
                                              long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
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
