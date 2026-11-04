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

final class BattleCubeIgnitionCatalogTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void selectedRequestIsExactSelectorOnly141904() {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(4).bytes();
        assertArrayEquals(new byte[] {0x14, 0x19, 0x04}, request);
        assertEquals(BattleSpellCatalog.CUBE_IGNITION,
                Protocol.selectorOnlySpellRequest(request).skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(25).c(4).d(1).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedEffectContract() {
        BattleSpellCatalog.CubeIgnitionSpell spell =
                BattleSpellCatalog.cubeIgnitionSpell(
                        BattleSpellCatalog.CUBE_IGNITION);
        assertEquals(205, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_706, spell.cubeGfx());
        assertEquals(6_708, spell.allyEffectGfx());
        assertEquals(6_709, spell.enemyEffectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(20, spell.durationSeconds());
        assertEquals(3, spell.effectRadius());
        assertEquals(32, spell.targetTo());
        assertEquals(2, spell.type());
        assertEquals(16, spell.nativeSkillId());
        assertEquals(10, spell.damageValue());
        assertEquals(49_156L, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(80_149, spell.effectNpcId());
        assertEquals(49_121L, spell.skillBookItemId());
        assertEquals("$5685", spell.skillBookNameId());
        assertEquals("$5939", spell.skillRowNameId());
        assertEquals("cube-ignition", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertNull(BattleSpellCatalog.cubeIgnitionSpell(204));
    }

    @Test void appliesOnlyRetainedOriginalIntMpReduction() {
        BattleSpellCatalog.CubeIgnitionSpell spell =
                BattleSpellCatalog.cubeIgnitionSpell(205);
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(29, spell.effectiveMpCost(1));
        assertEquals(28, spell.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(-1));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(3));
    }

    @Test void exactAttributeStoneDefinitionIsUsableAndStackable() {
        InventoryCatalog.Definition stone = InventoryCatalog.require(
                InventoryCatalog.ATTRIBUTE_STONE);
        assertEquals(49_156L, stone.catalogId());
        assertEquals(0, stone.useType());
        assertEquals(3_279, stone.icon());
        assertEquals(7_090, stone.groundGfx());
        assertEquals(0, stone.blessing());
        assertEquals("屬性石", stone.name());
        assertEquals("$6346", stone.detail());
        assertEquals(1_000, stone.unitWeight());
        assertTrue(stone.identified());
        assertTrue(stone.stackable());
        assertTrue(stone.tradable());
    }

    @Test void tcpFixtureIsOptInAndProductionDefaultRemainsOff() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .cubeIgnitionSpellFixtureEnabled());
        assertTrue(ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.cube-ignition-selftest.properties"))
                .cubeIgnitionSpellFixtureEnabled());
    }
}
