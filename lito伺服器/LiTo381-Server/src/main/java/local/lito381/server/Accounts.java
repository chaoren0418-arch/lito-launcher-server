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

interface AccountRepository extends AutoCloseable {
    void healthCheck() throws Exception;
    /** Explicit stopped-server migration; normal startup never performs DDL. */
    default void migrateLitoExtensions() throws Exception {}
    AuthResult authenticate(String account, String password) throws Exception;
    /** Native accounts.access_level for this exact classic-source database. */
    default int accessLevel(String account) throws Exception { return 0; }
    /** GM command policy from the selected static resource database. */
    default GmCommandDefinition gmCommand(String name) throws Exception { return null; }
    /** Database-owned LiTo Tournament point-shop rows for this source route. */
    default List<RepositoryTournamentShopOffer> tournamentShopOffers() throws Exception {
        return List.of();
    }
    /** Common static-database controls for LiTo battle rules, objectives and spawns. */
    default RepositoryBattleConfiguration battleConfiguration() throws Exception {
        return RepositoryBattleConfiguration.empty();
    }
    boolean accountExists(String account) throws Exception;
    /** Removes only an account explicitly marked as LiTo-owned and having no characters. */
    default boolean deleteLitoAccount(String account) throws Exception { return false; }
    AccountMutationResult createAccount(String account, String password) throws Exception;
    AccountMutationResult changePassword(String account, String oldPassword, String newPassword) throws Exception;
    AccountMutationResult registerQuiz(
            String account, String oldPassword, String newPassword, List<String> quizFields) throws Exception;
    AccountMutationResult changeAccountInfo(String account, int type, List<String> fields) throws Exception;
    List<CharacterSummary> characters(String account) throws Exception;
    /** Retained characters.HellTime for the selected original character. */
    default int hellTime(long characterObjectId) throws Exception { return 0; }
    /** Retained character_other killCount/deathCount for the selected character. */
    default SourceKillDeathCounts killDeathCounts(long characterObjectId) throws Exception {
        return SourceKillDeathCounts.none();
    }
    Set<Integer> learnedSkills(long characterObjectId) throws Exception;
    Map<Integer, Integer> questSteps(long characterObjectId) throws Exception;
    List<SourceInventoryItem> sourceInventory(long characterObjectId) throws Exception;
    /** Versioned LiTo-only inventory rows that cannot enter the shared static catalog. */
    default RepositoryPrivateInventorySnapshot litoPrivateInventory(
            long characterObjectId) throws Exception {
        return RepositoryPrivateInventorySnapshot.unavailable();
    }
    /** SQL-authoritative classic teleport bookmarks when available. */
    default RepositoryBookmarkSnapshot bookmarks(long characterObjectId) throws Exception {
        return RepositoryBookmarkSnapshot.unavailable();
    }
    /** Replaces the changed bookmark rows after a whole-snapshot compare. */
    default RepositoryBookmarkPersistenceResult persistBookmarks(
            long characterObjectId, List<RepositoryBookmark> expected,
            List<RepositoryBookmark> replacement) throws Exception {
        return RepositoryBookmarkPersistenceResult.READ_ONLY;
    }
    /** Published retained-buff snapshot from character_buff(_snapshot). */
    default RepositoryBuffSnapshot buffs(long characterObjectId) throws Exception {
        return RepositoryBuffSnapshot.unavailable();
    }
    /** Publishes a whole retained-buff snapshot after exact version-and-row CAS. */
    default RepositoryBuffPersistence persistBuffs(
            long characterObjectId, RepositoryBuffSnapshot expected,
            List<RepositoryBuff> replacement) throws Exception {
        return RepositoryBuffPersistence.readOnly(expected);
    }
    /** Native buddy rows plus LiTo group/display extensions. */
    default RepositoryBuddySnapshot buddies(long characterObjectId) throws Exception {
        return RepositoryBuddySnapshot.unavailable();
    }
    default RepositoryBuddyPersistence persistBuddies(
            long characterObjectId, RepositoryBuddySnapshot expected,
            List<RepositoryBuddyGroup> groups, List<RepositoryBuddy> buddies)
            throws Exception {
        return RepositoryBuddyPersistence.readOnly(expected);
    }
    /** Native physical letters keyed by the exact backing character_items.id. */
    default RepositoryLetterSnapshot letters(long characterObjectId) throws Exception {
        return RepositoryLetterSnapshot.unavailable();
    }
    /** Locked same-dynamic-database delivery: paper -> item + letter body. */
    default RepositoryLetterDelivery deliverLetter(
            long senderCharacterObjectId, long paperObjectId, String receiverName,
            int code, String date, String subject, String content) throws Exception {
        return new RepositoryLetterDelivery(RepositoryLetterDeliveryStatus.READ_ONLY, null);
    }
    /** Exact-row CAS transition from native 49016/unread to 49017/read. */
    default RepositoryLetterMutation openLetter(
            long characterObjectId, RepositoryLetter expected) throws Exception {
        return RepositoryLetterMutation.readOnly(expected);
    }
    /** Exact-row CAS removal of the backing item, body and LiTo provenance. */
    default RepositoryLetterMutation deleteLetter(
            long characterObjectId, RepositoryLetter expected) throws Exception {
        return RepositoryLetterMutation.readOnly(expected);
    }
    /** LiTo-native board page scoped to the selected dynamic database. */
    default RepositoryBoardSnapshot boardTopicsBefore(long cursor, int limit) throws Exception {
        return RepositoryBoardSnapshot.unavailable();
    }
    /** One LiTo-native board topic scoped to the selected dynamic database. */
    default RepositoryBoardLookup boardTopic(long topicId) throws Exception {
        return RepositoryBoardLookup.unavailable();
    }
    /** Atomically debits the exact Adena inventory snapshot and appends a topic. */
    default RepositoryBoardMutation postBoardTopic(
            long characterObjectId, String date, String title, String content,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryBoardMutation.unavailable();
    }
    /** Author-only exact-snapshot deletion of one LiTo-native board topic. */
    default RepositoryBoardMutation deleteBoardTopic(
            long characterObjectId, GameStateStore.BoardTopic expected) throws Exception {
        return RepositoryBoardMutation.unavailable();
    }
    /** Native server_盟屋_拍賣 rows plus LiTo optimistic revisions. */
    default RepositoryHouseAuctionSnapshot houseAuctions() throws Exception {
        return RepositoryHouseAuctionSnapshot.unavailable();
    }
    /** Atomic bidder debit, previous-bidder refund and native auction CAS. */
    default RepositoryHouseBidMutation persistHouseBid(
            long bidderCharacterObjectId, GameStateStore.HouseAuction expected,
            long amount, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryHouseBidMutation.unavailable();
    }
    /** Exact-snapshot expiry settlement for one native clan-house auction. */
    default RepositoryHouseSettlement persistHouseAuctionSettlement(
            GameStateStore.HouseAuction expected, long nowEpochSeconds,
            int taxIntervalDays) throws Exception {
        return RepositoryHouseSettlement.unavailable();
    }
    /** Native non-sale clan houses whose tax deadline has elapsed. */
    default RepositoryHouseTaxSnapshot expiredHouseTaxes(
            long nowEpochSeconds) throws Exception {
        return RepositoryHouseTaxSnapshot.unavailable();
    }
    /** Exact-snapshot conversion of one tax-defaulted house into an auction. */
    default RepositoryHouseTaxMutation persistHouseTaxExpiry(
            RepositoryHouseTaxState expected, long nowEpochSeconds,
            int taxIntervalDays) throws Exception {
        return RepositoryHouseTaxMutation.unavailable();
    }
    /** Selected-character view of the native keeper/owner/tax relationship. */
    default RepositoryHouseTaxQuote houseTaxQuote(
            long characterObjectId, int keeperNpcId) throws Exception {
        return RepositoryHouseTaxQuote.unavailable();
    }
    /** Atomic native house-tax deadline renewal and exact Adena debit. */
    default RepositoryHouseTaxPayment persistHouseTaxPayment(
            long characterObjectId, RepositoryHouseTaxQuote expected,
            long nowEpochSeconds, int taxIntervalDays, long taxAdena,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryHouseTaxPayment.unavailable();
    }
    /** Source-qualified durable classic hotel leases and transferable keys. */
    default RepositoryHotelSnapshot hotelState() throws Exception {
        return RepositoryHotelSnapshot.unavailable();
    }
    /** Source-qualified durable Water Dragon two-hour pass leases. */
    default RepositoryWaterDragonPassSnapshot waterDragonPassState() throws Exception {
        return RepositoryWaterDragonPassSnapshot.unavailable();
    }
    /** SQL-authoritative Tournament profiles for every character in this dynamic database. */
    default RepositoryTournamentRoster tournamentProfiles() throws Exception {
        return RepositoryTournamentRoster.unavailable();
    }
    /** Whole-profile CAS; a missing expected row may only create one matching character row. */
    default RepositoryTournamentMutation persistTournamentProfile(
            long characterObjectId, String characterName,
            RepositoryTournamentProfile expected,
            GameStateStore.TournamentProfileState replacement) throws Exception {
        return RepositoryTournamentMutation.unavailable();
    }
    /** Complete native clan row and every member in the selected dynamic database. */
    default RepositoryClanSnapshot clan(long characterObjectId) throws Exception {
        return RepositoryClanSnapshot.unavailable();
    }
    /** SQL-authoritative characters.Title for clan and non-clan characters. */
    default RepositoryCharacterTitle characterTitle(long characterObjectId) throws Exception {
        return RepositoryCharacterTitle.unavailable();
    }
    /** Exact-value CAS update of characters.Title in the selected dynamic database. */
    default RepositoryTitleMutation persistCharacterTitle(
            long characterObjectId, String expected, String replacement) throws Exception {
        return RepositoryTitleMutation.unavailable();
    }
    /** SQL-authoritative reciprocal characters.PartnerID relation. */
    default RepositoryMarriageState marriage(long characterObjectId) throws Exception {
        return RepositoryMarriageState.unavailable();
    }
    /** Locked two-row creation; succeeds only while both characters are unmarried. */
    default RepositoryMarriageMutation persistMarriage(
            long firstCharacterObjectId, long secondCharacterObjectId) throws Exception {
        return RepositoryMarriageMutation.unavailable();
    }
    /** Locked two-row removal; the expected partner prevents a stale divorce. */
    default RepositoryMarriageMutation persistDivorce(
            long characterObjectId, long expectedPartnerObjectId) throws Exception {
        return RepositoryMarriageMutation.unavailable();
    }
    /** Locked creation of clan_data plus the owner's characters membership. */
    default RepositoryClanMutation createClan(
            long ownerCharacterObjectId, String clanName) throws Exception {
        return RepositoryClanMutation.unavailable();
    }
    /** Locked admission of one same-route character into the owner's clan. */
    default RepositoryClanMutation joinClan(
            long ownerCharacterObjectId, String applicantName,
            int maximumMembers) throws Exception {
        return RepositoryClanMutation.unavailable();
    }
    /** Locked role change or removal (replacement role zero) of one clan member. */
    default RepositoryClanMutation changeClanMember(
            long ownerCharacterObjectId, String memberName,
            int expectedRole, int replacementRole) throws Exception {
        return RepositoryClanMutation.unavailable();
    }
    /** LiTo-only delayed-destruction bit attached to an otherwise native clan. */
    default RepositoryClanMutation setClanDestroyPending(
            long ownerCharacterObjectId, boolean expected, boolean replacement)
            throws Exception {
        return RepositoryClanMutation.unavailable();
    }
    /** Compensates only a newly created, LiTo-owned and otherwise untouched clan. */
    default boolean rollbackCreatedClan(
            long ownerCharacterObjectId, long clanId) throws Exception {
        return false;
    }
    /** SQL-authoritative account/clan/elven warehouse snapshot when available. */
    default RepositoryWarehouseSnapshot warehouse(
            GameStateStore.WarehouseScope scope, String owner) throws Exception {
        return RepositoryWarehouseSnapshot.unavailable();
    }
    /** SQL-authoritative native warehouse password when available. */
    default RepositoryWarehousePassword warehousePassword(String account) throws Exception {
        return RepositoryWarehousePassword.unavailable();
    }
    default RepositoryWarehousePasswordChange changeWarehousePassword(
            String account, int expectedOld, int replacement) throws Exception {
        return RepositoryWarehousePasswordChange.unavailable();
    }
    /**
     * Moves the changed native objects between character_items and one
     * warehouse while both MyISAM tables are exclusively locked.
     */
    default RepositoryWarehouseTransferResult persistWarehouseTransfer(
            long characterObjectId, GameStateStore.WarehouseScope scope, String owner,
            RuntimeInventory beforeInventory, RuntimeInventory beforeWarehouse,
            RuntimeInventory afterInventory, RuntimeInventory afterWarehouse,
            Map<Long, Long> localToSourceObjectIds)
            throws Exception {
        return RepositoryWarehouseTransferResult.READ_ONLY;
    }
    SourceEquippedFireResistance equippedFireResistance(long characterObjectId) throws Exception;
    SourceBaseMagicResistance baseMagicResistance(long characterObjectId) throws Exception;
    SourceEquipmentMagicStats equipmentMagicStats(long characterObjectId) throws Exception;
    /** Exact skills_魔屏無效技能 rows for the selected classic resource source. */
    default Set<Integer> counterMagicBypassSkills() throws Exception {
        // Memory fixtures model the selected primary 380_db profile.
        return Set.of(44, 87, 112, 132, 153, 161, 173, 174, 187, 188, 192);
    }
    /** Exact skills.reuseDelay rows for the selected classic resource source. */
    default Map<Integer, Integer> skillReuseDelays() throws Exception {
        // Memory fixtures use the selected catalog profile directly.
        return Map.of();
    }
    default SourceEquipmentEvaluation equipmentEvaluation(long characterObjectId)
            throws Exception {
        return new SourceEquipmentEvaluation(
                equipmentMagicStats(characterObjectId), SourceEquipmentVitalStats.none());
    }
    default SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId, Map<Long, Boolean> equippedOverrides) throws Exception {
        if (equippedOverrides == null) {
            throw new IllegalArgumentException("source equipment overrides are required");
        }
        if (!equippedOverrides.isEmpty()) {
            throw new UnsupportedOperationException(
                    "repository cannot evaluate source equipment overrides");
        }
        return equipmentEvaluation(characterObjectId);
    }
    SourcePhysicalGrowthStats physicalGrowthStats() throws Exception;
    SourceNpcPhysicalStats npcPhysicalStats(int npcId) throws Exception;
    SourceSummonTemplate summonTemplate(int npcId) throws Exception;
    List<SourcePet> pets(long characterObjectId) throws Exception;
    SourcePetTemplate petTemplate(int npcId) throws Exception;
    SourceInventoryWeightStats inventoryWeightStats(long characterObjectId) throws Exception;
    ClanEmblemData clanEmblem(String clanName) throws Exception;
    CastleSummary castleForClan(String clanName) throws Exception;
    CastleSummary castleById(int castleId) throws Exception;
    List<CastleWarSchedule> castleWarSchedules() throws Exception;
    /** Native server_城堡 plus LiTo-only security/mercenary CAS metadata. */
    default RepositoryCastleSnapshot castleState(int castleId) throws Exception {
        return RepositoryCastleSnapshot.unavailable();
    }
    /** Owner-authorized whole castle-state CAS inside one selected dynamic DB. */
    default RepositoryCastleMutation persistCastleState(
            long ownerCharacterObjectId, RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement) throws Exception {
        return RepositoryCastleMutation.unavailable();
    }
    /** Server-authoritative siege-end CAS for war time, tax and treasury reset. */
    default RepositoryCastleMutation persistCastleWarEnd(
            RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement) throws Exception {
        return RepositoryCastleMutation.unavailable();
    }
    /**
     * Idempotently reserves the official 100,000-Adena castle-security
     * service for one source war start. A READY result means the charge is
     * durable but the world expulsion still needs to be completed.
     */
    default RepositoryCastleSecuritySettlement prepareCastleSecurity(
            int castleId, long warStartMillis, long chargeAdena) throws Exception {
        return RepositoryCastleSecuritySettlement.unavailable();
    }
    /** Marks the corresponding READY security service after its world effect. */
    default RepositoryCastleSecuritySettlement completeCastleSecurity(
            int castleId, long warStartMillis) throws Exception {
        return RepositoryCastleSecuritySettlement.unavailable();
    }
    /** SQL-authoritative native server_紀錄_獎勵 rows for one routed account. */
    default RepositoryPendingRewardSnapshot pendingRewards(String account) throws Exception {
        return RepositoryPendingRewardSnapshot.unavailable();
    }
    /**
     * One SQL-first reward transition. The exact inventory/warehouse delta and
     * the native pending row are committed under the same selected-DB lock.
     */
    default RepositoryPendingRewardClaim claimPendingReward(
            long characterObjectId, String account,
            RepositoryPendingReward expected, String characterName, String ip,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            RuntimeInventory beforePersonalWarehouse,
            RuntimeInventory afterPersonalWarehouse,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryPendingRewardClaim.unavailable(expected);
    }
    /** Atomically moves one native castle id from its old clan to the winning clan. */
    default RepositoryCastleOwnershipStatus persistCastleOwnership(
            long winnerLeaderObjectId, int castleId) throws Exception {
        return RepositoryCastleOwnershipStatus.UNAVAILABLE;
    }
    /**
     * Moves Adena between one character inventory and the castle treasury
     * while both native MyISAM tables are held in the same lock group.
     */
    default RepositoryCastleTreasuryMutation persistCastleTreasuryTransfer(
            long ownerCharacterObjectId, RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds)
            throws Exception {
        return RepositoryCastleTreasuryMutation.unavailable();
    }
    boolean characterExists(String characterName) throws Exception;
    default RepositoryCharacterMutation createLitoCharacter(
            String account, Protocol.CustomCharacterRequest request) throws Exception {
        return RepositoryCharacterMutation.readOnly();
    }
    default RepositoryCharacterMutation deleteLitoCharacter(
            String account, String characterName) throws Exception {
        return RepositoryCharacterMutation.readOnly();
    }
    default RepositoryPersistenceResult persistLitoCharacter(
            LitoCharacterPersistence persistence) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }
    default RepositoryPersistenceResult persistLitoSourceInventory(
            long characterObjectId, List<LitoSourceInventoryMutation> mutations)
            throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }
    default RepositoryPersistenceResult persistLitoInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems) throws Exception {
        return persistLitoSourceInventory(characterObjectId, sourceMutations);
    }
    default RepositoryPersistenceResult persistLitoInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds) throws Exception {
        return persistLitoInventory(characterObjectId, sourceMutations, litoOwnedItems);
    }
    /**
     * Whole-owned-inventory commit with an optional LiTo-private snapshot CAS.
     * Shared and private rows are committed under the same MyISAM lock group.
     */
    default RepositoryPersistenceResult persistLitoInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory)
            throws Exception {
        return persistLitoInventory(characterObjectId, sourceMutations, litoOwnedItems,
                pets, deletedSourcePetCollarIds);
    }
    /** Whole inventory plus source-qualified hotel lease/key relation under one lock. */
    default RepositoryPersistenceResult persistLitoHotelInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            RepositoryHotelChange hotelChange) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }
    /** Whole inventory plus one Water Dragon pass lease under the same lock. */
    default RepositoryPersistenceResult persistLitoWaterDragonPassInventory(
            long characterObjectId,
            List<LitoSourceInventoryMutation> sourceMutations,
            List<LitoOwnedInventoryItem> litoOwnedItems,
            List<LitoPetPersistence> pets,
            Set<Long> deletedSourcePetCollarIds,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            RepositoryWaterDragonPassChange passChange) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }
    /** SQL-first CAS for the durable Tournament-reward subset only. */
    default RepositoryPersistenceResult persistTournamentRewardInventory(
            long characterObjectId,
            RepositoryPrivateInventorySnapshot expectedPrivateInventory,
            List<LitoOwnedInventoryItem> replacementRewards) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }
    String description();
    default AccountRepository repositoryForClassicSource(String classicServerName) {
        return this;
    }
    @Override default void close() throws Exception {}

    static AccountRepository open(ServerConfig config) throws Exception {
        return switch (config.databaseMode()) {
            case "memory" -> openMemory(config);
            case "legacy_mysql", "lito_mysql" -> openLegacy(config);
            default -> throw new IllegalArgumentException("Unsupported database.mode: " + config.databaseMode());
        };
    }

    private static AccountRepository openMemory(ServerConfig config) {
        if (config.classicServerNames().size() == 1) {
            return new MemoryAccountRepository(config.memoryAccount(), config.memoryPassword(),
                    config.memoryAuthenticationFailureAccount(),
                    config.memorySourceInventoryWeight(), config.memorySourceWeightRate(),
                    config.memorySourceFood(), config.memorySourcePrimaryLevel(),
                    config.memorySourcePrimaryMaxHp(),
                    config.memorySourcePrimaryMaxMp(),
                    config.memorySourcePrimaryArmorClass(),
                    config.memorySourcePrimaryStrength(),
                    config.memorySourcePrimaryConstitution(),
                    config.memorySourcePrimaryDexterity(),
                    config.memorySourcePrimaryCharisma(),
                    config.memorySourcePrimaryIntelligence(),
                    config.memorySourcePrimaryWisdom(),
                    config.memorySourceSecondaryMaxMp(),
                    config.memorySourceInventoryFixtureEnabled(),
                    config.memorySourceProjectileFixtureEnabled(),
                    config.memoryCastleWarFixtureStartDelaySeconds(),
                    config.memoryCastleWarFixtureDurationMinutes(),
                    config.memoryCastleWarFixtureIntervalMinutes());
        }
        Map<String, AccountRepository> routes = new LinkedHashMap<>();
        for (String source : config.classicServerNames()) {
            routes.put(source, new MemoryAccountRepository(
                    config.memoryAccount(), config.memoryPassword(),
                    config.memoryAuthenticationFailureAccount(),
                    config.memorySourceInventoryWeight(), config.memorySourceWeightRate(),
                    config.memorySourceFood(), config.memorySourcePrimaryLevel(),
                    config.memorySourcePrimaryMaxHp(),
                    config.memorySourcePrimaryMaxMp(),
                    config.memorySourcePrimaryArmorClass(),
                    config.memorySourcePrimaryStrength(),
                    config.memorySourcePrimaryConstitution(),
                    config.memorySourcePrimaryDexterity(),
                    config.memorySourcePrimaryCharisma(),
                    config.memorySourcePrimaryIntelligence(),
                    config.memorySourcePrimaryWisdom(),
                    config.memorySourceSecondaryMaxMp(),
                    config.memorySourceInventoryFixtureEnabled(),
                    config.memorySourceProjectileFixtureEnabled(),
                    config.memoryCastleWarFixtureStartDelaySeconds(),
                    config.memoryCastleWarFixtureDurationMinutes(),
                    config.memoryCastleWarFixtureIntervalMinutes()));
        }
        return new SourceRoutedAccountRepository(routes);
    }

    private static AccountRepository openLegacy(ServerConfig config) throws Exception {
        List<String> databases = config.legacySourceDatabases();
        List<String> resourceDatabases = config.legacySourceResourceDatabases();
        List<Double> weightRates = config.legacySourceWeightRates();
        List<Integer> sourceIds = config.classicServerIds();
        if (resourceDatabases.size() != config.classicServerNames().size()) {
            throw new IllegalArgumentException(
                    "classic source/resource database route count is inconsistent");
        }
        if (config.classicServerNames().size() == 1) {
            String database = databases.isEmpty()
                    ? config.legacyLoginDatabase() : databases.get(0);
            return new LegacyMySqlRepository(
                    config.legacySqlProperties(), config.classicServerNames().get(0),
                    sourceIds.get(0),
                    database, resourceDatabases.get(0),
                    weightRates.get(0), config.databaseReadOnly(), config.databaseMode());
        }
        if (databases.size() != config.classicServerNames().size()) {
            throw new IllegalArgumentException(
                    "classic source/database route count is inconsistent");
        }
        Map<String, AccountRepository> routes = new LinkedHashMap<>();
        for (int index = 0; index < config.classicServerNames().size(); index++) {
            routes.put(config.classicServerNames().get(index), new LegacyMySqlRepository(
                    config.legacySqlProperties(), config.classicServerNames().get(index),
                    sourceIds.get(index),
                    databases.get(index),
                    resourceDatabases.get(index), weightRates.get(index),
                    config.databaseReadOnly(), config.databaseMode()));
        }
        return new SourceRoutedAccountRepository(routes);
    }
}

/** One database-owned GM command definition; executors remain server allow-listed. */
record GmCommandDefinition(String name, int requiredAccessLevel, String executor,
                           String note, boolean systemCommand) {
    GmCommandDefinition {
        name = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        executor = executor == null ? "" : executor.trim();
        note = note == null ? "" : note.trim();
        if (name.isEmpty() || requiredAccessLevel < 0 || requiredAccessLevel > 200
                || executor.isEmpty()) {
            throw new IllegalArgumentException("invalid GM command definition");
        }
    }
}

/** One enabled row from lito_static.lito_tournament_shop. */
record RepositoryTournamentShopOffer(long itemId, int sortOrder,
                                     long buyPrice, long sellPrice) {
    RepositoryTournamentShopOffer {
        if (itemId <= 0 || sortOrder < 0 || buyPrice <= 0 || sellPrice <= 0) {
            throw new IllegalArgumentException("invalid Tournament shop database row");
        }
    }
}

record RepositoryBattleRule(int modeId, int mapId, boolean enabled,
                            int timeLimitSeconds, int goal, int minimumPlayers) {}

record RepositoryBattleObject(int modeId, int mapId, String role,
                              int x, int y, int gfx, int maximumHp) {}

record RepositoryBattleSpawn(int modeId, int mapId, int wave, int sortOrder,
                             int npcId, int quantity, Integer x, Integer y) {}

/** One database-owned player deployment cell for a native mode/map pair. */
record RepositoryBattlePlayerSpawn(int modeId, int mapId, int spawnIndex,
                                   int x, int y) {}

record RepositoryBattleConfiguration(List<RepositoryBattleRule> rules,
                                     List<RepositoryBattleObject> objects,
                                     List<RepositoryBattleSpawn> spawns,
                                     List<RepositoryBattlePlayerSpawn> playerSpawns) {
    RepositoryBattleConfiguration(List<RepositoryBattleRule> rules,
                                  List<RepositoryBattleObject> objects,
                                  List<RepositoryBattleSpawn> spawns) {
        this(rules, objects, spawns, List.of());
    }

    RepositoryBattleConfiguration {
        rules = rules == null ? List.of() : List.copyOf(rules);
        objects = objects == null ? List.of() : List.copyOf(objects);
        spawns = spawns == null ? List.of() : List.copyOf(spawns);
        playerSpawns = playerSpawns == null ? List.of() : List.copyOf(playerSpawns);
    }

    static RepositoryBattleConfiguration empty() {
        return new RepositoryBattleConfiguration(
                List.of(), List.of(), List.of(), List.of());
    }
}

record AuthResult(boolean accepted, boolean banned, String account,
                  Set<String> authorizedClassicSources) {
    AuthResult {
        authorizedClassicSources = authorizedClassicSources == null
                ? Set.of() : Set.copyOf(authorizedClassicSources);
    }

    AuthResult(boolean accepted, boolean banned, String account) {
        this(accepted, banned, account, Set.of());
    }

    static AuthResult rejected(String account) {
        return new AuthResult(false, false, account, Set.of());
    }

    boolean authorizesClassicSource(String classicServerName) {
        if (authorizedClassicSources.isEmpty()) return true;
        return authorizedClassicSources.stream()
                .anyMatch(source -> source.equalsIgnoreCase(classicServerName));
    }
}

/**
 * Immutable routing layer for the original character sources advertised by
 * S_SERVER_LIST. Authentication is checked independently against every
 * configured source; the returned capability names are then enforced when a
 * native C_C8 request selects the corresponding character directory.
 */
final class SourceRoutedAccountRepository implements AccountRepository {
    private record Route(String name, AccountRepository repository) {}

    private final Map<String, Route> routes = new LinkedHashMap<>();

    SourceRoutedAccountRepository(Map<String, AccountRepository> sourceRoutes) {
        if (sourceRoutes == null || sourceRoutes.size() < 2) {
            throw new IllegalArgumentException("source router requires at least two routes");
        }
        for (Map.Entry<String, AccountRepository> entry : sourceRoutes.entrySet()) {
            String name = entry.getKey() == null ? "" : entry.getKey().trim();
            AccountRepository repository = entry.getValue();
            String key = key(name);
            if (key.isEmpty() || repository == null || routes.containsKey(key)) {
                throw new IllegalArgumentException("invalid or duplicate classic source route");
            }
            routes.put(key, new Route(name, repository));
        }
    }

    @Override public void healthCheck() throws Exception {
        for (Route route : routes.values()) route.repository().healthCheck();
    }

    @Override public void migrateLitoExtensions() throws Exception {
        for (Route route : routes.values()) route.repository().migrateLitoExtensions();
    }

