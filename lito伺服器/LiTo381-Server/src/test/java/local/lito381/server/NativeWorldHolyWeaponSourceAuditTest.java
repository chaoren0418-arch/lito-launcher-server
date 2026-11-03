package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks LiTo.bin case7, selected skill8 rows and the routed WORLD handler. */
final class NativeWorldHolyWeaponSourceAuditTest {
    @Test
    void nativeProducerSelectedRowsAndServerRouteStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (8, 0, '神聖武器', 1, 7, 10, 0, 0, 0, 10, 1200, 'buff', 3, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 128, '$1977', 19, 2165, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (8, '神聖武器', 'DMG', 1);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (8, '神聖武器', 'HIT', 1);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB71D:", "loc_4CB741:");
        assertTrue(dispatch.contains("cases 7,8,22,36,42,43,47,54"));
        assertTrue(dispatch.contains("call    sub_4C9880"));
        String producer = between(
                assembly, "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        String clientUse = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String objectCases = between(clientUse, "case 1://", "case 2://");
        assertTrue(objectCases.contains("case 8:// 神聖武器"));
        assertTrue(objectCases.contains("targetId = readD();"));

        String catalog = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("HOLY_WEAPON, new WeaponBuffSpell(HOLY_WEAPON"));
        assertTrue(catalog.contains("1, 1, 0, 0, 79, 161, \"holy-weapon\""));
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("BattleSpellCatalog.weaponBuffSpell(selector.skillId())"));
        assertTrue(session.contains("useWeaponBuffSpell(payload, worldWeaponBuff)"));
        assertTrue(session.contains("weaponBuffStatus.modifier("));

        String transport = Files.readString(Path.of(
                "tools/classic_world_monster_physical_selftest.py"),
                StandardCharsets.UTF_8);
        String walk = between(transport, "def walk(", "def parse_attack(");
        assertTrue(walk.indexOf("session.send(move(x, y, heading))")
                < walk.indexOf("x += DX[heading]"));
        String acceptance = Files.readString(Path.of(
                "tools/classic_world_bless_weapon_selftest.py"),
                StandardCharsets.UTF_8);
        assertTrue(acceptance.contains("request_prefix = bytes((0x14, *SELECTOR))"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
