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

final class ClientSessionArmBreakerIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void nativeCastDamagesPaysMpAndInstallsNonRefreshingLease()
            throws Exception {
        Fixture fixture = fixture("damage", 0x3810_2130L, 0x3810_2131L);
        try (fixture) {
            cast(fixture.caster, fixture.target, 99);
            BattleArmBreakerStatus status = (BattleArmBreakerStatus)
                    field(fixture.target, "armBreakerStatus");
            long firstExpiry = status.activateIfAbsent(
                    System.nanoTime(), 12).expiresAtNanos();
            int firstHp = (int) field(fixture.target, "playerHp");
            assertTrue(firstHp < 500);
            assertEquals(77, field(fixture.caster, "playerMp"),
                    "raw MP25 is reduced by selected INT18 origin bonus");
            assertEquals(-2, status.meleeDamageModifier(System.nanoTime()));

            @SuppressWarnings("unchecked")
            Map<Integer, Long> cooldowns = (Map<Integer, Long>)
                    field(fixture.caster, "spellReadyNanos");
            cooldowns.clear();
            cast(fixture.caster, fixture.target, 99);
            BattleArmBreakerStatus.Activation recast =
                    status.activateIfAbsent(System.nanoTime(), 12);
            assertTrue(recast.alreadyActive());
            assertEquals(firstExpiry, recast.expiresAtNanos(),
                    "damaging recast must not refresh status1026");
            assertTrue((int) field(fixture.target, "playerHp") < firstHp);
            assertEquals(54, field(fixture.caster, "playerMp"));
        }
    }

    @Test void counterMagicConsumesBeforeDamageAndStatusMode() throws Exception {
        Fixture fixture = fixture("counter", 0x3810_2132L, 0x3810_2133L);
        try (fixture) {
            BattleTimedStatus timed = (BattleTimedStatus)
                    field(fixture.target, "timedStatus");
            BattleArmBreakerStatus status = (BattleArmBreakerStatus)
                    field(fixture.target, "armBreakerStatus");
            timed.activateCounterMagic(System.nanoTime(), 16);
            cast(fixture.caster, fixture.target, 99);
            assertEquals(500, field(fixture.target, "playerHp"));
            assertEquals(77, field(fixture.caster, "playerMp"));
            assertFalse(timed.counterMagicActive(System.nanoTime()));
            assertEquals(0, status.meleeDamageModifier(System.nanoTime()));
        }
    }

    @Test void classicWorldSelectorRestoresTargetAndAppliesSameLease()
            throws Exception {
        Fixture fixture = worldFixture(
                "world", 0x3810_2134L, 0x3810_2135L);
        try (fixture) {
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(26).c(4).bytes();
            invoke(fixture.caster, "useSelectorOnlyTargetedAttackSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AttackSpell.class},
                    request, BattleSpellCatalog.attackSpell(
                            BattleSpellCatalog.ARM_BREAKER));
            BattleArmBreakerStatus status = (BattleArmBreakerStatus)
                    field(fixture.target, "armBreakerStatus");
            assertTrue((int) field(fixture.target, "playerHp") < 500);
            assertEquals(77, field(fixture.caster, "playerMp"));
            assertEquals(-2, status.meleeDamageModifier(System.nanoTime()));
        }
    }

    private Fixture fixture(String name, long targetObjectId,
                            long casterObjectId) throws Exception {
        Fixture fixture = sessions(name, targetObjectId, casterObjectId);
        fixture.hub.connect(fixture.target);
        fixture.hub.connect(fixture.caster);
        fixture.hub.enterChannel(0, fixture.target);
        fixture.hub.enterChannel(0, fixture.caster);
        long roomId = fixture.hub.createRoom(0, new GameHub.RoomSettings(
                "ArmBreaker", "", 2, 1, 0, 0, 0, 0));
        fixture.hub.joinRoom(roomId, fixture.target);
        fixture.hub.joinRoom(roomId, fixture.caster);
        fixture.hub.setForce(roomId, fixture.caster, 0);
        fixture.hub.setForce(roomId, fixture.target, 1);
        fixture.hub.setReady(roomId, fixture.target, true);
        fixture.hub.setReady(roomId, fixture.caster, true);
        Method countdown = GameHub.class.getDeclaredMethod(
                "countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 60_000_000_000L;
        countdown.invoke(fixture.hub, future);
        countdown.invoke(fixture.hub, future + 2_000_000_000L);
        countdown.invoke(fixture.hub, future + 4_000_000_000L);
        prepare(fixture.target, "BATTLE", roomId, 4, 32741, 32798);
        prepare(fixture.caster, "BATTLE", roomId, 4, 32740, 32798);
        return fixture.withRoom(roomId);
    }

    private Fixture worldFixture(String name, long targetObjectId,
                                 long casterObjectId) throws Exception {
        Fixture fixture = sessions(name, targetObjectId, casterObjectId);
        fixture.hub.connect(fixture.target);
        fixture.hub.connect(fixture.caster);
        int mapId = 4;
        fixture.hub.enterClassicWorld(fixture.caster, mapId, 33313, 32339, 5);
        fixture.hub.enterClassicWorld(fixture.target, mapId, 33312, 32339, 5);
        prepare(fixture.caster, "WORLD", 0, mapId, 33313, 32339);
        prepare(fixture.target, "WORLD", 0, mapId, 33312, 32339);
        setField(fixture.caster, "lastCombatTargetObjectId",
                fixture.target.playerObjectId());
        return fixture;
    }

    private Fixture sessions(String name, long targetObjectId,
                             long casterObjectId) throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve(name + "-state.properties"), false);
        GameHub hub = new GameHub(config);
        ServerSocket targetListener = listener();
        Socket targetClient = connect(targetListener);
        Socket targetServer = targetListener.accept();
        ServerSocket casterListener = listener();
        Socket casterClient = connect(casterListener);
        Socket casterServer = casterListener.accept();
        ClientSession target = new ClientSession(
                targetServer, ClientSession.Endpoint.GAME, config, null, state,
                new CaptureLog(temporary.resolve(name + "-target")), hub);
        ClientSession caster = new ClientSession(
                casterServer, ClientSession.Endpoint.GAME, config, null, state,
                new CaptureLog(temporary.resolve(name + "-caster")), hub);
        initialize(target, character(name + "Target", 1, targetObjectId));
        initialize(caster, character(name + "Caster", 6, casterObjectId));
        setField(caster, "learnedSkills", Set.of(
                BattleSpellCatalog.ARM_BREAKER));
        setField(target, "outgoing", new Protocol.CipherState(0x2130));
        setField(caster, "outgoing", new Protocol.CipherState(0x2131));
        return new Fixture(hub, 0, target, caster, targetListener,
                targetClient, targetServer, casterListener,
                casterClient, casterServer);
    }

    private static void cast(ClientSession caster, ClientSession target,
                             int mirrorRoll) throws Exception {
        Protocol.AttackSpellRequest request = new Protocol.AttackSpellRequest(
                26, 4, target.playerObjectId(), 32741, 32798);
        invoke(caster, "useAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AttackSpell.class,
                        int.class, int.class, int.class, int.class},
                request,
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.ARM_BREAKER),
                mirrorRoll, -1, -1, 0);
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

    private static void prepare(ClientSession session, String phase,
                                long roomId, int mapId, int x, int y)
            throws Exception {
        setPhase(session, phase);
        setField(session, "currentRoomId", roomId);
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
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
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

    private record Fixture(GameHub hub, long roomId,
                           ClientSession target, ClientSession caster,
                           ServerSocket targetListener, Socket targetClient,
                           Socket targetServer, ServerSocket casterListener,
                           Socket casterClient, Socket casterServer)
            implements AutoCloseable {
        Fixture withRoom(long value) {
            return new Fixture(hub, value, target, caster,
                    targetListener, targetClient, targetServer,
                    casterListener, casterClient, casterServer);
        }

        @Override public void close() throws Exception {
            hub.close();
            targetServer.close();
            targetClient.close();
            targetListener.close();
            casterServer.close();
            casterClient.close();
            casterListener.close();
        }
    }
}
