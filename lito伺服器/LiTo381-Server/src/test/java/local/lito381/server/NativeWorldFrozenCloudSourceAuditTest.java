package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected-client skill22 producer, retained rows and both execution routes. */
final class NativeWorldFrozenCloudSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    @Test
    void nativeProducerSelectedRowsAndServerRoutesStayExact() throws Exception {
        String primary = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (22, 0, '寒冰氣息', 3, 5, 20, 0, 0, 0, 300, 0, 'attack', 3, 0, 2, 2, 0, 0, 4, 64, 0, 6, 3, 0, 32, '$1447', 18, 1804, 0, 0, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (22, '寒冰氣息', '40508,40504,40521,88', '45,3,3,3');"));

        String secondary = Files.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (22, '寒冰氣息', 3, 5, 20, 0, 0, 0, 300, 0, 'attack', 3, 0, 2, 2, 0, 0, 4, 64, 0, 6, 3, 0, 32, '$1447', 18, 1804, 0, 0, 0, 0);"));

        String assembly = Files.readString(Path.of(
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

        String clientUse = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        assertTrue(clientUse.contains("case 22:// 寒冰氣息"));
        assertTrue(clientUse.contains("targetId = readD();"));
        assertTrue(clientUse.contains("targetX = readH();"));
        assertTrue(clientUse.contains("targetY = readH();"));

        String catalog = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("static final int FROZEN_CLOUD = 22;"));
        assertTrue(catalog.contains(
                "FROZEN_CLOUD, new AreaAttackSpell("));
        assertTrue(catalog.contains(
                "ATTRIBUTE_WATER, 0, \"frozen-cloud\""));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains(
                "useClassicWorldAreaAttackSpell(payload, worldAreaAttack)"));
        assertTrue(session.contains("useAreaAttackSpell(payload, areaAttackSpell)"));
        assertTrue(session.contains("Protocol.attackSpellRequest(payload)"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
