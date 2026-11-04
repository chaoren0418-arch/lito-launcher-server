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
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill66 producer, source rows and both sleep-area paths. */
final class NativeWorldFogSleepingSkill66SourceAuditTest {
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
    void selectedAndSecondaryRowsPinFogOfSleepingResources()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (66, 0, '沉睡之霧', 9, 1,"
                + " 50, 0, 40318, 1, 1500, 32, 'buff', 3, 0, 0, 0, 0, 7,"
                + " 4, 4, -50, 10, 3, 0, 2, '$1477', 19, 760, 0, 0, 0,"
                + " 280);", skillRow(selected, 66));
        assertEquals("INSERT INTO `skills` VALUES (66, '沉睡之霧', 9, 1, 50,"
                + " 0, 40318, 1, 1500, 32, 'buff', 3, 0, 0, 0, 0, 7, 4,"
                + " 4, -50, 10, 3, 0, 2, '$1477', 19, 760, 0, 0, 0,"
                + " 280);", skillRow(secondary, 66));
        for (String sql : new String[] {selected, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_負面技能控制` VALUES"
                    + " (66, '沉睡之霧-睡眠耐性', 50, 42, 34, 0, 0, 0.35,"
                    + " 3, 0, 1, 0.05, 0, 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (66, '沉睡之霧', 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (66, '沉睡之霧', NULL, NULL);"));
            assertFalse(sql.contains(
                    "INSERT INTO `skills_buff_ability` VALUES (66,"));
        }
    }

    @Test
    void selectedLiToCase65IsCccdhhWithTargetFlagAndClientClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CB989:", "loc_4CB9A3:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 14,15,33,65,75",
                "mov     edx, 0Ah", "mov     [edi+68h], esi",
                "call    sub_4CA6E0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        String shared = between(assembly,
                "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(shared.contains("push    offset aCccdhh"));
        assertTrue(shared.contains("push    14h"));
        assertTrue(shared.contains("mov     dword_834870, eax"));
        assertTrue(shared.contains("mov     dword_834878, ecx"));
        assertArrayEquals("cccdhh\0".getBytes(StandardCharsets.US_ASCII),
                peBytesAtVa(UNPACKED, 0x00636028L, 7));
        assertEquals(1, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 65L, 1)[0]));
        assertEquals(1_500,
                readPeWordAtVa(UNPACKED, 0x00635774L + 65L * 2L));
    }

    @Test
    void catalogProtocolAndRetainedSleepLifecycleStayExact()
            throws Exception {
        BattleSpellCatalog.SleepSpell spell = BattleSpellCatalog.sleepSpell(
                BattleSpellCatalog.FOG_OF_SLEEPING);
        assertEquals(66, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(1, spell.column());
        assertEquals(50, spell.mpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(32, spell.durationSeconds());
        assertEquals(19, spell.actionId());
        assertEquals(760, spell.effectGfx());
        assertEquals(297, spell.targetFailureMessage());
        assertEquals(280, spell.casterFailureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(54, spell.probability(50, 40, 13, 0, 4, 20, 3));

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).d(16)
                .h(32_820).h(32_766).bytes();
        assertEquals(11, request.length);
        Protocol.AttackSpellRequest parsed = Protocol.attackSpellRequest(request);
        assertEquals(spell.skillId(), parsed.skillId());
        assertEquals(16, parsed.targetObjectId());
        assertEquals(32_820, parsed.x());
        assertEquals(32_766, parsed.y());
        assertArrayEquals(new byte[] {0x25, 0x0A}, Protocol.paralysis(
                Protocol.ParalysisMode.SLEEP_APPLY));
        assertArrayEquals(new byte[] {0x25, 0x0B}, Protocol.paralysis(
                Protocol.ParalysisMode.SLEEP_RELEASE));

        String retained = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        for (String anchor : new String[] {
                "case FOG_OF_SLEEPING:",
                "if (this._user.getId() == cha.getId())",
                "magic.calcProbabilityMagic(this._skillId)",
                "new S_ServerMessage(297)",
                "new S_Paralysis(S_Paralysis.TYPE_SLEEP, true)",
                "cha.setSleeped(true)"
        }) assertTrue(retained.contains(anchor), () -> "missing retained " + anchor);
    }

    @Test
    void bothPhasesLockRevalidateCommitThenCleanupBeforePublication()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String battle = between(session, "private void useSleepSpell(",
                "private void useLineAttackSpell(");
        for (String anchor : new String[] {
                "long originRoomId = currentRoomId;",
                "int originMapId = hub.battleMapId(originRoomId, this);",
                "int originX = positionX;", "int originY = positionY;",
                "phantasmTargets(", "BattleSleepTarget",
                "BattleTrueTarget.Kind.COMPANION",
                "ClientSession firstLock", "synchronized (firstLock)",
                "synchronized (secondLock)", "currentPrimary",
                "primaryStillAttackable", "endpointStillAllowed",
                "currentRoomId == originRoomId",
                "positionX == originX && positionY == originY",
                "BattleSpellCatalog.hasNativeLineOfSight(",
                "BattleAbsoluteBarrierRules.blocksIncomingSpell(",
                "companion.activateSleep(",
                "candidate.npcStats().boss()"
        }) assertTrue(battle.contains(anchor), () -> "missing BATTLE lease " + anchor);
        assertCommitCleanupPublishOrder(battle,
                "cancelMeditation(\"spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())",
                "revealInvisibility(\"spell-\" + spell.skillId())");
        assertFalse(battle.contains("commitSpellReady("));

        String world = between(session,
                "private void useClassicWorldSleepSpell(",
                "private void useSleepSpell(");
        for (String anchor : new String[] {
                "long originRoomId = currentRoomId;",
                "int originMapId = classicWorldMapId;",
                "int originInstanceId = classicWorldInstanceId;",
                "int originX = positionX;", "int originY = positionY;",
                "phantasmTargets(", "ClassicWorldSleepTarget",
                "ClientSession primaryPlayerLock", "ClientSession firstLock",
                "synchronized (firstLock)", "synchronized (secondLock)",
                "currentPrimary", "currentPrimary.kind() == primary.kind()",
                "positionX != originX || positionY != originY",
                "map.hasLineOfSight(originX, originY,",
                "map.isSafetyZone(originX, originY)",
                "companion.activateSleep(",
                "hub.sleepClassicWorldMonster("
        }) assertTrue(world.contains(anchor), () -> "missing WORLD lease " + anchor);
        assertCommitCleanupPublishOrder(world,
                "cancelMeditation(\"classic-world-spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"classic-world-spell-\" + spell.skillId())",
                "revealInvisibility(\"classic-world-spell-\" + spell.skillId())");
        assertFalse(world.contains("commitSpellReady("));
    }

    private static void assertCommitCleanupPublishOrder(
            String method, String meditationText, String barrierText,
            String invisibilityText) {
        int inventory = method.indexOf("inventory = nextInventory;");
        int mp = method.indexOf("playerMp -= mpCost;", inventory);
        int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int meditation = method.indexOf(meditationText, reuse);
        int barrier = method.indexOf(barrierText, meditation);
        int invisibility = method.indexOf(invisibilityText, barrier);
        int publish = method.indexOf("Protocol.objectAction(", invisibility);
        assertTrue(inventory >= 0 && mp > inventory && reuse > mp
                && meditation > reuse && barrier > meditation
                && invisibility > barrier && publish > invisibility,
                () -> "order=" + List.of(inventory, mp, reuse, meditation,
                        barrier, invisibility, publish));
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
        ByteBuffer little = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = little.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(little.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(little.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(little.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(little.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(little.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(little.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(little.getInt(section + 20));
            if (sectionRva <= rva
                    && rva + length <= sectionRva + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                byte[] result = new byte[length];
                System.arraycopy(bytes, offset, result, 0, length);
                return result;
            }
        }
        throw new IllegalArgumentException(
                "unmapped PE address " + Long.toHexString(va));
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        return Short.toUnsignedInt(ByteBuffer.wrap(peBytesAtVa(path, va, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
