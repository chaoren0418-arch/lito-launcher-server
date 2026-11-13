package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionDiamondGolemIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void battleSelectorTargetsPcRefreshesAcAndBypassesCounterMagic()
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
                    "diamond-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "diamond-caster");
            initialize(target, character("DiamondTarget", 1, 0x3810_2140L));
            initialize(caster, character("DiamondCaster", 6, 0x3810_2141L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Diamond Golem", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, target);
            hub.joinRoom(roomId, caster);
            prepareBattle(target, roomId, 32741, 32798);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(target, "outgoing", new Protocol.CipherState(0x2140));
            setField(caster, "outgoing", new Protocol.CipherState(0x2141));
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            assertEquals(10, target.battleArmorClass());
            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 16);
            cast(caster);

            assertEquals(75, field(caster, "playerMp"));
            assertTrue(targetTimed.counterMagicActive(System.nanoTime()),
                    "skill214 is retained in EXCEPT_COUNTER_MAGIC");
            assertEquals(-10, target.battleArmorClass());
            BattleAbilityStatus targetAbility =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            BattleAbilityStatus.DiamondGolem first =
                    targetAbility.diamondGolem(System.nanoTime());
            assertEquals(-20, first.armorClass());
            assertTrue(first.remainingSeconds() >= 63);

            clearCooldown(caster);
            cast(caster);
            assertEquals(50, field(caster, "playerMp"));
            assertTrue(targetAbility.diamondGolem(
                    System.nanoTime()).remainingSeconds() >= 63);
            assertTrue(targetAbility.cancelMagicEffects().diamondGolem());
            assertEquals(10, target.battleArmorClass());
        }
    }

    @Test void worldSelectorFallsBackToSelfAndRestoresAcOnCancellation()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept()) {
            ClientSession caster = session(server, config, state, hub,
                    "world-diamond-caster");
            initialize(caster,
                    character("WorldDiamondCaster", 6, 0x3810_2142L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM));
            setField(caster, "outgoing", new Protocol.CipherState(0x2142));
            hub.connect(caster);
            int mapId = 4;
            int x = 33313;
            int y = 32339;
            hub.enterClassicWorld(caster, mapId, x, y, 5);
            prepareWorld(caster, mapId, x, y);

            BattleTimedStatus timed =
                    (BattleTimedStatus) field(caster, "timedStatus");
            timed.activateCounterMagic(System.nanoTime(), 16);
            cast(caster);

            assertEquals(75, field(caster, "playerMp"));
            assertTrue(timed.counterMagicActive(System.nanoTime()));
            assertEquals(-10, caster.battleArmorClass());
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(caster, "abilityStatus");
            assertEquals(-20,
                    ability.diamondGolem(System.nanoTime()).armorClass());
            assertTrue(ability.cancelMagicEffects().diamondGolem());
            assertEquals(10, caster.battleArmorClass());
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
                .c(26).c(5).bytes();
        invoke(caster, "useDiamondGolemSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.DiamondGolemSpell.class},
                request, BattleSpellCatalog.diamondGolemSpell(
                        BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM));
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
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void prepareWorld(ClientSession session, int mapId,
                                     int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
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
