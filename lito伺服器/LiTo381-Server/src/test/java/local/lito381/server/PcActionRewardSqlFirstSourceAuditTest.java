package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PcActionRewardSqlFirstSourceAuditTest {
    @Test
    void helperCapabilityPrecedesWorldCompanionRoutingAndClaimIsSqlFirst()
            throws Exception {
        String session = Files.readString(Path.of("src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int shape = session.indexOf("handleShapeChangeAction(request, shapeChange)");
        int reward = session.indexOf("handlePendingRewardAction(request)", shape);
        int world = session.indexOf("if (phase == Phase.WORLD)", reward);
        assertTrue(shape >= 0 && reward > shape && world > reward);
        assertTrue(session.contains("definition.catalogId() == InventoryCatalog.PC_ACTION_HELPER"));
        assertTrue(session.contains("selectedSourceAccounts().claimPendingReward("));
        assertTrue(session.indexOf("claim.status() != RepositoryPendingRewardClaimStatus.SUCCESS")
                < session.indexOf("inventory = afterInventory"));
    }

    @Test
    void rewardRowAndDestinationShareOneLockedMutationDomain() throws Exception {
        String repository = Files.readString(Path.of("src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        int method = repository.indexOf("claimPendingReward(",
                repository.indexOf("final class LegacyMySqlRepository"));
        int lock = repository.indexOf("LOCK TABLES characters READ", method);
        int inventory = repository.indexOf("character_items WRITE", lock);
        int warehouse = repository.indexOf("character_warehouse WRITE", lock);
        int pending = repository.indexOf("`server_紀錄_獎勵` WRITE", lock);
        int exactUpdate = repository.indexOf("AND `強化值`=? AND `領取狀態`=0", pending);
        assertTrue(method >= 0 && lock > method && inventory > lock
                && warehouse > inventory && pending > warehouse && exactUpdate > pending);
        assertTrue(repository.contains("compensatePendingRewardInventory"));
        assertTrue(repository.contains("characterIdentityMatches"));
    }

    @Test
    void newLitoCharactersReceiveOnlyTheNativeHelperTemplate() throws Exception {
        String repository = Files.readString(Path.of("src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        int create = repository.indexOf("createLitoCharacter(",
                repository.indexOf("final class LegacyMySqlRepository"));
        int helper = repository.indexOf("InventoryCatalog.PC_ACTION_HELPER", create);
        int insert = repository.indexOf("insertCharacterItem(connection, objectId, helperObjectId", helper);
        int origin = repository.indexOf("upsertCharacterItemOrigin(connection, helperObjectId", insert);
        assertTrue(create >= 0 && helper > create && insert > helper && origin > insert);
    }

    @Test
    void isolatedLitoRuntimePinsTheSanitizedPcActionRewardOverlay() throws Exception {
        Path workspace = Path.of("..");
        Path assets = workspace.resolve("LiTo381-開發/resources/pc-action-html");
        LocalResources.assumeExisting("pc-action html", assets);
        LocalResources.assumeExisting("LiToCompatibilityResources.cs",
                workspace.resolve("LiTo381-Launcher/src/LiToCompatibilityResources.cs"));
        LocalResources.assumeExisting("TextPakGameplayOverlay",
                workspace.resolve("LiTo381-開發/tools/TextPakGameplayOverlay/Program.cs"));
        String launcher = Files.readString(
                workspace.resolve("LiTo381-Launcher/src/LiToCompatibilityResources.cs"),
                StandardCharsets.UTF_8);
        String overlayTool = Files.readString(
                workspace.resolve("LiTo381-開發/tools/TextPakGameplayOverlay/Program.cs"),
                StandardCharsets.UTF_8);
        assertTrue(launcher.contains("天堂小幫手領獎 UI"));
        assertTrue(launcher.contains(
                "412AF669F06DF26CB1590E80E458D448ED7A304F3917911D94CF3BC2DA676F77"));
        assertTrue(launcher.contains(
                "2A7CD3DD3838FBCBC6E93F99F26B5CFA04685787671562F447C2D61D81E939C1"));
        assertTrue(overlayTool.contains("Expected 6 gameplay HTML assets"));
        assertTrue(overlayTool.contains("archive.IsSorted"));

        String root = Files.readString(assets.resolve("pc_action-c.html"),
                StandardCharsets.UTF_8);
        assertTrue(root.contains("action=\"long_send\""));
        assertTrue(Files.readString(assets.resolve("send_s_0-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_0\""));
        for (int page = 1; page <= 4; page++) {
            String html = Files.readString(assets.resolve("send_s_" + page + "-c.html"),
                    StandardCharsets.UTF_8);
            assertTrue(html.contains("action=\"send_1\""));
            for (int argument = 0; argument < 10; argument++) {
                assertTrue(html.contains("<var src=\"#" + argument + "\">"));
            }
        }
        assertTrue(Files.readString(assets.resolve("send_s_2-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_up\""));
        assertTrue(Files.readString(assets.resolve("send_s_2-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_dn\""));
        assertTrue(Files.readString(assets.resolve("send_s_3-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_dn\""));
        assertTrue(Files.readString(assets.resolve("send_s_4-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_up\""));
    }
}
