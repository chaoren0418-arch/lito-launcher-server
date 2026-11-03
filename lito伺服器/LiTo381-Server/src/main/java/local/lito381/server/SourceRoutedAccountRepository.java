// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import local.lito381.server.AccountMutationResult;
import local.lito381.server.AccountRepository;
import local.lito381.server.AuthResult;
import local.lito381.server.CastleSummary;
import local.lito381.server.CastleWarSchedule;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClanEmblemData;
import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryBattleConfiguration;
import local.lito381.server.RepositoryBoardLookup;
import local.lito381.server.RepositoryBoardMutation;
import local.lito381.server.RepositoryBoardSnapshot;
import local.lito381.server.RepositoryCastleMutation;
import local.lito381.server.RepositoryCastleOwnershipStatus;
import local.lito381.server.RepositoryCastleSecuritySettlement;
import local.lito381.server.RepositoryCastleSnapshot;
import local.lito381.server.RepositoryCastleTreasuryMutation;
import local.lito381.server.RepositoryCharacterTitle;
import local.lito381.server.RepositoryHotelSnapshot;
import local.lito381.server.RepositoryHouseAuctionSnapshot;
import local.lito381.server.RepositoryHouseBidMutation;
import local.lito381.server.RepositoryHouseSettlement;
import local.lito381.server.RepositoryHouseTaxMutation;
import local.lito381.server.RepositoryHouseTaxPayment;
import local.lito381.server.RepositoryHouseTaxQuote;
import local.lito381.server.RepositoryHouseTaxSnapshot;
import local.lito381.server.RepositoryHouseTaxState;
import local.lito381.server.RepositoryMarriageMutation;
import local.lito381.server.RepositoryMarriageState;
import local.lito381.server.RepositoryPrivateInventorySnapshot;
import local.lito381.server.RepositoryTitleMutation;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.SourceBaseMagicResistance;
import local.lito381.server.SourceEquipmentEvaluation;
import local.lito381.server.SourceEquipmentMagicStats;
import local.lito381.server.SourceEquippedFireResistance;
import local.lito381.server.SourceInventoryItem;
import local.lito381.server.SourceInventoryWeightStats;
import local.lito381.server.SourceKillDeathCounts;
import local.lito381.server.SourceNpcPhysicalStats;
import local.lito381.server.SourcePet;
import local.lito381.server.SourcePetTemplate;
import local.lito381.server.SourcePhysicalGrowthStats;
import local.lito381.server.SourceSummonTemplate;