    @Override public AuthResult authenticate(String account, String password) throws Exception {
        Set<String> authorized = new LinkedHashSet<>();
        String canonicalAccount = account;
        boolean banned = false;
        Exception backendFailure = null;
        for (Route route : routes.values()) {
            try {
                AuthResult result = route.repository().authenticate(account, password);
                if (result.accepted()) {
                    authorized.add(route.name());
                    if (authorized.size() == 1) canonicalAccount = result.account();
                } else if (result.banned()) {
                    banned = true;
                }
            } catch (Exception failure) {
                if (backendFailure == null) backendFailure = failure;
                else backendFailure.addSuppressed(failure);
            }
        }
        if (!authorized.isEmpty()) {
            return new AuthResult(true, false, canonicalAccount, authorized);
        }
        // Do not convert a partial backend outage into a definitive password
        // rejection or ban result. Startup health checks normally prevent this
        // path, but the runtime behavior must remain fail-closed.
        if (backendFailure != null) throw backendFailure;
        return new AuthResult(false, banned, canonicalAccount, Set.of());
    }

    @Override public RepositoryBattleConfiguration battleConfiguration() throws Exception {
        return primary().battleConfiguration();
    }

    @Override public AccountRepository repositoryForClassicSource(String classicServerName) {
        Route route = routes.get(key(classicServerName));
        if (route == null) {
            throw new IllegalArgumentException("unknown classic source repository");
        }
        return route.repository();
    }

    @Override public AccountMutationResult createAccount(String account, String password)
            throws Exception {
        for (Route route : routes.values()) {
            if (route.repository().accountExists(account)) {
                return AccountMutationResult.EXISTS;
            }
        }
        List<AccountRepository> created = new ArrayList<>();
        try {
            for (Route route : routes.values()) {
                AccountMutationResult result = route.repository().createAccount(account, password);
                if (result != AccountMutationResult.SUCCESS) {
                    compensateAccountCreation(account, created, null);
                    return result;
                }
                created.add(route.repository());
            }
            return AccountMutationResult.SUCCESS;
        } catch (Exception failure) {
            compensateAccountCreation(account, created, failure);
            throw failure;
        }
    }

    @Override public AccountMutationResult changePassword(
            String account, String oldPassword, String newPassword) throws Exception {
        for (Route route : routes.values()) {
            if (!route.repository().accountExists(account)) {
                return AccountMutationResult.NOT_FOUND;
            }
            AuthResult authentication = route.repository().authenticate(account, oldPassword);
            if (!authentication.accepted()) {
                return AccountMutationResult.WRONG_PASSWORD;
            }
        }
        List<AccountRepository> changed = new ArrayList<>();
        try {
            for (Route route : routes.values()) {
                AccountMutationResult result = route.repository()
                        .changePassword(account, oldPassword, newPassword);
                if (result != AccountMutationResult.SUCCESS) {
                    compensatePasswordChange(account, oldPassword, newPassword, changed, null);
                    return result;
                }
                changed.add(route.repository());
            }
            return AccountMutationResult.SUCCESS;
        } catch (Exception failure) {
            compensatePasswordChange(account, oldPassword, newPassword, changed, failure);
            throw failure;
        }
    }

    @Override public boolean accountExists(String account) throws Exception {
        for (Route route : routes.values()) {
            if (route.repository().accountExists(account)) return true;
        }
        return false;
    }

    @Override public boolean deleteLitoAccount(String account) throws Exception {
        boolean deleted = true;
        Exception failure = null;
        for (Route route : routes.values()) {
            try {
                deleted &= route.repository().deleteLitoAccount(account);
            } catch (Exception routeFailure) {
                deleted = false;
                if (failure == null) failure = routeFailure;
                else failure.addSuppressed(routeFailure);
            }
        }
        if (failure != null) throw failure;
        return deleted;
    }

    @Override public AccountMutationResult registerQuiz(
            String account, String oldPassword, String newPassword, List<String> quizFields)
            throws Exception {
        return primary().registerQuiz(account, oldPassword, newPassword, quizFields);
    }

    @Override public AccountMutationResult changeAccountInfo(
            String account, int type, List<String> fields) throws Exception {
        return primary().changeAccountInfo(account, type, fields);
    }

    @Override public List<CharacterSummary> characters(String account) throws Exception {
        return primary().characters(account);
    }

    @Override public int hellTime(long characterObjectId) throws Exception {
        return primary().hellTime(characterObjectId);
    }

    @Override public SourceKillDeathCounts killDeathCounts(long characterObjectId)
            throws Exception {
        return primary().killDeathCounts(characterObjectId);
    }

    @Override public Set<Integer> learnedSkills(long characterObjectId) throws Exception {
        return primary().learnedSkills(characterObjectId);
    }

    @Override public Map<Integer, Integer> questSteps(long characterObjectId) throws Exception {
        return primary().questSteps(characterObjectId);
    }

    @Override public List<SourceInventoryItem> sourceInventory(long characterObjectId)
            throws Exception {
        return primary().sourceInventory(characterObjectId);
    }

    @Override public RepositoryPrivateInventorySnapshot litoPrivateInventory(
            long characterObjectId) throws Exception {
        return primary().litoPrivateInventory(characterObjectId);
    }

    @Override public RepositoryBoardSnapshot boardTopicsBefore(long cursor, int limit)
            throws Exception {
        return primary().boardTopicsBefore(cursor, limit);
    }

    @Override public RepositoryBoardLookup boardTopic(long topicId) throws Exception {
        return primary().boardTopic(topicId);
    }

    @Override public RepositoryBoardMutation postBoardTopic(
            long characterObjectId, String date, String title, String content,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return primary().postBoardTopic(characterObjectId, date, title, content,
                beforeInventory, afterInventory, localToSourceObjectIds);
    }

    @Override public RepositoryBoardMutation deleteBoardTopic(
            long characterObjectId, GameStateStore.BoardTopic expected) throws Exception {
        return primary().deleteBoardTopic(characterObjectId, expected);
    }

    @Override public RepositoryHouseAuctionSnapshot houseAuctions() throws Exception {
        return primary().houseAuctions();
    }

    @Override public RepositoryHouseBidMutation persistHouseBid(
            long bidderCharacterObjectId, GameStateStore.HouseAuction expected,
            long amount, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return primary().persistHouseBid(bidderCharacterObjectId, expected, amount,
                beforeInventory, afterInventory, localToSourceObjectIds);
    }

    @Override public RepositoryHouseSettlement persistHouseAuctionSettlement(
            GameStateStore.HouseAuction expected, long nowEpochSeconds,
            int taxIntervalDays) throws Exception {
        return primary().persistHouseAuctionSettlement(
                expected, nowEpochSeconds, taxIntervalDays);
    }

    @Override public RepositoryHouseTaxSnapshot expiredHouseTaxes(
            long nowEpochSeconds) throws Exception {
        return primary().expiredHouseTaxes(nowEpochSeconds);
    }

    @Override public RepositoryHouseTaxMutation persistHouseTaxExpiry(
            RepositoryHouseTaxState expected, long nowEpochSeconds,
            int taxIntervalDays) throws Exception {
        return primary().persistHouseTaxExpiry(
                expected, nowEpochSeconds, taxIntervalDays);
    }

    @Override public RepositoryHouseTaxQuote houseTaxQuote(
            long characterObjectId, int keeperNpcId) throws Exception {
        return primary().houseTaxQuote(characterObjectId, keeperNpcId);
    }

    @Override public RepositoryHouseTaxPayment persistHouseTaxPayment(
            long characterObjectId, RepositoryHouseTaxQuote expected,
            long nowEpochSeconds, int taxIntervalDays, long taxAdena,
            RuntimeInventory beforeInventory, RuntimeInventory afterInventory,
            Map<Long, Long> localToSourceObjectIds) throws Exception {
        return primary().persistHouseTaxPayment(
                characterObjectId, expected, nowEpochSeconds,
                taxIntervalDays, taxAdena, beforeInventory, afterInventory,
                localToSourceObjectIds);
    }

    @Override public RepositoryHotelSnapshot hotelState() throws Exception {
        return primary().hotelState();
    }

    @Override public SourceEquippedFireResistance equippedFireResistance(
            long characterObjectId) throws Exception {
        return primary().equippedFireResistance(characterObjectId);
    }

    @Override public SourceBaseMagicResistance baseMagicResistance(
            long characterObjectId) throws Exception {
        return primary().baseMagicResistance(characterObjectId);
    }

    @Override public SourceEquipmentMagicStats equipmentMagicStats(
            long characterObjectId) throws Exception {
        return primary().equipmentMagicStats(characterObjectId);
    }

    @Override public Set<Integer> counterMagicBypassSkills() throws Exception {
        return primary().counterMagicBypassSkills();
    }

    @Override public SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId) throws Exception {
        return primary().equipmentEvaluation(characterObjectId);
    }

    @Override public SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId, Map<Long, Boolean> equippedOverrides) throws Exception {
        return primary().equipmentEvaluation(characterObjectId, equippedOverrides);
    }

    @Override public SourcePhysicalGrowthStats physicalGrowthStats() throws Exception {
        return primary().physicalGrowthStats();
    }

    @Override public SourceNpcPhysicalStats npcPhysicalStats(int npcId) throws Exception {
        return primary().npcPhysicalStats(npcId);
    }

    @Override public SourceSummonTemplate summonTemplate(int npcId) throws Exception {
        return primary().summonTemplate(npcId);
    }

    @Override public List<SourcePet> pets(long characterObjectId) throws Exception {
        return primary().pets(characterObjectId);
    }

    @Override public SourcePetTemplate petTemplate(int npcId) throws Exception {
        return primary().petTemplate(npcId);
    }

    @Override public SourceInventoryWeightStats inventoryWeightStats(
            long characterObjectId) throws Exception {
        return primary().inventoryWeightStats(characterObjectId);
    }

    @Override public ClanEmblemData clanEmblem(String clanName) throws Exception {
        return primary().clanEmblem(clanName);
    }

    @Override public CastleSummary castleForClan(String clanName) throws Exception {
        return primary().castleForClan(clanName);
    }

    @Override public CastleSummary castleById(int castleId) throws Exception {
        return primary().castleById(castleId);
    }

    @Override public List<CastleWarSchedule> castleWarSchedules() throws Exception {
        return primary().castleWarSchedules();
    }

    @Override public RepositoryCastleSnapshot castleState(int castleId) throws Exception {
        return primary().castleState(castleId);
    }

    @Override public RepositoryCharacterTitle characterTitle(long characterObjectId)
            throws Exception {
        return primary().characterTitle(characterObjectId);
    }

    @Override public RepositoryTitleMutation persistCharacterTitle(
            long characterObjectId, String expected, String replacement) throws Exception {
        return primary().persistCharacterTitle(characterObjectId, expected, replacement);
    }

    @Override public RepositoryMarriageState marriage(long characterObjectId)
            throws Exception {
        return primary().marriage(characterObjectId);
    }

    @Override public RepositoryMarriageMutation persistMarriage(
            long firstCharacterObjectId, long secondCharacterObjectId) throws Exception {
        return primary().persistMarriage(firstCharacterObjectId, secondCharacterObjectId);
    }

    @Override public RepositoryMarriageMutation persistDivorce(
            long characterObjectId, long expectedPartnerObjectId) throws Exception {
        return primary().persistDivorce(characterObjectId, expectedPartnerObjectId);
    }

    @Override public RepositoryCastleMutation persistCastleState(
            long ownerCharacterObjectId, RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement) throws Exception {
        return primary().persistCastleState(
                ownerCharacterObjectId, expected, replacement);
    }

    @Override public RepositoryCastleMutation persistCastleWarEnd(
            RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement) throws Exception {
        return primary().persistCastleWarEnd(expected, replacement);
    }

    @Override public RepositoryCastleSecuritySettlement prepareCastleSecurity(
            int castleId, long warStartMillis, long chargeAdena) throws Exception {
        return primary().prepareCastleSecurity(
                castleId, warStartMillis, chargeAdena);
    }

    @Override public RepositoryCastleSecuritySettlement completeCastleSecurity(
            int castleId, long warStartMillis) throws Exception {
        return primary().completeCastleSecurity(castleId, warStartMillis);
    }

    @Override public RepositoryCastleOwnershipStatus persistCastleOwnership(
            long winnerLeaderObjectId, int castleId) throws Exception {
        return primary().persistCastleOwnership(winnerLeaderObjectId, castleId);
    }

    @Override public RepositoryCastleTreasuryMutation persistCastleTreasuryTransfer(
            long ownerCharacterObjectId, RepositoryCastleSnapshot expected,
            RepositoryCastleSnapshot replacement, RuntimeInventory beforeInventory,
            RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds)
            throws Exception {
        return primary().persistCastleTreasuryTransfer(ownerCharacterObjectId,
                expected, replacement, beforeInventory, afterInventory,
                localToSourceObjectIds);
    }

    @Override public boolean characterExists(String characterName) throws Exception {
        return primary().characterExists(characterName);
    }

    @Override public String description() {
        return routes.values().stream()
                .map(route -> route.name() + "=" + route.repository().description())
                .collect(java.util.stream.Collectors.joining(", ", "source_router(", ")"));
    }

    @Override public void close() throws Exception {
        Exception closeFailure = null;
        Set<AccountRepository> closed = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        for (Route route : routes.values()) {
            if (!closed.add(route.repository())) continue;
            try {
                route.repository().close();
            } catch (Exception failure) {
                if (closeFailure == null) closeFailure = failure;
                else closeFailure.addSuppressed(failure);
            }
        }
        if (closeFailure != null) throw closeFailure;
    }

    private AccountRepository primary() {
        return routes.values().iterator().next().repository();
    }

    private static void compensateAccountCreation(String account,
            List<AccountRepository> created, Exception original) throws Exception {
        Exception cleanupFailure = null;
        for (int index = created.size() - 1; index >= 0; index--) {
            try {
                if (!created.get(index).deleteLitoAccount(account)) {
                    throw new SQLException("LiTo account creation compensation was refused");
                }
            } catch (Exception failure) {
                if (cleanupFailure == null) cleanupFailure = failure;
                else cleanupFailure.addSuppressed(failure);
            }
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else throw cleanupFailure;
        }
    }

    private static void compensatePasswordChange(String account, String oldPassword,
            String newPassword, List<AccountRepository> changed, Exception original)
            throws Exception {
        Exception cleanupFailure = null;
        for (int index = changed.size() - 1; index >= 0; index--) {
            try {
                AccountMutationResult result = changed.get(index)
                        .changePassword(account, newPassword, oldPassword);
                if (result != AccountMutationResult.SUCCESS) {
                    throw new SQLException("LiTo password compensation failed: " + result);
                }
            } catch (Exception failure) {
                if (cleanupFailure == null) cleanupFailure = failure;
                else cleanupFailure.addSuppressed(failure);
            }
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else throw cleanupFailure;
        }
    }

    private static String key(String source) {
        return source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
    }
}

enum AccountMutationResult {
    SUCCESS,
    EXISTS,
    NOT_FOUND,
    WRONG_PASSWORD,
    READ_ONLY
}

enum RepositoryCharacterMutationStatus {
    SUCCESS,
    EXISTS,
    ACCOUNT_LIMIT,
    NOT_FOUND,
    NOT_OWNED,
    CLAN_OWNER,
    READ_ONLY
}

record RepositoryCharacterMutation(
        RepositoryCharacterMutationStatus status, CharacterSummary character) {
    static RepositoryCharacterMutation readOnly() {
        return new RepositoryCharacterMutation(
                RepositoryCharacterMutationStatus.READ_ONLY, null);
    }
}

enum RepositoryPersistenceResult { SUCCESS, CONFLICT, NOT_FOUND, READ_ONLY }

record RepositoryPendingReward(
        long sequence, String account, InventoryCatalog.Definition definition,
        int count, int enchant) {
    RepositoryPendingReward {
        account = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        if (sequence <= 0 || sequence > Integer.MAX_VALUE || account.isBlank()
                || definition == null || count <= 0
                || enchant < -128 || enchant > 127
                || (enchant != 0 && !definition.weapon() && !definition.armor())) {
            throw new IllegalArgumentException("invalid pending reward row");
        }
    }

    int itemId() {
        return Math.toIntExact(definition.catalogId());
    }
}

record RepositoryPendingRewardSnapshot(
        boolean authoritative, List<RepositoryPendingReward> rewards) {
    RepositoryPendingRewardSnapshot {
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
        if (!authoritative && !rewards.isEmpty()) {
            throw new IllegalArgumentException("unavailable pending rewards contain rows");
        }
    }

    static RepositoryPendingRewardSnapshot unavailable() {
        return new RepositoryPendingRewardSnapshot(false, List.of());
    }
}

enum RepositoryPendingRewardClaimStatus {
    SUCCESS, CONFLICT, NOT_FOUND, READ_ONLY, UNSUPPORTED_ITEM
}

record RepositoryPendingRewardClaim(
        RepositoryPendingRewardClaimStatus status,
        RepositoryPendingReward reward, boolean personalWarehouse) {
    RepositoryPendingRewardClaim {
        if (status == null) throw new IllegalArgumentException("reward claim status is required");
    }

    static RepositoryPendingRewardClaim unavailable(RepositoryPendingReward reward) {
        return new RepositoryPendingRewardClaim(
                RepositoryPendingRewardClaimStatus.READ_ONLY, reward, false);
    }
}

record RepositoryPrivateInventorySnapshot(
        boolean authoritative, long snapshotVersion,
        List<LitoOwnedInventoryItem> items) {
    RepositoryPrivateInventorySnapshot {
        if (snapshotVersion < 0 || snapshotVersion > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid private inventory version");
        }
        Map<Long, LitoOwnedInventoryItem> normalized = new java.util.TreeMap<>();
        for (LitoOwnedInventoryItem item : items == null ? List.<LitoOwnedInventoryItem>of()
                : items) {
            if (item == null || normalized.putIfAbsent(item.objectId(), item) != null) {
                throw new IllegalArgumentException(
                        "duplicate/null private inventory object");
            }
        }
        items = List.copyOf(normalized.values());
        if (!authoritative && (snapshotVersion != 0 || !items.isEmpty())) {
            throw new IllegalArgumentException(
                    "unavailable private inventory contains state");
        }
    }

    static RepositoryPrivateInventorySnapshot unavailable() {
        return new RepositoryPrivateInventorySnapshot(false, 0, List.of());
    }

    static RepositoryPrivateInventorySnapshot empty() {
        return new RepositoryPrivateInventorySnapshot(true, 0, List.of());
    }
}

enum RepositoryWarehouseTransferResult {
    SUCCESS, CONFLICT, NOT_FOUND, READ_ONLY, UNSUPPORTED_ITEM
}

enum RepositoryBookmarkPersistenceResult {
    SUCCESS, CONFLICT, NOT_FOUND, READ_ONLY
}

enum RepositoryBuffPersistenceStatus {
    SUCCESS, CONFLICT, NOT_FOUND, READ_ONLY
}

record RepositoryBuff(int skillId, int remainingSeconds, int polyId) {
    RepositoryBuff {
        if (skillId <= 0 || remainingSeconds == 0) {
            throw new IllegalArgumentException("invalid repository buff");
        }
    }
}

record RepositoryBuffSnapshot(boolean authoritative, long completedVersion,
                              List<RepositoryBuff> buffs) {
    RepositoryBuffSnapshot {
        if (completedVersion < 0) {
            throw new IllegalArgumentException("invalid repository buff version");
        }
        buffs = normalize(buffs);
    }
    static RepositoryBuffSnapshot unavailable() {
        return new RepositoryBuffSnapshot(false, 0, List.of());
    }
    private static List<RepositoryBuff> normalize(List<RepositoryBuff> source) {
        List<RepositoryBuff> rows = source == null ? List.of() : source;
        if (rows.size() > 512) {
            throw new IllegalArgumentException("repository buff snapshot exceeds 512 rows");
        }
        Map<Integer, RepositoryBuff> unique = new java.util.TreeMap<>();
        for (RepositoryBuff row : rows) {
            if (row == null || unique.putIfAbsent(row.skillId(), row) != null) {
                throw new IllegalArgumentException("duplicate or null repository buff row");
            }
        }
        return List.copyOf(unique.values());
    }
}

record RepositoryBuffPersistence(RepositoryBuffPersistenceStatus status,
                                 RepositoryBuffSnapshot snapshot) {
    RepositoryBuffPersistence {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("buff persistence result is incomplete");
        }
    }
    static RepositoryBuffPersistence readOnly(RepositoryBuffSnapshot expected) {
        return new RepositoryBuffPersistence(RepositoryBuffPersistenceStatus.READ_ONLY,
                expected == null ? RepositoryBuffSnapshot.unavailable() : expected);
    }
}

enum RepositoryBuddyPersistenceStatus {
    SUCCESS, CONFLICT, NOT_FOUND, READ_ONLY
}

record RepositoryBuddyGroup(int id, String name) {
    RepositoryBuddyGroup {
        name = name == null ? "" : name.trim();
        if (id < 1 || id > 255 || name.isBlank() || name.length() > 45) {
            throw new IllegalArgumentException("invalid repository buddy group");
        }
    }
}

record RepositoryBuddy(String name, long buddyObjectId, String targetSource,
                       int groupId, int relationshipStyle, int paletteOffset,
                       String origin) {
    RepositoryBuddy {
        name = name == null ? "" : name.trim();
        targetSource = targetSource == null ? "" : targetSource.trim();
        origin = origin == null ? "" : origin.trim();
        if (name.isBlank() || name.length() > 45 || buddyObjectId < 0
                || buddyObjectId > 0xFFFF_FFFFL || groupId < 1 || groupId > 255
                || relationshipStyle < 0 || relationshipStyle > 4
                || paletteOffset < 0 || paletteOffset > 0xC2
                || targetSource.length() > 45 || origin.length() > 45) {
            throw new IllegalArgumentException("invalid repository buddy");
        }
    }
}

record RepositoryBuddySnapshot(boolean authoritative,
                               List<RepositoryBuddyGroup> groups,
                               List<RepositoryBuddy> buddies) {
    RepositoryBuddySnapshot {
        groups = normalizeGroups(groups);
        buddies = normalizeBuddies(buddies, groups);
    }
    static RepositoryBuddySnapshot unavailable() {
        return new RepositoryBuddySnapshot(false, List.of(), List.of());
    }
    private static List<RepositoryBuddyGroup> normalizeGroups(
            List<RepositoryBuddyGroup> source) {
        List<RepositoryBuddyGroup> rows = source == null ? List.of() : source;
        Map<Integer, RepositoryBuddyGroup> ids = new java.util.TreeMap<>();
        Set<String> names = new LinkedHashSet<>();
        for (RepositoryBuddyGroup row : rows) {
            String name = row == null ? "" : row.name().toLowerCase(Locale.ROOT);
            if (row == null || ids.putIfAbsent(row.id(), row) != null
                    || !names.add(name)) {
                throw new IllegalArgumentException("duplicate or null buddy group");
            }
        }
        if (!ids.isEmpty() && !ids.containsKey(1)) {
            throw new IllegalArgumentException("buddy group 1 is required");
        }
        return List.copyOf(ids.values());
    }
    private static List<RepositoryBuddy> normalizeBuddies(
            List<RepositoryBuddy> source, List<RepositoryBuddyGroup> groups) {
        List<RepositoryBuddy> rows = source == null ? List.of() : source;
        if (rows.size() > 255) {
            throw new IllegalArgumentException("repository buddy snapshot exceeds 255 rows");
        }
        Set<Integer> groupIds = groups.stream()
                .map(RepositoryBuddyGroup::id).collect(java.util.stream.Collectors.toSet());
        Map<String, RepositoryBuddy> names = new java.util.TreeMap<>();
        for (RepositoryBuddy row : rows) {
            String name = row == null ? "" : row.name().toLowerCase(Locale.ROOT);
            if (row == null || !groupIds.contains(row.groupId())
                    || names.putIfAbsent(name, row) != null) {
                throw new IllegalArgumentException("invalid or duplicate repository buddy");
            }
        }
        return List.copyOf(names.values());
    }
}

record RepositoryBuddyPersistence(RepositoryBuddyPersistenceStatus status,
                                  RepositoryBuddySnapshot snapshot) {
    RepositoryBuddyPersistence {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("buddy persistence result is incomplete");
        }
    }
    static RepositoryBuddyPersistence readOnly(RepositoryBuddySnapshot expected) {
        return new RepositoryBuddyPersistence(RepositoryBuddyPersistenceStatus.READ_ONLY,
                expected == null ? RepositoryBuddySnapshot.unavailable() : expected);
    }
}

record RepositoryLetter(long itemObjectId, int code, String sender, String receiver,
                        String date, int templateId, String subject, String content,
                        boolean opened) {
    RepositoryLetter {
        GameStateStore.Letter validated = new GameStateStore.Letter(
                itemObjectId, code, sender, receiver, date, templateId,
                subject, content, opened);
        sender = validated.sender();
        receiver = validated.receiver();
        date = validated.date();
        subject = validated.subject();
        content = validated.content();
    }

    GameStateStore.Letter gameStateLetter() {
        return new GameStateStore.Letter(itemObjectId, code, sender, receiver, date,
                templateId, subject, content, opened);
    }
}

record RepositoryLetterSnapshot(boolean authoritative, List<RepositoryLetter> letters) {
    RepositoryLetterSnapshot {
        letters = letters == null ? List.of() : letters.stream()
                .sorted(java.util.Comparator.comparingLong(RepositoryLetter::itemObjectId))
                .toList();
        Set<Long> identities = new LinkedHashSet<>();
        if (letters.stream().anyMatch(letter -> letter == null
                || !identities.add(letter.itemObjectId()))) {
            throw new IllegalArgumentException("duplicate/null repository physical letter");
        }
    }

    static RepositoryLetterSnapshot unavailable() {
        return new RepositoryLetterSnapshot(false, List.of());
    }
}

enum RepositoryLetterDeliveryStatus {
    DELIVERED, NO_PAPER, RECIPIENT_NOT_FOUND, RECIPIENT_FULL, CONFLICT, READ_ONLY
}

record RepositoryLetterDelivery(RepositoryLetterDeliveryStatus status,
                                RepositoryLetter letter) {
    boolean delivered() {
        return status == RepositoryLetterDeliveryStatus.DELIVERED && letter != null;
    }
}

enum RepositoryLetterMutationStatus {
    SUCCESS, ALREADY_OPEN, NOT_FOUND, CONFLICT, READ_ONLY
}

record RepositoryLetterMutation(RepositoryLetterMutationStatus status,
                                 RepositoryLetter letter) {
    static RepositoryLetterMutation readOnly(RepositoryLetter expected) {
        return new RepositoryLetterMutation(RepositoryLetterMutationStatus.READ_ONLY, expected);
    }
}

record RepositoryBoardSnapshot(boolean authoritative,
                               List<GameStateStore.BoardTopic> topics) {
    RepositoryBoardSnapshot {
        topics = topics == null ? List.of() : List.copyOf(topics);
        Set<Long> identities = new LinkedHashSet<>();
        if ((!authoritative && !topics.isEmpty()) || topics.stream().anyMatch(topic ->
                topic == null || !identities.add(topic.id()))) {
            throw new IllegalArgumentException("invalid repository board snapshot");
        }
    }

    static RepositoryBoardSnapshot unavailable() {
        return new RepositoryBoardSnapshot(false, List.of());
    }
}

record RepositoryBoardLookup(boolean authoritative, GameStateStore.BoardTopic topic) {
    RepositoryBoardLookup {
        if (!authoritative && topic != null) {
            throw new IllegalArgumentException("unavailable board lookup contains a topic");
        }
    }

    static RepositoryBoardLookup unavailable() {
        return new RepositoryBoardLookup(false, null);
    }

    static RepositoryBoardLookup missing() {
        return new RepositoryBoardLookup(true, null);
    }
}

enum RepositoryBoardMutationStatus {
    SUCCESS, NOT_FOUND, NOT_AUTHOR, CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryBoardMutation(RepositoryBoardMutationStatus status,
                               GameStateStore.BoardTopic topic) {
    RepositoryBoardMutation {
        if (status == null || (status == RepositoryBoardMutationStatus.SUCCESS
                && topic == null)) {
            throw new IllegalArgumentException("invalid repository board mutation");
        }
    }

    static RepositoryBoardMutation unavailable() {
        return new RepositoryBoardMutation(RepositoryBoardMutationStatus.UNAVAILABLE, null);
    }

    static RepositoryBoardMutation readOnly(GameStateStore.BoardTopic topic) {
        return new RepositoryBoardMutation(RepositoryBoardMutationStatus.READ_ONLY, topic);
    }
}

record RepositoryTournamentProfile(long characterObjectId, String characterName,
                                   long snapshotVersion,
                                   GameStateStore.TournamentProfileState state) {
    RepositoryTournamentProfile {
        characterName = characterName == null ? "" : characterName.trim();
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL
                || characterName.isBlank() || characterName.length() > 45
                || snapshotVersion < 0 || snapshotVersion > 0xFFFF_FFFFL
                || state == null) {
            throw new IllegalArgumentException("invalid repository Tournament profile");
        }
    }
}

