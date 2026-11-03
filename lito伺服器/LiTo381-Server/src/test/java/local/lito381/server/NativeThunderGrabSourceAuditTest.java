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
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Dragon Knight skill192 Thunder Grab. */
final class NativeThunderGrabSourceAuditTest {
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

    @Test void selectedRowsPinHpDamageProbabilityBossCounterAndBook()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (192, 0, '奪命之雷', 24, 7,"
                + " 0, 45, 0, 0, 4000, 2, 'attack', 3, 80, 10, 2, 10, 0,"
                + " 8, 64, 0, 8, 0, 0, 128, '$5931', 18, 6512, 0, 0, 0, 0);",
                skillRow(primary, 192));
        assertEquals("INSERT INTO `skills` VALUES (192, '奪命之雷', 24, 7,"
                + " 0, 45, 0, 0, 0, 2, 'attack', 3, 80, 10, 2, 10, 0, 8,"
                + " 64, 0, 8, 0, 0, 128, '$5931', 18, 6512, 0, 0, 0, 0);",
                skillRow(secondary, 192));
        String probability = "INSERT INTO `skills_負面技能控制` VALUES"
                + " (192, '奪命之雷', 50, 30, 15, 0, 0, 0, 6, 0, 0, 0, 0, 0);";
        assertTrue(primary.contains(probability));
        assertTrue(secondary.contains(probability));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (192, '奪命之雷', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_魔屏無效技能` VALUES"
                + " (192, '奪命之雷');"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (192, '奪命之雷', NULL, NULL);"));
        assertTrue(primary.contains("49113, '龍騎士書版(奪命之雷)',"
                + " 'skill.Skill_DragonKnigh', '$5711'"));
    }

    @Test void selectedLiToEmitsOnly140x1707AndUses1800msClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(use.contains("ja      def_4CB66E"));
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(1_800, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.THUNDER_GRAB - 1) * 2L));
        assertEquals("14 17 07", Protocol.hex(new byte[] {0x14, 0x17, 0x07}));
    }

    @Test void retainedRuntimePinsTargetTailMutationModesAndTeleportGate()
            throws Exception {
        String packet = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int skill = packet.indexOf("case 192:// 奪命之雷 5150");
        String targetFamily = packet.substring(skill,
                packet.indexOf("break;", skill));
        assertTrue(targetFamily.contains("targetId = readD();"));
        assertTrue(targetFamily.contains("targetX = readH();"));
        assertTrue(targetFamily.contains("targetY = readH();"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("magic.calcProbabilityMagic(THUNDER_GRAB)"));
        assertTrue(use.contains("ThreadLocalRandom.current().nextInt(randomSec + 1)"));
        assertTrue(use.contains("gateOnDamageAcceptance"));
        assertTrue(use.contains("applyThunderGrabTargetEffect(target, thunderGrabMillis)"));
        assertTrue(use.contains("cha.setSkillEffect(L1SkillId.MOVE_STOP, millis)"));
        assertTrue(use.contains("new S_Paralysis(S_Paralysis.TYPE_BIND, true)"));
        String paralysis = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Paralysis.java"));
        assertTrue(paralysis.contains("this.writeC(0x18)"));
        assertTrue(paralysis.contains("this.writeC(0x19)"));
        String config = read(RETAINED.resolve(
                "config/其他控制端/4-職業_技能設定表.properties"));
        assertTrue(config.contains("SLAY_BREAK_TIMER = 2,2"));
        assertTrue(config.contains("SLAY_BREAK_NOT_TELEPORT = true"));
    }

    @Test void implementationRestoresTargetAndPinsAllRuntimeBranches()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("useSelectorOnlyTargetedAttackSpell("));
        assertTrue(session.contains("resolveSelectorOnlyPrimary("));
        assertTrue(session.contains("\"attack\""));
        assertTrue(session.contains("thunderGrab.counterMagicBypass()"));
        assertTrue(session.contains("applyPlayerThunderGrab("));
        assertTrue(session.contains("rollNpcThunderGrabDuration("));
        assertTrue(session.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.BIND_APPLY)"));
        assertTrue(session.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.BIND_RELEASE)"));
        assertTrue(session.contains("thunderGrabQuestMonster("));
        assertTrue(session.contains("thunderGrabTimeAttackActor("));
        assertTrue(session.contains("thunderGrabClassicWorldMonster("));

        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleTimedStatus.java"));
        assertTrue(status.contains("activateThunderGrab("));
        assertTrue(status.contains("expireThunderGrab("));
        assertTrue(status.contains("cancelThunderGrab("));
        assertTrue(status.contains("|| thunderGrabActive(nowNanos)"));
        assertTrue(status.contains("boolean thunderGrab,"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
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
        assertTrue(Files.isRegularFile(path), "missing source: " + path);
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
