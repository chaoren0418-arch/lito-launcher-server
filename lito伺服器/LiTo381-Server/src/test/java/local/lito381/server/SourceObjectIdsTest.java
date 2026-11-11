package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** T105 / Issue #306: characters from two source databases may share one objid. */
final class SourceObjectIdsTest {
    private static final Path CONFIG = Path.of(
            "src/test/resources/server-config/config/server.matchmaking-consumable-selftest.properties");
    private static final long SHARED_OBJID = 0x1000_2A2AL;

    @TempDir Path temporary;

    @Test
    void firstClaimKeepsRawObjidAndOtherSourceGetsA32BitAlias() {
        SourceObjectIds ids = new SourceObjectIds();
        Object sun = new Object(), moon = new Object();
        long a = ids.claim(sun, "Sun", SHARED_OBJID);
        long b = ids.claim(moon, "Moon", SHARED_OBJID);
        assertEquals(SHARED_OBJID, a, "first claimant keeps the source objid");
        assertNotEquals(a, b);
        assertTrue(SourceObjectIds.alias(b));
        assertTrue(b > 0 && b <= Integer.MAX_VALUE, "alias must fit a signed 32-bit packet field");
        assertEquals(new SourceObjectIds.Key("sun", SHARED_OBJID), ids.resolve(a));
        assertEquals(new SourceObjectIds.Key("moon", SHARED_OBJID), ids.resolve(b));
        // stable for the same owner
        assertEquals(b, ids.claim(moon, "Moon", SHARED_OBJID));
        assertEquals(a, ids.claim(sun, "SUN", SHARED_OBJID), "source names compare case-insensitively");
    }

    @Test
    void sameSourceAndObjidSharesOneRuntimeIdAndDistinctObjidsStayRaw() {
        SourceObjectIds ids = new SourceObjectIds();
        Object first = new Object(), reconnect = new Object(), other = new Object();
        assertEquals(SHARED_OBJID, ids.claim(first, "Sun", SHARED_OBJID));
        assertEquals(SHARED_OBJID, ids.claim(reconnect, "Sun", SHARED_OBJID));
        assertEquals(SHARED_OBJID + 1, ids.claim(other, "Moon", SHARED_OBJID + 1));
    }

    @Test
    void aliasesNeverCollideWithLaterRawObjidsOrEachOther() {
        SourceObjectIds ids = new SourceObjectIds();
        Object sun = new Object(), moon = new Object(), star = new Object(), late = new Object();
        ids.claim(sun, "Sun", SHARED_OBJID);
        long moonId = ids.claim(moon, "Moon", SHARED_OBJID);
        long starId = ids.claim(star, "Star", SHARED_OBJID);
        assertNotEquals(moonId, starId);
        // A source whose raw objid equals an issued alias must not take it.
        long lateId = ids.claim(late, "Late", moonId);
        assertNotEquals(moonId, lateId);
        assertTrue(SourceObjectIds.alias(lateId));
        assertEquals(4, Set.copyOf(ids.snapshot().stream().map(SourceObjectIds.Claim::runtimeId).toList()).size());
    }

    @Test
    void releaseFreesRawObjidAndChangingCharacterMovesTheClaim() {
        SourceObjectIds ids = new SourceObjectIds();
        Object sun = new Object(), moon = new Object();
        ids.claim(sun, "Sun", SHARED_OBJID);
        ids.release(sun);
        assertNull(ids.resolve(SHARED_OBJID));
        assertEquals(SHARED_OBJID, ids.claim(moon, "Moon", SHARED_OBJID));
        assertEquals(77L, ids.claim(moon, "Moon", 77L), "selecting another character drops the old claim");
        assertNull(ids.resolve(SHARED_OBJID));
        assertThrows(IllegalArgumentException.class, () -> ids.claim(sun, "Sun", 0));
        assertThrows(IllegalArgumentException.class, () -> ids.claim(sun, "Sun", 0x8000_0000L));
    }