record RepositoryTournamentRoster(boolean authoritative,
                                  List<RepositoryTournamentProfile> profiles) {
    RepositoryTournamentRoster {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
        if (!authoritative && !profiles.isEmpty()) {
            throw new IllegalArgumentException("unavailable Tournament roster contains rows");
        }
    }

    static RepositoryTournamentRoster unavailable() {
        return new RepositoryTournamentRoster(false, List.of());
    }
}

enum RepositoryTournamentMutationStatus {
    SUCCESS, NOT_FOUND, CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryTournamentMutation(RepositoryTournamentMutationStatus status,
                                    RepositoryTournamentProfile profile) {
    RepositoryTournamentMutation {
        if (status == null || status == RepositoryTournamentMutationStatus.SUCCESS
                && profile == null) {
            throw new IllegalArgumentException("invalid repository Tournament mutation");
        }
    }

    static RepositoryTournamentMutation unavailable() {
        return new RepositoryTournamentMutation(
                RepositoryTournamentMutationStatus.UNAVAILABLE, null);
    }
}

record RepositoryClanMember(long objectId, String name, int role, int nativeRank,
                            String title, String note) {
    RepositoryClanMember {
        name = name == null ? "" : name.trim();
        title = title == null ? "" : title;
        note = note == null ? "" : note;
        if (objectId <= 0 || objectId > 0xFFFF_FFFFL || name.isBlank()
                || name.length() > 45 || role < 1 || role > 4
                || nativeRank < 1 || nativeRank > 10
                || title.length() > 35 || note.length() > 62) {
            throw new IllegalArgumentException("invalid repository clan member");
        }
    }
}

record RepositoryCharacterTitle(boolean authoritative, boolean present, String title) {
    RepositoryCharacterTitle {
        title = title == null ? "" : title;
        if ((!authoritative && (present || !title.isEmpty()))
                || (present && title.length() > 35)) {
            throw new IllegalArgumentException("invalid repository character title");
        }
    }

    static RepositoryCharacterTitle unavailable() {
        return new RepositoryCharacterTitle(false, false, "");
    }

    static RepositoryCharacterTitle missing() {
        return new RepositoryCharacterTitle(true, false, "");
    }

    static RepositoryCharacterTitle present(String title) {
        return new RepositoryCharacterTitle(true, true, title);
    }
}

enum RepositoryTitleMutationStatus {
    SUCCESS, NOT_FOUND, CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryTitleMutation(RepositoryTitleMutationStatus status,
                               RepositoryCharacterTitle title) {
    RepositoryTitleMutation {
        if (status == null || title == null) {
            throw new IllegalArgumentException("character-title mutation is incomplete");
        }
    }

    static RepositoryTitleMutation unavailable() {
        return new RepositoryTitleMutation(RepositoryTitleMutationStatus.UNAVAILABLE,
                RepositoryCharacterTitle.unavailable());
    }

    static RepositoryTitleMutation readOnly(RepositoryCharacterTitle title) {
        return new RepositoryTitleMutation(RepositoryTitleMutationStatus.READ_ONLY,
                title == null ? RepositoryCharacterTitle.unavailable() : title);
    }
}

enum RepositoryMarriageStateStatus { AVAILABLE, NOT_FOUND, CONFLICT, UNAVAILABLE }

record RepositoryMarriageState(RepositoryMarriageStateStatus status,
                               long characterObjectId, String characterName,
                               long partnerObjectId, String partnerName) {
    RepositoryMarriageState {
        characterName = characterName == null ? "" : characterName.trim();
        partnerName = partnerName == null ? "" : partnerName.trim();
        if (status == null || characterObjectId < 0 || partnerObjectId < 0
                || characterObjectId > Integer.MAX_VALUE
                || partnerObjectId > Integer.MAX_VALUE
                || characterName.length() > 45 || partnerName.length() > 45
                || (status == RepositoryMarriageStateStatus.AVAILABLE
                        && (characterObjectId == 0 || characterName.isBlank()
                                || (partnerObjectId == 0) != partnerName.isBlank()))
                || (status == RepositoryMarriageStateStatus.NOT_FOUND
                        && (characterObjectId != 0 || !characterName.isEmpty()
                                || partnerObjectId != 0 || !partnerName.isEmpty()))
                || (status == RepositoryMarriageStateStatus.UNAVAILABLE
                        && (characterObjectId != 0 || !characterName.isEmpty()
                                || partnerObjectId != 0 || !partnerName.isEmpty()))) {
            throw new IllegalArgumentException("invalid repository marriage state");
        }
    }

    boolean authoritative() {
        return status != RepositoryMarriageStateStatus.UNAVAILABLE;
    }

    boolean consistent() {
        return status == RepositoryMarriageStateStatus.AVAILABLE;
    }

    boolean married() {
        return consistent() && partnerObjectId != 0;
    }

    static RepositoryMarriageState unavailable() {
        return new RepositoryMarriageState(
                RepositoryMarriageStateStatus.UNAVAILABLE, 0, "", 0, "");
    }

    static RepositoryMarriageState notFound() {
        return new RepositoryMarriageState(
                RepositoryMarriageStateStatus.NOT_FOUND, 0, "", 0, "");
    }

    static RepositoryMarriageState available(
            long characterObjectId, String characterName,
            long partnerObjectId, String partnerName) {
        return new RepositoryMarriageState(RepositoryMarriageStateStatus.AVAILABLE,
                characterObjectId, characterName, partnerObjectId, partnerName);
    }

    static RepositoryMarriageState conflict(
            long characterObjectId, String characterName,
            long partnerObjectId, String partnerName) {
        return new RepositoryMarriageState(RepositoryMarriageStateStatus.CONFLICT,
                characterObjectId, characterName, partnerObjectId, partnerName);
    }
}

enum RepositoryMarriageMutationStatus {
    SUCCESS, NOT_FOUND, ALREADY_MARRIED, STALE, INVALID, READ_ONLY, UNAVAILABLE
}

record RepositoryMarriageMutation(RepositoryMarriageMutationStatus status,
                                  RepositoryMarriageState first,
                                  RepositoryMarriageState second) {
    RepositoryMarriageMutation {
        if (status == null || first == null || second == null) {
            throw new IllegalArgumentException("repository marriage mutation is incomplete");
        }
    }

    boolean succeeded() {
        return status == RepositoryMarriageMutationStatus.SUCCESS;
    }

    static RepositoryMarriageMutation unavailable() {
        return new RepositoryMarriageMutation(
                RepositoryMarriageMutationStatus.UNAVAILABLE,
                RepositoryMarriageState.unavailable(),
                RepositoryMarriageState.unavailable());
    }

    static RepositoryMarriageMutation invalid() {
        return new RepositoryMarriageMutation(
                RepositoryMarriageMutationStatus.INVALID,
                RepositoryMarriageState.unavailable(),
                RepositoryMarriageState.unavailable());
    }
}

record RepositoryClanSnapshot(boolean authoritative, long clanId, String name,
                              long leaderObjectId, String leaderName,
                              int castleId, int houseId, int maximumOnlineUsers,
                              boolean showEmblem, String announcement,
                              long birthEpochSeconds, boolean destroyPending,
                              List<RepositoryClanMember> members) {
    RepositoryClanSnapshot {
        name = name == null ? "" : name.trim();
        leaderName = leaderName == null ? "" : leaderName.trim();
        announcement = announcement == null ? "" : announcement;
        members = normalizeClanMembers(members);
        boolean empty = clanId == 0;
        if (clanId < 0 || clanId > 0xFFFF_FFFFL || leaderObjectId < 0
                || leaderObjectId > 0xFFFF_FFFFL || castleId < 0 || houseId < 0
                || maximumOnlineUsers < 0 || birthEpochSeconds < 0
                || (empty && (!name.isEmpty() || leaderObjectId != 0
                        || !leaderName.isEmpty() || !members.isEmpty()))
                || (!empty && (name.isBlank() || name.length() > 45
                        || leaderObjectId == 0 || leaderName.isBlank()
                        || leaderName.length() > 45))) {
            throw new IllegalArgumentException("invalid repository clan snapshot");
        }
        if (!empty) {
            long owners = members.stream().filter(member -> member.role() == 4).count();
            String canonicalLeaderName = leaderName;
            boolean exactLeader = members.stream().anyMatch(member ->
                    member.objectId() == leaderObjectId
                            && member.name().equalsIgnoreCase(canonicalLeaderName)
                            && member.role() == 4);
            if (owners != 1 || !exactLeader) {
                throw new IllegalArgumentException("repository clan leader/member mismatch");
            }
        }
    }

    static RepositoryClanSnapshot unavailable() {
        return new RepositoryClanSnapshot(false, 0, "", 0, "", 0, 0,
                0, false, "", 0, false, List.of());
    }

    static RepositoryClanSnapshot empty() {
        return new RepositoryClanSnapshot(true, 0, "", 0, "", 0, 0,
                0, false, "", 0, false, List.of());
    }

    boolean present() {
        return authoritative && clanId != 0;
    }

    private static List<RepositoryClanMember> normalizeClanMembers(
            List<RepositoryClanMember> source) {
        List<RepositoryClanMember> rows = source == null ? List.of() : source;
        if (rows.size() > 1024) {
            throw new IllegalArgumentException("repository clan exceeds 1024 members");
        }
        Map<Long, RepositoryClanMember> identities = new java.util.TreeMap<>();
        Set<String> names = new LinkedHashSet<>();
        for (RepositoryClanMember row : rows) {
            String key = row == null ? "" : row.name().toLowerCase(Locale.ROOT);
            if (row == null || identities.putIfAbsent(row.objectId(), row) != null
                    || !names.add(key)) {
                throw new IllegalArgumentException("duplicate/null repository clan member");
            }
        }
        return List.copyOf(identities.values());
    }
}

enum RepositoryClanMutationStatus {
    SUCCESS, OWNER_ALREADY_MEMBER, DUPLICATE_NAME, NOT_FOUND, NOT_OWNER,
    APPLICANT_ALREADY_MEMBER, FULL, CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryClanMutation(RepositoryClanMutationStatus status,
                              RepositoryClanSnapshot snapshot) {
    RepositoryClanMutation {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("clan mutation result is incomplete");
        }
    }

    static RepositoryClanMutation unavailable() {
        return new RepositoryClanMutation(
                RepositoryClanMutationStatus.UNAVAILABLE,
                RepositoryClanSnapshot.unavailable());
    }

    static RepositoryClanMutation readOnly(RepositoryClanSnapshot snapshot) {
        return new RepositoryClanMutation(RepositoryClanMutationStatus.READ_ONLY,
                snapshot == null ? RepositoryClanSnapshot.unavailable() : snapshot);
    }
}

record RepositoryCastleSnapshot(boolean authoritative, int castleId, int taxRate,
                                long publicMoney, long warTimeMillis,
                                boolean securityEnabled,
                                GameStateStore.MercenaryRoster mercenaries,
                                long snapshotVersion) {
    RepositoryCastleSnapshot {
        if (castleId < 0 || castleId > 8 || taxRate < 0 || taxRate > 50
                || publicMoney < 0 || publicMoney > Integer.MAX_VALUE
                || warTimeMillis < 0 || mercenaries == null
                || snapshotVersion < 0
                || (authoritative && castleId == 0)
                || (!authoritative && (castleId != 0 || taxRate != 0
                        || publicMoney != 0 || warTimeMillis != 0
                        || securityEnabled || mercenaries.total() != 0
                        || snapshotVersion != 0))) {
            throw new IllegalArgumentException("invalid repository castle snapshot");
        }
        if (authoritative && taxRate < 10) {
            throw new IllegalArgumentException("repository castle tax is outside 10..50");
        }
    }

    static RepositoryCastleSnapshot unavailable() {
        return new RepositoryCastleSnapshot(false, 0, 0, 0, 0, false,
                new GameStateStore.MercenaryRoster(0, 0, 0, 0), 0);
    }
}

enum RepositoryCastleMutationStatus {
    SUCCESS, NOT_FOUND, NOT_OWNER, CONFLICT, READ_ONLY, UNAVAILABLE
}

enum RepositoryCastleOwnershipStatus {
    SUCCESS, NOT_FOUND, NOT_OWNER, ALREADY_OWNS_CASTLE, CONFLICT,
    READ_ONLY, UNAVAILABLE
}

record RepositoryCastleMutation(RepositoryCastleMutationStatus status,
                                RepositoryCastleSnapshot snapshot) {
    RepositoryCastleMutation {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("castle mutation result is incomplete");
        }
    }

    static RepositoryCastleMutation unavailable() {
        return new RepositoryCastleMutation(
                RepositoryCastleMutationStatus.UNAVAILABLE,
                RepositoryCastleSnapshot.unavailable());
    }

    static RepositoryCastleMutation readOnly(RepositoryCastleSnapshot snapshot) {
        return new RepositoryCastleMutation(RepositoryCastleMutationStatus.READ_ONLY,
                snapshot == null ? RepositoryCastleSnapshot.unavailable() : snapshot);
    }
}

enum RepositoryCastleSecuritySettlementStatus {
    READY, INSUFFICIENT_FUNDS, ALREADY_SETTLED, DISABLED, NOT_FOUND,
    CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryCastleSecuritySettlement(
        RepositoryCastleSecuritySettlementStatus status,
        RepositoryCastleSnapshot snapshot, long warStartMillis,
        long chargedAdena) {
    RepositoryCastleSecuritySettlement {
        if (status == null || snapshot == null || warStartMillis < 0
                || chargedAdena < 0 || chargedAdena > Integer.MAX_VALUE
                || (status == RepositoryCastleSecuritySettlementStatus.READY
                        && (warStartMillis == 0 || chargedAdena == 0))) {
            throw new IllegalArgumentException(
                    "castle security settlement result is incomplete");
        }
    }

    static RepositoryCastleSecuritySettlement unavailable() {
        return new RepositoryCastleSecuritySettlement(
                RepositoryCastleSecuritySettlementStatus.UNAVAILABLE,
                RepositoryCastleSnapshot.unavailable(), 0, 0);
    }

    static RepositoryCastleSecuritySettlement readOnly(
            RepositoryCastleSnapshot snapshot, long warStartMillis) {
        return new RepositoryCastleSecuritySettlement(
                RepositoryCastleSecuritySettlementStatus.READ_ONLY,
                snapshot == null ? RepositoryCastleSnapshot.unavailable() : snapshot,
                Math.max(0, warStartMillis), 0);
    }
}

enum RepositoryCastleTreasuryStatus {
    SUCCESS, NOT_FOUND, NOT_OWNER, CONFLICT, READ_ONLY, UNAVAILABLE,
    UNSUPPORTED_ITEM
}

record RepositoryCastleTreasuryMutation(
        RepositoryCastleTreasuryStatus status,
        RepositoryCastleSnapshot snapshot) {
    RepositoryCastleTreasuryMutation {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException(
                    "castle treasury mutation result is incomplete");
        }
    }

    static RepositoryCastleTreasuryMutation unavailable() {
        return new RepositoryCastleTreasuryMutation(
                RepositoryCastleTreasuryStatus.UNAVAILABLE,
                RepositoryCastleSnapshot.unavailable());
    }

    static RepositoryCastleTreasuryMutation readOnly(
            RepositoryCastleSnapshot snapshot) {
        return new RepositoryCastleTreasuryMutation(
                RepositoryCastleTreasuryStatus.READ_ONLY,
                snapshot == null ? RepositoryCastleSnapshot.unavailable() : snapshot);
    }
}

record RepositoryHouseAuctionSnapshot(
        boolean authoritative, List<GameStateStore.HouseAuction> auctions) {
    RepositoryHouseAuctionSnapshot {
        auctions = auctions == null ? List.of() : List.copyOf(auctions);
        if (!authoritative && !auctions.isEmpty()) {
            throw new IllegalArgumentException("unavailable house auction has rows");
        }
    }

    static RepositoryHouseAuctionSnapshot unavailable() {
        return new RepositoryHouseAuctionSnapshot(false, List.of());
    }

    GameStateStore.HouseAuction byId(long houseId) {
        return auctions.stream().filter(row -> row.houseId() == houseId)
                .findFirst().orElse(null);
    }
}

enum RepositoryHouseBidStatus {
    ACCEPTED, NOT_FOUND, STALE, EXPIRED, ALREADY_BIDDER,
    INVALID_AMOUNT, INVENTORY_CHANGED, REFUND_OVERFLOW,
    READ_ONLY, UNAVAILABLE
}

record RepositoryHouseBidMutation(
        RepositoryHouseBidStatus status, GameStateStore.HouseAuction auction,
        String previousBidder, long previousBidderObjectId, long previousPrice) {
    RepositoryHouseBidMutation {
        if (status == null || previousBidderObjectId < 0 || previousPrice < 0) {
            throw new IllegalArgumentException("invalid repository house bid result");
        }
        previousBidder = previousBidder == null ? "" : previousBidder;
    }

    static RepositoryHouseBidMutation unavailable() {
        return new RepositoryHouseBidMutation(
                RepositoryHouseBidStatus.UNAVAILABLE, null, "", 0, 0);
    }

    boolean accepted() { return status == RepositoryHouseBidStatus.ACCEPTED; }
}

enum RepositoryHouseSettlementStatus {
    TRANSFERRED, CANCELLED, EXTENDED, NOT_FOUND, STALE, NOT_EXPIRED,
    INVALID_OWNER, INVALID_BIDDER, PAYOUT_OVERFLOW, READ_ONLY, UNAVAILABLE
}

record RepositoryHouseSettlement(
        RepositoryHouseSettlementStatus status,
        GameStateStore.HouseAuction auction,
        long oldOwnerObjectId, long payout,
        long bidderObjectId, String bidder) {
    RepositoryHouseSettlement {
        if (status == null || oldOwnerObjectId < 0 || payout < 0
                || bidderObjectId < 0) {
            throw new IllegalArgumentException("invalid repository house settlement");
        }
        bidder = bidder == null ? "" : bidder;
    }

    static RepositoryHouseSettlement unavailable() {
        return new RepositoryHouseSettlement(
                RepositoryHouseSettlementStatus.UNAVAILABLE,
                null, 0, 0, 0, "");
    }

    boolean settled() {
        return status == RepositoryHouseSettlementStatus.TRANSFERRED
                || status == RepositoryHouseSettlementStatus.CANCELLED
                || status == RepositoryHouseSettlementStatus.EXTENDED;
    }
}

record RepositoryHouseTaxState(
        String sourceId, long houseId, String name, int area, String location,
        int onSale, int purchaseBasement, long taxDeadlineEpochSeconds) {
    RepositoryHouseTaxState {
        sourceId = sourceId == null ? "" : sourceId.trim();
        name = name == null ? "" : name;
        location = location == null ? "" : location;
        if (houseId <= 0 || houseId > 0xFFFF_FFFFL
                || name.isBlank() || name.length() > 45
                || area < 0 || location.length() > 45
                || onSale < 0 || purchaseBasement < 0
                || taxDeadlineEpochSeconds < 0) {
            throw new IllegalArgumentException("invalid repository house tax state");
        }
    }
}

record RepositoryHouseTaxSnapshot(
        boolean authoritative, List<RepositoryHouseTaxState> houses) {
    RepositoryHouseTaxSnapshot {
        houses = houses == null ? List.of() : List.copyOf(houses);
        if (!authoritative && !houses.isEmpty()) {
            throw new IllegalArgumentException("unavailable house tax snapshot has rows");
        }
    }

    static RepositoryHouseTaxSnapshot unavailable() {
        return new RepositoryHouseTaxSnapshot(false, List.of());
    }
}

enum RepositoryHouseTaxStatus {
    AUCTIONED, NOT_FOUND, STALE, NOT_EXPIRED, ALREADY_ON_SALE,
    OWNER_CONFLICT, AUCTION_CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryHouseTaxMutation(
        RepositoryHouseTaxStatus status, RepositoryHouseTaxState house,
        GameStateStore.HouseAuction auction, long previousClanId) {
    RepositoryHouseTaxMutation {
        if (status == null || previousClanId < 0) {
            throw new IllegalArgumentException("invalid repository house tax mutation");
        }
    }

    static RepositoryHouseTaxMutation unavailable() {
        return new RepositoryHouseTaxMutation(
                RepositoryHouseTaxStatus.UNAVAILABLE, null, null, 0);
    }

    boolean auctioned() { return status == RepositoryHouseTaxStatus.AUCTIONED; }
}

enum RepositoryHouseTaxQuoteStatus {
    MEMBER, OCCUPIED, VACANT, OWNER_CONFLICT, NOT_FOUND, UNAVAILABLE
}

record RepositoryHouseTaxQuote(
        RepositoryHouseTaxQuoteStatus status, RepositoryHouseTaxState house,
        int keeperNpcId, long ownerClanId, String ownerClanName,
        String ownerLeaderName) {
    RepositoryHouseTaxQuote {
        if (status == null || keeperNpcId < 0 || ownerClanId < 0) {
            throw new IllegalArgumentException("invalid repository house-tax quote");
        }
        ownerClanName = ownerClanName == null ? "" : ownerClanName;
        ownerLeaderName = ownerLeaderName == null ? "" : ownerLeaderName;
        if (status == RepositoryHouseTaxQuoteStatus.UNAVAILABLE
                && (house != null || keeperNpcId != 0 || ownerClanId != 0
                    || !ownerClanName.isEmpty() || !ownerLeaderName.isEmpty())) {
            throw new IllegalArgumentException("unavailable house-tax quote has data");
        }
        if (status != RepositoryHouseTaxQuoteStatus.UNAVAILABLE
                && status != RepositoryHouseTaxQuoteStatus.NOT_FOUND
                && (house == null || keeperNpcId <= 0)) {
            throw new IllegalArgumentException("house-tax quote is missing its house");
        }
        if ((status == RepositoryHouseTaxQuoteStatus.MEMBER
                || status == RepositoryHouseTaxQuoteStatus.OCCUPIED)
                && (ownerClanId <= 0 || ownerClanName.isBlank()
                    || ownerLeaderName.isBlank())) {
            throw new IllegalArgumentException("occupied house-tax quote has no owner");
        }
    }

    static RepositoryHouseTaxQuote unavailable() {
        return new RepositoryHouseTaxQuote(
                RepositoryHouseTaxQuoteStatus.UNAVAILABLE,
                null, 0, 0, "", "");
    }

    boolean member() { return status == RepositoryHouseTaxQuoteStatus.MEMBER; }
}

enum RepositoryHouseTaxPaymentStatus {
    SUCCESS, NOT_FOUND, NOT_MEMBER, STALE, ON_SALE,
    AUCTION_CONFLICT, INVENTORY_CONFLICT, READ_ONLY, UNAVAILABLE
}

record RepositoryHouseTaxPayment(
        RepositoryHouseTaxPaymentStatus status, RepositoryHouseTaxQuote quote) {
    RepositoryHouseTaxPayment {
        if (status == null) {
            throw new IllegalArgumentException("invalid repository house-tax payment");
        }
    }

    static RepositoryHouseTaxPayment unavailable() {
        return new RepositoryHouseTaxPayment(
                RepositoryHouseTaxPaymentStatus.UNAVAILABLE, null);
    }

    boolean paid() { return status == RepositoryHouseTaxPaymentStatus.SUCCESS; }
}

record RepositoryBookmark(String name, int x, int y, int mapId) {
    RepositoryBookmark {
        name = name == null ? "" : name.trim();
        if (name.isBlank() || name.length() > 45 || x < 0 || x > 0xFFFF
                || y < 0 || y > 0xFFFF || mapId < 0 || mapId > 0xFFFF) {
            throw new IllegalArgumentException("invalid repository bookmark");
        }
    }

    static RepositoryBookmark from(GameStateStore.Bookmark bookmark) {
        if (bookmark == null) throw new IllegalArgumentException("bookmark is required");
        return new RepositoryBookmark(
                bookmark.name(), bookmark.x(), bookmark.y(), bookmark.mapId());
    }

