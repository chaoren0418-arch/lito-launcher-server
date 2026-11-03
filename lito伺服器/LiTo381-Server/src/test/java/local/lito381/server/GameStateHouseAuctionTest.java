package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateHouseAuctionTest {
    @TempDir Path temporary;

    @Test
    void persistsOneShotBidsAndRefundsThePreviousBidderAtomically() throws Exception {
        Path file = temporary.resolve("house-auction-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.HouseAuction initial = state.houseAuction(262_145L);
        assertEquals(100_000L, initial.price());
        assertEquals("$1242 1$1195", initial.name());
        assertEquals(78, initial.area());

        Map<Long, Integer> knight = state.economy("TestKnight").inventory();
        Map<Long, Integer> knightAfter = removeAdena(knight, 100_000);
        GameStateStore.HouseBidMutation first = state.bidHouse(
                "TestKnight", 1, initial.houseId(), initial.revision(),
                100_000, knight, knightAfter);
        assertTrue(first.accepted());
        assertEquals("TestKnight", first.auction().bidder());
        assertEquals(initial.revision() + 1, first.auction().revision());

        GameStateStore.HouseBidMutation replay = state.bidHouse(
                "TestKnight", 1, initial.houseId(), initial.revision(),
                100_000, knight, knightAfter);
        assertEquals(GameStateStore.HouseBidStatus.STALE, replay.status());

        Map<Long, Integer> mageFunded = new LinkedHashMap<>(
                state.economy("TestMage").inventory());
        mageFunded.put(InventoryCatalog.ADENA, 200_001);
        state.saveEconomy("TestMage", 5_000, mageFunded);
        Map<Long, Integer> mageAfter = removeAdena(mageFunded, 100_001);
        GameStateStore.HouseBidMutation second = state.bidHouse(
                "TestMage", 2, initial.houseId(), first.auction().revision(),
                100_001, mageFunded, mageAfter);
        assertTrue(second.accepted());
        assertEquals("TestKnight", second.previousBidder());
        assertEquals("testknight", second.previousBidderIdentity());
        assertEquals(100_000, second.previousPrice());
        assertEquals(100_000, second.previousBidderInventory()
                .get(InventoryCatalog.ADENA));
        assertEquals(100_000, state.economy("TestKnight").inventory()
                .get(InventoryCatalog.ADENA));
        assertEquals(100_000, state.economy("TestMage").inventory()
                .get(InventoryCatalog.ADENA));

        Map<Long, Integer> knightFunded = new LinkedHashMap<>(
                state.economy("TestKnight").inventory());
        knightFunded.put(InventoryCatalog.ADENA, 200_002);
        state.saveEconomy("TestKnight", 5_000, knightFunded);
        Map<Long, Integer> forged = new LinkedHashMap<>(knightFunded);
        forged.put(InventoryCatalog.ADENA, 1);
        GameStateStore.HouseBidMutation invalid = state.bidHouse(
                "TestKnight", 1, initial.houseId(), second.auction().revision(),
                100_002, knightFunded, forged);
        assertFalse(invalid.accepted());
        assertEquals(GameStateStore.HouseBidStatus.INVENTORY_CHANGED, invalid.status());

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.HouseAuction durable = reloaded.houseAuction(initial.houseId());
        assertEquals("TestMage", durable.bidder());
        assertEquals(100_001, durable.price());
        assertEquals(second.auction().revision(), durable.revision());
        assertTrue(Files.readString(file).contains("format.version=43"));
    }

    @Test
    void sourceQualifiedHouseIdsStayIndependentAndClaimFormat19Once() throws Exception {
        Path file = temporary.resolve("source-house-auction-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        long houseId = 262_145L;
        String sun = GameStateStore.sourceCharacter("Sun", "SameHero");
        String moon = GameStateStore.sourceCharacter("Moon", "SameHero");

        GameStateStore.HouseAuction sunInitial = state.houseAuction("Sun", houseId);
        assertEquals("Sun", sunInitial.sourceId());
        Map<Long, Integer> sunInventory = state.economy(sun).inventory();
        GameStateStore.HouseBidMutation sunBid = state.bidHouse(
                "Sun", sun, 11, houseId, sunInitial.revision(), 100_000,
                sunInventory, removeAdena(sunInventory, 100_000));
        assertTrue(sunBid.accepted());

        GameStateStore.HouseAuction moonInitial = state.houseAuction("Moon", houseId);
        assertEquals("Moon", moonInitial.sourceId());
        assertEquals(100_000, moonInitial.price());
        assertEquals("", moonInitial.bidder());
        Map<Long, Integer> moonInventory = state.economy(moon).inventory();
        GameStateStore.HouseBidMutation moonBid = state.bidHouse(
                "Moon", moon, 12, houseId, moonInitial.revision(), 100_000,
                moonInventory, removeAdena(moonInventory, 100_000));
        assertTrue(moonBid.accepted());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals("SameHero", reloaded.houseAuction("Sun", houseId).bidder());
        assertEquals("SameHero", reloaded.houseAuction("Moon", houseId).bidder());
        assertEquals("Sun", reloaded.houseAuction("Sun", houseId).sourceId());
        assertEquals("Moon", reloaded.houseAuction("Moon", houseId).sourceId());
        assertTrue(Files.readString(file).contains("format.version=43"));
        assertTrue(Files.readString(file).contains("source=Sun"));
        assertTrue(Files.readString(file).contains("source=Moon"));
    }

    private static Map<Long, Integer> removeAdena(Map<Long, Integer> source, int amount) {
        Map<Long, Integer> result = new LinkedHashMap<>(source);
        int remaining = result.getOrDefault(InventoryCatalog.ADENA, 0) - amount;
        if (remaining == 0) result.remove(InventoryCatalog.ADENA);
        else result.put(InventoryCatalog.ADENA, remaining);
        return result;
    }
}
