package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #138 B-land (主控裁定第 4 點): replaces the text audit
 * {@code NativeHonorRespawnSourceAuditTest#delayedPathPreservesDeathCoordinatesAndOmitsSpawnMovement}
 * with the behavior it protected, on the CI self-test profile:
 * Honor Match deaths wait the native five seconds, resurrect at the death coordinates
 * (S_RESURRECT, never an S_MOVE_OBJECT spawn teleport), and the deferred path is
 * limited to Honor Match rooms.
 */
final class NativeHonorRespawnBehaviorTest {
    private static final Path CONFIG = Path.of(
            "src/test/resources/server-config/config/server.selftest.properties");

    @TempDir Path temporary;

    @Test
    void exactRuleIsLimitedToHonorMatch() {
        assertEquals(5_000L, GameHub.HONOR_RESPAWN_DELAY_MS);
        assertEquals(
                "https://lineagecompendium.com/www.lineagecompendium.com/lito.html",
                GameHub.HONOR_RESPAWN_RULE_SOURCE);
        assertTrue(BattleRules.Mode.HONOR_MATCH.respawn());
    }

    @Test
    void honorDeathWaitsFiveSecondsAndResurrectsAtDeathCoordinatesWithoutSpawnMovement()
            throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        GameStateStore state = new GameStateStore(
                temporary.resolve("honor-respawn.properties"), false);
        String source = config.classicServerNames().get(0);
        GameHub hub = new GameHub(config, state);
        try (hub;
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept();
             ServerSocket victimListener = listener();
             Socket victimClient = connect(victimListener);
             Socket victimServer = victimListener.accept()) {
            ClientSession attacker = session(attackerServer, config, state, hub, "attacker");
            ClientSession victim = session(victimServer, config, state, hub, "victim");
            initialize(attacker, character("HonorBehaviorA", 0, 0x3813_8100L), source);
            initialize(victim, character("HonorBehaviorV", 1, 0x3813_8101L), source);
            // Live ciphers before the battle starts, so every frame on the wire stays decodable.
            // Both room members receive S_RESURRECT.
            setField(attacker, "outgoing", new Protocol.CipherState(0x3813_8100));
            int seed = 0x3813_8101;
            setField(victim, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            long roomId = enterBattle(hub, attacker, victim, BattleRules.Mode.HONOR_MATCH);
            victimClient.setSoTimeout(300);
            drain(victimClient, receiver);

            int deathX = 32755;
            int deathY = 32802;
            setField(victim, "positionX", deathX);
            setField(victim, "positionY", deathY);
            setField(victim, "playerMaxHp", 100);
            setField(victim, "playerHp", 0);
            setField(victim, "lastAttacker", attacker);
            setField(victim, "deathConfirmationPending", true);

            long confirmedAt = System.nanoTime();
            invoke(victim, "confirmDeath", new Class<?>[] {byte[].class},
                    (Object) new Protocol.Writer().c(Protocol.C_LITO_CONFIRM_DEATH).bytes());
            // Immediately after the confirmation the corpse stays where it fell.
            assertEquals(0, (int) field(victim, "playerHp"));
            assertEquals(deathX, (int) field(victim, "positionX"));
            assertEquals(deathY, (int) field(victim, "positionY"));
            assertFalse(hub.matchSnapshot(roomId).alivePlayers().contains(
                    victim.playerObjectId()));

            victimClient.setSoTimeout(7_000);
            List<byte[]> beforeResurrect = new ArrayList<>();
            byte[] resurrected = null;
            while (resurrected == null) {
                byte[] packet = receive(victimClient, receiver);
                if ((packet[0] & 0xFF) == Protocol.S_RESURRECT) {
                    resurrected = packet;
                } else {
                    beforeResurrect.add(packet);
                }
            }
            long elapsedMillis = (System.nanoTime() - confirmedAt) / 1_000_000L;
            assertTrue(elapsedMillis >= 4_900L,
                    "Honor resurrection must wait the native five seconds, was " + elapsedMillis);
            assertTrue(beforeResurrect.stream()
                            .noneMatch(packet -> (packet[0] & 0xFF) == Protocol.S_MOVE_OBJECT),
                    "no spawn movement may precede the Honor resurrection");

            assertEquals(100, (int) field(victim, "playerHp"));
            assertEquals(deathX, (int) field(victim, "positionX"));
            assertEquals(deathY, (int) field(victim, "positionY"));
            assertTrue(hub.matchSnapshot(roomId).alivePlayers().contains(
                    victim.playerObjectId()));

            victimClient.setSoTimeout(300);
            for (byte[] packet : drain(victimClient, receiver)) {
                assertFalse((packet[0] & 0xFF) == Protocol.S_MOVE_OBJECT,
                        "Honor same-position resurrection must not teleport to a spawn anchor");
            }
        }
    }

    @Test
    void deferredRespawnIsRefusedOutsideHonorMatch() throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        GameStateStore state = new GameStateStore(
                temporary.resolve("honor-respawn-gate.properties"), false);
        String source = config.classicServerNames().get(0);
        GameHub hub = new GameHub(config, state);
        try (hub;
             ServerSocket attackerListener = listener();
             Socket attackerClient = connect(attackerListener);
             Socket attackerServer = attackerListener.accept();
             ServerSocket victimListener = listener();
             Socket victimClient = connect(victimListener);
             Socket victimServer = victimListener.accept();
             ServerSocket honorAttackerListener = listener();
             Socket honorAttackerClient = connect(honorAttackerListener);
             Socket honorAttackerServer = honorAttackerListener.accept();
             ServerSocket honorVictimListener = listener();
             Socket honorVictimClient = connect(honorVictimListener);
             Socket honorVictimServer = honorVictimListener.accept()) {
            ClientSession attacker = session(attackerServer, config, state, hub, "gate-attacker");
            ClientSession victim = session(victimServer, config, state, hub, "gate-victim");
            initialize(attacker, character("HonorGateA", 0, 0x3813_8110L), source);
            initialize(victim, character("HonorGateV", 1, 0x3813_8111L), source);
            long deathmatch = enterBattle(hub, attacker, victim, BattleRules.Mode.DEATHMATCH);

            ClientSession honorAttacker = session(
                    honorAttackerServer, config, state, hub, "gate-honor-attacker");
            ClientSession honorVictim = session(
                    honorVictimServer, config, state, hub, "gate-honor-victim");
            initialize(honorAttacker, character("HonorGateHA", 0, 0x3813_8112L), source);
            initialize(honorVictim, character("HonorGateHV", 1, 0x3813_8113L), source);
            long honor = enterBattle(hub, honorAttacker, honorVictim, BattleRules.Mode.HONOR_MATCH);

            assertFalse(hub.scheduleHonorRespawn(deathmatch, victim, 1L, 32741, 32798),
                    "only Honor Match may defer a same-position resurrection");
            // Positive control: the same started-battle conditions are accepted for Honor Match.
            // The token is not pending, so the scheduled completion is a no-op.
            assertTrue(hub.scheduleHonorRespawn(honor, honorVictim, 1L, 32741, 32798));
        }
    }

    private long enterBattle(GameHub hub, ClientSession attacker, ClientSession target,
                             BattleRules.Mode mode) throws Exception {
        hub.connect(attacker);
        hub.connect(target);
        hub.enterChannel(0, attacker);
        hub.enterChannel(0, target);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Honor Behavior", "", 2, mode.wireId(), 0, 0, 0, 0));
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
    }

    private static void prepareBattle(ClientSession session, long roomId, int x, int y)
            throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver) throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 65_535));
    }

    private static List<byte[]> drain(Socket client, Protocol.CipherState receiver)
            throws Exception {
        List<byte[]> packets = new ArrayList<>();
        while (true) {
            try {
                packets.add(receive(client, receiver));
            } catch (SocketTimeoutException done) {
                return packets;
            }
        }
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static void invoke(ClientSession session, String name, Class<?>[] parameters,
                               Object... arguments) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
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
