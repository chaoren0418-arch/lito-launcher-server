package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Retained database/source proof for ordinary WORLD EXP death loss. */
final class NativeClassicWorldDeathPenaltySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/public-source-audit-0.7.660/tw_tpai/L1J-TW_3.80c/src/l1j/server/server/model/Instance/L1PcInstance.java"));
    }

    private static final Path PRIMARY_SQL = Path.of("..", "1-伺服器", "db",
            "380_db.sql").toAbsolutePath().normalize();
    private static final Path RETAINED_PC = Path.of("..", "LiTo381-開發",
            "public-source-audit-0.7.660", "tw_tpai", "L1J-TW_3.80c", "src",
            "l1j", "server", "server", "model", "Instance", "L1PcInstance.java")
            .toAbsolutePath().normalize();

    @Test void everySelectedMapAndExpDeathRateMatchesRuntimeAuthority()
            throws Exception {
        String sql = LocalResources.readString(PRIMARY_SQL);
        assertEquals(ClassicWorldDeathPenaltyCatalog.SOURCE_SHA256,
                sha256(PRIMARY_SQL));

        Matcher maps = Pattern.compile(
                "^INSERT INTO `mapids` VALUES \\((.*)\\);$", Pattern.MULTILINE)
                .matcher(sql);
        int mapCount = 0;
        int disabledCount = 0;
        while (maps.find()) {
            List<String> field = splitSqlValues(maps.group(1));
            assertEquals(23, field.size());
            int mapId = Integer.parseInt(field.get(0));
            boolean enabled = Integer.parseInt(field.get(14)) == 1;
            assertEquals(!enabled,
                    ClassicWorldDeathPenaltyCatalog.mapDisabled(mapId),
                    "mapids.penalty mismatch map=" + mapId);
            assertEquals(enabled,
                    ClassicWorldDeathPenaltyCatalog.allowsSelectedMap(mapId, true),
                    "selected death-penalty admission mismatch map=" + mapId);
            mapCount++;
            if (!enabled) disabledCount++;
        }
        assertEquals(ClassicWorldDeathPenaltyCatalog.MAP_COUNT, mapCount);
        assertEquals(ClassicWorldDeathPenaltyCatalog.DISABLED_MAP_COUNT,
                disabledCount);
        assertFalse(ClassicWorldDeathPenaltyCatalog.allowsSelectedMap(4, false));

        Matcher experience = Pattern.compile(
                "^INSERT INTO `exp` VALUES \\((.*)\\);$", Pattern.MULTILINE)
                .matcher(sql);
        int expCount = 0;
        while (experience.find()) {
            List<String> field = splitSqlValues(experience.group(1));
            assertEquals(7, field.size());
            int row = Integer.parseInt(field.get(0));
            ClassicExperienceCatalog.Row actual =
                    ClassicExperienceCatalog.rows().get(row);
            assertEquals(row, actual.sourceLevel());
            assertEquals(Long.parseLong(field.get(1)), actual.cumulativeExperience());
            assertEquals(Double.parseDouble(field.get(2)), actual.experiencePenalty());
            assertEquals(Double.parseDouble(field.get(3)), actual.deathPenaltyRate());
            expCount++;
        }
        assertEquals(ClassicExperienceCatalog.ROW_COUNT, expCount);
    }

    @Test void retainedDeathOrdersActionBeforeExactExperienceFormula()
            throws Exception {
        String source = LocalResources.readString(RETAINED_PC).replace("\r\n", "\n");
        int action = source.indexOf(
                "sendPackets(new S_DoActionGFX(targetobjid, ActionCodes.ACTION_Die));");
        int method = source.indexOf("public void deathPenalty() {");
        assertTrue(action >= 0 && method > action);
        String body = source.substring(method,
                source.indexOf("private int _originalEr", method));
        for (String anchor : List.of(
                "ExpTable.getNeedExpNextLevel(oldLevel)",
                "needExp * 0.1", "needExp * 0.09", "needExp * 0.08",
                "needExp * 0.07", "needExp * 0.06", "needExp * 0.05",
                "addExp(-exp)")) {
            assertTrue(body.contains(anchor), () -> "missing retained anchor " + anchor);
        }
    }

    private static List<String> splitSqlValues(String values) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < values.length(); index++) {
            char current = values.charAt(index);
            if (current == '\'') {
                if (quoted && index + 1 < values.length()
                        && values.charAt(index + 1) == '\'') {
                    field.append('\'');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        assertFalse(quoted);
        fields.add(field.toString().trim());
        return fields;
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(LocalResources.readAllBytes(path)));
    }
}
