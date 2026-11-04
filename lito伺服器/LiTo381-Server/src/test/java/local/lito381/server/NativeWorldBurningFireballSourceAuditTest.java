package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill25 producer, source metadata and shared WORLD route. */
final class NativeWorldBurningFireballSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    @Test
    void selectedProducerRowsAndCurrentCatalogStayExact() throws Exception {
        String primary = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (25, 0, '燃燒的火球', 4, 0, 20, 0, 0, 0, 500, 0, 'attack', 3, 0, 9, 5, 0, 0, 2, 64, 0, 6, 3, 0, 1, '$1451', 18, 171, 0, 0, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (25, '燃燒的火球', NULL, NULL);"));

        String secondary = Files.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (25, '燃燒的火球', 4, 0, 20, 0, 0, 0, 1000, 0, 'attack', 3, 0, 9, 5, 0, 0, 2, 64, 0, 6, 3, 0, 1, '$1451', 18, 171, 0, 0, 0, 0);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String producer = between(assembly, "loc_4CBA5C:", "loc_4CBA92:");
        assertTrue(producer.contains(
                "jumptable 004CB66E cases 16,21,24,29"));
        assertTrue(producer.contains("mov     edx, 6"));
        assertTrue(producer.contains("call    sub_4CA6E0"));
        String request = between(assembly,
                "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(request.contains("push    14h"));
        assertTrue(request.contains("push    offset aCccdhh"));

        String retained = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String targetCoordinate = between(retained,
                "case 4:// 光箭", "case 1:// 初級治癒術");
        assertTrue(targetCoordinate.contains("case 25:// 燃燒的火球"));
        assertTrue(targetCoordinate.contains("targetId = readD();"));
        assertTrue(targetCoordinate.contains("targetX = readH();"));
        assertTrue(targetCoordinate.contains("targetY = readH();"));

        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.BURNING_FIREBALL);
        assertEquals(3, spell.row());
        assertEquals(0, spell.column());
        assertEquals(20, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(5, spell.diceCount());
        assertEquals(9, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(18, spell.actionId());
        assertEquals(171, spell.effectGfx());

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains(
                "useClassicWorldAreaAttackSpell(payload, worldAreaAttack)"));
        assertTrue(session.contains("useAreaAttackSpell(payload, areaAttackSpell)"));
        assertTrue(session.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(session.contains("publishClassicWorldAreaAttackSpellPacket("));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
