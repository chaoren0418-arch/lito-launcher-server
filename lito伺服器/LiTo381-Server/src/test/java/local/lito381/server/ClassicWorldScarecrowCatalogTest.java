package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldScarecrowCatalogTest {
    @Test
    void supportedRowsPreserveExactSourceBoundaryTemplatesAndPointGroups() {
        List<ClassicWorldNpcCatalog.StaticNpc> all =
                ClassicWorldScarecrowCatalog.all();
        assertEquals(84, ClassicWorldScarecrowCatalog.SOURCE_SPAWN_COUNT);
        assertEquals(37, all.size());
        assertEquals(47, ClassicWorldScarecrowCatalog.DEFERRED_SPAWN_COUNT);
        assertEquals(16, ClassicWorldScarecrowCatalog.MAP68_DEFERRED_COUNT);
        assertEquals(31, ClassicWorldScarecrowCatalog.MAP2005_DEFERRED_COUNT);
        assertEquals(28, ClassicWorldScarecrowCatalog.forMap(4).size());
        assertEquals(9, ClassicWorldScarecrowCatalog.forMap(304).size());
        assertTrue(ClassicWorldScarecrowCatalog.forMap(68).isEmpty());
        assertTrue(ClassicWorldScarecrowCatalog.forMap(2005).isEmpty());

        assertEquals(Set.of(45001, 45002, 45004), all.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::npcId)
                .collect(java.util.stream.Collectors.toSet()));
        assertTrue(all.stream().allMatch(npc ->
                npc.implementation().equals("L1Scarecrow")
                        && npc.level() == 1 && npc.hp() == 1 && npc.mp() == 0
                        && npc.experience() == 2 && npc.lawful() == 0
                        && npc.movementDistance() == 0));
        assertEquals(all.size(), new HashSet<>(all.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::objectId).toList()).size());
        assertTrue(all.stream().allMatch(npc ->
                ClassicWorldNpcCatalog.npc(npc.mapId(), npc.objectId()) == npc));

        assertPoint(4, 45001, 33102, 33314, 4, 114);
        assertPoint(4, 45002, 33114, 33314, 4, 116);
        assertPoint(304, 45004, 32793, 32810, 1, 2992);
        assertNull(ClassicWorldScarecrowCatalog.target(4, 0x6400_04FCL));
    }

    @Test
    void combatProjectionIsReadOnlyExactPhysicalInputNotMonsterRegistration() {
        ClassicWorldNpcCatalog.StaticNpc scarecrow = point(
                4, 45001, 33102, 33314);
        ClassicWorldMonsterPopulation.Snapshot projection =
                ClassicWorldScarecrowCatalog.combatProjection(scarecrow, 7);
        assertEquals(scarecrow.objectId(), projection.objectId());
        assertEquals(7, projection.heading());
        assertEquals(1, projection.currentHp());
        assertEquals(1, projection.maximumHp());
        assertTrue(projection.alive() && projection.present());
        assertEquals(0, projection.template().armorClass());
        assertEquals(0, projection.template().magicResistance());
        assertEquals(2, projection.template().experience());
        assertFalse(projection.template().large());
        assertEquals("L1Monster", projection.template().implementation(),
                "projection reuses math only; canonical NPC remains L1Scarecrow");
        assertEquals("L1Scarecrow", scarecrow.implementation());
    }

    @Test
    void putObjectAndChangeHeadingUseSelectedClientNativeLayouts() {
        ClassicWorldNpcCatalog.StaticNpc scarecrow = point(
                4, 45001, 33102, 33314);
        byte[] actor = Protocol.classicStaticNpc(scarecrow, 4);
        assertEquals(Protocol.S_PUT_OBJECT, actor[0] & 0xFF);
        Protocol.Reader reader = new Protocol.Reader(actor);
        reader.c();
        assertEquals(33102, reader.h());
        assertEquals(33314, reader.h());
        assertEquals(scarecrow.objectId(), reader.d());
        assertEquals(114, reader.h());
        reader.c();
        assertEquals(4, reader.c());

        assertArrayEquals(new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION)
                        .d(scarecrow.objectId()).c(5).bytes(),
                new Protocol.Writer().c(0x1C)
                        .d(scarecrow.objectId()).c(5).bytes());
    }

    @Test
    void memoryPhysicalRowsMatchSelected380Database() throws Exception {
        ServerConfig config = ServerConfig.load(
                java.nio.file.Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (AccountRepository accounts = AccountRepository.open(config)) {
            for (int npcId : List.of(45001, 45002, 45003, 45004)) {
                SourceNpcPhysicalStats stats = accounts.npcPhysicalStats(npcId);
                assertNotNull(stats);
                assertEquals(0, stats.armorClass());
                assertEquals(0, stats.magicResistance());
                assertEquals(0, stats.damageReduction());
                assertFalse(stats.large());
                assertFalse(stats.hard());
                assertFalse(stats.boss());
                assertEquals(1, stats.ranged());
            }
        }
    }

    private static void assertPoint(
            int mapId, int npcId, int x, int y, int heading, int gfx) {
        ClassicWorldNpcCatalog.StaticNpc row = point(mapId, npcId, x, y);
        assertEquals(heading, row.heading());
        assertEquals(gfx, row.gfxId());
        assertTrue(ClassicWorldNpcCatalog.occupies(mapId, x, y));
    }

    private static ClassicWorldNpcCatalog.StaticNpc point(
            int mapId, int npcId, int x, int y) {
        return ClassicWorldScarecrowCatalog.forMap(mapId).stream()
                .filter(row -> row.npcId() == npcId && row.x() == x && row.y() == y)
                .findFirst().orElseThrow();
    }
}
