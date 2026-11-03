package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class GameHubClassicWorldNpcStateTest {
    @Test
    void roiGfxTransitionIsAtomicSharedAndKeepsCanonicalStaticRowImmutable()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        ClassicWorldNpcCatalog.StaticNpc roi = ClassicWorldNpcCatalog.npc(
                ClassicWorldDemonTempleCatalog.MAP_ID,
                ClassicWorldNpcCatalog.objectId(ClassicWorldDemonTempleCatalog.ROI_SPAWN_ID));
        assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX, roi.gfxId());

        try (GameHub hub = new GameHub(config)) {
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    hub.classicWorldNpcGfx(roi));
            assertTrue(hub.transformClassicWorldNpcGfx(
                    roi, ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX,
                    hub.classicWorldNpcGfx(roi));
            assertFalse(hub.transformClassicWorldNpcGfx(
                    roi, ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
            assertTrue(hub.restoreClassicWorldNpcGfx(
                    roi, ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    hub.classicWorldNpcGfx(roi));
            assertFalse(hub.restoreClassicWorldNpcGfx(
                    roi, ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX, roi.gfxId());
        }

        try (GameHub freshHub = new GameHub(config)) {
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    freshHub.classicWorldNpcGfx(roi));
        }
    }
}
