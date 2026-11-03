package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native LiTo selector 21/4, retained skill173 and runtime mapping. */
final class NativePolluteWaterSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path SELECTED_DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void bothResourceDumpsPinSkillProbabilityBossAndSourceSpecificCounterMagic()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);

        assertTrue(primary.contains("INSERT INTO `skills` VALUES (173, 0, '污濁之水',"
                + " 22, 4, 50, 0, 40319, 1, 1000, 192, 'buff', 3, 0, 0, 0,"
                + " 50, 50, 4, 1, 0, 3, 0, 0, 16, '', 19, 5830, 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (173, '污濁之水',"
                + " 22, 4, 50, 0, 40319, 1, 0, 192, 'buff', 3, 0, 0, 0,"
                + " 50, 50, 4, 1, 0, 3, 0, 0, 16, '', 19, 5830, 0, 0, 0, 280);"));
        String control = "INSERT INTO `skills_負面技能控制` VALUES (173, '污濁之水',"
                + " 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);";
        String boss = "INSERT INTO `skills_負面boss免疫` VALUES (173, '污濁之水', 0);";
        assertTrue(primary.contains(control));
        assertTrue(secondary.contains(control));
        assertTrue(primary.contains(boss));
        assertTrue(secondary.contains(boss));

        String primaryBypass = tableBlock(primary, "skills_魔屏無效技能");
        String secondaryBypass = tableBlock(secondary, "skills_魔屏無效技能");
        assertTrue(primaryBypass.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (173, '污濁之水');"));
        assertFalse(secondaryBypass.contains("VALUES (173,"));
        assertTrue(secondaryBypass.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (4, '光箭');"));
    }

    @Test
    void retainedServerPinsRequestFormulaHealingOrderAndLegacyIconSlot()
            throws Exception {
        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 173:// 污濁之水"));
        assertTrue(request.contains("targetId = readD();"));
        assertTrue(request.contains("checkLoc = true;"));

        String probability = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(probability.contains(
                "SkillsProbabilityTable.get().getConfig(skillId)"));
        assertTrue(probability.contains("probability = config.getProbHigh();"));
        assertTrue(probability.contains("probability = config.getProbEqual();"));
        assertTrue(probability.contains("probability = config.getProbLow();"));
        assertTrue(probability.contains(
                "probability += _pc.getInt() * config.getBonusInt();"));
        assertTrue(probability.contains("if (config.getCheckMagicHit() == 1)"));
        assertTrue(probability.contains(
                "probability -= targetMr * config.getMrMultiplier();"));

        String spellUse = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        int spellPollute = spellUse.indexOf(
                "if (cha.hasSkillEffect(POLLUTE_WATER))");
        assertTrue(spellPollute >= 0);
        assertTrue(spellUse.substring(spellPollute,
                Math.min(spellUse.length(), spellPollute + 350))
                .contains("this._dmg = (this._dmg >> 1);"));

        String potion = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/hp/UserAddHp.java"));
        assertOrdered(potion,
                "addhp += pc.getPotionRecovery();",
                "if (pc.hasSkillEffect(POLLUTE_WATER))",
                "if (pc.hasSkillEffect(ADLV80_2_2))",
                "if (pc.hasSkillEffect(ADLV80_2_1))",
                "pc.adjustCurrentHp(addhp);");
        assertTrue(potion.contains("addhp = (addhp >> 1);"));
        assertTrue(potion.contains("addhp *= -1;"));

        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Icons.java"));
        assertTrue(icons.contains("data[32] = (byte)"
                + " (pc.getSkillEffectTimeSec(POLLUTE_WATER) / 4);"));
        // The selected LiTo opcode table maps FA to S_LITO_CLAN, so the old
        // generic PacketBox/S_Icons payload is evidence only and is not sent.
        assertEquals(0xFA, Protocol.S_LITO_CLAN);

        String list = read(RETAINED.resolve(
                "src/com/lineage/list/PcLvSkillList.java"));
        assertOrdered(list,
                "if ((elfAttr & 4) != 0)",
                "skillList.add(POLLUTE_WATER - 1)");
        String crystal = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/skill/"
                        + "Skill_SpiritCrystal_Water.java"));
        assertOrdered(crystal,
                "nameId.equalsIgnoreCase(\"$4717\")",
                "skillid = POLLUTE_WATER;",
                "magicLv = 15;",
                "Skill_Check.check(pc, item, skillid, magicLv, attribute)");
        assertTrue(spellUse.contains("((_player.getElfAttr() & magicattr) == 0)"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("POLLUTE_WATER"),
                "negative skill173 is transient and must not survive reconnect");

        for (String itemPath : new String[] {
                "src/com/lineage/data/item_etcitem/hp/UserHpr.java",
                "src/com/lineage/data/item_etcitem/mp/UserMpr.java",
                "src/com/lineage/data/item_etcitem/Fish.java",
                "src/com/lineage/server/model/Instance/L1NpcInstance.java",
                "src/com/lineage/server/model/proc/ProcSkillEffectHelper.java"}) {
            assertTrue(read(RETAINED.resolve(itemPath)).contains("POLLUTE_WATER"),
                    "missing retained recovery consumer " + itemPath);
        }
    }

    @Test
    void selectedLiToBinaryAndIdaProducerPinSelectorRangeAndCccdhhWireShape()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String selector = windowAfter(SELECTED_DISASSEMBLY, "loc_4CB755:", 28);
        assertTrue(selector.contains(
                "cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173"));
        assertTrue(selector.contains("mov     edx, 0Ah"));
        assertTrue(selector.contains("call    sub_4CA6E0"));

        String producer = windowAfter(SELECTED_DISASSEMBLY,
                "loc_4CA77C:", 42);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));
        assertTrue(producer.contains("push    ecx"));
        assertTrue(producer.contains("push    ebx"));
        assertEquals(1_000, readPeWordAtVa(UNPACKED,
                0x635774L
                        + (long) (BattleSpellCatalog.POLLUTE_WATER - 1) * 2L));
    }

    @Test
    void implementationPinsWaterGateBattleAndWorldTransientStatusAndRecovery()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("POLLUTE_WATER, new PolluteWaterSpell("));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("worldPolluteWater"));
        String method = session.substring(
                session.indexOf("private void usePolluteWaterSpell(\n"
                        + "            byte[] payload, BattleSpellCatalog.PolluteWaterSpell spell,\n"),
                session.indexOf("/** Server-side Water Dragon encounter producer"));
        assertTrue(method.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(method.contains("(elfAttribute & spell.attribute()) == 0"));
        assertTrue(method.contains("learnedSkills.contains(spell.skillId())"));
        assertTrue(method.contains("polluteWaterClassicWorldMonster("));
        assertTrue(method.contains("sourceCounterMagicBypassSkills.contains("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("persisted=false"));
        assertEquals(0, occurrences(method, "saveTimed"));

        assertTrue(session.contains("BattlePotionRecovery.adjustedAmount("));
        assertTrue(session.contains("BattleHealingRecovery.adjustedAmount("));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = virtualAddress - imageBase;
        int sections = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = sections + index * 40;
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
        throw new IllegalArgumentException("unmapped PE virtual address");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String tableBlock(String sql, String table) {
        String marker = "-- Table structure for " + table;
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, "missing table block " + table);
        int end = sql.indexOf("-- Table structure for ", start + marker.length());
        return sql.substring(start, end < 0 ? sql.length() : end);
    }

    private static void assertOrdered(String text, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int found = text.indexOf(fragment, previous + 1);
            assertTrue(found > previous, "missing/out-of-order fragment: " + fragment);
            previous = found;
        }
    }

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        assertTrue(Files.isRegularFile(path), "missing IDA evidence: " + path);
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int remaining = 0;
        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!found && line.contains(marker)) {
                    found = true;
                    remaining = followingLines;
                }
                if (found) {
                    result.append(line).append('\n');
                    if (remaining-- == 0) break;
                }
            }
        }
        assertTrue(found, "missing IDA marker: " + marker);
        return result.toString();
    }

    private static String sha256(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing selected client: " + path);
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
