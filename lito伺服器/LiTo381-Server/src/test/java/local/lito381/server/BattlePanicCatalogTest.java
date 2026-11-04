package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattlePanicCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRow217MatchesDatabaseAndClientContract() {
        BattleSpellCatalog.PanicSpell spell =
                BattleSpellCatalog.panicSpell(BattleSpellCatalog.PANIC);
        assertEquals(217, spell.skillId());
        assertEquals(27, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_506, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(30, spell.hpCost());
        assertEquals(4_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(4, spell.type());
        assertEquals(1, spell.nativeSkillId());
        assertEquals(-1, spell.abilityScoreModifier());
        assertEquals(49_133L, spell.skillBookItemId());
        assertEquals("$5697", spell.skillBookNameId());
        assertEquals("$5951", spell.skillRowNameId());
        assertFalse(spell.counterMagicBypass());
        assertFalse(spell.persisted());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.panicSpell(216));
    }

    @Test void exactSelectorProbabilityAndMpReductionArePinned() {
        byte[] request = new byte[] {0x14, 0x1B, 0x00};
        assertArrayEquals(request, new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(27).c(0).bytes());
        assertEquals(217, Protocol.selectorOnlySpellRequest(request).skillId());
        BattleSpellCatalog.PanicSpell spell =
                BattleSpellCatalog.panicSpell(BattleSpellCatalog.PANIC);
        assertEquals(55, spell.probability(60, 50, 0, 0, 0, false));
        assertEquals(50, spell.probability(50, 50, 20, 0, 0, false));
        assertEquals(35, spell.probability(40, 50, 0, 0, 0, false));
        assertEquals(90, spell.probability(99, 1, 200, 127, 0, false));
        assertEquals(1, spell.probability(1, 99, 0, -127, 255, false));
        assertEquals(0, spell.probability(99, 1, 200, 127, 0, true));
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(28, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.probability(0, 1, 0, 0, 0, false));
    }

    @Test void fixtureIsDisabledInOrdinaryProfile() throws Exception {
        assertFalse(ServerConfig.load(Path.of(
                "config/server.properties")).panicSpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.panic-selftest.properties"));
        assertTrue(validation.panicSpellFixtureEnabled());
        assertFalse(validation.insightSpellFixtureEnabled());
        assertFalse(validation.phantasmSpellFixtureEnabled());
    }
}