    GameStateStore.Bookmark gameStateBookmark() {
        return new GameStateStore.Bookmark(name, x, y, mapId);
    }
}

record RepositoryBookmarkSnapshot(boolean authoritative,
                                  List<RepositoryBookmark> bookmarks) {
    RepositoryBookmarkSnapshot {
        bookmarks = bookmarks == null ? List.of() : List.copyOf(bookmarks);
    }
    static RepositoryBookmarkSnapshot unavailable() {
        return new RepositoryBookmarkSnapshot(false, List.of());
    }
    List<GameStateStore.Bookmark> gameStateBookmarks() {
        return bookmarks.stream().map(RepositoryBookmark::gameStateBookmark).toList();
    }
}

record RepositoryWarehousePassword(boolean authoritative, int password) {
    RepositoryWarehousePassword {
        if (password < 0 || password > 999_999) {
            throw new IllegalArgumentException("invalid repository warehouse password");
        }
    }
    static RepositoryWarehousePassword unavailable() {
        return new RepositoryWarehousePassword(false, 0);
    }
}

record RepositoryWarehousePasswordChange(
        boolean authoritative, GameStateStore.WarehousePasswordStatus status) {
    RepositoryWarehousePasswordChange {
        if (status == null) throw new IllegalArgumentException("password status is required");
    }
    static RepositoryWarehousePasswordChange unavailable() {
        return new RepositoryWarehousePasswordChange(
                false, GameStateStore.WarehousePasswordStatus.UNCHANGED);
    }
}

record RepositoryWarehouseSnapshot(boolean authoritative,
                                   List<SourceInventoryItem> items) {
    RepositoryWarehouseSnapshot {
        items = items == null ? List.of() : List.copyOf(items);
    }
    static RepositoryWarehouseSnapshot unavailable() {
        return new RepositoryWarehouseSnapshot(false, List.of());
    }

    RuntimeInventory runtimeInventory() {
        if (!authoritative) return new RuntimeInventory();
        Map<Long, Integer> counts = new LinkedHashMap<>();
        Map<Long, List<RuntimeInventory.PersistentItem>> states = new LinkedHashMap<>();
        Map<Long, InventoryCatalog.Definition> definitions = new LinkedHashMap<>();
        for (SourceInventoryItem item : items) {
            long catalogId = Integer.toUnsignedLong(item.catalogId());
            counts.merge(catalogId, item.quantity(), Math::addExact);
            InventoryCatalog.Definition definition = item.definition();
            InventoryCatalog.Definition previous = definitions.putIfAbsent(
                    catalogId, definition);
            if (previous != null && !previous.equals(definition)) {
                throw new IllegalStateException(
                        "warehouse catalog id resolves to multiple templates: " + catalogId);
            }
            states.computeIfAbsent(catalogId, ignored -> new ArrayList<>()).add(
                    new RuntimeInventory.PersistentItem(
                            item.sourceObjectId(), item.quantity(), item.nativeEnchantLevel(),
                            item.durability(), item.identified(), item.blessing(), true,
                            item.tournamentReward(), false, definition,
                            Math.min(item.chargeCount(),
                                    definition.maximumChargeCount()),
                            item.expiresAtEpochMillis(), item.sealed(),
                            item.litoOwned()));
        }
        Map<Long, List<RuntimeInventory.PersistentItem>> immutable = new LinkedHashMap<>();
        states.forEach((id, rows) -> immutable.put(id, List.copyOf(rows)));
        return RuntimeInventory.fromDurableState(counts, immutable, definitions);
    }
}

record LitoCharacterPersistence(
        String account, String characterName, long objectId,
        int currentHp, int currentMp, int level, long experience, int lawful,
        int pkCount, long lastPkEpochSeconds,
        int elfPkCount, long lastElfPkEpochSeconds,
        int hellTime, int killCount, int deathCount,
        int x, int y, int mapId, int heading, int elfAttribute,
        Set<Integer> skills, Map<Integer, Integer> quests) {
    LitoCharacterPersistence {
        if (account == null || account.isBlank() || characterName == null
                || characterName.isBlank() || objectId <= 0) {
            throw new IllegalArgumentException("invalid LiTo character persistence identity");
        }
        currentHp = Math.max(0, currentHp);
        currentMp = Math.max(0, currentMp);
        level = Math.max(1, level);
        experience = Math.max(0, experience);
        hellTime = Math.max(0, hellTime);
        killCount = Math.max(0, killCount);
        deathCount = Math.max(0, deathCount);
        skills = skills == null ? Set.of() : Set.copyOf(skills);
        quests = quests == null ? Map.of() : Map.copyOf(quests);
    }
}

record LitoSourceInventoryMutation(
        long sourceObjectId, int itemId, int quantity, boolean delete,
        boolean equipped, boolean sealed, int enchantLevel, boolean identified,
        int durability, int chargeCount, int blessing, long expiresAtEpochMillis) {
    LitoSourceInventoryMutation {
        if (sourceObjectId <= 0 || sourceObjectId > 0xFFFF_FFFFL || itemId <= 0
                || (!delete && quantity <= 0) || enchantLevel < -128 || enchantLevel > 127
                || durability < 0 || durability > 0x7F || chargeCount < 0
                || blessing < RuntimeInventory.BLESSING_BLESSED
                || blessing > RuntimeInventory.BLESSING_UNIDENTIFIED
                || expiresAtEpochMillis < 0) {
            throw new IllegalArgumentException("invalid source inventory mutation");
        }
    }

    static LitoSourceInventoryMutation delete(long sourceObjectId, int itemId) {
        return new LitoSourceInventoryMutation(
                sourceObjectId, itemId, 0, true, false, false,
                0, false, 0, 0, RuntimeInventory.BLESSING_NORMAL, 0);
    }
}

record LitoOwnedInventoryItem(
        long objectId, int itemId, String itemName, int quantity,
        boolean equipped, boolean sealed, int enchantLevel, boolean identified,
        int durability, int chargeCount, int blessing, long expiresAtEpochMillis,
        boolean tournamentReward) {
    LitoOwnedInventoryItem {
        itemName = itemName == null ? "" : itemName.trim();
        if (objectId <= 0 || objectId > Integer.MAX_VALUE || itemId <= 0
                || itemName.isBlank() || quantity <= 0
                || enchantLevel < -128 || enchantLevel > 127
                || durability < 0 || durability > 0x7F || chargeCount < 0
                || blessing < RuntimeInventory.BLESSING_BLESSED
                || blessing > RuntimeInventory.BLESSING_UNIDENTIFIED
                || expiresAtEpochMillis < 0) {
            throw new IllegalArgumentException("invalid LiTo-owned inventory item");
        }
    }

    static LitoOwnedInventoryItem from(RuntimeInventory.Item item) {
        if (item == null || item.sourceBound() && !item.litoOwned()) {
            throw new IllegalArgumentException("item is not LiTo-owned");
        }
        return new LitoOwnedInventoryItem(
                item.objectId(), Math.toIntExact(item.definition().catalogId()),
                item.definition().name(), item.quantity(), item.equipped(), item.sealed(),
                item.enchantLevel(), item.identified(), item.durability(),
                item.chargeCount(), item.blessing(), item.expiresAtEpochMillis(),
                item.tournamentReward());
    }
}

enum RepositoryHotelChangeKind { RENT, RETURN }

record RepositoryHotelChange(
        RepositoryHotelChangeKind kind,
        GameStateStore.HotelLeaseState lease,
        GameStateStore.HotelReturnQuote returnQuote,
        Set<Long> keyObjectIds) {
    RepositoryHotelChange {
        if (kind == null) throw new IllegalArgumentException("hotel change kind is required");
        keyObjectIds = keyObjectIds == null ? Set.of() : Set.copyOf(keyObjectIds);
        if (kind == RepositoryHotelChangeKind.RENT
                && (lease == null || returnQuote != null || keyObjectIds.isEmpty())
                || kind == RepositoryHotelChangeKind.RETURN
                && (lease != null || returnQuote == null)) {
            throw new IllegalArgumentException("invalid hotel SQL change");
        }
    }

    static RepositoryHotelChange rent(
            GameStateStore.HotelLeaseState lease, List<Long> keyObjectIds) {
        return new RepositoryHotelChange(RepositoryHotelChangeKind.RENT,
                lease, null, new LinkedHashSet<>(keyObjectIds));
    }

    static RepositoryHotelChange returning(GameStateStore.HotelReturnQuote quote) {
        return new RepositoryHotelChange(RepositoryHotelChangeKind.RETURN,
                null, quote, quote.returnedKeyObjectIds());
    }
}

record RepositoryHotelSnapshot(
        boolean authoritative,
        List<GameStateStore.HotelLeaseState> leases,
        List<GameStateStore.HotelKeyState> keys) {
    RepositoryHotelSnapshot {
        leases = leases == null ? List.of() : List.copyOf(leases);
        keys = keys == null ? List.of() : List.copyOf(keys);
        Set<Integer> shows = new LinkedHashSet<>();
        Map<Integer, GameStateStore.HotelLeaseState> byShow = new LinkedHashMap<>();
        for (GameStateStore.HotelLeaseState lease : leases) {
            if (lease == null || !shows.add(lease.showId())) {
                throw new IllegalArgumentException("duplicate/null repository hotel lease");
            }
            byShow.put(lease.showId(), lease);
        }
        Set<Long> objects = new LinkedHashSet<>();
        for (GameStateStore.HotelKeyState key : keys) {
            GameStateStore.HotelLeaseState lease = key == null
                    ? null : byShow.get(key.showId());
            if (key == null || !objects.add(key.itemObjectId()) || lease == null
                    || key.npcId() != lease.npcId()
                    || key.meetingHall() != lease.meetingHall()
                    || key.expiresAtEpochSeconds() != lease.expiresAtEpochSeconds()) {
                throw new IllegalArgumentException("invalid repository hotel key relation");
            }
        }
        if (!authoritative && (!leases.isEmpty() || !keys.isEmpty())) {
            throw new IllegalArgumentException("unavailable hotel snapshot has rows");
        }
    }

    static RepositoryHotelSnapshot unavailable() {
        return new RepositoryHotelSnapshot(false, List.of(), List.of());
    }

    GameStateStore.HotelOccupancy occupancy(int npcId, String renterIdentity) {
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        long now = System.currentTimeMillis() / 1_000L;
        GameStateStore.HotelLeaseState renter = null;
        boolean[] occupied = new boolean[ClassicWorldHotelCatalog.LEASE_SLOT_COUNT];
        int rooms = 0;
        int halls = 0;
        for (GameStateStore.HotelLeaseState lease : leases) {
            if (lease.npcId() != npcId || !lease.active(now)) continue;
            occupied[lease.roomNumber()] = true;
            if (lease.meetingHall()) halls++; else rooms++;
            if (lease.renterIdentity().equalsIgnoreCase(identity)) renter = lease;
        }
        int free = -1;
        for (int index = 0; index < occupied.length; index++) {
            if (!occupied[index]) { free = index; break; }
        }
        return new GameStateStore.HotelOccupancy(renter, rooms, halls, free);
    }

    GameStateStore.HotelLeaseState activeLeaseForKeys(
            int npcId, Set<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) return null;
        long now = System.currentTimeMillis() / 1_000L;
        Map<Integer, GameStateStore.HotelLeaseState> byShow = leases.stream()
                .collect(java.util.stream.Collectors.toMap(
                        GameStateStore.HotelLeaseState::showId, lease -> lease));
        for (GameStateStore.HotelKeyState key : keys) {
            if (key.npcId() != npcId || !objectIds.contains(key.itemObjectId())) continue;
            GameStateStore.HotelLeaseState lease = byShow.get(key.showId());
            if (lease != null && lease.active(now)) return lease;
        }
        return null;
    }

    GameStateStore.HotelReturnQuote returnQuote(
            int npcId, String renterIdentity, Set<Long> inventoryObjectIds) {
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        long now = System.currentTimeMillis() / 1_000L;
        GameStateStore.HotelLeaseState owned = leases.stream()
                .filter(lease -> lease.npcId() == npcId && !lease.released()
                        && lease.renterIdentity().equalsIgnoreCase(identity))
                .sorted(java.util.Comparator.comparingInt(
                                (GameStateStore.HotelLeaseState lease) ->
                                        lease.active(now) ? 0 : 1)
                        .thenComparingInt(GameStateStore.HotelLeaseState::roomNumber))
                .findFirst().orElse(null);
        Set<Long> returned = keys.stream()
                .filter(key -> key.npcId() == npcId
                        && inventoryObjectIds != null
                        && inventoryObjectIds.contains(key.itemObjectId()))
                .map(GameStateStore.HotelKeyState::itemObjectId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        int refund = Math.multiplyExact(returned.size(),
                ClassicWorldHotelCatalog.REFUND_PER_RETURNED_KEY);
        if (owned != null && owned.active(now)) {
            refund = Math.addExact(refund,
                    ClassicWorldHotelCatalog.ACTIVE_LEASE_BASE_REFUND);
        }
        return new GameStateStore.HotelReturnQuote(
                npcId, identity, refund, owned != null, returned);
    }
}

record RepositoryWaterDragonPassChange(
        GameStateStore.WaterDragonPassState pass) {
    RepositoryWaterDragonPassChange {
        if (pass == null) {
            throw new IllegalArgumentException("Water Dragon pass SQL change is required");
        }
    }
}

record RepositoryWaterDragonPassSnapshot(
        boolean authoritative,
        List<GameStateStore.WaterDragonPassState> passes) {
    RepositoryWaterDragonPassSnapshot {
        passes = passes == null ? List.of() : List.copyOf(passes);
        Set<Long> objects = new LinkedHashSet<>();
        Set<Integer> rooms = new LinkedHashSet<>();
        Set<String> renters = new LinkedHashSet<>();
        for (GameStateStore.WaterDragonPassState pass : passes) {
            if (pass == null || !objects.add(pass.itemObjectId())
                    || !rooms.add(pass.roomNumber())
                    || !renters.add(pass.renterIdentity().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "duplicate/null repository Water Dragon pass");
            }
        }
        if (!authoritative && !passes.isEmpty()) {
            throw new IllegalArgumentException(
                    "unavailable Water Dragon pass snapshot has rows");
        }
    }

    static RepositoryWaterDragonPassSnapshot unavailable() {
        return new RepositoryWaterDragonPassSnapshot(false, List.of());
    }
}

record AccountInfoSnapshot(int type, List<String> fields) {
    AccountInfoSnapshot {
        fields = List.copyOf(fields);
    }
}

record QuizRegistrationSnapshot(List<String> fields) {
    QuizRegistrationSnapshot {
        fields = List.copyOf(fields);
    }
}

record CharacterSummary(
        String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
        int armorClass, int strength, int constitution, int dexterity, int charisma,
        int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
        long objectId, long experience, long lawful,
        int pkCount, long lastPkEpochSeconds,
        int pkCountForElf, long lastPkForElfEpochSeconds,
        long classId, long locationX, long locationY, long mapId,
        int originalConstitution, int food, int constitutionHprMaximum,
        int originalStrength, int originalDexterity, int elfAttribute) {
    /** Compatibility constructor for snapshots before native general-PK time. */
    CharacterSummary(
            String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
            int armorClass, int strength, int constitution, int dexterity, int charisma,
            int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
            long objectId, long experience, long lawful,
            int pkCount, int pkCountForElf, long lastPkForElfEpochSeconds,
            long classId, long locationX, long locationY, long mapId,
            int originalConstitution, int food, int constitutionHprMaximum,
            int originalStrength, int originalDexterity, int elfAttribute) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                originalIntelligence, originalWisdom, objectId, experience, lawful,
                pkCount, 0, pkCountForElf, lastPkForElfEpochSeconds,
                classId, locationX, locationY, mapId,
                originalConstitution, food, constitutionHprMaximum,
                originalStrength, originalDexterity, elfAttribute);
    }

    /** Compatibility constructor for snapshots before native elf-PK state. */
    CharacterSummary(
            String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
            int armorClass, int strength, int constitution, int dexterity, int charisma,
            int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
            long objectId, long experience, long lawful,
            int pkCount, long classId, long locationX, long locationY, long mapId,
            int originalConstitution, int food, int constitutionHprMaximum,
            int originalStrength, int originalDexterity, int elfAttribute) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                originalIntelligence, originalWisdom, objectId, experience, lawful,
                pkCount, 0, 0, 0, classId, locationX, locationY, mapId,
                originalConstitution, food, constitutionHprMaximum,
                originalStrength, originalDexterity, elfAttribute);
    }
    /** Compatibility constructor for snapshots before ElfAttr was imported. */
    CharacterSummary(
            String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
            int armorClass, int strength, int constitution, int dexterity, int charisma,
            int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
            long objectId, long experience, long lawful,
            int pkCount, long classId, long locationX, long locationY, long mapId,
            int originalConstitution, int food, int constitutionHprMaximum,
            int originalStrength, int originalDexterity) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                originalIntelligence, originalWisdom, objectId, experience, lawful,
                pkCount, classId, locationX, locationY, mapId,
                originalConstitution, food, constitutionHprMaximum,
                originalStrength, originalDexterity, 0);
    }

    /** Compatibility constructor for snapshots before original DEX was qualified. */
    CharacterSummary(
            String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
            int armorClass, int strength, int constitution, int dexterity, int charisma,
            int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
            long objectId, long experience, long lawful,
            int pkCount, long classId, long locationX, long locationY, long mapId,
            int originalConstitution, int food, int constitutionHprMaximum,
            int originalStrength) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                originalIntelligence, originalWisdom, objectId, experience, lawful,
                pkCount, classId, locationX, locationY, mapId,
                originalConstitution, food, constitutionHprMaximum,
                originalStrength, dexterity);
    }

    /** Compatibility constructor for snapshots before original STR was qualified. */
    CharacterSummary(
            String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
            int armorClass, int strength, int constitution, int dexterity, int charisma,
            int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
            long objectId, long experience, long lawful,
            int pkCount, long classId, long locationX, long locationY, long mapId,
            int originalConstitution, int food, int constitutionHprMaximum) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                originalIntelligence, originalWisdom, objectId, experience, lawful,
                pkCount, classId, locationX, locationY, mapId,
                originalConstitution, food, constitutionHprMaximum, strength, dexterity);
    }

    /** Compatibility constructor for isolated/state fixtures predating native HPR. */
    CharacterSummary(
            String name, String clanName, int type, int sex, int level, int maxHp, int maxMp,
            int armorClass, int strength, int constitution, int dexterity, int charisma,
            int intelligence, int wisdom, int originalIntelligence, int originalWisdom,
            long objectId, long experience, long lawful,
            int pkCount, long classId, long locationX, long locationY, long mapId) {
        this(name, clanName, type, sex, level, maxHp, maxMp, armorClass,
                strength, constitution, dexterity, charisma, intelligence, wisdom,
                originalIntelligence, originalWisdom, objectId, experience, lawful,
                pkCount, classId, locationX, locationY, mapId,
                constitution, 40, BattleNaturalHpRegen.fallbackConHprMaximum(constitution),
                strength, dexterity);
    }

    CharacterSummary {
        if (originalConstitution < 0 || originalStrength < 0 || originalDexterity < 0
                || food < 0 || constitutionHprMaximum < 1
                || pkCount < 0 || lastPkEpochSeconds < 0
                || pkCountForElf < 0 || lastPkForElfEpochSeconds < 0
                || elfAttribute < 0 || (elfAttribute & ~0x0F) != 0) {
            throw new IllegalArgumentException("invalid natural-HP-regeneration character fields");
        }
    }
}

record SourceKillDeathCounts(int kills, int deaths) {
    SourceKillDeathCounts {
        if (kills < 0 || deaths < 0) {
            throw new IllegalArgumentException("invalid source kill/death counts");
        }
    }

    static SourceKillDeathCounts none() {
        return new SourceKillDeathCounts(0, 0);
    }
}

/** Qualified base-only MR reconstructed from persisted classic character state. */
record SourceBaseMagicResistance(
        int value, int classBase, int levelBonus, int wisdomBonus, int originalBonus) {
    SourceBaseMagicResistance {
        if (value < 0 || classBase < 0 || levelBonus < 0 || wisdomBonus < 0
                || originalBonus < 0
                || value != classBase + levelBonus + wisdomBonus + originalBonus) {
            throw new IllegalArgumentException("invalid source base magic resistance");
        }
    }

    static SourceBaseMagicResistance from(
            int type, int level, int wisdomBonus, int originalWisdom) {
        if (level < 0 || wisdomBonus < 0) {
            throw new IllegalArgumentException("invalid source base MR inputs");
        }
        int classBase = BattleMagicResistance.classBase(type);
        int levelBonus = level / 2;
        int originalBonus = BattleMagicResistance.originalMrBonus(type, originalWisdom);
        return new SourceBaseMagicResistance(
                classBase + levelBonus + wisdomBonus + originalBonus,
                classBase, levelBonus, wisdomBonus, originalBonus);
    }

    static SourceBaseMagicResistance from(CharacterSummary character) {
        return from(character.type(), character.level(),
                BattleMagicResistance.wisdomMagicResistance(character.wisdom()),
                character.originalWisdom());
    }
}

/**
 * Read-only, source-qualified part of the original character's fire defence.
 *
 * <p>This is deliberately narrower than a live classic-server {@code getFire()}:
 * it contains only persisted, equipped rows that join to the authoritative
 * {@code armor.defense_fire} resource.  LiTo owns its in-match equipment and
 * timed effects separately, so callers can add those without pretending that
 * an unavailable legacy runtime value was zero.</p>
 */
record SourceEquippedFireResistance(int value, int matchedArmorRows) {
    SourceEquippedFireResistance {
        if (value < NumericBreakthrough.MIN_ELEMENTAL_RESISTANCE
                || value > NumericBreakthrough.MAX_ELEMENTAL_RESISTANCE
                || matchedArmorRows < 0) {
            throw new IllegalArgumentException("invalid source equipped fire resistance");
        }
    }

    static SourceEquippedFireResistance none() {
        return new SourceEquippedFireResistance(0, 0);
    }
}

/** Immutable, source-qualified raw inventory weight and its server multiplier. */
record SourceInventoryWeightStats(
        int rawWeight, int itemRows, int armorRows, int weaponRows, int etcRows,
        double serverWeightRate) {
    SourceInventoryWeightStats {
        if (rawWeight < 0 || itemRows < 0 || armorRows < 0 || weaponRows < 0
                || etcRows < 0 || itemRows != armorRows + weaponRows + etcRows
                || !Double.isFinite(serverWeightRate) || serverWeightRate <= 0.0) {
            throw new IllegalArgumentException("invalid source inventory-weight snapshot");
        }
    }

    static SourceInventoryWeightStats none(double serverWeightRate) {
        return new SourceInventoryWeightStats(0, 0, 0, 0, 0, serverWeightRate);
    }
}

/**
 * One immutable row from the selected original character's inventory.
 *
 * <p>The legacy object id remains provenance only. LiTo assigns a separate
 * durable object id before the row enters its writable sidecar, so duplicate
 * ids in two original-server databases can never collide. The complete row is
 * retained here while ordinary items, source equipment, projectiles and pet
 * collars continue through their own authoritative reconciliation pipelines.
 * Source equipment keeps this exact row provenance so LiTo can persist only a
 * sidecar equip flag and ask the read-only retained pipeline to recompute the
 * resulting character state.</p>
 */
record SourceMagicDollPower(
        int id, String note, String className, int valueMain, int probability,
        int intervalSeconds, int passiveSkillId, int mode, int attributeType,
        int gfxId, int gfxTarget, int valueSub, int triggerSkillId,
        int itemId, int itemCount, int statusId) {
    SourceMagicDollPower {
        note = note == null ? "" : note;
        className = className == null ? "" : className.trim();
        if (id <= 0 || className.isBlank() || probability < 0
                || intervalSeconds < 0 || passiveSkillId < 0 || gfxId < 0
                || gfxTarget < 0 || triggerSkillId < 0 || itemId < 0
                || itemCount < 0 || statusId < 0) {
            throw new IllegalArgumentException(
                    "invalid selected-source magic doll power");
        }
        boolean activeValid = switch (className) {
            case "ACTIVE_SKILL" -> probability > 0 && triggerSkillId > 0;
            case "ACTIVE_DMG" -> probability > 0 && mode != 0
                    && (mode == 3 || valueSub != 0);
            case "ACTIVE_REDUX" -> probability > 0 && valueSub > 0;
            default -> false;
        };
        boolean passiveValid = switch (className) {
            case "PASSIVE_STR", "PASSIVE_DEX", "PASSIVE_CON",
                    "PASSIVE_INT", "PASSIVE_WIS", "PASSIVE_CHA",
                    "PASSIVE_HP", "PASSIVE_MP", "PASSIVE_MR",
                    "PASSIVE_SP", "PASSIVE_AC", "PASSIVE_DMG",
                    "PASSIVE_HIT", "PASSIVE_BOW_DMG", "PASSIVE_BOW_HIT",
                    "PASSIVE_PIERCE", "PASSIVE_DMG_REDUX", "PASSIVE_DODGE",
                    "PASSIVE_ER", "PASSIVE_DEF_FIRE", "PASSIVE_DEF_WATER",
                    "PASSIVE_DEF_WIND", "PASSIVE_DEF_EARTH",
                    "PASSIVE_REGIST_FREEZE", "PASSIVE_REGIST_STONE",
                    "PASSIVE_REGIST_SLEEP", "PASSIVE_REGIST_BLIND",
                    "PASSIVE_REGIST_STUN", "PASSIVE_REGIST_SUSTAIN",
                    "PASSIVE_STUN_SUCCESS", "PASSIVE_MAGIC_DMG_MODIFIER",
                    "PASSIVE_MAGIC_HIT", "PASSIVE_POTION_RECOVERY",
                    "PASSIVE_POISON_RESIST", "PASSIVE_PVP_DMG",
                    "PASSIVE_PVP_REDUX", "PASSIVE_DMG_BOSS",
                    "PASSIVE_BOSS_REDUX", "PASSIVE_IGNORE_REDUCTION",
                    "PASSIVE_MELEE_REDUX", "PASSIVE_BOW_REDUX",
                    "PASSIVE_ADENA_RATE", "PASSIVE_DROP_RATE",
                    "PASSIVE_EXP_RATE", "PASSIVE_HP_REGEN",
                    "PASSIVE_MP_REGEN", "PASSIVE_MAGIC_DMG_REDUX",
                    "PASSIVE_WEIGHT_REDUCTION", "PASSIVE_MAGIC_BOW",
                    "PASSIVE_ARROW_SPR", "PASSIVE_BASE_DMG_SMALL",
                    "PASSIVE_BASE_DMG_LARGE" -> valueMain != 0;
            case "STATUS_HASTE", "STATUS_UNDERWATER" -> true;
            case "AUTO_SKILL" -> passiveSkillId > 0;
            case "TIMED_HP_REGEN", "TIMED_MP_REGEN" -> intervalSeconds > 0;
            case "TIMED_ITEM" -> intervalSeconds > 0 && itemId > 0;
            case "PASSIVE_WEIGHT" -> valueMain != 0 || valueSub != 0;
            default -> false;
        };
        if (!activeValid && !passiveValid) {
            throw new IllegalArgumentException(
                    "invalid selected-source magic doll power semantics id="
                            + id + " class=" + className);
        }
    }
}

record SourceMagicDollType(
        int itemId, int dollType, int iconId, List<Integer> powerIds,
        List<Integer> materialIds, List<Integer> materialCounts,
        int durationSeconds, int gfxId, String nameId,
        List<SourceMagicDollPower> powerDefinitions) {
    SourceMagicDollType(
            int itemId, int dollType, int iconId, List<Integer> powerIds,
            List<Integer> materialIds, List<Integer> materialCounts,
            int durationSeconds, int gfxId, String nameId) {
        this(itemId, dollType, iconId, powerIds, materialIds, materialCounts,
                durationSeconds, gfxId, nameId, List.of());
    }

    SourceMagicDollType {
        powerIds = powerIds == null ? List.of() : List.copyOf(powerIds);
        materialIds = materialIds == null ? List.of() : List.copyOf(materialIds);
        materialCounts = materialCounts == null ? List.of() : List.copyOf(materialCounts);
        powerDefinitions = powerDefinitions == null
                ? List.of() : List.copyOf(powerDefinitions);
        nameId = nameId == null ? "" : nameId;
        if (itemId <= 0 || dollType < 0 || iconId < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || gfxId <= 0 || gfxId > 0xFFFF || nameId.isBlank()
                || materialIds.size() != materialCounts.size()
                || powerIds.stream().anyMatch(id -> id == null || id <= 0)
                || materialIds.stream().anyMatch(id -> id == null || id <= 0)
                || materialCounts.stream().anyMatch(count -> count == null || count <= 0)
                || (!powerDefinitions.isEmpty()
                    && !powerDefinitions.stream().map(SourceMagicDollPower::id).toList()
                            .equals(powerIds))) {
            throw new IllegalArgumentException("invalid selected-source magic doll type");
        }
    }
}

record SourceInventoryItem(
        long sourceObjectId, int catalogId, int quantity, boolean equipped,
        int sourceEnchantLevel, int durability, boolean identified, int blessing,
        boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire,
        InventoryCatalog.NativeArmorStatus sourceArmorStatus,
        long expiresAtEpochMillis, boolean sealed,
        SourceMagicDollType magicDollType,
        boolean litoOwned, boolean tournamentReward, int chargeCount) {
    /** Format-41 compatibility before exact instance charge-count projection. */
    SourceInventoryItem(
            long sourceObjectId, int catalogId, int quantity, boolean equipped,
            int sourceEnchantLevel, int durability, boolean identified, int blessing,
            boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire,
            InventoryCatalog.NativeArmorStatus sourceArmorStatus,
            long expiresAtEpochMillis, boolean sealed,
            SourceMagicDollType magicDollType,
            boolean litoOwned, boolean tournamentReward) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel,
                durability, identified, blessing, linkedPetCollar, wire,
                sourceArmorStatus, expiresAtEpochMillis, sealed, magicDollType,
                litoOwned, tournamentReward, 0);
    }
    /** Format-39/source projection compatibility before LiTo SQL ownership. */
    SourceInventoryItem(
            long sourceObjectId, int catalogId, int quantity, boolean equipped,
            int sourceEnchantLevel, int durability, boolean identified, int blessing,
            boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire,
            InventoryCatalog.NativeArmorStatus sourceArmorStatus,
            long expiresAtEpochMillis, boolean sealed,
            SourceMagicDollType magicDollType) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel,
                durability, identified, blessing, linkedPetCollar, wire,
                sourceArmorStatus, expiresAtEpochMillis, sealed, magicDollType,
                false, false, 0);
    }
    SourceInventoryItem(
            long sourceObjectId, int catalogId, int quantity, boolean equipped,
            int sourceEnchantLevel, int durability, boolean identified, int blessing,
            boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel,
                durability, identified, blessing, linkedPetCollar, wire, null,
                0L, false, null);
    }

    /** Format-37 source projection compatibility before timed/sealed state. */
    SourceInventoryItem(
            long sourceObjectId, int catalogId, int quantity, boolean equipped,
            int sourceEnchantLevel, int durability, boolean identified, int blessing,
            boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire,
            InventoryCatalog.NativeArmorStatus sourceArmorStatus) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel,
                durability, identified, blessing, linkedPetCollar, wire,
                sourceArmorStatus, 0L, false, null);
    }

    /** Format-38 source projection compatibility before magic-doll metadata. */
    SourceInventoryItem(
            long sourceObjectId, int catalogId, int quantity, boolean equipped,
            int sourceEnchantLevel, int durability, boolean identified, int blessing,
            boolean linkedPetCollar, ClassicWorldDropCatalog.ItemWire wire,
            InventoryCatalog.NativeArmorStatus sourceArmorStatus,
            long expiresAtEpochMillis, boolean sealed) {
        this(sourceObjectId, catalogId, quantity, equipped, sourceEnchantLevel,
                durability, identified, blessing, linkedPetCollar, wire,
                sourceArmorStatus, expiresAtEpochMillis, sealed, null);
    }

    SourceInventoryItem {
        if (sourceObjectId <= 0 || sourceObjectId > 0xFFFF_FFFFL
                || catalogId <= 0 || quantity <= 0 || durability < 0
                || durability > 0x7F || blessing < RuntimeInventory.BLESSING_BLESSED
                || blessing > RuntimeInventory.BLESSING_UNIDENTIFIED
                || chargeCount < 0 || chargeCount > 0xFF
                || expiresAtEpochMillis < 0
                || tournamentReward && !litoOwned
                || wire == null || wire.itemId() != catalogId) {
            throw new IllegalArgumentException("invalid selected-source inventory row");
        }
        if ((sourceArmorStatus != null) != wire.kind().equals("armor")) {
            throw new IllegalArgumentException(
                    "selected-source armor status/template kind mismatch");
        }
        if (magicDollType != null && (magicDollType.itemId() != catalogId
                || !wire.kind().equals("etc") || wire.stackable()
                || equipped || linkedPetCollar)) {
            throw new IllegalArgumentException(
                    "selected-source magic doll/template state mismatch");
        }
    }

    int nativeEnchantLevel() {
        return (byte) sourceEnchantLevel;
    }

    InventoryCatalog.Definition definition() {
        InventoryCatalog.Definition definition = wire.inventoryDefinition(sourceArmorStatus);
        return magicDollType == null ? definition
                : definition.withEffect(InventoryCatalog.Effect.MAGIC_DOLL);
    }

    int nativeWeight() {
        return BattleInventoryWeight.itemWeight(quantity, wire.unitWeight());
    }

    /** Ordinary non-equipment slice that cannot perturb source combat state. */
    boolean ordinarySidecarImportable() {
        return wire.kind().equals("etc") && !equipped && !linkedPetCollar
                && !wire.useToken().equals("arrow")
                && !wire.useToken().equals("sting")
                && sourceEnchantLevel == 0 && durability == 0
                && (wire.stackable() || quantity == 1);
    }

    /** LiTo-owned SQL collar is adopted by exact object id before its pet row. */
    boolean litoOwnedPetCollarImportable() {
        return litoOwned && linkedPetCollar && wire.kind().equals("etc")
                && !equipped && quantity == 1 && sourceEnchantLevel == 0
                && durability == 0;
    }

    /**
     * A weapon row can be materialized without guessing an armor slot. Only
     * the one row matched to the qualified equipment snapshot is allowed to
     * drive combat; other source weapons remain visible but fail closed when
     * an equip request lacks per-instance combat provenance.
     */
    boolean sourceWeaponImportable() {
        return wire.kind().equals("weapon") && !linkedPetCollar && quantity == 1;
    }

    /** Armor can be materialized with an exact retained use-type and source row. */
    boolean sourceArmorImportable() {
        return wire.kind().equals("armor") && !linkedPetCollar && quantity == 1
                && sourceArmorStatus != null;
    }

    /** Exact retained arrow/sting row, materialized as a source-bound stack. */
    boolean sourceProjectileImportable() {
        return wire.kind().equals("etc") && !equipped && !linkedPetCollar
                && (wire.useToken().equals("arrow")
                    || wire.useToken().equals("sting"))
                && sourceEnchantLevel == 0 && durability == 0
                && (wire.stackable() || quantity == 1);
    }

    boolean sidecarImportable() {
        return ordinarySidecarImportable() || sourceWeaponImportable()
                || sourceArmorImportable() || sourceProjectileImportable()
                || litoOwnedPetCollarImportable();
    }
}

