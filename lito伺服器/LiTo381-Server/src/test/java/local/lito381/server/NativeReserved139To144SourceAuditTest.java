package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected-client no-producer slots139..144. */
final class NativeReserved139To144SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinSixExactNoneRows() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        for (int skillId = 139; skillId <= 144; skillId++) {
            int nativeId = 1 << (skillId - 137);
            assertTrue(primary.contains("INSERT INTO `skills` VALUES ("
                    + skillId + ", 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0,"
                    + " 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                    + nativeId + ", '', 19, 0, 0, 0, 0, 0);"));
            assertTrue(secondary.contains("INSERT INTO `skills` VALUES ("
                    + skillId + ", 'none', 0, 0, 0, 0, 0, 0, 0, 0,"
                    + " 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                    + nativeId + ", '', 19, 0, 0, 0, 0, 0);"));
            for (String sql : new String[] {primary, secondary}) {
                assertTrue(sql.contains("INSERT INTO `skills_item` VALUES ("
                        + skillId + ", 'none', NULL, NULL);"));
            }
        }
    }

    @Test void selectedDisassemblyGroupsAllSixIntoNoProducerBranch()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY);
        assertTrue(disassembly.contains("jumptable 004CB66E default case, cases"
                + " 23,80,82-85,87,88,91,93-95,111,120-127,134,135,138-143,171"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
