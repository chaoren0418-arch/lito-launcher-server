package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected-client skill23 producer, retained effects and both routes. */
final class NativeWorldRevealWeaknessSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    @Test
    void nativeProducerSelectedRowsRetainedEffectsAndRoutesStayExact()
            throws Exception {
        String primary = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (23, 0, '能量感測', 3, 6, 8, 0, 0, 0, 10, 0, 'buff', 2, 0, 0, 0, 0, 0, 0, 128, 0, -1, 0, 0, 64, '$1979', 19, 2510, 0, 0, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (23, '能量感測', '40508,40504,40521,88', '45,3,3,3');"));

        String secondary = LocalResources.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (23, '能量感測', 3, 6, 8, 0, 0, 0, 0, 0, 'buff', 2, 0, 0, 0, 0, 0, 0, 128, 0, -1, 0, 0, 64, '$1979', 19, 2510, 0, 0, 0, 280);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB71D:", "loc_4CB741:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 7,8,22,36,42,43,47,54,66,78"));
        assertTrue(dispatch.contains("call    sub_4C9880"));
        String producer = between(
                assembly, "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        String clientUse = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String clientGroup = between(clientUse, "case 1://", "case 2://");
        assertTrue(clientGroup.contains("case 23:// 能量感測"));
        assertTrue(clientGroup.contains("targetId = readD();"));
        assertTrue(clientGroup.contains("checkLoc = true;"));
        assertTrue(!clientGroup.contains("targetX = readH();"));

        String retained = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        String effect = between(retained,
                "this._skillId == WEAK_ELEMENTAL", "this._skillId == RETURN_TO_NATURE");
        assertTrue(effect.contains("(weakAttr & 1) == 1")
                && effect.contains("S_SkillSound(cha.getId(), 2169)")
                && effect.contains("(weakAttr & 2) == 2")
                && effect.contains("S_SkillSound(cha.getId(), 2167)")
                && effect.contains("(weakAttr & 4) == 4")
                && effect.contains("S_SkillSound(cha.getId(), 2166)")
                && effect.contains("(weakAttr & 8) == 8")
                && effect.contains("S_SkillSound(cha.getId(), 2168)"));

        String catalog = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("static final int REVEAL_WEAKNESS = 23;"));
        assertTrue(catalog.contains("REVEAL_WEAKNESS, new RevealWeaknessSpell("));
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("useRevealWeaknessSpell(payload, worldRevealWeakness)"));
        assertTrue(session.contains("useRevealWeaknessSpell(payload, revealWeaknessSpell)"));
        assertTrue(session.contains("Protocol.objectSpellRequest(payload)"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
