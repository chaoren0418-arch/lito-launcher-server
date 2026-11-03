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

final class ClassicWorldGunterCatalogTest {
    @Test void selectedMap3PinsCollisionFlagsAndTalkingIslandRoundTrip()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(3);
        assertEquals(3, map.mapId());
        assertEquals(32576, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32704, map.startY());
        assertEquals(32959, map.endY());
        assertEquals(256, map.width());
        assertEquals(256, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP3_SOURCE_SHA256, map.sourceSha256());
        assertEquals("800187B442AEF7EA69DF6C9345224EF5E33ECDFD46D7064DACC458F20ADC6A6B",
                ClassicWorldMapCatalog.MAP3_FLAGS_SOURCE_SHA256);
        assertEquals(ClassicWorldMapCatalog.MAP3_RESOURCE_SHA256,
                resourceHash("/maps/3.l38.gz"));
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32675, 32814));

        assertEquals("4A7DF3E1F96D2532396E3BA231F77D381F0874F60E1C89837AD165D790544B59",
                ClassicWorldDungeonCatalog.GUNTER_HOUSE_SOURCE_SHA256);
        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(0, 32510, 32863);
        assertNotNull(enter);
        assertEquals(171, enter.routeId());
        assertEquals(3, enter.destinationMapId());
        assertEquals(32675, enter.destinationX());
        assertEquals(32814, enter.destinationY());
        assertEquals(0, enter.destinationHeading());
        assertEquals(0, enter.requiredItemCatalogId());
        assertFalse(enter.scheduledPirateShip());

        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(3, 32675, 32815);
        assertNotNull(exit);
        assertEquals(172, exit.routeId());
        assertEquals(0, exit.destinationMapId());
        assertEquals(32509, exit.destinationX());
        assertEquals(32865, exit.destinationY());
        assertEquals(4, exit.destinationHeading());
    }

    @Test void selectedMap3PinsGunterGuardsAndExplicitlyEmptyCombatPopulation()
            throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> npcs = ClassicWorldNpcCatalog.all(3);
        assertEquals(3, npcs.size());
        assertEquals(List.of(781, 782, 1119),
                npcs.stream().map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        ClassicWorldNpcCatalog.StaticNpc gunter = npcs.get(2);
        assertEquals(70522, gunter.npcId());
        assertEquals(32670, gunter.x());
        assertEquals(32789, gunter.y());
        assertTrue(gunter.fixedMerchant());
        assertEquals("gunterk9", gunter.normalAction());
        assertEquals(ClassicWorldNpcCatalog.MAP3_RESOURCE_SHA256,
                resourceHash("/world/map3-static-npcs.lwn.gz"));
        assertTrue(ClassicWorldMonsterCatalog.definitions(3).isEmpty());
        assertEquals(ClassicWorldMonsterCatalog.MAP3_RESOURCE_SHA256,
                resourceHash("/world/map3-monsters.lwm.gz"));
        assertEquals(0, ClassicWorldDropCatalog.dropCount(3));
        assertTrue(ClassicWorldDropCatalog.items(3).isEmpty());
        assertEquals(ClassicWorldDropCatalog.MAP3_RESOURCE_SHA256,
                resourceHash("/world/map3-drops.lwd.gz"));
    }

    @Test void selectedMap3HomeScrollUsesItsExactTalkingIslandGetback() {
        assertEquals("795AD4F3CADBB6184317E01C95D4A1DF450DC7970AA05C0FCACA0C859C1BF5E6",
                ClassicWorldGetbackCatalog.MAP3_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(0, 32574, 32941, 1),
                ClassicWorldGetbackCatalog.resolve(3, 32670, 32789, 1, 0));
        assertTrue(ClassicWorldMapCatalog.forMap(0).standingPassable(32574, 32941));
    }

    private String resourceHash(String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(name)) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
