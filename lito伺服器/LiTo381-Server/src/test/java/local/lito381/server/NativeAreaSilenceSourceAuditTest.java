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

/** Cross-source proof for native Elf skill161 Area of Silence. */
final class NativeAreaSilenceSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_Chat.java"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_ChatGlobal.java"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
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

    @Test void databasesPinExactRowsAndSelectedControlTables() throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (161, 0,"
                + " '封印禁地', 21, 0, 40, 0, 40319, 8, 9000, 16, 'none',"
                + " 3, 0, 0, 0, 50, 30, 0, 1, 0, 0, -1, 1, 1, '', 19,"
                + " 2241, 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (161,"
                + " '封印禁地', 21, 0, 40, 0, 40319, 8, 9000, 16, 'none',"
                + " 3, 0, 0, 0, 50, 30, 0, 1, 0, 0, -1, 1, 1, '', 19,"
                + " 2241, 0, 0, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_負面技能控制` VALUES"
                + " (161, '封印禁地', 55, 45, 35, 0, 0, 0.3, 0, 0, 1,"
                + " 0.05, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (161, '封印禁地', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_魔屏無效技能` VALUES"
                + " (161, '封印禁地');"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (161, '封印禁地', NULL, NULL);"));
        assertFalse(primary.contains("INSERT INTO `skills_buff_ability` VALUES"
                + " (161,"));
    }

    @Test void selectedClientPinsSelectorOnlyProducerAndNineSecondClock()
            throws Exception {
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        assertTrue(asm.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,"
                + "96-98,100,101,103-105,108-110,113,114,116,118,119,128,"
                + "129,133,136,137,145,146,149,153-155,158,160,161,163,"
                + "168,170"));
        assertTrue(asm.contains("loc_4CBBD4:"));
        assertTrue(asm.contains("push    14h\npush    offset off_5028CC"));
        assertTrue(asm.contains("off_5028CC dd offset byte_636363"));
        assertEquals(9_000, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.AREA_OF_SILENCE - 1) * 2L));
    }

    @Test void retainedConsumersBlockMagicAndBothChatPaths() throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String say = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_Chat.java"));
        String global = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_ChatGlobal.java"));
        assertTrue(use.contains("pc.hasSkillEffect(AREA_OF_SILENCE)"));
        assertTrue(say.contains("pc.hasSkillEffect(AREA_OF_SILENCE)"));
        assertTrue(global.contains("pc.hasSkillEffect(AREA_OF_SILENCE)"));
        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("case AREA_OF_SILENCE:// 封印禁地"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("AREA_OF_SILENCE"));
    }

    @Test void implementationRoutesBothPhasesAndAllActorKinds() throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.areaSilenceSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(session.contains("target.timedStatus.activateAreaSilence("));
        assertTrue(session.contains("areaSilenceQuestMonster("));
        assertTrue(session.contains("areaSilenceTimeAttackActor("));
        assertTrue(session.contains("areaSilenceClassicWorldMonster("));
        assertTrue(session.contains("itemConsumeCount()"));
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
}
