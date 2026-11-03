package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ClassicWorldMapRecoveryCatalogTest {
    @Test
    void projectsEveryRetainedRecoveryRowAndAllSixMinusTwentyMaps() {
        assertEquals(29, ClassicWorldMapRecoveryCatalog.rules().size());
        assertEquals(Set.of(63, 65, 420, 558, 604, 779),
                ClassicWorldMapRecoveryCatalog.rules().stream()
                        .filter(rule -> rule.hp() == -20)
                        .map(ClassicWorldMapRecoveryCatalog.Rule::mapId)
                        .collect(Collectors.toSet()));
        assertEquals(18, ClassicWorldMapRecoveryCatalog.rules().stream()
                .filter(rule -> rule.mapId() >= 16_384)
                .count());
    }

    @Test
    void zoneWholeMapFallbackAndDefaultIntervalsAreExact() {
        ClassicWorldMapRecoveryCatalog.Rule forest =
                ClassicWorldMapRecoveryCatalog.ruleFor(4, 33_045, 32_325);
        assertEquals(5, forest.id());
        assertEquals(10, forest.hp());
        assertEquals(3, forest.mp());
        assertEquals(16, forest.intervalSeconds());
        assertNull(ClassicWorldMapRecoveryCatalog.ruleFor(4, 33_044, 32_325));

        ClassicWorldMapRecoveryCatalog.Rule undersea =
                ClassicWorldMapRecoveryCatalog.ruleFor(63, 0, 0);
        assertEquals(-20, undersea.hp());
        assertEquals(64, undersea.intervalSeconds());

        ClassicWorldMapRecoveryCatalog.Rule innerCastle =
                ClassicWorldMapRecoveryCatalog.ruleFor(15, 0, 0);
        assertEquals(-1, innerCastle.mapId());
        assertEquals(15, innerCastle.hp());
        assertEquals(10, innerCastle.mp());
    }

    @Test
    void regeneratedTempleDisablesOnlyItsNaturalMpPath() {
        assertTrue(ClassicWorldMapRecoveryCatalog.disablesNaturalMp(2000, 0, 0));
        assertFalse(ClassicWorldMapRecoveryCatalog.disablesNaturalMp(63, 0, 0));
        assertFalse(ClassicWorldMapRecoveryCatalog.disablesNaturalMp(4, 0, 0));
    }
}
