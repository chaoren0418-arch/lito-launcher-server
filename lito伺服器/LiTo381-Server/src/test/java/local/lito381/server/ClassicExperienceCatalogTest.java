package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicExperienceCatalogTest {
    @Test void selectedSourceResourceAndRetainedIndexingAreHashLocked() throws Exception {
        assertEquals(101, ClassicExperienceCatalog.rows().size());
        assertEquals(99, ClassicExperienceCatalog.MAXIMUM_LEVEL);
        assertEquals(0, ClassicExperienceCatalog.minimumExperienceForLevel(1));
        assertEquals(234_276, ClassicExperienceCatalog.minimumExperienceForLevel(22));
        assertEquals(279_861, ClassicExperienceCatalog.minimumExperienceForLevel(23));
        assertEquals(1.0, ClassicExperienceCatalog.penaltyRate(22));
        assertEquals(0.5, ClassicExperienceCatalog.penaltyRate(50));
        assertEquals(0.0, ClassicExperienceCatalog.deathPenaltyRate(10));
        assertEquals(0.1, ClassicExperienceCatalog.deathPenaltyRate(11));
        assertEquals(0.09, ClassicExperienceCatalog.deathPenaltyRate(45));
        assertEquals(0.05, ClassicExperienceCatalog.deathPenaltyRate(49));
        assertEquals(45_585,
                ClassicExperienceCatalog.experienceNeededForNextLevel(22));
        assertEquals(4_558, ClassicExperienceCatalog.deathPenalty(22));
        assertEquals(65_642, ClassicExperienceCatalog.deathPenalty(45));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/classic-experience.lex.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicExperienceCatalog.RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test void retainedLevelAndPenaltyRulesUseSelectedExpRows() {
        assertEquals(1, ClassicExperienceCatalog.levelForExperience(0));
        assertEquals(1, ClassicExperienceCatalog.levelForExperience(124));
        assertEquals(2, ClassicExperienceCatalog.levelForExperience(125));
        assertEquals(22, ClassicExperienceCatalog.levelForExperience(234_276));
        assertEquals(23, ClassicExperienceCatalog.levelForExperience(279_861));
        assertTrue(ClassicExperienceCatalog.isConsistent(22, 234_276));
        assertFalse(ClassicExperienceCatalog.isConsistent(22, 279_861));
        assertEquals(50, ClassicExperienceCatalog.adjustedReward(50, 101));
        assertEquals(75, ClassicExperienceCatalog.adjustedReward(50, 101, 1.5));
        assertEquals(1, ClassicExperienceCatalog.adjustedReward(52, 1, 3.0));
        assertEquals(0, ClassicExperienceCatalog.adjustedReward(99, 10_000));
        assertEquals(0, ClassicExperienceCatalog.deathPenalty(10));
    }
}
