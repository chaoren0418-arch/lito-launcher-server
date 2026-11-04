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

final class BattleInsightCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRequestAndAbilityReceiverAreExact() {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(26).c(7).bytes();
        assertArrayEquals(new byte[] {0x14, 0x1A, 0x07}, request);
        assertEquals(BattleSpellCatalog.INSIGHT,
                Protocol.selectorOnlySpellRequest(request).skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(26).c(7).d(17).bytes()));

        assertArrayEquals(new byte[] {
                0x50, 13, 13, 13, 13, 13, 12, 0
        }, Protocol.abilityScores(13, 13, 13, 12, 13, 13, 0));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedContract() {
        BattleSpellCatalog.InsightSpell spell =
                BattleSpellCatalog.insightSpell(BattleSpellCatalog.INSIGHT);
        assertEquals(216, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_505, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(3_600, spell.clientCastClockMillis());
        assertEquals(600, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(128, spell.nativeSkillId());
        assertEquals(1, spell.abilityScoreModifier());
        assertEquals(49_132L, spell.skillBookItemId());
        assertEquals("$5696", spell.skillBookNameId());
        assertEquals("$5950", spell.skillRowNameId());
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.persisted());
        assertEquals("insight", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.insightSpell(215));
    }

    @Test void appliesOnlyRetainedOriginalIntMpReduction() {
        BattleSpellCatalog.InsightSpell spell =
                BattleSpellCatalog.insightSpell(BattleSpellCatalog.INSIGHT);
        assertEquals(40, spell.effectiveMpCost(0));
        assertEquals(39, spell.effectiveMpCost(1));
        assertEquals(38, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(-1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(3));
    }

    @Test void fixtureIsOptInOnly() throws Exception {
        assertFalse(ServerConfig.load(Path.of(
                "config/server.properties")).insightSpellFixtureEnabled());
    }
}
