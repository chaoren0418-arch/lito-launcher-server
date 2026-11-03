package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LitoMySqlHotelIntegrationTest {
    private static final String ROUTE_ONE = "380 角色庫 1";
    private static final String ROUTE_TWO = "380 角色庫 2";
    private static final String CHARACTER = "LitoHotelGate";
    private static final int INN_NPC_ID = 70012;

    @Test
    void persistsSourceQualifiedHotelLeaseKeysAndReturn(@TempDir Path temporary)
            throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Path base = Path.of("config", "server.properties")
                .toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(base, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        properties.setProperty("database.read.only", "false");
        Path sqlConfig = Path.of("..", "1-伺服器", "config", "sql.properties")
                .toAbsolutePath().normalize();
        properties.setProperty("database.connection.properties", sqlConfig.toString());
        properties.setProperty("maps.directory",
                Path.of("maps").toAbsolutePath().normalize().toString());
        properties.setProperty("state.file", temporary.resolve("state.properties").toString());
        properties.setProperty("capture.directory", temporary.resolve("captures").toString());
        Path writableConfig = temporary.resolve("server.properties");
        store(properties, writableConfig);

        Properties jdbc = new Properties();
        try (Reader reader = Files.newBufferedReader(sqlConfig, StandardCharsets.UTF_8)) {
            jdbc.load(reader);
        }
        String jdbcUrl = jdbc.getProperty("URL1_LOGIN") + "380_ac_1"
                + jdbc.getProperty("URL3_LOGIN");
        String jdbcLogin = jdbc.getProperty("Login_LOGIN");
        String jdbcPassword = jdbc.getProperty("Password_LOGIN");

        try (AccountRepository accounts = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            accounts.healthCheck();
            AccountRepository source = accounts.repositoryForClassicSource(ROUTE_ONE);
            AccountRepository other = accounts.repositoryForClassicSource(ROUTE_TWO);
            RepositoryCharacterMutation stale = source.deleteLitoCharacter("2222", CHARACTER);
            assertTrue(stale.status() == RepositoryCharacterMutationStatus.NOT_OWNED
                    || stale.status() == RepositoryCharacterMutationStatus.SUCCESS);
            RepositoryCharacterMutation created = source.createLitoCharacter(
                    "2222", new Protocol.CustomCharacterRequest(
                            CHARACTER, 0, 1, List.of(18, 13, 15, 9, 12, 8), 0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
            long characterId = created.character().objectId();
            try {
                long adenaId;
                long keyId;
                long cleanupKeyId;
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl, jdbcLogin, jdbcPassword)) {
                    adenaId = allocateObjectId(connection);
                    keyId = allocateObjectId(connection);
                    cleanupKeyId = allocateObjectId(connection);
                }
                LitoOwnedInventoryItem initialAdena = owned(
                        adenaId, (int) InventoryCatalog.ADENA, "Hotel test Adena", 1_000, 0);
                RepositoryPrivateInventorySnapshot initialPrivate =
                        source.litoPrivateInventory(characterId);
                assertEquals(RepositoryPersistenceResult.SUCCESS,
                        source.persistLitoInventory(characterId, List.of(),
                                List.of(initialAdena), List.of(), Set.of(), initialPrivate));

                long expirySeconds = System.currentTimeMillis() / 1_000L
                        + ClassicWorldHotelCatalog.LEASE_SECONDS;
                GameStateStore.HotelLeaseState lease = new GameStateStore.HotelLeaseState(
                        Math.toIntExact(keyId), INN_NPC_ID, 0, false,
                        GameStateStore.sourceCharacter(ROUTE_ONE, CHARACTER),
                        expirySeconds, false);
                LitoOwnedInventoryItem rentAdena = owned(
                        adenaId, (int) InventoryCatalog.ADENA, "Hotel test Adena", 700, 0);
                LitoOwnedInventoryItem key = owned(
                        keyId, (int) ClassicWorldHotelCatalog.KEY_ITEM_ID,
                        "Hotel room key", 1, expirySeconds * 1_000L);
                RepositoryPrivateInventorySnapshot beforeRent =
                        source.litoPrivateInventory(characterId);
                RepositoryHotelChange rent = RepositoryHotelChange.rent(
                        lease, List.of(keyId));
                assertEquals(RepositoryPersistenceResult.SUCCESS,
                        source.persistLitoHotelInventory(characterId, List.of(),
                                List.of(rentAdena, key), List.of(), Set.of(),
                                beforeRent, rent));

                RepositoryHotelSnapshot stored = source.hotelState();
                assertTrue(stored.authoritative());
                assertEquals(1, stored.occupancy(INN_NPC_ID,
                        lease.renterIdentity()).activeRooms());
                assertEquals(lease, stored.activeLeaseForKeys(
                        INN_NPC_ID, Set.of(keyId)));
                assertTrue(other.hotelState().authoritative());
                assertTrue(other.hotelState().keys().stream()
                        .noneMatch(row -> row.itemObjectId() == keyId));
                assertSqlRows(jdbcUrl, jdbcLogin, jdbcPassword, keyId, true);

                // A fresh private-inventory CAS still cannot replay the same
                // lease or key relation after the SQL row became authoritative.
                assertEquals(RepositoryPersistenceResult.CONFLICT,
                        source.persistLitoHotelInventory(characterId, List.of(),
                                List.of(rentAdena, key), List.of(), Set.of(),
                                source.litoPrivateInventory(characterId), rent));

                Properties readOnlyProperties = new Properties();
                readOnlyProperties.putAll(properties);
                readOnlyProperties.setProperty("database.read.only", "true");
                Path readOnlyConfig = temporary.resolve("server-read-only.properties");
                store(readOnlyProperties, readOnlyConfig);
                try (AccountRepository readOnly = AccountRepository.open(
                        ServerConfig.load(readOnlyConfig))) {
                    assertEquals(RepositoryPersistenceResult.READ_ONLY,
                            readOnly.repositoryForClassicSource(ROUTE_ONE)
                                    .persistLitoHotelInventory(characterId, List.of(),
                                            List.of(rentAdena, key), List.of(), Set.of(),
                                            source.litoPrivateInventory(characterId), rent));
                }

                GameStateStore.HotelReturnQuote quote = stored.returnQuote(
                        INN_NPC_ID, lease.renterIdentity(), Set.of(keyId));
                assertTrue(quote.present());
                assertEquals(ClassicWorldHotelCatalog.ACTIVE_LEASE_BASE_REFUND
                                + ClassicWorldHotelCatalog.REFUND_PER_RETURNED_KEY,
                        quote.refund());
                LitoOwnedInventoryItem returnedAdena = owned(
                        adenaId, (int) InventoryCatalog.ADENA, "Hotel test Adena",
                        700 + quote.refund(), 0);
                assertEquals(RepositoryPersistenceResult.SUCCESS,
                        source.persistLitoHotelInventory(characterId, List.of(),
                                List.of(returnedAdena), List.of(), Set.of(),
                                source.litoPrivateInventory(characterId),
                                RepositoryHotelChange.returning(quote)));
                assertTrue(source.hotelState().keys().stream()
                        .noneMatch(row -> row.itemObjectId() == keyId));
                assertFalse(source.sourceInventory(characterId).stream()
                        .anyMatch(row -> row.sourceObjectId() == keyId));
                assertSqlRows(jdbcUrl, jdbcLogin, jdbcPassword, keyId, false);

                // Character deletion is the irreversible item-removal path:
                // it must release the lease and remove the destroyed key map.
                long cleanupExpiry = expirySeconds + 30;
                GameStateStore.HotelLeaseState cleanupLease =
                        new GameStateStore.HotelLeaseState(
                                Math.toIntExact(cleanupKeyId), INN_NPC_ID, 1, false,
                                lease.renterIdentity(), cleanupExpiry, false);
                LitoOwnedInventoryItem cleanupAdena = owned(
                        adenaId, (int) InventoryCatalog.ADENA, "Hotel test Adena", 480, 0);
                LitoOwnedInventoryItem cleanupKey = owned(
                        cleanupKeyId, (int) ClassicWorldHotelCatalog.KEY_ITEM_ID,
                        "Hotel room key", 1, cleanupExpiry * 1_000L);
                assertEquals(RepositoryPersistenceResult.SUCCESS,
                        source.persistLitoHotelInventory(characterId, List.of(),
                                List.of(cleanupAdena, cleanupKey), List.of(), Set.of(),
                                source.litoPrivateInventory(characterId),
                                RepositoryHotelChange.rent(
                                        cleanupLease, List.of(cleanupKeyId))));
                assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                        source.deleteLitoCharacter("2222", CHARACTER).status());
                assertSqlRows(jdbcUrl, jdbcLogin, jdbcPassword, cleanupKeyId, false);
                assertTrue(source.hotelState().keys().stream()
                        .noneMatch(row -> row.itemObjectId() == cleanupKeyId));
            } finally {
                RepositoryCharacterMutation cleanup =
                        source.deleteLitoCharacter("2222", CHARACTER);
                assertTrue(cleanup.status() == RepositoryCharacterMutationStatus.NOT_OWNED
                        || cleanup.status() == RepositoryCharacterMutationStatus.SUCCESS);
            }
        }
    }

    private static LitoOwnedInventoryItem owned(
            long objectId, int itemId, String name, int quantity, long expiryMillis) {
        return new LitoOwnedInventoryItem(objectId, itemId, name, quantity,
                false, false, 0, true, 0, 0,
                RuntimeInventory.BLESSING_NORMAL, expiryMillis, false);
    }

    private static void store(Properties properties, Path target) throws Exception {
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary LiTo hotel integration profile");
        }
    }

    private static void assertSqlRows(
            String url, String login, String password, long keyId, boolean present)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(url, login, password);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT (SELECT COUNT(*) FROM lito_hotel_lease_key "
                             + "WHERE item_obj_id=?) AS hotel_rows,"
                             + "(SELECT COUNT(*) FROM character_items_time "
                             + "WHERE itemr_obj_id=?) AS time_rows")) {
            query.setLong(1, keyId);
            query.setLong(2, keyId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                assertEquals(present ? 1 : 0, row.getInt("hotel_rows"));
                assertEquals(present ? 1 : 0, row.getInt("time_rows"));
            }
        }
    }

    private static long allocateObjectId(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE server_info SET maxid=LAST_INSERT_ID("
                    + "GREATEST(COALESCE(maxid,0),COALESCE(minid,10000),"
                    + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                    + "COALESCE((SELECT MAX(objid) FROM character_pets),0))+1) WHERE id=1");
            try (ResultSet row = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (!row.next()) throw new IllegalStateException("object id was not allocated");
                return row.getLong(1);
            }
        }
    }
}
