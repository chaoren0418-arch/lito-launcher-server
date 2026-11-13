package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PcActionRewardSqlFirstSourceAuditTest {
    // #138 B-land (主控裁定第 4 點): two text audits were removed from this file.
    //  - helperCapabilityPrecedesWorldCompanionRoutingAndClaimIsSqlFirst -> replaced by the
    //    behavior tests in PcActionRewardBehaviorTest (helper opens pc_action in WORLD; inventory
    //    is published only after a SUCCESS SQL claim; refusal keeps inventory and pending row).
    //  - rewardRowAndDestinationShareOneLockedMutationDomain -> the SQL lock group / exact-row
    //    update is exercised by LitoMySqlCastleIntegrationTest (read-only refusal, SUCCESS,
    //    NOT_FOUND replay and personal-warehouse fallback against a real MySQL route).
    @Test
    void newLitoCharactersReceiveOnlyTheNativeHelperTemplate() throws Exception {
        String repository = LocalResources.readString(Path.of("src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8)
                // #138 B-land: LegacyMySqlRepository now lives in its own file.
                + LocalResources.readString(Path.of(
                        "src/main/java/local/lito381/server/LegacyMySqlRepository.java"),
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
        LocalResources.assumeFiles(
                Path.of("../LiTo381-Launcher/src/LiToCompatibilityResources.cs"),
                Path.of("../LiTo381-開發/resources/pc-action-html/pc_action-c.html"),
                Path.of("../LiTo381-開發/resources/pc-action-html/send_s_0-c.html"),
                Path.of("../LiTo381-開發/resources/pc-action-html/send_s_2-c.html"),
                Path.of("../LiTo381-開發/resources/pc-action-html/send_s_3-c.html"),
                Path.of("../LiTo381-開發/resources/pc-action-html/send_s_4-c.html"),
                Path.of("../LiTo381-開發/tools/TextPakGameplayOverlay/Program.cs"));
        Path workspace = Path.of("..");
        Path assets = workspace.resolve("LiTo381-開發/resources/pc-action-html");
        String launcher = LocalResources.readString(
                workspace.resolve("LiTo381-Launcher/src/LiToCompatibilityResources.cs"),
                StandardCharsets.UTF_8);
        String overlayTool = LocalResources.readString(
                workspace.resolve("LiTo381-開發/tools/TextPakGameplayOverlay/Program.cs"),
                StandardCharsets.UTF_8);
        assertTrue(launcher.contains("天堂小幫手領獎 UI"));
        assertTrue(launcher.contains(
                "412AF669F06DF26CB1590E80E458D448ED7A304F3917911D94CF3BC2DA676F77"));
        assertTrue(launcher.contains(
                "2A7CD3DD3838FBCBC6E93F99F26B5CFA04685787671562F447C2D61D81E939C1"));
        assertTrue(overlayTool.contains("Expected 6 gameplay HTML assets"));
        assertTrue(overlayTool.contains("archive.IsSorted"));

        String root = LocalResources.readString(assets.resolve("pc_action-c.html"),
                StandardCharsets.UTF_8);
        assertTrue(root.contains("action=\"long_send\""));
        assertTrue(LocalResources.readString(assets.resolve("send_s_0-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_0\""));
        for (int page = 1; page <= 4; page++) {
            String html = LocalResources.readString(assets.resolve("send_s_" + page + "-c.html"),
                    StandardCharsets.UTF_8);
            assertTrue(html.contains("action=\"send_1\""));
            for (int argument = 0; argument < 10; argument++) {
                assertTrue(html.contains("<var src=\"#" + argument + "\">"));
            }
        }
        assertTrue(LocalResources.readString(assets.resolve("send_s_2-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_up\""));
        assertTrue(LocalResources.readString(assets.resolve("send_s_2-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_dn\""));
        assertTrue(LocalResources.readString(assets.resolve("send_s_3-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_dn\""));
        assertTrue(LocalResources.readString(assets.resolve("send_s_4-c.html"),
                StandardCharsets.UTF_8).contains("action=\"send_up\""));
    }
}
