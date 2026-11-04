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

/** Locks selected skill57 producer, retained healing rules and atomic leases. */
final class NativeWorldFullHealSkill57SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedAndSecondaryRowsRetainExactFullHealResources()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (57, 0, '全部治癒術', 8, 0,"
                + " 48, 0, 0, 0, 500, 0, 'buff', 3, 15, 17, 0, 0, 0, 12,"
                + " 16, 0, -1, 0, 0, 1, '$1471', 19, 11725, 0, 77, 0, 280);",
                skillRow(selected, 57));
        assertEquals("INSERT INTO `skills` VALUES (57, '全部治癒術', 8, 0, 48,"
                + " 0, 0, 0, 500, 0, 'buff', 3, 15, 17, 0, 0, 0, 12, 16, 0,"
                + " -1, 0, 0, 1, '$1471', 19, 11725, 0, 77, 0, 280);",
                skillRow(secondary, 57));
        String item = "INSERT INTO `skills_item` VALUES (57, '全部治癒術',"
                + " NULL, NULL);";
        assertEquals(1, count(selected, item));
        assertEquals(1, count(secondary, item));
        for (String database : new String[] {selected, secondary}) {
            assertFalse(database.contains(
                    "INSERT INTO `skills_buff_ability` VALUES (57,"));
            assertFalse(database.contains(
                    "INSERT INTO `skills_負面boss免疫` VALUES (57,"));
            assertFalse(database.contains(
                    "INSERT INTO `skills_負面技能控制` VALUES (57,"));
        }
    }

    @Test
    void selectedLiToCase56UsesObjectOnlyRequestAndFiveHundredMillisecondClock()
            throws Exception {
        assertEquals(
                "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CB675:", "loc_4CB71D:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 0,18,34,56", "and     edx, 7",
                "sar     eax, 3", "push    14h", "push    offset aCccd",
                "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertEquals("cccd\0", new String(
                peBytesAtVa(UNPACKED, 0x00502CE0L, 5),
                StandardCharsets.US_ASCII));
        assertEquals(0, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 56L, 1)[0]));
        assertEquals(500, readPeWordAtVa(
                UNPACKED, 0x00635774L + 56L * 2L));

        String parser = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String objectCases = between(parser,
                "case 1:// 初級治癒術", "case 2:// 日光術");
        assertTrue(objectCases.contains("case 57:// 全部治癒術"));
        assertTrue(objectCases.contains("targetId = readD();"));
        assertFalse(objectCases.contains("targetX = readH();"));
        assertFalse(objectCases.contains("targetY = readH();"));
    }

    @Test
    void catalogProtocolAndRetainedHealingOrderRemainAuthoritative()
            throws Exception {
        BattleSpellCatalog.HealingSpell spell =
                BattleSpellCatalog.healingSpell(BattleSpellCatalog.FULL_HEAL);
        assertEquals(57, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(11725, spell.effectGfx());
        assertEquals(48, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(14, spell.diceCount(8));
        assertEquals(17, spell.dieSides());
        assertEquals(77, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(-40, spell.npcHpDelta(1, 40));
        assertEquals(0, spell.npcHpDelta(3, 40));

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).d(0x65000012L).bytes();
        assertEquals(7, request.length);
        Protocol.ObjectSpellRequest parsed = Protocol.objectSpellRequest(request);
        assertEquals(57, parsed.skillId());
        assertEquals(0x65000012L, parsed.targetObjectId());

        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        String healing = between(magic, "public int calcHealing(",
                "private int calcMrDefense(");
        assertTrue(healing.contains(
                "final int diceCount = value + healDiceBonus"));
        assertTrue(healing.contains(
                "magicDamage += (_random.nextInt(dice) + 1)"));
        assertTrue(healing.contains(
                "alignmentRevision += (getLawful() / 32768.0)"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String typeHeal = between(use, "case L1Skills.TYPE_HEAL:",
                "// TODO SKILL移轉");
        int waterLife = typeHeal.indexOf("cha.hasSkillEffect(WATER_LIFE)");
        int pollute = typeHeal.indexOf("cha.hasSkillEffect(POLLUTE_WATER)");
        int dirty = typeHeal.indexOf("cha.hasSkillEffect(ADLV80_2_2)");
        int erosion = typeHeal.indexOf("cha.hasSkillEffect(ADLV80_2_3)");
        assertTrue(0 <= waterLife && waterLife < pollute
                && pollute < dirty && dirty < erosion);
        assertTrue(use.contains("cha.removeSkillEffect(WATER_LIFE)"));
        assertTrue(use.contains("(undeadType == 1)"));
        assertTrue(use.contains("this._dmg *= -1"));
        assertTrue(use.contains("(undeadType == 3)"));
        assertTrue(use.contains("this._dmg = 0"));
    }

    @Test
    void battleAndWorldRevalidateEndpointsBeforeAtomicCommitAndPublishLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session,
                "private void useClassicWorldHealingSpell(",
                "private void useHealingSpell(");
        String battle = between(session,
                "private void useHealingSpell(",
                "private void notifyPeersOfDisconnect(");
        for (String method : new String[] {world, battle}) {
            for (String anchor : new String[] {
                    "int originX = positionX", "int originY = positionY",
                    "BattleSpellCatalog.isInNativeScreen(",
                    "positionX == originX", "positionY == originY",
                    "BattleAbsoluteBarrierRules", "targetStillAvailable",
                    "endpointStillAllowed", "playerMp -= mpCost",
                    "spellReadyNanos.put(spell.skillId()",
                    "waterLifeStatus.active(", "waterLifeStatus.cancel()",
                    "BattleHealingRecovery.adjustedSingleTargetAmount(",
                    "cancelMeditation(", "cancelAbsoluteBarrier(",
                    "revealInvisibility(", "Protocol.effectObject("
            }) assertTrue(method.contains(anchor), () -> "missing lease " + anchor);
            int mp = method.indexOf("playerMp -= mpCost");
            int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
            int consume = method.indexOf("waterLifeStatus.cancel()", reuse);
            int meditation = method.indexOf("cancelMeditation(", reuse);
            int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
            int invisible = method.indexOf("revealInvisibility(", barrier);
            assertTrue(mp >= 0 && reuse > mp && consume > reuse
                    && meditation > reuse && barrier > meditation
                    && invisible > barrier);
            assertFalse(method.contains("commitSpellReady(spell.skillId()"));
        }
        assertTrue(world.contains("classicWorldInstanceId == originInstanceId"));
        assertTrue(world.contains("map.hasLineOfSight("));
        assertTrue(world.contains("hub.classicWorldMonsterPolluteWaterActive("));
        assertTrue(battle.contains("currentRoomId == originRoomId"));
        assertTrue(battle.contains("ArenaMapCatalog.forMap(originMapId)"));
        assertTrue(battle.contains("BattleSpellCatalog.hasNativeLineOfSight("));
        assertTrue(battle.contains("hub.visibleTimeAttackActors("));
    }

    private static int count(String source, String needle) {
        int result = 0;
        for (int at = 0; (at = source.indexOf(needle, at)) >= 0;
             at += needle.length()) result++;
        return result;
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from,
                () -> "missing source boundary " + start + " -> " + end);
        return text.substring(from, to);
    }

    private static byte[] peBytesAtVa(Path path, long va, int length)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva && rva + length <= sectionRva
                    + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                return java.util.Arrays.copyOfRange(bytes, offset, offset + length);
            }
        }
        throw new IllegalArgumentException("unmapped PE VA " + Long.toHexString(va));
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        return Short.toUnsignedInt(ByteBuffer.wrap(peBytesAtVa(path, va, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort());
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
