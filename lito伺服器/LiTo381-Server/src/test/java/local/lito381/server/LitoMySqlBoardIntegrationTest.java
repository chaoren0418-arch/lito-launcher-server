package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlBoardIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    @Test
    void boardIsPerRouteAndAtomicallyDebitsAdenaWithConflictAndOwnershipChecks(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(temporary.resolve("writable.properties"), writable);
        String account = "LtBoardGate";
        String authorName = "LitoBoardOne";
        String peerName = "LitoBoardPeer";
        String otherName = "LitoBoardTwo";

        ServerConfig writableServer = ServerConfig.load(writableConfig);
        String firstSource = writableServer.classicServerNames().get(0);
        String secondSource = writableServer.classicServerNames().get(1);
        String firstDatabase = writableServer.legacySourceDatabases().get(0);
        String secondDatabase = writableServer.legacySourceDatabases().get(1);

        try (AccountRepository routed = AccountRepository.open(
                writableServer)) {
            AccountRepository first = routed.repositoryForClassicSource(firstSource);
            AccountRepository second = routed.repositoryForClassicSource(secondSource);
            cleanup(first, account, authorName, peerName);
            cleanup(second, account, otherName);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(account, "BoardGate1"));
            long authorId = create(first, account, authorName, 0);
            long peerId = create(first, account, peerName, 1);
            long otherId = create(second, account, otherName, 0);
            GameStateStore tournament = new GameStateStore(
                    temporary.resolve("tournament.properties"), true);
            String authorIdentity = GameStateStore.sourceCharacter(
                    firstSource, authorName);
            tournament.bindRankingIdentity(authorIdentity, firstSource, 0, 1, 0);
            tournament.replaceTournamentAwards(authorIdentity,
                    new GameStateStore.TournamentAwards(
                            List.of(0x11, 0x22, 0x34, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                            List.of(1, 2, 3, 4, 5), true));
            tournament.recordResult(authorIdentity, true, false, 75, 0);
            GameStateStore.TournamentProfileState firstProfile =
                    tournament.tournamentProfileState(authorIdentity);
            RepositoryTournamentMutation profileCreated = first.persistTournamentProfile(
                    authorId, authorName, null, firstProfile);
            assertEquals(RepositoryTournamentMutationStatus.SUCCESS,
                    profileCreated.status());
            assertEquals(firstProfile, first.tournamentProfiles().profiles().stream()
                    .filter(profile -> profile.characterObjectId() == authorId)
                    .findFirst().orElseThrow().state());
            assertTrue(second.tournamentProfiles().profiles().stream()
                    .allMatch(profile -> profile.snapshotVersion() == 0));
            assertEquals(RepositoryTournamentMutationStatus.CONFLICT,
                    first.persistTournamentProfile(authorId, authorName, null,
                            firstProfile).status());
            tournament.recordResult(authorIdentity, false, true, 25, 0);
            GameStateStore.TournamentProfileState secondProfile =
                    tournament.tournamentProfileState(authorIdentity);
            RepositoryTournamentMutation profileUpdated = first.persistTournamentProfile(
                    authorId, authorName, profileCreated.profile(), secondProfile);
            assertEquals(RepositoryTournamentMutationStatus.SUCCESS,
                    profileUpdated.status());
            assertEquals(1, profileUpdated.profile().state().wins());
            assertEquals(1, profileUpdated.profile().state().losses());
            assertEquals(1, profileUpdated.profile().state().disconnects());
            assertEquals(RepositoryTournamentMutationStatus.CONFLICT,
                    first.persistTournamentProfile(authorId, authorName,
                            profileCreated.profile(), secondProfile).status());
            Jdbc firstJdbc = jdbc(writable, firstDatabase);
            Jdbc secondJdbc = jdbc(writable, secondDatabase);
            long authorAdenaId = allocateInventoryObjectId(firstJdbc);
            long peerAdenaId = authorAdenaId + 1;
            long otherAdenaId = allocateInventoryObjectId(secondJdbc);
            insertAdena(firstJdbc, authorId, authorAdenaId, 1_000);
            insertAdena(firstJdbc, peerId, peerAdenaId, 1_000);
            insertAdena(secondJdbc, otherId, otherAdenaId, 1_000);

            assertTrue(first.boardTopicsBefore(
                    Protocol.BOARD_INITIAL_CURSOR, 8).authoritative());
            RuntimeInventory authorBefore = adenaInventory(authorAdenaId, 1_000);
            RuntimeInventory authorAfter = authorBefore.copy();
            assertTrue(authorAfter.removeCatalog(
                    InventoryCatalog.ADENA, Protocol.BOARD_WRITE_FEE));
            RepositoryBoardMutation posted = first.postBoardTopic(authorId,
                    "26/09/05", "SQL board", "route one body", authorBefore, authorAfter,
                    Map.of(authorAdenaId, authorAdenaId));
            assertEquals(RepositoryBoardMutationStatus.SUCCESS, posted.status());
            assertEquals(700, itemCount(firstJdbc, authorId, authorAdenaId));
            assertEquals(posted.topic(), first.boardTopic(posted.topic().id()).topic());
            assertTrue(first.boardTopicsBefore(Protocol.BOARD_INITIAL_CURSOR, 8).topics()
                    .stream().anyMatch(topic -> topic.title().equals("SQL board")));
            assertFalse(second.boardTopicsBefore(Protocol.BOARD_INITIAL_CURSOR, 8).topics()
                    .stream().anyMatch(topic -> topic.title().equals("SQL board")));
            long unsafeId = posted.topic().id() + 1;
            insertNativeBoard(firstJdbc, unsafeId, authorName, "26/09/05",
                    "TW long content", "界".repeat(600));
            assertNull(first.boardTopic(unsafeId).topic());
            assertTrue(first.boardTopicsBefore(Protocol.BOARD_INITIAL_CURSOR, 8).topics()
                    .stream().noneMatch(topic -> topic.id() == unsafeId));

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("readonly.properties"), readOnly);
            RuntimeInventory current = adenaInventory(authorAdenaId, 700);
            RuntimeInventory charged = current.copy();
            assertTrue(charged.removeCatalog(
                    InventoryCatalog.ADENA, Protocol.BOARD_WRITE_FEE));
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                AccountRepository readOnlyFirst = readOnlyRouted
                        .repositoryForClassicSource(firstSource);
                assertEquals(secondProfile, readOnlyFirst.tournamentProfiles().profiles()
                        .stream().filter(profile -> profile.characterObjectId() == authorId)
                        .findFirst().orElseThrow().state());
                RepositoryTournamentMutation litoResultWrite = readOnlyFirst
                        .persistTournamentProfile(authorId, authorName,
                                profileUpdated.profile(), firstProfile);
                assertEquals(RepositoryTournamentMutationStatus.SUCCESS,
                        litoResultWrite.status());
                assertEquals(firstProfile, readOnlyFirst.tournamentProfiles().profiles()
                        .stream().filter(profile ->
                                profile.characterObjectId() == authorId)
                        .findFirst().orElseThrow().state());
                RepositoryBoardMutation refused = readOnlyRouted
                        .repositoryForClassicSource(firstSource)
                        .postBoardTopic(authorId, "26/09/05", "Read only", "no write",
                                current, charged, Map.of(authorAdenaId, authorAdenaId));
                assertEquals(RepositoryBoardMutationStatus.READ_ONLY, refused.status());
            }
            assertEquals(700, itemCount(firstJdbc, authorId, authorAdenaId));

            updateItemCount(firstJdbc, authorId, authorAdenaId, 699);
            RepositoryBoardMutation stale = first.postBoardTopic(authorId,
                    "26/09/05", "Stale", "must not post", current, charged,
                    Map.of(authorAdenaId, authorAdenaId));
            assertEquals(RepositoryBoardMutationStatus.CONFLICT, stale.status());
            assertEquals(699, itemCount(firstJdbc, authorId, authorAdenaId));
            assertFalse(first.boardTopicsBefore(Protocol.BOARD_INITIAL_CURSOR, 8).topics()
                    .stream().anyMatch(topic -> topic.title().equals("Stale")));
            updateItemCount(firstJdbc, authorId, authorAdenaId, 700);

            assertEquals(RepositoryBoardMutationStatus.NOT_AUTHOR,
                    first.deleteBoardTopic(peerId, posted.topic()).status());
            updateBoardTitle(firstJdbc, posted.topic().id(), "TW external edit");
            assertEquals(RepositoryBoardMutationStatus.CONFLICT,
                    first.deleteBoardTopic(authorId, posted.topic()).status());
            GameStateStore.BoardTopic currentTopic = first.boardTopic(posted.topic().id()).topic();
            assertEquals("TW external edit", currentTopic.title());
            assertEquals(RepositoryBoardMutationStatus.SUCCESS,
                    first.deleteBoardTopic(authorId, currentTopic).status());
            assertNull(first.boardTopic(posted.topic().id()).topic());

            RuntimeInventory otherBefore = adenaInventory(otherAdenaId, 1_000);
            RuntimeInventory otherAfter = otherBefore.copy();
            assertTrue(otherAfter.removeCatalog(
                    InventoryCatalog.ADENA, Protocol.BOARD_WRITE_FEE));
            RepositoryBoardMutation isolated = second.postBoardTopic(otherId,
                    "26/09/05", "Route two", "isolated body", otherBefore, otherAfter,
                    Map.of(otherAdenaId, otherAdenaId));
            assertEquals(RepositoryBoardMutationStatus.SUCCESS, isolated.status());
            assertEquals(700, itemCount(secondJdbc, otherId, otherAdenaId));
            assertFalse(first.boardTopicsBefore(Protocol.BOARD_INITIAL_CURSOR, 8).topics()
                    .stream().anyMatch(topic -> topic.title().equals("Route two")));

            RuntimeInventory peerBefore = adenaInventory(peerAdenaId, 1_000);
            RuntimeInventory peerAfter = peerBefore.copy();
            assertTrue(peerAfter.removeCatalog(
                    InventoryCatalog.ADENA, Protocol.BOARD_WRITE_FEE));
            RepositoryBoardMutation doomed = first.postBoardTopic(peerId,
                    "26/09/05", "Delete cleanup", "owned by deleted character",
                    peerBefore, peerAfter, Map.of(peerAdenaId, peerAdenaId));
            assertEquals(RepositoryBoardMutationStatus.SUCCESS, doomed.status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    first.deleteLitoCharacter(account, peerName).status());
            assertNull(first.boardTopic(doomed.topic().id()).topic());

            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    first.deleteLitoCharacter(account, authorName).status());
            assertTrue(first.tournamentProfiles().profiles().stream()
                    .noneMatch(profile -> profile.characterObjectId() == authorId));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    second.deleteLitoCharacter(account, otherName).status());
            assertTrue(routed.deleteLitoAccount(account));
        }
    }

    private static long create(AccountRepository repository, String account,
                               String name, int type) throws Exception {
        RepositoryCharacterMutation created = repository.createLitoCharacter(account,
                new Protocol.CustomCharacterRequest(name, type, 1,
                        List.of(18, 13, 15, 9, 12, 8), 0, 0));
        assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
        return created.character().objectId();
    }

    private static void cleanup(AccountRepository repository, String account,
                                String... characters) throws Exception {
        for (String character : characters) {
            repository.deleteLitoCharacter(account, character);
        }
        repository.deleteLitoAccount(account);
    }

    private static RuntimeInventory adenaInventory(long objectId, int quantity) {
        RuntimeInventory inventory = new RuntimeInventory();
        assertEquals(objectId, inventory.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.ADENA),
                new RuntimeInventory.PersistentItem(objectId, quantity, 0, 0,
                        true, RuntimeInventory.BLESSING_NORMAL, true, false,
                        false, null, 0, 0, false, false)));
        return inventory;
    }

    private static Properties integrationProperties(Path temporary, boolean readOnly)
            throws Exception {
        Properties properties = new Properties();
        try (Reader reader = LocalResources.newBufferedReader(
                Path.of("config", "server.properties"),
                StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        properties.setProperty("database.read.only", Boolean.toString(readOnly));
        properties.setProperty("database.connection.properties",
                Path.of("..", "1-伺服器", "config", "sql.properties")
                        .toAbsolutePath().normalize().toString());
        properties.setProperty("maps.directory",
                Path.of("maps").toAbsolutePath().normalize().toString());
        properties.setProperty("state.file", temporary.resolve(
                readOnly ? "readonly-state.properties" : "writable-state.properties")
                .toAbsolutePath().toString());
        properties.setProperty("capture.directory",
                temporary.resolve("captures").toAbsolutePath().toString());
        return properties;
    }

    private static Path writeConfig(Path path, Properties properties) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary board integration profile");
        }
        return path;
    }

    private record Jdbc(String url, String login, String password) {}

    private static Jdbc jdbc(Properties server, String database) throws Exception {
        Properties sql = new Properties();
        try (Reader reader = LocalResources.newBufferedReader(Path.of(
                server.getProperty("database.connection.properties")),
                StandardCharsets.UTF_8)) {
            sql.load(reader);
        }
        return new Jdbc(sql.getProperty("URL1_LOGIN") + database
                + sql.getProperty("URL3_LOGIN", ""),
                sql.getProperty("Login_LOGIN", sql.getProperty("Login")),
                sql.getProperty("Password_LOGIN", sql.getProperty("Password", "")));
    }

    private static long allocateInventoryObjectId(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT GREATEST("
                             + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                             + "COALESCE((SELECT MAX(id) FROM character_warehouse),0),"
                             + "COALESCE((SELECT MAX(id) FROM clan_warehouse),0),"
                             + "COALESCE((SELECT MAX(id) FROM character_elf_warehouse),0)) + 1")) {
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static void insertAdena(
            Jdbc jdbc, long characterId, long objectId, int count) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO character_items "
                             + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,"
                             + "enchantlvl,is_id,durability,charge_count,bless) "
                             + "VALUES (?,?,?,?,?,0,0,0,1,0,0,1)")) {
            insert.setLong(1, objectId);
            insert.setInt(2, Math.toIntExact(InventoryCatalog.ADENA));
            insert.setLong(3, characterId);
            insert.setString(4, "Adena");
            insert.setInt(5, count);
            assertEquals(1, insert.executeUpdate());
        }
    }

    private static long itemCount(Jdbc jdbc, long characterId, long objectId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT count FROM character_items WHERE char_id=? AND id=?")) {
            query.setLong(1, characterId);
            query.setLong(2, objectId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static void updateItemCount(
            Jdbc jdbc, long characterId, long objectId, int count) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE character_items SET count=? WHERE char_id=? AND id=?")) {
            update.setInt(1, count);
            update.setLong(2, characterId);
            update.setLong(3, objectId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static void updateBoardTitle(Jdbc jdbc, long topicId, String title)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `server_盟屋_佈告` SET title=? WHERE id=?")) {
            update.setString(1, title);
            update.setLong(2, topicId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static void insertNativeBoard(Jdbc jdbc, long topicId, String author,
                                          String date, String title, String content)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO `server_盟屋_佈告` "
                             + "(id,name,date,title,content) VALUES (?,?,?,?,?)")) {
            insert.setLong(1, topicId);
            insert.setString(2, author);
            insert.setString(3, date);
            insert.setString(4, title);
            insert.setString(5, content);
            assertEquals(1, insert.executeUpdate());
        }
    }
}
