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
        // T120 (#339): native blessing 0..3; 3 is the illusion state that
        // 380_db weapon rows 幻象之劍/幻象之弓 carry (1-伺服器 BLESS_ILLUSION).
        if (material < 0 || material > 22 || blessing < 0 || blessing > 3
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
                || material < 0 || material > 22 || blessing < 0 || blessing > 3
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
