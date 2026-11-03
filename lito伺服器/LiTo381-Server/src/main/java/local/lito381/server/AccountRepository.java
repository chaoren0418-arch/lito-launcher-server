// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import local.lito381.server.AccountMutationResult;
import local.lito381.server.AuthResult;
import local.lito381.server.CastleSummary;
import local.lito381.server.CastleWarSchedule;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClanEmblemData;
import local.lito381.server.GameStateStore;
import local.lito381.server.GmCommandDefinition;
import local.lito381.server.LegacyMySqlRepository;
import local.lito381.server.LitoCharacterPersistence;
import local.lito381.server.LitoOwnedInventoryItem;
import local.lito381.server.LitoPetPersistence;
import local.lito381.server.LitoSourceInventoryMutation;
import local.lito381.server.MemoryAccountRepository;
import local.lito381.server.Protocol;
import local.lito381.server.RepositoryBattleConfiguration;
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
import local.lito381.server.RepositoryCastleSnapshot;
import local.lito381.server.RepositoryCastleTreasuryMutation;
import local.lito381.server.RepositoryCharacterMutation;
import local.lito381.server.RepositoryCharacterTitle;
import local.lito381.server.RepositoryClanMutation;
import local.lito381.server.RepositoryClanSnapshot;
import local.lito381.server.RepositoryHotelChange;
import local.lito381.server.RepositoryHotelSnapshot;
import local.lito381.server.RepositoryHouseAuctionSnapshot;
import local.lito381.server.RepositoryHouseBidMutation;
import local.lito381.server.RepositoryHouseSettlement;
import local.lito381.server.RepositoryHouseTaxMutation;
import local.lito381.server.RepositoryHouseTaxPayment;
import local.lito381.server.RepositoryHouseTaxQuote;
import local.lito381.server.RepositoryHouseTaxSnapshot;
import local.lito381.server.RepositoryHouseTaxState;
import local.lito381.server.RepositoryLetter;
import local.lito381.server.RepositoryLetterDelivery;
import local.lito381.server.RepositoryLetterDeliveryStatus;
import local.lito381.server.RepositoryLetterMutation;
import local.lito381.server.RepositoryLetterSnapshot;
import local.lito381.server.RepositoryMarriageMutation;
import local.lito381.server.RepositoryMarriageState;
import local.lito381.server.RepositoryPendingReward;
import local.lito381.server.RepositoryPendingRewardClaim;
import local.lito381.server.RepositoryPendingRewardSnapshot;
import local.lito381.server.RepositoryPersistenceResult;
import local.lito381.server.RepositoryPrivateInventorySnapshot;
import local.lito381.server.RepositoryTitleMutation;
import local.lito381.server.RepositoryTournamentMutation;
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
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.SourceBaseMagicResistance;
import local.lito381.server.SourceEquipmentEvaluation;
import local.lito381.server.SourceEquipmentMagicStats;
import local.lito381.server.SourceEquipmentVitalStats;
import local.lito381.server.SourceEquippedFireResistance;
import local.lito381.server.SourceInventoryItem;
import local.lito381.server.SourceInventoryWeightStats;
import local.lito381.server.SourceKillDeathCounts;
import local.lito381.server.SourceNpcPhysicalStats;
import local.lito381.server.SourcePet;
import local.lito381.server.SourcePetTemplate;
import local.lito381.server.SourcePhysicalGrowthStats;
import local.lito381.server.SourceRoutedAccountRepository;
import local.lito381.server.SourceSummonTemplate;

