package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldWaterDragonCatalogTest {
    @Test
    void map1011IsHashLockedWithExactFlagsAndNativeLandings() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(1011);
        assertEquals(1011, map.mapId());
        assertEquals(32640, map.startX());
        assertEquals(33151, map.endX());
        assertEquals(32448, map.startY());
        assertEquals(32959, map.endY());
        assertEquals(512, map.width());
        assertEquals(512, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP1011_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals(ClassicWorldMapCatalog.MAP1011_RESOURCE_SHA256,
                resourceHash("/maps/1011.l38.gz"));
        assertTrue(ClassicWorldMapCatalog.supportedMapIds().contains(1011));
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());
        assertTrue(map.standingPassable(ClassicWorldWaterDragonCatalog.ENTRY.x(),
                ClassicWorldWaterDragonCatalog.ENTRY.y()));
        assertTrue(map.hasWalkableExit(ClassicWorldWaterDragonCatalog.ENTRY.x(),
                ClassicWorldWaterDragonCatalog.ENTRY.y()));
        assertTrue(map.hasWalkableExit(
                ClassicWorldWaterDragonCatalog.BOSS_GATE_DESTINATION.x(),
                ClassicWorldWaterDragonCatalog.BOSS_GATE_DESTINATION.y()));

        ClassicWorldMapCatalog.ClassicMap gate = ClassicWorldMapCatalog.forMap(613);
        assertEquals(List.of(32640, 32895, 32768, 33023, 256, 256),
                List.of(gate.startX(), gate.endX(), gate.startY(), gate.endY(),
                        gate.width(), gate.height()));
        assertEquals(ClassicWorldMapCatalog.MAP613_SOURCE_SHA256,
                gate.sourceSha256());
        assertEquals(ClassicWorldMapCatalog.MAP613_RESOURCE_SHA256,
                resourceHash("/maps/613.l38.gz"));
        assertTrue(gate.allowsEscape());
        assertFalse(gate.allowsPetRecall());
        assertFalse(gate.allowsRandomTeleport());
        assertTrue(gate.standingPassable(32809, 32896));
        assertTrue(gate.hasWalkableExit(32809, 32896));
    }

    @Test
    void questMetadataPopulationAndDoorsStayExact() {
        assertEquals(117, ClassicWorldWaterDragonCatalog.QUEST_ID);
        assertEquals(8, ClassicWorldWaterDragonCatalog.MAXIMUM_USERS);
        assertEquals(1_800, ClassicWorldWaterDragonCatalog.INSTANCE_SECONDS);
        assertEquals(60, ClassicWorldWaterDragonCatalog.MINIMUM_ENTRY_LEVEL);
        assertEquals(80, ClassicWorldWaterDragonCatalog.QUEST_METADATA_LEVEL);
        assertEquals(82504, ClassicWorldWaterDragonCatalog.PASS_ITEM_ID);
        assertEquals(6798, ClassicWorldWaterDragonCatalog.WATER_DRAGON_BLOOD_SKILL_ID);

        List<ClassicWorldWaterDragonCatalog.SpawnRow> rows =
                ClassicWorldWaterDragonCatalog.retainedSpawnRows();
        assertEquals(7, rows.size());
        assertEquals(List.of(50203, 50206, 50207, 50208, 50209, 50210, 50211),
                rows.stream().map(ClassicWorldWaterDragonCatalog.SpawnRow::spawnId)
                        .toList());
        assertEquals(84, ClassicWorldWaterDragonCatalog.instanceSpawnRows().stream()
                .mapToInt(ClassicWorldWaterDragonCatalog.SpawnRow::count).sum());
        assertEquals(List.of(70937, 70689, 70974, 70975, 70976, 70977, 70978),
                rows.stream().map(ClassicWorldWaterDragonCatalog.SpawnRow::npcId)
                        .toList());

        assertEquals(List.of(10008, 10009, 10010),
                ClassicWorldWaterDragonCatalog.doors().stream()
                        .map(ClassicWorldWaterDragonCatalog.Door::doorId).toList());
        assertEquals(10008,
                ClassicWorldWaterDragonCatalog.doorForGuardian(70976).doorId());
        assertEquals(10009,
                ClassicWorldWaterDragonCatalog.doorForGuardian(70977).doorId());
        assertEquals(10010,
                ClassicWorldWaterDragonCatalog.doorForGuardian(70978).doorId());
        assertEquals(0x6D00_2A08L,
                ClassicWorldWaterDragonCatalog.doors().get(0).objectId(42));
    }

    @Test
    void retainedRoutesGetbackBossTimingAndSaellBlessingStayExact() {
        assertEquals("4AD99E07F0DF2FE5204B3A38E1EB7E4AB2198BEBA2E6711266BF274557BC3063",
                ClassicWorldWaterDragonCatalog.SAELL_SOURCE_SHA256);
        assertEquals("89BCD86ED1C73B2D23AEF8FF12B0434EC543F5A5301E61D9B742E1001D0450B1",
                ClassicWorldWaterDragonCatalog.SAELL_SKILL_SOURCE_SHA256);
        int[][] routes = {
                {32921,32727,32762,32724,6}, {33002,32731,32762,32724,6},
                {32762,32724,32921,32727,5}, {32860,32660,32928,32658,4},
                {32928,32658,32860,32660,5}, {32941,32671,32993,32841,6},
                {32993,32841,32941,32671,5}
        };
        for (int[] expected : routes) {
            ClassicWorldDungeonCatalog.Route route =
                    ClassicWorldDungeonCatalog.find(1011, expected[0], expected[1]);
            assertNotNull(route);
            assertEquals(List.of(expected[2], expected[3], expected[4]),
                    List.of(route.destinationX(), route.destinationY(),
                            route.destinationHeading()));
        }
        ClassicWorldGetbackCatalog.Destination getback =
                ClassicWorldGetbackCatalog.resolve(1011, 32957, 32743, 1, 0);
        assertEquals(List.of(4, 33429, 32800),
                List.of(getback.mapId(), getback.x(), getback.y()));

        assertEquals(List.of(1657, 1658, 1659, 1660),
                ClassicWorldWaterDragonCatalog.startMessages().stream()
                        .map(ClassicWorldWaterDragonCatalog.TimedMessage::messageId)
                        .toList());
        assertEquals(List.of(30, 32, 34, 36),
                ClassicWorldWaterDragonCatalog.startMessages().stream()
                        .map(ClassicWorldWaterDragonCatalog.TimedMessage::offsetSeconds)
                        .toList());
        assertEquals(List.of(5, 10, 20, 30),
                ClassicWorldWaterDragonCatalog.deathMessages(71026).stream()
                        .map(ClassicWorldWaterDragonCatalog.TimedMessage::offsetSeconds)
                        .toList());
        assertEquals(List.of(30, 40, 50, 55, 56, 57, 58, 59),
                ClassicWorldWaterDragonCatalog.completionMessages().stream()
                        .map(ClassicWorldWaterDragonCatalog.TimedMessage::offsetSeconds)
                        .toList());
        assertEquals(List.of(71027, 71028, -1), List.of(
                ClassicWorldWaterDragonCatalog.bossPhase(71026).nextNpcId(),
                ClassicWorldWaterDragonCatalog.bossPhase(71027).nextNpcId(),
                ClassicWorldWaterDragonCatalog.bossPhase(71028).nextNpcId()));

        ClassicWorldWaterDragonCatalog.Blessing blessing =
                ClassicWorldWaterDragonCatalog.SAELL_BLESSING;
        assertEquals(List.of(4010, 2400, 7680, 8, 1, 80, 10, 6, 3, 30, -8),
                List.of(blessing.skillId(), blessing.durationSeconds(), blessing.gfxId(),
                        blessing.hpr(), blessing.mpr(), blessing.maximumHp(),
                        blessing.maximumMp(), blessing.bowHit(), blessing.bowDamage(),
                        blessing.waterResistance(), blessing.ac()));
    }

    private static String resourceHash(String resource) throws Exception {
        try (InputStream in = ClassicWorldWaterDragonCatalogTest.class
                .getResourceAsStream(resource)) {
            assertNotNull(in);
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }
}