/** Immutable source-qualified STR/DEX physical hit and damage tables. */
record SourcePhysicalGrowthStats(
        List<Integer> strengthHit, List<Integer> strengthDamage,
        List<Integer> dexterityHit, List<Integer> dexterityDamage) {
    static final int MAX_STAT = 127;

    SourcePhysicalGrowthStats {
        strengthHit = qualifiedTable(strengthHit, "STR hit");
        strengthDamage = qualifiedTable(strengthDamage, "STR damage");
        dexterityHit = qualifiedTable(dexterityHit, "DEX hit");
        dexterityDamage = qualifiedTable(dexterityDamage, "DEX damage");
    }

    static SourcePhysicalGrowthStats classic381() {
        List<Integer> strHit = new ArrayList<>(MAX_STAT);
        List<Integer> strDamage = new ArrayList<>(MAX_STAT);
        List<Integer> dexHit = new ArrayList<>(MAX_STAT);
        List<Integer> dexDamage = new ArrayList<>(MAX_STAT);
        for (int stat = 1; stat <= MAX_STAT; stat++) {
            strHit.add(BattlePhysicalAttack.strengthHit(stat));
            strDamage.add(classicStrengthDamage(stat));
            dexHit.add(BattlePhysicalAttack.dexterityHit(stat));
            dexDamage.add(classicDexterityDamage(stat));
        }
        return new SourcePhysicalGrowthStats(strHit, strDamage, dexHit, dexDamage);
    }

    int hit(boolean ranged, int stat) {
        return (ranged ? dexterityHit : strengthHit).get(index(stat));
    }

    int damage(boolean ranged, int stat) {
        return (ranged ? dexterityDamage : strengthDamage).get(index(stat));
    }

    private static int classicStrengthDamage(int strength) {
        int value = Math.max(1, Math.min(MAX_STAT, strength));
        if (value <= 22) return Math.floorDiv(value - 11, 2);
        int damage = 5;
        for (int stat = 23; stat <= value; stat++) {
            if (stat <= 28) {
                if (stat % 3 == 2) damage++;
            } else if (stat <= 32) {
                if (stat % 2 == 1) damage++;
            } else if (stat <= 34) {
                damage++;
            } else if (stat % 4 == 1) {
                damage++;
            }
        }
        return damage;
    }

    private static int classicDexterityDamage(int dexterity) {
        int value = Math.max(1, Math.min(MAX_STAT, dexterity));
        if (value <= 14) return 0;
        if (value <= 18) return value - 14;
        if (value <= 20) return 4;
        if (value <= 23) return 5;
        int damage = 5;
        for (int stat = 24; stat <= value; stat++) {
            if (stat <= 35 && stat % 3 == 1) damage++;
            else if (stat >= 36 && stat % 4 == 1) damage++;
        }
        return damage;
    }

    private static List<Integer> qualifiedTable(List<Integer> values, String name) {
        boolean containsNull = false;
        if (values != null) {
            for (Integer value : values) {
                if (value == null) {
                    containsNull = true;
                    break;
                }
            }
        }
        if (values == null || values.size() != MAX_STAT || containsNull) {
            throw new IllegalArgumentException(
                    "source physical " + name + " table must contain ids 1..127");
        }
        return List.copyOf(values);
    }

    private static int index(int stat) {
        return Math.max(1, Math.min(MAX_STAT, stat)) - 1;
    }
}

/** Immutable NPC combat and lawful fields imported from the selected source. */
record SourceNpcPhysicalStats(
        int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
        int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible,
        int lawful, boolean tamable, boolean boss,
        int maxMp, int strength, int dexterity, boolean eraseEligible,
        int ranged, int bowActionId, int poisonAttack, int paralysisAttack) {
    /** Compatibility constructor before native poison fields were qualified. */
    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible,
            int lawful, boolean tamable, boolean boss,
            int maxMp, int strength, int dexterity, boolean eraseEligible,
            int ranged, int bowActionId) {
        this(npcId, armorClass, large, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible,
                lawful, tamable, boss, maxMp, strength, dexterity,
                eraseEligible, ranged, bowActionId, 0, 0);
    }

    /** Compatibility constructor before native ranged/bowActId were qualified. */
    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible,
            int lawful, boolean tamable, boolean boss,
            int maxMp, int strength, int dexterity, boolean eraseEligible) {
        this(npcId, armorClass, large, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible,
                lawful, tamable, boss, maxMp, strength, dexterity,
                eraseEligible, 0, 0);
    }

    /** Compatibility constructor for snapshots before native IsErase was qualified. */
    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance, boolean turnUndeadEligible,
            int lawful, boolean tamable, boolean boss,
            int maxMp, int strength, int dexterity) {
        this(npcId, armorClass, large, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible,
                lawful, tamable, boss, maxMp, strength, dexterity, false);
    }

    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard) {
        this(npcId, armorClass, large, damageReduction, hard,
                0, 0, 0, false, 0, false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance) {
        this(npcId, armorClass, large, damageReduction, hard,
                undead, weakAttribute, magicResistance, false, 0,
                false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible) {
        this(npcId, armorClass, large, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, 0,
                false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats(
            int npcId, int armorClass, boolean large, int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible, int lawful) {
        this(npcId, armorClass, large, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, lawful,
                false, false, 0, 1, 1);
    }

    SourceNpcPhysicalStats {
        if (npcId <= 0 || damageReduction < 0 || undead < 0
                || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE
                || maxMp < 0 || strength < 0 || dexterity < 0
                || ranged < 0 || bowActionId < 0 || bowActionId > 0xFFFF
                || poisonAttack < 0 || paralysisAttack < 0
                || !switch (weakAttribute) {
                    case -8, -4, -2, -1, 0, 1, 2, 4, 8 -> true;
                    default -> false;
                }) {
            throw new IllegalArgumentException("invalid source NPC physical snapshot");
        }
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard) {
        return fromSourceRow(
                npcId, armorClass, size, damageReduction, hard, 0, 0, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance) {
        return fromSourceRow(npcId, armorClass, size, damageReduction, hard,
                undead, weakAttribute, magicResistance, false);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible) {
        return fromSourceRow(npcId, armorClass, size, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible, int lawful) {
        return fromSourceRow(npcId, armorClass, size, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, lawful,
                false, false, 0, 1, 1);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible, int lawful,
            boolean tamable, boolean boss, int maxMp, int strength, int dexterity) {
        return fromSourceRow(npcId, armorClass, size, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, lawful,
                tamable, boss, maxMp, strength, dexterity, false);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible, int lawful,
            boolean tamable, boolean boss, int maxMp, int strength, int dexterity,
            boolean eraseEligible) {
        return fromSourceRow(npcId, armorClass, size, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, lawful,
                tamable, boss, maxMp, strength, dexterity, eraseEligible, 0, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible, int lawful,
            boolean tamable, boolean boss, int maxMp, int strength, int dexterity,
            boolean eraseEligible, int ranged, int bowActionId) {
        return fromSourceRow(npcId, armorClass, size, damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, lawful,
                tamable, boss, maxMp, strength, dexterity, eraseEligible,
                ranged, bowActionId, 0, 0);
    }

    static SourceNpcPhysicalStats fromSourceRow(
            int npcId, int armorClass, String size,
            int damageReduction, boolean hard,
            int undead, int weakAttribute, int magicResistance,
            boolean turnUndeadEligible, int lawful,
            boolean tamable, boolean boss, int maxMp, int strength, int dexterity,
            boolean eraseEligible, int ranged, int bowActionId,
            int poisonAttack, int paralysisAttack) {
        String normalized = size == null ? "" : size.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("small") && !normalized.equals("large")) {
            throw new IllegalArgumentException("unsupported source NPC size: " + size);
        }
        return new SourceNpcPhysicalStats(
                npcId, armorClass, normalized.equals("large"), damageReduction, hard,
                undead, weakAttribute, magicResistance, turnUndeadEligible, lawful,
                tamable, boss, maxMp, strength, dexterity, eraseEligible,
                ranged, bowActionId, poisonAttack, paralysisAttack);
    }

    static SourceNpcPhysicalStats memoryFixture(int npcId) {
        // Loopback replay mirrors the exact rows verified in both configured
        // immutable 3.80/3.81-family resource stores.
        return switch (npcId) {
            // Exact selected 380_db.sql L1MonsterGuard Orc-castle arrow tower.
            // HP/MP are immutable in the instance; these are its attack and
            // defensive formula fields, including ranged=10/bowActId=66.
            case 60520 -> new SourceNpcPhysicalStats(
                    npcId, -50, false, 0, false,
                    0, 0, 80, false, 0, false, false,
                    10, 60, 60, false, 10, 66);
            // Exact selected 380_db.sql L1Scarecrow physical rows.
            case 45001, 45002, 45003, 45004 -> new SourceNpcPhysicalStats(
                    npcId, 0, false, 0, false,
                    0, 0, 0, false, 0, false, false,
                    0, 0, 0, false, 1, 0);
            // Exact retained 380_db.sql siege trebuchet rows.  Both are
            // L1Monster objects with 30,000 HP, 20 MP, AC -10 and otherwise
            // inert combat fields (MR/agro/speeds/experience are all zero).
            case 20000, 20001 -> new SourceNpcPhysicalStats(
                    npcId, -10, true, 0, false,
                    0, 0, 0, false, 0, false, false,
                    20, 0, 0, false, 0, 0);
            case 45262 -> new SourceNpcPhysicalStats(npcId, -4, true, 0, true,
                    0, 8, 0, false, -22, false, false, 10, 16, 15, true);
            case 45207 -> new SourceNpcPhysicalStats(npcId, -20, true, 0, false,
                    0, 8, 15, false, -25, true, false, 20, 17, 17, true);
            // Exact selected 380_db.sql tamable ghoul row: poison_atk=4.
            case 45157 -> new SourceNpcPhysicalStats(npcId, -4, false, 0, false,
                    1, 2, 13, true, -19, true, false, 30, 16, 16, true,
                    1, 0, 4, 0);
            case 45289 -> new SourceNpcPhysicalStats(npcId, -12, false, 0, false,
                    0, 2, 35, false, -40, true, false, 50, 17, 16, true);
            case 45259 -> new SourceNpcPhysicalStats(npcId, -15, true, 0, false,
                    0, 4, 5, false, -18, true, false, 30, 14, 12, true);
            case 45278 -> new SourceNpcPhysicalStats(npcId, -18, true, 0, false,
                    0, 0, 20, false, -34, true, false, 20, 20, 12, true);
            case 45322 -> new SourceNpcPhysicalStats(npcId, -25, false, 0, true,
                    2, 2, 40, false, -35, true, false, 50, 18, 12, true);
            // Exact selected 380_db.sql earth-elemental summon row.  The
            // retained IsErase=1 flag makes enemy target_to=NPC probability
            // spells, including skill29 Slow, legal against this actor.
            case 45306 -> new SourceNpcPhysicalStats(npcId, -15, false, 0, false,
                    0, 0, 35, false, 10, false, false, 0, 16, 16, true,
                    1, 0);
            // Exact selected summon rows whose retained L1AttackNpc actionX1
            // path emits S_UseArrowSkill beyond one tile.
            case 81215 -> new SourceNpcPhysicalStats(npcId, -3, false, 0, false,
                    0, 4, 5, false, 0, true, false, 70, 14, 14, true,
                    10, 66);
            // Exact selected summon rows retain L1AttackNpc poison_atk 1/2.
            case 81217 -> new SourceNpcPhysicalStats(npcId, 0, false, 0, false,
                    0, 0, 5, false, 0, true, false, 80, 16, 14, true,
                    1, 0, 1, 0);
            case 81227 -> new SourceNpcPhysicalStats(npcId, -5, false, 0, false,
                    0, 4, 20, false, 0, false, false, 110, 13, 16, true,
                    10, 66);
            case 81236 -> new SourceNpcPhysicalStats(npcId, -50, false, 0, false,
                    0, 0, 50, false, 0, false, false, 140, 15, 20, true,
                    3, 0, 2, 0);
            case 45390 -> new SourceNpcPhysicalStats(npcId, -20, false, 0, false,
                    0, 0, 0, false, -20, false, false, 20, 20, 10, true);
            case 45380 -> new SourceNpcPhysicalStats(npcId, -16, false, 0, false,
                    0, 0, 50, false, -39, false, false, 120, 17, 16, true);
            case 45601 -> new SourceNpcPhysicalStats(npcId, -75, false, 0, true,
                    3, 2, 150, false, -120, false, true, 250, 77, 17, false,
                    1, 0);
            case 45650 -> new SourceNpcPhysicalStats(npcId, -62, true, 0, true,
                    3, 0, 50, false, -180, false, true, 100, 67, 62, false,
                    2, 0);
            case 45161 -> new SourceNpcPhysicalStats(npcId, -3, false, 0, false,
                    1, 2, 18, true, -18, true, false, 30, 17, 16, true,
                    1, 0);
            case 45182 -> new SourceNpcPhysicalStats(npcId, -8, true, 0, true,
                    0, 8, 7, false, -14, true, false, 0, 46, 10, true);
            case 45812 -> new SourceNpcPhysicalStats(npcId, -23, false, 0, false,
                    0, 8, 35, false, -20, false, false, 500, 13, 13, true);
            case 45584 -> new SourceNpcPhysicalStats(npcId, -69, true, 0, true,
                    0, 2, 60, false, -110, false, true, 200, 72, 12, false,
                    1, 0);
            case 45610 -> new SourceNpcPhysicalStats(npcId, -67, true, 0, true,
                    0, 2, 70, false, -80, false, true, 350, 22, 18, false,
                    1, 0);
            case 45682 -> new SourceNpcPhysicalStats(npcId, -109, true, 0, true,
                    0, 2, 180, false, -500, false, true, 800, 85, 21, false,
                    2, 0);
            case 45683 -> new SourceNpcPhysicalStats(npcId, -109, true, 0, true,
                    0, 8, 180, false, -500, false, true, 5_000, 83, 20, false,
                    2, 0);
            case 45684 -> new SourceNpcPhysicalStats(npcId, -109, true, 0, true,
                    0, 4, 180, false, -500, false, true, 4_500, 83, 20, false,
                    2, 0);
            case 45065 -> new SourceNpcPhysicalStats(npcId, 8, false, 0, false,
                    1, 2, 5, true, -8, true, false, 12, 14, 6, true);
            case 81183, 81184, 81185, 81186, 81187, 81188 ->
                    new SourceNpcPhysicalStats(npcId, 8, false, 0, false,
                            1, 2, 5, true, -8, true, false, 12, 16, 6, true);
            default -> new SourceNpcPhysicalStats(npcId, 10, false, 0, false);
        };
    }
}

/** Exact selected-source NPC fields needed to materialize native summons. */
record SourceSummonTemplate(
        int npcId, String name, String nameId, int gfx, int level,
        int maxHp, int maxMp, int armorClass, int strength, int dexterity,
        int moveIntervalMillis, int attackIntervalMillis) {
    SourceSummonTemplate {
        if (!supportedNpcId(npcId) || name == null || name.isBlank()
                || nameId == null || nameId.isBlank() || gfx <= 0 || gfx > 0xFFFF
                || level <= 0 || level > 0xFF || maxHp <= 0 || maxMp < 0
                || strength <= 0 || dexterity <= 0
                || moveIntervalMillis <= 0 || attackIntervalMillis <= 0) {
            throw new IllegalArgumentException("invalid source summon template");
        }
    }

    static SourceSummonTemplate memoryFixture(int npcId) {
        return switch (npcId) {
            case 45303 -> source(npcId, "火之精靈", "$2004", 2276, 30, 400, 0, -11, 16, 18, 640, 1000);
            case 45304 -> source(npcId, "水之精靈", "$2005", 2274, 30, 400, 0, -13, 16, 18, 640, 1000);
            case 45305 -> source(npcId, "風之精靈", "$2006", 2275, 30, 400, 0, -13, 16, 18, 640, 1000);
            case 45306 -> source(npcId, "土之精靈", "$2007", 2273, 30, 400, 0, -15, 16, 18, 640, 1000);
            case 81050 -> source(npcId, "強力火之精靈", "$2008", 2266, 45, 600, 60, -18, 20, 18, 640, 980);
            case 81051 -> source(npcId, "強力水之精靈", "$2009", 2268, 45, 600, 60, -18, 18, 18, 640, 980);
            case 81052 -> source(npcId, "強力風之精靈", "$2010", 2282, 45, 720, 60, -18, 17, 18, 640, 980);
            case 81053 -> source(npcId, "強力土之精靈", "$2011", 2267, 45, 650, 60, -18, 19, 18, 640, 980);
            case 81210 -> source(npcId, "哈柏哥布林", "$1111", 1206, 20, 300, 20, 1, 13, 13, 640, 960);
            case 81211 -> source(npcId, "艾多倫", "$1043", 1150, 20, 180, 20, 10, 11, 11, 320, 960);
            case 81212 -> source(npcId, "安普", "$1044", 1152, 20, 120, 60, 6, 9, 14, 480, 1520);
            case 81213 -> source(npcId, "甘地妖魔", "$496", 784, 24, 350, 30, 0, 10, 10, 640, 1040);
            case 81214 -> source(npcId, "都達瑪拉妖魔", "$498", 788, 24, 210, 30, -5, 12, 14, 640, 1160);
            case 81215 -> source(npcId, "妖魔巡守", "$511", 790, 24, 140, 70, -3, 14, 14, 640, 1520);
            case 81216 -> source(npcId, "狂野毒牙", "$3379", 4207, 28, 400, 40, -6, 17, 13, 640, 920);
            case 81217 -> source(npcId, "狂野之毒", "$3378", 4205, 28, 240, 80, 0, 16, 14, 640, 680);
            case 81218 -> source(npcId, "狂野之魔", "$3380", 4203, 28, 160, 80, -10, 18, 12, 640, 880);
            case 81219 -> source(npcId, "食人妖精", "$325", 152, 32, 450, 50, -6, 19, 12, 640, 840);
            case 81220 -> source(npcId, "食人妖精王", "$937", 894, 32, 270, 50, -8, 21, 12, 640, 720);
            case 81221 -> source(npcId, "冰人", "$1791", 2158, 32, 180, 90, -20, 17, 17, 960, 1600);
            case 81222 -> source(npcId, "狂暴蜥蜴人", "$3381", 4149, 36, 500, 50, -4, 14, 16, 640, 960);
            case 81223 -> source(npcId, "重裝蜥蜴人", "$3383", 4151, 36, 300, 50, -15, 14, 18, 640, 1200);
            case 81224 -> source(npcId, "高等蜥蜴人", "$3382", 4192, 36, 200, 100, -10, 10, 16, 640, 1000);
            case 81225 -> source(npcId, "火蜥蜴", "$1505", 1666, 40, 550, 60, -23, 17, 15, 640, 1080);
            case 81226 -> source(npcId, "火焰戰士", "$1566", 1770, 40, 330, 60, -11, 15, 11, 960, 1520);
            case 81227 -> source(npcId, "火焰弓箭手", "$1572", 1832, 40, 220, 110, -5, 13, 16, 960, 2000);
            case 81228 -> source(npcId, "魔熊", "$2893", 3357, 44, 600, 40, -25, 14, 12, 426, 840);
            case 81229 -> source(npcId, "魔狼", "$2105", 2392, 48, 360, 70, -30, 22, 18, 480, 1440);
            case 81230 -> source(npcId, "魔蝙蝠", "$2892", 3415, 48, 240, 120, -6, 19, 12, 853, 1560);
            case 81231 -> source(npcId, "巨大守護螞蟻", "$5285", 6290, 48, 650, 60, -48, 20, 20, 480, 880);
            case 81232 -> source(npcId, "強化白螞蟻群", "$5282", 6161, 48, 390, 60, -12, 20, 20, 640, 880);
            case 81233 -> source(npcId, "巨大強化白螞蟻", "$5284", 6302, 48, 260, 130, -24, 15, 13, 640, 880);
            case 81234 -> source(npcId, "地獄奴隸", "$3812", 4622, 52, 700, 60, -41, 16, 15, 960, 960);
            case 81235 -> source(npcId, "闇精靈王", "$2896", 3365, 52, 420, 60, -40, 17, 16, 960, 1400);
            case 81236 -> source(npcId, "食腐獸", "$4306", 5442, 52, 280, 140, -50, 15, 20, 960, 1120);
            case 81237 -> source(npcId, "地獄束縛犬", "$3811", 4643, 50, 657, 278, -40, 16, 15, 640, 1040);
            case 81238 -> source(npcId, "變形怪", "$1554", 2389, 58, 1000, 100, -30, 20, 18, 1040, 1440);
            case 81239 -> source(npcId, "巨大牛人", "$2106", 2402, 58, 1000, 100, -65, 30, 12, 960, 1520);
            case 81240 -> source(npcId, "黑豹", "$2587", 3049, 80, 4000, 400, -80, 85, 63, 480, 440);
            default -> throw new IllegalArgumentException(
                    "unknown memory summon NPC id: " + npcId);
        };
    }

    static boolean supportedNpcId(int npcId) {
        return (npcId >= 45303 && npcId <= 45306)
                || (npcId >= 81050 && npcId <= 81053)
                || (npcId >= 81210 && npcId <= 81240);
    }

    private static SourceSummonTemplate source(
            int npcId, String name, String nameId, int gfx, int level,
            int maxHp, int maxMp, int armorClass, int strength, int dexterity,
            int moveIntervalMillis, int attackIntervalMillis) {
        return new SourceSummonTemplate(npcId, name, nameId, gfx, level,
                maxHp, maxMp, armorClass, strength, dexterity,
                moveIntervalMillis, attackIntervalMillis);
    }
}

/** Immutable character_pets row whose collar remains in the same source character. */
record SourcePet(
        long sourceCollarObjectId, long sourcePetObjectId,
        int collarCatalogId, int npcId, String name, int level,
        int maxHp, int maxMp, long experience, int lawful,
        boolean litoOwned) {
    /** Compatibility for legacy/read-only repositories without ownership metadata. */
    SourcePet(long sourceCollarObjectId, long sourcePetObjectId,
              int collarCatalogId, int npcId, String name, int level,
              int maxHp, int maxMp, long experience, int lawful) {
        this(sourceCollarObjectId, sourcePetObjectId, collarCatalogId, npcId,
                name, level, maxHp, maxMp, experience, lawful, false);
    }
    SourcePet {
        if (sourceCollarObjectId <= 0 || sourceCollarObjectId > 0xFFFF_FFFFL
                || sourcePetObjectId <= 0 || sourcePetObjectId > 0xFFFF_FFFFL
                || (collarCatalogId != InventoryCatalog.PET_COLLAR
                    && collarCatalogId != InventoryCatalog.HIGH_PET_COLLAR)
                || npcId <= 0 || name == null || name.isBlank()
                || level <= 0 || level > 0xFF || maxHp <= 0 || maxMp < 0
                || experience < 0 || experience > 0xFFFF_FFFFL
                || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE) {
            throw new IllegalArgumentException("invalid selected-source pet row");
        }
    }
}

/** Exact pet row paired with either a protected source collar or a LiTo-owned collar. */
record LitoPetPersistence(
        long collarObjectId, long petObjectId, int collarCatalogId,
        int npcId, String name, int level, int maxHp, int maxMp,
        long experience, int lawful, boolean litoOwned,
        long sourceCollarObjectId) {
    LitoPetPersistence {
        if (collarObjectId <= 0 || collarObjectId > 0xFFFF_FFFFL
                || petObjectId <= 0 || petObjectId > 0xFFFF_FFFFL
                || (collarCatalogId != InventoryCatalog.PET_COLLAR
                    && collarCatalogId != InventoryCatalog.HIGH_PET_COLLAR)
                || npcId <= 0 || name == null || name.isBlank()
                || level <= 0 || level > 0xFF || maxHp <= 0 || maxMp < 0
                || experience < 0 || experience > 0xFFFF_FFFFL
                || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE
                || sourceCollarObjectId < 0
                || sourceCollarObjectId > 0xFFFF_FFFFL
                || !litoOwned && sourceCollarObjectId == 0) {
            throw new IllegalArgumentException("invalid LiTo pet persistence row");
        }
    }
}

/** Exact selected npc+pettypes fields required for a persistent pet actor. */
record SourcePetTemplate(
        int npcId, String name, String nameId, int gfx, int sourceLevel,
        int baseMaxHp, int baseMaxMp, int armorClass, int strength, int dexterity,
        int moveIntervalMillis, int attackIntervalMillis,
        int collarCatalogId, int charismaCost,
        int tamingItemCatalogId, int tameRate,
        int hpUpMin, int hpUpMax, int mpUpMin, int mpUpMax,
        int minimumEvolutionLevel, int evolutionItemCatalogId, int evolutionNpcId,
        int constitution, int wisdom, int intelligence, int magicResistance,
        long baseExperience, int lawful, String size, int weakAttribute, int ranged,
        int bowActionId, int undead, int poisonAttack, int paralysisAttack,
        boolean aggressive, int damageReduction, boolean hard, boolean doppel,
        boolean boss, int lightSize, boolean changeHead) {
    SourcePetTemplate {
        if (npcId <= 0 || name == null || name.isBlank()
                || nameId == null || nameId.isBlank() || gfx <= 0 || gfx > 0xFFFF
                || sourceLevel <= 0 || sourceLevel > 0xFF
                || baseMaxHp <= 0 || baseMaxMp < 0
                || strength <= 0 || dexterity <= 0
                || moveIntervalMillis <= 0 || attackIntervalMillis <= 0
                || (collarCatalogId != InventoryCatalog.PET_COLLAR
                    && collarCatalogId != InventoryCatalog.HIGH_PET_COLLAR)
                || charismaCost <= 0 || tamingItemCatalogId < 0
                || tameRate <= 0 || tameRate > 10000
                || hpUpMin <= 0 || hpUpMax < hpUpMin
                || mpUpMin < 0 || mpUpMax < mpUpMin
                || minimumEvolutionLevel < 0 || minimumEvolutionLevel > 0xFF
                || ((minimumEvolutionLevel == 0)
                    != (evolutionItemCatalogId == 0 && evolutionNpcId == 0))
                || (minimumEvolutionLevel > 0
                    && (evolutionItemCatalogId <= 0 || evolutionNpcId <= 0))
                || constitution <= 0 || wisdom <= 0 || intelligence <= 0
                || magicResistance < 0 || baseExperience < 0
                || baseExperience > 0xFFFF_FFFFL
                || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE
                || size == null || (!size.equals("small") && !size.equals("large"))
                || ranged < 0 || bowActionId < 0 || bowActionId > 0xFFFF
                || undead < 0 || poisonAttack < 0 || paralysisAttack < 0
                || damageReduction < 0 || lightSize < 0 || lightSize > 0xFF) {
            throw new IllegalArgumentException("invalid selected-source pet template");
        }
    }

    boolean canEvolve() {
        return minimumEvolutionLevel > 0;
    }

    ClassicWorldMonsterCatalog.MonsterTemplate releasedMonsterTemplate(
            BattleCompanion.Snapshot pet) {
        if (pet == null || pet.npcId() != npcId || pet.level() <= 0
                || pet.maxHp() <= 0 || pet.maxMp() < 0) {
            throw new IllegalArgumentException("released pet does not match source template");
        }
        return new ClassicWorldMonsterCatalog.MonsterTemplate(
                npcId, nameId, "L1Monster", gfx, pet.level(), pet.maxHp(), pet.maxMp(),
                armorClass, strength, constitution, dexterity, wisdom, intelligence,
                magicResistance, baseExperience, lawful, size, weakAttribute, ranged,
                moveIntervalMillis, attackIntervalMillis, bowActionId, undead,
                poisonAttack, paralysisAttack, aggressive, damageReduction, hard,
                doppel, boss, lightSize, changeHead);
    }

    static SourcePetTemplate memoryFixture(int npcId) {
        return switch (npcId) {
            case 45034 -> new SourcePetTemplate(
                    45034, "牧羊犬", "$907", 936, 5,
                    30, 5, 9, 15, 15, 560, 960,
                    (int) InventoryCatalog.PET_COLLAR, 6, 40057, 1500,
                    5, 8, 1, 2,
                    30, (int) InventoryCatalog.EVOLUTION_FRUIT, 45687,
                    12, 7, 7, 1, 26, 0, "small", 0, 1,
                    0, 0, 0, 0, false, 0, false, false,
                    false, 0, false);
            case 45687 -> new SourcePetTemplate(
                    45687, "高等牧羊犬", "$2694", 3184, 5,
                    30, 5, 9, 15, 15, 560, 1000,
                    (int) InventoryCatalog.HIGH_PET_COLLAR, 6, 0, 1500,
                    5, 8, 1, 2,
                    45, (int) InventoryCatalog.VICTORY_FRUIT, 46046,
                    12, 7, 7, 1, 26, 0, "small", 0, 1,
                    0, 0, 0, 0, false, 0, false, false,
                    false, 0, false);
            case 46046 -> new SourcePetTemplate(
                    46046, "黃金龍", "$4076", 6396, 5,
                    60, 40, 30, 16, 16, 480, 800,
                    (int) InventoryCatalog.HIGH_PET_COLLAR, 6, 0, 1500,
                    5, 10, 4, 8,
                    0, 0, 0,
                    16, 10, 15, 15, 26, 0, "small", 0, 1,
                    0, 0, 0, 0, false, 0, false, false,
                    false, 0, false);
            default -> throw new IllegalArgumentException(
                    "unknown memory pet NPC id: " + npcId);
        };
    }
}

/** Immutable source weapon material/blessing, critical and elemental fields. */
record SourceWeaponCombatTraits(
        int material, int blessing,
        int attributeKind, int attributeLevel, int attributeDamage,
        int doubleDamageChance, double doubleDamageMultiplier,
        int maximumDamageChance,
        boolean doubleDamageEffect, boolean maximumDamageEffect,
        int inventoryGfx) {
    SourceWeaponCombatTraits(
            int material, int blessing,
            int attributeKind, int attributeLevel, int attributeDamage) {
        this(material, blessing, attributeKind, attributeLevel, attributeDamage,
                0, 0.0D, 0, false, false, 0);
    }

    SourceWeaponCombatTraits(
            int material, int blessing,
            int attributeKind, int attributeLevel, int attributeDamage,
            int doubleDamageChance, double doubleDamageMultiplier,
            int maximumDamageChance,
            boolean doubleDamageEffect, boolean maximumDamageEffect) {
        this(material, blessing, attributeKind, attributeLevel, attributeDamage,
                doubleDamageChance, doubleDamageMultiplier, maximumDamageChance,
                doubleDamageEffect, maximumDamageEffect, 0);
    }

    SourceWeaponCombatTraits {
        if (material < 0 || material > 22 || blessing < 0 || blessing > 2
                || !switch (attributeKind) {
                    case 0, 1, 2, 4, 8, 16, 32, 64, 128 -> true;
                    default -> false;
                }
                || attributeLevel < 0 || attributeDamage < 0
                || (attributeKind == 0) != (attributeLevel == 0)
                || (attributeLevel == 0) != (attributeDamage == 0)
                || doubleDamageChance < 0 || doubleDamageChance > 100
                || !Double.isFinite(doubleDamageMultiplier)
                || doubleDamageMultiplier < 0.0D
                || maximumDamageChance < 0 || maximumDamageChance > 100
                || inventoryGfx < 0 || inventoryGfx > 0xFFFF
                || (doubleDamageChance == 0) != (doubleDamageMultiplier == 0.0D)) {
            throw new IllegalArgumentException("invalid source weapon combat traits");
        }
    }

    static SourceWeaponCombatTraits none() {
        return new SourceWeaponCombatTraits(
                0, 1, 0, 0, 0, 0, 0.0D, 0, false, false, 0);
    }
}

/** Exact first usable arrow/sting chosen by the retained inventory scan. */
record SourceProjectilePhysicalStats(
        long objectId, int itemId, int useType, int damageSmall, int damageLarge,
        int material, int blessing,
        int arrowGfx, boolean consumeOnAttack, long count) {
    SourceProjectilePhysicalStats(
            int itemId, int useType, int damageSmall, int damageLarge,
            int arrowGfx, boolean consumeOnAttack, long count) {
        this(itemId, itemId, useType, damageSmall, damageLarge, 0, 1,
                arrowGfx, consumeOnAttack, count);
    }

    SourceProjectilePhysicalStats(
            long objectId, int itemId, int useType, int damageSmall, int damageLarge,
            int arrowGfx, boolean consumeOnAttack, long count) {
        this(objectId, itemId, useType, damageSmall, damageLarge, 0, 1,
                arrowGfx, consumeOnAttack, count);
    }

    SourceProjectilePhysicalStats {
        if (objectId < 0 || objectId > 0xFFFF_FFFFL || itemId < 0
                || (useType != 0 && useType != -2 && useType != -3)
                || damageSmall < 0 || damageLarge < 0
                || material < 0 || material > 22 || blessing < 0 || blessing > 2
                || arrowGfx < -1 || arrowGfx > 0xFFFF || count < 0) {
            throw new IllegalArgumentException("invalid source projectile snapshot");
        }
        if ((objectId == 0) != (itemId == 0)
                || (itemId == 0) != (useType == 0)
                || (itemId == 0) != (count == 0)) {
            throw new IllegalArgumentException("incomplete source projectile snapshot");
        }
    }

    static SourceProjectilePhysicalStats none() {
        return new SourceProjectilePhysicalStats(
                0, 0, 0, 0, 0, 0, 1, -1, false, 0);
    }

    boolean present() {
        return itemId != 0;
    }

    int damageMaximum(boolean targetLarge) {
        return Math.max(1, targetLarge ? damageLarge : damageSmall);
    }

    int effectGfx() {
        if (!present()) throw new IllegalStateException("empty projectile has no effect gfx");
        if (arrowGfx > 0) return arrowGfx;
        return useType == -3 ? 2989 : 66;
    }

    SourceProjectilePhysicalStats withObjectId(long value) {
        if (!present()) {
            throw new IllegalStateException("empty projectile has no object id");
        }
        return new SourceProjectilePhysicalStats(
                value, itemId, useType, damageSmall, damageLarge,
                material, blessing, arrowGfx, consumeOnAttack, count);
    }
}

/** Exact equipped weapon row retained from the immutable source database. */
record SourceWeaponPhysicalStats(
        int itemId, int weaponType, int damageSmall, int damageLarge, int range,
        int hitModifier, int damageModifier, int enchantLevel, int durability,
        int abilityMeleeHit, int abilityMeleeDamage,
        int abilityRangedHit, int abilityRangedDamage,
        SourceWeaponCombatTraits combatTraits,
        SourceProjectilePhysicalStats projectile,
        String nameId) {
    SourceWeaponPhysicalStats(
            int itemId, int weaponType, int damageSmall, int damageLarge, int range,
            int hitModifier, int damageModifier, int enchantLevel, int durability,
            int abilityMeleeHit, int abilityMeleeDamage,
            int abilityRangedHit, int abilityRangedDamage,
            SourceWeaponCombatTraits combatTraits,
            SourceProjectilePhysicalStats projectile) {
        this(itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage,
                combatTraits, projectile, "");
    }

    SourceWeaponPhysicalStats(
            int itemId, int weaponType, int damageSmall, int damageLarge, int range,
            int hitModifier, int damageModifier, int enchantLevel, int durability,
            int abilityMeleeHit, int abilityMeleeDamage,
            int abilityRangedHit, int abilityRangedDamage) {
        this(itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage,
                SourceWeaponCombatTraits.none(),
                SourceProjectilePhysicalStats.none(), "");
    }

    SourceWeaponPhysicalStats(
            int itemId, int weaponType, int damageSmall, int damageLarge, int range,
            int hitModifier, int damageModifier, int enchantLevel, int durability) {
        this(itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                0, 0, 0, 0);
    }

    SourceWeaponPhysicalStats {
        nameId = nameId == null ? "" : nameId;
        if (combatTraits == null || projectile == null
                || itemId < 0 || weaponType < 0 || weaponType > 18
                || damageSmall < 0 || damageLarge < 0 || range < -1
                || (range == -1 && weaponType != 4 && weaponType != 10 && weaponType != 13)
                || enchantLevel < NumericBreakthrough.MIN_ABILITY_MODIFIER
                || enchantLevel > NumericBreakthrough.MAX_ABILITY_MODIFIER
                || durability < 0 || durability > 0x7F) {
            throw new IllegalArgumentException("invalid source weapon physical snapshot");
        }
        if ((itemId == 0) != (weaponType == 0)) {
            throw new IllegalArgumentException("incomplete source weapon physical snapshot");
        }
        if (itemId == 0 && (damageSmall != 0 || damageLarge != 0 || range != 0
                || hitModifier != 0 || damageModifier != 0
                || enchantLevel != 0 || durability != 0
                || abilityMeleeHit != 0 || abilityMeleeDamage != 0
                || abilityRangedHit != 0 || abilityRangedDamage != 0
                || !combatTraits.equals(SourceWeaponCombatTraits.none())
                || projectile.present() || !nameId.isEmpty())) {
            throw new IllegalArgumentException("empty source weapon carries physical values");
        }
    }

    static SourceWeaponPhysicalStats none() {
        return new SourceWeaponPhysicalStats(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                SourceWeaponCombatTraits.none(),
                SourceProjectilePhysicalStats.none(), "");
    }

    boolean present() {
        return itemId != 0;
    }

    boolean ranged() {
        return weaponType == 4 || weaponType == 10 || weaponType == 13;
    }

    /** Retained L1AttackPc applies the independent ER roll to bows, not gauntlets. */
    boolean bow() {
        return weaponType == 4 || weaponType == 13;
    }

    int walkAction() {
        return switch (weaponType) {
            case 0 -> BattleActionCodes.WALK;
            case 1 -> BattleActionCodes.SWORD_WALK;
            case 2 -> BattleActionCodes.DAGGER_WALK;
            case 3 -> BattleActionCodes.TWO_HAND_SWORD_WALK;
            case 4, 13 -> BattleActionCodes.BOW_WALK;
            case 5, 14 -> BattleActionCodes.SPEAR_WALK;
            case 6, 15 -> BattleActionCodes.AXE_WALK;
            case 7, 16 -> BattleActionCodes.STAFF_WALK;
            case 8, 10 -> BattleActionCodes.THROWING_KNIFE_WALK;
            case 11 -> BattleActionCodes.CLAW_WALK;
            case 12 -> BattleActionCodes.EDORYU_WALK;
            case 17 -> BattleActionCodes.CLAW_WALK;
            case 18 -> BattleActionCodes.CHAINSWORD_WALK;
            default -> throw new IllegalStateException("unsupported source weapon type");
        };
    }

    int effectiveEnchant(boolean rangedAttack) {
        return rangedAttack ? enchantLevel : enchantLevel - durability;
    }

    int attackMaterial() {
        return ranged() && projectile.present()
                ? projectile.material() : combatTraits.material();
    }

    int attackBlessing() {
        return ranged() && projectile.present()
                ? projectile.blessing() : combatTraits.blessing();
    }

    SourceWeaponPhysicalStats withCombatTraits(SourceWeaponCombatTraits value) {
        if (!present() || value == null) {
            throw new IllegalStateException("source weapon cannot accept combat traits");
        }
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage, value, projectile, nameId);
    }

    SourceWeaponPhysicalStats withAbility(
            int meleeHit, int meleeDamage, int rangedHit, int rangedDamage) {
        if (!present()) throw new IllegalStateException("empty source weapon has no ability");
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                meleeHit, meleeDamage, rangedHit, rangedDamage,
                combatTraits, projectile, nameId);
    }

    SourceWeaponPhysicalStats withProjectile(SourceProjectilePhysicalStats value) {
        if (!present() || !ranged() || value == null || !value.present()) {
            throw new IllegalStateException("source weapon cannot accept projectile");
        }
        int requiredUseType = weaponType == 10 ? -3 : -2;
        if (value.useType() != requiredUseType) {
            throw new IllegalArgumentException("source projectile does not match weapon type");
        }
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage, combatTraits, value, nameId);
    }

    SourceWeaponPhysicalStats withoutProjectile() {
        if (!present()) {
            throw new IllegalStateException("empty source weapon has no projectile state");
        }
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage, combatTraits,
                SourceProjectilePhysicalStats.none(), nameId);
    }

    SourceWeaponPhysicalStats withNameId(String value) {
        if (!present() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("source weapon requires a native name id");
        }
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage,
                combatTraits, projectile, value);
    }

    SourceWeaponPhysicalStats withEnchantLevel(int value) {
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, value, durability,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage,
                combatTraits, projectile, nameId);
    }

    SourceWeaponPhysicalStats withDurability(int value) {
        if (!present() || value < 0 || value > 0x7F) {
            throw new IllegalArgumentException("invalid source weapon durability");
        }
        return new SourceWeaponPhysicalStats(
                itemId, weaponType, damageSmall, damageLarge, range,
                hitModifier, damageModifier, enchantLevel, value,
                abilityMeleeHit, abilityMeleeDamage,
                abilityRangedHit, abilityRangedDamage,
                combatTraits, projectile, nameId);
    }
}

