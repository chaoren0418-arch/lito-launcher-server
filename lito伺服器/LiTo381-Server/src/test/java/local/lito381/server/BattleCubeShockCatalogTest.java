package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattleCubeShockCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRow215MatchesDatabaseAndLiToSelectorContract() {
        BattleSpellCatalog.CubeShockSpell spell =
                BattleSpellCatalog.cubeShockSpell(BattleSpellCatalog.CUBE_SHOCK);
        assertNotNull(spell);
        assertEquals(215, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(6, spell.column());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(19, spell.actionId());
        assertEquals(55, spell.mpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(20, spell.durationSeconds());
        assertEquals(3, spell.effectRadius());
        assertEquals(32, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(64, spell.nativeSkillId());
        assertEquals(30, spell.resistanceValue());
        assertEquals(-10, spell.enemyMrModifier());
        assertEquals(49_156L, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(80_151, spell.effectNpcId());
        assertEquals(6_718, spell.cubeGfx());
        assertEquals(6_720, spell.allyEffectGfx());
        assertEquals(6_721, spell.enemyEffectGfx());
        assertEquals(49_131L, spell.skillBookItemId());
        assertEquals("$5695", spell.skillBookNameId());
        assertEquals("$5949", spell.skillRowNameId());
        assertEquals(53, spell.effectiveMpCost(2));
    }

    @Test void exactSelectorOnlyRequestIsFourteenOneASix() {
        assertEquals(215, (26 << 3) + 6 + 1);
        assertEquals(215, Protocol.selectorOnlySpellRequest(
                new byte[] {0x14, 0x1A, 0x06}).skillId());
    }

    @Test void fixtureIsOptInAndConfinedToItsSelftestConfiguration()
            throws Exception {
        assertEquals(false, ServerConfig.load(Path.of(
                "config/server.properties")).cubeShockSpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.cube-shock-selftest.properties"));
        assertEquals(true, validation.cubeShockSpellFixtureEnabled());
        assertEquals(false, validation.cubeQuakeSpellFixtureEnabled());
    }
}
