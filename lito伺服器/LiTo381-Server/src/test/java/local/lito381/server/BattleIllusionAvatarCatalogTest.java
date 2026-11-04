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

final class BattleIllusionAvatarCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(27).c(2).bytes();
        assertArrayEquals(new byte[] {0x14, 0x1B, 0x02}, request);
        assertEquals(219,
                Protocol.selectorOnlySpellRequest(request).skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(
                        new Protocol.Writer().c(Protocol.C_USE_SPELL)
                                .c(27).c(2).d(17).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedAbilityContract() {
        BattleSpellCatalog.IllusionAvatarSpell spell =
                BattleSpellCatalog.illusionAvatarSpell(
                        BattleSpellCatalog.ILLUSION_AVATAR);
        assertEquals(219, spell.skillId());
        assertEquals(27, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_575, spell.effectGfx());
        assertEquals(35, spell.mpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(4, spell.nativeSkillId());
        assertEquals(10, spell.meleeDamage());
        assertEquals(10, spell.rangedDamage());
        assertEquals(8, spell.spellPower());
        assertEquals(1.5D, spell.incomingDamageRate());
        assertEquals(49_135L, spell.skillBookItemId());
        assertEquals("$5699", spell.skillBookNameId());
        assertEquals("$5953", spell.skillRowNameId());
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.persisted());
        assertEquals("illusion-avatar", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.illusionAvatarSpell(218));
    }

    @Test void appliesOnlyRetainedOriginalIntMpReduction() {
        BattleSpellCatalog.IllusionAvatarSpell spell =
                BattleSpellCatalog.illusionAvatarSpell(
                        BattleSpellCatalog.ILLUSION_AVATAR);
        assertEquals(35, spell.effectiveMpCost(0));
        assertEquals(34, spell.effectiveMpCost(1));
        assertEquals(33, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(-1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(3));
    }

    @Test void fixtureIsIsolatedFromFormalAndPriorProfiles() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .illusionAvatarSpellFixtureEnabled());
        assertFalse(ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.joy-of-pain-selftest.properties"))
                .illusionAvatarSpellFixtureEnabled());
        ServerConfig validation = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.illusion-avatar-selftest.properties"));
        assertTrue(validation.illusionAvatarSpellFixtureEnabled());
        assertFalse(validation.joyOfPainSpellFixtureEnabled());
    }
}
