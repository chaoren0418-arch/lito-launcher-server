package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pins the recovered LiTo Time Attack selection model and its evidence boundary. */
final class NativeLitoTimeAttackSourceBoundaryAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeExisting("LiTo381-開發 local file", Path.of("..", "LiTo381-開發", "analysis",
            "native-lito-time-attack-source-boundary-archive"));
    }

    private static final Path ARCHIVE = Path.of("..", "LiTo381-開發", "analysis",
            "native-lito-time-attack-source-boundary-archive")
            .toAbsolutePath().normalize();

    @Test
    void contemporaryFukuokaReportPinsFixedOrderAndPreStartUnknownPattern()
            throws Exception {
        Path report = ARCHIVE.resolve("4gamer-fukuoka-20050819.html");
        assertEquals("0B05DF26F8CC760360F757CD7D4912394B3F9E43ACB5326ABAC6697076DD5616",
                sha256(report));
        String html = Files.readString(report, Charset.forName("windows-31j"));
        assertTrue(html.contains("一定の順番で出現するモンスター"));
        assertTrue(html.contains("始まってみるまで主催者側にも分からない"));
        assertTrue(html.contains("最悪のパターン"));
        assertTrue(html.contains("時間切れ5秒前"));
        assertTrue(html.contains("エンシェントジャイアントとグレートミノタウルス"));
    }

    @Test
    void adenArenaDeathmatchTableIsExplicitlyExcludedFromLitoMap540()
            throws Exception {
        Path report = ARCHIVE.resolve("compendium-dm.html");
        assertEquals("FD25006442B557988E7BEF754B0D6B05F54AE5E77D089560447B5EB88AFC6999",
                sha256(report));
        String html = Files.readString(report, StandardCharsets.ISO_8859_1);
        assertTrue(html.contains("Aden Arena Deathmatch"));
        assertTrue(html.contains("Lineage Tournament (Korea only)"));
        assertTrue(html.contains("twice a day in Giran arena"));
        assertTrue(html.contains("The match has four rounds"));
        assertFalse(BattleTimeAttackEncounter.ordinaryWavePatternRecovered());
        assertEquals("Lineage-Compendium-Aden-Arena-Deathmatch-not-LiTo-map540",
                BattleTimeAttackEncounter.EXCLUDED_ADEN_ARENA_DM_SOURCE);
    }

    private static String sha256(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing retained source: " + path);
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
