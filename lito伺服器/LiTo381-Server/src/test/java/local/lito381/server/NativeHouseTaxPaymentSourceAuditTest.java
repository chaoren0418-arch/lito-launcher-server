package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class NativeHouseTaxPaymentSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_NPCAction.java"));
    }

    @Test
    void retainedKeeperPaymentIsAtomicAndRoutedToTheSelectedDatabase()
            throws Exception {
        String repository = LocalResources.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "Accounts.java"), StandardCharsets.UTF_8);
        String session = LocalResources.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "ClientSession.java"), StandardCharsets.UTF_8);
        String retained = LocalResources.readString(Path.of("..", "1-伺服器", "src", "com",
                "lineage", "server", "clientpackets", "C_NPCAction.java"),
                StandardCharsets.UTF_8);

        assertTrue(retained.contains("case \"pay\": // 繳納稅金"));
        assertTrue(retained.contains("case \"payfee\": // 繳納稅金"));
        assertTrue(retained.contains("ConfigAlt.HOUSE_TAX_ADENA"));
        assertTrue(retained.contains("cal.add(Calendar.DATE, ConfigAlt.HOUSE_TAX_INTERVAL)"));

        assertTrue(repository.contains(
                "character_items WRITE, character_items_time WRITE"));
        assertTrue(repository.contains(
                "`server_盟屋_拍賣` READ, `server_盟屋_紀錄` WRITE"));
        assertTrue(repository.contains(
                "house-tax payment must debit the exact tax"));
        assertTrue(repository.contains(
                "AND tax_deadline=FROM_UNIXTIME(?)"));
        assertTrue(session.contains("HOUSE_TAX_ADENA = 2_000"));
        assertTrue(session.contains("HOUSE_TAX_INTERVAL_DAYS = 10"));
        assertTrue(session.contains("action().equalsIgnoreCase(\"payfee\")"));
        assertTrue(session.contains("persistHouseTaxPayment("));

        var housekeepers = ClassicWorldNpcCatalog.all(4).stream()
                .filter(ClassicWorldNpcCatalog.StaticNpc::housekeeper)
                .toList();
        assertTrue(!housekeepers.isEmpty());
        assertTrue(housekeepers.stream().allMatch(npc ->
                !npc.initialHtml(0).isBlank()
                        && npc.npcId() >= 50_501 && npc.npcId() <= 50_699));
    }
}
