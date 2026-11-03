// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.AccountMutationResult;
import local.lito381.server.AccountRepository;
import local.lito381.server.AuthResult;
import local.lito381.server.BattleControlCatalog;
import local.lito381.server.CastleSummary;
import local.lito381.server.CastleWarSchedule;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClanEmblemData;
import local.lito381.server.ClassicWorldDropCatalog;
import local.lito381.server.GameStateStore;
import local.lito381.server.GmCommandDefinition;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.LegacyEquipmentMagicLoader;
import local.lito381.server.LegacyInventoryWeightLoader;
import local.lito381.server.LitoCharacterPersistence;
import local.lito381.server.LitoOwnedInventoryItem;
import local.lito381.server.LitoPetPersistence;
import local.lito381.server.LitoSourceInventoryMutation;
import local.lito381.server.NativeNpcPhysicalSource;
import local.lito381.server.NativeSkillMastery;
import local.lito381.server.Protocol;
import local.lito381.server.RepositoryBattleConfiguration;
import local.lito381.server.RepositoryBattleObject;
import local.lito381.server.RepositoryBattlePlayerSpawn;
import local.lito381.server.RepositoryBattleRule;
import local.lito381.server.RepositoryBattleSpawn;
import local.lito381.server.RepositoryBoardLookup;
import local.lito381.server.RepositoryBoardMutation;
import local.lito381.server.RepositoryBoardSnapshot;
import local.lito381.server.RepositoryBookmark;
import local.lito381.server.RepositoryBookmarkPersistenceResult;
import local.lito381.server.RepositoryBookmarkSnapshot;
import local.lito381.server.RepositoryBuddy;
import local.lito381.server.RepositoryBuddyGroup;
import local.lito381.server.RepositoryBuddyPersistence;
import local.lito381.server.RepositoryBuddySnapshot;
import local.lito381.server.RepositoryBuff;
import local.lito381.server.RepositoryBuffPersistence;
import local.lito381.server.RepositoryBuffSnapshot;
import local.lito381.server.RepositoryCastleMutation;
import local.lito381.server.RepositoryCastleOwnershipStatus;
import local.lito381.server.RepositoryCastleSecuritySettlement;
import local.lito381.server.RepositoryCastleSecuritySettlementStatus;
import local.lito381.server.RepositoryCastleSnapshot;
import local.lito381.server.RepositoryCastleTreasuryMutation;
import local.lito381.server.RepositoryCharacterMutation;
import local.lito381.server.RepositoryCharacterTitle;
import local.lito381.server.RepositoryClanMember;
import local.lito381.server.RepositoryClanMutation;
import local.lito381.server.RepositoryClanMutationStatus;
import local.lito381.server.RepositoryClanSnapshot;
import local.lito381.server.RepositoryHotelChange;
import local.lito381.server.RepositoryHotelChangeKind;
import local.lito381.server.RepositoryHotelSnapshot;
import local.lito381.server.RepositoryHouseAuctionSnapshot;
import local.lito381.server.RepositoryHouseBidMutation;
import local.lito381.server.RepositoryHouseBidStatus;
import local.lito381.server.RepositoryHouseSettlement;
import local.lito381.server.RepositoryHouseSettlementStatus;
import local.lito381.server.RepositoryHouseTaxMutation;
import local.lito381.server.RepositoryHouseTaxPayment;
import local.lito381.server.RepositoryHouseTaxQuote;
import local.lito381.server.RepositoryHouseTaxQuoteStatus;
import local.lito381.server.RepositoryHouseTaxSnapshot;
import local.lito381.server.RepositoryHouseTaxState;
import local.lito381.server.RepositoryLetter;
import local.lito381.server.RepositoryLetterDelivery;
import local.lito381.server.RepositoryLetterMutation;
import local.lito381.server.RepositoryLetterSnapshot;
import local.lito381.server.RepositoryMarriageMutation;
import local.lito381.server.RepositoryMarriageMutationStatus;
import local.lito381.server.RepositoryMarriageState;
import local.lito381.server.RepositoryPendingReward;
import local.lito381.server.RepositoryPendingRewardClaim;
import local.lito381.server.RepositoryPendingRewardSnapshot;
import local.lito381.server.RepositoryPersistenceResult;
import local.lito381.server.RepositoryPrivateInventorySnapshot;
import local.lito381.server.RepositoryTitleMutation;
import local.lito381.server.RepositoryTournamentMutation;
import local.lito381.server.RepositoryTournamentMutationStatus;
import local.lito381.server.RepositoryTournamentProfile;
import local.lito381.server.RepositoryTournamentRoster;
import local.lito381.server.RepositoryTournamentShopOffer;
import local.lito381.server.RepositoryWarehousePassword;
import local.lito381.server.RepositoryWarehousePasswordChange;
import local.lito381.server.RepositoryWarehouseSnapshot;
import local.lito381.server.RepositoryWarehouseTransferResult;
import local.lito381.server.RepositoryWaterDragonPassChange;
import local.lito381.server.RepositoryWaterDragonPassSnapshot;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.SharedDatabase;
import local.lito381.server.SourceBaseMagicResistance;
import local.lito381.server.SourceBuffRead;
import local.lito381.server.SourceEquipmentEvaluation;
import local.lito381.server.SourceEquipmentMagicStats;
import local.lito381.server.SourceEquippedFireResistance;
import local.lito381.server.SourceInventoryItem;
import local.lito381.server.SourceInventoryWeightStats;
import local.lito381.server.SourceKillDeathCounts;
import local.lito381.server.SourceMagicDollPower;
import local.lito381.server.SourceMagicDollType;
import local.lito381.server.SourceNpcPhysicalStats;
import local.lito381.server.SourcePet;
import local.lito381.server.SourcePetTemplate;
import local.lito381.server.SourcePhysicalGrowthStats;
import local.lito381.server.SourceSummonTemplate;
import local.lito381.server.UnifiedConfiguration;

