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
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionMindBreakIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void battleCounterMagicPrecedesTargetMpThenAcceptedCastDrainsAndDamages()
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
            initialize(target, character("MindTarget", 1, 0x3810_2070L));
            initialize(caster, character("MindCaster", 6, 0x3810_2071L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.MIND_BREAK));
            enterBattle(hub, target, caster);
            setField(caster, "lastCombatTargetObjectId", target.playerObjectId());

            BattleTimedStatus status =
                    (BattleTimedStatus) field(target, "timedStatus");
            BattleEraseMagicStatus erase =
                    (BattleEraseMagicStatus) field(target, "eraseMagicStatus");
            long now = System.nanoTime();
            status.activateCounterMagic(now, 16);
            status.activateSleep(now, 16);
            erase.activate(now, 32);

            int mpCost = effectiveMpCost(caster);
            cast(caster);
            assertEquals(500 - 15, field(caster, "playerHp"));
            assertEquals(100 - mpCost, field(caster, "playerMp"));
            assertEquals(100, field(target, "playerMp"),
                    "Counter Magic must run before target MP debit");
            assertEquals(500, field(target, "playerHp"));
            assertFalse(status.counterMagicActive(System.nanoTime()));
            assertTrue(status.sleepActive(System.nanoTime()));
            assertTrue(erase.active(System.nanoTime()));

            clearCooldown(caster);
            cast(caster);
            int targetMp = (int) field(target, "playerMp");
            assertTrue(targetMp >= 50 && targetMp <= 95,
                    "native target debit must be exactly 5..50");
            assertTrue((int) field(target, "playerHp") < 500,
                    "successful strict target MP debit enables x4 damage");
            assertEquals(500 - 30, field(caster, "playerHp"));
            assertEquals(100 - mpCost * 2, field(caster, "playerMp"));
            assertFalse(status.sleepActive(System.nanoTime()));
            assertFalse(erase.active(System.nanoTime()));
        }
    }

    @Test void targetMpEqualToNativeMinimumNeverDebitsOrDamages()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("strict-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = session(targetServer, config, state, hub,
                    "strict-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "strict-caster");
            initialize(target, character("StrictTarget", 1, 0x3810_2072L));
            initialize(caster, character("StrictCaster", 6, 0x3810_2073L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.MIND_BREAK));
            enterBattle(hub, target, caster);
            setField(caster, "lastCombatTargetObjectId", target.playerObjectId());
            setField(target, "playerMp", 5);
            cast(caster);
            assertEquals(5, field(target, "playerMp"));
            assertEquals(500, field(target, "playerHp"));
        }
    }

    @Test void counterMirrorReflectsPreMultiplierDamageAfterTargetMpDebit()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("mirror-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = session(targetServer, config, state, hub,
                    "mirror-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "mirror-caster");
            initialize(target, character("MirrorTarget", 1, 0x3810_2076L));
            initialize(caster, character("MirrorCaster", 6, 0x3810_2077L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.MIND_BREAK));
            enterBattle(hub, target, caster);
            BattleCounterMirrorStatus mirror =
                    (BattleCounterMirrorStatus) field(target, "counterMirrorStatus");
            mirror.activate(System.nanoTime(), 16);
            Protocol.AttackSpellRequest request = new Protocol.AttackSpellRequest(
                    25, 6, target.playerObjectId(), 32741, 32798);
            invoke(caster, "useAttackSpell",
                    new Class<?>[] {Protocol.AttackSpellRequest.class,
                            BattleSpellCatalog.AttackSpell.class,
                            int.class, int.class, int.class},
                    request,
                    BattleSpellCatalog.attackSpell(BattleSpellCatalog.MIND_BREAK),
                    0, -1, -1);
            assertTrue((int) field(target, "playerMp") < 100,
                    "target MP debit precedes calcMagicDamage/Counter Mirror");
            assertEquals(500, field(target, "playerHp"));
            int casterHp = (int) field(caster, "playerHp");
            assertTrue(casterHp > 470 && casterHp < 485,
                    "reflection must use positive pre-x4 damage after HP15 cost");
            assertFalse(mirror.active(System.nanoTime()));
        }
    }

    @Test void classicWorldUsesSameSelectorAndTargetMpGate() throws Exception {
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
            initialize(target, character("WorldMindTarget", 1, 0x3810_2074L));
            initialize(caster, character("WorldMindCaster", 6, 0x3810_2075L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.MIND_BREAK));
            setField(target, "outgoing", new Protocol.CipherState(0x2074));
            setField(caster, "outgoing", new Protocol.CipherState(0x2075));
            hub.connect(target);
            hub.connect(caster);
            int mapId = 4;
            int casterX = 33313;
            int casterY = 32339;
            int targetX = casterX - 1;
            hub.enterClassicWorld(caster, mapId, casterX, casterY, 5);
            hub.enterClassicWorld(target, mapId, targetX, casterY, 5);
            prepareWorld(caster, mapId, casterX, casterY);
            prepareWorld(target, mapId, targetX, casterY);
            setField(caster, "lastCombatTargetObjectId", target.playerObjectId());
            cast(caster);
            assertTrue((int) field(target, "playerMp") >= 50
                    && (int) field(target, "playerMp") <= 95);
            assertTrue((int) field(target, "playerHp") < 500);
            assertEquals(485, field(caster, "playerHp"));
        }
    }

    private int effectiveMpCost(ClientSession caster) throws Exception {
        BattleSpellCatalog.MindBreakSpell spell =
                BattleSpellCatalog.mindBreakSpell(BattleSpellCatalog.MIND_BREAK);
        CharacterSummary character = (CharacterSummary) invokeResult(
                caster, "selectedCharacterSummary", new Class<?>[0]);
        return spell.attackSpell().effectiveMpCost(
                18, BattleSpellCatalog.originalMagicConsumeReduction(
                        character.type(), character.originalIntelligence()));
    }

    private void enterBattle(GameHub hub, ClientSession target,
                             ClientSession caster) throws Exception {
        hub.connect(target);
        hub.connect(caster);
        hub.enterChannel(0, target);
        hub.enterChannel(0, caster);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Mind", "", 2, 1, 0, 0, 0, 0));
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
        setField(target, "outgoing", new Protocol.CipherState(0x2070));
        setField(caster, "outgoing", new Protocol.CipherState(0x2071));
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void cast(ClientSession caster) throws Exception {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(6).bytes();
        invoke(caster, "useSelectorOnlyTargetedAttackSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.AttackSpell.class},
                request,
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.MIND_BREAK));
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

    private static Object invokeResult(ClientSession session, String name,
                                       Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(session, arguments);
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