/**
 * Source-qualified physical equipment layers, kept separate to prevent the
 * retained weapon template and normal enchant from being applied twice.
 */
/** Direct and set-derived maximum-vital/charisma layers omitted by older snapshots. */
record SourceEquipmentVitalStats(
        int itemMaximumHp, int setMaximumHp,
        int itemMaximumMp, int setMaximumMp,
        int itemCharisma, int setCharisma,
        int itemPotionRecovery, int setPotionRecovery) {
    static SourceEquipmentVitalStats none() {
        return new SourceEquipmentVitalStats(0, 0, 0, 0, 0, 0, 0, 0);
    }

    int totalMaximumHp() {
        return NumericBreakthrough.clampExtendedValue(
                (long) itemMaximumHp + setMaximumHp);
    }

    int totalMaximumMp() {
        return NumericBreakthrough.clampExtendedValue(
                (long) itemMaximumMp + setMaximumMp);
    }

    int effectiveCharisma(int baseCharisma) {
        return NumericBreakthrough.clampAbility(
                (long) baseCharisma + itemCharisma + setCharisma);
    }

    int totalPotionRecovery() {
        return Math.addExact(itemPotionRecovery, setPotionRecovery);
    }
}

/** One exact evaluation of the retained source equipment pipeline. */
record SourceEquipmentEvaluation(
        SourceEquipmentMagicStats magic, SourceEquipmentVitalStats vital) {
    SourceEquipmentEvaluation {
        if (magic == null || vital == null) {
            throw new IllegalArgumentException("incomplete source equipment evaluation");
        }
    }

    static SourceEquipmentEvaluation none() {
        return new SourceEquipmentEvaluation(
                SourceEquipmentMagicStats.none(), SourceEquipmentVitalStats.none());
    }
}

record SourceEquipmentElementalStats(
        int itemEarth, int itemFire, int itemWater, int itemWind,
        int setEarth, int setFire, int setWater, int setWind) {
    static SourceEquipmentElementalStats none() {
        return new SourceEquipmentElementalStats(0, 0, 0, 0, 0, 0, 0, 0);
    }

    int resistance(int attributeKind) {
        return switch (attributeKind) {
            case 1 -> Math.addExact(itemEarth, setEarth);
            case 2 -> Math.addExact(itemFire, setFire);
            case 4 -> Math.addExact(itemWater, setWater);
            case 8 -> Math.addExact(itemWind, setWind);
            default -> 0;
        };
    }
}

/**
 * One read-only equipped shield/guarder projected into LiTo's native
 * inventory so the original client-side skill-90 use-type-25 gate can run.
 */
record SourceDefensiveEquipment(
        int itemId, int armorType, int icon, int blessing, String nameId) {
    SourceDefensiveEquipment {
        nameId = nameId == null ? "" : nameId;
        if (itemId == 0) {
            if (armorType != 0 || icon != 0 || blessing != 1 || !nameId.isEmpty()) {
                throw new IllegalArgumentException(
                        "empty source defensive equipment carries values");
            }
        } else if (itemId < 0 || (armorType != 7 && armorType != 13)
                || icon <= 0 || icon > 0xFFFF
                || blessing < 0 || blessing > 3 || nameId.isBlank()) {
            throw new IllegalArgumentException(
                    "invalid source defensive equipment projection");
        }
    }

    static SourceDefensiveEquipment none() {
        return new SourceDefensiveEquipment(0, 0, 0, 1, "");
    }

    static SourceDefensiveEquipment memoryShield() {
        // Exact retained 380_db steel-shield resource: item 20220,
        // invgfx 508, name-id $1210, native armor type 7.
        return new SourceDefensiveEquipment(20220, 7, 508, 1, "$1210");
    }

    static SourceDefensiveEquipment memoryGuarder() {
        // The isolated fixture needs only a selected-client-valid native
        // marker; production always supplies the retained resource identity.
        return new SourceDefensiveEquipment(20220, 13, 508, 1, "$1210");
    }

    boolean present() {
        return itemId != 0;
    }

    int useType() {
        return present() ? 25 : 0;
    }
}

record SourceEquipmentPhysicalStats(
        SourceWeaponPhysicalStats weapon,
        int itemMeleeHit, int itemMeleeDamage,
        int itemRangedHit, int itemRangedDamage,
        int armorMeleeHit, int armorMeleeDamage,
        int armorRangedHit, int armorRangedDamage,
        int setMeleeHit, int setMeleeDamage,
        int setRangedHit, int setRangedDamage,
        int itemDexterity, int setDexterity,
        int itemPierce, int setPierce,
        int itemIgnoreReduction, int setIgnoreReduction,
        int itemMeleeReduction, int itemRangedReduction,
        int itemArmorClassModifier, int setArmorClassValue,
        int itemEvasionRate, int setEvasionRate,
        SourceEquipmentElementalStats elemental,
        boolean shieldEquipped, boolean guarderEquipped,
        SourceDefensiveEquipment defensiveEquipment,
        int pvpReduction) {
    /** Compatibility constructor for snapshots before PvP reduction was retained. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue,
            int itemEvasionRate, int setEvasionRate,
            SourceEquipmentElementalStats elemental,
            boolean shieldEquipped, boolean guarderEquipped,
            SourceDefensiveEquipment defensiveEquipment) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue,
                itemEvasionRate, setEvasionRate, elemental,
                shieldEquipped, guarderEquipped, defensiveEquipment, 0);
    }

    /** Compatibility constructor for snapshots before defensive-slot identity was retained. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue,
            int itemEvasionRate, int setEvasionRate,
            SourceEquipmentElementalStats elemental) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue,
                itemEvasionRate, setEvasionRate, elemental, false, false,
                SourceDefensiveEquipment.none());
    }

    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            boolean shieldEquipped, boolean guarderEquipped) {
        this(weapon,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none(),
                shieldEquipped, guarderEquipped,
                shieldEquipped ? SourceDefensiveEquipment.memoryShield()
                        : guarderEquipped
                                ? SourceDefensiveEquipment.memoryGuarder()
                                : SourceDefensiveEquipment.none());
    }
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before retained PvP defense was imported. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                0, 0, 0, 0, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before AC reconstruction was imported. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before four-element defense. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before equipment ER was qualified. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue,
            SourceEquipmentElementalStats elemental) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue, 0, 0, elemental);
    }

    SourceEquipmentPhysicalStats {
        if (weapon == null) {
            throw new IllegalArgumentException("source physical snapshot has no weapon state");
        }
        if (elemental == null) {
            throw new IllegalArgumentException("source physical snapshot has no elemental state");
        }
        if (defensiveEquipment == null || shieldEquipped && guarderEquipped
                || shieldEquipped != (defensiveEquipment.present()
                        && defensiveEquipment.armorType() == 7)
                || guarderEquipped != (defensiveEquipment.present()
                        && defensiveEquipment.armorType() == 13)) {
            throw new IllegalArgumentException(
                    "source defensive equipment identity does not match slot state");
        }
        if (pvpReduction < 0) {
            throw new IllegalArgumentException("negative source PvP reduction");
        }
    }

    static SourceEquipmentPhysicalStats none() {
        return new SourceEquipmentPhysicalStats(
                SourceWeaponPhysicalStats.none(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    int totalHit(boolean rangedAttack) {
        return totalHit(rangedAttack, true);
    }

    int totalHit(boolean rangedAttack, boolean sourceWeaponActive) {
        int equipment = rangedAttack
                ? itemRangedHit + armorRangedHit + setRangedHit
                : itemMeleeHit + armorMeleeHit + setMeleeHit;
        if (!sourceWeaponActive || !weapon.present()) return equipment;
        equipment = Math.addExact(equipment,
                rangedAttack ? weapon.abilityRangedHit() : weapon.abilityMeleeHit());
        return Math.addExact(equipment,
                Math.addExact(weapon.hitModifier(),
                        (int) (weapon.enchantLevel() * 0.6)));
    }

    int totalDamageBonus(boolean rangedAttack) {
        return totalDamageBonus(rangedAttack, true);
    }

    int totalDamageBonus(boolean rangedAttack, boolean sourceWeaponActive) {
        int equipment = rangedAttack
                ? itemRangedDamage + armorRangedDamage + setRangedDamage
                : itemMeleeDamage + armorMeleeDamage + setMeleeDamage;
        if (!sourceWeaponActive || !weapon.present()) return equipment;
        equipment = Math.addExact(equipment,
                rangedAttack ? weapon.abilityRangedDamage() : weapon.abilityMeleeDamage());
        return Math.addExact(equipment,
                Math.addExact(weapon.damageModifier(),
                        weapon.effectiveEnchant(rangedAttack)));
    }

    int effectiveDexterity(int baseDexterity) {
        return NumericBreakthrough.clampAbility(
                (long) baseDexterity + itemDexterity + setDexterity);
    }

    int totalPierce() {
        return Math.addExact(itemPierce, setPierce);
    }

    int totalIgnoreReduction() {
        return Math.addExact(itemIgnoreReduction, setIgnoreReduction);
    }

    int directionalReduction(boolean rangedAttack) {
        return rangedAttack ? itemRangedReduction : itemMeleeReduction;
    }

    int totalPvpReduction() {
        return pvpReduction;
    }

    int totalEvasionRate() {
        return Math.addExact(itemEvasionRate, setEvasionRate);
    }

    /** Selected retained configuration accepts either armor type 7 or 13. */
    boolean supportsSolidCarriage() {
        return shieldEquipped || guarderEquipped;
    }

    /** Mirrors L1PcInstance.getAc: direct item AC, then subtract set AC. */
    int armorClassModifier() {
        return NumericBreakthrough.clampExtendedValue(
                (long) itemArmorClassModifier - setArmorClassValue);
    }

    /** Adds only LiTo-owned selected-template effects; source rows stay untouched. */
    SourceEquipmentPhysicalStats withRentalEquipment(RentalEquipmentBonuses bonus) {
        if (bonus == null || bonus.matchedItems() == 0) return this;
        SourceEquipmentElementalStats withElemental =
                new SourceEquipmentElementalStats(
                        Math.addExact(elemental.itemEarth(), bonus.earthResistance()),
                        Math.addExact(elemental.itemFire(), bonus.fireResistance()),
                        Math.addExact(elemental.itemWater(), bonus.waterResistance()),
                        Math.addExact(elemental.itemWind(), bonus.windResistance()),
                        elemental.setEarth(), elemental.setFire(),
                        elemental.setWater(), elemental.setWind());
        return new SourceEquipmentPhysicalStats(
                weapon,
                itemMeleeHit, itemMeleeDamage, itemRangedHit, itemRangedDamage,
                Math.addExact(armorMeleeHit, bonus.meleeHit()),
                Math.addExact(armorMeleeDamage, bonus.meleeDamage()),
                Math.addExact(armorRangedHit, bonus.rangedHit()),
                Math.addExact(armorRangedDamage, bonus.rangedDamage()),
                setMeleeHit, setMeleeDamage, setRangedHit, setRangedDamage,
                Math.addExact(itemDexterity, bonus.dexterity()), setDexterity,
                itemPierce, setPierce, itemIgnoreReduction, setIgnoreReduction,
                Math.addExact(itemMeleeReduction, bonus.meleeReduction()),
                Math.addExact(itemRangedReduction, bonus.rangedReduction()),
                itemArmorClassModifier, setArmorClassValue,
                itemEvasionRate, setEvasionRate, withElemental,
                shieldEquipped, guarderEquipped, defensiveEquipment, pvpReduction);
    }
}

/**
 * Persisted, source-qualified magic-relevant equipment snapshot.
 *
 * <p>Item fields are values that the retained {@code L1EquipmentSlot} applies
 * directly while each equipped row is restored. Set fields are the later
 * {@code _setAbility} aggregate, including active cross-item card combos. Set
 * Set WIS is retained for the independent natural-MP-regeneration path.  It
 * remains deliberately excluded from base MR: the retained source does not
 * re-run {@code resetBaseMr()} after rebuilding the set aggregate.  Set SP is
 * absent because {@code getTargetSp()} does not read it.</p>
 */
