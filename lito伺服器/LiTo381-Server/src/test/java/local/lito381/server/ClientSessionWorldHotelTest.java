package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldHotelTest {
    @TempDir Path temporary;

    @Test
    void encryptedNativeRoomRentalEntryExitAndReturnRoundTrip()
            throws Exception {
        Fixture fixture = fixture(1, "HotelKnight");
        ClassicWorldNpcCatalog.StaticNpc npc = innkeeper(70012);
        try {
            fixture.open(npc.objectId());
            assertTrue(contains(fixture.packets(), Protocol.hypertext(
                    npc.objectId(), ClassicWorldHotelCatalog.INITIAL_HTML,
                    "", List.of())));

            fixture.action(npc.objectId(), ClassicWorldHotelCatalog.ROOM_ACTION);
            assertArrayEquals(Protocol.hypertextInput(
                            npc.objectId(), ClassicWorldHotelCatalog.PRICE_PER_KEY,
                            1, 1, ClassicWorldHotelCatalog.ROOM_KEY_MAXIMUM, 0,
                            ClassicWorldHotelCatalog.ROOM_INPUT_HTML, "", "",
                            List.of(npc.nameId(), "300")),
                    fixture.onlyPacket());

            fixture.input(npc.objectId(), 2);
            List<byte[]> rentedPackets = fixture.packets();
            assertTrue(contains(rentedPackets, Protocol.hypertext(
                    npc.objectId(), ClassicWorldHotelCatalog.RENTED_HTML,
                    "", List.of(npc.nameId()))));
            RuntimeInventory inventory = fixture.inventory();
            assertEquals(99_400,
                    inventory.catalogCounts().get(InventoryCatalog.ADENA));
            assertEquals(2,
                    inventory.catalogCounts().get(InventoryCatalog.HOTEL_KEY));
            Set<Long> keyIds = inventory.items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.HOTEL_KEY)
                    .map(RuntimeInventory.Item::objectId)
                    .collect(java.util.stream.Collectors.toSet());
            GameStateStore.HotelLeaseState lease = fixture.state()
                    .activeHotelLeaseForInventory(npc.npcId(), keyIds);
            assertNotNull(lease);
            assertFalse(lease.meetingHall());

            fixture.action(npc.objectId(), ClassicWorldHotelCatalog.ENTER_ACTION);
            assertArrayEquals(Protocol.portal(16384, 32745, 32803),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            fixture.assertLocation(16384, 32745, 32803, 6, lease.showId());

            fixture.enterPortal(32746, 32803);
            assertArrayEquals(Protocol.portal(0, 32599, 32931),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            fixture.assertLocation(0, 32599, 32931, 1, -1);

            fixture.action(npc.objectId(), ClassicWorldHotelCatalog.RETURN_ACTION);
            List<byte[]> returnedPackets = fixture.packets();
            assertTrue(contains(returnedPackets, Protocol.hypertext(
                    npc.objectId(), ClassicWorldHotelCatalog.RETURNED_HTML, "",
                    List.of(npc.nameId(), "100"))));
            inventory = fixture.inventory();
            assertEquals(99_500,
                    inventory.catalogCounts().get(InventoryCatalog.ADENA));
            assertFalse(inventory.catalogCounts().containsKey(
                    InventoryCatalog.HOTEL_KEY));
            assertEquals(0, fixture.state().hotelOccupancy(
                    npc.npcId(), fixture.session().sourceCharacterIdentity())
                    .activeRooms());
        } finally {
            fixture.close();
        }
    }

    @Test
    void onlyCrownReceivesTheNativeSixteenKeyHallInput()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc npc = innkeeper(70012);
        Fixture knight = fixture(1, "HallKnight");
        try {
            knight.action(npc.objectId(), ClassicWorldHotelCatalog.HALL_ACTION);
            assertArrayEquals(Protocol.hypertext(
                            npc.objectId(),
                            ClassicWorldHotelCatalog.HALL_CROWN_ONLY_HTML,
                            "", List.of()),
                    knight.onlyPacket());
        } finally {
            knight.close();
        }

        Fixture crown = fixture(0, "HallCrown");
        try {
            crown.action(npc.objectId(), ClassicWorldHotelCatalog.HALL_ACTION);
            assertArrayEquals(Protocol.hypertextInput(
                            npc.objectId(), ClassicWorldHotelCatalog.PRICE_PER_KEY,
                            1, 1, ClassicWorldHotelCatalog.HALL_KEY_MAXIMUM, 0,
                            ClassicWorldHotelCatalog.HALL_INPUT_HTML, "", "",
                            List.of(npc.nameId(), "300")),
                    crown.onlyPacket());
        } finally {
            crown.close();
        }
    }

    private Fixture fixture(int characterType, String name) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve(name + "-hotel.properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve(name + "-capture"));
        ClientSession session = new ClientSession(
                server, ClientSession.Endpoint.GAME, config,
                null, state, capture, hub);
        int seed = 0x3810_7001 + characterType;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", name.toLowerCase());
        setField(session, "selectedCharacter", name);
        ArenaMapCatalog.Point start = passableNear(0, 32596, 32929);
        CharacterSummary selected = character(
                name, characterType, start.x(), start.y());
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong ids = new AtomicLong(0x7200_0000L + characterType * 0x1000L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.ADENA, 100_000),
                ids::incrementAndGet));
        setField(session, "playerHp", 300);
        setField(session, "playerMaxHp", 300);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 80);
        setField(session, "classicWorldMapId", 0);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", start.x());
        setField(session, "positionY", start.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 0, start.x(), start.y(), 0, -1);
        return new Fixture(hub, state, capture, client, server, session, receiver);
    }

    private static CharacterSummary character(
            String name, int type, int x, int y) {
        return new CharacterSummary(name, "", type, 0, 80,
                300, 100, 0, 14, 14, 12, 12, 12, 12, 14, 14,
                0x7200_7001L + type, 0, 0, 0, 0,
                x, y, 0, 8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point passableNear(
            int mapId, int centerX, int centerY) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        for (int radius = 1; radius <= 5; radius++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int x = centerX - radius; x <= centerX + radius; x++) {
                    if (map.standingPassable(x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new IllegalStateException("innkeeper has no passable interaction tile");
    }

    private static ClassicWorldNpcCatalog.StaticNpc innkeeper(int npcId) {
        ClassicWorldHotelCatalog.Inn inn = ClassicWorldHotelCatalog.forNpc(npcId);
        return ClassicWorldNpcCatalog.npc(
                inn.exteriorMapId(), inn.objectId());
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet -> Arrays.equals(packet, expected));
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(
            Object target, String name, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf(
                (Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(
            GameHub hub, GameStateStore state, CaptureLog capture,
            Socket client, Socket server, ClientSession session,
            Protocol.CipherState receiver) {
        RuntimeInventory inventory() throws Exception {
            return (RuntimeInventory) field(session, "inventory");
        }

        void open(long objectId) throws Exception {
            invoke("openClassicWorldHypertext", new Class<?>[] {long.class},
                    objectId);
        }

        void action(long objectId, String action) throws Exception {
            invoke("handleClassicWorldNpcAction",
                    new Class<?>[] {Protocol.HypertextActionRequest.class},
                    new Protocol.HypertextActionRequest(
                            objectId, action, "",
                            Protocol.HypertextActionKind.GENERIC));
        }

        void input(long objectId, int amount) throws Exception {
            invoke("hypertextInputResult", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_HYPERTEXT_INPUT_RESULT)
                            .d(objectId).d(amount).c(0).s("").bytes());
        }

        void enterPortal(int x, int y) throws Exception {
            invoke("enterPortal", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_ENTER_PORTAL)
                            .h(x).h(y).bytes());
        }

        void completePortal() throws Exception {
            invoke("completePortal", new Class<?>[] {byte[].class},
                    new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        private void invoke(String methodName, Class<?>[] types, Object argument)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            method.invoke(session, argument);
        }

        byte[] onlyPacket() throws Exception {
            List<byte[]> packets = packets();
            assertEquals(1, packets.size());
            return packets.get(0);
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 65_535);
                    if (encrypted == null) return result;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
        }

        void assertLocation(
                int mapId, int x, int y, int heading, int instanceId)
                throws Exception {
            assertEquals(mapId, intField(session, "classicWorldMapId"));
            assertEquals(x, intField(session, "positionX"));
            assertEquals(y, intField(session, "positionY"));
            assertEquals(heading, intField(session, "playerHeading"));
            assertEquals(instanceId,
                    intField(session, "classicWorldInstanceId"));
            GameHub.ClassicWorldLocation location = hub.classicWorldLocation(session);
            assertEquals(List.of(mapId, x, y, heading, instanceId), List.of(
                    location.mapId(), location.x(), location.y(),
                    location.heading(), location.instanceId()));
        }

        void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }
}
