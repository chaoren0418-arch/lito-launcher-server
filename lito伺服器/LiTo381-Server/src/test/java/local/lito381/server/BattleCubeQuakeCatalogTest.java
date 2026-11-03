package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

final class BattleCubeQuakeCatalogTest {
    @Test void selectedRow210MatchesDatabaseAndLiToSelectorContract() {
        BattleSpellCatalog.CubeQuakeSpell spell =
                BattleSpellCatalog.cubeQuakeSpell(BattleSpellCatalog.CUBE_QUAKE);
        assertNotNull(spell);
        assertEquals(210, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(1, spell.column());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(19, spell.actionId());
        assertEquals(35, spell.mpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(20, spell.durationSeconds());
        assertEquals(3, spell.effectRadius());
        assertEquals(32, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(2, spell.nativeSkillId());
        assertEquals(30, spell.resistanceValue());
        assertEquals(49_156L, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(80_150, spell.effectNpcId());
        assertEquals(6_712, spell.cubeGfx());
        assertEquals(6_714, spell.allyEffectGfx());
        assertEquals(6_715, spell.enemyEffectGfx());
        assertEquals(49_126L, spell.skillBookItemId());
        assertEquals("$5690", spell.skillBookNameId());
        assertEquals("$5944", spell.skillRowNameId());
        assertEquals(33, spell.effectiveMpCost(2));
    }

    @Test void exactSelectorOnlyRequestIsFourteenOneAOne() {
        assertEquals(210, (26 << 3) + 1 + 1);
        assertEquals(210, Protocol.selectorOnlySpellRequest(
                new byte[] {0x14, 0x1A, 0x01}).skillId());
    }

    @Test void fixtureIsOptInAndConfinedToItsSelftestConfiguration() throws Exception {
        assertEquals(false, ServerConfig.load(java.nio.file.Path.of(
                "config/server.properties")).cubeQuakeSpellFixtureEnabled());
        assertEquals(true, ServerConfig.load(java.nio.file.Path.of(
                "src/test/resources/server-config/config/server.cube-quake-selftest.properties"))
                .cubeQuakeSpellFixtureEnabled());
        assertEquals(false, ServerConfig.load(java.nio.file.Path.of(
                "src/test/resources/server-config/config/server.cube-quake-selftest.properties"))
                .cubeIgnitionSpellFixtureEnabled());
    }
}
