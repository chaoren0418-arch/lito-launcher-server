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
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlCastleIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    private static final int CASTLE_ID = 8;

    @Test
    void castleSecuritySettlementChargesOncePerSourceWarAndRemembersShortfall(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("security-writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        Jdbc secondJdbc = jdbc(writable, "380_ac_2");
        int castleId = 7;
        NativeCastleBefore nativeBefore = nativeCastle(firstJdbc, castleId);
        CastleMetadataBefore metadataBefore = castleMetadata(firstJdbc, castleId);
        long firstWar = 1_900_000_100_000L;
        long secondWar = 1_900_000_200_000L;
        long thirdWar = 1_900_000_300_000L;

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            routed.migrateLitoExtensions();
            AccountRepository first = routed.repositoryForClassicSource(
                    "380 角色庫 1");
            AccountRepository second = routed.repositoryForClassicSource(
                    "380 角色庫 2");
            deleteCastleSecuritySettlements(firstJdbc, castleId);
            deleteCastleSecuritySettlements(secondJdbc, castleId);
            configureSecurityCastle(firstJdbc, castleId, 250_000, true,
                    metadataBefore.mercenaries(), metadataBefore.snapshotVersion() + 10);
            RepositoryCastleSnapshot otherRouteBefore = second.castleState(castleId);

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("security-readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryCastleSecuritySettlement refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .prepareCastleSecurity(castleId, firstWar, 100_000);
                assertEquals(RepositoryCastleSecuritySettlementStatus.READ_ONLY,
                        refused.status());
                assertEquals(250_000,
                        nativeCastle(firstJdbc, castleId).publicMoney());
            }

            RepositoryCastleSecuritySettlement prepared = first
                    .prepareCastleSecurity(castleId, firstWar, 100_000);
            assertEquals(RepositoryCastleSecuritySettlementStatus.READY,
                    prepared.status());
            assertEquals(100_000, prepared.chargedAdena());
            assertEquals(150_000, prepared.snapshot().publicMoney());
            RepositoryCastleSecuritySettlement retry = first
                    .prepareCastleSecurity(castleId, firstWar, 100_000);
            assertEquals(RepositoryCastleSecuritySettlementStatus.READY,
                    retry.status());
            assertEquals(150_000, retry.snapshot().publicMoney());
            assertEquals(RepositoryCastleSecuritySettlementStatus.ALREADY_SETTLED,
                    first.completeCastleSecurity(castleId, firstWar).status());
            assertEquals(RepositoryCastleSecuritySettlementStatus.ALREADY_SETTLED,
                    first.prepareCastleSecurity(
                            castleId, firstWar, 100_000).status());

            RepositoryCastleSecuritySettlement secondPrepared = first
                    .prepareCastleSecurity(castleId, secondWar, 100_000);
            assertEquals(RepositoryCastleSecuritySettlementStatus.READY,
                    secondPrepared.status());
            assertEquals(50_000, secondPrepared.snapshot().publicMoney());
            assertEquals(RepositoryCastleSecuritySettlementStatus.ALREADY_SETTLED,
                    first.completeCastleSecurity(castleId, secondWar).status());

            RepositoryCastleSecuritySettlement insufficient = first
                    .prepareCastleSecurity(castleId, thirdWar, 100_000);
            assertEquals(RepositoryCastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                    insufficient.status());
            assertEquals(50_000, insufficient.snapshot().publicMoney());
            assertEquals(3, castleSecuritySettlementCount(firstJdbc, castleId));
            assertEquals(0, castleSecuritySettlementCount(secondJdbc, castleId));
            assertEquals(otherRouteBefore, second.castleState(castleId));
        } finally {
            deleteCastleSecuritySettlements(firstJdbc, castleId);
            deleteCastleSecuritySettlements(secondJdbc, castleId);
            restoreCastle(firstJdbc, castleId, nativeBefore, metadataBefore);
        }
    }

    @Test
    void nativeCastleFieldsAndLitoMetadataUseOwnerAuthorizedSameRouteCas(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        Jdbc secondJdbc = jdbc(writable, "380_ac_2");
        NativeCastleBefore nativeBefore = nativeCastle(firstJdbc, CASTLE_ID);
        CastleMetadataBefore metadataBefore = castleMetadata(firstJdbc, CASTLE_ID);
        String account = "LtCastleGate";
        String ownerName = "LitoCastleOwn";
        String challengerName = "LitoCastleWin";
        String clanName = "LitoCastleSql";
        String challengerClanName = "LitoCastleNew";
        long ownerId = 0;
        long challengerId = 0;
        long clanId = 0;
        long challengerClanId = 0;
        long adenaObjectId = 0;

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            routed.migrateLitoExtensions();
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            cleanup(source, firstJdbc, account, ownerName);
            deleteTestCastleRewards(firstJdbc, account);
            deleteTestCastleRewards(secondJdbc, account);
            deleteTestWarehouseRewards(firstJdbc, account);
            deleteTestWarehouseRewards(secondJdbc, account);
            routed.deleteLitoAccount(account);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(account, "CastleGate1"));
            RepositoryCharacterMutation created = source.createLitoCharacter(account,
                    new Protocol.CustomCharacterRequest(ownerName, 0, 1,
                            List.of(18, 13, 15, 9, 12, 8), 0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
            ownerId = created.character().objectId();
            RepositoryClanMutation clan = source.createClan(ownerId, clanName);
            assertEquals(RepositoryClanMutationStatus.SUCCESS, clan.status());
            clanId = clan.snapshot().clanId();
            RepositoryCharacterMutation challenger = source.createLitoCharacter(account,
                    new Protocol.CustomCharacterRequest(challengerName, 0, 1,
                            List.of(18, 13, 15, 9, 12, 8), 0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    challenger.status());
            challengerId = challenger.character().objectId();
            RepositoryClanMutation challengerClan = source.createClan(
                    challengerId, challengerClanName);
            assertEquals(RepositoryClanMutationStatus.SUCCESS,
                    challengerClan.status());
            challengerClanId = challengerClan.snapshot().clanId();
            assignCastle(firstJdbc, clanId, CASTLE_ID);
            configureCastleReward(firstJdbc, CASTLE_ID,
                    Math.toIntExact(InventoryCatalog.ADENA), 7);
            assertEquals(CASTLE_ID, source.clan(ownerId).castleId());
            adenaObjectId = allocateInventoryObjectId(firstJdbc);
            insertAdena(firstJdbc, ownerId, adenaObjectId, 1_000);
            RuntimeInventory treasuryInventory = adenaInventory(adenaObjectId, 1_000);

            RepositoryCastleSnapshot initial = source.castleState(CASTLE_ID);
            assertTrue(initial.authoritative());
            assertEquals(CASTLE_ID, initial.castleId());
            RepositoryCastleSnapshot otherRoute = routed
                    .repositoryForClassicSource("380 角色庫 2")
                    .castleState(CASTLE_ID);
            assertTrue(otherRoute.authoritative());

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryCastleMutation refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistCastleState(ownerId, initial,
                                replacement(initial, initial.taxRate(),
                                        initial.publicMoney(), !initial.securityEnabled(),
                                        new GameStateStore.MercenaryRoster(1, 2, 3, 4)));
                assertEquals(RepositoryCastleMutationStatus.READ_ONLY, refused.status());
                assertEquals(initial, source.castleState(CASTLE_ID));

                long credit = initial.publicMoney() == Integer.MAX_VALUE ? -1 : 1;
                RuntimeInventory after = adjustedAdena(
                        treasuryInventory, credit);
                RepositoryCastleTreasuryMutation treasuryRefused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistCastleTreasuryTransfer(ownerId, initial,
                                replacement(initial, initial.taxRate(),
                                        Math.addExact(initial.publicMoney(), credit),
                                        initial.securityEnabled(), initial.mercenaries()),
                                treasuryInventory, after,
                                Map.of(adenaObjectId, adenaObjectId));
                assertEquals(RepositoryCastleTreasuryStatus.READ_ONLY,
                        treasuryRefused.status());
                assertEquals(1_000, itemCount(firstJdbc, ownerId, adenaObjectId));
                assertEquals(RepositoryCastleOwnershipStatus.READ_ONLY,
                        readOnlyRouted.repositoryForClassicSource("380 角色庫 1")
                                .persistCastleOwnership(challengerId, CASTLE_ID));
                RepositoryCastleSnapshot readOnlyWar = source.castleState(CASTLE_ID);
                RepositoryCastleMutation warRefused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistCastleWarEnd(readOnlyWar,
                                warEndReplacement(readOnlyWar,
                                        readOnlyWar.warTimeMillis() + 60_000L));
                assertEquals(RepositoryCastleMutationStatus.READ_ONLY,
                        warRefused.status());
                assertEquals(0, castleRewardCount(firstJdbc,
                        castleRewardEventKey(readOnlyWar)));
            }

            int nextTax = initial.taxRate() == 50 ? 49 : initial.taxRate() + 1;
            long nextMoney = initial.publicMoney() == Integer.MAX_VALUE
                    ? initial.publicMoney() - 1 : initial.publicMoney() + 1;
            RepositoryCastleSnapshot requested = replacement(initial, nextTax,
                    nextMoney, !initial.securityEnabled(),
                    new GameStateStore.MercenaryRoster(1, 2, 3, 4));
            RepositoryCastleMutation changed = source.persistCastleState(
                    ownerId, initial, requested);
            assertEquals(RepositoryCastleMutationStatus.SUCCESS, changed.status());
            assertEquals(nextTax, changed.snapshot().taxRate());
            assertEquals(nextMoney, changed.snapshot().publicMoney());
            assertEquals(!initial.securityEnabled(), changed.snapshot().securityEnabled());
            assertEquals(new GameStateStore.MercenaryRoster(1, 2, 3, 4),
                    changed.snapshot().mercenaries());
            assertEquals(initial.snapshotVersion() + 1,
                    changed.snapshot().snapshotVersion());
            assertEquals(nextTax, nativeCastle(firstJdbc, CASTLE_ID).taxRate());
            assertEquals(nextMoney, nativeCastle(firstJdbc, CASTLE_ID).publicMoney());
            assertEquals(otherRoute, routed.repositoryForClassicSource("380 角色庫 2")
                    .castleState(CASTLE_ID));
            assertEquals(0, castleMetadata(secondJdbc, CASTLE_ID).snapshotVersion());

            RepositoryCastleMutation stale = source.persistCastleState(
                    ownerId, initial, requested);
            assertEquals(RepositoryCastleMutationStatus.CONFLICT, stale.status());
            assertEquals(changed.snapshot(), stale.snapshot());

            long treasuryCredit = changed.snapshot().publicMoney()
                    <= Integer.MAX_VALUE - 200L ? 200 : -200;
            RuntimeInventory treasuryAfter = adjustedAdena(
                    treasuryInventory, treasuryCredit);
            RepositoryCastleSnapshot treasuryRequested = replacement(changed.snapshot(),
                    changed.snapshot().taxRate(),
                    Math.addExact(changed.snapshot().publicMoney(), treasuryCredit),
                    changed.snapshot().securityEnabled(), changed.snapshot().mercenaries());
            RepositoryCastleTreasuryMutation treasuryChanged =
                    source.persistCastleTreasuryTransfer(ownerId, changed.snapshot(),
                            treasuryRequested, treasuryInventory, treasuryAfter,
                            Map.of(adenaObjectId, adenaObjectId));
            assertEquals(RepositoryCastleTreasuryStatus.SUCCESS,
                    treasuryChanged.status());
            assertEquals(1_000 - treasuryCredit,
                    itemCount(firstJdbc, ownerId, adenaObjectId));
            assertEquals(treasuryRequested.publicMoney(),
                    nativeCastle(firstJdbc, CASTLE_ID).publicMoney());
            assertEquals(changed.snapshot().snapshotVersion() + 1,
                    treasuryChanged.snapshot().snapshotVersion());

            RepositoryCastleTreasuryMutation staleTreasury =
                    source.persistCastleTreasuryTransfer(ownerId, changed.snapshot(),
                            treasuryRequested, treasuryInventory, treasuryAfter,
                            Map.of(adenaObjectId, adenaObjectId));
            assertEquals(RepositoryCastleTreasuryStatus.CONFLICT,
                    staleTreasury.status());
            assertEquals(treasuryChanged.snapshot(), staleTreasury.snapshot());

            RepositoryCastleSnapshot treasuryRestored = replacement(
                    treasuryChanged.snapshot(), treasuryChanged.snapshot().taxRate(),
                    changed.snapshot().publicMoney(),
                    treasuryChanged.snapshot().securityEnabled(),
                    treasuryChanged.snapshot().mercenaries());
            RepositoryCastleTreasuryMutation treasuryReversed =
                    source.persistCastleTreasuryTransfer(ownerId,
                            treasuryChanged.snapshot(), treasuryRestored,
                            treasuryAfter, treasuryInventory,
                            Map.of(adenaObjectId, adenaObjectId));
            assertEquals(RepositoryCastleTreasuryStatus.SUCCESS,
                    treasuryReversed.status());
            assertEquals(1_000, itemCount(firstJdbc, ownerId, adenaObjectId));
            assertEquals(changed.snapshot().publicMoney(),
                    nativeCastle(firstJdbc, CASTLE_ID).publicMoney());

            RepositoryCastleSnapshot restoredRequest = replacement(
                    treasuryReversed.snapshot(),
                    nativeBefore.normalizedTaxRate(), nativeBefore.publicMoney(),
                    metadataBefore.securityEnabled(), metadataBefore.mercenaries());
            assertEquals(RepositoryCastleMutationStatus.SUCCESS,
                    source.persistCastleState(ownerId, treasuryReversed.snapshot(),
                            restoredRequest).status());

            assertEquals(RepositoryCastleOwnershipStatus.SUCCESS,
                    source.persistCastleOwnership(challengerId, CASTLE_ID));
            assertEquals(0, source.clan(ownerId).castleId());
            assertEquals(CASTLE_ID, source.clan(challengerId).castleId());
            assertEquals(RepositoryCastleOwnershipStatus.ALREADY_OWNS_CASTLE,
                    source.persistCastleOwnership(challengerId, CASTLE_ID));
            assertEquals(RepositoryCastleOwnershipStatus.SUCCESS,
                    source.persistCastleOwnership(ownerId, CASTLE_ID));
            assertEquals(CASTLE_ID, source.clan(ownerId).castleId());
            assertEquals(0, source.clan(challengerId).castleId());

            RepositoryCastleSnapshot beforeWarEnd = source.castleState(CASTLE_ID);
            long nextWarTime = Math.max(1_000L,
                    beforeWarEnd.warTimeMillis() + 60_000L);
            RepositoryCastleMutation warEnded = source.persistCastleWarEnd(
                    beforeWarEnd, warEndReplacement(beforeWarEnd, nextWarTime));
            String rewardEventKey = castleRewardEventKey(beforeWarEnd);
            assertEquals(RepositoryCastleMutationStatus.SUCCESS, warEnded.status());
            assertEquals(10, warEnded.snapshot().taxRate());
            assertEquals(0, warEnded.snapshot().publicMoney());
            assertEquals(nextWarTime / 1_000L * 1_000L,
                    warEnded.snapshot().warTimeMillis());
            assertEquals(beforeWarEnd.snapshotVersion() + 1,
                    warEnded.snapshot().snapshotVersion());
            assertEquals(new PendingCastleReward(account,
                            Math.toIntExact(InventoryCatalog.ADENA), 7, 0),
                    castleReward(firstJdbc, rewardEventKey));
            assertEquals(1, castleRewardCount(firstJdbc, rewardEventKey));
            assertEquals(0, castleRewardCount(secondJdbc, rewardEventKey));
            assertEquals(RepositoryCastleMutationStatus.CONFLICT,
                    source.persistCastleWarEnd(beforeWarEnd,
                            warEndReplacement(beforeWarEnd, nextWarTime)).status());
            assertEquals(1, castleRewardCount(firstJdbc, rewardEventKey));

            RepositoryPendingRewardSnapshot pending = source.pendingRewards(account);
            assertTrue(pending.authoritative());
            assertEquals(1, pending.rewards().size());
            RepositoryPendingReward reward = pending.rewards().get(0);
            assertEquals(Math.toIntExact(InventoryCatalog.ADENA), reward.itemId());
            assertEquals(7, reward.count());
            RuntimeInventory rewardAfter = treasuryInventory.copy();
            assertTrue(rewardAfter.addSystemReward(
                    reward.definition(), reward.count(), reward.enchant()));
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryPendingRewardClaim readOnlyClaim = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .claimPendingReward(ownerId, account, reward, ownerName,
                                "127.0.0.1", treasuryInventory, rewardAfter,
                                new RuntimeInventory(), new RuntimeInventory(),
                                Map.of(adenaObjectId, adenaObjectId));
                assertEquals(RepositoryPendingRewardClaimStatus.READ_ONLY,
                        readOnlyClaim.status());
                assertEquals(1, source.pendingRewards(account).rewards().size());
            }
            RepositoryPendingRewardClaim claimed = source.claimPendingReward(
                    ownerId, account, reward, ownerName, "127.0.0.1",
                    treasuryInventory, rewardAfter,
                    new RuntimeInventory(), new RuntimeInventory(),
                    Map.of(adenaObjectId, adenaObjectId));
            assertEquals(RepositoryPendingRewardClaimStatus.SUCCESS, claimed.status());
            assertFalse(claimed.personalWarehouse());
            assertEquals(1_007, itemCatalogCount(firstJdbc, ownerId,
                    Math.toIntExact(InventoryCatalog.ADENA)));
            assertTrue(source.pendingRewards(account).rewards().isEmpty());
            assertEquals(new PendingCastleReward(account,
                            Math.toIntExact(InventoryCatalog.ADENA), 7, 1),
                    castleReward(firstJdbc, rewardEventKey));
            RepositoryPendingRewardClaim replay = source.claimPendingReward(
                    ownerId, account, reward, ownerName, "127.0.0.1",
                    treasuryInventory, rewardAfter,
                    new RuntimeInventory(), new RuntimeInventory(),
                    Map.of(adenaObjectId, adenaObjectId));
            assertEquals(RepositoryPendingRewardClaimStatus.NOT_FOUND, replay.status());
            assertEquals(1_007, itemCatalogCount(firstJdbc, ownerId,
                    Math.toIntExact(InventoryCatalog.ADENA)));

            insertPendingReward(firstJdbc, account,
                    Math.toIntExact(InventoryCatalog.ADENA), 5,
                    "lito:test-reward-warehouse:" + ownerId);
            RepositoryPendingReward warehouseReward = source.pendingRewards(account)
                    .rewards().get(0);
            RepositoryWarehouseSnapshot warehouseSnapshot = source.warehouse(
                    GameStateStore.WarehouseScope.PERSONAL, account);
            assertTrue(warehouseSnapshot.authoritative());
            RuntimeInventory warehouseBefore = warehouseSnapshot.runtimeInventory();
            RuntimeInventory warehouseAfter = warehouseBefore.copy();
            assertTrue(warehouseAfter.addSystemReward(
                    warehouseReward.definition(), warehouseReward.count(),
                    warehouseReward.enchant()));
            RepositoryPendingRewardClaim warehouseClaim = source.claimPendingReward(
                    ownerId, account, warehouseReward, ownerName, "127.0.0.1",
                    rewardAfter, rewardAfter, warehouseBefore, warehouseAfter,
                    Map.of(adenaObjectId, adenaObjectId));
            assertEquals(RepositoryPendingRewardClaimStatus.SUCCESS,
                    warehouseClaim.status());
            assertTrue(warehouseClaim.personalWarehouse());
            assertEquals(5, warehouseCatalogCount(firstJdbc, account,
                    Math.toIntExact(InventoryCatalog.ADENA)));
            assertTrue(source.pendingRewards(account).rewards().isEmpty());
        } finally {
            if (adenaObjectId != 0) {
                deleteInventoryItem(firstJdbc, ownerId, adenaObjectId);
            }
            restoreCastle(firstJdbc, CASTLE_ID, nativeBefore, metadataBefore);
            deleteTestCastleRewards(firstJdbc, account);
            deleteTestCastleRewards(secondJdbc, account);
            deleteTestWarehouseRewards(firstJdbc, account);
            deleteTestWarehouseRewards(secondJdbc, account);
            if (clanId != 0) assignCastle(firstJdbc, clanId, 0);
            if (challengerClanId != 0) assignCastle(firstJdbc, challengerClanId, 0);
            try (AccountRepository routed = AccountRepository.open(
                    ServerConfig.load(writableConfig))) {
                cleanup(routed.repositoryForClassicSource("380 角色庫 1"),
                        firstJdbc, account, ownerName);
                cleanup(routed.repositoryForClassicSource("380 角色庫 1"),
                        firstJdbc, account, challengerName);
                routed.deleteLitoAccount(account);
            }
        }
        assertEquals(nativeBefore, nativeCastle(firstJdbc, CASTLE_ID));
        assertEquals(metadataBefore, castleMetadata(firstJdbc, CASTLE_ID));
        assertEquals(0, count(firstJdbc,
                "SELECT COUNT(*) FROM accounts WHERE login=?", account));
        assertEquals(0, count(firstJdbc,
                "SELECT COUNT(*) FROM characters WHERE account_name=?", account));
        assertEquals(0, count(firstJdbc,
                "SELECT COUNT(*) FROM clan_data WHERE clan_id=?", clanId));
        assertEquals(0, count(firstJdbc,
                "SELECT COUNT(*) FROM clan_data WHERE clan_id=?", challengerClanId));
        assertEquals(0, count(firstJdbc,
                "SELECT COUNT(*) FROM character_items WHERE char_id=?", ownerId));
        assertEquals(0, count(firstJdbc,
                "SELECT COUNT(*) FROM lito_item_origin WHERE char_obj_id=?", ownerId));
    }

    private static RepositoryCastleSnapshot replacement(
            RepositoryCastleSnapshot expected, int taxRate, long publicMoney,
            boolean securityEnabled, GameStateStore.MercenaryRoster mercenaries) {
        return new RepositoryCastleSnapshot(true, expected.castleId(), taxRate,
                publicMoney, expected.warTimeMillis(), securityEnabled,
                mercenaries, expected.snapshotVersion());
    }

    private static RepositoryCastleSnapshot warEndReplacement(
            RepositoryCastleSnapshot expected, long nextWarTimeMillis) {
        return new RepositoryCastleSnapshot(true, expected.castleId(), 10, 0,
                nextWarTimeMillis, expected.securityEnabled(),
                expected.mercenaries(), expected.snapshotVersion());
    }

    private static RuntimeInventory adenaInventory(long objectId, int quantity) {
        RuntimeInventory inventory = new RuntimeInventory();
        long admitted = inventory.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.ADENA),
                new RuntimeInventory.PersistentItem(objectId, quantity, 0, 0,
                        true, RuntimeInventory.BLESSING_NORMAL, true, false,
                        false, null, 0, 0, false, false));
        assertEquals(objectId, admitted);
        return inventory;
    }

    private static RuntimeInventory adjustedAdena(
            RuntimeInventory before, long treasuryCredit) {
        RuntimeInventory after = before.copy();
        boolean adjusted = treasuryCredit > 0
                ? after.removeCatalog(InventoryCatalog.ADENA, treasuryCredit)
                : after.add(InventoryCatalog.require(InventoryCatalog.ADENA),
                        Math.negateExact(treasuryCredit));
        assertTrue(adjusted);
        return after;
    }

    private static void cleanup(AccountRepository repository, Jdbc jdbc,
            String account, String character) throws Exception {
        for (CharacterSummary row : repository.characters(account)) {
            if (!row.name().equals(character)) continue;
            RepositoryClanSnapshot clan = repository.clan(row.objectId());
            if (clan.present() && clan.leaderObjectId() == row.objectId()) {
                assignCastle(jdbc, clan.clanId(), 0);
                repository.setClanDestroyPending(
                        clan.leaderObjectId(), clan.destroyPending(), false);
                repository.rollbackCreatedClan(clan.leaderObjectId(), clan.clanId());
            }
            repository.deleteLitoCharacter(account, character);
        }
        repository.deleteLitoAccount(account);
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
                        : "writable-state.properties").toAbsolutePath().toString());
        properties.setProperty("capture.directory",
                temporary.resolve("captures").toAbsolutePath().toString());
        return properties;
    }

    private static Path writeConfig(Path path, Properties properties) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary castle integration profile");
        }
        return path;
    }

    private record Jdbc(String url, String login, String password) {}
    private record NativeCastleBefore(
            int taxRate, long publicMoney, long warTimeMillis,
            int rewardItemId, int rewardCount) {
        int normalizedTaxRate() {
            return taxRate >= 10 && taxRate <= 50 ? taxRate : 10;
        }
    }
    private record CastleMetadataBefore(boolean present, boolean securityEnabled,
                                        GameStateStore.MercenaryRoster mercenaries,
                                        long snapshotVersion) {}

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

    private static NativeCastleBefore nativeCastle(Jdbc jdbc, int castleId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT tax_rate,public_money,item_id,count,"
                             + "COALESCE(UNIX_TIMESTAMP(war_time)*1000,0) "
                             + "FROM `server_城堡` WHERE castle_id=?")) {
            query.setInt(1, castleId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return new NativeCastleBefore(
                        row.getInt(1), row.getLong(2), row.getLong(5),
                        row.getInt(3), row.getInt(4));
            }
        }
    }

    private static long allocateInventoryObjectId(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT GREATEST("
                             + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                             + "COALESCE((SELECT MAX(id) FROM character_warehouse),0),"
                             + "COALESCE((SELECT MAX(id) FROM clan_warehouse),0),"
                             + "COALESCE((SELECT MAX(id) FROM character_elf_warehouse),0)) + 1")) {
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                long allocated = row.getLong(1);
                assertTrue(allocated > 0 && allocated <= Integer.MAX_VALUE);
                return allocated;
            }
        }
    }

    private static void insertAdena(
            Jdbc jdbc, long characterId, long objectId, int count) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO character_items "
                             + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,"
                             + "enchantlvl,is_id,durability,charge_count,bless) "
                             + "VALUES (?,?,?,?,?,0,0,0,1,0,0,1)")) {
            insert.setLong(1, objectId);
            insert.setInt(2, Math.toIntExact(InventoryCatalog.ADENA));
            insert.setLong(3, characterId);
            insert.setString(4, "Adena");
            insert.setInt(5, count);
            assertEquals(1, insert.executeUpdate());
        }
    }

    private static long itemCount(Jdbc jdbc, long characterId, long objectId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT count FROM character_items WHERE char_id=? AND id=?")) {
            query.setLong(1, characterId);
            query.setLong(2, objectId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static long count(Jdbc jdbc, String sql, Object value) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setObject(1, value);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static void deleteInventoryItem(
            Jdbc jdbc, long characterId, long objectId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM character_items_time WHERE itemr_obj_id=?")) {
                delete.setLong(1, objectId);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM lito_item_origin WHERE item_obj_id=?")) {
                delete.setLong(1, objectId);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM character_items WHERE char_id=? AND id=?")) {
                delete.setLong(1, characterId);
                delete.setLong(2, objectId);
                delete.executeUpdate();
            }
        }
    }

    private static CastleMetadataBefore castleMetadata(Jdbc jdbc, int castleId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT security_enabled,mercenary_0,mercenary_1,mercenary_2,"
                             + "mercenary_3,snapshot_version "
                             + "FROM lito_castle_metadata WHERE castle_id=?")) {
            query.setInt(1, castleId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return new CastleMetadataBefore(false, false,
                        new GameStateStore.MercenaryRoster(0, 0, 0, 0), 0);
                return new CastleMetadataBefore(true, row.getInt(1) != 0,
                        new GameStateStore.MercenaryRoster(
                                row.getInt(2), row.getInt(3), row.getInt(4), row.getInt(5)),
                        row.getLong(6));
            }
        }
    }

    private static void assignCastle(Jdbc jdbc, long clanId, int castleId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE clan_data SET hascastle=? WHERE clan_id=?")) {
            update.setInt(1, castleId);
            update.setLong(2, clanId);
            update.executeUpdate();
        }
    }

    private record PendingCastleReward(
            String account, int itemId, int count, int claimed) {}

    private static String castleRewardEventKey(RepositoryCastleSnapshot snapshot) {
        return "lito:castle-war:" + snapshot.castleId() + ":"
                + snapshot.warTimeMillis() / 1_000L;
    }

    private static void configureCastleReward(
            Jdbc jdbc, int castleId, int itemId, int count) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `server_城堡` SET item_id=?,count=? WHERE castle_id=?")) {
            update.setInt(1, itemId);
            update.setInt(2, count);
            update.setInt(3, castleId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static PendingCastleReward castleReward(Jdbc jdbc, String eventKey)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT `玩家帳號`,`發送物品編號`,`發送物品數量`,`領取狀態` "
                             + "FROM `server_紀錄_獎勵` WHERE lito_event_key=?")) {
            query.setString(1, eventKey);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                PendingCastleReward reward = new PendingCastleReward(
                        row.getString(1), row.getInt(2), row.getInt(3), row.getInt(4));
                assertFalse(row.next());
                return reward;
            }
        }
    }

    private static long castleRewardCount(Jdbc jdbc, String eventKey)
            throws Exception {
        return count(jdbc, "SELECT COUNT(*) FROM `server_紀錄_獎勵` "
                + "WHERE lito_event_key=?", eventKey);
    }

    private static long castleSecuritySettlementCount(
            Jdbc jdbc, int castleId) throws Exception {
        return count(jdbc, "SELECT COUNT(*) FROM lito_castle_security_settlement "
                + "WHERE castle_id=?", castleId);
    }

    private static void deleteCastleSecuritySettlements(
            Jdbc jdbc, int castleId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM lito_castle_security_settlement WHERE castle_id=?")) {
            delete.setInt(1, castleId);
            delete.executeUpdate();
        }
    }

    private static void configureSecurityCastle(
            Jdbc jdbc, int castleId, long publicMoney, boolean enabled,
            GameStateStore.MercenaryRoster mercenaries, long snapshotVersion)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE `server_城堡` SET public_money=? WHERE castle_id=?")) {
                update.setLong(1, publicMoney);
                update.setInt(2, castleId);
                assertEquals(1, update.executeUpdate());
            }
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
                update.setInt(1, castleId);
                update.setBoolean(2, enabled);
                update.setInt(3, mercenaries.type0());
                update.setInt(4, mercenaries.type1());
                update.setInt(5, mercenaries.type2());
                update.setInt(6, mercenaries.type3());
                update.setLong(7, snapshotVersion);
                assertTrue(update.executeUpdate() >= 1);
            }
        }
    }

    private static long itemCatalogCount(
            Jdbc jdbc, long characterId, int itemId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COALESCE(SUM(count),0) FROM character_items "
                             + "WHERE char_id=? AND item_id=?")) {
            query.setLong(1, characterId);
            query.setInt(2, itemId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static long warehouseCatalogCount(
            Jdbc jdbc, String account, int itemId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COALESCE(SUM(count),0) FROM character_warehouse "
                             + "WHERE account_name=? AND item_id=?")) {
            query.setString(1, account);
            query.setInt(2, itemId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static void insertPendingReward(
            Jdbc jdbc, String account, int itemId, int count, String eventKey)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO `server_紀錄_獎勵` "
                             + "(`玩家帳號`,`發送物品編號`,`發送物品數量`,"
                             + "`強化值`,`領取狀態`,lito_event_key) "
                             + "VALUES (?,?,?,0,0,?)")) {
            insert.setString(1, account);
            insert.setInt(2, itemId);
            insert.setInt(3, count);
            insert.setString(4, eventKey);
            assertEquals(1, insert.executeUpdate());
        }
    }

    private static void deleteTestWarehouseRewards(Jdbc jdbc, String account)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE w FROM character_warehouse w JOIN lito_warehouse_item_origin o "
                            + "ON o.item_obj_id=w.id WHERE o.scope='PERSONAL' "
                            + "AND o.owner_key=?")) {
                delete.setString(1, account);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM lito_warehouse_item_origin "
                            + "WHERE scope='PERSONAL' AND owner_key=?")) {
                delete.setString(1, account);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM `server_紀錄_獎勵` WHERE `玩家帳號`=? "
                            + "AND lito_event_key LIKE 'lito:test-reward-warehouse:%'")) {
                delete.setString(1, account);
                delete.executeUpdate();
            }
        }
    }

    private static void deleteTestCastleRewards(Jdbc jdbc, String account)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM `server_紀錄_獎勵` WHERE `玩家帳號`=? "
                             + "AND lito_event_key LIKE 'lito:castle-war:8:%'")) {
            delete.setString(1, account);
            delete.executeUpdate();
        }
    }

    private static void restoreCastle(Jdbc jdbc, int castleId,
            NativeCastleBefore nativeBefore, CastleMetadataBefore metadataBefore)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE `server_城堡` SET tax_rate=?,public_money=?,"
                            + "war_time=FROM_UNIXTIME(?),item_id=?,count=? "
                            + "WHERE castle_id=?")) {
                update.setInt(1, nativeBefore.taxRate());
                update.setLong(2, nativeBefore.publicMoney());
                update.setLong(3, nativeBefore.warTimeMillis() / 1_000L);
                update.setInt(4, nativeBefore.rewardItemId());
                update.setInt(5, nativeBefore.rewardCount());
                update.setInt(6, castleId);
                assertEquals(1, update.executeUpdate());
            }
            if (!metadataBefore.present()) {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_castle_metadata WHERE castle_id=?")) {
                    delete.setInt(1, castleId);
                    delete.executeUpdate();
                }
            } else {
                GameStateStore.MercenaryRoster mercenaries = metadataBefore.mercenaries();
                try (PreparedStatement update = connection.prepareStatement(
                        "INSERT INTO lito_castle_metadata "
                                + "(castle_id,security_enabled,mercenary_0,mercenary_1,"
                                + "mercenary_2,mercenary_3,snapshot_version) VALUES (?,?,?,?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "security_enabled=VALUES(security_enabled),"
                                + "mercenary_0=VALUES(mercenary_0),"
                                + "mercenary_1=VALUES(mercenary_1),"
                                + "mercenary_2=VALUES(mercenary_2),"
                                + "mercenary_3=VALUES(mercenary_3),"
                                + "snapshot_version=VALUES(snapshot_version)")) {
                    update.setInt(1, castleId);
                    update.setBoolean(2, metadataBefore.securityEnabled());
                    update.setInt(3, mercenaries.type0());
                    update.setInt(4, mercenaries.type1());
                    update.setInt(5, mercenaries.type2());
                    update.setInt(6, mercenaries.type3());
                    update.setLong(7, metadataBefore.snapshotVersion());
                    update.executeUpdate();
                }
            }
        }
    }
}
