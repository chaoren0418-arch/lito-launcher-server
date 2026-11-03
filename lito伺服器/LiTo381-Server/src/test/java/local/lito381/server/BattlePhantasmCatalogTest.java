package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattlePhantasmCatalogTest {
    @Test void selectedRow212MatchesDatabaseAndClientContract() {
        BattleSpellCatalog.PhantasmSpell spell =
                BattleSpellCatalog.phantasmSpell(BattleSpellCatalog.PHANTASM);
        assertNotNull(spell);
        assertEquals(212, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(3, spell.column());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(19, spell.actionId());
        assertEquals(6_530, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(25, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(640, spell.clientCastClockMillis());
        assertEquals(5, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(4, spell.type());
        assertEquals(8, spell.nativeSkillId());
        assertTrue(spell.checkMagicHit());
        assertTrue(spell.checkStoneResistance());
        assertFalse(spell.persisted());
        assertEquals(49_128L, spell.skillBookItemId());
        assertEquals("$5692", spell.skillBookNameId());
        assertEquals("$5946", spell.skillRowNameId());
        assertEquals(28, spell.effectiveMpCost(2));
    }

    @Test void exactSelectorAndSelectedProbabilityBoundsAreAuthoritative() {
        assertEquals(212, Protocol.selectorOnlySpellRequest(
                new byte[] {0x14, 0x1A, 0x03}).skillId());
        BattleSpellCatalog.PhantasmSpell spell =
                BattleSpellCatalog.phantasmSpell(BattleSpellCatalog.PHANTASM);
        assertEquals(35, spell.probability(50, 50, 0, 0));
        assertEquals(45, spell.probability(1, 99, 20, 10));
        assertEquals(90, spell.probability(99, 1, 200, -128));
        assertEquals(1, spell.probability(99, 1, -200, 127));
        assertThrows(IllegalArgumentException.class,
                () -> spell.probability(0, 1, 0, 0));
    }

    @Test void fixtureIsOptInAndConfinedToPhantasmSelftest() throws Exception {
        assertFalse(ServerConfig.load(java.nio.file.Path.of(
                "config/server.properties")).phantasmSpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(java.nio.file.Path.of(
                "src/test/resources/server-config/config/server.phantasm-selftest.properties"));
        assertTrue(validation.phantasmSpellFixtureEnabled());
        assertEquals(1, validation.phantasmFixturePercentileRoll());
        assertFalse(validation.patienceSpellFixtureEnabled());
        assertFalse(validation.spellMaterialFixtureEnabled());
        ServerConfig dragonEyeBoundary = ServerConfig.load(java.nio.file.Path.of(
                "src/test/resources/server-config/config/server.dragon-eye-phantasm-selftest.properties"));
        assertTrue(dragonEyeBoundary.phantasmSpellFixtureEnabled());
        assertEquals(33, dragonEyeBoundary.phantasmFixturePercentileRoll());
    }
}
