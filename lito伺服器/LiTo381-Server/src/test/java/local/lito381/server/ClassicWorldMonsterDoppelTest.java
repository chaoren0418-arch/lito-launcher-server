package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Retained first-target doppel appearance, timing and S_NPCPack behavior. */
final class ClassicWorldMonsterDoppelTest {
    private static final int MAP_ID = 4;
    private static final long OBJECT_BASE = 0x6843_0000L;
    private static final long MONSTER_ID = OBJECT_BASE + 1;
    private static final long PLAYER_ID = 0x4200_0773L;

    @Test
    void firstPlayerTargetCopiesPersistentAppearanceAndRuntimeSpeeds() {
        ClassicWorldMonsterPopulation population = fixture();
        ClassicWorldMonsterPopulation.Snapshot source = population.actor(
                MAP_ID, MONSTER_ID);
        assertEquals(31, source.gfxId());
        assertEquals("$1554", source.nameId());
        assertTrue(source.template().doppel());
        assertTrue(population.provoke(MAP_ID, MONSTER_ID, PLAYER_ID));

        ClassicWorldMonsterPopulation.DoppelTransformation transformation =
                population.doppelTransform(
                        MAP_ID, MONSTER_ID, PLAYER_ID,
                        "MirrorKnight", "Arena Crown", -1234, 61);
        assertNotNull(transformation);
        assertEquals(source, transformation.before());
        ClassicWorldMonsterPopulation.Snapshot after = transformation.after();
        assertEquals(MONSTER_ID, after.objectId());
        assertEquals(source.x(), after.x());
        assertEquals(source.y(), after.y());
        assertEquals(61, after.gfxId());
        assertEquals("MirrorKnight", after.nameId());
        assertEquals("Arena Crown", after.title());
        assertEquals(-1234, after.lawful());
        assertEquals(640, after.passispeed());
        assertEquals(900, after.atkspeed());
        assertNotNull(population.attack(
                MAP_ID, MONSTER_ID, PLAYER_ID,
                after.x() + 1, after.y(), 1L));
        assertFalse(population.actionReady(
                MAP_ID, MONSTER_ID, 900_000_000L));
        assertTrue(population.actionReady(
                MAP_ID, MONSTER_ID, 900_000_001L));

        population.beginTargetAction(MAP_ID, MONSTER_ID);
        assertNull(population.doppelTransform(
                MAP_ID, MONSTER_ID, PLAYER_ID,
                "Second", "Ignored", 0, 1));
    }

    @Test
    void newAiLifetimeMayCopyANewPlayerButCompanionIdMismatchCannot() {
        ClassicWorldMonsterPopulation population = fixture();
        assertTrue(population.provoke(MAP_ID, MONSTER_ID, PLAYER_ID));
        assertNull(population.doppelTransform(
                MAP_ID, MONSTER_ID, PLAYER_ID + 1,
                "Companion", "", 0, 61));
        assertNotNull(population.doppelTransform(
                MAP_ID, MONSTER_ID, PLAYER_ID,
                "First", "One", 10, 61));
        population.beginTargetAction(MAP_ID, MONSTER_ID);
        population.clearTarget(MAP_ID, MONSTER_ID, PLAYER_ID);
        population.resetUnobservedHome(MAP_ID, Set.of(), 1L);

        long nextPlayer = PLAYER_ID + 2;
        assertTrue(population.provoke(MAP_ID, MONSTER_ID, nextPlayer));
        ClassicWorldMonsterPopulation.DoppelTransformation second =
                population.doppelTransform(
                        MAP_ID, MONSTER_ID, nextPlayer,
                        "Second", "Two", 20, 0);
        assertNotNull(second);
        assertEquals("First", second.before().nameId());
        assertEquals("Second", second.after().nameId());
        assertEquals(0, second.after().gfxId());
    }

    @Test
    void sNpcPackCarriesSwordPcIdentityAndCurrentHasteSpeed() {
        ClassicWorldMonsterPopulation population = fixture();
        ClassicWorldMonsterPopulation.Snapshot initial = population.actor(
                MAP_ID, MONSTER_ID);
        Protocol.Reader initialPacket = new Protocol.Reader(
                Protocol.classicWorldMonster(initial));
        assertEquals(Protocol.S_PUT_OBJECT, initialPacket.c());
        initialPacket.h();
        initialPacket.h();
        initialPacket.d();
        assertEquals(31, initialPacket.h());
        assertEquals(0, initialPacket.c());
        initialPacket.c();
        initialPacket.c();
        assertEquals(BattleTimedStatus.NORMAL, initialPacket.c());
        initialPacket.d();
        initialPacket.h();
        assertEquals("$1554", initialPacket.s());
        assertEquals("", initialPacket.s());
        assertEquals(0x04, initialPacket.c());

        assertTrue(population.provoke(MAP_ID, MONSTER_ID, PLAYER_ID));
        ClassicWorldMonsterPopulation.Snapshot transformed =
                population.doppelTransform(
                        MAP_ID, MONSTER_ID, PLAYER_ID,
                        "MirrorKnight", "Arena Crown", -1234, 61).after();
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.classicWorldMonster(
                        transformed, BattleTimedStatus.HASTE));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(transformed.x(), packet.h());
        assertEquals(transformed.y(), packet.h());
        assertEquals(MONSTER_ID, packet.d());
        assertEquals(61, packet.h());
        assertEquals(0x04, packet.c());
        assertEquals(transformed.heading(), packet.c());
        packet.c();
        assertEquals(BattleTimedStatus.HASTE, packet.c());
        packet.d();
        assertEquals(-1234 & 0xFFFF, packet.h());
        assertEquals("MirrorKnight", packet.s());
        assertEquals("Arena Crown", packet.s());
        assertEquals(0x04, packet.c());
    }

    private static ClassicWorldMonsterPopulation fixture() {
        int[] home = openCenter();
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).stream()
                        .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                        .filter(candidate -> candidate.npcId() == 45189)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 994_773, "native-doppel-fixture",
                        1, 0, home[0], home[1], 0, 0, 0, 0, 0, 0, 2,
                        0, 0, false, 0, false, 0, template);
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                773L, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                List.of(definition));
    }

    private static int[] openCenter() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int x = map.startX() + 2; x < map.endX() - 2; x++) {
            for (int y = map.startY() + 2; y < map.endY() - 2; y++) {
                if (!ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)
                        && !ClassicWorldNpcCatalog.occupies(MAP_ID, x + 1, y)
                        && map.standingPassable(x, y)
                        && map.attackPosition(x, y, x + 1, y, 1)) {
                    return new int[]{x, y};
                }
            }
        }
        throw new AssertionError("missing doppel fixture cell");
    }
}
