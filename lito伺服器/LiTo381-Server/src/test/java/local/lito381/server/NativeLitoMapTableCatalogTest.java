package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class NativeLitoMapTableCatalogTest {
    @Test
    void exactTableHasFortyFiveArenaRowsAndTwoTownRows() {
        assertEquals(47, NativeLitoMapTableCatalog.active().size());
        Set<Integer> towns = NativeLitoMapTableCatalog.active().stream()
                .filter(entry -> entry.category().equals("town"))
                .map(NativeLitoMapTableCatalog.Entry::mapId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(500, 560), towns);

        Set<Integer> arenas = NativeLitoMapTableCatalog.active().stream()
                .filter(entry -> !entry.category().equals("town"))
                .map(NativeLitoMapTableCatalog.Entry::mapId)
                .collect(Collectors.toSet());
        assertEquals(ArenaMapCatalog.mapIds(), arenas);
    }

    @Test
    void lookupKeepsTownAndUnreachableResourceBoundariesDistinct() {
        assertTrue(NativeLitoMapTableCatalog.isTown(500));
        assertTrue(NativeLitoMapTableCatalog.isTown(560));
        Set<Integer> resourceOnly = Set.of(
                501, 502, 503, 507,
                541, 542, 543, 545, 546, 555, 567, 568,
                16_384, 16_896, 17_408);
        assertEquals(resourceOnly, NativeLitoMapTableCatalog.resourceOnlyMapIds());
        for (int mapId : resourceOnly) {
            assertNull(NativeLitoMapTableCatalog.forMap(mapId));
            assertFalse(NativeLitoMapTableCatalog.isActive(mapId));
            assertTrue(NativeLitoMapTableCatalog.hasResourceOnlyTerrain(mapId));
        }
    }

    @Test
    void activeAndResourceOnlyNamespacesNeverOverlap() {
        Set<Integer> active = NativeLitoMapTableCatalog.active().stream()
                .map(NativeLitoMapTableCatalog.Entry::mapId)
                .collect(Collectors.toSet());
        assertTrue(active.stream()
                .noneMatch(NativeLitoMapTableCatalog.resourceOnlyMapIds()::contains));
        assertEquals(62, active.size()
                + NativeLitoMapTableCatalog.resourceOnlyMapIds().size());
    }
}
