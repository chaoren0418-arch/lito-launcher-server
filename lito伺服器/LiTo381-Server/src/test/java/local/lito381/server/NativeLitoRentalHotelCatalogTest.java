package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeLitoRentalHotelCatalogTest {
    @Test void selectedResourceGeometryAndExplicitPolicyArePinned() throws Exception {
        NativeLitoRentalHotelCatalog catalog = NativeLitoRentalHotelCatalog.get();
        assertFalse(NativeLitoRentalHotelCatalog.HISTORICAL_MAP_TYPE_ASSIGNMENT_EXACT);
        assertFalse(NativeLitoRentalHotelCatalog.HISTORICAL_KEY_COUNT_THRESHOLDS_EXACT);
        assertTrue(NativeLitoRentalHotelCatalog.HISTORICAL_KEY_ITEM_RESOURCE_AVAILABLE);
        assertTrue(NativeLitoRentalHotelCatalog.HISTORICAL_KEY_TRANSFER_AVAILABLE);
        assertEquals(40312, NativeLitoRentalHotelCatalog.KEY_ITEM_ID);
        assertEquals(3, NativeLitoRentalHotelCatalog.rooms().size());
        assertEquals(501, NativeLitoRentalHotelCatalog.forKeyCount(1).mapId());
        assertEquals(502, NativeLitoRentalHotelCatalog.forKeyCount(2).mapId());
        assertEquals(502, NativeLitoRentalHotelCatalog.forKeyCount(4).mapId());
        assertEquals(503, NativeLitoRentalHotelCatalog.forKeyCount(5).mapId());
        assertEquals(503, NativeLitoRentalHotelCatalog.forKeyCount(8).mapId());
        assertNull(NativeLitoRentalHotelCatalog.forKeyCount(0));
        assertNull(NativeLitoRentalHotelCatalog.forKeyCount(9));

        int[] blocked = {8158, 8156, 8088};
        int index = 0;
        for (NativeLitoRentalHotelCatalog.Room room
                : NativeLitoRentalHotelCatalog.rooms()) {
            assertEquals(blocked[index++], catalog.blockedEdgeCount(room.mapId()));
            assertTrue(catalog.isStandingPassable(
                    room.mapId(), room.entryX(), room.entryY()));
            assertTrue(catalog.canStep(room.mapId(), room.entryX(), room.entryY(),
                    room.entryX() + 1, room.entryY())
                    || catalog.canStep(room.mapId(), room.entryX(), room.entryY(),
                            room.entryX() - 1, room.entryY())
                    || catalog.canStep(room.mapId(), room.entryX(), room.entryY(),
                            room.entryX(), room.entryY() + 1)
                    || catalog.canStep(room.mapId(), room.entryX(), room.entryY(),
                            room.entryX(), room.entryY() - 1));
        }
        assertFalse(catalog.isStandingPassable(500, 32800, 32800));
        assertNull(NativeLitoRentalHotelCatalog.forMap(500));

        try (InputStream input = getClass().getResourceAsStream(
                NativeLitoRentalHotelCatalog.RESOURCE)) {
            assertNotNull(input);
            assertEquals(NativeLitoRentalHotelCatalog.RESOURCE_SHA256,
                    java.util.HexFormat.of().withUpperCase().formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(input.readAllBytes())));
        }
    }
}
