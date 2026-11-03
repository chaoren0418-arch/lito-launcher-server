package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class NativeMapAndShipCatalogTest {
    @Test void coversEveryNativeMapPageExactlyOnce() {
        assertEquals(18, NativeMapItemCatalog.all().size());
        HashSet<Integer> pages = new HashSet<>();
        for (NativeMapItemCatalog.Item item : NativeMapItemCatalog.all()) {
            assertTrue(pages.add(item.page()));
            InventoryCatalog.Definition definition = InventoryCatalog.require(item.catalogId());
            assertEquals(InventoryCatalog.Effect.MAP, definition.effect());
            assertEquals(0, definition.useType());
            assertFalse(definition.stackable());
        }
        assertEquals(18, pages.size());
        assertEquals(16, NativeMapItemCatalog.requirePage(InventoryCatalog.WORLD_MAP));
    }

    @Test void acceptsOnlyExactBinaryShipEnvelopes() {
        assertEquals(4, ShipRouteCatalog.all().size());
        assertNotNull(ShipRouteCatalog.resolve(5, 4, 32564, 32723));
        assertNotNull(ShipRouteCatalog.resolve(5, 4, 32566, 32730));
        assertNotNull(ShipRouteCatalog.resolve(6, 0, 32630, 32972));
        assertNotNull(ShipRouteCatalog.resolve(6, 0, 32633, 32982));
        assertNotNull(ShipRouteCatalog.resolve(83, 70, 32936, 33052));
        assertNotNull(ShipRouteCatalog.resolve(84, 4, 33427, 33500));
        assertNull(ShipRouteCatalog.resolve(5, 4, 32563, 32723));
        assertNull(ShipRouteCatalog.resolve(5, 0, 32564, 32723));
        assertNull(ShipRouteCatalog.resolve(446, 440, 32297, 33086));
    }

    @Test void mapsBothExactEnterShipSelectorsToFixedClassicRoutes() {
        assertEquals(2, ShipRouteCatalog.boardings().size());
        ShipRouteCatalog.Boarding talkingIsland = ShipRouteCatalog.boarding(0);
        assertNotNull(talkingIsland);
        assertEquals(0, talkingIsland.sourceMap());
        assertEquals(5, talkingIsland.clientAnimationState());
        assertEquals(InventoryCatalog.SHIP_TICKET_TO_GLUDIO,
                talkingIsland.ticketCatalogId());
        assertEquals(5, talkingIsland.destinationMap());
        assertEquals(32733, talkingIsland.destinationX());
        assertEquals(32797, talkingIsland.destinationY());
        assertTrue(talkingIsland.accepts(0, 32622, 32960));
        assertTrue(talkingIsland.accepts(0, 32638, 32979));
        assertFalse(talkingIsland.accepts(0, 32639, 32979));

        ShipRouteCatalog.Boarding mainland = ShipRouteCatalog.boarding(1);
        assertNotNull(mainland);
        assertEquals(4, mainland.sourceMap());
        assertEquals(1, mainland.clientAnimationState());
        assertEquals(InventoryCatalog.SHIP_TICKET_TO_TALKING_ISLAND,
                mainland.ticketCatalogId());
        assertEquals(6, mainland.destinationMap());
        assertEquals(32735, mainland.destinationX());
        assertEquals(32795, mainland.destinationY());
        assertTrue(mainland.accepts(4, 32550, 32720));
        assertTrue(mainland.accepts(4, 32564, 32729));
        assertFalse(mainland.accepts(5, 32564, 32729));
        assertNull(ShipRouteCatalog.boarding(2));
    }
}