final class SourceRoutedAccountRepository
implements AccountRepository {
    private final Map<String, Route> routes = new LinkedHashMap<String, Route>();

    SourceRoutedAccountRepository(Map<String, AccountRepository> sourceRoutes) {
        if (sourceRoutes == null || sourceRoutes.size() < 2) {
            throw new IllegalArgumentException("source router requires at least two routes");
        }
        for (Map.Entry<String, AccountRepository> entry : sourceRoutes.entrySet()) {
            String name = entry.getKey() == null ? "" : entry.getKey().trim();
            AccountRepository repository = entry.getValue();
            String key = SourceRoutedAccountRepository.key(name);
            if (key.isEmpty() || repository == null || this.routes.containsKey(key)) {
                throw new IllegalArgumentException("invalid or duplicate classic source route");
            }
            this.routes.put(key, new Route(name, repository));
        }
    }

    @Override
    public void healthCheck() throws Exception {
        for (Route route : this.routes.values()) {
            route.repository().healthCheck();
        }
    }

    @Override
    public void migrateLitoExtensions() throws Exception {
        for (Route route : this.routes.values()) {
            route.repository().migrateLitoExtensions();
        }
    }

    @Override
    public AuthResult authenticate(String account, String password) throws Exception {
        LinkedHashSet<String> authorized = new LinkedHashSet<String>();
        String canonicalAccount = account;
        boolean banned = false;
        Exception backendFailure = null;
        for (Route route : this.routes.values()) {
            try {
                AuthResult result = route.repository().authenticate(account, password);
                if (result.accepted()) {
                    authorized.add(route.name());
                    if (authorized.size() != 1) continue;
                    canonicalAccount = result.account();
                    continue;
                }
                if (!result.banned()) continue;
                banned = true;
            }
            catch (Exception failure) {
                if (backendFailure == null) {
                    backendFailure = failure;
                    continue;
                }
                backendFailure.addSuppressed(failure);
            }
        }
        if (!authorized.isEmpty()) {
            return new AuthResult(true, false, canonicalAccount, authorized);
        }
        if (backendFailure != null) {
            throw backendFailure;
        }
        return new AuthResult(false, banned, canonicalAccount, Set.of());
    }

    @Override
    public RepositoryBattleConfiguration battleConfiguration() throws Exception {
        return this.primary().battleConfiguration();
    }

    @Override
    public AccountRepository repositoryForClassicSource(String classicServerName) {
        Route route = this.routes.get(SourceRoutedAccountRepository.key(classicServerName));
        if (route == null) {
            throw new IllegalArgumentException("unknown classic source repository");
        }
        return route.repository();
    }

    @Override
    public AccountMutationResult createAccount(String account, String password) throws Exception {
        for (Route route : this.routes.values()) {
            if (!route.repository().accountExists(account)) continue;
            return AccountMutationResult.EXISTS;
        }
        ArrayList<AccountRepository> created = new ArrayList<AccountRepository>();
        try {
            for (Route route : this.routes.values()) {
                AccountMutationResult result = route.repository().createAccount(account, password);
                if (result != AccountMutationResult.SUCCESS) {
                    SourceRoutedAccountRepository.compensateAccountCreation(account, created, null);
                    return result;
                }
                created.add(route.repository());
            }
            return AccountMutationResult.SUCCESS;
        }
        catch (Exception exception) {
            SourceRoutedAccountRepository.compensateAccountCreation(account, created, exception);
            throw exception;
        }
    }

    @Override
    public AccountMutationResult changePassword(String account, String oldPassword, String newPassword) throws Exception {
        for (Route route : this.routes.values()) {
            if (!route.repository().accountExists(account)) {
                return AccountMutationResult.NOT_FOUND;
            }
            AuthResult authentication = route.repository().authenticate(account, oldPassword);
            if (authentication.accepted()) continue;
            return AccountMutationResult.WRONG_PASSWORD;
        }
        ArrayList<AccountRepository> changed = new ArrayList<AccountRepository>();
        try {
            for (Route route : this.routes.values()) {
                AccountMutationResult result = route.repository().changePassword(account, oldPassword, newPassword);
                if (result != AccountMutationResult.SUCCESS) {
                    SourceRoutedAccountRepository.compensatePasswordChange(account, oldPassword, newPassword, changed, null);
                    return result;
                }
                changed.add(route.repository());
            }
            return AccountMutationResult.SUCCESS;
        }
        catch (Exception exception) {
            SourceRoutedAccountRepository.compensatePasswordChange(account, oldPassword, newPassword, changed, exception);
            throw exception;
        }
    }

    @Override
    public boolean accountExists(String account) throws Exception {
        for (Route route : this.routes.values()) {
            if (!route.repository().accountExists(account)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteLitoAccount(String account) throws Exception {
        boolean deleted = true;
        Exception failure = null;
        for (Route route : this.routes.values()) {
            try {
                deleted &= route.repository().deleteLitoAccount(account);
            }
            catch (Exception routeFailure) {
                deleted = false;
                if (failure == null) {
                    failure = routeFailure;
                    continue;
                }
                failure.addSuppressed(routeFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
        return deleted;
    }

    @Override
    public AccountMutationResult registerQuiz(String account, String oldPassword, String newPassword, List<String> quizFields) throws Exception {
        return this.primary().registerQuiz(account, oldPassword, newPassword, quizFields);
    }

    @Override
    public AccountMutationResult changeAccountInfo(String account, int type, List<String> fields) throws Exception {
        return this.primary().changeAccountInfo(account, type, fields);
    }

    @Override
    public List<CharacterSummary> characters(String account) throws Exception {
        return this.primary().characters(account);
    }

    @Override
    public int hellTime(long characterObjectId) throws Exception {
        return this.primary().hellTime(characterObjectId);
    }

    @Override
    public SourceKillDeathCounts killDeathCounts(long characterObjectId) throws Exception {
        return this.primary().killDeathCounts(characterObjectId);
    }

    @Override
    public Set<Integer> learnedSkills(long characterObjectId) throws Exception {
        return this.primary().learnedSkills(characterObjectId);
    }

    @Override
    public Map<Integer, Integer> questSteps(long characterObjectId) throws Exception {
        return this.primary().questSteps(characterObjectId);
    }

    @Override
    public List<SourceInventoryItem> sourceInventory(long characterObjectId) throws Exception {
        return this.primary().sourceInventory(characterObjectId);
    }

    @Override
    public RepositoryPrivateInventorySnapshot litoPrivateInventory(long characterObjectId) throws Exception {
        return this.primary().litoPrivateInventory(characterObjectId);
    }

    @Override
    public RepositoryBoardSnapshot boardTopicsBefore(long cursor, int limit) throws Exception {
        return this.primary().boardTopicsBefore(cursor, limit);
    }

    @Override
    public RepositoryBoardLookup boardTopic(long topicId) throws Exception {
        return this.primary().boardTopic(topicId);
    }

    @Override
    public RepositoryBoardMutation postBoardTopic(long characterObjectId, String date, String title, String content, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return this.primary().postBoardTopic(characterObjectId, date, title, content, beforeInventory, afterInventory, localToSourceObjectIds);
    }

    @Override
    public RepositoryBoardMutation deleteBoardTopic(long characterObjectId, GameStateStore.BoardTopic expected) throws Exception {
        return this.primary().deleteBoardTopic(characterObjectId, expected);
    }

    @Override
    public RepositoryHouseAuctionSnapshot houseAuctions() throws Exception {
        return this.primary().houseAuctions();
    }

    @Override
    public RepositoryHouseBidMutation persistHouseBid(long bidderCharacterObjectId, GameStateStore.HouseAuction expected, long amount, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return this.primary().persistHouseBid(bidderCharacterObjectId, expected, amount, beforeInventory, afterInventory, localToSourceObjectIds);
    }

    @Override
    public RepositoryHouseSettlement persistHouseAuctionSettlement(GameStateStore.HouseAuction expected, long nowEpochSeconds, int taxIntervalDays) throws Exception {
        return this.primary().persistHouseAuctionSettlement(expected, nowEpochSeconds, taxIntervalDays);
    }

    @Override
    public RepositoryHouseTaxSnapshot expiredHouseTaxes(long nowEpochSeconds) throws Exception {
        return this.primary().expiredHouseTaxes(nowEpochSeconds);
    }

    @Override
    public RepositoryHouseTaxMutation persistHouseTaxExpiry(RepositoryHouseTaxState expected, long nowEpochSeconds, int taxIntervalDays) throws Exception {
        return this.primary().persistHouseTaxExpiry(expected, nowEpochSeconds, taxIntervalDays);
    }

    @Override
    public RepositoryHouseTaxQuote houseTaxQuote(long characterObjectId, int keeperNpcId) throws Exception {
        return this.primary().houseTaxQuote(characterObjectId, keeperNpcId);
    }

    @Override
    public RepositoryHouseTaxPayment persistHouseTaxPayment(long characterObjectId, RepositoryHouseTaxQuote expected, long nowEpochSeconds, int taxIntervalDays, long taxAdena, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return this.primary().persistHouseTaxPayment(characterObjectId, expected, nowEpochSeconds, taxIntervalDays, taxAdena, beforeInventory, afterInventory, localToSourceObjectIds);
    }

    @Override
    public RepositoryHotelSnapshot hotelState() throws Exception {
        return this.primary().hotelState();
    }

    @Override
    public SourceEquippedFireResistance equippedFireResistance(long characterObjectId) throws Exception {
        return this.primary().equippedFireResistance(characterObjectId);
    }

    @Override
    public SourceBaseMagicResistance baseMagicResistance(long characterObjectId) throws Exception {
        return this.primary().baseMagicResistance(characterObjectId);
    }

    @Override
    public SourceEquipmentMagicStats equipmentMagicStats(long characterObjectId) throws Exception {
        return this.primary().equipmentMagicStats(characterObjectId);
    }

    @Override
    public Set<Integer> counterMagicBypassSkills() throws Exception {
        return this.primary().counterMagicBypassSkills();
    }

    @Override
    public SourceEquipmentEvaluation equipmentEvaluation(long characterObjectId) throws Exception {
        return this.primary().equipmentEvaluation(characterObjectId);
    }

    @Override
    public SourceEquipmentEvaluation equipmentEvaluation(long characterObjectId, Map<Long, Boolean> equippedOverrides) throws Exception {
        return this.primary().equipmentEvaluation(characterObjectId, equippedOverrides);
    }

    @Override
    public SourcePhysicalGrowthStats physicalGrowthStats() throws Exception {
        return this.primary().physicalGrowthStats();
    }

    @Override
    public SourceNpcPhysicalStats npcPhysicalStats(int npcId) throws Exception {
        return this.primary().npcPhysicalStats(npcId);
    }

    @Override
    public SourceSummonTemplate summonTemplate(int npcId) throws Exception {
        return this.primary().summonTemplate(npcId);
    }

    @Override
    public List<SourcePet> pets(long characterObjectId) throws Exception {
        return this.primary().pets(characterObjectId);
    }

    @Override
    public SourcePetTemplate petTemplate(int npcId) throws Exception {
        return this.primary().petTemplate(npcId);
    }

    @Override
    public SourceInventoryWeightStats inventoryWeightStats(long characterObjectId) throws Exception {
        return this.primary().inventoryWeightStats(characterObjectId);
    }

    @Override
    public ClanEmblemData clanEmblem(String clanName) throws Exception {
        return this.primary().clanEmblem(clanName);
    }

    @Override
    public CastleSummary castleForClan(String clanName) throws Exception {
        return this.primary().castleForClan(clanName);
    }

    @Override
    public CastleSummary castleById(int castleId) throws Exception {
        return this.primary().castleById(castleId);
    }

    @Override
    public List<CastleWarSchedule> castleWarSchedules() throws Exception {
        return this.primary().castleWarSchedules();
    }

    @Override
    public RepositoryCastleSnapshot castleState(int castleId) throws Exception {
        return this.primary().castleState(castleId);
    }

    @Override
    public RepositoryCharacterTitle characterTitle(long characterObjectId) throws Exception {
        return this.primary().characterTitle(characterObjectId);
    }

    @Override
    public RepositoryTitleMutation persistCharacterTitle(long characterObjectId, String expected, String replacement) throws Exception {
        return this.primary().persistCharacterTitle(characterObjectId, expected, replacement);
    }

    @Override
    public RepositoryMarriageState marriage(long characterObjectId) throws Exception {
        return this.primary().marriage(characterObjectId);
    }

    @Override
    public RepositoryMarriageMutation persistMarriage(long firstCharacterObjectId, long secondCharacterObjectId) throws Exception {
        return this.primary().persistMarriage(firstCharacterObjectId, secondCharacterObjectId);
    }

    @Override
    public RepositoryMarriageMutation persistDivorce(long characterObjectId, long expectedPartnerObjectId) throws Exception {
        return this.primary().persistDivorce(characterObjectId, expectedPartnerObjectId);
    }

    @Override
    public RepositoryCastleMutation persistCastleState(long ownerCharacterObjectId, RepositoryCastleSnapshot expected, RepositoryCastleSnapshot replacement) throws Exception {
        return this.primary().persistCastleState(ownerCharacterObjectId, expected, replacement);
    }

    @Override
    public RepositoryCastleMutation persistCastleWarEnd(RepositoryCastleSnapshot expected, RepositoryCastleSnapshot replacement) throws Exception {
        return this.primary().persistCastleWarEnd(expected, replacement);
    }

    @Override
    public RepositoryCastleSecuritySettlement prepareCastleSecurity(int castleId, long warStartMillis, long chargeAdena) throws Exception {
        return this.primary().prepareCastleSecurity(castleId, warStartMillis, chargeAdena);
    }

    @Override
    public RepositoryCastleSecuritySettlement completeCastleSecurity(int castleId, long warStartMillis) throws Exception {
        return this.primary().completeCastleSecurity(castleId, warStartMillis);
    }

    @Override
    public RepositoryCastleOwnershipStatus persistCastleOwnership(long winnerLeaderObjectId, int castleId) throws Exception {
        return this.primary().persistCastleOwnership(winnerLeaderObjectId, castleId);
    }

    @Override
    public RepositoryCastleTreasuryMutation persistCastleTreasuryTransfer(long ownerCharacterObjectId, RepositoryCastleSnapshot expected, RepositoryCastleSnapshot replacement, RuntimeInventory beforeInventory, RuntimeInventory afterInventory, Map<Long, Long> localToSourceObjectIds) throws Exception {
        return this.primary().persistCastleTreasuryTransfer(ownerCharacterObjectId, expected, replacement, beforeInventory, afterInventory, localToSourceObjectIds);
    }

    @Override
    public boolean characterExists(String characterName) throws Exception {
        return this.primary().characterExists(characterName);
    }

    @Override
    public String description() {
        return this.routes.values().stream().map(route -> route.name() + "=" + route.repository().description()).collect(Collectors.joining(", ", "source_router(", ")"));
    }

    @Override
    public void close() throws Exception {
        Exception closeFailure = null;
        Set closed = Collections.newSetFromMap(new IdentityHashMap());
        for (Route route : this.routes.values()) {
            if (!closed.add(route.repository())) continue;
            try {
                route.repository().close();
            }
            catch (Exception failure) {
                if (closeFailure == null) {
                    closeFailure = failure;
                    continue;
                }
                closeFailure.addSuppressed(failure);
            }
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    private AccountRepository primary() {
        return this.routes.values().iterator().next().repository();
    }

    private static void compensateAccountCreation(String account, List<AccountRepository> created, Exception original) throws Exception {
        Exception cleanupFailure = null;
        for (int index = created.size() - 1; index >= 0; --index) {
            try {
                if (created.get(index).deleteLitoAccount(account)) continue;
                throw new SQLException("LiTo account creation compensation was refused");
            }
            catch (Exception failure) {
                if (cleanupFailure == null) {
                    cleanupFailure = failure;
                    continue;
                }
                cleanupFailure.addSuppressed(failure);
            }
        }
        if (cleanupFailure != null) {
            if (original != null) {
                original.addSuppressed(cleanupFailure);
            } else {
                throw cleanupFailure;
            }
        }
    }

    private static void compensatePasswordChange(String account, String oldPassword, String newPassword, List<AccountRepository> changed, Exception original) throws Exception {
        Exception cleanupFailure = null;
        for (int index = changed.size() - 1; index >= 0; --index) {
            try {
                AccountMutationResult result = changed.get(index).changePassword(account, newPassword, oldPassword);
                if (result == AccountMutationResult.SUCCESS) continue;
                throw new SQLException("LiTo password compensation failed: " + String.valueOf((Object)result));
            }
            catch (Exception failure) {
                if (cleanupFailure == null) {
                    cleanupFailure = failure;
                    continue;
                }
                cleanupFailure.addSuppressed(failure);
            }
        }
        if (cleanupFailure != null) {
            if (original != null) {
                original.addSuppressed(cleanupFailure);
            } else {
                throw cleanupFailure;
            }
        }
    }

    private static String key(String source) {
        return source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
    }

    private record Route(String name, AccountRepository repository) {
    }
}
