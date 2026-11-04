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

/** Locks selected skill56 producer, retained rows and BATTLE/WORLD lease path. */
final class NativeWorldDiseaseSkill56SourceAuditTest {
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
    void selectedAndSecondaryRowsRetainProbabilityAbilityAndMaterialRules()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (56, 0, '疾病術', 7, 7, 10,"
                + " 0, 40318, 1, 500, 64, 'buff', 3, 0, 0, 0, 0, 10, 0, 4,"
                + " -10, 3, 0, 0, 128, '$1987', 19, 2230, 0, 0, 0, 280);",
                skillRow(selected, 56));
        assertEquals("INSERT INTO `skills` VALUES (56, '疾病術', 7, 7, 10, 0,"
                + " 40318, 1, 0, 64, 'buff', 3, 0, 0, 0, 0, 10, 0, 4, -10,"
                + " 3, 0, 0, 128, '$1987', 19, 2230, 0, 0, 0, 280);",
                skillRow(secondary, 56));
        for (String row : new String[] {
                "INSERT INTO `skills_負面boss免疫` VALUES (56, '疾病術', 5);",
                "INSERT INTO `skills_buff_ability` VALUES (56, '疾病術', 'AC', 12);",
                "INSERT INTO `skills_buff_ability` VALUES (56, '疾病術', 'DMG', -6);",
                "INSERT INTO `skills_item` VALUES (56, '疾病術', NULL, NULL);"
        }) {
            assertEquals(1, count(selected, row), "selected " + row);
            assertEquals(1, count(secondary, row), "secondary " + row);
        }
        assertFalse(selected.contains(
                "INSERT INTO `skills_buff_ability` VALUES (56, '疾病術', 'HIT'"));
        assertFalse(secondary.contains(
                "INSERT INTO `skills_buff_ability` VALUES (56, '疾病術', 'HIT'"));
        assertFalse(selected.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (56,"));
        assertFalse(secondary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (56,"));
    }

    @Test
    void selectedLiToCase55UsesObjectCoordinatesAndFiveHundredMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String route = between(assembly, "loc_4CB938:", "loc_4CB952:");
        assertTrue(route.contains(
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(route.contains("mov     edx, 3"));
        assertTrue(route.contains("call    sub_4CA6E0"));
        String producer = between(assembly,
                "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        for (String anchor : new String[] {
                "and     ecx, 7", "sar     ebx, 3", "push    eax",
                "push    ecx", "push    edx", "push    14h",
                "push    offset aCccdhh", "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertEquals("cccdhh\0", new String(
                peBytesAtVa(UNPACKED, 0x00636028L, 7),
                StandardCharsets.US_ASCII));
        assertEquals(1, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 55L, 1)[0]));
        assertEquals(500, readPeWordAtVa(
                UNPACKED, 0x00635774L + 55L * 2L));
    }

    @Test
    void catalogProtocolAndRetainedServerPinDiseaseContract() throws Exception {
        BattleSpellCatalog.WeaknessSpell spell =
                BattleSpellCatalog.weaknessSpell(BattleSpellCatalog.DISEASE);
        assertEquals(56, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2230, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(64, spell.durationSeconds());
        assertEquals(10, spell.probabilityDice());
        assertEquals(5, spell.bossModifier());
        assertEquals(-6, spell.meleeDamageModifier());
        assertEquals(0, spell.meleeHitModifier());
        assertEquals(12, spell.armorClassModifier());
        assertEquals(-10, spell.lawfulDelta());
        assertEquals(0, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).d(7).h(32768).h(32769).bytes();
        assertEquals(11, request.length);
        Protocol.AttackSpellRequest parsed = Protocol.attackSpellRequest(request);
        assertEquals(spell.skillId(), parsed.skillId());
        assertEquals(7, parsed.targetObjectId());
        assertEquals(32768, parsed.x());
        assertEquals(32769, parsed.y());

        String clientUse = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String clientCase = between(clientUse,
                "case 56:// 疾病術 1650", "case 2:// 日光術 1300");
        for (String anchor : new String[] {
                "case 57:// 全部治癒術 1650", "targetId = readD();",
                "checkLoc = true;"
        }) assertTrue(clientCase.contains(anchor), () -> "missing parser " + anchor);
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        for (String anchor : new String[] {
                "this._skillId == DISEASE", "magic.calcProbabilityMagic(this._skillId)",
                "this._player.addLawful(this._skill.getLawful())", "pc.updateBuffStatus()"
        }) assertTrue(use.contains(anchor), () -> "missing retained use " + anchor);
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        for (String anchor : new String[] {
                "case DISEASE: // 疾病", "pc.addDmgup(6)", "pc.addAc(-12)"
        }) assertTrue(stop.contains(anchor), () -> "missing retained stop " + anchor);
        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        for (String anchor : new String[] {
                "diceCount2 = getMagicBonus() + getMagicLevel() + 1;",
                "probability += _pc.getMagicHit();", "probability -= getTargetMr();",
                "probability /= bossModifier;"
        }) assertTrue(magic.contains(anchor), () -> "missing probability " + anchor);
    }

    @Test
    void battleAndWorldRevalidateEndpointsBeforeAtomicCommitAndPublishLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session,
                "private void useClassicWorldWeaknessSpell(",
                "private void useWeaknessSpell(");
        String battle = between(session,
                "private void useWeaknessSpell(byte[] payload,",
                "/** Exact selected Earth Bind path");
        for (String method : new String[] {world, battle}) {
            for (String anchor : new String[] {
                    "int originX = positionX", "int originY = positionY",
                    "BattleSpellCatalog.isInNativeScreen(",
                    "positionX == originX", "positionY == originY",
                    "targetStillAvailable", "endpointStillAllowed",
                    "config.weaknessFixtureDieRoll()",
                    "config.weaknessFixturePercentileRoll()",
                    "playerMp -= mpCost", "spellReadyNanos.put(spell.skillId()",
                    "cancelMeditation(", "cancelAbsoluteBarrier(",
                    "revealInvisibility(", "consumeCounterMagic(castNow)",
                    "Protocol.effectObject("
            }) assertTrue(method.contains(anchor), () -> "missing consumer " + anchor);
            int material = method.indexOf("nextInventory.removeCatalog(");
            int mp = method.indexOf("playerMp -= mpCost", material);
            int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
            int meditation = method.indexOf("cancelMeditation(", reuse);
            int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
            int invisible = method.indexOf("revealInvisibility(", barrier);
            assertTrue(material >= 0 && mp > material && reuse > mp
                    && meditation > reuse && barrier > meditation
                    && invisible > barrier);
            assertFalse(method.contains("commitSpellReady(spell.skillId()"));
        }
        assertTrue(world.contains("map.attackPosition(originX, originY,"));
        assertTrue(world.contains("classicWorldInstanceId != originInstanceId"));
        assertTrue(world.contains("hub.diseaseClassicWorldMonster("));
        assertTrue(battle.contains("BattleSpellCatalog.hasNativeLineOfSight("));
        assertTrue(battle.contains("ArenaMapCatalog.forMap(originMapId)"));
        assertTrue(battle.contains("currentRoomId == originRoomId"));
        assertTrue(battle.contains("hub.diseaseQuestMonster(originRoomId"));
        assertTrue(battle.contains("hub.diseaseTimeAttackActor(originRoomId"));
        assertTrue(session.contains("timedStatus.expireDisease(nowNanos)"));
        assertTrue(session.contains("Cancellation player cleared Disease target="));
        assertTrue(session.contains("int diseaseDamageModifier = diseaseActive"));
        assertTrue(session.contains("int diseaseArmorClass = timedStatus.diseaseActive"));
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
