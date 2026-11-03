package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldValakasCatalogTest {
    @Test
    void selectedMap67KeepsExactBoundsFlagsAndPortalCells() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID);
        assertEquals(67, map.mapId());
        assertEquals(32640, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32704, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(192, map.width());
        assertEquals(192, map.height());
        assertEquals("A4C22680348540BD77AF7A8EA708214C7E46515D86FE10894DDD4B499E23BAA2",
                map.sourceSha256());
        assertEquals("B2B3F217275C2FC8188DC1A1602167297B7F2E99D9C633B6CDEB60387C63E2AE",
                ClassicWorldMapCatalog.MAP67_FLAGS_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32797, 32746));
        assertTrue(map.standingPassable(32798, 32746));
        assertEquals("A3AE887F2261CC69AAA031467E5FD4383FD717ED7AF5BEA00CBFE7D581214BE8",
                resourceHash("/maps/67.l38.gz"));
    }

    @Test
    void selectedMap67ExplicitlyKeepsEmptyActorsMonstersAndDrops()
            throws Exception {
        assertEquals(0, ClassicWorldNpcCatalog.MAP67_ROW_COUNT);
        assertEquals("8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                ClassicWorldNpcCatalog.MAP67_SOURCE_SHA256);
        assertTrue(ClassicWorldNpcCatalog.all(67).isEmpty());
        assertEquals(0, ClassicWorldMonsterCatalog.MAP67_DEFINITION_COUNT);
        assertEquals(0, ClassicWorldMonsterCatalog.MAP67_POPULATION);
        assertTrue(ClassicWorldMonsterCatalog.definitions(67).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.MAP67_DROP_COUNT);
        assertEquals(0, ClassicWorldDropCatalog.MAP67_ITEM_COUNT);
        assertEquals(0, ClassicWorldDropCatalog.dropCount(67));
        assertTrue(ClassicWorldDropCatalog.items(67).isEmpty());
        assertEquals(ClassicWorldNpcCatalog.MAP67_RESOURCE_SHA256,
                resourceHash("/world/map67-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldMonsterCatalog.MAP67_RESOURCE_SHA256,
                resourceHash("/world/map67-monsters.lwm.gz"));
        assertEquals(ClassicWorldDropCatalog.MAP67_RESOURCE_SHA256,
                resourceHash("/world/map67-drops.lwd.gz"));
    }

    @Test
    void selectedMap67HomeScrollUsesItsMapWideAdenTownNineRow() {
        assertEquals("A1F96453400295F6C5B4EBF1C59C1C2F99D039F87ABA0A3F7BCC1F7B17493E93",
                ClassicWorldGetbackCatalog.MAP67_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33702, 32492, 9),
                ClassicWorldGetbackCatalog.resolve(67, 32797, 32746, 1, 0));
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
