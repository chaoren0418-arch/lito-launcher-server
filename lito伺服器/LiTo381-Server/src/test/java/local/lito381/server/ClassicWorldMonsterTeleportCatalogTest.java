package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterTeleportCatalogTest {
    @Test
    void selectedCatalogCoversEveryImportedMonsterTemplate() {
        Set<Integer> npcIds = new HashSet<>();
        for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                int npcId = definition.template().npcId();
                assertNotNull(ClassicWorldMonsterTeleportCatalog.forNpc(npcId),
                        "missing teleport NPC " + npcId);
                npcIds.add(npcId);
            }
        }
        assertEquals(422, npcIds.size());
        assertEquals(430, ClassicWorldMonsterTeleportCatalog.size());
        assertEquals(3, ClassicWorldMonsterTeleportCatalog.enabledCount());
    }

    @Test
    void onlySelectedSuccubusFamiliesHaveTeleportCapability() {
        assertTrue(Boolean.TRUE.equals(
                ClassicWorldMonsterTeleportCatalog.forNpc(45393)));
        assertTrue(Boolean.TRUE.equals(
                ClassicWorldMonsterTeleportCatalog.forNpc(45400)));
        assertTrue(Boolean.TRUE.equals(
                ClassicWorldMonsterTeleportCatalog.forNpc(45451)));
        assertFalse(Boolean.TRUE.equals(
                ClassicWorldMonsterTeleportCatalog.forNpc(45278)));
    }
}
