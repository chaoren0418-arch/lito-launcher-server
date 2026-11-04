package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlWaterDragonPassIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    private static final String ROUTE_ONE = "380 角色庫 1";
    private static final String ROUTE_TWO = "380 角色庫 2";
    private static final String CHARACTER = "LitoWaterPassGate";

    @Test
    void passLeaseIsAtomicRouteQualifiedReplaySafeAndDeletedWithCharacter(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Path base = Path.of("config", "server.properties")
                .toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (Reader reader = LocalResources.newBufferedReader(base, StandardCharsets.UTF_8)) {
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
        try (Reader reader = LocalResources.newBufferedReader(sqlConfig, StandardCharsets.UTF_8)) {
            jdbc.load(reader);
        }
        String jdbcUrl = jdbc.getProperty("URL1_LOGIN") + "380_ac_1"
                + jdbc.getProperty("URL3_LOGIN");
        String jdbcLogin = jdbc.getProperty("Login_LOGIN");
        String jdbcPassword = jdbc.getProperty("Password_LOGIN");

        try (AccountRepository accounts = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            accounts.migrateLitoExtensions();
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
                long passId;
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl, jdbcLogin, jdbcPassword)) {
                    adenaId = allocateObjectId(connection);
                    passId = allocateObjectId(connection);
                }
                LitoOwnedInventoryItem initialAdena = owned(
                        adenaId, (int) InventoryCatalog.ADENA,
                        "Water pass test Adena", 20_000, 0);
                assertEquals(RepositoryPersistenceResult.SUCCESS,
                        source.persistLitoInventory(characterId, List.of(),
                                List.of(initialAdena), List.of(), Set.of(),
                                source.litoPrivateInventory(characterId)));

                long expiry = System.currentTimeMillis() / 1_000L
                        + ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS;
                String renter = GameStateStore.sourceCharacter(ROUTE_ONE, CHARACTER);
                GameStateStore.WaterDragonPassState pass =
                        new GameStateStore.WaterDragonPassState(
                                ROUTE_ONE, passId, Math.toIntExact(passId), 0,
                                renter, expiry);
                LitoOwnedInventoryItem remainingAdena = owned(
                        adenaId, (int) InventoryCatalog.ADENA,
                        "Water pass test Adena", 10_400, 0);
                LitoOwnedInventoryItem passItem = owned(
                        passId, ClassicWorldWaterDragonCatalog.PASS_ITEM_ID,
                        "Water Dragon pass",
                        ClassicWorldWaterDragonCatalog.PASS_RENTAL_COUNT,
                        expiry * 1_000L);
                RepositoryWaterDragonPassChange change =
                        new RepositoryWaterDragonPassChange(pass);
                assertEquals(RepositoryPersistenceResult.SUCCESS,
                        source.persistLitoWaterDragonPassInventory(
                                characterId, List.of(),
                                List.of(remainingAdena, passItem), List.of(), Set.of(),
                                source.litoPrivateInventory(characterId), change));

                RepositoryWaterDragonPassSnapshot stored = source.waterDragonPassState();
                assertTrue(stored.authoritative());
                assertEquals(List.of(pass), stored.passes());
                RepositoryPrivateInventorySnapshot privateInventory =
                        source.litoPrivateInventory(characterId);
                assertTrue(privateInventory.items().stream().anyMatch(
                        item -> item.objectId() == passId
                                && item.itemId()
                                        == ClassicWorldWaterDragonCatalog.PASS_ITEM_ID
                                && item.expiresAtEpochMillis() == expiry * 1_000L));
                assertTrue(other.waterDragonPassState().passes().stream()
                        .noneMatch(row -> row.itemObjectId() == passId));
                assertSqlRow(jdbcUrl, jdbcLogin, jdbcPassword, passId, true);

                assertEquals(RepositoryPersistenceResult.CONFLICT,
                        source.persistLitoWaterDragonPassInventory(
                                characterId, List.of(),
                                List.of(remainingAdena, passItem), List.of(), Set.of(),
                                source.litoPrivateInventory(characterId), change));

                Properties readOnlyProperties = new Properties();
                readOnlyProperties.putAll(properties);
                readOnlyProperties.setProperty("database.read.only", "true");
                Path readOnlyConfig = temporary.resolve("server-read-only.properties");
                store(readOnlyProperties, readOnlyConfig);
                try (AccountRepository readOnly = AccountRepository.open(
                        ServerConfig.load(readOnlyConfig))) {
                    assertEquals(RepositoryPersistenceResult.READ_ONLY,
                            readOnly.repositoryForClassicSource(ROUTE_ONE)
                                    .persistLitoWaterDragonPassInventory(
                                            characterId, List.of(),
                                            List.of(remainingAdena, passItem),
                                            List.of(), Set.of(), privateInventory, change));
                }

                assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                        source.deleteLitoCharacter("2222", CHARACTER).status());
                assertSqlRow(jdbcUrl, jdbcLogin, jdbcPassword, passId, false);
                assertTrue(source.waterDragonPassState().passes().stream()
                        .noneMatch(row -> row.itemObjectId() == passId));
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
            properties.store(writer, "temporary LiTo Water Dragon integration profile");
        }
    }

    private static void assertSqlRow(
            String url, String login, String password, long passId, boolean present)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(url, login, password);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COUNT(*) FROM lito_water_dragon_pass WHERE item_obj_id=?")) {
            query.setLong(1, passId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                assertEquals(present ? 1 : 0, row.getInt(1));
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
                if (!row.next()) throw new IllegalStateException(
                        "object id was not allocated");
                return row.getLong(1);
            }
        }
    }
}
