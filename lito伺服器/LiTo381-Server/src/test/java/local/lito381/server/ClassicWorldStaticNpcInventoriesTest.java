package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldStaticNpcInventoriesTest {
    @Test
    void exactRetainedReceiverImplementationsAndStaticPopulationAreProjected() {
        assertTrue(ClassicWorldStaticNpcInventories.receiverImplementation("L1Npc"));
        assertTrue(ClassicWorldStaticNpcInventories.receiverImplementation("L1Monster"));
        assertTrue(ClassicWorldStaticNpcInventories.receiverImplementation("L1Guardian"));
        assertTrue(ClassicWorldStaticNpcInventories.receiverImplementation("L1Teleporter"));
        assertTrue(ClassicWorldStaticNpcInventories.receiverImplementation("L1Guard"));
        assertFalse(ClassicWorldStaticNpcInventories.receiverImplementation("L1Merchant"));
        assertFalse(ClassicWorldStaticNpcInventories.receiverImplementation("L1Dwarf"));

        List<ClassicWorldNpcCatalog.StaticNpc> receivers = receivers();
        assertEquals(ClassicWorldStaticNpcInventories.STATIC_RECEIVER_POPULATION,
                receivers.size());
        assertEquals(31, count(receivers, "L1Npc"));
        assertEquals(27, count(receivers, "L1Teleporter"));
        assertEquals(2, count(receivers, "L1Monster"));
        assertEquals(66, count(receivers, "L1Guard"));
        assertEquals(0, count(receivers, "L1Guardian"));
        assertTrue(receivers.stream().filter(npc -> npc.implementation().equals("L1Guard"))
                .allMatch(npc -> npc.hp() == 0));
    }

    @Test
    void acceptsStaticTeleporterAndIsolatesBagsByMapInstance() throws Exception {
        ClassicWorldNpcCatalog.StaticNpc npc = receivers().stream()
                .filter(row -> row.implementation().equals("L1Teleporter"))
                .findFirst().orElseThrow();
        RuntimeInventory.Item item = item(40508, 7);
        ClassicWorldStaticNpcInventories bags =
                new ClassicWorldStaticNpcInventories();

        assertEquals(ClassicWorldStaticNpcInventories.GiveAdmission.ACCEPTED,
                bags.canReceiveGivenItem(npc.mapId(), -1, npc.objectId(),
                        npc.x(), npc.y(), item, 7));
        ClassicWorldStaticNpcInventories.Receipt receipt =
                bags.receiveGivenItemPersisted(npc.mapId(), -1, npc.objectId(),
                        npc.x(), npc.y(), item, 7, () -> {});
        assertNotNull(receipt);
        assertEquals(40508, receipt.catalogId());
        assertEquals(7, receipt.quantity());
        assertEquals(7, bags.carriedItems(
                npc.mapId(), -1, npc.objectId()).get(0).quantity());
        assertTrue(bags.carriedItems(
                npc.mapId(), 91, npc.objectId()).isEmpty());
    }

    @Test
    void stalePositionMerchantAndFailedDurableCommitDoNotPublishBag()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc npc = receivers().stream()
                .filter(row -> row.implementation().equals("L1Npc"))
                .findFirst().orElseThrow();
        ClassicWorldNpcCatalog.StaticNpc merchant = ClassicWorldMapCatalog
                .supportedMapIds().stream()
                .flatMap(mapId -> ClassicWorldNpcCatalog.all(mapId).stream())
                .filter(ClassicWorldNpcCatalog.StaticNpc::fixedMerchant)
                .findFirst().orElseThrow();
        RuntimeInventory.Item item = item(40508, 1);
        ClassicWorldStaticNpcInventories bags =
                new ClassicWorldStaticNpcInventories();

        assertEquals(ClassicWorldStaticNpcInventories.GiveAdmission.STALE_POSITION,
                bags.canReceiveGivenItem(npc.mapId(), -1, npc.objectId(),
                        npc.x() + 1, npc.y(), item, 1));
        assertEquals(ClassicWorldStaticNpcInventories.GiveAdmission.UNKNOWN_TARGET,
                bags.canReceiveGivenItem(merchant.mapId(), -1, merchant.objectId(),
                        merchant.x(), merchant.y(), item, 1));
        assertNull(bags.receiveGivenItemPersisted(
                merchant.mapId(), -1, merchant.objectId(),
                merchant.x(), merchant.y(), item, 1, () -> {}));
        assertThrows(IOException.class, () -> bags.receiveGivenItemPersisted(
                npc.mapId(), -1, npc.objectId(), npc.x(), npc.y(), item, 1,
                () -> { throw new IOException("fixture durable failure"); }));
        assertTrue(bags.carriedItems(npc.mapId(), -1, npc.objectId()).isEmpty());
    }

    private static List<ClassicWorldNpcCatalog.StaticNpc> receivers() {
        return ClassicWorldMapCatalog.supportedMapIds().stream()
                .flatMap(mapId -> ClassicWorldNpcCatalog.all(mapId).stream())
                .filter(ClassicWorldStaticNpcInventories::staticReceiver)
                .toList();
    }

    private static long count(
            List<ClassicWorldNpcCatalog.StaticNpc> rows, String implementation) {
        return rows.stream().filter(npc ->
                npc.implementation().equals(implementation)).count();
    }

    private static RuntimeInventory.Item item(long catalogId, int quantity) {
        InventoryCatalog.Definition definition = InventoryCatalog.find(catalogId);
        assertNotNull(definition);
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(definition, quantity));
        return inventory.items().get(0);
    }
}
