package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class NativeLitoTownCatalogTest {
    @Test
    void exactZoneTableMapsLodunAndLenoaWithoutPromotingThemToArenas() {
        assertEquals(2, NativeLitoTownCatalog.towns().size());
        assertEquals("LODUN", NativeLitoTownCatalog.forMap(500).zoneKey());
        assertEquals("LENOA", NativeLitoTownCatalog.forMap(560).zoneKey());
        assertEquals(0, NativeLitoTownCatalog.forMap(500).zoneIndex());
        assertEquals(1, NativeLitoTownCatalog.forMap(560).zoneIndex());

        Set<Integer> townIds = NativeLitoTownCatalog.towns().stream()
                .map(NativeLitoTownCatalog.Town::mapId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(500, 560), townIds);
        assertEquals(Set.of(), townIds.stream()
                .filter(ArenaMapCatalog.mapIds()::contains)
                .collect(Collectors.toSet()));
        assertNull(NativeLitoTownCatalog.forMap(501));
    }
}