interface AccountRepository
extends AutoCloseable {
    public void healthCheck() throws Exception;

    default public void migrateLitoExtensions() throws Exception {
    }

    public AuthResult authenticate(String var1, String var2) throws Exception;

    default public int accessLevel(String account) throws Exception {
        return 0;
    }

    default public GmCommandDefinition gmCommand(String name) throws Exception {
        return null;
    }

    default public List<RepositoryTournamentShopOffer> tournamentShopOffers() throws Exception {
        return List.of();
    }

    default public RepositoryBattleConfiguration battleConfiguration() throws Exception {
        return RepositoryBattleConfiguration.empty();
    }

    public boolean accountExists(String var1) throws Exception;

    default public boolean deleteLitoAccount(String account) throws Exception {
        return false;
    }

    public AccountMutationResult createAccount(String var1, String var2) throws Exception;

    public AccountMutationResult changePassword(String var1, String var2, String var3) throws Exception;

    public AccountMutationResult registerQuiz(String var1, String var2, String var3, List<String> var4) throws Exception;

    public AccountMutationResult changeAccountInfo(String var1, int var2, List<String> var3) throws Exception;

    public List<CharacterSummary> characters(String var1) throws Exception;

    default public int hellTime(long characterObjectId) throws Exception {
        return 0;
    }

    default public SourceKillDeathCounts killDeathCounts(long characterObjectId) throws Exception {
        return SourceKillDeathCounts.none();
    }

    public Set<Integer> learnedSkills(long var1) throws Exception;

    public Map<Integer, Integer> questSteps(long var1) throws Exception;

    public List<SourceInventoryItem> sourceInventory(long var1) throws Exception;

    default public RepositoryPrivateInventorySnapshot litoPrivateInventory(long characterObjectId) throws Exception {
        return RepositoryPrivateInventorySnapshot.unavailable();
    }

    default public RepositoryBookmarkSnapshot bookmarks(long characterObjectId) throws Exception {
        return RepositoryBookmarkSnapshot.unavailable();
    }

    default public RepositoryBookmarkPersistenceResult persistBookmarks(long characterObjectId, List<RepositoryBookmark> expected, List<RepositoryBookmark> replacement) throws Exception {
        return RepositoryBookmarkPersistenceResult.READ_ONLY;
    }

    default public RepositoryBuffSnapshot buffs(long characterObjectId) throws Exception {
        return RepositoryBuffSnapshot.unavailable();
    }

    default public RepositoryBuffPersistence persistBuffs(long characterObjectId, RepositoryBuffSnapshot expected, List<RepositoryBuff> replacement) throws Exception {
        return RepositoryBuffPersistence.readOnly(expected);
    }

    default public RepositoryBuddySnapshot buddies(long characterObjectId) throws Exception {
        return RepositoryBuddySnapshot.unavailable();
    }

    default public RepositoryBuddyPersistence persistBuddies(long characterObjectId, RepositoryBuddySnapshot expected, List<RepositoryBuddyGroup> groups, List<RepositoryBuddy> buddies) throws Exception {
        return RepositoryBuddyPersistence.readOnly(expected);
    }

    default public RepositoryLetterSnapshot letters(long characterObjectId) throws Exception {
        return RepositoryLetterSnapshot.unavailable();
    }

    default public RepositoryLetterDelivery deliverLetter(long senderCharacterObjectId, long paperObjectId, String receiverName, int code, String date, String subject, String content) throws Exception {
        return new RepositoryLetterDelivery(RepositoryLetterDeliveryStatus.READ_ONLY, null);
    }

    default public RepositoryLetterMutation openLetter(long characterObjectId, RepositoryLetter expected) throws Exception {
        return RepositoryLetterMutation.readOnly(expected);
    }

    default public RepositoryLetterMutation deleteLetter(long characterObjectId, RepositoryLetter expected) throws Exception {
        return RepositoryLetterMutation.readOnly(expected);
    }

    default public RepositoryBoardSnapshot boardTopicsBefore(long cursor, int limit) throws Exception {
        return RepositoryBoardSnapshot.unavailable();
    }

    default public RepositoryBoardLookup boardTopic(long topicId) throws Exception {
        return RepositoryBoardLookup.unavailable();
    }

    default public RepositoryBoardMutation postBoardTopic(long characterObjectId, String date, String title, String content, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryBoardMutation.unavailable();
    }

    default public RepositoryBoardMutation deleteBoardTopic(long characterObjectId, GameStateStore.BoardTopic expected) throws Exception {
        return RepositoryBoardMutation.unavailable();
    }

    default public RepositoryHouseAuctionSnapshot houseAuctions() throws Exception {
        return RepositoryHouseAuctionSnapshot.unavailable();
    }

    default public RepositoryHouseBidMutation persistHouseBid(long bidderCharacterObjectId, GameStateStore.HouseAuction expected, long amount, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryHouseBidMutation.unavailable();
    }

    default public RepositoryHouseSettlement persistHouseAuctionSettlement(GameStateStore.HouseAuction expected, long nowEpochSeconds, int taxIntervalDays) throws Exception {
        return RepositoryHouseSettlement.unavailable();
    }

    default public RepositoryHouseTaxSnapshot expiredHouseTaxes(long nowEpochSeconds) throws Exception {
        return RepositoryHouseTaxSnapshot.unavailable();
    }

    default public RepositoryHouseTaxMutation persistHouseTaxExpiry(RepositoryHouseTaxState expected, long nowEpochSeconds, int taxIntervalDays) throws Exception {
        return RepositoryHouseTaxMutation.unavailable();
    }

    default public RepositoryHouseTaxQuote houseTaxQuote(long characterObjectId, int keeperNpcId) throws Exception {
        return RepositoryHouseTaxQuote.unavailable();
    }

    default public RepositoryHouseTaxPayment persistHouseTaxPayment(long characterObjectId, RepositoryHouseTaxQuote expected, long nowEpochSeconds, int taxIntervalDays, long taxAdena, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryHouseTaxPayment.unavailable();
    }

    default public RepositoryHotelSnapshot hotelState() throws Exception {
        return RepositoryHotelSnapshot.unavailable();
    }

    default public RepositoryWaterDragonPassSnapshot waterDragonPassState() throws Exception {
        return RepositoryWaterDragonPassSnapshot.unavailable();
    }

    default public RepositoryTournamentRoster tournamentProfiles() throws Exception {
        return RepositoryTournamentRoster.unavailable();
    }

    default public RepositoryTournamentMutation persistTournamentProfile(long characterObjectId, String characterName, RepositoryTournamentProfile expected, GameStateStore.TournamentProfileState replacement) throws Exception {
        return RepositoryTournamentMutation.unavailable();
    }

    default public RepositoryClanSnapshot clan(long characterObjectId) throws Exception {
        return RepositoryClanSnapshot.unavailable();
    }

    default public RepositoryCharacterTitle characterTitle(long characterObjectId) throws Exception {
        return RepositoryCharacterTitle.unavailable();
    }

    default public RepositoryTitleMutation persistCharacterTitle(long characterObjectId, String expected, String replacement) throws Exception {
        return RepositoryTitleMutation.unavailable();
    }

    default public RepositoryMarriageState marriage(long characterObjectId) throws Exception {
        return RepositoryMarriageState.unavailable();
    }

    default public RepositoryMarriageMutation persistMarriage(long firstCharacterObjectId, long secondCharacterObjectId) throws Exception {
        return RepositoryMarriageMutation.unavailable();
    }

    default public RepositoryMarriageMutation persistDivorce(long characterObjectId, long expectedPartnerObjectId) throws Exception {
        return RepositoryMarriageMutation.unavailable();
    }

    default public RepositoryClanMutation createClan(long ownerCharacterObjectId, String clanName) throws Exception {
        return RepositoryClanMutation.unavailable();
    }

    default public RepositoryClanMutation joinClan(long ownerCharacterObjectId, String applicantName, int maximumMembers) throws Exception {
        return RepositoryClanMutation.unavailable();
    }

    default public RepositoryClanMutation changeClanMember(long ownerCharacterObjectId, String memberName, int expectedRole, int replacementRole) throws Exception {
        return RepositoryClanMutation.unavailable();
    }

    default public RepositoryClanMutation setClanDestroyPending(long ownerCharacterObjectId, boolean expected, boolean replacement) throws Exception {
        return RepositoryClanMutation.unavailable();
    }

    default public boolean rollbackCreatedClan(long ownerCharacterObjectId, long clanId) throws Exception {
        return false;
    }

    default public RepositoryWarehouseSnapshot warehouse(GameStateStore.WarehouseScope scope, String owner) throws Exception {
        return RepositoryWarehouseSnapshot.unavailable();
    }

    default public RepositoryWarehousePassword warehousePassword(String account) throws Exception {
        return RepositoryWarehousePassword.unavailable();
    }

    default public RepositoryWarehousePasswordChange changeWarehousePassword(String account, int expectedOld, int replacement) throws Exception {
        return RepositoryWarehousePasswordChange.unavailable();
    }

    default public RepositoryWarehouseTransferResult persistWarehouseTransfer(long characterObjectId, GameStateStore.WarehouseScope scope, String owner, RuntimeInventory beforeInventory, RuntimeInventory beforeWarehouse, RuntimeInventory afterInventory, RuntimeInventory afterWarehouse, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryWarehouseTransferResult.READ_ONLY;
    }

    public SourceEquippedFireResistance equippedFireResistance(long var1) throws Exception;

    public SourceBaseMagicResistance baseMagicResistance(long var1) throws Exception;

    public SourceEquipmentMagicStats equipmentMagicStats(long var1) throws Exception;

    default public Set<Integer> counterMagicBypassSkills() throws Exception {
        return Set.of(44, 87, 112, 132, 153, 161, 173, 174, 187, 188, 192);
    }

    default public Map<Integer, Integer> skillReuseDelays() throws Exception {
        return Map.of();
    }

    default public SourceEquipmentEvaluation equipmentEvaluation(long characterObjectId) throws Exception {
        return new SourceEquipmentEvaluation(this.equipmentMagicStats(characterObjectId), SourceEquipmentVitalStats.none());
    }

    default public SourceEquipmentEvaluation equipmentEvaluation(long characterObjectId, Map<Long, Boolean> equippedOverrides) throws Exception {
        if (equippedOverrides == null) {
            throw new IllegalArgumentException("source equipment overrides are required");
        }
        if (!equippedOverrides.isEmpty()) {
            throw new UnsupportedOperationException("repository cannot evaluate source equipment overrides");
        }
        return this.equipmentEvaluation(characterObjectId);
    }

    public SourcePhysicalGrowthStats physicalGrowthStats() throws Exception;

    public SourceNpcPhysicalStats npcPhysicalStats(int var1) throws Exception;

    public SourceSummonTemplate summonTemplate(int var1) throws Exception;

    public List<SourcePet> pets(long var1) throws Exception;

    public SourcePetTemplate petTemplate(int var1) throws Exception;

    public SourceInventoryWeightStats inventoryWeightStats(long var1) throws Exception;

    public ClanEmblemData clanEmblem(String var1) throws Exception;

    public CastleSummary castleForClan(String var1) throws Exception;

    public CastleSummary castleById(int var1) throws Exception;

    public List<CastleWarSchedule> castleWarSchedules() throws Exception;

    default public RepositoryCastleSnapshot castleState(int castleId) throws Exception {
        return RepositoryCastleSnapshot.unavailable();
    }

    default public RepositoryCastleMutation persistCastleState(long ownerCharacterObjectId, RepositoryCastleSnapshot expected, RepositoryCastleSnapshot replacement) throws Exception {
        return RepositoryCastleMutation.unavailable();
    }

    default public RepositoryCastleMutation persistCastleWarEnd(RepositoryCastleSnapshot expected, RepositoryCastleSnapshot replacement) throws Exception {
        return RepositoryCastleMutation.unavailable();
    }

    default public RepositoryCastleSecuritySettlement prepareCastleSecurity(int castleId, long warStartMillis, long chargeAdena) throws Exception {
        return RepositoryCastleSecuritySettlement.unavailable();
    }

    default public RepositoryCastleSecuritySettlement completeCastleSecurity(int castleId, long warStartMillis) throws Exception {
        return RepositoryCastleSecuritySettlement.unavailable();
    }

    default public RepositoryPendingRewardSnapshot pendingRewards(String account) throws Exception {
        return RepositoryPendingRewardSnapshot.unavailable();
    }

    default public RepositoryPendingRewardClaim claimPendingReward(long characterObjectId, String account, RepositoryPendingReward expected, String characterName, String ip, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, RuntimeInventory beforePersonalWarehouse, RuntimeInventory afterPersonalWarehouse, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryPendingRewardClaim.unavailable(expected);
    }

    default public RepositoryCastleOwnershipStatus persistCastleOwnership(long winnerLeaderObjectId, int castleId) throws Exception {
        return RepositoryCastleOwnershipStatus.UNAVAILABLE;
    }

    default public RepositoryCastleTreasuryMutation persistCastleTreasuryTransfer(long ownerCharacterObjectId, RepositoryCastleSnapshot expected, RepositoryCastleSnapshot replacement, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return RepositoryCastleTreasuryMutation.unavailable();
    }

    public boolean characterExists(String var1) throws Exception;

    default public RepositoryCharacterMutation createLitoCharacter(String account, Protocol.CustomCharacterRequest request) throws Exception {
        return RepositoryCharacterMutation.readOnly();
    }

    default public RepositoryCharacterMutation deleteLitoCharacter(String account, String characterName) throws Exception {
        return RepositoryCharacterMutation.readOnly();
    }

    default public RepositoryPersistenceResult persistLitoCharacter(LitoCharacterPersistence persistence) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }

    default public RepositoryPersistenceResult persistLitoSourceInventory(long characterObjectId, List<LitoSourceInventoryMutation> mutations) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }

    default public RepositoryPersistenceResult persistLitoInventory(long characterObjectId, List<LitoSourceInventoryMutation> sourceMutations, List<LitoOwnedInventoryItem> litoOwnedItems) throws Exception {
        return this.persistLitoSourceInventory(characterObjectId, sourceMutations);
    }

    default public RepositoryPersistenceResult persistLitoInventory(long characterObjectId, List<LitoSourceInventoryMutation> sourceMutations, List<LitoOwnedInventoryItem> litoOwnedItems, List<LitoPetPersistence> pets, Set<Long> deletedSourcePetCollarIds) throws Exception {
        return this.persistLitoInventory(characterObjectId, sourceMutations, litoOwnedItems);
    }

    default public RepositoryPersistenceResult persistLitoInventory(long characterObjectId, List<LitoSourceInventoryMutation> sourceMutations, List<LitoOwnedInventoryItem> litoOwnedItems, List<LitoPetPersistence> pets, Set<Long> deletedSourcePetCollarIds, RepositoryPrivateInventorySnapshot expectedPrivateInventory) throws Exception {
        return this.persistLitoInventory(characterObjectId, sourceMutations, litoOwnedItems, pets, deletedSourcePetCollarIds);
    }

    default public RepositoryPersistenceResult persistLitoHotelInventory(long characterObjectId, List<LitoSourceInventoryMutation> sourceMutations, List<LitoOwnedInventoryItem> litoOwnedItems, List<LitoPetPersistence> pets, Set<Long> deletedSourcePetCollarIds, RepositoryPrivateInventorySnapshot expectedPrivateInventory, RepositoryHotelChange hotelChange) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }

    default public RepositoryPersistenceResult persistLitoWaterDragonPassInventory(long characterObjectId, List<LitoSourceInventoryMutation> sourceMutations, List<LitoOwnedInventoryItem> litoOwnedItems, List<LitoPetPersistence> pets, Set<Long> deletedSourcePetCollarIds, RepositoryPrivateInventorySnapshot expectedPrivateInventory, RepositoryWaterDragonPassChange passChange) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }

    default public RepositoryPersistenceResult persistTournamentRewardInventory(long characterObjectId, RepositoryPrivateInventorySnapshot expectedPrivateInventory, List<LitoOwnedInventoryItem> replacementRewards) throws Exception {
        return RepositoryPersistenceResult.READ_ONLY;
    }

    public String description();

    default public AccountRepository repositoryForClassicSource(String classicServerName) {
        return this;
    }

    @Override
    default public void close() throws Exception {
    }

    public static AccountRepository open(ServerConfig config) throws Exception {
        return switch (config.databaseMode()) {
            case "memory" -> AccountRepository.openMemory(config);
            case "legacy_mysql", "lito_mysql" -> AccountRepository.openLegacy(config);
            default -> throw new IllegalArgumentException("Unsupported database.mode: " + config.databaseMode());
        };
    }

    private static AccountRepository openMemory(ServerConfig config) {
        if (config.classicServerNames().size() == 1) {
            return new MemoryAccountRepository(config.memoryAccount(), config.memoryPassword(), config.memoryAuthenticationFailureAccount(), config.memorySourceInventoryWeight(), config.memorySourceWeightRate(), config.memorySourceFood(), config.memorySourcePrimaryLevel(), config.memorySourcePrimaryMaxHp(), config.memorySourcePrimaryMaxMp(), config.memorySourcePrimaryArmorClass(), config.memorySourcePrimaryStrength(), config.memorySourcePrimaryConstitution(), config.memorySourcePrimaryDexterity(), config.memorySourcePrimaryCharisma(), config.memorySourcePrimaryIntelligence(), config.memorySourcePrimaryWisdom(), config.memorySourceSecondaryMaxMp(), config.memorySourceInventoryFixtureEnabled(), config.memorySourceProjectileFixtureEnabled(), config.memoryCastleWarFixtureStartDelaySeconds(), config.memoryCastleWarFixtureDurationMinutes(), config.memoryCastleWarFixtureIntervalMinutes());
        }
        LinkedHashMap<String, AccountRepository> routes = new LinkedHashMap<String, AccountRepository>();
        for (String source : config.classicServerNames()) {
            routes.put(source, new MemoryAccountRepository(config.memoryAccount(), config.memoryPassword(), config.memoryAuthenticationFailureAccount(), config.memorySourceInventoryWeight(), config.memorySourceWeightRate(), config.memorySourceFood(), config.memorySourcePrimaryLevel(), config.memorySourcePrimaryMaxHp(), config.memorySourcePrimaryMaxMp(), config.memorySourcePrimaryArmorClass(), config.memorySourcePrimaryStrength(), config.memorySourcePrimaryConstitution(), config.memorySourcePrimaryDexterity(), config.memorySourcePrimaryCharisma(), config.memorySourcePrimaryIntelligence(), config.memorySourcePrimaryWisdom(), config.memorySourceSecondaryMaxMp(), config.memorySourceInventoryFixtureEnabled(), config.memorySourceProjectileFixtureEnabled(), config.memoryCastleWarFixtureStartDelaySeconds(), config.memoryCastleWarFixtureDurationMinutes(), config.memoryCastleWarFixtureIntervalMinutes()));
        }
        return new SourceRoutedAccountRepository(routes);
    }

    private static AccountRepository openLegacy(ServerConfig serverConfig) throws Exception {
        ServerConfig config;
        if (SharedDatabase.enabled(serverConfig)) {
            return SharedDatabase.open(serverConfig);
        }
        List<String> databases = config.legacySourceDatabases();
        List<String> resourceDatabases = config.legacySourceResourceDatabases();
        List<Double> weightRates = config.legacySourceWeightRates();
        List<Integer> sourceIds = config.classicServerIds();
        if (resourceDatabases.size() != config.classicServerNames().size()) {
            throw new IllegalArgumentException("classic source/resource database route count is inconsistent");
        }
        if (config.classicServerNames().size() == 1) {
            String database = databases.isEmpty() ? config.legacyLoginDatabase() : databases.get(0);
            return new LegacyMySqlRepository(config.legacySqlProperties(), config.classicServerNames().get(0), sourceIds.get(0), database, resourceDatabases.get(0), weightRates.get(0), config.databaseReadOnly(), config.databaseMode());
        }
        if (databases.size() != config.classicServerNames().size()) {
            throw new IllegalArgumentException("classic source/database route count is inconsistent");
        }
        LinkedHashMap<String, AccountRepository> routes = new LinkedHashMap<String, AccountRepository>();
        for (int index = 0; index < config.classicServerNames().size(); ++index) {
            routes.put(config.classicServerNames().get(index), new LegacyMySqlRepository(config.legacySqlProperties(), config.classicServerNames().get(index), sourceIds.get(index), databases.get(index), resourceDatabases.get(index), weightRates.get(index), config.databaseReadOnly(), config.databaseMode()));
        }
        return new SourceRoutedAccountRepository(routes);
    }
}
