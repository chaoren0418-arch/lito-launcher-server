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
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LitoMySqlMarriageIntegrationTest {
    @Test
    void partnerIdIsReciprocalSqlFirstReadOnlyAndRouteIsolated(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("writable.properties"), writable);
        String account = "LtMarryGate";
        String firstName = "LitoMarryOne";
        String secondName = "LitoMarryTwo";
        String routeTwoFirstName = "LitoMarryR2A";
        String routeTwoSecondName = "LitoMarryR2B";
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        Jdbc secondJdbc = jdbc(writable, "380_ac_2");

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository firstRoute = routed.repositoryForClassicSource("380 角色庫 1");
            AccountRepository secondRoute = routed.repositoryForClassicSource("380 角色庫 2");
            // Clean the overlong pre-release probe account left by an aborted
            // development run before the native origin-column limit was applied.
            routed.deleteLitoAccount("LtMarriageGate");
            clearPartnerIds(firstJdbc, firstName, secondName);
            clearPartnerIds(secondJdbc, routeTwoFirstName, routeTwoSecondName);
            cleanup(firstRoute, account, firstName, secondName);
            cleanup(secondRoute, account, routeTwoFirstName, routeTwoSecondName);
            routed.deleteLitoAccount(account);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(account, "MarriageGate1"));

            long firstId = create(firstRoute, account, firstName, 0);
            long secondId = create(firstRoute, account, secondName, 1);
            long routeTwoFirstId = create(
                    secondRoute, account, routeTwoFirstName, 0);
            long routeTwoSecondId = create(
                    secondRoute, account, routeTwoSecondName, 1);
            try {
                RepositoryMarriageState initial = firstRoute.marriage(firstId);
                assertTrue(initial.authoritative());
                assertTrue(initial.consistent());
                assertFalse(initial.married());
                assertEquals(RepositoryMarriageStateStatus.NOT_FOUND,
                        firstRoute.marriage(Integer.MAX_VALUE).status());

                Properties readOnly = integrationProperties(temporary, true);
                Path readOnlyConfig = writeConfig(
                        temporary.resolve("readonly.properties"), readOnly);
                try (AccountRepository readOnlyRouted = AccountRepository.open(
                        ServerConfig.load(readOnlyConfig))) {
                    AccountRepository readOnlyFirst = readOnlyRouted
                            .repositoryForClassicSource("380 角色庫 1");
                    assertEquals(RepositoryMarriageMutationStatus.READ_ONLY,
                            readOnlyFirst.persistMarriage(firstId, secondId).status());
                    assertFalse(readOnlyFirst.marriage(firstId).married());
                }

                RepositoryMarriageMutation married = firstRoute.persistMarriage(
                        firstId, secondId);
                assertEquals(RepositoryMarriageMutationStatus.SUCCESS,
                        married.status());
                assertEquals(secondId, nativePartner(firstJdbc, firstId));
                assertEquals(firstId, nativePartner(firstJdbc, secondId));
                assertEquals(secondName, firstRoute.marriage(firstId).partnerName());
                assertEquals(firstName, firstRoute.marriage(secondId).partnerName());
                assertEquals(RepositoryMarriageMutationStatus.ALREADY_MARRIED,
                        firstRoute.persistMarriage(firstId, secondId).status());

                // Each dynamic database owns an independent PartnerID namespace.
                assertEquals(0, nativePartner(secondJdbc, routeTwoFirstId));
                assertEquals(0, nativePartner(secondJdbc, routeTwoSecondId));

                setNativePartner(firstJdbc, secondId, 0);
                assertEquals(RepositoryMarriageStateStatus.CONFLICT,
                        firstRoute.marriage(firstId).status());
                assertEquals(RepositoryMarriageMutationStatus.STALE,
                        firstRoute.persistDivorce(firstId, secondId).status());
                assertEquals(secondId, nativePartner(firstJdbc, firstId));
                setNativePartner(firstJdbc, secondId, firstId);

                RepositoryMarriageMutation divorced = firstRoute.persistDivorce(
                        firstId, secondId);
                assertEquals(RepositoryMarriageMutationStatus.SUCCESS,
                        divorced.status());
                assertEquals(0, nativePartner(firstJdbc, firstId));
                assertEquals(0, nativePartner(firstJdbc, secondId));
                assertEquals(RepositoryMarriageMutationStatus.STALE,
                        firstRoute.persistDivorce(firstId, secondId).status());
            } finally {
                clearPartnerIds(firstJdbc, firstName, secondName);
                clearPartnerIds(secondJdbc, routeTwoFirstName, routeTwoSecondName);
                cleanup(firstRoute, account, firstName, secondName);
                cleanup(secondRoute, account, routeTwoFirstName, routeTwoSecondName);
                routed.deleteLitoAccount(account);
            }
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
            String... names) throws Exception {
        for (String name : names) repository.deleteLitoCharacter(account, name);
    }

    private static Properties integrationProperties(Path temporary, boolean readOnly)
            throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(
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
                readOnly ? "readonly-state.properties"
                        : "writable-state.properties").toString());
        properties.setProperty("capture.directory", temporary.resolve("captures").toString());
        return properties;
    }

    private static Path writeConfig(Path path, Properties properties) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary marriage integration profile");
        }
        return path;
    }

    private record Jdbc(String url, String login, String password) {}

    private static Jdbc jdbc(Properties server, String database) throws Exception {
        Properties sql = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(
                server.getProperty("database.connection.properties")),
                StandardCharsets.UTF_8)) {
            sql.load(reader);
        }
        return new Jdbc(sql.getProperty("URL1_LOGIN") + database
                + sql.getProperty("URL3_LOGIN", ""),
                sql.getProperty("Login_LOGIN", sql.getProperty("Login")),
                sql.getProperty("Password_LOGIN", sql.getProperty("Password", "")));
    }

    private static long nativePartner(Jdbc jdbc, long characterId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT PartnerID FROM characters WHERE objid=?")) {
            query.setLong(1, characterId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getLong(1) : -1;
            }
        }
    }

    private static void setNativePartner(Jdbc jdbc, long characterId, long partnerId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE characters SET PartnerID=? WHERE objid=?")) {
            update.setLong(1, partnerId);
            update.setLong(2, characterId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static void clearPartnerIds(Jdbc jdbc, String... names) throws Exception {
        if (names.length == 0) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(
                names.length, "?"));
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE characters SET PartnerID=0 WHERE char_name IN ("
                             + placeholders + ")")) {
            for (int index = 0; index < names.length; index++) {
                update.setString(index + 1, names[index]);
            }
            update.executeUpdate();
        }
    }
}