final class LegacyMySqlRepository
implements AccountRepository {
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
    private final Map<Integer, SourceNpcPhysicalStats> npcPhysicalCache = new ConcurrentHashMap<Integer, SourceNpcPhysicalStats>();
    private final Map<Integer, SourceSummonTemplate> summonTemplateCache = new ConcurrentHashMap<Integer, SourceSummonTemplate>();
    private final Map<Integer, SourcePetTemplate> petTemplateCache = new ConcurrentHashMap<Integer, SourcePetTemplate>();
    private final Map<Integer, InventoryCatalog.Definition> rewardItemCache = new ConcurrentHashMap<Integer, InventoryCatalog.Definition>();

    LegacyMySqlRepository(Path path, String string, String string2, String string3, double d, boolean bl, String string4) throws IOException, ClassNotFoundException {
        this(path, string, 0, string2, string3, d, bl, string4);
    }

    LegacyMySqlRepository(Path path, String string, int n, String string2, String string3, double d, boolean bl, String string4) throws IOException, ClassNotFoundException {
        Properties properties = new Properties();
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            Properties properties2 = properties;
            properties2.load(bufferedReader);
            UnifiedConfiguration.aliases(properties2);
        }
        String string5 = this.accountDatabase = string2 == null || string2.isBlank() ? LegacyMySqlRepository.required(properties, "URL2_LOGIN", null) : string2.trim();
        if (!this.accountDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("database.legacy.login.database contains unsupported characters");
        }
        String string6 = this.classicSourceName = string == null ? "" : string.trim();
        if (n < 0 || n > 255) {
            throw new IllegalArgumentException("classic source id is outside byte range");
        }
        this.classicSourceId = n;
        if (string3 == null || !string3.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("database.legacy.source.resource.databases contains unsupported characters");
        }
        this.resourceDatabase = string3;
        if (!Double.isFinite(d) || d <= 0.0) {
            throw new IllegalArgumentException("invalid legacy source weight rate");
        }
        this.serverWeightRate = d;
        this.accountUrl = LegacyMySqlRepository.jdbcUrlWithDatabase(properties, "URL1_LOGIN", this.accountDatabase, "URL3_LOGIN");
        this.accountLogin = LegacyMySqlRepository.required(properties, "Login_LOGIN", LegacyMySqlRepository.required(properties, "Login", null));
        this.accountPassword = properties.getProperty("Password_LOGIN", properties.getProperty("Password", ""));
        this.readOnly = bl;
        this.repositoryMode = string4;
        Class.forName(properties.getProperty("Driver_LOGIN", "com.mysql.cj.jdbc.Driver").trim());
        bufferedReader = new HikariConfig();
        bufferedReader.setJdbcUrl(this.accountUrl);
        bufferedReader.setUsername(this.accountLogin);
        bufferedReader.setPassword(this.accountPassword);
        bufferedReader.setReadOnly(bl);
        bufferedReader.setMinimumIdle(0);
        bufferedReader.setMaximumPoolSize(8);
        bufferedReader.setInitializationFailTimeout(-1L);
        bufferedReader.setConnectionTimeout(5000L);
        bufferedReader.setIdleTimeout(60000L);
        bufferedReader.setMaxLifetime(600000L);
        bufferedReader.setPoolName("lito-" + Integer.toUnsignedString((string + "\u0000" + this.accountDatabase).hashCode(), 36));
        this.dataSource = new HikariDataSource((HikariConfig)bufferedReader);
    }

    @Override
    public void healthCheck() throws SQLException {
        AutoCloseable autoCloseable;
        SharedDatabase.check(this);
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "accounts");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "characters");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_other");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_skills");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_quests");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_items");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_pets");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_teleport");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_buff");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_buddys");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_letter");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_warehouse");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_elf_warehouse");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "clan_warehouse");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_items_card");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "character_items_dark");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("skills"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("pettypes"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("commands"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("lito_tournament_shop"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("lito_battle_rule"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("lito_battle_object"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("lito_battle_spawn"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("lito_battle_player_spawn"));
        for (String object2 : LegacyEquipmentMagicLoader.requiredResourceTables()) {
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, LegacyEquipmentMagicLoader.qualified(this.resourceDatabase, object2));
        }
        try (Object object3 = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            LegacyEquipmentMagicLoader.healthCheck((Connection)object3, this.resourceDatabase);
            LegacyInventoryWeightLoader.healthCheck((Connection)object3, this.resourceDatabase);
            try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT OriginalStr,OriginalDex,OriginalCon,Food,HellTime FROM characters LIMIT 0");){
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT char_obj_id,killCount,deathCount FROM character_other LIMIT 0");){
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT npcid,ac,size,damage_reduction,hard,undead,weakAttr,mr,IsTU,IsErase,lawful,tamable,boss,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk FROM " + this.resourceTable("npc") + " LIMIT 0");){
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT p.item_obj_id,p.objid,p.npcid,p.name,p.lvl,p.hp,p.mp,p.exp,p.lawful,i.item_id FROM character_pets p JOIN character_items i ON i.id=p.item_obj_id LIMIT 0");){
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = object3.prepareStatement(this.sourceInventorySql() + " LIMIT 0");){
                preparedStatement.setLong(1, 0L);
                preparedStatement.setLong(2, 0L);
                preparedStatement.setLong(3, 0L);
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT id,char_id,name,locx,locy,mapid FROM character_teleport WHERE char_id=? LIMIT 0");){
                preparedStatement.setLong(1, 0L);
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = SourceBuffRead.healthStatement((Connection)object3, "SELECT b.char_obj_id,b.skill_id,b.remaining_time,b.poly_id,b.snapshot_version,m.completed_version FROM character_buff b LEFT JOIN character_buff_snapshot m ON m.char_obj_id=b.char_obj_id WHERE b.char_obj_id=? LIMIT 0");){
                preparedStatement.setLong(1, 0L);
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
            try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT BaseNpcId,CollarId,ChaCost,HpUpMin,HpUpMax,MpUpMin,MpUpMax,MinEvoLevel,EvolutionItemId,NpcIdForEvolving FROM " + this.resourceTable("pettypes") + " LIMIT 0");){
                autoCloseable = preparedStatement.executeQuery();
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
        }
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.exactResourceTable("\u89d2\u8272_\u7cbe\u795e\u6210\u9577"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.exactResourceTable("\u89d2\u8272_\u9ad4\u8cea\u6210\u9577"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.exactResourceTable("\u89d2\u8272_\u529b\u91cf\u6210\u9577"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.exactResourceTable("\u89d2\u8272_\u654f\u6377\u6210\u9577"));
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "clan_data");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "server_\u57ce\u5821");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "server_\u76df\u5c4b_\u62cd\u8ce3");
        this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "server_\u76df\u5c4b_\u4f48\u544a");
        if (this.repositoryMode.equals("lito_mysql")) {
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_character_origin");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_account_origin");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_item_origin");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_private_inventory");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_warehouse_item_origin");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_buddy_group");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_buddy_metadata");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_clan_metadata");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_castle_metadata");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_castle_security_settlement");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_house_auction_metadata");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_board_topic");
            if (this.classicSourceId == 0) {
                this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_tournament_profile");
            } else {
                this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, this.resourceTable("lito_tournament_character_record"));
            }
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_hotel_lease_key");
            this.checkTable(this.accountUrl, this.accountLogin, this.accountPassword, "lito_water_dragon_pass");
            object3 = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
            try {
                ResultSet resultSet;
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT char_obj_id,group_id,group_name FROM lito_buddy_group LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT char_obj_id,buddy_id,buddy_name,target_source,group_id,relationship_style,palette_offset,origin FROM lito_buddy_metadata LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT item_object_id,code,sender,receiver,date,template_id,subject,content,isCheck FROM character_letter LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT clan_id,lito_owned,destroy_pending,snapshot_version FROM lito_clan_metadata LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT castle_id,security_enabled,mercenary_0,mercenary_1,mercenary_2,mercenary_3,snapshot_version FROM lito_castle_metadata LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT castle_id,war_start_millis,treasury_before,charge_adena,charged_adena,prepared,completed FROM lito_castle_security_settlement LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT house_id,snapshot_version FROM lito_house_auction_metadata LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                try (PreparedStatement preparedStatement = object3.prepareStatement("SELECT id,name,date,title,content FROM `server_\u76df\u5c4b_\u4f48\u544a` LIMIT 0");){
                    autoCloseable = preparedStatement.executeQuery();
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                String string = this.classicSourceId == 0 ? "SELECT char_obj_id,char_name,profile_state,snapshot_version FROM lito_tournament_profile LIMIT 0" : "SELECT source_server_id,char_obj_id,char_name,wins,losses,disconnects,ranking_points,profile_state,snapshot_version FROM " + this.resourceTable("lito_tournament_character_record") + " LIMIT 0";
                autoCloseable = object3.prepareStatement(string);
                try {
                    resultSet = autoCloseable.executeQuery();
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
                finally {
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                autoCloseable = object3.prepareStatement("SELECT char_obj_id,inventory_state,snapshot_version FROM lito_private_inventory LIMIT 0");
                try {
                    resultSet = autoCloseable.executeQuery();
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
                finally {
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                autoCloseable = object3.prepareStatement("SELECT item_obj_id,show_id,npc_id,room_number,meeting_hall,renter_obj_id,renter_identity,expires_at,released FROM lito_hotel_lease_key LIMIT 0");
                try {
                    resultSet = autoCloseable.executeQuery();
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
                finally {
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                autoCloseable = object3.prepareStatement("SELECT item_obj_id,show_id,room_number,renter_obj_id,renter_identity,expires_at FROM lito_water_dragon_pass LIMIT 0");
                try {
                    resultSet = autoCloseable.executeQuery();
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
                finally {
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                autoCloseable = object3.prepareStatement("SELECT lito_event_key FROM `server_\u7d00\u9304_\u734e\u52f5` LIMIT 0");
                try {
                    resultSet = autoCloseable.executeQuery();
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
                finally {
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
                for (AutoCloseable autoCloseable2 : GameStateStore.WarehouseScope.values()) {
                    try (PreparedStatement preparedStatement = object3.prepareStatement(this.warehouseInventorySql((GameStateStore.WarehouseScope)((Object)autoCloseable2)) + " LIMIT 0");){
                        for (int i = 1; i <= 3; ++i) {
                            preparedStatement.setString(i, "");
                        }
                        ResultSet resultSet2 = preparedStatement.executeQuery();
                        if (resultSet2 == null) continue;
                        resultSet2.close();
                    }
                }
            }
            finally {
                if (object3 != null) {
                    object3.close();
                }
            }
        }
    }

    @Override
    public void migrateLitoExtensions() throws SQLException {
        if (SharedDatabase.registered(this)) {
            return;
        }
        if (!this.repositoryMode.equals("lito_mysql")) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(this.accountUrl, this.accountLogin, this.accountPassword);
             Statement statement = connection.createStatement();){
            connection.setReadOnly(false);
            if (!LegacyMySqlRepository.schemaColumnExists(connection, "server_\u7d00\u9304_\u734e\u52f5", "lito_event_key")) {
                statement.executeUpdate("ALTER TABLE `server_\u7d00\u9304_\u734e\u52f5` ADD COLUMN lito_event_key varchar(96) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL AFTER ip");
            }
            if (!LegacyMySqlRepository.schemaIndexExists(connection, "server_\u7d00\u9304_\u734e\u52f5", "uq_lito_reward_event")) {
                statement.executeUpdate("ALTER TABLE `server_\u7d00\u9304_\u734e\u52f5` ADD UNIQUE KEY uq_lito_reward_event (lito_event_key)");
            }
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_buddy_group (char_obj_id int(10) unsigned NOT NULL,group_id smallint(5) unsigned NOT NULL,group_name varchar(45) NOT NULL,PRIMARY KEY (char_obj_id,group_id),UNIQUE KEY uq_lito_buddy_group_name (char_obj_id,group_name)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_buddy_metadata (char_obj_id int(10) unsigned NOT NULL,buddy_id int(10) unsigned NOT NULL DEFAULT 0,buddy_name varchar(45) NOT NULL,target_source varchar(45) NOT NULL DEFAULT '',group_id smallint(5) unsigned NOT NULL DEFAULT 1,relationship_style tinyint(3) unsigned NOT NULL DEFAULT 1,palette_offset tinyint(3) unsigned NOT NULL DEFAULT 1,origin varchar(45) NOT NULL DEFAULT '',PRIMARY KEY (char_obj_id,buddy_name),KEY idx_lito_buddy_target (buddy_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_clan_metadata (clan_id int(10) unsigned NOT NULL,lito_owned tinyint(1) unsigned NOT NULL DEFAULT 0,destroy_pending tinyint(1) unsigned NOT NULL DEFAULT 0,snapshot_version int(10) unsigned NOT NULL DEFAULT 0,PRIMARY KEY (clan_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_castle_metadata (castle_id tinyint(3) unsigned NOT NULL,security_enabled tinyint(1) unsigned NOT NULL DEFAULT 0,mercenary_0 int(10) unsigned NOT NULL DEFAULT 0,mercenary_1 int(10) unsigned NOT NULL DEFAULT 0,mercenary_2 int(10) unsigned NOT NULL DEFAULT 0,mercenary_3 int(10) unsigned NOT NULL DEFAULT 0,snapshot_version int(10) unsigned NOT NULL DEFAULT 0,PRIMARY KEY (castle_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_castle_security_settlement (castle_id tinyint(3) unsigned NOT NULL,war_start_millis bigint(20) unsigned NOT NULL,treasury_before int(10) unsigned NOT NULL,charge_adena int(10) unsigned NOT NULL,charged_adena int(10) unsigned NOT NULL DEFAULT 0,prepared tinyint(1) unsigned NOT NULL DEFAULT 0,completed tinyint(1) unsigned NOT NULL DEFAULT 0,settled_at datetime NOT NULL,PRIMARY KEY (castle_id,war_start_millis)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_house_auction_metadata (house_id int(10) unsigned NOT NULL,snapshot_version int(10) unsigned NOT NULL DEFAULT 0,PRIMARY KEY (house_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_board_topic (topic_id int(10) unsigned NOT NULL,author_obj_id int(10) unsigned NOT NULL,author_name varchar(45) NOT NULL,post_date char(8) NOT NULL,title varchar(40) NOT NULL,content text NOT NULL,created_at datetime NOT NULL,PRIMARY KEY (topic_id),KEY idx_lito_board_author (author_obj_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_tournament_profile (char_obj_id int(10) unsigned NOT NULL,char_name varchar(45) NOT NULL,profile_state mediumtext NOT NULL,snapshot_version int(10) unsigned NOT NULL DEFAULT 1,updated_at datetime NOT NULL,PRIMARY KEY (char_obj_id),UNIQUE KEY uq_lito_tournament_character (char_name)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.resourceTable("lito_tournament_character_record") + " (source_server_id smallint(5) unsigned NOT NULL,char_obj_id int(10) unsigned NOT NULL,char_name varchar(45) NOT NULL,wins int(10) unsigned NOT NULL DEFAULT 0,losses int(10) unsigned NOT NULL DEFAULT 0,disconnects int(10) unsigned NOT NULL DEFAULT 0,ranking_points bigint(20) unsigned NOT NULL DEFAULT 0,profile_state mediumtext NOT NULL,snapshot_version int(10) unsigned NOT NULL DEFAULT 1,updated_at datetime NOT NULL,PRIMARY KEY (source_server_id,char_obj_id),UNIQUE KEY uq_lito_tournament_source_name (source_server_id,char_name)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_private_inventory (char_obj_id int(10) unsigned NOT NULL,inventory_state mediumtext NOT NULL,snapshot_version int(10) unsigned NOT NULL DEFAULT 1,updated_at datetime NOT NULL,PRIMARY KEY (char_obj_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_hotel_lease_key (item_obj_id int(10) unsigned NOT NULL,show_id int(10) unsigned NOT NULL,npc_id int(10) unsigned NOT NULL,room_number tinyint(3) unsigned NOT NULL,meeting_hall tinyint(1) unsigned NOT NULL DEFAULT 0,renter_obj_id int(10) unsigned NOT NULL,renter_identity varchar(95) NOT NULL,expires_at bigint(20) unsigned NOT NULL,released tinyint(1) unsigned NOT NULL DEFAULT 0,PRIMARY KEY (item_obj_id),KEY idx_lito_hotel_show (show_id),KEY idx_lito_hotel_occupancy (npc_id,released,expires_at),KEY idx_lito_hotel_renter (renter_obj_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lito_water_dragon_pass (item_obj_id int(10) unsigned NOT NULL,show_id int(10) unsigned NOT NULL,room_number tinyint(3) unsigned NOT NULL,renter_obj_id int(10) unsigned NOT NULL,renter_identity varchar(95) NOT NULL,expires_at bigint(20) unsigned NOT NULL,PRIMARY KEY (item_obj_id),UNIQUE KEY uq_lito_water_dragon_show (show_id),KEY idx_lito_water_dragon_occupancy (room_number,expires_at),KEY idx_lito_water_dragon_renter (renter_obj_id,expires_at)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
            this.migrateBattlePlayerSpawns(connection, statement);
        }
    }

    private void migrateBattlePlayerSpawns(Connection connection, Statement statement) throws SQLException {
        String string = this.resourceTable("lito_battle_player_spawn");
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + string + " (mode_id tinyint(3) unsigned NOT NULL,map_id smallint(5) unsigned NOT NULL,spawn_index tinyint(3) unsigned NOT NULL,x smallint(5) unsigned NOT NULL,y smallint(5) unsigned NOT NULL,enabled tinyint(1) unsigned NOT NULL DEFAULT 1,note varchar(255) NOT NULL DEFAULT '',PRIMARY KEY (mode_id,map_id,spawn_index),KEY idx_lito_battle_player_spawn_map (map_id,enabled)) ENGINE=MyISAM DEFAULT CHARSET=utf8");
        String string2 = "INSERT IGNORE INTO " + string + " (mode_id,map_id,spawn_index,x,y,enabled,note) VALUES (?,?,?,?,?,1,?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string2);){
            for (RepositoryBattlePlayerSpawn repositoryBattlePlayerSpawn : BattleControlCatalog.defaultPlayerSpawns()) {
                preparedStatement.setInt(1, repositoryBattlePlayerSpawn.modeId());
                preparedStatement.setInt(2, repositoryBattlePlayerSpawn.mapId());
                preparedStatement.setInt(3, repositoryBattlePlayerSpawn.spawnIndex());
                preparedStatement.setInt(4, repositoryBattlePlayerSpawn.x());
                preparedStatement.setInt(5, repositoryBattlePlayerSpawn.y());
                preparedStatement.setString(6, "built-in validated default");
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private static boolean schemaColumnExists(Connection connection, String string, String string2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setString(1, string);
                preparedStatement.setString(2, string2);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static boolean schemaIndexExists(Connection connection, String string, String string2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND INDEX_NAME=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setString(1, string);
                preparedStatement.setString(2, string2);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public AuthResult authenticate(String var1_1, String var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public int accessLevel(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public GmCommandDefinition gmCommand(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public List<RepositoryTournamentShopOffer> tournamentShopOffers() throws SQLException {
        String string = "SELECT item_id,sort_order,buy_price,sell_price FROM " + this.resourceTable("lito_tournament_shop") + " WHERE enabled<>0 ORDER BY sort_order,item_id";
        ArrayList<RepositoryTournamentShopOffer> arrayList = new ArrayList<RepositoryTournamentShopOffer>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                arrayList.add(new RepositoryTournamentShopOffer(resultSet.getLong("item_id"), resultSet.getInt("sort_order"), resultSet.getLong("buy_price"), resultSet.getLong("sell_price")));
            }
        }
        return List.copyOf(arrayList);
    }

    @Override
    public RepositoryBattleConfiguration battleConfiguration() throws SQLException {
        ArrayList<RepositoryBattleRule> arrayList = new ArrayList<RepositoryBattleRule>();
        ArrayList<RepositoryBattleObject> arrayList2 = new ArrayList<RepositoryBattleObject>();
        ArrayList<RepositoryBattleSpawn> arrayList3 = new ArrayList<RepositoryBattleSpawn>();
        ArrayList<RepositoryBattlePlayerSpawn> arrayList4 = new ArrayList<RepositoryBattlePlayerSpawn>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            ResultSet resultSet;
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT mode_id,map_id,enabled,time_limit_seconds,goal,minimum_players FROM " + this.resourceTable("lito_battle_rule") + " ORDER BY mode_id,map_id");){
                resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        arrayList.add(new RepositoryBattleRule(resultSet.getInt("mode_id"), resultSet.getInt("map_id"), resultSet.getInt("enabled") != 0, resultSet.getInt("time_limit_seconds"), resultSet.getInt("goal"), resultSet.getInt("minimum_players")));
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            preparedStatement = connection.prepareStatement("SELECT mode_id,map_id,role,x,y,gfx,maximum_hp FROM " + this.resourceTable("lito_battle_object") + " WHERE enabled<>0 ORDER BY mode_id,map_id,role");
            try {
                resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        arrayList2.add(new RepositoryBattleObject(resultSet.getInt("mode_id"), resultSet.getInt("map_id"), resultSet.getString("role"), resultSet.getInt("x"), resultSet.getInt("y"), resultSet.getInt("gfx"), resultSet.getInt("maximum_hp")));
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
            preparedStatement = connection.prepareStatement("SELECT mode_id,map_id,wave,sort_order,npc_id,quantity,x,y FROM " + this.resourceTable("lito_battle_spawn") + " WHERE enabled<>0 ORDER BY mode_id,map_id,wave,sort_order");
            try {
                resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        Object object = resultSet.getObject("x");
                        Object object2 = resultSet.getObject("y");
                        arrayList3.add(new RepositoryBattleSpawn(resultSet.getInt("mode_id"), resultSet.getInt("map_id"), resultSet.getInt("wave"), resultSet.getInt("sort_order"), resultSet.getInt("npc_id"), resultSet.getInt("quantity"), object == null ? null : Integer.valueOf(((Number)object).intValue()), object2 == null ? null : Integer.valueOf(((Number)object2).intValue())));
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
            preparedStatement = connection.prepareStatement("SELECT mode_id,map_id,spawn_index,x,y FROM " + this.resourceTable("lito_battle_player_spawn") + " WHERE enabled<>0 ORDER BY mode_id,map_id,spawn_index");
            try {
                resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        arrayList4.add(new RepositoryBattlePlayerSpawn(resultSet.getInt("mode_id"), resultSet.getInt("map_id"), resultSet.getInt("spawn_index"), resultSet.getInt("x"), resultSet.getInt("y")));
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        }
        return new RepositoryBattleConfiguration(arrayList, arrayList2, arrayList3, arrayList4);
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean accountExists(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public AccountMutationResult createAccount(String var1_1, String var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean deleteLitoAccount(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 3 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public AccountMutationResult changePassword(String string, String string2, String string3) throws SQLException {
        AutoCloseable autoCloseable;
        AutoCloseable autoCloseable2;
        String string4 = "SELECT password FROM accounts WHERE login = ? LIMIT 1";
        try (Object object = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            autoCloseable2 = object.prepareStatement(string4);
            try {
                autoCloseable2.setString(1, string);
                autoCloseable = autoCloseable2.executeQuery();
                try {
                    if (!autoCloseable.next()) {
                        AccountMutationResult accountMutationResult = AccountMutationResult.NOT_FOUND;
                        return accountMutationResult;
                    }
                    if (!string2.equals(autoCloseable.getString("password"))) {
                        AccountMutationResult accountMutationResult = AccountMutationResult.WRONG_PASSWORD;
                        return accountMutationResult;
                    }
                    if (this.readOnly || !this.repositoryMode.equals("lito_mysql")) {
                        AccountMutationResult accountMutationResult = AccountMutationResult.READ_ONLY;
                        return accountMutationResult;
                    }
                }
                finally {
                    if (autoCloseable != null) {
                        autoCloseable.close();
                    }
                }
            }
            finally {
                if (autoCloseable2 != null) {
                    autoCloseable2.close();
                }
            }
        }
        object = "UPDATE accounts SET password=?,lastactive=NOW() WHERE login=? AND password=?";
        autoCloseable2 = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
        try {
            AccountMutationResult accountMutationResult;
            block42: {
                autoCloseable = autoCloseable2.prepareStatement((String)object);
                try {
                    autoCloseable.setString(1, string3);
                    autoCloseable.setString(2, string);
                    autoCloseable.setString(3, string2);
                    AccountMutationResult accountMutationResult2 = accountMutationResult = autoCloseable.executeUpdate() == 1 ? AccountMutationResult.SUCCESS : AccountMutationResult.WRONG_PASSWORD;
                    if (autoCloseable == null) break block42;
                }
                catch (Throwable throwable) {
                    if (autoCloseable != null) {
                        try {
                            autoCloseable.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                autoCloseable.close();
            }
            return accountMutationResult;
        }
        finally {
            if (autoCloseable2 != null) {
                autoCloseable2.close();
            }
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public AccountMutationResult registerQuiz(String var1_1, String var2_2, String var3_3, List<String> var4_4) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public AccountMutationResult changeAccountInfo(String var1_1, int var2_2, List<String> var3_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public List<CharacterSummary> characters(String string) throws SQLException {
        String string2 = "SELECT c.char_name, c.Clanname, c.Type, c.Sex, c.level,\n       c.MaxHp, c.MaxMp, c.Ac,\n       c.Str, c.Con, c.Dex, c.Cha, c.Intel, c.Wis,\n       c.OriginalInt, c.OriginalWis, c.OriginalStr, c.OriginalDex,\n       objid, Exp, Lawful, PKcount, LastPk,\n       PkCountForElf, LastPkForElf, Class,\n       LocX, LocY, MapID, c.OriginalCon, c.Food, c.ElfAttr,\n       COALESCE(h.`\u56de\u8840`, 1) AS con_hpr_maximum\n  FROM characters c\n  LEFT JOIN %s h ON h.id = LEAST(GREATEST(c.Con, 1), 50)\n WHERE c.account_name = ? AND c.Banned = 0\n ORDER BY c.char_name\n LIMIT 8\n".formatted(this.exactResourceTable("\u89d2\u8272_\u9ad4\u8cea\u6210\u9577"));
        ArrayList<CharacterSummary> arrayList = new ArrayList<CharacterSummary>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string2);){
            preparedStatement.setString(1, string);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp("LastPk");
                    Timestamp timestamp2 = resultSet.getTimestamp("LastPkForElf");
                    arrayList.add(new CharacterSummary(resultSet.getString("char_name"), resultSet.getString("Clanname"), resultSet.getInt("Type"), resultSet.getInt("Sex"), resultSet.getInt("level"), resultSet.getInt("MaxHp"), resultSet.getInt("MaxMp"), resultSet.getInt("Ac"), resultSet.getInt("Str"), resultSet.getInt("Con"), resultSet.getInt("Dex"), resultSet.getInt("Cha"), resultSet.getInt("Intel"), resultSet.getInt("Wis"), resultSet.getInt("OriginalInt"), resultSet.getInt("OriginalWis"), resultSet.getLong("objid"), resultSet.getLong("Exp"), resultSet.getLong("Lawful"), resultSet.getInt("PKcount"), timestamp == null ? 0L : Math.max(0L, timestamp.getTime() / 1000L), resultSet.getInt("PkCountForElf"), timestamp2 == null ? 0L : Math.max(0L, timestamp2.getTime() / 1000L), resultSet.getLong("Class"), resultSet.getLong("LocX"), resultSet.getLong("LocY"), resultSet.getLong("MapID"), resultSet.getInt("OriginalCon"), resultSet.getInt("Food"), Math.max(1, resultSet.getInt("con_hpr_maximum")), resultSet.getInt("OriginalStr"), resultSet.getInt("OriginalDex"), resultSet.getInt("ElfAttr")));
                }
            }
        }
        return arrayList;
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCharacterMutation createLitoCharacter(String var1_1, Protocol.CustomCharacterRequest var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 1[TRYBLOCK]], but top level block is 12[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCharacterMutation deleteLitoCharacter(String var1_1, String var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public RepositoryPersistenceResult persistLitoCharacter(LitoCharacterPersistence litoCharacterPersistence) throws SQLException {
        if (this.readOnly || !this.repositoryMode.equals("lito_mysql")) {
            return RepositoryPersistenceResult.READ_ONLY;
        }
        Object object = litoCharacterPersistence.skills().iterator();
        while (object.hasNext()) {
            int n = object.next();
            if (n > 0 && n <= 224) continue;
            throw new IllegalArgumentException("skill id outside native range: " + n);
        }
        for (Map.Entry entry : litoCharacterPersistence.quests().entrySet()) {
            if ((Integer)entry.getKey() > 0 && (Integer)entry.getValue() >= 0) continue;
            throw new IllegalArgumentException("invalid quest persistence row: " + String.valueOf(entry));
        }
        object = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
        try {
            String string = "UPDATE characters SET CurHp=?,CurMp=?,level=?,HighLevel=GREATEST(HighLevel,?),Exp=?,Lawful=?,PKcount=?,LastPk=?,PkCountForElf=?,LastPkForElf=?,HellTime=?,LocX=?,LocY=?,MapID=?,Heading=?,ElfAttr=?,OnlineStatus=0 WHERE objid=? AND account_name=? AND char_name=?";
            try (Object object2 = object.prepareStatement(string);){
                int n = 1;
                object2.setInt(n++, litoCharacterPersistence.currentHp());
                object2.setInt(n++, litoCharacterPersistence.currentMp());
                object2.setInt(n++, litoCharacterPersistence.level());
                object2.setInt(n++, litoCharacterPersistence.level());
                object2.setLong(n++, litoCharacterPersistence.experience());
                object2.setInt(n++, litoCharacterPersistence.lawful());
                object2.setInt(n++, litoCharacterPersistence.pkCount());
                LegacyMySqlRepository.setEpochTimestamp(object2, n++, litoCharacterPersistence.lastPkEpochSeconds());
                object2.setInt(n++, litoCharacterPersistence.elfPkCount());
                LegacyMySqlRepository.setEpochTimestamp(object2, n++, litoCharacterPersistence.lastElfPkEpochSeconds());
                object2.setInt(n++, litoCharacterPersistence.hellTime());
                object2.setInt(n++, litoCharacterPersistence.x());
                object2.setInt(n++, litoCharacterPersistence.y());
                object2.setInt(n++, litoCharacterPersistence.mapId());
                object2.setInt(n++, litoCharacterPersistence.heading());
                object2.setInt(n++, litoCharacterPersistence.elfAttribute());
                object2.setLong(n++, litoCharacterPersistence.objectId());
                object2.setString(n++, litoCharacterPersistence.account());
                object2.setString(n, litoCharacterPersistence.characterName());
                if (object2.executeUpdate() != 1) {
                    RepositoryPersistenceResult repositoryPersistenceResult = RepositoryPersistenceResult.NOT_FOUND;
                    return repositoryPersistenceResult;
                }
            }
            object2 = object.prepareStatement("INSERT INTO character_other (char_obj_id,killCount,deathCount) VALUES (?,?,?) ON DUPLICATE KEY UPDATE killCount=VALUES(killCount),deathCount=VALUES(deathCount)");
            try {
                object2.setLong(1, litoCharacterPersistence.objectId());
                object2.setInt(2, litoCharacterPersistence.killCount());
                object2.setInt(3, litoCharacterPersistence.deathCount());
                object2.executeUpdate();
            }
            finally {
                if (object2 != null) {
                    object2.close();
                }
            }
            LegacyMySqlRepository.reconcileSkills((Connection)object, litoCharacterPersistence.objectId(), litoCharacterPersistence.skills());
            LegacyMySqlRepository.reconcileQuests((Connection)object, litoCharacterPersistence.objectId(), litoCharacterPersistence.quests());
            object2 = RepositoryPersistenceResult.SUCCESS;
            return object2;
        }
        finally {
            if (object != null) {
                object.close();
            }
        }
    }

    /*
     * Could not resolve type clashes
     * Loose catch block
     */
    @Override
    public RepositoryPersistenceResult persistLitoSourceInventory(long l, List<LitoSourceInventoryMutation> list) throws SQLException {
        if (this.readOnly || !this.repositoryMode.equals("lito_mysql")) {
            return RepositoryPersistenceResult.READ_ONLY;
        }
        if (l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid source inventory character id");
        }
        List<Object> list2 = list == null ? List.of() : List.copyOf(list);
        LinkedHashSet<Long> linkedHashSet = new LinkedHashSet<Long>();
        for (Object object2 : list2) {
            if (object2 != null && linkedHashSet.add(((LitoSourceInventoryMutation)object2).sourceObjectId())) continue;
            throw new IllegalArgumentException("duplicate/null source inventory mutation");
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            Object object;
            block28: {
                Object object2;
                block25: {
                    RepositoryPersistenceResult repositoryPersistenceResult;
                    block27: {
                        block26: {
                            object2 = connection.prepareStatement("SELECT 1 FROM characters WHERE objid=? LIMIT 1");
                            object2.setLong(1, l);
                            object = object2.executeQuery();
                            if (object.next()) break block25;
                            repositoryPersistenceResult = RepositoryPersistenceResult.NOT_FOUND;
                            if (object == null) break block26;
                            object.close();
                        }
                        if (object2 == null) break block27;
                        object2.close();
                    }
                    return repositoryPersistenceResult;
                }
                try {
                    block29: {
                        if (object != null) {
                            object.close();
                        }
                        break block29;
                        {
                            catch (Throwable throwable) {
                                if (object != null) {
                                    try {
                                        object.close();
                                    }
                                    catch (Throwable throwable2) {
                                        throwable.addSuppressed(throwable2);
                                    }
                                }
                                throw throwable;
                            }
                        }
                    }
                    for (LitoSourceInventoryMutation litoSourceInventoryMutation : list2) {
                        if (litoSourceInventoryMutation.delete()) {
                            LegacyMySqlRepository.deleteSourceInventoryRow(connection, l, litoSourceInventoryMutation.sourceObjectId());
                            continue;
                        }
                        LegacyMySqlRepository.updateSourceInventoryRow(connection, l, litoSourceInventoryMutation);
                    }
                    object = RepositoryPersistenceResult.SUCCESS;
                    if (object2 == null) break block28;
                }
                catch (Throwable throwable) {
                    if (object2 != null) {
                        try {
                            object2.close();
                        }
                        catch (Throwable throwable3) {
                            throwable.addSuppressed(throwable3);
                        }
                    }
                    throw throwable;
                }
                object2.close();
            }
            return object;
        }
    }

    @Override
    public RepositoryPersistenceResult persistLitoInventory(long l, List<LitoSourceInventoryMutation> list, List<LitoOwnedInventoryItem> list2) throws SQLException {
        return this.persistLitoInventory(l, list, list2, List.of(), Set.of());
    }

    @Override
    public RepositoryPersistenceResult persistLitoInventory(long l, List<LitoSourceInventoryMutation> list, List<LitoOwnedInventoryItem> list2, List<LitoPetPersistence> list3, Set<Long> set) throws SQLException {
        return this.persistLitoInventory(l, list, list2, list3, set, null);
    }

    @Override
    public RepositoryPersistenceResult persistLitoInventory(long l, List<LitoSourceInventoryMutation> list, List<LitoOwnedInventoryItem> list2, List<LitoPetPersistence> list3, Set<Long> set, RepositoryPrivateInventorySnapshot repositoryPrivateInventorySnapshot) throws SQLException {
        return this.persistLitoInventoryInternal(l, list, list2, list3, set, repositoryPrivateInventorySnapshot, null, null);
    }

    @Override
    public RepositoryPersistenceResult persistLitoHotelInventory(long l, List<LitoSourceInventoryMutation> list, List<LitoOwnedInventoryItem> list2, List<LitoPetPersistence> list3, Set<Long> set, RepositoryPrivateInventorySnapshot repositoryPrivateInventorySnapshot, RepositoryHotelChange repositoryHotelChange) throws SQLException {
        if (repositoryHotelChange == null) {
            throw new IllegalArgumentException("hotel SQL change is required");
        }
        return this.persistLitoInventoryInternal(l, list, list2, list3, set, repositoryPrivateInventorySnapshot, repositoryHotelChange, null);
    }

    @Override
    public RepositoryPersistenceResult persistLitoWaterDragonPassInventory(long l, List<LitoSourceInventoryMutation> list, List<LitoOwnedInventoryItem> list2, List<LitoPetPersistence> list3, Set<Long> set, RepositoryPrivateInventorySnapshot repositoryPrivateInventorySnapshot, RepositoryWaterDragonPassChange repositoryWaterDragonPassChange) throws SQLException {
        if (repositoryWaterDragonPassChange == null) {
            throw new IllegalArgumentException("Water Dragon SQL change is required");
        }
        if (!repositoryWaterDragonPassChange.pass().sourceId().equalsIgnoreCase(this.classicSourceName)) {
            throw new IllegalArgumentException("Water Dragon pass source does not match repository route");
        }
        return this.persistLitoInventoryInternal(l, list, list2, list3, set, repositoryPrivateInventorySnapshot, null, repositoryWaterDragonPassChange);
    }

    /*
     * Exception decompiling
     */
    private RepositoryPersistenceResult persistLitoInventoryInternal(long var1_1, List<LitoSourceInventoryMutation> var3_2, List<LitoOwnedInventoryItem> var4_3, List<LitoPetPersistence> var5_4, Set<Long> var6_5, RepositoryPrivateInventorySnapshot var7_6, RepositoryHotelChange var8_7, RepositoryWaterDragonPassChange var9_8) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static boolean validateWaterDragonPassChange(Connection connection, long l, RepositoryWaterDragonPassChange repositoryWaterDragonPassChange, Map<Long, LitoOwnedInventoryItem> map) throws SQLException {
        ResultSet resultSet;
        GameStateStore.WaterDragonPassState waterDragonPassState = repositoryWaterDragonPassChange.pass();
        LitoOwnedInventoryItem litoOwnedInventoryItem = map.get(waterDragonPassState.itemObjectId());
        if (litoOwnedInventoryItem == null || litoOwnedInventoryItem.itemId() != 82504 || litoOwnedInventoryItem.quantity() != 32 || litoOwnedInventoryItem.expiresAtEpochMillis() <= 0L || Math.abs(litoOwnedInventoryItem.expiresAtEpochMillis() / 1000L - waterDragonPassState.expiresAtEpochSeconds()) > 2L) {
            return false;
        }
        long l2 = System.currentTimeMillis() / 1000L;
        if (waterDragonPassState.expiresAtEpochSeconds() <= l2 || waterDragonPassState.expiresAtEpochSeconds() > l2 + 7200L + 2L) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM lito_water_dragon_pass WHERE expires_at<=?");){
            preparedStatement.setLong(1, l2);
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("SELECT 1 FROM lito_water_dragon_pass WHERE item_obj_id=? OR show_id=? OR room_number=? OR renter_obj_id=? LIMIT 1");
        try {
            preparedStatement.setLong(1, waterDragonPassState.itemObjectId());
            preparedStatement.setInt(2, waterDragonPassState.showId());
            preparedStatement.setInt(3, waterDragonPassState.roomNumber());
            preparedStatement.setLong(4, l);
            resultSet = preparedStatement.executeQuery();
            try {
                if (resultSet.next()) {
                    boolean bl = false;
                    return bl;
                }
            }
            finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM lito_water_dragon_pass WHERE expires_at>?");
        try {
            boolean bl;
            block36: {
                preparedStatement.setLong(1, l2);
                resultSet = preparedStatement.executeQuery();
                try {
                    boolean bl2 = bl = resultSet.next() && resultSet.getInt(1) < 16;
                    if (resultSet == null) break block36;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void applyWaterDragonPassChange(Connection connection, long l, RepositoryWaterDragonPassChange repositoryWaterDragonPassChange) throws SQLException {
        GameStateStore.WaterDragonPassState waterDragonPassState = repositoryWaterDragonPassChange.pass();
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_water_dragon_pass (item_obj_id,show_id,room_number,renter_obj_id,renter_identity,expires_at) VALUES (?,?,?,?,?,?)");){
            preparedStatement.setLong(1, waterDragonPassState.itemObjectId());
            preparedStatement.setInt(2, waterDragonPassState.showId());
            preparedStatement.setInt(3, waterDragonPassState.roomNumber());
            preparedStatement.setLong(4, l);
            preparedStatement.setString(5, waterDragonPassState.renterIdentity());
            preparedStatement.setLong(6, waterDragonPassState.expiresAtEpochSeconds());
            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException("Water Dragon pass insert count changed");
            }
        }
    }

    private static boolean validateHotelChange(Connection connection, long l, RepositoryHotelChange repositoryHotelChange, Map<Long, LitoOwnedInventoryItem> map) throws SQLException {
        return repositoryHotelChange.kind() == RepositoryHotelChangeKind.RENT ? LegacyMySqlRepository.validateHotelRent(connection, l, repositoryHotelChange, map) : LegacyMySqlRepository.validateHotelReturn(connection, l, repositoryHotelChange);
    }

    private static boolean validateHotelRent(Connection connection, long l, RepositoryHotelChange repositoryHotelChange, Map<Long, LitoOwnedInventoryItem> map) throws SQLException {
        GameStateStore.HotelLeaseState hotelLeaseState = repositoryHotelChange.lease();
        if (!repositoryHotelChange.keyObjectIds().contains(hotelLeaseState.showId())) {
            return false;
        }
        Object object = repositoryHotelChange.keyObjectIds().iterator();
        while (object.hasNext()) {
            long l2 = object.next();
            LitoOwnedInventoryItem litoOwnedInventoryItem = map.get(l2);
            if (litoOwnedInventoryItem != null && (long)litoOwnedInventoryItem.itemId() == 40312L && litoOwnedInventoryItem.quantity() == 1 && litoOwnedInventoryItem.expiresAtEpochMillis() > 0L && Math.abs(litoOwnedInventoryItem.expiresAtEpochMillis() / 1000L - hotelLeaseState.expiresAtEpochSeconds()) <= 2L) continue;
            return false;
        }
        object = connection.prepareStatement("SELECT 1 FROM lito_hotel_lease_key WHERE show_id=? OR item_obj_id=? LIMIT 1");
        try {
            for (long l3 : repositoryHotelChange.keyObjectIds()) {
                object.setLong(1, hotelLeaseState.showId());
                object.setLong(2, l3);
                ResultSet resultSet = object.executeQuery();
                try {
                    if (!resultSet.next()) continue;
                    boolean bl = false;
                    return bl;
                }
                finally {
                    if (resultSet == null) continue;
                    resultSet.close();
                }
            }
        }
        finally {
            if (object != null) {
                object.close();
            }
        }
        long l4 = System.currentTimeMillis() / 1000L;
        LinkedHashSet<Long> linkedHashSet = new LinkedHashSet<Long>();
        int n = 0;
        int n2 = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT show_id,room_number,meeting_hall,renter_obj_id FROM lito_hotel_lease_key WHERE npc_id=? AND released=0 AND expires_at>? GROUP BY show_id,room_number,meeting_hall,renter_obj_id");){
            preparedStatement.setInt(1, hotelLeaseState.npcId());
            preparedStatement.setLong(2, l4);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    long l5 = resultSet.getLong("show_id");
                    if (!linkedHashSet.add(l5)) continue;
                    if (resultSet.getLong("renter_obj_id") == l || resultSet.getInt("room_number") == hotelLeaseState.roomNumber()) {
                        boolean bl = false;
                        return bl;
                    }
                    if (resultSet.getBoolean("meeting_hall")) {
                        ++n2;
                        continue;
                    }
                    ++n;
                }
            }
        }
        return hotelLeaseState.meetingHall() ? n2 < 4 : n < 12;
    }

    private static boolean validateHotelReturn(Connection connection, long l, RepositoryHotelChange repositoryHotelChange) throws SQLException {
        GameStateStore.HotelReturnQuote hotelReturnQuote = repositoryHotelChange.returnQuote();
        HotelSqlLease hotelSqlLease = LegacyMySqlRepository.selectHotelLeaseForRenter(connection, hotelReturnQuote.npcId(), l);
        if (hotelReturnQuote.releasesLease() != (hotelSqlLease != null)) {
            return false;
        }
        int n = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT lito_hotel_lease_key.npc_id FROM lito_hotel_lease_key JOIN character_items ON character_items.id=lito_hotel_lease_key.item_obj_id WHERE lito_hotel_lease_key.item_obj_id=? AND character_items.char_id=? LIMIT 1");){
            for (long l2 : repositoryHotelChange.keyObjectIds()) {
                preparedStatement.setLong(1, l2);
                preparedStatement.setLong(2, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    if (!resultSet.next() || resultSet.getInt("npc_id") != hotelReturnQuote.npcId()) {
                        boolean bl = false;
                        return bl;
                    }
                    ++n;
                }
                finally {
                    if (resultSet == null) continue;
                    resultSet.close();
                }
            }
        }
        int n2 = Math.multiplyExact(n, 20);
        if (hotelSqlLease != null && hotelSqlLease.active()) {
            n2 = Math.addExact(n2, 60);
        }
        return hotelReturnQuote.refund() == n2;
    }

    private static HotelSqlLease selectHotelLeaseForRenter(Connection connection, int n, long l) throws SQLException {
        long l2 = System.currentTimeMillis() / 1000L;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT show_id,expires_at,released FROM lito_hotel_lease_key WHERE npc_id=? AND renter_obj_id=? GROUP BY show_id,expires_at,released ORDER BY IF(released=0 AND expires_at>?,0,1),show_id LIMIT 1");){
            HotelSqlLease hotelSqlLease;
            block16: {
                ResultSet resultSet;
                block14: {
                    HotelSqlLease hotelSqlLease2;
                    block15: {
                        preparedStatement.setInt(1, n);
                        preparedStatement.setLong(2, l);
                        preparedStatement.setLong(3, l2);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block14;
                            hotelSqlLease2 = null;
                            if (resultSet == null) break block15;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return hotelSqlLease2;
                }
                hotelSqlLease = new HotelSqlLease(resultSet.getLong("show_id"), !resultSet.getBoolean("released") && resultSet.getLong("expires_at") > l2);
                if (resultSet == null) break block16;
                resultSet.close();
            }
            return hotelSqlLease;
        }
    }

    private static void applyHotelChange(Connection connection, long l, RepositoryHotelChange repositoryHotelChange) throws SQLException {
        PreparedStatement preparedStatement;
        if (repositoryHotelChange.kind() == RepositoryHotelChangeKind.RENT) {
            GameStateStore.HotelLeaseState hotelLeaseState = repositoryHotelChange.lease();
            try (PreparedStatement preparedStatement2 = connection.prepareStatement("INSERT INTO lito_hotel_lease_key (item_obj_id,show_id,npc_id,room_number,meeting_hall,renter_obj_id,renter_identity,expires_at,released) VALUES (?,?,?,?,?,?,?,?,0)");){
                Object object = repositoryHotelChange.keyObjectIds().iterator();
                while (object.hasNext()) {
                    long l2 = object.next();
                    preparedStatement2.setLong(1, l2);
                    preparedStatement2.setLong(2, hotelLeaseState.showId());
                    preparedStatement2.setInt(3, hotelLeaseState.npcId());
                    preparedStatement2.setInt(4, hotelLeaseState.roomNumber());
                    preparedStatement2.setBoolean(5, hotelLeaseState.meetingHall());
                    preparedStatement2.setLong(6, l);
                    preparedStatement2.setString(7, hotelLeaseState.renterIdentity());
                    preparedStatement2.setLong(8, hotelLeaseState.expiresAtEpochSeconds());
                    preparedStatement2.addBatch();
                }
                object = preparedStatement2.executeBatch();
                if (((Object)object).length != repositoryHotelChange.keyObjectIds().size()) {
                    throw new SQLException("hotel lease/key insert count changed");
                }
            }
            return;
        }
        GameStateStore.HotelReturnQuote hotelReturnQuote = repositoryHotelChange.returnQuote();
        HotelSqlLease hotelSqlLease = LegacyMySqlRepository.selectHotelLeaseForRenter(connection, hotelReturnQuote.npcId(), l);
        if (hotelReturnQuote.releasesLease() && hotelSqlLease != null) {
            preparedStatement = connection.prepareStatement("UPDATE lito_hotel_lease_key SET released=1 WHERE show_id=?");
            try {
                preparedStatement.setLong(1, hotelSqlLease.showId());
                preparedStatement.executeUpdate();
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM lito_hotel_lease_key WHERE item_obj_id=?");
        try {
            for (long l3 : repositoryHotelChange.keyObjectIds()) {
                preparedStatement.setLong(1, l3);
                if (preparedStatement.executeUpdate() == 1) continue;
                throw new SQLException("hotel key relation disappeared");
            }
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    /*
     * Exception decompiling
     */
    private static RepositoryPrivateInventorySnapshot loadPrivateInventorySnapshot(Connection var0, long var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK]], but top level block is 4[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryPersistenceResult persistTournamentRewardInventory(long var1_1, RepositoryPrivateInventorySnapshot var3_2, List<LitoOwnedInventoryItem> var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static Set<Long> litoOwnedTournamentRewardIds(Connection connection, long l) throws SQLException {
        LinkedHashSet<Long> linkedHashSet = new LinkedHashSet<Long>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT item_obj_id FROM lito_item_origin WHERE char_obj_id=? AND tournament_reward=1");){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    linkedHashSet.add(resultSet.getLong(1));
                }
            }
        }
        return linkedHashSet;
    }

    private static void persistPrivateInventorySnapshot(Connection connection, long l, long l2, List<LitoOwnedInventoryItem> list) throws SQLException {
        long l3;
        try {
            l3 = Math.incrementExact(l2);
        }
        catch (ArithmeticException arithmeticException) {
            throw new SQLException("LiTo-private inventory version exhausted", arithmeticException);
        }
        if (l3 > 0xFFFFFFFFL) {
            throw new SQLException("LiTo-private inventory version exhausted");
        }
        String string = LegacyMySqlRepository.encodePrivateInventory(list);
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_private_inventory (char_obj_id,inventory_state,snapshot_version,updated_at) VALUES (?,?,?,NOW()) ON DUPLICATE KEY UPDATE inventory_state=VALUES(inventory_state),snapshot_version=VALUES(snapshot_version),updated_at=NOW()");){
            preparedStatement.setLong(1, l);
            preparedStatement.setString(2, string);
            preparedStatement.setLong(3, l3);
            if (preparedStatement.executeUpdate() < 1) {
                throw new SQLException("LiTo-private inventory upsert changed no rows");
            }
        }
    }

    private static String encodePrivateInventory(List<LitoOwnedInventoryItem> list) throws SQLException {
        RepositoryPrivateInventorySnapshot repositoryPrivateInventorySnapshot = new RepositoryPrivateInventorySnapshot(true, 0L, list);
        if (repositoryPrivateInventorySnapshot.items().size() > 256) {
            throw new SQLException("LiTo-private inventory exceeds native slot capacity");
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);){
                dataOutputStream.writeInt(1);
                dataOutputStream.writeInt(repositoryPrivateInventorySnapshot.items().size());
                for (LitoOwnedInventoryItem litoOwnedInventoryItem : repositoryPrivateInventorySnapshot.items()) {
                    dataOutputStream.writeLong(litoOwnedInventoryItem.objectId());
                    dataOutputStream.writeInt(litoOwnedInventoryItem.itemId());
                    dataOutputStream.writeUTF(litoOwnedInventoryItem.itemName());
                    dataOutputStream.writeInt(litoOwnedInventoryItem.quantity());
                    dataOutputStream.writeBoolean(litoOwnedInventoryItem.equipped());
                    dataOutputStream.writeBoolean(litoOwnedInventoryItem.sealed());
                    dataOutputStream.writeInt(litoOwnedInventoryItem.enchantLevel());
                    dataOutputStream.writeBoolean(litoOwnedInventoryItem.identified());
                    dataOutputStream.writeInt(litoOwnedInventoryItem.durability());
                    dataOutputStream.writeInt(litoOwnedInventoryItem.chargeCount());
                    dataOutputStream.writeInt(litoOwnedInventoryItem.blessing());
                    dataOutputStream.writeLong(litoOwnedInventoryItem.expiresAtEpochMillis());
                    dataOutputStream.writeBoolean(litoOwnedInventoryItem.tournamentReward());
                }
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        }
        catch (IOException iOException) {
            throw new SQLException("cannot encode LiTo-private inventory", iOException);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static List<LitoOwnedInventoryItem> decodePrivateInventory(String string) throws SQLException {
        try {
            byte[] byArray = Base64.getDecoder().decode(string == null ? "" : string);
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byArray);){
                List<LitoOwnedInventoryItem> list;
                try (DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);){
                    if (dataInputStream.readInt() != 1) {
                        throw new IOException("unsupported private inventory format");
                    }
                    int n = dataInputStream.readInt();
                    if (n < 0 || n > 256) {
                        throw new IOException("invalid private inventory size");
                    }
                    ArrayList<LitoOwnedInventoryItem> arrayList = new ArrayList<LitoOwnedInventoryItem>(n);
                    for (int i = 0; i < n; ++i) {
                        arrayList.add(new LitoOwnedInventoryItem(dataInputStream.readLong(), dataInputStream.readInt(), dataInputStream.readUTF(), dataInputStream.readInt(), dataInputStream.readBoolean(), dataInputStream.readBoolean(), dataInputStream.readInt(), dataInputStream.readBoolean(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readLong(), dataInputStream.readBoolean()));
                    }
                    if (byteArrayInputStream.available() != 0) {
                        throw new IOException("trailing private inventory data");
                    }
                    list = new RepositoryPrivateInventorySnapshot(true, 0L, arrayList).items();
                }
                return list;
            }
        }
        catch (IOException | IllegalArgumentException exception) {
            throw new SQLException("cannot decode LiTo-private inventory", exception);
        }
    }

    private static void validateSourcePetRow(Connection connection, long l, long l2) throws SQLException {
        ResultSet resultSet;
        String string = "SELECT 1 FROM character_items WHERE id=? AND char_id=? AND item_id IN (?,?) LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l);
            preparedStatement.setLong(3, 40314L);
            preparedStatement.setLong(4, 40316L);
            resultSet = preparedStatement.executeQuery();
            try {
                if (!resultSet.next()) {
                    throw new SQLException("source pet collar row disappeared: " + l2);
                }
            }
            finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
        preparedStatement = connection.prepareStatement("SELECT 1 FROM character_pets WHERE item_obj_id=? LIMIT 1");
        try {
            preparedStatement.setLong(1, l2);
            resultSet = preparedStatement.executeQuery();
            try {
                if (!resultSet.next()) {
                    throw new SQLException("source pet row disappeared: " + l2);
                }
            }
            finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void updateSourcePetRow(Connection connection, LitoPetPersistence litoPetPersistence) throws SQLException {
        String string = "UPDATE character_pets SET npcid=?,name=?,lvl=?,hp=?,mp=?,exp=?,lawful=? WHERE item_obj_id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            int n = 1;
            preparedStatement.setInt(n++, litoPetPersistence.npcId());
            preparedStatement.setString(n++, litoPetPersistence.name());
            preparedStatement.setInt(n++, litoPetPersistence.level());
            preparedStatement.setInt(n++, litoPetPersistence.maxHp());
            preparedStatement.setInt(n++, litoPetPersistence.maxMp());
            preparedStatement.setLong(n++, litoPetPersistence.experience());
            preparedStatement.setInt(n++, litoPetPersistence.lawful());
            preparedStatement.setLong(n, litoPetPersistence.sourceCollarObjectId());
            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException("source pet row disappeared: " + litoPetPersistence.sourceCollarObjectId());
            }
        }
    }

    private static void upsertLitoOwnedPetRow(Connection connection, LitoPetPersistence litoPetPersistence) throws SQLException {
        String string = "INSERT INTO character_pets (item_obj_id,objid,npcid,name,lvl,hp,mp,exp,lawful) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE objid=VALUES(objid),npcid=VALUES(npcid),name=VALUES(name),lvl=VALUES(lvl),hp=VALUES(hp),mp=VALUES(mp),exp=VALUES(exp),lawful=VALUES(lawful)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            int n = 1;
            preparedStatement.setLong(n++, litoPetPersistence.collarObjectId());
            preparedStatement.setLong(n++, litoPetPersistence.petObjectId());
            preparedStatement.setInt(n++, litoPetPersistence.npcId());
            preparedStatement.setString(n++, litoPetPersistence.name());
            preparedStatement.setInt(n++, litoPetPersistence.level());
            preparedStatement.setInt(n++, litoPetPersistence.maxHp());
            preparedStatement.setInt(n++, litoPetPersistence.maxMp());
            preparedStatement.setLong(n++, litoPetPersistence.experience());
            preparedStatement.setInt(n, litoPetPersistence.lawful());
            preparedStatement.executeUpdate();
        }
    }

    private static void deleteSourcePetAndCollar(Connection connection, long l, long l2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM character_pets WHERE item_obj_id=?");){
            preparedStatement.setLong(1, l2);
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_items_time WHERE itemr_obj_id=?");
        try {
            preparedStatement.setLong(1, l2);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_items WHERE id=? AND char_id=?");
        try {
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l);
            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException("source pet collar disappeared: " + l2);
            }
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private boolean resourceItemTemplateExists(Connection connection, int n) throws SQLException {
        String string = "SELECT ((SELECT COUNT(*) FROM " + this.resourceTable("weapon") + " WHERE item_id=?)+(SELECT COUNT(*) FROM " + this.resourceTable("armor") + " WHERE item_id=?)+(SELECT COUNT(*) FROM " + this.resourceTable("etcitem") + " WHERE item_id=?)) AS template_count";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            boolean bl;
            block12: {
                preparedStatement.setInt(1, n);
                preparedStatement.setInt(2, n);
                preparedStatement.setInt(3, n);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    boolean bl2 = bl = resultSet.next() && resultSet.getInt(1) == 1;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static boolean characterRowExists(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM characters WHERE objid=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static boolean characterItemExists(Connection connection, long l, long l2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM character_items WHERE id=? AND char_id=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l2);
                preparedStatement.setLong(2, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static boolean characterItemIdExists(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM character_items WHERE id=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static boolean litoItemOriginExists(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM lito_item_origin WHERE item_obj_id=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static Set<Long> litoOwnedItemIds(Connection connection, long l) throws SQLException {
        LinkedHashSet<Long> linkedHashSet = new LinkedHashSet<Long>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT item_obj_id FROM lito_item_origin WHERE char_obj_id=?");){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    linkedHashSet.add(resultSet.getLong(1));
                }
            }
        }
        return linkedHashSet;
    }

    private static void upsertLitoOwnedInventoryRow(Connection connection, long l, LitoOwnedInventoryItem litoOwnedInventoryItem) throws SQLException {
        String string = "INSERT INTO character_items (id,item_id,char_id,item_name,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,bless) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE item_id=VALUES(item_id),char_id=VALUES(char_id),item_name=VALUES(item_name),count=VALUES(count),is_equipped=VALUES(is_equipped),is_sealed=VALUES(is_sealed),enchantlvl=VALUES(enchantlvl),is_id=VALUES(is_id),durability=VALUES(durability),charge_count=VALUES(charge_count),bless=VALUES(bless)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            int n = 1;
            preparedStatement.setLong(n++, litoOwnedInventoryItem.objectId());
            preparedStatement.setInt(n++, litoOwnedInventoryItem.itemId());
            preparedStatement.setLong(n++, l);
            preparedStatement.setString(n++, litoOwnedInventoryItem.itemName());
            preparedStatement.setInt(n++, litoOwnedInventoryItem.quantity());
            preparedStatement.setBoolean(n++, litoOwnedInventoryItem.equipped());
            preparedStatement.setBoolean(n++, litoOwnedInventoryItem.sealed());
            preparedStatement.setInt(n++, litoOwnedInventoryItem.enchantLevel());
            preparedStatement.setBoolean(n++, litoOwnedInventoryItem.identified());
            preparedStatement.setInt(n++, litoOwnedInventoryItem.durability());
            preparedStatement.setInt(n++, litoOwnedInventoryItem.chargeCount());
            preparedStatement.setInt(n, litoOwnedInventoryItem.blessing());
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("INSERT INTO lito_item_origin (item_obj_id,char_obj_id,catalog_id,tournament_reward,created_at) VALUES (?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE char_obj_id=VALUES(char_obj_id),catalog_id=VALUES(catalog_id),tournament_reward=VALUES(tournament_reward)");
        try {
            preparedStatement.setLong(1, litoOwnedInventoryItem.objectId());
            preparedStatement.setLong(2, l);
            preparedStatement.setInt(3, litoOwnedInventoryItem.itemId());
            preparedStatement.setBoolean(4, litoOwnedInventoryItem.tournamentReward());
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        if (litoOwnedInventoryItem.expiresAtEpochMillis() == 0L) {
            preparedStatement = connection.prepareStatement("DELETE FROM character_items_time WHERE itemr_obj_id=?");
            try {
                preparedStatement.setLong(1, litoOwnedInventoryItem.objectId());
                preparedStatement.executeUpdate();
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        }
        preparedStatement = connection.prepareStatement("INSERT INTO character_items_time (itemr_obj_id,usertime) VALUES (?,?) ON DUPLICATE KEY UPDATE usertime=VALUES(usertime)");
        try {
            preparedStatement.setLong(1, litoOwnedInventoryItem.objectId());
            preparedStatement.setTimestamp(2, new Timestamp(litoOwnedInventoryItem.expiresAtEpochMillis()));
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void deleteLitoOwnedInventoryRow(Connection connection, long l, long l2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM character_pets WHERE item_obj_id=?");){
            preparedStatement.setLong(1, l2);
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_items_time WHERE itemr_obj_id=?");
        try {
            preparedStatement.setLong(1, l2);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_items WHERE id=? AND char_id=?");
        try {
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM lito_item_origin WHERE item_obj_id=? AND char_obj_id=?");
        try {
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void updateSourceInventoryRow(Connection connection, long l, LitoSourceInventoryMutation litoSourceInventoryMutation) throws SQLException {
        String string = "UPDATE character_items SET item_id=?,count=?,is_equipped=?,is_sealed=?,enchantlvl=?,is_id=?,durability=?,charge_count=?,bless=? WHERE id=? AND char_id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            int n = 1;
            preparedStatement.setInt(n++, litoSourceInventoryMutation.itemId());
            preparedStatement.setInt(n++, litoSourceInventoryMutation.quantity());
            preparedStatement.setBoolean(n++, litoSourceInventoryMutation.equipped());
            preparedStatement.setBoolean(n++, litoSourceInventoryMutation.sealed());
            preparedStatement.setInt(n++, litoSourceInventoryMutation.enchantLevel());
            preparedStatement.setBoolean(n++, litoSourceInventoryMutation.identified());
            preparedStatement.setInt(n++, litoSourceInventoryMutation.durability());
            preparedStatement.setInt(n++, litoSourceInventoryMutation.chargeCount());
            preparedStatement.setInt(n++, litoSourceInventoryMutation.blessing());
            preparedStatement.setLong(n++, litoSourceInventoryMutation.sourceObjectId());
            preparedStatement.setLong(n, l);
            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException("source inventory row disappeared: " + litoSourceInventoryMutation.sourceObjectId());
            }
        }
        if (litoSourceInventoryMutation.expiresAtEpochMillis() == 0L) {
            preparedStatement = connection.prepareStatement("DELETE FROM character_items_time WHERE itemr_obj_id=?");
            try {
                preparedStatement.setLong(1, litoSourceInventoryMutation.sourceObjectId());
                preparedStatement.executeUpdate();
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        }
        preparedStatement = connection.prepareStatement("INSERT INTO character_items_time (itemr_obj_id,usertime) VALUES (?,?) ON DUPLICATE KEY UPDATE usertime=VALUES(usertime)");
        try {
            preparedStatement.setLong(1, litoSourceInventoryMutation.sourceObjectId());
            preparedStatement.setTimestamp(2, new Timestamp(litoSourceInventoryMutation.expiresAtEpochMillis()));
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void deleteSourceInventoryRow(Connection connection, long l, long l2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM character_items_time WHERE itemr_obj_id=?");){
            preparedStatement.setLong(1, l2);
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_items WHERE id=? AND char_id=?");
        try {
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void reconcileSkills(Connection connection, long l, Set<Integer> set) throws SQLException {
        Iterator<Integer> iterator;
        LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT skill_id FROM character_skills WHERE char_obj_id=?");){
            preparedStatement.setLong(1, l);
            iterator = preparedStatement.executeQuery();
            try {
                while (iterator.next()) {
                    linkedHashSet.add(iterator.getInt(1));
                }
            }
            finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=? AND skill_id=?");
        try {
            iterator = linkedHashSet.iterator();
            while (iterator.hasNext()) {
                int n = (Integer)iterator.next();
                if (set.contains(n)) continue;
                preparedStatement.setLong(1, l);
                preparedStatement.setInt(2, n);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("INSERT IGNORE INTO character_skills (char_obj_id,skill_id,skill_name,is_active,activetimeleft) VALUES (?,?,?,0,0)");
        try {
            for (int n : set) {
                if (linkedHashSet.contains(n)) continue;
                preparedStatement.setLong(1, l);
                preparedStatement.setInt(2, n);
                preparedStatement.setString(3, "LiTo skill " + n);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void reconcileQuests(Connection connection, long l, Map<Integer, Integer> map) throws SQLException {
        Iterator<Map.Entry<Integer, Integer>> iterator;
        LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT quest_id FROM character_quests WHERE char_id=?");){
            preparedStatement.setLong(1, l);
            iterator = preparedStatement.executeQuery();
            try {
                while (iterator.next()) {
                    linkedHashSet.add(iterator.getInt(1));
                }
            }
            finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND quest_id=?");
        try {
            iterator = linkedHashSet.iterator();
            while (iterator.hasNext()) {
                int n = (Integer)iterator.next();
                if (map.containsKey(n)) continue;
                preparedStatement.setLong(1, l);
                preparedStatement.setInt(2, n);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("INSERT INTO character_quests (char_id,quest_id,quest_step) VALUES (?,?,?) ON DUPLICATE KEY UPDATE quest_step=VALUES(quest_step)");
        try {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                preparedStatement.setLong(1, l);
                preparedStatement.setInt(2, entry.getKey());
                preparedStatement.setInt(3, entry.getValue());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void setEpochTimestamp(PreparedStatement preparedStatement, int n, long l) throws SQLException {
        if (l <= 0L) {
            preparedStatement.setTimestamp(n, null);
        } else {
            preparedStatement.setTimestamp(n, new Timestamp(l * 1000L));
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public int hellTime(long var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public SourceKillDeathCounts killDeathCounts(long var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public Set<Integer> learnedSkills(long l) throws SQLException {
        if (l <= 0L || l > 0xFFFFFFFFL) {
            return Set.of();
        }
        String string = "SELECT cs.skill_id,s.skill_level FROM character_skills cs LEFT JOIN " + this.resourceTable("skills") + " s ON s.skill_id=cs.skill_id WHERE cs.char_obj_id=? ORDER BY cs.skill_id";
        LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string);){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    int n = resultSet.getInt("skill_id");
                    int n2 = resultSet.getInt("skill_level");
                    Integer n3 = resultSet.wasNull() ? null : Integer.valueOf(n2);
                    if (!NativeSkillMastery.importable(n, n3)) continue;
                    linkedHashSet.add(n);
                }
            }
        }
        return Set.copyOf(linkedHashSet);
    }

    @Override
    public Map<Integer, Integer> questSteps(long l) throws SQLException {
        if (l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        String string = "SELECT quest_id,quest_step FROM character_quests WHERE char_id=? ORDER BY quest_id";
        LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<Integer, Integer>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string);){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    int n = resultSet.getInt("quest_id");
                    int n2 = resultSet.getInt("quest_step");
                    if (n <= 0 || n2 < 0) {
                        throw new SQLException("invalid selected-source quest row for character: " + l);
                    }
                    if (linkedHashMap.putIfAbsent(n, n2) == null) continue;
                    throw new SQLException("duplicate selected-source quest row: " + n);
                }
            }
        }
        return Map.copyOf(linkedHashMap);
    }

    @Override
    public List<SourceInventoryItem> sourceInventory(long l) throws SQLException {
        if (l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            List<SourceInventoryItem> list = this.sourceInventoryRows(connection, this.sourceInventorySql(), l);
            return list;
        }
    }

    @Override
    public RepositoryPrivateInventorySnapshot litoPrivateInventory(long l) throws SQLException {
        LegacyMySqlRepository.requireCharacterObjectId(l, "LiTo-private inventory");
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryPrivateInventorySnapshot.unavailable();
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            if (!LegacyMySqlRepository.characterRowExists(connection, l)) {
                RepositoryPrivateInventorySnapshot repositoryPrivateInventorySnapshot = RepositoryPrivateInventorySnapshot.empty();
                return repositoryPrivateInventorySnapshot;
            }
            RepositoryPrivateInventorySnapshot repositoryPrivateInventorySnapshot = LegacyMySqlRepository.loadPrivateInventorySnapshot(connection, l);
            return repositoryPrivateInventorySnapshot;
        }
    }

    @Override
    public RepositoryBookmarkSnapshot bookmarks(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryBookmarkSnapshot.unavailable();
        }
        LegacyMySqlRepository.requireCharacterObjectId(l, "bookmark character");
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            Map<String, SqlBookmark> map = LegacyMySqlRepository.loadBookmarks(connection, l);
            RepositoryBookmarkSnapshot repositoryBookmarkSnapshot = new RepositoryBookmarkSnapshot(true, map.values().stream().map(SqlBookmark::bookmark).toList());
            return repositoryBookmarkSnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryBookmarkPersistenceResult persistBookmarks(long var1_1, List<RepositoryBookmark> var3_2, List<RepositoryBookmark> var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static void requireCharacterObjectId(long l, String string) {
        if (l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException(string + " object id is outside dword range");
        }
    }

    private static String bookmarkKey(String string) {
        return string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, RepositoryBookmark> normalizeBookmarks(List<RepositoryBookmark> list) {
        List<Object> list2;
        List<Object> list3 = list2 = list == null ? List.of() : list;
        if (list2.size() > 50) {
            throw new IllegalArgumentException("native bookmark snapshot exceeds 50 rows");
        }
        LinkedHashMap<String, RepositoryBookmark> linkedHashMap = new LinkedHashMap<String, RepositoryBookmark>();
        for (RepositoryBookmark repositoryBookmark : list2) {
            if (repositoryBookmark != null && linkedHashMap.putIfAbsent(LegacyMySqlRepository.bookmarkKey(repositoryBookmark.name()), repositoryBookmark) == null) continue;
            throw new IllegalArgumentException("duplicate or null bookmark row");
        }
        return linkedHashMap;
    }

    /*
     * Exception decompiling
     */
    private static Map<String, SqlBookmark> loadBookmarks(Connection var0, long var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public RepositoryBuffSnapshot buffs(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryBuffSnapshot.unavailable();
        }
        LegacyMySqlRepository.requireCharacterObjectId(l, "buff character");
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryBuffSnapshot repositoryBuffSnapshot = LegacyMySqlRepository.loadBuffSnapshot(connection, l);
            return repositoryBuffSnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryBuffPersistence persistBuffs(long var1_1, RepositoryBuffSnapshot var3_2, List<RepositoryBuff> var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static RepositoryBuffSnapshot loadBuffSnapshot(Connection connection, long l) throws SQLException {
        return SourceBuffRead.load(connection, l);
    }

    private static long nextBuffVersion(Connection connection, long l) throws SQLException {
        long l2 = 0L;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT MAX(snapshot_version) FROM character_buff WHERE char_obj_id=?");){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                if (resultSet.next()) {
                    l2 = resultSet.getLong(1);
                }
            }
        }
        long l3 = Math.multiplyExact(System.currentTimeMillis(), 1000L);
        return Math.max(Math.addExact(l2, 1L), l3);
    }

    @Override
    public RepositoryBuddySnapshot buddies(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryBuddySnapshot.unavailable();
        }
        LegacyMySqlRepository.requireCharacterObjectId(l, "buddy character");
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryBuddySnapshot repositoryBuddySnapshot = this.loadBuddySnapshot(connection, l);
            return repositoryBuddySnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryBuddyPersistence persistBuddies(long var1_1, RepositoryBuddySnapshot var3_2, List<RepositoryBuddyGroup> var4_3, List<RepositoryBuddy> var5_4) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    private RepositoryBuddySnapshot loadBuddySnapshot(Connection var1_1, long var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private void replaceBuddyRows(Connection connection, long l, RepositoryBuddySnapshot repositoryBuddySnapshot) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM character_buddys WHERE char_id=?");){
            preparedStatement.setLong(1, l);
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("DELETE FROM lito_buddy_metadata WHERE char_obj_id=?");
        try {
            preparedStatement.setLong(1, l);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("DELETE FROM lito_buddy_group WHERE char_obj_id=?");
        try {
            preparedStatement.setLong(1, l);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("INSERT INTO lito_buddy_group (char_obj_id,group_id,group_name) VALUES (?,?,?)");
        try {
            for (RepositoryBuddyGroup object : repositoryBuddySnapshot.groups()) {
                preparedStatement.setLong(1, l);
                preparedStatement.setInt(2, object.id());
                preparedStatement.setString(3, object.name());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        preparedStatement = connection.prepareStatement("INSERT INTO lito_buddy_metadata (char_obj_id,buddy_id,buddy_name,target_source,group_id,relationship_style,palette_offset,origin) VALUES (?,?,?,?,?,?,?,?)");
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO character_buddys (char_id,buddy_id,buddy_name) VALUES (?,?,?)");
            try {
                for (RepositoryBuddy repositoryBuddy : repositoryBuddySnapshot.buddies()) {
                    long l2 = this.resolveNativeBuddyId(connection, repositoryBuddy);
                    preparedStatement.setLong(1, l);
                    preparedStatement.setLong(2, l2);
                    preparedStatement.setString(3, repositoryBuddy.name());
                    preparedStatement.setString(4, repositoryBuddy.targetSource());
                    preparedStatement.setInt(5, repositoryBuddy.groupId());
                    preparedStatement.setInt(6, repositoryBuddy.relationshipStyle());
                    preparedStatement.setInt(7, repositoryBuddy.paletteOffset());
                    preparedStatement.setString(8, repositoryBuddy.origin());
                    preparedStatement.addBatch();
                    if (l2 == 0L) continue;
                    preparedStatement.setLong(1, l);
                    preparedStatement.setLong(2, l2);
                    preparedStatement.setString(3, repositoryBuddy.name());
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
                preparedStatement.executeBatch();
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private long resolveNativeBuddyId(Connection connection, RepositoryBuddy repositoryBuddy) throws SQLException {
        if (!(repositoryBuddy.targetSource().isBlank() || repositoryBuddy.targetSource().equalsIgnoreCase(this.accountDatabase) || repositoryBuddy.targetSource().equalsIgnoreCase(this.classicSourceName))) {
            return 0L;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT objid FROM characters WHERE char_name=? ORDER BY objid LIMIT 1");){
            long l;
            block13: {
                preparedStatement.setString(1, repositoryBuddy.name());
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    long l2 = l = resultSet.next() ? resultSet.getLong(1) : 0L;
                    if (resultSet == null) break block13;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return l;
        }
    }

    private static String buddyKey(String string) {
        return string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public RepositoryLetterSnapshot letters(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryLetterSnapshot.unavailable();
        }
        LegacyMySqlRepository.requireCharacterObjectId(l, "letter character");
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryLetterSnapshot repositoryLetterSnapshot = this.loadLetterSnapshot(connection, l);
            return repositoryLetterSnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryLetterDelivery deliverLetter(long var1_1, long var3_2, String var5_3, int var6_4, String var7_5, String var8_6, String var9_7) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 1[TRYBLOCK]], but top level block is 134[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryLetterMutation openLetter(long var1_1, RepositoryLetter var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryLetterMutation deleteLetter(long var1_1, RepositoryLetter var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private RepositoryLetterSnapshot loadLetterSnapshot(Connection connection, long l) throws SQLException {
        if (!LegacyMySqlRepository.characterRowExists(connection, l)) {
            return new RepositoryLetterSnapshot(true, List.of());
        }
        ArrayList<RepositoryLetter> arrayList = new ArrayList<RepositoryLetter>();
        String string = "SELECT l.item_object_id,l.code,l.sender,l.receiver,l.date,l.template_id,l.subject,l.content,l.isCheck,i.item_id FROM characters c JOIN character_letter l ON l.receiver=c.char_name LEFT JOIN character_items i ON i.id=l.item_object_id AND i.char_id=c.objid WHERE c.objid=? ORDER BY l.item_object_id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    arrayList.add(LegacyMySqlRepository.letterFromRow(resultSet, l));
                }
            }
        }
        return new RepositoryLetterSnapshot(true, arrayList);
    }

    private RepositoryLetter loadLetter(Connection connection, long l, long l2) throws SQLException {
        String string = "SELECT character_letter.item_object_id,character_letter.code,character_letter.sender,character_letter.receiver,character_letter.date,character_letter.template_id,character_letter.subject,character_letter.content,character_letter.isCheck,character_items.item_id FROM character_letter JOIN character_items ON character_items.id=character_letter.item_object_id WHERE character_items.char_id=? AND character_letter.item_object_id=? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            RepositoryLetter repositoryLetter;
            block12: {
                preparedStatement.setLong(1, l);
                preparedStatement.setLong(2, l2);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    RepositoryLetter repositoryLetter2 = repositoryLetter = resultSet.next() ? LegacyMySqlRepository.letterFromRow(resultSet, l) : null;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return repositoryLetter;
        }
    }

    private static RepositoryLetter letterFromRow(ResultSet resultSet, long l) throws SQLException {
        long l2;
        long l3 = resultSet.getLong("item_object_id");
        long l4 = resultSet.getLong("item_id");
        boolean bl = resultSet.wasNull();
        boolean bl2 = resultSet.getInt("isCheck") != 0;
        long l5 = l2 = bl2 ? 49017L : 49016L;
        if (bl || l4 != l2) {
            throw new SQLException("physical-letter/backing-item mismatch owner=" + l + " object=" + l3);
        }
        try {
            return new RepositoryLetter(l3, resultSet.getInt("code"), resultSet.getString("sender"), resultSet.getString("receiver"), resultSet.getString("date"), resultSet.getInt("template_id"), LegacyMySqlRepository.decodeTerminatedUtf16Le(resultSet.getBytes("subject")), LegacyMySqlRepository.decodeTerminatedUtf16Le(resultSet.getBytes("content")), bl2);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid native physical-letter row object=" + l3, illegalArgumentException);
        }
    }

    private static boolean sameLetterBody(RepositoryLetter repositoryLetter, RepositoryLetter repositoryLetter2) {
        return repositoryLetter != null && repositoryLetter2 != null && repositoryLetter.itemObjectId() == repositoryLetter2.itemObjectId() && repositoryLetter.code() == repositoryLetter2.code() && repositoryLetter.templateId() == repositoryLetter2.templateId() && repositoryLetter.sender().equals(repositoryLetter2.sender()) && repositoryLetter.receiver().equals(repositoryLetter2.receiver()) && repositoryLetter.date().equals(repositoryLetter2.date()) && repositoryLetter.subject().equals(repositoryLetter2.subject()) && repositoryLetter.content().equals(repositoryLetter2.content());
    }

    private static byte[] terminatedUtf16Le(String string) {
        byte[] byArray = (string == null ? "" : string).getBytes(StandardCharsets.UTF_16LE);
        byte[] byArray2 = Arrays.copyOf(byArray, byArray.length + 2);
        return byArray2;
    }

    private static String decodeTerminatedUtf16Le(byte[] byArray) {
        if (byArray == null || (byArray.length & 1) != 0) {
            throw new IllegalArgumentException("invalid terminated UTF-16LE letter field");
        }
        int n = byArray.length;
        int n2 = 0;
        while (n2 + 1 < byArray.length) {
            if (byArray[n2] == 0 && byArray[n2 + 1] == 0) {
                n = n2;
                break;
            }
            n2 += 2;
        }
        if (n == byArray.length) {
            throw new IllegalArgumentException("unterminated UTF-16LE letter field");
        }
        return new String(byArray, 0, n, StandardCharsets.UTF_16LE);
    }

    @Override
    public RepositoryBoardSnapshot boardTopicsBefore(long l, int n) throws SQLException {
        long l2;
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryBoardSnapshot.unavailable();
        }
        long l3 = l2 = l == 0L || l == Integer.MAX_VALUE ? Integer.MAX_VALUE : l;
        if (l2 <= 0L || l2 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid native board cursor");
        }
        int n2 = Math.max(0, Math.min(n, 8));
        ArrayList<GameStateStore.BoardTopic> arrayList = new ArrayList<GameStateStore.BoardTopic>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT id,name,date,title,content FROM `server_\u76df\u5c4b_\u4f48\u544a` WHERE id<? ORDER BY id DESC LIMIT ?");){
            preparedStatement.setLong(1, l2);
            preparedStatement.setInt(2, Math.max(n2, n2 * 8));
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next() && arrayList.size() < n2) {
                    StoredBoardTopic storedBoardTopic = this.boardTopicFromRow(connection, resultSet);
                    if (storedBoardTopic == null) continue;
                    arrayList.add(storedBoardTopic.topic());
                }
            }
        }
        return new RepositoryBoardSnapshot(true, arrayList);
    }

    @Override
    public RepositoryBoardLookup boardTopic(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryBoardLookup.unavailable();
        }
        if (l <= 0L || l >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid native board topic id");
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            StoredBoardTopic storedBoardTopic = this.loadBoardTopic(connection, l);
            RepositoryBoardLookup repositoryBoardLookup = storedBoardTopic == null ? RepositoryBoardLookup.missing() : new RepositoryBoardLookup(true, storedBoardTopic.topic());
            return repositoryBoardLookup;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryBoardMutation postBoardTopic(long var1_1, String var3_2, String var4_3, String var5_4, RuntimeInventory var6_5, RuntimeInventory var7_6, Map<Long, Long> var8_7) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 1[TRYBLOCK]], but top level block is 44[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryBoardMutation deleteBoardTopic(long var1_1, GameStateStore.BoardTopic var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public RepositoryHouseAuctionSnapshot houseAuctions() throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseAuctionSnapshot.unavailable();
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryHouseAuctionSnapshot repositoryHouseAuctionSnapshot = new RepositoryHouseAuctionSnapshot(true, this.loadHouseAuctions(connection));
            return repositoryHouseAuctionSnapshot;
        }
    }

    @Override
    public RepositoryHotelSnapshot hotelState() throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryHotelSnapshot.unavailable();
        }
        LinkedHashMap<Integer, GameStateStore.HotelLeaseState> linkedHashMap = new LinkedHashMap<Integer, GameStateStore.HotelLeaseState>();
        ArrayList<GameStateStore.HotelKeyState> arrayList = new ArrayList<GameStateStore.HotelKeyState>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT item_obj_id,show_id,npc_id,room_number,meeting_hall,renter_identity,expires_at,released FROM lito_hotel_lease_key ORDER BY npc_id,room_number,show_id,item_obj_id");
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                int n = resultSet.getInt("show_id");
                GameStateStore.HotelLeaseState hotelLeaseState = new GameStateStore.HotelLeaseState(n, resultSet.getInt("npc_id"), resultSet.getInt("room_number"), resultSet.getBoolean("meeting_hall"), resultSet.getString("renter_identity"), resultSet.getLong("expires_at"), resultSet.getBoolean("released"));
                GameStateStore.HotelLeaseState hotelLeaseState2 = linkedHashMap.putIfAbsent(n, hotelLeaseState);
                if (hotelLeaseState2 != null && !hotelLeaseState2.equals(hotelLeaseState)) {
                    throw new SQLException("inconsistent hotel lease rows show=" + n);
                }
                arrayList.add(new GameStateStore.HotelKeyState(resultSet.getLong("item_obj_id"), n, hotelLeaseState.npcId(), hotelLeaseState.meetingHall(), hotelLeaseState.expiresAtEpochSeconds()));
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid durable hotel SQL state", illegalArgumentException);
        }
        return new RepositoryHotelSnapshot(true, List.copyOf(linkedHashMap.values()), arrayList);
    }

    @Override
    public RepositoryWaterDragonPassSnapshot waterDragonPassState() throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryWaterDragonPassSnapshot.unavailable();
        }
        ArrayList<GameStateStore.WaterDragonPassState> arrayList = new ArrayList<GameStateStore.WaterDragonPassState>();
        long l = System.currentTimeMillis() / 1000L;
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT item_obj_id,show_id,room_number,renter_identity,expires_at FROM lito_water_dragon_pass WHERE expires_at>? ORDER BY room_number,item_obj_id");){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    arrayList.add(new GameStateStore.WaterDragonPassState(this.classicSourceName, resultSet.getLong("item_obj_id"), resultSet.getInt("show_id"), resultSet.getInt("room_number"), resultSet.getString("renter_identity"), resultSet.getLong("expires_at")));
                }
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid durable Water Dragon SQL state", illegalArgumentException);
        }
        return new RepositoryWaterDragonPassSnapshot(true, arrayList);
    }

    @Override
    public RepositoryHouseTaxQuote houseTaxQuote(long l, int n) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseTaxQuote.unavailable();
        }
        LegacyMySqlRepository.requireCharacterObjectId(l, "house-tax character");
        if (n <= 0) {
            throw new IllegalArgumentException("invalid house-tax keeper");
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryHouseTaxQuote repositoryHouseTaxQuote = this.loadHouseTaxQuote(connection, l, n);
            return repositoryHouseTaxQuote;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryHouseTaxPayment persistHouseTaxPayment(long var1_1, RepositoryHouseTaxQuote var3_2, long var4_3, int var6_4, long var7_5, RuntimeInventory var9_6, RuntimeInventory var10_7, Map<Long, Long> var11_8) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK], 2[TRYBLOCK]], but top level block is 59[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public RepositoryHouseTaxSnapshot expiredHouseTaxes(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryHouseTaxSnapshot.unavailable();
        }
        if (l <= 0L) {
            throw new IllegalArgumentException("invalid house-tax scan time");
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryHouseTaxSnapshot repositoryHouseTaxSnapshot = new RepositoryHouseTaxSnapshot(true, this.loadExpiredHouseTaxes(connection, l));
            return repositoryHouseTaxSnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryHouseTaxMutation persistHouseTaxExpiry(RepositoryHouseTaxState var1_1, long var2_2, int var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK]], but top level block is 56[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryHouseBidMutation persistHouseBid(long var1_1, GameStateStore.HouseAuction var3_2, long var4_3, RuntimeInventory var6_4, RuntimeInventory var7_5, Map<Long, Long> var8_6) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK]], but top level block is 73[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private RepositoryHouseBidMutation houseBid(RepositoryHouseBidStatus repositoryHouseBidStatus, GameStateStore.HouseAuction houseAuction) {
        return new RepositoryHouseBidMutation(repositoryHouseBidStatus, houseAuction, "", 0L, 0L);
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryHouseSettlement persistHouseAuctionSettlement(GameStateStore.HouseAuction var1_1, long var2_2, int var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK]], but top level block is 90[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private RepositoryHouseSettlement houseSettlement(RepositoryHouseSettlementStatus repositoryHouseSettlementStatus, GameStateStore.HouseAuction houseAuction, long l, long l2) {
        return new RepositoryHouseSettlement(repositoryHouseSettlementStatus, houseAuction, l, l2, houseAuction == null ? 0L : houseAuction.bidderObjectId(), houseAuction == null ? "" : houseAuction.bidder());
    }

    private List<RepositoryHouseTaxState> loadExpiredHouseTaxes(Connection connection, long l) throws SQLException {
        ArrayList<RepositoryHouseTaxState> arrayList = new ArrayList<RepositoryHouseTaxState>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT house_id,house_name,house_area,location,is_on_sale,is_purchase_basement,COALESCE(UNIX_TIMESTAMP(tax_deadline),0) tax_deadline_epoch FROM `server_\u76df\u5c4b_\u7d00\u9304` WHERE is_on_sale=0 AND tax_deadline<FROM_UNIXTIME(?) ORDER BY house_id");){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    arrayList.add(this.houseTaxState(resultSet));
                }
            }
        }
        return List.copyOf(arrayList);
    }

    private RepositoryHouseTaxState loadHouseTaxState(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT house_id,house_name,house_area,location,is_on_sale,is_purchase_basement,COALESCE(UNIX_TIMESTAMP(tax_deadline),0) tax_deadline_epoch FROM `server_\u76df\u5c4b_\u7d00\u9304` WHERE house_id=?");){
            RepositoryHouseTaxState repositoryHouseTaxState;
            block16: {
                ResultSet resultSet;
                block14: {
                    RepositoryHouseTaxState repositoryHouseTaxState2;
                    block15: {
                        preparedStatement.setLong(1, l);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block14;
                            repositoryHouseTaxState2 = null;
                            if (resultSet == null) break block15;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return repositoryHouseTaxState2;
                }
                RepositoryHouseTaxState repositoryHouseTaxState3 = this.houseTaxState(resultSet);
                RepositoryHouseTaxState repositoryHouseTaxState4 = repositoryHouseTaxState = resultSet.next() ? null : repositoryHouseTaxState3;
                if (resultSet == null) break block16;
                resultSet.close();
            }
            return repositoryHouseTaxState;
        }
    }

    private RepositoryHouseTaxQuote loadHouseTaxQuote(Connection connection, long l, int n) throws SQLException {
        Object object;
        long l2;
        Object object2;
        RepositoryHouseTaxState repositoryHouseTaxState = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT house_id,house_name,house_area,location,is_on_sale,is_purchase_basement,COALESCE(UNIX_TIMESTAMP(tax_deadline),0) tax_deadline_epoch FROM `server_\u76df\u5c4b_\u7d00\u9304` WHERE keeper_id=? LIMIT 1");){
            preparedStatement.setInt(1, n);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                if (resultSet.next()) {
                    repositoryHouseTaxState = this.houseTaxState(resultSet);
                }
            }
        }
        if (repositoryHouseTaxState == null) {
            return new RepositoryHouseTaxQuote(RepositoryHouseTaxQuoteStatus.NOT_FOUND, null, n, 0L, "", "");
        }
        try (Object object3 = connection.prepareStatement("SELECT ClanID FROM characters WHERE objid=? LIMIT 1");){
            object3.setLong(1, l);
            object2 = object3.executeQuery();
            try {
                if (!object2.next()) {
                    RepositoryHouseTaxQuote repositoryHouseTaxQuote = new RepositoryHouseTaxQuote(RepositoryHouseTaxQuoteStatus.NOT_FOUND, repositoryHouseTaxState, n, 0L, "", "");
                    return repositoryHouseTaxQuote;
                }
                l2 = object2.getLong(1);
            }
            finally {
                if (object2 != null) {
                    object2.close();
                }
            }
        }
        object3 = new ArrayList();
        object2 = connection.prepareStatement("SELECT clan_id,clan_name,leader_name FROM clan_data WHERE hashouse=? ORDER BY clan_id LIMIT 2");
        try {
            object2.setLong(1, repositoryHouseTaxState.houseId());
            object = object2.executeQuery();
            try {
                while (object.next()) {
                    object3.add(new HouseTaxOwner(object.getLong("clan_id"), object.getString("clan_name"), object.getString("leader_name")));
                }
            }
            finally {
                if (object != null) {
                    object.close();
                }
            }
        }
        finally {
            if (object2 != null) {
                object2.close();
            }
        }
        if (object3.size() > 1) {
            return new RepositoryHouseTaxQuote(RepositoryHouseTaxQuoteStatus.OWNER_CONFLICT, repositoryHouseTaxState, n, 0L, "", "");
        }
        if (object3.isEmpty()) {
            return new RepositoryHouseTaxQuote(RepositoryHouseTaxQuoteStatus.VACANT, repositoryHouseTaxState, n, 0L, "", "");
        }
        object2 = (HouseTaxOwner)object3.get(0);
        object = l2 > 0L && l2 == ((HouseTaxOwner)object2).clanId() ? RepositoryHouseTaxQuoteStatus.MEMBER : RepositoryHouseTaxQuoteStatus.OCCUPIED;
        return new RepositoryHouseTaxQuote((RepositoryHouseTaxQuoteStatus)((Object)object), repositoryHouseTaxState, n, ((HouseTaxOwner)object2).clanId(), ((HouseTaxOwner)object2).clanName(), ((HouseTaxOwner)object2).leaderName());
    }

    private static boolean nativeHouseAuctionExists(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM `server_\u76df\u5c4b_\u62cd\u8ce3` WHERE house_id=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private RepositoryHouseTaxState houseTaxState(ResultSet resultSet) throws SQLException {
        return new RepositoryHouseTaxState(this.classicSourceName, resultSet.getLong("house_id"), resultSet.getString("house_name"), resultSet.getInt("house_area"), resultSet.getString("location"), resultSet.getInt("is_on_sale"), resultSet.getInt("is_purchase_basement"), resultSet.getLong("tax_deadline_epoch"));
    }

    private static List<HouseSettlementClan> loadHouseTaxOwners(Connection connection, long l) throws SQLException {
        ArrayList<HouseSettlementClan> arrayList = new ArrayList<HouseSettlementClan>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT clan_id,hashouse FROM clan_data WHERE hashouse=? ORDER BY clan_id");){
            preparedStatement.setLong(1, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    arrayList.add(new HouseSettlementClan(resultSet.getLong("clan_id"), resultSet.getLong("hashouse")));
                }
            }
        }
        return List.copyOf(arrayList);
    }

    private static Long loadHouseAuctionVersion(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT snapshot_version FROM lito_house_auction_metadata WHERE house_id=?");){
            Long l2;
            block17: {
                ResultSet resultSet;
                block15: {
                    Long l3;
                    block16: {
                        preparedStatement.setLong(1, l);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block15;
                            l3 = null;
                            if (resultSet == null) break block16;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return l3;
                }
                long l4 = resultSet.getLong("snapshot_version");
                if (resultSet.next()) {
                    throw new SQLException("duplicate house auction metadata");
                }
                l2 = l4;
                if (resultSet == null) break block17;
                resultSet.close();
            }
            return l2;
        }
    }

    private static void compensateHouseTaxExpiry(Connection connection, RepositoryHouseTaxState repositoryHouseTaxState, HouseSettlementClan houseSettlementClan, Long l, boolean bl, boolean bl2, boolean bl3, boolean bl4, SQLException sQLException) {
        PreparedStatement preparedStatement;
        block33: {
            if (repositoryHouseTaxState == null) {
                return;
            }
            if (bl4 && houseSettlementClan != null) {
                try {
                    LegacyMySqlRepository.updateClanHouse(connection, houseSettlementClan.clanId(), houseSettlementClan.houseId());
                }
                catch (SQLException sQLException2) {
                    sQLException.addSuppressed(sQLException2);
                }
            }
            if (bl3) {
                try {
                    preparedStatement = connection.prepareStatement("UPDATE `server_\u76df\u5c4b_\u7d00\u9304` SET is_on_sale=?,is_purchase_basement=?,tax_deadline=FROM_UNIXTIME(?) WHERE house_id=?");
                    try {
                        preparedStatement.setInt(1, repositoryHouseTaxState.onSale());
                        preparedStatement.setInt(2, repositoryHouseTaxState.purchaseBasement());
                        preparedStatement.setLong(3, repositoryHouseTaxState.taxDeadlineEpochSeconds());
                        preparedStatement.setLong(4, repositoryHouseTaxState.houseId());
                        preparedStatement.executeUpdate();
                    }
                    finally {
                        if (preparedStatement != null) {
                            preparedStatement.close();
                        }
                    }
                }
                catch (SQLException sQLException3) {
                    sQLException.addSuppressed(sQLException3);
                }
            }
            if (bl2) {
                try {
                    if (l == null) {
                        preparedStatement = connection.prepareStatement("DELETE FROM lito_house_auction_metadata WHERE house_id=?");
                        try {
                            preparedStatement.setLong(1, repositoryHouseTaxState.houseId());
                            preparedStatement.executeUpdate();
                            break block33;
                        }
                        finally {
                            if (preparedStatement != null) {
                                preparedStatement.close();
                            }
                        }
                    }
                    LegacyMySqlRepository.writeHouseAuctionVersion(connection, repositoryHouseTaxState.houseId(), l);
                }
                catch (SQLException sQLException4) {
                    sQLException.addSuppressed(sQLException4);
                }
            }
        }
        if (bl) {
            try {
                preparedStatement = connection.prepareStatement("DELETE FROM `server_\u76df\u5c4b_\u62cd\u8ce3` WHERE house_id=?");
                try {
                    preparedStatement.setLong(1, repositoryHouseTaxState.houseId());
                    preparedStatement.executeUpdate();
                }
                finally {
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                }
            }
            catch (SQLException sQLException5) {
                sQLException.addSuppressed(sQLException5);
            }
        }
    }

    private static HouseSettlementClan loadHouseSettlementClan(Connection connection, long l, String string) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT clan_id,hashouse FROM clan_data WHERE leader_id=? AND leader_name=? ORDER BY clan_id");){
            HouseSettlementClan houseSettlementClan;
            block16: {
                ResultSet resultSet;
                block14: {
                    HouseSettlementClan houseSettlementClan2;
                    block15: {
                        preparedStatement.setLong(1, l);
                        preparedStatement.setString(2, string);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block14;
                            houseSettlementClan2 = null;
                            if (resultSet == null) break block15;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return houseSettlementClan2;
                }
                HouseSettlementClan houseSettlementClan3 = new HouseSettlementClan(resultSet.getLong("clan_id"), resultSet.getLong("hashouse"));
                HouseSettlementClan houseSettlementClan4 = houseSettlementClan = resultSet.next() ? null : houseSettlementClan3;
                if (resultSet == null) break block16;
                resultSet.close();
            }
            return houseSettlementClan;
        }
    }

    private static HouseSettlementRecord loadHouseSettlementRecord(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT is_on_sale,is_purchase_basement,DATE_FORMAT(tax_deadline,'%Y-%m-%d %H:%i:%s') tax_deadline FROM `server_\u76df\u5c4b_\u7d00\u9304` WHERE house_id=?");){
            HouseSettlementRecord houseSettlementRecord;
            block16: {
                ResultSet resultSet;
                block14: {
                    HouseSettlementRecord houseSettlementRecord2;
                    block15: {
                        preparedStatement.setLong(1, l);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block14;
                            houseSettlementRecord2 = null;
                            if (resultSet == null) break block15;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return houseSettlementRecord2;
                }
                HouseSettlementRecord houseSettlementRecord3 = new HouseSettlementRecord(resultSet.getInt("is_on_sale"), resultSet.getInt("is_purchase_basement"), resultSet.getString("tax_deadline"));
                HouseSettlementRecord houseSettlementRecord4 = houseSettlementRecord = resultSet.next() ? null : houseSettlementRecord3;
                if (resultSet == null) break block16;
                resultSet.close();
            }
            return houseSettlementRecord;
        }
    }

    private static void updateClanHouse(Connection connection, long l, long l2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE clan_data SET hashouse=? WHERE clan_id=?");){
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l);
            LegacyMySqlRepository.requireOne(preparedStatement, "clan house ownership changed");
        }
    }

    private static void requireOne(PreparedStatement preparedStatement, String string) throws SQLException {
        if (preparedStatement.executeUpdate() != 1) {
            throw new SQLException(string);
        }
    }

    private static void compensateHouseSettlement(Connection connection, GameStateStore.HouseAuction houseAuction, HouseSettlementClan houseSettlementClan, HouseSettlementClan houseSettlementClan2, HouseSettlementRecord houseSettlementRecord, long l, int n, boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5, boolean bl6, boolean bl7, SQLException sQLException) {
        block47: {
            PreparedStatement preparedStatement;
            block45: {
                if (houseAuction == null) {
                    return;
                }
                if (bl6) {
                    try {
                        LegacyMySqlRepository.restoreHouseAuction(connection, houseAuction);
                    }
                    catch (SQLException sQLException2) {
                        sQLException.addSuppressed(sQLException2);
                    }
                }
                if (bl7) {
                    try {
                        if (houseAuction.revision() == 0L) {
                            preparedStatement = connection.prepareStatement("DELETE FROM lito_house_auction_metadata WHERE house_id=?");
                            try {
                                preparedStatement.setLong(1, houseAuction.houseId());
                                preparedStatement.executeUpdate();
                                break block45;
                            }
                            finally {
                                if (preparedStatement != null) {
                                    preparedStatement.close();
                                }
                            }
                        }
                        LegacyMySqlRepository.writeHouseAuctionVersion(connection, houseAuction.houseId(), houseAuction.revision());
                    }
                    catch (SQLException sQLException3) {
                        sQLException.addSuppressed(sQLException3);
                    }
                }
            }
            if (bl5 && houseSettlementRecord != null) {
                try {
                    preparedStatement = connection.prepareStatement("UPDATE `server_\u76df\u5c4b_\u7d00\u9304` SET is_on_sale=?,is_purchase_basement=?,tax_deadline=? WHERE house_id=?");
                    try {
                        preparedStatement.setInt(1, houseSettlementRecord.onSale());
                        preparedStatement.setInt(2, houseSettlementRecord.purchaseBasement());
                        preparedStatement.setString(3, houseSettlementRecord.taxDeadline());
                        preparedStatement.setLong(4, houseAuction.houseId());
                        preparedStatement.executeUpdate();
                    }
                    finally {
                        if (preparedStatement != null) {
                            preparedStatement.close();
                        }
                    }
                }
                catch (SQLException sQLException4) {
                    sQLException.addSuppressed(sQLException4);
                }
            }
            if (bl4 && houseSettlementClan2 != null) {
                try {
                    LegacyMySqlRepository.updateClanHouse(connection, houseSettlementClan2.clanId(), houseSettlementClan2.houseId());
                }
                catch (SQLException sQLException5) {
                    sQLException.addSuppressed(sQLException5);
                }
            }
            if (bl3 && houseSettlementClan != null) {
                try {
                    LegacyMySqlRepository.updateClanHouse(connection, houseSettlementClan.clanId(), houseSettlementClan.houseId());
                }
                catch (SQLException sQLException6) {
                    sQLException.addSuppressed(sQLException6);
                }
            }
            if (bl2) {
                try {
                    if (bl) {
                        preparedStatement = connection.prepareStatement("DELETE FROM character_items WHERE id=? AND char_id=?");
                        try {
                            preparedStatement.setLong(1, l);
                            preparedStatement.setLong(2, houseAuction.oldOwnerObjectId());
                            preparedStatement.executeUpdate();
                            break block47;
                        }
                        finally {
                            if (preparedStatement != null) {
                                preparedStatement.close();
                            }
                        }
                    }
                    preparedStatement = connection.prepareStatement("UPDATE character_items SET count=? WHERE id=? AND char_id=?");
                    try {
                        preparedStatement.setInt(1, n);
                        preparedStatement.setLong(2, l);
                        preparedStatement.setLong(3, houseAuction.oldOwnerObjectId());
                        preparedStatement.executeUpdate();
                    }
                    finally {
                        if (preparedStatement != null) {
                            preparedStatement.close();
                        }
                    }
                }
                catch (SQLException sQLException7) {
                    sQLException.addSuppressed(sQLException7);
                }
            }
        }
    }

    private static void restoreHouseAuction(Connection connection, GameStateStore.HouseAuction houseAuction) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `server_\u76df\u5c4b_\u62cd\u8ce3` (house_id,house_name,house_area,deadline,price,location,old_owner,old_owner_id,bidder,bidder_id) VALUES (?,?,?,FROM_UNIXTIME(?),?,?,?,?,?,?) ON DUPLICATE KEY UPDATE house_name=VALUES(house_name),house_area=VALUES(house_area),deadline=VALUES(deadline),price=VALUES(price),location=VALUES(location),old_owner=VALUES(old_owner),old_owner_id=VALUES(old_owner_id),bidder=VALUES(bidder),bidder_id=VALUES(bidder_id)");){
            preparedStatement.setLong(1, houseAuction.houseId());
            preparedStatement.setString(2, houseAuction.name());
            preparedStatement.setInt(3, houseAuction.area());
            preparedStatement.setLong(4, houseAuction.deadlineEpochSeconds());
            preparedStatement.setLong(5, houseAuction.price());
            preparedStatement.setString(6, houseAuction.location());
            preparedStatement.setString(7, houseAuction.oldOwner());
            preparedStatement.setLong(8, houseAuction.oldOwnerObjectId());
            preparedStatement.setString(9, houseAuction.bidder());
            preparedStatement.setLong(10, houseAuction.bidderObjectId());
            preparedStatement.executeUpdate();
        }
    }

    private List<GameStateStore.HouseAuction> loadHouseAuctions(Connection connection) throws SQLException {
        String string = "SELECT `server_\u76df\u5c4b_\u62cd\u8ce3`.house_id,`server_\u76df\u5c4b_\u62cd\u8ce3`.house_name,`server_\u76df\u5c4b_\u62cd\u8ce3`.house_area,COALESCE(UNIX_TIMESTAMP(`server_\u76df\u5c4b_\u62cd\u8ce3`.deadline),0) deadline_epoch,`server_\u76df\u5c4b_\u62cd\u8ce3`.price,`server_\u76df\u5c4b_\u62cd\u8ce3`.location,`server_\u76df\u5c4b_\u62cd\u8ce3`.old_owner,`server_\u76df\u5c4b_\u62cd\u8ce3`.old_owner_id,`server_\u76df\u5c4b_\u62cd\u8ce3`.bidder,`server_\u76df\u5c4b_\u62cd\u8ce3`.bidder_id,COALESCE(lito_house_auction_metadata.snapshot_version,0) revision FROM `server_\u76df\u5c4b_\u62cd\u8ce3` LEFT JOIN lito_house_auction_metadata ON lito_house_auction_metadata.house_id=`server_\u76df\u5c4b_\u62cd\u8ce3`.house_id ORDER BY `server_\u76df\u5c4b_\u62cd\u8ce3`.house_id";
        ArrayList<GameStateStore.HouseAuction> arrayList = new ArrayList<GameStateStore.HouseAuction>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                arrayList.add(this.houseAuction(resultSet));
            }
        }
        return List.copyOf(arrayList);
    }

    private GameStateStore.HouseAuction loadHouseAuction(Connection connection, long l) throws SQLException {
        return this.loadHouseAuctions(connection).stream().filter(houseAuction -> houseAuction.houseId() == l).findFirst().orElse(null);
    }

    private GameStateStore.HouseAuction houseAuction(ResultSet resultSet) throws SQLException {
        return new GameStateStore.HouseAuction(this.classicSourceName, resultSet.getLong("house_id"), resultSet.getString("house_name"), resultSet.getInt("house_area"), resultSet.getLong("deadline_epoch"), resultSet.getLong("price"), resultSet.getString("location"), resultSet.getString("old_owner"), resultSet.getLong("old_owner_id"), resultSet.getString("bidder"), resultSet.getLong("bidder_id"), resultSet.getLong("revision"));
    }

    private static long[] refundableAdenaObject(Connection connection, long l, long l2) throws SQLException {
        long[] lArray;
        long l3;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT id,count FROM character_items WHERE char_id=? AND item_id=? ORDER BY id");){
            preparedStatement.setLong(1, l);
            preparedStatement.setLong(2, 40308L);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (true) {
                    if (resultSet.next()) {
                        long l4 = resultSet.getLong("count");
                        if (l4 < 0L || l4 + l2 > Integer.MAX_VALUE) continue;
                        long[] lArray2 = new long[]{resultSet.getLong("id"), l4, 0L};
                        return lArray2;
                        continue;
                    }
                    break;
                }
            }
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT GREATEST(COALESCE((SELECT MAX(id) FROM character_items),0),COALESCE((SELECT MAX(id) FROM character_warehouse),0),COALESCE((SELECT MAX(id) FROM clan_warehouse),0),COALESCE((SELECT MAX(id) FROM character_elf_warehouse),0))+1");
             ResultSet resultSet = preparedStatement.executeQuery();){
            if (!resultSet.next()) {
                long[] lArray3 = new long[]{0L, 0L, 0L};
                return lArray3;
            }
            l3 = resultSet.getLong(1);
        }
        if (l3 > 0L && l3 <= Integer.MAX_VALUE) {
            long[] lArray4 = new long[3];
            lArray4[0] = l3;
            lArray4[1] = 0L;
            lArray = lArray4;
            lArray4[2] = 1L;
        } else {
            long[] lArray5 = new long[3];
            lArray5[0] = 0L;
            lArray5[1] = 0L;
            lArray = lArray5;
            lArray5[2] = 0L;
        }
        return lArray;
    }

    private static void insertRawAdena(Connection connection, long l, long l2, int n) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO character_items (id,item_id,char_id,item_name,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,bless) VALUES (?,?,?,?,?,0,0,0,1,0,0,1)");){
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, 40308L);
            preparedStatement.setLong(3, l);
            preparedStatement.setString(4, "Adena");
            preparedStatement.setInt(5, n);
            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException("cannot insert house refund Adena");
            }
        }
    }

    private static void writeHouseAuctionVersion(Connection connection, long l, long l2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_house_auction_metadata (house_id,snapshot_version) VALUES (?,?) ON DUPLICATE KEY UPDATE snapshot_version=VALUES(snapshot_version)");){
            preparedStatement.setLong(1, l);
            preparedStatement.setLong(2, l2);
            preparedStatement.executeUpdate();
        }
    }

    private static void compensateHouseBid(Connection connection, long l, Set<Long> set, Map<Long, RuntimeInventory.Item> map, Map<Long, SqlStoredItem> map2, Map<Long, Boolean> map3, boolean bl, long l2, long l3, int n, boolean bl2, boolean bl3, GameStateStore.HouseAuction houseAuction, boolean bl4, SQLException sQLException) {
        block34: {
            PreparedStatement preparedStatement;
            block33: {
                if (bl4 && houseAuction != null) {
                    try {
                        preparedStatement = connection.prepareStatement("UPDATE `server_\u76df\u5c4b_\u62cd\u8ce3` SET price=?,bidder=?,bidder_id=? WHERE house_id=?");
                        try {
                            preparedStatement.setLong(1, houseAuction.price());
                            preparedStatement.setString(2, houseAuction.bidder());
                            preparedStatement.setLong(3, houseAuction.bidderObjectId());
                            preparedStatement.setLong(4, houseAuction.houseId());
                            preparedStatement.executeUpdate();
                            if (houseAuction.revision() == 0L) {
                                try (PreparedStatement preparedStatement2 = connection.prepareStatement("DELETE FROM lito_house_auction_metadata WHERE house_id=?");){
                                    preparedStatement2.setLong(1, houseAuction.houseId());
                                    preparedStatement2.executeUpdate();
                                    break block33;
                                }
                            }
                            LegacyMySqlRepository.writeHouseAuctionVersion(connection, houseAuction.houseId(), houseAuction.revision());
                        }
                        finally {
                            if (preparedStatement != null) {
                                preparedStatement.close();
                            }
                        }
                    }
                    catch (SQLException sQLException2) {
                        sQLException.addSuppressed(sQLException2);
                    }
                }
            }
            if (bl3) {
                try {
                    if (bl2) {
                        preparedStatement = connection.prepareStatement("DELETE FROM character_items WHERE id=? AND char_id=?");
                        try {
                            preparedStatement.setLong(1, l3);
                            preparedStatement.setLong(2, l2);
                            preparedStatement.executeUpdate();
                            break block34;
                        }
                        finally {
                            if (preparedStatement != null) {
                                preparedStatement.close();
                            }
                        }
                    }
                    preparedStatement = connection.prepareStatement("UPDATE character_items SET count=? WHERE id=? AND char_id=?");
                    try {
                        preparedStatement.setInt(1, n);
                        preparedStatement.setLong(2, l3);
                        preparedStatement.setLong(3, l2);
                        preparedStatement.executeUpdate();
                    }
                    finally {
                        if (preparedStatement != null) {
                            preparedStatement.close();
                        }
                    }
                }
                catch (SQLException sQLException3) {
                    sQLException.addSuppressed(sQLException3);
                }
            }
        }
        if (bl) {
            LegacyMySqlRepository.compensateCharacterInventory(connection, l, set, map, map2, map3, sQLException);
        }
    }

    @Override
    public RepositoryTournamentRoster tournamentProfiles() throws SQLException {
        if (SharedDatabase.registered(this)) {
            return SharedDatabase.profiles(this);
        }
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryTournamentRoster.unavailable();
        }
        ArrayList<RepositoryTournamentProfile> arrayList = new ArrayList<RepositoryTournamentProfile>();
        boolean bl = this.usesSharedTournamentTable();
        String string = bl ? "SELECT c.objid AS char_obj_id,c.char_name,t.source_server_id AS profile_source_id,t.char_obj_id AS profile_obj_id,t.wins,t.losses,t.disconnects,t.ranking_points,t.profile_state,t.snapshot_version FROM characters c LEFT JOIN " + this.tournamentRecordTable() + " t ON t.source_server_id=? AND t.char_obj_id=c.objid AND t.char_name=c.char_name WHERE c.Banned=0 ORDER BY c.objid" : "SELECT c.objid AS char_obj_id,c.char_name,t.char_obj_id AS profile_obj_id,t.profile_state,t.snapshot_version FROM characters c LEFT JOIN lito_tournament_profile t ON t.char_obj_id=c.objid AND t.char_name=c.char_name WHERE c.Banned=0 ORDER BY c.objid";
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string);){
            if (bl) {
                preparedStatement.setInt(1, this.classicSourceId);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    resultSet.getLong("profile_obj_id");
                    arrayList.add(resultSet.wasNull() ? new RepositoryTournamentProfile(resultSet.getLong("char_obj_id"), resultSet.getString("char_name"), 0L, LegacyMySqlRepository.emptyTournamentProfileState(this.classicSourceName)) : (bl ? this.tournamentProfileFromSharedRow(resultSet) : this.tournamentProfileFromRow(resultSet)));
                }
            }
        }
        return new RepositoryTournamentRoster(true, arrayList);
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryTournamentMutation persistTournamentProfile(long var1_1, String var3_2, RepositoryTournamentProfile var4_3, GameStateStore.TournamentProfileState var5_4) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 4 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private RepositoryTournamentProfile loadTournamentProfile(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT char_obj_id,char_name,profile_state,snapshot_version FROM lito_tournament_profile WHERE char_obj_id=? LIMIT 1");){
            RepositoryTournamentProfile repositoryTournamentProfile;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    RepositoryTournamentProfile repositoryTournamentProfile2 = repositoryTournamentProfile = resultSet.next() ? this.tournamentProfileFromRow(resultSet) : null;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return repositoryTournamentProfile;
        }
    }

    private boolean usesSharedTournamentTable() {
        return this.repositoryMode.equals("lito_mysql") && this.classicSourceId > 0;
    }

    private String tournamentRecordTable() {
        return this.resourceTable("lito_tournament_character_record");
    }

    private RepositoryTournamentMutation persistSharedTournamentProfile(long l, String string, RepositoryTournamentProfile repositoryTournamentProfile, GameStateStore.TournamentProfileState tournamentProfileState) throws SQLException {
        try (Connection connection = DriverManager.getConnection(this.accountUrl, this.accountLogin, this.accountPassword);){
            Object object;
            long l2;
            String string2;
            block34: {
                boolean bl;
                connection.setReadOnly(false);
                string2 = LegacyMySqlRepository.characterName(connection, l);
                if (string2 == null || !string2.equals(string)) {
                    RepositoryTournamentMutation repositoryTournamentMutation = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.NOT_FOUND, null);
                    return repositoryTournamentMutation;
                }
                RepositoryTournamentProfile repositoryTournamentProfile2 = this.loadSharedTournamentProfile(connection, l);
                boolean bl2 = bl = repositoryTournamentProfile == null || repositoryTournamentProfile.snapshotVersion() == 0L;
                if (bl ? repositoryTournamentProfile2 != null : !repositoryTournamentProfile.equals(repositoryTournamentProfile2)) {
                    RepositoryTournamentMutation repositoryTournamentMutation = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT, repositoryTournamentProfile2);
                    return repositoryTournamentMutation;
                }
                try {
                    l2 = repositoryTournamentProfile2 == null ? 1L : Math.incrementExact(repositoryTournamentProfile2.snapshotVersion());
                }
                catch (ArithmeticException arithmeticException) {
                    throw new SQLException("Tournament profile version exhausted", arithmeticException);
                }
                if (l2 > 0xFFFFFFFFL) {
                    throw new SQLException("Tournament profile version exhausted");
                }
                String string3 = LegacyMySqlRepository.encodeTournamentProfile(tournamentProfileState);
                if (repositoryTournamentProfile2 == null) {
                    try {
                        object = connection.prepareStatement("INSERT INTO " + this.tournamentRecordTable() + " (source_server_id,char_obj_id,char_name,wins,losses,disconnects,ranking_points,profile_state,snapshot_version,updated_at) VALUES (?,?,?,?,?,?,?,?,?,NOW())");
                        try {
                            this.bindSharedTournamentReplacement((PreparedStatement)object, 1, l, string2, tournamentProfileState, string3, l2);
                            if (object.executeUpdate() != 1) {
                                throw new SQLException("Tournament record insert changed no rows");
                            }
                            break block34;
                        }
                        finally {
                            if (object != null) {
                                object.close();
                            }
                        }
                    }
                    catch (SQLException sQLException) {
                        if (sQLException.getSQLState() == null || !sQLException.getSQLState().startsWith("23")) {
                            throw sQLException;
                        }
                        RepositoryTournamentMutation repositoryTournamentMutation = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT, this.loadSharedTournamentProfile(connection, l));
                        if (connection != null) {
                            connection.close();
                        }
                        return repositoryTournamentMutation;
                    }
                }
                object = connection.prepareStatement("UPDATE " + this.tournamentRecordTable() + " SET char_name=?,wins=?,losses=?,disconnects=?,ranking_points=?,profile_state=?,snapshot_version=?,updated_at=NOW() WHERE source_server_id=? AND char_obj_id=? AND snapshot_version=?");
                try {
                    object.setString(1, string2);
                    object.setInt(2, tournamentProfileState.wins());
                    object.setInt(3, tournamentProfileState.losses());
                    object.setInt(4, tournamentProfileState.disconnects());
                    object.setLong(5, tournamentProfileState.rankingPoints());
                    object.setString(6, string3);
                    object.setLong(7, l2);
                    object.setInt(8, this.classicSourceId);
                    object.setLong(9, l);
                    object.setLong(10, repositoryTournamentProfile2.snapshotVersion());
                    if (object.executeUpdate() != 1) {
                        RepositoryTournamentMutation repositoryTournamentMutation = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT, this.loadSharedTournamentProfile(connection, l));
                        return repositoryTournamentMutation;
                    }
                }
                finally {
                    if (object != null) {
                        object.close();
                    }
                }
            }
            object = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.SUCCESS, new RepositoryTournamentProfile(l, string2, l2, tournamentProfileState));
            return object;
        }
    }

    private void bindSharedTournamentReplacement(PreparedStatement preparedStatement, int n, long l, String string, GameStateStore.TournamentProfileState tournamentProfileState, String string2, long l2) throws SQLException {
        preparedStatement.setInt(n, this.classicSourceId);
        preparedStatement.setLong(n + 1, l);
        preparedStatement.setString(n + 2, string);
        preparedStatement.setInt(n + 3, tournamentProfileState.wins());
        preparedStatement.setInt(n + 4, tournamentProfileState.losses());
        preparedStatement.setInt(n + 5, tournamentProfileState.disconnects());
        preparedStatement.setLong(n + 6, tournamentProfileState.rankingPoints());
        preparedStatement.setString(n + 7, string2);
        preparedStatement.setLong(n + 8, l2);
    }

    private RepositoryTournamentProfile loadSharedTournamentProfile(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT source_server_id AS profile_source_id,char_obj_id,char_name,wins,losses,disconnects,ranking_points,profile_state,snapshot_version FROM " + this.tournamentRecordTable() + " WHERE source_server_id=? AND char_obj_id=? LIMIT 1");){
            RepositoryTournamentProfile repositoryTournamentProfile;
            block12: {
                preparedStatement.setInt(1, this.classicSourceId);
                preparedStatement.setLong(2, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    RepositoryTournamentProfile repositoryTournamentProfile2 = repositoryTournamentProfile = resultSet.next() ? this.tournamentProfileFromSharedRow(resultSet) : null;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return repositoryTournamentProfile;
        }
    }

    private RepositoryTournamentProfile tournamentProfileFromSharedRow(ResultSet resultSet) throws SQLException {
        int n = resultSet.getInt("profile_source_id");
        RepositoryTournamentProfile repositoryTournamentProfile = this.tournamentProfileFromRow(resultSet);
        GameStateStore.TournamentProfileState tournamentProfileState = repositoryTournamentProfile.state();
        if (n != this.classicSourceId || resultSet.getInt("wins") != tournamentProfileState.wins() || resultSet.getInt("losses") != tournamentProfileState.losses() || resultSet.getInt("disconnects") != tournamentProfileState.disconnects() || resultSet.getLong("ranking_points") != tournamentProfileState.rankingPoints()) {
            throw new SQLException("Tournament record columns disagree with profile state source=" + n + " object=" + repositoryTournamentProfile.characterObjectId());
        }
        return repositoryTournamentProfile;
    }

    private RepositoryTournamentProfile tournamentProfileFromRow(ResultSet resultSet) throws SQLException {
        long l = resultSet.getLong("char_obj_id");
        try {
            RepositoryTournamentProfile repositoryTournamentProfile = new RepositoryTournamentProfile(l, resultSet.getString("char_name"), resultSet.getLong("snapshot_version"), LegacyMySqlRepository.decodeTournamentProfile(resultSet.getString("profile_state")));
            if (!repositoryTournamentProfile.state().classicServerName().equalsIgnoreCase(this.classicSourceName) || repositoryTournamentProfile.state().rankingSnapshotPresent() && !repositoryTournamentProfile.state().snapshotClassicServerName().equalsIgnoreCase(this.classicSourceName)) {
                throw new IllegalArgumentException("Tournament profile belongs to another classic source");
            }
            return repositoryTournamentProfile;
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid Tournament profile row object=" + l, illegalArgumentException);
        }
    }

    private static GameStateStore.TournamentProfileState emptyTournamentProfileState(String string) {
        return new GameStateStore.TournamentProfileState(0L, 0, 0, 0, 0, 0L, 0, 0, Collections.nCopies(12, 0), Collections.nCopies(5, 0), false, string, -1, -1, -1, false, 0L, 0, 0, "", -1, -1, -1, false, 0, 0L, 0, 0, 0, 0, 0, 0, 0, 0, 0L, 1, 0L, 0L);
    }

    private static String encodeTournamentProfile(GameStateStore.TournamentProfileState tournamentProfileState) throws SQLException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);){
                dataOutputStream.writeInt(1);
                dataOutputStream.writeLong(tournamentProfileState.rankingPoints());
                dataOutputStream.writeInt(tournamentProfileState.wins());
                dataOutputStream.writeInt(tournamentProfileState.losses());
                dataOutputStream.writeInt(tournamentProfileState.disconnects());
                dataOutputStream.writeInt(tournamentProfileState.monthlyPeriod());
                dataOutputStream.writeLong(tournamentProfileState.monthlyRankingPoints());
                dataOutputStream.writeInt(tournamentProfileState.monthlyWins());
                dataOutputStream.writeInt(tournamentProfileState.monthlyLosses());
                LegacyMySqlRepository.writeIntegerList(dataOutputStream, tournamentProfileState.legacyCertificateCodes());
                LegacyMySqlRepository.writeIntegerList(dataOutputStream, tournamentProfileState.victoryBadgeEditions());
                dataOutputStream.writeBoolean(tournamentProfileState.litoMaster());
                dataOutputStream.writeUTF(tournamentProfileState.classicServerName());
                dataOutputStream.writeInt(tournamentProfileState.channel());
                dataOutputStream.writeInt(tournamentProfileState.sourceLevel());
                dataOutputStream.writeInt(tournamentProfileState.race());
                dataOutputStream.writeBoolean(tournamentProfileState.rankingSnapshotPresent());
                dataOutputStream.writeLong(tournamentProfileState.snapshotRankingPoints());
                dataOutputStream.writeInt(tournamentProfileState.snapshotWins());
                dataOutputStream.writeInt(tournamentProfileState.snapshotLosses());
                dataOutputStream.writeUTF(tournamentProfileState.snapshotClassicServerName());
                dataOutputStream.writeInt(tournamentProfileState.snapshotChannel());
                dataOutputStream.writeInt(tournamentProfileState.snapshotSourceLevel());
                dataOutputStream.writeInt(tournamentProfileState.snapshotRace());
                dataOutputStream.writeBoolean(tournamentProfileState.monthlyRankingSnapshotPresent());
                dataOutputStream.writeInt(tournamentProfileState.snapshotMonthlyPeriod());
                dataOutputStream.writeLong(tournamentProfileState.snapshotMonthlyRankingPoints());
                dataOutputStream.writeInt(tournamentProfileState.snapshotMonthlyWins());
                dataOutputStream.writeInt(tournamentProfileState.snapshotMonthlyLosses());
                dataOutputStream.writeInt(tournamentProfileState.snapshotMonthlyMovement());
                dataOutputStream.writeInt(tournamentProfileState.snapshotOverallMovement());
                dataOutputStream.writeInt(tournamentProfileState.snapshotClassicServerMovement());
                dataOutputStream.writeInt(tournamentProfileState.snapshotChannelMovement());
                dataOutputStream.writeInt(tournamentProfileState.snapshotLevelMovement());
                dataOutputStream.writeInt(tournamentProfileState.snapshotRaceMovement());
                dataOutputStream.writeLong(tournamentProfileState.experience());
                dataOutputStream.writeInt(tournamentProfileState.level());
                dataOutputStream.writeLong(tournamentProfileState.rankingSnapshotBoundaryEpochSeconds());
                dataOutputStream.writeLong(tournamentProfileState.monthlyRankingSnapshotBoundaryEpochSeconds());
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        }
        catch (IOException iOException) {
            throw new SQLException("cannot encode Tournament profile", iOException);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static GameStateStore.TournamentProfileState decodeTournamentProfile(String string) throws SQLException {
        try {
            byte[] byArray = Base64.getDecoder().decode(string == null ? "" : string);
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byArray);){
                GameStateStore.TournamentProfileState tournamentProfileState;
                try (DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);){
                    if (dataInputStream.readInt() != 1) {
                        throw new IOException("unsupported profile format");
                    }
                    GameStateStore.TournamentProfileState tournamentProfileState2 = new GameStateStore.TournamentProfileState(dataInputStream.readLong(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readLong(), dataInputStream.readInt(), dataInputStream.readInt(), LegacyMySqlRepository.readIntegerList(dataInputStream), LegacyMySqlRepository.readIntegerList(dataInputStream), dataInputStream.readBoolean(), dataInputStream.readUTF(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readBoolean(), dataInputStream.readLong(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readUTF(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readBoolean(), dataInputStream.readInt(), dataInputStream.readLong(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readLong(), dataInputStream.readInt(), dataInputStream.readLong(), dataInputStream.readLong());
                    if (byteArrayInputStream.available() != 0) {
                        throw new IOException("trailing profile data");
                    }
                    tournamentProfileState = tournamentProfileState2;
                }
                return tournamentProfileState;
            }
        }
        catch (IOException | IllegalArgumentException exception) {
            throw new SQLException("cannot decode Tournament profile", exception);
        }
    }

    private static void writeIntegerList(DataOutputStream dataOutputStream, List<Integer> list) throws IOException {
        dataOutputStream.writeInt(list.size());
        for (int n : list) {
            dataOutputStream.writeInt(n);
        }
    }

    private static List<Integer> readIntegerList(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        if (n < 0 || n > 64) {
            throw new IOException("invalid integer-list size");
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(dataInputStream.readInt());
        }
        return arrayList;
    }

    private StoredBoardTopic loadBoardTopic(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT id,name,date,title,content FROM `server_\u76df\u5c4b_\u4f48\u544a` WHERE id=? LIMIT 1");){
            StoredBoardTopic storedBoardTopic;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    StoredBoardTopic storedBoardTopic2 = storedBoardTopic = resultSet.next() ? this.boardTopicFromRow(connection, resultSet) : null;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return storedBoardTopic;
        }
    }

    private StoredBoardTopic boardTopicFromRow(Connection connection, ResultSet resultSet) throws SQLException {
        String string = resultSet.getString("name");
        long l = LegacyMySqlRepository.characterObjectId(connection, string);
        try {
            GameStateStore.BoardTopic boardTopic = new GameStateStore.BoardTopic(this.classicSourceName, resultSet.getLong("id"), string, GameStateStore.sourceCharacter(this.classicSourceName, string), resultSet.getString("date"), resultSet.getString("title"), resultSet.getString("content"));
            Protocol.boardRead(boardTopic);
            return new StoredBoardTopic(boardTopic, l);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            System.err.println("[LiTo] skipped unsafe shared native-board row source=" + this.classicSourceName + " id=" + resultSet.getLong("id") + " reason=" + illegalArgumentException.getMessage());
            return null;
        }
    }

    private static long nextBoardTopicId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();){
            long l;
            block14: {
                ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(id),0)+1 AS next_id FROM `server_\u76df\u5c4b_\u4f48\u544a`");
                try {
                    if (!resultSet.next()) {
                        throw new SQLException("cannot allocate native board topic id");
                    }
                    long l2 = resultSet.getLong("next_id");
                    if (l2 <= 0L || l2 >= Integer.MAX_VALUE) {
                        throw new SQLException("native board topic-id range exhausted");
                    }
                    l = l2;
                    if (resultSet == null) break block14;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return l;
        }
    }

    private static long characterObjectId(Connection connection, String string) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT objid FROM characters WHERE char_name=? LIMIT 1");){
            long l;
            block12: {
                preparedStatement.setString(1, string);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    long l2 = l = resultSet.next() ? resultSet.getLong(1) : 0L;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return l;
        }
    }

    private static boolean fitsNativeHouseBoardColumns(String string, String string2, String string3, String string4) {
        return LegacyMySqlRepository.mysqlCharacterLength(string) <= 16 && LegacyMySqlRepository.mysqlCharacterLength(string2) <= 16 && LegacyMySqlRepository.mysqlCharacterLength(string3) <= 16 && LegacyMySqlRepository.mysqlCharacterLength(string4) <= 1000;
    }

    private static int mysqlCharacterLength(String string) {
        return string == null ? Integer.MAX_VALUE : string.codePointCount(0, string.length());
    }

    private static String characterName(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT char_name FROM characters WHERE objid=? LIMIT 1");){
            String string;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    String string2 = string = resultSet.next() ? resultSet.getString(1) : null;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return string;
        }
    }

    private static void insertPhysicalLetterItem(Connection connection, long l, long l2, long l3) throws SQLException {
        String string = "INSERT INTO character_items (id,item_id,char_id,item_name,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,bless) VALUES (?,?,?,?,1,0,0,0,1,0,0,0)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            preparedStatement.setLong(1, l2);
            preparedStatement.setLong(2, l3);
            preparedStatement.setLong(3, l);
            preparedStatement.setString(4, "$1075");
            preparedStatement.executeUpdate();
        }
    }

    private static void compensatePhysicalLetterInsert(Connection connection, long l, long l2, SQLException sQLException) {
        if (l <= 0L) {
            return;
        }
        String[] stringArray = new String[]{"DELETE FROM character_letter WHERE item_object_id=?", "DELETE FROM lito_item_origin WHERE item_obj_id=?", "DELETE FROM character_items WHERE id=? AND char_id=?"};
        for (int i = 0; i < stringArray.length; ++i) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(stringArray[i]);){
                preparedStatement.setLong(1, l);
                if (i == 2) {
                    preparedStatement.setLong(2, l2);
                }
                preparedStatement.executeUpdate();
                continue;
            }
            catch (SQLException sQLException2) {
                sQLException.addSuppressed(sQLException2);
            }
        }
    }

    @Override
    public RepositoryWarehouseSnapshot warehouse(GameStateStore.WarehouseScope warehouseScope, String string) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryWarehouseSnapshot.unavailable();
        }
        if (warehouseScope == null || string == null || string.isBlank()) {
            throw new IllegalArgumentException("warehouse scope/owner is required");
        }
        String string2 = string.trim();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryWarehouseSnapshot repositoryWarehouseSnapshot = new RepositoryWarehouseSnapshot(true, this.sourceInventoryRows(connection, this.warehouseInventorySql(warehouseScope), string2));
            return repositoryWarehouseSnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryWarehousePassword warehousePassword(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryWarehousePasswordChange changeWarehousePassword(String var1_1, int var2_2, int var3_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryWarehouseTransferResult persistWarehouseTransfer(long var1_1, GameStateStore.WarehouseScope var3_2, String var4_3, RuntimeInventory var5_4, RuntimeInventory var6_5, RuntimeInventory var7_6, RuntimeInventory var8_7, Map<Long, Long> var9_8) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [9[TRYBLOCK]], but top level block is 33[WHILELOOP]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static Map<Long, PositionedWarehouseItem> positionedWarehouseItems(RuntimeInventory runtimeInventory, RuntimeInventory runtimeInventory2, Map<Long, Long> map) {
        LinkedHashMap<Long, PositionedWarehouseItem> linkedHashMap = new LinkedHashMap<Long, PositionedWarehouseItem>();
        LegacyMySqlRepository.addPositionedItems(linkedHashMap, runtimeInventory, false, map);
        LegacyMySqlRepository.addPositionedItems(linkedHashMap, runtimeInventory2, true, map);
        return Map.copyOf(linkedHashMap);
    }

    private static void addPositionedItems(Map<Long, PositionedWarehouseItem> map, RuntimeInventory runtimeInventory, boolean bl, Map<Long, Long> map2) {
        for (RuntimeInventory.Item item : runtimeInventory.items()) {
            long l = map2.getOrDefault(item.objectId(), item.objectId());
            if (l > 0L && l <= Integer.MAX_VALUE && map.putIfAbsent(l, new PositionedWarehouseItem(item, bl)) == null) continue;
            throw new IllegalArgumentException("duplicate/out-of-range SQL warehouse object id: " + l);
        }
    }

    private static Map<Long, String> runtimeWarehouseSignature(RuntimeInventory runtimeInventory, Map<Long, Long> map) {
        LinkedHashMap<Long, String> linkedHashMap = new LinkedHashMap<Long, String>();
        for (RuntimeInventory.Item item : runtimeInventory.items()) {
            long l = map.getOrDefault(item.objectId(), item.objectId());
            linkedHashMap.put(l, LegacyMySqlRepository.warehouseSignature(item));
        }
        return linkedHashMap;
    }

    private static Map<Long, String> warehouseSignature(Connection connection, String string, String string2, String string3) throws SQLException {
        String string4 = "SELECT id,item_id,count,is_sealed,enchantlvl,is_id,durability,charge_count,bless FROM " + string + " WHERE " + string2 + "=? ORDER BY id";
        LinkedHashMap<Long, String> linkedHashMap = new LinkedHashMap<Long, String>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(string4);){
            preparedStatement.setString(1, string3);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    long l = resultSet.getLong("id");
                    String string5 = resultSet.getInt("item_id") + ":" + resultSet.getLong("count") + ":" + resultSet.getBoolean("is_sealed") + ":" + resultSet.getInt("enchantlvl") + ":" + resultSet.getBoolean("is_id") + ":" + resultSet.getInt("durability") + ":" + resultSet.getInt("charge_count") + ":" + resultSet.getInt("bless");
                    if (linkedHashMap.putIfAbsent(l, string5) == null) continue;
                    throw new SQLException("duplicate warehouse object id: " + l);
                }
            }
        }
        return linkedHashMap;
    }

    private static String warehouseSignature(RuntimeInventory.Item item) {
        return item.definition().catalogId() + ":" + item.quantity() + ":" + item.sealed() + ":" + item.enchantLevel() + ":" + item.identified() + ":" + item.durability() + ":" + item.chargeCount() + ":" + item.blessing();
    }

    private static SqlStoredItem loadStoredItem(Connection connection, String string, String string2, String string3, long l, long l2, boolean bl) throws SQLException {
        String string4 = bl ? "SELECT item_id,item_name,count,attr_enchant_kind,attr_enchant_level,gamno,greater FROM " + string + " WHERE id=? AND " + string2 + "=? LIMIT 1" : "SELECT item_id,item_name,count,attr_enchant_kind,attr_enchant_level,gamno,greater FROM character_items WHERE id=? AND char_id=? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string4);){
            SqlStoredItem sqlStoredItem;
            block18: {
                ResultSet resultSet;
                block16: {
                    SqlStoredItem sqlStoredItem2;
                    block17: {
                        preparedStatement.setLong(1, l2);
                        if (bl) {
                            preparedStatement.setString(2, string3);
                        } else {
                            preparedStatement.setLong(2, l);
                        }
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block16;
                            sqlStoredItem2 = null;
                            if (resultSet == null) break block17;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return sqlStoredItem2;
                }
                sqlStoredItem = new SqlStoredItem(resultSet.getInt("item_id"), resultSet.getString("item_name"), resultSet.getLong("count"), resultSet.getInt("attr_enchant_kind"), resultSet.getInt("attr_enchant_level"), resultSet.getString("gamno"), resultSet.getInt("greater"));
                if (resultSet == null) break block18;
                resultSet.close();
            }
            return sqlStoredItem;
        }
    }

    private static void deleteStoredItem(Connection connection, String string, String string2, String string3, long l, long l2, Boolean bl) throws SQLException {
        Object object;
        if (bl != null) {
            object = bl != false ? "DELETE FROM " + string + " WHERE id=? AND " + string2 + "=?" : "DELETE FROM character_items WHERE id=? AND char_id=?";
            try (PreparedStatement preparedStatement = connection.prepareStatement((String)object);){
                preparedStatement.setLong(1, l2);
                if (bl.booleanValue()) {
                    preparedStatement.setString(2, string3);
                } else {
                    preparedStatement.setLong(2, l);
                }
                preparedStatement.executeUpdate();
            }
        }
        object = connection.prepareStatement("DELETE FROM character_items_time WHERE itemr_obj_id=?");
        try {
            object.setLong(1, l2);
            object.executeUpdate();
        }
        finally {
            if (object != null) {
                object.close();
            }
        }
    }

    private static void insertCharacterItem(Connection connection, long l, long l2, RuntimeInventory.Item item, SqlStoredItem sqlStoredItem) throws SQLException {
        String string = "INSERT INTO character_items (id,item_id,char_id,item_name,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,remaining_time,last_used,bless,attr_enchant_kind,attr_enchant_level,gamno,greater) VALUES (?,?,?,?,?,?,?,?,?,?,?,0,NULL,?,?,?,?,?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            int n = 1;
            preparedStatement.setLong(n++, l2);
            preparedStatement.setInt(n++, Math.toIntExact(item.definition().catalogId()));
            preparedStatement.setLong(n++, l);
            preparedStatement.setString(n++, LegacyMySqlRepository.storedName(item, sqlStoredItem));
            preparedStatement.setInt(n++, item.quantity());
            preparedStatement.setBoolean(n++, item.equipped());
            preparedStatement.setBoolean(n++, item.sealed());
            preparedStatement.setInt(n++, item.enchantLevel());
            preparedStatement.setBoolean(n++, item.identified());
            preparedStatement.setInt(n++, item.durability());
            preparedStatement.setInt(n++, item.chargeCount());
            preparedStatement.setInt(n++, item.blessing());
            preparedStatement.setInt(n++, sqlStoredItem == null ? 0 : sqlStoredItem.attrEnchantKind());
            preparedStatement.setInt(n++, sqlStoredItem == null ? 0 : sqlStoredItem.attrEnchantLevel());
            preparedStatement.setString(n++, sqlStoredItem == null ? null : sqlStoredItem.gamNo());
            preparedStatement.setInt(n, sqlStoredItem == null ? 0 : sqlStoredItem.greater());
            preparedStatement.executeUpdate();
        }
        if (item.expiresAtEpochMillis() > 0L) {
            preparedStatement = connection.prepareStatement("INSERT INTO character_items_time (itemr_obj_id,usertime) VALUES (?,?)");
            try {
                preparedStatement.setLong(1, l2);
                preparedStatement.setTimestamp(2, new Timestamp(item.expiresAtEpochMillis()));
                preparedStatement.executeUpdate();
            }
            finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        }
    }

    private static void insertWarehouseItem(Connection connection, String string, String string2, String string3, long l, RuntimeInventory.Item item, SqlStoredItem sqlStoredItem) throws SQLException {
        String string4 = "INSERT INTO " + string + " (id," + string2 + ",item_id,item_name,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,remaining_time,last_used,bless,attr_enchant_kind,attr_enchant_level,gamno,greater) VALUES (?,?,?,?,?,0,?,?,?,?,?,?,NULL,?,?,?,?,?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string4);){
            int n = 1;
            preparedStatement.setLong(n++, l);
            preparedStatement.setString(n++, string3);
            preparedStatement.setInt(n++, Math.toIntExact(item.definition().catalogId()));
            preparedStatement.setString(n++, LegacyMySqlRepository.storedName(item, sqlStoredItem));
            preparedStatement.setInt(n++, item.quantity());
            preparedStatement.setBoolean(n++, item.sealed());
            preparedStatement.setInt(n++, item.enchantLevel());
            preparedStatement.setBoolean(n++, item.identified());
            preparedStatement.setInt(n++, item.durability());
            preparedStatement.setInt(n++, item.chargeCount());
            long l2 = item.expiresAtEpochMillis() <= 0L ? 0L : Math.max(1L, (item.expiresAtEpochMillis() - System.currentTimeMillis() + 999L) / 1000L);
            preparedStatement.setInt(n++, (int)Math.min(Integer.MAX_VALUE, l2));
            preparedStatement.setInt(n++, item.blessing());
            preparedStatement.setInt(n++, sqlStoredItem == null ? 0 : sqlStoredItem.attrEnchantKind());
            preparedStatement.setInt(n++, sqlStoredItem == null ? 0 : sqlStoredItem.attrEnchantLevel());
            preparedStatement.setString(n++, sqlStoredItem == null ? null : sqlStoredItem.gamNo());
            preparedStatement.setInt(n, sqlStoredItem == null ? 0 : sqlStoredItem.greater());
            preparedStatement.executeUpdate();
        }
    }

    private static String storedName(RuntimeInventory.Item item, SqlStoredItem sqlStoredItem) {
        String string = sqlStoredItem == null ? null : sqlStoredItem.itemName();
        return string == null || string.isBlank() ? item.definition().name() : string;
    }

    private static boolean litoWarehouseItemOriginExists(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM lito_warehouse_item_origin WHERE item_obj_id=? LIMIT 1");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    bl = resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static void deleteItemOrigins(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM lito_item_origin WHERE item_obj_id=?");){
            preparedStatement.setLong(1, l);
            preparedStatement.executeUpdate();
        }
        preparedStatement = connection.prepareStatement("DELETE FROM lito_warehouse_item_origin WHERE item_obj_id=?");
        try {
            preparedStatement.setLong(1, l);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private static void upsertCharacterItemOrigin(Connection connection, long l, long l2, RuntimeInventory.Item item) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_item_origin (item_obj_id,char_obj_id,catalog_id,tournament_reward,created_at) VALUES (?,?,?,?,NOW())");){
            preparedStatement.setLong(1, l);
            preparedStatement.setLong(2, l2);
            preparedStatement.setInt(3, Math.toIntExact(item.definition().catalogId()));
            preparedStatement.setBoolean(4, item.tournamentReward());
            preparedStatement.executeUpdate();
        }
    }

    private static void upsertWarehouseItemOrigin(Connection connection, long l, GameStateStore.WarehouseScope warehouseScope, String string, RuntimeInventory.Item item) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_warehouse_item_origin (item_obj_id,scope,owner_key,catalog_id,tournament_reward,created_at) VALUES (?,?,?,?,?,NOW())");){
            preparedStatement.setLong(1, l);
            preparedStatement.setString(2, warehouseScope.name());
            preparedStatement.setString(3, string);
            preparedStatement.setInt(4, Math.toIntExact(item.definition().catalogId()));
            preparedStatement.setBoolean(5, item.tournamentReward());
            preparedStatement.executeUpdate();
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private List<SourceInventoryItem> sourceInventoryRows(Connection connection, String string, Object object) throws SQLException {
        ArrayList<SourceInventoryItem> arrayList = new ArrayList<SourceInventoryItem>();
        LinkedHashSet<Long> linkedHashSet = new LinkedHashSet<Long>();
        Map<Integer, SourceMagicDollPower> map = this.sourceMagicDollPowers(connection);
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            for (int i = 1; i <= 3; ++i) {
                preparedStatement.setObject(i, object);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    long l = resultSet.getLong("source_object_id");
                    if (!linkedHashSet.add(l)) {
                        throw new SQLException("selected-source inventory object joins more than one template: " + l);
                    }
                    long l2 = resultSet.getLong("item_count");
                    if (l2 <= 0L || l2 > Integer.MAX_VALUE) {
                        throw new SQLException("selected-source item count is outside native runtime range: " + l);
                    }
                    try {
                        arrayList.add(LegacyMySqlRepository.sourceInventoryItem(resultSet, map, l, (int)l2));
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        throw new SQLException("invalid selected-source inventory/template row: " + l, illegalArgumentException);
                        return List.copyOf(arrayList);
                    }
                }
            }
        }
    }

    private static SourceInventoryItem sourceInventoryItem(ResultSet resultSet, Map<Integer, SourceMagicDollPower> map, long l, int n) throws SQLException {
        Object object;
        int n2 = resultSet.getInt("item_id");
        ClassicWorldDropCatalog.ItemWire itemWire = new ClassicWorldDropCatalog.ItemWire(n2, resultSet.getString("item_kind"), resultSet.getString("name_id"), resultSet.getString("use_token"), resultSet.getInt("invgfx"), resultSet.getInt("grdgfx"), resultSet.getInt("template_bless"), resultSet.getBoolean("stackable"), resultSet.getBoolean("tradable"), resultSet.getInt("unit_weight"), resultSet.getInt("itemdesc_id"), resultSet.getString("weapon_type"), resultSet.getInt("dmg_small"), resultSet.getInt("dmg_large"), resultSet.getString("material"), resultSet.getInt("safenchant"), resultSet.getInt("class_mask"), resultSet.getBoolean("cant_delete"));
        InventoryCatalog.NativeArmorStatus nativeArmorStatus = !"armor".equals(resultSet.getString("item_kind")) ? null : new InventoryCatalog.NativeArmorStatus(resultSet.getInt("armor_ac"), ClassicWorldDropCatalog.materialCode(resultSet.getString("material")), resultSet.getInt("unit_weight"), resultSet.getInt("class_mask"), resultSet.getInt("safenchant"), resultSet.getInt("armor_str"), resultSet.getInt("armor_dex"), resultSet.getInt("armor_con"), resultSet.getInt("armor_wis"), resultSet.getInt("armor_int"), resultSet.getInt("armor_cha"), resultSet.getInt("armor_hp"), resultSet.getInt("armor_mp"), resultSet.getInt("armor_mr"), resultSet.getInt("armor_sp"), resultSet.getBoolean("armor_haste"), resultSet.getInt("armor_fire"), resultSet.getInt("armor_water"), resultSet.getInt("armor_wind"), resultSet.getInt("armor_earth"), resultSet.getInt("armor_blind"), resultSet.getInt("armor_sleep"), resultSet.getInt("armor_freeze"), resultSet.getInt("armor_stone"), resultSet.getInt("armor_sustain"), resultSet.getInt("armor_stun"), ClassicWorldDropCatalog.armorTypeCode(resultSet.getString("use_token")));
        SourceMagicDollType sourceMagicDollType = null;
        if (resultSet.getObject("doll_item_id") != null) {
            object = LegacyMySqlRepository.commaSeparatedPositiveInts(resultSet.getString("doll_powers"), "powers");
            ArrayList<SourceMagicDollPower> arrayList = new ArrayList<SourceMagicDollPower>();
            Iterator iterator = object.iterator();
            while (iterator.hasNext()) {
                int n3 = (Integer)iterator.next();
                SourceMagicDollPower sourceMagicDollPower = map.get(n3);
                if (sourceMagicDollPower == null) {
                    throw new SQLException("magic-doll type references missing etcitem_doll_power_new id: " + n3);
                }
                arrayList.add(sourceMagicDollPower);
            }
            sourceMagicDollType = new SourceMagicDollType(resultSet.getInt("doll_item_id"), resultSet.getInt("doll_type"), resultSet.getInt("doll_icon"), (List<Integer>)object, LegacyMySqlRepository.commaSeparatedPositiveInts(resultSet.getString("doll_material_ids"), "need"), LegacyMySqlRepository.commaSeparatedPositiveInts(resultSet.getString("doll_material_counts"), "count"), resultSet.getInt("doll_duration"), resultSet.getInt("doll_gfx"), resultSet.getString("doll_name"), arrayList);
        }
        object = resultSet.getTimestamp("expires_at");
        return new SourceInventoryItem(l, n2, n, resultSet.getBoolean("is_equipped"), resultSet.getInt("enchantlvl"), resultSet.getInt("durability"), resultSet.getBoolean("is_identified"), resultSet.getInt("instance_bless"), resultSet.getBoolean("linked_pet_collar"), itemWire, nativeArmorStatus, object == null ? 0L : ((Timestamp)object).getTime(), resultSet.getBoolean("instance_sealed"), sourceMagicDollType, resultSet.getBoolean("lito_owned"), resultSet.getBoolean("tournament_reward"), Math.max(0, Math.min(255, resultSet.getInt("charge_count"))));
    }

    /*
     * Exception decompiling
     */
    @Override
    public SourceEquippedFireResistance equippedFireResistance(long var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public SourceBaseMagicResistance baseMagicResistance(long var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public SourceEquipmentMagicStats equipmentMagicStats(long l) throws SQLException {
        return this.equipmentEvaluation(l).magic();
    }

    @Override
    public Set<Integer> counterMagicBypassSkills() throws SQLException {
        LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>();
        String string = "SELECT skill_id FROM " + this.resourceDatabase + ".`skills_\u9b54\u5c4f\u7121\u6548\u6280\u80fd` ORDER BY skill_id";
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                int n = resultSet.getInt("skill_id");
                if (n <= 0) {
                    throw new SQLException("invalid Counter Magic bypass skill id: " + n);
                }
                linkedHashSet.add(n);
            }
        }
        return Set.copyOf(linkedHashSet);
    }

    @Override
    public Map<Integer, Integer> skillReuseDelays() throws SQLException {
        LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<Integer, Integer>();
        String string = "SELECT skill_id,reuseDelay FROM " + this.resourceTable("skills") + " ORDER BY skill_id";
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                int n = resultSet.getInt("skill_id");
                int n2 = resultSet.getInt("reuseDelay");
                if (n > 0 && n2 >= 0 && linkedHashMap.put(n, n2) == null) continue;
                throw new SQLException("invalid or duplicate source skill reuse row: " + n);
            }
        }
        return Map.copyOf(linkedHashMap);
    }

    @Override
    public SourceEquipmentEvaluation equipmentEvaluation(long l) throws SQLException {
        return this.equipmentEvaluation(l, Map.of());
    }

    @Override
    public SourceEquipmentEvaluation equipmentEvaluation(long l, Map<Long, Boolean> map) throws SQLException {
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            SourceEquipmentEvaluation sourceEquipmentEvaluation = LegacyEquipmentMagicLoader.loadEvaluation(connection, this.resourceDatabase, l, System.currentTimeMillis(), map);
            return sourceEquipmentEvaluation;
        }
    }

    @Override
    public SourcePhysicalGrowthStats physicalGrowthStats() throws SQLException {
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            SourcePhysicalGrowthStats sourcePhysicalGrowthStats = new SourcePhysicalGrowthStats(this.physicalGrowthColumn(connection, "\u89d2\u8272_\u529b\u91cf\u6210\u9577", "\u547d\u4e2d"), this.physicalGrowthColumn(connection, "\u89d2\u8272_\u529b\u91cf\u6210\u9577", "\u50b7\u5bb3"), this.physicalGrowthColumn(connection, "\u89d2\u8272_\u654f\u6377\u6210\u9577", "\u547d\u4e2d"), this.physicalGrowthColumn(connection, "\u89d2\u8272_\u654f\u6377\u6210\u9577", "\u50b7\u5bb3"));
            return sourcePhysicalGrowthStats;
        }
    }

    /*
     * Exception decompiling
     */
    public SourceNpcPhysicalStats nativeLegacyNpcPhysicalStats(int var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public SourceSummonTemplate summonTemplate(int var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public List<SourcePet> pets(long l) throws SQLException {
        if (l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        String string = this.repositoryMode.equals("lito_mysql") ? "LEFT JOIN lito_item_origin o ON o.item_obj_id=p.item_obj_id " : "";
        String string2 = this.repositoryMode.equals("lito_mysql") ? "IF(o.item_obj_id IS NULL,0,1) AS lito_owned " : "0 AS lito_owned ";
        String string3 = "SELECT p.item_obj_id,p.objid,p.npcid,p.name,p.lvl,p.hp,p.mp,p.exp,p.lawful,i.item_id," + string2 + "FROM character_pets p JOIN character_items i ON i.id=p.item_obj_id " + string + "WHERE i.char_id=? AND i.item_id IN (?,?) ORDER BY p.item_obj_id";
        ArrayList<SourcePet> arrayList = new ArrayList<SourcePet>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(string3);){
            preparedStatement.setLong(1, l);
            preparedStatement.setLong(2, 40314L);
            preparedStatement.setLong(3, 40316L);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    try {
                        arrayList.add(new SourcePet(resultSet.getLong("item_obj_id"), resultSet.getLong("objid"), resultSet.getInt("item_id"), resultSet.getInt("npcid"), resultSet.getString("name"), resultSet.getInt("lvl"), resultSet.getInt("hp"), resultSet.getInt("mp"), resultSet.getLong("exp"), resultSet.getInt("lawful"), resultSet.getBoolean("lito_owned")));
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        throw new SQLException("invalid selected-source pet row for character: " + l, illegalArgumentException);
                        return List.copyOf(arrayList);
                    }
                }
            }
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public SourcePetTemplate petTemplate(int var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public SourceInventoryWeightStats inventoryWeightStats(long l) throws SQLException {
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            SourceInventoryWeightStats sourceInventoryWeightStats = LegacyInventoryWeightLoader.load(connection, this.resourceDatabase, l, this.serverWeightRate);
            return sourceInventoryWeightStats;
        }
    }

    @Override
    public RepositoryClanSnapshot clan(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryClanSnapshot.unavailable();
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryClanSnapshot repositoryClanSnapshot = LegacyMySqlRepository.loadClanSnapshot(connection, l);
            return repositoryClanSnapshot;
        }
    }

    @Override
    public RepositoryCharacterTitle characterTitle(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryCharacterTitle.unavailable();
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryCharacterTitle repositoryCharacterTitle = LegacyMySqlRepository.loadCharacterTitle(connection, l);
            return repositoryCharacterTitle;
        }
    }

    @Override
    public RepositoryMarriageState marriage(long l) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryMarriageState.unavailable();
        }
        if (!LegacyMySqlRepository.validMarriageObjectId(l)) {
            return RepositoryMarriageState.notFound();
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryMarriageState repositoryMarriageState = LegacyMySqlRepository.loadMarriageState(connection, l);
            return repositoryMarriageState;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryMarriageMutation persistMarriage(long var1_1, long var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryMarriageMutation persistDivorce(long var1_1, long var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryTitleMutation persistCharacterTitle(long var1_1, String var3_2, String var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryClanMutation createClan(long var1_1, String var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 1[TRYBLOCK]], but top level block is 10[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryClanMutation joinClan(long var1_1, String var3_2, int var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryClanMutation changeClanMember(long var1_1, String var3_2, int var4_3, int var5_4) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryClanMutation setClanDestroyPending(long var1_1, boolean var3_2, boolean var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean rollbackCreatedClan(long var1_1, long var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK], 2[TRYBLOCK]], but top level block is 6[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static RepositoryClanMutation clanMutation(RepositoryClanMutationStatus repositoryClanMutationStatus, RepositoryClanSnapshot repositoryClanSnapshot) {
        return new RepositoryClanMutation(repositoryClanMutationStatus, repositoryClanSnapshot == null ? RepositoryClanSnapshot.empty() : repositoryClanSnapshot);
    }

    private static boolean validMarriageObjectId(long l) {
        return l > 0L && l <= Integer.MAX_VALUE;
    }

    private static RepositoryMarriageMutation marriageMutation(RepositoryMarriageMutationStatus repositoryMarriageMutationStatus, RepositoryMarriageState repositoryMarriageState, RepositoryMarriageState repositoryMarriageState2) {
        return new RepositoryMarriageMutation(repositoryMarriageMutationStatus, repositoryMarriageState == null ? RepositoryMarriageState.unavailable() : repositoryMarriageState, repositoryMarriageState2 == null ? RepositoryMarriageState.unavailable() : repositoryMarriageState2);
    }

    private static RepositoryMarriageState loadMarriageState(Connection connection, long l) throws SQLException {
        MarriageCharacterRow marriageCharacterRow = LegacyMySqlRepository.loadMarriageCharacter(connection, l);
        if (marriageCharacterRow == null) {
            return RepositoryMarriageState.notFound();
        }
        if (marriageCharacterRow.partnerObjectId() == 0L) {
            return RepositoryMarriageState.available(marriageCharacterRow.objectId(), marriageCharacterRow.name(), 0L, "");
        }
        if (!LegacyMySqlRepository.validMarriageObjectId(marriageCharacterRow.partnerObjectId())) {
            return RepositoryMarriageState.conflict(marriageCharacterRow.objectId(), marriageCharacterRow.name(), 0L, "");
        }
        MarriageCharacterRow marriageCharacterRow2 = LegacyMySqlRepository.loadMarriageCharacter(connection, marriageCharacterRow.partnerObjectId());
        if (marriageCharacterRow2 == null) {
            return RepositoryMarriageState.conflict(marriageCharacterRow.objectId(), marriageCharacterRow.name(), marriageCharacterRow.partnerObjectId(), "");
        }
        if (marriageCharacterRow2.partnerObjectId() != marriageCharacterRow.objectId()) {
            return RepositoryMarriageState.conflict(marriageCharacterRow.objectId(), marriageCharacterRow.name(), marriageCharacterRow2.objectId(), marriageCharacterRow2.name());
        }
        return RepositoryMarriageState.available(marriageCharacterRow.objectId(), marriageCharacterRow.name(), marriageCharacterRow2.objectId(), marriageCharacterRow2.name());
    }

    private static MarriageCharacterRow loadMarriageCharacter(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT objid,char_name,COALESCE(PartnerID,0) AS partner_id FROM characters WHERE objid=? LIMIT 1");){
            MarriageCharacterRow marriageCharacterRow;
            block17: {
                ResultSet resultSet;
                block15: {
                    MarriageCharacterRow marriageCharacterRow2;
                    block16: {
                        preparedStatement.setLong(1, l);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block15;
                            marriageCharacterRow2 = null;
                            if (resultSet == null) break block16;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return marriageCharacterRow2;
                }
                String string = resultSet.getString("char_name");
                long l2 = resultSet.getLong("objid");
                long l3 = resultSet.getLong("partner_id");
                if (!LegacyMySqlRepository.validMarriageObjectId(l2) || string == null || string.isBlank() || string.trim().length() > 45) {
                    throw new SQLException("invalid selected-source marriage character: " + l);
                }
                marriageCharacterRow = new MarriageCharacterRow(l2, string.trim(), l3);
                if (resultSet == null) break block17;
                resultSet.close();
            }
            return marriageCharacterRow;
        }
    }

    private static int updateMarriagePartner(Connection connection, long l, long l2, long l3) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE characters SET PartnerID=? WHERE objid=? AND PartnerID=?");){
            preparedStatement.setLong(1, l3);
            preparedStatement.setLong(2, l);
            preparedStatement.setLong(3, l2);
            int n = preparedStatement.executeUpdate();
            return n;
        }
    }

    private static void requireMarriageCompensation(Connection connection, long l, long l2, long l3, SQLException sQLException) throws SQLException {
        try {
            if (LegacyMySqlRepository.updateMarriagePartner(connection, l, l2, l3) != 1) {
                throw new SQLException("cannot compensate the first characters.PartnerID row");
            }
        }
        catch (SQLException sQLException2) {
            if (sQLException == null) {
                throw sQLException2;
            }
            sQLException.addSuppressed(sQLException2);
        }
    }

    private static RepositoryCharacterTitle loadCharacterTitle(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COALESCE(Title,'') FROM characters WHERE objid=? LIMIT 1");){
            preparedStatement.setLong(1, l);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                RepositoryCharacterTitle repositoryCharacterTitle = RepositoryCharacterTitle.missing();
                return repositoryCharacterTitle;
            }
            RepositoryCharacterTitle repositoryCharacterTitle = RepositoryCharacterTitle.present(resultSet.getString(1));
            return repositoryCharacterTitle;
            finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
    }

    private static RepositoryClanSnapshot loadClanSnapshot(Connection connection, long l) throws SQLException {
        AutoCloseable autoCloseable;
        long l2;
        String string;
        boolean bl;
        int n;
        int n2;
        int n3;
        String string2;
        long l3;
        String string3;
        AutoCloseable autoCloseable2;
        CharacterClanRow characterClanRow = LegacyMySqlRepository.characterClanRow(connection, l, null);
        if (characterClanRow == null || characterClanRow.clanId() == 0L) {
            return RepositoryClanSnapshot.empty();
        }
        long l4 = characterClanRow.clanId();
        try (Object object = connection.prepareStatement("SELECT clan_name,leader_id,leader_name,hascastle,hashouse,max_online_user,showEmblem,Announcement,COALESCE(UNIX_TIMESTAMP(ClanBirthDay),0) AS birth_epoch FROM clan_data WHERE clan_id=? LIMIT 1");){
            object.setLong(1, l4);
            autoCloseable2 = object.executeQuery();
            try {
                if (!autoCloseable2.next()) {
                    throw new SQLException("character references missing clan_data row: " + l4);
                }
                string3 = autoCloseable2.getString("clan_name");
                l3 = autoCloseable2.getLong("leader_id");
                string2 = autoCloseable2.getString("leader_name");
                n3 = Math.max(0, autoCloseable2.getInt("hascastle"));
                n2 = Math.max(0, autoCloseable2.getInt("hashouse"));
                n = Math.max(0, autoCloseable2.getInt("max_online_user"));
                bl = autoCloseable2.getInt("showEmblem") != 0;
                string = autoCloseable2.getString("Announcement");
                l2 = Math.max(0L, autoCloseable2.getLong("birth_epoch"));
            }
            finally {
                if (autoCloseable2 != null) {
                    autoCloseable2.close();
                }
            }
        }
        object = new ArrayList();
        autoCloseable2 = connection.prepareStatement("SELECT objid,char_name,ClanRank,Title,clanNote FROM characters WHERE ClanID=? ORDER BY objid");
        try {
            autoCloseable2.setLong(1, l4);
            autoCloseable = autoCloseable2.executeQuery();
            try {
                while (autoCloseable.next()) {
                    int n4 = autoCloseable.getInt("ClanRank");
                    long l5 = autoCloseable.getLong("objid");
                    int n5 = l5 == l3 ? 4 : LegacyMySqlRepository.litoClanRole(n4);
                    object.add(new RepositoryClanMember(l5, autoCloseable.getString("char_name"), n5, n4, autoCloseable.getString("Title"), autoCloseable.getString("clanNote")));
                }
            }
            finally {
                if (autoCloseable != null) {
                    autoCloseable.close();
                }
            }
        }
        finally {
            if (autoCloseable2 != null) {
                autoCloseable2.close();
            }
        }
        boolean bl2 = false;
        autoCloseable = connection.prepareStatement("SELECT destroy_pending FROM lito_clan_metadata WHERE clan_id=?");
        try {
            autoCloseable.setLong(1, l4);
            try (ResultSet resultSet = autoCloseable.executeQuery();){
                bl2 = resultSet.next() && resultSet.getInt(1) != 0;
            }
        }
        finally {
            if (autoCloseable != null) {
                autoCloseable.close();
            }
        }
        try {
            return new RepositoryClanSnapshot(true, l4, string3, l3, string2, n3, n2, n, bl, string, l2, bl2, (List<RepositoryClanMember>)object);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid selected-source clan snapshot: " + l4, illegalArgumentException);
        }
    }

    private static CharacterClanRow characterClanRow(Connection connection, long l, String string) throws SQLException {
        String string2 = string == null ? "SELECT objid,char_name,ClanID,ClanRank FROM characters WHERE objid=? LIMIT 1" : "SELECT objid,char_name,ClanID,ClanRank FROM characters WHERE objid=? AND char_name=? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string2);){
            CharacterClanRow characterClanRow;
            block13: {
                preparedStatement.setLong(1, l);
                if (string != null) {
                    preparedStatement.setString(2, string);
                }
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    CharacterClanRow characterClanRow2 = characterClanRow = resultSet.next() ? new CharacterClanRow(resultSet.getLong("objid"), resultSet.getString("char_name"), resultSet.getLong("ClanID"), resultSet.getInt("ClanRank")) : null;
                    if (resultSet == null) break block13;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return characterClanRow;
        }
    }

    private static CharacterClanRow characterClanRow(Connection connection, String string) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT objid,char_name,ClanID,ClanRank FROM characters WHERE char_name=? LIMIT 1");){
            CharacterClanRow characterClanRow;
            block12: {
                preparedStatement.setString(1, string);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    CharacterClanRow characterClanRow2 = characterClanRow = resultSet.next() ? new CharacterClanRow(resultSet.getLong("objid"), resultSet.getString("char_name"), resultSet.getLong("ClanID"), resultSet.getInt("ClanRank")) : null;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return characterClanRow;
        }
    }

    private static int litoClanRole(int n) throws SQLException {
        return switch (n) {
            case 1, 5, 8 -> 3;
            case 2, 7 -> 2;
            case 3, 6, 9 -> 1;
            case 4, 10 -> 4;
            default -> throw new SQLException("invalid native ClanRank: " + n);
        };
    }

    private static int nativeClanRank(int n) {
        return switch (n) {
            case 1 -> 3;
            case 2 -> 2;
            case 3 -> 1;
            default -> throw new IllegalArgumentException("invalid mutable LiTo clan role");
        };
    }

    private static long allocateClanObjectId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();){
            long l;
            block15: {
                if (statement.executeUpdate("UPDATE server_info SET maxid=LAST_INSERT_ID(GREATEST(COALESCE(maxid,0),COALESCE(minid,10000),COALESCE((SELECT MAX(objid) FROM characters),0),COALESCE((SELECT MAX(id) FROM character_items),0),COALESCE((SELECT MAX(objid) FROM character_pets),0),COALESCE((SELECT MAX(clan_id) FROM clan_data),0))+1) WHERE id=1") != 1) {
                    throw new SQLException("server_info allocator row 1 is missing");
                }
                ResultSet resultSet = statement.executeQuery("SELECT LAST_INSERT_ID()");
                try {
                    if (!resultSet.next()) {
                        throw new SQLException("clan object id allocation failed");
                    }
                    long l2 = resultSet.getLong(1);
                    if (l2 <= 0L || l2 > Integer.MAX_VALUE) {
                        throw new SQLException("clan object id exhausted signed native range");
                    }
                    l = l2;
                    if (resultSet == null) break block15;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return l;
        }
    }

    private static boolean litoOwnedClan(Connection connection, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT lito_owned FROM lito_clan_metadata WHERE clan_id=?");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    boolean bl2 = bl = resultSet.next() && resultSet.getInt(1) != 0;
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static void compensateClanCreation(Connection connection, long l, long l2, SQLException sQLException) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE characters SET ClanID=0,Clanname='',ClanRank=0 WHERE objid=? AND ClanID=?");){
            preparedStatement.setLong(1, l);
            preparedStatement.setLong(2, l2);
            preparedStatement.executeUpdate();
        }
        catch (SQLException sQLException2) {
            sQLException.addSuppressed(sQLException2);
        }
        for (String string : List.of("DELETE FROM lito_clan_metadata WHERE clan_id=?", "DELETE FROM clan_data WHERE clan_id=?")) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(string);
                try {
                    preparedStatement.setLong(1, l2);
                    preparedStatement.executeUpdate();
                }
                finally {
                    if (preparedStatement == null) continue;
                    preparedStatement.close();
                }
            }
            catch (SQLException sQLException3) {
                sQLException.addSuppressed(sQLException3);
            }
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public ClanEmblemData clanEmblem(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public CastleSummary castleForClan(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public CastleSummary castleById(int var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public List<CastleWarSchedule> castleWarSchedules() throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public RepositoryCastleSnapshot castleState(int n) throws SQLException {
        if (!this.repositoryMode.equals("lito_mysql")) {
            return RepositoryCastleSnapshot.unavailable();
        }
        if (n < 1 || n > 8) {
            throw new IllegalArgumentException("castle id must be 1..8");
        }
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);){
            RepositoryCastleSnapshot repositoryCastleSnapshot = LegacyMySqlRepository.loadCastleSnapshot(connection, n);
            return repositoryCastleSnapshot;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCastleMutation persistCastleState(long var1_1, RepositoryCastleSnapshot var3_2, RepositoryCastleSnapshot var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 1[TRYBLOCK]], but top level block is 48[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCastleMutation persistCastleWarEnd(RepositoryCastleSnapshot var1_1, RepositoryCastleSnapshot var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK]], but top level block is 42[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCastleSecuritySettlement prepareCastleSecurity(int var1_1, long var2_2, long var4_3) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK], 2[TRYBLOCK]], but top level block is 77[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCastleSecuritySettlement completeCastleSecurity(int var1_1, long var2_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCastleOwnershipStatus persistCastleOwnership(long var1_1, int var3_2) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 1[TRYBLOCK]], but top level block is 13[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static void clearCastleOwner(Connection connection, long l, int n, SQLException sQLException) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE clan_data SET hascastle=0 WHERE clan_id=? AND hascastle=?");){
            preparedStatement.setLong(1, l);
            preparedStatement.setInt(2, n);
            preparedStatement.executeUpdate();
        }
        catch (SQLException sQLException2) {
            if (sQLException != null) {
                sQLException.addSuppressed(sQLException2);
            }
            throw sQLException2;
        }
    }

    private static void restoreCastleOwner(Connection connection, long l, int n, SQLException sQLException) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE clan_data SET hascastle=? WHERE clan_id=? AND hascastle=0");){
            preparedStatement.setInt(1, n);
            preparedStatement.setLong(2, l);
            preparedStatement.executeUpdate();
        }
        catch (SQLException sQLException2) {
            if (sQLException != null) {
                sQLException.addSuppressed(sQLException2);
            }
            throw sQLException2;
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public RepositoryPendingRewardSnapshot pendingRewards(String string) throws SQLException {
        String string2;
        String string3 = string2 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
        if (string2.isBlank()) {
            throw new IllegalArgumentException("pending reward account is required");
        }
        ArrayList<RepositoryPendingReward> arrayList = new ArrayList<RepositoryPendingReward>();
        try (Connection connection = this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `\u6d41\u6c34\u865f`,`\u73a9\u5bb6\u5e33\u865f`,`\u767c\u9001\u7269\u54c1\u7de8\u865f`,`\u767c\u9001\u7269\u54c1\u6578\u91cf`,`\u5f37\u5316\u503c` FROM `server_\u7d00\u9304_\u734e\u52f5` WHERE `\u73a9\u5bb6\u5e33\u865f`=? AND `\u9818\u53d6\u72c0\u614b`=0 ORDER BY `\u6d41\u6c34\u865f`");){
            preparedStatement.setString(1, string2);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                while (resultSet.next()) {
                    int n = resultSet.getInt("\u767c\u9001\u7269\u54c1\u7de8\u865f");
                    InventoryCatalog.Definition definition = this.rewardItemDefinition(connection, n);
                    if (definition == null) {
                        throw new SQLException("pending reward references missing/ambiguous static item template: sequence=" + resultSet.getLong("\u6d41\u6c34\u865f") + " item=" + n);
                    }
                    try {
                        arrayList.add(new RepositoryPendingReward(resultSet.getLong("\u6d41\u6c34\u865f"), resultSet.getString("\u73a9\u5bb6\u5e33\u865f"), definition, resultSet.getInt("\u767c\u9001\u7269\u54c1\u6578\u91cf"), resultSet.getInt("\u5f37\u5316\u503c")));
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        throw new SQLException("invalid pending reward row: sequence=" + resultSet.getLong("\u6d41\u6c34\u865f"), illegalArgumentException);
                        return new RepositoryPendingRewardSnapshot(true, arrayList);
                    }
                }
            }
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryPendingRewardClaim claimPendingReward(long var1_1, String var3_2, RepositoryPendingReward var4_3, String var5_4, String var6_5, RuntimeInventory var7_6, RuntimeInventory var8_7, RuntimeInventory var9_8, RuntimeInventory var10_9, Map<Long, Long> var11_10) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK], 3[TRYBLOCK]], but top level block is 53[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Exception decompiling
     */
    @Override
    public RepositoryCastleTreasuryMutation persistCastleTreasuryTransfer(long var1_1, RepositoryCastleSnapshot var3_2, RepositoryCastleSnapshot var4_3, RuntimeInventory var5_4, RuntimeInventory var6_5, Map<Long, Long> var7_6) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [4[TRYBLOCK], 3[TRYBLOCK]], but top level block is 57[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static Map<Long, RuntimeInventory.Item> positionedInventoryItems(RuntimeInventory runtimeInventory, Map<Long, Long> map) {
        LinkedHashMap<Long, RuntimeInventory.Item> linkedHashMap = new LinkedHashMap<Long, RuntimeInventory.Item>();
        for (RuntimeInventory.Item item : runtimeInventory.items()) {
            long l = map.getOrDefault(item.objectId(), item.objectId());
            if (l > 0L && l <= Integer.MAX_VALUE && linkedHashMap.putIfAbsent(l, item) == null) continue;
            throw new IllegalArgumentException("duplicate/out-of-range SQL inventory object id: " + l);
        }
        return Map.copyOf(linkedHashMap);
    }

    private static Boolean validatePendingRewardDelta(RepositoryPendingReward repositoryPendingReward, Map<Long, PositionedWarehouseItem> map, Map<Long, PositionedWarehouseItem> map2, Set<Long> set) {
        if (set.isEmpty()) {
            return null;
        }
        Boolean bl = null;
        long l = 0L;
        for (long l2 : set) {
            PositionedWarehouseItem positionedWarehouseItem = map.get(l2);
            PositionedWarehouseItem positionedWarehouseItem2 = map2.get(l2);
            if (positionedWarehouseItem2 == null || positionedWarehouseItem != null && positionedWarehouseItem.warehouse() != positionedWarehouseItem2.warehouse()) {
                return null;
            }
            if (bl == null) {
                bl = positionedWarehouseItem2.warehouse();
            } else if (bl.booleanValue() != positionedWarehouseItem2.warehouse()) {
                return null;
            }
            RuntimeInventory.Item item = positionedWarehouseItem2.item();
            if (item.definition().catalogId() != (long)repositoryPendingReward.itemId() || item.enchantLevel() != repositoryPendingReward.enchant() || item.equipped() || item.durability() != 0 || item.tournamentReward() || item.sealed()) {
                return null;
            }
            int n = 0;
            if (positionedWarehouseItem != null) {
                RuntimeInventory.Item item2 = positionedWarehouseItem.item();
                if (item2.definition().catalogId() != (long)repositoryPendingReward.itemId() || item2.enchantLevel() != repositoryPendingReward.enchant() || item2.equipped() != item.equipped() || item2.durability() != item.durability() || item2.identified() != item.identified() || item2.blessing() != item.blessing() || item2.sourceBound() != item.sourceBound() || item2.tournamentReward() != item.tournamentReward() || item2.chargeCount() != item.chargeCount() || item2.expiresAtEpochMillis() != item.expiresAtEpochMillis() || item2.sealed() != item.sealed() || item2.litoOwned() != item.litoOwned() || !item2.definition().equals(item.definition())) {
                    return null;
                }
                n = item2.quantity();
            } else if (item.sourceBound() || item.litoOwned()) {
                return null;
            }
            int n2 = item.quantity() - n;
            if (n2 <= 0) {
                return null;
            }
            try {
                l = Math.addExact(l, (long)n2);
            }
            catch (ArithmeticException arithmeticException) {
                return null;
            }
        }
        return l == (long)repositoryPendingReward.count() ? bl : null;
    }

    private static boolean characterIdentityMatches(Connection connection, long l, String string, String string2) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT account_name,char_name FROM characters WHERE objid=?");){
            boolean bl;
            block12: {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    boolean bl2 = bl = resultSet.next() && string.equalsIgnoreCase(resultSet.getString("account_name")) && string2.equals(resultSet.getString("char_name")) && !resultSet.next();
                    if (resultSet == null) break block12;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl;
        }
    }

    private static RepositoryPendingReward loadPendingRewardLocked(Connection connection, long l, InventoryCatalog.Definition definition) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT `\u73a9\u5bb6\u5e33\u865f`,`\u767c\u9001\u7269\u54c1\u7de8\u865f`,`\u767c\u9001\u7269\u54c1\u6578\u91cf`,`\u5f37\u5316\u503c` FROM `server_\u7d00\u9304_\u734e\u52f5` WHERE `\u6d41\u6c34\u865f`=? AND `\u9818\u53d6\u72c0\u614b`=0");){
            RepositoryPendingReward repositoryPendingReward;
            block21: {
                ResultSet resultSet;
                block19: {
                    RepositoryPendingReward repositoryPendingReward2;
                    block20: {
                        block17: {
                            RepositoryPendingReward repositoryPendingReward3;
                            block18: {
                                preparedStatement.setLong(1, l);
                                resultSet = preparedStatement.executeQuery();
                                try {
                                    if (resultSet.next()) break block17;
                                    repositoryPendingReward3 = null;
                                    if (resultSet == null) break block18;
                                }
                                catch (Throwable throwable) {
                                    if (resultSet != null) {
                                        try {
                                            resultSet.close();
                                        }
                                        catch (Throwable throwable2) {
                                            throwable.addSuppressed(throwable2);
                                        }
                                    }
                                    throw throwable;
                                }
                                resultSet.close();
                            }
                            return repositoryPendingReward3;
                        }
                        if ((long)resultSet.getInt("\u767c\u9001\u7269\u54c1\u7de8\u865f") == definition.catalogId()) break block19;
                        repositoryPendingReward2 = null;
                        if (resultSet == null) break block20;
                        resultSet.close();
                    }
                    return repositoryPendingReward2;
                }
                RepositoryPendingReward repositoryPendingReward4 = new RepositoryPendingReward(l, resultSet.getString("\u73a9\u5bb6\u5e33\u865f"), definition, resultSet.getInt("\u767c\u9001\u7269\u54c1\u6578\u91cf"), resultSet.getInt("\u5f37\u5316\u503c"));
                if (resultSet.next()) {
                    throw new SQLException("duplicate pending reward sequence: " + l);
                }
                repositoryPendingReward = repositoryPendingReward4;
                if (resultSet == null) break block21;
                resultSet.close();
            }
            return repositoryPendingReward;
        }
    }

    private static boolean storedItemStateMatches(Connection connection, String string, String string2, String string3, long l, long l2, PositionedWarehouseItem positionedWarehouseItem) throws SQLException {
        boolean bl = positionedWarehouseItem.warehouse();
        String string4 = bl ? "SELECT item_id,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,bless FROM " + string + " WHERE id=? AND " + string2 + "=?" : "SELECT item_id,count,is_equipped,is_sealed,enchantlvl,is_id,durability,charge_count,bless FROM character_items WHERE id=? AND char_id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string4);){
            boolean bl2;
            block14: {
                preparedStatement.setLong(1, l2);
                if (bl) {
                    preparedStatement.setString(2, string3);
                } else {
                    preparedStatement.setLong(2, l);
                }
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    RuntimeInventory.Item item = positionedWarehouseItem.item();
                    boolean bl3 = bl2 = resultSet.next() && (long)resultSet.getInt("item_id") == item.definition().catalogId() && resultSet.getLong("count") == (long)item.quantity() && resultSet.getBoolean("is_equipped") == item.equipped() && resultSet.getBoolean("is_sealed") == item.sealed() && resultSet.getInt("enchantlvl") == item.enchantLevel() && resultSet.getBoolean("is_id") == item.identified() && resultSet.getInt("durability") == item.durability() && resultSet.getInt("charge_count") == item.chargeCount() && resultSet.getInt("bless") == item.blessing() && !resultSet.next();
                    if (resultSet == null) break block14;
                }
                catch (Throwable throwable) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                resultSet.close();
            }
            return bl2;
        }
    }

    private static void compensatePendingRewardInventory(Connection connection, long l, String string, Set<Long> set, Map<Long, PositionedWarehouseItem> map, Map<Long, SqlStoredItem> map2, Map<Long, Boolean> map3, SQLException sQLException) {
        for (long l2 : set) {
            try {
                LegacyMySqlRepository.deleteStoredItem(connection, "character_warehouse", "account_name", string, l, l2, null);
                try (Object object = connection.prepareStatement("DELETE FROM character_items WHERE id=? AND char_id=?");){
                    object.setLong(1, l2);
                    object.setLong(2, l);
                    object.executeUpdate();
                }
                object = connection.prepareStatement("DELETE FROM character_warehouse WHERE id=? AND account_name=?");
                try {
                    object.setLong(1, l2);
                    object.setString(2, string);
                    object.executeUpdate();
                }
                finally {
                    if (object != null) {
                        object.close();
                    }
                }
                LegacyMySqlRepository.deleteItemOrigins(connection, l2);
                object = map.get(l2);
                if (object == null) continue;
                if (((PositionedWarehouseItem)object).warehouse()) {
                    LegacyMySqlRepository.insertWarehouseItem(connection, "character_warehouse", "account_name", string, l2, ((PositionedWarehouseItem)object).item(), map2.get(l2));
                    if (!map3.getOrDefault(l2, false).booleanValue()) continue;
                    LegacyMySqlRepository.upsertWarehouseItemOrigin(connection, l2, GameStateStore.WarehouseScope.PERSONAL, string, ((PositionedWarehouseItem)object).item());
                    continue;
                }
                LegacyMySqlRepository.insertCharacterItem(connection, l, l2, ((PositionedWarehouseItem)object).item(), map2.get(l2));
                if (!map3.getOrDefault(l2, false).booleanValue()) continue;
                LegacyMySqlRepository.upsertCharacterItemOrigin(connection, l2, l, ((PositionedWarehouseItem)object).item());
            }
            catch (SQLException sQLException2) {
                sQLException.addSuppressed(sQLException2);
            }
        }
    }

    private static Set<Long> changedInventoryObjectIds(Map<Long, RuntimeInventory.Item> map, Map<Long, RuntimeInventory.Item> map2) {
        LinkedHashSet<Long> linkedHashSet = new LinkedHashSet<Long>();
        linkedHashSet.addAll(map.keySet());
        linkedHashSet.addAll(map2.keySet());
        linkedHashSet.removeIf(l -> Objects.equals(map.get(l), map2.get(l)));
        return Set.copyOf(linkedHashSet);
    }

    private static boolean sqlInventoryObjectIdAvailable(Connection connection, long l) throws SQLException {
        for (String object : List.of("character_items", "character_warehouse", "clan_warehouse", "character_elf_warehouse")) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM " + object + " WHERE id=? LIMIT 1");
            try {
                preparedStatement.setLong(1, l);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    if (!resultSet.next()) continue;
                    boolean bl = false;
                    return bl;
                }
                finally {
                    if (resultSet == null) continue;
                    resultSet.close();
                }
            }
            finally {
                if (preparedStatement == null) continue;
                preparedStatement.close();
            }
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM character_pets WHERE objid=? OR item_obj_id=? LIMIT 1");){
            preparedStatement.setLong(1, l);
            preparedStatement.setLong(2, l);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                if (resultSet.next()) {
                    boolean bl = false;
                    return bl;
                }
            }
        }
        return true;
    }

    private static void writeCastleMetadata(Connection connection, RepositoryCastleSnapshot repositoryCastleSnapshot, long l) throws SQLException {
        GameStateStore.MercenaryRoster mercenaryRoster = repositoryCastleSnapshot.mercenaries();
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_castle_metadata (castle_id,security_enabled,mercenary_0,mercenary_1,mercenary_2,mercenary_3,snapshot_version) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE security_enabled=VALUES(security_enabled),mercenary_0=VALUES(mercenary_0),mercenary_1=VALUES(mercenary_1),mercenary_2=VALUES(mercenary_2),mercenary_3=VALUES(mercenary_3),snapshot_version=VALUES(snapshot_version)");){
            preparedStatement.setInt(1, repositoryCastleSnapshot.castleId());
            preparedStatement.setBoolean(2, repositoryCastleSnapshot.securityEnabled());
            preparedStatement.setInt(3, mercenaryRoster.type0());
            preparedStatement.setInt(4, mercenaryRoster.type1());
            preparedStatement.setInt(5, mercenaryRoster.type2());
            preparedStatement.setInt(6, mercenaryRoster.type3());
            preparedStatement.setLong(7, l);
            preparedStatement.executeUpdate();
        }
    }

    private static void compensateCharacterInventory(Connection connection, long l, Set<Long> set, Map<Long, RuntimeInventory.Item> map, Map<Long, SqlStoredItem> map2, Map<Long, Boolean> map3, SQLException sQLException) {
        for (long l2 : set) {
            try {
                LegacyMySqlRepository.deleteStoredItem(connection, "", "", "", l, l2, false);
                LegacyMySqlRepository.deleteItemOrigins(connection, l2);
                RuntimeInventory.Item item = map.get(l2);
                if (item == null) continue;
                LegacyMySqlRepository.insertCharacterItem(connection, l, l2, item, map2.get(l2));
                if (!map3.getOrDefault(l2, false).booleanValue()) continue;
                LegacyMySqlRepository.upsertCharacterItemOrigin(connection, l2, l, item);
            }
            catch (SQLException sQLException2) {
                sQLException.addSuppressed(sQLException2);
            }
        }
    }

    /*
     * Exception decompiling
     */
    private static RepositoryCastleSnapshot loadCastleSnapshot(Connection var0, int var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK]], but top level block is 4[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static SqlCastleSecuritySettlement loadCastleSecuritySettlement(Connection connection, int n, long l) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT treasury_before,charge_adena,charged_adena,prepared,completed FROM lito_castle_security_settlement WHERE castle_id=? AND war_start_millis=? LIMIT 1");){
            SqlCastleSecuritySettlement sqlCastleSecuritySettlement;
            block16: {
                ResultSet resultSet;
                block14: {
                    SqlCastleSecuritySettlement sqlCastleSecuritySettlement2;
                    block15: {
                        preparedStatement.setInt(1, n);
                        preparedStatement.setLong(2, l);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block14;
                            sqlCastleSecuritySettlement2 = null;
                            if (resultSet == null) break block15;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return sqlCastleSecuritySettlement2;
                }
                sqlCastleSecuritySettlement = new SqlCastleSecuritySettlement(resultSet.getLong("treasury_before"), resultSet.getLong("charge_adena"), resultSet.getLong("charged_adena"), resultSet.getInt("prepared") != 0, resultSet.getInt("completed") != 0);
                if (resultSet == null) break block16;
                resultSet.close();
            }
            return sqlCastleSecuritySettlement;
        }
    }

    private static RepositoryCastleSecuritySettlement castleSecuritySettlement(RepositoryCastleSecuritySettlementStatus repositoryCastleSecuritySettlementStatus, RepositoryCastleSnapshot repositoryCastleSnapshot, long l, long l2) {
        return new RepositoryCastleSecuritySettlement(repositoryCastleSecuritySettlementStatus, repositoryCastleSnapshot == null ? RepositoryCastleSnapshot.unavailable() : repositoryCastleSnapshot, l, l2);
    }

    private static CastleWarRewardTarget loadCastleWarRewardTarget(Connection connection, int n, long l) throws SQLException {
        long l2;
        Object object;
        int n2;
        int n3;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT item_id,count FROM `server_\u57ce\u5821` WHERE castle_id=?");){
            preparedStatement.setInt(1, n);
            try (ResultSet resultSet = preparedStatement.executeQuery();){
                if (!resultSet.next()) {
                    CastleWarRewardTarget castleWarRewardTarget = null;
                    return castleWarRewardTarget;
                }
                n3 = resultSet.getInt("item_id");
                n2 = resultSet.getInt("count");
            }
        }
        if (n3 <= 0 || n2 <= 0) {
            return null;
        }
        try (Object object2 = connection.prepareStatement("SELECT clan_id,leader_id FROM clan_data WHERE hascastle=? ORDER BY clan_id");){
            object2.setInt(1, n);
            object = object2.executeQuery();
            try {
                if (!object.next()) {
                    CastleWarRewardTarget castleWarRewardTarget = null;
                    return castleWarRewardTarget;
                }
                l2 = object.getLong("leader_id");
                if (object.next()) {
                    throw new SQLException("multiple clans own castle " + n);
                }
            }
            finally {
                if (object != null) {
                    object.close();
                }
            }
        }
        if (l2 <= 0L || l2 > 0xFFFFFFFFL) {
            return null;
        }
        object = connection.prepareStatement("SELECT account_name FROM characters WHERE objid=?");
        try {
            object.setLong(1, l2);
            try (ResultSet resultSet = object.executeQuery();){
                if (!resultSet.next()) {
                    throw new SQLException("castle owner leader is missing: " + l2);
                }
                object2 = resultSet.getString("account_name");
                if (resultSet.next()) {
                    throw new SQLException("duplicate castle leader object id: " + l2);
                }
            }
        }
        finally {
            if (object != null) {
                object.close();
            }
        }
        if (object2 == null || ((String)object2).isBlank()) {
            throw new SQLException("castle owner leader account is missing: " + l2);
        }
        object = "lito:castle-war:" + n + ":" + Math.max(0L, l / 1000L);
        return new CastleWarRewardTarget(((String)object2).trim(), n3, n2, (String)object);
    }

    private static boolean ensureCastleWarReward(Connection connection, CastleWarRewardTarget castleWarRewardTarget) throws SQLException {
        CastleWarRewardTarget castleWarRewardTarget2 = LegacyMySqlRepository.loadCastleWarReward(connection, castleWarRewardTarget.eventKey());
        boolean bl = false;
        if (castleWarRewardTarget2 == null) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `server_\u7d00\u9304_\u734e\u52f5` (`\u73a9\u5bb6\u5e33\u865f`,`\u767c\u9001\u7269\u54c1\u7de8\u865f`,`\u767c\u9001\u7269\u54c1\u6578\u91cf`,`\u5f37\u5316\u503c`,`\u9818\u53d6\u72c0\u614b`,lito_event_key) VALUES (?,?,?,0,0,?)");){
                preparedStatement.setString(1, castleWarRewardTarget.account());
                preparedStatement.setInt(2, castleWarRewardTarget.itemId());
                preparedStatement.setInt(3, castleWarRewardTarget.count());
                preparedStatement.setString(4, castleWarRewardTarget.eventKey());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("castle reward insert affected no row");
                }
                bl = true;
            }
            castleWarRewardTarget2 = LegacyMySqlRepository.loadCastleWarReward(connection, castleWarRewardTarget.eventKey());
        }
        if (!castleWarRewardTarget.equals(castleWarRewardTarget2)) {
            throw new SQLException("castle reward event key conflicts: " + castleWarRewardTarget.eventKey());
        }
        return bl;
    }

    private static CastleWarRewardTarget loadCastleWarReward(Connection connection, String string) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT `\u73a9\u5bb6\u5e33\u865f`,`\u767c\u9001\u7269\u54c1\u7de8\u865f`,`\u767c\u9001\u7269\u54c1\u6578\u91cf`,`\u5f37\u5316\u503c`,`\u9818\u53d6\u72c0\u614b`,lito_event_key FROM `server_\u7d00\u9304_\u734e\u52f5` WHERE lito_event_key=?");){
            CastleWarRewardTarget castleWarRewardTarget;
            block17: {
                ResultSet resultSet;
                block15: {
                    CastleWarRewardTarget castleWarRewardTarget2;
                    block16: {
                        preparedStatement.setString(1, string);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block15;
                            castleWarRewardTarget2 = null;
                            if (resultSet == null) break block16;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return castleWarRewardTarget2;
                }
                int n = resultSet.getInt("\u5f37\u5316\u503c");
                int n2 = resultSet.getInt("\u9818\u53d6\u72c0\u614b");
                CastleWarRewardTarget castleWarRewardTarget3 = new CastleWarRewardTarget(resultSet.getString("\u73a9\u5bb6\u5e33\u865f"), resultSet.getInt("\u767c\u9001\u7269\u54c1\u7de8\u865f"), resultSet.getInt("\u767c\u9001\u7269\u54c1\u6578\u91cf"), resultSet.getString("lito_event_key"));
                if (n != 0 || n2 < 0 || n2 > 1 || resultSet.next()) {
                    throw new SQLException("invalid castle reward row: " + string);
                }
                castleWarRewardTarget = castleWarRewardTarget3;
                if (resultSet == null) break block17;
                resultSet.close();
            }
            return castleWarRewardTarget;
        }
    }

    private static void compensateCastleWarReward(Connection connection, String string, SQLException sQLException) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `server_\u7d00\u9304_\u734e\u52f5` WHERE lito_event_key=?");){
            preparedStatement.setString(1, string);
            preparedStatement.executeUpdate();
        }
        catch (SQLException sQLException2) {
            sQLException.addSuppressed(sQLException2);
        }
    }

    private static void compensateCastleMutation(Connection connection, RepositoryCastleSnapshot repositoryCastleSnapshot, SQLException sQLException) {
        block24: {
            Object object;
            try {
                object = connection.prepareStatement("UPDATE `server_\u57ce\u5821` SET tax_rate=?,public_money=?,war_time=FROM_UNIXTIME(?) WHERE castle_id=?");
                try {
                    object.setInt(1, repositoryCastleSnapshot.taxRate());
                    object.setLong(2, repositoryCastleSnapshot.publicMoney());
                    object.setLong(3, repositoryCastleSnapshot.warTimeMillis() / 1000L);
                    object.setInt(4, repositoryCastleSnapshot.castleId());
                    object.executeUpdate();
                }
                finally {
                    if (object != null) {
                        object.close();
                    }
                }
            }
            catch (SQLException sQLException2) {
                sQLException.addSuppressed(sQLException2);
            }
            try {
                if (repositoryCastleSnapshot.snapshotVersion() == 0L) {
                    object = connection.prepareStatement("DELETE FROM lito_castle_metadata WHERE castle_id=?");
                    try {
                        object.setInt(1, repositoryCastleSnapshot.castleId());
                        object.executeUpdate();
                        break block24;
                    }
                    finally {
                        if (object != null) {
                            object.close();
                        }
                    }
                }
                object = repositoryCastleSnapshot.mercenaries();
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO lito_castle_metadata (castle_id,security_enabled,mercenary_0,mercenary_1,mercenary_2,mercenary_3,snapshot_version) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE security_enabled=VALUES(security_enabled),mercenary_0=VALUES(mercenary_0),mercenary_1=VALUES(mercenary_1),mercenary_2=VALUES(mercenary_2),mercenary_3=VALUES(mercenary_3),snapshot_version=VALUES(snapshot_version)");){
                    preparedStatement.setInt(1, repositoryCastleSnapshot.castleId());
                    preparedStatement.setBoolean(2, repositoryCastleSnapshot.securityEnabled());
                    preparedStatement.setInt(3, ((GameStateStore.MercenaryRoster)object).type0());
                    preparedStatement.setInt(4, ((GameStateStore.MercenaryRoster)object).type1());
                    preparedStatement.setInt(5, ((GameStateStore.MercenaryRoster)object).type2());
                    preparedStatement.setInt(6, ((GameStateStore.MercenaryRoster)object).type3());
                    preparedStatement.setLong(7, repositoryCastleSnapshot.snapshotVersion());
                    preparedStatement.executeUpdate();
                }
            }
            catch (SQLException sQLException3) {
                sQLException.addSuppressed(sQLException3);
            }
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean characterExists(String var1_1) throws SQLException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public String description() {
        return this.repositoryMode + "(dynamic-database=" + this.accountDatabase + ", resource-database=" + this.resourceDatabase + ", read-only=" + this.readOnly + ")";
    }

    private String resourceTable(String string) {
        return SharedDatabase.resourceTable(this, string, this.resourceDatabase);
    }

    private InventoryCatalog.Definition rewardItemDefinition(Connection connection, int n) throws SQLException {
        if (n <= 0) {
            return null;
        }
        InventoryCatalog.Definition definition = this.rewardItemCache.get(n);
        if (definition != null) {
            return definition;
        }
        ArrayList<InventoryCatalog.Definition> arrayList = new ArrayList<InventoryCatalog.Definition>(3);
        InventoryCatalog.Definition definition2 = this.rewardWeaponDefinition(connection, n);
        InventoryCatalog.Definition definition3 = this.rewardArmorDefinition(connection, n);
        InventoryCatalog.Definition definition4 = this.rewardEtcDefinition(connection, n);
        if (definition2 != null) {
            arrayList.add(definition2);
        }
        if (definition3 != null) {
            arrayList.add(definition3);
        }
        if (definition4 != null) {
            arrayList.add(definition4);
        }
        if (arrayList.size() != 1) {
            return null;
        }
        InventoryCatalog.Definition definition5 = (InventoryCatalog.Definition)arrayList.get(0);
        InventoryCatalog.Definition definition6 = this.rewardItemCache.putIfAbsent(n, definition5);
        return definition6 == null ? definition5 : definition6;
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private InventoryCatalog.Definition rewardWeaponDefinition(Connection connection, int n) throws SQLException {
        String string = "SELECT item_id,COALESCE(NULLIF(name,''),name_id) AS display_name,invgfx,grdgfx,bless,weight,itemdesc_id,type,dmg_small,dmg_large,material,safenchant,cant_delete,trade,(IF(use_royal<>0,1,0)|IF(use_knight<>0,2,0)|IF(use_elf<>0,4,0)|IF(use_mage<>0,8,0)|IF(use_darkelf<>0,16,0)|IF(use_dragonknight<>0,32,0)|IF(use_illusionist<>0,64,0)) AS class_mask FROM " + this.resourceTable("weapon") + " WHERE item_id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            InventoryCatalog.Definition definition;
            block19: {
                ResultSet resultSet;
                block17: {
                    InventoryCatalog.Definition definition2;
                    block18: {
                        preparedStatement.setInt(1, n);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block17;
                            definition2 = null;
                            if (resultSet == null) break block18;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return definition2;
                }
                ClassicWorldDropCatalog.ItemWire itemWire = new ClassicWorldDropCatalog.ItemWire(n, "weapon", resultSet.getString("display_name"), "weapon", resultSet.getInt("invgfx"), resultSet.getInt("grdgfx"), resultSet.getInt("bless"), false, resultSet.getInt("trade") == 0, resultSet.getInt("weight"), resultSet.getInt("itemdesc_id"), resultSet.getString("type"), resultSet.getInt("dmg_small"), resultSet.getInt("dmg_large"), resultSet.getString("material"), resultSet.getInt("safenchant"), resultSet.getInt("class_mask"), resultSet.getBoolean("cant_delete"));
                if (resultSet.next()) {
                    throw new SQLException("duplicate weapon item id: " + n);
                }
                definition = itemWire.inventoryDefinition();
                if (resultSet == null) break block19;
                resultSet.close();
            }
            return definition;
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid reward weapon template: " + n, illegalArgumentException);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private InventoryCatalog.Definition rewardArmorDefinition(Connection connection, int n) throws SQLException {
        String string = "SELECT item_id,COALESCE(NULLIF(name,''),name_id) AS display_name,invgfx,grdgfx,bless,weight,itemdesc_id,type,material,safenchant,cant_delete,trade,ABS(ac) AS armor_ac,add_str,add_dex,add_con,add_wis,add_int,add_cha,add_hp,add_mp,m_def,add_sp,haste_item,defense_fire,defense_water,defense_wind,defense_earth,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,(IF(use_royal<>0,1,0)|IF(use_knight<>0,2,0)|IF(use_elf<>0,4,0)|IF(use_mage<>0,8,0)|IF(use_darkelf<>0,16,0)|IF(use_dragonknight<>0,32,0)|IF(use_illusionist<>0,64,0)) AS class_mask FROM " + this.resourceTable("armor") + " WHERE item_id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            InventoryCatalog.Definition definition;
            block19: {
                ResultSet resultSet;
                block17: {
                    InventoryCatalog.Definition definition2;
                    block18: {
                        preparedStatement.setInt(1, n);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block17;
                            definition2 = null;
                            if (resultSet == null) break block18;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return definition2;
                }
                InventoryCatalog.NativeArmorStatus nativeArmorStatus = new InventoryCatalog.NativeArmorStatus(resultSet.getInt("armor_ac"), ClassicWorldDropCatalog.materialCode(resultSet.getString("material")), resultSet.getInt("weight"), resultSet.getInt("class_mask"), resultSet.getInt("safenchant"), resultSet.getInt("add_str"), resultSet.getInt("add_dex"), resultSet.getInt("add_con"), resultSet.getInt("add_wis"), resultSet.getInt("add_int"), resultSet.getInt("add_cha"), resultSet.getInt("add_hp"), resultSet.getInt("add_mp"), resultSet.getInt("m_def"), resultSet.getInt("add_sp"), resultSet.getBoolean("haste_item"), resultSet.getInt("defense_fire"), resultSet.getInt("defense_water"), resultSet.getInt("defense_wind"), resultSet.getInt("defense_earth"), resultSet.getInt("regist_blind"), resultSet.getInt("regist_sleep"), resultSet.getInt("regist_freeze"), resultSet.getInt("regist_stone"), resultSet.getInt("regist_sustain"), resultSet.getInt("regist_stun"), ClassicWorldDropCatalog.armorTypeCode(resultSet.getString("type")));
                ClassicWorldDropCatalog.ItemWire itemWire = new ClassicWorldDropCatalog.ItemWire(n, "armor", resultSet.getString("display_name"), resultSet.getString("type"), resultSet.getInt("invgfx"), resultSet.getInt("grdgfx"), resultSet.getInt("bless"), false, resultSet.getInt("trade") == 0, resultSet.getInt("weight"), resultSet.getInt("itemdesc_id"), "", 0, 0, resultSet.getString("material"), resultSet.getInt("safenchant"), resultSet.getInt("class_mask"), resultSet.getBoolean("cant_delete"));
                if (resultSet.next()) {
                    throw new SQLException("duplicate armor item id: " + n);
                }
                definition = itemWire.inventoryDefinition(nativeArmorStatus);
                if (resultSet == null) break block19;
                resultSet.close();
            }
            return definition;
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid reward armor template: " + n, illegalArgumentException);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private InventoryCatalog.Definition rewardEtcDefinition(Connection connection, int n) throws SQLException {
        String string = "SELECT item_id,COALESCE(NULLIF(name,''),name_id) AS display_name,use_type,invgfx,grdgfx,bless,stackable,weight,itemdesc_id,material,cant_delete,trade FROM " + this.resourceTable("etcitem") + " WHERE item_id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);){
            InventoryCatalog.Definition definition;
            block19: {
                ResultSet resultSet;
                block17: {
                    InventoryCatalog.Definition definition2;
                    block18: {
                        preparedStatement.setInt(1, n);
                        resultSet = preparedStatement.executeQuery();
                        try {
                            if (resultSet.next()) break block17;
                            definition2 = null;
                            if (resultSet == null) break block18;
                        }
                        catch (Throwable throwable) {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        resultSet.close();
                    }
                    return definition2;
                }
                ClassicWorldDropCatalog.ItemWire itemWire = new ClassicWorldDropCatalog.ItemWire(n, "etc", resultSet.getString("display_name"), resultSet.getString("use_type"), resultSet.getInt("invgfx"), resultSet.getInt("grdgfx"), resultSet.getInt("bless"), resultSet.getBoolean("stackable"), resultSet.getInt("trade") == 0, resultSet.getInt("weight"), resultSet.getInt("itemdesc_id"), "", 0, 0, resultSet.getString("material"), -1, 0, resultSet.getBoolean("cant_delete"));
                if (resultSet.next()) {
                    throw new SQLException("duplicate etcitem id: " + n);
                }
                definition = itemWire.inventoryDefinition();
                if (resultSet == null) break block19;
                resultSet.close();
            }
            return definition;
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new SQLException("invalid reward etcitem template: " + n, illegalArgumentException);
        }
    }

    private String sourceInventorySql() {
        String string = this.repositoryMode.equals("lito_mysql") ? "LEFT JOIN lito_item_origin o ON o.item_obj_id=i.id " : "";
        String string2 = this.repositoryMode.equals("lito_mysql") ? "IF(o.item_obj_id IS NULL,0,1) AS lito_owned,IFNULL(o.tournament_reward,0) AS tournament_reward," : "0 AS lito_owned,0 AS tournament_reward,";
        String string3 = " LEFT JOIN character_pets p ON p.item_obj_id=i.id LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id " + string;
        String string4 = "i.id AS source_object_id,i.item_id,i.count AS item_count,i.is_equipped,i.enchantlvl,i.durability,i.is_id AS is_identified,i.bless AS instance_bless,i.is_sealed AS instance_sealed,i.charge_count AS charge_count,t.usertime AS expires_at,IF(p.item_obj_id IS NULL,0,1) AS linked_pet_collar," + string2;
        String string5 = "SELECT " + string4 + "'weapon' AS item_kind,w.name_id,'weapon' AS use_token,w.invgfx,w.grdgfx,w.bless AS template_bless,0 AS stackable,IF(w.trade=0,1,0) AS tradable,w.weight AS unit_weight,w.itemdesc_id,w.type AS weapon_type,w.dmg_small,w.dmg_large,w.material,w.safenchant,w.cant_delete AS cant_delete,(IF(w.use_royal<>0,1,0)|IF(w.use_knight<>0,2,0)|IF(w.use_elf<>0,4,0)|IF(w.use_mage<>0,8,0)|IF(w.use_darkelf<>0,16,0)|IF(w.use_dragonknight<>0,32,0)|IF(w.use_illusionist<>0,64,0)) AS class_mask,0 AS armor_ac,0 AS armor_str,0 AS armor_dex,0 AS armor_con,0 AS armor_wis,0 AS armor_int,0 AS armor_cha,0 AS armor_hp,0 AS armor_mp,0 AS armor_mr,0 AS armor_sp,0 AS armor_haste,0 AS armor_fire,0 AS armor_water,0 AS armor_wind,0 AS armor_earth,0 AS armor_blind,0 AS armor_sleep,0 AS armor_freeze,0 AS armor_stone,0 AS armor_sustain,0 AS armor_stun,NULL AS doll_item_id,0 AS doll_type,0 AS doll_icon,'' AS doll_powers,'' AS doll_material_ids,'' AS doll_material_counts,0 AS doll_duration,0 AS doll_gfx,'' AS doll_name FROM character_items i JOIN " + this.resourceTable("weapon") + " w ON w.item_id=i.item_id" + string3 + "WHERE i.char_id=?";
        String string6 = "SELECT " + string4 + "'armor' AS item_kind,a.name_id,a.type AS use_token,a.invgfx,a.grdgfx,a.bless AS template_bless,0 AS stackable,IF(a.trade=0,1,0) AS tradable,a.weight AS unit_weight,a.itemdesc_id,'' AS weapon_type,0 AS dmg_small,0 AS dmg_large,a.material,a.safenchant,a.cant_delete AS cant_delete,(IF(a.use_royal<>0,1,0)|IF(a.use_knight<>0,2,0)|IF(a.use_elf<>0,4,0)|IF(a.use_mage<>0,8,0)|IF(a.use_darkelf<>0,16,0)|IF(a.use_dragonknight<>0,32,0)|IF(a.use_illusionist<>0,64,0)) AS class_mask,ABS(a.ac) AS armor_ac,a.add_str AS armor_str,a.add_dex AS armor_dex,a.add_con AS armor_con,a.add_wis AS armor_wis,a.add_int AS armor_int,a.add_cha AS armor_cha,a.add_hp AS armor_hp,a.add_mp AS armor_mp,a.m_def AS armor_mr,a.add_sp AS armor_sp,a.haste_item AS armor_haste,a.defense_fire AS armor_fire,a.defense_water AS armor_water,a.defense_wind AS armor_wind,a.defense_earth AS armor_earth,a.regist_blind AS armor_blind,a.regist_sleep AS armor_sleep,a.regist_freeze AS armor_freeze,a.regist_stone AS armor_stone,a.regist_sustain AS armor_sustain,a.regist_stun AS armor_stun,NULL AS doll_item_id,0 AS doll_type,0 AS doll_icon,'' AS doll_powers,'' AS doll_material_ids,'' AS doll_material_counts,0 AS doll_duration,0 AS doll_gfx,'' AS doll_name FROM character_items i JOIN " + this.resourceTable("armor") + " a ON a.item_id=i.item_id" + string3 + "WHERE i.char_id=?";
        String string7 = "SELECT " + string4 + "'etc' AS item_kind,e.name_id,e.use_type AS use_token,e.invgfx,e.grdgfx,e.bless AS template_bless,e.stackable,IF(e.trade=0,1,0) AS tradable,e.weight AS unit_weight,e.itemdesc_id,'' AS weapon_type,0 AS dmg_small,0 AS dmg_large,e.material,-1 AS safenchant,e.cant_delete AS cant_delete,0 AS class_mask,0 AS armor_ac,0 AS armor_str,0 AS armor_dex,0 AS armor_con,0 AS armor_wis,0 AS armor_int,0 AS armor_cha,0 AS armor_hp,0 AS armor_mp,0 AS armor_mr,0 AS armor_sp,0 AS armor_haste,0 AS armor_fire,0 AS armor_water,0 AS armor_wind,0 AS armor_earth,0 AS armor_blind,0 AS armor_sleep,0 AS armor_freeze,0 AS armor_stone,0 AS armor_sustain,0 AS armor_stun,d.itemid AS doll_item_id,d.doll_type,d.icon AS doll_icon,d.powers AS doll_powers,d.need AS doll_material_ids,d.`count` AS doll_material_counts,d.time AS doll_duration,d.gfxid AS doll_gfx,d.nameid AS doll_name FROM character_items i JOIN " + this.resourceTable("etcitem") + " e ON e.item_id=i.item_id LEFT JOIN " + this.resourceTable("etcitem_doll_type") + " d ON d.itemid=i.item_id" + string3 + "WHERE i.char_id=?";
        return "SELECT * FROM (" + string5 + " UNION ALL " + string6 + " UNION ALL " + string7 + ") source_inventory ORDER BY source_object_id";
    }

    private String warehouseInventorySql(GameStateStore.WarehouseScope warehouseScope) {
        if (warehouseScope == null) {
            throw new IllegalArgumentException("warehouse scope is required");
        }
        String string = LegacyMySqlRepository.warehouseTable(warehouseScope);
        String string2 = LegacyMySqlRepository.warehouseOwnerColumn(warehouseScope);
        String string3 = this.sourceInventorySql();
        string3 = string3.replace("FROM character_items i JOIN", "FROM " + string + " i JOIN");
        string3 = string3.replace("LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id ", "");
        string3 = string3.replace("LEFT JOIN lito_item_origin o ON o.item_obj_id=i.id ", "LEFT JOIN lito_warehouse_item_origin o ON o.item_obj_id=i.id AND o.scope='" + warehouseScope.name() + "' AND o.owner_key=i." + string2 + " ");
        string3 = string3.replace("t.usertime AS expires_at", "CASE WHEN IFNULL(i.remaining_time,0)>0 THEN TIMESTAMPADD(SECOND,i.remaining_time,CURRENT_TIMESTAMP) ELSE NULL END AS expires_at");
        return string3.replace("WHERE i.char_id=?", "WHERE i." + string2 + "=?");
    }

    private static String warehouseTable(GameStateStore.WarehouseScope warehouseScope) {
        return switch (warehouseScope) {
            default -> throw new IncompatibleClassChangeError();
            case GameStateStore.WarehouseScope.PERSONAL -> "character_warehouse";
            case GameStateStore.WarehouseScope.CLAN -> "clan_warehouse";
            case GameStateStore.WarehouseScope.ELF -> "character_elf_warehouse";
        };
    }

    private static String warehouseOwnerColumn(GameStateStore.WarehouseScope warehouseScope) {
        return warehouseScope == GameStateStore.WarehouseScope.CLAN ? "clan_name" : "account_name";
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private Map<Integer, SourceMagicDollPower> sourceMagicDollPowers(Connection connection) throws SQLException {
        String string = "SELECT id,note,classname,value_main,probability,interval_time,passive_skill_id,mode,attr_type,gfx_id,gfx_target,value_sub,trigger_skill_id,item_id,item_count,status_id FROM " + this.resourceTable("etcitem_doll_power_new") + " ORDER BY id";
        LinkedHashMap<Integer, SourceMagicDollPower> linkedHashMap = new LinkedHashMap<Integer, SourceMagicDollPower>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(string);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                try {
                    SourceMagicDollPower sourceMagicDollPower = new SourceMagicDollPower(resultSet.getInt("id"), resultSet.getString("note"), resultSet.getString("classname"), resultSet.getInt("value_main"), resultSet.getInt("probability"), resultSet.getInt("interval_time"), resultSet.getInt("passive_skill_id"), resultSet.getInt("mode"), resultSet.getInt("attr_type"), resultSet.getInt("gfx_id"), resultSet.getInt("gfx_target"), resultSet.getInt("value_sub"), resultSet.getInt("trigger_skill_id"), resultSet.getInt("item_id"), resultSet.getInt("item_count"), resultSet.getInt("status_id"));
                    if (linkedHashMap.putIfAbsent(sourceMagicDollPower.id(), sourceMagicDollPower) == null) continue;
                    throw new SQLException("duplicate etcitem_doll_power_new id: " + sourceMagicDollPower.id());
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    throw new SQLException("invalid etcitem_doll_power_new row id=" + resultSet.getInt("id"), illegalArgumentException);
                    return Map.copyOf(linkedHashMap);
                }
            }
        }
    }

    private static List<Integer> commaSeparatedPositiveInts(String string, String string2) throws SQLException {
        if (string == null || string.isBlank()) {
            return List.of();
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (String string3 : string.split(",", -1)) {
            String string4 = string3.trim();
            if (string4.isEmpty()) {
                throw new SQLException("empty magic-doll " + string2 + " token");
            }
            try {
                int n = Integer.parseInt(string4);
                if (n <= 0) {
                    throw new NumberFormatException("not positive");
                }
                arrayList.add(n);
            }
            catch (NumberFormatException numberFormatException) {
                throw new SQLException("invalid magic-doll " + string2 + " token: " + string4, numberFormatException);
            }
        }
        return List.copyOf(arrayList);
    }

    private String exactResourceTable(String string) {
        if (!("\u89d2\u8272_\u7cbe\u795e\u6210\u9577".equals(string) || "\u89d2\u8272_\u9ad4\u8cea\u6210\u9577".equals(string) || "\u89d2\u8272_\u529b\u91cf\u6210\u9577".equals(string) || "\u89d2\u8272_\u654f\u6377\u6210\u9577".equals(string))) {
            throw new IllegalArgumentException("unsupported exact resource table identifier");
        }
        return this.resourceDatabase + ".`" + string + "`";
    }

    private List<Integer> physicalGrowthColumn(Connection connection, String string, String string2) throws SQLException {
        if (!"\u547d\u4e2d".equals(string2) && !"\u50b7\u5bb3".equals(string2)) {
            throw new IllegalArgumentException("unsupported physical growth column");
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>(127);
        String string3 = "SELECT id,`" + string2 + "` FROM " + this.exactResourceTable(string) + " ORDER BY id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(string3);
             ResultSet resultSet = preparedStatement.executeQuery();){
            int n = 1;
            while (resultSet.next()) {
                if (resultSet.getInt("id") != n || n > 127) {
                    throw new SQLException("source physical growth ids are not contiguous 1..127");
                }
                arrayList.add(resultSet.getInt(string2));
                ++n;
            }
        }
        if (arrayList.size() != 127) {
            throw new SQLException("source physical growth table does not contain ids 1..127");
        }
        return List.copyOf(arrayList);
    }

    private Connection connection(String string, String string2, String string3) throws SQLException {
        if (!(this.accountUrl.equals(string) && this.accountLogin.equals(string2) && this.accountPassword.equals(string3))) {
            throw new SQLException("repository connection route mismatch");
        }
        return this.dataSource.getConnection();
    }

    @Override
    public void close() {
        this.dataSource.close();
    }

    private void checkTable(String string, String string2, String string3, String string4) throws SQLException {
        try (Connection connection = this.connection(string, string2, string3);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM " + string4 + " LIMIT 1");){
            preparedStatement.executeQuery().close();
        }
    }

    private static String jdbcUrlWithDatabase(Properties properties, String string, String string2, String string3) {
        String string4 = LegacyMySqlRepository.required(properties, string, null);
        String string5 = properties.getProperty(string3, "").trim();
        return string4 + string2 + string5;
    }

    private static String required(Properties properties, String string, String string2) {
        String string3 = properties.getProperty(string);
        if (string3 == null || string3.isBlank()) {
            if (string2 != null) {
                return string2;
            }
            throw new IllegalArgumentException("Missing legacy SQL property: " + string);
        }
        return string3.trim();
    }

    private static /* synthetic */ boolean lambda$persistCastleTreasuryTransfer$11(Map map, Map map2, Long l) {
        RuntimeInventory.Item item = (RuntimeInventory.Item)map.get(l);
        RuntimeInventory.Item item2 = (RuntimeInventory.Item)map2.get(l);
        return item != null && item.definition().catalogId() != 40308L || item2 != null && item2.definition().catalogId() != 40308L;
    }

    private static /* synthetic */ boolean lambda$claimPendingReward$10(Map map, Map map2, Long l) {
        return Objects.equals(map.get(l), map2.get(l));
    }

    private static /* synthetic */ boolean lambda$changeClanMember$9(String string, RepositoryClanMember repositoryClanMember) {
        return repositoryClanMember.name().equalsIgnoreCase(string);
    }

    private static /* synthetic */ boolean lambda$joinClan$8(RepositoryClanMember repositoryClanMember) {
        return repositoryClanMember.role() != 3;
    }

    private static /* synthetic */ boolean lambda$persistWarehouseTransfer$7(Map map, Map map2, Long l) {
        return Objects.equals(map.get(l), map2.get(l));
    }

    private static /* synthetic */ boolean lambda$persistHouseBid$5(Map map, Map map2, Long l) {
        RuntimeInventory.Item item = (RuntimeInventory.Item)map.get(l);
        RuntimeInventory.Item item2 = (RuntimeInventory.Item)map2.get(l);
        return item != null && item.definition().catalogId() != 40308L || item2 != null && item2.definition().catalogId() != 40308L;
    }

    private static /* synthetic */ boolean lambda$persistHouseTaxPayment$4(Map map, Map map2, Long l) {
        RuntimeInventory.Item item = (RuntimeInventory.Item)map.get(l);
        RuntimeInventory.Item item2 = (RuntimeInventory.Item)map2.get(l);
        return item == null || item.definition().catalogId() != 40308L || item2 != null && (item2.definition().catalogId() != 40308L || item2.quantity() >= item.quantity());
    }

    private static /* synthetic */ boolean lambda$postBoardTopic$3(Map map, Map map2, Long l) {
        RuntimeInventory.Item item = (RuntimeInventory.Item)map.get(l);
        RuntimeInventory.Item item2 = (RuntimeInventory.Item)map2.get(l);
        return item == null || item.definition().catalogId() != 40308L || item2 != null && (item2.definition().catalogId() != 40308L || item2.quantity() >= item.quantity());
    }

    private static /* synthetic */ void lambda$persistBookmarks$2(Map map, String string, SqlBookmark sqlBookmark) {
        map.put(string, sqlBookmark.bookmark());
    }

    private static /* synthetic */ LitoOwnedInventoryItem lambda$persistLitoInventoryInternal$1(LitoOwnedInventoryItem litoOwnedInventoryItem) {
        return litoOwnedInventoryItem;
    }

    private static /* synthetic */ LitoOwnedInventoryItem lambda$persistLitoInventoryInternal$0(LitoOwnedInventoryItem litoOwnedInventoryItem) {
        return litoOwnedInventoryItem;
    }

    Connection nativeMaterialCharacterConnection() throws Exception {
        return this.connection(this.accountUrl, this.accountLogin, this.accountPassword);
    }

    @Override
    public SourceNpcPhysicalStats npcPhysicalStats(int n) throws SQLException {
        SourceNpcPhysicalStats sourceNpcPhysicalStats = NativeNpcPhysicalSource.lookup(this.resourceTable("npc"), n);
        if (sourceNpcPhysicalStats != null) {
            return sourceNpcPhysicalStats;
        }
        return this.nativeLegacyNpcPhysicalStats(n);
    }

    private record SqlStoredItem(int itemId, String itemName, long count, int attrEnchantKind, int attrEnchantLevel, String gamNo, int greater) {
    }

    private record HotelSqlLease(long showId, boolean active) {
    }

    private record SqlBookmark(long id, RepositoryBookmark bookmark) {
    }

    private record SqlBuddyMetadata(long buddyObjectId, String targetSource, int groupId, int relationshipStyle, int paletteOffset, String origin) {
    }

    private record StoredBoardTopic(GameStateStore.BoardTopic topic, long authorObjectId) {
    }

    private record HouseSettlementClan(long clanId, long houseId) {
    }

    private record HouseSettlementRecord(int onSale, int purchaseBasement, String taxDeadline) {
    }

    private record HouseTaxOwner(long clanId, String clanName, String leaderName) {
    }

    private record PositionedWarehouseItem(RuntimeInventory.Item item, boolean warehouse) {
    }

    private record CharacterClanRow(long objectId, String name, long clanId, int nativeRank) {
    }

    private record MarriageCharacterRow(long objectId, String name, long partnerObjectId) {
    }

    private record CastleWarRewardTarget(String account, int itemId, int count, String eventKey) {
    }

    private record SqlCastleSecuritySettlement(long treasuryBefore, long chargeAdena, long chargedAdena, boolean prepared, boolean completed) {
    }
}
