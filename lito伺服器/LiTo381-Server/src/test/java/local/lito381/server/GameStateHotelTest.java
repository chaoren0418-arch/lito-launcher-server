package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateHotelTest {
    private static final long KEY_ONE = 0x7000_1001L;
    private static final long KEY_TWO = 0x7000_1002L;

    @TempDir Path temporary;

    @Test
    void rentalKeysLeaseAndReturnSurviveRestartAtomically() throws Exception {
        Path file = temporary.resolve("hotel-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> rentedInventory = Map.of(
                InventoryCatalog.ADENA, 99_400,
                InventoryCatalog.HOTEL_KEY, 2);
        Map<Long, List<RuntimeInventory.PersistentItem>> rentedStates = Map.of(
                InventoryCatalog.HOTEL_KEY, List.of(key(KEY_ONE), key(KEY_TWO)));

        GameStateStore.HotelRentMutation mutation =
                state.saveEconomyStateAndHotelLease(
                        "InnOwner", 0, rentedInventory, rentedStates,
                        70012, false, "S:INNOWNER", List.of(KEY_ONE, KEY_TWO));
        assertTrue(mutation.rented());
        assertEquals(0, mutation.lease().roomNumber());
        assertEquals(Math.toIntExact(KEY_ONE), mutation.lease().showId());
        assertEquals(1, state.hotelOccupancy(70012, "S:OTHER").activeRooms());
        assertNotNull(state.activeHotelLeaseForInventory(
                70012, Set.of(KEY_TWO)));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.HotelLeaseState lease =
                reloaded.activeHotelLeaseForInventory(70012, Set.of(KEY_ONE));
        assertNotNull(lease);
        assertEquals("S:INNOWNER", lease.renterIdentity());

        GameStateStore.HotelReturnQuote quote = reloaded.hotelReturnQuote(
                70012, "S:INNOWNER", Set.of(KEY_ONE, KEY_TWO));
        assertTrue(quote.present());
        assertTrue(quote.releasesLease());
        assertEquals(100, quote.refund());
        reloaded.saveEconomyStateAndHotelReturn(
                "InnOwner", 0, Map.of(InventoryCatalog.ADENA, 99_500),
                Map.of(), quote);

        assertEquals(0, reloaded.hotelOccupancy(
                70012, "S:INNOWNER").activeRooms());
        assertNull(reloaded.activeHotelLeaseForInventory(
                70012, Set.of(KEY_ONE, KEY_TWO)));
        assertFalse(reloaded.hotelReturnQuote(
                70012, "S:INNOWNER", Set.of(KEY_ONE, KEY_TWO)).present());
        assertEquals(99_500, new GameStateStore(file, true)
                .economy("InnOwner").inventory().get(InventoryCatalog.ADENA));
    }

    @Test
    void nativePoolAllowsTwelveRoomsAndFourHallsAcrossSixteenSlots()
            throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("memory.properties"), false);
        long objectId = 0x7001_0000L;
        for (int index = 0; index < 12; index++) {
            GameStateStore.HotelRentMutation room = rent(
                    state, "Room" + index, false, objectId++);
            assertTrue(room.rented());
        }
        assertEquals(GameStateStore.HotelRentStatus.ROOMS_FULL,
                rent(state, "RoomFull", false, objectId++).status());
        for (int index = 0; index < 4; index++) {
            GameStateStore.HotelRentMutation hall = rent(
                    state, "Hall" + index, true, objectId++);
            assertTrue(hall.rented());
        }
        GameStateStore.HotelOccupancy full =
                state.hotelOccupancy(70012, "Nobody");
        assertEquals(12, full.activeRooms());
        assertEquals(4, full.activeHalls());
        assertEquals(-1, full.firstFreeSlot());
        assertEquals(GameStateStore.HotelRentStatus.HALLS_FULL,
                rent(state, "HallFull", true, objectId).status());
    }

    @Test
    void sharedKeyRemainsAnAdmissionCredentialAfterGroundTransfer()
            throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("shared-key.properties"), false);
        GameStateStore.HotelRentMutation rental = rent(
                state, "Owner", false, KEY_ONE);
        assertTrue(rental.rented());
        RuntimeInventory.Item runtimeKey = new RuntimeInventory.Item(
                KEY_ONE, InventoryCatalog.require(InventoryCatalog.HOTEL_KEY),
                1, false);
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item dropped = ground.putPersisted(
                16384, rental.lease().showId(), runtimeKey, 1,
                32745, 32803, 0x7000_2222, () -> {});
        assertEquals(KEY_ONE, dropped.objectId());

        ClassicWorldGroundItems.Take pickedUp = ground.takePersisted(
                16384, rental.lease().showId(), KEY_ONE, 1, () -> {});
        RuntimeInventory guest = new RuntimeInventory();
        assertTrue(guest.addTransferred(pickedUp.taken().inventoryItem(), 1));
        Set<Long> guestObjects = guest.items().stream()
                .map(RuntimeInventory.Item::objectId)
                .collect(java.util.stream.Collectors.toSet());
        GameStateStore.HotelLeaseState admitted =
                state.activeHotelLeaseForInventory(70012, guestObjects);
        assertNotNull(admitted);
        assertEquals(rental.lease().showId(), admitted.showId());
    }

    @Test
    void sqlRefusalRollsBackLeaseKeysAndEconomyBeforePublication()
            throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("sql-refusal.properties"), false);
        IOException failure = assertThrows(IOException.class,
                () -> state.saveEconomyStateAndHotelLease(
                        "SqlOwner", 0,
                        Map.of(InventoryCatalog.ADENA, 99_700,
                                InventoryCatalog.HOTEL_KEY, 1),
                        Map.of(InventoryCatalog.HOTEL_KEY,
                                List.of(key(KEY_ONE))),
                        70012, false, "S:SQLOWNER", List.of(KEY_ONE),
                        ignored -> { throw new IOException("read-only"); }));
        assertEquals("read-only", failure.getMessage());
        assertEquals(0, state.hotelOccupancy(70012, "S:SQLOWNER").activeRooms());
        assertNull(state.activeHotelLeaseForInventory(70012, Set.of(KEY_ONE)));
        assertFalse(state.economy("SqlOwner").inventory()
                .containsKey(InventoryCatalog.HOTEL_KEY));
    }

    private static GameStateStore.HotelRentMutation rent(
            GameStateStore state, String name, boolean hall, long objectId)
            throws Exception {
        return state.saveEconomyStateAndHotelLease(
                name, 0,
                Map.of(InventoryCatalog.ADENA, 99_700,
                        InventoryCatalog.HOTEL_KEY, 1),
                Map.of(InventoryCatalog.HOTEL_KEY, List.of(key(objectId))),
                70012, hall, "S:" + name.toUpperCase(), List.of(objectId));
    }

    private static RuntimeInventory.PersistentItem key(long objectId) {
        return new RuntimeInventory.PersistentItem(
                objectId, 1, 0, 0, true, 0);
    }
}
