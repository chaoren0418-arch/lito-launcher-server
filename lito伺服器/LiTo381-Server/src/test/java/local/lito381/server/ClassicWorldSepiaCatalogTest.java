package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldSepiaCatalogTest {
    @Test
    void selectedMap302KeepsExactBoundsFlagsAndLanding() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID);
        assertEquals(302, map.mapId());
        assertEquals(32704, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32832, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(64, map.width());
        assertEquals(64, map.height());
        assertEquals("301534D948C572E2B5A22B7205FD55E4BCF954F74019457683EC543F5CCB8D08",
                map.sourceSha256());
        assertEquals("9D091C820FC90D166B17B8F268B6D8FF1FC6E4A6851B04DD437CC30D7A02B30D",
                ClassicWorldMapCatalog.MAP302_FLAGS_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32740, 32860));
        assertEquals("C274638C3B3B25868F19649829275742DD5E685D9FE0B53C606AA7C877B43BFF",
                resourceHash("/maps/302.l38.gz"));
    }

    @Test
    void selectedMap302ExplicitlyKeepsEmptyActorsMonstersAndDrops()
            throws Exception {
        assertEquals(0, ClassicWorldNpcCatalog.MAP302_ROW_COUNT);
        assertTrue(ClassicWorldNpcCatalog.all(302).isEmpty());
        assertEquals(0, ClassicWorldMonsterCatalog.MAP302_DEFINITION_COUNT);
        assertEquals(0, ClassicWorldMonsterCatalog.MAP302_POPULATION);
        assertTrue(ClassicWorldMonsterCatalog.definitions(302).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.MAP302_DROP_COUNT);
        assertEquals(0, ClassicWorldDropCatalog.MAP302_ITEM_COUNT);
        assertEquals(0, ClassicWorldDropCatalog.dropCount(302));
        assertTrue(ClassicWorldDropCatalog.items(302).isEmpty());
        assertEquals(ClassicWorldNpcCatalog.MAP302_RESOURCE_SHA256,
                resourceHash("/world/map302-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldMonsterCatalog.MAP302_RESOURCE_SHA256,
                resourceHash("/world/map302-monsters.lwm.gz"));
        assertEquals(ClassicWorldDropCatalog.MAP302_RESOURCE_SHA256,
                resourceHash("/world/map302-drops.lwd.gz"));
    }

    @Test
    void selectedMap302MissingGetbackRowUsesRetainedDefaultIslandReturn() {
        assertEquals("9C085F8BC0A0069739A6D4D3B787B3761DB85959056B8C5A0EBE75D80826A5FB",
                ClassicWorldGetbackCatalog.MAP302_ABSENCE_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33089, 33397, 2),
                ClassicWorldGetbackCatalog.resolve(302, 32740, 32860, 1, 0));
    }

    private String resourceHash(String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(name)) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