    @Test
    void settlementKeepsSourceQualifiedIdentityAndRejectsSharedRuntimeIds() throws Exception {
        String battle = UUID.randomUUID().toString();
        long sunRuntime = SHARED_OBJID, moonRuntime = SourceObjectIds.ALIAS_FIRST;
        BattleRules.Resolution result = new BattleRules.Resolution(
                true, false, Set.of(sunRuntime), Set.of(moonRuntime), "test");
        SettlementJournal.Entry sun = DurableBattleSettlement.entry(battle, sunRuntime,
                "380_ac", "Sun", "380_lito", SHARED_OBJID, "SunHero", result, Set.of());
        SettlementJournal.Entry moon = DurableBattleSettlement.entry(battle, moonRuntime,
                "380_ac_1", "Moon", "380_lito", SHARED_OBJID, "MoonHero", result, Set.of(moonRuntime));
        assertTrue(sun.won());
        assertEquals("380_ac", sun.database());
        assertEquals(SHARED_OBJID, sun.character(), "settlement writes the raw source objid");
        assertFalse(moon.won());
        assertTrue(moon.disconnected());
        assertEquals("380_ac_1", moon.database());
        assertEquals(SHARED_OBJID, moon.character());
        assertNotEquals(sun.id(), moon.id(), "receipt keys are source-qualified");
        assertNull(DurableBattleSettlement.entry(battle, 999L, "380_ac", "Sun", "380_lito",
                999L, "Bystander", result, Set.of()));

        DurableBattleSettlement.requireDistinct(List.of(
                new DurableBattleSettlement.Identity(sunRuntime, "380_ac", SHARED_OBJID),
                new DurableBattleSettlement.Identity(moonRuntime, "380_ac_1", SHARED_OBJID),
                new DurableBattleSettlement.Identity(sunRuntime, "380_ac", SHARED_OBJID)));
        // Pre-T105 behaviour: both players would carry the raw objid and settle identically.
        assertThrows(IOException.class, () -> DurableBattleSettlement.requireDistinct(List.of(
                new DurableBattleSettlement.Identity(SHARED_OBJID, "380_ac", SHARED_OBJID),
                new DurableBattleSettlement.Identity(SHARED_OBJID, "380_ac_1", SHARED_OBJID))));
    }

