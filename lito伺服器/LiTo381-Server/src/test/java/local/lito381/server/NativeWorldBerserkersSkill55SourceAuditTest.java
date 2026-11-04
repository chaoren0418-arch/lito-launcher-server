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

/** Locks selected skill55 producer, source rows and BATTLE/WORLD commit path. */
final class NativeWorldBerserkersSkill55SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
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
    void selectedAndSecondaryRowsRetainReuseAndAbilityRules() throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (55, 0, '狂暴術', 7, 6, 40,"
                + " 0, 0, 0, 10, 320, 'buff', 12, 0, 0, 0, 0, 0, 0, 2, 0,"
                + " -1, 0, 0, 64, '$1986', 19, 3943, 0, 0, 0, 280);",
                skillRow(selected, 55));
        assertEquals("INSERT INTO `skills` VALUES (55, '狂暴術', 7, 6, 40, 0,"
                + " 0, 0, 0, 320, 'buff', 12, 0, 0, 0, 0, 0, 0, 2, 0, -1,"
                + " 0, 0, 64, '$1986', 19, 3943, 0, 0, 0, 280);",
                skillRow(secondary, 55));
        for (String row : new String[] {
                "INSERT INTO `skills_buff_ability` VALUES (55, '狂暴術', 'AC', 10);",
                "INSERT INTO `skills_buff_ability` VALUES (55, '狂暴術', 'DMG', 5);",
                "INSERT INTO `skills_buff_ability` VALUES (55, '狂暴術', 'HIT', 2);",
                "INSERT INTO `skills_item` VALUES (55, '狂暴術', NULL, NULL);"
        }) {
            assertEquals(1, count(selected, row), "selected " + row);
            assertEquals(1, count(secondary, row), "secondary " + row);
        }
    }

    @Test
    void selectedLiToCase54UsesOneObjectTailAndTenMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String route = between(assembly, "loc_4CB71D:", "loc_4CB741:");
        assertTrue(route.contains(
                "jumptable 004CB66E cases 7,8,22,36,42,43,47,54,66,78,147,148,150,157,159,162,164,165,167,169"));
        assertTrue(route.contains("mov     bl, byte ptr [ebp+arg_4]"));
        assertTrue(route.contains("call    sub_4C9880"));
        String producer = between(assembly, "loc_4C99DB:", "loc_4C9A0E:");
        for (String anchor : new String[] {
                "push    eax", "push    ecx", "push    esi", "push    14h",
                "push    offset aCccd", "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertEquals("cccd\0", new String(
                peBytesAtVa(UNPACKED, 0x00502CE0L, 5), StandardCharsets.US_ASCII));
        assertEquals(0, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 54L, 1)[0]));
        assertEquals(10, readPeWordAtVa(UNPACKED, 0x00635774L + 54L * 2L));
    }

    @Test
    void catalogProtocolAndRetainedServerPinCombatAbilityContract()
            throws Exception {
        BattleSpellCatalog.CombatBuffSpell spell =
                BattleSpellCatalog.combatBuffSpell(BattleSpellCatalog.BERSERKERS);
        assertEquals(55, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3943, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(320, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(12, spell.targetTo());
        assertEquals(10, spell.armorClass());
        assertEquals(2, spell.meleeHit());
        assertEquals(5, spell.meleeDamage());
        assertEquals(280, spell.failureMessage());

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).d(7).bytes();
        assertEquals(7, request.length);
        Protocol.ObjectSpellRequest parsed = Protocol.objectSpellRequest(request);
        assertEquals(spell.skillId(), parsed.skillId());
        assertEquals(7, parsed.targetObjectId());

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        for (String anchor : new String[] {
                "this._skillId == BERSERKERS", "pc.addAc(10)",
                "pc.addDmgup(5)", "pc.addHitup(2)", "pc.updateBuffStatus()"
        }) assertTrue(use.contains(anchor), () -> "missing retained use " + anchor);
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        for (String anchor : new String[] {
                "case BERSERKERS:", "cha.addAc(-10)",
                "cha.addDmgup(-5)", "cha.addHitup(-2)"
        }) assertTrue(stop.contains(anchor), () -> "missing retained stop " + anchor);
    }

    @Test
    void sharedConsumerCommitsBeforeCasterCleanupAndPublishesTargetLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session,
                "BattleSpellCatalog.CombatBuffSpell worldCombatBuff",
                "BattleSpellCatalog.DamageProtectionSpell worldDamageProtection");
        assertTrue(world.contains("useCombatBuffSpell(payload, worldCombatBuff)"));
        String battle = between(session,
                "BattleSpellCatalog.CombatBuffSpell combatBuffSpell",
                "BattleSpellCatalog.DamageProtectionSpell damageProtectionSpell");
        assertTrue(battle.contains("useCombatBuffSpell(payload, combatBuffSpell)"));

        String method = between(session, "private void useCombatBuffSpell(",
                "private void useDamageProtectionSpell(");
        for (String anchor : new String[] {
                "Protocol.objectSpellRequest(payload)", "sameNonemptyClan(target)",
                "sameClassicParty(target)", "classicWorldInstanceId == originInstanceId",
                "target.abilityStatus.activateBerserk(",
                "spellReadyNanos.put(spell.skillId()", "cancelMeditation(",
                "cancelAbsoluteBarrier(", "revealInvisibility(",
                "target.sendNativeBattleStatus(", "Protocol.manaPoint("
        }) assertTrue(method.contains(anchor), () -> "missing consumer " + anchor);
        int mp = method.indexOf("playerMp -= mpCost");
        int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int lease = method.indexOf("target.abilityStatus.activateBerserk(", reuse);
        int meditation = method.indexOf("cancelMeditation(", lease);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisible = method.indexOf("revealInvisibility(", barrier);
        int action = method.indexOf("Protocol.objectAction(", invisible);
        int status = method.indexOf("target.sendNativeBattleStatus(", action);
        assertTrue(mp >= 0 && reuse > mp && lease > reuse
                && meditation > lease && barrier > meditation
                && invisible > barrier && action > invisible && status > action);
        assertFalse(method.contains("commitSpellReady("));

        String physical = between(session, "private void attackClassicWorld(",
                "private void attackClassicWorldPlayer(");
        assertTrue(physical.contains("abilityStatus.berserk(physicalAttackNowNanos)"));
        assertTrue(physical.contains("ranged ? 0 : berserk.meleeHit()"));
        assertTrue(physical.contains("ranged ? 0 : berserk.meleeDamage()"));
        assertTrue(session.contains("abilityStatus.expireBerserk(nowNanos)"));
        assertTrue(session.contains("Cancellation player cleared Berserkers target="));
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
