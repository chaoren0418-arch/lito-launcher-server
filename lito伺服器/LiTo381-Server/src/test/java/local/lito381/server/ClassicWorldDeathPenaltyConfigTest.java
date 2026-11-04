package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ClassicWorldDeathPenaltyConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test
    void productionUsesNativeRandomnessAndAggregateSelftestSuppressesChance()
            throws Exception {
        ServerConfig production = ServerConfig.load(Path.of("config/server.properties"));
        ServerConfig selftest = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));

        assertEquals(0, production.classicWorldDeathItemChanceFixtureRoll());
        assertEquals(0, production.classicWorldDeathItemCountFixtureRoll());
        assertEquals(-1, production.classicWorldDeathItemSelectorFixtureIndex());
        assertEquals(0, production.classicWorldDeathSkillChanceFixtureRoll());
        assertEquals(0, production.classicWorldDeathSkillCountFixtureRoll());
        assertEquals(-1, production.classicWorldDeathSkillSelectorFixtureIndex());
        assertEquals(100, selftest.classicWorldDeathItemChanceFixtureRoll());
        assertEquals(100, selftest.classicWorldDeathSkillChanceFixtureRoll());
    }
}
