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

/** Locks selected skill29 producer, resources, retained speed and shared routes. */
final class NativeWorldSlowSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void selectedAndSecondaryRowsPreserveTheirReuseDifference() throws Exception {
        String primary = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (29, 0, '緩速術', 4, 4, 20, 0, 40318, 1, 500, 64, 'buff', 3, 0, 0, 0, 0, 12, 8, 1, 0, -1, 0, 0, 16, '$1455', 19, 752, 0, 277, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (29, '緩速術', 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (29, '緩速術', 5);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (29, '緩速術', NULL, NULL);"));

        String secondary = Files.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (29, '緩速術', 4, 4, 20, 0, 40318, 1, 1000, 64, 'buff', 3, 0, 0, 0, 0, 12, 8, 1, 0, -1, 0, 0, 16, '$1455', 19, 752, 0, 277, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (29, '緩速術', 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (29, '緩速術', 5);"));
    }

    @Test
    void selectedClientUsesCase28CccdhhAndFiveHundredMillisecondClock()
            throws Exception {
        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB755:", "loc_4CBBC1:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173"));
        assertTrue(dispatch.contains("mov     edx, 0Ah"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        byte[] recovered = Files.readAllBytes(Path.of(
                "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(500, peWordAtVirtualAddress(recovered, 0x635774 + 28 * 2));

        String clientUse = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        assertTrue(clientUse.contains("case 29:// 緩速術 1600"));
        assertTrue(clientUse.contains("targetId = readD();"));
        assertTrue(clientUse.contains("checkLoc = true;"));
    }

    @Test
    void retainedSlowNeutralizesHasteOrCreatesTheNativeSpeedLease()
            throws Exception {
        String retained = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(retained.contains(
                "(this._skillId == SLOW) || (this._skillId == MASS_SLOW)"));
        assertTrue(retained.contains("pc.getHasteItemEquipped() > 0"));
        assertTrue(retained.contains("cha.getBraveSpeed() == 5"));
        assertTrue(retained.contains(
                "new S_SkillHaste(cha.getId(), 2, this._getBuffIconDuration)"));
        assertTrue(retained.contains("cha.setMoveSpeed(2);"));
        assertTrue(retained.contains("cha.removeSkillEffect(skillNum);"));
        assertTrue(retained.contains("cha.setMoveSpeed(0);"));
    }

    @Test
    void selectedCatalogAndSharedBattleWorldRoutesStayConnected() throws Exception {
        BattleSpellCatalog.EntangleSpell spell =
                BattleSpellCatalog.entangleSpell(BattleSpellCatalog.SLOW);
        assertEquals(29, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(752, spell.effectGfx());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(8, spell.maximumRange());
        assertEquals(64, spell.durationSeconds());
        assertEquals(5, spell.bossModifier());
        SourceNpcPhysicalStats earthElemental =
                SourceNpcPhysicalStats.memoryFixture(45306);
        assertEquals(45306, earthElemental.npcId());
        assertEquals(-15, earthElemental.armorClass());
        assertEquals(35, earthElemental.magicResistance());
        assertTrue(earthElemental.eraseEligible());

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("useEntangleSpell(payload, worldEntangle,"));
        assertTrue(session.contains("useEntangleSpell(payload, entangleSpell,"));
        String handler = between(session, "private void useEntangleSpell(",
                "/** Exact selected skill153/case152 cccdhh target path. */");
        assertTrue(handler.contains("spell.effectiveMpCost("));
        assertTrue(handler.contains("probabilityBlocked = probability == 0"));
        assertTrue(handler.contains("target.timedStatus.activateSlow("));
        assertTrue(handler.contains("controlledCompanion.activateSlow("));
        assertTrue(handler.contains("companionTarget.kind().classicWorldEligible()"));
        assertTrue(handler.contains("controlledCompanion.consumeEraseMagic("));
        assertTrue(handler.contains("controlledCompanion.earthBindActive("));
        assertTrue(handler.contains("hub.slowClassicWorldMonster("));
        assertTrue(handler.contains("Protocol.speed("));

        String accounts = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        assertTrue(accounts.contains("BattleSpellCatalog.SLOW,"));
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
