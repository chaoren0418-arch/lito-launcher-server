package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class NativeLitoRentalTownMapCatalogTest {
    @Test void pinsSelectedClientTownPagesAndImageResources() {
        assertEquals(2, NativeLitoRentalTownMapCatalog.all().size());
        assertEquals(new NativeLitoRentalTownMapCatalog.Page(
                        500, 1, 0x1A2C, "town"),
                NativeLitoRentalTownMapCatalog.findByMap(500));
        assertEquals(new NativeLitoRentalTownMapCatalog.Page(
                        560, 2, 0x1A2D, "town2"),
                NativeLitoRentalTownMapCatalog.findByMap(560));
        assertEquals(311, NativeLitoRentalTownMapCatalog.ALPHA_MASK_WIDTH);
        assertEquals(215, NativeLitoRentalTownMapCatalog.ALPHA_MASK_HEIGHT);
        assertEquals(
                "selected-LiTo.bin-sub_4AFFB0-sub_48D990-sub_48DE10-LTmapstr",
                NativeLitoRentalTownMapCatalog.AUTHORITY);
    }

    @Test void doesNotInventPagesForRentalHotelOrBattleMaps() {
        for (int mapId : new int[] {0, 501, 502, 503, 504, 559, 561}) {
            assertNull(NativeLitoRentalTownMapCatalog.findByMap(mapId));
            assertEquals(0, NativeLitoRentalTownMapCatalog.wirePageForMap(mapId));
        }
    }
}
