package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionConfusionIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void exactSelectorPreservesProbabilityOrderAndNonRefreshingSilence()
            throws Exception {
        Path configuration = temporary.resolve("server.properties");
        Files.writeString(configuration,
                Files.readString(Path.of("config/server.properties"))
                        + System.lineSeparator()
                        + "confusion.fixture.d100=1"
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
                    "ConfusionDefender", 1, 0x3810_2020L));
            initialize(attacker, character(
                    "ConfusionAttacker", 6, 0x3810_2021L));
            setField(attacker, "learnedSkills", Set.of(
                    BattleSpellCatalog.CONFUSION));
            hub.connect(defender);
            hub.connect(attacker);
            hub.enterChannel(0, defender);
            hub.enterChannel(0, attacker);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Confusion", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, defender);
            hub.joinRoom(roomId, attacker);
            hub.setForce(roomId, attacker, 0);
            hub.setForce(roomId, defender, 1);
            hub.setReady(roomId, defender, true);
            hub.setReady(roomId, attacker, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 60_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);
            prepareBattle(defender, roomId, 32741, 32798);
            prepareBattle(attacker, roomId, 32740, 32798);
            setField(defender, "outgoing", new Protocol.CipherState(0x2020));
            setField(attacker, "outgoing", new Protocol.CipherState(0x2021));
            setField(attacker, "lastCombatTargetObjectId",
                    defender.playerObjectId());

            BattleTimedStatus defenderStatus = (BattleTimedStatus)
                    field(defender, "timedStatus");
            BattleEraseMagicStatus erase = (BattleEraseMagicStatus)
                    field(defender, "eraseMagicStatus");
            long activatedAt = System.nanoTime();
            defenderStatus.activateCounterMagic(activatedAt, 16);
            defenderStatus.activateSleep(activatedAt, 16);
            erase.activate(activatedAt, 32);

            byte[] exactNativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(25).c(1).bytes();
            cast(attacker, exactNativeRequest);

            assertEquals(90, field(attacker, "playerHp"));
            // This Illusionist fixture has the retained original-INT MP
            // reduction of two, so the database MP15 resolves to MP13.
            assertEquals(87, field(attacker, "playerMp"));
            assertEquals(100, field(defender, "playerHp"));
            assertFalse(defenderStatus.counterMagicActive(System.nanoTime()));
            assertFalse(defenderStatus.sleepActive(System.nanoTime()));
            assertFalse(erase.active(System.nanoTime()));
            assertFalse(defenderStatus.silenceActive(System.nanoTime()),
                    "successful Counter Magic must refuse the Silence effect");

            clearCooldown(attacker);
            cast(attacker, exactNativeRequest);
            assertEquals(80, field(attacker, "playerHp"));
            assertEquals(74, field(attacker, "playerMp"));
            assertEquals(100, field(defender, "playerHp"));
            assertTrue(defenderStatus.silenceActive(System.nanoTime()));
            long firstExpiry = (long) field(defenderStatus,
                    "silenceExpiresAtNanos");

            clearCooldown(attacker);
            cast(attacker, exactNativeRequest);
            assertEquals(70, field(attacker, "playerHp"));
            assertEquals(61, field(attacker, "playerMp"));
            assertEquals(firstExpiry,
                    (long) field(defenderStatus, "silenceExpiresAtNanos"),
                    "Confusion must not refresh the existing skill64 lease");
            assertEquals(100, field(defender, "playerHp"));
        }
    }

    @Test void exactSelectorUsesTheSameContractInClassicWorld()
            throws Exception {
        Path configuration = temporary.resolve("world-server.properties");
        Files.writeString(configuration,
                Files.readString(Path.of("config/server.properties"))
                        + System.lineSeparator()
                        + "confusion.fixture.d100=1"
                        + System.lineSeparator());
        ServerConfig config = ServerConfig.load(configuration);
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config,
                    null, state,
                    new CaptureLog(temporary.resolve("world-target-capture")), hub);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config,
                    null, state,
                    new CaptureLog(temporary.resolve("world-caster-capture")), hub);
            initialize(target, character(
                    "WorldConfusionTarget", 1, 0x3810_2022L));
            initialize(caster, character(
                    "WorldConfusionCaster", 6, 0x3810_2023L));
            setField(caster, "learnedSkills", Set.of(
                    BattleSpellCatalog.CONFUSION));
            setField(target, "outgoing", new Protocol.CipherState(0x2022));
            setField(caster, "outgoing", new Protocol.CipherState(0x2023));
            hub.connect(target);
            hub.connect(caster);

            int mapId = 4;
            int casterX = 33313;
            int casterY = 32339;
            int targetX = casterX - 1;
            int targetY = casterY;
            assertFalse(ClassicWorldMapCatalog.forMap(mapId)
                    .isSafetyZone(casterX, casterY));
            assertFalse(ClassicWorldMapCatalog.forMap(mapId)
                    .isSafetyZone(targetX, targetY));
            hub.enterClassicWorld(caster, mapId, casterX, casterY, 5);
            hub.enterClassicWorld(target, mapId, targetX, targetY, 5);
            prepareWorld(caster, mapId, casterX, casterY);
            prepareWorld(target, mapId, targetX, targetY);
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            BattleTimedStatus targetStatus = (BattleTimedStatus)
                    field(target, "timedStatus");
            BattleEraseMagicStatus erase = (BattleEraseMagicStatus)
                    field(target, "eraseMagicStatus");
            long activatedAt = System.nanoTime();
            targetStatus.activateCounterMagic(activatedAt, 16);
            targetStatus.activateSleep(activatedAt, 16);
            erase.activate(activatedAt, 32);
            byte[] exactNativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(25).c(1).bytes();

            cast(caster, exactNativeRequest);
            assertEquals(90, field(caster, "playerHp"));
            assertEquals(87, field(caster, "playerMp"));
            assertEquals(100, field(target, "playerHp"));
            assertFalse(targetStatus.counterMagicActive(System.nanoTime()));
            assertFalse(targetStatus.sleepActive(System.nanoTime()));
            assertFalse(erase.active(System.nanoTime()));
            assertFalse(targetStatus.silenceActive(System.nanoTime()));

            clearCooldown(caster);
            cast(caster, exactNativeRequest);
            assertEquals(80, field(caster, "playerHp"));
            assertEquals(74, field(caster, "playerMp"));
            assertEquals(100, field(target, "playerHp"));
            assertTrue(targetStatus.silenceActive(System.nanoTime()));
        }
    }

    private static void cast(ClientSession attacker, byte[] request)
            throws Exception {
        invoke(attacker, "useSelectorOnlyTargetedAttackSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.AttackSpell.class},
                request, BattleSpellCatalog.attackSpell(
                        BattleSpellCatalog.CONFUSION));
    }

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession session) throws Exception {
        ((Map<Integer, Long>) field(session, "spellReadyNanos")).clear();
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

    private static void prepareWorld(
            ClientSession session, int mapId, int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
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
