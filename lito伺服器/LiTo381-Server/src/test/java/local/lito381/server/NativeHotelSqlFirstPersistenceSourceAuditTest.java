package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NativeHotelSqlFirstPersistenceSourceAuditTest {
    @Test
    void hotelKeysAndAdenaCommitBeforeSidecarAndClientPublication()
            throws Exception {
        String state = Files.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "GameStateStore.java"), StandardCharsets.UTF_8);
        String session = Files.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "ClientSession.java"), StandardCharsets.UTF_8);
        String repository = Files.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "Accounts.java"), StandardCharsets.UTF_8);

        int sql = state.indexOf("sqlPersistence.persist(lease);");
        int sidecar = state.indexOf("save();", sql);
        assertTrue(sql >= 0 && sidecar > sql);
        assertTrue(state.contains(
                "hotel rental sidecar mirror failed after SQL commit"));
        assertTrue(state.contains(
                "hotel return sidecar mirror failed after SQL commit"));

        assertTrue(session.contains("persistLitoHotelInventory("));
        assertTrue(session.contains("RepositoryHotelChange.rent("));
        assertTrue(session.contains("RepositoryHotelChange.returning("));
        assertTrue(session.contains("hotelChange != null ? \"hotel\""));
        assertTrue(session.contains("+ \" inventory SQL commit refused: \""));
        assertTrue(session.contains("ClassicWorldHotelCatalog.LEASE_SECONDS"));
        assertTrue(session.contains("keyExpiryMillis"));
        assertTrue(repository.contains(
                "character_items WRITE, character_items_time WRITE"));
        assertTrue(repository.contains(
                "lito_item_origin WRITE, lito_private_inventory WRITE"));
        assertTrue(repository.contains("lito_hotel_lease_key WRITE"));
    }
}
