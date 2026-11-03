package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldTrebuchetCatalogTest {
    @Test
    void preservesSourceMessageAndProvidesLosslessCp950ClientText() {
        assertEquals("炮弹", ClassicWorldTrebuchetCatalog.SOURCE_NORMAL_SHELL_NAME);
        assertEquals("砲彈", ClassicWorldTrebuchetCatalog.CLIENT_NORMAL_SHELL_NAME);
        assertEquals("炮弹不足投石机无法使用.",
                ClassicWorldTrebuchetCatalog.SOURCE_NO_AMMO_MESSAGE);
        assertEquals("砲彈不足投石機無法使用.",
                ClassicWorldTrebuchetCatalog.CLIENT_NO_AMMO_MESSAGE);
        assertDoesNotThrow(() -> java.nio.charset.Charset.forName("windows-950")
                .newEncoder().encode(java.nio.CharBuffer.wrap(
                        ClassicWorldTrebuchetCatalog.CLIENT_NO_AMMO_MESSAGE)));
    }

    @Test
    void registersExactNormalShellTemplateWithoutFabricatingSilenceShell() {
        InventoryCatalog.Definition shell = InventoryCatalog.find(46_281);
        assertEquals(46_281, shell.catalogId());
        assertEquals(0, shell.useType());
        assertEquals(592, shell.icon());
        assertEquals(3_965, shell.groundGfx());
        assertEquals(0, shell.blessing());
        assertEquals("砲彈", shell.name());
        assertEquals("砲彈", shell.detail());
        assertEquals(0, shell.buyPrice());
        assertEquals(0, shell.sellPrice());
        assertTrue(shell.stackable());
        assertTrue(shell.tradable());
        assertEquals(BattleActionCodes.WALK, shell.weaponWalkAction());
        assertEquals(1_000, shell.unitWeight());
        assertNull(InventoryCatalog.find(46_282));
    }

    @Test
    void retainsBothDanglingNativeHtmlIds() {
        assertEquals("corca", ClassicWorldTrebuchetCatalog.html(20_000));
        assertEquals("corcd", ClassicWorldTrebuchetCatalog.html(20_001));
        assertEquals("", ClassicWorldTrebuchetCatalog.html(20_002));
    }

    @Test
    void resolvesEveryRetainedActionAtExactBounds() {
        ClassicWorldTrebuchetCatalog.Shot outer =
                ClassicWorldTrebuchetCatalog.resolve(20_000, "3", bound -> 0);
        assertEquals(33_629, outer.x());
        assertEquals(32_730, outer.y());
        assertEquals(12_205, outer.effectGfx());
        assertTrue(outer.damaging());
        assertEquals(46_281, outer.consumedItemId());

        ClassicWorldTrebuchetCatalog.Shot tower =
                ClassicWorldTrebuchetCatalog.resolve(
                        20_000, "4", bound -> bound - 1);
        assertEquals(33_634, tower.x());
        assertEquals(32_680, tower.y());

        ClassicWorldTrebuchetCatalog.Shot silenceOuter =
                ClassicWorldTrebuchetCatalog.resolve(
                        20_000, "1-14", bound -> bound - 1);
        assertEquals(33_634, silenceOuter.x());
        assertEquals(32_733, silenceOuter.y());
        assertFalse(silenceOuter.damaging());
        assertEquals(46_282, silenceOuter.consumedItemId());

        ClassicWorldTrebuchetCatalog.Shot silenceTower =
                ClassicWorldTrebuchetCatalog.resolve(
                        20_000, "1-15", bound -> 0);
        assertEquals(32_675, silenceTower.y());
        assertFalse(silenceTower.damaging());

        ClassicWorldTrebuchetCatalog.Shot defence =
                ClassicWorldTrebuchetCatalog.resolve(
                        20_001, "9", bound -> bound - 1);
        assertEquals(33_634, defence.x());
        assertEquals(32_738, defence.y());
        assertEquals(12_193, defence.effectGfx());
        assertTrue(defence.damaging());
    }

    @Test
    void rejectsUnretainedActionsAndInvalidRandomOutput() {
        assertNull(ClassicWorldTrebuchetCatalog.resolve(
                20_000, "9", bound -> 0));
        assertNull(ClassicWorldTrebuchetCatalog.resolve(
                20_001, "3", bound -> 0));
        assertNull(ClassicWorldTrebuchetCatalog.resolve(
                99, "3", bound -> 0));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldTrebuchetCatalog.resolve(
                        20_000, "3", bound -> bound));
    }

    @Test
    void appliesTheRetainedInclusiveFourTileImpactSquare() {
        ClassicWorldTrebuchetCatalog.Shot shot =
                ClassicWorldTrebuchetCatalog.resolve(20_000, "3", bound -> 0);
        assertTrue(shot.impacts(shot.x() - 4, shot.y() + 4));
        assertTrue(shot.impacts(shot.x() + 4, shot.y() - 4));
        assertFalse(shot.impacts(shot.x() - 5, shot.y()));
        assertFalse(shot.impacts(shot.x(), shot.y() + 5));
    }
}
