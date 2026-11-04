package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlLetterIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    @Test
    void physicalLettersUseExactBackingIdsLockedCasAndRouteIsolation(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(temporary.resolve("writable.properties"), writable);
        String account = "LtLetterG";
        String senderName = "LitoLetterA";
        String receiverName = "LitoLetterB";
        String otherRouteName = "LitoLetterC";

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            // Cleanup an account name used by an earlier interrupted probe.
            routed.deleteLitoAccount("LitoLetterGate");
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            AccountRepository other = routed.repositoryForClassicSource("380 角色庫 2");
            for (AccountRepository repository : List.of(source, other)) {
                repository.deleteLitoCharacter(account, senderName);
                repository.deleteLitoCharacter(account, receiverName);
                repository.deleteLitoCharacter(account, otherRouteName);
                repository.deleteLitoAccount(account);
            }
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(account, "LetterGate1"));
            long senderId = create(source, account, senderName);
            long receiverId = create(source, account, receiverName);
            long otherRouteId = create(other, account, otherRouteName);
            Jdbc jdbc = jdbc(writable, "380_ac_1");
            long paperId = allocateObjectId(jdbc);
            LitoOwnedInventoryItem paper = new LitoOwnedInventoryItem(
                    paperId, Math.toIntExact(InventoryCatalog.LETTER_PAPER), "$1075", 3,
                    false, false, 0, true, 0, 0, 0, 0, false);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(senderId, List.of(), List.of(paper)));

            assertEquals(RepositoryLetterDeliveryStatus.RECIPIENT_NOT_FOUND,
                    source.deliverLetter(senderId, paperId, otherRouteName, 1,
                            "26/09/11", "route", "isolated").status());
            assertTrue(other.letters(otherRouteId).letters().isEmpty());

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(temporary.resolve("readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                AccountRepository readOnlySource = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1");
                assertEquals(RepositoryLetterDeliveryStatus.READ_ONLY,
                        readOnlySource.deliverLetter(senderId, paperId, receiverName, 1,
                                "26/09/11", "唯讀", "不得消耗").status());
                assertEquals(3, itemCount(jdbc, paperId));
            }

            RepositoryLetterDelivery delivered = source.deliverLetter(
                    senderId, paperId, receiverName, 7, "26/09/11",
                    "測試主旨", "UTF-16LE 內容");
            assertTrue(delivered.delivered());
            RepositoryLetter letter = delivered.letter();
            assertNotNull(letter);
            assertEquals(2, itemCount(jdbc, paperId));
            assertEquals(InventoryCatalog.LETTER_CLOSED,
                    itemCatalog(jdbc, letter.itemObjectId(), receiverId));
            assertTrue(originExists(jdbc, letter.itemObjectId(), receiverId,
                    InventoryCatalog.LETTER_CLOSED));
            RepositoryLetterSnapshot loaded = source.letters(receiverId);
            assertTrue(loaded.authoritative());
            assertEquals(List.of(letter), loaded.letters());

            byte[] originalSubject = fieldBytes(jdbc, letter.itemObjectId(), "subject");
            setFieldBytes(jdbc, letter.itemObjectId(), "subject",
                    terminated("外部變更"));
            RepositoryLetterMutation conflict = source.openLetter(receiverId, letter);
            assertEquals(RepositoryLetterMutationStatus.CONFLICT, conflict.status());
            assertEquals(InventoryCatalog.LETTER_CLOSED,
                    itemCatalog(jdbc, letter.itemObjectId(), receiverId));
            setFieldBytes(jdbc, letter.itemObjectId(), "subject", originalSubject);

            RepositoryLetterMutation opened = source.openLetter(receiverId, letter);
            assertEquals(RepositoryLetterMutationStatus.SUCCESS, opened.status());
            assertTrue(opened.letter().opened());
            assertEquals(InventoryCatalog.LETTER_OPEN,
                    itemCatalog(jdbc, letter.itemObjectId(), receiverId));
            assertTrue(originExists(jdbc, letter.itemObjectId(), receiverId,
                    InventoryCatalog.LETTER_OPEN));
            assertEquals(RepositoryLetterMutationStatus.ALREADY_OPEN,
                    source.openLetter(receiverId, letter).status());
            assertEquals(RepositoryLetterMutationStatus.CONFLICT,
                    source.deleteLetter(receiverId, letter).status());
            assertEquals(RepositoryLetterMutationStatus.SUCCESS,
                    source.deleteLetter(receiverId, opened.letter()).status());
            assertFalse(itemExists(jdbc, letter.itemObjectId()));
            assertFalse(letterExists(jdbc, letter.itemObjectId()));
            assertFalse(originExists(jdbc, letter.itemObjectId(), receiverId,
                    InventoryCatalog.LETTER_OPEN));

            RepositoryLetterDelivery pending = source.deliverLetter(
                    senderId, paperId, receiverName, 2, "26/09/11",
                    "刪角", "雙向清理");
            assertTrue(pending.delivered());
            RepositoryLetterDelivery lastPaper = source.deliverLetter(
                    senderId, paperId, receiverName, 3, "26/09/11",
                    "末張", "刪除實體信紙列");
            assertTrue(lastPaper.delivered());
            assertFalse(itemExists(jdbc, paperId));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    source.deleteLitoCharacter(account, senderName).status());
            assertFalse(letterExists(jdbc, pending.letter().itemObjectId()));
            assertFalse(itemExists(jdbc, pending.letter().itemObjectId()));
            assertFalse(letterExists(jdbc, lastPaper.letter().itemObjectId()));
            assertFalse(itemExists(jdbc, lastPaper.letter().itemObjectId()));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    source.deleteLitoCharacter(account, receiverName).status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    other.deleteLitoCharacter(account, otherRouteName).status());
            assertTrue(routed.deleteLitoAccount(account));
        }
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
        properties.setProperty("state.file",
                temporary.resolve(readOnly ? "readonly-state.properties"
                        : "writable-state.properties").toString());
        properties.setProperty("capture.directory", temporary.resolve("captures").toString());
        return properties;
    }

    private static Path writeConfig(Path path, Properties properties) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary physical-letter integration profile");
        }
        return path;
    }

    private static long create(AccountRepository source, String account, String name)
            throws Exception {
        RepositoryCharacterMutation created = source.createLitoCharacter(account,
                new Protocol.CustomCharacterRequest(name, 0, 1,
                        List.of(18, 13, 15, 9, 12, 8), 0, 0));
        assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
        return created.character().objectId();
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

    private static long allocateObjectId(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLES server_info WRITE, characters READ, "
                    + "character_items READ, character_pets READ");
            try {
                statement.executeUpdate("UPDATE server_info SET maxid=LAST_INSERT_ID("
                        + "GREATEST(COALESCE(maxid,0),COALESCE(minid,10000),"
                        + "COALESCE((SELECT MAX(objid) FROM characters),0),"
                        + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                        + "COALESCE((SELECT MAX(objid) FROM character_pets),0))+1) WHERE id=1");
                try (ResultSet row = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
                    row.next();
                    return row.getLong(1);
                }
            } finally {
                statement.execute("UNLOCK TABLES");
            }
        }
    }

    private static int itemCount(Jdbc jdbc, long objectId) throws Exception {
        return intQuery(jdbc, "SELECT count FROM character_items WHERE id=?", objectId);
    }

    private static long itemCatalog(Jdbc jdbc, long objectId, long owner) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT item_id FROM character_items WHERE id=? AND char_id=?")) {
            query.setLong(1, objectId);
            query.setLong(2, owner);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static boolean itemExists(Jdbc jdbc, long objectId) throws Exception {
        return intQuery(jdbc, "SELECT COUNT(*) FROM character_items WHERE id=?", objectId) != 0;
    }

    private static boolean letterExists(Jdbc jdbc, long objectId) throws Exception {
        return intQuery(jdbc, "SELECT COUNT(*) FROM character_letter WHERE item_object_id=?",
                objectId) != 0;
    }

    private static boolean originExists(Jdbc jdbc, long objectId, long owner, long catalog)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COUNT(*) FROM lito_item_origin WHERE item_obj_id=? "
                             + "AND char_obj_id=? AND catalog_id=?")) {
            query.setLong(1, objectId);
            query.setLong(2, owner);
            query.setLong(3, catalog);
            try (ResultSet row = query.executeQuery()) {
                row.next();
                return row.getInt(1) == 1;
            }
        }
    }

    private static int intQuery(Jdbc jdbc, String sql, long objectId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getInt(1) : 0;
            }
        }
    }

    private static byte[] fieldBytes(Jdbc jdbc, long objectId, String column)
            throws Exception {
        if (!column.equals("subject") && !column.equals("content")) {
            throw new IllegalArgumentException("unsupported letter blob column");
        }
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT " + column + " FROM character_letter WHERE item_object_id=?")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getBytes(1);
            }
        }
    }

    private static void setFieldBytes(Jdbc jdbc, long objectId, String column, byte[] value)
            throws Exception {
        if (!column.equals("subject") && !column.equals("content")) {
            throw new IllegalArgumentException("unsupported letter blob column");
        }
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE character_letter SET " + column + "=? WHERE item_object_id=?")) {
            update.setBytes(1, value);
            update.setLong(2, objectId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static byte[] terminated(String value) {
        byte[] text = value.getBytes(StandardCharsets.UTF_16LE);
        return java.util.Arrays.copyOf(text, text.length + 2);
    }
}
