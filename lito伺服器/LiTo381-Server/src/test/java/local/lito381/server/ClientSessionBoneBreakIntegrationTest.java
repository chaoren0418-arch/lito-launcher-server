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
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionBoneBreakIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void nativeSuccessDamagesKeepsLawfulAndInstallsTwoSecondActorLock()
            throws Exception {
        Fixture fixture = fixture("success", 0x3810_2080L, 0x3810_2081L);
        try (fixture) {
            cast(fixture.caster, fixture.target, 99, 801);
            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(fixture.target, "timedStatus");
            assertTrue((int) field(fixture.target, "playerHp") < 500);
            assertEquals(82, field(fixture.caster, "playerMp"),
                    "raw MP20 is reduced by the selected INT18 origin bonus");
            assertEquals(0, field(fixture.caster, "playerLawful"),
                    "issue #309: 380_db.sql row208 lawful=0");
            assertTrue(targetStatus.boneBreakActive(System.nanoTime()));
            assertTrue(targetStatus.actionLocked(System.nanoTime()));
            List<BattleBoneBreakEffects.Effect> effects = fixture.hub
                    .boneBreakEffects(fixture.roomId, System.nanoTime());
            assertEquals(1, effects.size());
            assertEquals(fixture.target.playerObjectId(),
                    effects.get(0).targetObjectId());
        }
    }

    @Test void roll800StillDamagesButCreatesNoStatusOrEffect()
            throws Exception {
        Fixture fixture = fixture("threshold", 0x3810_2082L, 0x3810_2083L);
        try (fixture) {
            cast(fixture.caster, fixture.target, 99, 800);
            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(fixture.target, "timedStatus");
            assertTrue((int) field(fixture.target, "playerHp") < 500);
            assertFalse(targetStatus.boneBreakActive(System.nanoTime()));
            assertTrue(fixture.hub.boneBreakEffects(
                    fixture.roomId, System.nanoTime()).isEmpty());
            assertEquals(0, field(fixture.caster, "playerLawful"),
                    "issue #309: 380_db.sql row208 lawful=0");
        }
    }

    @Test void counterMagicConsumesBeforeBothDamageCallsAndPrivateStatusRoll()
            throws Exception {
        Fixture fixture = fixture("counter", 0x3810_2084L, 0x3810_2085L);
        try (fixture) {
            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(fixture.target, "timedStatus");
            targetStatus.activateCounterMagic(System.nanoTime(), 16);
            cast(fixture.caster, fixture.target, 99, 801);
            assertEquals(500, field(fixture.target, "playerHp"));
            assertEquals(82, field(fixture.caster, "playerMp"),
                    "Counter Magic does not refund the admitted cast");
            assertEquals(0, field(fixture.caster, "playerLawful"),
                    "issue #309: 380_db.sql row208 lawful=0");
            assertFalse(targetStatus.counterMagicActive(System.nanoTime()));
            assertFalse(targetStatus.boneBreakActive(System.nanoTime()));
            assertTrue(fixture.hub.boneBreakEffects(
                    fixture.roomId, System.nanoTime()).isEmpty());
        }
    }

    @Test void firstCounterMirrorReflectsButSecondDamageAndStatusStillCommit()
            throws Exception {
        Fixture fixture = fixture("mirror", 0x3810_2086L, 0x3810_2087L);
        try (fixture) {
            BattleCounterMirrorStatus mirror = (BattleCounterMirrorStatus)
                    field(fixture.target, "counterMirrorStatus");
            mirror.activate(System.nanoTime(), 16);
            cast(fixture.caster, fixture.target, 0, 801);
            assertTrue((int) field(fixture.caster, "playerHp") < 500,
                    "the first calcMagicDamage result must reflect to caster");
            assertTrue((int) field(fixture.target, "playerHp") < 500,
                    "the retained mode's second calcMagicDamage still commits");
            assertFalse(mirror.active(System.nanoTime()));
            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(fixture.target, "timedStatus");
            assertTrue(targetStatus.boneBreakActive(System.nanoTime()),
                    "Counter Mirror does not suppress the private status roll");
        }
    }

    @Test void classicWorldSelectorUsesTheSameDamageStatusAndEffectLifecycle()
            throws Exception {
        Path configPath = temporary.resolve("world-bone-break.properties");
        Files.writeString(configPath,
                LocalResources.readString(Path.of("config/server.properties"))
                        + System.lineSeparator()
                        + "bone.break.fixture.d1000=801"
                        + System.lineSeparator());
        ServerConfig config = ServerConfig.load(configPath);
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
            initialize(target, character("WorldBoneTarget", 1, 0x3810_2088L));
            initialize(caster, character("WorldBoneCaster", 6, 0x3810_2089L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.BONE_BREAK));
            setField(caster, "playerLawful", 0);
            setField(target, "outgoing", new Protocol.CipherState(0x2088));
            setField(caster, "outgoing", new Protocol.CipherState(0x2089));
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

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(25).c(7).bytes();
            invoke(caster, "useSelectorOnlyTargetedAttackSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AttackSpell.class},
                    request,
                    BattleSpellCatalog.attackSpell(BattleSpellCatalog.BONE_BREAK));

            BattleTimedStatus status =
                    (BattleTimedStatus) field(target, "timedStatus");
            assertTrue((int) field(target, "playerHp") < 500);
            assertEquals(82, field(caster, "playerMp"));
            assertEquals(0, field(caster, "playerLawful"),
                    "issue #309: 380_db.sql row208 lawful=0");
            assertTrue(status.boneBreakActive(System.nanoTime()));
            List<BattleBoneBreakEffects.Effect> effects =
                    hub.boneBreakEffects(mapId, -1, System.nanoTime());
            assertEquals(1, effects.size());
            assertEquals(target.playerObjectId(), effects.get(0).targetObjectId());
        }
    }

    private Fixture fixture(String name, long targetObjectId,
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
        ClientSession target = session(targetServer, config, state, hub,
                name + "-target");
        ClientSession caster = session(casterServer, config, state, hub,
                name + "-caster");
        initialize(target, character(name + "Target", 1, targetObjectId));
        initialize(caster, character(name + "Caster", 6, casterObjectId));
        setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.BONE_BREAK));
        setField(caster, "playerLawful", 0);
        long roomId = enterBattle(hub, target, caster);
        setField(caster, "lastCombatTargetObjectId", target.playerObjectId());
        return new Fixture(hub, roomId, target, caster,
                targetListener, targetClient, targetServer,
                casterListener, casterClient, casterServer);
    }

    private static void cast(ClientSession caster, ClientSession target,
                             int mirrorRoll, int boneBreakRoll) throws Exception {
        Protocol.AttackSpellRequest request = new Protocol.AttackSpellRequest(
                25, 7, target.playerObjectId(), 32741, 32798);
        invoke(caster, "useAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AttackSpell.class,
                        int.class, int.class, int.class, int.class},
                request,
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.BONE_BREAK),
                mirrorRoll, -1, -1, boneBreakRoll);
    }

    private long enterBattle(GameHub hub, ClientSession target,
                             ClientSession caster) throws Exception {
        hub.connect(target);
        hub.connect(caster);
        hub.enterChannel(0, target);
        hub.enterChannel(0, caster);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Bone", "", 2, 1, 0, 0, 0, 0));
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
        setField(target, "outgoing", new Protocol.CipherState(0x2080));
        setField(caster, "outgoing", new Protocol.CipherState(0x2081));
        return roomId;
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
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
        @Override public void close() throws Exception {
            targetServer.close();
            targetClient.close();
            targetListener.close();
            casterServer.close();
            casterClient.close();
            casterListener.close();
        }
    }
}
