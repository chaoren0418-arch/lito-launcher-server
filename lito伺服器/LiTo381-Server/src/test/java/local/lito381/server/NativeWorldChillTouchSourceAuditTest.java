package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks LiTo.bin case9, selected skill10 data and retained life-drain order. */
final class NativeWorldChillTouchSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    @Test
    void nativeProducerSelectedRowRetainedMeaningAndServerRouteStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (10, 0, '寒冷戰慄', 2, 1, 8, 0, 0, 0, 10, 0, 'attack', 3, 8, 10, 2, 0, 0, 4, 64, -2, 3, 0, 0, 2, '$1442', 18, 252, 0, 0, 0, 0);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB8B4:", "loc_4CB8F1:");
        assertTrue(dispatch.contains("jumptable 004CB66E case 9"));
        assertTrue(dispatch.contains("push    12h"));
        assertTrue(dispatch.contains("mov     edx, 2"));
        assertTrue(dispatch.contains("mov     ecx, 9"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        String clientUse = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String coordinateCases = between(clientUse,
                "case 4:// 光箭", "case 1:// 初級治癒術");
        assertTrue(coordinateCases.contains("case 10:// 寒冷戰慄"));
        assertTrue(coordinateCases.contains("targetId = readD();"));
        assertTrue(coordinateCases.contains("targetX = readH();"));
        assertTrue(coordinateCases.contains("targetY = readH();"));

        String retained = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(retained.contains(
                "(this._skillId == CHILL_TOUCH) || (this._skillId == VAMPIRIC_TOUCH)"));
        assertTrue(retained.contains("heal = this._dmg;"));
        assertTrue(retained.contains(
                "if (heal > 0 && castSnapshot._user != null)"));
        assertTrue(retained.contains(
                "castSnapshot._user.adjustCurrentHpIfAlive(heal, 0);"));

        String catalog = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains(
                "Map.entry(CHILL_TOUCH, new AttackSpell(CHILL_TOUCH, 1, 1, 18, 252,"));
        assertTrue(catalog.contains(
                "8, 10, 3, 8, 2, 10, ATTRIBUTE_WATER, -2, \"chill-touch\"))"));
        assertTrue(catalog.contains(
                "return skillId == CHILL_TOUCH || skillId == VAMPIRIC_TOUCH;"));

        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains(
                "useClassicWorldAttackSpell(payload, worldAttack)"));
        assertTrue(session.contains("useAttackSpell(payload, attackSpell)"));
        assertTrue(session.contains("applyAcceptedLifeDrain("));
        assertTrue(session.contains("if (!spell.drainsLife() || damage <= 0)"));
        assertTrue(session.contains("if (playerHp <= 0) return 0;"));
        assertTrue(session.contains("Math.addExact((long) playerHp, (long) damage)"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
