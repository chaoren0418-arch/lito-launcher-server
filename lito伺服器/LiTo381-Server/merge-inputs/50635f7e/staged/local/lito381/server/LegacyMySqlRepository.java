package local.lito381.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.Reader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ORIGINAL SOURCE extracted from Accounts.java for T020-B08 staged three-way port
// (package-private top-level class; not a nested type).
final class LegacyMySqlRepository implements AccountRepository {
    // The selected legacy Lineage source is an immutable input. Write paths
    // below require both the separate lito_mysql mode and an explicit writable
    // profile, so legacy_mysql can never mutate retained TW380 data.
    private final String accountUrl;
    private final String accountLogin;
    private final String accountPassword;
    private final String accountDatabase;
    private final String classicSourceName;
    private final int classicSourceId;
    private final String resourceDatabase;
    private final double serverWeightRate;
    private final boolean readOnly;
    private final String repositoryMode;
    private final HikariDataSource dataSource;
    private final Map<Integer, SourceNpcPhysicalStats> npcPhysicalCache =
            new ConcurrentHashMap<>();
    private final Map<Integer, SourceSummonTemplate> summonTemplateCache =
            new ConcurrentHashMap<>();
    private final Map<Integer, SourcePetTemplate> petTemplateCache =
            new ConcurrentHashMap<>();
    private final Map<Integer, InventoryCatalog.Definition> rewardItemCache =
            new ConcurrentHashMap<>();
    private record SqlBookmark(long id, RepositoryBookmark bookmark) {}
    private record SqlBuddyMetadata(long buddyObjectId, String targetSource,
                                    int groupId, int relationshipStyle,
                                    int paletteOffset, String origin) {}

    LegacyMySqlRepository(Path legacyProperties, String classicSourceName,
            String loginDatabaseOverride,
            String resourceDatabase, double serverWeightRate, boolean readOnly,
            String repositoryMode)
            throws IOException, ClassNotFoundException {
        this(legacyProperties, classicSourceName, 0, loginDatabaseOverride,
                resourceDatabase, serverWeightRate, readOnly, repositoryMode);
    }

    LegacyMySqlRepository(Path legacyProperties, String classicSourceName,
            int classicSourceId, String loginDatabaseOverride,
            String resourceDatabase, double serverWeightRate, boolean readOnly,
            String repositoryMode)
            throws IOException, ClassNotFoundException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(legacyProperties, StandardCharsets.UTF_8)) {
            p.load(reader);
            UnifiedConfiguration.aliases(p);
        }
        accountDatabase = loginDatabaseOverride == null || loginDatabaseOverride.isBlank()
                ? required(p, "URL2_LOGIN", null)
                : loginDatabaseOverride.trim();
        if (!accountDatabase.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("database.legacy.login.database contains unsupported characters");
        }
        this.classicSourceName = classicSourceName == null
                ? "" : classicSourceName.trim();
        if (classicSourceId < 0 || classicSourceId > 0xFF) {
            throw new IllegalArgumentException("classic source id is outside byte range");
        }
        this.classicSourceId = classicSourceId;
        if (resourceDatabase == null || !resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "database.legacy.source.resource.databases contains unsupported characters");
        }
        this.resourceDatabase = resourceDatabase;
        if (!Double.isFinite(serverWeightRate) || serverWeightRate <= 0.0) {
            throw new IllegalArgumentException("invalid legacy source weight rate");
        }
        this.serverWeightRate = serverWeightRate;
        accountUrl = jdbcUrlWithDatabase(p, "URL1_LOGIN", accountDatabase, "URL3_LOGIN");
        accountLogin = requiredWithFallback(p, "Login_LOGIN", "Login");
        accountPassword = p.getProperty("Password_LOGIN", p.getProperty("Password", ""));
        this.readOnly = readOnly;
        this.repositoryMode = repositoryMode;
        Class.forName(p.getProperty("Driver_LOGIN", "com.mysql.cj.jdbc.Driver").trim());
        HikariConfig pool = new HikariConfig();
        pool.setJdbcUrl(accountUrl);
        pool.setUsername(accountLogin);
        pool.setPassword(accountPassword);
        pool.setReadOnly(readOnly);
        pool.setMinimumIdle(0);
        pool.setMaximumPoolSize(8);
        // Preserve the repository's lazy-construction boundary: startup health
        // checks still fail explicitly when SQL is unavailable, while read-only
        // mutations can short-circuit without opening a connection at all.
        pool.setInitializationFailTimeout(-1);
        pool.setConnectionTimeout(5_000);
        pool.setIdleTimeout(60_000);
        pool.setMaxLifetime(600_000);
        pool.setPoolName("lito-" + Integer.toUnsignedString(
                (classicSourceName + "\u0000" + accountDatabase).hashCode(), 36));
        dataSource = new HikariDataSource(pool);
    }

    @Override public void healthCheck() throws SQLException {
        checkTable(accountUrl, accountLogin, accountPassword, "accounts");
        checkTable(accountUrl, accountLogin, accountPassword, "characters");
        checkTable(accountUrl, accountLogin, accountPassword, "character_other");
        checkTable(accountUrl, accountLogin, accountPassword, "character_skills");
        checkTable(accountUrl, accountLogin, accountPassword, "character_quests");
        checkTable(accountUrl, accountLogin, accountPassword, "character_items");
        checkTable(accountUrl, accountLogin, accountPassword, "character_pets");
        checkTable(accountUrl, accountLogin, accountPassword, "character_teleport");
        checkTable(accountUrl, accountLogin, accountPassword, "character_buff");
        checkTable(accountUrl, accountLogin, accountPassword, "character_buddys");
        checkTable(accountUrl, accountLogin, accountPassword, "character_letter");
        checkTable(accountUrl, accountLogin, accountPassword, "character_warehouse");
        checkTable(accountUrl, accountLogin, accountPassword, "character_elf_warehouse");
        checkTable(accountUrl, accountLogin, accountPassword, "clan_warehouse");
        checkTable(accountUrl, accountLogin, accountPassword, "character_items_card");
        checkTable(accountUrl, accountLogin, accountPassword, "character_items_dark");
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("skills"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("pettypes"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("commands"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("lito_tournament_shop"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("lito_battle_rule"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("lito_battle_object"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("lito_battle_spawn"));
        checkTable(accountUrl, accountLogin, accountPassword,
                resourceTable("lito_battle_player_spawn"));
        for (String table : LegacyEquipmentMagicLoader.requiredResourceTables()) {
            checkTable(accountUrl, accountLogin, accountPassword,
                    LegacyEquipmentMagicLoader.qualified(resourceDatabase, table));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            LegacyEquipmentMagicLoader.healthCheck(connection, resourceDatabase);
            LegacyInventoryWeightLoader.healthCheck(connection, resourceDatabase);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT OriginalStr,OriginalDex,OriginalCon,Food,HellTime FROM characters LIMIT 0");
                 ResultSet ignored = statement.executeQuery()) {
                // Metadata-only validation for native natural-HPR inputs.
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT char_obj_id,killCount,deathCount FROM character_other LIMIT 0");
                 ResultSet ignored = statement.executeQuery()) {
                // Metadata-only validation for retained kill/death counters.
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT npcid,ac,size,damage_reduction,hard,undead,weakAttr,mr,IsTU,IsErase,lawful,"
                            + "tamable,boss,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk FROM "
                            + resourceTable("npc") + " LIMIT 0");
                 ResultSet ignored = statement.executeQuery()) {
                // Metadata-only validation for native NPC physical inputs.
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.item_obj_id,p.objid,p.npcid,p.name,p.lvl,p.hp,p.mp,p.exp,"
                            + "p.lawful,i.item_id FROM character_pets p "
                            + "JOIN character_items i ON i.id=p.item_obj_id LIMIT 0");
                 ResultSet ignored = statement.executeQuery()) {
                // Metadata-only validation for source pet/collar identity.
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    sourceInventorySql() + " LIMIT 0")) {
                statement.setLong(1, 0);
                statement.setLong(2, 0);
                statement.setLong(3, 0);
                try (ResultSet ignored = statement.executeQuery()) {
                    // Metadata-only validation for the complete selected-source
                    // inventory/template projection.
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id,char_id,name,locx,locy,mapid "
                            + "FROM character_teleport WHERE char_id=? LIMIT 0")) {
                statement.setLong(1, 0);
                try (ResultSet ignored = statement.executeQuery()) {
                    // Exact retained bookmark projection.
                }
            }
            try (PreparedStatement statement = SourceBuffRead.healthStatement(connection,
                    "SELECT b.char_obj_id,b.skill_id,b.remaining_time,b.poly_id,"
                            + "b.snapshot_version,m.completed_version "
                            + "FROM character_buff b LEFT JOIN character_buff_snapshot m "
                            + "ON m.char_obj_id=b.char_obj_id WHERE b.char_obj_id=? LIMIT 0")) {
                statement.setLong(1, 0);
                try (ResultSet ignored = statement.executeQuery()) {
                    // Exact retained crash-safe buff snapshot projection.
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT BaseNpcId,CollarId,ChaCost,HpUpMin,HpUpMax,MpUpMin,MpUpMax,"
                            + "MinEvoLevel,EvolutionItemId,NpcIdForEvolving FROM "
                            + resourceTable("pettypes") + " LIMIT 0");
                 ResultSet ignored = statement.executeQuery()) {
                // Metadata-only validation for retained pet growth inputs.
            }
        }
        checkTable(accountUrl, accountLogin, accountPassword,
                exactResourceTable("角色_精神成長"));
        checkTable(accountUrl, accountLogin, accountPassword,
                exactResourceTable("角色_體質成長"));
        checkTable(accountUrl, accountLogin, accountPassword,
                exactResourceTable("角色_力量成長"));
        checkTable(accountUrl, accountLogin, accountPassword,
                exactResourceTable("角色_敏捷成長"));
        checkTable(accountUrl, accountLogin, accountPassword, "clan_data");
        checkTable(accountUrl, accountLogin, accountPassword, "server_城堡");
        checkTable(accountUrl, accountLogin, accountPassword, "server_盟屋_拍賣");
        checkTable(accountUrl, accountLogin, accountPassword, "server_盟屋_佈告");
        if (repositoryMode.equals("lito_mysql")) {
            checkTable(accountUrl, accountLogin, accountPassword, "lito_character_origin");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_account_origin");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_item_origin");
            checkTable(accountUrl, accountLogin, accountPassword,
                    "lito_private_inventory");
            checkTable(accountUrl, accountLogin, accountPassword,
                    "lito_warehouse_item_origin");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_buddy_group");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_buddy_metadata");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_clan_metadata");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_castle_metadata");
            checkTable(accountUrl, accountLogin, accountPassword,
                    "lito_castle_security_settlement");
            checkTable(accountUrl, accountLogin, accountPassword,
                    "lito_house_auction_metadata");
            checkTable(accountUrl, accountLogin, accountPassword, "lito_board_topic");
            if (classicSourceId == 0) {
                checkTable(accountUrl, accountLogin, accountPassword,
                        "lito_tournament_profile");
            } else {
                checkTable(accountUrl, accountLogin, accountPassword,
                        resourceTable("lito_tournament_character_record"));
            }
            checkTable(accountUrl, accountLogin, accountPassword,
                    "lito_hotel_lease_key");
            checkTable(accountUrl, accountLogin, accountPassword,
                    "lito_water_dragon_pass");
            try (Connection connection = connection(
                    accountUrl, accountLogin, accountPassword)) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT char_obj_id,group_id,group_name "
                                + "FROM lito_buddy_group LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Exact LiTo buddy group projection.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT char_obj_id,buddy_id,buddy_name,target_source,group_id,"
                                + "relationship_style,palette_offset,origin "
                                + "FROM lito_buddy_metadata LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Exact LiTo buddy metadata projection.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT item_object_id,code,sender,receiver,date,template_id,"
                                + "subject,content,isCheck FROM character_letter LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Exact native physical-letter projection. character_mail is
                    // deliberately a separate protocol and is never touched here.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT clan_id,lito_owned,destroy_pending,snapshot_version "
                                + "FROM lito_clan_metadata LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // LiTo-only metadata attached to a native clan_data identity.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT castle_id,security_enabled,mercenary_0,mercenary_1,"
                                + "mercenary_2,mercenary_3,snapshot_version "
                                + "FROM lito_castle_metadata LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // LiTo-only state attached to native server_城堡 rows.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT castle_id,war_start_millis,treasury_before,"
                                + "charge_adena,charged_adena,prepared,completed "
                                + "FROM lito_castle_security_settlement LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Crash-resumable, once-per-war castle-security ledger.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT house_id,snapshot_version "
                                + "FROM lito_house_auction_metadata LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Optimistic versions attached to native house auctions.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id,name,date,title,content "
                                + "FROM `server_盟屋_佈告` LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Exact retained native board projection shared with TW.
                }
                String tournamentProjection = classicSourceId == 0
                        ? "SELECT char_obj_id,char_name,profile_state,snapshot_version "
                                + "FROM lito_tournament_profile LIMIT 0"
                        : "SELECT source_server_id,char_obj_id,char_name,wins,losses,"
                                + "disconnects,ranking_points,profile_state,"
                                + "snapshot_version FROM "
                                + resourceTable("lito_tournament_character_record")
                                + " LIMIT 0";
                try (PreparedStatement statement = connection.prepareStatement(
                        tournamentProjection);
                     ResultSet ignored = statement.executeQuery()) {
                    // Versioned, complete, source-qualified Tournament result.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT char_obj_id,inventory_state,snapshot_version "
                                + "FROM lito_private_inventory LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Versioned LiTo-only owned-inventory projection.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT item_obj_id,show_id,npc_id,room_number,meeting_hall,"
                                + "renter_obj_id,renter_identity,expires_at,released "
                                + "FROM lito_hotel_lease_key LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Source-qualified durable classic hotel key/lease relation.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT item_obj_id,show_id,room_number,renter_obj_id,"
                                + "renter_identity,expires_at "
                                + "FROM lito_water_dragon_pass LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Source-qualified durable Water Dragon pass relation.
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT lito_event_key FROM `server_紀錄_獎勵` LIMIT 0");
                     ResultSet ignored = statement.executeQuery()) {
                    // Nullable unique key makes LiTo event rewards idempotent while
                    // native/TW producers continue to write their original columns.
                }
                for (GameStateStore.WarehouseScope scope
                        : GameStateStore.WarehouseScope.values()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            warehouseInventorySql(scope) + " LIMIT 0")) {
                        for (int index = 1; index <= 3; index++) {
                            statement.setString(index, "");
                        }
                        try (ResultSet ignored = statement.executeQuery()) {
                            // Exact shared warehouse/template/ownership schema.
                        }
                    }
                }
            }
        }
    }

    @Override public void migrateLitoExtensions() throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) return;
        try (Connection connection = DriverManager.getConnection(
                     accountUrl, accountLogin, accountPassword);
             Statement statement = connection.createStatement()) {
            connection.setReadOnly(false);
            if (!schemaColumnExists(connection,
                    "server_紀錄_獎勵", "lito_event_key")) {
                statement.executeUpdate("ALTER TABLE `server_紀錄_獎勵` "
                        + "ADD COLUMN lito_event_key varchar(96) CHARACTER SET utf8 "
                        + "COLLATE utf8_general_ci NULL DEFAULT NULL AFTER ip");
            }
            if (!schemaIndexExists(connection,
                    "server_紀錄_獎勵", "uq_lito_reward_event")) {
                statement.executeUpdate("ALTER TABLE `server_紀錄_獎勵` "
                        + "ADD UNIQUE KEY uq_lito_reward_event (lito_event_key)");
            }
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_buddy_group ("
                    + "char_obj_id int(10) unsigned NOT NULL,"
                    + "group_id smallint(5) unsigned NOT NULL,"
                    + "group_name varchar(45) NOT NULL,"
                    + "PRIMARY KEY (char_obj_id,group_id),"
                    + "UNIQUE KEY uq_lito_buddy_group_name (char_obj_id,group_name)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_buddy_metadata ("
                    + "char_obj_id int(10) unsigned NOT NULL,"
                    + "buddy_id int(10) unsigned NOT NULL DEFAULT 0,"
                    + "buddy_name varchar(45) NOT NULL,"
                    + "target_source varchar(45) NOT NULL DEFAULT '',"
                    + "group_id smallint(5) unsigned NOT NULL DEFAULT 1,"
                    + "relationship_style tinyint(3) unsigned NOT NULL DEFAULT 1,"
                    + "palette_offset tinyint(3) unsigned NOT NULL DEFAULT 1,"
                    + "origin varchar(45) NOT NULL DEFAULT '',"
                    + "PRIMARY KEY (char_obj_id,buddy_name),"
                    + "KEY idx_lito_buddy_target (buddy_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_clan_metadata ("
                    + "clan_id int(10) unsigned NOT NULL,"
                    + "lito_owned tinyint(1) unsigned NOT NULL DEFAULT 0,"
                    + "destroy_pending tinyint(1) unsigned NOT NULL DEFAULT 0,"
                    + "snapshot_version int(10) unsigned NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (clan_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_castle_metadata ("
                    + "castle_id tinyint(3) unsigned NOT NULL,"
                    + "security_enabled tinyint(1) unsigned NOT NULL DEFAULT 0,"
                    + "mercenary_0 int(10) unsigned NOT NULL DEFAULT 0,"
                    + "mercenary_1 int(10) unsigned NOT NULL DEFAULT 0,"
                    + "mercenary_2 int(10) unsigned NOT NULL DEFAULT 0,"
                    + "mercenary_3 int(10) unsigned NOT NULL DEFAULT 0,"
                    + "snapshot_version int(10) unsigned NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (castle_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS lito_castle_security_settlement ("
                            + "castle_id tinyint(3) unsigned NOT NULL,"
                            + "war_start_millis bigint(20) unsigned NOT NULL,"
                            + "treasury_before int(10) unsigned NOT NULL,"
                            + "charge_adena int(10) unsigned NOT NULL,"
                            + "charged_adena int(10) unsigned NOT NULL DEFAULT 0,"
                            + "prepared tinyint(1) unsigned NOT NULL DEFAULT 0,"
                            + "completed tinyint(1) unsigned NOT NULL DEFAULT 0,"
                            + "settled_at datetime NOT NULL,"
                            + "PRIMARY KEY (castle_id,war_start_millis)"
                            + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_house_auction_metadata ("
                    + "house_id int(10) unsigned NOT NULL,"
                    + "snapshot_version int(10) unsigned NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (house_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_board_topic ("
                    + "topic_id int(10) unsigned NOT NULL,"
                    + "author_obj_id int(10) unsigned NOT NULL,"
                    + "author_name varchar(45) NOT NULL,"
                    + "post_date char(8) NOT NULL,"
                    + "title varchar(40) NOT NULL,"
                    + "content text NOT NULL,"
                    + "created_at datetime NOT NULL,"
                    + "PRIMARY KEY (topic_id),"
                    + "KEY idx_lito_board_author (author_obj_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_tournament_profile ("
                    + "char_obj_id int(10) unsigned NOT NULL,"
                    + "char_name varchar(45) NOT NULL,"
                    + "profile_state mediumtext NOT NULL,"
                    + "snapshot_version int(10) unsigned NOT NULL DEFAULT 1,"
                    + "updated_at datetime NOT NULL,"
                    + "PRIMARY KEY (char_obj_id),"
                    + "UNIQUE KEY uq_lito_tournament_character (char_name)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS "
                    + resourceTable("lito_tournament_character_record") + " ("
                    + "source_server_id smallint(5) unsigned NOT NULL,"
                    + "char_obj_id int(10) unsigned NOT NULL,"
                    + "char_name varchar(45) NOT NULL,"
                    + "wins int(10) unsigned NOT NULL DEFAULT 0,"
                    + "losses int(10) unsigned NOT NULL DEFAULT 0,"
                    + "disconnects int(10) unsigned NOT NULL DEFAULT 0,"
                    + "ranking_points bigint(20) unsigned NOT NULL DEFAULT 0,"
                    + "profile_state mediumtext NOT NULL,"
                    + "snapshot_version int(10) unsigned NOT NULL DEFAULT 1,"
                    + "updated_at datetime NOT NULL,"
                    + "PRIMARY KEY (source_server_id,char_obj_id),"
                    + "UNIQUE KEY uq_lito_tournament_source_name "
                    + "(source_server_id,char_name)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_private_inventory ("
                    + "char_obj_id int(10) unsigned NOT NULL,"
                    + "inventory_state mediumtext NOT NULL,"
                    + "snapshot_version int(10) unsigned NOT NULL DEFAULT 1,"
                    + "updated_at datetime NOT NULL,"
                    + "PRIMARY KEY (char_obj_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_hotel_lease_key ("
                    + "item_obj_id int(10) unsigned NOT NULL,"
                    + "show_id int(10) unsigned NOT NULL,"
                    + "npc_id int(10) unsigned NOT NULL,"
                    + "room_number tinyint(3) unsigned NOT NULL,"
                    + "meeting_hall tinyint(1) unsigned NOT NULL DEFAULT 0,"
                    + "renter_obj_id int(10) unsigned NOT NULL,"
                    + "renter_identity varchar(95) NOT NULL,"
                    + "expires_at bigint(20) unsigned NOT NULL,"
                    + "released tinyint(1) unsigned NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (item_obj_id),"
                    + "KEY idx_lito_hotel_show (show_id),"
                    + "KEY idx_lito_hotel_occupancy (npc_id,released,expires_at),"
                    + "KEY idx_lito_hotel_renter (renter_obj_id)"
                    + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_water_dragon_pass ("
                    + "item_obj_id int(10) unsigned NOT NULL,"
                    + "show_id int(10) unsigned NOT NULL,"
                    + "room_number tinyint(3) unsigned NOT NULL,"
                    + "renter_obj_id int(10) unsigned NOT NULL,"
                    + "renter_identity varchar(95) NOT NULL,"
                    + "expires_at bigint(20) unsigned NOT NULL,"
                    + "PRIMARY KEY (item_obj_id),"
                    + "UNIQUE KEY uq_lito_water_dragon_show (show_id),"
                    + "KEY idx_lito_water_dragon_occupancy (room_number,expires_at),"
                     + "KEY idx_lito_water_dragon_renter (renter_obj_id,expires_at)"
                     + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
            migrateBattlePlayerSpawns(connection, statement);
        }
    }

    private void migrateBattlePlayerSpawns(Connection connection, Statement statement)
            throws SQLException {
        String table = resourceTable("lito_battle_player_spawn");
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "mode_id tinyint(3) unsigned NOT NULL,"
                + "map_id smallint(5) unsigned NOT NULL,"
                + "spawn_index tinyint(3) unsigned NOT NULL,"
                + "x smallint(5) unsigned NOT NULL,"
                + "y smallint(5) unsigned NOT NULL,"
                + "enabled tinyint(1) unsigned NOT NULL DEFAULT 1,"
                + "note varchar(255) NOT NULL DEFAULT '',"
                + "PRIMARY KEY (mode_id,map_id,spawn_index),"
                + "KEY idx_lito_battle_player_spawn_map (map_id,enabled)"
                + ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
        String insert = "INSERT IGNORE INTO " + table
                + " (mode_id,map_id,spawn_index,x,y,enabled,note)"
                + " VALUES (?,?,?,?,?,1,?)";
        try (PreparedStatement seed = connection.prepareStatement(insert)) {
            for (RepositoryBattlePlayerSpawn row
                    : BattleControlCatalog.defaultPlayerSpawns()) {
                seed.setInt(1, row.modeId());
                seed.setInt(2, row.mapId());
                seed.setInt(3, row.spawnIndex());
                seed.setInt(4, row.x());
                seed.setInt(5, row.y());
                seed.setString(6, "built-in validated default");
                seed.addBatch();
            }
            seed.executeBatch();
        }
    }

    private static boolean schemaColumnExists(Connection connection,
            String table, String column) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? "
                        + "AND COLUMN_NAME=? LIMIT 1")) {
            query.setString(1, table);
            query.setString(2, column);
            try (ResultSet row = query.executeQuery()) {
                return row.next();
            }
        }
    }

    private static boolean schemaIndexExists(Connection connection,
            String table, String index) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? "
                        + "AND INDEX_NAME=? LIMIT 1")) {
            query.setString(1, table);
            query.setString(2, index);
            try (ResultSet row = query.executeQuery()) {
                return row.next();
            }
        }
    }

    @Override public AuthResult authenticate(String account, String suppliedPassword) throws SQLException {
        String sql = "SELECT login, password, banned FROM accounts WHERE login = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return AuthResult.rejected(account);
                boolean banned = result.getInt("banned") != 0;
                boolean accepted = !banned && suppliedPassword.equals(result.getString("password"));
                return new AuthResult(accepted, banned, result.getString("login"));
            }
        }
    }

    @Override public int accessLevel(String account) throws SQLException {
        if (account == null || account.isBlank()) return 0;
        String sql = "SELECT access_level FROM accounts WHERE login = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return 0;
                return Math.max(0, Math.min(200, result.getInt("access_level")));
            }
        }
    }

    @Override public GmCommandDefinition gmCommand(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        String sql = "SELECT name,access_level,class_name,note,system FROM "
                + resourceTable("commands") + " WHERE name=? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new GmCommandDefinition(
                        result.getString("name"),
                        Math.max(0, Math.min(200, result.getInt("access_level"))),
                        result.getString("class_name"), result.getString("note"),
                        result.getInt("system") != 0);
            }
        }
    }

    @Override public List<RepositoryTournamentShopOffer> tournamentShopOffers()
            throws SQLException {
        String sql = "SELECT item_id,sort_order,buy_price,sell_price FROM "
                + resourceTable("lito_tournament_shop")
                + " WHERE enabled<>0 ORDER BY sort_order,item_id";
        List<RepositoryTournamentShopOffer> offers = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                offers.add(new RepositoryTournamentShopOffer(
                        result.getLong("item_id"), result.getInt("sort_order"),
                        result.getLong("buy_price"), result.getLong("sell_price")));
            }
        }
        return List.copyOf(offers);
    }

    @Override public RepositoryBattleConfiguration battleConfiguration()
            throws SQLException {
        List<RepositoryBattleRule> rules = new ArrayList<>();
        List<RepositoryBattleObject> objects = new ArrayList<>();
        List<RepositoryBattleSpawn> spawns = new ArrayList<>();
        List<RepositoryBattlePlayerSpawn> playerSpawns = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT mode_id,map_id,enabled,time_limit_seconds,goal,minimum_players FROM "
                            + resourceTable("lito_battle_rule")
                            + " ORDER BY mode_id,map_id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rules.add(new RepositoryBattleRule(
                            result.getInt("mode_id"), result.getInt("map_id"),
                            result.getInt("enabled") != 0,
                            result.getInt("time_limit_seconds"),
                            result.getInt("goal"), result.getInt("minimum_players")));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT mode_id,map_id,role,x,y,gfx,maximum_hp FROM "
                            + resourceTable("lito_battle_object")
                            + " WHERE enabled<>0 ORDER BY mode_id,map_id,role");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    objects.add(new RepositoryBattleObject(
                            result.getInt("mode_id"), result.getInt("map_id"),
                            result.getString("role"), result.getInt("x"),
                            result.getInt("y"), result.getInt("gfx"),
                            result.getInt("maximum_hp")));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT mode_id,map_id,wave,sort_order,npc_id,quantity,x,y FROM "
                            + resourceTable("lito_battle_spawn")
                            + " WHERE enabled<>0 ORDER BY mode_id,map_id,wave,sort_order");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Object x = result.getObject("x");
                    Object y = result.getObject("y");
                    spawns.add(new RepositoryBattleSpawn(
                            result.getInt("mode_id"), result.getInt("map_id"),
                            result.getInt("wave"), result.getInt("sort_order"),
                            result.getInt("npc_id"), result.getInt("quantity"),
                            x == null ? null : ((Number) x).intValue(),
                            y == null ? null : ((Number) y).intValue()));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT mode_id,map_id,spawn_index,x,y FROM "
                            + resourceTable("lito_battle_player_spawn")
                            + " WHERE enabled<>0 ORDER BY mode_id,map_id,spawn_index");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    playerSpawns.add(new RepositoryBattlePlayerSpawn(
                            result.getInt("mode_id"), result.getInt("map_id"),
                            result.getInt("spawn_index"), result.getInt("x"),
                            result.getInt("y")));
                }
            }
        }
        return new RepositoryBattleConfiguration(rules, objects, spawns, playerSpawns);
    }

    @Override public boolean accountExists(String account) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE login = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override public AccountMutationResult createAccount(String account, String password) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return accountExists(account)
                    ? AccountMutationResult.EXISTS : AccountMutationResult.READ_ONLY;
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES accounts WRITE, lito_account_origin WRITE");
            boolean accountInserted = false;
            try {
                try (PreparedStatement existing = connection.prepareStatement(
                        "SELECT 1 FROM accounts WHERE login=? LIMIT 1")) {
                    existing.setString(1, account);
                    try (ResultSet row = existing.executeQuery()) {
                        if (row.next()) return AccountMutationResult.EXISTS;
                    }
                }
                try (PreparedStatement stale = connection.prepareStatement(
                        "DELETE FROM lito_account_origin WHERE login=?")) {
                    stale.setString(1, account);
                    stale.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO accounts (access_level,server_no,login,password,lastactive,"
                                + "ip,host,banned,character_slot,spw,warehouse,ezpay) "
                                + "VALUES (0,0,?,?,NOW(),'','',0,0,?,-256,0)")) {
                    insert.setString(1, account);
                    insert.setString(2, password);
                    insert.setString(3, "未建立超級密碼");
                    insert.executeUpdate();
                    accountInserted = true;
                }
                try (PreparedStatement marker = connection.prepareStatement(
                        "INSERT INTO lito_account_origin (login,created_at) VALUES (?,NOW())")) {
                    marker.setString(1, account);
                    marker.executeUpdate();
                }
                return AccountMutationResult.SUCCESS;
            } catch (SQLException failure) {
                if (accountInserted) {
                    try (PreparedStatement cleanup = connection.prepareStatement(
                            "DELETE FROM accounts WHERE login=?")) {
                        cleanup.setString(1, account);
                        cleanup.executeUpdate();
                    } catch (SQLException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public boolean deleteLitoAccount(String account) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) return false;
        String sql = "DELETE accounts,lito_account_origin FROM accounts "
                + "JOIN lito_account_origin ON lito_account_origin.login=accounts.login "
                + "LEFT JOIN characters ON characters.account_name=accounts.login "
                + "WHERE accounts.login=? AND characters.objid IS NULL";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES accounts WRITE, lito_account_origin WRITE, characters READ");
            try (PreparedStatement delete = connection.prepareStatement(sql)) {
                delete.setString(1, account);
                return delete.executeUpdate() >= 2;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public AccountMutationResult changePassword(
            String account, String oldPassword, String newPassword) throws SQLException {
        String sql = "SELECT password FROM accounts WHERE login = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return AccountMutationResult.NOT_FOUND;
                if (!oldPassword.equals(result.getString("password"))) {
                    return AccountMutationResult.WRONG_PASSWORD;
                }
                if (readOnly || !repositoryMode.equals("lito_mysql")) {
                    return AccountMutationResult.READ_ONLY;
                }
            }
        }
        String update = "UPDATE accounts SET password=?,lastactive=NOW() "
                + "WHERE login=? AND password=?";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, newPassword);
            statement.setString(2, account);
            statement.setString(3, oldPassword);
            return statement.executeUpdate() == 1
                    ? AccountMutationResult.SUCCESS : AccountMutationResult.WRONG_PASSWORD;
        }
    }

    @Override public AccountMutationResult registerQuiz(
            String account, String oldPassword, String newPassword, List<String> quizFields)
            throws SQLException {
        String sql = "SELECT password FROM accounts WHERE login = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return AccountMutationResult.NOT_FOUND;
                if (!oldPassword.equals(result.getString("password"))) {
                    return AccountMutationResult.WRONG_PASSWORD;
                }
                // The selected TW account database has no recovered native
                // question/answer columns and remains an immutable source.
                // Never synthesize schema changes or an UPDATE path here.
                return AccountMutationResult.READ_ONLY;
            }
        }
    }

    @Override public AccountMutationResult changeAccountInfo(
            String account, int type, List<String> fields) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE login = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return AccountMutationResult.NOT_FOUND;
                // The original TW account database remains immutable.  Profile
                // changes are supported only by the isolated memory repository.
                return AccountMutationResult.READ_ONLY;
            }
        }
    }

    @Override public List<CharacterSummary> characters(String account) throws SQLException {
        String sql = """
                SELECT c.char_name, c.Clanname, c.Type, c.Sex, c.level,
                       c.MaxHp, c.MaxMp, c.Ac,
                       c.Str, c.Con, c.Dex, c.Cha, c.Intel, c.Wis,
                       c.OriginalInt, c.OriginalWis, c.OriginalStr, c.OriginalDex,
                       objid, Exp, Lawful, PKcount, LastPk,
                       PkCountForElf, LastPkForElf, Class,
                       LocX, LocY, MapID, c.OriginalCon, c.Food, c.ElfAttr,
                       COALESCE(h.`回血`, 1) AS con_hpr_maximum
                  FROM characters c
                  LEFT JOIN %s h ON h.id = LEAST(GREATEST(c.Con, 1), 50)
                 WHERE c.account_name = ? AND c.Banned = 0
                 ORDER BY c.char_name
                 LIMIT 8
                """.formatted(exactResourceTable("角色_體質成長"));
        List<CharacterSummary> result = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    java.sql.Timestamp lastPk = row.getTimestamp("LastPk");
                    java.sql.Timestamp lastPkForElf =
                            row.getTimestamp("LastPkForElf");
                    result.add(new CharacterSummary(
                            row.getString("char_name"), row.getString("Clanname"),
                            row.getInt("Type"), row.getInt("Sex"), row.getInt("level"),
                            row.getInt("MaxHp"), row.getInt("MaxMp"), row.getInt("Ac"),
                            row.getInt("Str"), row.getInt("Con"), row.getInt("Dex"),
                            row.getInt("Cha"), row.getInt("Intel"), row.getInt("Wis"),
                            row.getInt("OriginalInt"), row.getInt("OriginalWis"),
                            row.getLong("objid"), row.getLong("Exp"), row.getLong("Lawful"),
                            row.getInt("PKcount"),
                            lastPk == null ? 0
                                    : Math.max(0, lastPk.getTime() / 1_000L),
                            row.getInt("PkCountForElf"),
                            lastPkForElf == null ? 0
                                    : Math.max(0, lastPkForElf.getTime() / 1_000L),
                            row.getLong("Class"),
                            row.getLong("LocX"), row.getLong("LocY"),
                            row.getLong("MapID"), row.getInt("OriginalCon"),
                            row.getInt("Food"), Math.max(1, row.getInt("con_hpr_maximum")),
                            row.getInt("OriginalStr"), row.getInt("OriginalDex"),
                            row.getInt("ElfAttr")));
                }
            }
        }
        return result;
    }

    @Override public RepositoryCharacterMutation createLitoCharacter(
            String account, Protocol.CustomCharacterRequest request) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return RepositoryCharacterMutation.readOnly();
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
            Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES server_info WRITE, characters WRITE, "
                    + "character_other WRITE, character_items WRITE, "
                    + "character_items_time WRITE, character_pets READ, "
                    + "lito_character_origin WRITE, lito_item_origin WRITE");
            long objectId = 0;
            try {
                try (PreparedStatement duplicate = connection.prepareStatement(
                        "SELECT 1 FROM characters WHERE char_name=? LIMIT 1")) {
                    duplicate.setString(1, request.name());
                    try (ResultSet row = duplicate.executeQuery()) {
                        if (row.next()) return new RepositoryCharacterMutation(
                                RepositoryCharacterMutationStatus.EXISTS, null);
                    }
                }
                try (PreparedStatement count = connection.prepareStatement(
                        "SELECT COUNT(*) FROM characters WHERE account_name=? AND Banned=0")) {
                    count.setString(1, account);
                    try (ResultSet row = count.executeQuery()) {
                        row.next();
                        if (row.getInt(1) >= 8) return new RepositoryCharacterMutation(
                                RepositoryCharacterMutationStatus.ACCOUNT_LIMIT, null);
                    }
                }
                try (Statement allocate = connection.createStatement()) {
                    int changed = allocate.executeUpdate(
                            "UPDATE server_info SET maxid=LAST_INSERT_ID("
                                    + "GREATEST(COALESCE(maxid,0),"
                                    + "COALESCE(minid,10000),"
                                    + "COALESCE((SELECT MAX(objid) FROM characters),0),"
                                    + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                                    + "COALESCE((SELECT MAX(objid) FROM character_pets),0))+1) "
                                    + "WHERE id=1");
                    if (changed != 1) throw new SQLException("server_info id=1 is missing");
                    try (ResultSet row = allocate.executeQuery("SELECT LAST_INSERT_ID()")) {
                        if (!row.next()) throw new SQLException("object id allocation failed");
                        objectId = row.getLong(1);
                    }
                }
                List<Integer> attributes = request.attributes();
                int strength = attributes.get(0);
                int dexterity = attributes.get(1);
                int constitution = attributes.get(2);
                int wisdom = attributes.get(3);
                int charisma = attributes.get(4);
                int intelligence = attributes.get(5);
                int classId = BattleClassGfx.forCharacter(
                        request.characterClass(), request.sex());
                String sql = "INSERT INTO characters (account_name,char_name,objid,level,"
                        + "HighLevel,MaxHp,CurHp,MaxMp,CurMp,Ac,Str,Con,Dex,Cha,Intel,Wis,"
                        + "Class,Sex,Type,Heading,LocX,LocY,MapID,Food,OriginalStr,OriginalCon,"
                        + "OriginalDex,OriginalCha,OriginalInt,OriginalWis,CreateTime) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement insert = connection.prepareStatement(sql)) {
                    int index = 1;
                    insert.setString(index++, account);
                    insert.setString(index++, request.name());
                    insert.setLong(index++, objectId);
                    insert.setInt(index++, 1);
                    insert.setInt(index++, 1);
                    insert.setInt(index++, 50);
                    insert.setInt(index++, 50);
                    insert.setInt(index++, 10);
                    insert.setInt(index++, 10);
                    insert.setInt(index++, 10);
                    insert.setInt(index++, strength);
                    insert.setInt(index++, constitution);
                    insert.setInt(index++, dexterity);
                    insert.setInt(index++, charisma);
                    insert.setInt(index++, intelligence);
                    insert.setInt(index++, wisdom);
                    insert.setInt(index++, classId);
                    insert.setInt(index++, request.sex());
                    insert.setInt(index++, request.characterClass());
                    insert.setInt(index++, 4);
                    insert.setInt(index++, 32600);
                    insert.setInt(index++, 32700);
                    insert.setInt(index++, 4);
                    insert.setInt(index++, 40);
                    insert.setInt(index++, strength);
                    insert.setInt(index++, constitution);
                    insert.setInt(index++, dexterity);
                    insert.setInt(index++, charisma);
                    insert.setInt(index++, intelligence);
                    insert.setInt(index++, wisdom);
                    insert.setInt(index, Integer.parseInt(
                            java.time.LocalDate.now().format(
                                    java.time.format.DateTimeFormatter.BASIC_ISO_DATE)));
                    insert.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO character_other (char_obj_id) VALUES (?)")) {
                    insert.setLong(1, objectId);
                    insert.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO lito_character_origin "
                                + "(char_obj_id,account_name,char_name,created_at) "
                                + "VALUES (?,?,?,NOW())")) {
                    insert.setLong(1, objectId);
                    insert.setString(2, account);
                    insert.setString(3, request.name());
                    insert.executeUpdate();
                }
                long helperObjectId;
                try (Statement allocate = connection.createStatement()) {
                    int changed = allocate.executeUpdate(
                            "UPDATE server_info SET maxid=LAST_INSERT_ID("
                                    + "GREATEST(COALESCE(maxid,0),"
                                    + "COALESCE(minid,10000),"
                                    + "COALESCE((SELECT MAX(objid) FROM characters),0),"
                                    + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                                    + "COALESCE((SELECT MAX(objid) FROM character_pets),0))+1) "
                                    + "WHERE id=1");
                    if (changed != 1) throw new SQLException("server_info id=1 is missing");
                    try (ResultSet row = allocate.executeQuery("SELECT LAST_INSERT_ID()")) {
                        if (!row.next()) throw new SQLException("helper item id allocation failed");
                        helperObjectId = row.getLong(1);
                    }
                }
                InventoryCatalog.Definition helper = InventoryCatalog.require(
                        InventoryCatalog.PC_ACTION_HELPER);
                RuntimeInventory.Item helperItem = new RuntimeInventory.Item(
                        helperObjectId, helper, 1, false, 0, 0, false,
                        helper.blessing(), false, false, helper.initialChargeCount());
                insertCharacterItem(connection, objectId, helperObjectId, helperItem, null);
                upsertCharacterItemOrigin(connection, helperObjectId, objectId, helperItem);
                CharacterSummary character = new CharacterSummary(
                        request.name(), "", request.characterClass(), request.sex(),
                        1, 50, 10, 10, strength, constitution, dexterity,
                        charisma, intelligence, wisdom, intelligence, wisdom,
                        objectId, 0, 0, 0, 0, 0, 0,
                        classId, 32600, 32700, 4, constitution, 40, 1,
                        strength, dexterity, 0);
                return new RepositoryCharacterMutation(
                        RepositoryCharacterMutationStatus.SUCCESS, character);
            } catch (SQLException | RuntimeException failure) {
                if (objectId != 0) {
                    try (Statement cleanup = connection.createStatement()) {
                        cleanup.executeUpdate("DELETE FROM character_other WHERE char_obj_id="
                                + objectId);
                        cleanup.executeUpdate("DELETE FROM lito_character_origin WHERE char_obj_id="
                                + objectId);
                        cleanup.executeUpdate("DELETE FROM lito_item_origin WHERE char_obj_id="
                                + objectId);
                        cleanup.executeUpdate("DELETE FROM character_items WHERE char_id="
                                + objectId);
                        cleanup.executeUpdate("DELETE FROM characters WHERE objid=" + objectId);
                    } catch (SQLException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryCharacterMutation deleteLitoCharacter(
            String account, String characterName) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return RepositoryCharacterMutation.readOnly();
        }
        String tournamentTable = usesSharedTournamentTable()
                ? tournamentRecordTable() : "lito_tournament_profile";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE, character_other WRITE, "
                    + "character_skills WRITE, character_quests WRITE, "
                    + "character_items WRITE, character_items_time WRITE, "
                    + "character_pets WRITE, character_teleport WRITE, "
                     + "character_buff WRITE, character_buff_snapshot WRITE, "
                     + "character_buddys WRITE, lito_buddy_group WRITE, "
                     + "lito_buddy_metadata WRITE, lito_character_origin WRITE, "
                     + "lito_item_origin WRITE, character_letter WRITE, "
                     + "`server_盟屋_佈告` WRITE, lito_board_topic WRITE, "
                     + tournamentTable + " WRITE, "
                     + "lito_private_inventory WRITE, "
                     + "lito_hotel_lease_key WRITE, "
                     + "lito_water_dragon_pass WRITE, "
                     + "clan_data READ");
            try {
                long objectId;
                try (PreparedStatement owned = connection.prepareStatement(
                        "SELECT char_obj_id FROM lito_character_origin "
                                + "WHERE account_name=? AND char_name=? LIMIT 1")) {
                    owned.setString(1, account);
                    owned.setString(2, characterName);
                    try (ResultSet row = owned.executeQuery()) {
                        if (!row.next()) return new RepositoryCharacterMutation(
                                RepositoryCharacterMutationStatus.NOT_OWNED, null);
                        objectId = row.getLong(1);
                    }
                }
                try (PreparedStatement leader = connection.prepareStatement(
                        "SELECT 1 FROM clan_data WHERE leader_id=? LIMIT 1")) {
                    leader.setLong(1, objectId);
                    try (ResultSet row = leader.executeQuery()) {
                        if (row.next()) return new RepositoryCharacterMutation(
                                RepositoryCharacterMutationStatus.CLAN_OWNER, null);
                    }
                }
                List<Long> itemObjectIds = new ArrayList<>();
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT id FROM character_items WHERE char_id=?")) {
                    query.setLong(1, objectId);
                    try (ResultSet row = query.executeQuery()) {
                        while (row.next()) itemObjectIds.add(row.getLong(1));
                    }
                }
                // Native deletion removes letters both received from and sent by
                // this character. Remove their exact backing items as well so no
                // 49016/49017 object survives without a body row.
                List<Long> relatedLetterObjectIds = new ArrayList<>();
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT item_object_id FROM character_letter "
                                + "WHERE receiver=? OR sender=?")) {
                    query.setString(1, characterName);
                    query.setString(2, characterName);
                    try (ResultSet row = query.executeQuery()) {
                        while (row.next()) relatedLetterObjectIds.add(row.getLong(1));
                    }
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_letter WHERE receiver=? OR sender=?")) {
                    delete.setString(1, characterName);
                    delete.setString(2, characterName);
                    delete.executeUpdate();
                }
                // Deleting the renter must free every transferred copy of the
                // lease, while keys that are themselves being destroyed must
                // not leave an orphaned object relation behind.
                try (PreparedStatement release = connection.prepareStatement(
                        "UPDATE lito_hotel_lease_key SET released=1 "
                                + "WHERE renter_obj_id=?")) {
                    release.setLong(1, objectId);
                    release.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_hotel_lease_key WHERE item_obj_id=?")) {
                    for (long itemObjectId : itemObjectIds) {
                        delete.setLong(1, itemObjectId);
                        delete.addBatch();
                    }
                    delete.executeBatch();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_water_dragon_pass WHERE renter_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement deleteTime = connection.prepareStatement(
                             "DELETE FROM character_items_time WHERE itemr_obj_id=?");
                     PreparedStatement deletePet = connection.prepareStatement(
                             "DELETE FROM character_pets WHERE item_obj_id=?");
                     PreparedStatement deleteItem = connection.prepareStatement(
                             "DELETE FROM character_items WHERE id=?");
                     PreparedStatement deleteOrigin = connection.prepareStatement(
                             "DELETE FROM lito_item_origin WHERE item_obj_id=?")) {
                    for (long letterObjectId : relatedLetterObjectIds) {
                        deleteTime.setLong(1, letterObjectId);
                        deleteTime.addBatch();
                        deletePet.setLong(1, letterObjectId);
                        deletePet.addBatch();
                        deleteItem.setLong(1, letterObjectId);
                        deleteItem.addBatch();
                        deleteOrigin.setLong(1, letterObjectId);
                        deleteOrigin.addBatch();
                    }
                    deleteTime.executeBatch();
                    deletePet.executeBatch();
                    deleteItem.executeBatch();
                    deleteOrigin.executeBatch();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
                    for (long itemObjectId : itemObjectIds) {
                        delete.setLong(1, itemObjectId);
                        delete.addBatch();
                    }
                    delete.executeBatch();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_pets WHERE item_obj_id=?")) {
                    for (long itemObjectId : itemObjectIds) {
                        delete.setLong(1, itemObjectId);
                        delete.addBatch();
                    }
                    delete.executeBatch();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_items WHERE char_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_item_origin WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_private_inventory WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_skills WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_quests WHERE char_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_teleport WHERE char_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_buff WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_buff_snapshot WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_buddys WHERE char_id=? OR buddy_id=?")) {
                    delete.setLong(1, objectId);
                    delete.setLong(2, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_buddy_group WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_buddy_metadata "
                                + "WHERE char_obj_id=? OR buddy_id=?")) {
                    delete.setLong(1, objectId);
                    delete.setLong(2, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_board_topic WHERE author_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM `server_盟屋_佈告` WHERE name=?")) {
                    delete.setString(1, characterName);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM " + tournamentTable + " WHERE "
                                + (usesSharedTournamentTable()
                                        ? "source_server_id=? AND char_obj_id=?"
                                        : "char_obj_id=?"))) {
                    int parameter = 1;
                    if (usesSharedTournamentTable()) {
                        delete.setInt(parameter++, classicSourceId);
                    }
                    delete.setLong(parameter, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_other WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM characters WHERE objid=? AND account_name=? AND char_name=?")) {
                    delete.setLong(1, objectId);
                    delete.setString(2, account);
                    delete.setString(3, characterName);
                    if (delete.executeUpdate() != 1) return new RepositoryCharacterMutation(
                            RepositoryCharacterMutationStatus.NOT_FOUND, null);
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_character_origin WHERE char_obj_id=?")) {
                    delete.setLong(1, objectId);
                    delete.executeUpdate();
                }
                return new RepositoryCharacterMutation(
                        RepositoryCharacterMutationStatus.SUCCESS, null);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryPersistenceResult persistLitoCharacter(
            LitoCharacterPersistence value) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return RepositoryPersistenceResult.READ_ONLY;
        }
        for (int skillId : value.skills()) {
            if (skillId <= 0 || skillId > 224) {
                throw new IllegalArgumentException("skill id outside native range: " + skillId);
            }
        }
        for (Map.Entry<Integer, Integer> quest : value.quests().entrySet()) {
            if (quest.getKey() <= 0 || quest.getValue() < 0) {
                throw new IllegalArgumentException("invalid quest persistence row: " + quest);
            }
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            String updateSql = "UPDATE characters SET CurHp=?,CurMp=?,level=?,HighLevel="
                    + "GREATEST(HighLevel,?),Exp=?,Lawful=?,PKcount=?,LastPk=?,"
                    + "PkCountForElf=?,LastPkForElf=?,HellTime=?,LocX=?,LocY=?,MapID=?,"
                    + "Heading=?,ElfAttr=?,OnlineStatus=0 WHERE objid=? AND account_name=? "
                    + "AND char_name=?";
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                int index = 1;
                update.setInt(index++, value.currentHp());
                update.setInt(index++, value.currentMp());
                update.setInt(index++, value.level());
                update.setInt(index++, value.level());
                update.setLong(index++, value.experience());
                update.setInt(index++, value.lawful());
                update.setInt(index++, value.pkCount());
                setEpochTimestamp(update, index++, value.lastPkEpochSeconds());
                update.setInt(index++, value.elfPkCount());
                setEpochTimestamp(update, index++, value.lastElfPkEpochSeconds());
                update.setInt(index++, value.hellTime());
                update.setInt(index++, value.x());
                update.setInt(index++, value.y());
                update.setInt(index++, value.mapId());
                update.setInt(index++, value.heading());
                update.setInt(index++, value.elfAttribute());
                update.setLong(index++, value.objectId());
                update.setString(index++, value.account());
                update.setString(index, value.characterName());
                if (update.executeUpdate() != 1) return RepositoryPersistenceResult.NOT_FOUND;
            }
            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO character_other (char_obj_id,killCount,deathCount) "
                            + "VALUES (?,?,?) ON DUPLICATE KEY UPDATE "
                            + "killCount=VALUES(killCount),deathCount=VALUES(deathCount)")) {
                upsert.setLong(1, value.objectId());
                upsert.setInt(2, value.killCount());
                upsert.setInt(3, value.deathCount());
                upsert.executeUpdate();
            }
            reconcileSkills(connection, value.objectId(), value.skills());
            reconcileQuests(connection, value.objectId(), value.quests());
            return RepositoryPersistenceResult.SUCCESS;
        }
    }

    @Override public RepositoryPersistenceResult persistLitoSourceInventory(
            long characterObjectId, List<LitoSourceInventoryMutation> mutations)
            throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return RepositoryPersistenceResult.READ_ONLY;
        }
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid source inventory character id");
        }
        List<LitoSourceInventoryMutation> rows =
                mutations == null ? List.of() : List.copyOf(mutations);
        Set<Long> unique = new LinkedHashSet<>();
        for (LitoSourceInventoryMutation row : rows) {
            if (row == null || !unique.add(row.sourceObjectId())) {
                throw new IllegalArgumentException("duplicate/null source inventory mutation");
            }
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement owns = connection.prepareStatement(
                     "SELECT 1 FROM characters WHERE objid=? LIMIT 1")) {
            owns.setLong(1, characterObjectId);
            try (ResultSet found = owns.executeQuery()) {
                if (!found.next()) return RepositoryPersistenceResult.NOT_FOUND;
            }
            for (LitoSourceInventoryMutation row : rows) {
                if (row.delete()) {
                    deleteSourceInventoryRow(connection, characterObjectId, row.sourceObjectId());
                } else {
                    updateSourceInventoryRow(connection, characterObjectId, row);
                }
            }
            return RepositoryPersistenceResult.SUCCESS;
        }
    }

    @Override public RepositoryPersistenceResult persistLitoInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems) throws SQLException {
        return persistLitoInventory(characterObjectId, sourceMutations,
                litoOwnedItems, List.of(), Set.of());
    }

    @Override public RepositoryPersistenceResult persistLitoInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds) throws SQLException {
        return persistLitoInventory(characterObjectId, sourceMutations,
                litoOwnedItems, pets, deletedSourcePetCollarIds, null);
    }

    @Override public RepositoryPersistenceResult persistLitoInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory)
            throws SQLException {
        return persistLitoInventoryInternal(characterObjectId, sourceMutations,
                litoOwnedItems, pets, deletedSourcePetCollarIds,
                expectedPrivateInventory, null, null);
    }

    @Override public RepositoryPersistenceResult persistLitoHotelInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            RepositoryHotelChange hotelChange) throws SQLException {
        if (hotelChange == null) {
            throw new IllegalArgumentException("hotel SQL change is required");
        }
        return persistLitoInventoryInternal(characterObjectId, sourceMutations,
                litoOwnedItems, pets, deletedSourcePetCollarIds,
                expectedPrivateInventory, hotelChange, null);
    }

    @Override public RepositoryPersistenceResult persistLitoWaterDragonPassInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            RepositoryWaterDragonPassChange passChange) throws SQLException {
        if (passChange == null) {
            throw new IllegalArgumentException("Water Dragon SQL change is required");
        }
        if (!passChange.pass().sourceId().equalsIgnoreCase(classicSourceName)) {
            throw new IllegalArgumentException(
                    "Water Dragon pass source does not match repository route");
        }
        return persistLitoInventoryInternal(characterObjectId, sourceMutations,
                litoOwnedItems, pets, deletedSourcePetCollarIds,
                expectedPrivateInventory, null, passChange);
    }

    private RepositoryPersistenceResult persistLitoInventoryInternal(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            RepositoryHotelChange hotelChange,
            RepositoryWaterDragonPassChange waterDragonChange) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return RepositoryPersistenceResult.READ_ONLY;
        }
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid LiTo inventory character id");
        }
        List<LitoSourceInventoryMutation> sourceRows = sourceMutations == null
                ? List.of() : List.copyOf(sourceMutations);
        List<LitoOwnedInventoryItem> ownedRows = litoOwnedItems == null
                ? List.of() : List.copyOf(litoOwnedItems);
        List<LitoPetPersistence> petRows = pets == null
                ? List.of() : List.copyOf(pets);
        Set<Long> deletedPetCollars = deletedSourcePetCollarIds == null
                ? Set.of() : Set.copyOf(deletedSourcePetCollarIds);
        if (expectedPrivateInventory != null
                && !expectedPrivateInventory.authoritative()) {
            throw new IllegalArgumentException(
                    "private inventory CAS requires an authoritative snapshot");
        }
        Set<Long> sourceIds = new LinkedHashSet<>();
        for (LitoSourceInventoryMutation row : sourceRows) {
            if (row == null || !sourceIds.add(row.sourceObjectId())) {
                throw new IllegalArgumentException("duplicate/null source inventory mutation");
            }
        }
        Set<Long> ownedIds = new LinkedHashSet<>();
        for (LitoOwnedInventoryItem row : ownedRows) {
            if (row == null || !ownedIds.add(row.objectId())
                    || sourceIds.contains(row.objectId())) {
                throw new IllegalArgumentException(
                        "duplicate/null/overlapping LiTo-owned inventory row");
            }
        }
        Set<Long> petCollarIds = new LinkedHashSet<>();
        Set<Long> petObjectIds = new LinkedHashSet<>();
        for (LitoPetPersistence row : petRows) {
            if (row == null || !petCollarIds.add(row.collarObjectId())
                    || !petObjectIds.add(row.petObjectId())) {
                throw new IllegalArgumentException("duplicate/null LiTo pet row");
            }
        }
        for (Long sourceCollarId : deletedPetCollars) {
            if (sourceCollarId == null || sourceCollarId <= 0
                    || sourceCollarId > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException("invalid deleted source pet collar id");
            }
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            if (!characterRowExists(connection, characterObjectId)) {
                return RepositoryPersistenceResult.NOT_FOUND;
            }
            List<LitoOwnedInventoryItem> sharedRows = new ArrayList<>();
            List<LitoOwnedInventoryItem> privateRows = new ArrayList<>();
            for (LitoOwnedInventoryItem row : ownedRows) {
                // Only items represented exactly once in the shared static catalog
                // may enter character_items where TW13081901.bin can also see them.
                if (resourceItemTemplateExists(connection, row.itemId())) {
                    sharedRows.add(row);
                } else {
                    if (InventoryCatalog.find(row.itemId()) == null) {
                        throw new SQLException("LiTo-private item has no runtime template: "
                                + row.itemId());
                    }
                    privateRows.add(row);
                }
            }
            Set<Long> sharedIds = sharedRows.stream()
                    .map(LitoOwnedInventoryItem::objectId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            try (Statement lock = connection.createStatement()) {
                lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                        + "character_items_time WRITE, character_pets WRITE, "
                        + "lito_item_origin WRITE, lito_private_inventory WRITE"
                        + (hotelChange == null ? ""
                                : ", lito_hotel_lease_key WRITE")
                        + (waterDragonChange == null ? ""
                                : ", lito_water_dragon_pass WRITE"));
                try {
                    if (!characterRowExists(connection, characterObjectId)) {
                        return RepositoryPersistenceResult.NOT_FOUND;
                    }
                    RepositoryPrivateInventorySnapshot currentPrivate =
                            loadPrivateInventorySnapshot(connection, characterObjectId);
                    if (expectedPrivateInventory != null
                            && !expectedPrivateInventory.equals(currentPrivate)) {
                        return RepositoryPersistenceResult.CONFLICT;
                    }
                    for (LitoSourceInventoryMutation row : sourceRows) {
                        if (litoItemOriginExists(connection, row.sourceObjectId())) {
                            throw new SQLException("LiTo-owned object entered immutable source "
                                    + "mutation set: " + row.sourceObjectId());
                        }
                        if (!characterItemExists(
                                connection, characterObjectId, row.sourceObjectId())) {
                            throw new SQLException("source inventory row disappeared: "
                                    + row.sourceObjectId());
                        }
                    }
                    for (LitoOwnedInventoryItem row : sharedRows) {
                        boolean origin = litoItemOriginExists(connection, row.objectId());
                        if (!origin && characterItemIdExists(connection, row.objectId())) {
                            throw new SQLException("LiTo inventory object id collides with "
                                    + "a non-owned row: " + row.objectId());
                        }
                    }
                    for (LitoOwnedInventoryItem row : privateRows) {
                        if (characterItemIdExists(connection, row.objectId())) {
                            throw new SQLException("LiTo-private inventory object id collides "
                                    + "with character_items: " + row.objectId());
                        }
                    }
                    Map<Long, LitoOwnedInventoryItem> sharedById = sharedRows.stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    LitoOwnedInventoryItem::objectId, row -> row));
                    Map<Long, LitoOwnedInventoryItem> privateById = privateRows.stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    LitoOwnedInventoryItem::objectId, row -> row));
                    if (hotelChange != null && !validateHotelChange(
                            connection, characterObjectId, hotelChange, sharedById)) {
                        return RepositoryPersistenceResult.CONFLICT;
                    }
                    if (waterDragonChange != null && !validateWaterDragonPassChange(
                            connection, characterObjectId, waterDragonChange,
                            privateById)) {
                        return RepositoryPersistenceResult.CONFLICT;
                    }
                    for (LitoPetPersistence pet : petRows) {
                        if (pet.litoOwned()) {
                            LitoOwnedInventoryItem collar = sharedById.get(
                                    pet.collarObjectId());
                            if (collar == null || collar.itemId() != pet.collarCatalogId()) {
                                throw new SQLException(
                                        "LiTo-owned pet has no owned SQL collar: "
                                                + pet.collarObjectId());
                            }
                            if (pet.sourceCollarObjectId() > 0
                                    && pet.sourceCollarObjectId()
                                            != pet.collarObjectId()) {
                                validateSourcePetRow(connection, characterObjectId,
                                        pet.sourceCollarObjectId());
                            }
                        } else {
                            if (litoItemOriginExists(
                                    connection, pet.sourceCollarObjectId())) {
                                throw new SQLException(
                                        "owned pet entered protected source update set: "
                                                + pet.sourceCollarObjectId());
                            }
                            validateSourcePetRow(connection, characterObjectId,
                                    pet.sourceCollarObjectId());
                        }
                    }
                    for (long sourceCollarId : deletedPetCollars) {
                        if (litoItemOriginExists(connection, sourceCollarId)) {
                            throw new SQLException(
                                    "owned pet entered source tombstone set: "
                                            + sourceCollarId);
                        }
                        validateSourcePetRow(
                                connection, characterObjectId, sourceCollarId);
                    }
                    Set<Long> obsolete = litoOwnedItemIds(
                            connection, characterObjectId);
                    obsolete.removeAll(sharedIds);
                    for (LitoSourceInventoryMutation row : sourceRows) {
                        if (row.delete()) {
                            deleteSourceInventoryRow(
                                    connection, characterObjectId, row.sourceObjectId());
                        } else {
                            updateSourceInventoryRow(connection, characterObjectId, row);
                        }
                    }
                    for (LitoOwnedInventoryItem row : sharedRows) {
                        upsertLitoOwnedInventoryRow(connection, characterObjectId, row);
                    }
                    for (LitoPetPersistence pet : petRows) {
                        if (pet.litoOwned()) {
                            upsertLitoOwnedPetRow(connection, pet);
                            if (pet.sourceCollarObjectId() > 0
                                    && pet.sourceCollarObjectId()
                                            != pet.collarObjectId()) {
                                deleteSourcePetAndCollar(connection, characterObjectId,
                                        pet.sourceCollarObjectId());
                            }
                        } else {
                            updateSourcePetRow(connection, pet);
                        }
                    }
                    for (long sourceCollarId : deletedPetCollars) {
                        deleteSourcePetAndCollar(
                                connection, characterObjectId, sourceCollarId);
                    }
                    for (long objectId : obsolete) {
                        deleteLitoOwnedInventoryRow(connection, characterObjectId, objectId);
                    }
                    persistPrivateInventorySnapshot(connection, characterObjectId,
                            currentPrivate.snapshotVersion(), privateRows);
                    if (hotelChange != null) {
                        applyHotelChange(connection, characterObjectId, hotelChange);
                    }
                    if (waterDragonChange != null) {
                        applyWaterDragonPassChange(
                                connection, characterObjectId, waterDragonChange);
                    }
                    return RepositoryPersistenceResult.SUCCESS;
                } finally {
                    lock.execute("UNLOCK TABLES");
                }
            }
        }
    }

    private static boolean validateWaterDragonPassChange(
            Connection connection, long characterObjectId,
            RepositoryWaterDragonPassChange change,
            Map<Long, LitoOwnedInventoryItem> privateById) throws SQLException {
        GameStateStore.WaterDragonPassState pass = change.pass();
        LitoOwnedInventoryItem item = privateById.get(pass.itemObjectId());
        if (item == null
                || item.itemId() != ClassicWorldWaterDragonCatalog.PASS_ITEM_ID
                || item.quantity() != ClassicWorldWaterDragonCatalog.PASS_RENTAL_COUNT
                || item.expiresAtEpochMillis() <= 0
                || Math.abs(item.expiresAtEpochMillis() / 1_000L
                        - pass.expiresAtEpochSeconds()) > 2L) {
            return false;
        }
        long now = System.currentTimeMillis() / 1_000L;
        if (pass.expiresAtEpochSeconds() <= now
                || pass.expiresAtEpochSeconds() > now
                        + ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS + 2L) {
            return false;
        }
        try (PreparedStatement prune = connection.prepareStatement(
                "DELETE FROM lito_water_dragon_pass WHERE expires_at<=?")) {
            prune.setLong(1, now);
            prune.executeUpdate();
        }
        try (PreparedStatement collision = connection.prepareStatement(
                "SELECT 1 FROM lito_water_dragon_pass "
                        + "WHERE item_obj_id=? OR show_id=? OR room_number=? "
                        + "OR renter_obj_id=? LIMIT 1")) {
            collision.setLong(1, pass.itemObjectId());
            collision.setInt(2, pass.showId());
            collision.setInt(3, pass.roomNumber());
            collision.setLong(4, characterObjectId);
            try (ResultSet row = collision.executeQuery()) {
                if (row.next()) return false;
            }
        }
        try (PreparedStatement count = connection.prepareStatement(
                "SELECT COUNT(*) FROM lito_water_dragon_pass WHERE expires_at>?")) {
            count.setLong(1, now);
            try (ResultSet row = count.executeQuery()) {
                return row.next() && row.getInt(1)
                        < ClassicWorldWaterDragonCatalog.ROOM_COUNT;
            }
        }
    }

    private static void applyWaterDragonPassChange(
            Connection connection, long characterObjectId,
            RepositoryWaterDragonPassChange change) throws SQLException {
        GameStateStore.WaterDragonPassState pass = change.pass();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO lito_water_dragon_pass "
                        + "(item_obj_id,show_id,room_number,renter_obj_id,"
                        + "renter_identity,expires_at) VALUES (?,?,?,?,?,?)")) {
            insert.setLong(1, pass.itemObjectId());
            insert.setInt(2, pass.showId());
            insert.setInt(3, pass.roomNumber());
            insert.setLong(4, characterObjectId);
            insert.setString(5, pass.renterIdentity());
            insert.setLong(6, pass.expiresAtEpochSeconds());
            if (insert.executeUpdate() != 1) {
                throw new SQLException("Water Dragon pass insert count changed");
            }
        }
    }

    private record HotelSqlLease(long showId, boolean active) {}

    private static boolean validateHotelChange(
            Connection connection, long characterObjectId,
            RepositoryHotelChange change,
            Map<Long, LitoOwnedInventoryItem> sharedById) throws SQLException {
        return change.kind() == RepositoryHotelChangeKind.RENT
                ? validateHotelRent(connection, characterObjectId, change, sharedById)
                : validateHotelReturn(connection, characterObjectId, change);
    }

    private static boolean validateHotelRent(
            Connection connection, long characterObjectId,
            RepositoryHotelChange change,
            Map<Long, LitoOwnedInventoryItem> sharedById) throws SQLException {
        GameStateStore.HotelLeaseState lease = change.lease();
        if (!change.keyObjectIds().contains((long) lease.showId())) {
            return false;
        }
        for (long objectId : change.keyObjectIds()) {
            LitoOwnedInventoryItem key = sharedById.get(objectId);
            if (key == null || key.itemId() != ClassicWorldHotelCatalog.KEY_ITEM_ID
                    || key.quantity() != 1 || key.expiresAtEpochMillis() <= 0
                    || Math.abs(key.expiresAtEpochMillis() / 1_000L
                            - lease.expiresAtEpochSeconds()) > 2L) {
                return false;
            }
        }
        try (PreparedStatement collision = connection.prepareStatement(
                "SELECT 1 FROM lito_hotel_lease_key WHERE show_id=? "
                        + "OR item_obj_id=? LIMIT 1")) {
            for (long objectId : change.keyObjectIds()) {
                collision.setLong(1, lease.showId());
                collision.setLong(2, objectId);
                try (ResultSet row = collision.executeQuery()) {
                    if (row.next()) return false;
                }
            }
        }
        long now = System.currentTimeMillis() / 1_000L;
        Set<Long> shows = new LinkedHashSet<>();
        int rooms = 0;
        int halls = 0;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT show_id,room_number,meeting_hall,renter_obj_id "
                        + "FROM lito_hotel_lease_key WHERE npc_id=? "
                        + "AND released=0 AND expires_at>? "
                        + "GROUP BY show_id,room_number,meeting_hall,renter_obj_id")) {
            query.setInt(1, lease.npcId());
            query.setLong(2, now);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    long show = row.getLong("show_id");
                    if (!shows.add(show)) continue;
                    if (row.getLong("renter_obj_id") == characterObjectId
                            || row.getInt("room_number") == lease.roomNumber()) {
                        return false;
                    }
                    if (row.getBoolean("meeting_hall")) halls++;
                    else rooms++;
                }
            }
        }
        return lease.meetingHall()
                ? halls < ClassicWorldHotelCatalog.HALL_CAPACITY
                : rooms < ClassicWorldHotelCatalog.ROOM_CAPACITY;
    }

    private static boolean validateHotelReturn(
            Connection connection, long characterObjectId,
            RepositoryHotelChange change) throws SQLException {
        GameStateStore.HotelReturnQuote quote = change.returnQuote();
        HotelSqlLease owned = selectHotelLeaseForRenter(
                connection, quote.npcId(), characterObjectId);
        if (quote.releasesLease() != (owned != null)) return false;
        int mappedKeys = 0;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT lito_hotel_lease_key.npc_id "
                        + "FROM lito_hotel_lease_key JOIN character_items "
                        + "ON character_items.id=lito_hotel_lease_key.item_obj_id "
                        + "WHERE lito_hotel_lease_key.item_obj_id=? "
                        + "AND character_items.char_id=? LIMIT 1")) {
            for (long objectId : change.keyObjectIds()) {
                query.setLong(1, objectId);
                query.setLong(2, characterObjectId);
                try (ResultSet row = query.executeQuery()) {
                    if (!row.next() || row.getInt("npc_id") != quote.npcId()) {
                        return false;
                    }
                    mappedKeys++;
                }
            }
        }
        int expectedRefund = Math.multiplyExact(mappedKeys,
                ClassicWorldHotelCatalog.REFUND_PER_RETURNED_KEY);
        if (owned != null && owned.active()) {
            expectedRefund = Math.addExact(expectedRefund,
                    ClassicWorldHotelCatalog.ACTIVE_LEASE_BASE_REFUND);
        }
        return quote.refund() == expectedRefund;
    }

    private static HotelSqlLease selectHotelLeaseForRenter(
            Connection connection, int npcId, long characterObjectId)
            throws SQLException {
        long now = System.currentTimeMillis() / 1_000L;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT show_id,expires_at,released FROM lito_hotel_lease_key "
                        + "WHERE npc_id=? AND renter_obj_id=? "
                        + "GROUP BY show_id,expires_at,released "
                        + "ORDER BY IF(released=0 AND expires_at>?,0,1),show_id LIMIT 1")) {
            query.setInt(1, npcId);
            query.setLong(2, characterObjectId);
            query.setLong(3, now);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                return new HotelSqlLease(row.getLong("show_id"),
                        !row.getBoolean("released")
                                && row.getLong("expires_at") > now);
            }
        }
    }

    private static void applyHotelChange(
            Connection connection, long characterObjectId,
            RepositoryHotelChange change) throws SQLException {
        if (change.kind() == RepositoryHotelChangeKind.RENT) {
            GameStateStore.HotelLeaseState lease = change.lease();
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO lito_hotel_lease_key "
                            + "(item_obj_id,show_id,npc_id,room_number,meeting_hall,"
                            + "renter_obj_id,renter_identity,expires_at,released) "
                            + "VALUES (?,?,?,?,?,?,?,?,0)")) {
                for (long objectId : change.keyObjectIds()) {
                    insert.setLong(1, objectId);
                    insert.setLong(2, lease.showId());
                    insert.setInt(3, lease.npcId());
                    insert.setInt(4, lease.roomNumber());
                    insert.setBoolean(5, lease.meetingHall());
                    insert.setLong(6, characterObjectId);
                    insert.setString(7, lease.renterIdentity());
                    insert.setLong(8, lease.expiresAtEpochSeconds());
                    insert.addBatch();
                }
                int[] changed = insert.executeBatch();
                if (changed.length != change.keyObjectIds().size()) {
                    throw new SQLException("hotel lease/key insert count changed");
                }
            }
            return;
        }
        GameStateStore.HotelReturnQuote quote = change.returnQuote();
        HotelSqlLease owned = selectHotelLeaseForRenter(
                connection, quote.npcId(), characterObjectId);
        if (quote.releasesLease() && owned != null) {
            try (PreparedStatement release = connection.prepareStatement(
                    "UPDATE lito_hotel_lease_key SET released=1 WHERE show_id=?")) {
                release.setLong(1, owned.showId());
                release.executeUpdate();
            }
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM lito_hotel_lease_key WHERE item_obj_id=?")) {
            for (long objectId : change.keyObjectIds()) {
                delete.setLong(1, objectId);
                if (delete.executeUpdate() != 1) {
                    throw new SQLException("hotel key relation disappeared");
                }
            }
        }
    }

    private static RepositoryPrivateInventorySnapshot loadPrivateInventorySnapshot(
            Connection connection, long characterObjectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT inventory_state,snapshot_version FROM lito_private_inventory "
                        + "WHERE char_obj_id=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return RepositoryPrivateInventorySnapshot.empty();
                long version = row.getLong("snapshot_version");
                try {
                    return new RepositoryPrivateInventorySnapshot(true, version,
                            decodePrivateInventory(row.getString("inventory_state")));
                } catch (IllegalArgumentException invalid) {
                    throw new SQLException("invalid LiTo-private inventory row character="
                            + characterObjectId, invalid);
                }
            }
        }
    }

    @Override public RepositoryPersistenceResult persistTournamentRewardInventory(
            long characterObjectId,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            List<LitoOwnedInventoryItem> replacementRewards) throws SQLException {
        if (readOnly || !repositoryMode.equals("lito_mysql")) {
            return RepositoryPersistenceResult.READ_ONLY;
        }
        requireCharacterObjectId(characterObjectId, "Tournament reward inventory");
        if (expectedPrivateInventory == null
                || !expectedPrivateInventory.authoritative()) {
            throw new IllegalArgumentException(
                    "Tournament reward CAS requires an authoritative private snapshot");
        }
        List<LitoOwnedInventoryItem> rewards = replacementRewards == null
                ? List.of() : List.copyOf(replacementRewards);
        Set<Long> objectIds = new LinkedHashSet<>();
        for (LitoOwnedInventoryItem item : rewards) {
            if (item == null || !item.tournamentReward()
                    || !objectIds.add(item.objectId())) {
                throw new IllegalArgumentException(
                        "Tournament reward snapshot contains duplicate/non-reward row");
            }
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            List<LitoOwnedInventoryItem> sharedRewards = new ArrayList<>();
            List<LitoOwnedInventoryItem> privateRewards = new ArrayList<>();
            for (LitoOwnedInventoryItem item : rewards) {
                if (resourceItemTemplateExists(connection, item.itemId())) {
                    sharedRewards.add(item);
                } else {
                    if (InventoryCatalog.find(item.itemId()) == null) {
                        throw new SQLException("LiTo-private reward has no runtime template: "
                                + item.itemId());
                    }
                    privateRewards.add(item);
                }
            }
            Set<Long> replacementSharedIds = sharedRewards.stream()
                    .map(LitoOwnedInventoryItem::objectId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            try (Statement lock = connection.createStatement()) {
                lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                        + "character_items_time WRITE, character_pets WRITE, "
                        + "lito_item_origin WRITE, lito_private_inventory WRITE");
                try {
                    if (!characterRowExists(connection, characterObjectId)) {
                        return RepositoryPersistenceResult.NOT_FOUND;
                    }
                    RepositoryPrivateInventorySnapshot currentPrivate =
                            loadPrivateInventorySnapshot(connection, characterObjectId);
                    if (!expectedPrivateInventory.equals(currentPrivate)) {
                        return RepositoryPersistenceResult.CONFLICT;
                    }
                    for (LitoOwnedInventoryItem item : sharedRewards) {
                        boolean origin = litoItemOriginExists(connection, item.objectId());
                        if (!origin && characterItemIdExists(connection, item.objectId())) {
                            throw new SQLException("Tournament reward object id collides with "
                                    + "a non-owned character item: " + item.objectId());
                        }
                    }
                    for (LitoOwnedInventoryItem item : privateRewards) {
                        if (characterItemIdExists(connection, item.objectId())) {
                            throw new SQLException("LiTo-private reward object id collides "
                                    + "with character_items: " + item.objectId());
                        }
                    }
                    Set<Long> obsoleteSharedRewards =
                            litoOwnedTournamentRewardIds(connection, characterObjectId);
                    obsoleteSharedRewards.removeAll(replacementSharedIds);
                    for (LitoOwnedInventoryItem item : sharedRewards) {
                        upsertLitoOwnedInventoryRow(connection, characterObjectId, item);
                    }
                    for (long objectId : obsoleteSharedRewards) {
                        deleteLitoOwnedInventoryRow(
                                connection, characterObjectId, objectId);
                    }
                    List<LitoOwnedInventoryItem> nextPrivate = new ArrayList<>();
                    for (LitoOwnedInventoryItem item : currentPrivate.items()) {
                        if (!item.tournamentReward()) nextPrivate.add(item);
                    }
                    nextPrivate.addAll(privateRewards);
                    persistPrivateInventorySnapshot(connection, characterObjectId,
                            currentPrivate.snapshotVersion(), nextPrivate);
                    return RepositoryPersistenceResult.SUCCESS;
                } finally {
                    lock.execute("UNLOCK TABLES");
                }
            }
        }
    }

    private static Set<Long> litoOwnedTournamentRewardIds(
            Connection connection, long characterObjectId) throws SQLException {
        Set<Long> result = new LinkedHashSet<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT item_obj_id FROM lito_item_origin "
                        + "WHERE char_obj_id=? AND tournament_reward=1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) result.add(row.getLong(1));
            }
        }
        return result;
    }

    private static void persistPrivateInventorySnapshot(
            Connection connection, long characterObjectId, long currentVersion,
            List<LitoOwnedInventoryItem> items) throws SQLException {
        final long nextVersion;
        try {
            nextVersion = Math.incrementExact(currentVersion);
        } catch (ArithmeticException exhausted) {
            throw new SQLException("LiTo-private inventory version exhausted", exhausted);
        }
        if (nextVersion > 0xFFFF_FFFFL) {
            throw new SQLException("LiTo-private inventory version exhausted");
        }
        String encoded = encodePrivateInventory(items);
        try (PreparedStatement upsert = connection.prepareStatement(
                "INSERT INTO lito_private_inventory "
                        + "(char_obj_id,inventory_state,snapshot_version,updated_at) "
                        + "VALUES (?,?,?,NOW()) ON DUPLICATE KEY UPDATE "
                        + "inventory_state=VALUES(inventory_state),"
                        + "snapshot_version=VALUES(snapshot_version),updated_at=NOW()")) {
            upsert.setLong(1, characterObjectId);
            upsert.setString(2, encoded);
            upsert.setLong(3, nextVersion);
            if (upsert.executeUpdate() < 1) {
                throw new SQLException("LiTo-private inventory upsert changed no rows");
            }
        }
    }

    private static String encodePrivateInventory(List<LitoOwnedInventoryItem> items)
            throws SQLException {
        RepositoryPrivateInventorySnapshot normalized =
                new RepositoryPrivateInventorySnapshot(true, 0, items);
        if (normalized.items().size() > RuntimeInventory.MAX_SLOTS) {
            throw new SQLException("LiTo-private inventory exceeds native slot capacity");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(1);
                out.writeInt(normalized.items().size());
                for (LitoOwnedInventoryItem item : normalized.items()) {
                    out.writeLong(item.objectId());
                    out.writeInt(item.itemId());
                    out.writeUTF(item.itemName());
                    out.writeInt(item.quantity());
                    out.writeBoolean(item.equipped());
                    out.writeBoolean(item.sealed());
                    out.writeInt(item.enchantLevel());
                    out.writeBoolean(item.identified());
                    out.writeInt(item.durability());
                    out.writeInt(item.chargeCount());
                    out.writeInt(item.blessing());
                    out.writeLong(item.expiresAtEpochMillis());
                    out.writeBoolean(item.tournamentReward());
                }
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new SQLException("cannot encode LiTo-private inventory", impossible);
        }
    }

    private static List<LitoOwnedInventoryItem> decodePrivateInventory(String encoded)
            throws SQLException {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded == null ? "" : encoded);
            try (ByteArrayInputStream source = new ByteArrayInputStream(bytes);
                 DataInputStream in = new DataInputStream(source)) {
                if (in.readInt() != 1) {
                    throw new IOException("unsupported private inventory format");
                }
                int size = in.readInt();
                if (size < 0 || size > RuntimeInventory.MAX_SLOTS) {
                    throw new IOException("invalid private inventory size");
                }
                List<LitoOwnedInventoryItem> items = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    items.add(new LitoOwnedInventoryItem(
                            in.readLong(), in.readInt(), in.readUTF(), in.readInt(),
                            in.readBoolean(), in.readBoolean(), in.readInt(),
                            in.readBoolean(), in.readInt(), in.readInt(), in.readInt(),
                            in.readLong(), in.readBoolean()));
                }
                if (source.available() != 0) {
                    throw new IOException("trailing private inventory data");
                }
                return new RepositoryPrivateInventorySnapshot(true, 0, items).items();
            }
        } catch (IOException | IllegalArgumentException invalid) {
            throw new SQLException("cannot decode LiTo-private inventory", invalid);
        }
    }

    private static void validateSourcePetRow(Connection connection,
                                              long characterObjectId,
                                              long collarObjectId)
            throws SQLException {
        String collarSql = "SELECT 1 FROM character_items WHERE id=? AND char_id=? "
                + "AND item_id IN (?,?) LIMIT 1";
        try (PreparedStatement query = connection.prepareStatement(collarSql)) {
            query.setLong(1, collarObjectId);
            query.setLong(2, characterObjectId);
            query.setLong(3, InventoryCatalog.PET_COLLAR);
            query.setLong(4, InventoryCatalog.HIGH_PET_COLLAR);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("source pet collar row disappeared: "
                            + collarObjectId);
                }
            }
        }
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM character_pets WHERE item_obj_id=? LIMIT 1")) {
            query.setLong(1, collarObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("source pet row disappeared: "
                            + collarObjectId);
                }
            }
        }
    }

    private static void updateSourcePetRow(Connection connection,
                                           LitoPetPersistence pet)
            throws SQLException {
        String sql = "UPDATE character_pets SET npcid=?,name=?,lvl=?,hp=?,mp=?,exp=?,"
                + "lawful=? WHERE item_obj_id=?";
        try (PreparedStatement update = connection.prepareStatement(sql)) {
            int index = 1;
            update.setInt(index++, pet.npcId());
            update.setString(index++, pet.name());
            update.setInt(index++, pet.level());
            update.setInt(index++, pet.maxHp());
            update.setInt(index++, pet.maxMp());
            update.setLong(index++, pet.experience());
            update.setInt(index++, pet.lawful());
            update.setLong(index, pet.sourceCollarObjectId());
            if (update.executeUpdate() != 1) {
                throw new SQLException("source pet row disappeared: "
                        + pet.sourceCollarObjectId());
            }
        }
    }

    private static void upsertLitoOwnedPetRow(Connection connection,
                                               LitoPetPersistence pet)
            throws SQLException {
        String sql = "INSERT INTO character_pets "
                + "(item_obj_id,objid,npcid,name,lvl,hp,mp,exp,lawful) "
                + "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                + "objid=VALUES(objid),npcid=VALUES(npcid),name=VALUES(name),"
                + "lvl=VALUES(lvl),hp=VALUES(hp),mp=VALUES(mp),"
                + "exp=VALUES(exp),lawful=VALUES(lawful)";
        try (PreparedStatement upsert = connection.prepareStatement(sql)) {
            int index = 1;
            upsert.setLong(index++, pet.collarObjectId());
            upsert.setLong(index++, pet.petObjectId());
            upsert.setInt(index++, pet.npcId());
            upsert.setString(index++, pet.name());
            upsert.setInt(index++, pet.level());
            upsert.setInt(index++, pet.maxHp());
            upsert.setInt(index++, pet.maxMp());
            upsert.setLong(index++, pet.experience());
            upsert.setInt(index, pet.lawful());
            upsert.executeUpdate();
        }
    }

    private static void deleteSourcePetAndCollar(Connection connection,
                                                  long characterObjectId,
                                                  long collarObjectId)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_pets WHERE item_obj_id=?")) {
            delete.setLong(1, collarObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
            delete.setLong(1, collarObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_items WHERE id=? AND char_id=?")) {
            delete.setLong(1, collarObjectId);
            delete.setLong(2, characterObjectId);
            if (delete.executeUpdate() != 1) {
                throw new SQLException("source pet collar disappeared: "
                        + collarObjectId);
            }
        }
    }

    private boolean resourceItemTemplateExists(Connection connection, int itemId)
            throws SQLException {
        String sql = "SELECT ((SELECT COUNT(*) FROM " + resourceTable("weapon")
                + " WHERE item_id=?)+(SELECT COUNT(*) FROM " + resourceTable("armor")
                + " WHERE item_id=?)+(SELECT COUNT(*) FROM " + resourceTable("etcitem")
                + " WHERE item_id=?)) AS template_count";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, itemId);
            query.setInt(2, itemId);
            query.setInt(3, itemId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() && row.getInt(1) == 1;
            }
        }
    }

    private static boolean characterRowExists(Connection connection, long characterObjectId)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM characters WHERE objid=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) { return row.next(); }
        }
    }

    private static boolean characterItemExists(
            Connection connection, long characterObjectId, long objectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM character_items WHERE id=? AND char_id=? LIMIT 1")) {
            query.setLong(1, objectId);
            query.setLong(2, characterObjectId);
            try (ResultSet row = query.executeQuery()) { return row.next(); }
        }
    }

    private static boolean characterItemIdExists(Connection connection, long objectId)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM character_items WHERE id=? LIMIT 1")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) { return row.next(); }
        }
    }

    private static boolean litoItemOriginExists(Connection connection, long objectId)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM lito_item_origin WHERE item_obj_id=? LIMIT 1")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) { return row.next(); }
        }
    }

    private static Set<Long> litoOwnedItemIds(
            Connection connection, long characterObjectId) throws SQLException {
        Set<Long> result = new LinkedHashSet<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT item_obj_id FROM lito_item_origin WHERE char_obj_id=?")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) result.add(row.getLong(1));
            }
        }
        return result;
    }

    private static void upsertLitoOwnedInventoryRow(
            Connection connection, long characterObjectId,
            LitoOwnedInventoryItem row) throws SQLException {
        String itemSql = "INSERT INTO character_items "
                + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,enchantlvl,"
                + "is_id,durability,charge_count,bless) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE item_id=VALUES(item_id),char_id=VALUES(char_id),"
                + "item_name=VALUES(item_name),count=VALUES(count),"
                + "is_equipped=VALUES(is_equipped),is_sealed=VALUES(is_sealed),"
                + "enchantlvl=VALUES(enchantlvl),is_id=VALUES(is_id),"
                + "durability=VALUES(durability),charge_count=VALUES(charge_count),"
                + "bless=VALUES(bless)";
        try (PreparedStatement upsert = connection.prepareStatement(itemSql)) {
            int index = 1;
            upsert.setLong(index++, row.objectId());
            upsert.setInt(index++, row.itemId());
            upsert.setLong(index++, characterObjectId);
            upsert.setString(index++, row.itemName());
            upsert.setInt(index++, row.quantity());
            upsert.setBoolean(index++, row.equipped());
            upsert.setBoolean(index++, row.sealed());
            upsert.setInt(index++, row.enchantLevel());
            upsert.setBoolean(index++, row.identified());
            upsert.setInt(index++, row.durability());
            upsert.setInt(index++, row.chargeCount());
            upsert.setInt(index, row.blessing());
            upsert.executeUpdate();
        }
        try (PreparedStatement upsert = connection.prepareStatement(
                "INSERT INTO lito_item_origin "
                        + "(item_obj_id,char_obj_id,catalog_id,tournament_reward,created_at) "
                        + "VALUES (?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE "
                        + "char_obj_id=VALUES(char_obj_id),catalog_id=VALUES(catalog_id),"
                        + "tournament_reward=VALUES(tournament_reward)")) {
            upsert.setLong(1, row.objectId());
            upsert.setLong(2, characterObjectId);
            upsert.setInt(3, row.itemId());
            upsert.setBoolean(4, row.tournamentReward());
            upsert.executeUpdate();
        }
        if (row.expiresAtEpochMillis() == 0) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
                delete.setLong(1, row.objectId());
                delete.executeUpdate();
            }
        } else {
            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO character_items_time (itemr_obj_id,usertime) VALUES (?,?) "
                            + "ON DUPLICATE KEY UPDATE usertime=VALUES(usertime)")) {
                upsert.setLong(1, row.objectId());
                upsert.setTimestamp(2,
                        new java.sql.Timestamp(row.expiresAtEpochMillis()));
                upsert.executeUpdate();
            }
        }
    }

    private static void deleteLitoOwnedInventoryRow(
            Connection connection, long characterObjectId, long objectId) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_pets WHERE item_obj_id=?")) {
            delete.setLong(1, objectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
            delete.setLong(1, objectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_items WHERE id=? AND char_id=?")) {
            delete.setLong(1, objectId);
            delete.setLong(2, characterObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM lito_item_origin WHERE item_obj_id=? AND char_obj_id=?")) {
            delete.setLong(1, objectId);
            delete.setLong(2, characterObjectId);
            delete.executeUpdate();
        }
    }

    private static void updateSourceInventoryRow(
            Connection connection, long characterObjectId,
            LitoSourceInventoryMutation row) throws SQLException {
        String sql = "UPDATE character_items SET item_id=?,count=?,is_equipped=?,"
                + "is_sealed=?,enchantlvl=?,is_id=?,durability=?,charge_count=?,bless=? "
                + "WHERE id=? AND char_id=?";
        try (PreparedStatement update = connection.prepareStatement(sql)) {
            int index = 1;
            update.setInt(index++, row.itemId());
            update.setInt(index++, row.quantity());
            update.setBoolean(index++, row.equipped());
            update.setBoolean(index++, row.sealed());
            update.setInt(index++, row.enchantLevel());
            update.setBoolean(index++, row.identified());
            update.setInt(index++, row.durability());
            update.setInt(index++, row.chargeCount());
            update.setInt(index++, row.blessing());
            update.setLong(index++, row.sourceObjectId());
            update.setLong(index, characterObjectId);
            if (update.executeUpdate() != 1) {
                throw new SQLException("source inventory row disappeared: "
                        + row.sourceObjectId());
            }
        }
        if (row.expiresAtEpochMillis() == 0) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
                delete.setLong(1, row.sourceObjectId());
                delete.executeUpdate();
            }
        } else {
            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO character_items_time (itemr_obj_id,usertime) VALUES (?,?) "
                            + "ON DUPLICATE KEY UPDATE usertime=VALUES(usertime)")) {
                upsert.setLong(1, row.sourceObjectId());
                upsert.setTimestamp(2, new java.sql.Timestamp(row.expiresAtEpochMillis()));
                upsert.executeUpdate();
            }
        }
    }

    private static void deleteSourceInventoryRow(
            Connection connection, long characterObjectId, long sourceObjectId)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
            delete.setLong(1, sourceObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_items WHERE id=? AND char_id=?")) {
            delete.setLong(1, sourceObjectId);
            delete.setLong(2, characterObjectId);
            delete.executeUpdate();
        }
    }

    private static void reconcileSkills(
            Connection connection, long objectId, Set<Integer> desired) throws SQLException {
        Set<Integer> existing = new LinkedHashSet<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT skill_id FROM character_skills WHERE char_obj_id=?")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) existing.add(row.getInt(1));
            }
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_skills WHERE char_obj_id=? AND skill_id=?")) {
            for (int skillId : existing) {
                if (desired.contains(skillId)) continue;
                delete.setLong(1, objectId);
                delete.setInt(2, skillId);
                delete.addBatch();
            }
            delete.executeBatch();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT IGNORE INTO character_skills "
                        + "(char_obj_id,skill_id,skill_name,is_active,activetimeleft) "
                        + "VALUES (?,?,?,0,0)")) {
            for (int skillId : desired) {
                if (existing.contains(skillId)) continue;
                insert.setLong(1, objectId);
                insert.setInt(2, skillId);
                insert.setString(3, "LiTo skill " + skillId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void reconcileQuests(
            Connection connection, long objectId, Map<Integer, Integer> desired)
            throws SQLException {
        Set<Integer> existing = new LinkedHashSet<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT quest_id FROM character_quests WHERE char_id=?")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) existing.add(row.getInt(1));
            }
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_quests WHERE char_id=? AND quest_id=?")) {
            for (int questId : existing) {
                if (desired.containsKey(questId)) continue;
                delete.setLong(1, objectId);
                delete.setInt(2, questId);
                delete.addBatch();
            }
            delete.executeBatch();
        }
        try (PreparedStatement upsert = connection.prepareStatement(
                "INSERT INTO character_quests (char_id,quest_id,quest_step) VALUES (?,?,?) "
                        + "ON DUPLICATE KEY UPDATE quest_step=VALUES(quest_step)")) {
            for (Map.Entry<Integer, Integer> quest : desired.entrySet()) {
                upsert.setLong(1, objectId);
                upsert.setInt(2, quest.getKey());
                upsert.setInt(3, quest.getValue());
                upsert.addBatch();
            }
            upsert.executeBatch();
        }
    }

    private static void setEpochTimestamp(
            PreparedStatement statement, int index, long epochSeconds) throws SQLException {
        if (epochSeconds <= 0) statement.setTimestamp(index, null);
        else statement.setTimestamp(index, new java.sql.Timestamp(epochSeconds * 1_000L));
    }

    @Override public int hellTime(long characterObjectId) throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(
                    "legacy character object id is outside dword range");
        }
        String sql = "SELECT HellTime FROM characters WHERE objid=? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Math.max(0, row.getInt("HellTime")) : 0;
            }
        }
    }

    @Override public SourceKillDeathCounts killDeathCounts(long characterObjectId)
            throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(
                    "legacy character object id is outside dword range");
        }
        String sql = "SELECT killCount,deathCount FROM character_other "
                + "WHERE char_obj_id=? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next()
                        ? new SourceKillDeathCounts(
                                Math.max(0, row.getInt("killCount")),
                                Math.max(0, row.getInt("deathCount")))
                        : SourceKillDeathCounts.none();
            }
        }
    }

    @Override public Set<Integer> learnedSkills(long characterObjectId) throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) return Set.of();
        // Mirror C_LoginToServer: a persisted character_skills row grants
        // neither a client bit nor server mastery unless the matching classic
        // template exists and has a positive level inside the 28-byte bitmap.
        // A LEFT JOIN keeps malformed/orphan rows observable to this filter
        // while preventing level-zero historical placeholders (including
        // skills 92..96) from leaking into LiTo's S_ADD_SPELL.
        String sql = "SELECT cs.skill_id,s.skill_level FROM character_skills cs "
                + "LEFT JOIN " + resourceTable("skills")
                + " s ON s.skill_id=cs.skill_id WHERE cs.char_obj_id=? "
                + "ORDER BY cs.skill_id";
        Set<Integer> result = new LinkedHashSet<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    int skillId = row.getInt("skill_id");
                    int value = row.getInt("skill_level");
                    Integer skillLevel = row.wasNull() ? null : value;
                    if (NativeSkillMastery.importable(skillId, skillLevel)) {
                        result.add(skillId);
                    }
                }
            }
        }
        return Set.copyOf(result);
    }

    @Override public Map<Integer, Integer> questSteps(long characterObjectId)
            throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(
                    "legacy character object id is outside dword range");
        }
        String sql = "SELECT quest_id,quest_step FROM character_quests "
                + "WHERE char_id=? ORDER BY quest_id";
        Map<Integer, Integer> result = new LinkedHashMap<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    int questId = row.getInt("quest_id");
                    int step = row.getInt("quest_step");
                    if (questId <= 0 || step < 0) {
                        throw new SQLException("invalid selected-source quest row for character: "
                                + characterObjectId);
                    }
                    if (result.putIfAbsent(questId, step) != null) {
                        throw new SQLException("duplicate selected-source quest row: "
                                + questId);
                    }
                }
            }
        }
        return Map.copyOf(result);
    }

    @Override public List<SourceInventoryItem> sourceInventory(long characterObjectId)
            throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(
                    "legacy character object id is outside dword range");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return sourceInventoryRows(connection, sourceInventorySql(),
                    characterObjectId);
        }
    }

    @Override public RepositoryPrivateInventorySnapshot litoPrivateInventory(
            long characterObjectId) throws SQLException {
        requireCharacterObjectId(characterObjectId, "LiTo-private inventory");
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryPrivateInventorySnapshot.unavailable();
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            if (!characterRowExists(connection, characterObjectId)) {
                return RepositoryPrivateInventorySnapshot.empty();
            }
            return loadPrivateInventorySnapshot(connection, characterObjectId);
        }
    }

    @Override public RepositoryBookmarkSnapshot bookmarks(long characterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBookmarkSnapshot.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "bookmark character");
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            Map<String, SqlBookmark> rows = loadBookmarks(connection, characterObjectId);
            return new RepositoryBookmarkSnapshot(true, rows.values().stream()
                    .map(SqlBookmark::bookmark).toList());
        }
    }

    @Override public RepositoryBookmarkPersistenceResult persistBookmarks(
            long characterObjectId, List<RepositoryBookmark> expected,
            List<RepositoryBookmark> replacement) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryBookmarkPersistenceResult.READ_ONLY;
        }
        requireCharacterObjectId(characterObjectId, "bookmark character");
        Map<String, RepositoryBookmark> expectedRows = normalizeBookmarks(expected);
        Map<String, RepositoryBookmark> replacementRows = normalizeBookmarks(replacement);
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_teleport WRITE");
            try {
                if (!characterRowExists(connection, characterObjectId)) {
                    return RepositoryBookmarkPersistenceResult.NOT_FOUND;
                }
                Map<String, SqlBookmark> currentRows = loadBookmarks(
                        connection, characterObjectId);
                Map<String, RepositoryBookmark> currentValues = new LinkedHashMap<>();
                currentRows.forEach((name, row) -> currentValues.put(name, row.bookmark()));
                if (!currentValues.equals(expectedRows)) {
                    return RepositoryBookmarkPersistenceResult.CONFLICT;
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_teleport WHERE id=?")) {
                    for (Map.Entry<String, SqlBookmark> entry : currentRows.entrySet()) {
                        RepositoryBookmark next = replacementRows.get(entry.getKey());
                        if (entry.getValue().bookmark().equals(next)) continue;
                        delete.setLong(1, entry.getValue().id());
                        delete.addBatch();
                    }
                    delete.executeBatch();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO character_teleport "
                                + "(char_id,name,locx,locy,mapid) VALUES (?,?,?,?,?)")) {
                    for (Map.Entry<String, RepositoryBookmark> entry
                            : replacementRows.entrySet()) {
                        SqlBookmark old = currentRows.get(entry.getKey());
                        if (old != null && old.bookmark().equals(entry.getValue())) continue;
                        RepositoryBookmark next = entry.getValue();
                        insert.setLong(1, characterObjectId);
                        insert.setString(2, next.name());
                        insert.setInt(3, next.x());
                        insert.setInt(4, next.y());
                        insert.setInt(5, next.mapId());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                return RepositoryBookmarkPersistenceResult.SUCCESS;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private static void requireCharacterObjectId(long characterObjectId, String label) {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(label + " object id is outside dword range");
        }
    }

    private static String bookmarkKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, RepositoryBookmark> normalizeBookmarks(
            List<RepositoryBookmark> bookmarks) {
        List<RepositoryBookmark> source = bookmarks == null ? List.of() : bookmarks;
        if (source.size() > 50) {
            throw new IllegalArgumentException("native bookmark snapshot exceeds 50 rows");
        }
        Map<String, RepositoryBookmark> result = new LinkedHashMap<>();
        for (RepositoryBookmark bookmark : source) {
            if (bookmark == null
                    || result.putIfAbsent(bookmarkKey(bookmark.name()), bookmark) != null) {
                throw new IllegalArgumentException("duplicate or null bookmark row");
            }
        }
        return result;
    }

    private static Map<String, SqlBookmark> loadBookmarks(
            Connection connection, long characterObjectId) throws SQLException {
        Map<String, SqlBookmark> result = new LinkedHashMap<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT id,name,locx,locy,mapid FROM character_teleport "
                        + "WHERE char_id=? ORDER BY id")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    long id = row.getLong("id");
                    try {
                        RepositoryBookmark bookmark = new RepositoryBookmark(
                                row.getString("name"), row.getInt("locx"),
                                row.getInt("locy"), row.getInt("mapid"));
                        String key = bookmarkKey(bookmark.name());
                        if (id <= 0 || result.putIfAbsent(
                                key, new SqlBookmark(id, bookmark)) != null) {
                            throw new SQLException(
                                    "duplicate or invalid selected-source bookmark row");
                        }
                    } catch (IllegalArgumentException invalid) {
                        throw new SQLException(
                                "invalid selected-source bookmark row for character: "
                                        + characterObjectId, invalid);
                    }
                }
            }
        }
        if (result.size() > 50) {
            throw new SQLException(
                    "selected-source bookmark snapshot exceeds native 50-row limit");
        }
        return result;
    }

    @Override public RepositoryBuffSnapshot buffs(long characterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBuffSnapshot.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "buff character");
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadBuffSnapshot(connection, characterObjectId);
        }
    }

    @Override public RepositoryBuffPersistence persistBuffs(
            long characterObjectId, RepositoryBuffSnapshot expected,
            List<RepositoryBuff> replacement) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryBuffPersistence.readOnly(expected);
        }
        requireCharacterObjectId(characterObjectId, "buff character");
        if (expected == null || !expected.authoritative()) {
            throw new IllegalArgumentException("authoritative expected buff snapshot is required");
        }
        RepositoryBuffSnapshot normalizedReplacement =
                new RepositoryBuffSnapshot(true, 0, replacement);
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_buff WRITE, "
                    + "character_buff_snapshot WRITE");
            try {
                if (!characterRowExists(connection, characterObjectId)) {
                    return new RepositoryBuffPersistence(
                            RepositoryBuffPersistenceStatus.NOT_FOUND,
                            RepositoryBuffSnapshot.unavailable());
                }
                RepositoryBuffSnapshot current = loadBuffSnapshot(
                        connection, characterObjectId);
                if (!current.equals(expected)) {
                    return new RepositoryBuffPersistence(
                            RepositoryBuffPersistenceStatus.CONFLICT, current);
                }
                long nextVersion = nextBuffVersion(connection, characterObjectId);
                try {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO character_buff "
                                    + "(char_obj_id,skill_id,remaining_time,poly_id,snapshot_version) "
                                    + "VALUES (?,?,?,?,?)")) {
                        for (RepositoryBuff buff : normalizedReplacement.buffs()) {
                            insert.setLong(1, characterObjectId);
                            insert.setInt(2, buff.skillId());
                            insert.setInt(3, buff.remainingSeconds());
                            insert.setInt(4, buff.polyId());
                            insert.setLong(5, nextVersion);
                            insert.addBatch();
                        }
                        insert.executeBatch();
                    }
                    try (PreparedStatement publish = connection.prepareStatement(
                            "INSERT INTO character_buff_snapshot "
                                    + "(char_obj_id,completed_version,completed_at) "
                                    + "VALUES (?,?,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE "
                                    + "completed_version=VALUES(completed_version),"
                                    + "completed_at=CURRENT_TIMESTAMP")) {
                        publish.setLong(1, characterObjectId);
                        publish.setLong(2, nextVersion);
                        publish.executeUpdate();
                    }
                } catch (SQLException failure) {
                    try (PreparedStatement cleanup = connection.prepareStatement(
                            "DELETE FROM character_buff WHERE char_obj_id=? "
                                    + "AND snapshot_version=?")) {
                        cleanup.setLong(1, characterObjectId);
                        cleanup.setLong(2, nextVersion);
                        cleanup.executeUpdate();
                    } catch (SQLException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                    throw failure;
                }
                try (PreparedStatement cleanup = connection.prepareStatement(
                        "DELETE FROM character_buff WHERE char_obj_id=? "
                                + "AND snapshot_version<>?")) {
                    cleanup.setLong(1, characterObjectId);
                    cleanup.setLong(2, nextVersion);
                    cleanup.executeUpdate();
                }
                return new RepositoryBuffPersistence(
                        RepositoryBuffPersistenceStatus.SUCCESS,
                        new RepositoryBuffSnapshot(true, nextVersion,
                                normalizedReplacement.buffs()));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private static RepositoryBuffSnapshot loadBuffSnapshot(
            Connection connection, long characterObjectId) throws SQLException {
        return SourceBuffRead.load(connection, characterObjectId);
    }

    private static long nextBuffVersion(
            Connection connection, long characterObjectId) throws SQLException {
        long maximum = 0;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT MAX(snapshot_version) FROM character_buff WHERE char_obj_id=?")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (row.next()) maximum = row.getLong(1);
            }
        }
        long wallClock = Math.multiplyExact(System.currentTimeMillis(), 1_000L);
        return Math.max(Math.addExact(maximum, 1L), wallClock);
    }

    @Override public RepositoryBuddySnapshot buddies(long characterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBuddySnapshot.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "buddy character");
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadBuddySnapshot(connection, characterObjectId);
        }
    }

    @Override public RepositoryBuddyPersistence persistBuddies(
            long characterObjectId, RepositoryBuddySnapshot expected,
            List<RepositoryBuddyGroup> groups, List<RepositoryBuddy> buddies)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryBuddyPersistence.readOnly(expected);
        }
        requireCharacterObjectId(characterObjectId, "buddy character");
        if (expected == null || !expected.authoritative()) {
            throw new IllegalArgumentException(
                    "authoritative expected buddy snapshot is required");
        }
        RepositoryBuddySnapshot replacement =
                new RepositoryBuddySnapshot(true, groups, buddies);
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_buddys WRITE, "
                    + "lito_buddy_group WRITE, lito_buddy_metadata WRITE");
            try {
                if (!characterRowExists(connection, characterObjectId)) {
                    return new RepositoryBuddyPersistence(
                            RepositoryBuddyPersistenceStatus.NOT_FOUND,
                            RepositoryBuddySnapshot.unavailable());
                }
                RepositoryBuddySnapshot current =
                        loadBuddySnapshot(connection, characterObjectId);
                if (!current.equals(expected)) {
                    return new RepositoryBuddyPersistence(
                            RepositoryBuddyPersistenceStatus.CONFLICT, current);
                }
                try {
                    replaceBuddyRows(connection, characterObjectId, replacement);
                } catch (SQLException failure) {
                    try {
                        replaceBuddyRows(connection, characterObjectId, current);
                    } catch (SQLException restoreFailure) {
                        failure.addSuppressed(restoreFailure);
                    }
                    throw failure;
                }
                return new RepositoryBuddyPersistence(
                        RepositoryBuddyPersistenceStatus.SUCCESS,
                        loadBuddySnapshot(connection, characterObjectId));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private RepositoryBuddySnapshot loadBuddySnapshot(
            Connection connection, long characterObjectId) throws SQLException {
        List<RepositoryBuddyGroup> groups = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT group_id,group_name FROM lito_buddy_group "
                        + "WHERE char_obj_id=? ORDER BY group_id")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    try {
                        groups.add(new RepositoryBuddyGroup(
                                row.getInt("group_id"), row.getString("group_name")));
                    } catch (IllegalArgumentException invalid) {
                        throw new SQLException("invalid LiTo buddy group for character: "
                                + characterObjectId, invalid);
                    }
                }
            }
        }
        if (groups.isEmpty()) groups.add(new RepositoryBuddyGroup(1, "Friends"));

        Map<String, Map.Entry<String, SqlBuddyMetadata>> metadata =
                new LinkedHashMap<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT buddy_id,buddy_name,target_source,group_id,"
                        + "relationship_style,palette_offset,origin "
                        + "FROM lito_buddy_metadata WHERE char_obj_id=? "
                        + "ORDER BY buddy_name")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    String name = row.getString("buddy_name");
                    String key = buddyKey(name);
                    SqlBuddyMetadata value = new SqlBuddyMetadata(
                            row.getLong("buddy_id"), row.getString("target_source"),
                            row.getInt("group_id"), row.getInt("relationship_style"),
                            row.getInt("palette_offset"), row.getString("origin"));
                    if (key.isEmpty() || metadata.putIfAbsent(
                            key, Map.entry(name, value)) != null) {
                        throw new SQLException("duplicate or invalid LiTo buddy metadata");
                    }
                }
            }
        }

        List<RepositoryBuddy> result = new ArrayList<>();
        Set<String> nativeNames = new LinkedHashSet<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT buddy_id,buddy_name FROM character_buddys "
                        + "WHERE char_id=? ORDER BY id")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    String name = row.getString("buddy_name");
                    String key = buddyKey(name);
                    Map.Entry<String, SqlBuddyMetadata> extension = metadata.remove(key);
                    SqlBuddyMetadata value = extension == null
                            ? new SqlBuddyMetadata(0, "", 1, 1, 1, "")
                            : extension.getValue();
                    if (!nativeNames.add(key)) {
                        throw new SQLException("duplicate selected-source buddy name");
                    }
                    try {
                        result.add(new RepositoryBuddy(name, row.getLong("buddy_id"),
                                value.targetSource(), value.groupId(),
                                value.relationshipStyle(), value.paletteOffset(),
                                value.origin()));
                    } catch (IllegalArgumentException invalid) {
                        throw new SQLException("invalid selected-source buddy row", invalid);
                    }
                }
            }
        }
        for (Map.Entry<String, SqlBuddyMetadata> extension : metadata.values()) {
            SqlBuddyMetadata value = extension.getValue();
            try {
                // A metadata row without its native row is deliberately exposed
                // with object id zero. This makes an external TW deletion visible
                // to whole-snapshot CAS instead of silently masking it.
                result.add(new RepositoryBuddy(extension.getKey(), 0,
                        value.targetSource(), value.groupId(),
                        value.relationshipStyle(), value.paletteOffset(),
                        value.origin()));
            } catch (IllegalArgumentException invalid) {
                throw new SQLException("invalid LiTo-only buddy row", invalid);
            }
        }
        try {
            return new RepositoryBuddySnapshot(true, groups, result);
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid buddy snapshot for character: "
                    + characterObjectId, invalid);
        }
    }

    private void replaceBuddyRows(Connection connection, long characterObjectId,
            RepositoryBuddySnapshot replacement) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM character_buddys WHERE char_id=?")) {
            delete.setLong(1, characterObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM lito_buddy_metadata WHERE char_obj_id=?")) {
            delete.setLong(1, characterObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM lito_buddy_group WHERE char_obj_id=?")) {
            delete.setLong(1, characterObjectId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO lito_buddy_group "
                        + "(char_obj_id,group_id,group_name) VALUES (?,?,?)")) {
            for (RepositoryBuddyGroup group : replacement.groups()) {
                insert.setLong(1, characterObjectId);
                insert.setInt(2, group.id());
                insert.setString(3, group.name());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insertMetadata = connection.prepareStatement(
                     "INSERT INTO lito_buddy_metadata "
                             + "(char_obj_id,buddy_id,buddy_name,target_source,group_id,"
                             + "relationship_style,palette_offset,origin) "
                             + "VALUES (?,?,?,?,?,?,?,?)");
             PreparedStatement insertNative = connection.prepareStatement(
                     "INSERT INTO character_buddys "
                             + "(char_id,buddy_id,buddy_name) VALUES (?,?,?)")) {
            for (RepositoryBuddy buddy : replacement.buddies()) {
                long resolvedId = resolveNativeBuddyId(connection, buddy);
                insertMetadata.setLong(1, characterObjectId);
                insertMetadata.setLong(2, resolvedId);
                insertMetadata.setString(3, buddy.name());
                insertMetadata.setString(4, buddy.targetSource());
                insertMetadata.setInt(5, buddy.groupId());
                insertMetadata.setInt(6, buddy.relationshipStyle());
                insertMetadata.setInt(7, buddy.paletteOffset());
                insertMetadata.setString(8, buddy.origin());
                insertMetadata.addBatch();
                if (resolvedId != 0) {
                    insertNative.setLong(1, characterObjectId);
                    insertNative.setLong(2, resolvedId);
                    insertNative.setString(3, buddy.name());
                    insertNative.addBatch();
                }
            }
            insertMetadata.executeBatch();
            insertNative.executeBatch();
        }
    }

    private long resolveNativeBuddyId(Connection connection, RepositoryBuddy buddy)
            throws SQLException {
        if (!buddy.targetSource().isBlank()
                && !buddy.targetSource().equalsIgnoreCase(accountDatabase)
                && !buddy.targetSource().equalsIgnoreCase(classicSourceName)) {
            return 0;
        }
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT objid FROM characters WHERE char_name=? ORDER BY objid LIMIT 1")) {
            query.setString(1, buddy.name());
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getLong(1) : 0;
            }
        }
    }

    private static String buddyKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    @Override public RepositoryLetterSnapshot letters(long characterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryLetterSnapshot.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "letter character");
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadLetterSnapshot(connection, characterObjectId);
        }
    }

    @Override public RepositoryLetterDelivery deliverLetter(
            long senderCharacterObjectId, long paperObjectId, String receiverName,
            int code, String date, String subject, String content) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return new RepositoryLetterDelivery(
                    RepositoryLetterDeliveryStatus.READ_ONLY, null);
        }
        requireCharacterObjectId(senderCharacterObjectId, "letter sender");
        if (paperObjectId <= 0 || paperObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid letter paper object id");
        }
        // Validate every native packet field before acquiring or mutating SQL rows.
        RepositoryLetter request = new RepositoryLetter(1, code, "sender", receiverName,
                date, 0, subject, content, false);
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES server_info WRITE, characters READ, "
                    + "character_items WRITE, character_items_time WRITE, "
                    + "character_pets READ, character_letter WRITE, lito_item_origin WRITE");
            long newObjectId = 0;
            try {
                String senderName;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT char_name FROM characters WHERE objid=? LIMIT 1")) {
                    query.setLong(1, senderCharacterObjectId);
                    try (ResultSet row = query.executeQuery()) {
                        if (!row.next()) return new RepositoryLetterDelivery(
                                RepositoryLetterDeliveryStatus.CONFLICT, null);
                        senderName = row.getString(1);
                    }
                }
                long receiverObjectId;
                String canonicalReceiver;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT objid,char_name FROM characters WHERE char_name=? "
                                + "ORDER BY objid LIMIT 1")) {
                    query.setString(1, receiverName);
                    try (ResultSet row = query.executeQuery()) {
                        if (!row.next()) return new RepositoryLetterDelivery(
                                RepositoryLetterDeliveryStatus.RECIPIENT_NOT_FOUND, null);
                        receiverObjectId = row.getLong(1);
                        canonicalReceiver = row.getString(2);
                    }
                }
                int paperCount;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT count FROM character_items WHERE id=? AND char_id=? "
                                + "AND item_id=? LIMIT 1")) {
                    query.setLong(1, paperObjectId);
                    query.setLong(2, senderCharacterObjectId);
                    query.setLong(3, InventoryCatalog.LETTER_PAPER);
                    try (ResultSet row = query.executeQuery()) {
                        if (!row.next() || (paperCount = row.getInt(1)) <= 0) {
                            return new RepositoryLetterDelivery(
                                    RepositoryLetterDeliveryStatus.NO_PAPER, null);
                        }
                    }
                }
                int receiverSlots;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT COUNT(*) FROM character_items WHERE char_id=?")) {
                    query.setLong(1, receiverObjectId);
                    try (ResultSet row = query.executeQuery()) {
                        row.next();
                        receiverSlots = row.getInt(1);
                    }
                }
                int slotsAfter = receiverSlots + 1
                        - (receiverObjectId == senderCharacterObjectId && paperCount == 1 ? 1 : 0);
                if (slotsAfter > RuntimeInventory.MAX_SLOTS) {
                    return new RepositoryLetterDelivery(
                            RepositoryLetterDeliveryStatus.RECIPIENT_FULL, null);
                }
                try (Statement allocate = connection.createStatement()) {
                    int changed = allocate.executeUpdate(
                            "UPDATE server_info SET maxid=LAST_INSERT_ID("
                                    + "GREATEST(COALESCE(maxid,0),COALESCE(minid,10000),"
                                    + "COALESCE((SELECT MAX(objid) FROM characters),0),"
                                    + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                                    + "COALESCE((SELECT MAX(objid) FROM character_pets),0))+1) "
                                    + "WHERE id=1");
                    if (changed != 1) throw new SQLException("server_info id=1 is missing");
                    try (ResultSet row = allocate.executeQuery("SELECT LAST_INSERT_ID()")) {
                        if (!row.next()) throw new SQLException("letter object id allocation failed");
                        newObjectId = row.getLong(1);
                    }
                }
                RepositoryLetter letter = new RepositoryLetter(newObjectId, code,
                        senderName, canonicalReceiver, date, 0, subject, content, false);
                try {
                    insertPhysicalLetterItem(connection, receiverObjectId, newObjectId,
                            InventoryCatalog.LETTER_CLOSED);
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO character_letter (item_object_id,code,sender,receiver,"
                                    + "date,template_id,subject,content,isCheck) "
                                    + "VALUES (?,?,?,?,?,?,?,?,?)")) {
                        insert.setLong(1, newObjectId);
                        insert.setInt(2, letter.code());
                        insert.setString(3, letter.sender());
                        insert.setString(4, letter.receiver());
                        insert.setString(5, letter.date());
                        insert.setInt(6, letter.templateId());
                        insert.setBytes(7, terminatedUtf16Le(letter.subject()));
                        insert.setBytes(8, terminatedUtf16Le(letter.content()));
                        insert.setInt(9, 0);
                        insert.executeUpdate();
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO lito_item_origin (item_obj_id,char_obj_id,catalog_id,"
                                    + "tournament_reward,created_at) VALUES (?,?,?,?,NOW())")) {
                        insert.setLong(1, newObjectId);
                        insert.setLong(2, receiverObjectId);
                        insert.setLong(3, InventoryCatalog.LETTER_CLOSED);
                        insert.setBoolean(4, false);
                        insert.executeUpdate();
                    }
                    // Consume the paper only after the recipient rows exist. This
                    // keeps every earlier failure compensatable without recreating
                    // an arbitrary native character_items row; the following
                    // metadata cleanup is idempotent under the exclusive locks.
                    if (paperCount == 1) {
                        try (PreparedStatement delete = connection.prepareStatement(
                                "DELETE FROM character_items WHERE id=? AND char_id=? "
                                        + "AND item_id=? AND count=1")) {
                            delete.setLong(1, paperObjectId);
                            delete.setLong(2, senderCharacterObjectId);
                            delete.setLong(3, InventoryCatalog.LETTER_PAPER);
                            if (delete.executeUpdate() != 1) throw new SQLException(
                                    "letter paper changed while tables were locked");
                        }
                        try (PreparedStatement cleanup = connection.prepareStatement(
                                "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
                            cleanup.setLong(1, paperObjectId);
                            cleanup.executeUpdate();
                        }
                        try (PreparedStatement cleanup = connection.prepareStatement(
                                "DELETE FROM lito_item_origin WHERE item_obj_id=?")) {
                            cleanup.setLong(1, paperObjectId);
                            cleanup.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE character_items SET count=count-1 WHERE id=? AND char_id=? "
                                        + "AND item_id=? AND count=?")) {
                            update.setLong(1, paperObjectId);
                            update.setLong(2, senderCharacterObjectId);
                            update.setLong(3, InventoryCatalog.LETTER_PAPER);
                            update.setInt(4, paperCount);
                            if (update.executeUpdate() != 1) throw new SQLException(
                                    "letter paper changed while tables were locked");
                        }
                    }
                    return new RepositoryLetterDelivery(
                            RepositoryLetterDeliveryStatus.DELIVERED, letter);
                } catch (SQLException failure) {
                    compensatePhysicalLetterInsert(connection, newObjectId, receiverObjectId,
                            failure);
                    throw failure;
                }
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryLetterMutation openLetter(
            long characterObjectId, RepositoryLetter expected) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryLetterMutation.readOnly(expected);
        }
        if (expected == null || expected.opened()) {
            throw new IllegalArgumentException("an expected closed physical letter is required");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                    + "character_letter WRITE, lito_item_origin WRITE");
            try {
                RepositoryLetter current = loadLetter(
                        connection, characterObjectId, expected.itemObjectId());
                if (current == null) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.NOT_FOUND, null);
                if (!sameLetterBody(current, expected)) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.CONFLICT, current);
                if (current.opened()) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.ALREADY_OPEN, current);
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE character_items SET item_id=? WHERE id=? AND char_id=? "
                                + "AND item_id=?")) {
                    update.setLong(1, InventoryCatalog.LETTER_OPEN);
                    update.setLong(2, expected.itemObjectId());
                    update.setLong(3, characterObjectId);
                    update.setLong(4, InventoryCatalog.LETTER_CLOSED);
                    if (update.executeUpdate() != 1) return new RepositoryLetterMutation(
                            RepositoryLetterMutationStatus.CONFLICT,
                            loadLetter(connection, characterObjectId, expected.itemObjectId()));
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE character_letter SET isCheck=1 WHERE item_object_id=? "
                                + "AND isCheck=0")) {
                    update.setLong(1, expected.itemObjectId());
                    if (update.executeUpdate() != 1) {
                        try (PreparedStatement restore = connection.prepareStatement(
                                "UPDATE character_items SET item_id=? WHERE id=? AND char_id=?")) {
                            restore.setLong(1, InventoryCatalog.LETTER_CLOSED);
                            restore.setLong(2, expected.itemObjectId());
                            restore.setLong(3, characterObjectId);
                            restore.executeUpdate();
                        }
                        return new RepositoryLetterMutation(
                                RepositoryLetterMutationStatus.CONFLICT,
                                loadLetter(connection, characterObjectId,
                                        expected.itemObjectId()));
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE lito_item_origin SET catalog_id=? WHERE item_obj_id=? "
                                + "AND char_obj_id=?")) {
                    update.setLong(1, InventoryCatalog.LETTER_OPEN);
                    update.setLong(2, expected.itemObjectId());
                    update.setLong(3, characterObjectId);
                    update.executeUpdate();
                }
                return new RepositoryLetterMutation(RepositoryLetterMutationStatus.SUCCESS,
                        loadLetter(connection, characterObjectId, expected.itemObjectId()));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryLetterMutation deleteLetter(
            long characterObjectId, RepositoryLetter expected) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryLetterMutation.readOnly(expected);
        }
        if (expected == null) throw new IllegalArgumentException(
                "an expected physical letter is required");
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                    + "character_items_time WRITE, character_pets WRITE, "
                    + "character_letter WRITE, lito_item_origin WRITE");
            try {
                RepositoryLetter current = loadLetter(
                        connection, characterObjectId, expected.itemObjectId());
                if (current == null) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.NOT_FOUND, null);
                if (!current.equals(expected)) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.CONFLICT, current);
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_letter WHERE item_object_id=?")) {
                    delete.setLong(1, expected.itemObjectId());
                    if (delete.executeUpdate() != 1) return new RepositoryLetterMutation(
                            RepositoryLetterMutationStatus.CONFLICT, current);
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_pets WHERE item_obj_id=?")) {
                    delete.setLong(1, expected.itemObjectId());
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
                    delete.setLong(1, expected.itemObjectId());
                    delete.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM character_items WHERE id=? AND char_id=? AND item_id=?")) {
                    delete.setLong(1, expected.itemObjectId());
                    delete.setLong(2, characterObjectId);
                    delete.setLong(3, expected.opened()
                            ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED);
                    if (delete.executeUpdate() != 1) throw new SQLException(
                            "physical-letter item disappeared after body deletion");
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_item_origin WHERE item_obj_id=? AND char_obj_id=?")) {
                    delete.setLong(1, expected.itemObjectId());
                    delete.setLong(2, characterObjectId);
                    delete.executeUpdate();
                }
                return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.SUCCESS, current);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private RepositoryLetterSnapshot loadLetterSnapshot(
            Connection connection, long characterObjectId) throws SQLException {
        if (!characterRowExists(connection, characterObjectId)) {
            return new RepositoryLetterSnapshot(true, List.of());
        }
        List<RepositoryLetter> result = new ArrayList<>();
        String sql = "SELECT l.item_object_id,l.code,l.sender,l.receiver,l.date,"
                + "l.template_id,l.subject,l.content,l.isCheck,i.item_id "
                + "FROM characters c JOIN character_letter l ON l.receiver=c.char_name "
                + "LEFT JOIN character_items i ON i.id=l.item_object_id AND i.char_id=c.objid "
                + "WHERE c.objid=? ORDER BY l.item_object_id";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) result.add(letterFromRow(row, characterObjectId));
            }
        }
        return new RepositoryLetterSnapshot(true, result);
    }

    private RepositoryLetter loadLetter(Connection connection, long characterObjectId,
                                        long itemObjectId) throws SQLException {
        String sql = "SELECT character_letter.item_object_id,character_letter.code,"
                + "character_letter.sender,character_letter.receiver,character_letter.date,"
                + "character_letter.template_id,character_letter.subject,"
                + "character_letter.content,character_letter.isCheck,character_items.item_id "
                + "FROM character_letter JOIN character_items ON character_items.id="
                + "character_letter.item_object_id WHERE character_items.char_id=? "
                + "AND character_letter.item_object_id=? LIMIT 1";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, characterObjectId);
            query.setLong(2, itemObjectId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? letterFromRow(row, characterObjectId) : null;
            }
        }
    }

    private static RepositoryLetter letterFromRow(ResultSet row, long ownerObjectId)
            throws SQLException {
        long itemObjectId = row.getLong("item_object_id");
        long itemId = row.getLong("item_id");
        boolean missingBackingItem = row.wasNull();
        boolean opened = row.getInt("isCheck") != 0;
        long requiredItemId = opened
                ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED;
        if (missingBackingItem || itemId != requiredItemId) {
            throw new SQLException("physical-letter/backing-item mismatch owner="
                    + ownerObjectId + " object=" + itemObjectId);
        }
        try {
            return new RepositoryLetter(itemObjectId, row.getInt("code"),
                    row.getString("sender"), row.getString("receiver"),
                    row.getString("date"), row.getInt("template_id"),
                    decodeTerminatedUtf16Le(row.getBytes("subject")),
                    decodeTerminatedUtf16Le(row.getBytes("content")), opened);
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid native physical-letter row object="
                    + itemObjectId, invalid);
        }
    }

    private static boolean sameLetterBody(RepositoryLetter left, RepositoryLetter right) {
        return left != null && right != null
                && left.itemObjectId() == right.itemObjectId()
                && left.code() == right.code() && left.templateId() == right.templateId()
                && left.sender().equals(right.sender())
                && left.receiver().equals(right.receiver())
                && left.date().equals(right.date())
                && left.subject().equals(right.subject())
                && left.content().equals(right.content());
    }

    private static byte[] terminatedUtf16Le(String value) {
        byte[] text = (value == null ? "" : value).getBytes(StandardCharsets.UTF_16LE);
        byte[] terminated = java.util.Arrays.copyOf(text, text.length + 2);
        return terminated;
    }

    private static String decodeTerminatedUtf16Le(byte[] value) {
        if (value == null || (value.length & 1) != 0) {
            throw new IllegalArgumentException("invalid terminated UTF-16LE letter field");
        }
        int end = value.length;
        for (int index = 0; index + 1 < value.length; index += 2) {
            if (value[index] == 0 && value[index + 1] == 0) {
                end = index;
                break;
            }
        }
        if (end == value.length) {
            throw new IllegalArgumentException("unterminated UTF-16LE letter field");
        }
        return new String(value, 0, end, StandardCharsets.UTF_16LE);
    }

    @Override public RepositoryBoardSnapshot boardTopicsBefore(long cursor, int limit)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBoardSnapshot.unavailable();
        }
        long threshold = cursor == 0 || cursor == Protocol.BOARD_INITIAL_CURSOR
                ? Protocol.BOARD_INITIAL_CURSOR : cursor;
        if (threshold <= 0 || threshold > Protocol.BOARD_INITIAL_CURSOR) {
            throw new IllegalArgumentException("invalid native board cursor");
        }
        int pageSize = Math.max(0, Math.min(limit, Protocol.BOARD_PAGE_SIZE));
        List<GameStateStore.BoardTopic> topics = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT id,name,date,title,content "
                             + "FROM `server_盟屋_佈告` WHERE id<? "
                             + "ORDER BY id DESC LIMIT ?")) {
            query.setLong(1, threshold);
            query.setInt(2, Math.max(pageSize, pageSize * 8));
            try (ResultSet row = query.executeQuery()) {
                while (row.next() && topics.size() < pageSize) {
                    StoredBoardTopic stored = boardTopicFromRow(connection, row);
                    if (stored != null) topics.add(stored.topic());
                }
            }
        }
        return new RepositoryBoardSnapshot(true, topics);
    }

    @Override public RepositoryBoardLookup boardTopic(long topicId) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBoardLookup.unavailable();
        }
        if (topicId <= 0 || topicId >= Protocol.BOARD_INITIAL_CURSOR) {
            throw new IllegalArgumentException("invalid native board topic id");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            StoredBoardTopic topic = loadBoardTopic(connection, topicId);
            return topic == null ? RepositoryBoardLookup.missing()
                    : new RepositoryBoardLookup(true, topic.topic());
        }
    }

    @Override public RepositoryBoardMutation postBoardTopic(
            long characterObjectId, String date, String title, String content,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBoardMutation.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "board author");
        if (beforeInventory == null || afterInventory == null) {
            throw new IllegalArgumentException("board inventory snapshots are required");
        }
        // Validate every native string before touching SQL. The final id is assigned
        // under the board-table lock, so id 1 is only a range-validation placeholder.
        GameStateStore.BoardTopic probe = new GameStateStore.BoardTopic(
                classicSourceName, 1, "author",
                GameStateStore.sourceCharacter(classicSourceName, "author"),
                date, title, content);
        Protocol.boardRead(probe);
        long beforeAdena = beforeInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        long afterAdena = afterInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        if (beforeAdena - afterAdena != Protocol.BOARD_WRITE_FEE) {
            throw new IllegalArgumentException("board post must debit the exact write fee");
        }
        Map<Long, Long> sourceIds = localToSourceObjectIds == null
                ? Map.of() : Map.copyOf(localToSourceObjectIds);
        Map<Long, RuntimeInventory.Item> before = positionedInventoryItems(
                beforeInventory, sourceIds);
        Map<Long, RuntimeInventory.Item> after = positionedInventoryItems(
                afterInventory, sourceIds);
        Set<Long> changed = changedInventoryObjectIds(before, after);
        if (changed.isEmpty() || changed.stream().anyMatch(sqlObjectId -> {
            RuntimeInventory.Item old = before.get(sqlObjectId);
            RuntimeInventory.Item next = after.get(sqlObjectId);
            return old == null || old.definition().catalogId() != InventoryCatalog.ADENA
                    || next != null && (next.definition().catalogId() != InventoryCatalog.ADENA
                    || next.quantity() >= old.quantity());
        })) {
            throw new IllegalArgumentException("board post may only reduce existing Adena objects");
        }
        if (readOnly) return RepositoryBoardMutation.readOnly(null);

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                    + "character_items_time WRITE, character_pets READ, "
                    + "lito_item_origin WRITE, "
                    + "lito_warehouse_item_origin WRITE, "
                    + "`server_盟屋_佈告` WRITE");
            Map<Long, SqlStoredItem> stored = new LinkedHashMap<>();
            Map<Long, Boolean> owned = new LinkedHashMap<>();
            boolean inventoryChanged = false;
            long insertedTopicId = 0;
            try {
                String authorName = characterName(connection, characterObjectId);
                if (authorName == null) return new RepositoryBoardMutation(
                        RepositoryBoardMutationStatus.NOT_FOUND, null);
                if (!fitsNativeHouseBoardColumns(
                        authorName, date, title, content)) {
                    throw new IllegalArgumentException(
                            "board post exceeds retained server_盟屋_佈告 columns");
                }
                for (long sqlObjectId : changed) {
                    RuntimeInventory.Item old = before.get(sqlObjectId);
                    SqlStoredItem row = loadStoredItem(connection, "", "", "",
                            characterObjectId, sqlObjectId, false);
                    if (row == null || row.itemId() != InventoryCatalog.ADENA
                            || row.count() != old.quantity()) {
                        return new RepositoryBoardMutation(
                                RepositoryBoardMutationStatus.CONFLICT, null);
                    }
                    stored.put(sqlObjectId, row);
                    owned.put(sqlObjectId, litoItemOriginExists(connection, sqlObjectId));
                }
                long nextTopicId = nextBoardTopicId(connection);
                GameStateStore.BoardTopic topic = new GameStateStore.BoardTopic(
                        classicSourceName, nextTopicId, authorName,
                        GameStateStore.sourceCharacter(classicSourceName, authorName),
                        date, title, content);
                Protocol.boardRead(topic);
                // From this point every SQL failure must restore the complete old
                // inventory snapshot before releasing the MyISAM table locks.
                inventoryChanged = true;
                for (long sqlObjectId : changed) {
                    RuntimeInventory.Item next = after.get(sqlObjectId);
                    deleteStoredItem(connection, "", "", "", characterObjectId,
                            sqlObjectId, false);
                    deleteItemOrigins(connection, sqlObjectId);
                    if (next != null) {
                        insertCharacterItem(connection, characterObjectId, sqlObjectId,
                                next, stored.get(sqlObjectId));
                        if (owned.getOrDefault(sqlObjectId, false)) {
                            upsertCharacterItemOrigin(connection, sqlObjectId,
                                    characterObjectId, next);
                        }
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO `server_盟屋_佈告` "
                                + "(id,name,date,title,content) VALUES (?,?,?,?,?)")) {
                    insert.setLong(1, nextTopicId);
                    insert.setString(2, authorName);
                    insert.setString(3, date);
                    insert.setString(4, title);
                    insert.setString(5, content);
                    if (insert.executeUpdate() != 1) {
                        throw new SQLException("native board topic insert changed no rows");
                    }
                    insertedTopicId = nextTopicId;
                }
                return new RepositoryBoardMutation(
                        RepositoryBoardMutationStatus.SUCCESS, topic);
            } catch (SQLException failure) {
                if (insertedTopicId != 0) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM `server_盟屋_佈告` WHERE id=?")) {
                        delete.setLong(1, insertedTopicId);
                        delete.executeUpdate();
                    } catch (SQLException compensation) {
                        failure.addSuppressed(compensation);
                    }
                }
                if (inventoryChanged) compensateCharacterInventory(connection,
                        characterObjectId, changed, before, stored, owned, failure);
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryBoardMutation deleteBoardTopic(
            long characterObjectId, GameStateStore.BoardTopic expected) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryBoardMutation.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "board author");
        if (expected == null) throw new IllegalArgumentException(
                "an expected native board topic is required");
        Protocol.boardRead(expected);
        if (readOnly) return RepositoryBoardMutation.readOnly(expected);
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, `server_盟屋_佈告` WRITE");
            try {
                if (!characterRowExists(connection, characterObjectId)) {
                    return new RepositoryBoardMutation(
                            RepositoryBoardMutationStatus.NOT_FOUND, null);
                }
                StoredBoardTopic current = loadBoardTopic(connection, expected.id());
                if (current == null) return new RepositoryBoardMutation(
                        RepositoryBoardMutationStatus.NOT_FOUND, null);
                if (!current.topic().equals(expected)) return new RepositoryBoardMutation(
                        RepositoryBoardMutationStatus.CONFLICT, current.topic());
                if (current.authorObjectId() != characterObjectId) {
                    return new RepositoryBoardMutation(
                            RepositoryBoardMutationStatus.NOT_AUTHOR, current.topic());
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM `server_盟屋_佈告` WHERE id=? AND name=?")) {
                    delete.setLong(1, expected.id());
                    delete.setString(2, expected.author());
                    if (delete.executeUpdate() != 1) return new RepositoryBoardMutation(
                            RepositoryBoardMutationStatus.CONFLICT, current.topic());
                }
                return new RepositoryBoardMutation(
                        RepositoryBoardMutationStatus.SUCCESS, current.topic());
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryHouseAuctionSnapshot houseAuctions()
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseAuctionSnapshot.unavailable();
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return new RepositoryHouseAuctionSnapshot(true, loadHouseAuctions(connection));
        }
    }

    @Override public RepositoryHotelSnapshot hotelState() throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHotelSnapshot.unavailable();
        }
        Map<Integer, GameStateStore.HotelLeaseState> leases = new LinkedHashMap<>();
        List<GameStateStore.HotelKeyState> keys = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT item_obj_id,show_id,npc_id,room_number,meeting_hall,"
                             + "renter_identity,expires_at,released "
                             + "FROM lito_hotel_lease_key "
                             + "ORDER BY npc_id,room_number,show_id,item_obj_id");
             ResultSet row = query.executeQuery()) {
            while (row.next()) {
                int showId = row.getInt("show_id");
                GameStateStore.HotelLeaseState lease =
                        new GameStateStore.HotelLeaseState(
                                showId, row.getInt("npc_id"),
                                row.getInt("room_number"),
                                row.getBoolean("meeting_hall"),
                                row.getString("renter_identity"),
                                row.getLong("expires_at"),
                                row.getBoolean("released"));
                GameStateStore.HotelLeaseState previous =
                        leases.putIfAbsent(showId, lease);
                if (previous != null && !previous.equals(lease)) {
                    throw new SQLException(
                            "inconsistent hotel lease rows show=" + showId);
                }
                keys.add(new GameStateStore.HotelKeyState(
                        row.getLong("item_obj_id"), showId, lease.npcId(),
                        lease.meetingHall(), lease.expiresAtEpochSeconds()));
            }
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid durable hotel SQL state", invalid);
        }
        return new RepositoryHotelSnapshot(
                true, List.copyOf(leases.values()), keys);
    }

    @Override public RepositoryWaterDragonPassSnapshot waterDragonPassState()
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryWaterDragonPassSnapshot.unavailable();
        }
        List<GameStateStore.WaterDragonPassState> passes = new ArrayList<>();
        long now = System.currentTimeMillis() / 1_000L;
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT item_obj_id,show_id,room_number,renter_identity,expires_at "
                             + "FROM lito_water_dragon_pass WHERE expires_at>? "
                             + "ORDER BY room_number,item_obj_id")) {
            query.setLong(1, now);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    passes.add(new GameStateStore.WaterDragonPassState(
                            classicSourceName,
                            row.getLong("item_obj_id"), row.getInt("show_id"),
                            row.getInt("room_number"),
                            row.getString("renter_identity"),
                            row.getLong("expires_at")));
                }
            }
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid durable Water Dragon SQL state", invalid);
        }
        return new RepositoryWaterDragonPassSnapshot(true, passes);
    }

    @Override public RepositoryHouseTaxQuote houseTaxQuote(
            long characterObjectId, int keeperNpcId) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseTaxQuote.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "house-tax character");
        if (keeperNpcId <= 0) {
            throw new IllegalArgumentException("invalid house-tax keeper");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadHouseTaxQuote(connection, characterObjectId, keeperNpcId);
        }
    }

    @Override public RepositoryHouseTaxPayment persistHouseTaxPayment(
            long characterObjectId, RepositoryHouseTaxQuote expected,
            long nowEpochSeconds, int taxIntervalDays, long taxAdena,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseTaxPayment.unavailable();
        }
        requireCharacterObjectId(characterObjectId, "house-tax character");
        if (expected == null || !expected.member()
                || expected.house() == null
                || !expected.house().sourceId().equalsIgnoreCase(classicSourceName)
                || nowEpochSeconds <= 0 || taxIntervalDays <= 0
                || taxIntervalDays > 365 || taxAdena <= 0
                || taxAdena > Integer.MAX_VALUE
                || beforeInventory == null || afterInventory == null) {
            throw new IllegalArgumentException("invalid SQL house-tax payment");
        }
        long beforeAdena = beforeInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        long afterAdena = afterInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        if (beforeAdena - afterAdena != taxAdena) {
            throw new IllegalArgumentException(
                    "house-tax payment must debit the exact tax");
        }
        Map<Long, Long> sourceIds = localToSourceObjectIds == null
                ? Map.of() : Map.copyOf(localToSourceObjectIds);
        Map<Long, RuntimeInventory.Item> before = positionedInventoryItems(
                beforeInventory, sourceIds);
        Map<Long, RuntimeInventory.Item> after = positionedInventoryItems(
                afterInventory, sourceIds);
        Set<Long> changed = changedInventoryObjectIds(before, after);
        if (changed.isEmpty() || changed.stream().anyMatch(sqlObjectId -> {
            RuntimeInventory.Item old = before.get(sqlObjectId);
            RuntimeInventory.Item next = after.get(sqlObjectId);
            return old == null || old.definition().catalogId() != InventoryCatalog.ADENA
                    || next != null && (next.definition().catalogId()
                            != InventoryCatalog.ADENA
                    || next.quantity() >= old.quantity());
        })) {
            throw new IllegalArgumentException(
                    "house-tax payment may only reduce existing Adena objects");
        }
        if (readOnly) {
            return new RepositoryHouseTaxPayment(
                    RepositoryHouseTaxPaymentStatus.READ_ONLY, expected);
        }

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, clan_data READ, "
                    + "character_items WRITE, character_items_time WRITE, "
                    + "lito_item_origin WRITE, lito_warehouse_item_origin WRITE, "
                    + "`server_盟屋_拍賣` READ, `server_盟屋_紀錄` WRITE");
            RepositoryHouseTaxQuote current = null;
            Map<Long, SqlStoredItem> stored = new LinkedHashMap<>();
            Map<Long, Boolean> owned = new LinkedHashMap<>();
            boolean inventoryChanged = false;
            boolean houseChanged = false;
            try {
                current = loadHouseTaxQuote(
                        connection, characterObjectId, expected.keeperNpcId());
                if (current.status() == RepositoryHouseTaxQuoteStatus.NOT_FOUND) {
                    return new RepositoryHouseTaxPayment(
                            RepositoryHouseTaxPaymentStatus.NOT_FOUND, current);
                }
                if (!current.equals(expected)) {
                    return new RepositoryHouseTaxPayment(
                            RepositoryHouseTaxPaymentStatus.STALE, current);
                }
                if (!current.member()) {
                    return new RepositoryHouseTaxPayment(
                            RepositoryHouseTaxPaymentStatus.NOT_MEMBER, current);
                }
                if (current.house().onSale() != 0) {
                    return new RepositoryHouseTaxPayment(
                            RepositoryHouseTaxPaymentStatus.ON_SALE, current);
                }
                if (nativeHouseAuctionExists(
                        connection, current.house().houseId())) {
                    return new RepositoryHouseTaxPayment(
                            RepositoryHouseTaxPaymentStatus.AUCTION_CONFLICT, current);
                }
                for (long sqlObjectId : changed) {
                    RuntimeInventory.Item old = before.get(sqlObjectId);
                    SqlStoredItem row = loadStoredItem(connection, "", "", "",
                            characterObjectId, sqlObjectId, false);
                    if (row == null || row.itemId() != InventoryCatalog.ADENA
                            || row.count() != old.quantity()) {
                        return new RepositoryHouseTaxPayment(
                                RepositoryHouseTaxPaymentStatus.INVENTORY_CONFLICT,
                                current);
                    }
                    stored.put(sqlObjectId, row);
                    owned.put(sqlObjectId,
                            litoItemOriginExists(connection, sqlObjectId));
                }

                inventoryChanged = true;
                for (long sqlObjectId : changed) {
                    RuntimeInventory.Item next = after.get(sqlObjectId);
                    deleteStoredItem(connection, "", "", "", characterObjectId,
                            sqlObjectId, false);
                    deleteItemOrigins(connection, sqlObjectId);
                    if (next != null) {
                        insertCharacterItem(connection, characterObjectId,
                                sqlObjectId, next, stored.get(sqlObjectId));
                        if (owned.getOrDefault(sqlObjectId, false)) {
                            upsertCharacterItemOrigin(connection, sqlObjectId,
                                    characterObjectId, next);
                        }
                    }
                }

                houseChanged = true;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `server_盟屋_紀錄` SET tax_deadline="
                                + "DATE_ADD(DATE_FORMAT(FROM_UNIXTIME(?),"
                                + "'%Y-%m-%d %H:00:00'), INTERVAL ? DAY) "
                                + "WHERE house_id=? AND keeper_id=? AND is_on_sale=0 "
                                + "AND tax_deadline=FROM_UNIXTIME(?)")) {
                    update.setLong(1, nowEpochSeconds);
                    update.setInt(2, taxIntervalDays);
                    update.setLong(3, current.house().houseId());
                    update.setInt(4, current.keeperNpcId());
                    update.setLong(5,
                            current.house().taxDeadlineEpochSeconds());
                    requireOne(update, "native house-tax payment changed");
                }
                RepositoryHouseTaxQuote updated = loadHouseTaxQuote(
                        connection, characterObjectId, current.keeperNpcId());
                long expectedUpperDeadline;
                try {
                    expectedUpperDeadline = Math.addExact(nowEpochSeconds,
                            Math.multiplyExact((long) taxIntervalDays, 86_400L));
                } catch (ArithmeticException overflow) {
                    throw new SQLException("house-tax payment deadline overflow", overflow);
                }
                if (!updated.member()
                        || updated.house().taxDeadlineEpochSeconds()
                            < expectedUpperDeadline - 3_600L
                        || updated.house().taxDeadlineEpochSeconds()
                            > expectedUpperDeadline) {
                    throw new SQLException(
                            "native house-tax payment cannot be reloaded");
                }
                return new RepositoryHouseTaxPayment(
                        RepositoryHouseTaxPaymentStatus.SUCCESS, updated);
            } catch (SQLException failure) {
                if (houseChanged && current != null && current.house() != null) {
                    try (PreparedStatement restore = connection.prepareStatement(
                            "UPDATE `server_盟屋_紀錄` SET tax_deadline="
                                    + "FROM_UNIXTIME(?) WHERE house_id=?")) {
                        restore.setLong(1,
                                current.house().taxDeadlineEpochSeconds());
                        restore.setLong(2, current.house().houseId());
                        requireOne(restore,
                                "native house-tax deadline compensation changed");
                    } catch (SQLException compensation) {
                        failure.addSuppressed(compensation);
                    }
                }
                if (inventoryChanged) {
                    compensateCharacterInventory(connection, characterObjectId,
                            changed, before, stored, owned, failure);
                }
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryHouseTaxSnapshot expiredHouseTaxes(
            long nowEpochSeconds) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseTaxSnapshot.unavailable();
        }
        if (nowEpochSeconds <= 0) {
            throw new IllegalArgumentException("invalid house-tax scan time");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return new RepositoryHouseTaxSnapshot(
                    true, loadExpiredHouseTaxes(connection, nowEpochSeconds));
        }
    }

    @Override public RepositoryHouseTaxMutation persistHouseTaxExpiry(
            RepositoryHouseTaxState expected, long nowEpochSeconds,
            int taxIntervalDays) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseTaxMutation.unavailable();
        }
        if (expected == null || nowEpochSeconds <= 0 || taxIntervalDays <= 0
                || taxIntervalDays > 365
                || !expected.sourceId().equalsIgnoreCase(classicSourceName)) {
            throw new IllegalArgumentException("invalid SQL house-tax expiry");
        }
        if (readOnly) {
            return new RepositoryHouseTaxMutation(
                    RepositoryHouseTaxStatus.READ_ONLY, expected, null, 0);
        }

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES clan_data WRITE, `server_盟屋_拍賣` WRITE, "
                    + "`server_盟屋_紀錄` WRITE, lito_house_auction_metadata WRITE");
            RepositoryHouseTaxState current = null;
            HouseSettlementClan owner = null;
            Long previousMetadata = null;
            boolean auctionChanged = false;
            boolean metadataChanged = false;
            boolean houseChanged = false;
            boolean ownerChanged = false;
            try {
                current = loadHouseTaxState(connection, expected.houseId());
                if (current == null) {
                    return new RepositoryHouseTaxMutation(
                            RepositoryHouseTaxStatus.NOT_FOUND, null, null, 0);
                }
                if (!current.equals(expected)) {
                    return new RepositoryHouseTaxMutation(
                            RepositoryHouseTaxStatus.STALE, current, null, 0);
                }
                if (current.onSale() != 0) {
                    return new RepositoryHouseTaxMutation(
                            RepositoryHouseTaxStatus.ALREADY_ON_SALE,
                            current, null, 0);
                }
                if (current.taxDeadlineEpochSeconds() >= nowEpochSeconds) {
                    return new RepositoryHouseTaxMutation(
                            RepositoryHouseTaxStatus.NOT_EXPIRED,
                            current, null, 0);
                }
                if (loadHouseAuction(connection, current.houseId()) != null) {
                    return new RepositoryHouseTaxMutation(
                            RepositoryHouseTaxStatus.AUCTION_CONFLICT,
                            current, null, 0);
                }
                List<HouseSettlementClan> owners = loadHouseTaxOwners(
                        connection, current.houseId());
                if (owners.size() > 1) {
                    return new RepositoryHouseTaxMutation(
                            RepositoryHouseTaxStatus.OWNER_CONFLICT,
                            current, null, 0);
                }
                owner = owners.isEmpty() ? null : owners.get(0);
                previousMetadata = loadHouseAuctionVersion(
                        connection, current.houseId());

                auctionChanged = true;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO `server_盟屋_拍賣` "
                                + "(house_id,house_name,house_area,deadline,price,location,"
                                + "old_owner,old_owner_id,bidder,bidder_id) "
                                + "VALUES (?,?,?,DATE_ADD(DATE_FORMAT(FROM_UNIXTIME(?),"
                                + "'%Y-%m-%d %H:00:00'), INTERVAL 5 DAY),"
                                + "100000,?,'',0,'',0)")) {
                    insert.setLong(1, current.houseId());
                    insert.setString(2, current.name());
                    insert.setInt(3, current.area());
                    insert.setLong(4, nowEpochSeconds);
                    insert.setString(5, current.location());
                    requireOne(insert, "native house tax auction insert changed");
                }
                metadataChanged = true;
                writeHouseAuctionVersion(connection, current.houseId(), 1);
                houseChanged = true;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `server_盟屋_紀錄` SET is_on_sale=1,"
                                + "is_purchase_basement=1,tax_deadline="
                                + "DATE_ADD(DATE_FORMAT(FROM_UNIXTIME(?),"
                                + "'%Y-%m-%d %H:00:00'), INTERVAL ? DAY) "
                                + "WHERE house_id=?")) {
                    update.setLong(1, nowEpochSeconds);
                    update.setInt(2, Math.addExact(5, taxIntervalDays));
                    update.setLong(3, current.houseId());
                    requireOne(update, "native house tax record changed");
                }
                if (owner != null) {
                    ownerChanged = true;
                    updateClanHouse(connection, owner.clanId(), 0);
                }
                GameStateStore.HouseAuction auction = loadHouseAuction(
                        connection, current.houseId());
                RepositoryHouseTaxState updated = loadHouseTaxState(
                        connection, current.houseId());
                if (auction == null || updated == null) {
                    throw new SQLException("house tax mutation cannot be reloaded");
                }
                return new RepositoryHouseTaxMutation(
                        RepositoryHouseTaxStatus.AUCTIONED, updated, auction,
                        owner == null ? 0 : owner.clanId());
            } catch (ArithmeticException overflow) {
                SQLException failure = new SQLException(
                        "house tax interval overflow", overflow);
                compensateHouseTaxExpiry(connection, current, owner, previousMetadata,
                        auctionChanged, metadataChanged, houseChanged,
                        ownerChanged, failure);
                throw failure;
            } catch (SQLException failure) {
                compensateHouseTaxExpiry(connection, current, owner, previousMetadata,
                        auctionChanged, metadataChanged, houseChanged,
                        ownerChanged, failure);
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryHouseBidMutation persistHouseBid(
            long bidderCharacterObjectId, GameStateStore.HouseAuction expected,
            long amount, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseBidMutation.unavailable();
        }
        if (bidderCharacterObjectId <= 0 || bidderCharacterObjectId > 0xFFFF_FFFFL
                || expected == null || expected.houseId() <= 0
                || amount <= 0 || amount > 2_000_000_000L
                || beforeInventory == null || afterInventory == null) {
            throw new IllegalArgumentException("invalid SQL house-auction bid");
        }
        long beforeAdena = beforeInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        long afterAdena = afterInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        if (beforeAdena - afterAdena != amount) {
            throw new IllegalArgumentException("house bid does not match Adena debit");
        }
        Map<Long, Long> sourceIds = localToSourceObjectIds == null
                ? Map.of() : Map.copyOf(localToSourceObjectIds);
        Map<Long, RuntimeInventory.Item> before = positionedInventoryItems(
                beforeInventory, sourceIds);
        Map<Long, RuntimeInventory.Item> after = positionedInventoryItems(
                afterInventory, sourceIds);
        Set<Long> changed = changedInventoryObjectIds(before, after);
        if (changed.isEmpty() || changed.stream().anyMatch(sqlObjectId -> {
            RuntimeInventory.Item old = before.get(sqlObjectId);
            RuntimeInventory.Item next = after.get(sqlObjectId);
            return old != null && old.definition().catalogId() != InventoryCatalog.ADENA
                    || next != null && next.definition().catalogId() != InventoryCatalog.ADENA;
        })) {
            throw new IllegalArgumentException("house bid may change only Adena objects");
        }
        if (readOnly) {
            return new RepositoryHouseBidMutation(RepositoryHouseBidStatus.READ_ONLY,
                    houseAuctions().byId(expected.houseId()), "", 0, 0);
        }

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                    + "character_items_time WRITE, lito_item_origin WRITE, "
                    + "lito_warehouse_item_origin WRITE, character_warehouse READ, "
                    + "clan_warehouse READ, character_elf_warehouse READ, "
                    + "`server_盟屋_拍賣` WRITE, lito_house_auction_metadata WRITE");
            Map<Long, SqlStoredItem> stored = new LinkedHashMap<>();
            Map<Long, Boolean> owned = new LinkedHashMap<>();
            boolean bidderInventoryChanged = false;
            boolean refundChanged = false;
            boolean refundInserted = false;
            boolean auctionChanged = false;
            long refundObjectId = 0;
            int refundOldCount = 0;
            long previousBidderObjectId = 0;
            GameStateStore.HouseAuction current = null;
            try {
                current = loadHouseAuction(connection, expected.houseId());
                if (current == null) return houseBid(RepositoryHouseBidStatus.NOT_FOUND, null);
                if (!current.equals(expected)) {
                    return houseBid(RepositoryHouseBidStatus.STALE, current);
                }
                if (System.currentTimeMillis() / 1_000L > current.deadlineEpochSeconds()) {
                    return houseBid(RepositoryHouseBidStatus.EXPIRED, current);
                }
                String bidderName = characterName(connection, bidderCharacterObjectId);
                if (bidderName == null || bidderName.isBlank()) {
                    return houseBid(RepositoryHouseBidStatus.INVENTORY_CHANGED, current);
                }
                if (current.bidderObjectId() == bidderCharacterObjectId
                        || current.bidder().equalsIgnoreCase(bidderName)) {
                    return houseBid(RepositoryHouseBidStatus.ALREADY_BIDDER, current);
                }
                long minimum = current.bidder().isBlank()
                        ? current.price() : Math.addExact(current.price(), 1);
                if (amount < minimum) {
                    return houseBid(RepositoryHouseBidStatus.INVALID_AMOUNT, current);
                }
                for (long sqlObjectId : changed) {
                    RuntimeInventory.Item old = before.get(sqlObjectId);
                    SqlStoredItem row = old == null ? null : loadStoredItem(
                            connection, "", "", "", bidderCharacterObjectId,
                            sqlObjectId, false);
                    if (old != null && (row == null
                            || row.itemId() != Math.toIntExact(old.definition().catalogId())
                            || row.count() != old.quantity())) {
                        return houseBid(RepositoryHouseBidStatus.INVENTORY_CHANGED, current);
                    }
                    if (old == null && !sqlInventoryObjectIdAvailable(
                            connection, sqlObjectId)) {
                        return houseBid(RepositoryHouseBidStatus.INVENTORY_CHANGED, current);
                    }
                    stored.put(sqlObjectId, row);
                    owned.put(sqlObjectId,
                            old != null && litoItemOriginExists(connection, sqlObjectId));
                }
                previousBidderObjectId = current.bidderObjectId();
                if (!current.bidder().isBlank()) {
                    String previousBidderName = characterName(
                            connection, previousBidderObjectId);
                    if (previousBidderObjectId <= 0 || previousBidderName == null
                            || !previousBidderName.equalsIgnoreCase(current.bidder())) {
                        return houseBid(RepositoryHouseBidStatus.REFUND_OVERFLOW, current);
                    }
                    long[] refund = refundableAdenaObject(
                            connection, previousBidderObjectId, current.price());
                    refundObjectId = refund[0];
                    refundOldCount = Math.toIntExact(refund[1]);
                    refundInserted = refund[2] != 0;
                    if (refundObjectId == 0) {
                        return houseBid(RepositoryHouseBidStatus.REFUND_OVERFLOW, current);
                    }
                }

                for (long sqlObjectId : changed) {
                    RuntimeInventory.Item old = before.get(sqlObjectId);
                    RuntimeInventory.Item next = after.get(sqlObjectId);
                    SqlStoredItem metadata = stored.get(sqlObjectId);
                    boolean nextOwned = owned.get(sqlObjectId)
                            || next != null && (!next.sourceBound() || next.litoOwned());
                    bidderInventoryChanged = true;
                    deleteStoredItem(connection, "", "", "",
                            bidderCharacterObjectId, sqlObjectId,
                            old == null ? null : false);
                    deleteItemOrigins(connection, sqlObjectId);
                    if (next != null) {
                        insertCharacterItem(connection, bidderCharacterObjectId,
                                sqlObjectId, next, metadata);
                        if (nextOwned) upsertCharacterItemOrigin(connection,
                                sqlObjectId, bidderCharacterObjectId, next);
                    }
                }
                if (refundObjectId != 0) {
                    refundChanged = true;
                    if (refundInserted) {
                        insertRawAdena(connection, previousBidderObjectId,
                                refundObjectId, Math.toIntExact(current.price()));
                    } else {
                        try (PreparedStatement refund = connection.prepareStatement(
                                "UPDATE character_items SET count=? "
                                        + "WHERE id=? AND char_id=? AND item_id=?")) {
                            refund.setLong(1, Math.addExact(refundOldCount, current.price()));
                            refund.setLong(2, refundObjectId);
                            refund.setLong(3, previousBidderObjectId);
                            refund.setLong(4, InventoryCatalog.ADENA);
                            if (refund.executeUpdate() != 1) {
                                throw new SQLException("house previous-bidder refund changed");
                            }
                        }
                    }
                }
                auctionChanged = true;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `server_盟屋_拍賣` SET price=?,bidder=?,bidder_id=? "
                                + "WHERE house_id=?")) {
                    update.setLong(1, amount);
                    update.setString(2, bidderName);
                    update.setLong(3, bidderCharacterObjectId);
                    update.setLong(4, current.houseId());
                    if (update.executeUpdate() != 1) {
                        throw new SQLException("house auction row changed during lock");
                    }
                }
                writeHouseAuctionVersion(connection, current.houseId(),
                        Math.addExact(current.revision(), 1));
                GameStateStore.HouseAuction updated = loadHouseAuction(
                        connection, current.houseId());
                return new RepositoryHouseBidMutation(
                        RepositoryHouseBidStatus.ACCEPTED, updated,
                        current.bidder(), current.bidderObjectId(), current.price());
            } catch (ArithmeticException overflow) {
                SQLException failure = new SQLException("house auction numeric overflow", overflow);
                compensateHouseBid(connection, bidderCharacterObjectId, changed,
                        before, stored, owned, bidderInventoryChanged,
                        previousBidderObjectId, refundObjectId, refundOldCount,
                        refundInserted, refundChanged, current, auctionChanged, failure);
                throw failure;
            } catch (SQLException failure) {
                compensateHouseBid(connection, bidderCharacterObjectId, changed,
                        before, stored, owned, bidderInventoryChanged,
                        previousBidderObjectId, refundObjectId, refundOldCount,
                        refundInserted, refundChanged, current, auctionChanged, failure);
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private RepositoryHouseBidMutation houseBid(
            RepositoryHouseBidStatus status, GameStateStore.HouseAuction auction) {
        return new RepositoryHouseBidMutation(status, auction, "", 0, 0);
    }

    @Override public RepositoryHouseSettlement persistHouseAuctionSettlement(
            GameStateStore.HouseAuction expected, long nowEpochSeconds,
            int taxIntervalDays) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseSettlement.unavailable();
        }
        if (expected == null || expected.houseId() <= 0
                || nowEpochSeconds <= 0 || taxIntervalDays <= 0
                || taxIntervalDays > 365) {
            throw new IllegalArgumentException("invalid SQL house-auction settlement");
        }
        if (readOnly) {
            return houseSettlement(RepositoryHouseSettlementStatus.READ_ONLY,
                    houseAuctions().byId(expected.houseId()), 0, 0);
        }

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, clan_data WRITE, "
                    + "character_items WRITE, character_warehouse READ, "
                    + "clan_warehouse READ, character_elf_warehouse READ, "
                    + "`server_盟屋_拍賣` WRITE, `server_盟屋_紀錄` WRITE, "
                    + "lito_house_auction_metadata WRITE");
            GameStateStore.HouseAuction current = null;
            HouseSettlementClan oldClan = null;
            HouseSettlementClan bidderClan = null;
            HouseSettlementRecord house = null;
            long payoutObjectId = 0;
            int payoutOldCount = 0;
            boolean payoutInserted = false;
            boolean payoutChanged = false;
            boolean oldClanChanged = false;
            boolean bidderClanChanged = false;
            boolean houseChanged = false;
            boolean auctionChanged = false;
            boolean metadataChanged = false;
            try {
                current = loadHouseAuction(connection, expected.houseId());
                if (current == null) {
                    return houseSettlement(RepositoryHouseSettlementStatus.NOT_FOUND,
                            null, 0, 0);
                }
                if (!current.equals(expected)) {
                    return houseSettlement(RepositoryHouseSettlementStatus.STALE,
                            current, 0, 0);
                }
                if (current.deadlineEpochSeconds() > nowEpochSeconds) {
                    return houseSettlement(RepositoryHouseSettlementStatus.NOT_EXPIRED,
                            current, 0, 0);
                }
                house = loadHouseSettlementRecord(connection, current.houseId());
                if (house == null) {
                    throw new SQLException("native house record is missing");
                }

                boolean hasOwner = !current.oldOwner().isBlank()
                        || current.oldOwnerObjectId() != 0;
                boolean hasBidder = !current.bidder().isBlank()
                        || current.bidderObjectId() != 0;
                if (hasOwner && (current.oldOwner().isBlank()
                        || current.oldOwnerObjectId() == 0
                        || !current.oldOwner().equalsIgnoreCase(characterName(
                                connection, current.oldOwnerObjectId())))) {
                    return houseSettlement(RepositoryHouseSettlementStatus.INVALID_OWNER,
                            current, 0, 0);
                }
                if (hasBidder && (current.bidder().isBlank()
                        || current.bidderObjectId() == 0
                        || !current.bidder().equalsIgnoreCase(characterName(
                                connection, current.bidderObjectId())))) {
                    return houseSettlement(RepositoryHouseSettlementStatus.INVALID_BIDDER,
                            current, 0, 0);
                }
                if (hasOwner) {
                    oldClan = loadHouseSettlementClan(
                            connection, current.oldOwnerObjectId(), current.oldOwner());
                    if (oldClan == null || oldClan.houseId() != current.houseId()) {
                        return houseSettlement(RepositoryHouseSettlementStatus.INVALID_OWNER,
                                current, 0, 0);
                    }
                }
                if (hasBidder) {
                    bidderClan = loadHouseSettlementClan(
                            connection, current.bidderObjectId(), current.bidder());
                    if (bidderClan == null || bidderClan.houseId() != 0) {
                        return houseSettlement(RepositoryHouseSettlementStatus.INVALID_BIDDER,
                                current, 0, 0);
                    }
                }

                if (!hasOwner && !hasBidder) {
                    auctionChanged = true;
                    try (PreparedStatement extend = connection.prepareStatement(
                            "UPDATE `server_盟屋_拍賣` SET deadline="
                                    + "DATE_ADD(DATE_FORMAT(FROM_UNIXTIME(?),"
                                    + "'%Y-%m-%d %H:00:00'), INTERVAL 5 DAY) "
                                    + "WHERE house_id=?")) {
                        extend.setLong(1, nowEpochSeconds);
                        extend.setLong(2, current.houseId());
                        requireOne(extend, "house auction extension changed");
                    }
                    metadataChanged = true;
                    writeHouseAuctionVersion(connection, current.houseId(),
                            Math.addExact(current.revision(), 1));
                    GameStateStore.HouseAuction extended = loadHouseAuction(
                            connection, current.houseId());
                    return houseSettlement(RepositoryHouseSettlementStatus.EXTENDED,
                            extended, 0, 0);
                }

                long payout = hasOwner && hasBidder
                        ? Math.floorDiv(Math.multiplyExact(current.price(), 9), 10)
                        : 0;
                if (payout > Integer.MAX_VALUE) {
                    return houseSettlement(RepositoryHouseSettlementStatus.PAYOUT_OVERFLOW,
                            current, 0, 0);
                }
                if (payout > 0) {
                    long[] target = refundableAdenaObject(
                            connection, current.oldOwnerObjectId(), payout);
                    payoutObjectId = target[0];
                    payoutOldCount = Math.toIntExact(target[1]);
                    payoutInserted = target[2] != 0;
                    if (payoutObjectId == 0) {
                        return houseSettlement(
                                RepositoryHouseSettlementStatus.PAYOUT_OVERFLOW,
                                current, 0, 0);
                    }
                    payoutChanged = true;
                    if (payoutInserted) {
                        insertRawAdena(connection, current.oldOwnerObjectId(),
                                payoutObjectId, Math.toIntExact(payout));
                    } else {
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE character_items SET count=? "
                                        + "WHERE id=? AND char_id=? AND item_id=?")) {
                            update.setLong(1, Math.addExact(payoutOldCount, payout));
                            update.setLong(2, payoutObjectId);
                            update.setLong(3, current.oldOwnerObjectId());
                            update.setLong(4, InventoryCatalog.ADENA);
                            requireOne(update, "house seller payout changed");
                        }
                    }
                }
                if (hasOwner && hasBidder) {
                    oldClanChanged = true;
                    updateClanHouse(connection, oldClan.clanId(), 0);
                }
                if (hasBidder) {
                    bidderClanChanged = true;
                    updateClanHouse(connection, bidderClan.clanId(), current.houseId());
                }
                houseChanged = true;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `server_盟屋_紀錄` SET is_on_sale=0,tax_deadline="
                                + "DATE_ADD(DATE_FORMAT(FROM_UNIXTIME(?),"
                                + "'%Y-%m-%d %H:00:00'), INTERVAL ? DAY) "
                                + "WHERE house_id=?")) {
                    update.setLong(1, nowEpochSeconds);
                    update.setInt(2, taxIntervalDays);
                    update.setLong(3, current.houseId());
                    requireOne(update, "native house settlement changed");
                }
                auctionChanged = true;
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM `server_盟屋_拍賣` WHERE house_id=?")) {
                    delete.setLong(1, current.houseId());
                    requireOne(delete, "native house auction deletion changed");
                }
                metadataChanged = true;
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_house_auction_metadata WHERE house_id=?")) {
                    delete.setLong(1, current.houseId());
                    delete.executeUpdate();
                }
                return new RepositoryHouseSettlement(
                        hasBidder ? RepositoryHouseSettlementStatus.TRANSFERRED
                                : RepositoryHouseSettlementStatus.CANCELLED,
                        null, current.oldOwnerObjectId(), payout,
                        current.bidderObjectId(), current.bidder());
            } catch (ArithmeticException overflow) {
                SQLException failure = new SQLException(
                        "house settlement numeric overflow", overflow);
                compensateHouseSettlement(connection, current, oldClan, bidderClan,
                        house, payoutObjectId, payoutOldCount, payoutInserted,
                        payoutChanged, oldClanChanged, bidderClanChanged,
                        houseChanged, auctionChanged, metadataChanged, failure);
                throw failure;
            } catch (SQLException failure) {
                compensateHouseSettlement(connection, current, oldClan, bidderClan,
                        house, payoutObjectId, payoutOldCount, payoutInserted,
                        payoutChanged, oldClanChanged, bidderClanChanged,
                        houseChanged, auctionChanged, metadataChanged, failure);
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private RepositoryHouseSettlement houseSettlement(
            RepositoryHouseSettlementStatus status,
            GameStateStore.HouseAuction auction,
            long oldOwnerObjectId, long payout) {
        return new RepositoryHouseSettlement(status, auction,
                oldOwnerObjectId, payout,
                auction == null ? 0 : auction.bidderObjectId(),
                auction == null ? "" : auction.bidder());
    }

    private record HouseSettlementClan(long clanId, long houseId) {}
    private record HouseSettlementRecord(
            int onSale, int purchaseBasement, String taxDeadline) {}

    private List<RepositoryHouseTaxState> loadExpiredHouseTaxes(
            Connection connection, long nowEpochSeconds) throws SQLException {
        List<RepositoryHouseTaxState> result = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT house_id,house_name,house_area,location,is_on_sale,"
                        + "is_purchase_basement,COALESCE(UNIX_TIMESTAMP(tax_deadline),0) "
                        + "tax_deadline_epoch FROM `server_盟屋_紀錄` "
                        + "WHERE is_on_sale=0 AND tax_deadline<FROM_UNIXTIME(?) "
                        + "ORDER BY house_id")) {
            query.setLong(1, nowEpochSeconds);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) result.add(houseTaxState(row));
            }
        }
        return List.copyOf(result);
    }

    private RepositoryHouseTaxState loadHouseTaxState(
            Connection connection, long houseId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT house_id,house_name,house_area,location,is_on_sale,"
                        + "is_purchase_basement,COALESCE(UNIX_TIMESTAMP(tax_deadline),0) "
                        + "tax_deadline_epoch FROM `server_盟屋_紀錄` WHERE house_id=?")) {
            query.setLong(1, houseId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                RepositoryHouseTaxState result = houseTaxState(row);
                return row.next() ? null : result;
            }
        }
    }

    private record HouseTaxOwner(
            long clanId, String clanName, String leaderName) {}

    private RepositoryHouseTaxQuote loadHouseTaxQuote(
            Connection connection, long characterObjectId, int keeperNpcId)
            throws SQLException {
        RepositoryHouseTaxState house = null;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT house_id,house_name,house_area,location,is_on_sale,"
                        + "is_purchase_basement,"
                        + "COALESCE(UNIX_TIMESTAMP(tax_deadline),0) "
                        + "tax_deadline_epoch FROM `server_盟屋_紀錄` "
                        + "WHERE keeper_id=? LIMIT 1")) {
            query.setInt(1, keeperNpcId);
            try (ResultSet row = query.executeQuery()) {
                if (row.next()) house = houseTaxState(row);
            }
        }
        if (house == null) {
            return new RepositoryHouseTaxQuote(
                    RepositoryHouseTaxQuoteStatus.NOT_FOUND,
                    null, keeperNpcId, 0, "", "");
        }

        long characterClanId;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT ClanID FROM characters WHERE objid=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    return new RepositoryHouseTaxQuote(
                            RepositoryHouseTaxQuoteStatus.NOT_FOUND,
                            house, keeperNpcId, 0, "", "");
                }
                characterClanId = row.getLong(1);
            }
        }

        List<HouseTaxOwner> owners = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT clan_id,clan_name,leader_name FROM clan_data "
                        + "WHERE hashouse=? ORDER BY clan_id LIMIT 2")) {
            query.setLong(1, house.houseId());
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) owners.add(new HouseTaxOwner(
                        row.getLong("clan_id"), row.getString("clan_name"),
                        row.getString("leader_name")));
            }
        }
        if (owners.size() > 1) {
            return new RepositoryHouseTaxQuote(
                    RepositoryHouseTaxQuoteStatus.OWNER_CONFLICT,
                    house, keeperNpcId, 0, "", "");
        }
        if (owners.isEmpty()) {
            return new RepositoryHouseTaxQuote(
                    RepositoryHouseTaxQuoteStatus.VACANT,
                    house, keeperNpcId, 0, "", "");
        }
        HouseTaxOwner owner = owners.get(0);
        RepositoryHouseTaxQuoteStatus status = characterClanId > 0
                && characterClanId == owner.clanId()
                ? RepositoryHouseTaxQuoteStatus.MEMBER
                : RepositoryHouseTaxQuoteStatus.OCCUPIED;
        return new RepositoryHouseTaxQuote(
                status, house, keeperNpcId, owner.clanId(),
                owner.clanName(), owner.leaderName());
    }

    private static boolean nativeHouseAuctionExists(
            Connection connection, long houseId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM `server_盟屋_拍賣` WHERE house_id=? LIMIT 1")) {
            query.setLong(1, houseId);
            try (ResultSet row = query.executeQuery()) {
                return row.next();
            }
        }
    }

    private RepositoryHouseTaxState houseTaxState(ResultSet row) throws SQLException {
        return new RepositoryHouseTaxState(
                classicSourceName, row.getLong("house_id"),
                row.getString("house_name"), row.getInt("house_area"),
                row.getString("location"), row.getInt("is_on_sale"),
                row.getInt("is_purchase_basement"),
                row.getLong("tax_deadline_epoch"));
    }

    private static List<HouseSettlementClan> loadHouseTaxOwners(
            Connection connection, long houseId) throws SQLException {
        List<HouseSettlementClan> result = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT clan_id,hashouse FROM clan_data "
                        + "WHERE hashouse=? ORDER BY clan_id")) {
            query.setLong(1, houseId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) result.add(new HouseSettlementClan(
                        row.getLong("clan_id"), row.getLong("hashouse")));
            }
        }
        return List.copyOf(result);
    }

    private static Long loadHouseAuctionVersion(
            Connection connection, long houseId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT snapshot_version FROM lito_house_auction_metadata "
                        + "WHERE house_id=?")) {
            query.setLong(1, houseId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                long result = row.getLong("snapshot_version");
                if (row.next()) throw new SQLException(
                        "duplicate house auction metadata");
                return result;
            }
        }
    }

    private static void compensateHouseTaxExpiry(
            Connection connection, RepositoryHouseTaxState current,
            HouseSettlementClan owner, Long previousMetadata,
            boolean auctionChanged, boolean metadataChanged,
            boolean houseChanged, boolean ownerChanged, SQLException primary) {
        if (current == null) return;
        if (ownerChanged && owner != null) {
            try {
                updateClanHouse(connection, owner.clanId(), owner.houseId());
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (houseChanged) {
            try (PreparedStatement restore = connection.prepareStatement(
                    "UPDATE `server_盟屋_紀錄` SET is_on_sale=?,"
                            + "is_purchase_basement=?,tax_deadline=FROM_UNIXTIME(?) "
                            + "WHERE house_id=?")) {
                restore.setInt(1, current.onSale());
                restore.setInt(2, current.purchaseBasement());
                restore.setLong(3, current.taxDeadlineEpochSeconds());
                restore.setLong(4, current.houseId());
                restore.executeUpdate();
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (metadataChanged) {
            try {
                if (previousMetadata == null) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM lito_house_auction_metadata WHERE house_id=?")) {
                        delete.setLong(1, current.houseId());
                        delete.executeUpdate();
                    }
                } else {
                    writeHouseAuctionVersion(
                            connection, current.houseId(), previousMetadata);
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (auctionChanged) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM `server_盟屋_拍賣` WHERE house_id=?")) {
                delete.setLong(1, current.houseId());
                delete.executeUpdate();
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
    }

    private static HouseSettlementClan loadHouseSettlementClan(
            Connection connection, long leaderObjectId, String leaderName)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT clan_id,hashouse FROM clan_data "
                        + "WHERE leader_id=? AND leader_name=? ORDER BY clan_id")) {
            query.setLong(1, leaderObjectId);
            query.setString(2, leaderName);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                HouseSettlementClan result = new HouseSettlementClan(
                        row.getLong("clan_id"), row.getLong("hashouse"));
                return row.next() ? null : result;
            }
        }
    }

    private static HouseSettlementRecord loadHouseSettlementRecord(
            Connection connection, long houseId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT is_on_sale,is_purchase_basement,"
                        + "DATE_FORMAT(tax_deadline,'%Y-%m-%d %H:%i:%s') tax_deadline "
                        + "FROM `server_盟屋_紀錄` WHERE house_id=?")) {
            query.setLong(1, houseId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                HouseSettlementRecord result = new HouseSettlementRecord(
                        row.getInt("is_on_sale"), row.getInt("is_purchase_basement"),
                        row.getString("tax_deadline"));
                return row.next() ? null : result;
            }
        }
    }

    private static void updateClanHouse(
            Connection connection, long clanId, long houseId) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE clan_data SET hashouse=? WHERE clan_id=?")) {
            update.setLong(1, houseId);
            update.setLong(2, clanId);
            requireOne(update, "clan house ownership changed");
        }
    }

    private static void requireOne(PreparedStatement statement, String message)
            throws SQLException {
        if (statement.executeUpdate() != 1) throw new SQLException(message);
    }

    private static void compensateHouseSettlement(
            Connection connection, GameStateStore.HouseAuction current,
            HouseSettlementClan oldClan, HouseSettlementClan bidderClan,
            HouseSettlementRecord house, long payoutObjectId, int payoutOldCount,
            boolean payoutInserted, boolean payoutChanged,
            boolean oldClanChanged, boolean bidderClanChanged,
            boolean houseChanged, boolean auctionChanged,
            boolean metadataChanged, SQLException primary) {
        if (current == null) return;
        if (auctionChanged) {
            try {
                restoreHouseAuction(connection, current);
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (metadataChanged) {
            try {
                if (current.revision() == 0) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM lito_house_auction_metadata WHERE house_id=?")) {
                        delete.setLong(1, current.houseId());
                        delete.executeUpdate();
                    }
                } else {
                    writeHouseAuctionVersion(
                            connection, current.houseId(), current.revision());
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (houseChanged && house != null) {
            try {
                try (PreparedStatement restore = connection.prepareStatement(
                        "UPDATE `server_盟屋_紀錄` SET is_on_sale=?,"
                                + "is_purchase_basement=?,tax_deadline=? WHERE house_id=?")) {
                    restore.setInt(1, house.onSale());
                    restore.setInt(2, house.purchaseBasement());
                    restore.setString(3, house.taxDeadline());
                    restore.setLong(4, current.houseId());
                    restore.executeUpdate();
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (bidderClanChanged && bidderClan != null) {
            try {
                updateClanHouse(connection, bidderClan.clanId(), bidderClan.houseId());
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (oldClanChanged && oldClan != null) {
            try {
                updateClanHouse(connection, oldClan.clanId(), oldClan.houseId());
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (payoutChanged) {
            try {
                if (payoutInserted) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM character_items WHERE id=? AND char_id=?")) {
                        delete.setLong(1, payoutObjectId);
                        delete.setLong(2, current.oldOwnerObjectId());
                        delete.executeUpdate();
                    }
                } else {
                    try (PreparedStatement restore = connection.prepareStatement(
                            "UPDATE character_items SET count=? WHERE id=? AND char_id=?")) {
                        restore.setInt(1, payoutOldCount);
                        restore.setLong(2, payoutObjectId);
                        restore.setLong(3, current.oldOwnerObjectId());
                        restore.executeUpdate();
                    }
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
    }

    private static void restoreHouseAuction(
            Connection connection, GameStateStore.HouseAuction source)
            throws SQLException {
        try (PreparedStatement restore = connection.prepareStatement(
                "INSERT INTO `server_盟屋_拍賣` "
                        + "(house_id,house_name,house_area,deadline,price,location,"
                        + "old_owner,old_owner_id,bidder,bidder_id) "
                        + "VALUES (?,?,?,FROM_UNIXTIME(?),?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE house_name=VALUES(house_name),"
                        + "house_area=VALUES(house_area),deadline=VALUES(deadline),"
                        + "price=VALUES(price),location=VALUES(location),"
                        + "old_owner=VALUES(old_owner),old_owner_id=VALUES(old_owner_id),"
                        + "bidder=VALUES(bidder),bidder_id=VALUES(bidder_id)")) {
            restore.setLong(1, source.houseId());
            restore.setString(2, source.name());
            restore.setInt(3, source.area());
            restore.setLong(4, source.deadlineEpochSeconds());
            restore.setLong(5, source.price());
            restore.setString(6, source.location());
            restore.setString(7, source.oldOwner());
            restore.setLong(8, source.oldOwnerObjectId());
            restore.setString(9, source.bidder());
            restore.setLong(10, source.bidderObjectId());
            restore.executeUpdate();
        }
    }

    private List<GameStateStore.HouseAuction> loadHouseAuctions(
            Connection connection) throws SQLException {
        String sql = "SELECT `server_盟屋_拍賣`.house_id,"
                + "`server_盟屋_拍賣`.house_name,`server_盟屋_拍賣`.house_area,"
                + "COALESCE(UNIX_TIMESTAMP(`server_盟屋_拍賣`.deadline),0) deadline_epoch,"
                + "`server_盟屋_拍賣`.price,`server_盟屋_拍賣`.location,"
                + "`server_盟屋_拍賣`.old_owner,`server_盟屋_拍賣`.old_owner_id,"
                + "`server_盟屋_拍賣`.bidder,"
                + "`server_盟屋_拍賣`.bidder_id,"
                + "COALESCE(lito_house_auction_metadata.snapshot_version,0) revision "
                + "FROM `server_盟屋_拍賣` LEFT JOIN lito_house_auction_metadata "
                + "ON lito_house_auction_metadata.house_id=`server_盟屋_拍賣`.house_id "
                + "ORDER BY `server_盟屋_拍賣`.house_id";
        List<GameStateStore.HouseAuction> rows = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(sql);
             ResultSet row = query.executeQuery()) {
            while (row.next()) rows.add(houseAuction(row));
        }
        return List.copyOf(rows);
    }

    private GameStateStore.HouseAuction loadHouseAuction(
            Connection connection, long houseId) throws SQLException {
        return loadHouseAuctions(connection).stream()
                .filter(row -> row.houseId() == houseId).findFirst().orElse(null);
    }

    private GameStateStore.HouseAuction houseAuction(ResultSet row)
            throws SQLException {
        return new GameStateStore.HouseAuction(classicSourceName,
                row.getLong("house_id"), row.getString("house_name"),
                row.getInt("house_area"), row.getLong("deadline_epoch"),
                row.getLong("price"), row.getString("location"),
                row.getString("old_owner"), row.getLong("old_owner_id"),
                row.getString("bidder"),
                row.getLong("bidder_id"), row.getLong("revision"));
    }

    /** Returns object id, old count and insert-new flag. */
    private static long[] refundableAdenaObject(
            Connection connection, long characterObjectId, long amount)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT id,count FROM character_items "
                        + "WHERE char_id=? AND item_id=? ORDER BY id")) {
            query.setLong(1, characterObjectId);
            query.setLong(2, InventoryCatalog.ADENA);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    long count = row.getLong("count");
                    if (count >= 0 && count + amount <= Integer.MAX_VALUE) {
                        return new long[] {row.getLong("id"), count, 0};
                    }
                }
            }
        }
        long allocated;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT GREATEST("
                        + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                        + "COALESCE((SELECT MAX(id) FROM character_warehouse),0),"
                        + "COALESCE((SELECT MAX(id) FROM clan_warehouse),0),"
                        + "COALESCE((SELECT MAX(id) FROM character_elf_warehouse),0))+1")) {
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return new long[] {0, 0, 0};
                allocated = row.getLong(1);
            }
        }
        return allocated > 0 && allocated <= Integer.MAX_VALUE
                ? new long[] {allocated, 0, 1} : new long[] {0, 0, 0};
    }

    private static void insertRawAdena(
            Connection connection, long characterObjectId,
            long objectId, int count) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO character_items "
                        + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,"
                        + "enchantlvl,is_id,durability,charge_count,bless) "
                        + "VALUES (?,?,?,?,?,0,0,0,1,0,0,1)")) {
            insert.setLong(1, objectId);
            insert.setLong(2, InventoryCatalog.ADENA);
            insert.setLong(3, characterObjectId);
            insert.setString(4, "Adena");
            insert.setInt(5, count);
            if (insert.executeUpdate() != 1) {
                throw new SQLException("cannot insert house refund Adena");
            }
        }
    }

    private static void writeHouseAuctionVersion(
            Connection connection, long houseId, long version) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "INSERT INTO lito_house_auction_metadata (house_id,snapshot_version) "
                        + "VALUES (?,?) ON DUPLICATE KEY UPDATE "
                        + "snapshot_version=VALUES(snapshot_version)")) {
            update.setLong(1, houseId);
            update.setLong(2, version);
            update.executeUpdate();
        }
    }

    private static void compensateHouseBid(
            Connection connection, long bidderCharacterObjectId, Set<Long> changed,
            Map<Long, RuntimeInventory.Item> before,
            Map<Long, SqlStoredItem> stored, Map<Long, Boolean> owned,
            boolean bidderInventoryChanged, long previousBidderObjectId,
            long refundObjectId, int refundOldCount, boolean refundInserted,
            boolean refundChanged, GameStateStore.HouseAuction expected,
            boolean auctionChanged, SQLException primary) {
        if (auctionChanged && expected != null) {
            try (PreparedStatement restore = connection.prepareStatement(
                    "UPDATE `server_盟屋_拍賣` SET price=?,bidder=?,bidder_id=? "
                            + "WHERE house_id=?")) {
                restore.setLong(1, expected.price());
                restore.setString(2, expected.bidder());
                restore.setLong(3, expected.bidderObjectId());
                restore.setLong(4, expected.houseId());
                restore.executeUpdate();
                if (expected.revision() == 0) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM lito_house_auction_metadata WHERE house_id=?")) {
                        delete.setLong(1, expected.houseId());
                        delete.executeUpdate();
                    }
                } else {
                    writeHouseAuctionVersion(
                            connection, expected.houseId(), expected.revision());
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (refundChanged) {
            try {
                if (refundInserted) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM character_items WHERE id=? AND char_id=?")) {
                        delete.setLong(1, refundObjectId);
                        delete.setLong(2, previousBidderObjectId);
                        delete.executeUpdate();
                    }
                } else {
                    try (PreparedStatement restore = connection.prepareStatement(
                            "UPDATE character_items SET count=? WHERE id=? AND char_id=?")) {
                        restore.setInt(1, refundOldCount);
                        restore.setLong(2, refundObjectId);
                        restore.setLong(3, previousBidderObjectId);
                        restore.executeUpdate();
                    }
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
        if (bidderInventoryChanged) {
            compensateCharacterInventory(connection, bidderCharacterObjectId,
                    changed, before, stored, owned, primary);
        }
    }

    @Override public RepositoryTournamentRoster tournamentProfiles() throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryTournamentRoster.unavailable();
        }
        List<RepositoryTournamentProfile> profiles = new ArrayList<>();
        boolean shared = usesSharedTournamentTable();
        String sql = shared
                ? "SELECT c.objid AS char_obj_id,c.char_name,"
                        + "t.source_server_id AS profile_source_id,"
                        + "t.char_obj_id AS profile_obj_id,t.wins,t.losses,"
                        + "t.disconnects,t.ranking_points,t.profile_state,"
                        + "t.snapshot_version FROM characters c LEFT JOIN "
                        + tournamentRecordTable() + " t ON t.source_server_id=? "
                        + "AND t.char_obj_id=c.objid AND t.char_name=c.char_name "
                        + "WHERE c.Banned=0 ORDER BY c.objid"
                : "SELECT c.objid AS char_obj_id,c.char_name,"
                        + "t.char_obj_id AS profile_obj_id,t.profile_state,"
                        + "t.snapshot_version FROM characters c "
                        + "LEFT JOIN lito_tournament_profile t "
                        + "ON t.char_obj_id=c.objid AND t.char_name=c.char_name "
                        + "WHERE c.Banned=0 ORDER BY c.objid";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement query = connection.prepareStatement(sql)) {
            if (shared) query.setInt(1, classicSourceId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    row.getLong("profile_obj_id");
                    profiles.add(row.wasNull()
                            ? new RepositoryTournamentProfile(
                                    row.getLong("char_obj_id"),
                                    row.getString("char_name"), 0,
                                    emptyTournamentProfileState(classicSourceName))
                            : shared ? tournamentProfileFromSharedRow(row)
                                    : tournamentProfileFromRow(row));
                }
            }
        }
        return new RepositoryTournamentRoster(true, profiles);
    }

    @Override public RepositoryTournamentMutation persistTournamentProfile(
            long characterObjectId, String characterName,
            RepositoryTournamentProfile expected,
            GameStateStore.TournamentProfileState replacement) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryTournamentMutation.unavailable();
        }
        // Constructor compatibility without a source id retains the old
        // per-dynamic-schema policy. Production source routes use a central,
        // LiTo-owned table and may update only that table even when every
        // retained source schema is configured read-only.
        if (readOnly && !usesSharedTournamentTable()) {
            return new RepositoryTournamentMutation(
                    RepositoryTournamentMutationStatus.READ_ONLY, null);
        }
        requireCharacterObjectId(characterObjectId, "Tournament profile");
        String requestedName = characterName == null ? "" : characterName.trim();
        if (requestedName.isBlank() || requestedName.length() > 45 || replacement == null
                || expected != null && (expected.characterObjectId() != characterObjectId
                        || !expected.characterName().equals(requestedName))
                || !replacement.classicServerName().equalsIgnoreCase(classicSourceName)
                || replacement.rankingSnapshotPresent()
                        && !replacement.snapshotClassicServerName()
                                .equalsIgnoreCase(classicSourceName)) {
            throw new IllegalArgumentException("invalid Tournament profile CAS");
        }
        if (usesSharedTournamentTable()) {
            return persistSharedTournamentProfile(characterObjectId,
                    requestedName, expected, replacement);
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, lito_tournament_profile WRITE");
            try {
                String canonicalName = characterName(connection, characterObjectId);
                if (canonicalName == null || !canonicalName.equals(requestedName)) {
                    return new RepositoryTournamentMutation(
                            RepositoryTournamentMutationStatus.NOT_FOUND, null);
                }
                RepositoryTournamentProfile current = loadTournamentProfile(
                        connection, characterObjectId);
                boolean expectMissing = expected == null || expected.snapshotVersion() == 0;
                if (expectMissing ? current != null : !expected.equals(current)) {
                    return new RepositoryTournamentMutation(
                            RepositoryTournamentMutationStatus.CONFLICT, current);
                }
                long nextVersion = current == null ? 1 : Math.incrementExact(
                        current.snapshotVersion());
                if (nextVersion > 0xFFFF_FFFFL) {
                    throw new SQLException("Tournament profile version exhausted");
                }
                String encoded = encodeTournamentProfile(replacement);
                if (current == null) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO lito_tournament_profile "
                                    + "(char_obj_id,char_name,profile_state,snapshot_version,"
                                    + "updated_at) VALUES (?,?,?,?,NOW())")) {
                        insert.setLong(1, characterObjectId);
                        insert.setString(2, canonicalName);
                        insert.setString(3, encoded);
                        insert.setLong(4, nextVersion);
                        if (insert.executeUpdate() != 1) {
                            throw new SQLException("Tournament profile insert changed no rows");
                        }
                    }
                } else {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE lito_tournament_profile SET char_name=?,profile_state=?,"
                                    + "snapshot_version=?,updated_at=NOW() "
                                    + "WHERE char_obj_id=? AND snapshot_version=?")) {
                        update.setString(1, canonicalName);
                        update.setString(2, encoded);
                        update.setLong(3, nextVersion);
                        update.setLong(4, characterObjectId);
                        update.setLong(5, current.snapshotVersion());
                        if (update.executeUpdate() != 1) {
                            return new RepositoryTournamentMutation(
                                    RepositoryTournamentMutationStatus.CONFLICT,
                                    loadTournamentProfile(connection, characterObjectId));
                        }
                    }
                }
                return new RepositoryTournamentMutation(
                        RepositoryTournamentMutationStatus.SUCCESS,
                        new RepositoryTournamentProfile(characterObjectId, canonicalName,
                                nextVersion, replacement));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        } catch (ArithmeticException exhausted) {
            throw new SQLException("Tournament profile version exhausted", exhausted);
        }
    }

    private RepositoryTournamentProfile loadTournamentProfile(
            Connection connection, long characterObjectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT char_obj_id,char_name,profile_state,snapshot_version "
                        + "FROM lito_tournament_profile WHERE char_obj_id=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? tournamentProfileFromRow(row) : null;
            }
        }
    }

    private boolean usesSharedTournamentTable() {
        return repositoryMode.equals("lito_mysql") && classicSourceId > 0;
    }

    private String tournamentRecordTable() {
        return resourceTable("lito_tournament_character_record");
    }

    private RepositoryTournamentMutation persistSharedTournamentProfile(
            long characterObjectId, String requestedName,
            RepositoryTournamentProfile expected,
            GameStateStore.TournamentProfileState replacement) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                accountUrl, accountLogin, accountPassword)) {
            connection.setReadOnly(false);
            String canonicalName = characterName(connection, characterObjectId);
            if (canonicalName == null || !canonicalName.equals(requestedName)) {
                return new RepositoryTournamentMutation(
                        RepositoryTournamentMutationStatus.NOT_FOUND, null);
            }
            RepositoryTournamentProfile current = loadSharedTournamentProfile(
                    connection, characterObjectId);
            boolean expectMissing = expected == null || expected.snapshotVersion() == 0;
            if (expectMissing ? current != null : !expected.equals(current)) {
                return new RepositoryTournamentMutation(
                        RepositoryTournamentMutationStatus.CONFLICT, current);
            }
            long nextVersion;
            try {
                nextVersion = current == null ? 1
                        : Math.incrementExact(current.snapshotVersion());
            } catch (ArithmeticException exhausted) {
                throw new SQLException("Tournament profile version exhausted", exhausted);
            }
            if (nextVersion > 0xFFFF_FFFFL) {
                throw new SQLException("Tournament profile version exhausted");
            }
            String encoded = encodeTournamentProfile(replacement);
            if (current == null) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO " + tournamentRecordTable()
                                + " (source_server_id,char_obj_id,char_name,wins,losses,"
                                + "disconnects,ranking_points,profile_state,"
                                + "snapshot_version,updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,NOW())")) {
                    bindSharedTournamentReplacement(insert, 1, characterObjectId,
                            canonicalName, replacement, encoded, nextVersion);
                    if (insert.executeUpdate() != 1) {
                        throw new SQLException(
                                "Tournament record insert changed no rows");
                    }
                } catch (SQLException collision) {
                    if (collision.getSQLState() == null
                            || !collision.getSQLState().startsWith("23")) {
                        throw collision;
                    }
                    return new RepositoryTournamentMutation(
                            RepositoryTournamentMutationStatus.CONFLICT,
                            loadSharedTournamentProfile(connection, characterObjectId));
                }
            } else {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE " + tournamentRecordTable()
                                + " SET char_name=?,wins=?,losses=?,disconnects=?,"
                                + "ranking_points=?,profile_state=?,snapshot_version=?,"
                                + "updated_at=NOW() WHERE source_server_id=? "
                                + "AND char_obj_id=? AND snapshot_version=?")) {
                    update.setString(1, canonicalName);
                    update.setInt(2, replacement.wins());
                    update.setInt(3, replacement.losses());
                    update.setInt(4, replacement.disconnects());
                    update.setLong(5, replacement.rankingPoints());
                    update.setString(6, encoded);
                    update.setLong(7, nextVersion);
                    update.setInt(8, classicSourceId);
                    update.setLong(9, characterObjectId);
                    update.setLong(10, current.snapshotVersion());
                    if (update.executeUpdate() != 1) {
                        return new RepositoryTournamentMutation(
                                RepositoryTournamentMutationStatus.CONFLICT,
                                loadSharedTournamentProfile(
                                        connection, characterObjectId));
                    }
                }
            }
            return new RepositoryTournamentMutation(
                    RepositoryTournamentMutationStatus.SUCCESS,
                    new RepositoryTournamentProfile(characterObjectId, canonicalName,
                            nextVersion, replacement));
        }
    }

    private void bindSharedTournamentReplacement(
            PreparedStatement statement, int first, long characterObjectId,
            String canonicalName, GameStateStore.TournamentProfileState replacement,
            String encoded, long version) throws SQLException {
        statement.setInt(first, classicSourceId);
        statement.setLong(first + 1, characterObjectId);
        statement.setString(first + 2, canonicalName);
        statement.setInt(first + 3, replacement.wins());
        statement.setInt(first + 4, replacement.losses());
        statement.setInt(first + 5, replacement.disconnects());
        statement.setLong(first + 6, replacement.rankingPoints());
        statement.setString(first + 7, encoded);
        statement.setLong(first + 8, version);
    }

    private RepositoryTournamentProfile loadSharedTournamentProfile(
            Connection connection, long characterObjectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT source_server_id AS profile_source_id,char_obj_id,char_name,"
                        + "wins,losses,disconnects,ranking_points,profile_state,"
                        + "snapshot_version FROM " + tournamentRecordTable()
                        + " WHERE source_server_id=? AND char_obj_id=? LIMIT 1")) {
            query.setInt(1, classicSourceId);
            query.setLong(2, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? tournamentProfileFromSharedRow(row) : null;
            }
        }
    }

    private RepositoryTournamentProfile tournamentProfileFromSharedRow(ResultSet row)
            throws SQLException {
        int sourceId = row.getInt("profile_source_id");
        RepositoryTournamentProfile profile = tournamentProfileFromRow(row);
        GameStateStore.TournamentProfileState state = profile.state();
        if (sourceId != classicSourceId
                || row.getInt("wins") != state.wins()
                || row.getInt("losses") != state.losses()
                || row.getInt("disconnects") != state.disconnects()
                || row.getLong("ranking_points") != state.rankingPoints()) {
            throw new SQLException("Tournament record columns disagree with profile state "
                    + "source=" + sourceId + " object="
                    + profile.characterObjectId());
        }
        return profile;
    }

    private RepositoryTournamentProfile tournamentProfileFromRow(ResultSet row)
            throws SQLException {
        long objectId = row.getLong("char_obj_id");
        try {
            RepositoryTournamentProfile profile = new RepositoryTournamentProfile(
                    objectId, row.getString("char_name"),
                    row.getLong("snapshot_version"),
                    decodeTournamentProfile(row.getString("profile_state")));
            if (!profile.state().classicServerName().equalsIgnoreCase(classicSourceName)
                    || profile.state().rankingSnapshotPresent()
                            && !profile.state().snapshotClassicServerName()
                                    .equalsIgnoreCase(classicSourceName)) {
                throw new IllegalArgumentException(
                        "Tournament profile belongs to another classic source");
            }
            return profile;
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid Tournament profile row object=" + objectId,
                    invalid);
        }
    }

    private static GameStateStore.TournamentProfileState emptyTournamentProfileState(
            String classicSourceName) {
        return new GameStateStore.TournamentProfileState(
                0, 0, 0, 0, 0, 0, 0, 0,
                java.util.Collections.nCopies(12, 0),
                java.util.Collections.nCopies(5, 0), false,
                classicSourceName, -1, -1, -1, false,
                0, 0, 0, "", -1, -1, -1,
                false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 1, 0, 0);
    }

    private static String encodeTournamentProfile(GameStateStore.TournamentProfileState state)
            throws SQLException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(1);
                out.writeLong(state.rankingPoints());
                out.writeInt(state.wins()); out.writeInt(state.losses());
                out.writeInt(state.disconnects()); out.writeInt(state.monthlyPeriod());
                out.writeLong(state.monthlyRankingPoints());
                out.writeInt(state.monthlyWins()); out.writeInt(state.monthlyLosses());
                writeIntegerList(out, state.legacyCertificateCodes());
                writeIntegerList(out, state.victoryBadgeEditions());
                out.writeBoolean(state.litoMaster());
                out.writeUTF(state.classicServerName());
                out.writeInt(state.channel()); out.writeInt(state.sourceLevel());
                out.writeInt(state.race()); out.writeBoolean(state.rankingSnapshotPresent());
                out.writeLong(state.snapshotRankingPoints());
                out.writeInt(state.snapshotWins()); out.writeInt(state.snapshotLosses());
                out.writeUTF(state.snapshotClassicServerName());
                out.writeInt(state.snapshotChannel());
                out.writeInt(state.snapshotSourceLevel()); out.writeInt(state.snapshotRace());
                out.writeBoolean(state.monthlyRankingSnapshotPresent());
                out.writeInt(state.snapshotMonthlyPeriod());
                out.writeLong(state.snapshotMonthlyRankingPoints());
                out.writeInt(state.snapshotMonthlyWins());
                out.writeInt(state.snapshotMonthlyLosses());
                out.writeInt(state.snapshotMonthlyMovement());
                out.writeInt(state.snapshotOverallMovement());
                out.writeInt(state.snapshotClassicServerMovement());
                out.writeInt(state.snapshotChannelMovement());
                out.writeInt(state.snapshotLevelMovement());
                out.writeInt(state.snapshotRaceMovement());
                out.writeLong(state.experience()); out.writeInt(state.level());
                out.writeLong(state.rankingSnapshotBoundaryEpochSeconds());
                out.writeLong(state.monthlyRankingSnapshotBoundaryEpochSeconds());
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new SQLException("cannot encode Tournament profile", impossible);
        }
    }

    private static GameStateStore.TournamentProfileState decodeTournamentProfile(String encoded)
            throws SQLException {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded == null ? "" : encoded);
            try (ByteArrayInputStream source = new ByteArrayInputStream(bytes);
                 DataInputStream in = new DataInputStream(source)) {
                if (in.readInt() != 1) throw new IOException("unsupported profile format");
                GameStateStore.TournamentProfileState state =
                        new GameStateStore.TournamentProfileState(
                                in.readLong(), in.readInt(), in.readInt(), in.readInt(),
                                in.readInt(), in.readLong(), in.readInt(), in.readInt(),
                                readIntegerList(in), readIntegerList(in), in.readBoolean(),
                                in.readUTF(), in.readInt(), in.readInt(), in.readInt(),
                                in.readBoolean(), in.readLong(), in.readInt(), in.readInt(),
                                in.readUTF(), in.readInt(), in.readInt(), in.readInt(),
                                in.readBoolean(), in.readInt(), in.readLong(), in.readInt(),
                                in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                                in.readInt(), in.readInt(), in.readInt(), in.readLong(),
                                in.readInt(), in.readLong(), in.readLong());
                if (source.available() != 0) throw new IOException("trailing profile data");
                return state;
            }
        } catch (IOException | IllegalArgumentException invalid) {
            throw new SQLException("cannot decode Tournament profile", invalid);
        }
    }

    private static void writeIntegerList(DataOutputStream out, List<Integer> values)
            throws IOException {
        out.writeInt(values.size());
        for (int value : values) out.writeInt(value);
    }

    private static List<Integer> readIntegerList(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > 64) throw new IOException("invalid integer-list size");
        List<Integer> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) values.add(in.readInt());
        return values;
    }

    private record StoredBoardTopic(GameStateStore.BoardTopic topic, long authorObjectId) {}

    private StoredBoardTopic loadBoardTopic(Connection connection, long topicId)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT id,name,date,title,content "
                        + "FROM `server_盟屋_佈告` WHERE id=? LIMIT 1")) {
            query.setLong(1, topicId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? boardTopicFromRow(connection, row) : null;
            }
        }
    }

    private StoredBoardTopic boardTopicFromRow(Connection connection, ResultSet row)
            throws SQLException {
        String authorName = row.getString("name");
        long authorObjectId = characterObjectId(connection, authorName);
        try {
            GameStateStore.BoardTopic topic = new GameStateStore.BoardTopic(
                    classicSourceName, row.getLong("id"), authorName,
                    GameStateStore.sourceCharacter(classicSourceName, authorName),
                    row.getString("date"), row.getString("title"),
                    row.getString("content"));
            Protocol.boardRead(topic);
            return new StoredBoardTopic(topic, authorObjectId);
        } catch (IllegalArgumentException invalid) {
            System.err.println("[LiTo] skipped unsafe shared native-board row source="
                    + classicSourceName + " id=" + row.getLong("id")
                    + " reason=" + invalid.getMessage());
            return null;
        }
    }

    private static long nextBoardTopicId(Connection connection) throws SQLException {
        try (Statement query = connection.createStatement();
             ResultSet row = query.executeQuery(
                     "SELECT COALESCE(MAX(id),0)+1 AS next_id "
                             + "FROM `server_盟屋_佈告`")) {
            if (!row.next()) throw new SQLException("cannot allocate native board topic id");
            long next = row.getLong("next_id");
            if (next <= 0 || next >= Protocol.BOARD_INITIAL_CURSOR) {
                throw new SQLException("native board topic-id range exhausted");
            }
            return next;
        }
    }

    private static long characterObjectId(Connection connection, String characterName)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT objid FROM characters WHERE char_name=? LIMIT 1")) {
            query.setString(1, characterName);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getLong(1) : 0;
            }
        }
    }

    private static boolean fitsNativeHouseBoardColumns(
            String authorName, String date, String title, String content) {
        return mysqlCharacterLength(authorName) <= 16
                && mysqlCharacterLength(date) <= 16
                && mysqlCharacterLength(title) <= 16
                && mysqlCharacterLength(content) <= 1000;
    }

    private static int mysqlCharacterLength(String value) {
        return value == null ? Integer.MAX_VALUE
                : value.codePointCount(0, value.length());
    }

    private static String characterName(Connection connection, long characterObjectId)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT char_name FROM characters WHERE objid=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getString(1) : null;
            }
        }
    }

    private static void insertPhysicalLetterItem(Connection connection, long characterObjectId,
                                                 long objectId, long catalogId)
            throws SQLException {
        String sql = "INSERT INTO character_items (id,item_id,char_id,item_name,count,"
                + "is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,bless) "
                + "VALUES (?,?,?,?,1,0,0,0,1,0,0,0)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setLong(1, objectId);
            insert.setLong(2, catalogId);
            insert.setLong(3, characterObjectId);
            insert.setString(4, "$1075");
            insert.executeUpdate();
        }
    }

    private static void compensatePhysicalLetterInsert(
            Connection connection, long objectId, long receiverObjectId,
            SQLException failure) {
        if (objectId <= 0) return;
        String[] statements = {
                "DELETE FROM character_letter WHERE item_object_id=?",
                "DELETE FROM lito_item_origin WHERE item_obj_id=?",
                "DELETE FROM character_items WHERE id=? AND char_id=?"
        };
        for (int index = 0; index < statements.length; index++) {
            try (PreparedStatement cleanup = connection.prepareStatement(statements[index])) {
                cleanup.setLong(1, objectId);
                if (index == 2) cleanup.setLong(2, receiverObjectId);
                cleanup.executeUpdate();
            } catch (SQLException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    @Override public RepositoryWarehouseSnapshot warehouse(
            GameStateStore.WarehouseScope scope, String owner) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryWarehouseSnapshot.unavailable();
        }
        if (scope == null || owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("warehouse scope/owner is required");
        }
        String normalizedOwner = owner.trim();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return new RepositoryWarehouseSnapshot(true,
                    sourceInventoryRows(connection, warehouseInventorySql(scope),
                            normalizedOwner));
        }
    }

    @Override public RepositoryWarehousePassword warehousePassword(String account)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryWarehousePassword.unavailable();
        }
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("warehouse password account is required");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT warehouse FROM accounts WHERE login=? LIMIT 1")) {
            query.setString(1, account.trim());
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return new RepositoryWarehousePassword(true, 0);
                int value = row.getInt(1);
                return new RepositoryWarehousePassword(
                        true, value <= 0 || value > 999_999 ? 0 : value);
            }
        }
    }

    @Override public RepositoryWarehousePasswordChange changeWarehousePassword(
            String account, int expectedOld, int replacement) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryWarehousePasswordChange.unavailable();
        }
        if (account == null || account.isBlank() || expectedOld < 0
                || expectedOld > 999_999 || replacement < 0 || replacement > 999_999) {
            throw new IllegalArgumentException("invalid SQL warehouse password mutation");
        }
        if (readOnly) {
            return new RepositoryWarehousePasswordChange(
                    true, GameStateStore.WarehousePasswordStatus.UNCHANGED);
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES accounts WRITE");
            try {
                int current;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT warehouse FROM accounts WHERE login=? LIMIT 1")) {
                    query.setString(1, account.trim());
                    try (ResultSet row = query.executeQuery()) {
                        if (!row.next()) {
                            return new RepositoryWarehousePasswordChange(true,
                                    GameStateStore.WarehousePasswordStatus.WRONG_OLD);
                        }
                        int raw = row.getInt(1);
                        current = raw <= 0 || raw > 999_999 ? 0 : raw;
                    }
                }
                if (current != expectedOld) {
                    return new RepositoryWarehousePasswordChange(true,
                            GameStateStore.WarehousePasswordStatus.WRONG_OLD);
                }
                if (current == replacement) {
                    return new RepositoryWarehousePasswordChange(true,
                            GameStateStore.WarehousePasswordStatus.UNCHANGED);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE accounts SET warehouse=? WHERE login=?")) {
                    update.setInt(1, replacement == 0 ? -256 : replacement);
                    update.setString(2, account.trim());
                    if (update.executeUpdate() != 1) {
                        return new RepositoryWarehousePasswordChange(true,
                                GameStateStore.WarehousePasswordStatus.WRONG_OLD);
                    }
                }
                return new RepositoryWarehousePasswordChange(true,
                        GameStateStore.WarehousePasswordStatus.CHANGED);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryWarehouseTransferResult persistWarehouseTransfer(
            long characterObjectId, GameStateStore.WarehouseScope scope, String owner,
            RuntimeInventory beforeInventory, RuntimeInventory beforeWarehouse,
            RuntimeInventory afterInventory, RuntimeInventory afterWarehouse,
            Map<Long, Long> localToSourceObjectIds) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryWarehouseTransferResult.READ_ONLY;
        }
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL
                || scope == null || owner == null || owner.isBlank()
                || beforeInventory == null || beforeWarehouse == null
                || afterInventory == null || afterWarehouse == null) {
            throw new IllegalArgumentException("invalid SQL warehouse transfer");
        }
        Map<Long, Long> sourceIds = localToSourceObjectIds == null
                ? Map.of() : Map.copyOf(localToSourceObjectIds);
        Map<Long, PositionedWarehouseItem> before = positionedWarehouseItems(
                beforeInventory, beforeWarehouse, sourceIds);
        Map<Long, PositionedWarehouseItem> after = positionedWarehouseItems(
                afterInventory, afterWarehouse, sourceIds);
        Set<Long> changed = new LinkedHashSet<>();
        changed.addAll(before.keySet());
        changed.addAll(after.keySet());
        changed.removeIf(id -> java.util.Objects.equals(before.get(id), after.get(id)));
        if (changed.isEmpty()) return RepositoryWarehouseTransferResult.SUCCESS;

        String warehouseTable = warehouseTable(scope);
        String ownerColumn = warehouseOwnerColumn(scope);
        String normalizedOwner = owner.trim();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            for (long sqlObjectId : changed) {
                PositionedWarehouseItem next = after.get(sqlObjectId);
                if (next != null && !resourceItemTemplateExists(
                        connection, Math.toIntExact(next.item().definition().catalogId()))) {
                    return RepositoryWarehouseTransferResult.UNSUPPORTED_ITEM;
                }
            }
            try (Statement lock = connection.createStatement()) {
                lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                        + "character_items_time WRITE, character_pets WRITE, "
                        + "lito_item_origin WRITE, lito_warehouse_item_origin WRITE, "
                        + warehouseTable + " WRITE");
                try {
                    if (!characterRowExists(connection, characterObjectId)) {
                        return RepositoryWarehouseTransferResult.NOT_FOUND;
                    }
                    if (!warehouseSignature(connection, warehouseTable, ownerColumn,
                            normalizedOwner).equals(runtimeWarehouseSignature(
                                    beforeWarehouse, sourceIds))) {
                        return RepositoryWarehouseTransferResult.CONFLICT;
                    }
                    Map<Long, SqlStoredItem> stored = new LinkedHashMap<>();
                    for (long sqlObjectId : changed) {
                        PositionedWarehouseItem old = before.get(sqlObjectId);
                        SqlStoredItem row = old == null ? null : loadStoredItem(
                                connection, warehouseTable, ownerColumn, normalizedOwner,
                                characterObjectId, sqlObjectId, old.warehouse());
                        if (old != null && old.item().sourceBound() && row == null) {
                            return RepositoryWarehouseTransferResult.CONFLICT;
                        }
                        if (row != null && (row.itemId() != Math.toIntExact(
                                old.item().definition().catalogId())
                                || row.count() != old.item().quantity())) {
                            return RepositoryWarehouseTransferResult.CONFLICT;
                        }
                        stored.put(sqlObjectId, row);
                    }
                    for (long sqlObjectId : changed) {
                        PositionedWarehouseItem old = before.get(sqlObjectId);
                        PositionedWarehouseItem next = after.get(sqlObjectId);
                        SqlStoredItem metadata = stored.get(sqlObjectId);
                        boolean owned = litoItemOriginExists(connection, sqlObjectId)
                                || litoWarehouseItemOriginExists(connection, sqlObjectId)
                                || next != null && (!next.item().sourceBound()
                                    || next.item().litoOwned());
                        deleteStoredItem(connection, warehouseTable, ownerColumn,
                                normalizedOwner, characterObjectId, sqlObjectId,
                                old == null ? null : old.warehouse());
                        deleteItemOrigins(connection, sqlObjectId);
                        if (next == null) {
                            try (PreparedStatement deletePet = connection.prepareStatement(
                                    "DELETE FROM character_pets WHERE item_obj_id=?")) {
                                deletePet.setLong(1, sqlObjectId);
                                deletePet.executeUpdate();
                            }
                            continue;
                        }
                        if (next.warehouse()) {
                            insertWarehouseItem(connection, warehouseTable, ownerColumn,
                                    normalizedOwner, sqlObjectId, next.item(), metadata);
                            if (owned) upsertWarehouseItemOrigin(connection, sqlObjectId,
                                    scope, normalizedOwner, next.item());
                        } else {
                            insertCharacterItem(connection, characterObjectId, sqlObjectId,
                                    next.item(), metadata);
                            if (owned) upsertCharacterItemOrigin(connection, sqlObjectId,
                                    characterObjectId, next.item());
                        }
                    }
                    return RepositoryWarehouseTransferResult.SUCCESS;
                } finally {
                    lock.execute("UNLOCK TABLES");
                }
            }
        }
    }

    private record PositionedWarehouseItem(RuntimeInventory.Item item, boolean warehouse) {}

    private record SqlStoredItem(
            int itemId, String itemName, long count, int attrEnchantKind,
            int attrEnchantLevel, String gamNo, int greater) {}

    private static Map<Long, PositionedWarehouseItem> positionedWarehouseItems(
            RuntimeInventory inventory, RuntimeInventory warehouse,
            Map<Long, Long> sourceIds) {
        Map<Long, PositionedWarehouseItem> result = new LinkedHashMap<>();
        addPositionedItems(result, inventory, false, sourceIds);
        addPositionedItems(result, warehouse, true, sourceIds);
        return Map.copyOf(result);
    }

    private static void addPositionedItems(
            Map<Long, PositionedWarehouseItem> destination, RuntimeInventory source,
            boolean warehouse, Map<Long, Long> sourceIds) {
        for (RuntimeInventory.Item item : source.items()) {
            long sqlObjectId = sourceIds.getOrDefault(item.objectId(), item.objectId());
            if (sqlObjectId <= 0 || sqlObjectId > Integer.MAX_VALUE
                    || destination.putIfAbsent(sqlObjectId,
                            new PositionedWarehouseItem(item, warehouse)) != null) {
                throw new IllegalArgumentException(
                        "duplicate/out-of-range SQL warehouse object id: " + sqlObjectId);
            }
        }
    }

    private static Map<Long, String> runtimeWarehouseSignature(
            RuntimeInventory warehouse, Map<Long, Long> sourceIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (RuntimeInventory.Item item : warehouse.items()) {
            long id = sourceIds.getOrDefault(item.objectId(), item.objectId());
            result.put(id, warehouseSignature(item));
        }
        return result;
    }

    private static Map<Long, String> warehouseSignature(
            Connection connection, String table, String ownerColumn, String owner)
            throws SQLException {
        String sql = "SELECT id,item_id,count,is_sealed,enchantlvl,is_id,durability,"
                + "charge_count,bless FROM " + table + " WHERE " + ownerColumn
                + "=? ORDER BY id";
        Map<Long, String> result = new LinkedHashMap<>();
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, owner);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    long id = row.getLong("id");
                    String signature = row.getInt("item_id") + ":" + row.getLong("count")
                            + ":" + row.getBoolean("is_sealed") + ":"
                            + row.getInt("enchantlvl") + ":" + row.getBoolean("is_id")
                            + ":" + row.getInt("durability") + ":"
                            + row.getInt("charge_count") + ":" + row.getInt("bless");
                    if (result.putIfAbsent(id, signature) != null) {
                        throw new SQLException("duplicate warehouse object id: " + id);
                    }
                }
            }
        }
        return result;
    }

    private static String warehouseSignature(RuntimeInventory.Item item) {
        return item.definition().catalogId() + ":" + item.quantity() + ":"
                + item.sealed() + ":" + item.enchantLevel() + ":"
                + item.identified() + ":" + item.durability() + ":"
                + item.chargeCount() + ":" + item.blessing();
    }

    private static SqlStoredItem loadStoredItem(
            Connection connection, String warehouseTable, String ownerColumn, String owner,
            long characterObjectId, long objectId, boolean warehouse) throws SQLException {
        String sql = warehouse
                ? "SELECT item_id,item_name,count,attr_enchant_kind,attr_enchant_level,"
                    + "gamno,greater FROM " + warehouseTable + " WHERE id=? AND "
                    + ownerColumn + "=? LIMIT 1"
                : "SELECT item_id,item_name,count,attr_enchant_kind,attr_enchant_level,"
                    + "gamno,greater FROM character_items WHERE id=? AND char_id=? LIMIT 1";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, objectId);
            if (warehouse) query.setString(2, owner);
            else query.setLong(2, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                return new SqlStoredItem(row.getInt("item_id"), row.getString("item_name"),
                        row.getLong("count"), row.getInt("attr_enchant_kind"),
                        row.getInt("attr_enchant_level"), row.getString("gamno"),
                        row.getInt("greater"));
            }
        }
    }

    private static void deleteStoredItem(
            Connection connection, String warehouseTable, String ownerColumn, String owner,
            long characterObjectId, long objectId, Boolean warehouse) throws SQLException {
        if (warehouse != null) {
            String sql = warehouse
                    ? "DELETE FROM " + warehouseTable + " WHERE id=? AND "
                        + ownerColumn + "=?"
                    : "DELETE FROM character_items WHERE id=? AND char_id=?";
            try (PreparedStatement delete = connection.prepareStatement(sql)) {
                delete.setLong(1, objectId);
                if (warehouse) delete.setString(2, owner);
                else delete.setLong(2, characterObjectId);
                delete.executeUpdate();
            }
        }
        try (PreparedStatement deleteTime = connection.prepareStatement(
                "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
            deleteTime.setLong(1, objectId);
            deleteTime.executeUpdate();
        }
    }

    private static void insertCharacterItem(
            Connection connection, long characterObjectId, long objectId,
            RuntimeInventory.Item item, SqlStoredItem metadata) throws SQLException {
        String sql = "INSERT INTO character_items "
                + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,enchantlvl,"
                + "is_id,durability,charge_count,remaining_time,last_used,bless,"
                + "attr_enchant_kind,attr_enchant_level,gamno,greater) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,0,NULL,?,?,?,?,?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            int index = 1;
            insert.setLong(index++, objectId);
            insert.setInt(index++, Math.toIntExact(item.definition().catalogId()));
            insert.setLong(index++, characterObjectId);
            insert.setString(index++, storedName(item, metadata));
            insert.setInt(index++, item.quantity());
            insert.setBoolean(index++, item.equipped());
            insert.setBoolean(index++, item.sealed());
            insert.setInt(index++, item.enchantLevel());
            insert.setBoolean(index++, item.identified());
            insert.setInt(index++, item.durability());
            insert.setInt(index++, item.chargeCount());
            insert.setInt(index++, item.blessing());
            insert.setInt(index++, metadata == null ? 0 : metadata.attrEnchantKind());
            insert.setInt(index++, metadata == null ? 0 : metadata.attrEnchantLevel());
            insert.setString(index++, metadata == null ? null : metadata.gamNo());
            insert.setInt(index, metadata == null ? 0 : metadata.greater());
            insert.executeUpdate();
        }
        if (item.expiresAtEpochMillis() > 0) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO character_items_time (itemr_obj_id,usertime) VALUES (?,?)")) {
                insert.setLong(1, objectId);
                insert.setTimestamp(2, new java.sql.Timestamp(item.expiresAtEpochMillis()));
                insert.executeUpdate();
            }
        }
    }

    private static void insertWarehouseItem(
            Connection connection, String table, String ownerColumn, String owner,
            long objectId, RuntimeInventory.Item item, SqlStoredItem metadata)
            throws SQLException {
        String sql = "INSERT INTO " + table + " (id," + ownerColumn
                + ",item_id,item_name,count,is_equipped,is_sealed,enchantlvl,is_id,"
                + "durability,charge_count,remaining_time,last_used,bless,"
                + "attr_enchant_kind,attr_enchant_level,gamno,greater) "
                + "VALUES (?,?,?,?,?,0,?,?,?,?,?,?,NULL,?,?,?,?,?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            int index = 1;
            insert.setLong(index++, objectId);
            insert.setString(index++, owner);
            insert.setInt(index++, Math.toIntExact(item.definition().catalogId()));
            insert.setString(index++, storedName(item, metadata));
            insert.setInt(index++, item.quantity());
            insert.setBoolean(index++, item.sealed());
            insert.setInt(index++, item.enchantLevel());
            insert.setBoolean(index++, item.identified());
            insert.setInt(index++, item.durability());
            insert.setInt(index++, item.chargeCount());
            long remaining = item.expiresAtEpochMillis() <= 0 ? 0
                    : Math.max(1L, (item.expiresAtEpochMillis()
                            - System.currentTimeMillis() + 999L) / 1000L);
            insert.setInt(index++, (int) Math.min(Integer.MAX_VALUE, remaining));
            insert.setInt(index++, item.blessing());
            insert.setInt(index++, metadata == null ? 0 : metadata.attrEnchantKind());
            insert.setInt(index++, metadata == null ? 0 : metadata.attrEnchantLevel());
            insert.setString(index++, metadata == null ? null : metadata.gamNo());
            insert.setInt(index, metadata == null ? 0 : metadata.greater());
            insert.executeUpdate();
        }
    }

    private static String storedName(RuntimeInventory.Item item, SqlStoredItem metadata) {
        String retained = metadata == null ? null : metadata.itemName();
        return retained == null || retained.isBlank() ? item.definition().name() : retained;
    }

    private static boolean litoWarehouseItemOriginExists(
            Connection connection, long objectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM lito_warehouse_item_origin WHERE item_obj_id=? LIMIT 1")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) { return row.next(); }
        }
    }

    private static void deleteItemOrigins(Connection connection, long objectId)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM lito_item_origin WHERE item_obj_id=?")) {
            delete.setLong(1, objectId);
            delete.executeUpdate();
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM lito_warehouse_item_origin WHERE item_obj_id=?")) {
            delete.setLong(1, objectId);
            delete.executeUpdate();
        }
    }

    private static void upsertCharacterItemOrigin(
            Connection connection, long objectId, long characterObjectId,
            RuntimeInventory.Item item) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO lito_item_origin "
                        + "(item_obj_id,char_obj_id,catalog_id,tournament_reward,created_at) "
                        + "VALUES (?,?,?,?,NOW())")) {
            insert.setLong(1, objectId);
            insert.setLong(2, characterObjectId);
            insert.setInt(3, Math.toIntExact(item.definition().catalogId()));
            insert.setBoolean(4, item.tournamentReward());
            insert.executeUpdate();
        }
    }

    private static void upsertWarehouseItemOrigin(
            Connection connection, long objectId, GameStateStore.WarehouseScope scope,
            String owner, RuntimeInventory.Item item) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO lito_warehouse_item_origin "
                        + "(item_obj_id,scope,owner_key,catalog_id,tournament_reward,created_at) "
                        + "VALUES (?,?,?,?,?,NOW())")) {
            insert.setLong(1, objectId);
            insert.setString(2, scope.name());
            insert.setString(3, owner);
            insert.setInt(4, Math.toIntExact(item.definition().catalogId()));
            insert.setBoolean(5, item.tournamentReward());
            insert.executeUpdate();
        }
    }

    private List<SourceInventoryItem> sourceInventoryRows(
            Connection connection, String sql, Object owner) throws SQLException {
        List<SourceInventoryItem> result = new ArrayList<>();
        Set<Long> sourceObjectIds = new LinkedHashSet<>();
        Map<Integer, SourceMagicDollPower> dollPowers = sourceMagicDollPowers(connection);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 3; index++) statement.setObject(index, owner);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    long sourceObjectId = row.getLong("source_object_id");
                    if (!sourceObjectIds.add(sourceObjectId)) {
                        throw new SQLException("selected-source inventory object joins more than "
                                + "one template: " + sourceObjectId);
                    }
                    long sourceCount = row.getLong("item_count");
                    if (sourceCount <= 0 || sourceCount > Integer.MAX_VALUE) {
                        throw new SQLException("selected-source item count is outside native "
                                + "runtime range: " + sourceObjectId);
                    }
                    try {
                        result.add(sourceInventoryItem(
                                row, dollPowers, sourceObjectId, (int) sourceCount));
                    } catch (IllegalArgumentException invalid) {
                        throw new SQLException("invalid selected-source inventory/template row: "
                                + sourceObjectId, invalid);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static SourceInventoryItem sourceInventoryItem(
            ResultSet row, Map<Integer, SourceMagicDollPower> dollPowers,
            long sourceObjectId, int sourceCount) throws SQLException {
        int catalogId = row.getInt("item_id");
        ClassicWorldDropCatalog.ItemWire wire = new ClassicWorldDropCatalog.ItemWire(
                catalogId, row.getString("item_kind"), row.getString("name_id"),
                row.getString("use_token"), row.getInt("invgfx"), row.getInt("grdgfx"),
                row.getInt("template_bless"), row.getBoolean("stackable"),
                row.getBoolean("tradable"), row.getInt("unit_weight"),
                row.getInt("itemdesc_id"), row.getString("weapon_type"),
                row.getInt("dmg_small"), row.getInt("dmg_large"),
                row.getString("material"), row.getInt("safenchant"),
                row.getInt("class_mask"), row.getBoolean("cant_delete"));
        InventoryCatalog.NativeArmorStatus armorStatus =
                !"armor".equals(row.getString("item_kind")) ? null
                        : new InventoryCatalog.NativeArmorStatus(
                                row.getInt("armor_ac"),
                                ClassicWorldDropCatalog.materialCode(row.getString("material")),
                                row.getInt("unit_weight"), row.getInt("class_mask"),
                                row.getInt("safenchant"), row.getInt("armor_str"),
                                row.getInt("armor_dex"), row.getInt("armor_con"),
                                row.getInt("armor_wis"), row.getInt("armor_int"),
                                row.getInt("armor_cha"), row.getInt("armor_hp"),
                                row.getInt("armor_mp"), row.getInt("armor_mr"),
                                row.getInt("armor_sp"), row.getBoolean("armor_haste"),
                                row.getInt("armor_fire"), row.getInt("armor_water"),
                                row.getInt("armor_wind"), row.getInt("armor_earth"),
                                row.getInt("armor_blind"), row.getInt("armor_sleep"),
                                row.getInt("armor_freeze"), row.getInt("armor_stone"),
                                row.getInt("armor_sustain"), row.getInt("armor_stun"),
                                ClassicWorldDropCatalog.armorTypeCode(
                                        row.getString("use_token")));
        SourceMagicDollType magicDoll = null;
        if (row.getObject("doll_item_id") != null) {
            List<Integer> powerIds = commaSeparatedPositiveInts(
                    row.getString("doll_powers"), "powers");
            List<SourceMagicDollPower> powerDefinitions = new ArrayList<>();
            for (int powerId : powerIds) {
                SourceMagicDollPower power = dollPowers.get(powerId);
                if (power == null) {
                    throw new SQLException("magic-doll type references missing "
                            + "etcitem_doll_power_new id: " + powerId);
                }
                powerDefinitions.add(power);
            }
            magicDoll = new SourceMagicDollType(
                    row.getInt("doll_item_id"), row.getInt("doll_type"),
                    row.getInt("doll_icon"), powerIds,
                    commaSeparatedPositiveInts(row.getString("doll_material_ids"), "need"),
                    commaSeparatedPositiveInts(row.getString("doll_material_counts"), "count"),
                    row.getInt("doll_duration"), row.getInt("doll_gfx"),
                    row.getString("doll_name"), powerDefinitions);
        }
        java.sql.Timestamp expiresAt = row.getTimestamp("expires_at");
        return new SourceInventoryItem(
                sourceObjectId, catalogId, sourceCount, row.getBoolean("is_equipped"),
                row.getInt("enchantlvl"), row.getInt("durability"),
                row.getBoolean("is_identified"), row.getInt("instance_bless"),
                row.getBoolean("linked_pet_collar"), wire, armorStatus,
                expiresAt == null ? 0L : expiresAt.getTime(),
                row.getBoolean("instance_sealed"), magicDoll,
                row.getBoolean("lito_owned"), row.getBoolean("tournament_reward"),
                Math.max(0, Math.min(0xFF, row.getInt("charge_count"))));
    }

    @Override public SourceEquippedFireResistance equippedFireResistance(
            long characterObjectId) throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        // Only immutable, persisted equipment is imported.  Live classic
        // buffs, custom cards/talents and other process-local additions are
        // intentionally not guessed; LiTo applies its own match statuses on
        // top of this qualified snapshot.
        String sql = """
                SELECT COALESCE(SUM(a.defense_fire), 0) AS fire_resistance,
                       COUNT(a.item_id) AS matched_armor_rows
                  FROM character_items i
                  JOIN %s a ON a.item_id = i.item_id
                 WHERE i.char_id = ? AND i.is_equipped = 1
                """.formatted(resourceTable("armor"));
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("equipped fire-resistance aggregate returned no row");
                }
                int resistance = NumericBreakthrough.clampElementalResistance(
                        row.getLong("fire_resistance"));
                int matchedRows = row.getInt("matched_armor_rows");
                if (matchedRows < 0) {
                    throw new SQLException("invalid equipped armor row count");
                }
                return new SourceEquippedFireResistance(resistance, matchedRows);
            }
        }
    }

    @Override public SourceBaseMagicResistance baseMagicResistance(
            long characterObjectId) throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        String sql = """
                SELECT c.Type, c.level, c.Wis, c.OriginalWis,
                       COALESCE(w.`魔防`, 0) AS wisdom_mr
                  FROM characters c
                  LEFT JOIN %s w ON w.id = LEAST(GREATEST(c.Wis, 0), 50)
                 WHERE c.objid = ?
                 LIMIT 1
                """.formatted(exactResourceTable("角色_精神成長"));
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("base MR source character was not found");
                }
                int wisdom = row.getInt("Wis");
                int wisdomMr = row.getInt("wisdom_mr");
                if (wisdom >= 0 && wisdom <= 50
                        && wisdomMr != BattleMagicResistance.wisdomMagicResistance(wisdom)) {
                    throw new SQLException("source wisdom MR resource differs from traced 3.81 table");
                }
                return SourceBaseMagicResistance.from(
                        row.getInt("Type"), row.getInt("level"), wisdomMr,
                        row.getInt("OriginalWis"));
            }
        }
    }

    @Override public SourceEquipmentMagicStats equipmentMagicStats(
            long characterObjectId) throws SQLException {
        return equipmentEvaluation(characterObjectId).magic();
    }

    @Override public Set<Integer> counterMagicBypassSkills() throws SQLException {
        Set<Integer> skills = new LinkedHashSet<>();
        String sql = "SELECT skill_id FROM "
                + resourceDatabase + ".`skills_魔屏無效技能`"
                + " ORDER BY skill_id";
        try (Connection connection = connection(
                    accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            while (row.next()) {
                int skillId = row.getInt("skill_id");
                if (skillId <= 0) {
                    throw new SQLException(
                            "invalid Counter Magic bypass skill id: " + skillId);
                }
                skills.add(skillId);
            }
        }
        return Set.copyOf(skills);
    }

    @Override public Map<Integer, Integer> skillReuseDelays() throws SQLException {
        Map<Integer, Integer> delays = new LinkedHashMap<>();
        String sql = "SELECT skill_id,reuseDelay FROM "
                + resourceTable("skills") + " ORDER BY skill_id";
        try (Connection connection = connection(
                    accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            while (row.next()) {
                int skillId = row.getInt("skill_id");
                int reuseDelay = row.getInt("reuseDelay");
                if (skillId <= 0 || reuseDelay < 0
                        || delays.put(skillId, reuseDelay) != null) {
                    throw new SQLException(
                            "invalid or duplicate source skill reuse row: " + skillId);
                }
            }
        }
        return Map.copyOf(delays);
    }

    @Override public SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId) throws SQLException {
        return equipmentEvaluation(characterObjectId, Map.of());
    }

    @Override public SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId, Map<Long, Boolean> equippedOverrides)
            throws SQLException {
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return LegacyEquipmentMagicLoader.loadEvaluation(
                    connection, resourceDatabase, characterObjectId,
                    System.currentTimeMillis(), equippedOverrides);
        }
    }

    @Override public SourcePhysicalGrowthStats physicalGrowthStats() throws SQLException {
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return new SourcePhysicalGrowthStats(
                    physicalGrowthColumn(connection, "角色_力量成長", "命中"),
                    physicalGrowthColumn(connection, "角色_力量成長", "傷害"),
                    physicalGrowthColumn(connection, "角色_敏捷成長", "命中"),
                    physicalGrowthColumn(connection, "角色_敏捷成長", "傷害"));
        }
    }

    @Override public SourceNpcPhysicalStats npcPhysicalStats(int npcId) throws SQLException {
        if (npcId <= 0) throw new IllegalArgumentException("invalid source NPC id");
        SourceNpcPhysicalStats cached = npcPhysicalCache.get(npcId);
        if (cached != null) return cached;
        String sql = "SELECT npcid,ac,size,damage_reduction,hard,undead,weakAttr,mr,IsTU,IsErase,lawful,"
                + "tamable,boss,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk FROM "
                + resourceTable("npc") + " WHERE npcid=? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, npcId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("source NPC physical row was not found: " + npcId);
                }
                SourceNpcPhysicalStats loaded;
                try {
                    loaded = SourceNpcPhysicalStats.fromSourceRow(
                            row.getInt("npcid"), row.getInt("ac"), row.getString("size"),
                            row.getInt("damage_reduction"), row.getBoolean("hard"),
                            row.getInt("undead"), row.getInt("weakAttr"), row.getInt("mr"),
                            row.getBoolean("IsTU"), row.getInt("lawful"),
                            row.getBoolean("tamable"), row.getBoolean("boss"),
                            row.getInt("mp"), row.getInt("str"), row.getInt("dex"),
                            row.getBoolean("IsErase"), row.getInt("ranged"),
                            row.getInt("bowActId"), row.getInt("poison_atk"),
                            row.getInt("paralysis_atk"));
                } catch (IllegalArgumentException invalidRow) {
                    throw new SQLException(
                            "invalid source NPC physical row: " + npcId, invalidRow);
                }
                SourceNpcPhysicalStats existing = npcPhysicalCache.putIfAbsent(npcId, loaded);
                return existing == null ? loaded : existing;
            }
        }
    }

    @Override public SourceSummonTemplate summonTemplate(int npcId) throws SQLException {
        if (!SourceSummonTemplate.supportedNpcId(npcId)) {
            throw new IllegalArgumentException("invalid source summon NPC id");
        }
        SourceSummonTemplate cached = summonTemplateCache.get(npcId);
        if (cached != null) return cached;
        String sql = "SELECT npcid,name,nameid,gfxid,lvl,hp,mp,ac,str,dex,passispeed,atkspeed FROM "
                + resourceTable("npc") + " WHERE npcid=? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, npcId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("source summon NPC row was not found: " + npcId);
                }
                SourceSummonTemplate loaded;
                try {
                    loaded = new SourceSummonTemplate(
                            row.getInt("npcid"), row.getString("name"),
                            row.getString("nameid"), row.getInt("gfxid"),
                            row.getInt("lvl"), row.getInt("hp"), row.getInt("mp"),
                            row.getInt("ac"), row.getInt("str"), row.getInt("dex"),
                            row.getInt("passispeed"), row.getInt("atkspeed"));
                } catch (IllegalArgumentException invalidRow) {
                    throw new SQLException(
                            "invalid source summon NPC row: " + npcId, invalidRow);
                }
                SourceSummonTemplate existing = summonTemplateCache.putIfAbsent(
                        npcId, loaded);
                return existing == null ? loaded : existing;
            }
        }
    }

    @Override public List<SourcePet> pets(long characterObjectId) throws SQLException {
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        String ownershipJoin = repositoryMode.equals("lito_mysql")
                ? "LEFT JOIN lito_item_origin o ON o.item_obj_id=p.item_obj_id " : "";
        String ownershipProjection = repositoryMode.equals("lito_mysql")
                ? "IF(o.item_obj_id IS NULL,0,1) AS lito_owned " : "0 AS lito_owned ";
        String sql = "SELECT p.item_obj_id,p.objid,p.npcid,p.name,p.lvl,p.hp,p.mp,p.exp,"
                + "p.lawful,i.item_id," + ownershipProjection + "FROM character_pets p "
                + "JOIN character_items i ON i.id=p.item_obj_id "
                + ownershipJoin
                + "WHERE i.char_id=? AND i.item_id IN (?,?) ORDER BY p.item_obj_id";
        List<SourcePet> result = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            statement.setLong(2, InventoryCatalog.PET_COLLAR);
            statement.setLong(3, InventoryCatalog.HIGH_PET_COLLAR);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    try {
                        result.add(new SourcePet(
                                row.getLong("item_obj_id"), row.getLong("objid"),
                                row.getInt("item_id"), row.getInt("npcid"),
                                row.getString("name"), row.getInt("lvl"),
                                row.getInt("hp"), row.getInt("mp"),
                                row.getLong("exp"), row.getInt("lawful"),
                                row.getBoolean("lito_owned")));
                    } catch (IllegalArgumentException invalidRow) {
                        throw new SQLException("invalid selected-source pet row for character: "
                                + characterObjectId, invalidRow);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    @Override public SourcePetTemplate petTemplate(int npcId) throws SQLException {
        if (npcId <= 0) throw new IllegalArgumentException("invalid source pet NPC id");
        SourcePetTemplate cached = petTemplateCache.get(npcId);
        if (cached != null) return cached;
        String sql = "SELECT n.npcid,n.name,n.nameid,n.gfxid,n.lvl,n.hp,n.mp,n.ac,"
                + "n.str,n.dex,n.passispeed,n.atkspeed,p.CollarId,p.ChaCost,"
                + "p.ItemIdForTaming,p.TameRate,"
                + "p.HpUpMin,p.HpUpMax,p.MpUpMin,p.MpUpMax,p.MinEvoLevel,"
                + "p.EvolutionItemId,p.NpcIdForEvolving,n.con,n.wis,n.intel,n.mr,"
                + "n.exp,n.lawful,n.size,n.weakAttr,n.ranged,n.bowActId,n.undead,"
                + "n.poison_atk,n.paralysis_atk,n.agro,n.damage_reduction,n.hard,"
                + "n.doppel,n.boss,n.light_size,n.change_head FROM "
                + resourceTable("npc") + " n JOIN " + resourceTable("pettypes")
                + " p ON p.BaseNpcId=n.npcid WHERE n.npcid=? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, npcId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("source pet NPC row was not found: " + npcId);
                }
                SourcePetTemplate loaded;
                try {
                    loaded = new SourcePetTemplate(
                            row.getInt("npcid"), row.getString("name"),
                            row.getString("nameid"), row.getInt("gfxid"),
                            row.getInt("lvl"), row.getInt("hp"), row.getInt("mp"),
                            row.getInt("ac"), row.getInt("str"), row.getInt("dex"),
                            row.getInt("passispeed"), row.getInt("atkspeed"),
                            row.getInt("CollarId"), row.getInt("ChaCost"),
                            row.getInt("ItemIdForTaming"), row.getInt("TameRate"),
                            row.getInt("HpUpMin"), row.getInt("HpUpMax"),
                            row.getInt("MpUpMin"), row.getInt("MpUpMax"),
                            row.getInt("MinEvoLevel"), row.getInt("EvolutionItemId"),
                            row.getInt("NpcIdForEvolving"), row.getInt("con"),
                            row.getInt("wis"), row.getInt("intel"), row.getInt("mr"),
                            row.getLong("exp"), row.getInt("lawful"),
                            row.getString("size"), row.getInt("weakAttr"),
                            row.getInt("ranged"), row.getInt("bowActId"),
                            row.getInt("undead"), row.getInt("poison_atk"),
                            row.getInt("paralysis_atk"), row.getBoolean("agro"),
                            row.getInt("damage_reduction"), row.getBoolean("hard"),
                            row.getBoolean("doppel"), row.getBoolean("boss"),
                            row.getInt("light_size"), row.getBoolean("change_head"));
                } catch (IllegalArgumentException invalidRow) {
                    throw new SQLException("invalid source pet NPC row: " + npcId, invalidRow);
                }
                SourcePetTemplate existing = petTemplateCache.putIfAbsent(npcId, loaded);
                return existing == null ? loaded : existing;
            }
        }
    }

    @Override public SourceInventoryWeightStats inventoryWeightStats(
            long characterObjectId) throws SQLException {
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return LegacyInventoryWeightLoader.load(
                    connection, resourceDatabase, characterObjectId, serverWeightRate);
        }
    }

    @Override public RepositoryClanSnapshot clan(long characterObjectId) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryClanSnapshot.unavailable();
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadClanSnapshot(connection, characterObjectId);
        }
    }

    @Override public RepositoryCharacterTitle characterTitle(long characterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCharacterTitle.unavailable();
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadCharacterTitle(connection, characterObjectId);
        }
    }

    @Override public RepositoryMarriageState marriage(long characterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryMarriageState.unavailable();
        }
        if (!validMarriageObjectId(characterObjectId)) {
            return RepositoryMarriageState.notFound();
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadMarriageState(connection, characterObjectId);
        }
    }

    @Override public RepositoryMarriageMutation persistMarriage(
            long firstCharacterObjectId, long secondCharacterObjectId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryMarriageMutation.unavailable();
        }
        if (!validMarriageObjectId(firstCharacterObjectId)
                || !validMarriageObjectId(secondCharacterObjectId)
                || firstCharacterObjectId == secondCharacterObjectId) {
            return RepositoryMarriageMutation.invalid();
        }
        if (readOnly) {
            return marriageMutation(RepositoryMarriageMutationStatus.READ_ONLY,
                    marriage(firstCharacterObjectId), marriage(secondCharacterObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE");
            try {
                RepositoryMarriageState first = loadMarriageState(
                        connection, firstCharacterObjectId);
                RepositoryMarriageState second = loadMarriageState(
                        connection, secondCharacterObjectId);
                if (first.status() == RepositoryMarriageStateStatus.NOT_FOUND
                        || second.status() == RepositoryMarriageStateStatus.NOT_FOUND) {
                    return marriageMutation(
                            RepositoryMarriageMutationStatus.NOT_FOUND, first, second);
                }
                if (!first.consistent() || !second.consistent()) {
                    return marriageMutation(
                            RepositoryMarriageMutationStatus.STALE, first, second);
                }
                if (first.married() || second.married()) {
                    return marriageMutation(
                            RepositoryMarriageMutationStatus.ALREADY_MARRIED, first, second);
                }

                boolean firstChanged = false;
                try {
                    if (updateMarriagePartner(connection, firstCharacterObjectId,
                            0, secondCharacterObjectId) != 1) {
                        return marriageMutation(RepositoryMarriageMutationStatus.STALE,
                                loadMarriageState(connection, firstCharacterObjectId),
                                loadMarriageState(connection, secondCharacterObjectId));
                    }
                    firstChanged = true;
                    if (updateMarriagePartner(connection, secondCharacterObjectId,
                            0, firstCharacterObjectId) != 1) {
                        requireMarriageCompensation(connection, firstCharacterObjectId,
                                secondCharacterObjectId, 0, null);
                        return marriageMutation(RepositoryMarriageMutationStatus.STALE,
                                loadMarriageState(connection, firstCharacterObjectId),
                                loadMarriageState(connection, secondCharacterObjectId));
                    }
                } catch (SQLException failure) {
                    if (firstChanged) {
                        requireMarriageCompensation(connection, firstCharacterObjectId,
                                secondCharacterObjectId, 0, failure);
                    }
                    throw failure;
                }
                first = loadMarriageState(connection, firstCharacterObjectId);
                second = loadMarriageState(connection, secondCharacterObjectId);
                if (!first.married() || first.partnerObjectId() != secondCharacterObjectId
                        || !second.married()
                        || second.partnerObjectId() != firstCharacterObjectId) {
                    throw new SQLException("marriage relation is not reciprocal after update");
                }
                return marriageMutation(
                        RepositoryMarriageMutationStatus.SUCCESS, first, second);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryMarriageMutation persistDivorce(
            long characterObjectId, long expectedPartnerObjectId) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryMarriageMutation.unavailable();
        }
        if (!validMarriageObjectId(characterObjectId)
                || !validMarriageObjectId(expectedPartnerObjectId)
                || characterObjectId == expectedPartnerObjectId) {
            return RepositoryMarriageMutation.invalid();
        }
        if (readOnly) {
            return marriageMutation(RepositoryMarriageMutationStatus.READ_ONLY,
                    marriage(characterObjectId), marriage(expectedPartnerObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE");
            try {
                RepositoryMarriageState first = loadMarriageState(
                        connection, characterObjectId);
                RepositoryMarriageState second = loadMarriageState(
                        connection, expectedPartnerObjectId);
                if (first.status() == RepositoryMarriageStateStatus.NOT_FOUND
                        || second.status() == RepositoryMarriageStateStatus.NOT_FOUND) {
                    return marriageMutation(
                            RepositoryMarriageMutationStatus.NOT_FOUND, first, second);
                }
                if (!first.married() || first.partnerObjectId() != expectedPartnerObjectId
                        || !second.married()
                        || second.partnerObjectId() != characterObjectId) {
                    return marriageMutation(
                            RepositoryMarriageMutationStatus.STALE, first, second);
                }

                boolean firstChanged = false;
                try {
                    if (updateMarriagePartner(connection, characterObjectId,
                            expectedPartnerObjectId, 0) != 1) {
                        return marriageMutation(RepositoryMarriageMutationStatus.STALE,
                                loadMarriageState(connection, characterObjectId),
                                loadMarriageState(connection, expectedPartnerObjectId));
                    }
                    firstChanged = true;
                    if (updateMarriagePartner(connection, expectedPartnerObjectId,
                            characterObjectId, 0) != 1) {
                        requireMarriageCompensation(connection, characterObjectId,
                                0, expectedPartnerObjectId, null);
                        return marriageMutation(RepositoryMarriageMutationStatus.STALE,
                                loadMarriageState(connection, characterObjectId),
                                loadMarriageState(connection, expectedPartnerObjectId));
                    }
                } catch (SQLException failure) {
                    if (firstChanged) {
                        requireMarriageCompensation(connection, characterObjectId,
                                0, expectedPartnerObjectId, failure);
                    }
                    throw failure;
                }
                first = loadMarriageState(connection, characterObjectId);
                second = loadMarriageState(connection, expectedPartnerObjectId);
                if (!first.consistent() || first.married()
                        || !second.consistent() || second.married()) {
                    throw new SQLException("marriage relation remains after divorce update");
                }
                return marriageMutation(
                        RepositoryMarriageMutationStatus.SUCCESS, first, second);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryTitleMutation persistCharacterTitle(
            long characterObjectId, String expectedValue, String replacementValue)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryTitleMutation.unavailable();
        }
        String expected = expectedValue == null ? "" : expectedValue;
        String replacement = replacementValue == null ? "" : replacementValue;
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL
                || expected.length() > 35 || replacement.length() > 35) {
            return new RepositoryTitleMutation(RepositoryTitleMutationStatus.CONFLICT,
                    RepositoryCharacterTitle.missing());
        }
        if (readOnly) {
            return RepositoryTitleMutation.readOnly(characterTitle(characterObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE");
            try {
                RepositoryCharacterTitle current =
                        loadCharacterTitle(connection, characterObjectId);
                if (!current.present()) {
                    return new RepositoryTitleMutation(
                            RepositoryTitleMutationStatus.NOT_FOUND, current);
                }
                if (!current.title().equals(expected)) {
                    return new RepositoryTitleMutation(
                            RepositoryTitleMutationStatus.CONFLICT, current);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE characters SET Title=? WHERE objid=?")) {
                    update.setString(1, replacement);
                    update.setLong(2, characterObjectId);
                    if (update.executeUpdate() != 1) {
                        return new RepositoryTitleMutation(
                                RepositoryTitleMutationStatus.CONFLICT,
                                loadCharacterTitle(connection, characterObjectId));
                    }
                }
                return new RepositoryTitleMutation(
                        RepositoryTitleMutationStatus.SUCCESS,
                        RepositoryCharacterTitle.present(replacement));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryClanMutation createClan(
            long ownerCharacterObjectId, String requestedName) throws SQLException {
        String clanName = requestedName == null ? "" : requestedName.trim();
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryClanMutation.unavailable();
        }
        if (clanName.isBlank() || clanName.length() > 45) {
            return new RepositoryClanMutation(RepositoryClanMutationStatus.CONFLICT,
                    RepositoryClanSnapshot.empty());
        }
        if (readOnly) {
            return RepositoryClanMutation.readOnly(clan(ownerCharacterObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES server_info WRITE, characters WRITE, "
                    + "character_items READ, character_pets READ, clan_data WRITE, "
                    + "lito_clan_metadata WRITE");
            long clanId = 0;
            try {
                CharacterClanRow owner = characterClanRow(
                        connection, ownerCharacterObjectId, null);
                if (owner == null) {
                    return new RepositoryClanMutation(
                            RepositoryClanMutationStatus.NOT_FOUND,
                            RepositoryClanSnapshot.empty());
                }
                if (owner.clanId() != 0) {
                    return new RepositoryClanMutation(
                            RepositoryClanMutationStatus.OWNER_ALREADY_MEMBER,
                            loadClanSnapshot(connection, ownerCharacterObjectId));
                }
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT clan_id FROM clan_data WHERE clan_name=? LIMIT 1")) {
                    query.setString(1, clanName);
                    try (ResultSet row = query.executeQuery()) {
                        if (row.next()) {
                            return new RepositoryClanMutation(
                                    RepositoryClanMutationStatus.DUPLICATE_NAME,
                                    RepositoryClanSnapshot.empty());
                        }
                    }
                }
                clanId = allocateClanObjectId(connection);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO clan_data (clan_id,clan_name,leader_id,leader_name,"
                                + "hascastle,hashouse,max_online_user,showEmblem,"
                                + "Announcement,ClanBirthDay) "
                                + "VALUES (?,?,?,?,0,0,0,0,'',NOW())")) {
                    insert.setLong(1, clanId);
                    insert.setString(2, clanName);
                    insert.setLong(3, ownerCharacterObjectId);
                    insert.setString(4, owner.name());
                    insert.executeUpdate();
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE characters SET ClanID=?,Clanname=?,ClanRank=4 "
                                + "WHERE objid=? AND ClanID=0")) {
                    update.setLong(1, clanId);
                    update.setString(2, clanName);
                    update.setLong(3, ownerCharacterObjectId);
                    if (update.executeUpdate() != 1) {
                        throw new SQLException("clan owner changed while tables were locked");
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO lito_clan_metadata "
                                + "(clan_id,lito_owned,destroy_pending,snapshot_version) "
                                + "VALUES (?,1,0,1)")) {
                    insert.setLong(1, clanId);
                    insert.executeUpdate();
                }
                return new RepositoryClanMutation(
                        RepositoryClanMutationStatus.SUCCESS,
                        loadClanSnapshot(connection, ownerCharacterObjectId));
            } catch (SQLException failure) {
                if (clanId != 0) {
                    compensateClanCreation(
                            connection, ownerCharacterObjectId, clanId, failure);
                }
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryClanMutation joinClan(
            long ownerCharacterObjectId, String expectedApplicantName,
            int maximumMembers) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryClanMutation.unavailable();
        }
        if (readOnly) {
            return RepositoryClanMutation.readOnly(clan(ownerCharacterObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE, clan_data READ, "
                    + "lito_clan_metadata READ");
            try {
                CharacterClanRow owner = characterClanRow(
                        connection, ownerCharacterObjectId, null);
                if (owner == null) return clanMutation(
                        RepositoryClanMutationStatus.NOT_FOUND,
                        RepositoryClanSnapshot.empty());
                RepositoryClanSnapshot current = loadClanSnapshot(
                        connection, ownerCharacterObjectId);
                if (!current.present()
                        || current.leaderObjectId() != ownerCharacterObjectId) {
                    return clanMutation(RepositoryClanMutationStatus.NOT_OWNER, current);
                }
                CharacterClanRow applicant = characterClanRow(
                        connection, expectedApplicantName == null
                                ? "" : expectedApplicantName.trim());
                if (applicant == null) return clanMutation(
                        RepositoryClanMutationStatus.NOT_FOUND, current);
                if (applicant.clanId() != 0) return clanMutation(
                        RepositoryClanMutationStatus.APPLICANT_ALREADY_MEMBER, current);
                if (maximumMembers <= 0 || current.members().stream()
                        .filter(member -> member.role() != 3).count() >= maximumMembers) {
                    return clanMutation(RepositoryClanMutationStatus.FULL, current);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE characters SET ClanID=?,Clanname=?,ClanRank=2 "
                                + "WHERE objid=? AND ClanID=0")) {
                    update.setLong(1, current.clanId());
                    update.setString(2, current.name());
                    update.setLong(3, applicant.objectId());
                    if (update.executeUpdate() != 1) return clanMutation(
                            RepositoryClanMutationStatus.CONFLICT, current);
                }
                return clanMutation(RepositoryClanMutationStatus.SUCCESS,
                        loadClanSnapshot(connection, ownerCharacterObjectId));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryClanMutation changeClanMember(
            long ownerCharacterObjectId, String requestedMemberName,
            int expectedRole, int replacementRole) throws SQLException {
        String memberName = requestedMemberName == null ? "" : requestedMemberName.trim();
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryClanMutation.unavailable();
        }
        if (memberName.isBlank() || expectedRole < 1 || expectedRole > 4
                || replacementRole < 0 || replacementRole > 3) {
            return clanMutation(RepositoryClanMutationStatus.CONFLICT,
                    RepositoryClanSnapshot.empty());
        }
        if (readOnly) {
            return RepositoryClanMutation.readOnly(clan(ownerCharacterObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE, clan_data READ, "
                    + "lito_clan_metadata READ");
            try {
                RepositoryClanSnapshot current = loadClanSnapshot(
                        connection, ownerCharacterObjectId);
                if (!current.present()
                        || current.leaderObjectId() != ownerCharacterObjectId) {
                    return clanMutation(RepositoryClanMutationStatus.NOT_OWNER, current);
                }
                RepositoryClanMember member = current.members().stream()
                        .filter(candidate -> candidate.name().equalsIgnoreCase(memberName))
                        .findFirst().orElse(null);
                if (member == null) return clanMutation(
                        RepositoryClanMutationStatus.NOT_FOUND, current);
                if (member.objectId() == current.leaderObjectId()) return clanMutation(
                        RepositoryClanMutationStatus.NOT_OWNER, current);
                if (member.role() != expectedRole) return clanMutation(
                        RepositoryClanMutationStatus.CONFLICT, current);
                String sql = replacementRole == 0
                        ? "UPDATE characters SET ClanID=0,Clanname='',ClanRank=0 "
                                + "WHERE objid=? AND ClanID=? AND ClanRank=?"
                        : "UPDATE characters SET ClanRank=? "
                                + "WHERE objid=? AND ClanID=? AND ClanRank=?";
                try (PreparedStatement update = connection.prepareStatement(sql)) {
                    int index = 0;
                    if (replacementRole != 0) {
                        update.setInt(++index, nativeClanRank(replacementRole));
                    }
                    update.setLong(++index, member.objectId());
                    update.setLong(++index, current.clanId());
                    update.setInt(++index, member.nativeRank());
                    if (update.executeUpdate() != 1) return clanMutation(
                            RepositoryClanMutationStatus.CONFLICT,
                            loadClanSnapshot(connection, ownerCharacterObjectId));
                }
                return clanMutation(RepositoryClanMutationStatus.SUCCESS,
                        loadClanSnapshot(connection, ownerCharacterObjectId));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryClanMutation setClanDestroyPending(
            long ownerCharacterObjectId, boolean expected, boolean replacement)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryClanMutation.unavailable();
        }
        if (readOnly) {
            return RepositoryClanMutation.readOnly(clan(ownerCharacterObjectId));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, clan_data READ, "
                    + "lito_clan_metadata WRITE");
            try {
                RepositoryClanSnapshot current = loadClanSnapshot(
                        connection, ownerCharacterObjectId);
                if (!current.present()
                        || current.leaderObjectId() != ownerCharacterObjectId) {
                    return clanMutation(RepositoryClanMutationStatus.NOT_OWNER, current);
                }
                if (current.destroyPending() != expected) return clanMutation(
                        RepositoryClanMutationStatus.CONFLICT, current);
                try (PreparedStatement update = connection.prepareStatement(
                        "INSERT INTO lito_clan_metadata "
                                + "(clan_id,lito_owned,destroy_pending,snapshot_version) "
                                + "VALUES (?,0,?,1) ON DUPLICATE KEY UPDATE "
                                + "destroy_pending=VALUES(destroy_pending),"
                                + "snapshot_version=snapshot_version+1")) {
                    update.setLong(1, current.clanId());
                    update.setBoolean(2, replacement);
                    update.executeUpdate();
                }
                return clanMutation(RepositoryClanMutationStatus.SUCCESS,
                        loadClanSnapshot(connection, ownerCharacterObjectId));
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public boolean rollbackCreatedClan(
            long ownerCharacterObjectId, long clanId) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly
                || ownerCharacterObjectId <= 0 || clanId <= 0) return false;
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters WRITE, clan_data WRITE, "
                    + "lito_clan_metadata WRITE");
            try {
                RepositoryClanSnapshot current = loadClanSnapshot(
                        connection, ownerCharacterObjectId);
                if (!current.present() || current.clanId() != clanId
                        || current.leaderObjectId() != ownerCharacterObjectId
                        || current.castleId() != 0 || current.houseId() != 0
                        || current.members().size() != 1
                        || !litoOwnedClan(connection, clanId)) return false;
                try (PreparedStatement clear = connection.prepareStatement(
                        "UPDATE characters SET ClanID=0,Clanname='',ClanRank=0 "
                                + "WHERE objid=? AND ClanID=?")) {
                    clear.setLong(1, ownerCharacterObjectId);
                    clear.setLong(2, clanId);
                    if (clear.executeUpdate() != 1) return false;
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM clan_data WHERE clan_id=? AND leader_id=? "
                                + "AND hascastle=0 AND hashouse=0")) {
                    delete.setLong(1, clanId);
                    delete.setLong(2, ownerCharacterObjectId);
                    if (delete.executeUpdate() != 1) return false;
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_clan_metadata WHERE clan_id=? AND lito_owned=1")) {
                    delete.setLong(1, clanId);
                    return delete.executeUpdate() == 1;
                }
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private static RepositoryClanMutation clanMutation(
            RepositoryClanMutationStatus status, RepositoryClanSnapshot snapshot) {
        return new RepositoryClanMutation(status,
                snapshot == null ? RepositoryClanSnapshot.empty() : snapshot);
    }

    private record MarriageCharacterRow(
            long objectId, String name, long partnerObjectId) {}

    private static boolean validMarriageObjectId(long objectId) {
        // characters.PartnerID is the retained signed INT(10) column.
        return objectId > 0 && objectId <= Integer.MAX_VALUE;
    }

    private static RepositoryMarriageMutation marriageMutation(
            RepositoryMarriageMutationStatus status,
            RepositoryMarriageState first, RepositoryMarriageState second) {
        return new RepositoryMarriageMutation(status,
                first == null ? RepositoryMarriageState.unavailable() : first,
                second == null ? RepositoryMarriageState.unavailable() : second);
    }

    private static RepositoryMarriageState loadMarriageState(
            Connection connection, long characterObjectId) throws SQLException {
        MarriageCharacterRow character = loadMarriageCharacter(
                connection, characterObjectId);
        if (character == null) return RepositoryMarriageState.notFound();
        if (character.partnerObjectId() == 0) {
            return RepositoryMarriageState.available(character.objectId(),
                    character.name(), 0, "");
        }
        if (!validMarriageObjectId(character.partnerObjectId())) {
            return RepositoryMarriageState.conflict(character.objectId(),
                    character.name(), 0, "");
        }
        MarriageCharacterRow partner = loadMarriageCharacter(
                connection, character.partnerObjectId());
        if (partner == null) {
            return RepositoryMarriageState.conflict(character.objectId(),
                    character.name(), character.partnerObjectId(), "");
        }
        if (partner.partnerObjectId() != character.objectId()) {
            return RepositoryMarriageState.conflict(character.objectId(),
                    character.name(), partner.objectId(), partner.name());
        }
        return RepositoryMarriageState.available(character.objectId(), character.name(),
                partner.objectId(), partner.name());
    }

    private static MarriageCharacterRow loadMarriageCharacter(
            Connection connection, long characterObjectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT objid,char_name,COALESCE(PartnerID,0) AS partner_id "
                        + "FROM characters WHERE objid=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                String name = row.getString("char_name");
                long objectId = row.getLong("objid");
                long partnerObjectId = row.getLong("partner_id");
                if (!validMarriageObjectId(objectId) || name == null
                        || name.isBlank() || name.trim().length() > 45) {
                    throw new SQLException("invalid selected-source marriage character: "
                            + characterObjectId);
                }
                return new MarriageCharacterRow(
                        objectId, name.trim(), partnerObjectId);
            }
        }
    }

    private static int updateMarriagePartner(
            Connection connection, long characterObjectId,
            long expectedPartnerObjectId, long replacementPartnerObjectId)
            throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE characters SET PartnerID=? WHERE objid=? AND PartnerID=?")) {
            update.setLong(1, replacementPartnerObjectId);
            update.setLong(2, characterObjectId);
            update.setLong(3, expectedPartnerObjectId);
            return update.executeUpdate();
        }
    }

    private static void requireMarriageCompensation(
            Connection connection, long characterObjectId,
            long expectedPartnerObjectId, long replacementPartnerObjectId,
            SQLException primaryFailure) throws SQLException {
        try {
            if (updateMarriagePartner(connection, characterObjectId,
                    expectedPartnerObjectId, replacementPartnerObjectId) != 1) {
                throw new SQLException(
                        "cannot compensate the first characters.PartnerID row");
            }
        } catch (SQLException compensationFailure) {
            if (primaryFailure == null) throw compensationFailure;
            primaryFailure.addSuppressed(compensationFailure);
        }
    }

    private static RepositoryCharacterTitle loadCharacterTitle(
            Connection connection, long characterObjectId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT COALESCE(Title,'') FROM characters WHERE objid=? LIMIT 1")) {
            query.setLong(1, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return RepositoryCharacterTitle.missing();
                try {
                    return RepositoryCharacterTitle.present(row.getString(1));
                } catch (IllegalArgumentException invalid) {
                    throw new SQLException("invalid selected-source character title: "
                            + characterObjectId, invalid);
                }
            }
        }
    }

    private static RepositoryClanSnapshot loadClanSnapshot(
            Connection connection, long characterObjectId) throws SQLException {
        CharacterClanRow character = characterClanRow(connection, characterObjectId, null);
        if (character == null || character.clanId() == 0) {
            return RepositoryClanSnapshot.empty();
        }
        long clanId = character.clanId();
        String clanName;
        long leaderId;
        String leaderName;
        int castleId;
        int houseId;
        int maximumOnline;
        boolean showEmblem;
        String announcement;
        long birth;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT clan_name,leader_id,leader_name,hascastle,hashouse,"
                        + "max_online_user,showEmblem,Announcement,"
                        + "COALESCE(UNIX_TIMESTAMP(ClanBirthDay),0) AS birth_epoch "
                        + "FROM clan_data WHERE clan_id=? LIMIT 1")) {
            query.setLong(1, clanId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("character references missing clan_data row: " + clanId);
                }
                clanName = row.getString("clan_name");
                leaderId = row.getLong("leader_id");
                leaderName = row.getString("leader_name");
                castleId = Math.max(0, row.getInt("hascastle"));
                houseId = Math.max(0, row.getInt("hashouse"));
                maximumOnline = Math.max(0, row.getInt("max_online_user"));
                showEmblem = row.getInt("showEmblem") != 0;
                announcement = row.getString("Announcement");
                birth = Math.max(0, row.getLong("birth_epoch"));
            }
        }
        List<RepositoryClanMember> members = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT objid,char_name,ClanRank,Title,clanNote "
                        + "FROM characters WHERE ClanID=? ORDER BY objid")) {
            query.setLong(1, clanId);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    int nativeRank = row.getInt("ClanRank");
                    long objectId = row.getLong("objid");
                    int role = objectId == leaderId ? 4 : litoClanRole(nativeRank);
                    members.add(new RepositoryClanMember(objectId,
                            row.getString("char_name"), role, nativeRank,
                            row.getString("Title"), row.getString("clanNote")));
                }
            }
        }
        boolean destroyPending = false;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT destroy_pending FROM lito_clan_metadata WHERE clan_id=?")) {
            query.setLong(1, clanId);
            try (ResultSet row = query.executeQuery()) {
                destroyPending = row.next() && row.getInt(1) != 0;
            }
        }
        try {
            return new RepositoryClanSnapshot(true, clanId, clanName,
                    leaderId, leaderName, castleId, houseId, maximumOnline,
                    showEmblem, announcement, birth, destroyPending, members);
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid selected-source clan snapshot: " + clanId, invalid);
        }
    }

    private record CharacterClanRow(long objectId, String name, long clanId, int nativeRank) {}

    private static CharacterClanRow characterClanRow(
            Connection connection, long objectId, String expectedName) throws SQLException {
        String sql = expectedName == null
                ? "SELECT objid,char_name,ClanID,ClanRank FROM characters WHERE objid=? LIMIT 1"
                : "SELECT objid,char_name,ClanID,ClanRank FROM characters "
                        + "WHERE objid=? AND char_name=? LIMIT 1";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, objectId);
            if (expectedName != null) query.setString(2, expectedName);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? new CharacterClanRow(row.getLong("objid"),
                        row.getString("char_name"), row.getLong("ClanID"),
                        row.getInt("ClanRank")) : null;
            }
        }
    }

    private static CharacterClanRow characterClanRow(
            Connection connection, String characterName) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT objid,char_name,ClanID,ClanRank FROM characters "
                        + "WHERE char_name=? LIMIT 1")) {
            query.setString(1, characterName);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? new CharacterClanRow(row.getLong("objid"),
                        row.getString("char_name"), row.getLong("ClanID"),
                        row.getInt("ClanRank")) : null;
            }
        }
    }

    private static int litoClanRole(int nativeRank) throws SQLException {
        return switch (nativeRank) {
            case 1, 5, 8 -> 3;
            case 2, 7 -> 2;
            case 3, 6, 9 -> 1;
            case 4, 10 -> 4;
            default -> throw new SQLException("invalid native ClanRank: " + nativeRank);
        };
    }

    private static int nativeClanRank(int litoRole) {
        return switch (litoRole) {
            case 1 -> 3;
            case 2 -> 2;
            case 3 -> 1;
            default -> throw new IllegalArgumentException("invalid mutable LiTo clan role");
        };
    }

    private static long allocateClanObjectId(Connection connection) throws SQLException {
        try (Statement update = connection.createStatement()) {
            if (update.executeUpdate("UPDATE server_info SET maxid=LAST_INSERT_ID("
                    + "GREATEST(COALESCE(maxid,0),COALESCE(minid,10000),"
                    + "COALESCE((SELECT MAX(objid) FROM characters),0),"
                    + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                    + "COALESCE((SELECT MAX(objid) FROM character_pets),0),"
                    + "COALESCE((SELECT MAX(clan_id) FROM clan_data),0))+1) WHERE id=1") != 1) {
                throw new SQLException("server_info allocator row 1 is missing");
            }
            try (ResultSet row = update.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (!row.next()) throw new SQLException("clan object id allocation failed");
                long id = row.getLong(1);
                if (id <= 0 || id > Integer.MAX_VALUE) {
                    throw new SQLException("clan object id exhausted signed native range");
                }
                return id;
            }
        }
    }

    private static boolean litoOwnedClan(Connection connection, long clanId)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT lito_owned FROM lito_clan_metadata WHERE clan_id=?")) {
            query.setLong(1, clanId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() && row.getInt(1) != 0;
            }
        }
    }

    private static void compensateClanCreation(Connection connection,
            long ownerCharacterObjectId, long clanId, SQLException primary) {
        try (PreparedStatement clear = connection.prepareStatement(
                "UPDATE characters SET ClanID=0,Clanname='',ClanRank=0 "
                        + "WHERE objid=? AND ClanID=?")) {
            clear.setLong(1, ownerCharacterObjectId);
            clear.setLong(2, clanId);
            clear.executeUpdate();
        } catch (SQLException compensation) {
            primary.addSuppressed(compensation);
        }
        for (String sql : List.of(
                "DELETE FROM lito_clan_metadata WHERE clan_id=?",
                "DELETE FROM clan_data WHERE clan_id=?")) {
            try (PreparedStatement delete = connection.prepareStatement(sql)) {
                delete.setLong(1, clanId);
                delete.executeUpdate();
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
    }

    @Override public ClanEmblemData clanEmblem(String clanName) throws SQLException {
        if (clanName == null || clanName.isBlank()) return null;
        // The selected legacy schema stores the stable clan identity in
        // clan_data.clan_id and its optional cache-busting identity in
        // clan_emblem.emblem_id.  Older rows legitimately contain NULL;
        // same-family 3.81 servers use clan_id as the initial emblem id.
        String sql = """
                SELECT d.clan_id, e.emblem_id, e.emblemicon
                  FROM clan_data d
                  JOIN clan_emblem e ON e.clan_id = d.clan_id
                 WHERE d.clan_name = ?
                 LIMIT 1
                """;
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clanName);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                long emblemId = row.getLong("emblem_id");
                if (row.wasNull() || emblemId == 0) emblemId = row.getLong("clan_id");
                return new ClanEmblemData(emblemId, row.getBytes("emblemicon"));
            }
        }
    }

    @Override public CastleSummary castleForClan(String clanName) throws SQLException {
        if (clanName == null || clanName.isBlank()) return null;
        // The legacy schema is authoritative only for ownership and the
        // initial castle values. Runtime mutations are written exclusively
        // to LiTo's local sidecar; this repository deliberately exposes no
        // UPDATE path.
        String sql = """
                SELECT c.castle_id, d.clan_name, d.leader_name, d.leader_id, c.name,
                       COALESCE(UNIX_TIMESTAMP(c.war_time) * 1000, 0) AS war_time_millis,
                       c.tax_rate, c.public_money
                  FROM clan_data d
                  JOIN `server_城堡` c ON c.castle_id = d.hascastle
                 WHERE d.clan_name = ? AND d.hascastle <> 0
                 LIMIT 1
                """;
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clanName);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                return new CastleSummary(
                        row.getInt("castle_id"), row.getString("clan_name"),
                        row.getString("leader_name"), row.getString("name"),
                        Math.max(0, row.getLong("war_time_millis")),
                        Math.max(0, row.getInt("tax_rate")),
                        Math.max(0, row.getLong("public_money")),
                        Math.max(0, row.getLong("leader_id")));
            }
        }
    }

    @Override public CastleSummary castleById(int castleId) throws SQLException {
        if (castleId <= 0) return null;
        String sql = """
                SELECT c.castle_id, d.clan_name, d.leader_name, d.leader_id, c.name,
                       COALESCE(UNIX_TIMESTAMP(c.war_time) * 1000, 0) AS war_time_millis,
                       c.tax_rate, c.public_money
                  FROM `server_城堡` c
                  JOIN clan_data d ON d.hascastle = c.castle_id
                 WHERE c.castle_id = ?
                 LIMIT 1
                """;
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, castleId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                return new CastleSummary(row.getInt("castle_id"),
                        row.getString("clan_name"), row.getString("leader_name"),
                        row.getString("name"), Math.max(0, row.getLong("war_time_millis")),
                        Math.max(0, row.getInt("tax_rate")),
                        Math.max(0, row.getLong("public_money")),
                        Math.max(0, row.getLong("leader_id")));
            }
        }
    }

    @Override public List<CastleWarSchedule> castleWarSchedules() throws SQLException {
        String sql = """
                SELECT castle_id, name,
                       COALESCE(UNIX_TIMESTAMP(war_time) * 1000, 0) AS war_time_millis,
                       war_duration, war_interval
                  FROM `server_城堡`
                 ORDER BY castle_id
                """;
        List<CastleWarSchedule> schedules = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            while (row.next()) {
                try {
                    schedules.add(new CastleWarSchedule(
                            row.getInt("castle_id"), row.getString("name"),
                            Math.max(0, row.getLong("war_time_millis")),
                            row.getInt("war_duration"), row.getInt("war_interval")));
                } catch (IllegalArgumentException invalidRow) {
                    throw new SQLException("invalid selected-source castle-war row: "
                            + row.getInt("castle_id"), invalidRow);
                }
            }
        }
        if (schedules.size() != 8) {
            throw new SQLException("selected-source server_城堡 must contain 8 schedules");
        }
        for (int index = 0; index < schedules.size(); index++) {
            if (schedules.get(index).castleId() != index + 1) {
                throw new SQLException("selected-source castle-war ids must be contiguous 1..8");
            }
        }
        return List.copyOf(schedules);
    }

    @Override public RepositoryCastleSnapshot castleState(int castleId)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleSnapshot.unavailable();
        }
        if (castleId < 1 || castleId > 8) {
            throw new IllegalArgumentException("castle id must be 1..8");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            return loadCastleSnapshot(connection, castleId);
        }
    }

    @Override public RepositoryCastleMutation persistCastleState(
            long ownerCharacterObjectId, RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleMutation.unavailable();
        }
        if (expected == null || replacement == null || !expected.authoritative()
                || !replacement.authoritative()
                || expected.castleId() != replacement.castleId()
                || expected.warTimeMillis() != replacement.warTimeMillis()
                || expected.snapshotVersion() != replacement.snapshotVersion()) {
            return new RepositoryCastleMutation(
                    RepositoryCastleMutationStatus.CONFLICT,
                    expected == null ? RepositoryCastleSnapshot.unavailable() : expected);
        }
        if (readOnly) {
            return RepositoryCastleMutation.readOnly(castleState(expected.castleId()));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, clan_data READ, "
                    + "lito_clan_metadata READ, `server_城堡` WRITE, "
                    + "lito_castle_metadata WRITE");
            RepositoryCastleSnapshot current = RepositoryCastleSnapshot.unavailable();
            boolean nativeChanged = false;
            try {
                current = loadCastleSnapshot(connection, expected.castleId());
                if (!current.authoritative()) {
                    return new RepositoryCastleMutation(
                            RepositoryCastleMutationStatus.NOT_FOUND, current);
                }
                RepositoryClanSnapshot ownerClan = loadClanSnapshot(
                        connection, ownerCharacterObjectId);
                if (!ownerClan.present()
                        || ownerClan.leaderObjectId() != ownerCharacterObjectId
                        || ownerClan.castleId() != current.castleId()) {
                    return new RepositoryCastleMutation(
                            RepositoryCastleMutationStatus.NOT_OWNER, current);
                }
                if (!current.equals(expected)) {
                    return new RepositoryCastleMutation(
                            RepositoryCastleMutationStatus.CONFLICT, current);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `server_城堡` SET tax_rate=?,public_money=? "
                                + "WHERE castle_id=?")) {
                    update.setInt(1, replacement.taxRate());
                    update.setLong(2, replacement.publicMoney());
                    update.setInt(3, current.castleId());
                    if (update.executeUpdate() != 1) {
                        return new RepositoryCastleMutation(
                                RepositoryCastleMutationStatus.CONFLICT, current);
                    }
                    nativeChanged = true;
                }
                GameStateStore.MercenaryRoster mercenaries = replacement.mercenaries();
                try (PreparedStatement update = connection.prepareStatement(
                        "INSERT INTO lito_castle_metadata "
                                + "(castle_id,security_enabled,mercenary_0,mercenary_1,"
                                + "mercenary_2,mercenary_3,snapshot_version) "
                                + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                                + "security_enabled=VALUES(security_enabled),"
                                + "mercenary_0=VALUES(mercenary_0),"
                                + "mercenary_1=VALUES(mercenary_1),"
                                + "mercenary_2=VALUES(mercenary_2),"
                                + "mercenary_3=VALUES(mercenary_3),"
                                + "snapshot_version=VALUES(snapshot_version)")) {
                    update.setInt(1, current.castleId());
                    update.setBoolean(2, replacement.securityEnabled());
                    update.setInt(3, mercenaries.type0());
                    update.setInt(4, mercenaries.type1());
                    update.setInt(5, mercenaries.type2());
                    update.setInt(6, mercenaries.type3());
                    update.setLong(7, Math.addExact(current.snapshotVersion(), 1));
                    update.executeUpdate();
                }
                return new RepositoryCastleMutation(
                        RepositoryCastleMutationStatus.SUCCESS,
                        loadCastleSnapshot(connection, current.castleId()));
            } catch (ArithmeticException overflow) {
                throw new SQLException("castle snapshot version overflow", overflow);
            } catch (SQLException failure) {
                if (nativeChanged && current.authoritative()) {
                    compensateCastleMutation(connection, current, failure);
                }
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryCastleMutation persistCastleWarEnd(
            RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleMutation.unavailable();
        }
        if (expected == null || replacement == null
                || !expected.authoritative() || !replacement.authoritative()
                || expected.castleId() != replacement.castleId()
                || replacement.taxRate() != 10 || replacement.publicMoney() != 0
                || replacement.warTimeMillis() <= 0
                || expected.securityEnabled() != replacement.securityEnabled()
                || !expected.mercenaries().equals(replacement.mercenaries())
                || expected.snapshotVersion() != replacement.snapshotVersion()) {
            return new RepositoryCastleMutation(
                    RepositoryCastleMutationStatus.CONFLICT,
                    expected == null ? RepositoryCastleSnapshot.unavailable() : expected);
        }
        if (readOnly) {
            return RepositoryCastleMutation.readOnly(castleState(expected.castleId()));
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES `server_城堡` WRITE, lito_castle_metadata WRITE, "
                    + "clan_data READ, characters READ, `server_紀錄_獎勵` WRITE");
            RepositoryCastleSnapshot current = RepositoryCastleSnapshot.unavailable();
            boolean nativeChanged = false;
            CastleWarRewardTarget reward = null;
            boolean rewardInserted = false;
            try {
                current = loadCastleSnapshot(connection, expected.castleId());
                if (!current.authoritative()) {
                    return new RepositoryCastleMutation(
                            RepositoryCastleMutationStatus.NOT_FOUND, current);
                }
                if (!current.equals(expected)) {
                    return new RepositoryCastleMutation(
                            RepositoryCastleMutationStatus.CONFLICT, current);
                }
                reward = loadCastleWarRewardTarget(connection,
                        current.castleId(), expected.warTimeMillis());
                if (reward != null) {
                    rewardInserted = ensureCastleWarReward(connection, reward);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `server_城堡` SET war_time=FROM_UNIXTIME(?),"
                                + "tax_rate=10,public_money=0 WHERE castle_id=?")) {
                    update.setLong(1, replacement.warTimeMillis() / 1_000L);
                    update.setInt(2, current.castleId());
                    if (update.executeUpdate() != 1) {
                        return new RepositoryCastleMutation(
                                RepositoryCastleMutationStatus.CONFLICT, current);
                    }
                    nativeChanged = true;
                }
                writeCastleMetadata(connection, replacement,
                        Math.addExact(current.snapshotVersion(), 1));
                return new RepositoryCastleMutation(
                        RepositoryCastleMutationStatus.SUCCESS,
                        loadCastleSnapshot(connection, current.castleId()));
            } catch (ArithmeticException overflow) {
                SQLException failure = new SQLException(
                        "castle war-end snapshot version overflow", overflow);
                if (nativeChanged && current.authoritative()) {
                    compensateCastleMutation(connection, current, failure);
                }
                if (rewardInserted) compensateCastleWarReward(
                        connection, reward.eventKey(), failure);
                throw failure;
            } catch (SQLException failure) {
                if (nativeChanged && current.authoritative()) {
                    compensateCastleMutation(connection, current, failure);
                }
                if (rewardInserted) compensateCastleWarReward(
                        connection, reward.eventKey(), failure);
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryCastleSecuritySettlement prepareCastleSecurity(
            int castleId, long warStartMillis, long chargeAdena)
            throws SQLException {
        if (castleId < 1 || castleId > 7 || warStartMillis <= 0
                || chargeAdena <= 0 || chargeAdena > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "invalid SQL castle security settlement");
        }
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleSecuritySettlement.unavailable();
        }
        if (readOnly) {
            return RepositoryCastleSecuritySettlement.readOnly(
                    castleState(castleId), warStartMillis);
        }
        try (Connection connection = connection(
                     accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES `server_城堡` WRITE, "
                    + "lito_castle_metadata WRITE, "
                    + "lito_castle_security_settlement WRITE");
            RepositoryCastleSnapshot current = RepositoryCastleSnapshot.unavailable();
            boolean nativeChanged = false;
            try {
                current = loadCastleSnapshot(connection, castleId);
                if (!current.authoritative()) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.NOT_FOUND,
                            current, warStartMillis, 0);
                }
                if (!current.securityEnabled()) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.DISABLED,
                            current, warStartMillis, 0);
                }

                SqlCastleSecuritySettlement ledger = loadCastleSecuritySettlement(
                        connection, castleId, warStartMillis);
                if (ledger != null && ledger.chargeAdena() != chargeAdena) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.CONFLICT,
                            current, warStartMillis, ledger.chargedAdena());
                }
                if (ledger != null && ledger.completed()) {
                    RepositoryCastleSecuritySettlementStatus status =
                            ledger.chargedAdena() == 0
                                    ? RepositoryCastleSecuritySettlementStatus
                                            .INSUFFICIENT_FUNDS
                                    : RepositoryCastleSecuritySettlementStatus
                                            .ALREADY_SETTLED;
                    return castleSecuritySettlement(status, current,
                            warStartMillis, ledger.chargedAdena());
                }
                if (ledger != null && ledger.prepared()) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.READY,
                            current, warStartMillis, ledger.chargedAdena());
                }

                if (ledger == null) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO lito_castle_security_settlement "
                                    + "(castle_id,war_start_millis,treasury_before,"
                                    + "charge_adena,charged_adena,prepared,completed,"
                                    + "settled_at) VALUES (?,?,?,?,0,?,?,NOW())")) {
                        insert.setInt(1, castleId);
                        insert.setLong(2, warStartMillis);
                        insert.setLong(3, current.publicMoney());
                        insert.setLong(4, chargeAdena);
                        boolean affordable = current.publicMoney() >= chargeAdena;
                        insert.setBoolean(5, !affordable);
                        insert.setBoolean(6, !affordable);
                        if (insert.executeUpdate() != 1) {
                            return castleSecuritySettlement(
                                    RepositoryCastleSecuritySettlementStatus.CONFLICT,
                                    current, warStartMillis, 0);
                        }
                    }
                    ledger = loadCastleSecuritySettlement(
                            connection, castleId, warStartMillis);
                }
                if (ledger == null) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.CONFLICT,
                            current, warStartMillis, 0);
                }
                if (ledger.completed()) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                            current, warStartMillis, 0);
                }

                long chargedMoney = Math.subtractExact(
                        ledger.treasuryBefore(), chargeAdena);
                if (current.publicMoney() == ledger.treasuryBefore()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE `server_城堡` SET public_money=? "
                                    + "WHERE castle_id=? AND public_money=?")) {
                        update.setLong(1, chargedMoney);
                        update.setInt(2, castleId);
                        update.setLong(3, ledger.treasuryBefore());
                        if (update.executeUpdate() != 1) {
                            return castleSecuritySettlement(
                                    RepositoryCastleSecuritySettlementStatus.CONFLICT,
                                    current, warStartMillis, 0);
                        }
                    }
                    nativeChanged = true;
                    RepositoryCastleSnapshot replacement =
                            new RepositoryCastleSnapshot(true, current.castleId(),
                                    current.taxRate(), chargedMoney,
                                    current.warTimeMillis(), current.securityEnabled(),
                                    current.mercenaries(), current.snapshotVersion());
                    writeCastleMetadata(connection, replacement,
                            Math.addExact(current.snapshotVersion(), 1));
                } else if (current.publicMoney() == chargedMoney) {
                    // A process may have stopped after the native MyISAM row
                    // changed but before the ledger was marked prepared. A
                    // harmless version gap is preferable to preserving a
                    // potentially live pre-charge dialog capability.
                    writeCastleMetadata(connection, current,
                            Math.addExact(current.snapshotVersion(), 1));
                } else {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.CONFLICT,
                            current, warStartMillis, 0);
                }

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE lito_castle_security_settlement "
                                + "SET charged_adena=?,prepared=1,settled_at=NOW() "
                                + "WHERE castle_id=? AND war_start_millis=? "
                                + "AND prepared=0 AND completed=0")) {
                    update.setLong(1, chargeAdena);
                    update.setInt(2, castleId);
                    update.setLong(3, warStartMillis);
                    if (update.executeUpdate() != 1) {
                        SqlCastleSecuritySettlement concurrent =
                                loadCastleSecuritySettlement(
                                        connection, castleId, warStartMillis);
                        if (concurrent == null || !concurrent.prepared()
                                || concurrent.chargedAdena() != chargeAdena) {
                            throw new SQLException(
                                    "castle security ledger preparation conflict");
                        }
                    }
                }
                current = loadCastleSnapshot(connection, castleId);
                return castleSecuritySettlement(
                        RepositoryCastleSecuritySettlementStatus.READY,
                        current, warStartMillis, chargeAdena);
            } catch (ArithmeticException overflow) {
                SQLException failure = new SQLException(
                        "castle security settlement overflow", overflow);
                if (nativeChanged && current.authoritative()) {
                    compensateCastleMutation(connection, current, failure);
                }
                throw failure;
            } catch (SQLException failure) {
                if (nativeChanged && current.authoritative()) {
                    compensateCastleMutation(connection, current, failure);
                }
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryCastleSecuritySettlement completeCastleSecurity(
            int castleId, long warStartMillis) throws SQLException {
        if (castleId < 1 || castleId > 7 || warStartMillis <= 0) {
            throw new IllegalArgumentException(
                    "invalid SQL castle security completion");
        }
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleSecuritySettlement.unavailable();
        }
        if (readOnly) {
            return RepositoryCastleSecuritySettlement.readOnly(
                    castleState(castleId), warStartMillis);
        }
        try (Connection connection = connection(
                     accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES `server_城堡` READ, "
                    + "lito_castle_metadata READ, "
                    + "lito_castle_security_settlement WRITE");
            try {
                RepositoryCastleSnapshot current = loadCastleSnapshot(
                        connection, castleId);
                SqlCastleSecuritySettlement ledger = loadCastleSecuritySettlement(
                        connection, castleId, warStartMillis);
                if (ledger == null) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.NOT_FOUND,
                            current, warStartMillis, 0);
                }
                if (!ledger.prepared()) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.CONFLICT,
                            current, warStartMillis, ledger.chargedAdena());
                }
                if (ledger.chargedAdena() == 0) {
                    return castleSecuritySettlement(
                            RepositoryCastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                            current, warStartMillis, 0);
                }
                if (!ledger.completed()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE lito_castle_security_settlement SET completed=1,"
                                    + "settled_at=NOW() WHERE castle_id=? "
                                    + "AND war_start_millis=? AND prepared=1 "
                                    + "AND completed=0")) {
                        update.setInt(1, castleId);
                        update.setLong(2, warStartMillis);
                        if (update.executeUpdate() != 1) {
                            return castleSecuritySettlement(
                                    RepositoryCastleSecuritySettlementStatus.CONFLICT,
                                    current, warStartMillis,
                                    ledger.chargedAdena());
                        }
                    }
                }
                return castleSecuritySettlement(
                        RepositoryCastleSecuritySettlementStatus.ALREADY_SETTLED,
                        current, warStartMillis, ledger.chargedAdena());
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    @Override public RepositoryCastleOwnershipStatus persistCastleOwnership(
            long winnerLeaderObjectId, int castleId) throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleOwnershipStatus.UNAVAILABLE;
        }
        if (winnerLeaderObjectId <= 0 || winnerLeaderObjectId > 0xFFFF_FFFFL
                || castleId < 1 || castleId > 8) {
            throw new IllegalArgumentException("invalid SQL castle ownership mutation");
        }
        if (readOnly) return RepositoryCastleOwnershipStatus.READ_ONLY;
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, clan_data WRITE, "
                    + "lito_clan_metadata READ");
            long previousClanId = 0;
            long winnerClanId = 0;
            boolean previousCleared = false;
            boolean winnerAssigned = false;
            try {
                RepositoryClanSnapshot winner = loadClanSnapshot(
                        connection, winnerLeaderObjectId);
                if (!winner.present()) return RepositoryCastleOwnershipStatus.NOT_FOUND;
                if (winner.leaderObjectId() != winnerLeaderObjectId) {
                    return RepositoryCastleOwnershipStatus.NOT_OWNER;
                }
                if (winner.castleId() != 0) {
                    return RepositoryCastleOwnershipStatus.ALREADY_OWNS_CASTLE;
                }
                winnerClanId = winner.clanId();
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT clan_id FROM clan_data WHERE hascastle=? ORDER BY clan_id")) {
                    query.setInt(1, castleId);
                    try (ResultSet row = query.executeQuery()) {
                        if (row.next()) previousClanId = row.getLong("clan_id");
                        if (row.next()) return RepositoryCastleOwnershipStatus.CONFLICT;
                    }
                }
                if (previousClanId != 0) {
                    try (PreparedStatement clear = connection.prepareStatement(
                            "UPDATE clan_data SET hascastle=0 "
                                    + "WHERE clan_id=? AND hascastle=?")) {
                        clear.setLong(1, previousClanId);
                        clear.setInt(2, castleId);
                        if (clear.executeUpdate() != 1) {
                            return RepositoryCastleOwnershipStatus.CONFLICT;
                        }
                        previousCleared = true;
                    }
                }
                try (PreparedStatement assign = connection.prepareStatement(
                        "UPDATE clan_data SET hascastle=? "
                                + "WHERE clan_id=? AND leader_id=? AND hascastle=0")) {
                    assign.setInt(1, castleId);
                    assign.setLong(2, winnerClanId);
                    assign.setLong(3, winnerLeaderObjectId);
                    if (assign.executeUpdate() != 1) {
                        if (previousCleared) restoreCastleOwner(
                                connection, previousClanId, castleId, null);
                        return RepositoryCastleOwnershipStatus.CONFLICT;
                    }
                    winnerAssigned = true;
                }
                return RepositoryCastleOwnershipStatus.SUCCESS;
            } catch (SQLException failure) {
                if (winnerAssigned) clearCastleOwner(
                        connection, winnerClanId, castleId, failure);
                if (previousCleared) restoreCastleOwner(
                        connection, previousClanId, castleId, failure);
                throw failure;
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private static void clearCastleOwner(
            Connection connection, long clanId, int castleId, SQLException primary)
            throws SQLException {
        try (PreparedStatement clear = connection.prepareStatement(
                "UPDATE clan_data SET hascastle=0 WHERE clan_id=? AND hascastle=?")) {
            clear.setLong(1, clanId);
            clear.setInt(2, castleId);
            clear.executeUpdate();
        } catch (SQLException compensation) {
            if (primary != null) primary.addSuppressed(compensation);
            else throw compensation;
        }
    }

    private static void restoreCastleOwner(
            Connection connection, long clanId, int castleId, SQLException primary)
            throws SQLException {
        try (PreparedStatement restore = connection.prepareStatement(
                "UPDATE clan_data SET hascastle=? WHERE clan_id=? AND hascastle=0")) {
            restore.setInt(1, castleId);
            restore.setLong(2, clanId);
            restore.executeUpdate();
        } catch (SQLException compensation) {
            if (primary != null) primary.addSuppressed(compensation);
            else throw compensation;
        }
    }

    @Override public RepositoryPendingRewardSnapshot pendingRewards(String requestedAccount)
            throws SQLException {
        String account = requestedAccount == null ? ""
                : requestedAccount.trim().toLowerCase(Locale.ROOT);
        if (account.isBlank()) {
            throw new IllegalArgumentException("pending reward account is required");
        }
        List<RepositoryPendingReward> rewards = new ArrayList<>();
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT `流水號`,`玩家帳號`,`發送物品編號`,`發送物品數量`,"
                             + "`強化值` FROM `server_紀錄_獎勵` "
                             + "WHERE `玩家帳號`=? AND `領取狀態`=0 "
                             + "ORDER BY `流水號`")) {
            query.setString(1, account);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    int itemId = row.getInt("發送物品編號");
                    InventoryCatalog.Definition definition = rewardItemDefinition(
                            connection, itemId);
                    if (definition == null) {
                        throw new SQLException("pending reward references missing/ambiguous "
                                + "static item template: sequence=" + row.getLong("流水號")
                                + " item=" + itemId);
                    }
                    try {
                        rewards.add(new RepositoryPendingReward(
                                row.getLong("流水號"), row.getString("玩家帳號"),
                                definition, row.getInt("發送物品數量"),
                                row.getInt("強化值")));
                    } catch (IllegalArgumentException invalid) {
                        throw new SQLException("invalid pending reward row: sequence="
                                + row.getLong("流水號"), invalid);
                    }
                }
            }
        }
        return new RepositoryPendingRewardSnapshot(true, rewards);
    }

    @Override public RepositoryPendingRewardClaim claimPendingReward(
            long characterObjectId, String requestedAccount,
            RepositoryPendingReward expected, String requestedCharacterName,
            String requestedIp, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory, RuntimeInventory beforePersonalWarehouse,
            RuntimeInventory afterPersonalWarehouse,
            Map<Long, Long> localToSourceObjectIds) throws SQLException {
        String account = requestedAccount == null ? "" : requestedAccount.trim();
        String characterName = requestedCharacterName == null
                ? "" : requestedCharacterName.trim();
        String ip = requestedIp == null ? "" : requestedIp.trim();
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL
                || account.isBlank() || characterName.isBlank() || characterName.length() > 45
                || ip.length() > 45 || expected == null
                || !expected.account().equalsIgnoreCase(account)
                || beforeInventory == null || afterInventory == null
                || beforePersonalWarehouse == null || afterPersonalWarehouse == null) {
            throw new IllegalArgumentException("invalid SQL pending reward claim");
        }
        Map<Long, Long> sourceIds = localToSourceObjectIds == null
                ? Map.of() : Map.copyOf(localToSourceObjectIds);
        Map<Long, PositionedWarehouseItem> before = positionedWarehouseItems(
                beforeInventory, beforePersonalWarehouse, sourceIds);
        Map<Long, PositionedWarehouseItem> after = positionedWarehouseItems(
                afterInventory, afterPersonalWarehouse, sourceIds);
        Set<Long> changed = new LinkedHashSet<>();
        changed.addAll(before.keySet());
        changed.addAll(after.keySet());
        changed.removeIf(id -> java.util.Objects.equals(before.get(id), after.get(id)));
        Boolean personalWarehouse = validatePendingRewardDelta(
                expected, before, after, changed);
        if (personalWarehouse == null) {
            throw new IllegalArgumentException("inventory delta does not equal pending reward");
        }
        if (readOnly) {
            return new RepositoryPendingRewardClaim(
                    RepositoryPendingRewardClaimStatus.READ_ONLY, expected,
                    personalWarehouse);
        }
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryPendingRewardClaim.unavailable(expected);
        }

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            InventoryCatalog.Definition sourceDefinition = rewardItemDefinition(
                    connection, expected.itemId());
            if (sourceDefinition == null || !sourceDefinition.equals(expected.definition())) {
                return new RepositoryPendingRewardClaim(
                        RepositoryPendingRewardClaimStatus.UNSUPPORTED_ITEM,
                        expected, personalWarehouse);
            }
            try (Statement lock = connection.createStatement()) {
                lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                        + "character_items_time WRITE, character_pets READ, "
                        + "lito_item_origin WRITE, lito_warehouse_item_origin WRITE, "
                        + "character_warehouse WRITE, clan_warehouse READ, "
                        + "character_elf_warehouse READ, `server_紀錄_獎勵` WRITE");
                Map<Long, SqlStoredItem> stored = new LinkedHashMap<>();
                Map<Long, Boolean> owned = new LinkedHashMap<>();
                boolean inventoryChanged = false;
                try {
                    if (!characterIdentityMatches(
                            connection, characterObjectId, account, characterName)) {
                        return new RepositoryPendingRewardClaim(
                                RepositoryPendingRewardClaimStatus.NOT_FOUND,
                                expected, personalWarehouse);
                    }
                    RepositoryPendingReward current = loadPendingRewardLocked(
                            connection, expected.sequence(), expected.definition());
                    if (!expected.equals(current)) {
                        return new RepositoryPendingRewardClaim(
                                current == null
                                        ? RepositoryPendingRewardClaimStatus.NOT_FOUND
                                        : RepositoryPendingRewardClaimStatus.CONFLICT,
                                current == null ? expected : current, personalWarehouse);
                    }
                    if (personalWarehouse && !warehouseSignature(
                            connection, "character_warehouse", "account_name", account)
                            .equals(runtimeWarehouseSignature(
                                    beforePersonalWarehouse, sourceIds))) {
                        return new RepositoryPendingRewardClaim(
                                RepositoryPendingRewardClaimStatus.CONFLICT,
                                current, true);
                    }
                    for (long sqlObjectId : changed) {
                        PositionedWarehouseItem old = before.get(sqlObjectId);
                        SqlStoredItem row = old == null ? null : loadStoredItem(
                                connection, "character_warehouse", "account_name", account,
                                characterObjectId, sqlObjectId, old.warehouse());
                        if (old != null && (row == null || !storedItemStateMatches(
                                connection, "character_warehouse", "account_name", account,
                                characterObjectId, sqlObjectId, old))) {
                            return new RepositoryPendingRewardClaim(
                                    RepositoryPendingRewardClaimStatus.CONFLICT,
                                    current, personalWarehouse);
                        }
                        if (old == null && !sqlInventoryObjectIdAvailable(
                                connection, sqlObjectId)) {
                            return new RepositoryPendingRewardClaim(
                                    RepositoryPendingRewardClaimStatus.CONFLICT,
                                    current, personalWarehouse);
                        }
                        stored.put(sqlObjectId, row);
                        owned.put(sqlObjectId, old != null
                                && (litoItemOriginExists(connection, sqlObjectId)
                                || litoWarehouseItemOriginExists(connection, sqlObjectId)));
                    }
                    for (long sqlObjectId : changed) {
                        PositionedWarehouseItem old = before.get(sqlObjectId);
                        PositionedWarehouseItem next = after.get(sqlObjectId);
                        deleteStoredItem(connection, "character_warehouse", "account_name",
                                account, characterObjectId, sqlObjectId,
                                old == null ? null : old.warehouse());
                        deleteItemOrigins(connection, sqlObjectId);
                        if (next.warehouse()) {
                            insertWarehouseItem(connection, "character_warehouse",
                                    "account_name", account, sqlObjectId,
                                    next.item(), stored.get(sqlObjectId));
                            upsertWarehouseItemOrigin(connection, sqlObjectId,
                                    GameStateStore.WarehouseScope.PERSONAL,
                                    account, next.item());
                        } else {
                            insertCharacterItem(connection, characterObjectId,
                                    sqlObjectId, next.item(), stored.get(sqlObjectId));
                            upsertCharacterItemOrigin(connection, sqlObjectId,
                                    characterObjectId, next.item());
                        }
                        inventoryChanged = true;
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE `server_紀錄_獎勵` SET `領取狀態`=1,"
                                    + "`領取玩家角色`=?,`領取時間`=NOW(),`ip`=? "
                                    + "WHERE `流水號`=? AND `玩家帳號`=? "
                                    + "AND `發送物品編號`=? AND `發送物品數量`=? "
                                    + "AND `強化值`=? AND `領取狀態`=0")) {
                        update.setString(1, characterName);
                        update.setString(2, ip);
                        update.setLong(3, expected.sequence());
                        update.setString(4, account);
                        update.setInt(5, expected.itemId());
                        update.setInt(6, expected.count());
                        update.setInt(7, expected.enchant());
                        if (update.executeUpdate() != 1) {
                            SQLException conflict = new SQLException(
                                    "pending reward changed while tables were locked");
                            compensatePendingRewardInventory(connection, characterObjectId,
                                    account, changed, before, stored, owned, conflict);
                            if (conflict.getSuppressed().length > 0) {
                                throw conflict;
                            }
                            return new RepositoryPendingRewardClaim(
                                    RepositoryPendingRewardClaimStatus.CONFLICT,
                                    expected, personalWarehouse);
                        }
                    }
                    return new RepositoryPendingRewardClaim(
                            RepositoryPendingRewardClaimStatus.SUCCESS,
                            expected, personalWarehouse);
                } catch (SQLException failure) {
                    if (inventoryChanged) {
                        compensatePendingRewardInventory(connection, characterObjectId,
                                account, changed, before, stored, owned, failure);
                    }
                    throw failure;
                } finally {
                    lock.execute("UNLOCK TABLES");
                }
            }
        }
    }

    @Override public RepositoryCastleTreasuryMutation persistCastleTreasuryTransfer(
            long ownerCharacterObjectId, RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds)
            throws SQLException {
        if (!repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleTreasuryMutation.unavailable();
        }
        if (ownerCharacterObjectId <= 0 || ownerCharacterObjectId > 0xFFFF_FFFFL
                || expected == null || replacement == null
                || !expected.authoritative() || !replacement.authoritative()
                || expected.castleId() != replacement.castleId()
                || expected.taxRate() != replacement.taxRate()
                || expected.warTimeMillis() != replacement.warTimeMillis()
                || expected.securityEnabled() != replacement.securityEnabled()
                || !expected.mercenaries().equals(replacement.mercenaries())
                || expected.snapshotVersion() != replacement.snapshotVersion()
                || beforeInventory == null || afterInventory == null) {
            throw new IllegalArgumentException("invalid SQL castle treasury transfer");
        }
        long beforeAdena = beforeInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        long afterAdena = afterInventory.catalogCounts()
                .getOrDefault(InventoryCatalog.ADENA, 0);
        long inventoryDebit;
        long treasuryCredit;
        try {
            inventoryDebit = Math.subtractExact(beforeAdena, afterAdena);
            treasuryCredit = Math.subtractExact(
                    replacement.publicMoney(), expected.publicMoney());
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("castle treasury delta overflow", overflow);
        }
        if (inventoryDebit == 0 || inventoryDebit != treasuryCredit) {
            throw new IllegalArgumentException(
                    "castle treasury and character Adena deltas differ");
        }
        Map<Long, Long> sourceIds = localToSourceObjectIds == null
                ? Map.of() : Map.copyOf(localToSourceObjectIds);
        Map<Long, RuntimeInventory.Item> before = positionedInventoryItems(
                beforeInventory, sourceIds);
        Map<Long, RuntimeInventory.Item> after = positionedInventoryItems(
                afterInventory, sourceIds);
        Set<Long> changed = changedInventoryObjectIds(before, after);
        if (changed.isEmpty() || changed.stream().anyMatch(sqlObjectId -> {
            RuntimeInventory.Item old = before.get(sqlObjectId);
            RuntimeInventory.Item next = after.get(sqlObjectId);
            return (old != null
                    && old.definition().catalogId() != InventoryCatalog.ADENA)
                    || (next != null
                    && next.definition().catalogId() != InventoryCatalog.ADENA);
        })) {
            throw new IllegalArgumentException(
                    "castle treasury transfer may change only Adena objects");
        }
        if (readOnly) {
            return RepositoryCastleTreasuryMutation.readOnly(
                    castleState(expected.castleId()));
        }

        try (Connection connection = connection(accountUrl, accountLogin, accountPassword)) {
            for (long sqlObjectId : changed) {
                RuntimeInventory.Item next = after.get(sqlObjectId);
                if (next != null && !resourceItemTemplateExists(
                        connection, Math.toIntExact(next.definition().catalogId()))) {
                    return new RepositoryCastleTreasuryMutation(
                            RepositoryCastleTreasuryStatus.UNSUPPORTED_ITEM, expected);
                }
            }
            try (Statement lock = connection.createStatement()) {
                lock.execute("LOCK TABLES characters READ, clan_data READ, "
                        + "lito_clan_metadata READ, character_items WRITE, "
                        + "character_items_time WRITE, character_pets READ, "
                        + "lito_item_origin WRITE, "
                        + "lito_warehouse_item_origin WRITE, character_warehouse READ, "
                        + "clan_warehouse READ, character_elf_warehouse READ, "
                        + "`server_城堡` WRITE, lito_castle_metadata WRITE");
                RepositoryCastleSnapshot current = RepositoryCastleSnapshot.unavailable();
                Map<Long, SqlStoredItem> stored = new LinkedHashMap<>();
                Map<Long, Boolean> owned = new LinkedHashMap<>();
                boolean inventoryChanged = false;
                boolean castleChanged = false;
                try {
                    current = loadCastleSnapshot(connection, expected.castleId());
                    if (!current.authoritative()) {
                        return new RepositoryCastleTreasuryMutation(
                                RepositoryCastleTreasuryStatus.NOT_FOUND, current);
                    }
                    RepositoryClanSnapshot ownerClan = loadClanSnapshot(
                            connection, ownerCharacterObjectId);
                    if (!ownerClan.present()
                            || ownerClan.leaderObjectId() != ownerCharacterObjectId
                            || ownerClan.castleId() != current.castleId()) {
                        return new RepositoryCastleTreasuryMutation(
                                RepositoryCastleTreasuryStatus.NOT_OWNER, current);
                    }
                    if (!current.equals(expected)) {
                        return new RepositoryCastleTreasuryMutation(
                                RepositoryCastleTreasuryStatus.CONFLICT, current);
                    }
                    for (long sqlObjectId : changed) {
                        RuntimeInventory.Item old = before.get(sqlObjectId);
                        SqlStoredItem row = old == null ? null : loadStoredItem(
                                connection, "", "", "", ownerCharacterObjectId,
                                sqlObjectId, false);
                        if (old != null && (row == null
                                || row.itemId() != Math.toIntExact(
                                        old.definition().catalogId())
                                || row.count() != old.quantity())) {
                            return new RepositoryCastleTreasuryMutation(
                                    RepositoryCastleTreasuryStatus.CONFLICT, current);
                        }
                        if (old == null && !sqlInventoryObjectIdAvailable(
                                connection, sqlObjectId)) {
                            return new RepositoryCastleTreasuryMutation(
                                    RepositoryCastleTreasuryStatus.CONFLICT, current);
                        }
                        stored.put(sqlObjectId, row);
                        owned.put(sqlObjectId,
                                old != null && litoItemOriginExists(connection, sqlObjectId));
                    }
                    for (long sqlObjectId : changed) {
                        RuntimeInventory.Item old = before.get(sqlObjectId);
                        RuntimeInventory.Item next = after.get(sqlObjectId);
                        SqlStoredItem metadata = stored.get(sqlObjectId);
                        boolean nextOwned = owned.get(sqlObjectId)
                                || next != null && (!next.sourceBound() || next.litoOwned());
                        deleteStoredItem(connection, "", "", "",
                                ownerCharacterObjectId, sqlObjectId,
                                old == null ? null : false);
                        deleteItemOrigins(connection, sqlObjectId);
                        if (next != null) {
                            insertCharacterItem(connection, ownerCharacterObjectId,
                                    sqlObjectId, next, metadata);
                            if (nextOwned) upsertCharacterItemOrigin(connection,
                                    sqlObjectId, ownerCharacterObjectId, next);
                        }
                        inventoryChanged = true;
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE `server_城堡` SET public_money=? WHERE castle_id=?")) {
                        update.setLong(1, replacement.publicMoney());
                        update.setInt(2, current.castleId());
                        if (update.executeUpdate() != 1) {
                            throw new SQLException("castle treasury row changed during lock");
                        }
                        castleChanged = true;
                    }
                    writeCastleMetadata(connection, replacement,
                            Math.addExact(current.snapshotVersion(), 1));
                    return new RepositoryCastleTreasuryMutation(
                            RepositoryCastleTreasuryStatus.SUCCESS,
                            loadCastleSnapshot(connection, current.castleId()));
                } catch (ArithmeticException overflow) {
                    SQLException failure = new SQLException(
                            "castle treasury snapshot version overflow", overflow);
                    if (castleChanged) compensateCastleMutation(connection, current, failure);
                    if (inventoryChanged) compensateCharacterInventory(connection,
                            ownerCharacterObjectId, changed, before, stored, owned, failure);
                    throw failure;
                } catch (SQLException failure) {
                    if (castleChanged) compensateCastleMutation(connection, current, failure);
                    if (inventoryChanged) compensateCharacterInventory(connection,
                            ownerCharacterObjectId, changed, before, stored, owned, failure);
                    throw failure;
                } finally {
                    lock.execute("UNLOCK TABLES");
                }
            }
        }
    }

    private static Map<Long, RuntimeInventory.Item> positionedInventoryItems(
            RuntimeInventory inventory, Map<Long, Long> sourceIds) {
        Map<Long, RuntimeInventory.Item> result = new LinkedHashMap<>();
        for (RuntimeInventory.Item item : inventory.items()) {
            long sqlObjectId = sourceIds.getOrDefault(item.objectId(), item.objectId());
            if (sqlObjectId <= 0 || sqlObjectId > Integer.MAX_VALUE
                    || result.putIfAbsent(sqlObjectId, item) != null) {
                throw new IllegalArgumentException(
                        "duplicate/out-of-range SQL inventory object id: " + sqlObjectId);
            }
        }
        return Map.copyOf(result);
    }

    /** false=inventory, true=personal warehouse, null=invalid delta. */
    private static Boolean validatePendingRewardDelta(
            RepositoryPendingReward reward,
            Map<Long, PositionedWarehouseItem> before,
            Map<Long, PositionedWarehouseItem> after,
            Set<Long> changed) {
        if (changed.isEmpty()) return null;
        Boolean destination = null;
        long added = 0;
        for (long objectId : changed) {
            PositionedWarehouseItem old = before.get(objectId);
            PositionedWarehouseItem next = after.get(objectId);
            if (next == null || old != null && old.warehouse() != next.warehouse()) return null;
            if (destination == null) destination = next.warehouse();
            else if (destination.booleanValue() != next.warehouse()) return null;
            RuntimeInventory.Item nextItem = next.item();
            if (nextItem.definition().catalogId() != reward.itemId()
                    || nextItem.enchantLevel() != reward.enchant()
                    || nextItem.equipped() || nextItem.durability() != 0
                    || nextItem.tournamentReward() || nextItem.sealed()) return null;
            int oldCount = 0;
            if (old != null) {
                RuntimeInventory.Item oldItem = old.item();
                if (oldItem.definition().catalogId() != reward.itemId()
                        || oldItem.enchantLevel() != reward.enchant()
                        || oldItem.equipped() != nextItem.equipped()
                        || oldItem.durability() != nextItem.durability()
                        || oldItem.identified() != nextItem.identified()
                        || oldItem.blessing() != nextItem.blessing()
                        || oldItem.sourceBound() != nextItem.sourceBound()
                        || oldItem.tournamentReward() != nextItem.tournamentReward()
                        || oldItem.chargeCount() != nextItem.chargeCount()
                        || oldItem.expiresAtEpochMillis() != nextItem.expiresAtEpochMillis()
                        || oldItem.sealed() != nextItem.sealed()
                        || oldItem.litoOwned() != nextItem.litoOwned()
                        || !oldItem.definition().equals(nextItem.definition())) return null;
                oldCount = oldItem.quantity();
            } else if (nextItem.sourceBound() || nextItem.litoOwned()) {
                return null;
            }
            int delta = nextItem.quantity() - oldCount;
            if (delta <= 0) return null;
            try {
                added = Math.addExact(added, delta);
            } catch (ArithmeticException overflow) {
                return null;
            }
        }
        return added == reward.count() ? destination : null;
    }

    private static boolean characterIdentityMatches(
            Connection connection, long objectId, String account, String characterName)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT account_name,char_name FROM characters WHERE objid=?")) {
            query.setLong(1, objectId);
            try (ResultSet row = query.executeQuery()) {
                return row.next() && account.equalsIgnoreCase(row.getString("account_name"))
                        && characterName.equals(row.getString("char_name")) && !row.next();
            }
        }
    }

    private static RepositoryPendingReward loadPendingRewardLocked(
            Connection connection, long sequence,
            InventoryCatalog.Definition definition) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT `玩家帳號`,`發送物品編號`,`發送物品數量`,`強化值` "
                        + "FROM `server_紀錄_獎勵` WHERE `流水號`=? "
                        + "AND `領取狀態`=0")) {
            query.setLong(1, sequence);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                if (row.getInt("發送物品編號") != definition.catalogId()) return null;
                RepositoryPendingReward reward = new RepositoryPendingReward(
                        sequence, row.getString("玩家帳號"), definition,
                        row.getInt("發送物品數量"), row.getInt("強化值"));
                if (row.next()) throw new SQLException(
                        "duplicate pending reward sequence: " + sequence);
                return reward;
            }
        }
    }

    private static boolean storedItemStateMatches(
            Connection connection, String warehouseTable, String ownerColumn,
            String owner, long characterObjectId, long objectId,
            PositionedWarehouseItem expected) throws SQLException {
        boolean warehouse = expected.warehouse();
        String sql = warehouse
                ? "SELECT item_id,count,is_equipped,is_sealed,enchantlvl,is_id,"
                    + "durability,charge_count,bless FROM " + warehouseTable
                    + " WHERE id=? AND " + ownerColumn + "=?"
                : "SELECT item_id,count,is_equipped,is_sealed,enchantlvl,is_id,"
                    + "durability,charge_count,bless FROM character_items "
                    + "WHERE id=? AND char_id=?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, objectId);
            if (warehouse) query.setString(2, owner);
            else query.setLong(2, characterObjectId);
            try (ResultSet row = query.executeQuery()) {
                RuntimeInventory.Item item = expected.item();
                return row.next()
                        && row.getInt("item_id") == item.definition().catalogId()
                        && row.getLong("count") == item.quantity()
                        && row.getBoolean("is_equipped") == item.equipped()
                        && row.getBoolean("is_sealed") == item.sealed()
                        && row.getInt("enchantlvl") == item.enchantLevel()
                        && row.getBoolean("is_id") == item.identified()
                        && row.getInt("durability") == item.durability()
                        && row.getInt("charge_count") == item.chargeCount()
                        && row.getInt("bless") == item.blessing()
                        && !row.next();
            }
        }
    }

    private static void compensatePendingRewardInventory(
            Connection connection, long characterObjectId, String account,
            Set<Long> changed, Map<Long, PositionedWarehouseItem> before,
            Map<Long, SqlStoredItem> stored, Map<Long, Boolean> owned,
            SQLException primary) {
        for (long objectId : changed) {
            try {
                deleteStoredItem(connection, "character_warehouse", "account_name",
                        account, characterObjectId, objectId, null);
                try (PreparedStatement deleteInventory = connection.prepareStatement(
                        "DELETE FROM character_items WHERE id=? AND char_id=?")) {
                    deleteInventory.setLong(1, objectId);
                    deleteInventory.setLong(2, characterObjectId);
                    deleteInventory.executeUpdate();
                }
                try (PreparedStatement deleteWarehouse = connection.prepareStatement(
                        "DELETE FROM character_warehouse WHERE id=? AND account_name=?")) {
                    deleteWarehouse.setLong(1, objectId);
                    deleteWarehouse.setString(2, account);
                    deleteWarehouse.executeUpdate();
                }
                deleteItemOrigins(connection, objectId);
                PositionedWarehouseItem old = before.get(objectId);
                if (old == null) continue;
                if (old.warehouse()) {
                    insertWarehouseItem(connection, "character_warehouse", "account_name",
                            account, objectId, old.item(), stored.get(objectId));
                    if (owned.getOrDefault(objectId, false)) {
                        upsertWarehouseItemOrigin(connection, objectId,
                                GameStateStore.WarehouseScope.PERSONAL,
                                account, old.item());
                    }
                } else {
                    insertCharacterItem(connection, characterObjectId, objectId,
                            old.item(), stored.get(objectId));
                    if (owned.getOrDefault(objectId, false)) {
                        upsertCharacterItemOrigin(connection, objectId,
                                characterObjectId, old.item());
                    }
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
    }

    private static Set<Long> changedInventoryObjectIds(
            Map<Long, RuntimeInventory.Item> before,
            Map<Long, RuntimeInventory.Item> after) {
        Set<Long> changed = new LinkedHashSet<>();
        changed.addAll(before.keySet());
        changed.addAll(after.keySet());
        changed.removeIf(id -> java.util.Objects.equals(before.get(id), after.get(id)));
        return Set.copyOf(changed);
    }

    private static boolean sqlInventoryObjectIdAvailable(
            Connection connection, long objectId) throws SQLException {
        for (String table : List.of("character_items", "character_warehouse",
                "clan_warehouse", "character_elf_warehouse")) {
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT 1 FROM " + table + " WHERE id=? LIMIT 1")) {
                query.setLong(1, objectId);
                try (ResultSet row = query.executeQuery()) {
                    if (row.next()) return false;
                }
            }
        }
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM character_pets WHERE objid=? OR item_obj_id=? LIMIT 1")) {
            query.setLong(1, objectId);
            query.setLong(2, objectId);
            try (ResultSet row = query.executeQuery()) {
                if (row.next()) return false;
            }
        }
        return true;
    }

    private static void writeCastleMetadata(
            Connection connection, RepositoryCastleSnapshot snapshot,
            long snapshotVersion) throws SQLException {
        GameStateStore.MercenaryRoster mercenaries = snapshot.mercenaries();
        try (PreparedStatement update = connection.prepareStatement(
                "INSERT INTO lito_castle_metadata "
                        + "(castle_id,security_enabled,mercenary_0,mercenary_1,"
                        + "mercenary_2,mercenary_3,snapshot_version) "
                        + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                        + "security_enabled=VALUES(security_enabled),"
                        + "mercenary_0=VALUES(mercenary_0),"
                        + "mercenary_1=VALUES(mercenary_1),"
                        + "mercenary_2=VALUES(mercenary_2),"
                        + "mercenary_3=VALUES(mercenary_3),"
                        + "snapshot_version=VALUES(snapshot_version)")) {
            update.setInt(1, snapshot.castleId());
            update.setBoolean(2, snapshot.securityEnabled());
            update.setInt(3, mercenaries.type0());
            update.setInt(4, mercenaries.type1());
            update.setInt(5, mercenaries.type2());
            update.setInt(6, mercenaries.type3());
            update.setLong(7, snapshotVersion);
            update.executeUpdate();
        }
    }

    private static void compensateCharacterInventory(
            Connection connection, long characterObjectId, Set<Long> changed,
            Map<Long, RuntimeInventory.Item> before,
            Map<Long, SqlStoredItem> stored, Map<Long, Boolean> owned,
            SQLException primary) {
        for (long sqlObjectId : changed) {
            try {
                deleteStoredItem(connection, "", "", "", characterObjectId,
                        sqlObjectId, false);
                deleteItemOrigins(connection, sqlObjectId);
                RuntimeInventory.Item old = before.get(sqlObjectId);
                if (old != null) {
                    insertCharacterItem(connection, characterObjectId, sqlObjectId,
                            old, stored.get(sqlObjectId));
                    if (owned.getOrDefault(sqlObjectId, false)) {
                        upsertCharacterItemOrigin(connection, sqlObjectId,
                                characterObjectId, old);
                    }
                }
            } catch (SQLException compensation) {
                primary.addSuppressed(compensation);
            }
        }
    }

    private static RepositoryCastleSnapshot loadCastleSnapshot(
            Connection connection, int castleId) throws SQLException {
        String sql = "SELECT `server_城堡`.castle_id,`server_城堡`.tax_rate,"
                + "`server_城堡`.public_money,"
                + "COALESCE(UNIX_TIMESTAMP(`server_城堡`.war_time)*1000,0) "
                + "AS war_time_millis,"
                + "COALESCE(lito_castle_metadata.security_enabled,0) AS security_enabled,"
                + "COALESCE(lito_castle_metadata.mercenary_0,0) AS mercenary_0,"
                + "COALESCE(lito_castle_metadata.mercenary_1,0) AS mercenary_1,"
                + "COALESCE(lito_castle_metadata.mercenary_2,0) AS mercenary_2,"
                + "COALESCE(lito_castle_metadata.mercenary_3,0) AS mercenary_3,"
                + "COALESCE(lito_castle_metadata.snapshot_version,0) AS snapshot_version "
                + "FROM `server_城堡` LEFT JOIN lito_castle_metadata "
                + "ON lito_castle_metadata.castle_id=`server_城堡`.castle_id "
                + "WHERE `server_城堡`.castle_id=? LIMIT 1";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, castleId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return RepositoryCastleSnapshot.unavailable();
                int nativeTax = row.getInt("tax_rate");
                int tax = nativeTax >= 10 && nativeTax <= 50 ? nativeTax : 10;
                try {
                    return new RepositoryCastleSnapshot(true, row.getInt("castle_id"),
                            tax, Math.max(0, row.getLong("public_money")),
                            Math.max(0, row.getLong("war_time_millis")),
                            row.getInt("security_enabled") != 0,
                            new GameStateStore.MercenaryRoster(
                                    row.getInt("mercenary_0"),
                                    row.getInt("mercenary_1"),
                                    row.getInt("mercenary_2"),
                                    row.getInt("mercenary_3")),
                            Math.max(0, row.getLong("snapshot_version")));
                } catch (IllegalArgumentException invalid) {
                    throw new SQLException("invalid selected-source castle snapshot: "
                            + castleId, invalid);
                }
            }
        }
    }

    private record SqlCastleSecuritySettlement(
            long treasuryBefore, long chargeAdena, long chargedAdena,
            boolean prepared, boolean completed) {}

    private static SqlCastleSecuritySettlement loadCastleSecuritySettlement(
            Connection connection, int castleId, long warStartMillis)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT treasury_before,charge_adena,charged_adena,prepared,completed "
                        + "FROM lito_castle_security_settlement "
                        + "WHERE castle_id=? AND war_start_millis=? LIMIT 1")) {
            query.setInt(1, castleId);
            query.setLong(2, warStartMillis);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                return new SqlCastleSecuritySettlement(
                        row.getLong("treasury_before"),
                        row.getLong("charge_adena"),
                        row.getLong("charged_adena"),
                        row.getInt("prepared") != 0,
                        row.getInt("completed") != 0);
            }
        }
    }

    private static RepositoryCastleSecuritySettlement castleSecuritySettlement(
            RepositoryCastleSecuritySettlementStatus status,
            RepositoryCastleSnapshot snapshot, long warStartMillis,
            long chargedAdena) {
        return new RepositoryCastleSecuritySettlement(
                status, snapshot == null
                        ? RepositoryCastleSnapshot.unavailable() : snapshot,
                warStartMillis, chargedAdena);
    }

    private record CastleWarRewardTarget(
            String account, int itemId, int count, String eventKey) {}

    private static CastleWarRewardTarget loadCastleWarRewardTarget(
            Connection connection, int castleId, long warTimeMillis)
            throws SQLException {
        int itemId;
        int count;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT item_id,count FROM `server_城堡` WHERE castle_id=?")) {
            query.setInt(1, castleId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                itemId = row.getInt("item_id");
                count = row.getInt("count");
            }
        }
        if (itemId <= 0 || count <= 0) return null;

        long leaderId;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT clan_id,leader_id FROM clan_data WHERE hascastle=? "
                        + "ORDER BY clan_id")) {
            query.setInt(1, castleId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                leaderId = row.getLong("leader_id");
                if (row.next()) {
                    throw new SQLException("multiple clans own castle " + castleId);
                }
            }
        }
        if (leaderId <= 0 || leaderId > 0xFFFF_FFFFL) return null;

        String account;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT account_name FROM characters WHERE objid=?")) {
            query.setLong(1, leaderId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("castle owner leader is missing: " + leaderId);
                }
                account = row.getString("account_name");
                if (row.next()) {
                    throw new SQLException("duplicate castle leader object id: " + leaderId);
                }
            }
        }
        if (account == null || account.isBlank()) {
            throw new SQLException("castle owner leader account is missing: " + leaderId);
        }
        String eventKey = "lito:castle-war:" + castleId + ":"
                + Math.max(0, warTimeMillis / 1_000L);
        return new CastleWarRewardTarget(account.trim(), itemId, count, eventKey);
    }

    /** Returns true only when this attempt inserted the pending reward row. */
    private static boolean ensureCastleWarReward(
            Connection connection, CastleWarRewardTarget expected) throws SQLException {
        CastleWarRewardTarget existing = loadCastleWarReward(
                connection, expected.eventKey());
        boolean inserted = false;
        if (existing == null) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO `server_紀錄_獎勵` "
                            + "(`玩家帳號`,`發送物品編號`,`發送物品數量`,`強化值`,"
                            + "`領取狀態`,lito_event_key) VALUES (?,?,?,0,0,?)")) {
                insert.setString(1, expected.account());
                insert.setInt(2, expected.itemId());
                insert.setInt(3, expected.count());
                insert.setString(4, expected.eventKey());
                if (insert.executeUpdate() != 1) {
                    throw new SQLException("castle reward insert affected no row");
                }
                inserted = true;
            }
            existing = loadCastleWarReward(connection, expected.eventKey());
        }
        if (!expected.equals(existing)) {
            throw new SQLException("castle reward event key conflicts: "
                    + expected.eventKey());
        }
        return inserted;
    }

    private static CastleWarRewardTarget loadCastleWarReward(
            Connection connection, String eventKey) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT `玩家帳號`,`發送物品編號`,`發送物品數量`,`強化值`,"
                        + "`領取狀態`,lito_event_key FROM `server_紀錄_獎勵` "
                        + "WHERE lito_event_key=?")) {
            query.setString(1, eventKey);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                int enchant = row.getInt("強化值");
                int claimed = row.getInt("領取狀態");
                CastleWarRewardTarget result = new CastleWarRewardTarget(
                        row.getString("玩家帳號"), row.getInt("發送物品編號"),
                        row.getInt("發送物品數量"), row.getString("lito_event_key"));
                if (enchant != 0 || claimed < 0 || claimed > 1 || row.next()) {
                    throw new SQLException("invalid castle reward row: " + eventKey);
                }
                return result;
            }
        }
    }

    private static void compensateCastleWarReward(
            Connection connection, String eventKey, SQLException primary) {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM `server_紀錄_獎勵` WHERE lito_event_key=?")) {
            delete.setString(1, eventKey);
            delete.executeUpdate();
        } catch (SQLException compensation) {
            primary.addSuppressed(compensation);
        }
    }

    private static void compensateCastleMutation(
            Connection connection, RepositoryCastleSnapshot expected,
            SQLException primary) {
        try (PreparedStatement restore = connection.prepareStatement(
                "UPDATE `server_城堡` SET tax_rate=?,public_money=?,"
                        + "war_time=FROM_UNIXTIME(?) WHERE castle_id=?")) {
            restore.setInt(1, expected.taxRate());
            restore.setLong(2, expected.publicMoney());
            restore.setLong(3, expected.warTimeMillis() / 1_000L);
            restore.setInt(4, expected.castleId());
            restore.executeUpdate();
        } catch (SQLException compensation) {
            primary.addSuppressed(compensation);
        }
        try {
            if (expected.snapshotVersion() == 0) {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_castle_metadata WHERE castle_id=?")) {
                    delete.setInt(1, expected.castleId());
                    delete.executeUpdate();
                }
            } else {
                GameStateStore.MercenaryRoster mercenaries = expected.mercenaries();
                try (PreparedStatement restore = connection.prepareStatement(
                        "INSERT INTO lito_castle_metadata "
                                + "(castle_id,security_enabled,mercenary_0,mercenary_1,"
                                + "mercenary_2,mercenary_3,snapshot_version) "
                                + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                                + "security_enabled=VALUES(security_enabled),"
                                + "mercenary_0=VALUES(mercenary_0),"
                                + "mercenary_1=VALUES(mercenary_1),"
                                + "mercenary_2=VALUES(mercenary_2),"
                                + "mercenary_3=VALUES(mercenary_3),"
                                + "snapshot_version=VALUES(snapshot_version)")) {
                    restore.setInt(1, expected.castleId());
                    restore.setBoolean(2, expected.securityEnabled());
                    restore.setInt(3, mercenaries.type0());
                    restore.setInt(4, mercenaries.type1());
                    restore.setInt(5, mercenaries.type2());
                    restore.setInt(6, mercenaries.type3());
                    restore.setLong(7, expected.snapshotVersion());
                    restore.executeUpdate();
                }
            }
        } catch (SQLException compensation) {
            primary.addSuppressed(compensation);
        }
    }

    @Override public boolean characterExists(String characterName) throws SQLException {
        String sql = "SELECT 1 FROM characters WHERE char_name = ? LIMIT 1";
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, characterName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override public String description() {
        return repositoryMode + "(dynamic-database=" + accountDatabase
                + ", resource-database=" + resourceDatabase
                + ", read-only=" + readOnly + ")";
    }

    private String resourceTable(String table) {
        if (table == null || !table.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("unsupported resource table identifier");
        }
        return resourceDatabase + "." + table;
    }

    /** Resolves an arbitrary native reward item from the selected static DB. */
    private InventoryCatalog.Definition rewardItemDefinition(
            Connection connection, int itemId) throws SQLException {
        if (itemId <= 0) return null;
        InventoryCatalog.Definition cached = rewardItemCache.get(itemId);
        if (cached != null) return cached;
        List<InventoryCatalog.Definition> matches = new ArrayList<>(3);
        InventoryCatalog.Definition weapon = rewardWeaponDefinition(connection, itemId);
        InventoryCatalog.Definition armor = rewardArmorDefinition(connection, itemId);
        InventoryCatalog.Definition etc = rewardEtcDefinition(connection, itemId);
        if (weapon != null) matches.add(weapon);
        if (armor != null) matches.add(armor);
        if (etc != null) matches.add(etc);
        if (matches.size() != 1) return null;
        InventoryCatalog.Definition selected = matches.get(0);
        InventoryCatalog.Definition existing = rewardItemCache.putIfAbsent(itemId, selected);
        return existing == null ? selected : existing;
    }

    private InventoryCatalog.Definition rewardWeaponDefinition(
            Connection connection, int itemId) throws SQLException {
        String sql = "SELECT item_id,COALESCE(NULLIF(name,''),name_id) AS display_name,"
                + "invgfx,grdgfx,bless,weight,itemdesc_id,type,dmg_small,dmg_large,"
                + "material,safenchant,cant_delete,trade,"
                + "(IF(use_royal<>0,1,0)|IF(use_knight<>0,2,0)|"
                + "IF(use_elf<>0,4,0)|IF(use_mage<>0,8,0)|"
                + "IF(use_darkelf<>0,16,0)|IF(use_dragonknight<>0,32,0)|"
                + "IF(use_illusionist<>0,64,0)) AS class_mask "
                + "FROM " + resourceTable("weapon") + " WHERE item_id=?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, itemId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                ClassicWorldDropCatalog.ItemWire wire = new ClassicWorldDropCatalog.ItemWire(
                        itemId, "weapon", row.getString("display_name"), "weapon",
                        row.getInt("invgfx"), row.getInt("grdgfx"), row.getInt("bless"),
                        false, row.getInt("trade") == 0, row.getInt("weight"), row.getInt("itemdesc_id"),
                        row.getString("type"), row.getInt("dmg_small"),
                        row.getInt("dmg_large"), row.getString("material"),
                        row.getInt("safenchant"), row.getInt("class_mask"),
                        row.getBoolean("cant_delete"));
                if (row.next()) throw new SQLException("duplicate weapon item id: " + itemId);
                return wire.inventoryDefinition();
            }
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid reward weapon template: " + itemId, invalid);
        }
    }

    private InventoryCatalog.Definition rewardArmorDefinition(
            Connection connection, int itemId) throws SQLException {
        String sql = "SELECT item_id,COALESCE(NULLIF(name,''),name_id) AS display_name,"
                + "invgfx,grdgfx,bless,weight,itemdesc_id,type,material,safenchant,"
                + "cant_delete,trade,ABS(ac) AS armor_ac,add_str,add_dex,add_con,add_wis,"
                + "add_int,add_cha,add_hp,add_mp,m_def,add_sp,haste_item,"
                + "defense_fire,defense_water,defense_wind,defense_earth,"
                + "regist_blind,regist_sleep,regist_freeze,regist_stone,"
                + "regist_sustain,regist_stun,"
                + "(IF(use_royal<>0,1,0)|IF(use_knight<>0,2,0)|"
                + "IF(use_elf<>0,4,0)|IF(use_mage<>0,8,0)|"
                + "IF(use_darkelf<>0,16,0)|IF(use_dragonknight<>0,32,0)|"
                + "IF(use_illusionist<>0,64,0)) AS class_mask "
                + "FROM " + resourceTable("armor") + " WHERE item_id=?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, itemId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                InventoryCatalog.NativeArmorStatus armor =
                        new InventoryCatalog.NativeArmorStatus(
                                row.getInt("armor_ac"),
                                ClassicWorldDropCatalog.materialCode(row.getString("material")),
                                row.getInt("weight"), row.getInt("class_mask"),
                                row.getInt("safenchant"), row.getInt("add_str"),
                                row.getInt("add_dex"), row.getInt("add_con"),
                                row.getInt("add_wis"), row.getInt("add_int"),
                                row.getInt("add_cha"), row.getInt("add_hp"),
                                row.getInt("add_mp"), row.getInt("m_def"),
                                row.getInt("add_sp"), row.getBoolean("haste_item"),
                                row.getInt("defense_fire"), row.getInt("defense_water"),
                                row.getInt("defense_wind"), row.getInt("defense_earth"),
                                row.getInt("regist_blind"), row.getInt("regist_sleep"),
                                row.getInt("regist_freeze"), row.getInt("regist_stone"),
                                row.getInt("regist_sustain"), row.getInt("regist_stun"),
                                ClassicWorldDropCatalog.armorTypeCode(row.getString("type")));
                ClassicWorldDropCatalog.ItemWire wire = new ClassicWorldDropCatalog.ItemWire(
                        itemId, "armor", row.getString("display_name"), row.getString("type"),
                        row.getInt("invgfx"), row.getInt("grdgfx"), row.getInt("bless"),
                        false, row.getInt("trade") == 0, row.getInt("weight"), row.getInt("itemdesc_id"),
                        "", 0, 0, row.getString("material"), row.getInt("safenchant"),
                        row.getInt("class_mask"), row.getBoolean("cant_delete"));
                if (row.next()) throw new SQLException("duplicate armor item id: " + itemId);
                return wire.inventoryDefinition(armor);
            }
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid reward armor template: " + itemId, invalid);
        }
    }

    private InventoryCatalog.Definition rewardEtcDefinition(
            Connection connection, int itemId) throws SQLException {
        String sql = "SELECT item_id,COALESCE(NULLIF(name,''),name_id) AS display_name,"
                + "use_type,invgfx,grdgfx,bless,stackable,weight,itemdesc_id,"
                + "material,cant_delete,trade FROM " + resourceTable("etcitem")
                + " WHERE item_id=?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, itemId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                ClassicWorldDropCatalog.ItemWire wire = new ClassicWorldDropCatalog.ItemWire(
                        itemId, "etc", row.getString("display_name"),
                        row.getString("use_type"), row.getInt("invgfx"),
                        row.getInt("grdgfx"), row.getInt("bless"),
                        row.getBoolean("stackable"), row.getInt("trade") == 0,
                        row.getInt("weight"), row.getInt("itemdesc_id"), "", 0, 0,
                        row.getString("material"), -1, 0,
                        row.getBoolean("cant_delete"));
                if (row.next()) throw new SQLException("duplicate etcitem id: " + itemId);
                return wire.inventoryDefinition();
            }
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid reward etcitem template: " + itemId, invalid);
        }
    }

    private String sourceInventorySql() {
        String ownershipJoin = repositoryMode.equals("lito_mysql")
                ? "LEFT JOIN lito_item_origin o ON o.item_obj_id=i.id " : "";
        String ownershipProjection = repositoryMode.equals("lito_mysql")
                ? "IF(o.item_obj_id IS NULL,0,1) AS lito_owned,"
                    + "IFNULL(o.tournament_reward,0) AS tournament_reward,"
                : "0 AS lito_owned,0 AS tournament_reward,";
        String stateJoins = " LEFT JOIN character_pets p ON p.item_obj_id=i.id "
                + "LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id "
                + ownershipJoin;
        String common = "i.id AS source_object_id,i.item_id,i.count AS item_count,"
                + "i.is_equipped,i.enchantlvl,i.durability,i.is_id AS is_identified,"
                + "i.bless AS instance_bless,i.is_sealed AS instance_sealed,"
                + "i.charge_count AS charge_count,"
                + "t.usertime AS expires_at,IF(p.item_obj_id IS NULL,0,1) "
                + "AS linked_pet_collar," + ownershipProjection;
        String weapon = "SELECT " + common
                + "'weapon' AS item_kind,w.name_id,'weapon' AS use_token,"
                + "w.invgfx,w.grdgfx,w.bless AS template_bless,0 AS stackable,"
                + "IF(w.trade=0,1,0) AS tradable,w.weight AS unit_weight,w.itemdesc_id,w.type "
                + "AS weapon_type,w.dmg_small,w.dmg_large,w.material,w.safenchant,"
                + "w.cant_delete AS cant_delete,"
                + "(IF(w.use_royal<>0,1,0)|IF(w.use_knight<>0,2,0)|"
                + "IF(w.use_elf<>0,4,0)|IF(w.use_mage<>0,8,0)|"
                + "IF(w.use_darkelf<>0,16,0)|IF(w.use_dragonknight<>0,32,0)|"
                + "IF(w.use_illusionist<>0,64,0)) AS class_mask,"
                + "0 AS armor_ac,0 AS armor_str,0 AS armor_dex,0 AS armor_con,"
                + "0 AS armor_wis,0 AS armor_int,0 AS armor_cha,0 AS armor_hp,"
                + "0 AS armor_mp,0 AS armor_mr,0 AS armor_sp,0 AS armor_haste,"
                + "0 AS armor_fire,0 AS armor_water,0 AS armor_wind,0 AS armor_earth,"
                + "0 AS armor_blind,0 AS armor_sleep,0 AS armor_freeze,0 AS armor_stone,"
                + "0 AS armor_sustain,0 AS armor_stun,"
                + "NULL AS doll_item_id,0 AS doll_type,0 AS doll_icon,'' AS doll_powers,"
                + "'' AS doll_material_ids,'' AS doll_material_counts,"
                + "0 AS doll_duration,0 AS doll_gfx,'' AS doll_name "
                + "FROM character_items i JOIN " + resourceTable("weapon")
                + " w ON w.item_id=i.item_id" + stateJoins + "WHERE i.char_id=?";
        String armor = "SELECT " + common
                + "'armor' AS item_kind,a.name_id,a.type AS use_token,"
                + "a.invgfx,a.grdgfx,a.bless AS template_bless,0 AS stackable,"
                + "IF(a.trade=0,1,0) AS tradable,a.weight AS unit_weight,a.itemdesc_id,'' "
                + "AS weapon_type,0 AS dmg_small,0 AS dmg_large,a.material,a.safenchant,"
                + "a.cant_delete AS cant_delete,"
                + "(IF(a.use_royal<>0,1,0)|IF(a.use_knight<>0,2,0)|"
                + "IF(a.use_elf<>0,4,0)|IF(a.use_mage<>0,8,0)|"
                + "IF(a.use_darkelf<>0,16,0)|IF(a.use_dragonknight<>0,32,0)|"
                + "IF(a.use_illusionist<>0,64,0)) AS class_mask,ABS(a.ac) AS armor_ac,"
                + "a.add_str AS armor_str,a.add_dex AS armor_dex,a.add_con AS armor_con,"
                + "a.add_wis AS armor_wis,a.add_int AS armor_int,a.add_cha AS armor_cha,"
                + "a.add_hp AS armor_hp,a.add_mp AS armor_mp,a.m_def AS armor_mr,"
                + "a.add_sp AS armor_sp,a.haste_item AS armor_haste,"
                + "a.defense_fire AS armor_fire,a.defense_water AS armor_water,"
                + "a.defense_wind AS armor_wind,a.defense_earth AS armor_earth,"
                + "a.regist_blind AS armor_blind,a.regist_sleep AS armor_sleep,"
                + "a.regist_freeze AS armor_freeze,a.regist_stone AS armor_stone,"
                + "a.regist_sustain AS armor_sustain,a.regist_stun AS armor_stun,"
                + "NULL AS doll_item_id,0 AS doll_type,0 AS doll_icon,'' AS doll_powers,"
                + "'' AS doll_material_ids,'' AS doll_material_counts,"
                + "0 AS doll_duration,0 AS doll_gfx,'' AS doll_name "
                + "FROM character_items i JOIN " + resourceTable("armor")
                + " a ON a.item_id=i.item_id" + stateJoins + "WHERE i.char_id=?";
        String etc = "SELECT " + common
                + "'etc' AS item_kind,e.name_id,e.use_type AS use_token,"
                + "e.invgfx,e.grdgfx,e.bless AS template_bless,e.stackable,"
                + "IF(e.trade=0,1,0) AS tradable,e.weight AS unit_weight,e.itemdesc_id,'' "
                + "AS weapon_type,0 AS dmg_small,0 AS dmg_large,e.material,-1 AS safenchant,"
                + "e.cant_delete AS cant_delete,"
                + "0 AS class_mask,0 AS armor_ac,0 AS armor_str,0 AS armor_dex,"
                + "0 AS armor_con,0 AS armor_wis,0 AS armor_int,0 AS armor_cha,"
                + "0 AS armor_hp,0 AS armor_mp,0 AS armor_mr,0 AS armor_sp,"
                + "0 AS armor_haste,0 AS armor_fire,0 AS armor_water,0 AS armor_wind,"
                + "0 AS armor_earth,0 AS armor_blind,0 AS armor_sleep,0 AS armor_freeze,"
                + "0 AS armor_stone,0 AS armor_sustain,0 AS armor_stun,"
                + "d.itemid AS doll_item_id,d.doll_type,d.icon AS doll_icon,"
                + "d.powers AS doll_powers,d.need AS doll_material_ids,"
                + "d.`count` AS doll_material_counts,d.time AS doll_duration,"
                + "d.gfxid AS doll_gfx,d.nameid AS doll_name "
                + "FROM character_items i JOIN " + resourceTable("etcitem")
                + " e ON e.item_id=i.item_id LEFT JOIN "
                + resourceTable("etcitem_doll_type")
                + " d ON d.itemid=i.item_id" + stateJoins + "WHERE i.char_id=?";
        return "SELECT * FROM (" + weapon + " UNION ALL " + armor + " UNION ALL "
                + etc + ") source_inventory ORDER BY source_object_id";
    }

    private String warehouseInventorySql(GameStateStore.WarehouseScope scope) {
        if (scope == null) throw new IllegalArgumentException("warehouse scope is required");
        String table = warehouseTable(scope);
        String ownerColumn = warehouseOwnerColumn(scope);
        String sql = sourceInventorySql();
        sql = sql.replace("FROM character_items i JOIN",
                "FROM " + table + " i JOIN");
        sql = sql.replace("LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id ", "");
        sql = sql.replace("LEFT JOIN lito_item_origin o ON o.item_obj_id=i.id ",
                "LEFT JOIN lito_warehouse_item_origin o ON o.item_obj_id=i.id "
                        + "AND o.scope='" + scope.name() + "' AND o.owner_key=i."
                        + ownerColumn + " ");
        sql = sql.replace("t.usertime AS expires_at",
                "CASE WHEN IFNULL(i.remaining_time,0)>0 THEN "
                        + "TIMESTAMPADD(SECOND,i.remaining_time,CURRENT_TIMESTAMP) "
                        + "ELSE NULL END AS expires_at");
        return sql.replace("WHERE i.char_id=?",
                "WHERE i." + ownerColumn + "=?");
    }

    private static String warehouseTable(GameStateStore.WarehouseScope scope) {
        return switch (scope) {
            case PERSONAL -> "character_warehouse";
            case CLAN -> "clan_warehouse";
            case ELF -> "character_elf_warehouse";
        };
    }

    private static String warehouseOwnerColumn(GameStateStore.WarehouseScope scope) {
        return scope == GameStateStore.WarehouseScope.CLAN ? "clan_name" : "account_name";
    }

    private Map<Integer, SourceMagicDollPower> sourceMagicDollPowers(
            Connection connection) throws SQLException {
        String sql = "SELECT id,note,classname,value_main,probability,interval_time,"
                + "passive_skill_id,mode,attr_type,gfx_id,gfx_target,value_sub,"
                + "trigger_skill_id,item_id,item_count,status_id FROM "
                + resourceTable("etcitem_doll_power_new") + " ORDER BY id";
        Map<Integer, SourceMagicDollPower> result = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            while (row.next()) {
                try {
                    SourceMagicDollPower power = new SourceMagicDollPower(
                            row.getInt("id"), row.getString("note"),
                            row.getString("classname"), row.getInt("value_main"),
                            row.getInt("probability"), row.getInt("interval_time"),
                            row.getInt("passive_skill_id"), row.getInt("mode"),
                            row.getInt("attr_type"), row.getInt("gfx_id"),
                            row.getInt("gfx_target"), row.getInt("value_sub"),
                            row.getInt("trigger_skill_id"), row.getInt("item_id"),
                            row.getInt("item_count"), row.getInt("status_id"));
                    if (result.putIfAbsent(power.id(), power) != null) {
                        throw new SQLException("duplicate etcitem_doll_power_new id: "
                                + power.id());
                    }
                } catch (IllegalArgumentException invalid) {
                    throw new SQLException("invalid etcitem_doll_power_new row id="
                            + row.getInt("id"), invalid);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static List<Integer> commaSeparatedPositiveInts(
            String source, String column) throws SQLException {
        if (source == null || source.isBlank()) return List.of();
        List<Integer> result = new ArrayList<>();
        for (String token : source.split(",", -1)) {
            String value = token.trim();
            if (value.isEmpty()) {
                throw new SQLException("empty magic-doll " + column + " token");
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0) throw new NumberFormatException("not positive");
                result.add(parsed);
            } catch (NumberFormatException invalid) {
                throw new SQLException("invalid magic-doll " + column
                        + " token: " + value, invalid);
            }
        }
        return List.copyOf(result);
    }

    private String exactResourceTable(String table) {
        if (!"角色_精神成長".equals(table) && !"角色_體質成長".equals(table)
                && !"角色_力量成長".equals(table) && !"角色_敏捷成長".equals(table)) {
            throw new IllegalArgumentException("unsupported exact resource table identifier");
        }
        return resourceDatabase + ".`" + table + "`";
    }

    private List<Integer> physicalGrowthColumn(
            Connection connection, String table, String column) throws SQLException {
        if (!"命中".equals(column) && !"傷害".equals(column)) {
            throw new IllegalArgumentException("unsupported physical growth column");
        }
        List<Integer> values = new ArrayList<>(SourcePhysicalGrowthStats.MAX_STAT);
        String sql = "SELECT id,`" + column + "` FROM " + exactResourceTable(table)
                + " ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            int expectedId = 1;
            while (row.next()) {
                if (row.getInt("id") != expectedId
                        || expectedId > SourcePhysicalGrowthStats.MAX_STAT) {
                    throw new SQLException("source physical growth ids are not contiguous 1..127");
                }
                values.add(row.getInt(column));
                expectedId++;
            }
        }
        if (values.size() != SourcePhysicalGrowthStats.MAX_STAT) {
            throw new SQLException("source physical growth table does not contain ids 1..127");
        }
        return List.copyOf(values);
    }

    private Connection connection(String url, String login, String password) throws SQLException {
        if (!accountUrl.equals(url) || !accountLogin.equals(login)
                || !accountPassword.equals(password)) {
            throw new SQLException("repository connection route mismatch");
        }
        return dataSource.getConnection();
    }

    @Override public void close() {
        dataSource.close();
    }

    private void checkTable(String url, String login, String password, String table) throws SQLException {
        try (Connection connection = connection(url, login, password);
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1")) {
            statement.executeQuery().close();
        }
    }

    private static String jdbcUrlWithDatabase(Properties p, String prefixKey, String database, String suffixKey) {
        String prefix = required(p, prefixKey, null);
        String suffix = p.getProperty(suffixKey, "").trim();
        return prefix + database + suffix;
    }

    // Resolve the generic property only when the preferred value is absent or blank.
    private static String requiredWithFallback(Properties p, String key,
            String fallbackKey) {
        String value = p.getProperty(key);
        return value == null || value.isBlank()
                ? required(p, fallbackKey, null) : value.trim();
    }

    private static String required(Properties p, String key, String fallback) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            if (fallback != null) return fallback;
            throw new IllegalArgumentException("Missing legacy SQL property: " + key);
        }
        return value.trim();
    }
}
