package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattleCubeHarmonyCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedPrimaryRowAndRequestShapeAreExact() {
        BattleSpellCatalog.CubeHarmonySpell spell =
                BattleSpellCatalog.cubeHarmonySpell(
                        BattleSpellCatalog.CUBE_HARMONY);
        assertEquals(220, spell.skillId());
        assertEquals(27, spell.row());
        assertEquals(3, spell.column());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(19, spell.actionId());
        assertEquals(6_724, spell.cubeGfx());
        assertEquals(6_727, spell.effectGfx());
        assertEquals(55, spell.mpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(32, spell.clientCastClockMillis());
        assertEquals(20, spell.durationSeconds());
        assertEquals(3, spell.effectRadius());
        assertEquals(32, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(8, spell.nativeSkillId());
        assertEquals(5, spell.mpRecovery());
        assertEquals(25, spell.hpDamage());
        assertEquals(49_156L, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(80_152, spell.effectNpcId());
        assertEquals(49_136L, spell.skillBookItemId());
        assertEquals("$5700", spell.skillBookNameId());
        assertEquals("$5954", spell.skillRowNameId());
        assertEquals(55, spell.effectiveMpCost(0));
        assertEquals(53, spell.effectiveMpCost(2));
    }

    @Test void fixtureIsIsolatedFromOrdinaryProfile() throws Exception {
        assertEquals(false, ServerConfig.load(Path.of(
                "config/server.properties")).cubeHarmonySpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.cube-harmony-selftest.properties"));
        assertEquals(true, validation.cubeHarmonySpellFixtureEnabled());
        assertEquals(false, validation.cubeShockSpellFixtureEnabled());
    }
}
