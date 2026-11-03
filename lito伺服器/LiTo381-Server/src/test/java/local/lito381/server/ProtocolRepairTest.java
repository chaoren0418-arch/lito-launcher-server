package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolRepairTest {
    @Test
    void parsesOpcodeOnlyListRequestAndExactCddRepairSelection() {
        Protocol.requireFixableItemRequest(new byte[] {(byte) Protocol.C_FIXABLE_ITEM});
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.requireFixableItemRequest(new byte[] {
                        (byte) Protocol.C_FIXABLE_ITEM, 0}));

        Protocol.RepairRequest request = Protocol.repairRequest(new Protocol.Writer()
                .c(Protocol.C_FIX).d(0x7000_0100L).d(0x6300_0001L).bytes());
        assertEquals(0x7000_0100L, request.inventoryObjectId());
        assertEquals(0x6300_0001L, request.npcObjectId());
        assertThrows(IllegalArgumentException.class, () -> Protocol.repairRequest(
                new Protocol.Writer().c(Protocol.C_FIX).d(0x7000_0100L).d(0).bytes()));
    }

    @Test
    void writesExactDhHeaderAndDcRowsWithinSignedDamageRange() {
        List<Protocol.RepairableItem> rows = List.of(
                new Protocol.RepairableItem(0x7000_0100L, 3),
                new Protocol.RepairableItem(0x7000_0101L, 127));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_FIXABLE_ITEM_LIST)
                        .d(200).h(2).d(0x7000_0100L).c(3)
                        .d(0x7000_0101L).c(127).bytes(),
                Protocol.fixableItemList(200, rows));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_FIXABLE_ITEM_LIST)
                        .d(200).h(0).bytes(),
                Protocol.fixableItemList(200, List.of()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.fixableItemList(
                200, List.of(new Protocol.RepairableItem(1, 128))));
    }
}