record SourceEquipmentMagicStats(
        int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
        int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
        int armorTemplateDamageReduction,
        int setIntelligence, int setWisdom, int setConstitution,
        int setMagicResistance, int setMagicDamage,
        int setFireResistance, int setDamageReduction,
        int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
        int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
        int matchedSingleCardRows,
        int activeSetBonuses, int activeCardCombos,
        int itemStrength, int itemWeightReduction,
        int armorTemplateWeightReduction,
        int setStrength, int setWeightReduction,
        int itemMagicHit, int setMagicHit,
        SourceEquipmentPhysicalStats physical,
        boolean healingHelmEquipped,
        int itemBlindResistance, int setBlindResistance,
        int itemSleepResistance, int setSleepResistance,
        int itemFreezeResistance, int setFreezeResistance,
        int itemStoneResistance, int setStoneResistance,
        int itemSustainResistance, int setSustainResistance,
        boolean hasteCostHalvingHelmEquipped,
        boolean greaterHasteCostHalvingHelmEquipped,
        int itemStunResistance, int setStunResistance,
        int itemStunSuccess, int setStunSuccess) {
    /** Compatibility constructor before Shock Stun resistance/success was imported. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction,
            int itemMagicHit, int setMagicHit,
            SourceEquipmentPhysicalStats physical,
            boolean healingHelmEquipped,
            int itemBlindResistance, int setBlindResistance,
            int itemSleepResistance, int setSleepResistance,
            int itemFreezeResistance, int setFreezeResistance,
            int itemStoneResistance, int setStoneResistance,
            int itemSustainResistance, int setSustainResistance,
            boolean hasteCostHalvingHelmEquipped,
            boolean greaterHasteCostHalvingHelmEquipped) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, itemMagicHit, setMagicHit,
                physical, healingHelmEquipped,
                itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance,
                itemFreezeResistance, setFreezeResistance,
                itemStoneResistance, setStoneResistance,
                itemSustainResistance, setSustainResistance,
                hasteCostHalvingHelmEquipped, greaterHasteCostHalvingHelmEquipped,
                0, 0, 0, 0);
    }
    /** Compatibility constructor before the distinct Greater Haste helm was retained. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction,
            int itemMagicHit, int setMagicHit,
            SourceEquipmentPhysicalStats physical,
            boolean healingHelmEquipped,
            int itemBlindResistance, int setBlindResistance,
            int itemSleepResistance, int setSleepResistance,
            int itemFreezeResistance, int setFreezeResistance,
            int itemStoneResistance, int setStoneResistance,
            int itemSustainResistance, int setSustainResistance,
            boolean hasteCostHalvingHelmEquipped) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, itemMagicHit, setMagicHit,
                physical, healingHelmEquipped,
                itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance,
                itemFreezeResistance, setFreezeResistance,
                itemStoneResistance, setStoneResistance,
                itemSustainResistance, setSustainResistance,
                hasteCostHalvingHelmEquipped, false);
    }
    /** Compatibility constructor before Haste-cost helm identity was retained. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction,
            int itemMagicHit, int setMagicHit,
            SourceEquipmentPhysicalStats physical,
            boolean healingHelmEquipped,
            int itemBlindResistance, int setBlindResistance,
            int itemSleepResistance, int setSleepResistance,
            int itemFreezeResistance, int setFreezeResistance,
            int itemStoneResistance, int setStoneResistance,
            int itemSustainResistance, int setSustainResistance) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, itemMagicHit, setMagicHit,
                physical, healingHelmEquipped,
                itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance,
                itemFreezeResistance, setFreezeResistance,
                itemStoneResistance, setStoneResistance,
                itemSustainResistance, setSustainResistance, false);
    }
    /** Compatibility constructor before Sustain resistance was imported. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction,
            int itemMagicHit, int setMagicHit,
            SourceEquipmentPhysicalStats physical,
            boolean healingHelmEquipped,
            int itemBlindResistance, int setBlindResistance,
            int itemSleepResistance, int setSleepResistance,
            int itemFreezeResistance, int setFreezeResistance,
            int itemStoneResistance, int setStoneResistance) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, itemMagicHit, setMagicHit,
                physical, healingHelmEquipped,
                itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance,
                itemFreezeResistance, setFreezeResistance,
                itemStoneResistance, setStoneResistance, 0, 0);
    }
    /** Compatibility constructor before status-resistance layers were retained. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction,
            int itemMagicHit, int setMagicHit,
            SourceEquipmentPhysicalStats physical,
            boolean healingHelmEquipped) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, itemMagicHit, setMagicHit,
                physical, healingHelmEquipped, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** Compatibility constructor for snapshots before equipped helm identity was retained. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction,
            int itemMagicHit, int setMagicHit,
            SourceEquipmentPhysicalStats physical) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, itemMagicHit, setMagicHit,
                physical, false);
    }

    /** Focused fixture constructor for the retained direct-plus-set magic-hit rule. */
    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none());
    }

    /** Focused fixture for Shock Stun hit, resistance and success layers. */
    SourceEquipmentMagicStats(
            int itemMagicHit, int setMagicHit,
            int itemStunResistance, int setStunResistance,
            int itemStunSuccess, int setStunSuccess) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(),
                false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                false, false,
                itemStunResistance, setStunResistance,
                itemStunSuccess, setStunSuccess);
    }

    /** Focused fixture constructor for equipment identity plus magic-hit layers. */
    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit,
                              boolean healingHelmEquipped) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(),
                healingHelmEquipped);
    }

    /** Focused fixture constructor for magic hit, helm identity and blind resistance. */
    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit,
                              boolean healingHelmEquipped,
                              int itemBlindResistance, int setBlindResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(),
                healingHelmEquipped, itemBlindResistance, setBlindResistance,
                0, 0, 0, 0, 0, 0);
    }

    /** Focused fixture for both retained direct-plus-set status resistances. */
    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit,
                              boolean healingHelmEquipped,
                              int itemBlindResistance, int setBlindResistance,
                              int itemSleepResistance, int setSleepResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(),
                healingHelmEquipped, itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance, 0, 0, 0, 0);
    }

    /** Focused fixture for all retained direct-plus-set status resistances. */
    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit,
                              boolean healingHelmEquipped,
                              int itemBlindResistance, int setBlindResistance,
                              int itemSleepResistance, int setSleepResistance,
                              int itemFreezeResistance, int setFreezeResistance,
                              int itemStoneResistance, int setStoneResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(),
                healingHelmEquipped, itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance,
                itemFreezeResistance, setFreezeResistance,
                itemStoneResistance, setStoneResistance);
    }

    /** Focused fixture including the retained Earth Bind Sustain resistance. */
    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit,
                              boolean healingHelmEquipped,
                              int itemBlindResistance, int setBlindResistance,
                              int itemSleepResistance, int setSleepResistance,
                              int itemFreezeResistance, int setFreezeResistance,
                              int itemStoneResistance, int setStoneResistance,
                              int itemSustainResistance, int setSustainResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(),
                healingHelmEquipped, itemBlindResistance, setBlindResistance,
                itemSleepResistance, setSleepResistance,
                itemFreezeResistance, setFreezeResistance,
                itemStoneResistance, setStoneResistance,
                itemSustainResistance, setSustainResistance);
    }

    SourceEquipmentMagicStats(
            SourceEquipmentPhysicalStats physical,
            int matchedEquipmentRows, int matchedArmorRows) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                matchedEquipmentRows, matchedArmorRows, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, physical);
    }

    /** Loopback fixture retaining a physical weapon plus Shock Stun success. */
    SourceEquipmentMagicStats(
            SourceEquipmentPhysicalStats physical,
            int matchedEquipmentRows, int matchedArmorRows,
            int itemStunSuccess) {
        this(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                matchedEquipmentRows, matchedArmorRows, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, physical,
                false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                false, false, 0, 0, itemStunSuccess, 0);
    }

    /** Loopback fixture constructor for retained player physical defense. */
    SourceEquipmentMagicStats(
            SourceEquipmentPhysicalStats physical,
            int armorTemplateDamageReduction, int setDamageReduction,
            int matchedEquipmentRows, int matchedArmorRows) {
        this(0, 0, 0, 0, 0, 0, 0, armorTemplateDamageReduction,
                0, 0, 0, 0, 0, 0, setDamageReduction,
                0, 0, 0, 0,
                matchedEquipmentRows, matchedArmorRows, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, physical);
    }

    /** Compatibility constructor for snapshots before physical equipment was imported. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setWisdom, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage, setFireResistance,
                setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, 0, 0,
                SourceEquipmentPhysicalStats.none());
    }
    /** Compatibility constructor for the pre-MPR source snapshot. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos,
            int itemStrength, int itemWeightReduction,
            int armorTemplateWeightReduction,
            int setStrength, int setWeightReduction) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, 0, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                itemHpRegen, setHpRegen, 0, 0,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                itemStrength, itemWeightReduction, armorTemplateWeightReduction,
                setStrength, setWeightReduction, 0, 0,
                SourceEquipmentPhysicalStats.none());
    }

    /** Compatibility constructor for the pre-weight equipment snapshot. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setConstitution,
            int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows,
            int activeSetBonuses, int activeCardCombos) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, 0, setConstitution, setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, 0, 0,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                0, 0, 0, 0, 0);
    }

    /** Compatibility constructor for pre-HPR unit fixtures. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos) {
        this(itemIntelligence, itemWisdom, 0, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, 0, setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction, 0, 0,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos);
    }

    /** Compatibility constructor for the HPR snapshot before effective CON was added. */
    SourceEquipmentMagicStats(
            int itemIntelligence, int itemWisdom, int itemSpellPower,
            int itemMagicResistance, int itemMagicDamage, int itemFireResistance,
            int armorTemplateDamageReduction,
            int setIntelligence, int setMagicResistance, int setMagicDamage,
            int setFireResistance, int setDamageReduction,
            int itemHpRegen, int setHpRegen,
            int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows,
            int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos) {
        this(itemIntelligence, itemWisdom, 0, itemSpellPower,
                itemMagicResistance, itemMagicDamage, itemFireResistance,
                armorTemplateDamageReduction,
                setIntelligence, 0, setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction, itemHpRegen, setHpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos);
    }

    SourceEquipmentMagicStats {
        if (physical == null) {
            throw new IllegalArgumentException("source equipment snapshot has no physical layer");
        }
        if (matchedEquipmentRows < 0 || matchedArmorRows < 0
                || matchedVipRows < 0 || matchedSingleCardRows < 0
                || activeSetBonuses < 0
                || activeCardCombos < 0 || matchedArmorRows > matchedEquipmentRows) {
            throw new IllegalArgumentException("invalid source equipment magic snapshot");
        }
        if (matchedVipRows > matchedEquipmentRows
                || matchedArmorRows + matchedVipRows > matchedEquipmentRows) {
            throw new IllegalArgumentException("invalid source equipment row classification");
        }
    }

    static SourceEquipmentMagicStats none() {
        return new SourceEquipmentMagicStats(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0);
    }

    int effectiveIntelligence(int baseIntelligence) {
        return NumericBreakthrough.clampAbility(
                (long) baseIntelligence + itemIntelligence + setIntelligence);
    }

    int magicIntelligenceTerm(int baseIntelligence) {
        return Math.max(1, NumericBreakthrough.clampExtendedValue(
                (long) effectiveIntelligence(baseIntelligence) + itemSpellPower));
    }

    int totalMagicDamage() {
        return itemMagicDamage + setMagicDamage;
    }

    /** Mirrors retained L1PcInstance.getMagicHit(). */
    int totalMagicHit() {
        return Math.addExact(itemMagicHit, setMagicHit);
    }

    /** Mirrors retained L1PcInstance.getRegistBlind direct-plus-set clamp. */
    int totalBlindResistance() {
        return Math.max(-128, Math.min(127,
                Math.addExact(itemBlindResistance, setBlindResistance)));
    }

    /** Mirrors retained L1PcInstance.getRegistSleep direct-plus-set clamp. */
    int totalSleepResistance() {
        return Math.max(-128, Math.min(127,
                Math.addExact(itemSleepResistance, setSleepResistance)));
    }

    /** Mirrors retained L1PcInstance.getRegistFreeze direct-plus-set clamp. */
    int totalFreezeResistance() {
        return Math.max(-128, Math.min(127,
                Math.addExact(itemFreezeResistance, setFreezeResistance)));
    }

    /** Retained for the later stone-status family; not used as freeze resistance. */
    int totalStoneResistance() {
        return Math.max(-128, Math.min(127,
                Math.addExact(itemStoneResistance, setStoneResistance)));
    }

    /** Mirrors retained L1PcInstance.getRegistSustain direct-plus-set clamp. */
    int totalSustainResistance() {
        return Math.max(-128, Math.min(127,
                Math.addExact(itemSustainResistance, setSustainResistance)));
    }

    /** Mirrors retained L1PcInstance.getRegistStun direct-plus-set clamp. */
    int totalStunResistance() {
        return Math.max(-128, Math.min(127,
                Math.addExact(itemStunResistance, setStunResistance)));
    }

    /** Mirrors retained L1PcInstance.getStunSuccess without a byte clamp. */
    int totalStunSuccess() {
        return Math.addExact(itemStunSuccess, setStunSuccess);
    }

    int totalFireResistance() {
        return NumericBreakthrough.clampExtendedValue(
                (long) itemFireResistance + setFireResistance);
    }

    int totalHpRegen() {
        return itemHpRegen + setHpRegen;
    }

    int effectiveMprWisdom(int baseWisdom) {
        return NumericBreakthrough.clampAbility(
                (long) baseWisdom + itemWisdom + setWisdom);
    }

    int totalMpRegen() {
        return itemMpRegen + setMpRegen;
    }

    int effectiveConstitution(int baseConstitution) {
        return NumericBreakthrough.clampAbility(
                (long) baseConstitution + itemConstitution + setConstitution);
    }

    int effectiveStrength(int baseStrength) {
        return NumericBreakthrough.clampAbility(
                (long) baseStrength + itemStrength + setStrength);
    }

    int totalWeightReduction() {
        return itemWeightReduction + armorTemplateWeightReduction + setWeightReduction;
    }

    int totalDirectMagicResistance() {
        return NumericBreakthrough.clampExtendedValue(
                (long) itemMagicResistance + setMagicResistance);
    }

    int armorReductionBase() {
        return armorTemplateDamageReduction + setDamageReduction;
    }

    /**
     * Layers exact equipped session-owned rental effects over the immutable
     * selected-character snapshot without changing its source row counters or
     * set/card provenance.
     */
    SourceEquipmentMagicStats withRentalEquipment(RentalEquipmentBonuses bonus) {
        if (bonus == null || bonus.matchedItems() == 0) return this;
        return new SourceEquipmentMagicStats(
                Math.addExact(itemIntelligence, bonus.intelligence()),
                Math.addExact(itemWisdom, bonus.wisdom()),
                Math.addExact(itemConstitution, bonus.constitution()),
                Math.addExact(itemSpellPower, bonus.spellPower()),
                Math.addExact(itemMagicResistance, bonus.magicResistance()),
                Math.addExact(itemMagicDamage, bonus.magicDamage()),
                Math.addExact(itemFireResistance, bonus.fireResistance()),
                Math.addExact(armorTemplateDamageReduction,
                        bonus.damageReduction()),
                setIntelligence, setWisdom, setConstitution,
                setMagicResistance, setMagicDamage,
                setFireResistance, setDamageReduction,
                Math.addExact(itemHpRegen, bonus.hpRegen()), setHpRegen,
                Math.addExact(itemMpRegen, bonus.mpRegen()), setMpRegen,
                matchedEquipmentRows, matchedArmorRows, matchedVipRows,
                matchedSingleCardRows, activeSetBonuses, activeCardCombos,
                Math.addExact(itemStrength, bonus.strength()),
                Math.addExact(itemWeightReduction, bonus.weightReduction()),
                armorTemplateWeightReduction, setStrength, setWeightReduction,
                itemMagicHit, setMagicHit,
                physical.withRentalEquipment(bonus),
                healingHelmEquipped,
                Math.addExact(itemBlindResistance, bonus.blindResistance()),
                setBlindResistance,
                Math.addExact(itemSleepResistance, bonus.sleepResistance()),
                setSleepResistance,
                Math.addExact(itemFreezeResistance, bonus.freezeResistance()),
                setFreezeResistance,
                Math.addExact(itemStoneResistance, bonus.stoneResistance()),
                setStoneResistance,
                Math.addExact(itemSustainResistance, bonus.sustainResistance()),
                setSustainResistance,
                hasteCostHalvingHelmEquipped,
                greaterHasteCostHalvingHelmEquipped,
                Math.addExact(itemStunResistance, bonus.stunResistance()),
                setStunResistance, itemStunSuccess, setStunSuccess);
    }
}

/** Immutable ownership and seed state imported from the selected legacy server. */
record CastleSummary(
        int castleId, String clanName, String leaderName, String castleName,
        long warTimeMillis, int taxRate, long publicMoney,
        long leaderObjectId) {
    CastleSummary {
        if (castleId <= 0 || clanName == null || clanName.isBlank()
                || leaderName == null || leaderName.isBlank()
                || castleName == null || castleName.isBlank()
                || warTimeMillis < 0 || taxRate < 0 || taxRate > 100
                || publicMoney < 0 || leaderObjectId < 0
                || leaderObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid legacy castle summary");
        }
    }

    CastleSummary(int castleId, String clanName, String leaderName,
                  String castleName, long warTimeMillis,
                  int taxRate, long publicMoney) {
        this(castleId, clanName, leaderName, castleName,
                warTimeMillis, taxRate, publicMoney, 0);
    }
}

/** Exact per-castle timing columns loaded from the selected server_城堡 row. */
record CastleWarSchedule(
        int castleId, String castleName, long warTimeMillis,
        int warDurationMinutes, int warIntervalMinutes) {
    CastleWarSchedule {
        if (castleId < 1 || castleId > 8 || castleName == null || castleName.isBlank()
                || warTimeMillis < 0 || warDurationMinutes <= 0
                || warIntervalMinutes <= warDurationMinutes) {
            throw new IllegalArgumentException("invalid legacy castle-war schedule");
        }
        try {
            Math.addExact(warTimeMillis,
                    Math.multiplyExact((long) warDurationMinutes, 60_000L));
            Math.multiplyExact((long) warIntervalMinutes, 60_000L);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("castle-war schedule exceeds millisecond range",
                    overflow);
        }
    }
}

record ClanEmblemData(long emblemId, byte[] pixels) {
    ClanEmblemData {
        if (emblemId <= 0 || emblemId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("clan emblem id is outside native dword range");
        }
        if (pixels == null || pixels.length != Protocol.EMBLEM_BYTES) {
            throw new IllegalArgumentException("clan emblem must contain exactly "
                    + Protocol.EMBLEM_BYTES + " bytes");
        }
        pixels = pixels.clone();
    }

    @Override public byte[] pixels() {
        return pixels.clone();
    }
}

final class MemoryAccountRepository implements AccountRepository {
    private static final long DEFAULT_CASTLE_WAR_START_MILLIS =
            1_787_971_500_000L;
    private final String baseAccount;
    private final String authenticationFailureAccount;
    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();
    private final Set<String> litoOwnedAccounts = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, AccountInfoSnapshot> accountInfo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, QuizRegistrationSnapshot> registeredQuiz = new ConcurrentHashMap<>();
    private final int sourceInventoryWeight;
    private final double sourceWeightRate;
    private final int sourceFood;
    private final int sourcePrimaryLevel;
    private final int sourcePrimaryMaxHp;
    private final int sourcePrimaryMaxMp;
    private final int sourcePrimaryArmorClass;
    private final int sourcePrimaryStrength;
    private final int sourcePrimaryConstitution;
    private final int sourcePrimaryDexterity;
    private final int sourcePrimaryCharisma;
    private final int sourcePrimaryIntelligence;
    private final int sourcePrimaryWisdom;
    private final int sourceSecondaryMaxMp;
    private final boolean sourceInventoryFixtureEnabled;
    private final boolean sourceProjectileFixtureEnabled;
    private final long sourceCastleWarStartMillis;
    private final int sourceCastleWarDurationMinutes;
    private final int sourceCastleWarIntervalMinutes;

