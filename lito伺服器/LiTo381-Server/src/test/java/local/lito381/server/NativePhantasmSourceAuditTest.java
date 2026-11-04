package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Cross-source proof for selected Illusionist skill212 Phantasm. */
final class NativePhantasmSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
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

    @Test void selectedRowsPinCostProbabilityBookAndNoReagent() throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (212, 0, '幻想', 27, 3, 30,"
                + " 25, 0, 0, 3000, 5, 'buff', 3, 0, 0, 0, 30, 0, 0, 4,"
                + " 0, 3, 0, 0, 8, '$5946', 19, 6530, 0, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (212, '幻想', 27, 3, 30, 25,"
                + " 0, 0, 0, 5, 'buff', 3, 0, 0, 0, 30, 0, 0, 4, 0, 3,"
                + " 0, 0, 8, '$5946', 19, 6530, 0, 0, 0, 0);",
                skillRow(secondary));
        String probability = "INSERT INTO `skills_負面技能控制` VALUES"
                + " (212, '幻想-石化耐性(固定30%+隨機)', 35, 35, 35, 0, 0,"
                + " 0, 2, 0, 1, 0, 0, 0);";
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains(probability));
            assertTrue(sql.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (212, '幻想', 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (212, '幻想', NULL, NULL);"));
            assertTrue(sql.contains("49128, '記憶水晶(幻想)',"
                    + " 'skill.Skill_Illusionist', '$5692'"));
        }
    }

    @Test void selectedLiToEmits141a03AndUses640MsClock() throws Exception {
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
        assertEquals(640, readPeWordAtVa(UNPACKED, 0x63591AL));
        assertEquals("14 1A 03", Protocol.hex(
                new byte[] {0x14, 0x1A, 0x03}));
    }

    @Test void retainedModeInstallsFogAndProbabilityOrderingWakesFirst()
            throws Exception {
        Path mode = RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/PHANTASM.java");
        Path use = RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java");
        assertEquals("CFE5318620D880900261C67DAB01808086A09D03AC7D28BA6BDB627DBDEB2467",
                sha256(mode));
        String modeText = read(mode);
        assertTrue(modeText.contains("cha.setSkillEffect(L1SkillId.FOG_OF_SLEEPING,"));
        assertTrue(modeText.contains("S_Paralysis.TYPE_SLEEP, true"));
        String useText = read(use);
        int probability = useText.indexOf("isSuccess = magic.calcProbabilityMagic(this._skillId)");
        int wake = useText.indexOf("cha.removeSkillEffect(FOG_OF_SLEEPING)", probability);
        int counter = useText.indexOf("this.isUseCounterMagic(cha)", wake);
        assertTrue(probability >= 0 && wake > probability && counter > wake);
        assertTrue(useText.substring(0, useText.indexOf("default:",
                useText.indexOf("pc.hasSkillEffect(ABSOLUTE_BARRIER)")))
                .contains("case FOG_OF_SLEEPING:"));
    }

    @Test void implementationRoutesBattleAndWorldWithoutSkill212Persistence()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String population = read(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"));
        assertTrue(catalog.contains("PHANTASM = 212"));
        assertTrue(catalog.contains("clientCastClockMillis != 640"));
        assertTrue(session.contains("usePhantasmSpell(payload, worldPhantasm)"));
        assertTrue(session.contains("usePhantasmSpell(payload, phantasmSpell)"));
        assertTrue(session.contains("targetPlayer.cancelSleep(\"probability-spell-\""));
        assertTrue(session.contains("targetPlayer.dragonEyeStatus.snapshot(targetNow)"));
        assertTrue(session.contains(".stoneResistance())));"));
        assertTrue(session.contains("timedStatus.consumeCounterMagic(castNow)"));
        assertTrue(session.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.SLEEP_APPLY)"));
        assertTrue(population.contains("damagePoisonStatus.sleepActive(nowNanos)"));
        assertTrue(population.contains("damagePoisonStatus.cancelSleep()"));
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (212,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
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
