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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionThunderGrabIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void exactSelectorConsumesHpDamagesAndBypassesCounterMagicThenBinds()
            throws Exception {
        Path configuration = temporary.resolve("server.properties");
        Files.writeString(configuration,
                LocalResources.readString(Path.of("config/server.properties")));
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
                    "ThunderDefender", 1, 0x3810_1920L));
            initialize(attacker, character(
                    "ThunderAttacker", 5, 0x3810_1921L));
            setField(attacker, "learnedSkills", Set.of(
                    BattleSpellCatalog.THUNDER_GRAB));
            hub.connect(defender);
            hub.connect(attacker);
            hub.enterChannel(0, defender);
            hub.enterChannel(0, attacker);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Thunder Grab", "", 2, 1, 0, 0, 0, 0));
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
            prepareBattle(defender, roomId, 32741, 32798, 1_000);
            prepareBattle(attacker, roomId, 32740, 32798, 100);
            setField(defender, "outgoing", new Protocol.CipherState(0x1920));
            setField(attacker, "outgoing", new Protocol.CipherState(0x1921));
            setField(attacker, "lastCombatTargetObjectId",
                    defender.playerObjectId());

            BattleTimedStatus defenderStatus = (BattleTimedStatus)
                    field(defender, "timedStatus");
            long activatedAt = System.nanoTime();
            defenderStatus.activateCounterMagic(activatedAt, 16);

            byte[] exactNativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(23).c(7).bytes();
            invoke(attacker, "useSelectorOnlyTargetedAttackSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AttackSpell.class},
                    exactNativeRequest, BattleSpellCatalog.attackSpell(
                            BattleSpellCatalog.THUNDER_GRAB));

            assertEquals(55, field(attacker, "playerHp"));
            assertEquals(100, field(attacker, "playerMp"));
            assertTrue((int) field(defender, "playerHp") < 1_000);
            assertTrue(defenderStatus.counterMagicActive(System.nanoTime()),
                    "skill192 must not consume Counter Magic");

            // Repeat through the same production damage path with deterministic
            // bind fixtures, independent of the first cast's random roll.
            defenderStatus.cancelThunderGrab();
            setField(attacker, "playerHp", 100);
            setField(defender, "playerHp", 1_000);
            @SuppressWarnings("unchecked")
            Map<Integer, Long> cooldowns =
                    (Map<Integer, Long>) field(attacker, "spellReadyNanos");
            cooldowns.clear();
            Protocol.AttackSpellRequest restored =
                    new Protocol.AttackSpellRequest(
                            23, 7, defender.playerObjectId(), 32741, 32798);
            invoke(attacker, "useAttackSpell",
                    new Class<?>[] {Protocol.AttackSpellRequest.class,
                            BattleSpellCatalog.AttackSpell.class,
                            int.class, int.class, int.class},
                    restored, BattleSpellCatalog.attackSpell(
                            BattleSpellCatalog.THUNDER_GRAB), 99, 1, 0);

            assertEquals(55, field(attacker, "playerHp"));
            assertEquals(100, field(attacker, "playerMp"));
            assertTrue((int) field(defender, "playerHp") < 1_000);
            assertTrue(defenderStatus.counterMagicActive(System.nanoTime()));
            assertTrue(defenderStatus.thunderGrabActive(System.nanoTime()));
            assertTrue(defenderStatus.actionLocked(System.nanoTime()));
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
        setField(session, "playerMaxHp", hp);
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
