package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Dragon Knight skill193 Horror of Death. */
final class NativeHorrorOfDeathSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillUse.java"));
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
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedRowsPinResourcesProbabilityBossBookAndWardBehavior()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (193, 0, '驚悚死神', 25, 0,"
                + " 0, 50, 0, 0, 1000, 60, 'buff', 3, 0, 0, 0, 0, 26, 0,"
                + " 1, 0, 3, 0, 0, 1, '$5932', 19, 6588, 0, 0, 0, 280);",
                skillRow(primary, 193));
        assertEquals("INSERT INTO `skills` VALUES (193, '驚悚死神', 25, 0,"
                + " 0, 50, 0, 0, 0, 60, 'buff', 3, 0, 0, 0, 0, 26, 0, 1,"
                + " 0, 3, 0, 0, 1, '$5932', 19, 6588, 0, 0, 0, 280);",
                skillRow(secondary, 193));
        String probability = "INSERT INTO `skills_負面技能控制` VALUES"
                + " (193, '驚悚死神', 55, 45, 35, 0, 0, 0.35, 0, 0, 1,"
                + " 0.05, 0, 0);";
        assertTrue(primary.contains(probability));
        assertTrue(secondary.contains(probability));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (193, '驚悚死神', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (193, '驚悚死神', NULL, NULL);"));
        assertTrue(primary.contains("49114, '龍騎士書版(驚悚死神)',"
                + " 'skill.Skill_DragonKnigh', '$5712'"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (193,"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_相消無效技能` VALUES (193,"));
    }

    @Test void selectedClientEmitsOnly141800AndUsesZeroClock() throws Exception {
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
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.HORROR_OF_DEATH - 1) * 2L));
        assertEquals("14 18 00", Protocol.hex(
                new byte[] {0x14, 0x18, 0x00}));
    }

    @Test void retained381PinsTargetTailDivergenceAndPlayerStatPenalty()
            throws Exception {
        String packet = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int skill = packet.indexOf("case 193:// 驚悚死神 5150");
        int targetRead = packet.indexOf("targetId = readD();", skill);
        assertTrue(skill >= 0);
        assertTrue(targetRead > skill);
        String targetFamily = packet.substring(skill,
                packet.indexOf("break;", targetRead));
        assertTrue(targetFamily.contains("targetId = readD();"));

        String retained = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillUse.java"));
        int effect = retained.indexOf(
                "this._skillId == HORROR_OF_DEATH");
        String branch = retained.substring(effect, retained.indexOf("}",
                retained.indexOf("}", effect) + 1) + 1);
        assertTrue(branch.contains("pc.addStr(-5);"));
        assertTrue(branch.contains("pc.addInt(-5);"));
        String stop = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillStop.java"));
        int stopCase = stop.indexOf("case HORROR_OF_DEATH:");
        String stopBranch = stop.substring(stopCase,
                stop.indexOf("break;", stopCase));
        assertTrue(stopBranch.contains("pc.addStr(5);"));
        assertTrue(stopBranch.contains("pc.addInt(5);"));
    }

    @Test void implementationConnectsSelectorProbabilityStatsCancellationAndExpiry()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("HORROR_OF_DEATH = 193"));
        assertTrue(catalog.contains("\"horror-of-death\""));
        assertTrue(catalog.contains("return horrorOfDeath() ? -5 : 0"));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleTimedStatus.java"));
        assertTrue(status.contains("activateHorrorOfDeath("));
        assertTrue(status.contains("expireHorrorOfDeath("));
        assertTrue(status.contains("cancelHorrorOfDeath()"));
        assertTrue(status.contains("boolean horrorOfDeath,"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("spell.horrorOfDeath()"));
        assertTrue(session.contains("timedStatus.activateHorrorOfDeath("));
        assertTrue(session.contains("cancellation.timed().horrorOfDeath()"));
        assertTrue(session.contains("timedStatus.expireHorrorOfDeath("));
        assertTrue(session.contains("timedStatus.horrorOfDeathActive(nowNanos) ? -5"));
        assertTrue(session.contains("config.horrorOfDeathFixtureD100()"));
        assertTrue(session.contains("target.companion().activateHorrorOfDeath("));
        assertTrue(session.contains("hub.horrorOfDeathClassicWorldMonster("));
        assertTrue(session.contains("hub.horrorOfDeathQuestMonster("));
        assertTrue(session.contains("hub.horrorOfDeathTimeAttackActor("));
        String coverage = read(Path.of("tools/protocol_coverage.py"));
        assertTrue(coverage.contains(
                "skill193 Horror of Death is selected-client selector-only"
                        + " C_USE_SPELL 14 18 00"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }
}
