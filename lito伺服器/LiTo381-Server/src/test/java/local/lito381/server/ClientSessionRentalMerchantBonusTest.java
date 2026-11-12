package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #371: renting a retained merchant item in the rental town and
 * equipping it through the real {@code C_USE_ITEM} path must publish the
 * item's attributes in the status packets, exactly once, and removing it
 * must restore the previous values.
 */
final class ClientSessionRentalMerchantBonusTest {
    private static final int LOVE_RESOURCE_INDEX = 12;
    private static final int KRI_RESOURCE_INDEX = 11;

    @TempDir Path temporary;

    @Test void equippedMerchantRingAndCloakPublishTheirAttributesAndRestoreOnRemoval()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("merchant-bonus-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("merchant-bonus-capture")), hub);
            CharacterSummary character = new CharacterSummary(
                    "MerchantBonus", "", 1, 0, 50, 500, 100,
                    10, 13, 10, 10, 9, 8, 11, 8, 11,
                    0x5000_0371L, 0, 0, 0, 0, 0, 0, 0);
            setPhase(session, "BATTLE");
            setField(session, "selectedCharacter", character.name());
            setField(session, "characters", List.of(character));
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "currentRoomId", 0x5000_0001L);
            setField(session, "rentalTownLightGemCount", 20L);
            int seed = 0x3810_0371;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            int baseArmorClass = NumericBreakthrough.clampArmorClass(
                    (long) character.armorClass());

            // 29047 fire-salamander ring: fire resistance 10, armor class 0.
            RuntimeInventory.Item ring = rent(session, client, receiver,
                    LOVE_RESOURCE_INDEX, 29047);
            assertFalse(ring.equipped());

