package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeLitoTownReachabilityBoundaryTest {
    private static final Set<Integer> TOWN_MAPS = Set.of(500, 560);

    @Test
    void ordinaryNativeModeSteppingDoesNotAdmitServerGatedTownScenes() {
        Set<Integer> roomMaps = java.util.Arrays.stream(BattleRules.Mode.values())
                .filter(BattleRules.Mode::isBattleScene)
                .flatMap(mode -> mode.maps().stream())
                .collect(Collectors.toSet());

        assertFalse(roomMaps.stream().anyMatch(TOWN_MAPS::contains));
    }

    @Test
    void exactWireModeSixOwnsBothRetainedTownRows() {
        assertEquals(BattleRules.Mode.TOWN, BattleRules.Mode.fromWire(6));
        assertEquals(Set.of(500, 560), Set.copyOf(BattleRules.Mode.TOWN.maps()));
        assertFalse(BattleRules.Mode.TOWN.isBattleScene());
        assertArrayEquals(new byte[] {(byte) 0xD5, 0x02, 0x01, 0x00, 0x00, 0x00, 0x06},
                Protocol.litoRoomMode(1, BattleRules.Mode.TOWN));
        assertArrayEquals(new byte[] {(byte) 0xD5, 0x03, 0x01, 0x00, 0x00, 0x00,
                        0x30, 0x02},
                Protocol.litoRoomMap(1, 560));
        Protocol.LitoGameInfoRequest request = Protocol.litoGameInfoRequest(
                new byte[] {(byte) 0xD1, 0x03, (byte) 0xF4, 0x01});
        Protocol.LitoMapSelectionRequest map =
                assertInstanceOf(Protocol.LitoMapSelectionRequest.class, request);
        assertEquals(500, map.mapId());
    }

    @Test
    void townScenesRemainOutsideTheProductionArenaCatalog() {
        for (int mapId : TOWN_MAPS) {
            assertThrows(IllegalArgumentException.class,
                    () -> ArenaMapCatalog.forMap(mapId));
        }
    }

    @Test
    void exactTownS32ManifestsExposeLoadedSectorsWithoutInventingSpawnPoints() {
        NativeLitoTownCatalog.Town lodun = NativeLitoTownCatalog.forMap(500);
        assertEquals(18, lodun.loadedSectors().size());
        assertEquals("871DED11A6E61698E6C221C41F9E48DCF134AD0BC0DC09778A11263A09967F13",
                lodun.resourceSha256());
        assertEquals(32640, lodun.minX());
        assertEquals(32895, lodun.maxX());
        assertEquals(32640, lodun.minY());
        assertEquals(32959, lodun.maxY());
        assertTrue(lodun.containsResourceCoordinate(32770, 32818));

        NativeLitoTownCatalog.Town lenoa = NativeLitoTownCatalog.forMap(560);
        assertEquals(11, lenoa.loadedSectors().size());
        assertEquals("DEEC05D279524BB9B565A4DD276403CD849726EFC5C7CCE13ADCA6B5AA7758B7",
                lenoa.resourceSha256());
        assertEquals(32128, lenoa.minX());
        assertEquals(32447, lenoa.maxX());
        assertEquals(32640, lenoa.minY());
        assertEquals(32831, lenoa.maxY());
        assertTrue(lenoa.containsResourceCoordinate(32328, 32768));
        assertFalse(lenoa.containsResourceCoordinate(32128, 32640));
    }
}
