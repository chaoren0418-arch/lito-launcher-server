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

/** Cross-source proof for native Dragon Knight skill183 Guard Brake. */
final class NativeGuardBrakeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
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

    @Test void databasesPinExactTargetCostDurationProbabilityAndPlayerAc()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        String schema = primary.substring(primary.indexOf("CREATE TABLE `skills`"),
                primary.indexOf("-- Records of skills"));
        assertTrue(schema.indexOf("`mpConsume`") < schema.indexOf("`hpConsume`"));
        assertEquals("INSERT INTO `skills` VALUES (183, 0, '護衛毀滅', 23, 6,"
                + " 0, 25, 0, 0, 1000, 60, 'buff', 3, 0, 0, 0, 0, 26, 0,"
                + " 1, 0, 3, 0, 0, 64, '$5922', 19, 6508, 0, 0, 0, 280);",
                skillRow(primary, 183));
        assertEquals("INSERT INTO `skills` VALUES (183, '護衛毀滅', 23, 6,"
                + " 0, 25, 0, 0, 0, 60, 'buff', 3, 0, 0, 0, 0, 26, 0, 1,"
                + " 0, 3, 0, 0, 64, '$5922', 19, 6508, 0, 0, 0, 280);",
                skillRow(secondary, 183));
        BattleSpellCatalog.GuardBrakeSpell spell =
                BattleSpellCatalog.guardBrakeSpell(BattleSpellCatalog.GUARD_BRAKE);
        assertEquals(0, spell.mpCost());
        assertEquals(25, spell.hpCost());
        assertTrue(primary.contains("INSERT INTO `skills_負面技能控制` VALUES"
                + " (183, '護衛毀滅', 55, 45, 35, 0, 0, 0.35, 0, 0, 1,"
                + " 0.05, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (183, '護衛毀滅', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_buff_ability` VALUES"
                + " (183, '護衛毀滅', 'AC', 15);"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (183, '護衛毀滅', NULL, NULL);"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (183,"));
    }

    @Test void selectedLiToUsesPost175SelectorOnlyProducerAnd1800Clock()
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
                0x635774L + (long) (BattleSpellCatalog.GUARD_BRAKE - 1) * 2L));
    }

    @Test void retainedRuntimePinsConfigPriorityCounterCancellationAndNoPersistence()
            throws Exception {
        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains(
                "SkillsProbabilityTable.get().getConfig(skillId)"));
        assertTrue(magic.contains(
                "probability += _pc.getInt() * config.getBonusInt();"));
        assertTrue(magic.contains("probability += _pc.getMagicHit();"));
        assertTrue(magic.contains(
                "probability -= targetMr * config.getMrMultiplier();"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertFalse(exceptCounter.contains("GUARD_BRAKE"));
        assertTrue(use.contains("this._skillId == GUARD_BRAKE"));
        assertTrue(use.contains("pc.updateBuffStatus()"));
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case GUARD_BRAKE"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        String buffArray = buffs.substring(buffs.indexOf("_buffSkill = {"),
                buffs.indexOf("};", buffs.indexOf("_buffSkill = {")));
        assertFalse(buffArray.contains("GUARD_BRAKE"));
    }

    @Test void implementationRoutesBothPhasesAndAllStatusLifecycleEdges()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.guardBrakeSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(session.contains("lastCombatTargetObjectId"));
        assertTrue(session.contains("resolveGuardBrakeTarget("));
        assertTrue(session.contains("guardBrakeTargetStillEligible("));
        assertTrue(session.contains("guardBrakeStatus.activate("));
        assertTrue(session.contains("guardBrakeStatus.cancel()"));
        assertTrue(session.contains("guardBrakeStatus.expire("));
        assertTrue(session.contains("guardBrakeStatus.armorClass("));
        assertTrue(session.contains("consumeCounterMagic(castNow)"));
        assertTrue(session.contains("npc-mechanical-ac=0"));
        assertFalse(session.contains("saveTimedGuardBrake"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
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
