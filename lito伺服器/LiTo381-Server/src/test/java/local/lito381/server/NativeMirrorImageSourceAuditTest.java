package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Illusionist skill201 Mirror Image. */
final class NativeMirrorImageSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackMode.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/MIRROR_IMAGE.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/model/L1AttackMode.java"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void noneRows196Through200PrecedeFirstRealIllusionistRow()
            throws Exception {
        String primary = read(PRIMARY);
        for (int skillId = 196; skillId <= 200; skillId++) {
            String row = skillRow(primary, skillId);
            assertTrue(row.contains("'none'"), "nonempty reserved row " + skillId);
            assertTrue(row.contains(", 0, 0, 0, 0);"),
                    "reserved row exposes resources " + skillId);
        }
        assertFalse(skillRow(primary, 201).contains("'none', 0, 0, 0, 0);"));
    }

    @Test void bothSelectedDatabasesPinMpReuseDurationActionAndBook()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (201, 0, '鏡像', 26, 0,"
                + " 10, 0, 0, 0, 10, 1200, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 1, '$5935', 19, 6813, 0, 0, 0, 0);",
                skillRow(primary, 201));
        assertEquals("INSERT INTO `skills` VALUES (201, '鏡像', 26, 0, 10,"
                + " 0, 0, 0, 0, 1200, 'none', 0, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 0, 0, 0, 1, '$5935', 19, 6813, 0, 0, 0, 0);",
                skillRow(secondary, 201));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (201, '鏡像', NULL, NULL);"));
            assertFalse(sql.contains("INSERT INTO `skills_相消免疫技能` VALUES"
                    + " (201,"));
            assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (201,"));
        }
        assertTrue(primary.contains("49117, '記憶水晶(鏡像)'"));
    }

    @Test void selectedLiToUsesSelectorOnlyProducerAnd1800msClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(1_800, readPeWordAtVa(UNPACKED, 0x635904L));
    }

    @Test void retainedRuntimePinsIndependentDodgeNoRefreshAndWardBypass()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/MIRROR_IMAGE.java"));
        for (String fragment : new String[] {
                "if (!srcpc.hasSkillEffect(MIRROR_IMAGE))",
                "srcpc.setSkillEffect(MIRROR_IMAGE, integer * 1000)",
                "srcpc.add_dodge(5)", "cha.add_dodge(-5)"}) {
            assertTrue(mode.contains(fragment), "missing retained mode: " + fragment);
        }
        String attack = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackMode.java"));
        assertTrue(attack.contains("double divisor = 8.0"));
        assertTrue(attack.contains("attackerDice -= hitReduction"));
        String original381 = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/L1AttackMode.java"));
        assertTrue(original381.contains("attackerDice -= character.get_dodge()"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("MIRROR_IMAGE"));
        String persistence = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(persistence.contains("import static com.lineage.server.model.skill."
                + "L1SkillId.MIRROR_IMAGE"));
    }

    @Test void implementationStacksBothDodgeLeasesAndDispatchesBothPhases()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useDodgeBuffSpell(payload, worldDodgeBuff)",
                "useDodgeBuffSpell(payload, dodgeBuffSpell)",
                "abilityStatus.activateMirrorImage(",
                "abilityStatus.mirrorImage(nowNanos).dodgeModifier()",
                "abilityStatus.uncannyDodge(nowNanos).dodgeModifier()",
                "abilityStatus.expireMirrorImage(nowNanos)",
                "Cancellation player cleared Mirror Image"}) {
            assertTrue(session.contains(fragment), "missing implementation: " + fragment);
        }
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleAbilityStatus.java"));
        assertTrue(status.contains("mirrorImageExpiresAtNanos"));
        assertTrue(status.contains("boolean mirrorImage"));
        assertTrue(status.contains("clearMirrorImage();"));
    }

    private static String skillRow(String sql, int skillId) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (" + skillId + ","))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int table = optional + Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
        for (int i = 0; i < sections; i++) {
            int section = table + i * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva
                    && rva + 2 <= sectionRva + Math.max(virtualSize, rawSize)) {
                return Short.toUnsignedInt(image.getShort(Math.toIntExact(
                        rawOffset + rva - sectionRva)));
            }
        }
        throw new IllegalArgumentException("unmapped PE VA");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
