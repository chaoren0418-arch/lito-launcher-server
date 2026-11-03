package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldGerardCatalogTest {
    @Test
    void selectedMap22KeepsExactBoundsFlagsAndPortalCell() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID);
        assertEquals(22, map.mapId());
        assertEquals(32704, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32704, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(128, map.width());
        assertEquals(128, map.height());
        assertEquals("0AF75942C1EAF9E0FAF89EBF853B62BDC71D877B402FDB4545268B5F3A0FF81C",
                map.sourceSha256());
        assertEquals("F69A2CB1905914484F21258FEA5EC2AFCEB29AFD28941D7BA89439C5B8370873",
                ClassicWorldMapCatalog.MAP22_FLAGS_SOURCE_SHA256);
        assertEquals("33AA4565F93AEA1117AB15412F84BE2003FE830199D63374CF0669E600A98EE3",
                ClassicWorldMapCatalog.MAP22_FLAGS_SECONDARY_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32769, 32767));
        assertEquals("1E9DD0913BD628DAF1AA636D137C2D805D03BC6FBA8D4AED672D090C10DC5841",
                resourceHash("/maps/22.l38.gz"));
    }

    @Test
    void selectedMap22PinsOnlyItsExactMap4RoundTrip() {
        assertEquals("3E7F0F7DD420BD03207918C1A9C4A7883198AE84D0DAA42BECDB106509D248A7",
                ClassicWorldDungeonCatalog.GERARD_SOURCE_SHA256);
        assertEquals("05F0F4027A0DDA7E92A00187DD793B068B0677955466C3D673B3418A823FEC12",
                ClassicWorldDungeonCatalog.GERARD_NORMALIZED_SOURCE_SHA256);
        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(4, 33138, 33357);
        assertEquals(67, enter.routeId());
        assertEquals(22, enter.destinationMapId());
        assertEquals(32769, enter.destinationX());
        assertEquals(32767, enter.destinationY());
        assertEquals(5, enter.destinationHeading());
        assertEquals(0, enter.requiredItemCatalogId());
        assertFalse(enter.scheduledPirateShip());

        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(22, 32769, 32767);
        assertEquals(68, exit.routeId());
        assertEquals(4, exit.destinationMapId());
        assertEquals(33138, exit.destinationX());
        assertEquals(33357, exit.destinationY());
        assertEquals(2, exit.destinationHeading());
        assertEquals(2, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.sourceMapId() == 22
                        || route.destinationMapId() == 22).count());
    }

    @Test
    void selectedMap22ExplicitlyKeepsEmptyActorsMonstersAndDrops()
            throws Exception {
        assertEquals(0, ClassicWorldNpcCatalog.MAP22_ROW_COUNT);
        assertTrue(ClassicWorldNpcCatalog.all(22).isEmpty());
        assertEquals(0, ClassicWorldMonsterCatalog.MAP22_DEFINITION_COUNT);
        assertEquals(0, ClassicWorldMonsterCatalog.MAP22_POPULATION);
        assertTrue(ClassicWorldMonsterCatalog.definitions(22).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.MAP22_DROP_COUNT);
        assertEquals(0, ClassicWorldDropCatalog.MAP22_ITEM_COUNT);
        assertEquals(0, ClassicWorldDropCatalog.dropCount(22));
        assertTrue(ClassicWorldDropCatalog.items(22).isEmpty());
        assertEquals(ClassicWorldNpcCatalog.MAP22_RESOURCE_SHA256,
                resourceHash("/world/map22-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldMonsterCatalog.MAP22_RESOURCE_SHA256,
                resourceHash("/world/map22-monsters.lwm.gz"));
        assertEquals(ClassicWorldDropCatalog.MAP22_RESOURCE_SHA256,
                resourceHash("/world/map22-drops.lwd.gz"));
    }

    @Test
    void selectedMap22HomeScrollUsesItsDirectMap4GetbackRow() {
        assertEquals("5EA42DBE903B1C662A6A9CFDF913F52D642D2C29D9CF7EBE77CEE306B9B2B78D",
                ClassicWorldGetbackCatalog.MAP22_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33089, 33397, 0),
                ClassicWorldGetbackCatalog.resolve(22, 32769, 32767, 1, 0));
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(33089, 33397));
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
