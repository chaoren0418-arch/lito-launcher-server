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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

/** Encrypted wire proof for the native D3 -> DB result-controller sequence. */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionBattleResultStatisticsTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void fatalTransitionPublishesIdenticalAuthoritativeKillAndDeathRows()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-result-statistics.properties"), false);
        GameHub hub = new GameHub(config);
        try (hub;
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept();
             ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept()) {
            attackerClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession attacker = session(attackerServer, config, state, hub,
                    "attacker");
            ClientSession target = session(targetServer, config, state, hub,
                    "target");
            CharacterSummary attackerCharacter = character(
                    "ResultAttacker", 0, 0x3810_6190L);
            CharacterSummary targetCharacter = character(
                    "ResultTarget", 1, 0x3810_6191L);
            initialize(attacker, attackerCharacter);
            initialize(target, targetCharacter);

            long roomId = enterBattle(hub, attacker, target);
            int attackerSeed = 0x3810_6190;
            int targetSeed = 0x3810_6191;
            setField(attacker, "outgoing", new Protocol.CipherState(attackerSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState attackerReceiver =
                    new Protocol.CipherState(attackerSeed);
            Protocol.CipherState targetReceiver =
                    new Protocol.CipherState(targetSeed);

            BattleRules.Resolution resolution =
                    hub.playerDefeated(roomId, attacker, target);
            assertTrue(resolution.ended());
            attacker.serverEndBattle(resolution);
            target.serverEndBattle(resolution);

            Map<String, ResultRow> expected = new LinkedHashMap<>();
            expected.put(attacker.litoDisplayName(),
                    new ResultRow(0, 1, 0, 0, 0, 0));
            expected.put(target.litoDisplayName(),
                    new ResultRow(0, 0, 1, 0, 1, 1));
            assertEquals(expected, resultRows(attackerClient, attackerReceiver));
            assertEquals(expected, resultRows(targetClient, targetReceiver));
            assertEquals(1, state.tournamentStatistics(
                    attacker.sourceCharacterIdentity()).wins(),
                    "the authoritative winner must be persisted at battle end");
            assertEquals(1, state.tournamentStatistics(
                    target.sourceCharacterIdentity()).losses(),
                    "the authoritative loser must be persisted at battle end");
        }
    }

    @Test
    void timeoutPublishesScoresBeforeResettingTheMatch() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-timeout-statistics.properties"), false);
        GameHub hub = new GameHub(config);
        try (hub;
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept();
             ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept()) {
            attackerClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession attacker = session(attackerServer, config, state, hub,
                    "timeout-attacker");
            ClientSession target = session(targetServer, config, state, hub,
                    "timeout-target");
            initialize(attacker, character("TimeoutAttacker", 0, 0x3810_6290L));
            initialize(target, character("TimeoutTarget", 1, 0x3810_6291L));

            long roomId = enterBattle(
                    hub, attacker, target, BattleRules.Mode.HONOR_MATCH);
            BattleRules.Resolution defeat =
                    hub.playerDefeated(roomId, attacker, target);
            assertFalse(defeat.ended(), "Honor Match player defeat must respawn");

            int attackerSeed = 0x3810_6290;
            int targetSeed = 0x3810_6291;
            setField(attacker, "outgoing", new Protocol.CipherState(attackerSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState attackerReceiver = new Protocol.CipherState(attackerSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            countdown.invoke(hub, Long.MAX_VALUE);
            BattleRules.Snapshot finished = hub.matchSnapshot(roomId);
            assertEquals(BattleRules.Phase.FINISHED, finished.phase());
            assertEquals(1, finished.playerScores().get(attacker.playerObjectId()));
            assertEquals(1, finished.playerDeaths().get(target.playerObjectId()));

            BattleRules.Resolution timeout = new BattleRules.Resolution(
                    true, false, java.util.Set.of(),
                    java.util.Set.of(attacker.playerObjectId(), target.playerObjectId()),
                    "honormatch-timeout");
            attacker.serverEndBattle(timeout);
            target.serverEndBattle(timeout);
            hub.endBattle(roomId);

            Map<String, ResultRow> expected = new LinkedHashMap<>();
            expected.put(attacker.litoDisplayName(),
                    new ResultRow(0, 1, 0, 0, 0, 0));
            expected.put(target.litoDisplayName(),
                    new ResultRow(0, 0, 1, 0, 1, 1));
            assertEquals(expected, resultRows(attackerClient, attackerReceiver));
            assertEquals(expected, resultRows(targetClient, targetReceiver));
            assertEquals(BattleRules.Phase.WAITING, hub.matchSnapshot(roomId).phase());
        }
    }

    private long enterBattle(GameHub hub, ClientSession attacker,
                             ClientSession target) throws Exception {
        return enterBattle(hub, attacker, target, BattleRules.Mode.DEATHMATCH);
    }

    private long enterBattle(GameHub hub, ClientSession attacker,
                             ClientSession target, BattleRules.Mode mode) throws Exception {
        hub.connect(attacker);
        hub.connect(target);
        hub.enterChannel(0, attacker);
        hub.enterChannel(0, target);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Result Statistics", "", 2,
                mode.wireId(), 0, 0, 0, 0));
        hub.joinRoom(roomId, attacker);
        hub.joinRoom(roomId, target);
        hub.setForce(roomId, attacker, 0);
        hub.setForce(roomId, target, 1);
        hub.setReady(roomId, attacker, true);
        hub.setReady(roomId, target, true);
        Method countdown = GameHub.class.getDeclaredMethod(
                "countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 60_000_000_000L;
        countdown.invoke(hub, future);
        countdown.invoke(hub, future + 2_000_000_000L);
        countdown.invoke(hub, future + 4_000_000_000L);
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

    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
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
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "shopPoints", 20_000L);
        setField(session, "nativeTournamentEconomyInitialized", true);
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

    private static Map<String, ResultRow> resultRows(
            Socket client, Protocol.CipherState receiver) throws Exception {
        assertEquals(Protocol.S_LITO_END_GAME,
                receiveUntilOpcode(client, receiver,
                        Protocol.S_LITO_END_GAME, 8)[0] & 0xFF);
        Map<String, ResultRow> rows = new LinkedHashMap<>();
        for (int index = 0; index < 2; index++) {
            byte[] packet = receiveUntilOpcode(client, receiver,
                    Protocol.S_LITO_POINT, 4);
            int stringEnd = zeroTerminatedEnd(packet, 1);
            String name = new String(packet, 1, stringEnd - 2,
                    Protocol.LEGACY_TEXT);
            rows.put(name, new ResultRow(
                    unsignedInt(packet, stringEnd),
                    unsignedInt(packet, stringEnd + 4),
                    unsignedInt(packet, stringEnd + 8),
                    unsignedInt(packet, stringEnd + 12),
                    packet[stringEnd + 16] & 0xFF,
                    packet[stringEnd + 17] & 0xFF));
            assertEquals(stringEnd + 18, packet.length);
        }
        return rows;
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver,
            int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receiver.decrypt(
                    Protocol.readFrame(client.getInputStream(), 8_192));
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode
                + " within " + maximumFrames + " frames");
    }

    private static int zeroTerminatedEnd(byte[] payload, int offset) {
        while (offset < payload.length && payload[offset] != 0) offset++;
        if (offset >= payload.length) {
            throw new AssertionError("unterminated native string");
        }
        return offset + 1;
    }

    private static long unsignedInt(byte[] payload, int offset) {
        return Integer.toUnsignedLong((payload[offset] & 0xFF)
                | ((payload[offset + 1] & 0xFF) << 8)
                | ((payload[offset + 2] & 0xFF) << 16)
                | ((payload[offset + 3] & 0xFF) << 24));
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

    private record ResultRow(long points, long kills, long deaths, long assists,
                             int firstForce, int secondForce) {}
}
