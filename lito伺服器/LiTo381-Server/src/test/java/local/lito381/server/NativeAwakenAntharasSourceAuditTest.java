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

/** Cross-source proof for native Dragon Knight skill185 Awaken: Antharas. */
final class NativeAwakenAntharasSourceAuditTest {
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

    @Test void selectedDatabasesPinResourcesAbilitiesImmunityAndDragonActions()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (185, 0, '覺醒：安塔瑞斯',"
                + " 24, 0, 0, 1, 49157, 1, 0, 0, 'none', 0, 0, 0, 0, 0,"
                + " 0, 0, 2, 0, 0, 0, 0, 1, '$5924', 19, 6975, 0, 0, 0, 0);",
                skillRow(primary, 185));
        assertEquals("INSERT INTO `skills` VALUES (185, '覺醒：安塔瑞斯', 24,"
                + " 0, 0, 1, 49157, 1, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 1, '$5924', 19, 6975, 0, 0, 0, 0);",
                skillRow(secondary, 185));
        assertTrue(primary.contains("INSERT INTO `skills_相消免疫技能` VALUES"
                + " (185, '覺醒：安塔瑞斯');"));
        assertTrue(primary.contains("INSERT INTO `skills_buff_ability` VALUES"
                + " (185, '覺醒：安塔瑞斯', 'AC', -12);"));
        assertTrue(primary.contains("INSERT INTO `skills_buff_ability` VALUES"
                + " (185, '覺醒：安塔瑞斯', 'HP', 127);"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (185, '覺醒：安塔瑞斯', NULL, NULL);"));
        assertTrue(primary.contains("INSERT INTO `etcitem` VALUES (NULL, 49157,"
                + " '刻印的骨頭碎片', '0', '$6072', 'material', 'normal', 'bone',"
                + " 1000, 3025, 6927, 3605, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 127);"));
        for (int action : new int[] {0, 1, 18, 19, 4, 5, 11, 12, 24, 25, 50, 51, 62, 63}) {
            assertTrue(primary.contains("INSERT INTO `spr_action` VALUES (9362, "
                    + action + ","), "missing dragon action " + action);
        }
    }

    @Test void selectedLiToUsesSelectorOnlyProducerAndZeroClientClock()
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
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.AWAKEN_ANTHARAS - 1) * 2L));
    }

    @Test void retainedRuntimePinsTogglePolyDrainWhitelistAndResourceOrdering()
            throws Exception {
        String packet = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int awaken = packet.indexOf("case 185:// 覺醒：安塔瑞斯");
        String targetFamily = packet.substring(awaken,
                packet.indexOf("break;", awaken));
        assertTrue(targetFamily.contains("targetId = readD();"));
        assertFalse(targetFamily.contains("targetX = readH();"));

        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/AWAKEN_ANTHARAS.java"));
        assertTrue(mode.contains("getAwakeSkillId() == L1SkillId.AWAKEN_ANTHARAS"));
        assertTrue(mode.contains("L1SkillStop.stopSkill"));
        assertTrue(mode.contains("setAwakeSkillId(L1SkillId.AWAKEN_ANTHARAS)"));
        assertTrue(mode.contains("L1BuffUtil.doPoly(srcpc)"));
        assertTrue(mode.contains("startMpReductionByAwake()"));
        assertTrue(mode.contains("setAwakeSkillId(0)"));
        assertTrue(mode.contains("L1BuffUtil.undoPoly(pc)"));

        String timer = read(RETAINED.resolve(
                "src/com/lineage/server/timecontroller/skill/Skill_Awake_Timer.java"));
        assertTrue(timer.contains("L1PcInstance.INTERVAL_BY_AWAKE"));
        assertTrue(timer.contains("tgpc.debitCurrentMpUpTo(8)"));
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"));
        assertTrue(pc.contains("INTERVAL_BY_AWAKE = 4"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("pc.sendPackets(new S_ServerMessage(1385))"));
        for (String allowed : new String[] {"MAGMA_BREATH", "SHOCK_SKIN",
                "FREEZING_BREATH", "THUNDER_GRAB"}) {
            assertTrue(use.contains("this._skillId != " + allowed));
        }
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("AWAKEN_ANTHARAS"));
        assertTrue(use.indexOf("commitMaterialAndAcquireCastLocks()")
                < use.indexOf("runSkill();", use.indexOf("commitMaterialAndAcquireCastLocks()")));
        assertTrue(use.indexOf("runSkill();", use.indexOf("commitMaterialAndAcquireCastLocks()"))
                < use.indexOf("useConsume();"));
    }

    @Test void implementationKeepsAllIndefiniteAwakeningSubsystemsConnected()
            throws Exception {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(185);
        assertEquals(23, spell.row());
        assertEquals(0, spell.column());
        assertEquals(-12, spell.armorClassModifier());
        assertEquals(127, spell.maximumHpBonus());
        assertEquals(9_362, spell.dragonGfx());
        assertEquals(8, spell.mpDrainAmount());
        assertEquals(4, spell.mpDrainIntervalSeconds());
        assertTrue(spell.counterMagicBypass());
        assertTrue(spell.cancellationImmune());

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useAwakenSpell(payload, worldAwaken)",
                "useAwakenSpell(payload, awakenSpell)",
                "Protocol.selectorOnlySpellRequest(payload)",
                "nextInventory.removeCatalog(",
                "awakenStatus.toggle(",
                "polymorphStatus.clear();",
                "broadcastAwakenAppearance(",
                "serverAwakeningMpDrain(nowNanos)",
                "awakenStatus.pollMpDrainTicks(nowNanos)",
                "awakeningAdjustedMaximumHp(",
                "awakenStatus.snapshot().armorClassModifier()",
                "S_MESSAGE_CODE awakening-spell-rejected=1385"}) {
            assertTrue(session.contains(fragment), "missing implementation: " + fragment);
        }
        int cancellation = session.indexOf("private void useCancellationSpell(");
        int nextMethod = session.indexOf("\n    private void ", cancellation + 10);
        String cancellationMethod = session.substring(cancellation, nextMethod);
        assertFalse(cancellationMethod.contains("awakenStatus.clear"));
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
