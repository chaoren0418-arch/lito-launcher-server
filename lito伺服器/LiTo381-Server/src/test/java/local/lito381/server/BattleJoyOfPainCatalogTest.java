package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattleJoyOfPainCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRow218MatchesDatabaseAndClientContract() {
        BattleSpellCatalog.JoyOfPainSpell spell =
                BattleSpellCatalog.joyOfPainSpell(BattleSpellCatalog.JOY_OF_PAIN);
        assertEquals(218, spell.skillId());
        assertEquals(27, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_528, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(2, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(2, spell.nativeSkillId());
        assertEquals(49_134L, spell.skillBookItemId());
        assertEquals("$5698", spell.skillBookNameId());
        assertEquals("$5952", spell.skillRowNameId());
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.persisted());
        assertEquals(40, spell.effectiveMpCost(0));
        assertEquals(38, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.joyOfPainSpell(217));
    }

    @Test void nativeSelectorOnlyRequestIsExact() {
        byte[] request = new byte[] {0x14, 0x1B, 0x01};
        assertArrayEquals(request, new Protocol.Writer()
                .c(Protocol.C_USE_SPELL).c(27).c(1).bytes());
        assertEquals(218,
                Protocol.selectorOnlySpellRequest(request).skillId());
    }

    @Test void fixtureIsIsolatedFromOrdinaryAndPriorProfiles() throws Exception {
        assertFalse(ServerConfig.load(Path.of(
                "config/server.properties")).joyOfPainSpellFixtureEnabled());
        assertFalse(ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.panic-selftest.properties"))
                .joyOfPainSpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.joy-of-pain-selftest.properties"));
        assertTrue(validation.joyOfPainSpellFixtureEnabled());
        assertFalse(validation.panicSpellFixtureEnabled());
    }
}
