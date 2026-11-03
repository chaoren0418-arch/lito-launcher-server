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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionIllusionAvatarIntegrationTest {
    @TempDir Path temporary;

    @Test void battleCastRefreshesWithoutStackAndAmplifiesAcceptedPlayerDamage()
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
            initialize(target, character("AvatarTarget", 1, 0x3810_2190L));
            initialize(caster, character("AvatarCaster", 6, 0x3810_2191L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_AVATAR));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Illusion Avatar", "", 2, 1, 0, 0, 0, 0));
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
            setField(target, "outgoing", new Protocol.CipherState(0x2190));
            setField(caster, "outgoing", new Protocol.CipherState(0x2191));
            setField(caster, "lastCombatTargetObjectId",
                    target.playerObjectId());

            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 16);
            cast(caster);

            assertEquals(65, field(caster, "playerMp"));
            assertTrue(targetTimed.counterMagicActive(System.nanoTime()));
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            BattleAbilityStatus.IllusionAvatar active =
                    ability.illusionAvatar(System.nanoTime());
            assertEquals(10, active.meleeDamage());
            assertEquals(10, active.rangedDamage());
            assertEquals(8, active.spellPower());
            assertEquals(1.5D, active.incomingDamageRate());
            assertEquals(8, spellPower(target));
            assertEquals(0, spellPower(caster));

            invoke(target, "takePlayerDamage",
                    new Class<?>[] {int.class, ClientSession.class,
                            boolean.class, boolean.class},
                    3, caster, false, true);
            assertEquals(496, field(target, "playerHp"),
                    "retained (int)(3*1.5) commits four damage");

            clearCooldown(caster);
            cast(caster);
            assertEquals(30, field(caster, "playerMp"));
            BattleAbilityStatus.IllusionAvatar refreshed =
                    ability.illusionAvatar(System.nanoTime());
            assertEquals(10, refreshed.meleeDamage());
            assertEquals(10, refreshed.rangedDamage());
            assertEquals(8, refreshed.spellPower());
            assertTrue(refreshed.remainingSeconds() >= 63);

            target.serverExpireTimedStatuses(
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(65));
            assertEquals(new BattleAbilityStatus.IllusionAvatar(
                            0, 0, 0, 1.0D, 0),
                    ability.illusionAvatar(System.nanoTime()));
            assertEquals(0, spellPower(target));
        }
    }

    @Test void worldSelfCastAmplifiesMonsterDamageThroughCentralCommit()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (SessionSocket sockets = socketPair()) {
            ClientSession caster = session(sockets.server(), config, state, hub,
                    "world-caster");
            initialize(caster, character("WorldAvatar", 6, 0x3810_2192L));
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.ILLUSION_AVATAR));
            setField(caster, "outgoing", new Protocol.CipherState(0x2192));
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

            assertEquals(65, field(caster, "playerMp"));
            assertTrue(timed.counterMagicActive(System.nanoTime()));
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(caster, "abilityStatus");
            assertEquals(8,
                    ability.illusionAvatar(System.nanoTime()).spellPower());
            invoke(caster, "takeClassicWorldMonsterDamage",
                    new Class<?>[] {int.class, long.class}, 3, 45_008L);
            assertEquals(496, field(caster, "playerHp"));
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
                .c(27).c(2).bytes();
        invoke(caster, "useIllusionAvatarSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.IllusionAvatarSpell.class},
                request, BattleSpellCatalog.illusionAvatarSpell(
                        BattleSpellCatalog.ILLUSION_AVATAR));
    }

    private static int spellPower(ClientSession session) throws Exception {
        return (int) invoke(session, "spellPowerModifier",
                new Class<?>[] {long.class}, System.nanoTime());
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
