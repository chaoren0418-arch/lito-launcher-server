package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldBowCatalogTest {
    @Test
    void preservesAllTwentyNineInvisibleMapTwoAperturesAndNativeEndpoints() {
        List<ClassicWorldNpcCatalog.StaticNpc> bows = ClassicWorldBowCatalog.all();
        assertEquals(29, bows.size());
        assertEquals(List.of(
                679, 680, 681, 682, 683, 684, 685, 686, 687, 688,
                689, 690, 691, 692, 693, 694, 695, 696, 697, 698,
                699, 700, 701, 702, 703, 704, 705, 706, 709),
                bows.stream().map(
                        ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals(List.of(
                "32678,32859", "32680,32863", "32682,32863",
                "32663,32864", "32666,32865", "32660,32868",
                "32660,32869", "32661,32873", "32725,32855",
                "32723,32842", "32737,32853", "32735,32853",
                "32733,32853", "32731,32853", "32729,32853",
                "32727,32853", "32725,32853", "32691,32834",
                "32791,32838", "32793,32841", "32715,32800",
                "32719,32801", "32756,32802", "32757,32803",
                "32752,32804", "32752,32805", "32736,32806",
                "32734,32807", "32670,32872"),
                bows.stream().map(ClassicWorldBowCatalog::lane)
                        .map(lane -> lane.endpointX() + "," + lane.endpointY())
                        .toList());

        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldBowCatalog.MAP_ID);
        for (ClassicWorldNpcCatalog.StaticNpc bow : bows) {
            assertTrue(ClassicWorldBowCatalog.isBow(bow));
            assertEquals(0, bow.gfxId());
            assertTrue(map.rawCell(bow.x(), bow.y()) != 0);
            ClassicWorldBowCatalog.Lane lane = ClassicWorldBowCatalog.lane(bow);
            int dx = bow.heading() == 2 ? 1 : bow.heading() == 6 ? -1 : 0;
            int dy = bow.heading() == 4 ? 1 : bow.heading() == 0 ? -1 : 0;
            int firstZeroX = lane.endpointX() - dx;
            int firstZeroY = lane.endpointY() - dy;
            assertEquals(0, map.rawCell(firstZeroX, firstZeroY),
                    "spawn=" + bow.spawnId());
            assertTrue(map.rawCell(firstZeroX - dx, firstZeroY - dy) != 0,
                    "spawn=" + bow.spawnId());
            assertTrue(lane.contains(bow.x(), bow.y()));
        }
    }

    @Test
    void aperturePacketKeepsTargetZeroDamageFifteenAndFixedProjectileTail() {
        ClassicWorldNpcCatalog.StaticNpc bow = ClassicWorldBowCatalog.all().get(0);
        ClassicWorldBowCatalog.Lane lane = ClassicWorldBowCatalog.lane(bow);
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.classicWorldBowAttack(
                        bow.objectId(), 0, bow.heading(), bow.x(), bow.y(),
                        lane.endpointX(), lane.endpointY()));
        assertEquals(Protocol.S_ATTACK, packet.c());
        assertEquals(BattleActionCodes.ATTACK, packet.c());
        assertEquals(bow.objectId(), packet.d());
        assertEquals(0, packet.d());
        assertEquals(ClassicWorldBowCatalog.DAMAGE, packet.c());
        assertEquals(bow.heading(), packet.c());
        assertEquals(ClassicWorldBowCatalog.NATIVE_PROJECTILE_SEQUENCE, packet.d());
        assertEquals(ClassicWorldBowCatalog.PROJECTILE_GFX, packet.h());
        assertEquals(0x7f, packet.c());
        assertEquals(bow.x(), packet.h());
        assertEquals(bow.y(), packet.h());
        assertEquals(lane.endpointX(), packet.h());
        assertEquals(lane.endpointY(), packet.h());
        assertEquals(0, packet.c());
        assertEquals("", packet.s());
        assertEquals(0, packet.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.rangedPhysicalAttack(
                        1, bow.objectId(), 0, ClassicWorldBowCatalog.DAMAGE,
                        bow.heading(), ClassicWorldBowCatalog.NATIVE_PROJECTILE_SEQUENCE,
                        ClassicWorldBowCatalog.PROJECTILE_GFX,
                        bow.x(), bow.y(), lane.endpointX(), lane.endpointY()));
        assertFalse(ClassicWorldBowCatalog.isBow(
                ClassicWorldNpcCatalog.all(1).get(0)));
    }
}
