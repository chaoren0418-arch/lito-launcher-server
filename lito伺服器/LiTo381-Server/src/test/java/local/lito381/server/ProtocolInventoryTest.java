package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ProtocolInventoryTest {
    private static final Protocol.InventoryWireItem SWORD = new Protocol.InventoryWireItem(
            0x7000_0001L, 1, 0, 24, 1, 2, 1,
            "Sword", new byte[] {(byte) 0xAA, (byte) 0xBB});

    @Test void writesNativeSingleAndBatchInventoryRecords() {
        byte[] record = new byte[] {
                0x01, 0x00, 0x00, 0x70,
                0x01, 0x00, 0x18, 0x00, 0x01,
                0x02, 0x00, 0x00, 0x00, 0x01,
                'S', 'w', 'o', 'r', 'd', 0x00,
                0x02, (byte) 0xAA, (byte) 0xBB
        };
        assertArrayEquals(concat(new byte[] {0x16}, record), Protocol.addInventory(SWORD));
        assertArrayEquals(concat(new byte[] {0x7C, 0x01}, record),
                Protocol.inventoryBatch(List.of(SWORD)));
    }

    @Test void writesExactNativeLiToTransferStoneAndDefaultUseContract() {
        Protocol.InventoryWireItem stone = Protocol.litoTransferStone(0x6C00_0001L);
        assertEquals(20_155, stone.icon());
        assertEquals(0, stone.useType());
        assertEquals("傳送石", stone.name());
        assertArrayEquals(hex(
                "16 01 00 00 6C 00 00 BB 4E 01 01 00 00 00 01"
                        + " B6 C7 B0 65 A5 DB 00 00"),
                Protocol.addInventory(stone));

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(stone.objectId()).s("").bytes();
        assertArrayEquals(hex("1C 01 00 00 6C 00"), request);
        assertEquals(new Protocol.InventoryUse(0x6C00_0001L, ""),
                Protocol.defaultInventoryUse(request));
    }

    @Test void writesNativeRentalVillageResourcePresentationRecords() {
        Protocol.InventoryWireItem gem = Protocol.litoRentalLightGem(
                0x6C00_0002L, 20);
        assertEquals(20_005, gem.icon());
        assertEquals(20, gem.count());
        assertEquals(0, gem.useType());
        assertEquals("光之寶石", gem.name());

        Protocol.InventoryWireItem map = Protocol.litoRentalTownMap(0x6C00_0003L);
        assertEquals(20_128, map.icon());
        assertEquals(1, map.count());
        assertEquals(Protocol.LITO_TOWN_MAP_USE_TYPE_POLICY, map.useType());
        assertEquals(0, Protocol.LITO_TOWN_MAP_USE_TYPE_POLICY);
        assertEquals(false, Protocol.LITO_TOWN_MAP_HISTORICAL_USE_TYPE_EXACT);
        assertEquals(false, Protocol.LITO_TOWN_MAP_HISTORICAL_CLICK_RESPONSE_AVAILABLE);
        assertEquals(true, Protocol.LITO_TOWN_MAP_SELECTED_CLIENT_RESPONSE_EXACT);
        assertEquals("無界擂台地圖", map.name());
        assertArrayEquals(hex(
                "16 03 00 00 6C 00 00 A0 4E 01 01 00 00 00 01"
                        + " B5 4C AC C9 BE DD A5 78 A6 61 B9 CF 00 00"),
                Protocol.addInventory(map));

        byte[] mapRequest = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(map.objectId()).s("").bytes();
        assertArrayEquals(hex("1C 03 00 00 6C 00"), mapRequest);
        assertEquals(new Protocol.InventoryUse(0x6C00_0003L, ""),
                Protocol.defaultInventoryUse(mapRequest));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRentalLightGem(0x6C00_0002L, 0));
    }

    @Test void writesNativeInventoryMutationPackets() {
        assertArrayEquals(hex("17 01 00 00 70"), Protocol.removeInventory(0x7000_0001L));
        assertArrayEquals(hex("18 01 00 00 70 58 00"),
                Protocol.changeInventoryDescription(0x7000_0001L, "X"));
        assertArrayEquals(hex("19 01 00 00 70 01"),
                Protocol.changeInventoryCharge(0x7000_0001L, 1));
        assertArrayEquals(hex("26 01 01"), Protocol.changeAbility(1, true));
        assertArrayEquals(hex("26 02 00"), Protocol.changeAbility(2, false));
        assertArrayEquals(hex("26 05 01"), Protocol.changeAbility(5, true));
        assertArrayEquals(hex("1A 01 00 00 70 03 00 00 00"),
                Protocol.changeInventoryCount(0x7000_0001L, 3));
        assertArrayEquals(hex("0E 01 00 00 70 02"),
                Protocol.changeInventoryBlessing(0x7000_0001L, 2));
        assertArrayEquals(hex("0E 01 00 00 70 82"),
                Protocol.changeInventoryBlessing(0x7000_0001L, 130));
        assertArrayEquals(hex("77 44 33 22 11 08 07"),
                Protocol.underwaterBreath(0x1122_3344L, 1_800));
        assertArrayEquals(hex("77 44 33 22 11 FF FF"),
                Protocol.underwaterBreath(0x1122_3344L, -1));
        assertArrayEquals(hex("1D 44 33 22 11 04"),
                Protocol.wield(0x1122_3344L, BattleActionCodes.SWORD_WALK));
        assertArrayEquals(hex("6F 01 00 00 70 58 00 44 33 22 11 02 AA BB"),
                Protocol.changeInventoryDescriptionExtended(
                        0x7000_0001L, "X", 0x1122_3344L,
                        new byte[] {(byte) 0xAA, (byte) 0xBB}));
    }

    @Test void preservesBluePotionSignedUseTypeOnInventoryAndDefaultUseWire() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.BLUE_POTION, 2), () -> 0x7000_0008L);
        RuntimeInventory.Item item = inventory.items().get(0);
        byte[] batch = Protocol.inventoryBatch(List.of(item.wireItem()));

        assertEquals(Protocol.S_ADD_INVENTORY_BATCH, Byte.toUnsignedInt(batch[0]));
        assertEquals(1, Byte.toUnsignedInt(batch[1]));
        assertEquals(0x7000_0008L, Integer.toUnsignedLong(
                java.nio.ByteBuffer.wrap(batch, 2, 4)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt()));
        assertEquals(0xFA, Byte.toUnsignedInt(batch[6]));

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(item.objectId()).s("").bytes();
        assertArrayEquals(hex("1C 08 00 00 70 00"), request);
        assertEquals(new Protocol.InventoryUse(0x7000_0008L, ""),
                Protocol.defaultInventoryUse(request));
    }

    @Test void pinsCookingBookF8ToDefaultCdsAndRejectsLegacyCdccTail() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.COOKING_BOOK_LEVEL_ONE, 1),
                () -> 0x7000_0055L);
        RuntimeInventory.Item book = inventory.items().get(0);
        byte[] batch = Protocol.inventoryBatch(List.of(book.wireItem()));

        assertEquals(0xF8, Byte.toUnsignedInt(batch[6]));
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(book.objectId()).s("").bytes();
        assertArrayEquals(hex("1C 55 00 00 70 00"), nativeRequest);
        assertEquals(new Protocol.InventoryUse(0x7000_0055L, ""),
                Protocol.defaultInventoryUse(nativeRequest));

        byte[] incompatibleLegacyTail = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(book.objectId()).c(0).c(7).bytes();
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.defaultInventoryUse(incompatibleLegacyTail));
    }

    @Test void parsesNativeGroundAndInventoryRequests() {
        Protocol.GroundPickup pickup = Protocol.groundPickup(new Protocol.Writer()
                .c(Protocol.C_GET).h(0x8123).h(0x4567)
                .d(0x1122_3344L).d(2).c(9).bytes());
        assertEquals(new Protocol.GroundPickup(0x8123, 0x4567, 0x1122_3344L, 2, 9), pickup);

        Protocol.InventoryDrop drop = Protocol.inventoryDrop(new Protocol.Writer()
                .c(Protocol.C_DROP).h(100).h(120).d(0x7000_0001L).d(3).bytes());
        assertEquals(new Protocol.InventoryDrop(100, 120, 0x7000_0001L, 3), drop);

        Protocol.GiveRequest give = Protocol.giveRequest(new Protocol.Writer()
                .c(Protocol.C_GIVE).d(2).h(32740).h(32798)
                .d(0x7000_0001L).d(2).bytes());
        assertEquals(new Protocol.GiveRequest(
                2, 32740, 32798, 0x7000_0001L, 2), give);

        byte[] wandUse = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(0x7000_0001L).d(0x1122_3344L).h(32740).h(32798).bytes();
        assertEquals(new Protocol.TargetLocationInventoryUse(
                        0x7000_0001L, 0x1122_3344L, 32740, 32798),
                Protocol.targetLocationInventoryUse(wandUse));

        assertEquals(new Protocol.InventoryUse(0x7000_0001L, ""),
                Protocol.defaultInventoryUse(new Protocol.Writer()
                        .c(Protocol.C_USE_ITEM).d(0x7000_0001L).s("").bytes()));
        byte[] lightUse = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(0x7000_0002L).d(0x1122_3344L).bytes();
        assertEquals(0x7000_0002L, Protocol.inventoryUseObjectId(lightUse));
        assertEquals(new Protocol.LightInventoryUse(0x7000_0002L, 0x1122_3344L),
                Protocol.lightInventoryUse(lightUse));
        byte[] homeUse = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                .d(0x7000_0005L).h(303).h(0).h(0).bytes();
        assertEquals(new Protocol.HomeTeleportInventoryUse(
                        0x7000_0005L, 303, 0, 0),
                Protocol.homeTeleportInventoryUse(homeUse));
        assertEquals(new Protocol.AppraisalInventoryUse(0x7000_0004L, 0x7000_0001L),
                Protocol.appraisalInventoryUse(new Protocol.Writer()
                        .c(Protocol.C_USE_ITEM).d(0x7000_0004L).d(0x7000_0001L).bytes()));
        assertEquals(new Protocol.ChoiceInventoryUse(0x7000_0006L, 0x7000_0001L),
                Protocol.choiceInventoryUse(new Protocol.Writer()
                        .c(Protocol.C_USE_ITEM).d(0x7000_0006L).d(0x7000_0001L).bytes()));
        assertEquals(new Protocol.ShipTicketInventoryUse(0x7000_0003L, 1),
                Protocol.shipTicketInventoryUse(new Protocol.Writer()
                        .c(Protocol.C_USE_ITEM).d(0x7000_0003L).c(1).bytes()));
        assertEquals(0x7000_0001L, Protocol.inventoryObjectRequest(
                new Protocol.Writer().c(Protocol.C_CHECK_INVENTORY).d(0x7000_0001L).bytes(),
                Protocol.C_CHECK_INVENTORY));
        assertEquals(0x7000_0001L, Protocol.inventoryObjectRequest(
                new Protocol.Writer().c(Protocol.C_DESTROY_ITEM).d(0x7000_0001L).bytes(),
                Protocol.C_DESTROY_ITEM));
    }

    @Test void writesExactNativeGroundObjectAndPickupCoordinate() {
        assertEquals(32749, Protocol.groundPickupWireX(32730));
        byte[] packet = Protocol.groundItem(
                0x5000_0001L, 100, 120, 45, 3, "Potion (3)");
        Protocol.Reader in = new Protocol.Reader(packet);
        assertEquals(Protocol.S_PUT_OBJECT, in.c());
        assertEquals(100, in.h());
        assertEquals(120, in.h());
        assertEquals(0x5000_0001L, in.d());
        assertEquals(45, in.h());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(3, in.d());
        assertEquals(0, in.h());
        assertEquals("Potion (3)", in.s());
        assertEquals("", in.s());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals("", in.s());
        assertEquals("", in.s());
        assertEquals(0, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals("", in.s());
        assertEquals(0xFFFF, in.h());
        assertEquals(0, in.c());
        assertEquals(0, in.remaining());
    }

    @Test void writesExactNativeIdentifyDescription() {
        assertArrayEquals(hex("3F 3B 05 8A 00 02 24 35 35 00 31 32 00"),
                Protocol.identifyCode(1339, 138, List.of("$55", "12")));
        assertArrayEquals(hex("3F 00 00 86 00 03 24 31 30 30 00 36 2B 30 00 38 2B 30 00"),
                Protocol.identifyCode(0, 134, List.of("$100", "6+0", "8+0")));
    }

    @Test void parsesShipMapRequestsAndWritesExactMapPage() {
        assertEquals(new Protocol.GotoMapRequest(true, -1, -1, -1),
                Protocol.gotoMapRequest(hex("59 FF FF")));
        assertEquals(new Protocol.GotoMapRequest(false, 4, 32565, 32727),
                Protocol.gotoMapRequest(new Protocol.Writer().c(Protocol.C_GOTO_MAP)
                        .h(4).h(32565).h(32727).bytes()));
        assertEquals(new Protocol.EnterShipRequest(0),
                Protocol.enterShipRequest(hex("66 00")));
        assertEquals(new Protocol.EnterShipRequest(1),
                Protocol.enterShipRequest(hex("66 01")));
        assertArrayEquals(hex("6E 01 00 00 70 10"),
                Protocol.showMap(0x7000_0001L, 16));
    }

    @Test void rejectsTruncatedOrOutOfRangeInventoryContracts() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.groundPickup(hex("0B 01 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.inventoryDrop(hex("0C 00 00 00 00 01 00 00 70 00 00 00 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.giveRequest(hex(
                        "11 02 00 00 00 00 00 00 00 01 00 00 70 00 00 00 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.defaultInventoryUse(hex("1C 01 00 00 70")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.lightInventoryUse(hex("1C 01 00 00 70 44 33 22")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.homeTeleportInventoryUse(
                        hex("1C 01 00 00 70 2F 01 00 00 01 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.appraisalInventoryUse(hex("1C 01 00 00 70 02 00 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.choiceInventoryUse(hex("1C 01 00 00 70 02 00 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.shipTicketInventoryUse(hex("1C 01 00 00 70 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.gotoMapRequest(hex("59 00 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.gotoMapRequest(hex("59 04 00 35 7F")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.enterShipRequest(hex("66")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.enterShipRequest(hex("66 02")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.enterShipRequest(hex("66 00 00")));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.showMap(1, 19));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.inventoryBatch(java.util.Collections.nCopies(256, SWORD)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.changeInventoryBlessing(1, 4));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.changeAbility(-1, true));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.changeAbility(6, true));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.underwaterBreath(1, 0x1_0000));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.underwaterBreath(1, -2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.identifyCode(0, 138, List.of()));
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static byte[] hex(String source) {
        String[] fields = source.split(" ");
        byte[] result = new byte[fields.length];
        for (int index = 0; index < fields.length; index++) {
            result[index] = (byte) Integer.parseInt(fields[index], 16);
        }
        return result;
    }
}
