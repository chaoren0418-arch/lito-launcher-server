package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldHotelCatalogTest {
    @Test
    void retainedNineInnContractOwnsEighteenDistinctAppendOnlyMaps()
            throws Exception {
        assertEquals(9, ClassicWorldHotelCatalog.inns().size());
        assertEquals(18, ClassicWorldHotelCatalog.MAP_IDS.size());
        assertEquals(18, new HashSet<>(ClassicWorldHotelCatalog.MAP_IDS).size());
        int firstHotel = ClassicWorldMapCatalog.supportedMapIds().indexOf(
                ClassicWorldHotelCatalog.MAP_IDS.get(0));
        assertEquals(ClassicWorldHotelCatalog.MAP_IDS,
                ClassicWorldMapCatalog.supportedMapIds().subList(
                        firstHotel, firstHotel + ClassicWorldHotelCatalog.MAP_IDS.size()));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int mapId : ClassicWorldHotelCatalog.MAP_IDS) {
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(32704, map.startX());
            assertEquals(32767, map.endX());
            assertEquals(32768, map.startY());
            assertEquals(32831, map.endY());
            assertEquals(64, map.width());
            assertEquals(64, map.height());
            assertEquals(ClassicWorldHotelCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertFalse(map.allowsRandomTeleport());

            digest.reset();
            try (InputStream input = getClass().getResourceAsStream(
                    "/maps/" + mapId + ".l38.gz")) {
                assertNotNull(input);
                digest.update(input.readAllBytes());
            }
            assertEquals(ClassicWorldHotelCatalog.MAP_RESOURCE_SHA256.get(mapId),
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
    }

    @Test
    void everyInnkeeperMatchesItsSelectedStaticNpcAndBothLandings() {
        Set<Integer> npcIds = new HashSet<>();
        for (ClassicWorldHotelCatalog.Inn inn : ClassicWorldHotelCatalog.inns()) {
            assertTrue(npcIds.add(inn.npcId()));
            ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                    inn.exteriorMapId(), inn.objectId());
            assertNotNull(npc);
            assertTrue(ClassicWorldHotelCatalog.isInnkeeper(npc));
            assertEquals(inn.npcId(), npc.npcId());
            assertEquals(inn.x(), npc.x());
            assertEquals(inn.y(), npc.y());
            for (ClassicWorldHotelCatalog.Location landing
                    : new ClassicWorldHotelCatalog.Location[] {
                        inn.room(), inn.hall()}) {
                assertTrue(ClassicWorldMapCatalog.forMap(landing.mapId())
                        .standingPassable(landing.x(), landing.y()),
                        "stranded hotel entry " + landing.mapId());
            }
        }
    }

    @Test
    void selectedDungeonRowsExposeOnlyTheEighteenHotelExits() {
        long count = ClassicWorldDungeonCatalog.all().stream()
                .filter(ClassicWorldDungeonCatalog::isHotelRoute).count();
        assertEquals(18, count);
        assertEquals(645, ClassicWorldDungeonCatalog.all().size());
        assertEquals(ClassicWorldHotelCatalog.DUNGEON_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.HOTEL_SOURCE_SHA256);
        for (ClassicWorldDungeonCatalog.Route route
                : ClassicWorldDungeonCatalog.all().stream()
                    .filter(ClassicWorldDungeonCatalog::isHotelRoute).toList()) {
            assertTrue(ClassicWorldHotelCatalog.isHotelMap(route.sourceMapId()));
            assertEquals(0, route.requiredItemCatalogId());
            assertTrue(ClassicWorldDungeonCatalog.destinationLandingSafe(route));
        }
    }

    @Test
    void exactHotelKeyRowIsNativeTradableNonStackableIronKey() {
        InventoryCatalog.Definition key =
                InventoryCatalog.require(ClassicWorldHotelCatalog.KEY_ITEM_ID);
        assertEquals(0xFF, key.useType());
        assertEquals(59, key.icon());
        assertEquals(151, key.groundGfx());
        assertEquals("$954", key.name());
        assertEquals(1_000, key.unitWeight());
        assertFalse(key.stackable());
        assertTrue(key.tradable());
        assertEquals(0, key.blessing());
    }

    @Test
    void sourceEvidencePinsTextPacketAndDatabaseFamilies() {
        for (String hash : Set.of(
                ClassicWorldHotelCatalog.MAPIDS_SOURCE_SHA256,
                ClassicWorldHotelCatalog.GETBACK_SOURCE_SHA256,
                ClassicWorldHotelCatalog.DUNGEON_SOURCE_SHA256,
                ClassicWorldHotelCatalog.KEY_ITEM_SOURCE_SHA256,
                ClassicWorldHotelCatalog.NPC_SPAWN_SOURCE_SHA256,
                ClassicWorldHotelCatalog.NPC_ACTION_SOURCE_SHA256,
                ClassicWorldHotelCatalog.RETAINED_C_NPC_ACTION_SHA256,
                ClassicWorldHotelCatalog.RETAINED_C_AMOUNT_SHA256,
                ClassicWorldHotelCatalog.RETAINED_S_HOW_MANY_KEY_SHA256,
                ClassicWorldHotelCatalog.SELECTED_TEXT_PAK_SHA256)) {
            assertEquals(64, hash.length());
        }
        assertEquals(300, ClassicWorldHotelCatalog.PRICE_PER_KEY);
        assertEquals(14_400, ClassicWorldHotelCatalog.LEASE_SECONDS);
        assertEquals(12, ClassicWorldHotelCatalog.ROOM_CAPACITY);
        assertEquals(4, ClassicWorldHotelCatalog.HALL_CAPACITY);
        assertEquals(8, ClassicWorldHotelCatalog.ROOM_KEY_MAXIMUM);
        assertEquals(16, ClassicWorldHotelCatalog.HALL_KEY_MAXIMUM);
    }
}