    MemoryAccountRepository(String account, String password) {
        this(account, password, "", 0, 1.0);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount) {
        this(account, password, authenticationFailureAccount, 0, 1.0);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, 40, 52);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood, 52);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, 110, false, false);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourceSecondaryMaxMp) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, sourceSecondaryMaxMp, false, false);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, false);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourcePrimaryMaxHp, int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, sourcePrimaryMaxHp, sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled,
                -1, 60, 10_080);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourcePrimaryMaxHp, int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled,
                            int castleWarFixtureStartDelaySeconds,
                            int castleWarFixtureDurationMinutes,
                            int castleWarFixtureIntervalMinutes) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, sourcePrimaryMaxHp,
                100, 10, 13, 10, 10, 9, 8, 11,
                sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled,
                castleWarFixtureStartDelaySeconds,
                castleWarFixtureDurationMinutes, castleWarFixtureIntervalMinutes);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourcePrimaryMaxHp, int sourcePrimaryMaxMp,
                            int sourcePrimaryArmorClass,
                            int sourcePrimaryStrength, int sourcePrimaryConstitution,
                            int sourcePrimaryDexterity, int sourcePrimaryCharisma,
                            int sourcePrimaryIntelligence, int sourcePrimaryWisdom,
                            int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled,
                            int castleWarFixtureStartDelaySeconds,
                            int castleWarFixtureDurationMinutes,
                            int castleWarFixtureIntervalMinutes) {
        this.baseAccount = account;
        this.authenticationFailureAccount = key(authenticationFailureAccount);
        if (sourceInventoryWeight < 0 || !Double.isFinite(sourceWeightRate)
                || sourceWeightRate <= 0.0 || sourceFood < 0 || sourceFood > 255
                || sourcePrimaryLevel < 1
                || sourcePrimaryLevel > NumericBreakthrough.MAX_LEVEL
                || sourcePrimaryMaxHp < 1
                || sourcePrimaryMaxHp > NumericBreakthrough.MAX_VITAL
                || sourcePrimaryMaxMp < 1
                || sourcePrimaryMaxMp > NumericBreakthrough.MAX_VITAL
                || sourcePrimaryArmorClass < NumericBreakthrough.MIN_ARMOR_CLASS
                || sourcePrimaryArmorClass > NumericBreakthrough.MAX_ARMOR_CLASS
                || sourcePrimaryStrength < 0
                || sourcePrimaryStrength > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryConstitution < 0
                || sourcePrimaryConstitution > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryDexterity < 0
                || sourcePrimaryDexterity > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryCharisma < 0
                || sourcePrimaryCharisma > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryIntelligence < 0
                || sourcePrimaryIntelligence > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryWisdom < 0
                || sourcePrimaryWisdom > NumericBreakthrough.MAX_ABILITY
                || sourceSecondaryMaxMp < 1
                || sourceSecondaryMaxMp > NumericBreakthrough.MAX_VITAL
                || castleWarFixtureStartDelaySeconds < -1
                || castleWarFixtureStartDelaySeconds > 3_600
                || castleWarFixtureDurationMinutes < 1
                || castleWarFixtureIntervalMinutes <= castleWarFixtureDurationMinutes) {
            throw new IllegalArgumentException("invalid memory inventory-weight fixture");
        }
        this.sourceInventoryWeight = sourceInventoryWeight;
        this.sourceWeightRate = sourceWeightRate;
        this.sourceFood = sourceFood;
        this.sourcePrimaryLevel = sourcePrimaryLevel;
        this.sourcePrimaryMaxHp = sourcePrimaryMaxHp;
        this.sourcePrimaryMaxMp = sourcePrimaryMaxMp;
        this.sourcePrimaryArmorClass = sourcePrimaryArmorClass;
        this.sourcePrimaryStrength = sourcePrimaryStrength;
        this.sourcePrimaryConstitution = sourcePrimaryConstitution;
        this.sourcePrimaryDexterity = sourcePrimaryDexterity;
        this.sourcePrimaryCharisma = sourcePrimaryCharisma;
        this.sourcePrimaryIntelligence = sourcePrimaryIntelligence;
        this.sourcePrimaryWisdom = sourcePrimaryWisdom;
        this.sourceSecondaryMaxMp = sourceSecondaryMaxMp;
        this.sourceInventoryFixtureEnabled = sourceInventoryFixtureEnabled;
        this.sourceProjectileFixtureEnabled = sourceProjectileFixtureEnabled;
        this.sourceCastleWarStartMillis = castleWarFixtureStartDelaySeconds < 0
                ? DEFAULT_CASTLE_WAR_START_MILLIS
                : Math.addExact(System.currentTimeMillis(),
                        Math.multiplyExact(
                                (long) castleWarFixtureStartDelaySeconds, 1_000L));
        this.sourceCastleWarDurationMinutes = castleWarFixtureDurationMinutes;
        this.sourceCastleWarIntervalMinutes = castleWarFixtureIntervalMinutes;
        credentials.put(key(account), password);
        credentials.put(key(account + "2"), password);
        credentials.put(key(account + "3"), password);
        credentials.put(key(account + "4"), password);
        credentials.put(key(account + "5"), password);
        credentials.put(key(account + "6"), password);
        credentials.put(key(account + "7"), password);
        credentials.put(key(account + "8"), password);
        credentials.put(key(account + "9"), password);
        credentials.put(key(account + "10"), password);
        credentials.put(key(account + "11"), password);
        credentials.put(key(account + "12"), password);
        credentials.put(key(account + "13"), password);
        credentials.put(key(account + "14"), password);
        credentials.put(key(account + "15"), password);
        credentials.put(key(account + "16"), password);
        credentials.put(key(account + "17"), password);
        credentials.put(key(account + "18"), password);
        credentials.put(key(account + "19"), password);
        credentials.put(key(account + "20"), password);
        credentials.put(key(account + "21"), password);
        credentials.put(key(account + "22"), password);
        credentials.put(key(account + "23"), password);
        credentials.put(key(account + "24"), password);
        credentials.put(key(account + "25"), password);
        credentials.put(key(account + "26"), password);
    }

    @Override public void healthCheck() {}

    @Override public GmCommandDefinition gmCommand(String name) {
        return "de".equalsIgnoreCase(name == null ? "" : name.trim())
                ? new GmCommandDefinition("de", 200, "L1SpawnDe",
                    "loopback equivalent of lito_static.commands", false)
                : null;
    }

    @Override public List<RepositoryTournamentShopOffer> tournamentShopOffers() {
        return TournamentShopCatalog.defaultSourceRows();
    }

    @Override public AuthResult authenticate(String candidate, String suppliedPassword)
            throws SQLException {
        if (!authenticationFailureAccount.isEmpty()
                && authenticationFailureAccount.equals(key(candidate))) {
            throw new SQLException("configured memory authentication failure fixture");
        }
        String expected = credentials.get(key(candidate));
        boolean ok = expected != null && expected.equals(suppliedPassword);
        return ok ? new AuthResult(true, false, candidate) : AuthResult.rejected(candidate);
    }

    @Override public boolean accountExists(String account) {
        return credentials.containsKey(key(account));
    }

    @Override public boolean deleteLitoAccount(String account) {
        String normalized = key(account);
        return litoOwnedAccounts.remove(normalized)
                && credentials.remove(normalized) != null;
    }

    @Override public AccountMutationResult createAccount(String account, String password) {
        String normalized = key(account);
        if (credentials.putIfAbsent(normalized, password) != null) {
            return AccountMutationResult.EXISTS;
        }
        litoOwnedAccounts.add(normalized);
        return AccountMutationResult.SUCCESS;
    }

    @Override public AccountMutationResult changePassword(
            String account, String oldPassword, String newPassword) {
        String normalized = key(account);
        String current = credentials.get(normalized);
        if (current == null) return AccountMutationResult.NOT_FOUND;
        if (!current.equals(oldPassword)) return AccountMutationResult.WRONG_PASSWORD;
        return credentials.replace(normalized, oldPassword, newPassword)
                ? AccountMutationResult.SUCCESS : AccountMutationResult.WRONG_PASSWORD;
    }

    @Override public synchronized AccountMutationResult registerQuiz(
            String account, String oldPassword, String newPassword, List<String> quizFields) {
        String normalized = key(account);
        String current = credentials.get(normalized);
        if (current == null) return AccountMutationResult.NOT_FOUND;
        if (!current.equals(oldPassword)) return AccountMutationResult.WRONG_PASSWORD;
        credentials.put(normalized, newPassword);
        registeredQuiz.put(normalized, new QuizRegistrationSnapshot(quizFields));
        return AccountMutationResult.SUCCESS;
    }

    QuizRegistrationSnapshot registeredQuizForTest(String account) {
        return registeredQuiz.get(key(account));
    }

    @Override public AccountMutationResult changeAccountInfo(
            String account, int type, List<String> fields) {
        String normalized = key(account);
        if (!credentials.containsKey(normalized)) return AccountMutationResult.NOT_FOUND;
        accountInfo.put(normalized, new AccountInfoSnapshot(type, fields));
        return AccountMutationResult.SUCCESS;
    }

    AccountInfoSnapshot accountInfoForTest(String account) {
        return accountInfo.get(key(account));
    }

    @Override public List<CharacterSummary> characters(String requestedAccount) {
        boolean basePlayer = baseAccount.equalsIgnoreCase(requestedAccount);
        boolean secondPlayer = (baseAccount + "2").equalsIgnoreCase(requestedAccount);
        boolean observer = (baseAccount + "3").equalsIgnoreCase(requestedAccount);
        boolean emblemPlayer = (baseAccount + "4").equalsIgnoreCase(requestedAccount);
        boolean elfPlayer = (baseAccount + "5").equalsIgnoreCase(requestedAccount);
        boolean clanMate = (baseAccount + "6").equalsIgnoreCase(requestedAccount);
        boolean kentOwner = (baseAccount + "7").equalsIgnoreCase(requestedAccount);
        boolean femalePlayer = (baseAccount + "8").equalsIgnoreCase(requestedAccount);
        boolean giranOwner = (baseAccount + "9").equalsIgnoreCase(requestedAccount);
        boolean giranMate = (baseAccount + "10").equalsIgnoreCase(requestedAccount);
        boolean physicalPlayer = (baseAccount + "11").equalsIgnoreCase(requestedAccount);
        boolean defensePlayer = (baseAccount + "12").equalsIgnoreCase(requestedAccount);
        boolean clawPlayer = (baseAccount + "13").equalsIgnoreCase(requestedAccount);
        boolean edoryuPlayer = (baseAccount + "14").equalsIgnoreCase(requestedAccount);
        boolean weaponDummy = (baseAccount + "15").equalsIgnoreCase(requestedAccount);
        boolean kiringkuPlayer = (baseAccount + "16").equalsIgnoreCase(requestedAccount);
        boolean chainswordPlayer = (baseAccount + "17").equalsIgnoreCase(requestedAccount);
        boolean bowPlayer = (baseAccount + "18").equalsIgnoreCase(requestedAccount);
        boolean counterBarrierPlayer =
                (baseAccount + "19").equalsIgnoreCase(requestedAccount);
        boolean windawoodOwner = (baseAccount + "20").equalsIgnoreCase(requestedAccount);
        boolean windawoodMate = (baseAccount + "21").equalsIgnoreCase(requestedAccount);
        boolean dwarfOwner = (baseAccount + "22").equalsIgnoreCase(requestedAccount);
        boolean dwarfMate = (baseAccount + "23").equalsIgnoreCase(requestedAccount);
        boolean adenOwner = (baseAccount + "24").equalsIgnoreCase(requestedAccount);
        boolean adenMate = (baseAccount + "25").equalsIgnoreCase(requestedAccount);
        boolean siegeOwner = (baseAccount + "26").equalsIgnoreCase(requestedAccount);
        if (!basePlayer && !secondPlayer && !observer && !emblemPlayer
                && !elfPlayer && !clanMate && !kentOwner && !femalePlayer
                && !giranOwner && !giranMate && !physicalPlayer && !defensePlayer
                && !clawPlayer && !edoryuPlayer && !weaponDummy
                && !kiringkuPlayer && !chainswordPlayer && !bowPlayer
                && !counterBarrierPlayer && !windawoodOwner
                && !windawoodMate && !dwarfOwner && !dwarfMate
                && !adenOwner && !adenMate && !siegeOwner) return List.of();
        return List.of(new CharacterSummary(
                siegeOwner ? "TestSiegeLord"
                        : adenMate ? "TestAdenMate"
                        : adenOwner ? "TestAdenLord"
                        : dwarfMate ? "TestDwarfMate"
                        : dwarfOwner ? "TestDwarfLord"
                        : windawoodMate ? "TestWindawoodMate"
                        : windawoodOwner ? "TestWindawoodLord"
                        : counterBarrierPlayer ? "TestCounterBarrier"
                        : bowPlayer ? "TestBow" : chainswordPlayer ? "TestChainsword"
                        : kiringkuPlayer ? "TestKiringku" : weaponDummy ? "TestWeaponDummy"
                        : edoryuPlayer ? "TestEdoryu"
                        : clawPlayer ? "TestClaw" : defensePlayer ? "TestDefense"
                        : physicalPlayer ? "TestPhysical" : giranMate ? "TestGiranMate"
                        : giranOwner ? "TestGiranLord" : femalePlayer ? "TestPrincess"
                        : kentOwner ? "TestKentLord"
                        : clanMate ? "TestClanMate" : elfPlayer ? "TestElf" : emblemPlayer ? "TestEmblem"
                        : observer ? "TestObserver" : secondPlayer ? "TestMage" : "TestKnight",
                siegeOwner ? "LegacySiege"
                        : adenOwner || adenMate ? "LegacyAden"
                        : dwarfOwner || dwarfMate ? "LegacyDwarf"
                        : windawoodOwner || windawoodMate ? "LegacyWindawood"
                        : giranOwner || giranMate ? "LegacyGiran" : kentOwner ? "LegacyKent"
                        : emblemPlayer || clanMate ? "LegacyEmblem" : "",
                kiringkuPlayer ? 6 : chainswordPlayer ? 5 : bowPlayer ? 2
                        : counterBarrierPlayer ? 1
                        : clawPlayer || edoryuPlayer ? 4 : elfPlayer ? 2
                        : secondPlayer || observer || clanMate || giranMate || windawoodMate
                        || dwarfMate || adenMate
                        || physicalPlayer || defensePlayer || weaponDummy ? 1 : 0,
                femalePlayer ? 1 : 0,
                observer ? 15 : secondPlayer ? 47 : elfPlayer ? 45
                        : clanMate || giranMate || windawoodMate || dwarfMate ? 40
                        : kentOwner || windawoodOwner || dwarfOwner || siegeOwner ? 55
                        : sourcePrimaryLevel,
                weaponDummy || counterBarrierPlayer ? 2000
                        : secondPlayer ? 100 : sourcePrimaryMaxHp,
                observer ? 10 : secondPlayer ? sourceSecondaryMaxMp
                        : basePlayer ? sourcePrimaryMaxMp : 100,
                defensePlayer ? -10 : elfPlayer ? 100
                        : basePlayer ? sourcePrimaryArmorClass : 10,
                basePlayer ? sourcePrimaryStrength : 13,
                basePlayer ? sourcePrimaryConstitution : 10,
                basePlayer ? sourcePrimaryDexterity : 10,
                basePlayer ? sourcePrimaryCharisma : siegeOwner ? 18
                        : adenOwner || adenMate ? 18
                        : dwarfOwner || dwarfMate ? 18
                        : windawoodOwner || windawoodMate ? 18
                        : giranOwner || giranMate ? 18 : 9,
                basePlayer ? sourcePrimaryIntelligence : kiringkuPlayer ? 13 : 8,
                basePlayer ? sourcePrimaryWisdom : 11,
                kiringkuPlayer ? 13 : 8, 11,
                siegeOwner ? 27
                        : adenMate ? 26 : adenOwner ? 25
                        : dwarfMate ? 24 : dwarfOwner ? 23
                        : windawoodMate ? 22 : windawoodOwner ? 21
                        : counterBarrierPlayer ? 20
                        : bowPlayer ? 19 : chainswordPlayer ? 18 : kiringkuPlayer ? 17
                        : weaponDummy ? 16 : edoryuPlayer ? 15 : clawPlayer ? 14
                        : defensePlayer ? 13 : physicalPlayer ? 12 : giranMate ? 11
                        : giranOwner ? 10 : femalePlayer ? 9 : kentOwner ? 8
                        : clanMate ? 7 : elfPlayer ? 6 : emblemPlayer ? 5
                        : observer ? 4 : secondPlayer ? 3 : 1,
                0, basePlayer ? 1234 : secondPlayer ? -1234 : 0,
                basePlayer ? 7 : 0, 0, 32600, 32700, 4,
                10, sourceFood, BattleNaturalHpRegen.fallbackConHprMaximum(10),
                13, 10, elfPlayer ? BattleSpellCatalog.ATTRIBUTE_EARTH : 0));
    }

    @Override public Set<Integer> learnedSkills(long characterObjectId) {
        if (characterObjectId == 1) {
            // Skill 131 is a loopback-only bridge so the source Knight fixture
            // can prove Light in ordinary WORLD without modifying either
            // retained character_skills database.
            return Set.of(1, 2, 4, 6, 7, 11, 15, 16, 17, 18, 25, 34, 35,
                    44, 58, 63, 77, 78, BattleSpellCatalog.WORLD_TREE_CALL);
        }
        if (characterObjectId == 3) {
            // Wind Cutter is a loopback-only cross-player fixture used to
            // prove that awakening elemental/MR modifiers enter the same
            // retained ordinary-magic combat calculation.  No source DB row
            // is written or inferred from this memory-only account. HEAL_ALL
            // is likewise a loopback-only bridge so the already learned
            // Absolute Barrier and skill49 can prove their commit/cleanup
            // ordering without mutating either retained character-skills DB.
            return Set.of(1, 3, 4, BattleSpellCatalog.WIND_CUTTER,
                    BattleSpellCatalog.HEAL_ALL,
                    BattleSpellCatalog.HOLY_WEAPON,
                    BattleSpellCatalog.CURE_POISON,
                    BattleSpellCatalog.CHILL_TOUCH,
                    BattleSpellCatalog.VAMPIRIC_TOUCH,
                    BattleSpellCatalog.SLOW,
                    BattleSpellCatalog.EARTH_JAIL,
                    BattleSpellCatalog.FROZEN_CLOUD,
                    BattleSpellCatalog.REVEAL_WEAKNESS,
                    BattleSpellCatalog.DETECTION,
                    14, 19, 20, 26, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
                    42, 43, 44, 45, 46, 47, 48, 51, 52, 53, 54, 56, 57, 59, 60, 61, 62, 64,
                    65, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
                    131, BattleSpellCatalog.EARTH_BIND, 206);
        }
        // Loopback-only royal source-clan fixture for native skills 113-118.
        // TestEmblem is character type 0; keep all royal spells off TestElf.
        if (characterObjectId == 5) return Set.of(
                BattleSpellCatalog.TRUE_TARGET, BattleSpellCatalog.GLOWING_AURA,
                BattleSpellCatalog.SHINING_AURA, BattleSpellCatalog.CALL_CLAN,
                BattleSpellCatalog.BRAVE_AURA, BattleSpellCatalog.RUN_CLAN,
                131);
        // Loopback-only source-clan fixture used to prove HEAL_ALL's exact
        // clan/pet target_to=20 behavior and the native WORLD pledge warehouse
        // path without changing the legacy DB. Skill 131 is the already-proven
        // selector-only bridge from Tournament into the imported classic world.
        if (characterObjectId == 7) return Set.of(49, 55, 69, 131);
        // Loopback-only melee carrier for Brave Aura's ordinary-WORLD damage
        // acceptance. Skill131 is the already-qualified bridge into the
        // imported classic world; no retained character_skills row changes.
        if (characterObjectId == 12) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL);
        // Loopback-only Kent owner carrier for the selected map15/240-243
        // component. Skill 131 is the already-qualified WORLD bridge.
        if (characterObjectId == 8) return Set.of(131);
        // Loopback-only female crown carrier for the native Zero -> Gunter
        // story acceptance. Skill 131 is only the already-qualified WORLD
        // bridge; production characters still come from the read-only source.
        if (characterObjectId == 9) return Set.of(131);
        // Loopback-only Giran owner/member carriers use the already-qualified
        // World Tree bridge to reach the retained map52 steward component.
        if (characterObjectId == 10) return Set.of(
                BattleSpellCatalog.BLESSED_ARMOR,
                BattleSpellCatalog.WORLD_TREE_CALL);
        if (characterObjectId == 11) return Set.of(131);
        // Loopback-only Windawood owner/member carriers for map23/24/29.
        if (characterObjectId == 21 || characterObjectId == 22) return Set.of(131);
        // Loopback-only Dwarf Castle owner/member carriers for map66.
        if (characterObjectId == 23 || characterObjectId == 24) return Set.of(131);
        // Loopback-only Aden Castle owner/member carriers for map300.
        if (characterObjectId == 25 || characterObjectId == 26) return Set.of(131);
        // Loopback-only unlanded royal/clan leader for the retained castle-war
        // declaration -> tower -> crown -> ownership integration path.
        if (characterObjectId == 27) return Set.of(131);
        if (characterObjectId == 6) return Set.of(
                129, 130, 131, BattleSpellCatalog.ELEMENTAL_FALL_DOWN,
                BattleSpellCatalog.COUNTER_MIRROR,
                BattleSpellCatalog.CLEAR_MIND,
                BattleSpellCatalog.RESIST_ELEMENTAL,
                BattleSpellCatalog.BLOODY_SOUL,
                BattleSpellCatalog.LESSER_ELEMENTAL,
                BattleSpellCatalog.GREATER_ELEMENTAL,
                 BattleSpellCatalog.BURNING_WEAPON,
                 BattleSpellCatalog.NATURES_BLESSING,
                 BattleSpellCatalog.WATER_LIFE,
                 157, 169, 176);
        // Loopback-only bow Elf fixture. Skill131 is the already-qualified
        // WORLD bridge; skill132 is the selected/retained Triple Arrow row
        // paired with this fixture's qualified bow and source-arrow ledger.
        // No retained character_skills row or client resource is modified.
        if (characterObjectId == 19) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL,
                BattleSpellCatalog.TRIPLE_ARROW);
        // Loopback-only Dragon Knight/chainsword carrier for qualified native
        // Dragon Knight skills181..194.
        // Skill131 is the already-qualified bridge into ordinary WORLD; the
        // selected source databases and client remain read-only.
        if (characterObjectId == 18) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL,
                BattleSpellCatalog.DRAGON_SKIN,
                BattleSpellCatalog.BURNING_SLASH,
                BattleSpellCatalog.GUARD_BRAKE,
                BattleSpellCatalog.MAGMA_BREATH,
                BattleSpellCatalog.AWAKEN_ANTHARAS,
                BattleSpellCatalog.BLOODLUST,
                BattleSpellCatalog.FOE_SLAYER,
                BattleSpellCatalog.RESIST_FEAR,
                 BattleSpellCatalog.SHOCK_SKIN,
                 BattleSpellCatalog.AWAKEN_FAFURION,
                 BattleSpellCatalog.MORTAL_BODY,
                 BattleSpellCatalog.THUNDER_GRAB,
                 BattleSpellCatalog.HORROR_OF_DEATH,
                 BattleSpellCatalog.FREEZING_BREATH,
                 BattleSpellCatalog.AWAKEN_VALAKAS);
        // Loopback-only Illusionist/kiringku carrier for the retained
        // selector-only skills202/207/208/213 in BATTLE and ordinary WORLD.
        // The selected source databases and client remain read-only.
        if (characterObjectId == 17) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL,
                BattleSpellCatalog.CONFUSION,
                BattleSpellCatalog.MIND_BREAK,
                BattleSpellCatalog.BONE_BREAK,
                BattleSpellCatalog.ARM_BREAKER);
        // Loopback-only Edoryu fixture proving LiTo's native weapon/action
        // gates in BATTLE and ordinary WORLD. Skills 8 and 12 exercise the
        // retained character-vs-item weapon-enchant replacement group;
        // skill131 is only the already-qualified Tournament -> retained-world
        // bridge. Neither selected source database is modified.
        if (characterObjectId == 15) return Set.of(
                BattleSpellCatalog.HOLY_WEAPON,
                BattleSpellCatalog.ENCHANT_WEAPON,
                BattleSpellCatalog.SHOCK_STUN,
                BattleSpellCatalog.WORLD_TREE_CALL);
        // Loopback-only target fixture for authoritative skill-72 TCP
        // evidence. The selected legacy DB remains read-only and unchanged.
        if (characterObjectId == 16) return Set.of(27, 31, 44, 60, 131,
                BattleSpellCatalog.REDUCTION_ARMOR,
                BattleSpellCatalog.BOUNCE_ATTACK,
                BattleSpellCatalog.SOLID_CARRIAGE);
        // Loopback-only two-hand-sword fixture for the selected-client
        // Counter Barrier gate and retained reflection pipeline.
        if (characterObjectId == 20) {
            return Set.of(44, BattleSpellCatalog.COUNTER_BARRIER,
                    BattleSpellCatalog.WORLD_TREE_CALL);
        }
        return Set.of();
    }

    @Override public Map<Integer, Integer> questSteps(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        // Loopback-only knight carrier. This is the exact prerequisite needed
        // to enter the selected 10702 Gunter chain; no retained database row is
        // created or changed by this fixture.
        return characterObjectId == 3 ? Map.of(10701, 1) : Map.of();
    }

    @Override public List<SourceInventoryItem> sourceInventory(long characterObjectId) {
        if (sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25)) {
            // Exact selected 380_db armor row 20089 (type=armor/type2=2).
            // This isolated carrier makes the retained NPC skill21 item-local
            // AC lease observable without writing either original database.
            ClassicWorldDropCatalog.ItemWire armor =
                    new ClassicWorldDropCatalog.ItemWire(
                            20089, "armor", "$116", "armor",
                            51, 3830, 0, false, true, 70_000, 0,
                            "", 0, 0, "leather", 4, 0x7F);
            InventoryCatalog.NativeArmorStatus status =
                    new InventoryCatalog.NativeArmorStatus(
                            2, ClassicWorldDropCatalog.materialCode("leather"),
                            70_000, 0x7F, 4,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2);
            return List.of(new SourceInventoryItem(
                    0x0F00_2008L, 20089, 1, true, 0, 0,
                    true, RuntimeInventory.BLESSING_BLESSED,
                    false, armor, status));
        }
        if (sourceInventoryFixtureEnabled && sourceProjectileFixtureEnabled
                && characterObjectId == 1) {
            // Combined loopback-only fixture for format-30 warehouse/player
            // transfer acceptance. Existing one-flag source fixtures remain
            // byte-for-byte unchanged.
            ClassicWorldDropCatalog.ItemWire token =
                    new ClassicWorldDropCatalog.ItemWire(
                            65_001, "etc", "Source Trade Token", "none",
                            321, 322, 1, true, true, 1_250, 87,
                            "", 0, 0, "plant", -1, 0);
            return List.of(new SourceInventoryItem(
                    2001L, 65_001, 3, false, 0, 0,
                    true, RuntimeInventory.BLESSING_NORMAL, false, token));
        }
        if (sourceInventoryFixtureEnabled && !sourceProjectileFixtureEnabled
                && characterObjectId == 3) {
            ClassicWorldDropCatalog.ItemWire claw =
                    new ClassicWorldDropCatalog.ItemWire(
                            40590, "etc", "$2023", "none", 139, 3963, 0,
                            false, false, 1000, 0, "", 0, 0, "bone", -1, 0);
            return List.of(new SourceInventoryItem(
                    0x0F00_4059L, 40590, 1, false, 0, 0,
                    true, RuntimeInventory.BLESSING_BLESSED, false, claw));
        }
        if (sourceProjectileFixtureEnabled && characterObjectId == 19) {
            ClassicWorldDropCatalog.ItemWire bow =
                    new ClassicWorldDropCatalog.ItemWire(
                            172, "weapon", "$24", "weapon", 278, 12, 0,
                            false, true, 30_000, 0, "bow", 2, 2,
                            "wood", 6, 0x4F);
            ClassicWorldDropCatalog.ItemWire arrow =
                    new ClassicWorldDropCatalog.ItemWire(
                            40743, "etc", "$61", "arrow", 7, 66, 0,
                            true, true, 30, 0, "", 0, 0,
                            "wood", -1, 0);
            return List.of(
                    new SourceInventoryItem(
                            1900L, 172, 1, true, 0, 0,
                            true, RuntimeInventory.BLESSING_BLESSED, false, bow),
                    new SourceInventoryItem(
                            1901L, 40743, 100, false, 0, 0,
                            true, RuntimeInventory.BLESSING_BLESSED, false, arrow));
        }
        return List.of();
    }

    @Override public SourceEquippedFireResistance equippedFireResistance(
            long characterObjectId) {
        // Loopback-only evidence fixture: TestMage carries one exact 20304
        // Fire Spirit Ring (+30). Other memory characters have no source gear.
        return characterObjectId == 3
                ? new SourceEquippedFireResistance(30, 1)
                : SourceEquippedFireResistance.none();
    }

    @Override public SourceBaseMagicResistance baseMagicResistance(long characterObjectId) {
        CharacterSummary character = charactersForObjectId(characterObjectId);
        if (character == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        return SourceBaseMagicResistance.from(character);
    }

    @Override public SourceEquipmentMagicStats equipmentMagicStats(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        if (characterObjectId == 12) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    24, 1, 6, 8, 1, 0, 0, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(11, 1, 0, 0, 0))
                    .withAbility(1, 2, 0, 0);
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    4, 5, 0, 0,
                    6, 7, 0, 0,
                    8, 9, 0, 0,
                    0, 0, 0, 0,
                    4, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 3, 1);
        }
        if (sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25)) {
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            SourceWeaponPhysicalStats.none(),
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            -2, 0);
            return new SourceEquipmentMagicStats(physical, 1, 1);
        }
        if (characterObjectId == 13) {
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    SourceWeaponPhysicalStats.none(),
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 3, 5);
            return new SourceEquipmentMagicStats(physical, 15, 0, 1, 1);
        }
        if (characterObjectId == 14 || characterObjectId == 15) {
            boolean claw = characterObjectId == 14;
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    claw ? 152 : 69, claw ? 11 : 12,
                    8, claw ? 7 : 6, 1, 2, 0, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            11, 0, 0, 0, 0,
                            claw ? 0 : 10, claw ? 0.0D : 1.2D,
                            claw ? 33 : 0, false, false));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0);
            return characterObjectId == 15
                    ? new SourceEquipmentMagicStats(physical, 1, 0, 100)
                    : new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 17) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    270, 17, 16, 16, 1, 1, 2, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            13, 0, 0, 0, 0,
                            0, 0.0D, 0, false, false, 3018));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 27) {
            // Loopback-only castle-objective carrier.  This is the same exact
            // selected 380_db item 52 contract used by the counter-barrier
            // fixture; it keeps gate/tower verification on L1AttackPc damage
            // instead of reducing native 10,000 HP objectives.
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    52, 3, 17, 21, 1, 0, 0, 0, 0)
                    .withNameId("$3");
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            weapon,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 18) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    272, 18, 16, 12, 2, 2, 5, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            13, 0, 0, 0, 0,
                            0, 0.0D, 0, false, false, 3033));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 19) {
            // Loopback-only bow fixture proving the retained bow-only ER
            // second roll and Solid Carriage's +15 contribution.  It never
            // mutates either retained database or the selected client.
            SourceProjectilePhysicalStats arrow =
                    new SourceProjectilePhysicalStats(
                            1901L, 40743, -2, 5, 7, 66, true, 100);
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    172, 4, 6, 6, 15, 0, 0, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            8, 1, 0, 0, 0))
                    .withProjectile(arrow);
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            weapon,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 20) {
            // Exact selected 380_db item 52: two-hand sword, native weapon
            // type 3 / walk action 50, large damage 21 and name-id $3.
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    52, 3, 17, 21, 1, 0, 0, 0, 0)
                    .withNameId("$3");
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            weapon,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 16) {
            // Loopback-only exact selected-mode fixture: an equipped shield
            // (armor type 7 / native use type 25) admits Solid Carriage.
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    SourceWeaponPhysicalStats.none(), true, false);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        // Keep the loopback Fire Spirit Ring fixture in the richer source
        // snapshot. Other memory characters deliberately carry no source gear.
        return characterObjectId == 3
                ? new SourceEquipmentMagicStats(
                        0, 0, 0, 0, 0, 30, 0,
                        0, 0, 0, 0, 0,
                        1, 1, 0, 0, 0, 0)
                : SourceEquipmentMagicStats.none();
    }

    @Override public SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId, Map<Long, Boolean> equippedOverrides) {
        if (equippedOverrides == null) {
            throw new IllegalArgumentException(
                    "memory source equipment overrides are required");
        }
        if (equippedOverrides.isEmpty()) {
            return new SourceEquipmentEvaluation(
                    equipmentMagicStats(characterObjectId),
                    SourceEquipmentVitalStats.none());
        }
        if (sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25)
                && equippedOverrides.keySet().equals(Set.of(0x0F00_2008L))) {
            SourceEquipmentMagicStats magic = Boolean.TRUE.equals(
                    equippedOverrides.get(0x0F00_2008L))
                    ? equipmentMagicStats(characterObjectId)
                    : SourceEquipmentMagicStats.none();
            return new SourceEquipmentEvaluation(
                    magic, SourceEquipmentVitalStats.none());
        }
        if (!sourceProjectileFixtureEnabled || characterObjectId != 19
                || !equippedOverrides.keySet().equals(Set.of(1900L))) {
            throw new IllegalArgumentException(
                    "unknown memory source equipment override");
        }
        SourceEquipmentMagicStats magic = Boolean.TRUE.equals(
                equippedOverrides.get(1900L))
                ? equipmentMagicStats(characterObjectId)
                : new SourceEquipmentMagicStats(
                        SourceEquipmentPhysicalStats.none(), 0, 0);
        return new SourceEquipmentEvaluation(
                magic, SourceEquipmentVitalStats.none());
    }

    @Override public SourcePhysicalGrowthStats physicalGrowthStats() {
        return SourcePhysicalGrowthStats.classic381();
    }

    @Override public SourceNpcPhysicalStats npcPhysicalStats(int npcId) {
        return SourceNpcPhysicalStats.memoryFixture(npcId);
    }

    @Override public SourceSummonTemplate summonTemplate(int npcId) {
        return SourceSummonTemplate.memoryFixture(npcId);
    }

    @Override public List<SourcePet> pets(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        // One loopback-only retained-shape row keeps collar recall reachable
        // without creating or modifying a legacy character_pets row.
        if (characterObjectId == 3) {
            return List.of(new SourcePet(0x3810, 0x3811,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "牧羊犬", 5, 30, 5, 750, 0));
        }
        // A second loopback-only row pins the qualified native C_GIVE
        // evolution transition without changing any retained source database.
        if (characterObjectId == 19) {
            return List.of(new SourcePet(0x3820, 0x3821,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "進化驗證犬", 30, 120, 24, 60_000, 0));
        }
        return List.of();
    }

    @Override public SourcePetTemplate petTemplate(int npcId) {
        return SourcePetTemplate.memoryFixture(npcId);
    }

    @Override public SourceInventoryWeightStats inventoryWeightStats(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        int ordinaryFixtureRows = sourceInventoryFixtureEnabled
                && !sourceProjectileFixtureEnabled && characterObjectId == 3 ? 1 : 0;
        int transferFixtureRows = sourceInventoryFixtureEnabled
                && sourceProjectileFixtureEnabled && characterObjectId == 1 ? 1 : 0;
        int projectileFixtureRows = sourceProjectileFixtureEnabled
                && characterObjectId == 19 ? 2 : 0;
        int armorFixtureRows = sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25) ? 1 : 0;
        int fixtureRows = ordinaryFixtureRows + transferFixtureRows
                + projectileFixtureRows + armorFixtureRows;
        int baseRows = sourceInventoryWeight == 0 ? 0 : 1;
        int fixtureWeight = ordinaryFixtureRows == 0 ? 0
                : BattleInventoryWeight.itemWeight(1, 1000);
        if (transferFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(3, 1_250));
        }
        if (projectileFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(1, 30_000));
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(100, 30));
        }
        if (armorFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(1, 70_000));
        }
        return new SourceInventoryWeightStats(
                BattleInventoryWeight.addWeight(sourceInventoryWeight, fixtureWeight),
                baseRows + fixtureRows, armorFixtureRows,
                projectileFixtureRows == 0 ? 0 : 1,
                baseRows + ordinaryFixtureRows + transferFixtureRows
                        + (projectileFixtureRows == 0 ? 0 : 1), sourceWeightRate);
    }

    private CharacterSummary charactersForObjectId(long characterObjectId) {
        for (int suffix = 0; suffix <= 26; suffix++) {
            String account = suffix == 0 ? baseAccount : baseAccount + suffix;
            CharacterSummary matched = characters(account).stream()
                    .filter(character -> character.objectId() == characterObjectId)
                    .findFirst().orElse(null);
            if (matched != null) return matched;
        }
        return null;
    }

    @Override public ClanEmblemData clanEmblem(String clanName) {
        boolean sourceFixture = "LegacyEmblem".equalsIgnoreCase(clanName);
        boolean siegeFixture = "LegacySiege".equalsIgnoreCase(clanName);
        if (!sourceFixture && !siegeFixture) return null;
        byte[] pixels = new byte[Protocol.EMBLEM_BYTES];
        for (int pixel = 0; pixel < pixels.length / 2; pixel++) {
            int color = ((pixel / 16) & 1) == (pixel & 1) ? 0x7FFF : 0x0421;
            pixels[pixel * 2] = (byte) color;
            pixels[pixel * 2 + 1] = (byte) (color >>> 8);
        }
        return new ClanEmblemData(
                siegeFixture ? 0x1020_3041L : 0x1020_3040L, pixels);
    }

    @Override public CastleSummary castleForClan(String clanName) {
        if ("LegacyEmblem".equalsIgnoreCase(clanName)) {
            return new CastleSummary(5, "LegacyEmblem", "TestEmblem", "海音城",
                    1_787_971_500_000L, 10, 10_000, 5);
        }
        if ("LegacyKent".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the exact selected castle-1 row.
            return new CastleSummary(1, "LegacyKent", "TestKentLord", "肯特城",
                    1_221_408_000_000L, 0, 0, 8);
        }
        if ("LegacyGiran".equalsIgnoreCase(clanName)) {
            // Loopback-only fixture for the one preserved castle_soldier row.
            return new CastleSummary(4, "LegacyGiran", "TestGiranLord", "奇岩城",
                    1_787_971_500_000L, 10, 60_000, 10);
        }
        if ("LegacyWindawood".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the retained castle-3 Othmond flow.
            return new CastleSummary(3, "LegacyWindawood", "TestWindawoodLord",
                    "風木城", 1_787_971_500_000L, 10, 446, 21);
        }
        if ("LegacyDwarf".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the exact selected castle-6 row.
            return new CastleSummary(6, "LegacyDwarf", "TestDwarfLord",
                    "侏儒城", 1_221_408_000_000L, 0, 0, 23);
        }
        if ("LegacyAden".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the exact selected castle-7 row.
            return new CastleSummary(7, "LegacyAden", "TestAdenLord",
                    "亞丁城", 1_221_408_000_000L, 0, 49, 25);
        }
        return null;
    }

    @Override public CastleSummary castleById(int castleId) {
        for (String clan : List.of("LegacyEmblem", "LegacyKent", "LegacyGiran",
                "LegacyWindawood", "LegacyOrc", "LegacyDwarf", "LegacyAden")) {
            CastleSummary castle = castleForClan(clan);
            if (castle != null && castle.castleId() == castleId) return castle;
        }
        return null;
    }

    @Override public List<CastleWarSchedule> castleWarSchedules() {
        long start = sourceCastleWarStartMillis;
        int duration = sourceCastleWarDurationMinutes;
        int interval = sourceCastleWarIntervalMinutes;
        return List.of(
                new CastleWarSchedule(1, "肯特城", start, duration, interval),
                new CastleWarSchedule(2, "妖魔城", start, duration, interval),
                new CastleWarSchedule(3, "風木城", start, duration, interval),
                new CastleWarSchedule(4, "奇岩城", start, duration, interval),
                new CastleWarSchedule(5, "海音城", start, duration, interval),
                new CastleWarSchedule(6, "侏儒城", start, duration, interval),
                new CastleWarSchedule(7, "亞丁城", start, duration, interval),
                new CastleWarSchedule(8, "狄亞得要塞", start, duration, interval));
    }

    @Override public boolean characterExists(String characterName) {
        return characterName != null && (characterName.equalsIgnoreCase("TestKnight")
                || characterName.equalsIgnoreCase("TestMage")
                || characterName.equalsIgnoreCase("TestObserver")
                || characterName.equalsIgnoreCase("TestEmblem")
                || characterName.equalsIgnoreCase("TestElf")
                || characterName.equalsIgnoreCase("TestClanMate")
                || characterName.equalsIgnoreCase("TestKentLord")
                || characterName.equalsIgnoreCase("TestPrincess")
                || characterName.equalsIgnoreCase("TestGiranLord")
                || characterName.equalsIgnoreCase("TestGiranMate")
                || characterName.equalsIgnoreCase("TestPhysical")
                || characterName.equalsIgnoreCase("TestDefense")
                || characterName.equalsIgnoreCase("TestClaw")
                || characterName.equalsIgnoreCase("TestEdoryu")
                || characterName.equalsIgnoreCase("TestKiringku")
                || characterName.equalsIgnoreCase("TestChainsword")
                || characterName.equalsIgnoreCase("TestBow")
                || characterName.equalsIgnoreCase("TestCounterBarrier")
                || characterName.equalsIgnoreCase("TestWeaponDummy")
                || characterName.equalsIgnoreCase("TestWindawoodLord")
                || characterName.equalsIgnoreCase("TestWindawoodMate")
                || characterName.equalsIgnoreCase("TestDwarfLord")
                || characterName.equalsIgnoreCase("TestDwarfMate")
                || characterName.equalsIgnoreCase("TestAdenLord")
                || characterName.equalsIgnoreCase("TestAdenMate")
                || characterName.equalsIgnoreCase("TestSiegeLord"));
    }

    @Override public String description() { return "memory(loopback self-test only)"; }

    private static String key(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
    }
}

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
        return LegacyJdbcNetworkDefaults.apply(prefix + database + suffix);
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
