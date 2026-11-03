package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Prevents historical rental-town boundaries from masking current production. */
final class NativeLitoTownFlowConsistencyTest {
    @Test
    void currentCoverageAndProductionAgreeThatTheDefaultOffSceneIsExecutable()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String coverage = read(Path.of("tools/protocol_coverage.py"));

        assertTrue(session.contains("private void startRentalTownScene()"));
        assertTrue(session.contains("sendEncrypted(bootstrap.world(),"));
        assertTrue(session.contains("sendEncrypted(bootstrap.start(),"));
        assertTrue(session.contains("sendEncrypted(Protocol.worldTime(gameSeconds),"));
        assertTrue(session.contains("sendEncrypted(Protocol.inventoryBatch(items),"));
        assertTrue(session.contains("Protocol.litoRentalLightGem("));
        assertTrue(session.contains("sendPointStats();"));
        assertTrue(session.contains("private void removeRentalTownServiceItems("));
        assertTrue(session.contains("sendRentalTownActor(\"scene-bootstrap\")"));
        assertTrue(coverage.contains(
                "S_CE/S_CD → S_WORLD/S_D2/S_TIME/S_7C/S_DB → local-player S_PUT_OBJECT"));
        assertTrue(coverage.contains("Layer3移動、Layer7傳送及退出回目錄"));
        assertFalse(coverage.contains("所以不啟動場景"));
    }

    @Test
    void identityCatalogDoesNotClaimThatImplementedLifecycleIsStillGated()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/RentalItemSetCatalog.java"));
        assertTrue(catalog.contains("{@link ClientSession}"));
        assertTrue(catalog.contains(
                "orchestrates the session-local inventory after"));
        assertTrue(catalog.contains(
                "identification/auto-equip policy remain explicit server policy"));
        assertFalse(catalog.contains(
                "spawn and lifecycle ordering remain evidence-gated"));
    }

    @Test
    void historicalPolicyBoundaryRemainsExplicitInsteadOfBeingPromotedToFact() {
        for (int mapId : new int[] {500, 560}) {
            NativeLitoTownScenePlan.Scene scene =
                    NativeLitoTownScenePlan.require(mapId);
            assertFalse(scene.historicalSpawnExact());
            assertTrue(scene.spawnAuthority().contains("server-selected"));
            assertTrue(scene.loadoutPolicy()
                    == RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS);
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
