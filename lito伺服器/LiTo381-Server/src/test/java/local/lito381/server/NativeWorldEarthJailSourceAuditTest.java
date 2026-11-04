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

/** Locks selected skill30 producer, rows and shared BATTLE/WORLD area route. */
final class NativeWorldEarthJailSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void selectedAndSecondaryRowsPreserveTheirReuseDifference() throws Exception {
        String primary = LocalResources.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (30, 0, '巖牢', 4, 5, 24, 0, 0, 0, 500, 0, 'attack', 3, 0, 8, 5, 0, 0, 1, 64, 0, 8, 3, 0, 32, '$1971', 18, 1805, 0, 0, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (30, '巖牢', 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (30, '巖牢', NULL, NULL);"));

        String secondary = LocalResources.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (30, '巖牢', 4, 5, 24, 0, 0, 0, 400, 0, 'attack', 3, 0, 8, 5, 0, 0, 1, 64, 0, 8, 3, 0, 32, '$1971', 18, 1805, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (30, '巖牢', 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (30, '巖牢', NULL, NULL);"));
    }

    @Test
    void selectedClientUsesCase29CccdhhAndOneHundredMillisecondClock()
            throws Exception {
        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CBA5C:", "loc_4CBA92:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 16,21,24,29"));
        assertTrue(dispatch.contains("mov     edx, 6"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        byte[] recovered = LocalResources.readAllBytes(Path.of(
                "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(100, peWordAtVirtualAddress(recovered, 0x635774 + 29 * 2));

        String clientUse = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        assertTrue(clientUse.contains("case 30:// 巖牢 1600"));
        assertTrue(clientUse.contains("targetId = readD();"));
        assertTrue(clientUse.contains("targetX = readH();"));
        assertTrue(clientUse.contains("targetY = readH();"));
    }

    @Test
    void selectedCatalogAndSharedBattleWorldRoutesStayConnected() throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.EARTH_JAIL);
        assertEquals(30, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(1_805, spell.effectGfx());
        assertEquals(24, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(8, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(5, spell.diceCount());
        assertEquals(8, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_EARTH, spell.attribute());

        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("useClassicWorldAreaAttackSpell(payload, worldAreaAttack)"));
        assertTrue(session.contains("useAreaAttackSpell(payload, areaAttackSpell)"));
        assertTrue(session.contains("publishClassicWorldAreaAttackSpellPacket("));
        assertTrue(session.contains("Protocol.attackMany("));

        String accounts = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        assertTrue(accounts.contains("BattleSpellCatalog.EARTH_JAIL,"));
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
