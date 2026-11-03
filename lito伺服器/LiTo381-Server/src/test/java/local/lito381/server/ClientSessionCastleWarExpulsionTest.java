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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionCastleWarExpulsionTest {
    @TempDir Path temporary;

    @Test
    void beginExpelsOutsiderThroughNativePortalHandshakeButKeepsDefender()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config)) {
            GameStateStore state = new GameStateStore(
                    temporary.resolve("castle-war-expel.properties"), false);
            Fixture outsider = fixture(config, accounts, state, hub,
                    "test11", "TestPhysical", 0x3300_0001);
            Fixture defender = fixture(config, accounts, state, hub,
                    "test23", "TestDwarfMate", 0x3300_0002);
            try {
                ClassicWorldCastleWarClock.Event begin =
                        new ClassicWorldCastleWarClock.Event(
                                ClassicWorldCastleWarClock.EventKind.BEGIN,
                                6, "侏儒城", 1_000, 61_000);
                ClassicWorldCastleWarField.Point destination =
                        ClassicWorldCastleWarField.returnPoint(6, 2);

                assertTrue(outsider.session.serverClassicCastleWarExpel(
                        begin, destination));
                List<byte[]> expulsion = outsider.packets();
                assertTrue(expulsion.stream().anyMatch(packet ->
                        java.util.Arrays.equals(packet,
                                Protocol.effectObject(outsider.objectId(),
                                        ClassicWorldTeleportCatalog.TELEPORT_EFFECT_GFX))));
                assertTrue(expulsion.stream().anyMatch(packet ->
                        java.util.Arrays.equals(packet, Protocol.portal(
                                destination.mapId(), destination.x(), destination.y()))));
                assertEquals(66, hub.classicWorldLocation(outsider.session).mapId(),
                        "server position changes only after C_GOTO_PORTAL");

                outsider.completePortal();
                outsider.packets();
                GameHub.ClassicWorldLocation expelled =
                        hub.classicWorldLocation(outsider.session);
                assertEquals(List.of(destination.mapId(), destination.x(),
                                destination.y(), 5, -1),
                        List.of(expelled.mapId(), expelled.x(), expelled.y(),
                                expelled.heading(), expelled.instanceId()));

                // The defender legitimately observed the outsider's effect
                // and map-leave removal; isolate its own eligibility result.
                defender.packets();
                assertFalse(defender.session.serverClassicCastleWarExpel(
                        begin, destination));
                assertTrue(defender.packets().isEmpty());
                GameHub.ClassicWorldLocation retained =
                        hub.classicWorldLocation(defender.session);
                assertEquals(List.of(66, defender.startX(), defender.startY()),
                        List.of(retained.mapId(), retained.x(), retained.y()));
            } finally {
                outsider.close();
                defender.close();
            }
        }
    }

    @Test
    void enabledSecurityChargesOnceAndStagesOnlyOnePreSiegeExpulsion()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("castle-security-world.properties"), true);
        String sourceName = config.classicServerNames().get(0);
        long start = System.currentTimeMillis() - 1_000L;
        CastleSummary seed = new CastleSummary(
                6, "LegacyDwarf", "TestDwarfLord", "Dwarf",
                start, 10, 250_000);
        GameStateStore.CastleState castle = state.castle(sourceName, seed);
        assertTrue(state.saveCastleSecurity(sourceName, castle, true));

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state, accounts)) {
            Fixture outsider = fixture(config, accounts, state, hub,
                    "test11", "TestPhysical", 0x3300_0011);
            Fixture defender = fixture(config, accounts, state, hub,
                    "test23", "TestDwarfMate", 0x3300_0012);
            try {
                CastleWarSchedule schedule = new CastleWarSchedule(
                        6, "侏儒城", start, 1, 5);
                hub.registerClassicCastleWarSchedules(
                        sourceName, List.of(schedule));

                List<byte[]> outsiderPackets = outsider.packets();
                long portalCount = outsiderPackets.stream()
                        .filter(packet -> (packet[0] & 0xFF) == Protocol.S_PORTAL)
                        .count();
                assertEquals(1, portalCount,
                        "paid pre-siege service and ordinary BEGIN must not double-stage");
                assertEquals(150_000,
                        state.castle(sourceName, seed).publicMoney());
                assertTrue(defender.packets().stream().noneMatch(packet ->
                        (packet[0] & 0xFF) == Protocol.S_PORTAL));

                // Re-registering the same active schedule cannot emit another
                // BEGIN or charge the durable settlement a second time.
                hub.registerClassicCastleWarSchedules(
                        sourceName, List.of(schedule));
                assertEquals(150_000,
                        state.castle(sourceName, seed).publicMoney());
                assertTrue(outsider.packets().isEmpty());
            } finally {
                outsider.close();
                defender.close();
            }
        }
    }

    private Fixture fixture(ServerConfig config, AccountRepository accounts,
                            GameStateStore state, GameHub hub,
                            String account, String characterName, int seed)
            throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        ClientSession session = new ClientSession(
                server, ClientSession.Endpoint.GAME, config, accounts, state,
                new CaptureLog(temporary.resolve("capture-" + account)), hub);
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", account);
        setField(session, "selectedCharacter", characterName);
        CharacterSummary character = accounts.characters(account).stream()
                .filter(candidate -> candidate.name().equals(characterName))
                .findFirst().orElseThrow();
        ClassicWorldCastleWarField.Point staging = dwarfWarPoint();
        setField(session, "characters", List.of(character));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7700_0000L + seed));
        setField(session, "classicWorldMapId", 66);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", staging.x());
        setField(session, "positionY", staging.y());
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", character.level());
        hub.connect(session);
        hub.enterClassicWorld(session, 66, staging.x(), staging.y(), 0);
        return new Fixture(client, server, session, receiver,
                character.objectId(), staging.x(), staging.y());
    }

    private static ClassicWorldCastleWarField.Point dwarfWarPoint() {
        ClassicWorldCastleWarField.Area area =
                ClassicWorldCastleWarField.area(6);
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(area.mapId());
        for (int x = area.x1(); x <= area.x2(); x++) {
            for (int y = area.y1(); y <= area.y2(); y++) {
                if (map.standingPassable(x, y)
                        && !ClassicWorldNpcCatalog.occupies(area.mapId(), x, y)) {
                    return new ClassicWorldCastleWarField.Point(x, y, area.mapId());
                }
            }
        }
        throw new AssertionError("no passable Dwarf castle war tile");
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

    private record Fixture(Socket client, Socket server, ClientSession session,
                           Protocol.CipherState receiver, long objectId,
                           int startX, int startY) {
        void completePortal() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completePortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session,
                    (Object) new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 8192);
                    if (encrypted == null) break;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException timeout) {
                    break;
                }
            }
            return result;
        }

        void close() throws Exception {
            hubLeave();
            client.close();
            server.close();
        }

        private void hubLeave() {
            // ClientSession's socket is not running a reader thread in this
            // direct packet fixture; its hub closes after both fixtures.
        }
    }
}