    @Test
    void twoSourcesWithTheSameObjidFightInOneRoomAndSettleSeparately() throws Exception {
        ServerConfig config = twoSourceConfig();
        assertEquals(List.of("Sun", "Moon"), config.classicServerNames());
        GameStateStore state = new GameStateStore(temporary.resolve("collision.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (hub;
             AccountRepository accounts = AccountRepository.open(config);
             ServerSocket sunListener = listener();
             Socket sunClient = connect(sunListener);
             Socket sunServer = sunListener.accept();
             ServerSocket moonListener = listener();
             Socket moonClient = connect(moonListener);
             Socket moonServer = moonListener.accept()) {
            sunClient.setSoTimeout(1_000);
            moonClient.setSoTimeout(1_000);
            ClientSession sun = session(sunServer, config, accounts, state, hub, "sun");
            ClientSession moon = session(moonServer, config, accounts, state, hub, "moon");
            initialize(sun, character("SunHero", 0, SHARED_OBJID), "Sun");
            initialize(moon, character("MoonHero", 1, SHARED_OBJID), "Moon");
            setField(sun, "outgoing", new Protocol.CipherState(0x3830_6001));
            setField(moon, "outgoing", new Protocol.CipherState(0x3830_6002));

            long sunId = sun.playerObjectId(), moonId = moon.playerObjectId();
            assertEquals(SHARED_OBJID, sun.sourceCharacterObjectId());
            assertEquals(SHARED_OBJID, moon.sourceCharacterObjectId());
            assertNotEquals(sunId, moonId, "in-battle object ids must not collide");
            assertTrue(sunId > 0 && sunId <= Integer.MAX_VALUE);
            assertTrue(moonId > 0 && moonId <= Integer.MAX_VALUE);
            assertEquals(new SourceObjectIds.Key("Moon", SHARED_OBJID),
                    SourceObjectIds.of(hub).resolve(moonId));

            long roomId = enterBattle(hub, sun, moon);
            assertEquals(sunId, sun.playerObjectId(), "runtime id is stable through battle start");
            assertEquals(moonId, moon.playerObjectId());

            BattleRules.Resolution resolution = hub.playerDefeated(roomId, sun, moon);
            assertTrue(resolution.ended());
            assertEquals(Set.of(sunId), resolution.winners());
            assertEquals(Set.of(moonId), resolution.losers());
            sun.serverEndBattle(resolution);
            moon.serverEndBattle(resolution);

            assertEquals(1, state.tournamentStatistics(sun.sourceCharacterIdentity()).wins());
            assertEquals(0, state.tournamentStatistics(sun.sourceCharacterIdentity()).losses());
            assertEquals(0, state.tournamentStatistics(moon.sourceCharacterIdentity()).wins());
            assertEquals(1, state.tournamentStatistics(moon.sourceCharacterIdentity()).losses());

            String battle = UUID.randomUUID().toString();
            var sunEntry = DurableBattleSettlement.entry(battle, sunId, "380_ac", "Sun", "380_lito",
                    sun.sourceCharacterObjectId(), "SunHero", resolution, Set.of());
            var moonEntry = DurableBattleSettlement.entry(battle, moonId, "380_ac_1", "Moon", "380_lito",
                    moon.sourceCharacterObjectId(), "MoonHero", resolution, Set.of());
            assertTrue(sunEntry.won());
            assertFalse(moonEntry.won());
            assertEquals(SHARED_OBJID, sunEntry.character());
            assertEquals(SHARED_OBJID, moonEntry.character());
            assertNotEquals(sunEntry.database(), moonEntry.database());
        }
    }

    private ServerConfig twoSourceConfig() throws Exception {
        String source = Files.readString(CONFIG, StandardCharsets.UTF_8).replace(
                "login.classic.server.names=LiTo 3.81C Isolated Self-Test",
                "login.classic.server.names=Sun,Moon");
        Path path = temporary.resolve("server.two-source-collision.properties");
        Files.writeString(path, source, StandardCharsets.UTF_8);
        return ServerConfig.load(path);
    }

    private long enterBattle(GameHub hub, ClientSession attacker, ClientSession target)
            throws Exception {
        hub.connect(attacker);
        hub.connect(target);
        hub.enterChannel(0, attacker);
        hub.enterChannel(0, target);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Source Collision", "", 2,
                BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
        assertTrue(hub.tryJoinRoom(roomId, attacker).joined());
        assertTrue(hub.tryJoinRoom(roomId, target).joined());
        hub.setForce(roomId, attacker, 0);
        hub.setForce(roomId, target, 1);
        assertEquals(GameHub.ReadyTransition.NONE,
                hub.setReady(roomId, target, true).transition());
        assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED,
                hub.requestOwnerStart(roomId, attacker).transition());
        Method countdown = GameHub.class.getDeclaredMethod("countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 60_000_000_000L;
        countdown.invoke(hub, future);
        countdown.invoke(hub, future + 2_000_000_000L);
        countdown.invoke(hub, future + 4_000_000_000L);
        assertTrue(hub.battleStarted(roomId));
        prepareBattle(attacker, roomId, 32740, 32798);
        prepareBattle(target, roomId, 32741, 32798);
        return roomId;
    }

    private ClientSession session(Socket socket, ServerConfig config, AccountRepository accounts,
                                  GameStateStore state, GameHub hub, String name) throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                accounts, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void initialize(ClientSession session, CharacterSummary character,
                                   String source) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", new RuntimeInventory());
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
        SourceEquippedFireResistance fire = new SourceEquippedFireResistance(0, 0);
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats", vital);
        setField(session, "sourceEquippedFireResistance", fire);
        setField(session, "nativeTournamentMagicBaseline", magic);
        setField(session, "nativeTournamentVitalBaseline", vital);
        setField(session, "nativeTournamentFireResistanceBaseline", fire);
        setField(session, "sourceBaseMagicResistance", SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "shopPoints", 20_000L);
        setField(session, "nativeTournamentEconomyInitialized", true);
    }

    private static void prepareBattle(ClientSession session, long roomId, int x, int y)
            throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int type, long objectId) {
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

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
