package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LitoExternalMapTest {
    @Test
    void parsesAnExternal380MapIntoRuntimeCollisionAndFlags(@TempDir Path temporary)
            throws Exception {
        Path file = temporary.resolve("18.txt");
        Files.writeString(file, "0,0,0\n3,15,0\n0,0,0\n",
                java.nio.charset.StandardCharsets.UTF_8);
        LitoMapRepository.Definition definition = new LitoMapRepository.Definition(
                18, 18, 32724, 32726, 32785, 32787,
                false, true, true, true, true, file);

        ClassicWorldMapCatalog.ClassicMap map = LitoMapRepository.loadTextMap(definition);

        assertEquals(18, map.mapId());
        assertEquals(3, map.width());
        assertEquals(3, map.height());
        assertEquals(64, map.sourceSha256().length());
        assertEquals(3, map.rawCell(32724, 32786));
        assertEquals(15, map.rawCell(32725, 32786));
        assertTrue(map.standingPassable(32725, 32786));
        assertTrue(map.canStep(32724, 32786, 32725, 32786));
        assertFalse(map.allowsBookmark());
        assertTrue(map.allowsRandomTeleport());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsDeathPenalty());
        assertTrue(map.allowsPetRecall());
    }

    @Test
    void derivesEffectiveBoundsFromFileButRejectsInconsistentRows(@TempDir Path temporary)
            throws IOException {
        Path file = temporary.resolve("9999.txt");
        Files.writeString(file, "1,2\n3,4,5\n", java.nio.charset.StandardCharsets.UTF_8);
        LitoMapRepository.Definition definition = new LitoMapRepository.Definition(
                9999, 9999, 0, 2, 0, 1,
                false, false, false, false, false, file);

        assertThrows(IOException.class, () -> LitoMapRepository.loadTextMap(definition));
    }
}
