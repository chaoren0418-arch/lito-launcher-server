package local.lito381.server;

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
        if (SharedDatabase.enabled(config)) {
            return SharedDatabase.open(config);
        }
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
