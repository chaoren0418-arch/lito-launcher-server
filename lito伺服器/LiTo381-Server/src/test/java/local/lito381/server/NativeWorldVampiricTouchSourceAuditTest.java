package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill28 producer, resources, retained drain and shared routes. */
final class NativeWorldVampiricTouchSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED_RELATIVE);
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void selectedAndSecondaryRowsPreserveTheirReuseDifference() throws Exception {
        String primary = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (28, 0, '吸血鬼之吻', 4, 3, 12, 0, 0, 0, 50, 0, 'attack', 3, 12, 10, 3, 0, 0, 4, 64, -5, 4, 0, 0, 8, '$1454', 18, 236, 0, 0, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (28, '吸血鬼之吻', 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (28, '吸血鬼之吻', NULL, NULL);"));

        String secondary = Files.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (28, '吸血鬼之吻', 4, 3, 12, 0, 0, 0, 100, 0, 'attack', 3, 12, 10, 3, 0, 0, 4, 64, -5, 4, 0, 0, 8, '$1454', 18, 236, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (28, '吸血鬼之吻', 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (28, '吸血鬼之吻', NULL, NULL);"));
    }

    @Test
    void selectedClientUsesCase27CccdhhAndOneHundredMillisecondClock()
            throws Exception {
        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB938:", "loc_4CB952:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(dispatch.contains("mov     edx, 3"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        byte[] recovered = Files.readAllBytes(Path.of(
                "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(100, peWordAtVirtualAddress(recovered, 0x635774 + 27 * 2));

        String clientUse = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String coordinateCases = between(
                clientUse, "case 4:// 光箭", "case 1:// 初級治癒術");
        assertTrue(coordinateCases.contains("case 28:// 吸血鬼之吻"));
        assertTrue(coordinateCases.contains("targetId = readD();"));
        assertTrue(coordinateCases.contains("targetX = readH();"));
        assertTrue(coordinateCases.contains("targetY = readH();"));
    }

    @Test
    void retainedAcceptedDamageBecomesCasterHealing() throws Exception {
        String retained = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(retained.contains(
                "(this._skillId == CHILL_TOUCH) || (this._skillId == VAMPIRIC_TOUCH)"));
        assertTrue(retained.contains("heal = this._dmg;"));
        assertTrue(retained.contains("final boolean gateOnDamageAcceptance = this._dmg > 0;"));
        assertTrue(retained.contains("if (heal > 0 && castSnapshot._user != null)"));
        assertTrue(retained.contains(
                "castSnapshot._user.adjustCurrentHpIfAlive(heal, 0);"));
    }

    @Test
    void selectedCatalogAndSharedBattleWorldRoutesStayConnected() throws Exception {
        BattleSpellCatalog.AttackSpell spell = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.VAMPIRIC_TOUCH);
        assertEquals(28, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(3, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(236, spell.effectGfx());
        assertEquals(12, spell.mpCost());
        assertEquals(50, spell.reuseDelayMillis());
        assertEquals(4, spell.maximumRange());
        assertEquals(12, spell.baseDamage());
        assertEquals(3, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertTrue(spell.drainsLife());

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("useClassicWorldAttackSpell(payload, worldAttack)"));
        assertTrue(session.contains("useAttackSpell(payload, attackSpell)"));
        assertTrue(session.contains("if (!spell.drainsLife() || damage <= 0)"));
        assertTrue(session.contains("applyAcceptedLifeDrain("));
        assertTrue(session.contains("Math.addExact((long) playerHp, (long) damage)"));

        String accounts = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        assertTrue(accounts.contains("BattleSpellCatalog.VAMPIRIC_TOUCH,"));
    }

    @Test
    void releaseCoverageKeepsTheNativeSkill28Contract() throws Exception {
        String coverage = Files.readString(Path.of("tools/protocol_coverage.py"),
                StandardCharsets.UTF_8);
        assertTrue(coverage.contains(
                "skill28 Vampiric Touch / 吸血鬼之吻 follows selected LiTo.bin"));
        assertTrue(coverage.contains("exact object-coordinate cccdhh 14 03 03"));
        assertTrue(coverage.contains("server reuse50ms"));
        assertTrue(coverage.contains("HP58->80"));
        assertTrue(coverage.contains("MP85->73"));
    }

    private static int peWordAtVirtualAddress(byte[] data, int address) {
        ByteBuffer bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pe = bytes.getInt(0x3C);
        int sections = Short.toUnsignedInt(bytes.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(bytes.getShort(pe + 20));
        int optional = pe + 24;
        int imageBase = bytes.getInt(optional + 28);
        int rva = address - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            int virtualSize = bytes.getInt(section + 8);
            int sectionRva = bytes.getInt(section + 12);
            int rawSize = bytes.getInt(section + 16);
            int rawOffset = bytes.getInt(section + 20);
            long upper = Integer.toUnsignedLong(sectionRva)
                    + Math.max(Integer.toUnsignedLong(virtualSize),
                            Integer.toUnsignedLong(rawSize));
            if (Integer.toUnsignedLong(rva) >= Integer.toUnsignedLong(sectionRva)
                    && Integer.toUnsignedLong(rva) + 2 <= upper) {
                return Short.toUnsignedInt(bytes.getShort(
                        rawOffset + rva - sectionRva));
            }
        }
        throw new IllegalArgumentException("unmapped PE address " + address);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