            use(session, ring);
            byte[] equippedStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS);
            byte[] equippedArmorClass = receiveUntilOpcode(
                    client, receiver, Protocol.S_AC);
            byte[] expectedEquippedArmorClass =
                    Protocol.armorClass(baseArmorClass, 10, 0, 0, 0);
            assertArrayEquals(expectedEquippedArmorClass, equippedArmorClass,
                    "S_AC must carry fire resistance 10 exactly once");
            assertArrayEquals(resistanceTail(expectedEquippedArmorClass),
                    tail(equippedStatus,
                            resistanceTail(expectedEquippedArmorClass).length),
                    "S_STATUS must end with fire/water/wind/earth = 10/0/0/0");
            assertArrayEquals(Protocol.magicStatus(0, 0), receiveUntilOpcode(
                    client, receiver, Protocol.S_MAGIC_STATUS));
            assertTrue(rentalTownItem(session, ring.objectId()).equipped());

            // 29032 gorgon-leather cloak: spell power 10, native AC 12.
            RuntimeInventory.Item cloak = rent(session, client, receiver,
                    KRI_RESOURCE_INDEX, 29032);
            use(session, cloak);
            int cloakArmorClass = NumericBreakthrough.clampArmorClass(
                    (long) character.armorClass() - 12);
            assertArrayEquals(
                    Protocol.armorClass(cloakArmorClass, 10, 0, 0, 0),
                    receiveUntilOpcode(client, receiver, Protocol.S_AC),
                    "ring fire resistance stays 10 and cloak AC applies once");
            assertArrayEquals(Protocol.magicStatus(10, 0), receiveUntilOpcode(
                    client, receiver, Protocol.S_MAGIC_STATUS),
                    "S_MAGIC_STATUS must carry spell power 10 exactly once");

            // Removing the ring restores fire resistance; the cloak stays.
            use(session, rentalTownItem(session, ring.objectId()));
            byte[] removedStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS);
            byte[] expectedRemovedArmorClass =
                    Protocol.armorClass(cloakArmorClass, 0, 0, 0, 0);
            assertArrayEquals(expectedRemovedArmorClass, receiveUntilOpcode(
                    client, receiver, Protocol.S_AC));
            assertArrayEquals(resistanceTail(expectedRemovedArmorClass),
                    tail(removedStatus,
                            resistanceTail(expectedRemovedArmorClass).length));
            assertArrayEquals(Protocol.magicStatus(10, 0), receiveUntilOpcode(
                    client, receiver, Protocol.S_MAGIC_STATUS));
            assertFalse(rentalTownItem(session, ring.objectId()).equipped());

            // Removing the cloak restores the selected character baseline.
            use(session, rentalTownItem(session, cloak.objectId()));
            assertArrayEquals(Protocol.armorClass(baseArmorClass, 0, 0, 0, 0),
                    receiveUntilOpcode(client, receiver, Protocol.S_AC));
            assertArrayEquals(Protocol.magicStatus(0, 0), receiveUntilOpcode(
                    client, receiver, Protocol.S_MAGIC_STATUS));
            assertEquals(RentalEquipmentBonuses.none(),
                    ((RuntimeInventory) field(session, "rentalTownInventory"))
                            .equippedRentalBonuses());
        } finally {
            hub.close();
        }
    }

    /** Rents one item through C_HACTION + C_BUY_SELL at its retained merchant. */
    private static RuntimeInventory.Item rent(
            ClientSession session, Socket client, Protocol.CipherState receiver,
            int merchantResourceIndex, int sourceItemId) throws Exception {
        NativeLitoRentalTownNpcCatalog.Npc merchant =
                NativeLitoRentalTownNpcCatalog.forMap(500).stream()
                        .filter(npc -> npc.resourceIndex() == merchantResourceIndex)
                        .findFirst().orElseThrow();
        List<NativeLitoRentalMerchantCatalog.Item> offered =
                NativeLitoRentalMerchantCatalog.offeredBy(merchantResourceIndex);
        int order = -1;
        for (int index = 0; index < offered.size(); index++) {
            if (offered.get(index).sourceItemId() == sourceItemId) order = index;
        }
        assertTrue(order >= 0, "merchant does not offer " + sourceItemId);
        setField(session, "positionX", merchant.x());
        setField(session, "positionY", merchant.y());
        handle(session, new Protocol.Writer().c(Protocol.C_HACTION)
                .d(merchant.objectId())
                .s(NativeLitoRentalTownNpcCatalog.RENT_ACTION).s("").bytes());
        receiveUntilOpcode(client, receiver, Protocol.S_BUY_LIST);
        handle(session, new Protocol.Writer()
                .c(Protocol.C_BUY_SELL).d(merchant.objectId())
                .c(0).c(1).c(0).d(order).d(1).bytes());
        RuntimeInventory.Item rented =
                ((RuntimeInventory) field(session, "rentalTownInventory"))
                        .items().stream()
                        .filter(item -> item.definition().catalogId() == sourceItemId)
                        .findFirst().orElseThrow();
        assertArrayEquals(Protocol.addInventory(rented.wireItem()),
                receiveUntilOpcode(client, receiver, Protocol.S_ADD_INVENTORY));
        return rented;
    }

    private static void use(ClientSession session, RuntimeInventory.Item item)
            throws Exception {
        handle(session, new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(item.objectId()).s("").bytes());
    }

    private static RuntimeInventory.Item rentalTownItem(
            ClientSession session, long objectId) throws Exception {
        return ((RuntimeInventory) field(session, "rentalTownInventory"))
                .item(objectId);
    }

    /** The fire/water/wind/earth block shared by S_AC and the end of S_STATUS. */
    private static byte[] resistanceTail(byte[] armorClassPacket) {
        int head = 1 + (NumericBreakthrough.enabled() ? 4 : 1);
        return Arrays.copyOfRange(armorClassPacket, head, armorClassPacket.length);
    }

    private static byte[] tail(byte[] packet, int length) {
        return Arrays.copyOfRange(packet, packet.length - length, packet.length);
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver, int opcode)
            throws Exception {
        for (int index = 0; index < 32; index++) {
            byte[] packet = receiver.decrypt(
                    Protocol.readFrame(client.getInputStream(), 8_192));
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode);
    }

    private static void handle(ClientSession session, byte[] packet) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) packet);
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
