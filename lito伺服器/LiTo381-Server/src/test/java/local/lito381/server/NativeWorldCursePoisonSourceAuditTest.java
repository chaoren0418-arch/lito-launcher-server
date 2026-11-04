package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected LiTo.bin case10 over the incompatible older target-only parser. */
final class NativeWorldCursePoisonSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/poison/L1DamagePoison.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    @Test
    void nativeEnvelopeSelectedResourcesRetainedPoisonAndBothRoutesStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (11, 0, '毒咒', 2, 2, 10, 0, 40318, 1, 500, 30, 'buff', 3, 0, 0, 0, 0, 12, 4, 4, -10, 1, 0, 0, 4, '$1443', 19, 745, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (11, '毒咒', 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (11, '毒咒', '40495,40499,40505,88', '10,8,3,1');"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB938:", "loc_4CB952:");
        assertTrue(dispatch.contains("jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(dispatch.contains("mov     edx, 3"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        String olderClient = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String olderTargetOnly = between(
                olderClient, "case 1:// 初級治癒術", "case 2:// 日光術");
        assertTrue(olderTargetOnly.contains("case 11:// 毒咒"));
        assertTrue(olderTargetOnly.contains("targetId = readD();"));
        assertFalse(olderTargetOnly.contains("targetX = readH();"));

        String retainedUse = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(retainedUse.contains(
                "L1DamagePoison.doInfection(this._user, cha, 3000, 5);"));
        String retainedPoison = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/poison/L1DamagePoison.java"),
                StandardCharsets.UTF_8);
        assertTrue(retainedPoison.contains(
                "_target.setSkillEffect(STATUS_POISON, 30000);"));
        assertTrue(retainedPoison.contains("_target.setPoisonEffect(1);"));
        assertTrue(retainedPoison.contains("isValidTarget(cha)"));

        String catalog = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains(
                "CURSE_POISON, new PoisonSpell(CURSE_POISON, 1, 2, 19, 745,"));
        assertTrue(catalog.contains(
                "10, 500, 1, 30, 3_000, 5, 12, 0, -10, 280,"));
        assertTrue(catalog.contains("40318, 1, \"curse-poison\")"));

        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String world = between(session,
                "private void useClassicWorldPoisonSpell(",
                "private void usePoisonSpell(");
        String battle = between(session,
                "private void usePoisonSpell(",
                "/** Retained skill-33 path");
        for (String route : new String[] {world, battle}) {
            assertTrue(route.contains(
                    "Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(payload);"));
            assertTrue(route.contains("request.x()"));
            assertTrue(route.contains("request.y()"));
            assertTrue(route.contains("spell.itemConsumeId()"));
            assertTrue(route.contains("spell.tickIntervalMillis()"));
            assertTrue(route.contains("spell.tickDamage()"));
        }
        assertTrue(world.contains("hub.poisonClassicWorldMonster("));
        assertTrue(battle.contains("hub.poisonQuestMonster("));
        assertTrue(battle.contains("hub.poisonTimeAttackActor("));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
