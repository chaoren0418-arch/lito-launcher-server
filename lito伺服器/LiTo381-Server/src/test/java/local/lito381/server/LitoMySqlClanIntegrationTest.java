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
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlClanIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    @Test
    void nativeClanRowsUseLockedSameRouteMutationsAndLitoMetadata(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("writable.properties"), writable);
        String account = "LtClanGate";
        String ownerName = "LitoClanOwn";
        String memberName = "LitoClanMem";
        String otherName = "LitoClanTwo";
        String clanName = "LitoSqlClan";

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            AccountRepository other = routed.repositoryForClassicSource("380 角色庫 2");
            cleanup(source, account, ownerName, memberName);
            cleanup(other, account, otherName);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(account, "ClanGate1"));
            long ownerId = create(source, account, ownerName, 0);
            long memberId = create(source, account, memberName, 1);
            long otherId = create(other, account, otherName, 0);
            assertEquals(RepositoryClanSnapshot.empty(), source.clan(ownerId));
            assertEquals(RepositoryCharacterTitle.present(""),
                    source.characterTitle(ownerId));
            assertEquals(RepositoryCharacterTitle.present(""),
                    other.characterTitle(otherId));

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                AccountRepository readOnlySource = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1");
                assertEquals(RepositoryClanMutationStatus.READ_ONLY,
                        readOnlySource.createClan(ownerId, clanName).status());
                assertEquals(RepositoryTitleMutationStatus.READ_ONLY,
                        readOnlySource.persistCharacterTitle(
                                ownerId, "", "ReadOnlyTitle").status());
                assertEquals(0, nativeClanId(jdbc(writable, "380_ac_1"), ownerId));
            }

            RepositoryTitleMutation title = source.persistCharacterTitle(
                    ownerId, "", "SqlTitle");
            assertEquals(RepositoryTitleMutationStatus.SUCCESS, title.status());
            assertEquals(RepositoryCharacterTitle.present("SqlTitle"),
                    source.characterTitle(ownerId));
            assertEquals(RepositoryCharacterTitle.present(""),
                    other.characterTitle(otherId));
            assertEquals(RepositoryTitleMutationStatus.CONFLICT,
                    source.persistCharacterTitle(ownerId, "", "StaleTitle").status());
            assertEquals("SqlTitle", nativeTitle(
                    jdbc(writable, "380_ac_1"), ownerId));

            RepositoryClanMutation created = source.createClan(ownerId, clanName);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, created.status());
            RepositoryClanSnapshot clan = created.snapshot();
            assertTrue(clan.present());
            assertEquals(ownerId, clan.leaderObjectId());
            assertEquals(4, clan.members().get(0).role());
            assertEquals(4, clan.members().get(0).nativeRank());
            Jdbc firstJdbc = jdbc(writable, "380_ac_1");
            assertNativeMembership(firstJdbc, ownerId, clan.clanId(), clanName, 4);
            assertTrue(litoOwned(firstJdbc, clan.clanId()));
            assertEquals(RepositoryCharacterMutationStatus.CLAN_OWNER,
                    source.deleteLitoCharacter(account, ownerName).status());
            assertTrue(source.clan(ownerId).present());

            assertEquals(RepositoryClanMutationStatus.DUPLICATE_NAME,
                    source.createClan(memberId, clanName).status());
            RepositoryClanMutation isolated = other.createClan(otherId, clanName);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, isolated.status());
            assertTrue(isolated.snapshot().present());
            assertTrue(other.rollbackCreatedClan(otherId, isolated.snapshot().clanId()));

            assertEquals(RepositoryClanMutationStatus.NOT_FOUND,
                    source.joinClan(ownerId, "WrongRouteName", 10).status());
            RepositoryClanMutation joined = source.joinClan(
                    ownerId, memberName, 10);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, joined.status());
            assertEquals(2, joined.snapshot().members().size());
            assertNativeMembership(firstJdbc, memberId, clan.clanId(), clanName, 2);

            RepositoryClanMutation promoted = source.changeClanMember(
                    ownerId, memberName, 2, 1);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, promoted.status());
            assertNativeMembership(firstJdbc, memberId, clan.clanId(), clanName, 3);
            setNativeRank(firstJdbc, memberId, 2);
            RepositoryClanMutation stale = source.changeClanMember(
                    ownerId, memberName, 1, 3);
            assertEquals(RepositoryClanMutationStatus.CONFLICT, stale.status());
            assertEquals(2, member(stale.snapshot(), memberName).role());

            RepositoryClanMutation candidate = source.changeClanMember(
                    ownerId, memberName, 2, 3);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, candidate.status());
            assertNativeMembership(firstJdbc, memberId, clan.clanId(), clanName, 1);
            assertEquals(3, member(candidate.snapshot(), memberName).role());
            RepositoryClanMutation removed = source.changeClanMember(
                    ownerId, memberName, 3, 0);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, removed.status());
            assertEquals(0, nativeClanId(firstJdbc, memberId));

            RepositoryClanMutation pending = source.setClanDestroyPending(
                    ownerId, false, true);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, pending.status());
            assertTrue(pending.snapshot().destroyPending());
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryClanMutation refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .setClanDestroyPending(ownerId, true, false);
                assertEquals(RepositoryClanMutationStatus.READ_ONLY, refused.status());
                assertTrue(source.clan(ownerId).destroyPending());
            }
            assertEquals(RepositoryClanMutationStatus.CONFLICT,
                    source.setClanDestroyPending(ownerId, false, false).status());
            assertEquals(RepositoryClanMutationStatus.SUCCESS,
                    source.setClanDestroyPending(ownerId, true, false).status());

            assertTrue(source.rollbackCreatedClan(ownerId, clan.clanId()));
            assertEquals(RepositoryClanSnapshot.empty(), source.clan(ownerId));
            assertFalse(clanExists(firstJdbc, clan.clanId()));
            assertFalse(clanMetadataExists(firstJdbc, clan.clanId()));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    source.deleteLitoCharacter(account, ownerName).status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    source.deleteLitoCharacter(account, memberName).status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    other.deleteLitoCharacter(account, otherName).status());
            assertTrue(routed.deleteLitoAccount(account));
        }
    }

    private static RepositoryClanMember member(
            RepositoryClanSnapshot clan, String name) {
        return clan.members().stream().filter(row -> row.name().equals(name))
                .findFirst().orElseThrow();
    }

    private static void cleanup(AccountRepository repository, String account,
            String... characters) throws Exception {
        for (String character : characters) {
            RepositoryClanSnapshot clan = repository.characters(account).stream()
                    .filter(row -> row.name().equals(character))
                    .findFirst().map(row -> {
                        try {
                            return repository.clan(row.objectId());
                        } catch (Exception failure) {
                            throw new RuntimeException(failure);
                        }
                    }).orElse(RepositoryClanSnapshot.empty());
            if (clan.present() && clan.leaderName().equals(character)) {
                for (RepositoryClanMember member : clan.members()) {
                    if (member.objectId() != clan.leaderObjectId()) {
                        repository.changeClanMember(clan.leaderObjectId(),
                                member.name(), member.role(), 0);
                    }
                }
                repository.setClanDestroyPending(
                        clan.leaderObjectId(), clan.destroyPending(), false);
                repository.rollbackCreatedClan(clan.leaderObjectId(), clan.clanId());
            }
            repository.deleteLitoCharacter(account, character);
        }
        repository.deleteLitoAccount(account);
    }

    private static long create(AccountRepository repository, String account,
            String name, int type) throws Exception {
        RepositoryCharacterMutation created = repository.createLitoCharacter(account,
                new Protocol.CustomCharacterRequest(name, type, 1,
                        List.of(18, 13, 15, 9, 12, 8), 0, 0));
        assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
        return created.character().objectId();
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
            properties.store(writer, "temporary clan integration profile");
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

    private static long nativeClanId(Jdbc jdbc, long characterId) throws Exception {
        return longQuery(jdbc, "SELECT ClanID FROM characters WHERE objid=?", characterId);
    }

    private static String nativeTitle(Jdbc jdbc, long characterId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT Title FROM characters WHERE objid=?")) {
            query.setLong(1, characterId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getString(1) : null;
            }
        }
    }

    private static void assertNativeMembership(Jdbc jdbc, long characterId,
            long clanId, String clanName, int rank) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT ClanID,Clanname,ClanRank FROM characters WHERE objid=?")) {
            query.setLong(1, characterId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                assertEquals(clanId, row.getLong("ClanID"));
                assertEquals(clanName, row.getString("Clanname"));
                assertEquals(rank, row.getInt("ClanRank"));
            }
        }
    }

    private static void setNativeRank(Jdbc jdbc, long characterId, int rank)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE characters SET ClanRank=? WHERE objid=?")) {
            update.setInt(1, rank);
            update.setLong(2, characterId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static boolean litoOwned(Jdbc jdbc, long clanId) throws Exception {
        return longQuery(jdbc,
                "SELECT lito_owned FROM lito_clan_metadata WHERE clan_id=?", clanId) == 1;
    }

    private static boolean clanExists(Jdbc jdbc, long clanId) throws Exception {
        return longQuery(jdbc,
                "SELECT COUNT(*) FROM clan_data WHERE clan_id=?", clanId) != 0;
    }

    private static boolean clanMetadataExists(Jdbc jdbc, long clanId) throws Exception {
        return longQuery(jdbc,
                "SELECT COUNT(*) FROM lito_clan_metadata WHERE clan_id=?", clanId) != 0;
    }

    private static long longQuery(Jdbc jdbc, String sql, long id) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, id);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getLong(1) : 0;
            }
        }
    }
}
