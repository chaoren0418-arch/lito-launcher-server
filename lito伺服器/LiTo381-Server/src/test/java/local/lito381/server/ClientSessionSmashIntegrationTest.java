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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionSmashIntegrationTest {
    @TempDir Path temporary;

    @Test void exactSelectorUsesCounterFirstThenAppliesBattleDamage()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = session(targetServer, config, state, hub,
                    "battle-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "battle-caster");
            initialize(target, character("SmashTarget", 1, 0x3810_2030L));
            initialize(caster, character("SmashCaster", 6, 0x3810_2031L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.SMASH));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Smash", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, target);
            hub.joinRoom(roomId, caster);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, target, true);
            hub.setReady(roomId, caster, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 60_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);
            prepareBattle(target, roomId, 32741, 32798);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(target, "outgoing", new Protocol.CipherState(0x2030));
            setField(caster, "outgoing", new Protocol.CipherState(0x2031));
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            BattleEraseMagicStatus erase =
                    (BattleEraseMagicStatus) field(target, "eraseMagicStatus");
            long activatedAt = System.nanoTime();
            targetStatus.activateCounterMagic(activatedAt, 16);
            targetStatus.activateSleep(activatedAt, 16);
            erase.activate(activatedAt, 32);

            cast(caster);
            assertEquals(95, field(caster, "playerMp"));
            assertEquals(500, field(target, "playerHp"));
            assertFalse(targetStatus.counterMagicActive(System.nanoTime()));
            assertTrue(targetStatus.sleepActive(System.nanoTime()));
            assertTrue(erase.active(System.nanoTime()));

            clearCooldown(caster);
            cast(caster);
            assertEquals(90, field(caster, "playerMp"));
            int remainingHp = (int) field(target, "playerHp");
            assertTrue(remainingHp < 500 && remainingHp >= 300,
                    "retained 50+2d10 formula must produce bounded damage");
            assertFalse(targetStatus.sleepActive(System.nanoTime()));
            assertFalse(erase.active(System.nanoTime()));
        }
    }

    @Test void exactSelectorUsesSameContractInClassicWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = session(targetServer, config, state, hub,
                    "world-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "world-caster");
            initialize(target, character("WorldSmashTarget", 1, 0x3810_2032L));
            initialize(caster, character("WorldSmashCaster", 6, 0x3810_2033L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.SMASH));
            setField(target, "outgoing", new Protocol.CipherState(0x2032));
            setField(caster, "outgoing", new Protocol.CipherState(0x2033));
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

            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            BattleEraseMagicStatus erase =
                    (BattleEraseMagicStatus) field(target, "eraseMagicStatus");
            long activatedAt = System.nanoTime();
            targetStatus.activateCounterMagic(activatedAt, 16);
            targetStatus.activateSleep(activatedAt, 16);
            erase.activate(activatedAt, 32);

            cast(caster);
            assertEquals(95, field(caster, "playerMp"));
            assertEquals(500, field(target, "playerHp"));
            assertFalse(targetStatus.counterMagicActive(System.nanoTime()));
            assertTrue(targetStatus.sleepActive(System.nanoTime()));
            assertTrue(erase.active(System.nanoTime()));

            clearCooldown(caster);
            cast(caster);
            assertEquals(90, field(caster, "playerMp"));
            int remainingHp = (int) field(target, "playerHp");
            assertTrue(remainingHp < 500 && remainingHp >= 300);
            assertFalse(targetStatus.sleepActive(System.nanoTime()));
            assertFalse(erase.active(System.nanoTime()));
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
                .c(25).c(2).bytes();
        invoke(caster, "useSelectorOnlyTargetedAttackSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.AttackSpell.class},
                request, BattleSpellCatalog.attackSpell(BattleSpellCatalog.SMASH));
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
