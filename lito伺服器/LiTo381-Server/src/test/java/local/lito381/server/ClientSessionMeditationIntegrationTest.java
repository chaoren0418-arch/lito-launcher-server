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

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionMeditationIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void worldCastUsesExactRetainedMpOrderingAndInstallsPlusFiveMprLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket socket = socketPair()) {
            ClientSession caster = session(socket.server(), config, state, hub,
                    "world-meditation");
            initialize(caster, character("WorldMeditation", 15, 13,
                    0x3810_0320L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.MEDITATION));
            setField(caster, "outgoing", new Protocol.CipherState(0x3200));
            hub.connect(caster);
            int mapId = 4;
            int x = 33313;
            int y = 32339;
            hub.enterClassicWorld(caster, mapId, x, y, 5);
            prepareWorld(caster, mapId, x, y, 500, 100);

            cast(caster);

            BattleTimedStatus status = status(caster);
            assertEquals(470, field(caster, "playerHp"));
            assertEquals(100, field(caster, "playerMp"),
                    "5-3 circle reductions-2 original INT reaches retained zero cost");
            assertTrue(status.meditationActive(System.nanoTime()));
            assertEquals(6, BattleNaturalMpRegen.amount(
                    new BattleNaturalMpRegen.Profile(0, 12, 12, 0),
                    false, status.meditationActive(System.nanoTime()), false, 0));

            invoke(caster, "cancelMeditation", new Class<?>[] {String.class},
                    "authoritative-move");
            assertFalse(status.meditationActive(System.nanoTime()));
        }
    }

    @Test
    void hpGateRejectsBeforeRefreshAndPreservesExistingMeditation()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("hp-gate-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket socket = socketPair()) {
            ClientSession caster = session(socket.server(), config, state, hub,
                    "hp-gate-meditation");
            initialize(caster, character("HpGateMeditation", 12, 12,
                    0x3810_0321L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.MEDITATION));
            setField(caster, "outgoing", new Protocol.CipherState(0x3210));
            hub.connect(caster);
            int mapId = 4;
            int x = 33313;
            int y = 32339;
            hub.enterClassicWorld(caster, mapId, x, y, 5);
            prepareWorld(caster, mapId, x, y, 30, 100);
            BattleTimedStatus status = status(caster);
            status.activateMeditation(System.nanoTime(), 640);

            cast(caster);

            assertEquals(30, field(caster, "playerHp"));
            assertEquals(100, field(caster, "playerMp"));
            assertTrue(status.meditationActive(System.nanoTime()),
                    "rejected casts must not break or refresh the existing lease");
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
                .c(3).c(7).bytes();
        invoke(caster, "useRegenSpell",
                new Class<?>[] {byte[].class, BattleSpellCatalog.RegenSpell.class},
                request, BattleSpellCatalog.regenSpell(BattleSpellCatalog.MEDITATION));
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

    private static void prepareWorld(ClientSession session, int mapId,
                                     int x, int y, int hp, int mp) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", hp);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", mp);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int intelligence,
                                              int originalIntelligence,
                                              long objectId) {
        return new CharacterSummary(name, "", 0, 0, 50, 500, 100,
                10, 12, 12, 12, 12, intelligence, 12,
                originalIntelligence, 12, objectId, 100, 0, 0,
                1, 33313, 32339, 4, 12, 40, 1, 12, 12, 0);
    }

    private static BattleTimedStatus status(ClientSession session)
            throws Exception {
        return (BattleTimedStatus) field(session, "timedStatus");
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = ClientSession.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = ClientSession.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Field field = ClientSession.class.getDeclaredField("phase");
        field.setAccessible(true);
        field.set(session, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
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
}
