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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionIllusionLichIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void battleSelectorRestoresPcTargetBypassesCounterAndRefreshesSpLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket targetSocket = socketPair();
             SessionSocket casterSocket = socketPair()) {
            ClientSession target = session(targetSocket.server(), config, state, hub,
                    "battle-target");
            ClientSession caster = session(casterSocket.server(), config, state, hub,
                    "battle-caster");
            initialize(target, character("LichTarget", 1, 0x3810_2090L));
            initialize(caster, character("LichCaster", 6, 0x3810_2091L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_LICH));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Illusion Lich", "", 2, 1, 0, 0, 0, 0));
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
            setField(target, "outgoing", new Protocol.CipherState(0x2090));
            setField(caster, "outgoing", new Protocol.CipherState(0x2091));
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 16);
            cast(caster);

            assertEquals(85, field(caster, "playerMp"));
            assertTrue(targetTimed.counterMagicActive(System.nanoTime()),
                    "skill209 is retained in EXCEPT_COUNTER_MAGIC");
            BattleAbilityStatus targetAbility =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            assertEquals(new BattleAbilityStatus.IllusionLich(2, 64),
                    targetAbility.illusionLich(System.nanoTime()));
            assertEquals(2, spellPower(target));
            assertEquals(0, spellPower(caster));

            clearCooldown(caster);
            cast(caster);
            assertEquals(70, field(caster, "playerMp"));
            BattleAbilityStatus.IllusionLich refreshed =
                    targetAbility.illusionLich(System.nanoTime());
            assertEquals(2, refreshed.spellPower(),
                    "recast refreshes and does not stack to SP+4");
            assertTrue(refreshed.remainingSeconds() >= 63);

            target.serverExpireTimedStatuses(
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(65));
            assertEquals(new BattleAbilityStatus.IllusionLich(0, 0),
                    targetAbility.illusionLich(System.nanoTime()));
            assertEquals(0, spellPower(target));
        }
    }

    @Test void worldSelectorFallsBackToSelfAndPublishesSpTwo() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket sockets = socketPair()) {
            ClientSession caster = session(sockets.server(), config, state, hub,
                    "world-caster");
            initialize(caster, character("WorldLichCaster", 6, 0x3810_2092L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_LICH));
            setField(caster, "outgoing", new Protocol.CipherState(0x2092));
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

            assertEquals(85, field(caster, "playerMp"));
            assertTrue(timed.counterMagicActive(System.nanoTime()));
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(caster, "abilityStatus");
            assertEquals(2,
                    ability.illusionLich(System.nanoTime()).spellPower());
            assertEquals(2, spellPower(caster));
        }
    }

    @Test void battleSelectorFailsClosedWhenTwoExternalPcsAreAmbiguous()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("ambiguous-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket firstSocket = socketPair();
             SessionSocket secondSocket = socketPair();
             SessionSocket casterSocket = socketPair()) {
            ClientSession first = session(firstSocket.server(), config, state, hub,
                    "ambiguous-first");
            ClientSession second = session(secondSocket.server(), config, state, hub,
                    "ambiguous-second");
            ClientSession caster = session(casterSocket.server(), config, state, hub,
                    "ambiguous-caster");
            initialize(first, character("LichFirst", 1, 0x3810_2093L));
            initialize(second, character("LichSecond", 1, 0x3810_2094L));
            initialize(caster, character("LichAmbiguous", 6, 0x3810_2095L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_LICH));
            hub.connect(first);
            hub.connect(second);
            hub.connect(caster);
            hub.enterChannel(0, first);
            hub.enterChannel(0, second);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Ambiguous", "", 3, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, first);
            hub.joinRoom(roomId, second);
            hub.joinRoom(roomId, caster);
            prepareBattle(first, roomId, 32741, 32798);
            prepareBattle(second, roomId, 32740, 32799);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(caster, "outgoing", new Protocol.CipherState(0x2095));

            cast(caster);
            assertEquals(100, field(caster, "playerMp"));
            assertFalse(active(first));
            assertFalse(active(second));
            assertFalse(active(caster));
        }
    }

    private boolean active(ClientSession session) throws Exception {
        BattleAbilityStatus ability =
                (BattleAbilityStatus) field(session, "abilityStatus");
        return ability.illusionLich(System.nanoTime()).remainingSeconds() > 0;
    }

    private static int spellPower(ClientSession session) throws Exception {
        return (int) invoke(session, "spellPowerModifier",
                new Class<?>[] {long.class}, System.nanoTime());
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void cast(ClientSession caster) throws Exception {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(26).c(0).bytes();
        invoke(caster, "useIllusionLichSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.IllusionLichSpell.class},
                request, BattleSpellCatalog.illusionLichSpell(
                        BattleSpellCatalog.ILLUSION_LICH));
    }

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession session) throws Exception {
        ((Map<Integer, Long>) field(session, "spellReadyNanos")).clear();
    }

    private static SessionSocket socketPair() throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
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

    private static Object invoke(ClientSession session, String name,
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
