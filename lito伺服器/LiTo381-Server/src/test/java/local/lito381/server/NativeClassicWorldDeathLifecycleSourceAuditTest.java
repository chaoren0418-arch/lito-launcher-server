package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Selected retained-server proof for the complete death-policy authority. */
final class NativeClassicWorldDeathLifecycleSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path ROOT = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SETTINGS = ROOT.resolve("config/charsettings.properties");
    private static final Path SQL = ROOT.resolve("db/380_db.sql");
    private static final Path PC = ROOT.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");
    private static final Path INVENTORY = ROOT.resolve(
            "src/com/lineage/server/model/L1PcInventory.java");
    private static final Path CONTROL = ROOT.resolve(
            "src/com/lineage/server/datatables/DeathPenaltyItemControlTable.java");
    private static final Path DEATH_SAFE = ROOT.resolve(
            "src/com/lineage/data/item_etcitem/add/DeathSafe.java");

    @Test void selectedFilesRemainTheReviewedDeathPolicyAuthority() throws Exception {
        assertEquals("1E324A0A35B30DBF6BE4BB07487C5AC8ABA92D609D9F2C45DA11130BCFEC6A99",
                sha256(SETTINGS));
        assertEquals(ClassicWorldDeathPenaltyCatalog.SOURCE_SHA256, sha256(SQL));
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(PC));
        assertEquals("30EBCE722C976B775AFFFF8285C2D382E178E9376E62F8C0FEDF13E266E5F1FF",
                sha256(INVENTORY));
        assertEquals("B1274A2206B7CFAD705F7D7EC506B23BD5CEA453EE3064A5627CD31B807F8748",
                sha256(CONTROL));
        assertEquals("D01C9BC46702055EE8166CD6DEEEF460A5034286BAA7BF0DF383A6D27F18B7D6",
                sha256(DEATH_SAFE));
    }

    @Test void sixItemAndSkillBandsMatchSelectedCharSettings() throws Exception {
        String settings = Files.readString(SETTINGS);
        assertEquals(ClassicWorldDeathPenaltyCatalog.itemRules(),
                rules(settings, "DeathPenaltyItem"));
        assertEquals(ClassicWorldDeathPenaltyCatalog.skillRules(),
                rules(settings, "DeathPenaltySkill"));
    }

    @Test void allFortyEightSelectedControlRowsMatchRuntime() throws Exception {
        String sql = Files.readString(SQL);
        Matcher rows = Pattern.compile(
                "^INSERT INTO `系統_死亡噴出控制` VALUES \\((\\d+), '.*?', (\\d+)\\);$",
                Pattern.MULTILINE).matcher(sql);
        Map<Long, ClassicWorldDeathPenaltyCatalog.Control> selected =
                new LinkedHashMap<>();
        while (rows.find()) {
            long itemId = Long.parseLong(rows.group(1));
            int code = Integer.parseInt(rows.group(2));
            selected.put(itemId, java.util.Arrays.stream(
                            ClassicWorldDeathPenaltyCatalog.Control.values())
                    .filter(value -> value.code() == code).findFirst().orElseThrow());
        }
        assertEquals(48, selected.size());
        assertEquals(selected, ClassicWorldDeathPenaltyCatalog.controls());
    }

    @Test void selectedDeathSafeRowsAndNativeOrderingRemainExplicit() throws Exception {
        String sql = Files.readString(SQL);
        for (var safe : ClassicWorldDeathPenaltyCatalog.deathSafeItems().values()) {
            String classToken = "'add.DeathSafe " + safe.categoryMask() + " "
                    + safe.consume() + "'";
            assertTrue(sql.contains(", " + safe.itemId() + ",")
                    && sql.contains(classToken));
        }
        assertTrue(sql.contains(
                "INSERT INTO `道具_物品時間限制` VALUES (350354, 168, '全能保護符')"));

        String source = Files.readString(PC).replace("\r\n", "\n");
        int action = source.indexOf("new S_DoActionGFX(targetobjid, ActionCodes.ACTION_Die)");
        int item = source.indexOf("this.lostRate();", action);
        int skill = source.indexOf("this.lostSkillRate();", item);
        int experience = source.indexOf("this.expRate();", skill);
        assertTrue(action >= 0 && item > action && skill > item && experience > skill,
                "retained action -> item -> skill -> EXP order drifted");
        String inventory = Files.readString(INVENTORY);
        assertTrue(inventory.contains("final int rnd = random.nextInt(_items.size());"));
        assertTrue(inventory.contains("if (!isEligibleForDeathDrop(penaltyItem))"));
        assertTrue(inventory.contains("DROP_ALWAYS_VANISH"));
    }

    private static List<ClassicWorldDeathPenaltyCatalog.Rule> rules(
            String settings, String prefix) {
        List<ClassicWorldDeathPenaltyCatalog.Rule> result = new ArrayList<>();
        Pattern line = Pattern.compile("(?m)^" + prefix + "\\d+\\s*=\\s*"
                + "(-?\\d+),(-?\\d+),(\\d+),(\\d+)\\s*$");
        Matcher matcher = line.matcher(settings);
        while (matcher.find()) {
            result.add(new ClassicWorldDeathPenaltyCatalog.Rule(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4))));
        }
        return List.copyOf(result);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
