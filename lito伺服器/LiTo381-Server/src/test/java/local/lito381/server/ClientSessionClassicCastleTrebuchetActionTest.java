package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicCastleTrebuchetActionTest {
    @TempDir Path temporary;

    @Test
    void worldHypertextDispatcherRoutesDynamicTrebuchetToNpcActionDomain()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("trebuchet-action.properties"), false);
        try (GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(temporary.resolve("capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    capture, hub);
            Protocol.CipherState receiver =
                    new Protocol.CipherState(0x3456_789A);
            setField(session, "outgoing",
                    new Protocol.CipherState(0x3456_789A));
            setEnumField(session, "phase", "WORLD");
            setField(session, "account", "test26");
            setField(session, "selectedCharacter", "TestSiegeLord");
            setField(session, "characters", List.of(character()));
            setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                    Map.of(), () -> 0x7100_1001L));
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "classicWorldMapId", 4);

            String source = config.classicServerNames().get(0);
            setField(session, "selectedClassicServerName", source);
            long start = System.currentTimeMillis() - 60_000L;
            hub.registerClassicCastleWarSchedules(source, List.of(
                    new CastleWarSchedule(4, "奇岩城", start, 10, 20)));
            ClassicWorldCastleWarField.WarObject trebuchet =
                    hub.classicCastleWarObjects(source).stream()
                            .filter(object -> object.kind()
                                    == ClassicWorldCastleWarField.Kind.TREBUCHET)
                            .filter(object -> object.npcId()
                                    == ClassicWorldCastleWarField
                                            .ATTACK_TREBUCHET_NPC_ID)
                            .findFirst().orElseThrow();
            setField(session, "positionX", trebuchet.x());
            setField(session, "positionY", trebuchet.y() - 1);

            byte[] action = "3".getBytes(Charset.forName("windows-950"));
            byte[] payload = new byte[7 + action.length];
            payload[0] = 0x27;
            putDword(payload, 1, trebuchet.objectId());
            System.arraycopy(action, 0, payload, 5, action.length);
            payload[5 + action.length] = 0;
            payload[6 + action.length] = 0;
            invokeHypertextAction(session, payload);

            byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
            assertArrayEquals(Protocol.questHelpMessage(
                            ClassicWorldTrebuchetCatalog.CLIENT_NO_AMMO_MESSAGE,
                            trebuchet.x(), trebuchet.y() - 1),
                    receiver.decrypt(encrypted));
        }
    }

    @Test
    void retainedShotsConsumeOnlyRealShellsDamageWorldPlayersAndKeepVisualBug()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("trebuchet-live-action.properties"), false);
        String source = config.classicServerNames().get(0);
        long start = System.currentTimeMillis() - 60_000L;
        try (GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("live-action-capture"))) {
            hub.registerClassicCastleWarSchedules(source, List.of(
                    new CastleWarSchedule(4, "奇岩城", start, 10, 20)));
            ClassicWorldCastleWarField.WarObject attack =
                    hub.classicCastleWarObjects(source).stream()
                            .filter(object -> object.kind()
                                    == ClassicWorldCastleWarField.Kind.TREBUCHET)
                            .filter(object -> object.npcId()
                                    == ClassicWorldCastleWarField
                                            .ATTACK_TREBUCHET_NPC_ID)
                            .findFirst().orElseThrow();
            ClassicWorldCastleWarField.WarObject defence =
                    hub.classicCastleWarObjects(source).stream()
                            .filter(object -> object.kind()
                                    == ClassicWorldCastleWarField.Kind.TREBUCHET)
                            .filter(object -> object.npcId()
                                    == ClassicWorldCastleWarField
                                            .DEFENCE_TREBUCHET_NPC_ID)
                            .findFirst().orElseThrow();

            try (SessionSocket operator = openWorldSession(
                        config, state, capture, hub, 0x1111_0001,
                        "test26", "TestSiegeLord", 0x7100_0001L,
                        attack.x(), attack.y() - 1, 1_000,
                        Map.of(InventoryCatalog.CASTLE_TREBUCHET_SHELL, 3));
                 SessionSocket victim = openWorldSession(
                        config, state, capture, hub, 0x2222_0002,
                        "test5", "TestElf", 0x7100_0002L,
                        33_631, 32_731, 1_000, Map.of())) {
                invokeHypertextAction(operator.session(),
                        hypertextAction(attack.objectId(), "3"));
                List<byte[]> operatorPackets = drain(operator);
                List<byte[]> victimPackets = drain(victim);
                assertLocationEffect(victimPackets, 33_629, 33_634,
                        32_730, 32_733,
                        ClassicWorldTrebuchetCatalog.ATTACK_EFFECT_GFX);
                assertTrue(containsPacket(victimPackets,
                        Protocol.objectAction(attack.objectId(), 1)));
                assertTrue(containsPacket(victimPackets, new Protocol.Writer()
                        .c(Protocol.S_HIT_POINT).h(700).h(1_000).bytes()));
                assertTrue(containsPacket(victimPackets, Protocol.objectAction(
                        0x7100_0002L, BattleActionCodes.DAMAGE)));
                assertEquals(2, shellCount(operator.session()));
                assertEquals(700, intField(victim.session(), "playerHp"));

                // The retained gate applies before action resolution and the
                // attack-side ten-second lease rejects the immediate repeat.
                invokeHypertextAction(operator.session(),
                        hypertextAction(attack.objectId(), "3"));
                operatorPackets = drain(operator);
                assertTrue(containsPacket(
                        operatorPackets, Protocol.messageCode(3_680)));
                assertTrue(drain(victim).isEmpty());
                assertEquals(2, shellCount(operator.session()));
                assertEquals(700, intField(victim.session(), "playerHp"));

                // Source action 1-14 still requires a normal shell, then tries
                // to consume nonexistent 46282 and ignores failure.  It is a
                // GFX-2177 visual only, not an invented silence status.
                setField(operator.session(), "attackTrebuchetAvailableNanos", 0L);
                invokeHypertextAction(operator.session(),
                        hypertextAction(attack.objectId(), "1-14"));
                drain(operator);
                victimPackets = drain(victim);
                assertTrue(containsPacket(victimPackets, Protocol.effectObject(
                        0x7100_0002L,
                        ClassicWorldTrebuchetCatalog.SILENCE_EFFECT_GFX)));
                assertEquals(2, shellCount(operator.session()));
                assertEquals(700, intField(victim.session(), "playerHp"));

                moveWorld(hub, victim.session(), 33_631, 32_677);
                setField(operator.session(), "attackTrebuchetAvailableNanos", 0L);
                invokeHypertextAction(operator.session(),
                        hypertextAction(attack.objectId(), "1-15"));
                drain(operator);
                victimPackets = drain(victim);
                assertTrue(containsPacket(victimPackets, Protocol.effectObject(
                        0x7100_0002L,
                        ClassicWorldTrebuchetCatalog.SILENCE_EFFECT_GFX)));
                assertEquals(2, shellCount(operator.session()));
                assertEquals(700, intField(victim.session(), "playerHp"));

                setField(operator.session(), "attackTrebuchetAvailableNanos", 0L);
                invokeHypertextAction(operator.session(),
                        hypertextAction(attack.objectId(), "4"));
                drain(operator);
                victimPackets = drain(victim);
                assertLocationEffect(victimPackets, 33_629, 33_634,
                        32_675, 32_680,
                        ClassicWorldTrebuchetCatalog.ATTACK_EFFECT_GFX);
                assertEquals(1, shellCount(operator.session()));
                assertEquals(400, intField(victim.session(), "playerHp"));

                moveWorld(hub, operator.session(), defence.x(), defence.y() - 1);
                moveWorld(hub, victim.session(), 33_631, 32_736);
                invokeHypertextAction(operator.session(),
                        hypertextAction(defence.objectId(), "9"));
                drain(operator);
                victimPackets = drain(victim);
                assertLocationEffect(victimPackets, 33_629, 33_634,
                        32_735, 32_738,
                        ClassicWorldTrebuchetCatalog.DEFENCE_EFFECT_GFX);
                assertEquals(0, shellCount(operator.session()));
                assertEquals(100, intField(victim.session(), "playerHp"));

                GameStateStore.Economy durable = state.economy(
                        GameStateStore.sourceCharacter(source, "TestSiegeLord"));
                assertEquals(0, durable.inventory().getOrDefault(
                        InventoryCatalog.CASTLE_TREBUCHET_SHELL, 0));
            }
        }
    }

    private static SessionSocket openWorldSession(
            ServerConfig config, GameStateStore state, CaptureLog capture,
            GameHub hub, int cipherSeed, String account, String character,
            long objectId, int x, int y, int hp,
            Map<Long, Integer> inventory) throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(75);
        ClientSession session = new ClientSession(
                server, ClientSession.Endpoint.GAME, config, null, state,
                capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(cipherSeed);
        setField(session, "outgoing", new Protocol.CipherState(cipherSeed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", account);
        setField(session, "selectedCharacter", character);
        setField(session, "characters", List.of(character(character, objectId, hp)));
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                inventory, () -> objectId + 0x100));
        setField(session, "playerHp", hp);
        setField(session, "playerMaxHp", hp);
        setField(session, "classicWorldMapId", 4);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "selectedClassicServerName",
                config.classicServerNames().get(0));
        hub.connect(session);
        hub.enterClassicWorld(session, 4, x, y, 0);
        return new SessionSocket(session, client, server, receiver, hub);
    }

    private static void moveWorld(
            GameHub hub, ClientSession session, int x, int y) throws Exception {
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        assertTrue(hub.updateClassicWorld(session, 4, x, y, 0));
    }

    private static List<byte[]> drain(SessionSocket fixture) throws Exception {
        List<byte[]> packets = new ArrayList<>();
        while (true) {
            try {
                byte[] encrypted = Protocol.readFrame(
                        fixture.client().getInputStream(), 65_535);
                packets.add(fixture.receiver().decrypt(encrypted));
            } catch (SocketTimeoutException done) {
                return packets;
            }
        }
    }

    private static void assertLocationEffect(
            List<byte[]> packets, int minimumX, int maximumX,
            int minimumY, int maximumY, int gfx) {
        assertTrue(packets.stream().anyMatch(packet -> packet.length == 7
                && packet[0] == (byte) Protocol.S_EFFECT_LOC
                && unsignedShort(packet, 1) >= minimumX
                && unsignedShort(packet, 1) <= maximumX
                && unsignedShort(packet, 3) >= minimumY
                && unsignedShort(packet, 3) <= maximumY
                && unsignedShort(packet, 5) == gfx),
                () -> packets.stream().map(java.util.HexFormat.of()::formatHex)
                        .toList().toString());
    }

    private static int unsignedShort(byte[] source, int offset) {
        return (source[offset] & 0xFF) | (source[offset + 1] & 0xFF) << 8;
    }

    private static boolean containsPacket(
            List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected));
    }

    private static int shellCount(ClientSession session) throws Exception {
        return ((RuntimeInventory) field(session, "inventory"))
                .catalogCounts().getOrDefault(
                        InventoryCatalog.CASTLE_TREBUCHET_SHELL, 0);
    }

    private static int intField(Object target, String name) throws Exception {
        return (int) field(target, name);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private record SessionSocket(
            ClientSession session, Socket client, Socket server,
            Protocol.CipherState receiver, GameHub hub) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            hub.disconnect(session);
            client.close();
            server.close();
        }
    }

    private static CharacterSummary character() {
        return character("TestSiegeLord");
    }

    private static CharacterSummary character(String name) {
        return character(name, 3, 100);
    }

    private static CharacterSummary character(String name, long objectId, int hp) {
        return new CharacterSummary(name, "", 0, 0,
                27, hp, 100, 0, 10, 10, 10, 10, 10, 10, 10, 10,
                objectId, 0, 0, 0, 3, 33051, 32337, 4,
                8, 40, 1, 8, 8, 0);
    }

    private static void invokeHypertextAction(
            ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "hypertextAction", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static byte[] hypertextAction(long objectId, String action) {
        byte[] encoded = action.getBytes(Charset.forName("windows-950"));
        byte[] payload = new byte[7 + encoded.length];
        payload[0] = 0x27;
        putDword(payload, 1, objectId);
        System.arraycopy(encoded, 0, payload, 5, encoded.length);
        payload[5 + encoded.length] = 0;
        payload[6 + encoded.length] = 0;
        return payload;
    }

    private static void putDword(byte[] destination, int offset, long value) {
        for (int index = 0; index < 4; index++) {
            destination[offset + index] = (byte) (value >>> (index * 8));
        }
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf(
                (Class<? extends Enum>) field.getType(), value));
    }
}
