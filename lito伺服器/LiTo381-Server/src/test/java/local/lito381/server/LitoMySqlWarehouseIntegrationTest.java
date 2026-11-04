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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlWarehouseIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    @Test
    void passwordAndAllThreeWarehousesUseAtomicSqlAuthority(@TempDir Path temporary)
            throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Path base = Path.of("config", "server.properties")
                .toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (Reader reader = LocalResources.newBufferedReader(base, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        properties.setProperty("database.read.only", "false");
        properties.setProperty("database.connection.properties",
                Path.of("..", "1-伺服器", "config", "sql.properties")
                        .toAbsolutePath().normalize().toString());
        properties.setProperty("maps.directory",
                Path.of("maps").toAbsolutePath().normalize().toString());
        properties.setProperty("state.file", temporary.resolve("state.properties").toString());
        properties.setProperty("capture.directory", temporary.resolve("captures").toString());
        Path writableConfig = temporary.resolve("server.properties");
        try (Writer writer = Files.newBufferedWriter(writableConfig, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary writable warehouse integration profile");
        }

        String account = "LitoWhGate";
        String character = "LitoWhGate";
        String buddyCharacter = "LitoWhBuddy";
        String secondCharacter = "LitoWhGate2";
        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            for (String route : List.of("380 角色庫 1", "380 角色庫 2")) {
                AccountRepository cleanup = routed.repositoryForClassicSource(route);
                cleanup.deleteLitoCharacter(account, character);
                cleanup.deleteLitoCharacter(account, buddyCharacter);
                cleanup.deleteLitoCharacter(account, secondCharacter);
                cleanup.deleteLitoAccount(account);
            }
            clearWarehouse(properties, account, "LitoWhClan");
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(account, "Warehouse1"));
            RepositoryCharacterMutation created = source.createLitoCharacter(account,
                    new Protocol.CustomCharacterRequest(
                            character, 0, 1, List.of(18, 13, 15, 9, 12, 8), 0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
            long characterId = created.character().objectId();
            RepositoryCharacterMutation buddyCreated = source.createLitoCharacter(account,
                    new Protocol.CustomCharacterRequest(
                            buddyCharacter, 0, 1,
                            List.of(18, 13, 15, 9, 12, 8), 0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    buddyCreated.status());
            AccountRepository secondSource =
                    routed.repositoryForClassicSource("380 角色庫 2");
            RepositoryCharacterMutation secondCreated = secondSource.createLitoCharacter(
                    account, new Protocol.CustomCharacterRequest(
                            secondCharacter, 0, 1,
                            List.of(18, 13, 15, 9, 12, 8), 0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    secondCreated.status());
            try {
                RepositoryBuddySnapshot initialBuddies = source.buddies(characterId);
                assertTrue(initialBuddies.authoritative());
                assertEquals(List.of(new RepositoryBuddyGroup(1, "Friends")),
                        initialBuddies.groups());
                assertTrue(initialBuddies.buddies().isEmpty());
                List<RepositoryBuddyGroup> buddyGroups = List.of(
                        new RepositoryBuddyGroup(1, "Friends"),
                        new RepositoryBuddyGroup(2, "Raid"));
                List<RepositoryBuddy> buddyRows = List.of(
                        new RepositoryBuddy(buddyCharacter,
                                buddyCreated.character().objectId(), "380 角色庫 1",
                                2, 2, 3, "Local"),
                        new RepositoryBuddy(secondCharacter, 0, "380 角色庫 2",
                                1, 1, 1, "Remote"));
                RepositoryBuddyPersistence buddyWrite = source.persistBuddies(
                        characterId, initialBuddies, buddyGroups, buddyRows);
                assertEquals(RepositoryBuddyPersistenceStatus.SUCCESS,
                        buddyWrite.status());
                assertEquals(buddyGroups, buddyWrite.snapshot().groups());
                assertEquals(buddyRows, buddyWrite.snapshot().buddies());
                assertNativeBuddyRows(properties, "380_ac_1", characterId,
                        Map.of(buddyCharacter, buddyCreated.character().objectId()));
                assertTrue(secondSource.buddies(
                        secondCreated.character().objectId()).buddies().isEmpty());

                Properties buddySql = sqlProperties(properties);
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl(buddySql, "380_ac_1"), sqlLogin(buddySql),
                        sqlPassword(buddySql));
                     PreparedStatement insert = connection.prepareStatement(
                             "INSERT INTO character_buddys "
                                     + "(char_id,buddy_id,buddy_name) VALUES (?,?,?)")) {
                    insert.setLong(1, characterId);
                    insert.setLong(2, 0x7012_3456L);
                    insert.setString(3, "ExternalTwBuddy");
                    assertEquals(1, insert.executeUpdate());
                }
                RepositoryBuddyPersistence staleBuddyWrite = source.persistBuddies(
                        characterId, buddyWrite.snapshot(), buddyGroups, List.of());
                assertEquals(RepositoryBuddyPersistenceStatus.CONFLICT,
                        staleBuddyWrite.status());
                assertTrue(staleBuddyWrite.snapshot().buddies().stream()
                        .anyMatch(buddy -> buddy.name().equals("ExternalTwBuddy")));
                RepositoryBuddyPersistence reconciledBuddies = source.persistBuddies(
                        characterId, staleBuddyWrite.snapshot(), buddyGroups, buddyRows);
                assertEquals(RepositoryBuddyPersistenceStatus.SUCCESS,
                        reconciledBuddies.status());

                RepositoryBuffSnapshot initialBuffs = source.buffs(characterId);
                assertTrue(initialBuffs.authoritative());
                assertEquals(0, initialBuffs.completedVersion());
                assertTrue(initialBuffs.buffs().isEmpty());
                List<RepositoryBuff> retainedBuffs = List.of(
                        new RepositoryBuff(148, 900, -1),
                        new RepositoryBuff(4026, 500, 3_000),
                        new RepositoryBuff(9_999, -1, 77));
                RepositoryBuffPersistence buffWrite = source.persistBuffs(
                        characterId, initialBuffs, retainedBuffs);
                assertEquals(RepositoryBuffPersistenceStatus.SUCCESS,
                        buffWrite.status());
                assertTrue(buffWrite.snapshot().completedVersion() > 0);
                assertEquals(retainedBuffs, source.buffs(characterId).buffs());

                Properties buffSql = sqlProperties(properties);
                long externalVersion = buffWrite.snapshot().completedVersion() + 1;
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl(buffSql, "380_ac_1"), sqlLogin(buffSql),
                        sqlPassword(buffSql));
                     PreparedStatement row = connection.prepareStatement(
                             "INSERT INTO character_buff "
                                     + "(char_obj_id,skill_id,remaining_time,poly_id,snapshot_version) "
                                     + "VALUES (?,?,?,?,?)");
                     PreparedStatement marker = connection.prepareStatement(
                             "UPDATE character_buff_snapshot SET completed_version=? "
                                     + "WHERE char_obj_id=?")) {
                    row.setLong(1, characterId);
                    row.setInt(2, 155);
                    row.setInt(3, 456);
                    row.setInt(4, -1);
                    row.setLong(5, externalVersion);
                    assertEquals(1, row.executeUpdate());
                    marker.setLong(1, externalVersion);
                    marker.setLong(2, characterId);
                    assertEquals(1, marker.executeUpdate());
                }
                RepositoryBuffPersistence staleBuffWrite = source.persistBuffs(
                        characterId, buffWrite.snapshot(), List.of());
                assertEquals(RepositoryBuffPersistenceStatus.CONFLICT,
                        staleBuffWrite.status());
                assertEquals(List.of(new RepositoryBuff(155, 456, -1)),
                        staleBuffWrite.snapshot().buffs());
                RepositoryBuffPersistence clearedBuffs = source.persistBuffs(
                        characterId, staleBuffWrite.snapshot(), List.of());
                assertEquals(RepositoryBuffPersistenceStatus.SUCCESS,
                        clearedBuffs.status());
                assertTrue(source.buffs(characterId).buffs().isEmpty());

                RepositoryBookmarkSnapshot initialBookmarks = source.bookmarks(characterId);
                assertTrue(initialBookmarks.authoritative());
                assertTrue(initialBookmarks.bookmarks().isEmpty());
                List<RepositoryBookmark> bookmarks = List.of(
                        new RepositoryBookmark("LiTo Aden", 33970, 33245, 4),
                        new RepositoryBookmark("LiTo Kent", 33080, 32764, 4));
                assertEquals(RepositoryBookmarkPersistenceResult.SUCCESS,
                        source.persistBookmarks(characterId, List.of(), bookmarks));
                assertEquals(bookmarks, source.bookmarks(characterId).bookmarks());

                Properties bookmarkSql = sqlProperties(properties);
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl(bookmarkSql, "380_ac_1"), sqlLogin(bookmarkSql),
                        sqlPassword(bookmarkSql));
                     PreparedStatement update = connection.prepareStatement(
                             "UPDATE character_teleport SET locx=locx+1 "
                                     + "WHERE char_id=? AND name=?")) {
                    update.setLong(1, characterId);
                    update.setString(2, "LiTo Aden");
                    assertEquals(1, update.executeUpdate());
                }
                assertEquals(RepositoryBookmarkPersistenceResult.CONFLICT,
                        source.persistBookmarks(characterId, bookmarks, List.of()));
                RepositoryBookmarkSnapshot refreshedBookmarks = source.bookmarks(characterId);
                assertEquals(RepositoryBookmarkPersistenceResult.SUCCESS,
                        source.persistBookmarks(characterId, refreshedBookmarks.bookmarks(),
                                List.of()));
                assertTrue(source.bookmarks(characterId).bookmarks().isEmpty());

                RepositoryBookmarkSnapshot secondInitial = secondSource.bookmarks(
                        secondCreated.character().objectId());
                assertTrue(secondInitial.authoritative());
                RepositoryBuffSnapshot secondInitialBuffs = secondSource.buffs(
                        secondCreated.character().objectId());
                RepositoryBuff secondRouteBuff = new RepositoryBuff(163, 444, -1);
                RepositoryBuffPersistence secondBuffWrite = secondSource.persistBuffs(
                        secondCreated.character().objectId(), secondInitialBuffs,
                        List.of(secondRouteBuff));
                assertEquals(RepositoryBuffPersistenceStatus.SUCCESS,
                        secondBuffWrite.status());
                assertEquals(List.of(secondRouteBuff), secondSource.buffs(
                        secondCreated.character().objectId()).buffs());
                RepositoryBookmark secondRouteBookmark = new RepositoryBookmark(
                        "Route Two", 32612, 32774, 4);
                assertEquals(RepositoryBookmarkPersistenceResult.SUCCESS,
                        secondSource.persistBookmarks(secondCreated.character().objectId(),
                                secondInitial.bookmarks(), List.of(secondRouteBookmark)));
                assertEquals(List.of(secondRouteBookmark), secondSource.bookmarks(
                        secondCreated.character().objectId()).bookmarks());
                Properties readOnlyProperties = new Properties();
                readOnlyProperties.putAll(properties);
                readOnlyProperties.setProperty("database.read.only", "true");
                Path readOnlyConfig = temporary.resolve("server-read-only.properties");
                try (Writer writer = Files.newBufferedWriter(
                        readOnlyConfig, StandardCharsets.UTF_8)) {
                    readOnlyProperties.store(writer,
                            "temporary read-only bookmark integration profile");
                }
                try (AccountRepository readOnlyRouted = AccountRepository.open(
                        ServerConfig.load(readOnlyConfig))) {
                    AccountRepository readOnlySecond =
                            readOnlyRouted.repositoryForClassicSource("380 角色庫 2");
                    assertEquals(List.of(secondRouteBookmark), readOnlySecond.bookmarks(
                            secondCreated.character().objectId()).bookmarks());
                    RepositoryBuffSnapshot readOnlyBuffs = readOnlySecond.buffs(
                            secondCreated.character().objectId());
                    assertEquals(List.of(secondRouteBuff), readOnlyBuffs.buffs());
                    assertEquals(RepositoryBuffPersistenceStatus.READ_ONLY,
                            readOnlySecond.persistBuffs(
                                    secondCreated.character().objectId(), readOnlyBuffs,
                                    List.of()).status());
                    assertEquals(List.of(secondRouteBuff), readOnlySecond.buffs(
                            secondCreated.character().objectId()).buffs());
                    RepositoryBuddySnapshot readOnlyBuddies =
                            readOnlyRouted.repositoryForClassicSource("380 角色庫 1")
                                    .buddies(characterId);
                    assertEquals(RepositoryBuddyPersistenceStatus.READ_ONLY,
                            readOnlyRouted.repositoryForClassicSource("380 角色庫 1")
                                    .persistBuddies(characterId, readOnlyBuddies,
                                            List.of(new RepositoryBuddyGroup(1, "Friends")),
                                            List.of()).status());
                    assertEquals(buddyRows, readOnlyRouted
                            .repositoryForClassicSource("380 角色庫 1")
                            .buddies(characterId).buddies());
                    assertEquals(RepositoryBookmarkPersistenceResult.READ_ONLY,
                            readOnlySecond.persistBookmarks(
                                    secondCreated.character().objectId(),
                                    List.of(secondRouteBookmark), List.of()));
                    assertEquals(List.of(secondRouteBookmark), readOnlySecond.bookmarks(
                            secondCreated.character().objectId()).bookmarks());
                }

                assertEquals(0, source.warehousePassword(account).password());
                assertEquals(GameStateStore.WarehousePasswordStatus.CHANGED,
                        source.changeWarehousePassword(account, 0, 654321).status());
                assertEquals(654321, source.warehousePassword(account).password());
                assertEquals(GameStateStore.WarehousePasswordStatus.WRONG_OLD,
                        source.changeWarehousePassword(account, 1, 0).status());
                assertEquals(GameStateStore.WarehousePasswordStatus.CHANGED,
                        source.changeWarehousePassword(account, 654321, 0).status());
                assertEquals(0, source.warehousePassword(account).password());

                int offset = 0;
                for (GameStateStore.WarehouseScope scope
                        : GameStateStore.WarehouseScope.values()) {
                    String owner = scope == GameStateStore.WarehouseScope.CLAN
                            ? "LitoWhClan" : account;
                    RepositoryWarehouseSnapshot initial = source.warehouse(scope, owner);
                    assertTrue(initial.authoritative());
                    assertTrue(initial.items().isEmpty());

                    RuntimeInventory beforeInventory = new RuntimeInventory();
                    int currentOffset = offset;
                    InventoryCatalog.Definition storedDefinition = InventoryCatalog.require(
                            scope == GameStateStore.WarehouseScope.ELF
                                    ? InventoryCatalog.PURE_MITHRIL
                                    : InventoryCatalog.ADENA);
                    assertTrue(beforeInventory.add(
                            storedDefinition, 100 + currentOffset,
                            () -> 0x7001_1000L + currentOffset));
                    RuntimeInventory beforeWarehouse = initial.runtimeInventory();
                    RuntimeInventory.Item offered = beforeInventory.items().get(0);
                    ClassicWarehouseTransaction.Outcome deposited =
                            ClassicWarehouseTransaction.apply(
                                    scope == GameStateStore.WarehouseScope.PERSONAL ? 2
                                            : scope == GameStateStore.WarehouseScope.CLAN ? 4 : 8,
                                    Map.of(offered.objectId(), 40L), 1,
                                    Set.of(offered.objectId()), beforeInventory,
                                    beforeWarehouse);
                    assertTrue(deposited.accepted());
                    assertEquals(RepositoryWarehouseTransferResult.SUCCESS,
                            source.persistWarehouseTransfer(
                                    characterId, scope, owner, beforeInventory,
                                    beforeWarehouse, deposited.inventory(),
                                    deposited.warehouse(), Map.of()));

                    RepositoryWarehouseSnapshot stored = source.warehouse(scope, owner);
                    assertEquals(40, stored.items().stream()
                            .mapToInt(SourceInventoryItem::quantity).sum());
                    assertTrue(stored.items().get(0).litoOwned());
                    assertTrue(source.sourceInventory(characterId).stream()
                            .anyMatch(item -> item.quantity() == 60 + currentOffset
                                    && item.litoOwned()));

                    RuntimeInventory sqlInventory = new RepositoryWarehouseSnapshot(
                            true, source.sourceInventory(characterId)).runtimeInventory();
                    RuntimeInventory sqlWarehouse = stored.runtimeInventory();
                    RuntimeInventory.Item storedItem = sqlWarehouse.items().get(0);
                    int retrieveMode = scope == GameStateStore.WarehouseScope.PERSONAL ? 3
                            : scope == GameStateStore.WarehouseScope.CLAN ? 5 : 9;
                    ClassicWarehouseTransaction.Outcome retrieved =
                            ClassicWarehouseTransaction.apply(
                                    retrieveMode, Map.of(storedItem.objectId(), 10L), 1,
                                    Set.of(storedItem.objectId()), sqlInventory, sqlWarehouse);
                    assertTrue(retrieved.accepted());
                    assertEquals(RepositoryWarehouseTransferResult.SUCCESS,
                            source.persistWarehouseTransfer(
                                    characterId, scope, owner, sqlInventory, sqlWarehouse,
                                    retrieved.inventory(), retrieved.warehouse(), Map.of()));
                    assertEquals(30, source.warehouse(scope, owner).items().stream()
                            .mapToInt(SourceInventoryItem::quantity).sum());
                    offset += 10;
                }

                RepositoryWarehouseSnapshot stale = source.warehouse(
                        GameStateStore.WarehouseScope.PERSONAL, account);
                RuntimeInventory staleWarehouse = stale.runtimeInventory();
                RuntimeInventory currentInventory = new RepositoryWarehouseSnapshot(
                        true, source.sourceInventory(characterId)).runtimeInventory();
                RuntimeInventory.Item row = staleWarehouse.items().get(0);
                Properties sql = sqlProperties(properties);
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl(sql, "380_ac_1"), sqlLogin(sql), sqlPassword(sql));
                     PreparedStatement update = connection.prepareStatement(
                             "UPDATE character_warehouse SET count=count+1 WHERE id=?")) {
                    update.setLong(1, row.objectId());
                    assertEquals(1, update.executeUpdate());
                }
                ClassicWarehouseTransaction.Outcome staleAttempt =
                        ClassicWarehouseTransaction.apply(
                                3, Map.of(row.objectId(), 1L), 1,
                                Set.of(row.objectId()), currentInventory, staleWarehouse);
                assertTrue(staleAttempt.accepted());
                assertEquals(RepositoryWarehouseTransferResult.CONFLICT,
                        source.persistWarehouseTransfer(characterId,
                                GameStateStore.WarehouseScope.PERSONAL, account,
                                currentInventory, staleWarehouse, staleAttempt.inventory(),
                                staleAttempt.warehouse(), Map.of()));

                assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                        source.deleteLitoCharacter(account, buddyCharacter).status());
                RepositoryBuddySnapshot afterTargetDelete = source.buddies(characterId);
                assertFalse(afterTargetDelete.buddies().stream()
                        .anyMatch(buddy -> buddy.name().equals(buddyCharacter)));
                assertTrue(afterTargetDelete.buddies().stream()
                        .anyMatch(buddy -> buddy.name().equals(secondCharacter)));
            } finally {
                clearWarehouse(properties, account, "LitoWhClan");
                source.deleteLitoCharacter(account, character);
                source.deleteLitoCharacter(account, buddyCharacter);
                secondSource.deleteLitoCharacter(account, secondCharacter);
                assertBuddyRowsDeleted(properties, "380_ac_1", characterId);
                assertBuffRowsDeleted(properties, "380_ac_1", characterId);
                assertBuffRowsDeleted(properties, "380_ac_2",
                        secondCreated.character().objectId());
                for (String route : List.of("380 角色庫 1", "380 角色庫 2")) {
                    routed.repositoryForClassicSource(route).deleteLitoAccount(account);
                }
                assertFalse(routed.accountExists(account));
            }
        }
    }

    private static Properties sqlProperties(Properties server) throws Exception {
        Properties sql = new Properties();
        try (Reader reader = LocalResources.newBufferedReader(
                Path.of(server.getProperty("database.connection.properties")),
                StandardCharsets.UTF_8)) {
            sql.load(reader);
        }
        return sql;
    }

    private static String jdbcUrl(Properties sql, String database) {
        return sql.getProperty("URL1_LOGIN") + database
                + sql.getProperty("URL3_LOGIN", "");
    }

    private static String sqlLogin(Properties sql) {
        return sql.getProperty("Login_LOGIN", sql.getProperty("Login"));
    }

    private static String sqlPassword(Properties sql) {
        return sql.getProperty("Password_LOGIN", sql.getProperty("Password", ""));
    }

    private static void clearWarehouse(
            Properties server, String account, String clan) throws Exception {
        Properties sql = sqlProperties(server);
        for (String database : List.of("380_ac_1", "380_ac_2")) {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(sql, database), sqlLogin(sql), sqlPassword(sql))) {
            for (String statement : List.of(
                    "DELETE FROM character_warehouse WHERE account_name=?",
                    "DELETE FROM character_elf_warehouse WHERE account_name=?")) {
                try (PreparedStatement delete = connection.prepareStatement(statement)) {
                    delete.setString(1, account);
                    delete.executeUpdate();
                }
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM clan_warehouse WHERE clan_name=?")) {
                delete.setString(1, clan);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM lito_warehouse_item_origin WHERE owner_key IN (?,?)")) {
                delete.setString(1, account);
                delete.setString(2, clan);
                delete.executeUpdate();
            }
        }
        }
    }

    private static void assertBuffRowsDeleted(
            Properties server, String database, long characterId) throws Exception {
        Properties sql = sqlProperties(server);
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(sql, database), sqlLogin(sql), sqlPassword(sql))) {
            for (String table : List.of("character_buff", "character_buff_snapshot")) {
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT COUNT(*) FROM " + table + " WHERE char_obj_id=?")) {
                    query.setLong(1, characterId);
                    try (java.sql.ResultSet row = query.executeQuery()) {
                        assertTrue(row.next());
                        assertEquals(0, row.getInt(1));
                    }
                }
            }
        }
    }

    private static void assertNativeBuddyRows(Properties server, String database,
            long characterId, Map<String, Long> expected) throws Exception {
        Properties sql = sqlProperties(server);
        Map<String, Long> actual = new java.util.LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(sql, database), sqlLogin(sql), sqlPassword(sql));
             PreparedStatement query = connection.prepareStatement(
                     "SELECT buddy_name,buddy_id FROM character_buddys "
                             + "WHERE char_id=? ORDER BY id")) {
            query.setLong(1, characterId);
            try (java.sql.ResultSet row = query.executeQuery()) {
                while (row.next()) actual.put(row.getString(1), row.getLong(2));
            }
        }
        assertEquals(expected, actual);
    }

    private static void assertBuddyRowsDeleted(
            Properties server, String database, long characterId) throws Exception {
        Properties sql = sqlProperties(server);
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(sql, database), sqlLogin(sql), sqlPassword(sql))) {
            for (String querySql : List.of(
                    "SELECT COUNT(*) FROM character_buddys "
                            + "WHERE char_id=? OR buddy_id=?",
                    "SELECT COUNT(*) FROM lito_buddy_group WHERE char_obj_id=?",
                    "SELECT COUNT(*) FROM lito_buddy_metadata "
                            + "WHERE char_obj_id=? OR buddy_id=?")) {
                try (PreparedStatement query = connection.prepareStatement(querySql)) {
                    query.setLong(1, characterId);
                    if (querySql.contains(" OR ")) query.setLong(2, characterId);
                    try (java.sql.ResultSet row = query.executeQuery()) {
                        assertTrue(row.next());
                        assertEquals(0, row.getInt(1));
                    }
                }
            }
        }
    }
}
