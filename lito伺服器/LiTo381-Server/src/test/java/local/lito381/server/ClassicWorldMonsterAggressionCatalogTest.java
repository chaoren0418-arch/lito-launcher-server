package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterAggressionCatalogTest {
    @Test
    void selectedCatalogCoversEveryImportedMonsterTemplate() {
        Set<Integer> npcIds = new HashSet<>();
        for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                ClassicWorldMonsterCatalog.MonsterTemplate template =
                        definition.template();
                ClassicWorldMonsterAggressionCatalog.Profile profile =
                        ClassicWorldMonsterAggressionCatalog.forNpc(template.npcId());
                assertNotNull(profile, "missing aggression NPC " + template.npcId());
                assertEquals(template.aggressive(), profile.aggressive(),
                        "agro mismatch NPC " + template.npcId());
                npcIds.add(template.npcId());
            }
        }
        assertEquals(422, npcIds.size());
        assertEquals(430, ClassicWorldMonsterAggressionCatalog.size());

        ClassicWorldMonsterAggressionCatalog.Profile ogre =
                ClassicWorldMonsterAggressionCatalog.forNpc(45278);
        assertTrue(ogre.aggressive());
        assertTrue(ogre.agroSosc());
        assertFalse(ogre.agroCoi());
        assertEquals("ogre", ogre.family());
        assertEquals(0, ogre.agroFamily());
        assertEquals(1, ClassicWorldMonsterAggressionCatalog
                .forNpc(45009).agroFamily());
        ClassicWorldMonsterAggressionCatalog.Profile darkElfGeneral =
                ClassicWorldMonsterAggressionCatalog.forNpc(45900);
        assertFalse(darkElfGeneral.aggressive());
        assertFalse(darkElfGeneral.agroSosc());
        assertTrue(darkElfGeneral.agroCoi());
        assertEquals(-2, ClassicWorldMonsterAggressionCatalog.forNpc(45736).karma());
    }

    @Test
    void retainedDecisionOrderCoversPassiveShapeInvisibleKarmaAndSpecialCases() {
        var active = profile(45001, true, false, false, "", -1, -1, 0);
        var shapeOnly = profile(45002, false, true, false, "", -1, -1, 0);
        var coiOnly = profile(45003, false, false, true, "", -1, -1, 0);
        var passive = profile(45004, false, false, false, "", -1, -1, 0);

        assertEquals(A.AGGRESSIVE, acquire(4, active, player(1, 61, 61,
                false, false, 0, 0, false)));
        // Selected retained source checks agrososc and then still checks agro;
        // shape change does not suppress a normally aggressive monster.
        assertEquals(A.AGGRESSIVE, acquire(4, active, player(1, 61, 29,
                true, false, 0, 0, false)));
        assertEquals(A.SHAPE_CHANGE, acquire(4, shapeOnly, player(1, 61, 29,
                true, false, 0, 0, false)));
        assertEquals(A.NONE, acquire(4, shapeOnly, player(1, 61, 29,
                true, true, 0, 0, false)));
        assertEquals(A.NONE, acquire(4, coiOnly, player(1, 61, 61,
                false, true, 0, 0, false)));
        assertEquals(A.NONE, acquire(4, passive, player(1, 61, 61,
                false, false, 0, 0, false)));
        assertEquals(A.CHAOTIC_PLAYER, acquire(4, passive, player(1, 61, 61,
                false, true, -1001, 0, false)));

        var negativeKarma = profile(45736, true, true, true, "daemon",
                -1, -1, -2);
        assertEquals(A.NONE, acquire(4, negativeKarma, player(1, 61, 61,
                false, false, 0, 1, false)));
        assertEquals(A.NONE, acquire(4, negativeKarma, player(1, 61, 6034,
                false, false, 0, 0, false)));
        assertEquals(A.AGGRESSIVE, acquire(4, negativeKarma, player(1, 61, 61,
                false, false, 0, -1, false)));

        assertEquals(A.ARENA, acquire(88, passive, player(1, 61, 61,
                false, false, 0, 0, false)));
        assertEquals(A.NONE, acquire(88, passive, player(1, 61, 61,
                false, true, 0, 0, false)));
        assertEquals(A.ARENA, acquire(88, coiOnly, player(1, 61, 61,
                false, true, 0, 0, false)));
    }

    @Test
    void retainedKurtzOrcTempleAndOriginalGfxBranchesStayDistinct() {
        var kurtz = profile(45600, false, false, false, "", -1, -1, 0);
        assertEquals(A.KURTZ_CROWN, acquire(4, kurtz, player(0, 0, 0,
                false, true, 0, 0, false)));
        assertEquals(A.NONE, acquire(4, kurtz, player(0, 0, 29,
                true, false, 0, 0, false)));
        assertEquals(A.KURTZ_DARK_ELF, acquire(4, kurtz, player(4, 2786, 29,
                true, true, 0, 0, false)));

        var orc = profile(45009, true, true, true, "orc", -1, -1, 0);
        assertEquals(A.NONE, acquire(4, orc, player(1, 61, 61,
                false, false, 0, 0, true)));
        assertEquals(A.AGGRESSIVE, acquire(4, orc, player(1, 61, 61,
                false, false, 0, 0, false)));
        assertEquals(A.NONE, acquire(410, orc, player(1, 61, 4261,
                true, false, 0, 0, false)));

        var gfx = profile(49999, false, false, false, "", 61, 138, 0);
        assertEquals(A.BASE_GFX_1, acquire(4, gfx, player(1, 61, 29,
                true, false, 0, 0, false)));
        assertEquals(A.BASE_GFX_2, acquire(4, gfx, player(2, 138, 61,
                false, false, 0, 0, false)));
    }

    private static ClassicWorldMonsterAggressionCatalog.Profile profile(
            int npcId, boolean agro, boolean sosc, boolean coi, String family,
            int gfx1, int gfx2, int karma) {
        return new ClassicWorldMonsterAggressionCatalog.Profile(
                npcId, agro, sosc, coi, family, 0, gfx1, gfx2, karma);
    }

    private static ClassicWorldMonsterAggressionCatalog.PlayerDisposition player(
            int type, int baseGfx, int currentGfx, boolean shape,
            boolean invisible, int lawful, int karma, boolean orcOwner) {
        return new ClassicWorldMonsterAggressionCatalog.PlayerDisposition(
                type, baseGfx, currentGfx, shape, invisible, lawful, karma, orcOwner);
    }

    private static ClassicWorldMonsterAggressionCatalog.Acquisition acquire(
            int mapId, ClassicWorldMonsterAggressionCatalog.Profile monster,
            ClassicWorldMonsterAggressionCatalog.PlayerDisposition player) {
        return ClassicWorldMonsterAggressionCatalog.acquisition(mapId, monster, player);
    }

    private static final class A {
        static final ClassicWorldMonsterAggressionCatalog.Acquisition NONE =
                ClassicWorldMonsterAggressionCatalog.Acquisition.NONE;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition ARENA =
                ClassicWorldMonsterAggressionCatalog.Acquisition.ARENA;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition KURTZ_CROWN =
                ClassicWorldMonsterAggressionCatalog.Acquisition.KURTZ_CROWN;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition KURTZ_DARK_ELF =
                ClassicWorldMonsterAggressionCatalog.Acquisition.KURTZ_DARK_ELF;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition CHAOTIC_PLAYER =
                ClassicWorldMonsterAggressionCatalog.Acquisition.CHAOTIC_PLAYER;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition SHAPE_CHANGE =
                ClassicWorldMonsterAggressionCatalog.Acquisition.SHAPE_CHANGE;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition AGGRESSIVE =
                ClassicWorldMonsterAggressionCatalog.Acquisition.AGGRESSIVE;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition BASE_GFX_1 =
                ClassicWorldMonsterAggressionCatalog.Acquisition.BASE_GFX_1;
        static final ClassicWorldMonsterAggressionCatalog.Acquisition BASE_GFX_2 =
                ClassicWorldMonsterAggressionCatalog.Acquisition.BASE_GFX_2;
    }
}
