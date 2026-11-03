package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldDeathContextTest {
    @Test void encounterGuardianUsesExactTwentyAndTenBoundaries() {
        assertTrue(ClassicWorldDeathContext.encounterProtects(20, 30));
        assertTrue(ClassicWorldDeathContext.encounterProtects(1, 11));
        assertFalse(ClassicWorldDeathContext.encounterProtects(20, 29));
        assertFalse(ClassicWorldDeathContext.encounterProtects(21, 31));
        assertFalse(ClassicWorldDeathContext.encounterProtects(20, 20));
    }

    @Test void castleAreaPkGateUsesExactNativeFields() {
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(33089, 32717));
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .isNormalZone(33089, 32717));
        assertTrue(ClassicWorldDeathContext.castleAreaSuppressesPk(
                4, 33089, 32717));
        assertTrue(ClassicWorldDeathContext.castleAreaSuppressesPk(
                320, 32900, 32850));
        assertFalse(ClassicWorldDeathContext.castleAreaSuppressesPk(
                4, 33313, 32339));
    }
}
