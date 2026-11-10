package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #138 B-land (主控裁定第 4 點): replaces the text audit
 * {@code NativeTournamentMatchEndEconomySourceAuditTest#resultPublishesPrizeBeforeRemovingBoughtMatchItems}.
 * Protected behavior: when RESULT is created the authoritative outcome is recorded, and only
 * afterwards the per-match economy is reset (allowance cleared, match-bought items removed,
 * source-bound equipment and Tournament rewards retained).  Runs on the CI self-test profile
 * with {@code native.tournament.economy.enabled=true}.
 */
final class NativeTournamentMatchEndEconomyBehaviorTest {
    private static final Path CONFIG = Path.of(
            "src/test/resources/server-config/config/server.matchmaking-consumable-selftest.properties");

    @TempDir Path temporary;

    @Test
    void resultRecordsOutcomeThenResetsPerMatchEconomyKeepingSourceAndRewardItems()
            throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        assertTrue(config.nativeTournamentEconomyEnabled());
        GameStateStore state = new GameStateStore(
                temporary.resolve("match-end-economy.properties"), false);
        String source = config.classicServerNames().get(0);
        GameHub hub = new GameHub(config, state);
        try (hub;
             ServerSocket winnerListener = listener();
             Socket winnerClient = connect(winnerListener);
             Socket winnerServer = winnerListener.accept();
             ServerSocket loserListener = listener();
             Socket loserClient = connect(loserListener);
             Socket loserServer = loserListener.accept()) {
            winnerClient.setSoTimeout(1_000);
            loserClient.setSoTimeout(1_000);
            ClientSession winner = session(winnerServer, config, state, hub, "winner");
            ClientSession loser = session(loserServer, config, state, hub, "loser");

            RuntimeInventory winnerInventory = new RuntimeInventory();
            long sourceSword = winnerInventory.addDistinctState(
                    InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                    new RuntimeInventory.PersistentItem(
                            0x7138_0001L, 1, 0, 0, true, 1, true, true));
            long reward = winnerInventory.addDistinctState(
                    InventoryCatalog.require(41308L),
                    new RuntimeInventory.PersistentItem(
                            0x7138_0002L, 1, 0, 0, true, 1, false, true, false, null, 0));
            assertTrue(winnerInventory.add(
                    InventoryCatalog.require(InventoryCatalog.TRAINING_SWORD), 1));
            long bought = winnerInventory.items().stream()
                    .filter(item -> item.definition().catalogId() == InventoryCatalog.TRAINING_SWORD)
                    .findFirst().orElseThrow().objectId();
            assertTrue(sourceSword != 0 && reward != 0);

            initialize(winner, character("EconomyWinner", 0, 0x3813_8200L), source,
                    winnerInventory);
            initialize(loser, character("EconomyLoser", 1, 0x3813_8201L), source,
                    new RuntimeInventory());
            setField(winner, "outgoing", new Protocol.CipherState(0x3813_8200));
            setField(loser, "outgoing", new Protocol.CipherState(0x3813_8201));
            long roomId = enterBattle(hub, winner, loser);
            // Battle start already cleared the unused allowance; model in-match purchases.
            setField(winner, "shopPoints", 12_345L);
            setField(winner, "inventory", winnerInventory);

            BattleRules.Resolution resolution = hub.playerDefeated(roomId, winner, loser);
            assertTrue(resolution.ended());
            assertTrue(resolution.winners().contains(winner.playerObjectId()));
            winner.serverEndBattle(resolution);
            loser.serverEndBattle(resolution);

            assertEquals("RESULT", String.valueOf(field(winner, "phase")));
            assertEquals("RESULT", String.valueOf(field(loser, "phase")));
            // The outcome is recorded in the same RESULT transition ...
            assertEquals(1, state.tournamentStatistics(
                    winner.sourceCharacterIdentity()).wins());
            assertEquals(1, state.tournamentStatistics(
                    loser.sourceCharacterIdentity()).losses());
            // ... and the per-match economy is reset only after that ("battle-result" reason).
            assertEquals(0L, (long) field(winner, "shopPoints"));
            RuntimeInventory after = (RuntimeInventory) field(winner, "inventory");
            assertNull(after.item(bought), "match-bought items are removed at RESULT");
            assertNotNull(after.item(sourceSword), "source-bound equipment is retained");
            assertNotNull(after.item(reward), "Tournament rewards survive the reset");
            assertFalse((boolean) field(winner, "ready"));
        }
    }

    private long enterBattle(GameHub hub, ClientSession attacker, ClientSession target)
            throws Exception {
        hub.connect(attacker);
        hub.connect(target);
        hub.enterChannel(0, attacker);
        hub.enterChannel(0, target);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Match End Economy", "", 2,
                BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
        assertTrue(hub.tryJoinRoom(roomId, attacker).joined());
        assertTrue(hub.tryJoinRoom(roomId, target).joined());
        hub.setForce(roomId, attacker, 0);
        hub.setForce(roomId, target, 1);
        // Same start sequence as GameHubChannelTest#opcodeOnlyNativeStart...: the participant
        // readies (no transition yet); the room owner (first joiner) then starts the countdown.
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

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void initialize(ClientSession session, CharacterSummary character,
                                   String source, RuntimeInventory inventory) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", inventory);
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

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
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
