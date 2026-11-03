package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class LitoDatabaseLayoutTest {
    private static final Pattern CREATE_TABLE =
            Pattern.compile("(?im)^CREATE TABLE\\s");

    @Test void generatedLayoutContainsOneStaticAndTwoCompleteDynamicSchemas()
            throws Exception {
        assertDump("lito_static.sql", "lito_static", 183);
        assertDump("380_ac_1.sql", "380_ac_1", 86);
        assertDump("380_ac_2.sql", "380_ac_2", 86);
        for (String file : List.of("380_ac_1.sql", "380_ac_2.sql")) {
            String sql = Files.readString(Path.of("db", "generated", file),
                    StandardCharsets.UTF_8);
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `lito_buddy_group`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `lito_buddy_metadata`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `lito_clan_metadata`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `lito_castle_metadata`"));
            assertTrue(sql.contains(
                    "CREATE TABLE IF NOT EXISTS `lito_castle_security_settlement`"));
            assertTrue(sql.contains(
                    "CREATE TABLE IF NOT EXISTS `lito_house_auction_metadata`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `lito_board_topic`"));
            assertTrue(sql.contains(
                    "CREATE TABLE IF NOT EXISTS `lito_tournament_profile`"));
            assertTrue(sql.contains(
                    "CREATE TABLE IF NOT EXISTS `lito_private_inventory`"));
            assertTrue(sql.contains(
                    "CREATE TABLE IF NOT EXISTS `lito_hotel_lease_key`"));
            assertTrue(sql.contains(
                    "CREATE TABLE IF NOT EXISTS `lito_water_dragon_pass`"));
            assertTrue(sql.contains("ADD COLUMN `lito_event_key` varchar(96)"));
            assertTrue(sql.contains(
                    "ADD UNIQUE KEY `uq_lito_reward_event` (`lito_event_key`)"));
        }
        String staticSql = Files.readString(
                Path.of("db", "generated", "lito_static.sql"),
                StandardCharsets.UTF_8);
        assertTrue(staticSql.contains(
                "CREATE TABLE IF NOT EXISTS `lito_tournament_character_record`"));
        assertTrue(staticSql.contains(
                "PRIMARY KEY (`source_server_id`,`char_obj_id`)"));
        assertTrue(staticSql.contains(
                "UNIQUE KEY `uq_lito_tournament_source_name` "
                        + "(`source_server_id`,`char_name`)"));
    }

    @Test void generatorOwnsEveryRequiredLitoExtensionTable() throws Exception {
        String generator = Files.readString(
                Path.of("tools", "Build-LiToSqlLayout.ps1"), StandardCharsets.UTF_8);
        String repository = Files.readString(
                Path.of("src", "main", "java", "local", "lito381", "server",
                        "Accounts.java"), StandardCharsets.UTF_8);
        String normalizedRepository = repository.replaceAll("\\s+", " ");
        assertTrue(generator.contains("ALTER TABLE `server_紀錄_獎勵`"));
        assertTrue(repository.contains("schemaColumnExists(connection,"));
        assertTrue(repository.contains("schemaIndexExists(connection,"));
        for (String table : List.of(
                "lito_character_origin", "lito_account_origin", "lito_item_origin",
                "lito_warehouse_item_origin", "lito_buddy_group",
                "lito_buddy_metadata", "lito_clan_metadata",
                "lito_castle_metadata", "lito_castle_security_settlement",
                "lito_house_auction_metadata",
                "lito_board_topic",
                "lito_tournament_profile", "lito_private_inventory",
                "lito_hotel_lease_key", "lito_water_dragon_pass")) {
            assertTrue(generator.contains("CREATE TABLE IF NOT EXISTS `" + table + "`"),
                    "generator missing " + table);
            assertTrue(normalizedRepository.contains(
                            "checkTable(accountUrl, accountLogin, accountPassword, \""
                            + table + "\")"),
                    "health check missing " + table);
        }
        assertTrue(generator.contains(
                "CREATE TABLE IF NOT EXISTS `lito_tournament_character_record`"));
        assertTrue(normalizedRepository.contains(
                "resourceTable(\"lito_tournament_character_record\")"));
        assertTrue(normalizedRepository.contains(
                "source_server_id=? AND char_obj_id=?"));
    }

    @Test void productionConfigsBindEveryDynamicRouteToTheSameStaticSchema()
            throws Exception {
        for (String mode : List.of("standard", "rental-source", "rental-battle")) {
            ServerConfig config = ServerConfig.load(
                    Path.of("config", "server.properties"), mode);
            assertEquals("lito_mysql", config.databaseMode(), mode);
            assertEquals(List.of("380_ac_1", "380_ac_2"),
                    config.legacySourceDatabases(), mode);
            assertEquals(List.of("lito_static", "lito_static"),
                    config.legacySourceResourceDatabases(), mode);
            assertTrue(config.databaseReadOnly(), mode);
        }
    }

    private static void assertDump(String file, String database, int tableCount)
            throws Exception {
        Path path = Path.of("db", "generated", file);
        assertTrue(Files.isRegularFile(path), path.toString());
        String sql = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(sql.startsWith(
                "-- Generated by Build-LiToSqlLayout.ps1; do not edit by hand."));
        assertTrue(sql.contains("USE `" + database + "`;"));
        assertEquals(tableCount, CREATE_TABLE.matcher(sql).results().count(), file);
    }
}
