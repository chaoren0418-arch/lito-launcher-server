package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class GameStateWaterDragonTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private static final Instant START = Instant.parse("2026-08-22T00:00:00Z");

    @TempDir Path temporary;

    @Test
    void passObjectRoomAndLeaseSurviveRestartThenExpireOffline() throws Exception {
        Path file = temporary.resolve("water-dragon.properties");
        Clock startClock = Clock.fixed(START, ZONE);
        GameStateStore state = new GameStateStore(file, true, startClock);
        AtomicLong objectIds = new AtomicLong(0x7000_0000L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of((long) ClassicWorldWaterDragonCatalog.PASS_ITEM_ID,
                        ClassicWorldWaterDragonCatalog.PASS_RENTAL_COUNT),
                objectIds::incrementAndGet);
        RuntimeInventory.Item passItem = inventory.items().get(0);
        String owner = GameStateStore.sourceCharacter("Sun", "LeaseOwner");
        state.saveEconomyStateAndWaterDragonPass(
                owner, 0, inventory.catalogCounts(), inventory.catalogItemStates(),
                passItem.objectId(), Math.toIntExact(passItem.objectId()), 7, owner);

        GameStateStore reloaded = new GameStateStore(file, true, Clock.fixed(
                START.plusSeconds(60), ZONE));
        List<GameStateStore.WaterDragonPassState> active =
                reloaded.activeWaterDragonPasses();
        assertEquals(1, active.size());
        assertEquals(passItem.objectId(), active.get(0).itemObjectId());
        assertEquals(7, active.get(0).roomNumber());
        assertEquals(ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS - 60,
                active.get(0).remainingSeconds(
                        reloaded.currentEpochSeconds()));
        assertEquals(passItem.objectId(), reloaded.economy(owner).itemStates()
                .get((long) ClassicWorldWaterDragonCatalog.PASS_ITEM_ID)
                .get(0).objectId());

        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.restoreClassicWorldWaterDragonPasses(
                    "Sun", active, 5_000_000_000L,
                    reloaded.currentEpochSeconds());
            assertEquals(1, hub.classicWorldWaterDragonRentedRooms(
                    "Sun", 5_000_000_000L));
            assertEquals(passItem.objectId(), hub.classicWorldWaterDragonPass(
                    "Sun", java.util.Set.of(passItem.objectId()),
                    5_000_000_000L).itemObjectId());
        }

        GameStateStore expired = new GameStateStore(file, true, Clock.fixed(
                START.plusSeconds(
                        ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS + 1L),
                ZONE));
        assertTrue(expired.activeWaterDragonPasses().isEmpty());
    }

    @Test
    void identicalObjectAndRoomRemainIsolatedByClassicSource() throws Exception {
        Path file = temporary.resolve("water-dragon-routes.properties");
        GameStateStore state = new GameStateStore(
                file, true, Clock.fixed(START, ZONE));
        long objectId = 0x7000_1234L;
        long expiry = START.getEpochSecond()
                + ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS;
        Map<Long, Integer> counts = Map.of(
                (long) ClassicWorldWaterDragonCatalog.PASS_ITEM_ID,
                ClassicWorldWaterDragonCatalog.PASS_RENTAL_COUNT);
        String one = GameStateStore.sourceCharacter("Route One", "SameId");
        String two = GameStateStore.sourceCharacter("Route Two", "SameId");
        state.saveEconomyStateAndWaterDragonPass(
                "Route One", one, 0, counts, Map.of(), objectId,
                Math.toIntExact(objectId), 0, one, expiry);
        state.saveEconomyStateAndWaterDragonPass(
                "Route Two", two, 0, counts, Map.of(), objectId,
                Math.toIntExact(objectId), 0, two, expiry);

        GameStateStore reloaded = new GameStateStore(file, true, Clock.fixed(
                START.plusSeconds(30), ZONE));
        assertEquals(1, reloaded.activeWaterDragonPasses("Route One").size());
        assertEquals(1, reloaded.activeWaterDragonPasses("Route Two").size());
        try (GameHub hub = new GameHub(ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties")))) {
            long nowNanos = 9_000_000_000L;
            hub.restoreClassicWorldWaterDragonPasses(
                    "Route One", reloaded.activeWaterDragonPasses("Route One"),
                    nowNanos, reloaded.currentEpochSeconds());
            hub.restoreClassicWorldWaterDragonPasses(
                    "Route Two", reloaded.activeWaterDragonPasses("Route Two"),
                    nowNanos, reloaded.currentEpochSeconds());
            GameHub.WaterDragonPass first = hub.classicWorldWaterDragonPass(
                    "Route One", java.util.Set.of(objectId), nowNanos);
            GameHub.WaterDragonPass second = hub.classicWorldWaterDragonPass(
                    "Route Two", java.util.Set.of(objectId), nowNanos);
            assertEquals(1, hub.classicWorldWaterDragonRentedRooms(
                    "Route One", nowNanos));
            assertEquals(1, hub.classicWorldWaterDragonRentedRooms(
                    "Route Two", nowNanos));
            assertFalse(first.showId() == second.showId());
        }
    }

    @Test
    void completionBloodCooldownUsesWallClockAcrossRestart() throws Exception {
        Path file = temporary.resolve("water-dragon-blood.properties");
        String owner = GameStateStore.sourceCharacter("Sun", "BloodOwner");
        GameStateStore state = new GameStateStore(
                file, true, Clock.fixed(START, ZONE));
        state.saveWaterDragonBlood(owner,
                ClassicWorldWaterDragonCatalog.COMPLETION_BLOOD_SECONDS);

        GameStateStore reloaded = new GameStateStore(file, true, Clock.fixed(
                START.plusSeconds(125), ZONE));
        assertEquals(
                ClassicWorldWaterDragonCatalog.COMPLETION_BLOOD_SECONDS - 125,
                reloaded.waterDragonBloodRemainingSeconds(owner));

        GameStateStore expired = new GameStateStore(file, true, Clock.fixed(
                START.plusSeconds(
                        ClassicWorldWaterDragonCatalog.COMPLETION_BLOOD_SECONDS + 1L),
                ZONE));
        assertEquals(0, expired.waterDragonBloodRemainingSeconds(owner));
        expired.saveWaterDragonBlood(owner, 0);
        assertFalse(expired.waterDragonBloodRemainingSeconds(owner) > 0);
    }
}
