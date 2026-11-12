package local.lito381.server;

import static local.lito381.server.DurableSettlementCiFixture.KNIGHT;
import static local.lito381.server.DurableSettlementCiFixture.LOSS_REWARD;
import static local.lito381.server.DurableSettlementCiFixture.WIN_REWARD;
import static local.lito381.server.DurableSettlementCiFixture.WIZARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * T124 (#355): several rooms in one {@link GameHub}, players disconnecting and reconnecting at every
 * battle phase, settled through the production path
 * (registerBattleDisconnect → resolveBattleDisconnectSafely → TeamDisconnectSettlement →
 * DurableBattleSettlement → SettlementJournal → SettlementSql → CI MySQL character_lito).
 *
 * <p>Checks per round: the room is gone, nothing is left unsaved, departed sessions are gone from
 * the hub; at the end: every character_lito row equals the model (each result exactly once) before
 * and after a WAL replay, and no released profile was recreated.</p>
 */
final class BattleDisconnectStressMySqlIntegrationTest {
    private static Properties ci;
    @TempDir Path temporary;

    @BeforeAll
    static void requireCiFixture() throws Exception {
        ci = DurableSettlementCiFixture.requireCiConfig();
    }

    enum Scenario {
        /** Socket closes in the room before the battle starts: no settlement at all. */
        BEFORE_START,
        /** One side's socket closes during the battle: disconnect loss, the other side wins. */
        DURING_BATTLE,
        /** Both sockets close inside the 300 ms window: the match is void. */
        BOTH_DURING_BATTLE,
        /** Battle ended; the loser's socket closes while results are being delivered. */
        WHILE_SETTLING,
        /** Results delivered and endBattle done; a socket closes on the result screen. */
        AFTER_RESULT,
        /** Socket closes in battle and the player logs in again before the resolver runs. */
        RECONNECT_BEFORE_RESOLUTION
    }

    record Identity(String database, long character) {
        String name() { return DurableSettlementCiFixture.name(character); }
        String source() { return DurableSettlementCiFixture.source(database); }
        String stateKey() { return GameStateStore.sourceCharacter(source(), name()); }
    }

    record Outcome(boolean won, boolean disconnected) {}

    /** A logged-in game connection for one identity; a reconnect is a new Player. */
    private final class Player {
        final Identity identity;
        final ClientSession session;
        final Socket client;
        final Socket server;

        Player(Identity identity, ClientSession session, Socket client, Socket server) {
            this.identity = identity;
            this.session = session;
            this.client = client;
            this.server = server;
        }

        long id() { return session.playerObjectId(); }
    }

    private ServerConfig config;
    private GameHub hub;
    private GameStateStore state;
    private AccountRepository memoryAccounts;
    private DurableSettlementCiFixture fixture;
    private final AtomicInteger sessions = new AtomicInteger();
    private final Map<Identity, List<Outcome>> expected = new ConcurrentHashMap<>();
    private final List<Throwable> roundFailures = Collections.synchronizedList(new ArrayList<>());

    @Test
    @Timeout(value = 240, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void roomsWithRepeatedDisconnectsAndReconnectsSettleOnceAndLeaveNothingBehind() throws Exception {
        config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        assertEquals(WIN_REWARD, config.battleWinReward());
        assertEquals(LOSS_REWARD, config.battleLossReward());
        Field journalField = DurableBattleSettlement.class.getDeclaredField("journal");
        journalField.setAccessible(true);
        Object previousJournal = journalField.get(null);
        fixture = new DurableSettlementCiFixture(ci, temporary.resolve("fixture"));
        state = new GameStateStore(temporary.resolve("stress-state.properties"), false);
        int now = GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis());
        SettlementSql.Counters base = new SettlementSql.Counters(50, 50, 100_000, 5, now, 50_000, 5, 5);
        List<Identity> identities = new ArrayList<>();
        for (String database : DurableSettlementCiFixture.SOURCES)
            for (long character : List.of(KNIGHT, WIZARD)) {
                Identity id = new Identity(database, character);
                identities.add(id);
                fixture.seed(database, character, base, 500);
                expected.put(id, Collections.synchronizedList(new ArrayList<>()));
            }
        Path wal = temporary.resolve("battle-settlements");
        SettlementJournal journal = new SettlementJournal(wal);
        journalField.set(null, journal);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub opened = new GameHub(config, state, accounts)) {
            memoryAccounts = accounts;
            hub = opened;
            // Three rooms at once; each pairs a Knight and a Wizard from different sources (distinct objids).
            List<Identity[]> pairs = List.of(
                    new Identity[] {new Identity("380_ac", KNIGHT), new Identity("380_ac_1", WIZARD)},
                    new Identity[] {new Identity("380_ac_1", KNIGHT), new Identity("380_ac_2", WIZARD)},
                    new Identity[] {new Identity("380_ac_2", KNIGHT), new Identity("380_ac", WIZARD)});
            int rounds = Scenario.values().length * 2;
            Map<Identity, Player> current = new ConcurrentHashMap<>();
            for (Identity[] pair : pairs) for (Identity id : pair) current.put(id, login(id));
            ExecutorService pool = Executors.newFixedThreadPool(pairs.size());
            try {
                List<Future<?>> jobs = new ArrayList<>();
                for (int r = 0; r < pairs.size(); r++) {
                    int roomIndex = r;
                    Identity[] pair = pairs.get(r);
                    jobs.add(pool.submit(() -> {
                        for (int round = 0; round < rounds; round++) {
                            Scenario scenario = Scenario.values()[(round + roomIndex) % Scenario.values().length];
                            // Alternate who disconnects / wins.
                            Identity a = pair[round % 2], b = pair[(round + 1) % 2];
                            try {
                                play(scenario, roomIndex, round, current, a, b);
                            } catch (Throwable t) {
                                roundFailures.add(new AssertionError(
                                        "room " + roomIndex + " round " + round + " " + scenario + ": " + t, t));
                                return;
                            }
                        }
                    }));
                }
                for (Future<?> job : jobs) {
                    try {
                        job.get(150, TimeUnit.SECONDS);
                    } catch (java.util.concurrent.TimeoutException stuck) {
                        throw new AssertionError("room scripts did not finish (deadlock?)\n" + threadDump(), stuck);
                    }
                }
            } finally {
                pool.shutdownNow();
            }
            if (!roundFailures.isEmpty()) {
                AssertionError failure = new AssertionError(roundFailures.size() + " round(s) failed");
                for (Throwable t : roundFailures) failure.addSuppressed(t);
                throw failure;
            }
            // Stale 300 ms resolver tasks of earlier generations must all be no-ops.
            Thread.sleep(2 * GameHub.BATTLE_DISCONNECT_GRACE_MS + 400);

            // character_lito already holds every result once from the live record() path. (When an
            // earlier test in this JVM opened DurableBattleSettlement, its 2 s retry worker also
            // replays this WAL concurrently, as in production.)
            assertDatabaseMatchesModel(identities, base, now, "live path");
            // One more replay of every WAL file (what DurableBattleSettlement.retry does) adds nothing.
            journal.replay(SettlementSql::apply);
            SettlementSql.drainAll();
            assertEquals(0, journal.pending());
            assertDatabaseMatchesModel(identities, base, now, "after WAL replay");

            for (Player p : current.values()) logout(p);
            assertTrue(connected().isEmpty(), "no session left in the hub: " + connected());
            for (long roomId : rooms().keySet())
                assertTrue(NativeRoomRules.testRoom(hub, roomId), "only channel default rooms remain, found " + roomId);
            for (Identity id : identities)
                assertFalse(DurableBattleSettlement.hasProfile(state, id.stateKey()),
                        "released profile recreated for " + id);
            int results = expected.values().stream().mapToInt(List::size).sum();
            System.out.println("[T124] hub stress: rooms=" + pairs.size() + " rounds/room=" + rounds
                    + " sessions=" + sessions.get() + " results=" + results);
            assertTrue(results > 0);
        } finally {
            journalField.set(null, previousJournal);
            journal.close();
            fixture.close();
        }
    }

    /** Full JVM thread dump with monitor owners, so a CI hang shows the lock cycle. */
    static String threadDump() {
        var mx = java.lang.management.ManagementFactory.getThreadMXBean();
        StringBuilder out = new StringBuilder();
        long[] deadlocked = mx.findDeadlockedThreads();
        out.append("deadlocked=").append(deadlocked == null ? "none" : java.util.Arrays.toString(deadlocked)).append('\n');
        for (var info : mx.dumpAllThreads(true, true)) {
            out.append('"').append(info.getThreadName()).append("\" id=").append(info.getThreadId())
                    .append(' ').append(info.getThreadState());
            if (info.getLockName() != null) out.append(" on ").append(info.getLockName());
            if (info.getLockOwnerName() != null) out.append(" owned by \"").append(info.getLockOwnerName()).append('"');
            out.append('\n');
            for (StackTraceElement e : info.getStackTrace()) out.append("    at ").append(e).append('\n');
        }
        String dump = out.toString();
        System.out.println("[T124] thread dump\n" + dump);
        return dump;
    }

    private void assertDatabaseMatchesModel(List<Identity> identities, SettlementSql.Counters base, int now,
            String when) throws Exception {
        for (Identity id : identities) {
            SettlementSql.Counters model = base;
            List<Outcome> outcomes;
            synchronized (expected.get(id)) { outcomes = List.copyOf(expected.get(id)); }
            for (Outcome o : outcomes) model = SettlementSql.next(model, o.won(), o.disconnected(), WIN_REWARD, LOSS_REWARD, now);
            assertEquals(model, fixture.counters(id.database(), id.character()), when + ": counters " + id + " " + outcomes);
            assertEquals(500L + outcomes.size(), fixture.version(id.database(), id.character()), when + ": versions " + id);
        }
    }

    // ---------------------------------------------------------------- one round

    private void play(Scenario scenario, int roomIndex, int round, Map<Identity, Player> current,
            Identity aId, Identity bId) throws Exception {
        Player a = current.get(aId), b = current.get(bId);
        long roomId = createRoom("T124-" + roomIndex + "-" + round, a, b);
        switch (scenario) {
            case BEFORE_START -> {
                assertFalse(disconnect(a, roomId), "a room-phase disconnect is not a battle disconnect");
                assertNotNull(rooms().get(roomId), "the room survives while one member remains");
                current.put(aId, login(aId));
                leaveResult(b, roomId);
            }
            case DURING_BATTLE -> {
                long generation = startBattle(roomId, a, b);
                assertTrue(disconnect(a, roomId));
                resolve(roomId, generation);
                resolve(roomId, generation); // the scheduled 300 ms task runs as well
                expect(aId, false, true);
                expect(bId, true, false);
                // Duplicate / contradicting callbacks after the freeze change nothing.
                a.session.serverRecordBattleDisconnect();
                b.session.serverRecordBattleResult(false);
                b.session.serverRecordBattleResult(true);
                current.put(aId, login(aId));
                leaveResult(b, roomId);
            }
            case BOTH_DURING_BATTLE -> {
                long generation = startBattle(roomId, a, b);
                assertTrue(disconnect(a, roomId));
                assertTrue(disconnect(b, roomId));
                resolve(roomId, generation);
                a.session.serverRecordBattleDisconnect();
                b.session.serverRecordBattleDisconnect();
                current.put(aId, login(aId));
                current.put(bId, login(bId));
            }
            case WHILE_SETTLING -> {
                long generation = startBattle(roomId, a, b);
                var verdict = new BattleRules.Resolution(true, false, Set.of(a.id()), Set.of(b.id()), "t124-normal-end");
                a.session.serverEndBattle(verdict); // freezes and records both combatants first
                expect(aId, true, false);
                expect(bId, false, false);
                // The loser's socket closes before its own result packet: still a normal (non-disconnect) loss.
                assertTrue(disconnect(b, roomId));
                resolve(roomId, generation);
                b.session.serverRecordBattleDisconnect();
                current.put(bId, login(bId));
                endBattleIfPresent(roomId);
                leaveResult(a, roomId);
            }
            case AFTER_RESULT -> {
                startBattle(roomId, a, b);
                var verdict = new BattleRules.Resolution(true, false, Set.of(b.id()), Set.of(a.id()), "t124-normal-end");
                a.session.serverEndBattle(verdict);
                b.session.serverEndBattle(verdict);
                expect(aId, false, false);
                expect(bId, true, false);
                endBattleIfPresent(roomId);
                assertFalse(disconnect(a, roomId), "a result-screen disconnect is not a battle disconnect");
                a.session.serverRecordBattleResult(true);
                current.put(aId, login(aId));
                leaveResult(b, roomId);
            }
            case RECONNECT_BEFORE_RESOLUTION -> {
                long generation = startBattle(roomId, a, b);
                assertTrue(disconnect(a, roomId));
                Player again = login(aId);
                current.put(aId, again);
                resolve(roomId, generation);
                expect(aId, false, true);
                expect(bId, true, false);
                // The new lobby session has no frozen result; a stray callback is refused, not counted.
                again.session.serverRecordBattleResult(true);
                leaveResult(b, roomId);
            }
        }
        assertFalse(rooms().containsKey(roomId), "room " + roomId + " retained after " + scenario);
        assertTrue(DurableBattleSettlement.cleanupAllowed(hub, roomId), "unsaved settlement after " + scenario);
        Collection<ClientSession> live = connected();
        for (Player p : List.of(a, b))
            if (p.server.isClosed()) assertFalse(live.contains(p.session), "departed session kept in hub");
    }

    private void expect(Identity id, boolean won, boolean disconnected) {
        expected.get(id).add(new Outcome(won, disconnected));
    }

    // ---------------------------------------------------------------- hub / session plumbing

    private Player login(Identity id) throws Exception {
        int seed = 0x3550_0000 + sessions.incrementAndGet();
        Socket client;
        Socket server;
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            client = new Socket("127.0.0.1", listener.getLocalPort());
            server = listener.accept();
        }
        drain(client);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME, config, memoryAccounts, state,
                new CaptureLog(temporary.resolve("capture-" + seed)), hub);
        // The selected source routes to its own CI database (SharedDatabase.ROUTES).
        setField(session, "accounts", fixture.repository(id.database()));
        setField(session, "selectedClassicServerName", id.source());
        setField(session, "outgoing", new Protocol.CipherState(seed));
        CharacterSummary character = new CharacterSummary(id.name(), "", id.character() == KNIGHT ? 0 : 1, 0, 50,
                500, 100, 10, 12, 12, 12, 12, 18, 12, 18, 12, id.character(), 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
        setField(session, "account", id.name() + "-" + id.database());
        setField(session, "selectedCharacter", id.name());
        setField(session, "characters", List.of(character));
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
        hub.connect(session);
        hub.enterChannel(0, session);
        setPhase(session, "CHANNEL");
        return new Player(id, session, client, server);
    }

    /** Client side reads and discards everything so the server never blocks on a full socket buffer. */
    private static void drain(Socket client) {
        Thread reader = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try (InputStream in = client.getInputStream()) {
                while (in.read(buffer) >= 0) { /* discard */ }
            } catch (Exception ignored) {
                // socket closed by the test
            }
        }, "t124-client-drain");
        reader.setDaemon(true);
        reader.start();
    }

    private long createRoom(String name, Player a, Player b) throws Exception {
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(name, "", 2,
                BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
        for (Player p : List.of(a, b)) {
            assertTrue(hub.tryJoinRoom(roomId, p.session).joined(), "join " + p.identity);
            setField(p.session, "currentRoomId", roomId);
            setPhase(p.session, "ROOM");
        }
        assertFalse(NativeRoomRules.testRoom(hub, roomId), "a created room is ranked");
        hub.setForce(roomId, a.session, 0);
        hub.setForce(roomId, b.session, 1);
        return roomId;
    }

    /** Direct battle start (the countdown's state change without map/actor preparation). */
    @SuppressWarnings("unchecked")
    private long startBattle(long roomId, Player a, Player b) throws Exception {
        long generation;
        synchronized (hub) {
            Object room = rooms().get(roomId);
            Map<ClientSession, Integer> battleForces = (Map<ClientSession, Integer>) get(room, "battleForces");
            Collection<ClientSession> roster = (Collection<ClientSession>) get(room, "battleRoster");
            battleForces.put(a.session, 0);
            battleForces.put(b.session, 1);
            roster.add(a.session);
            roster.add(b.session);
            generation = (long) get(room, "battleGeneration") + 1;
            setField(room, "battleGeneration", generation);
            setField(room, "battleStarted", true);
            Map<Long, Integer> forces = new LinkedHashMap<>();
            forces.put(a.id(), 0);
            forces.put(b.id(), 1);
            ((BattleRules.MatchState) get(room, "match")).begin(List.of(a.id(), b.id()), forces, a.id(), System.nanoTime());
        }
        for (Player p : List.of(a, b)) {
            setPhase(p.session, "BATTLE");
            setField(p.session, "resultRecorded", false);
            setField(p.session, "playerHp", 500);
            setField(p.session, "playerMaxHp", 500);
            setField(p.session, "playerMp", 100);
            setField(p.session, "playerMaxMp", 100);
            DurableBattleSettlement.begin(p.session); // what serverPrepareBattle does first
        }
        return generation;
    }

    /** Socket close as ClientSession's reader loop sees it: notifyPeersOfDisconnect, then hub.disconnect. */
    private boolean disconnect(Player p, long roomId) throws Exception {
        p.client.close();
        p.server.close();
        boolean battleCombatant = get(p.session, "phase").toString().equals("BATTLE")
                && hub.registerBattleDisconnect(roomId, p.session);
        hub.disconnect(p.session);
        return battleCombatant;
    }

    private void logout(Player p) throws Exception {
        p.client.close();
        p.server.close();
        hub.disconnect(p.session);
    }

    private void resolve(long roomId, long generation) throws Exception {
        Method m = GameHub.class.getDeclaredMethod("resolveBattleDisconnectSafely", long.class, long.class);
        m.setAccessible(true);
        m.invoke(hub, roomId, generation);
    }

    private void endBattleIfPresent(long roomId) {
        if (rooms().containsKey(roomId)) hub.endBattle(roomId);
    }

    /** The surviving player confirms the result screen and returns to the lobby. */
    private void leaveResult(Player p, long roomId) throws Exception {
        hub.leaveRoom(roomId, p.session);
        setField(p.session, "currentRoomId", 0L);
        setPhase(p.session, "CHANNEL");
    }

    private Map<Long, ?> rooms() {
        synchronized (hub) {
            try {
                return new LinkedHashMap<>((Map<Long, ?>) get(hub, "rooms"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<ClientSession> connected() {
        synchronized (hub) {
            try {
                return List.copyOf((Collection<ClientSession>) get(hub, "connectedSessions"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = field(target.getClass(), name);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        field(target.getClass(), name).set(target, value);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // superclass
            }
        }
        throw new NoSuchFieldException(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
