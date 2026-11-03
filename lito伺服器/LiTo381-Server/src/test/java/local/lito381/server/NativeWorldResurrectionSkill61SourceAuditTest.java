package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Native/client/database/source proof for skill61 Resurrection. */
final class NativeWorldResurrectionSkill61SourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedAndSecondaryRowsPinSkill61AndEverySelectedMapFlag()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY_SQL));
        assertEquals("INSERT INTO `skills` VALUES (61, 0, '返生術', 8, 4,"
                + " 50, 0, 0, 0, 10, 0, 'buff', 3, 0, 0, 0, 0, 0, 0,"
                + " 32, 0, -1, 0, 0, 16, '$1475', 19, 3944, 0, 0, 0, 280);",
                skillRow(primary, 61));
        assertEquals("INSERT INTO `skills` VALUES (61, '返生術', 8, 4, 50,"
                + " 0, 0, 0, 0, 0, 'buff', 3, 0, 0, 0, 0, 0, 0, 32,"
                + " 0, -1, 0, 0, 16, '$1475', 19, 3944, 0, 0, 0, 280);",
                skillRow(secondary, 61));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (61, '返生術', NULL, NULL);"));
        }

        Pattern rows = Pattern.compile(
                "^INSERT INTO `mapids` VALUES \\((.*)\\);$", Pattern.MULTILINE);
        Matcher matcher = rows.matcher(primary);
        int matched = 0;
        int blocked = 0;
        while (matcher.find()) {
            List<String> fields = splitSqlValues(matcher.group(1));
            assertEquals(23, fields.size());
            int mapId = Integer.parseInt(fields.get(0));
            boolean sourceAllows = Integer.parseInt(fields.get(12)) == 1;
            assertEquals(!sourceAllows,
                    ClassicWorldResurrectionCatalog.mapBlocked(mapId),
                    "mapids.resurrection mismatch map=" + mapId);
            assertEquals(sourceAllows,
                    ClassicWorldResurrectionCatalog.allowsSelectedMap(mapId, true),
                    "selected map admission mismatch map=" + mapId);
            matched++;
            if (!sourceAllows) blocked++;
        }
        assertEquals(731, matched);
        assertEquals(ClassicWorldResurrectionCatalog.BLOCKED_MAP_COUNT, blocked);
        assertTrue(ClassicWorldResurrectionCatalog.allowsSelectedMap(4, true));
        assertFalse(ClassicWorldResurrectionCatalog.allowsSelectedMap(303, true));
        assertFalse(ClassicWorldResurrectionCatalog.allowsSelectedMap(4, false));
    }

    @Test
    void selectedLiToCase60EmitsExactObjectRequestAndTenMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String router = between(assembly, "loc_4CB8A0:", "loc_4CB8B4:");
        assertTrue(router.contains("jumptable 004CB66E cases 40,60,74"));
        assertTrue(router.contains("call    sub_4CA4F0"));
        String producer = between(assembly,
                "sub_4CA4F0 proc near", "sub_4CA4F0 endp");
        for (String anchor : new String[] {
                "cmp     al, 3", "cmp     al, 2", "cmp     eax, 1Eh",
                "cmp     eax, 0Fh", "mov     edx, dword_63D530",
                "and     eax, 7", "sar     edi, 3", "push    14h",
                "push    offset aCccd", "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        String callback = between(assembly,
                "sub_4CA3C0 proc near", "sub_4CA3C0 endp");
        for (String anchor : new String[] {
                "cmp     al, 2", "cmp     al, 3", "push    esi",
                "and     ecx, 7", "sar     eax, 3", "push    14h",
                "push    offset aCccd"
        }) assertTrue(callback.contains(anchor), () -> "missing callback " + anchor);
        assertEquals("cccd\0", new String(
                peBytesAtVa(UNPACKED_CLIENT, 0x00502CE0L, 5),
                StandardCharsets.US_ASCII));
        assertEquals(10, readPeWordAtVa(UNPACKED_CLIENT,
                0x00635774L + (BattleSpellCatalog.RESURRECTION - 1L) * 2L));

        String ask = between(assembly,
                "sub_4A6890 proc near", "sub_4A6890 endp");
        assertTrue(ask.indexOf("cmp     ax, 141h") < ask.indexOf("loc_4A6A0C:"));
        String response = between(assembly,
                "sub_4A6F70 proc near", "sub_4A6F70 endp");
        assertTrue(response.contains("push    offset aDcdh"));
    }

    @Test
    void catalogAndProtocolPinQuestion321HalfAndQuarterHpContracts() {
        BattleSpellCatalog.ResurrectionSpell spell =
                BattleSpellCatalog.resurrectionSpell(BattleSpellCatalog.RESURRECTION);
        assertEquals(61, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3944, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(321, spell.questionId());
        assertEquals(50, spell.restoredPlayerHp(100));
        assertEquals(25, spell.restoredNpcHp(100));
        assertFalse(spell.restoresExperience());

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(7).c(4).d(0x1122_3344L).bytes();
        Protocol.ObjectSpellRequest parsed = Protocol.objectSpellRequest(request);
        assertEquals(61, parsed.skillId());
        assertEquals(0x1122_3344L, parsed.targetObjectId());
        assertEquals(7, request.length);
        assertArrayEquals(new byte[] {0x24, 0x41, 0x01},
                Protocol.resurrectionAsk());
        assertArrayEquals(new byte[] {0x23, 0x41, 0x01, 0x01, 0x00},
                new Protocol.Writer().c(Protocol.C_ANSWER).h(321).c(1).s("").bytes());
        assertArrayEquals(new byte[] {
                0x11, 0x44, 0x33, 0x22, 0x11, 0x00,
                (byte) 0x88, 0x77, 0x66, 0x55, 0x68, 0x0F
        }, Protocol.resurrect(0x1122_3344L, 0, 0x5566_7788L, 3944));
    }

    @Test
    void retainedModeAndImplementationPreserveCommitThenPromptSuppression()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/RESURRECTION.java"));
        for (String anchor : new String[] {
                "srcpc.getId() != pc.getId()",
                "World.get().getVisiblePlayer(pc, 0)",
                "new S_ServerMessage(592)", "pc.isDead()",
                "pc.getMap().isUseResurrection()", "pc.setGres(false)",
                "pc.setTempID(srcpc.getId())", "new S_Message_YN(321)",
                "!(cha instanceof L1TowerInstance)", "isCantResurrect()",
                "npc instanceof L1PetInstance",
                "npc.resurrect(npc.getMaxHp() / 4)"
        }) assertTrue(mode.contains(anchor), () -> "missing retained mode " + anchor);

        String answer = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_Attr.java"));
        String question321 = between(answer, "case 321:", "case 322:");
        assertTrue(question321.contains("pc.setTempID(0)"));
        assertTrue(question321.contains("pc.resurrect(pc.getMaxHp() / 2)"));
        assertTrue(question321.contains("pc.setCurrentHp(pc.getMaxHp() / 2)"));
        assertTrue(question321.contains("new S_Resurrection(pc, resusepc1, 0)"));

        String retainedUse = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String normal = between(retainedUse,
                "case TYPE_NORMAL -> { // 魔法詠唱時", "case TYPE_LOGIN ->");
        int commit = normal.indexOf("commitMaterialAndAcquireCastLocks()");
        int breakStatus = normal.indexOf("breakInterruptedCastEffects()", commit);
        int motion = normal.indexOf("sendCastMotion()", breakStatus);
        int run = normal.indexOf("runSkill()", motion);
        int sideEffects = normal.indexOf("applySideEffectsAfterNormalCast()", run);
        assertTrue(commit >= 0 && breakStatus > commit && motion > breakStatus
                && run > motion && sideEffects > run);

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session,
                "private void useClassicWorldResurrectionSpell(",
                "private void useCallOfNatureSpell(");
        int worldCommit = world.indexOf("commitClassicWorldResurrectionCast(");
        int worldBreak = world.indexOf("cancelMeditation(\"spell-\"", worldCommit);
        int worldReveal = world.indexOf("revealInvisibility(\"spell-\"", worldBreak);
        int worldAction = world.indexOf("Protocol.objectAction(", worldReveal);
        int worldMapGate = world.indexOf("allowsSelectedMap(", worldAction);
        int worldPending = world.indexOf("playerTarget.pendingResurrection", worldMapGate);
        assertTrue(worldCommit >= 0 && worldBreak > worldCommit
                && worldReveal > worldBreak && worldAction > worldReveal
                && worldMapGate > worldAction && worldPending > worldMapGate);
        assertTrue(world.contains("prompt suppressed map-resurrection-disabled"));

        String worldCommitMethod = between(session,
                "private ResurrectionCastCommit commitClassicWorldResurrectionCast(",
                "private void useCallOfNatureSpell(");
        assertTrue(worldCommitMethod.contains("spellReadyNanos.put(spell.skillId()"));
        assertFalse(worldCommitMethod.contains("cancelMeditation("));
        assertFalse(worldCommitMethod.contains("revealInvisibility("));
        assertFalse(worldCommitMethod.contains("commitSpellReady("));

        String battle = between(session,
                "private void useResurrectionSpell(byte[] payload,",
                "private void publishCreatedZombie(");
        int battleCommit = battle.indexOf("commitResurrectionCast(");
        int battleBreak = battle.indexOf("cancelMeditation(\"spell-\"", battleCommit);
        int battleReveal = battle.indexOf("revealInvisibility(\"spell-\"", battleBreak);
        int battleAction = battle.indexOf("Protocol.objectAction(", battleReveal);
        assertTrue(battleCommit >= 0 && battleBreak > battleCommit
                && battleReveal > battleBreak && battleAction > battleReveal);
        String battleCommitMethod = battle.substring(
                battle.indexOf("private ResurrectionCastCommit commitResurrectionCast("));
        assertTrue(battleCommitMethod.contains("spellReadyNanos.put(spell.skillId()"));
        assertFalse(battleCommitMethod.contains("cancelMeditation("));
        assertFalse(battleCommitMethod.contains("revealInvisibility("));
        assertFalse(battleCommitMethod.contains("commitSpellReady("));

        String callback = between(session,
                "private void answerResurrection(",
                "private void clearPendingAskCallbacks(");
        assertTrue(callback.indexOf("pendingResurrection = null")
                < callback.indexOf("if (pending == null)"));
        assertTrue(callback.contains("answer.questionId() == spell.questionId()"));
        assertTrue(callback.contains("answer.answer() == 1 && answer.text().isEmpty()"));
        assertTrue(callback.contains("ClassicWorldResurrectionCatalog.allowsSelectedMap("));
        assertTrue(callback.contains("Protocol.effectObject(playerObjectId(), 230)"));
        assertTrue(callback.contains("Protocol.resurrect("));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static List<String> splitSqlValues(String values) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < values.length(); index++) {
            char current = values.charAt(index);
            if (current == '\'') {
                if (quoted && index + 1 < values.length()
                        && values.charAt(index + 1) == '\'') {
                    field.append('\'');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        assertFalse(quoted, "unterminated selected SQL string");
        fields.add(field.toString().trim());
        return fields;
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
        assertEquals(0x0000_4550, image.getInt(pe));
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
