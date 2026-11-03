package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldHellTest {
    @Test
    void durationAndFixedRateClockMatchRetainedPcHellTimer() {
        assertEquals(300, ClassicWorldHell.durationTicks(0));
        assertEquals(300, ClassicWorldHell.durationTicks(10));
        assertEquals(600, ClassicWorldHell.durationTicks(11));
        assertEquals(27_300, ClassicWorldHell.durationTicks(100));

        ClassicWorldHell.Clock clock = new ClassicWorldHell.Clock();
        long start = 7_000_000_000L;
        assertEquals(0, clock.pollDue(start));
        assertEquals(0, clock.pollDue(start + ClassicWorldHell.TICK_NANOS - 1));
        assertEquals(1, clock.pollDue(start + ClassicWorldHell.TICK_NANOS));
        assertEquals(3, clock.pollDue(start + 4 * ClassicWorldHell.TICK_NANOS));
        assertEquals(0, clock.pollDue(start + 4 * ClassicWorldHell.TICK_NANOS));
        clock.clear();
        assertEquals(0, clock.pollDue(start + 99 * ClassicWorldHell.TICK_NANOS));
    }

    @Test
    void entryAndFiveReturnDestinationsRemainExact() {
        assertEquals(new ClassicWorldHell.Destination(666, 32701, 32777, 5),
                ClassicWorldHell.entry());
        assertEquals(List.of(
                new ClassicWorldHell.Destination(4, 32750, 32435, 5),
                new ClassicWorldHell.Destination(4, 32745, 32447, 5),
                new ClassicWorldHell.Destination(4, 32738, 32452, 5),
                new ClassicWorldHell.Destination(4, 32741, 32436, 5),
                new ClassicWorldHell.Destination(4, 32749, 32446, 5)),
                ClassicWorldHell.returnDestinations());
        assertEquals(ClassicWorldHell.returnDestination(0),
                ClassicWorldHell.returnDestination(5));
        assertEquals(ClassicWorldHell.returnDestination(4),
                ClassicWorldHell.returnDestination(-1));
    }

    @Test
    void map666ResourcePinsBoundsEntryAndSelectedSource() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldHell.MAP_ID);
        assertNotNull(map);
        assertEquals(32640, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32704, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(192, map.width());
        assertEquals(192, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP666_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.standingPassable(
                ClassicWorldHell.ENTRY_X, ClassicWorldHell.ENTRY_Y));
        assertResourceHash("/maps/666.l38.gz",
                ClassicWorldMapCatalog.MAP666_RESOURCE_SHA256);
    }

    @Test
    void map666KeepsAllThirteenSpawnGroupsAndFourHundredEightyActors()
            throws Exception {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldMonsterCatalog.definitions(ClassicWorldHell.MAP_ID);
        assertEquals(13, definitions.size());
        assertEquals(480, definitions.stream().mapToInt(
                ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(2898, definitions.get(0).spawnId());
        assertEquals(50, definitions.get(0).count());
        assertEquals(45241, definitions.get(0).template().npcId());
        assertEquals(2910, definitions.get(12).spawnId());
        assertEquals(20, definitions.get(12).count());
        assertEquals(45451, definitions.get(12).template().npcId());
        assertResourceHash("/world/map666-monsters.lwm.gz",
                ClassicWorldMonsterCatalog.MAP666_RESOURCE_SHA256);

        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
        List<ClassicWorldMonsterPopulation.Snapshot> actors =
                population.withinTileDistance(666,
                        ClassicWorldHell.ENTRY_X, ClassicWorldHell.ENTRY_Y,
                        Integer.MAX_VALUE);
        assertEquals(480, actors.size());
        for (ClassicWorldMonsterPopulation.Snapshot actor : actors) {
            assertTrue(actor.spawnId() >= 2898 && actor.spawnId() <= 2910);
            assertTrue(ClassicWorldMapCatalog.forMap(666)
                    .standingPassable(actor.x(), actor.y()));
        }
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition : definitions) {
            assertEquals(definition.count(), actors.stream()
                    .filter(actor -> actor.spawnId() == definition.spawnId())
                    .count());
        }
    }

    private void assertResourceHash(String resource, String expected)
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input);
            digest.update(input.readAllBytes());
        }
        assertEquals(expected, HexFormat.of().withUpperCase()
                .formatHex(digest.digest()));
    }
}
