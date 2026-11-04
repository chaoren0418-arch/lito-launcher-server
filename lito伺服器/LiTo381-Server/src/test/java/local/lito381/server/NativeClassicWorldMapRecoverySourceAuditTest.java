package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned retained authority for independent map environment ticks. */
final class NativeClassicWorldMapRecoverySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/MapRecoveryTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/pc/PcRegenTimer.java"));
    }

    private static final Path SERVER = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path TABLE = SERVER.resolve(
            "src/com/lineage/server/datatables/MapRecoveryTable.java");
    private static final Path EXECUTOR = SERVER.resolve(
            "src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java");
    private static final Path TIMER = SERVER.resolve(
            "src/com/lineage/server/timecontroller/pc/PcRegenTimer.java");
    private static final Path SQL = SERVER.resolve("db/380_db.sql");
    private static final Path CLIENT = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java")
            .toAbsolutePath().normalize();

    @Test
    void retainedSourcesRemainPinned() throws Exception {
        assertEquals("AC7649F0A3B0C28135365531BE46156CB24397BCA19E20DFE67C40DF3370373A",
                sha256(TABLE));
        assertEquals("4B69D91E865630CE7218A8DB7574989C7052CFF84EA98738CE570017E606FDB1",
                sha256(EXECUTOR));
        assertEquals("159799F3EB8367247C87953162E3E1F88C026DEB3A550D9503D08348323AC871",
                sha256(TIMER));
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(SQL));
    }

    @Test
    void retainedSqlAndRuntimeCatalogHaveTheSameTwentyNineRows()
            throws Exception {
        String sql = readLegacy(SQL);
        Pattern pattern = Pattern.compile(
                "(?m)^INSERT INTO `mapids_恢復區域` VALUES \\((\\d+), (-?\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (-?\\d+), (-?\\d+), (\\d+), ([01]), '([^']+)'\\);",
                Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(sql);
        int index = 0;
        while (matcher.find()) {
            ClassicWorldMapRecoveryCatalog.Rule rule =
                    ClassicWorldMapRecoveryCatalog.rules().get(index++);
            assertEquals(Integer.parseInt(matcher.group(1)), rule.id());
            assertEquals(Integer.parseInt(matcher.group(2)), rule.mapId());
            assertEquals(Integer.parseInt(matcher.group(3)), rule.minX());
            assertEquals(Integer.parseInt(matcher.group(4)), rule.minY());
            assertEquals(Integer.parseInt(matcher.group(5)), rule.maxX());
            assertEquals(Integer.parseInt(matcher.group(6)), rule.maxY());
            assertEquals(Integer.parseInt(matcher.group(7)), rule.hp());
            assertEquals(Integer.parseInt(matcher.group(8)), rule.mp());
            assertEquals(Integer.parseInt(matcher.group(9)),
                    rule.configuredIntervalSeconds());
            assertEquals("1".equals(matcher.group(10)), rule.disableNaturalMp());
            assertEquals(matcher.group(11), rule.note());
        }
        assertEquals(29, index);
    }

    @Test
    void independentCadenceFloorAndProtectionMatchRetainedConsumption()
            throws Exception {
        String executor = readLegacy(EXECUTOR);
        ordered(executor, "getMapRegenInterval", "getRecovery(pc)",
                "data.interval <= 0", "DEFAULT_INTERVAL_SEC");
        String timer = readLegacy(TIMER);
        ordered(timer, "mapChanged", "resetMapRegenTick()",
                "addMapRegenTick(1)", "getMapRegenInterval(tgpc)",
                "processMapRegen(tgpc)");
        ordered(executor, "int hpVal = data.hp", "hpVal < 0 && inLifeStream",
                "hpVal < 0 && pc.hasSkillEffect(STATUS_UNDERWATER_BREATH)",
                "hpDelta += hpVal", "hpDelta += 3",
                "adjustCurrentHp(hpDelta, 1)");
        ordered(executor, "private void regenMp", "isNaturalMpDisabled(tgpc)",
                "return;");

        String client = LocalResources.readString(CLIENT);
        assertTrue(client.contains("serverClassicWorldMapRecovery(nowNanos"));
        assertTrue(client.contains("activeMagicDollUnderwaterBreath()"));
        assertTrue(client.contains("timedStatus.breath(nowNanos)"));
        assertTrue(client.contains("Math.max(1L"));
        assertTrue(client.contains("disablesNaturalMp("));
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // Try the next retained source encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source " + path);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(LocalResources.readAllBytes(path)));
    }
}
