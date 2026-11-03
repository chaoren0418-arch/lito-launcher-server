package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NativeLitoZoneCatalogTest {
    @Test
    void exactZoneTableContainsTwoWholeMapTownLabels() {
        assertEquals(2, NativeLitoZoneCatalog.colors().size());
        assertEquals(new NativeLitoZoneCatalog.Color(0, 45, 95, 209),
                NativeLitoZoneCatalog.colors().get(0));
        assertEquals(new NativeLitoZoneCatalog.Color(1, 10, 173, 90),
                NativeLitoZoneCatalog.colors().get(1));

        NativeLitoZoneCatalog.Zone lodun = NativeLitoZoneCatalog.forMap(500);
        NativeLitoZoneCatalog.Zone lenoa = NativeLitoZoneCatalog.forMap(560);
        assertEquals("LODUN", lodun.key());
        assertEquals(0, lodun.colorIndex());
        assertTrue(lodun.coversWholeMap());
        assertEquals("LENOA", lenoa.key());
        assertEquals(1, lenoa.colorIndex());
        assertTrue(lenoa.coversWholeMap());
        assertNull(NativeLitoZoneCatalog.forMap(501));
    }

    @Test
    void everyZoneRowBelongsToAnExactTownRowAndNeverAnArena() {
        for (NativeLitoZoneCatalog.Zone zone : NativeLitoZoneCatalog.zones()) {
            assertTrue(NativeLitoMapTableCatalog.isTown(zone.mapId()));
            assertTrue(!ArenaMapCatalog.mapIds().contains(zone.mapId()));
        }
    }
}
