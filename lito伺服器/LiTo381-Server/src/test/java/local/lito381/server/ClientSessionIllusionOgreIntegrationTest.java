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

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionIllusionOgreIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void battleSelectorRestoresLastPcTargetAndRefreshesMeleeLease()
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
            initialize(target, character("OgreTarget", 1, 0x3810_2040L));
            initialize(caster, character("OgreCaster", 6, 0x3810_2041L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_OGRE));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Illusion Ogre", "", 2, 1, 0, 0, 0, 0));
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
            setField(target, "outgoing", new Protocol.CipherState(0x2040));
            setField(caster, "outgoing", new Protocol.CipherState(0x2041));
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 16);
            cast(caster);

            assertEquals(85, field(caster, "playerMp"));
            assertTrue(targetTimed.counterMagicActive(System.nanoTime()),
                    "skill204 is retained in EXCEPT_COUNTER_MAGIC");
            BattleAbilityStatus targetAbility =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            BattleAbilityStatus.IllusionOgre first =
                    targetAbility.illusionOgre(System.nanoTime());
            assertEquals(4, first.meleeHit());
            assertEquals(4, first.meleeDamage());
            assertTrue(first.remainingSeconds() >= 63);
            BattleAbilityStatus casterAbility =
                    (BattleAbilityStatus) field(caster, "abilityStatus");
            assertEquals(0,
                    casterAbility.illusionOgre(System.nanoTime()).remainingSeconds());

            clearCooldown(caster);
            cast(caster);
            assertEquals(70, field(caster, "playerMp"));
            BattleAbilityStatus.IllusionOgre refreshed =
                    targetAbility.illusionOgre(System.nanoTime());
            assertEquals(4, refreshed.meleeHit());
            assertEquals(4, refreshed.meleeDamage());
            assertTrue(refreshed.remainingSeconds() >= 63,
                    "same skill refreshes its 64-second lease");
        }
    }

    @Test void worldSelectorFallsBackToSelfWhenNoExternalPcExists()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept()) {
            ClientSession caster = session(server, config, state, hub,
                    "world-caster");
            initialize(caster, character("WorldOgreCaster", 6, 0x3810_2042L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_OGRE));
            setField(caster, "outgoing", new Protocol.CipherState(0x2042));
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
            BattleAbilityStatus.IllusionOgre effect =
                    ability.illusionOgre(System.nanoTime());
            assertEquals(4, effect.meleeHit());
            assertEquals(4, effect.meleeDamage());
            assertTrue(effect.remainingSeconds() >= 63);
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
            initialize(first, character("OgreFirst", 1, 0x3810_2043L));
            initialize(second, character("OgreSecond", 1, 0x3810_2044L));
            initialize(caster, character("OgreAmbiguous", 6, 0x3810_2045L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_OGRE));
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
            setField(caster, "outgoing", new Protocol.CipherState(0x2045));

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
        return ability.illusionOgre(System.nanoTime()).remainingSeconds() > 0;
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void cast(ClientSession caster) throws Exception {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(3).bytes();
        invoke(caster, "useIllusionOgreSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.IllusionOgreSpell.class},
                request, BattleSpellCatalog.illusionOgreSpell(
                        BattleSpellCatalog.ILLUSION_OGRE));
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
