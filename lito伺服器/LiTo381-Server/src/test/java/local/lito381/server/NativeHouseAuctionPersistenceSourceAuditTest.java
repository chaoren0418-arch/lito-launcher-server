package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the selected-source clan-house bid flow to the routed SQL gate. */
final class NativeHouseAuctionPersistenceSourceAuditTest {
    @Test
    void nativeBidSemanticsAndSelectedDatabaseGateRemainConnected() throws Exception {
        String source = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_Amount.java"),
                StandardCharsets.UTF_8);
        int boards = source.indexOf("AuctionBoardTable.get().getAuctionBoardTableList()");
        int price = source.indexOf("board.setPrice(finalAmount)", boards);
        int bidder = source.indexOf("board.setBidder(finalPcName)", price);
        int bidderId = source.indexOf("board.setBidderId(finalPc.getId())", bidder);
        int update = source.indexOf("AuctionBoardTable.get().updateAuctionBoard(board)", bidderId);
        assertTrue(boards >= 0 && price > boards && bidder > price
                && bidderId > bidder && update > bidderId);

        String accounts = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        int legacy = accounts.indexOf("final class LegacyMySqlRepository");
        assertTrue(legacy >= 0);
        String repository = accounts.substring(legacy);
        assertTrue(repository.contains("houseAuctions()"));
        assertTrue(repository.contains("persistHouseBid("));
        assertTrue(repository.contains("persistHouseAuctionSettlement("));
        assertTrue(repository.contains("`server_盟屋_拍賣` WRITE"));
        assertTrue(repository.contains("character_items WRITE"));
        assertTrue(repository.contains("lito_house_auction_metadata WRITE"));
        assertTrue(repository.contains("UPDATE `server_盟屋_拍賣` SET price=?,bidder=?,bidder_id=?"));
        assertTrue(repository.contains("house previous-bidder refund changed"));
        assertTrue(repository.contains("compensateHouseBid("));
        assertTrue(repository.contains("compensateHouseSettlement("));
        assertTrue(repository.contains("`server_盟屋_紀錄` WRITE"));
        assertTrue(repository.contains("clan_data WRITE"));
        assertTrue(repository.contains("old_owner_id"));
        assertTrue(repository.contains("expiredHouseTaxes("));
        assertTrue(repository.contains("persistHouseTaxExpiry("));
        assertTrue(repository.contains("house tax auction insert changed"));
        assertTrue(repository.contains("compensateHouseTaxExpiry("));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("selectedSourceAccounts().persistHouseBid("));
        assertTrue(session.contains(
                "RepositoryHouseAuctionSnapshot snapshot = selectedSourceAccounts()"));
        assertTrue(session.contains(".houseAuctions();"));
        assertTrue(session.contains("receiveHouseAuctionRefund("));

        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        assertTrue(hub.contains("houseAuctionTickSafely"));
        assertTrue(hub.contains("persistHouseAuctionSettlement(auction, now, 10)"));
        assertTrue(hub.contains("houseTaxTickSafely"));
        assertTrue(hub.contains("persistHouseTaxExpiry("));

        String taxTimer = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/timecontroller/server/"
                        + "ServerHouseTaxTimer.java"), StandardCharsets.UTF_8);
        int expired = taxTimer.indexOf("house.getTaxDeadline().before(getRealTime())");
        int auction = taxTimer.indexOf("AuctionBoardTable.get().insertAuctionBoard(board)",
                expired);
        int onSale = taxTimer.indexOf("house.setOnSale(true)", auction);
        int basement = taxTimer.indexOf("house.setPurchaseBasement(true)", onSale);
        int clearOwner = taxTimer.indexOf("clan.setHouseId(0)", basement);
        assertTrue(expired >= 0 && auction > expired && onSale > auction
                && basement > onSale && clearOwner > basement);
    }
}
