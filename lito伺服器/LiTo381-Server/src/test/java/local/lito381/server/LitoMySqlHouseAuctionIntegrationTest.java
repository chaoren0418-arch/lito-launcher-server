package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlHouseAuctionIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    private static final long HOUSE_ID = 262_145L;

    @Test
    void nativeAuctionBidDebitsAndRefundsAtomicallyPerDynamicRoute(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        AuctionBefore auctionBefore = auction(firstJdbc);
        MetadataBefore metadataBefore = metadata(firstJdbc);
        String firstAccount = "LtHouseBidA";
        String secondAccount = "LtHouseBidB";
        String firstName = "LitoHouseBidA";
        String secondName = "LitoHouseBidB";
        long firstId = 0;
        long secondId = 0;
        long firstAdenaId = 0;
        long secondAdenaId = 0;

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            cleanup(source, firstAccount, firstName);
            cleanup(source, secondAccount, secondName);
            routed.deleteLitoAccount(firstAccount);
            routed.deleteLitoAccount(secondAccount);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(firstAccount, "HouseBidA1"));
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(secondAccount, "HouseBidB1"));
            RepositoryCharacterMutation first = source.createLitoCharacter(
                    firstAccount, royal(firstName));
            RepositoryCharacterMutation second = source.createLitoCharacter(
                    secondAccount, royal(secondName));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, first.status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, second.status());
            firstId = first.character().objectId();
            secondId = second.character().objectId();
            firstAdenaId = allocateInventoryObjectId(firstJdbc);
            insertAdena(firstJdbc, firstId, firstAdenaId, 100);
            secondAdenaId = allocateInventoryObjectId(firstJdbc);
            insertAdena(firstJdbc, secondId, secondAdenaId, 500);
            installFixture(firstJdbc);

            GameStateStore.HouseAuction otherRoute = routed
                    .repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID);
            GameStateStore.HouseAuction initial = source.houseAuctions().byId(HOUSE_ID);
            assertTrue(initial != null);
            assertEquals(100, initial.price());
            assertEquals("", initial.bidder());

            RuntimeInventory firstBefore = adena(firstAdenaId, 100);
            RuntimeInventory firstAfter = firstBefore.copy();
            assertTrue(firstAfter.removeCatalog(InventoryCatalog.ADENA, 100));
            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryHouseBidMutation refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistHouseBid(firstId, initial, 100,
                                firstBefore, firstAfter,
                                Map.of(firstAdenaId, firstAdenaId));
                assertEquals(RepositoryHouseBidStatus.READ_ONLY, refused.status());
                assertEquals(100, adenaTotal(firstJdbc, firstId));
            }

            RepositoryHouseBidMutation accepted = source.persistHouseBid(
                    firstId, initial, 100, firstBefore, firstAfter,
                    Map.of(firstAdenaId, firstAdenaId));
            assertEquals(RepositoryHouseBidStatus.ACCEPTED, accepted.status());
            assertEquals(firstName, accepted.auction().bidder());
            assertEquals(0, adenaTotal(firstJdbc, firstId));

            RuntimeInventory secondBefore = adena(secondAdenaId, 500);
            RuntimeInventory secondAfter = secondBefore.copy();
            assertTrue(secondAfter.removeCatalog(InventoryCatalog.ADENA, 101));
            RepositoryHouseBidMutation stale = source.persistHouseBid(
                    secondId, initial, 101, secondBefore, secondAfter,
                    Map.of(secondAdenaId, secondAdenaId));
            assertEquals(RepositoryHouseBidStatus.STALE, stale.status());
            assertEquals(500, adenaTotal(firstJdbc, secondId));

            RepositoryHouseBidMutation replaced = source.persistHouseBid(
                    secondId, accepted.auction(), 101, secondBefore, secondAfter,
                    Map.of(secondAdenaId, secondAdenaId));
            assertEquals(RepositoryHouseBidStatus.ACCEPTED, replaced.status());
            assertEquals(secondName, replaced.auction().bidder());
            assertEquals(firstName, replaced.previousBidder());
            assertEquals(firstId, replaced.previousBidderObjectId());
            assertEquals(100, replaced.previousPrice());
            assertEquals(100, adenaTotal(firstJdbc, firstId));
            assertEquals(399, adenaTotal(firstJdbc, secondId));
            assertEquals(initial.revision() + 2, replaced.auction().revision());
            assertEquals(otherRoute, routed.repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID));
        } finally {
            restoreAuction(firstJdbc, auctionBefore, metadataBefore);
            try (AccountRepository routed = AccountRepository.open(
                    ServerConfig.load(writableConfig))) {
                AccountRepository source = routed
                        .repositoryForClassicSource("380 角色庫 1");
                cleanup(source, firstAccount, firstName);
                cleanup(source, secondAccount, secondName);
                routed.deleteLitoAccount(firstAccount);
                routed.deleteLitoAccount(secondAccount);
            }
        }
        assertEquals(auctionBefore, auction(firstJdbc));
        assertEquals(metadataBefore, metadata(firstJdbc));
    }

    @Test
    void expiredNativeAuctionTransfersHouseAndPaysSellerPerDynamicRoute(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("settlement-writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        AuctionBefore auctionBefore = auction(firstJdbc);
        MetadataBefore metadataBefore = metadata(firstJdbc);
        HouseBefore houseBefore = house(firstJdbc);
        String sellerAccount = "LtHouseSellA";
        String bidderAccount = "LtHouseWinB";
        String sellerName = "LitoHouseSellA";
        String bidderName = "LitoHouseWinB";
        long sellerId = 0;
        long bidderId = 0;
        long sellerClanId = 0;
        long bidderClanId = 0;

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            cleanup(source, sellerAccount, sellerName);
            cleanup(source, bidderAccount, bidderName);
            routed.deleteLitoAccount(sellerAccount);
            routed.deleteLitoAccount(bidderAccount);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(sellerAccount, "HouseSellA1"));
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(bidderAccount, "HouseWinB1"));
            RepositoryCharacterMutation seller = source.createLitoCharacter(
                    sellerAccount, royal(sellerName));
            RepositoryCharacterMutation bidder = source.createLitoCharacter(
                    bidderAccount, royal(bidderName));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, seller.status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, bidder.status());
            sellerId = seller.character().objectId();
            bidderId = bidder.character().objectId();
            sellerClanId = allocateClanId(firstJdbc);
            bidderClanId = sellerClanId + 1;
            insertClan(firstJdbc, sellerClanId, "LiTo賣屋盟", sellerId,
                    sellerName, HOUSE_ID);
            insertClan(firstJdbc, bidderClanId, "LiTo得標盟", bidderId,
                    bidderName, 0);
            installSettlementFixture(firstJdbc, sellerName, sellerId,
                    bidderName, bidderId);

            GameStateStore.HouseAuction otherRoute = routed
                    .repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID);
            GameStateStore.HouseAuction expired = source.houseAuctions().byId(HOUSE_ID);
            assertTrue(expired != null);
            assertEquals(sellerId, expired.oldOwnerObjectId());
            assertEquals(bidderId, expired.bidderObjectId());
            assertEquals(1_000, expired.price());

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("settlement-readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryHouseSettlement refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistHouseAuctionSettlement(expired,
                                System.currentTimeMillis() / 1_000L, 10);
                assertEquals(RepositoryHouseSettlementStatus.READ_ONLY,
                        refused.status());
                assertEquals(HOUSE_ID, clanHouse(firstJdbc, sellerClanId));
                assertEquals(0, adenaTotal(firstJdbc, sellerId));
            }

            RepositoryHouseSettlement settled = source
                    .persistHouseAuctionSettlement(expired,
                            System.currentTimeMillis() / 1_000L, 10);
            assertEquals(RepositoryHouseSettlementStatus.TRANSFERRED,
                    settled.status());
            assertEquals(sellerId, settled.oldOwnerObjectId());
            assertEquals(900, settled.payout());
            assertEquals(bidderId, settled.bidderObjectId());
            assertEquals(900, adenaTotal(firstJdbc, sellerId));
            assertEquals(0, clanHouse(firstJdbc, sellerClanId));
            assertEquals(HOUSE_ID, clanHouse(firstJdbc, bidderClanId));
            assertEquals(0, houseOnSale(firstJdbc));
            assertTrue(source.houseAuctions().byId(HOUSE_ID) == null);
            assertEquals(RepositoryHouseSettlementStatus.NOT_FOUND,
                    source.persistHouseAuctionSettlement(expired,
                            System.currentTimeMillis() / 1_000L, 10).status());
            assertEquals(otherRoute, routed.repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID));
        } finally {
            deleteClan(firstJdbc, bidderClanId);
            deleteClan(firstJdbc, sellerClanId);
            restoreHouse(firstJdbc, houseBefore);
            restoreAuction(firstJdbc, auctionBefore, metadataBefore);
            deleteCharacterAdena(firstJdbc, sellerId);
            try (AccountRepository routed = AccountRepository.open(
                    ServerConfig.load(writableConfig))) {
                AccountRepository source = routed
                        .repositoryForClassicSource("380 角色庫 1");
                cleanup(source, sellerAccount, sellerName);
                cleanup(source, bidderAccount, bidderName);
                routed.deleteLitoAccount(sellerAccount);
                routed.deleteLitoAccount(bidderAccount);
            }
        }
        assertEquals(auctionBefore, auction(firstJdbc));
        assertEquals(metadataBefore, metadata(firstJdbc));
        assertEquals(houseBefore, house(firstJdbc));
    }

    @Test
    void unownedAuctionWithoutBidderExtendsFiveDaysWithRevisionCas(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path config = writeConfig(
                temporary.resolve("extension-writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        AuctionBefore auctionBefore = auction(firstJdbc);
        MetadataBefore metadataBefore = metadata(firstJdbc);
        long now = System.currentTimeMillis() / 1_000L;
        try (Connection connection = DriverManager.getConnection(
                     firstJdbc.url(), firstJdbc.login(), firstJdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `server_盟屋_拍賣` SET deadline=FROM_UNIXTIME(?),"
                             + "old_owner='',old_owner_id=0,bidder='',bidder_id=0 "
                             + "WHERE house_id=?")) {
            update.setLong(1, now - 60);
            update.setLong(2, HOUSE_ID);
            assertEquals(1, update.executeUpdate());
        }
        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(config))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            GameStateStore.HouseAuction otherRoute = routed
                    .repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID);
            GameStateStore.HouseAuction expired = source.houseAuctions().byId(HOUSE_ID);
            assertTrue(expired != null);
            RepositoryHouseSettlement extended = source
                    .persistHouseAuctionSettlement(expired, now, 10);
            assertEquals(RepositoryHouseSettlementStatus.EXTENDED,
                    extended.status());
            assertTrue(extended.auction() != null);
            assertEquals(expired.revision() + 1,
                    extended.auction().revision());
            assertTrue(extended.auction().deadlineEpochSeconds()
                    >= now + 5L * 86_400L - 3_600L);
            assertTrue(extended.auction().deadlineEpochSeconds()
                    <= now + 5L * 86_400L);
            assertEquals(RepositoryHouseSettlementStatus.STALE,
                    source.persistHouseAuctionSettlement(expired, now, 10).status());
            assertEquals(otherRoute, routed.repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID));
        } finally {
            restoreAuction(firstJdbc, auctionBefore, metadataBefore);
        }
        assertEquals(auctionBefore, auction(firstJdbc));
        assertEquals(metadataBefore, metadata(firstJdbc));
    }

    @Test
    void expiredHouseTaxCreatesNativeAuctionAndClearsClanPerDynamicRoute(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("tax-writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        AuctionBefore auctionBefore = auction(firstJdbc);
        MetadataBefore metadataBefore = metadata(firstJdbc);
        HouseBefore houseBefore = house(firstJdbc);
        String ownerAccount = "LtHouseTaxA";
        String ownerName = "LitoHouseTaxA";
        long ownerId = 0;
        long ownerClanId = 0;
        long now = System.currentTimeMillis() / 1_000L;

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            cleanup(source, ownerAccount, ownerName);
            routed.deleteLitoAccount(ownerAccount);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(ownerAccount, "HouseTaxA1"));
            RepositoryCharacterMutation owner = source.createLitoCharacter(
                    ownerAccount, royal(ownerName));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, owner.status());
            ownerId = owner.character().objectId();
            ownerClanId = allocateClanId(firstJdbc);
            insertClan(firstJdbc, ownerClanId, "LiTo欠稅盟", ownerId,
                    ownerName, HOUSE_ID);
            installHouseTaxFixture(firstJdbc, now - 60);

            GameStateStore.HouseAuction otherRoute = routed
                    .repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID);
            RepositoryHouseTaxState expired = source.expiredHouseTaxes(now)
                    .houses().stream().filter(row -> row.houseId() == HOUSE_ID)
                    .findFirst().orElseThrow();
            assertEquals(0, expired.onSale());
            assertEquals(now - 60, expired.taxDeadlineEpochSeconds());

            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("tax-readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryHouseTaxMutation refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistHouseTaxExpiry(expired, now, 10);
                assertEquals(RepositoryHouseTaxStatus.READ_ONLY, refused.status());
                assertEquals(HOUSE_ID, clanHouse(firstJdbc, ownerClanId));
                assertTrue(auctionOrNull(firstJdbc) == null);
            }

            RepositoryHouseTaxState staleExpected = new RepositoryHouseTaxState(
                    expired.sourceId(), expired.houseId(), expired.name(),
                    expired.area(), expired.location(), expired.onSale(),
                    expired.purchaseBasement(),
                    expired.taxDeadlineEpochSeconds() - 1);
            assertEquals(RepositoryHouseTaxStatus.STALE,
                    source.persistHouseTaxExpiry(staleExpected, now, 10).status());

            RepositoryHouseTaxMutation auctioned = source.persistHouseTaxExpiry(
                    expired, now, 10);
            assertEquals(RepositoryHouseTaxStatus.AUCTIONED, auctioned.status());
            assertEquals(ownerClanId, auctioned.previousClanId());
            assertEquals(100_000, auctioned.auction().price());
            assertEquals("", auctioned.auction().oldOwner());
            assertEquals(0, auctioned.auction().oldOwnerObjectId());
            assertEquals("", auctioned.auction().bidder());
            assertEquals(0, auctioned.auction().bidderObjectId());
            assertEquals(1, auctioned.auction().revision());
            assertTrue(auctioned.auction().deadlineEpochSeconds()
                    >= now + 5L * 86_400L - 3_600L);
            assertTrue(auctioned.auction().deadlineEpochSeconds()
                    <= now + 5L * 86_400L);
            assertEquals(1, auctioned.house().onSale());
            assertEquals(1, auctioned.house().purchaseBasement());
            assertTrue(auctioned.house().taxDeadlineEpochSeconds()
                    >= now + 15L * 86_400L - 3_600L);
            assertTrue(auctioned.house().taxDeadlineEpochSeconds()
                    <= now + 15L * 86_400L);
            assertEquals(0, clanHouse(firstJdbc, ownerClanId));
            assertEquals(RepositoryHouseTaxStatus.STALE,
                    source.persistHouseTaxExpiry(expired, now, 10).status());
            assertEquals(otherRoute, routed.repositoryForClassicSource("380 角色庫 2")
                    .houseAuctions().byId(HOUSE_ID));
        } finally {
            deleteClan(firstJdbc, ownerClanId);
            restoreHouse(firstJdbc, houseBefore);
            restoreAuction(firstJdbc, auctionBefore, metadataBefore);
            try (AccountRepository routed = AccountRepository.open(
                    ServerConfig.load(writableConfig))) {
                AccountRepository source = routed
                        .repositoryForClassicSource("380 角色庫 1");
                cleanup(source, ownerAccount, ownerName);
                routed.deleteLitoAccount(ownerAccount);
            }
        }
        assertEquals(auctionBefore, auction(firstJdbc));
        assertEquals(metadataBefore, metadata(firstJdbc));
        assertEquals(houseBefore, house(firstJdbc));
    }

    @Test
    void nativeHousekeeperPaymentDebitsAdenaAndRenewsDeadlineAtomically(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Properties writable = integrationProperties(temporary, false);
        Path writableConfig = writeConfig(
                temporary.resolve("tax-payment-writable.properties"), writable);
        Jdbc firstJdbc = jdbc(writable, "380_ac_1");
        Jdbc secondJdbc = jdbc(writable, "380_ac_2");
        AuctionBefore auctionBefore = auction(firstJdbc);
        MetadataBefore metadataBefore = metadata(firstJdbc);
        HouseBefore houseBefore = house(firstJdbc);
        HouseBefore otherRouteBefore = house(secondJdbc);
        String ownerAccount = "LtTaxPayOwner";
        String outsiderAccount = "LtTaxPayOther";
        String ownerName = "LitoTaxPayOwner";
        String outsiderName = "LitoTaxPayOther";
        long ownerId = 0;
        long outsiderId = 0;
        long ownerClanId = 0;
        long adenaId = 0;
        long now = System.currentTimeMillis() / 1_000L;
        int keeperNpcId = houseKeeper(firstJdbc);

        try (AccountRepository routed = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            AccountRepository source = routed.repositoryForClassicSource("380 角色庫 1");
            cleanup(source, ownerAccount, ownerName);
            cleanup(source, outsiderAccount, outsiderName);
            routed.deleteLitoAccount(ownerAccount);
            routed.deleteLitoAccount(outsiderAccount);
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(ownerAccount, "TaxPayOwner1"));
            assertEquals(AccountMutationResult.SUCCESS,
                    routed.createAccount(outsiderAccount, "TaxPayOther1"));
            RepositoryCharacterMutation owner = source.createLitoCharacter(
                    ownerAccount, royal(ownerName));
            RepositoryCharacterMutation outsider = source.createLitoCharacter(
                    outsiderAccount, royal(outsiderName));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, owner.status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, outsider.status());
            ownerId = owner.character().objectId();
            outsiderId = outsider.character().objectId();
            ownerClanId = allocateClanId(firstJdbc);
            insertClan(firstJdbc, ownerClanId, "LiTo繳稅盟", ownerId,
                    ownerName, HOUSE_ID);
            setCharacterClan(firstJdbc, ownerId, ownerClanId, "LiTo繳稅盟");
            adenaId = allocateInventoryObjectId(firstJdbc);
            insertAdena(firstJdbc, ownerId, adenaId, 5_000);
            installHouseTaxFixture(firstJdbc, now - 60);

            RepositoryHouseTaxQuote quote = source.houseTaxQuote(
                    ownerId, keeperNpcId);
            assertEquals(RepositoryHouseTaxQuoteStatus.MEMBER, quote.status());
            assertEquals(HOUSE_ID, quote.house().houseId());
            assertEquals(ownerClanId, quote.ownerClanId());
            assertEquals(RepositoryHouseTaxQuoteStatus.OCCUPIED,
                    source.houseTaxQuote(outsiderId, keeperNpcId).status());
            assertEquals(RepositoryHouseTaxQuoteStatus.NOT_FOUND,
                    source.houseTaxQuote(ownerId, Integer.MAX_VALUE).status());

            RuntimeInventory before = adena(adenaId, 5_000);
            RuntimeInventory after = before.copy();
            assertTrue(after.removeCatalog(InventoryCatalog.ADENA, 2_000));
            Properties readOnly = integrationProperties(temporary, true);
            Path readOnlyConfig = writeConfig(
                    temporary.resolve("tax-payment-readonly.properties"), readOnly);
            try (AccountRepository readOnlyRouted = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                RepositoryHouseTaxPayment refused = readOnlyRouted
                        .repositoryForClassicSource("380 角色庫 1")
                        .persistHouseTaxPayment(ownerId, quote, now, 10, 2_000,
                                before, after, Map.of(adenaId, adenaId));
                assertEquals(RepositoryHouseTaxPaymentStatus.READ_ONLY,
                        refused.status());
                assertEquals(5_000, adenaTotal(firstJdbc, ownerId));
            }

            installHouseTaxFixture(firstJdbc, now - 30);
            assertEquals(RepositoryHouseTaxPaymentStatus.STALE,
                    source.persistHouseTaxPayment(ownerId, quote, now, 10, 2_000,
                            before, after, Map.of(adenaId, adenaId)).status());
            assertEquals(5_000, adenaTotal(firstJdbc, ownerId));

            // Retained payFee semantics resets from the current hour; it does
            // not stack another interval on a far-future deadline.
            installHouseTaxFixture(firstJdbc, now + 20L * 86_400L);
            RepositoryHouseTaxQuote current = source.houseTaxQuote(
                    ownerId, keeperNpcId);
            RepositoryHouseTaxPayment paid = source.persistHouseTaxPayment(
                    ownerId, current, now, 10, 2_000,
                    before, after, Map.of(adenaId, adenaId));
            assertEquals(RepositoryHouseTaxPaymentStatus.SUCCESS, paid.status());
            assertEquals(3_000, adenaTotal(firstJdbc, ownerId));
            assertTrue(paid.quote().house().taxDeadlineEpochSeconds()
                    >= now + 10L * 86_400L - 3_600L);
            assertTrue(paid.quote().house().taxDeadlineEpochSeconds()
                    <= now + 10L * 86_400L);
            assertTrue(paid.quote().house().taxDeadlineEpochSeconds()
                    < current.house().taxDeadlineEpochSeconds());
            assertEquals(otherRouteBefore, house(secondJdbc));
        } finally {
            deleteClan(firstJdbc, ownerClanId);
            restoreHouse(firstJdbc, houseBefore);
            restoreAuction(firstJdbc, auctionBefore, metadataBefore);
            deleteCharacterAdena(firstJdbc, ownerId);
            try (AccountRepository routed = AccountRepository.open(
                    ServerConfig.load(writableConfig))) {
                AccountRepository source = routed
                        .repositoryForClassicSource("380 角色庫 1");
                cleanup(source, ownerAccount, ownerName);
                cleanup(source, outsiderAccount, outsiderName);
                routed.deleteLitoAccount(ownerAccount);
                routed.deleteLitoAccount(outsiderAccount);
            }
        }
        assertEquals(auctionBefore, auction(firstJdbc));
        assertEquals(metadataBefore, metadata(firstJdbc));
        assertEquals(houseBefore, house(firstJdbc));
        assertEquals(otherRouteBefore, house(secondJdbc));
    }

    private static Protocol.CustomCharacterRequest royal(String name) {
        return new Protocol.CustomCharacterRequest(
                name, 0, 1, List.of(18, 13, 15, 9, 12, 8), 0, 0);
    }

    private static RuntimeInventory adena(long objectId, int count) {
        RuntimeInventory inventory = new RuntimeInventory();
        assertEquals(objectId, inventory.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.ADENA),
                new RuntimeInventory.PersistentItem(objectId, count, 0, 0,
                        true, RuntimeInventory.BLESSING_NORMAL, true, false,
                        false, null, 0, 0, false, false)));
        return inventory;
    }

    private static void cleanup(AccountRepository source, String account, String name)
            throws Exception {
        for (CharacterSummary row : source.characters(account)) {
            if (row.name().equals(name)) source.deleteLitoCharacter(account, name);
        }
        source.deleteLitoAccount(account);
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
        properties.setProperty("state.file", temporary.resolve(
                readOnly ? "readonly-state.properties" : "writable-state.properties")
                .toAbsolutePath().toString());
        properties.setProperty("capture.directory",
                temporary.resolve("captures").toAbsolutePath().toString());
        return properties;
    }

    private static Path writeConfig(Path path, Properties properties) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary house integration profile");
        }
        return path;
    }

    private record Jdbc(String url, String login, String password) {}
    private record AuctionBefore(long houseId, String name, int area,
                                 Timestamp deadline, long price, String location,
                                 String oldOwner, long oldOwnerId,
                                 String bidder, long bidderId) {}
    private record MetadataBefore(boolean present, long revision) {}
    private record HouseBefore(int onSale, int purchaseBasement,
                               Timestamp taxDeadline) {}

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

    private static AuctionBefore auction(Jdbc jdbc) throws Exception {
        AuctionBefore result = auctionOrNull(jdbc);
        assertTrue(result != null);
        return result;
    }

    private static AuctionBefore auctionOrNull(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT house_id,house_name,house_area,deadline,price,location,"
                             + "old_owner,old_owner_id,bidder,bidder_id "
                             + "FROM `server_盟屋_拍賣` WHERE house_id=?")) {
            query.setLong(1, HOUSE_ID);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                return new AuctionBefore(row.getLong(1), row.getString(2),
                        row.getInt(3), row.getTimestamp(4), row.getLong(5),
                        row.getString(6), row.getString(7), row.getLong(8),
                        row.getString(9), row.getLong(10));
            }
        }
    }

    private static void installHouseTaxFixture(
            Jdbc jdbc, long taxDeadlineEpochSeconds) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM `server_盟屋_拍賣` WHERE house_id=?")) {
                delete.setLong(1, HOUSE_ID);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM lito_house_auction_metadata WHERE house_id=?")) {
                delete.setLong(1, HOUSE_ID);
                delete.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE `server_盟屋_紀錄` SET is_on_sale=0,"
                            + "is_purchase_basement=0,tax_deadline=FROM_UNIXTIME(?) "
                            + "WHERE house_id=?")) {
                update.setLong(1, taxDeadlineEpochSeconds);
                update.setLong(2, HOUSE_ID);
                assertEquals(1, update.executeUpdate());
            }
        }
    }

    private static MetadataBefore metadata(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT snapshot_version FROM lito_house_auction_metadata "
                             + "WHERE house_id=?")) {
            query.setLong(1, HOUSE_ID);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? new MetadataBefore(true, row.getLong(1))
                        : new MetadataBefore(false, 0);
            }
        }
    }

    private static void installFixture(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `server_盟屋_拍賣` SET deadline=FROM_UNIXTIME(?),"
                             + "price=100,bidder='',bidder_id=0 WHERE house_id=?")) {
            update.setLong(1, System.currentTimeMillis() / 1_000L + 3_600L);
            update.setLong(2, HOUSE_ID);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static void installSettlementFixture(
            Jdbc jdbc, String sellerName, long sellerId,
            String bidderName, long bidderId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE `server_盟屋_拍賣` SET deadline=FROM_UNIXTIME(?),"
                            + "price=1000,old_owner=?,old_owner_id=?,bidder=?,"
                            + "bidder_id=? WHERE house_id=?")) {
                update.setLong(1, System.currentTimeMillis() / 1_000L - 60);
                update.setString(2, sellerName);
                update.setLong(3, sellerId);
                update.setString(4, bidderName);
                update.setLong(5, bidderId);
                update.setLong(6, HOUSE_ID);
                assertEquals(1, update.executeUpdate());
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE `server_盟屋_紀錄` SET is_on_sale=1 WHERE house_id=?")) {
                update.setLong(1, HOUSE_ID);
                assertEquals(1, update.executeUpdate());
            }
        }
    }

    private static void restoreAuction(
            Jdbc jdbc, AuctionBefore before, MetadataBefore metadata) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbc.url(), jdbc.login(), jdbc.password())) {
            try (PreparedStatement update = connection.prepareStatement(
                    "INSERT INTO `server_盟屋_拍賣` "
                            + "(house_name,house_area,deadline,price,location,old_owner,"
                            + "old_owner_id,bidder,bidder_id,house_id) VALUES (?,?,?,?,?,?,?,?,?,?) "
                            + "ON DUPLICATE KEY UPDATE house_name=VALUES(house_name),"
                            + "house_area=VALUES(house_area),deadline=VALUES(deadline),"
                            + "price=VALUES(price),location=VALUES(location),"
                            + "old_owner=VALUES(old_owner),old_owner_id=VALUES(old_owner_id),"
                            + "bidder=VALUES(bidder),bidder_id=VALUES(bidder_id)")) {
                update.setString(1, before.name());
                update.setInt(2, before.area());
                update.setTimestamp(3, before.deadline());
                update.setLong(4, before.price());
                update.setString(5, before.location());
                update.setString(6, before.oldOwner());
                update.setLong(7, before.oldOwnerId());
                update.setString(8, before.bidder());
                update.setLong(9, before.bidderId());
                update.setLong(10, before.houseId());
                assertTrue(update.executeUpdate() >= 1);
            }
            if (!metadata.present()) {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM lito_house_auction_metadata WHERE house_id=?")) {
                    delete.setLong(1, HOUSE_ID);
                    delete.executeUpdate();
                }
            } else {
                try (PreparedStatement update = connection.prepareStatement(
                        "INSERT INTO lito_house_auction_metadata "
                                + "(house_id,snapshot_version) VALUES (?,?) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "snapshot_version=VALUES(snapshot_version)")) {
                    update.setLong(1, HOUSE_ID);
                    update.setLong(2, metadata.revision());
                    update.executeUpdate();
                }
            }
        }
    }

    private static HouseBefore house(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT is_on_sale,is_purchase_basement,tax_deadline "
                             + "FROM `server_盟屋_紀錄` WHERE house_id=?")) {
            query.setLong(1, HOUSE_ID);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return new HouseBefore(row.getInt(1), row.getInt(2),
                        row.getTimestamp(3));
            }
        }
    }

    private static void restoreHouse(Jdbc jdbc, HouseBefore before) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `server_盟屋_紀錄` SET is_on_sale=?,"
                             + "is_purchase_basement=?,tax_deadline=? WHERE house_id=?")) {
            update.setInt(1, before.onSale());
            update.setInt(2, before.purchaseBasement());
            update.setTimestamp(3, before.taxDeadline());
            update.setLong(4, HOUSE_ID);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static long allocateClanId(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COALESCE(MAX(clan_id),0)+10 FROM clan_data")) {
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static void insertClan(Jdbc jdbc, long clanId, String clanName,
                                   long leaderId, String leaderName, long houseId)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO clan_data (clan_id,clan_name,leader_id,leader_name,"
                             + "hascastle,hashouse,max_online_user,showEmblem) "
                             + "VALUES (?,?,?,?,0,?,0,0)")) {
            insert.setLong(1, clanId);
            insert.setString(2, clanName);
            insert.setLong(3, leaderId);
            insert.setString(4, leaderName);
            insert.setLong(5, houseId);
            assertEquals(1, insert.executeUpdate());
        }
    }

    private static void deleteClan(Jdbc jdbc, long clanId) throws Exception {
        if (clanId <= 0) return;
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM clan_data WHERE clan_id=?")) {
            delete.setLong(1, clanId);
            delete.executeUpdate();
        }
    }

    private static void setCharacterClan(
            Jdbc jdbc, long characterId, long clanId, String clanName)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE characters SET ClanID=?,Clanname=? WHERE objid=?")) {
            update.setLong(1, clanId);
            update.setString(2, clanName);
            update.setLong(3, characterId);
            assertEquals(1, update.executeUpdate());
        }
    }

    private static int houseKeeper(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT keeper_id FROM `server_盟屋_紀錄` WHERE house_id=?")) {
            query.setLong(1, HOUSE_ID);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getInt(1);
            }
        }
    }

    private static long clanHouse(Jdbc jdbc, long clanId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT hashouse FROM clan_data WHERE clan_id=?")) {
            query.setLong(1, clanId);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static int houseOnSale(Jdbc jdbc) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT is_on_sale FROM `server_盟屋_紀錄` WHERE house_id=?")) {
            query.setLong(1, HOUSE_ID);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getInt(1);
            }
        }
    }

    private static void deleteCharacterAdena(Jdbc jdbc, long characterId)
            throws Exception {
        if (characterId <= 0) return;
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM character_items WHERE char_id=? AND item_id=?")) {
            delete.setLong(1, characterId);
            delete.setLong(2, InventoryCatalog.ADENA);
            delete.executeUpdate();
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
                             + "COALESCE((SELECT MAX(id) FROM character_elf_warehouse),0))+1")) {
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
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
            insert.setLong(2, InventoryCatalog.ADENA);
            insert.setLong(3, characterId);
            insert.setString(4, "Adena");
            insert.setInt(5, count);
            assertEquals(1, insert.executeUpdate());
        }
    }

    private static long adenaTotal(Jdbc jdbc, long characterId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     jdbc.url(), jdbc.login(), jdbc.password());
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COALESCE(SUM(count),0) FROM character_items "
                             + "WHERE char_id=? AND item_id=?")) {
            query.setLong(1, characterId);
            query.setLong(2, InventoryCatalog.ADENA);
            try (ResultSet row = query.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }
}
