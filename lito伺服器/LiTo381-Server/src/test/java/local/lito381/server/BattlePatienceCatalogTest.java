package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattlePatienceCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRow211MatchesDatabaseSourceAndLiToContract() {
        BattleSpellCatalog.PatienceSpell spell =
                BattleSpellCatalog.patienceSpell(BattleSpellCatalog.PATIENCE);
        assertNotNull(spell);
        assertEquals(211, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(2, spell.column());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(19, spell.actionId());
        assertEquals(6_507, spell.effectGfx());
        assertEquals(25, spell.mpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(16, spell.clientCastClockMillis());
        assertEquals(600, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(4, spell.nativeSkillId());
        assertEquals(2, spell.physicalDamageReduction());
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.persisted());
        assertEquals(49_127L, spell.skillBookItemId());
        assertEquals("$5691", spell.skillBookNameId());
        assertEquals("$5945", spell.skillRowNameId());
        assertEquals(23, spell.effectiveMpCost(2));
    }

    @Test void exactSelectorOnlyRequestIsFourteenOneATwo() {
        assertEquals(211, Protocol.selectorOnlySpellRequest(
                new byte[] {0x14, 0x1A, 0x02}).skillId());
    }

    @Test void fixtureIsOptInAndConfinedToPatienceSelftest() throws Exception {
        assertFalse(ServerConfig.load(java.nio.file.Path.of(
                "config/server.properties")).patienceSpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(java.nio.file.Path.of(
                "src/test/resources/server-config/config/server.patience-selftest.properties"));
        assertTrue(validation.patienceSpellFixtureEnabled());
        assertFalse(validation.cubeQuakeSpellFixtureEnabled());
        assertFalse(validation.spellMaterialFixtureEnabled());
    }
}
